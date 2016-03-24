// Copyright 2011 Google Inc.
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

import com.google.enterprise.connector.notes.NotesConnector;
import com.google.enterprise.connector.notes.NotesConnectorDocument;
import com.google.enterprise.connector.spi.ConnectorPersistentStore;
import com.google.enterprise.connector.spi.Document;
import com.google.enterprise.connector.spi.DocumentList;
import com.google.enterprise.connector.spi.LocalDatabase;
import com.google.enterprise.connector.spi.RepositoryException;
import com.google.enterprise.connector.spi.RepositoryLoginException;
import com.google.enterprise.connector.spi.TraversalManager;
import com.google.enterprise.connector.util.database.testing.TestLocalDatabase;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

// TODO: We need a method to remove all crawl docs from the
// connector database. The tests use various pieces of the crawl
// process, leaving the database in a potentially inconsistent
// state. Running enough tests causes test failures even though
// all tests pass starting with a clean connector database.
public class ConnectorFixture extends TestCase {

  private static final Logger LOGGER = Logger.getLogger(
      ConnectorFixture.class.getName());
  static String server;
  static String database;
  static String idpassword;

  static String getRequiredProperty(String key) {
    String value = System.getProperty(key);
    assertNotNull(key, value);
    return value;
  }

  static String getOptionalProperty(String key) {
    return System.getProperty(key);
  }

  static NotesConnector getConnector(boolean allowMaintenanceThread,
      boolean allowCrawlerThread) throws RepositoryException {
    // Get test properties.
    server = getRequiredProperty("javatest.server");
    database = getRequiredProperty("javatest.database");
    idpassword = getRequiredProperty("javatest.idpassword");

    // Instantiate and configure connector.
    NotesConnector connector = new NotesConnector();
    if (!allowMaintenanceThread) {
      connector.maintThread = new NotesMaintenanceThread(null, null);
    }
    if (!allowCrawlerThread) {
      connector.vecCrawlerThreads = new Vector<NotesCrawlerThread>();
    }
    connector.setServer(server);
    connector.setDatabase(database);
    connector.setIdPassword(idpassword);
    connector.setPolicyAclPattern(
        "^googleconnector://{0}.localhost/doc?docid={1}");
    connector.setGoogleConnectorName("notestest");
    connector.setDatabaseAccess(new ConnectorPersistentStore() {
        @Deprecated @Override
        public com.google.enterprise.connector.spi.LocalDocumentStore
            getLocalDocumentStore() {
          return null;
        }
        @Override
        public LocalDatabase getLocalDatabase() {
          // TODO: update resource directory when resources are implemented
          return new TestLocalDatabase("Lotus_Notes", null);
        }
      });

    return connector;
  }

  public static NotesTraversalManager getTraversalManager(NotesConnectorSession
      connectorSession) throws RepositoryLoginException, RepositoryException {
    return connectorSession.getTraversalManager();
  }

  public static List<Document> traverseAll(NotesConnectorSession
      connectorSession) throws RepositoryLoginException, RepositoryException {
    ArrayList<Document> documents = new ArrayList<Document>();
    TraversalManager tm = getTraversalManager(connectorSession);
    // Get the first set of documents.
    tm.setBatchHint(20);
    DocumentList docList = tm.startTraversal();
    assertNotNull("startTraversal returned a null document list", docList);
    while (docList != null) {
      Document doc;
      while (null != (doc = docList.nextDocument())) {
        documents.add((NotesConnectorDocument) doc);
      }
      String checkpoint = docList.checkpoint();
      // Resume traversal.
      tm.setBatchHint(20);
      docList = tm.resumeTraversal(checkpoint);
    }
    return documents;
  }

  NotesConnector connector;
  boolean allowMaintenanceThread = false;
  boolean allowCrawlerThread = false;

  public ConnectorFixture() {
    super();
  }

  @Override
  protected void setUp() throws Exception {
    LOGGER.entering(this.getClass().getName(), getName());
    connector = getConnector(allowMaintenanceThread, allowCrawlerThread);
  }

  @Override
  protected void tearDown() throws Exception {
    connector.shutdown();
    super.tearDown();
  }
}
