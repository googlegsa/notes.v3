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

import com.google.common.collect.ImmutableSet;
import com.google.enterprise.connector.notes.client.NotesItem;
import com.google.enterprise.connector.notes.client.NotesSession;
import com.google.enterprise.connector.notes.client.mock.NotesDatabaseMock;
import com.google.enterprise.connector.notes.client.mock.NotesDateTimeMock;
import com.google.enterprise.connector.notes.client.mock.NotesDocumentMock;
import com.google.enterprise.connector.notes.client.mock.NotesItemMock;
import com.google.enterprise.connector.notes.client.mock.SessionFactoryMock;
import com.google.enterprise.connector.spi.Document;
import com.google.enterprise.connector.spi.Principal;
import com.google.enterprise.connector.spi.Property;
import com.google.enterprise.connector.spi.SpiConstants;
import com.google.enterprise.connector.spi.SpiConstants.ActionType;
import com.google.enterprise.connector.spi.SpiConstants.CaseSensitivityType;
import com.google.enterprise.connector.spi.SpiConstants.DocumentType;
import com.google.enterprise.connector.spi.SpiConstants.PrincipalType;
import com.google.enterprise.connector.spi.Value;
import com.google.enterprise.connector.spiimpl.PrincipalValue;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Vector;

public class NotesConnectorDocumentTest extends TestCase {

  private static NotesConnector connector;
  private static SessionFactoryMock factory;
  private static NotesConnectorSession connectorSession;
  private static NotesSession session;
  private static NotesDatabaseMock namesDatabase;
  private static NotesDatabaseMock connectorDatabase;
  private static final Calendar testCalendar;
  private static final Date testDate;
  static {
    testDate = new Date();
    testCalendar = Calendar.getInstance();
    testCalendar.setTime(testDate);
  }

  public static Test suite() {
    return new TestSetup(
        new TestSuite(NotesConnectorDocumentTest.class)) {
      @Override protected void setUp() throws Exception {
        connector = NotesConnectorTest.getConnector();
        factory = (SessionFactoryMock) connector.getSessionFactory();
        NotesConnectorSessionTest.configureFactoryForSession(factory);
        connectorSession = (NotesConnectorSession) connector.login();
        session = connectorSession.createNotesSession();
        connectorDatabase = (NotesDatabaseMock) session.getDatabase(
            connectorSession.getServer(), connectorSession.getDatabase());
        namesDatabase = (NotesDatabaseMock) session.getDatabase(
            connectorSession.getServer(), connectorSession.getDirectory());

        NotesUserGroupManagerTest.addNotesUser(connectorSession,
            namesDatabase, "cn=John Smith/ou=Tests/o=Tests", "jsmith");
        NotesUserGroupManagerTest.addNotesUser(connectorSession,
            namesDatabase, "cn=Martha Jones/ou=Tests/o=Tests", "mjones");
        NotesUserGroupManagerTest.addNotesGroup(namesDatabase,
            "adventurers");
        NotesUserGroupManager userGroupManager =
            new NotesUserGroupManager(connectorSession);
        try {
          userGroupManager.setUpResources(true);
          userGroupManager.clearTables(userGroupManager.getConnection());
        } finally {
          userGroupManager.releaseResources();
        }
        userGroupManager.updateUsersGroups();
      }

      @Override protected void tearDown() throws Exception {
        connector.shutdown();
      }
    };
  }

  public NotesConnectorDocumentTest() {
    super();
  }

  public void testSetMetaFieldsText() throws Exception {
    NotesConnectorDocument doc = new NotesConnectorDocument(null, null, null);
    doc.docProps = new HashMap<String, List<Value>>();

    NotesDocumentMock crawlDoc = new NotesDocumentMock();
    crawlDoc.addItem(new NotesItemMock("name", "x.foo", "type", NotesItem.TEXT,
            "values", "this is the text for field foo"));
    doc.crawlDoc = crawlDoc;
    doc.setMetaFields();
    Property p = doc.findProperty("foo");
    assertNotNull("property foo missing", p);
    Value v = p.nextValue();
    assertNotNull("property foo value missing", v);
    assertEquals("property foo", "this is the text for field foo",
        v.toString());
    assertNull(p.nextValue());
  }

