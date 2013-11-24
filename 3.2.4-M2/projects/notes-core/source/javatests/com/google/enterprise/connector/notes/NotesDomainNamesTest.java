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

import junit.framework.TestCase;

import java.util.LinkedHashMap;
import java.util.List;

public class NotesDomainNamesTest extends TestCase {
  private String[] names = {
      "CN=Lemon Grass/OU=West/O=ABC",
      "CN=Apple Pie/OU=South/OU=West/O=ABC",
      "CN=Ginger Bread/OU=North/OU=West/O=ABC",
      "CN=Red Beans/OU=North/OU=East/O=ABC",
      "CN=Green Beans/OU=South/OU=East/O=ABC",
      "CN=Soy Beans/OU=South/OU=East/O=ABC",
      "CN=Bitter Melon/OU=East/O=ABC"
  };
  
  private NotesDomainNames dns;
  
  private void initDomainTree() {
    dns = new NotesDomainNames();
    int id = 1;
    for (String name : names) {
      List<String> userExpandedDomains = 
          dns.computeExpandedWildcardDomainNames(name);
      for (String expandedDomain : userExpandedDomains) {
        dns.add(expandedDomain, new Long(id++));
      }
    }
  }
  
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    initDomainTree();
  }

  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public void testDomainSize() {
    assertEquals(7, dns.size());
  }

  public void testSubdomains() {
    LinkedHashMap<String, Long> westDomains = 
        dns.getSubDomainNames("/OU=West/O=ABC");
    assertEquals(3, westDomains.size());
    assertTrue("*/ou=west/o=abc", westDomains.containsKey("*/ou=west/o=abc"));
    assertTrue("*/ou=north/ou=west/o=abc",
        westDomains.containsKey("*/ou=north/ou=west/o=abc"));
    assertTrue("*/ou=south/ou=west/o=abc",
            westDomains.containsKey("*/ou=south/ou=west/o=abc"));
    
    LinkedHashMap<String, Long> eastDomains = 
        dns.getSubDomainNames("/OU=East/O=ABC");
    assertEquals(3, eastDomains.size());
    assertTrue("*/ou=east/o=abc", eastDomains.containsKey("*/ou=east/o=abc"));
    assertTrue("*/ou=north/ou=east/o=abc",
        eastDomains.containsKey("*/ou=north/ou=east/o=abc"));
    assertTrue("*/ou=south/ou=east/o=abc",
        eastDomains.containsKey("*/ou=south/ou=east/o=abc"));

    LinkedHashMap<String, Long> subWestDomains = 
        dns.getSubDomainNames("/OU=South/OU=West/O=ABC");
    assertEquals(1, subWestDomains.size());
    assertTrue("*/ou=south/ou=west/o=abc",
            subWestDomains.containsKey("*/ou=south/ou=west/o=abc"));

    LinkedHashMap<String, Long> subEastDomains = 
        dns.getSubDomainNames("/OU=South/OU=East/O=ABC");
    assertEquals(1, subEastDomains.size());
    assertTrue("*/ou=south/ou=east/o=abc",
            subEastDomains.containsKey("*/ou=south/ou=east/o=abc"));
  }
  
  public void testExpandedWildcardDomainNames() {
    List<String> expandedDomains = 
        dns.computeExpandedWildcardDomainNames(names[0]);
    assertEquals(2, expandedDomains.size());
  }
}
