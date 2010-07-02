package com.naholyr.android.horairessncf.activity;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.naholyr.android.horairessncf.R;

public class AboutActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTitle("À propos");
		setContentView(R.layout.about);

		((Button) findViewById(R.id.AboutDialog_ButtonClose)).setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				finish();
			}
		});
	}

}
