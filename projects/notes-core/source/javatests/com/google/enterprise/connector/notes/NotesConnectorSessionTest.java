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

import junit.framework.TestCase;

import java.util.ArrayList; 
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NotesConnectorSessionTest extends TestCase {

  private String server; 
  private String database; 
  private String idpassword; 
  private NotesConnector connector; 

  public NotesConnectorSessionTest() {
  }
  
  private String getProperty(String key) {
    String value = System.getProperty(key);
    assertNotNull(key, value);
    return value; 
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp(); 
    server = getProperty("javatest.server"); 
    database = getProperty("javatest.database"); 
    idpassword = getProperty("javatest.idpassword"); 
    connector = new NotesConnector();
    connector.setServer(server);
    connector.setDatabase(database);
    connector.setIdPassword(idpassword);
  }

  @Override
  protected void tearDown() throws Exception {
    connector.shutdown();
    super.tearDown();
  }

  /**
   * Tests NotesConnector.login and the properties set in
   * NotesConnectorSession. Assumes that the Notes GSA Connector
   * config is using default values where appropriate. 
   *
   * @throws RepositoryLoginException
   * @throws RepositoryException
   */
  public void testProperties() throws RepositoryLoginException, 
      RepositoryException {
    NotesConnectorSession session = (NotesConnectorSession) connector.login();
    assertEquals("maxCrawlQDepth", 5000, session.getMaxCrawlQDepth());
    assertEquals("deletionBatchSize", 300, session.getDeletionBatchSize());
    assertEquals("numCrawlerThreads", 1, session.getNumCrawlerThreads());
    assertNotNull("notifier", session.getNotifier());
    assertTrue("spoolDir", session.getSpoolDir().endsWith("gsaSpool")); 
    assertEquals("domain", "", session.getDomain(server));
    assertEquals("mimetype", "application/msword", session.getMimeType("doc"));
    assertEquals("idpassword", idpassword, session.getPassword());
    assertEquals("server", server, session.getServer());
    assertEquals("maxFileSize", 30 * 1024 * 1024, session.getMaxFileSize());
    assertEquals("database", database, session.getDatabase());
    assertSame("connector", connector, session.getConnector());

    assertFalse(".doc is excluded", session.isExcludedExtension("doc")); 
    assertFalse(".DOC is excluded", session.isExcludedExtension("DOC"));
    assertTrue(".jpg is included", session.isExcludedExtension("jpg")); 
    // TODO: File extensions are currently case-sensitive, so
    // this fails. Modify this assertion appropriately when a
    // decision is reached about whether extensions should be
    // case-insensitive.
    //assertTrue(".JPG is included", session.isExcludedExtension("JPG")); 
  }
}

