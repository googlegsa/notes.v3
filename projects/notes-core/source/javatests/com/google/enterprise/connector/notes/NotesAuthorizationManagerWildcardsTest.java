//Copyright 2012 Google Inc.
//
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.

package com.google.enterprise.connector.notes;

import com.google.enterprise.connector.notes.NotesUserGroupManager.User;
import com.google.enterprise.connector.notes.client.NotesItem;
import com.google.enterprise.connector.notes.client.NotesSession;
import com.google.enterprise.connector.notes.client.mock.NotesDatabaseMock;
import com.google.enterprise.connector.notes.client.mock.NotesDocumentMock;
import com.google.enterprise.connector.notes.client.mock.NotesItemMock;
import com.google.enterprise.connector.notes.client.mock.NotesSessionMock;
import com.google.enterprise.connector.notes.client.mock.SessionFactoryMock;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.HashMap;

/*
* checkDatabaseAccess
* -------------------
* Database document contains
*   PERMITGROUPS = * /c=UK
* All UK users should be authorized by checkDatabaseAccess
* Non-UK users should not be authorized by checkDatabaseAccess
*
* Database document contains
*   PERMITGROUPS = * /ou=Gryffindor/o=Hogwarts/c=UK
* Gryffindor users should be authorized by checkDatabaseAccess
* All other users should not be authorized by checkDatabaseAccess
*
* Database document contains
*   PERMITGROUPS = * /o=Hogwarts/c=UK
* Hogwarts users should be authorized by checkDatabaseAccess
* All other users should not be authorized by checkDatabaseAccess
*
* checkDocumentReaders
* --------------------
* Database document contains
*   PERMITUSERS = "-default"
* Document readers contains
*   * /o=Durmstrang/c=BGR
* Krum should be allowed access
* No other users should be allowed access
*
* authorizeDocids
* ---------------
* TODO
*
* TODO: set up nested groups and test access
*/
public class NotesAuthorizationManagerWildcardsTest extends TestCase {

private static NotesConnector connector;
private static SessionFactoryMock factory;
private static NotesConnectorSession connectorSession;
private static NotesSession session;
private static NotesDatabaseMock namesDatabase;
private static NotesDocumentMock hogwartsDatabaseDocument;
private static NotesUserGroupManager userGroupManager;
private static HashMap<String, User> users = new HashMap<String, User>();

