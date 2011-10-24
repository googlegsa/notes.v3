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

import com.google.enterprise.connector.notes.client.NotesACL;
import com.google.enterprise.connector.notes.client.NotesDatabase;
import com.google.enterprise.connector.notes.client.NotesDateTime;
import com.google.enterprise.connector.notes.client.NotesDocument;
import com.google.enterprise.connector.notes.client.NotesDocumentCollection;
import com.google.enterprise.connector.notes.client.NotesView;
import com.google.enterprise.connector.spi.RepositoryException;

import java.util.Vector;
import java.util.logging.Logger;

class NotesDatabaseMock extends NotesBaseMock
    implements NotesDatabase {
  private static final String CLASS_NAME = NotesDatabaseMock.class.getName();

  /** The logger for this class. */
  private static final Logger LOGGER =
      Logger.getLogger(CLASS_NAME);

  NotesDatabaseMock() {
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesView getView(String view) throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getView");
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public boolean openByReplicaID(String server, String replicaId)
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "openByReplicaID");
    return true;
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesDocument getDocumentByUNID(String unid)
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getDocumentByUNID");
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesDocument createDocument() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "createDocument");
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public String getReplicaID() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getReplicaID");
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public String getFilePath() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getFilePath");
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesDocumentCollection search(String formula)
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "search");
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesDocumentCollection search(String formula,
      NotesDateTime startDate) throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "search");
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesDocumentCollection search(String formula,
      NotesDateTime startDate, int maxDocs) throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "search");
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesACL getACL() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getACL");
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public Vector getACLActivityLog() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getACLActivityLog");
    return null;
  }

  public String toString() {
    try {
      return getFilePath();
    } catch (RepositoryException e) {
      return "";
    }
  }
}
