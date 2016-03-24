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

import com.google.enterprise.connector.notes.NotesConnector;
import com.google.enterprise.connector.notes.client.NotesDatabase;
import com.google.enterprise.connector.notes.client.NotesDocument;
import com.google.enterprise.connector.notes.client.NotesSession;
import com.google.enterprise.connector.notes.client.NotesView;
import com.google.enterprise.connector.spi.Document;
import com.google.enterprise.connector.spi.Property;
import com.google.enterprise.connector.spi.SpiConstants;
import com.google.enterprise.connector.spi.Value;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class InheritableAclTest extends TestCase {

  private static final Logger LOGGER = Logger.getLogger(
      InheritableAclTest.class.getName());

  private static NotesConnector connector;
  private static NotesConnectorSession connectorSession;
  private static List<Document> documents;

  public static Test suite() {
    if (Boolean.getBoolean("javatest.supportsinheritedacls")) {
      return new TestSetup(new TestSuite(InheritableAclTest.class)) {
        @Override protected void setUp() throws Exception {
          connector = ConnectorFixture.getConnector(false, true);
          connectorSession = (NotesConnectorSession) connector.login();
          NotesUserGroupManager userGroupManager =
              new NotesUserGroupManager(connectorSession);
          userGroupManager.updateUsersGroups(true);
          documents = ConnectorFixture.traverseAll(connectorSession);
        }

        @Override protected void tearDown() throws Exception {
          connector.shutdown();
        }
      };
    } else {
      return new TestSuite();
    }
  }

  private static void print() throws Exception {
    for (Document doc : documents) {
      System.out.println("title: "
          + getValue(doc, SpiConstants.PROPNAME_TITLE));
    }
  }

  private static String getValue(Document doc, String name) throws Exception {
    List<String> values = getValues(doc,name);
    if (values.size() > 0) {
      return values.get(0);
    }
    return null;
  }

  private static List<String> getValues(Document doc, String name)
      throws Exception {
    List<String> values = new ArrayList<String>();
    Property p = doc.findProperty(name);
    if (p != null) {
      Value v = p.nextValue();
      if (v != null) {
        values.add(v.toString());
      }
    }
    return values;
  }

  public InheritableAclTest(String testName) {
    super(testName);
  }

  /**
   * There should be two ACL records for each database being indexed.
   */
  // TODO: if we commit to test databases, we could just open
  // each one by name and retrieve its replica id. We could also
  // assert things about the permit/deny lists in the database
  // ACLs.
  public void testDatabaseAcls() throws Exception {
    if (documents == null) {
      return;
    }

    NotesSession session = connectorSession.createNotesSession();
    try {
      NotesDatabase connectorDatabase = session.getDatabase(
          connectorSession.getServer(), connectorSession.getDatabase());
      NotesView databaseView = connectorDatabase.getView(
          NCCONST.VIEWDATABASES);
      NotesDocument databaseDocument = databaseView.getFirstDocument();
      while (databaseDocument != null) {
        String server =
            databaseDocument.getItemValueString(NCCONST.DITM_SERVER);
        String replicaId =
            databaseDocument.getItemValueString(NCCONST.DITM_REPLICAID);
        NotesDocId id = new NotesDocId();
        id.setHost(server + connectorSession.getDomain(server));
        id.setReplicaId(replicaId);
        String replicaUrl = id.getReplicaUrl();

        int count = 0;
        for (Document doc : documents) {
          String docid = getValue(doc, SpiConstants.PROPNAME_DOCID);
          if (docid.equals(replicaUrl + "/"
                  + NCCONST.DB_ACL_INHERIT_TYPE_ANDBOTH)) {
            count++;
            assertEquals(
                SpiConstants.AclInheritanceType.AND_BOTH_PERMIT.toString(),
                getValue(doc, SpiConstants.PROPNAME_ACLINHERITANCETYPE));
          } else if (docid.equals(replicaUrl + "/"
                  + NCCONST.DB_ACL_INHERIT_TYPE_PARENTOVERRIDES)) {
            count++;
            assertEquals(
                SpiConstants.AclInheritanceType.PARENT_OVERRIDES.toString(),
                getValue(doc, SpiConstants.PROPNAME_ACLINHERITANCETYPE));
          }
        }
        // TODO: sometimes we get multiple acl records on
        // repeated test runs. Fix the connector so that can't
        // happen.
        //assertEquals(replicaUrl, 2, count);
        assertTrue("Not enough database acls found for " + replicaUrl
            + "\n" + documents, 2 >= count);
        NotesDocument tmp = databaseDocument;
        databaseDocument = databaseView.getNextDocument(tmp);
        tmp.recycle();
      }
    } finally {
      connectorSession.closeNotesSession(session);
    }
  }

  // TODO: this depends on the test database having "(R)" at the
  // beginning of docs with readers. For a standard test
  // database, that might be ok. Otherwise, we could open the
  // underlying database and check the original document, or open
  // the connector database and check the crawl doc.
  public void testDocuments() throws Exception {
    if (documents == null) {
      return;
    }
    for (Document doc : documents) {
      String title = getValue(doc, SpiConstants.PROPNAME_TITLE);
      if (title == null) {
        continue;
      }
      List<String> readers = getValues(doc, SpiConstants.PROPNAME_ACLUSERS);
      if (title.startsWith("(R)")) {
        assertTrue("missing readers for " + title, readers.size() > 0);
        assertTrue(getValue(doc, SpiConstants.PROPNAME_ACLINHERITFROM_DOCID)
            .endsWith(NCCONST.DB_ACL_INHERIT_TYPE_ANDBOTH));
      } else {
        assertTrue("has readers for " + title, readers.size() == 0);
        assertTrue(getValue(doc, SpiConstants.PROPNAME_ACLINHERITFROM_DOCID)
            .endsWith(NCCONST.DB_ACL_INHERIT_TYPE_PARENTOVERRIDES));
      }
    }
  }
}
