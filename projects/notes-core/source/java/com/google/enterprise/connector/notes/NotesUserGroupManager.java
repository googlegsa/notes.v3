// Copyright (C) 2011 Google Inc.
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.enterprise.connector.notes.client.NotesDatabase;
import com.google.enterprise.connector.notes.client.NotesDateTime;
import com.google.enterprise.connector.notes.client.NotesDocument;
import com.google.enterprise.connector.notes.client.NotesDocumentCollection;
import com.google.enterprise.connector.notes.client.NotesEmbeddedObject;
import com.google.enterprise.connector.notes.client.NotesItem;
import com.google.enterprise.connector.notes.client.NotesRichTextItem;
import com.google.enterprise.connector.notes.client.NotesSession;
import com.google.enterprise.connector.notes.client.NotesView;
import com.google.enterprise.connector.notes.client.NotesViewEntry;
import com.google.enterprise.connector.notes.client.NotesViewNavigator;
import com.google.enterprise.connector.spi.RepositoryException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/* Synchronization blocks in this class are intended to take
 * advantage of the current implementation strategy in which
 * there's a single instance of this class in a single
 * thread. This class is the only part of the connector
 * performing update/deletion operations on the user and group
 * cache. The authn and authz threads may read from the user and
 * group cache, but they don't update it. This class only locks
 * the database changes, not reads, because nothing else should
 * be changing the database.
 */
public class NotesUserGroupManager {
  private static final String CLASS_NAME = NotesUserGroupManager.class
      .getName();
  private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

  static Collection<String> getGsaUsers(
      NotesConnectorSession notesConnectorSession,
      NotesDatabase connectorDatabase,
      Collection notesUsers) throws RepositoryException {
    return getGsaUsers(notesConnectorSession, connectorDatabase,
        notesUsers, false);
  }

