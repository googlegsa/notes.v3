// Copyright 2011 Google Inc.
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
import com.google.enterprise.connector.notes.NotesTraversalManager;
import com.google.enterprise.connector.spi.AuthenticationManager;
import com.google.enterprise.connector.spi.AuthenticationResponse;
import com.google.enterprise.connector.spi.Connector;
import com.google.enterprise.connector.spi.Document;
import com.google.enterprise.connector.spi.DocumentList;
import com.google.enterprise.connector.spi.RepositoryException;
import com.google.enterprise.connector.spi.RepositoryLoginException;
import com.google.enterprise.connector.spi.Session;
import com.google.enterprise.connector.spi.SimpleAuthenticationIdentity;
import com.google.enterprise.connector.spi.SpiConstants;

import java.util.Collection;

public class NotesAuthenticationManagerTest extends ConnectorFixture {

  private String username;
  private String password;

  public NotesAuthenticationManagerTest() {
    super();
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    username = ConnectorFixture.getRequiredProperty(
        "javatest.authentication.username");
    password = ConnectorFixture.getRequiredProperty(
        "javatest.authentication.password");

    // Temporary fix for the need to create user/group cache.
    Session session = connector.login();
    NotesUserGroupManager ug = new NotesUserGroupManager();
    ug.updatePeopleGroups((NotesConnectorSession) session, true);
  }

  /**
   * Tests an invalid user name.
   *
   * @throws RepositoryException
   */
  public void testInvalidUser() throws RepositoryException {
    Session session = connector.login();
    AuthenticationManager manager = session.getAuthenticationManager();
    AuthenticationResponse response = manager.authenticate(
        new SimpleAuthenticationIdentity("not a real username"));
    assertFalse(response.isValid());
  }

  /**
   * Tests a valid user name.
   *
   * @throws RepositoryException
   */
  public void testValidUser() throws RepositoryException {
    Session session = connector.login();
    AuthenticationManager manager = session.getAuthenticationManager();
    AuthenticationResponse response = manager.authenticate(
        new SimpleAuthenticationIdentity(username, password));
    assertTrue("Failed to authenticate: " + username, response.isValid());
    Collection<String> groups = response.getGroups();
    if (groups != null) {
      String groupPrefix = ((NotesConnectorSession) session)
          .getGsaGroupPrefix();
      for (String group : groups) {
          assertTrue(group, group.startsWith(groupPrefix));
          assertTrue(group, group.indexOf(" ") < 0);
      }
    }
  }

  /**
   * Tests a valid user name with no password.
   *
   * @throws RepositoryException
   */
  public void testValidUserGroupResolutionOnly() throws RepositoryException {
    Session session = connector.login();
    AuthenticationManager manager = session.getAuthenticationManager();
    AuthenticationResponse response = manager.authenticate(
        new SimpleAuthenticationIdentity(username, null));
    assertTrue("Authenticated: " + username, response.isValid());
    Collection<String> groups = response.getGroups();
    if (groups != null) {
      String groupPrefix = ((NotesConnectorSession) session)
          .getGsaGroupPrefix();
      for (String group : groups) {
          assertTrue(group, group.startsWith(groupPrefix));
          assertTrue(group, group.indexOf(" ") < 0);
      }
    }
  }
}

