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

import com.google.enterprise.connector.notes.NCCONST;
import com.google.enterprise.connector.notes.NotesConnectorSession;
import com.google.enterprise.connector.notes.client.NotesItem;
import com.google.enterprise.connector.spi.RepositoryException;

public class NotesMockUtil {

  public static NotesItemMock createNotesItemText(String name, String value) {
    return new NotesItemMock(
        "name", name, "type", NotesItem.TEXT, "values", value);
  }
  
  public static NotesItemMock createNotesItemNumber(String name, Object value) {
    return new NotesItemMock(
        "name", name, "type", NotesItem.TEXT, "values", value);
  }

  public static NotesItemMock createNotesItemMultiValues(
      String name, String...values) {
    return new NotesItemMock(
        "name", name, "type", NotesItem.TEXT, "values", values);
  }

  public static NotesItemMock createNotesItemMultiValues(
      String name, Vector<String> values) {
    return createNotesItemMultiValues(
        name, values.toArray(new String[values.size()]));
  }
  
  public static void createUser(String canonicalName, String shortName, 
      String httpPassword, NotesDatabaseMock nab, NotesConnectorSession ncs) 
          throws RepositoryException {
    NotesDocumentMock doc = 
        createPersonDocument(canonicalName, shortName, httpPassword);
    doc.addItem(
        createNotesItemText("evaluate_" + ncs.getUserNameFormula(), shortName));
    doc.addItem(createNotesItemNumber(
        "evaluate_" + ncs.getUserSelectionFormula(), 1.0));
    nab.addDocument(doc, NCCONST.DIRVIEW_PEOPLEGROUPFLAT, 
        NCCONST.DIRVIEW_USERS, NCCONST.DIRVIEW_VIMUSERS, 
        NCCONST.DIRVIEW_SERVERACCESS);
  }
  
  public static NotesDocumentMock createPersonDocument(String canonicalName, 
      String shortName, String httpPassword) throws RepositoryException {
    NotesNameMock notesName = new NotesNameMock(canonicalName, shortName);
    NotesDocumentMock doc = new NotesDocumentMock();
    doc.addItem(createNotesItemText("Form", NCCONST.DIRFORM_PERSON));
    doc.addItem(createNotesItemText("ShortName", shortName));
    doc.addItem(createNotesItemText("HTTPPassword", httpPassword));
    doc.addItem(createNotesItemMultiValues(
        "FullName", canonicalName, shortName));
    doc.addItem(createNotesItemText("CanonicalName", canonicalName));
    doc.addItem(createNotesItemText("CommonName", notesName.getCommonName()));
    doc.addItem(createNotesItemText(
        "AbbreviatedName", notesName.getAbbreviated()));
    doc.addItem(createNotesItemText("FlatName", notesName.getFlatName()));
    
    return doc;
  }
  
  public static NotesDocumentMock createGroupDocument(
      String name, String...members) throws RepositoryException {
    NotesDocumentMock doc = new NotesDocumentMock();
    doc.addItem(createNotesItemText(NCCONST.ITMFORM, NCCONST.DIRFORM_GROUP));
    doc.addItem(createNotesItemText(NCCONST.GITM_LISTNAME, name));
    doc.addItem(createNotesItemText(NCCONST.GITM_GROUPTYPE, 
        NCCONST.DIR_ACCESSCONTROLGROUPTYPES));
    doc.addItem(createNotesItemMultiValues(NCCONST.GITM_MEMBERS, members));
    doc.addItem(createNotesItemText("FlatName", name));
    
    return doc;
  }
  
  private NotesMockUtil() {
    //Prevent initialization
  }
}
