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

import com.google.enterprise.connector.spi.Document;
import com.google.enterprise.connector.spi.Property;
import com.google.enterprise.connector.spi.RepositoryException;
import com.google.enterprise.connector.spi.SimpleProperty;
import com.google.enterprise.connector.spi.SpiConstants.ActionType;
import com.google.enterprise.connector.spi.SpiConstants;
import com.google.enterprise.connector.spi.Value;
import lotus.domino.DateTime;
import lotus.domino.Item;
import lotus.domino.NotesException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
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
  private HashMap<String, List<Value>> docProps;
  private String UNID = null;
  FileInputStream fin = null;
  String docid = null;
  lotus.domino.Document crawlDoc = null;

  NotesConnectorDocument() {
    final String METHOD = "NotesConnectorDocument";
    LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
        "NotesConnectorDocument being created.");
  }

  public void closeInputStream() {
    try {
      if (null != fin) {
        fin.close();
      }
      fin = null;
    } catch (Exception e) {
      // Changed log level to WARNING. 
      LOGGER.log(Level.WARNING, CLASS_NAME, e);
    }
  }
        
  public void setCrawlDoc(String unid, lotus.domino.Document backenddoc) {
    final String METHOD = "setcrawlDoc";
    LOGGER.entering(CLASS_NAME, METHOD);
    crawlDoc = backenddoc;
    UNID = unid;
    try {
      if (crawlDoc.getItemValueString(NCCONST.ITM_ACTION)
          .equalsIgnoreCase(ActionType.ADD.toString())){
        addDocument();
      }
      if (crawlDoc.getItemValueString(NCCONST.ITM_ACTION)
          .equalsIgnoreCase(ActionType.DELETE.toString())) {
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
        
      // Load the Connector Manager SPI Properties first
      putTextItem(SpiConstants.PROPNAME_DOCID, NCCONST.ITM_DOCID, null);
      putTextItem(SpiConstants.PROPNAME_TITLE, NCCONST.ITM_TITLE, null);
      setDateProperties();
      setContentProperty();
        
      // PROPNAME_CONTENTURL
      // PROPNAME_FEEDTYPE
      // PROPNAME_FEEDID
      // PROPNAME_SEARCHURL -> DO NOT MAP THIS - it causes the
      //     gsa to try and crawl the doc
      // PROPNAME_SECURITYTOKEN
      putTextItem(SpiConstants.PROPNAME_MIMETYPE, NCCONST.ITM_MIMETYPE, null);
      putTextItem(SpiConstants.PROPNAME_DISPLAYURL, NCCONST.ITM_DOCID, null);
      putBooleanItem(SpiConstants.PROPNAME_ISPUBLIC, NCCONST.ITM_ISPUBLIC, null);
      // PROPNAME_ACLGROUPS
      // PROPNAME_ACLUSERS
      // PROPNAME_GROUP_ROLES_PROPNAME_PREFIX
      // PROPNAME_USER_ROLES_PROPNAME_PREFIX
      putTextItem(SpiConstants.PROPNAME_ACTION, NCCONST.ITM_ACTION, null);
      // PROPNAME_FOLDER
      // TODO: FIX THIS UPGRADE TO NEW SPI
      //putBooleanItem("google:lock", NCCONST.ITM_LOCK, "true");
      putBooleanItem(SpiConstants.PROPNAME_LOCK, NCCONST.ITM_LOCK, "true");
      
      // PROPNAME_PAGERANK                    
      // PERSISTABLE_ATTRIBUTES
      // PROPNAME_MANAGER_SHOULD_PERSIST
      // PROPNAME_CONNECTOR_INSTANCE - Reserved for CM
      // PROPNAME_CONNECTOR_TYPE
      // PROPNAME_PRIMARY_FOLDER
      // PROPNAME_TIMESTAMP
      // PROPNAME_MESSAGE
      // PROPNAME_SNAPSHOT
      // PROPNAME_CONTAINER
      // PROPNAME_PERSISTED_CUSTOMDATA_1
      // PROPNAME_PERSISTED_CUSTOMDATA_2
      
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
      putTextListItem(NCCONST.PROPNAME_NCALLATTACHMENTS,
          NCCONST.ITM_GMETAALLATTACHMENTS, null);
      putTextItem(NCCONST.PROPNAME_NCAUTHORS, NCCONST.ITM_GMETAWRITERNAME, null);
      putTextItem(NCCONST.PROPNAME_NCFORM, NCCONST.ITM_GMETAFORM, null);
      setCustomProperties();
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

  protected void setContentProperty() 
      throws NotesException, FileNotFoundException {
    boolean isAttachment = docid.contains("/$File/");
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

  protected void setDateProperties() throws NotesException {
    final String METHOD = "setDateProperties";
    
    DateTime dt = (DateTime) crawlDoc
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
                
    DateTime createdate = (DateTime) crawlDoc
        .getItemValueDateTimeArray(NCCONST.ITM_GMETACREATEDATE).elementAt(0);
    String nccreatedate = sdf.format(createdate.toJavaDate());
    docProps.put(NCCONST.PROPNAME_CREATEDATE, 
        asList(Value.getStringValue(nccreatedate)));
    createdate.recycle();
  }
        
  protected void putTextListItem(String PropName, String ItemName,
      String DefaultText) throws NotesException {
    final String METHOD = "putTextItem";
    Vector<?> vText = crawlDoc.getItemValue(ItemName);
    if (0 == vText.size()) {
      LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
          "Using default value document. " + PropName + " in " + docid);
      if (null != DefaultText) {
        docProps.put(PropName, asList(Value.getStringValue(DefaultText)));
      }
      return;
    }
    List<Value> list = new LinkedList<Value>();
    for (int i= 0; i < vText.size(); i++) {
      String ItemListElementText = vText.elementAt(i).toString();
      if (null != ItemListElementText) {
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
      String DefaultText) throws NotesException {
    final String METHOD = "putTextItem";
    String text = null;
    Item itm = crawlDoc.getFirstItem(ItemName);
                
    // Does the item exist?
    if (null == itm) {
      if (null != DefaultText) {
        docProps.put(PropName, asList(Value.getStringValue(DefaultText)));
      }
      return;
    }
                
    // Get the text of the item
    text = itm.getText(1024 * 1024 * 2);  // Maximum of 2mb of text
    if ((null == text) || (0 == text.length())) { // Does this field exist?
      LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD, 
          "Using default value document. " + PropName + " in " + docid);
      if (null != DefaultText) {
        text = DefaultText;
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
      String DefaultText) throws NotesException {
    final String METHOD = "putTextItem";
    String text = crawlDoc.getItemValueString(ItemName);
    if ((null == text) || (0 == text.length())) { // Does this field exist?
      // At this point there is nothing we can do except log an error
      LOGGER.logp(Level.FINEST, CLASS_NAME, METHOD,
          "Using default value document. " + PropName + " in " + docid);
      text = DefaultText;
    }
    docProps.put(PropName, asList(Value.getBooleanValue(text)));
  }

  public String getUNID() {
    return UNID;
  }

  /* @Override */
  public Property findProperty(String name) throws RepositoryException {
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
