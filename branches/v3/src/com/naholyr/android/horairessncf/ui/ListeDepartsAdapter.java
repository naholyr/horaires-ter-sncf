package com.naholyr.android.horairessncf.ui;

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.naholyr.android.horairessncf.Depart;
import com.naholyr.android.horairessncf.R;

public class ListeDepartsAdapter extends SimpleCursorAdapter {

	private static final int LAYOUT = R.layout.depart_item;
	private static final String[] FROM = new String[] { Depart.TYPE, Depart.NUMERO, Depart.DESTINATION, Depart.HEURE_DEPART, Depart.QUAI, Depart.RETARD, Depart.MOTIF_RETARD };
	private static final int[] TO = new int[] { R.id.type_text, R.id.numero, R.id.destination, R.id.heure, R.id.quai, R.id.retard, R.id.motif };

	private static Animation mDelayAnim = null;

	public ListeDepartsAdapter(Context context, Cursor c) {
		super(context, LAYOUT, c, FROM, TO);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		// Basic replacements
		View v = super.getView(position, convertView, parent);

		// Alternate background colors
		v.findViewById(R.id.depart_item).setBackgroundResource(position % 2 == 1 ? R.color.depart_2 : R.color.depart_1);

		// Delay information
		TextView delay = (TextView) v.findViewById(R.id.retard);
		boolean hasDelay = delay != null && !TextUtils.isEmpty(delay.getText());
		final View delayLayout = v.findViewById(R.id.layout_retard);
		final View delayText = v.findViewById(R.id.motif);
		// No delay : hide layout...
		if (!hasDelay) {
			delayLayout.setVisibility(View.GONE);
			delayText.setVisibility(View.GONE);
		}
		// ...else show it with an animation
		else {
			delayLayout.setVisibility(View.VISIBLE);
			delayText.setVisibility(View.VISIBLE);
			if (mDelayAnim == null) {
				mDelayAnim = AnimationUtils.loadAnimation(v.getContext(), android.R.anim.slide_in_left);
				mDelayAnim.setStartOffset(1000);
				mDelayAnim.setDuration(500);
			}
			delayLayout.startAnimation(mDelayAnim);
			delayText.startAnimation(mDelayAnim);
		}

		// Train type : text to image (if possible)
		TextView typeText = (TextView) v.findViewById(R.id.type_text);
		ImageView typeImage = (ImageView) v.findViewById(R.id.type_image);
		if (typeText != null && typeImage != null) {
			String type = typeText.getText().toString().toLowerCase().replace(' ', '_');
			int id = v.getContext().getResources().getIdentifier("com.naholyr.android.horairessncf:drawable/type_train_" + type, null, null);
			if (id != 0) {
				typeImage.setImageResource(id);
				typeImage.setVisibility(View.VISIBLE);
				typeText.setVisibility(View.GONE);
			} else {
				typeImage.setVisibility(View.GONE);
				typeText.setVisibility(View.VISIBLE);
			}
		}

		// No information for quai : hide element
		TextView quai = (TextView) v.findViewById(R.id.quai);
		if (quai != null) {
			quai.setVisibility(TextUtils.isEmpty(quai.getText()) ? View.GONE : View.VISIBLE);
		}

		return v;
	}

}
