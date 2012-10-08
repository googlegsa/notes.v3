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

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

import com.google.enterprise.connector.notes.client.NotesDocument;
import com.google.enterprise.connector.notes.client.NotesView;
import com.google.enterprise.connector.notes.client.NotesViewEntryCollection;
import com.google.enterprise.connector.notes.client.NotesViewNavigator;
import com.google.enterprise.connector.spi.RepositoryException;

public class NotesViewMock extends NotesBaseMock implements NotesView {
  private static final String CLASS_NAME = NotesViewMock.class.getName();

  /** The logger for this class. */
  private static final Logger LOGGER =
      Logger.getLogger(CLASS_NAME);

  private String viewName;
  private List<NotesDocumentMock> documents;
  private String[] fields;
  ViewNavFromCategoryCreator viewNavFromCategoryCreator;

  NotesViewMock(String viewName, List<NotesDocumentMock> documents) {
    this.viewName = viewName;
    this.documents = documents;
  }

  public void setFields(String[] fields) {
    this.fields = fields;
  }

  String[] getFields() {
    return fields;
  }

  void setViewNavFromCategoryCreator(ViewNavFromCategoryCreator creator) {
    this.viewNavFromCategoryCreator = creator;
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
    if (null == documents || documents.size() == 0) {
      return null;
    }
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
    LOGGER.entering(CLASS_NAME, "getDocumentByKey");
    return getDocumentByKey(key, false);
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
    LOGGER.entering(CLASS_NAME, "getDocumentByKey " + key);
    if (fields == null || fields.length == 0) {
      LOGGER.finest("view fields are null");
      return null;
    }
    for (NotesDocumentMock doc : documents) {
      String docValue = doc.getItemValueString(fields[0]).toLowerCase();
      LOGGER.finest("checking doc item " + fields[0]
          + " with value " + docValue);
      if (exact) {
        if (docValue.equalsIgnoreCase(key.toString())) {
          return doc;
        }
      } else {
        if (docValue.startsWith(key.toString().toLowerCase())) {
          return doc;
        }
      }
    }
    return null;
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
    if (null != viewNavFromCategoryCreator) {
      ArrayList<NotesDocumentMock> docsInCategory =
          new ArrayList<NotesDocumentMock>();
      for (NotesDocumentMock doc : documents) {
        if (viewNavFromCategoryCreator.documentIsInCategory(category, doc)) {
          docsInCategory.add(doc);
          LOGGER.finest("Adding doc " + doc + " to category " + category);
        }
      }
      return new NotesViewNavigatorMock(
          new NotesViewMock(viewName, docsInCategory));
    }
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

    LOGGER.finest("View " + viewName + " has documents: " + documents);
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesViewEntryCollection getAllEntries() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getAllEntries");
    return null;
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
