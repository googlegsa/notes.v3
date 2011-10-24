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
import com.google.enterprise.connector.notes.client.mock.NotesDocumentMock;

import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class NotesCrawlerThreadTest extends TestCase {

  public NotesCrawlerThreadTest() {
    super();
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
    sourceDoc.addItem("name", "foo", "type", NotesItem.TEXT,
        "values", "this is the text for field foo");
    sourceDoc.addItem("name", "bar", "type", NotesItem.TEXT,
        "values", "this is the text for field bar");
    sourceDoc.addItem("name", "baz", "type", NotesItem.TEXT,
        "values", "this is the text for field baz");

    crawler.mapMetaFields(crawlDoc, sourceDoc);
    assertTrue("crawl doc missing x.foo", crawlDoc.hasItem("x.foo"));
    assertEquals("this is the text for field foo",
        crawlDoc.getItemValueString("x.foo"));
    assertTrue("crawl doc missing x.mappedbar", crawlDoc.hasItem("x.mappedbar"));
    assertEquals("this is the text for field bar",
        crawlDoc.getItemValueString("x.mappedbar"));
    assertFalse("crawl doc has x.mappedbaz", crawlDoc.hasItem("x.mappedbaz"));

    // Add a form name to the doc. Now the metafield mapping with
    // a form qualification should apply.
    sourceDoc.addItem("name", "form", "type", NotesItem.TEXT,
        "values", "bazform");
    crawlDoc = new NotesDocumentMock();
    crawler.mapMetaFields(crawlDoc, sourceDoc);
    assertTrue("crawl doc missing x.foo", crawlDoc.hasItem("x.foo"));
    assertEquals("this is the text for field foo",
        crawlDoc.getItemValueString("x.foo"));
    assertTrue("crawl doc missing x.mappedbar", crawlDoc.hasItem("x.mappedbar"));
    assertEquals("this is the text for field bar",
        crawlDoc.getItemValueString("x.mappedbar"));
    assertTrue("crawl doc missing has x.mappedbaz",
        crawlDoc.hasItem("x.mappedbaz"));
    assertEquals("this is the text for field baz",
        crawlDoc.getItemValueString("x.mappedbaz"));
  }
}
