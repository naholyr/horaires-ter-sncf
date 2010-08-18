package com.naholyr.android.horairessncf.view;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
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
		private Drawable mIcon;
		private View.OnClickListener mCallback;

		public Action(Context context, String label, int iconResId, View.OnClickListener callback) {
			mLabel = label;
			mIcon = context.getResources().getDrawable(iconResId);
			mCallback = callback;
		}

		public Action(String label, Drawable icon, View.OnClickListener callback) {
			mLabel = label;
			mIcon = icon;
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
				iv.setImageDrawable(mIcon);
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

	public static QuickActionWindow getWindow(Activity activity) {
		return getWindow(activity, new Action[] {});
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
		super.showAtLocation(anchor, Gravity.NO_GRAVITY, 0, 0);

		// http://github.com/ruqqq/WorldHeritageSite/blob/master/src/sg/ruqqq/WHSFinder/QuickActionWindow.java
		// ((Activity)
		// anchor.getContext()).getWindowManager().getDefaultDisplay().getHeight();
		if (isShowing()) {
			int yoff;
			final View contentView = getContentView();
			contentView.measure(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			final int blockHeight = contentView.getMeasuredHeight();
			final int[] anchorLocation = new int[2];
			if (anchorLocation[1] > blockHeight) {
				// showArrow(R.id.arrow_down, requestedX);
				yoff = -anchor.getMeasuredHeight() - blockHeight + 30;
				// windowAnimations = R.style.QuickActionAboveAnimation;

			} else {
				// showArrow(R.id.arrow_up, requestedX);
				yoff = -30;
				// windowAnimations = R.style.QuickActionBelowAnimation;
			}
			// setAnimationStyle(windowAnimations);
			// mTrack.startAnimation(mTrackAnim);
			this.update(anchor, 0, yoff, -1, blockHeight);
		}
	}

	public Action addAction(String label, Drawable icon, View.OnClickListener callback) {
		Action action = new Action(label, icon, callback);
		addActionView(action);

		return action;
	}

	public Action addAction(Context context, String label, int iconResId, View.OnClickListener callback) {
		return addAction(label, context.getResources().getDrawable(iconResId), callback);
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

	public void addActionsForIntent(Context context, Intent queryIntent) {
		PackageManager pm = context.getPackageManager();
		List<ResolveInfo> results = pm.queryIntentActivities(queryIntent,
				PackageManager.GET_RESOLVED_FILTER);
		for (ResolveInfo result : results) {
			String label = result.loadLabel(pm).toString();
			Drawable icon = result.loadIcon(pm);
			final Intent intent = new Intent(queryIntent);
			intent.setClassName(result.activityInfo.packageName, result.activityInfo.name);
			addActionForIntent(context, label, icon, intent);
		}		
	}

	public void addActionForIntent(final Context context, String label, Drawable icon, final Intent intent) {
		addAction(label, icon, new View.OnClickListener() {
			public void onClick(View v) {
				context.startActivity(intent);
			}
		});
	}

}
