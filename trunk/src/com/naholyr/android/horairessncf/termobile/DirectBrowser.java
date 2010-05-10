package com.naholyr.android.horairessncf.termobile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.SparseArray;

import com.naholyr.android.horairessncf.ProchainTrain;
import com.naholyr.android.horairessncf.Util;
import com.naholyr.android.horairessncf.HTTP.Request;
import com.naholyr.android.horairessncf.HTTP.Response;
import com.naholyr.android.horairessncf.activity.ProgressHandlerActivity;

public class DirectBrowser implements IBrowser {

	private static final String CHARSET = "ISO-8859-1";

	@SuppressWarnings("serial")
	public static final class ItemFormatException extends Exception {

	}

	public static final class ProchainDepart extends ProchainTrain.Depart {

		public static final Pattern PATTERN_ITEM = Pattern.compile("<p (?:class|align)=\"right\">.*?<hr */>");

		private static final Pattern PATTERN_ATTENTION_DEPART = Pattern.compile("D.*?part dans (.*?)<br");
		private static final Pattern PATTERN_TYPE_NUMERO = Pattern
				.compile("<(?:span|font)[^>]*>Mode *: *</(?:span|font)> *([^<>]+).*?<br[^>]*> *<(?:span|font)[^>]*>N.*? *: *</(?:span|font)> *(.*?) *<br");
		private static final Pattern PATTERN_HEURE = Pattern.compile("<(?:span|font)[^>]*>Heure *: *</(?:span|font)> *(.*?) *<br");
		private static final Pattern PATTERN_DESTINATION = Pattern.compile("<(?:span|font)[^>]*>Destination *: *</(?:span|font)> *(.*?) *<br");
		private static final Pattern PATTERN_RETARD = Pattern.compile("<(?:span|font)[^>]*>Retard *: *</(?:span|font)>(.*?)<br(.*?Motif *:.*?<br)?");

		public static final class Retard implements ProchainTrain.Retard {

			private static final Pattern PATTERN_DUREE = Pattern.compile("(?:<(?:span|font)[^>]*>)?Retard *: *</(?:span|font)> *(.*?) *<br");
			private static final Pattern PATTERN_MOTIF = Pattern.compile("<(?:span|font)[^>]*>Motif *: *</(?:span|font)> *(.*?) *<br");

			private static final String MOTIF_DEFAULT = "Sans motif";

			public String retard = null;
			public String motif = null;

			public Retard(String html) {
				/*
				 * <img src="../../../img/warningIMODE.jpg"/> <font
				 * color="black">Retard :</font> 50min<br/> <img
				 * src="../../../img/warningwhite.gif"/> <font
				 * color="black">Motif :</font> Conditions météo<br/>
				 */
				Matcher mDuree = PATTERN_DUREE.matcher(html);
				if (mDuree.find()) {
					retard = mDuree.group(1);
				}

				Matcher mMotif = PATTERN_MOTIF.matcher(html);
				if (mMotif.find()) {
					motif = mMotif.group(1);
				} else {
					motif = MOTIF_DEFAULT;
				}
			}

			@Override
			public String getDuree() {
				return retard;
			}

			@Override
			public String getMotif() {
				return motif;
			}
		}

		public List<ProchainTrain.Retard> retards = null;
		public String attentionDepart = null;
		public Integer type = null;
		public String typeLabel = null;
		public String numero = null;
		public String heure = null;
		public String destination = null;

		public ProchainDepart(String html) throws ItemFormatException {
			/*
			 * <p align="right"> <font color="black">Résultat</font> 2/11 </p>
			 * <p> <img alt="Logo" src="../../../img/clock131.gif"
			 * style="margin-right: 4px;" />Départ dans 14min<br/> <font
			 * color="black">Mode :</font> Car TER<br/> <font color="black">N°
			 * :</font> C49604<br/> <font color="black">Heure :</font> 22h16
			 * <br/> <font color="black">Destination :</font> gare de
			 * Bourg-en-Bresse<br/> <font color="black"><a
			 * href="detail.jsp?idxHoraire=1">Arrêts desservis </a></font><br/>
			 * <img src="../../../img/warningIMODE.jpg"/> <font
			 * color="black">Retard :</font> 50min<br/> <img
			 * src="../../../img/warningwhite.gif"/> <font color="black">Motif
			 * :</font> Conditions météo<br/> <img
			 * src="../../../img/warningIMODE.jpg"/> <font color="black">Retard
			 * :</font> 25min<br/> <img src="../../../img/warningwhite.gif"/>
			 * <font color="black">Motif :</font> Conditions météo<br/> <!--
			 * <sncfTag:Lien href="submit" libelle="Arrêts desservis" >
			 * <postfield name="idxHoraire" value="$(idxHoraire)"></postfield>
			 * </sncfTag:Lien> --> </p> <hr/>
			 */
			Matcher mAttentionDepart = PATTERN_ATTENTION_DEPART.matcher(html);
			if (mAttentionDepart.find()) {
				attentionDepart = mAttentionDepart.group(1);
			}

			Matcher mTypeNumero = PATTERN_TYPE_NUMERO.matcher(html);
			if (mTypeNumero.find()) {
				typeLabel = mTypeNumero.group(1);
				type = Util.typeTrainFromLabel(typeLabel);
				numero = mTypeNumero.group(2);
			}

			Matcher mHeure = PATTERN_HEURE.matcher(html);
			if (mHeure.find()) {
				heure = mHeure.group(1);
			}

			Matcher mDestination = PATTERN_DESTINATION.matcher(html);
			if (mDestination.find()) {
				destination = mDestination.group(1);
			}

			retards = new ArrayList<ProchainTrain.Retard>();
			Matcher mRetard = PATTERN_RETARD.matcher(html);
			while (mRetard.find()) {
				Retard retard = new Retard(mRetard.group());
				retards.add(retard);
			}
		}

