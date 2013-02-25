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
import edu.cmu.scs.fluorite.commands.BaseDocumentChangeEvent;
import edu.cmu.scs.fluorite.commands.FileOpenCommand;
import edu.cmu.scs.fluorite.commands.ICommand;
import edu.cmu.scs.fluorite.model.DocumentChangeListener;
import edu.cmu.scs.fluorite.model.EventRecorder;
import edu.cmu.scs.fluorite.model.Events;

public class RuntimeHistoryManager implements DocumentChangeListener {
	
	// used for keeping track of the last index of the DC
	private Map<FileKey, Integer> mNextIndexToApply;
	
	private Map<FileKey, List<RuntimeDC>> mDocumentChanges;
	private FileKey mCurrentFileKey;
	
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
	}

	public void scheduleTask(Runnable runnable) {
		if (mStarted) {
			runnable.run();
		}
		else {
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

	@Override
	public void activeFileChanged(FileOpenCommand foc) {
		activeFileChanged(foc.getProjectName(), foc.getFilePath(), foc.getSnapshot());
	}

	/**
	 * Simply updates the current file path.
	 */
	public void activeFileChanged(String projectName, String filePath, String snapshot) {
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
		RuntimeDC runtimeDocChange = RuntimeDC.createRuntimeDocumentChange(docChange);
		
		List<RuntimeDC> list = getRuntimeDocumentChanges(key);
		if (list == null) {
			throw new IllegalArgumentException("Key does not exist!");
		}
		list.add(runtimeDocChange);
		
		// Fire runtime document change event
		fireRuntimeDCAddedEvent(runtimeDocChange);
	}
	
	public List<RuntimeDC> filterDocumentChangesByIds(final List<Integer> ids) {
		if (ids == null) {
			throw new IllegalArgumentException();
		}
		
		return filterDocumentChanges(new IRuntimeDCFilter() {
			@Override
			public boolean filter(RuntimeDC runtimeDC) {
				return ids.contains(runtimeDC.getOriginal().getCommandIndex());
			}
		});
	}
	
	public List<RuntimeDC> filterDocumentChangesByRegion(final int startOffset, final int endOffset) {
		return filterDocumentChanges(new IRuntimeDCFilter() {
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
		if (filter == null) {
			throw new IllegalArgumentException();
		}
		
		// Lazy-evaluation of the dynamic segments!
		FileKey fileKey = getCurrentFileKey();
		List<RuntimeDC> list = calculateDynamicSegments(fileKey);
		
		// Then filter the results.
		List<RuntimeDC> result = new ArrayList<RuntimeDC>();
		for (RuntimeDC dc : list) {
			if (filter.filter(dc)) {
				result.add(dc);
			}
		}
		
		return result;
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
				if (!(command instanceof BaseDocumentChangeEvent)) {
					continue;
				}
				
				BaseDocumentChangeEvent docChange = (BaseDocumentChangeEvent)command;
				if (docChange instanceof FileOpenCommand) {
					activeFileChanged((FileOpenCommand)docChange);
				} else {
					// Doc Change generated by Diff. (see PastHistoryManager)
					if (docChange instanceof IDiffDC) {
						addRuntimeDCFromOriginalDC(docChange,
								((IDiffDC) docChange).getFileKey());
					}
					// Normal Doc Changes.
					else {
						addRuntimeDCFromOriginalDC(docChange);
					}
				}
			}
		}

		// Notify the listeners (mainly, Timeline View)
		// Timeline view only needs the events newly added.
		firePastLogsReadEvent(listEvents);
	}
	
}
