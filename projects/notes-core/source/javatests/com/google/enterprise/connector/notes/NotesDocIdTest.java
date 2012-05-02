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
}
