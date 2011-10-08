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

import com.google.enterprise.connector.notes.client.NotesACL;
import com.google.enterprise.connector.notes.client.NotesACLEntry;
import com.google.enterprise.connector.spi.RepositoryException;

import java.util.Vector;
import java.util.logging.Logger;

class NotesACLMock extends NotesBaseMock implements NotesACL {
  private static final String CLASS_NAME = NotesACLMock.class.getName();

  /** The logger for this class. */
  private static final Logger LOGGER =
      Logger.getLogger(CLASS_NAME);

  NotesACLMock() {
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesACLEntry getFirstEntry() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getFirstEntry");
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesACLEntry getNextEntry() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getNextEntry");
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public NotesACLEntry getNextEntry(NotesACLEntry previousEntry)
      throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getNextEntry(prev)");
    return null;
  }

  /** {@inheritDoc} */
  /* @Override */
  public Vector getRoles() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getRoles)");
    return null;
  }
}
