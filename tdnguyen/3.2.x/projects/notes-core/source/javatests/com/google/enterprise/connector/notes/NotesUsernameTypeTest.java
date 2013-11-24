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

import junit.framework.TestCase;

public class NotesUsernameTypeTest extends TestCase {
  private AuthenticationIdentity userId;

  public NotesUsernameTypeTest() {
    super();
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    userId = getUserId();
  }

  public void testUsernameType() {
    NotesUsernameType usernameType =
        NotesUsernameType.findUsernameType("username");
    assertEquals(usernameType.toString(), "username");
    assertEquals(userId.getUsername(), usernameType.getUsername(userId));
  }

  public void testNtDomainUsernameType() {
    NotesUsernameType usernameType =
        NotesUsernameType.findUsernameType("ntdomain");
    assertEquals(usernameType.toString(), "domain\\username");
    assertEquals(userId.getDomain() + "\\" + userId.getUsername(),
        usernameType.getUsername(userId));
  }

  public void testInternetUsernameType() {
    NotesUsernameType usernameType =
        NotesUsernameType.findUsernameType("internet");
    assertEquals(usernameType.toString(), "username@domain");
    assertEquals(userId.getUsername() + "@" + userId.getDomain(),
        usernameType.getUsername(userId));
  }

  public void testInvalidUsernameType() {
    NotesUsernameType usernameType =
        NotesUsernameType.findUsernameType("Invalid Name");
    assertNull(usernameType);
  }

  private AuthenticationIdentity getUserId() {
    return new AuthenticationIdentity() {

      @Override
      public String getDomain() {
        return "internet.com";
      }

      @Override
      public String getPassword() {
        return "secret";
      }

      @Override
      public String getUsername() {
        return "jsmith";
      }
    };
  }
}
