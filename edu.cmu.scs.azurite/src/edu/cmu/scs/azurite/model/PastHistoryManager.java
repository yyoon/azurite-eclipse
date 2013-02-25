package edu.cmu.scs.azurite.model;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import name.fraser.neil.plaintext.diff_match_patch;
import name.fraser.neil.plaintext.diff_match_patch.Diff;
import edu.cmu.scs.azurite.commands.diff.DiffDelete;
import edu.cmu.scs.azurite.commands.diff.DiffInsert;
import edu.cmu.scs.fluorite.commands.BaseDocumentChangeEvent;
import edu.cmu.scs.fluorite.commands.FileOpenCommand;
import edu.cmu.scs.fluorite.commands.ICommand;
import edu.cmu.scs.fluorite.model.DocumentChangeListener;
import edu.cmu.scs.fluorite.model.EventRecorder;
import edu.cmu.scs.fluorite.model.Events;
import edu.cmu.scs.fluorite.util.LogReader;
import edu.cmu.scs.fluorite.util.Utilities;

/**
 * @author YoungSeok Yoon
 *
 */
public class PastHistoryManager implements DocumentChangeListener {

	// Should always contain events 
	private Deque<Events> mPastEvents;
	
	private class InitialSnapshotElement {
		private long mSessionId;
		private long mTimestamp;
		private String mSnapshot;
		
		public InitialSnapshotElement(long sessionId, long timestamp, String snapshot) {
			mSessionId = sessionId;
			mTimestamp = timestamp;
			mSnapshot = snapshot;
		}

		public long getSessionId() {
			return mSessionId;
		}

		public long getTimestamp() {
			return mTimestamp;
		}

		public String getSnapshot() {
			return mSnapshot;
		}

		public void setSessionId(long sessionId) {
			mSessionId = sessionId;
		}

		public void setTimestamp(long timestamp) {
			mTimestamp = timestamp;
		}

		public void setSnapshot(String snapshot) {
			mSnapshot = snapshot;
		}
	}
	
	private Map<FileKey, InitialSnapshotElement> mInitialSnapshots;
	
	private PastHistoryManager() {
		mPastEvents = new LinkedList<Events>();
		
		mInitialSnapshots = new HashMap<FileKey, InitialSnapshotElement>();
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
				processEvents(events);
			}
			
			// Notify the RuntimeHistoryManager.
			RuntimeHistoryManager.getInstance().pastLogsRead(tempEvents);
		}
		
		return 0;
	}

	private void processEvents(Events events) {
		Map<FileKey, InitialSnapshotElement> localInitialSnapshots =
				new HashMap<FileKey, InitialSnapshotElement>();
		Map<FileKey, String> localFinalSnapshots = new HashMap<FileKey, String>();
		
		FileKey curFileKey = null;
		
		// Process the events here..
		for (ICommand command : events.getCommands()) {
			if (!(command instanceof BaseDocumentChangeEvent)) { continue; }
			
			if (command instanceof FileOpenCommand) {
				FileOpenCommand foc = (FileOpenCommand)command;
				FileKey key = new FileKey(foc.getProjectName(), foc.getFilePath());
				
				if (!localInitialSnapshots.containsKey(key)) {
					InitialSnapshotElement elem = new InitialSnapshotElement(
							foc.getSessionId(), foc.getTimestamp(),
							foc.getSnapshot());
					localInitialSnapshots.put(key, elem);
					
					// update the final snapshot as well.
					localFinalSnapshots.put(key, foc.getSnapshot());
				}
				
				curFileKey = key;
			}
			
			BaseDocumentChangeEvent docChange = (BaseDocumentChangeEvent)command;
			String updatedContent = docChange.applyToString(localFinalSnapshots.get(curFileKey));
			localFinalSnapshots.put(curFileKey, updatedContent);
		}
		
		// Inject intermediate diffs.
		injectDiffDCs(events, localInitialSnapshots, localFinalSnapshots);
		
		// Update the initial snapshots map.
		for (FileKey key : localInitialSnapshots.keySet()) {
			if (!mInitialSnapshots.containsKey(key)) {
				mInitialSnapshots.put(key, localInitialSnapshots.get(key));
			} else {
				InitialSnapshotElement elem = mInitialSnapshots.get(key);
				InitialSnapshotElement localElem = localInitialSnapshots.get(key);
				
				elem.setSessionId(localElem.getSessionId());
				elem.setTimestamp(localElem.getTimestamp());
				elem.setSnapshot(localElem.getSnapshot());
			}
		}

		// Finally, add the events to the end.
		mPastEvents.addFirst(events);
	}

	private void injectDiffDCs(Events events,
			Map<FileKey, InitialSnapshotElement> localInitialSnapshots,
			Map<FileKey, String> localFinalSnapshots) {
		diff_match_patch dmp = new diff_match_patch();
		
		for (FileKey key : localInitialSnapshots.keySet()) {
			if (!mInitialSnapshots.containsKey(key)) { continue; }
			
			String finalContent = localFinalSnapshots.get(key);
			InitialSnapshotElement elem = mInitialSnapshots.get(key);
			
			// If the final snapshot of this session differs from
			// the initial snapshot that we know so far,
			// compute the diffs and add fake operations.
			if (elem.getSnapshot() != null && !elem.getSnapshot().equals(finalContent)) {
				LinkedList<Diff> diffs = dmp.diff_main(finalContent, elem.getSnapshot());
				int curOffset = 0;
				int curLength = finalContent.length();
				for (Diff diff : diffs) {
					switch (diff.operation) {
						case INSERT: {
							DiffInsert di = new DiffInsert(key, curOffset, diff.text, null);
							di.setSessionId(elem.getSessionId());
							di.setTimestamp(elem.getTimestamp());
							di.setTimestamp2(elem.getTimestamp());
							
							curOffset += diff.text.length();
							curLength += diff.text.length();
							
							di.setDocLength(curLength);
							
							events.addCommand(di);
							break;
						}
							
						case DELETE: {
							DiffDelete dd = new DiffDelete(key, curOffset,
									diff.text.length(), -1, -1, diff.text, null);
							dd.setSessionId(elem.getSessionId());
							dd.setTimestamp(elem.getTimestamp());
							dd.setTimestamp2(elem.getTimestamp());
							
							curLength -= diff.text.length();
							
							dd.setDocLength(curLength);
							
							events.addCommand(dd);
							break;
						}
							
						case EQUAL: {
							curOffset += diff.text.length();
							break;
						}
					}
				}
			}
		}
	}

	@Override
	public void activeFileChanged(FileOpenCommand foc) {
		FileKey key = new FileKey(foc.getProjectName(), foc.getFilePath());
		
		if (!mInitialSnapshots.containsKey(key)) {
			InitialSnapshotElement elem = new InitialSnapshotElement(
					foc.getSessionId(), foc.getTimestamp(), foc.getSnapshot());
			mInitialSnapshots.put(key, elem);
		}
	}

	@Override
	public void documentChanged(BaseDocumentChangeEvent docChange) {
	}

	@Override
	public void documentChangeFinalized(BaseDocumentChangeEvent docChange) {
	}

	@Override
	public void documentChangeUpdated(BaseDocumentChangeEvent docChange) {
	}
	
}
