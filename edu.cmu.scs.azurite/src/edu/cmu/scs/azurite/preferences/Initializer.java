package edu.cmu.scs.azurite.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

import edu.cmu.scs.azurite.plugin.Activator;

public class Initializer extends AbstractPreferenceInitializer {

	public static final String Pref_HistorySearchSelectedCode = "Azurite_HistorySearchSelectedCode";
	public static final String Pref_HistorySearchCurrentSession = "Azurite_HistorySearchCurrentSession";
	public static final String Pref_HistorySearchCaseSensitive = "Azurite_HistorySearchCaseSensitive";

	public Initializer() {
	}

	@Override
	public void initializeDefaultPreferences() {
		Activator.getDefault().getPreferenceStore()
				.setDefault(Pref_HistorySearchSelectedCode, true);
		Activator.getDefault().getPreferenceStore()
				.setDefault(Pref_HistorySearchCurrentSession, false);
		Activator.getDefault().getPreferenceStore()
				.setDefault(Pref_HistorySearchCaseSensitive, false);
	}

}
