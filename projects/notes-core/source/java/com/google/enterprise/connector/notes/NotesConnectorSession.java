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
import com.google.enterprise.connector.spi.AuthenticationManager;
import com.google.enterprise.connector.spi.AuthorizationManager;
import com.google.enterprise.connector.spi.RepositoryException;
import com.google.enterprise.connector.spi.Session;
import com.google.enterprise.connector.spi.TraversalManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotesConnectorSession implements Session {
  private static final String CLASS_NAME = NotesConnectorSession.class.getName();
  private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);
  private String server = null;
  private String database = null;
  private String password = "";
  private NotesConnector connector = null;
  private Vector<String> ExcludedExtns = null;
  private int MaxFileSize;
  private String SpoolDir = null;
  private HashMap<String,String> MimeTypeMap = null;
  HashMap<String, String> ServerDomainMap = null;
  private NotesPollerNotifier npn = null;
  private int maxCrawlQDepth;
  private int deletionBatchSize;
  private int numCrawlerThreads;
  private int cacheUpdateInterval;
  private String directory = null;
  private String userNameFormula = null;
  private String userSelectionFormula = null;

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
          "Connector serverkeyfilename: " +
          ns.getEnvironmentString(NCCONST.INISERVERKEYFILENAME, true));
      LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
          "Connector debug_outfile: " +
          ns.getEnvironmentString(NCCONST.INIDEBUGOUTFILE, true));

      NotesDatabase db = ns.getDatabase(server, database);
      configValidated = loadConfig(ns,db);

      db.recycle();
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
      java.io.File sdir = new java.io.File(SpoolDir);

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

      // Number of docs to check when deleting
      cacheUpdateInterval = systemDoc.getItemValueInteger(
          NCCONST.SITM_CACHEUPDATEINTERVAL);
      if (cacheUpdateInterval < 1)  {
        LOGGER.logp(Level.SEVERE, CLASS_NAME, METHOD,
            "Invalid setting for cache update interval: " + cacheUpdateInterval);
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

      userSelectionFormula = systemDoc.getItemValueString(NCCONST.SITM_USERSELECTIONFORMULA);
      if (0 == userSelectionFormula.length()) {
        LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
            "User Selection formula is empty - using default");
    	  userSelectionFormula = NCCONST.DEFAULT_USERSELECTIONFORMULA;
      }
      LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
          "User Selection formula: " + userSelectionFormula);
      
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
      NotesViewEntry tmpsve;
      HashMap<String, String> TmpServerRegionMap =
          new HashMap<String, String>();
      while (null != sve) {
        Vector<?> ColumnVals = sve.getColumnValues();
        // This is a problem with the Notes Java API.
        // when this column has 1 element we get a String
        // when this column has more than 1 element we get a Vector
        // The alternative is to test whether this is a vector
        // before we start using it i.e.
        // if (ColumnVals.elementAt(0).getClass().getName()
        //    .compareTo("java.util.Vector")
        String TmpServer = ColumnVals.elementAt(0).toString().toLowerCase();
        if (TmpServer.charAt(0) == '[') {
          TmpServer = TmpServer.substring(1);
        }
        if (TmpServer.charAt(TmpServer.length() - 1) == ']') {
          TmpServer = TmpServer.substring(0, TmpServer.length() - 1);
        }
        String TmpDomain = ColumnVals.elementAt(2).toString().toLowerCase();
        String TmpMsg = String.format("Server %s is in domain %s",
            TmpServer, TmpDomain);
        LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD, TmpMsg);
        TmpServerRegionMap.put(TmpServer, TmpDomain);
        tmpsve = svn.getNext();
        sve.recycle();
        sve = tmpsve;
      }
      svn.recycle();
      serversView.recycle();
      ServerDomainMap =  TmpServerRegionMap;
      if (0 == TmpServerRegionMap.size()) {
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
    String domain = ServerDomainMap.get(server.toLowerCase());
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

  public String getUserSelectionFormula() {
    return userSelectionFormula;
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

  /* @Override */
  public AuthenticationManager getAuthenticationManager() {
    //TODO: Should we always return the same AuthenticationManager?
    return new NotesAuthenticationManager(this);
  }

  /* @Override */
  public AuthorizationManager getAuthorizationManager() {
    //TODO: Should we always return the same AuthorizationManager?
    return new NotesAuthorizationManager(this);
  }

  /* @Override */
  public TraversalManager getTraversalManager() {
    //TODO: Should we always return the same TraversalManager?
    return new NotesTraversalManager(this);
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
