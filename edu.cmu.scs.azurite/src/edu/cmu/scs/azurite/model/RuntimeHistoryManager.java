package edu.cmu.scs.azurite.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.ListenerList;

import edu.cmu.scs.azurite.commands.diff.IDiffDC;
import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;
import edu.cmu.scs.azurite.commands.runtime.Segment;
import edu.cmu.scs.fluorite.commands.AnnotateCommand;
import edu.cmu.scs.fluorite.commands.BaseDocumentChangeEvent;
import edu.cmu.scs.fluorite.commands.EclipseCommand;
import edu.cmu.scs.fluorite.commands.FileOpenCommand;
import edu.cmu.scs.fluorite.commands.ICommand;
import edu.cmu.scs.fluorite.commands.JUnitCommand;
import edu.cmu.scs.fluorite.commands.RunCommand;
import edu.cmu.scs.fluorite.model.CommandExecutionListener;
import edu.cmu.scs.fluorite.model.DocumentChangeListener;
import edu.cmu.scs.fluorite.model.EventRecorder;
import edu.cmu.scs.fluorite.model.Events;

public class RuntimeHistoryManager implements DocumentChangeListener, CommandExecutionListener {
	
	// used for keeping track of the last index of the DC
	private Map<FileKey, Integer> mNextIndexToApply;
	
	private Map<FileKey, List<RuntimeDC>> mDocumentChanges;
	private FileKey mCurrentFileKey;
	
	private List<ICommand> mEventsToBeDisplayed;
	
	private ListenerList mRuntimeDocumentChangeListeners;

	private List<Runnable> mScheduledTasks;
	
	private boolean mStarted;
	
	// Keep the document changes made in the current session separately.
	private Events mCurrentSessionEvents;
	
	/**
	 * Basic constructor. Only use this public constructor for testing purposes!
	 * Otherwise, use <code>getInstance</code> static method instead.
	 */
	public RuntimeHistoryManager() {
		clearData();
		
		mRuntimeDocumentChangeListeners = new ListenerList();
		
		mScheduledTasks = new ArrayList<Runnable>();
		
		mStarted = false;
		
		long startTimestamp = EventRecorder.getInstance().getStartTimestamp();
		mCurrentSessionEvents = new Events(Collections.<ICommand> emptyList(),
				"Current Session", Long.toString(startTimestamp), "",
				startTimestamp);
	}

	private void clearData() {
		mDocumentChanges = new HashMap<FileKey, List<RuntimeDC>>();
		mNextIndexToApply = new HashMap<FileKey, Integer>();
		mCurrentFileKey = null;
		mEventsToBeDisplayed = new ArrayList<ICommand>();
	}

	public void scheduleTask(Runnable runnable) {
		if (mStarted) {
			runnable.run();
		} else {
			mScheduledTasks.add(runnable);
		}
	}

	private static RuntimeHistoryManager _instance;
	/**
	 * Returns the singleton instance of this class.
	 * @return The singleton instance of this class.
	 */
	public static RuntimeHistoryManager getInstance() {
		if (_instance == null) {
			_instance = new RuntimeHistoryManager();
		}
		
		return _instance;
	}
	
	/**
	 * Start the Runtime History Manager, which is essentially the main model
	 * of Azurite 
	 */
	public void start() {
		EventRecorder.getInstance().addDocumentChangeListener(this);
		EventRecorder.getInstance().addDocumentChangeListener(PastHistoryManager.getInstance());
		EventRecorder.getInstance().addCommandExecutionListener(this);
		mStarted = true;
		
		// Execute all the scheduled tasks.
		for (Runnable runnable : mScheduledTasks) {
			runnable.run();
		}
	}
	
	/**
	 * Stop the Runtime History Manager.
	 */
	public void stop() {
		EventRecorder.getInstance().removeDocumentChangeListener(this);
	}

	/**
	 * Add runtime document change listener
	 * @param listener
	 */
	public void addRuntimeDocumentChangeListener(RuntimeDCListener listener) {
		mRuntimeDocumentChangeListeners.add(listener);
	}
	
	/**
	 * Remove runtime document change listener
	 * @param listener
	 */
	public void removeRuntimeDocumentChangeListener(RuntimeDCListener listener) {
		mRuntimeDocumentChangeListeners.remove(listener);
	}
	
	private void fireActiveFileChangedEvent(String projectName, String filePath) {
		for (Object listenerObj : mRuntimeDocumentChangeListeners.getListeners()) {
			((RuntimeDCListener)listenerObj).activeFileChanged(projectName, filePath);
		}
	}
	
