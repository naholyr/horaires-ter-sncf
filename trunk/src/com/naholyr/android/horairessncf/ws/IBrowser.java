package com.naholyr.android.horairessncf.ws;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import android.util.SparseArray;

public interface IBrowser {

	public static interface ProgressListener {

		/**
		 * Met à jour le message de progression
		 * 
		 * @param message
		 */
		public void updateMessage(String message);

	}

	/**
	 * Attache un "progress listener" Renvoie son identifiant numérique interne
	 * 
	 * Note : l'absence de doublons dépendra de l'implémentation...
	 * 
	 * @param listener
	 */
	public int addProgressListener(ProgressListener listener);

	/**
	 * Détache un "progress listener" par son id numérique interne
	 * 
	 * @param listener
	 */
	public void removeProgressListener(int index);

	/**
	 * Détache un "progress listener"
	 * 
	 * @param listener
	 */
	public void removeProgressListener(ProgressListener listener);

	/**
	 * Récupérer la liste des trains, sans forcément redemander la liste au
	 * serveur
	 * 
	 * @param nbItems
	 * @return
	 * @throws IOException
	 */
	public List<ProchainTrain.Depart> getItems(int nbItems) throws IOException;

	/**
	 * Récupérer la liste des trains, en forçant une requête au serveur
	 * 
	 * @param nbItems
	 * @param refresh
	 * @return
	 * @throws IOException
	 */
	public List<ProchainTrain.Depart> getItems(int nbItems, boolean refresh) throws IOException;

	/**
	 * Confirmer la gare exacte par son ID
	 * 
	 * @param id
	 */
	public void confirmGare(int id);

	/**
	 * Retourne la liste des gares correspondant au nom recherché.
	 * 
	 * @param nom
	 * @param nbItems
	 *            Nombre de trains qui sera demandé (Conseil : si le serveur le
	 *            permet, mettre en cache la liste des départs directement)
	 * @return
	 * @throws IOException
	 */
	public SparseArray<String> searchGares(String nom, int nbItems) throws IOException;

	/**
	 * Informations sur un train particulier
	 * 
	 * @param numero
	 * @return
	 * @throws IOException
	 */
	public ProchainTrain.Depart getItem(String numero) throws IOException;

	/**
	 * Liste des arrêts d'un train
	 * 
	 * @param numeroTrain
	 * @return
	 * @throws IOException
	 */
	public List<Map<String, Object>> getArrets(String numeroTrain) throws IOException;

}
