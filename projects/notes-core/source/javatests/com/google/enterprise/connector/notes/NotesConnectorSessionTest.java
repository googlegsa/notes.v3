// Copyright 2012 Google Inc.
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

import com.google.enterprise.connector.notes.client.NotesItem;
import com.google.enterprise.connector.notes.client.mock.NotesDatabaseMock;
import com.google.enterprise.connector.notes.client.mock.NotesDocumentMock;
import com.google.enterprise.connector.notes.client.mock.NotesItemMock;
import com.google.enterprise.connector.notes.client.mock.SessionFactoryMock;

import junit.framework.TestCase;

public class NotesConnectorSessionTest extends TestCase {

  public static void configureFactoryForSession(
      SessionFactoryMock factory) throws Exception {
    // Set environment properties;
    factory.setEnvironmentProperty(NCCONST.INIDIRECTORY,
        System.getProperty("javatest.inidirectory"));

    // Create config database.
    NotesDatabaseMock configDatabase = new NotesDatabaseMock("testserver",
        "testconfig.nsf");
    factory.addDatabase(configDatabase);
    configDatabase.setViewFields(NCCONST.VIEWSERVERS, "server",
        "region", "domain");

    // Create stub names database.
    NotesDatabaseMock namesDatabase = new NotesDatabaseMock("testserver",
        "testnames.nsf");
    factory.addDatabase(namesDatabase);
    NotesDocumentMock notesPerson = new NotesDocumentMock();
    namesDatabase.addDocument(notesPerson, "($Users)");
    namesDatabase.setViewFields("($Users)", "userid", "HTTPPassword");
    notesPerson.addItem(new NotesItemMock("name", "userid",
            "type", NotesItem.TEXT, "values", "cn=Test User/ou=Tests/o=Tests"));
    notesPerson.addItem(new NotesItemMock("name", "HTTPPassword",
            "type", NotesItem.TEXT, "values", "password"));
    NotesDocumentMock connectorPerson = new NotesDocumentMock();
    configDatabase.addDocument(connectorPerson, NCCONST.VIEWPEOPLECACHE);
    configDatabase.setViewFields( NCCONST.VIEWPEOPLECACHE,
        NCCONST.PCITM_USERNAME, NCCONST.PCITM_NOTESNAME, NCCONST.PCITM_GROUPS);
    connectorPerson.addItem(new NotesItemMock("name", NCCONST.PCITM_USERNAME,
            "type", NotesItem.TEXT, "values", "testuser"));
    connectorPerson.addItem(new NotesItemMock("name", NCCONST.PCITM_NOTESNAME,
            "type", NotesItem.TEXT, "values", "cn=Test User/ou=Tests/o=Tests"));
    connectorPerson.addItem(new NotesItemMock("name", NCCONST.PCITM_GROUPS,
            "type", NotesItem.TEXT, "values", "Group1", "Group2"));

    // Create config document.
    NotesDocumentMock systemConfig = new NotesDocumentMock();
    configDatabase.addDocument(systemConfig, NCCONST.VIEWSYSTEMSETUP);

    systemConfig.addItem(new NotesItemMock("name", NCCONST.SITM_MAXFILESIZE,
            "type", NotesItem.NUMBERS,
            "values", new Double(1)));
    systemConfig.addItem(new NotesItemMock("name",
            NCCONST.SITM_EXCLUDEDEXTENSIONS, "type", NotesItem.TEXT,
            "values", "jpg", "gif", "png"));
    systemConfig.addItem(new NotesItemMock("name", NCCONST.SITM_SPOOLDIR,
            "type", NotesItem.TEXT, "values", ""));
    systemConfig.addItem(new NotesItemMock("name", NCCONST.SITM_MAXCRAWLQDEPTH,
            "type", NotesItem.NUMBERS, "values", new Double(3)));
    systemConfig.addItem(new NotesItemMock("name",
            NCCONST.SITM_CACHEUPDATEINTERVAL, "type", NotesItem.NUMBERS,
            "values", new Double(1024)));
    systemConfig.addItem(new NotesItemMock("name", NCCONST.SITM_DIRECTORY,
            "type", NotesItem.TEXT, "values", "testnames.nsf"));
    systemConfig.addItem(new NotesItemMock("name",
            NCCONST.SITM_USERNAMEFORMULA, "type", NotesItem.TEXT,
            "values", ""));
    systemConfig.addItem(new NotesItemMock("name",
            NCCONST.SITM_USERSELECTIONFORMULA, "type", NotesItem.TEXT,
            "values", ""));
    systemConfig.addItem(new NotesItemMock("name", NCCONST.SITM_GSAGROUPPREFIX,
            "type", NotesItem.TEXT, "values", "Domino"));
    systemConfig.addItem(new NotesItemMock("name",
            NCCONST.SITM_DELETIONBATCHSIZE, "type", NotesItem.NUMBERS,
            "values", new Double(100)));
    // Use 0 to prevent NotesConnector from starting threads during tests.
    systemConfig.addItem(new NotesItemMock("name",
            NCCONST.SITM_NUMCRAWLERTHREADS, "type", NotesItem.NUMBERS,
            "values", new Double(0)));
    systemConfig.addItem(new NotesItemMock("name", NCCONST.SITM_MIMETYPES,
            "type", NotesItem.TEXT, "values", "txt@text/plain",
            "html@text/html", "doc@application/msword"));

    // Create a servers document.
    NotesDocumentMock serverDocument = new NotesDocumentMock();
    configDatabase.addDocument(serverDocument, NCCONST.VIEWSERVERS);

    // TODO: check the actual item names in the notes database.
    serverDocument.addItem(new NotesItemMock("name", "server",
            "type", NotesItem.TEXT, "values", "testserver"));
    serverDocument.addItem(new NotesItemMock("name", "region",
            "type", NotesItem.TEXT, "values", "testregion"));
    serverDocument.addItem(new NotesItemMock("name", "domain",
            "type", NotesItem.TEXT, "values", "testdomain"));
  }

