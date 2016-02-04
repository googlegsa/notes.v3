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
  private Vector<String> ExcludedExtns = null;
  private int MaxFileSize;
  private String SpoolDir = null;
  private HashMap<String,String> MimeTypeMap = null;
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

  public NotesConnectorSession(NotesConnector Connector,
      NotesPollerNotifier connectorNpn, String Password,
      String Server, String Database) throws RepositoryException {
    final String METHOD = "NotesConnectorSession";
    server = Server;
    database = Database;
    password = Password;
    connector = Connector;
    NotesSession ns = null;
    boolean configValidated = false;
    LOGGER.entering(CLASS_NAME, METHOD);
    LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
        "NotesConnectorSession being created.");

    try {
      // Create and recycle sessions as we need them to avoid memory leaks
      // Init the thread and try to login to validate credentials are correct
      npn = connectorNpn;
      ns = createNotesSession();

      LOGGER.log(Level.INFO, "Notes version is {0}", ns.getNotesVersion());
      LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
          "Connector platform is " + ns.getPlatform());
      LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
          "Connector config database on server: " + server);
      LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
          "Connector config database path: " + database);
      LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
          "Connector local data directory: " +
          ns.getEnvironmentString(NCCONST.INIDIRECTORY, true));
      LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
          "Connector kittype: " +
          ns.getEnvironmentString(NCCONST.INIKITTYPE, true));
      LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
          "Connector keyfilename: " +
          ns.getEnvironmentString(NCCONST.INIKEYFILENAME, true));
      LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
          "Connector user: " +
          ns.getCommonUserName());
      LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
          "Connector serverkeyfilename: " +
          ns.getEnvironmentString(NCCONST.INISERVERKEYFILENAME, true));
      LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
          "Connector debug_outfile: " +
          ns.getEnvironmentString(NCCONST.INIDEBUGOUTFILE, true));

      NotesDatabase db = ns.getDatabase(server, database);
      configValidated = loadConfig(ns,db);

      db.recycle();
      notesDocManager = new NotesDocumentManager(this);
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, CLASS_NAME, e);
      throw new RepositoryException("NotesConnectorSession error", e);
    } finally {
      closeNotesSession(ns);
    }

    // If we could not validate our config then let the connector
    // manager know session creation has failed.
    if (!configValidated) {
      LOGGER.logp(Level.SEVERE, CLASS_NAME, METHOD,
          "!!!!!   Invalid Notes Connector System Configuration. !!!!!");
      throw new RepositoryException("Invalid system setup document.");
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
      LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
          "Loading configuration from system setup document.");

      NotesView vw = db.getView(NCCONST.VIEWSYSTEMSETUP);
      NotesDocument systemDoc = vw.getFirstDocument();
      if (null == systemDoc) {
        LOGGER.logp(Level.SEVERE, CLASS_NAME, METHOD,
            "System configuration document not found.");
        return false;
      }

      // "." means no file extension.  Replace with an empty string if it exists.
      ExcludedExtns = (Vector<String>)
          systemDoc.getItemValue(NCCONST.SITM_EXCLUDEDEXTENSIONS);
      for (int i = 0; i < ExcludedExtns.size(); i++ ) {
        LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
            "The following file extensions will be excluded " +
            ExcludedExtns.elementAt(i).toString());
        if (ExcludedExtns.elementAt(i).equals(".")) {
          ExcludedExtns.set(i, "");
        }
      }

      MaxFileSize = 1024 * 1024 *
          systemDoc.getItemValueInteger(NCCONST.SITM_MAXFILESIZE);
      LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
          "Maximum attachment size is " + MaxFileSize);

      // If 0, use the default value
      if (0 == MaxFileSize)
        MaxFileSize = 1024 * 1024 * NCCONST.DEFAULT_MAX_FILE_LIMIT;

      // Get the spool directory for processing attachments
      SpoolDir = systemDoc.getItemValueString(NCCONST.SITM_SPOOLDIR);
      if ((null == SpoolDir) || (0 == SpoolDir.length())) {
        SpoolDir = String.format("%s/%s",
            ns.getEnvironmentString(NCCONST.INIDIRECTORY, true),
            NCCONST.DEFAULT_ATTACHMENT_DIR);;
      }
      File sdir = new File(SpoolDir);

      // Make the directory and make sure we can write to it
      sdir.mkdirs();
      if (!sdir.isDirectory() || !sdir.canWrite()) {
        LOGGER.logp(Level.SEVERE, CLASS_NAME, METHOD,
            "Can't write to spool directory " + SpoolDir);
        return false;
      }
      LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
          "Attachment spool directory is set to " + SpoolDir);

      // Threshhold for polling
      maxCrawlQDepth = systemDoc.getItemValueInteger(
          NCCONST.SITM_MAXCRAWLQDEPTH);
      if (maxCrawlQDepth < 1)  {
        LOGGER.logp(Level.SEVERE, CLASS_NAME, METHOD,
            "Invalid setting for maxCrawlQDepth: " + maxCrawlQDepth);
        return false;
      }
      LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
          "maxCrawlQDepth is " + maxCrawlQDepth);

      // Time between user/group cache updates
      cacheUpdateInterval = systemDoc.getItemValueInteger(
          NCCONST.SITM_CACHEUPDATEINTERVAL);
      if (cacheUpdateInterval < 1)  {
        LOGGER.logp(Level.SEVERE, CLASS_NAME, METHOD,
            "Invalid setting for cache update interval: "
            + cacheUpdateInterval);
        return false;
      }
      LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
          "cacheUpdateInterval is " + cacheUpdateInterval);

      // Get the directory and see if we can open it
      directory = systemDoc.getItemValueString(
          NCCONST.SITM_DIRECTORY);
      LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
          "Path to Domino directory: " + directory);

      NotesDatabase dirDb = ns.getDatabase(this.getServer(), directory);
      dirDb.recycle();

      userNameFormula = systemDoc.getItemValueString(
          NCCONST.SITM_USERNAMEFORMULA);
      if (0 == userNameFormula.length()) {
        LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
            "User Name formula is empty - using default");
        userNameFormula = NCCONST.DEFAULT_USERNAMEFORMULA;
      }
      LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
            "User Name formula: " + userNameFormula);

      String usernameTypeConfig = systemDoc.getItemValueString(
          NCCONST.SITM_USERNAMETYPE);
      if (usernameTypeConfig != null && usernameTypeConfig.length() > 0) {
        usernameType = NotesUsernameType.findUsernameType(
            usernameTypeConfig.toUpperCase());
      }
      LOGGER.log(Level.CONFIG, "Notes username type: " + usernameType.name());

      userSelectionFormula = systemDoc.getItemValueString(
          NCCONST.SITM_USERSELECTIONFORMULA);
      if (0 == userSelectionFormula.length()) {
        LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
            "User Selection formula is empty - using default");
        userSelectionFormula = NCCONST.DEFAULT_USERSELECTIONFORMULA;
      }
      LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
          "User Selection formula: " + userSelectionFormula);

      gsaGroupPrefix =
          systemDoc.getItemValueString(NCCONST.SITM_GSAGROUPPREFIX);
      if (null != gsaGroupPrefix) {
        gsaGroupPrefix = gsaGroupPrefix.trim();
      }
      LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
          "Group prefix: " + gsaGroupPrefix);

      // Number of docs to check when deleting
      deletionBatchSize = systemDoc.getItemValueInteger(
          NCCONST.SITM_DELETIONBATCHSIZE);
      if (deletionBatchSize < 1)  {
        LOGGER.logp(Level.SEVERE, CLASS_NAME, METHOD,
            "Invalid setting for deletionBatchSize: " + deletionBatchSize);
        return false;
      }
      LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
          "deletionBatchSize is " + deletionBatchSize);

      // Number of crawler threads to spawn
      numCrawlerThreads = systemDoc.getItemValueInteger(
          NCCONST.SITM_NUMCRAWLERTHREADS);
      if ((numCrawlerThreads < 0) || (numCrawlerThreads > 5)) {
        LOGGER.logp(Level.SEVERE, CLASS_NAME, METHOD,
            "Invalid setting for numCrawlerThreads: " + numCrawlerThreads);
        return false;
      }
      LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
          "numCrawlerThreads is " + numCrawlerThreads);

      // Load server regions
      LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD, "Loading server domains.");
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
            LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD, "Empty server value");
            continue;
          }
          server = ((String) serverVector.elementAt(0)).toLowerCase();
        } else {
            LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
                "Unknown server value " + serverObject);
            continue;
        }
        LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
            "Server {0} is in domain {1}", new Object[] { server, domain });
        serverDomainMap.put(server, domain);
        NotesViewEntry tmpsve = svn.getNext();
        sve.recycle();
        sve = tmpsve;
      }
      svn.recycle();
      serversView.recycle();
      if (0 == serverDomainMap.size()) {
        LOGGER.logp(Level.SEVERE, CLASS_NAME, METHOD,
            "No regions have been configured for this connector.");
        return false;
      }

      // Load the mimetypes
      // TODO:  Fix the extension list to include new ones like .docx
      LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD, "Loading mimetypes.");
      Vector<?> mimeTypeData = systemDoc.getItemValue(NCCONST.SITM_MIMETYPES);
      HashMap<String, String> tmpMimeExtnMap = new HashMap<String, String>();
      for (int i = 0; i < mimeTypeData.size(); i++) {
        String mimerecord = mimeTypeData.elementAt(i).toString();
        String ext = mimerecord.substring(0,
            mimerecord.indexOf('@')).toLowerCase();
        String mimetype = mimerecord.substring(mimerecord.indexOf('@') + 1);
        String TmpMsg = String.format(
            "File extension %s is set for mimetype %s", ext, mimetype);
        LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD, TmpMsg);
        tmpMimeExtnMap.put(ext, mimetype);
      }
      // Load into a new map then reassign to minimize threading issues
      MimeTypeMap = tmpMimeExtnMap;

      String retainMetaDataConfig =
          systemDoc.getItemValueString(NCCONST.SITM_RETAINMETADATA);
      retainMetaData = "yes".equalsIgnoreCase(retainMetaDataConfig);
      LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
          "RetainMetaData configured value: " + retainMetaDataConfig);
      LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
          "RetainMetaData: " + retainMetaData);

      systemDoc.recycle();
      LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
          "Configuration successfully loaded.");
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
    return SpoolDir;
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
    String mimeType = MimeTypeMap.get(extn.toLowerCase());
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
    return MaxFileSize;
  }

  public String getDatabase() {
    return database ;
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
    int lastIndex = ExcludedExtns.lastIndexOf(extension);
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
        LOGGER.logp(Level.WARNING, CLASS_NAME, METHOD,
            "Error closing session", t);
      }

      LOGGER.exiting(CLASS_NAME, METHOD);
    }
  }
}
