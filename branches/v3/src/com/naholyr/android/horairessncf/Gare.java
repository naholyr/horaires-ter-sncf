package com.naholyr.android.horairessncf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import com.naholyr.android.horairessncf.providers.GaresContentProvider;

public class Gare {

	public static final String CONTENT_TYPE = "vnd.android.cursor.item/vnd.horairessncf.gare";

	public static final String ID = "_id";
	public static final String NOM = "nom";
	public static final String REGION = "region";
	public static final String ADRESSE = "adresse";
	public static final String LATITUDE = "latitude";
	public static final String LONGITUDE = "longitude";

	public static final class Gares implements BaseColumns {
		public static final Uri CONTENT_URI = Uri.parse("content://" + GaresContentProvider.AUTHORITY + "/gares");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.horairessncf.gare";
	}

	public static final class Favorites {
		private SharedPreferences preferences;

		protected Favorites(Context context) {
			preferences = context.getSharedPreferences("favorites", Context.MODE_PRIVATE);
		}

		public boolean add(String nom) {
			if (!has(nom)) {
				return preferences.edit().putBoolean(nom, true).commit();
			}
			return false;
		}

		public boolean has(String nom) {
			return preferences.contains(nom);
		}

		public boolean remove(String nom) {
			if (has(nom)) {
				return preferences.edit().remove(nom).commit();
			}
			return false;
		}

		public List<String> getAll() {
			Map<String, ?> data = preferences.getAll();
			List<String> noms = new ArrayList<String>();
			for (Map.Entry<String, ?> item : data.entrySet()) {
				if ((Boolean) item.getValue()) {
					noms.add(item.getKey());
				}
			}
			return noms;
		}
	}

	public static ContentValues values(String nom, String region, String adresse, double latitude, double longitude) {
		ContentValues v = new ContentValues();
		v.put(ID, (Integer) null);
		v.put(NOM, nom);
		v.put(REGION, region);
		v.put(ADRESSE, adresse);
		v.put(LATITUDE, latitude);
		v.put(LONGITUDE, longitude);

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
		Cursor c = context.getContentResolver().query(Uri.withAppendedPath(Gares.CONTENT_URI, "/" + id), null, null, null, null);
		if (c.moveToFirst()) {
			return c.getString(c.getColumnIndex(NOM));
		}
		return null;
	}

}
