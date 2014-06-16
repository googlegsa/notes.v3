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

import com.google.enterprise.connector.notes.NCCONST;
import com.google.enterprise.connector.notes.NotesConnectorException;
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
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotesDatabaseMock extends NotesBaseMock
    implements NotesDatabase {
  private static final String CLASS_NAME = NotesDatabaseMock.class.getName();
  private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

  private static final int NOTES_ERR_BAD_UNID = 4091;

  private NotesSessionMock session;

  private List<NotesDocumentMock> documents =
      new ArrayList<NotesDocumentMock>();
  private Map<String, List<NotesDocumentMock>> views =
      new HashMap<String, List<NotesDocumentMock>>();
  private Map<String, String[]> viewFields =
      new HashMap<String, String[]>();
  private Map<String, ViewNavFromCategoryCreator> viewNavFromCategoryCreators =
      new HashMap<String, ViewNavFromCategoryCreator>();
  private String server;
  private String name;
  private String replicaId;
  private Vector<String> aclActivityLog = new Vector<String>();
  private NotesACLMock acl;

  public NotesDatabaseMock(String server, String name) {
    this(server, name, null);
  }

  public NotesDatabaseMock(String server, String name, String replicaId) {
    this.server = server;
    this.name = name;
    this.replicaId = replicaId;
  }

  public String getServer() {
    return server;
  }

  public String getName() {
    return name;
  }

  void setSession(NotesSessionMock session) {
    this.session = session;
  }

  public List<NotesDocumentMock> getDocumentsByField(String fieldName,
      String fieldValue) {
    List<NotesDocumentMock> doclist = new ArrayList<NotesDocumentMock>();
    for (NotesDocumentMock doc : documents) {
      try {
        if (fieldValue.equalsIgnoreCase(doc.getItemValueString(fieldName))) {
          doclist.add(doc);
        }
      } catch (RepositoryException e) {
        String unid = null;
        try {
          unid = doc.getUniversalID();
        } catch (RepositoryException e2) {
          LOGGER.log(Level.WARNING, "Unable to look up document universal ID",
              e2);
        } finally {
          LOGGER.log(Level.WARNING, "Unable to look up " + fieldName
              + " field for " + fieldValue + " value in document [UNID: "
              + unid + "]", e);
        }
      }
    }
    return doclist;
  }

  public void addDocument(NotesDocumentMock document,
      String... documentViewNames) {
    documents.add(document);
    document.setDatabase(this);
    for (String documentViewName : documentViewNames) {
      List<NotesDocumentMock> view = views.get(documentViewName);
      if (null == view) {
        view = new ArrayList<NotesDocumentMock>();
        views.put(documentViewName, view);
      }
      LOGGER.fine("Adding document " + document + " to view "
          + documentViewName);
      view.add(document);
    }
  }

  void removeDocument(NotesDocumentMock document) {
    documents.remove(document);
    for (String viewName : views.keySet()) {
      List<NotesDocumentMock> docs = views.get(viewName);
      docs.remove(document);
    }
  }

  public void setViewFields(String viewName, String... fields) {
    viewFields.put(viewName, fields);
  }

  public void addViewNavFromCategoryCreator(String viewName,
      ViewNavFromCategoryCreator creator) {
    viewNavFromCategoryCreators.put(viewName, creator);
  }

  public void setACLActivityLog(String aclActivityLog) {
    this.aclActivityLog.add(aclActivityLog);
  }

  public void setACL(NotesACLMock acl) {
    this.acl = acl;
  }

  /** {@inheritDoc} */
  @Override
  public NotesView getView(String view) throws RepositoryException {
    LOGGER.fine("getting view: " + view);

    List<NotesDocumentMock> documents = views.get(view);
    if (null != documents) {
      NotesViewMock v = new NotesViewMock(view, documents);
      String[] fields = viewFields.get(view);
      if (null != fields) {
        v.setFields(fields);
      }
      v.setViewNavFromCategoryCreator(viewNavFromCategoryCreators.get(view));
      return v;
    }
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public boolean openByReplicaID(String server, String replicaId)
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "openByReplicaID");
    NotesDatabaseMock db = (NotesDatabaseMock) session.getDatabaseByReplicaId(
        server, replicaId);
    if (null == db) {
      return false;
    }
    this.documents = db.documents;
    this.views = db.views;
    this.viewFields = db.viewFields;
    this.viewNavFromCategoryCreators = db.viewNavFromCategoryCreators;
    this.server = db.server;
    this.name = db.name;
    this.replicaId = db.replicaId;
    this.aclActivityLog = db.aclActivityLog;
    this.acl = db.acl;

    return true;
  }

  /** {@inheritDoc} */
  @Override
  public NotesDocument getDocumentByUNID(final String unid)
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getDocumentByUNID");
    for (NotesDocumentMock doc: documents) {
      if (unid.equals(doc.getItemValueString(NCCONST.NCITM_UNID))) {
        return doc;
      }
    }
    throw new NotesConnectorException() {
        @Override public String getMessage() {
          return unid + " document is not found";
        }
        @Override public int getId() {
          return NOTES_ERR_BAD_UNID;
        }
    };
  }

  /** {@inheritDoc} */
  @Override
  public NotesDocument createDocument() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "createDocument");
    NotesDocumentMock document = new NotesDocumentMock();
    addDocument(document);
    return document;
  }

  /** {@inheritDoc} */
  @Override
  public String getReplicaID() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getReplicaID");
    return replicaId;
  }

  /** {@inheritDoc} */
  @Override
  public String getFilePath() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getFilePath");
    return name;
  }

  /** {@inheritDoc} */
  @Override
  public NotesDocumentCollection search(String formula)
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "search");
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public NotesDocumentCollection search(String formula,
      NotesDateTime startDate) throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "search");
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public NotesDocumentCollection search(String formula,
      NotesDateTime startDate, int maxDocs) throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "search");
    return new NotesDocumentCollectionMock(documents);
  }

  /** {@inheritDoc} */
  @Override
  public NotesACL getACL() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getACL");
    if (acl != null) {
      return acl;
    }
    return new NotesACLMock();
  }

  /** {@inheritDoc} */
  @Override
  public Vector getACLActivityLog() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getACLActivityLog");
    return aclActivityLog;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isOpen() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "isOpen");
    return true;
  }

  @Override
  public String toString() {
    try {
      return getFilePath();
    } catch (RepositoryException e) {
      return "";
    }
  }
}
