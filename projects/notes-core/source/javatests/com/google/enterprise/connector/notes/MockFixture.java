// Copyright 2014 Google Inc.
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

import com.google.enterprise.connector.notes.client.NotesDateTime;
import com.google.enterprise.connector.notes.client.NotesItem;
import com.google.enterprise.connector.notes.client.NotesView;
import com.google.enterprise.connector.notes.client.mock.NotesDatabaseMock;
import com.google.enterprise.connector.notes.client.mock.NotesDateTimeMock;
import com.google.enterprise.connector.notes.client.mock.NotesDocumentMock;
import com.google.enterprise.connector.notes.client.mock.NotesItemMock;
import com.google.enterprise.connector.notes.client.mock.SessionFactoryMock;
import com.google.enterprise.connector.spi.RepositoryException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

public class MockFixture {
  private static int unidSeq = 0;
  private static final String UNID_PREFIX = "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX";

  public static String getUniqueId() {
    String sid = UNID_PREFIX + String.valueOf(++unidSeq);
    return sid.substring(sid.length() - UNID_PREFIX.length());
  }

  public static NotesDatabaseMock newMockDatabase(String server,
      String filePath, String replicaId, List<NotesDocumentMock> docs,
      String viewName) {
    NotesDatabaseMock srcDb =
        new NotesDatabaseMock(server, filePath, replicaId);
    for (NotesDocumentMock doc : docs) {
      srcDb.addDocument(doc, viewName);
    }
    return srcDb;
  }

  public static NotesDocumentMock getSrcDbDocument(NotesDatabaseMock configDb,
      NotesDatabaseMock srcDb) throws RepositoryException {
    NotesView vwSrcDbs = configDb.getView(NCCONST.VIEWDATABASES);
    NotesDocumentMock doc = (NotesDocumentMock) vwSrcDbs.getFirstDocument();
    while (doc != null) {
      if (srcDb.getReplicaID().equals(
          doc.getItemValueString(NCCONST.DITM_REPLICAID))) {
        return doc;
      }
      doc = (NotesDocumentMock) vwSrcDbs.getNextDocument(doc);
    }
    return null;
  }

  public static void setupSourceDatabase(NotesDatabaseMock configDb,
      NotesDatabaseMock srcDb, String authType, String template,
      boolean enabled, NotesDateTime lastUpdate, String aclText)
          throws Exception {
    NotesDocumentMock doc = (NotesDocumentMock) configDb.getDocumentByUNID(
        TESTCONST.DBSRC_REPLICAID);
    if (doc == null) {
      doc = new NotesDocumentMock();
      configDb.addDocument(doc, NCCONST.VIEWDATABASES);
      configDb.setViewFields(NCCONST.VIEWDATABASES, NCCONST.DITM_REPLICAID);
    }
    doc.addItem(new NotesItemMock("name", NCCONST.NCITM_UNID, "type",
        NotesItem.TEXT, "values", TESTCONST.DBSRC_REPLICAID));
    doc.addItem(new NotesItemMock("name", NCCONST.ITMFORM, "type",
        NotesItem.TEXT, "values", "DATABASE"));
    doc.addItem(new NotesItemMock("name", NCCONST.DITM_DBNAME, "type",
        NotesItem.TEXT, "values", srcDb.getName()));
    doc.addItem(new NotesItemMock("name", NCCONST.DITM_SERVER, "type",
        NotesItem.TEXT, "values", srcDb.getServer()));
    doc.addItem(new NotesItemMock("name", NCCONST.DITM_REPLICAID, "type",
        NotesItem.TEXT, "values", srcDb.getReplicaID()));
    doc.addItem(new NotesItemMock("name", NCCONST.DITM_AUTHTYPE, "type",
        NotesItem.TEXT, "values", authType));
    doc.addItem(new NotesItemMock("name", NCCONST.DITM_TEMPLATE, "type",
        NotesItem.TEXT, "values", template));
    doc.addItem(new NotesItemMock("name", NCCONST.DITM_CRAWLENABLED, "type",
        NotesItem.NUMBERS, "values",
        (enabled ? Integer.valueOf(1) : Integer.valueOf(0))));
    doc.addItem(new NotesItemMock("name", NCCONST.DITM_LASTUPDATE, "type",
        NotesItem.DATETIMES, "values", lastUpdate));

    // Default values
    srcDb.setACLActivityLog(aclText);
    doc.addItem(new NotesItemMock("name", NCCONST.DITM_ACLTEXT, "type",
        NotesItem.TEXT, "values", aclText));
  }

