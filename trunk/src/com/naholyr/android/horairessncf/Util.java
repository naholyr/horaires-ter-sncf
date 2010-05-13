package com.naholyr.android.horairessncf;

/*
 import java.io.BufferedReader;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.io.OutputStreamWriter;
 import java.net.HttpURLConnection;
 import java.net.URL;
 import java.net.URLEncoder;
 */
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;

public class Util {

	public static final int NB_GARES_TOTAL = 3081;

	public static final int BACKGROUND_1 = 0x33FFFFFF;
	public static final int BACKGROUND_2 = 0x33999999;

	public static final int STATUS_NOT_FOUND = 0;
	public static final int STATUS_FOUND_SINGLE = 1;
	public static final int STATUS_FOUND_MULTIPLE = 2;

	public static final int READ_URL_SOCKET_TIMEOUT = 30000;
	public static final int READ_URL_READY_TIMEOUT = 15000;
	public static final int GEOLOCATION_TIMEOUT = 30000;

	public static final double EARTH_RADIUS_KM = 6365d;
	public static final double ONE_DEGREE_LAT_KM = 111d;

	public static final String LINK_TERMOBILE = "http://www.termobile.fr";
	public static final String LINK_GAREENMOUVEMENT = "http://www.gares-en-mouvement.com";

	public static final String URL_GARES = "http://www.gares-en-mouvement.com/index.php";
	public static final String MASK_GARES_ITEM = "(?i)<a href=\"gare\\.php\\?gare=([a-z]*)\".*?>(.*?)</a>";
	public static final int MASK_GARES_ITEM_INDEX_CODE = 1;
	public static final int MASK_GARES_ITEM_INDEX_NOM = 2;

	public static final String URL_INFO_GARE = "http://www.gares-en-mouvement.com/votre_gare.php?gare=CODE";
	public static final String MASK_GARE_INFO = "(?i)<[^>]*? class=(\"?)fiche_adresse_titre.*?<p.*?>(.*?)</p>";
	public static final int MASK_GARE_INFO_INDEX = 2;
	public static final String MASK_GARE_TITRE = "(?i)<h1.*?>\\s*(.*?)\\s*</h1>";
	public static final int MASK_GARE_TITRE_INDEX = 1;

	public static final String URL_HORAIRES = "http://www.gares-en-mouvement.com/infos_temps_reel.php?gare=CODE";
	public static final String URL_TVS_PROCHAINS_DEPARTS = "http://www.gares-en-mouvement.com/include/tvs.php?nom_gare=&tab=dep&TVS=http://www.gares-en-mouvement.com/tvs/TVS?wsdl&code_tvs=CODE_TVS&tab_summary_dep=&caption=&type=T&numero=N&num=N&heure=H&dest=D&origine=P&situation=S&voie=V&heur=h&h=h&minut=m&m=m&arriv=A&gare=CODE&retard=R";
	public static final String URL_TVS_PROCHAINES_ARRIVEES = "http://www.gares-en-mouvement.com/include/tvs.php?nom_gare=&tab=arr&TVS=http://www.gares-en-mouvement.com/tvs/TVS?wsdl&code_tvs=CODE_TVS&tab_summary_dep=&caption=&type=T&numero=N&num=N&heure=H&dest=D&origine=P&situation=S&voie=V&heur=h&h=h&minut=m&m=m&arriv=A&gare=CODE&retard=R";
	public static final String MASK_CODE_TVS = "(?i)get_tvs\\(\".*?\",\".*?\",\".*?\",\"(.*?)\"";
	public static final int MASK_CODE_TVS_INDEX = 1;

	public static final int MAX_NB_TRIES_GEOLOC = 5;

	public static final String MASK_TRAIN_RETARD = "^R *: *(.*)$";
	public static final String MASK_TRAIN_ARRIVE = "^A$";
	public static final String MASK_TRAIN_SUPPRIME = "^S$";

	public static final String PREFS_FAVORIS_GARE = "favoris_gares";
	public static final String PREFS_DATA = "date";
	public static final String PREFS_DATA_LAST_HOME = "last_home";
	public static final String PREFS_DATA_LAST_SEARCH = "last_search";

