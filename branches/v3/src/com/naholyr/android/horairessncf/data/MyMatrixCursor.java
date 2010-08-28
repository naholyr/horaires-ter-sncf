package com.naholyr.android.horairessncf.data;

import java.util.ArrayList;

import android.database.AbstractCursor;

public class MyMatrixCursor extends AbstractCursor {

	String[] mColumnNames;

	ArrayList<Object[]> mRows;

	public MyMatrixCursor(String[] columnNames) {
		mColumnNames = columnNames;
		mRows = new ArrayList<Object[]>();
	}

	public void addRow(Object[] row) {
		mRows.add(row);
		// onChange(true);
	}

	public void clear() {
		mRows.clear();
		// onChange(true);
	}

	@Override
	public String[] getColumnNames() {
		return mColumnNames;
	}

	@Override
	public int getCount() {
		return mRows.size();
	}

	Object get(int column) {
		return mRows.get(getPosition())[column];
	}

	@Override
	public double getDouble(int column) {
		return (Double) get(column);
	}

	@Override
	public float getFloat(int column) {
		return (Float) get(column);
	}

	@Override
	public int getInt(int column) {
		return (Integer) get(column);
	}

	@Override
	public long getLong(int column) {
		return (Long) get(column);
	}

	@Override
	public short getShort(int column) {
		return (Short) get(column);
	}

	@Override
	public String getString(int column) {
		return (String) get(column);
	}

	@Override
	public boolean isNull(int column) {
		return get(column) == null;
	}

}
