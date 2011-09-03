package com.google.enterprise.connector.notes;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Vector;
import java.util.Map;
import java.util.HashMap;

import lotus.domino.*;

import com.google.enterprise.connector.spi.AuthenticationManager;
import com.google.enterprise.connector.spi.AuthorizationManager;
import com.google.enterprise.connector.spi.Session;
import com.google.enterprise.connector.spi.TraversalManager;
import com.google.enterprise.connector.spi.RepositoryException;

public class NotesConnectorSession implements Session {
	private static final String CLASS_NAME = NotesConnectorSession.class.getName();
	private static final Logger _logger = Logger.getLogger(CLASS_NAME);
	private String server = null;
	private String database = null;
	private String password = "";
	private NotesConnector connector = null;
	private String ACLDbReplicaId;
	private Vector<String> ExcludedExtns = null;
	private int MaxFileSize;
	private String SpoolDir = null;
	private HashMap<String,String> MimeTypeMap = null;
	HashMap<String, String> ServerDomainMap = null;
	private NotesPollerNotifier npn = null;
	private int maxCrawlQDepth;
	private int deletionBatchSize;
	private int numCrawlerThreads;

	
	public NotesConnectorSession(NotesConnector Connector, NotesPollerNotifier connectorNpn, String Password, String Server, String Database) throws RepositoryException {
		final String METHOD = "NotesConnectorSession";
		server = Server;
		database = Database;
		password = Password;
		connector = Connector;
		lotus.domino.Session ns = null;
		boolean configValidated = false;
		_logger.entering(CLASS_NAME, METHOD);
		_logger.logp(Level.INFO, CLASS_NAME, METHOD, "NotesConnectorSession being created.");

		try {
			// Create and recycle sessions as we need them to avoid memory leaks
			// Init the thread and try to login to validate credentials are correct
			npn = connectorNpn;
			ns = createNotesSession();
			_logger.logp(Level.CONFIG, CLASS_NAME, METHOD, "Connector platform is " + ns.getPlatform());
			_logger.logp(Level.CONFIG, CLASS_NAME, METHOD, "Connector config database on server: " + server);			
			_logger.logp(Level.CONFIG, CLASS_NAME, METHOD, "Connector config database path: " + database);
			_logger.logp(Level.CONFIG, CLASS_NAME, METHOD, "Connector local data directory: " + ns.getEnvironmentString(NCCONST.INIDIRECTORY, true));
			_logger.logp(Level.CONFIG, CLASS_NAME, METHOD, "Connector kittype: " + ns.getEnvironmentString(NCCONST.INIKITTYPE, true));
			_logger.logp(Level.CONFIG, CLASS_NAME, METHOD, "Connector keyfilename: " + ns.getEnvironmentString(NCCONST.INIKEYFILENAME, true));
			_logger.logp(Level.CONFIG, CLASS_NAME, METHOD, "Connector serverkeyfilename: " + ns.getEnvironmentString(NCCONST.INISERVERKEYFILENAME, true));
			_logger.logp(Level.CONFIG, CLASS_NAME, METHOD, "Connector debug_outfile: " + ns.getEnvironmentString(NCCONST.INIDEBUGOUTFILE, true));
			
			lotus.domino.Database db = ns.getDatabase(server, database);
			configValidated = loadConfig(ns,db);

			db.recycle();
		}
		catch(Exception e)
		{
			_logger.log(Level.SEVERE, CLASS_NAME, e);
			throw(new RepositoryException ("NotesConnectorSession error", e));
		}
		finally
		{
			closeNotesSession(ns);
		}
		
		// If we could not validate our config then let the connector manager know session creation has failed.
		if(!configValidated) {
			_logger.logp(Level.SEVERE, CLASS_NAME, METHOD, "!!!!!   Invalid Notes Connector System Configuration. !!!!!");
			throw(new RepositoryException ("Invalid system setup document."));
		}
		_logger.exiting(CLASS_NAME, METHOD);
	}
	
