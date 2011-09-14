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
import com.google.enterprise.connector.spi.RepositoryException;
import com.google.enterprise.connector.spi.RepositoryLoginException;
import com.google.enterprise.connector.spi.Session;
import com.google.enterprise.connector.spi.TraversalManager;
import junit.framework.TestCase;

public class NotesConnectorTest extends TestCase {

  @Override
  protected void setUp() throws Exception {
  }

  /*
   * Test method for
   * 'com.google.enterprise.connector.notes.NotesConnectorSession.login()'
   */
  public void testLogin() throws RepositoryLoginException, RepositoryException {
    Connector connector = new NotesConnector();
    ((NotesConnector) connector).setServer(ncTest.server);
    ((NotesConnector) connector).setDatabase(ncTest.database);
    ((NotesConnector) connector).setIdPassword(ncTest.idpassword);
    Session sess = connector.login();
    assertNotNull(sess);
    assertTrue(sess instanceof NotesConnectorSession);
  }

  public void testReset() throws RepositoryLoginException, RepositoryException {
    Connector connector = new NotesConnector();
    ((NotesConnector) connector).setServer(ncTest.server);
    ((NotesConnector) connector).setDatabase(ncTest.database);
    ((NotesConnector) connector).setIdPassword(ncTest.idpassword);
    Session sess = connector.login();
    TraversalManager tm = sess.getTraversalManager();
    assertNotNull(tm);
    assertTrue(tm instanceof NotesTraversalManager);
    tm.startTraversal();
    try {
      java.lang.Thread.sleep(10000);
    } catch (Exception e) {
    }
  }
}

