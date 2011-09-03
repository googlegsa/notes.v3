package com.google.enterprise.connector.notes;

public class NCCONST {
	// View names in the config database
	// View names.  Use aliases so the view can moved in the UI without affecting the connector.
	public static final String VIEWTEMPLATES = "($Templates)";  //TODO:  Change this to an alias
	public static final String VIEWDATABASES = "($Databases)";  //TODO:  Change this to an alias
	public static final String VIEWCRAWLQ = "NCCrawlQ";
	public static final String VIEWSUBMITQ = "NCSubmitQ";
	public static final String VIEWINDEXED = "NCIndexed";
	public static final String VIEWINCRAWL = "NCInCrawl";
	public static final String VIEWSYSTEMSETUP = "NCSystemSetup";
	public static final String VIEWSERVERS = "NCServers";
	public static final String VIEWSECURITY = "NCSecurity";
	
	// Form Names
	public static final String ITMFORM = "Form";
	public static final String FORMCRAWLREQUEST = "NCCrawlRequest";

	// Item names used in the Database configuration document
	public static final String DITM_DBNAME = "DatabaseName";
	public static final String DITM_LASTUPDATE = "LastUpdated";
	public static final String DITM_UPDATEFREQUENCY = "UpdateFrequency";
	public static final String DITM_TEMPLATE = "Template";
	public static final String DITM_CRAWLENABLED = "Enabled";
	public static final String DITM_STOPPED = "Stopped";
	public static final String DITM_DOMAIN = "Domain";
	public static final String DITM_DBCATEGORIES = "DatabaseCategories";
	public static final String DITM_DREPLICASERVERSS = "ReplicaServers";
	public static final String DITM_AUTHTYPE = "DbAuthType";
	public static final String DITM_REPLICAID = "DbRepID";
	public static final String DITM_SERVER = "Server";
	public static final String DITM_REPLICASERVERS = "ReplicaServers";
	public static final String DITM_LOCKATTRIBUTE = "LockAttribute";
	public static final String DITM_CHECKDELETIONS = "CheckDeletions";
	public static final String DITM_ACLTEXT = "DbACLText";
	

	// Authorization methods specified in the field ITM_DAUTHTYPE
	public static final String AUTH_NONE = "none";
	public static final String AUTH_ACL = "acl";
	public static final String AUTH_CONNECTOR ="connector";

	//Item names used in the template documents
	public static final String TITM_TEMPLATENAME = "TemplateName";
	public static final String TITM_SEARCHSTRING = "SearchString";
	public static final String TITM_SEARCHRESULTSFIELDS = "SearchResultsFields";
	public static final String TITM_DESCRIPTIONFIELDS = "DescriptionFields";
	//public static final String TITM_AUTOGENERATE = "AutoGenerate";  -- NOT USED
	public static final String AUTOGENERATE_YES = "Yes";  // Size in MB
	
	//Item names used in forms documents
	public static final String FITM_LASTALIAS = "LastAlias";
	public static final String FITM_FIELDSTOINDEX = "FieldsToIndex";
	public static final String FITM_SEARCHRESULTSFORMULA = "SearchResultsFormula";
	public static final String FITM_DESCRIPTIONFORMULA = "DescriptionFormula";
	
	
	// Access Control Database Views
	public static final String VIEWACPEOPLE = "($People)";
	public static final String ACITM_GROUPS = "Groups";
	public static final String ACITM_NOTESNAME = "NotesName";
	
	//Item names used by the connector for crawling documents 
	public static final String NCITM_DB = "NC.Database"; 		// The source database for this 
	public static final String NCITM_UNID = "NC.UNID";			// The UNID of the source document
	public static final String NCITM_STATE = "NC.State";			// The state of the document
	public static final String NCITM_REPLICAID = "NC.ReplicaID"; 		// The source database for this 
	public static final String NCITM_SERVER = "NC.Server"; 		// The source database for this 
	public static final String NCITM_TEMPLATE = "NC.Template"; 		// The source database for this
	public static final String NCITM_DOMAIN = "NC.Domain";	// The domain for this document
	public static final String NCITM_AUTHTYPE = "NC.AuthType"; // The type of authorization for the document
	public static final String NCITM_LOCK = "NC.Lock"; // Whether to lock the document or not
	public static final String NCITM_DOCREADERS = "NC.DocReaders"; // The readers of this document
	public static final String NCITM_DOCAUTHORS = "NC.DocAuthors"; // The readers of this document
	public static final String NCITM_DBNOACCESSUSERS = "NC.NoAccessUsers";    // Users explicitly listed with no-access
	public static final String NCITM_DOCAUTHORREADERS = "NC.DocAuthorReaders"; // The readers of this document
	public static final String NCITM_CONFLICT = "$Conflict";

	public static final String NCITM_DBPERMITUSERS = "NC.DBPermitUsers";  // Users with access 
	public static final String NCITM_DBPERMITGROUPS = "NC.DBPermitGroups";  // Users with access 
	public static final String NCITM_ROLEPREFIX = "NC.DBROLES";  // Prefix for roles

	
	public static final String STATENEW = "New";				// New document to be crawled
	//public static final String STATEUPDATED = "Update";			// NOT USED Previously submitted document needs to be updated
	public static final String STATEDELETED =  "DeletePending";
	public static final String STATEINCRAWL = "InCrawl";				// Possible States for documents
	public static final String STATEFETCHED = "Fetched";				// Possible States for documents
	//public static final String STATEINSUBMIT = "InSubmit";				// NOT USED Possible States for documents
	public static final String STATEINDEXED = "Indexed";				// Possible States for documents
	public static final String STATEERROR = "Error";				// Possible States for documents


