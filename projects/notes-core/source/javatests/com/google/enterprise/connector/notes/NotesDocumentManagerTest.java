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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import junit.framework.TestCase;

import com.google.enterprise.connector.notes.client.NotesDocument;
import com.google.enterprise.connector.notes.client.NotesItem;
import com.google.enterprise.connector.notes.client.NotesSession;
import com.google.enterprise.connector.notes.client.mock.NotesDateTimeMock;
import com.google.enterprise.connector.notes.client.mock.NotesDocumentMock;
import com.google.enterprise.connector.notes.client.mock.NotesItemMock;
import com.google.enterprise.connector.notes.client.mock.SessionFactoryMock;
import com.google.enterprise.connector.spi.RepositoryException;

public class NotesDocumentManagerTest extends TestCase{
  private NotesConnector connector;
  private SessionFactoryMock factory;
  private NotesConnectorSession connectorSession;
  private NotesSession session;
  private NotesDocumentManager notesDocManager;
  
  private List<NotesDocument> docs;
  private static int NUM_OF_DOCS = 1000;
  
  public void setUp() throws Exception {
    super.setUp();
    connector = NotesConnectorTest.getConnector();
    factory = (SessionFactoryMock) connector.getSessionFactory();
    NotesConnectorSessionTest.configureFactoryForSession(factory);
    connectorSession = (NotesConnectorSession) connector.login();
    session = connectorSession.createNotesSession();
    notesDocManager = new NotesDocumentManager(connectorSession);
    generateDocuments();
    updateSearchIndex();
  }

  public void tearDown() throws Exception {
    docs.clear();
  }

  public void testTableNames() {
    assertNotNull(notesDocManager.indexedTableName);
    assertNotNull(notesDocManager.readersTableName);
  }

  public void testDatabaseConnection() throws SQLException {
    Connection conn = notesDocManager.getDatabaseConnection();
    assertNotNull(conn);
    notesDocManager.releaseDatabaseConnection(conn);
  }
 
  public void testCountIndexedDocuments() {
    Connection conn = null;
    try {
      conn = notesDocManager.getDatabaseConnection();
      String sql = "select count(*) from " 
          + notesDocManager.indexedTableName;
      PreparedStatement pstmt = conn.prepareStatement(sql);
      ResultSet rs = pstmt.executeQuery();
      if (rs.next()) {
        assertEquals(NUM_OF_DOCS, rs.getInt(1));
      }
      rs.close();
      pstmt.close();
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      notesDocManager.releaseDatabaseConnection(conn);
    }
  }
  
  public void testHasIndexedDocument() throws RepositoryException {
    NotesDocument doc = docs.get(NUM_OF_DOCS - 1);
    Connection conn = null;
    try {
      conn = notesDocManager.getDatabaseConnection();
      assertTrue(notesDocManager.hasIndexedDocument(
          doc.getItemValueString(NCCONST.NCITM_UNID),
          doc.getItemValueString(NCCONST.NCITM_REPLICAID),
          conn));
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      if(conn != null)
        this.notesDocManager.releaseDatabaseConnection(conn);
    }
  }
  
  public void testStartUnid() throws Exception {
    Map<String,NotesDocId> docIds = 
        notesDocManager.getIndexedDocuments(
            TESTCONST.TEST_UNID1, TESTCONST.DBSRC_REPLICAID, 50);
    String firstUnid = docIds.keySet().iterator().next();
    assertEquals(TESTCONST.TEST_UNID1, firstUnid);
  }
  
  public void testGetIndexedDocuments() throws RepositoryException {
    Map<String,NotesDocId> docIds = 
        notesDocManager.getIndexedDocuments(null, null, NUM_OF_DOCS);
    assertEquals(NUM_OF_DOCS,docIds.size());
    
    //Get a collection equal to batch size which has the first expected unid
    NotesDocument doc = docs.get(NUM_OF_DOCS / 2);
    String unid = doc.getItemValueString(NCCONST.NCITM_UNID);
    String replicaid = doc.getItemValueString(NCCONST.NCITM_REPLICAID);
    docIds = notesDocManager.getIndexedDocuments(unid, 
        replicaid, NUM_OF_DOCS/4);
    assertEquals(NUM_OF_DOCS/4, docIds.size());
    
    Set<String> keys = docIds.keySet();
    assertEquals(unid, keys.iterator().next());
  }
  
