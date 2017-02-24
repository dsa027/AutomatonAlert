package com.aldersonet.automatonalert.Util;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;

public class AAProgressDialog extends ProgressDialog {

	String mPreamble;
	Context mContext;

	public AAProgressDialog(
			Context context, String preamble) {
		super(context);
		mPreamble = preamble;
		mContext = context;
	}

	@Override
	public void setMessage(final CharSequence message) {
		((Activity)mContext).runOnUiThread(new Runnable() {
			public void run() {
				String s = mPreamble + "\r\n\r\n" + message;
				AAProgressDialog.super.setMessage(s);
			}
		});
	}
}
