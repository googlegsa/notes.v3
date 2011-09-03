package com.google.enterprise.connector.notes;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Vector;

import lotus.domino.NotesFactory;
import lotus.domino.NotesThread;
import lotus.domino.NotesException;
import lotus.domino.View;
import lotus.domino.Document;
import lotus.domino.Item;
import lotus.domino.DocumentCollection;
import com.google.enterprise.connector.spi.SpiConstants.ActionType;

public class NotesCrawlerThread extends Thread {
	private static final String CLASS_NAME = NotesCrawlerThread.class.getName();
	private static final Logger _logger = Logger.getLogger(CLASS_NAME);
	private String attachRoot = null;
	NotesConnector nc = null;
	NotesConnectorSession ncs = null;
	lotus.domino.Session ns = null;
	lotus.domino.Database cdb = null;
	lotus.domino.Document templateDoc = null;
	lotus.domino.Document formDoc = null;
	lotus.domino.DocumentCollection formsdc = null;
	String OpenDbRepId = "";
	lotus.domino.Database srcdb = null;
	lotus.domino.View crawlQueue = null;


	NotesCrawlerThread(NotesConnector Connector, NotesConnectorSession Session) {
		final String METHOD = "NotesCrawlerThread";
		_logger.logp(Level.INFO, CLASS_NAME, METHOD, "NotesCrawlerThread being created.");

		nc = Connector;
		ncs = Session;
	}
		
	// Since we are multi-threaded, each thread has its own objects which are not shared.
	// Hence the calling thread must pass the Domino objects to this method.
	private static synchronized lotus.domino.Document getNextFromCrawlQueue(lotus.domino.Session ns, lotus.domino.View crawlQueue){
		final String METHOD = "getNextFromCrawlQueue";
		try
		{
			crawlQueue.refresh();
			lotus.domino.Document nextDoc = crawlQueue.getFirstDocument();
			if (nextDoc == null)
				return(null);
			_logger.logp(Level.FINER, CLASS_NAME, METHOD, "Prefetching document");
			nextDoc.replaceItemValue(NCCONST.NCITM_STATE, NCCONST.STATEINCRAWL);
			nextDoc.save(true);
			
			return(nextDoc);
		}
		catch(Exception e)
		{
			_logger.log(Level.SEVERE, CLASS_NAME, e); 
		}
		finally
		{
		}
		return null;
	}
	
	protected void loadTemplateDoc(String TemplateName) throws NotesException {
		final String METHOD = "loadTemplate";
		_logger.entering(CLASS_NAME, METHOD);
		
		// Is a template document all ready loaded?
		if (null != templateDoc) {
			// Is this the one we need?  
			if (TemplateName.equals(templateDoc.getItemValueString(NCCONST.TITM_TEMPLATENAME)))
				return;
			templateDoc.recycle();
			templateDoc = null;
			if (null != formsdc)
				formsdc.recycle(); 
			formsdc = null;
			if (null != formDoc)
				formDoc.recycle();
			formDoc = null;
		}
		View vw = cdb.getView(NCCONST.VIEWTEMPLATES);
		templateDoc = vw.getDocumentByKey(TemplateName, true);
		formsdc = templateDoc.getResponses();
		vw.recycle();
	}
	
