package com.naholyr.android.horairessncf.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 5;
	private static final String DATABASE_NAME = "gares.db";

	public static final String TABLE_GARES = "gares";

	private static final String TAG = DatabaseHelper.class.getName();

	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		Log.i(TAG, "CREATE TABLES");
		db.execSQL("CREATE TABLE gares (_id INTEGER PRIMARY KEY, nom VARCHAR UNIQUE, index_nom VARCHAR, region VARCHAR NOT NULL, adresse VARCHAR NOT NULL, latitude DOUBLE NOT NULL, longitude DOUBLE NOT NULL, favorite TINYINT NOT NULL DEFAULT 0)");
		db.execSQL("CREATE INDEX IF NOT EXISTS index_nom ON gares (index_nom)");
		db.execSQL("CREATE INDEX IF NOT EXISTS latitude_longitude ON gares (latitude, longitude)");
		db.execSQL("CREATE INDEX IF NOT EXISTS favorite ON gares (favorite)");
		db.execSQL("CREATE TABLE db_updates (categorie VARCHAR, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.i(TAG, "DROP TABLES");
		db.execSQL("DROP TABLE IF EXISTS gares");
		db.execSQL("DROP TABLE IF EXISTS db_updates");
		onCreate(db);
	}

	public static String getLastUpdate(SQLiteDatabase db) {
		String s = null;
		Cursor c = db.query("db_updates", new String[] { "MAX(updated_at)" }, "categorie=\"" + DatabaseHelper.TABLE_GARES + "\"", null, null, null, null);
		if (c.moveToFirst()) {
			s = c.getString(0);
		}
		c.close();
		return s;
	}

}
