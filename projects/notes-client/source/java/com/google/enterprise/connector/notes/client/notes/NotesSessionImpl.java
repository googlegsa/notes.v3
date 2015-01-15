// Copyright 2011 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.enterprise.connector.notes.client.notes;

import com.google.enterprise.connector.notes.client.NotesDatabase;
import com.google.enterprise.connector.notes.client.NotesDateTime;
import com.google.enterprise.connector.notes.client.NotesDocument;
import com.google.enterprise.connector.notes.client.NotesName;
import com.google.enterprise.connector.notes.client.NotesSession;

import lotus.domino.Database;
import lotus.domino.NotesException;
import lotus.domino.Session;

import java.util.Date;
import java.util.Vector;

class NotesSessionImpl extends NotesBaseImpl<Session>
    implements NotesSession {

  NotesSessionImpl(Session session) {
    super(session);
  }

  /** {@inheritDoc} */
  @Override
  public String getPlatform() throws NotesConnectorExceptionImpl {
    try {
      return getNotesObject().getPlatform();
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public String getNotesVersion() throws NotesConnectorExceptionImpl {
    try {
      return getNotesObject().getNotesVersion();
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public String getCommonUserName() throws NotesConnectorExceptionImpl {
    try {
      return getNotesObject().getCommonUserName();
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public boolean verifyPassword(String password, String hashedPassword)
      throws NotesConnectorExceptionImpl {
    try {
      return getNotesObject().verifyPassword(password, hashedPassword);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public String getEnvironmentString(String name, boolean isSystem)
      throws NotesConnectorExceptionImpl {
    try {
      return getNotesObject().getEnvironmentString(name, isSystem);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public NotesDatabase getDatabase(String server, String database)
      throws NotesConnectorExceptionImpl {
    try {
      Database databaseObj = getNotesObject().getDatabase(server, database);
      if (databaseObj == null) {
        return null;
      }
      return new NotesDatabaseImpl(databaseObj);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public Vector evaluate(String formula) throws NotesConnectorExceptionImpl {
    try {
      return TypeConverter.toConnectorValues(
          getNotesObject().evaluate(formula));
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public Vector evaluate(String formula, NotesDocument document)
      throws NotesConnectorExceptionImpl {
    try {
      return TypeConverter.toConnectorValues(
          getNotesObject().evaluate(formula,
              ((NotesDocumentImpl) document).getNotesObject()));
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public NotesDateTime createDateTime(String date)
      throws NotesConnectorExceptionImpl {
    try {
      return new NotesDateTimeImpl(getNotesObject().createDateTime(date));
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public NotesDateTime createDateTime(Date date)
      throws NotesConnectorExceptionImpl {
    try {
      return new NotesDateTimeImpl(getNotesObject().createDateTime(date));
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public NotesName createName(String name)
      throws NotesConnectorExceptionImpl {
    try {
      return new NotesNameImpl(getNotesObject().createName(name));
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }
}
