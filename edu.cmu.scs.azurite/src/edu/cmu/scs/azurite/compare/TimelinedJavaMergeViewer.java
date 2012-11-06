package edu.cmu.scs.azurite.compare;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.jdt.internal.ui.compare.JavaMergeViewerCopy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

public class TimelinedJavaMergeViewer extends JavaMergeViewerCopy {
	
	protected static final int TIMELINE_HEIGHT = 50;
	
	private Canvas fTimeline;

	public TimelinedJavaMergeViewer(Composite parent, int styles,
			CompareConfiguration mp) {
		super(parent, styles, mp);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void createControls(Composite composite) {
		// TODO Auto-generated method stub
		super.createControls(composite);
		
		fTimeline = new Canvas(composite, SWT.NONE);
		fTimeline.addPaintListener(new PaintListener() {

			public void paintControl(PaintEvent e) {
				Rectangle r = fTimeline.getClientArea();
				
				e.gc.setBackground(e.display.getSystemColor(SWT.COLOR_WHITE));
				e.gc.fillRectangle(r.x, r.y, r.width, r.height);
			}
			
		});
	}

	@Override
	protected void handleResizeAncestor(int x, int y, int width, int height) {
		super.handleResizeAncestor(x, y, width, height);
	}

	@Override
	protected void handleResizeLeftRight(int x, int y, int width1,
			int centerWidth, int width2, int height) {
		super.handleResizeLeftRight(x, y, width1, centerWidth, width2, height);
		
		// Timeline area
		fTimeline.setBounds(x, y + height, width1 + centerWidth + width2, TIMELINE_HEIGHT);
	}

	@Override
	protected Rectangle getClientArea(Composite composite) {
		Rectangle r = super.getClientArea(composite);
		
		if (r.height > TIMELINE_HEIGHT)
			r.height -= TIMELINE_HEIGHT;
		
		return r;
	}

}
