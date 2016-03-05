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
class NotesMaintenanceThread extends Thread {
  private static final String CLASS_NAME =
      NotesMaintenanceThread.class.getName();
  private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

  private final NotesConnector nc;
  private final NotesConnectorSession ncs;
  private final NotesUserGroupManager nugm;

  NotesMaintenanceThread(NotesConnector connector,
      NotesConnectorSession session) throws RepositoryException {
    LOGGER.log(Level.FINEST, "NotesMaintenanceThread being created.");

    nc = connector;
    ncs = session;
    nugm = (session == null) ? null : session.getUserGroupManager();
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
        LOGGER.log(Level.FINE,
            "Maintenance thread is updating User Group Cache.");
        nugm.updateUsersGroups();
        LOGGER.log(Level.FINE,
            "Maintenance thread checking for deletions [Batch Size: {0}]",
            batchsize);
        lastdocid = checkForDeletions(lastdocid, batchsize);
        LOGGER.log(Level.FINE,
            "Maintenance thread sleeping after checking for deletions.");
        npn.waitForWork();
        LOGGER.log(Level.FINE,
            "Maintenance thread resuming to check for deletions.");
      } catch (Exception e) {
        LOGGER.log(Level.SEVERE, CLASS_NAME, e);
        // Lets say the server we are connected to goes down
        // while we are crawling We don't want to fill up the
        // logs with errors so go to sleep after 5 exceptions
        exceptionCount++;
        if (exceptionCount > 5) {
          LOGGER.log(Level.WARNING,
              "Too many exceptions. Maintenance thread sleeping.");
          npn.waitForWork();
          LOGGER.log(Level.WARNING,
              "Maintenance thread resuming after too many exceptions " +
              "were encountered.");
        }
      }
    }
    LOGGER.log(Level.INFO,
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
    NotesSession ns = null;
    NotesDatabase cdb = null;
    DeletionHandler handler = null;
    NotesDocId notesId = null;
    try {
      LOGGER.log(Level.INFO, "Checking for deletions");
      ns = ncs.createNotesSession();
      // TODO(jlacey): See getStopped.
      // CheckTime = ns.createDateTime("1/1/1900");
      // CheckTime.setNow();
      cdb = ns.getDatabase(ncs.getServer(), ncs.getDatabase());
      NotesView DatabaseView = cdb.getView(NCCONST.VIEWDATABASES);
      DatabaseView.refresh();
      LOGGER.log(Level.FINE, "Entries in database view: {0}",
          DatabaseView.getEntryCount());

      NotesDocumentManager docMgr = ncs.getNotesDocumentManager();
      handler = new DeletionHandler(docMgr, ns, cdb, DatabaseView);
      Map<String, NotesDocId> indexedDocuments;
      if (Strings.isNullOrEmpty(startdocid)) {
        indexedDocuments = docMgr.getIndexedDocuments(null, null, batchsize);
        LOGGER.log(Level.FINE, "Restarting deletion check.");
      } else {
        NotesDocId startNotesId = new NotesDocId(startdocid);
        indexedDocuments = docMgr.getIndexedDocuments(startNotesId.getDocId(),
            startNotesId.getReplicaId(), batchsize);

        if (!indexedDocuments.containsKey(startNotesId.getDocId())) {
          LOGGER.log(Level.FINE, "Restarting deletion check.");
        }
      }
      for (Map.Entry<String, NotesDocId> entry : indexedDocuments.entrySet()) {
        if (nc.getShutdown()) {
          break;
        }
        String unid = entry.getKey();
        notesId = entry.getValue();
        LOGGER.log(Level.FINER, "Checking deletion for document: {0}", notesId);
        try {
          lastdocid = notesId.toString();
          handler.checkForDeletion(unid, notesId);
        } catch (RepositoryException e) {
          LOGGER.log(Level.WARNING,
              "Unable to process document: " + notesId, e);
          // Skip current UNID and process next.
        }
      }
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE,
          "Aborting check for deletions at document: " + notesId, e);
    } finally {
      Util.recycle(cdb);
      if (handler != null) {
        handler.recycleAll();
      }

      ncs.closeNotesSession(ns);
      LOGGER.exiting(CLASS_NAME, METHOD);
    }
    return lastdocid;
  }

  private static class DeletionHandler {
    private final NotesDocumentManager docMgr;
    private final NotesSession ns;
    private final NotesDatabase cdb;
    private final NotesView DatabaseView;

    private NotesDocument DbConfigDoc = null;
    private String DbConfigDocRepId = "";
    private NotesDatabase SrcDb = null;
    private String OpenDbRepId = "";
    private NotesDocument TemplateDoc = null;
    private NotesDocument SourceDocument = null;

    public DeletionHandler(NotesDocumentManager docMgr, NotesSession ns,
        NotesDatabase cdb, NotesView DatabaseView) {
      this.docMgr = docMgr;
      this.ns = ns;
      this.cdb = cdb;
      this.DatabaseView = DatabaseView;
    }

    public void recycleAll() {
      Util.recycle(DbConfigDoc, SrcDb, TemplateDoc, SourceDocument);
    }

    public void checkForDeletion(String unid, NotesDocId notesId)
        throws RepositoryException {
      //Validate database config using replica ID
      loadDbConfigDoc(notesId.getReplicaId(), DatabaseView);
      if (DbConfigDoc == null) {
        LOGGER.log(Level.SEVERE,
            "Skipping document because no database config found for {0}",
            notesId);
        return;
      }
      //When a database is in stopped mode we purge all documents
      if (getStopped()) {
        LOGGER.log(Level.FINER,
            "Deleting document because database is being purged. {0}", notesId);
        sendDeleteRequest(notesId);
        return;
      }
      //Is this database configured to check for deletions?
      String checkDeletions = DbConfigDoc.getItemValueString(
          NCCONST.DITM_CHECKDELETIONS);
      if (checkDeletions.toLowerCase().contentEquals("no")) {
        LOGGER.log(Level.FINER,
            "Skipping document because deletion checking is disabled. {0}",
            notesId);
        return;
      }
      //Is crawling enabled for this database?  If not then
      //skip to the next document
      int isEnabled = DbConfigDoc.getItemValueInteger(
          NCCONST.DITM_CRAWLENABLED);
      if (isEnabled != 1) {
        LOGGER.log(Level.FINER,
            "Skipping document because database crawling is disabled. {0}",
            notesId);
        return;
      }
      //Try and open the source database
      boolean isSrcDbOpened = openSourceDatabase(notesId);
      if (!isSrcDbOpened) {
        LOGGER.log(Level.SEVERE, "Skipping document because source "
            + "database could not be opened: {0}!!{1}",
            new Object[] { notesId.getServer(), notesId.getReplicaId() });
        return;
      }

      boolean isDocDeleted = loadSourceDocument(unid);
      if (isDocDeleted) {
        LOGGER.log(Level.FINEST, "Document has been deleted: {0}, UNID {1}",
            new Object[] { notesId, unid });
        sendDeleteRequest(notesId);
        return;
      }

      boolean isConflict = SourceDocument.hasItem(NCCONST.NCITM_CONFLICT);
      if (isConflict) {
        LOGGER.log(Level.FINEST,
            "Deleting document due to conflict: {0}", notesId);
        sendDeleteRequest(notesId);
        return;
      }

      String templateName =
          DbConfigDoc.getItemValueString(NCCONST.DITM_TEMPLATE);
      loadTemplateDoc(templateName);
      if (null == TemplateDoc) {
        // The tests check this, so avoid MessageFormat-style.
        LOGGER.log(Level.SEVERE, "Skipping selection criteria check " +
            "because template could not be opened: " + notesId +
            ", Template: "+ templateName + ", Database: " +
            notesId.getServer() + "!!" + SrcDb.getFilePath());
        return;
      }

      boolean meetsCriteria = checkSelectionCriteria();
      if (!meetsCriteria) {
        LOGGER.log(Level.FINER, "Deleting document because "
            + "selection formula returned false: {0}, Database: {1}!!{2}",
            new Object[] { notesId, notesId.getServer(), SrcDb.getFilePath() });
        sendDeleteRequest(notesId);
      }
    }

    /*
     *   Only start purging documents after a little while
     *
     */
    protected boolean getStopped() throws RepositoryException {
      // TODO:  Think about adding variable to provide some grace time
      /*
        LOGGER.log(Level.FINEST, "CheckTime is: {0}", CheckTime);
        DateTime lm = DbConfigDoc.getLastModified();
        LOGGER.log(Level.FINEST, "Last Modified is: {0}", lm);

        int timediff = CheckTime.timeDifference(lm);
        LOGGER.log(Level.FINEST, "Time Diff is: {0}", timediff);
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
      }
      LOGGER.exiting(CLASS_NAME, METHOD);
      return isSrcDbOpen;
    }

    protected void loadTemplateDoc(String TemplateName)
        throws RepositoryException {
      final String METHOD = "loadTemplateDoc";
      LOGGER.entering(CLASS_NAME, METHOD);
      LOGGER.log(Level.FINEST, "Loading template: {0}", TemplateName);
      // Is a template document all ready loaded?
      if (null != TemplateDoc) {
        // Is this the one we need?
        String existingTemplate = TemplateDoc.getItemValueString(
            NCCONST.TITM_TEMPLATENAME);
        LOGGER.log(Level.FINEST, "Existing template is: {0}", TemplateName);
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
        LOGGER.log(Level.FINEST, "Loaded template: {0}",
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
        LOGGER.log(Level.SEVERE,
            "Maintenance thread can't find database config for replica: {0}",
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
      // The tests check this, so avoid MessageFormat-style.
      LOGGER.log(Level.FINEST, "Using selection formula: " + SelectionFormula);

      Vector<Double> VecEvalResult =  (Vector<Double>)
          ns.evaluate(SelectionFormula, SourceDocument);
      // A Selection formula will return a vector of doubles.
      if (1 == VecEvalResult.elementAt(0)) {
        LOGGER.log(Level.FINEST, "Selection formula returned true");
        LOGGER.exiting(CLASS_NAME, METHOD);
        return true;
      }
      LOGGER.log(Level.FINEST, "Selection formula returned false");
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
          LOGGER.exiting(CLASS_NAME, METHOD);
          return true;
        } else {
          throw e;
        }
      }
      LOGGER.exiting(CLASS_NAME, METHOD);
      return !SourceDocument.isValid() || SourceDocument.isDeleted();
    }

    /*
     * Create a request to delete this document
     */
    private void createDeleteRequest(String googleDocId) throws RepositoryException {
      final String METHOD = "createDeleteRequest";
      LOGGER.entering(CLASS_NAME, METHOD);
      LOGGER.log(Level.FINER,
          "Send deletion request to GSA for {0}", googleDocId);
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
}
