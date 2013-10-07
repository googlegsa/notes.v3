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

import com.google.enterprise.connector.notes.client.NotesDatabase;
import com.google.enterprise.connector.notes.client.NotesDocument;
import com.google.enterprise.connector.notes.client.NotesItem;
import com.google.enterprise.connector.notes.client.NotesSession;
import com.google.enterprise.connector.notes.client.NotesView;
import com.google.enterprise.connector.notes.client.mock.NotesDatabaseMock;
import com.google.enterprise.connector.notes.client.mock.NotesDocumentMock;
import com.google.enterprise.connector.notes.client.mock.NotesItemMock;
import com.google.enterprise.connector.notes.client.mock.SessionFactoryMock;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;

public class NotesCrawlerThreadTest extends TestCase {

  private NotesConnector connector;
  private SessionFactoryMock factory;
  private NotesConnectorSession connectorSession;
  private NotesSession session;

  public NotesCrawlerThreadTest() {
    super();
  }

  protected void setUp() throws Exception {
    super.setUp();
    connector = NotesConnectorTest.getConnector();
    factory = (SessionFactoryMock) connector.getSessionFactory();
    NotesConnectorSessionTest.configureFactoryForSession(factory);
    connectorSession = (NotesConnectorSession) connector.login();
    session = connectorSession.createNotesSession();
    
    NotesDatabaseMock dbConfigMock =
        (NotesDatabaseMock) session.getDatabase("testserver", "testconfig.nsf");
    NotesDocumentMock docTemplateMock = new NotesDocumentMock();
    docTemplateMock.addItem(new NotesItemMock("name",
        NCCONST.TITM_TEMPLATENAME, "type", NotesItem.TEXT, "values",
        "Discussion"));
    docTemplateMock.addItem(new NotesItemMock("name",
        NCCONST.TITM_SEARCHRESULTSFIELDS, "type", NotesItem.TEXT, "values",
        "@True"));
    dbConfigMock.addDocument(docTemplateMock, NCCONST.VIEWTEMPLATES);
    dbConfigMock.setViewFields(NCCONST.VIEWTEMPLATES,
        NCCONST.TITM_TEMPLATENAME);
  }

  protected void tearDown() {
    if (null != connector) {
      connector.shutdown();
    }
  }

  /**
   * Tests meta field configuration parsing.
   */
  public void testMetaField() {
    final String[][] data = {
      { "bar", null, "bar", "bar" },
      { "foo=bar", null, "foo", "bar" },
      { "form===foo=bar", "form", "foo", "bar" },
      { "form=formname===foo=bar", "form=formname", "foo", "bar" },
      { "form===foo=bar=baz", null, null, null },
      { null, null, null, null },
      { "", null, null, null },
      { "   ", null, null, null },
    };
    for (String[] v : data) {
      NotesCrawlerThread.MetaField mf = new NotesCrawlerThread.MetaField(v[0]);
      assertEquals("form", v[1], mf.getFormName());
      assertEquals("field", v[2], mf.getFieldName());
      assertEquals("meta", v[3], mf.getMetaName());
    }
  }

  public void testMapFields() throws Exception {
    NotesCrawlerThread crawler =
        new NotesCrawlerThread(connector, connectorSession);
    crawler.connectQueue();
    crawler.loadTemplateDoc("Discussion");

    NotesDocumentMock crawlDoc = new NotesDocumentMock();
    crawlDoc.addItem(new NotesItemMock("name", NCCONST.NCITM_SERVER, "type",
        NotesItem.TEXT, "values", "testserver"));
    crawlDoc.addItem(new NotesItemMock("name", NCCONST.NCITM_REPLICAID, "type",
        NotesItem.TEXT, "values", "replicaid"));
    crawlDoc.addItem(new NotesItemMock("name", NCCONST.NCITM_UNID, "type",
        NotesItem.TEXT, "values", "unid"));
    crawlDoc.addItem(new NotesItemMock("name", NCCONST.NCITM_TEMPLATE, "type",
        NotesItem.TEXT, "values", "Discussion"));

    NotesDocumentMock sourceDoc = new NotesDocumentMock();
    sourceDoc.addItem(new NotesItemMock("name", NCCONST.ITM_GMETAFORM, "type",
        NotesItem.TEXT, "values", "Main Topic"));
    sourceDoc.addItem(new NotesItemMock("name", NCCONST.ITM_LASTMODIFIED,
        "type", NotesItem.DATETIMES, "values", new Date()));
    sourceDoc.addItem(new NotesItemMock("name", NCCONST.ITM_GMETAWRITERNAME,
        "type", NotesItem.TEXT, "values", "Mickey Mouse"));
    sourceDoc.addItem(new NotesItemMock("name", NCCONST.ITM_GMETALASTUPDATE,
        "type", NotesItem.DATETIMES, "values", new Date()));
    sourceDoc.addItem(new NotesItemMock("name", NCCONST.ITM_GMETACREATEDATE,
        "type", NotesItem.DATETIMES, "values", new Date()));

    crawler.mapFields(crawlDoc, sourceDoc);
    assertNotNull(crawlDoc.getItemValueString(NCCONST.ITM_DISPLAYURL));
  }

