// Copyright 2014 Google Inc.
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
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class TestUtil {

  /**
   * Capture and return log messages that match the substring.
   */
  public static List<String> captureLogMessages(Class<?> clazz,
      final String substr) {
    // Setup logger
    final List<String> logs = new ArrayList<String>();
    Logger logger = Logger.getLogger(clazz.getName());
    logger.setLevel(Level.ALL);

    logger.addHandler(new Handler() {
        @Override public void close() {}
        @Override public void flush() {}

        @Override public void publish(LogRecord arg0) {
          if (arg0.getMessage().contains(substr)) {
            logs.add(arg0.getMessage());
          }
        }
    });
    return logs;
  }

  private TestUtil() {}
}