	// The following item names map directly to the SPI constants for document properties
	// i.e. The value for PROPNAME_DOCID should be stored in ITM_DOCID.  
	// We can't use the PROPNAME constancts since the colon : is not allowed in field names
	public static final String ITM_ACLGROUPS = "google.aclgroups";
	public static final String ITM_ACLUSERS = "google.aclusers";
	public static final String ITM_ACTION = "google.action";
	public static final String ITM_CONTENT = "google.content";
	public static final String ITM_CONTENTPATH = "google.contentpath"; // If this is an attachment, we store the path here
	public static final String ITM_CONTENTURL = "google.contenturl";
	public static final String ITM_DISPLAYURL = "google.displayurl";
	public static final String ITM_DOCID = "google.docid";
	public static final String ITM_ISPUBLIC = "google.ispublic";
	public static final String ITM_LASTMODIFIED = "google.lastmodified";
	public static final String ITM_MIMETYPE = "google.mimetype";
	public static final String ITM_SEARCHURL = "google.searchurl" ;
	public static final String ITM_SECURITYTOKEN = "google.securitytoken";
	public static final String ITM_TITLE = "google.title";
	public static final String ITM_LOCK = "google.lock";
	
	// The following are standard meta fields for the connector
	public static final String ITM_GMETAWRITERNAME = "lnmeta.writername";		// The authors of a document
	public static final String ITM_GMETALASTUPDATE = "lnmeta.modifydate";	// The date a document was last updated
	public static final String ITM_GMETACREATEDATE = "lnmeta.createdate";	// The data a document was created
	public static final String ITM_GMETADATABASE = "lnmeta.database";		// The database title
	public static final String ITM_GMETACATEGORIES = "lnmeta.categories";	// The categories for a database in the connector configuration
	public static final String ITM_GMETASERVERS = "lnmeta.servers";			// The replica servers for a database in the connector config
	public static final String ITM_GMETAWEBLINK = "lnmeta.weblink";			// The HTTP URL
	public static final String ITM_GMETANOTESLINK = "lnmeta.noteslink";		// The Notes URL
	public static final String ITM_GMETATOPIC = "lnmeta.topic";				// The title generated by Search Results formula
	public static final String ITM_GMETADESCRIPTION = "lnmeta.description";			// The description generated by Search REsults path
	public static final String ITM_GMETADOCPATH = "lnmeta.docpath";			// The HTTP URL of this document
	public static final String ITM_GMETAATTACHMENTS = "lnmeta.attachments";	// The names of any attachments in this document
	public static final String ITM_GMETAALLATTACHMENTS = "lnmeta.allattachments";	// The names of any attachments in this document
	public static final String ITM_GMETAFORM = "lnmeta.form";				// The form name for this documen
	public static final String ITM_GMETAREPLICASERVERS = "lnmeta.replicaservers";				// Replica servers for this document
	
	// Notes.INI environment variables
	public static final String INIDIRECTORY = "Directory";
	public static final String INIKEYFILENAME = "KeyFileName";
	public static final String INIKITTYPE = "KitType";
	public static final String INISERVERKEYFILENAME = "ServerKeyFileName";
	public static final String INIDEBUGOUTFILE="debug_outfile";
	
	// Item names in the system setup configuration
	public static final String SITM_ACLDBREPLICAID = "AccessDbRepID";
	public static final String SITM_EXCLUDEDEXTENSIONS = "ExcludeFileTypes";
	public static final String SITM_MAXFILESIZE = "MaxFileSize";
	public static final String SITM_MIMETYPES = "MimeTypes";
	public static final String SITM_SPOOLDIR = "SpoolDir";
	public static final String SITM_MAXCRAWLQDEPTH = "MaxCrawlQDepth";
	public static final String SITM_DELETIONBATCHSIZE = "DeletionBatchSize";
	public static final String SITM_NUMCRAWLERTHREADS = "NumCrawlerThreads";
	

	// Default configuration for the connector
	public static final int DEFAULT_MAX_FILE_LIMIT = 30;  // Size in MB
	public static final String DEFAULT_ATTACHMENT_DIR = "gsaSpool";  // In the notes data directory
	public static final String DEFAULT_MIMETYPE = "text/plain";
	public static final String DEFAULT_DOCMIMETYPE = "text/plain";  // 
	public static final String DEFAULT_TITLE = "Document title not found";
	public static final String DEFAULT_DESCRIPTION = "Document description not found";
	
	// Domino properties  
	public static final String PROPNAME_DESCRIPTION = "Description";
	public static final String PROPNAME_NCLASTUPDATE = "dom_lastmodified";
	public static final String PROPNAME_NCDATABASE = "dom_database";
	public static final String PROPNAME_NCCATEGORIES = "dom_dbcategories";
	public static final String PROPNAME_NCREPLICASERVERS = "dom_servers";
	public static final String PROPNAME_NCNOTESLINK = "dom_noteslink";
	public static final String PROPNAME_NCDOCPATH = "dom_docpath";
	public static final String PROPNAME_NCATTACHMENTS = "dom_docattachments";
	public static final String PROPNAME_NCALLATTACHMENTS = "dom_docallattachments";
	public static final String PROPNAME_NCAUTHORS = "dom_authors";
	public static final String PROPNAME_NCFORM = "dom_docform";
	public static final String PROPNAME_CREATEDATE = "dom_createdate";
}




