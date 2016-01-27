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

import com.google.common.base.Strings;
import com.google.enterprise.connector.logging.NDC;
import com.google.enterprise.connector.notes.client.NotesDatabase;
import com.google.enterprise.connector.notes.client.NotesDateTime;
import com.google.enterprise.connector.notes.client.NotesDocument;
import com.google.enterprise.connector.notes.client.NotesError;
import com.google.enterprise.connector.notes.client.NotesSession;
import com.google.enterprise.connector.notes.client.NotesView;
import com.google.enterprise.connector.spi.RepositoryException;
import com.google.enterprise.connector.spi.SpiConstants.ActionType;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class checks for deletions of indexed documents and
 * updates the user and group cache.
 *
 * Documents should be deleted if they meet either of the
 * following criteria.
 * 1.  They no longer exist in the source database.
 * 2.  They belong to a database which is marked for deletion.
 */
public class NotesMaintenanceThread extends Thread {
  private static final String CLASS_NAME =
      NotesMaintenanceThread.class.getName();
  private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

  NotesConnector nc = null;
  NotesConnectorSession ncs;
  NotesUserGroupManager nugm;
  NotesDatabase cdb = null;
  NotesPollerNotifier npn = null;
  String OpenDbRepId = "";
  NotesDatabase SrcDb = null;
  NotesDocument DbConfigDoc = null;
  NotesSession ns = null;
  String DbConfigDocRepId = "";
  NotesDateTime CheckTime = null;
  String IndexedDocRepId = "";
  NotesDocument TemplateDoc = null;
  NotesDocument SourceDocument = null;
  NotesDocument IndexedDoc = null;

  NotesMaintenanceThread() {
  }

  NotesMaintenanceThread(NotesConnector connector,
      NotesConnectorSession session) throws RepositoryException {
    final String METHOD = "NotesMaintenanceThread";
    LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
        "NotesMaintenanceThread being created.");

