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

import com.google.enterprise.connector.notes.client.NotesACLEntry;
import com.google.enterprise.connector.notes.client.NotesName;

import lotus.domino.ACLEntry;
import lotus.domino.NotesException;

import java.util.Vector;

class NotesACLEntryImpl extends NotesBaseImpl<ACLEntry>
    implements NotesACLEntry {

  static {
    assert TYPE_MIXED_GROUP == ACLEntry.TYPE_MIXED_GROUP;
    assert TYPE_PERSON == ACLEntry.TYPE_PERSON;
    assert TYPE_PERSON_GROUP == ACLEntry.TYPE_PERSON_GROUP;
    assert TYPE_SERVER == ACLEntry.TYPE_SERVER;
    assert TYPE_SERVER_GROUP == ACLEntry.TYPE_SERVER_GROUP;
    assert TYPE_UNSPECIFIED == ACLEntry.TYPE_UNSPECIFIED;
  }

  NotesACLEntryImpl(ACLEntry aclEntry) {
    super(aclEntry);
  }

  /** {@inheritDoc} */
  /* @Override */
  public int getUserType() throws NotesConnectorExceptionImpl {
    try {
      return getNotesObject().getUserType();
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  /* @Override */
  public int getLevel() throws NotesConnectorExceptionImpl {
    try {
      return getNotesObject().getLevel();
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  /* @Override */
  public boolean isRoleEnabled(String role)
      throws NotesConnectorExceptionImpl {
    try {
      return getNotesObject().isRoleEnabled(role);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
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
  public NotesName getNameObject() throws NotesConnectorExceptionImpl {
    try {
      return new NotesNameImpl(getNotesObject().getNameObject());
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  /* @Override */
  public Vector getRoles() throws NotesConnectorExceptionImpl {
    try {
      return TypeConverter.toConnectorValues(getNotesObject().getRoles());
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }
}
