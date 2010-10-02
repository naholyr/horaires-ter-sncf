package com.naholyr.android.horairessncf.ui;

import java.io.IOException;
import java.io.InputStream;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.widget.LinearLayout;

import com.naholyr.android.horairessncf.R;

public class AboutDialog {

	public static Dialog create(Context context) {
		final LinearLayout contentView = new LinearLayout(context);
		contentView.setOrientation(LinearLayout.VERTICAL);
		String aboutTitle = context.getString(R.string.app_name);
		try {
			PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA);
			aboutTitle += "\nv. " + pInfo.versionName;
		} catch (NameNotFoundException e) {
			// Don't add version info
		}
		try {
			WebView aboutView = new WebView(context);
			InputStream aboutStream = context.getResources().openRawResource(R.raw.about);
			int aboutStreamSize = aboutStream.available();
			byte[] aboutStreamBuffer = new byte[aboutStreamSize];
			aboutStream.read(aboutStreamBuffer);
			aboutStream.close();
			String aboutContent = new String(aboutStreamBuffer);
			aboutView.loadData(aboutContent, "text/html", "utf-8");
			contentView.addView(aboutView, LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		} catch (IOException e) {
			// Don't add about info
		}
		return new AlertDialog.Builder(context).setTitle(aboutTitle).setIcon(R.drawable.icon).setView(contentView).setCancelable(true).setNeutralButton(android.R.string.ok, null)
				.create();
	}

}
