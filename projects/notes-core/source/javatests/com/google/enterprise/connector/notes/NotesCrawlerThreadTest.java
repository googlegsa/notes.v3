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

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import com.google.enterprise.connector.notes.client.NotesDatabase;
import com.google.enterprise.connector.notes.client.NotesDocument;
import com.google.enterprise.connector.notes.client.NotesEmbeddedObject;
import com.google.enterprise.connector.notes.client.NotesItem;
import com.google.enterprise.connector.notes.client.NotesSession;
import com.google.enterprise.connector.notes.client.NotesView;
import com.google.enterprise.connector.notes.client.mock.NotesDatabaseMock;
import com.google.enterprise.connector.notes.client.mock.NotesDocumentMock;
import com.google.enterprise.connector.notes.client.mock.NotesItemMock;
import com.google.enterprise.connector.notes.client.mock.SessionFactoryMock;

import junit.framework.TestCase;

import org.easymock.Capture;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.Vector;

public class NotesCrawlerThreadTest extends TestCase {

  private NotesConnector connector;
  private SessionFactoryMock factory;
  private NotesConnectorSession connectorSession;
  private NotesSession session;
  private NotesDatabaseMock dbConfigMock;

  public NotesCrawlerThreadTest() {
    super();
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    connector = NotesConnectorTest.getConnector();
    factory = (SessionFactoryMock) connector.getSessionFactory();
    NotesConnectorSessionTest.configureFactoryForSession(factory);
    connectorSession = (NotesConnectorSession) connector.login();
    session = connectorSession.createNotesSession();

    dbConfigMock =
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

  @Override
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

    crawlerThread.setDocumentReaderNames(crawlDoc, srcDoc);
    assertEquals("",
        crawlDoc.getItemValueString(NCCONST.NCITM_DOCAUTHORREADERS));

    NotesItemMock item = new NotesItemMock("name", "readers",
        "type", NotesItem.TEXT, "values", "reader 1");
    item.setReaders(true);
    srcDoc.addItem(item);
    crawlerThread.setDocumentReaderNames(crawlDoc, srcDoc);
    assertEquals("reader 1",
        crawlDoc.getItemValueString(NCCONST.NCITM_DOCAUTHORREADERS));

    item = new NotesItemMock("name", "readers",
        "type", NotesItem.TEXT, "values", "reader 1", "reader 2");
    item.setReaders(true);
    srcDoc.replaceItemValue("readers", item);
    crawlerThread.setDocumentReaderNames(crawlDoc, srcDoc);
    assertEquals("reader 1;reader 2",
        crawlDoc.getFirstItem(NCCONST.NCITM_DOCAUTHORREADERS).getText(100));

    item = new NotesItemMock("name", "authors", "type", NotesItem.TEXT,
        "values", "author 1");
    item.setAuthors(true);
    srcDoc.addItem(item);
    crawlerThread.setDocumentReaderNames(crawlDoc, srcDoc);
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

    crawlerThread.setDocumentReaderNames(crawlDoc, srcDoc);
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
    crawlerThread.setDocumentReaderNames(crawlDoc, srcDoc);
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
    crawlerThread.setDocumentReaderNames(crawlDoc, srcDoc);
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
    crawlerThread.setDocumentSecurity(crawlDoc);
    assertEquals(Boolean.TRUE.toString(),
        crawlDoc.getItemValueString(NCCONST.ITM_ISPUBLIC));

    item = new NotesItemMock("name", NCCONST.NCITM_AUTHTYPE,
        "type", NotesItem.TEXT, "values", NCCONST.AUTH_ACL);
    crawlDoc.replaceItemValue(NCCONST.NCITM_AUTHTYPE, item);
    crawlerThread.setDocumentSecurity(crawlDoc);
    assertEquals(Boolean.FALSE.toString(),
        crawlDoc.getItemValueString(NCCONST.ITM_ISPUBLIC));

    item = new NotesItemMock("name", NCCONST.NCITM_AUTHTYPE,
        "type", NotesItem.TEXT, "values", NCCONST.AUTH_CONNECTOR);
    crawlDoc.replaceItemValue(NCCONST.NCITM_AUTHTYPE, item);
    crawlerThread.setDocumentSecurity(crawlDoc);
    assertEquals(Boolean.FALSE.toString(),
        crawlDoc.getItemValueString(NCCONST.ITM_ISPUBLIC));
  }

