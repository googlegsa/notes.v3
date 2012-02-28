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
import com.google.enterprise.connector.notes.client.NotesSession;
import com.google.enterprise.connector.notes.client.NotesView;
import com.google.enterprise.connector.notes.client.NotesViewEntry;
import com.google.enterprise.connector.notes.client.NotesViewNavigator;
import com.google.enterprise.connector.spi.DocumentList;
import com.google.enterprise.connector.spi.TraversalManager;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotesTraversalManager implements TraversalManager {
  //private static final int MAX_DOCID = 1000;
  private int batchHint = 10;
  private static final String CLASS_NAME =
      NotesTraversalManager.class.getName();
  private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);
  private NotesConnectorSession ncs = null;

  public NotesTraversalManager(NotesConnectorSession session) {
    ncs = session;
  }

  /* @Override */
  public void setBatchHint(int hint) {
    final String METHOD = "setBatchHint";
    LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD, "batchHint set to : " + hint);
    batchHint = hint;
  }

  /* @Override */
  public DocumentList startTraversal() {
    LOGGER.info("Start traversal");
    // This will reset the start date on all connector
    NotesDatabasePoller.resetDatabases(ncs);
    return traverse("0");
  }

  /* @Override */
  public DocumentList resumeTraversal(String checkpoint) {
    return traverse(checkpoint);
  }

  /**
   * Utility method to produce a {@code DocumentList} containing the next
   * batch of {@code Document} from the checkpoint.
   *
   * @param checkpoint
   *            a String representing the last document number processed.
   */
  private DocumentList traverse(String checkpoint) {
    final String METHOD = "traverse";
    List<String> unidList = new ArrayList<String>(batchHint);
    NotesSession ns = null;

    try {
      LOGGER.entering(CLASS_NAME, METHOD);

      LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
          "Resuming from checkpoint: " + checkpoint);

      ns = ncs.createNotesSession();
      NotesDatabase cdb = ns.getDatabase(ncs.getServer(), ncs.getDatabase());

      // Poll for changes
      // TODO:  Consider moving this to the housekeeping thread
      // Since it takes two polling cycles to get documents into the GSA
      // if the system is idle

      NotesDatabasePoller dbpoller = new NotesDatabasePoller(ncs);
      dbpoller.pollDatabases(ns, cdb, ncs.getMaxCrawlQDepth());
      NotesPollerNotifier npn = ncs.getNotifier();
      npn.wakeWorkers();

      // Give the worker threads a chance to pre-fetch documents
      Thread.sleep(2000);

      // Get list of pre-fetched documents and put these in the doclist
      NotesView submitQ = cdb.getView(NCCONST.VIEWSUBMITQ);
      NotesViewNavigator submitQNav = submitQ.createViewNav();
      NotesViewEntry ve = submitQNav.getFirst();
      int batchSize = 0;
      while (( ve != null) && (batchSize < batchHint)) {
        batchSize++;
        String unid = ve.getColumnValues().elementAt(1).toString();
        LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
            "Adding document to list" + unid);
        unidList.add(unid);
        NotesViewEntry prevVe = ve;
        ve = submitQNav.getNext(prevVe);
        prevVe.recycle();
      }
      submitQNav.recycle();
      submitQ.recycle();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      ncs.closeNotesSession(ns);
      ns = null;
    }

    LOGGER.exiting(CLASS_NAME, METHOD);

    if (unidList.size() == 0) {
      return null;
    }
    return new NotesConnectorDocumentList(ncs, unidList);
  }
}
