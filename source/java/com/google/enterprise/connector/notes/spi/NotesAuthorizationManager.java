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

import com.google.enterprise.connector.spi.AuthenticationIdentity;
import com.google.enterprise.connector.spi.AuthorizationManager;
import com.google.enterprise.connector.spi.AuthorizationResponse;
import com.google.enterprise.connector.spi.RepositoryException;

import java.util.Collection;

/**
 * @author Deepti Nagarkar
 */
public class NotesAuthorizationManager implements AuthorizationManager {

	public Collection<AuthorizationResponse> authorizeDocids(
			Collection<String> arg0, AuthenticationIdentity arg1)
			throws RepositoryException {
		// TODO Auto-generated method stub
		return null;
	}

}
