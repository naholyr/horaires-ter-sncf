package com.naholyr.android.horairessncf.activity;

import org.acra.ErrorReporter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.SearchRecentSuggestions;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.naholyr.android.horairessncf.Common;
import com.naholyr.android.horairessncf.Gare;
import com.naholyr.android.horairessncf.R;
import com.naholyr.android.horairessncf.data.GaresSearchSuggestionsProvider;
import com.naholyr.android.horairessncf.data.UpdateService;
import com.naholyr.android.horairessncf.ui.AboutDialog;
import com.naholyr.android.horairessncf.ui.ListeGaresAdapter;
import com.naholyr.android.ui.QuickActionWindow;

public class GaresActivity extends ListActivity {

	private static final int REQUEST_UPDATE_STATUS = 1;

	public static final String ACTION_GEOLOCATION = "geolocation";
	public static final String ACTION_FAVORITES = "favorites";
	public static final String ACTION_LAST_HOME = "last";
	public static final String ACTION_SEARCH = Intent.ACTION_SEARCH;

	public static final String EXTRA_DISPLAY_MODE = "mode";

	private static final int DIALOG_ABOUT = 1;
	private static final int DIALOG_PAYPAL = 2;
	private static final int DIALOG_GEOLOCATION_FAILED = 3;

	private static final String PREF_LAST_HOME = "last_home";

	private double mLatitude = 0;
	private double mLongitude = 0;

	private String mAction;

	private SharedPreferences mPreferences;

	@Override
	protected ListAdapter getAdapter(Cursor c) {
		startManagingCursor(c);
		if (mLatitude != 0 && mLongitude != 0) {
			return new ListeGaresAdapter(this, c, mLatitude, mLongitude);
		} else {
			return new ListeGaresAdapter(this, c, mLatitude, mLongitude);
		}
	}

	@Override
	protected int getLayout() {
		return R.layout.gares;
	}

