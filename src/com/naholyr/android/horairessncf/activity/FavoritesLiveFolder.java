package com.naholyr.android.horairessncf.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.net.Uri;
import android.os.Bundle;
import android.provider.LiveFolders;

import com.naholyr.android.horairessncf.R;
import com.naholyr.android.horairessncf.data.GaresContentProvider;

public class FavoritesLiveFolder extends Activity {

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		final String action = intent.getAction();

		if (LiveFolders.ACTION_CREATE_LIVE_FOLDER.equals(action)) {
			setResult(RESULT_OK, createLiveFolder());
		} else {
			setResult(RESULT_CANCELED);
		}

		finish();
	}

	private Intent createLiveFolder() {
		final Intent intent = new Intent();

		Uri source = Uri.parse("content://" + GaresContentProvider.AUTHORITY + "/gares/favorites");
		Uri target = Uri.parse("content://" + GaresContentProvider.AUTHORITY + "/gares/#");

		intent.setData(source);
		intent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_NAME, getFolderName());
		intent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_ICON, getFolderIcon());
		intent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_DISPLAY_MODE, LiveFolders.DISPLAY_MODE_LIST);
		intent.putExtra(LiveFolders.EXTRA_LIVE_FOLDER_BASE_INTENT, new Intent(Intent.ACTION_VIEW, target));

		return intent;
	}

	private String getFolderName() {
		return "Gares favorites";
	}

	private ShortcutIconResource getFolderIcon() {
		int id = R.drawable.icon;

		return Intent.ShortcutIconResource.fromContext(this, id);
	}

}