  private NotesDocumentMock getSourceDocument() throws Exception {
    NotesDocumentMock srcDoc = new NotesDocumentMock();
    NotesItemMock item = new NotesItemMock("name", "field 1",
        "type", NotesItem.TEXT, "values", "value for field 1");
    srcDoc.addItem(item);
    item = new NotesItemMock("name", "field 2",
        "type", NotesItem.TEXT, "values", "value for field 2");
    srcDoc.addItem(item);
    item = new NotesItemMock("name", "field 2",
        "type", NotesItem.TEXT, "values", "same field2 - should not appear");
    srcDoc.addItem(item);
    item = new NotesItemMock("name", "form",
        "type", NotesItem.TEXT, "values", "form - should not appear");
    srcDoc.addItem(item);
    item = new NotesItemMock("name", "$field",
        "type", NotesItem.TEXT, "values", "$field - should not appear");
    srcDoc.addItem(item);
    item = new NotesItemMock("name", "attachment",
        "type", NotesItem.ATTACHMENT,
        "values", "attachment - should not appear");
    srcDoc.addItem(item);
    return srcDoc;
  }

  public void testGetContentFields_SourceDoc() throws Exception {
    NotesDocumentMock srcDoc = getSourceDocument();

    NotesCrawlerThread crawlerThread = new NotesCrawlerThread(null, null);
    String content = crawlerThread.getContentFields(srcDoc);
    assertEquals("\nvalue for field 1\nvalue for field 2", content);
  }

  public void testGetContentFields_FormDoc() throws Exception {
    NotesDocumentMock srcDoc = getSourceDocument();

    NotesCrawlerThread crawlerThread = new NotesCrawlerThread(null, null);
    NotesDocumentMock formDoc = new NotesDocumentMock();
    NotesItemMock item = new NotesItemMock("name", NCCONST.FITM_FIELDSTOINDEX,
        "type", NotesItem.TEXT, "values", "field 2");
    formDoc.addItem(item);
    crawlerThread.formDoc = formDoc;
    String content = crawlerThread.getContentFields(srcDoc);
    assertEquals("\nvalue for field 2", content);
  }

  public void testNullAttachment() throws Exception {
    NotesDocumentMock docSrc = new NotesDocumentMock();
    NotesCrawlerThread crawlerThread = new NotesCrawlerThread(null, null);
    assertNull("Attachment is not null",
        crawlerThread.createAttachmentDoc(null, docSrc, "nonexistent-file.doc",
        null));
  }

  public void testLongAttachmentFileName() throws Exception {
    String attachmentName = "this_is_a_very_long_file_name_"
        + "this_is_a_very_long_file_name_this_is_a_very_long_file_name_"
        + "this_is_a_very_long_file_name_this_is_a_very_long_file_name_"
        + "this_is_a_very_long_file_name_this_is_a_very_long_file_name.doc";

    NotesConnector nc = createNiceMock(NotesConnector.class);
    NotesConnectorSession ncs = createNiceMock(NotesConnectorSession.class);
    NotesSession ns = createMock(NotesSession.class);
    NotesDatabase cdb = createNiceMock(NotesDatabase.class);
    NotesView crawlQ = createNiceMock(NotesView.class);
    expect(ncs.getServer()).andReturn("domino1");
    expect(ncs.getDatabase()).andReturn("gsaconfig.nsf");
    expect(ncs.getSpoolDir()).andReturn("spooldir");
    expect(ncs.createNotesSession()).andReturn(ns);
    expect(ns.getDatabase(isA(String.class), isA(String.class))).andReturn(cdb);
    expect(cdb.getView(isA(String.class))).andReturn(crawlQ);
    expect(cdb.getReplicaID()).andReturn("REPLICA100");

    NotesDocument docCrawl = createNiceMock(NotesDocument.class);
    expect(docCrawl.getUniversalID()).andReturn("UNID100");

    NotesDocument docSrc = createNiceMock(NotesDocument.class);
    NotesEmbeddedObject embObj = createNiceMock(NotesEmbeddedObject.class);
    expect(docSrc.getAttachment(attachmentName)).andReturn(embObj);
    expect(embObj.getType()).andReturn(NotesEmbeddedObject.EMBED_ATTACHMENT);
    expect(embObj.getFileSize()).andReturn(1);

    NotesDocument docAttach = createNiceMock(NotesDocument.class);
    expect(cdb.createDocument()).andReturn(docAttach);
    Capture<String> captureContentPath = new Capture<String>();
    expect(docAttach.replaceItemValue(eq(NCCONST.ITM_CONTENTPATH),
        capture(captureContentPath))).andReturn(null);
    replay(ncs, ns, cdb, crawlQ, docCrawl, docSrc, docAttach, embObj);

    NotesCrawlerThread crawler = new NotesCrawlerThread(nc, ncs);
    crawler.connectQueue();
    String attachmentId =
        crawler.createAttachmentDoc(docCrawl, docSrc, attachmentName, "Text");

    assertNotNull(attachmentId);
    assertEquals(40, attachmentId.length());
    assertEquals("spooldir/attachments/REPLICA100/UNID100/" + attachmentId,
        captureContentPath.getValue());
    verify(docAttach);
  }

