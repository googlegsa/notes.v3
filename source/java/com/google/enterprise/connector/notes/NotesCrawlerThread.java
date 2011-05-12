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

package com.google.enterprise.connector.notes;

import lotus.domino.Document;
import lotus.domino.NotesThread;
import lotus.domino.Session;



/**
 * @author deepti_nagarkar
 * Apr 22, 2011
 * 2011
 */
public class NotesCrawlerThread extends NotesThread {

	private Session session; 
	//This is the name of config database in which the document stubs and details will be stored
	private static String strConfigDB="google-connector.nsf";
	
	  public void runNotes() 
	    { 
	    
	    } 

	
	
	/**
	 * Gets the next document to be processed frmm the 
	 * Traversal Queue
	 * @return
	 */
	public synchronized static Document getNextFromQueue(Session objSession) {
		Document objDoc = null ;
		return objDoc;
	}

	/**
	 * Retrieves the metadata , ACL information and content
	 * for the given document and adds it to the config database
	 * Also changes the state of the received doc to Incrawl 
	 * and after processing to Fetched.
	 * Also retrieves the attachments and creates a separate doc
	 * for each one in the config database view
	 * @param objDocToCrawl
	 */
	public void crawlDocument(Document objDocToCrawl, String strParentDb ) {
		
	}

	/**
	 * This method applies the Metadata template to the Document sent 
	 * It reads the template from the Config database and retrieves the fields
	 * and other details accordingly
	 * @param doc
	 */
	private void transformMetadata(Document doc) {
		
	}
	
	/**
	 * Extract the ACL information for the document and add it to the Doc
	 * Stub in the config Database 
	 * @param doc
	 */
	private void storeDocumentACL(Document doc) {
		
		
	}

	private void processAttachments(Document doc) {
		
	}

	private void setDocumentState(Document doc , String strState) {
		
	}
}
