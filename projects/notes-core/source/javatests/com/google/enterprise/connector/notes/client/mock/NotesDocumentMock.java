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

package com.google.enterprise.connector.notes.client.mock;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.enterprise.connector.notes.NCCONST;
import com.google.enterprise.connector.notes.client.NotesDateTime;
import com.google.enterprise.connector.notes.client.NotesDocument;
import com.google.enterprise.connector.notes.client.NotesDocumentCollection;
import com.google.enterprise.connector.notes.client.NotesEmbeddedObject;
import com.google.enterprise.connector.notes.client.NotesItem;
import com.google.enterprise.connector.notes.client.NotesRichTextItem;
import com.google.enterprise.connector.spi.RepositoryException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

public class NotesDocumentMock extends NotesBaseMock
    implements NotesDocument {
  private static final String CLASS_NAME = NotesDocumentMock.class.getName();

  /** The logger for this class. */
  private static final Logger LOGGER =
      Logger.getLogger(CLASS_NAME);

  private NotesDatabaseMock database;

  private final Multimap<String, NotesItemMock> items =
      ArrayListMultimap.create();

  private final List<NotesDocumentMock> responses =
      new ArrayList<NotesDocumentMock>();

  private NotesDateTime lastModified;

  /* The constructor's currently public for testing. At some
   * point, we might be able to build a more thorough test data
   * framework and remove the need for tests to construct mock
   * objects explicitly.
   */
  public NotesDocumentMock() {
    this.lastModified = new NotesDateTimeMock(new Date());
  }

  public void setDatabase(NotesDatabaseMock database) {
    this.database = database;
  }

  public void addItem(NotesItemMock item) throws RepositoryException {
    this.items.put(item.getName().toLowerCase(), item);
  }

  public void addResponse(NotesDocumentMock response) {
    this.responses.add(response);
  }

  @Override
  public boolean isDeleted() throws RepositoryException {
    return false;
  }

  @Override
  public boolean isValid() throws RepositoryException {
    return true;
  }

  /** {@inheritDoc} */
  @Override
  public boolean hasItem(String name) throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "hasItem");
    return items.containsKey(name.toLowerCase());
  }

  /* Helper method to return the first value of the multi map */
  private NotesItemMock getFirst(String name) {
    Collection<NotesItemMock> values = items.get(name.toLowerCase());
    if (values.isEmpty()) {
      return null;
    } else {
      return values.iterator().next();
    }
  }

  /** {@inheritDoc} */
  @Override
  public String getItemValueString(String name) throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getItemValueString");
    NotesItemMock item = getFirst(name);
    if (item == null) {
      return "";
    }
    Vector values = item.getValues();
    if (values == null || values.size() == 0) {
      return "";
    }
    switch (item.getType()) {
      case NotesItem.NUMBERS:
      case NotesItem.DATETIMES:
        return "";
      case NotesItem.RICHTEXT:
        // Artificial limit, but ok for testing for now. TODO:
        // implement Item.getText().
        return item.getText(10 * 1024);
      default:
        return values.get(0).toString();
    }
  }

  /** {@inheritDoc} */
  @Override
  public int getItemValueInteger(String name) throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getItemValueInteger");
    NotesItemMock item = getFirst(name);
    if (item == null) {
      return 0;
    }
    Vector values = item.getValues();
    if (values == null) {
      return 0;
    }
    switch (item.getType()) {
      case NotesItem.NUMBERS:
        return new Double(values.get(0).toString()).intValue();
      default:
        return 0;
    }
  }

  /** {@inheritDoc} */
  @Override
  public Vector getItemValue(String name) throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getItemValue");
    NotesItemMock item = getFirst(name);
    if (item == null) {
      return new Vector();
    }
    Vector values = item.getValues();
    if (values == null) {
      return new Vector();
    }
    return values;
  }

  /** {@inheritDoc} */
  @Override
  public NotesItem getFirstItem(String name) throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getFirstItem");
    return getFirst(name);
  }

  /** {@inheritDoc} */
  @Override
  public Vector getItems() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getItems");
    return new Vector<Object>(items.values());
  }

  /** {@inheritDoc} */
  @Override
  public Vector getItemValueDateTimeArray(String name)
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getItemValueDateTimeArray");
    NotesItemMock item = getFirst(name);
    if (item == null) {
      return new Vector(); // TODO: check that this is right.
    }
    Vector values = item.getValues();
    if (values == null) {
      return new Vector();
    }
    if (values.size() == 0) {
      return values;
    }
    Date date = (Date) values.get(0);
    Vector<NotesDateTimeMock> dateValues = new Vector<NotesDateTimeMock>();
    dateValues.add(new NotesDateTimeMock(date));
    return dateValues;
  }

  /** {@inheritDoc} */
  @Override
  public void removeItem(String name) throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "removeItem");
    items.removeAll(name.toLowerCase());
  }

  /** {@inheritDoc} */
  @Override
  public NotesItem replaceItemValue(String name, Object value)
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "replaceItemValue " + name);
    NotesItemMock item;
    if (value instanceof NotesItemMock) {
      item = (NotesItemMock) value;
    } else if (value instanceof Vector) {
      Vector values = (Vector) value;
      if (values.size() == 0) {
        item = new NotesItemMock("name", name.toLowerCase(),
            "type", NotesItem.TEXT);
      } else {
        item = new NotesItemMock("name", name.toLowerCase(),
            "type", getNotesType(values.get(0)), "values", values);
      }
    } else {
      item = new NotesItemMock("name", name.toLowerCase(),
          "type", getNotesType(value), "values", value);
    }
    items.removeAll(name.toLowerCase());
    items.put(name.toLowerCase(), item);
    return item;
  }

  /** {@inheritDoc} */
  @Override
  public NotesItem appendItemValue(String name, Object value)
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "appendItemValue");
    NotesItemMock item = getFirst(name);
    if (item == null) {
      return replaceItemValue(name, value);
    }
    // TODO(jlacey): This is not adding new items of the same name,
    // but adding multiple values to a single item. The code is unused.
    if (value instanceof Vector) {
      item.appendToTextList((Vector) value);
    } else {
      item.appendToTextList(value.toString());
    }
    return item;
  }

  /** {@inheritDoc} */
  @Override
  public boolean save() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "save");
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public boolean save(boolean force) throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "save");
    return false;
  }

  /** {@inheritDoc} */
  @Override
  public boolean remove(boolean force) throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "remove");
    database.removeDocument(this);
    return true;
  }

  /** {@inheritDoc} */
  @Override
  public NotesDocumentCollection getResponses() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getResponses");
    return new NotesDocumentCollectionMock(responses);
  }

  /** {@inheritDoc} */
  @Override
  public NotesEmbeddedObject getAttachment(String filename)
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getAttachment");
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public String getNotesURL() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getNotesURL");
    NotesItemMock item = getFirst(NCCONST.ITM_DOCID);
    return (item != null) ? item.toString() : null;
  }

  /** {@inheritDoc} */
  @Override
  public String getUniversalID() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getUniversalID");
    return getItemValueString(NCCONST.NCITM_UNID);
  }

  /** {@inheritDoc} */
  @Override
  public void copyAllItems(NotesDocument doc, boolean replace)
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "copyAllItems");
  }

  /** {@inheritDoc} */
  @Override
  public NotesRichTextItem createRichTextItem(String name)
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "createRichTextItem");
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public NotesDateTime getCreated() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getCreated");
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public NotesDateTime getLastModified() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getLastModified");
    return lastModified;
  }

  public void setLastModified(NotesDateTime lastModified) {
    this.lastModified = lastModified;
    items.removeAll(NCCONST.ITM_LASTMODIFIED.toLowerCase());
    items.put(NCCONST.ITM_LASTMODIFIED.toLowerCase(),
        new NotesItemMock("name", NCCONST.ITM_LASTMODIFIED, "type",
            NotesItem.DATETIMES, "values", lastModified));
  }

  /** {@inheritDoc} */
  @Override
  public Vector getAuthors() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getAuthors");
    return null;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    try {
      for (NotesItemMock item : items.values()) {
        buf.append(item.toString()).append("\n");
      }
      return buf.toString();
      // In the Notes API, this method returns getUniversalID,
      // but for testing purposes it's more interesting to see
      // the items comprising this document.
      //return getUniversalID();
    } catch (Exception e) {
      return "";
    }
  }

  private int getNotesType(Object value) {
    int type = -1;
    if (value instanceof String) {
      type = NotesItem.TEXT;
    } else if (value instanceof Integer) {
      type = NotesItem.NUMBERS;
    } else if (value instanceof Double) {
      type = NotesItem.NUMBERS;
    } else if (value instanceof NotesDateTime) {
      type = NotesItem.DATETIMES;
    }
    return type;
  }
}
