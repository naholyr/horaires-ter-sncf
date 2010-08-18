package com.naholyr.android.horairessncf.activity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;

import com.naholyr.android.horairessncf.Gare;
import com.naholyr.android.horairessncf.R;
import com.naholyr.android.horairessncf.providers.DatabaseHelper;

public class UpdateActivity extends Activity {

	// Notification
	public static final int NOTIFICATION_ID = R.string.update_notification;

	// Progress dialog
	static final int PROGRESS_DIALOG = 0;
	static final int MSG_PROGRESS = 0;
	static final int MSG_FINISHED = 1;
	static final int MSG_SET_MAX = 2;
	static final int MSG_SET_MESSAGE = 3;

	// Results
	public static final int RESULT_NO_UPDATE = RESULT_FIRST_USER;
	public static final int RESULT_ERROR = RESULT_FIRST_USER + 1;

	PowerManager.WakeLock mWakeLock;
	ProgressThread progressThread;
	ProgressDialog progressDialog;

	/**
	 * Initialization : show progress dialog
	 */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		showDialog(PROGRESS_DIALOG);
	}

	/**
	 * Progress dialog
	 */
	protected Dialog onCreateDialog(int id) {
		switch (id) {
			case PROGRESS_DIALOG:
				progressDialog = new ProgressDialog(this);
				progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				progressDialog.setMessage("Contact serveur...");
				progressDialog.setTitle("Mise à jour des gares");
				progressDialog.setCancelable(true);
				progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						if (progressThread != null) {
							if (progressThread.isAlive()) {
								progressThread.setState(ProgressThread.STATE_DONE);
								setResult(RESULT_CANCELED);
								finish();
							}
						}
					}
				});
				// Thread attached to progress dialog
				progressThread = new ProgressThread(handler);
				progressThread.start();
				return progressDialog;
			default:
				return null;
		}
	}

	/**
	 * Handle communication between thread and progress bar
	 */
	final Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			if (msg.what == MSG_PROGRESS) {
				progressDialog.setProgress(msg.arg1);
			} else if (msg.what == MSG_FINISHED) {
				dismissDialog(PROGRESS_DIALOG);
				progressThread.setState(ProgressThread.STATE_DONE);
				setResult(RESULT_OK);
				finish();
			} else if (msg.what == MSG_SET_MAX) {
				progressDialog.setMax(msg.arg1);
			} else if (msg.what == MSG_SET_MESSAGE) {
				progressDialog.setMessage((String) msg.obj);
			}
		}
	};

	/**
	 * Main thread
	 */
	private class ProgressThread extends Thread {
		Handler mHandler;
		final static int STATE_DONE = 0;
		final static int STATE_RUNNING = 1;
		int mState;

		ProgressThread(Handler h) {
			mHandler = h;
		}

		public void run() {
			mState = STATE_RUNNING;

			// Cancel current notification
			if (mState == STATE_RUNNING) {
				NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
				notificationManager.cancel(NOTIFICATION_ID);
			}

			// Vérouiller l'écran pendant le téléchargement
			if (mState == STATE_RUNNING) {
				PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
				mWakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "HorairesSNCF_Download");
				mWakeLock.acquire();
			}

			// Récupérer la base de données
			String updateDate = null;
			SQLiteDatabase db = null;
			if (mState == STATE_RUNNING) {
				db = new DatabaseHelper(getApplicationContext()).getWritableDatabase();
				Cursor c = db.query("db_updates", new String[] { "MAX(updated_at)" }, "categorie=\"" + DatabaseHelper.TABLE_GARES + "\"", null, null, null, null);
				if (c.moveToFirst()) {
					String s = c.getString(0);
					if (s != null) {
						updateDate = s;
					}
				}
				c.close();
			}

			// Récupérer les données
			InputStream stream = null;
			if (mState == STATE_RUNNING) {
				try {
					String path = "/data/gares.php" + (updateDate != null ? "?last_update=" + URLEncoder.encode(updateDate) : "");
					URL url = new URL("http", "horaires-ter-sncf.naholyr.fr", 80, path);
					HttpURLConnection connection = (HttpURLConnection) url.openConnection();
					stream = connection.getInputStream();
				} catch (IOException e) {
					error(e);
				}
			}

			if (mState == STATE_RUNNING) {
				mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_MESSAGE, "Recherche nombre de gares..."));
				BufferedReader reader = new BufferedReader(new InputStreamReader(stream), 512);
				String line;
				int total = 0;
				try {
					// Ligne 1 : nombre de gares
					if (mState == STATE_RUNNING) {
						if ((line = reader.readLine()) != null) {
							try {
								total = Integer.parseInt(line.trim());
								mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_MESSAGE, line.trim() + " mise(s) à jour..."));
								mHandler.sendMessage(mHandler.obtainMessage(MSG_SET_MAX, total, 0));
							} catch (NumberFormatException e) {
								error(e);
							}
						} else {
							error();
						}
					}
					if (total == 0) {
						setResult(RESULT_NO_UPDATE);
						mState = STATE_DONE;
						dismissDialog(PROGRESS_DIALOG);
						finish();
					}
					// Ligne 2 : date de mise à jour
					if (mState == STATE_RUNNING) {
						if ((line = reader.readLine()) != null) {
							updateDate = line.trim();
						} else {
							error();
						}
					}
					// Ligne 3 et suivantes : gares
					db.beginTransaction();
					if (updateDate == null) {
						db.execSQL("DELETE FROM " + DatabaseHelper.TABLE_GARES);
					}
					int progress = 0;
					int numErrors = 0;
					int numMaxErrors = (5 * total) / 100;
					while (mState == STATE_RUNNING && (line = reader.readLine()) != null) {
						// Format :
						// 37#DAltkirch#Alsace#Alsace,France#48.3181795#7.4416241
						String[] parts = line.trim().split("#", 6);
						if (parts.length == 6) {
							try {
								if (parts[2].startsWith("DELETED ")) {
									db.delete(DatabaseHelper.TABLE_GARES, Gare._ID + "=" + parts[0], null);
								} else {
									db.replace(DatabaseHelper.TABLE_GARES, null, Gare.values(Integer.parseInt(parts[0]), parts[2], parts[1], parts[3], Double.valueOf(parts[4]),
											Double.valueOf(parts[5])));
								}
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
						if (numErrors > numMaxErrors) {
							error(new Throwable(numErrors + " erreurs au chargement"));
						}
						// Enregistrer la progression
						if (mState == STATE_RUNNING) {
							progress++;
							if (total <= 50 || progress % (total / 50) == 0) {
								mHandler.sendMessage(mHandler.obtainMessage(MSG_PROGRESS, progress, 0));
							}
						}
					}
					if (mState == STATE_RUNNING) {
						ContentValues values = new ContentValues();
						values.put("categorie", DatabaseHelper.TABLE_GARES);
						values.put("updated_at", updateDate);
						db.insert("db_updates", null, values);
						db.setTransactionSuccessful();
						db.endTransaction();
					}
				} catch (IOException e) {
					error(e);
				}
			}

			// Fin de la tache, on ferme toutes les connexions ouvertes
			if (db != null) {
				if (db.inTransaction()) {
					db.endTransaction();
				}
				db.close();
				// On a modifié les données directement dans la BDD, il faut
				// notifier le content provider que son contenu a changé
				// cela permet d'avertir les ListActivity de se mettre à jour
				getContentResolver().notifyChange(Gare.Gares.CONTENT_URI, null);
			}
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (mWakeLock != null && mWakeLock.isHeld()) {
				mWakeLock.release();
				mWakeLock = null;
			}
			if (mState == STATE_RUNNING) {
				mState = STATE_DONE;
				mHandler.sendEmptyMessage(MSG_FINISHED);
			}
		}

		public void setState(int state) {
			mState = state;
		}

		private void error(Throwable t) {
			t.printStackTrace();
			error();
		}

		private void error() {
			setResult(RESULT_ERROR);
			mState = STATE_DONE;
			dismissDialog(PROGRESS_DIALOG);
			finish();
		}
	}
}