  public void testGetDocumentReaders() throws RepositoryException {
    NotesDocument doc = docs.get(0);
    Set<String> reader1 = notesDocManager.getDocumentReaders(
        doc.getItemValueString(NCCONST.NCITM_UNID),
        doc.getItemValueString(NCCONST.NCITM_REPLICAID));
    assertEquals(4, reader1.size());
    
    doc = docs.get(1);
    Set<String> reader2 = notesDocManager.getDocumentReaders(
        doc.getItemValueString(NCCONST.NCITM_UNID),
        doc.getItemValueString(NCCONST.NCITM_REPLICAID));
    assertEquals(0, reader2.size());
  }
  
  public void testDeleteDocument() throws RepositoryException {
    NotesDocument doc = null;
    String unid = null;
    String repid = null;
    Connection conn = null;
    try {
      conn = notesDocManager.getDatabaseConnection();
      for (int i = 0; i < NUM_OF_DOCS/10; i++) {
        doc = docs.get(i);
        unid = doc.getItemValueString(NCCONST.NCITM_UNID);
        repid = doc.getItemValueString(NCCONST.NCITM_REPLICAID);
        assertTrue(notesDocManager.deleteDocument(unid, repid, conn));
      }
    } catch (SQLException e) {
      e.printStackTrace();
    } finally {
      if (conn != null) {
        notesDocManager.releaseDatabaseConnection(conn);
      }
    }
  }

  public void testClearTables() throws RepositoryException {
    assertTrue(notesDocManager.clearTables());
  }
  
  public void testDropTables() throws RepositoryException {
    assertTrue(notesDocManager.dropTables());
  }

  Map<String,NotesDocId> getIndexedDocument(
      String startUnid, String replicaId, int batchSize) 
          throws RepositoryException {
    return notesDocManager.getIndexedDocuments(
        startUnid, replicaId, batchSize);
  }
  
  void generateDocuments() throws RepositoryException {
    docs = new ArrayList<NotesDocument>();
    String host = TESTCONST.SERVER_DOMINO_WEB + TESTCONST.DOMAIN;
    String replicaId = TESTCONST.DBSRC_REPLICAID;
  
    int digitCount1 = String.valueOf(NUM_OF_DOCS).length();
    StringBuilder baseUnid = new StringBuilder();
    for(int i = 0; i < (32 - digitCount1); i++){
      baseUnid.append("X");
    }
    NotesDocument docNew = null;
    int digitCount2 = 0;
    for (int x = 0; x < NUM_OF_DOCS; x++) {
      digitCount2 = String.valueOf(x).length();
      StringBuilder unid = new StringBuilder();
      unid.append(baseUnid);
      for (int y = 0; y < (32 - baseUnid.length() - digitCount2); y++) {
        unid.append("0");
      }
      unid.append(x);
      if (x % 2 == 0) {
        docNew = createNotesDocumentWithAllInfo();
      } else {
        docNew = createNotesDocumentWithoutReaders();
      }
      docNew.replaceItemValue(NCCONST.ITM_DOCID, 
          "http://" + host + "/" + replicaId + "/0/" + unid.toString());
      docNew.replaceItemValue(NCCONST.ITM_GMETANOTESLINK, 
          "notes://" + TESTCONST.SERVER_DOMINO + "/__" + replicaId + ".nsf/0/" + 
          unid.toString() + "?OpenDocument");
      docNew.replaceItemValue(NCCONST.NCITM_UNID, unid.toString());
      docs.add(docNew);
    }
  }
  
  private void updateSearchIndex() throws RepositoryException {
    Connection conn = null;
    try {
      conn = notesDocManager.getDatabaseConnection();
      for(NotesDocument doc : docs){
        notesDocManager.addIndexedDocument(doc, conn);
      }
    } catch (Exception e) {
      throw new RepositoryException(e);
    } finally {
      notesDocManager.releaseDatabaseConnection(conn);
    }
  }
  
