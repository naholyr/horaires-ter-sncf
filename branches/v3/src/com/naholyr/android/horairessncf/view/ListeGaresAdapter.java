package com.naholyr.android.horairessncf.view;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.naholyr.android.horairessncf.Gare;
import com.naholyr.android.horairessncf.R;
import com.naholyr.android.horairessncf.Gare.Favorites;

public class ListeGaresAdapter extends SimpleCursorAdapter {

	private static final NumberFormat distanceFormat = new DecimalFormat("0.00");

	private static final int LAYOUT = R.layout.gare_item;
	private static final String[] FROM = new String[] { Gare.NOM, Gare.ADRESSE };
	private static final int[] TO = new int[] { R.id.nom, R.id.adresse };

	private Context mContext;

	private Double mCenterLatitude, mCenterLongitude;

	public ListeGaresAdapter(Context context, Cursor c) {
		super(context, LAYOUT, c, FROM, TO);
		mContext = context;
		mCenterLatitude = null;
		mCenterLongitude = null;
	}

	public ListeGaresAdapter(Context context, Cursor c, double latitude, double longitude) {
		super(context, LAYOUT, c, FROM, TO);
		mContext = context;
		mCenterLatitude = latitude;
		mCenterLongitude = longitude;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// Basic replacements
		View v = super.getView(position, convertView, parent);

		try {
			Cursor c = getCursor();
			long id = getItemId(position);

			// Distance
			if (mCenterLatitude != null && mCenterLongitude != null) {
				double latitude = c.getDouble(c.getColumnIndex(Gare.LATITUDE));
				double longitude = c.getDouble(c.getColumnIndex(Gare.LONGITUDE));
				String txt = distanceFormat.format(getDistance(latitude, longitude)) + " km";
				((TextView) v.findViewById(R.id.distance)).setText(txt);
				((TextView) v.findViewById(R.id.distance)).setVisibility(View.VISIBLE);
			} else {
				((TextView) v.findViewById(R.id.distance)).setVisibility(View.GONE);
			}

			// Favori icon
			((ImageView) v.findViewById(R.id.favicon)).setImageDrawable(OnFavoriClickListener.getIcon(mContext, id));
			((ImageView) v.findViewById(R.id.favicon)).setOnClickListener(new OnFavoriClickListener(mContext, id));
		} catch (CursorIndexOutOfBoundsException e) {
			e.printStackTrace();
		}

		return v;
	}

	private double getDistance(double latitude, double longitude) {
		return 13;
	}

	private static final class OnFavoriClickListener implements View.OnClickListener {

		private static Favorites mFavorites;

		private Context mContext;
		private long mId;

		private static Favorites getFavorites(Context context) {
			if (mFavorites == null) {
				mFavorites = Gare.getFavorites(context);
			}
			return mFavorites;
		}

		public OnFavoriClickListener(Context context, long id) {
			mContext = context;
			mId = id;
		}

		public static Drawable getIcon(Context context, long id) {
			String nom = Gare.getNom(context, id);

			return getIcon(context, nom);
		}

		public static Drawable getIcon(Context context, String nom) {
			int favoriIcon = getFavorites(context).has(nom) ? R.drawable.star_on : R.drawable.star_off;
			Drawable icon = context.getResources().getDrawable(favoriIcon);

			return icon;
		}

		public void onClick(View v) {
			Favorites favs = getFavorites(mContext);
			String nom = Gare.getNom(mContext, mId);
			if (favs.has(nom)) {
				favs.remove(nom);
			} else {
				favs.add(nom);
			}
			((ImageView) v).setImageDrawable(getIcon(mContext, nom));
		}

	}

}
