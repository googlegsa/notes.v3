# Introduction #
The Connector allows an index to be rebuilt for each monitored database. Unlike disabling a database that deletes index content and prevents further indexing of a particular database, rebuilding merely flushes the index content for the particular database. When the crawler next runs, the content will be rebuilt and added back to the GSA.


# Details #
To rebuild the index for all databases that use a template, see [Rebuilding Databases](http://code.google.com/p/google-enterprise-connector-notes/wiki/Rebuilding_databases).

To rebuild the index for a single database, complete the following steps:

1.  Open the Connector database from the server that is currently responsible for managing the database you wish to modify (distributed mode) or the administration server (centralized mode).
2.  Select "Databases" from the menu.
3.  Locate the database in the view and open the document.
4.  Press the "Edit button".
5.  Press the "Roll-back" button. At this point the following dialogue will appear:

<img src='https://google-enterprise-connector-notes.googlecode.com/svn/wiki/images/RebuildDB.png' width='600' height='200' />

If you wish to activate a full rebuild the index for the source database, press OK. Alternatively, you can roll the database back by a specific number of months, days, and hours. Rolling back is useful if your source database has become corrupt and a restore from backup has been made. In this instance you can wind the index back to a time shortly before the backup was taken.

At this point the following events will occur:

  * All index documents (or documents added since the rollback time) for this database will be marked for removal at the GSA. A prompt showing the number of documents removed will be displayed.
  * The last indexed date will be cleared (or set back to the rollback time). This ensures that a full index rebuild (or an incremental build from the rollback time) of this database will occur the next time the monitored database is indexed.
  * The database parameters document will be switched to read mode.

6.  Press "Exit" to close the database parameters document.

**NOTE: The following rules apply:**

  * When the crawler is in distributed mode it is only possible to carry out this action from the server responsible for indexing the database concerned.
  * When the crawler is in centralized mode it is only possible to carry out this action from the administration server.
  * The database must be enabled.
  * The “Roll-back” button is only visible for databases that have been indexed previously.