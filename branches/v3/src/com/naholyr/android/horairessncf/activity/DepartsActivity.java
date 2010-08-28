package com.naholyr.android.horairessncf.activity;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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
			return Depart.retrieveById(this, mId);
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
						final String nom = c.getString(c.getColumnIndex(Gare.NOM));
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								TextView title1 = (TextView) findViewById(R.id.title1);
								TextView title2 = (TextView) findViewById(R.id.title2);
								title1.setText(nom);
								title2.setText(nom);
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.departs, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_refresh: {
				getListView().setVisibility(View.GONE);
				getCursor().requery();
				if (getCursor().getCount() > 0) {
					getListView().setVisibility(View.VISIBLE);
				} else {
					findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
				}
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

}
