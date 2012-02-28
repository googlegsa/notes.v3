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

import com.google.enterprise.connector.notes.client.NotesDatabase;
import com.google.enterprise.connector.notes.client.NotesDocument;
import com.google.enterprise.connector.notes.client.NotesSession;
import com.google.enterprise.connector.notes.client.NotesThread;
import com.google.enterprise.connector.notes.client.NotesView;
import com.google.enterprise.connector.notes.client.NotesViewEntry;
import com.google.enterprise.connector.notes.client.NotesViewNavigator;
import com.google.enterprise.connector.spi.AuthenticationIdentity;
import com.google.enterprise.connector.spi.AuthorizationManager;
import com.google.enterprise.connector.spi.AuthorizationResponse;
import com.google.enterprise.connector.spi.RepositoryException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

class NotesAuthorizationManager implements AuthorizationManager {
  private static final String CLASS_NAME =
      NotesAuthorizationManager.class.getName();
  private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

  private NotesConnectorSession ncs = null;

  public NotesAuthorizationManager(NotesConnectorSession session) {
    final String METHOD = "NotesAuthorizationManager";
    LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
        "NotesAuthorizationManager being created.");
    ncs = session;
  }

  /* The docid is generated by the application and always takes the format
   * http://server.domain/ReplicaID/0/UniversalDocumentID
   * The protocol is always http://
   *
   * TODO: We need better documentation for the expected docid
   * format here. Plus constants (preferably generated
   * ones). Also, what happens if the docid uses HTTPS?
   *
   * Consider using java.net.URL and String.split to get the
   * pieces. Attachment docid values can be longer.
   */
  protected String getRepIdFromDocId(String docId) {
    int start = docId.indexOf('/', 7);  // Find the first slash after http://
    return docId.substring(start + 1, start + 17);
  }

  /* The docid is generated by the application and always takes the format
   * http://server.domain/ReplicaID/0/UniversalDocumentID
   * The protocol is always http://
   */
  protected String getUNIDFromDocId(String docId) {
    int start = docId.indexOf('/', 7);  // Find the first slash after http://
    return docId.substring(start + 20, start + 52);
  }

  // Explain Lotus Notes Authorization Rules

  // TODO: Add LRU Cache for ALLOW/DENY
  /* @Override */
  @SuppressWarnings("unchecked")
  public Collection<AuthorizationResponse> authorizeDocids(
      Collection<String> docIds, AuthenticationIdentity id) {
    final String METHOD = "authorizeDocids";
    long elapsedTimeMillis = 0;
    long startTime = System.currentTimeMillis();

    String pvi = id.getUsername();
    ArrayList<AuthorizationResponse> authorized =
        new ArrayList<AuthorizationResponse>(docIds.size());

    String notesName = null;
    Vector<String> userGroups = null;
    NotesSession ns = null;
    try {
      LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
          "Authorizing documents for user " + pvi);
      ns = ncs.createNotesSession();

      NotesDatabase cdb = ns.getDatabase(ncs.getServer(), ncs.getDatabase());

      // TODO: Some of this code is very similar to code
      // elsewhere (esp.  NotesAuthenticationManager), with the
      // exception of the securityView in the middle (but unused
      // until later). Extract a helper method somewhere?
      NotesView securityView = cdb.getView(NCCONST.VIEWSECURITY);
      NotesView people = null;
      NotesDocument personDoc = null;
      synchronized(ncs.getConnector().getPeopleCacheLock()) {
        people = cdb.getView(NCCONST.VIEWPEOPLECACHE);
        people.refresh();
        // Resolve the PVI to their Notes names and groups
        personDoc = people.getDocumentByKey(id.getUsername(), true);
        if (null != personDoc) {
          notesName = personDoc.getItemValueString(NCCONST.ACITM_NOTESNAME)
              .toLowerCase();
          userGroups = (Vector<String>) personDoc.getItemValue(
              NCCONST.ACITM_GROUPS);
        }
      }
      if (null == personDoc) {
        // Changed log level to FINE as an AuthZ message.
        LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
            "Person not found in ACL database. DENY all docs. " + pvi);
      } else {
        notesName = personDoc.getItemValueString(NCCONST.ACITM_NOTESNAME)
            .toLowerCase();

        // TODO: Logged string differs here and in
        // NotesAuthenticationManager. This could all be part of
        // the helper method.
        LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
            "PVI:NOTESNAME mapping is " + pvi + ":" + notesName);
        userGroups = (Vector<String>) personDoc.getItemValue(
            NCCONST.ACITM_GROUPS);
        for (int i = 0; i < userGroups.size(); i++) {
          userGroups.set(i, userGroups.elementAt(i).toString().toLowerCase());
        }
        LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
            "Groups for " + pvi + " are: " + userGroups.toString());
      }

      // The first document in the category will always be the database document

      for (String docId : docIds) {
        NotesViewNavigator secVN = null;
        NotesDocument dbdoc = null;
        try {
          // Extract the database and UNID from the URL
          String repId = getRepIdFromDocId(docId);
          String unid = getUNIDFromDocId(docId);
          // Per-document log message
          LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
              "Authorizing documents for user " + pvi + " : " +
              repId + " : " + unid);

          if (null == personDoc) {
            // We didn't find this person, so deny all.
            // Alternatively we could return INDETERMINATE...
            authorized.add(new AuthorizationResponse(false, docId));
            continue;
          }

          // Assume we have access to the document unless proven otherwise...
          boolean docallow = true;
          // Get the category from the security view for this database
          secVN = securityView.createViewNavFromCategory(repId);
          // The first document in the category is ALWAYS the database document
          dbdoc = secVN.getFirstDocument().getDocument();
          boolean dballow = checkDatabaseAccess(notesName, dbdoc, userGroups);

          // Only check document level security if we are allowed at the database level
          if (dballow) {
            Vector<String> searchKey = new Vector<String>(3);
            searchKey.addElement(repId);
            // Database documents are type '1' in this view.
            // Crawler documents are type '2'
            searchKey.addElement("2");
            searchKey.addElement(unid);
            LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
                "Search key is  " + searchKey.toString());
            NotesDocument crawlDoc =
                securityView.getDocumentByKey(searchKey, true);
            if (crawlDoc != null) {
              // Found a crawldoc, so we will need to check document level access
              docallow = checkDocumentReaders(notesName, userGroups, crawlDoc,
                  dbdoc);
              crawlDoc.recycle();
            } else {
              // There is no crawldoc with reader lists
              // restrictions so nothing to do
              LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
                  "No document level security for " + unid);
            }
          }
          boolean allow = docallow && dballow;
          LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
              "Final auth decision is " + allow + " " + unid);
          authorized.add(new AuthorizationResponse(allow, docId));
        } catch (Throwable t) {
          LOGGER.logp(Level.WARNING, CLASS_NAME, METHOD,
              "Failed to complete check for " + docId, t);
          authorized.add(new AuthorizationResponse(
              AuthorizationResponse.Status.INDETERMINATE, docId));
        } finally {
          if (LOGGER.isLoggable(Level.FINER)) {
          elapsedTimeMillis = System.currentTimeMillis() - startTime;
          LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
              "ElapsedAuthorizationResponseTime: " + elapsedTimeMillis
              + " Documents authorized: " + authorized.size());
          }
          if (null != secVN) {
            secVN.recycle();
          }
          if (null != dbdoc) {
            dbdoc.recycle();
          }
        }
      }
      if (null != personDoc) {
        personDoc.recycle();
      }
      if (null != securityView) {
        securityView.recycle();
      }
      if (null != people) {
        people.recycle();
      }
      if (null != cdb) {
        cdb.recycle();
      }
    } catch (Exception e) {
      // TODO: what Notes exceptions can be caught here? Should
      // we be catching exceptions within the method on a
      // per-document basis? Do we return the right response if
      // an exception occurs during the processing of the
      // document list?
      LOGGER.log(Level.SEVERE, CLASS_NAME, e);
    } finally {
      ncs.closeNotesSession(ns);
    }
    if (LOGGER.isLoggable(Level.FINER)) {
      for (int i = 0; i < authorized.size(); i++) {
        AuthorizationResponse ar = authorized.get(i);
        LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
            "AuthorizationResponse: " + ar.getDocid() + " : " + ar.isValid());
      }
    }
    // Get elapsed time in milliseconds
    elapsedTimeMillis = System.currentTimeMillis() - startTime;
    LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
        "TotalAuthorizationResponseTime: " + elapsedTimeMillis
        + " milliseconds.  Documents in batch: " + docIds.size() +
        " Documents authorized: " + authorized.size());
    return authorized;
  }

  protected static String getCommonName(String notesName) {
    if (notesName.startsWith("cn=")) {
      int index = notesName.indexOf('/');
      if (index > 0)
        return notesName.substring(3, index);
    }
    return null;
  }

  protected boolean checkDocumentReaders(String notesName,
      Vector<String> userGroups, NotesDocument crawldoc,
      NotesDocument dbdoc) throws RepositoryException {
    final String METHOD = "checkDocumentReaders";
    LOGGER.entering(CLASS_NAME, METHOD);

    Vector<?> AllowAuthors =
        crawldoc.getItemValue(NCCONST.NCITM_DOCAUTHORREADERS);
    LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
        "Document reader list is " + AllowAuthors);

    // Check using the Notes name
    LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
        "Checking document level access for: " +  notesName);
    if  (AllowAuthors.contains(notesName)) {
       LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
           "ALLOWED: User is in authors " + notesName);
       LOGGER.exiting(CLASS_NAME, METHOD);
       return true;
    }

    // Check using the common name
    String CommonName = getCommonName(notesName);
    LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
        "Checking document level access for user: " +  CommonName);
    if (null != CommonName) {
      if  (AllowAuthors.contains(CommonName)) {
        LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
            "ALLOWED: User is in authors " + CommonName);
        LOGGER.exiting(CLASS_NAME, METHOD);
        return true;
      }
    }

    // Check using groups
    for (int i = 0; i < userGroups.size(); i++) {
      String group = userGroups.elementAt(i).toString();
      LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
          "Checking document level access for group: " + group);
      if (AllowAuthors.contains(group)) {
        LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
            "ALLOWED: Group is in authors " + group);
        LOGGER.exiting(CLASS_NAME, METHOD);
        return true;
      }
    }

    // Expand roles and check using roles
    Vector<String> Roles = expandRoles(notesName, userGroups, dbdoc);
    for (int i = 0; i < Roles.size(); i++) {
      String role = Roles.elementAt(i).toString();
      LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
          "Checking document level access for role: " + role);
      if (AllowAuthors.contains(role)) {
        LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
            "ALLOWED: Role is in authors " + role);
        LOGGER.exiting(CLASS_NAME, METHOD);
        return true;
      }
    }
    LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
        "DENIED: User's security principles are not in document access lists.");
    LOGGER.exiting(CLASS_NAME, METHOD);
    return false;
  }

  // Testing with R8.5 roles do not expand to nested groups.
  // You must be a direct member of the group to get the role
  // TODO: Check and validate this with other versions
  @SuppressWarnings("unchecked")
  protected Vector <String> expandRoles(String notesName,
      Vector<String> userGroups, NotesDocument dbdoc)
      throws RepositoryException {
    final String METHOD = "expandRoles";
    LOGGER.entering(CLASS_NAME, METHOD);

    // Do we have any roles?

    Vector<?> dbroles = dbdoc.getItemValue(NCCONST.NCITM_ROLEPREFIX);
    Vector<String> enabledRoles = new Vector<String>();
    LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD, "Checking roles for user: " +
        notesName + " using: " + dbroles.toString());
    if (dbroles.size() < 1) {
      return enabledRoles;
    }

    Vector<String> credentials = (Vector<String>) userGroups.clone();
    credentials.add(notesName);
    credentials.add(getCommonName(notesName));
    StringBuffer searchstring = new StringBuffer(512);

    for (int i = 0; i < dbroles.size(); i++) {
      String roledata = dbroles.elementAt(i).toString();
      for (int j = 0; j < credentials.size(); j++) {
        searchstring.setLength(0);
        searchstring.append('~');
        searchstring.append(credentials.elementAt(j));
        searchstring.append('~');
        if (roledata.contains(searchstring)) {
            enabledRoles.add(
                roledata.substring(0, roledata.indexOf(']')+1).toLowerCase());
        }
      }
    }
    LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD, "Roles enabled for user: " +
        notesName + " are: " + enabledRoles.toString());
    LOGGER.exiting(CLASS_NAME, METHOD);
    return enabledRoles;
  }

  protected boolean checkDatabaseAccess(String notesName,
      NotesDocument DbDoc, Vector<?> userGroups)
      throws RepositoryException {
    final String METHOD = "checkDatabaseAccess";
    LOGGER.entering(CLASS_NAME, METHOD);
    
    String CommonName = getCommonName(notesName);
    if (checkDenyUser(notesName, DbDoc)) {
      LOGGER.exiting(CLASS_NAME, METHOD);
      return false;
    }
    if (null != CommonName) {
      if (checkDenyUser(CommonName, DbDoc)) {
        LOGGER.exiting(CLASS_NAME, METHOD);
        return false;
      }
    }
    if (checkAllowUser(notesName, DbDoc)) {
      LOGGER.exiting(CLASS_NAME, METHOD);
      return true;
    }
    if (null != CommonName) {
      if (checkAllowUser(CommonName, DbDoc)) {
        LOGGER.exiting(CLASS_NAME, METHOD);
        return true;
      }
    }
    if (checkAllowGroup(userGroups, DbDoc )) {
      LOGGER.exiting(CLASS_NAME, METHOD);
      return true;
    }
    LOGGER.exiting(CLASS_NAME, METHOD);
    return false;
  }

  // TODO:  the access groups may not need to be summary data. to avoid 64k
  protected boolean checkAllowGroup(Vector<?> userGroups,
      NotesDocument dbdoc) throws RepositoryException {
    final String METHOD = "checkAllowGroup";
    LOGGER.entering(CLASS_NAME, METHOD);

    Vector<?> AllowGroups = dbdoc.getItemValue(NCCONST.NCITM_DBPERMITGROUPS);
    LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
        "Checking database ACL for allow for groups. " +
        "Allow groups are: " + AllowGroups.toString());

    for (int i = 0; i < userGroups.size(); i++) {
      String group = userGroups.elementAt(i).toString();
      LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD, "Checking group " + group);
      if (AllowGroups.contains(group)) {
        LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
            "ALLOWED: User is allowed through group " + group);
        LOGGER.exiting(CLASS_NAME, METHOD);
        return true;
      }
    }
    LOGGER.exiting(CLASS_NAME, METHOD);
    return false;
  }

  protected boolean checkAllowUser(String userName,
      NotesDocument dbdoc) throws RepositoryException {
    final String METHOD = "checkAllowUser";
    LOGGER.entering(CLASS_NAME, METHOD);

    LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
        "Checking database ACL for allow to user");
    Vector<?> AllowList = dbdoc.getItemValue(NCCONST.NCITM_DBPERMITUSERS);
    if  (AllowList.contains("-default-")) {
      LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
          "ALLOWED: Default is allowed " + userName);
      LOGGER.exiting(CLASS_NAME, METHOD);
      return true;
    }
    if  (AllowList.contains(userName)) {
      LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
          "ALLOWED: User is explictly allowed " + userName);
      LOGGER.exiting(CLASS_NAME, METHOD);
      return true;
    }
    return false;
  }

  protected boolean checkDenyUser(String userName, NotesDocument dbdoc)
      throws RepositoryException {
    final String METHOD = "checkDenyUser";
    LOGGER.entering(CLASS_NAME, METHOD);

    Vector<?> DenyList = dbdoc.getItemValue(NCCONST.NCITM_DBNOACCESSUSERS);
    LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
        "Checking database ACL for explicit deny to user");
    if  (DenyList.contains(userName)) {
      LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
          "DENIED: User is explictly denied " + userName);
      LOGGER.exiting(CLASS_NAME, METHOD);
      return true;
    }
    LOGGER.exiting(CLASS_NAME, METHOD);
    return false;
  }
}
