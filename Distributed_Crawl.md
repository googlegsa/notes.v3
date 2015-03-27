## Deployment of the Connector – Distributed crawler mode ##

In distributed mode, the Connector database is replicated to multiple Domino servers and the crawl process is shared amongst these servers. In this mode the crawler on each server is restricted to crawling databases only on the host server. As the Connector is replicated, each server’s updates (index stubs) can be seen from any other replica providing a consistent view of GSA entries across all instances. In addition, a built in locking mechanism ensures that each registered database can only be managed from one single instance of the Connector. This ensures that Notes documents can never be duplicated at the GSA thus the integrity of the GSA index is guaranteed at all times.

Distributed mode is useful where there is a large geographical spread of servers.


## Deployment of the Connector – Centralized crawler mode ##

In centralized mode, the Connector is installed onto a single Domino server. As part of the setup process, the administrator will choose the Connector’s Administration Server. This is the server that will be responsible for all index updates and request handling. The only additional requirement is that the administration server is listed as a trusted server in each remote server’s Server document in the Domino Directory.

For example, if the Connector database is installed onto a server named “Venus” and you wanted to index a database on “Saturn”, then you would update Saturn’s server document to include the server Venus in it’s “Trusted server” list. A screenshot is shown below, but please refer to your Domino Administrator guide for more details on configuring trusted servers.

<img src='https://google-enterprise-connector-notes.googlecode.com/svn/wiki/images/DistConfig.png' width='400' />

This diagram shows how the crawl and feed process differs depending upon the mode selected.

<img src='https://google-enterprise-connector-notes.googlecode.com/svn/wiki/images/DistCrawlDiagram.png' width='500' />

# Changing the master server for a database #
To change the server that is responsible for indexing a particular database, complete the following steps.

1. Open the Connector database from the server that is currently responsible for managing the database you wish to modify (distributed mode) or the administration server (centralized mode).

2. Select "Databases" from the menu.

3. Locate the database in the view and open the document.

4. Press the "Edit button".

5. Press the "Change Master Server" button. At this point a prompt will appear advising you that changing the index server will force a full rebuild of documents indexed from this database. Select "Yes" to continue.

6. Select the new server from the list of servers presented and press "OK". At this point the following events will occur:
  * All index documents for this database will be marked for removal from the GSA. A prompt showing the number of documents removed will be displayed.
  * The last indexed date will be cleared to indicate to the new index server that a full re-build is required.
  * The database parameters document will be updated with the new master server's name and region.
  * The database parameters document will be switched to read mode. Where the crawler is setup to run in distributed mode, this ensures that the database parameters can no longer be updated from this server.
  * The index for this database will be rebuilt the next time the crawler runs on this server (centralized mode) or once the new server receives the updated parameters document through normal replication (distributed mode).
7. Press "Exit" to close the database parameters document.


**NOTE: The following rules apply**
  * When the crawler is in distributed mode it is only possible to carry out this action from the server responsible for indexing the database concerned.
  * When the crawler is in centralized mode it is only possible to carry out this action from the administration server.
  * The database must be enabled.