  public static NotesDatabaseMock setupNotesTemplate(SessionFactoryMock factory,
      String configDbName, String templateName, String searchFormula,
      boolean setLookupField) throws Exception {
    NotesDatabaseMock configDatabase = factory.getDatabase(configDbName);
    setupNotesTemplate(configDatabase, templateName, searchFormula,
        setLookupField);
    return configDatabase;
  }

  public static void setupNotesTemplate(NotesDatabaseMock configDb,
      String templateName, String searchFormula, boolean setLookupField)
          throws Exception {
    NotesDocumentMock docTmpl = new NotesDocumentMock();
    docTmpl.addItem(new NotesItemMock("name", NCCONST.NCITM_UNID, "type",
        NotesItem.TEXT, "values", getUniqueId()));
    docTmpl.addItem(new NotesItemMock("name", "Form", "type", NotesItem.TEXT,
        "values", "TEMPLATE"));
    docTmpl.addItem(new NotesItemMock("name", "TemplateName", "type",
        NotesItem.TEXT, "values", templateName));
    if (searchFormula != null) {
      docTmpl.addItem(new NotesItemMock("name", "SearchString", "type",
          NotesItem.TEXT, "values", searchFormula));
    }
    configDb.addDocument(docTmpl, NCCONST.VIEWTEMPLATES);
    if (setLookupField) {
      configDb.setViewFields(NCCONST.VIEWTEMPLATES, "TemplateName");
    }
  }

  public static List<NotesDocumentMock> generateDocuments(int count)
      throws RepositoryException {
    List<NotesDocumentMock> docs = new ArrayList<NotesDocumentMock>();
    String host = TESTCONST.SERVER_DOMINO_WEB + TESTCONST.DOMAIN;
    String replicaId = TESTCONST.DBSRC_REPLICAID;
  
    int digitCount1 = String.valueOf(count).length();
    StringBuilder baseUnid = new StringBuilder();
    for (int i = 0; i < (32 - digitCount1); i++){
      baseUnid.append("X");
    }
    unidSeq = 0;
    for (int x = 0; x < count; x++) {
      String unid = getUniqueId();
      NotesDocumentMock docNew;
      if (x % 2 == 0) {
        docNew = createNotesDocumentWithAllInfo(unid);
      } else {
        docNew = createNotesDocumentWithoutReaders(unid);
      }
      docNew.replaceItemValue(NCCONST.ITM_DOCID, 
          "http://" + host + "/" + replicaId + "/0/" + unid.toString());
      docNew.replaceItemValue(NCCONST.ITM_GMETANOTESLINK, 
          "notes://" + TESTCONST.SERVER_DOMINO + "/__" + replicaId + ".nsf/0/"
          + unid + "?OpenDocument");
      docNew.replaceItemValue(NCCONST.NCITM_UNID, unid);
      docs.add(docNew);
    }
    return docs;
  }

