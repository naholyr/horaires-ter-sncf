package com.naholyr.android.ui;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.SparseIntArray;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.naholyr.android.ui.QuickActionWindow.IntentItem;
import com.naholyr.android.ui.QuickActionWindow.Item;

public class SampleActivity extends ListActivity {

	ArrayList<HashMap<String, String>> mData = new ArrayList<HashMap<String, String>>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Inflate layout
		setContentView(R.layout.main);

		// Set list data
		addRow("John Doe", "john.doe@fake.mel");
		addRow("Darth Vader", "i.am.not.evil@fake.mel");
		addRow("Steve Jobs", "i.am.evil@fake.mel");
		addRow("Malcolm X", "x@fake.mel");
		addRow("Andy Rubin", "andy.rubin@fake.mel");
		addRow("Jean-Pierre Foucault", "omg@fake.mel");
		setListAdapter(new SimpleAdapter(this, mData, android.R.layout.simple_list_item_2, new String[] { "line1", "line2" }, new int[] { android.R.id.text1, android.R.id.text2 }));

		// Click on item
		final Context context = this;
		getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> adapter, final View anchor, final int position, final long id) {

				// Configuration of the quick action window
				SparseIntArray windowConfiguration = new SparseIntArray();
				windowConfiguration.put(QuickActionWindow.Config.WINDOW_LAYOUT, R.layout.quick_action_window);
				windowConfiguration.put(QuickActionWindow.Config.WINDOW_BACKGROUND_IF_ABOVE, R.drawable.quick_actions_background_above);
				windowConfiguration.put(QuickActionWindow.Config.WINDOW_BACKGROUND_IF_BELOW, R.drawable.quick_actions_background_below);
				windowConfiguration.put(QuickActionWindow.Config.ITEM_LAYOUT, R.layout.quick_action_item);
				windowConfiguration.put(QuickActionWindow.Config.WINDOW_ANIMATION_STYLE, R.style.Animation_QuickActionWindow);
				windowConfiguration.put(QuickActionWindow.Config.ITEM_APPEAR_ANIMATION, R.anim.quick_action_item_appear);
				windowConfiguration.put(QuickActionWindow.Config.CONTAINER, R.id.quick_actions);
				windowConfiguration.put(QuickActionWindow.Config.ITEM_ICON, R.id.quick_action_icon);
				windowConfiguration.put(QuickActionWindow.Config.ITEM_LABEL, R.id.quick_action_label);
				windowConfiguration.put(QuickActionWindow.Config.ARROW_OFFSET, 20);

				// Initialize items : we use an initializer to enable cache of
				// items (as we have intent items, it's not a bad habit to query
				// activities only once ;))
				QuickActionWindow.Initializer itemsInitializer = new QuickActionWindow.Initializer() {
					@Override
					public void setItems(QuickActionWindow window) {

						// 1. Add some basic (not dynamic) items
						// Option "yes"
						window.addItem(new QuickActionWindow.Item(context, android.R.string.yes, android.R.drawable.ic_input_add, new QuickActionWindow.Item.Callback() {
							@Override
							public void onClick(Item item, View anchor) {
								Toast.makeText(context, "You clicked yes :)", Toast.LENGTH_SHORT).show();
							}
						}));
						// Option "no"
						window.addItem(new QuickActionWindow.Item(context, android.R.string.no, android.R.drawable.ic_input_delete, new QuickActionWindow.Item.Callback() {
							@Override
							public void onClick(Item item, View anchor) {
								Toast.makeText(context, "You clicked no :(", Toast.LENGTH_SHORT).show();
							}
						}));

						// 2. Add items that will respond to a specific intent
						// Link to Android Market
						Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?q=pub:\"Nicolas Chambrier\""));
						window.addItemsForIntent(context, marketIntent, null);
						// Open a non-existing activity
						Intent undefIntent = new Intent("ACTION_UNDEFINED", Uri.parse("undef://undef/undex?undef=undef"));
						window.addItem(new QuickActionWindow.IntentItem(context, "Undef.", (Drawable) null, undefIntent, new QuickActionWindow.IntentItem.ErrorCallback() {
							@Override
							public void onError(ActivityNotFoundException e, IntentItem item) {
								Toast.makeText(context, "Activity not found (clicked on position " + position + ")", Toast.LENGTH_LONG).show();
							}
						}));
						// Open application only if it's installed, else add an
						// item that will open Android Market
						Intent undefIntent2 = new Intent(Intent.ACTION_EDIT);
						undefIntent2.setType("mime/type");
						QuickActionWindow.Advertisement[] ads = new QuickActionWindow.Advertisement[] { new QuickActionWindow.MarketAdvertisement("pkg", ".Activity",
								"Activity not found", null, "Android Market not found"), };
						window.addItemsForIntent(context, undefIntent2, null, ads);
						// Special case : Intent with extras
						// This one will propose to add/edit contact by name.
						// No extra added here: we'll dispatch them outside item
						// initializer.
						Intent contactIntent = new Intent(ContactsContract.Intents.Insert.ACTION, ContactsContract.Contacts.CONTENT_URI);
						contactIntent.putExtra(ContactsContract.Intents.EXTRA_FORCE_CREATE, true);
						window.addItemsForIntent(context, contactIntent, null);

					}
				};

				// Initialize window
				QuickActionWindow window = QuickActionWindow.getWindow(SampleActivity.this, windowConfiguration, itemsInitializer, 0);

				// Special case of the intent with extras : dispatch extras now
				Bundle extras = new Bundle();
				Intent contactIntent = new Intent(ContactsContract.Intents.Insert.ACTION, ContactsContract.Contacts.CONTENT_URI);
				extras.putString(ContactsContract.Intents.Insert.NAME, ((TextView) anchor.findViewById(android.R.id.text1)).getText().toString());
				extras.putString(ContactsContract.Intents.Insert.EMAIL, ((TextView) anchor.findViewById(android.R.id.text2)).getText().toString());
				window.dispatchIntentExtras(extras, contactIntent);

				// Show window
				window.show(anchor);
			}

		});
	}

	private void addRow(String name, String mail) {
		HashMap<String, String> row = new HashMap<String, String>();
		row.put("line1", name);
		row.put("line2", mail);
		mData.add(row);
	}

}