  private NotesDocument createNotesDocumentWithAllInfo() 
      throws RepositoryException{
    NotesDocumentMock docMock = new NotesDocumentMock();
    docMock.addItem(new NotesItemMock("name", "Form", "type", NotesItem.TEXT, 
        "values",NCCONST.FORMCRAWLREQUEST));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_ACTION, "type", 
        NotesItem.TEXT, "values","add"));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_DOCID, "type", 
        NotesItem.TEXT, "values",
        "http://" + TESTCONST.SERVER_DOMINO_WEB + TESTCONST.DOMAIN + 
        "/" + TESTCONST.DBSRC_REPLICAID + "/0/XXXXXXXXXXXXXXXXXXXXXXXXXXXX0000"));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_ISPUBLIC, "type", 
        NotesItem.TEXT, "values","true"));
    NotesDateTimeMock dtMock = new NotesDateTimeMock(null);
    dtMock.setNow();
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_LASTMODIFIED, 
        "type", NotesItem.DATETIMES, "values", dtMock));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_LOCK, "type", 
        NotesItem.TEXT, "values","true"));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_MIMETYPE, "type", 
        NotesItem.TEXT, "values","text/plain"));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_TITLE, "type", 
        NotesItem.TEXT, "values","This is a test"));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_GMETAALLATTACHMENTS, 
        "type", NotesItem.TEXT, "values","allattachments"));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_GMETAATTACHMENTS,
        "type", NotesItem.TEXT, "values","attachments"));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_GMETACATEGORIES, 
        "type", NotesItem.TEXT, "values","Discussion"));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_GMETACREATEDATE, "type", 
        NotesItem.DATETIMES, "values",dtMock));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_GMETADATABASE, "type", 
        NotesItem.TEXT, "values","Discussion Database"));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_GMETADESCRIPTION, "type", 
        NotesItem.TEXT, "values","Descrition: this is a test document"));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_GMETAFORM, "type", 
        NotesItem.TEXT, "values","MainTopic"));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_GMETALASTUPDATE, "type", 
        NotesItem.DATETIMES, "values",dtMock));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_GMETANOTESLINK, "type", 
        NotesItem.TEXT, "values",
        "notes://" + TESTCONST.SERVER_DOMINO + "/__" + TESTCONST.DBSRC_REPLICAID + 
        ".nsf/0/XXXXXXXXXXXXXXXXXXXXXXXXXXXX0000?OpenDocument"));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_GMETAREPLICASERVERS, "type", 
        NotesItem.TEXT, "values","server1/mtv/us,server2/mtv/us"));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_GMETAWRITERNAME, "type", 
        NotesItem.TEXT, "values","CN=Jean Writer/OU=MTV/O=GOV"));
    docMock.addItem(new NotesItemMock("name", NCCONST.NCITM_AUTHTYPE, "type", 
        NotesItem.TEXT, "values","connector"));
    
    Vector<String> vecAuthorReaders = new Vector<String>();
    vecAuthorReaders.add("cn=John Doe/ou=mtv/o=us");
    vecAuthorReaders.add("[dbadmin]");
    vecAuthorReaders.add("LocalDomainAdmins");
    NotesItemMock authorReaders = new NotesItemMock("name", 
        NCCONST.NCITM_DOCAUTHORREADERS, "type", NotesItem.TEXT, 
        "values",vecAuthorReaders);
    docMock.addItem(authorReaders);
      
    Vector<String> readers = new Vector<String>();
    readers.add("cn=John Doe/ou=mtv/o=us");
    readers.add("[dbadmin]");
    readers.add("LocalDomainAdmins");
    readers.add("cn=Jane Doe/ou=mtv/o=us");
    NotesItemMock docReaders = new NotesItemMock("name", NCCONST.NCITM_DOCREADERS, 
        "type", NotesItem.TEXT, "values",readers);
    docMock.addItem(docReaders);
    docMock.addItem(new NotesItemMock("name", NCCONST.NCITM_DOMAIN, "type", 
        NotesItem.TEXT, "values","gsa-connectors.com"));
    docMock.addItem(new NotesItemMock("name", NCCONST.NCITM_REPLICAID, "type", 
        NotesItem.TEXT, "values",TESTCONST.DBSRC_REPLICAID));
    docMock.addItem(new NotesItemMock("name", NCCONST.NCITM_SERVER, "type", 
        NotesItem.TEXT, "values","mickey1/mtv/us"));
    docMock.addItem(new NotesItemMock("name", NCCONST.NCITM_STATE, "type", 
        NotesItem.TEXT, "values","Indexed"));
    docMock.addItem(new NotesItemMock("name", NCCONST.NCITM_TEMPLATE, "type", 
        NotesItem.TEXT, "values","Discussion"));
    docMock.addItem(new NotesItemMock("name", NCCONST.NCITM_UNID, "type", 
        NotesItem.TEXT, "values","XXXXXXXXXXXXXXXXXXXXXXXXXXXX0000"));
    docMock.addItem(new NotesItemMock("name", "x.meta_custom1", "type", 
        NotesItem.TEXT, "values","testing custom meta field"));
    docMock.addItem(new NotesItemMock("name", "x.meta_customer", "type", 
        NotesItem.TEXT, "values","Sesame Street"));  
    return docMock;
  }
  
  private NotesDocument createNotesDocumentWithoutReaders() 
      throws RepositoryException{
    NotesDocumentMock docMock = new NotesDocumentMock();
    docMock.addItem(new NotesItemMock("name", "Form", "type", NotesItem.TEXT, 
        "values",NCCONST.FORMCRAWLREQUEST));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_ACTION, "type", 
        NotesItem.TEXT, "values","add"));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_DOCID, "type", 
        NotesItem.TEXT, "values",
        "http://" + TESTCONST.SERVER_DOMINO_WEB + TESTCONST.DOMAIN + 
        "/" + TESTCONST.DBSRC_REPLICAID + "/0/XXXXXXXXXXXXXXXXXXXXXXXXXXXX0001"));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_ISPUBLIC, "type", 
        NotesItem.TEXT, "values","true"));
    NotesDateTimeMock dtMock = new NotesDateTimeMock(null);
    dtMock.setNow();
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_LASTMODIFIED, 
        "type", NotesItem.DATETIMES, "values", dtMock));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_LOCK, "type", 
        NotesItem.TEXT, "values","true"));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_MIMETYPE, "type", 
        NotesItem.TEXT, "values","text/plain"));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_TITLE, "type", 
        NotesItem.TEXT, "values","This is a test"));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_GMETAALLATTACHMENTS, 
        "type", NotesItem.TEXT, "values","allattachments"));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_GMETAATTACHMENTS,
        "type", NotesItem.TEXT, "values","attachments"));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_GMETACATEGORIES, 
        "type", NotesItem.TEXT, "values","Discussion"));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_GMETACREATEDATE, "type", 
        NotesItem.DATETIMES, "values",dtMock));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_GMETADATABASE, "type", 
        NotesItem.TEXT, "values","Discussion Database"));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_GMETADESCRIPTION, "type", 
        NotesItem.TEXT, "values","Descrition: this is a test document"));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_GMETAFORM, "type", 
        NotesItem.TEXT, "values","MainTopic"));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_GMETALASTUPDATE, "type", 
        NotesItem.DATETIMES, "values",dtMock));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_GMETANOTESLINK, "type", 
        NotesItem.TEXT, "values",
        "notes://" + TESTCONST.SERVER_DOMINO + "/__" + TESTCONST.DBSRC_REPLICAID + 
        ".nsf/0/XXXXXXXXXXXXXXXXXXXXXXXXXXXX0001?OpenDocument"));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_GMETAREPLICASERVERS, "type", 
        NotesItem.TEXT, "values","mickey1/mtv/us,server2/mtv/us"));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_GMETAWRITERNAME, "type", 
        NotesItem.TEXT, "values","CN=Jean Writer/OU=MTV/O=GOV"));
    docMock.addItem(new NotesItemMock("name", NCCONST.NCITM_AUTHTYPE, "type", 
        NotesItem.TEXT, "values","connector"));
    docMock.addItem(new NotesItemMock("name", NCCONST.NCITM_DOMAIN, "type", 
        NotesItem.TEXT, "values","gsa-connectors.com"));
    docMock.addItem(new NotesItemMock("name", NCCONST.NCITM_REPLICAID, "type", 
        NotesItem.TEXT, "values","85257608004F5587"));
    docMock.addItem(new NotesItemMock("name", NCCONST.NCITM_SERVER, "type", 
        NotesItem.TEXT, "values","mickey1/mtv/us"));
    docMock.addItem(new NotesItemMock("name", NCCONST.NCITM_STATE, "type", 
        NotesItem.TEXT, "values","Indexed"));
    docMock.addItem(new NotesItemMock("name", NCCONST.NCITM_TEMPLATE, "type", 
        NotesItem.TEXT, "values","Discussion"));
    docMock.addItem(new NotesItemMock("name", NCCONST.NCITM_UNID, "type", 
        NotesItem.TEXT, "values","XXXXXXXXXXXXXXXXXXXXXXXXXXXX0001"));
    return docMock;
  }
}
