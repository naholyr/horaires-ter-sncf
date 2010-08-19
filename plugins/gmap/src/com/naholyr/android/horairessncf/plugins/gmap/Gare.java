package com.naholyr.android.horairessncf.plugins.gmap;

import android.net.Uri;
import android.provider.BaseColumns;

public class Gare implements BaseColumns {

	public static final String CONTENT_TYPE = "vnd.android.cursor.item/vnd.horairessncf.gare";

	public static final String NOM = "nom";
	public static final String REGION = "region";
	public static final String ADRESSE = "adresse";
	public static final String LATITUDE = "latitude";
	public static final String LONGITUDE = "longitude";
	public static final String FAVORITE = "favorite";

	public static final class Gares {
		public static final Uri CONTENT_URI = Uri.parse("content://naholyr.horairessncf.providers.GaresContentProvider/gares");
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.horairessncf.gare";
	}

}
