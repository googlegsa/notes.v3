// Copyright 2012 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.enterprise.connector.notes;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.enterprise.connector.notes.client.NotesDocument;
import com.google.enterprise.connector.notes.client.NotesItem;
import com.google.enterprise.connector.spi.RepositoryException;
import com.google.enterprise.connector.util.database.DatabaseConnectionPool;
import com.google.enterprise.connector.util.database.JdbcDatabase;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * NotesDocumentManager Manages search indexes in H2 database.
 * The search indexes are managed in following tables:
 * NCIndexed_<Connector> table:
 *   docid (primary key)
 *   unid (32 characters)
 *   replicaid (16 characters)
 *   server (100 characters)
 *   protocol (5 characters)
 *   host (100 characters)
 *
 * NCIndexedReaders_<Connector> table:
 *   id (primary key)
 *   docid (foreign key)
 *   reader (100 characters)
 *
 * Note: the <Connector> value will be assigned at runtime from the
 * Connector Manager to avoid table naming conflicts or duplicates.
 *
 */
public class NotesDocumentManager {
  private static final String CLASS_NAME =
      NotesDocumentManager.class.getName();
  private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

  private final NotesConnectorSession ncSession;
  private final JdbcDatabase jdbcDatabase;
  private final DatabaseConnectionPool connectionPool;
  @VisibleForTesting final String indexedTableName;
  @VisibleForTesting final String readersTableName;
  @VisibleForTesting final String attachmentsTableName;

  NotesDocumentManager(NotesConnectorSession ncs)
      throws RepositoryException {
    this.ncSession = ncs;
    this.jdbcDatabase = ncSession.getConnector().getJdbcDatabase();
    this.connectionPool = jdbcDatabase.getConnectionPool();
    String connectorName = ncSession.getConnector().getGoogleConnectorName();
    this.indexedTableName = jdbcDatabase.makeTableName(
        NCCONST.TABLE_INDEXED_PREFIX, connectorName);
    this.readersTableName = jdbcDatabase.makeTableName(
        NCCONST.TABLE_READERS_PREFIX, connectorName);
    this.attachmentsTableName = jdbcDatabase.makeTableName(
        NCCONST.TABLE_ATTACHMENTS_PREFIX, connectorName);
    initializeDatabase();
  }

  private void initializeDatabase() throws RepositoryException {
    final String METHOD = "initializeDatabase";

    //Build DDL statement to create indexed table
    StringBuilder indexedDDL = new StringBuilder();
    indexedDDL.append("create table ").append(indexedTableName).append("(");
    indexedDDL.append("docid long auto_increment primary key, ");
    indexedDDL.append("unid varchar(")
        .append(NCCONST.COLUMN_SIZE_UNID).append(") not null, ");
    indexedDDL.append("replicaid varchar(")
        .append(NCCONST.COLUMN_SIZE_REPLICAID).append(") not null, ");
    indexedDDL.append("server varchar(")
        .append(NCCONST.COLUMN_SIZE_SERVER).append("), ");
    indexedDDL.append("protocol varchar(")
        .append(NCCONST.COLUMN_SIZE_PROTOCOL).append("), ");
    indexedDDL.append("host varchar(")
        .append(NCCONST.COLUMN_SIZE_HOST).append(")");
    indexedDDL.append(")");
    
    //Build create index statement for indexed table
    StringBuilder createIndexSQL = new StringBuilder();
    createIndexSQL.append("create index idx_" + indexedTableName);
    createIndexSQL.append(" on ").append(indexedTableName);
    createIndexSQL.append("(unid, replicaid)");
    
    //Create table and index
    jdbcDatabase.verifyTableExists(indexedTableName,
        new String[]{indexedDDL.toString(), createIndexSQL.toString()});
    LOGGER.logp(Level.FINE, CLASS_NAME, METHOD, "Create/verify " + 
        indexedTableName);

    //Verify or create readers table
    StringBuilder readersDDL = new StringBuilder();
    readersDDL.append("create table ")
        .append(this.readersTableName).append("(");
    readersDDL.append("id long auto_increment primary key, ");
    readersDDL.append("reader varchar(").append(NCCONST.COLUMN_SIZE_READER);
    readersDDL.append(") not null, ").append("docid long not null");
    readersDDL.append(", foreign key(docid) references ");
    readersDDL.append(indexedTableName).append("(docid))");
    jdbcDatabase.verifyTableExists(readersTableName,
        new String[]{readersDDL.toString()});
    LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
        "Create/verify " + this.readersTableName);

