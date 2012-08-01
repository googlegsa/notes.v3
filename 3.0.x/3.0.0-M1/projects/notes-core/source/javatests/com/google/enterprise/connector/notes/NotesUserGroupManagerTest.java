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
import com.google.enterprise.connector.notes.client.mock.ViewNavFromCategoryCreator;
import com.google.enterprise.connector.spi.LocalDatabase;
import com.google.enterprise.connector.spi.Session;
import com.google.enterprise.connector.util.database.JdbcDatabase;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import javax.sql.DataSource;

public class NotesUserGroupManagerTest extends TestCase {

  private static NotesConnector connector;
  private static SessionFactoryMock factory;
  private static NotesConnectorSession connectorSession;
  private static NotesSession session;
  private static NotesDatabaseMock namesDatabase;
  private static final int groupCount = 8;
  private static final int userCount = 11;

  public static Test suite() {
    return new TestSetup(
        new TestSuite(NotesUserGroupManagerTest.class)) {
      protected void setUp() throws Exception {
        connector = NotesConnectorTest.getConnector();
        factory = (SessionFactoryMock) connector.getSessionFactory();
        NotesConnectorSessionTest.configureFactoryForSession(factory);
        connectorSession = (NotesConnectorSession) connector.login();
        session = connectorSession.createNotesSession();
        namesDatabase = (NotesDatabaseMock) session.getDatabase(
            connectorSession.getServer(), connectorSession.getDirectory());
        // Create helper views.
        namesDatabase.setViewFields("notesnamelookup", NCCONST.PITM_FULLNAME);
        namesDatabase.setViewFields("grouplookup", NCCONST.GITM_LISTNAME);
        NotesUserGroupManager userGroupManager =
            new NotesUserGroupManager(connectorSession);

        addNotesUser(namesDatabase, "cn=Anakin Skywalker/ou=Tests/o=Tests",
          "anakin");
        addNotesUser(namesDatabase, "cn=Ahsoka Tano/ou=Tests/o=Tests",
          "ahsoka");
        addNotesUser(namesDatabase, "cn=Obi-Wan Kenobi/ou=Tests/o=Tests",
          "kenobi");
        addNotesUser(namesDatabase, "cn=Mace Windu/ou=Tests/o=Tests",
          "mace");
        addNotesUser(namesDatabase, "cn=Yoda/ou=Tests/o=Tests",
          "yoda");
        addNotesUser(namesDatabase, "cn=Chancellor Palpatine/ou=Tests/o=Tests",
          "palpatine");
        addNotesUser(namesDatabase, "cn=Count Dooku/ou=Tests/o=Tests",
          "dooku");
        addNotesUser(namesDatabase, "cn=Padme Amidala/ou=Tests/o=Tests",
          "padme");
        addNotesUser(namesDatabase, "cn=Rex/ou=Tests/o=Tests", "rex");
        addNotesUser(namesDatabase, "cn=Cody/ou=Tests/o=Tests", "cody");
        addNotesUser(namesDatabase, "cn=Echo/ou=Tests/o=Tests", "echo");

        addNotesGroup(namesDatabase, "Jedi", "Masters", "Padawan Learners");
        addNotesGroup(namesDatabase, "Masters",
            "CN=Anakin Skywalker/OU=Tests/O=Tests",
            "CN=Obi-Wan Kenobi/OU=Tests/O=Tests",
            "CN=Mace Windu/OU=Tests/O=Tests",
            "CN=Yoda/OU=Tests/O=Tests");
        addNotesGroup(namesDatabase, "Padawan Learners",
          "cn=ahsoka tano/ou=tests/o=tests");
        addNotesGroup(namesDatabase, "Separatists",
          "cn=count dooku/ou=tests/o=tests");
        addNotesGroup(namesDatabase, "Senators",
            "cn=padme amidala/ou=tests/o=tests");
        addNotesGroup(namesDatabase, "Clones",
            "cn=rex/ou=tests/o=tests",
            "cn=cody/ou=tests/o=tests",
            "cn=echo/ou=tests/o=tests");
        addNotesGroup(namesDatabase, "Good Guys", "Jedi", "Senators", "Clones");
        addNotesGroup(namesDatabase, "Bad Guys", "Separatists",
          "cn=chancellor palpatine/ou=tests/o=tests");


        // Create some databases with ACLs to be crawled.

        // First, create the connector database records, then
        // create the database. For the purposes of
        // NotesUserGroupManager, we only need a database, no
        // documents.
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
        databaseDocument = new NotesDocumentMock();
        databaseDocument.addItem(new NotesItemMock("name", NCCONST.DITM_DBNAME,
            "type", NotesItem.TEXT, "values", "SenateExpenseReports.nsf"));
        databaseDocument.addItem(new NotesItemMock("name",
            NCCONST.DITM_REPLICAID, "type", NotesItem.TEXT,
            "values", "serreplicaid0123"));
        databaseDocument.addItem(new NotesItemMock("name", NCCONST.DITM_SERVER,
            "type", NotesItem.TEXT, "values", "SenateServer"));
        configDatabase.addDocument(databaseDocument, NCCONST.VIEWDATABASES);
        databaseDocument = new NotesDocumentMock();
        databaseDocument.addItem(new NotesItemMock("name", NCCONST.DITM_DBNAME,
            "type", NotesItem.TEXT, "values", "EvilSeparatistPlots.nsf"));
        databaseDocument.addItem(new NotesItemMock("name",
            NCCONST.DITM_REPLICAID, "type", NotesItem.TEXT,
            "values", "espreplicaid0123"));
        databaseDocument.addItem(new NotesItemMock("name", NCCONST.DITM_SERVER,
            "type", NotesItem.TEXT, "values", "SenateServer"));
        configDatabase.addDocument(databaseDocument, NCCONST.VIEWDATABASES);

        NotesDatabaseMock notesDatabase = new NotesDatabaseMock(
            "JediServer", "JediTrainingManuals.nsf", "jtmreplicaid0123");
        ((NotesSessionMock) session).addDatabase(notesDatabase);
        NotesACLMock acl = new NotesACLMock();
        acl.addAclEntry(new NotesACLEntryMock(
            "cn=Ahsoka Tano/ou=Tests/o=Tests", NotesACLEntry.TYPE_PERSON,
            NotesACL.LEVEL_READER, "[student]"));
        acl.addAclEntry(new NotesACLEntryMock(
            "cn=Anakin Skywalker/ou=Tests/o=Tests", NotesACLEntry.TYPE_PERSON,
            NotesACL.LEVEL_AUTHOR, "[tacticsexpert]"));
        acl.addAclEntry(new NotesACLEntryMock(
            "cn=Yoda/ou=Tests/o=Tests", NotesACLEntry.TYPE_PERSON,
            NotesACL.LEVEL_AUTHOR, "[philosopher]"));
        acl.addAclEntry(new NotesACLEntryMock("masters",
            NotesACLEntry.TYPE_PERSON_GROUP, NotesACL.LEVEL_AUTHOR,
            "[holderofopinions]", "[duplicategroupname]"));
        acl.addAclEntry(new NotesACLEntryMock("bad guys",
            NotesACLEntry.TYPE_PERSON_GROUP, NotesACL.LEVEL_NOACCESS));
        notesDatabase.setACL(acl);

        notesDatabase = new NotesDatabaseMock(
            "SenateServer", "SenateExpenseReports.nsf", "serreplicaid0123");
        ((NotesSessionMock) session).addDatabase(notesDatabase);
        acl = new NotesACLMock();
        acl.addAclEntry(new NotesACLEntryMock(
            "cn=Chancellor Palpatine/ou=Tests/o=Tests",
            NotesACLEntry.TYPE_PERSON, NotesACL.LEVEL_READER,
            "[removablerole]"));
        acl.addAclEntry(new NotesACLEntryMock("senators",
            NotesACLEntry.TYPE_PERSON_GROUP, NotesACL.LEVEL_AUTHOR,
            "[creator]", "[duplicategroupname]"));
        notesDatabase.setACL(acl);

        notesDatabase = new NotesDatabaseMock(
            "SenateServer", "EvilSeparatistPlots.nsf", "espreplicaid0123");
        ((NotesSessionMock) session).addDatabase(notesDatabase);
        acl = new NotesACLMock();
        acl.addAclEntry(new NotesACLEntryMock(
            "cn=Chancellor Palpatine/ou=Tests/o=Tests",
            NotesACLEntry.TYPE_PERSON, NotesACL.LEVEL_MANAGER,
            "[evilmastermind]"));
        acl.addAclEntry(new NotesACLEntryMock(
            "Count Dooku", // Test a short name in the ACL
            NotesACLEntry.TYPE_PERSON, NotesACL.LEVEL_AUTHOR,
            "[henchman]"));
        acl.addAclEntry(new NotesACLEntryMock(
            "bad guys", NotesACLEntry.TYPE_PERSON_GROUP,
            NotesACL.LEVEL_READER));
        notesDatabase.setACL(acl);

        try {
          userGroupManager.setUpResources(true);
          userGroupManager.clearTables(userGroupManager.getConnection());
        } finally {
          userGroupManager.releaseResources();
        }
      }

      protected void tearDown() throws Exception {
        connector.shutdown();
      }
    };
  }

