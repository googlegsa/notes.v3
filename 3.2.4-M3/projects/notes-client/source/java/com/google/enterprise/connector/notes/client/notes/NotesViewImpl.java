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
import com.google.enterprise.connector.notes.client.NotesView;
import com.google.enterprise.connector.notes.client.NotesViewEntryCollection;
import com.google.enterprise.connector.notes.client.NotesViewNavigator;
import com.google.enterprise.connector.spi.RepositoryException;

import lotus.domino.Document;
import lotus.domino.NotesException;
import lotus.domino.View;
import lotus.domino.ViewEntryCollection;

import java.util.Vector;

class NotesViewImpl extends NotesBaseImpl<View> implements NotesView {

  NotesViewImpl(View view) {
    super(view);
  }

  /** {@inheritDoc} */
  @Override
  public int getEntryCount() throws NotesConnectorExceptionImpl {
    try {
      return getNotesObject().getEntryCount();
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public NotesDocument getFirstDocument()
      throws NotesConnectorExceptionImpl {
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
  public NotesDocument getNextDocument(NotesDocument previousDocument)
      throws NotesConnectorExceptionImpl {
    try {
      Document doc = getNotesObject().getNextDocument(
          ((NotesDocumentImpl) previousDocument).getNotesObject());
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
  public NotesDocument getDocumentByKey(Object key)
      throws NotesConnectorExceptionImpl {
    try {
      Document doc = getNotesObject().getDocumentByKey(key);
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
  public NotesDocument getDocumentByKey(Vector key)
      throws NotesConnectorExceptionImpl {
    try {
      Document doc = getNotesObject().getDocumentByKey(
          (Vector) TypeConverter.toNotesItemValue(key));
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
  public NotesDocument getDocumentByKey(Object key, boolean exact)
      throws NotesConnectorExceptionImpl {
    try {
      Document doc = getNotesObject().getDocumentByKey(key, exact);
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
  public NotesDocument getDocumentByKey(Vector key, boolean exact)
      throws NotesConnectorExceptionImpl {
    try {
      Document doc = getNotesObject().getDocumentByKey(
          (Vector) TypeConverter.toNotesItemValue(key), exact);
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
  public NotesViewNavigator createViewNavFromCategory(String category)
      throws NotesConnectorExceptionImpl {
    try {
      return new NotesViewNavigatorImpl(
          getNotesObject().createViewNavFromCategory(category));
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }
  /** {@inheritDoc} */
  @Override
  public NotesViewNavigator createViewNav()
      throws NotesConnectorExceptionImpl {
    try {
      return new NotesViewNavigatorImpl(getNotesObject().createViewNav());
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void refresh() throws NotesConnectorExceptionImpl {
    try {
      getNotesObject().refresh();
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public NotesViewEntryCollection getAllEntries() 
      throws NotesConnectorExceptionImpl {
    try {
      ViewEntryCollection results = getNotesObject().getAllEntries();
      return new NotesViewEntryCollectionImpl(results);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public NotesViewEntryCollection getAllEntriesByKey(Vector keys)
      throws NotesConnectorExceptionImpl {
    try {
      ViewEntryCollection results = getNotesObject().getAllEntriesByKey(
          TypeConverter.toNotesItemValue(keys));
      return new NotesViewEntryCollectionImpl(results);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public NotesViewEntryCollection getAllEntriesByKey(Object key)
      throws NotesConnectorExceptionImpl {
    try {
      ViewEntryCollection results = getNotesObject().getAllEntriesByKey(
          TypeConverter.toNotesItemValue(key));
      return new NotesViewEntryCollectionImpl(results);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public NotesViewEntryCollection getAllEntriesByKey(Vector keys, boolean exact)
      throws NotesConnectorExceptionImpl {
    try {
      ViewEntryCollection results = getNotesObject().getAllEntriesByKey(
          TypeConverter.toNotesItemValue(keys), exact);
      return new NotesViewEntryCollectionImpl(results);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public NotesViewEntryCollection getAllEntriesByKey(Object key, boolean exact)
      throws NotesConnectorExceptionImpl {
    try {
      ViewEntryCollection results = getNotesObject().getAllEntriesByKey(
          TypeConverter.toNotesItemValue(key), exact);
      return new NotesViewEntryCollectionImpl(results);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }
}
