// Copyright 2011 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.enterprise.connector.notes;

import com.google.common.annotations.VisibleForTesting;
import com.google.enterprise.connector.notes.client.NotesDatabase;
import com.google.enterprise.connector.notes.client.NotesDocument;
import com.google.enterprise.connector.notes.client.NotesDocumentCollection;
import com.google.enterprise.connector.notes.client.NotesEmbeddedObject;
import com.google.enterprise.connector.notes.client.NotesItem;
import com.google.enterprise.connector.notes.client.NotesRichTextItem;
import com.google.enterprise.connector.notes.client.NotesSession;
import com.google.enterprise.connector.notes.client.NotesView;
import com.google.enterprise.connector.spi.RepositoryException;
import com.google.enterprise.connector.spi.SpiConstants.ActionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotesCrawlerThread extends Thread {
  private static final String CLASS_NAME = NotesCrawlerThread.class.getName();
  private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);
  static final String META_FIELDS_PREFIX = "x.";

  private String attachRoot = null;
  private NotesConnector nc = null;
  private NotesConnectorSession ncs = null;
  private NotesSession ns = null;
  private NotesDatabase cdb = null;
  private NotesDocument templateDoc = null;
  private NotesDocument formDoc = null;
  private NotesDocumentCollection formsdc = null;
  private String openDbRepId = "";
  private NotesDatabase srcdb = null;
  private NotesView crawlQueue = null;

  @VisibleForTesting
  List<MetaField> metaFields;

  NotesCrawlerThread(NotesConnector Connector, NotesConnectorSession Session) {
    final String METHOD = "NotesCrawlerThread";
    LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
        "NotesCrawlerThread being created.");

    nc = Connector;
    ncs = Session;
  }

  // Since we are multi-threaded, each thread has its own objects
  // which are not shared.  Hence the calling thread must pass
  // the Domino objects to this method.
  private static synchronized NotesDocument getNextFromCrawlQueue(
      NotesSession ns, NotesView crawlQueue) {
    final String METHOD = "getNextFromCrawlQueue";
    try {
      crawlQueue.refresh();
      NotesDocument nextDoc = crawlQueue.getFirstDocument();
      if (nextDoc == null) {
        return null;
      }
      LOGGER.logp(Level.FINER, CLASS_NAME, METHOD, "Prefetching document");
      nextDoc.replaceItemValue(NCCONST.NCITM_STATE, NCCONST.STATEINCRAWL);
      nextDoc.save(true);

      return nextDoc;
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, CLASS_NAME, e);
    } finally {
    }
    return null;
  }

  protected void loadTemplateDoc(String TemplateName)
      throws RepositoryException {
    final String METHOD = "loadTemplate";
    LOGGER.entering(CLASS_NAME, METHOD);

    // Is a template document all ready loaded?
    if (null != templateDoc) {
      // Is this the one we need?
      if (TemplateName.equals(
              templateDoc.getItemValueString(NCCONST.TITM_TEMPLATENAME))) {
        return;
      }
      templateDoc.recycle();
      templateDoc = null;
      if (null != formsdc) {
        formsdc.recycle();
      }
      formsdc = null;
      if (null != formDoc) {
        formDoc.recycle();
      }
      formDoc = null;
    }
    NotesView vw = cdb.getView(NCCONST.VIEWTEMPLATES);
    templateDoc = vw.getDocumentByKey(TemplateName, true);
    formsdc = templateDoc.getResponses();

    // Parse any configured MetaFields once per template load.
    Vector templateMetaFields =
        templateDoc.getItemValue(NCCONST.TITM_METAFIELDS);
    metaFields = new ArrayList<MetaField>(templateMetaFields.size());
    for (Object o : templateMetaFields) {
      metaFields.add(new MetaField((String) o));
    }
    if (LOGGER.isLoggable(Level.FINEST)) {
      LOGGER.finest("template MetaFields: '" + templateMetaFields
          + "'; parsed MetaFields: " + metaFields);
    }
    vw.recycle();
  }

  protected void loadForm(String FormName) throws RepositoryException {
    final String METHOD = "loadForm";
    LOGGER.entering(CLASS_NAME, METHOD);

    if (null != formDoc) {
      if (FormName == formDoc.getItemValueString(NCCONST.FITM_LASTALIAS)) {
        return;
      }
      formDoc.recycle();
      formDoc = null;
    }
    if (null == formsdc) {
      return;
    }
    formDoc = formsdc.getFirstDocument();
    while (null != formDoc) {
      String formDocName = formDoc.getItemValueString(NCCONST.FITM_LASTALIAS);
      if (formDocName.equals(FormName)) {
        return;
      }
      NotesDocument prevDoc = formDoc;
      formDoc = formsdc.getNextDocument(prevDoc);
      prevDoc.recycle();
    }
  }

  /*
   *   Some comments on Domino.
   *
   *   Reader security is only enforced in Domino if there are
   *   Readers fields on the document and they are non-blank
   *
   *   Authors fields also provide read access to the document if
   *   document level security is enforced.  However if there are
   *   authors fields, but not any non-blank readers fields,
   *   document level security will not be enforced.
   */
  protected boolean getDocumentReaderNames(NotesDocument crawlDoc,
      NotesDocument srcDoc) throws RepositoryException {
    final String METHOD = "getDocumentReaderNames";
    LOGGER.entering(CLASS_NAME, METHOD);

    Vector<?> allItems = srcDoc.getItems();
    try {
      Vector<String> authorReaders = new Vector<String>();
      boolean hasReaders = false;
      int authorItemIndex = -1;

      // Find the Readers field, if any.
      for (int i = 0; i < allItems.size(); i++) {
        NotesItem item = (NotesItem) allItems.elementAt(i);
        if (item.isReaders()) {
          hasReaders = copyValues(item, authorReaders, "readers");
        } else if (item.isAuthors()) {
          authorItemIndex = i;
        }
      }
      // If there are Readers, add any Authors to the Readers list
      // for AuthZ purposes. With no Readers, database security applies.
      if (hasReaders && authorItemIndex != -1) {
        copyValues((NotesItem) allItems.elementAt(authorItemIndex),
            authorReaders, "authors");
      }

      LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
          "Document readers are " + authorReaders);
      if (authorReaders.size() > 0) {
        crawlDoc.replaceItemValue(NCCONST.NCITM_DOCAUTHORREADERS, authorReaders);
        crawlDoc.replaceItemValue(NCCONST.NCITM_DOCREADERS, authorReaders);
      }
      return hasReaders;
    } finally {
      srcDoc.recycle(allItems);
    }
  }

  private boolean copyValues(NotesItem item, Vector<String> destination,
      String description) throws RepositoryException {
    final String METHOD = "copyValues";
    Vector values = item.getValues();
    int count = 0;
    if (null != values) {
      LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
          "Adding " + description + " " + values.toString());
      for (; count < values.size(); count++) {
        destination.add(values.elementAt(count).toString().toLowerCase());
      }
    }
    return count > 0;
  }

  // This function will set google security fields for the document
  protected void setDocumentSecurity(NotesDocument crawlDoc,
      NotesDocument srcDoc) throws RepositoryException {
    final String METHOD = "setDocumentSecurity";
    LOGGER.entering(CLASS_NAME, METHOD);

    String AuthType = crawlDoc.getItemValueString(NCCONST.NCITM_AUTHTYPE);

    if (AuthType.equals(NCCONST.AUTH_NONE)) {
      crawlDoc.replaceItemValue(NCCONST.ITM_ISPUBLIC, Boolean.TRUE.toString());
      return;
    }
    if (AuthType.equals(NCCONST.AUTH_ACL)) {
      crawlDoc.replaceItemValue(NCCONST.ITM_ISPUBLIC, Boolean.FALSE.toString());

      ;  // TODO: Handle document ACLs
      return;
    }
    if (AuthType.equals(NCCONST.AUTH_CONNECTOR)) {
      crawlDoc.replaceItemValue(NCCONST.ITM_ISPUBLIC, Boolean.FALSE.toString());
      ;
      return;
    }
  }

  protected void evaluateField(NotesDocument crawlDoc, NotesDocument srcDoc,
      String formula, String ItemName, String Default)
      throws RepositoryException {
    final String METHOD = "evaluateField";
    LOGGER.entering(CLASS_NAME, METHOD);

    Vector<?> VecEvalResult = null;
    String Result = null;
    try {
      LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
          "Evaluating formula for item " + ItemName + " : src is: " + formula);
      VecEvalResult = ns.evaluate(formula, srcDoc);
      // Make sure we dont' get an empty vector or an empty string
      if (VecEvalResult != null) {
        if (VecEvalResult.size() > 0) {
          Result = VecEvalResult.elementAt(0).toString();
          LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
              "Evaluating formula result is: " + Result);
        }
      }
      if (null == Result) {
        Result = Default;
      }
      if (Result.length() == 0) {
        Result = Default;
      }
    } catch (RepositoryException e) {
      LOGGER.log(Level.SEVERE, CLASS_NAME, e);
    } finally {
      crawlDoc.replaceItemValue(ItemName, Result);
    }
    LOGGER.exiting(CLASS_NAME, METHOD);
  }


  // TODO: Consider mapping other fields so they can be used for
  // dynamic navigation.  This could be an configurable option.

  // This function will map the fields from the source database
  // to the crawl doc using the configuration specified in
  // formDoc
  protected void mapFields(NotesDocument crawlDoc, NotesDocument srcDoc)
      throws RepositoryException {
    final String METHOD = "mapFields";
    LOGGER.entering(CLASS_NAME, METHOD);

    // Copy the standard fields
    String NotesURL = srcDoc.getNotesURL();
    String HttpURL = getHTTPURL(crawlDoc);
    crawlDoc.replaceItemValue(NCCONST.ITM_DOCID, HttpURL);
    crawlDoc.replaceItemValue(NCCONST.ITM_GMETAFORM,
        srcDoc.getItemValueString(NCCONST.ITMFORM));
    crawlDoc.replaceItemValue(NCCONST.ITM_LASTMODIFIED,
        srcDoc.getLastModified());
    crawlDoc.replaceItemValue(NCCONST.ITM_GMETAWRITERNAME, srcDoc.getAuthors());
    crawlDoc.replaceItemValue(NCCONST.ITM_GMETALASTUPDATE,
        srcDoc.getLastModified());
    crawlDoc.replaceItemValue(NCCONST.ITM_GMETACREATEDATE, srcDoc.getCreated());

    // We need to generate the title and description using a formula
    String formula = null;
    // When there is no form configuration use the config from the template
    if (formDoc != null) {
      formula = formDoc.getItemValueString(NCCONST.FITM_SEARCHRESULTSFORMULA);
    }
    else {
      formula = templateDoc.getItemValueString(NCCONST.TITM_SEARCHRESULTSFIELDS);
    }
    evaluateField(crawlDoc, srcDoc, formula, NCCONST.ITM_TITLE, "");

    // Again..when there is no form configuration use the config
    // from the template
    if (formDoc != null) {
      formula = formDoc.getItemValueString(NCCONST.FITM_DESCRIPTIONFORMULA);
    }
    else {
      formula = templateDoc.getItemValueString(NCCONST.TITM_DESCRIPTIONFIELDS);
    }
    evaluateField(crawlDoc, srcDoc, formula, NCCONST.ITM_GMETADESCRIPTION, "");
    LOGGER.exiting(CLASS_NAME, METHOD);

    // Don't map these here -> just do it in the document properties
    // crawlDoc.replaceItemValue(NCCONST.ITM_DISPLAYURL, HttpURL);
    // crawlDoc.replaceItemValue(NCCONST.ITM_GMETATOPIC,
    //     VecSearchTitle.elementAt(0));
    // DO NOT MAP THIS FIELD - it will force the GSA to try and crawl this URL
    // crawlDoc.replaceItemValue(NCCONST.ITM_SEARCHURL, HttpURL);
  }

  @VisibleForTesting
  void mapMetaFields(NotesDocument crawlDoc, NotesDocument srcDoc)
      throws RepositoryException {
    final String METHOD = "mapMetaFields";
    LOGGER.entering(CLASS_NAME, METHOD);
    NotesItem item = null;
    for (MetaField mf : metaFields) {
      try {
        if (null == mf.getFieldName()) {
          if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
                "Skipping null fieldname");
          }
          continue;
        }
        String configForm = mf.getFormName();
        if (null != configForm) {
          String docForm = srcDoc.getItemValueString(NCCONST.ITMFORM);
          if (!configForm.equalsIgnoreCase(docForm)) {
            if (LOGGER.isLoggable(Level.FINEST)) {
              LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
                  "Skipping metafields because configured form {0} does not "
                  + "match doc form {1}",
                  new Object[] { configForm, docForm });
            }
            continue;
          }
        }
        if (!srcDoc.hasItem(mf.getFieldName())) {
          if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
                "Source doc does not have field: " + mf.getFieldName());
          }
          continue;
        }
        // If there are multiple items with the same name (not a
        // common Notes occurrence), only the first item will be
        // mapped.
        item = srcDoc.getFirstItem(mf.getFieldName());
        if (null == item.getValues()) {
          if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
                "Source doc does not have value for: " + mf.getFieldName());
          }
          continue;
        }
        Object content = item;
        if (item.getType() == NotesItem.RICHTEXT) {
          content = item.getText(2 * 1024);
        }
        if (crawlDoc.hasItem(META_FIELDS_PREFIX + mf.getMetaName())) {
          LOGGER.logp(Level.WARNING, CLASS_NAME, METHOD,
              "Mapping meta fields: meta field {0} already exists in crawl doc",
              mf.getMetaName());
          // If multiple Notes fields are mapped to the same meta
          // field, only the first mapping will be used.
          continue;
        }
        crawlDoc.replaceItemValue(META_FIELDS_PREFIX + mf.getMetaName(),
            content);
        if (LOGGER.isLoggable(Level.FINEST)) {
          LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
              "Mapped meta field : " + META_FIELDS_PREFIX
              + mf.getMetaName() + " =  " + content);
        }
      } catch (RepositoryException e) {
        LOGGER.logp(Level.WARNING, CLASS_NAME, METHOD,
            "Error mapping MetaField " + mf, e);
      } finally {
        if (null != item) {
          item.recycle();
        }
      }
    }
    LOGGER.exiting(CLASS_NAME, METHOD);
  }

  protected String getHTTPURL(NotesDocument crawlDoc)
      throws RepositoryException {

    String httpURL = null;
    String server = null;

    // Get the domain name associated with the server
    server = crawlDoc.getItemValueString(NCCONST.NCITM_SERVER);
    String domain = ncs.getDomain(server);

    httpURL = String.format("http://%s%s/%s/0/%s",
        crawlDoc.getItemValueString(NCCONST.NCITM_SERVER),
        domain,
        crawlDoc.getItemValueString(NCCONST.NCITM_REPLICAID),
        crawlDoc.getItemValueString(NCCONST.NCITM_UNID));
    return httpURL;
  }

  protected String getContentFields(NotesDocument srcDoc)
      throws RepositoryException {
    final String METHOD = "getContentFields";
    LOGGER.entering(CLASS_NAME, METHOD);

    // TODO:  Handle stored forms
    StringBuffer content = new StringBuffer();
    // If we have a form document then we have a specified list
    // of fields to index
    if (null != formDoc) {
      Vector<?> v = formDoc.getItemValue(NCCONST.FITM_FIELDSTOINDEX);
      for (int i = 0; i < v.size(); i++) {
        String fieldName = v.elementAt(i).toString();
        // Fields beginning with $ are reserved fields in Domino
        // Do not index the Form field ever
        if ((fieldName.charAt(0) == '$') ||
            (fieldName.equalsIgnoreCase("form"))) {
          continue;
        }
        content.append("\n");
        NotesItem tmpItem = srcDoc.getFirstItem(fieldName);
        if (null != tmpItem) {
          // Must use getText to get more than 64k of text
          content.append(tmpItem.getText(2 * 1024 * 1024));
          tmpItem.recycle();
        }
      }
      LOGGER.exiting(CLASS_NAME, METHOD);
      return content.toString();
    }

    // Otherwise we will index all allowable fields
    Vector <?> vi = srcDoc.getItems();
    for (int j = 0; j < vi.size(); j++) {
      NotesItem itm = (NotesItem) vi.elementAt(j);
      String ItemName = itm.getName();
      if ((ItemName.charAt(0) == '$') || (ItemName.equalsIgnoreCase("form"))) {
        continue;
      }
      int type = itm.getType();
      switch (type) {
        case NotesItem.TEXT:
        case NotesItem.NUMBERS:
        case NotesItem.DATETIMES:
        case NotesItem.RICHTEXT:
        case NotesItem.NAMES:
        case NotesItem.AUTHORS:
        case NotesItem.READERS:
          content.append("\n");
          NotesItem tmpItem = srcDoc.getFirstItem(ItemName);
          if (null != tmpItem) {
            // Must use getText to get more than 64k of text
            content.append(tmpItem.getText(2 * 1024 * 1024));
            tmpItem.recycle();
          }
          break;
        default:
          break;
      }
    }
    LOGGER.exiting(CLASS_NAME, METHOD);
    return content.toString();
  }

  protected boolean prefetchDoc(NotesDocument crawlDoc) {
    final String METHOD = "prefetchDoc";
    LOGGER.entering(CLASS_NAME, METHOD);

    String NotesURL = null;
    NotesDocument srcDoc = null;
    try {
      NotesURL = crawlDoc.getItemValueString(NCCONST.ITM_GMETANOTESLINK);
      LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
          "Prefetching document " + NotesURL);

      // Get the template for this document
      loadTemplateDoc(crawlDoc.getItemValueString(NCCONST.NCITM_TEMPLATE));
      if (null == templateDoc) {
        LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
            "No template found for document " +
            crawlDoc.getItemValueString(NCCONST.ITM_GMETANOTESLINK));
        return false;
      }

      // Check to see if the database we all ready have open is
      // the right one by comparing replicaids
      String crawlDocDbRepId = crawlDoc.getItemValueString(
          NCCONST.NCITM_REPLICAID);
      if (!crawlDocDbRepId.contentEquals(openDbRepId)) {
        // Different ReplicaId - Recycle and close the old database
        if (srcdb != null) {
          srcdb.recycle();
          srcdb= null;
        }
        // Open the new database
        srcdb = ns.getDatabase(null, null);
        srcdb.openByReplicaID(crawlDoc.getItemValueString(
                NCCONST.NCITM_SERVER), crawlDocDbRepId);
        openDbRepId = crawlDocDbRepId;
      }

      // Load our source document
      srcDoc = srcdb.getDocumentByUNID(crawlDoc.getItemValueString(
              NCCONST.NCITM_UNID));
      // Get the form configuration for this document
      loadForm(srcDoc.getItemValueString(NCCONST.ITMFORM));
      if (null == formDoc) {
        LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
            "No form definition found.  Using template definition " +
            "to process document " + NotesURL);
      }

      boolean hasReaders = getDocumentReaderNames(crawlDoc, srcDoc);
      if (hasReaders) {
        if (NCCONST.AUTH_ACL.equals(
            crawlDoc.getItemValueString(NCCONST.NCITM_AUTHTYPE))) {
          LOGGER.logp(Level.WARNING, CLASS_NAME, METHOD,
              "Document " + NotesURL + " has document-level security, "
              + "but the connector is configured to use database-level "
              + "Policy ACLs. This document will not be indexed.");
          return false;
        }
      }
      setDocumentSecurity(crawlDoc, srcDoc);

      mapFields(crawlDoc, srcDoc);
      mapMetaFields(crawlDoc, srcDoc);

      // Process the attachments associated with this document
      // When there are multiple attachments with the same name
      // Lotus Notes automatically generates unique names for next document
      Vector<?> va = ns.evaluate("@AttachmentNames", srcDoc);

      NotesItem attachItems = crawlDoc.replaceItemValue(
          NCCONST.ITM_GMETAATTACHMENTS, "");
      for (int i = 0; i < va.size(); i++) {
        String attachName = va.elementAt(i).toString();

        if (attachName.length() == 0) {
          continue;
        }
        String xtn = null;
        int period = attachName.lastIndexOf(".");
        if (period == -1) {
          xtn = "";
        } else {
          xtn = attachName.substring(period + 1);
        }
        if (!ncs.isExcludedExtension(xtn.toLowerCase())) {
          boolean success = createAttachmentDoc(crawlDoc, srcDoc,
              attachName, ncs.getMimeType(xtn));
          if (success) {
            attachItems.appendToTextList(attachName);
          }
        } else {
          LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
              "Excluding attachment in " + NotesURL + " : " + attachName);
        }
      }
      crawlDoc.replaceItemValue(NCCONST.ITM_GMETAALLATTACHMENTS, va);
      // Get our content after processing attachments
      // We don't want the document content in the attachment docs
      // Our content must be stored as non-summary rich text to
      // avoid the 32/64K limits in Domino
      NotesRichTextItem contentItem = crawlDoc.createRichTextItem(
          NCCONST.ITM_CONTENT);
      String content = getContentFields(srcDoc);
      contentItem.appendText(content);
      contentItem.setSummary(false);

      // Update the status of the document to be fetched.
      crawlDoc.replaceItemValue(NCCONST.ITM_ACTION, ActionType.ADD.toString());
      srcDoc.recycle();
      return true;
    } catch(Exception e) {
      LOGGER.log(Level.SEVERE, CLASS_NAME, e);
      return false;
    } finally {
      LOGGER.exiting(CLASS_NAME, METHOD);
    }
  }

  // This function creates a document for an attachment
  public boolean createAttachmentDoc(NotesDocument crawlDoc,
      NotesDocument srcDoc, String AttachmentName, String MimeType)
      throws RepositoryException {
    final String METHOD = "createAttachmentDoc";
    String AttachmentURL = null;
    LOGGER.entering(CLASS_NAME, METHOD);
    NotesEmbeddedObject eo = null;
    NotesDocument attachDoc = null;

    try {
      // Error access the attachment
      eo = srcDoc.getAttachment(AttachmentName);

      if (eo.getType() != NotesEmbeddedObject.EMBED_ATTACHMENT) {
        // The object is not an attachment - could be an OLE object or link
        LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
            "Ignoring embedded object " + AttachmentName);
        eo.recycle();
        return false;
      }

      if (null == eo) {
        LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
            "Attachment could not be accessed " + AttachmentName);
        return false;
      }

      // Don't send attachments larger than the limit
      if (eo.getFileSize() > ncs.getMaxFileSize()) {
        LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
            "Attachment larger than the configured limit and content " +
            "will not be sent. " + AttachmentName);
      }

      attachDoc = cdb.createDocument();
      crawlDoc.copyAllItems(attachDoc, true);
      crawlDoc.replaceItemValue(NCCONST.ITM_GMETAATTACHMENTS, AttachmentName);

      String encodedAttachmentName = null;
      try {
        encodedAttachmentName = java.net.URLEncoder.encode(
            AttachmentName, "UTF-8");
      } catch (Exception e) {
        attachDoc.recycle();
        eo.recycle();
        return false;
      }
      AttachmentURL = String.format("%s/$File/%s?OpenElement",
          this.getHTTPURL(crawlDoc), encodedAttachmentName);
      attachDoc.replaceItemValue(NCCONST.ITM_DOCID, AttachmentURL);

      // Only if we have a supported mime type do we send the content.
      if ((0 != MimeType.length()) ||
          (eo.getFileSize() > ncs.getMaxFileSize())) {
        attachDoc.replaceItemValue(NCCONST.ITM_MIMETYPE, MimeType);
        String attachmentPath = getAttachmentFilePath(crawlDoc,
            encodedAttachmentName);
        eo.extractFile(attachmentPath);
        attachDoc.replaceItemValue(NCCONST.ITM_CONTENTPATH, attachmentPath);
      } else {
        // Not a supported attachment so sending meta data only
        // with the filename as content
        attachDoc.replaceItemValue(NCCONST.ITM_CONTENT, AttachmentName);
        attachDoc.replaceItemValue(NCCONST.ITM_MIMETYPE,
            NCCONST.DEFAULT_MIMETYPE);
      }
      eo.recycle();

      // DO NOT MAP THESE FIELDS
      // attachDoc.replaceItemValue(NCCONST.ITM_DISPLAYURL, AttachmentURL);

      // Set the state of this document to be fetched
      attachDoc.replaceItemValue(NCCONST.ITM_ACTION, ActionType.ADD.toString());
      attachDoc.replaceItemValue(NCCONST.NCITM_STATE, NCCONST.STATEFETCHED);
      attachDoc.save();
      attachDoc.recycle();
      LOGGER.exiting(CLASS_NAME, METHOD);
      return true;
    } catch (Exception e) {
      LOGGER.logp(Level.SEVERE, CLASS_NAME, METHOD,
          "Error pre-fetching attachment: " + AttachmentName +
          " in document: " + srcDoc.getNotesURL(), e);
      if (null != eo) {
        eo.recycle();
      }
      if (null != attachDoc) {
        attachDoc.replaceItemValue(NCCONST.NCITM_STATE, NCCONST.STATEERROR);
        attachDoc.save();
        attachDoc.recycle();
      }
      return false;
    }
  }

  // This function will generate an unique file path for an attachment object.
  // Consider the situation where a document is updated twice and
  // appears in the submitq twice In this case, the first submit
  // will delete the doc.  The second submit will then send an
  // empty doc So we must use the UNID of the crawl request to
  // generate the unique filename
  public String getAttachmentFilePath(NotesDocument crawlDoc,
      String attachName) throws RepositoryException {
    String dirName = String.format("%s/attachments/%s/%s",
        ncs.getSpoolDir(),
        cdb.getReplicaID(),
        crawlDoc.getUniversalID());
    new java.io.File(dirName).mkdirs();
    String FilePath = String.format("%s/%s", dirName, attachName);
    //TODO:  Ensure that FilePath is a valid Windows filepath
    return FilePath;
  }

  public void connectQueue() throws RepositoryException {
    if (null == ns) {
      ns = ncs.createNotesSession();
    }
    if (null == cdb) {
      cdb = ns.getDatabase(ncs.getServer(), ncs.getDatabase());
    }
    if (crawlQueue == null) {
      crawlQueue = cdb.getView(NCCONST.VIEWCRAWLQ);
    }
  }


  /*
   * We accumulate objects as pre-fetch documents
   * De-allocate these in reverse order
   */
  public void disconnectQueue()  {
    final String METHOD = "disconnectQueue";
    LOGGER.entering(CLASS_NAME, METHOD);
    try {
      if (null != templateDoc) {
        templateDoc.recycle();
      }
      templateDoc = null;

      if (null != formDoc) {
        formDoc.recycle();
      }
      formDoc = null;

      if (null != formsdc) {
        formsdc.recycle();
      }
      formsdc = null;

      if (null != srcdb) {
        openDbRepId = "";
        srcdb.recycle();
        srcdb = null;
      }

      if (null != crawlQueue) {
        crawlQueue.recycle();
      }
      crawlQueue = null;

      if (null != cdb) {
        cdb.recycle();
      }
      cdb = null;

      if (null != ns) {
        ncs.closeNotesSession(ns);
      }
      ns = null;
    } catch (RepositoryException e) {
      LOGGER.log(Level.WARNING, CLASS_NAME, e);
    } finally {
      LOGGER.exiting(CLASS_NAME, METHOD);
    }
  }

  @Override
  public void run() {
    final String METHOD = "run";
    int exceptionCount = 0;
    LOGGER.entering(CLASS_NAME, METHOD);
    NotesPollerNotifier npn = ncs.getNotifier();
    while (nc.getShutdown() == false) {
      try {
        NotesDocument crawlDoc = null;
        // Only get from the queue if there is more than 300MB in the
        // spool directory
        // TODO: getFreeSpace is a Java 1.6 method.
        java.io.File spoolDir = new java.io.File(ncs.getSpoolDir());
        LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
            "Spool free space is " + spoolDir.getFreeSpace());
        if (spoolDir.getFreeSpace()/1000000 < 300) {
          LOGGER.logp(Level.WARNING, CLASS_NAME, METHOD,
              "Insufficient space in spool directory to process " +
              "new documents.  Need at least 300MB.");
          npn.waitForWork();
          LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
              "Crawler thread resuming after spool directory had " +
              "insufficient space.");
          continue;
        }
        LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
            "Connecting to crawl queue.");
        connectQueue();
        crawlDoc = getNextFromCrawlQueue(ns, crawlQueue);
        if (crawlDoc == null) {
          LOGGER.logp(Level.FINE, CLASS_NAME, METHOD, this.getName() +
              ": Crawl queue is empty.  Crawler thread sleeping.");
          // If we have finished processing the queue shutdown our connections
          disconnectQueue();
          npn.waitForWork();
          LOGGER.logp(Level.FINE, CLASS_NAME, METHOD, this.getName() +
              "Crawler thread resuming after crawl queue was empty.");
          continue;
        }
        if (prefetchDoc(crawlDoc)) {
          crawlDoc.replaceItemValue(NCCONST.NCITM_STATE, NCCONST.STATEFETCHED);
        } else  {
          crawlDoc.replaceItemValue(NCCONST.NCITM_STATE, NCCONST.STATEERROR);
        }
        crawlDoc.save(true);
        crawlDoc.recycle();
      } catch (Exception e) {
        LOGGER.log(Level.SEVERE, CLASS_NAME, e);
        // Lets say the server we are connected to goes down
        // while we are crawling We don't want to fill up the
        // logs with errors so go to sleep after 5 exceptions
        exceptionCount++;
        if (exceptionCount > 5) {
          LOGGER.logp(Level.WARNING, CLASS_NAME, METHOD,
              "Too many exceptions.  Crawler thread sleeping.");
          npn.waitForWork();
          LOGGER.logp(Level.WARNING, CLASS_NAME, METHOD,
              "Crawler thread resuming after too many exceptions " +
              "were encountered.");
        }
      }
    }
    disconnectQueue();
    LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
        "Connector shutdown - NotesCrawlerThread exiting.");
    LOGGER.exiting(CLASS_NAME, METHOD);
  }

  @VisibleForTesting
  static class MetaField {
    private static final Pattern formFieldMetaPattern =
        Pattern.compile("\\A(.+)===([^=]+)=([^=]+)\\z");
    private static final Pattern fieldMetaPattern =
        Pattern.compile("\\A([^=]+)=([^=]+)\\z");
    private static final Pattern fieldPattern =
        Pattern.compile("\\A([^=]+)\\z");

    private String formName;
    private String fieldName;
    private String metaName;

    MetaField(String configString) {
      String METHOD = "MetaField";
      if (configString == null) {
        return;
      }
      configString = configString.trim();
      if (configString.length() == 0) {
        return;
      }

      Matcher matcher = formFieldMetaPattern.matcher(configString);
      if (matcher.matches()) {
        formName = matcher.group(1);
        fieldName = matcher.group(2);
        metaName = matcher.group(3);
        return;
      }
      matcher = fieldMetaPattern.matcher(configString);
      if (matcher.matches()) {
        fieldName = matcher.group(1);
        metaName = matcher.group(2);
        return;
      }
      matcher = fieldPattern.matcher(configString);
      if (matcher.matches()) {
        fieldName = matcher.group(1);
        metaName = fieldName;
        return;
      }
      LOGGER.logp(Level.WARNING, CLASS_NAME, METHOD,
          "Unable to parse custom meta field definition; skipping: "
          + configString);
    }

    String getFormName() {
      return formName;
    }

    String getFieldName() {
      return fieldName;
    }

    String getMetaName() {
      return metaName;
    }

    public String toString() {
      return "[form: " + formName + "; field: " + fieldName
          + "; meta: " + metaName + "]";
    }
  }
}
