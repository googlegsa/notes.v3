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

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.google.enterprise.connector.notes.client.NotesSession;
import com.google.enterprise.connector.notes.client.mock.NotesDatabaseMock;
import com.google.enterprise.connector.notes.client.mock.NotesViewMock;
import com.google.enterprise.connector.notes.client.mock.NotesDocumentMock;
import com.google.enterprise.connector.notes.client.mock.NotesMockUtil;
import com.google.enterprise.connector.notes.client.mock.SessionFactoryMock;
import com.google.enterprise.connector.spi.RepositoryException;

public class NotesUserGroupManagerTest extends TestCase {
  private static NotesConnector connector;
  private static SessionFactoryMock factory;
  private static NotesConnectorSession connectorSession;
  private static NotesSession session;
  private static NotesDatabaseMock namesDatabase;
  private static NotesDatabaseMock configDatabase;
  
  public static Test suite() {
    return new TestSetup(
        new TestSuite(NotesUserGroupManagerTest.class)) {
      protected void setUp() throws Exception {
        connector = NotesConnectorTest.getConnector();
        factory = (SessionFactoryMock) connector.getSessionFactory();
        NotesConnectorSessionTest.configureFactoryForSession(factory);
        connectorSession = (NotesConnectorSession) connector.login();
        session = connectorSession.createNotesSession();
        namesDatabase = (NotesDatabaseMock) session.getDatabase(
            connectorSession.getServer(), connectorSession.getDirectory());
        configDatabase = (NotesDatabaseMock) session.getDatabase(
            connectorSession.getServer(), connectorSession.getDatabase());
      }
      
      protected void tearDown() throws Exception {
        connector.shutdown();
      }
    };
  }
  
  private NotesUserGroupManager userGrpMgr;
  
  public NotesUserGroupManagerTest() {
    super();
  }
  
  protected void setUp() throws RepositoryException {
    initNotesDirectory(namesDatabase, connectorSession);
    initConfigDatabase(configDatabase);
    userGrpMgr = new NotesUserGroupManager();
    userGrpMgr.updatePeopleGroups(connectorSession, true);
  }
  
  protected void tearDown() {
    
  }
  
  private static void initPeople(NotesDatabaseMock nab, 
      NotesConnectorSession ncs) throws RepositoryException {
    NotesMockUtil.createUser("CN=Green Ball/OU=West/O=NotesTest", "greenball", 
        "pass123", nab, ncs);
    NotesMockUtil.createUser("CN=Red Ball/OU=West/O=NotesTest", "redball", 
        "pass123", nab, ncs);
    NotesMockUtil.createUser("CN=Blue Ball/OU=West/O=NotesTest", "blueball", 
        "pass123", nab, ncs);
    NotesMockUtil.createUser("CN=Yellow Ball/OU=East/O=NotesTest", "yellowball", 
        "pass123", nab, ncs);
    NotesMockUtil.createUser("CN=Organge Ball/OU=East/O=NotesTest", 
        "orangeball", "pass123", nab, ncs);
    NotesMockUtil.createUser("CN=Brown Ball/OU=East/O=NotesTest", "brownball", 
        "pass123", nab, ncs);
    nab.setViewFields(NCCONST.DIRVIEW_PEOPLEGROUPFLAT, "FlatName");
    nab.setViewFields(NCCONST.DIRVIEW_USERS, "CommonName", "AbbreviateName");
    nab.setViewFields(NCCONST.DIRVIEW_SERVERACCESS, "CanonicalName");
    nab.setViewFields(NCCONST.DIRVIEW_VIMUSERS, "AbbreviatedName");
  }

  private static void initGroups(NotesDatabaseMock nab) 
      throws RepositoryException {
    NotesDocumentMock doc = NotesMockUtil.createGroupDocument("West_Group", 
        "CN=Green Ball/OU=West/O=NotesTest",
        "CN=Red Ball/OU=West/O=NotesTest",
        "CN=Blue Ball/OU=West/O=NotesTest");
    nab.addDocument(doc, NCCONST.DIRVIEW_VIMGROUPS);

    doc = NotesMockUtil.createGroupDocument("East_Group", 
        "CN=Yellow Ball/OU=East/O=NotesTest",
        "CN=Organge Ball/OU=East/O=NotesTest",
        "CN=Brown Ball/OU=East/O=NotesTest");
    nab.addDocument(doc, NCCONST.DIRVIEW_VIMGROUPS, 
        NCCONST.DIRVIEW_SERVERACCESS);

    doc = NotesMockUtil.createGroupDocument("Nested_Group", "West_Group", 
        "East_Group");
    nab.addDocument(doc, NCCONST.DIRVIEW_VIMGROUPS, 
        NCCONST.DIRVIEW_SERVERACCESS);
    nab.setViewFields(NCCONST.DIRVIEW_VIMGROUPS, NCCONST.GITM_LISTNAME);
    nab.setViewFields(NCCONST.DIRVIEW_SERVERACCESS, "FlatName");
  }

