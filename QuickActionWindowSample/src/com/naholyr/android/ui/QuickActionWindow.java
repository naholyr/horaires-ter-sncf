package com.naholyr.android.ui;

import java.util.ArrayList;
import java.util.List;

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
import android.util.SparseIntArray;
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

/**
 * Dropdown floating window with action items.
 * 
 * @see http://code.google.com/p/horaires-ter-sncf/wiki/QuickActionWindow
 * 
 * @author naholyr
 */
public class QuickActionWindow extends PopupWindow {

	public static final String VERSION = "1.0";

	public static final String AUTHOR = "Nicolas Chambrier <naholyr@gmail.com>";

	/**
	 * Use an initializer when you want to initialize the items of your action
	 * window only once and they get cached for later use of "getWindow()".
	 * 
	 * @author naholyr
	 */
	public static interface Initializer {

		/**
		 * Initialize items of the window : note that after this method is
		 * called, the result of window.getItems() will be cached, and later
		 * calls of "getWindow()" with the same window ID will result in not
		 * calling "setItems()" again.
		 * 
		 * @param window
		 */
		public void setItems(QuickActionWindow window);

	}

	/**
	 * QuickActionWindow configuration class. Extend and define the attributes
	 * for your own use when you initialize a window.
	 * 
	 * @author naholyr
	 */
	public static interface Config {

		/**
		 * Mandatory : Window layout ID. Must be defined by subclass.
		 */
		public static final int WINDOW_LAYOUT = 1;

		/**
		 * Background applied to window content view (root view of the window
		 * layout) when window is above the anchor. Leave null if your default
		 * background is for above position.
		 */
		public static final int WINDOW_BACKGROUND_IF_ABOVE = 2;

		/**
		 * Background applied to window content view (root view of the window
		 * layout) when window is below the anchor. Leave null if your default
		 * background is for below position.
		 */
		public static final int WINDOW_BACKGROUND_IF_BELOW = 3;

		/**
		 * Mandatory : Quick action item layout ID.
		 */
		public static final int ITEM_LAYOUT = 4;

		/**
		 * Quick action window animation style (for opening and closing
		 * animations). Leave null to disable.
		 */
		public static final int WINDOW_ANIMATION_STYLE = 5;

		/**
		 * Animation played on each item layout when window is open. Leave null
		 * to disable.
		 */
		public static final int ITEM_APPEAR_ANIMATION = 6;

		/**
		 * Subview of the content view which will hold all items. Leave null to
		 * use the root view directly.
		 */
		public static final int CONTAINER = 7;

		/**
		 * ImageView for the item icon. Leave null if your items won't show
		 * icons.
		 */
		public static final int ITEM_ICON = 8;

		/**
		 * TextView for the item label. Leave null if your items won't show
		 * label.
		 */
		public static final int ITEM_LABEL = 9;

		/**
		 * Vertical offset applied to the window, so that the arrow will overlap
		 * a little the anchor. Best value should be between 50% and 75% of the
		 * height of the arrow in your window background.
		 */
		public static final int ARROW_OFFSET = 10;

	}

	/**
	 * An action item
	 * 
	 * @author naholyr
	 */
	public static class Item {

		/**
		 * Callback used when use clicks on an item.
		 * 
		 * @author naholyr
		 */
		public static interface Callback {

			/**
			 * @param item
			 *            The selected item.
			 */
			public void onClick(Item item, View anchor);

		}

		String mLabel = null;

		Drawable mIcon = null;

		Callback mCallback;

		View mView = null;

		/**
		 * @param label
		 *            Label (can be null if your window configuration didn't set
		 *            itemLabel).
		 * @param icon
		 *            Icon (can be null if your window configuration didn't set
		 *            itemIcon).
		 * @param callback
		 *            Handler of user clicks.
		 */
		public Item(String label, Drawable icon, Callback callback) {
			mLabel = label;
			mIcon = icon;
			mCallback = callback;
		}

		/**
		 * @see {@link Item#Item(String, Drawable, Callback)}
		 * @param context
		 * @param labelResId
		 * @param iconResId
		 * @param callback
		 */
		public Item(Context context, Integer labelResId, Integer iconResId, Callback callback) {
			this(labelResId != null ? context.getString(labelResId) : null, iconResId != null ? context.getResources().getDrawable(iconResId) : null, callback);
		}

		/**
		 * @see {@link Item#Item(String, Drawable, Callback)}
		 * @param context
		 * @param label
		 * @param iconResId
		 * @param callback
		 */
		public Item(Context context, String label, Integer iconResId, Callback callback) {
			this(label, iconResId != null ? context.getResources().getDrawable(iconResId) : null, callback);
		}