	// Loads configuration from the system config doc
	// Returns true if the configuration loads succesfully
	// Returns false if an error occurs
	public boolean loadConfig(lotus.domino.Session ns, Database db) {
		final String METHOD = "loadConfiguration";
		_logger.entering(CLASS_NAME, METHOD);
		
		try {
			_logger.logp(Level.CONFIG, CLASS_NAME, METHOD, "Loading configuration from system setup document.");

			View vw = db.getView(NCCONST.VIEWSYSTEMSETUP);
			Document systemDoc = vw.getFirstDocument();
			if (null == systemDoc) {
				_logger.logp(Level.SEVERE, CLASS_NAME, METHOD, "System configuration document not found.");
				return (false);
			}

			ACLDbReplicaId = systemDoc.getItemValueString(NCCONST.SITM_ACLDBREPLICAID);
			if (null == ACLDbReplicaId) {
				_logger.logp(Level.SEVERE, CLASS_NAME, METHOD, "Access Control Database has not been set up.");
				return false;
			}
			if (ACLDbReplicaId.length() == 0) {
				_logger.logp(Level.SEVERE, CLASS_NAME, METHOD, "Access Control Database has not been set up.");
				return false;
			}

			
			// "." means no file extension.  Replace with an empty string if it exists.
			ExcludedExtns = (Vector<String>) systemDoc.getItemValue(NCCONST.SITM_EXCLUDEDEXTENSIONS);
			for (int i=0; i < ExcludedExtns.size(); i++ ) {
				_logger.logp(Level.CONFIG, CLASS_NAME, METHOD, "The following file extensions will be exluded " + ExcludedExtns.elementAt(i).toString());
				if (ExcludedExtns.elementAt(i).equals("."))
						ExcludedExtns.set(i, "");
			}

			
			MaxFileSize = 1024*1024*systemDoc.getItemValueInteger(NCCONST.SITM_MAXFILESIZE);
			_logger.logp(Level.CONFIG, CLASS_NAME, METHOD, "Maximum attachment size is " + MaxFileSize);

			// If 0, use the default value
			if (0 == MaxFileSize)
				MaxFileSize = 1024*1024*NCCONST.DEFAULT_MAX_FILE_LIMIT;

			// Get the spool directory for processing attachments
			SpoolDir = systemDoc.getItemValueString(NCCONST.SITM_SPOOLDIR);
			if ((null == SpoolDir) || (0 == SpoolDir.length()))
				SpoolDir = String.format("%s/%s", ns.getEnvironmentString(NCCONST.INIDIRECTORY, true), NCCONST.DEFAULT_ATTACHMENT_DIR);;
			java.io.File sdir = new java.io.File(SpoolDir);
			
			// Make the directory and make sure we can write to it
			sdir.mkdirs();
			if (!sdir.isDirectory() || !sdir.canWrite()) {
				_logger.logp(Level.SEVERE, CLASS_NAME, METHOD, "Can't write to spool directory " + SpoolDir);
				return false;
			}
			_logger.logp(Level.CONFIG, CLASS_NAME, METHOD, "Attachment spool directory is set to " + SpoolDir);
			
			// Threshhold for polling
			maxCrawlQDepth = systemDoc.getItemValueInteger(NCCONST.SITM_MAXCRAWLQDEPTH);
			if (maxCrawlQDepth < 1)  {
				_logger.logp(Level.SEVERE, CLASS_NAME, METHOD, "Invalid setting for maxCrawlQDepth: " + maxCrawlQDepth);
				return false;
			}
			_logger.logp(Level.CONFIG, CLASS_NAME, METHOD, "maxCrawlQDepth is " + maxCrawlQDepth);
			
			// Number of docs to check when deleting
			deletionBatchSize = systemDoc.getItemValueInteger(NCCONST.SITM_DELETIONBATCHSIZE);
			if (deletionBatchSize < 1)  {
				_logger.logp(Level.SEVERE, CLASS_NAME, METHOD, "Invalid setting for deletionBatchSize: " + deletionBatchSize);
				return false;
			}
			_logger.logp(Level.CONFIG, CLASS_NAME, METHOD, "deletionBatchSize is " + deletionBatchSize);

			// Number of crawler threads to spawn
			numCrawlerThreads = systemDoc.getItemValueInteger(NCCONST.SITM_NUMCRAWLERTHREADS);
			if ((numCrawlerThreads < 0) || (numCrawlerThreads > 5)) {
				_logger.logp(Level.SEVERE, CLASS_NAME, METHOD, "Invalid setting for numCrawlerThreads: " + numCrawlerThreads);
				return false;
			}
			_logger.logp(Level.CONFIG, CLASS_NAME, METHOD, "numCrawlerThreads is " + numCrawlerThreads);
			
			// Load server regions
			_logger.logp(Level.CONFIG, CLASS_NAME, METHOD, "Loading server domains.");
			View serversView = db.getView(NCCONST.VIEWSERVERS);
			serversView.refresh();
			ViewNavigator svn = serversView.createViewNav();
			ViewEntry sve = svn.getFirst();
			ViewEntry tmpsve;
			HashMap<String, String> TmpServerRegionMap = new HashMap<String, String>();			
			while (null != sve) {
				Vector<?> ColumnVals = sve.getColumnValues();
				// This is a problem with the Notes Java API.
				// when this column has 1 element we get a String
				// when this column has more than 1 element we get a Vector
				// The alternative is to test whether this is a vector before we start using it
				// i.e.  if ( ColumnVals.elementAt(0).getClass().getName().compareTo("java.util.Vector" ) 
				String TmpServer = ColumnVals.elementAt(0).toString().toLowerCase();
				if (TmpServer.charAt(0) == '[')
					TmpServer = TmpServer.substring(1);
				if (TmpServer.charAt(TmpServer.length()-1) == ']')
					TmpServer = TmpServer.substring(0, TmpServer.length()-1);
				String TmpDomain = ColumnVals.elementAt(2).toString().toLowerCase();
				String TmpMsg = String.format("Server %s is in domain %s", TmpServer, TmpDomain); 
				_logger.logp(Level.CONFIG, CLASS_NAME, METHOD, TmpMsg);
				TmpServerRegionMap.put(TmpServer, TmpDomain);
				tmpsve = svn.getNext();
				sve.recycle();
				sve = tmpsve;
			}
			svn.recycle();
			serversView.recycle();
			ServerDomainMap =  TmpServerRegionMap;
			if (0 == TmpServerRegionMap.size()) {
				_logger.logp(Level.SEVERE, CLASS_NAME, METHOD, "No regions have been configured for this connector.");
				return(false);
			}
			
			// Load the mimetypes
			// TODO:  Fix the extension list to include new ones like .docx
			_logger.logp(Level.CONFIG, CLASS_NAME, METHOD, "Loading mimetypes.");
			Vector<?> mimeTypeData = systemDoc.getItemValue(NCCONST.SITM_MIMETYPES);
			HashMap<String, String> tmpMimeExtnMap = new HashMap<String, String>();
			for (int i=0; i< mimeTypeData.size(); i++) {
				String mimerecord = mimeTypeData.elementAt(i).toString();
				String ext = mimerecord.substring(0, mimerecord.indexOf('@')).toLowerCase();
				String mimetype = mimerecord.substring(mimerecord.indexOf('@')+1);
				String TmpMsg = String.format("File extension %s is set for mimetype %s", ext, mimetype); 
				_logger.logp(Level.CONFIG, CLASS_NAME, METHOD, TmpMsg);
				tmpMimeExtnMap.put(ext, mimetype);
			}
			// Load into a new map then reassign to minimize threading issues
			MimeTypeMap = tmpMimeExtnMap;
			
			systemDoc.recycle();
			_logger.logp(Level.CONFIG, CLASS_NAME, METHOD, "Configuration successfully loaded.");
		}
		catch(Exception e)
		{
			_logger.log(Level.SEVERE, CLASS_NAME, e);
			return(false);
		}
		finally
		{
			_logger.exiting(CLASS_NAME, METHOD);
		}
		return true;
	}
	
	
	public int getMaxCrawlQDepth(){
		return maxCrawlQDepth;
	}

