package com.naholyr.android.horairessncf.view;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

public class ListPreferenceDynamicSummary extends ListPreference {

	public ListPreferenceDynamicSummary(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public ListPreferenceDynamicSummary(Context context) {
		super(context);
	}

	public CharSequence getSummary() {
		CharSequence summary = super.getSummary();
		if (summary != null) {
			CharSequence entry = getEntry();
			if (entry == null) {
				entry = "??";
			}
			summary = summary.toString().replace("%s", entry);
		}

		return summary;
	}

	protected void onDialogClosed(boolean positiveResult) {
		super.onDialogClosed(positiveResult);

		if (positiveResult) {
			// Refresh suppary
			setSummary(getSummary());
		}
	}

}
