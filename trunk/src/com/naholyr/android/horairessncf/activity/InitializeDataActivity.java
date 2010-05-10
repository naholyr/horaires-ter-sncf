// WorkInProgress : activity de mise à jour des données depuis le site
package com.naholyr.android.horairessncf.activity;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.database.SQLException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.naholyr.android.horairessncf.DataHelper;
import com.naholyr.android.horairessncf.R;
import com.naholyr.android.horairessncf.Util;

public class InitializeDataActivity extends Activity {

	// Notification ID when using UpdateThread
	private static final int NOTIFICATION_ID = R.string.update_notification;
	private static final long UPDATE_CHECK_INTERVAL = 1000 * 12 * 3600;

	// Download url
	private static final String SPEC = "http";
	private static final String HOST = "termobile-ws.sfhost.net";
	private static final String PATH_DATA = "/data/gares.utf8.txt";
	private static final String PATH_HASH = "/data/gares.utf8.txt.md5";

	// Download buffer
	private static final int BUFFER_SIZE = 8192;

	// Messages
	private static final int MSG_SET_TEXT = 1;
	private static final int MSG_SET_PB_DETERMINATE = 2;
	private static final int MSG_SET_PB_MAX = 3;
	private static final int MSG_SET_PB_PROGRESS = 4;
	private static final int MSG_SET_VISIBILITY = 5;

	// Results
	public static final int RESULT_UPDATED = 1;
	public static final int RESULT_NO_UPDATE = 2;
	public static final int RESULT_ERROR = 3;

	// State of the view, to restore it when configuration change
	Bundle viewState = new Bundle();

	MainThread mainThread;

