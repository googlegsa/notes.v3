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

import com.google.enterprise.connector.notes.client.NotesDatabase;
import com.google.enterprise.connector.notes.client.NotesDocument;
import com.google.enterprise.connector.notes.client.NotesSession;
import com.google.enterprise.connector.notes.client.NotesView;
import com.google.enterprise.connector.spi.Document;
import com.google.enterprise.connector.spi.DocumentList;
import com.google.enterprise.connector.spi.RepositoryException;
import com.google.enterprise.connector.spi.SpiConstants.ActionType;

import java.io.File;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

class NotesConnectorDocumentList implements DocumentList {
  private static final String CLASS_NAME =
      NotesConnectorDocumentList.class.getName();
  private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);
  private final Iterator<String> iterator;
  private NotesConnectorDocument ncdoc = null;
  private final NotesConnectorSession ncs;
  private NotesSession ns = null;

  /** The connector database */
  private NotesDatabase db = null;
  private Connection databaseConnection = null;

  /** The backend document being crawled */
  private NotesDocument crawldoc = null;

  /** The list of UNIDs included in this document list */
  private final List<String> unidList;

  public NotesConnectorDocumentList(NotesConnectorSession doclistncs,
      List<String> documents) {
    final String METHOD = "Constructor";
    LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
        "NotesConnectorDocumentList being created: " + documents);
    this.unidList = documents;
    this.iterator = documents.iterator();
    this.ncs = doclistncs;
  }

  @Override
  public Document nextDocument() {
    final String METHOD = "nextDocument";
    LOGGER.entering(CLASS_NAME, METHOD);
    try {
      // The connector manager has finished last doc so recycle it
      Util.recycle(crawldoc);
      if (null != ncdoc) {
        ncdoc.closeInputStream();
      }
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
      crawldoc = db.getDocumentByUNID(unid);
      if (null == ncdoc) {
        ncdoc = new NotesConnectorDocument(ncs, ns, db);
      }
      ncdoc.setCrawlDoc(unid, crawldoc);
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, CLASS_NAME, e);
    } finally {
      LOGGER.exiting(CLASS_NAME, METHOD);
    }
    return ncdoc;
  }

  private void checkpointDelete(NotesDocument deleteDoc,
      NotesView docidvw) throws RepositoryException {
    final String METHOD = "checkpointDelete";
    LOGGER.entering(CLASS_NAME, METHOD);
    String docid = null;
    try {
      docid = deleteDoc.getItemValueString(NCCONST.ITM_DOCID);
      NotesDocId notesId = new NotesDocId(docid);
      docidvw.refresh();
      NotesDocument prevDoc = docidvw.getDocumentByKey(docid, true);
      if (prevDoc != null) {
        prevDoc.remove(true);
      }
      deleteDoc.remove(true);
      if (!Util.isAttachment(docid)) {
        ncs.getNotesDocumentManager().deleteDocument(
            notesId.getDocId(), notesId.getReplicaId(), databaseConnection);
      }
    } catch (MalformedURLException e) {
      LOGGER.severe("Invalid google docid: " + docid);
    }
    LOGGER.exiting(CLASS_NAME, METHOD);
  }

  private void checkpointAcl(NotesDocument aclDoc) throws RepositoryException {
    final String METHOD = "checkpointAcl";
    LOGGER.entering(CLASS_NAME, METHOD);
    try {
      aclDoc.remove(true);
    } catch (Exception e) {
      LOGGER.logp(Level.WARNING, CLASS_NAME, METHOD,
          "Failed to delete ACL document from connector queue", e);
    }
    LOGGER.exiting(CLASS_NAME, METHOD);
  }

  private void checkpointAdd(NotesDocument indexedDoc,
      NotesView docidvw) throws RepositoryException {
    final String METHOD = "checkpointAdd";
    LOGGER.entering(CLASS_NAME, METHOD);
    // getItemValueString returns null in Domino 6.5 or earlier.
    // Empty string after.  Handle both
    String attachPath = indexedDoc.getItemValueString(NCCONST.ITM_CONTENTPATH);
    if (null != attachPath) {
      if (attachPath.length() > 0) {
        LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
            "Checkpoint cleaning up attachment: " + attachPath);
        File f = new File(attachPath);
        f.delete();
        // Remove the parent directory for the document if it is empty
        File parentDir = new File(
            attachPath.substring(0, attachPath.lastIndexOf('/')));
        String[] dirContents = parentDir.list();
        if (dirContents != null) {  // If this is a valid directory
          if (dirContents.length == 0) {  // If the directory is empty
            parentDir.delete();
          }
        }
        // Leave the directory for the database.
      }
    }

    // Delete the content, but leave the meta-data.
    // TODO:   Consider moving content to a text file and then we can cache it
    indexedDoc.removeItem(NCCONST.ITM_CONTENT);
    // Do we all ready have a document with this url all ready?
    String docid = indexedDoc.getItemValueString(NCCONST.ITM_DOCID);
    docidvw.refresh();
    NotesDocument prevDoc = docidvw.getDocumentByKey(docid, true);
    if (null != prevDoc) {
      prevDoc.remove(true);
    }

    boolean isRetained = true;
    if (!ncs.getRetainMetaData()) {
      if (!NCCONST.AUTH_CONNECTOR.equals(
          indexedDoc.getItemValueString(NCCONST.NCITM_AUTHTYPE))) {
        LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
              "Deleting metadata for indexed doc not using connector authz: "
              + docid);
        isRetained = false;
      } else if (indexedDoc.getItemValue(NCCONST.NCITM_DOCAUTHORREADERS)
          .size() == 0) {
        LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
              "Deleting metadata for indexed doc with no readers: " + docid);
        isRetained = false;
      }
    }
    if (isRetained) {
      if (this.ncs.getNotesDocumentManager().addIndexedDocument(
          indexedDoc, databaseConnection) == true) {
        indexedDoc.remove(true);
        LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
          "Retain indexed document in database");
      } else {
        LOGGER.logp(Level.WARNING, CLASS_NAME, METHOD,
            "Failed to add document to database (DocID: " + docid + ")");
      }
    } else {
      indexedDoc.remove(true);
      LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
        "Delete indexed document from Notes");
    }
    LOGGER.exiting(CLASS_NAME, METHOD);
  }

  @Override
  public String checkpoint() throws RepositoryException {
    final String METHOD = "checkpoint";
    String checkPointUnid = null;
    try {
      LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
          "Connector checkpoint documents.");

      // If we don't have a new checkpoint we return null
      if (ncdoc != null) {
        try {
          //Obtain database connection
          databaseConnection = ncs.getNotesDocumentManager()
            .getDatabaseConnection();
          if (databaseConnection == null) {
            throw new RepositoryException(
                "Database connection is not initialized");
          }

          //Otherwise our checkpoint should be the UNID of the
          //current document in the doclist
          checkPointUnid = ncdoc.getUNID();
          NotesView docidvw = db.getView(NCCONST.VIEWINDEXED);

          // We need to iterate through the doclist and clean up
          // the pre-fetched documents and file system objects
          String indexedDocUnid = "";
          for (Iterator<String> ci = unidList.iterator(); ci.hasNext();) {
            indexedDocUnid = ci.next();
            LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
                "Checkpointing document: " + indexedDocUnid);
            try {
              NotesDocument indexedDoc = db.getDocumentByUNID(indexedDocUnid);
              if (indexedDoc.getItemValueString(NCCONST.ITM_ACTION)
                  .equalsIgnoreCase(ActionType.ADD.toString())) {
                // Handle ACL documents separately from content documents.
                if (indexedDoc.hasItem(NCCONST.NCITM_DBACL)) {
                  checkpointAcl(indexedDoc);
                } else {
                  checkpointAdd(indexedDoc, docidvw);
                }
              } else if (indexedDoc.getItemValueString(NCCONST.ITM_ACTION)
                  .equalsIgnoreCase(ActionType.DELETE.toString())) {
                checkpointDelete(indexedDoc, docidvw);
              }
              Util.recycle(indexedDoc);
              // Remove from the document list
              ci.remove();
              // Exit when we get to the checkpoint document
              if (indexedDocUnid.equals(checkPointUnid)) {
                break;
              }
            } catch (Exception e) {
              LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
                  "Error checkpointing document: " + indexedDocUnid, e);
            }
          }
        } catch (RepositoryException re) {
          LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
              "Failed to update search index in database", re);
        } finally {
          //Release database connection
          ncs.getNotesDocumentManager()
            .releaseDatabaseConnection(databaseConnection);
        }
      } else {
        LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
            "Checkpoint for empty document list.");
      }
      // Without lifecycle methods, use the checkpoint to clean up our session
      Util.recycle(crawldoc, db);
      if (this.ns != null) {
        ncs.closeNotesSession(ns);
      }
      this.db = null;
      this.ns = null;
      this.crawldoc = null;
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, CLASS_NAME, e);
    } finally {
    }

    LOGGER.log(Level.FINE, CLASS_NAME, "Checkpoint: " + checkPointUnid);
    return checkPointUnid;
  }

  @Override
  public String toString() {
    return unidList.toString();
  }
}
