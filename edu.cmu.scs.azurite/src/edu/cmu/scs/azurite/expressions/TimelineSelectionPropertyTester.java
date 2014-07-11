package edu.cmu.scs.azurite.expressions;

import org.eclipse.core.expressions.PropertyTester;

import edu.cmu.scs.azurite.views.TimelineViewPart;

public class TimelineSelectionPropertyTester extends PropertyTester {

	@Override
	public boolean test(Object receiver, String property, Object[] args,
			Object expectedValue) {
		
		TimelineViewPart timeline = TimelineViewPart.getInstance();
		if (timeline == null) {
			return false;
		}
		
		int numSelected = timeline.getSelectedRectsCount();
		
		switch (property) {
		
		case "rectSelected": {
			return numSelected > 0;
		}
		
		case "singleRectSelected": {
			return numSelected == 1;
		}
		
		case "multiRectsSelected": {
			return numSelected > 1;
		}
		
		case "rangeSelected": {
			return timeline.isRangeSelected();
		}
		
		case "markerVisible": {
			return timeline.isMarkerVisible();
		}
		
		default:
			break;
		}
		
		return false;
	}

}
