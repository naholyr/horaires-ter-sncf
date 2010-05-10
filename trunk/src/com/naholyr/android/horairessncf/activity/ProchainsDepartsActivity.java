package com.naholyr.android.horairessncf.activity;

import java.io.IOException;
import java.util.List;

import org.acra.ErrorReporter;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.naholyr.android.horairessncf.Gare;
import com.naholyr.android.horairessncf.GaresContentProvider;
import com.naholyr.android.horairessncf.ProchainTrain;
import com.naholyr.android.horairessncf.R;
import com.naholyr.android.horairessncf.TERMobileBrowser;
import com.naholyr.android.horairessncf.Util;
import com.naholyr.android.horairessncf.termobile.IBrowser;
import com.naholyr.android.horairessncf.view.ListeProchainsDepartsAdapter;

public class ProchainsDepartsActivity extends ProgressHandlerActivity {

	public static final String EXTRA_NOM_GARE = "nom";

	private Gare gare;
	private String nomGare;
	private int idGare;

	private static IBrowser client = null;
	private List<ProchainTrain.Depart> trains;

	private SharedPreferences preferences;
	private int nbTrains;

	protected static final int DIALOG_WAIT = 1;

	protected static final int MSG_UPDATE_LIST_DATA = 101;
	protected static final int MSG_HIDE_WARNING_NORESULT = 102;
	protected static final int MSG_SHOW_WARNING_NORESULT = 103;
	protected static final int MSG_CHOOSE_GARE = 104;

	private SparseArray<String> options;

