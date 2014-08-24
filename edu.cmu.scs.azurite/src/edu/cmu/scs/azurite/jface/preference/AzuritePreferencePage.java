package edu.cmu.scs.azurite.jface.preference;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;

import edu.cmu.scs.azurite.plugin.Activator;
import edu.cmu.scs.azurite.preferences.Initializer;

public class AzuritePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	
	private Table table;

	@Override
	protected Control createContents(Composite parent) {
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout());
		comp.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		
		Label label = new Label(comp, SWT.NONE);
		label.setText("Events to be displayed in the Timeline View");
		
		this.table = new Table(comp, SWT.SINGLE | SWT.CHECK | SWT.FULL_SELECTION | SWT.BORDER | SWT.CHECK);
		this.table.setLayoutData(new GridData(GridData.FILL_BOTH));
		this.table.setHeaderVisible(true);
		
		TableColumn col;
		col = new TableColumn(this.table, SWT.LEFT);
		col.setText("Type");
		col.setWidth(100);
		
		col = new TableColumn(this.table, SWT.LEFT);
		col.setText("Color");
		col.setWidth(100);
		
		col = new TableColumn(this.table, SWT.LEFT);
		col.setText("Icon Path");
		col.setWidth(200);
		
		// Read the existing values.
		readIntoTable(store.getString(Initializer.Pref_EventDisplaySettings));
		
		return comp;
	}
	
	private void readIntoTable(String settings) {
		this.table.removeAll();
		
		try (StringReader reader = new StringReader(settings)) {
			IMemento memento = XMLMemento.createReadRoot(reader);
			for (IMemento child : memento.getChildren()) {
				String type = child.getString("type");
				String color = child.getString("color");
				String iconPath = child.getString("iconPath");
				boolean enabled = child.getBoolean("enabled");
				
				TableItem item = new TableItem(this.table, SWT.NONE);
				item.setText(new String[] { type, color, iconPath });
				item.setChecked(enabled);
			}
		} catch (WorkbenchException e) {
			e.printStackTrace();
		}
	}
	
	private String getStringFromTable() {
		try (StringWriter writer = new StringWriter()) {
			XMLMemento root = XMLMemento.createWriteRoot(Initializer.Pref_EventDisplaySettings);
			for (TableItem item : this.table.getItems()) {
				IMemento child = root.createChild("item");
				child.putString("type", item.getText(0));
				child.putString("color", item.getText(1));
				child.putString("iconPath", item.getText(2));
				child.putBoolean("enabled", item.getChecked());
			}
			
			root.save(writer);
			
			return writer.toString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	@Override
	public void init(IWorkbench workbench) {
		// Do nothing.
		// Still need this, because Eclipse will generate an error
		// if this class can't be cast to IWorkbenchPreferencePage.
	}

	@Override
	protected void performDefaults() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		readIntoTable(store.getDefaultString(Initializer.Pref_EventDisplaySettings));
		
		super.performDefaults();
	}

	@Override
	public boolean performOk() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.setValue(Initializer.Pref_EventDisplaySettings, getStringFromTable());
		
		return super.performOk();
	}

}
