package com.naholyr.android.horairessncf;

import java.util.HashMap;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import com.naholyr.android.horairessncf.data.ArretsContentProvider;

public class Arret implements BaseColumns {

	public static final String CONTENT_TYPE = "vnd.android.cursor.item/vnd.horairessncf.arret";

	public static final String NOM_GARE = "nom_gare";
	public static final String ID_GARE = "id_gare";
	public static final String HEURE = "heure";
	public static final String QUAI = "quai";

	public static final class Arrets {
		public static final Uri CONTENT_URI = Uri.parse("content://" + ArretsContentProvider.AUTHORITY + "/arrets");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.horairessncf.arret";
	}

	public static Cursor query(Context context, String appendPath, HashMap<String, String> queryParameters) {
		Uri uri = Arrets.CONTENT_URI;

		if (appendPath != null) {
			uri = Uri.withAppendedPath(uri, appendPath);
		}

		if (queryParameters != null) {
			Uri.Builder uriBuilder = uri.buildUpon();
			for (String key : queryParameters.keySet()) {
				uriBuilder.appendQueryParameter(key, queryParameters.get(key));
			}
			uri = uriBuilder.build();
		}

		return context.getContentResolver().query(uri, null, null, null, null);
	}

	public static Cursor retrieveById(Context context, long id) {
		return query(context, "par-id/" + String.valueOf(id), null);
	}

	public static Cursor retrieveByDepart(Context context, long idDepart) {
		Cursor result = null;

		Cursor c = Depart.retrieveById(context, idDepart);
		if (c != null) {
			if (c.moveToFirst()) {
				String numeroTrain = c.getString(c.getColumnIndexOrThrow(Depart.NUMERO));
				result = retrieveByNumeroTrain(context, numeroTrain);
			}
			c.close();
		}

		return result;
	}

	public static Cursor retrieveByNumeroTrain(Context context, String numeroTrain) {
		return query(context, numeroTrain, null);
	}

}
