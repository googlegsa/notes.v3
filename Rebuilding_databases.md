# Introduction #
Templates can define the indexing parameters for potentially hundreds of databases. The index for an individual database may be rebuilt from the databases parameters document as described in the previous section. However, if there is a change to the template (for example, you wish to include some additional fields from a particular form), you can rebuild all databases that use that template in one step rather than rebuilding the index for each database individually.


# Details #
To rebuild a single database see [Rebuild A Single Database](http://code.google.com/p/google-enterprise-connector-notes/wiki/Rebuild_single_database?ts=1275929130&updated=Rebuild_single_database).
To rebuild the index documents for all databases that use a specific template, complete the following steps:

  1. Select "Templates" from the menu.
  1. Open the desired template document, and press the "Edit" button.
  1. Press the "Rebuild Database Indexes" button. At this point you will be asked if you wish to continue with this action. Select "Yes" to rebuild all indexes. Another prompt confirming that the rebuild will take place will appear.
  1. Press "OK" at this prompt to close the prompt and the template document.

**NOTE: It is only possible to carry out this action from the administration server.**