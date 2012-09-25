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

package com.google.enterprise.connector.notes.client.mock;

import java.util.Vector;
import java.util.logging.Logger;

import com.google.enterprise.connector.notes.client.NotesName;
import com.google.enterprise.connector.spi.RepositoryException;

public class NotesNameMock extends NotesBaseMock implements NotesName {
  private static final String CLASS_NAME = NotesNameMock.class.getName();

  /** The logger for this class. */
  private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

  private String canonicalName;
  private String shortName;
  private String abbreviatedName;
  private String commonName;
  private String flatName;
  private Vector<String> OUs;
  private String organization;

  public NotesNameMock(String canonicalName) throws RepositoryException {
    this.canonicalName = canonicalName;
    initNames();
  }
  
  public NotesNameMock(String canonicalName, String shortName) 
          throws RepositoryException {
    this.canonicalName = canonicalName;
    this.shortName = shortName;
    initNames();
  }

  /** {@inheritDoc} */
  public String getCanonical() {
    return this.canonicalName;
  }
  
  public String getShortName() {
    return this.shortName;
  }
  
  public String getAbbreviated() {
    return this.abbreviatedName;
  }
  
  public String getCommonName() {
    return this.commonName;
  }
  
  public String getFlatName() {
    return this.flatName;
  }

  private void initNames() throws RepositoryException {
    OUs = new Vector<String>(10);
    String[] ary = canonicalName.split("/");
    for (int i = 0; i < ary.length; i++) {
      String[] pair = ary[i].split("=");
      if (pair.length != 2)
        throw new RepositoryException("Invalid Notes name: " + canonicalName);
      if ("CN".equalsIgnoreCase(pair[0])) {
        commonName = pair[1].trim();
      } else if ("OU".equalsIgnoreCase(pair[0])) {
        OUs.add(pair[1]);
      } else if ("O".equalsIgnoreCase(pair[0])) {
        organization = pair[1];
      }
    }
    if (commonName == null)
      throw new RepositoryException(
              "Failed to compute common name: " + canonicalName);
    
    initAbbreviateName();
    
    if (shortName == null)
      initShortName();
    
    initFlatName();
  }
  
  private void initAbbreviateName() {
    StringBuilder buf = new StringBuilder();
    buf.append(commonName);
    for (String ou : OUs) {
      buf.append("/").append(ou);
    }
    buf.append("/").append(organization);
    this.abbreviatedName = buf.toString();
  }
  
  private void initShortName() {
    int pos1 = commonName.indexOf(" ");
    if (pos1 != -1) {
      int pos2 = commonName.lastIndexOf(" ");
      shortName = (commonName.substring(0,pos1) + 
              commonName.substring(pos2 + 1)).toLowerCase();
    } else {
      shortName = commonName.toLowerCase();
    }
  }
  
  /*
   * Flat name is Last + " , " + First + " " + Middle
   */
  private void initFlatName() {
    int pos = commonName.lastIndexOf(" ");
    if (pos == -1) {
      this.flatName = commonName;
    } else {
      StringBuilder buf = new StringBuilder();
      buf.append(commonName.substring(pos).trim());
      buf.append(" , ").append(commonName.substring(0, pos).trim());
      this.flatName = buf.toString();
    }
  }
  
  public String toString() {
    return this.canonicalName;
  }
}
