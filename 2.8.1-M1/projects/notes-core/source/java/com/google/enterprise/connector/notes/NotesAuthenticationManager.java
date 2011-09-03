package com.google.enterprise.connector.notes;


import java.util.logging.Level;
import java.util.logging.Logger;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.NotesFactory;
import lotus.domino.NotesThread;
import lotus.domino.Session;
import lotus.domino.View;


import com.google.enterprise.connector.spi.AuthenticationIdentity;
import com.google.enterprise.connector.spi.AuthenticationManager;
import com.google.enterprise.connector.spi.AuthenticationResponse;


class NotesAuthenticationManager implements AuthenticationManager {
	private static final String CLASS_NAME = NotesAuthenticationManager.class.getName();
	private static final Logger _logger = Logger.getLogger(CLASS_NAME);
	private NotesConnectorSession ncs = null;
	Session nSession = null;
	Database namesDb = null;
	Database acDb = null;
	View usersVw = null;
	View peopleVw = null;
	Document authDoc = null;
	Document personDoc = null;
	
	
	public NotesAuthenticationManager(NotesConnectorSession session){
		final String METHOD = "NotesAuthenticationManager";
		_logger.logp(Level.INFO, CLASS_NAME, METHOD, "NotesAuthenticationManager being created.");

		ncs = session;
	}
	
	public void recycleDominoObjects () {
		try {
			if (null != personDoc)
				personDoc.recycle();
			if (null != authDoc)
				authDoc.recycle();
			if (null != usersVw)
				usersVw.recycle();
			if (null!= peopleVw)
				peopleVw.recycle();
			if (null != acDb)
				acDb.recycle();
			if (null!= namesDb)
				namesDb.recycle();
			if (nSession!= null)
				nSession.recycle();
		}
		catch (Exception e) {
			_logger.log(Level.SEVERE, CLASS_NAME, e);
		}
	}
	
	public AuthenticationResponse authenticate(AuthenticationIdentity id) {
		final String METHOD = "authenticate";
		_logger.entering(CLASS_NAME, METHOD);
		
		try {
			String pvi = id.getUsername();
			_logger.logp(Level.INFO, CLASS_NAME, METHOD, "Authenticating user " + pvi);
			NotesThread.sinitThread();
			
			nSession = NotesFactory.createSessionWithFullAccess(ncs.getPassword());
			namesDb = nSession.getDatabase(ncs.getServer(), "names.nsf");  //TODO: Check what we need to support here

			acDb = nSession.getDatabase(null, null);
			_logger.logp(Level.FINER, CLASS_NAME, METHOD, "Opening ACL database " + ncs.getServer() + " : " + ncs.getACLDbReplicaId());
			acDb.openByReplicaID(ncs.getServer(), ncs.getACLDbReplicaId());

			peopleVw = acDb.getView(NCCONST.VIEWACPEOPLE);

			// Resolve the PVI to their Notes names and groups
			personDoc = peopleVw.getDocumentByKey(pvi, true);
			if (null == personDoc) {
				_logger.logp(Level.WARNING, CLASS_NAME, METHOD, "Person not found in ACL database " + pvi);
				return new AuthenticationResponse(false, null);
			}
			String NotesName = personDoc.getItemValueString(NCCONST.ACITM_NOTESNAME).toLowerCase();
			_logger.logp(Level.INFO, CLASS_NAME, METHOD, "Authentication user using Notes name " + NotesName);
			usersVw = namesDb.getView("($Users)");

			// Resolve the PVI to their Notes names and groups
			authDoc = usersVw.getDocumentByKey(NotesName, true);
			if (null == authDoc) {

				return new AuthenticationResponse(false, null);
			}
			String hashedPassword = authDoc.getItemValueString("HTTPPassword");
			if (nSession.verifyPassword(id.getPassword(), hashedPassword)) {
					_logger.logp(Level.INFO, CLASS_NAME, METHOD, "User succesfully authenticated " + NotesName);
					return new AuthenticationResponse(true,null);
			}
			_logger.logp(Level.WARNING, CLASS_NAME, METHOD, "User failed authentication " + NotesName);
		}
		catch(Exception e)
		{
			_logger.log(Level.SEVERE, CLASS_NAME, e);
		}
		finally
		{
			recycleDominoObjects();
			NotesThread.stermThread();
			_logger.exiting(CLASS_NAME, METHOD);
		}
		return new AuthenticationResponse(false, null);
	}
}