		@Override
		public String getDestination() {
			return destination;
		}

		@Override
		public String getHeure() {
			return heure;
		}

		@Override
		public String getNumero() {
			return numero;
		}

		@Override
		public List<ProchainTrain.Retard> getRetards() {
			return retards;
		}

		@Override
		public int getType() {
			if (type == null) {
				return TYPE_AUTRE;
			} else {
				return type;
			}
		}

		@Override
		public String getTypeLabel() {
			return typeLabel;
		}

		@Override
		public String getVoie() {
			return null;
		}

		@Override
		public boolean isAQuai() {
			// TODO
			return false;
		}

		@Override
		public boolean isSupprime() {
			// TODO
			return false;
		}

	}

	private String sessionCookie = null;
	private long sessionTime;

	private static final long SESSION_MAX_AGE = 15 * 60 * 1000;

	public DirectBrowser() throws IOException {
		progressHandlerActivity = null;
		progressHandlerDialogId = 0;
		getSessionCookie();
	}

	private ProgressHandlerActivity progressHandlerActivity = null;
	private int progressHandlerDialogId = 0;

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.naholyr.android.horairessncf.termobile.IBrowser#
	 * setProgressHandlerActivity
	 * (com.naholyr.android.horairessncf.ProgressHandlerActivity)
	 */
	public void setProgressHandlerActivity(ProgressHandlerActivity progressHandlerActivity) {
		this.progressHandlerActivity = progressHandlerActivity;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.naholyr.android.horairessncf.termobile.IBrowser#
	 * getProgressHandlerActivity()
	 */
	public ProgressHandlerActivity getProgressHandlerActivity() {
		return progressHandlerActivity;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.naholyr.android.horairessncf.termobile.IBrowser#
	 * setProgressHandlerDialogId(int)
	 */
	public void setProgressHandlerDialogId(int progressHandlerDialogId) {
		this.progressHandlerDialogId = progressHandlerDialogId;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @seecom.naholyr.android.horairessncf.termobile.IBrowser#
	 * getProgressHandlerDialogId()
	 */
	public int getProgressHandlerDialogId() {
		return progressHandlerDialogId;
	}

	public DirectBrowser(ProgressHandlerActivity activity, int dialogId) throws IOException {
		progressHandlerActivity = activity;
		progressHandlerDialogId = dialogId;
		getSessionCookie();
	}

	private static final int STEP_COOKIE = 0;
	private static final int STEP_SEARCH_GARE = 1;
	private static final int STEP_CONFIRM_GARE = 2;
	private static final int STEP_SEARCH_HORAIRES = 3;
	private static final int STEP_FOUND_HORAIRES = 4;

	private String getStatusMessage(int step) {
		String message = "";

		switch (step) {
			case STEP_FOUND_HORAIRES:
				message = "OK" + message;
			case STEP_SEARCH_HORAIRES:
				message = "OK\n4/4 Recherche horaires... " + message;
			case STEP_CONFIRM_GARE:
				message = "OK\n3/4 Confirmation gare... " + message;
			case STEP_SEARCH_GARE:
				message = "OK\n2/4 Recherche gare... " + message;
			case STEP_COOKIE:
				message = "1/4 Session... " + message;
		}

		return message;
	}

	private void updateStatusMessage(int step) {
		if (progressHandlerActivity != null) {
			progressHandlerActivity.sendMessage(ProgressHandlerActivity.MSG_SET_DIALOG_MESSAGE, progressHandlerDialogId, getStatusMessage(step));
		}
		return;
	}

	private String getSessionCookie() throws IOException {
		long now = Calendar.getInstance().getTimeInMillis();

		if (sessionCookie == null || now - sessionTime > SESSION_MAX_AGE) {
			updateStatusMessage(STEP_COOKIE);

			Response response = Request.GET("www.termobile.fr", 80, "/pages/imode/accueil.jsp", false, CHARSET);
			sessionCookie = response.getHeader("set-cookie");
			sessionTime = now;
			// Remove the "; path=..." or "; domain=..." information from cookie
			int semicolon = sessionCookie.indexOf(';');
			if (semicolon >= 0) {
				sessionCookie = sessionCookie.substring(0, semicolon).trim();
			}
			// TODO handle cookies in a more generic way (more than 1 cookie,
			// etc...)

			updateStatusMessage(STEP_SEARCH_GARE);
		}

		return sessionCookie;
	}

	private Integer idGare = null;

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.naholyr.android.horairessncf.termobile.IBrowser#getItems(int)
	 */
	public List<ProchainTrain.Depart> getItems(int nbItems) throws IOException {
		return getItems(nbItems, true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.naholyr.android.horairessncf.termobile.IBrowser#getItems(int,
	 * boolean)
	 */
	public List<ProchainTrain.Depart> getItems(int nbItems, boolean refresh) throws IOException {
		if (idGare == null) {
			return null;
		}

		List<ProchainTrain.Depart> items = new ArrayList<ProchainTrain.Depart>();
		String resultPage;

		// 1. Confirmer la gare choisie
		{
			updateStatusMessage(STEP_CONFIRM_GARE);

			Map<String, String> post = new HashMap<String, String>();
			post.put("idxGare", String.valueOf(idGare));
			post.put("idxNombre", String.valueOf(nbItems));

			Map<String, String> headers = Request.getDefaultHeaders(CHARSET);
			headers.put("Cookie", getSessionCookie());

			Response response = Request.POST("www.termobile.fr", 80, "/rechercherRPD.do", headers, post, false, CHARSET);
			resultPage = response.getHeader("location");

			delayHttpRequests();
		}

		// 2. Récupérer les résultats
		{
			updateStatusMessage(STEP_SEARCH_HORAIRES);

			Map<String, String> headers = Request.getDefaultHeaders(CHARSET);
			headers.put("Cookie", getSessionCookie());

			String path = resultPage.substring("http://www.termobile.fr".length());
			Response response = Request.GET("www.termobile.fr", 80, path, headers, true, CHARSET);

			if (response.getStatusCode() != 200) {
				throw new IOException("Le serveur TERMobile a renvoyé une erreur (" + response.getStatusCode() + " - " + response.getStatusDescription() + ")");
			}

			// Clean HTML
			String html = response.getBody();
			html = html.trim().replaceAll("[\t\r\n]", "").replaceAll(" *<", "<").replaceAll("> *", ">").replaceAll("&nbsp;", " ");
			// convert to utf8
			Matcher m = ProchainDepart.PATTERN_ITEM.matcher(html);
			while (m.find()) {
				try {
					items.add(new ProchainDepart(m.group()));
				} catch (ItemFormatException e) {
					// Ignorer
					continue;
				}
			}

			updateStatusMessage(STEP_FOUND_HORAIRES);
		}

		return items;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.naholyr.android.horairessncf.termobile.IBrowser#confirmGare(int)
	 */
	public void confirmGare(int id) {
		idGare = id;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.naholyr.android.horairessncf.termobile.IBrowser#searchGares(java.
	 * lang.String)
	 */
	public SparseArray<String> searchGares(String nom) throws IOException {
		SparseArray<String> result = new SparseArray<String>();

		updateStatusMessage(STEP_SEARCH_GARE);

		// Recherche de la gare sur le site
		Map<String, String> headers1 = Request.getDefaultHeaders(CHARSET);
		headers1.put("Cookie", getSessionCookie());
		Map<String, String> post = new HashMap<String, String>();
		post.put("valeurGare", nom);
		Response response1 = Request.POST("www.termobile.fr", 80, "/confirmerGareRPD.do", headers1, post, false, CHARSET);
		// Page de redirection
		String resultPage = response1.getHeader("location");
		String path = resultPage.substring("http://www.termobile.fr".length());
		// Délai entre les requêtes
		delayHttpRequests();
		// Récupération de la page des résultats
		Map<String, String> headers2 = Request.getDefaultHeaders(CHARSET);
		headers2.put("Cookie", getSessionCookie());
		Response response2 = Request.GET("www.termobile.fr", 80, path, headers2, true, CHARSET);
		String body = response2.getBody();

		Matcher mSelect = Pattern.compile("(?i)<select[^>]*name=['\"]idxGare['\"][^>]*>(.*?)</select>").matcher(body);
		if (mSelect.find()) {
			String options = mSelect.group(1);
			// <option value="1743" class="red">villefranche-d''alb.-ce</option>
			Matcher mOption = Pattern.compile("(?i)<option[^>]*value=['\"]([0-9]+)['\"][^>]*>(.*?)</option>").matcher(options);
			while (mOption.find()) {
				int key = Integer.parseInt(mOption.group(1));
				String value = mOption.group(2);
				result.put(key, value);
			}
		}

		updateStatusMessage(STEP_CONFIRM_GARE);

		return result;
	}

	/**
	 * Attendre entre 0.8 et 1.9 secondes (arbitraire)
	 * 
	 * Permet d'ajouter un délai entre les requêtes HTTP, histoire de ne pas se
	 * faire bannir trop vite :)
	 */
	private void delayHttpRequests() {
		long delay = ((long) (Math.random() * 1100)) + 800;
		long time1 = Calendar.getInstance().getTimeInMillis();
		while (true) {
			long time2 = Calendar.getInstance().getTimeInMillis();
			if (time2 - time1 >= delay) {
				break;
			}
		}
	}

}
