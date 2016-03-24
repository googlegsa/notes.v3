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

import com.google.enterprise.connector.spi.RepositoryException;

public class NotesConnectorSessionTest extends ConnectorFixture {

  public NotesConnectorSessionTest() {
    super();
  }

  /**
   * Tests NotesConnector.login and the properties set in
   * NotesConnectorSession. Assumes that the Notes GSA Connector
   * config is using default values where appropriate.
   *
   * @throws RepositoryException
   */
  public void testProperties() throws RepositoryException {
    NotesConnectorSession session = (NotesConnectorSession) connector.login();
    assertEquals("maxCrawlQDepth", 5000, session.getMaxCrawlQDepth());
    assertEquals("deletionBatchSize", 300, session.getDeletionBatchSize());
    assertEquals("numCrawlerThreads", 1, session.getNumCrawlerThreads());
    assertNotNull("notifier", session.getNotifier());
    assertTrue("spoolDir", session.getSpoolDir().endsWith("gsaSpool"));
    assertEquals("domain", "", session.getDomain(ConnectorFixture.server));
    assertEquals("mimetype", "application/msword", session.getMimeType("doc"));
    assertEquals("idpassword", ConnectorFixture.idpassword,
        session.getPassword());
    assertEquals("server", ConnectorFixture.server, session.getServer());
    assertEquals("maxFileSize", 30 * 1024 * 1024, session.getMaxFileSize());
    assertEquals("database", ConnectorFixture.database, session.getDatabase());
    assertSame("connector", connector, session.getConnector());

    assertFalse(".doc is excluded", session.isExcludedExtension("doc"));
    assertFalse(".DOC is excluded", session.isExcludedExtension("DOC"));
    assertTrue(".jpg is included", session.isExcludedExtension("jpg"));
    // TODO: File extensions are currently case-sensitive, so
    // this fails. Modify this assertion appropriately when a
    // decision is reached about whether extensions should be
    // case-insensitive.
    //assertTrue(".JPG is included", session.isExcludedExtension("JPG"));
  }
}

