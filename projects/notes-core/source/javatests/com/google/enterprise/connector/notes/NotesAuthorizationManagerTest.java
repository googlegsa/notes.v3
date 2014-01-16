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

import com.google.common.collect.Lists;
import com.google.enterprise.connector.notes.NotesUserGroupManager.User;
import com.google.enterprise.connector.notes.client.NotesACL;
import com.google.enterprise.connector.notes.client.NotesACLEntry;
import com.google.enterprise.connector.notes.client.NotesItem;
import com.google.enterprise.connector.notes.client.NotesSession;
import com.google.enterprise.connector.notes.client.mock.NotesACLEntryMock;
import com.google.enterprise.connector.notes.client.mock.NotesACLMock;
import com.google.enterprise.connector.notes.client.mock.NotesDatabaseMock;
import com.google.enterprise.connector.notes.client.mock.NotesDocumentMock;
import com.google.enterprise.connector.notes.client.mock.NotesItemMock;
import com.google.enterprise.connector.notes.client.mock.NotesSessionMock;
import com.google.enterprise.connector.notes.client.mock.SessionFactoryMock;
import com.google.enterprise.connector.spi.AuthorizationResponse;
import com.google.enterprise.connector.spi.Property;
import com.google.enterprise.connector.spi.SimpleAuthenticationIdentity;
import com.google.enterprise.connector.spi.SimpleTraversalContext;
import com.google.enterprise.connector.spi.SpiConstants;
import com.google.enterprise.connector.spi.SpiConstants.ActionType;
import com.google.enterprise.connector.spi.TraversalContextAware;
import com.google.enterprise.connector.spi.Value;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

public class NotesAuthorizationManagerTest extends TestCase {

  private static NotesConnector connector;
  private static SessionFactoryMock factory;
  private static NotesConnectorSession connectorSession;
  private static NotesSession session;
  private static NotesDatabaseMock namesDatabase;
  private static NotesDatabaseMock testDb;

