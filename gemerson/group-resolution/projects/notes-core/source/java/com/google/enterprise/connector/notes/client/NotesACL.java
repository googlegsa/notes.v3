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

public interface NotesACL extends NotesBase {
  static final int LEVEL_AUTHOR = 3;
  static final int LEVEL_DEPOSITOR = 1;
  static final int LEVEL_DESIGNER = 5;
  static final int LEVEL_EDITOR = 4;
  static final int LEVEL_MANAGER = 6;
  static final int LEVEL_NOACCESS = 0;
  static final int LEVEL_READER = 2;

  /**
   * Returns the first entry.
   *
   * @return the first entry
   * @throws RepositoryException
   */
  NotesACLEntry getFirstEntry() throws RepositoryException;

  /**
   * Returns the next entry.
   *
   * @return the next entry
   * @throws RepositoryException
   */
  NotesACLEntry getNextEntry() throws RepositoryException;

  /**
   * Returns the next entry.
   *
   * @param previousEntry the entry before the one to return
   * @return the next entry, or null
   * @throws RepositoryException
   */
  NotesACLEntry getNextEntry(NotesACLEntry previousEntry)
      throws RepositoryException;

  /**
   * Returns the roles defined in this ACL.
   *
   * @return the roles defined in this ACL as a vector of strings
   * @throws RepositoryException
   */
  Vector getRoles() throws RepositoryException;
}
