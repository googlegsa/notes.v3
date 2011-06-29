//Copyright 2011 Google Inc.
//
//Licensed under the Apache License, Version 2.0 (the "License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing, software
//distributed under the License is distributed on an "AS IS" BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
package com.google.enterprise.connector.notes.spi;

import com.google.enterprise.connector.spi.Connector;
import com.google.enterprise.connector.spi.RepositoryException;
import com.google.enterprise.connector.spi.RepositoryLoginException;
import com.google.enterprise.connector.spi.Session;

/**
 * @author Deepti Nagarkar
 */
public class NotesConnector implements Connector {

	private NotesSession session;
	private String username = "pspl pspl";
	private String password = "pspl!@#";
	private String server = "GDC40-LotusNotes";
	private String domain = ".persistent.co.in";

	public Session login() throws RepositoryLoginException, RepositoryException {
		// TODO Auto-generated method stub
		return null;
	}

}
