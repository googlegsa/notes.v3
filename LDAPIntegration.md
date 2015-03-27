# LDAP Authentication #
One authentication method supported by the GSA is LDAP. Microsoft’s Active Directory is one such LDAP compliant directory. The following is a discussion on how to configure the GSA to use Active Directory as the authentication method for your Notes users, and then use the Access Control database supplied to provide the authorization mechanism.

This can be useful when you wish to use NTLM as the default Authorization mechanism, so that users can Authenticate once against Active Directory, and have the same credentials used to Authorize against Notes content, and content protected by NTLM.

# Details #

1. In the Connector system setup profile, set your GSA authentication method to Http Basic or NTLM.

<img src='http://google-enterprise-connector-notes.googlecode.com/svn/wiki/images/HTTPBasicForLDAP.png' />

2. Create the Access Control database from the Access Control tab in the Connector system setup profile. As part of this process you must ensure that the system has a way to map a Notes name to the LDAP username. To achieve this mapping you must enter a formula or a Person document field name that the authorization system can use to map the logon name (as held at the GSA) to the Notes username.

Consider the following example:

| **Notes name** | **Active Directory login name** | **LDAP name returned to the GSA after login** |
|:---------------|:--------------------------------|:----------------------------------------------|
|John Doe/my-domain |<p>jdoe </p> <p> <b>Or</b> </p> <p>jdoe@mydomain.com </p><p>Where “mydomain.com” is the AD DNS suffix, and “John” and “Doe” are the first and last name values in AD for this user.</p> | CN=John Doe,CN=Users,DC=mydomain,DC=com |

In the example above, you have two choices:

  * You could add a field to the person document form (or the **$PersonExtensibleSchema** sub-form), which computes the LDAP name from the Notes name and then update your existing users with a simple formula agent. Then enter this fieldname into the "**Formula to use to generate user name**" field.
  * Alternatively you could simply add the following formula to the "**Formula to use to generate user name**" field. This will then evaluate at run time and there is no need to make any changes to your Domino directory.
```
 "CN=" + FirstName + " " + LastName + ",CN=Users,DC=c-search-solutions,DC=com"
```

Note: [R4](https://code.google.com/p/google-enterprise-connector-notes/source/detail?r=4).6.4 of the GSA software will store the user’s fully distinguished name from the Active Directory as shown above. However, [R5](https://code.google.com/p/google-enterprise-connector-notes/source/detail?r=5) of the GSA software now holds the user’s actual login name instead of the fully distinguished name. Depending upon which version of the GSA software you are running you will need to adjust your formula as required.

If you are unsure about the username format that is being used by the GSA to identify each user, you can enable the logging of authorization requests from the Access Control database’s setup profile and then perform a search across secure content. The incoming authorization requests can then be examined from the Requests view. From the incoming request you will be able to identify the user name that is held at the GSA and then adjust your Notes setup formulae accordingly to allow the correct mapping to occur between the two names.

_**Note**: If you change the formula you will need to refresh the access control lists by running the Directory Monitor once more using the “Update Now” button._

3. Run the Directory Monitor agent in the Access control database to allow the authorization cache to be built for your users and groups.

4. Login to the GSA as the Admin user, and from the Administration section, select LDAP settings. Complete the form to point the GSA at your Active Directory. The screenshot below shows the results of the GSA LDAP detection against Active Directory.

**Refer to the GSA Help documentation for more details if required.**

5. In the Authorization SPI section of the GSA (Serving > Access Control), you should enter the URL of the authorization agent in the Access Control database, as documented in the [installation guide](InstallationGuide.md). An example is shown below. (HTTP and HTTPS are supported. Note: if you use Https you must ensure that the GSA and your Domino servers can communicate over SSL. You should refer to the GSA Help for more details on this subject).

_**Note**: There is no need to complete the Authentication SPI fields here as you are using LDAP for authentication, not Domino as described in the [installation guide](InstallationGuide.md)._

**Other tasks are as documented in the [installation guide](InstallationGuide.md).**