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
import com.naholyr.android.horairessncf.ws.ProchainTrain.Retard;

public class DepartsContentProvider extends android.content.ContentProvider {

	public static final String AUTHORITY = "naholyr.horairessncf.providers.DepartsContentProvider";

	public static final int DEPARTS_PAR_ID_GARE = 0;

	public static final int DEFAULT_LIMIT = 12;

	public static final String[] COLUMN_NAMES = new String[] { Depart._ID, Depart.TYPE, Depart.NUMERO, Depart.DESTINATION, Depart.HEURE_DEPART, Depart.ORIGINE,
			Depart.HEURE_ARRIVEE, Depart.RETARD, Depart.MOTIF_RETARD, Depart.QUAI };

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
				long i = 0;
				for (com.naholyr.android.horairessncf.ws.ProchainTrain.Depart depart : departs) {
					String dureeRetard = null, motifRetard = null;
					List<Retard> retards = depart.getRetards();
					for (Retard retard : retards) {
						if (dureeRetard == null || dureeRetard.compareToIgnoreCase(retard.getDuree()) < 0) {
							dureeRetard = retard.getDuree();
							motifRetard = retard.getMotif();
						}
					}
					addRow(new Object[] { ++i, depart.getTypeLabel(), depart.getNumero(), depart.getDestination(), depart.getHeure(), depart.getOrigine(), null, dureeRetard,
							motifRetard, depart.getVoie() });
				}
			} else {
				// TODO Exception not found
			}
		}

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

		long id;
		switch (sUriMatcher.match(uri)) {
			case DEPARTS_PAR_ID_GARE:
				id = Long.valueOf(uri.getPathSegments().get(1));
				Cursor cGare = Gare.retrieveById(getContext(), id);
				if (cGare == null || !cGare.moveToFirst()) {
					throw new IllegalArgumentException("ID gare invalide ! id=" + id);
				}
				String nomGare = cGare.getString(cGare.getColumnIndex(Gare.NOM));
				IBrowser browser = getBrowserInstance();
				try {
					c.query(browser, nomGare, limit);
				} catch (IOException e) {
					// FIXME Auto-generated catch block
					e.printStackTrace();
				}
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