  public void testSetMetaFieldsNumber() throws Exception {
    NotesConnectorDocument doc = new NotesConnectorDocument(null, null, null);
    doc.docProps = new HashMap<String, List<Value>>();

    NotesDocumentMock crawlDoc = new NotesDocumentMock();
    crawlDoc.addItem(new NotesItemMock("name", "x.foo", "type",
            NotesItem.NUMBERS, "values", new Double(11)));
    doc.crawlDoc = crawlDoc;
    doc.setMetaFields();
    Property p = doc.findProperty("foo");
    assertNotNull("property foo missing", p);
    Value v = p.nextValue();
    assertNotNull("property foo value missing", v);
    assertEquals("property foo", "11.0", v.toString());
    assertNull(p.nextValue());
  }

  public void testSetMetaFieldsDateTime() throws Exception {
    NotesConnectorDocument doc = new NotesConnectorDocument(null, null, null);
    doc.docProps = new HashMap<String, List<Value>>();

    Date testDate = new Date();
    Calendar testCalendar = Calendar.getInstance();
    testCalendar.setTime(testDate);

    NotesDocumentMock crawlDoc = new NotesDocumentMock();
    crawlDoc.addItem(new NotesItemMock("name", "x.foo", "type",
            NotesItem.NUMBERS, "values", new NotesDateTimeMock(testDate)));
    doc.crawlDoc = crawlDoc;
    doc.setMetaFields();
    Property p = doc.findProperty("foo");
    assertNotNull("property foo missing", p);
    Value v = p.nextValue();
    assertNotNull("property foo value missing", v);
    assertEquals("property foo",
        Value.calendarToIso8601(testCalendar), v.toString());
    assertNull(p.nextValue());
  }

  public void testSetMetaFieldsTextMultipleValues() throws Exception {
    NotesConnectorDocument doc = new NotesConnectorDocument(null, null, null);
    doc.docProps = new HashMap<String, List<Value>>();

    NotesDocumentMock crawlDoc = new NotesDocumentMock();
    crawlDoc.addItem(new NotesItemMock("name", "x.foo", "type", NotesItem.TEXT,
            "values", "foo text 1", "foo text 2", "foo text 3"));
    doc.crawlDoc = crawlDoc;
    doc.setMetaFields();
    Property p = doc.findProperty("foo");
    assertNotNull("property foo missing", p);
    Value v = p.nextValue();
    assertNotNull("property foo value missing", v);
    assertEquals("property foo 1", "foo text 1", v.toString());
    v = p.nextValue();
    assertEquals("property foo 2", "foo text 2", v.toString());
    v = p.nextValue();
    assertEquals("property foo 3", "foo text 3", v.toString());
    assertNull(p.nextValue());
  }

  public void testDeleteDocument() throws Exception {
    NotesDocumentMock crawlDoc = new NotesDocumentMock();
    crawlDoc.addItem(new NotesItemMock("name", NCCONST.ITM_ACTION,
            "type", NotesItem.TEXT, "values", ActionType.DELETE));
    crawlDoc.addItem(new NotesItemMock("name", NCCONST.ITM_DOCID, "type",
            NotesItem.TEXT, "values", "docid"));

    NotesConnectorDocument document =
        new NotesConnectorDocument(null, null, null);
    document.setCrawlDoc("unid", crawlDoc);
    assertEquals(2, document.docProps.size());
    assertPropertyEquals("docid", document, SpiConstants.PROPNAME_DOCID);
    assertPropertyEquals(ActionType.DELETE.toString(), document,
        SpiConstants.PROPNAME_ACTION);
  }