	private static Map<String, Pattern> patterns = new HashMap<String, Pattern>();

	public static Pattern getPattern(String pattern) {
		if (!patterns.containsKey(pattern)) {
			patterns.put(pattern, Pattern.compile(pattern));
		}

		return patterns.get(pattern);
	}

	public static double distance(double lat1, double lon1, double lat2, double lon2) {
		return EARTH_RADIUS_KM
				* 2
				* Math.asin(Math.sqrt(Math.pow(Math.sin((lat1 - lat2) * Math.PI / 180 / 2), 2) + Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180)
						* Math.pow(Math.sin((lon1 - lon2) * Math.PI / 180 / 2), 2)));
	}

	public static void showError(final Activity activity, String message) {
		showError(activity, message, true);
	}

	public static void showError(final Activity activity, String message, Runnable onClose) {
		showError(activity, message, true, onClose);
	}

	public static void showError(final Activity activity, String message, boolean addDisclaimer) {
		showError(activity, message, addDisclaimer, new Runnable() {
			public void run() {
				activity.finish();
			}
		});
	}

	public static void showError(final Activity activity, String message, boolean addDisclaimer, final Runnable onClose) {
		if (addDisclaimer) {
			message += "\n\nN'hésitez pas à vous rendre sur 'termobile.fr' pour réessayer";
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle("Erreur");
		builder.setIcon(android.R.drawable.ic_dialog_alert);
		builder.setMessage(message);
		builder.setCancelable(true);
		builder.setNegativeButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				onClose.run();
			}
		});
		builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
			public void onCancel(DialogInterface dialog) {
				onClose.run();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();

	}

	public static int typeTrainFromLabel(String typeLabel) {
		if (typeLabel.equals("TGV")) {
			return ProchainTrain.TYPE_TGV;
		} else if (typeLabel.equals("Car TER")) {
			return ProchainTrain.TYPE_CAR;
		} else if (typeLabel.equals("Train TER")) {
			return ProchainTrain.TYPE_TER;
		} else if (typeLabel.equals("Corail")) {
			return ProchainTrain.TYPE_CORAIL;
		}
		return ProchainTrain.TYPE_AUTRE;
	}

	private static final String PLAIN_ASCII = "AaEeIiOoUu" // grave
			+ "AaEeIiOoUuYy" // acute
			+ "AaEeIiOoUuYy" // circumflex
			+ "AaOoNn" // tilde
			+ "AaEeIiOoUuYy" // umlaut
			+ "Aa" // ring
			+ "Cc" // cedilla
			+ "OoUu" // double acute
	;

	private static final String UNICODE = "\u00C0\u00E0\u00C8\u00E8\u00CC\u00EC\u00D2\u00F2\u00D9\u00F9" + "\u00C1\u00E1\u00C9\u00E9\u00CD\u00ED\u00D3\u00F3\u00DA\u00FA\u00DD\u00FD"
			+ "\u00C2\u00E2\u00CA\u00EA\u00CE\u00EE\u00D4\u00F4\u00DB\u00FB\u0176\u0177" + "\u00C3\u00E3\u00D5\u00F5\u00D1\u00F1"
			+ "\u00C4\u00E4\u00CB\u00EB\u00CF\u00EF\u00D6\u00F6\u00DC\u00FC\u0178\u00FF" + "\u00C5\u00E5" + "\u00C7\u00E7" + "\u0150\u0151\u0170\u0171";

	public static String removeAccents(String s) {
		if (s == null) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		int n = s.length();
		for (int i = 0; i < n; i++) {
			char c = s.charAt(i);
			int pos = UNICODE.indexOf(c);
			if (pos > -1) {
				sb.append(PLAIN_ASCII.charAt(pos));
			} else {
				sb.append(c);
			}
		}

		return sb.toString();
	}

	public static SharedPreferences getFavsPreferences(Context context) {
		return context.getSharedPreferences(Util.PREFS_FAVORIS_GARE, Context.MODE_PRIVATE);
	}

}