	private void fireRuntimeDCAddedEvent(RuntimeDC docChange) {
		for (Object listenerObj : mRuntimeDocumentChangeListeners.getListeners()) {
			((RuntimeDCListener)listenerObj).runtimeDCAdded(docChange);
		}
	}
	
	private void fireDocumentChangeAddedEvent(BaseDocumentChangeEvent docChange) {
		for (Object listenerObj : mRuntimeDocumentChangeListeners.getListeners()) {
			((RuntimeDCListener)listenerObj).documentChangeAdded(docChange);
		}
	}
	
	private void fireDocumentChangeUpdatedEvent(BaseDocumentChangeEvent docChange) {
		for (Object listenerObj : mRuntimeDocumentChangeListeners.getListeners()) {
			((RuntimeDCListener)listenerObj).documentChangeUpdated(docChange);
		}
	}
	
	private void firePastLogsReadEvent(List<Events> events) {
		for (Object listenerObj : mRuntimeDocumentChangeListeners.getListeners()) {
			((RuntimeDCListener)listenerObj).pastLogsRead(events);
		}
	}
	
	private void fireDocumentChangeAmendedEvent(BaseDocumentChangeEvent oldDocChange, BaseDocumentChangeEvent newDocChange) {
		for (Object listenerObj : mRuntimeDocumentChangeListeners.getListeners()) {
			((RuntimeDCListener)listenerObj).documentChangeAmended(oldDocChange, newDocChange);
		}
	}
	
	public Set<FileKey> getFileKeys() {
		return mDocumentChanges.keySet();
	}
	
	/**
	 * Returns all the runtime document changes associated with the current file.
	 * @return all the runtime document changes associated with the current file.
	 */
	public List<RuntimeDC> getRuntimeDocumentChanges() {
		return getRuntimeDocumentChanges(getCurrentFileKey());
	}
	
	/**
	 * Returns all the runtime document changes associated with the given file.
	 * @param filePath Fullpath to the source file
	 * @return all the runtime document changes associated with the given file.
	 */
	public List<RuntimeDC> getRuntimeDocumentChanges(FileKey key) {
		if (mDocumentChanges.containsKey(key)) {
			return mDocumentChanges.get(key);
		}
		
		return null;
	}
	
	/**
	 * Returns the current file path.
	 * @return the current file path.
	 */
	public String getCurrentFile() {
		if (mCurrentFileKey != null) {
			return mCurrentFileKey.getFilePath();
		}
		
		return null;
	}
	
	/**
	 * Returns the current project name.
	 * @return the current project name.
	 */
	public String getCurrentProject() {
		if (mCurrentFileKey != null) {
			return mCurrentFileKey.getProjectName();
		}
		
		return null;
	}
	
	public FileKey getCurrentFileKey() {
		return mCurrentFileKey;
	}
	
	private void setCurrentFileKey(FileKey newFileKey) {
		mCurrentFileKey = newFileKey;
	}
	
	public List<ICommand> getEventsToBeDisplayed() {
		return Collections.unmodifiableList(mEventsToBeDisplayed);
	}

	@Override
	public void activeFileChanged(FileOpenCommand foc) {
		processFileOpenCommand(foc);
		
		mCurrentSessionEvents.addCommand(foc);
	}

	public void processFileOpenCommand(FileOpenCommand foc) {
		activeFileChanged(foc.getProjectName(), foc.getFilePath(),
				foc.getSnapshot());
		
		// If there was a change between the two snapshots..
		// This can only happen for the current session, because the past log
		// files don't have the prevSnapshot value.
		if (foc.getSnapshot() != null && foc.getPrevSnapshot() != null) {
			PastHistoryManager.getInstance().injectDiffDCs(getCurrentFileKey(),
					foc.getPrevSnapshot(), foc.getSnapshot(),	// before and after.
					foc.getSessionId(), foc.getTimestamp(),		// sessionid, timestamp.
					true,										// auto assign command id in this case.
					new IAddCommand() {							// what to do with the created diffs.
						@Override
						public void addCommand(ICommand command) {
							if (command instanceof BaseDocumentChangeEvent) {
								BaseDocumentChangeEvent docChange =
										(BaseDocumentChangeEvent) command;
								documentChanged(docChange);
								documentChangeFinalized(docChange);
							}
						}
					});
		}
	}

