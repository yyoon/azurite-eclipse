package edu.cmu.scs.azurite.jface.widgets;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;

import edu.cmu.scs.fluorite.util.Utilities;


public class AlternativeButton extends Composite {
	
	private static final int THICK_BORDER_WIDTH = 5;
	private static final int THIN_BORDER_WIDTH = 1;
	private static final int MARGIN_WIDTH = 3;
	
	private boolean mSelected;
	
	private Composite mThickBorderComposite;
	private Composite mThinBorderComposite;
	private StyledText mStyledText;
	
	private List<SelectionListener> mSelectionListeners;

	public AlternativeButton(Composite parent, int style) {
		super(parent, style);
		
		mSelected = false;
		mSelectionListeners = new ArrayList<SelectionListener>();
		
		createComponents(this);
		
		addMouseDownListener(this);
	}
	
	public void addSelectionListener(SelectionListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException();
		}
		
		mSelectionListeners.add(listener);
	}
	
	public void removeSelectionListener(SelectionListener listener) {
		if (listener == null) {
			throw new IllegalArgumentException();
		}
		
		mSelectionListeners.remove(listener);
	}
	
	private void fireWidgetSelected() {
		Event event = new Event();
		event.widget = this;
		
		for (SelectionListener listener : mSelectionListeners) {
			listener.widgetSelected(new SelectionEvent(event));
		}
	}
	
	private void addMouseDownListener(Composite parent) {
		for (Control child : parent.getChildren()) {
			child.addMouseListener(new MouseAdapter() {
	
				@Override
				public void mouseDown(MouseEvent e) {
					handleMouseDown();
				}
				
			});
			
			if (child instanceof Composite) {
				addMouseDownListener((Composite)child);
			}
		}
	}
	
	private void handleMouseDown() {
		if (getSelected()) {
			// do nothing.
		}
		
		// de-select all the peers.
		for (Control child : getParent().getChildren()) {
			if (child instanceof AlternativeButton) {
				((AlternativeButton)child).setSelected(false);
			}
		}
		
		setSelected(true);
		
		fireWidgetSelected();
	}

	private void createComponents(Composite parent) {
		setLayout(new FillLayout());
		
		mThickBorderComposite = new Composite(parent, SWT.NONE);
		FillLayout fillLayout = new FillLayout();
		fillLayout.marginWidth = fillLayout.marginHeight = THICK_BORDER_WIDTH - THIN_BORDER_WIDTH;
		mThickBorderComposite.setLayout(fillLayout);
		
		mThinBorderComposite = new Composite(mThickBorderComposite, SWT.NONE);
		fillLayout = new FillLayout();
		fillLayout.marginWidth = fillLayout.marginHeight = THIN_BORDER_WIDTH;
		mThinBorderComposite.setLayout(fillLayout);
		
		Composite mid = new Composite(mThinBorderComposite, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.marginWidth = gridLayout.marginHeight = MARGIN_WIDTH;
		mid.setLayout(gridLayout);
		
		mStyledText = new StyledText(mid, SWT.NO_FOCUS | SWT.H_SCROLL);
		mStyledText.setAlwaysShowScrollBars(false);
		mStyledText.setEditable(false);
		mStyledText.setFont(Utilities.getFont());
		
		mStyledText.setCursor(getDisplay().getSystemCursor(SWT.CURSOR_ARROW));
		mStyledText.setCaret(null);
		
		// Hack for hiding selection.
		mStyledText.setSelectionBackground(mStyledText.getBackground());
		mStyledText.setSelectionForeground(mStyledText.getForeground());

		GridData gridData = new GridData(SWT.CENTER, SWT.CENTER, true, true);
		mStyledText.setLayoutData(gridData);
		
		mid.setBackground(mStyledText.getBackground());
		
		updateBorder();
	}
	
	private void updateBorder() {
		int thickColorID = getSelected() ? SWT.COLOR_LIST_SELECTION : SWT.COLOR_WIDGET_BACKGROUND; 
		Color thickBorderColor = getDisplay().getSystemColor(thickColorID);
		
		int thinColorID = getSelected() ? SWT.COLOR_LIST_SELECTION : SWT.COLOR_BLACK;
		Color thinBorderColor = getDisplay().getSystemColor(thinColorID);
		
		mThickBorderComposite.setBackground(thickBorderColor);
		mThinBorderComposite.setBackground(thinBorderColor);
		redraw();
	}
	
	public void setSelected(boolean selected) {
		mSelected = selected;
		updateBorder();
	}
	
	public boolean getSelected() {
		return mSelected;
	}
	
	public void setAlternativeCode(String code) {
		if (code == null) {
			throw new IllegalArgumentException();
		}
		
		mStyledText.setText(code);
	}

}
