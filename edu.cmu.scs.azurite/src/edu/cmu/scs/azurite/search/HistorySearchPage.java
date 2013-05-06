package edu.cmu.scs.azurite.search;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.fieldassist.ComboContentAdapter;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.FindReplaceDocumentAdapterContentProposalProvider;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.search.internal.core.text.PatternConstructor;
import org.eclipse.search.internal.ui.SearchMessages;
import org.eclipse.search.ui.ISearchPage;
import org.eclipse.search.ui.ISearchPageContainer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.fieldassist.ContentAssistCommandAdapter;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;
import edu.cmu.scs.azurite.commands.runtime.Segment;
import edu.cmu.scs.azurite.model.IRuntimeDCFilter;
import edu.cmu.scs.azurite.model.OperationId;
import edu.cmu.scs.azurite.model.RuntimeHistoryManager;
import edu.cmu.scs.azurite.model.undo.Chunk;
import edu.cmu.scs.azurite.model.undo.SelectiveUndoEngine;
import edu.cmu.scs.azurite.ui.handlers.HandlerUtilities;
import edu.cmu.scs.azurite.views.TimelineViewPart;
import edu.cmu.scs.fluorite.commands.BaseDocumentChangeEvent;
import edu.cmu.scs.fluorite.model.EventRecorder;
import edu.cmu.scs.fluorite.util.Utilities;

@SuppressWarnings("restriction")
public class HistorySearchPage extends DialogPage implements ISearchPage {

	private boolean fFirstTime= true;
	private boolean fIsCaseSensitive;
	private boolean fIsRegExSearch;
	private boolean fScopeSelectedCode;
	private boolean fCurrentSession;
	
	private Combo fPattern;
	private Button fIsCaseSensitiveCheckbox;
	private Button fIsRegExCheckbox;
	private CLabel fStatusLabel;
	
	// Custom scopes
	private Button fButtonAll;
	private Button fButtonSelectedCode;
	
	private Button fButtonAllSessions;
	private Button fButtonCurrentSession;
	
	private ISearchPageContainer fContainer;

	private ContentAssistCommandAdapter fPatternFieldContentAssist;

	@Override
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);
		
		Composite result = new Composite(parent, SWT.NONE);
		result.setFont(parent.getFont());
		GridLayout layout = new GridLayout(2, false);
		result.setLayout(layout);
		
		createTextPatternControls(result);
		
		Label separator = new Label(result, SWT.NONE);
		separator.setVisible(false);
		GridData data = new GridData(GridData.FILL, GridData.FILL, false, false, 2, 1);
		data.heightHint = convertHeightInCharsToPixels(1) / 3;
		separator.setLayoutData(data);
		
		createCustomScopeControls(result);
		
		setControl(result);
		Dialog.applyDialogFont(result);
	}
	
	private void createCustomScopeControls(Composite parent) {
		Composite dirScopeComp = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, true);
		layout.marginLeft = -5;
		dirScopeComp.setLayout(layout);
		dirScopeComp.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false, 2, 1));

		Group locScopeGroup = new Group(dirScopeComp, SWT.NONE);
		locScopeGroup.setText("Location scope");
		locScopeGroup.setLayout(new GridLayout());
		locScopeGroup.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false, 1, 1));

		fButtonAll = new Button(locScopeGroup, SWT.RADIO);
		fButtonAll.setText("All");
		
		fButtonSelectedCode = new Button(locScopeGroup, SWT.RADIO);
		fButtonSelectedCode.setText("Selected code");
		fButtonSelectedCode.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fScopeSelectedCode = fButtonSelectedCode.getSelection();
			}
		});
		
		ISelection selection= getSelection();
		if (selection instanceof ITextSelection && !selection.isEmpty() && ((ITextSelection)selection).getLength() > 0) {
			fButtonSelectedCode.setSelection(true);
		}
		else {
			fButtonAll.setSelection(true);
		}
		
		Group sessionScopeGroup = new Group(dirScopeComp, SWT.NONE);
		sessionScopeGroup.setText("Session scope");
		sessionScopeGroup.setLayout(new GridLayout());
		sessionScopeGroup.setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false, 1, 1));
		
		fButtonAllSessions = new Button(sessionScopeGroup, SWT.RADIO);
		fButtonAllSessions.setText("All sessions");
		
		fButtonCurrentSession = new Button(sessionScopeGroup, SWT.RADIO);
		fButtonCurrentSession.setText("Current session");
		fButtonCurrentSession.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				fCurrentSession = fButtonCurrentSession.getSelection();
			}
		});
		fButtonCurrentSession.setSelection(true);

