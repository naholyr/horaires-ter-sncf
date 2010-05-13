package com.naholyr.android.horairessncf.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.naholyr.android.horairessncf.R;

public class AboutActivity extends Activity {

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTitle("À propos");
		setContentView(R.layout.apropos);

		new Thread(new Runnable() {
			public void run() {
				runOnUiThread(new Runnable() {
					public void run() {
						((Button) findViewById(R.id.AboutDialog_ButtonClose)).setOnClickListener(new View.OnClickListener() {
							public void onClick(View v) {
								finish();
							}
						});
						((TextView) findViewById(R.id.AboutDialog_TextObjectifs)).setText(R.string.text_objectifs);
						((TextView) findViewById(R.id.AboutDialog_TextRappel)).setText(R.string.text_rappel);
						((TextView) findViewById(R.id.AboutDialog_TextAvertissements)).setText(R.string.text_avertissements);
						((TextView) findViewById(R.id.AboutDialog_TextEvolutions)).setText(R.string.text_evolutions);
						((TextView) findViewById(R.id.AboutDialog_TextChangelog)).setText(R.string.text_changelog);
					}
				});
			}
		}).start();
	}

}