  private static void initNotesDirectory(NotesDatabaseMock nab, 
      NotesConnectorSession ncs) throws RepositoryException {
    initPeople(nab, ncs);
    initGroups(nab);
  }
  
  private static void initConfigDatabasePeople(NotesDatabaseMock dbConfig) 
      throws RepositoryException {
    NotesDocumentMock doc = new NotesDocumentMock();
    doc.addItem(NotesMockUtil.createNotesItemText("Form", "Person"));
    doc.addItem(NotesMockUtil.createNotesItemText("UserName", "gsaadmin"));
    doc.addItem(NotesMockUtil.createNotesItemText(
        "NotesName", "cn=gsa administrator/o=notestest"));
    doc.addItem(NotesMockUtil.createNotesItemMultiValues(
        "Groups", "LocalDomainAdmins", "o=notestest"));
    dbConfig.addDocument(doc, NCCONST.VIEWPEOPLECACHE);
  }
  
  private static void initConfigDatabaseGroups(NotesDatabaseMock dbConfig) 
      throws RepositoryException {
    NotesDocumentMock doc = new NotesDocumentMock();
    doc.addItem(NotesMockUtil.createNotesItemText("Form", "Group"));
    doc.addItem(NotesMockUtil.createNotesItemText(
        NCCONST.GCITM_GROUPNAME, "LocalDomainAdmins"));
    doc.addItem(NotesMockUtil.createNotesItemMultiValues(
        NCCONST.GCITM_CHILDGROUPS, ""));
    dbConfig.addDocument(doc, NCCONST.VIEWGROUPCACHE, NCCONST.VIEWPARENTGROUPS);
  }
  
  private static void initConfigDatabase(NotesDatabaseMock dbConfig) 
      throws RepositoryException {
    initConfigDatabasePeople(dbConfig);
    initConfigDatabaseGroups(dbConfig);
    dbConfig.setViewFields(NCCONST.VIEWPEOPLECACHE, "UserName");
    dbConfig.setViewFields(NCCONST.VIEWGROUPCACHE, NCCONST.GCITM_GROUPNAME);
    dbConfig.setViewFields(NCCONST.VIEWPARENTGROUPS, NCCONST.GCITM_CHILDGROUPS);
  }
  
  public void testPeopleCreatedInDirectory() throws RepositoryException {
    NotesViewMock view = 
        (NotesViewMock) namesDatabase.getView(NCCONST.DIRVIEW_USERS);
    NotesDocumentMock doc = 
        (NotesDocumentMock) view.getDocumentByKey("Green Ball", true);
    assertNotNull(doc);
  }
  
  public void testGroupsCreatedInDirectory() throws RepositoryException {
    NotesViewMock view = 
        (NotesViewMock) namesDatabase.getView(NCCONST.DIRVIEW_VIMGROUPS);
    NotesDocumentMock doc1 = 
        (NotesDocumentMock) view.getDocumentByKey("West_Group", true);
    assertNotNull(doc1);
    NotesDocumentMock doc2 = 
        (NotesDocumentMock) view.getDocumentByKey("East_Group", true);
    assertNotNull(doc2);
    NotesDocumentMock doc3 = 
        (NotesDocumentMock) view.getDocumentByKey("Nested_Group", true);
    assertNotNull(doc3);
  }
  
  public void testPeopleCreatedInCache() throws RepositoryException {
    NotesViewMock view = 
        (NotesViewMock) configDatabase.getView(NCCONST.VIEWPEOPLECACHE);
    NotesDocumentMock doc = 
        (NotesDocumentMock) view.getDocumentByKey("gsaadmin", true);
    assertNotNull(doc);
  }

  public void testGroupsCreatedInCache() throws RepositoryException {
    NotesViewMock view = 
        (NotesViewMock) configDatabase.getView(NCCONST.VIEWGROUPCACHE);
    NotesDocumentMock doc = 
        (NotesDocumentMock) view.getDocumentByKey("LocalDomainAdmins", true);
    assertNotNull(doc);
  }
}