  private NotesConnector connector;

  /**
   * Tests creating a session.
   */
  public void testCreateSession() throws Exception {
    connector = new NotesConnector(
        "com.google.enterprise.connector.notes.client.mock.SessionFactoryMock");
    SessionFactoryMock factory = (SessionFactoryMock)
        connector.getSessionFactory();

    NotesConnectorSessionTest.configureFactoryForSession(factory);
    NotesConnectorSession session = new NotesConnectorSession(connector,
        null, "testpassword", "testserver", "testconfig.nsf");

    assertEquals(3, session.getMaxCrawlQDepth());
    assertEquals(100, session.getDeletionBatchSize());
    assertEquals(0, session.getNumCrawlerThreads());
    assertEquals(System.getProperty("javatest.inidirectory") + "/gsaSpool",
        session.getSpoolDir());
    assertEquals("testdomain", session.getDomain("testserver"));
    assertEquals("", session.getDomain("notatestserver"));
    assertEquals("", session.getMimeType("notanext"));
    assertEquals("text/plain", session.getMimeType("txt"));
    assertEquals("text/html", session.getMimeType("html"));
    assertEquals(1024, session.getCacheUpdateInterval());
    assertEquals("testnames.nsf", session.getDirectory());
    assertEquals(NCCONST.DEFAULT_USERNAMEFORMULA,
        session.getUserNameFormula());
    assertEquals(NCCONST.DEFAULT_USERSELECTIONFORMULA,
        session.getUserSelectionFormula());
    assertEquals("testpassword", session.getPassword());
    assertEquals("testserver", session.getServer());
    assertEquals("testconfig.nsf", session.getDatabase());
    assertEquals(1 * 1024 * 1024, session.getMaxFileSize());
    assertTrue(session.isExcludedExtension("jpg"));
    assertFalse(session.isExcludedExtension("doc"));
  }
}
