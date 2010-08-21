package com.naholyr.android.horairessncf.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.SearchRecentSuggestions;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.Toast;

import com.naholyr.android.horairessncf.Gare;
import com.naholyr.android.horairessncf.R;
import com.naholyr.android.horairessncf.providers.GaresSearchSuggestionsProvider;
import com.naholyr.android.horairessncf.service.UpdateService;
import com.naholyr.android.horairessncf.view.AboutDialog;
import com.naholyr.android.horairessncf.view.ListeGaresAdapter;
import com.naholyr.android.horairessncf.view.QuickActionWindow;

public class GaresActivity extends ListActivity {

	public static final int REQUEST_UPDATE_STATUS = 1;

	public static final String ACTION_GEOLOCATION = "geolocation";
	public static final String ACTION_FAVORITES = "favorites";
	public static final String ACTION_SEARCH = Intent.ACTION_SEARCH;
	private static final String DEFAULT_ACTION = ACTION_GEOLOCATION;

	public static final String EXTRA_DISPLAY_MODE = "mode";

	public static final int DIALOG_ABOUT = 1;
	public static final int DIALOG_PAYPAL = 2;

	private String mAction;

	private SharedPreferences mPreferences;

	@Override
	protected ListAdapter getAdapter(Cursor c) {
		return new ListeGaresAdapter(this, c);
	}

	@Override
	protected int getLayout() {
		return R.layout.gares;
	}

