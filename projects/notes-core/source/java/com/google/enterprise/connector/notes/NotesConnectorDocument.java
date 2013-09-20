// Copyright 2011 Google Inc. All Rights Reserved.
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
import com.google.common.base.Strings;
import com.google.enterprise.connector.notes.client.NotesDatabase;
import com.google.enterprise.connector.notes.client.NotesDateTime;
import com.google.enterprise.connector.notes.client.NotesDocument;
import com.google.enterprise.connector.notes.client.NotesItem;
import com.google.enterprise.connector.notes.client.NotesSession;
import com.google.enterprise.connector.spi.Document;
import com.google.enterprise.connector.spi.Principal;
import com.google.enterprise.connector.spi.Property;
import com.google.enterprise.connector.spi.RepositoryException;
import com.google.enterprise.connector.spi.SecureDocument;
import com.google.enterprise.connector.spi.SimpleDocument;
import com.google.enterprise.connector.spi.SimpleProperty;
import com.google.enterprise.connector.spi.SpiConstants.AclAccess;
import com.google.enterprise.connector.spi.SpiConstants.AclInheritanceType;
import com.google.enterprise.connector.spi.SpiConstants.AclScope;
import com.google.enterprise.connector.spi.SpiConstants.ActionType;
import com.google.enterprise.connector.spi.SpiConstants.CaseSensitivityType;
import com.google.enterprise.connector.spi.SpiConstants.DocumentType;
import com.google.enterprise.connector.spi.SpiConstants.PrincipalType;
import com.google.enterprise.connector.spi.SpiConstants;
import com.google.enterprise.connector.spi.Value;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotesConnectorDocument implements Document {
  private static final String CLASS_NAME =
      NotesConnectorDocument.class.getName();
  private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

  @VisibleForTesting
  HashMap<String, List<Value>> docProps;

  private final NotesConnectorSession notesConnectorSession;
  private final NotesSession notesSession;
  private final NotesDatabase connectorDatabase;
  private String UNID = null;
  private FileInputStream fin = null;
  private String docid = null;
  private boolean isAttachment = false;

  @VisibleForTesting
  NotesDocument crawlDoc = null;

  NotesConnectorDocument(NotesConnectorSession notesConnectorSession,
      NotesSession notesSession, NotesDatabase connectorDatabase) {
    final String METHOD = "NotesConnectorDocument";
    LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
        "NotesConnectorDocument being created.");
    this.notesConnectorSession = notesConnectorSession;
    this.notesSession = notesSession;
    this.connectorDatabase = connectorDatabase;
  }

  public void closeInputStream() {
    try {
      if (fin != null) {
        fin.close();
      }
      fin = null;
    } catch (Exception e) {
      // Changed log level to WARNING.
      LOGGER.log(Level.WARNING, CLASS_NAME, e);
    }
  }

  public void setCrawlDoc(String unid, NotesDocument backenddoc) {
    final String METHOD = "setcrawlDoc";
    LOGGER.entering(CLASS_NAME, METHOD);
    crawlDoc = backenddoc;
    UNID = unid;
    try {
      String action = crawlDoc.getItemValueString(NCCONST.ITM_ACTION);
      if (action.equalsIgnoreCase(ActionType.ADD.toString())){
        if (crawlDoc.hasItem(NCCONST.NCITM_DBACL)) {
          addDatabaseAcl();
        } else {
          addDocument();
        }
      } else if (action.equalsIgnoreCase(ActionType.DELETE.toString())) {
        deleteDocument();
      }
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, CLASS_NAME, e);
    } finally {
      crawlDoc = null;
      LOGGER.exiting(CLASS_NAME, METHOD);
    }
  }

  public void deleteDocument() {
    final String METHOD = "deleteDocument";
    LOGGER.entering(CLASS_NAME, METHOD);

    try {
      docProps = new HashMap<String, List<Value>>();
      docid = crawlDoc.getItemValueString(NCCONST.ITM_DOCID);
      LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
          "Delete Request document properties for " + docid);
      putTextItem(SpiConstants.PROPNAME_DOCID, NCCONST.ITM_DOCID, null);
      putTextItem(SpiConstants.PROPNAME_ACTION, NCCONST.ITM_ACTION, null);
    } catch (Exception e) {
      LOGGER.log(Level.SEVERE, CLASS_NAME, e);
    } finally {
      LOGGER.exiting(CLASS_NAME, METHOD);
    }
  }

  public void addDocument() {
    final String METHOD = "addDocument";
    LOGGER.entering(CLASS_NAME, METHOD);
    try {
      docProps = new HashMap<String, List<Value>>();
      docid = crawlDoc.getItemValueString(NCCONST.ITM_DOCID);
      LOGGER.logp(Level.FINER, CLASS_NAME, METHOD,
          "Loading document properties for " + docid);
      isAttachment = docid.contains("/$File/");

      // Load the Connector Manager SPI Properties first
      docProps.put(SpiConstants.PROPNAME_DOCUMENTTYPE,
          asList(Value.getStringValue(DocumentType.RECORD.toString())));
      putTextItem(SpiConstants.PROPNAME_DOCID, NCCONST.ITM_DOCID, null);
      putTextItem(SpiConstants.PROPNAME_TITLE, NCCONST.ITM_TITLE, null);
      setDateProperties();
      setContentProperty();

      // PROPNAME_SEARCHURL -> DO NOT MAP THIS - it causes the
      //     gsa to try and crawl the doc
      putTextItem(SpiConstants.PROPNAME_MIMETYPE, NCCONST.ITM_MIMETYPE, null);
      putTextItem(SpiConstants.PROPNAME_DISPLAYURL,
          NCCONST.ITM_DISPLAYURL,null);
      putBooleanItem(SpiConstants.PROPNAME_ISPUBLIC, NCCONST.ITM_ISPUBLIC, null);
      putTextItem(SpiConstants.PROPNAME_ACTION, NCCONST.ITM_ACTION, null);
      // TODO: FIX THIS UPGRADE TO NEW SPI
      //putBooleanItem("google:lock", NCCONST.ITM_LOCK, "true");
      putBooleanItem(SpiConstants.PROPNAME_LOCK, NCCONST.ITM_LOCK, "true");

      putTextItem(NCCONST.PROPNAME_DESCRIPTION,
          NCCONST.ITM_GMETADESCRIPTION, null);
      putTextItem(NCCONST.PROPNAME_NCDATABASE, NCCONST.ITM_GMETADATABASE, null);
      putTextListItem(NCCONST.PROPNAME_NCCATEGORIES,
          NCCONST.ITM_GMETACATEGORIES, null);
      putTextListItem(NCCONST.PROPNAME_NCREPLICASERVERS,
          NCCONST.ITM_GMETAREPLICASERVERS, null);
      putTextItem(NCCONST.PROPNAME_NCNOTESLINK,
          NCCONST.ITM_GMETANOTESLINK, null);
      putTextListItem(NCCONST.PROPNAME_NCATTACHMENTS,
          NCCONST.ITM_GMETAATTACHMENTS, null);
      putTextListItem(NCCONST.PROPNAME_NCATTACHMENTFILENAME,
          NCCONST.ITM_GMETAATTACHMENTFILENAME, null);
      putTextListItem(NCCONST.PROPNAME_NCALLATTACHMENTS,
          NCCONST.ITM_GMETAALLATTACHMENTS, null);
      putTextItem(NCCONST.PROPNAME_NCAUTHORS, NCCONST.ITM_GMETAWRITERNAME,
          null);
      putTextItem(NCCONST.PROPNAME_NCFORM, NCCONST.ITM_GMETAFORM, null);
      setCustomProperties();
      setMetaFields();

      // If using ACLs for the database, and the GSA supports
      // inherited ACLs, construct the ACL. (GSA 7.0+)
      if (crawlDoc.getItemValueString(NCCONST.NCITM_AUTHTYPE)
          .equals(NCCONST.AUTH_ACL)
          && ((NotesTraversalManager) notesConnectorSession
              .getTraversalManager()).getTraversalContext()
          .supportsInheritedAcls()) {

        LOGGER.logp(Level.FINE, CLASS_NAME, METHOD,
            "Creating GSA ACL for document: " + docid);
        String replicaUrl = new NotesDocId(docid).getReplicaUrl();
        Vector readers =
            crawlDoc.getItemValue(NCCONST.NCITM_DOCAUTHORREADERS);
        if (readers.size() > 0) {
          createSecureDocumentWithReaders(replicaUrl, readers);
        } else {
          createSecureDocumentWithoutReaders(replicaUrl);
        }
      }
    } catch (Exception e) {
      // TODO: Handle errors correctly so that we remove the
      // document from the queue if it is corrupt.
      LOGGER.log(Level.SEVERE, CLASS_NAME, e);
    } finally {
      LOGGER.exiting(CLASS_NAME, METHOD);
    }
  }

  private void createSecureDocumentWithoutReaders(String replicaUrl)
      throws RepositoryException {
    final String METHOD = "createSecureDocumentWithoutReaders";

    docProps.put(SpiConstants.PROPNAME_ACLINHERITFROM_DOCID,
        asList(Value.getStringValue(
        replicaUrl + "/" + NCCONST.DB_ACL_INHERIT_TYPE_PARENTOVERRIDES)));
    if (LOGGER.isLoggable(Level.FINEST)) {
      LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
          "inherit from: " + replicaUrl
          + "/" + NCCONST.DB_ACL_INHERIT_TYPE_PARENTOVERRIDES);
    }
  }

  private void createSecureDocumentWithReaders(String replicaUrl,
      Vector readers) throws RepositoryException {
    final String METHOD = "createSecureDocumentWithReaders";

    docProps.put(SpiConstants.PROPNAME_ACLINHERITFROM_DOCID,
        asList(Value.getStringValue(
        replicaUrl + "/" + NCCONST.DB_ACL_INHERIT_TYPE_ANDBOTH)));
    // mapNotesNamesToGsaNames modifies the readers Vector, so
    // create a copy here.
    @SuppressWarnings("unchecked")
        Vector localReaders = new Vector(readers);
    Collection<String> gsaReaders = notesConnectorSession
        .getUserGroupManager().mapNotesNamesToGsaNames(
            notesSession, localReaders, true);
    // Add the replica id prefix to each role in the readers list.
    ArrayList<String> modifiedReaders = new ArrayList<String>();
    for (int i = 0; i < localReaders.size(); i++) {
      String reader = localReaders.elementAt(i).toString().trim();
      // The only way to tell if an entry is a role, as far
      // as I can tell, is to look for [] around the name.
      if (reader.startsWith("[") && reader.endsWith("]")) {
        reader = crawlDoc.getItemValueString(NCCONST.NCITM_REPLICAID)
            + "/" + reader;
      }
      modifiedReaders.add(reader);
    }
    Collection<String> gsaGroups = GsaUtil.getGsaGroups(
        modifiedReaders, notesConnectorSession.getGsaGroupPrefix());
    String userNamespace;
    PrincipalType principalType;
    if (notesConnectorSession.getConnector().getGsaNamesAreGlobal()) {
      userNamespace =
          notesConnectorSession.getConnector().getGlobalNamespace();
      principalType = PrincipalType.UNKNOWN;
    } else {
      userNamespace =
          notesConnectorSession.getConnector().getLocalNamespace();
      principalType = PrincipalType.UNQUALIFIED;
    }
    ArrayList<Value> values = new ArrayList<Value>();
    for (String reader : gsaReaders) {
      Principal principal = new Principal(principalType, userNamespace,
          reader, CaseSensitivityType.EVERYTHING_CASE_INSENSITIVE);
      values.add(Value.getPrincipalValue(principal));
    }
    docProps.put(SpiConstants.PROPNAME_ACLUSERS, values);

    values = new ArrayList<Value>();
    for (String group : gsaGroups) {
      Principal principal = new Principal(PrincipalType.UNQUALIFIED,
          notesConnectorSession.getConnector().getLocalNamespace(),
          group, CaseSensitivityType.EVERYTHING_CASE_INSENSITIVE);
      values.add(Value.getPrincipalValue(principal));
    }
    docProps.put(SpiConstants.PROPNAME_ACLGROUPS, values);

    if (LOGGER.isLoggable(Level.FINEST)) {
      LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
          "inherit from: " + replicaUrl
          + "/" + NCCONST.DB_ACL_INHERIT_TYPE_ANDBOTH);
      LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
          "readers:users: " + gsaReaders);
      LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
          "Using namespace for user names: " + userNamespace);
      LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
          "readers:groups: " + gsaGroups);
    }
  }

  public void addDatabaseAcl() {
    final String METHOD = "addDatabaseAcl";
    LOGGER.entering(CLASS_NAME, METHOD);
    try {
      docProps = new HashMap<String, List<Value>>();
      docProps.put(SpiConstants.PROPNAME_DOCUMENTTYPE,
          asList(Value.getStringValue(DocumentType.ACL.toString())));
      putTextItem(SpiConstants.PROPNAME_ACTION, NCCONST.ITM_ACTION, null);
      docProps.put(SpiConstants.PROPNAME_DOCID,
          asList(Value.getStringValue(
          crawlDoc.getItemValueString(NCCONST.ITM_DOCID))));
      String inheritType = crawlDoc.getItemValueString(
          NCCONST.NCITM_DBACLINHERITTYPE);
      if (inheritType.equals(NCCONST.DB_ACL_INHERIT_TYPE_ANDBOTH)) {
        docProps.put(SpiConstants.PROPNAME_ACLINHERITANCETYPE,
            asList(Value.getStringValue(
            AclInheritanceType.AND_BOTH_PERMIT.toString())));
      } else if (inheritType.equals(
              NCCONST.DB_ACL_INHERIT_TYPE_PARENTOVERRIDES)) {
        docProps.put(SpiConstants.PROPNAME_ACLINHERITANCETYPE,
            asList(Value.getStringValue(
            AclInheritanceType.PARENT_OVERRIDES.toString())));
      } else {
        // Since we create the crawl docs, this would be a
        // development-time error rather than a possible run-time
        // error.
        LOGGER.logp(Level.WARNING, CLASS_NAME, METHOD,
            "Unexpected inheritance type: " + inheritType);
      }
      String userNamespace;
      PrincipalType principalType;
      if (notesConnectorSession.getConnector().getGsaNamesAreGlobal()) {
        userNamespace =
            notesConnectorSession.getConnector().getGlobalNamespace();
        principalType = PrincipalType.UNKNOWN;
      } else {
        userNamespace =
            notesConnectorSession.getConnector().getLocalNamespace();
        principalType = PrincipalType.UNQUALIFIED;
      }
      LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
          "Using namespace for user names: " + userNamespace);

      putAclPrincipals(principalType, NCCONST.NCITM_DBPERMITUSERS,
          SpiConstants.PROPNAME_ACLUSERS, userNamespace);
      putAclPrincipals(principalType, NCCONST.NCITM_DBNOACCESSUSERS,
          SpiConstants.PROPNAME_ACLDENYUSERS, userNamespace);
      putAclPrincipals(PrincipalType.UNQUALIFIED, NCCONST.NCITM_DBPERMITGROUPS,
          SpiConstants.PROPNAME_ACLGROUPS,
          notesConnectorSession.getConnector().getLocalNamespace());
      putAclPrincipals(PrincipalType.UNQUALIFIED,
          NCCONST.NCITM_DBNOACCESSGROUPS, SpiConstants.PROPNAME_ACLDENYGROUPS,
          notesConnectorSession.getConnector().getLocalNamespace());
    } catch (Exception e) {
      // TODO: Handle errors correctly so that we remove the
      // document from the queue if it is corrupt.
      LOGGER.log(Level.SEVERE, CLASS_NAME, e);
    } finally {
      LOGGER.exiting(CLASS_NAME, METHOD);
    }
  }

  protected void setCustomProperties() {
    // TODO: Set Custom properties
  }

  @VisibleForTesting
  void setMetaFields() throws RepositoryException {
    Vector items = crawlDoc.getItems();
    for (Object i : items) {
      NotesItem item = (NotesItem) i;
      String name = item.getName();
      if (!name.startsWith(NotesCrawlerThread.META_FIELDS_PREFIX)) {
        continue;
      }
      if (LOGGER.isLoggable(Level.FINEST)) {
        LOGGER.finest("found a custom property to map to a meta field: "
            + name);
      }
      putItemValues(
          name.substring(NotesCrawlerThread.META_FIELDS_PREFIX.length()), name);
    }
  }

  protected void setContentProperty()
      throws RepositoryException, FileNotFoundException {
    if (isAttachment) {
      String filePath = crawlDoc.getItemValueString(NCCONST.ITM_CONTENTPATH);
      // For unsupported attachments, we don't send content so
      // content path is empty
      if (0 != filePath.length()) {
        FileInputStream fin = new FileInputStream(filePath);
        docProps.put(SpiConstants.PROPNAME_CONTENT,
            asList(Value.getBinaryValue(fin)));
      } else {
        // The filename should be inthe content
        putTextItem(SpiConstants.PROPNAME_CONTENT, NCCONST.ITM_CONTENT, "");
      }
      //fin.close();
    } else {
      putTextItem(SpiConstants.PROPNAME_CONTENT,
          NCCONST.ITM_CONTENT, "Document content");
    }
  }

  protected void setDateProperties() throws RepositoryException {
    final String METHOD = "setDateProperties";

    NotesDateTime dt = (NotesDateTime) crawlDoc
        .getItemValueDateTimeArray(NCCONST.ITM_GMETALASTUPDATE).elementAt(0);
    Calendar tmpCal = Calendar.getInstance();
    tmpCal.setTime(dt.toJavaDate());
    docProps.put(SpiConstants.PROPNAME_LASTMODIFIED,
        asList(Value.getDateValue(tmpCal)));
    LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
        "Last update is " + tmpCal.toString());

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss' 'Z");
    String nclastupdate = sdf.format(dt.toJavaDate());
    docProps.put(NCCONST.PROPNAME_NCLASTUPDATE,
        asList(Value.getStringValue(nclastupdate)));
    dt.recycle();

    NotesDateTime createdate = (NotesDateTime) crawlDoc
        .getItemValueDateTimeArray(NCCONST.ITM_GMETACREATEDATE).elementAt(0);
    String nccreatedate = sdf.format(createdate.toJavaDate());
    docProps.put(NCCONST.PROPNAME_CREATEDATE,
        asList(Value.getStringValue(nccreatedate)));
    createdate.recycle();
  }

  protected void putTextListItem(String PropName, String ItemName,
      String defaultText) throws RepositoryException {
    final String METHOD = "putTextItem";
    Vector<?> vText = crawlDoc.getItemValue(ItemName);
    if (0 == vText.size()) {
      LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
          "Using default value document. " + PropName + " in " + docid);
      if (defaultText != null) {
        docProps.put(PropName, asList(Value.getStringValue(defaultText)));
      }
      return;
    }
    List<Value> list = new LinkedList<Value>();
    for (int i= 0; i < vText.size(); i++) {
      String ItemListElementText = vText.elementAt(i).toString();
      if (ItemListElementText != null) {
        if (0 != ItemListElementText.length()) {
          list.add(Value.getStringValue(vText.elementAt(i).toString()));;
        }
      }
    }
    LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
        "Adding property " + PropName + " ::: " + list);
    docProps.put(PropName, list);
  }

  // This method puts the text of an itme into a meta field.
  // Items with multiple values are separated by semicolons
  protected void putTextItem(String PropName, String ItemName,
      String defaultText) throws RepositoryException {
    final String METHOD = "putTextItem";
    String text = null;
    NotesItem itm = crawlDoc.getFirstItem(ItemName);

    // Does the item exist?
    if (itm == null) {
      if (defaultText != null) {
        docProps.put(PropName, asList(Value.getStringValue(defaultText)));
      }
      return;
    }

    // Get the text of the item
    text = itm.getText(1024 * 1024 * 2);  // Maximum of 2mb of text
    if (Strings.isNullOrEmpty(text)) { // Does this field exist?
      LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
          "Using default value document. " + PropName + " in " + docid);
      if (defaultText != null) {
        text = defaultText;
      }
      else {
        return;
      }
    }
    LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
        "Adding property " + PropName);
    docProps.put(PropName, asList(Value.getStringValue(text)));
  }

  protected void putBooleanItem(String PropName, String ItemName,
      String defaultText) throws RepositoryException {
    final String METHOD = "putTextItem";
    String text = crawlDoc.getItemValueString(ItemName);
    if (Strings.isNullOrEmpty(text)) { // Does this field exist?
      // At this point there is nothing we can do except log an error
      LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
          "Using default value document. " + PropName + " in " + docid);
      text = defaultText;
    }
    docProps.put(PropName, asList(Value.getBooleanValue(text)));
  }

  /**
   * Copies the values from an Item to the document properties as Values.
   *
   * @param propName the document property name
   * @param itemName the crawl doc item name
   * @throws RepositoryException
   */
  private void putItemValues(String propName, String itemName)
      throws RepositoryException {
    final String METHOD = "putItemValues";
    Vector<?> values = crawlDoc.getItemValue(itemName);
    if (0 == values.size()) {
      return;
    }
    List<Value> list = new LinkedList<Value>();
    for (int i = 0; i < values.size(); i++) {
      Object value = values.get(i);
      if (value == null) {
        continue;
      }
      if (value instanceof String) {
        String v = (String) value;
        if (v.length() == 0) {
          continue;
        }
        list.add(Value.getStringValue(v));
      } else if (value instanceof Double) {
        list.add(Value.getDoubleValue((Double) value));
      } else if (value instanceof NotesDateTime) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(((NotesDateTime) value).toJavaDate());
        list.add(Value.getDateValue(cal));
      } else {
        if (LOGGER.isLoggable(Level.FINEST)) {
          LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
              "Unexpected item value type: " + value.getClass().getName());
        }
      }
    }
    if (list.size() > 0) {
      if (LOGGER.isLoggable(Level.FINEST)) {
        LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
            "Adding property " + propName + " ::: " + list);
      }
      docProps.put(propName, list);
    }
  }

  private void putAclPrincipals(PrincipalType principalType,
      String principalItemName, String propertyName, String namespace)
      throws RepositoryException {
    final String METHOD = "putAclPrincipals";
    Vector values = crawlDoc.getItemValue(principalItemName);
    if (values.size() == 0) {
      return;
    }
    ArrayList<Value> docValues = new ArrayList<Value>(values.size());
    for (Object principal : values) {
      Principal docPrincipal = new Principal(principalType, namespace,
          principal.toString(),
          CaseSensitivityType.EVERYTHING_CASE_INSENSITIVE);
      docValues.add(Value.getPrincipalValue(docPrincipal));
    }
    docProps.put(propertyName, docValues);
  }

  public String getUNID() {
    return UNID;
  }

  /* @Override */
  public Property findProperty(String name) throws RepositoryException {
    // Maintain the ability to check docProps directly for testing.
    List<Value> list = docProps.get(name);
    Property prop = null;
    if (list != null) {
      prop = new SimpleProperty(list);
    }
    return prop;
  }

  /* @Override */
  public Set<String> getPropertyNames() throws RepositoryException {
    return docProps.keySet();
  }

  private List<Value> asList(Value value) {
    List<Value> list = new LinkedList<Value>();
    list.add(value);
    return list;
  }
}
