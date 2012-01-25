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

import com.google.common.annotations.VisibleForTesting;
import com.google.enterprise.connector.notes.client.SessionFactory;
import com.google.enterprise.connector.spi.Connector;
import com.google.enterprise.connector.spi.ConnectorShutdownAware;
import com.google.enterprise.connector.spi.RepositoryException;

import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotesConnector implements Connector, ConnectorShutdownAware  {
  private static final String CLASS_NAME = NotesConnector.class.getName();
  private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);
  private String password = "";
  private String server = null;
  private String database = null;
  private String workingDir = null;
  private String connectorName;
  private String policyAclPattern;
  private String googleFeedHost;
  private String gsaUsername;
  private String gsaPassword;
  private boolean shutdown = false;
  private boolean deleted = false;
  NotesConnectorSession ncs = null;
  @VisibleForTesting
  NotesMaintenanceThread maintThread = null;
  private NotesCrawlerThread crawlerThread = null;
  NotesPollerNotifier npn = null;
  Vector<NotesCrawlerThread> vecCrawlerThreads = null;
  SessionFactory sessionFactory;
  private final Object peopleCacheLock = new Object();

  NotesConnector() {
    this(
        "com.google.enterprise.connector.notes.client.notes.SessionFactoryImpl");
  }

  NotesConnector(String sessionFactoryClass) {
    final String METHOD = "NotesConnector";
    LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
        "NotesConnector being created.");
    try {
      sessionFactory = (SessionFactory)
          Class.forName(sessionFactoryClass).newInstance();
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
      throw new RuntimeException(e);
    }
  }

  /* @Override */
  public com.google.enterprise.connector.spi.Session login()
      throws RepositoryException {
    final String METHOD = "login";

    // We always want to return ok here

    // If we are all ready logged in, return the existing session
    // The Notes libraries take care of creating actual
    // connections to the server using RPCs
    if (null != ncs) {
      return ncs;
    }

    if (null == npn) {
      npn = new NotesPollerNotifier(this);
    }
    // If a session can't be created, the method below should
    // throw a RepositoryException
    ncs = new NotesConnectorSession(this, npn, password, server, database);

    // Start a crawler thread
    // Reset any documents before we start crawling

    if (null == maintThread) {
      maintThread = new NotesMaintenanceThread(this, ncs);
      maintThread.start();
    }

    /*
      if (null == crawlerThread) {
      NotesDatabasePoller.resetCrawlQueue(ncs);
      crawlerThread = new NotesCrawlerThread(this, ncs);
      crawlerThread.start();
      }
    */
    if (null == vecCrawlerThreads) {
      vecCrawlerThreads =
          new Vector<NotesCrawlerThread>(ncs.getNumCrawlerThreads());
      for (int i = 0; i < ncs.getNumCrawlerThreads(); i++) {
        vecCrawlerThreads.add(new NotesCrawlerThread(this, ncs));
        NotesCrawlerThread tmpThread = vecCrawlerThreads.elementAt(i);
        tmpThread.setName(NotesCrawlerThread.class.getSimpleName() + i);
        LOGGER.logp(Level.INFO, CLASS_NAME, METHOD,
            "Starting crawler thread " + tmpThread.getName());
        tmpThread.start();
      }
    }
    npn.setNumThreads(ncs.getNumCrawlerThreads() + 1);
    return ncs;
  }

  // The following setters are necessary for Spring to pass configuration to us
  public void setIdPassword(String idPassword) {
    final String METHOD = "setIdPassword";
    LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
        "Connector config Password being set");
    password = idPassword;
  }

  public void setServer(String server) {
    final String METHOD = "setServer";
    LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
        "Connector config Server = " + server);
    this.server = server;
  }

  public void setDatabase(String database) {
    final String METHOD = "setDatabase";
    LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
        "Connector config Database = " + database);
    this.database = database;
  }

  public void setGoogleConnectorWorkDir(String googleConnectorWorkDir) {
    final String METHOD = "setGoogleConnectorWorkDir";
    LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
        "Connector config GoogleConnectorWorkDir = " + googleConnectorWorkDir);
    workingDir = googleConnectorWorkDir;
  }

  public void setGoogleConnectorName(String googleConnectorName) {
    final String METHOD = "setGoogleConnectorName";
    LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
        "Connector config GoogleConnectorName = " + googleConnectorName);
    connectorName = googleConnectorName;
  }

  public void setPolicyAclPattern(String policyAclPattern) {
    final String METHOD = "setPolicyAclPattern";
    LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
        "Connector config policyAclPattern = " + policyAclPattern);
    this.policyAclPattern = policyAclPattern;
  }

  public void setGoogleFeedHost(String googleFeedHost) {
    final String METHOD = "setGoogleFeedHost";
    LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
        "Connector config Google feed host = " + googleFeedHost);
    this.googleFeedHost = googleFeedHost;
  }

  public void setGsaUsername(String gsaUsername) {
    final String METHOD = "setGsaUsername";
    LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
        "Connector config gsaUsername = " + gsaUsername);
    this.gsaUsername = gsaUsername;
  }

  public void setGsaPassword(String gsaPassword) {
    final String METHOD = "setGsaPassword";
    LOGGER.logp(Level.CONFIG, CLASS_NAME, METHOD,
        "Connector config gsaPassword set");
    this.gsaPassword = gsaPassword;
  }

  public String getIdPassword() {
    return password;
  }

  public String getServer() {
    return server;
  }

  public String getDatabase() {
    return database;
  }

  public String getGoogleConnectorWorkDir(String googleConnectorWorkDir) {
    return workingDir;
  }

  public String getGoogleConnectorName() {
    return connectorName;
  }

  public String getPolicyAclPattern() {
    return policyAclPattern;
  }

  public String getGsaProtocol() {
    return "http";
  }

  public String getGoogleFeedHost() {
    return googleFeedHost;
  }

  public int getGsaPort() {
    return 8000;
  }

  public String getGsaUsername() {
    return gsaUsername;
  }

  public String getGsaPassword() {
    return gsaPassword;
  }

  /**
   * Gets the <code>SessionFactory</code> for this Connector.
   *
   * @return the <code>SessionFactory</code>
   */
  SessionFactory getSessionFactory() {
    return sessionFactory;
  }

  Object getPeopleCacheLock() {
    return peopleCacheLock;
  }

  /* @Override */
  public void delete() {
    final String METHOD = "delete";
    LOGGER.logp(Level.INFO, CLASS_NAME, METHOD,
        "Connector is being DELETED!!!");
    deleted = true;
  }

  public boolean getDelete() {
    final String METHOD = "getDelete";
    LOGGER.entering(CLASS_NAME, METHOD);
    return deleted;
  }

  /* @Override */
  public void shutdown() {
    final String METHOD = "shutdown";

    // There are two possibilities here.  Set a latch variable
    // and wait is on option.
    // TODO:  Use signalling to the other threads to get them to shutdown
    LOGGER.logp(Level.INFO, CLASS_NAME, METHOD,
        "Connector is shutting down. Waking all threads!!!");
    shutdown = true;
    if (null != vecCrawlerThreads) {
      for (int i = 0; i < vecCrawlerThreads.size() + 1; i++)  {
        // Notify each CrawlerThread and the MaintenanceThread
        npn.wakeWorkers();
      }
      try {
        java.lang.Thread.sleep(5000);
      } catch (Exception e) {
        LOGGER.log(Level.SEVERE, CLASS_NAME, e);
      }
      npn.wakeWorkers();
    }
  }

  // TODO: consider renaming to isShutdown.
  public boolean getShutdown() {
    return shutdown;
  }
}
