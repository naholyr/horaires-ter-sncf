package com.naholyr.android.horairessncf;

import java.io.IOException;

import com.naholyr.android.horairessncf.activity.ProgressHandlerActivity;
import com.naholyr.android.horairessncf.termobile.IBrowser;
import com.naholyr.android.horairessncf.termobile.JSONServerBrowser;

public class TERMobileBrowser {

	private static IBrowser instance = null;

	public static IBrowser getInstance(ProgressHandlerActivity activity, int dialogId) throws IOException {
		if (instance == null) {
			instance = new JSONServerBrowser(activity, dialogId);
		} else {
			instance.setProgressHandlerActivity(activity);
			instance.setProgressHandlerDialogId(dialogId);
		}

		return instance;
	}

}
