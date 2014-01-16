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

package com.google.enterprise.connector.notes.client.notes;

import com.google.enterprise.connector.notes.client.NotesACL;
import com.google.enterprise.connector.notes.client.NotesDatabase;
import com.google.enterprise.connector.notes.client.NotesDateTime;
import com.google.enterprise.connector.notes.client.NotesDocument;
import com.google.enterprise.connector.notes.client.NotesDocumentCollection;
import com.google.enterprise.connector.notes.client.NotesView;

import lotus.domino.Database;
import lotus.domino.DateTime;
import lotus.domino.NotesException;
import lotus.domino.View;

import java.util.Vector;

class NotesDatabaseImpl extends NotesBaseImpl<Database>
    implements NotesDatabase {

  NotesDatabaseImpl(Database database) {
    super(database);
  }

  /** {@inheritDoc} */
  @Override
  public NotesView getView(String view) throws NotesConnectorExceptionImpl {
    try {
      View viewObj = getNotesObject().getView(view);
      if (viewObj == null) {
        return null;
      }
      return new NotesViewImpl(viewObj);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public boolean openByReplicaID(String server, String replicaId)
      throws NotesConnectorExceptionImpl {
    try {
      return getNotesObject().openByReplicaID(server, replicaId);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  /* Throws an exception if the UNID isn't found. */
  @Override
  public NotesDocument getDocumentByUNID(String unid)
      throws NotesConnectorExceptionImpl {
    try {
      return new NotesDocumentImpl(getNotesObject().getDocumentByUNID(unid));
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public NotesDocument createDocument() throws NotesConnectorExceptionImpl {
    try {
      return new NotesDocumentImpl(getNotesObject().createDocument());
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public String getReplicaID() throws NotesConnectorExceptionImpl {
    try {
      return getNotesObject().getReplicaID();
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public String getFilePath() throws NotesConnectorExceptionImpl {
    try {
      return getNotesObject().getFilePath();
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public NotesDocumentCollection search(String formula)
      throws NotesConnectorExceptionImpl {
    try {
      return new NotesDocumentCollectionImpl(
          getNotesObject().search(formula));
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public NotesDocumentCollection search(String formula, NotesDateTime startDate)
      throws NotesConnectorExceptionImpl {
    try {
      return new NotesDocumentCollectionImpl(
          getNotesObject().search(formula,
              ((NotesDateTimeImpl) startDate).getNotesObject()));
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public NotesDocumentCollection search(String formula, NotesDateTime startDate,
      int maxDocs) throws NotesConnectorExceptionImpl {
    try {
      DateTime notesStartDate = null;
      if (startDate != null) {
        notesStartDate = ((NotesDateTimeImpl) startDate).getNotesObject();
      }

      return new NotesDocumentCollectionImpl(
          getNotesObject().search(formula, notesStartDate, maxDocs));
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public NotesACL getACL() throws NotesConnectorExceptionImpl {
    try {
      return new NotesACLImpl(getNotesObject().getACL());
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public Vector getACLActivityLog() throws NotesConnectorExceptionImpl {
    try {
      return getNotesObject().getACLActivityLog();
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public boolean isOpen() throws NotesConnectorExceptionImpl {
    try {
      return getNotesObject().isOpen();
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }
}
