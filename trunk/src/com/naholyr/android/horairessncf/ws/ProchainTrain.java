package com.naholyr.android.horairessncf.ws;

import java.util.List;

public interface ProchainTrain {

	public static final int TYPE_AUTRE = 0;
	public static final int TYPE_TER = 1;
	public static final int TYPE_TGV = 2;
	public static final int TYPE_CAR = 3;
	public static final int TYPE_CORAIL = 4;

	public static interface Retard {

		public String getDuree();

		public String getMotif();

	}

	// Identification

	public int getType();

	public String getTypeLabel();

	public String getNumero();

	public String getOrigine();

	public String getDestination();

	// Horaire

	public String getHeure();

	public List<Retard> getRetards();

	// Situation

	public boolean isAQuai();

	public boolean isSupprime();

	public String getVoie();

	// Arrivée / Départ

	public static abstract class Arrivee implements ProchainTrain {
		public String getDestination() {
			return null;
		}
	}

	public static abstract class Depart implements ProchainTrain {
		public String getOrigine() {
			return null;
		}
	}

}
