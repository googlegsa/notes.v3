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

import com.google.enterprise.connector.notes.client.NotesItem;
import com.google.enterprise.connector.spi.RepositoryException;

import java.util.Collections;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;

class NotesItemMock extends NotesBaseMock implements NotesItem {
  private static final String CLASS_NAME = NotesItemMock.class.getName();

  /** The logger for this class. */
  private static final Logger LOGGER =
      Logger.getLogger(CLASS_NAME);

  private Map<String, Object> properties;

  NotesItemMock() {
    this.properties = Collections.emptyMap();
  }

  NotesItemMock(Map<String, Object> properties) {
    this.properties = properties;
  }

  /** {@inheritDoc} */
  /* @Override */
  public String getName() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getName");
    return (String) properties.get("name");
  }

  /** {@inheritDoc} */
  /* @Override */
  public int getType() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getType");
    Object type = properties.get("type");
    if (null == type) {
      return -1;
    }
    return (Integer) type;
  }

  /** {@inheritDoc} */
  /* @Override */
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
  /* @Override */
  public boolean isReaders() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "isReaders");
    return false;
  }

  /** {@inheritDoc} */
  /* @Override */
  public boolean isAuthors() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "isAuthors");
    return false;
  }

  /** {@inheritDoc} */
  /* @Override */
  public Vector getValues() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getValues");
    return (Vector) properties.get("values");
  }

  /** {@inheritDoc} */
  /* @Override */
  public void appendToTextList(String value) throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "appendToTextList");
  }

  /** {@inheritDoc} */
  /* @Override */
  public void appendToTextList(Vector values) throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "appendToTextList");
  }

  /** {@inheritDoc} */
  /* @Override */
  public void setSummary(boolean summary) throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "setSummary");
  }

  public String toString() {
    try {
      return getName();
    } catch (RepositoryException e) {
      return "";
    }
  }
}
