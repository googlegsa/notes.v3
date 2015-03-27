# Introduction #
The Google Search Appliance Connector for Lotus Notes allows you to easily integrate content securely with Lotus Notes, seamlessly integrating your Notes content with the rest of your enterprise content.

The Google Search Appliance Connector for Lotus Notes is a secure, highly flexible crawler for Notes that connects your Domino servers to the GSA, using a crawler which has been fully tested and approved by IBM Laboratories. This software was designed and developed by Notes developers with an in depth understanding of the design structure, principles and advantages of the Notes database environment. As such the system has a number of features rarely seen in a Notes database crawler.

Read the core [Installation Guide](InstallationGuide.md) now.

Read about [Rebuilding a Single Database](Rebuild_single_database.md).

Read about [Rebuilding All Databases from a Template](Rebuilding_databases.md).

Read about [Configuring for Distributed crawling](Distributed_Crawl.md).

**Still to come**

Stay tuned for documentation on:
  * Selective indexing

# Features #
### Native Notes application ###
The Connector is a native Notes application. As such it can be installed onto any Notes server platform [R6](https://code.google.com/p/google-enterprise-connector-notes/source/detail?r=6) or later. The Connector supports any Notes database regardless of its design and search results at the GSA can be opened in the browser or using the Notes client.

### Full and “Selective” content crawling ###
For each type of database registered, you choose precisely which documents and fields you wish to index through template configuration profiles. These profiles tell the Connector which content to send to the GSA and how to build key metadata values. Once a template profile is defined, you can assign any number of databases to it so that content extraction is consistent across all databases of the same type. Sample templates are provided for common Notes database types such as Document Libraries, Discussion, Team Rooms, Mail files, etc.

### Respect for document level security ###
The connector integrates with GSA security to ensure that only documents that a user has at least Reader access to in the source databases are displayed in the search results. The Connector respects all levels of security (database ACL and document level items) including ACL roles present in Readers/Authors fields.

The Connector also monitors the ACL of each database and where changes are made that impact the confidentiality of contained documents, the system will automatically recreate index entries with the updated security information.

### Replica awareness ###
The system has a built in understanding of the location of your servers. This ensures that duplicate content can never be submitted to the GSA where replicas of databases reside on multiple servers.

### Make any Notes field a GSA Metadata value ###
Through the modification of a style sheet, you can make any of your Notes fields available as a GSA metadata value to support named field searches at the GSA.

### Support for Notes document versioning ###
Where a Notes application saves each update as a new Notes document, (such as a document management application) you can choose to have the Connector remove all prior versions of the saved document from the GSA as the new version is added to the index. This ensures that your users are only ever presented with the latest version of the document in the search results.