  public void testAddDocument() throws Exception {
    NotesDocumentMock crawlDoc = getCrawlDoc(false);

    NotesConnectorDocument document = new NotesConnectorDocument(
        connectorSession, session, connectorDatabase);
    document.setCrawlDoc("unid", crawlDoc);

    assertPropertyEquals("http://host:42/replicaid/0/docid",
        document, SpiConstants.PROPNAME_DOCID);
    assertPropertyEquals(ActionType.ADD.toString(), document,
        SpiConstants.PROPNAME_ACTION);
    assertPropertyEquals("This is the title", document,
        SpiConstants.PROPNAME_TITLE);
    assertPropertyEquals("text/plain", document,
        SpiConstants.PROPNAME_MIMETYPE);
    assertPropertyEquals("http://host:42/replicaid/0/docid",
        document, SpiConstants.PROPNAME_DISPLAYURL);
    assertPropertyEquals("true", document, SpiConstants.PROPNAME_ISPUBLIC);
    assertPropertyEquals("CATEGORY 1", document, NCCONST.PROPNAME_NCCATEGORIES);
    assertPropertyEquals("CATEGORY 2", document, NCCONST.PROPNAME_NCCATEGORIES,
      1);
    assertPropertyEquals(Value.calendarToIso8601(testCalendar),
        document, SpiConstants.PROPNAME_LASTMODIFIED);
    assertPropertyEquals("http://host:42/replicaid/"
        + NCCONST.DB_ACL_INHERIT_TYPE_PARENTOVERRIDES,
        document, SpiConstants.PROPNAME_ACLINHERITFROM_DOCID);
    assertNull(document.findProperty(SpiConstants.PROPNAME_ACLUSERS));
    assertNull(document.findProperty(SpiConstants.PROPNAME_ACLGROUPS));
  }

  public void testDocumentDisplayURL() throws Exception {
    NotesDocumentMock crawlDoc = getAttachmentDoc();
    NotesConnectorDocument document = new NotesConnectorDocument(
        connectorSession, session, connectorDatabase);
    document.setCrawlDoc("unid123", crawlDoc);
    Property displayUrl =
        document.findProperty(SpiConstants.PROPNAME_DISPLAYURL);
    assertNotNull(displayUrl);
  }

  public void testAttachmentURLs() throws Exception {
    NotesDocumentMock crawlDoc = getAttachmentDoc();
    NotesConnectorDocument document = new NotesConnectorDocument(
        connectorSession, session, connectorDatabase);
    document.setCrawlDoc("unid123", crawlDoc);
    assertPropertyEquals("http://host:42/replicaid/0/unid/$File/attachmentUnid",
        document, SpiConstants.PROPNAME_DOCID);
    assertPropertyEquals(
        "http://host:42/replicaid/0/unid/$File/filename.doc?OpenElement",
        document, SpiConstants.PROPNAME_DISPLAYURL);
  }

  public void testAddDocumentWithReaders() throws Exception {
    NotesDocumentMock crawlDoc = getCrawlDoc(true);
    NotesConnectorDocument document = new NotesConnectorDocument(
        connectorSession, session, connectorDatabase);
    document.setCrawlDoc("unid", crawlDoc);

    assertPropertyEquals("http://host:42/replicaid/0/docid",
        document, SpiConstants.PROPNAME_DOCID);
    assertPropertyEquals(ActionType.ADD.toString(), document,
        SpiConstants.PROPNAME_ACTION);
    assertPropertyEquals("This is the title", document,
        SpiConstants.PROPNAME_TITLE);
    assertPropertyEquals("text/plain", document,
        SpiConstants.PROPNAME_MIMETYPE);
    assertPropertyEquals("http://host:42/replicaid/0/docid",
        document, SpiConstants.PROPNAME_DISPLAYURL);
    assertPropertyEquals("true", document, SpiConstants.PROPNAME_ISPUBLIC);
    assertPropertyEquals("CATEGORY 1", document, NCCONST.PROPNAME_NCCATEGORIES);
    assertPropertyEquals("CATEGORY 2", document, NCCONST.PROPNAME_NCCATEGORIES,
      1);
    assertPropertyEquals(Value.calendarToIso8601(testCalendar),
        document, SpiConstants.PROPNAME_LASTMODIFIED);
    assertPropertyEquals("http://host:42/replicaid/"
        + NCCONST.DB_ACL_INHERIT_TYPE_ANDBOTH,
        document, SpiConstants.PROPNAME_ACLINHERITFROM_DOCID);
    assertEquals(ImmutableSet.of("jsmith"),
        getPrincipalNames(document, SpiConstants.PROPNAME_ACLUSERS));
    assertEquals(ImmutableSet.of("Domino%2F*", "Domino%2Freadergroup",
            "Domino%2Freplicaid%2F%5Breaderrole%5D"),
        getPrincipalNames(document, SpiConstants.PROPNAME_ACLGROUPS));
  }

