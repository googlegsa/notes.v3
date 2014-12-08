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

import com.google.enterprise.connector.notes.client.NotesDateTime;
import com.google.enterprise.connector.spi.RepositoryException;

import lotus.domino.DateTime;
import lotus.domino.NotesException;

import java.util.Date;

class NotesDateTimeImpl extends NotesBaseImpl<DateTime>
    implements NotesDateTime {

  NotesDateTimeImpl(DateTime dateTime) {
    super(dateTime);
  }

  /** {@inheritDoc} */
  @Override
  public void adjustSecond(int seconds) throws RepositoryException {
    try {
      getNotesObject().adjustSecond(seconds);
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public Date toJavaDate() throws NotesConnectorExceptionImpl {
    try {
      return getNotesObject().toJavaDate();
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void setNow() throws NotesConnectorExceptionImpl {
    try {
      getNotesObject().setNow();
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }
  
  /** {@inheritDoc} */
  @Override
  public void setAnyTime() throws NotesConnectorExceptionImpl {
    try {
      getNotesObject().setAnyTime();
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public int timeDifference(NotesDateTime otherDateTime)
      throws NotesConnectorExceptionImpl {
    try {
      return getNotesObject().timeDifference(
          ((NotesDateTimeImpl) otherDateTime).getNotesObject());
    } catch (NotesException e) {
      throw new NotesConnectorExceptionImpl(e);
    }
  }
}
