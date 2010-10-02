package com.naholyr.android.horairessncf.ui;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;

import com.naholyr.android.horairessncf.Arret;
import com.naholyr.android.horairessncf.R;

public class ListeArretsAdapter extends SimpleCursorAdapter {

	private static final int LAYOUT = R.layout.arret_item;
	private static final String[] FROM = new String[] { Arret.NOM_GARE, Arret.HEURE };
	private static final int[] TO = new int[] { R.id.nom, R.id.heure };

	public ListeArretsAdapter(Context context, Cursor c) {
		super(context, LAYOUT, c, FROM, TO);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// Basic replacements
		View v = super.getView(position, convertView, parent);

		// Alternate background colors
		v.findViewById(R.id.arret_item).setBackgroundResource(position % 2 == 1 ? R.color.depart_2 : R.color.depart_1);

		return v;
	}

}
