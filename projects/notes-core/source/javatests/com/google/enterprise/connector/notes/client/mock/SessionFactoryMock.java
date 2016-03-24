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
import com.google.enterprise.connector.notes.client.NotesThread;
import com.google.enterprise.connector.notes.client.SessionFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class SessionFactoryMock implements SessionFactory {
  private static final String CLASS_NAME =
      SessionFactoryMock.class.getName();

  /** The logger for this class. */
  private static final Logger LOGGER =
      Logger.getLogger(CLASS_NAME);

  private final List<NotesDatabaseMock> databases =
      new ArrayList<NotesDatabaseMock>();
  private final Map<String, String> environment = new HashMap<String, String>();

  public SessionFactoryMock() {
  }

  /** {@inheritDoc} */
  @Override
  public NotesSession createSessionWithFullAccess(String password) {
    LOGGER.entering(CLASS_NAME, "createSessionWithFullAccess");
    return new NotesSessionMock(databases, environment);
  }

  /** {@inheritDoc} */
  @Override
  public NotesThread getNotesThread() {
    LOGGER.entering(CLASS_NAME, "getNotesThread");
    return new NotesThreadMock();
  }

  public void addDatabase(NotesDatabaseMock database) {
    databases.add(database);
  }

  public NotesDatabaseMock getDatabase(String name) {
    for (NotesDatabaseMock database : databases) {
      if (name.equals(database.getName())) {
        return database;
      }
    }
    return null;
  }

  public void setEnvironmentProperty(String name, String value) {
    environment.put(name, value);
  }
}
