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

import java.util.Vector;
import java.util.logging.Logger;

public class NotesDocumentMock extends NotesBaseMock
    implements NotesDocument {
  private static final String CLASS_NAME = NotesDocumentMock.class.getName();

  /** The logger for this class. */
  private static final Logger LOGGER =
      Logger.getLogger(CLASS_NAME);

  public NotesDocumentMock() {
  }

  /** {@inheritDoc} */
  /* @Override */
  public boolean hasItem(String name) throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "hasItem");
    return false;
  }

  /** {@inheritDoc} */
  /* @Override */
  public String getItemValueString(String name) throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getItemValueString");
    return null;
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
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesItem getFirstItem(String name) throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getFirstItem");
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public Vector getItems() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getItems");
    return null;
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
    LOGGER.entering(CLASS_NAME, "replaceItemValue");
    return null;
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
}
