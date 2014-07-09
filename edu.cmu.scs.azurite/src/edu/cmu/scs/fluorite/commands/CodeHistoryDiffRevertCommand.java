package edu.cmu.scs.fluorite.commands;

import java.util.Map;

import org.eclipse.ui.IEditorPart;

import edu.cmu.scs.fluorite.model.EventRecorder;

public class CodeHistoryDiffRevertCommand extends AbstractCommand {

	@Override
	public boolean execute(IEditorPart target) {
		return false;
	}

	@Override
	public void dump() {
	}

	@Override
	public Map<String, String> getAttributesMap() {
		return null;
	}

	@Override
	public Map<String, String> getDataMap() {
		return null;
	}

	@Override
	public String getCommandType() {
		return "CodeHistoryDiffRevertCommand";
	}

	@Override
	public String getName() {
		return "CodeHistoryDiffRevert";
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
