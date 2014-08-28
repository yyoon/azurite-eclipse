package edu.cmu.scs.azurite.preferences;

import java.io.IOException;
import java.io.StringWriter;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;

import edu.cmu.scs.azurite.plugin.Activator;

public class Initializer extends AbstractPreferenceInitializer {

	public static final String Pref_HistorySearchSelectedCode = "Azurite_HistorySearchSelectedCode";
	public static final String Pref_HistorySearchCurrentSession = "Azurite_HistorySearchCurrentSession";
	public static final String Pref_HistorySearchCaseSensitive = "Azurite_HistorySearchCaseSensitive";
	
	public static final String Pref_EnableMarkers = "Azurite_EnableMarkers";
	public static final String Pref_InteractiveSelectiveUndoShowChunks = "Azurite_InteractiveSelectiveUndoShowChunks";
	public static final String Pref_EventDisplaySettings = "Azurite_EventDisplaySettings";
	
	private static final String[][] DefaultEventDisplaySettings = {
		{ "JUnitCommand",                        "green",        "images/event_icons/junitsucc.gif",  "true" },
		{ "JUnitCommand(true)",                  "green",        "images/event_icons/junitsucc.gif",  "true" },
		{ "JUnitCommand(false)",                 "red",          "images/event_icons/juniterr.gif",   "true" },
		{ "RunCommand",                          "green",        "images/event_icons/run_exc.gif",    "true" },
		{ "Tag",                                 "gold",         "images/event_icons/tag.png",        "true" },
		{ "Annotation",                          "goldenrod",    "images/event_icons/annotation.png", "true" },
		{ "org.eclipse.ui.file.save",            "lightskyblue", "images/event_icons/save_edit.gif",  "false" },
		{ "org.eclipse.egit.ui.team.Commit",     "goldenrod",    "images/event_icons/commit.gif",     "true" },
		{ "org.eclipse.egit.ui.team.Pull",       "goldenrod",    "images/event_icons/pull.gif",       "true" },
		{ "org.eclipse.egit.ui.team.Fetch",      "goldenrod",    "images/event_icons/fetch.gif",      "true" },
		{ "org.eclipse.egit.ui.team.Push",       "goldenrod",    "images/event_icons/push.gif",       "true" },
		{ "org.eclipse.egit.ui.team.Merge",      "goldenrod",    "images/event_icons/merge.gif",      "true" },
		{ "org.eclipse.egit.ui.team.Reset",      "goldenrod",    "images/event_icons/reset.gif",      "true" },
		{ "org.eclipse.egit.ui.team.Rebase",     "goldenrod",    "images/event_icons/rebase.gif",     "true" },
		{ "org.eclipse.egit.ui.CheckoutCommand", "goldenrod",    "images/event_icons/checkout.gif",   "true" },
	};

	public Initializer() {
	}

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		store.setDefault(Pref_HistorySearchSelectedCode, true);
		store.setDefault(Pref_HistorySearchCurrentSession, false);
		store.setDefault(Pref_HistorySearchCaseSensitive, false);
		
		store.setDefault(Pref_EnableMarkers, false);
		store.setDefault(Pref_InteractiveSelectiveUndoShowChunks, false);
		
		try (StringWriter writer = new StringWriter()) {
			XMLMemento root = XMLMemento.createWriteRoot(Pref_EventDisplaySettings);
			for (String[] row : DefaultEventDisplaySettings) {
				IMemento child = root.createChild("item");
				child.putString("type", row[0]);
				child.putString("color", row[1]);
				child.putString("iconPath", row[2]);
				child.putString("enabled", row[3]);
			}
			
			root.save(writer);
			
			store.setDefault(Pref_EventDisplaySettings, writer.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
