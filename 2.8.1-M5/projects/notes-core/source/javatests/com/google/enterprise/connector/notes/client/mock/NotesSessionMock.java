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

import com.google.enterprise.connector.notes.client.NotesSession;
import com.google.enterprise.connector.notes.client.NotesDatabase;
import com.google.enterprise.connector.notes.client.NotesDateTime;
import com.google.enterprise.connector.notes.client.NotesDocument;
import com.google.enterprise.connector.notes.client.NotesView;
import com.google.enterprise.connector.spi.RepositoryException;

import java.util.Vector;
import java.util.logging.Logger;

class NotesSessionMock extends NotesBaseMock
    implements NotesSession {
  private static final String CLASS_NAME = NotesSessionMock.class.getName();

  /** The logger for this class. */
  private static final Logger LOGGER =
      Logger.getLogger(CLASS_NAME);

  NotesSessionMock() {
  }

  /** {@inheritDoc} */
  /* @Override */
  public String getPlatform() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getPlatform");
    return null;
 }

  /** {@inheritDoc} */
  /* @Override */
  public String getCommonUserName() throws RepositoryException {
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public boolean verifyPassword(String password, String hashedPassword)
      throws RepositoryException {
    return false;
  }

  /** {@inheritDoc} */
  /* @Override */
  public String getEnvironmentString(String name, boolean isSystem)
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getEnvironmentString");
    return null;
 }

  /** {@inheritDoc} */
  /* @Override */
  public NotesDatabase getDatabase(String server, String database)
      throws RepositoryException {
   LOGGER.entering(CLASS_NAME, "getDatabase");
   return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public Vector evaluate(String formula) throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "evaluate");
    return null;
 }

  /** {@inheritDoc} */
  /* @Override */
  public Vector evaluate(String formula, NotesDocument document)
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "evaluate");
    return null;
 }

  /** {@inheritDoc} */
  /* @Override */
  public NotesDateTime createDateTime(String date) throws RepositoryException {
   LOGGER.entering(CLASS_NAME, "createDateTime");
   return null;
  }

  /* TODO: implement getUserName.
  public String toString() {
    try {
      return getUserName();
    } catch (RepositoryException e) {
      return "";
    }
  }
  */
}
