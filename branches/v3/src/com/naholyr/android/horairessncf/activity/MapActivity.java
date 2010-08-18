package com.naholyr.android.horairessncf.activity;

import com.naholyr.android.horairessncf.Gare;

public class MapActivity extends com.google.android.maps.MapActivity {

	public static final String EXTRA_ID = Gare._ID;

	public static final String EXTRA_LATITUDE = "latitude";
	public static final String EXTRA_LONGITUDE = "longitude";

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

}