	/**
	 * Simply updates the current file path.
	 */
	public void activeFileChanged(String projectName, String filePath,
			String snapshot) {
		FileKey key = new FileKey(projectName, filePath);
		setCurrentFileKey(key);
		
		if (!mDocumentChanges.containsKey(key)) {
			mDocumentChanges.put(key, new ArrayList<RuntimeDC>());
			mNextIndexToApply.put(key, 0);
		}
		
		fireActiveFileChangedEvent(key.getProjectName(), key.getFilePath());
	}
	
	public void handleSnapshot(String snapshot) {
		// TODO extract diff.
		// Apply to the current file.
		if (snapshot != null) {
			getRuntimeDocumentChanges().clear();
			mNextIndexToApply.put(getCurrentFileKey(), 0);
		}
	}

	@Override
	public void documentChanged(BaseDocumentChangeEvent docChange) {
		fireDocumentChangeAddedEvent(docChange);
		
		mCurrentSessionEvents.addCommand(docChange);
	}
	
	@Override
	public void documentChangeUpdated(BaseDocumentChangeEvent docChange) {
		fireDocumentChangeUpdatedEvent(docChange);
	}
	
	@Override
	public void documentChangeFinalized(BaseDocumentChangeEvent docChange) {
		addRuntimeDCFromOriginalDC(docChange);
	}
	
	private void addRuntimeDCFromOriginalDC(BaseDocumentChangeEvent docChange) {
		addRuntimeDCFromOriginalDC(docChange, getCurrentFileKey());
	}
	
	private void addRuntimeDCFromOriginalDC(BaseDocumentChangeEvent docChange, FileKey key) {
		addRuntimeDCFromOriginalDC(docChange, key, true);
	}
	
	private void addRuntimeDCFromOriginalDC(BaseDocumentChangeEvent docChange, FileKey key, boolean fireEvent) {
		RuntimeDC runtimeDocChange = RuntimeDC.createRuntimeDocumentChange(docChange);
		runtimeDocChange.setBelongsTo(key);
		
		List<RuntimeDC> list = getRuntimeDocumentChanges(key);
		if (list == null) {
			throw new IllegalArgumentException("Key does not exist!");
		}
		list.add(runtimeDocChange);
		
		// Fire runtime document change event
		if (fireEvent) {
			fireRuntimeDCAddedEvent(runtimeDocChange);
		}
	}
	
	public List<RuntimeDC> filterDocumentChangesByIds(FileKey key, final List<OperationId> ids) {
		if (key == null || ids == null) {
			throw new IllegalArgumentException();
		}
		
		return filterDocumentChanges(key, new IRuntimeDCFilter() {
			@Override
			public boolean filter(RuntimeDC runtimeDC) {
				return ids.contains(runtimeDC.getOperationId());
			}
		});
	}
	
	public List<RuntimeDC> filterDocumentChangesByIds(final List<OperationId> ids) {
		return filterDocumentChangesByIds(getCurrentFileKey(), ids);
	}
	
	public RuntimeDC filterDocumentChangeById(FileKey key, final OperationId id) {
		if (key == null || id == null) {
			throw new IllegalArgumentException();
		}
		
		List<RuntimeDC> intermediateResult = filterDocumentChanges(key,
				new IRuntimeDCFilter() {
			@Override
			public boolean filter(RuntimeDC runtimeDC) {
				return id.equals(runtimeDC.getOperationId());
			}
		});
		
		if (intermediateResult == null || intermediateResult.isEmpty()) {
			return null;
		} else {
			return intermediateResult.get(0);
		}
	}

	public List<RuntimeDC> filterDocumentChangesGreaterThanId(final OperationId id) {
		return filterDocumentChangesGreaterThanId(getCurrentFileKey(), id);
	}

	public List<RuntimeDC> filterDocumentChangesGreaterThanId(FileKey key, final OperationId id) {
		if (key == null || id == null) {
			throw new IllegalArgumentException();
		}
		
		return filterDocumentChanges(key, new IRuntimeDCFilter() {
			@Override
			public boolean filter(RuntimeDC runtimeDC) {
				return id.compareTo(runtimeDC.getOperationId()) < 0;
			}
		});
	}
	
	public List<RuntimeDC> filterDocumentChangesLaterThanTimestamp(final long absTimestamp) {
		return filterDocumentChangesLaterThanTimestamp(getCurrentFileKey(), absTimestamp);
	}
	