  /**
   * Tests mapping meta fields.
   */
  public void testMapMetaFields() throws Exception {
    NotesCrawlerThread crawler = new NotesCrawlerThread(null, null);
    ArrayList<NotesCrawlerThread.MetaField> mf =
        new ArrayList<NotesCrawlerThread.MetaField>();
    mf.add(new NotesCrawlerThread.MetaField("foo"));
    mf.add(new NotesCrawlerThread.MetaField("bar=mappedbar"));
    mf.add(new NotesCrawlerThread.MetaField("bazform===baz=mappedbaz"));
    crawler.metaFields = mf;

    NotesDocumentMock crawlDoc = new NotesDocumentMock();
    NotesDocumentMock sourceDoc = new NotesDocumentMock();
    sourceDoc.addItem(new NotesItemMock("name", "foo", "type", NotesItem.TEXT,
        "values", "this is the text for field foo"));
    sourceDoc.addItem(new NotesItemMock("name", "bar", "type", NotesItem.TEXT,
        "values", "this is the text for field bar"));
    sourceDoc.addItem(new NotesItemMock("name", "baz", "type", NotesItem.TEXT,
        "values", "this is the text for field baz"));

    crawler.mapMetaFields(crawlDoc, sourceDoc);
    assertTrue("crawl doc missing x.foo", crawlDoc.hasItem("x.foo"));
    assertEquals("this is the text for field foo",
        crawlDoc.getItemValueString("x.foo"));
    assertTrue("crawl doc missing x.mappedbar",
        crawlDoc.hasItem("x.mappedbar"));
    assertEquals("this is the text for field bar",
        crawlDoc.getItemValueString("x.mappedbar"));
    assertFalse("crawl doc has x.mappedbaz", crawlDoc.hasItem("x.mappedbaz"));

    // Add a form name to the doc. Now the metafield mapping with
    // a form qualification should apply.
    sourceDoc.addItem(new NotesItemMock("name", "form", "type", NotesItem.TEXT,
        "values", "bazform"));
    crawlDoc = new NotesDocumentMock();
    crawler.mapMetaFields(crawlDoc, sourceDoc);
    assertTrue("crawl doc missing x.foo", crawlDoc.hasItem("x.foo"));
    assertEquals("this is the text for field foo",
        crawlDoc.getItemValueString("x.foo"));
    assertTrue("crawl doc missing x.mappedbar",
        crawlDoc.hasItem("x.mappedbar"));
    assertEquals("this is the text for field bar",
        crawlDoc.getItemValueString("x.mappedbar"));
    assertTrue("crawl doc missing has x.mappedbaz",
        crawlDoc.hasItem("x.mappedbaz"));
    assertEquals("this is the text for field baz",
        crawlDoc.getItemValueString("x.mappedbaz"));
  }