  /**
   * Returns a collection of distinct GSA usernames. Each name in
   * notesUsers is checked against the connector's user cache. If
   * found, the corresponding GSA username is returned. If the
   * removeUsers parameter is true, the Notes name is removed
   * from notesUsers to allow the caller to process the remaining
   * items as non-user data. For example, document Reader names
   * are not marked in a way that lets us distinguish between
   * users and groups, so we can use this to help when
   * construting ACLs.
   */
  static Collection<String> getGsaUsers(
      NotesConnectorSession notesConnectorSession,
      NotesDatabase connectorDatabase, Collection<?> notesUsers,
      boolean removeUsers) throws RepositoryException {
    final String METHOD = "getGsaUsers";
    LinkedHashSet<String> gsaUsers =
        new LinkedHashSet<String>(notesUsers.size());
    List<Object> verifiedUsers = new ArrayList<Object>();
    NotesView people = null;
    try {
      synchronized(notesConnectorSession.getConnector().getPeopleCacheLock()) {
        people = connectorDatabase.getView(NCCONST.VIEWNOTESNAMELOOKUP);
        people.refresh();
        for (Object userObj : notesUsers) {
          //  Notes names are cached in lower case.
          String user = userObj.toString().toLowerCase();
          NotesDocument personDoc = null;
          try {
            personDoc = findPersonDocument(people, user);
            if (personDoc == null) {
              continue;
            }
            verifiedUsers.add(userObj);
            String pvi = personDoc.getItemValueString(NCCONST.PCITM_USERNAME)
                .toLowerCase();
            if (LOGGER.isLoggable(Level.FINEST)) {
              LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
                  "GSA mapping: " + user + " = " + pvi);
            }
            gsaUsers.add(pvi);
          } finally {
            if (personDoc != null) {
              personDoc.recycle();
            }
          }
        }
      }
    } finally {
      if (people != null) {
        people.recycle();
      }
    }
    if (removeUsers) {
      notesUsers.removeAll(verifiedUsers);
    }
    return gsaUsers;
  }

  /* It seems as if we might see groups here that aren't
   * otherwise known to the connector, so just accept all names
   * passed in and convert them to GSA format.
   */
  static Collection<String> getGsaGroups(
      NotesConnectorSession notesConnectorSession,
      Collection notesGroups) throws RepositoryException {
    final String METHOD = "getGsaGroups";
    LinkedHashSet<String> gsaGroups =
        new LinkedHashSet<String>(notesGroups.size());
    // Prefix group names with the configured prefix.
    String groupPrefix = notesConnectorSession.getGsaGroupPrefix();
    if (LOGGER.isLoggable(Level.FINEST)) {
      LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
          "Using group prefix '" + groupPrefix + "'");
    }
    // Allow no prefix.
    if (Strings.isNullOrEmpty(groupPrefix)) {
      groupPrefix = "";
    } else if (!groupPrefix.endsWith("/")) {
      groupPrefix += "/";
    }
    for (Object groupObj : notesGroups) {
      String group = groupObj.toString();
      try {
        gsaGroups.add(
            URLEncoder.encode(groupPrefix + group.toLowerCase(), "UTF-8"));
      } catch (UnsupportedEncodingException e) {
        throw new AssertionError(e);
      }
    }
    return gsaGroups;
  }

  @VisibleForTesting
  static NotesDocument findPersonDocument(NotesView people, String user)
      throws RepositoryException {
    final String METHOD = "findPersonDocument";
    NotesDocument personDoc = null;
    if (user.startsWith("cn=")) {
      personDoc = people.getDocumentByKey(user, true);
      if (personDoc == null) {
        if (LOGGER.isLoggable(Level.FINEST)) {
          LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
              "Person not found in user cache: " + user);
        }
      }
    } else {
      String userLookup = "cn=" + user + "/";
      personDoc = people.getDocumentByKey(userLookup, false);
      if (personDoc == null) {
        if (LOGGER.isLoggable(Level.FINEST)) {
          LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
              "Person not found in user cache: " + userLookup);
        }
      }
    }
    return personDoc;
  }

  /**
   * Replaces all groups with the given replicaid prefix with the
   * current list given in the roles parameter.
   */
  @SuppressWarnings("unchecked")
  static void replaceRoleGroupsForUser(
      NotesConnectorSession notesConnectorSession,
      NotesDatabase connectorDatabase, String user, String replicaId,
      Collection roles) throws RepositoryException {
    final String METHOD = "replaceRoleGroupsForUser";
    if (roles == null) {
      roles = new Vector<String>();
    }
    synchronized(notesConnectorSession.getConnector().getPeopleCacheLock()) {
      NotesView people = null;
      NotesDocument personDoc = null;
      try {
        people = connectorDatabase.getView(NCCONST.VIEWNOTESNAMELOOKUP);
        people.refresh();
        personDoc = findPersonDocument(people, user);
        if (personDoc == null) {
          return;
        }
        Vector currentGroups = personDoc.getItemValue(NCCONST.PCITM_GROUPS);
        // Remove previous role entries for this database.
        ArrayList<String> tmpRoles = new ArrayList<String>();
        for (Object groupObj : currentGroups) {
          String group = groupObj.toString();
          if (group.startsWith(replicaId)) {
            tmpRoles.add(group);
          }
        }
        currentGroups.removeAll(tmpRoles);
        tmpRoles.clear();

        // Prefix roles with replica id and add to existing groups.
        for (Object role : roles) {
          tmpRoles.add(replicaId + "/" + role.toString());
        }
        currentGroups.addAll(tmpRoles);
        if (LOGGER.isLoggable(Level.FINEST)) {
          LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
              "Updating groups for " + user + " to " + currentGroups);
        }
        personDoc.replaceItemValue(NCCONST.PCITM_GROUPS, currentGroups);
        personDoc.save();
      } finally {
        if (personDoc != null) {
          personDoc.recycle();
        }
        if (people != null) {
          people.recycle();
        }
      }
    }
  }

  /**
   * Replaces the values in the GCITM_GROUPROLES field with
   * the given replicaId prefix with the values in the roles
   * parameter.
   */
  @SuppressWarnings("unchecked")
  static void replaceRoleGroupsForGroup(
      NotesConnectorSession notesConnectorSession,
      NotesDatabase connectorDatabase, String group, String replicaId,
      Collection roles) throws RepositoryException {
    final String METHOD = "replaceRoleGroupsForGroup";
    if (roles == null) {
      roles = new Vector<String>();
    }
    synchronized(notesConnectorSession.getConnector().getPeopleCacheLock()) {
      NotesView groups = null;
      NotesDocument groupDoc = null;
      try {
        groups = connectorDatabase.getView(NCCONST.VIEWGROUPCACHE);
        groups.refresh();
        groupDoc = groups.getDocumentByKey(group, true);
        if (groupDoc == null) {
          if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
                "Didn't find group in group cache; creating " + group);
          }
          groupDoc = connectorDatabase.createDocument();
          groupDoc.replaceItemValue(NCCONST.ITMFORM, NCCONST.DIRFORM_GROUP);
          groupDoc.replaceItemValue(NCCONST.GCITM_GROUPNAME, group);
        }
        Vector currentRoles = groupDoc.getItemValue(NCCONST.GCITM_GROUPROLES);
        // Remove previous role entries for this database.
        ArrayList<String> tmpRoles = new ArrayList<String>();
        for (Object roleObj : currentRoles) {
          String role = roleObj.toString();
          if (role.startsWith(replicaId)) {
            tmpRoles.add(role);
          }
        }
        currentRoles.removeAll(tmpRoles);
        tmpRoles.clear();

        // Prefix roles with replica id and add to existing groups.
        for (Object role : roles) {
          tmpRoles.add(replicaId + "/" + role.toString());
        }
        currentRoles.addAll(tmpRoles);
        if (LOGGER.isLoggable(Level.FINEST)) {
          LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
              "Updating roles for " + replicaId + "/" + group
              + " to " + currentRoles);
        }
        groupDoc.replaceItemValue(NCCONST.GCITM_GROUPROLES, currentRoles);
        groupDoc.save();
      } finally {
        if (groupDoc != null) {
          groupDoc.recycle();
        }
        if (groups != null) {
          groups.recycle();
        }
      }
    }
  }

  /**
   * Returns all the roles for the given list of groups in a single list.
   */
  @SuppressWarnings("unchecked")
  static Collection<String> getRolesForGroups(
      NotesConnectorSession notesConnectorSession,
      NotesDatabase connectorDatabase, Collection groups)
      throws RepositoryException {
    final String METHOD = "getRolesForGroups";

    ArrayList<String> roles = new ArrayList<String>();
    synchronized(notesConnectorSession.getConnector().getPeopleCacheLock()) {
      NotesView groupsView = connectorDatabase.getView(NCCONST.VIEWGROUPCACHE);
      if (groupsView == null) {
        LOGGER.logp(Level.WARNING, CLASS_NAME, METHOD,
            "Failed to open group cache view");
        return roles;
      }
      try {
        groupsView.refresh();
        for (Object groupObj : groups) {
          String group = groupObj.toString();
          NotesDocument groupDoc = groupsView.getDocumentByKey(group, true);
          if (groupDoc == null) {
            continue;
          }
          try {
            Vector currentRoles = groupDoc.getItemValue(
                NCCONST.GCITM_GROUPROLES);
            roles.addAll((Vector<String>) currentRoles);
          } finally {
            groupDoc.recycle();
          }
        }
      } finally {
        groupsView.recycle();
      }
    }
    return roles;
  }

  private NotesSession ns = null;
  private NotesDatabase cdb = null;
  private NotesDatabase dirdb = null;
  private Vector<String> processedGroups = null;
  private Vector<String> existingMembers = null;
  private Vector<String> userNestedGroups = null;
  private NotesView vwPG = null;
  private NotesView vwGroupCache = null;
  private NotesView vwPeopleCache = null;
  private NotesView vwServerAccess = null;
  private NotesView vwParentGroups = null;
  private NotesView vwVimUsers = null;
  private NotesView vwVimGroups = null;

  private void cleanUpNotesObjects() {
    final String METHOD = "cleanUpNotesObjects";
    try {
      LOGGER.entering(CLASS_NAME, METHOD);
      if (vwPG != null)
        vwPG.recycle();
      if (vwGroupCache != null)
        vwGroupCache.recycle();
      if (vwPeopleCache != null)
        vwPeopleCache.recycle();
      if (vwServerAccess != null)
        vwServerAccess.recycle();
      if (vwParentGroups != null)
        vwParentGroups.recycle();
      if (vwVimUsers != null)
        vwVimUsers.recycle();
      if (vwVimGroups != null)
        vwVimGroups.recycle();
      if (cdb != null)
        cdb.recycle();
      if (dirdb != null)
        dirdb.recycle();
    } catch (RepositoryException e) {
      LOGGER.log(Level.SEVERE, CLASS_NAME, e);
    }
    LOGGER.exiting(CLASS_NAME, METHOD);
  }

  void updatePeopleGroups(NotesConnectorSession ncs) {
    updatePeopleGroups(ncs, false);
  }

  /**
   * Updates the cached lists of people and groups. When force is
   * true, the configure cache update interval is ignored and the
   * user and group cache is updated.
   *
   * @param ncs the session
   * @param force if true, force an update
   */
  void updatePeopleGroups(NotesConnectorSession ncs, boolean force) {
    final String METHOD = "updatePeopleGroups";
    LOGGER.entering(CLASS_NAME, METHOD);
    LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
        "Starting People Group Cache update.");
    try {
      ns = ncs.createNotesSession();
      cdb = ns.getDatabase(ncs.getServer(), ncs.getDatabase());
      vwPeopleCache = cdb.getView(NCCONST.VIEWPEOPLECACHE);
      vwPeopleCache.refresh();
      // Check our update interval. If the cache is empty, assume
      // we ought to update it.
      if (!force
          && vwPeopleCache.getEntryCount() > 0
          && !checkCachePollInterval(cdb, ncs)) {
        return;
      }

      dirdb = ns.getDatabase(ncs.getServer(), ncs.getDirectory());
      vwPG = dirdb.getView(NCCONST.DIRVIEW_PEOPLEGROUPFLAT);
      vwGroupCache = cdb.getView(NCCONST.VIEWGROUPCACHE);
      vwServerAccess = dirdb.getView(NCCONST.DIRVIEW_SERVERACCESS);
      vwParentGroups = cdb.getView(NCCONST.VIEWPARENTGROUPS);
      vwPG.refresh();
      vwServerAccess.refresh();

      // Pass 1 - Update nested groups
      vwGroupCache.refresh();
      updateGroups(ncs, vwPG, vwGroupCache);

      // Pass 2 - Update people
      vwGroupCache.refresh();
      vwParentGroups.refresh();
      vwPeopleCache.refresh();
      updatePeople(ncs, vwPG, vwPeopleCache, vwServerAccess, vwParentGroups);

      // Pass 3 - Delete any users that no longer exist
      vwVimUsers = dirdb.getView(NCCONST.DIRVIEW_VIMUSERS);
      vwVimUsers.refresh();
      checkDeletions(ncs, vwPeopleCache, vwVimUsers);

      // Pass 4 - Delete any groups that no longer exist
      vwVimGroups = dirdb.getView(NCCONST.DIRVIEW_VIMGROUPS);
      vwVimGroups.refresh();
      checkDeletions(ncs, vwGroupCache, vwVimGroups);
      setLastCacheUpdate();
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, CLASS_NAME, e);
    } finally {
      cleanUpNotesObjects();
      ncs.closeNotesSession(ns);
      LOGGER.exiting(CLASS_NAME, METHOD);
    }
  }

  private void setLastCacheUpdate() {
    final String METHOD = "setLastCacheUpdate";
    LOGGER.entering(CLASS_NAME, METHOD);
    try {
      NotesDateTime now = ns.createDateTime("1/1/1900");
      now.setNow();

      NotesView vw = cdb.getView(NCCONST.VIEWSYSTEMSETUP);
      NotesDocument systemDoc = vw.getFirstDocument();
      if (systemDoc == null) {
        LOGGER.logp(Level.SEVERE, CLASS_NAME, METHOD,
            "System configuration document not found.");
        return;
      }
      systemDoc.replaceItemValue(NCCONST.SITM_LASTCACHEUPDATE, now);
      systemDoc.save(true);
      LOGGER.logp(Level.INFO, CLASS_NAME, METHOD,
          "Directory Cache last update time set to " + now.toString());
      systemDoc.recycle();
      now.recycle();
      vw.recycle();
    } catch (RepositoryException e) {
      LOGGER.log(Level.SEVERE, CLASS_NAME, e);
    }
    LOGGER.exiting(CLASS_NAME, METHOD);
  }

  private boolean checkCachePollInterval(NotesDatabase cdb,
      NotesConnectorSession ncs) throws RepositoryException {
    final String METHOD = "checkPollInterval";
    LOGGER.entering(CLASS_NAME, METHOD);
    boolean needToUpdate = true;

    NotesDateTime lastCacheUpdate = ns.createDateTime("1/1/2010");
    NotesDateTime now = ns.createDateTime("1/1/1900");
    now.setNow();

    NotesView vw = cdb.getView(NCCONST.VIEWSYSTEMSETUP);
    NotesDocument systemDoc = vw.getFirstDocument();
    if (systemDoc == null) {
      LOGGER.logp(Level.SEVERE, CLASS_NAME, METHOD,
          "System configuration document not found.");
      return false;
    }
    // Get the update interval from the system configuration
    int cacheUpdateInterval = ncs.getCacheUpdateInterval();

    Vector<?> vecLastCacheUpdate = systemDoc
        .getItemValue(NCCONST.SITM_LASTCACHEUPDATE);
    if (0 < vecLastCacheUpdate.size()) {
      lastCacheUpdate = (NotesDateTime) vecLastCacheUpdate.firstElement();
      LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
          "Last dirctory cache update time is " + lastCacheUpdate);
    }

    double elapsedMinutes = now.timeDifference(lastCacheUpdate) / 60;
    LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
        "Time difference since last directory cache update is : "
        + elapsedMinutes);

    // Check poll interval
    if (cacheUpdateInterval > elapsedMinutes) {
      LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
          "Directory cache poll interval has not yet elapsed.");
      needToUpdate = false;
    }
    vw.recycle();
    systemDoc.recycle();
    lastCacheUpdate.recycle();
    now.recycle();
    ns.recycle(vecLastCacheUpdate);
    LOGGER.entering(CLASS_NAME, METHOD);
    return needToUpdate;
  }

  private void checkDeletions(NotesConnectorSession ncs, NotesView checkView,
      NotesView vwPG) throws RepositoryException {
    final String METHOD = "checkDeletions";
    String key = null;
    NotesDocument prevDoc = null;
    boolean remove = false;
    boolean selected = false;
    boolean isPerson = false;
    LOGGER.entering(CLASS_NAME, METHOD);

    checkView.refresh();

    LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
        "Checking for directory deletions.  Number of documents to check : "
        + checkView.getEntryCount());
    String userSelectionFormula = ncs.getUserSelectionFormula();
    NotesDocument doc = checkView.getFirstDocument();
    while (doc != null) {
      try {
        remove = false;
        isPerson = false;
        selected = true;
        prevDoc = doc;
        String formName = doc.getItemValueString(NCCONST.ITMFORM);
        if (formName.contentEquals(NCCONST.DIRFORM_GROUP)) {
          key = doc.getItemValueString(NCCONST.GCITM_GROUPNAME);
        }
        if (formName.contentEquals(NCCONST.DIRFORM_PERSON)) {
          isPerson = true;
          String abbrevFormula = String.format("@Name([ABBREVIATE];\"%s\")",
              doc.getItemValueString(NCCONST.PCITM_NOTESNAME));
          key = ns.evaluate(abbrevFormula).elementAt(0).toString();
        }
        NotesDocument searchDoc = vwPG.getDocumentByKey(key);
        LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
            "Directory deletion check for: " + key);
        if (searchDoc == null) {
          // This person or group no longer exists. Remove them
          LOGGER.logp(Level.INFO, CLASS_NAME, METHOD,
              "User/Group no longer exists in source directory "
              + "and will be deleted: " + key);
          remove = true;
        } else {
          // Do an additional check for Persons to make sure they
          // still meet the selection criteria
          if (isPerson) {
            selected = checkPersonSelectionFormula(userSelectionFormula, doc);
          }
          searchDoc.recycle();
        }
        if (!selected) {
          LOGGER.logp(Level.INFO, CLASS_NAME, METHOD,
              "User no longer meets selection criteria and will be deleted: "
              + key);
          remove = true;
        }

        if (remove) {
          synchronized(ncs.getConnector().getPeopleCacheLock()) {
            doc.remove(true);
          }
        }
        doc = checkView.getNextDocument(prevDoc);
        prevDoc.recycle();
      } catch (RepositoryException e) {
        LOGGER.log(Level.SEVERE, CLASS_NAME, e);
        doc = checkView.getNextDocument(prevDoc);
        prevDoc.recycle();
      }
    }
    LOGGER.exiting(CLASS_NAME, METHOD);
  }

  @SuppressWarnings("unchecked")
  private void getNestedGroups(NotesView vwPG, String groupName)
      throws RepositoryException {
    final String METHOD = "getNestedGroups";
    LOGGER.entering(CLASS_NAME, METHOD);

    LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
        "Processing nested groups for " + groupName);
    if (processedGroups.contains(groupName)) {
      LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
          "All ready processed group." + groupName);
      return;
    }
    processedGroups.add(groupName);

    // Get the group document
    NotesDocument groupDoc = vwPG.getDocumentByKey(groupName, true);
    if (groupDoc == null) {
      LOGGER.logp(Level.WARNING, CLASS_NAME, METHOD,
          "Failed to find document for group: " + groupName);
      return;
    }
    Vector<String> grpMembers = groupDoc.getItemValue(NCCONST.GITM_MEMBERS);

    for (String mem : grpMembers) {
      NotesDocument memDoc = vwPG.getDocumentByKey(mem);
      if (memDoc == null) {
        continue;
      }
      if (!memDoc.getItemValueString(NCCONST.ITMFORM).contentEquals(
              NCCONST.DIRFORM_GROUP)) {
        memDoc.recycle();
        continue;
      }
      String groupType = memDoc.getItemValueString(NCCONST.GITM_GROUPTYPE);
      if (!NCCONST.DIR_ACCESSCONTROLGROUPTYPES.contains(groupType)) {
        memDoc.recycle();
        continue;
      }
      existingMembers.add(mem);
      getNestedGroups(vwPG, mem);
    }
    LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD, "Group list is "
        + existingMembers);
    groupDoc.recycle();
    LOGGER.exiting(CLASS_NAME, METHOD);
  }

  private void updateGroups(NotesConnectorSession ncs, NotesView vwPG,
      NotesView vwParentGroups) throws RepositoryException {
    final String METHOD = "updateGroups";
    NotesDocument prevDoc = null;
    NotesDocument groupDoc = null;
    LOGGER.entering(CLASS_NAME, METHOD);

    LOGGER.logp(Level.FINE, CLASS_NAME, METHOD, "Starting group cache update");
    NotesDocument doc = vwPG.getFirstDocument();
    // Pass 1 - Update all the groups and work out nested groups
    while (doc != null) {
      try {
        prevDoc = doc;
        String groupName = doc.getItemValueString(NCCONST.GITM_LISTNAME);
        LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD, "Processing group "
            + groupName);
        // Only process groups
        if (!doc.getItemValueString(NCCONST.ITMFORM).contentEquals(
                NCCONST.DIRFORM_GROUP)) {
          LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
              "This is not a group: '" + groupName + "'");
          doc = vwPG.getNextDocument(prevDoc);
          prevDoc.recycle();
          continue;
        }

        // Only process access control type groups
        String groupType = doc.getItemValueString(NCCONST.GITM_GROUPTYPE);
        if (!NCCONST.DIR_ACCESSCONTROLGROUPTYPES.contains(groupType)) {
          LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
              "This is not an access control group " + groupName);
          doc = vwPG.getNextDocument(prevDoc);
          prevDoc.recycle();
          continue;
        }

        // Find members
        processedGroups = new Vector<String>(100);
        existingMembers = new Vector<String>(100);
        getNestedGroups(vwPG, groupName);

        // Remove duplicates
        LinkedHashSet<String> uniqueMembers = new LinkedHashSet<String>(
            existingMembers);
        existingMembers.clear();
        existingMembers.addAll(uniqueMembers);

        LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
            "Nested group members for '" + groupName + "' are: "
            + existingMembers);
        groupDoc = vwParentGroups.getDocumentByKey(groupName, true);
        synchronized(ncs.getConnector().getPeopleCacheLock()) {
          if (groupDoc == null) {
            LOGGER.logp(Level.INFO, CLASS_NAME, METHOD, "Creating group "
                + groupName);
            groupDoc = cdb.createDocument();
            groupDoc.replaceItemValue(NCCONST.ITMFORM, NCCONST.DIRFORM_GROUP);
            groupDoc.replaceItemValue(NCCONST.GCITM_GROUPNAME, groupName);
          }
          groupDoc.replaceItemValue(NCCONST.GCITM_CHILDGROUPS, existingMembers);
          groupDoc.save(true);
        }
        groupDoc.recycle();
        groupDoc = null;
        doc = vwPG.getNextDocument(prevDoc);
        prevDoc.recycle();
      } catch (RepositoryException e) {
        LOGGER.log(Level.SEVERE, CLASS_NAME, e);
        if (groupDoc != null)
          groupDoc.recycle();
        vwPG.refresh();
        doc = vwPG.getNextDocument(prevDoc);
        prevDoc.recycle();
      }
    }
    LOGGER.exiting(CLASS_NAME, METHOD);
  }

  private String evaluatePVI(String userNameFormula, NotesDocument doc)
      throws RepositoryException {
    Vector<?> vecEvalResult = ns.evaluate(userNameFormula, doc);
    // Make sure we dont' get an empty vector or an empty string
    if (vecEvalResult != null) {
      if (vecEvalResult.size() > 0) {
        return (vecEvalResult.elementAt(0).toString());
      }
    }
    return "";
  }

  private void updatePeople(NotesConnectorSession ncs, NotesView vwPG,
      NotesView vwPeopleCache, NotesView vwServerAccess, NotesView vwGroupCache)
      throws RepositoryException {
    final String METHOD = "updatePeople";
    LOGGER.entering(CLASS_NAME, METHOD);

    NotesDocument prevDoc = null;
    NotesDocument personDoc = null;
    NotesDocument doc = vwPG.getFirstDocument();

    String userSelectionFormula = ncs.getUserSelectionFormula();
    LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
        "User selection formula result is: " + userSelectionFormula);

    String userNameFormula = ncs.getUserNameFormula();
    LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
        "User name formula result is: " + userNameFormula);

    while (doc != null) {
      try {
        prevDoc = doc;

        // Only process people
        if (!doc.getItemValueString(NCCONST.ITMFORM).contentEquals(
                NCCONST.DIRFORM_PERSON)) {
          doc = vwPG.getNextDocument(prevDoc);
          prevDoc.recycle();
          continue;
        }

        String fullName = doc.getItemValue(NCCONST.PITM_FULLNAME)
            .firstElement().toString().toLowerCase();
        LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD, "Processing user: "
            + fullName);

        // Get their PVI
        String pvi = evaluatePVI(userNameFormula, doc);
        if (0 == pvi.length()) {
          LOGGER.logp(Level.WARNING, CLASS_NAME, METHOD,
              "Could not evaluate username for: " + fullName);
          doc = vwPG.getNextDocument(prevDoc);
          prevDoc.recycle();
          continue;
        }
        LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD, "PVI evaluates to: "
            + pvi);

        // Does this person still match the selection formula?
        boolean selected = checkPersonSelectionFormula(userSelectionFormula,
            doc);

        personDoc = vwPeopleCache.getDocumentByKey(pvi);
        // If the person no longer fits the criteria - remove them
        if (!selected && (personDoc != null)) {
          LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
              "Removing user as they no longer fit selection criteria: "
              + fullName);
          doc = vwPG.getNextDocument(prevDoc);
          synchronized(ncs.getConnector().getPeopleCacheLock()) {
            personDoc.remove(true);
          }
          prevDoc.recycle();
          continue;
        }
        if (!selected) {
          LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
              "Skipping user as they do not fit selection criteria: "
              + fullName);
          doc = vwPG.getNextDocument(prevDoc);
          prevDoc.recycle();
          continue;
        }

        // TODO: if it's guaranteed that createDocument and
        // replaceItemValue don't affect the server, then we
        // could further restrict the scope of the synchronized
        // block, but I don't know that.
        synchronized(ncs.getConnector().getPeopleCacheLock()) {
          // This person doesn't exist yet, create them
          if (personDoc == null) {
            LOGGER.logp(Level.INFO, CLASS_NAME, METHOD, "Creating user: "
                + fullName + " using PVI: " + pvi);
            personDoc = cdb.createDocument();
            personDoc.replaceItemValue(NCCONST.ITMFORM, NCCONST.DIRFORM_PERSON);
            personDoc.replaceItemValue(NCCONST.PCITM_USERNAME, pvi);
          }

          // Update their fullname and groups
          LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD, "Updating user: "
              + fullName + " using PVI: " + pvi);
          personDoc.replaceItemValue(NCCONST.PCITM_NOTESNAME,
              doc.getItemValue(NCCONST.PITM_FULLNAME).firstElement().toString()
              .toLowerCase());

          vwServerAccess.refresh();
          userNestedGroups = new Vector<String>(100);
          // Get groups for full name
          getParentGroups(fullName, vwServerAccess, vwGroupCache);

          // Get groups from common name
          getParentGroups(NotesAuthorizationManager.getCommonName(fullName),
              vwServerAccess, vwGroupCache);

          // Get groups from DN hierarchy
          getGroupsFromDN(fullName);

          // Sort and eliminate duplicates
          LinkedHashSet<String> uniqueGroups = new LinkedHashSet<String>(
              userNestedGroups);
          userNestedGroups.clear();
          userNestedGroups.addAll(uniqueGroups);

          LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD, "Group list for user "
              + fullName + " are " + userNestedGroups);
          personDoc.replaceItemValue(NCCONST.PCITM_GROUPS, userNestedGroups);
          personDoc.save(true);
        }
        personDoc.recycle();
        personDoc = null;
        doc = vwPG.getNextDocument(prevDoc);
        prevDoc.recycle();
      } catch (RepositoryException e) {
        LOGGER.log(Level.SEVERE, CLASS_NAME, e);
        if (personDoc != null) {
          personDoc.recycle();
          personDoc = null;
        }
        vwPG.refresh();
        doc = vwPG.getNextDocument(prevDoc);
        prevDoc.recycle();
      }
    }
    LOGGER.exiting(CLASS_NAME, METHOD);
  }

  private void getGroupsFromDN(String dn) {
    final String METHOD = "getGroupsFromDN";
    // TODO: use the Name class to parse the name?
    while (true) {
      int index = dn.indexOf('/');
      if (-1 == index)
        break;
      String ou = dn.substring(index + 1);
      LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
          "Group list adding OU " + ou);
      userNestedGroups.add(ou);
      dn = ou;
    }
  }

  @SuppressWarnings("unchecked")
  private void getParentGroups(String name, NotesView vwServerAccess,
      NotesView vwGroupCache) throws RepositoryException {
    final String METHOD = "getParentGroups";
    LOGGER.entering(CLASS_NAME, METHOD);

    NotesViewNavigator nvnAccess = vwServerAccess
        .createViewNavFromCategory(name);
    NotesViewEntry nveAccessEntry = nvnAccess.getFirst();
    while (nveAccessEntry != null) {
      NotesDocument accessdoc = nveAccessEntry.getDocument();
      String listName = accessdoc.getItemValueString(NCCONST.GITM_LISTNAME);
      LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD, "LIST NAME IS " + listName);
      // Add this group
      userNestedGroups.add(listName);

      // And any groups this group is nested in (parents)
      NotesViewNavigator nvnParentGroups = vwGroupCache
          .createViewNavFromCategory(listName);
      NotesViewEntry nveParentGroupEntry = nvnParentGroups.getFirst();
      while (nveParentGroupEntry != null) {
        NotesDocument listDoc = nveParentGroupEntry.getDocument();
        userNestedGroups.addAll(listDoc.getItemValue(NCCONST.GCITM_GROUPNAME));
        listDoc.recycle();
        NotesViewEntry prevParentGroupEntry = nveParentGroupEntry;
        nveParentGroupEntry = nvnParentGroups.getNext(prevParentGroupEntry);
        prevParentGroupEntry.recycle();
      }
      nvnParentGroups.recycle();
      NotesViewEntry prevAccessEntry = nveAccessEntry;
      nveAccessEntry = nvnAccess.getNext(prevAccessEntry);
      prevAccessEntry.recycle();
      accessdoc.recycle();
    }
    LOGGER.exiting(CLASS_NAME, METHOD);
  }

  @SuppressWarnings("unchecked")
  private boolean checkPersonSelectionFormula(String userSelectionFormula,
      NotesDocument personDoc) throws RepositoryException {
    Vector<Double> vecEvalResult = (Vector<Double>) ns.evaluate(
        userSelectionFormula, personDoc);
    // A Selection formula will return a vector of doubles.
    if (1 == vecEvalResult.elementAt(0)) {
      return true;
    }
    return false;
  }
}
