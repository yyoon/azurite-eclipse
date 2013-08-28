package edu.cmu.scs.azurite.jface.dialogs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.CompareViewerSwitchingPane;
import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;
import edu.cmu.scs.azurite.compare.AzuriteCompareInput;
import edu.cmu.scs.azurite.compare.AzuriteCompareLabelProvider;
import edu.cmu.scs.azurite.compare.SimpleCompareItem;
import edu.cmu.scs.azurite.jface.viewers.ChunksTreeViewer;
import edu.cmu.scs.azurite.jface.widgets.AlternativeButton;
import edu.cmu.scs.azurite.model.FileKey;
import edu.cmu.scs.azurite.model.OperationId;
import edu.cmu.scs.azurite.model.RuntimeHistoryManager;
import edu.cmu.scs.azurite.model.undo.Chunk;
import edu.cmu.scs.azurite.model.undo.SelectiveUndoEngine;
import edu.cmu.scs.azurite.model.undo.UndoAlternative;
import edu.cmu.scs.azurite.views.RectSelectionListener;
import edu.cmu.scs.azurite.views.TimelineViewPart;
import edu.cmu.scs.fluorite.util.Utilities;

public class InteractiveSelectiveUndoDialog extends TitleAreaDialog implements RectSelectionListener {
	
	private static final int DEFAULT_WIDTH = 800;
	private static final int DEFAULT_HEIGHT = 600;
	
	private static final int MINIMUM_CHUNKS_HEIGHT = 100;
	private static final int MINIMUM_BOTTOM_AREA_HEIGHT = 200;
	
	private static final int MARGIN_WIDTH = 10;
	private static final int MARGIN_HEIGHT = 10;
	
	private static final int SPACING = 10;
	
	private static final int SURROUNDING_CONTEXT_SIZE = 3;
	
	private static final String TEXT = "Azurite - Interactive Selective Undo";
	private static final String TITLE = "Interactive Selective Undo";
	private static final String DEFAULT_MESSAGE = "The preview will be updated as you select/deselect rectangles from the timeline.\nNOTE: Please do not edit source code while this dialog is open.";
	
	private static final String CHUNKS_TITLE = "Changes to be performed";
	private static final String ALTERNATIVES_TITLE = "Choose one of the alternatives below to resolve the conflict.";
	
	protected static final String NEXT_CHANGE_ID = "edu.cmu.scs.azurite.nextChange";
	protected static final String NEXT_CHANGE_TOOLTOP = "Select Next Chunk";
	
	protected static final String PREVIOUS_CHANGE_ID = "edu.cmu.scs.azurite.previousChange";
	protected static final String PREVIOUS_CHANGE_TOOLTIP = "Select Previous Chunk";
	
	private static final String INFORMATION_SELECT_RECTS = "You must select some rectangles to undo.";
	private static final String INFORMATION_SELECT_CHUNK = "Select a chunk from the list on the top to see the preview.";
	
	private static InteractiveSelectiveUndoDialog instance = null;
	public static InteractiveSelectiveUndoDialog getInstance() {
		return instance;
	}

	private class NextChunk extends Action {
		public NextChunk() {
			setId(NEXT_CHANGE_ID);
			setImageDescriptor(CompareUI.DESC_ETOOL_NEXT);
			setDisabledImageDescriptor(CompareUI.DESC_DTOOL_NEXT);
			setHoverImageDescriptor(CompareUI.DESC_CTOOL_NEXT);
			setToolTipText(NEXT_CHANGE_TOOLTOP);
		}
		public void run() {
			mChunksTreeViewer.revealNext();
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
			mChunksTreeViewer.revealPrevious();
		}
	}
	
	private class TopLevelElement {
		private FileKey mFileKey;
		private List<ChunkLevelElement> mChunks;
		
