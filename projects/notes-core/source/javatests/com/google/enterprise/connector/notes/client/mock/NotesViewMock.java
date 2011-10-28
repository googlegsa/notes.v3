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

import com.google.enterprise.connector.notes.client.NotesView;
import com.google.enterprise.connector.notes.client.NotesViewNavigator;
import com.google.enterprise.connector.notes.client.NotesDocument;
import com.google.enterprise.connector.spi.RepositoryException;

import java.util.Vector;
import java.util.logging.Logger;

class NotesViewMock extends NotesBaseMock implements NotesView {
  private static final String CLASS_NAME = NotesViewMock.class.getName();

  /** The logger for this class. */
  private static final Logger LOGGER =
      Logger.getLogger(CLASS_NAME);

  NotesViewMock() {
  }

  /** {@inheritDoc} */
  /* @Override */
  public int getEntryCount() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getEntryCount");
    return -1;
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesDocument getFirstDocument()
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getFirstDocument");
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesDocument getNextDocument(NotesDocument previousDocument)
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getNextDocument");
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesDocument getDocumentByKey(Object key)
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getDocumentByKey");
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
    LOGGER.entering(CLASS_NAME, "createViewNavFromCategory");
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesViewNavigator createViewNav()
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "createViewNav");
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public void refresh() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "refresh");
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
