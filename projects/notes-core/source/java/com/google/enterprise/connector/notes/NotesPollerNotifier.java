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

import java.util.logging.Level;
import java.util.logging.Logger;

class NotesPollerNotifier {
  private static final String CLASS_NAME = NotesPollerNotifier.class.getName();
  private static final Logger LOGGER = Logger.getLogger(CLASS_NAME);
  private  NotesConnector nc;
  private int NumThreads = 1;

  public NotesPollerNotifier(NotesConnector connector) {
    nc = connector;
  }

  /**
   * Wrapper around java.lang.Object wait().
   */
  synchronized void waitForWork() {
    try {
      // If we are shutting down, don't wait
      if (nc.getShutdown()) {
        LOGGER.log(Level.INFO, "Connector is shutting down.");
        return;
      }
      LOGGER.log(Level.FINE, "Thread waiting.");
      wait();
      LOGGER.log(Level.FINE, "Thread resuming.");
    } catch (InterruptedException e) {
      LOGGER.log(Level.SEVERE, CLASS_NAME, e);
    }
  }

  synchronized void setNumThreads(int i) {
    NumThreads = i;
  }

  /**
   * Calls java.lang.Object.notifyAll() numThreads times (once
   * per crawler thread, plus once for the maintenance thread
   * (see NotesConnector where numThreads is set)).
   */
  synchronized void wakeWorkers() {
    LOGGER.log(Level.FINE, "Waking worker threads.");
    for (int i = 0; i < NumThreads; i++) {
      notifyAll();
    }
  }
}


