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
import com.google.enterprise.connector.spi.SpiConstants;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

class NotesConnectorDocumentList implements DocumentList {
  private static final String CLASS_NAME =
      NotesConnectorDocumentList.class.getName();
  private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);
  private Iterator<String> iterator;
  private NotesConnectorDocument ncdoc = null;
  private NotesConnectorSession ncs;
  private NotesSession ns = null;

  /** The connector database */
  private NotesDatabase db = null;

  /** The backend document being crawled */
  private NotesDocument crawldoc = null;

  /** The list of UNIDs included in this document list */
  private List<String> unidList = null;

  public NotesConnectorDocumentList(NotesConnectorSession doclistncs,
      List<String> documents) {
    final String METHOD = "Constructor";
    LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
        "NotesConnectorDocumentList being created.");
    this.unidList = documents;
    this.iterator = documents.iterator();
    this.ncs = doclistncs;
  }

  /* @Override */
  public Document nextDocument() {
    final String METHOD = "nextDocument";
    LOGGER.entering(CLASS_NAME, METHOD);
    try {
      // The connector manager has finished last doc so recycle it
      if (null != crawldoc) {
        crawldoc.recycle();
      }
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
    }
    LOGGER.exiting(CLASS_NAME, METHOD);
    return ncdoc;
  }

  public void checkpointDelete(NotesDocument deleteDoc,
      NotesView docidvw) throws RepositoryException {
    final String METHOD = "checkpointDelete";
    LOGGER.entering(CLASS_NAME, METHOD);
    String docid = deleteDoc.getItemValueString(NCCONST.ITM_DOCID);
    docidvw.refresh();
    NotesDocument prevDoc = docidvw.getDocumentByKey(docid, true);
    if (null != prevDoc) {
      prevDoc.remove(true);
    }
    deleteDoc.remove(true);
    LOGGER.exiting(CLASS_NAME, METHOD);
  }

  public void checkpointAdd(NotesDocument indexedDoc,
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
        java.io.File f = new java.io.File(attachPath);
        f.delete();
        // Remove the parent directory for the document if it is empty
        java.io.File parentDir = new java.io.File(
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

    indexedDoc.replaceItemValue(NCCONST.NCITM_STATE, NCCONST.STATEINDEXED);
    indexedDoc.save(true);
    if (!ncs.getRetainMetaData()) {
      if (!NCCONST.AUTH_CONNECTOR.equals(
              indexedDoc.getItemValueString(NCCONST.NCITM_AUTHTYPE))) {
        LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
            "Deleting metadata for indexed doc not using connector authz: "
            + docid);
        indexedDoc.remove(true);
      } else if (indexedDoc.getItemValue(NCCONST.NCITM_DOCAUTHORREADERS)
          .size() == 0) {
        LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
            "Deleting metadata for indexed doc with no readers: " + docid);
        indexedDoc.remove(true);
      }
    }
    LOGGER.exiting(CLASS_NAME, METHOD);
  }

  /* @Override */
  public String checkpoint() throws RepositoryException {
    final String METHOD = "checkpoint";
    String checkPointUnid = null;
    try {
      LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
          "Connector checkpoint documents.");

      // If we don't have a new checkpoint we return null
      if (ncdoc != null) {
        //Otherwise our checkpoint should be the UNID of the
        //current document in the doclist
        checkPointUnid = ncdoc.getUNID();
        NotesView docidvw = db.getView(NCCONST.VIEWINDEXED);

        // We need to iterate through the doclist and clean up
        // the pre-fetched documents and file system objects
        String indexedDocUnid = "";
        for (Iterator<String> ci = unidList.iterator(); ci.hasNext();) {
          indexedDocUnid =  ci.next();
          LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
              "Checkpointing document: " + indexedDocUnid);
          NotesDocument indexedDoc =
              db.getDocumentByUNID(indexedDocUnid);
          if (indexedDoc.getItemValueString(NCCONST.ITM_ACTION)
              .equalsIgnoreCase(SpiConstants.ActionType.ADD.toString())) {
            if (indexedDoc.hasItem(NCCONST.NCITM_DBACL)) {
              checkpointDelete(indexedDoc, docidvw);
            } else {
              checkpointAdd(indexedDoc, docidvw);
            }
          } else if (indexedDoc.getItemValueString(NCCONST.ITM_ACTION)
              .equalsIgnoreCase(SpiConstants.ActionType.DELETE.toString())) {
            checkpointDelete(indexedDoc, docidvw);
          }
          indexedDoc.recycle();
          // Remove from the document list
          ci.remove();
          // Exit when we get to the checkpoint document
          if (indexedDocUnid.equals(checkPointUnid)) {
            break;
          }
        }
      }
      else
        LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
            "Checkpoint for empty document list.");
      // Without lifecycle methods, use the checkpoint to clean up our session
      if (this.crawldoc != null) {
        this.crawldoc.recycle();
      }
      if (this.db != null) {
        this.db.recycle();
      }
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

  public String toString() {
    return unidList.toString();
  }
}