  public void testDocumentPrincipalValues() throws Exception {
    NotesDocumentMock crawlDoc = getCrawlDoc(true);
    NotesConnectorSession connectorSession =
        (NotesConnectorSession) connector.login();
    NotesSession session = connectorSession.createNotesSession();
    NotesDatabaseMock connectorDatabase =
        (NotesDatabaseMock) session.getDatabase(
        connectorSession.getServer(), connectorSession.getDatabase());

    NotesConnectorDocument document = new NotesConnectorDocument(
        connectorSession, session, connectorDatabase);
    document.setCrawlDoc("unid", crawlDoc);

    // Check defaults.
    assertEquals(getExpectedPrincipals(PrincipalType.UNKNOWN,
            connector.getGlobalNamespace(), ImmutableSet.of("jsmith"),
            CaseSensitivityType.EVERYTHING_CASE_INSENSITIVE),
        getPrincipals(document, SpiConstants.PROPNAME_ACLUSERS));
    assertEquals(getExpectedPrincipals(PrincipalType.UNQUALIFIED,
            connector.getLocalNamespace(),
            ImmutableSet.of("Domino%2F*", "Domino%2Freadergroup",
                "Domino%2Freplicaid%2F%5Breaderrole%5D"),
            CaseSensitivityType.EVERYTHING_CASE_INSENSITIVE),
        getPrincipals(document, SpiConstants.PROPNAME_ACLGROUPS));

    // Change usernames to local namespace. Groups should stay local.
    try {
      connector.setGsaNamesAreGlobal(false);
      document =
          new NotesConnectorDocument(connectorSession, session, connectorDatabase);
      document.setCrawlDoc("unid", crawlDoc);
      assertEquals(getExpectedPrincipals(PrincipalType.UNQUALIFIED,
              connector.getLocalNamespace(), ImmutableSet.of("jsmith"),
              CaseSensitivityType.EVERYTHING_CASE_INSENSITIVE),
          getPrincipals(document, SpiConstants.PROPNAME_ACLUSERS));
      assertEquals(getExpectedPrincipals(PrincipalType.UNQUALIFIED,
              connector.getLocalNamespace(),
              ImmutableSet.of("Domino%2F*", "Domino%2Freadergroup",
                  "Domino%2Freplicaid%2F%5Breaderrole%5D"),
              CaseSensitivityType.EVERYTHING_CASE_INSENSITIVE),
          getPrincipals(document, SpiConstants.PROPNAME_ACLGROUPS));
    } finally {
      connector.setGsaNamesAreGlobal(true);
    }
  }

  public void testAddDatabaseAcl() throws Exception {
    NotesDocumentMock crawlDoc = getCrawlDatabaseAcl();
    NotesConnectorSession connectorSession =
        (NotesConnectorSession) connector.login();
    NotesSession session = connectorSession.createNotesSession();
    NotesDatabaseMock connectorDatabase =
        (NotesDatabaseMock) session.getDatabase(
        connectorSession.getServer(), connectorSession.getDatabase());

    NotesConnectorDocument document = new NotesConnectorDocument(
        connectorSession, session, connectorDatabase);
    document.setCrawlDoc("unid", crawlDoc);

    assertPropertyEquals("docid", document, SpiConstants.PROPNAME_DOCID);
    assertPropertyEquals(ActionType.ADD.toString(), document,
        SpiConstants.PROPNAME_ACTION);
    assertPropertyEquals(
        SpiConstants.AclInheritanceType.AND_BOTH_PERMIT.toString(),
        document, SpiConstants.PROPNAME_ACLINHERITANCETYPE);
    assertPropertyEquals(DocumentType.ACL.toString(), document,
            SpiConstants.PROPNAME_DOCUMENTTYPE);
    assertEquals(ImmutableSet.of("user1", "user2"),
        getPrincipalNames(document, SpiConstants.PROPNAME_ACLUSERS));
    assertEquals(ImmutableSet.of("user3", "user4"),
        getPrincipalNames(document, SpiConstants.PROPNAME_ACLDENYUSERS));
    assertEquals(ImmutableSet.of("Domino%2Fgroup1", "Domino%2Fgroup2"),
        getPrincipalNames(document, SpiConstants.PROPNAME_ACLGROUPS));
    assertEquals(ImmutableSet.of("Domino%2Fgroup3", "Domino%2Fgroup4"),
        getPrincipalNames(document, SpiConstants.PROPNAME_ACLDENYGROUPS));
  }

