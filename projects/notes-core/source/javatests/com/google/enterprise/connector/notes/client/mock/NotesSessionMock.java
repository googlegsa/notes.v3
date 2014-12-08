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

import com.google.enterprise.connector.notes.TESTCONST;
import com.google.enterprise.connector.notes.client.NotesDatabase;
import com.google.enterprise.connector.notes.client.NotesDateTime;
import com.google.enterprise.connector.notes.client.NotesDocument;
import com.google.enterprise.connector.notes.client.NotesName;
import com.google.enterprise.connector.notes.client.NotesSession;
import com.google.enterprise.connector.spi.RepositoryException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;

public class NotesSessionMock extends NotesBaseMock
    implements NotesSession {
  private static final String CLASS_NAME = NotesSessionMock.class.getName();

  /** The logger for this class. */
  private static final Logger LOGGER =
      Logger.getLogger(CLASS_NAME);

  private final List<NotesDatabaseMock> databases;
  private final Map<String, String> environment;
  private final String versionString;

  NotesSessionMock(List<NotesDatabaseMock> databases,
      Map<String, String> environment) {
    this.databases = databases;
    this.environment = environment;
    String envVer = environment.get(TESTCONST.NOTES_VERSION);
    if (envVer == null) {
      this.versionString = TESTCONST.NotesVersion.VERSION_8.toString();
    } else {
      this.versionString = envVer;
    }
  }

  public void addDatabase(NotesDatabaseMock database) {
    databases.add(database);
  }

  /** {@inheritDoc} */
  @Override
  public String getPlatform() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getPlatform");
    return null;
 }

  /** {@inheritDoc} */
  @Override
  public String getNotesVersion() throws RepositoryException {
    return versionString;
  }

  /** {@inheritDoc} */
  @Override
  public String getCommonUserName() throws RepositoryException {
    return null;
  }

  /** {@inheritDoc} */
  @Override
  public boolean verifyPassword(String password, String hashedPassword)
      throws RepositoryException {
    return password.equals(hashedPassword);
  }

  /** {@inheritDoc} */
  @Override
  public String getEnvironmentString(String name, boolean isSystem)
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getEnvironmentString");
    return environment.get(name);
 }

  /** {@inheritDoc} */
  @Override
  public NotesDatabase getDatabase(String server, String database)
      throws RepositoryException {
   LOGGER.entering(CLASS_NAME, "getDatabase");

   if (null == server && null == database) {
     NotesDatabaseMock db = new NotesDatabaseMock(null, null);
     db.setSession(this);
     return db;
   }
   for (NotesDatabaseMock db : databases) {
     if (server.equals(db.getServer()) && database.equals(db.getName())) {
       return db;
     }
   }
   return null;
  }

  NotesDatabase getDatabaseByReplicaId(String server, String replicaId)
      throws RepositoryException {
   for (NotesDatabaseMock db : databases) {
     if (server.equals(db.getServer())
         && replicaId.equals(db.getReplicaID())) {
       return db;
     }
   }
   return null;
  }

  /** {@inheritDoc} */
  @Override
  public Vector evaluate(String formula) throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "evaluate");
    // Handle the special formula in NotesUserGroupManager
    if (formula.startsWith("@Name([ABBREVIATE]")) {
      int start = formula.indexOf("\"");
      int end = formula.lastIndexOf("\"");
      if (start > -1 && end > -1) {
        String result = formula.substring(start + 1, end);
        Vector<String> returnResult = new Vector<String>();
        returnResult.add(result);
        return returnResult;
      } else {
        throw new RepositoryException(formula);
      }
    }

    // Return a result meaning "true";
    Vector<Integer> result = new Vector<Integer>();
    result.add(new Integer(1));
    return result;
 }

  /** {@inheritDoc} */
  @Override
  public Vector evaluate(String formula, NotesDocument document)
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "evaluate");

    Vector result = document.getItemValue("evaluate_" + formula);
    LOGGER.fine("evaluate " + formula + " returning " + result);
    return result;
 }

  /** {@inheritDoc} */
  @Override
  public NotesDateTime createDateTime(String date) throws RepositoryException {
   LOGGER.entering(CLASS_NAME, "createDateTime");
   try {
     SimpleDateFormat format = new SimpleDateFormat("M/d/yyyy");
     Date d = format.parse(date);
     return new NotesDateTimeMock(d);
   } catch (ParseException e) {
     throw new RepositoryException(e);
   }
  }

  @Override
  public NotesDateTime createDateTime(Date date) throws RepositoryException {
    return new NotesDateTimeMock(date);
  }

  /** {@inheritDoc} */
  @Override
  public NotesName createName(String name) throws RepositoryException {
    return new NotesNameMock(name);
  }

  /* TODO: implement getUserName.
  @Override
  public String toString() {
    try {
      return getUserName();
    } catch (RepositoryException e) {
      return "";
    }
  }
  */
}
