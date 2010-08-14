package com.naholyr.android.horairessncf;

import android.app.ListActivity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.naholyr.android.horairessncf.view.ListeGaresAdapter;

public class MainActivity extends ListActivity {

	protected Cursor mCursor;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);
		Uri uri = Gare.Gares.CONTENT_URI;// Uri.parse("content://" +
		mCursor = this.getContentResolver().query(uri, null, null, null, null);
		startManagingCursor(mCursor);
		ListAdapter adapter = new ListeGaresAdapter(this, mCursor);
		setListAdapter(adapter);

		getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
				Log.d("id", String.valueOf(id));
			}
		});

		getListView().setHapticFeedbackEnabled(true);
		registerForContextMenu(getListView());
	}

	@Override
	public void onCreateContextMenu(android.view.ContextMenu menu, View v, android.view.ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.gare_context, menu);
		// Favoris ?
		String nom = Gare.getNom(this, ((AdapterContextMenuInfo) menuInfo).id);
		Gare.Favorites favs = Gare.getFavorites(this);
		if (nom != null) {
			if (favs.has(nom)) {
				menu.findItem(R.id.add_fav).setVisible(false);
			} else {
				menu.findItem(R.id.remove_fav).setVisible(false);
			}
		} else {
			menu.findItem(R.id.add_fav).setVisible(false);
			menu.findItem(R.id.remove_fav).setVisible(false);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		long id = ((AdapterContextMenuInfo) item.getMenuInfo()).id;
		switch (item.getItemId()) {
			case R.id.add_fav:
			case R.id.remove_fav:
				return ((AdapterContextMenuInfo) item.getMenuInfo()).targetView.findViewById(R.id.favicon).performClick();
			case R.id.show_gmap:
				showGoogleMap(id);
				return true;
			case R.id.show_departs:
				showProchainsDeparts(id);
				return true;
			case R.id.itineraire_from:
			case R.id.itineraire_to:
				Toast.makeText(this, "Fonctionnalité indisponible pour le moment", Toast.LENGTH_LONG).show();
				return false;
			default:
				return super.onContextItemSelected(item);
		}
	}

	private void showProchainsDeparts(long id) {
		Log.d("action", "Prochains départs");
		/*
		 * Intent intent = new Intent(getContext(),
		 * ProchainsDepartsActivity.class);
		 * intent.putExtra(ProchainsDepartsActivity.EXTRA_NOM_GARE,
		 * gare.getNom());
		 * intent.putExtra(ProchainsDepartsActivity.EXTRA_CALLED_FROM_MAIN_ACTIVITY
		 * , true); getContext().startActivity(intent);
		 */
	}

	private void showGoogleMap(long id) {
		Log.d("action", "Google Maps");
		/*
		 * Intent intent = new Intent(getContext(), MapActivity.class);
		 * intent.putExtra(MapActivity.EXTRA_LATITUDE, gare.getLatitude());
		 * intent.putExtra(MapActivity.EXTRA_LONGITUDE, gare.getLongitude());
		 * getContext().startActivity(intent);
		 */
	}

}