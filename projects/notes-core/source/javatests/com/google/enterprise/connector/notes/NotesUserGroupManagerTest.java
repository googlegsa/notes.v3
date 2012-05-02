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

import com.google.enterprise.connector.notes.NotesConnector;
import com.google.enterprise.connector.notes.NotesConnectorSession;
import com.google.enterprise.connector.notes.client.NotesDatabase;
import com.google.enterprise.connector.notes.client.NotesDocument;
import com.google.enterprise.connector.notes.client.NotesItem;
import com.google.enterprise.connector.notes.client.NotesSession;
import com.google.enterprise.connector.notes.client.NotesView;
import com.google.enterprise.connector.notes.client.mock.NotesDatabaseMock;
import com.google.enterprise.connector.notes.client.mock.NotesDocumentMock;
import com.google.enterprise.connector.notes.client.mock.NotesItemMock;
import com.google.enterprise.connector.notes.client.mock.SessionFactoryMock;
import com.google.enterprise.connector.spi.Session;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class NotesUserGroupManagerTest extends TestCase {

  private NotesConnector connector;
  private SessionFactoryMock factory;

  public NotesUserGroupManagerTest() {
    super();
  }

  protected void setUp() throws Exception {
    super.setUp();
    connector = NotesConnectorTest.getConnector();
    factory = (SessionFactoryMock) connector.getSessionFactory();
    NotesConnectorSessionTest.configureFactoryForSession(factory);
  }

  protected void tearDown() {
    if (connector != null) {
      connector.shutdown();
    }
  }

  public void testGetGsaGroups() throws Exception {
    Vector<String> groups = new Vector<String>();
    groups.add("group 1");
    groups.add("group 2");
    groups.add("group 3");

    NotesConnectorSession connectorSession =
        (NotesConnectorSession) connector.login();
    Collection<String> gsaGroups = NotesUserGroupManager.getGsaGroups(
        connectorSession, groups);
    assertEquals(3, gsaGroups.size());
    for (int i = 1; i <= gsaGroups.size(); i++) {
      assertTrue(gsaGroups.toString(),
          gsaGroups.contains("Domino%2Fgroup+" + i));
    }
  }

  public void testGetGsaUsers() throws Exception {
    Vector<String> users = new Vector<String>();
    users.add("cn=Test User/ou=Tests/o=Tests");
    users.add("cn=Test User/ou=Tests/o=Tests");

    NotesConnectorSession connectorSession =
        (NotesConnectorSession) connector.login();
    NotesSession session = connectorSession.createNotesSession();
    NotesDatabaseMock connectorDatabase =
        (NotesDatabaseMock) session.getDatabase(
        connectorSession.getServer(), connectorSession.getDatabase());
    Collection<String> gsaUsers = NotesUserGroupManager.getGsaUsers(
        connectorSession, connectorDatabase, users);
    assertEquals(1, gsaUsers.size());
    assertTrue(gsaUsers.contains("testuser"));
  }

  public void testGetGsaUsersUnknownUser() throws Exception {
    Vector<String> users = new Vector<String>();
    users.add("cn=Test User/ou=Tests/o=Tests");
    users.add("foo");

    NotesConnectorSession connectorSession =
        (NotesConnectorSession) connector.login();
    NotesSession session = connectorSession.createNotesSession();
    NotesDatabaseMock connectorDatabase =
        (NotesDatabaseMock) session.getDatabase(
        connectorSession.getServer(), connectorSession.getDatabase());
    Collection<String> gsaUsers = NotesUserGroupManager.getGsaUsers(
        connectorSession, connectorDatabase, users);
    assertEquals(1, gsaUsers.size());
    assertTrue(gsaUsers.contains("testuser"));
  }

  public void testGetGsaUsersRemoveUsers() throws Exception {
    Vector<String> users = new Vector<String>();
    users.add("cn=Test User/ou=Tests/o=Tests");
    users.add("foo");
    users.add("GroupName");

    NotesConnectorSession connectorSession =
        (NotesConnectorSession) connector.login();
    NotesSession session = connectorSession.createNotesSession();
    NotesDatabaseMock connectorDatabase =
        (NotesDatabaseMock) session.getDatabase(
        connectorSession.getServer(), connectorSession.getDatabase());
    Collection<String> gsaUsers = NotesUserGroupManager.getGsaUsers(
        connectorSession, connectorDatabase, users, true);
    assertEquals(1, gsaUsers.size());
    assertTrue(gsaUsers.contains("testuser"));

    assertEquals(2, users.size());
    assertTrue(users.contains("foo"));
    assertTrue(users.contains("GroupName"));
  }

  // There are three groups (two groups and a role) in the test
  // data by default.
  public void testReplaceRoleGroupsForUser() throws Exception {
    Vector<String> roles = new Vector<String>();
    roles.add("[superhero]");
    NotesConnectorSession connectorSession =
        (NotesConnectorSession) connector.login();
    NotesSession session = connectorSession.createNotesSession();
    NotesDatabaseMock connectorDatabase =
        (NotesDatabaseMock) session.getDatabase(
        connectorSession.getServer(), connectorSession.getDatabase());

    NotesView people = connectorDatabase.getView(NCCONST.VIEWNOTESNAMELOOKUP);
    NotesDocument personDoc = NotesUserGroupManager.findPersonDocument(
        people, "Test User");
    assertNotNull(personDoc);
    Vector currentGroups = personDoc.getItemValue(NCCONST.PCITM_GROUPS);
    assertEquals(3, currentGroups.size());

    NotesUserGroupManager.replaceRoleGroupsForUser(
        connectorSession, connectorDatabase, "Test User",
        "dbwithrolesid", roles);

    currentGroups = personDoc.getItemValue(NCCONST.PCITM_GROUPS);
    assertEquals(4, currentGroups.size());
    assertTrue(currentGroups.contains("dbwithrolesid/[superhero]"));

    roles.clear();
    roles.add("[sidekick]");
    roles.add("[cook]");
    NotesUserGroupManager.replaceRoleGroupsForUser(
        connectorSession, connectorDatabase, "Test User",
        "dbwithrolesid", roles);

    currentGroups = personDoc.getItemValue(NCCONST.PCITM_GROUPS);
    assertEquals(5, currentGroups.size());
    assertFalse(currentGroups.contains("dbwithrolesid/[superhero]"));
    assertTrue(currentGroups.contains("dbwithrolesid/[sidekick]"));
    assertTrue(currentGroups.contains("dbwithrolesid/[cook]"));

    roles.clear();
    NotesUserGroupManager.replaceRoleGroupsForUser(
        connectorSession, connectorDatabase, "Test User",
        "dbwithrolesid", roles);
    // TODO: this removes the other existing role too? See
    // NotesConnectorSessionTest. Maybe use a different replica
    // id for this test?
    currentGroups = personDoc.getItemValue(NCCONST.PCITM_GROUPS);
    assertEquals(3, currentGroups.size());
    assertFalse(currentGroups.contains("dbwithrolesid/[sidekick]"));
    assertFalse(currentGroups.contains("dbwithrolesid/[cook]"));
  }

  // Can't test having replaceRoleGroupsForGroup create the
  // group; the mock client has its own way to add docs to
  // groups.
  public void testReplaceRoleGroupsForGroup() throws Exception {
    NotesConnectorSession connectorSession =
        (NotesConnectorSession) connector.login();
    NotesSession session = connectorSession.createNotesSession();
    NotesDatabaseMock connectorDatabase =
        (NotesDatabaseMock) session.getDatabase(
        connectorSession.getServer(), connectorSession.getDatabase());

    Vector<String> roles = new Vector<String>();
    roles.add("[superhero]");

    NotesUserGroupManager.replaceRoleGroupsForGroup(
        connectorSession, connectorDatabase, "Group1",
        "replicaid", roles);

    NotesView groups = connectorDatabase.getView(NCCONST.VIEWGROUPCACHE);
    NotesDocument groupDoc = groups.getDocumentByKey("Group1", true);
    assertNotNull(groupDoc);
    Vector currentGroups = groupDoc.getItemValue(NCCONST.GCITM_GROUPROLES);
    assertEquals(1, currentGroups.size());
    assertTrue(currentGroups.contains("replicaid/[superhero]"));

    roles.clear();
    roles.add("[sidekick]");
    roles.add("[cook]");
    NotesUserGroupManager.replaceRoleGroupsForGroup(
        connectorSession, connectorDatabase, "Group1",
        "replicaid", roles);

    currentGroups = groupDoc.getItemValue(NCCONST.GCITM_GROUPROLES);
    assertEquals(2, currentGroups.size());
    assertFalse(currentGroups.contains("replicaid/[superhero]"));
    assertTrue(currentGroups.contains("replicaid/[sidekick]"));
    assertTrue(currentGroups.contains("replicaid/[cook]"));

    roles.clear();
    NotesUserGroupManager.replaceRoleGroupsForGroup(
        connectorSession, connectorDatabase, "Group1",
        "replicaid", roles);

    currentGroups = groupDoc.getItemValue(NCCONST.GCITM_GROUPROLES);
    assertEquals(0, currentGroups.size());
  }
}

