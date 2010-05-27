package com.naholyr.android.horairessncf.activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.acra.ErrorReporter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager.NameNotFoundException;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.BadTokenException;
import android.widget.EditText;
import android.widget.Toast;

import com.naholyr.android.horairessncf.DataHelper;
import com.naholyr.android.horairessncf.Gare;
import com.naholyr.android.horairessncf.R;
import com.naholyr.android.horairessncf.Util;
import com.naholyr.android.horairessncf.view.ListeGaresView;

public class MainActivity extends ProgressHandlerActivity {

	private List<Gare> gares;

	private SharedPreferences preferences;
	private String versionName;

	public static final int DIALOG_SCREEN_PROGRESS = 0;
	public static final int DIALOG_PROGRESS_DATA_INIT = 1;
	public static final int DIALOG_WAIT_GARES_GEO = 2;
	public static final int DIALOG_WAIT_GARES_SEARCH = 3;
	public static final int DIALOG_WAIT_GARES_FAVORITES = 4;

	private static final int HOME_LAST_USED = 0;
	private static final int HOME_FAVORITES = 1;
	private static final int HOME_GEOLOCATION = 2;
	private static final int HOME_SEARCH = 3;

	private static final int MSG_UPDATE_LIST_DATA = 101;
	private static final int MSG_SHOW_GEOLOCATION_ERROR = 102;
	private static final int MSG_SEARCH = 103;

	private DataHelper dataHelper;
	private Location location;
	private String geolocationStatus = null;
	private Double latitude = null;
	private Double longitude = null;

	private boolean geolocationCancelled = false;
	private boolean searchCancelled = false;

	private SharedPreferences prefs_data;
	private SharedPreferences prefs_favs;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Preferences
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		prefs_data = getSharedPreferences(Util.PREFS_DATA, Context.MODE_PRIVATE);
		prefs_favs = Util.getPreferencesGaresFavories(this);

