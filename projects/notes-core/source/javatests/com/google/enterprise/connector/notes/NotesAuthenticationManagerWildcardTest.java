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

import com.google.enterprise.connector.notes.client.NotesSession;
import com.google.enterprise.connector.notes.client.mock.NotesDatabaseMock;
import com.google.enterprise.connector.notes.client.mock.NotesNameMock;
import com.google.enterprise.connector.notes.client.mock.SessionFactoryMock;
import com.google.enterprise.connector.spi.AuthenticationManager;
import com.google.enterprise.connector.spi.AuthenticationResponse;
import com.google.enterprise.connector.spi.Principal;
import com.google.enterprise.connector.spi.SimpleAuthenticationIdentity;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class NotesAuthenticationManagerWildcardTest extends TestCase {

  private static NotesConnector connector;
  private static SessionFactoryMock factory;
  private static NotesConnectorSession connectorSession;
  private static NotesSession session;
  private static NotesDatabaseMock namesDatabase;

  private static String[] names = {
      "CN=Lemon Grass/OU=West/O=ABC",
      "CN=Apple Pie/OU=South/OU=West/O=ABC",
      "CN=Ginger Bread/OU=North/OU=West/O=ABC",
      "CN=Red Beans/OU=North/OU=East/O=ABC",
      "CN=Green Beans/OU=South/OU=East/O=ABC",
      "CN=Soy Beans/OU=South/OU=East/O=ABC",
      "CN=Bitter Melon/OU=East/O=ABC"
  };

  private static String[] wildcards = {
      "*/O=ABC",
      "*/OU=West/O=ABC",
      "*/OU=South/OU=West/O=ABC",
      "*/OU=North/OU=West/O=ABC",
      "*/OU=East/O=ABC",
      "*/OU=South/OU=East/O=ABC",
      "*/OU=North/OU=East/O=ABC"
  };

  private static Map<String, NotesNameMock> notesNamesCache;

  public static Test suite() {
    return new TestSetup(
        new TestSuite(NotesAuthenticationManagerWildcardTest.class)) {
      protected void setUp() throws Exception {
        connector = NotesConnectorTest.getConnector();
        factory = (SessionFactoryMock) connector.getSessionFactory();
        NotesConnectorSessionTest.configureFactoryForSession(factory);
        connectorSession = (NotesConnectorSession) connector.login();
        session = connectorSession.createNotesSession();
        namesDatabase = (NotesDatabaseMock) session.getDatabase(
            connectorSession.getServer(), connectorSession.getDirectory());

        // Generate users.
        notesNamesCache = new HashMap<String, NotesNameMock>();
        for (String name : names) {
          NotesNameMock nameMock = new NotesNameMock(name);
          NotesUserGroupManagerTest.addNotesUser(connectorSession,
              namesDatabase, nameMock.getCanonical(), nameMock.getShortName());
          notesNamesCache.put(name, nameMock);
        }

        // Generate groups and members.
        NotesUserGroupManagerTest.addNotesGroup(namesDatabase,
            "wildcard_group", names);

        // Set parent_group for wildcard_group.
        NotesUserGroupManagerTest.addNotesGroup(namesDatabase,
            "parent_group", "wildcard_group");

        // Set nested_group for wildcard_group.
        NotesUserGroupManagerTest.addNotesGroup(namesDatabase,
            "wildcard_group", "nested_group");

        // Set circular group.
        NotesUserGroupManagerTest.addNotesGroup(namesDatabase,
            "nested_group", "parent_group");

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

  public NotesAuthenticationManagerWildcardTest() {
    super();
  }

  protected void setUp() throws Exception {
    super.setUp();
    authenticationManager = connectorSession.getAuthenticationManager();
  }

  public void testAuthenticateKnownUser() throws Exception {
    // Validate the first name in the list
    String user = names[0];
    NotesNameMock nameMock = notesNamesCache.get(user);
    AuthenticationResponse response = authenticationManager.authenticate(
        new SimpleAuthenticationIdentity(nameMock.getShortName(),"password"));
    assertTrue(user + " is valid: ", response.isValid());
    testUserGroups(connectorSession, response, user);
  }

  private void testUserGroups(NotesConnectorSession connectorSession,
      AuthenticationResponse response, String user) throws Exception {
    @SuppressWarnings("unchecked")
    Collection<Principal> groups =
        (Collection<Principal>) response.getGroups();
    assertNotNull("Missing groups", groups);
    assertEquals(groups.toString(), 8, groups.size());
    Collection<String> groupNames = new ArrayList<String>();
    for (Principal principal : groups) {
      groupNames.add(principal.getName());
    }
    
    // Test user's expanded wildcard groups
    assertTrue("wildcard_group",
        groupNames.contains("Domino%2Fwildcard_group"));
    assertTrue("parent_group", groupNames.contains("Domino%2Fparent_group"));
    assertTrue("nested_group", groupNames.contains("Domino%2Fnested_group"));
    assertTrue("/west/abc",
            groupNames.contains("Domino%2Fou%3Dwest%2Fo%3Dabc"));
    assertTrue("*/west/abc",
        groupNames.contains("Domino%2F*%2Fou%3Dwest%2Fo%3Dabc"));
    assertTrue("/abc", groupNames.contains("Domino%2Fo%3Dabc"));
    assertTrue("*/abc", groupNames.contains("Domino%2F*%2Fo%3Dabc"));
  }
}
