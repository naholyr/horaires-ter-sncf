package com.naholyr.android.horairessncf.activity;

import com.naholyr.android.horairessncf.Util;

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

	public static final int MSG_SHOW_DIALOG = 10;
	public static final int MSG_DISMISS_DIALOG = 11;
	public static final int MSG_SET_DIALOG_TITLE = 12;
	public static final int MSG_SET_DIALOG_MESSAGE = 13;
	public static final int MSG_SHOW_ERROR = 14;

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
				int dialogId = msg.getData().getInt("value");
				Dialog dialog = getDialog(dialogId);
				if (dialog != null && !dialog.isShowing()) {
					getDialog(dialogId).show();
				}
				break;
			}
			case MSG_DISMISS_DIALOG: {
				int dialogId = msg.getData().getInt("value");
				Dialog dialog = getDialog(dialogId);
				if (dialog != null && dialog.isShowing()) {
					getDialog(dialogId).dismiss();
				}
				break;
			}
			case MSG_SET_DIALOG_TITLE: {
				int dialogId = msg.getData().getInt("id");
				String title = msg.getData().getString("value");
				Dialog dialog = getDialog(dialogId);
				if (dialog != null) {
					getDialog(dialogId).setTitle(title);
				}
				break;
			}
			case MSG_SET_DIALOG_MESSAGE: {
				int dialogId = msg.getData().getInt("id");
				String message = msg.getData().getString("value");
				Dialog dialog = getDialog(dialogId);
				if (dialog != null) {
					getDialog(dialogId).setMessage(message);
				}
				break;
			}
			case MSG_SHOW_ERROR: {
				String message = msg.getData().getString("value");
				Util.showError(this, message);
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

}