  public static Test suite() {
    return new TestSetup(
        new TestSuite(NotesAuthorizationManagerTest.class)) {
      @Override protected void setUp() throws Exception {
        connector = NotesConnectorTest.getConnector();
        factory = (SessionFactoryMock) connector.getSessionFactory();
        NotesConnectorSessionTest.configureFactoryForSession(factory);
        connectorSession = (NotesConnectorSession) connector.login();
        session = connectorSession.createNotesSession();
        namesDatabase = (NotesDatabaseMock) session.getDatabase(
            connectorSession.getServer(), connectorSession.getDirectory());

        NotesUserGroupManagerTest.addNotesUser(connectorSession,
            namesDatabase, "cn=Anakin Skywalker/ou=Tests/o=Tests",
            "anakin");
        NotesUserGroupManagerTest.addNotesUser(connectorSession,
            namesDatabase, "cn=Jane Smith/ou=Tests/o=Tests",
            "jsmith");
        NotesUserGroupManagerTest.addNotesGroup(namesDatabase,
            "masters",
            "cn=anakin skywalker/ou=tests/o=tests",
            "*/ou=tests/o=tests");
        NotesUserGroupManagerTest.addNotesGroup(namesDatabase,
            "jedi", "masters");
        NotesUserGroupManagerTest.addNotesGroup(namesDatabase,
            "good guys", "jedi");

        NotesDatabaseMock configDatabase = (NotesDatabaseMock) session
            .getDatabase(connectorSession.getServer(),
            connectorSession.getDatabase());
        NotesDocumentMock databaseDocument = new NotesDocumentMock();
        databaseDocument.addItem(new NotesItemMock("name", NCCONST.DITM_DBNAME,
            "type", NotesItem.TEXT, "values", "JediTrainingManuals.nsf"));
        databaseDocument.addItem(new NotesItemMock("name",
            NCCONST.DITM_REPLICAID, "type", NotesItem.TEXT,
            "values", "jtmreplicaid0123"));
        databaseDocument.addItem(new NotesItemMock("name", NCCONST.DITM_SERVER,
            "type", NotesItem.TEXT, "values", "JediServer"));
        databaseDocument.addItem(new NotesItemMock("name",
            NCCONST.NCITM_DBPERMITUSERS, "type", NotesItem.TEXT,
            "values", "cn=anakin skywalker/ou=tests/o=tests"));

        configDatabase.addDocument(databaseDocument, NCCONST.VIEWDATABASES,
            NCCONST.VIEWSECURITY);
        NotesDatabaseMock notesDatabase = new NotesDatabaseMock(
            "JediServer", "JediTrainingManuals.nsf", "jtmreplicaid0123");
        ((NotesSessionMock) session).addDatabase(notesDatabase);
        NotesACLMock acl = new NotesACLMock();
        acl.addAclEntry(new NotesACLEntryMock(
            "cn=Anakin Skywalker/ou=Tests/o=Tests", NotesACLEntry.TYPE_PERSON,
            NotesACL.LEVEL_AUTHOR, "[tacticsexpert]"));
        acl.addAclEntry(new NotesACLEntryMock("masters",
            NotesACLEntry.TYPE_PERSON_GROUP, NotesACL.LEVEL_AUTHOR,
            "[holderofopinions]"));
        notesDatabase.setACL(acl);

        databaseDocument = new NotesDocumentMock();
        databaseDocument.addItem(new NotesItemMock("name", NCCONST.DITM_DBNAME,
            "type", NotesItem.TEXT, "values", "testDb.nsf"));
        databaseDocument.addItem(new NotesItemMock("name",
            NCCONST.DITM_REPLICAID, "type", NotesItem.TEXT,
            "values", "testdb_replicaid"));
        databaseDocument.addItem(new NotesItemMock("name", NCCONST.DITM_SERVER,
            "type", NotesItem.TEXT, "values", "server"));
        databaseDocument.addItem(new NotesItemMock("name",
            NCCONST.NCITM_DBPERMITGROUPS, "type", NotesItem.TEXT,
            "values", "jedi"));
        configDatabase.addDocument(databaseDocument, NCCONST.VIEWDATABASES,
                NCCONST.VIEWSECURITY);

        testDb = new NotesDatabaseMock(
            "server", "testDb.nsf", "testdb_replicaid");
        ((NotesSessionMock) session).addDatabase(testDb);
        NotesACLMock testAcl = new NotesACLMock();
        testAcl.addAclEntry(new NotesACLEntryMock("jedi",
            NotesACLEntry.TYPE_PERSON_GROUP,
            NotesACL.LEVEL_READER, "[reader]"));
        testDb.setACL(testAcl);

        NotesUserGroupManager userGroupManager =
            connectorSession.getUserGroupManager();
        try {
          userGroupManager.setUpResources(true);
          userGroupManager.clearTables(userGroupManager.getConnection());
        } finally {
          userGroupManager.releaseResources();
        }
        userGroupManager.updateUsersGroups(true);
      }
    };
  }

  private NotesAuthorizationManager authorizationManager;

  public NotesAuthorizationManagerTest() {
    super();
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    authorizationManager =
        (NotesAuthorizationManager) connectorSession.getAuthorizationManager();
    connectorSession.getUserGroupManager().updateRoles(testDb);
  }

  public void testCheckAllowUserFullName() throws Exception {
    NotesDocumentMock databaseDocument = new NotesDocumentMock();
    databaseDocument.addItem(new NotesItemMock("name",
            NCCONST.NCITM_DBPERMITUSERS, "type", NotesItem.TEXT,
            "values", "cn=anakin skywalker/ou=tests/o=tests"));
    assertTrue(authorizationManager.checkAllowUser(databaseDocument,
            "cn=anakin skywalker/ou=tests/o=tests"));
  }

  public void testCheckAllowUserCommonName() throws Exception {
    NotesDocumentMock databaseDocument = new NotesDocumentMock();
    databaseDocument.addItem(new NotesItemMock("name",
            NCCONST.NCITM_DBPERMITUSERS, "type", NotesItem.TEXT,
            "values", "anakin skywalker"));
    assertTrue(authorizationManager.checkAllowUser(databaseDocument,
            "anakin skywalker"));
  }

