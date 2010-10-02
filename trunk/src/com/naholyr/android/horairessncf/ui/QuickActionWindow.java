package com.naholyr.android.horairessncf.ui;

import java.util.ArrayList;
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
import android.os.Bundle;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.naholyr.android.horairessncf.R;

public class QuickActionWindow extends PopupWindow {

	public static interface WindowInitializer {
		public void setItems(QuickActionWindow window);
	}

	public static class Item {

		String mLabel;
		Drawable mIcon;
		View.OnClickListener mCallback;
		View mView = null;

		public Item(Context context, String label, int iconResId, View.OnClickListener callback) {
			mLabel = label;
			mIcon = context.getResources().getDrawable(iconResId);
			mCallback = callback;
		}

		public Item(String label, Drawable icon, View.OnClickListener callback) {
			mLabel = label;
			mIcon = icon;
			mCallback = callback;
		}

		public View getView(LayoutInflater inflater, int layout, int icon, int label) {
			if (mView == null) {
				mView = inflater.inflate(layout, null);
				ImageView iv = (ImageView) mView.findViewById(icon);
				if (iv != null) {
					iv.setImageDrawable(mIcon);
				}
				TextView tv = (TextView) mView.findViewById(label);
				if (tv != null) {
					tv.setText(mLabel);
				}
				mView.setClickable(true);
				mView.setOnClickListener(getCallback());
			}

			return mView;
		}

		public View getView() {
			return mView;
		}

		public View.OnClickListener getCallback() {
			return mCallback;
		}

	}

	public static class IntentItem extends Item {

		Intent mIntent;
		Context mContext;

		public IntentItem(Context context, String label, int iconResId, final Intent intent) {
			super(context, label, iconResId, null);
			mContext = context;
			mIntent = new Intent(intent);
		}

		public IntentItem(Context context, String label, Drawable icon, final Intent intent) {
			super(label, icon, null);
			mContext = context;
			mIntent = new Intent(intent);
		}

		public Intent getIntent() {
			return mIntent;
		}

		public Context getContext() {
			return mContext;
		}

