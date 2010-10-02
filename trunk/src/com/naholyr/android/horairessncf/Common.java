package com.naholyr.android.horairessncf;

import java.util.ArrayList;
import java.util.List;

import org.acra.ErrorReporter;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.Toast;

import com.naholyr.android.ui.QuickActionWindow;
import com.naholyr.android.ui.QuickActionWindow.IntentItem;

public class Common {

	public static final String TAG = "HorairesTERSNCF";

	private static final String PLAIN_ASCII = "AaEeIiOoUu" // grave
			+ "AaEeIiOoUuYy" // acute
			+ "AaEeIiOoUuYy" // circumflex
			+ "AaOoNn" // tilde
			+ "AaEeIiOoUuYy" // umlaut
			+ "Aa" // ring
			+ "Cc" // cedilla
			+ "OoUu" // double acute
	;

	private static final String UNICODE = "\u00C0\u00E0\u00C8\u00E8\u00CC\u00EC\u00D2\u00F2\u00D9\u00F9"
			+ "\u00C1\u00E1\u00C9\u00E9\u00CD\u00ED\u00D3\u00F3\u00DA\u00FA\u00DD\u00FD" + "\u00C2\u00E2\u00CA\u00EA\u00CE\u00EE\u00D4\u00F4\u00DB\u00FB\u0176\u0177"
			+ "\u00C3\u00E3\u00D5\u00F5\u00D1\u00F1" + "\u00C4\u00E4\u00CB\u00EB\u00CF\u00EF\u00D6\u00F6\u00DC\u00FC\u0178\u00FF" + "\u00C5\u00E5" + "\u00C7\u00E7"
			+ "\u0150\u0151\u0170\u0171";

	public static String removeAccents(String s) {
		if (s == null) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		int n = s.length();
		for (int i = 0; i < n; i++) {
			char c = s.charAt(i);
			int pos = UNICODE.indexOf(c);
			if (pos > -1) {
				sb.append(PLAIN_ASCII.charAt(pos));
			} else {
				sb.append(c);
			}
		}

		return sb.toString();
	}

	public static final Integer DEFAULT_NB_TRAINS = 12;

	public static final Integer QUICK_ACTION_WINDOW_GARE = 1;
	public static final Integer QUICK_ACTION_WINDOW_DEPART = 2;
	public static final Integer QUICK_ACTION_WINDOW_ARRET_UNCOMPLETE = 2;
	public static final Integer QUICK_ACTION_WINDOW_ARRET_COMPLETE = 2;

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

	public final static class PluginMarketAdvertisement extends QuickActionWindow.MarketAdvertisement {
		private static final String PKG_PREFIX = "com.naholyr.android.horairessncf.plugins.";

		public PluginMarketAdvertisement(Context context, String plugin, String activity, int icon, String label) {
			super(PKG_PREFIX + plugin, ".activity." + activity, label, context.getResources().getDrawable(icon), "Erreur : Android Market non installé sur ce périphérique !");
		}
	}

	public static final int GARE = 1;
	public static final int ARRET = 2;
	public static final int TRAIN = 3;

	private static final QuickActionWindow.Advertisement[] getQuickActionAds(Context context, int type) {
		List<QuickActionWindow.Advertisement> ads = new ArrayList<QuickActionWindow.Advertisement>();

		switch (type) {
			case GARE:
			case ARRET: {
				// ads.add(new PluginMarketAdvertisement(context, "gmap",
				// "MapActivity", R.drawable.quick_action_gmap,
				// "Localiser sur une carte"));
				// ads.add(new PluginMarketAdvertisement(context, "itineraire",
				// "ItineraireFromActivity",
				// R.drawable.quick_action_itineraire_from,
				// "Itinéraire depuis cette gare..."));
				// ads.add(new PluginMarketAdvertisement(context, "itineraire",
				// "ItineraireToActivity",
				// R.drawable.quick_action_itineraire_to,
				// "Itinéraire vers cette gare..."));
				break;
			}
			case TRAIN: {
				// ads.add(new PluginMarketAdvertisement(context,
				// "notification", "MainActivity",
				// R.drawable.quick_action_notification, "Suivre ce train"));
				break;
			}
		}

		return ads.toArray(new QuickActionWindow.Advertisement[0]);
	}

