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

import junit.framework.TestCase;

import java.net.MalformedURLException;

public class NotesDocIdTest extends TestCase {

  /**
   * Tests creating a docId from an url.
   */
  public void testCreateDocIdFromUrl() throws MalformedURLException {
    NotesDocId id = new NotesDocId(
        "http://NewYork/852578CE004AF5F8/0/A882F2482DCAC783852578CE004B0346");
    assertEquals("http", id.getProtocol());
    assertEquals("NewYork", id.getHost());
    assertEquals(-1, id.getPort());
    assertEquals("852578CE004AF5F8", id.getReplicaId());
    assertEquals("A882F2482DCAC783852578CE004B0346", id.getDocId());
    assertEquals(
        "http://NewYork/852578CE004AF5F8/0/A882F2482DCAC783852578CE004B0346",
        id.toString());
    assertEquals(
        "http://NewYork/852578CE004AF5F8",
        id.getReplicaUrl());
  }

  /**
   * Tests building a database url.
   */
  public void testCreateDatabaseUrl() {
    NotesDocId id = new NotesDocId();
    id.setHost("NewYork");
    id.setReplicaId("852578CE004AF5F8");
    assertEquals((Object) "http://NewYork/852578CE004AF5F8", id.toString());
  }
  
  public void testNotesDocIdConstructor() throws MalformedURLException{
    String url = "http://dominoServer1.gsa-connectors.com/85257608004F5587/0/03493D9F9F29AE9D85257607005F56F8";
    NotesDocId notesId = new NotesDocId(url);
    assertEquals("http", notesId.getProtocol());
    assertEquals("dominoServer1", notesId.getServer());
    assertEquals("dominoServer1.gsa-connectors.com", notesId.getHost());
    assertEquals("85257608004F5587", notesId.getReplicaId());
    assertEquals("03493D9F9F29AE9D85257607005F56F8", notesId.getDocId());
  }
  
  public void testDefaultConstructor() {
    String url = "http://dominoServer1.gsa-connectors.com/85257608004F5587/0/"
        + "03493D9F9F29AE9D85257607005F56F8";
    NotesDocId notesId = new NotesDocId();
    notesId.setDocId("03493D9F9F29AE9D85257607005F56F8");
    notesId.setReplicaId("85257608004F5587");
    notesId.setProtocol("http");
    notesId.setHost("dominoServer1.gsa-connectors.com");
    assertEquals(url, notesId.toString());
  }
}
