package com.naholyr.android.horairessncf.activity;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.naholyr.android.horairessncf.R;
import com.naholyr.android.horairessncf.termobile.JSONServerBrowser;
import com.naholyr.android.horairessncf.termobile.JSONWebServiceClient;
import com.naholyr.android.horairessncf.termobile.JSONWebServiceClient.JSONResponse;
import com.naholyr.android.horairessncf.view.ListeArretsAdapter;

public class DetailsTrainActivity extends Activity {

	public static final String EXTRA_NUMERO = "numero";

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Read extra
		final String numeroTrain = getIntent().getExtras().getString(EXTRA_NUMERO);
		if (numeroTrain == null) {
			throw new IllegalAccessError("Extra 'numero' obligatoire !");
		}
		setTitle("Train n°" + numeroTrain);

		// Window progress bar
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

		// Layout
		setContentView(R.layout.details_train);

		// Enable progress bar
		setProgressBarIndeterminateVisibility(true);

		// Main thread
		new Thread() {
			public void run() {
				JSONWebServiceClient client = new JSONWebServiceClient();
				Map<String, Object> params = new HashMap<String, Object>();
				params.put("num", numeroTrain);
				try {
					// Request
					JSONResponse response = client.query(JSONServerBrowser.WS_TRAIN_URI, params);
					if (response.isSuccess()) {
						JSONObject data = response.getSuccessData();
						JSONObject heures = data.getJSONObject("heures");
						JSONArray gares = data.getJSONArray("arrets");
						final String[] mGares = new String[gares.length()];
						final Map<String, String> mHeures = new HashMap<String, String>();
						for (int i = 0; i < gares.length(); i++) {
							String gare = gares.getString(i);
							mGares[i] = gare;
							if (heures.has(gare)) {
								String heure = heures.getString(gare);
								mHeures.put(gare, heure);
							}
						}
						// Fill data
						runOnUiThread(new Runnable() {
							public void run() {
								ListAdapter adapter = new ListeArretsAdapter(getApplicationContext(), mGares, mHeures);
								((ListView) findViewById(R.id.DetailsTrain_Arrets)).setAdapter(adapter);
								setProgressBarIndeterminateVisibility(false);
							}
						});
					} else {
						runOnUiThread(new Runnable() {
							public void run() {
								Toast.makeText(getApplicationContext(), "Erreur lors de la récupération des informations du train", Toast.LENGTH_SHORT).show();
								finish();
							}
						});
					}
				} catch (MalformedURLException e) {
					throw new IllegalArgumentException("Invalid url (" + e.getMessage() + ")");
				} catch (IOException e) {
					// Skip
				} catch (JSONException e) {
					// Skip
				}
			}
		}.start();
	}
}
