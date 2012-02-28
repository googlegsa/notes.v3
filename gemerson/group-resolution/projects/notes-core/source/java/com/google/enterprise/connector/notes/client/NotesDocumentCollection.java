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

public interface NotesDocumentCollection extends NotesBase {
  /**
   * Gets the first document.
   *
   * @return the first document, or null
   * @throws RepositoryException
   */
  NotesDocument getFirstDocument() throws RepositoryException;

  /**
   * Gets the next document.
   *
   * @return the next document, or null
   * @throws RepositoryException
   */
  NotesDocument getNextDocument() throws RepositoryException;

  /**
   * Gets the next document following the given document.
   *
   * @param document the document
   * @return the next document, or null
   * @throws RepositoryException
   */
  NotesDocument getNextDocument(NotesDocument document)
      throws RepositoryException;

  /**
   * Returns the number of documents.
   *
   * @return the number of documents
   * @throws RepositoryException
   */
  int getCount() throws RepositoryException;
}
