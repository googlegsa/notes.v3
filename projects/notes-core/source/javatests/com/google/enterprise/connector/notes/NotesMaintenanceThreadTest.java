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

import com.google.enterprise.connector.notes.client.mock.SessionFactoryMock;

import junit.framework.TestCase;

import java.util.Map;

public class NotesMaintenanceThreadTest extends TestCase {
  private NotesConnector connector;
  private SessionFactoryMock factory;
  private NotesConnectorSession connectorSession;
  private NotesMaintenanceThread maintenanceThread;
  private NotesDocumentManagerTest notesDocMgrDbTest;
  
  private static int BATCH_SIZE = 500;
  
  public void setUp() throws Exception {
    super.setUp();
    connector = NotesConnectorTest.getConnector();
    factory = (SessionFactoryMock) connector.getSessionFactory();
    NotesConnectorSessionTest.configureFactoryForSession(factory);
    connectorSession = (NotesConnectorSession) connector.login();
    maintenanceThread = 
        new NotesMaintenanceThread(connector, connectorSession);
    notesDocMgrDbTest = new NotesDocumentManagerTest();
    notesDocMgrDbTest.setUp();
  }

  public void tearDown() throws Exception {
  }
  
  public void testCheckForDeletions() throws Exception {
    Map<String,NotesDocId> docs = 
        notesDocMgrDbTest.getIndexedDocument(null, null, 1);
    NotesDocId startUnid = docs.get(docs.keySet().iterator().next());
    maintenanceThread.checkForDeletions(startUnid.toString(), BATCH_SIZE);
  }
}
