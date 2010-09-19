package com.naholyr.android.horairessncf;

import org.acra.CrashReportingApplication;

import android.os.Bundle;

public class MyApplication extends CrashReportingApplication {

	@Override
	public String getFormId() {
		return "dDZCNjJxYnIyeDdyM1pud3lSRWQ4QkE6MQ";
	}

	@Override
	public Bundle getCrashResources() {
		Bundle result = new Bundle();
		result.putInt(RES_NOTIF_TICKER_TEXT, R.string.crash_notif_ticker_text);
		result.putInt(RES_NOTIF_TITLE, R.string.crash_notif_title);
		result.putInt(RES_NOTIF_TEXT, R.string.crash_notif_text);
		result.putInt(RES_NOTIF_ICON, android.R.drawable.stat_notify_error);
		result.putInt(RES_DIALOG_TEXT, R.string.crash_dialog_text);
		result.putInt(RES_DIALOG_ICON, android.R.drawable.ic_dialog_info);
		result.putInt(RES_DIALOG_TITLE, R.string.crash_dialog_title);
		result.putInt(RES_DIALOG_COMMENT_PROMPT, R.string.crash_dialog_comment_prompt);
		result.putInt(RES_DIALOG_OK_TOAST, R.string.crash_dialog_ok_toast);
		return result;
	}

}