  public void testCheckAllowUserDefault() throws Exception {
    NotesDocumentMock databaseDocument = new NotesDocumentMock();
    databaseDocument.addItem(new NotesItemMock("name",
            NCCONST.NCITM_DBPERMITUSERS, "type", NotesItem.TEXT,
            "values", "-default-"));
    assertTrue(authorizationManager.checkAllowUser(databaseDocument,
            "random user"));
  }

  public void testCheckAllowUserNotInList() throws Exception {
    NotesDocumentMock databaseDocument = new NotesDocumentMock();
    databaseDocument.addItem(new NotesItemMock("name",
            NCCONST.NCITM_DBPERMITUSERS, "type", NotesItem.TEXT,
            "values", "cn=anakin skywalker/ou=tests/o=tests"));
    assertFalse(authorizationManager.checkAllowUser(databaseDocument,
            "random user"));
  }

  public void testCheckDenyUserFullName() throws Exception {
    NotesDocumentMock databaseDocument = new NotesDocumentMock();
    databaseDocument.addItem(new NotesItemMock("name",
            NCCONST.NCITM_DBNOACCESSUSERS, "type", NotesItem.TEXT,
            "values", "cn=anakin skywalker/ou=tests/o=tests"));
    assertTrue(authorizationManager.checkDenyUser(databaseDocument,
            "cn=anakin skywalker/ou=tests/o=tests", "anakin skywalker"));
  }

  public void testCheckDenyUserCommonName() throws Exception {
    NotesDocumentMock databaseDocument = new NotesDocumentMock();
    databaseDocument.addItem(new NotesItemMock("name",
            NCCONST.NCITM_DBNOACCESSUSERS, "type", NotesItem.TEXT,
            "values", "anakin skywalker"));
    assertTrue(authorizationManager.checkDenyUser(databaseDocument,
            "cn=anakin skywalker/ou=tests/o=tests", "anakin skywalker"));
  }

  public void testCheckDenyUserNotInList() throws Exception {
    NotesDocumentMock databaseDocument = new NotesDocumentMock();
    databaseDocument.addItem(new NotesItemMock("name",
            NCCONST.NCITM_DBNOACCESSUSERS, "type", NotesItem.TEXT,
            "values", "anakin skywalker"));
    assertFalse(authorizationManager.checkDenyUser(
            databaseDocument, "foo"));
  }

  public void testCheckAllowGroup() throws Exception {
    NotesDocumentMock databaseDocument = new NotesDocumentMock();
    databaseDocument.addItem(new NotesItemMock("name",
            NCCONST.NCITM_DBPERMITGROUPS, "type", NotesItem.TEXT,
            "values", "masters"));
    ArrayList<String> groups = Lists.newArrayList("masters", "jedi");
    assertTrue(
        authorizationManager.checkAllowGroup(databaseDocument, groups));
  }

  public void testCheckDatabaseAccess() throws Exception {
    NotesDocumentMock databaseDocument = new NotesDocumentMock();
    databaseDocument.addItem(new NotesItemMock("name",
            NCCONST.NCITM_DBPERMITUSERS, "type", NotesItem.TEXT,
            "values", "CN=Anakin Skywalker/OU=Tests/O=Tests",
            "yoda"));
    databaseDocument.addItem(new NotesItemMock("name",
            NCCONST.NCITM_DBPERMITGROUPS, "type", NotesItem.TEXT,
            "values", "Masters"));
    databaseDocument.addItem(new NotesItemMock("name",
            NCCONST.NCITM_DBNOACCESSUSERS, "type", NotesItem.TEXT,
            "values", "Grievous"));
    databaseDocument.addItem(new NotesItemMock("name",
            NCCONST.NCITM_DBNOACCESSGROUPS, "type", NotesItem.TEXT,
            "values", "Separatists"));

    // Allowed access by name.
    User user = new User(1L, "cn=anakin skywalker/ou=tests/o=tests", "anakin");
    user.addGroup("foo");
    assertTrue(
        authorizationManager.checkDatabaseAccess(databaseDocument, user));
    user = new User(1L, "cn=yoda/ou=tests", "yoda");
    user.addGroup("foo");
    assertTrue(
        authorizationManager.checkDatabaseAccess(databaseDocument, user));
    // Denied access by name.
    user = new User(1L, "cn=grievous/ou=tests", "g");
    user.addGroup("foo");
    assertFalse(
        authorizationManager.checkDatabaseAccess(databaseDocument, user));
    // Allowed access by group.
    user = new User(1L, "cn=foo/ou=tests", "g");
    user.addGroup("masters");
    assertTrue(
        authorizationManager.checkDatabaseAccess(databaseDocument, user));
    // TODO: Denied access by group.
    //assertTrue(authorizationManager.checkDatabaseAccess(databaseDocument,
    // "foo", Lists.newArrayList("separatists")));
  }

