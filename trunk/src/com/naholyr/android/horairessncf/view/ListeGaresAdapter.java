package com.naholyr.android.horairessncf.view;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.naholyr.android.horairessncf.Gare;
import com.naholyr.android.horairessncf.R;
import com.naholyr.android.horairessncf.Util;

public class ListeGaresAdapter extends SimpleAdapter {

	private static final NumberFormat distanceFormat = new DecimalFormat("0.00");

	private static final int LAYOUT = R.layout.gare_item;
	private static final String[] FROM = new String[] { "nom", "adresse" };
	private static final int[] TO = new int[] { R.id.GareItemNom, R.id.GareItemAdresse };

	private Context context;

	public ListeGaresAdapter(Context context, List<Gare> gares) {
		super(context, getData(gares), LAYOUT, FROM, TO);
		this.context = context;
	}

	public ListeGaresAdapter(Context context, List<Gare> gares, double latitude, double longitude) {
		super(context, getData(gares, latitude, longitude), LAYOUT, FROM, TO);
		this.context = context;
	}

	private static List<Map<String, Object>> getData(List<Gare> gares, Double latitude, Double longitude) {
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		for (Gare gare : gares) {
			Map<String, Object> row = new HashMap<String, Object>();
			row.put("gare", gare);
			row.put("nom", gare.getNom());
			row.put("adresse", gare.getAdresse());
			if (latitude != null && longitude != null) {
				row.put("distance", gare.getDistance(latitude, longitude));
			}
			result.add(row);
		}
		return result;
	}

	private static List<Map<String, Object>> getData(List<Gare> gares) {
		return getData(gares, null, null);
	}

	@SuppressWarnings("unchecked")
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		Map<String, Object> item = (Map<String, Object>) getItem(position);
		Gare gare = (Gare) item.get("gare");

		// Basic replacements
		View v = super.getView(position, convertView, parent);
		v.setBackgroundColor(position % 2 == 1 ? Util.BACKGROUND_1 : Util.BACKGROUND_2);

		// Distance
		if (item.containsKey("distance")) {
			String txt = distanceFormat.format((Double) item.get("distance")) + " km";
			((TextView) v.findViewById(R.id.GareItemDistance)).setText(txt);
		} else {
			((TextView) v.findViewById(R.id.GareItemDistance)).setText("");
		}

		// Favori icon
		((ImageView) v.findViewById(R.id.GareItemFavoriIcon)).setImageDrawable(OnFavoriClickListener.getIcon(context, gare));
		((ImageView) v.findViewById(R.id.GareItemFavoriIcon)).setOnClickListener(new OnFavoriClickListener(context, gare));

		return v;
	}

	private static final class OnFavoriClickListener implements View.OnClickListener {

		private Context context;
		private Gare gare;

		public static Drawable getIcon(Context context, Gare gare) {
			int favoriIcon = gare.isFavori() ? android.R.drawable.star_big_on : android.R.drawable.star_big_off;
			Drawable icon = context.getResources().getDrawable(favoriIcon);

			return icon;
		}

		public OnFavoriClickListener(Context context, Gare gare) {
			this.context = context;
			this.gare = gare;
		}

		@Override
		public void onClick(View v) {
			gare.setFavori(!gare.isFavori());
			((ImageView) v).setImageDrawable(getIcon(context, gare));
		}

	}

}
