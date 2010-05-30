package com.naholyr.android.horairessncf.activity;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.SparseArray;

public class ProgressHandlerActivity extends Activity {

	SparseArray<ProgressDialog> dialogs = new SparseArray<ProgressDialog>();

	protected boolean mDisableDialogs = false;

	public static final int MSG_SHOW_DIALOG = 10;
	public static final int MSG_DISMISS_DIALOG = 11;
	public static final int MSG_SET_DIALOG_TITLE = 12;
	public static final int MSG_SET_DIALOG_MESSAGE = 13;

	public void sendMessage(int what, Bundle data) {
		Message msg = Message.obtain(handler, what);
		if (data != null) {
			msg.setData(data);
		}
		handler.sendMessage(msg);
	}

	public void sendMessage(int what) {
		sendMessage(what, (Bundle) null);
	}

	public void sendMessage(int what, String value) {
		Bundle data = new Bundle();
		data.putString("value", value);
		sendMessage(what, data);
	}

	public void sendMessage(int what, int value) {
		Bundle data = new Bundle();
		data.putInt("value", value);
		sendMessage(what, data);
	}

	public void sendMessage(int what, int id, String value) {
		Bundle data = new Bundle();
		data.putInt("id", id);
		data.putString("value", value);
		sendMessage(what, data);
	}

	public void sendMessage(int what, int id, int value) {
		Bundle data = new Bundle();
		data.putInt("id", id);
		data.putInt("value", value);
		sendMessage(what, data);
	}

	protected ProgressDialog createProgressDialog(int id, String title, String message, int style, boolean cancelable, DialogInterface.OnCancelListener onCancel) {
		if (mDisableDialogs) {
			return null;
		}

		ProgressDialog dialog = new ProgressDialog(this);
		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.setCancelable(false);
		dialog.setProgressStyle(style);

		if (cancelable) {
			dialog.setCancelable(true);
			final Activity this_ = this;
			if (onCancel != null) {
				dialog.setOnCancelListener(onCancel);
			} else {
				dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
					public void onCancel(DialogInterface dialog) {
						this_.finish();
					}
				});
			}
		}

		dialogs.put(id, dialog);

		return dialog;
	}

	protected ProgressDialog createProgressDialog(int id, String title, String message, int style, boolean cancelable) {
		return createProgressDialog(id, title, message, style, cancelable, null);
	}

	protected ProgressDialog createProgressDialog(int id, String title, String message, boolean cancelable, DialogInterface.OnCancelListener onCancel) {
		return createProgressDialog(id, title, message, ProgressDialog.STYLE_HORIZONTAL, cancelable, onCancel);
	}

	protected ProgressDialog createProgressDialog(int id, String title, String message, boolean cancelable) {
		return createProgressDialog(id, title, message, cancelable, null);
	}

	protected ProgressDialog createProgressDialog(int id, String title, String message) {
		return createProgressDialog(id, title, message, false);
	}

	protected ProgressDialog createWaitDialog(int id, String title, String message, boolean cancelable, DialogInterface.OnCancelListener onCancel) {
		return createProgressDialog(id, title, message, ProgressDialog.STYLE_SPINNER, cancelable, onCancel);
	}

	protected ProgressDialog createWaitDialog(int id, String title, String message, boolean cancelable) {
		return createWaitDialog(id, title, message, cancelable, null);
	}

	protected ProgressDialog createWaitDialog(int id, String title, String message) {
		return createWaitDialog(id, title, message, false);
	}

	public ProgressDialog getDialog(int id) {
		return dialogs.get(id);
	}

	protected void handleMessage(Message msg) {
		switch (msg.what) {
			case MSG_SHOW_DIALOG: {
				if (!mDisableDialogs) {
					int dialogId = msg.getData().getInt("value");
					Dialog dialog = getDialog(dialogId);
					if (dialog != null && !dialog.isShowing()) {
						dialog.show();
					}
				}
				break;
			}
			case MSG_DISMISS_DIALOG: {
				if (!mDisableDialogs) {
					int dialogId = msg.getData().getInt("value");
					Dialog dialog = getDialog(dialogId);
					if (!mDisableDialogs && dialog != null && dialog.isShowing()) {
						dialog.dismiss();
					}
				}
				break;
			}
			case MSG_SET_DIALOG_TITLE: {
				if (!mDisableDialogs) {
					int dialogId = msg.getData().getInt("id");
					String title = msg.getData().getString("value");
					Dialog dialog = getDialog(dialogId);
					if (dialog != null) {
						dialog.setTitle(title);
					}
				}
				break;
			}
			case MSG_SET_DIALOG_MESSAGE: {
				if (!mDisableDialogs) {
					int dialogId = msg.getData().getInt("id");
					String message = msg.getData().getString("value");
					ProgressDialog dialog = getDialog(dialogId);
					if (dialog != null) {
						dialog.setMessage(message);
					}
				}
				break;
			}
		}
	}

	private Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			ProgressHandlerActivity.this.handleMessage(msg);
		}
	};

	public Handler getHandler() {
		return handler;
	}

	@Override
	protected void onPause() {
		super.onPause();
		mDisableDialogs = true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		mDisableDialogs = false;
	}

}