	private final Handler mHandler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			switch (msg.what) {
				case MSG_SET_TEXT: {
					if (msg.arg2 == 0) {
						((TextView) findViewById(msg.arg1)).setText((String) msg.obj);
					} else {
						((TextView) findViewById(msg.arg1)).setText(msg.arg2);
					}
					return true;
				}
				case MSG_SET_PB_DETERMINATE: {
					((ProgressBar) findViewById(msg.arg1)).setIndeterminate(msg.arg2 == 0);
					return true;
				}
				case MSG_SET_PB_MAX: {
					((ProgressBar) findViewById(msg.arg1)).setMax(msg.arg2);
					return true;
				}
				case MSG_SET_PB_PROGRESS: {
					((ProgressBar) findViewById(msg.arg1)).setProgress(msg.arg2);
					return true;
				}
				case MSG_SET_VISIBILITY: {
					findViewById(msg.arg1).setVisibility(msg.arg2);
					return true;
				}
				default: {
					return false;
				}
			}
		}
	});

	private void sendMsg(int what, int arg1, int arg2) {
		mHandler.sendMessage(Message.obtain(mHandler, what, arg1, arg2));
	}

	private void sendMsg(int what, int arg1, int arg2, Object obj) {
		mHandler.sendMessage(Message.obtain(mHandler, what, arg1, arg2, obj));
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTitle("Mise à jour des données...");

		setContentView(R.layout.initialize_data);

		new Thread(new Runnable() {
			@Override
			public void run() {
				// Complete view
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						((Button) findViewById(R.id.InitDialog_ButtonAbout)).setOnClickListener(new View.OnClickListener() {
							@Override
							public void onClick(View v) {
								startActivity(new Intent(InitializeDataActivity.this, AboutActivity.class));
							}
						});
					}
				});

				// Cancel current notification
				NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
				notificationManager.cancel(NOTIFICATION_ID);

				// Update database
				try {
					DataHelper helper = DataHelper.getInstance(InitializeDataActivity.this);
					mainThread = new MainThread(helper);
					mainThread.start();
				} catch (IOException e) {
					Util.showError(InitializeDataActivity.this, "Erreur lors de la mise à jour des données ! Essayez de redémarrer l'application SVP.");
				}
			}
		}).start();
	}

	private final class MainThread extends Thread {

		private DataHelper mHelper;

		public boolean working = false;

		private final void error(final String msg) {
			setResult(RESULT_ERROR);
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Util.showError(InitializeDataActivity.this, "Erreur : " + msg);
				}
			});
		}

		private void finishWithResult(int result) {
			setResult(result);
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Activity parent = getParent();
					if (parent != null) {
						Toast.makeText(parent, "Mise à jour terminée", Toast.LENGTH_SHORT).show();
					}
					InitializeDataActivity.this.finish();
				}
			});
		}

		public boolean isWorking() {
			return working;
		}

		public void cancel() {
			working = false;
			setResult(RESULT_CANCELED);
		}

		public MainThread(DataHelper helper) {
			mHelper = helper;
		}

		public void run() {
			working = true;
			try {
				String[] lines;
				String signature;
				// 1. Récupération du MD5
				if (working) {
					URL url = new URL(SPEC, HOST, 80, PATH_HASH);
					HttpURLConnection connection;
					try {
						connection = (HttpURLConnection) url.openConnection();
						connection.connect();
					} catch (IOException e) {
						error("Impossible de télécharger le fichier de signature (URL inaccessible !)");
						working = false;
						return;
					}
					if (connection.getResponseCode() / 100 != 2) {
						error("Fichier introuvable !");
					}
					InputStream stream = connection.getInputStream();
					byte[] buffer = new byte[32];
					stream.read(buffer);
					signature = new String(buffer);
					viewState.putBoolean("signature", true);
					sendMsg(MSG_SET_TEXT, R.id.InitDialog_TextRemoteMD5, 0, "OK");
				} else {
					return;
				}
				// 2. Téléchargement fichier
				if (working) {
					URL url = new URL(SPEC, HOST, 80, PATH_DATA);
					HttpURLConnection connection;
					try {
						connection = (HttpURLConnection) url.openConnection();
						connection.connect();
					} catch (IOException e) {
						error("Impossible de télécharger le fichier de données (URL inaccessible !)");
						working = false;
						return;
					}
					if (connection.getResponseCode() / 100 != 2) {
						error("Fichier introuvable !");
					}
					int contentLength = connection.getContentLength();
					viewState.putInt("dl_total", contentLength);
					if (contentLength > 0) {
						sendMsg(MSG_SET_PB_DETERMINATE, R.id.InitDialog_ProgressDownload, 1);
						sendMsg(MSG_SET_PB_MAX, R.id.InitDialog_ProgressDownload, contentLength);
						sendMsg(MSG_SET_TEXT, R.id.InitDialog_TextTotalDownload, 0, String.valueOf((int) (contentLength / 1024)));
						sendMsg(MSG_SET_TEXT, R.id.InitDialog_TextDownload, 0, "0");
					} else {
						sendMsg(MSG_SET_TEXT, R.id.InitDialog_TextDownload, 0, "0");
						sendMsg(MSG_SET_PB_DETERMINATE, R.id.InitDialog_ProgressDownload, 0);
					}
					sendMsg(MSG_SET_VISIBILITY, R.id.InitDialog_TextDownloadLayout, View.VISIBLE);
					InputStream stream = connection.getInputStream();
					byte[] buffer;
					int read;
					int progress = 0;
					String fullFile = "";
					do {
						if (!working) {
							return;
						}
						buffer = new byte[BUFFER_SIZE];
						read = stream.read(buffer);
						fullFile += new String(buffer);
						progress += read;
						viewState.putInt("dl_progress", progress);
						if (contentLength > 0) {
							sendMsg(MSG_SET_PB_PROGRESS, R.id.InitDialog_ProgressDownload, progress);
						}
						sendMsg(MSG_SET_TEXT, R.id.InitDialog_TextDownload, 0, String.valueOf((int) (progress / 1024)));
					} while (read != -1);
					// Listing des lignes pour étape suivante
					lines = fullFile.trim().split("\n");
				} else {
					return;
				}
				// Démarrage d'une transaction
				if (working) {
					mHelper.getDb().beginTransaction();
				} else {
					return;
				}
				// 3. Chargement des données
				if (working) {
					viewState.putInt("load_total", lines.length);
					sendMsg(MSG_SET_PB_DETERMINATE, R.id.InitDialog_ProgressLoad, 1);
					sendMsg(MSG_SET_PB_PROGRESS, R.id.InitDialog_ProgressLoad, 0);
					sendMsg(MSG_SET_PB_MAX, R.id.InitDialog_ProgressLoad, lines.length);
					sendMsg(MSG_SET_TEXT, R.id.InitDialog_TextTotalGares, 0, String.valueOf(lines.length));
					sendMsg(MSG_SET_TEXT, R.id.InitDialog_TextNumGare, 0, "0");
					sendMsg(MSG_SET_VISIBILITY, R.id.InitDialog_TextLoadLayout, View.VISIBLE);
					// progression par tranches
					int mult = Math.max(1, lines.length / 50);
					int progress = 0;
					viewState.putInt("load_progress", lines.length);
					mHelper.truncate();
					for (int i = 0; i < lines.length; i++) {
						if (!working) {
							// rollback and leave
							mHelper.getDb().endTransaction();
							return;
						}
						String line = lines[i];
						// Format :
						// DAltkirch#Alsace#Alsace,France#48.3181795#7.4416241
						String[] parts = line.split("#");
						if (parts.length > 0) {
							try {
								String nom = parts[0];
								String region = parts[1];
								String adresse = parts[2];
								double latitude = Double.valueOf(parts[3]);
								double longitude = Double.valueOf(parts[4]);
								progress++;
								if (progress % mult == 0) {
									viewState.putInt("load_progress", progress);
									sendMsg(MSG_SET_PB_PROGRESS, R.id.InitDialog_ProgressLoad, progress);
									sendMsg(MSG_SET_TEXT, R.id.InitDialog_TextNumGare, 0, String.valueOf(progress));
								}
								mHelper.insert(nom, region, adresse, latitude, longitude);
							} catch (NumberFormatException e) {
								Log.e(getClass().getName(), line, e);
							} catch (SQLException e) {
								Log.e(getClass().getName(), "SQL Error", e);
							}
						}
					}
					viewState.putInt("load_progress", lines.length);
					sendMsg(MSG_SET_PB_PROGRESS, R.id.InitDialog_ProgressLoad, lines.length);
				} else {
					return;
				}
				// 4. Stocker la nouvelle version du fichier
				if (working) {
					mHelper.saveNewUpdateHash(signature);
				}
				// Finaliser la transaction
				if (working) {
					mHelper.getDb().setTransactionSuccessful();
				}
				mHelper.getDb().endTransaction();
				// Finir l'activity
				int result = working ? RESULT_UPDATED : RESULT_CANCELED;
				working = false;
				finishWithResult(result);
			} catch (MalformedURLException e) {
				error(e.getLocalizedMessage());
			} catch (IOException e) {
				error(e.getLocalizedMessage());
			}
		}

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		setContentView(R.layout.initialize_data);

		if (viewState.containsKey("signature") && viewState.getBoolean("signature")) {
			((TextView) findViewById(R.id.InitDialog_TextRemoteMD5)).setText("OK");
		}

		if (viewState.containsKey("dl_total")) {
			int contentLength = viewState.getInt("dl_total");
			ProgressBar b = (ProgressBar) findViewById(R.id.InitDialog_ProgressDownload);
			if (contentLength > 0) {
				b.setIndeterminate(false);
				b.setMax(contentLength);
				((TextView) findViewById(R.id.InitDialog_TextTotalDownload)).setText(String.valueOf((int) (contentLength / 1024)));
				int progress = viewState.containsKey("dl_progress") ? viewState.getInt("dl_progress") : 0;
				b.setProgress(progress);
				((TextView) findViewById(R.id.InitDialog_TextDownload)).setText(String.valueOf((int) (progress / 1024)));
				findViewById(R.id.InitDialog_TextDownloadLayout).setVisibility(View.VISIBLE);
			} else {
				b.setIndeterminate(true);
			}
		}

		if (viewState.containsKey("load_total")) {
			int total = viewState.getInt("load_total");
			ProgressBar b = (ProgressBar) findViewById(R.id.InitDialog_ProgressLoad);
			b.setIndeterminate(false);
			b.setMax(total);
			((TextView) findViewById(R.id.InitDialog_TextTotalGares)).setText(String.valueOf(total));
			int progress = viewState.containsKey("load_progress") ? viewState.getInt("load_progress") : 0;
			b.setProgress(progress);
			((TextView) findViewById(R.id.InitDialog_TextNumGare)).setText(String.valueOf(progress));
			findViewById(R.id.InitDialog_TextLoadLayout).setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void finish() {
		if (mainThread != null && mainThread.isWorking()) {
			mainThread.cancel();
		}
		super.finish();
	}

	protected final static class UpdateThread extends Thread {

		private Context mContext;

		public UpdateThread(Context context) {
			mContext = context;
		}

		public void run() {
			// Do we need to check ?
			SharedPreferences prefs = mContext.getSharedPreferences(Util.PREFS_DATA, Context.MODE_PRIVATE);
			long lastCheck = prefs.getLong("last_update_check", 0);
			long now = System.currentTimeMillis();
			if (now - lastCheck >= UPDATE_CHECK_INTERVAL) {
				// Check if there is a new version
				String currentVersion;
				try {
					DataHelper helper = DataHelper.getInstance(mContext);
					currentVersion = helper.getLastUpdateHash();
				} catch (IOException e) {
					// Failed opening helper = no current version
					currentVersion = null;
				}
				// If we have a current version, check the latest one
				boolean doUpdate = true;
				if (currentVersion != null) {
					try {
						URL url = new URL(SPEC, HOST, 80, PATH_HASH);
						HttpURLConnection connection = (HttpURLConnection) url.openConnection();
						connection.connect();
						InputStream stream = connection.getInputStream();
						byte[] buffer = new byte[32];
						stream.read(buffer);
						String latestVersion = new String(buffer);
						doUpdate = !latestVersion.equals(currentVersion);
					} catch (MalformedURLException e) {
						return;
					} catch (IOException e) {
						return;
					}
				}
				// If there is an update available, notify user
				if (doUpdate) {
					NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(NOTIFICATION_SERVICE);
					// Initialize notification
					int icon = R.drawable.icon;
					CharSequence tickerText = mContext.getString(R.string.update_notification);
					long when = System.currentTimeMillis();
					Notification notification = new Notification(icon, tickerText, when);
					// Expanded message + intent
					CharSequence contentTitle = "Mise à jour des gares";
					CharSequence contentText = "Cliquez ici pour mettre à jour la liste des gares";
					Intent notificationIntent = new Intent(mContext, InitializeDataActivity.class);
					PendingIntent contentIntent = PendingIntent.getActivity(mContext, 0, notificationIntent, 0);
					notification.setLatestEventInfo(mContext, contentTitle, contentText, contentIntent);
					// Send notification
					notificationManager.notify(NOTIFICATION_ID, notification);
				}
				// Remember the last check datetime
				Editor editor = prefs.edit();
				editor.putLong("last_update_check", System.currentTimeMillis());
				editor.commit();
			}
		}

	}

}