		// Window progress bar
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		// Dialogs
		createProgressDialog(DIALOG_PROGRESS_DATA_INIT, "Initialisation...",
				"Initialisation des données (cette opération ne s'effectuera qu'une seule fois, au premier lancement)...", true);
		createWaitDialog(DIALOG_WAIT_GARES_GEO, "Liste des gares...", "Calcul de votre position actuelle, et listing des gares autour de vous...", true,
				new DialogInterface.OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						geolocationCancelled = true;
						showSearch();
					}
				});
		createWaitDialog(DIALOG_WAIT_GARES_SEARCH, "Liste des gares...", "Recherche en cours...", true, new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				searchCancelled = true;
				showSearch();
			}
		});
		createProgressDialog(DIALOG_WAIT_GARES_FAVORITES, "Liste des gares...", "Récupération de la liste de vos gares favories...", true);

		// Data access helper
		try {
			dataHelper = DataHelper.getInstance(getApplication());
		} catch (IOException e) {
			Util.showError(MainActivity.this, "Erreur lors de l'accès à la base de données ! Essayez de redémarrer l'application SVP.", e);
			return;
		}
		// Check for data updates
		ErrorReporter.getInstance().addCustomData("update_hash", dataHelper.getLastUpdateHash());
		long lastUpdate = dataHelper.getLastUpdateTime();
		final boolean dataInitialization;
		if (lastUpdate == 0) {
			// No data ! Initialize data now
			new AlertDialog.Builder(this).setCancelable(true).setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					Toast.makeText(MainActivity.this, "Initialisation annulée par l'utilisateur", Toast.LENGTH_LONG).show();
					finish();
				}
			}).setPositiveButton("Charger", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					Intent intent = new Intent(MainActivity.this, InitializeDataActivity.class);
					startActivityForResult(intent, InitializeDataActivity.RESULT_UPDATED);
				}
			}).setTitle("Initialisation des données").setMessage("Aucune donnée n'a été trouvée. Validez pour télécharger les gares maintenant.").setIcon(R.drawable.icon).create()
					.show();
			dataInitialization = true;
		} else {
			dataInitialization = false;
		}

		// Layout
		setContentView(R.layout.main);

		// Buttons
		findViewById(R.id.ButtonMenu).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				openOptionsMenu();
			}
		});

		// Main thread
		new Thread() {
			public void run() {
				// Start home, eventually show about dialog if first time
				try {
					versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
				} catch (NameNotFoundException e) {
					versionName = null;
				}
				if (versionName == null || !preferences.getString("app_version", "").equals(versionName)) {
					// Show about dialog, if there is no initialization pending
					if (!dataInitialization) {
						showAbout();
					}
					if (versionName != null) {
						preferences.edit().putString("app_version", versionName).commit();
					}
				}

				// If no data initialization is pending, start home now
				if (!dataInitialization) {
					displayHome();
				}

				// Send pending error reports
				ErrorReporter.getInstance().checkAndSendReports(MainActivity.this);
			}
		}.start();
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (resultCode) {
			case InitializeDataActivity.RESULT_CANCELED: {
				Toast.makeText(this, "Mise à jour annulée par l'utilisateur", Toast.LENGTH_LONG).show();
				finish();
				break;
			}
			case InitializeDataActivity.RESULT_NO_UPDATE:
			case InitializeDataActivity.RESULT_UPDATED: {
				Toast.makeText(this, "Mise à jour terminée", Toast.LENGTH_SHORT).show();
				displayHome();
				break;
			}
			case InitializeDataActivity.RESULT_ERROR:
			default: {
				Toast.makeText(this, "Erreur lors de l'initialisation des données ! Redémarrez l'application SVP.", Toast.LENGTH_LONG).show();
				finish();
				break;
			}
		}
	}

	private final class GeolocationThread extends Thread {
		public void run() {
			runOnUiThread(new Runnable() {
				public void run() {
					setTitle("Les gares autour de...");
					setProgressBarIndeterminateVisibility(true);
				}
			});
			sendMessage(MSG_SHOW_DIALOG, DIALOG_WAIT_GARES_GEO);

			storeLastHome(HOME_GEOLOCATION);

			LocationManager locManager = (LocationManager) getSystemService(LOCATION_SERVICE);

			latitude = null;
			longitude = null;

			location = null;
			long time1 = Calendar.getInstance().getTimeInMillis();
			geolocationCancelled = false;
			while (location == null && !geolocationCancelled) {
				long time2 = Calendar.getInstance().getTimeInMillis();
				if (time2 - time1 > Util.GEOLOCATION_TIMEOUT) {
					break;
				}
				String provider = "network";
				if (!locManager.isProviderEnabled(provider)) {
					break;
				} else {
					String status = "Tentative de localisation par le réseau...";
					if (!status.equals(geolocationStatus)) {
						geolocationStatus = status;
						sendMessage(MSG_SET_DIALOG_MESSAGE, DIALOG_WAIT_GARES_GEO, geolocationStatus);
					}

					location = locManager.getLastKnownLocation(provider);

					if (location != null) {
						latitude = location.getLatitude();
						longitude = location.getLongitude();

						geolocationStatus = "Dernière position connue obtenue avec succès";
						sendMessage(MSG_SET_DIALOG_MESSAGE, DIALOG_WAIT_GARES_GEO, geolocationStatus);

						new Thread() {
							public void run() {
								Geocoder geocoder = new Geocoder(MainActivity.this);
								List<Address> addresses;
								try {
									addresses = geocoder.getFromLocation(latitude, longitude, 1);
								} catch (IOException e) {
									addresses = null;
								}
								final Address address;
								if (addresses != null && addresses.size() > 0) {
									address = addresses.get(0);
								} else {
									address = null;
								}
								runOnUiThread(new Runnable() {
									public void run() {
										if (address == null) {
											setTitle("Les gares autour de <" + latitude + "," + longitude + ">");
										} else {
											setTitle("Les gares autour de " + address.getLocality());
										}
										setProgressBarIndeterminateVisibility(false);
									}
								});
							}
						}.start();
					}
				}
			}

			if (!geolocationCancelled) {
				if (location != null) {
					(new ListeGaresThread(location)).start();
				} else {
					sendMessage(MSG_SHOW_GEOLOCATION_ERROR);
				}
			}
		}
	}

	private class ListeGaresThread extends Thread {

		private double latitude;
		private double longitude;

		public ListeGaresThread(Location location) {
			super();
			latitude = location.getLatitude();
			longitude = location.getLongitude();
		}

		public void run() {
			// Liste gares
			sendMessage(MSG_SET_DIALOG_TITLE, DIALOG_WAIT_GARES_GEO, "Les gares autour de vous...");

			if (!geolocationCancelled) {
				try {
					int defaultRadiusKm = getResources().getInteger(R.string.default_radiuskm);
					int radiusKm = Integer.parseInt(preferences.getString(getString(R.string.pref_radiuskm), String.valueOf(defaultRadiusKm)));
					if (!geolocationCancelled) {
						gares = Gare.getAll(dataHelper, latitude, longitude, radiusKm);
					}
				} catch (IOException e1) {
					gares = new ArrayList<Gare>();
				}
			}

			if (!geolocationCancelled) {
				updateListeGares(DIALOG_WAIT_GARES_GEO);
			}
		}

	}

	private class FavorisThread extends Thread {

		public void run() {
			runOnUiThread(new Runnable() {
				public void run() {
					setTitle("Mes gares favories");
					setProgressBarIndeterminateVisibility(true);
				}
			});

			storeLastHome(HOME_FAVORITES);

			gares = new ArrayList<Gare>();
			SharedPreferences prefs = getSharedPreferences(Util.PREFS_FAVORIS_GARE, Context.MODE_PRIVATE);
			Map<String, ?> allPrefs = prefs.getAll();
			for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
				boolean isFav = (Boolean) entry.getValue();
				String nom = entry.getKey();
				if (isFav) {
					try {
						Gare gare = new Gare(dataHelper, nom);
						gares.add(gare);
					} catch (IOException e) {
						Log.e("ErrorGare", e.getMessage());
					}
				}
			}

			updateListeGares(DIALOG_SCREEN_PROGRESS);
		}

	}

	private class SearchThread extends Thread {

		private String keywords;

		public SearchThread(String keywords) {
			this.keywords = keywords;
		}

		public void run() {
			runOnUiThread(new Runnable() {
				public void run() {
					setProgressBarIndeterminateVisibility(true);
					setTitle("Recherche : " + keywords);
				}
			});

			// Note : on ne stocke pas la recherche comme last_home
			// Histoire de ne pas changer le choix utilisateur à chaque
			// recherche
			// storeLastHome(HOME_SEARCH);
			storeLastSearch(keywords);

			searchCancelled = false;
			if (!searchCancelled) {
				String[] words = keywords.split(" +");
				List<String> noms = dataHelper.selectByKeywords(words);
				try {
					if (!searchCancelled) {
						gares = Gare.getAll(dataHelper, noms);
					}
				} catch (IOException e1) {
					gares = new ArrayList<Gare>();
				}
			}
			if (!searchCancelled) {
				updateListeGares(-1, DIALOG_SCREEN_PROGRESS);
			}
		}

	}

	protected void handleMessage(Message msg) {
		super.handleMessage(msg);

		switch (msg.what) {
			case MSG_UPDATE_LIST_DATA: {
				((ListeGaresView) findViewById(R.id.ListeGares)).setData(gares, latitude, longitude);
				int dialogId = msg.getData().getInt("value");
				if (dialogId > 0) {
					try {
						getDialog(dialogId).dismiss();
					} catch (BadTokenException e) {
						// Activity not running : ignore
					}
				}
				break;
			}
			case MSG_SHOW_GEOLOCATION_ERROR: {
				setProgressBarIndeterminateVisibility(false);
				Dialog waitDialog = getDialog(DIALOG_WAIT_GARES_GEO);
				if (waitDialog != null) {
					try {
						waitDialog.dismiss();
					} catch (BadTokenException e) {
						// Activity not running : ignore
					}
				}
				Util.showError(this,
						"La géolocalisation a échoué, vérifiez que vous avez activé la localisation par le réseau et que vous êtes sous couverture de votre opérateur "
								+ "avant de relancer l'application.", new Runnable() {
							public void run() {
								sendMessage(MSG_SEARCH);
							}
						});
				break;
			}
			case MSG_SEARCH: {
				showSearch();
				break;
			}
		}
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_main_geolocation: {
				(new GeolocationThread()).start();
				return true;
			}
			case R.id.menu_main_favorites: {
				(new FavorisThread()).start();
				return true;
			}
			case R.id.menu_main_search: {
				showSearch();
				return true;
			}
			case R.id.menu_main_about: {
				showAbout();
				return true;
			}
			case R.id.menu_main_preferences: {
				Intent intent = new Intent(this, PreferencesActivity.class);
				startActivity(intent);
				return true;
			}
			case R.id.menu_main_lien_termobile: {
				Uri uri = Uri.parse(Util.LINK_TERMOBILE);
				Intent i = new Intent(Intent.ACTION_VIEW, uri);
				startActivity(i);
				return true;
			}
			case R.id.menu_main_lien_garesenmouvement: {
				Uri uri = Uri.parse(Util.LINK_GAREENMOUVEMENT);
				Intent i = new Intent(Intent.ACTION_VIEW, uri);
				startActivity(i);
				return true;
			}
			case R.id.menu_main_map: {
				Intent intent = new Intent(this, MapActivity.class);
				startActivity(intent);
				return true;
			}
			default:
				return false;
		}
	}

	private void showAbout() {
		startActivity(new Intent(this, AboutActivity.class));
	}

	private void updateListeGares(int nbGares, int dialogToDismiss) {
		if (gares.size() > 0 && nbGares >= 0) {
			// Tri par éloignement + favoris en premier
			final boolean favsFirst = preferences.getBoolean(getString(R.string.pref_favsfirst), true);
			Collections.sort(gares, new Comparator<Gare>() {
				public int compare(Gare g1, Gare g2) {
					if (favsFirst) {
						if (g1.isFavori(prefs_favs) && !g2.isFavori(prefs_favs)) {
							return -1;
						} else if (!g1.isFavori(prefs_favs) && g2.isFavori(prefs_favs)) {
							return 1;
						}
					}
					if (latitude == null || longitude == null) {
						return 0;
					}
					double d1 = g1.getDistance(latitude, longitude);
					double d2 = g2.getDistance(latitude, longitude);
					return (d1 > d2) ? 1 : ((d1 < d2) ? -1 : 0);
				}
			});
			// Récupération du nombre de gares à afficher
			if (nbGares == 0) {
				int defaultNbGares = getResources().getInteger(R.string.default_nbgares);
				nbGares = Integer.parseInt(preferences.getString(getString(R.string.pref_nbgares), String.valueOf(defaultNbGares)));
			}
			nbGares = Math.min(gares.size(), nbGares);
			// Sous-ensemble
			gares = gares.subList(0, nbGares);
		}
		// Finir les barres de chargement
		sendMessage(MSG_UPDATE_LIST_DATA, dialogToDismiss);
		if (dialogToDismiss == DIALOG_SCREEN_PROGRESS) {
			runOnUiThread(new Runnable() {
				public void run() {
					setProgressBarIndeterminateVisibility(false);
				}
			});
		}
	}

	private void updateListeGares(int dialogToDismiss) {
		updateListeGares(0, dialogToDismiss);
	}

	public boolean onSearchRequested() {
		showSearch();

		return true;
	}

	private void showSearch() {
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);

		dialog.setTitle("Recherche");
		dialog.setMessage("Entrez une partie du nom de la gare recherchée :");

		// Set an EditText view to get user input
		final EditText input = new EditText(this);
		input.setSingleLine();
		input.setText(getLastSearch());
		dialog.setView(input);

		// Ok
		dialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				(new SearchThread(input.getText().toString())).start();
			}
		});

		// Cancel
		dialog.setCancelable(true);
		dialog.setNegativeButton(android.R.string.cancel, null);

		dialog.show();
	}

	private void storeLastHome(int home_val) {
		Editor editor = prefs_data.edit();
		editor.putInt(Util.PREFS_DATA_LAST_HOME, home_val);
		editor.commit();
	}

	private void storeLastSearch(String search) {
		Editor editor = prefs_data.edit();
		editor.putString(Util.PREFS_DATA_LAST_SEARCH, search);
		editor.commit();
	}

	private int getLastHome() {
		return prefs_data.getInt(Util.PREFS_DATA_LAST_HOME, getResources().getInteger(R.string.default_last_home));
	}

	private int getHome() {
		int home = Integer.valueOf(preferences.getString(getString(R.string.pref_home), getString(R.string.default_home)));
		if (home == HOME_LAST_USED) {
			home = getLastHome();
		}
		if (home == HOME_LAST_USED) {
			return getResources().getInteger(R.string.default_last_home);
		} else {
			return home;
		}
	}

	private String getLastSearch() {
		return prefs_data.getString(Util.PREFS_DATA_LAST_SEARCH, "");
	}

	private void displayHome() {
		// Start the update thread
		new InitializeDataActivity.UpdateThread(this).start();
		// Start home screen depending on preferences
		switch (getHome()) {
			case HOME_FAVORITES: {
				(new FavorisThread()).start();
				break;
			}
			case HOME_SEARCH: {
				sendMessage(MSG_SEARCH);
				break;
			}
			case HOME_GEOLOCATION:
			default: {
				(new GeolocationThread()).start();
				break;
			}
		}
	}

}