  // Used by this class.
  private static NotesDocumentMock addNotesUser(
      NotesDatabaseMock namesDatabase, String notesname, String gsaname)
      throws Exception {
    return addNotesUser(connectorSession, namesDatabase, notesname, gsaname);
  }

  // Used by other test classes.
  static NotesDocumentMock addNotesUser(NotesConnectorSession connectorSession,
      NotesDatabaseMock namesDatabase, String notesname, String gsaname)
      throws Exception {
    NotesDocumentMock notesPerson = new NotesDocumentMock();
    notesPerson.addItem(new NotesItemMock("name", "unid",
            "type", NotesItem.TEXT, "values", gsaname));
    notesPerson.addItem(new NotesItemMock("name", NCCONST.PITM_FULLNAME,
            "type", NotesItem.TEXT, "values", notesname));
    notesPerson.addItem(new NotesItemMock("name", "HTTPPassword",
            "type", NotesItem.TEXT, "values", "password"));
    notesPerson.addItem(new NotesItemMock("name", NCCONST.ITMFORM,
            "type", NotesItem.TEXT, "values", NCCONST.DIRFORM_PERSON));
    notesPerson.addItem(new NotesItemMock("name",
            "evaluate_" + connectorSession.getUserNameFormula(),
            "type", NotesItem.TEXT, "values", gsaname));
    notesPerson.addItem(new NotesItemMock("name",
            "evaluate_" + connectorSession.getUserSelectionFormula(),
            "type", NotesItem.TEXT, "values", 1.0));
    namesDatabase.addDocument(notesPerson, NCCONST.DIRVIEW_USERS,
        NCCONST.DIRVIEW_PEOPLEGROUPFLAT, NCCONST.DIRVIEW_SERVERACCESS,
        NCCONST.DIRVIEW_VIMUSERS, "notesnamelookup");
    return notesPerson;
  }

