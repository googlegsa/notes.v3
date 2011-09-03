package com.google.enterprise.connector.notes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import lotus.domino.*;
import java.util.Vector;

import javax.swing.text.html.HTMLDocument.Iterator;

import com.google.enterprise.connector.spi.AuthenticationIdentity;
import com.google.enterprise.connector.spi.AuthorizationManager;
import com.google.enterprise.connector.spi.AuthorizationResponse;
import com.google.enterprise.connector.spi.RepositoryException;

class NotesAuthorizationManager implements AuthorizationManager {
	private static final String CLASS_NAME = NotesAuthorizationManager.class.getName();
	private static final Logger _logger = Logger.getLogger(CLASS_NAME);
	
	private NotesConnectorSession ncs = null;

	
	public NotesAuthorizationManager(NotesConnectorSession session){
		final String METHOD = "NotesAuthorizationManager";
		_logger.logp(Level.INFO, CLASS_NAME, METHOD, "NotesAuthorizationManager being created.");

		ncs = session;
	}

	protected String getRepIdFromDocId(String docId) {
		int start = docId.indexOf('/', 7);  // Find the first slash after http://
		return (docId.substring(start+1, start+17)); 
	}
	
	protected String getUNIDFromDocId(String docId) {
		int start = docId.indexOf('/', 7);  // Find the first slash after http://
		return (docId.substring(start+20, start+52)); 
	}

	// Explain Lotus Notes Authorization Rules
	
	
	// TODO: Add LRU Cache for ALLOW/DENY
	public Collection<AuthorizationResponse> authorizeDocids(Collection<String> docIds, AuthenticationIdentity id) {
		final String METHOD = "authorizeDocids";
		String NotesName = null;
		Vector<String> UserGroups = null;
		_logger.entering(CLASS_NAME, METHOD);
		ArrayList<AuthorizationResponse> authorized = new ArrayList<AuthorizationResponse>(docIds.size());
		String pvi = id.getUsername();
		try {
			_logger.logp(Level.CONFIG, CLASS_NAME, METHOD, "Authorizing documents for user " + pvi);
			NotesThread.sinitThread();
			Session ns = NotesFactory.createSessionWithFullAccess(ncs.getPassword());
			lotus.domino.Database cdb = ns.getDatabase(ncs.getServer(), ncs.getDatabase());

			Database acdb = ns.getDatabase(null, null);
			_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "Opening ACL database " + ncs.getServer() + " : " + ncs.getACLDbReplicaId());

			acdb.openByReplicaID(ncs.getServer(), ncs.getACLDbReplicaId());
			View securityView = cdb.getView(NCCONST.VIEWSECURITY);
			View people = acdb.getView(NCCONST.VIEWACPEOPLE);

			// Resolve the PVI to their Notes names and groups
			Document personDoc = people.getDocumentByKey(id.getUsername(), true);
			if (null == personDoc) {
				_logger.logp(Level.WARNING, CLASS_NAME, METHOD, "Person not found in ACL database. DENY all docs. " + pvi);
			}
			else {
				NotesName = personDoc.getItemValueString(NCCONST.ACITM_NOTESNAME).toLowerCase();
				_logger.logp(Level.FINER, CLASS_NAME, METHOD, "PVI:NOTESNAME mapping is " + pvi + ":" + NotesName);
				UserGroups = (Vector<String>)personDoc.getItemValue(NCCONST.ACITM_GROUPS);
				for (int i=0; i<UserGroups.size(); i++) {
					UserGroups.set(i, UserGroups.elementAt(i).toString().toLowerCase());
				}
				_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "Groups for " + pvi + " are: " + UserGroups.toString());
			}


			// The first document in the category will always be the database document

			for (String docId : docIds) {
				// Extract the database and UNID from the URL
				String repId = getRepIdFromDocId(docId); 
				String unid = getUNIDFromDocId(docId);
				_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "Authorizing documents for user " + pvi + " : " + repId + " : " + unid);
	
				if (null == personDoc) {
					// We didn't find this person, so deny all.  Alternatively we could return INDETERMINATE...
					authorized.add(new AuthorizationResponse(false, docId));
					continue;
				}
				
