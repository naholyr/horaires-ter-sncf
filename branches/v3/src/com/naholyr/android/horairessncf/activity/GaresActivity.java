package com.naholyr.android.horairessncf.activity;

import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.Toast;

import com.naholyr.android.horairessncf.Gare;
import com.naholyr.android.horairessncf.R;
import com.naholyr.android.horairessncf.Gare.Gares;
import com.naholyr.android.horairessncf.providers.GaresSearchSuggestionsProvider;
import com.naholyr.android.horairessncf.view.ListeGaresAdapter;
import com.naholyr.android.horairessncf.view.QuickActionWindow;

public class GaresActivity extends ListActivity {

	public static final int REQUEST_UPDATE_STATUS = 0;

	public static final String ACTION_GEOLOCATION = "geolocation";
	public static final String ACTION_FAVORITES = "favorites";
	public static final String ACTION_SEARCH = Intent.ACTION_SEARCH;

	public static final String EXTRA_DISPLAY_MODE = "mode";

	private String mAction;

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
		// Special case SEARCH_ACTION : store user's query
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
		// FIXME Add way for user to clear history
		// SearchRecentSuggestions suggestions = new
		// SearchRecentSuggestions(this, HelloSuggestionProvider.AUTHORITY,
		// HelloSuggestionProvider.MODE);
		// suggestions.clearHistory();
		return c;
	}

	@Override
	protected void requestWindowFeatures() {
		super.requestWindowFeatures();
		requestWindowFeature(Window.FEATURE_NO_TITLE);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Intent queryIntent = getIntent();
		mAction = queryIntent.getAction();
		// Action Bar
		if (ACTION_FAVORITES.equals(mAction)) {
			findViewById(R.id.action_bar_favorites).setVisibility(View.GONE);
			findViewById(R.id.action_bar_geolocation).setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					showGares(ACTION_GEOLOCATION);
					finish();
				}
			});
		} else if (ACTION_GEOLOCATION.equals(mAction)) {
			findViewById(R.id.action_bar_geolocation).setVisibility(View.GONE);
			findViewById(R.id.action_bar_favorites).setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					showGares(ACTION_FAVORITES);
					finish();
				}
			});
		}
		findViewById(R.id.action_bar_search).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startSearch(null, false, null, false);
			}
		});
		// Quick actions
		getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
				showPopup(view);
			}
		});
		// Data initialization
		findViewById(android.R.id.empty).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showUpdate();
			}
		});
	}

	private void showPopup(final View anchor) {
		int position = getListView().getPositionForView(anchor);
		final long id = getListView().getItemIdAtPosition(position);
		QuickActionWindow.Action[] actions = new QuickActionWindow.Action[5];
		actions[0] = new QuickActionWindow.Action(getString(R.string.action_prochains_departs), R.drawable.quick_action_prochains_departs, new View.OnClickListener() {
			public void onClick(View v) {
				showProchainsDeparts(id);
			}
		});
		int favStringId, favIconId;
		if (Gare.getFavorites(this).has(Gare.getNom(this, id))) {
			favStringId = R.string.action_remove_favorite;
			favIconId = R.drawable.quick_action_remove_favorite;
		} else {
			favStringId = R.string.action_add_favorite;
			favIconId = R.drawable.quick_action_add_favorite;
		}
		actions[1] = new QuickActionWindow.Action(getString(favStringId), favIconId, new View.OnClickListener() {
			public void onClick(View v) {
				anchor.findViewById(R.id.favicon).performClick();
			}
		});
		actions[2] = new QuickActionWindow.Action(getString(R.string.action_gmap), R.drawable.quick_action_gmap, new View.OnClickListener() {
			public void onClick(View v) {
				showGoogleMap(id);
			}
		});
		actions[3] = new QuickActionWindow.Action(getString(R.string.action_itineraire_from), R.drawable.quick_action_itineraire_from, new View.OnClickListener() {
			public void onClick(View v) {
				showItineraire(id, true);
			}
		});
		actions[4] = new QuickActionWindow.Action(getString(R.string.action_itineraire_to), R.drawable.quick_action_itineraire_to, new View.OnClickListener() {
			public void onClick(View v) {
				showItineraire(id, false);
			}
		});
		QuickActionWindow.showActions(this, actions, anchor);
	}

	private void showItineraire(long id, boolean from) {
		Toast.makeText(this, "Fonctionnalité indisponible pour le moment", Toast.LENGTH_LONG).show();
		showUpdate();
	}

	private void showProchainsDeparts(long id) {
		Intent intent = new Intent(this, DepartsActivity.class);
		intent.putExtra(DepartsActivity.EXTRA_ID, id);
		startActivity(intent);
	}

	private void showGoogleMap(long id) {
		Cursor c = getContentResolver().query(Uri.withAppendedPath(Gares.CONTENT_URI, "/" + id), null, null, null, null);
		if (c.moveToFirst()) {
			double latitude = c.getDouble(c.getColumnIndex(Gare.LATITUDE));
			double longitude = c.getDouble(c.getColumnIndex(Gare.LONGITUDE));
			Intent intent = new Intent(this, MapActivity.class);
			intent.putExtra(MapActivity.EXTRA_LATITUDE, latitude);
			intent.putExtra(MapActivity.EXTRA_LONGITUDE, longitude);
			startActivity(intent);
			c.close();
		} else {
			Toast.makeText(this, "Impossible de récupérer les informations de la gare sélectionnée", Toast.LENGTH_LONG).show();
		}
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
			case REQUEST_UPDATE_STATUS:
				switch (resultCode) {
					case UpdateActivity.RESULT_OK:
						Toast.makeText(this, "Mise à jour terminée avec succès", Toast.LENGTH_SHORT).show();
						getCursor().requery();
						break;
					case UpdateActivity.RESULT_CANCELED:
						Toast.makeText(this, "Mise à jour annulée par l'utilisateur", Toast.LENGTH_SHORT).show();
						break;
					case UpdateActivity.RESULT_ERROR:
						Toast.makeText(this, "Echec de la mise à jour", Toast.LENGTH_LONG).show();
						break;
					case UpdateActivity.RESULT_NO_UPDATE:
						Toast.makeText(this, "Aucune mise à jour à effectuer", Toast.LENGTH_LONG).show();
						break;
				}
				break;
		}
	}

}