  public void testGetNextFromCrawlQueue() throws Exception {
    NotesDatabaseMock configDatabase = factory.getDatabase("testconfig.nsf");
    NotesDocumentMock crawlRequestDoc = new NotesDocumentMock();
    configDatabase.addDocument(crawlRequestDoc, NCCONST.VIEWCRAWLQ);

    // Mimic the creation of a crawl request doc in NotesDatabasePoller.
    crawlRequestDoc.addItem(new NotesItemMock("name", NCCONST.NCITM_STATE,
            "type", NotesItem.TEXT, "values", NCCONST.STATENEW));
    crawlRequestDoc.addItem(new NotesItemMock("name", NCCONST.ITM_MIMETYPE,
            "type", NotesItem.TEXT, "values", NCCONST.DEFAULT_DOCMIMETYPE));
    crawlRequestDoc.addItem(new NotesItemMock("name", NCCONST.ITMFORM,
                "type", NotesItem.TEXT, "values", NCCONST.FORMCRAWLREQUEST));
    crawlRequestDoc.addItem(new NotesItemMock("name", NCCONST.NCITM_UNID,
            "type", NotesItem.TEXT, "values", "sourcedocunid"));
    crawlRequestDoc.addItem(new NotesItemMock("name", NCCONST.NCITM_REPLICAID,
            "type", NotesItem.TEXT, "values", "sourcedbreplicaid"));
    crawlRequestDoc.addItem(new NotesItemMock("name", NCCONST.NCITM_SERVER,
            "type", NotesItem.TEXT, "values", "testserver"));
    crawlRequestDoc.addItem(new NotesItemMock("name", NCCONST.NCITM_TEMPLATE,
            "type", NotesItem.TEXT, "values", "testtemplate"));
    crawlRequestDoc.addItem(new NotesItemMock("name", NCCONST.NCITM_DOMAIN,
            "type", NotesItem.TEXT, "values", "testdomain"));
    crawlRequestDoc.addItem(new NotesItemMock("name", NCCONST.NCITM_AUTHTYPE,
            "type", NotesItem.TEXT, "values", NCCONST.AUTH_CONNECTOR));
    crawlRequestDoc.addItem(
        new NotesItemMock("name", NCCONST.ITM_GMETANOTESLINK,
            "type", NotesItem.TEXT, "values", "notes url"));

    NotesConnectorSession connectorSession =
        (NotesConnectorSession) connector.login();
    NotesSession session = connectorSession.createNotesSession();
    NotesDatabase configDb = session.getDatabase("testserver",
        "testconfig.nsf");
    NotesView crawlQueue = configDb.getView(NCCONST.VIEWCRAWLQ);
    assertEquals(1, crawlQueue.getEntryCount());
    NotesDocument docFromQueue = NotesCrawlerThread.getNextFromCrawlQueue(
        session, crawlQueue);
    assertNotNull("No doc from queue", docFromQueue);
    assertEquals(NCCONST.STATEINCRAWL,
        docFromQueue.getItemValueString(NCCONST.NCITM_STATE));
  }

  public void testLoadTemplateDoc() throws Exception {
    NotesDatabaseMock configDatabase = factory.getDatabase("testconfig.nsf");
    configDatabase.setViewFields(NCCONST.VIEWTEMPLATES,
        NCCONST.TITM_TEMPLATENAME);
    NotesDocumentMock template1 = new NotesDocumentMock();
    configDatabase.addDocument(template1, NCCONST.VIEWTEMPLATES);
    template1.addItem(new NotesItemMock("name", NCCONST.TITM_TEMPLATENAME,
            "type", NotesItem.TEXT, "values", "testtemplate1"));
    template1.addItem(new NotesItemMock("name", NCCONST.TITM_METAFIELDS,
            "type", NotesItem.TEXT, "values", "foo", "bar=mappedbar"));
    NotesDocumentMock template2 = new NotesDocumentMock();
    configDatabase.addDocument(template2, NCCONST.VIEWTEMPLATES);
    template2.addItem(new NotesItemMock("name", NCCONST.TITM_TEMPLATENAME,
            "type", NotesItem.TEXT, "values", "testtemplate2"));
    template2.addItem(new NotesItemMock("name", NCCONST.TITM_METAFIELDS,
            "type", NotesItem.TEXT, "values", "foo", "bar=mappedbar"));
    NotesDocumentMock templateResponse = new NotesDocumentMock();
    template2.addResponse(templateResponse);

    NotesConnectorSession connectorSession =
        (NotesConnectorSession) connector.login();
    NotesCrawlerThread crawlerThread = new NotesCrawlerThread(connector,
        connectorSession);
    assertNull(crawlerThread.templateDoc);
    assertNull(crawlerThread.metaFields);
    crawlerThread.connectQueue();
    crawlerThread.loadTemplateDoc("testtemplate1");
    assertNotNull(crawlerThread.templateDoc);
    assertNotNull(crawlerThread.formsdc);
    assertNotNull(crawlerThread.metaFields);
    assertEquals("testtemplate1",
        crawlerThread.templateDoc
        .getItemValueString(NCCONST.TITM_TEMPLATENAME));
    for (NotesCrawlerThread.MetaField mf : crawlerThread.metaFields) {
      assertTrue(mf.getFieldName().equals("foo") ||
          mf.getFieldName().equals("bar"));
    }
    crawlerThread.loadTemplateDoc("testtemplate2");
    assertEquals("testtemplate2",
        crawlerThread.templateDoc
        .getItemValueString(NCCONST.TITM_TEMPLATENAME));
  }