	public List<RuntimeDC> filterDocumentChangesLaterThanTimestamp(FileKey key, final long absTimestamp) {
		if (key == null) {
			throw new IllegalArgumentException();
		}
		
		return filterDocumentChanges(key, new IRuntimeDCFilter() {
			@Override
			public boolean filter(RuntimeDC runtimeDC) {
				long timestamp = runtimeDC.getOriginal().getSessionId() + runtimeDC.getOriginal().getTimestamp();
				
				return absTimestamp < timestamp;
			}
		});
	}
	
	public List<RuntimeDC> filterDocumentChangesLaterThanOrEqualToTimestamp(final long absTimestamp) {
		return filterDocumentChangesLaterThanOrEqualToTimestamp(getCurrentFileKey(), absTimestamp);
	}
	
	public List<RuntimeDC> filterDocumentChangesLaterThanOrEqualToTimestamp(FileKey key, final long absTimestamp) {
		if (key == null) {
			throw new IllegalArgumentException();
		}
		
		return filterDocumentChanges(key, new IRuntimeDCFilter() {
			@Override
			public boolean filter(RuntimeDC runtimeDC) {
				long timestamp = runtimeDC.getOriginal().getSessionId() + runtimeDC.getOriginal().getTimestamp();
				
				return absTimestamp <= timestamp;
			}
		});
	}
	
	public List<RuntimeDC> filterDocumentChangesEarlierThanTimestamp(final long absTimestamp) {
		return filterDocumentChangesEarlierThanTimestamp(getCurrentFileKey(), absTimestamp);
	}
	
	public List<RuntimeDC> filterDocumentChangesEarlierThanTimestamp(FileKey key, final long absTimestamp) {
		if (key == null) {
			throw new IllegalArgumentException();
		}
		
		return filterDocumentChanges(key, new IRuntimeDCFilter() {
			@Override
			public boolean filter(RuntimeDC runtimeDC) {
				long timestamp = runtimeDC.getOriginal().getSessionId() + runtimeDC.getOriginal().getTimestamp();
				
				return absTimestamp > timestamp;
			}
		});
	}
	
	public List<RuntimeDC> filterDocumentChangesEarlierThanOrEqualToTimestamp(final long absTimestamp) {
		return filterDocumentChangesEarlierThanOrEqualToTimestamp(getCurrentFileKey(), absTimestamp);
	}
	
	public List<RuntimeDC> filterDocumentChangesEarlierThanOrEqualToTimestamp(FileKey key, final long absTimestamp) {
		if (key == null) {
			throw new IllegalArgumentException();
		}
		
		return filterDocumentChanges(key, new IRuntimeDCFilter() {
			@Override
			public boolean filter(RuntimeDC runtimeDC) {
				long timestamp = runtimeDC.getOriginal().getSessionId() + runtimeDC.getOriginal().getTimestamp();
				
				return absTimestamp >= timestamp;
			}
		});
	}
	
	public List<RuntimeDC> filterDocumentChangesLaterThanOrEqualToAndEarlierThanTimestamps(final long absTimestampStart, final long absTimestampEnd) {
		return filterDocumentChangesLaterThanOrEqualToAndEarlierThanTimestamps(getCurrentFileKey(), absTimestampStart, absTimestampEnd);
	}
	
	public List<RuntimeDC> filterDocumentChangesLaterThanOrEqualToAndEarlierThanTimestamps(FileKey key, final long absTimestampStart, final long absTimestampEnd) {
		if (key == null) {
			throw new IllegalArgumentException();
		}
		
		return filterDocumentChanges(key, new IRuntimeDCFilter() {
			@Override
			public boolean filter(RuntimeDC runtimeDC) {
				long timestamp = runtimeDC.getOriginal().getSessionId() + runtimeDC.getOriginal().getTimestamp();
				
				return absTimestampStart <= timestamp && timestamp < absTimestampEnd;
			}
		});
	}
	
	public RuntimeDC filterDocumentChangeByIdWithoutCalculating(FileKey key, final OperationId id) {
		if (key == null || id == null) {
			throw new IllegalArgumentException();
		}
		
		List<RuntimeDC> intermediateResult = filterDocumentChanges(key,
				new IRuntimeDCFilter() {
			@Override
			public boolean filter(RuntimeDC runtimeDC) {
				return id.equals(runtimeDC.getOperationId());
			}
		}, false);
		
		if (intermediateResult == null || intermediateResult.isEmpty()) {
			return null;
		} else {
			return intermediateResult.get(0);
		}
	}
	
	public List<RuntimeDC> filterDocumentChangesByRegion(final int startOffset, final int endOffset) {
		return filterDocumentChangesByRegion(getCurrentFileKey(), startOffset, endOffset);
	}
	
