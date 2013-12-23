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
import com.google.enterprise.connector.notes.client.NotesViewEntry;
import com.google.enterprise.connector.notes.client.NotesViewNavigator;
import com.google.enterprise.connector.notes.client.NotesDocument;
import com.google.enterprise.connector.spi.RepositoryException;

import java.util.logging.Logger;

class NotesViewNavigatorMock extends NotesBaseMock
    implements NotesViewNavigator {
  private static final String CLASS_NAME =
      NotesViewNavigatorMock.class.getName();

  /** The logger for this class. */
  private static final Logger LOGGER =
      Logger.getLogger(CLASS_NAME);

  private NotesViewMock view;

  private NotesDocument currentDoc;

  NotesViewNavigatorMock(NotesViewMock view) throws RepositoryException {
    this.view = view;
    LOGGER.fine("created viewnav with view of size: " + getCount());
  }

  /** {@inheritDoc} */
  @Override
  public int getCount() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getCount");
    return view.getEntryCount();
  }

  /** {@inheritDoc} */
  @Override
  public NotesViewEntry getFirst() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getFirst");
    currentDoc = view.getFirstDocument();
    if (null == currentDoc) {
      return null;
    }
    return new NotesViewEntryMock(view, currentDoc);
  }

  /** {@inheritDoc} */
  @Override
  public NotesViewEntry getNext() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getNext");
    currentDoc = view.getNextDocument(currentDoc);
    if (null == currentDoc) {
      return null;
    }
    return new NotesViewEntryMock(view, currentDoc);
  }

  /** {@inheritDoc} */
  @Override
  public NotesViewEntry getNext(NotesViewEntry previousEntry)
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getNext(prev)");
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public NotesViewEntry getFirstDocument() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getFirstDocument");
    return getFirst();
 }
}
