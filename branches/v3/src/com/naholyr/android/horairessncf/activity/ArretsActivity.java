package com.naholyr.android.horairessncf.activity;

import java.security.InvalidParameterException;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.naholyr.android.horairessncf.Arret;
import com.naholyr.android.horairessncf.Common;
import com.naholyr.android.horairessncf.Depart;
import com.naholyr.android.horairessncf.R;
import com.naholyr.android.horairessncf.ui.ListeArretsAdapter;
import com.naholyr.android.ui.QuickActionWindow;

public class ArretsActivity extends ListActivity {

	public static final String EXTRA_ID = Depart._ID;
	public static final String EXTRA_NUMERO = Depart.NUMERO;
	public static final String EXTRA_TITLE_FORMAT = "title_format";

	public static final String DEFAULT_TITLE_FORMAT = "Arrêts du n°%s";

	long mIdTrain;
	String mNumeroTrain;

	SharedPreferences mPreferences;

	@Override
	protected ListAdapter getAdapter(Cursor c) {
		return new ListeArretsAdapter(this, c);
	}

	@Override
	protected int getLayout() {
		return R.layout.arrets;
	}

	@Override
	protected Cursor queryCursor() throws Throwable {
		// Complete train info depending on parameters
		if (mNumeroTrain == null) {
			Cursor c = Depart.retrieveById(this, mIdTrain);
			if (c != null) {
				if (c.moveToFirst()) {
					mNumeroTrain = c.getString(c.getColumnIndexOrThrow(Depart.NUMERO));
					c.close();
				} else {
					c.close();
					throw new InvalidParameterException("Invalid ID : no Depart found");
				}
			}
		}
		// Update title
		String format = getIntent().getStringExtra(EXTRA_TITLE_FORMAT);
		if (format == null) {
			format = DEFAULT_TITLE_FORMAT;
		}
		final TextView title1 = (TextView) findViewById(R.id.title1);
		final TextView title2 = (TextView) findViewById(R.id.title2);
		final String title;
		if (mNumeroTrain != null) {
			title = String.format(format, mNumeroTrain);
		} else {
			title = null;
		}
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (title != null) {
					title1.setText(title);
					title2.setText(title);
				}
				// Typeface font = Typeface.createFromAsset(getAssets(),
				// "CALIBRIB.TTF");
				// title1.setTypeface(font);
				// title2.setTypeface(font);
			}
		});
		// Execute query
		if (mIdTrain != 0) {
			Log.d(Common.TAG, "retrieveByDepart(" + mIdTrain + ")");
			return Arret.retrieveByDepart(this, mIdTrain);
		} else {
			Log.d(Common.TAG, "retrieveByNumeroTrain(" + mNumeroTrain + ")");
			return Arret.retrieveByNumeroTrain(this, mNumeroTrain);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Read parameters
		final Intent queryIntent = getIntent();
		Bundle extras = queryIntent.getExtras();
		for (String key : extras.keySet()) {
			Log.d(Common.TAG, "Extra." + key + " = " + extras.get(key));
		}
		mIdTrain = queryIntent.getLongExtra(EXTRA_ID, 0);
		mNumeroTrain = queryIntent.getStringExtra(EXTRA_NUMERO);
		if (mNumeroTrain == null && mIdTrain == 0) {
			throw new InvalidParameterException("Expected extra '" + EXTRA_NUMERO + "' or '" + EXTRA_ID + "'");
		}
		// Preferences
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		// Create activity
		super.onCreate(savedInstanceState);
	}

	@Override
	protected QuickActionWindow getQuickActionWindow(View anchor, int position, long id) {
		// Retrieve nomGare, and search in local database
		Cursor item = (Cursor) getListView().getItemAtPosition(position);
		final int idGare = item.getInt(item.getColumnIndex(Arret.ID_GARE));

		if (idGare != 0) {
			return Common.getQuickActionWindow(this, Common.ARRET, idGare);
		} else {
			Toast.makeText(this, "Gare non trouvée dans la base de données.", Toast.LENGTH_SHORT);
			return null;
		}
	}
}
