package edu.cmu.scs.azurite.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.zest.core.widgets.Graph;
import org.eclipse.zest.core.widgets.GraphConnection;
import org.eclipse.zest.core.widgets.GraphNode;
import org.eclipse.zest.core.widgets.ZestStyles;
import org.eclipse.zest.layouts.algorithms.HorizontalLayoutAlgorithm;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;
import edu.cmu.scs.azurite.commands.runtime.RuntimeDelete;
import edu.cmu.scs.azurite.commands.runtime.RuntimeInsert;
import edu.cmu.scs.azurite.commands.runtime.RuntimeReplace;
import edu.cmu.scs.azurite.jface.widgets.NonMovableGraph;
import edu.cmu.scs.azurite.model.RuntimeDCListener;
import edu.cmu.scs.azurite.model.RuntimeHistoryManager;
import edu.cmu.scs.azurite.model.undo.SelectiveUndoEngine;
import edu.cmu.scs.fluorite.commands.BaseDocumentChangeEvent;
import edu.cmu.scs.fluorite.model.Events;

public class GraphViewPart extends ViewPart implements RuntimeDCListener, MouseListener {
	
	private Graph mGraph;
	private List<GraphNode> mNodes;
	private GraphNode mLastNode;
	
	private Menu mContextMenu;
	private MenuItem mSelectiveUndoMenuItem;
	
	private final Color COLOR_INSERT;
	private final Color COLOR_DELETE;
	private final Color COLOR_REPLACE;
	
	public GraphViewPart() {
		super();
		
		Display display = Display.getDefault();
		COLOR_INSERT = new Color(display, 150, 200, 150);
		COLOR_DELETE = new Color(display, 200, 150, 175);
		COLOR_REPLACE = new Color(display, 200, 150, 50);
	}

	@Override
	public void createPartControl(Composite parent) {
		mGraph = new NonMovableGraph(parent, SWT.NONE);
		mGraph.setNodeStyle(ZestStyles.NODES_NO_ANIMATION);
		mGraph.setLayoutAlgorithm(new HorizontalLayoutAlgorithm(), true);
		mGraph.addMouseListener(this);
		mGraph.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				updateEnabled();
			}
		});
		
		mContextMenu = new Menu(parent.getShell(), SWT.POP_UP);
		mSelectiveUndoMenuItem = new MenuItem(mContextMenu, SWT.PUSH);
		mSelectiveUndoMenuItem.setText("Undo Selected Operations");
		mSelectiveUndoMenuItem.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				selectiveUndo();
			}
		});
		updateEnabled();
		
		mGraph.setMenu(mContextMenu);
		
		mNodes = new ArrayList<GraphNode>();
		
		// Register to the EventRecorder.
		RuntimeHistoryManager.getInstance().addRuntimeDocumentChangeListener(this);
		
		// Make sure that this runs after the EventRecorder initialized.
		RuntimeHistoryManager.getInstance().scheduleTask(new Runnable() {
			public void run() {
				// Make sure these run in the SWT thread.
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
//						updateCurrentFileInfo();
						updateGraph();
					}
				});
			}
		});
	}

	private void selectiveUndo() {
		@SuppressWarnings("rawtypes")
		List selection = mGraph.getSelection();
		
		List<RuntimeDC> selectedChanges =
				new ArrayList<RuntimeDC>();
		
		for (Object obj : selection) {
			GraphNode node = (GraphNode) obj;
			RuntimeDC docChange = (RuntimeDC)node.getData();
			
			selectedChanges.add(docChange);
		}
		
		SelectiveUndoEngine.getInstance().doSelectiveUndo(
				selectedChanges.toArray(new RuntimeDC[0]));
	}

	@Override
	public void dispose() {
		super.dispose();
		
		RuntimeHistoryManager.getInstance().removeRuntimeDocumentChangeListener(this);
	}

	@Override
	public void setFocus() {
	}
	
	public void documentChangeFinalized(final BaseDocumentChangeEvent docChange) {
	}
	
	private void updateGraph() {
		List<RuntimeDC> docChanges = RuntimeHistoryManager.getInstance()
				.getRuntimeDocumentChanges();
		
		clearGraph();
		
		// Add new nodes.
		for (RuntimeDC docChange : docChanges) {
			appendNode(docChange, false);
		}
		
		mGraph.applyLayout();
	}

	private void clearGraph() {
		// Deselect everything (otherwise, an error may occur)
		mGraph.setSelection(null);
		
		// Dispose all the connections
		while (mGraph.getConnections().size() > 0) {
			((GraphConnection)mGraph.getConnections().get(0)).dispose();
		}
		
		// Dispose all the nodes
		for (GraphNode node : mNodes) {
			node.dispose();
		}
		
		mLastNode = null;
	}
	
