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
import com.google.enterprise.connector.notes.client.mock.SessionFactoryMock;
import com.google.enterprise.connector.spi.RepositoryException;
import com.google.enterprise.connector.spi.SimpleTraversalContext;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

public class NotesDatabasePollerTest extends TestCase {

  static class DatabasePollerTestable extends NotesDatabasePoller {

    boolean calledUpdateGsaPolicyAcl;

    DatabasePollerTestable(NotesConnectorSession session) {
      super(session);
    }

    @Override
    boolean updateGsaPolicyAcl(NotesSession session,
        NotesDatabase connectorDatabase, NotesDocument dbdoc,
        Collection<String> permitUsers, Collection<String> permitGroups)
        throws RepositoryException {
      calledUpdateGsaPolicyAcl = true;
      return true;
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
  private boolean supportsInheritedAcls;

  public NotesDatabasePollerTest() {
    super();
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    // TODO: handle both versions of acl support within the tests
    // and avoid manual property editing.
    supportsInheritedAcls =
        Boolean.getBoolean("javatest.supportsinheritedacls");
    connector = NotesConnectorTest.getConnector();
    factory = (SessionFactoryMock) connector.getSessionFactory();
    NotesConnectorSessionTest.configureFactoryForSession(factory);
    connectorSession =
        (NotesConnectorSession) connector.login();
    SimpleTraversalContext context = new SimpleTraversalContext();
    context.setSupportsInheritedAcls(supportsInheritedAcls);
    connectorSession.getTraversalManager().setTraversalContext(context);
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
    if (supportsInheritedAcls) {
      assertNotNull(aclCrawlDoc);
      assertEquals("true",
          aclCrawlDoc.getItemValueString(NCCONST.NCITM_DBACL));
      assertEquals("http://testserver.testdomain/replica_id_16chr/"
          + NCCONST.DB_ACL_INHERIT_TYPE_ANDBOTH,
          aclCrawlDoc.getItemValueString(NCCONST.ITM_DOCID));
    } else {
      assertTrue(poller.calledUpdateGsaPolicyAcl);
      assertNull(aclCrawlDoc);
    }
  }

  private NotesDocument getDocumentByUnid(NotesDatabase db, String replicaId) {
    try {
      return db.getDocumentByUNID(replicaId);
    } catch (RepositoryException e) {
      assertTrue(e.getMessage().contains(replicaId));
      return null;
    }
  }

  public void testPollSourceDatabaseByLastModified() throws Exception {
    NotesDateTime lastUpdated =
        connectorSession.createNotesSession().createDateTime("1/1/1970");
    List<NotesDocumentMock> docs = MockFixture.generateDocuments(1);
    Vector<?> vecLastModified =
        docs.get(docs.size() - 1).getItemValue(NCCONST.ITM_LASTMODIFIED);

    // Setup source database
    NotesDatabaseMock configDb = factory.getDatabase("testconfig.nsf");
    NotesDatabaseMock srcDb = MockFixture.newMockDatabase("mickey1/mtv/us",
        "test.nsf", TESTCONST.DBSRC_REPLICAID, docs, NCCONST.VIEWINDEXED);
    factory.addDatabase(srcDb);
    MockFixture.setupSourceDatabase(configDb, srcDb, NCCONST.AUTH_ACL,
        "Discussion", true, lastUpdated, "ACL LOG");

    // Setup template document including formulas
    String searchFormula =
        "Select Form *= \"Main Topic\":\"MainTopic\":\"Response\"";
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

    // Poll source database
    poller.pollDatabases(connectorSession.createNotesSession(), configDb, 10);

    // Expect the last updated time in the database source document to be
    // equal to the last modified time of the source document.
    NotesDocumentMock docDbSrc =
        (NotesDocumentMock) configDb.getDocumentByUNID(
            TESTCONST.DBSRC_REPLICAID);
    Vector<?> vecLastUpdated = docDbSrc.getItemValue(NCCONST.DITM_LASTUPDATE);
    assertEquals(vecLastModified, vecLastUpdated);
  }
}
