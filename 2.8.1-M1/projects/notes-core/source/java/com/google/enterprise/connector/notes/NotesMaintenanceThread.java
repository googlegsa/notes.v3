package com.google.enterprise.connector.notes;

import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.enterprise.connector.spi.SpiConstants.ActionType;

import lotus.domino.*;

// The purpose of this class is check for deletions of indexed documents
// Documents should be deleted if the meet either of the following criteria
// 1.  They no longer exist in the source database
// 2.  They belong to database which is marked for deletion



public class NotesMaintenanceThread extends Thread {
	private static final String CLASS_NAME = NotesMaintenanceThread.class.getName();
	private static final Logger _logger = Logger.getLogger(CLASS_NAME);
	
	NotesConnector nc = null;
	NotesConnectorSession ncs = null;
	lotus.domino.Database cdb = null;
	NotesPollerNotifier npn = null;
	String OpenDbRepId = "";
	Database SrcDb = null;
	Document DbConfigDoc = null;
	lotus.domino.Session ns = null;
	String DbConfigDocRepId = "";
	DateTime CheckTime = null;
	String IndexedDocRepId = "";
	Document TemplateDoc = null;
	Document SourceDocument = null;
	Document IndexedDoc = null;
	
	NotesMaintenanceThread(NotesConnector Connector, NotesConnectorSession Session) {
		final String METHOD = "NotesMaintenanceThread";
		_logger.logp(Level.INFO, CLASS_NAME, METHOD, "NotesMaintenanceThread being created.");

		nc = Connector;
		ncs = Session;
	}
	
	public void run() {
		final String METHOD = "run";
		_logger.entering(CLASS_NAME, METHOD);
		
		int exceptionCount = 0;
		int batchsize = ncs.getDeletionBatchSize();
		String lastdocid = "";
		NotesPollerNotifier npn = ncs.getNotifier();
		while (nc.getShutdown() == false) {
			try
			{
				_logger.logp(Level.INFO, CLASS_NAME, METHOD, "Maintenance thread checking for deletions.");
				checkForDeletions(lastdocid, batchsize);
				_logger.logp(Level.INFO, CLASS_NAME, METHOD, "Maintenance thread sleeping after checking for deletions.");
				npn.waitForWork();
				_logger.logp(Level.INFO, CLASS_NAME, METHOD, "Maintenance thread resuming to check for deletions.");
				
			}
			catch(Exception e)
			{
				_logger.log(Level.SEVERE, CLASS_NAME, e);
				// Lets say the server we are connected to goes down while we are crawling
				// We don't want to fill up the logs with errors so go to sleep after 5 exceptions
				exceptionCount++;
				if (exceptionCount > 5) {
					_logger.logp(Level.WARNING, CLASS_NAME, METHOD, "Too many exceptions.  Maintenance thread sleeping.");
					npn.waitForWork();
					_logger.logp(Level.WARNING, CLASS_NAME, METHOD, "Maintenance thread resuming after too many exceptions were encountered.");
				}
			}
		}
		_logger.logp(Level.INFO, CLASS_NAME, METHOD, "Maintenance thread exiting after connector shutdown.");
		_logger.exiting(CLASS_NAME, METHOD);
	}
	