  public void testDatabaseAclPrincipalValues() throws Exception {
    NotesDocumentMock crawlDoc = getCrawlDatabaseAcl();
    NotesConnectorSession connectorSession =
        (NotesConnectorSession) connector.login();
    NotesSession session = connectorSession.createNotesSession();
    NotesDatabaseMock connectorDatabase =
        (NotesDatabaseMock) session.getDatabase(
        connectorSession.getServer(), connectorSession.getDatabase());

    NotesConnectorDocument document = new NotesConnectorDocument(
        connectorSession, session, connectorDatabase);
    document.setCrawlDoc("unid", crawlDoc);

    // Check defaults.
    assertTrue(connector.getGsaNamesAreGlobal());
    assertEquals(getExpectedPrincipals(PrincipalType.UNKNOWN,
            connector.getGlobalNamespace(), ImmutableSet.of("user1", "user2"),
            CaseSensitivityType.EVERYTHING_CASE_INSENSITIVE),
        getPrincipals(document, SpiConstants.PROPNAME_ACLUSERS));
    assertEquals(getExpectedPrincipals(PrincipalType.UNQUALIFIED,
            connector.getLocalNamespace(),
            ImmutableSet.of("Domino%2Fgroup1", "Domino%2Fgroup2"),
            CaseSensitivityType.EVERYTHING_CASE_INSENSITIVE),
        getPrincipals(document, SpiConstants.PROPNAME_ACLGROUPS));

    // Change usernames to local namespace. Groups should stay local.
    try {
      connector.setGsaNamesAreGlobal(false);
      document =
          new NotesConnectorDocument(connectorSession, session, connectorDatabase);
      document.setCrawlDoc("unid", crawlDoc);
      assertEquals(getExpectedPrincipals(PrincipalType.UNQUALIFIED,
              connector.getLocalNamespace(), ImmutableSet.of("user1", "user2"),
              CaseSensitivityType.EVERYTHING_CASE_INSENSITIVE),
          getPrincipals(document, SpiConstants.PROPNAME_ACLUSERS));
      assertEquals(getExpectedPrincipals(PrincipalType.UNQUALIFIED,
              connector.getLocalNamespace(),
              ImmutableSet.of("Domino%2Fgroup1", "Domino%2Fgroup2"),
              CaseSensitivityType.EVERYTHING_CASE_INSENSITIVE),
          getPrincipals(document, SpiConstants.PROPNAME_ACLGROUPS));
    } finally {
      connector.setGsaNamesAreGlobal(true);
    }
  }

  private void assertPropertyEquals(String expected,
      NotesConnectorDocument document, String property) throws Exception {
    assertPropertyEquals(expected, document, property, 0);
  }

  private void assertPropertyEquals(String expected,
      NotesConnectorDocument document, String property, int index)
      throws Exception {
    Property p = document.findProperty(property);
    assertNotNull("Missing property " + property, p);

    int i = 0;
    Value v;
    while ((v = p.nextValue()) != null) {
      if (i == index) {
        assertEquals(expected, v.toString());
        return;
      }
      i++;
    }
    fail("No value for property at index: " + property + "/" + index);
  }