  static NotesDocumentMock addNotesGroup(
      NotesDatabaseMock namesDatabase, String groupName,
      String... members) throws Exception {
    NotesDocumentMock notesGroup = new NotesDocumentMock();
    notesGroup.addItem(new NotesItemMock("name", "unid",
            "type", NotesItem.TEXT, "values", groupName));
    notesGroup.addItem(new NotesItemMock("name", NCCONST.GITM_LISTNAME,
            "type", NotesItem.TEXT, "values", groupName));
    notesGroup.addItem(new NotesItemMock("name", NCCONST.ITMFORM,
            "type", NotesItem.TEXT, "values", NCCONST.DIRFORM_GROUP));
    notesGroup.addItem(new NotesItemMock("name", NCCONST.GITM_GROUPTYPE,
            "type", NotesItem.TEXT, "values",
            NCCONST.DIR_ACCESSCONTROLGROUPTYPES));
    if (members != null && members.length > 0) {
      notesGroup.addItem(new NotesItemMock("name", NCCONST.GITM_MEMBERS,
              "type", NotesItem.TEXT, "values", members));
    }
    namesDatabase.addDocument(notesGroup,
        NCCONST.DIRVIEW_PEOPLEGROUPFLAT, NCCONST.DIRVIEW_SERVERACCESS,
        NCCONST.DIRVIEW_VIMGROUPS, "grouplookup");
    return notesGroup;
  }

  private NotesUserGroupManager userGroupManager;
  private Connection conn;
  private HashMap<String, Long> groups = new HashMap<String, Long>();
  private HashMap<Long, HashSet<Long>> groupChildren =
      new HashMap<Long, HashSet<Long>>();
  private HashMap<String, Long> notesUserNames = new HashMap<String, Long>();
  private HashMap<String, Long> gsaUserNames = new HashMap<String, Long>();
  private HashMap<Long, HashSet<Long>> userGroups =
      new HashMap<Long, HashSet<Long>>();
  private HashMap<String, Long> roles = new HashMap<String, Long>();
  private HashMap<Long, HashSet<Long>> userRoles =
      new HashMap<Long, HashSet<Long>>();
  private HashMap<Long, HashSet<Long>> groupRoles =
      new HashMap<Long, HashSet<Long>>();

  public NotesUserGroupManagerTest() {
    super();
  }

  protected void setUp() throws Exception {
    userGroupManager = new NotesUserGroupManager(connectorSession);
    userGroupManager.setUpResources(true);
    conn = userGroupManager.getConnection();
  }

  protected void tearDown() {
    userGroupManager.releaseResources();
  }