		public TopLevelElement(FileKey fileKey, List<Chunk> chunks) {
			if (fileKey == null || chunks == null) {
				throw new IllegalArgumentException();
			}
			
			mFileKey = fileKey;
			
			mChunks = new ArrayList<ChunkLevelElement>();
			for (Chunk chunk : chunks) {
				mChunks.add(new ChunkLevelElement(chunk, this));
			}
		}
		
		public FileKey getFileKey() {
			return mFileKey;
		}
		
		public List<ChunkLevelElement> getChunks() {
			return Collections.unmodifiableList(mChunks);
		}
		
		public boolean hasUnresolvedConflict() {
			for (ChunkLevelElement chunkElem : mChunks) {
				if (chunkElem.hasUnresolvedConflict()) {
					return true;
				}
			}
			
			return false;
		}
	}
	
	private class ChunkLevelElement {
		private Chunk mChunk;
		
		private List<UndoAlternative> mAlternatives;
		private UndoAlternative mChosenAlternative;
		
		private TopLevelElement mParent;
		
		public ChunkLevelElement(Chunk chunk, TopLevelElement parent) {
			if (chunk == null || parent == null) {
				throw new IllegalArgumentException();
			}
			
			mChunk = chunk;
			
			mParent = parent;
			
			calculateUndoAlternatives();
		}
		
		private void calculateUndoAlternatives() {
			if (getChunk().hasConflictOutsideThisChunk()) {
				try {
					IDocument doc = findDocumentForChunk(getChunk());
					Chunk expandedChunk = getChunk().getExpandedChunkWithDepth(SelectiveUndoEngine.MAX_EXPANSION_DEPTH);
					int initialOffset = expandedChunk.getStartOffset();
					String initialContent = doc.get(initialOffset, expandedChunk.getChunkLength());
					
					mAlternatives = SelectiveUndoEngine.getInstance().doSelectiveUndoChunkWithConflicts(getChunk(), initialContent);
					mChosenAlternative = null;
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		public Chunk getChunk() {
			return mChunk;
		}
		
		public TopLevelElement getParent() {
			return mParent;
		}
		
		public boolean hasUnresolvedConflict() {
			return mChunk.hasConflictOutsideThisChunk() && mChosenAlternative == null;
		}
		
		public List<UndoAlternative> getUndoAlternatives() {
			return mAlternatives;
		}
		
		public UndoAlternative getChosenAlternative() {
			return mChosenAlternative;
		}
		
		public void setChosenAlternative(UndoAlternative chosenAlternative) {
			mChosenAlternative = chosenAlternative;
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

		// Assume that inputElement is typed as Map<FileKey, List<Chunk>>
		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof Object[]) {
				return (Object[]) inputElement; 
			} else {
				return null;
			}
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof TopLevelElement) {
				TopLevelElement topElem = (TopLevelElement) parentElement;
				return topElem.getChunks().toArray();
			}
			
			return null;
		}

		@Override
		public Object getParent(Object element) {
			if (element instanceof ChunkLevelElement) {
				ChunkLevelElement chunkElem = (ChunkLevelElement) element;
				return chunkElem.getParent();
			}
			
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof TopLevelElement) {
				TopLevelElement topElem = (TopLevelElement) element;
				return !topElem.getChunks().isEmpty();
			}
			
			return false;
		}
		
	}
	
	private class ChunksLabelProvider extends LabelProvider {
		
		private Map<ImageDescriptor, Image> mImages;
		
		public ChunksLabelProvider() {
			mImages = new HashMap<ImageDescriptor, Image>();
		}

