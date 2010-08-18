package com.naholyr.android.horairessncf.providers;

import android.content.Context;
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

	public void onCreate(SQLiteDatabase db) {
		Log.d(TAG, "CREATE TABLES");
		db.execSQL("CREATE TABLE gares (_id INTEGER PRIMARY KEY, nom VARCHAR UNIQUE, region VARCHAR NOT NULL, adresse VARCHAR NOT NULL, latitude DOUBLE NOT NULL, longitude DOUBLE NOT NULL, favorite TINYINT NOT NULL DEFAULT 0)");
		db.execSQL("CREATE TABLE db_updates (categorie VARCHAR, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
	}

	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.d(TAG, "DROP TABLES");
		db.execSQL("DROP TABLE IF EXISTS gares");
		db.execSQL("DROP TABLE IF EXISTS db_updates");
		onCreate(db);
	}
}
