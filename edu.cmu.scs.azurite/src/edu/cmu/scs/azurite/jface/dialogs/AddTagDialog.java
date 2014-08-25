package edu.cmu.scs.azurite.jface.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class AddTagDialog extends Dialog {
	
	private Text textComment;
	private String comment;
	private String title;

	public AddTagDialog(Shell parentShell, String title) {
		super(parentShell);

		this.comment = "";
		this.title = title;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, OK, "OK", true);
		createButton(parent, CANCEL, "Cancel", false);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		getShell().setText(this.title);

		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout());
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Label label = new Label(comp, SWT.NONE);
		label.setText("(Optional) Leave a message for the tag.");

		this.textComment = new Text(comp, SWT.BORDER);
		this.textComment.setSize(200, -1);
		this.textComment.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		return comp;
	}

	@Override
	protected void buttonPressed(int buttonId) {
		setReturnCode(buttonId);
		
		this.comment = this.textComment.getText();
		close();
	}

	@Override
	protected void cancelPressed() {
		setReturnCode(CANCEL);
		close();
	}

	public String getComment() {
		return this.comment;
	}

}
