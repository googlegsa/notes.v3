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
import com.google.enterprise.connector.spi.Connector;
import com.google.enterprise.connector.spi.Document;
import com.google.enterprise.connector.spi.DocumentList;
import com.google.enterprise.connector.spi.RepositoryException;
import com.google.enterprise.connector.spi.RepositoryLoginException;
import com.google.enterprise.connector.spi.Session;
import com.google.enterprise.connector.spi.SpiConstants;
import com.google.enterprise.connector.spi.TraversalManager;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NotesConnectorTest extends TestCase {

  private String server;
  private String database;
  private String idpassword;
  private NotesConnector connector;

  public NotesConnectorTest() {
    super();
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    server = ConnectorFixture.getRequiredProperty("javatest.server");
    database = ConnectorFixture.getRequiredProperty("javatest.database");
    idpassword = ConnectorFixture.getRequiredProperty("javatest.idpassword");
  }

  /**
   * Tests a missing database parameter on login.
   */
  public void testLoginMissingDatabase() {
    NotesConnector connector = new NotesConnector();
    try {
      connector.setServer(server);
      connector.setIdPassword(idpassword);

      connector.setDatabase(null);
      try {
        connector.login();
        fail("No exception with missing database");
      } catch (RepositoryException e) {
        // Expected result
      } catch (Throwable t) {
        assertTrue("Unexpected exception on login: " + t, false);
      }
    } finally {
      connector.shutdown();
    }
  }

  /**
   * Tests a missing server parameter on login.
   */
  public void testLoginMissingServer() {
    NotesConnector connector = new NotesConnector();
    try {
      connector.setDatabase(database);
      connector.setIdPassword(idpassword);

      connector.setServer(null);
      try {
        connector.login();
        fail("No exception with missing server");
      } catch (RepositoryException e) {
        // Expected result
      } catch (Throwable t) {
        assertTrue("Unexpected exception on login: " + t, false);
      }
    } finally {
      connector.shutdown();
    }
  }

  /**
   * Tests a missing idpassword parameter on login.
   */
  /* TODO: This test resulted in a hanging Java process and the
   * error "The ID file is locked by another process. Try again
   * later" on other tests.
   */
  /*
  public void testLoginMissingIdpassword() throws RepositoryException {
    NotesConnector connector = new NotesConnector();
    try {
      connector.setDatabase(database);
      connector.setServer(server);

      connector.setIdPassword(null);
      try {
        connector.login();
        fail("No exception with missing idpassword");
      } catch (RepositoryException e) {
        // Expected result
      } catch (Throwable t) {
        assertTrue("Unexpected exception on login: " + t, false);
      }
    } finally {
      connector.shutdown();
    }
  }
  */

  /**
   * Tests calling NotesConnector.login a second time.
   *
   * @throws RepositoryLoginException
   * @throws RepositoryException
   */
  public void testLogin() throws RepositoryLoginException,
      RepositoryException {
    NotesConnector connector = new NotesConnector();
    try {
      connector.setDatabase(database);
      connector.setIdPassword(idpassword);
      connector.setServer(server);
      NotesConnectorSession session = (NotesConnectorSession) connector.login();
      assertSame("Didn't get same session back on second call to login",
          session, connector.login());
    } finally {
      connector.shutdown();
    }
  }

  /**
   * Tests calling NotesConnector.login a second time.
   *
   * @throws RepositoryLoginException
   * @throws RepositoryException
   */
  public void testShutdown() throws RepositoryException {
    NotesConnector connector = new NotesConnector();
    assertFalse("Shutdown before login", connector.getShutdown());

    connector.setDatabase(database);
    connector.setIdPassword(idpassword);
    connector.setServer(server);
    try {
      NotesConnectorSession session = (NotesConnectorSession) connector.login();
      connector.shutdown();
      assertTrue("Shutdown after calling shutdown", connector.getShutdown());
    } catch (RepositoryException e) {
      connector.shutdown();
      throw e;
    }
  }
}

