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

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Helper for constructing/parsing doc id strings.
 */
/* TODO: handle file attachments in the id URLs. */
class NotesDocId {
  private String protocol = "http";
  private String host;
  private int port = -1;
  private String replicaId;
  private String docId;
  private String originalDocId;

  NotesDocId() {
  }

  NotesDocId(String docId) throws MalformedURLException {
    originalDocId = docId;
    URL url = new URL(docId);
    this.protocol = url.getProtocol();
    this.host = url.getHost();
    this.port = url.getPort();
    String path = url.getPath();
    if (path.length() == 0) {
      return;
    }
    if (path.startsWith("/")) {
      path = path.substring(1);
    }
    String[] parts = path.split("/");
    if (parts.length >= 3) { // extra path elements for attachments
      this.replicaId = parts[0];
      this.docId = parts[2];
    } else if (parts.length == 1) {
      this.replicaId = parts[0];
    }
  }

  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }

  public String getProtocol() {
    return protocol;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public String getHost() {
    return host;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public int getPort() {
    return port;
  }

  public void setReplicaId(String replicaId) {
    this.replicaId = replicaId;
  }

  public String getReplicaId() {
    return replicaId;
  }

  public void setDocId(String docId) {
    this.docId = docId;
  }

  public String getDocId() {
    return docId;
  }

  String getPath() {
    if (null == docId) {
      return "/" + replicaId;
    }
    return "/" + replicaId + "/0/" + docId;
  }

  String getReplicaUrl() {
    try {
      URL url = new URL(protocol, host, port, "/" + replicaId);
      return url.toString();
    } catch (MalformedURLException e) {
      // TODO
      return null;
    }
  }

  public String toString() {
    try {
      URL url = new URL(protocol, host, port, getPath());
      return url.toString();
    } catch (MalformedURLException e) {
      // TODO
      return null;
    }
  }
}
