package com.naholyr.android.horairessncf.activity;

import android.os.Bundle;

import com.naholyr.android.horairessncf.R;

public class PreferencesActivity extends android.preference.PreferenceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.preferences);
	}

}
