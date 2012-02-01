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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;

public class NotesDatabaseMock extends NotesBaseMock
    implements NotesDatabase {
  private static final String CLASS_NAME = NotesDatabaseMock.class.getName();

  /** The logger for this class. */
  private static final Logger LOGGER =
      Logger.getLogger(CLASS_NAME);

  private List<NotesDocumentMock> documents =
      new ArrayList<NotesDocumentMock>();
  private Map<String, List<NotesDocumentMock>> views =
      new HashMap<String, List<NotesDocumentMock>>();
  private Map<String, String[]> viewFields =
      new HashMap<String, String[]>();
  private String server;
  private String name;

  public NotesDatabaseMock(String server, String name) {
    this.server = server;
    this.name = name;
  }

  public String getServer() {
    return server;
  }

  public String getName() {
    return name;
  }

  public void addDocument(NotesDocumentMock document,
      String... documentViewNames) {
    documents.add(document);
    for (String documentViewName : documentViewNames) {
      List<NotesDocumentMock> view = views.get(documentViewName);
      if (null == view) {
        view = new ArrayList<NotesDocumentMock>();
        views.put(documentViewName, view);
      }
      view.add(document);
    }
  }

  public void setViewFields(String viewName, String... fields) {
    viewFields.put(viewName, fields);
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesView getView(String view) throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getView");
    List<NotesDocumentMock> documents = views.get(view);
    if (null != documents) {
      NotesViewMock v = new NotesViewMock(documents);
      String[] fields = viewFields.get(view);
      if (null != fields) {
        v.setFields(fields);
      }
      return v;
    }
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

  /** {@inheritDoc} */
  /* @Override */
  public boolean isOpen() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "isOpen");
    return false;
  }

  public String toString() {
    try {
      return getFilePath();
    } catch (RepositoryException e) {
      return "";
    }
  }
}
