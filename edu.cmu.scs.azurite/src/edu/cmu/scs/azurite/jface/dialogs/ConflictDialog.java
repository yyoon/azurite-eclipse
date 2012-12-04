package edu.cmu.scs.azurite.jface.dialogs;

import java.util.List;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.ui.text.JavaSourceViewerConfiguration;
import org.eclipse.jdt.ui.text.JavaTextTools;
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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import edu.cmu.scs.azurite.model.undo.UndoAlternative;
import edu.cmu.scs.fluorite.util.Utilities;

@SuppressWarnings("restriction")
public class ConflictDialog extends Shell {
	
	private static final int WIDTH = 1024;
	private static final int HEIGHT = 768;
	
	private static final int BUTTON_WIDTH = 92;
	private static final int BUTTON_HEIGHT = 25;

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
		super(parent, SWT.SHELL_TRIM | SWT.APPLICATION_MODAL);
		
		mOriginalDoc = originalDoc;
		mCopyDoc = new Document(originalDoc.get());
		mOffset = offset;
		mLength = length;
		mOriginalLength = length;
		mAlternatives = alternatives;
		
		mBackground = new Color(getDisplay(), 186, 205, 224);
		
		setImage(parent.getImage());
		setText("Selective Undo - Conflict Detected");
		setSize(WIDTH, HEIGHT);
		
		// Enable ESC key.
		enableEscapeKey();
		
		// Use GridLayout.
		setLayout(new GridLayout(3, true));
		
		// Code Preview Group
		createCodePreviewGroup();
		
		// Alternatives Group
		createAlternativesGroup();
		
		// Buttons
		createButtonPanel();
	}
	
	@Override
	protected void checkSubclass() {
		// Do nothing here so that I can subclass without getting an Exception.
		// super.checkSubclass();
	}

	private void createCodePreviewGroup() {
		Group groupPreview = new Group(this, SWT.NONE);
		groupPreview.setText("Code Preview");
		groupPreview.setLayout(new FillLayout());
		groupPreview.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true, 2, 1));
		
		
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

	private void createAlternativesGroup() {
		Group groupAlternatives = new Group(this, SWT.NONE);
		groupAlternatives.setText("Alternatives");
		
		RowLayout rowLayout = new RowLayout(SWT.VERTICAL);
		rowLayout.fill = true;
		rowLayout.pack = false;
		rowLayout.spacing = 10;
		
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

	private void createButtonPanel() {
		Composite buttonPanel = new Composite(this, SWT.NONE);
		RowLayout rowLayout = new RowLayout();
		rowLayout.pack = false;
		buttonPanel.setLayout(rowLayout);
		buttonPanel.setLayoutData(new GridData(GridData.END, GridData.CENTER, true, false, 3, 1));
		
		Button buttonOK = new Button(buttonPanel, SWT.PUSH);
		buttonOK.setText("OK");
		buttonOK.setLayoutData(new RowData(BUTTON_WIDTH, BUTTON_HEIGHT));
		buttonOK.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleOK();
			}
		});
		setDefaultButton(buttonOK);
		
		Button buttonCancel = new Button(buttonPanel, SWT.PUSH);
		buttonCancel.setText("Cancel");
		buttonCancel.setLayoutData(new RowData(BUTTON_WIDTH, BUTTON_HEIGHT));
		buttonCancel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleCancel();
			}
		});
	}

	private void enableEscapeKey() {
		addListener (SWT.Traverse, new Listener () {
			public void handleEvent (Event event) {
				switch (event.detail) {
					case SWT.TRAVERSE_ESCAPE:
						handleCancel();
						event.detail = SWT.TRAVERSE_NONE;
						event.doit = false;
						break;
				}
			}
		});
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
	
	private void handleCancel() {
		// Simply close this dialog without doing anything.
		close();
	}
	
	private void handleOK() {
		// Apply the change.
		try {
			mOriginalDoc.replace(mOffset, mOriginalLength, mCopyDoc.get(mOffset, mLength));
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		
		// And then close this dialog.
		close();
	}
}
