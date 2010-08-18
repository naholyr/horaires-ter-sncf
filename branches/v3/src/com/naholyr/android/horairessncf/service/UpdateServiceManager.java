package com.naholyr.android.horairessncf.service;

import org.acra.ErrorReporter;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;

public class UpdateServiceManager extends BroadcastReceiver {

	private static final String PREFS_FILE = "update_service_manager";
	private static final String PREF_KEY = "started_version";

	public static void initialize(Context context) {
		if (!initialized(context)) {
			UpdateServiceAlarmReceiver.scheduleNext(context, AlarmManager.INTERVAL_FIFTEEN_MINUTES);
			markAsInitialized(context);
		}
	}

	private static void markAsInitialized(Context context) {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
		try {
			int version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
			prefs.edit().putInt(PREF_KEY, version).commit();
		} catch (NameNotFoundException e) {
			ErrorReporter.getInstance().handleException(e);
		}
	}

	public static boolean initialized(Context context) {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
		int version;
		try {
			version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
			ErrorReporter.getInstance().handleException(e);
			return false;
		}
		return prefs.getInt(PREF_KEY, 0) == version;
	}

	@Override
	public void onReceive(Context context, Intent intent2) {
		if (Intent.ACTION_BOOT_COMPLETED.equals(intent2.getAction())) {
			initialize(context);
		}
	}

}
