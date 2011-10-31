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

public interface NotesView extends NotesBase {
  /**
   * Returns the number of documents in the view.
   *
   * @return the number of documents in the view
   * @throws RepositoryException
   */
  int getEntryCount() throws RepositoryException;

  /**
   * Gets the first document.
   *
   * @return the first document, or null
   * @throws RepositoryException
   */
  NotesDocument getFirstDocument() throws RepositoryException;

  /**
   * Gets the next document after the given document.
   *
   * @param previousDocument the previous document
   * @return the next document, or null
   * @throws RepositoryException
   */
  NotesDocument getNextDocument(NotesDocument previousDocument)
      throws RepositoryException;

  /**
   * Gets a document using a key.
   *
   * @param key the key
   * @return the matching document, or null
   * @throws RepositoryException
   */
  NotesDocument getDocumentByKey(Object key)
      throws RepositoryException;

  /**
   * Gets a document using a key vector. The vector may contain
   * string, number, DateTime, or DateRange objects.
   *
   * @param key the key
   * @return the matching document, or null
   * @throws RepositoryException
   */
  NotesDocument getDocumentByKey(Vector key)
      throws RepositoryException;

  /**
   * Gets a document using a key.
   *
   * @param key the key
   * @param exact true if the match must be exact
   * @return the matching document, or null
   * @throws RepositoryException
   */
  NotesDocument getDocumentByKey(Object key, boolean exact)
      throws RepositoryException;

  /**
   * Gets a document using a key vector. The vector may contain
   * string, number, DateTime, or DateRange objects.
   *
   * @param key the key
   * @param exact true if the match must be exact
   * @return the matching document, or null
   * @throws RepositoryException
   */
  NotesDocument getDocumentByKey(Vector key, boolean exact)
      throws RepositoryException;

  /**
   * Creates a NotesViewNavigator for entries in this view with
   * the given category.
   *
   * @param category the category name
   * @return the NotesViewNavigator
   * @throws RepositoryException
   */
  NotesViewNavigator createViewNavFromCategory(String category)
      throws RepositoryException;

  /**
   * Creates a NotesViewNavigator for entries in this view.
   *
   * @return the NotesViewNavigator
   * @throws RepositoryException
   */
  NotesViewNavigator createViewNav() throws RepositoryException;

  /**
   * Updates the view.
   *
   * @throws RepositoryException
   */
  void refresh() throws RepositoryException;

  /**
   * Finds document entries in this view by key value.
   *
   * @param keys the column keys; each object may be a string,
   * number, NotesDateTime, or NotesDateRange
   * @return the matching entries, or an empty vector
   * @throws RepositoryException
   */
  NotesViewEntryCollection getAllEntriesByKey(Vector keys)
      throws RepositoryException;

  /**
   * Finds document entries in this view by key value.
   *
   * @param key the column key; may be a string,
   * number, NotesDateTime, or NotesDateRange
   * @return the matching entries, or an empty vector
   * @throws RepositoryException
   */
  NotesViewEntryCollection getAllEntriesByKey(Object key)
      throws RepositoryException;

  /**
   * Finds document entries in this view by key value.
   *
   * @param keys the column keys; each object may be a string,
   * number, NotesDateTime, or NotesDateRange
   * @param exact requires an exact match
   * @return the matching entries, or an empty vector
   * @throws RepositoryException
   */
  NotesViewEntryCollection getAllEntriesByKey(Vector keys, boolean exact)
      throws RepositoryException;

  /**
   * Finds document entries in this view by key value.
   *
   * @param key the column key; may be a string,
   * number, NotesDateTime, or NotesDateRange
   * @param exact requires an exact match
   * @return the matching entries, or an empty vector
   * @throws RepositoryException
   */
  NotesViewEntryCollection getAllEntriesByKey(Object key, boolean exact)
      throws RepositoryException;
}
