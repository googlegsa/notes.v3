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

import junit.framework.TestCase;

public class ConnectorFixture extends TestCase {

  static String getRequiredProperty(String key) {
    String value = System.getProperty(key);
    assertNotNull(key, value);
    return value;
  }

  static String getOptionalProperty(String key) {
    return System.getProperty(key);
  }

  String server;
  String database;
  String idpassword;
  String googleFeedHost;
  String gsausername;
  String gsapassword;
  NotesConnector connector;

  public ConnectorFixture() {
    super();
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    server = ConnectorFixture.getRequiredProperty("javatest.server");
    database = ConnectorFixture.getRequiredProperty("javatest.database");
    idpassword = ConnectorFixture.getRequiredProperty("javatest.idpassword");
    googleFeedHost = ConnectorFixture.getOptionalProperty("javatest.googleFeedHost");
    gsausername = ConnectorFixture.getOptionalProperty("javatest.gsausername");
    gsapassword = ConnectorFixture.getOptionalProperty("javatest.gsapassword");
    connector = new NotesConnector();
    connector.setServer(server);
    connector.setDatabase(database);
    connector.setIdPassword(idpassword);
    if (googleFeedHost != null) {
      connector.setGoogleFeedHost(googleFeedHost);
    }
    if (gsausername != null) {
      connector.setGsaUsername(gsausername);
    }
    if (gsapassword != null) {
      connector.setGsaPassword(gsapassword);
    }
    connector.setPolicyAclPattern(
        "^googleconnector://{0}.localhost/doc?docid={1}");
  }

  @Override
  protected void tearDown() throws Exception {
    connector.shutdown();
    super.tearDown();
  }
}
