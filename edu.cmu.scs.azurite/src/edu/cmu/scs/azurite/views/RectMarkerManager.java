package edu.cmu.scs.azurite.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IDocument;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;
import edu.cmu.scs.azurite.commands.runtime.Segment;
import edu.cmu.scs.azurite.model.FileKey;
import edu.cmu.scs.azurite.model.OperationId;
import edu.cmu.scs.azurite.model.RuntimeHistoryManager;

public class RectMarkerManager implements RectSelectionListener {
	
	private static final String BASE_MARKER_ID = "edu.cmu.scs.azurite.baseMarker";
	
	private Map<OperationId, List<IMarker>> currentMarkers;
	
	public RectMarkerManager() {
		this.currentMarkers = new HashMap<OperationId, List<IMarker>>();
	}

	@Override
	public void rectSelectionChanged() {
		try {
			// Get the workspace root.
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			
			// Get the current selection
			List<OperationId> ids = TimelineViewPart.getInstance().getRectSelection();
			
			// Remove all the stale markers
			Iterator<OperationId> it = this.currentMarkers.keySet().iterator();
			while (it.hasNext()) {
				OperationId oid = it.next();
				if (!ids.contains(oid)) {
					for (IMarker marker : this.currentMarkers.get(oid)) {
						marker.delete();
					}
					it.remove();
				}
			}
			
			// Add all the new markers
			List<OperationId> newIds = new ArrayList<OperationId>();
			for (OperationId oid : ids) {
				if (!this.currentMarkers.containsKey(oid)) {
					newIds.add(oid);
				}
			}
			
			Map<FileKey, List<RuntimeDC>> fileDCMap = RuntimeHistoryManager
					.getInstance().extractFileDCMapFromOperationIds(newIds);
			
			for (FileKey fileKey : fileDCMap.keySet()) {
				IFile fileResource = root.getFileForLocation(new Path(fileKey.getFilePath()));
				if (fileResource == null) {
					continue;
				}
				
				IDocument doc = edu.cmu.scs.azurite.util.Utilities.findDocumentForKey(fileKey);
				if (doc == null) {
					continue;
				}
				
				for (RuntimeDC dc : fileDCMap.get(fileKey)) {
					List<IMarker> markers = new ArrayList<IMarker>();
					this.currentMarkers.put(dc.getOperationId(), markers);
					
					for (Segment segment : dc.getAllSegments()) {
						IMarker marker = fileResource.createMarker("edu.cmu.scs.azurite." + dc.getTypeString() + "Marker");
						marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
						marker.setAttribute(IMarker.MESSAGE, dc.getMarkerMessage());
						marker.setAttribute(IMarker.LINE_NUMBER, doc.getLineOfOffset(segment.getOffset()));
						marker.setAttribute(IMarker.CHAR_START, segment.getOffset());
						marker.setAttribute(IMarker.CHAR_END, segment.getEffectiveEndOffset());
						
						markers.add(marker);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void removeAllMarkers() {
		try {
			// Remove all the markers
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			root.deleteMarkers(BASE_MARKER_ID, true, IResource.DEPTH_INFINITE);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