  public void testLoadForm() throws Exception {
    NotesDatabaseMock configDatabase = factory.getDatabase("testconfig.nsf");
    configDatabase.setViewFields(NCCONST.VIEWTEMPLATES,
        NCCONST.TITM_TEMPLATENAME);
    NotesDocumentMock template1 = new NotesDocumentMock();
    configDatabase.addDocument(template1, NCCONST.VIEWTEMPLATES);
    template1.addItem(new NotesItemMock("name", NCCONST.TITM_TEMPLATENAME,
            "type", NotesItem.TEXT, "values", "testtemplate1"));
    NotesDocumentMock templateResponse = new NotesDocumentMock();
    templateResponse.addItem(new NotesItemMock("name", NCCONST.FITM_LASTALIAS,
            "type", NotesItem.TEXT, "values", "testtemplate1form"));
    template1.addResponse(templateResponse);

    NotesConnectorSession connectorSession =
        (NotesConnectorSession) connector.login();
    NotesCrawlerThread crawlerThread = new NotesCrawlerThread(connector,
        connectorSession);
    crawlerThread.connectQueue();
    crawlerThread.loadTemplateDoc("testtemplate1");
    crawlerThread.loadForm("testtemplate1form");
    assertNotNull(crawlerThread.formDoc);
    assertEquals("testtemplate1form",
        crawlerThread.formDoc.getItemValueString(NCCONST.FITM_LASTALIAS));
  }

  public void testGetDocumentReaderNames() throws Exception {
    NotesDocumentMock crawlDoc = new NotesDocumentMock();
    NotesDocumentMock srcDoc = new NotesDocumentMock();
    NotesCrawlerThread crawlerThread = new NotesCrawlerThread(null, null);

    crawlerThread.getDocumentReaderNames(crawlDoc, srcDoc);
    assertEquals("",
        crawlDoc.getItemValueString(NCCONST.NCITM_DOCAUTHORREADERS));

    NotesItemMock item = new NotesItemMock("name", "readers",
        "type", NotesItem.TEXT, "values", "reader 1");
    item.setReaders(true);
    srcDoc.addItem(item);
    crawlerThread.getDocumentReaderNames(crawlDoc, srcDoc);
    assertEquals("reader 1",
        crawlDoc.getItemValueString(NCCONST.NCITM_DOCAUTHORREADERS));

    item = new NotesItemMock("name", "readers",
        "type", NotesItem.TEXT, "values", "reader 1", "reader 2");
    item.setReaders(true);
    srcDoc.replaceItemValue("readers", item);
    crawlerThread.getDocumentReaderNames(crawlDoc, srcDoc);
    assertEquals("reader 1;reader 2",
        crawlDoc.getFirstItem(NCCONST.NCITM_DOCAUTHORREADERS).getText(100));

    item = new NotesItemMock("name", "authors", "type", NotesItem.TEXT,
        "values", "author 1");
    item.setAuthors(true);
    srcDoc.addItem(item);
    crawlerThread.getDocumentReaderNames(crawlDoc, srcDoc);
    Vector values = crawlDoc.getItemValue(NCCONST.NCITM_DOCAUTHORREADERS);
    assertEquals(3, values.size());
    assertTrue(values.contains("reader 1"));
    assertTrue(values.contains("reader 2"));
    assertTrue(values.contains("author 1"));
  }

