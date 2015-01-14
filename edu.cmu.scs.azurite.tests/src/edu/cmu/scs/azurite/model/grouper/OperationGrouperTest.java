package edu.cmu.scs.azurite.model.grouper;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.Document;
import org.junit.Before;
import org.junit.Test;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;
import edu.cmu.scs.azurite.model.FileKey;
import edu.cmu.scs.azurite.model.OperationId;
import edu.cmu.scs.azurite.model.RuntimeHistoryManager;
import edu.cmu.scs.fluorite.commands.FileOpenCommand;
import edu.cmu.scs.fluorite.commands.ICommand;
import edu.cmu.scs.fluorite.commands.document.DocChange;
import edu.cmu.scs.fluorite.model.Events;
import edu.cmu.scs.fluorite.util.LogReader;

public class OperationGrouperTest {
	
	private static final int NUM_LEVELS = OperationGrouper.NUM_LEVELS;
	
	private RuntimeHistoryManager manager;
	private OperationGrouper grouper;
	
	@Before
	public void setUp() {
		manager = new RuntimeHistoryManager();
		grouper = manager.getOperationGrouper();
	}
	
	@Test
	public void testSampleLog1() {
		testHelper("data/Log2015-01-01-09-37-07-377.xml");
	}

	public void testHelper(String logPath) {
		LogReader reader = new LogReader();
		Events events = reader.readDocumentChanges(logPath);
		
		// Used for debugging convenience.
		@SuppressWarnings("unchecked")
		List<Integer>[] levels = new ArrayList[NUM_LEVELS];
		for (int i = 0; i < NUM_LEVELS; ++i) {
			levels[i] = new ArrayList<Integer>();
		}
		
		List<ICommand> commands = events.getCommands();
		for (int i = 0; i < commands.size(); ++i) {
			ICommand command = commands.get(i);
			if (!(command instanceof DocChange)) { continue; }
			
			DocChange docChange = (DocChange) command;
			
			if (docChange instanceof FileOpenCommand) {
				manager.activeFileChanged((FileOpenCommand) docChange);
				continue;
			}
			
			try {
				manager.documentChangeFinalized(docChange);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
//			count(levels);
			
//			System.out.println(docChange);
		}
		
		count(levels);
		
		// Flush all the changes.
		grouper.flushAllPendingChanges();
		
		printSummary(levels);
		
		output(logPath, events);
	}

	private void count(List<Integer>[] levels) {
		for (int level = 0; level < NUM_LEVELS; ++level) { levels[level].clear(); }
		for (RuntimeDC dc : manager.getRuntimeDocumentChanges()) {
			for (int level = 0; level < NUM_LEVELS; ++level) {
				int id = dc.getCollapseID(level);
				int lastid = levels[level].isEmpty() ? -1 : levels[level].get(levels[level].size() - 1);
				if (id != lastid) {
					levels[level].add(id);
				}
			}
		}
	}

	private void printSummary(List<Integer>[] levels) {
		for (int level = 0; level < NUM_LEVELS; ++level) {
			System.out.printf("[%d:%4d] ", level, levels[level].size());
			for (int i = 0; i < levels[level].size(); ++i) {
				if (i > 0) { System.out.print(", "); }
				System.out.print(levels[level].get(i));
			}
			System.out.println();
		}
	}
	
	private void output(String originalLogPath, Events events) {
		int slashIndex = -1;
		slashIndex = Math.max(slashIndex, originalLogPath.lastIndexOf('/'));
		slashIndex = Math.max(slashIndex, originalLogPath.lastIndexOf('\\'));
		
		String filename = slashIndex == -1 ? originalLogPath : originalLogPath.substring(slashIndex + 1);
		
		int dotIndex = filename.lastIndexOf('.');
		String filenameWithoutExt = dotIndex == -1 ? filename : filename.substring(0, dotIndex);
		
		// Create the output directory if it does not exist.
		new File("Output/").mkdir();
		
		for (int level = 0; level < NUM_LEVELS; ++level) {
			String outputFile = String.format("Output/%s_Level%d.xml", filenameWithoutExt, level);
			
			PrintWriter writer = null;
			try {
				writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(outputFile, false), "UTF-8"), true);
				
				try (BufferedReader in = new BufferedReader(new FileReader(originalLogPath))) {
					writer.println(in.readLine());
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				Map<FileKey, Document> snapshots = new HashMap<FileKey, Document>();
				DocChange pendingChange = null;
				FileKey key = null;
				for (ICommand command : events.getCommands()) {
					if (command instanceof FileOpenCommand) {
						if (pendingChange != null) {
							writer.print(pendingChange.persist());
							pendingChange.apply(snapshots.get(key));
							pendingChange = null;
						}
						
						FileOpenCommand foc = (FileOpenCommand) command;
						key = new FileKey(foc.getProjectName(), foc.getFilePath());
						
						if (foc.getSnapshot() != null) {
							snapshots.put(key, new Document(foc.getSnapshot()));
						}
						
						writer.print(command.persist());
					} else if (command instanceof DocChange) {
						if (pendingChange != null) {
							RuntimeDC dc = this.manager.filterDocumentChangeByIdWithoutCalculating(
									key,
									new OperationId(command.getSessionId(), command.getCommandIndex()));
							
							if (dc.getCollapseID(level) == command.getCommandIndex()) {
								writer.print(pendingChange.persist());
								pendingChange.apply(snapshots.get(key));
								pendingChange = (DocChange) command;
							} else {
								int id = pendingChange.getCommandIndex();
								pendingChange = DocChange.mergeChanges(pendingChange, (DocChange) command, snapshots.get(key));
								
								if (pendingChange != null) {
									pendingChange.setCommandIndex(id);
								}
							}
						} else {
							pendingChange = (DocChange) command;
						}
					}
				}
				
				if (pendingChange != null) {
					writer.print(pendingChange.persist());
				}
				
				writer.println("</Events>");
				
				writer.close();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (writer != null) {
					writer.close();
				}
			}
		}
	}
	
}
