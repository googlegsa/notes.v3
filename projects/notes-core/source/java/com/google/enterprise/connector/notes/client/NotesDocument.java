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

public interface NotesDocument extends NotesBase {
  /**
   * Returns true if the document is deleted.
   *
   * @return true if the document is deleted
   * @throws RepositoryException
   */
  boolean isDeleted() throws RepositoryException;

  /**
   * Returns true if the document is valid.
   *
   * @return true if the document is valid
   * @throws RepositoryException
   */
  boolean isValid() throws RepositoryException;

  /**
   * Returns true if the document has the given item.
   *
   * @param name the item name
   * @return true if the document has an item with that name
   * @throws RepositoryException
   */
  boolean hasItem(String name) throws RepositoryException;

  /**
   * Returns the value of an item.
   *
   * @param name the item name
   * @return the value; the value of the first item if multiple
   * items share a name; or an empty string
   * @throws RepositoryException
   */
  String getItemValueString(String name) throws RepositoryException;

  /**
   * Returns the value of an item.
   *
   * @param name the item name
   * @return the value; 0 if the item has no value or does not exist
   * @throws RepositoryException
   */
  int getItemValueInteger(String name) throws RepositoryException;

  /**
   * Returns the value of an item.
   *
   * @param name the item name
   * @return the value; the Vector may contain String, Double, or
   * NotesDateTime elements; an empty Vector if the name doesn't
   * exist or has no value
   * @throws RepositoryException
   */
  Vector getItemValue(String name) throws RepositoryException;

  /**
   * Returns an item.
   *
   * @param name the item name
   * @return the item, or null
   * @throws RepositoryException
   */
  NotesItem getFirstItem(String name) throws RepositoryException;

  /**
   * Gets all items in the document.
   *
   * @return all items
   * @throws RepositoryException
   */
  Vector getItems() throws RepositoryException;

  /**
   * Returns the value of an item.
   *
   * @param name the item name
   * @return the value; the Vector may contain NotesDateTime or
   * NotesDateTimeRange elements
   * @throws RepositoryException
   */
  Vector getItemValueDateTimeArray(String name) throws RepositoryException;

  /**
   * Removes an item from a document.
   *
   * @param name the item name
   * @throws RepositoryException
   */
  void removeItem(String name) throws RepositoryException;

  /**
   * Replaces the item with a new one with the given value, or
   * creates a new item with the value. The value parameter can
   * be of type String, Integer, Double, NotesDateTime, Vector
   * with String/Integer/Double/NotesDateTime elements, or
   * NotesItem.
   *
   * @param name the item name
   * @param value the item value
   * @return the new item
   * @throws RepositoryException
   */
  NotesItem replaceItemValue(String name, Object value)
      throws RepositoryException;

  /**
   * Creates a new item with the given value, or creates a second
   * item with the same name.
   *
   * @param name the item name
   * @param value the item value
   * @return the new item
   * @throws RepositoryException
   */
  NotesItem appendItemValue(String name, Object value)
  throws RepositoryException;

  /**
   * Saves changes.
   *
   * @return true if the document is saved
   * @throws RepositoryException
   */
  boolean save() throws RepositoryException;

  /**
   * Saves changes.
   *
   * @param force force the save
   * @return true if the document is saved
   * @throws RepositoryException
   */
  boolean save(boolean force) throws RepositoryException;

  /**
   * Removes a document from a database.
   *
   * @param force if true, forces removal even if the document
   * has been modified
   * @return true if the document is removed
   * @throws RepositoryException
   */
  boolean remove(boolean force) throws RepositoryException;

  /**
   * Gets the responses to a document.
   *
   * @return the responses; possibly an empty collection
   * @throws RepositoryException
   */
  NotesDocumentCollection getResponses() throws RepositoryException;

  /**
   * Gets a file attachment.
   *
   * @param filename the filename
   * @return the attachment, or null
   * @throws RepositoryException
   */
  NotesEmbeddedObject getAttachment(String filename)
      throws RepositoryException;

  /**
   * Gets the Notes URL.
   *
   * @return the Notes URL
   * @throws RepositoryException
   */
  String getNotesURL() throws RepositoryException;

  /**
   * Gets the universal ID.
   *
   * @return the universal ID
   * @throws RepositoryException
   */
  String getUniversalID() throws RepositoryException;

  /**
   * Copies all document items into the specified document.
   *
   * @param doc the destination document
   * @param replace if true, the items in the destination
   * document are replaced
   * @throws RepositoryException
   */
  void copyAllItems(NotesDocument doc, boolean replace)
      throws RepositoryException;

  /**
   * Creates a rich text item in the document.
   *
   * @param name the item name
   * @return the new item
   * @throws RepositoryException
   */
  NotesRichTextItem createRichTextItem(String name)
      throws RepositoryException;

  /**
   * Gets the date the document was created.
   *
   * @return the date document was created
   * @throws RepositoryException
   */
  NotesDateTime getCreated() throws RepositoryException;

  /**
   * Gets the date the document was last modified.
   *
   * @return the date document was last modified
   * @throws RepositoryException
   */
  NotesDateTime getLastModified() throws RepositoryException;

  /**
   * Gets document authors.
   *
   * @return document authors as a vector of strings
   * @throws RepositoryException
   */
  Vector getAuthors() throws RepositoryException;
}
