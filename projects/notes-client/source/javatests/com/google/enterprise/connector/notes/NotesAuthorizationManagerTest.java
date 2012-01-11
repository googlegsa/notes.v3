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
import com.google.enterprise.connector.spi.AuthorizationManager;
import com.google.enterprise.connector.spi.AuthorizationResponse;
import com.google.enterprise.connector.spi.Connector;
import com.google.enterprise.connector.spi.Document;
import com.google.enterprise.connector.spi.DocumentList;
import com.google.enterprise.connector.spi.RepositoryException;
import com.google.enterprise.connector.spi.RepositoryLoginException;
import com.google.enterprise.connector.spi.Session;
import com.google.enterprise.connector.spi.SimpleAuthenticationIdentity;
import com.google.enterprise.connector.spi.SpiConstants;
import com.google.enterprise.connector.spi.TraversalManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NotesAuthorizationManagerTest extends ConnectorFixture {

  private String username;

  public NotesAuthorizationManagerTest() {
    super();
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    username = ConnectorFixture.getRequiredProperty(
        "javatest.authorization.username");
  }

  /**
   * Tests reading the repository id.
   *
   * @throws RepositoryException
   */
  public void testGetRepIdFromDocId() throws RepositoryException {
    Session session = connector.login();
    NotesAuthorizationManager manager =
        (NotesAuthorizationManager) session.getAuthorizationManager();
    String docId =
        "http://NewYork/852578CE004AF5F8/0/E54902C71C28594F852578CE004B223B";
    assertEquals("852578CE004AF5F8", manager.getRepIdFromDocId(docId));
  }

  /**
   * Tests reading the UNID.
   *
   * @throws RepositoryException
   */
  public void testGetUnidFromDocId() throws RepositoryException {
    Session session = connector.login();
    NotesAuthorizationManager manager =
        (NotesAuthorizationManager) session.getAuthorizationManager();
    String docId =
        "http://NewYork/852578CE004AF5F8/0/E54902C71C28594F852578CE004B223B";
    assertEquals("E54902C71C28594F852578CE004B223B",
        manager.getUNIDFromDocId(docId));
  }

  /**
   * Tests the authorize response for an invalid user.
   *
   * @throws RepositoryException
   */
  public void testAuthorizeDocIdsInvalidUser() throws RepositoryException {
    Session session = connector.login();
    NotesAuthorizationManager manager =
        (NotesAuthorizationManager) session.getAuthorizationManager();
    Collection<String> docIdList = new ArrayList<String>(5);
    docIdList.add(
        "http://NewYork/852578CE004AF5F8/0/A882F2482DCAC783852578CE004B0346");
    docIdList.add(
        "http://NewYork/852578CE004AF5F8/0/D2DB3C67F6263353852578CE004B1955");
    docIdList.add(
        "http://NewYork/852578CE004AF5F8/0/CCDA1D73DA54B411852578CE004B1BE2");
    docIdList.add(
        "http://NewYork/852578CE004AF5F8/0/1726A130A2B970CC852578CE004B1E70");
    docIdList.add(
        "http://NewYork/852578CE004AF5F8/0/E54902C71C28594F852578CE004B223B");
    Collection<AuthorizationResponse> responseList = manager.authorizeDocids(
        docIdList, new SimpleAuthenticationIdentity("not a valid user"));

    assertTrue("Too many elements in response list",
        responseList.size() <= docIdList.size());
    for (AuthorizationResponse response : responseList) {
      assertFalse(response.getDocid(), response.isValid());
    }
  }

  public void testAuthorizeDocids()
      throws RepositoryLoginException, RepositoryException {
    Session session = connector.login();
    List<String> docIds = getDocIds(session);
    assertEquals(5, docIds.size());
    // Try to construct an invalid docId that's still
    // syntactically valid. It should be authorized since it
    // won't appear in the security view.
    StringBuilder id = new StringBuilder(docIds.get(2));
    int slash = id.lastIndexOf("/");
    id.replace(slash + 1, id.length() - 1, "12345678901234567890123456789012");
    docIds.add(4, id.toString());

    NotesAuthorizationManager manager =
        (NotesAuthorizationManager) session.getAuthorizationManager();
    Collection<AuthorizationResponse> responseList = manager.authorizeDocids(
        docIds, new SimpleAuthenticationIdentity(username));
    assertEquals(6, responseList.size());
    for (AuthorizationResponse response : responseList) {
      assertTrue(response.getDocid(), response.isValid());
    }
  }

  public void testAuthorizeDocidsMalformedDocid()
      throws RepositoryLoginException, RepositoryException {
    Session session = connector.login();
    List<String> docIds = getDocIds(session);
    assertEquals(5, docIds.size());
    // Set a invalid docId that's not syntactically valid.
    docIds.add(4, "not a doc id");
    assertEquals(6, docIds.size());

    NotesAuthorizationManager manager =
        (NotesAuthorizationManager) session.getAuthorizationManager();
    Collection<AuthorizationResponse> responseList = manager.authorizeDocids(
        docIds, new SimpleAuthenticationIdentity(username));
    assertEquals(6, responseList.size());
    for (AuthorizationResponse response : responseList) {
      if ("not a doc id".equals(response.getDocid())) {
        assertFalse(response.getDocid(), response.isValid());
      } else {
        assertTrue(response.getDocid(), response.isValid());
      }
    }
  }

  public void testNullUserId() throws RepositoryException {
    Session session = connector.login();
    List<String> docIds = new ArrayList<String>();
    docIds.add("id 1");
    docIds.add("id 2");
    docIds.add("id 3");
    NotesAuthorizationManager manager =
        (NotesAuthorizationManager) session.getAuthorizationManager();
    Collection<AuthorizationResponse> responseList = manager.authorizeDocids(
        docIds, new SimpleAuthenticationIdentity(null));
    assertEquals(0, responseList.size());
    for (AuthorizationResponse response : responseList) {
      assertEquals(AuthorizationResponse.Status.INDETERMINATE,
          response.getStatus());
    }
  }

  // Get a few valid docids.
  private List<String> getDocIds(Session session) throws RepositoryException {
    TraversalManager tm = session.getTraversalManager();
    tm.setBatchHint(5);
    DocumentList docList = tm.startTraversal();
    Document doc;
    List<String> docIdList = new ArrayList<String>(10);
    assertNotNull("startTraversal returned a null document list", docList);
    while (null != (doc = docList.nextDocument())) {
      String docId = doc.findProperty(SpiConstants.PROPNAME_DOCID)
          .nextValue().toString();
      assertNotNull("Missing doc id", docId);
      docIdList.add(docId);
    }
    return docIdList;
  }
}

