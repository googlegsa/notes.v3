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

public interface NotesEmbeddedObject extends NotesBase {
  final int EMBED_ATTACHMENT = 1454;
  final int EMBED_OBJECT = 1453;
  final int EMBED_OBJECTLINK = 1452;

  /**
   * Returns the type: object, object link, attachment.
   *
   * @return the type
   * @throws RepositoryException
   */
  int getType() throws RepositoryException;

  /**
   * Returns the file size in bytes.
   *
   * @return the file size; 0 if not an attachment
   * @throws RepositoryException
   */
  int getFileSize() throws RepositoryException;

  /**
   * Writes a file to the file system.
   *
   * @param path the path for the file, including the file name
   * @throws RepositoryException
   */
  void extractFile(String path) throws RepositoryException;
}