	@Override
	protected Cursor queryCursor() throws LocationException {
		// All cases : retrieve current location (very fast, last known location
		// by network)
		// Special case : in geolocation mode, we rethrow the exception, to show
		// dialog
		// Other cases simply won't show distance
		try {
			getNetworkLocation();
		} catch (LocationException e) {
			if (ACTION_GEOLOCATION.equals(mAction)) {
				throw e;
			}
		}
		// Build content provider URI
		Cursor c;
		if (ACTION_FAVORITES.equals(mAction)) {
			// Favorites
			c = Gare.retrieveFavorites(this, null);
		} else if (ACTION_SEARCH.equals(mAction)) {
			// Search
			String keywords = getIntent().getStringExtra(SearchManager.QUERY);
			String limit = mPreferences.getString(getString(R.string.pref_nbgares), getString(R.string.default_nbgares));
			c = Gare.retrieveByKeywords(this, keywords, limit);
		} else { // Default : ACTION_GEOLOCATION
			// Try geolocation, then build URI with location information
			int rayon = Integer.parseInt(mPreferences.getString(getString(R.string.pref_radiuskm), getString(R.string.default_radiuskm)));
			String limit = mPreferences.getString(getString(R.string.pref_nbgares), getString(R.string.default_nbgares));
			int latE6 = (int) (mLatitude * 1000000);
			int lonE6 = (int) (mLongitude * 1000000);
			boolean favsFirst = mPreferences.getBoolean(getString(R.string.pref_favsfirst), true);
			c = Gare.retrieveByLocation(this, latE6, lonE6, rayon, limit, favsFirst);
		}
		// Special case ACTION_SEARCH : store user's query
		if (ACTION_SEARCH.equals(mAction)) {
			String query = getIntent().getStringExtra(SearchManager.QUERY);
			SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this, GaresSearchSuggestionsProvider.AUTHORITY, GaresSearchSuggestionsProvider.MODE);
			int count = c.getCount();
			String line2 = null;
			// 2 lines : line 2 is hint about search result
			if (count == 0) {
				line2 = getString(R.string.nb_results_0);
			} else if (count == 1) {
				c.moveToFirst();
				line2 = c.getString(c.getColumnIndexOrThrow(Gare.NOM));
			} else {
				line2 = getString(R.string.nb_results_more).replace("%d", String.valueOf(count));
			}
			suggestions.saveRecentQuery(query, line2);
		}
		// Special case ACTION_FAVORITES : data can be empty but no need for
		// initialization
		if ((ACTION_FAVORITES.equals(mAction) || ACTION_SEARCH.equals(mAction)) && c.getCount() == 0) {

			fixEmptyResults();
		}
		return c;
	}

	@Override
	protected void onQueryFailure(Throwable e) {
		e.printStackTrace();
		if (e instanceof LocationException) {
			showDialog(DIALOG_GEOLOCATION_FAILED);
		} else {
			Toast.makeText(this, "La recherche a échoué de manière inattendue !", Toast.LENGTH_LONG);
			ErrorReporter.getInstance().handleException(e);
			finish();
		}
	}

	private void fixEmptyResults() {
		Cursor cAll = Gare.query(this, null, null);
		int count = 0;
		if (cAll != null) {
			count = cAll.getCount();
		}
		if (count != 0) {
			// We have data, just no favorite
			if (ACTION_FAVORITES.equals(mAction)) {
				((TextView) findViewById(R.id.txt_no_data_gares)).setText(R.string.no_data_favorites);
			} else if (ACTION_SEARCH.equals(mAction)) {
				((TextView) findViewById(R.id.txt_no_data_gares)).setText(R.string.no_data_search);
			}
			findViewById(R.id.txt_no_data_gares).setVisibility(View.VISIBLE);
			findViewById(R.id.txt_no_data_gares_more).setVisibility(View.VISIBLE);
			findViewById(R.id.button_init_data).setVisibility(View.GONE);
		} else {
			// We really have no data
			findViewById(R.id.txt_no_data_gares).setVisibility(View.GONE);
			findViewById(R.id.txt_no_data_gares_more).setVisibility(View.GONE);
			findViewById(R.id.button_init_data).setVisibility(View.VISIBLE);
			findViewById(R.id.button_init_data).setEnabled(true);
		}
	}

	@Override
	protected void requestWindowFeatures() {
		super.requestWindowFeatures();
		requestWindowFeature(Window.FEATURE_NO_TITLE);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		Log.d(Common.TAG, "onCreate");
		// Preferences
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		// Intent action
		final Intent queryIntent = getIntent();
		mAction = queryIntent.getAction();
		// Define last action and store the requested one
		if (!ACTION_FAVORITES.equals(mAction) && !ACTION_GEOLOCATION.equals(mAction) && !ACTION_SEARCH.equals(mAction)) {
			String prefAction = mPreferences.getString(getString(R.string.pref_home), ACTION_LAST_HOME);
			if (prefAction.equals(ACTION_LAST_HOME)) {
				mAction = mPreferences.getString(PREF_LAST_HOME, getString(R.string.default_last_home));
			} else {
				mAction = prefAction;
			}
		}
		// Create activity
		super.onCreate(savedInstanceState);
		// Store last home for later access
		mPreferences.edit().putString(PREF_LAST_HOME, mAction).commit();
		// Action Bar
		if (ACTION_FAVORITES.equals(mAction)) {
			findViewById(R.id.action_bar_favorites).setVisibility(View.GONE);
			((ImageView) findViewById(R.id.title1)).setImageResource(R.drawable.title1_favorites);
			((ImageView) findViewById(R.id.title2)).setImageResource(R.drawable.title2_favorites);
		} else if (ACTION_GEOLOCATION.equals(mAction)) {
			findViewById(R.id.action_bar_geolocation).setVisibility(View.GONE);
			((ImageView) findViewById(R.id.title1)).setImageResource(R.drawable.title1_geolocation);
			((ImageView) findViewById(R.id.title2)).setImageResource(R.drawable.title2_geolocation);
		} else if (ACTION_SEARCH.equals(mAction)) {
			((ImageView) findViewById(R.id.title1)).setImageResource(R.drawable.title1_search);
			((ImageView) findViewById(R.id.title2)).setImageResource(R.drawable.title2_search);
		}
		findViewById(R.id.action_bar_geolocation).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showGares(ACTION_GEOLOCATION);
				finish();
			}
		});
		findViewById(R.id.action_bar_favorites).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showGares(ACTION_FAVORITES);
				finish();
			}
		});
		findViewById(R.id.action_bar_search).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startSearch(null, false, null, false);
			}
		});
		// Data initialization
		findViewById(R.id.button_init_data).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showUpdate();
				v.setEnabled(false);
			}
		});
		// Check updates now
		if (!mPreferences.getBoolean(getString(R.string.pref_disable_auto_update), false)) {
			UpdateService.scheduleNow(getApplicationContext());
		}
	}

	@Override
	protected QuickActionWindow getQuickActionWindow(final View anchor, final int position, final long id) {
		return Common.getQuickActionWindow(this, Common.GARE, id);
	}

	private void showGares(String action) {
		Intent intent = new Intent(GaresActivity.this, GaresActivity.class);
		intent.setAction(action);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(intent);
	}

	private void showUpdate() {
		Intent intent = new Intent(this, UpdateActivity.class);
		startActivityForResult(intent, REQUEST_UPDATE_STATUS);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case REQUEST_UPDATE_STATUS: {
				switch (resultCode) {
					case UpdateActivity.RESULT_OK:
						Toast.makeText(this, "Mise à jour terminée avec succès", Toast.LENGTH_SHORT).show();
						getCursor().requery();
						// FIXME put that in a content observer ?
						fixEmptyResults();
						break;
					case UpdateActivity.RESULT_CANCELED:
						Toast.makeText(this, "Mise à jour annulée par l'utilisateur", Toast.LENGTH_SHORT).show();
						// Re-enable button
						findViewById(R.id.button_init_data).setEnabled(true);
						break;
					case UpdateActivity.RESULT_ERROR:
						Toast.makeText(this, "Echec de la mise à jour", Toast.LENGTH_LONG).show();
						// Re-enable button
						findViewById(R.id.button_init_data).setEnabled(true);
						break;
					case UpdateActivity.RESULT_NO_UPDATE:
						Toast.makeText(this, "Aucune mise à jour à effectuer", Toast.LENGTH_LONG).show();
						break;
				}
				break;
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.gares, menu);
		if (mPreferences.getBoolean(getString(R.string.pref_disable_auto_update), false)) {
			MenuItem item = menu.findItem(R.id.menu_update);
			if (item != null) {
				item.setVisible(true);
			}
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_about: {
				showDialog(DIALOG_ABOUT);
				return true;
			}
			case R.id.menu_preferences: {
				Intent intent = new Intent(this, PreferencesActivity.class);
				startActivity(intent);
				return true;
			}
			case R.id.menu_update: {
				showUpdate();
			}
			case R.id.menu_paypal: {
				showDialog(DIALOG_PAYPAL);
			}
			default:
				return false;
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
			case DIALOG_ABOUT: {
				return AboutDialog.create(this);
			}
			case DIALOG_PAYPAL: {
				AlertDialog.Builder builder = new AlertDialog.Builder(this)
						.setTitle("Faire un don")
						.setIcon(R.drawable.icon)
						.setMessage(
								"Vous allez être redirigé vers le site PayPal...\n\nNotez que pour supporter le développement, vous pouvez aussi télécharger les plugins payants (et utiles !) sur l'Android Market :)");
				builder.setNegativeButton("Annuler", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				});
				builder.setCancelable(true).setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						Toast.makeText(getApplicationContext(), "Tant pis, une prochaine fois :)", Toast.LENGTH_LONG).show();
					}
				});
				builder.setNeutralButton("Voir les plugins", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Uri uri = Uri.parse("market://search?q=Plugin+Horaires+TER+SNCF");
						try {
							startActivity(new Intent(Intent.ACTION_VIEW, uri));
						} catch (ActivityNotFoundException e) {
							Toast.makeText(getApplicationContext(), "L'Android Market n'a pas pu être démarré. Vérifiez qu'il est bien inclus dans votre système !",
									Toast.LENGTH_LONG).show();
						}
					}
				});
				builder.setPositiveButton("Continuer", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Toast.makeText(getApplicationContext(), "Merci d'avance de m'aider à continuer !", Toast.LENGTH_LONG).show();
						Uri uri = Uri
								.parse("https://www.paypal.com/fr/cgi-bin/webscr?cmd=_xclick&business=naholyr%40yahoo.fr&item_name=Nicolas+Chambrier+pour+Horaires+TER+SNCF&currency_code=EUR");
						startActivity(new Intent(Intent.ACTION_VIEW, uri));
					}
				});
				return builder.create();
			}
			case DIALOG_GEOLOCATION_FAILED: {
				AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle("Géolocalisation échouée").setIcon(R.drawable.icon).setMessage(
						"La géolocalisation a échoué. Souhaitez-vous basculer vers la liste des gares favorites, ou effectuer une recherche ?");
				builder.setNegativeButton("Quitter", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				});
				builder.setCancelable(true).setOnCancelListener(new DialogInterface.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface dialog) {
						finish();
					}
				});
				builder.setNeutralButton("Recherche", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						startSearch(null, false, null, false);
					}
				});
				builder.setPositiveButton("Favorites", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						showGares(ACTION_FAVORITES);
					}
				});
				return builder.create();
			}
		}
		return super.onCreateDialog(id);
	}

	@SuppressWarnings("serial")
	public static class LocationException extends Exception {
	}

	private void getNetworkLocation() throws LocationException {
		String provider = LocationManager.NETWORK_PROVIDER;
		LocationManager locManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		if (!locManager.isProviderEnabled(provider)) {
			if (Common.DEBUG) {
				mLatitude = Common.DEBUG_LATITUDE;
				mLongitude = Common.DEBUG_LONGITUDE;
			} else {
				throw new LocationException();
			}
		} else {
			Location location = locManager.getLastKnownLocation(provider);
			if (location == null) {
				throw new LocationException();
			} else {
				mLatitude = location.getLatitude();
				mLongitude = location.getLongitude();
			}
		}
	}

}
