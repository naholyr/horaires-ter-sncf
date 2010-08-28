package com.naholyr.android.horairessncf.data;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.List;

import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import android.util.SparseArray;

import com.naholyr.android.horairessncf.Depart;
import com.naholyr.android.horairessncf.Gare;
import com.naholyr.android.horairessncf.ws.IBrowser;
import com.naholyr.android.horairessncf.ws.JSONServerBrowser;
import com.naholyr.android.horairessncf.ws.ProchainTrain;

public class DepartsContentProvider extends android.content.ContentProvider {

	public static final String AUTHORITY = "naholyr.horairessncf.providers.DepartsContentProvider";

	public static final int DEPARTS_PAR_GARE = 0;
	public static final int DEPART_PAR_ID = 1;

	public static final int DEFAULT_LIMIT = 12;

	public static final String[] COLUMN_NAMES = new String[] { Depart._ID, Depart.TYPE, Depart.NUMERO, Depart.DESTINATION, Depart.HEURE_DEPART, Depart.ORIGINE,
			Depart.HEURE_ARRIVEE, Depart.RETARD, Depart.MOTIF_RETARD, Depart.QUAI };

	private static final UriMatcher sUriMatcher;
	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(AUTHORITY, "departs/par-gare", DEPARTS_PAR_GARE);
		sUriMatcher.addURI(AUTHORITY, "departs/#", DEPART_PAR_ID);
	}

	private static final SparseArray<ProchainTrain.Depart> mCachedResultsById = new SparseArray<ProchainTrain.Depart>();

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public String getType(Uri uri) {
		switch (sUriMatcher.match(uri)) {
			case DEPARTS_PAR_GARE:
				return Depart.Departs.CONTENT_TYPE;
			case DEPART_PAR_ID:
				return Depart.CONTENT_TYPE;
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

	private static final class DepartCursor extends MyMatrixCursor {

		private IBrowser mBrowser;
		private String mNomGare;
		private int mLimite;

		public DepartCursor() {
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

		void query(IBrowser browser, String nomGare, int limit) throws IOException {
			mBrowser = browser;
			mNomGare = nomGare;
			mLimite = limit;
			query();
		}

		public void query() throws IOException {
			// mUpdatedRows.clear();
			clear();
			SparseArray<String> gares = mBrowser.searchGares(mNomGare, mLimite);
			String selectedNomGare = null;
			int selectedIdGare = 0;
			if (gares.size() == 1) {
				selectedNomGare = gares.valueAt(0);
				selectedIdGare = gares.keyAt(0);
			} else if (gares.size() > 1) {
				// TODO select
			} else {
				// TODO not found
			}
			if (selectedNomGare != null && selectedIdGare != 0) {
				mBrowser.confirmGare(selectedIdGare);
				Log.d("WS", "query...");
				List<com.naholyr.android.horairessncf.ws.ProchainTrain.Depart> departs = mBrowser.getItems(mLimite, true);
				Log.d("WS", "...found " + departs.size() + " item(s)");
				for (ProchainTrain.Depart depart : departs) {
					addRow(departToRow(depart));
				}
			} else {
				// TODO Exception not found
			}
		}

	}

	/**
	 * Convert a ProchainTrain.Depart to a Object[] MatrixCursor-compatible
	 * array
	 * 
	 * @param depart
	 * @param cache
	 * @return
	 */
	private static Object[] departToRow(ProchainTrain.Depart depart, boolean cache) {
		int id = 0;
		if (cache) {
			int index = mCachedResultsById.indexOfValue(depart);
			if (index >= 0) {
				id = mCachedResultsById.keyAt(index);
			} else {
				while (mCachedResultsById.indexOfKey(id) >= 0) {
					id++;
				}
				mCachedResultsById.put(id, depart);
			}
		}
		String dureeRetard = null, motifRetard = null;
		List<ProchainTrain.Retard> retards = depart.getRetards();
		for (ProchainTrain.Retard retard : retards) {
			if (dureeRetard == null || dureeRetard.compareToIgnoreCase(retard.getDuree()) < 0) {
				dureeRetard = retard.getDuree();
				motifRetard = retard.getMotif();
			}
		}
		return new Object[] { (long) id, depart.getTypeLabel(), depart.getNumero(), depart.getDestination(), depart.getHeure(), depart.getOrigine(), null, dureeRetard,
				motifRetard, depart.getVoie() };
	}

	/**
	 * Get row, caching result and attributing a new id
	 * 
	 * @param depart
	 * @return
	 */
	private static Object[] departToRow(ProchainTrain.Depart depart) {
		return departToRow(depart, true);
	}

	/**
	 * Get row by id
	 * 
	 * @param id
	 * @return
	 */
	private static Object[] departToRow(long id) throws IndexOutOfBoundsException {
		ProchainTrain.Depart depart = mCachedResultsById.get((int) id, null);
		if (depart == null) {
			throw new IndexOutOfBoundsException("Invalid ID " + id);
		}
		Object[] row = departToRow(depart, false);
		row[0] = id;

		return row;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		DepartCursor c = new DepartCursor();

		int limit;
		try {
			String sLimit = uri.getQueryParameter("limit");
			limit = Integer.valueOf(sLimit);
		} catch (NumberFormatException e) {
			limit = DEFAULT_LIMIT;
		}

		switch (sUriMatcher.match(uri)) {
			case DEPARTS_PAR_GARE:
				String nomGare = uri.getQueryParameter("nom");
				if (nomGare == null) {
					String qIdGare = uri.getQueryParameter("id");
					if (qIdGare == null) {
						throw new IllegalArgumentException("Mandatory parameters missing : use /gare?id=XXX or /gare?nom=YYY");
					}
					long idGare = Long.valueOf(qIdGare);
					Cursor cGare = Gare.retrieveById(getContext(), idGare);
					if (cGare == null || !cGare.moveToFirst()) {
						throw new IllegalArgumentException("ID gare invalide ! id=" + idGare);
					}
					nomGare = cGare.getString(cGare.getColumnIndex(Gare.NOM));
				}
				IBrowser browser = getBrowserInstance();
				try {
					c.query(browser, nomGare, limit);
				} catch (IOException e) {
					// FIXME Auto-generated catch block
					e.printStackTrace();
				}
				break;
			case DEPART_PAR_ID:
				long id = Long.valueOf(uri.getPathSegments().get(1));
				c.addRow(departToRow(id));
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