	protected void loadForm(String FormName) throws NotesException {
		final String METHOD = "loadForm";
		_logger.entering(CLASS_NAME, METHOD);
		
		if (null != formDoc) {
			if (FormName == formDoc.getItemValueString(NCCONST.FITM_LASTALIAS)) 
				return;
			formDoc.recycle();
			formDoc = null;
		}
		if (null == formsdc)
			return;
		formDoc = formsdc.getFirstDocument();
		while (null != formDoc) {
			String formDocName = formDoc.getItemValueString(NCCONST.FITM_LASTALIAS);
			if (formDocName.equals(FormName))
				return;
			Document prevDoc = formDoc;
			formDoc = formsdc.getNextDocument(prevDoc);
			prevDoc.recycle();
		}
		
	}
	/*
	 *   Some comments on Domino.
	 *   Reader security is only enforced in Domino if there are Readers fields on the document and they are non-blank
	 *   Authors fields also provide read access to the document if document level security is enforced.  However if there
	 *   are authors fields, but not any non-blank readers fields, document level security will not be enforced.
	 */
	protected void getDocumentReaderNames(Document crawlDoc, Document srcDoc) throws NotesException {
		final String METHOD = "getDocumentReaderNames";
		_logger.entering(CLASS_NAME, METHOD);
		
		
		Item itm = null;
		Item allReaders = crawlDoc.replaceItemValue(NCCONST.NCITM_DOCREADERS, null);
		Vector<?> allItems = srcDoc.getItems();
		Vector<String> AuthorReaders = new Vector<String>();
		
		for (int i=0; i<allItems.size(); i++) {
			itm = (Item)allItems.elementAt(i);
			if (itm.isReaders()) {
				Vector<?> readersVals = itm.getValues();
				if (null != readersVals) {
					_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "Adding readers " + readersVals.toString());
					allReaders.appendToTextList(readersVals);
					for (int j=0; j < readersVals.size(); j++) {
						AuthorReaders.add(readersVals.elementAt(j).toString().toLowerCase());
					}
				}
			}
			if (itm.isAuthors()) {
				Vector<?> authorsVals = itm.getValues();
				if (null != authorsVals) {
					_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "Adding authors " + authorsVals.toString());
					for (int j=0; j < authorsVals.size(); j++) {
						AuthorReaders.add(authorsVals.elementAt(j).toString().toLowerCase());
					}
				}
			}
		}
		_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "Document readers are " + AuthorReaders);
		if (AuthorReaders.size() > 0)
			crawlDoc.replaceItemValue(NCCONST.NCITM_DOCAUTHORREADERS, AuthorReaders);
	}
	
	// This function will set google security fields for the document
	protected void setDocumentSecurity(Document crawlDoc, Document srcDoc) throws NotesException {
		final String METHOD = "setDocumentSecurity";
		_logger.entering(CLASS_NAME, METHOD);
		
		String AuthType = crawlDoc.getItemValueString(NCCONST.NCITM_AUTHTYPE);
		
		if (AuthType.equals(NCCONST.AUTH_NONE)) {
			crawlDoc.replaceItemValue(NCCONST.ITM_ISPUBLIC, Boolean.TRUE.toString());
			return;
		}
		if (AuthType.equals(NCCONST.AUTH_ACL)) {
			crawlDoc.replaceItemValue(NCCONST.ITM_ISPUBLIC, Boolean.FALSE.toString());

			;  // TODO: Handle document ACLs
			return;
		}
		if (AuthType.equals(NCCONST.AUTH_CONNECTOR)) {
			crawlDoc.replaceItemValue(NCCONST.ITM_ISPUBLIC, Boolean.FALSE.toString());

			;  
			return;
		}
			
	}

	protected void evaluateField(Document crawlDoc, Document srcDoc, String formula, String ItemName, String Default) throws NotesException {
		final String METHOD = "evaluateField";
		_logger.entering(CLASS_NAME, METHOD);
		
		Vector<?> VecEvalResult = null;
		String Result = null;
		try {
			_logger.logp(Level.FINER, CLASS_NAME, METHOD, "Evaluating formula for item " + ItemName + " : src is: " + formula);
			VecEvalResult = ns.evaluate(formula, srcDoc);
			// Make sure we dont' get an empty vector or an empty string
			if (VecEvalResult != null)
				if (VecEvalResult.size()>0) {
					Result = VecEvalResult.elementAt(0).toString();
					_logger.logp(Level.FINER, CLASS_NAME, METHOD, "Evaluating formula result is: " + Result);
				}
			if (null == Result)
				Result = Default;
			if (Result.length() == 0)
				Result = Default;
		}
		catch (NotesException e) {
			_logger.log(Level.SEVERE, CLASS_NAME, e);
		}
		finally {
			crawlDoc.replaceItemValue(ItemName, Result);			
		}
		_logger.exiting(CLASS_NAME, METHOD);
	}
	
	
	// TODO: Consider mapping other fields so they can be used for dynamic navigation.
	// This could be an configurable option.
	
	// This function will map the fields from the source database to the crawl doc
	// using the configuration specified in formDoc
	protected void mapFields(Document crawlDoc, Document srcDoc) throws NotesException{
		final String METHOD = "mapFields";
		_logger.entering(CLASS_NAME, METHOD);

		// Copy the standard fields
		String NotesURL = srcDoc.getNotesURL();
		String HttpURL = getHTTPURL(crawlDoc);
		crawlDoc.replaceItemValue(NCCONST.ITM_DOCID, HttpURL);
		crawlDoc.replaceItemValue(NCCONST.ITM_GMETAFORM, srcDoc.getItemValueString(NCCONST.ITMFORM));
		crawlDoc.replaceItemValue(NCCONST.ITM_LASTMODIFIED, srcDoc.getLastModified());
		crawlDoc.replaceItemValue(NCCONST.ITM_GMETAWRITERNAME, srcDoc.getAuthors());
		crawlDoc.replaceItemValue(NCCONST.ITM_GMETALASTUPDATE, srcDoc.getLastModified());
		crawlDoc.replaceItemValue(NCCONST.ITM_GMETACREATEDATE, srcDoc.getCreated());
		
		// We need to generate the title and description using a formula
		String formula = null;
		// When there is no form configuration use the config from the template
		if (formDoc != null)
			formula = formDoc.getItemValueString(NCCONST.FITM_SEARCHRESULTSFORMULA);
		else
			formula = templateDoc.getItemValueString(NCCONST.TITM_SEARCHRESULTSFIELDS);
		evaluateField(crawlDoc, srcDoc, formula, NCCONST.ITM_TITLE, "");
		
		// Again..when there is no form configuration use the config from the template
		if (formDoc != null)
			formula = formDoc.getItemValueString(NCCONST.FITM_DESCRIPTIONFORMULA);
		else
			formula = templateDoc.getItemValueString(NCCONST.TITM_DESCRIPTIONFIELDS);
		evaluateField(crawlDoc, srcDoc, formula, NCCONST.ITM_GMETADESCRIPTION, "");
		_logger.exiting(CLASS_NAME, METHOD);
		
		// Don't map these here -> just do it in the document properties
		// crawlDoc.replaceItemValue(NCCONST.ITM_DISPLAYURL, HttpURL);
		// crawlDoc.replaceItemValue(NCCONST.ITM_GMETATOPIC, VecSearchTitle.elementAt(0));		
		// DO NOT MAP THIS FIELD - it will force the GSA to try and crawl this URL
		// crawlDoc.replaceItemValue(NCCONST.ITM_SEARCHURL, HttpURL);  
	}
	
	protected String getHTTPURL(lotus.domino.Document crawlDoc) throws NotesException {
		
		String httpURL = null;
		String server = null;
		
		// Get the domain name associated with the server
		server = crawlDoc.getItemValueString(NCCONST.NCITM_SERVER);
		String domain = ncs.getDomain(server);
		
		httpURL = String.format("http://%s%s/%s/0/%s", crawlDoc.getItemValueString(NCCONST.NCITM_SERVER), 
				domain,
				crawlDoc.getItemValueString(NCCONST.NCITM_REPLICAID),
				crawlDoc.getItemValueString(NCCONST.NCITM_UNID));
		return (httpURL);
	}	
	
	
	protected String getContentFields (lotus.domino.Document srcDoc) throws NotesException {
		final String METHOD = "getContentFields";
		_logger.entering(CLASS_NAME, METHOD);
		
		// TODO:  Handle stored forms
		StringBuffer content = new StringBuffer();
		// If we have a form document then we have a specified list of fields to index
		if (null != formDoc) {
			Vector<?> v = formDoc.getItemValue(NCCONST.FITM_FIELDSTOINDEX);
			for (int i = 0; i<v.size(); i++) {
				String fieldName = v.elementAt(i).toString();
				// Fields beginning with $ are reserved fields in Domino
				// Do not index the Form field ever
				if ((fieldName.charAt(0) == '$') || (fieldName.equalsIgnoreCase("form")))
					continue;
				content.append("\n");
				Item tmpItem = srcDoc.getFirstItem(fieldName);
				if (null != tmpItem) {
					content.append(tmpItem.getText(2*1024*1024));  // Must use getText to get more than 64k of text
					tmpItem.recycle();
				}
			}
			_logger.exiting(CLASS_NAME, METHOD);
			return content.toString();
		}

		// Otherwise we will index all allowable fields
		Vector <?> vi = srcDoc.getItems();
		for (int j = 0; j<vi.size(); j++) {
			Item itm = (Item)vi.elementAt(j);
			String ItemName = itm.getName();
			if ((ItemName.charAt(0) == '$') || (ItemName.equalsIgnoreCase("form")))
				continue;
			int type = itm.getType();
			switch (type) {
				case Item.TEXT:
				case Item.NUMBERS:
				case Item.DATETIMES:
				case Item.RICHTEXT:
				case Item.NAMES:
				case Item.AUTHORS:
				case Item.READERS:
					content.append("\n");
					Item tmpItem = srcDoc.getFirstItem(ItemName);
					if (null != tmpItem) {
						content.append(tmpItem.getText(2*1024*1024));  // Must use getText to get more than 64k of text
						tmpItem.recycle();
					}
					break;
				default:
					break;
			}			
		}
		_logger.exiting(CLASS_NAME, METHOD);
		return content.toString();
	}
	
	protected boolean prefetchDoc(lotus.domino.Document crawlDoc) {
		final String METHOD = "prefetchDoc";
		_logger.entering(CLASS_NAME, METHOD);
		
		
		String NotesURL = null;
		lotus.domino.Document srcDoc = null;
		try
		{
			NotesURL = crawlDoc.getItemValueString(NCCONST.ITM_GMETANOTESLINK);
			_logger.logp(Level.INFO, CLASS_NAME, METHOD, "Prefetching document " + NotesURL);
			
			// Get the template for this document
			loadTemplateDoc(crawlDoc.getItemValueString(NCCONST.NCITM_TEMPLATE));
			if (null == templateDoc) {
				_logger.logp(Level.INFO, CLASS_NAME, METHOD, "No template found for document " + crawlDoc.getItemValueString(NCCONST.ITM_GMETANOTESLINK));
				return false;
			}

			// Check to see if the database we all ready have open is the right one by comparing replicaids
			String crawlDocDbRepId = crawlDoc.getItemValueString(NCCONST.NCITM_REPLICAID);
			if (!crawlDocDbRepId.contentEquals(OpenDbRepId)) {
				// Different ReplicaId - Recycle and close the old database
				if (srcdb != null) {
					srcdb.recycle();
					srcdb= null;
				}
				// Open the new database
				srcdb = ns.getDatabase(null, null); 
				srcdb.openByReplicaID(crawlDoc.getItemValueString(NCCONST.NCITM_SERVER), crawlDocDbRepId);
				OpenDbRepId = crawlDocDbRepId;
			}
			
			// Load our source document
			srcDoc = srcdb.getDocumentByUNID(crawlDoc.getItemValueString(NCCONST.NCITM_UNID)); 
			loadForm(srcDoc.getItemValueString(NCCONST.ITMFORM));
			if (null == formDoc)
				_logger.logp(Level.INFO, CLASS_NAME, METHOD, "No form definition found.  Using template definition to process document " + NotesURL);
			
			// Get the form configuration for this document
			getDocumentReaderNames(crawlDoc, srcDoc);
			setDocumentSecurity(crawlDoc, srcDoc);
			
			mapFields(crawlDoc, srcDoc);
			
			// Process the attachments associated with this document
			// When there are multiple attachments with the same name
			// Lotus Notes automatically generates unique names for next document
		    Vector<?> va = ns.evaluate("@AttachmentNames", srcDoc);

		   
		    
		    Item attachItems = crawlDoc.replaceItemValue(NCCONST.ITM_GMETAATTACHMENTS, "");
		    for (int i = 0; i<va.size(); i++) {
		    	String attachName = va.elementAt(i).toString();
		    	
		    	if (attachName.length() == 0)
		    		continue;
		    	String xtn = null;
		    	int period = attachName.lastIndexOf(".");
		    	if (period == -1)
		    		xtn = "";
		    	else
		    		xtn = attachName.substring(period+1);
		    	if (!ncs.isExcludedExtension(xtn.toLowerCase())) {
		    		boolean success = createAttachmentDoc(crawlDoc, srcDoc, attachName, ncs.getMimeType(xtn));
		    		if (success)
		    			attachItems.appendToTextList(attachName);
		    		
		    	}
		    	else
					_logger.logp(Level.INFO, CLASS_NAME, METHOD, "Excluding attachment in " + NotesURL + " : " + attachName);
			}
		    crawlDoc.replaceItemValue(NCCONST.ITM_GMETAALLATTACHMENTS, va);
			// Get our content after processing attachments
		    // We don't want the document content in the attachment docs
		    // Our content must be stored as non-summary rich text to avoid the 32/64K limits in Domino
			lotus.domino.RichTextItem contentItem = crawlDoc.createRichTextItem(NCCONST.ITM_CONTENT);
			String content = getContentFields(srcDoc);
			contentItem.appendText(content);
			contentItem.setSummary(false);
			
			// Update the status of the document to be fetched.
		    crawlDoc.replaceItemValue(NCCONST.ITM_ACTION, ActionType.ADD.toString());
			srcDoc.recycle();
			return true;
		}
		catch(Exception e)
		{
			_logger.log(Level.SEVERE, CLASS_NAME, e);
			return false;
		}
		finally
		{
			_logger.exiting(CLASS_NAME, METHOD);
		}
	}
	
	// This function creates a document for an attachment
	public boolean createAttachmentDoc (lotus.domino.Document crawlDoc, lotus.domino.Document srcDoc, String AttachmentName, String MimeType) throws NotesException
	{
		final String METHOD = "createAttachmentDoc";
		String AttachmentURL = null;
		_logger.entering(CLASS_NAME, METHOD);
		lotus.domino.EmbeddedObject eo = null;
		lotus.domino.Document attachDoc = null;
		
		try {
			// Error access the attachment
			eo = srcDoc.getAttachment(AttachmentName);

			if (eo.getType() !=  lotus.domino.EmbeddedObject.EMBED_ATTACHMENT) {
				// The object is not an attachment - could be an OLE object or link
				_logger.logp(Level.WARNING, CLASS_NAME, METHOD, "Ignoring embedded object " + AttachmentName);
				eo.recycle();
				return false;
			}

			if (null == eo) {
				_logger.logp(Level.WARNING, CLASS_NAME, METHOD, "Attachment could not be accessed " + AttachmentName);
				return false;
			}

			// Don't send attachments larger than the limit
			if (eo.getFileSize() > ncs.getMaxFileSize()) {
				_logger.logp(Level.WARNING, CLASS_NAME, METHOD, "Attachment larger than the configured limit and content will not be sent. " + AttachmentName);
			}

			attachDoc = cdb.createDocument();
			crawlDoc.copyAllItems(attachDoc, true);
			crawlDoc.replaceItemValue(NCCONST.ITM_GMETAATTACHMENTS, AttachmentName);

			String encodedAttachmentName = null;
			try {
				encodedAttachmentName = java.net.URLEncoder.encode(AttachmentName, "UTF-8");
			}
			catch (Exception e) {
				attachDoc.recycle();
				eo.recycle();
				return false;
			}
			AttachmentURL = String.format ("%s/$File/%s?OpenElement", this.getHTTPURL(crawlDoc), encodedAttachmentName );
			attachDoc.replaceItemValue(NCCONST.ITM_DOCID, AttachmentURL);

			// Only if we have a supported mime type do we send the content.
			if ((0 != MimeType.length()) || (eo.getFileSize() > ncs.getMaxFileSize())) {
				attachDoc.replaceItemValue(NCCONST.ITM_MIMETYPE, MimeType);
				String attachmentPath = getAttachmentFilePath(crawlDoc,encodedAttachmentName);
				eo.extractFile(attachmentPath);
				attachDoc.replaceItemValue(NCCONST.ITM_CONTENTPATH, attachmentPath );
			}
			else {
				// Not a supported attachment so sending meta data only with the filename as content
				attachDoc.replaceItemValue(NCCONST.ITM_CONTENT, AttachmentName);
				attachDoc.replaceItemValue(NCCONST.ITM_MIMETYPE, NCCONST.DEFAULT_MIMETYPE);
			}
			eo.recycle();		


			// DO NOT MAP THESE FIELDS
			// attachDoc.replaceItemValue(NCCONST.ITM_DISPLAYURL, AttachmentURL);

			// Set the state of this document to be fetched
			attachDoc.replaceItemValue(NCCONST.ITM_ACTION, ActionType.ADD.toString());
			attachDoc.replaceItemValue(NCCONST.NCITM_STATE, NCCONST.STATEFETCHED);
			attachDoc.save();
			attachDoc.recycle();
			_logger.exiting(CLASS_NAME, METHOD);
			return true;
		}
		catch (Exception e) {
			_logger.log(Level.SEVERE, CLASS_NAME, e);
			_logger.logp(Level.SEVERE, CLASS_NAME, METHOD, "Error pre-fetching attachment: " + AttachmentName + " in document: " + srcDoc.getNotesURL());
			if (null != eo)
				eo.recycle();
			if (null != attachDoc) {
				attachDoc.replaceItemValue(NCCONST.NCITM_STATE, NCCONST.STATEERROR);
				attachDoc.save();
				attachDoc.recycle();
			}
			return false;
		}
	}
	
	// This function will generate an unique file path for an attachment object.
	// Consider the situation where a document is updated twice and appears in the submitq twice
	// In this case, the first submit will delete the doc.  The second submit will then send an empty doc
	// So we must use the UNID of the crawl request to generate the unique filename
	public String getAttachmentFilePath  (lotus.domino.Document crawlDoc, String attachName) throws NotesException
	{
		String dirName = String.format("%s/attachments/%s/%s", 
				ncs.getSpoolDir(), 
				cdb.getReplicaID(),
				crawlDoc.getUniversalID());
		new java.io.File(dirName).mkdirs();
		String FilePath = String.format("%s/%s", dirName, attachName);
		//TODO:  Ensure that FilePath is a valid Windows filepath
		return(FilePath);
	}
	
	public void connectQueue() throws NotesException {
		if (null == ns)
			ns = ncs.createNotesSession();
		if (null == cdb)
			cdb = ns.getDatabase(ncs.getServer(), ncs.getDatabase());
		if (crawlQueue == null)
			crawlQueue = cdb.getView(NCCONST.VIEWCRAWLQ);		
	}
	
	
	/*
	 * We accumulate objects as pre-fetch documents
	 * De-allocate these in reverse order
	 */
	public void disconnectQueue()  {
		final String METHOD = "disconnectQueue";
		_logger.entering(CLASS_NAME, METHOD);
		try {
			if (null != templateDoc)
				templateDoc.recycle();
			templateDoc = null;
			
			if (null != formDoc)
				formDoc.recycle();
			formDoc = null;
			
			if (null != formsdc)
				formsdc.recycle();
			formsdc = null;
			
			if (null != srcdb) {
				OpenDbRepId = "";
				srcdb.recycle();
				srcdb = null;
			}
			
			if (null != crawlQueue)
				crawlQueue.recycle();
			crawlQueue = null;
			
			if (null != cdb)
				cdb.recycle();
			cdb = null;
			
			if (null != ns)
				ncs.closeNotesSession(ns);
			ns = null;
		}
		catch (NotesException e) {
			_logger.log(Level.SEVERE, CLASS_NAME, e);
		}
		finally {
			_logger.exiting(CLASS_NAME, METHOD);
		}
	}
	
	public void run() {
		final String METHOD = "run";
		int exceptionCount = 0;
		_logger.entering(CLASS_NAME, METHOD);
		NotesPollerNotifier npn = ncs.getNotifier();
		while (nc.getShutdown() == false) {
			try
			{
				lotus.domino.Document crawlDoc = null;
				// Only get from the queue if there is more than 300MB in the
				// spool directory
				java.io.File spoolDir = new java.io.File(ncs.getSpoolDir());
				_logger.logp(Level.FINE, CLASS_NAME, METHOD, "Spool free space is " + spoolDir.getFreeSpace());
				if (spoolDir.getFreeSpace()/1000000 < 300) {
					_logger.logp(Level.INFO, CLASS_NAME, METHOD, "Insufficient space in spool directory to process new documents.  Need at least 300MB.");
					npn.waitForWork();
					_logger.logp(Level.INFO, CLASS_NAME, METHOD, "Crawler thread resuming after spool directory had insufficient space.");
					continue;
				}
				_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "Connecting to crawl queue.");
				connectQueue();
				crawlDoc = getNextFromCrawlQueue(ns, crawlQueue);	
				if (crawlDoc == null) {
					_logger.logp(Level.INFO, CLASS_NAME, METHOD, this.getName() + ": Crawl queue is empty.  Crawler thread sleeping.");
					// If we have finished processing the queue shutdown our connections
					disconnectQueue();
					npn.waitForWork();
					_logger.logp(Level.INFO, CLASS_NAME, METHOD, this.getName() +"Crawler thread resuming after crawl queue was empty.");
					continue;
				}
				if (prefetchDoc(crawlDoc))
					crawlDoc.replaceItemValue(NCCONST.NCITM_STATE, NCCONST.STATEFETCHED);
				else 
					crawlDoc.replaceItemValue(NCCONST.NCITM_STATE, NCCONST.STATEERROR);
				crawlDoc.save(true);
				crawlDoc.recycle();
			}
			catch(Exception e)
			{
				_logger.log(Level.SEVERE, CLASS_NAME, e);
				// Lets say the server we are connected to goes down while we are crawling
				// We don't want to fill up the logs with errors so go to sleep after 5 exceptions
				exceptionCount++;
				if (exceptionCount > 5) {
					_logger.logp(Level.WARNING, CLASS_NAME, METHOD, "Too many exceptions.  Crawler thread sleeping.");
					npn.waitForWork();
					_logger.logp(Level.WARNING, CLASS_NAME, METHOD, "Crawler thread resuming after too many exceptions were encountered.");
				}
			}
		}
		disconnectQueue();
		_logger.logp(Level.FINE, CLASS_NAME, METHOD, "Connector shutdown - NotesCrawlerThread exiting.");
		_logger.entering(CLASS_NAME, METHOD);
	}


}
