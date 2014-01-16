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

import com.google.enterprise.connector.notes.client.NotesBase;

import lotus.domino.Base;
import lotus.domino.NotesException;

import java.util.Vector;

class NotesBaseImpl<E extends Base> implements NotesBase {

  private final E notesObject;

  NotesBaseImpl(E notesObject) {
    this.notesObject = notesObject;
  }

  E getNotesObject() {
    return notesObject;
  }

  /** {@inheritDoc} */
  @Override
  public void recycle() throws NotesConnectorExceptionImpl {
    try {
      notesObject.recycle();
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  @SuppressWarnings("unchecked")
  public void recycle(Vector objects) throws NotesConnectorExceptionImpl {
    if (objects == null) {
      return;
    }
    try {
      Vector notesObjects = new Vector(objects.size());
      for (Object o : objects) {
        if (!(o instanceof NotesBaseImpl)) {
          continue;
        }
        notesObjects.add(((NotesBaseImpl) o).getNotesObject());
      }
      notesObject.recycle(notesObjects);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  @Override
  public String toString() {
    return notesObject.toString();
  }
}
