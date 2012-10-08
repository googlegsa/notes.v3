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

import com.google.enterprise.connector.notes.NotesConnector;
import com.google.enterprise.connector.notes.NotesConnectorSession;
import com.google.enterprise.connector.notes.client.NotesACL;
import com.google.enterprise.connector.notes.client.NotesACLEntry;
import com.google.enterprise.connector.notes.client.NotesDatabase;
import com.google.enterprise.connector.notes.client.NotesDocument;
import com.google.enterprise.connector.notes.client.NotesItem;
import com.google.enterprise.connector.notes.client.NotesSession;
import com.google.enterprise.connector.notes.client.NotesView;
import com.google.enterprise.connector.notes.client.mock.NotesACLMock;
import com.google.enterprise.connector.notes.client.mock.NotesACLEntryMock;
import com.google.enterprise.connector.notes.client.mock.NotesDatabaseMock;
import com.google.enterprise.connector.notes.client.mock.NotesDocumentMock;
import com.google.enterprise.connector.notes.client.mock.NotesItemMock;
import com.google.enterprise.connector.notes.client.mock.NotesSessionMock;
import com.google.enterprise.connector.notes.client.mock.SessionFactoryMock;
import com.google.enterprise.connector.spi.AuthenticationManager;
import com.google.enterprise.connector.spi.AuthenticationResponse;
import com.google.enterprise.connector.spi.Principal;
import com.google.enterprise.connector.spi.Session;
import com.google.enterprise.connector.spi.SimpleAuthenticationIdentity;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class NotesAuthenticationManagerTest extends TestCase {

  private static NotesConnector connector;
  private static SessionFactoryMock factory;
  private static NotesConnectorSession connectorSession;
  private static NotesSession session;
  private static NotesDatabaseMock namesDatabase;

  public static Test suite() {
    return new TestSetup(
        new TestSuite(NotesAuthenticationManagerTest.class)) {
      protected void setUp() throws Exception {
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
        NotesUserGroupManagerTest.addNotesGroup(namesDatabase,
            "masters",
            "cn=anakin skywalker/ou=tests/o=tests");
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
        configDatabase.addDocument(databaseDocument, NCCONST.VIEWDATABASES);
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

  private AuthenticationManager authenticationManager;

  public NotesAuthenticationManagerTest() {
    super();
  }

  protected void setUp() throws Exception {
    super.setUp();
    authenticationManager = connectorSession.getAuthenticationManager();
  }

  public void testAuthenticateUnknownUser() throws Exception {
    AuthenticationResponse response = authenticationManager
        .authenticate(new SimpleAuthenticationIdentity("foo"));
    assertFalse(response.isValid());
  }

  public void testAuthenticateKnownUserNoPassword() throws Exception {
    AuthenticationResponse response = authenticationManager
        .authenticate(new SimpleAuthenticationIdentity("anakin"));
    assertTrue("known user not valid", response.isValid());
    verifyGroups(connectorSession, response);
  }

  public void testAuthenticateKnownUserBadPassword() throws Exception {
    AuthenticationResponse response = authenticationManager
        .authenticate(new SimpleAuthenticationIdentity("anakin", "foo"));
    assertFalse("valid with bad password", response.isValid());
    verifyGroups(connectorSession, response);
  }

  public void testAuthenticateKnownUser() throws Exception {
    AuthenticationResponse response = authenticationManager.authenticate(
        new SimpleAuthenticationIdentity("anakin", "password"));
    assertTrue("known user not valid", response.isValid());
    verifyGroups(connectorSession, response);
  }

  private void verifyGroups(NotesConnectorSession connectorSession,
      AuthenticationResponse response) throws Exception {
    @SuppressWarnings("unchecked")
        Collection<Principal> groups =
        (Collection<Principal>) response.getGroups();
    assertNotNull("Missing groups", groups);
    assertEquals(groups.toString(), 9, groups.size());
    Collection<String> groupNames = new ArrayList<String>();
    for (Principal principal : groups) {
      groupNames.add(principal.getName());
    }

    String groupPrefix = connectorSession.getGsaGroupPrefix();
    assertTrue(groupNames + "/good guys",
        groupNames.contains(groupPrefix + "%2Fgood+guys"));
    assertTrue(groupNames + "/jedi", groupNames.contains(groupPrefix + "%2Fjedi"));
    assertTrue(groupNames + "/masters",
        groupNames.contains(groupPrefix + "%2Fmasters"));
    assertTrue(groupNames + "/o=tests",
        groupNames.contains(groupPrefix + "%2Fo%3Dtests"));
    assertTrue(groupNames + "/ou=tests/o=tests",
        groupNames.contains(groupPrefix + "%2Fou%3Dtests%2Fo%3Dtests"));
    assertTrue("*/ou=tests/o=tests",
        groupNames.contains(groupPrefix + "%2F*%2Fou%3Dtests%2Fo%3Dtests"));
    assertTrue("*/o=tests",
        groupNames.contains(groupPrefix + "%2F*%2Fo%3Dtests"));
    // Check for roles: one assigned to the user, one assigned to
    // one of the user's groups.
    assertTrue(groupNames + "/[tacticsexpert]",
        groupNames.contains(
            groupPrefix + "%2Fjtmreplicaid0123%2F%5Btacticsexpert%5D"));
    assertTrue(groupNames + "/[holderofopinions]",
        groupNames.contains(
            groupPrefix + "%2Fjtmreplicaid0123%2F%5Bholderofopinions%5D"));
  }
}
