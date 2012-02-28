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
import com.google.enterprise.connector.notes.client.mock.SessionFactoryMock;
import com.google.enterprise.connector.spi.Session;

import junit.framework.TestCase;

public class NotesConnectorTest extends TestCase {

  static NotesConnector getConnector() {
    NotesConnector connector = new NotesConnector(
        "com.google.enterprise.connector.notes.client.mock.SessionFactoryMock");
    connector.setIdPassword("testpassword");
    connector.setServer("testserver");
    connector.setDatabase("testconfig.nsf");
    // Initialize this to prevent NotesConnector from creating one.
    connector.maintThread = new NotesMaintenanceThread(null, null);
    return connector;
  }

  private NotesConnector connector;

  /**
   * Tests creating a connector with a mock factory.
   */
  public void testCreateConnector() throws Exception {
    connector = new NotesConnector(
        "com.google.enterprise.connector.notes.client.mock.SessionFactoryMock");
    assertTrue(connector.getSessionFactory() instanceof SessionFactoryMock);
  }

  public void testLoginShutdown() throws Exception {
    connector = NotesConnectorTest.getConnector();
    SessionFactoryMock factory = (SessionFactoryMock)
        connector.getSessionFactory();
    NotesConnectorSessionTest.configureFactoryForSession(factory);

    assertFalse(connector.getShutdown());
    try {
      Session session = connector.login();
    } finally {
      connector.shutdown();
    }
    assertTrue(connector.getShutdown());
  }

  public void testDelete() throws Exception {
    connector = new NotesConnector(
        "com.google.enterprise.connector.notes.client.mock.SessionFactoryMock");
    assertFalse(connector.getDelete());
    connector.delete();
    assertTrue(connector.getDelete());
  }
}