  public void testInitializeUserCache() throws Exception {
    JdbcDatabase jdbcDatabase =
        connectorSession.getConnector().getJdbcDatabase();
    assertTrue(jdbcDatabase.verifyTableExists(
            userGroupManager.userTableName, null));
    assertTrue(jdbcDatabase.verifyTableExists(
            userGroupManager.groupTableName, null));
    assertTrue(jdbcDatabase.verifyTableExists(
            userGroupManager.roleTableName, null));
    assertTrue(jdbcDatabase.verifyTableExists(
            userGroupManager.userGroupsTableName, null));
    assertTrue(jdbcDatabase.verifyTableExists(
            userGroupManager.userRolesTableName, null));
    assertTrue(jdbcDatabase.verifyTableExists(
            userGroupManager.groupRolesTableName, null));
    assertTrue(jdbcDatabase.verifyTableExists(
            userGroupManager.groupChildrenTableName, null));
  }

  public void testUpdateGroups() throws Exception {
    setUpGroups();
    assertEquals(groups.toString(), groupCount, groups.size());
    HashSet<Long> children = groupChildren.get(groups.get("good guys"));
    assertEquals(5, children.size());
    assertGroupHasChild("good guys", "jedi");
    assertGroupHasChild("good guys", "masters");
    assertGroupHasChild("good guys", "padawan learners");
    assertGroupHasChild("good guys", "senators");
    assertGroupHasChild("good guys", "clones");
    children = groupChildren.get(groups.get("jedi"));
    assertEquals(2, children.size());
    assertGroupHasChild("jedi", "masters");
    assertGroupHasChild("jedi", "padawan learners");
  }

  public void testUpdateUsers() throws Exception {
    setUpUsers();
    assertEquals(userCount, notesUserNames.size());
    assertUserHasGroup("ahsoka", "padawan learners");
    assertUserHasGroup("ahsoka", "jedi");
    assertUserHasGroup("ahsoka", "good guys");
    assertUserHasGroup("ahsoka", "ou=tests/o=tests");
    assertUserHasGroup("ahsoka", "o=tests");
    assertUserHasGroup("palpatine", "bad guys");
    assertUserHasGroup("palpatine", "ou=tests/o=tests");
    assertUserHasGroup("palpatine", "o=tests");
  }

  public void testUpdateRoles() throws Exception {
    setUpRoles();
    assertUserHasRole("palpatine", "espreplicaid0123/[evilmastermind]");
    assertUserHasRole("dooku", "espreplicaid0123/[henchman]");
    assertGroupHasRole("senators", "serreplicaid0123/[creator]");

    // Same role name in different databases.
    Long id1 = assertRoleExists("serreplicaid0123/[duplicategroupname]");
    Long id2 = assertRoleExists("jtmreplicaid0123/[duplicategroupname]");
    assertTrue(id1 != id2);
  }

  public void testRolesAclEntryDeleted() throws Exception {
    setUpRoles();
    assertUserHasRole("palpatine", "serreplicaid0123/[removablerole]");

    // Modify one of the previously-created ACLs (we don't
    // actually have editing support, so just recreate it here).
    NotesDatabaseMock notesDatabase = (NotesDatabaseMock) session.getDatabase(
        "SenateServer", "SenateExpenseReports.nsf");
    NotesACLMock originalAcl = (NotesACLMock) notesDatabase.getACL();
    try {
      NotesACLMock acl = new NotesACLMock();
      // Remove Palpatine from this database's ACL
      acl.addAclEntry(new NotesACLEntryMock("senators",
              NotesACLEntry.TYPE_PERSON_GROUP, NotesACL.LEVEL_AUTHOR,
              "[creator]", "[duplicategroupname]"));
      notesDatabase.setACL(acl);
      setUpRoles();
      assertRoleDoesNotExist("serreplicaid0123/[removablerole]");
      assertUserDoesNotHaveRole("palpatine",
          "serreplicaid0123/[removablerole]");
    } finally {
      notesDatabase.setACL(originalAcl);
    }
  }

  public void testDeleteRolesForDatabase() throws Exception {
    setUpRoles();
    assertRoleExists("serreplicaid0123/[removablerole]");
    HashSet<String> currentDatabases = new HashSet<String>();
    currentDatabases.add("jtmreplicaid0123");
    currentDatabases.add("espreplicaid0123");
    userGroupManager.checkDatabaseDeletions(currentDatabases);
    getRoleData();
    assertRoleDoesNotExist("serreplicaid0123/[removablerole]");
  }

