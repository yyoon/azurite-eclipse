package edu.cmu.scs.azurite.views;

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

	@Override
	public void rectSelectionChanged() {
		try {
			// Remove all the markers
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			root.deleteMarkers("edu.cmu.scs.azurite.baseMarker", true, IResource.DEPTH_INFINITE);
			
			// Add appropriate markers...
			List<OperationId> ids = TimelineViewPart.getInstance().getRectSelection();
			
			Map<FileKey, List<RuntimeDC>> fileDCMap = RuntimeHistoryManager
					.getInstance().extractFileDCMapFromOperationIds(ids);
			
			for (FileKey fileKey : fileDCMap.keySet()) {
				IFile fileResource = root.getFileForLocation(new Path(fileKey.getFilePath()));
				if (fileResource == null) {
					continue;
				}
				
				IDocument doc = edu.cmu.scs.azurite.util.Utilities.findDocumentForKey(fileKey);
				
				for (RuntimeDC dc : fileDCMap.get(fileKey)) {
					for (Segment segment : dc.getAllSegments()) {
						IMarker marker = fileResource.createMarker("edu.cmu.scs.azurite." + dc.getTypeString() + "Marker");
						marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO);
						marker.setAttribute(IMarker.MESSAGE, dc.getMarkerMessage());
						marker.setAttribute(IMarker.LINE_NUMBER, doc.getLineOfOffset(segment.getOffset()));
						marker.setAttribute(IMarker.CHAR_START, segment.getOffset());
						marker.setAttribute(IMarker.CHAR_END, segment.getEffectiveEndOffset());
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
			root.deleteMarkers("edu.cmu.scs.azurite.baseMarker", true, IResource.DEPTH_INFINITE);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
