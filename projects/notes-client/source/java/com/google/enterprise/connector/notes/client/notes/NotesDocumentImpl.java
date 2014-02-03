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

import com.google.enterprise.connector.notes.client.NotesDateTime;
import com.google.enterprise.connector.notes.client.NotesDocument;
import com.google.enterprise.connector.notes.client.NotesDocumentCollection;
import com.google.enterprise.connector.notes.client.NotesEmbeddedObject;
import com.google.enterprise.connector.notes.client.NotesItem;
import com.google.enterprise.connector.notes.client.NotesRichTextItem;

import lotus.domino.Document;
import lotus.domino.EmbeddedObject;
import lotus.domino.Item;
import lotus.domino.NotesException;

import java.util.Vector;

class NotesDocumentImpl extends NotesBaseImpl<Document>
    implements NotesDocument {

  NotesDocumentImpl(Document document) {
    super(document);
  }

  /** {@inheritDoc} */
  @Override
  public boolean hasItem(String name) throws NotesConnectorExceptionImpl {
    try {
      return getNotesObject().hasItem(name);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public String getItemValueString(String name)
      throws NotesConnectorExceptionImpl {
    try {
      return getNotesObject().getItemValueString(name);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public int getItemValueInteger(String name)
      throws NotesConnectorExceptionImpl {
    try {
      return getNotesObject().getItemValueInteger(name);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public Vector getItemValue(String name) throws NotesConnectorExceptionImpl {
    try {
      return TypeConverter.toConnectorValues(
          getNotesObject().getItemValue(name));
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public NotesItem getFirstItem(String name)
      throws NotesConnectorExceptionImpl {
    try {
      Item item = getNotesObject().getFirstItem(name);
      if (item == null) {
        return null;
      }
      return new NotesItemImpl(item);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public Vector getItems() throws NotesConnectorExceptionImpl {
    try {
      return TypeConverter.toConnectorItems(getNotesObject().getItems());
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public Vector getItemValueDateTimeArray(String name)
      throws NotesConnectorExceptionImpl {
    try {
      return TypeConverter.toConnectorValues(
          getNotesObject().getItemValueDateTimeArray(name));
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void removeItem(String name) throws NotesConnectorExceptionImpl {
    try {
      getNotesObject().removeItem(name);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }


  /** {@inheritDoc} */
  @Override
  public NotesItem replaceItemValue(String name, Object value)
      throws NotesConnectorExceptionImpl {
    try {
      Item item = getNotesObject().replaceItemValue(name,
          TypeConverter.toNotesItemValue(value));
      if (item == null) { // It's not clear if this can happen.
        return null;
      }
      return new NotesItemImpl(item);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public NotesItem appendItemValue(String name, Object value)
      throws NotesConnectorExceptionImpl {
    try {
      Item item = getNotesObject().appendItemValue(name,
          TypeConverter.toNotesItemValue(value));
      if (item == null) { // It's not clear if this can happen.
        return null;
      }
      return new NotesItemImpl(item);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public boolean save() throws NotesConnectorExceptionImpl {
    try {
      return getNotesObject().save();
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public boolean save(boolean force) throws NotesConnectorExceptionImpl {
    try {
      return getNotesObject().save(force);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public boolean remove(boolean force) throws NotesConnectorExceptionImpl {
    try {
      return getNotesObject().remove(force);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public NotesDocumentCollection getResponses()
      throws NotesConnectorExceptionImpl {
    try {
      return new NotesDocumentCollectionImpl(getNotesObject().getResponses());
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public NotesEmbeddedObject getAttachment(String filename)
      throws NotesConnectorExceptionImpl {
    try {
      EmbeddedObject obj = getNotesObject().getAttachment(filename);
      if (obj == null) {
        return null;
      }
      return new NotesEmbeddedObjectImpl(obj);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public String getNotesURL() throws NotesConnectorExceptionImpl {
    try {
      return getNotesObject().getNotesURL();
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public String getUniversalID() throws NotesConnectorExceptionImpl {
    try {
      return getNotesObject().getUniversalID();
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void copyAllItems(NotesDocument doc, boolean replace)
      throws NotesConnectorExceptionImpl {
    try {
      getNotesObject().copyAllItems(
          ((NotesDocumentImpl) doc).getNotesObject(), replace);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public NotesRichTextItem createRichTextItem(String name)
      throws NotesConnectorExceptionImpl {
    try {
      return new NotesRichTextItemImpl(
          getNotesObject().createRichTextItem(name));
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public NotesDateTime getCreated() throws NotesConnectorExceptionImpl {
    try {
      return new NotesDateTimeImpl(getNotesObject().getCreated());
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public NotesDateTime getLastModified() throws NotesConnectorExceptionImpl {
    try {
      return new NotesDateTimeImpl(getNotesObject().getLastModified());
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public Vector getAuthors() throws NotesConnectorExceptionImpl {
    try {
      return getNotesObject().getAuthors();
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }
}
