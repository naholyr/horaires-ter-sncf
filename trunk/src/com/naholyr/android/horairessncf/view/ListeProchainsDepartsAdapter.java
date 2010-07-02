package com.naholyr.android.horairessncf.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleAdapter;

import com.naholyr.android.horairessncf.ProchainTrain;
import com.naholyr.android.horairessncf.R;
import com.naholyr.android.horairessncf.Util;

public class ListeProchainsDepartsAdapter extends SimpleAdapter {

	private static final int LAYOUT = R.layout.depart_item;
	private static final String[] FROM = new String[] { "destination", "heure", "type", "numero", "retard_duree", "retard_motif", "voie" };
	private static final int[] TO = new int[] { R.id.TrainItemDestination, R.id.TrainItemHeure, R.id.TrainItemType, R.id.TrainItemNumero, R.id.TrainItemRetardDuree, R.id.TrainItemRetardMotif,
			R.id.TrainItemVoie };

	public ListeProchainsDepartsAdapter(Context context, List<ProchainTrain.Depart> trains) {
		super(context, getData(trains), LAYOUT, FROM, TO);
	}

	private static List<Map<String, Object>> getData(List<ProchainTrain.Depart> trains) {
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		for (ProchainTrain.Depart train : trains) {
			Map<String, Object> row = new HashMap<String, Object>();
			row.put("train", train);
			row.put("destination", train.getDestination());
			row.put("heure", train.getHeure());
			row.put("numero", train.getNumero());
			row.put("type", train.getTypeLabel());
			row.put("voie", train.getVoie());
			row.put("supprime", train.isSupprime());
			String durees = "";
			String motifs = "";
			for (ProchainTrain.Retard retard : train.getRetards()) {
				if (!durees.equals("")) {
					durees += ", ";
				}
				durees += retard.getDuree();
				if (!motifs.equals("")) {
					motifs += ", ";
				}
				motifs += retard.getMotif();
			}
			row.put("retard_duree", durees);
			row.put("retard_motif", motifs);
			result.add(row);
		}
		return result;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		@SuppressWarnings("unchecked")
		Map<String, Object> item = (Map<String, Object>) getItem(position);
		// ProchainTrain.Depart train = (ProchainTrain.Depart)
		// item.get("train");

		// Basic replacements
		View v = super.getView(position, convertView, parent);
		v.setBackgroundColor(position % 2 == 1 ? Util.BACKGROUND_1 : Util.BACKGROUND_2);

		// Retard
		if (!item.containsKey("retard_duree") || item.get("retard_duree") == null || item.get("retard_duree").equals("")) {
			v.findViewById(R.id.TrainItemRetard).setVisibility(View.GONE);
		} else {
			v.findViewById(R.id.TrainItemRetard).setVisibility(View.VISIBLE);
		}

		// Supprimé ?
		if (item.containsKey("supprime") && (Boolean) item.get("supprime")) {
			v.findViewById(R.id.TrainItemSupprime).setVisibility(View.VISIBLE);
		} else {
			v.findViewById(R.id.TrainItemSupprime).setVisibility(View.GONE);
		}

		// Voie
		if (item.containsKey("voie") && item.get("voie") != null && !item.get("voie").equals("")) {
			v.findViewById(R.id.TrainItemVoie).setVisibility(View.VISIBLE);
		} else {
			v.findViewById(R.id.TrainItemVoie).setVisibility(View.GONE);
		}

		return v;
	}

}
