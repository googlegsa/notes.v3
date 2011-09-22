// Copyright 2011 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.enterprise.connector.notes;

import com.google.enterprise.connector.spi.SpiConstants.ActionType;
import lotus.domino.Database;
import lotus.domino.DateTime;
import lotus.domino.NotesError;
import lotus.domino.NotesException;
import lotus.domino.View;

import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;


// The purpose of this class is check for deletions of indexed documents
// Documents should be deleted if the meet either of the following criteria
// 1.  They no longer exist in the source database
// 2.  They belong to database which is marked for deletion
public class NotesMaintenanceThread extends Thread {
  private static final String CLASS_NAME = 
      NotesMaintenanceThread.class.getName();
  private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);
	
  NotesConnector nc = null;
  NotesConnectorSession ncs = null;
  Database cdb = null;
  NotesPollerNotifier npn = null;
  String OpenDbRepId = "";
  Database SrcDb = null;
  lotus.domino.Document DbConfigDoc = null;
  lotus.domino.Session ns = null;
  String DbConfigDocRepId = "";
  DateTime CheckTime = null;
  String IndexedDocRepId = "";
  lotus.domino.Document TemplateDoc = null;
  lotus.domino.Document SourceDocument = null;
  lotus.domino.Document IndexedDoc = null;
	
  NotesMaintenanceThread(NotesConnector Connector,
      NotesConnectorSession Session) {
    final String METHOD = "NotesMaintenanceThread";
    LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
        "NotesMaintenanceThread being created.");

    nc = Connector;
    ncs = Session;
  }
	
  @Override
  public void run() {
    final String METHOD = "run";
    LOGGER.entering(CLASS_NAME, METHOD);
		
    int exceptionCount = 0;
    int batchsize = ncs.getDeletionBatchSize();
    String lastdocid = "";
    NotesPollerNotifier npn = ncs.getNotifier();
    while (nc.getShutdown() == false) {
      try {
        LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
            "Maintenance thread checking for deletions.");
        checkForDeletions(lastdocid, batchsize);
        LOGGER.logp(Level.FINE, CLASS_NAME, METHOD, 
            "Maintenance thread sleeping after checking for deletions.");
        npn.waitForWork();
        LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
            "Maintenance thread resuming to check for deletions.");
      } catch (Exception e) {
        LOGGER.log(Level.SEVERE, CLASS_NAME, e);
        // Lets say the server we are connected to goes down
        // while we are crawling We don't want to fill up the
        // logs with errors so go to sleep after 5 exceptions
        exceptionCount++;
        if (exceptionCount > 5) {
          LOGGER.logp(Level.WARNING, CLASS_NAME, METHOD, 
              "Too many exceptions.  Maintenance thread sleeping.");
          npn.waitForWork();
          LOGGER.logp(Level.WARNING, CLASS_NAME, METHOD, 
              "Maintenance thread resuming after too many exceptions " +
              "were encountered.");
        }
      }
    }
    LOGGER.logp(Level.INFO, CLASS_NAME, METHOD,
        "Maintenance thread exiting after connector shutdown.");
    LOGGER.exiting(CLASS_NAME, METHOD);
  }
	
  /*
   * Checks for documents which have been deleted in the INDEXED view
   * startdocid - id to start checking from
   * batchsize - number of documents to check in this batch
   * return value - id of the last document checked
   */
  protected String checkForDeletions(String startdocid, int batchsize) {
    final String METHOD = "checkForDeletions";
    LOGGER.entering(CLASS_NAME, METHOD);

    int NumChecked = 0;
    String lastdocid = "";
    lotus.domino.Document PrevDoc = null;

    try {
      LOGGER.logp(Level.INFO, CLASS_NAME, METHOD, "Checking for deletions "); 
      ns = ncs.createNotesSession();
      CheckTime = ns.createDateTime("1/1/1900");
      CheckTime.setNow();
      cdb = ns.getDatabase(ncs.getServer(), ncs.getDatabase());
      View DatabaseView = cdb.getView(NCCONST.VIEWDATABASES);
			
      DatabaseView.refresh();
      LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
          "MaintenanceThread: Entries in database view: " +
          DatabaseView.getEntryCount());
			
      View IndexedView = cdb.getView(NCCONST.VIEWINDEXED);
      IndexedView.refresh();
      LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
          "MaintenanceThread: Entries in indexed view: " +
          IndexedView.getEntryCount());

      IndexedDoc = IndexedView.getDocumentByKey(startdocid);
      if (null == IndexedDoc) {
        LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
            "MaintenanceThread: Restarting deletion check.");
        IndexedDoc = IndexedView.getFirstDocument();
      }
      while ((null != IndexedDoc) && 
          (NumChecked < batchsize) && 
          (!nc.getShutdown())) {
        NumChecked++;
        IndexedView.refresh();
        String DocId = IndexedDoc.getItemValueString(NCCONST.ITM_DOCID);
        LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
            "MaintenanceThread: Checking deletion for document:  " + DocId);
				
        String State = IndexedDoc.getItemValueString(NCCONST.NCITM_STATE);
        if (!State.equalsIgnoreCase(NCCONST.STATEINDEXED)) {
          LOGGER.logp(Level.SEVERE, CLASS_NAME, METHOD, 
              "MaintenanceThread: Skipping deletion check since state " +
              "is not indexed." + DocId);
          PrevDoc = IndexedDoc;
          IndexedDoc = IndexedView.getNextDocument(PrevDoc);
          PrevDoc.recycle();
          continue;
        }

        loadDbConfigDoc(IndexedDoc.getItemValueString(NCCONST.NCITM_REPLICAID),
            DatabaseView);
        if (null == DbConfigDoc) {
          LOGGER.logp(Level.SEVERE, CLASS_NAME, METHOD,
              "MaintenanceThread: Skipping document because no " +
              "database config found for " + DocId);
          PrevDoc = IndexedDoc;
          IndexedDoc = IndexedView.getNextDocument(PrevDoc);
          PrevDoc.recycle();
          continue;
        }
				
        // When a database is in stopped mode we purge all documents
        if (getStopped()) {
          LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
              "MaintenanceThread: Deleting document because database " +
              "is being purged. " + DocId);
          createDeleteRequest(IndexedDoc,
              IndexedDoc.getItemValueString(NCCONST.ITM_DOCID));
          PrevDoc = IndexedDoc;
          IndexedDoc = IndexedView.getNextDocument(PrevDoc);
          PrevDoc.recycle();
          continue;
        }
				
        // Is this database configured to check for deletions?
        String checkDeletions = DbConfigDoc.getItemValueString(
            NCCONST.DITM_CHECKDELETIONS);
        if (checkDeletions.toLowerCase().contentEquals("no")) {
          LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
              "MaintenanceThread: Skipping document because " +
              "deletion checking is not enabled. " + DocId);
          PrevDoc = IndexedDoc;
          IndexedDoc = IndexedView.getNextDocument(PrevDoc);
          PrevDoc.recycle();
          continue;					
        }
				
        // Is crawling enabled for this database?  If not then
        // skip to the next document
        int isEnabled = DbConfigDoc.getItemValueInteger(
            NCCONST.DITM_CRAWLENABLED);
        if (isEnabled != 1) {
          LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
              "MaintenanceThread: Skipping document because " +
              "database crawling is disabled. " + DocId);
          PrevDoc = IndexedDoc;
          IndexedDoc = IndexedView.getNextDocument(PrevDoc);
          PrevDoc.recycle();
          continue;					
        }
				
        // Try and open the source database
        boolean SrcDbOpened = openSourceDatabase(IndexedDoc);
        if (!SrcDbOpened) {
          LOGGER.logp(Level.SEVERE, CLASS_NAME, METHOD,
              "MaintenanceThread: Skipping document because source " +
              "database could not be opened : " + DocId);
          PrevDoc = IndexedDoc;
          IndexedDoc = IndexedView.getNextDocument(PrevDoc);
          PrevDoc.recycle();
          continue; 
        }
				
        boolean DocDeleted = loadSourceDocument(
            IndexedDoc.getItemValueString(NCCONST.NCITM_UNID));
        if (DocDeleted) {
          createDeleteRequest(IndexedDoc, 
              IndexedDoc.getItemValueString(NCCONST.ITM_DOCID));
          PrevDoc = IndexedDoc;
          IndexedDoc = IndexedView.getNextDocument(PrevDoc);
          PrevDoc.recycle();
          continue;
        }
				
        boolean isConflict = SourceDocument.hasItem(NCCONST.NCITM_CONFLICT);
        if (isConflict) {
          createDeleteRequest(IndexedDoc, 
              IndexedDoc.getItemValueString(NCCONST.ITM_DOCID));
          PrevDoc = IndexedDoc;
          IndexedDoc = IndexedView.getNextDocument(PrevDoc);
          PrevDoc.recycle();
          continue;				
        }
				
        loadTemplateDoc(DbConfigDoc.getItemValueString(NCCONST.DITM_TEMPLATE));
        if (null == TemplateDoc) {
          LOGGER.logp(Level.SEVERE, CLASS_NAME, METHOD,
              "MaintenanceThread: Skipping selection criteria " +
              "check because template could not be opened : " + DocId);
          PrevDoc = IndexedDoc;
          IndexedDoc = IndexedView.getNextDocument(PrevDoc);
          PrevDoc.recycle();
          continue; 
        }
				
        boolean meetsCriteria = checkSelectionCriteria();
        if (!meetsCriteria) {
          LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
              "MaintenanceThread: Deleting document because " +
              "selection formula returned false : " + DocId);
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
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, CLASS_NAME, e); 
    } finally {
      cleanUpNotesObjects();
      ncs.closeNotesSession(ns);
      LOGGER.exiting(CLASS_NAME, METHOD);
    }
    return lastdocid;
  }
	
  protected void cleanUpNotesObjects() {
    final String METHOD = "cleanUpNotesObjects";
    LOGGER.entering(CLASS_NAME, METHOD);
    try {
      if (null != CheckTime) {
        CheckTime.recycle();
      }
      CheckTime = null;
      if (null != SourceDocument) {
        SourceDocument.recycle();
      }
      SourceDocument = null;
      if (null != TemplateDoc) {
        TemplateDoc.recycle();
      }
      TemplateDoc = null;
      if (null != IndexedDoc) {
        IndexedDoc.recycle();
      }
      IndexedDoc = null;
      DbConfigDocRepId = "";
      if (null != DbConfigDoc) {
        DbConfigDoc.recycle();
      }
      DbConfigDoc = null;
      OpenDbRepId ="";
      if (null != SrcDb) {
        SrcDb.recycle();
      }
      SrcDb=null;
			
      if (null != cdb) {
        cdb.recycle();
      }
      cdb = null;
    } catch (NotesException e) {
      // TODO: changed log level to WARNING. Can an exception
      // here be SEVERE?
      LOGGER.log(Level.WARNING, CLASS_NAME, e); 
    }
    LOGGER.exiting(CLASS_NAME, METHOD);
  }

  /*
   *   Only start purging documents after a little while
   *   
   */
  protected boolean getStopped() throws NotesException {
    final String METHOD = "getStopped";
		
    // TODO:  Think about adding variable to provide some grace time
    /*
      LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD, 
          "CheckTime is: " + CheckTime);
      DateTime lm = DbConfigDoc.getLastModified();
      LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD, 
          "Last Modified is: " + lm);

      int timediff = CheckTime.timeDifference(lm);
      LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD, 
          "Time Diff is: " + timediff);
      if (CheckTime.timeDifference(lm) < 300) {
        return false;
      }
    */
    if (1 == DbConfigDoc.getItemValueInteger(NCCONST.DITM_STOPPED)) {
      return true;
    }
    return false; 
  }

  protected boolean openSourceDatabase(lotus.domino.Document IndexedDoc)
      throws NotesException {
    final String METHOD = "openSourceDatabase";
    LOGGER.entering(CLASS_NAME, METHOD);
		
    String ReplicaId = IndexedDoc.getItemValueString(NCCONST.NCITM_REPLICAID);
    if (OpenDbRepId.contentEquals(ReplicaId)) {
      return true;
    }
    // Different ReplicaId - Recycle and close the old database
    if (SrcDb != null) {
      SrcDb.recycle();
      SrcDb= null;
      OpenDbRepId = "";
    }
    // Open the new database
    SrcDb = ns.getDatabase(null, null); 
    boolean srcdbisopen = SrcDb.openByReplicaID(
        IndexedDoc.getItemValueString(NCCONST.NCITM_SERVER), ReplicaId);
    if (srcdbisopen) {
      OpenDbRepId = ReplicaId;
    } else {
      LOGGER.logp(Level.SEVERE, CLASS_NAME, METHOD, 
          "Maintenance thread can't open database: " + ReplicaId);
    }
    LOGGER.exiting(CLASS_NAME, METHOD);
    return srcdbisopen;
  }

  protected void loadTemplateDoc(String TemplateName) throws NotesException {
    final String METHOD = "loadTemplateDoc";
    LOGGER.entering(CLASS_NAME, METHOD);
    LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
        "Loading template: " + TemplateName);
    // Is a template document all ready loaded?
    if (null != TemplateDoc) {
      // Is this the one we need?  
      String existingTemplate = TemplateDoc.getItemValueString(
          NCCONST.TITM_TEMPLATENAME); 
      LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
          "Existing template is: " + TemplateName);
      if (TemplateName.equals(existingTemplate)) {
        return;
      }
      TemplateDoc.recycle();
      TemplateDoc = null;
    }
    View vw = cdb.getView(NCCONST.VIEWTEMPLATES);
    TemplateDoc = vw.getDocumentByKey(TemplateName, true);
    vw.recycle();
    if (null != TemplateDoc) {
      LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
          "Loaded template: " + 
          TemplateDoc.getItemValueString(NCCONST.TITM_TEMPLATENAME));
    }
    LOGGER.exiting(CLASS_NAME, METHOD);
  }
	
  protected void loadDbConfigDoc(String ReplicaId, View DatabaseView) 
      throws NotesException {
    final String METHOD = "loadDbConfigDoc";
    LOGGER.entering(CLASS_NAME, METHOD);
    if (ReplicaId.contentEquals(DbConfigDocRepId)) {
      return;
    }
    if (DbConfigDoc != null) {
      DbConfigDoc.recycle();
      DbConfigDoc = null;
      DbConfigDocRepId = "";
    }
    DbConfigDoc = DatabaseView.getDocumentByKey(ReplicaId);
    if (null == DbConfigDoc) {
      LOGGER.logp(Level.SEVERE, CLASS_NAME, METHOD, 
          "Maintenance thread can't find database config for replica : " +
          ReplicaId);
      return;
    }
    DbConfigDocRepId = ReplicaId;
    LOGGER.exiting(CLASS_NAME, METHOD);
    return;
  }
	
  @SuppressWarnings("unchecked")
  protected boolean checkSelectionCriteria() throws NotesException {
    final String METHOD = "checkSelectionCriteria";
    LOGGER.entering(CLASS_NAME, METHOD);

    String SelectionFormula = TemplateDoc.getItemValueString(
        NCCONST.TITM_SEARCHSTRING);
    LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
        "Using selection formula: " + SelectionFormula);

    Vector<Double> VecEvalResult =  (Vector<Double>) 
        ns.evaluate(SelectionFormula, SourceDocument);
    // A Selection formula will return a vector of doubles.  
    if (1 == VecEvalResult.elementAt(0)) {
      LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD, 
          "Selection formula returned true");
      LOGGER.exiting(CLASS_NAME, METHOD);
      return true;
    }
    LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD, 
        "Selection formula returned false");
    LOGGER.exiting(CLASS_NAME, METHOD);
    return false;
  }
	
  /*
   * Check to see if the source document still exists in the source database
   */
  protected boolean loadSourceDocument(String UNID) throws NotesException {
    final String METHOD = "loadSourceDocument";
    LOGGER.entering(CLASS_NAME, METHOD);
		
    try {
      // See if we can get the document
      if (null != SourceDocument) {
        SourceDocument.recycle();
        SourceDocument = null;
      }
      SourceDocument = SrcDb.getDocumentByUNID(UNID);
    } catch (NotesException e) {
      if (e.id == NotesError.NOTES_ERR_BAD_UNID) {
        LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
            "Document has been deleted " + UNID);
        LOGGER.exiting(CLASS_NAME, METHOD);
        return true;
      }
    }
    LOGGER.exiting(CLASS_NAME, METHOD);
    return false;
  }

  /*
   * Create a request to delete this document
   */
  protected void createDeleteRequest(lotus.domino.Document IndexedDoc,
      String DocId) throws NotesException {
    final String METHOD = "createDeleteRequest";
    LOGGER.entering(CLASS_NAME, METHOD);
    lotus.domino.Document DeleteReq = cdb.createDocument();
    DeleteReq.appendItemValue(NCCONST.ITMFORM, NCCONST.FORMCRAWLREQUEST);
    DeleteReq.replaceItemValue(NCCONST.ITM_ACTION, ActionType.DELETE.toString());
    DeleteReq.replaceItemValue(NCCONST.ITM_DOCID, DocId);
    DeleteReq.replaceItemValue(NCCONST.NCITM_STATE, NCCONST.STATEFETCHED);
    DeleteReq.save(true);
    IndexedDoc.replaceItemValue(NCCONST.NCITM_STATE, NCCONST.STATEDELETED);
    IndexedDoc.save(true);
    LOGGER.exiting(CLASS_NAME, METHOD);
  }
}
