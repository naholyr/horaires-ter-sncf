package com.naholyr.android.horairessncf.ui;

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
import com.naholyr.android.horairessncf.Gare.Favorites;
import com.naholyr.android.horairessncf.R;

public class ListeGaresAdapter extends SimpleCursorAdapter {

	private static final NumberFormat distanceFormat = new DecimalFormat("0.00");

	private static final int LAYOUT = R.layout.gare_item;
	private static final String[] FROM = new String[] { Gare.NOM, Gare.ADRESSE };
	private static final int[] TO = new int[] { R.id.nom, R.id.adresse };

	public static final double EARTH_RADIUS_KM = 6365d;

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
				double latitude = c.getDouble(c.getColumnIndexOrThrow(Gare.LATITUDE));
				double longitude = c.getDouble(c.getColumnIndexOrThrow(Gare.LONGITUDE));
				double distance = getDistance(latitude, longitude);
				if (distance != -1) {
					String txt = distanceFormat.format(distance) + " km";
					((TextView) v.findViewById(R.id.distance)).setText(txt);
					((TextView) v.findViewById(R.id.distance)).setVisibility(View.VISIBLE);
				}
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
		if (mCenterLatitude == null || mCenterLongitude == null) {
			return -1;
		}
		return EARTH_RADIUS_KM
				* 2
				* Math.asin(Math.sqrt(Math.pow(Math.sin((latitude - mCenterLatitude) * Math.PI / 180 / 2), 2) + Math.cos(latitude * Math.PI / 180)
						* Math.cos(mCenterLatitude * Math.PI / 180) * Math.pow(Math.sin((longitude - mCenterLongitude) * Math.PI / 180 / 2), 2)));
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
			int favoriIcon = getFavorites(context).has(id) ? R.drawable.star_on : R.drawable.star_off;

			return context.getResources().getDrawable(favoriIcon);
		}

		@Override
		public void onClick(View v) {
			Favorites favs = getFavorites(mContext);
			if (favs.has(mId)) {
				favs.remove(mId);
			} else {
				favs.add(mId);
			}
			((ImageView) v).setImageDrawable(getIcon(mContext, mId));
		}

	}

}
