package com.naholyr.android.horairessncf.activity;

public class MapActivity extends com.google.android.maps.MapActivity {

	public static final String EXTRA_LATITUDE = "latitude";
	public static final String EXTRA_LONGITUDE = "longitude";

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

}
