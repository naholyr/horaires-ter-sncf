package com.naholyr.android.horairessncf.view;

import java.util.List;

import org.acra.ErrorReporter;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

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
				@Override
				public void onClick(View v) {
					mCallback.onClick(v);
				}
			});

			return convertView;
		}

	}

	public static final class AdvertisementAction {

		public String name;
		public Drawable icon;
		public String label;
		public String packageName;

		public AdvertisementAction(String name, Drawable icon, String label, String packageName) {
			this.name = name;
			this.icon = icon;
			this.label = label;
			this.packageName = packageName;
		}

	}

	private LayoutInflater mInflater;

	public static QuickActionWindow showActions(Activity activity, int layout, Action[] actions, View anchor) {
		QuickActionWindow w = getWindow(activity, layout, actions);
		w.show(anchor);

		return w;
	}

	public static QuickActionWindow getWindow(Activity activity, int layout, Action[] actions) {
		LayoutInflater inflater = activity.getLayoutInflater();
		View contentView = inflater.inflate(layout, null);

		return new QuickActionWindow(inflater, contentView, actions);
	}

	public static QuickActionWindow getWindow(Activity activity, int layout) {
		return getWindow(activity, layout, new Action[] {});
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
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		// Opening animation
		setAnimationStyle(R.style.Animation_QuickActionWindow);
	}

	public void show(View anchor) {
		show(anchor, 0, 0);
	}

	public void show(View anchor, int backgroundIfAbove, int additionalOffset) {
		super.showAtLocation(anchor, Gravity.NO_GRAVITY, 0, 0);

		// http://github.com/ruqqq/WorldHeritageSite/blob/master/src/sg/ruqqq/WHSFinder/QuickActionWindow.java
		if (isShowing()) {
			int yoff;
			final View contentView = getContentView();
			contentView.measure(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			final int blockHeight = contentView.getMeasuredHeight();
			final int[] anchorLocation = new int[2];
			anchor.getLocationOnScreen(anchorLocation);
			if (anchorLocation[1] > blockHeight) {
				// Display above anchor
				yoff = -anchor.getMeasuredHeight() - blockHeight + additionalOffset;
				if (backgroundIfAbove != 0) {
					contentView.setBackgroundResource(backgroundIfAbove);
				}
			} else {
				// Display below anchor
				yoff = -additionalOffset;
			}
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
			@Override
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
		addActionsForIntent(context, queryIntent, null);
	}

	public void addActionsForIntent(Context context, Intent queryIntent, AdvertisementAction[] ads) {
		// List activities handling request intent
		PackageManager pm = context.getPackageManager();
		List<ResolveInfo> results = pm.queryIntentActivities(queryIntent, PackageManager.GET_RESOLVED_FILTER);
		String[] names = new String[results.size()];
		for (int i = 0; i < names.length; i++) {
			// Generate associated action, and store name for later use
			ResolveInfo result = results.get(i);
			names[i] = result.activityInfo.name;
			String label = result.loadLabel(pm).toString();
			Drawable icon = result.loadIcon(pm);
			final Intent intent = new Intent(queryIntent);
			intent.setClassName(result.activityInfo.packageName, result.activityInfo.name);
			addActivityAction(context, label, icon, intent);
		}
		// Check found names, and eventually add advertisement actions
		if (ads != null) {
			for (AdvertisementAction ad : ads) {
				boolean found = false;
				for (int i = 0; i < names.length; i++) {
					if (names[i].equals(ad.name)) {
						found = true;
						break;
					}
				}
				if (!found) {
					addActivityAction(context, ad.label, ad.icon, new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + ad.packageName)));
				}
			}
		}
	}

	public void addActivityAction(final Context context, String label, Drawable icon, final Intent intent) {
		addAction(label, icon, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					context.startActivity(intent);
				} catch (ActivityNotFoundException e) {
					Toast.makeText(context, "Erreur : Application introuvable", Toast.LENGTH_LONG).show();
					ErrorReporter.getInstance().handleSilentException(e);
				}
			}
		});
	}

}