  public void testCheckDocumentReaders() throws Exception {
    User user = new User(1L, "cn=anakin skywalker/ou=tests/o=tests", "anakin");
    user.addGroup("masters");
    user.addRole("jtmreplicaid0123", "[tacticsexpert]");

    // User in readers list.
    assertTrue(authorizationManager.checkDocumentReaders(user,
            Lists.newArrayList("cn=anakin skywalker/ou=tests/o=tests"),
            "jtmreplicaid0123"));
    // User common name in readers list.
    assertTrue(authorizationManager.checkDocumentReaders(user,
            Lists.newArrayList("anakin skywalker"),
            "jtmreplicaid0123"));
    // User group in readers list.
    assertTrue(authorizationManager.checkDocumentReaders(user,
            Lists.newArrayList("masters"),
            "jtmreplicaid0123"));
    // User role in readers list.
    assertTrue(authorizationManager.checkDocumentReaders(user,
            Lists.newArrayList("[tacticsexpert]"),
            "jtmreplicaid0123"));
  }

  public void testCheckDocumentReadersWithRoles() throws Exception {
    super.setUp();
    SimpleTraversalContext context = new SimpleTraversalContext();
    context.setSupportsInheritedAcls(true);
    ((TraversalContextAware) connectorSession.getTraversalManager())
        .setTraversalContext(context);

    User user =
        connectorSession.getUserGroupManager().getUserByGsaName("jsmith");
    assertNotNull(user);

    NotesDocumentMock notesDoc = createNotesDocument("server",
        "testdb_replicaid", "unid0001", ActionType.ADD);
    NotesConnectorDocument document = new NotesConnectorDocument(
        connectorSession, session, testDb);
    document.setCrawlDoc("unid0001", notesDoc);

    Collection<String> readers = new ArrayList<String>();
    getDocumentProperty(document, readers, SpiConstants.PROPNAME_ACLUSERS);
    getDocumentProperty(document, readers, SpiConstants.PROPNAME_ACLGROUPS);

    assertTrue(isMember(user, readers));
  }

  private boolean isMember(User user, Collection<String> readers) {
    for (String userRole : user.getRoles()) {
      for (String reader : readers) {
        if (reader.indexOf(userRole) > -1) {
          return true;
        }
      }
    }
    return false;
  }

  private void getDocumentProperty(NotesConnectorDocument doc,
      Collection<String> readers, String propName) throws Exception {
    Property prop = doc.findProperty(propName);
    assertNotNull(prop);
    Value v;
    while ((v = prop.nextValue()) != null) {
      String decoded = URLDecoder.decode(v.toString(), "UTF-8");
      readers.add(decoded.replaceFirst("Domino/", ""));
    }
  }

