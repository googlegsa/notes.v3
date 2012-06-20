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

public class NCCONST {
  // View names in the config database

  // View names.  Use aliases so the view can moved in the UI
  // without affecting the connector.

  // TODO:  Change this to an alias
  public static final String VIEWTEMPLATES = "($Templates)";
  // TODO:  Change this to an alias
  public static final String VIEWDATABASES = "($Databases)";
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
  public static final String TITM_METAFIELDS = "MetaFields";
  //public static final String TITM_AUTOGENERATE = "AutoGenerate";  -- NOT USED
  public static final String AUTOGENERATE_YES = "Yes";  // Size in MB

  //Item names used in forms documents
  public static final String FITM_LASTALIAS = "LastAlias";
  public static final String FITM_FIELDSTOINDEX = "FieldsToIndex";
  public static final String FITM_SEARCHRESULTSFORMULA = "SearchResultsFormula";
  public static final String FITM_DESCRIPTIONFORMULA = "DescriptionFormula";

  // Directory View
  /** These are default views in the Domino directory and must exist */
  public static final String DIRVIEW_PEOPLEGROUPFLAT = "($PeopleGroupsFlat)";
  public static final String DIRVIEW_SERVERACCESS = "($ServerAccess)";
  public static final String DIRVIEW_VIMUSERS = "($VimPeople)";
  public static final String DIRVIEW_VIMGROUPS = "($VimGroups)";
  public static final String DIRVIEW_USERS = "($Users)";

  public static final String DIRFORM_PERSON = "Person";
  public static final String DIRFORM_GROUP = "Group";

  // Group Items from Domino Directory
  public static final String GITM_LISTNAME = "ListName";
  public static final String GITM_GROUPTYPE = "GroupType";
  public static final String GITM_MEMBERS = "Members";
  public static final String DIR_ACCESSCONTROLGROUPTYPES = "02";

  // Person Items from Domino Directory
  public static final String PITM_FULLNAME = "FullName";

  //Item names used by the connector for crawling documents

  /** The source database for this document */
  public static final String NCITM_DB = "NC.Database";

  /** The UNID of the source document */
  public static final String NCITM_UNID = "NC.UNID";

  /** The state of the document */
  public static final String NCITM_STATE = "NC.State";

  /** The replica id of the database for this document */
  public static final String NCITM_REPLICAID = "NC.ReplicaID";

  /** The source server for this document */
  public static final String NCITM_SERVER = "NC.Server";

  /** The connector template for this document */
  public static final String NCITM_TEMPLATE = "NC.Template";

  /** The domain for this document */
  public static final String NCITM_DOMAIN = "NC.Domain";

  /** The type of authorization for the document (derived from the database) */
  public static final String NCITM_AUTHTYPE = "NC.AuthType";

  /** Field set to "true" for database acl crawl documents. */
  public static final String NCITM_DBACL = "NC.DbAcl";

  /** Field set to the inherit type for database acl crawl documents. */
  public static final String NCITM_DBACLINHERITTYPE = "NC.DbAclInheritType";

  /** Whether to lock the document or not */
  public static final String NCITM_LOCK = "NC.Lock";

  /** The readers of this document */
  /* Currently unused. */
  public static final String NCITM_DOCREADERS = "NC.DocReaders";

  /** The readers of this document */
  public static final String NCITM_DOCAUTHORS = "NC.DocAuthors";

  /** Users explicitly listed with no-access to the database */
  public static final String NCITM_DBNOACCESSUSERS = "NC.NoAccessUsers";

  /** The readers of this document */
  public static final String NCITM_DOCAUTHORREADERS = "NC.DocAuthorReaders";

  public static final String NCITM_CONFLICT = "$Conflict";

  /** Users with database access */
  public static final String NCITM_DBPERMITUSERS = "NC.DBPermitUsers";

  /** Groups with database access */
  public static final String NCITM_DBPERMITGROUPS = "NC.DBPermitGroups";

  /** Groups with no database access */
  public static final String NCITM_DBNOACCESSGROUPS = "NC.DBNoAccessGroups";

  /** Prefix for roles */
  public static final String NCITM_ROLEPREFIX = "NC.DBROLES";

  /** Possible States for documents */
  public static final String STATENEW = "New";
  // NOT USED Previously submitted document needs to be updated
  //public static final String STATEUPDATED = "Update";
  public static final String STATEDELETED =  "DeletePending";
  public static final String STATEINCRAWL = "InCrawl";
  public static final String STATEFETCHED = "Fetched";
  // NOT USED
  //public static final String STATEINSUBMIT = "InSubmit";
  public static final String STATEINDEXED = "Indexed";
  public static final String STATEERROR = "Error";


  // The following item names map directly to the SPI constants
  // for document properties i.e. The value for PROPNAME_DOCID
  // should be stored in ITM_DOCID.  We can't use the PROPNAME
  // constants since the colon : is not allowed in field names

  public static final String ITM_ACLGROUPS = "google.aclgroups";
  public static final String ITM_ACLUSERS = "google.aclusers";
  public static final String ITM_ACTION = "google.action";
  public static final String ITM_CONTENT = "google.content";

  /** If this is an attachment, we store the path here */
  public static final String ITM_CONTENTPATH = "google.contentpath";
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

  /** The authors of a document */
  public static final String ITM_GMETAWRITERNAME = "lnmeta.writername";

