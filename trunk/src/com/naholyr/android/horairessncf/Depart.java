package com.naholyr.android.horairessncf;

import java.util.HashMap;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import com.naholyr.android.horairessncf.data.DepartsContentProvider;

public class Depart implements BaseColumns {

	public static final String CONTENT_TYPE = "vnd.android.cursor.item/vnd.horairessncf.depart";

	public static final String ORIGINE = "origine";
	public static final String DESTINATION = "destination";
	public static final String HEURE_DEPART = "heure_depart";
	public static final String HEURE_ARRIVEE = "heure_arrivee";
	public static final String RETARD = "retard";
	public static final String MOTIF_RETARD = "motif_retard";
	public static final String QUAI = "quai";
	public static final String TYPE = "type";
	public static final String NUMERO = "numero";

	public static final class Departs {
		public static final Uri CONTENT_URI = Uri.parse("content://" + DepartsContentProvider.AUTHORITY + "/departs");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.horairessncf.depart";
	}

	public static Cursor query(Context context, String appendPath, HashMap<String, String> queryParameters) {
		Uri uri = Departs.CONTENT_URI;

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
		return query(context, String.valueOf(id), null);
	}

	public static Cursor retrieveByGare(Context context, long idGare, Integer limit) {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("id", String.valueOf(idGare));
		if (limit != null) {
			params.put("limit", String.valueOf(limit));
		}

		return query(context, "par-gare", params);
	}

	public static Cursor retrieveByGare(Context context, String nomGare, Integer limit) {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("nom", nomGare);
		if (limit != null) {
			params.put("limit", String.valueOf(limit));
		}

		return query(context, "par-gare", params);
	}

}
