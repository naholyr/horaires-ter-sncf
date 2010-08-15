package com.naholyr.android.horairessncf.view;

import android.app.Activity;
import android.graphics.drawable.BitmapDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.naholyr.android.horairessncf.R;

public class QuickActionWindow extends PopupWindow {

	public static final class Action {
		private String mLabel;
		private int mIconResId;
		private View.OnClickListener mCallback;

		public Action(String label, int drawableResId, View.OnClickListener callback) {
			mLabel = label;
			mIconResId = drawableResId;
			mCallback = callback;
		}

		public View getView(Activity activity) {
			return getView(activity.getLayoutInflater());
		}

		public View getView(Activity activity, int layoutId) {
			return getView(activity.getLayoutInflater(), layoutId);
		}

		public View getView(LayoutInflater inflater) {
			return getView(inflater, R.layout.quick_action_item);
		}

		public View getView(LayoutInflater inflater, int layoutId) {
			return getView(inflater.inflate(layoutId, null));
		}

		public View getView(View convertView) {
			return getView(convertView, R.id.quick_action_icon, R.id.quick_action_label);
		}

		public View.OnClickListener getCallback() {
			return mCallback;
		}

		public View getView(final View convertView, int iconResId, int labelResId) {
			ImageView iv = (ImageView) convertView.findViewById(iconResId);
			if (iv != null) {
				iv.setImageResource(mIconResId);
			}
			TextView tv = (TextView) convertView.findViewById(labelResId);
			if (tv != null) {
				tv.setText(mLabel);
			}
			convertView.setClickable(true);
			convertView.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					mCallback.onClick(v);
				}
			});

			return convertView;
		}

	}

	private LayoutInflater mInflater;

	public static QuickActionWindow showActions(Activity activity, Action[] actions, View anchor) {
		QuickActionWindow w = getWindow(activity, actions);
		w.show(anchor);

		return w;
	}

	public static QuickActionWindow getWindow(Activity activity, Action[] actions) {
		LayoutInflater inflater = activity.getLayoutInflater();
		View contentView = inflater.inflate(R.layout.quick_action_window, null);

		return new QuickActionWindow(inflater, contentView, actions);
	}

	private QuickActionWindow(LayoutInflater inflater, View contentView, Action[] actions) {
		super(contentView, ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT, false);
		mInflater = inflater;
		for (Action action : actions) {
			addActionView(action);
		}
		// Fake background to enable key events
		setBackgroundDrawable(new BitmapDrawable());
		// Touchable and focusable
		setTouchable(true);
		setFocusable(true);
		// Click outside = close popup
		setOutsideTouchable(true);
		// Root view goes to bottom on the screen : handle click outside the
		// main view
		contentView.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dismiss();
			}
		});
		// Opening animation
		setAnimationStyle(R.style.Animation_QuickActionWindow);
	}

	public void show(View anchor) {
		showAsDropDown(anchor, 0, -30);
	}

	public Action addAction(String label, int iconResId, View.OnClickListener callback) {
		Action action = new Action(label, iconResId, callback);
		addActionView(action);

		return action;
	}

	protected void addActionView(Action action) {
		final View.OnClickListener listener = action.getCallback();
		View v = action.getView(mInflater);
		v.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				listener.onClick(v);
				dismiss();
			}
		});
		addView(v);
	}

	protected void addView(View v) {
		ViewGroup container = (ViewGroup) getContentView().findViewById(R.id.quick_actions);
		container.addView(v);
	}

}
