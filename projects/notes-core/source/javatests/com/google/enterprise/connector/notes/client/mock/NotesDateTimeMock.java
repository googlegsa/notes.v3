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

package com.google.enterprise.connector.notes.client.mock;

import com.google.enterprise.connector.notes.client.NotesDateTime;
import com.google.enterprise.connector.spi.RepositoryException;

import java.util.Date;
import java.util.logging.Logger;

class NotesDateTimeMock extends NotesBaseMock
    implements NotesDateTime {
  private static final String CLASS_NAME = NotesDateTimeMock.class.getName();

  /** The logger for this class. */
  private static final Logger LOGGER =
      Logger.getLogger(CLASS_NAME);

  NotesDateTimeMock() {
  }

  /** {@inheritDoc} */
  /* @Override */
  public Date toJavaDate() throws RepositoryException {
   LOGGER.entering(CLASS_NAME, "");
   return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public void setNow() throws RepositoryException {
   LOGGER.entering(CLASS_NAME, "setNow");
  }

  /** {@inheritDoc} */
  /* @Override */
  public int timeDifference(NotesDateTime otherDateTime)
      throws RepositoryException {
   LOGGER.entering(CLASS_NAME, "timeDifference");
   return -1;
  }
}
