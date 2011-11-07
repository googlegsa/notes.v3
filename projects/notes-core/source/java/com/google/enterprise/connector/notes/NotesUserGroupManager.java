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
// limitations under the License.package com.google.enterprise.connector.notes;

package com.google.enterprise.connector.notes;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Collections;
import java.util.Vector;
import java.util.LinkedHashSet;

import com.google.enterprise.connector.notes.client.NotesDatabase;
import com.google.enterprise.connector.notes.client.NotesDocument;
import com.google.enterprise.connector.notes.client.NotesDocumentCollection;
import com.google.enterprise.connector.notes.client.NotesEmbeddedObject;
import com.google.enterprise.connector.notes.client.NotesItem;
import com.google.enterprise.connector.notes.client.NotesRichTextItem;
import com.google.enterprise.connector.notes.client.NotesSession;
import com.google.enterprise.connector.notes.client.NotesView;
import com.google.enterprise.connector.notes.client.NotesViewEntry;
import com.google.enterprise.connector.notes.client.NotesViewNavigator;
import com.google.enterprise.connector.notes.client.NotesDateTime;
import com.google.enterprise.connector.spi.RepositoryException;

public class NotesUserGroupManager {
	private static final String CLASS_NAME = NotesUserGroupManager.class
	    .getName();
	private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

	NotesSession ns = null;
	NotesDatabase cdb = null;
	NotesDatabase dirdb = null;
	Vector<String> processedGroups = null;
	Vector<String> existingMembers = null;
	Vector<String> userNestedGroups = null;
	NotesView vwPG = null;
	NotesView vwGroupCache = null;
	NotesView vwPeopleCache = null;
	NotesView vwServerAccess = null;
	NotesView vwParentGroups = null;
	NotesView vwVimUsers = null;
	NotesView vwVimGroups = null;

