package edu.cmu.scs.azurite.jface.dialogs;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.compare.CompareUI;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;
import edu.cmu.scs.azurite.jface.widgets.ChunksTreeViewer;
import edu.cmu.scs.azurite.model.FileKey;
import edu.cmu.scs.azurite.model.OperationId;
import edu.cmu.scs.azurite.model.RuntimeHistoryManager;
import edu.cmu.scs.azurite.model.undo.Chunk;
import edu.cmu.scs.azurite.model.undo.SelectiveUndoEngine;
import edu.cmu.scs.azurite.plugin.Activator;
import edu.cmu.scs.azurite.views.TimelineViewPart;

public class InteractiveSelectiveUndoDialog extends TitleAreaDialog {
	
	private static final int MINIMUM_CHUNKS_HEIGHT = 100;
	
	private static final int MARGIN_WIDTH = 10;
	private static final int MARGIN_HEIGHT = 10;
	
	private static final int SPACING = 10;
	
	private static final String TEXT = "Azurite - Interactive Selective Undo";
	private static final String TITLE = "Interactive Selective Undo";
	private static final String DEFAULT_MESSAGE = "The preview will be updated as you select/deselect rectangles from the timeline.";
	
	private static final String CHUNKS_TITLE = "Changes to be performed";
	
	protected static final String NEXT_CHANGE_ID = "edu.cmu.scs.azurite.nextChange";
	protected static final String NEXT_CHANGE_TOOLTOP = "Select Next Chunk";
	
	protected static final String PREVIOUS_CHANGE_ID = "edu.cmu.scs.azurite.previousChange";
	protected static final String PREVIOUS_CHANGE_TOOLTIP = "Select Previous Chunk";

	private class NextChunk extends Action {
		public NextChunk() {
			setId(NEXT_CHANGE_ID);
			setImageDescriptor(CompareUI.DESC_ETOOL_NEXT);
			setDisabledImageDescriptor(CompareUI.DESC_DTOOL_NEXT);
			setHoverImageDescriptor(CompareUI.DESC_CTOOL_NEXT);
			setToolTipText(NEXT_CHANGE_TOOLTOP);
		}
		public void run() {
			chunksTreeViewer.revealNext();
		}
	}

	private class PreviousChunk extends Action {
		public PreviousChunk() {
			setId(PREVIOUS_CHANGE_ID);
			setImageDescriptor(CompareUI.DESC_ETOOL_PREV);
			setDisabledImageDescriptor(CompareUI.DESC_DTOOL_PREV);
			setHoverImageDescriptor(CompareUI.DESC_CTOOL_PREV);
			setToolTipText(PREVIOUS_CHANGE_TOOLTIP);
		}
		public void run() {
			chunksTreeViewer.revealPrevious();
		}
	}
	
	private class ChunksContentProvider implements ITreeContentProvider {

		@Override
		public void dispose() {
			// Do nothing.
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// Do nothing.
		}

