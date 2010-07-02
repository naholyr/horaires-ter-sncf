// WorkInProgress : activity de mise à jour des données depuis le site
package com.naholyr.android.horairessncf.activity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import android.os.PowerManager;
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
	private static final long UPDATE_CHECK_INTERVAL = 12 * 3600;

	// Buffer size
	private static final int LINE_BUFFER_SIZE = 512;
	private static final int HASH_SIZE = 32;

	// Download url
	private static final String SPEC = "http";
	private static final String HOST = "termobile-ws.sfhost.net";
	private static final String PATH_DATA = "/data/gares.utf8.txt";
	private static final String PATH_HASH = "/data/gares.utf8.txt.md5";

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

	// Main thread
	MainThread mainThread;

	// Messages handler from main thread and children
	private final Handler mHandler = new Handler(new Handler.Callback() {
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

	// Shortcut to send messages to handler
	private void sendMsg(int what, int arg1, int arg2) {
		mHandler.sendMessage(Message.obtain(mHandler, what, arg1, arg2));
	}

	private void sendMsg(int what, int arg1, int arg2, Object obj) {
		mHandler.sendMessage(Message.obtain(mHandler, what, arg1, arg2, obj));
	}

	// Initialization
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTitle("Mise à jour des données...");

		setContentView(R.layout.initialize_data);

		((Button) findViewById(R.id.InitDialog_ButtonAbout)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startActivity(new Intent(InitializeDataActivity.this, AboutActivity.class));
			}
		});

		new Thread() {
			@Override
			public void run() {
				// Cancel current notification
				NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
				notificationManager.cancel(NOTIFICATION_ID);

				// Update database
				try {
					final DataHelper helper = DataHelper.getInstance(InitializeDataActivity.this);
					mainThread = new MainThread(helper);
					mainThread.start();
				} catch (IOException e) {
					Util.showError(InitializeDataActivity.this, "Erreur lors de la mise à jour des données ! Essayez de redémarrer l'application SVP.", e);
				}
			}
		}.start();
	}

	private final class MainThread extends Thread {

		private DataHelper mHelper;

		private PowerManager.WakeLock mWakeLock;

		public boolean mWorking = false;

		private final void error(String msg) {
			error(msg, null);
		}

		private final void error(Throwable e) {
			error(null, e);
		}

		private final void error(final String msg, final Throwable e) {
			setResult(RESULT_ERROR);
			if (mWorking) {
				final Runnable finishAfterError = new Runnable() {
					public void run() {
						finishWithResult(RESULT_ERROR);
					}
				};
				runOnUiThread(new Runnable() {
					public void run() {
						if (e != null) {
							if (msg != null) {
								Util.showError(InitializeDataActivity.this, msg, e, finishAfterError);
							} else {
								Util.showError(InitializeDataActivity.this, e, finishAfterError);
							}
						} else if (msg != null) {
							Util.showError(InitializeDataActivity.this, msg, finishAfterError);
						}
					}
				});
			}
			mWorking = false;
		}

		private void finishWithResult(int result) {
			setResult(result);
			runOnUiThread(new Runnable() {
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
			return mWorking;
		}

		public void cancel() {
			mWorking = false;
			if (mHelper.getDb().inTransaction()) {
				mHelper.getDb().endTransaction();
			}
			if (mWakeLock != null) {
				mWakeLock.release();
				mWakeLock = null;
			}
			setResult(RESULT_CANCELED);
		}

		public MainThread(DataHelper helper) {
			mHelper = helper;
		}

		@Override
		public void run() {
			mWorking = true;
			try {
				String signature;
				// 1. Récupération du MD5
				if (mWorking) {
					URL url = new URL(SPEC, HOST, 80, PATH_HASH);
					HttpURLConnection connection;
					try {
						connection = (HttpURLConnection) url.openConnection();
						connection.connect();
					} catch (IOException e) {
						error("Impossible de télécharger le fichier de signature (URL inaccessible !)", e);
						return;
					}
					if (connection.getResponseCode() != 200 && connection.getResponseCode() != 302) {
						error("Fichier introuvable (statut incorrect) !");
						return;
					}
					InputStream stream = connection.getInputStream();
					byte[] buffer = new byte[HASH_SIZE];
					stream.read(buffer);
					signature = new String(buffer);
					viewState.putBoolean("signature", true);
					sendMsg(MSG_SET_TEXT, R.id.InitDialog_TextRemoteMD5, 0, "OK");
				} else {
					return;
				}
				// Démarrage d'une transaction
				if (mWorking) {
					mHelper.getDb().beginTransaction();
				} else {
					return;
				}
				// 2. Chargement des données
				if (mWorking) {
					URL url = new URL(SPEC, HOST, 80, PATH_DATA);
					HttpURLConnection connection;
					try {
						connection = (HttpURLConnection) url.openConnection();
						connection.connect();
					} catch (IOException e) {
						error("Impossible de télécharger le fichier de données (URL inaccessible !)", e);
						return;
					}
					if (connection.getResponseCode() != 200 && connection.getResponseCode() != 302) {
						error("Fichier introuvable (statut incorrect) !");
						return;
					}
					int contentLength = connection.getContentLength();
					if (contentLength > 0) {
						int kb = (int) (contentLength / 1024);
						sendMsg(MSG_SET_TEXT, R.id.InitDialog_TextTotalDownload, 0, String.valueOf(kb));
						viewState.putInt("download_total", kb);
					} else {
						sendMsg(MSG_SET_TEXT, R.id.InitDialog_TextTotalDownload, 0, "?");
					}
					sendMsg(MSG_SET_PB_DETERMINATE, R.id.InitDialog_ProgressLoad, 1);
					sendMsg(MSG_SET_PB_PROGRESS, R.id.InitDialog_ProgressLoad, 0);
					sendMsg(MSG_SET_PB_MAX, R.id.InitDialog_ProgressLoad, Util.NB_GARES_TOTAL);
					sendMsg(MSG_SET_TEXT, R.id.InitDialog_TextTotalGares, 0, String.valueOf(Util.NB_GARES_TOTAL));
					viewState.putInt("load_total", Util.NB_GARES_TOTAL);
					sendMsg(MSG_SET_TEXT, R.id.InitDialog_TextNumGare, 0, "0");
					sendMsg(MSG_SET_VISIBILITY, R.id.InitDialog_TextLoadLayout, View.VISIBLE);
					viewState.putInt("load_progress", 0);
					// Vérouiller l'écran pendant le téléchargement
					PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
					mWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "HorairesSNCF_Download");
					mWakeLock.acquire();
					// Buffered reader pour du ligne à ligne
					BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()), LINE_BUFFER_SIZE);
					String line;
					int numGare = 0;
					int numErrors = 0;
					int numMaxErrors = 5 * Util.NB_GARES_TOTAL / 100;
					mHelper.truncate();
					while (true) {
						try {
							line = reader.readLine();
							if (line == null) {
								break;
							}
						} catch (IOException e) {
							line = null;
							numErrors++;
						}
						if (line != null) {
							// Format :
							// DAltkirch#Alsace#Alsace,France#48.3181795#7.4416241
							String[] parts = line.split("#", 5);
							if (parts.length == 5) {
								try {
									String nom = parts[0];
									String region = parts[1];
									String adresse = parts[2];
									double latitude = Double.valueOf(parts[3]);
									double longitude = Double.valueOf(parts[4]);
									mHelper.insert(nom, region, adresse, latitude, longitude);
								} catch (NumberFormatException e) {
									Log.e(getClass().getName(), line, e);
									numErrors++;
								} catch (SQLException e) {
									Log.e(getClass().getName(), "SQL Error", e);
									numErrors++;
								}
							} else {
								Log.e("horairessncf", "Skip invalid line " + line);
								numErrors++;
							}
						}
						if (numErrors > numMaxErrors) {
							error("Trop d'erreurs rencontrées dans le fichier (+ de 5%), merci d'essayer encore.", new Throwable(numErrors + " erreurs au chargement"));
							return;
						}
						// Enregistrer la progression
						numGare++;
						if (numGare % (Util.NB_GARES_TOTAL / 50) == 0) {
							sendMsg(MSG_SET_TEXT, R.id.InitDialog_TextNumGare, 0, String.valueOf(numGare));
							sendMsg(MSG_SET_PB_PROGRESS, R.id.InitDialog_ProgressLoad, numGare);
						}
						viewState.putInt("load_progress", numGare);
					}
					// Tolérance : on tolère une marge de 5% d'erreurs + 5%
					// d'incohérence dans la constante NB_GARES_TOTAL
					if (numGare < Util.NB_GARES_TOTAL - 2 * numMaxErrors) {
						error("Après chargement des données, le nombre de gares (" + numGare + ") semble incohérent avec le nombre attendu. Essayez de relancer l'application SVP",
								new Throwable(numGare + " gares chargées incohérent"));
						return;
					}
					mWakeLock.release();
					mWakeLock = null;
				} else {
					return;
				}
				// 3. Stocker la nouvelle version du fichier
				if (mWorking) {
					mHelper.saveNewUpdateHash(signature);
				}
				// Finaliser la transaction
				if (mWorking) {
					mHelper.getDb().setTransactionSuccessful();
				}
				mHelper.getDb().endTransaction();
				// Finir l'activity
				int result = mWorking ? RESULT_UPDATED : RESULT_CANCELED;
				mWorking = false;
				finishWithResult(result);
			} catch (MalformedURLException e) {
				error(e);
				return;
			} catch (IOException e) {
				error(e);
				return;
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

		if (viewState.containsKey("download_total")) {
			((TextView) findViewById(R.id.InitDialog_TextTotalDownload)).setText(String.valueOf(viewState.getInt("download_total")));
		} else {
			((TextView) findViewById(R.id.InitDialog_TextTotalDownload)).setText("?");
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

		@Override
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
