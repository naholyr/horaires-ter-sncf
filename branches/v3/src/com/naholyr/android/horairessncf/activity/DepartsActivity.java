package com.naholyr.android.horairessncf.activity;

import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.naholyr.android.horairessncf.Depart;
import com.naholyr.android.horairessncf.Gare;
import com.naholyr.android.horairessncf.R;
import com.naholyr.android.horairessncf.ui.ListeDepartsAdapter;

public class DepartsActivity extends ListActivity {

	public static final String EXTRA_ID = Gare._ID;

	long mId = 0;

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
		if (mId == 0) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(DepartsActivity.this, "Erreur : paramètres insuffisants", Toast.LENGTH_LONG).show();
				}
			});
			return null;
		} else {
			Uri uri = Uri.withAppendedPath(Depart.Departs.CONTENT_URI, String.valueOf(mId));
			return getContentResolver().query(uri, null, null, null, null);
		}
	}

	@Override
	protected void requestWindowFeatures() {
		super.requestWindowFeatures();
		requestWindowFeature(Window.FEATURE_NO_TITLE);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Retrieve Station
		mId = getIntent().getLongExtra(EXTRA_ID, 0);
		// Parent process
		super.onCreate(savedInstanceState);
		// Update title
		if (mId != 0) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					Cursor c = Gare.retrieveById(getApplicationContext(), mId);
					if (c != null && c.moveToFirst()) {
						String nom = c.getString(c.getColumnIndex(Gare.NOM));
						final TextView title1 = (TextView) findViewById(R.id.title1);
						final TextView title2 = (TextView) findViewById(R.id.title2);
						title1.setText(nom);
						title2.setText(nom);
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Typeface font = Typeface.createFromAsset(getAssets(), "CALIBRIB.TTF");
								title1.setTypeface(font);
								title2.setTypeface(font);
							}
						});
						c.close();
					}
				}
			}).start();
		}
	}

}
