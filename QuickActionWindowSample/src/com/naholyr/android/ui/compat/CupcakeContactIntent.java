package com.naholyr.android.ui.compat;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Contacts.People;
import android.provider.Contacts.Intents;

@SuppressWarnings("deprecation")
public class CupcakeContactIntent implements ContactIntent {

	@Override
	public void addExtras(Bundle extras, String name, String email) {
		extras.putString(Intents.Insert.NAME, name);
		extras.putString(Intents.Insert.EMAIL, email);
	}

	@Override
	public Intent getIntent() {
		final Intent contactIntent = new Intent(Intents.Insert.ACTION, People.CONTENT_URI);
		contactIntent.putExtra(Intents.EXTRA_FORCE_CREATE, true);

		return contactIntent;
	}

}