  /** The date a document was last updated */
  public static final String ITM_GMETALASTUPDATE = "lnmeta.modifydate";

  /** The data a document was created */
  public static final String ITM_GMETACREATEDATE = "lnmeta.createdate";

  /** The database title */
  public static final String ITM_GMETADATABASE = "lnmeta.database";

  /** The categories for a database in the connector configuration */
  public static final String ITM_GMETACATEGORIES = "lnmeta.categories";

  /** The replica servers for a database in the connector config */
  public static final String ITM_GMETASERVERS = "lnmeta.servers";

  /** The HTTP URL */
  public static final String ITM_GMETAWEBLINK = "lnmeta.weblink";

  /** The Notes URL */
  public static final String ITM_GMETANOTESLINK = "lnmeta.noteslink";

  /** The title generated by Search Results formula */
  public static final String ITM_GMETATOPIC = "lnmeta.topic";

  /** The description generated by Search REsults path */
  public static final String ITM_GMETADESCRIPTION = "lnmeta.description";

  /** The HTTP URL of this document */
  public static final String ITM_GMETADOCPATH = "lnmeta.docpath";

  /** The names of any attachments in this document */
  public static final String ITM_GMETAATTACHMENTS = "lnmeta.attachments";

  /** The filename of this attachment document */
  public static final String ITM_GMETAATTACHMENTFILENAME = "lnmeta.attachmentfilename";

  /** The names of any attachments in this document */
  public static final String ITM_GMETAALLATTACHMENTS = "lnmeta.allattachments";

  /** The form name for this document */
  public static final String ITM_GMETAFORM = "lnmeta.form";

  /** Replica servers for this document */
  public static final String ITM_GMETAREPLICASERVERS = "lnmeta.replicaservers";

  // Notes.INI environment variables
  public static final String INIDIRECTORY = "Directory";
  public static final String INIKEYFILENAME = "KeyFileName";
  public static final String INIKITTYPE = "KitType";
  public static final String INISERVERKEYFILENAME = "ServerKeyFileName";
  public static final String INIDEBUGOUTFILE="debug_outfile";

  // Item names in the system setup configuration
  public static final String SITM_EXCLUDEDEXTENSIONS = "ExcludeFileTypes";
  public static final String SITM_MAXFILESIZE = "MaxFileSize";
  public static final String SITM_MIMETYPES = "MimeTypes";
  public static final String SITM_SPOOLDIR = "SpoolDir";
  public static final String SITM_MAXCRAWLQDEPTH = "MaxCrawlQDepth";
  public static final String SITM_DELETIONBATCHSIZE = "DeletionBatchSize";
  public static final String SITM_NUMCRAWLERTHREADS = "NumCrawlerThreads";
  public static final String SITM_CACHEUPDATEINTERVAL = "CacheUpdateInterval";
  public static final String SITM_LASTCACHEUPDATE = "LastCacheUpdate";
  public static final String SITM_RETAINMETADATA = "RetainMetaData";

  /** Path to the Domino directory on the server */
  public static final String SITM_DIRECTORY = "Directory";

  /** Selection formula to determine which users will get processed */
  public static final String SITM_USERSELECTIONFORMULA = "UserSelectionFormula";

  /** Formula to generate GSA Identity (PVI) for authN and authZ */
  public static final String SITM_USERNAMEFORMULA = "UserNameFormula";

  /** Group prefix for group names sent to the GSA. */
  public static final String SITM_GSAGROUPPREFIX = "GSAGroupPrefix";


  // Default configuration for the connector

  /** Size in MB */
  public static final int DEFAULT_MAX_FILE_LIMIT = 30;

  /** In the notes data directory */
  public static final String DEFAULT_ATTACHMENT_DIR = "gsaSpool";
  public static final String DEFAULT_MIMETYPE = "text/plain";
  public static final String DEFAULT_DOCMIMETYPE = "text/plain";
  public static final String DEFAULT_TITLE = "Document title not found";
  public static final String DEFAULT_DESCRIPTION =
      "Document description not found";
  public static final String DEFAULT_USERNAMEFORMULA = "ShortName";
  public static final String DEFAULT_USERSELECTIONFORMULA =
      "Select Form = \"Person\"";

  // Domino properties
  public static final String PROPNAME_DESCRIPTION = "Description";
  public static final String PROPNAME_NCLASTUPDATE = "dom_lastmodified";
  public static final String PROPNAME_NCDATABASE = "dom_database";
  public static final String PROPNAME_NCCATEGORIES = "dom_dbcategories";
  public static final String PROPNAME_NCREPLICASERVERS = "dom_servers";
  public static final String PROPNAME_NCNOTESLINK = "dom_noteslink";
  public static final String PROPNAME_NCDOCPATH = "dom_docpath";
  public static final String PROPNAME_NCATTACHMENTS = "dom_docattachments";
  public static final String PROPNAME_NCATTACHMENTFILENAME = "dom_attachmentfilename";
  public static final String PROPNAME_NCALLATTACHMENTS = "dom_docallattachments";
  public static final String PROPNAME_NCAUTHORS = "dom_authors";
  public static final String PROPNAME_NCFORM = "dom_docform";
  public static final String PROPNAME_CREATEDATE = "dom_createdate";

  public static final String DB_ACL_INHERIT_TYPE_ANDBOTH = "AndBoth";
  public static final String DB_ACL_INHERIT_TYPE_PARENTOVERRIDES
      = "ParentOverrides";
}




