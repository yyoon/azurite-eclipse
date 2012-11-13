package edu.cmu.scs.azurite.views;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import edu.cmu.scs.azurite.commands.runtime.BaseRuntimeDocumentChange;
import edu.cmu.scs.azurite.model.RuntimeDocumentChangeListener;
import edu.cmu.scs.azurite.model.RuntimeHistoryManager;
import edu.cmu.scs.azurite.model.undo.SelectiveUndoEngine;
import edu.cmu.scs.fluorite.model.EventRecorder;

public class TimelineViewPart extends ViewPart implements RuntimeDocumentChangeListener {

	private static final String PATH = "C:/Users/asder/Desktop/timeline/";
//	private static final String PATH = "D:/timeline_ver6/";

	private Browser browser;
	
	@Override
	public void createPartControl(Composite parent) {
		
		browser = new Browser(parent, SWT.NONE);
		new ReadFileFunction(browser, "readLog");
		new UndoFunction(browser, "doUndo");
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
                System.out.println("Page loaded");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // Execute JavaScript in the browser
                browser.execute("call();");
                
                EventRecorder.getInstance().scheduleTask(new Runnable() {

					@Override
					public void run() {
						browser.execute("set_start_timestamp("
								+ EventRecorder.getInstance().getStartTimestamp()
								+ ");");
					}
                	
                });
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
	
	class ReadFileFunction extends BrowserFunction {

		public ReadFileFunction(Browser browser, String name) {
			super(browser, name);
		}

		@Override
		public Object function(Object[] arguments) {
			return fileToString();
		}

	}

    public String fileToString() {
    	InputStream    fis = null;
		BufferedReader br;
		String         line;
		StringBuilder builder = new StringBuilder();
		
		try {
			fis = new FileInputStream(PATH + "Log2012-09-24-10-41-36-725.xml");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		br = new BufferedReader(new InputStreamReader(fis, Charset.forName("UTF-8")));
		try {
			while ((line = br.readLine()) != null) {
			    builder.append(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) { }
		}
		
		
		
		Writer out = null;
		try {
			out = new BufferedWriter(new OutputStreamWriter(
				    new FileOutputStream(PATH + "test.js"), "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			try {
			    try {
			    	System.out.println("asdsadsadasd");
					out.write(builder.toString());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} finally {
			    try {
					out.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		
		return builder.toString();
    }

	@Override
	public void activeFileChanged(String projectName, String filePath) {
		String executeStr = String.format("add_file('%1$s');",
				filePath.replace('\\', '/'));	// avoid escaping..
		browser.execute(executeStr);
	}

	@Override
	public void runtimeDocumentChangeAdded(BaseRuntimeDocumentChange docChange) {
		String executeStr = String.format("add_block(%1$d, %2$d, %3$d, %4$d);",
				docChange.getOriginal().getCommandIndex(),
				docChange.getOriginal().getTimestamp(),
				docChange.getOriginal().getTimestamp2(),
				docChange.getTypeIndex());
		browser.execute(executeStr);
	}
}
