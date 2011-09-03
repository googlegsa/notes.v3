package com.google.enterprise.connector.notes;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import lotus.domino.View;


import com.google.enterprise.connector.spi.Document;
import com.google.enterprise.connector.spi.DocumentList;
//import com.google.enterprise.connector.spi.Property;
import com.google.enterprise.connector.spi.RepositoryException;
import com.google.enterprise.connector.spi.SpiConstants;

class NotesConnectorDocumentList implements DocumentList {
	private static final String CLASS_NAME = NotesConnectorDocumentList.class.getName();
	private static final Logger _logger = Logger.getLogger(CLASS_NAME);
	private Iterator<String> iterator;
	private NotesConnectorDocument ncdoc = null;
	private NotesConnectorSession ncs;
	private lotus.domino.Session ns = null;
	private lotus.domino.Database db = null;			// The connector database
	private lotus.domino.Document crawldoc = null;		// The backend document being crawled
	private List<String>unidList = null;				// The list of UNIDs included in this document list


	public NotesConnectorDocumentList(NotesConnectorSession doclistncs, List<String> documents) {
		final String METHOD = "Constructor";
		_logger.logp(Level.INFO, CLASS_NAME, METHOD, "NotesConnectorDocumentList being created.");
		this.unidList = documents;
		this.iterator = documents.iterator();
		this.ncs = doclistncs;
	}

	public Document nextDocument() {
		final String METHOD = "nextDocument";
		_logger.entering(CLASS_NAME, METHOD);

		
		try {
			// The connector manager has finished last doc so recycle it
			if (null != crawldoc) {
				crawldoc.recycle();
			}
			if (null != ncdoc)
				ncdoc.closeInputStream();
			
			// Is there a next document?	
			if (!iterator.hasNext()) {
				return null;
			}

			String unid = iterator.next();
			// Create a session if we don't have one
			if (null == this.ns) {
				ns = ncs.createNotesSession();
			}
			if (null == db) {
				db = ns.getDatabase(ncs.getServer(), ncs.getDatabase());
			}
			crawldoc =db.getDocumentByUNID(unid);
			if (null == ncdoc)
				ncdoc = new NotesConnectorDocument();
			ncdoc.setCrawlDoc(unid,crawldoc); 
			
		}
		catch(Exception e)
		{
			_logger.log(Level.SEVERE, CLASS_NAME, e);
		}
		finally
		{
			_logger.exiting(CLASS_NAME, METHOD);
		}
		return ncdoc;
	}

	public void checkpointDelete(lotus.domino.Document deleteDoc, lotus.domino.View docidvw) throws lotus.domino.NotesException {
		final String METHOD = "checkpointDelete";
		_logger.entering(CLASS_NAME, METHOD);
		String docid = deleteDoc.getItemValueString(NCCONST.ITM_DOCID);
		docidvw.refresh();
		lotus.domino.Document prevDoc = docidvw.getDocumentByKey(docid, true);
		if (null != prevDoc)
			prevDoc.remove(true); 
		deleteDoc.remove(true);
		_logger.exiting(CLASS_NAME, METHOD);
	}
	
	public void checkpointAdd(lotus.domino.Document indexedDoc, lotus.domino.View docidvw) throws lotus.domino.NotesException {
		final String METHOD = "checkpointAdd";
		_logger.entering(CLASS_NAME, METHOD);
		// getItemValueString returns null in Domino 6.5 or earlier.  Empty string after.  Handle both
		String attachPath = indexedDoc.getItemValueString(NCCONST.ITM_CONTENTPATH);
		if ( null != attachPath) {
			if (attachPath.length() > 0) {
				_logger.logp(Level.FINER, CLASS_NAME, METHOD, "Checkpoint cleaning up attachment: " + attachPath);					
				java.io.File f = new java.io.File(attachPath);
				f.delete();
				// Remove the parent directory for the document if it is empty
				java.io.File parentDir = new java.io.File(attachPath.substring(0,attachPath.lastIndexOf('/')));
				String[] dirContents = parentDir.list();
				if (dirContents != null)  // If this is a valid directory
					if (dirContents.length == 0)  // If the directory is empty
						parentDir.delete();   
				// Leave the directory for the database.  
			}
		}
		
		// Delete the content, but leave the meta-data.
		// TODO:   Consider moving content to a text file and then we can cache it 
		indexedDoc.removeItem(NCCONST.ITM_CONTENT);
		// Do we all ready have a document with this url all ready?
		String docid = indexedDoc.getItemValueString(NCCONST.ITM_DOCID);
		docidvw.refresh();
		lotus.domino.Document prevDoc = docidvw.getDocumentByKey(docid, true);
		if (null != prevDoc)
			prevDoc.remove(true); 
		indexedDoc.replaceItemValue(NCCONST.NCITM_STATE, NCCONST.STATEINDEXED);
		indexedDoc.save(true);
		_logger.exiting(CLASS_NAME, METHOD);
	}
	
	public String checkpoint() throws RepositoryException {
		final String METHOD = "checkpoint";
		String checkPointUnid = null;
		try {
			_logger.entering(CLASS_NAME, METHOD);
			_logger.logp(Level.INFO, CLASS_NAME, METHOD, "Connector checkpoint documents.");
			
			// If we don't have a new checkpoint we return null
			if (ncdoc != null) {				
				//Otherwise our checkpoint should be the UNID of the current document in the doclist
				checkPointUnid = ncdoc.getUNID();
				View docidvw = db.getView(NCCONST.VIEWINDEXED);
				
				// We need to iterate through the doclist and clean up the pre-fetched documents and file system objects
				String indexedDocUnid = "";
				for (Iterator<String> ci = unidList.iterator(); ci.hasNext();) {
					indexedDocUnid =  ci.next();
					_logger.logp(Level.FINER, CLASS_NAME, METHOD, "Checkpointing document: " + indexedDocUnid);					
					lotus.domino.Document indexedDoc = db.getDocumentByUNID(indexedDocUnid);
					if (indexedDoc.getItemValueString(NCCONST.ITM_ACTION).equalsIgnoreCase(SpiConstants.ActionType.ADD.toString())) {
						checkpointAdd(indexedDoc, docidvw);
					}
					if (indexedDoc.getItemValueString(NCCONST.ITM_ACTION).equalsIgnoreCase(SpiConstants.ActionType.DELETE.toString())) {
						checkpointDelete(indexedDoc, docidvw);
					}
					indexedDoc.recycle();
					// Remove from the document list
					ci.remove();
					// Exit when we get to the checkpoint document
					if (indexedDocUnid.equals(checkPointUnid))
						break;
				}
			}
			else
				_logger.logp(Level.FINE, CLASS_NAME, METHOD, "Checkpoint for empty document list.");					
			// Without lifecycle methods, use the checkpoint to clean up our session
			if (this.crawldoc != null)
				this.crawldoc.recycle();
			if (this.db != null)
				this.db.recycle();
			if (this.ns != null)
				ncs.closeNotesSession(ns);
			this.db = null;
			this.ns = null;
			this.crawldoc = null;
		}
		catch(Exception e)
		{
			_logger.log(Level.SEVERE, CLASS_NAME, e);
		}
		finally
		{
			_logger.exiting(CLASS_NAME, METHOD);
		}

		return checkPointUnid;
	}
}
