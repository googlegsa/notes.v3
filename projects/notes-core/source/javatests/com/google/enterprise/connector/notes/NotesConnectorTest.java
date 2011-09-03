package com.google.enterprise.connector.notes;

import com.google.enterprise.connector.notes.NotesConnector;
import com.google.enterprise.connector.notes.NotesConnectorSession;
import com.google.enterprise.connector.notes.NotesTraversalManager;
import com.google.enterprise.connector.spi.Connector;
import com.google.enterprise.connector.spi.RepositoryLoginException;
import com.google.enterprise.connector.spi.RepositoryException;
import com.google.enterprise.connector.spi.Session;
import com.google.enterprise.connector.spi.TraversalManager;

import junit.framework.TestCase;

public class NotesConnectorTest extends TestCase {

        protected void setUp() throws Exception {
        }

        /*
         * Test method for
         * 'com.google.enterprise.connector.notes.NotesConnectorSession.login()'
         */
        public void testLogin() throws RepositoryLoginException, RepositoryException {
                Connector connector = new NotesConnector();
                ((NotesConnector) connector).setServer(ncTest.server);
                ((NotesConnector) connector).setDatabase(ncTest.database);
                ((NotesConnector) connector).setIDPassword(ncTest.idpassword);
                Session sess = connector.login();
                assertNotNull(sess);
                assertTrue(sess instanceof NotesConnectorSession);
        }

        public void testReset() throws RepositoryLoginException, RepositoryException {
                Connector connector = new NotesConnector();
                ((NotesConnector) connector).setServer(ncTest.server);
                ((NotesConnector) connector).setDatabase(ncTest.database);
                ((NotesConnector) connector).setIDPassword(ncTest.idpassword);
                Session sess = connector.login();
				TraversalManager tm = sess.getTraversalManager();
                assertNotNull(tm);
                assertTrue(tm instanceof NotesTraversalManager);
				tm.startTraversal();
		try {
		java.lang.Thread.sleep(10000);
		}
		catch (Exception e) {}
        }


}

