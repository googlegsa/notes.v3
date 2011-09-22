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

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Collection;

public class NotesAuthorizationManagerTest extends TestCase {

  private String server; 
  private String database; 
  private String idpassword; 
  private NotesConnector connector; 

  public NotesAuthorizationManagerTest() {
  }
  
  private String getProperty(String key) {
    String value = System.getProperty(key);
    assertNotNull(key, value);
    return value; 
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp(); 
    server = getProperty("javatest.server"); 
    database = getProperty("javatest.database"); 
    idpassword = getProperty("javatest.idpassword"); 
    connector = new NotesConnector();
    connector.setServer(server);
    connector.setDatabase(database);
    connector.setIdPassword(idpassword);
  }

  @Override
  protected void tearDown() throws Exception {
    connector.shutdown();
    super.tearDown();
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
}