  private NotesDocumentMock getCrawlDatabaseAcl() throws Exception {
    // Mimic NotesDatabasePoller.createDatabaseAclDocument
    NotesDocumentMock crawlDoc = new NotesDocumentMock();
    crawlDoc.addItem(new NotesItemMock("name", NCCONST.NCITM_DBACL, "type",
            NotesItem.TEXT, "values", "true"));
    crawlDoc.addItem(new NotesItemMock("name", NCCONST.NCITM_DBACLINHERITTYPE,
            "type", NotesItem.TEXT, "values",
            NCCONST.DB_ACL_INHERIT_TYPE_ANDBOTH));
    crawlDoc.addItem(new NotesItemMock("name", NCCONST.NCITM_STATE,
            "type", NotesItem.TEXT, "values", NCCONST.STATEFETCHED));
    crawlDoc.addItem(new NotesItemMock("name", NCCONST.ITM_ACTION,
            "type", NotesItem.TEXT, "values", ActionType.ADD));
    crawlDoc.addItem(new NotesItemMock("name", NCCONST.NCITM_UNID, "type",
            NotesItem.TEXT, "values", "unid"));
    crawlDoc.addItem(new NotesItemMock("name", NCCONST.ITM_DOCID, "type",
            NotesItem.TEXT, "values", "docid"));
    crawlDoc.addItem(new NotesItemMock("name", NCCONST.NCITM_DBPERMITUSERS,
            "type", NotesItem.TEXT, "values", "user1", "user2"));
    crawlDoc.addItem(new NotesItemMock("name", NCCONST.NCITM_DBNOACCESSUSERS,
            "type", NotesItem.TEXT, "values", "user3", "user4"));
    crawlDoc.addItem(new NotesItemMock("name", NCCONST.NCITM_DBPERMITGROUPS,
            "type", NotesItem.TEXT, "values",
            "Domino%2Fgroup1", "Domino%2Fgroup2"));
    crawlDoc.addItem(new NotesItemMock("name", NCCONST.NCITM_DBNOACCESSGROUPS,
            "type", NotesItem.TEXT, "values",
            "Domino%2Fgroup3", "Domino%2Fgroup4"));
    return crawlDoc;
  }

  private NotesDocumentMock getCrawlDoc(boolean hasReaders)
      throws Exception {
    NotesDocumentMock crawlDoc = new NotesDocumentMock();
    crawlDoc.addItem(new NotesItemMock("name", NCCONST.ITM_ACTION,
            "type", NotesItem.TEXT, "values", ActionType.ADD));
    crawlDoc.addItem(new NotesItemMock("name", NCCONST.ITM_DOCID, "type",
            NotesItem.TEXT, "values", "http://host:42/replicaid/0/docid"));
    crawlDoc.addItem(new NotesItemMock("name", NCCONST.ITM_DISPLAYURL, "type",
            NotesItem.TEXT, "values", "http://host:42/replicaid/0/docid"));
    crawlDoc.addItem(new NotesItemMock("name", NCCONST.NCITM_REPLICAID,
            "type", NotesItem.TEXT, "values", "replicaid"));
    crawlDoc.addItem(new NotesItemMock("name", NCCONST.ITM_TITLE, "type",
            NotesItem.TEXT, "values", "This is the title"));
    crawlDoc.addItem(new NotesItemMock("name", NCCONST.ITM_MIMETYPE, "type",
            NotesItem.TEXT, "values", "text/plain"));
    crawlDoc.addItem(new NotesItemMock("name", NCCONST.ITM_ISPUBLIC, "type",
            NotesItem.TEXT, "values", "true"));
    crawlDoc.addItem(new NotesItemMock("name",
            NCCONST.ITM_GMETADESCRIPTION, "type",
            NotesItem.TEXT, "values", "This is the description"));
    crawlDoc.addItem(new NotesItemMock("name", NCCONST.ITM_GMETADATABASE,
            "type", NotesItem.TEXT, "values", "crawled database"));
    crawlDoc.addItem(new NotesItemMock("name", NCCONST.ITM_GMETACATEGORIES,
            "type", NotesItem.TEXT, "values", "CATEGORY 1", "CATEGORY 2"));
    crawlDoc.addItem(new NotesItemMock("name", NCCONST.ITM_GMETAREPLICASERVERS,
            "type", NotesItem.TEXT, "values", "replica server"));
    crawlDoc.addItem(new NotesItemMock("name", NCCONST.ITM_GMETANOTESLINK,
            "type", NotesItem.TEXT, "values", "/notes/link"));
    crawlDoc.addItem(new NotesItemMock("name", NCCONST.ITM_GMETAWRITERNAME,
            "type", NotesItem.TEXT, "values", "An Author"));
    crawlDoc.addItem(new NotesItemMock("name", NCCONST.ITM_GMETAFORM, "type",
            NotesItem.TEXT, "values", "docform"));
    crawlDoc.addItem(new NotesItemMock("name", NCCONST.ITM_CONTENT, "type",
            NotesItem.TEXT, "values", "This is the content"));
    crawlDoc.addItem(new NotesItemMock("name", NCCONST.NCITM_AUTHTYPE, "type",
            NotesItem.TEXT, "values", NCCONST.AUTH_ACL));
    if (hasReaders) {
      Vector<String> readers = new Vector<String>();
      readers.add("cn=John Smith/ou=Tests/o=Tests");
      readers.add("*");
      crawlDoc.addItem(new NotesItemMock("name", NCCONST.NCITM_DOCAUTHORREADERS,
              "type", NotesItem.TEXT, "values", readers,
              "readergroup", "[readerrole]"));
    }

    crawlDoc.addItem(new NotesItemMock("name", NCCONST.ITM_GMETALASTUPDATE,
            "type", NotesItem.DATETIMES, "values", testDate));
    crawlDoc.addItem(new NotesItemMock("name", NCCONST.ITM_GMETACREATEDATE,
            "type", NotesItem.DATETIMES, "values", testDate));

    return crawlDoc;
  }

