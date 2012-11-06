package edu.cmu.scs.azurite.jface.widgets;

import org.eclipse.draw2d.SWTEventDispatcher;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.zest.core.widgets.Graph;

public class NonMovableGraph extends Graph {

	public NonMovableGraph(Composite parent, int style) {
		super(parent, style);
		
		getLightweightSystem().setEventDispatcher(new SWTEventDispatcher() {

			@Override
			public void dispatchMouseMoved(MouseEvent me) {
				// Do nothing here.
//				super.dispatchMouseMoved(me);
			}
			
		});
	}

}