	public List<RuntimeDC> filterDocumentChangesByRegion(FileKey key, final int startOffset, final int endOffset) {
		return filterDocumentChanges(key, new IRuntimeDCFilter() {
			@Override
			public boolean filter(RuntimeDC runtimeDC) {
				List<Segment> segments = runtimeDC.getAllSegments();
				
				for (Segment segment : segments) {
					if (startOffset < segment.getEffectiveEndOffset() &&
						segment.getOffset() < endOffset) {
						return true;
					}
				}
				
				return false;
			}
		});
	}
	
	public List<RuntimeDC> filterDocumentChanges(IRuntimeDCFilter filter) {
		return filterDocumentChanges(getCurrentFileKey(), filter);
	}
	
	public List<RuntimeDC> filterDocumentChanges(FileKey key, IRuntimeDCFilter filter) {
		return filterDocumentChanges(key, filter, true);
	}
	
	public List<RuntimeDC> filterDocumentChanges(FileKey key, IRuntimeDCFilter filter, boolean calculate) {
		if (key == null || filter == null) {
			throw new IllegalArgumentException();
		}
		
		// It's a little bit odd, but make sure that there's no pending document changes
		// in the EventRecorder side
		EventRecorder.getInstance().fireLastDocumentChangeFinalizedEvent();
		
		// Lazy-evaluation of the dynamic segments!
		List<RuntimeDC> list = calculate ? calculateDynamicSegments(key) :
			getRuntimeDocumentChanges(key);
		
		// Then filter the results.
		List<RuntimeDC> result = new ArrayList<RuntimeDC>();
		for (RuntimeDC dc : list) {
			if (filter.filter(dc)) {
				result.add(dc);
			}
		}
		
		return result;
	}
	
	public boolean hasDocumentChangesLaterThanTimestamp(final long absTimestamp) {
		return hasDocumentChangesLaterThanTimestamp(getCurrentFileKey(), absTimestamp);
	}
	
	public boolean hasDocumentChangesLaterThanTimestamp(FileKey key, final long absTimestamp) {
		if (key == null) {
			throw new IllegalArgumentException();
		}
		
		return hasDocumentChanges(key, new IRuntimeDCFilter() {
			@Override
			public boolean filter(RuntimeDC runtimeDC) {
				long timestamp = runtimeDC.getOriginal().getSessionId() + runtimeDC.getOriginal().getTimestamp();
				
				return absTimestamp < timestamp;
			}
		});
	}
	
	public boolean hasDocumentChanges(IRuntimeDCFilter filter) {
		return hasDocumentChanges(getCurrentFileKey(), filter);
	}
	
	public boolean hasDocumentChanges(FileKey key, IRuntimeDCFilter filter) {
		return hasDocumentChanges(key, filter, true);
	}
	
	public boolean hasDocumentChanges(FileKey key, IRuntimeDCFilter filter, boolean calculate) {
		if (key == null || filter == null) {
			throw new IllegalArgumentException();
		}
		
		// It's a little bit odd, but make sure that there's no pending document changes
		// in the EventRecorder side
		EventRecorder.getInstance().fireLastDocumentChangeFinalizedEvent();
		
		// Lazy-evaluation of the dynamic segments!
		List<RuntimeDC> list = calculate ? calculateDynamicSegments(key) :
			getRuntimeDocumentChanges(key);
		
		// Then filter the results.
		for (RuntimeDC dc : list) {
			if (filter.filter(dc)) {
				return true;
			}
		}
		
		return false;
	}

	public List<RuntimeDC> calculateDynamicSegments(FileKey fileKey) {
		List<RuntimeDC> list = getRuntimeDocumentChanges(fileKey);
		if (list == null) {
			throw new IllegalStateException();
		}
		
		for (int i = mNextIndexToApply.get(fileKey); i < list.size(); ++i) {
			for (int j = 0; j < i; ++j) {
				list.get(i).applyTo(list.get(j));
			}
		}
		mNextIndexToApply.put(fileKey, list.size());
		return list;
	}
	
