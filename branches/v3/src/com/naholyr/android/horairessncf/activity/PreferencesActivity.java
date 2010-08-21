package com.naholyr.android.horairessncf.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.Preference;
import android.provider.SearchRecentSuggestions;
import android.widget.Toast;

import com.naholyr.android.horairessncf.R;
import com.naholyr.android.horairessncf.data.GaresSearchSuggestionsProvider;
import com.naholyr.android.horairessncf.data.UpdateService;

public class PreferencesActivity extends android.preference.PreferenceActivity {

	private static final int DIALOG_CLEAR_HISTORY = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);

		findPreference(getString(R.string.pref_disable_auto_update)).setOnPreferenceChangeListener(
				new Preference.OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference, Object newValue) {
						if ((Boolean) newValue) {
							UpdateService.unschedule(getApplicationContext());
						} else {
							UpdateService.scheduleNow(getApplicationContext(), true);
						}
						return true;
					}
				});
		findPreference(getString(R.string.pref_disable_search_history)).setOnPreferenceChangeListener(
				new Preference.OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference, Object newValue) {
						if ((Boolean) newValue) {
							showDialog(DIALOG_CLEAR_HISTORY);
						}
						return true;
					}
				});
		findPreference(getString(R.string.pref_clear_search_history)).setOnPreferenceClickListener(
				new Preference.OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference preference) {
						showDialog(DIALOG_CLEAR_HISTORY);
						return true;
					}
				});
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_CLEAR_HISTORY: {
			return new AlertDialog.Builder(this).setCancelable(true).setTitle("Historique de recherche")
					.setMessage("Vider votre historique de recherche maintenant ?").setIcon(R.drawable.icon)
					.setNegativeButton("Non", null).setPositiveButton("Oui", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							SearchRecentSuggestions suggestions = new SearchRecentSuggestions(getApplicationContext(),
									GaresSearchSuggestionsProvider.AUTHORITY, GaresSearchSuggestionsProvider.MODE);
							suggestions.clearHistory();
							Toast.makeText(getApplicationContext(), "Votre historique de recherche a été supprimé",
									Toast.LENGTH_SHORT).show();
						}
					}).create();
		}
		default:
			return super.onCreateDialog(id);
		}
	}

}
