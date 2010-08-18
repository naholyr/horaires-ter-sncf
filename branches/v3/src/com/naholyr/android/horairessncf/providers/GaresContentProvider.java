package com.naholyr.android.horairessncf.providers;

import java.util.HashMap;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import com.google.android.maps.GeoPoint;
import com.naholyr.android.horairessncf.Gare;

public class GaresContentProvider extends android.content.ContentProvider {

	public static final String AUTHORITY = "naholyr.horairessncf.providers.GaresContentProvider";

	private static final int GARES = 1;
	private static final int GARE_PAR_NOM = 2;
	private static final int GARE_PAR_ID = 3;
	private static final int GARES_PAR_GEO = 4;
	private static final int GARES_PAR_NOM = 5;
	private static final int GARES_FAVORITES = 6;

	private static final UriMatcher sUriMatcher;
	private static final HashMap<String, String> sProjectionMap;
	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(AUTHORITY, DatabaseHelper.TABLE_GARES, GARES);
		sUriMatcher.addURI(AUTHORITY, DatabaseHelper.TABLE_GARES + "/recherche/*", GARES_PAR_NOM);
		sUriMatcher.addURI(AUTHORITY, DatabaseHelper.TABLE_GARES + "/latitude/#/longitude/#/rayon/#", GARES_PAR_GEO);
		sUriMatcher.addURI(AUTHORITY, DatabaseHelper.TABLE_GARES + "/favorites", GARES_FAVORITES);
		sUriMatcher.addURI(AUTHORITY, DatabaseHelper.TABLE_GARES + "/#", GARE_PAR_ID);
		sUriMatcher.addURI(AUTHORITY, DatabaseHelper.TABLE_GARES + "/*", GARE_PAR_NOM);
		sProjectionMap = new HashMap<String, String>();
		sProjectionMap.put(Gare._ID, Gare._ID);
		sProjectionMap.put(Gare.NOM, Gare.NOM);
		sProjectionMap.put(Gare.REGION, Gare.REGION);
		sProjectionMap.put(Gare.ADRESSE, Gare.ADRESSE);
		sProjectionMap.put(Gare.LATITUDE, Gare.LATITUDE);
		sProjectionMap.put(Gare.LONGITUDE, Gare.LONGITUDE);
		sProjectionMap.put(Gare.FAVORITE, Gare.FAVORITE);
	}

	protected static DatabaseHelper mDbHelper = null;

	@Override
	public boolean onCreate() {
		mDbHelper = new DatabaseHelper(getContext());
		return true;
	}

	@Override
	public String getType(Uri uri) {
		switch (sUriMatcher.match(uri)) {
			case GARES:
			case GARES_PAR_NOM:
			case GARES_PAR_GEO:
			case GARES_FAVORITES:
				return Gare.Gares.CONTENT_TYPE;
			case GARE_PAR_ID:
			case GARE_PAR_NOM:
				return Gare.CONTENT_TYPE;
			default:
				throw new IllegalArgumentException("Unsupported URI: " + uri);
		}
	}

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		int count;

		switch (sUriMatcher.match(uri)) {
			case GARE_PAR_NOM: {
				String nomGare = uri.getPathSegments().get(1);
				String selection = Gare.NOM + " = '" + nomGare.replaceAll("'", "''") + "'";
				if (!TextUtils.isEmpty(where)) {
					selection += " AND (" + where + ")";
				}
				count = db.delete(DatabaseHelper.TABLE_GARES, selection, whereArgs);
				break;
			}
			case GARE_PAR_ID: {
				long idGare = Long.valueOf(uri.getPathSegments().get(1));
				String selection = Gare._ID + " = " + idGare;
				if (!TextUtils.isEmpty(where)) {
					selection += " AND (" + where + ")";
				}
				count = db.delete(DatabaseHelper.TABLE_GARES, selection, whereArgs);
				break;
			}
			case GARES: {
				count = db.delete(DatabaseHelper.TABLE_GARES, where, whereArgs);
				break;
			}
			case UriMatcher.NO_MATCH:
			default:
				throw new IllegalArgumentException("Unsupported URI for delete: " + uri);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		if (sUriMatcher.match(uri) != GARES) {
			throw new IllegalArgumentException("Unsupported URI for insert: " + uri);
		}

		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		long rowid = db.insert(DatabaseHelper.TABLE_GARES, Gare.NOM, values);
		if (rowid > 0) {
			return ContentUris.withAppendedId(Gare.Gares.CONTENT_URI, rowid);
		} else {
			throw new SQLException("Failed to insert row into " + uri);
		}
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder sqlBuilder = new SQLiteQueryBuilder();
		sqlBuilder.setTables(DatabaseHelper.TABLE_GARES);
		sqlBuilder.setProjectionMap(sProjectionMap);

		switch (sUriMatcher.match(uri)) {
			case GARES:
				// Nothing
				break;
			case GARE_PAR_ID:
				sqlBuilder.appendWhere(Gare._ID + " = " + Long.valueOf(uri.getPathSegments().get(1)));
				break;
			case GARE_PAR_NOM:
				sqlBuilder.appendWhere(Gare.NOM + " = '" + uri.getPathSegments().get(1).replaceAll("'", "''") + "'");
				break;
			case GARES_FAVORITES:
				sqlBuilder.appendWhere(Gare.FAVORITE + " = 1");
				break;
			case GARES_PAR_GEO:
				// FIXME Recherche géolocalisée
				break;
			case GARES_PAR_NOM: {
				String[] keywords = uri.getPathSegments().get(2).split(" +");
				sqlBuilder.appendWhere("1 = 1");
				for (String keyword : keywords) {
					String like = "%" + keyword + "%";
					sqlBuilder.appendWhere(" AND " + Gare.NOM + " LIKE ");
					sqlBuilder.appendWhereEscapeString(like);
				}
				break;
			}
		}

		if (TextUtils.isEmpty(sortOrder)) {
			sortOrder = getDefaultOrderBy(null);
		}

		SQLiteDatabase db = mDbHelper.getReadableDatabase();
		Cursor c = sqlBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);

		c.setNotificationUri(getContext().getContentResolver(), uri);

		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
		SQLiteDatabase db = mDbHelper.getWritableDatabase();
		int count;

		switch (sUriMatcher.match(uri)) {
			case GARE_PAR_NOM: {
				String nomGare = uri.getPathSegments().get(1);
				String selection = Gare.NOM + " = '" + nomGare.replaceAll("'", "''") + "'";
				if (!TextUtils.isEmpty(where)) {
					selection += " AND (" + where + ")";
				}
				count = db.update(DatabaseHelper.TABLE_GARES, values, selection, whereArgs);
				break;
			}
			case GARE_PAR_ID: {
				long idGare = Long.valueOf(uri.getPathSegments().get(1));
				String selection = Gare._ID + " = " + idGare;
				if (!TextUtils.isEmpty(where)) {
					selection += " AND (" + where + ")";
				}
				count = db.update(DatabaseHelper.TABLE_GARES, values, selection, whereArgs);
				break;
			}
			case GARES: {
				count = db.update(DatabaseHelper.TABLE_GARES, values, where, whereArgs);
				break;
			}
			case UriMatcher.NO_MATCH:
			default:
				throw new IllegalArgumentException("Unsupported URI for update: " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);

		return count;
	}

	public static String getDefaultOrderBy(GeoPoint p) {
		if (p == null) {
			return Gare.NOM;
		} else {
			// (a-a')� + (b-b')�
			return String.valueOf(p.getLatitudeE6());
		}
	}

}
