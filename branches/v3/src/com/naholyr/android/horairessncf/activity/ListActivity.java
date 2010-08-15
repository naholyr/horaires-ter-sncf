package com.naholyr.android.horairessncf.activity;

import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.ListAdapter;

import com.naholyr.android.horairessncf.R;

abstract public class ListActivity extends android.app.ListActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeatures();
		setContentView(getLayout());
		findViewById(android.R.id.empty).setVisibility(View.GONE);
		findViewById(R.id.loading).setVisibility(View.VISIBLE);
		new Thread(new Runnable() {
			public void run() {
				final Cursor c = queryCursor();
				if (c != null) {
					runOnUiThread(new Runnable() {
						public void run() {
							findViewById(R.id.loading).setVisibility(View.GONE);
							startManagingCursor(c);
							ListAdapter adapter = getAdapter(c);
							setListAdapter(adapter);
						}
					});
				} else {
					finish();
				}
			}
		}).start();
	}

	protected void requestWindowFeatures() {
	}

	abstract protected int getLayout();

	abstract protected Cursor queryCursor();

	abstract protected ListAdapter getAdapter(Cursor c);

}