  public void testUserDeletions() throws Exception {
    setUpRoles();

    // Find the Notes doc for the user to be deleted. Use a
    // user with a role.
    NotesView nameView = namesDatabase.getView("notesnamelookup");
    NotesDocumentMock doc = (NotesDocumentMock) nameView.getDocumentByKey(
        "cn=Yoda/ou=Tests/o=Tests");
    assertNotNull("No yoda", doc);
    try {
      Long id = assertUserExists("yoda");
      assertUserHasGroup("yoda", "jedi");
      assertUserHasRole("yoda", "jtmreplicaid0123/[philosopher]");

      // Delete the user from the Notes database. Call
      // checkUserDeletions and verify that the user was removed
      // from the user cache.
      doc.remove(true);
      userGroupManager.checkUserDeletions();
      getGroupData();
      getUserData();
      getRoleData();
      assertUserDoesNotExist("yoda");
      assertNull("user still has groups", userGroups.get(id));
      assertNull("user still has roles", userRoles.get(id));
    } finally {
      addNotesUser(namesDatabase, "cn=Yoda/ou=Tests/o=Tests",
          "yoda");
    }
  }

  public void testGroupDeletions() throws Exception {
    userGroupManager.updateGroups();
    userGroupManager.updateUsers();

    // Find the Notes doc for the group to be deleted.
    // Use a group with children.
    NotesView nameView = namesDatabase.getView("grouplookup");
    NotesDocumentMock doc = (NotesDocumentMock) nameView.getDocumentByKey(
        "jedi");
    assertNotNull("No jedi", doc);
    try {
      // Get the current user/group data and verify that the group is there.
      getGroupData();
      getUserData();

      Long id = groups.get("jedi");
      assertNotNull("missing test group before test", id);

      // Delete the group from the Notes database. Call
      // checkGroupDeletions and verify that the group was removed
      // from the group cache.
      doc.remove(true);
      userGroupManager.checkGroupDeletions();
      getGroupData();
      getUserData();
      assertGroupDoesNotExist("jedi");
      assertNull("group still has child groups", groupChildren.get(id));
      for (Long userId : userGroups.keySet()) {
        HashSet<Long> g = userGroups.get(userId);
        assertFalse(g.contains(id));
      }
    } finally {
      addNotesGroup(namesDatabase, "jedi", "masters", "padawan learners");
    }
  }

  public void testUserNoLongerSelected() throws Exception {
    userGroupManager.updateGroups();
    userGroupManager.updateUsers();

    // Find the Notes doc for the user to be deleted.
    NotesView nameView = namesDatabase.getView("notesnamelookup");
    NotesDocumentMock doc = (NotesDocumentMock) nameView.getDocumentByKey(
        "cn=Anakin Skywalker/ou=Tests/o=Tests");
    assertNotNull("No anakin", doc);
    try {
      // Get the current user/group data and verify that the user is there.
      getGroupData();
      getUserData();

      Long id = assertUserExists("anakin");
      assertNotNull("user has no groups", userGroups.get(id));

      // Change the result of the user selection formula. Call
      // checkUserDeletions and verify that the user was removed
      // from the user cache.
      doc.replaceItemValue(
          "evaluate_" + connectorSession.getUserSelectionFormula(),
          new NotesItemMock("name",
              "evaluate_" + connectorSession.getUserSelectionFormula(),
              "type", NotesItem.TEXT, "values", 0.0));

      userGroupManager.checkUserDeletions();
      getGroupData();
      getUserData();
      assertUserDoesNotExist("anakin");
      assertNull("user still has groups", userGroups.get(id));
    } finally {
      doc.replaceItemValue(
          "evaluate_" + connectorSession.getUserSelectionFormula(),
          new NotesItemMock("name",
              "evaluate_" + connectorSession.getUserSelectionFormula(),
              "type", NotesItem.TEXT, "values", 1.0));
    }
  }

  public void testChangeUserGsaName() throws Exception {
    setUpRoles();

    // Find the Notes doc for the user to be changed.
    NotesView nameView = namesDatabase.getView("notesnamelookup");
    NotesDocumentMock doc = (NotesDocumentMock) nameView.getDocumentByKey(
        "cn=Obi-Wan Kenobi/ou=Tests/o=Tests");
    assertNotNull("No kenobi", doc);
    try {
      assertUserExists("kenobi");
      assertUserDoesNotExist("ben");

      // Change the user name.
      doc.replaceItemValue(
          "evaluate_" + connectorSession.getUserNameFormula(),
          new NotesItemMock("name",
            "evaluate_" + connectorSession.getUserNameFormula(),
            "type", NotesItem.TEXT, "values", "ben"));

      setUpRoles();
      assertUserExists("ben");
      assertUserDoesNotExist("kenobi");
    } finally {
      doc.replaceItemValue(
          "evaluate_" + connectorSession.getUserNameFormula(),
          new NotesItemMock("name",
            "evaluate_" + connectorSession.getUserNameFormula(),
            "type", NotesItem.TEXT, "values", "kenobi"));
    }
  }

