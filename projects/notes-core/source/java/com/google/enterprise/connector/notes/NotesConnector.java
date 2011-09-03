package com.google.enterprise.connector.notes;


import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Vector;
import com.google.enterprise.connector.spi.*;

public class NotesConnector implements Connector, ConnectorShutdownAware  {
	private static final String CLASS_NAME = NotesConnector.class.getName();
	private static final Logger _logger = Logger.getLogger(CLASS_NAME);
	private String password = "";
	private String server = null;
	private String database = null;
	private String workingDir = null;
	private boolean shutdown = false;
	private boolean deleted = false;
	NotesConnectorSession ncs = null;
	private NotesMaintenanceThread maintThread = null;
	private NotesCrawlerThread crawlerThread = null;
	NotesPollerNotifier npn = null;
	Vector<NotesCrawlerThread> vecCrawlerThreads = null;
	
	NotesConnector() {
		final String METHOD = "NotesConnector";
		_logger.logp(Level.INFO, CLASS_NAME, METHOD, "NotesConnector being created.");
	}

	public Session login() throws RepositoryException {
		final String METHOD = "login";


		// We always want to return ok here
		_logger.entering(CLASS_NAME, METHOD);
		
		// If we are all ready logged in, return the existing session
		// The Notes libraries take care of creating actual connections to the server using RPCs
		if (null != ncs)
			return ncs;
		
		
		if (null == npn)
			npn = new NotesPollerNotifier(this);
		// If a session can't be created, the method below should throw a RepositoryException
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
			vecCrawlerThreads = new Vector<NotesCrawlerThread>(ncs.getNumCrawlerThreads());
			for (int i=0; i<ncs.getNumCrawlerThreads(); i++) {
				vecCrawlerThreads.add(new NotesCrawlerThread(this, ncs));
				NotesCrawlerThread tmpThread = vecCrawlerThreads.elementAt(i);
				tmpThread.setName(NotesCrawlerThread.class.getSimpleName()+i);
				_logger.logp(Level.INFO, CLASS_NAME, METHOD, "Starting crawler thread " + tmpThread.getName());
				tmpThread.start();
			}
		}
		npn.setNumThreads(ncs.getNumCrawlerThreads()+1);
		return ncs;
	}
	
	// The following setters are necessary for Spring to pass configuration to us
	public void setIDPassword(String IDPassword) {
		final String METHOD = "setNotesIDPassword";
		_logger.logp(Level.INFO, CLASS_NAME, METHOD, "Connector config Password being set");
		password = IDPassword;
	}

	public void setServer(String Server) {
		final String METHOD = "setServer";
		_logger.logp(Level.INFO, CLASS_NAME, METHOD, "Connector config Server=" + Server);
		server = Server;
	}
	public void setDatabase(String Database) {
		final String METHOD = "setDatabase";
		_logger.logp(Level.INFO, CLASS_NAME, METHOD, "Connector config Database=" + Database);
		database = Database;
	}
	
	public void setGoogleConnectorWorkDir(String googleConnectorWorkDir) {
		final String METHOD = "setGoogleConnectorWorkDir";
		_logger.logp(Level.INFO, CLASS_NAME, METHOD, "Connector config GoogleConnectorWorkDir=" + googleConnectorWorkDir);
		workingDir = googleConnectorWorkDir;
	}

	public String getIDPassword() {
		return(password);
	}

	public String getServer() {
		return(server);
	}

	public String getDatabase() {
		return(database);
	}

	public String getGoogleConnectorWorkDir(String googleConnectorWorkDir) {
		return(workingDir);
	}
	
	
	public void delete() {
		final String METHOD = "delete";
		_logger.logp(Level.INFO, CLASS_NAME, METHOD, "Connector is being DELETED!!!");
		deleted = true;
	}

	public boolean getDelete() {
		final String METHOD = "getDelete";
		_logger.entering(CLASS_NAME, METHOD);
		return(deleted);
	}
	
	public void shutdown() {
		final String METHOD = "shutdown";
		// There are two possibilities here.  Set a latch variable and wait is on option.
		// TODO:  Use signalling to the other threads to get them to shutdown
		_logger.entering(CLASS_NAME, METHOD);
		_logger.logp(Level.INFO, CLASS_NAME, METHOD, "Connector is shutting down. Waking all threads!!!");
		shutdown = true;
		for(int i=0; i<vecCrawlerThreads.size()+1; i++)  // Notify each CrawlerThread and the MaintenanceThread
			npn.wakeWorkers();
		try {
			java.lang.Thread.sleep(5000);
		}
		catch (Exception e) {
			_logger.log(Level.SEVERE, CLASS_NAME, e);
		}
		npn.wakeWorkers();
		_logger.exiting(CLASS_NAME, METHOD);
	}

	public boolean getShutdown() {
		//final String METHOD = "getShutdown";
		//_logger.entering(CLASS_NAME, METHOD);
		return(shutdown);
	}

}
