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

import com.google.enterprise.connector.notes.client.NotesACL;
import com.google.enterprise.connector.notes.client.NotesACLEntry;

import lotus.domino.ACL;
import lotus.domino.ACLEntry;
import lotus.domino.NotesException;

import java.util.Vector;

class NotesACLImpl extends NotesBaseImpl<ACL> implements NotesACL {

  static {
    assert LEVEL_AUTHOR == ACL.LEVEL_AUTHOR;
    assert LEVEL_DEPOSITOR == ACL.LEVEL_DEPOSITOR;
    assert LEVEL_DESIGNER == ACL.LEVEL_DESIGNER;
    assert LEVEL_EDITOR == ACL.LEVEL_EDITOR;
    assert LEVEL_MANAGER == ACL.LEVEL_MANAGER;
    assert LEVEL_NOACCESS == ACL.LEVEL_NOACCESS;
    assert LEVEL_READER == ACL.LEVEL_READER;
  }

  NotesACLImpl(ACL acl) {
    super(acl);
  }

  /** {@inheritDoc} */
  /* Apparently never returns null. */
  @Override
  public NotesACLEntry getFirstEntry() throws NotesConnectorExceptionImpl {
    try {
      return new NotesACLEntryImpl(getNotesObject().getFirstEntry());
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public NotesACLEntry getNextEntry() throws NotesConnectorExceptionImpl {
    try {
      ACLEntry entry = getNotesObject().getNextEntry();
      if (entry == null) {
        return null;
      }
      return new NotesACLEntryImpl(entry);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public NotesACLEntry getNextEntry(NotesACLEntry previousEntry)
      throws NotesConnectorExceptionImpl {
    try {
      ACLEntry entry = getNotesObject().getNextEntry(
          ((NotesACLEntryImpl) previousEntry).getNotesObject());
      if (entry == null) {
        return null;
      }
      return new NotesACLEntryImpl(entry);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public Vector getRoles() throws NotesConnectorExceptionImpl {
    try {
      return getNotesObject().getRoles();
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }
}
