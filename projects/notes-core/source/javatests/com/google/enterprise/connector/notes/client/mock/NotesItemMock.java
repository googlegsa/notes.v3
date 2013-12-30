// Copyright 2011 Google Inc.
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

package com.google.enterprise.connector.notes.client.mock;

import com.google.enterprise.connector.notes.client.NotesDateTime;
import com.google.enterprise.connector.notes.client.NotesItem;
import com.google.enterprise.connector.spi.RepositoryException;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;

public class NotesItemMock extends NotesBaseMock implements NotesItem {
  private static final String CLASS_NAME = NotesItemMock.class.getName();

  /** The logger for this class. */
  private static final Logger LOGGER =
      Logger.getLogger(CLASS_NAME);

  private Map<String, Object> properties = new HashMap<String, Object>();
  private boolean isReaders = false;
  private boolean isAuthors = false;

  public NotesItemMock(Object... args) {
    int valuesIndex = -1;
    for (int i = 0; i < args.length; i = i + 2) {
      String name = args[i].toString();
      if ("values".equals(name)) {
        valuesIndex = i + 1;
        break;
      }
      properties.put(name, args[i + 1]);
    }
    if (valuesIndex != -1 && valuesIndex < args.length) {
      Vector<Object> values = new Vector<Object>();
      for (int i = valuesIndex; i < args.length; i++) {
        if (null == args[i]) {
          continue;
        }
        if (args[i] instanceof Vector) {
          for (Object o : (Vector) args[i]) {
            values.add(o);
          }
        } else if (args[i] instanceof Object[]) {
          for (Object o : (Object[]) args[i]) {
            values.add(o);
          }
        } else {
          values.add(args[i]);
        }
      }
      properties.put("values", values);
      LOGGER.finest("NotesItemMock<init> values: " + properties.get("values"));
    }
  }

  /** {@inheritDoc} */
  @Override
  @SuppressWarnings("unchecked")
  public NotesDateTime getDateTimeValue() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getDateTimeValue");
    Vector<Object> values = (Vector<Object>)properties.get("values");
    for (Object o : values) {
      if (o instanceof NotesDateTime) {
        return (NotesDateTime)o;
      }
    }
    return null;
  }
  
  /** {@inheritDoc} */
  @Override
  public String getName() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getName");
    return (String) properties.get("name");
  }

  /** {@inheritDoc} */
  @Override
  public int getType() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getType");
    Object type = properties.get("type");
    if (null == type) {
      return -1;
    }
    return (Integer) type;
  }

  /** {@inheritDoc} */
  @Override
  public String getText(int maxlen) throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getText");
    Vector values = getValues();
    if (values == null) {
      return null;
    }
    StringBuilder builder = new StringBuilder();
    for (Object o : values) {
      builder.append(o.toString()).append(";");
    }
    builder.deleteCharAt(builder.length() -1);
    return builder.toString();
  }

  /** {@inheritDoc} */
  @Override
  public boolean isReaders() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "isReaders");
    return isReaders;
  }

  public void setReaders(boolean readers) {
    isReaders = readers;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isAuthors() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "isAuthors");
    return isAuthors;
  }

  public void setAuthors(boolean authors) {
    isAuthors = authors;
  }

  /** {@inheritDoc} */
  @Override
  public Vector getValues() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getValues");
    return (Vector) properties.get("values");
  }

  /** {@inheritDoc} */
  @Override
  public void appendToTextList(String value) throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "appendToTextList");
    @SuppressWarnings("unchecked")
        Vector<String> values = (Vector<String>) getValues();
    if (values == null) {
      Vector<String> v = new Vector<String>();
      v.add(value);
      properties.put("values", v);
    } else {
      values.add(value);
    }
  }

  /** {@inheritDoc} */
  @Override
  @SuppressWarnings("unchecked")
  public void appendToTextList(Vector values) throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "appendToTextList");
    Vector currentValues = getValues();
    if (currentValues == null) {
      Vector v = new Vector();
      v.addAll(values);
      properties.put("values", v);
    } else {
      currentValues.addAll(values);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void setSummary(boolean summary) throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "setSummary");
  }

  public String toString() {
    try {
      return getName() + " = " + getText(256);
    } catch (RepositoryException e) {
      return "";
    }
  }
}