  public void testRunningRepeatedly() throws Exception {
    userGroupManager.clearTables(conn);
    for (int i = 0; i < 10; i++) {
      userGroupManager.updateUsersGroups(true);
      getGroupData();
      getUserData();
      assertEquals(groups.toString(), groupCount + 2, groups.size());
      assertEquals(userCount, notesUserNames.size());
    }
  }

  public void testGetUser() throws Exception {
    setUpRoles();

    NotesUserGroupManager.User user =
        userGroupManager.getUserByGsaName("yoda");
    assertYodaUserProperties(user);
    user = userGroupManager.getUserByNotesName("cn=yoda/ou=tests/o=tests");
    assertYodaUserProperties(user);
  }

  private void assertYodaUserProperties(
      NotesUserGroupManager.User user) throws Exception {
    assertEquals("yoda", user.getGsaName());
    assertEquals("cn=yoda/ou=tests/o=tests", user.getNotesName());
    Collection<String> groups = user.getGroups();
    assertEquals("size of: " + groups, 5, groups.size());
    assertTrue("good guys", groups.contains("good guys"));
    assertTrue("jedi", groups.contains("jedi"));
    assertTrue("masters", groups.contains("masters"));
    assertTrue("o=tests", groups.contains("o=tests"));
    assertTrue("ou=tests/o=tests", groups.contains("ou=tests/o=tests"));
    Collection<String> roles = user.getRoles();
    assertEquals("size of: " + roles, 3, roles.size());
    assertTrue("jtmreplicaid0123/[philosopher]",
        roles.contains("jtmreplicaid0123/[philosopher]"));
    assertTrue("jtmreplicaid0123/[holderofopinions]",
        roles.contains("jtmreplicaid0123/[holderofopinions]"));
    assertTrue("jtmreplicaid0123/[duplicategroupname]",
        roles.contains("jtmreplicaid0123/[duplicategroupname]"));

    roles = user.getRolesByDatabase("jtmreplicaid0123");
    assertEquals("size of: " + roles, 3, roles.size());
    assertTrue("[philosopher]", roles.contains("[philosopher]"));
    assertTrue("[holderofopinions]", roles.contains("[holderofopinions]"));
    assertTrue("[duplicategroupname]", roles.contains("[duplicategroupname]"));
  }

  public void testGetUserByGsaNameNoRoles() throws Exception {
    setUpRoles();

    NotesUserGroupManager.User user =
        userGroupManager.getUserByGsaName("cody");
    assertEquals("cody", user.getGsaName());
    assertEquals("cn=cody/ou=tests/o=tests", user.getNotesName());
    Collection<String> groups = user.getGroups();
    assertEquals("size of: " + groups, 4, groups.size());
    assertTrue("good guys", groups.contains("good guys"));
    assertTrue("jedi", groups.contains("clones"));
    assertTrue("o=tests", groups.contains("o=tests"));
    assertTrue("ou=tests/o=tests", groups.contains("ou=tests/o=tests"));
    Collection<String> roles = user.getRoles();
    assertEquals("size of: " + roles, 0, roles.size());
  }

  public void testMapNotesNamesToGsaNames() throws Exception {
    setUpUsers();

    Collection<String> gsaNames = userGroupManager.mapNotesNamesToGsaNames(
        session,
        Lists.newArrayList("cn=Padme Amidala/ou=Tests/o=Tests",
            "cn=Count Dooku/ou=Tests/o=Tests",
            "cn=Ahsoka Tano/ou=Tests/o=Tests"), false);
    assertEquals(3, gsaNames.size());
    assertTrue(gsaNames.contains("padme"));
    assertTrue(gsaNames.contains("dooku"));
    assertTrue(gsaNames.contains("ahsoka"));
  }

  public void testMapNotesNamesToGsaNamesCommonNames() throws Exception {
    setUpUsers();

    Collection<String> gsaNames = userGroupManager.mapNotesNamesToGsaNames(
        session,
        Lists.newArrayList("Padme Amidala",
            "Count Dooku",
            "Ahsoka Tano"), false);
    assertEquals(3, gsaNames.size());
    assertTrue(gsaNames.contains("padme"));
    assertTrue(gsaNames.contains("dooku"));
    assertTrue(gsaNames.contains("ahsoka"));
  }

