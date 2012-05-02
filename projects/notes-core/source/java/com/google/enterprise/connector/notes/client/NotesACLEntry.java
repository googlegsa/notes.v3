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

public interface NotesACLEntry extends NotesBase {

  static final int TYPE_MIXED_GROUP = 3;
  static final int TYPE_PERSON = 1;
  static final int TYPE_PERSON_GROUP = 4;
  static final int TYPE_SERVER = 2;
  static final int TYPE_SERVER_GROUP = 5;
  static final int TYPE_UNSPECIFIED = 0;

  /**
   * Returns the user type.
   *
   * @return the user type
   * @throws RepositoryException
   */
  int getUserType() throws RepositoryException;

  /**
   * Returns the level.
   *
   * @return the level
   * @throws RepositoryException
   */
  int getLevel() throws RepositoryException;

  /**
   * Returns true if the role is enabled.
   *
   * @param role the role
   * @return true if the role is enabled
   * @throws RepositoryException
   */
  boolean isRoleEnabled(String role) throws RepositoryException;

  /**
   * Returns the name of the entry.
   *
   * @return the name
   * @throws RepositoryException
   */
  String getName() throws RepositoryException;

  /**
   * Returns the roles that are enabled for this entry.
   *
   * @return the roles that are enabled for this entry
   * @throws RepositoryException
   */
  Vector getRoles() throws RepositoryException;
}