  public static Test suite() {
    return new TestSetup(new TestSuite(
            NotesAuthorizationManagerWildcardsTest.class)) {
      @Override protected void setUp() throws Exception {
        connector = NotesConnectorTest.getConnector();
        factory = (SessionFactoryMock) connector.getSessionFactory();
        NotesConnectorSessionTest.configureFactoryForSession(factory);
        connectorSession = (NotesConnectorSession) connector.login();
        session = connectorSession.createNotesSession();
        namesDatabase = (NotesDatabaseMock) session.getDatabase(
            connectorSession.getServer(), connectorSession.getDirectory());

        NotesUserGroupManagerTest.addNotesUser(connectorSession, namesDatabase,
            "", "");
        NotesUserGroupManagerTest.addNotesUser(connectorSession, namesDatabase,
            "cn=Harry Potter/ou=Gryffindor/o=Hogwarts/c=UK", "harry");
        NotesUserGroupManagerTest.addNotesUser(connectorSession, namesDatabase,
            "cn=Hermione Granger/ou=Gryffindor/o=Hogwarts/c=UK", "Hermione");
        NotesUserGroupManagerTest.addNotesUser(connectorSession, namesDatabase,
            "cn=Ron Weasley/ou=Gryffindor/o=Hogwarts/c=UK", "Ron");
        NotesUserGroupManagerTest.addNotesUser(connectorSession, namesDatabase,
            "cn=Luna Lovegood/ou=Ravenclaw/o=Hogwarts/c=UK", "Luna");
        NotesUserGroupManagerTest.addNotesUser(connectorSession, namesDatabase,
            "cn=Cho Chang/ou=Ravenclaw/o=Hogwarts/c=UK", "Cho");
        NotesUserGroupManagerTest.addNotesUser(connectorSession, namesDatabase,
            "cn=Draco Malfoy/ou=Slytherin/o=Hogwarts/c=UK", "Draco");
        NotesUserGroupManagerTest.addNotesUser(connectorSession, namesDatabase,
            "cn=Cedric Diggory/ou=Hufflepuff/o=Hogwarts/c=UK", "Cedric");
        NotesUserGroupManagerTest.addNotesUser(connectorSession, namesDatabase,
            "cn=Albus Dumbledore/ou=Professors/o=Hogwarts/c=UK",
            "AlbusDumbledore");
        NotesUserGroupManagerTest
            .addNotesUser(
                connectorSession,
                namesDatabase,
                "cn=Arthur Weasley/ou=Misuse of Muggle Artifacts/o=Ministry of Magic/c=UK",
                "Arthur");
        NotesUserGroupManagerTest.addNotesUser(connectorSession, namesDatabase,
            "cn=Fleur Delacour/o=Beauxbatons/c=FR", "Fleur");
        NotesUserGroupManagerTest.addNotesUser(connectorSession, namesDatabase,
            "cn=Viktor Krum/o=Durmstrang/c=BGR", "Viktor");

        // Create wildcard, parent and nested groups.
        NotesUserGroupManagerTest.addNotesGroup(namesDatabase, 
            "Gryffindor_Group",
            "cn=Harry Potter/ou=Gryffindor/o=Hogwarts/c=UK",
            "cn=Hermione Granger/ou=Gryffindor/o=Hogwarts/c=UK",
            "cn=Ron Weasley/ou=Gryffindor/o=Hogwarts/c=UK");
        NotesUserGroupManagerTest.addNotesGroup(namesDatabase,
            "parent_group", "Gryffindor_Group");
        NotesUserGroupManagerTest.addNotesGroup(namesDatabase,
            "Gryffindor_Group", "nested_group");
        
        // Create a database with an ACL with wildcards.
        NotesDatabaseMock configDatabase = (NotesDatabaseMock) session
            .getDatabase(connectorSession.getServer(),
                connectorSession.getDatabase());
        // hogwartsDatabaseDocument is the connector's database
        // document caching the database configuration.
        hogwartsDatabaseDocument = new NotesDocumentMock();
        hogwartsDatabaseDocument.addItem(getTextItem(NCCONST.DITM_DBNAME,
            "HogwartsDocs.nsf"));
        hogwartsDatabaseDocument.addItem(getTextItem(NCCONST.DITM_REPLICAID,
            "hogreplicaid0123"));
        hogwartsDatabaseDocument.addItem(getTextItem(NCCONST.DITM_SERVER,
            "SchoolServer"));
        hogwartsDatabaseDocument.addItem(getTextItem(NCCONST.NCITM_UNID,
            "hogwartsdbdocunid"));
        configDatabase.addDocument(hogwartsDatabaseDocument,
            NCCONST.VIEWDATABASES, NCCONST.VIEWSECURITY);

        // notesDatabase is the actual database being crawled.
        NotesDatabaseMock notesDatabase = new NotesDatabaseMock("SchoolServer",
            "HogwartsDocs.nsf", "hogreplicaid0123");
        ((NotesSessionMock) session).addDatabase(notesDatabase);

        userGroupManager = connectorSession.getUserGroupManager();
        try {
          userGroupManager.setUpResources(true);
          userGroupManager.clearTables(userGroupManager.getConnection());
        } finally {
          userGroupManager.releaseResources();
        }
        userGroupManager.updateUsersGroups(true);
        getUsers("harry", "hermione", "ron", "luna", "cho", "draco", "cedric",
            "albusdumbledore", "arthur", "fleur", "viktor");
      }
    };
  }