  public void testMapNotesNamesToGsaNamesEmptyList() throws Exception {
    setUpUsers();

    Collection<String> gsaNames = userGroupManager.mapNotesNamesToGsaNames(
        session, null, false);
    assertEquals(0, gsaNames.size());
    gsaNames = userGroupManager.mapNotesNamesToGsaNames(session,
        Collections.emptyList(), false);
    assertEquals(0, gsaNames.size());  }

  public void testMapNotesNamesToGsaNamesInvalidUser() throws Exception {
    setUpUsers();

    Collection<String> gsaNames = userGroupManager.mapNotesNamesToGsaNames(
        session,
        Lists.newArrayList("cn=Padme Amidala/ou=Tests/o=Tests",
            "cn=Count Dooku/ou=Tests/o=Tests",
            "bad user",
            "bad user",
            "cn=Ahsoka Tano/ou=Tests/o=Tests"), false);
    assertEquals(3, gsaNames.size());
    assertTrue(gsaNames.contains("padme"));
    assertTrue(gsaNames.contains("dooku"));
    assertTrue(gsaNames.contains("ahsoka"));
  }

  public void testMapNotesNamesToGsaNamesRemoveUsers() throws Exception {
    setUpUsers();

    ArrayList<String> notesNames = Lists.newArrayList(
        "cn=Padme Amidala/ou=Tests/o=Tests",
        "cn=Count Dooku/ou=Tests/o=Tests",
        "bad user",
        "bad user",
        "cn=Ahsoka Tano/ou=Tests/o=Tests");
    Collection<String> gsaNames = userGroupManager.mapNotesNamesToGsaNames(
        session, notesNames, true);
    assertEquals(3, gsaNames.size());
    assertTrue(gsaNames.contains("padme"));
    assertTrue(gsaNames.contains("dooku"));
    assertTrue(gsaNames.contains("ahsoka"));
    assertEquals(2, notesNames.size());
  }

  private void setUpGroups() throws Exception {
    userGroupManager.updateGroups();
    getGroupData();
  }

  private void setUpUsers() throws Exception {
    userGroupManager.updateGroups();
    userGroupManager.updateUsers();
    getGroupData(); // Must be done after updateUsers
    getUserData();
  }

  private void setUpRoles() throws Exception {
    setUpUsers();
    userGroupManager.updateRoles();
    getRoleData();
  }

  private Long assertUserExists(String user) {
    Long userId = gsaUserNames.get(user);
    assertNotNull("Missing user: " + user, userId);
    return userId;
  }

  private void assertUserDoesNotExist(String user) {
    Long userId = gsaUserNames.get(user);
    assertNull("Has user: " + user, userId);
  }

  private Long assertGroupExists(String group) {
    Long groupId = groups.get(group);
    assertNotNull("Missing group: " + group, groupId);
    return groupId;
  }

  private void assertGroupDoesNotExist(String group) {
    Long groupId = groups.get(group);
    assertNull("Has group: " + group, groupId);
  }

  private Long assertRoleExists(String role) {
    Long roleId = roles.get(role);
    assertNotNull("Missing role: " + role, roleId);
    return roleId;
  }

  private void assertRoleDoesNotExist(String role) {
    Long roleId = roles.get(role);
    assertNull("Has role: " + role, roleId);
  }

  private void assertGroupHasChild(String parentGroup, String childGroup) {
    HashSet<Long> children = groupChildren.get(groups.get(parentGroup));
    assertTrue(parentGroup + "->" + childGroup,
        children.contains(groups.get(childGroup)));
  }

  private void assertUserHasGroup(String gsaname, String groupName) {
    Long userId = assertUserExists(gsaname);
    Long groupId = assertGroupExists(groupName);
    assertTrue(gsaname + "->" + groupName,
        userGroups.get(userId).contains(groupId));
  }

  private void assertUserHasRole(String gsaname, String role)
      throws Exception {
    Long roleId = assertRoleExists(role);
    Long userId = assertUserExists(gsaname);
    assertTrue(gsaname + " missing role: " + role,
        userRoles.get(userId).contains(roleId));
  }

  private void assertUserDoesNotHaveRole(String gsaname, String role) {
    Long roleId = roles.get(role);
    if (roleId == null) {
      return;
    }
    Long userId = assertUserExists(gsaname);
    assertFalse(gsaname + " has role: " + role,
        userRoles.get(userId).contains(roleId));
  }

  private void assertGroupHasRole(String group, String role)
      throws Exception {
    Long roleId = assertRoleExists(role);
    Long groupId = assertGroupExists(group);
    assertTrue(group + " missing role: " + role,
        groupRoles.get(groupId).contains(roleId));
  }

