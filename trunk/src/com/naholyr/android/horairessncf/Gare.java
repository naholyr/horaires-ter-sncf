package com.naholyr.android.horairessncf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class Gare {

	protected String nom;
	protected String region;
	protected String adresse;
	protected double latitude;
	protected double longitude;

	public Gare(DataHelper dataHelper, String nom) throws IOException {
		this.nom = nom;

		Map<String, Object> result = dataHelper.selectOne(nom);

		load(result, "nom = '" + nom + "'");
	}

	public Gare(DataHelper dataHelper, int rowid) throws IOException {
		Map<String, Object> result = dataHelper.selectOne(rowid);

		load(result, "rowid = '" + rowid + "'");
	}

	protected void load(Map<String, Object> result, String ident) throws IOException {
		if (result != null) {
			nom = (String) result.get("nom");
			region = (String) result.get("region");
			adresse = (String) result.get("adresse");
			latitude = (Double) result.get("latitude");
			longitude = (Double) result.get("longitude");
		} else {
			throw new IOException("Données de la gare introuvable : " + ident);
		}
	}

	protected void save(DataHelper dataHelper) throws IOException {
		dataHelper.insertOrUpdate(nom, region, adresse, latitude, longitude);
	}

	public static List<Gare> getAll(DataHelper dataHelper) throws IOException {
		List<Gare> gares = new ArrayList<Gare>();

		List<String> noms = dataHelper.selectAll();
		for (String nom : noms) {
			gares.add(new Gare(dataHelper, nom));
		}

		return gares;
	}

	public static List<Gare> getAll(DataHelper dataHelper, double latitude, double longitude, double radius_km) throws IOException {
		double latitude_radians = latitude * (Math.PI / 180);
		double latitude_delta = radius_km / Util.ONE_DEGREE_LAT_KM;
		double longitude_delta = radius_km / Math.abs(Math.cos(latitude_radians) * Util.ONE_DEGREE_LAT_KM);
		double latitude_min = latitude - latitude_delta;
		double latitude_max = latitude + latitude_delta;
		double longitude_min = longitude - longitude_delta;
		double longitude_max = longitude + longitude_delta;

		List<String> noms = dataHelper.selectInBox(latitude_min, latitude_max, longitude_min, longitude_max);

		return getAll(dataHelper, noms);
	}

	public static List<Gare> getAll(DataHelper dataHelper, List<String> noms) throws IOException {
		List<Gare> gares = new ArrayList<Gare>();

		for (String nom : noms) {
			gares.add(new Gare(dataHelper, nom));
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

	public boolean isFavori(SharedPreferences prefs) {
		// SharedPreferences prefs =
		// context.getSharedPreferences(Util.PREFS_FAVORIS_GARE,
		// Context.MODE_PRIVATE);

		return prefs.getBoolean(getNom(), false);
	}

	public void setFavori(SharedPreferences prefs, boolean favori) {
		if (isFavori(prefs) != favori) {
			//
			Editor editor = prefs.edit();
			if (favori) {
				editor.putBoolean(getNom(), favori);
			} else {
				editor.remove(getNom());
			}
			editor.commit();
		}
	}

}
