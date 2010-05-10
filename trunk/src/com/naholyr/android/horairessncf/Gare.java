package com.naholyr.android.horairessncf;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.naholyr.android.horairessncf.activity.ProchainsDepartsActivity;
import com.naholyr.android.horairessncf.activity.ProgressHandlerActivity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;

public class Gare {

	protected String nom;
	protected String region;
	protected String adresse;
	protected double latitude;
	protected double longitude;

	private ProgressHandlerActivity context;

	public Gare(ProgressHandlerActivity context, String nom) throws IOException {
		this.nom = nom;
		this.context = context;

		DataHelper dataHelper = DataHelper.getInstance(context);
		Map<String, Object> result = dataHelper.selectOne(nom);

		load(result);
	}

	public Gare(ProgressHandlerActivity context, int rowid) throws IOException {
		this.context = context;

		DataHelper dataHelper = DataHelper.getInstance(context);
		Map<String, Object> result = dataHelper.selectOne(rowid);

		load(result);
	}

	protected void load(Map<String, Object> result) throws IOException {
		if (result != null) {
			nom = (String) result.get("nom");
			region = (String) result.get("region");
			adresse = (String) result.get("adresse");
			latitude = (Double) result.get("latitude");
			longitude = (Double) result.get("longitude");
		} else {
			throw new IOException();
		}
	}

	protected void save() throws IOException {
		DataHelper dataHelper = DataHelper.getInstance(context);

		dataHelper.insertOrUpdate(nom, region, adresse, latitude, longitude);
	}

	public static List<Gare> getAll(ProgressHandlerActivity context, DataHelper helper) throws IOException {
		List<Gare> gares = new ArrayList<Gare>();

		List<String> noms = helper.selectAll();
		for (String nom : noms) {
			gares.add(new Gare(context, nom));
		}

		return gares;
	}

	public static List<Gare> getAll(ProgressHandlerActivity context) throws IOException {
		return getAll(context, DataHelper.getInstance(context));
	}

	public static List<Gare> getAll(ProgressHandlerActivity context, DataHelper helper, double latitude, double longitude, double radius_km) throws IOException {
		double latitude_radians = latitude * (Math.PI / 180);
		double latitude_delta = radius_km / Util.ONE_DEGREE_LAT_KM;
		double longitude_delta = radius_km / Math.abs(Math.cos(latitude_radians) * Util.ONE_DEGREE_LAT_KM);
		double latitude_min = latitude - latitude_delta;
		double latitude_max = latitude + latitude_delta;
		double longitude_min = longitude - longitude_delta;
		double longitude_max = longitude + longitude_delta;

		List<String> noms = helper.selectInBox(latitude_min, latitude_max, longitude_min, longitude_max);

		return getAll(context, noms);
	}

	public static List<Gare> getAll(ProgressHandlerActivity context, double latitude, double longitude, double radius_km) throws IOException {
		return getAll(context, DataHelper.getInstance(context), latitude, longitude, radius_km);
	}

	public static List<Gare> getAll(ProgressHandlerActivity context, List<String> noms) throws IOException {
		List<Gare> gares = new ArrayList<Gare>();

		for (String nom : noms) {
			gares.add(new Gare(context, nom));
		}

		return gares;
	}

	protected List<ProchainTrain.Depart> getProchainDeparts() throws IOException {
		List<ProchainTrain.Depart> result = new ArrayList<ProchainTrain.Depart>();

		return result;
	}

	protected List<ProchainTrain.Arrivee> getProchainesArrivees() throws IOException {
		List<ProchainTrain.Arrivee> result = new ArrayList<ProchainTrain.Arrivee>();

		return result;
	}

	public String getNom() {
		return nom;
	}

	public String getRegion() {
		return region;
	}

	public String getAdresse() {
		return adresse;
	}

	public double getLatitude() {
		return latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public double getDistance(double latitude, double longitude) {
		return Util.distance(getLatitude(), getLongitude(), latitude, longitude);
	}

	public boolean isFavori() {
		SharedPreferences prefs = context.getSharedPreferences(Util.PREFS_FAVORIS_GARE, Context.MODE_PRIVATE);

		return prefs.getBoolean(getNom(), false);
	}

	public void setFavori(boolean favori) {
		if (isFavori() != favori) {
			SharedPreferences prefs = context.getSharedPreferences(Util.PREFS_FAVORIS_GARE, Context.MODE_PRIVATE);
			Editor editor = prefs.edit();
			if (favori) {
				editor.putBoolean(getNom(), favori);
			} else {
				editor.remove(getNom());
			}
			editor.commit();
		}
	}

	public void showInGoogleMaps() {
		showInGoogleMaps(false);
	}

	public void showInGoogleMaps(boolean query) {
		String sUri = "geo:" + getLatitude() + "," + getLongitude();
		if (query) {
			sUri += "?q=" + URLEncoder.encode(getAdresse());
		}
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(sUri));
		context.startActivity(intent);
	}

	public void showProchainsDepartsActivity() {
		Intent intent = new Intent(context, ProchainsDepartsActivity.class);
		intent.putExtra("nom", getNom());
		context.startActivity(intent);
	}

}
