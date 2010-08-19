package com.naholyr.android.horairessncf.service;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class UpdateServiceManager extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent2) {
		if (Intent.ACTION_BOOT_COMPLETED.equals(intent2.getAction())) {
			UpdateServiceAlarmReceiver.scheduleNext(context, AlarmManager.INTERVAL_FIFTEEN_MINUTES);
		}
	}

	public static void start(Context context) {
		UpdateServiceAlarmReceiver.scheduleNext(context, 0);
	}

}
