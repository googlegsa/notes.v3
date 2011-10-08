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

import com.google.enterprise.connector.notes.client.NotesACLEntry;
import com.google.enterprise.connector.spi.RepositoryException;

import java.util.logging.Logger;

class NotesACLEntryMock extends NotesBaseMock
    implements NotesACLEntry {
  private static final String CLASS_NAME = NotesACLEntryMock.class.getName();

  /** The logger for this class. */
  private static final Logger LOGGER =
      Logger.getLogger(CLASS_NAME);

  NotesACLEntryMock() {
  }

  /** {@inheritDoc} */
  /* @Override */
  public int getUserType() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getUserType");
    return -1;
  }

  /** {@inheritDoc} */
  /* @Override */
  public int getLevel() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getLevel");
    return -1;
  }

  /** {@inheritDoc} */
  /* @Override */
  public boolean isRoleEnabled(String role) throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "isRoleEnabled");
    return true;
  }

  /** {@inheritDoc} */
  /* @Override */
  public String getName() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getName");
    return "ACL Entry Name";
  }
}
