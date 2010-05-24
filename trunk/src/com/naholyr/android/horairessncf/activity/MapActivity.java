package com.naholyr.android.horairessncf.activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.naholyr.android.horairessncf.DataHelper;
import com.naholyr.android.horairessncf.R;
import com.naholyr.android.horairessncf.Util;

import de.android1.overlaymanager.ManagedOverlay;
import de.android1.overlaymanager.ManagedOverlayGestureDetector;
import de.android1.overlaymanager.ManagedOverlayItem;
import de.android1.overlaymanager.OverlayManager;
import de.android1.overlaymanager.ZoomEvent;
import de.android1.overlaymanager.lazyload.LazyLoadCallback;
import de.android1.overlaymanager.lazyload.LazyLoadException;

public class MapActivity extends com.google.android.maps.MapActivity {

	private OverlayManager overlayManager;
	private DataHelper dataHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().requestFeature(Window.FEATURE_PROGRESS);
		setProgressBarVisibility(true);

		setContentView(R.layout.maps);

		MapView mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);

		overlayManager = new OverlayManager(getApplication(), mapView);

		try {
			dataHelper = DataHelper.getInstance(getApplication());
		} catch (IOException e) {
			Util.showError(this, "Erreur lors de l'accès à la base de données ! Essayez de redémarrer l'application SVP.", e);
			return;
		}

		Drawable markerDrawable = getResources().getDrawable(R.drawable.map_marker);
		ManagedOverlay managedOverlay = overlayManager.createOverlay("gares", markerDrawable);
		managedOverlay.enableLazyLoadAnimation((ImageView) findViewById(R.id.map_loading));

		managedOverlay.setLazyLoadCallback(new LazyLoadCallback() {
			@Override
			public List<ManagedOverlayItem> lazyload(GeoPoint topLeft, GeoPoint bottomRight, ManagedOverlay overlay) throws LazyLoadException {
				List<ManagedOverlayItem> items = new ArrayList<ManagedOverlayItem>();
				String latitudeMin = String.valueOf(((double) bottomRight.getLatitudeE6()) / 1000000);
				String latitudeMax = String.valueOf(((double) topLeft.getLatitudeE6()) / 1000000);
				String longitudeMin = String.valueOf(((double) topLeft.getLongitudeE6()) / 1000000);
				String longitudeMax = String.valueOf(((double) bottomRight.getLongitudeE6()) / 1000000);
				Cursor cursor = dataHelper.getDb().query("gares", new String[] { "nom", "adresse", "latitude", "longitude" }, "latitude BETWEEN ? AND ? AND longitude BETWEEN ? AND ?",
						new String[] { latitudeMin, latitudeMax, longitudeMin, longitudeMax }, null, null, null);
				if (cursor.moveToFirst()) {
					do {
						GeoPoint p = new GeoPoint((int) (cursor.getDouble(2) * 1000000), (int) (cursor.getDouble(3) * 1000000));
						items.add(new ManagedOverlayItem(p, cursor.getString(0), cursor.getString(1)));
					} while (cursor.moveToNext());
					cursor.close();
				}
				return items;
			}
		});

		managedOverlay.setOnOverlayGestureListener(new ManagedOverlayGestureDetector.OnOverlayGestureListener() {

			@Override
			public boolean onZoom(ZoomEvent arg0, ManagedOverlay arg1) {
				return false;
			}

			@Override
			public boolean onSingleTap(MotionEvent arg0, ManagedOverlay arg1, GeoPoint arg2, ManagedOverlayItem item) {
				if (item != null) {
					Toast.makeText(MapActivity.this, "Gare : " + item.getTitle() + "\n\n" + "Appui long pour plus d'options", Toast.LENGTH_LONG).show();
					return true;
				}
				return false;
			}

			@Override
			public boolean onScrolled(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3, ManagedOverlay arg4) {
				return false;
			}

			@Override
			public void onLongPressFinished(MotionEvent arg0, ManagedOverlay arg1, GeoPoint arg2, ManagedOverlayItem item) {
				if (item != null) {
					final String nom = item.getTitle();
					AlertDialog.Builder builder = new AlertDialog.Builder(MapActivity.this);
					builder.setTitle("Options");
					builder.setItems(new CharSequence[] { "Prochains départs" }, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							if (item == 0) {
								Intent intent = new Intent(MapActivity.this, ProchainsDepartsActivity.class);
								intent.putExtra(ProchainsDepartsActivity.EXTRA_NOM_GARE, nom);
								intent.putExtra(ProchainsDepartsActivity.EXTRA_CALLED_FROM_MAIN_ACTIVITY, true);
								MapActivity.this.startActivity(intent);
							}
						}
					});
					AlertDialog alert = builder.create();
					alert.show();
				}
			}

			@Override
			public void onLongPress(MotionEvent arg0, ManagedOverlay arg1) {
			}

			@Override
			public boolean onDoubleTap(MotionEvent arg0, ManagedOverlay arg1, GeoPoint arg2, ManagedOverlayItem arg3) {
				return false;
			}
		});

		overlayManager.populate();
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

}