				boolean docallow = true;
				// Get the category from the security view for this database
				ViewNavigator secVN = securityView.createViewNavFromCategory(repId);
				// The first document in the category is ALWAYS the database document
				Document dbdoc = secVN.getFirstDocument().getDocument();
				// If there is more than one document in the category, we will need to check for document level reader access lists
				int securityCount = secVN.getCount();
				_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "Count for viewNavigator is  " + securityCount);

				boolean dballow = checkDatabaseAccess(NotesName, dbdoc, UserGroups);

				// Only check document level security if it exists
				if (dballow && (securityCount > 1)) {  
					Vector<String> searchKey = new Vector<String>(3);
					searchKey.addElement(repId);
					searchKey.addElement("2");	// Database documents are type '1' in this view.  Crawler documents are type '2'
					searchKey.addElement(unid);
					_logger.logp(Level.CONFIG, CLASS_NAME, METHOD, "Search key is  " + searchKey.toString());
					Document crawlDoc = securityView.getDocumentByKey(searchKey, true); 
					if (crawlDoc != null) {
						// Found a crawldoc, so we will need to check document level access
						docallow = checkDocumentReaders(NotesName, UserGroups, crawlDoc, dbdoc);
						crawlDoc.recycle();
					}
					else {
						// There is no crawldoc with reader lists restrictions so nothing to do
						_logger.logp(Level.CONFIG, CLASS_NAME, METHOD, "No document level security for " + unid);
					}
				}
				secVN.recycle();
				dbdoc.recycle();
				boolean allow = docallow && dballow;
				_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "Finale auth decision is " + allow + " " + unid);					
				authorized.add(new AuthorizationResponse(allow, docId));
			}

			personDoc.recycle();
			securityView.recycle();
			people.recycle();
			cdb.recycle();
			acdb.recycle();
			ns.recycle();
			_logger.logp(Level.CONFIG, CLASS_NAME, METHOD, "Connector config database path: ");
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
		if(_logger.isLoggable(Level.FINEST)){
			for (int i=0; i<authorized.size(); i++) {
				AuthorizationResponse ar =authorized.get(i);
				_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "AuthorizationResponse: " + ar.getDocid() + " : " + ar.isValid());					
			}
		}
		return authorized;
	}

	protected String getCommonName(String NotesName) {
		if (NotesName.startsWith("cn=")) {
			int index = NotesName.indexOf('/');
			if (index > 0)
				return (NotesName.substring(3, index));
		}
		return null;
	}
	protected boolean checkDocumentReaders(String NotesName, Vector<String> UserGroups, Document crawldoc, Document dbdoc) throws NotesException {
		final String METHOD = "checkDocumentReaders"; 
		_logger.entering(CLASS_NAME, METHOD);

		Vector<?> AllowAuthors = crawldoc.getItemValue(NCCONST.NCITM_DOCAUTHORREADERS);
		_logger.logp(Level.CONFIG, CLASS_NAME, METHOD, "Document reader list is " + AllowAuthors);
		
		// Check using the Notes name
		_logger.logp(Level.CONFIG, CLASS_NAME, METHOD, "Checking document level access for: " +  NotesName);
		if  (AllowAuthors.contains(NotesName)) {
			_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "ALLOWED: User is in authors " + NotesName);
			_logger.exiting(CLASS_NAME, METHOD);
			return true;
		}
		
		// Check using the common name
		String CommonName = getCommonName(NotesName);
		_logger.logp(Level.CONFIG, CLASS_NAME, METHOD, "Checking document level access for user: " +  CommonName);
		if (null != CommonName) {
			if  (AllowAuthors.contains(CommonName)) {
				_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "ALLOWED: User is in authors " + CommonName);
				_logger.exiting(CLASS_NAME, METHOD);
				return true;
			}
		}

		// Check using groups
		for (int i=0; i < UserGroups.size(); i++) {
			String group = UserGroups.elementAt(i).toString();
			_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "Checking document level access for group: " + group);
			if (AllowAuthors.contains(group)) {
				_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "ALLOWED: Group is in authors " + group);
				_logger.exiting(CLASS_NAME, METHOD);
				return true;
			}
		}

		// Expand roles and check using roles
		Vector<String> Roles = expandRoles(NotesName, UserGroups, dbdoc);
		for (int i=0; i < Roles.size(); i++) {
			String role = Roles.elementAt(i).toString();
			_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "Checking document level access for role: " + role);
			if (AllowAuthors.contains(role)) {
				_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "ALLOWED: Role is in authors " + role);
				_logger.exiting(CLASS_NAME, METHOD);
				return true;
			}
		}
		_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "DENIED: User's security principles are not in document access lists.");
		_logger.exiting(CLASS_NAME, METHOD);
		return false;
	}

	// Testing with R8.5 roles do not expand to nested groups.  You must be a direct member of the group to get the role
	// TODO: Check and validate this with other versions
	protected Vector <String> expandRoles(String NotesName, Vector<String> UserGroups, Document dbdoc) throws NotesException {
		final String METHOD = "expandRoles";
		_logger.entering(CLASS_NAME, METHOD);
		
		// Do we have any roles?

		Vector<?> dbroles = dbdoc.getItemValue(NCCONST.NCITM_ROLEPREFIX);
		Vector<String> enabledRoles = new Vector<String>();
		_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "Checking roles for user: " + NotesName + " using: " + dbroles.toString());
		if (dbroles.size() < 1)
			return enabledRoles;
		
		Vector<String> credentials = (Vector<String>) UserGroups.clone();
		credentials.add(NotesName);
		credentials.add(getCommonName(NotesName));
		StringBuffer searchstring = new StringBuffer(512);
		
		for (int i=0; i<dbroles.size(); i++) {
			String roledata = dbroles.elementAt(i).toString();
			for (int j=0; j<credentials.size(); j++) {
				searchstring.setLength(0);
				searchstring.append('~');
				searchstring.append(credentials.elementAt(j));
				searchstring.append('~');
				if (roledata.contains(searchstring)) {
					enabledRoles.add(roledata.substring(0, roledata.indexOf(']')+1).toLowerCase());
				}
			}
		}
		_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "Roles enabled for user: " + NotesName + " are: " + enabledRoles.toString());
		_logger.exiting(CLASS_NAME, METHOD);
		return enabledRoles;
	}
	
	protected boolean checkDatabaseAccess(String NotesName, Document DbDoc, Vector<?> UserGroups) throws NotesException {
		final String METHOD = "checkDatabaseAccess";
		_logger.entering(CLASS_NAME, METHOD);
		
		String CommonName = getCommonName(NotesName);
		if (checkDenyUser(NotesName, DbDoc)) {
			_logger.exiting(CLASS_NAME, METHOD);
			return false;
		}
		if (null != CommonName) {
			if (checkDenyUser(CommonName, DbDoc)) {
				_logger.exiting(CLASS_NAME, METHOD);
				return false;
			}
		}
		if (checkAllowUser(NotesName, DbDoc)) {
			_logger.exiting(CLASS_NAME, METHOD);
			return true;
		}
		if (null != CommonName) {
			if (checkAllowUser(CommonName, DbDoc)) {
				_logger.exiting(CLASS_NAME, METHOD);
				return true;
			}
		}
		if (checkAllowGroup(UserGroups, DbDoc )){
			_logger.exiting(CLASS_NAME, METHOD);
			return true;
		}
		_logger.exiting(CLASS_NAME, METHOD);
		return false;
	}
	//TODO:  the access groups may not need to be summary data. to avoid 64k 
	protected boolean checkAllowGroup(Vector<?>UserGroups, Document dbdoc)throws NotesException {
		final String METHOD = "checkAllowGroup"; 
		_logger.entering(CLASS_NAME, METHOD);

		_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "Checking database ACL for allow for groups");
		Vector<?> AllowGroups = dbdoc.getItemValue(NCCONST.NCITM_DBPERMITGROUPS);
		_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "Allow groups are: " + AllowGroups.toString());
		
		for (int i=0; i < UserGroups.size(); i++) {
			String group = UserGroups.elementAt(i).toString();
			_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "Checking group " + group);
			if (AllowGroups.contains(group)) {
				_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "ALLOWED: User is allowed through group " + group);
				_logger.exiting(CLASS_NAME, METHOD);
				return true;
			}
		}
		_logger.exiting(CLASS_NAME, METHOD);
		return false;
	}

	
	protected boolean checkAllowUser(String userName, Document dbdoc) throws NotesException {
		final String METHOD = "checkAllowUser"; 
		_logger.entering(CLASS_NAME, METHOD);

		_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "Checking database ACL for allow to user");
		Vector<?> AllowList = dbdoc.getItemValue(NCCONST.NCITM_DBPERMITUSERS);
		if  (AllowList.contains("-default-")) {
			_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "ALLOWED: Default is allowed " + userName);
			_logger.exiting(CLASS_NAME, METHOD);
			return true;
		}
		if  (AllowList.contains(userName)) {
			_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "ALLOWED: User is explictly allowed " + userName);
			_logger.exiting(CLASS_NAME, METHOD);
			return true;
		}
		
		return false;
	}

	protected boolean checkDenyUser(String userName, Document dbdoc) throws NotesException {
		final String METHOD = "checkDenyUser"; 
		_logger.entering(CLASS_NAME, METHOD);
		
		Vector<?> DenyList = dbdoc.getItemValue(NCCONST.NCITM_DBNOACCESSUSERS);
		_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "Checking database ACL for explicit deny to user");
		if  (DenyList.contains(userName)) {
			_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "DENIED: User is explictly denied " + userName);
			_logger.exiting(CLASS_NAME, METHOD);
			return true;
		}
		_logger.exiting(CLASS_NAME, METHOD);
		return false;
	}
	

	
	
}