	protected void cleanUpNotesObjects() {		
		final String METHOD = "updatePeopleGroups";
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
		} catch (RepositoryException e) {
			LOGGER.log(Level.SEVERE, CLASS_NAME, e);
		}
		LOGGER.exiting(CLASS_NAME, METHOD);
	}

	public void updatePeopleGroups(NotesConnectorSession ncs) {
		final String METHOD = "updatePeopleGroups";
		LOGGER.entering(CLASS_NAME, METHOD);
		LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
		    "Starting People Group Cache update.");

		try {
			ns = ncs.createNotesSession();
			cdb = ns.getDatabase(ncs.getServer(), ncs.getDatabase());

			// Check our update interval
			if (!checkCachePollInterval(cdb, ncs)) {
				cdb.recycle();
				return;
			}

			dirdb = ns.getDatabase(ncs.getServer(), ncs.getDirectory());
			vwPG = dirdb.getView(NCCONST.DIRVIEW_PEOPLEGROUPFLAT);
			vwGroupCache = cdb.getView(NCCONST.VIEWGROUPCACHE);
			vwPeopleCache = cdb.getView(NCCONST.VIEWPEOPLECACHE);
			vwServerAccess = dirdb.getView(NCCONST.DIRVIEW_SERVERACCESS);
			vwParentGroups = cdb.getView(NCCONST.VIEWPARENTGROUPS);
			vwPG.refresh();
			vwServerAccess.refresh();

			// Pass 1 - Update nested groups
			vwGroupCache.refresh();
			updateGroups(ncs, vwPG, vwGroupCache);

			// Pass 2 - Update People
			vwGroupCache.refresh();
			vwParentGroups.refresh();
			vwPeopleCache.refresh();
			updatePeople(ncs, vwPG, vwPeopleCache, vwServerAccess, vwParentGroups);

			// Pass3 - Delete any users that no longer exist
			vwVimUsers = dirdb.getView(NCCONST.DIRVIEW_VIMUSERS);
			vwVimUsers.refresh();
			checkDeletions(ncs, vwPeopleCache, vwVimUsers);

			// Pass4 - Delete any groups that no longer exist
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

	protected void setLastCacheUpdate() {
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

	protected boolean checkCachePollInterval(NotesDatabase cdb,
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

	protected void checkDeletions(NotesConnectorSession ncs, NotesView checkView,
	    NotesView vwPG) throws RepositoryException {
		final String METHOD = "checkDeletions";
		String Key = null;
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
				if (formName.contentEquals(NCCONST.DIRFORM_GROUP))
					Key = doc.getItemValueString(NCCONST.GCITM_GROUPNAME);
				if (formName.contentEquals(NCCONST.DIRFORM_PERSON)) {
					isPerson = true;
					String abbrevFormula = String.format("@Name([ABBREVIATE];\"%s\")",
					    doc.getItemValueString(NCCONST.PCITM_NOTESNAME));
					Key = ns.evaluate(abbrevFormula).elementAt(0).toString();
				}
				NotesDocument searchDoc = vwPG.getDocumentByKey(Key);
				LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
				    "Directory deletion check for: " + Key);
				if (null == searchDoc) {
					// This person or group no longer exists. Remove them
					LOGGER.logp(Level.INFO, CLASS_NAME, METHOD,
					    "User/Group no longer exists in source directory and will be deleted: "
					        + Key);
					remove = true;
				} else {
					// Do an additional check for Persons to make sure they still meet the
					// selection criteria
					if (isPerson)
						selected = checkPersonSelectionFormula(userSelectionFormula, doc);
					searchDoc.recycle();
				}
				if (!selected) {
					LOGGER.logp(Level.INFO, CLASS_NAME, METHOD,
					    "User no longer meets selection criteria and will be deleted: "
					        + Key);
					remove = true;
				}

				if (remove)
					doc.remove(true);

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
	protected void getNestedGroups(NotesView vwPG, String GroupName)
	    throws RepositoryException {
		final String METHOD = "getNestedGroups";
		LOGGER.entering(CLASS_NAME, METHOD);

		LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
		    "Processing nested groups for " + GroupName);
		if (processedGroups.contains(GroupName)) {
			LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
			    "All ready processed group." + GroupName);
			return;
		}
		processedGroups.add(GroupName);

		// Get the group document
		NotesDocument groupDoc = vwPG.getDocumentByKey(GroupName, true);
		Vector<String> grpMembers = groupDoc.getItemValue(NCCONST.GITM_MEMBERS);

		for (String mem : grpMembers) {
			NotesDocument memDoc = vwPG.getDocumentByKey(mem);
			if (null == memDoc)
				continue;
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

	protected void updateGroups(NotesConnectorSession ncs, NotesView vwPG,
	    NotesView vwParentGroups) throws RepositoryException {
		final String METHOD = "updateGroups";
		NotesDocument prevDoc = null;
		NotesDocument groupDoc = null;
		LOGGER.entering(CLASS_NAME, METHOD);

		LOGGER.logp(Level.FINE, CLASS_NAME, METHOD, "Starting group cache update");
		NotesDocument doc = vwPG.getFirstDocument();
		// Pass1 - Update all the groups and work out nested groups
		while (null != doc) {
			try {
				prevDoc = doc;
				String groupName = doc.getItemValueString(NCCONST.GITM_LISTNAME);
				LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD, "Processing group "
				    + groupName);
				// Only process groups
				if (!doc.getItemValueString(NCCONST.ITMFORM).contentEquals(
				    NCCONST.DIRFORM_GROUP)) {
					LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD, "This is not a group "
					    + groupName);
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
				if (null == groupDoc) {
					LOGGER.logp(Level.INFO, CLASS_NAME, METHOD, "Creating group "
					    + groupName);
					groupDoc = cdb.createDocument();
					groupDoc.replaceItemValue(NCCONST.ITMFORM, NCCONST.DIRFORM_GROUP);
					groupDoc.replaceItemValue(NCCONST.GCITM_GROUPNAME, groupName);
				}
				groupDoc.replaceItemValue(NCCONST.GCITM_CHILDGROUPS, existingMembers);
				groupDoc.save(true);
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

	public String evaluatePVI(String userNameFormula, NotesDocument doc)
	    throws RepositoryException {
		Vector<?> VecEvalResult = ns.evaluate(userNameFormula, doc);
		// Make sure we dont' get an empty vector or an empty string
		if (VecEvalResult != null) {
			if (VecEvalResult.size() > 0) {
				return (VecEvalResult.elementAt(0).toString());
			}
		}
		return ("");
	}

	protected void updatePeople(NotesConnectorSession ncs, NotesView vwPG,
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
				if (!selected && (null != personDoc)) {
					LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
					    "Removing user as they no longer fit selection criteria: "
					        + fullName);
					doc = vwPG.getNextDocument(prevDoc);
					personDoc.remove(true);
					prevDoc.recycle();
					continue;
				}

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
				personDoc.recycle();
				personDoc = null;
				doc = vwPG.getNextDocument(prevDoc);
				prevDoc.recycle();
			} catch (RepositoryException e) {
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

	protected void getGroupsFromDN(String dn) {
		final String METHOD = "getGroupsFromDN";

		while (true) {
			int index = dn.indexOf('/');
			if (-1 == index)
				break;
			String ou = dn.substring(index + 1);
			LOGGER
			    .logp(Level.FINER, CLASS_NAME, METHOD, "Group list adding OU " + ou);
			userNestedGroups.add(ou);
			dn = ou;
		}
	}

	@SuppressWarnings("unchecked")
	protected void getParentGroups(String name, NotesView vwServerAccess,
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
	protected boolean checkPersonSelectionFormula(String userSelectionFormula,
	    NotesDocument personDoc) throws RepositoryException {
		Vector<Double> VecEvalResult = (Vector<Double>) ns.evaluate(
		    userSelectionFormula, personDoc);
		// A Selection formula will return a vector of doubles.
		if (1 == VecEvalResult.elementAt(0)) {
			return true;
		}
		return false;
	}

}
