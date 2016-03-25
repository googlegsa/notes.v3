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

import com.google.enterprise.connector.spi.ConfigureResponse;
import com.google.enterprise.connector.spi.ConnectorFactory;
import com.google.enterprise.connector.spi.ConnectorType;
import com.google.enterprise.connector.spi.XmlUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;

public class NotesConnectorType implements ConnectorType {
  private static final String CLASS_NAME = NotesConnectorType.class.getName();
  private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);
  private static final String VALUE = "value";
  private static final String NAME = "name";
  private static final String TEXT = "text";
  private static final String TYPE = "type";
  private static final String PASSWORD = "password";

  private List<String> keys = null;
  private Set<String> keySet = null;

  /**
   * Set the keys that are required for configuration. One of the overloadings
   * of this method must be called exactly once before the SPI methods are used.
   *
   * @param keys A list of String keys
   */
  public void setConfigKeys(List<String> keys) {
    if (this.keys != null) {
      throw new IllegalStateException();
    }
    this.keys = keys;
    this.keySet = new HashSet<String>(keys);
  }

  /**
   * Set the keys that are required for configuration. One of the overloadings
   * of this method must be called exactly once before the SPI methods are used.
   *
   * @param keys An array of String keys
   */
  public void setConfigKeys(String[] keys) {
    setConfigKeys(Arrays.asList(keys));
  }

  /**
   * Validates whether a string is an acceptable value for a specific key.
   *
   * @param key
   * @param val
   * @return true if the val is acceptable for this key
   */
  private boolean validateConfigPair(String key, String val) {
    if (val == null || val.length() == 0) {
      // Empty passwords are allowed. GSA configuration is optional.
      if (key.equals("idPassword")
          || key.equals("gsaUsername")
          || key.equals("gsaPassword")) {
        return true;
      }
      return false;
    }
    return true;
  }

  /**
   * Validates all values in the map.
   *
   * @param configData the data
   * @return true if all keys have valid values
   */
  private boolean validateConfigMap(Map<String, String> configData) {
    for (String key : keys) {
      String val = configData.get(key);
      if (!validateConfigPair(key, val)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns the value for the key from the ResourceBundle, or
   * the key if no resource is found.
   *
   * @param labels the resources
   * @param key the key
   * @return the resource, or the key
   */
  private String getResource(ResourceBundle labels, String key) {
    try {
      return labels.getString(key);
    } catch (MissingResourceException e) {
      LOGGER.config("Missing resource bundle key: " + key);
      return key;
    }
  }

  /**
   * Adds an HTML attribute to the buffer.
   *
   * @param buf the buffer
   * @param attrName the attribute name
   * @param attrValue the attribute value
   */
  private void appendAttribute(StringBuilder buf, String attrName,
      String attrValue) {
    try {
      XmlUtils.xmlAppendAttr(attrName, attrValue, buf);
    } catch (IOException e) {
      // Can't happen with StringBuilder.
      throw new AssertionError(e);
    }
  }

  /**
   * Adds a row for an HTML text input element.
   *
   * @param buf the buffer
   * @param values the data; should not be null
   * @param labels the row label resources
   * @param shouldValidate true if the value for the given key
   *     should be validated
   * @param name the input element name
   * @param type the input element type (text or password)
   * @param isRequired true if the field is required
   */
  /* See Livelink Connector for source for styles used here */
  private void appendTextInputRow(StringBuilder buf,
      Map<String, String> configMap, ResourceBundle labels,
      boolean shouldValidate, String name, String type, boolean isRequired) {

    buf.append("<tr valign='top'>")
        .append("<td style='white-space:nowrap'>")
        .append("<div style='float:left;'>");
    String value = configMap.get(name);
    boolean isValid = !shouldValidate || validateConfigPair(name, value);
    if (!isValid) {
      buf.append("<font color='red'>");
    }
    buf.append(getResource(labels, name));
    if (!isValid) {
      buf.append("</font>");
    }
    buf.append("</div>");
    if (isRequired) {
      buf.append("<div style='text-align: right; ").
          append("color: red; font-weight: bold; ").
          append("margin-right: 0.3em;\'>*</div>");
    }
    buf.append("</td><td><input");
    appendAttribute(buf, TYPE, type);
    if (null != value) {
      appendAttribute(buf, VALUE, value);
    }
    appendAttribute(buf, NAME, name);
    buf.append("/></td></tr>\r\n");
  }

  private void appendYesNoRow(StringBuilder buf,
      Map<String, String> configMap, ResourceBundle labels,
      String name, boolean isRequired, boolean defaultValue) {

    buf.append("<tr valign='top'>")
        .append("<td style='white-space:nowrap'>")
        .append("<div style='float:left;'>");
    String configValue = configMap.get(name);
    boolean value;
    if (configValue == null) {
      value = defaultValue;
    } else {
      value = Boolean.parseBoolean(configValue);
    }
    buf.append(getResource(labels, name));
    buf.append("</div>");
    if (isRequired) {
      buf.append("<div style='text-align: right; ").
          append("color: red; font-weight: bold; ").
          append("margin-right: 0.3em;\'>*</div>");
    }
    buf.append("</td><td>");
    buf.append("<input");
    appendAttribute(buf, TYPE, "radio");
    appendAttribute(buf, VALUE, Boolean.TRUE.toString());
    appendAttribute(buf, NAME, name);
    if (value) {
      appendAttribute(buf, "checked", "checked");
    }
    buf.append("/>");
    buf.append(getResource(labels, "yes"));
    buf.append("&nbsp;&nbsp;<input");
    appendAttribute(buf, TYPE, "radio");
    appendAttribute(buf, VALUE, Boolean.FALSE.toString());
    appendAttribute(buf, NAME, name);
    if (!value) {
      appendAttribute(buf, "checked", "checked");
    }
    buf.append("/>");
    buf.append(getResource(labels, "no"));
    buf.append("</td></tr>\r\n");
  }

  /**
   * Adds a row for a configuration section label.
   *
   * @param buf the buffer
   * @param label the label
   */
  private void appendSectionLabel(StringBuilder buf, String label,
      String help) {
    buf.append("<tr valign='top'>")
        .append("<td style='padding-top:3ex; white-space:nowrap'")
        .append(" colspan='2'><b>");
    buf.append(label);
    buf.append("</b></td></tr>\r\n");
    if (null != help) {
      buf.append("<tr valign='top'>")
          .append("<td colspan='2'>")
          .append(help)
          .append("</td></tr>\r\n");
    }
  }

  /**
   * Creates the configuration form.
   *
   * @param configMap the form data; may be null
   * @param labels the resources
   * @param shouldValidate if true, the data will be validated
   *     and invalid properties flagged
   */
  private String makeConfigForm(Map<String, String> configMap,
      ResourceBundle labels, boolean shouldValidate) {
    StringBuilder buf = new StringBuilder(2048);
    if (configMap == null) {
      configMap = Collections.emptyMap();
    }
    appendTextInputRow(buf, configMap, labels, shouldValidate,
        "idPassword", PASSWORD, false);
    appendTextInputRow(buf, configMap, labels, shouldValidate,
        "database", TEXT, true);
    appendTextInputRow(buf, configMap, labels, shouldValidate,
        "server", TEXT, true);

    appendYesNoRow(buf, configMap, labels, "gsaNamesAreGlobal", true, true);

    appendSectionLabel(buf, getResource(labels, "gsaPropertiesSection"),
        getResource(labels, "gsaPropertiesSectionHelp"));

    appendTextInputRow(buf, configMap, labels, shouldValidate,
        "gsaUsername", TEXT, false);
    appendTextInputRow(buf, configMap, labels, shouldValidate,
        "gsaPassword", PASSWORD, false);

    // Toss in all the stuff that's in the map but isn't in the keyset
    // taking care to list them in alphabetic order (this is mainly for
    // testability).
    for (String key : new TreeSet<String>(configMap.keySet())) {
      if (!keySet.contains(key)) {
        // add another hidden field to preserve this data
        String val = configMap.get(key);
        buf.append("<input type=\"hidden\" value=\"");
        buf.append(val);
        buf.append("\" name=\"");
        buf.append(key);
        buf.append("\"/>\r\n");
      }
    }
    return buf.toString();
  }

  @Override
  public ConfigureResponse getConfigForm(Locale locale) {
    try {
      ConfigureResponse result = new ConfigureResponse("",
          makeConfigForm(null, getResources(locale), false));
      LOGGER.config("getConfigForm form:\n" + result.getFormSnippet());
      return result;
    } catch (Throwable t) {
      return getErrorResponse(getExceptionMessages(null, t));
    }
  }

  @Override
  public ConfigureResponse validateConfig(Map<String, String> configData,
      Locale locale, ConnectorFactory connectorFactory) {
    try {
      if (validateConfigMap(configData)) {
        // TODO: We could let the connector do its own validation
        // using connectorFactory.makeConnector(configData).

        // all is ok
        return null;
      }
      ResourceBundle messages = getResources(locale);
      ConfigureResponse result = new ConfigureResponse(
          getResource(messages, "missingRequiredInformation"),
          makeConfigForm(configData, messages, true));
      LOGGER.config("validateConfig new form:\n" + result.getFormSnippet());
      return result;
    } catch (Throwable t) {
      return getErrorResponse(getExceptionMessages(null, t));
    }
  }

  @Override
  public ConfigureResponse getPopulatedConfigForm(
      Map<String, String> configMap, Locale locale) {
    try {
      return new ConfigureResponse("",
          makeConfigForm(configMap, getResources(locale), false));
    } catch (Throwable t) {
      return getErrorResponse(getExceptionMessages(null, t));
    }
  }

  /**
   * Localizes the resource bundle name.
   *
   * @param locale the locale to look up
   * @return the ResourceBundle
   * @throws MissingResourceException if the bundle can't be found
   */
  /* See Livelink Connector */
  private ResourceBundle getResources(Locale locale)
      throws MissingResourceException {
    return ResourceBundle.getBundle("config.NotesConnectorResources", locale);
  }

  /**
   * Returns a form snippet containing the given string as an error message.
   *
   * @param error the error message to include
   * @return a ConfigureResponse consisting of a form snippet
   * with just an error message
   */
  /* See Livelink Connector */
  private ConfigureResponse getErrorResponse(String error) {
    StringBuilder buffer = new StringBuilder(
        "<tr><td colspan=\"2\"><font color=\"red\">");
    try {
      XmlUtils.xmlAppendAttrValue(error, buffer);
    } catch (IOException e) {
      // Can't happen with StringBuilder.
      throw new AssertionError(e);
    }
    buffer.append("</font></td></tr>");
    return new ConfigureResponse(null, buffer.toString());
  }

  /**
   * Returns the exception's message, or the exception class
   * name if no message is present.
   *
   * @param t the exception
   * @return a message
   */
  /* See Livelink Connector */
  private String getExceptionMessage(Throwable t) {
    String message = t.getLocalizedMessage();
    if (message != null) {
      return message;
    }
    return t.getClass().getName();
  }

  /**
   * Returns a message containing the description text, the
   * message from the given exception, and any chained
   * exception messages.
   *
   * @param description the description text
   * @param t the exception
   * @return a message
   */
  /* See Livelink Connector */
  private String getExceptionMessages(String description, Throwable t) {
    StringBuilder buffer = new StringBuilder();
    if (description != null) {
      buffer.append(description).append(" ");
    }
    buffer.append(getExceptionMessage(t)).append(" ");
    Throwable next = t;
    while ((next = next.getCause()) != null) {
      buffer.append(getExceptionMessage(next));
    }
    return buffer.toString();
  }
}
