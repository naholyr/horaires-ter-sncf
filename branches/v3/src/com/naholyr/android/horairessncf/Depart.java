package com.naholyr.android.horairessncf;

import android.net.Uri;
import android.provider.BaseColumns;

import com.naholyr.android.horairessncf.providers.DepartsContentProvider;

public class Depart implements BaseColumns {

	public static final String CONTENT_TYPE = "vnd.android.cursor.item/vnd.horairessncf.depart";

	public static final String ORIGINE = "origine";
	public static final String DESTINATION = "destination";
	public static final String HEURE_DEPART = "heure_depart";
	public static final String HEURE_ARRIVEE = "heure_arrivee";
	public static final String RETARD = "retard";
	public static final String MOTIF_RETARD = "motif_retard";
	public static final String QUAI = "quai";

	public static final class Departs {
		public static final Uri CONTENT_URI = Uri.parse("content://" + DepartsContentProvider.AUTHORITY + "/departs");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.horairessncf.depart";
	}

}
