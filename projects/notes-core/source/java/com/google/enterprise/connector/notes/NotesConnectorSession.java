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
import com.google.enterprise.connector.notes.client.NotesView;
import com.google.enterprise.connector.notes.client.NotesViewEntry;
import com.google.enterprise.connector.notes.client.NotesViewNavigator;
import com.google.enterprise.connector.spi.AuthenticationManager;
import com.google.enterprise.connector.spi.AuthorizationManager;
import com.google.enterprise.connector.spi.RepositoryException;
import com.google.enterprise.connector.spi.Session;

import java.io.File;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotesConnectorSession implements Session {
  private static final String CLASS_NAME = NotesConnectorSession.class.getName();
  private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

  private NotesTraversalManager traversalManager;
  private NotesUserGroupManager userGroupManager;
  private final String server;
  private final String database;
  private final String password;
  private final NotesConnector connector;
  private Vector<String> excludedExtns = null;
  private int maxFileSize;
  private String spoolDir = null;
  private HashMap<String, String> mimeTypeMap = null;
  private final HashMap<String, String> serverDomainMap =
      new HashMap<String, String>();
  private final NotesPollerNotifier npn;
  private int maxCrawlQDepth;
  private int deletionBatchSize;
  private int numCrawlerThreads;
  private int cacheUpdateInterval;
  private String directory = null;
  private String userNameFormula = null;
  private String userSelectionFormula = null;
  private String gsaGroupPrefix;
  private boolean retainMetaData = true;
  private final NotesDocumentManager notesDocManager;
  private NotesUsernameType usernameType = NotesUsernameType.USERNAME;

  public NotesConnectorSession(NotesConnector connector,
      NotesPollerNotifier connectorNpn, String password,
      String server, String database) throws RepositoryException {
    final String METHOD = "NotesConnectorSession";
    this.server = server;
    this.database = database;
    this.password = password;
    this.connector = connector;
    NotesSession ns = null;
    boolean configValidated = false;
    LOGGER.entering(CLASS_NAME, METHOD);
    LOGGER.log(Level.FINEST, "NotesConnectorSession being created.");

    try {
      // Create and recycle sessions as we need them to avoid memory leaks
      // Init the thread and try to login to validate credentials are correct
      npn = connectorNpn;
      ns = createNotesSession();

      LOGGER.log(Level.INFO, "Notes version is {0}", ns.getNotesVersion());
      LOGGER.log(Level.CONFIG, "Connector platform is {0}", ns.getPlatform());
      LOGGER.log(Level.CONFIG, "Connector config database on server: {0}",
          server);
      LOGGER.log(Level.CONFIG, "Connector config database path: {0}", database);
      LOGGER.log(Level.CONFIG, "Connector local data directory: {0}",
          ns.getEnvironmentString(NCCONST.INIDIRECTORY, true));
      LOGGER.log(Level.CONFIG, "Connector kittype: {0}",
          ns.getEnvironmentString(NCCONST.INIKITTYPE, true));
      LOGGER.log(Level.CONFIG, "Connector keyfilename: {0}",
          ns.getEnvironmentString(NCCONST.INIKEYFILENAME, true));
      LOGGER.log(Level.CONFIG, "Connector user: {0}", ns.getCommonUserName());
      LOGGER.log(Level.CONFIG, "Connector serverkeyfilename: {0}",
          ns.getEnvironmentString(NCCONST.INISERVERKEYFILENAME, true));
      LOGGER.log(Level.CONFIG, "Connector debug_outfile: {0}",
          ns.getEnvironmentString(NCCONST.INIDEBUGOUTFILE, true));

      NotesDatabase db = ns.getDatabase(server, database);
      configValidated = loadConfig(ns, db);

      db.recycle();
      notesDocManager = new NotesDocumentManager(this);
    } catch (Exception e) {
      throw new RepositoryException("NotesConnectorSession error", e);
    } finally {
      closeNotesSession(ns);
    }

    // If we could not validate our config then let the connector
    // manager know session creation has failed.
    if (!configValidated) {
      throw new RepositoryException(
          "Invalid Notes Connector System Configuration.");
    }
    LOGGER.exiting(CLASS_NAME, METHOD);
  }

  // Loads configuration from the system config doc
  // Returns true if the configuration loads succesfully
  // Returns false if an error occurs
  @SuppressWarnings("unchecked")
  public boolean loadConfig(NotesSession ns, NotesDatabase db) {
    final String METHOD = "loadConfiguration";
    LOGGER.entering(CLASS_NAME, METHOD);

    try {
      LOGGER.log(Level.CONFIG,
          "Loading configuration from system setup document.");

      NotesView vw = db.getView(NCCONST.VIEWSYSTEMSETUP);
      NotesDocument systemDoc = vw.getFirstDocument();
      if (null == systemDoc) {
        LOGGER.log(Level.SEVERE, "System configuration document not found.");
        return false;
      }

      // "." means no file extension.  Replace with an empty string if it exists.
      excludedExtns = (Vector<String>)
          systemDoc.getItemValue(NCCONST.SITM_EXCLUDEDEXTENSIONS);
      for (int i = 0; i < excludedExtns.size(); i++) {
        LOGGER.log(Level.CONFIG,
            "The following file extensions will be excluded {0}",
            excludedExtns.elementAt(i));
        if (excludedExtns.elementAt(i).equals(".")) {
          excludedExtns.set(i, "");
        }
      }

      maxFileSize = 1024 * 1024
          * systemDoc.getItemValueInteger(NCCONST.SITM_MAXFILESIZE);
      LOGGER.log(Level.CONFIG, "Maximum attachment size is {0}", maxFileSize);

      // If 0, use the default value
      if (maxFileSize == 0) {
        maxFileSize = 1024 * 1024 * NCCONST.DEFAULT_MAX_FILE_LIMIT;
      }

      // Get the spool directory for processing attachments
      spoolDir = systemDoc.getItemValueString(NCCONST.SITM_SPOOLDIR);
      if (Strings.isNullOrEmpty(spoolDir)) {
        spoolDir = String.format("%s/%s",
            ns.getEnvironmentString(NCCONST.INIDIRECTORY, true),
            NCCONST.DEFAULT_ATTACHMENT_DIR);;
      }
      File sdir = new File(spoolDir);

      // Make the directory and make sure we can write to it
      sdir.mkdirs();
      if (!sdir.isDirectory() || !sdir.canWrite()) {
        LOGGER.log(Level.SEVERE,
            "Can't write to spool directory {0}", spoolDir);
        return false;
      }
      LOGGER.log(Level.CONFIG,
          "Attachment spool directory is set to {0}", spoolDir);

      // Threshhold for polling
      maxCrawlQDepth = systemDoc.getItemValueInteger(
          NCCONST.SITM_MAXCRAWLQDEPTH);
      if (maxCrawlQDepth < 1)  {
        LOGGER.log(Level.SEVERE,
            "Invalid setting for maxCrawlQDepth: {0}", maxCrawlQDepth);
        return false;
      }
      LOGGER.log(Level.CONFIG, "maxCrawlQDepth is {0}", maxCrawlQDepth);

      // Time between user/group cache updates
      cacheUpdateInterval = systemDoc.getItemValueInteger(
          NCCONST.SITM_CACHEUPDATEINTERVAL);
      if (cacheUpdateInterval < 1)  {
        LOGGER.log(Level.SEVERE,
            "Invalid setting for cache update interval: {0}",
            cacheUpdateInterval);
        return false;
      }
      LOGGER.log(Level.CONFIG,
          "cacheUpdateInterval is {0}", cacheUpdateInterval);

      // Get the directory and see if we can open it
      directory = systemDoc.getItemValueString(
          NCCONST.SITM_DIRECTORY);
      LOGGER.log(Level.CONFIG, "Path to Domino directory: {0}", directory);

      NotesDatabase dirDb = ns.getDatabase(this.getServer(), directory);
      dirDb.recycle();

      userNameFormula = systemDoc.getItemValueString(
          NCCONST.SITM_USERNAMEFORMULA);
      if (0 == userNameFormula.length()) {
        LOGGER.log(Level.CONFIG, "User Name formula is empty - using default");
        userNameFormula = NCCONST.DEFAULT_USERNAMEFORMULA;
      }
      LOGGER.log(Level.CONFIG, "User Name formula: {0}", userNameFormula);

      String usernameTypeConfig = systemDoc.getItemValueString(
          NCCONST.SITM_USERNAMETYPE);
      if (usernameTypeConfig != null && usernameTypeConfig.length() > 0) {
        usernameType = NotesUsernameType.findUsernameType(
            usernameTypeConfig.toUpperCase());
      }
      LOGGER.log(Level.CONFIG, "Notes username type: {0}", usernameType.name());

      userSelectionFormula = systemDoc.getItemValueString(
          NCCONST.SITM_USERSELECTIONFORMULA);
      if (0 == userSelectionFormula.length()) {
        LOGGER.log(Level.CONFIG,
            "User Selection formula is empty - using default");
        userSelectionFormula = NCCONST.DEFAULT_USERSELECTIONFORMULA;
      }
      LOGGER.log(Level.CONFIG,
          "User Selection formula: {0}", userSelectionFormula);

      gsaGroupPrefix =
          systemDoc.getItemValueString(NCCONST.SITM_GSAGROUPPREFIX);
      if (null != gsaGroupPrefix) {
        gsaGroupPrefix = gsaGroupPrefix.trim();
      }
      LOGGER.log(Level.CONFIG, "Group prefix: {0}", gsaGroupPrefix);

      // Number of docs to check when deleting
      deletionBatchSize = systemDoc.getItemValueInteger(
          NCCONST.SITM_DELETIONBATCHSIZE);
      if (deletionBatchSize < 1)  {
        LOGGER.log(Level.SEVERE,
            "Invalid setting for deletionBatchSize: {0}", deletionBatchSize);
        return false;
      }
      LOGGER.log(Level.CONFIG, "deletionBatchSize is {0}", deletionBatchSize);

      // Number of crawler threads to spawn
      numCrawlerThreads = systemDoc.getItemValueInteger(
          NCCONST.SITM_NUMCRAWLERTHREADS);
      if ((numCrawlerThreads < 0) || (numCrawlerThreads > 5)) {
        LOGGER.log(Level.SEVERE,
            "Invalid setting for numCrawlerThreads: {0}", numCrawlerThreads);
        return false;
      }
      LOGGER.log(Level.CONFIG, "numCrawlerThreads is {0}", numCrawlerThreads);

      // Load server regions
      LOGGER.log(Level.CONFIG, "Loading server domains.");
      NotesView serversView = db.getView(NCCONST.VIEWSERVERS);
      serversView.refresh();
      NotesViewNavigator svn = serversView.createViewNav();
      NotesViewEntry sve = svn.getFirst();
      while (null != sve) {
        Vector<?> columnVals = sve.getColumnValues();
        String domain = columnVals.elementAt(2).toString().toLowerCase();
        if (!Strings.isNullOrEmpty(domain)) {
          if (!domain.trim().startsWith(".")) {
            domain = "." + domain.trim();
          }
        }

        // This is a problem with the Notes Java API. When the
        // server field for a given region has 1 element we get a
        // String in the server column of the ViewEntry. When
        // the server field has more than 1 element we get one
        // ViewEntry for each server value, but the value
        // returned in the getColumnValues Vector is a Vector
        // with one element.
        String server;
        Object serverObject = columnVals.elementAt(0);
        if (serverObject instanceof String) {
          server = ((String) serverObject).toLowerCase();
        } else if (serverObject instanceof Vector) {
          Vector serverVector = (Vector) serverObject;
          if (serverVector.size() == 0) {
            LOGGER.log(Level.CONFIG, "Empty server value");
            continue;
          }
          server = ((String) serverVector.elementAt(0)).toLowerCase();
        } else {
            LOGGER.log(Level.CONFIG, "Unknown server value {0}", serverObject);
            continue;
        }
        LOGGER.log(Level.CONFIG,
            "Server {0} is in domain {1}", new Object[] { server, domain });
        serverDomainMap.put(server, domain);
        NotesViewEntry tmpsve = svn.getNext();
        sve.recycle();
        sve = tmpsve;
      }
      svn.recycle();
      serversView.recycle();
      if (0 == serverDomainMap.size()) {
        LOGGER.log(Level.SEVERE,
            "No regions have been configured for this connector.");
        return false;
      }

      // Load the mimetypes
      // TODO:  Fix the extension list to include new ones like .docx
      LOGGER.log(Level.CONFIG, "Loading mimetypes.");
      Vector<?> mimeTypeData = systemDoc.getItemValue(NCCONST.SITM_MIMETYPES);
      HashMap<String, String> tmpMimeExtnMap = new HashMap<String, String>();
      for (int i = 0; i < mimeTypeData.size(); i++) {
        String mimerecord = mimeTypeData.elementAt(i).toString();
        String ext = mimerecord.substring(0,
            mimerecord.indexOf('@')).toLowerCase();
        String mimetype = mimerecord.substring(mimerecord.indexOf('@') + 1);
        LOGGER.log(Level.CONFIG, "File extension {0} is set for mimetype {1}",
            new Object[] { ext, mimetype });
        tmpMimeExtnMap.put(ext, mimetype);
      }
      // Load into a new map then reassign to minimize threading issues
      mimeTypeMap = tmpMimeExtnMap;

      String retainMetaDataConfig =
          systemDoc.getItemValueString(NCCONST.SITM_RETAINMETADATA);
      retainMetaData = "yes".equalsIgnoreCase(retainMetaDataConfig);
      LOGGER.log(Level.CONFIG,
          "RetainMetaData configured value: {0}", retainMetaDataConfig);
      LOGGER.log(Level.CONFIG, "RetainMetaData: {0}", retainMetaData);

      systemDoc.recycle();
      LOGGER.log(Level.CONFIG, "Configuration successfully loaded.");
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, CLASS_NAME, e);
      return false;
    } finally {
      LOGGER.exiting(CLASS_NAME, METHOD);
    }
    return true;
  }

  public int getMaxCrawlQDepth() {
    return maxCrawlQDepth;
  }

  public int getDeletionBatchSize() {
    return deletionBatchSize;
  }

  public int getNumCrawlerThreads() {
    return numCrawlerThreads;
  }

  public NotesPollerNotifier getNotifier() {
    return npn;
  }

  public String getSpoolDir() {
    return spoolDir;
  }

  public String getDomain(String server) {
    String domain = serverDomainMap.get(server.toLowerCase());
    if (null == domain) {
      return "";
    } else {
      return domain;
    }
  }

  public String getMimeType(String extn) {
    String mimeType = mimeTypeMap.get(extn.toLowerCase());
    if (null == mimeType) {
      return "";
    } else {
      return mimeType;
    }
  }

  public int getCacheUpdateInterval() {
    return cacheUpdateInterval;
  }

  public String getDirectory() {
    return directory;
  }

  public String getUserNameFormula() {
    return userNameFormula;
  }

  public NotesUsernameType getUsernameType() {
    return usernameType;
  }

  public String getUserSelectionFormula() {
    return userSelectionFormula;
  }

  public String getGsaGroupPrefix() {
    return gsaGroupPrefix;
  }

  public boolean getRetainMetaData() {
    return retainMetaData;
  }

  public String getPassword() {
    return password;
  }

  public String getServer() {
    return server;
  }

  public int getMaxFileSize() {
    return maxFileSize;
  }

  public String getDatabase() {
    return database;
  }

  public NotesConnector getConnector() {
    return connector;
  }

  public NotesDocumentManager getNotesDocumentManager() {
    return notesDocManager;
  }

  @Override
  public AuthenticationManager getAuthenticationManager() {
    //TODO: Should we always return the same AuthenticationManager?
    return new NotesAuthenticationManager(this);
  }

  @Override
  public AuthorizationManager getAuthorizationManager() {
    //TODO: Should we always return the same AuthorizationManager?
    return new NotesAuthorizationManager(this);
  }

  @Override
  public synchronized NotesTraversalManager getTraversalManager() {
    if (traversalManager == null) {
      traversalManager = new NotesTraversalManager(this);
    }
    return traversalManager;
  }

  public synchronized NotesUserGroupManager getUserGroupManager()
      throws RepositoryException {
    if (userGroupManager == null) {
      userGroupManager = new NotesUserGroupManager(this);
    }
    return userGroupManager;
  }

  public boolean isExcludedExtension(String extension) {
    boolean excluded = false;

    // Trim leading . character
    int lastIndex = excludedExtns.lastIndexOf(extension);
    if (lastIndex != -1) {
      excluded = true;
    }
    return excluded;
  }

  public NotesSession createNotesSession() throws RepositoryException {
    final String METHOD = "createNotesSession";
    LOGGER.entering(CLASS_NAME, METHOD);
    NotesSession ns = null;
    try {
      // Create and recycle sessions as we need them to avoid memory leaks
      // Init the thread and try to login to validate credentials are correct
      connector.getSessionFactory().getNotesThread().sinitThread();
      ns = connector.getSessionFactory().createSessionWithFullAccess(password);
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, CLASS_NAME, e);
      throw new RepositoryException("Failed to create Notes Session", e);
    } finally {
      LOGGER.exiting(CLASS_NAME, METHOD);
    }
    return ns;
  }

  public void closeNotesSession(NotesSession ns) {
    final String METHOD = "closeNotesSession";
    LOGGER.entering(CLASS_NAME, METHOD);
    try {
      if (null != ns) {
        ns.recycle();
        ns = null;
      }
    } catch (Exception e) {
      // TODO: Should this be a WARNING, or does failure to
      // recycle a session lead to larger-scale connector
      // failure?
      LOGGER.log(Level.SEVERE, CLASS_NAME, e);
    } finally {
      try {
        connector.getSessionFactory().getNotesThread().stermThread();
      } catch (Throwable t) {
        LOGGER.log(Level.WARNING, "Error closing session", t);
      }

      LOGGER.exiting(CLASS_NAME, METHOD);
    }
  }
}
