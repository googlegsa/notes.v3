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

import com.google.enterprise.connector.notes.client.NotesACL;
import com.google.enterprise.connector.notes.client.NotesACLEntry;
import com.google.enterprise.connector.notes.client.NotesDatabase;
import com.google.enterprise.connector.notes.client.NotesDateTime;
import com.google.enterprise.connector.notes.client.NotesDocument;
import com.google.enterprise.connector.notes.client.NotesDocumentCollection;
import com.google.enterprise.connector.notes.client.NotesEmbeddedObject;
import com.google.enterprise.connector.notes.client.NotesItem;
import com.google.enterprise.connector.notes.client.NotesSession;
import com.google.enterprise.connector.notes.client.NotesView;
import com.google.enterprise.connector.spi.RepositoryException;

import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotesDatabasePoller {
  private static final String CLASS_NAME = NotesDatabasePoller.class.getName();
  private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

  NotesDateTime pollTime = null; // Configuration database
  NotesView templateView = null;
  NotesView unidView = null;
  NotesView srcdbView = null;

  // This method will reset any documents in the crawl queue that are
  // in the INCRAWL state back to NEW state
  public static void resetCrawlQueue(NotesConnectorSession ncs) {
    final String METHOD = "resetCrawlQueue";
    NotesSession ns = null;

    LOGGER.entering(CLASS_NAME, METHOD);
    try {
      ns = ncs.createNotesSession();
      NotesDatabase cdb = ns.getDatabase(
          ncs.getServer(), ncs.getDatabase());

      // Reset the last update date for each configured database
      NotesView incrawlView = cdb.getView(NCCONST.VIEWINCRAWL);
      incrawlView.refresh();
      NotesDocument srcDoc = incrawlView.getFirstDocument();
      while (null != srcDoc) {
        LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
            "Connector starting - Resetting crawl document found " +
            "in INCRAWL state " + srcDoc.getUniversalID());
        srcDoc.replaceItemValue(NCCONST.NCITM_STATE, NCCONST.STATENEW);
        NotesDocument prevDoc = srcDoc;
        srcDoc = incrawlView.getNextDocument(prevDoc);

        // Don't save this until we have the next doc in the view.
        // Otherwise an exception will result
        prevDoc.save();
        prevDoc.recycle();
      }
      incrawlView.recycle();
    } catch (Exception e) {
      LOGGER.logp(Level.SEVERE, CLASS_NAME, METHOD,
          "Error resetting crawl document.", e);
    } finally {
      ncs.closeNotesSession(ns);
      LOGGER.exiting(CLASS_NAME, METHOD);
    }
  }

  public static void resetDatabases(NotesConnectorSession ncs) {
    final String METHOD = "resetDatabases";
    NotesSession ns = null;
    LOGGER.entering(CLASS_NAME, METHOD);
    try {
      ns = ncs.createNotesSession();
      NotesDatabase cdb = ns.getDatabase(
          ncs.getServer(), ncs.getDatabase());

      // Reset the last update date for each configured database
      NotesView srcdbView = cdb.getView(NCCONST.VIEWDATABASES);
      srcdbView.refresh();
      NotesDocument srcdbDoc = srcdbView.getFirstDocument();
      while (null != srcdbDoc) {
        LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
            "Connector reset - Resetting database last update date for " +
            srcdbDoc.getItemValue(NCCONST.DITM_DBNAME));
        srcdbDoc.removeItem(NCCONST.DITM_LASTUPDATE);
        srcdbDoc.save(true);
        NotesDocument prevDoc = srcdbDoc;
        srcdbDoc = srcdbView.getNextDocument(prevDoc);
        prevDoc.recycle();
      }
      srcdbView.recycle();
    } catch (Exception e) {
      LOGGER.logp(Level.SEVERE, CLASS_NAME, METHOD,
          "Error resetting connector.", e);
    } finally {
      ncs.closeNotesSession(ns);
      LOGGER.exiting(CLASS_NAME, METHOD);
    }
  }

  public void pollDatabases(NotesSession ns, NotesDatabase cdb,
      int maxDepth) {
    final String METHOD = "pollDatabases";
    LOGGER.entering(CLASS_NAME, METHOD);
    try {
      // TODO: use Date or Calendar to avoid the Notes library
      // dependency on the operating system's settings for date
      // formats.
      pollTime = ns.createDateTime("1/1/1900");
      pollTime.setNow();

      templateView = cdb.getView(NCCONST.VIEWTEMPLATES);
      srcdbView = cdb.getView(NCCONST.VIEWDATABASES);
      srcdbView.refresh();
      NotesView vwSubmitQ = cdb.getView(NCCONST.VIEWSUBMITQ);
      NotesView vwCrawlQ = cdb.getView(NCCONST.VIEWCRAWLQ);

      // TODO: Make this loop shutdown aware

      NotesDocument srcdbDoc = srcdbView.getFirstDocument();
      while (null != srcdbDoc) {
        vwSubmitQ.refresh();
        vwCrawlQ.refresh();
        int qDepth = vwSubmitQ.getEntryCount() + vwCrawlQ.getEntryCount();
        LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
            "Total documents in crawl and submit queues is: " + qDepth);
        if (vwSubmitQ.getEntryCount() + vwCrawlQ.getEntryCount() > maxDepth) {
          LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
              "Queue threshold reached.  Suspending polling. size/max=" +
              qDepth + "/" + maxDepth);
          srcdbDoc.recycle();
          break;
        }
        LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
            "Source Database Config Document " +
            srcdbDoc.getItemValue(NCCONST.DITM_DBNAME));
        pollSourceDatabase(ns, cdb, srcdbDoc);
        NotesDocument prevDoc = srcdbDoc;
        srcdbDoc = srcdbView.getNextDocument(prevDoc);
        prevDoc.recycle();
      }
      vwSubmitQ.recycle();
      vwCrawlQ.recycle();
      pollTime.recycle();
      templateView.recycle();
      srcdbView.recycle();
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, CLASS_NAME, e);
    } finally {
      LOGGER.exiting(CLASS_NAME, METHOD);
    }
  }

  public void processRoles(NotesACL acl, NotesDocument dbdoc)
      throws RepositoryException {
    final String METHOD = "processRoles";
    LOGGER.entering(CLASS_NAME, METHOD);
    Vector<String> ExpandedRoleGroups = new Vector<String>();
    Vector<?> Roles = acl.getRoles();
    LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
        "Roles are " + Roles.toString());
    for (int i = 0; i < Roles.size(); i++) {
      String RoleName = Roles.elementAt(i).toString();
      StringBuffer RoleGroups = new StringBuffer();
      RoleGroups.append(RoleName);
      NotesACLEntry ae = acl.getFirstEntry();
      while (null != ae) {
        int RoleType = ae.getUserType();
        if ((RoleType != NotesACLEntry.TYPE_SERVER) &&
            (RoleType != NotesACLEntry.TYPE_SERVER_GROUP)) {
          if (ae.isRoleEnabled(Roles.elementAt(i).toString())) {
            RoleGroups.append("~~");
            RoleGroups.append(ae.getName().toLowerCase());
            RoleGroups.append("~~");
          }
        }
        NotesACLEntry prevae = ae;
        ae = acl.getNextEntry(prevae);
        prevae.recycle();
      }
      // Skip roles with no groups or users assigned
      if (RoleGroups.charAt(RoleGroups.length() - 1) != ']') {
        ExpandedRoleGroups.add(RoleGroups.toString());
      }
    }
    dbdoc.replaceItemValue(NCCONST.NCITM_ROLEPREFIX, ExpandedRoleGroups);
    LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
        "Roles are " + Roles.toString());
    LOGGER.exiting(CLASS_NAME, METHOD);
  }

  public boolean processACL(NotesDatabase srcdb, NotesDocument dbdoc) {
    final String METHOD = "processACL";
    LOGGER.entering(CLASS_NAME, METHOD);
    try {
      // To determine if the ACL has changed we check the log
      String aclActivityText = srcdb.getACLActivityLog()
          .firstElement().toString();
      if (aclActivityText.contentEquals(
              dbdoc.getItemValueString(NCCONST.DITM_ACLTEXT))) {
        LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
            "ACL has not changed.  Skipping ACL processing. ");
        return false;
      }
      LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
          "New ACL Text is. " + aclActivityText);
      dbdoc.replaceItemValue(NCCONST.DITM_ACLTEXT, aclActivityText);
      NotesACL acl = srcdb.getACL();

      // TODO: If we make this a static method and allow the
      // authorization manager to call it we need to consider how
      // the last update will be handled.

      NotesItem noAccessUsers = dbdoc.replaceItemValue(
          NCCONST.NCITM_DBNOACCESSUSERS, null);
      // None of these need to be appear in views.
      noAccessUsers.setSummary(false);
      NotesItem PermitUsers = dbdoc.replaceItemValue(
          NCCONST.NCITM_DBPERMITUSERS, null);
      PermitUsers.setSummary(false);
      NotesItem PermitGroups = dbdoc.replaceItemValue(
          NCCONST.NCITM_DBPERMITGROUPS, null);
      PermitGroups.setSummary(false);

      NotesACLEntry ae = acl.getFirstEntry();
      while (null != ae) {
        LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
            "Checking ACL Entry: " + ae.getName());
        int userType = ae.getUserType();
        // If this is a user explicitly listed with DEPOSITOR or NO ACCESS
        if (NotesACL.LEVEL_READER > ae.getLevel()) {
          // We only need to add people here
          if ((userType == NotesACLEntry.TYPE_PERSON) ||
              (userType == NotesACLEntry.TYPE_UNSPECIFIED)) {
            LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
                "Adding the user entry to deny list: " + ae.getName());
            noAccessUsers.appendToTextList(ae.getName().toLowerCase());
          }
        }

        // If this entry has an access level greater than DEPOSITOR
        if (NotesACL.LEVEL_DEPOSITOR < ae.getLevel()) {
          // Add to the PERMIT USERS if they are a user
          if ((userType == NotesACLEntry.TYPE_PERSON) ||
              (userType == NotesACLEntry.TYPE_UNSPECIFIED)) {
            LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
                "Adding the user entry to person allow list: " + ae.getName());
            PermitUsers.appendToTextList(ae.getName().toLowerCase());
          }
          // Add to the PERMIT GROUPS if they are a group
          if  ((userType == NotesACLEntry.TYPE_MIXED_GROUP) ||
              (userType == NotesACLEntry.TYPE_PERSON_GROUP) ||
              (userType == NotesACLEntry.TYPE_UNSPECIFIED)) {
            LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
                "Adding the user entry to group allow list: " + ae.getName());
            PermitGroups.appendToTextList(ae.getName().toLowerCase());
          }
        }

        // Now check which roles this ACL entry has
        NotesACLEntry prevae = ae;
        ae = acl.getNextEntry(prevae);
        prevae.recycle();
      }
      noAccessUsers.recycle();
      PermitUsers.recycle();
      PermitGroups.recycle();
      processRoles(acl,dbdoc);
      acl.recycle();
    } catch (Exception e) {
      // TODO: should we return false here?
      LOGGER.log(Level.SEVERE, CLASS_NAME, e);
    } finally {
      LOGGER.exiting(CLASS_NAME, METHOD);
    }
    return true;
  }

  /*
   *
   * This function should probably return the number of documents queued
   * that way we can prevent overflowing the database
   *
   */
  private void pollSourceDatabase(NotesSession ns,
      NotesDatabase cdb, NotesDocument srcdbDoc) {
    final String METHOD = "pollSourceDatabase";
    NotesDateTime lastUpdated = null;
    Vector<?> lastUpdatedV = null;
    LOGGER.entering(CLASS_NAME, METHOD);

    try {
      // There are configuration options to stop and disable databases
      // In either of these states, we skip processing the database
      if (1 != srcdbDoc.getItemValueInteger(NCCONST.DITM_CRAWLENABLED)) {
        LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
            "Skipping database - Database is DISABLED.");
        return;
      }
      if (1 == srcdbDoc.getItemValueInteger(NCCONST.DITM_STOPPED)) {
        LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
            "Skipping database - Database is STOPPED.");
        return;
      }

      // When was this database last updated?
      lastUpdatedV = srcdbDoc.getItemValue(NCCONST.DITM_LASTUPDATE);
      if (0 < lastUpdatedV.size ()) {
        lastUpdated = (NotesDateTime) lastUpdatedV.firstElement();
        LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
            "Last processed time was " + lastUpdated);
      } else {
        lastUpdated = ns.createDateTime("1/1/1980");
        LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
            "Database has never been processed.");
      }

      // What's our poll interval?
      double pollInterval = srcdbDoc.getItemValueInteger(
          NCCONST.DITM_UPDATEFREQUENCY);
      double elapsedMinutes = pollTime.timeDifference(lastUpdated) / 60;
      LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
          "Time difference is : " + elapsedMinutes);

      // Check poll interval
      if (pollInterval > elapsedMinutes) {
        LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
            "Skipping database - Poll interval has not yet elapsed.");
        lastUpdated.recycle();
        ns.recycle(lastUpdatedV);
        return;
      }

      // Get modified documents
      NotesDatabase srcdb = ns.getDatabase(null, null);
      srcdb.openByReplicaID(
          srcdbDoc.getItemValueString(NCCONST.DITM_SERVER),
          srcdbDoc.getItemValueString(NCCONST.DITM_REPLICAID));
      
      // Did the database open succeed? If not exit
      if (!srcdb.isOpen()) {
        LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
            "Skipping database - Database could not be opened.");
        lastUpdated.recycle();
        ns.recycle(lastUpdatedV);
      	srcdb.recycle();
      	return;
      }

      if (processACL(srcdb, srcdbDoc)) {
        // If the ACL has changed and we are using per Document
        // ACLs we need to resend all documents.
        if (srcdbDoc.getItemValueString(NCCONST.DITM_AUTHTYPE)
            .contentEquals(NCCONST.AUTH_ACL)) {
          LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
              "Database ACL has changed - Resetting last update " +
              "to reindex all document ACLs.");
          lastUpdated = ns.createDateTime("1/1/1980");
        }
      }

      // From the template, we get the search string to determine
      // which documents should be processed
      NotesDocument templateDoc = templateView.getDocumentByKey(
          srcdbDoc.getItemValueString(NCCONST.DITM_TEMPLATE), true);
      String searchString = templateDoc.getItemValueString(
          NCCONST.TITM_SEARCHSTRING);
      // We append the last processed date as a modifier
      searchString += " & @Modified > [" + lastUpdated + "]";
      LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
          "Search string is: " + searchString);

      //getDBReaderGroups(srcdb);

      NotesDocumentCollection dc = srcdb.search(searchString, null, 0);
      LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
          srcdb.getFilePath() + " Number of documents to be processed: " +
          dc.getCount());
      NotesDocument curDoc = dc.getFirstDocument();
      while (null != curDoc) {
        String NotesURL = curDoc.getNotesURL();
        LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
            "Processing document " + NotesURL);
        if (curDoc.hasItem(NCCONST.NCITM_CONFLICT)) {
          LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
              "Skipping conflict document " + NotesURL);
          NotesDocument prevDoc = curDoc;
          curDoc = dc.getNextDocument(prevDoc);
          prevDoc.recycle();
          continue;
        }

        // Create a new crawl request
        NotesDocument crawlRequestDoc = cdb.createDocument();
        crawlRequestDoc.appendItemValue(NCCONST.NCITM_STATE, NCCONST.STATENEW);
        crawlRequestDoc.appendItemValue(NCCONST.ITM_MIMETYPE,
            NCCONST.DEFAULT_DOCMIMETYPE);

        // Create the fields necessary to crawl the document
        crawlRequestDoc.appendItemValue(NCCONST.ITMFORM,
            NCCONST.FORMCRAWLREQUEST);
        crawlRequestDoc.appendItemValue(NCCONST.NCITM_UNID,
            curDoc.getUniversalID());
        crawlRequestDoc.appendItemValue(NCCONST.NCITM_REPLICAID,
            srcdbDoc.getItemValueString(NCCONST.DITM_REPLICAID));
        crawlRequestDoc.appendItemValue(NCCONST.NCITM_SERVER,
            srcdbDoc.getItemValueString(NCCONST.DITM_SERVER));
        crawlRequestDoc.appendItemValue(NCCONST.NCITM_TEMPLATE,
            srcdbDoc.getItemValueString(NCCONST.DITM_TEMPLATE));
        crawlRequestDoc.appendItemValue(NCCONST.NCITM_DOMAIN,
            srcdbDoc.getItemValueString(NCCONST.DITM_DOMAIN));
        crawlRequestDoc.appendItemValue(NCCONST.NCITM_AUTHTYPE,
            srcdbDoc.getItemValueString(NCCONST.DITM_AUTHTYPE));

        // Map the lock field directly across
        crawlRequestDoc.appendItemValue(NCCONST.ITM_LOCK,
            srcdbDoc.getItemValueString(NCCONST.DITM_LOCKATTRIBUTE)
            .toLowerCase());

        // Add any database level meta data to the document
        crawlRequestDoc.appendItemValue(NCCONST.ITM_GMETAREPLICASERVERS,
            srcdbDoc.getItemValue(NCCONST.DITM_REPLICASERVERS));
        crawlRequestDoc.appendItemValue(NCCONST.ITM_GMETACATEGORIES,
            srcdbDoc.getItemValue(NCCONST.DITM_DBCATEGORIES));
        crawlRequestDoc.appendItemValue(NCCONST.ITM_GMETADATABASE,
            srcdbDoc.getItemValueString(NCCONST.DITM_DBNAME));
        crawlRequestDoc.appendItemValue(NCCONST.ITM_GMETANOTESLINK, NotesURL);

        crawlRequestDoc.save();
        crawlRequestDoc.recycle();  //TEST THIS
        crawlRequestDoc = null;
        NotesDocument prevDoc = curDoc;
        curDoc = dc.getNextDocument(prevDoc);
        prevDoc.recycle();
      }
      dc.recycle();

      // Set last modified date
      srcdbDoc.replaceItemValue(NCCONST.DITM_LASTUPDATE, pollTime);
      srcdbDoc.save();

      // TODO: Handle db.search for case where there are more
      // that 5000 documents
      // Suspect this limitation is for DIIOP only and not for RPC access
      // Have sucessfully tested up to 9000 documents

      // Recycle our objects
      srcdb.recycle();
      templateDoc.recycle();
      lastUpdated.recycle();
      ns.recycle(lastUpdatedV);
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, CLASS_NAME, e);
    } finally {
      LOGGER.exiting(CLASS_NAME, METHOD);
    }
  }
}
