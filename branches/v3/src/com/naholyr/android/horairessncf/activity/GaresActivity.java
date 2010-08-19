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
import com.naholyr.android.horairessncf.providers.GaresSearchSuggestionsProvider;
import com.naholyr.android.horairessncf.service.UpdateServiceManager;
import com.naholyr.android.horairessncf.view.ListeGaresAdapter;
import com.naholyr.android.horairessncf.view.QuickActionWindow;

public class GaresActivity extends ListActivity {

	public static final int REQUEST_UPDATE_STATUS = 0;

	public static final String ACTION_GEOLOCATION = "geolocation";
	public static final String ACTION_FAVORITES = "favorites";
	public static final String ACTION_SEARCH = Intent.ACTION_SEARCH;
	private static final String DEFAULT_ACTION = ACTION_GEOLOCATION;

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
		// FIXME Add way for user to clear history
		// SearchRecentSuggestions suggestions = new
		// SearchRecentSuggestions(this, HelloSuggestionProvider.AUTHORITY,
		// HelloSuggestionProvider.MODE);
		// suggestions.clearHistory();
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
		final Intent queryIntent = getIntent();
		mAction = queryIntent.getAction();
		if (!ACTION_FAVORITES.equals(mAction) && !ACTION_GEOLOCATION.equals(mAction) && !ACTION_SEARCH.equals(mAction)) {
			mAction = DEFAULT_ACTION;
		}
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
		findViewById(R.id.button_init_data).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				showUpdate();
				v.setEnabled(false);
			}
		});
		// Check updates now
		new Thread(new Runnable() {
			public void run() {
				UpdateServiceManager.start(getApplicationContext());
			}
		}).start();
	}

	private void showPopup(final View anchor) {
		int position = getListView().getPositionForView(anchor);
		final long id = getListView().getItemIdAtPosition(position);

		QuickActionWindow w = QuickActionWindow.getWindow(this);

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
			public void onClick(View v) {
				anchor.findViewById(R.id.favicon).performClick();
			}
		});

		// Other actions : all activities handling the content type
		final Intent pluginIntent = new Intent(Intent.ACTION_VIEW);
		pluginIntent.setType(Gare.CONTENT_TYPE);
		pluginIntent.putExtra(Gare._ID, id);
		w.addActionsForIntent(this, pluginIntent);

		w.show(anchor);
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
