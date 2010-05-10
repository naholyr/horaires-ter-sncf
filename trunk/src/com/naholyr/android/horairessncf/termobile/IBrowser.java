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

	public abstract List<ProchainTrain.Depart> getItems(int nbItems) throws IOException;

	public abstract List<ProchainTrain.Depart> getItems(int nbItems, boolean refresh) throws IOException;

	public abstract void confirmGare(int id);

	public abstract SparseArray<String> searchGares(String nom) throws IOException;

}