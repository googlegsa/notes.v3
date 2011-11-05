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

package com.google.enterprise.connector.notes.client;

import com.google.enterprise.connector.spi.RepositoryException;

import java.util.Vector;

public interface NotesSession extends NotesBase {

  /**
   * Returns the name of the platform the session is running on.
   *
   * @return the platform name
   */
  String getPlatform() throws RepositoryException;

  /**
   * Verifies the password.
   *
   * @param password the plain-text password
   * @param hashedPassword the hashed password
   * @return true if the passwords match
   * @throws RepositoryException
   */
  boolean verifyPassword(String password, String hashedPassword)
      throws RepositoryException;

  /**
   * Returns the value of an enviroment variable.
   *
   * @param name the environment variable's name
   * @param isSystem if true, the exact variable name is used; if
   * false, "$" is prepended to the variable name
   * @return the variable's value
   */
  String getEnvironmentString(String name, boolean isSystem)
      throws RepositoryException;

  /**
   * Creates a NotesDatabase object. The underlying database will
   * be opened if possible.
   *
   * @param server the server name
   * @param database the directory path and filename of the database
   * @return a NotesDatabase object, or null
   * @throws RepositoryException
   */
  NotesDatabase getDatabase(String server, String database)
      throws RepositoryException;

  /**
   * Evaluates a formula.
   *
   * @param formula the formula to evaluate
   * @return the result
   * @throws RepositoryException
   */
  Vector evaluate(String formula) throws RepositoryException;

  /**
   * Evaluates a formula.
   *
   * @param formula the formula to evaluate
   * @param document the document context for the formula
   * @return the result
   * @throws RepositoryException
   */
  Vector evaluate(String formula, NotesDocument document)
      throws RepositoryException;

  /**
   * Creates a NotesDateTime object.
   *
   * @param date the date
   * @return a NotesDateTime object
   * @throws RepositoryException
   */
  NotesDateTime createDateTime(String date) throws RepositoryException; 
}
