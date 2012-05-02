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

import com.google.common.base.Strings;
import com.google.enterprise.connector.notes.client.NotesDatabase;
import com.google.enterprise.connector.notes.client.NotesDocument;
import com.google.enterprise.connector.notes.client.NotesSession;
import com.google.enterprise.connector.notes.client.NotesThread;
import com.google.enterprise.connector.notes.client.NotesView;
import com.google.enterprise.connector.notes.client.NotesViewEntry;
import com.google.enterprise.connector.notes.client.NotesViewNavigator;
import com.google.enterprise.connector.spi.AuthenticationIdentity;
import com.google.enterprise.connector.spi.AuthenticationManager;
import com.google.enterprise.connector.spi.AuthenticationResponse;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

class NotesAuthenticationManager implements AuthenticationManager {
  private static final String CLASS_NAME =
      NotesAuthenticationManager.class.getName();
  private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

  private NotesConnectorSession ncs = null;
  private NotesSession nSession = null;
  private NotesDatabase namesDb = null;
  private NotesDatabase acDb = null;
  private NotesView usersVw = null;
  private NotesView peopleVw = null;
  private NotesDocument authDoc = null;
  private NotesDocument personDoc = null;

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
      if (null != peopleVw) {
        peopleVw.recycle();
      }
      if (null != acDb) {
        acDb.recycle();
      }
      if (null != namesDb) {
        namesDb.recycle();
      }
    }
    catch (Exception e) {
      LOGGER.log(Level.WARNING, CLASS_NAME, e);
    }
  }

  /* @Override */
  @SuppressWarnings("unchecked")
  public AuthenticationResponse authenticate(AuthenticationIdentity id) {
    final String METHOD = "authenticate";

    try {
      String pvi = id.getUsername();
      LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
          "Authenticating user " + pvi);

      nSession = ncs.createNotesSession();

      // TODO: Check what we need to support here
      namesDb = nSession.getDatabase(ncs.getServer(), ncs.getDirectory());
      NotesDatabase acDb = nSession.getDatabase(ncs.getServer(),
          ncs.getDatabase());

      String notesName;
      Vector groups;
      synchronized(ncs.getConnector().getPeopleCacheLock()) {
        peopleVw = acDb.getView(NCCONST.VIEWPEOPLECACHE);
        peopleVw.refresh();
        // Resolve the PVI to their Notes names and groups
        personDoc = peopleVw.getDocumentByKey(pvi, true);
        if (null != personDoc) {
          notesName = personDoc.getItemValueString(NCCONST.PCITM_NOTESNAME)
              .toLowerCase();
          groups = personDoc.getItemValue(NCCONST.PCITM_GROUPS);
          groups.addAll(NotesUserGroupManager.getRolesForGroups(
              ncs, acDb, groups));
        } else {
          LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
              "Person not found in connector user database " + pvi);
          return new AuthenticationResponse(false, null);
        }
      }
      LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
          "Authentication user using Notes name " + notesName);
      // Verify that the notesName (mapped pvi->notesName) is
      // really in Notes right now.
      usersVw = namesDb.getView("($Users)");
      authDoc = usersVw.getDocumentByKey(notesName, true);
      if (null == authDoc) {
        LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
            "Username not found in Notes");
        return new AuthenticationResponse(false, null);
      }

      Collection<String> prefixedGroups = null;
      if (groups.size() > 0) {
        prefixedGroups = NotesUserGroupManager.getGsaGroups(ncs, groups);
      }
      String idLog = getIdentityLog(pvi, notesName, groups, prefixedGroups);
      if (null != id.getPassword()) {
        String hashedPassword = authDoc.getItemValueString("HTTPPassword");
        if (nSession.verifyPassword(id.getPassword(), hashedPassword)) {
          LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
              "User succesfully authenticated: " + idLog);
          return new AuthenticationResponse(true, null, prefixedGroups);
        } else {
          LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
              "User failed authentication: " + idLog);
          return new AuthenticationResponse(false, null, prefixedGroups);
        }
      } else {
        LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
            "No password; returning groups only: " + idLog);
        // Although we don't actually know that the entity that
        // submitted this username has a valid password, we have
        // to return true because the GSA will refute the
        // identity otherwise. This situation occurs when the GSA
        // uses another authentication mechanism and uses the
        // connector for group resolution only.
        return new AuthenticationResponse(true, null, prefixedGroups);
      }
    } catch (Exception e) {
      // TODO: what kinds of Notes exceptions can be caught here?
      // Should we rethrow an exception (RepositoryException?)
      // rather than just returning false?
      LOGGER.log(Level.SEVERE, CLASS_NAME, e);
    } finally {
      recycleDominoObjects();
      // TODO: Is it ok to close the session here? Is there a
      // reason it wasn't closed?
      ncs.closeNotesSession(nSession);
    }
    return new AuthenticationResponse(false, null);
  }

  private String getIdentityLog(String pvi, String notesName,
      Vector groups, Collection<String> prefixedGroups) {
    return "pvi: " + pvi + "; Notes name: " + notesName
        + "; groups: " + groups + "; groups sent: " + prefixedGroups;
  }
}
