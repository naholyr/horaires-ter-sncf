package com.naholyr.android.horairessncf.activity;

import java.io.IOException;
import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.naholyr.android.horairessncf.DataHelper;
import com.naholyr.android.horairessncf.R;
import com.naholyr.android.horairessncf.Util;

public class MapActivity extends com.google.android.maps.MapActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().requestFeature(Window.FEATURE_PROGRESS);
		setProgressBarVisibility(true);

		setContentView(R.layout.maps);

		final MapView map = (MapView) findViewById(R.id.mapview);

		map.setBuiltInZoomControls(true);

		final GaresOverlay overlay = new GaresOverlay(this, R.drawable.map_marker);

		final DataHelper dataHelper;
		try {
			dataHelper = DataHelper.getInstance(getApplication());
		} catch (IOException e) {
			Util.showError(this, "Erreur lors de l'accès à la base de données ! Essayez de redémarrer l'application SVP.", e);
			return;
		}

		new Thread() {
			public void run() {
				final long total = dataHelper.countGares();
				Cursor cursor = dataHelper.getDb().query("gares", new String[] { "nom", "adresse", "latitude", "longitude" }, null, null, null, null, null);
				if (cursor.moveToFirst()) {
					do {
						overlay.addItem(cursor.getString(0), cursor.getString(1), cursor.getDouble(2), cursor.getDouble(3));
						runOnUiThread(new Runnable() {
							public void run() {
								Log.d("nb", "" + overlay.size());
								setProgress((int) ((overlay.size() * 10000) / total));
							}
						});
					} while (cursor.moveToNext());
					cursor.close();
					overlay.doPopulate();
					runOnUiThread(new Runnable() {
						public void run() {
							map.getOverlays().add(overlay);
							map.invalidate();
						}
					});
				}
			};
		}.start();
	}

	private final static class GaresOverlay extends ItemizedOverlay<OverlayItem> {

		private Context mContext;

		private ArrayList<OverlayItem> mOverlays = new ArrayList<OverlayItem>();

		public GaresOverlay(Context context, Drawable drawable) {
			super(boundCenterBottom(drawable));
			mContext = context;
		}

		public GaresOverlay(Context context, int drawableId) {
			this(context, context.getResources().getDrawable(drawableId));
		}

		public void addItem(String nom, String adresse, double latitude, double longitude) {
			GeoPoint p = new GeoPoint((int) (latitude * 1000000), (int) (longitude * 1000000));
			OverlayItem item = new OverlayItem(p, nom, adresse);
			mOverlays.add(item);
		}

		public void doPopulate() {
			populate();
		}

		@Override
		public int size() {
			return mOverlays.size();
		}

		@Override
		protected OverlayItem createItem(int i) {
			return mOverlays.get(i);
		}

		@Override
		protected boolean onTap(int index) {
			Toast.makeText(mContext, "Click", Toast.LENGTH_LONG).show();
			return true;
		}

	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

}
