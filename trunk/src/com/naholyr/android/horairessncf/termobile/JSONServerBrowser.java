package com.naholyr.android.horairessncf.termobile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.SparseArray;

import com.naholyr.android.horairessncf.ProchainTrain;
import com.naholyr.android.horairessncf.Util;
import com.naholyr.android.horairessncf.activity.ProgressHandlerActivity;
import com.naholyr.android.horairessncf.termobile.JSONWebServiceClient.JSONResponse;

public class JSONServerBrowser implements IBrowser {

	public static final String WS_HOST = "http://termobile-ws.sfhost.net";
	public static final String WS_GARE_URI = WS_HOST + "/prochainsdeparts.php";
	public static final String WS_TRAIN_URI = WS_HOST + "/train.php";

	private int mIdGare;
	private String mNomGare;
	private List<ProchainTrain.Depart> mCachedDeparts;

	private static final JSONWebServiceClient mClient = new JSONWebServiceClient();

	public static final class JSONDepart extends ProchainTrain.Depart {

		public static final class JSONRetard implements ProchainTrain.Retard {

			private static final String MOTIF_DEFAULT = "Sans motif";

			private String mDuree;
			private String mMotif;

			public JSONRetard(JSONObject object) throws JSONException {
				// Durée
				if (object.has("retard")) {
					mDuree = object.getString("retard");
				} else {
					mDuree = null;
				}
				// Motif
				if (object.has("motif")) {
					mMotif = object.getString("motif");
				} else {
					mMotif = MOTIF_DEFAULT;
				}
			}

			public String getDuree() {
				return mDuree;
			}

			public String getMotif() {
				return mMotif;
			}

		}

		private String mDestination;
		private String mHeure;
		private String mNumero;
		private String mTypeLabel;
		private String mVoie;
		private boolean mAQuai;
		private boolean mSupprime;
		private List<ProchainTrain.Retard> mRetards;

		public JSONDepart(JSONObject object) {
			// Destination
			if (object.has("destination")) {
				try {
					mDestination = object.getString("destination");
				} catch (JSONException e) {
					mDestination = null;
				}
			} else {
				mDestination = null;
			}
			// Heure
			if (object.has("heure")) {
				try {
					mHeure = object.getString("heure");
				} catch (JSONException e) {
					mHeure = null;
				}
			} else {
				mHeure = null;
			}
			// Numéro
			if (object.has("numero")) {
				try {
					mNumero = object.getString("numero");
				} catch (JSONException e) {
					mNumero = null;
				}
			} else {
				mNumero = null;
			}
			// Type
			if (object.has("type")) {
				try {
					mTypeLabel = object.getString("type");
				} catch (JSONException e) {
					mTypeLabel = null;
				}
			} else {
				mTypeLabel = null;
			}
			// Voie
			if (object.has("voie")) {
				try {
					mVoie = object.getString("voie");
				} catch (JSONException e) {
					mVoie = null;
				}
			} else {
				mVoie = null;
			}
			// A quai ?
			if (object.has("aquai")) {
				try {
					mAQuai = object.getBoolean("aquai");
				} catch (JSONException e) {
					mAQuai = false;
				}
			} else {
				mAQuai = false;
			}
			// Supprimé ?
			if (object.has("supprime")) {
				try {
					mSupprime = object.getBoolean("supprime");
				} catch (JSONException e) {
					mSupprime = false;
				}
			} else {
				mSupprime = false;
			}
			// Retards
			mRetards = new ArrayList<ProchainTrain.Retard>();
			if (object.has("retards")) {
				try {
					JSONArray retards = object.getJSONArray("retards");
					for (int i = 0; i < retards.length(); i++) {
						try {
							mRetards.add(new JSONRetard(retards.getJSONObject(i)));
						} catch (JSONException e) {
							// skip
						}
					}
				} catch (JSONException e) {
					// TODO handle exception
				}
			}
		}

		public String getDestination() {
			return mDestination;
		}

		public String getHeure() {
			return mHeure;
		}

		public String getNumero() {
			return mNumero;
		}

		public List<ProchainTrain.Retard> getRetards() {
			return mRetards;
		}

		public int getType() {
			return Util.typeTrainFromLabel(getTypeLabel());
		}

		public String getTypeLabel() {
			return mTypeLabel;
		}

		public String getVoie() {
			return mVoie;
		}

		public boolean isAQuai() {
			return mAQuai;
		}

		public boolean isSupprime() {
			return mSupprime;
		}

	}

	private ProgressHandlerActivity progressHandlerActivity = null;
	private int progressHandlerDialogId = 0;

	public void setProgressHandlerActivity(ProgressHandlerActivity progressHandlerActivity) {
		this.progressHandlerActivity = progressHandlerActivity;
	}

	public ProgressHandlerActivity getProgressHandlerActivity() {
		return progressHandlerActivity;
	}

	public void setProgressHandlerDialogId(int progressHandlerDialogId) {
		this.progressHandlerDialogId = progressHandlerDialogId;
	}

	public int getProgressHandlerDialogId() {
		return progressHandlerDialogId;
	}

	public JSONServerBrowser(ProgressHandlerActivity activity, int dialogId) {
		progressHandlerActivity = activity;
		progressHandlerDialogId = dialogId;
	}

	private static final int STEP_SEARCH_GARE = 1;
	private static final int STEP_SEARCH_HORAIRES = 2;
	private static final int STEP_FOUND_HORAIRES = 3;

	private static final int MAX_ESSAIS = 3;
	private int numEssai = 0;

