/**
 * 
 */
package edu.cmu.scs.azurite.model;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

import edu.cmu.scs.fluorite.model.EventRecorder;
import edu.cmu.scs.fluorite.model.Events;
import edu.cmu.scs.fluorite.util.LogReader;
import edu.cmu.scs.fluorite.util.Utilities;

/**
 * @author YoungSeok Yoon
 *
 */
public class PastHistoryManager {

	// Should always contain events 
	private Deque<Events> mPastEvents;	 
	
	private PastHistoryManager() {
		mPastEvents = new LinkedList<Events>();
	}

	private static PastHistoryManager _instance;
	/**
	 * Returns the singleton instance of this class.
	 * @return The singleton instance of this class.
	 */
	public static PastHistoryManager getInstance() {
		if (_instance == null) {
			_instance = new PastHistoryManager();
		}
		
		return _instance;
	}
	
	public Deque<Events> getPastEvents() {
		return mPastEvents;
	}
	
	public int readPastLogs(int count) {
		if (count <= 0) {
			throw new IllegalArgumentException("Count should be positive!");
		}
		
		File logLocation = null;
		try {
			logLocation = Utilities.getLogLocation();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		if (!logLocation.isDirectory()) {
			throw new IllegalStateException("Log location is not a directory!");
		}

		// Get the earliest log file that is read.
		long currentStartTimestamp = EventRecorder.getInstance().getStartTimestamp();
		if (mPastEvents.size() > 0) {
			currentStartTimestamp = mPastEvents.peekFirst().getStartTimestamp();
		}
		
		final String currentLogName = Utilities.getUniqueLogNameByTimestamp(
				currentStartTimestamp, false);
		
		// Retrieve *.xml files from the log location.
		File[] logFiles = logLocation.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				// TODO use regex to match the file naming pattern.
				return name != null && name.endsWith(".xml")
						&& name.compareTo(currentLogName) < 0;
			}
		});
		
		// Sort the files (just in case.)
		Arrays.sort(logFiles, new Comparator<File>() {
			public int compare(File lhs, File rhs) {
				return lhs.getName().compareTo(rhs.getName());
			}
		});
		
		// Pick the last 'count' number of files.
		File[] logFilesToRead = logFiles;
		if (logFiles.length >= count) {
			logFilesToRead = Arrays.copyOfRange(logFiles, logFiles.length
					- count, logFiles.length);
		}

		// Read the logs.
		List<Events> tempEvents = new ArrayList<Events>();
		
		LogReader reader = new LogReader();
		for (File logFile : logFilesToRead) {
			Events events = reader.readDocumentChanges(logFile.getAbsolutePath());
			if (events != null) {
				tempEvents.add(events);
			}
		}
		
		if (!tempEvents.isEmpty()) {
			// Add in reverse order.
			Collections.reverse(tempEvents);
			for (Events events : tempEvents) {
				mPastEvents.addFirst(events);
			}
			
			// Notify the RuntimeHistoryManager.
			RuntimeHistoryManager.getInstance().pastLogsRead(tempEvents);
		}
		
		return 0;
	}
	
}