		/**
		 * @see {@link Item#Item(String, Drawable, Callback)}
		 * @param context
		 * @param labelResId
		 * @param icon
		 * @param callback
		 */
		public Item(Context context, Integer labelResId, Drawable icon, Callback callback) {
			this(labelResId != null ? context.getString(labelResId) : null, icon, callback);
		}

		/**
		 * @param inflater
		 * @param layout
		 * @param icon
		 * @param label
		 * @return Item view. If it's not built yet it will be inflated and
		 *         filled now.
		 */
		public View getView(LayoutInflater inflater, int layout, Integer icon, Integer label) {
			if (mView == null) {
				mView = inflater.inflate(layout, null);
				if (icon != null && mIcon != null) {
					ImageView iv = (ImageView) mView.findViewById(icon);
					if (iv != null) {
						iv.setImageDrawable(mIcon);
					}
				}
				if (label != null && mLabel != null) {
					TextView tv = (TextView) mView.findViewById(label);
					if (tv != null) {
						tv.setText(mLabel);
					}
				}
				if (mCallback != null) {
					mView.setClickable(true);
					mView.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View anchor) {
							mCallback.onClick(Item.this, anchor);
						}
					});
				}
			}

			return mView;
		}

		/**
		 * @return Cached view, if it's not built it won't be built yet and this
		 *         method will return null.
		 */
		public View getView() {
			return mView;
		}

		/**
		 * 
		 * @return
		 */
		public Callback getCallback() {
			return mCallback;
		}

	}

	/**
	 * Action item dedicated to launching an activity.
	 * 
	 * @author naholyr
	 */
	public static class IntentItem extends Item {

		/**
		 * Used if activity described by the intent cannot be found
		 * 
		 * @author naholyr
		 */
		public static interface ErrorCallback {

			/**
			 * Activity not found !
			 * 
			 * @param e
			 *            Exception thrown by
			 *            {@link Context#startActivity(Intent)}
			 */
			public void onError(ActivityNotFoundException e, IntentItem item);

		}

		/**
		 * Callback used by intent items
		 */
		static final Callback INTENT_ITEM_CALLBACK = new Callback() {

			void onClick(IntentItem item) {
				try {
					item.getContext().startActivity(item.getIntent());
				} catch (ActivityNotFoundException e) {
					ErrorCallback callback = item.getErrorCallback();
					if (callback != null) {
						callback.onError(e, item);
					}
				}
			}

			@Override
			public void onClick(Item item, View anchor) {
				if (item instanceof IntentItem) {
					onClick((IntentItem) item);
				} else {
					throw new IllegalArgumentException("Expected IntentItem");
				}
			}

		};

		Intent mIntent;

		Context mContext;

		ErrorCallback mErrorCallback;

		/**
		 * Build an action item dedicated to launching activities.
		 * 
		 * @param context
		 *            Activity context.
		 * @param label
		 *            Item's label.
		 * @param icon
		 *            Item's icon.
		 * @param intent
		 *            Target activity intent.
		 * @param errorCallback
		 *            ActivityNotFound handler.
		 */
		public IntentItem(Context context, String label, Drawable icon, Intent intent, ErrorCallback errorCallback) {
			super(label, icon, INTENT_ITEM_CALLBACK);
			mContext = context;
			mIntent = new Intent(intent);
			mErrorCallback = errorCallback;
		}

		/**
		 * No error callback.
		 * 
		 * @see {@link IntentItem#IntentItem(Context, String, Drawable, Intent, ErrorCallback)}
		 * @param context
		 * @param label
		 * @param icon
		 * @param intent
		 */
		public IntentItem(Context context, String label, Drawable icon, Intent intent) {
			this(context, label, icon, intent, null);
		}

		/**
		 * Use resource id for label and icon.
		 * 
		 * @see {@link IntentItem#IntentItem(Context, String, Drawable, Intent, ErrorCallback)}
		 * @param context
		 * @param labelResId
		 * @param iconResId
		 * @param intent
		 * @param errorCallback
		 */
		public IntentItem(Context context, Integer labelResId, Integer iconResId, Intent intent, ErrorCallback errorCallback) {
			this(context, labelResId != null ? context.getString(labelResId) : null, iconResId != null ? context.getResources().getDrawable(iconResId) : null, intent,
					errorCallback);
		}

		/**
		 * No error callback.
		 * 
		 * @see {@link IntentItem#IntentItem(Context, String, Drawable, Intent, ErrorCallback)}
		 * @param context
		 * @param labelResId
		 * @param iconResId
		 * @param intent
		 */
		public IntentItem(Context context, Integer labelResId, Integer iconResId, Intent intent) {
			this(context, labelResId, iconResId, intent, null);
		}

		/**
		 * Use resource id for icon.
		 * 
		 * @see {@link IntentItem#IntentItem(Context, String, Drawable, Intent, ErrorCallback)}
		 * @param context
		 * @param label
		 * @param iconResId
		 * @param intent
		 * @param errorCallback
		 */
		public IntentItem(Context context, String label, Integer iconResId, Intent intent, ErrorCallback errorCallback) {
			this(context, label, iconResId != null ? context.getResources().getDrawable(iconResId) : null, intent, errorCallback);
		}

		/**
		 * No error callback.
		 * 
		 * @see {@link IntentItem#IntentItem(Context, String, Drawable, Intent, ErrorCallback)}
		 * @param context
		 * @param label
		 * @param iconResId
		 * @param intent
		 */
		public IntentItem(Context context, String label, Integer iconResId, Intent intent) {
			this(context, label, iconResId, intent, null);
		}

		/**
		 * Use resource id for label.
		 * 
		 * @see {@link IntentItem#IntentItem(Context, String, Drawable, Intent, ErrorCallback)}
		 * @param context
		 * @param labelResId
		 * @param icon
		 * @param intent
		 * @param errorCallback
		 */
		public IntentItem(Context context, Integer labelResId, Drawable icon, Intent intent, ErrorCallback errorCallback) {
			this(context, labelResId != null ? context.getString(labelResId) : null, icon, intent, errorCallback);
		}

		/**
		 * No error callback.
		 * 
		 * @see {@link IntentItem#IntentItem(Context, String, Drawable, Intent, ErrorCallback)}
		 * @param context
		 * @param labelResId
		 * @param icon
		 * @param intent
		 */
		public IntentItem(Context context, Integer labelResId, Drawable icon, Intent intent) {
			this(context, labelResId, icon, intent, null);
		}

		/**
		 * 
		 * @return
		 */
		public Intent getIntent() {
			return mIntent;
		}

		/**
		 * 
		 * @return
		 */
		public Context getContext() {
			return mContext;
		}

		/**
		 * 
		 * @return
		 */
		public ErrorCallback getErrorCallback() {
			return mErrorCallback;
		}

	}

	/**
	 * This class will help you define specific placeholders in your quick
	 * action window for IntentItems you would expect but who have not been
	 * found. For each of these ones not found, an intent item will be added
	 * instead.
	 * 
	 * @author naholyr
	 */
	public static abstract class Advertisement {

		/**
		 * Expected package name
		 */
		public String packageName;

		/**
		 * Expected class name
		 */
		public String activityName;

		/**
		 * Placeholder icon
		 */
		public Drawable icon;

		/**
		 * Placeholder label
		 */
		public String label;

		/**
		 * @param packageName
		 * @param activityName
		 * @param label
		 * @param icon
		 */
		public Advertisement(String packageName, String activityName, String label, Drawable icon) {
			this.packageName = packageName;
			this.activityName = activityName;
			this.icon = icon;
			this.label = label;
		}

		/**
		 * @param context
		 * @param errorCallback
		 *            Error callback of the generated item.
		 * @return Item added as the placeholder for expected IntentItem. It
		 *         will generate an intent item to Android Market details page
		 *         of the targeted package, with no error handler.
		 */
		public abstract Item getReplacementItem(Context context);

		public Object getActivityClassName() {
			if (activityName.charAt(0) == '.') {
				return packageName + activityName;
			} else {
				return activityName;
			}
		}

	}

	/**
	 * Specific advertisement placeholder that will simply open Android Market
	 * to the expected package details page.
	 * 
	 * @author naholyr
	 */
	public static class MarketAdvertisement extends Advertisement {

		private IntentItem.ErrorCallback mErrorCallback;

		/**
		 * @see {@link Advertisement#IntentItemAdvertisement(String, Drawable, String, String)}
		 * @param packageName
		 * @param activityName
		 * @param label
		 * @param icon
		 * @param marketNotFoundCallback
		 *            Error handler for situation where Android Market activity
		 *            has not been found.
		 */
		public MarketAdvertisement(String packageName, String activityName, String label, Drawable icon, IntentItem.ErrorCallback marketNotFoundCallback) {
			super(packageName, activityName, label, icon);
			mErrorCallback = marketNotFoundCallback;
		}

		/**
		 * If Android Market is not installed on device, a short toast will be
		 * shown with given error message.
		 * 
		 * @param packageName
		 * @param activityName
		 * @param label
		 * @param icon
		 * @param marketNotInstalledOnThisDeviceStringResId
		 *            Error message string resource ID.
		 */
		public MarketAdvertisement(String packageName, String activityName, String label, Drawable icon, final String marketNotInstalledOnThisDevice) {
			this(packageName, activityName, label, icon, new IntentItem.ErrorCallback() {
				@Override
				public void onError(ActivityNotFoundException e, IntentItem item) {
					Toast.makeText(item.getContext(), marketNotInstalledOnThisDevice, Toast.LENGTH_SHORT).show();
				}
			});
		}

		@Override
		public Item getReplacementItem(Context context) {
			Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));

			return new IntentItem(context, label, icon, intent, mErrorCallback);
		}

	}

	LayoutInflater mInflater;

	ArrayList<Item> mItems;

	boolean mDismissOnClick;

	SparseIntArray mConfig;

	static SparseArray<ArrayList<Item>> cachedItems = new SparseArray<ArrayList<Item>>();

	/**
	 * @see {@link #getWindow(Activity, Config, List)}
	 * @param activity
	 * @param config
	 *            Window configuration.
	 * @param initializer
	 *            Items initializer.
	 * @param windowID
	 *            Unique id of this window (re-use the same ID for later use, so
	 *            that items-caching can be relied on).
	 * @return Quick action window fully initialized.
	 */
	public static QuickActionWindow getWindow(Activity activity, SparseIntArray config, Initializer initializer, Integer windowID) {
		ArrayList<Item> items = null;
		if (windowID != null) {
			items = cachedItems.get(windowID, null);
		}

		QuickActionWindow w = getWindow(activity, config, items);

		if (items == null && initializer != null) {
			items = new ArrayList<Item>();
			initializer.setItems(w);
			if (windowID != null) {
				cachedItems.put(windowID, w.getItems());
			}
		}

		return w;
	}

	/**
	 * @param activity
	 * @param config
	 * @param items
	 * @return Quick action window with specified items already added.
	 */
	public static QuickActionWindow getWindow(Activity activity, SparseIntArray config, List<Item> items) {
		LayoutInflater inflater = activity.getLayoutInflater();
		return new QuickActionWindow(inflater, config, items);
	}

	/**
	 * @see {@link #getWindow(Activity, Config, List)}
	 * @param activity
	 * @param config
	 * @return Quick action window with no item.
	 */
	public static QuickActionWindow getWindow(Activity activity, SparseIntArray config) {
		return getWindow(activity, config, null);
	}

	/**
	 * inflater and config cannot be null.
	 * 
	 * @param inflater
	 * @param config
	 * @param items
	 */
	protected QuickActionWindow(LayoutInflater inflater, SparseIntArray config, List<Item> items) {
		super(inflater.inflate(config.get(Config.WINDOW_LAYOUT), null), ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT, false);
		if (config == null) {
			throw new NullPointerException();
		}
		mConfig = config;
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
		getContentView().setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
		// Opening animation
		int windowAnimationStyle = mConfig.get(Config.WINDOW_ANIMATION_STYLE, -1);
		if (windowAnimationStyle != -1) {
			setAnimationStyle(windowAnimationStyle);
		}
	}

	/**
	 * Set to false so that when user clicks on an action item, it won't close
	 * the quick action window.
	 * 
	 * @param enabled
	 */
	public void setDismissOnClick(boolean enabled) {
		mDismissOnClick = enabled;
	}

	/**
	 * Shows quick action window attached to the specified anchor. It will be
	 * shown above or below the anchor, depending on space available up or
	 * under.
	 * 
	 * @param anchor
	 */
	public void show(View anchor) {
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
				yoff = -anchor.getMeasuredHeight() - blockHeight + mConfig.get(Config.ARROW_OFFSET, 0);
				int windowBackground = mConfig.get(Config.WINDOW_BACKGROUND_IF_ABOVE, -1);
				if (windowBackground != -1) {
					contentView.setBackgroundResource(windowBackground);
				}
			} else {
				// Display below anchor
				yoff = -mConfig.get(Config.ARROW_OFFSET, 0);
				int windowBackground = mConfig.get(Config.WINDOW_BACKGROUND_IF_BELOW, -1);
				if (windowBackground != -1) {
					contentView.setBackgroundResource(windowBackground);
				}
			}
			update(anchor, 0, yoff, -1, blockHeight);

			// Animation for all views
			int itemAnimation = mConfig.get(Config.ITEM_APPEAR_ANIMATION, -1);
			if (itemAnimation != -1) {
				Animation anim = AnimationUtils.loadAnimation(anchor.getContext(), itemAnimation);
				for (int i = 0; i < contentView.getChildCount(); i++) {
					View v = contentView.getChildAt(i);
					v.startAnimation(anim);
				}
			}
		}
	}

	/**
	 * Add a new item to the window. Note that view will be attached
	 * dynamically, so you can call this method even if window is already
	 * showing.
	 * 
	 * @param item
	 * @return the window for chaining calls.
	 */
	public QuickActionWindow addItem(Item item) {
		mItems.add(item);
		addItemView(item);

		return this;
	}

	/**
	 * Add a basic item, built with parameters.
	 * 
	 * @see {@link #addItem(Item)}
	 * @see {@link Item#Item(String, Drawable, com.com.naholyr.android.horairessncf.ui.QuickActionWindow.Item.Callback)}
	 * @param label
	 * @param icon
	 * @param callback
	 * @return the window for chaining calls.
	 */
	public QuickActionWindow addItem(String label, Drawable icon, Item.Callback callback) {
		Item item = new Item(label, icon, callback);
		addItem(item);

		return this;
	}

	/**
	 * Add view of the corresponding item.
	 * 
	 * @param item
	 */
	protected void addItemView(final Item item) {
		final Item.Callback listener = item.getCallback();
		int itemLayout = mConfig.get(Config.ITEM_LAYOUT);
		int itemIcon = mConfig.get(Config.ITEM_ICON, -1);
		int itemLabel = mConfig.get(Config.ITEM_LABEL, -1);
		View v = item.getView(mInflater, itemLayout, itemIcon != -1 ? itemIcon : null, itemLabel != -1 ? itemLabel : null);
		v.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View anchor) {
				listener.onClick(item, anchor);
				if (mDismissOnClick) {
					dismiss();
				}
			}
		});
		addView(v);
	}

	/**
	 * Add a view in the window items container.
	 * 
	 * @param v
	 */
	protected void addView(View v) {
		ViewGroup container;
		int containerId = mConfig.get(Config.CONTAINER, -1);
		if (containerId != -1) {
			container = (ViewGroup) getContentView().findViewById(containerId);
		} else {
			container = (ViewGroup) getContentView();
		}
		// Remove view from its original parent before adding it.
		// It allows re-using the same views, or we'll have to catch a layout
		// exception !
		ViewGroup parent = (ViewGroup) v.getParent();
		if (parent != null) {
			parent.removeView(v);
		}
		// Attach view now that it's free.
		container.addView(v);
	}

	/**
	 * Add IntentItem instances for all activities responding to queryIntent.
	 * 
	 * @param context
	 * @param queryIntent
	 *            Intent targeting activities that will be launched by intent
	 *            items.
	 * @param errorCallback
	 */
	public void addItemsForIntent(Context context, Intent queryIntent, IntentItem.ErrorCallback errorCallback) {
		addItemsForIntent(context, queryIntent, errorCallback, null);
	}

	/**
	 * @see {@link #addItemsForIntent(Context, Intent)}
	 * @param context
	 * @param queryIntent
	 * @param errorCallback
	 * @param ads
	 *            Placeholder describers for expected activities that have not
	 *            been found.
	 */
	public void addItemsForIntent(Context context, Intent queryIntent, IntentItem.ErrorCallback errorCallback, Advertisement[] ads) {
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
			addItem(new IntentItem(context, label, icon, intent, errorCallback));
		}
		// Check found names, and eventually add advertisement actions
		if (ads != null) {
			for (Advertisement ad : ads) {
				boolean found = false;
				for (int i = 0; i < names.length; i++) {
					if (names[i].equals(ad.getActivityClassName())) {
						found = true;
						break;
					}
				}
				if (!found) {
					addItem(ad.getReplacementItem(context));
				}
			}
		}
	}

	/**
	 * Every IntentItem instance of this window will add given extras to its
	 * intents.
	 * 
	 * @param extras
	 *            Extras to be added.
	 * @param baseIntent
	 *            Intent used for comparison : only the IntentItem instances
	 *            whose intent will correspond to this base intent will be
	 *            targeted.
	 */
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

	/**
	 * 
	 * @return
	 */
	public ArrayList<Item> getItems() {
		return mItems;
	}

}
