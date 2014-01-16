// Copyright 2012 Google Inc.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotesDomainNames {
  private static final String CLASS_NAME = NotesDomainNames.class.getName();
  private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);

  private final SortedMap<String, Long> domainTree;

  public NotesDomainNames() {
    domainTree = Collections.synchronizedSortedMap(new TreeMap<String, Long>());
  }

  public void add(String name, Long id) {
    domainTree.put(reverse(name.toLowerCase()), id);
  }

  public Long get(String name) {
    return domainTree.get(reverse(name.toLowerCase()));
  }

  public LinkedHashMap<String, Long> getSubDomainNames(
      String domainName) {
    final String METHOD = "getSubDomainNames";
    LinkedHashMap<String, Long> lmap = new LinkedHashMap<String, Long>();
    String reversedName = reverse(domainName.toLowerCase());
      SortedMap<String, Long> subs = domainTree.tailMap(reversedName);
    for (String key : subs.keySet()) {
      if (!key.startsWith(reversedName))
        break;
      lmap.put(reverse(key), subs.get(key));
    }
    LOGGER.logp(Level.FINE, CLASS_NAME, METHOD, "Sub domain names for " + 
        domainName + ": " + lmap.keySet().toString());
    return lmap;
  }

  public int size() {
    return domainTree.size();
  }

  /**
   * Reverse canonical or abbreviated name
   * @param canonical or abbreviated name
   * @param delimiter
   * @return
   */
  private String reverse(String name, String delim) {
    String[] ary = name.split(delim);
    StringBuilder buf = new StringBuilder();
    for (int i = ary.length - 1; i >= 0; i--) {
      buf.append(ary[i]).append(delim);
    }
    return buf.deleteCharAt(buf.length() - 1).toString();
  }

  private String reverse(String name) {
    return reverse(name, "/");
  }

  @Override
  public String toString() {
    return domainTree.toString();
  }

  /**
   * Compute person's expanded domain hierarchies.
   */
  public List<String> computeExpandedWildcardDomainNames(String canonicalName) {
    final String METHOD = "computeWildcardDomainNames";
    List<String> ous = new ArrayList<String>();
    for (int index = canonicalName.indexOf('/'); index != -1; 
        index = canonicalName.indexOf('/')) {
      String ou = canonicalName.substring(index + 1);
      canonicalName = ou;
      ous.add("*/" + ou);
    }
    LOGGER.logp(Level.FINE, CLASS_NAME, METHOD, "Domains for " + 
        canonicalName + ": " + ous);
    return ous;
  }
}
