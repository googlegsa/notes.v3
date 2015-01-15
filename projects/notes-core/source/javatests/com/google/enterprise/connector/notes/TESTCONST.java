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

package com.google.enterprise.connector.notes;

public class TESTCONST {
  public enum NotesVersion {
    VERSION_8("Release 8.5.3FP6|November 22, 2013"),
    VERSION_9("Release 9.0|March 08, 2013");
  
    private final String versionString;
    
    private NotesVersion(String versionString) {
      this.versionString = versionString;
    }

    @Override
    public String toString() {
      return versionString;
    }
  }

  public static final String NOTES_VERSION = "javatest.notesversion";
  public static final String SERVER_DOMINO = "dominoserver1/ou=mtv/o=us";
  public static final String SERVER_DOMINO_WEB = "dominoserver1";
  public static final String DOMAIN = ".gsa-connectors.com";
  public static final String DBSRC_REPLICAID = "85257608004F5587";
  public static final String TEST_UNID1 = "XXXXXXXXXXXXXXXXXXXXXXXXXXXX0049";

  private TESTCONST() {
    // Not implemented
  }
}
