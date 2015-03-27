# Installation Instructions #
The **Google Search Appliance Connector For Lotus Notes** version 2.6.10 and earlier is installed entirely on your Domino infrastructure.

You should install the connector to a domino server to which you have administrative access.


## Connector Installation ##
### Create the Google Search Appliance Connector For Lotus Notes databases ###
1. Copy the two templates (.NTF files) provided to your Notes client data directory

2. Rename the file "google-access-control-xxx.ntf" to "google-access-control.ntf"

3. Using the Domino Administrator client, sign them with a trusted ID.

4.    Before creating the database, the connector templates (google-connector.ntf and google-access-control.ntf) in the Domino Data  folder, need to be signed using the ID of the user that is going to run the connector using the Domino Administrator
> Steps to sign the template

> a.    Sign in to  the Domino Administrator with the User ID of the user that is going to run the connector

> b.    Click on the “google-connector.ntf”

> c.    Click on Tools-->Database-->Sign

> d.    Click on OK


<img src='http://google-enterprise-connector-notes.googlecode.com/svn/wiki/images/SignTemplate.JPG' />

The ID you sign the template with should have rights to run restricted agents on all servers where the Connector database is to be deployed and at least READER level access to all registered databases and their document sets.

5.Create an instance of the Domino Connector database from the “google-connector-xxx.ntf” template in the root Notes data directory on your Domino server.

Upon initial creation the database will automatically open to a new setup form. An example screenshot is provided below.

<img src='http://google-enterprise-connector-notes.googlecode.com/svn/wiki/images/InitialScreen.png' />

6. Set the appropriate value for your Administrators group, and your log retention period.

7. Specify the User ID in the Programmability Restrictions section of the Current Server Document.
Current Server Document-->Security-->Programmability Restrictions

<img src='http://google-enterprise-connector-notes.googlecode.com/svn/wiki/images/AgentSignPriv.JPG' />

### Configure the GSA Settings ###
  1. Click on the Google Search Appliance Tab
  1. Enter the fully qualified host name of the appliance. The host name should include the Feeder listener’s port number and folder. This is **19900/xmlfeed**. For example
```
gsa.yourdomain.com:19900/xmlfeed
```
For more information please refer to the GSA help materials.

2. Select a Security Mechanism,
Important: If you leave the GSA authentication method set to “none” then ALL Notes documents will be sent to the GSA as public documents irrespective of the restrictions on the Notes document itself. Any other setting will ensure that the GSA checks for authorization for each result that does not by default allow reader access to everyone.

It is also worth noting that the Connector crawler will only mark a document as secure for the GSA where security is imposed against the document either through database ACL restrictions, or document level readers/authors fields, or a combination of both. If a document does not have security imposed, it will be marked as public when it is sent to the GSA.

The Connector also respects groups and application specific database roles when deciding who has reader access to each Notes document.

An example of a completed Google Search Appliance tab is shown below.

<img src='http://google-enterprise-connector-notes.googlecode.com/svn/wiki/images/GSATab.png' />

### Configure your Domino Servers in the Connector ###

The Connector has a built in awareness of the servers in your Domino Organization. It uses this awareness to build the fully qualified URLs to each source document and attachment. The system also uses the server lists to build replica server lists for each database as you register them with the system.

<img src='http://google-enterprise-connector-notes.googlecode.com/svn/wiki/images/DominoServer.png' />

Before a database can be registered with the connector for crawling, the host server must first have been defined in a region document.

1. Click the "Add Region" button. The new region form will be displayed.

<img src='http://google-enterprise-connector-notes.googlecode.com/svn/wiki/images/Region.png' />

2. Enter a descriptive name for the region; e.g. **`CorpDomain`**

3. Enter the domain name for the region.

4. Select the appropriate servers from the list of servers in your Domino directory using the button provided. The screenshot below shows the completed region document.

5. Press the “Save and Exit” button to return to the system setup form.

### Configure Access Control ###

1. Ensure that the filename of your organisation’s Domino Directory is correct. The default is “names.nsf” and in most cases this will not need to be changed. If you are unsure, check with your Domino Administrator.

<img src='http://google-enterprise-connector-notes.googlecode.com/svn/wiki/images/AccessControl.png' />

2. To limit the number of users cached by the access control system, enter a standard Notes document selection formula into the User selection formula field. Tip: Where you have a large number of users (>25,000 users) and your testing is restricted to just a subset of those users, entering a selection formula here will offer optimum “Directory Monitor” performance as it prevents the system from caching unnecessary user details.

3. If you are using the Domino’s own authentication with the name that a user is known by at the GSA will match that user’s Notes name, you can leave the “Formula to use to generate user name field “ blank. Otherwise enter a Formula that can be used to map the Domino name to the username correctly.

