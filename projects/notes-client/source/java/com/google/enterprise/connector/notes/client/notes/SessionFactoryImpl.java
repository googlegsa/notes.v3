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

import com.google.enterprise.connector.notes.client.NotesSession;
import com.google.enterprise.connector.notes.client.NotesThread;
import com.google.enterprise.connector.notes.client.SessionFactory;

import lotus.domino.NotesException;
import lotus.domino.NotesFactory;

public class SessionFactoryImpl implements SessionFactory {

  public SessionFactoryImpl() {
  }

  /** {@inheritDoc} */
  @Override
  public NotesSession createSessionWithFullAccess(String password)
      throws NotesConnectorExceptionImpl {
    try {
      return new NotesSessionImpl(
          NotesFactory.createSessionWithFullAccess(password));
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public NotesThread getNotesThread() {
    return new NotesThreadImpl();
  }
}