	private static final void addQuickActionAds(final Activity activity, Intent pluginIntent, QuickActionWindow window, int type) {
		window.addItemsForIntent(activity, pluginIntent, new QuickActionWindow.IntentItem.ErrorCallback() {
			@Override
			public void onError(ActivityNotFoundException e, IntentItem item) {
				Toast.makeText(item.getContext(), "Erreur : Application Market introuvable", Toast.LENGTH_LONG).show();
				ErrorReporter.getInstance().handleSilentException(e);
			}
		}, getQuickActionAds(activity, type));
	}

	public static final QuickActionWindow getQuickActionWindow(final Activity activity, final int type, final long id) {
		final Intent pluginIntent = new Intent(Intent.ACTION_VIEW);
		QuickActionWindow window = null;
		Bundle extras = new Bundle();

		switch (type) {
			case GARE:
			case ARRET: {
				pluginIntent.setType(Gare.CONTENT_TYPE);
				window = QuickActionWindow.getWindow(activity, Common.QUICK_ACTION_WINDOW_CONFIGURATION, new QuickActionWindow.Initializer() {
					@Override
					public void setItems(QuickActionWindow window) {
						// Plugins
						addQuickActionAds(activity, pluginIntent, window, type);
					}
				}, Common.QUICK_ACTION_WINDOW_GARE);
				// Add item "add/remove to favorites", always here
				int favStringId, favIconId;
				if (Gare.getFavorites(activity).has(id)) {
					favStringId = R.string.action_remove_favorite;
					favIconId = R.drawable.quick_action_remove_favorite;
				} else {
					favStringId = R.string.action_add_favorite;
					favIconId = R.drawable.quick_action_add_favorite;
				}
				window.addItem(activity.getString(favStringId), activity.getResources().getDrawable(favIconId), new QuickActionWindow.Item.Callback() {
					@Override
					public void onClick(QuickActionWindow window, QuickActionWindow.Item item, View anchor) {
						View favButton = anchor.findViewById(R.id.favicon);
						if (favButton != null) {
							favButton.performClick();
						} else {
							Gare.getFavorites(anchor.getContext()).add(id);
						}
					}
				}, 0);
				// Complete intent items, adding station ID
				extras.putLong(Gare._ID, id);
				window.dispatchIntentExtras(extras, pluginIntent);
				// Integration with Google Maps
				Cursor c = Gare.retrieveById(activity, id);
				if (c != null && c.moveToFirst()) {
					Double latitude = c.getDouble(c.getColumnIndex(Gare.LATITUDE));
					Double longitude = c.getDouble(c.getColumnIndex(Gare.LATITUDE));
					if (latitude != null && longitude != null && latitude != 0 && longitude != 0) {
						Intent gmapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:" + latitude + "," + longitude));
						window.addItemsForIntent(activity, gmapIntent, null);
					}
				}
				break;
			}
			case TRAIN: {
				pluginIntent.setType(Depart.CONTENT_TYPE);
				window = QuickActionWindow.getWindow(activity, Common.QUICK_ACTION_WINDOW_CONFIGURATION, new QuickActionWindow.Initializer() {
					@Override
					public void setItems(QuickActionWindow window) {
						// Plugins
						addQuickActionAds(activity, pluginIntent, window, type);
					}
				}, Common.QUICK_ACTION_WINDOW_DEPART);
				// Complete intent items, adding station ID
				extras.putLong(Depart._ID, id);
				window.dispatchIntentExtras(extras, pluginIntent);
				break;
			}
		}

		return window;
	}

}
