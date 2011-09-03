package com.google.enterprise.connector.notes;

import java.util.logging.Level;
import java.util.logging.Logger;


public class NotesPollerNotifier {
	private static final String CLASS_NAME = NotesPollerNotifier.class.getName();
	private static final Logger _logger = Logger.getLogger(CLASS_NAME);
	private  NotesConnector nc;
	int NumThreads = 1;
	
	public NotesPollerNotifier(NotesConnector connector) {
		final String METHOD="NotesPollerNotifier";
		_logger.logp(Level.INFO, CLASS_NAME, METHOD, "NotesPollerNotifier being created.");
		nc = connector;
	}
	
	synchronized void waitForWork(long timeout) {
		final String METHOD="waitForWork";
		try {
			// If we are shutting down, don't wait
			if (nc.getShutdown()) {
				_logger.logp(Level.INFO, CLASS_NAME, METHOD, "Connector is shutting down.");
				return;
			}
			_logger.logp(Level.FINE, CLASS_NAME, METHOD, "Thread waiting with timeout.");
			wait(timeout);
			_logger.logp(Level.FINE, CLASS_NAME, METHOD, "Thread resuming.");
		}
		catch (InterruptedException e) {
			_logger.log(Level.SEVERE, CLASS_NAME, e);
		}
	}

	
	synchronized void waitForWork() {
		final String METHOD="waitForWork";
		try {
			// If we are shutting down, don't wait
			if (nc.getShutdown()) {
				_logger.logp(Level.INFO, CLASS_NAME, METHOD, "Connector is shutting down.");
				return;
			}
			_logger.logp(Level.FINE, CLASS_NAME, METHOD, "Thread waiting.");
			wait();
			_logger.logp(Level.FINE, CLASS_NAME, METHOD, "Thread resuming.");
		}
		catch (InterruptedException e) {
			_logger.log(Level.SEVERE, CLASS_NAME, e);
		}
	}

	synchronized void setNumThreads(int i){
		NumThreads = i;
	}
	
	synchronized void wakeWorkers() {
		final String METHOD="wakeWorkers";
		_logger.logp(Level.FINE, CLASS_NAME, METHOD, "Waking worker threads.");
		for(int i=0; i<NumThreads; i++) 
			notifyAll();
	}
}


