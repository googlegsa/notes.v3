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

import com.google.enterprise.connector.notes.client.NotesACL;
import com.google.enterprise.connector.notes.client.NotesACLEntry;
import com.google.enterprise.connector.notes.client.NotesDatabase;
import com.google.enterprise.connector.notes.client.NotesDateTime;
import com.google.enterprise.connector.notes.client.NotesDocument;
import com.google.enterprise.connector.notes.client.NotesItem;
import com.google.enterprise.connector.notes.client.NotesSession;
import com.google.enterprise.connector.notes.client.mock.NotesACLEntryMock;
import com.google.enterprise.connector.notes.client.mock.NotesACLMock;
import com.google.enterprise.connector.notes.client.mock.NotesDatabaseMock;
import com.google.enterprise.connector.notes.client.mock.NotesDateTimeMock;
import com.google.enterprise.connector.notes.client.mock.NotesDocumentMock;
import com.google.enterprise.connector.notes.client.mock.NotesItemMock;
import com.google.enterprise.connector.notes.client.mock.NotesSessionMock;
import com.google.enterprise.connector.notes.client.mock.SessionFactoryMock;
import com.google.enterprise.connector.spi.RepositoryException;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class NotesDatabasePollerTest extends TestCase {

  private static Map<String, Date> lastCrawlCache = new HashMap<String, Date>();

  static class DatabasePollerTestable extends NotesDatabasePoller {

    boolean calledUpdateGsaPolicyAcl;

    DatabasePollerTestable(NotesConnectorSession session) {
      super(session, lastCrawlCache);
    }

    @Override
    void logGsaPolicyAcl(NotesDocument dbdoc) throws RepositoryException {
      calledUpdateGsaPolicyAcl = true;
    }
  }

  private static NotesACLMock acl;

  static {
    acl = new NotesACLMock();
    acl.addAclEntry(new NotesACLEntryMock("-Default-",
        NotesACLEntry.TYPE_UNSPECIFIED, NotesACL.LEVEL_NOACCESS));
    acl.addAclEntry(new NotesACLEntryMock("allowed user1",
        NotesACLEntry.TYPE_PERSON, NotesACL.LEVEL_READER));
    acl.addAclEntry(new NotesACLEntryMock("allowed user2",
        NotesACLEntry.TYPE_PERSON, NotesACL.LEVEL_READER));
    acl.addAclEntry(new NotesACLEntryMock("denied user1",
        NotesACLEntry.TYPE_PERSON, NotesACL.LEVEL_NOACCESS));
    acl.addAclEntry(new NotesACLEntryMock("allowed group1",
        NotesACLEntry.TYPE_PERSON_GROUP, NotesACL.LEVEL_READER));
    acl.addAclEntry(new NotesACLEntryMock("allowed group2",
        NotesACLEntry.TYPE_PERSON_GROUP, NotesACL.LEVEL_READER));
    acl.addAclEntry(new NotesACLEntryMock("denied group1",
        NotesACLEntry.TYPE_PERSON_GROUP, NotesACL.LEVEL_NOACCESS));
  }

  private NotesConnector connector;
  private SessionFactoryMock factory;
  private NotesConnectorSession connectorSession;
  private DatabasePollerTestable poller;

  public NotesDatabasePollerTest() {
    super();
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    connector = NotesConnectorTest.getConnector();
    factory = (SessionFactoryMock) connector.getSessionFactory();
    NotesConnectorSessionTest.configureFactoryForSession(factory);
    connectorSession =
        (NotesConnectorSession) connector.login();
    poller = new DatabasePollerTestable(connectorSession);
  }

  @Override
  protected void tearDown() {
    if (null != connector) {
      connector.shutdown();
    }
  }

  public void testPermitDeny() throws Exception {
    ArrayList<String> permitUsers = new ArrayList<String>();
    ArrayList<String> permitGroups = new ArrayList<String>();
    ArrayList<String> noAccessUsers = new ArrayList<String>();
    ArrayList<String> noAccessGroups = new ArrayList<String>();

    poller.getPermitDeny(NotesDatabasePollerTest.acl, permitUsers, permitGroups,
        noAccessUsers, noAccessGroups, connectorSession.createNotesSession());
    assertEquals(2, permitUsers.size());
    assertEquals("allowed user1", permitUsers.get(0));
    assertEquals("allowed user2", permitUsers.get(1));
    assertEquals(2, noAccessUsers.size());
    assertEquals("denied user1", noAccessUsers.get(1));
    assertEquals(2, permitGroups.size());
    assertEquals("allowed group1", permitGroups.get(0));
    assertEquals("allowed group2", permitGroups.get(1));
    assertEquals("denied groups is not zero", 0, noAccessGroups.size());
  }

  public void testProcessAclCreateDbAclCrawlDoc() throws Exception {
    // Set up a source database with an ACL and a database
    // document for that source database.
    NotesDatabaseMock sourceDatabase = new NotesDatabaseMock("testserver",
        "sourcedatabase.nsf");
    sourceDatabase.setACLActivityLog("today");
    sourceDatabase.setACL(NotesDatabasePollerTest.acl);
    factory.addDatabase(sourceDatabase);
    NotesDocumentMock databaseDocument = new NotesDocumentMock();
    databaseDocument.addItem(new NotesItemMock("name", NCCONST.DITM_ACLTEXT,
        "type", NotesItem.TEXT, "values", "yesterday"));
    databaseDocument.addItem(new NotesItemMock("name", NCCONST.DITM_AUTHTYPE,
        "type", NotesItem.TEXT, "values", NCCONST.AUTH_ACL));
    databaseDocument.addItem(new NotesItemMock("name", NCCONST.DITM_REPLICAID,
        "type", NotesItem.TEXT, "values", "replica_id_16chr"));
    databaseDocument.addItem(new NotesItemMock("name", NCCONST.DITM_SERVER,
        "type", NotesItem.TEXT, "values", connector.getServer()));
    databaseDocument.addItem(new NotesItemMock("name", NCCONST.DITM_DOMAIN,
        "type", NotesItem.TEXT, "values",
        connectorSession.getDomain(connector.getServer())));

    NotesSession session = connectorSession.createNotesSession();
    NotesDatabaseMock connectorDatabase =
        (NotesDatabaseMock) session.getDatabase(
        connectorSession.getServer(), connectorSession.getDatabase());

    /*
    // Add the test groups to the group cache.
    NotesDocumentMock connectorGroup = new NotesDocumentMock();
    connectorGroup.addItem(new NotesItemMock("name", NCCONST.GCITM_GROUPNAME,
            "type", NotesItem.TEXT, "values", "allowed group2"));
    connectorDatabase.addDocument(connectorGroup, NCCONST.VIEWGROUPCACHE);
    */

    assertFalse(databaseDocument.hasItem(NCCONST.NCITM_DBPERMITUSERS));
    assertFalse(databaseDocument.hasItem(NCCONST.NCITM_DBPERMITGROUPS));
    assertFalse(databaseDocument.hasItem(NCCONST.NCITM_DBNOACCESSUSERS));
    assertFalse(databaseDocument.hasItem(NCCONST.NCITM_DBNOACCESSGROUPS));
    assertNull(getDocumentByUnid(connectorDatabase, "replica_id_16chr"));

    poller.processACL(session, connectorDatabase,
        sourceDatabase, databaseDocument);

    assertTrue(databaseDocument.hasItem(NCCONST.NCITM_DBPERMITUSERS));
    assertTrue(databaseDocument.hasItem(NCCONST.NCITM_DBPERMITGROUPS));
    assertTrue(databaseDocument.hasItem(NCCONST.NCITM_DBNOACCESSUSERS));
    assertTrue(databaseDocument.hasItem(NCCONST.NCITM_DBNOACCESSGROUPS));

    NotesDocument aclCrawlDoc =
        getDocumentByUnid(connectorDatabase, "replica_id_16chr");
    assertNotNull(aclCrawlDoc);
    assertEquals("true",
        aclCrawlDoc.getItemValueString(NCCONST.NCITM_DBACL));
    assertEquals("http://testserver.testdomain/replica_id_16chr/"
        + NCCONST.DB_ACL_INHERIT_TYPE_ANDBOTH,
        aclCrawlDoc.getItemValueString(NCCONST.ITM_DOCID));
  }

  private NotesDocument getDocumentByUnid(NotesDatabase db, String replicaId) {
    try {
      return db.getDocumentByUNID(replicaId);
    } catch (RepositoryException e) {
      assertTrue(e.getMessage().contains(replicaId));
      return null;
    }
  }

  public void testPollDatabases_Notes8() throws Exception {
    factory.setEnvironmentProperty(TESTCONST.NOTES_VERSION,
        TESTCONST.NotesVersion.VERSION_8.toString());

    String[] timestamp1 = {
        "11/12/2014 10:00:02",
        "11/12/2014 10:00:01",
        "11/12/2014 10:00:03",
        "11/12/2014 10:00:00"
    };
    String[] timestamp2 = {
        "11/12/2014 10:00:03",
        "06/30/2014 05:00:00"
    };
    testPollDatabases(timestamp1, timestamp2, timestamp2[0]);
  }

  public void testPollDatabases_Notes9() throws Exception {
    factory.setEnvironmentProperty(TESTCONST.NOTES_VERSION,
        TESTCONST.NotesVersion.VERSION_9.toString());

    String[] timestamp1 = {
        "11/12/2014 10:15:02",
        "11/12/2014 10:15:01",
        "11/12/2014 10:15:03"
    };
    String[] timestamp2 = {
        "10/12/2014 08:00:00",
        "11/12/2014 10:15:03"
    };
    testPollDatabases(timestamp1, timestamp2, timestamp2[1]);
  }

  private void testPollDatabases(String[] timestamp1, String[] timestamp2,
      String expectedTime) throws Exception {
    NotesDateTimeMock expectedLastUpdate = MockFixture.parseTime(expectedTime);

    // Setup config and source databases
    NotesDatabaseMock configDb = setUpConfigDb();
    NotesDatabaseMock srcDb =  setUpSourceDb(configDb);

    // Generates documents in source database
    generateSourceDocs(srcDb, NCCONST.VIEWINDEXED, timestamp1);
    assertEquals(timestamp1.length, getDocCount(srcDb));

    int docsBeforePolling = getDocCount(configDb);
    poller.pollDatabases(connectorSession.createNotesSession(), configDb, 100);
    assertEquals(docsBeforePolling + timestamp1.length, getDocCount(configDb));
    assertEquals(expectedLastUpdate, getLastUpdatedTime(configDb, srcDb));

    // Adds new documents (timestamp2) and expects the number of documents in
    // source database must be equal to the number of elements in timestamp1
    // and timestamp2.
    generateSourceDocs(srcDb, NCCONST.VIEWINDEXED, timestamp2);
    assertEquals(timestamp1.length + timestamp2.length, getDocCount(srcDb));

    // The database poller polls more than 1 document whose timestamp is equal
    // to the last updated time; however, only 1 crawl document is created due
    // to the existence of previously crawled documents in the lastCrawlCache.
    docsBeforePolling = getDocCount(configDb);
    poller.pollDatabases(connectorSession.createNotesSession(), configDb, 100);
    assertEquals(docsBeforePolling + 1, getDocCount(configDb));
    assertEquals(expectedLastUpdate, getLastUpdatedTime(configDb, srcDb));
  }

  private NotesDatabaseMock setUpConfigDb() throws Exception {
    String searchFormula =
        "Select Form *= \"Main Topic\":\"MainTopic\":\"Response\"";
    NotesDatabaseMock configDb = factory.getDatabase("testconfig.nsf");

    // Setup template document and search formulas
    MockFixture.setupNotesTemplate(configDb, "Discussion", searchFormula, true);

    // Setup phantom docs in crawl and submit views
    configDb.addDocument(
        MockFixture.createNotesDocumentWithoutReaders("XYZ1"),
        NCCONST.VIEWSUBMITQ);
    configDb.addDocument(
        MockFixture.createNotesDocumentWithoutReaders("XYZ2"),
        NCCONST.VIEWCRAWLQ);
    configDb.setViewFields(NCCONST.VIEWSUBMITQ, NCCONST.NCITM_UNID);
    configDb.setViewFields(NCCONST.VIEWCRAWLQ, NCCONST.NCITM_UNID);

    return configDb;
  }

  private NotesDatabaseMock setUpSourceDb(NotesDatabaseMock configDb)
      throws Exception {
    NotesSessionMock sessionMock =
        (NotesSessionMock) connectorSession.createNotesSession();
    NotesDateTime lastUpdated = sessionMock.createDateTime("1/1/1970");
    NotesDatabaseMock srcDb = new NotesDatabaseMock("mickey1/mtv/us",
        "test.nsf", TESTCONST.DBSRC_REPLICAID);
    factory.addDatabase(srcDb);
    MockFixture.setupSourceDatabase(configDb, srcDb, NCCONST.AUTH_ACL,
        "Discussion", true, lastUpdated, "ACL LOG");

    return srcDb;
  }

  private void generateSourceDocs(NotesDatabaseMock db, String viewName,
      String... timestamps) throws Exception {
    List<NotesDocumentMock> docs =
        MockFixture.generateDocuments(timestamps.length);
    for (int i = 0; i < timestamps.length; i++) {
      NotesDocumentMock doc = docs.get(i);
      doc.setLastModified(MockFixture.parseTime(timestamps[i]));
      db.addDocument(doc, viewName);
    }
  }

  private NotesDateTimeMock getLastUpdatedTime(NotesDatabaseMock configDb,
      NotesDatabaseMock srcDb) throws RepositoryException {
    NotesDocumentMock docDbSrc =
        (NotesDocumentMock) configDb.getDocumentByUNID(srcDb.getReplicaID());
    Vector<?> lastUpdated = docDbSrc.getItemValue(NCCONST.DITM_LASTUPDATE);
    if (lastUpdated == null) {
      return null;
    } else {
      return (NotesDateTimeMock) lastUpdated.get(0);
    }
  }

  private int getDocCount(NotesDatabaseMock db) throws Exception {
    return db.search(null).getCount();
  }
}
