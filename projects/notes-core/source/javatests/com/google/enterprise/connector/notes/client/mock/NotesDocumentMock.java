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

import com.google.enterprise.connector.notes.client.NotesDateTime;
import com.google.enterprise.connector.notes.client.NotesDocument;
import com.google.enterprise.connector.notes.client.NotesDocumentCollection;
import com.google.enterprise.connector.notes.client.NotesEmbeddedObject;
import com.google.enterprise.connector.notes.client.NotesItem;
import com.google.enterprise.connector.notes.client.NotesRichTextItem;
import com.google.enterprise.connector.notes.client.NotesView;
import com.google.enterprise.connector.spi.RepositoryException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;

public class NotesDocumentMock extends NotesBaseMock
    implements NotesDocument {
  private static final String CLASS_NAME = NotesDocumentMock.class.getName();

  /** The logger for this class. */
  private static final Logger LOGGER =
      Logger.getLogger(CLASS_NAME);

  private Map<String, NotesItemMock> items =
      new HashMap<String, NotesItemMock>();

  /* The constructor's currently public for testing. At some
   * point, we might be able to build a more thorough test data
   * framework and remove the need for tests to construct mock
   * objects explicitly.
   */
  public NotesDocumentMock() {
  }

  /* Helper for tests to use to construct a document. Use
   * arguments in the form (attrname, value, attrname,
   * value, ..., "values", <one or more values>).
   */
  public void addItem(Object ... args) throws RepositoryException {
    Map<String, Object> data = new HashMap<String, Object>();
    int valuesIndex = -1;
    for (int i = 0; i < args.length; i = i + 2) {
      String name = args[i].toString();
      if ("values".equals(name)) {
        valuesIndex = i + 1;
        break;
      }
      data.put(name, args[i + 1]);
      LOGGER.finest("item attr: " + name + " = " + args[i + 1]);
    }
    if (valuesIndex != -1 && valuesIndex < args.length) {
      data.put("values", new Vector<Object>(
          Arrays.asList(args).subList(valuesIndex, args.length)));
      LOGGER.finest("values: " + data.get("values"));
    }
    NotesItemMock item = new NotesItemMock(data);
    if (null == item.getName()) {
      throw new RuntimeException("Missing name in item");
    }
    this.items.put(item.getName().toLowerCase(), item);
  }

  /** {@inheritDoc} */
  /* @Override */
  public boolean hasItem(String name) throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "hasItem");
    return items.containsKey(name.toLowerCase());
  }

  /** {@inheritDoc} */
  /* @Override */
  public String getItemValueString(String name) throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getItemValueString");
    NotesItemMock item = items.get(name.toLowerCase());
    if (null == item) {
      return "";
    }
    Vector values = item.getValues();
    if (null == values) {
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
  /* @Override */
  public int getItemValueInteger(String name) throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getItemValueInteger");
    return -1;
  }

  /** {@inheritDoc} */
  /* @Override */
  public Vector getItemValue(String name) throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getItemValue");
    NotesItemMock item = items.get(name.toLowerCase());
    if (null == item) {
      return new Vector();
    }
    Vector values = item.getValues();
    if (null == values) {
      return new Vector();
    }
    return values;
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesItem getFirstItem(String name) throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getFirstItem");
    return items.get(name.toLowerCase());
  }

  /** {@inheritDoc} */
  /* @Override */
  public Vector getItems() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getItems");
    return new Vector<Object>(items.values());
  }

  /** {@inheritDoc} */
  /* @Override */
  public Vector getItemValueDateTimeArray(String name)
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getItemValueDateTimeArray");
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public void removeItem(String name) throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "removeItem");
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesItem replaceItemValue(String name, Object value)
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "replaceItemValue " + name);
    NotesItemMock item = null;
    if (value instanceof NotesItemMock) {
      item = (NotesItemMock) value;
      items.put(name.toLowerCase(), item);
    } else {
      Map<String, Object> data = new HashMap<String, Object>();
      data.put("name", name.toLowerCase());
      Vector<Object> values = new Vector<Object>();
      values.add(value);
      if (value instanceof String) {
        data.put("type", new Integer(NotesItem.TEXT));
      } else if (value instanceof Integer) {
        data.put("type", new Integer(NotesItem.NUMBERS));
      } else if (value instanceof Double) {
        data.put("type", new Integer(NotesItem.NUMBERS));
      } else if (value instanceof NotesDateTime) {
        data.put("type", new Integer(NotesItem.DATETIMES));
      }
      // TODO: handle the rest of the possible data types
      data.put("values", values);
      item = new NotesItemMock(data);
      items.put(name.toLowerCase(), item);
    }
    return item;
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesItem appendItemValue(String name, Object value)
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "appendItemValue");
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public boolean save() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "save");
    return false;
  }

  /** {@inheritDoc} */
  /* @Override */
  public boolean save(boolean force) throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "save");
    return false;
  }

  /** {@inheritDoc} */
  /* @Override */
  public boolean remove(boolean force) throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "remove");
    return false;
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesDocumentCollection getResponses() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getResponses");
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesEmbeddedObject getAttachment(String filename)
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getAttachment");
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public String getNotesURL() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getNotesURL");
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public String getUniversalID() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getUniversalID");
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public void copyAllItems(NotesDocument doc, boolean replace)
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "copyAllItems");
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesRichTextItem createRichTextItem(String name)
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "createRichTextItem");
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesDateTime getCreated() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getCreated");
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesDateTime getLastModified() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getLastModified");
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public Vector getAuthors() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getAuthors");
    return null;
  }

  public String toString() {
    try {
      return getUniversalID();
    } catch (RepositoryException e) {
      return "";
    }
  }
}