    // Verify and create attachments table
    StringBuilder attachmentsDDL = new StringBuilder();
    attachmentsDDL.append("create table ");
    attachmentsDDL.append(attachmentsTableName).append("(");
    attachmentsDDL.append("id long auto_increment primary key, ");
    attachmentsDDL.append("attachment_unid varchar(");
    attachmentsDDL.append(NCCONST.COLUMN_SIZE_UNID);
    attachmentsDDL.append(") not null, ").append("docid long not null");
    attachmentsDDL.append(", foreign key(docid) references ");
    attachmentsDDL.append(indexedTableName).append("(docid))");

    jdbcDatabase.verifyTableExists(attachmentsTableName,
        new String[]{attachmentsDDL.toString()});
    LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
        "Create/verify " + attachmentsTableName);
  }

  /**
   * Helper method to set auto commit.
   */
  private boolean setAutoCommit(Connection conn, boolean isAutoCommit) {
    final String METHOD = "setAutoCommit";
    boolean isSet = false;
    try {
      //Set default auto commit
      conn.setAutoCommit(isAutoCommit);
      isSet = true;
    } catch (SQLException sqle) {
      LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
          "Failed to set auto commit to " + isAutoCommit, sqle);
    }
    return isSet;
  }

  /**
   * Caller needs to obtain the connection before invoking
   * updateSearchIndex method.
   */
  Connection getDatabaseConnection() throws SQLException {
    final String METHOD = "getDatabaseConnection";
    LOGGER.logp(Level.FINE, CLASS_NAME, METHOD, "Obtain connection from pool");
    Connection connection = connectionPool.getConnection();
    return connection;
  }

  /**
   * Caller needs to release connection after invoking
   * updateSearchIndex method.
   */
  void releaseDatabaseConnection(Connection connection) {
    final String METHOD = "releaseDatabaseConnection";
    connectionPool.releaseConnection(connection);
    LOGGER.logp(Level.FINE, CLASS_NAME, METHOD, "Release connection to pool");
  }

  /**
   * Update search indexes.
   */
  boolean addIndexedDocument(NotesDocument docIndexed, Connection connection)
     throws RepositoryException {
    final String METHOD = "updateSearchIndex";
    LOGGER.entering(CLASS_NAME, METHOD);
    boolean isUpdated = false;

    //Validate connection, auto commit and indexed document
    if (connection == null)
      throw new RepositoryException("Database connection is null");

    if (!setAutoCommit(connection, false)) {
      throw new RepositoryException("Failed to disable auto commit");
    }

    if (docIndexed == null)
      return isUpdated;

    //Get NC.UNID, NC.Server, google.docid
    String unid = null;
    String server = null;
    String gid = null;
    try {
      unid = docIndexed.getItemValueString(NCCONST.NCITM_UNID);
      if (Strings.isNullOrEmpty(unid))
        return isUpdated;
      LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
          "Add indexed document UNID#" + unid + " to database");
      server = docIndexed.getItemValueString(NCCONST.NCITM_SERVER);
      if (server.length() > NCCONST.COLUMN_SIZE_SERVER) {
        server = server.substring(0, NCCONST.COLUMN_SIZE_SERVER);
      }
      gid = docIndexed.getItemValueString(NCCONST.ITM_DOCID);
    } catch (RepositoryException re) {
      LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
          "NC.UNID, NC.Server and google.docid fields are not accessible.");
      return isUpdated;
    }
    if (Strings.isNullOrEmpty(gid)) {
      LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
          "google.docid is null or empty");
      return isUpdated;
    }

    //Compute from google.docid
    NotesDocId notesId = null;
    try {
      notesId = new NotesDocId(gid);
    } catch (MalformedURLException e2) {
      LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
          "Malformed docid: " + gid, e2);
      notesId = new NotesDocId();
      notesId.setDocId(unid);
      String domain = docIndexed.getItemValueString(NCCONST.NCITM_DOMAIN);
      notesId.setHost(server + domain);
      notesId.setProtocol(gid.split(":")[0]);
      String replicaId = docIndexed.getItemValueString(NCCONST.NCITM_REPLICAID);
      notesId.setReplicaId(replicaId);
    }

    PreparedStatement pstmt = null;
    try {
      // Delete existing readers, attachments and document before inserting new
      deleteDocument(unid, notesId.getReplicaId(), connection);
      
      //Insert into indexed table
      pstmt = connection.prepareStatement(
          "insert into " + indexedTableName +
          "(unid, replicaid, server, host, protocol) values(?,?,?,?,?)",
          Statement.RETURN_GENERATED_KEYS);
      pstmt.setString(1, unid);
      pstmt.setString(2, notesId.getReplicaId());
      pstmt.setString(3, server);
      pstmt.setString(4, notesId.getHost());
      pstmt.setString(5, notesId.getProtocol());
      pstmt.executeUpdate();
      ResultSet rs = pstmt.getGeneratedKeys();
      if (rs.next()) {
        long docid = rs.getLong(1);
        rs.close();
        pstmt.close();
        //Insert into readers table
        Set<String> readers = getReaders(docIndexed, new String[] {
            NCCONST.NCITM_DOCREADERS,NCCONST.NCITM_DOCAUTHORREADERS});
        pstmt = connection.prepareStatement(
            "insert into " + readersTableName + "(reader, docid) values(?,?)");
        for (String reader : readers) {
          if (reader.length() > NCCONST.COLUMN_SIZE_READER) {
            reader = reader.substring(0, NCCONST.COLUMN_SIZE_READER);
          }
          pstmt.setString(1, reader);
          pstmt.setLong(2, docid);
          pstmt.addBatch();
        }
        pstmt.executeBatch();
        pstmt.close();
        
        // Insert attachment names
        NotesItem itemAttachmentIds =
            docIndexed.getFirstItem(NCCONST.ITM_GMETAATTACHMENTDOCIDS);
        if (itemAttachmentIds != null) {
          Vector attachmentIds = itemAttachmentIds.getValues();
          if (attachmentIds != null && attachmentIds.size() > 0) {
            pstmt = connection.prepareStatement(
                "insert into " + attachmentsTableName
                + "(attachment_unid, docid) values(?,?)");
            for (int i = 0; i < attachmentIds.size(); i++) {
              String attachmentId = (String) attachmentIds.get(i);
              pstmt.setString(1, attachmentId);
              pstmt.setLong(2, docid);
              pstmt.addBatch();
              LOGGER.log(Level.FINEST,
                  "Insert attachment: {0}", attachmentId);
            }
            pstmt.executeBatch();
            pstmt.close();
          }
        }
      }
      connection.commit();
      isUpdated = true;
    } catch (SQLException sqle) {
      LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
          "Unable to add indexed document to database (UNID: " + unid + ")",
          sqle);
      try {
        connection.rollback();
      } catch (SQLException sqle2) {
        LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
            "Failed to rollback transaction");
        throw new AssertionError(sqle2);
      }
    } finally {
      if (!setAutoCommit(connection, true)) {
        LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
          "Failed to enable auto commit");
      }
    }
    LOGGER.exiting(CLASS_NAME, METHOD);
    return isUpdated;
  }

  /**
   * The method queries the Indexed table and return a map of document unique id
   * and entries.  The first record is the startUnid if it is
   * existed within the database.
   * @param startUnid
   * @param batchSize
   * @return Map<unid,replicaid>
   */
  Map<String, NotesDocId> getIndexedDocuments(String startUnid,
      String replicaId, int batchSize) throws RepositoryException {
    final String METHOD = "getIndexedDocuments";
    LOGGER.entering(CLASS_NAME, METHOD);

    Map<String, NotesDocId> indexedDocEntries =
        new LinkedHashMap<String, NotesDocId>(batchSize);
    Connection conn = null;
    try {
      conn = getDatabaseConnection();
      //Check startUnid
      boolean isExisted = false;
      if (!Strings.isNullOrEmpty(startUnid)) {
        isExisted = hasIndexedDocument(startUnid, replicaId, conn);
        LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
            "Start unique ID#" + startUnid + " is found");
      }
      PreparedStatement pstmt = conn.prepareStatement(
          "select unid, replicaid, server, host, protocol from " +
              indexedTableName + " order by unid",
          ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
      ResultSet rs = pstmt.executeQuery();
      String unid = null;
      if (isExisted) {
        //Scroll to the startUnid
        boolean isFound = false;
        while (rs.next()) {
          unid = rs.getString(1);
          if (unid.equalsIgnoreCase(startUnid)) {
            isFound = true;
            break;
          }
        }
        if (isFound) {
          if (rs.isLast()) {
            rs.beforeFirst();
            LOGGER.logp(Level.FINE, CLASS_NAME, METHOD, "Doc ID#" + unid +
                " is at the end of collection; reset to first record");
          } else {
            rs.previous();
            LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
                "Collection started with " + unid + " document ID");
          }
        } else {
          LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
              "Document unique id was not found in " + indexedTableName +
              " table");
          rs.beforeFirst();
        }
      }
      int count = 0;
      while (rs.next() && count < batchSize) {
        NotesDocId notesId = new NotesDocId();
        unid = rs.getString(1);
        notesId.setDocId(rs.getString(1));
        notesId.setReplicaId(rs.getString(2));
        notesId.setServer(rs.getString(3));
        notesId.setHost(rs.getString(4));
        notesId.setProtocol(rs.getString(5));
        indexedDocEntries.put(unid, notesId);
        count++;
      }
      rs.close();
      pstmt.close();
    } catch (SQLException e) {
      LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
          "Failed to query " + indexedTableName + "table");
      throw new RepositoryException(e);
    } finally {
      if (conn != null) {
        releaseDatabaseConnection(conn);
      }
    }
    LOGGER.exiting(CLASS_NAME, METHOD);
    return indexedDocEntries;
  }

  Set<String> getDocumentReaders(String unid, String replicaid)
      throws RepositoryException {
    final String METHOD = "getDocumentReaders";
    LOGGER.entering(CLASS_NAME, METHOD);

    Set<String> readers = new HashSet<String>();
    Connection conn = null;
    try {
      conn = getDatabaseConnection();
      PreparedStatement pstmt = conn.prepareStatement(
          "select reader from " + readersTableName + " where docid ="
          + "(select docid from " + indexedTableName + " where unid = ?"
          + " and replicaid = ?)");
      pstmt.setString(1, unid);
      pstmt.setString(2, replicaid);
      ResultSet rs = pstmt.executeQuery();
      while (rs.next()) {
        readers.add(rs.getString(1));
      }
      rs.close();
      pstmt.close();
    } catch (SQLException e) {
      LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
          "Failed to lookup readers for " + unid + " document");
    } finally {
      if (conn != null) {
        releaseDatabaseConnection(conn);
      }
      LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
          "Found " + readers.size() + " reader(s) in " + unid + " document");
    }
    LOGGER.exiting(CLASS_NAME, METHOD);

    return readers;
  }

  boolean hasIndexedDocument(String unid, String replicaid, Connection conn)
      throws RepositoryException {
    final String METHOD = "hasIndexedDocument";
    LOGGER.entering(CLASS_NAME, METHOD);
    boolean hasItem = false;
    try {
      PreparedStatement pstmt = conn.prepareStatement(
          "select count(*) from " + indexedTableName
          + " where unid=? and replicaid=?");
      pstmt.setString(1, unid);
      pstmt.setString(2, replicaid);
      ResultSet rs = pstmt.executeQuery();
      if (rs.next()) {
        if (rs.getInt(1) > 0) {
          hasItem = true;
        }
      }
      rs.close();
      pstmt.close();
    } catch (SQLException e) {
      LOGGER.logp(Level.FINE, CLASS_NAME, METHOD, "Failed to lookup " + unid +
          " in " + indexedTableName + " table");
      throw new RepositoryException(e);
    }
    LOGGER.exiting(CLASS_NAME, METHOD);

    return hasItem;
  }

  Set<String> getAttachmentIds(Connection conn, String unid,
      String replicaid) {
    LOGGER.log(Level.FINE,
        "Get attachment names for document [UNID: {0}, REPLICAID: {1}]",
        new Object[] {unid, replicaid});

    Set<String> attachmentNames = new HashSet<String>();
    if (conn == null) {
      LOGGER.log(Level.WARNING,
          "Failed to lookup attachment names.  Database connection is null");
      return attachmentNames;
    }

    try {
      PreparedStatement pstmt = conn.prepareStatement(
          "select attachment_unid from " + attachmentsTableName
          + " where docid in (select docid from " + indexedTableName
          + " where unid = ? and replicaid = ?)");
      pstmt.setString(1, unid);
      pstmt.setString(2, replicaid);
      ResultSet rs = pstmt.executeQuery();
      while (rs.next()) {
        attachmentNames.add(rs.getString(1));
      }
      rs.close();
      pstmt.close();
    } catch (SQLException e) {
      LOGGER.log(Level.WARNING,
          "Failed to get attachment file names from database", e);
    }
    return attachmentNames;
  }

  boolean deleteDocument(String unid, String replicaid)
      throws RepositoryException {
    final String METHOD = "deleteDocument";
    boolean isDeleted = false;
    Connection conn = null;
    try {
      conn = getDatabaseConnection();
      isDeleted = deleteDocument(unid, replicaid, conn);
    } catch (SQLException e) {
      LOGGER.logp(Level.WARNING, CLASS_NAME, METHOD,
          "Failed to obtain database connection from pool");
    } finally {
      if (conn != null) {
        releaseDatabaseConnection(conn);
      }
    }
    return isDeleted;
  }

  boolean deleteDocument(String unid, String replicaid, Connection conn)
      throws RepositoryException {
    final String METHOD = "deleteDocument";
    LOGGER.entering(CLASS_NAME, METHOD);

    //Validate database connection and auto commit
    if (conn == null) {
      throw new RepositoryException("Database connection is not initialized");
    }
    if (!setAutoCommit(conn, false)) {
      throw new RepositoryException("Failed to disable auto commit");
    }

    boolean isDeleted = false;
    try {
      //Delete readers
      PreparedStatement pstmt = conn.prepareStatement(
          "delete from " + readersTableName +
          " where docid = (select docid from " + indexedTableName +
          " where unid = ? and replicaid = ?)");
      pstmt.setString(1, unid);
      pstmt.setString(2, replicaid);
      pstmt.executeUpdate();
      pstmt.close();

      //Delete attachments
      pstmt = conn.prepareStatement(
          "delete from " + attachmentsTableName +
          " where docid = (select docid from " + indexedTableName +
          " where unid = ? and replicaid = ?)");
      pstmt.setString(1, unid);
      pstmt.setString(2, replicaid);
      pstmt.executeUpdate();
      pstmt.close();

      //Delete document
      pstmt = conn.prepareStatement(
          "delete from " + indexedTableName + " where unid=? and replicaid=?");
      pstmt.setString(1, unid);
      pstmt.setString(2, replicaid);
      pstmt.executeUpdate();
      pstmt.close();

      //Commit
      try {
        conn.commit();
        isDeleted = true;
        LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
            "Document " + unid + " is deleted");
      } catch (SQLException sqle) {
        try {
          LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
              "Failed to commit deleting " + unid + " document", sqle);
          conn.rollback();
        } catch (SQLException sqle2) {
          LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
              "Failed to rollbak from deleting " + unid + " document", sqle2);
        }
      }
    } catch (SQLException sqle) {
      LOGGER.logp(Level.WARNING, CLASS_NAME, METHOD,
          "Failed to delete " + unid + " document", sqle);
    } finally {
      if (!setAutoCommit(conn, true)) {
        LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
            "Failed to enable auto commit");
      }
    }
    LOGGER.exiting(CLASS_NAME, METHOD);
    return isDeleted;
  }

  /*
   * Compute a list of unique reader names
   */
  private Set<String> getReaders(
      NotesDocument docIndexed, String...readerFieldNames) {
    final String METHOD = "getDocumentReaders";
    Set<String> readers = new HashSet<String>();
    for (String readerFieldName : readerFieldNames) {
      try {
        Vector fieldValues = docIndexed.getItemValue(readerFieldName);
        for (int i=0; i < fieldValues.size(); i++) {
          String fieldValue = (String) fieldValues.get(i);
          readers.add(fieldValue);
        }
      } catch (RepositoryException e) {
        LOGGER.logp(Level.FINE, CLASS_NAME, METHOD, readerFieldName +
          " reader field is not existed in indexed document");
      }
    }
    return readers;
  }

  private void executeUpdates(boolean autoCommit, String...statements)
      throws SQLException {
    final String METHOD = "executeUpdates";
    Connection connection = getDatabaseConnection();
    Statement stmt = null;
    try {
      stmt = connection.createStatement();
      connection.setAutoCommit(autoCommit);
      connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
      for (String statement : statements) {
        stmt.executeUpdate(statement);
        LOGGER.logp(Level.FINE, CLASS_NAME, METHOD, "Executed " + statement);
      }
      if (autoCommit == false) {
        try {
          connection.commit();
          LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
              "Committed all transactions successfully");
        } catch (SQLException sqle) {
          connection.rollback();
          LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
              "Rolled back all transactions");
          throw sqle;
        }
      }
    } catch (SQLException e) {
      LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
          "Failed to update H2 database", e);
      throw e;
    } finally {
      if (stmt != null) {
        try {
          stmt.close();
        } catch (SQLException e) {
          LOGGER.logp(Level.WARNING, CLASS_NAME, METHOD, e.getMessage());
        }
      }
      if (connection != null) {
        connectionPool.releaseConnection(connection);
      }
    }
  }

  boolean clearTables() throws RepositoryException {
    final String METHOD = "clearTables";
    LOGGER.entering(CLASS_NAME, METHOD);
    boolean isClear = false;
    try {
      String[] statements = {
          "delete from " + readersTableName,
          "delete from " + attachmentsTableName,
          "delete from " + indexedTableName
      };
      executeUpdates(false, statements);
      isClear = true;
      LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
          "All data in " + indexedTableName + " and " + readersTableName +
          " tables are purged");
    } catch (SQLException e) {
      LOGGER.logp(Level.FINE, CLASS_NAME, METHOD, "Failed to clear all data");
      throw new RepositoryException(e);
    }
    LOGGER.exiting(CLASS_NAME, METHOD);
    return isClear;
  }

  boolean dropTables() throws RepositoryException {
    final String METHOD = "dropTables";
    LOGGER.entering(CLASS_NAME, METHOD);
    boolean isDropped = false;
    try {
      String[] statements = {
          "drop index if exists idx_" + indexedTableName,
          "drop table " + readersTableName,
          "drop table " + attachmentsTableName,
          "drop table " + indexedTableName
      };
      executeUpdates(false, statements);
      isDropped = true;
      LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
          indexedTableName + " and " + readersTableName +
          " tables were dropped");
    } catch (SQLException e) {
      LOGGER.logp(Level.FINE, CLASS_NAME, METHOD, "Failed to drop tables");
      throw new RepositoryException(e);
    }
    LOGGER.exiting(CLASS_NAME, METHOD);
    return isDropped;
  }
}
