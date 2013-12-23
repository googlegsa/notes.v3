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

import com.google.enterprise.connector.notes.client.NotesViewEntry;
import com.google.enterprise.connector.notes.client.NotesViewEntryCollection;
import com.google.enterprise.connector.spi.RepositoryException;

import lotus.domino.NotesException;
import lotus.domino.ViewEntry;
import lotus.domino.ViewEntryCollection;

import java.util.logging.Logger;

class NotesViewEntryCollectionImpl extends NotesBaseImpl<ViewEntryCollection>
    implements NotesViewEntryCollection {

  NotesViewEntryCollectionImpl(ViewEntryCollection viewEntryCollection) {
    super(viewEntryCollection);
  }

  /** {@inheritDoc} */
  @Override
  public int getCount() throws RepositoryException {
    try {
      return getNotesObject().getCount();
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public NotesViewEntry getEntry(Object entry) throws RepositoryException {
    try {
      ViewEntry foundEntry = getNotesObject().getEntry(
          TypeConverter.toNotesItemValue(entry));
      if (foundEntry == null) {
        return null;
      }
      return new NotesViewEntryImpl(foundEntry);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public NotesViewEntry getFirstEntry() throws RepositoryException {
    try {
      ViewEntry foundEntry = getNotesObject().getFirstEntry();
      if (foundEntry == null) {
        return null;
      }
      return new NotesViewEntryImpl(foundEntry);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public NotesViewEntry getNextEntry() throws RepositoryException {
    try {
      ViewEntry foundEntry = getNotesObject().getNextEntry();
      if (foundEntry == null) {
        return null;
      }
      return new NotesViewEntryImpl(foundEntry);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public NotesViewEntry getNextEntry(NotesViewEntry previousEntry)
      throws RepositoryException {
    try {
      ViewEntry foundEntry = getNotesObject().getEntry(
          TypeConverter.toNotesItemValue(previousEntry));
      if (foundEntry == null) {
        return null;
      }
      return new NotesViewEntryImpl(foundEntry);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public NotesViewEntry getLastEntry() throws RepositoryException {
    try {
      ViewEntry foundEntry = getNotesObject().getLastEntry();
      if (foundEntry == null) {
        return null;
      }
      return new NotesViewEntryImpl(foundEntry);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }
}