	/*
	 * Checks for documents which have been deleted in the INDEXED view
	 * startdocid - id to start checking from
	 * batchsize - number of documents to check in this batch
	 * return value - id of the last document checked
	 */
	protected String checkForDeletions(String startdocid, int batchsize){
		final String METHOD = "checkForDeletions";
		_logger.entering(CLASS_NAME, METHOD);

		int NumChecked = 0;
		String lastdocid = "";
		lotus.domino.Document PrevDoc = null;

		try {
			
			_logger.logp(Level.INFO, CLASS_NAME, METHOD, "Checking for deletions ");			
			ns = ncs.createNotesSession();
			CheckTime = ns.createDateTime("1/1/1900");
			CheckTime.setNow();
			cdb = ns.getDatabase(ncs.getServer(), ncs.getDatabase());
			View DatabaseView = cdb.getView(NCCONST.VIEWDATABASES);
			
			DatabaseView.refresh();
			_logger.logp(Level.INFO, CLASS_NAME, METHOD, "MaintenanceThread: Entries in database view: " + DatabaseView.getEntryCount());
			
			View IndexedView = cdb.getView(NCCONST.VIEWINDEXED);
			IndexedView.refresh();
			_logger.logp(Level.INFO, CLASS_NAME, METHOD, "MaintenanceThread: Entries in indexed view: " + IndexedView.getEntryCount());

			IndexedDoc = IndexedView.getDocumentByKey(startdocid);
			if (null == IndexedDoc) {
				_logger.logp(Level.INFO, CLASS_NAME, METHOD, "MaintenanceThread: Restarting deletion check.");
				IndexedDoc = IndexedView.getFirstDocument();
			}
			while ((null != IndexedDoc) && (NumChecked < batchsize) && (!nc.getShutdown()))
			{
				NumChecked++;
				IndexedView.refresh();
				String DocId = IndexedDoc.getItemValueString(NCCONST.ITM_DOCID);
				_logger.logp(Level.FINER, CLASS_NAME, METHOD, "MaintenanceThread: Checking deletion for document:  " + DocId);
				
				String State = IndexedDoc.getItemValueString(NCCONST.NCITM_STATE);
				if (!State.equalsIgnoreCase(NCCONST.STATEINDEXED)){
					_logger.logp(Level.SEVERE, CLASS_NAME, METHOD, "MaintenanceThread: Skipping deletion check since state is not indexed." + DocId);
					PrevDoc = IndexedDoc;
					IndexedDoc = IndexedView.getNextDocument(PrevDoc);
					PrevDoc.recycle();
					continue;
				}

				loadDbConfigDoc(IndexedDoc.getItemValueString(NCCONST.NCITM_REPLICAID), DatabaseView);
				if (null == DbConfigDoc) {
					_logger.logp(Level.SEVERE, CLASS_NAME, METHOD, "MaintenanceThread: Skipping document because no database config found for " + DocId);
					PrevDoc = IndexedDoc;
					IndexedDoc = IndexedView.getNextDocument(PrevDoc);
					PrevDoc.recycle();
					continue;
				}
				
				// When a database is in stopped mode we purge all documents
				if (getStopped()) {
					_logger.logp(Level.INFO, CLASS_NAME, METHOD, "MaintenanceThread: Deleting document because database is being purged. " + DocId);
					createDeleteRequest(IndexedDoc, IndexedDoc.getItemValueString(NCCONST.ITM_DOCID));
					PrevDoc = IndexedDoc;
					IndexedDoc = IndexedView.getNextDocument(PrevDoc);
					PrevDoc.recycle();
					continue;
				}
				
				// Is this database configured to check for deletions?
				String checkDeletions = DbConfigDoc.getItemValueString(NCCONST.DITM_CHECKDELETIONS);
				if (checkDeletions.toLowerCase().contentEquals("no")) {
					_logger.logp(Level.FINER, CLASS_NAME, METHOD, "MaintenanceThread: Skipping document because deletion checking is not enabled. " + DocId);
					PrevDoc = IndexedDoc;
					IndexedDoc = IndexedView.getNextDocument(PrevDoc);
					PrevDoc.recycle();
					continue;					
				}
				
				// Is crawling enabled for this database?  If not then skip to the next document
				int isEnabled = DbConfigDoc.getItemValueInteger(NCCONST.DITM_CRAWLENABLED);
				if (isEnabled != 1) {
					_logger.logp(Level.FINER, CLASS_NAME, METHOD, "MaintenanceThread: Skipping document because database crawling is disabled. " + DocId);
					PrevDoc = IndexedDoc;
					IndexedDoc = IndexedView.getNextDocument(PrevDoc);
					PrevDoc.recycle();
					continue;					
				}
				
				// Try and open the source database
				boolean SrcDbOpened = openSourceDatabase(IndexedDoc);
				if (!SrcDbOpened) {
					_logger.logp(Level.SEVERE, CLASS_NAME, METHOD, "MaintenanceThread: Skipping document because source database could not be opened : " + DocId);
					PrevDoc = IndexedDoc;
					IndexedDoc = IndexedView.getNextDocument(PrevDoc);
					PrevDoc.recycle();
					continue;										
				}
				
				boolean DocDeleted = loadSourceDocument(IndexedDoc.getItemValueString(NCCONST.NCITM_UNID));
				if (DocDeleted) {
					createDeleteRequest(IndexedDoc, IndexedDoc.getItemValueString(NCCONST.ITM_DOCID));
					PrevDoc = IndexedDoc;
					IndexedDoc = IndexedView.getNextDocument(PrevDoc);
					PrevDoc.recycle();
					continue;
				}
				
				boolean isConflict = SourceDocument.hasItem(NCCONST.NCITM_CONFLICT);
				if (isConflict){
					createDeleteRequest(IndexedDoc, IndexedDoc.getItemValueString(NCCONST.ITM_DOCID));
					PrevDoc = IndexedDoc;
					IndexedDoc = IndexedView.getNextDocument(PrevDoc);
					PrevDoc.recycle();
					continue;				
				}
				
				loadTemplateDoc(DbConfigDoc.getItemValueString(NCCONST.DITM_TEMPLATE));
				if (null == TemplateDoc) {
					_logger.logp(Level.SEVERE, CLASS_NAME, METHOD, "MaintenanceThread: Skipping selection criteria check because template could not be opened : " + DocId);
					PrevDoc = IndexedDoc;
					IndexedDoc = IndexedView.getNextDocument(PrevDoc);
					PrevDoc.recycle();
					continue;															
				}
				
				boolean meetsCriteria = checkSelectionCriteria();
				if(!meetsCriteria) {
					_logger.logp(Level.INFO, CLASS_NAME, METHOD, "MaintenanceThread: Deleting document because selection formula returned false : " + DocId);
					PrevDoc = IndexedDoc;
					IndexedDoc = IndexedView.getNextDocument(PrevDoc);
					PrevDoc.recycle();
					continue;															
				}
				
				// Document has passed all checks and should remain in the index
				lastdocid = IndexedDoc.getItemValueString(NCCONST.NCITM_UNID);
				PrevDoc = IndexedDoc;
				IndexedDoc = IndexedView.getNextDocument(PrevDoc);
				PrevDoc.recycle();		
			}
			IndexedView.recycle();
		}
		catch(Exception e)
		{
			_logger.log(Level.SEVERE, CLASS_NAME, e); 
		}
		finally
		{
			cleanUpNotesObjects();
			ncs.closeNotesSession(ns);
			_logger.exiting(CLASS_NAME, METHOD);
		}
		return (lastdocid);
	}
	
