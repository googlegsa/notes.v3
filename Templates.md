# Templates #
**NOTE: This page contains instructions for configuration of Lotus Notes Templates. You should make sure you have access to a Notes Administrator to carry out these tasks.**

Most crawlers for Notes only support full content crawling of a Notes database. Invariably however, a Notes database will contain documents and fields that are used only to support the Notes application itself. This information adds no value to the search experience and as such should be excluded from the search index. the Google Search Appliance Connector for Lotus Notes offers this support through the use of its template profiles.

Without an understanding of the design of the Notes database it is difficult to know what information is of value and what isn’t and therefore the normal approach to building a searchable index is to crawl and extract every item on each Notes document.

Every Notes database that is registered with the Connector must be assigned to a template profile. Templates tell the system which documents and fields to select as part of the content extraction process for the GSA. They also tell the system how to build key metadata values such as document title and description.

In this way, you can control exactly which information is extracted form your Notes databases for inclusion at the GSA.

The diagram below shows how template information is used to determine what information is extracted from each database that has been registered with the Connector.

This approach has three main benefits. These are:
  * Templates allow you to control exactly what Notes information is sent to the GSA
  * Form profiles allow common metadata values prior to submission to the GSA.
  * Document selection ensures that only documents of value are sent to the GSA. Any document that falls outside of the criteria specified after inclusion in the index will be removed from the GSA just as if it had been deleted from the source database

|Sample templates are provided with the connector for the more common types of Notes database such as Discussion, Team room, Document Library, Office Library, Mail files, Document Manager etc. For the lazy, there is even an “All documents and fields” template. These can be used as is or modified as required. You can also define your own profiles for any custom applications as required. |
|:-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|

Although the Google Search Appliance Connector For Lotus Notes does support full content crawling through it’s **All documents and fields** template, the Connector also offers the far more effective selective content crawling by using template profiles to describe each type of database that will be crawled. These profiles tell the crawler which documents and fields to select as part of the crawl process for each assigned database.

In addition to defining the documents and fields to include in the index, templates in the Connector database are used to define the key meta data values that are passed to the GSA for each Notes document. These are the document title and document description values.

### Using Templates to leverage Metadata ###
The Connector provides a facility to make any Notes field a metadata item in addition to the default values shown above. You can then use this information in search results, search forms, and to support field level search of your Notes content from the GSA using the advanced Google search operators.

By carrying the common metadata from the host Notes document with each attachment record users can discover attachments from the parent document’s metadata as well as through the attachment content itself.

By including link information in the metadata it is possible to offer the following sort of functionality from the search results.

  * Open any attachment from any Notes document found in the result set
  * Open parent document from any attachment
  * Open any other parent document attachment from an attachment in the search results


