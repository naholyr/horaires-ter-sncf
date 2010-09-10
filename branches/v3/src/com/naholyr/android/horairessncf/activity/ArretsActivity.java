package com.naholyr.android.horairessncf.activity;

import java.security.InvalidParameterException;

import org.acra.ErrorReporter;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.naholyr.android.horairessncf.Arret;
import com.naholyr.android.horairessncf.Common;
import com.naholyr.android.horairessncf.Depart;
import com.naholyr.android.horairessncf.Gare;
import com.naholyr.android.horairessncf.R;
import com.naholyr.android.ui.QuickActionWindow;
import com.naholyr.android.ui.QuickActionWindow.IntentItem;

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
		return new SimpleCursorAdapter(this, R.layout.arret_item, c, new String[] { Arret.NOM_GARE, Arret.HEURE }, new int[] { android.R.id.text1, android.R.id.text2 });
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
				Typeface font = Typeface.createFromAsset(getAssets(), "CALIBRIB.TTF");
				title1.setTypeface(font);
				title2.setTypeface(font);
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
		String keywords = item.getString(item.getColumnIndexOrThrow(Arret.NOM_GARE));
		// Clean keywords
		keywords = (" " + Common.removeAccents(keywords).replace('-', ' ') + " ").replaceAll("(?i: st | ville | les | la | le | du | des | de |[^A-Z0-9a-z])", " ");
		Log.d(Common.TAG, "nomGare = " + keywords);
		// Search gare by keywords, and assume this is the one ;)
		Cursor gare = Gare.retrieveByKeywords(this, keywords, "1");
		long idGare = 0;
		if (gare != null) {
			if (gare.moveToFirst()) {
				// Fix name
				String nomGare = gare.getString(gare.getColumnIndexOrThrow(Gare.NOM));
				((TextView) anchor.findViewById(android.R.id.text1)).setText(nomGare);
				// ID
				idGare = gare.getLong(gare.getColumnIndexOrThrow(Gare._ID));
			}
			gare.close();
		}
		Log.d(Common.TAG, "idGare = " + idGare);

		if (idGare != 0) {
			// FIXME Actions with gare OK
		} else {
			// FIXME Message or actions with gare not found
		}

		return null;
	}
}