//		dirScopeComp.pack();
	}

	private void createTextPatternControls(Composite parent) {
		// grid layout with 2 columns
		
		// Info text
		Label label = new Label(parent, SWT.LEAD);
		label.setText(SearchMessages.SearchPage_containingText_text);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 2, 1));
		label.setFont(parent.getFont());
		
		// Pattern combo
		fPattern= new Combo(parent, SWT.SINGLE | SWT.BORDER);
		// Not done here to prevent page from resizing
		// fPattern.setItems(getPreviousSearchPatterns());
		fPattern.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				// TODO implement this similar to TextSearchPage.
				// handleWidgetSelected();
				updateOKStatus();
			}
		});
		// add some listeners for regex syntax checking
		fPattern.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				updateOKStatus();
			}
		});
		fPattern.setFont(parent.getFont());
		GridData data= new GridData(GridData.FILL, GridData.FILL, true, false, 1, 1);
		data.widthHint= convertWidthInCharsToPixels(50);
		fPattern.setLayoutData(data);

		ComboContentAdapter contentAdapter= new ComboContentAdapter();
		FindReplaceDocumentAdapterContentProposalProvider findProposer= new FindReplaceDocumentAdapterContentProposalProvider(true);
		fPatternFieldContentAssist= new ContentAssistCommandAdapter(
				fPattern,
				contentAdapter,
				findProposer,
				ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS,
				new char[0],
				true);
		fPatternFieldContentAssist.setEnabled(fIsRegExSearch);

		fIsCaseSensitiveCheckbox= new Button(parent, SWT.CHECK);
		fIsCaseSensitiveCheckbox.setText(SearchMessages.SearchPage_caseSensitive);
		fIsCaseSensitiveCheckbox.setSelection(fIsCaseSensitive);
		fIsCaseSensitiveCheckbox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fIsCaseSensitive= fIsCaseSensitiveCheckbox.getSelection();
			}
		});
		fIsCaseSensitiveCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		fIsCaseSensitiveCheckbox.setFont(parent.getFont());

		// Text line which explains the special characters
		fStatusLabel= new CLabel(parent, SWT.LEAD);
		fStatusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		fStatusLabel.setFont(parent.getFont());
		fStatusLabel.setAlignment(SWT.LEFT);
		fStatusLabel.setText(SearchMessages.SearchPage_containingText_hint);

		// RegEx checkbox
		fIsRegExCheckbox= new Button(parent, SWT.CHECK);
		fIsRegExCheckbox.setText(SearchMessages.SearchPage_regularExpression);
		fIsRegExCheckbox.setSelection(fIsRegExSearch);

		fIsRegExCheckbox.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fIsRegExSearch= fIsRegExCheckbox.getSelection();
				updateOKStatus();

				// TODO implement this.
				// writeConfiguration();
				fPatternFieldContentAssist.setEnabled(fIsRegExSearch);
			}
		});
		fIsRegExCheckbox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		fIsRegExCheckbox.setFont(parent.getFont());
		
	}

	final void updateOKStatus() {
		boolean regexStatus= validateRegex();
		getContainer().setPerformActionEnabled(regexStatus);
	}

	private boolean validateRegex() {
		if (fIsRegExCheckbox.getSelection()) {
			try {
				PatternConstructor.createPattern(fPattern.getText(), fIsCaseSensitive, true);
			} catch (PatternSyntaxException e) {
				String locMessage= e.getLocalizedMessage();
				int i= 0;
				while (i < locMessage.length() && "\n\r".indexOf(locMessage.charAt(i)) == -1) { //$NON-NLS-1$
					i++;
				}
				statusMessage(true, locMessage.substring(0, i)); // only take first line
				return false;
			}
			statusMessage(false, ""); //$NON-NLS-1$
		} else {
			statusMessage(false, SearchMessages.SearchPage_containingText_hint);
		}
		return true;
	}

	private void statusMessage(boolean error, String message) {
		fStatusLabel.setText(message);
		if (error)
			fStatusLabel.setForeground(JFaceColors.getErrorText(fStatusLabel.getDisplay()));
		else
			fStatusLabel.setForeground(null);
	}

	/**
	 * Sets the search page's container.
	 * @param container the container to set
	 */
	@Override
	public void setContainer(ISearchPageContainer container) {
		fContainer= container;
	}

	private ISearchPageContainer getContainer() {
		return fContainer;
	}

	@Override
	public boolean performAction() {
		String searchText = fPattern.getText();
		String searchTextLowerCase = searchText.toLowerCase();

		// Find the editor part and retrieve the attached IDocument.
		IEditorReference[] editorRefs = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
		IEditorPart editor = null;
		IDocument doc = null;
		for (IEditorReference ref : editorRefs) {
			try {
				if (ref.getEditorInput() != null &&
						ref.getEditorInput().equals(getContainer().getActiveEditorInput())) {
					editor = ref.getEditor(false);
					if (editor != null) {
						doc = Utilities.getDocument(editor);
					}
					
					break;
				}
			}
			catch (PartInitException e) {
				e.printStackTrace();
			}
		}
		
		if (editor == null) { return false; }
		if (doc == null) { return false; }
		
		// TODO move the getOperationsInSelectedRegion to somewhere else.
		List<RuntimeDC> dcs = fScopeSelectedCode ? HandlerUtilities.getOperationsInSelectedRegion()
				: RuntimeHistoryManager.getInstance().filterDocumentChanges(new IRuntimeDCFilter() {
					@Override
					public boolean filter(RuntimeDC runtimeDC) {
						return true;
					}
				});
		if (dcs == null || dcs.isEmpty()) { return true; }
		
		// Only current session?
		if (fCurrentSession) {
			List<RuntimeDC> newDCs = new ArrayList<RuntimeDC>();
			for (RuntimeDC dc : dcs) {
				if (dc.getOriginal().getSessionId() == EventRecorder.getInstance().getStartTimestamp()) {
					newDCs.add(dc);
				}
			}
			
			dcs = newDCs;
		}
		
		// There was no document changes in the selected scope.
		if (dcs.isEmpty()) { return true; }
		
		
		// FIXME This selective undo is ridiculously inefficient, but maybe OK for now.
		// TODO This is almost a cloned code from CodeHistoryDiffViewer.java Modularize.
		// TODO This is the third clone (HistorySearchHandler -> HistorySearchPage)
		List<RuntimeDC> resultDCs = new ArrayList<RuntimeDC>();
		
		// determine the code scope.
		int selectionStart = 0;
		int selectionEnd = doc.getLength();
		
		if (fScopeSelectedCode) {
			ITextSelection selection = HandlerUtilities.getSelectedRegion();
			selectionStart = selection.getOffset();
			selectionEnd = selection.getOffset() + selection.getLength();
		}
		
		int selectionLength = selectionEnd - selectionStart;
		String codeContent = doc.get();
		String selectedCode = codeContent.substring(selectionStart,  selectionEnd);
		
		for (int version = 1; version <= dcs.size(); ++version) {
			String resultingCode = null;
			
			if (version == dcs.size()) {
				resultingCode = codeContent;
			}
			else {
				// Get the previous versions by performing undo.
				List<RuntimeDC> subList = dcs.subList(version, dcs.size());
				Chunk chunk = new Chunk();
				for (RuntimeDC dc : subList) {
					for (Segment segment : dc.getAllSegments()) {
						if (segment.isDeletion()) {
							if (selectionStart < segment.getOffset() && segment.getOffset() < selectionEnd) {
								chunk.add(segment);
							}
						}
						else {
							if (segment.getOffset() < selectionEnd && segment.getEndOffset() > selectionStart) {
								chunk.add(segment);
							}
						}
					}
				}
				Collections.sort(chunk, Segment.getLocationComparator());
				
				int startOffset = chunk.getStartOffset();
				int endOffset = chunk.getEndOffset();
				String initialContent = codeContent.substring(startOffset, endOffset);
				
				String undoResult = SelectiveUndoEngine.getInstance()
						.doSelectiveUndoChunkWithoutConflicts(chunk, initialContent);
				
				StringBuilder historyContent = new StringBuilder(selectedCode);
				historyContent.replace(
						Math.max(startOffset - selectionStart, 0),
						Math.min(endOffset - selectionStart, selectionLength),
						undoResult);
				
				resultingCode = historyContent.toString();
			}
			
			// resultingCode should not be null.
			if (resultingCode == null) { 
				throw new IllegalStateException();
			}
			
			// Check if the resulting code contains the provided search text.
			// There may be locale problem?
			// http://javapapers.com/core-java/javas-tolowercase-has-got-a-surprise-for-you/
			if (fIsCaseSensitive) {
				if (resultingCode.contains(searchText)) {
					resultDCs.add(dcs.get(version - 1));
				}
			}
			else {
				if (resultingCode.toLowerCase().contains(searchTextLowerCase)) {
					resultDCs.add(dcs.get(version - 1));
				}
			}
		}
		
		// Now the resultDCs contains all the searched operations.
		
		// Extract the ids.
		List<OperationId> ids = new ArrayList<OperationId>();
		for (RuntimeDC dc : resultDCs) {
			BaseDocumentChangeEvent original = dc.getOriginal();
			ids.add(new OperationId(original.getSessionId(), original.getCommandIndex()));
		}
		
		// Send this to the timeline view, if it's available.
		TimelineViewPart timelineViewPart = TimelineViewPart.getInstance();
		if (timelineViewPart != null) {
			timelineViewPart.addSelection(ids, true);
		}
		
		return true;
	}

	@Override
	public void setVisible(boolean visible) {
		if (visible && fPattern != null) {
			if (fFirstTime) {
				fFirstTime= false;
				// Set item and text here to prevent page from resizing
				// TODO implement these.
//				fPattern.setItems(getPreviousSearchPatterns());
//				fExtensions.setItems(fPreviousExtensions);
//				if (fExtensions.getItemCount() == 0) {
//					loadFilePatternDefaults();
//				}
				if (!initializePatternControl()) {
					fPattern.select(0);
					// TODO implement this
					// handleWidgetSelected();
				}
			}
			fPattern.setFocus();
		}
		updateOKStatus();

		IEditorInput editorInput= getContainer().getActiveEditorInput();
		getContainer().setActiveEditorCanProvideScopeSelection(editorInput != null && editorInput.getAdapter(IFile.class) != null);

		super.setVisible(visible);
	}

	private boolean initializePatternControl() {
		ISelection selection= getSelection();
		if (selection instanceof ITextSelection && !selection.isEmpty() && ((ITextSelection)selection).getLength() > 0) {
			String text= ((ITextSelection) selection).getText();
			if (text != null) {
				if (fIsRegExSearch)
					fPattern.setText(FindReplaceDocumentAdapter.escapeForRegExPattern(text));
				else
					fPattern.setText(insertEscapeChars(text));

/*				if (fPreviousExtensions.length > 0) {
					fExtensions.setText(fPreviousExtensions[0]);
				} else {
/					String extension= getExtensionFromEditor();
					if (extension != null)
						fExtensions.setText(extension);
					else
						fExtensions.setText("*"); //$NON-NLS-1$
//				}
*/				return true;
			}
		}
		return false;
	}

	private String insertEscapeChars(String text) {
		if (text == null || text.equals("")) //$NON-NLS-1$
			return ""; //$NON-NLS-1$
		StringBuffer sbIn= new StringBuffer(text);
		BufferedReader reader= new BufferedReader(new StringReader(text));
		int lengthOfFirstLine= 0;
		try {
			lengthOfFirstLine= reader.readLine().length();
		} catch (IOException ex) {
			return ""; //$NON-NLS-1$
		}
		StringBuffer sbOut= new StringBuffer(lengthOfFirstLine + 5);
		int i= 0;
		while (i < lengthOfFirstLine) {
			char ch= sbIn.charAt(i);
			if (ch == '*' || ch == '?' || ch == '\\')
				sbOut.append("\\"); //$NON-NLS-1$
			sbOut.append(ch);
			i++;
		}
		return sbOut.toString();
	}

	private ISelection getSelection() {
		return fContainer.getSelection();
	}

}