### Adding a Template to the Database ###
If you wish to extend the provided templates with your own, you can do so by adding your own templates. To add a template to the Connector database, complete the following steps:

  1. Add a [template document](Templates#Adding-a-Template-Document.md).
  1. Add form definition documents.
  1. Add sub form definition documents (optional).

## Adding a Template Document ##

To add a template to the Connector database, complete the following detailed steps:

1. Select "Templates" from the menu.

2. Click the "New Template" button. You will be prompted to enter the template name.

3. Enter the name of the template and press “OK”. The system will now check to make sure that the name has not been used before. If the name is unique, the new template form will be displayed, otherwise you will be asked to try again.

**Complete the “Template Details” tab as follows:**

  * Enter a brief description of the template.
  * Using the button provided choose a database that will be used as the source of design information for this template. Please note the following points:
    1. The system uses the chosen database as the source of its form and sub-form lists and field information.
    1. The databases that you assign to this template later should share a common design with the database you choose here for the template information.
    1. The database you choose here provides design details only; it is not by default indexed by the system.
    1. You can choose to index the chosen database if you wish by adding a database parameters document later.
  * Enter the selection formula to define the documents that should be selected for inclusion in the index. The formula entered here should be written in the same way as a Notes view selection formula; for example, if you wanted to index all Person documents in the Domino Directory (and only Person documents) you would enter:
```
Select Form = "Person"
```
  * The system can optionally automatically generate summary documents. Summary documents will be created automatically for any document that is covered by the selection formula entered above which do not have a form definition document defined. The default setting is “No” and to ensure best performance and meaningful results it is recommended that this setting remains unaltered.


### Auto-generate rules ###
If the template being defined represents databases that meet both of the following rules, you can use auto generation, and there is no requirement to add any form definition documents to the template.
  1. You require ALL fields to be indexed for all documents selected by the template selection formula
  1. You will use field names for the generation of document titles and descriptions (rather than formulae).

To provide greater control over index contents, it is recommended that the auto-generate feature be disabled in favor of defining a definition document for each form that is covered by the document selection formula. Form definition documents are described below.

An example template details tab is shown below. This simple example shows the template parameters required to index the person and group documents in the Domino Directory.

<img src='http://google-enterprise-connector-notes.googlecode.com/svn/wiki/images/FormTemplate.png' />

### Form Details ###
Form documents identify the fields that should be extracted from each of the documents being indexed. They also contain the formulae required to build the title and description values that are held on each index document.

1. Switch to the Form Details tab. An example screen is shown below:

<img src='http://google-enterprise-connector-notes.googlecode.com/svn/wiki/images/FormDefinition.png' />

2. Press the “Add Form Definition” button. Select the desired form from the list presented and press “OK”. The new form document will be displayed for completion. An example screen is shown below:

<img src='http://google-enterprise-connector-notes.googlecode.com/svn/wiki/images/FormDetails.png' />

3. Enter a formula to generate the search results text. For example, if you were defining the form document for the Person form in the Domino Directory and you wanted to display the name and telephone number of each person in the search results, you might enter the following formula:
```
FirstName + " " + LastName + " (" + OfficePhoneNumber + ")"
```

4. Enter a formula to generate the description of the document. In our example of the Domino Directory, if you wished to show each person's job title, department and country as the description of the document you might enter the following formula:
```
"Job Title " + JobTitle + ", Department " + Department + ", Country " + Country
```
_**Tip**: Use the “Validate formulae” button to test the formula syntax._

5. Switch to the “Field Details” tab. An example screen is shown below using the Person form from the address book. The system automatically presents the fields on the form for selection.

<img src='http://google-enterprise-connector-notes.googlecode.com/svn/wiki/images/FieldDetails.png' />

6. Enter the names of any additional fields you wish to include in the index that are not shown in the auto-generated list. This allows you to index fields, which may be present on the documents, but not on the form itself as might be the case where a field has been added to a document through the use of an agent. Separate multiple values with a comma or a semi-colon.

7. Select the fields that you wish to extract from each source document that uses this form.

8. Press the “Save” button to save the form definition document.

9. If the form contains sub forms, continue from step 10, otherwise press the “Exit” button to return to the template document.

10. To add a sub form definition, move to the “Sub form Details” tab, and press the “Add Sub form Details” button.

11. Select the desired sub form from the list presented and press “OK”. The new sub form document will be displayed for completion.

12. Select the fields that you wish to extract from each source document that uses this sub form.

13. Press the “Save” button to save the sub form definition document.

14. Press the “Exit” button to return to the form definition document.

Repeat steps 10 to 14 to add all sub forms as required for the current form.

Repeat steps 1 to 9 to add all forms as required for the current template.

### Reassigning a template ###
The Connector template definition documents read the design of the database originally chosen when the template document was first created to get form, sub-form and field details to assist in the completion of template information.

Occasionally you may need to modify a set of template parameters but the original database is no longer available for reference. In such instances, you can re-assign the template profile to a new database. Should you ever need to do this however, you should be sure that both the old and new databases share a common design.

To reassign a template to a new database, complete the following steps:

  1. Select the “Templates” option from the menu.
  1. Locate the desired template and double click to open the document.
  1. Press the “Edit” button to take the document into edit mode.
  1. Press the “Change Source Database” button, and answer, “Yes” to the warning prompt.
  1. From the Open-Database dialogue, locate and select the new database on one of your servers and press the “Open” button.
  1. Press the “Save” action bar button.
  1. You can now make changes to the template parameters as required.


---


_**Note**: To ensure that system integrity is maintained at all times, it is only possible to create and edit template documents and their associated form and sub-form definition documents from the administration server._