	/**
	 * @see android.app.Activity#onCreate(Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Preferences
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		int defaultNbItems = getResources().getInteger(R.string.default_nbtrains);
		nbTrains = Integer.parseInt(preferences.getString(getString(R.string.pref_nbtrains), String.valueOf(defaultNbItems)));

		// View
		setContentView(R.layout.prochainsdeparts);

		// Refresh button
		findViewById(R.id.ButtonRefreshProchainsDeparts).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				new RefreshThread(true).start();
			}
		});

		// Dialogues de progression
		createWaitDialog(DIALOG_WAIT, "Patienter...", "Interrogation du serveur...", true);

		// Start the update thread
		new InitializeDataActivity.UpdateThread(this).start();

		// Récupération de la gare passée en extra info
		String nom = getIntent().getStringExtra(EXTRA_NOM_GARE);
		Log.d("prochains départs", "nom = " + nom);
		if (nom == null) {
			Uri uri = getIntent().getData();
			Log.d("prochains départs", "uri = " + uri.toString());
			if (uri != null) {
				switch (GaresContentProvider.URI_MATCHER.match(uri)) {
					case GaresContentProvider.PROCHAINS_DEPARTS_NOM: {
						nom = uri.getPathSegments().get(1);
						Log.d("prochains départs", "nom = " + nom);
						break;
					}
					case GaresContentProvider.PROCHAINS_DEPARTS_ROWID: {
						int rowid = Integer.valueOf(uri.getPathSegments().get(1));
						Log.d("prochains départs", "rowid = " + rowid);
						try {
							gare = new Gare(this, rowid);
						} catch (IOException e) {
							Util.showError(this, "Impossible de trouver la gare ID#" + rowid + " dans la base de données");
							return;
						}
						break;
					}
				}
			}
		}
		// Stocker le nom dans les prochains rapports d'erreur
		ErrorReporter.getInstance().addCustomData("nomGare", nom);
		if (nom != null) {
			try {
				gare = new Gare(this, nom);
			} catch (IOException e) {
				Util.showError(this, "Impossible de trouver la gare '" + nom + "' dans la base de données");
				return;
			}
		}
		if (gare == null) {
			Util.showError(this, "Paramètres de lancement insuffisants !");
			return;
		}

		nomGare = gare.getNom();
		setTitle(nomGare);

		Log.d("prochains départs", "nomGare = " + nomGare);

		// Requête
		new SearchThread().start();
	}

	private final class SearchThread extends Thread {
		@Override
		public void run() {
			sendMessage(MSG_SHOW_DIALOG, DIALOG_WAIT);
			sendMessage(MSG_SET_DIALOG_MESSAGE, DIALOG_WAIT, "Recherche de la gare...");
			sendMessage(MSG_HIDE_WARNING_NORESULT);

			boolean dismissDialog = false;

			if (gare != null) {
				try {
					if (client == null) {
						client = TERMobileBrowser.getInstance(ProchainsDepartsActivity.this, DIALOG_WAIT);
					}

					Exception error = null;
					try {
						options = client.searchGares(gare.getNom());
					} catch (Exception e) {
						error = e;
						options = new SparseArray<String>();
					}

					if (options.size() > 1) {
						// Choix de la gare
						sendMessage(MSG_CHOOSE_GARE);
					} else if (options.size() == 1) {
						// On n'a qu'une possibilité
						idGare = options.keyAt(0);
						new RefreshThread(false).start();
					} else {
						String msg = "TERMobile n'a permis de trouver aucune gare trouvée correspondant à votre demande !";
						if (error != null) {
							msg += "\n\nErreur : " + error.getLocalizedMessage();
						}
						sendMessage(MSG_SHOW_ERROR, msg);
						dismissDialog = true;
					}
					// trains = client.getItems(nbTrains, gare.getNom(),
					// idGare);
				} catch (IOException e) {
					sendMessage(MSG_SHOW_ERROR, e.getMessage());
					dismissDialog = true;
				}
			}

			if (dismissDialog) {
				getDialog(DIALOG_WAIT).dismiss();
			}
		}
	}

	private final class RefreshThread extends Thread {

		private boolean mRefresh;

		public RefreshThread(boolean refresh) {
			mRefresh = refresh;
		}

		@Override
		public void run() {
			sendMessage(MSG_HIDE_WARNING_NORESULT);
			sendMessage(MSG_SHOW_DIALOG, DIALOG_WAIT);
			sendMessage(MSG_SET_DIALOG_MESSAGE, DIALOG_WAIT, "Recherche des horaires...");

			client.confirmGare(idGare);

			try {
				trains = client.getItems(nbTrains, mRefresh);
				if (trains.size() == 0) {
					sendMessage(MSG_SHOW_WARNING_NORESULT);
				} else {
					sendMessage(MSG_UPDATE_LIST_DATA);
				}
			} catch (IOException e) {
				sendMessage(MSG_SHOW_ERROR, e.getMessage());
			}

			getDialog(DIALOG_WAIT).dismiss();
		}

	}

	@Override
	protected void handleMessage(Message msg) {
		super.handleMessage(msg);

		switch (msg.what) {
			case MSG_UPDATE_LIST_DATA: {
				ListAdapter adapter = new ListeProchainsDepartsAdapter(this, trains);
				((ListView) findViewById(R.id.ListeDeparts)).setAdapter(adapter);
				break;
			}
			case MSG_HIDE_WARNING_NORESULT: {
				findViewById(R.id.AlertProchainsDeparts).setVisibility(View.GONE);
				break;
			}
			case MSG_SHOW_WARNING_NORESULT: {
				findViewById(R.id.AlertProchainsDeparts).setVisibility(View.VISIBLE);
				break;
			}
			case MSG_CHOOSE_GARE: {
				final CharSequence[] noms = new CharSequence[options.size()];
				final int[] ids = new int[options.size()];
				for (int i = 0; i < options.size(); i++) {
					noms[i] = options.valueAt(i);
					ids[i] = options.keyAt(i);
				}
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Confirmez la gare");
				// builder.setMessage("La gare sélectionnée peut correspondre à plusieurs possibilités, confirmez votre choix SVP");
				builder.setItems(noms, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						idGare = ids[item];
						new RefreshThread(false).start();
					}
				});
				AlertDialog alert = builder.create();
				alert.show();
				break;
			}
		}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		setContentView(R.layout.prochainsdeparts);
	}

}
