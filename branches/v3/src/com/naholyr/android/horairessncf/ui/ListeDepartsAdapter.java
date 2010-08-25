package com.naholyr.android.horairessncf.ui;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.naholyr.android.horairessncf.Depart;
import com.naholyr.android.horairessncf.R;

public class ListeDepartsAdapter extends SimpleCursorAdapter {

	private static final int LAYOUT = R.layout.depart_item;
	private static final String[] FROM = new String[] { Depart.DESTINATION, Depart.HEURE_DEPART, Depart.QUAI, Depart.RETARD, Depart.MOTIF_RETARD };
	private static final int[] TO = new int[] { R.id.destination, R.id.heure, R.id.quai, R.id.retard, R.id.motif };

	public ListeDepartsAdapter(Context context, Cursor c) {
		super(context, LAYOUT, c, FROM, TO);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// Basic replacements
		View v = super.getView(position, convertView, parent);

		// No delay : hide layout
		TextView delay = (TextView) v.findViewById(R.id.retard);
		if (TextUtils.isEmpty(delay.getText())) {
			v.findViewById(R.id.layout_retard).setVisibility(View.GONE);
		} else {
			v.findViewById(R.id.layout_retard).setVisibility(View.VISIBLE);
		}

		// No information for quai : hide element
		TextView quai = (TextView) v.findViewById(R.id.quai);
		if (TextUtils.isEmpty(quai.getText())) {
			quai.setVisibility(View.GONE);
		} else {
			quai.setVisibility(View.VISIBLE);
		}

		return v;
	}

}
