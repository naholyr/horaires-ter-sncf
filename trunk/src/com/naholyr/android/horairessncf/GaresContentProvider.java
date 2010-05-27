package com.naholyr.android.horairessncf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class GaresContentProvider extends ContentProvider {

	public static final String PROVIDER_NAME = "com.naholyr.android.horairessncf.garescontentprovider";

	public static final Uri CONTENT_URI = Uri.parse("content://" + PROVIDER_NAME);
	public static final Uri PROCHAINS_DEPARTS_URI_ROWID = Uri.parse("content://" + GaresContentProvider.PROVIDER_NAME + "/departs/#");
	public static final Uri LIVE_FOLDER_URI_FAVORITES = Uri.parse("content://" + GaresContentProvider.PROVIDER_NAME + "/live_folder/favorites");
	public static final Uri LIVE_FOLDER_URI_GEOLOCATION = Uri.parse("content://" + GaresContentProvider.PROVIDER_NAME + "/live_folder/geolocation");

	public static final String CONTENT_TYPE_LIST = "vnd.android.cursor.dir/vnd.horairessncf.gare";
	public static final String CONTENT_TYPE_ITEM = "vnd.android.cursor.item/vnd.horairessncf.gare";

	private static final int LIVE_FOLDER_FAVS = 1;
	private static final int LIVE_FOLDER_GEO = 2;
	public static final int PROCHAINS_DEPARTS_ROWID = 3;
	public static final int PROCHAINS_DEPARTS_NOM = 4;
	public static final UriMatcher URI_MATCHER;
	static {
		URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
		URI_MATCHER.addURI(PROVIDER_NAME, "live_folder/favorites", LIVE_FOLDER_FAVS);
		URI_MATCHER.addURI(PROVIDER_NAME, "live_folder/geolocation", LIVE_FOLDER_GEO);
		URI_MATCHER.addURI(GaresContentProvider.PROVIDER_NAME, "departs/#", PROCHAINS_DEPARTS_ROWID);
		URI_MATCHER.addURI(GaresContentProvider.PROVIDER_NAME, "departs/*", PROCHAINS_DEPARTS_NOM);
	}

	public static final String NOM = "nom";

	private DataHelper mDataHelper;

	/**
	 * @see android.content.ContentProvider#getType(Uri)
	 */
	public String getType(Uri uri) {
		switch (URI_MATCHER.match(uri)) {
		case LIVE_FOLDER_FAVS:
		case LIVE_FOLDER_GEO:
			return CONTENT_TYPE_LIST;
		case PROCHAINS_DEPARTS_ROWID:
		case PROCHAINS_DEPARTS_NOM:
			return CONTENT_TYPE_ITEM;
		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
	}

	/**
	 * @see android.content.ContentProvider#onCreate()
	 */
	public boolean onCreate() {
		DataHelper helper = getDataHelper();

		return helper != null;
	}

	private DataHelper getDataHelper() {
		if (mDataHelper == null) {
			try {
				mDataHelper = DataHelper.getInstance(getContext());
			} catch (IOException e) {
				Log.e("live_folder", "failed init db", e);
			} catch (UnsupportedOperationException e) {
				Log.d("content_provider", "cannot init db from this context", e);
			}
		}
		return mDataHelper;
	}

	/**
	 * @see android.content.ContentProvider#query(Uri,String[],String,String[],String)
	 */
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		DataHelper helper = getDataHelper();

		if (helper == null) {
			Log.e("content_provider", "data helper not set");
			return null;
		}

		switch (URI_MATCHER.match(uri)) {
		case LIVE_FOLDER_FAVS: {
			// Liste des gares favorites
			List<String> noms = new ArrayList<String>();
			SharedPreferences prefs = getContext().getSharedPreferences(Util.PREFS_FAVORIS_GARE, Context.MODE_PRIVATE);
			Map<String, ?> allPrefs = prefs.getAll();
			for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
				boolean isFav = (Boolean) entry.getValue();
				String nom = entry.getKey();
				if (isFav) {
					noms.add(nom);
				}
			}

			Cursor c = helper.selectForLiveFolder(noms.toArray(new String[0]), "nom ASC");
			c.setNotificationUri(getContext().getContentResolver(), uri);

			return c;
		}
		case LIVE_FOLDER_GEO: {
			// Récupérer la localisation
			LocationManager locManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
			String provider = "network";
			if (locManager.isProviderEnabled(provider)) {
				Location location = locManager.getLastKnownLocation(provider);
				double latitude = location.getLatitude();
				double longitude = location.getLongitude();

				SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
				int default_radius_km = getContext().getResources().getInteger(R.string.default_radiuskm);
				int radius_km = Integer.parseInt(preferences.getString(getContext().getString(R.string.pref_radiuskm), String.valueOf(default_radius_km)));

				double latitude_radians = latitude * (Math.PI / 180);
				double latitude_delta = radius_km / Util.ONE_DEGREE_LAT_KM;
				double longitude_delta = radius_km / Math.abs(Math.cos(latitude_radians) * Util.ONE_DEGREE_LAT_KM);
				double latitude_min = latitude - latitude_delta;
				double latitude_max = latitude + latitude_delta;
				double longitude_min = longitude - longitude_delta;
				double longitude_max = longitude + longitude_delta;

				List<String> noms = helper.selectInBox(latitude_min, latitude_max, longitude_min, longitude_max);

				// TODO Tri par distance
				Cursor c = helper.selectForLiveFolder(noms.toArray(new String[0]), "nom ASC");
				c.setNotificationUri(getContext().getContentResolver(), uri);

				return c;
			} else {
				Toast.makeText(getContext(), "Erreur : gÃ©olocalisation indisponible !", Toast.LENGTH_LONG).show();

				return null;
			}
		}
		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
	}

	/**
	 * @see android.content.ContentProvider#delete(Uri,String,String[])
	 */
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		throw new UnsupportedOperationException("readonly content provider");
	}

	/**
	 * @see android.content.ContentProvider#insert(Uri,ContentValues)
	 */
	public Uri insert(Uri uri, ContentValues values) {
		throw new UnsupportedOperationException("readonly content provider");
	}

	/**
	 * @see android.content.ContentProvider#update(Uri,ContentValues,String,String[])
	 */
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		throw new UnsupportedOperationException("readonly content provider");
	}

}