  private static NotesItemMock getTextItem(String name, String... values) {
    return new NotesItemMock("name", name, "type", NotesItem.TEXT, "values",
        values);
  }

  private static void getUsers(String... names) {
    for (String name : names) {
      users.put(name, userGroupManager.getUserByGsaName(name));
    }
  }

  private NotesAuthorizationManager authorizationManager;

  public NotesAuthorizationManagerWildcardsTest() {
    super();
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    authorizationManager = (NotesAuthorizationManager) connectorSession
        .getAuthorizationManager();
  }

  @Override
  protected void tearDown() throws Exception {
    // Clean up database document permit/deny so each test can
    // start with what it wants to test.
    hogwartsDatabaseDocument.removeItem(NCCONST.NCITM_DBPERMITGROUPS);
    super.tearDown();
  }

  public void testDatabaseWildcardAccess() throws Exception {
    // TODO: create the database document using
    // NotesDatabasePoller.getPermitDeny instead of manually
    // recreating the data.
    hogwartsDatabaseDocument.addItem(new NotesItemMock("name",
        NCCONST.NCITM_DBPERMITGROUPS, "type", NotesItem.TEXT, "values",
        "*/o=Hogwarts/c=UK"));
    hasDatabaseAccess(hogwartsDatabaseDocument, true, "harry", "hermione",
        "ron", "luna", "cho", "draco", "cedric", "albusdumbledore");
    hasDatabaseAccess(hogwartsDatabaseDocument, false, "arthur", "fleur",
        "viktor");
  }

  public void testDatabaseGroupAccess() throws Exception {
    // TODO: create the database document using
    // NotesDatabasePoller.getPermitDeny instead of manually
    // recreating the data.
    hogwartsDatabaseDocument.addItem(new NotesItemMock("name",
        NCCONST.NCITM_DBPERMITGROUPS, "type", NotesItem.TEXT, "values",
        "Gryffindor_Group"));
    hasDatabaseAccess(hogwartsDatabaseDocument, true, "harry", "hermione",
        "ron");
    hasDatabaseAccess(hogwartsDatabaseDocument, false, "fleur", "viktor");
  }

  public void testDatabaseNestedGroupAccess_parent() throws Exception {
    // TODO: create the database document using
    // NotesDatabasePoller.getPermitDeny instead of manually
    // recreating the data.
    
    // The Gryffindor_Group group is nested inside the
    // parent_group; therefore, all members should be mapped to parent_group.
    hogwartsDatabaseDocument.addItem(new NotesItemMock("name",
        NCCONST.NCITM_DBPERMITGROUPS, "type", NotesItem.TEXT, "values",
        "parent_group"));
    hasDatabaseAccess(hogwartsDatabaseDocument, true, "harry", "hermione",
        "ron");
    hasDatabaseAccess(hogwartsDatabaseDocument, false, "fleur", "viktor");
  }

  public void testDatabaseNestedGroupAccess_child() throws Exception {
    // Since nested_group group is nested inside Gryffindor_Group group, all
    // members should not be mapped to nested_group
    hogwartsDatabaseDocument.addItem(new NotesItemMock("name",
        NCCONST.NCITM_DBPERMITGROUPS, "type", NotesItem.TEXT, "values",
        "nested_group"));
    hasDatabaseAccess(hogwartsDatabaseDocument, false, "harry", "hermione",
        "ron");
  }

  private void hasDatabaseAccess(NotesDocumentMock databaseDocument,
      boolean hasAccess, String... names) throws Exception {
    for (String name : names) {
      if (hasAccess) {
        assertTrue(name + " isn't authorized",
            authorizationManager.checkDatabaseAccess(databaseDocument,
                users.get(name)));
      } else {
        assertFalse(name + " is authorized",
            authorizationManager.checkDatabaseAccess(databaseDocument,
                users.get(name)));
      }
    }
  }
}