		@Override
		public Image getImage(Object element) {
			if (element instanceof TopLevelElement) {
				TopLevelElement topElem = (TopLevelElement) element;
				FileKey fileKey = topElem.getFileKey();
				
				ImageDescriptor baseImageDesc = PlatformUI.getWorkbench().getEditorRegistry().getImageDescriptor(fileKey.getFileNameOnly());
				Image baseImage = getImage(baseImageDesc);

				if (topElem.hasUnresolvedConflict()) {
					ImageDescriptor decError = PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_DEC_FIELD_ERROR);
					DecorationOverlayIcon decorated = new DecorationOverlayIcon(baseImage, decError, IDecoration.BOTTOM_RIGHT);
					
					return getImage(decorated);
				}
				else {
					return baseImage;
				}
			}
			else if (element instanceof ChunkLevelElement) {
				ChunkLevelElement chunkElem = (ChunkLevelElement) element;
				
				ImageDescriptor errorImage = PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJS_ERROR_TSK);
				return chunkElem.hasUnresolvedConflict() ? getImage(errorImage) : null;
			}
			else {
				return null;
			}
		}

		@Override
		public String getText(Object element) {
			if (element instanceof TopLevelElement) {
				TopLevelElement topElem = (TopLevelElement) element;
				return topElem.getFileKey().getFileNameOnly();
			}
			else if (element instanceof ChunkLevelElement) {
				ChunkLevelElement chunkElem = (ChunkLevelElement) element;
				Chunk chunk = chunkElem.getChunk();
				IDocument doc = findDocumentForChunk(chunk);
				return getLabelForChunk(chunk, doc);
			}
			else {
				return super.getText(element);
			}
		}
		
		@Override
		public void dispose() {
			super.dispose();
			
			for (Image image : mImages.values()) {
				image.dispose();
			}
		}
		
		private Image getImage(ImageDescriptor imgDesc) {
			if (!mImages.containsKey(imgDesc)) {
				Image image = imgDesc.createImage();
				mImages.put(imgDesc, image);
			}
			
			return mImages.get(imgDesc);
		}
		
	}
	
	// Menu
	private Menu mMenuBar;
	// ----------------------------------------
	
	// Sash Forms
	private SashForm mTopSash;
	private SashForm mBottomSash;
	// ----------------------------------------
	
	// For the top part
	private ChunksTreeViewer mChunksTreeViewer;
	// ----------------------------------------
	
	private Composite mBottomArea;
	private StackLayout mBottomStackLayout;	// the StackLayout object used for panel switching in the bottom area.
	
	// For Preview Panel ---------------
	private Composite mConflictResolutionArea;
	
	private CompareViewerSwitchingPane mPreviewPane;
	private CompareConfiguration mCompareConfiguration;
	private String mCompareTitle;
	// ----------------------------------------
	
	// For Conflict Resolution Panel ----------
	@SuppressWarnings("restriction")
	private org.eclipse.jdt.internal.ui.util.ViewerPane mConflictResolutionPane;
	// ----------------------------------------
	
	// For Information Panel ------------------
	private Label mInformationLabel;
	// ----------------------------------------

	public InteractiveSelectiveUndoDialog(Shell parent) {
		super(parent);
		
		// Make the dialog modeless.
		setShellStyle(SWT.CLOSE | SWT.MODELESS | SWT.BORDER | SWT.TITLE | SWT.RESIZE);
		setBlockOnOpen(false);
		
		setHelpAvailable(false);
		
		// Create the default compare configuration.
		mCompareConfiguration = createCompareConfiguration();
	}

	private CompareConfiguration createCompareConfiguration() {
		CompareConfiguration configuration = new CompareConfiguration();
		configuration.setDefaultLabelProvider(new AzuriteCompareLabelProvider());
		return configuration;
	}

	@Override
	public void create() {
		super.create();
		
		instance = this;
		
		getShell().setText(TEXT);
		
		setTitle(TITLE);
		setMessage(DEFAULT_MESSAGE);
		
		getShell().setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
		
		// Register myself to the timeline.
		TimelineViewPart.getInstance().addRectSelectionListener(this);
	}

	@Override
	protected void handleShellCloseEvent() {
		cleanup();
		
		super.handleShellCloseEvent();
	}

	@Override
	protected void cancelPressed() {
		cleanup();
		
		super.cancelPressed();
	}

	@Override
	protected void okPressed() {
		cleanup();
		
		super.okPressed();
	}

	private void cleanup() {
		// Deregister myself from the timeline.
		TimelineViewPart.getInstance().removeRectSelectionListener(this);
		
		instance = null;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite composite = createMainArea(parent);
		
		// Create the top sash.
		mTopSash = new SashForm(composite, SWT.VERTICAL);
		
		// Chunks tree on the top.
		createChunksTreeViewer(mTopSash);
		
		// Bottom Area. Use StackLayout to switch between panels.
		createBottomArea(mTopSash);
		
		mTopSash.setSashWidth(SPACING);
		mTopSash.setWeights(new int[] { 1, 3 });
		
		// Setup the menu
		createMenuBar();
		
		return composite;
	}

	@Override
	protected Control createContents(Composite parent) {
		Control contents = super.createContents(parent);
		
		updateOKButtonEnabled();
		
		return contents;
	}

	private void createMenuBar() {
		mMenuBar = new Menu(getShell(), SWT.BAR);
		
		final MenuItem testMenu = new MenuItem(mMenuBar, SWT.CASCADE);
		testMenu.setText("Test");
		
		getShell().setMenuBar(mMenuBar);
	}

	private Composite createMainArea(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		
		// Use SashForm.
		FillLayout fillLayout = new FillLayout();
		fillLayout.marginWidth = MARGIN_WIDTH;
		fillLayout.marginHeight = MARGIN_HEIGHT;
		
		composite.setLayout(fillLayout);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		return composite;
	}

	@SuppressWarnings("restriction")
	private void createChunksTreeViewer(Composite parent) {
		org.eclipse.jdt.internal.ui.util.ViewerPane chunksPane =
				new org.eclipse.jdt.internal.ui.util.ViewerPane(parent, SWT.BORDER | SWT.FLAT);
		mChunksTreeViewer = new ChunksTreeViewer(chunksPane, SWT.NONE);
		
		// Set the label text.
		chunksPane.setText(CHUNKS_TITLE);
		
		// Setup the toolbar.
		ToolBarManager tbm = chunksPane.getToolBarManager();
		tbm.add(new NextChunk());
		tbm.add(new PreviousChunk());
		
		tbm.update(true);
		
		// Setup the model for the tree viewer.
		mChunksTreeViewer.setContentProvider(createChunksTreeContentProvider());
		mChunksTreeViewer.setLabelProvider(createChunksTreeLabelProvider());
		mChunksTreeViewer.setComparator(createChunksTreeComparator());
		mChunksTreeViewer.addSelectionChangedListener(createSelectionChangedListener());
		
		// This means that the top-level elements will automatically be expanded.
		mChunksTreeViewer.setAutoExpandLevel(2);
		
		// Set the initial input
		setChunksTreeViewerInput();
		
		// Set the content of the ViewerPane to the tree-control.
		chunksPane.setContent(mChunksTreeViewer.getControl());
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
				ChunkLevelElement lhs = (ChunkLevelElement) e1;
				ChunkLevelElement rhs = (ChunkLevelElement) e2;
				
				return Chunk.getLocationComparator().compare(lhs.getChunk(), rhs.getChunk());
			}
		};
	}

	private ISelectionChangedListener createSelectionChangedListener() {
		return new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection sel = (IStructuredSelection) event.getSelection();
				updateBottomPanel(sel);
			}
		};
	}
	
	private void setChunksTreeViewerInput() {
		// If the viewer is not initialized, do nothing.
		if (mChunksTreeViewer == null) {
			return;
		}
		
		TimelineViewPart timeline = TimelineViewPart.getInstance();
		// If the timeline is not open, do nothing.
		if (timeline == null) {
			return;
		}
		
		List<OperationId> ids = timeline.getRectSelection();
		
		Map<FileKey, List<RuntimeDC>> fileDCMap = RuntimeHistoryManager
				.getInstance().extractFileDCMapFromOperationIds(ids);
		
		List<TopLevelElement> topList = new ArrayList<TopLevelElement>();
		
		for (FileKey fileKey : fileDCMap.keySet()) {
			List<Chunk> chunksForThisFile = SelectiveUndoEngine.getInstance()
					.determineChunksWithRuntimeDCs(fileDCMap.get(fileKey));
			
			TopLevelElement topElem = new TopLevelElement(fileKey, chunksForThisFile);
			topList.add(topElem);
		}
		
		// Set the input here.
		mChunksTreeViewer.setInput(topList.toArray(new TopLevelElement[topList.size()]));
		
		updateOKButtonEnabled();
	}

	@Override
	public void rectSelectionChanged() {
		// TODO maybe preserve the previous selections about the conflict resolution?
		setChunksTreeViewerInput();
		updateBottomPanel();
	}
	
	private void createBottomArea(Composite parent) {
		// Create the bottom sash form.
		mBottomSash = new SashForm(parent, SWT.VERTICAL);
		mBottomSash.setSashWidth(SPACING);
		
		// Create the conflict resolution area.
		createConflictResolutionArea(mBottomSash);
		
		// Create the bottom area
		mBottomArea = new Composite(mBottomSash, SWT.NONE);
		mBottomSash.setWeights(new int[] { 1, 3 });
		
		// Use StackLayout
		mBottomStackLayout = new StackLayout();
		mBottomArea.setLayout(mBottomStackLayout);
		
		// Create the side-by-side preview panel.
		createPreviewPanel(mBottomArea);
		
		// Information panel, telling some useful information
		createInformationPanel(mBottomArea);
		
		// Update the bottom panel.
		updateBottomPanel();
	}
	
	@SuppressWarnings("restriction")
	private void createConflictResolutionArea(Composite parent) {
		mConflictResolutionPane = new org.eclipse.jdt.internal.ui.util.ViewerPane(
				mBottomSash, SWT.BORDER | SWT.FLAT);
		
		// Set the label text.
		mConflictResolutionPane.setText(ALTERNATIVES_TITLE);
	}
	
	private void createPreviewPanel(Composite parent) {
		mPreviewPane = new CompareViewerSwitchingPane(parent, SWT.BORDER | SWT.FLAT) {
			@Override
			protected Viewer getViewer(Viewer oldViewer, Object input) {
				// TODO Auto-generated method stub
				Viewer v = CompareUI.findContentViewer(oldViewer, input, this, mCompareConfiguration);
				v.getControl().setData(CompareUI.COMPARE_VIEWER_TITLE, mCompareTitle);
				
				return v;
			}
		};
	}
	
	private void showConflictResolution() {
		mBottomSash.setMaximizedControl(null);
		mBottomSash.setWeights(new int[] { 1, 3 });
	}
	
	private void hideConflictResolution() {
		mBottomSash.setMaximizedControl(mBottomArea);
	}
	
	private void showBottomPanel(boolean showConflictResolution, String informationMessage) {
		if (showConflictResolution) {
			showConflictResolution();
		} else {
			hideConflictResolution();
		}
		
		if (informationMessage != null) {
			// Show the information panel.
			mInformationLabel.setText(informationMessage);
			mBottomStackLayout.topControl = mInformationLabel;
		} else {
			// Show the preview panel.
			mBottomStackLayout.topControl = mPreviewPane;
		}
		
		mBottomArea.layout();
	}
	
	private void createInformationPanel(Composite parent) {
		mInformationLabel = new Label(parent, SWT.BORDER | SWT.FLAT);
	}
	
	private void showPreviewPanel(Chunk chunk) {
		if (chunk == null) {
			throw new IllegalArgumentException();
		}
		
		try {
			IDocument doc = findDocumentForChunk(chunk);
			
			// Original source
			String originalContents = doc.get(chunk.getStartOffset(), chunk.getChunkLength());
			
			// Calculate the preview using selective undo engine
			String undoResult = SelectiveUndoEngine.getInstance()
					.doSelectiveUndoChunkWithoutConflicts(chunk, originalContents);
			
			mCompareTitle = getLabelForChunk(chunk, doc);
			
			// Calculate the startline / endline from the originalContents.
			int startLine = doc.getLineOfOffset(chunk.getStartOffset());
			int endLine = doc.getLineOfOffset(chunk.getEndOffset());
			
			// Add surrounding context before/after the code
			int contextStartLine = Math.max(startLine - SURROUNDING_CONTEXT_SIZE, 0);
			int contextEndLine = Math.min(endLine + SURROUNDING_CONTEXT_SIZE, doc.getNumberOfLines() - 1);
			
			int contextStartOffset = doc.getLineOffset(contextStartLine);
			int contextEndOffset = doc.getLineOffset(contextEndLine) + doc.getLineLength(contextEndLine);
			
			String contextBefore = doc.get(
					contextStartOffset,
					chunk.getStartOffset() - contextStartOffset);
			String contextAfter = doc.get(
					chunk.getEndOffset(),
					contextEndOffset - chunk.getEndOffset());
			
			setPreviewInput(
					contextBefore + originalContents + contextAfter,
					contextBefore + undoResult + contextAfter);
			
			// Bring the preview panel to top.
			showBottomPanel(false, null);
		}
		catch (Exception e) {
			e.printStackTrace();
			
			// Display an error message on the screen.
			String msg = "Error occurred while generating the preview.";
			showBottomPanel(false, msg);
		}
	}
	
	private void setPreviewInput(String originalContents, String undoResult) {
		// Left, right items.
		SimpleCompareItem leftItem = new SimpleCompareItem(
				"Original Source",
				originalContents,
				false);
		SimpleCompareItem rightItem = new SimpleCompareItem(
				"Preview of Selective Undo Result",
				undoResult,
				false);
		
		// Build the compareInput and feed it into the compare viewer switching pane.
		AzuriteCompareInput compareInput = new AzuriteCompareInput(
				leftItem,	// Original Source
				rightItem);	// Preview Source
		
		mPreviewPane.setInput(compareInput);
	}
	
	private void showPreviewPanel(TopLevelElement topElem) {
		if (topElem == null) {
			throw new IllegalArgumentException();
		}
		
		try {
			FileKey fileKey = topElem.getFileKey();
			IDocument doc = findDocumentForKey(fileKey);
			
			// Get the chunks.
			List<ChunkLevelElement> chunkElems = topElem.getChunks();
			List<Chunk> chunks = new ArrayList<Chunk>();
			Map<Chunk, UndoAlternative> chosenAlternatives =
					new HashMap<Chunk, UndoAlternative>();
			for (ChunkLevelElement chunkElem : chunkElems) {
				Chunk chunk = chunkElem.getChunk();
				chunks.add(chunkElem.getChunk());
				
				if (chunk.hasConflictOutsideThisChunk()) {
					chosenAlternatives.put(chunk, chunkElem.getChosenAlternative());
				}
			}
			
			// Original source
			String originalContents = doc.get();
			
			// Copy of the source.
			IDocument docResult = new Document(originalContents);
			SelectiveUndoEngine.getInstance()
					.doSelectiveUndoWithChunks(chunks, docResult, chosenAlternatives);
			String undoResult = docResult.get();
			
			mCompareTitle = fileKey.getFileNameOnly();
			
			setPreviewInput(originalContents, undoResult);
			
			// Bring the preview panel to top.
			showBottomPanel(false, null);
		}
		catch (Exception e) {
			e.printStackTrace();
			
			// Display an error message on the screen.
			String msg = "Error occurred while generating the preview.";
			showBottomPanel(false, msg);
		}
	}
	
	public String getLabelForChunk(Chunk chunk, IDocument doc) {
		try {
			int startLine = doc.getLineOfOffset(chunk.getStartOffset());
			int endLine = doc.getLineOfOffset(chunk.getEndOffset());
			if (startLine == endLine) {
				return chunk.getBelongsTo().getFileNameOnly() + ": line " + startLine;
			}
			else {
				return chunk.getBelongsTo().getFileNameOnly() + ": lines " + startLine + "-" + endLine;
			}
		}
		catch (Exception e) {
			return chunk.getBelongsTo().getFileNameOnly();
		}
	}

	public IDocument findDocumentForChunk(Chunk chunk) {
		return findDocumentForKey(chunk.getBelongsTo());
	}
	
	public IDocument findDocumentForKey(FileKey fileKey) {
		try {
			// Retrieve the IDocument, using the file information.
			IDocument doc = findDocumentFromOpenEditors(fileKey);
			// If this file is not open, then just connect it with the relative path.
			if (doc == null) {
				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				IWorkspaceRoot root = workspace.getRoot();
				
				IPath absPath = new Path(fileKey.getFilePath());
				IPath relPath = absPath.makeRelativeTo(root.getLocation());
				
				ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
				manager.connect(relPath, LocationKind.IFILE, null);
				ITextFileBuffer buffer = manager.getTextFileBuffer(relPath, LocationKind.IFILE);
				
				doc = buffer.getDocument();
			}
			return doc;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private IDocument findDocumentFromOpenEditors(FileKey fileKey) {
		try {
			IEditorReference[] editorRefs = PlatformUI.getWorkbench()
					.getActiveWorkbenchWindow().getActivePage()
					.getEditorReferences();
			
			for (IEditorReference editorRef : editorRefs) {
				IEditorInput input = editorRef.getEditorInput();
				if (input instanceof IFileEditorInput) {
					IFileEditorInput fileInput = (IFileEditorInput) input;
					IFile file = fileInput.getFile();
					
					FileKey key = new FileKey(
							file.getProject().getName(),
							file.getLocation().toOSString());
					
					// This is the same file!
					// Get the IDocument object from this editor.
					if (fileKey.equals(key)) {
						return Utilities.getDocument(editorRef.getEditor(false));
					}
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}

	private void showConflictResolutionPanel(final ChunkLevelElement chunkElem) {
		// Delete the conflict resolution area if there was previously.
		clearConflictResolutionArea();
		
		mConflictResolutionArea = new Composite(mConflictResolutionPane, SWT.NONE);
		mConflictResolutionArea.setLayout(new FillLayout());
		
		mConflictResolutionPane.setContent(mConflictResolutionArea);
		
		// Add the alternatives
		for (final UndoAlternative alternative : chunkElem.getUndoAlternatives()) {
			AlternativeButton buttonAlternative = new AlternativeButton(mConflictResolutionArea, SWT.RADIO);
			buttonAlternative.setAlternativeCode(alternative.getResultingCode());
			buttonAlternative.setToolTipText(alternative.getDescription());
			
			if (chunkElem.getChosenAlternative() == alternative) {
				buttonAlternative.setSelected(true);
				showPreviewForConflictResolution(chunkElem);
			}
			
			buttonAlternative.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					chunkElem.setChosenAlternative(alternative);
					mChunksTreeViewer.update(chunkElem, null);
					mChunksTreeViewer.update(chunkElem.getParent(), null);
					showPreviewForConflictResolution(chunkElem);
					
					updateOKButtonEnabled();
				}
			});
		}
		
		mConflictResolutionArea.layout(true);

		// update the layout so that the conflict resolution panel is shown.
		if (chunkElem.getChosenAlternative() == null) {
			String msg = "Please select one of the alternatives to resolve this conflict.";
			showBottomPanel(true, msg);
		} else {
			showBottomPanel(true, null);
		}
	}
	
	private void updateOKButtonEnabled() {
		Button okButton = null;
		
		try {
			okButton = getButton(OK);
			
			boolean enabled = true;
			for (TopLevelElement topElem : (TopLevelElement[]) mChunksTreeViewer.getInput()) {
				if (topElem.hasUnresolvedConflict()) {
					enabled = false;
					break;
				}
			}
			
			okButton.setEnabled(enabled);
		}
		catch (Exception e) {
			if (okButton != null) {
				okButton.setEnabled(false);
			}
		}
	}
	
	private void showPreviewForConflictResolution(ChunkLevelElement chunkElem) {
		try {
			Chunk chunk = chunkElem.getChunk();
			IDocument doc = findDocumentForChunk(chunk);
			
			Chunk expandedChunk = chunk.getExpandedChunkWithDepth(SelectiveUndoEngine.MAX_EXPANSION_DEPTH);
			
			// Original source
			String originalContents = doc.get(expandedChunk.getStartOffset(), expandedChunk.getChunkLength());
			
			// Calculate the preview using selective undo engine
			String undoResult = chunkElem.getChosenAlternative().getResultingCode();
			
			mCompareTitle = getLabelForChunk(expandedChunk, doc);
			
			// Calculate the startline / endline from the originalContents.
			int startLine = doc.getLineOfOffset(expandedChunk.getStartOffset());
			int endLine = doc.getLineOfOffset(expandedChunk.getEndOffset());
			
			// Add surrounding context before/after the code
			int contextStartLine = Math.max(startLine - SURROUNDING_CONTEXT_SIZE, 0);
			int contextEndLine = Math.min(endLine + SURROUNDING_CONTEXT_SIZE, doc.getNumberOfLines() - 1);
			
			int contextStartOffset = doc.getLineOffset(contextStartLine);
			int contextEndOffset = doc.getLineOffset(contextEndLine) + doc.getLineLength(contextEndLine);
			
			String contextBefore = doc.get(
					contextStartOffset,
					expandedChunk.getStartOffset() - contextStartOffset);
			String contextAfter = doc.get(
					expandedChunk.getEndOffset(),
					contextEndOffset - expandedChunk.getEndOffset());

			setPreviewInput(
					contextBefore + originalContents + contextAfter,
					contextBefore + undoResult + contextAfter);
			
			showBottomPanel(true, null);
		}
		catch (Exception e) {
			e.printStackTrace();
			
			// Display an error message on the screen.
			String msg = "Error occurred while generating the preview.";
			showBottomPanel(true, msg);
		}
	}

	public void clearConflictResolutionArea() {
		if (mConflictResolutionArea != null) {
			mConflictResolutionArea.dispose();
			mConflictResolutionArea = null;
			mConflictResolutionPane.layout(true);
		}
	}
	
	public void updateBottomPanel() {
		updateBottomPanel((IStructuredSelection)mChunksTreeViewer.getSelection());
	}

	public void updateBottomPanel(IStructuredSelection sel) {
		if (sel.size() == 1) {
			Object firstElement = sel.getFirstElement();
			
			if (firstElement instanceof ChunkLevelElement) {
				ChunkLevelElement chunkElem = (ChunkLevelElement) firstElement;
				Chunk chunk = chunkElem.getChunk();
				
				if (chunk.hasConflictOutsideThisChunk()) {
					// Show the conflict resolution panel.
					showConflictResolutionPanel(chunkElem);
				} else {
					// Show a normal preview..
					showPreviewPanel(chunk);
				}
			}
			else if (firstElement instanceof TopLevelElement) {
				TopLevelElement topElem = (TopLevelElement) firstElement;
				
				if (topElem.hasUnresolvedConflict()) {
					String msg = "You must resolve all the conflicts under this file to be able to see the entire preview.";
					showBottomPanel(false, msg);
				}
				else {
					showPreviewPanel(topElem);
				}
			}
		}
		else {
			TopLevelElement[] input = (TopLevelElement[]) mChunksTreeViewer.getInput();
			boolean chunksEmpty = input == null || input.length == 0;
			
			String msg = chunksEmpty ? INFORMATION_SELECT_RECTS
					: INFORMATION_SELECT_CHUNK;
			showBottomPanel(false, msg);
		}
	}
	
}
