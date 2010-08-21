package com.naholyr.android.horairessncf.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

import com.naholyr.android.horairessncf.R;
import com.naholyr.android.horairessncf.activity.UpdateActivity;

public class UpdateService extends BroadcastReceiver {

	public static final String TAG = UpdateService.class.getName();

	public static final String ALARM_ACTION = "com.naholyr.android.horairessncf.action.UPDATE_GARES";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "Received alarm " + intent.getAction());
		if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
			scheduleNext(context, AlarmManager.INTERVAL_FIFTEEN_MINUTES);
		} else if (ALARM_ACTION.equals(intent.getAction())) {
			// Check latest update
			try {
				SQLiteDatabase db = new DatabaseHelper(context).getReadableDatabase();
				String updateDate = DatabaseHelper.getLastUpdate(db);
				db.close();
				if (updateDate == null) {
					// No data, force update
					sendUpdateNotification(context);
				} else {
					// Retrieve update file
					InputStream stream = null;
					try {
						String path = "/data/gares.php" + (updateDate != null ? "?last_update=" + URLEncoder.encode(updateDate) : "");
						URL url = new URL("http", "horaires-ter-sncf.naholyr.fr", 80, path);
						HttpURLConnection connection = (HttpURLConnection) url.openConnection();
						stream = connection.getInputStream();
					} catch (IOException e) {
						Log.e(TAG, "Error retrieving update file", e);
					}
					BufferedReader reader = new BufferedReader(new InputStreamReader(stream), 512);
					// Read first line = number of stations
					try {
						String line = reader.readLine();
						if (line != null) {
							try {
								if (Integer.parseInt(line.trim()) > 0) {
									sendUpdateNotification(context);
								} else {
									Log.i(TAG, "No update available");
								}
							} catch (NumberFormatException e) {
								Log.e(TAG, "Invalid number in update file : " + line, e);
							}
						} else {
							Log.e(TAG, "No data in update file");
						}
					} catch (IOException e) {
						Log.e(TAG, "Cannot read update file", e);
					}
				}
			} catch (SQLException e) {
				Log.e(TAG, "Database already in use ?");
			}

			// Schedule next check
			scheduleNext(context, AlarmManager.INTERVAL_HALF_DAY);
		}
	}

	private void sendUpdateNotification(Context context) {
		NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		CharSequence tickerText = "Mise à jour des gares disponible";
		Notification notification = new Notification(R.drawable.icon, tickerText, System.currentTimeMillis());
		Intent intent = new Intent(context, UpdateActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
		notification.setLatestEventInfo(context, "Mise à jour des gares", "Cliquez pour mettre à jour vos données", pendingIntent);
		mNotificationManager.notify(UpdateActivity.NOTIFICATION_ID, notification);
	}

	public static PendingIntent getPendingIntent(Context context) {
		Intent i = new Intent(context, UpdateService.class);
		i.setAction(UpdateService.ALARM_ACTION);
		return PendingIntent.getBroadcast(context, 0, i, 0);
	}

	public static void scheduleNext(Context context, long interval) {
		scheduleNext(context, interval, false);
	}
	
	public static void scheduleNext(Context context, long interval, boolean toast) {
		// Schedule automatic updates
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		long time = System.currentTimeMillis() + interval;
		PendingIntent intent = getPendingIntent(context);
		alarmManager.set(AlarmManager.RTC_WAKEUP, time, intent);
		// Log & toast
		Log.d(TAG, "Scheduled alarm to " + DateFormat.format("dd/MM/dd/yy h:mm", time));
		if (toast) {
			Toast.makeText(context, "Prochaine vérification planifiée pour " + DateFormat.format("h:mm", time), Toast.LENGTH_SHORT).show();
		}
	}

	public static void scheduleNow(Context context) {
		scheduleNow(context, false);
	}

	public static void scheduleNow(Context context, boolean toast) {
		scheduleNext(context, 100, false);
		// Log & toast
		if (toast) {
			Toast.makeText(context, "Vérification des mises à jour démarrée...", Toast.LENGTH_SHORT).show();
		}
	}

	public static void unschedule(Context context) {
		unschedule(context, false);
	}

	public static void unschedule(Context context, boolean toast) {
		AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		PendingIntent intent = getPendingIntent(context);
		alarmManager.cancel(intent);
		// Log & toast
		Log.d(TAG, "Cancelled alarm");
		if (toast) {
			Toast.makeText(context, "Vérifications de mise à jour désactivées", Toast.LENGTH_SHORT).show();
		}
	}
	
}
