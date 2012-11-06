package edu.cmu.scs.azurite.compare;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;

import edu.cmu.scs.fluorite.commands.BaseDocumentChangeEvent;
import edu.cmu.scs.fluorite.commands.Delete;
import edu.cmu.scs.fluorite.commands.FileOpenCommand;
import edu.cmu.scs.fluorite.commands.ICommand;
import edu.cmu.scs.fluorite.commands.Insert;
import edu.cmu.scs.fluorite.commands.Replace;

public class HistoryCompareItem extends BaseCompareItem {
	
	public HistoryCompareItem(String name, long time, IEditorPart editor, BaseDocumentChangeEvent untilHere) {
		super(name, time, false);
		
		this.mEditor = editor;
		mUntilHere = untilHere;
		
		if (this.mEditor != null) {
			IEditorInput input = editor.getEditorInput();
			if (input instanceof IFileEditorInput) {
				try {
					IFileEditorInput fileInput = (IFileEditorInput)input;
					IFile file = fileInput.getFile();
					IProject project = file.getProject();
					this.mProjectName = project.getName();
					this.mFilePath = fileInput.getFile().getLocation().toOSString();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public HistoryCompareItem(String name, IEditorPart editor, BaseDocumentChangeEvent untilHere) {
		this(name, 0, editor, untilHere);
	}
	
	private IEditorPart mEditor;
	
	private String mFilePath;
	private String mProjectName;
	
	private BaseDocumentChangeEvent mUntilHere;

	public InputStream getContents() throws CoreException {
		StringBuffer content = new StringBuffer();
		boolean currentFile = false;
		
//		List<ICommand> docChanges = EventRecorder.getInstance().getDocumentChangeCommands();
		List<ICommand> docChanges = new ArrayList<ICommand>();
		for (ICommand command : docChanges) {
			if (!(command instanceof BaseDocumentChangeEvent)) {
				continue;
			}
			
			if (command instanceof FileOpenCommand) {
				FileOpenCommand foc = (FileOpenCommand) command;
				if (!isSameFile(foc)) {
					currentFile = false;
					continue;
				}
				
				// This one has a snapshot. Replace the entire content with the snapshot
				if (foc.getSnapshot() != null) {
					content = new StringBuffer(foc.getSnapshot()); 
				}
				
				currentFile = true;
			} else if (currentFile) {
				// Now, the command should be one of Delete / Insert / Replace.
				// TODO: Make sure this works with different sets of new line characters.
				if (command instanceof Delete) {
					Delete del = (Delete) command;
					content.delete(del.getOffset(), del.getOffset() + del.getLength());
				} else if (command instanceof Insert) {
					Insert ins = (Insert) command;
					content.insert(ins.getOffset(), ins.getText());
				} else if (command instanceof Replace) {
					Replace rep = (Replace) command;
					content.replace(rep.getOffset(), rep.getOffset() + rep.getLength(), rep.getInsertedText());
				} else {
					// Must not be called.
					assert(false);
				}
			}
			
			if (command == mUntilHere) {
				break;
			}
		}
		
		return new ByteArrayInputStream(content.toString().getBytes());
	}
	
	private boolean isSameFile(FileOpenCommand fileOpenCommand) {
		// TODO: Only consider the relative path.
		// Also consider the case where the file has been moved into another package.
		
		if (fileOpenCommand == null || fileOpenCommand.getFilePath() == null ||
				fileOpenCommand.getProjectName() == null ||
				mFilePath == null || mProjectName == null) {
			return false;
		}
		
		return fileOpenCommand.getFilePath().equals(mFilePath) && fileOpenCommand.getProjectName().equals(mProjectName);
	}

}
