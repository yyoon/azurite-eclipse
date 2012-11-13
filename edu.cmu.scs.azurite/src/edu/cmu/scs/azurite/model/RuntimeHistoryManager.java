package edu.cmu.scs.azurite.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.ListenerList;

import edu.cmu.scs.fluorite.commands.BaseDocumentChangeEvent;
import edu.cmu.scs.fluorite.commands.FileOpenCommand;
import edu.cmu.scs.fluorite.model.DocumentChangeListener;
import edu.cmu.scs.fluorite.model.EventRecorder;
import edu.cmu.scs.azurite.commands.runtime.BaseRuntimeDocumentChange;

public class RuntimeHistoryManager implements DocumentChangeListener {
	
	/**
	 * @author YoungSeok Yoon
	 * 
	 * A FileKey class is composed of a project name and a file name.
	 */
	private class FileKey {
		
		private final String mProjectName;
		private final String mFilePath;
		
		public FileKey(String projectName, String filePath) {
			mProjectName = projectName;
			mFilePath = filePath;
		}
		
		public String getProjectName() {
			return mProjectName;
		}
		
		public String getFilePath() {
			return mFilePath;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof FileKey)) {
				return false;
			}
			
			return	mProjectName.equals(((FileKey)obj).mProjectName) &&
					mFilePath.equals(((FileKey)obj).mFilePath);
		}
		
		@Override
		public int hashCode() {
			if (mProjectName == null && mFilePath == null)
				return 0;
			else if (mProjectName == null)
				return mFilePath.hashCode();
			else if (mFilePath == null)
				return mProjectName.hashCode();
			else
				return this.mProjectName.hashCode() ^ this.mFilePath.hashCode();
		}
		
	}
	
	private Map<FileKey, List<BaseRuntimeDocumentChange>> mDocumentChanges;
	private FileKey mCurrentFileKey;
	
	private ListenerList mRuntimeDocumentChangeListeners;

	private List<Runnable> mScheduledTasks;
	
	private boolean mStarted;
	
	/**
	 * Basic constructor
	 */
	public RuntimeHistoryManager() {
		mDocumentChanges = new HashMap<FileKey, List<BaseRuntimeDocumentChange>>();
		mCurrentFileKey = null;
		
		mRuntimeDocumentChangeListeners = new ListenerList();
		
		mScheduledTasks = new ArrayList<Runnable>();
		
		mStarted = false;
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
	public void addRuntimeDocumentChangeListener(RuntimeDocumentChangeListener listener) {
		mRuntimeDocumentChangeListeners.add(listener);
	}
	
	/**
	 * Remove runtime document change listener
	 * @param listener
	 */
	public void removeRuntimeDocumentChangeListener(RuntimeDocumentChangeListener listener) {
		mRuntimeDocumentChangeListeners.remove(listener);
	}
	
	private void fireActiveFileChangedEvent(String projectName, String filePath) {
		for (Object listenerObj : mRuntimeDocumentChangeListeners.getListeners()) {
			((RuntimeDocumentChangeListener)listenerObj).activeFileChanged(projectName, filePath);
		}
	}
	
	private void fireDocumentChangedEvent(BaseRuntimeDocumentChange docChange) {
		for (Object listenerObj : mRuntimeDocumentChangeListeners.getListeners()) {
			((RuntimeDocumentChangeListener)listenerObj).runtimeDocumentChangeAdded(docChange);
		}
	}
	/**
	 * Returns all the runtime document changes associated with the current file.
	 * @return all the runtime document changes associated with the current file.
	 */
	public List<BaseRuntimeDocumentChange> getRuntimeDocumentChanges() {
		return getRuntimeDocumentChanges(getCurrentFileKey());
	}
	
	/**
	 * Returns all the runtime document changes associated with the given file.
	 * @param filePath Fullpath to the source file
	 * @return all the runtime document changes associated with the given file.
	 */
	public List<BaseRuntimeDocumentChange> getRuntimeDocumentChanges(FileKey key) {
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
	
	private FileKey getCurrentFileKey() {
		return mCurrentFileKey;
	}
	
	private void setCurrentFileKey(FileKey newFileKey) {
		mCurrentFileKey = newFileKey;
	}

	/**
	 * Simply updates the current file path.
	 */
	public void activeFileChanged(FileOpenCommand foc) {
		FileKey key = new FileKey(foc.getProjectName(), foc.getFilePath());
		setCurrentFileKey(key);
		
		if (!mDocumentChanges.containsKey(key)) {
			mDocumentChanges.put(key, new ArrayList<BaseRuntimeDocumentChange>());
		}
		
		fireActiveFileChangedEvent(key.getProjectName(), key.getFilePath());
	}

	public void documentChanged(BaseDocumentChangeEvent docChange) {
		// Do nothing here.
	}
	
	public void documentChangeFinalized(BaseDocumentChangeEvent docChange) {
		BaseRuntimeDocumentChange runtimeDocChange = BaseRuntimeDocumentChange.createRuntimeDocumentChange(docChange);
		
		List<BaseRuntimeDocumentChange> list = mDocumentChanges.get(getCurrentFileKey());
		for (BaseRuntimeDocumentChange existingDocChange : list) {
			runtimeDocChange.applyTo(existingDocChange);
		}
		
		list.add(runtimeDocChange);
		
		// Fire runtime document change event
		fireDocumentChangedEvent(runtimeDocChange);
	}
	
	public List<BaseRuntimeDocumentChange> filterDocumentChangesByIds(List<Integer> ids) {
		if (ids == null) {
			throw new IllegalArgumentException();
		}
		
		List<BaseRuntimeDocumentChange> list = mDocumentChanges.get(getCurrentFileKey());
		if (list == null) {
			throw new IllegalStateException();
		}
		
		List<BaseRuntimeDocumentChange> result = new ArrayList<BaseRuntimeDocumentChange>();
		for (BaseRuntimeDocumentChange docChange : list) {
			if (ids.contains(docChange.getOriginal().getCommandIndex())) {
				result.add(docChange);
			}
		}
		
		return result;
	}
}
