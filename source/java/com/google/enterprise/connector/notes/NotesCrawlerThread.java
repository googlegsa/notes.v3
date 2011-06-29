//Copyright 2011 Google Inc.
//
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.

/**
 *
 */
package com.google.enterprise.connector.notes;

import java.util.Vector;

import javax.swing.SwingUtilities;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.DocumentCollection;
import lotus.domino.Item;
import lotus.domino.NotesException;
import lotus.domino.NotesFactory;
import lotus.domino.NotesThread;
import lotus.domino.Session;
import lotus.domino.View;

/**
 * @author deepti_nagarkar Apr 22, 2011 2011
 */
public class NotesCrawlerThread extends NotesThread {

	private Session session;
	// This is the name of config database in which the document stubs and
	// details will be stored

	private static String strConfigDB = "gconfigDB.nsf";// "google-connector.nsf";
	private static String strServer = "GDC40-LotusNotes";
	private Document currentDbConfigDoc;// Refers to the config document for the
	private Database configDatabase;

	// database to be crawled

	public Document getCurrentDbConfigDoc() {
		return currentDbConfigDoc;
	}

	public void setCurrentDbConfigDoc(Document currentDbConfigDoc) {
		this.currentDbConfigDoc = currentDbConfigDoc;
	}

	public void runNotes() {
		try {
			SwingUtilities.invokeLater(new Runnable() {

				public void run() {
					// TODO Auto-generated method stub

				}
			});

			session = NotesFactory.createSessionWithFullAccess("pspl!@#");
			// To bypass Readers fields restrictions
			// Session s = NotesFactory.createSessionWithFullAccess();
			String p = session.getPlatform();
			System.out.println("Platform = " + p);
			Database db = session.getDatabase("GDC40-LotusNotes", "dolphins.nsf");
			System.out.println("DB Open = " + db.isOpen());
			// db.open();
			// this.populateTraversalQ(session);
			Document docToCrawl = NotesCrawlerThread.getNextFromQueue(session);
			System.out.println("Returned from the queue " + docToCrawl);
			configDatabase = session.getDatabase(strServer, strConfigDB);
			if (!configDatabase.isOpen()) {
				configDatabase.open();
			}

			crawlDocument(session, docToCrawl);

			// Retrieve the sequence number
			// Retrieve the Database Name
			// String strParentDB =
			// docToCrawl.getItemValueString(LotusConstants.CS_DATABASE);
			// Call Crawl Document method to populate the document metadata and
			// retrieve attachments
			// this.crawlDocument(docToCrawl, strParentDB);
			// db.open();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param objSession
	 * @param currentDbConfigDoc - Refers to the config document for the
	 *            database to be crawled
	 * @return
	 */
	public synchronized static Document getNextFromQueue(Session objSession) {

		String strFunctionName = "getNextFromQueue";
		Database objConfigDB = null;
		Document docStub = null;
		try {
			if (objSession == null) {
				System.out.println("Null session reveieved. Creating a new session");
				objSession = NotesFactory.createSession();
			}
			// Start the NotesThread by called sinitThread
			// lotus.notes.NotesThread.sinitThread();
			// Open the config database view
			objConfigDB = objSession.getDatabase(strServer, strConfigDB);
			if (!objConfigDB.isOpen()) {
				objConfigDB.open();
			}
			// Retrieve the Traversal Queue
			View traversalQ = objConfigDB.getView("Traversal Queue");
			// Check if there are any documents in the view with status new
			int docCount = traversalQ.getEntryCount();// getAllEntriesByKey(LotusConstants.STATUS_NEW,
			// false).getCount();
			System.out.println("The Document Count in Traversal Queue "
					+ docCount);
			while (docCount == 0) {
				System.out.println("There are no documents in the Traversal Queue. Thread going to sleep.");
				// if there are no documents in the view, sleep
				try {
					Thread.sleep(2000);

				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				docCount = traversalQ.getEntryCount();// (LotusConstants.STATUS_NEW,
				// false).getCount();
			}
			// Retrieve the first document from the queue with status New
			docStub = traversalQ.getFirstDocument();

			// Check the document status
			Item iStatus = docStub.getFirstItem(LotusConstants.CS_STATUS);
			String strStatus = iStatus.getValueString();
			String docUNID = docStub.getItemValueString(LotusConstants.CS_ORIGDOCID);
			System.out.println("The Document ID " + docUNID);
			if (strStatus.equalsIgnoreCase(LotusConstants.STATUS_NEW)) {
				// Change the doc staus to incrawl
				System.out.println("Reterieved the doc "
						+ docStub.getItemValue(LotusConstants.CS_DATABASE)
						+ " "
						+ docStub.getItemValue(LotusConstants.CS_ORIGDOCID));
				// iStatus.setValueString(LotusConstants.STATUS_INCRAWL);

				// docStub.appendItemValue(LotusConstants.CS_STATUS,LotusConstants.STATUS_INCRAWL);
				docStub.save(true);
				traversalQ.refresh();
				return docStub;
			}

		} catch (NotesException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			// Release the config database handle
			try {
				objConfigDB.recycle();
				objConfigDB = null;
			} catch (NotesException e) {
				// TODO Auto-generated catch block
				System.out.println("[" + strFunctionName
						+ "]Error Recycling the config database object");
				e.printStackTrace();
			}
			// Call sTermThread to stop the NotesThread
			// lotus.notes.NotesThread.stermThread();
		}

		System.out.println("Returning the document stub " + docStub);
		return docStub;
	}

	/**
	 * Retrieves the metadata , ACL information and content for the given
	 * document and adds it to the config database Also changes the state of the
	 * received doc to Incrawl and after processing to Fetched. Also retrieves
	 * the attachments and creates a separate doc for each one in the config
	 * database view
	 * 
	 * @param session
	 * @param objDocToCrawl - Refers to the document for whihc the metadata is
	 *            to be fetched
	 * @param currentDbConfigDoc - Refers to the config document for the
	 *            database to be crawled
	 */
	private void crawlDocument(Session session, Document objDocToCrawl) {
		try {
			// Retrieve the parent database for the document
			String strParentDB = objDocToCrawl.getItemValueString(LotusConstants.CS_DATABASE);
			// Retrieve the document UNID
			String strDocUNID = objDocToCrawl.getItemValueString(LotusConstants.CS_ORIGDOCID);
			// Database configDB = session.getCurrentDatabase();
			Database currentDb = session.getDatabase(strServer, strParentDB);
			// Retrieve the document from the database
			Document currentDoc = currentDb.getDocumentByUNID(strDocUNID);
			System.out.println("The database  retrieved " + strParentDB);
			// Fetch the Template configured for the Database
			// Retrieve the standard meta data fields and set them in the
			// document
			objDocToCrawl.appendItemValue("CS_OriginalDocForm", currentDoc.getItemValue("Form"));
			objDocToCrawl.appendItemValue("CS_OriginalDocAuthor", currentDoc.getAuthors());
			objDocToCrawl.appendItemValue("CS_OriginalDocCreated", currentDoc.getCreated());
			objDocToCrawl.appendItemValue("CS_OriginalDocModified", currentDoc.getLastModified());
			objDocToCrawl.appendItemValue("CS_OriginalDocUnid", currentDoc.getUniversalID());
			// objDocToCrawl.appendItemValue("CS_OriginalDocLinkNotes","notes://"+currentDb.Server
			// ( 0 )+currentDb.Domain ( 0
			// )+Mid$(currentDoc.NotesURL,InStr(currentDoc.NotesURL,"/__"),Len(currentDoc.NotesURL))
			// 'currentDoc.NotesURL);
			objDocToCrawl.appendItemValue("CS_IndexServer", currentDb.getServer());
			objDocToCrawl.appendItemValue("CS_IndexServerDomain", currentDb.getURL());
			objDocToCrawl.appendItemValue("CS_OriginalDatabase", currentDb.getTitle());
			objDocToCrawl.appendItemValue("CS_OriginalDatabaseRepID", currentDb.getReplicaID());
			objDocToCrawl.appendItemValue("CS_Categories", currentDb.getCategories());
			objDocToCrawl.appendItemValue("CS_ThisDbRepID", currentDb.getReplicaID());
			// objDocToCrawl.appendItemValue("CS_Region",currentDb.getRegionInfo);
			// objDocToCrawl.appendItemValue("CS_ReplicaServers",currentDb.getReplicationInfo().getReplicaServer);
			// objDocToCrawl.appendItemValue(CS_TemplateStamp,templateDoc.TemplateStamp(0)
			// objDocToCrawl.appendItemValue("CS_IsDDM", currentDb.IsDom());
			// Retrieve the attachments from the document (if any)
			if (currentDoc.hasEmbedded()) {
				// Chekc if the document has attachments
				processAttachments(currentDoc, objDocToCrawl);
			}
			objDocToCrawl.save();

			// Retrieve the metadata fields according to the template defn
			transformMetadata(objDocToCrawl);
			// Create a doc stub with ACL Info, metadata info
			// Check if the doc has readers/authors field
			// if no, then inherit the database readers list
			// if yes, then extract the readers/authors and add to the document
			// ACL
			// Create a doc stub in the Config Database for each attachment

			//
		} catch (NotesException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * This method applies the Metadata template to the Document sent It reads
	 * the template from the Config database and retrieves the fields and other
	 * details accordingly
	 * 
	 * @param doc
	 */
	private void transformMetadata(Document doc) {
		// Get the templates view from the Config Database
		View templatesView;
		try {
			// Retrieve the parent database for the document
			String strParentDB = doc.getItemValueString(LotusConstants.CS_DATABASE);
			// Retrieve the document UNID
			String strDocUNID = doc.getItemValueString(LotusConstants.CS_ORIGDOCID);
			// Database configDB = session.getCurrentDatabase();
			Database currentDb = session.getDatabase(strServer, strParentDB);
			// Retrieve the document from the database
			Document currentDoc = currentDb.getDocumentByUNID(strDocUNID);
			System.out.println("The database  retrieved " + strParentDB);

			templatesView = configDatabase.getView("($Templates)");
			Document templateDoc = templatesView.getDocumentByKey(currentDbConfigDoc.getItemValue("Template"));
			DocumentCollection forms = templateDoc.getResponses();
			// Read the template associated with this database
			// Get the form parameters document for the current document.
			Document formDoc = forms.getFirstDocument();
			boolean formDocFound = false;
			while (formDoc != null) {
				String strForm = formDoc.getItemValueString("LastAlias");
				String currForm = doc.getItemValueString("Form");
				if (strForm.equalsIgnoreCase(currForm)) {
					formDocFound = true;
					break;
				} else {
					formDoc = forms.getNextDocument();
				}
			}
			if (formDocFound) {
				// Retrieve the metadata from the template
				Vector<String> metaList = /*
										 * formDoc.getItems(); fieldsToIndex =
										 */formDoc.getItemValue("FieldsToIndex");

				if (metaList == null) {
					System.out.println("Error retrieving metadata list from the form document");
				} else {
					for (int i = 0; i < metaList.size(); i++) {
						String tmpMetaTag = metaList.get(i);
						System.out.println("The meta data value to be fetched "
								+ tmpMetaTag);
						if (currentDoc.hasItem(tmpMetaTag)) {
							doc.copyItem(currentDoc.getFirstItem(tmpMetaTag));
							// Copy the items tothe document stub one by one
							// according to theTemplate configured for the
							// database
							doc.save();
						} else {
							System.out.println("The metadata[" + tmpMetaTag
									+ "] not present in the document");
						}
					}
				}
				// get the metadata items from the document and put in the
				// document
				// stub
			}
		} catch (NotesException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * Extract the ACL information for the document and add it to the Doc Stub
	 * in the config Database
	 * 
	 * @param doc
	 */
	private void storeDocumentACL(Document doc) {

	}

	/**
	 * This method retrieves the attachments for the document and appends them
	 * as responses to the document stub
	 * 
	 * @param doc
	 */
	private void processAttachments(Document doc, Document docStub) {

	}

	private void setDocumentState(Document doc, String strStatus) {
		try {
			doc.appendItemValue(LotusConstants.CS_STATUS, strStatus);
			doc.save();
		} catch (NotesException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
