package com.naholyr.android.horairessncf.activity;

import android.database.Cursor;
import android.net.Uri;
import android.view.Window;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import com.naholyr.android.horairessncf.Depart;
import com.naholyr.android.horairessncf.Gare;
import com.naholyr.android.horairessncf.R;

public class DepartsActivity extends ListActivity {

	public static final String EXTRA_ID = Gare._ID;

	@Override
	protected ListAdapter getAdapter(Cursor c) {
		return new SimpleCursorAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, c, new String[] { Depart.DESTINATION }, new int[] { android.R.id.text1 });
	}

	@Override
	protected int getLayout() {
		return R.layout.departs;
	}

	@Override
	protected Cursor queryCursor() {
		long id = getIntent().getLongExtra(EXTRA_ID, 0);
		if (id == 0) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(DepartsActivity.this, "Erreur : paramètres insuffisants", Toast.LENGTH_LONG).show();
				}
			});
			return null;
		} else {
			Uri uri = Uri.withAppendedPath(Depart.Departs.CONTENT_URI, String.valueOf(id));
			return getContentResolver().query(uri, null, null, null, null);
		}
	}

	@Override
	protected void requestWindowFeatures() {
		super.requestWindowFeatures();
		requestWindowFeature(Window.FEATURE_NO_TITLE);
	}

}
