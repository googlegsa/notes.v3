// Copyright (C) 2011 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.enterprise.connector.notes;

import com.google.enterprise.connector.notes.client.NotesDatabase;
import com.google.enterprise.connector.notes.client.NotesDateTime;
import com.google.enterprise.connector.notes.client.NotesDocument;
import com.google.enterprise.connector.notes.client.NotesSession;
import com.google.enterprise.connector.notes.client.NotesView;
import com.google.enterprise.connector.notes.client.NotesViewEntry;
import com.google.enterprise.connector.notes.client.NotesViewNavigator;
import com.google.enterprise.connector.spi.RepositoryException;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
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

  private NotesSession ns = null;
  private NotesDatabase cdb = null;
  private NotesDatabase dirdb = null;
  private Vector<String> processedGroups = null;
  private Vector<String> existingMembers = null;
  private HashSet<String> userNestedGroups = null;
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
      if (null != vwPG)
        vwPG.recycle();
      if (null != vwGroupCache)
        vwGroupCache.recycle();
      if (null != vwPeopleCache)
        vwPeopleCache.recycle();
      if (null != vwServerAccess)
        vwServerAccess.recycle();
      if (null != vwParentGroups)
        vwParentGroups.recycle();
      if (null != vwVimUsers)
        vwVimUsers.recycle();
      if (null != vwVimGroups)
        vwVimGroups.recycle();
      if (null != cdb)
        cdb.recycle();
      if (null != dirdb)
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
      if (null == systemDoc) {
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
    if (null == systemDoc) {
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
    while (null != doc) {
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
        if (null == searchDoc) {
          // This person or group no longer exists. Remove them
          LOGGER.logp(Level.INFO, CLASS_NAME, METHOD,
              "User/Group no longer exists in source directory "
              + "and will be deleted: " + key);
          remove = true;
        } else {
          // Do an additional check for Persons to make sure they
          // still meet the selection criteria
          if (isPerson) {
            // Apply formula to Notes directory person document instead of 
            // connector's cached document in configuration database
            selected = 
                checkPersonSelectionFormula(userSelectionFormula, searchDoc);
          }
          searchDoc.recycle();
        }
        if (!selected) {
          LOGGER.logp(Level.INFO, CLASS_NAME, METHOD,
              "User no longer meets selection criteria and will be deleted: "
              + key);
          remove = true;
        }
        doc = checkView.getNextDocument(prevDoc);
        if (remove) {
          synchronized(ncs.getConnector().getPeopleCacheLock()) {
            prevDoc.remove(true);
          }
        }
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
    if (null == groupDoc) {
      LOGGER.logp(Level.WARNING, CLASS_NAME, METHOD,
          "Failed to find document for group: " + groupName);
      return;
    }
    Vector<String> grpMembers = groupDoc.getItemValue(NCCONST.GITM_MEMBERS);

    for (String mem : grpMembers) {
      NotesDocument memDoc = vwPG.getDocumentByKey(mem);
      if (null == memDoc) {
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
    while (null != doc) {
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
          if (null == groupDoc) {
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
        if (null != groupDoc)
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

    while (null != doc) {
      try {
        prevDoc = doc;

        // Only process people
        if (!doc.getItemValueString(NCCONST.ITMFORM).contentEquals(
                NCCONST.DIRFORM_PERSON)) {
          doc = vwPG.getNextDocument(prevDoc);
          prevDoc.recycle();
          continue;
        }

        // Need to check for FullName field in case there is a bad
        // or corrupted person document in the directory.  Skip the document if
        // the field is not existed or is empty.
        String fullName = null;
        Vector<String> fullNameValues = doc.getItemValue(NCCONST.PITM_FULLNAME);
        if (fullNameValues.size() > 0) {
          fullName = fullNameValues.firstElement().toString().toLowerCase();
        }
        if (fullName == null) {
          doc = vwPG.getNextDocument(prevDoc);
          prevDoc.recycle();
          continue;
        }
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
        if (!selected && (null != personDoc)) {
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
          if (null == personDoc) {
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
          userNestedGroups = new HashSet<String>();
          // Get groups for full name
          getParentGroups(fullName, vwServerAccess, vwGroupCache);

          // Get groups from common name
          getParentGroups(NotesAuthorizationManager.getCommonName(fullName),
              vwServerAccess, vwGroupCache);

          // Get groups from DN hierarchy
          getGroupsFromDN(fullName);

          // Sort and eliminate duplicates
          Vector<String> userGroups = 
              new Vector<String> (userNestedGroups.size());
          userGroups.addAll(userNestedGroups);
          Collections.sort(userGroups);

          LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD, "Group list for user "
              + fullName + " are " + userGroups);
          personDoc.replaceItemValue(NCCONST.PCITM_GROUPS, userGroups);
          personDoc.save(true);
          userNestedGroups.clear();
          userGroups.clear();
        }
        personDoc.recycle();
        personDoc = null;
        doc = vwPG.getNextDocument(prevDoc);
        prevDoc.recycle();
      } catch (Exception e) {
        LOGGER.log(Level.SEVERE, CLASS_NAME, e);
        if (null != personDoc) {
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
      // Prepend a wildcard to each OU to support wildcard configuration
      // in group membership and in database ACLs.
      userNestedGroups.add("*/" + ou);
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
    while (null != nveAccessEntry) {
      NotesDocument accessdoc = nveAccessEntry.getDocument();
      String listName = accessdoc.getItemValueString(NCCONST.GITM_LISTNAME);
      LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD, "LIST NAME IS " + listName);
      // Add this group
      userNestedGroups.add(listName);

      // And any groups this group is nested in (parents)
      NotesViewNavigator nvnParentGroups = vwGroupCache
          .createViewNavFromCategory(listName);
      NotesViewEntry nveParentGroupEntry = nvnParentGroups.getFirst();
      while (null != nveParentGroupEntry) {
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