  private NotesDocumentMock createNotesDocument(String server, String replicaid,
      String unid, ActionType actionType) throws Exception {
    String docid = "http://" + server + "/" + replicaid + "/0/" + unid;
    NotesDocumentMock doc = new NotesDocumentMock();
    doc.addItem(new NotesItemMock("name", NCCONST.ITM_ACTION, "type",
            NotesItem.TEXT, "values", actionType));
    doc.addItem(new NotesItemMock("name", NCCONST.ITM_DOCID, "type",
            NotesItem.TEXT, "values", docid));
    doc.addItem(new NotesItemMock("name", NCCONST.NCITM_REPLICAID,
            "type", NotesItem.TEXT, "values", replicaid));
    doc.addItem(new NotesItemMock("name", NCCONST.ITM_MIMETYPE,
            "type", NotesItem.TEXT, "values", "text/plain"));
    doc.addItem(new NotesItemMock("name", NCCONST.ITM_ISPUBLIC,
            "type", NotesItem.TEXT, "values", "false"));
    doc.addItem(new NotesItemMock("name", NCCONST.NCITM_DOCAUTHORREADERS,
            "type", NotesItem.READERS, "values", "[reader]"));
    doc.addItem(new NotesItemMock("name", NCCONST.NCITM_AUTHTYPE,
            "type", NotesItem.TEXT, "values", NCCONST.AUTH_ACL));

    doc.addItem(new NotesItemMock("name", NCCONST.ITM_TITLE,
            "type", NotesItem.TEXT, "values", "Test document with readers"));
    doc.addItem(new NotesItemMock("name", NCCONST.ITM_GMETADESCRIPTION,
            "type", NotesItem.TEXT, "values",
            "Test parent groups with [reader] role"));
    doc.addItem(new NotesItemMock("name", NCCONST.ITM_CONTENT,
            "type", NotesItem.TEXT, "values",
            "Test parent groups with [reader] role where [reader] is in the readers field"));

    doc.addItem(new NotesItemMock("name", NCCONST.ITM_GMETADATABASE,
            "type", NotesItem.TEXT, "values", "Database Title"));
    doc.addItem(new NotesItemMock("name", NCCONST.ITM_GMETACATEGORIES,
            "type", NotesItem.TEXT, "values", "Category 1"));
    doc.addItem(new NotesItemMock("name", NCCONST.ITM_GMETAREPLICASERVERS,
            "type", NotesItem.TEXT, "values", server));
    doc.addItem(new NotesItemMock("name", NCCONST.ITM_GMETANOTESLINK,
            "type", NotesItem.TEXT, "values",
            "notes://" + server + "/" + replicaid));
    doc.addItem(new NotesItemMock("name", NCCONST.ITM_GMETAWRITERNAME,
            "type", NotesItem.TEXT, "values", "Walt Disney"));
    doc.addItem(new NotesItemMock("name", NCCONST.ITM_GMETAFORM,
            "type", NotesItem.TEXT, "values", "Discussion"));

    Date d = new Date();
    doc.addItem(new NotesItemMock("name", NCCONST.ITM_GMETALASTUPDATE,
            "type", NotesItem.TEXT, "values", d));
    doc.addItem(new NotesItemMock("name", NCCONST.ITM_GMETACREATEDATE,
            "type", NotesItem.TEXT, "values", d));
    return doc;
  }

  public void testAuthorizeDocidsUnknownUser() throws Exception {
    SimpleAuthenticationIdentity id = new SimpleAuthenticationIdentity("foo");
    ArrayList<String> docids = Lists.newArrayList("docid1", "docid2");
    Collection<AuthorizationResponse> response =
        authorizationManager.authorizeDocids(docids, id);
    assertEquals(docids.size(), response.size());
    for (AuthorizationResponse r : response) {
      assertFalse(r.isValid());
    }
  }

  public void testAuthorizeDocidsAllowedByDatabase() throws Exception {
    SimpleAuthenticationIdentity id =
        new SimpleAuthenticationIdentity("anakin");
    ArrayList<String> docids = Lists.newArrayList(
        "http://host:80/jtmreplicaid0123/0/E54902C71C28594F852578CE004B223B",
        "http://host:80/jtmreplicaid0123/0/E54902C71C28594F852578CE004B223C",
        "http://host:80/jtmreplicaid0123/0/E54902C71C28594F852578CE004B223D");
    Collection<AuthorizationResponse> response =
        authorizationManager.authorizeDocids(docids, id);
    assertEquals(docids.size(), response.size());
    for (AuthorizationResponse r : response) {
      assertTrue(r.isValid());
    }
  }
}