	public int getDeletionBatchSize(){
		return deletionBatchSize;
	}
	
	public int getNumCrawlerThreads(){
		return numCrawlerThreads;
	}
	
	public NotesPollerNotifier getNotifier(){
		return npn;
	}
	
	public String getSpoolDir(){
		return SpoolDir;
	}
	

	public String getDomain(String server) {
		String domain = ServerDomainMap.get(server.toLowerCase());
		if (null == domain)
			return "";
		else
			return domain;
	}

	public String getMimeType(String extn) {
		String mimeType = MimeTypeMap.get(extn.toLowerCase());
		if (null == mimeType)
			return ("");
		else
			return(mimeType);
	}
	
	public String getPassword() {
		return(password);
	}

	public String getServer() {
		return(server);
	}
	
	public int getMaxFileSize() {
		return(MaxFileSize);
	}

	public String getACLDbReplicaId() {
		return(ACLDbReplicaId);
	}		
	
	
	public String getDatabase() {
		return(database);
	}		
	
	public NotesConnector getConnector() {
		return(connector);
	}

	public AuthenticationManager getAuthenticationManager() {
		//TODO: Should we always return the same AuthenticationManager?
		final String METHOD = "getAuthenticatationManager";
		_logger.entering(CLASS_NAME, METHOD);
		_logger.exiting(CLASS_NAME, METHOD);
		return new NotesAuthenticationManager(this);
	}