  private NotesDocumentMock getAttachmentDoc() throws Exception {
    NotesDocumentMock crawlDoc = getCrawlDoc(false);
    crawlDoc.replaceItemValue(NCCONST.ITM_DOCID,
        String.format(NCCONST.SITM_ATTACHMENTDOCID,
        "http://host:42/replicaid/0/unid", "attachmentUnid"));
    crawlDoc.replaceItemValue(NCCONST.ITM_DISPLAYURL,
        String.format(NCCONST.SITM_ATTACHMENTDISPLAYURL,
        "http://host:42/replicaid/0/unid", "filename.doc"));
    return crawlDoc;
  }

  /** Gets all values of the given property, which must be Principals. */
  private Set<Principal> getPrincipals(Document document,
      String propertyName) throws Exception {
    Property property = document.findProperty(propertyName);
    assertNotNull("Missing " + propertyName, property);
    ImmutableSet.Builder<Principal> builder =
        new ImmutableSet.Builder<Principal>();
    Value value;
    while ((value = property.nextValue()) != null) {
      assertTrue("Not PrincipalValue: " + propertyName,
          value instanceof PrincipalValue);
      builder.add(((PrincipalValue) value).getPrincipal());
    }
    return builder.build();
  }

  /** Gets the principal names for all values of the given property. */
  private Set<String> getPrincipalNames(Document document,
      String propertyName) throws Exception {
    Property property = document.findProperty(propertyName);
    assertNotNull("Missing " + propertyName, property);
    ImmutableSet.Builder<String> builder = new ImmutableSet.Builder<String>();
    Value value;
    while ((value = property.nextValue()) != null) {
      assertTrue("Not PrincipalValue: " + propertyName,
          value instanceof PrincipalValue);
      builder.add(((PrincipalValue) value).getPrincipal().getName());
    }
    return builder.build();
  }

  /** Constructs a set of expected Principals with the given options. */
  private Set<Principal> getExpectedPrincipals(PrincipalType principalType,
      String namespace, Set<String> names,
      CaseSensitivityType caseSensitivityType) {
    ImmutableSet.Builder<Principal> builder =
        new ImmutableSet.Builder<Principal>();
    for (String name : names) {
      builder.add(
          new Principal(principalType, namespace, name, caseSensitivityType));
    }
    return builder.build();
  }
}
