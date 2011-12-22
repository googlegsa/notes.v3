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

import com.google.enterprise.connector.notes.NotesConnector;
import com.google.enterprise.connector.notes.NotesConnectorSession;
import com.google.enterprise.connector.notes.NotesTraversalManager;
import com.google.enterprise.connector.spi.Connector;
import com.google.enterprise.connector.spi.Document;
import com.google.enterprise.connector.spi.DocumentList;
import com.google.enterprise.connector.spi.RepositoryException;
import com.google.enterprise.connector.spi.RepositoryLoginException;
import com.google.enterprise.connector.spi.Session;
import com.google.enterprise.connector.spi.SpiConstants;
import com.google.enterprise.connector.spi.TraversalManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NotesTraversalManagerTest extends ConnectorFixture {

  public NotesTraversalManagerTest() {
    super();
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    // Temporary fix for the need to create user/group cache.
    Session session = connector.login();
    NotesUserGroupManager ug = new NotesUserGroupManager();
    ug.updatePeopleGroups((NotesConnectorSession) session, true);
  }

  /**
   * Tests TraversalManager.resume to verify that a call to
   * resumeTraversal doesn't return the same documents as the
   * initial call to startTraversal.
   *
   * @throws RepositoryLoginException
   * @throws RepositoryException
   */
  public void testTraversalReturnsNewDocumentsOnResume()
      throws RepositoryLoginException, RepositoryException {
    Session session = connector.login();
    TraversalManager tm = session.getTraversalManager();
    assertNotNull(tm);
    assertTrue(tm instanceof NotesTraversalManager);

    // Get the first set of documents.
    tm.setBatchHint(5);
    DocumentList docList = tm.startTraversal();
    Document doc;
    String checkpoint = null;
    List<String> docIdList = new ArrayList<String>(10);
    assertNotNull("startTraversal returned a null document list", docList);
    while (null != (doc = docList.nextDocument())) {
      String docId = doc.findProperty(SpiConstants.PROPNAME_DOCID)
          .nextValue().toString();
      assertNotNull("Missing doc id", docId);
      docIdList.add(docId);
    }
    checkpoint = docList.checkpoint();
    assertNotNull("Checkpoint was null", checkpoint);
    assertTrue("No docs found", docIdList.size() > 0);

    // Resume traversal.
    tm.setBatchHint(5);
    docList = tm.resumeTraversal(checkpoint);
    while (null != (doc = docList.nextDocument())) {
      String docId = doc.findProperty(SpiConstants.PROPNAME_DOCID).
          nextValue().toString();
      assertNotNull("Missing doc id", docId);
      assertFalse("Found same docid in new doc list: " + docId,
          docIdList.contains(docId));
    }
  }

  /**
   * Traversing all documents, then restarting with
   * TraversalManager.startTraversal, should give the same list
   * of documents on each traversal. This test verifies that no
   * documents are skipped. Duplicates during a single traversal
   * are accepted.
   *
   * @throws RepositoryLoginException
   * @throws RepositoryException
   */
  /* TODO: consider creating a helper to take a TraversalManager
   * and return the list of docid values.
   */
  /*
  public void testTraverseAllDocuments()
      throws RepositoryLoginException, RepositoryException {
    Session session = connector.login();
    TraversalManager tm = session.getTraversalManager();

    Set<String> docIdListFirstTraversal = new HashSet<String>(100);
    List<String> duplicatesFirstTraversal = new ArrayList<String>(100);
    // Get the first set of documents.
    tm.setBatchHint(20);
    DocumentList docList = tm.startTraversal();
    while (docList != null) {
      Document doc = null;
      while (null != (doc = docList.nextDocument())) {
        String docId = doc.findProperty(SpiConstants.PROPNAME_DOCID).
            nextValue().toString();
        if (!docIdListFirstTraversal.add(docId)) {
          duplicatesFirstTraversal.add(docId);
        }
      }
      String checkpoint = docList.checkpoint();
      assertNotNull("Checkpoint was null", checkpoint);

      // Resume traversal.
      tm.setBatchHint(20);
      docList = tm.resumeTraversal(checkpoint);
    }
    assertTrue("No documents traversed", docIdListFirstTraversal.size() > 0);
    // TODO: do we want to investigate the presence of duplicates?
    //if (duplicatesFirstTraversal.size() > 0) {
    //  System.out.println("Found duplicates during first traversal: " +
    //      duplicatesFirstTraversal.size());
    //}

    Set<String> docIdListSecondTraversal = new HashSet<String>(100);
    List<String> duplicatesSecondTraversal = new ArrayList<String>(100);
    // Get the second set of documents.
    tm.setBatchHint(20);
    docList = tm.startTraversal();
    while (docList != null) {
      Document doc = null;
      while (null != (doc = docList.nextDocument())) {
        String docId = doc.findProperty(SpiConstants.PROPNAME_DOCID).
            nextValue().toString();
        if (!docIdListSecondTraversal.add(docId)) {
          duplicatesSecondTraversal.add(docId);
        }
      }
      String checkpoint = docList.checkpoint();
      assertNotNull("Checkpoint was null", checkpoint);

      // Resume traversal.
      tm.setBatchHint(20);
      docList = tm.resumeTraversal(checkpoint);
    }
    assertTrue("No documents traversed on second traversal",
        docIdListSecondTraversal.size() > 0);

    assertEquals("Set of documents in first and second traversals differ",
        docIdListFirstTraversal, docIdListSecondTraversal);

    // TODO: do we want to investigate the presence of duplicates?
    //if (duplicatesSecondTraversal.size() > 0) {
    //  System.out.println("Found duplicates during second traversal: " +
    //      duplicatesSecondTraversal.size());
    //}
  }
  */
}

