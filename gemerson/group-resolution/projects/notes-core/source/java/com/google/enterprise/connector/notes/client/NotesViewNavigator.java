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

public interface NotesViewNavigator extends NotesBase {
  /**
   * Gets the number of entries.
   *
   * @return the number of entries.
   * @throws RepositoryException
   */
  int getCount() throws RepositoryException;

  /**
   * Gets the first entry.
   *
   * @return the first entry, or null
   * @throws RepositoryException
   */
  NotesViewEntry getFirst() throws RepositoryException;

  /**
   * Gets the next entry.
   *
   * @return the next entry, or null
   * @throws RepositoryException
   */
  NotesViewEntry getNext() throws RepositoryException;

  /**
   * Gets the next entry.
   *
   * @param previousEntry the previous entry
   * @return the next entry, or null
   * @throws RepositoryException
   */
  NotesViewEntry getNext(NotesViewEntry previousEntry)
      throws RepositoryException;

  /**
   * Gets the first document entry.
   *
   * @return the first document, or null
   * @throws RepositoryException
   */
  NotesViewEntry getFirstDocument() throws RepositoryException;
}
