package com.naholyr.android.horairessncf.data;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.List;
import java.util.Map;

import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.util.SparseArray;

import com.naholyr.android.horairessncf.Arret;
import com.naholyr.android.horairessncf.ws.IBrowser;
import com.naholyr.android.horairessncf.ws.JSONServerBrowser;

public class ArretsContentProvider extends android.content.ContentProvider {

	public static final String AUTHORITY = "naholyr.horairessncf.providers.ArretsContentProvider";

	public static final int ARRETS_PAR_NUMERO_TRAIN = 0;
	public static final int ARRET_PAR_ID = 1;

	public static final String[] COLUMN_NAMES = new String[] { Arret._ID, Arret.HEURE, Arret.ID_GARE, Arret.NOM_GARE, Arret.QUAI };

	private static final UriMatcher sUriMatcher;
	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(AUTHORITY, "arrets/*", ARRETS_PAR_NUMERO_TRAIN);
		sUriMatcher.addURI(AUTHORITY, "arrets/par-id/#", ARRET_PAR_ID);
	}

	private static final SparseArray<Object[]> mCachedResultsById = new SparseArray<Object[]>();

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public String getType(Uri uri) {
		switch (sUriMatcher.match(uri)) {
			case ARRETS_PAR_NUMERO_TRAIN:
				return Arret.Arrets.CONTENT_TYPE;
			case ARRET_PAR_ID:
				return Arret.CONTENT_TYPE;
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

	private static final class ArretCursor extends MyMatrixCursor {

		private IBrowser mBrowser;
		private String mNumeroTrain;

		public ArretCursor() {
			super(COLUMN_NAMES);
		}

		@Override
		public boolean requery() {
			clear();
			try {
				query();
				return true;
			} catch (IOException e) {
				// FIXME
				return false;
			}
		}

		void query(IBrowser browser, String numeroTrain) throws IOException {
			mBrowser = browser;
			mNumeroTrain = numeroTrain;
			query();
		}

		public void query() throws IOException {
			clear();
			List<Map<String, Object>> arrets = mBrowser.getArrets(mNumeroTrain);
			for (Map<String, Object> arret : arrets) {
				addRow(arretToRow(arret));
			}
		}

	}

	/**
	 * Convert a ProchainTrain.Depart to a Object[] MatrixCursor-compatible
	 * array
	 * 
	 * @param arret
	 * @param cache
	 * @return
	 */
	private static Object[] arretToRow(Map<String, Object> arret, boolean cache) {
		Object[] row = new Object[COLUMN_NAMES.length];
		for (int column_index = 0; column_index < COLUMN_NAMES.length; column_index++) {
			String column_name = COLUMN_NAMES[column_index];
			if (arret.containsKey(column_name)) {
				row[column_index] = arret.get(column_name);
			} else {
				row[column_index] = null;
			}
		}
		if (row[0] == null) {
			row[0] = 0L;
		}

		if (cache) {
			int id = Integer.valueOf(String.valueOf(row[0]));
			if (id == 0) {
				while (mCachedResultsById.indexOfKey(id) >= 0) {
					id++;
				}
				row[0] = (long) id;
			}
			mCachedResultsById.put(id, row);
		}

		return row;
	}

	/**
	 * Get row, caching result and attributing a new id
	 * 
	 * @param depart
	 * @return
	 */
	private static Object[] arretToRow(Map<String, Object> arret) {
		return arretToRow(arret, true);
	}

	/**
	 * Get row by id
	 * 
	 * @param id
	 * @return
	 */
	private static Object[] arretToRow(long id) throws IndexOutOfBoundsException {
		Object[] arret = mCachedResultsById.get((int) id, null);
		if (arret == null) {
			throw new IndexOutOfBoundsException("Invalid ID " + id);
		}

		return arret;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		ArretCursor c = new ArretCursor();

		switch (sUriMatcher.match(uri)) {
			case ARRETS_PAR_NUMERO_TRAIN:
				String numeroTrain = uri.getPathSegments().get(1);
				if (numeroTrain == null) {
					throw new IllegalArgumentException("Mandatory parameters missing : use /arrets/XXX");
				}
				IBrowser browser = getBrowserInstance();
				try {
					c.query(browser, numeroTrain);
				} catch (IOException e) {
					// FIXME Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case ARRET_PAR_ID:
				long id = Long.valueOf(uri.getPathSegments().get(2));
				c.addRow(arretToRow(id));
				break;
			default:
				throw new InvalidParameterException();
		}

		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
		throw new UnsupportedOperationException();
	}

	private static IBrowser mBrowserInstance = null;

	public static IBrowser getBrowserInstance() {
		if (mBrowserInstance == null) {
			mBrowserInstance = new JSONServerBrowser();
		}

		return mBrowserInstance;
	}

}
