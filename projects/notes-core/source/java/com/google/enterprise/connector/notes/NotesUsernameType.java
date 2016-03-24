// Copyright 2013 Google Inc.
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

package com.google.enterprise.connector.notes;

import com.google.enterprise.connector.spi.AuthenticationIdentity;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enum type for Lotus Notes username type configuration.  The configuration
 * for username type must match the name of an enum defined in this class.
 * 
 * @since 3.0.6
 */
enum NotesUsernameType {
  USERNAME("username") {
    @Override
    public String getUsername(AuthenticationIdentity userId) {
      return userId.getUsername();
    }
  },
  NTDOMAIN("domain\\username") {
    @Override
    public String getUsername(AuthenticationIdentity userId) {
      return userId.getDomain() + "\\" + userId.getUsername();
    }
  },
  INTERNET("username@domain") {
    @Override
    public String getUsername(AuthenticationIdentity userId) {
      return userId.getUsername() + "@" + userId.getDomain();
    }
  };

  private static final Logger LOGGER =
      Logger.getLogger(NotesUsernameType.class.getName());

  private final String tag;

  private NotesUsernameType(String tag) {
    this.tag = tag;
  }

  public abstract String getUsername(AuthenticationIdentity userId);

  public static NotesUsernameType findUsernameType(String enumName) {
    try {
      return Enum.valueOf(NotesUsernameType.class, enumName.toUpperCase());
    } catch (IllegalArgumentException e) {
      LOGGER.log(Level.WARNING, "Invalid Notes username type: " + enumName);
      return null;
    }
  }

  @Override
  public String toString() {
    return tag;
  }
}
