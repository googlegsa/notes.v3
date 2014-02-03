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
import com.google.enterprise.connector.notes.client.NotesDocumentCollection;
import com.google.enterprise.connector.spi.RepositoryException;

import java.util.List;
import java.util.logging.Logger;

public class NotesDocumentCollectionMock extends NotesBaseMock
    implements NotesDocumentCollection {
  private static final String CLASS_NAME =
      NotesDocumentCollectionMock.class.getName();

  /** The logger for this class. */
  private static final Logger LOGGER =
      Logger.getLogger(CLASS_NAME);

  private List<NotesDocumentMock> documents;

  private int lastReturned = -1;

  public NotesDocumentCollectionMock(List<NotesDocumentMock> documents) {
    this.documents = documents;
  }

  /** {@inheritDoc} */
  @Override
  public NotesDocument getFirstDocument() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getFirstDocument");
    if (documents.size() == 0) {
      return null;
    }
    lastReturned = 0;
    return documents.get(0);
  }

  /** {@inheritDoc} */
  @Override
  public NotesDocument getNextDocument() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getNextDocument");
    if (lastReturned + 1 < documents.size()) {
      lastReturned++;
      return documents.get(lastReturned);
    }
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public NotesDocument getNextDocument(NotesDocument document)
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getNextDocument");
    int position = -1;
    for (int i = 0; i < documents.size(); i++) {
      if (document == documents.get(i)) {
        position = i;
        break;
      }
    }
    if (position != -1) {
      if (position + 1 < documents.size()) {
        lastReturned = position + 1;
        return documents.get(position + 1);
      }
    }
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public int getCount() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getCount");
    return documents.size();
  }
}