	@Override
	protected Cursor queryCursor() {
		// Build content provider URI
		Uri uri = Gare.Gares.CONTENT_URI;
		if (ACTION_FAVORITES.equals(mAction)) {
			uri = Uri.withAppendedPath(uri, "favorites");
		} else if (ACTION_GEOLOCATION.equals(mAction)) {
			// FIXME Geolocalisation
			double latitude = 0;
			double longitude = 0;
			int rayon = 15;
			uri = Uri.withAppendedPath(uri, "/latitude/" + latitude + "/longitude/" + longitude + "/rayon/" + rayon);
		} else if (ACTION_SEARCH.equals(mAction)) {
			String keywords = getIntent().getStringExtra(SearchManager.QUERY);
			uri = Uri.withAppendedPath(uri, "recherche/" + Uri.encode(keywords));
		}
		// Run query
		Cursor c = this.getContentResolver().query(uri, null, null, null, null);
		// Special case ACTION_SEARCH : store user's query
		if (ACTION_SEARCH.equals(getIntent().getAction())) {
			String query = getIntent().getStringExtra(SearchManager.QUERY);
			SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this, GaresSearchSuggestionsProvider.AUTHORITY, GaresSearchSuggestionsProvider.MODE);
			int count = c.getCount();
			String line2 = null;
			// 2 lines : line 2 is hint about search result
			if (count == 0) {
				line2 = getString(R.string.nb_results_0);
			} else if (count == 1) {
				c.moveToFirst();
				line2 = c.getString(c.getColumnIndex(Gare.NOM));
			} else {
				line2 = getString(R.string.nb_results_more).replace("%d", String.valueOf(count));
			}
			suggestions.saveRecentQuery(query, line2);
		}
		// Special case ACTION_FAVORITES : data can be empty but no need for
		// initialization
		if (ACTION_FAVORITES.equals(getIntent().getAction()) && c.getCount() == 0) {
			fixEmptyFavorites();
		}
		return c;
	}

	private void fixEmptyFavorites() {
		Cursor cAll = getContentResolver().query(Gare.Gares.CONTENT_URI, null, null, null, null);
		int count = 0;
		if (cAll != null) {
			count = cAll.getCount();
		}
		if (count != 0) {
			// We have data, just no favorite
			findViewById(R.id.txt_add_favorite).setVisibility(View.VISIBLE);
			findViewById(R.id.button_init_data).setVisibility(View.GONE);
		} else {
			// We really have no data
			findViewById(R.id.txt_add_favorite).setVisibility(View.GONE);
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
		super.onCreate(savedInstanceState);
		// Preferences
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		// Intent action
		final Intent queryIntent = getIntent();
		mAction = queryIntent.getAction();
		if (!ACTION_FAVORITES.equals(mAction) && !ACTION_GEOLOCATION.equals(mAction) && !ACTION_SEARCH.equals(mAction)) {
			mAction = DEFAULT_ACTION;
		}
		// Action Bar
		if (ACTION_FAVORITES.equals(mAction)) {
			findViewById(R.id.action_bar_favorites).setVisibility(View.GONE);
		} else if (ACTION_GEOLOCATION.equals(mAction)) {
			findViewById(R.id.action_bar_geolocation).setVisibility(View.GONE);
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
		// Quick actions
		getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
				showQuickActions(view);
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
			new Thread(new Runnable() {
				@Override
				public void run() {
					UpdateService.scheduleNow(getApplicationContext());
				}
			}).start();
		}
	}

	private void showQuickActions(final View anchor) {
		int position = getListView().getPositionForView(anchor);
		final long id = getListView().getItemIdAtPosition(position);

		QuickActionWindow w = QuickActionWindow.getWindow(this, R.layout.quick_action_window);

		// Embedded action : favorite
		int favStringId, favIconId;
		if (Gare.getFavorites(this).has(id)) {
			favStringId = R.string.action_remove_favorite;
			favIconId = R.drawable.quick_action_remove_favorite;
		} else {
			favStringId = R.string.action_add_favorite;
			favIconId = R.drawable.quick_action_add_favorite;
		}
		w.addAction(getString(favStringId), getResources().getDrawable(favIconId), new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				anchor.findViewById(R.id.favicon).performClick();
			}
		});

		// Advertisements for other activities : GMap & itineraires
		QuickActionWindow.AdvertisementAction[] ads = new QuickActionWindow.AdvertisementAction[] {
				new QuickActionWindow.AdvertisementAction("com.naholyr.android.horairessncf.plugins.gmap.activity.MapActivity", getResources().getDrawable(
						R.drawable.quick_action_gmap), "Voir sur une carte", "com.naholyr.android.horairessncf.plugins.gmap"),
				new QuickActionWindow.AdvertisementAction("com.naholyr.android.horairessncf.plugins.itineraire.activity.ItineraireFromActivity", getResources().getDrawable(
						R.drawable.quick_action_itineraire_from), "Partir de...", "com.naholyr.android.horairessncf.plugins.itineraire"),
				new QuickActionWindow.AdvertisementAction("com.naholyr.android.horairessncf.plugins.itineraire.activity.ItineraireToActivity", getResources().getDrawable(
						R.drawable.quick_action_itineraire_to), "Aller vers...", "com.naholyr.android.horairessncf.plugins.itineraire"), };

		// Other actions : all activities handling the content type
		final Intent pluginIntent = new Intent(Intent.ACTION_VIEW);
		pluginIntent.setType(Gare.CONTENT_TYPE);
		pluginIntent.putExtra(Gare._ID, id);
		w.addActionsForIntent(this, pluginIntent, ads);

		w.show(anchor, R.drawable.quick_actions_background_above, 30);
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
						fixEmptyFavorites();
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
				return new AlertDialog.Builder(this)
						.setTitle("Faire un don")
						.setIcon(R.drawable.icon)
						.setMessage(
								"Vous allez être redirigé vers le site PayPal...\n\nNotez que pour supporter le développement, vous pouvez aussi télécharger les plugins payants (et utiles !) sur l'Android Market :)")
						.setNegativeButton("Annuler", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.cancel();
							}
						}).setCancelable(true).setOnCancelListener(new DialogInterface.OnCancelListener() {
							@Override
							public void onCancel(DialogInterface dialog) {
								Toast.makeText(getApplicationContext(), "Tant pis, une prochaine fois :)", Toast.LENGTH_LONG).show();
							}
						}).setNeutralButton("Voir les plugins", new DialogInterface.OnClickListener() {
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
						}).setPositiveButton("Continuer", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								Toast.makeText(getApplicationContext(), "Merci d'avance de m'aider à continuer !", Toast.LENGTH_LONG).show();
								Uri uri = Uri
										.parse("https://www.paypal.com/fr/cgi-bin/webscr?cmd=_xclick&business=naholyr%40yahoo.fr&item_name=Nicolas+Chambrier+pour+Horaires+TER+SNCF&currency_code=EUR");
								startActivity(new Intent(Intent.ACTION_VIEW, uri));
							}
						}).create();
			}
		}
		return super.onCreateDialog(id);
	}

}