	public AuthorizationManager getAuthorizationManager() {
		//TODO: Should we always return the same AuthorizationManager?
		final String METHOD = "getAuthorizationManager";
		_logger.entering(CLASS_NAME, METHOD);
		_logger.exiting(CLASS_NAME, METHOD);
		return new NotesAuthorizationManager(this);
	}

	public TraversalManager getTraversalManager() {
		//TODO: Should we always return the same TraversalManager?
		final String METHOD = "getTraversalManager";
		_logger.entering(CLASS_NAME, METHOD);
		return new NotesTraversalManager(this);
	}
	
	public boolean isExcludedExtension(String extension ) {
		boolean excluded = false;

		// Trim leading . character
		int lastIndex = ExcludedExtns.lastIndexOf(extension);
		if (lastIndex != -1)
			excluded = true;
		return(excluded);
	}
	
	public lotus.domino.Session createNotesSession () {
		final String METHOD = "createNotesSession";
		_logger.entering(CLASS_NAME, METHOD);
		lotus.domino.Session ns = null;
		try {
			// Create and recycle sessions as we need them to avoid memory leaks
			// Init the thread and try to login to validate credentials are correct
			NotesThread.sinitThread();
			ns = NotesFactory.createSessionWithFullAccess(password);
		}
		catch(Exception e)
		{
			_logger.log(Level.SEVERE, CLASS_NAME, e);
		}
		finally
		{
			_logger.exiting(CLASS_NAME, METHOD);
		}
		return (ns);
	}
	
	public void closeNotesSession (lotus.domino.Session ns) {
		final String METHOD = "closeNotesSession";
		_logger.entering(CLASS_NAME, METHOD);
		try {
			if (null != ns) {
				ns.recycle();
				ns = null;
			}
		}
		catch(Exception e)
		{
			_logger.log(Level.SEVERE, CLASS_NAME, e);
		}
		finally
		{
			NotesThread.stermThread();
			_logger.exiting(CLASS_NAME, METHOD);
		}
	}
}
