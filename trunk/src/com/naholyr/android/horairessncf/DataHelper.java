package com.naholyr.android.horairessncf;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.provider.LiveFolders;
import android.util.Log;

public class DataHelper {

	private static final int DATABASE_VERSION = 14;

	private static final String DATABASE_NAME = "gares.db";
	private static final String TABLE_NAME = "gares";
	private static final String[] SELECT_COLUMNS = new String[] { "nom", "region", "adresse", "latitude", "longitude" };
	private static final String SELECT_WHERE_NOM = "nom LIKE ?";
	private static final String SELECT_WHERE_ROWID = "_ROWID_ = ";
	private static final String SELECT_WHERE_BOX = "latitude between ? and ? and longitude between ? and ?";
	private static final String[] SELECT_ALL_COLUMNS = new String[] { "nom" };
	private static final String SELECT_ALL_ORDER = "region ASC, nom ASC";
	private static final String SQL_DROP = "DROP TABLE IF EXISTS gares";
	private static final String SQL_CREATE = "CREATE TABLE gares (nom VARCHAR PRIMARY KEY, region VARCHAR NOT NULL, adresse VARCHAR NOT NULL, latitude DOUBLE NOT NULL, longitude DOUBLE NOT NULL)";
	private static final String SQL_DROP2 = "DROP TABLE IF EXISTS db_updates";
	private static final String SQL_CREATE2 = "CREATE TABLE db_updates (categorie VARCHAR, updated_at DATETIME DEFAULT CURRENT_TIMESTAMP, hash VARCHAR)";

	private static SQLiteDatabase db;

	private static DataHelper instance = null;

	/**
	 * Initializes a read-only database access.
	 * 
	 * @param context
	 * @throws IOException
	 */
	public DataHelper(Context context) throws IOException {
		OpenHelper openHelper = new OpenHelper(context);
		db = openHelper.getWritableDatabase();
	}

	public static DataHelper getInstance(Context context) throws IOException {
		if (instance == null) {
			instance = new DataHelper(context);
		}

		return instance;
	}

	private static class OpenHelper extends SQLiteOpenHelper {

		OpenHelper(Context context) throws IOException {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		public void onCreate(SQLiteDatabase db) {
			db.execSQL(SQL_DROP);
			db.execSQL(SQL_CREATE);
			db.execSQL(SQL_DROP2);
			db.execSQL(SQL_CREATE2);
		}

		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			onCreate(db);
		}
	}

	public long insert(String nom, String region, String adresse, Double latitude, Double longitude) {
		ContentValues values = new ContentValues(5);
		values.put("nom", nom);
		values.put("region", region);
		values.put("adresse", adresse);
		values.put("latitude", latitude);
		values.put("longitude", longitude);

		return db.insert(TABLE_NAME, null, values);
	}

	public int update(String nom, String region, String adresse, Double latitude, Double longitude) {
		ContentValues values = new ContentValues(4);
		values.put("region", region);
		values.put("adresse", adresse);
		values.put("latitude", latitude);
		values.put("longitude", longitude);

		return db.update(TABLE_NAME, values, SELECT_WHERE_NOM, new String[] { nom });
	}

	private Map<String, Object> fromCursor(Cursor cursor) throws NumberFormatException, ArrayIndexOutOfBoundsException {
		Map<String, Object> result = null;
		result = new HashMap<String, Object>();
		result.put("nom", cursor.getString(0));
		result.put("region", cursor.getString(1));
		result.put("adresse", cursor.getString(2));
		result.put("latitude", cursor.getDouble(3));
		result.put("longitude", cursor.getDouble(4));

		return result;
	}

	public Map<String, Object> selectOne(int rowid) {
		Cursor cursor = db.query(TABLE_NAME, SELECT_COLUMNS, SELECT_WHERE_ROWID + String.valueOf(rowid), null, null, null, null);

		Map<String, Object> result = null;

		if (cursor.moveToFirst()) {
			try {
				result = fromCursor(cursor);
			} catch (NumberFormatException e) {
				Log.e(getClass().getName(), "Erreur format integer", e);
			} catch (ArrayIndexOutOfBoundsException e) {
				Log.e(getClass().getName(), "Erreur tableau", e);
			} catch (NullPointerException e) {
				Log.e(getClass().getName(), "cursor.getString(6) == NULL", e);
			}
		}
		cursor.close();

		return result;
	}

	public Map<String, Object> selectOne(String nom) {
		Cursor cursor = db.query(TABLE_NAME, SELECT_COLUMNS, SELECT_WHERE_NOM, new String[] { nom }, null, null, null);

		Map<String, Object> result = null;

		if (cursor.moveToFirst()) {
			try {
				result = fromCursor(cursor);
			} catch (NumberFormatException e) {
				Log.e(getClass().getName(), "Erreur format integer", e);
			} catch (ArrayIndexOutOfBoundsException e) {
				Log.e(getClass().getName(), "Erreur tableau", e);
			} catch (NullPointerException e) {
				Log.e(getClass().getName(), "cursor.getString(6) == NULL", e);
			}
		}
		cursor.close();

		return result;
	}