	private String getStatusMessage(int step) {
		String message = "";

		switch (step) {
			case STEP_FOUND_HORAIRES:
				message = "OK" + message;
			case STEP_SEARCH_HORAIRES:
				message = "OK\n2/2 Recherche horaires... " + message;
			case STEP_SEARCH_GARE:
				message = "1/2 Recherche gare... " + message;
		}
		message = "Essai " + numEssai + "/" + MAX_ESSAIS + ":\n" + message;

		return message;
	}

	private void updateStatusMessage(int step) {
		if (progressHandlerActivity != null) {
			progressHandlerActivity.sendMessage(ProgressHandlerActivity.MSG_SET_DIALOG_MESSAGE, progressHandlerDialogId, getStatusMessage(step));
		}
		return;
	}

	public void confirmGare(int id) {
		mIdGare = id;
	}

	public List<ProchainTrain.Depart> getItems(int nbItems) throws IOException {
		return getItems(nbItems, true);
	}

	public SparseArray<String> searchGares(String nom, int nbItems) throws IOException {
		numEssai = 1;
		while (numEssai <= MAX_ESSAIS) {
			try {
				updateStatusMessage(STEP_SEARCH_GARE);
				HashMap<String, Object> params = new HashMap<String, Object>();
				params.put("gare", Util.removeAccents(nom));
				params.put("nb", nbItems);
				JSONResponse response = mClient.query(WS_GARE_URI, params);
				if (response.isError()) {
					throw new IOException(response.getErrorMessage());
				}
				if (response.isSuccess()) {
					mNomGare = nom;
					if (isListeDeparts(response)) {
						int idGare = getIdGare(response);
						if (idGare != 0) {
							mCachedDeparts = getListeDeparts(response);
						}
						SparseArray<String> gares = new SparseArray<String>();
						gares.put(idGare, getNomGare(response));
						return gares;
					} else {
						if (isListeGares(response)) {
							return getListeGares(response);
						} else {
							throw new IOException("Unexpected error 2");
						}
					}
				} else {
					throw new IOException("Unexpected error 1");
				}
			} catch (Throwable e) {
				numEssai++;
				if (numEssai > MAX_ESSAIS) {
					throw new IOException("Erreur serveur : " + e.getMessage());
				}
			}
		}
		throw new IOException("Erreur U1 ! Trop d'essais pour contacter le serveur, et pas d'erreur reconnue");
	}

	public List<ProchainTrain.Depart> getItems(int nbItems, boolean refresh) throws IOException {
		if (mCachedDeparts != null && !refresh) {
			// Résultats cachés depuis l'appel précédent
			List<ProchainTrain.Depart> result = mCachedDeparts;
			mCachedDeparts = null;
			return result;
		}
		numEssai = 1;
		while (numEssai <= MAX_ESSAIS) {
			try {
				updateStatusMessage(STEP_SEARCH_HORAIRES);
				HashMap<String, Object> params = new HashMap<String, Object>();
				params.put("gare", Util.removeAccents(mNomGare));
				params.put("id", mIdGare);
				params.put("nb", nbItems);
				JSONResponse response = mClient.query(WS_GARE_URI, params);
				if (response.isError()) {
					throw new IOException(response.getErrorMessage());
				}
				if (response.isSuccess()) {
					updateStatusMessage(STEP_FOUND_HORAIRES);
					return getListeDeparts(response);
				} else {
					throw new IOException("Unexpected error 3");
				}
			} catch (JSONException e) {
				numEssai++;
				if (numEssai > MAX_ESSAIS) {
					throw new IOException("Erreur serveur : " + e.getMessage());
				}
			}
		}
		throw new IOException("Erreur U2 ! Trop d'essais pour contacter le serveur, et pas d'erreur reconnue");
	}

	public ProchainTrain.Depart getItem(String numero) throws IOException {
		throw new UnsupportedOperationException("Not implemented yet");
	}

	// Analyse de la réponse

	private static boolean isListeDeparts(JSONResponse response) {
		if (response.isSuccess()) {
			JSONObject data = response.getSuccessData();
			return data.has("departs");
		}
		return false;
	}

	private static List<ProchainTrain.Depart> getListeDeparts(JSONResponse response) {
		if (!response.isSuccess()) {
			return null;
		}
		JSONObject data = response.getSuccessData();
		List<ProchainTrain.Depart> result = new ArrayList<ProchainTrain.Depart>();
		try {
			JSONArray departs = data.getJSONArray("departs");
			for (int i = 0; i < departs.length(); i++) {
				result.add(new JSONDepart(departs.getJSONObject(i)));
			}
		} catch (JSONException e) {
			// TODO handle error
		}
		return result;
	}

	private static boolean isListeGares(JSONResponse response) {
		if (response.isSuccess()) {
			JSONObject data = response.getSuccessData();
			return data.has("gares");
		}
		return false;
	}

	private static SparseArray<String> getListeGares(JSONResponse response) {
		if (!response.isSuccess()) {
			return null;
		}
		try {
			SparseArray<String> gares = new SparseArray<String>();
			JSONObject listeGares = response.getSuccessData().getJSONObject("gares");
			Iterator<?> noms = listeGares.keys();
			while (noms.hasNext()) {
				String nom = (String) noms.next();
				int id = listeGares.getInt(nom);
				gares.put(id, nom);
			}
			return gares;
		} catch (JSONException e) {
			return null;
		}
	}

	private static int getIdGare(JSONResponse response) {
		if (response.isSuccess()) {
			try {
				return response.getSuccessData().getInt("id");
			} catch (JSONException e) {
				return 0;
			}
		} else {
			return 0;
		}
	}

	private static String getNomGare(JSONResponse response) {
		if (response.isSuccess()) {
			try {
				return response.getSuccessData().getString("nom");
			} catch (JSONException e) {
				return null;
			}
		} else {
			return null;
		}
	}

}
