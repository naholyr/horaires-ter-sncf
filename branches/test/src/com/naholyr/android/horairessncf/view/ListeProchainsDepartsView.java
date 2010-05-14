package com.naholyr.android.horairessncf.view;

import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import com.naholyr.android.horairessncf.ProchainTrain;
import com.naholyr.android.horairessncf.activity.DetailsTrainActivity;

public class ListeProchainsDepartsView extends ListView implements OnItemClickListener {

	public ListeProchainsDepartsView(Context context) {
		super(context);
		init(context);
	}

	public ListeProchainsDepartsView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public ListeProchainsDepartsView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}

	private void init(Context context) {
		setOnItemClickListener(this);
		// prefs_favs = Util.getPreferencesTrainsSuivis(context);
	}

	private ProchainTrain.Depart getTrain(int position) {
		return (ProchainTrain.Depart) ((Map<?, ?>) getItemAtPosition(position)).get("train");
	}

	private void showDetails(View v, int position) {
		showDetails(v, getTrain(position));
	}

	private void showDetails(final View v, final ProchainTrain train) {
		Intent intent = new Intent(getContext(), DetailsTrainActivity.class);
		intent.putExtra("numero", train.getNumero());
		getContext().startActivity(intent);
	}

	public void onItemClick(AdapterView<?> adapterView, View v, int position, long arg3) {
		showDetails(v, position);
	}

	public void setData(List<ProchainTrain.Depart> trains) {
		ListAdapter adapter = new ListeProchainsDepartsAdapter(getContext(), trains);
		setAdapter(adapter);
	}

}
