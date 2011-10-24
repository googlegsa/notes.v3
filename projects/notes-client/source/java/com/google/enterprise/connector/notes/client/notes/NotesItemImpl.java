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

import com.google.enterprise.connector.notes.client.NotesItem;

import lotus.domino.Item;
import lotus.domino.NotesException;

import java.util.Vector;

class NotesItemImpl extends NotesBaseImpl<Item> implements NotesItem {

  static {
    assert ACTIONCD == Item.ACTIONCD;
    assert ASSISTANTINFO == Item.ASSISTANTINFO;
    assert ATTACHMENT == Item.ATTACHMENT;
    assert AUTHORS == Item.AUTHORS;
    assert COLLATION == Item.COLLATION;
    assert DATETIMES == Item.DATETIMES;
    assert EMBEDDEDOBJECT == Item.EMBEDDEDOBJECT;
    assert ERRORITEM == Item.ERRORITEM;
    assert FORMULA == Item.FORMULA;
    assert HTML == Item.HTML;
    assert ICON == Item.ICON;
    assert LSOBJECT == Item.LSOBJECT;
    assert MIME_PART == Item.MIME_PART;
    assert NAMES == Item.NAMES;
    assert NOTELINKS == Item.NOTELINKS;
    assert NOTEREFS == Item.NOTEREFS;
    assert NUMBERS == Item.NUMBERS;
    assert OTHEROBJECT == Item.OTHEROBJECT;
    assert QUERYCD == Item.QUERYCD;
    assert READERS == Item.READERS;
    assert RICHTEXT == Item.RICHTEXT;
    assert SIGNATURE == Item.SIGNATURE;
    assert TEXT == Item.TEXT;
    assert UNAVAILABLE == Item.UNAVAILABLE;
    assert UNKNOWN == Item.UNKNOWN;
    assert USERDATA == Item.USERDATA;
    assert USERID == Item.USERID;
    assert VIEWMAPDATA == Item.VIEWMAPDATA;
    assert VIEWMAPLAYOUT == Item.VIEWMAPLAYOUT;
  }

  NotesItemImpl(Item item) {
    super(item);
  }

  /** {@inheritDoc} */
  /* @Override */
  public String getName() throws NotesConnectorExceptionImpl {
    try {
      return getNotesObject().getName();
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  /* @Override */
  public int getType() throws NotesConnectorExceptionImpl {
    try {
      return getNotesObject().getType();
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  /* @Override */
  public String getText(int maxlen) throws NotesConnectorExceptionImpl {
    try {
      return getNotesObject().getText(maxlen);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  /* @Override */
  public boolean isReaders() throws NotesConnectorExceptionImpl {
    try {
      return getNotesObject().isReaders();
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  /* @Override */
  public boolean isAuthors() throws NotesConnectorExceptionImpl {
    try {
      return getNotesObject().isAuthors();
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  /* @Override */
  public Vector getValues() throws NotesConnectorExceptionImpl {
    try {
      return TypeConverter.toConnectorValues(getNotesObject().getValues());
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  /* @Override */
  public void appendToTextList(String value)
      throws NotesConnectorExceptionImpl {
    try {
      getNotesObject().appendToTextList(value);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  /* @Override */
  public void appendToTextList(Vector values)
      throws NotesConnectorExceptionImpl {
    try {
      getNotesObject().appendToTextList(values);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  /* @Override */
  public void setSummary(boolean summary) throws NotesConnectorExceptionImpl {
    try {
      getNotesObject().setSummary(summary);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }
}
