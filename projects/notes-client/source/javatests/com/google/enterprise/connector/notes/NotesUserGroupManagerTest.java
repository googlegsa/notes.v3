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

import com.google.enterprise.connector.notes.NotesConnectorSession;
import com.google.enterprise.connector.notes.client.NotesSession;
import com.google.enterprise.connector.util.database.JdbcDatabase;

import junit.framework.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

public class NotesUserGroupManagerTest extends ConnectorFixture {

  private NotesConnectorSession connectorSession;
  private NotesSession session;
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
  // TODO: update these values when we have a standard test directory.
  private int userCount = 15;
  private int groupCount = 12;

  public NotesUserGroupManagerTest() {
    super();
  }

  protected void setUp() throws Exception {
    super.setUp();
    connectorSession = (NotesConnectorSession) connector.login();
    userGroupManager = new NotesUserGroupManager(connectorSession);
    userGroupManager.setUpResources(true);
    conn = userGroupManager.getConnection();
  }

  protected void tearDown() throws Exception {
    userGroupManager.releaseResources();
    super.tearDown();
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

  public void testRunningRepeatedly() throws Exception {
    userGroupManager.clearTables(conn);
    for (int i = 0; i < 10; i++) {
      userGroupManager.updateUsersGroups(true);
      getGroupData();
      getUserData();
      getRoleData();
      assertEquals(userCount, notesUserNames.size());
      assertEquals(groupCount, groups.size());
    }
  }

  /**
   * Test mapping canonical and common names.
   * TODO: abbreviated names?
   */
  public void testMapNotesNamesToGsaNames() throws Exception {
    getGroupData();
    getUserData();
    String[] names = new String[0];
    names = notesUserNames.keySet().toArray(names);
    for (int i = 0; i < names.length; i++) {
      if (i % 2 == 0) {
        names[i] = NotesAuthorizationManager.getCommonName(names[i]);
      }
    }
    Collection<String> gsaNames = userGroupManager.mapNotesNamesToGsaNames(
        userGroupManager.getNotesSession(),
        Arrays.asList(names), false);
    assertEquals(names.length, gsaNames.size());
    // TODO: consider adding an invalid name and verifying that
    // it's not included in the returned collection.
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
    getGroupData();
    getUserData();
    getRoleData();
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

