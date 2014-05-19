package edu.cmu.scs.azurite.jface.dialogs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.LocationEvent;
import org.eclipse.swt.browser.LocationListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class AboutDialog extends Dialog {
	
	private static final String TITLE = "About Azurite";
	
	private static final String CLOSE_BUTTON_TEXT = "Close";
	
	private static final int CLOSE_BUTTON_ID = Dialog.OK;
	
	private static final int DIALOG_WIDTH = 600;
	private static final int DIALOG_HEIGHT = 500;

	public AboutDialog(Shell parentShell) {
		super(parentShell);
		
		setShellStyle(SWT.Close | SWT.BORDER | SWT.TITLE | SWT.RESIZE);
	}

	@Override
	protected Point getInitialLocation(Point initialSize) {
		Rectangle screenBounds = getShell().getDisplay().getBounds();
		return new Point(
				(screenBounds.width - initialSize.x) / 2,
				(screenBounds.height - initialSize.y) / 2);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(DIALOG_WIDTH, DIALOG_HEIGHT);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		getShell().setText(TITLE);
		getShell().setMinimumSize(DIALOG_WIDTH, DIALOG_HEIGHT);
		
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout());
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Browser browser = new Browser(comp, SWT.NONE);
		browser.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		// Extract the background color of the dialog, and use it as the body background color.
		Color bg = comp.getBackground();
		String rgb = "rgb(" + bg.getRed() + "," + bg.getGreen() + "," + bg.getBlue() + ")";
		
		try {
			URL indexUrl = FileLocator.toFileURL(
					Platform.getBundle("edu.cmu.scs.azurite").getEntry("/html/about.html"));
			
			// Instead of using the setURL method, read the file and attach the desired css here.
			BufferedReader in = new BufferedReader(new InputStreamReader(indexUrl.openStream()));
			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = in.readLine()) != null) {
				builder.append(line + "\n");
			}
			in.close();
			
			String additionalCSS = "<style>body { background: " + rgb + "; }</style>";
			
			browser.setText(builder.toString() + additionalCSS);
			
			// When the user clicks a link within the about page,
			// launch the system default browser instead.
			browser.addLocationListener(new LocationListener() {
				@Override
				public void changing(LocationEvent event) {
					URL url;
					try {
						url = new URL(event.location);
						PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(url);
						event.doit = false;
					} catch (MalformedURLException | PartInitException e) {
						e.printStackTrace();
					}
				}
				
				@Override
				public void changed(LocationEvent event) {
					// Do nothing here.
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return comp;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, CLOSE_BUTTON_ID, CLOSE_BUTTON_TEXT, true);
	}

}
