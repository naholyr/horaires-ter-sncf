package com.naholyr.android.horairessncf.data;

import android.content.SearchRecentSuggestionsProvider;

public class GaresSearchSuggestionsProvider extends SearchRecentSuggestionsProvider {

	public final static String AUTHORITY = "naholyr.horairessncf.providers.GaresSearchSuggestionsProvider";
	public final static int MODE = DATABASE_MODE_QUERIES | DATABASE_MODE_2LINES;

	public GaresSearchSuggestionsProvider() {
		setupSuggestions(AUTHORITY, MODE);
	}

}