    nc = connector;
    ncs = session;
    if (session != null) {
      nugm = session.getUserGroupManager();
    }
  }

  @Override
  public void run() {
    NDC.push("Maintenance " + nc.getGoogleConnectorName());
    final String METHOD = "run";
    LOGGER.entering(CLASS_NAME, METHOD);

    int exceptionCount = 0;
    int batchsize = ncs.getDeletionBatchSize();
    String lastdocid = "";
    NotesPollerNotifier npn = ncs.getNotifier();
    while (nc.getShutdown() == false) {
      try {
        LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
            "Maintenance thread is updating User Group Cache.");
        nugm.updateUsersGroups();
        LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
            "Maintenance thread checking for deletions [Batch Size: " + 
            batchsize + "]");
        lastdocid = checkForDeletions(lastdocid, batchsize);
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

    String lastdocid = startdocid;
    Map<String,NotesDocId> indexedDocuments = null;
    try {
      LOGGER.logp(Level.INFO, CLASS_NAME, METHOD, "Checking for deletions ");
      ns = ncs.createNotesSession();
      CheckTime = ns.createDateTime("1/1/1900");
      CheckTime.setNow();
      cdb = ns.getDatabase(ncs.getServer(), ncs.getDatabase());
      NotesView DatabaseView = cdb.getView(NCCONST.VIEWDATABASES);
      DatabaseView.refresh();
      LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
          "MaintenanceThread: Entries in database view: " +
          DatabaseView.getEntryCount());
      
      if (Strings.isNullOrEmpty(startdocid)) {
        indexedDocuments = ncs.getNotesDocumentManager()
            .getIndexedDocuments(null, null, batchsize);
        LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
            "MaintenanceThread: Restarting deletion check.");
      } else {
        NotesDocId startNotesId = new NotesDocId(startdocid);
        indexedDocuments = ncs.getNotesDocumentManager()
            .getIndexedDocuments(startNotesId.getDocId(), 
                startNotesId.getReplicaId(), batchsize);

        if (!indexedDocuments.containsKey(startNotesId.getDocId())) {
          LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
              "MaintenanceThread: Restarting deletion check.");
        }
      }
      for (Map.Entry<String, NotesDocId> entry : indexedDocuments.entrySet()) {
        if (nc.getShutdown()) {
          break;
        }
        NotesDocId notesId = entry.getValue();
        LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
            "MaintenanceThread: Checking deletion for document: " + notesId);
        try {
          lastdocid = notesId.toString();
          //Validate database config using replica ID
          loadDbConfigDoc(notesId.getReplicaId(), DatabaseView);
          if (DbConfigDoc == null) {
            LOGGER.logp(Level.SEVERE, CLASS_NAME, METHOD,
                "MaintenanceThread: Skipping document because no " +
                "database config found for " + notesId);
            continue;
          }
          //When a database is in stopped mode we purge all documents
          if (getStopped()) {
            LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
                "MaintenanceThread: Deleting document because database " +
                "is being purged. " + notesId);
            sendDeleteRequest(notesId);
            continue;
          }
          //Is this database configured to check for deletions?
          String checkDeletions = DbConfigDoc.getItemValueString(
              NCCONST.DITM_CHECKDELETIONS);
          if (checkDeletions.toLowerCase().contentEquals("no")) {
            LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
                "MaintenanceThread: Skipping document because " +
                "deletion checking is not enabled. " + notesId);
            continue;
          }
          //Is crawling enabled for this database?  If not then
          //skip to the next document
          int isEnabled = DbConfigDoc.getItemValueInteger(
              NCCONST.DITM_CRAWLENABLED);
          if (isEnabled != 1) {
            LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
                "MaintenanceThread: Skipping document because " +
                "database crawling is disabled. " + notesId);
            continue;
          }
          //Try and open the source database
          boolean isSrcDbOpened = openSourceDatabase(notesId);
          if (!isSrcDbOpened) {
            LOGGER.logp(Level.SEVERE, CLASS_NAME, METHOD,
                "MaintenanceThread: Skipping document because source " +
                "database could not be opened: " + notesId);
            continue;
          }

          boolean isDocDeleted = loadSourceDocument(entry.getKey());
          if (isDocDeleted) {
            sendDeleteRequest(notesId);
            continue;
          }

          boolean isConflict = SourceDocument.hasItem(NCCONST.NCITM_CONFLICT);
          if (isConflict) {
            sendDeleteRequest(notesId);
            continue;
          }

          String templateName =
              DbConfigDoc.getItemValueString(NCCONST.DITM_TEMPLATE);
          loadTemplateDoc(templateName);
          if (null == TemplateDoc) {
            LOGGER.logp(Level.SEVERE, CLASS_NAME, METHOD,
                "MaintenanceThread: Skipping selection criteria check " +
                "because template could not be opened: " + notesId +
                ", Template: "+ templateName + ", Database: " +
                notesId.getServer() + "!!" + SrcDb.getFilePath());
            continue;
          }

          boolean meetsCriteria = checkSelectionCriteria();
          if (!meetsCriteria) {
            LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
                "MaintenanceThread: Deleting document because " +
                "selection formula returned false: " + notesId +
                ", Database: " + notesId.getServer() + "!!" +
                SrcDb.getFilePath());
            sendDeleteRequest(notesId);
            continue;
          }
        } catch (RepositoryException e) {
          LOGGER.logp(Level.WARNING, CLASS_NAME, METHOD,
              "Unable to process document: " + notesId, e);
          // Skip current UNID and process next.
          continue;
        }
      }
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, CLASS_NAME, e);
    } finally {
      if (indexedDocuments != null) {
        indexedDocuments.clear();
      }
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
      OpenDbRepId = "";
      if (null != SrcDb) {
        SrcDb.recycle();
      }
      SrcDb = null;

      if (null != cdb) {
        cdb.recycle();
      }
      cdb = null;
    } catch (RepositoryException e) {
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
  protected boolean getStopped() throws RepositoryException {
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

  private boolean openSourceDatabase(NotesDocId notesId)
      throws RepositoryException {
    final String METHOD = "openSourceDatabase";
    LOGGER.entering(CLASS_NAME, METHOD);
    if (OpenDbRepId.contentEquals(notesId.getReplicaId())) {
      return true;
    }
    //Different replicaId - Recycle and close the old database
    if (SrcDb != null) {
      SrcDb.recycle();
      SrcDb= null;
      OpenDbRepId = "";
    }
    // Open the new database
    SrcDb = ns.getDatabase(null, null);
    boolean isSrcDbOpen = SrcDb.openByReplicaID(notesId.getServer(), 
        notesId.getReplicaId());
    if (isSrcDbOpen) {
      OpenDbRepId = notesId.getReplicaId();
    } else {
      LOGGER.logp(Level.SEVERE, CLASS_NAME, METHOD,
          "Maintenance thread can't open database: " + notesId.getServer()
          + "!!" + notesId.getReplicaId());
    }
    LOGGER.exiting(CLASS_NAME, METHOD);
    return isSrcDbOpen;
  }

  protected void loadTemplateDoc(String TemplateName)
      throws RepositoryException {
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
    NotesView vw = cdb.getView(NCCONST.VIEWTEMPLATES);
    TemplateDoc = vw.getDocumentByKey(TemplateName, true);
    vw.recycle();
    if (null != TemplateDoc) {
      LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
          "Loaded template: " +
          TemplateDoc.getItemValueString(NCCONST.TITM_TEMPLATENAME));
    }
    LOGGER.exiting(CLASS_NAME, METHOD);
  }

  protected void loadDbConfigDoc(String ReplicaId, NotesView DatabaseView)
      throws RepositoryException {
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
  protected boolean checkSelectionCriteria() throws RepositoryException {
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
  protected boolean loadSourceDocument(String UNID)
      throws RepositoryException {
    final String METHOD = "loadSourceDocument";
    LOGGER.entering(CLASS_NAME, METHOD);

    try {
      // See if we can get the document
      if (null != SourceDocument) {
        SourceDocument.recycle();
        SourceDocument = null;
      }
      SourceDocument = SrcDb.getDocumentByUNID(UNID);
    } catch (NotesConnectorException e) {
      if (e.getId() == NotesError.NOTES_ERR_BAD_UNID) {
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
  private void createDeleteRequest(String googleDocId) throws RepositoryException {
    final String METHOD = "createDeleteRequest";
    LOGGER.entering(CLASS_NAME, METHOD);
    LOGGER.logp(Level.FINER, CLASS_NAME, METHOD, 
        "Send deletion request to GSA for " + googleDocId);
    NotesDocument DeleteReq = cdb.createDocument();
    DeleteReq.appendItemValue(NCCONST.ITMFORM, NCCONST.FORMCRAWLREQUEST);
    DeleteReq.replaceItemValue(NCCONST.ITM_ACTION, ActionType.DELETE.toString());
    DeleteReq.replaceItemValue(NCCONST.ITM_DOCID, googleDocId);
    DeleteReq.replaceItemValue(NCCONST.NCITM_STATE, NCCONST.STATEFETCHED);
    DeleteReq.save(true);
    DeleteReq.recycle();
    LOGGER.exiting(CLASS_NAME, METHOD);
  }

  private void createDeleteRequestForAttachments(NotesDocId notesId)
      throws RepositoryException {
    NotesDocumentManager docMgr = ncs.getNotesDocumentManager();
    Connection conn = null;
    try {
      conn = docMgr.getDatabaseConnection();
      Set<String> attachmentSet = docMgr.getAttachmentIds(conn,
          notesId.getDocId(), notesId.getReplicaId());
      for (String attachmentId : attachmentSet) {
        String attachmentUrl = String.format(NCCONST.SITM_ATTACHMENTDOCID,
            notesId.toString(), attachmentId);
        try {
          createDeleteRequest(attachmentUrl);
        } catch (RepositoryException re) {
          LOGGER.log(Level.WARNING, "Failed to create delete request for "
              + attachmentUrl, re);
        }
      }
    } catch (SQLException e) {
      LOGGER.log(Level.SEVERE, "Unable to connect to H2 database", e);
    } finally {
      if (conn != null) {
        docMgr.releaseDatabaseConnection(conn);
      }
    }
  }

  private void sendDeleteRequest(NotesDocId notesId)
      throws RepositoryException {
    createDeleteRequestForAttachments(notesId);
    createDeleteRequest(notesId.toString());
  }
}