	public void pastLogsRead(List<Events> listEvents) {
		// TODO implement history merging.
		
		// Clear the entire history.
		clearData();
		
		// Current session data should also be included.
		List<Events> listAllEvents = new ArrayList<Events>();
		listAllEvents.addAll(PastHistoryManager.getInstance().getPastEvents());
		listAllEvents.add(mCurrentSessionEvents);
		
		// Add document changes from the history.
		for (Events events : listAllEvents) {
			List<ICommand> commands = events.getCommands();
			for (ICommand command : commands) {
				if (shouldCommandBeDisplayed(command)) {
					mEventsToBeDisplayed.add(command);
					continue;
				}
				
				if (!(command instanceof BaseDocumentChangeEvent)) {
					continue;
				}
				
				BaseDocumentChangeEvent docChange = (BaseDocumentChangeEvent)command;
				if (docChange instanceof FileOpenCommand) {
					processFileOpenCommand((FileOpenCommand)docChange);
				} else {
					processDocumentChange(docChange);
				}
			}
		}

		// Notify the listeners (mainly, Timeline View)
		// Timeline view only needs the events newly added.
		firePastLogsReadEvent(listEvents);
	}

	private void processDocumentChange(BaseDocumentChangeEvent docChange) {
		// Doc Change generated by Diff. (see PastHistoryManager)
		if (docChange instanceof IDiffDC) {
			addRuntimeDCFromOriginalDC(docChange,
					((IDiffDC) docChange).getFileKey());
		} else {
		// Normal Doc Changes.
			addRuntimeDCFromOriginalDC(docChange);
		}
	}

	public Map<FileKey, List<RuntimeDC>> extractFileDCMapFromOperationIds(
			List<OperationId> ids) {
		// Filter only the files that contain one or more selected rectangles.
		Set<FileKey> fileKeys = getFileKeys();
		Map<FileKey, List<RuntimeDC>> params = new HashMap<FileKey, List<RuntimeDC>>();
		for (FileKey key : fileKeys) {
			List<RuntimeDC> filteredIds = filterDocumentChangesByIds(key, ids);
			if (filteredIds.size() == 0) {
				continue;
			}
			
			params.put(key, filteredIds);
		}
		return params;
	}

	public static boolean shouldCommandBeDisplayed(ICommand command) {
		if (command instanceof JUnitCommand) {
			return true;
		} else if (command instanceof RunCommand) {
			return true;
		} else if (command instanceof AnnotateCommand) {
			return true;
		} else if (command instanceof EclipseCommand) {
			EclipseCommand eclipseCommand = (EclipseCommand) command;
			switch (eclipseCommand.getCommandID()) {
				case "org.eclipse.ui.file.save":
					
				// Egit commands
				case "org.eclipse.egit.ui.team.Commit":
				case "org.eclipse.egit.ui.team.Pull":
				case "org.eclipse.egit.ui.team.Fetch":
				case "org.eclipse.egit.ui.team.Push":
				case "org.eclipse.egit.ui.team.Merge":
				case "org.eclipse.egit.ui.team.Reset":
				case "org.eclipse.egit.ui.team.Rebase":
				case "org.eclipse.egit.ui.CheckoutCommand":
					return true;
					
				default:
					return false;
			}
		}
		
		return false;
	}

	@Override
	public void commandExecuted(ICommand command) {
		if (shouldCommandBeDisplayed(command)) {
			mCurrentSessionEvents.addCommand(command);
			mEventsToBeDisplayed.add(command);
		}
	}

	@Override
	public void documentChangeAmended(BaseDocumentChangeEvent oldDocChange, BaseDocumentChangeEvent newDocChange) {
		// Find out the runtime DC associated with the oldDocChange,
		// and replace the originalDC to the new one.
		// Presumably, this is the last runtime DC associated with the current file.
		List<RuntimeDC> dcs = getRuntimeDocumentChanges();
		if (dcs == null || dcs.isEmpty()) {
			throw new IllegalStateException("Could not find the runtimeDC associated with the oldDocChange.");
		}
		
		RuntimeDC candidateDC = dcs.get(dcs.size() - 1);
		if (candidateDC.getOriginal() != oldDocChange) {
			throw new IllegalStateException("The last runtime DC is not the desired one!");
		}
		
		if (mNextIndexToApply.get(getCurrentFileKey()) >= dcs.size()) {
			throw new IllegalStateException("The dynamic segment calculation is already done!");
		}
		
		// Delete the last one from dcs, and add a new one.
		dcs.remove(dcs.size() - 1);
		addRuntimeDCFromOriginalDC(newDocChange, getCurrentFileKey(), false);
		
		// Also, amend the current session events.
		int oldIndex = mCurrentSessionEvents.getCommands().indexOf(oldDocChange);
		mCurrentSessionEvents.getCommands().set(oldIndex, newDocChange);
		
		// Finally, notify the listeners
		fireDocumentChangeAmendedEvent(oldDocChange, newDocChange);
	}
	
}