  public void testSendDeleteRequests() throws Exception {
    NotesDocId notesId = new NotesDocId("http://testserver/replicaid/0/unid");
    
    NotesDocumentMock crawlDoc = new NotesDocumentMock();
    crawlDoc.replaceItemValue(NCCONST.NCITM_SERVER, notesId.getServer());
    crawlDoc.replaceItemValue(NCCONST.NCITM_REPLICAID, notesId.getReplicaId());
    crawlDoc.replaceItemValue(NCCONST.NCITM_UNID, notesId.getDocId());
    crawlDoc.replaceItemValue(NCCONST.ITM_DOCID, notesId.toString());
    
    Vector<String> attachmentNames = new Vector<String>();
    attachmentNames.add("attachment1.doc");
    attachmentNames.add("attachment2.doc");
    crawlDoc.replaceItemValue(NCCONST.ITM_GMETAATTACHMENTDOCIDS,
        attachmentNames);

    NotesDocumentManager docMgr = connectorSession.getNotesDocumentManager();

    Connection conn = docMgr.getDatabaseConnection();
    try {
      docMgr.addIndexedDocument(crawlDoc, conn);
      Set<String> attachUnids = docMgr.getAttachmentIds(conn,
          notesId.getDocId(), notesId.getReplicaId());
      assertEquals(2, attachUnids.size());

      NotesCrawlerThread crawlerThread = new NotesCrawlerThread(connector,
          connectorSession);
      crawlerThread.connectQueue();

      // Replace attachment
      attachmentNames.set(0, "attachmentX.doc");
      crawlerThread.enqueue(notesId, attachmentNames);
      String expectedReplaceId = String.format(NCCONST.SITM_ATTACHMENTDOCID,
          notesId.toString(), "attachment1.doc");
      List<NotesDocumentMock> doclist1 = dbConfigMock.getDocumentsByField(
          NCCONST.ITM_DOCID, expectedReplaceId);
      assertEquals("No delete request is created for replaced attachment", 1,
          doclist1.size());
      NotesDocument deleteReq = doclist1.get(0);
      assertEquals("Action is not equal delete", "delete",
          deleteReq.getItemValueString(NCCONST.ITM_ACTION));
      crawlDoc.replaceItemValue(NCCONST.ITM_GMETAATTACHMENTDOCIDS,
          attachmentNames);
      docMgr.addIndexedDocument(crawlDoc, conn);

      // Remove attachment
      String expectedRemoveId = String.format(NCCONST.SITM_ATTACHMENTDOCID,
          notesId.toString(), attachmentNames.get(0));
      attachmentNames.remove(0);
      crawlerThread.enqueue(notesId, attachmentNames);
      List<NotesDocumentMock> doclist2 =
          dbConfigMock.getDocumentsByField(NCCONST.ITM_DOCID, expectedRemoveId);
      assertEquals("No delete request is created for removed attachment", 1,
          doclist2.size());
      NotesDocument deleteReq2 = doclist2.get(0);
      assertEquals("Action is not equal delete", "delete",
          deleteReq2.getItemValueString(NCCONST.ITM_ACTION));
    } finally {
      docMgr.releaseDatabaseConnection(conn);
    }
  }
}

