package com.naholyr.android.horairessncf.data;

import java.security.InvalidParameterException;

import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import com.naholyr.android.horairessncf.Depart;

public class DepartsContentProvider extends android.content.ContentProvider {

	public static final String AUTHORITY = "naholyr.horairessncf.providers.DepartsContentProvider";

	public static final int DEPARTS_PAR_ID_GARE = 0;

	public static final String[] COLUMN_NAMES = new String[] { Depart._ID, Depart.DESTINATION, Depart.HEURE_DEPART, Depart.ORIGINE, Depart.HEURE_ARRIVEE, Depart.RETARD,
			Depart.MOTIF_RETARD, Depart.QUAI };

	private static final UriMatcher sUriMatcher;
	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(AUTHORITY, "departs/#", DEPARTS_PAR_ID_GARE);
	}

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public String getType(Uri uri) {
		switch (sUriMatcher.match(uri)) {
			case DEPARTS_PAR_ID_GARE:
				return Depart.Departs.CONTENT_TYPE;
			default:
				throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
	}

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		MatrixCursor c = new MatrixCursor(COLUMN_NAMES);

		long id;
		switch (sUriMatcher.match(uri)) {
			case DEPARTS_PAR_ID_GARE:
				id = Long.valueOf(uri.getPathSegments().get(1));
				break;
			default:
				throw new InvalidParameterException();
		}
		Log.d("ID", String.valueOf(id));

		c.addRow(new Object[] { 1, "Dijon-Ville", "17h22", "Grenoble", "17h50", "20 min", "Signalisation", "J" });
		c.addRow(new Object[] { 2, "Dijon-Ville", "17h22", "Grenoble", "17h50", "20 min", "Signalisation", "J" });
		c.addRow(new Object[] { 3, "Dijon-Ville", "17h22", "Grenoble", "17h50", "20 min", "Signalisation", "J" });
		c.addRow(new Object[] { 4, "Dijon-Ville", "17h22", "Grenoble", "17h50", "20 min", "Signalisation", "J" });
		c.addRow(new Object[] { 5, "Dijon-Ville", "17h22", "Grenoble", "17h50", "20 min", "Signalisation", "J" });
		c.addRow(new Object[] { 6, "Dijon-Ville", "17h22", "Grenoble", "17h50", "20 min", "Signalisation", "J" });
		c.addRow(new Object[] { 7, "Dijon-Ville", "17h22", "Grenoble", "17h50", "20 min", "Signalisation", "J" });
		c.addRow(new Object[] { 8, "Dijon-Ville", "17h22", "Grenoble", "17h50", "20 min", "Signalisation", "J" });
		c.addRow(new Object[] { 9, "Dijon-Ville", "17h22", "Grenoble", "17h50", "20 min", "Signalisation", "J" });
		c.addRow(new Object[] { 10, "Dijon-Ville", "17h22", "Grenoble", "17h50", "20 min", "Signalisation", "J" });
		c.addRow(new Object[] { 11, "Dijon-Ville", "17h22", "Grenoble", "17h50", "20 min", "Signalisation", "J" });
		c.addRow(new Object[] { 12, "Dijon-Ville", "17h22", "Grenoble", "17h50", "20 min", "Signalisation", "J" });

		c.setNotificationUri(getContext().getContentResolver(), uri);

		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
		throw new UnsupportedOperationException();
	}

}
