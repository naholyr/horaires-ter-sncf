package com.naholyr.android.horairessncf.providers;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class DatabaseHelper extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 2;
	private static final String DATABASE_NAME = "gares.db";
	
	public static final String TABLE_GARES = "gares";

	DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public void onCreate(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE gares (_id INTEGER PRIMARY KEY, nom VARCHAR UNIQUE, region VARCHAR NOT NULL, adresse VARCHAR NOT NULL, latitude DOUBLE NOT NULL, longitude DOUBLE NOT NULL)");
		db.execSQL("CREATE TABLE db_updates (categorie VARCHAR, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP, hash VARCHAR UNIQUE)");
		db.execSQL("CREATE TABLE gares_favorites (nom VARCHAR PRIMARY KEY)");
	}

	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS gares");
		db.execSQL("DROP TABLE IF EXISTS db_updates");
		db.execSQL("DROP TABLE IF EXISTS gares_favorites");
		onCreate(db);
	}
}
