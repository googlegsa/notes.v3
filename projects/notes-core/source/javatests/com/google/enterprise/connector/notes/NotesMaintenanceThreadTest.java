// Copyright 2012 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.enterprise.connector.notes;

import com.google.enterprise.connector.notes.client.NotesDocument;
import com.google.enterprise.connector.notes.client.NotesItem;
import com.google.enterprise.connector.notes.client.mock.NotesDatabaseMock;
import com.google.enterprise.connector.notes.client.mock.NotesDocumentMock;
import com.google.enterprise.connector.notes.client.mock.NotesItemMock;
import com.google.enterprise.connector.notes.client.mock.SessionFactoryMock;
import com.google.enterprise.connector.spi.SpiConstants.ActionType;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class NotesMaintenanceThreadTest extends TestCase {
  private NotesConnector connector;
  private SessionFactoryMock factory;
  private NotesConnectorSession connectorSession;
  private NotesMaintenanceThread maintenanceThread;
  private NotesDocumentManagerTest notesDocMgrDbTest;
  
  private static int BATCH_SIZE = 500;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    connector = NotesConnectorTest.getConnector();
    factory = (SessionFactoryMock) connector.getSessionFactory();
    NotesConnectorSessionTest.configureFactoryForSession(factory);
    connectorSession = (NotesConnectorSession) connector.login();
    maintenanceThread = 
        new NotesMaintenanceThread(connector, connectorSession);
    notesDocMgrDbTest = new NotesDocumentManagerTest();
    notesDocMgrDbTest.setUp();
  }

  public void testCheckForDeletions() throws Exception {
    // Setup deleted document
    String delUNID = "XXXXXXXXXXXXXXXXXXXXXXXXXXXX0000";
    NotesDatabaseMock srcDb = setupSourceDatabase("mickey1/mtv/us", "test.nsf",
        TESTCONST.DBSRC_REPLICAID, notesDocMgrDbTest.getDocuments(),
        NCCONST.VIEWINDEXED);
    NotesDocument delDoc = srcDb.getDocumentByUNID(delUNID);
    delDoc.remove(true);
    String docId = "http://" + TESTCONST.SERVER_DOMINO_WEB + TESTCONST.DOMAIN
        + "/" + TESTCONST.DBSRC_REPLICAID + "/0/" + delUNID;

    // Setup template document including formulas
    String searchString =
        "Select Form *= \"Main Topic\":\"MainTopic\":\"Response\"";
    NotesDatabaseMock configDatabase = factory.getDatabase("testconfig.nsf");
    NotesDocumentMock docTmpl = new NotesDocumentMock();
    docTmpl.addItem(new NotesItemMock("name", "Form", "type", NotesItem.TEXT,
        "values", "TEMPLATE"));
    docTmpl.addItem(new NotesItemMock("name", "TemplateName", "type",
        NotesItem.TEXT, "values", "Discussion"));
    docTmpl.addItem(new NotesItemMock("name", "SearchString", "type",
        NotesItem.TEXT, "values", searchString));
    configDatabase.addDocument(docTmpl, NCCONST.VIEWTEMPLATES);
    configDatabase.setViewFields(NCCONST.VIEWTEMPLATES, "TemplateName");

    // Run check for deletion
    Map<String, NotesDocId> docs = 
        notesDocMgrDbTest.getIndexedDocument(null, null, 1);
    NotesDocId startUnid = docs.get(docs.keySet().iterator().next());
    maintenanceThread.checkForDeletions(startUnid.toString(), BATCH_SIZE);

    List<NotesDocumentMock> docList =
        configDatabase.getDocumentsByField(NCCONST.ITM_DOCID, docId);
    assertEquals(1, docList.size());
    NotesDocumentMock docDeletedReq = docList.get(0);
    assertEquals(ActionType.DELETE.toString(),
        docDeletedReq.getItemValueString(NCCONST.ITM_ACTION));
  }

  public void testMissingSelectionCriteria() throws Exception {
    // Setup log messages and source database
    List<String> logs = captureLogMessages("selection criteria");
    setupSourceDatabase("mickey1/mtv/us", "test.nsf",
        TESTCONST.DBSRC_REPLICAID, notesDocMgrDbTest.getDocuments(),
        NCCONST.VIEWINDEXED);

    // Create template document without formulas
    NotesDatabaseMock configDatabase = factory.getDatabase("testconfig.nsf");
    NotesDocumentMock docTmpl = new NotesDocumentMock();
    docTmpl.addItem(new NotesItemMock("name", "Form", "type", NotesItem.TEXT,
        "values", "TEMPLATE"));
    docTmpl.addItem(new NotesItemMock("name", "TemplateName", "type",
        NotesItem.TEXT, "values", "Discussion"));
    configDatabase.addDocument(docTmpl, NCCONST.VIEWTEMPLATES);

    // Run deletion check to capture error logs
    Map<String,NotesDocId> docs = 
        notesDocMgrDbTest.getIndexedDocument(null, null, 1);
    NotesDocId startUnid = docs.get(docs.keySet().iterator().next());
    maintenanceThread.checkForDeletions(startUnid.toString(), BATCH_SIZE);

    // Test log message
    assertEquals(BATCH_SIZE, logs.size());
    assertTrue("Source server is not logged",
        logs.get(0).contains("mickey1/mtv/us"));
    assertTrue("Source database is not logged",
        logs.get(0).contains("test.nsf"));
    assertTrue(logs.get(0).contains(TESTCONST.DBSRC_REPLICAID));
  }

  public void testValidSelectionCriteriaFormulas() throws Exception {
    // Setup log messages and source database
    List<String> logs = captureLogMessages("selection formula");
    setupSourceDatabase("mickey1/mtv/us", "test.nsf",
        TESTCONST.DBSRC_REPLICAID, notesDocMgrDbTest.getDocuments(),
        NCCONST.VIEWINDEXED);

    // Create template document including formulas
    String searchString =
        "Select Form *= \"Main Topic\":\"MainTopic\":\"Response\"";
    NotesDatabaseMock configDatabase = factory.getDatabase("testconfig.nsf");
    NotesDocumentMock docTmpl = new NotesDocumentMock();
    docTmpl.addItem(new NotesItemMock("name", "Form", "type", NotesItem.TEXT,
        "values", "TEMPLATE"));
    docTmpl.addItem(new NotesItemMock("name", "TemplateName", "type",
        NotesItem.TEXT, "values", "Discussion"));
    docTmpl.addItem(new NotesItemMock("name", "SearchString", "type",
        NotesItem.TEXT, "values", searchString));
    configDatabase.addDocument(docTmpl, NCCONST.VIEWTEMPLATES);
    configDatabase.setViewFields(NCCONST.VIEWTEMPLATES, "TemplateName");

    // Run deletion check to capture error logs
    Map<String,NotesDocId> docs = 
        notesDocMgrDbTest.getIndexedDocument(null, null, 1);
    NotesDocId startUnid = docs.get(docs.keySet().iterator().next());
    maintenanceThread.checkForDeletions(startUnid.toString(), BATCH_SIZE);

    // Test log message
    assertTrue(logs.size() > 0);
    assertTrue(logs.get(0).contains(searchString));
  }

  /**
   * Capture and return log messages that match the substring.
   */
  private List<String> captureLogMessages(final String substr) {
    // Setup logger
    final List<String> logs = new ArrayList<String>();
    Logger logger = Logger.getLogger(NotesMaintenanceThread.class.getName());
    logger.setLevel(Level.ALL);

    logger.addHandler(new Handler() {
        @Override public void close() {}
        @Override public void flush() {}

        @Override public void publish(LogRecord arg0) {
          if (arg0.getMessage().contains(substr)) {
            logs.add(arg0.getMessage());
          }
        }
    });
    return logs;
  }

  private NotesDatabaseMock setupSourceDatabase(String server, String filePath,
      String replicaId, List<NotesDocumentMock> docs, String viewName) {
    NotesDatabaseMock srcDb =
        new NotesDatabaseMock(server, filePath, replicaId);
    for (NotesDocumentMock doc : docs) {
      srcDb.addDocument(doc, viewName);
    }
    factory.addDatabase(srcDb);
    return srcDb;
  }
}
