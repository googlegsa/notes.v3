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

import com.google.enterprise.connector.notes.client.NotesView;
import com.google.enterprise.connector.notes.client.NotesViewEntry;
import com.google.enterprise.connector.notes.client.NotesViewNavigator;
import com.google.enterprise.connector.notes.client.NotesDocument;

import lotus.domino.ViewEntry;
import lotus.domino.ViewNavigator;
import lotus.domino.NotesException;

class NotesViewNavigatorImpl extends NotesBaseImpl<ViewNavigator>
    implements NotesViewNavigator {

  NotesViewNavigatorImpl(ViewNavigator viewNavigator) {
    super(viewNavigator);
  }

  /** {@inheritDoc} */
  @Override
  public int getCount() throws NotesConnectorExceptionImpl {
    try {
      return getNotesObject().getCount();
    } catch (Exception e) { // Never throws NotesException
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public NotesViewEntry getFirst() throws NotesConnectorExceptionImpl {
    try {
      ViewEntry first = getNotesObject().getFirst();
      if (first == null) {
        return null;
      }
      return new NotesViewEntryImpl(first);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public NotesViewEntry getNext() throws NotesConnectorExceptionImpl {
    try {
      ViewEntry next = getNotesObject().getNext();
      if (next == null) {
        return null;
      }
      return new NotesViewEntryImpl(next);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public NotesViewEntry getNext(NotesViewEntry previousEntry)
      throws NotesConnectorExceptionImpl {
    try {
      ViewEntry next = getNotesObject().getNext(
          ((NotesViewEntryImpl) previousEntry).getNotesObject());
      if (next == null) {
        return null;
      }
      return new NotesViewEntryImpl(next);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public NotesViewEntry getFirstDocument() throws NotesConnectorExceptionImpl {
    try {
      ViewEntry first = getNotesObject().getFirstDocument();
      if (first == null) {
        return null;
      }
      return new NotesViewEntryImpl(first);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }
}
