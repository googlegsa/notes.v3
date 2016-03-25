// Copyright 2011 Google Inc.
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

package com.google.enterprise.connector.notes.client.mock;

import com.google.enterprise.connector.notes.client.NotesACLEntry;
import com.google.enterprise.connector.notes.client.NotesName;
import com.google.enterprise.connector.spi.RepositoryException;

import java.util.Arrays;
import java.util.Vector;
import java.util.logging.Logger;

public class NotesACLEntryMock extends NotesBaseMock
    implements NotesACLEntry {
  private static final String CLASS_NAME = NotesACLEntryMock.class.getName();

  /** The logger for this class. */
  private static final Logger LOGGER =
      Logger.getLogger(CLASS_NAME);

  private final String name;
  private final int userType;
  private final int level;
  private final Vector<String> roles;

  public NotesACLEntryMock(String name, int userType, int level,
      String... roles) {
    LOGGER.fine("creating acl entry for " + name
        + " with roles " + Arrays.asList(roles));

    this.name = name;
    this.userType = userType;
    this.level = level;
    if (roles != null) {
      this.roles = new Vector<String>(Arrays.asList(roles));
    } else {
      this.roles = new Vector<String>();
    }
  }

  /** {@inheritDoc} */
  @Override
  public int getUserType() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getUserType");
    return userType;
  }

  /** {@inheritDoc} */
  @Override
  public int getLevel() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getLevel");
    return level;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isRoleEnabled(String role) throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "isRoleEnabled");
    return true;
  }

  /** {@inheritDoc} */
  @Override
  public String getName() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getName");
    return name;
  }

  /** {@inheritDoc} */
  @Override
  public NotesName getNameObject() throws RepositoryException {
    return new NotesNameMock(name);
  }

  /** {@inheritDoc} */
  @Override
  public Vector getRoles() throws RepositoryException {
    LOGGER.entering(CLASS_NAME, "getRoles");
    return roles;
  }

  @Override
  public String toString() {
    try {
      return getName();
    } catch (RepositoryException e) {
      return "";
    }
  }
}
