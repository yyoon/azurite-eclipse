package edu.cmu.scs.fluorite.commands;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ui.IEditorPart;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.cmu.scs.fluorite.commands.AbstractCommand;
import edu.cmu.scs.fluorite.commands.ICommand;
import edu.cmu.scs.fluorite.model.EventRecorder;

public class HistorySearchCommand extends AbstractCommand {
	
	private static final String SEARCH_TEXT_TAG = "searchText";
	private static final String CASE_SENSITIVE_ATTR = "caseSensitive";
	private static final String SELECTED_CODE_ATTR = "selectedCode";
	private static final String CURRENT_SESSION_ATTR = "currentSession";

	public HistorySearchCommand() {
	}
	
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
	public void createFrom(Element commandElement) {
		super.createFrom(commandElement);
		
		Attr attr = null;
		String value = null;
		NodeList nodeList = null;
		
		if ((attr = commandElement.getAttributeNode(CASE_SENSITIVE_ATTR)) != null) {
			mCaseSensitive = Boolean.parseBoolean(attr.getValue());
		}
		
		if ((attr = commandElement.getAttributeNode(SELECTED_CODE_ATTR)) != null) {
			mSelectedCode = Boolean.parseBoolean(attr.getValue());
		}
		
		if ((attr = commandElement.getAttributeNode(CURRENT_SESSION_ATTR)) != null) {
			mCurrentSession = Boolean.parseBoolean(attr.getValue());
		}
		
		if ((nodeList = commandElement.getElementsByTagName(SEARCH_TEXT_TAG)).getLength() > 0) {
			Node textNode = nodeList.item(0);
			value = textNode.getTextContent();
			mSearchText = value.equals("null") ? null : value;
		}
		else {
			mSearchText = null;
		}
	}

	@Override
	public boolean execute(IEditorPart target) {
		return false;
	}

	@Override
	public void dump() {
		// Do nothing
	}

	@Override
	public Map<String, String> getAttributesMap() {
		Map<String, String> result = new HashMap<String, String>();
		result.put(CASE_SENSITIVE_ATTR, Boolean.toString(mCaseSensitive));
		result.put(SELECTED_CODE_ATTR, Boolean.toString(mSelectedCode));
		result.put(CURRENT_SESSION_ATTR, Boolean.toString(mCurrentSession));
		
		return result;
	}

	@Override
	public Map<String, String> getDataMap() {
		Map<String, String> result = new HashMap<String, String>();
		result.put(SEARCH_TEXT_TAG, mSearchText);
		
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