  public static NotesDocumentMock createNotesDocumentWithAllInfo(String unid) 
      throws RepositoryException {
    NotesDocumentMock docMock = new NotesDocumentMock();
    docMock.addItem(new NotesItemMock("name", "Form", "type", NotesItem.TEXT, 
        "values",NCCONST.FORMCRAWLREQUEST));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_ACTION, "type", 
        NotesItem.TEXT, "values","add"));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_DOCID, "type", 
        NotesItem.TEXT, "values",
        "http://" + TESTCONST.SERVER_DOMINO_WEB + TESTCONST.DOMAIN + 
        "/" + TESTCONST.DBSRC_REPLICAID + "/0/" + unid));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_ISPUBLIC, "type", 
        NotesItem.TEXT, "values","true"));
    NotesDateTimeMock dtMock = new NotesDateTimeMock(new Date());
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
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_GMETACREATEDATE,
        "type", NotesItem.DATETIMES, "values",dtMock));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_GMETADATABASE,
        "type", NotesItem.TEXT, "values","Discussion Database"));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_GMETADESCRIPTION,
        "type", NotesItem.TEXT, "values",
        "Descrition: this is a test document"));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_GMETAFORM, "type", 
        NotesItem.TEXT, "values","MainTopic"));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_GMETALASTUPDATE,
        "type", NotesItem.DATETIMES, "values",dtMock));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_GMETANOTESLINK,
        "type", NotesItem.TEXT, "values", "notes://" + TESTCONST.SERVER_DOMINO
        + "/__" + TESTCONST.DBSRC_REPLICAID + ".nsf/0/" + unid
        + "?OpenDocument"));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_GMETAREPLICASERVERS,
        "type", NotesItem.TEXT, "values","server1/mtv/us,server2/mtv/us"));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_GMETAWRITERNAME,
        "type", NotesItem.TEXT, "values","CN=Jean Writer/OU=MTV/O=GOV"));
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
    NotesItemMock docReaders = new NotesItemMock("name",
        NCCONST.NCITM_DOCREADERS, "type", NotesItem.TEXT, "values",readers);
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
        NotesItem.TEXT, "values", unid));
    docMock.addItem(new NotesItemMock("name", "x.meta_custom1", "type", 
        NotesItem.TEXT, "values","testing custom meta field"));
    docMock.addItem(new NotesItemMock("name", "x.meta_customer", "type", 
        NotesItem.TEXT, "values","Sesame Street"));  
    return docMock;
  }

  public static NotesDocumentMock createNotesDocumentWithoutReaders(
      String unid) throws RepositoryException{
    NotesDocumentMock docMock = new NotesDocumentMock();
    docMock.addItem(new NotesItemMock("name", "Form", "type", NotesItem.TEXT, 
        "values",NCCONST.FORMCRAWLREQUEST));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_ACTION, "type", 
        NotesItem.TEXT, "values","add"));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_DOCID, "type", 
        NotesItem.TEXT, "values",
        "http://" + TESTCONST.SERVER_DOMINO_WEB + TESTCONST.DOMAIN + 
        "/" + TESTCONST.DBSRC_REPLICAID + "/0/" + unid));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_ISPUBLIC, "type", 
        NotesItem.TEXT, "values","true"));
    NotesDateTimeMock dtMock = new NotesDateTimeMock(new Date());
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
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_GMETACREATEDATE,
        "type", NotesItem.DATETIMES, "values",dtMock));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_GMETADATABASE, "type",
        NotesItem.TEXT, "values","Discussion Database"));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_GMETADESCRIPTION,
        "type", NotesItem.TEXT, "values",
        "Descrition: this is a test document"));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_GMETAFORM, "type", 
        NotesItem.TEXT, "values","MainTopic"));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_GMETALASTUPDATE,
        "type", NotesItem.DATETIMES, "values",dtMock));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_GMETANOTESLINK,
        "type", NotesItem.TEXT, "values", "notes://" + TESTCONST.SERVER_DOMINO
        + "/__" + TESTCONST.DBSRC_REPLICAID + ".nsf/0/" + unid
        + "?OpenDocument"));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_GMETAREPLICASERVERS,
        "type", NotesItem.TEXT, "values","mickey1/mtv/us,server2/mtv/us"));
    docMock.addItem(new NotesItemMock("name", NCCONST.ITM_GMETAWRITERNAME,
        "type", NotesItem.TEXT, "values","CN=Jean Writer/OU=MTV/O=GOV"));
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
        NotesItem.TEXT, "values", unid));
    return docMock;
  }
}
