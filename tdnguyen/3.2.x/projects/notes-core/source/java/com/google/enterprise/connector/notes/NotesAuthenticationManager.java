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
import com.google.enterprise.connector.notes.NotesUserGroupManager.User;
import com.google.enterprise.connector.spi.AuthenticationIdentity;
import com.google.enterprise.connector.spi.AuthenticationManager;
import com.google.enterprise.connector.spi.AuthenticationResponse;
import com.google.enterprise.connector.spi.Principal;
import com.google.enterprise.connector.spi.SpiConstants.CaseSensitivityType;
import com.google.enterprise.connector.spi.SpiConstants.PrincipalType;

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

  private NotesConnectorSession connectorSession;

  public NotesAuthenticationManager(NotesConnectorSession connectorSession) {
    final String METHOD = "<init>";
    LOGGER.entering(CLASS_NAME, METHOD);
    this.connectorSession = connectorSession;
    LOGGER.exiting(CLASS_NAME, METHOD);
  }

  @Override
  @SuppressWarnings("unchecked")
  public AuthenticationResponse authenticate(AuthenticationIdentity id) {
    final String METHOD = "authenticate";
    LOGGER.entering(CLASS_NAME, METHOD);

    try {
      String gsaName = connectorSession.getUsernameType().getUsername(id);
      LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
          "Authenticating user: " + gsaName + " using " +
          connectorSession.getUsernameType() + " username type");

      // Find the user in the connector cache.
      User user =
          connectorSession.getUserGroupManager().getUserByGsaName(gsaName);
      if (user == null) {
        LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
            gsaName + " user is not authenticated");
        return new AuthenticationResponse(false, null);
      }
      LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
          user.getNotesName() + " user is authenticated");

      // Find the user in Notes.
      NotesSession notesSession = connectorSession.createNotesSession();
      NotesDatabase notesDirectory = null;
      NotesView notesUsersView = null;
      NotesDocument notesUserDoc = null;
      boolean hasValidPassword = false;
      try {
        notesDirectory = notesSession.getDatabase(
            connectorSession.getServer(), connectorSession.getDirectory());
        notesUsersView = notesDirectory.getView(NCCONST.DIRVIEW_USERS);
        notesUserDoc =
            notesUsersView.getDocumentByKey(user.getNotesName(), true);
        if (notesUserDoc == null) {
          LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
              "Username not found in Notes directory");
          return new AuthenticationResponse(false, null);
        }
        if (id.getPassword() != null) {
          String hashedPassword =
              notesUserDoc.getItemValueString("HTTPPassword");
          hasValidPassword =
              notesSession.verifyPassword(id.getPassword(), hashedPassword);
        }
      } finally {
        Util.recycle(notesUserDoc);
        Util.recycle(notesUsersView);
        Util.recycle(notesDirectory);
        connectorSession.closeNotesSession(notesSession);
      }

      Collection<String> groupsAndRoles = user.getGroupsAndRoles();
      Collection<String> prefixedGroups = GsaUtil.getGsaGroups(
          groupsAndRoles, connectorSession.getGsaGroupPrefix());
      Collection<Principal> principalGroups = null;
      if (prefixedGroups.size() != 0) {
        principalGroups = new ArrayList<Principal>(prefixedGroups.size());
        for (String group : prefixedGroups) {
          Principal principal = new Principal(PrincipalType.UNQUALIFIED,
              connectorSession.getConnector().getLocalNamespace(),
              group, CaseSensitivityType.EVERYTHING_CASE_INSENSITIVE);
          principalGroups.add(principal);
        }
      }
      String idLog = getIdentityLog(gsaName, user.getNotesName(),
          groupsAndRoles, prefixedGroups);
      if (id.getPassword() != null) {
        if (hasValidPassword) {
          LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
              "User succesfully authenticated: " + idLog);
          return new AuthenticationResponse(true, null, principalGroups);
        } else {
          LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
              "User failed authentication: " + idLog);
          return new AuthenticationResponse(false, null, principalGroups);
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
        LOGGER.fine("principalgroups: " + principalGroups);
        return new AuthenticationResponse(true, null, principalGroups);
      }
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, CLASS_NAME, e);
    } finally {
      LOGGER.exiting(CLASS_NAME, METHOD);
    }
    return new AuthenticationResponse(false, null);
  }

  private String getIdentityLog(String pvi, String notesName,
      Collection<String> groups, Collection<String> prefixedGroups) {
    return "pvi: " + pvi + "; Notes name: " + notesName
        + "; groups: " + groups + "; groups sent: " + prefixedGroups;
  }
}
