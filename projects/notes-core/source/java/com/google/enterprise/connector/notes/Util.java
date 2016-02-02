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
import com.google.common.base.Charsets;
import com.google.enterprise.connector.notes.client.NotesBase;
import com.google.enterprise.connector.spi.RepositoryException;
import com.google.enterprise.connector.util.Base16;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Helpers.
 */
@VisibleForTesting
public class Util {
  private static final String CLASS_NAME = Util.class.getName();
  private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);
  private static final String DEFAULT_ALGORITHM = "SHA1";

  // Refer to TESTCONST.NotesVersion class for examples of version string.
  private static final Pattern VERSION_EIGHT_OR_OLDER =
      Pattern.compile(" [1-8]\\.[0-9]");

  static void recycle(NotesBase... objects) {
    for (NotesBase obj : objects) {
      if (null != obj) {
        try {
          obj.recycle();
        } catch (RepositoryException e) {
          LOGGER.log(Level.WARNING,
              "Error calling recycle on Notes object: " + obj, e);
        }
      }
    }
  }

  static void recycle(NotesBase obj, Vector vec) {
    if (null != obj && null != vec) {
      try {
        obj.recycle(vec);
      } catch (RepositoryException e) {
        LOGGER.log(Level.WARNING,
            "Error calling recycle on Notes object: " + obj
            + " with data: " + vec, e);
      }
    }
  }

  static void close(Statement stmt) {
    if (null != stmt) {
      try {
        stmt.close();
      } catch (SQLException e) {
        LOGGER.log(Level.WARNING, "Error closing statement", e);
      }
    }
  }

  static void close(ResultSet rs) {
    if (null != rs) {
      try {
        rs.close();
      } catch (SQLException e) {
        LOGGER.log(Level.WARNING, "Error closing result set", e);
      }
    }
  }

  static void executeStatements(Connection connection, boolean autoCommit,
      String... statements) throws SQLException {
    if (connection == null) {
      throw new SQLException("Database connection is null");
    }
    Statement stmt = connection.createStatement();
    try {
      connection.setAutoCommit(autoCommit);
      connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
      for (String statement : statements) {
        stmt.executeUpdate(statement);
        LOGGER.log(Level.FINE, "Executed {0}", statement);
      }
      if (autoCommit == false) {
        try {
          connection.commit();
          LOGGER.log(Level.FINE, "Committed all transactions successfully");
        } catch (SQLException sqle) {
          connection.rollback();
          LOGGER.log(Level.FINE, "Rolled back all transactions");
          throw sqle;
        }
      }
    } finally {
      try {
        stmt.close();
      } catch (SQLException e) {
        LOGGER.log(Level.WARNING, e.getMessage());
      }
    }
  }

  static boolean isCanonical(String name) {
    if (name == null) {
      return false;
    }
    return name.toLowerCase().startsWith("cn=");
  }

  static boolean isAttachment(String url) {
    return url != null && url.toLowerCase().contains("/$file/");
  }

  static void invokeGC() {
    Runtime rt = Runtime.getRuntime();
    LOGGER.log(Level.FINEST, "Memory free [before GC invocation]: " +
        (rt.freeMemory() / 1024) + "kb" + ", Total: " +
        (rt.totalMemory() / 1024) + "kb");
    rt.gc();
    LOGGER.log(Level.FINEST, "Memory free [after GC invocation ]: " +
        (rt.freeMemory() / 1024) + "kb" + ", Total: " +
        (rt.totalMemory() / 1024) + "kb");
  }

  static String buildString(String...args) {
    StringBuilder buf = new StringBuilder();
    for (String arg : args) {
      buf.append(arg);
    }
    return buf.toString();
  }

  static String hash(String word) {
    try {
      MessageDigest digest = MessageDigest.getInstance(DEFAULT_ALGORITHM);
      byte[] hashBytes = digest.digest(word.getBytes(Charsets.UTF_8));
      String hashStr = Base16.lowerCase().encode(hashBytes);
      LOGGER.log(Level.FINEST, "Create a hash for {0} => {1}",
          new Object[] {word, hashStr});
      return hashStr;
    } catch (NoSuchAlgorithmException e) {
      LOGGER.log(Level.WARNING, "Unable to initialize " + DEFAULT_ALGORITHM
          + " message digest");
      return null;
    }
  }

  @VisibleForTesting
  public static boolean isNotesVersionEightOrOlder(String versionString) {
    return VERSION_EIGHT_OR_OLDER.matcher(versionString).find();
  }

  /**
   * Private constructor to prevent instantiation.
   */
  private Util() {
  }
}