		public View.OnClickListener getCallback() {
			return new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					try {
						mContext.startActivity(mIntent);
					} catch (ActivityNotFoundException e) {
						Toast.makeText(mContext, "Erreur : Application introuvable", Toast.LENGTH_LONG).show();
						ErrorReporter.getInstance().handleSilentException(e);
					}
				}
			};
		}
	}

	public static class ItemAdvertisement {

		public String name;
		public Drawable icon;
		public String label;
		public String packageName;

		public ItemAdvertisement(String name, Drawable icon, String label, String packageName) {
			this.name = name;
			this.icon = icon;
			this.label = label;
			this.packageName = packageName;
		}

	}

	LayoutInflater mInflater;
	ArrayList<Item> mItems;
	boolean mDismissOnClick;

	static SparseArray<ArrayList<Item>> cachedItems = new SparseArray<ArrayList<Item>>();

	public static QuickActionWindow getWindow(Activity activity, int layout, WindowInitializer initializer, Integer windowID) {
		ArrayList<Item> items = null;
		if (windowID != null) {
			items = cachedItems.get(windowID, null);
		}

		QuickActionWindow w = getWindow(activity, layout, items);

		if (items == null && initializer != null) {
			items = new ArrayList<Item>();
			initializer.setItems(w);
			if (windowID != null) {
				cachedItems.put(windowID, w.getItems());
			}
		}

		return w;
	}

	public static QuickActionWindow getWindow(Activity activity, int layout, List<Item> actions) {
		LayoutInflater inflater = activity.getLayoutInflater();
		View contentView = inflater.inflate(layout, null);

		return new QuickActionWindow(inflater, contentView, actions);
	}

	public static QuickActionWindow getWindow(Activity activity, int layout) {
		return getWindow(activity, layout, null);
	}

	protected QuickActionWindow(LayoutInflater inflater, View contentView, List<Item> items) {
		super(contentView, ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT, false);
		mDismissOnClick = true;
		mItems = new ArrayList<Item>();
		mInflater = inflater;
		if (items != null) {
			for (Item item : items) {
				addItem(item);
			}
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

	public void setDismissOnClick(boolean enabled) {
		mDismissOnClick = enabled;
	}

	public void show(View anchor) {
		show(anchor, 0, 0);
	}

	public void show(View anchor, int backgroundIfAbove, int additionalOffset) {
		showAtLocation(anchor, Gravity.NO_GRAVITY, 0, 0);

		// http://github.com/ruqqq/WorldHeritageSite/blob/master/src/sg/ruqqq/WHSFinder/QuickActionWindow.java
		if (isShowing()) {
			int yoff;
			final ViewGroup contentView = (ViewGroup) getContentView();
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
			update(anchor, 0, yoff, -1, blockHeight);

			// Animation for all views
			Animation anim = AnimationUtils.loadAnimation(anchor.getContext(), R.anim.quick_action_item_appear);
			for (int i = 0; i < contentView.getChildCount(); i++) {
				View v = contentView.getChildAt(i);
				v.startAnimation(anim);
			}
		}
	}

	public QuickActionWindow addItem(Item item) {
		mItems.add(item);
		addItemView(item);

		return this;
	}

	public QuickActionWindow addItem(String label, Drawable icon, View.OnClickListener callback) {
		Item action = new Item(label, icon, callback);
		addItem(action);

		return this;
	}

	public QuickActionWindow addAction(Context context, String label, int iconResId, View.OnClickListener callback) {
		return addItem(label, context.getResources().getDrawable(iconResId), callback);
	}

	protected void addItemView(Item item) {
		final View.OnClickListener listener = item.getCallback();
		View v = item.getView(mInflater, R.layout.quick_action_item, R.id.quick_action_icon, R.id.quick_action_label);
		v.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				listener.onClick(v);
				if (mDismissOnClick) {
					dismiss();
				}
			}
		});
		addView(v);
	}

	protected void addView(View v) {
		ViewGroup container = (ViewGroup) getContentView().findViewById(R.id.quick_actions);
		container.addView(v);
	}

	public void addItemsForIntent(Context context, Intent queryIntent) {
		addItemsForIntent(context, queryIntent, null);
	}

	public void addItemsForIntent(Context context, Intent queryIntent, ItemAdvertisement[] ads) {
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
			addItem(new IntentItem(context, label, icon, intent));
		}
		// Check found names, and eventually add advertisement actions
		if (ads != null) {
			for (ItemAdvertisement ad : ads) {
				boolean found = false;
				for (int i = 0; i < names.length; i++) {
					if (names[i].equals(ad.name)) {
						found = true;
						break;
					}
				}
				if (!found) {
					Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + ad.packageName));
					addItem(new IntentItem(context, ad.label, ad.icon, intent));
				}
			}
		}
	}

	public void dispatchIntentExtras(Bundle extras, Intent baseIntent) {
		for (Item action : mItems) {
			if (action instanceof IntentItem) {
				Intent intent = ((IntentItem) action).getIntent();
				Intent comparisonIntent = new Intent(intent);
				// Nullify component that may have been set : we want to compare
				// filter base on action/uri/type/etc, not component himself
				comparisonIntent.setComponent(null);
				if (baseIntent == null || comparisonIntent.filterEquals(baseIntent)) {
					// Matched intent : fill it with given extras
					intent.putExtras(extras);
				}
			}
		}
	}

	public ArrayList<Item> getItems() {
		return mItems;
	}

	@Override
	public void dismiss() {
		// Remove items views from their parent, so that if we re-use them it
		// will work fine. If we don't do that, caching items and re-use them to
		// show another action window will throw a layout exception
		for (Item item : mItems) {
			View v = item.getView();
			if (v != null) {
				((ViewGroup) v.getParent()).removeView(v);
			}
		}
		super.dismiss();
	}

}
