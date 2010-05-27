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
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
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

	public static final int MAX_MARKERS = 100;

	private MapView mMapView;
	private OverlayManager mOverlayManager;
	private MyLocationOverlay mMyLocationOverlay;
	private DataHelper mDataHelper;

	private boolean mDisabledMyLocation = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().requestFeature(Window.FEATURE_PROGRESS);
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		setProgressBarVisibility(true);

		setContentView(R.layout.maps);

		mMapView = (MapView) findViewById(R.id.mapview);
		mMapView.setBuiltInZoomControls(true);

		try {
			mDataHelper = DataHelper.getInstance(getApplication());
		} catch (IOException e) {
			Util.showError(this, "Erreur lors de l'accès à la base de données ! Essayez de redémarrer l'application SVP.", e);
			return;
		}

		// Gares

		mOverlayManager = new OverlayManager(getApplication(), mMapView);
		Drawable markerDrawable = getResources().getDrawable(R.drawable.map_marker);
		ManagedOverlay managedOverlay = mOverlayManager.createOverlay("gares", markerDrawable);
		managedOverlay.enableLazyLoadAnimation((ImageView) findViewById(R.id.map_loading));

		mMapView.getOverlays().add(managedOverlay);

		managedOverlay.setLazyLoadCallback(new LazyLoadCallback() {
			@Override
			public List<ManagedOverlayItem> lazyload(GeoPoint topLeft, GeoPoint bottomRight, ManagedOverlay overlay) throws LazyLoadException {
				List<ManagedOverlayItem> items = new ArrayList<ManagedOverlayItem>();
				String latitudeMin = String.valueOf(((double) bottomRight.getLatitudeE6()) / 1000000);
				String latitudeMax = String.valueOf(((double) topLeft.getLatitudeE6()) / 1000000);
				String longitudeMin = String.valueOf(((double) topLeft.getLongitudeE6()) / 1000000);
				String longitudeMax = String.valueOf(((double) bottomRight.getLongitudeE6()) / 1000000);
				if (latitudeMax.equals(latitudeMin) && longitudeMax.equals(longitudeMin)) {
					// topLeft == bottomRight : chargement non terminé ? On
					// retente un peu plus tard
					overlay.invokeLazyLoad(500);
				} else {
					Cursor cursor = mDataHelper.getDb().query("gares", new String[] { "nom", "adresse", "latitude", "longitude" },
							"latitude BETWEEN ? AND ? AND longitude BETWEEN ? AND ?", new String[] { latitudeMin, latitudeMax, longitudeMin, longitudeMax }, null, null,
							"RANDOM() LIMIT " + (MAX_MARKERS + 1));
					int i = 0;
					if (cursor.moveToFirst()) {
						do {
							i++;
							if (i > MAX_MARKERS) {
								runOnUiThread(new Runnable() {
									@Override
									public void run() {
										Toast.makeText(MapActivity.this, "Trop de gares inclus dans la zone visible (+ de " + MAX_MARKERS + ")", Toast.LENGTH_LONG).show();
									}
								});
								break;
							}
							GeoPoint p = new GeoPoint((int) (cursor.getDouble(2) * 1000000), (int) (cursor.getDouble(3) * 1000000));
							items.add(new ManagedOverlayItem(p, cursor.getString(0), cursor.getString(1)));
						} while (cursor.moveToNext());
						cursor.close();
					}
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
					Toast.makeText(MapActivity.this, item.getTitle() + "\n(appui long pour les options)", Toast.LENGTH_SHORT);
					return true;
				}
				return false;
			}

			@Override
			public boolean onScrolled(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3, ManagedOverlay arg4) {
				return false;
			}

			@Override
			public void onLongPressFinished(MotionEvent event, ManagedOverlay overlay, GeoPoint point, ManagedOverlayItem item) {
				if (item != null) {
					final String nom = item.getTitle();
					AlertDialog.Builder dialog = new AlertDialog.Builder(MapActivity.this);
					dialog.setTitle(nom);
					dialog.setIcon(R.drawable.icon);
					dialog.setMessage("Choisissez une action pour la gare '" + nom + "'");
					dialog.setPositiveButton("Prochains départs", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							Intent intent = new Intent(MapActivity.this, ProchainsDepartsActivity.class);
							intent.putExtra(ProchainsDepartsActivity.EXTRA_NOM_GARE, nom);
							intent.putExtra(ProchainsDepartsActivity.EXTRA_CALLED_FROM_MAIN_ACTIVITY, true);
							MapActivity.this.startActivity(intent);
						}
					});
					dialog.setCancelable(true);
					dialog.setNegativeButton("Fermer", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int item) {
							dialog.cancel();
						}
					});
					dialog.show();
				} else {

				}
			}

			@Override
			public void onLongPress(MotionEvent event, ManagedOverlay overlay) {
				mMapView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
			}

			@Override
			public boolean onDoubleTap(MotionEvent event, ManagedOverlay overlay, GeoPoint point, ManagedOverlayItem item) {
				mMapView.getController().animateTo(point);
				mMapView.getController().zoomIn();
				return true;
			}
		});

		mOverlayManager.populate();

		// My Location

		mMyLocationOverlay = new MyLocationOverlay(getApplicationContext(), mMapView);
		mMapView.getOverlays().add(mMyLocationOverlay);
		enableMyLocation();
	}

	private void enableMyLocation() {
		mMyLocationOverlay.enableMyLocation();
		mMyLocationOverlay.enableCompass();
		mMyLocationOverlay.runOnFirstFix(new Runnable() {
			@Override
			public void run() {
				mMapView.getController().animateTo(mMyLocationOverlay.getMyLocation());
				mOverlayManager.getOverlay("gares").invokeLazyLoad(500);
			}
		});
		mDisabledMyLocation = false;
	}

	private void disableMyLocation() {
		mMyLocationOverlay.disableCompass();
		mMyLocationOverlay.disableMyLocation();
		mDisabledMyLocation = true;
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (!mDisabledMyLocation) {
			enableMyLocation();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (!mDisabledMyLocation) {
			disableMyLocation();
		}
	}

	private void updateCheckableMenuItemIcon(MenuItem item) {
		if (item.isCheckable()) {
			item.setIcon(item.isChecked() ? android.R.drawable.button_onoff_indicator_on : android.R.drawable.button_onoff_indicator_off);
		}
	}

	private void switchCheckableMenuItem(MenuItem item) {
		if (item.isCheckable()) {
			item.setChecked(!item.isChecked());
			updateCheckableMenuItemIcon(item);
			Toast.makeText(this, (item.isChecked() ? "Activation" : "Désactivation") + " \"" + item.getTitle() + "\"...", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.map, menu);
		for (int i = 0; i < menu.size(); i++) {
			updateCheckableMenuItemIcon(menu.getItem(i));
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_map_mylocation: {
				switchCheckableMenuItem(item);
				if (item.isChecked()) {
					enableMyLocation();
				} else {
					disableMyLocation();
				}
				return true;
			}
			case R.id.menu_map_satellite: {
				switchCheckableMenuItem(item);
				mMapView.setSatellite(item.isChecked());
				return true;
			}
			case R.id.menu_map_streetview: {
				switchCheckableMenuItem(item);
				mMapView.setStreetView(item.isChecked());
				return true;
			}
		}
		return false;
	}

}
