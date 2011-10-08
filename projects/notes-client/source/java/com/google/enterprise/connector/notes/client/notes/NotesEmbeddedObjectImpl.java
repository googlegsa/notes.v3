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

import com.google.enterprise.connector.notes.client.NotesEmbeddedObject;

import lotus.domino.EmbeddedObject;
import lotus.domino.NotesException;

import java.util.Date;

class NotesEmbeddedObjectImpl extends NotesBaseImpl<EmbeddedObject>
    implements NotesEmbeddedObject {

  NotesEmbeddedObjectImpl(EmbeddedObject embeddedObject) {
    super(embeddedObject);
  }

  /** {@inheritDoc} */
  /* @Override */
  public int getType() throws NotesConnectorExceptionImpl {
    try {
      return getNotesObject().getType();
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  /* @Override */
  public int getFileSize() throws NotesConnectorExceptionImpl {
    try {
      return getNotesObject().getFileSize();
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  /* @Override */
  public void extractFile(String path) throws NotesConnectorExceptionImpl {
    try {
      getNotesObject().extractFile(path);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }
}
