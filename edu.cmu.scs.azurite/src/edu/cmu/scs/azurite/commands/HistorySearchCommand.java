package edu.cmu.scs.azurite.commands;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.IEditorPart;

import edu.cmu.scs.fluorite.commands.AbstractCommand;
import edu.cmu.scs.fluorite.commands.ICommand;
import edu.cmu.scs.fluorite.model.EventRecorder;

public class HistorySearchCommand extends AbstractCommand {
	
	public HistorySearchCommand(String searchText, boolean caseSensitive, boolean selectedCode, boolean currentSession) {
		mSearchText = searchText;
		mCaseSensitive = caseSensitive;
		mSelectedCode = selectedCode;
		mCurrentSession = currentSession;
	}
	
	private String mSearchText;
	private boolean mCaseSensitive;
	private boolean mSelectedCode;
	private boolean mCurrentSession;

	@Override
	public boolean execute(IEditorPart target) {
		return false;
	}

	@Override
	public void dump() {
	}

	@Override
	public Map<String, String> getAttributesMap() {
		Map<String, String> result = new HashMap<String, String>();
		result.put("caseSensitive", Boolean.toString(mCaseSensitive));
		result.put("selectedCode", Boolean.toString(mSelectedCode));
		result.put("currentSession", Boolean.toString(mCurrentSession));
		
		return result;
	}

	@Override
	public Map<String, String> getDataMap() {
		Map<String, String> result = new HashMap<String, String>();
		result.put("searchText", mSearchText);
		
		return result;
	}

	@Override
	public String getCommandType() {
		return "HistorySearchCommand";
	}

	@Override
	public String getName() {
		return "HistorySearch";
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public String getCategory() {
		return EventRecorder.MacroCommandCategory;
	}

	@Override
	public String getCategoryID() {
		return EventRecorder.MacroCommandCategoryID;
	}

	@Override
	public boolean combine(ICommand anotherCommand) {
		return false;
	}

}
