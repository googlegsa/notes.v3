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

import com.google.enterprise.connector.notes.client.NotesViewEntry;
import com.google.enterprise.connector.notes.client.NotesViewEntryCollection;
import com.google.enterprise.connector.spi.RepositoryException;

import java.util.logging.Logger;

class NotesViewEntryCollectionMock extends NotesBaseMock
    implements NotesViewEntryCollection {
  private static final String CLASS_NAME =
      NotesViewEntryCollectionMock.class.getName();

  /** The logger for this class. */
  private static final Logger LOGGER =
      Logger.getLogger(CLASS_NAME);

  NotesViewEntryCollectionMock() {
  }

  /** {@inheritDoc} */
  /* @Override */
  public int getCount() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getCount");
    return -1;
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesViewEntry getEntry(Object entry) throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getEntry");
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesViewEntry getFirstEntry() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getFirstEntry");
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesViewEntry getNextEntry() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getNextEntry");
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesViewEntry getNextEntry(NotesViewEntry previousEntry)
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getNextEntry");
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesViewEntry getLastEntry() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getLastEntry");
    return null;
  }
}
