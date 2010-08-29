package com.naholyr.android.ui.compat;

import android.content.Intent;
import android.os.Bundle;

public interface ContactIntent {

	public static final class Factory {

		public static ContactIntent get(ClassLoader classLoader) {
			try {
				classLoader.loadClass("android.provider.ContactsContract");
				return new ContractContactIntent();
			} catch (ClassNotFoundException e) {
				return new CupcakeContactIntent();
			}
		}

	}

	public Intent getIntent();

	public void addExtras(Bundle extras, String name, String email);

}
