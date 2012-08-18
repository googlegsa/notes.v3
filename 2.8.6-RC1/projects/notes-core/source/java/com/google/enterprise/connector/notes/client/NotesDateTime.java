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

import java.util.Date;

public interface NotesDateTime extends NotesBase {

  /**
   * Returns this date as a Date.
   *
   * @return a Date
   * @throws RepositoryException
   */
  Date toJavaDate() throws RepositoryException;

  /**
   * Sets the value of this NotesDateTime object to the current
   * date and time.
   *
   * @throws RepositoryException
   */
  void setNow() throws RepositoryException;

  /**
   * Returns the difference between this NotesDateTime and
   * another, in seconds.
   *
   * @param dateTime a NotesDateTime
   * @return the time difference
   * @throws RepositoryException
   */
  int timeDifference(NotesDateTime dateTime) throws RepositoryException;
}
