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
import edu.cmu.scs.azurite.model.FileKey;
import edu.cmu.scs.azurite.model.OperationId;
import edu.cmu.scs.azurite.model.RuntimeDCListener;
import edu.cmu.scs.azurite.model.RuntimeHistoryManager;
import edu.cmu.scs.azurite.model.undo.SelectiveUndoEngine;
import edu.cmu.scs.fluorite.commands.BaseDocumentChangeEvent;
import edu.cmu.scs.fluorite.commands.Delete;
import edu.cmu.scs.fluorite.commands.FileOpenCommand;
import edu.cmu.scs.fluorite.commands.ICommand;
import edu.cmu.scs.fluorite.commands.Insert;
import edu.cmu.scs.fluorite.commands.Replace;
import edu.cmu.scs.fluorite.model.EventRecorder;
import edu.cmu.scs.fluorite.model.Events;

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
		new LogFunction(browser, BROWSER_FUNC_PREFIX + "log");
		
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
			
			try {
				Object[] selected = (Object[])arguments[0];
				
				// Convert everything into integer.
				List<OperationId> ids = new ArrayList<OperationId>();
				for (Object element : selected) {
					if (!(element instanceof Object[])) { continue; }
					
					Object[] idComponents = (Object[])element;
					if (idComponents.length == 2 &&
							idComponents[0] instanceof Number &&
							idComponents[1] instanceof Number) {
						Number sid = (Number)idComponents[0];
						Number id = (Number)idComponents[1];
						
						ids.add(new OperationId(sid.longValue(), id.longValue()));
					}
				}
				
				SelectiveUndoEngine.getInstance().doSelectiveUndo(
						RuntimeHistoryManager.getInstance().filterDocumentChangesByIds(ids));
				
				return "ok";
			}
			catch (Exception e) {
				e.printStackTrace();
				return "fail";
			}
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
                    		
                    		scrollToEnd();
            			}
            		});
            	}
            });
            
			return "ok";
		}
	}
	
	class LogFunction extends BrowserFunction {
		
		public LogFunction(Browser browser, String name) {
			super(browser, name);
		}
		
		@Override
		public Object function(Object[] arguments) {
			System.out.println( arguments[0] );
			
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

	private void addOperation(BaseDocumentChangeEvent docChange, boolean scroll) {
		String executeStr = String.format("addOperation(%1$d, %2$d, %3$d, %4$d, %5$f, %6$f, %7$d, %8$s);",
				docChange.getSessionId(),
				docChange.getCommandIndex(),
				docChange.getTimestamp(),
				docChange.getTimestamp2(),
				docChange.getY1(),
				docChange.getY2(),
				getTypeIndex(docChange),
				Boolean.toString(scroll));
		browser.execute(executeStr);
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
		long timestamp = EventRecorder.getInstance().getStartTimestamp();
		
		StringBuffer buffer = new StringBuffer();
		buffer.append("addSelectionsByIds([");
		
		if (!ids.isEmpty()) {
			buffer.append(timestamp);
		}
		for (int i = 1; i < ids.size(); ++i) {
			buffer.append(", " + timestamp);
		}
		buffer.append("], [");
		
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
	
	public void activateFirebugLite() {
        browser.setUrl("javascript:(function(F,i,r,e,b,u,g,L,I,T,E){if(F.getElementById(b))return;E=F[i+'NS']&&F.documentElement.namespaceURI;E=E?F[i+'NS'](E,'script'):F[i]('script');E[r]('id',b);E[r]('src',I+g+T);E[r](b,u);(F[e]('head')[0]||F[e]('body')[0]).appendChild(E);E=new%20Image;E[r]('src',I+L);})(document,'createElement','setAttribute','getElementsByTagName','FirebugLite','3','releases/lite/1.3/firebug-lite.js','releases/lite/latest/skin/xp/sprite.png','https://getfirebug.com/','#startOpened');");
	}

	@Override
	public void pastLogsRead(List<Events> listEvents) {
		if (listEvents == null) {
			throw new IllegalArgumentException();
		}
		
		if (listEvents.isEmpty()) { 
			return;
		}
		
		// Update the start timestamp.
		browser.execute("setStartTimestamp("
				+ listEvents.get(0).getStartTimestamp() + ", true, true);");

		// Add all the things.
		for (Events events : listEvents) {
			// Add the operations
			for (ICommand command : events.getCommands()) {
				if (!(command instanceof BaseDocumentChangeEvent)) {
					continue;
				}
				
				BaseDocumentChangeEvent docChange = (BaseDocumentChangeEvent)command;
				if (docChange instanceof FileOpenCommand) {
					FileOpenCommand foc = (FileOpenCommand)docChange;
					activeFileChanged(foc.getProjectName(), foc.getFilePath());
				} else {
					addOperation(docChange, false);
				}
			}
		}
		
		// Update the data.
		browser.execute("adjustData();");
	}
	
	private void scrollToEnd() {
		browser.execute("showUntil(global.maxTimestamp);");
	}
	
}
