package com.naholyr.android.horairessncf.termobile;

import java.io.IOException;
import java.util.List;

import android.util.SparseArray;

import com.naholyr.android.horairessncf.ProchainTrain;
import com.naholyr.android.horairessncf.activity.ProgressHandlerActivity;

public interface IBrowser {

	public abstract void setProgressHandlerActivity(ProgressHandlerActivity progressHandlerActivity);

	public abstract ProgressHandlerActivity getProgressHandlerActivity();

	public abstract void setProgressHandlerDialogId(int progressHandlerDialogId);

	public abstract int getProgressHandlerDialogId();

	/**
	 * Récupérer la liste des trains, sans forcément redemander la liste au
	 * serveur
	 * 
	 * @param nbItems
	 * @return
	 * @throws IOException
	 */
	public abstract List<ProchainTrain.Depart> getItems(int nbItems) throws IOException;

	/**
	 * Récupérer la liste des trains, en forçant une requête au serveur
	 * 
	 * @param nbItems
	 * @param refresh
	 * @return
	 * @throws IOException
	 */
	public abstract List<ProchainTrain.Depart> getItems(int nbItems, boolean refresh) throws IOException;

	/**
	 * Confirmer la gare exacte par son ID
	 * 
	 * @param id
	 */
	public abstract void confirmGare(int id);

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
	public abstract SparseArray<String> searchGares(String nom, int nbItems) throws IOException;

}