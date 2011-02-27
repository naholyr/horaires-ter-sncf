package com.naholyr.android.horairessncf.activity;

import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.naholyr.android.horairessncf.Common;
import com.naholyr.android.horairessncf.R;
import com.naholyr.android.ui.QuickActionWindow;
import com.ubikod.capptain.android.sdk.activity.CapptainListActivity;

abstract public class ListActivity extends CapptainListActivity {

	private Cursor mCursor;

	protected boolean mLoading = false;

	private Handler mQueryFailureHandler = new Handler(new Handler.Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			if (msg.obj != null && msg.obj instanceof Throwable) {
				onQueryFailure((Throwable) msg.obj);
			} else {
				onQueryFailure(null);
			}
			return true;
		}
	});

	@Override
	public void onCreate(Bundle savedInstanceState) {
		requestWindowFeatures();
		super.onCreate(savedInstanceState);
		setContentView(getLayout());
		findViewById(android.R.id.empty).setVisibility(View.GONE);
		findViewById(R.id.loading).setVisibility(View.VISIBLE);
		new QueryTask().execute();
	}

	@Override
	protected void onListItemClick(ListView listView, View view, int position, long id) {
		// Quick Actions
		showQuickActions(view, position, id);
	}

	protected void showQuickActions(final View anchor, final int position, final long id) {
		QuickActionWindow window = getQuickActionWindow(anchor, position, id);
		if (window != null) {
			Log.d(Common.TAG, "favicon ? " + (anchor.findViewById(R.id.favicon) == null ? "no" : "yes"));
			Log.d(Common.TAG, "nom ? " + (anchor.findViewById(R.id.nom) == null ? "no" : "yes"));
			window.show(anchor);
		}
	}

	private class QueryTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			try {
				mCursor = queryCursor();
				if (mCursor != null) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							findViewById(R.id.loading).setVisibility(View.GONE);
							ListAdapter adapter = getAdapter(mCursor);
							setListAdapter(adapter);
						}
					});
				} else {
					mQueryFailureHandler.sendEmptyMessage(0);
				}
			} catch (Throwable e) {
				mQueryFailureHandler.sendMessage(Message.obtain(mQueryFailureHandler, 0, e));
			}
			return null;
		}

	}

	protected void onQueryFailure(Throwable e) {
		Log.e(Common.TAG, "Query failed", e);
		finish();
	}

	protected Cursor getCursor() {
		return mCursor;
	}

	protected void refresh(final Runnable postRefresh) {
		final View list = getListView();
		final View empty = findViewById(android.R.id.empty);
		final View loading = findViewById(R.id.loading);
		list.setVisibility(View.GONE);
		empty.setVisibility(View.GONE);
		loading.setVisibility(View.VISIBLE);
		mLoading = true;
		new Thread() {
			public void run() {
				getCursor().requery();
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (getCursor().getCount() > 0) {
							list.setVisibility(View.VISIBLE);
						} else {
							empty.setVisibility(View.VISIBLE);
						}
						loading.setVisibility(View.GONE);
						if (postRefresh != null) {
							postRefresh.run();
						}
					}
				});
				mLoading = false;
			}
		}.start();
	}

	protected void requestWindowFeatures() {
	}

	abstract protected int getLayout();

	abstract protected Cursor queryCursor() throws Throwable;

	abstract protected ListAdapter getAdapter(Cursor c);

	abstract protected QuickActionWindow getQuickActionWindow(View anchor, int position, long id);

}
