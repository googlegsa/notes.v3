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
import com.google.enterprise.connector.notes.client.NotesDatabase;
import com.google.enterprise.connector.notes.client.NotesDocument;
import com.google.enterprise.connector.notes.client.NotesItem;
import com.google.enterprise.connector.notes.client.NotesSession;
import com.google.enterprise.connector.notes.client.NotesView;
import com.google.enterprise.connector.notes.client.mock.NotesDatabaseMock;
import com.google.enterprise.connector.notes.client.mock.NotesDocumentMock;
import com.google.enterprise.connector.notes.client.mock.NotesItemMock;
import com.google.enterprise.connector.notes.client.mock.SessionFactoryMock;
import com.google.enterprise.connector.spi.AuthenticationManager;
import com.google.enterprise.connector.spi.AuthenticationResponse;
import com.google.enterprise.connector.spi.Principal;
import com.google.enterprise.connector.spi.Session;
import com.google.enterprise.connector.spi.SimpleAuthenticationIdentity;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class NotesAuthenticationManagerTest extends TestCase {

  private NotesConnector connector;
  private SessionFactoryMock factory;

  public NotesAuthenticationManagerTest() {
    super();
  }

  protected void setUp() throws Exception {
    super.setUp();
    connector = NotesConnectorTest.getConnector();
    factory = (SessionFactoryMock) connector.getSessionFactory();
    NotesConnectorSessionTest.configureFactoryForSession(factory);
  }

  protected void tearDown() {
    if (null != connector) {
      connector.shutdown();
    }
  }

  public void testAuthenticateUnknownUser() throws Exception {
    NotesConnectorSession connectorSession =
        (NotesConnectorSession) connector.login();
    AuthenticationManager authN = connectorSession.getAuthenticationManager();
    AuthenticationResponse response =
        authN.authenticate(new SimpleAuthenticationIdentity("foo"));
    assertFalse(response.isValid());
  }

  public void testAuthenticateKnownUserNoPassword() throws Exception {
    NotesConnectorSession connectorSession =
        (NotesConnectorSession) connector.login();
    AuthenticationManager authN = connectorSession.getAuthenticationManager();
    AuthenticationResponse response =
        authN.authenticate(new SimpleAuthenticationIdentity("testuser"));
    assertTrue(response.isValid());
    verifyGroups(connectorSession, response);
  }

  public void testAuthenticateKnownUser() throws Exception {
    NotesConnectorSession connectorSession =
        (NotesConnectorSession) connector.login();
    AuthenticationManager authN = connectorSession.getAuthenticationManager();
    AuthenticationResponse response = authN.authenticate(
        new SimpleAuthenticationIdentity("testuser", "password"));
    assertTrue(response.isValid());
    verifyGroups(connectorSession, response);
  }

  // Checks the default test data created in
  // NotesConnectorSessionTest.configureFactoryForSession.
  private void verifyGroups(NotesConnectorSession connectorSession,
      AuthenticationResponse response) throws Exception {
    @SuppressWarnings("unchecked")
        Collection<Principal> groups =
        (Collection<Principal>) response.getGroups();
    assertNotNull("Missing groups", groups);
    assertEquals(4, groups.size());
    Collection<String> groupNames = new ArrayList<String>();
    for (Principal principal : groups) {
      groupNames.add(principal.getName());
    }
    String groupPrefix = connectorSession.getGsaGroupPrefix();
    assertTrue("Missing group 1: " + groupNames,
        groupNames.contains(groupPrefix + "%2Fgroup1"));
    assertTrue("Missing group 2: " + groupNames,
        groupNames.contains(groupPrefix + "%2Fgroup2"));
    assertTrue(groupNames.contains(
            groupPrefix + "%2Freplicaid%2F%5Btestrole%5D"));
    assertTrue(groupNames.contains(
            groupPrefix + "%2Freplicaid%2F%5Bgrouprole%5D"));
  }
}