  private void getGroupData() throws Exception {
    groups.clear();
    groupChildren.clear();

    ResultSet rs = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
          ResultSet.CONCUR_READ_ONLY).executeQuery(
        "select * from " + userGroupManager.groupTableName);
    while (rs.next()) {
      groups.put(rs.getString("groupname"), rs.getLong("groupid"));
    }
    rs = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
          ResultSet.CONCUR_READ_ONLY).executeQuery(
        "select * from " + userGroupManager.groupChildrenTableName);
    while (rs.next()) {
      Long parentId = rs.getLong("parentgroupid");
      Long childId = rs.getLong("childgroupid");
      HashSet<Long> children = groupChildren.get(parentId);
      if (children == null) {
        children = new HashSet<Long>();
        groupChildren.put(parentId, children);
      }
      assertTrue("Duplicate group child: " + parentId + ", " + childId,
          children.add(childId));
      assertTrue("No group record for " + parentId,
          groups.containsValue(parentId));
      assertTrue("No group record for " + childId,
          groups.containsValue(childId));
    }
  }

  private void getUserData() throws Exception {
    notesUserNames.clear();
    gsaUserNames.clear();
    userGroups.clear();

    ResultSet rs = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
          ResultSet.CONCUR_READ_ONLY).executeQuery(
        "select * from " + userGroupManager.userTableName);
    while (rs.next()) {
      notesUserNames.put(rs.getString("notesname"), rs.getLong("userid"));
      gsaUserNames.put(rs.getString("gsaname"), rs.getLong("userid"));
    }
    rs = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
          ResultSet.CONCUR_READ_ONLY).executeQuery(
        "select * from " + userGroupManager.userGroupsTableName);
    while (rs.next()) {
      Long userId = rs.getLong("userid");
      Long groupId = rs.getLong("groupid");
      HashSet<Long> tmp = userGroups.get(userId);
      if (tmp == null) {
        tmp = new HashSet<Long>();
        userGroups.put(userId, tmp);
      }
      assertTrue("Duplicate group membership: " + userId + ", " + groupId,
          tmp.add(groupId));
      assertTrue("No user record for " + userId,
          gsaUserNames.containsValue(userId));
      assertTrue("No group record for " + groupId,
          groups.containsValue(groupId));
    }
  }

  private void getRoleData() throws Exception {
    roles.clear();
    userRoles.clear();
    groupRoles.clear();

    ResultSet rs = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
          ResultSet.CONCUR_READ_ONLY).executeQuery(
        "select * from " + userGroupManager.roleTableName);
    while (rs.next()) {
      roles.put(rs.getString("replicaid") + "/" + rs.getString("rolename"),
          rs.getLong("roleid"));
    }
    rs = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
        ResultSet.CONCUR_READ_ONLY).executeQuery(
            "select * from " + userGroupManager.userRolesTableName);
    while (rs.next()) {
      Long userId = rs.getLong("userid");
      Long roleId = rs.getLong("roleid");
      HashSet<Long> tmp = userRoles.get(userId);
      if (tmp == null) {
        tmp = new HashSet<Long>();
        userRoles.put(userId, tmp);
      }
      assertTrue("Duplicate user roles: " + userId + ", " + roleId,
          tmp.add(roleId));
      assertTrue("No user record for " + userId,
          gsaUserNames.containsValue(userId));
      assertTrue("No role record for " + roleId,
          roles.containsValue(roleId));
    }
    rs = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,
        ResultSet.CONCUR_READ_ONLY).executeQuery(
            "select * from " + userGroupManager.groupRolesTableName);
    while (rs.next()) {
      Long groupId = rs.getLong("groupid");
      Long roleId = rs.getLong("roleid");
      HashSet<Long> tmp = groupRoles.get(groupId);
      if (tmp == null) {
        tmp = new HashSet<Long>();
        groupRoles.put(groupId, tmp);
      }
      assertTrue("Duplicate group roles: " + groupId + ", " + roleId,
          tmp.add(roleId));
      assertTrue("No group record for " + groupId,
          groups.containsValue(groupId));
      assertTrue("No role record for " + roleId,
          roles.containsValue(roleId));
    }
  }

  private void printCache() throws Exception {
    System.out.println("**************************");
    System.out.println("Groups: " + groups.toString());
    System.out.println("Group children: " + groupChildren);
    System.out.println("Users: " + notesUserNames);
    System.out.println("Users: " + gsaUserNames);
    System.out.println("User groups: " + userGroups);
    System.out.println("Roles: " + roles);
    System.out.println("User roles: " + userRoles);
    System.out.println("Group roles: " + groupRoles);
    System.out.println("**************************");
  }
}

