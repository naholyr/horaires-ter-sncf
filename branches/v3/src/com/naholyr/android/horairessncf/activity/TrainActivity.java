package com.naholyr.android.horairessncf.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;

import com.naholyr.android.horairessncf.Depart;
import com.naholyr.android.horairessncf.R;
import com.naholyr.android.ui.QuickActionWindow;

public class TrainActivity extends ListActivity {

	long mIdTrain;

	SharedPreferences mPreferences;

	@Override
	protected ListAdapter getAdapter(Cursor c) {
		return new SimpleCursorAdapter(this, android.R.layout.simple_list_item_2, c, new String[] { "text1", "text2" }, new int[] { android.R.id.text1, android.R.id.text2 });
	}

	@Override
	protected int getLayout() {
		return R.layout.departs;
	}

	@Override
	protected Cursor queryCursor() throws Throwable {
		MatrixCursor c = new MatrixCursor(new String[] { "_id", "text1", "text2" });
		c.addRow(new Object[] { 1L, "Titre 1", "description 1" });
		c.addRow(new Object[] { 2L, "Titre 2", "description 2" });
		c.addRow(new Object[] { 3L, "Titre 3", "description 3" });
		c.addRow(new Object[] { 4L, "Titre 4", "description 4" });
		c.addRow(new Object[] { 5L, "Titre 5", "description 5" });
		c.addRow(new Object[] { 6L, "Titre 6", "description 6" });
		c.addRow(new Object[] { 7L, "Titre 7", "description 7" });
		c.addRow(new Object[] { 8L, "Titre 8", "description 8" });

		return c;
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
