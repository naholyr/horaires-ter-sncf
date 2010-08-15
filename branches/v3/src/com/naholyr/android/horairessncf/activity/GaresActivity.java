package com.naholyr.android.horairessncf.activity;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.Toast;

import com.naholyr.android.horairessncf.Gare;
import com.naholyr.android.horairessncf.R;
import com.naholyr.android.horairessncf.Gare.Gares;
import com.naholyr.android.horairessncf.view.ListeGaresAdapter;
import com.naholyr.android.horairessncf.view.QuickActionWindow;

public class GaresActivity extends ListActivity {

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
		Uri uri = Gare.Gares.CONTENT_URI;
		return this.getContentResolver().query(uri, null, null, null, null);
	}

	@Override
	protected void requestWindowFeatures() {
		super.requestWindowFeatures();
		requestWindowFeature(Window.FEATURE_NO_TITLE);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Quick actions
		getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
				showPopup(view);
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
		} else {
			Toast.makeText(this, "Impossible de récupérer les informations de la gare sélectionnée", Toast.LENGTH_LONG).show();
		}
	}

}
