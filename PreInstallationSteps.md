## Pre-Installation Steps - ##
### Lotus Notes Environment Preparation ###
You will need to complete the following tasks to install the connector.

### Install Dependent Java Libraries ###
The Connector uses the Jakarta Commons HttpClient software to post content to the GSA so before any feeding can begin, these files must be copied to your Domino Server.

1. Download the Commons clients from the http://archive.apache.org/dist/httpcomponents/.

You will need the following jars:
  * commons-codec-1.3.jar
http://archive.apache.org/dist/commons/codec/binaries/commons-codec-1.3.zip

  * commons-httpclient-3.0.jar
http://archive.apache.org/dist/httpcomponents/commons-httpclient/3.0/binary/commons-httpclient-3.0.zip

  * commons-logging.jar
http://archive.apache.org/dist/commons/logging/binaries/logging.1.0.zip

_**Note:** You only need to deploy the jars to the Domino server on which the Connector is hosted. You do NOT need to deploy them to every Domino server._




2. Copy them to your Domino Server's external Java classes’ folder. On a Microsoft Windows platform, this folder is the **“jvm\lib\ext”** folder, which is located beneath the Domino program folder. For example: If you install Domino to **“C:\Lotus\Domino”**, you would copy your jar files to: **“C:\Lotus\Domino\jvm\lib\ext”**.

3. Update the security restrictions in the server’s **“java.policy”** file. This file is located beneath the Domino program folder in the **“jvm\lib\security”** folder. Edit the file and add the following permission lines between the braces where other permissions are granted.

```
permission java.util.PropertyPermission "org.apache.commons.logging.simplelog.defaultlog", "write";
permission java.util.logging.LoggingPermission "control";
```

### Configure Notes Agents ###
The crawl and feed processes run as agents inside the Connector database. To ensure that the agents can complete without being prematurely terminated by the server’s agent manager, you should increase the **“Max Lotusscript`/`Java execution time”** daytime and nighttime parameters for any server that will be taking part in the crawl and feed process.

1. Increase the setting in your Domino server document to 360 minutes or higher. This setting can be found in your server document in the **“Agent Manager”** subsection under **“Server tasks”**. The screenshot below shows the **“Agent Manager”** tab in sample server document.

<img src='http://google-enterprise-connector-notes.googlecode.com/svn/wiki/images/AgentSettings.png' />


2. Restart the Domino server for your changes to take effect.

## GSA Configuration ##
### Configure the GSA for Integration ###
The GSA will only accept information from your Domino servers if you have first identified them through the GSA admin console. To identify your servers complete the following steps.

**NOTE: All screenshots are from GSA version 6.8. These screens are illustrative only, as the Google Search Appliance Connector For Lotus Notes is compatible with versions of GSA 6.4 above.**

1. Log into the GSA as a user of type **Administrator**, and selected the "Crawl and Index" menu option.

2.Complete the “Crawl URLs” section by adding the fully qualified host name of your Domino server. Below you can see how an example server is identified to the Appliance.

<img src='http://google-enterprise-connector-notes.googlecode.com/svn/wiki/images/FollowAndCrawl.png' width='800' />

**Note: You must include the _"http://"_ prefix and the trailing forward slash _"/"_**

Note. To find the fully qualified host name of your own servers, refer to the Basics tab of your Server documents in the Domino directory.

3. Select the “Feeds” option from the “Crawl and Index” menu and confirm that the GSA is ready to accept content from your Notes server. Enter any IP Addresses from which the GSA can receive feeds, in a comma-separated format.

### GSA Serve Time Configurations ###

### Configure the GSA for Authentication and Authorization ###
The Appliance supports a variety of options here including LDAP, forms based authentication, SSO, client certificates, and custom authentication schemes using the SAML standard. By default, the connector will be configured using the connector’s own implementation of the Google Authorization (AuthN) SPI. This implementation will authenticate users against the Domino directory on behalf of the GSA. To configure the GSA to use the Connectors AuthN implementation, complete the following steps.

Please Note: For all the URLs below (Login, AuthN and AuthZ) use "https://" if Domino Server HTTP service is configured to run in secure. Else use "http//"



#### Configuration when Security Manager is enabled (GSA 6.4 and above) ####

1. Login to the GSA as an administrator user and select Universal Login Auth Mechanisms from the Serving menu and select the SAML Tab. Complete the four fields as described below:
**Mechanism Name**

Give the SAML Mechanism a name. For e.g. LotusSAML

**Issuer Id**
Give the issuer id as LotusSAMLIssuer

**Login URL**

Enter the URL to the Login form in the Access Control database. The URL should take the form **"https://"** plus the **"fully qualified Domino server hostname/"** plus the **"access control database path"**, and **"/Login?OpenForm"**. For example:

```
https://notes.yourdomain.com/google-access-control.nsf/Login?OpenForm 
```

**Artifact Resolve URL**

Enter the URL to the authentication agent in the Access Control database. The URL should take the form **"https://"** plus the **"fully qualified Domino server hostname/"** plus the **"access control database path”**, and **"/AuthN?OpenAgent"**. For example:
```
https://notes.yourdomain.com/google-access-control.nsf/AuthN?OpenAgent
```

Click on Save.

An example of the SAML Configuration on the Universal Login Auth Mechanims at the GSA is shown below:

<img src='http://google-enterprise-connector-notes.googlecode.com/svn/wiki/images/AuthN_GSA.PNG' />

2. Select Access Control from the Serving Menu and set the following:-
**Authorization – Authorization Service URL**

Enter the URL to the authorization agent in the Access Control database. The URL should take the form **"https://"** plus the **"fully qualified Domino server hostname/"** plus the **"access control database path"**, and **"/AuthZ?OpenAgent"**. For example:

```
https://notes.yourdomain.com/google-access-control.nsf/AuthZ?OpenAgent
```


An example of the Access Control form at the GSA is shown below:

<img src='http://google-enterprise-connector-notes.googlecode.com/svn/wiki/images/AuthZ_GSA.PNG' />

**Note: Both HTTP and HTTPS are supported. However, if you use HTTPS you must ensure that the GSA and your Domino servers can communicate over SSL. You should refer to the GSA Help and your Domino Administration Help database for more details on this subject.**

### Set the serve protocol ###

  1. From the “Administration” menu, select “SSL Settings”.
  1. Set the “Force secure connections when serving?” field to “**Use HTTPS when serving secure results, but not when serving public results.**”.
  1. Save these settings with the “Save Setup” button and logout of the GSA admin console.

### Customized Frontend Stylesheet for the GSA ###

If you want to use the custom stylesheet optimized for the Notes Connector, you should upload this to your frontend of choice on the GSA via Serving > Front Ends, using the stylesheet file **domino\_stylesheet.xslt**. This is an example stylesheet that shows how you can use the meta data sent from the connector to server custom results at the GSA. Note: This stylesheet does not consider any other result type except Notes so it will need to be modified to present other data sources at the GSA in any production environment.