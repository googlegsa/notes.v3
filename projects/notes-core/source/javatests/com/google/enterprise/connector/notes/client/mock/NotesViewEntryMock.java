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
import com.google.enterprise.connector.notes.client.NotesItem;
import com.google.enterprise.connector.notes.client.NotesViewEntry;
import com.google.enterprise.connector.spi.RepositoryException;

import java.util.Vector;
import java.util.logging.Logger;

class NotesViewEntryMock extends NotesBaseMock
    implements NotesViewEntry {
  private static final String CLASS_NAME = NotesViewEntryMock.class.getName();

  /** The logger for this class. */
  private static final Logger LOGGER =
      Logger.getLogger(CLASS_NAME);

  NotesViewMock view;
  NotesDocument document;

  NotesViewEntryMock(NotesViewMock view, NotesDocument document) {
    this.view = view;
    this.document = document;
  }

  /** {@inheritDoc} */
  /* @Override */
  public Vector getColumnValues() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getColumnValues");
    Vector values = new Vector();
    for (String field: view.getFields()) {
      NotesItem item = document.getFirstItem(field);
      values.add(item.getValues().get(0));
    }
    return values;
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesDocument getDocument() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getDocument");
    return document;
  }
}
