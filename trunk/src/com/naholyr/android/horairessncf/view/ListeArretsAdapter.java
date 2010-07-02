package com.naholyr.android.horairessncf.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleAdapter;

import com.naholyr.android.horairessncf.R;
import com.naholyr.android.horairessncf.activity.ProchainsDepartsActivity;

public class ListeArretsAdapter extends SimpleAdapter {

	private static final int LAYOUT = R.layout.arret_item;
	private static final String[] FROM = new String[] { "gare", "heure" };
	private static final int[] TO = new int[] { R.id.ArretItemGare, R.id.ArretItemHeure };

	public ListeArretsAdapter(Context context, String[] gares, Map<String, String> heures) {
		super(context, getData(gares, heures), LAYOUT, FROM, TO);
	}

	private static List<Map<String, Object>> getData(String[] gares, Map<String, String> heures) {
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		for (String gare : gares) {
			String heure = "";
			if (heures.containsKey(gare)) {
				heure = heures.get(gare);
			}
			Map<String, Object> row = new HashMap<String, Object>();
			row.put("gare", gare);
			row.put("heure", heure);
			result.add(row);
		}
		return result;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		@SuppressWarnings("unchecked")
		Map<String, Object> item = (Map<String, Object>) getItem(position);

		// Basic replacements
		View v = super.getView(position, convertView, parent);

		// Heure
		if (item.containsKey("heure") && !item.get("heure").equals("")) {
			v.findViewById(R.id.ArretItemLayoutHeure).setVisibility(View.VISIBLE);
		} else {
			v.findViewById(R.id.ArretItemLayoutHeure).setVisibility(View.GONE);
		}

		// Click = prochains départs
		final String gare = (String) item.get("gare");
		v.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent(v.getContext(), ProchainsDepartsActivity.class);
				intent.putExtra(ProchainsDepartsActivity.EXTRA_NOM_GARE, gare);
				intent.putExtra(ProchainsDepartsActivity.EXTRA_CALLED_FROM_MAIN_ACTIVITY, false);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				v.getContext().startActivity(intent);
			}
		});

		return v;
	}

}
