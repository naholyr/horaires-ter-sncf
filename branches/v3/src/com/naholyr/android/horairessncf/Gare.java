package com.naholyr.android.horairessncf;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import com.naholyr.android.horairessncf.data.GaresContentProvider;

public class Gare implements BaseColumns {

	public static final String CONTENT_TYPE = "vnd.android.cursor.item/vnd.horairessncf.gare";

	public static final String NOM = "nom";
	public static final String REGION = "region";
	public static final String ADRESSE = "adresse";
	public static final String LATITUDE = "latitude";
	public static final String LONGITUDE = "longitude";
	public static final String FAVORITE = "favorite";

	public static final class Gares {
		public static final Uri CONTENT_URI = Uri.parse("content://" + GaresContentProvider.AUTHORITY + "/gares");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.horairessncf.gare";
	}

	public static final class Favorites {
		private Context mContext;

		protected Favorites(Context context) {
			mContext = context;
		}

		public boolean add(long id) {
			if (!has(id)) {
				Uri uri = Uri.withAppendedPath(Gares.CONTENT_URI, String.valueOf(id));
				ContentValues values = new ContentValues();
				values.put(FAVORITE, 1);
				mContext.getContentResolver().update(uri, values, null, null);
				return true;
			}
			return false;
		}

		public boolean has(long id) {
			Uri uri = Uri.withAppendedPath(Gares.CONTENT_URI, String.valueOf(id));
			Cursor c = mContext.getContentResolver().query(uri, new String[] { FAVORITE }, null, null, null);
			if (!c.moveToFirst()) {
				return false;
			}
			boolean isFavorite = c.getInt(c.getColumnIndex(FAVORITE)) > 0;
			c.close();
			return isFavorite;
		}

		public boolean remove(long id) {
			if (has(id)) {
				Uri uri = Uri.withAppendedPath(Gares.CONTENT_URI, String.valueOf(id));
				ContentValues values = new ContentValues();
				values.put(FAVORITE, 0);
				mContext.getContentResolver().update(uri, values, null, null);
				return true;
			}
			return false;
		}

		public long[] getAll() {
			Uri uri = Uri.withAppendedPath(Gares.CONTENT_URI, "favorites");
			Cursor c = mContext.getContentResolver().query(uri, new String[] { _ID }, null, null, null);
			ArrayList<Long> result = new ArrayList<Long>();
			if (c != null) {
				while (c.moveToNext()) {
					result.add(c.getLong(c.getColumnIndex(_ID)));
				}
			}
			long[] result_as_array = new long[result.size()];
			for (int i = 0; i < result_as_array.length; i++) {
				result_as_array[i] = result.get(i);
			}
			return result_as_array;
		}
	}

	public static ContentValues values(String nom, String region, String adresse, double latitude, double longitude) {
		ContentValues v = new ContentValues();
		v.put(_ID, (Integer) null);
		v.put(NOM, nom);
		v.put(REGION, region);
		v.put(ADRESSE, adresse);
		v.put(LATITUDE, latitude);
		v.put(LONGITUDE, longitude);

		return v;
	}

	public static ContentValues values(int id, String nom, String region, String adresse, double latitude, double longitude) {
		ContentValues v = values(nom, region, adresse, latitude, longitude);
		v.put(_ID, id);

		return v;
	}

	private static Favorites favorites = null;

	public static Favorites getFavorites(Context context) {
		if (favorites == null) {
			favorites = new Favorites(context);
		}
		return favorites;
	}

	public static String getNom(Context context, long id) {
		Cursor c = context.getContentResolver().query(Uri.withAppendedPath(Gares.CONTENT_URI, String.valueOf(id)), null, null, null, null);
		if (c.moveToFirst()) {
			String nom = c.getString(c.getColumnIndex(NOM));
			c.close();

			return nom;
		}
		return null;
	}

}
