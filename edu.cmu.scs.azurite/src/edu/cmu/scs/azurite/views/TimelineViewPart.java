package edu.cmu.scs.azurite.views;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.part.ViewPart;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;
import edu.cmu.scs.azurite.model.RuntimeDCListener;
import edu.cmu.scs.azurite.model.RuntimeHistoryManager;
import edu.cmu.scs.azurite.model.RuntimeHistoryManager.FileKey;
import edu.cmu.scs.azurite.model.undo.SelectiveUndoEngine;
import edu.cmu.scs.fluorite.commands.BaseDocumentChangeEvent;
import edu.cmu.scs.fluorite.commands.Delete;
import edu.cmu.scs.fluorite.commands.Insert;
import edu.cmu.scs.fluorite.commands.Replace;
import edu.cmu.scs.fluorite.model.EventRecorder;

public class TimelineViewPart extends ViewPart implements RuntimeDCListener {

	private Browser browser;
	
	private static TimelineViewPart me = null;
	private static String BROWSER_FUNC_PREFIX = "__AZURITE__";
	
	/**
	 * Not a singleton pattern per se.
	 * This object keeps the reference of itself upon GUI element creation.
	 * Provided just for convenience.
	 * @return The timelineviewpart's object. Could be null, if the view is not shown.
	 */
	public static TimelineViewPart getInstance() {
		return me;
	}
	
	@Override
	public void createPartControl(Composite parent) {
		
		me = this;
		
		browser = new Browser(parent, SWT.NONE);
		new UndoFunction(browser, BROWSER_FUNC_PREFIX + "selectiveUndo");
		new InitializeFunction(browser, BROWSER_FUNC_PREFIX + "initialize");
		
		// Retrieve the full URL of /html/index.html in our project.
		try {
			URL indexUrl = FileLocator.toFileURL(Platform.getBundle(
					"edu.cmu.scs.azurite").getEntry("/html/index.html"));
			browser.setUrl(indexUrl.toString());
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		browser.addProgressListener(new ProgressListener() {
            
            public void completed(ProgressEvent event) {
            }
            
            public void changed(ProgressEvent event) {
            }
        });
		
		// Register to the EventRecorder.
		RuntimeHistoryManager.getInstance().addRuntimeDocumentChangeListener(this);
	}

	@Override
	public void dispose() {
		RuntimeHistoryManager.getInstance().removeRuntimeDocumentChangeListener(this);
		
		me = null;
		
		super.dispose();
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub

	}


	class UndoFunction extends BrowserFunction {

		public UndoFunction(Browser browser, String name) {
			super(browser, name);
		}

		@Override
		public Object function(Object[] arguments) {
			if (arguments == null || arguments.length != 1 || arguments[0] == null) {
				return "fail";
			}
			
			Object[] selected = (Object[])arguments[0];
			
			// Convert everything into integer.
			List<Integer> ids = new ArrayList<Integer>();
			for (Object element : selected) {
				if (element instanceof Number) {
					ids.add(((Number)element).intValue());
				}
			}
			
			SelectiveUndoEngine.getInstance().doSelectiveUndo(
					RuntimeHistoryManager.getInstance().filterDocumentChangesByIds(ids));
			
/*			for(int i = 0; i < selected.length; i++){
				System.out.print(selected[i].toString() + ", ");
			}
			System.out.println("");*/
			
			return "ok";
		}

	}
	
	class InitializeFunction extends BrowserFunction {
		
		public InitializeFunction(Browser browser, String name) {
			super(browser, name);
		}
		
		@Override
		public Object function(Object[] arguments) {
            
			// Set the start Timestamp.
            EventRecorder.getInstance().scheduleTask(new Runnable() {
				public void run() {
					browser.execute("setStartTimestamp("
							+ EventRecorder.getInstance().getStartTimestamp()
							+ ");");
				}
            });
            
            // Read the existing runtime document changes.
            RuntimeHistoryManager.getInstance().scheduleTask(new Runnable() {
            	public void run() {
            		Display.getDefault().asyncExec(new Runnable() {
            			public void run() {
                    		RuntimeHistoryManager manager = RuntimeHistoryManager.getInstance(); 
                    		for (FileKey key : manager.getFileKeys()) {
                    			addFile(key.getFilePath());
                    			for (RuntimeDC dc : manager.getRuntimeDocumentChanges(key)) {
                    				addOperation(dc.getOriginal(), false);
                    			}
                    		}
                    		
                    		redraw();
            			}
            		});
            	}
            });
            
			return "ok";
		}
	}
	
	@Override
	public void activeFileChanged(String projectName, String filePath) {
		if (projectName == null || filePath == null) {
			// Some non-text file is opened maybe?
			return;
		}
		
		addFile(filePath);
	}

	@Override
	public void runtimeDCAdded(RuntimeDC docChange) {
		// Do nothing here. Now the blocks are added at real time.
	}
	
	@Override
	public void documentChangeAdded(BaseDocumentChangeEvent docChange) {
		addOperation(docChange, true);
	}

	private void addFile(String filePath) {
		String executeStr = String.format("addFile('%1$s');",
				filePath.replace('\\', '/'));	// avoid escaping..
		browser.execute(executeStr);
	}

	private void addOperation(BaseDocumentChangeEvent docChange, boolean drawImmediately) {
		String executeStr = String.format("addOperation(%1$d, %2$d, %3$d, %4$d, %5$s);",
				docChange.getCommandIndex(),
				docChange.getTimestamp(),
				docChange.getTimestamp2(),
				getTypeIndex(docChange),
				drawImmediately);
		browser.execute(executeStr);
	}
	
	private void redraw() {
		Display.getDefault().asyncExec(new Runnable() {
			public void run() { browser.execute("redraw();"); }
		});
	}
	
	private int getTypeIndex(BaseDocumentChangeEvent docChange) {
		if (docChange instanceof Insert) {
			return 0;
		}
		else if (docChange instanceof Delete) {
			return 1;
		}
		else if (docChange instanceof Replace) {
			return 2;
		}
		else {
			return -1;
		}
	}

	@Override
	public void documentChangeUpdated(BaseDocumentChangeEvent docChange) {
		String executeStr = String.format(
				"updateOperationTimestamp2(%1$d, %2$d);",
				docChange.getCommandIndex(), docChange.getTimestamp2());
		browser.execute(executeStr);
	}

	/**
	 * Add selections to the timeline view. Must be called from the SWT EDT.
	 * @param ids list of ids to be selected
	 * @param clearSelection indicates whether the existing selections should be discarded before adding new selections.
	 */
	public void addSelection(List<Integer> ids, boolean clearSelection) {
		StringBuffer buffer = new StringBuffer();
		buffer.append("addSelectionsByIds([");
		
		Iterator<Integer> it = ids.iterator();
		if (it.hasNext()) {
			Integer first = it.next();
			buffer.append(first.toString());
			
			while (it.hasNext()) {
				buffer.append(", " + it.next());
			}
		}
		
		buffer.append("], " + Boolean.toString(clearSelection) + ");");
		
		browser.execute(buffer.toString());
	}
}
