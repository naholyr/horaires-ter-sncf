package com.naholyr.android.horairessncf.activity;

import java.security.InvalidParameterException;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.naholyr.android.horairessncf.Common;
import com.naholyr.android.horairessncf.Depart;
import com.naholyr.android.horairessncf.Gare;
import com.naholyr.android.horairessncf.R;
import com.naholyr.android.horairessncf.data.GaresContentProvider;
import com.naholyr.android.horairessncf.ui.ListeDepartsAdapter;
import com.naholyr.android.ui.QuickActionWindow;

public class DepartsActivity extends ListActivity {

	public static final String EXTRA_ID = Gare._ID;

	long mIdGare = 0;

	@Override
	protected ListAdapter getAdapter(Cursor c) {
		return new ListeDepartsAdapter(getApplicationContext(), c);
	}

	@Override
	protected int getLayout() {
		return R.layout.departs;
	}

	@Override
	protected Cursor queryCursor() {
		// Lookup gare info
		final Cursor c = Gare.retrieveById(getApplicationContext(), mIdGare);
		final TextView title1 = (TextView) findViewById(R.id.title1);
		final TextView title2 = (TextView) findViewById(R.id.title2);
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (c != null) {
					if (c.moveToFirst()) {
						String nom = c.getString(c.getColumnIndexOrThrow(Gare.NOM));
						title1.setText(nom);
						title2.setText(nom);
					}
					c.close();
				}
				// Typeface font = Typeface.createFromAsset(getAssets(),
				// "CALIBRIB.TTF");
				// title1.setTypeface(font);
				// title2.setTypeface(font);
			}
		});
		// Query
		return Depart.retrieveByGare(this, mIdGare, Common.DEFAULT_NB_TRAINS);
	}

	@Override
	protected void requestWindowFeatures() {
		super.requestWindowFeatures();
		requestWindowFeature(Window.FEATURE_NO_TITLE);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Read parameters
		mIdGare = getIntent().getLongExtra(EXTRA_ID, 0);
		Log.d(Common.TAG, "idGare = " + mIdGare);
		if (mIdGare == 0) {
			// From live folder ?
			Uri data = getIntent().getData();
			if (data != null) {
				if (GaresContentProvider.URI_MATCHER.match(data) == GaresContentProvider.GARE_PAR_ID) {
					mIdGare = Long.valueOf(data.getPathSegments().get(1));
					getIntent().putExtra(EXTRA_ID, mIdGare);
				}
			}
		}
		if (mIdGare == 0) {
			throw new InvalidParameterException("Expected extra '" + EXTRA_ID + "'");
		}
		// Create activity
		super.onCreate(savedInstanceState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.departs, menu);
		MenuItem item = menu.findItem(R.id.menu_refresh);
		if (item != null) {
			item.setEnabled(!mLoading);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_refresh: {
				refresh(null);
				break;
			}
			case R.id.menu_back: {
				Intent intent = new Intent(this, GaresActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
				finish();
				break;
			}
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected QuickActionWindow getQuickActionWindow(View anchor, final int position, final long id) {
		return Common.getQuickActionWindow(this, Common.TRAIN, id);
	}

}
