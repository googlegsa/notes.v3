// Copyright 2012 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.enterprise.connector.notes.client;

import com.google.enterprise.connector.spi.RepositoryException;

public interface NotesName extends NotesBase {

  /**
   * Returns a hierarchical name in canonical form. Returns an
   * Internet or flat name as-is.
   *
   * @return the name
   * @throws RepositoryException
   */
  String getCanonical() throws RepositoryException;

  /**
   * Return abbreviated name based on the canonical format.
   * 
   * @return abbreviated name
   * @throws RepositoryException
   */
  String getAbbreviated() throws RepositoryException;
}
