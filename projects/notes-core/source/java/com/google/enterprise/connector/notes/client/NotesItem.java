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

public interface NotesItem extends NotesBase {
  static final int TEXT = 1280;
  static final int NUMBERS = 768;
  static final int DATETIMES = 1024;
  static final int RICHTEXT = 1;
  static final int NAMES = 1074;
  static final int AUTHORS = 1076;
  static final int READERS = 1075;

  /**
   * Returns the item's name.
   *
   * @return the item's name
   * @throws RepositoryException
   */
  String getName() throws RepositoryException;

  /**
   * Returns the item's type.
   *
   * @return the item's type
   * @throws RepositoryException
   */
  int getType() throws RepositoryException;

  /**
   * Returns the text.
   *
   * @param maxlen the maximum text length to return
   * @return the item, as text
   * @throws RepositoryException
   */
  String getText(int maxlen) throws RepositoryException;

  /**
   * Indicates if the item is of type Readers.
   *
   * @return true if the item is of type Readers
   * @throws RepositoryException
   */
  boolean isReaders() throws RepositoryException;

  /**
   * Indicates if the item is of type Authors.
   *
   * @return true if the item is of type Authors
   * @throws RepositoryException
   */
  boolean isAuthors() throws RepositoryException;

  /**
   * Returns the values of the item.
   *
   * @return the values, or null
   * @throws RepositoryException
   */
  Vector getValues() throws RepositoryException;

  /**
   * Adds value to text list.
   *
   * @param value the value
   * @throws RepositoryException
   */
  void appendToTextList(String value) throws RepositoryException;

  /**
   * Adds values to text list.
   *
   * @param values the values as a vector of strings
   * @throws RepositoryException
   */
  void appendToTextList(Vector values) throws RepositoryException;

  /**
   * Indicates whether the item contains summary data.
   *
   * @param summary true if the item contains summary data
   * @throws RepositoryException
   */
  void setSummary(boolean summary) throws RepositoryException;
}