  public void testGetDocumentReaderNamesMultipleFields()
      throws Exception {
    NotesDocumentMock crawlDoc = new NotesDocumentMock();
    NotesDocumentMock srcDoc = new NotesDocumentMock();
    NotesCrawlerThread crawlerThread = new NotesCrawlerThread(null, null);

    crawlerThread.getDocumentReaderNames(crawlDoc, srcDoc);
    assertEquals("",
        crawlDoc.getItemValueString(NCCONST.NCITM_DOCAUTHORREADERS));

    // Check that the values from two Readers field are found.
    NotesItemMock item = new NotesItemMock("name", "readers",
        "type", NotesItem.TEXT, "values", "reader 1");
    item.setReaders(true);
    srcDoc.addItem(item);
    item = new NotesItemMock("name", "readers2",
        "type", NotesItem.TEXT, "values", "reader 2");
    item.setReaders(true);
    srcDoc.addItem(item);
    crawlerThread.getDocumentReaderNames(crawlDoc, srcDoc);
    Vector values = crawlDoc.getItemValue(NCCONST.NCITM_DOCAUTHORREADERS);
    assertEquals(2, values.size());
    assertTrue(values.contains("reader 1"));
    assertTrue(values.contains("reader 2"));

    // Check that the Authors field is added.
    item = new NotesItemMock("name", "authors", "type", NotesItem.TEXT,
        "values", "author 1");
    item.setAuthors(true);
    srcDoc.addItem(item);
    item = new NotesItemMock("name", "authors2", "type", NotesItem.TEXT,
        "values", "author 2", "author 3");
    item.setAuthors(true);
    srcDoc.addItem(item);
    crawlerThread.getDocumentReaderNames(crawlDoc, srcDoc);
    values = crawlDoc.getItemValue(NCCONST.NCITM_DOCAUTHORREADERS);
    assertEquals(5, values.size());
    assertTrue(values.contains("reader 1"));
    assertTrue(values.contains("reader 2"));
    assertTrue(values.contains("author 1"));
    assertTrue(values.contains("author 2"));
    assertTrue(values.contains("author 3"));
  }

  public void testSetDocumentSecurity() throws Exception {
    NotesDocumentMock crawlDoc = new NotesDocumentMock();
    NotesItemMock item = new NotesItemMock("name", NCCONST.NCITM_AUTHTYPE,
        "type", NotesItem.TEXT, "values", NCCONST.AUTH_NONE);
    crawlDoc.addItem(item);

    NotesCrawlerThread crawlerThread = new NotesCrawlerThread(null, null);
    crawlerThread.setDocumentSecurity(crawlDoc, null);
    assertEquals(Boolean.TRUE.toString(),
        crawlDoc.getItemValueString(NCCONST.ITM_ISPUBLIC));

    item = new NotesItemMock("name", NCCONST.NCITM_AUTHTYPE,
        "type", NotesItem.TEXT, "values", NCCONST.AUTH_ACL);
    crawlDoc.replaceItemValue(NCCONST.NCITM_AUTHTYPE, item);
    crawlerThread.setDocumentSecurity(crawlDoc, null);
    assertEquals(Boolean.FALSE.toString(),
        crawlDoc.getItemValueString(NCCONST.ITM_ISPUBLIC));

    item = new NotesItemMock("name", NCCONST.NCITM_AUTHTYPE,
        "type", NotesItem.TEXT, "values", NCCONST.AUTH_CONNECTOR);
    crawlDoc.replaceItemValue(NCCONST.NCITM_AUTHTYPE, item);
    crawlerThread.setDocumentSecurity(crawlDoc, null);
    assertEquals(Boolean.FALSE.toString(),
        crawlDoc.getItemValueString(NCCONST.ITM_ISPUBLIC));
  }

  public void testGetContentFields() throws Exception {
    NotesDocumentMock srcDoc = new NotesDocumentMock();
    NotesItemMock item = new NotesItemMock("name", "field 1",
        "type", NotesItem.TEXT, "values", "value for field 1");
    srcDoc.addItem(item);
    item = new NotesItemMock("name", "field 2",
        "type", NotesItem.TEXT, "values", "value for field 2");
    srcDoc.addItem(item);
    item = new NotesItemMock("name", "form",
        "type", NotesItem.TEXT, "values", "form - should not appear");
    srcDoc.addItem(item);
    item = new NotesItemMock("name", "$field",
        "type", NotesItem.TEXT, "values", "$field - should not appear");
    srcDoc.addItem(item);

    NotesCrawlerThread crawlerThread = new NotesCrawlerThread(null, null);
    String content = crawlerThread.getContentFields(srcDoc);
    assertEquals("\nvalue for field 1\nvalue for field 2", content);

    NotesDocumentMock formDoc = new NotesDocumentMock();
    item = new NotesItemMock("name", NCCONST.FITM_FIELDSTOINDEX,
        "type", NotesItem.TEXT, "values", "field 2");
    formDoc.addItem(item);
    crawlerThread.formDoc = formDoc;
    content = crawlerThread.getContentFields(srcDoc);
    assertEquals("\nvalue for field 2", content);
  }

  public void testNullAttachment() throws Exception {
    NotesDocumentMock docSrc = new NotesDocumentMock();
    NotesCrawlerThread crawlerThread = new NotesCrawlerThread(null, null);
    assertNull("Attachment is not null",
        crawlerThread.createAttachmentDoc(null, docSrc, "nonexistent-file.doc",
        null));
  }
}

