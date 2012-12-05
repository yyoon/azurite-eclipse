package edu.cmu.scs.azurite.jface.dialogs;

import java.util.List;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.LineNumberRulerColumn;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;

import edu.cmu.scs.azurite.model.undo.UndoAlternative;
import edu.cmu.scs.fluorite.util.Utilities;

@SuppressWarnings("restriction")
public class ConflictDialog extends TitleAreaDialog {
	
	private static final int MINIMUM_HEIGHT = 500;
	
	private static final String TEXT = "Selective Undo";
	private static final String TITLE = "Conflict Detected";
	private static final String DEFAULT_MESSAGE = "One or more conflicts were detected while performing selective undo.";

	private IDocument mOriginalDoc;
	private IDocument mCopyDoc;
	private int mOffset;
	private int mLength;
	private int mOriginalLength;
	private List<UndoAlternative> mAlternatives;
	
	private ISourceViewer mCodePreview;
	private Color mBackground;

	public ConflictDialog(Shell parent, IDocument originalDoc,
			int offset, int length, List<UndoAlternative> alternatives) {
		super(parent);
		
		mOriginalDoc = originalDoc;
		mCopyDoc = new Document(originalDoc.get());
		mOffset = offset;
		mLength = length;
		mOriginalLength = length;
		mAlternatives = alternatives;
		
		mBackground = new Color(parent.getDisplay(), 186, 205, 224);
		
		setHelpAvailable(false);
	}

	@Override
	public void create() {
		super.create();
		
		getShell().setText(TEXT);
		
		setTitle(TITLE);
		setMessage(DEFAULT_MESSAGE);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		// Use GridLayout.
		parent.setLayout(new GridLayout(3, true));
		
		// Code Preview Group
		createCodePreviewGroup(parent);
		
		// Alternatives Group
		createAlternativesGroup(parent);
		
		return parent;
	}

	private void createCodePreviewGroup(Composite parent) {
		Group groupPreview = new Group(parent, SWT.NONE);
		groupPreview.setText("Code Preview");
		
		FillLayout fillLayout = new FillLayout();
		fillLayout.marginWidth = 10;
		fillLayout.marginHeight = 10;
		groupPreview.setLayout(fillLayout);
		
		GridData gridData = new GridData(GridData.FILL, GridData.FILL, true, true, 2, 1);
		gridData.minimumHeight = MINIMUM_HEIGHT;
		groupPreview.setLayoutData(gridData);
		
		
		// Create the Java Source Viewer.
		CompositeRuler ruler = new CompositeRuler();
		
		LineNumberRulerColumn lnrc = new LineNumberRulerColumn();
		lnrc.setFont(Utilities.getFont());
		
		ruler.addDecorator(0, lnrc);
		
		mCodePreview = new SourceViewer(groupPreview,
				ruler, SWT.H_SCROLL | SWT.V_SCROLL);
		
		// Setting up the Java Syntax Highlighting
		JavaTextTools tools = JavaPlugin.getDefault().getJavaTextTools();
		tools.setupJavaDocumentPartitioner(mCopyDoc);

		JavaSourceViewerConfiguration config =
				new JavaSourceViewerConfiguration(
						tools.getColorManager(),
						JavaPlugin.getDefault().getCombinedPreferenceStore(),
						null, null);
		
		mCodePreview.configure(config);
		mCodePreview.setDocument(mCopyDoc);
		mCodePreview.setEditable(false);
		mCodePreview.getTextWidget().setFont(Utilities.getFont());
	}

	private void createAlternativesGroup(Composite parent) {
		Group groupAlternatives = new Group(parent, SWT.NONE);
		groupAlternatives.setText("Alternatives");
		
		RowLayout rowLayout = new RowLayout(SWT.VERTICAL);
		rowLayout.fill = true;
		rowLayout.pack = false;
		rowLayout.spacing = 10;
		rowLayout.marginWidth = 10;
		rowLayout.marginHeight = 10;
		
		groupAlternatives.setLayout(rowLayout);
		groupAlternatives.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true, 1, 1));
		
		// Add alternative Buttons
		// TODO replace the radio buttons with prettier custom buttons.
		for (int i = 0; i < mAlternatives.size(); ++i) {
			UndoAlternative alternative = mAlternatives.get(i);
			
			Button buttonAlternative = new Button(groupAlternatives, SWT.RADIO);
			buttonAlternative.setText(alternative.getResultingCode());
			buttonAlternative.setToolTipText(alternative.getDescription());
			
			if (i == 0) {
				buttonAlternative.setSelection(true);
			}
			
			final int currentIndex = i;
			buttonAlternative.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					selectAlternative(currentIndex);
				}
			});
		}
		
		selectAlternative(0);
	}

	private void selectAlternative(int index) {
		if (index < 0 || index >= mAlternatives.size()) {
			throw new IllegalArgumentException();
		}
		
		UndoAlternative alternative = mAlternatives.get(index);
		
		try {
			mCopyDoc.replace(mOffset, mLength, alternative.getResultingCode());
		} catch (BadLocationException e) {
			e.printStackTrace();
			return;
		}
		
		mLength = alternative.getResultingCode().length();
		
		// Highlight the area!
		StyleRange range = new StyleRange(mOffset, mLength, null, mBackground);
		mCodePreview.getTextWidget().setStyleRange(range);
	}
	
	@Override
	protected void cancelPressed() {
		super.cancelPressed();
	}

	@Override
	protected void okPressed() {
		try {
			mOriginalDoc.replace(mOffset, mOriginalLength, mCopyDoc.get(mOffset, mLength));
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		
		super.okPressed();
	}

	@Override
	protected boolean isResizable() {
		return true;
	}
	
}
