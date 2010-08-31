package com.naholyr.android.horairessncf;

import android.content.Context;
import android.util.SparseIntArray;

import com.naholyr.android.ui.QuickActionWindow;

public class Common {

	public static final boolean DEBUG = true;

	public static final double DEBUG_LATITUDE = 45.7605367;
	public static final double DEBUG_LONGITUDE = 4.8589391;

	public static final SparseIntArray QUICK_ACTION_WINDOW_CONFIGURATION = new SparseIntArray() {
		{
			put(QuickActionWindow.Config.WINDOW_LAYOUT, R.layout.quick_action_window);
			put(QuickActionWindow.Config.WINDOW_BACKGROUND_IF_ABOVE, R.drawable.quick_actions_background_above);
			put(QuickActionWindow.Config.WINDOW_BACKGROUND_IF_BELOW, R.drawable.quick_actions_background_below);
			put(QuickActionWindow.Config.ITEM_LAYOUT, R.layout.quick_action_item);
			put(QuickActionWindow.Config.WINDOW_ANIMATION_STYLE, R.style.Animation_QuickActionWindow);
			put(QuickActionWindow.Config.ITEM_APPEAR_ANIMATION, R.anim.quick_action_item_appear);
			put(QuickActionWindow.Config.CONTAINER, R.id.quick_actions);
			put(QuickActionWindow.Config.ITEM_ICON, R.id.quick_action_icon);
			put(QuickActionWindow.Config.ITEM_LABEL, R.id.quick_action_label);
			put(QuickActionWindow.Config.ARROW_OFFSET, 20);

		}
	};

	public static final String TAG = "HorairesTERSNCF";

	public static final Integer DEFAULT_NB_TRAINS = 12;

	public static final Integer QUICK_ACTION_WINDOW_GARE = 1;
	public static final Integer QUICK_ACTION_WINDOW_DEPART = 2;

	public final static class PluginMarketAdvertisement extends QuickActionWindow.MarketAdvertisement {
		private static final String PKG_PREFIX = "com.naholyr.android.horairessncf.plugins.";

		public PluginMarketAdvertisement(Context context, String plugin, String activity, int icon, String label) {
			super(PKG_PREFIX + plugin, ".activity." + activity, label, context.getResources().getDrawable(icon), "Erreur : Android Market non installé sur ce périphérique !");
		}
	}

}
