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

import com.google.enterprise.connector.notes.client.NotesDocument;
import com.google.enterprise.connector.notes.client.NotesView;
import com.google.enterprise.connector.notes.client.NotesViewEntryCollection;
import com.google.enterprise.connector.notes.client.NotesViewNavigator;
import com.google.enterprise.connector.spi.RepositoryException;

import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

class NotesViewMock extends NotesBaseMock implements NotesView {
  private static final String CLASS_NAME = NotesViewMock.class.getName();

  /** The logger for this class. */
  private static final Logger LOGGER =
      Logger.getLogger(CLASS_NAME);

  private List<NotesDocumentMock> documents;

  private String[] fields;

  NotesViewMock(List<NotesDocumentMock> documents) {
    this.documents = documents;
  }

  void setFields(String[] fields) {
    this.fields = fields;
  }

  String[] getFields() {
    return fields;
  }

  /** {@inheritDoc} */
  /* @Override */
  public int getEntryCount() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getEntryCount");
    return documents.size();
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesDocument getFirstDocument()
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getFirstDocument");
    return documents.get(0);
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesDocument getNextDocument(NotesDocument previousDocument)
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getNextDocument");
    NotesDocumentMock prev = (NotesDocumentMock) previousDocument;
    for (int i = 0; i < documents.size(); i++) {
      if (documents.get(i).equals(prev)) {
        if (i < documents.size() - 1) {
          return documents.get(i + 1);
        }
      }
    }
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  /* For tests, assume that the key field is the first configured
   * field in the view. */
  public NotesDocument getDocumentByKey(Object key)
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getDocumentByKey " + key);
    if (fields == null || fields.length == 0) {
      LOGGER.finest("view fields are null");
      return null;
    }
    for (NotesDocumentMock doc : documents) {
      LOGGER.finest("checking doc item " + fields[0] +
          " with value " + doc.getItemValueString(fields[0]));
      if (key.equals(doc.getItemValueString(fields[0]))) {
        return doc;
      }
    }
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesDocument getDocumentByKey(Vector key)
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getDocumentByKey(Vector)");
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesDocument getDocumentByKey(Object key, boolean exact)
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getDocumentByKey");
    return getDocumentByKey(key);
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesDocument getDocumentByKey(Vector key, boolean exact)
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getDocumentByKey(Vector)");
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesViewNavigator createViewNavFromCategory(String category)
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "createViewNavFromCategory");
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesViewNavigator createViewNav()
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "createViewNav");
    return new NotesViewNavigatorMock(this);
  }

  /** {@inheritDoc} */
  /* @Override */
  public void refresh() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "refresh");
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesViewEntryCollection getAllEntriesByKey(Vector keys)
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getAllEntriesByKey");
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesViewEntryCollection getAllEntriesByKey(Object key)
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getAllEntriesByKey");
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesViewEntryCollection getAllEntriesByKey(Vector keys, boolean exact)
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getAllEntriesByKey");
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesViewEntryCollection getAllEntriesByKey(Object key, boolean exact)
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getAllEntriesByKey");
    return null;
  }

  /* TODO: implement getName
  public String toString() {
    try {
      return getName();
    } catch (RepositoryException e) {
      return "";
    }
  }
  */
}
