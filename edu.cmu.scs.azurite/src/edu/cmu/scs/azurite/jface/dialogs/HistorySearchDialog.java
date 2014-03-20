package edu.cmu.scs.azurite.jface.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import edu.cmu.scs.azurite.plugin.Activator;
import edu.cmu.scs.azurite.preferences.Initializer;

public class HistorySearchDialog extends Dialog {
	private Text mTextSearch;
	
	private Button mButtonCaseSensitive;
	
	private Button mButtonSelectedCode;
	
	private Button mButtonCurrentSession;

	private String mInitialSearchString;

	private boolean mCreatedDialogArea;
	
	private boolean mCaseSensitive;
	private boolean mScopeSelectedCode;
	private boolean mCurrentSession;
	private String mSearchText;

	public static final int SEARCH = IDialogConstants.OK_ID;
	public static final int CLOSE = IDialogConstants.CANCEL_ID;

	private static HistorySearchDialog _instance = null;

	public static HistorySearchDialog getInstance() {
		return _instance;
	}

	public HistorySearchDialog(Shell shell, String initialSearchString) {
		super(shell);
		
		setShellStyle(SWT.CLOSE | SWT.MODELESS | SWT.BORDER | SWT.TITLE
				| SWT.RESIZE);
		
		mInitialSearchString = initialSearchString;
		mCreatedDialogArea = false;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		getShell().setText("History Search");
		
		Composite comp = new Composite(parent, SWT.None);
		comp.setLayout(new GridLayout());
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));

		Composite stringComp = new Composite(comp, SWT.NONE);
		stringComp.setLayout(new GridLayout(2, false));
		stringComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label label = new Label(stringComp, SWT.NONE);
		label.setText("Search &text:");

		mTextSearch = new Text(stringComp, SWT.BORDER);
		mTextSearch.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true,
				false));

		stringComp.pack();

		Composite dirScopeComp = new Composite(comp, SWT.NONE);
		dirScopeComp.setLayout(new GridLayout(2, true));
		dirScopeComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Group locScopeGroup = new Group(dirScopeComp, SWT.NONE);
		locScopeGroup.setText("Location scope");
		locScopeGroup.setLayout(new GridLayout());
		locScopeGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Button buttonAll = new Button(locScopeGroup, SWT.RADIO);
		buttonAll.setText("All");
		
		mButtonSelectedCode = new Button(locScopeGroup, SWT.RADIO);
		mButtonSelectedCode.setText("Selected code");
		
		Group sessionScopeGroup = new Group(dirScopeComp, SWT.NONE);
		sessionScopeGroup.setText("Session scope");
		sessionScopeGroup.setLayout(new GridLayout());
		sessionScopeGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Button buttonAllSessions = new Button(sessionScopeGroup, SWT.RADIO);
		buttonAllSessions.setText("All sessions");
		
		mButtonCurrentSession = new Button(sessionScopeGroup, SWT.RADIO);
		mButtonCurrentSession.setText("Latest session");

		Group optionsGroup = new Group(dirScopeComp, SWT.NONE);
		optionsGroup.setText("Options");
		optionsGroup.setLayout(new GridLayout(2, false));
		optionsGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false,
				2, 1));

		mButtonCaseSensitive = new Button(optionsGroup, SWT.CHECK);
		mButtonCaseSensitive.setText("&Case sensitive");

		dirScopeComp.pack();

		// populate fields
		if (mInitialSearchString != null) {
			mTextSearch.setText(mInitialSearchString);
		}

		IPreferenceStore prefStore = Activator.getDefault()
				.getPreferenceStore();

		if (prefStore.getBoolean(Initializer.Pref_HistorySearchSelectedCode)) {
			mButtonSelectedCode.setSelection(true);
		} else {
			buttonAll.setSelection(true);
		}

		if (prefStore.getBoolean(Initializer.Pref_HistorySearchCurrentSession)) {
			mButtonCurrentSession.setSelection(true);
		} else {
			buttonAllSessions.setSelection(true);
		}

		mButtonCaseSensitive.setSelection(prefStore
				.getBoolean(Initializer.Pref_HistorySearchCaseSensitive));

		mTextSearch.selectAll();

		mCreatedDialogArea = true;

		_instance = this;

		return comp;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		if (!mCreatedDialogArea) {
			return;
		}

		createButton(parent, SEARCH, "Search", true);
		createButton(parent, CLOSE, "Close", false);
	}

	@Override
	public boolean close() {
		_instance = null;
		
		mCaseSensitive = mButtonCaseSensitive.getSelection();
		mScopeSelectedCode = mButtonSelectedCode.getSelection();
		mCurrentSession = mButtonCurrentSession.getSelection();
		mSearchText = mTextSearch.getText();

		IPreferenceStore prefStore = Activator.getDefault()
				.getPreferenceStore();
		if (prefStore != null) {
			prefStore.setValue(Initializer.Pref_HistorySearchSelectedCode, mScopeSelectedCode);
			prefStore.setValue(Initializer.Pref_HistorySearchCurrentSession, mCurrentSession);
			prefStore.setValue(Initializer.Pref_HistorySearchCaseSensitive, mCaseSensitive);
		}

		return super.close();
	}
	
	public boolean isCaseSensitive() {
		return mCaseSensitive;
	}
	
	public boolean isScopeSelectedCode() {
		return mScopeSelectedCode;
	}
	
	public boolean isCurrentSession() {
		return mCurrentSession;
	}
	
	public String getSearchText() {
		return mSearchText;
	}

}
