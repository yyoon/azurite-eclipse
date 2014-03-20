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
import edu.cmu.scs.fluorite.commands.AbstractCommand;
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
	
	/* package */ class SnapshotElement {
		private long mSessionId;
		private long mTimestamp;
		private String mSnapshot;
		
		public SnapshotElement(long sessionId, long timestamp, String snapshot) {
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
	
	class IntegerContainer {
		public int value;
	}
	
	/* package */ Map<FileKey, SnapshotElement> mInitialSnapshots;
	
	private PastHistoryManager() {
		mPastEvents = new LinkedList<Events>();
		
		mInitialSnapshots = new HashMap<FileKey, SnapshotElement>();
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

		readPastLogs(logFilesToRead);
		
		return logFilesToRead.length;
	}

	public void readPastLogs(File[] logFilesToRead) {
		// Read the logs.
		List<Events> tempEvents = new ArrayList<Events>();
		
		LogReader reader = new LogReader();
		for (File logFile : logFilesToRead) {
			Events events = reader.readAll(logFile.getAbsolutePath());
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
	}

	private void processEvents(Events events) {
		Map<FileKey, SnapshotElement> localInitialSnapshots =
				new HashMap<FileKey, SnapshotElement>();
		Map<FileKey, String> localFinalSnapshots = new HashMap<FileKey, String>();
		
		FileKey curFileKey = null;
		
		final Events copyEvents = new Events(
				Collections.<ICommand> emptyList(), "", Long.toString(events
						.getStartTimestamp()), "", events.getStartTimestamp());
		
		final IntegerContainer insertedCount = new IntegerContainer();
		insertedCount.value = 0;
		
		// Process the events here..
		for (ICommand command : events.getCommands()) {
			
			// Adjust the command index.
			command.setCommandIndex(command.getCommandIndex() + insertedCount.value);
			copyEvents.addCommand(command);
			
			if (!(command instanceof BaseDocumentChangeEvent)) {
				continue;
			}
			
			if (command instanceof FileOpenCommand) {
				final FileOpenCommand foc = (FileOpenCommand)command;
				FileKey key = new FileKey(foc.getProjectName(), foc.getFilePath());
				
				if (!localInitialSnapshots.containsKey(key)) {
					SnapshotElement elem = new SnapshotElement(
							foc.getSessionId(), foc.getTimestamp(),
							foc.getSnapshot());
					localInitialSnapshots.put(key, elem);
					
					localFinalSnapshots.put(key, foc.getSnapshot());
				}
				
				curFileKey = key;
				
				// Inject DiffDCs here, too.
				String knownFinalSnapshot = localFinalSnapshots.get(curFileKey);
				if (foc.getSnapshot() != null && knownFinalSnapshot != null
						&& !foc.getSnapshot().equals(knownFinalSnapshot)) {
					injectDiffDCs(curFileKey, knownFinalSnapshot,
							foc.getSnapshot(), foc.getSessionId(),
							foc.getTimestamp(), false, new IAddCommand() {
								@Override
								public void addCommand(ICommand command) {
									++insertedCount.value;
									command.setCommandIndex(foc
											.getCommandIndex()
											+ insertedCount.value);
									copyEvents.addCommand(command);
								}
							});
				}
			}
			
			BaseDocumentChangeEvent docChange = (BaseDocumentChangeEvent)command;
			String originalContent = localFinalSnapshots.get(curFileKey);
			if (originalContent != null || docChange instanceof FileOpenCommand) {
				String updatedContent = docChange.applyToString(originalContent);
				localFinalSnapshots.put(curFileKey, updatedContent);
			}
		}
		
		// Inject intermediate diffs.
		injectDiffDCsWhileReadingPreviousLog(copyEvents, localInitialSnapshots, localFinalSnapshots);
		
		// Update the initial snapshots map.
		for (FileKey key : localInitialSnapshots.keySet()) {
			if (!mInitialSnapshots.containsKey(key)) {
				mInitialSnapshots.put(key, localInitialSnapshots.get(key));
			} else {
				SnapshotElement elem = mInitialSnapshots.get(key);
				SnapshotElement localElem = localInitialSnapshots.get(key);
				
				elem.setSessionId(localElem.getSessionId());
				elem.setTimestamp(localElem.getTimestamp());
				elem.setSnapshot(localElem.getSnapshot());
			}
		}

		// Finally, add the events to the end.
		mPastEvents.addFirst(copyEvents);
	}
	
	public void injectDiffDCs(FileKey key, String before, String after,
			long sessionId, long timestamp, boolean autoAssignId,
			IAddCommand addCommand) {
		if (before == null || after == null) {
			throw new IllegalArgumentException("Cannot process null strings.");
		}
		
		diff_match_patch dmp = new diff_match_patch();
		LinkedList<Diff> diffs = dmp.diff_main(before, after);
		int curOffset = 0;
		int curLength = before.length();
		
		boolean incrementCommandID = AbstractCommand.getIncrementCommandID();
		try {
			if (autoAssignId == false) {
				AbstractCommand.setIncrementCommandID(false);
			}
			
			for (Diff diff : diffs) {
				switch (diff.operation) {
					case INSERT: {
						DiffInsert di = new DiffInsert(key, curOffset, diff.text, null);
						di.setSessionId(sessionId);
						di.setTimestamp(timestamp);
						di.setTimestamp2(timestamp);
						
						curOffset += diff.text.length();
						curLength += diff.text.length();
						
						di.setDocLength(curLength);
						
						addCommand.addCommand(di);
						break;
					}
					
					case DELETE: {
						DiffDelete dd = new DiffDelete(key, curOffset,
								diff.text.length(), -1, -1, diff.text, null);
						dd.setSessionId(sessionId);
						dd.setTimestamp(timestamp);
						dd.setTimestamp2(timestamp);
						
						curLength -= diff.text.length();
						
						dd.setDocLength(curLength);
						
						addCommand.addCommand(dd);
						break;
					}
					
					case EQUAL: {
						curOffset += diff.text.length();
						break;
					}
				}
			}
		} finally {
			AbstractCommand.setIncrementCommandID(incrementCommandID);
		}
	}

	private void injectDiffDCsWhileReadingPreviousLog(final Events events,
			Map<FileKey, SnapshotElement> localInitialSnapshots,
			Map<FileKey, String> localFinalSnapshots) {
		
		for (FileKey key : localInitialSnapshots.keySet()) {
			if (!mInitialSnapshots.containsKey(key)) {
				continue;
			}
			
			String finalContent = localFinalSnapshots.get(key);
			SnapshotElement elem = mInitialSnapshots.get(key);
			
			// If the final snapshot of this session differs from
			// the initial snapshot that we know so far,
			// compute the diffs and add fake operations.
			if (elem.getSnapshot() == null || finalContent == null) {
				continue;
			}
			
			if (!elem.getSnapshot().equals(finalContent)) {
				injectDiffDCs(key, finalContent, elem.getSnapshot(),
						elem.getSessionId(), elem.getTimestamp(), false,
						new IAddCommand() {
							@Override
							public void addCommand(ICommand command) {
								ICommand lastCommand = events.getCommands()
										.get(events.getCommands().size() - 1);

								// Fake the command index.
								command.setCommandIndex(lastCommand
										.getCommandIndex() + 1);
								events.addCommand(command);
							}
						});
			}
		}
	}

	@Override
	public void activeFileChanged(FileOpenCommand foc) {
		FileKey key = new FileKey(foc.getProjectName(), foc.getFilePath());
		
		if (!mInitialSnapshots.containsKey(key)) {
			SnapshotElement elem = new SnapshotElement(
					foc.getSessionId(), foc.getTimestamp(), foc.getSnapshot());
			mInitialSnapshots.put(key, elem);
		}
	}

	@Override
	public void documentChanged(BaseDocumentChangeEvent docChange) {
		// Do nothing for this event
	}

	@Override
	public void documentChangeFinalized(BaseDocumentChangeEvent docChange) {
		// Do nothing for this event
	}

	@Override
	public void documentChangeUpdated(BaseDocumentChangeEvent docChange) {
		// Do nothing for this event
	}

	@Override
	public void documentChangeAmended(BaseDocumentChangeEvent oldDocChange, BaseDocumentChangeEvent newDocChange) {
		// Do nothing for this event
	}
	
}