4. Press the “Create Access Control Database” button. The access control database will now be created on your Domino server. The Access Control tab will now refresh and you will see the “Open Access Control Database” button. You can of course open the database when you need to using the “File-Database-Open” menu option from your Notes Client.

It is recommended that you now open the access control database, and select the System Setup menu option to reveal the access control setup profile.
**Access Control Database Setup**

On the General Settings, enter the GSA Address and click on "Save"
In the Security Manager Artifact consumer URL, enter the GSA Address and the Security Manager Assertion Consumer URL. In case of Legacy AuthN Mode of the GSA is being used, give the URL as -
https://< gsa>/SamlArtifactConsumer

Else in case of normal SM, give the URL as -
https://< gsa>//security-manager/samlassertionconsumer

Click on "Save"


<img src='http://google-enterprise-connector-notes.googlecode.com/svn/wiki/images/SM_AssertionURL.JPG' />


5. Click on the Advanced tab and enable the logging of authorization requests as shown below. This allows the system to record each incoming request from the GSA and the response. This is particularly useful for checking the incoming username format provided by the GSA.

<img src='http://google-enterprise-connector-notes.googlecode.com/svn/wiki/images/AdvancedAccess.png' />

_**Note**: Whilst this feature is useful for checking your configuration, the logging of authorization requests should not be used in a production environment for performance reasons._

Typically you should leave the "**Response to send for non Notes resources**" to **indeterminate" so that the GSA will revert to it's default Authorization  mechanism.**

6. Press the “Save and Exit” button to close the Access Control database set-up profile. Within an hour the Directory Monitor will have run and built the necessary cache information about your users and groups so that is can handle incoming authorization requests from the GSA.

_**Tip**: You can also run the Directory Monitor at any time using the “Update Now” action button that can be found in the People and Groups views._

_**Note**: The authorization mechanism respects usernames and group names in database ACLs and any Reader/Authors items at the document level. The system also respects application specific roles. Respect for group membership includes implicit as well as explicit group membership. Explicit group membership is where a user is listed directly as a group member. Implicit group membership is where a user can be found in any descendant group. For example: John Smith is considered to a member of “Sales-Global” through his explicit membership of “Sales-APAC” if “Sales-APAC” is a member of “Sales-Global”._

7. Once you have returned to the Connector’s setup profile, press the “Save and Exit” button to close the setup profile in the Connector database.

You are now ready to begin registering databases with the connector.

### Register Databases to be Searched ###

Before a database can be crawled by the system a database parameters document must exist. Databases can be registered one at a time, in bulk, or using the API provided. For more details you should refer to the full Installation and Administration guide. In this scenario, we will register the Domino Directory to demonstrate the process.

To register a new database, complete the following steps.

1. Select "Databases" from the Connector menu.

2. Click the "New-Database" button. The new database form will be displayed.

3. Select the Domino Directory from your Domino server using the button provided.

4. Select the template to apply to this database using the button provided. See [templates](Templates.md) for more information about template selection.

5. Choose the option for "Index Document ACLs". Select "Yes" if the ACLs for each document are to be fed to the GSA. The GSA will use these ACLs while Authorizing documents for search results. Select "No" if the ACLs are not to be fed to the GSA for each document. The GSA will use the Connector AuthZ agent for Authorization of Search Results

6. Save the database using the button provided and exit. The next time the crawler runs the desired content will be added to the system. An example of the completed database parameters document is shown below:

<img src='http://google-enterprise-connector-notes.googlecode.com/svn/wiki/images/Database.png' />

You are encouraged to read the page that covers [template profile management](Templates.md) to gain a full appreciation of the options that are available to in the areas of selective indexing and document transformation.

### Checking the Crawler ###

Once the crawler has run, the databases view will be updated to show the numbers of documents indexed and when as shown in the screenshot below.

<img src='http://google-enterprise-connector-notes.googlecode.com/svn/wiki/images/DBCount.png' />

In addition you can refer to the “Submitted” view to see the document stubs that are retained in the connector that support the ongoing synchronization between the original Notes database and the Appliance. The submitted view following the crawling of our Domino Directory is shown below.

<img src='http://google-enterprise-connector-notes.googlecode.com/svn/wiki/images/Submitted.png' />

From the “Submitted” view, you can open the summary document to get more detailed information. To do this select a document and press the “Open” action bar button, and then choose Summary details. The summary document will be displayed as shown in the screenshot below.

<img src='http://google-enterprise-connector-notes.googlecode.com/svn/wiki/images/DocSummary.PNG' />

From here you can view the details that are retained in the connector. The security tab shows which users and groups have reader or above access to this document. An example is shown below.

<img src='http://google-enterprise-connector-notes.googlecode.com/svn/wiki/images/DocSecurity.png' />

You will note that because there are values in the Readers token, that the GSA authentication method is set to “httpbasic”. This ensures that the GSA will seek authorization from Domino before showing this document to any user in the search results.