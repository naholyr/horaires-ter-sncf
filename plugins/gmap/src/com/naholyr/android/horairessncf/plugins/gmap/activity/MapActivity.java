package com.naholyr.android.horairessncf.plugins.gmap.activity;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.naholyr.android.horairessncf.plugins.gmap.Gare;
import com.naholyr.android.horairessncf.plugins.gmap.Gare.Gares;

public class MapActivity extends com.google.android.maps.MapActivity {

	public static final String EXTRA_ID = Gare._ID;

	public static final String EXTRA_LATITUDE = "latitude";
	public static final String EXTRA_LONGITUDE = "longitude";

	private double mLatitude, mLongitude;
	private long mId;

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		final Intent intent = getIntent();
		if (intent.hasExtra(EXTRA_ID)) {
			mId = intent.getLongExtra(EXTRA_ID, 0);
			Uri uri = Uri.withAppendedPath(Gares.CONTENT_URI, String.valueOf(mId));
			Cursor c = getContentResolver().query(uri, null, null, null, null);
			if (c != null) {
				if (c.moveToFirst()) {
					mLatitude = c.getDouble(c.getColumnIndex(Gare.LATITUDE));
					mLongitude = c.getDouble(c.getColumnIndex(Gare.LONGITUDE));
				} else {
					Toast.makeText(this, "Impossible de récupérer les informations de la gare sélectionnée", Toast.LENGTH_LONG).show();
				}
				c.close();
			}
		} else if (intent.hasExtra(EXTRA_LATITUDE) && intent.hasExtra(EXTRA_LONGITUDE)) {
			mLatitude = intent.getDoubleExtra(EXTRA_LATITUDE, 0);
			mLongitude = intent.getDoubleExtra(EXTRA_LONGITUDE, 0);
		} else {
			Toast.makeText(this, "Erreur : paramètres insuffisants", Toast.LENGTH_LONG).show();
		}
	}

}