	protected void cleanUpNotesObjects() {
		final String METHOD = "cleanUpNotesObjects";
		_logger.entering(CLASS_NAME, METHOD);
		try {
			if (null != CheckTime)
				CheckTime.recycle();
			CheckTime = null;
			if (null != SourceDocument)
				SourceDocument.recycle();
			SourceDocument = null;
			if (null != TemplateDoc)
				TemplateDoc.recycle();
			TemplateDoc = null;
			if (null != IndexedDoc)
				IndexedDoc.recycle();
			IndexedDoc = null;
			DbConfigDocRepId = "";
			if (null != DbConfigDoc)
				DbConfigDoc.recycle();
			DbConfigDoc = null;
			OpenDbRepId ="";
			if (null != SrcDb)
				SrcDb.recycle();
			SrcDb=null;
			
			if (null != cdb)
				cdb.recycle();
			cdb = null;
		}
		catch (NotesException e) {
			_logger.log(Level.SEVERE, CLASS_NAME, e); 
		}
		_logger.exiting(CLASS_NAME, METHOD);
	}
	/*
	 *   Only start purging documents after a little while
	 *   
	 */
	protected boolean getStopped () throws NotesException{
		final String METHOD = "getStopped";
		
		// TODO:  Think about adding variable to provide some grace time
		/*
		_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "CheckTime is: " + CheckTime);
		DateTime lm = DbConfigDoc.getLastModified();
		_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "Last Modified is: " + lm);

		int timediff = CheckTime.timeDifference(lm);
		_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "Time Diff is: " + timediff);
		if (CheckTime.timeDifference(lm) < 300) {
			return false;
		}
		*/
		if (1 == DbConfigDoc.getItemValueInteger(NCCONST.DITM_STOPPED))
			return true;
		return false; 
	}

