package com.naholyr.android.horairessncf.activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
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

	public static final String EXTRA_LATITUDE = "latitude";
	public static final String EXTRA_LONGITUDE = "longitude";

	public static final int MAX_MARKERS = 100;

	private MapView mMapView;
	private OverlayManager mOverlayManager;
	private ManagedOverlay mGaresOverlay;
	private MyLocationOverlay mMyLocationOverlay;
	private DataHelper mDataHelper;
	private Button mBoutonGare;
	private TextView mTexteRechercheGare;
	private Button mBoutonRechercheGare;
	private SeekBar mSeekBar;

	private boolean mDisabledMyLocation = false;

	private boolean mPersistentZoomControls = false;
	private final Handler mZoomControlHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			((ViewGroup) findViewById(R.id.layout_controls)).setVisibility(msg.what);
			((ViewGroup) findViewById(R.id.layout_recherche)).setVisibility(msg.what);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		getWindow().requestFeature(Window.FEATURE_PROGRESS);
		getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		setProgressBarVisibility(true);

		setContentView(R.layout.maps);

		mBoutonGare = (Button) findViewById(R.id.nom_gare);
		mTexteRechercheGare = (TextView) findViewById(R.id.text_address);
		mBoutonRechercheGare = (Button) findViewById(R.id.btn_search);
		mSeekBar = (SeekBar) findViewById(R.id.zoombar);
		mMapView = (MapView) findViewById(R.id.mapview);
		mOverlayManager = new OverlayManager(getApplication(), mMapView);

		Toast.makeText(this, "Rappel : N'oubliez pas votre touche \"menu\" :) pour les options avancées", Toast.LENGTH_SHORT).show();

		new Thread() {
			@Override
			public void run() {
				runOnUiThread(new Runnable() {
					public void run() {
						initBehaviors();
					}
				});
			}
		}.start();
	}

	protected void initBehaviors() {
		// Init overlay gares

		Drawable markerDrawable = getResources().getDrawable(R.drawable.map_marker);
		mGaresOverlay = mOverlayManager.createOverlay("gares", markerDrawable);
		mGaresOverlay.enableLazyLoadAnimation((ImageView) findViewById(R.id.map_loading));
		mMapView.getOverlays().add(mGaresOverlay);

		// Contrôle du zoom

		mMapView.setBuiltInZoomControls(false);
		mSeekBar.setMax(mMapView.getMaxZoomLevel() - 1);
		mSeekBar.setProgress(mMapView.getZoomLevel() - 1);
		mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			public void onStopTrackingTouch(SeekBar seekBar) {
				mMapView.getController().setZoom(seekBar.getProgress() + 1);
				mGaresOverlay.invokeLazyLoad(1000);
			}

			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			}
		});

		// Data

		try {
			mDataHelper = DataHelper.getInstance(getApplication());
		} catch (IOException e) {
			Util.showError(this, "Erreur lors de l'accès à la base de données ! Essayez de redémarrer l'application SVP.", e);
			return;
		}

		// Gares

		mGaresOverlay.setLazyLoadCallback(new LazyLoadCallback() {
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
									public void run() {
										Toast.makeText(MapActivity.this, "Trop de gares inclus dans la zone visible (+ de " + MAX_MARKERS + ")", Toast.LENGTH_LONG).show();
									}
								});
								break;
							}
							GeoPoint p = new GeoPoint((int) (cursor.getDouble(2) * 1000000), (int) (cursor.getDouble(3) * 1000000));
							items.add(new ManagedOverlayItem(p, cursor.getString(0), cursor.getString(1)));
						} while (cursor.moveToNext());
					}
					cursor.close();
				}
				return items;
			}
		});

		mGaresOverlay.setOnOverlayGestureListener(new ManagedOverlayGestureDetector.OnOverlayGestureListener() {

			public boolean onZoom(ZoomEvent arg0, ManagedOverlay arg1) {
				return false;
			}

			public boolean onSingleTap(MotionEvent arg0, ManagedOverlay arg1, GeoPoint arg2, ManagedOverlayItem item) {
				if (item != null) {
					mBoutonGare.setText(item.getTitle());
					return true;
				} else {
					return false;
				}
			}

			public boolean onScrolled(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3, ManagedOverlay arg4) {
				return false;
			}

			public void onLongPressFinished(MotionEvent event, ManagedOverlay overlay, GeoPoint point, ManagedOverlayItem item) {
				// Non utilisé : buggé :(
			}

			public void onLongPress(MotionEvent event, ManagedOverlay overlay) {
				// Non utilisé : buggé :(
			}

			public boolean onDoubleTap(MotionEvent event, ManagedOverlay overlay, GeoPoint point, ManagedOverlayItem item) {
				mMapView.getController().animateTo(point);
				mMapView.getController().zoomIn();
				mSeekBar.setProgress(mMapView.getZoomLevel() - 1);
				return true;
			}
		});

		mOverlayManager.populate();

		// My Location

		mMyLocationOverlay = new MyLocationOverlay(getApplicationContext(), mMapView);
		mMapView.getOverlays().add(mMyLocationOverlay);

		// Zone "gare sélectionnée"

		mBoutonGare.setClickable(true);
		mBoutonGare.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				final CharSequence nom = mBoutonGare.getText();
				if (nom == null || nom.toString().trim().length() == 0) {
					Toast.makeText(v.getContext(), "Sélectionnez une gare", Toast.LENGTH_SHORT).show();
				} else {
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
				}
			}
		});

		// Géolocalisation

		final Geocoder geocoder = new Geocoder(this);
		mBoutonRechercheGare.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				// Cacher le clavier :)
				InputMethodManager mgr = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				mgr.hideSoftInputFromWindow(mTexteRechercheGare.getWindowToken(), 0);
				// Effectuer la recherche
				final String adresse = mTexteRechercheGare.getText().toString().trim();
				if (adresse.equals("")) {
					Toast.makeText(MapActivity.this, "Veuillez entrer une adresse", Toast.LENGTH_SHORT).show();
				} else {
					final ProgressDialog dialog = ProgressDialog.show(MapActivity.this, "Recherche", "Veuillez patienter...", true);
					new Thread() {
						@Override
						public void run() {
							try {
								List<Address> results = geocoder.getFromLocationName(adresse, 1);
								if (results.size() > 0) {
									Address result = results.get(0);
									final GeoPoint p = new GeoPoint((int) (result.getLatitude() * 1000000), (int) (result.getLongitude() * 1000000));
									runOnUiThread(new Runnable() {
										public void run() {
											mMapView.getController().animateTo(p);
											mMapView.getController().setZoom(15);
											mSeekBar.setProgress(14);
											mGaresOverlay.invokeLazyLoad(1000);
										}
									});
								} else {
									runOnUiThread(new Runnable() {
										public void run() {
											Toast.makeText(MapActivity.this, "Aucun résultat", Toast.LENGTH_SHORT).show();
										}
									});
								}
							} catch (IOException e) {
								runOnUiThread(new Runnable() {
									public void run() {
										Toast.makeText(MapActivity.this, "La recherche a échoué", Toast.LENGTH_SHORT).show();
									}
								});
							}
							dialog.dismiss();
						}
					}.start();
				}
			}
		});

		// Gestion des zones de contrôle

		initFadingControls();

		// Lecture des extras

		Intent intent = getIntent();
		if (intent.hasExtra(EXTRA_LATITUDE) && intent.hasExtra(EXTRA_LONGITUDE)) {
			double latitude = intent.getDoubleExtra(EXTRA_LATITUDE, 0);
			double longitude = intent.getDoubleExtra(EXTRA_LONGITUDE, 0);
			GeoPoint p = new GeoPoint((int) (latitude * 1000000), (int) (longitude * 1000000));
			mMapView.getController().animateTo(p);
			mMapView.getController().setZoom(15);
			mSeekBar.setProgress(14);
			mGaresOverlay.invokeLazyLoad(1000);
		}
	}

	private void initFadingControls() {
		mZoomControlHandler.sendEmptyMessage(View.GONE);

		View.OnTouchListener fixAppearOnTouch = new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				mZoomControlHandler.removeMessages(View.GONE);
				mZoomControlHandler.sendEmptyMessage(View.VISIBLE);
				return false;
			}
		};

		View.OnTouchListener tempAppearOnTouch = new View.OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				mZoomControlHandler.removeMessages(View.GONE);
				mZoomControlHandler.sendEmptyMessage(View.VISIBLE);
				if (!mPersistentZoomControls) {
					mZoomControlHandler.sendEmptyMessageDelayed(View.GONE, 5000);
				}
				return false;
			}
		};

		mMapView.setOnTouchListener(tempAppearOnTouch);
		mSeekBar.setOnTouchListener(tempAppearOnTouch);
		mBoutonGare.setOnTouchListener(tempAppearOnTouch);
		mBoutonRechercheGare.setOnTouchListener(tempAppearOnTouch);
		mTexteRechercheGare.setOnTouchListener(fixAppearOnTouch);
	}

	private void enableMyLocation() {
		mMyLocationOverlay.enableMyLocation();
		mMyLocationOverlay.enableCompass();
		mMyLocationOverlay.runOnFirstFix(new Runnable() {
			public void run() {
				mMapView.getController().animateTo(mMyLocationOverlay.getMyLocation());
				mMapView.getController().setZoom(14);
				mSeekBar.setProgress(13);
				mGaresOverlay.invokeLazyLoad(1000);
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
		if (mMyLocationOverlay != null && !mDisabledMyLocation) {
			enableMyLocation();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mMyLocationOverlay != null && !mDisabledMyLocation) {
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
			case R.id.menu_map_persistent_controls: {
				switchCheckableMenuItem(item);
				mPersistentZoomControls = item.isChecked();
				mZoomControlHandler.sendEmptyMessage(mPersistentZoomControls ? View.VISIBLE : View.GONE);
				return true;
			}
			case R.id.menu_map_google_maps: {
				GeoPoint p = mMapView.getMapCenter();
				double latitude = ((double) p.getLatitudeE6()) / 1000000;
				double longitude = ((double) p.getLongitudeE6()) / 1000000;
				String uri = "geo:" + latitude + "," + longitude;
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uri)));
				return true;
			}
		}
		return false;
	}

}
