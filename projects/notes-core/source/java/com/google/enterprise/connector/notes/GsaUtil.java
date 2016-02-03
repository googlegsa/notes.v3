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

import com.google.common.base.Strings;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

/**
 * Helpers.
 */
class GsaUtil {

  static Collection<String> getGsaGroups(Collection<?> notesGroups,
      String groupPrefix)  {
    if (notesGroups == null || notesGroups.size() == 0) {
      return Collections.emptySet();
    }
    LinkedHashSet<String> gsaGroups =
        new LinkedHashSet<String>(notesGroups.size());
    // Prefix group names with the configured prefix.
    // Allow no prefix.
    if (Strings.isNullOrEmpty(groupPrefix)) {
      groupPrefix = "";
    } else if (!groupPrefix.endsWith("/")) {
      groupPrefix += "/";
    }
    for (Object groupObj : notesGroups) {
      String group = groupObj.toString();
      try {
        gsaGroups.add(
            URLEncoder.encode(groupPrefix + group.toLowerCase(), "UTF-8"));
      } catch (UnsupportedEncodingException e) {
        throw new AssertionError(e);
      }
    }
    return gsaGroups;
  }

  /**
   * Private constructor to prevent instantiation.
   */
  private GsaUtil() {
  }
}
