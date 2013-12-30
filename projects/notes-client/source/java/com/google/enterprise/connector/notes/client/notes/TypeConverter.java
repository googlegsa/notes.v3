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

package com.google.enterprise.connector.notes.client.notes;

import lotus.domino.DateRange;
import lotus.domino.DateTime;
import lotus.domino.Item;

import java.util.Vector;

/**
 * Utility to convert Notes objects to client interface objects,
 * or the other way around.
 */
class TypeConverter {

  @SuppressWarnings("unchecked")
  static Vector toConnectorValues(Vector notesValues)
      throws NotesConnectorExceptionImpl {
    if (notesValues == null) {
      return null;
    }
    Vector connectorValues = new Vector(notesValues.size());
    for (Object notesValue : notesValues) {
      if (notesValue instanceof Vector) {
        connectorValues.add(toConnectorValues((Vector) notesValue));
      } else if (notesValue instanceof DateTime) {
        connectorValues.add(new NotesDateTimeImpl((DateTime) notesValue));
      } else if (notesValue instanceof DateRange) {
        connectorValues.add(new NotesDateRangeImpl((DateRange) notesValue));
      } else if (notesValue instanceof String) {
        connectorValues.add(notesValue);
      } else if (notesValue instanceof Double) {
        connectorValues.add(notesValue);
      } else {
        throw new NotesConnectorExceptionImpl("Unexpected notes data type: " +
            notesValue.getClass().getName());
      }
    }
    return connectorValues;
  }

  @SuppressWarnings("unchecked")
  static Vector toConnectorItems(Vector notesItems) {
    if (notesItems == null) {
      return null;
    }
    Vector connectorItems = new Vector(notesItems.size());
    for (Object item : notesItems) {
      connectorItems.add(new NotesItemImpl((Item) item));
    }
    return connectorItems;
  }

  // List types even when they don't need a conversion to check
  // for any unexpected argument types.
  @SuppressWarnings("unchecked")
  static Object toNotesItemValue(Object connectorObject)
      throws NotesConnectorExceptionImpl {
    if (connectorObject == null) {
      return null;
    }
    if (connectorObject instanceof String) {
      return connectorObject;
    }
    if (connectorObject instanceof Integer) {
      return connectorObject;
    }
    if (connectorObject instanceof Double) {
      return connectorObject;
    }
    if (connectorObject instanceof NotesDateTimeImpl) {
      return ((NotesDateTimeImpl) connectorObject).getNotesObject();
    }
    if (connectorObject instanceof NotesDateRangeImpl) {
      return ((NotesDateRangeImpl) connectorObject).getNotesObject();
    }
    if (connectorObject instanceof NotesItemImpl) {
      return ((NotesItemImpl) connectorObject).getNotesObject();
    }
    if (connectorObject instanceof NotesViewEntryImpl) {
      return ((NotesViewEntryImpl) connectorObject).getNotesObject();
    }
    if (connectorObject instanceof Vector) {
      Vector connectorObjects = (Vector) connectorObject;
      Vector notesObjects = new Vector(connectorObjects.size());
      for (Object conObj : connectorObjects) {
        notesObjects.add(toNotesItemValue(conObj));
      }
      return notesObjects;
    }
    throw new NotesConnectorExceptionImpl(
        "Unexpected connector object to be converted to a Notes object: " +
        connectorObject.getClass().getName());
  }

  /**
   * Private constructor to prevent instantiation.
   */
  private TypeConverter() {
  }
}
