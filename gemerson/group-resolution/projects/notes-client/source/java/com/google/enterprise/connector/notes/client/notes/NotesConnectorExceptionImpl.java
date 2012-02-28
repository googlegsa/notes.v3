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

import com.google.enterprise.connector.notes.NotesConnectorException;

import lotus.domino.NotesException;

class NotesConnectorExceptionImpl extends NotesConnectorException {
  public NotesConnectorExceptionImpl(String message) {
    super(message);
  }

  public NotesConnectorExceptionImpl(Throwable rootCause) {
    super(rootCause);
  }

  public NotesConnectorExceptionImpl(String message,
      NotesException rootCause) {
    super(message, rootCause);
    id = rootCause.id;
  }

  public NotesConnectorExceptionImpl(NotesException rootCause) {
    super(rootCause);
    id = rootCause.id;
  }
}
