package com.google.enterprise.connector.notes;

import java.util.ArrayList; //import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import lotus.domino.*;

//import java.io.FileInputStream;
//import java.io.FileOutputStream;
//import java.io.ObjectInputStream;
//import java.io.ObjectOutputStream;

import com.google.enterprise.connector.notes.NotesConnectorDocumentList;
//import com.google.enterprise.connector.spi.Document;
import com.google.enterprise.connector.spi.DocumentList;
import com.google.enterprise.connector.spi.TraversalManager;

public class NotesTraversalManager implements TraversalManager {
	//private static final int MAX_DOCID = 1000;
	private int batchHint = 10;
	private static final String CLASS_NAME = NotesTraversalManager.class.getName();
	private static final Logger _logger = Logger.getLogger(CLASS_NAME);
	private NotesConnectorSession ncs = null;
	
	public NotesTraversalManager(NotesConnectorSession session){
		ncs = session;
	}

	
	
	public void setBatchHint(int hint) {
		final String METHOD = "setBatchHint";
		_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "batchHint set to : " + hint);
		batchHint = hint;
	}

	public DocumentList startTraversal() {
		final String METHOD = "startTraversal";
		_logger.entering(CLASS_NAME, METHOD);

		// This will reset the start date on all connector		
		NotesDatabasePoller.resetDatabases(ncs);
		return traverse("0");
		
	}
 
	public DocumentList resumeTraversal(String checkpoint) {
		return traverse(checkpoint);
	}

	/**
	 * Utility method to produce a {@code DocumentList} containing the next
	 * batch of {@code Document} from the checkpoint.
	 * 
	 * @param checkpoint
	 *            a String representing the last document number processed.
	 */
	private DocumentList traverse(String checkpoint) {
		final String METHOD = "traverse";
		List<String> unidList = new ArrayList<String>(batchHint);
		Session ns = null;

		try {
			_logger.entering(CLASS_NAME, METHOD);

			_logger.logp(Level.FINE, CLASS_NAME, METHOD, "Resuming from checkpoint: " + checkpoint);


			ns = ncs.createNotesSession();
			Database cdb = ns.getDatabase(ncs.getServer(), ncs.getDatabase());
			
			// Poll for changes
			// TODO:  Consider moving this to the housekeeping thread
			// Since it takes two polling cycles to get documents into the GSA
			// if the system is idle
			
			NotesDatabasePoller dbpoller = new NotesDatabasePoller();
			dbpoller.pollDatabases(ns, cdb, ncs.getMaxCrawlQDepth());
			NotesPollerNotifier npn = ncs.getNotifier();
			npn.wakeWorkers();
			Thread.sleep(2000);  // Give the worker threads a chance to pre-fetch documents
			
			// Get list of pre-fetched documents and put these in the doclist
			View submitQ = cdb.getView(NCCONST.VIEWSUBMITQ);
			lotus.domino.ViewNavigator submitQNav = submitQ.createViewNav();
			lotus.domino.ViewEntry ve = submitQNav.getFirst();
			int batchSize = 0;
			while (( ve != null) && (batchSize < batchHint)) {
				batchSize++;
				String unid = ve.getColumnValues().elementAt(1).toString();
				_logger.logp(Level.FINEST, CLASS_NAME, METHOD, "Adding document to list" + unid);
				unidList.add(unid);
				lotus.domino.ViewEntry prevVe = ve;
				ve = submitQNav.getNext(prevVe);
				prevVe.recycle();
			}		
			submitQNav.recycle();
			submitQ.recycle();
						 
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			ncs.closeNotesSession(ns);
			ns = null;
		}

		_logger.exiting(CLASS_NAME, METHOD);
		return new NotesConnectorDocumentList(ncs, unidList);
		
	}

}
