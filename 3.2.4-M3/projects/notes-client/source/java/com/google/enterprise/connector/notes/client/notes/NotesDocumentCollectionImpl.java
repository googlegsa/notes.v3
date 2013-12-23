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

import com.google.enterprise.connector.notes.client.NotesDocument;
import com.google.enterprise.connector.notes.client.NotesDocumentCollection;
import com.google.enterprise.connector.notes.client.NotesEmbeddedObject;
import com.google.enterprise.connector.notes.client.NotesItem;
import com.google.enterprise.connector.notes.client.NotesView;

import lotus.domino.Document;
import lotus.domino.DocumentCollection;
import lotus.domino.NotesException;

import java.util.Vector;

class NotesDocumentCollectionImpl extends NotesBaseImpl<DocumentCollection>
    implements NotesDocumentCollection {

  NotesDocumentCollectionImpl(DocumentCollection documentCollection) {
    super(documentCollection);
  }

  /** {@inheritDoc} */
  @Override
  public NotesDocument getFirstDocument() throws NotesConnectorExceptionImpl {
    try {
      Document doc = getNotesObject().getFirstDocument();
      if (doc == null) {
        return null;
      }
      return new NotesDocumentImpl(doc);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public NotesDocument getNextDocument() throws NotesConnectorExceptionImpl {
    try {
      Document doc = getNotesObject().getNextDocument();
      if (doc == null) {
        return null;
      }
      return new NotesDocumentImpl(doc);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public NotesDocument getNextDocument(NotesDocument document)
      throws NotesConnectorExceptionImpl {
    try {
      Document doc = getNotesObject().getNextDocument(
          ((NotesDocumentImpl) document).getNotesObject());
      if (doc == null) {
        return null;
      }
      return new NotesDocumentImpl(doc);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public int getCount() throws NotesConnectorExceptionImpl {
    try {
      return getNotesObject().getCount();
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }
}
