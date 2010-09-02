package com.naholyr.android.horairessncf.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;

import com.naholyr.android.horairessncf.Arret;
import com.naholyr.android.horairessncf.Depart;
import com.naholyr.android.horairessncf.R;
import com.naholyr.android.ui.QuickActionWindow;

public class ArretsActivity extends ListActivity {

	long mIdTrain;

	SharedPreferences mPreferences;

	@Override
	protected ListAdapter getAdapter(Cursor c) {
		return new SimpleCursorAdapter(this, android.R.layout.simple_list_item_2, c, new String[] { Arret.NOM_GARE, Arret.HEURE }, new int[] { android.R.id.text1,
				android.R.id.text2 });
	}

	@Override
	protected int getLayout() {
		return R.layout.departs;
	}

	@Override
	protected Cursor queryCursor() throws Throwable {
		return Arret.retrieveByDepart(this, mIdTrain);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Preferences
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		// Intent action
		final Intent queryIntent = getIntent();
		mIdTrain = queryIntent.getLongExtra(Depart._ID, 0);
	}

	@Override
	protected QuickActionWindow getQuickActionWindow(int position, long id) {
		return null;
	}

}