		// Assume that inputElement is typed as List<Chunk>
		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof List<?>) {
				@SuppressWarnings("unchecked")
				List<Chunk> chunkList = (List<Chunk>) inputElement;
				
				return chunkList.toArray(new Chunk[chunkList.size()]);
			} else {
				return null;
			}
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			return null;
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return false;
		}
		
	}
	
	private class ChunksLabelProvider extends LabelProvider {
		
		private Image exclamationIcon;
		
		public ChunksLabelProvider() {
			super();
			
			this.exclamationIcon = Activator.getImageDescriptor("icons/error.png").createImage();
		}

		@Override
		public Image getImage(Object element) {
			// TODO return the exclamation icon when there is a conflict to be resolved in this chunk.
			return this.exclamationIcon;
//			return null;
		}

		@Override
		public String getText(Object element) {
			if (element instanceof Chunk) {
				Chunk chunk = (Chunk) element;
				FileKey fileKey = chunk.getBelongsTo();
				
				Path filePath = Paths.get(fileKey.getFilePath());
				return filePath.getFileName().toString();
			} else {
				return super.getText(element);
			}
		}
		
		@Override
		public void dispose() {
			super.dispose();
			
			this.exclamationIcon.dispose();
		}
		
	}
	
	private ChunksTreeViewer chunksTreeViewer;

	public InteractiveSelectiveUndoDialog(Shell parent) {
		super(parent);
		// TODO Auto-generated constructor stub
		
		// Make the dialog modeless.
		setShellStyle(SWT.CLOSE | SWT.MODELESS | SWT.BORDER | SWT.TITLE);
		setBlockOnOpen(false);
		
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
		Composite composite = createMainArea(parent);
		
		// Involved Operations Group
		createChunksTreeViewer(composite);
		
		return composite;
	}

	private Composite createMainArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		
		// Use GridLayout.
		GridLayout gridLayout = new GridLayout(3, false);
		gridLayout.marginWidth = MARGIN_WIDTH;
		gridLayout.marginHeight = 0;
		gridLayout.marginTop = MARGIN_HEIGHT;
		gridLayout.horizontalSpacing = SPACING;
		gridLayout.verticalSpacing = SPACING;
		composite.setLayout(gridLayout);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		return composite;
	}

	@SuppressWarnings("restriction")
	private void createChunksTreeViewer(Composite parent) {
		org.eclipse.jdt.internal.ui.util.ViewerPane chunksPane =
				new org.eclipse.jdt.internal.ui.util.ViewerPane(parent, SWT.BORDER | SWT.FLAT);
		this.chunksTreeViewer = new ChunksTreeViewer(chunksPane, SWT.NONE);
		
		// Layout the chunks pane within the dialog area.
		GridData gridData = new GridData(GridData.FILL, GridData.FILL, true, true, 3, 1);
		gridData.minimumHeight = MINIMUM_CHUNKS_HEIGHT;
		chunksPane.setLayoutData(gridData);
		
		// Set the label text.
		chunksPane.setText(CHUNKS_TITLE);
		
		// Setup the toolbar.
		ToolBarManager tbm = chunksPane.getToolBarManager();
		tbm.add(new NextChunk());
		tbm.add(new PreviousChunk());
		
		tbm.update(true);
		
		// Setup the model for the tree viewer.
		this.chunksTreeViewer.setContentProvider(createChunksTreeContentProvider());
		this.chunksTreeViewer.setLabelProvider(createChunksTreeLabelProvider());
		this.chunksTreeViewer.setComparator(createChunksTreeComparator());
		this.chunksTreeViewer.addSelectionChangedListener(createSelectionChangedListener());
		
		// Set the initial input
		setChunksTreeViewerInput();
		
		// Set the content of the ViewerPane to the tree-control.
		chunksPane.setContent(this.chunksTreeViewer.getControl());
	}

	private ITreeContentProvider createChunksTreeContentProvider() {
		return new ChunksContentProvider();
	}

	private ILabelProvider createChunksTreeLabelProvider() {
		return new ChunksLabelProvider();
	}

	private ViewerComparator createChunksTreeComparator() {
		return new ViewerComparator() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				// TODO implement the comparator.
				return 0;
			}
		};
	}

	private ISelectionChangedListener createSelectionChangedListener() {
		return new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection sel = (IStructuredSelection) event.getSelection();
				if (sel.size() == 1) {
					
				}
				else {
//					showPreview(null);
				}
			}
		};
	}
	
	private void setChunksTreeViewerInput() {
		// If the viewer is not initialized, do nothing.
		if (this.chunksTreeViewer == null) {
			return;
		}
		
		TimelineViewPart timeline = TimelineViewPart.getInstance();
		// If the timeline is not open, do nothing.
		if (timeline == null) {
			return;
		}
		
		Object selected = timeline.evaluateJSCode("return getStandardRectSelection();");
		List<OperationId> ids = TimelineViewPart.translateSelection(selected);
		
		Map<FileKey, List<RuntimeDC>> fileDCMap = RuntimeHistoryManager
				.getInstance().extractFileDCMapFromOperationIds(ids);
		
		List<Chunk> chunks = new ArrayList<Chunk>();
		for (FileKey fileKey : fileDCMap.keySet()) {
			List<Chunk> chunksForThisFile = SelectiveUndoEngine.getInstance()
					.determineChunksWithRuntimeDCs(fileDCMap.get(fileKey));
			chunks.addAll(chunksForThisFile);
		}
		
		// Set the input here.
		this.chunksTreeViewer.setInput(chunks);
	}
	
}
