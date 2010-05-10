package com.naholyr.android.horairessncf.view;

import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import com.naholyr.android.horairessncf.Gare;
import com.naholyr.android.horairessncf.R;

public class ListeGaresView extends ListView implements OnItemClickListener, OnItemLongClickListener {

	private static final String OPT_SUPPR_FAVORI = "Supprimer des favoris";
	private static final String OPT_ADD_FAVORI = "Ajouter aux favoris";
	private static final String OPT_GMAP = "Localiser sur une carte";
	private static final String OPT_DEPARTS = "Prochains départs";

	public ListeGaresView(Context context) {
		super(context);
		setBehaviors();
	}

	public ListeGaresView(Context context, AttributeSet attrs) {
		super(context, attrs);
		setBehaviors();
	}

	public ListeGaresView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		setBehaviors();
	}

	private void setBehaviors() {
		setOnItemClickListener(this);
		setOnItemLongClickListener(this);
	}

	@SuppressWarnings("unchecked")
	private Gare getGare(int position) {
		return (Gare) ((Map<String, Object>) getItemAtPosition(position)).get("gare");
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View v, int position, long arg3) {
		Gare gare = getGare(position);
		gare.showProchainsDepartsActivity();
	}

	@Override
	public boolean onItemLongClick(AdapterView<?> adapterView, final View v, int position, long arg3) {
		final Gare gare = getGare(position);

		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		final CharSequence[] items = { gare.isFavori() ? OPT_SUPPR_FAVORI : OPT_ADD_FAVORI, OPT_GMAP, OPT_DEPARTS };

		builder.setTitle(gare.getNom());
		builder.setItems(items, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int item) {
				CharSequence opt = items[item];
				if (opt.equals(OPT_SUPPR_FAVORI) || opt.equals(OPT_ADD_FAVORI)) {
					v.findViewById(R.id.GareItemFavoriIcon).performClick();
				} else if (opt.equals(OPT_GMAP)) {
					gare.showInGoogleMaps(true);
				} else if (opt.equals(OPT_DEPARTS)) {
					gare.showProchainsDepartsActivity();
				}
			}
		});
		builder.setCancelable(true);
		builder.setNegativeButton("Annuler", null);
		AlertDialog alert = builder.create();
		alert.show();

		return true;
	}

	public void setData(List<Gare> gares, Double latitude, Double longitude) {
		ListAdapter adapter;
		if (latitude == null || longitude == null) {
			adapter = new ListeGaresAdapter(getContext(), gares);
		} else {
			adapter = new ListeGaresAdapter(getContext(), gares, latitude, longitude);
		}
		((ListView) findViewById(R.id.ListeGares)).setAdapter(adapter);
	}

}