/*	private void updateCurrentFileInfo() {
		IEditorPart editor = Utilities.getActiveEditor();
		
		if (editor != null) {
			IEditorInput input = editor.getEditorInput();
			if (input instanceof IFileEditorInput) {
				try {
					IFileEditorInput fileInput = (IFileEditorInput)input;
					IFile file = fileInput.getFile();
					IProject project = file.getProject();
					project.getName();
					fileInput.getFile().getLocation().toOSString();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
*/	
	private void appendNode(RuntimeDC docChange) {
		appendNode(docChange, true);
	}
	
	private void appendNode(RuntimeDC docChange, boolean applyLayoutImmediately) {
		// Determine the type (Insert / Delete / Replace)
		String typeName = docChange.getClass().getSimpleName();
		if (typeName.startsWith("Runtime")) {
			typeName = typeName.substring("Runtime".length());
		}
		
		GraphNode node = new GraphNode(mGraph, SWT.NONE, typeName);
		node.setData(docChange);
		setColor(node);
		setTooltip(node);
		
		if (mLastNode != null) {
			new GraphConnection(mGraph, ZestStyles.CONNECTIONS_SOLID, mLastNode, node);
		}
		
		mNodes.add(node);
		mLastNode = node;
		
		if (applyLayoutImmediately) {
			mGraph.applyLayout();
		}
	}
	
	private void setColor(GraphNode node) {
		RuntimeDC docChange = (RuntimeDC)node.getData();
		if (docChange == null) return;

		if (docChange instanceof RuntimeInsert) {
			node.setBackgroundColor(COLOR_INSERT);
		}
		else if (docChange instanceof RuntimeDelete) {
			node.setBackgroundColor(COLOR_DELETE);
		}
		else if (docChange instanceof RuntimeReplace) {
			node.setBackgroundColor(COLOR_REPLACE);
		}
	}
	
	private void setTooltip(GraphNode node) {
		RuntimeDC docChange = (RuntimeDC)node.getData();
		if (docChange == null) return;
		
		if (docChange instanceof RuntimeInsert) {
			RuntimeInsert insert = (RuntimeInsert)docChange;
			IFigure tooltip = new Label(insert.getOriginal().getText());
			node.setTooltip(tooltip);
		}
		else if (docChange instanceof RuntimeDelete) {
			RuntimeDelete delete = (RuntimeDelete)docChange;
			IFigure tooltip = new Label(delete.getOriginal().getText());
			node.setTooltip(tooltip);
		}
		else if (docChange instanceof RuntimeReplace) {
			RuntimeReplace replace = (RuntimeReplace)docChange;
			String message = "-\n" + replace.getOriginal().getDeletedText() +
					"\n+\n" + replace.getOriginal().getInsertedText();
			IFigure tooltip = new Label(message);
			node.setTooltip(tooltip);
		}
	}

	public void mouseDoubleClick(MouseEvent e) {
		@SuppressWarnings("rawtypes")
		List selection = mGraph.getSelection();
		if (selection.size() != 1) {
			// Do nothing for now.
			return;
		}
		
		GraphNode selectedNode = (GraphNode)selection.get(0);
		launchCompareUI((BaseDocumentChangeEvent)selectedNode.getData());
	}

	public void mouseDown(MouseEvent e) {
	}

	public void mouseUp(MouseEvent e) {
	}
	
	private void launchCompareUI(BaseDocumentChangeEvent docChange) {
/*		IEditorPart activeEditor = Utilities.getActiveEditor();
		
		CompareInput input = new CompareInput(
				new HistoryCompareItem(
						"Old Version",
						activeEditor,
						docChange),
				new SimpleCompareItem(
						"Current Version",
						Utilities.getDocument(activeEditor).get(),
						true)
				);
		
		CompareUI.openCompareEditor(input);
		
		CompareInput.setLastCompareInput(input);
*/	}

	private void updateEnabled() {
		mSelectiveUndoMenuItem.setEnabled( mGraph.getSelection().size() > 0 );
	}

	@Override
	public void activeFileChanged(String projectName, String filePath) {
		updateGraph();
	}

	@Override
	public void runtimeDCAdded(final RuntimeDC docChange) {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				appendNode(docChange);
			}
		});
	}

	@Override
	public void documentChangeAdded(BaseDocumentChangeEvent docChange) {
	}

	@Override
	public void documentChangeUpdated(BaseDocumentChangeEvent docChange) {
	}

	@Override
	public void pastLogsRead(List<Events> listEvents) {
	}

}
