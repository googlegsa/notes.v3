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

package com.google.enterprise.connector.notes;

import com.google.enterprise.connector.spi.AuthenticationIdentity;
import com.google.enterprise.connector.spi.AuthenticationManager;
import com.google.enterprise.connector.spi.AuthenticationResponse;
import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.NotesFactory;
import lotus.domino.NotesThread;
import lotus.domino.Session;
import lotus.domino.View;

import java.util.logging.Level;
import java.util.logging.Logger;


class NotesAuthenticationManager implements AuthenticationManager {
  private static final String CLASS_NAME =
      NotesAuthenticationManager.class.getName();
  private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

  private NotesConnectorSession ncs = null;
  Session nSession = null;
  Database namesDb = null;
  Database acDb = null;
  View usersVw = null;
  View peopleVw = null;
  Document authDoc = null;
  Document personDoc = null;

  public NotesAuthenticationManager(NotesConnectorSession session) {
    final String METHOD = "NotesAuthenticationManager";
    LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
        "NotesAuthenticationManager being created.");

    ncs = session;
  }

  public void recycleDominoObjects() {
    try {
      if (null != personDoc) {
        personDoc.recycle();
      }
      if (null != authDoc) {
        authDoc.recycle();
      }
      if (null != usersVw) {
        usersVw.recycle();
      }
      if (null!= peopleVw) {
        peopleVw.recycle();
      }
      if (null != acDb) {
        acDb.recycle();
      }
      if (null!= namesDb) {
        namesDb.recycle();
      }
      if (nSession!= null) {
        nSession.recycle();
      }
    }
    catch (Exception e) {
      LOGGER.log(Level.WARNING, CLASS_NAME, e);
    }
  }

  /* @Override */
  public AuthenticationResponse authenticate(AuthenticationIdentity id) {
    final String METHOD = "authenticate";

    try {
      String pvi = id.getUsername();
      LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
          "Authenticating user " + pvi);
      NotesThread.sinitThread();

      nSession = NotesFactory.createSessionWithFullAccess(ncs.getPassword());

      // TODO: Check what we need to support here
      namesDb = nSession.getDatabase(ncs.getServer(), "names.nsf");

      acDb = nSession.getDatabase(null, null);
      LOGGER.logp(Level.FINER, CLASS_NAME, METHOD, "Opening ACL database " +
          ncs.getServer() + " : " + ncs.getACLDbReplicaId());
      acDb.openByReplicaID(ncs.getServer(), ncs.getACLDbReplicaId());

      peopleVw = acDb.getView(NCCONST.VIEWACPEOPLE);

      // Resolve the PVI to their Notes names and groups
      personDoc = peopleVw.getDocumentByKey(pvi, true);
      if (null == personDoc) {
        LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
            "Person not found in ACL database " + pvi);
        return new AuthenticationResponse(false, null);
      }
      String NotesName = personDoc.getItemValueString(NCCONST.ACITM_NOTESNAME)
          .toLowerCase();
      LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
          "Authentication user using Notes name " + NotesName);
      usersVw = namesDb.getView("($Users)");

      // Resolve the PVI to their Notes names and groups
      authDoc = usersVw.getDocumentByKey(NotesName, true);
      if (null == authDoc) {
        return new AuthenticationResponse(false, null);
      }
      String hashedPassword = authDoc.getItemValueString("HTTPPassword");
      if (nSession.verifyPassword(id.getPassword(), hashedPassword)) {
        LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
            "User succesfully authenticated " + NotesName);
        return new AuthenticationResponse(true, null);
      }
      LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
          "User failed authentication " + NotesName);
    } catch (Exception e) {
      // TODO: what kinds of Notes exceptions can be caught here?
      // Should we rethrow an exception (RepositoryException?)
      // rather than just returning false?
      LOGGER.log(Level.SEVERE, CLASS_NAME, e);
    } finally {
      recycleDominoObjects();
      NotesThread.stermThread();
    }
    return new AuthenticationResponse(false, null);
  }
}