	public List<String> selectInBox(double latitude_min, double latitude_max, double longitude_min, double longitude_max) {
		Cursor cursor = db.query(TABLE_NAME, SELECT_ALL_COLUMNS, SELECT_WHERE_BOX, new String[] { String.valueOf(latitude_min), String.valueOf(latitude_max),
				String.valueOf(longitude_min), String.valueOf(longitude_max) }, null, null, null);

		List<String> result = new ArrayList<String>();

		if (cursor.moveToFirst()) {
			do {
				result.add(cursor.getString(0));
			} while (cursor.moveToNext());
		}
		cursor.close();

		return result;
	}

	public List<String> selectAll() {
		List<String> codes = new ArrayList<String>();
		Cursor cursor = db.query(TABLE_NAME, SELECT_ALL_COLUMNS, "", null, null, null, SELECT_ALL_ORDER);
		if (cursor.moveToFirst()) {
			do {
				codes.add(cursor.getString(0));
			} while (cursor.moveToNext());
		}
		cursor.close();

		return codes;
	}

	public void insertOrUpdate(String nom, String region, String adresse, Double latitude, Double longitude) {
		if (selectOne(nom) != null) {
			// Update
			update(nom, region, adresse, latitude, longitude);
		} else {
			// Insert
			insert(nom, region, adresse, latitude, longitude);
		}
	}

	/**
	 * 
	 * @param words
	 * @return
	 */
	public List<String> selectByKeywords(String[] words) {
		String select = "";
		for (int i = 0; i < words.length; i++) {
			String word = words[i].replace("'", "").trim();
			if (word.length() > 2) {
				if (!select.equals("")) {
					select += " AND ";
				}
				select += "nom LIKE '%" + word + "%'";
			}
		}

		List<String> noms = new ArrayList<String>();
		Cursor cursor = db.query(TABLE_NAME, SELECT_ALL_COLUMNS, select, new String[0], null, null, null);
		if (cursor.moveToFirst()) {
			do {
				noms.add(cursor.getString(0));
			} while (cursor.moveToNext());
		}
		cursor.close();

		return noms;
	}

	private static final HashMap<String, String> LIVE_FOLDER_PROJECTION_MAP;
	static {
		LIVE_FOLDER_PROJECTION_MAP = new HashMap<String, String>();
		LIVE_FOLDER_PROJECTION_MAP.put(LiveFolders._ID, "_ROWID_ AS " + LiveFolders._ID);
		LIVE_FOLDER_PROJECTION_MAP.put(LiveFolders.NAME, "nom AS " + LiveFolders.NAME);
		LIVE_FOLDER_PROJECTION_MAP.put(LiveFolders.DESCRIPTION, "adresse AS " + LiveFolders.DESCRIPTION);
	}

	public Cursor selectForLiveFolder(String[] noms, String orderBy) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		qb.setTables(TABLE_NAME);
		qb.setProjectionMap(LIVE_FOLDER_PROJECTION_MAP);

		String selection = "";
		for (int i = 0; i < noms.length; i++) {
			if (i > 0) {
				selection += " OR ";
			}
			selection += "nom = ?";
		}
		String[] projection = null;
		return qb.query(db, projection, selection, noms, null, null, orderBy);
	}

	public long countGares() {
		try {
			return db.compileStatement("SELECT COUNT(*) FROM " + TABLE_NAME).simpleQueryForLong();
		} catch (SQLiteException e) {
			return 0;
		}
	}

	public String getLastUpdateHash() {
		try {
			return db.compileStatement("SELECT hash FROM db_updates ORDER BY updated_at DESC LIMIT 1").simpleQueryForString();
		} catch (SQLiteException e) {
			return null;
		}
	}

	public long getLastUpdateTime() {
		String dateTime;
		try {
			dateTime = db.compileStatement("SELECT MAX(updated_at) FROM db_updates").simpleQueryForString();
		} catch (SQLiteException e) {
			dateTime = null;
		}

		if (dateTime == null) {
			return 0;
		}

		DateFormat iso8601Format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		try {
			return iso8601Format.parse(dateTime).getTime();
		} catch (ParseException e) {
			return 0;
		}
	}

	public long saveNewUpdateHash(String hash) {
		ContentValues values = new ContentValues(2);
		values.put("categorie", "gares");
		values.put("hash", hash);

		return db.insert("db_updates", null, values);
	}

	public void truncate() {
		db.execSQL("DELETE FROM " + TABLE_NAME);
	}

	public SQLiteDatabase getDb() {
		return db;
	}

}
