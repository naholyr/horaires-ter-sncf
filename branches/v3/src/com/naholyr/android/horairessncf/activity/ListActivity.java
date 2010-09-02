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

abstract public class ListActivity extends android.app.ListActivity {

	private Cursor mCursor;

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
		super.onCreate(savedInstanceState);
		requestWindowFeatures();
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
		QuickActionWindow window = getQuickActionWindow(position, id);
		if (window != null) {
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
							startManagingCursor(mCursor);
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

	protected void requestWindowFeatures() {
	}

	abstract protected int getLayout();

	abstract protected Cursor queryCursor() throws Throwable;

	abstract protected ListAdapter getAdapter(Cursor c);

	abstract protected QuickActionWindow getQuickActionWindow(int position, long id);

}
