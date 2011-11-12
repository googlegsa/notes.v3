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

public interface NotesDatabase extends NotesBase {
  /**
   * Finds a view or folder in the database.
   *
   * @return the view
   * @throws RepositoryException
   */
  NotesView getView(String view) throws RepositoryException;

  /**
   * Opens the specified database using the replica id.
   *
   * @param server the server
   * @param replicaId the replica id
   * @return true if the replica was found and opened
   * @throws RepositoryException
   */
  boolean openByReplicaID(String server, String replicaId)
      throws RepositoryException;

  /**
   * Finds a document in this database using the universal ID.
   *
   * @param unid the universal id
   * @return the document
   * @throws RepositoryException if the document is not found
   */
  NotesDocument getDocumentByUNID(String unid) throws RepositoryException;

  /**
   * Creates a new document in the database.
   *
   * @return the new document
   * @throws RepositoryException
   */
  NotesDocument createDocument() throws RepositoryException;

  /**
   * Gets the replica id for the database.
   *
   * @return the replica id
   * @throws RepositoryException
   */
  String getReplicaID() throws RepositoryException;

  /**
   * Gets the path and file name of the database.
   *
   * @return the path and file name of the database
   * @throws RepositoryException
   */
  String getFilePath() throws RepositoryException;

  /**
   * Searches for documents matching the formula.
   *
   * @param formula the search formula
   * @return the matching documents
   * @throws RepositoryException
   */
  NotesDocumentCollection search(String formula) throws RepositoryException;

  /**
   * Searches for documents matching the formula.
   *
   * @param formula the search formula
   * @param startDate the earliest document creation/modification
   * date to return; may be null
   * @return the matching documents
   * @throws RepositoryException
   */
  NotesDocumentCollection search(String formula, NotesDateTime startDate)
      throws RepositoryException;

  /**
   * Searches for documents matching the formula.
   *
   * @param formula the search formula
   * @param startDate the earliest document creation/modification
   * date to return; may be null
   * @param maxDocs the maximum number of documents to return;
   * use 0 to get all documents up to the configured limit
   * @return the matching documents
   * @throws RepositoryException
   */
  NotesDocumentCollection search(String formula, NotesDateTime startDate,
      int maxDocs) throws RepositoryException;

  /**
   * Gets the ACL for the database.
   *
   * @return the ACL
   * @throws RepositoryException
   */
  NotesACL getACL() throws RepositoryException;

  /**
   * Gets the ACL activity log for the database.
   *
   * @return the ACL activity log; a vector of strings
   * @throws RepositoryException
   */
  Vector getACLActivityLog() throws RepositoryException;

  /**
   * Checks whether a database is open or not
   *
   * @return true if the database is open
   * @throws RepositoryException
   */
  boolean isOpen() throws RepositoryException;
}