	protected boolean openSourceDatabase(Document IndexedDoc) throws NotesException{
		final String METHOD = "openSourceDatabase";
		_logger.entering(CLASS_NAME, METHOD);
		
		String ReplicaId = IndexedDoc.getItemValueString(NCCONST.NCITM_REPLICAID);
		if (OpenDbRepId.contentEquals(ReplicaId))
			return true;
		// Different ReplicaId - Recycle and close the old database
		if (SrcDb != null) {
			SrcDb.recycle();
			SrcDb= null;
			OpenDbRepId = "";
		}
		// Open the new database
		SrcDb = ns.getDatabase(null, null); 
		boolean srcdbisopen = SrcDb.openByReplicaID(IndexedDoc.getItemValueString(NCCONST.NCITM_SERVER), ReplicaId);
		if (srcdbisopen)
			OpenDbRepId = ReplicaId;
		else
			_logger.logp(Level.SEVERE, CLASS_NAME, METHOD, "Maintenance thread can't open database: " + ReplicaId);
		_logger.exiting(CLASS_NAME, METHOD);
		return (srcdbisopen);
	}

	
	protected void loadTemplateDoc(String TemplateName) throws NotesException {
		final String METHOD = "loadTemplateDoc";
		_logger.entering(CLASS_NAME, METHOD);
		_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "Loading template: " + TemplateName);
		// Is a template document all ready loaded?
		if (null != TemplateDoc) {
			// Is this the one we need?  
			String existingTemplate = TemplateDoc.getItemValueString(NCCONST.TITM_TEMPLATENAME); 
			_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "Existing template is: " + TemplateName);
			if (TemplateName.equals(existingTemplate))
				return;
			TemplateDoc.recycle();
			TemplateDoc = null;
		}
		View vw = cdb.getView(NCCONST.VIEWTEMPLATES);
		TemplateDoc = vw.getDocumentByKey(TemplateName, true);
		vw.recycle();
		if (null != TemplateDoc)
			_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "Loaded template: " + TemplateDoc.getItemValueString(NCCONST.TITM_TEMPLATENAME));
		_logger.exiting(CLASS_NAME, METHOD);
	}
	
	protected void loadDbConfigDoc(String ReplicaId, View DatabaseView) throws NotesException {
		final String METHOD = "loadDbConfigDoc";
		_logger.entering(CLASS_NAME, METHOD);
		if (ReplicaId.contentEquals(DbConfigDocRepId))
			return;
		if (DbConfigDoc != null) {
			DbConfigDoc.recycle();
			DbConfigDoc = null;
			DbConfigDocRepId = "";
		}
		DbConfigDoc = DatabaseView.getDocumentByKey(ReplicaId);
		if (null == DbConfigDoc) {
			_logger.logp(Level.SEVERE, CLASS_NAME, METHOD, "Maintenance thread can't find database config for replica : " 
					+ ReplicaId);
			return;
		}
		DbConfigDocRepId = ReplicaId;
		_logger.exiting(CLASS_NAME, METHOD);
		return;
	}
	
	protected boolean checkSelectionCriteria() throws NotesException {
		final String METHOD = "checkSelectionCriteria";
		_logger.entering(CLASS_NAME, METHOD);

		String SelectionFormula = TemplateDoc.getItemValueString(NCCONST.TITM_SEARCHSTRING);
		_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "Using selection formula: " + SelectionFormula);

		Vector<Double> VecEvalResult =  (Vector<Double>) ns.evaluate(SelectionFormula, SourceDocument);
		// A Selection formula will return a vector of doubles.  
		if (1 == VecEvalResult.elementAt(0)) {
			_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "Selection formula returned true");
			_logger.exiting(CLASS_NAME, METHOD);
			return (true);
		}
		_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "Selection formula returned false");
		_logger.exiting(CLASS_NAME, METHOD);
		return (false);
	}
	
	
	/*
	 * Check to see if the source document still exists in the source database
	 */
	protected boolean loadSourceDocument(String UNID) throws NotesException {
		final String METHOD = "loadSourceDocument";
		_logger.entering(CLASS_NAME, METHOD);
		
		try {
			// See if we can get the document
			if (null != SourceDocument) {
				SourceDocument.recycle();
				SourceDocument = null;
			}
			SourceDocument = SrcDb.getDocumentByUNID(UNID);
		}
		catch (NotesException e) {
			if (e.id == NotesError.NOTES_ERR_BAD_UNID) {
				_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "Document has been deleted " + UNID);
				_logger.exiting(CLASS_NAME, METHOD);
				return (true);
			}
		}
		_logger.exiting(CLASS_NAME, METHOD);
		return(false);
	}

	/*
	 * Create a request to delete this document
	 */
	protected void createDeleteRequest(Document IndexedDoc, String DocId) throws NotesException {
		final String METHOD = "createDeleteRequest";
		_logger.entering(CLASS_NAME, METHOD);
		Document DeleteReq = cdb.createDocument();
		DeleteReq.appendItemValue(NCCONST.ITMFORM, NCCONST.FORMCRAWLREQUEST);
		DeleteReq.replaceItemValue(NCCONST.ITM_ACTION, ActionType.DELETE.toString());
		DeleteReq.replaceItemValue(NCCONST.ITM_DOCID, DocId);
		DeleteReq.replaceItemValue(NCCONST.NCITM_STATE, NCCONST.STATEFETCHED);
		DeleteReq.save(true);
		IndexedDoc.replaceItemValue(NCCONST.NCITM_STATE, NCCONST.STATEDELETED);
		IndexedDoc.save(true);
		_logger.exiting(CLASS_NAME, METHOD);
	}

}
