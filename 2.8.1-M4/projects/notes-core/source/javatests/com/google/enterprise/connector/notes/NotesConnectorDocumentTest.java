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

import com.google.enterprise.connector.notes.client.NotesItem;
import com.google.enterprise.connector.notes.client.mock.NotesDateTimeMock;
import com.google.enterprise.connector.notes.client.mock.NotesDocumentMock;
import com.google.enterprise.connector.spi.Property;
import com.google.enterprise.connector.spi.Value;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class NotesConnectorDocumentTest extends TestCase {

  public NotesConnectorDocumentTest() {
    super();
  }

  public void testSetMetaFieldsText() throws Exception {
    NotesConnectorDocument doc = new NotesConnectorDocument();
    doc.docProps = new HashMap<String, List<Value>>();

    NotesDocumentMock crawlDoc = new NotesDocumentMock();
    crawlDoc.addItem("name", "x.foo", "type", NotesItem.TEXT,
        "values", "this is the text for field foo");
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
    NotesConnectorDocument doc = new NotesConnectorDocument();
    doc.docProps = new HashMap<String, List<Value>>();

    NotesDocumentMock crawlDoc = new NotesDocumentMock();
    crawlDoc.addItem("name", "x.foo", "type", NotesItem.NUMBERS,
        "values", new Double(11));
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
    NotesConnectorDocument doc = new NotesConnectorDocument();
    doc.docProps = new HashMap<String, List<Value>>();

    Date testDate = new Date();
    Calendar testCalendar = Calendar.getInstance();
    testCalendar.setTime(testDate);

    NotesDocumentMock crawlDoc = new NotesDocumentMock();
    crawlDoc.addItem("name", "x.foo", "type", NotesItem.NUMBERS,
        "values", new NotesDateTimeMock(testDate));
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
    NotesConnectorDocument doc = new NotesConnectorDocument();
    doc.docProps = new HashMap<String, List<Value>>();

    NotesDocumentMock crawlDoc = new NotesDocumentMock();
    crawlDoc.addItem("name", "x.foo", "type", NotesItem.TEXT,
        "values", "foo text 1", "foo text 2", "foo text 3");
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
}
