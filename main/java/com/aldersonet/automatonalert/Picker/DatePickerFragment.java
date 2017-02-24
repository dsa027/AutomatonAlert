package com.aldersonet.automatonalert.Picker;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.widget.DatePicker;

import java.util.Calendar;
import java.util.Date;

public class DatePickerFragment extends DialogFragment {

	Calendar mCompareToDate;
	Context mContext;
	IDPListener mDPListener;
	int mRetCode;

	DatePickerDialog mDatePickerDialog;
	boolean mDialogCanceled = false;

	int mHoldHourOfDay;
	int mHoldMinute;

	public interface IDPListener {
		public boolean datePickerDateChanged(Calendar date, int retCode);
		public void datePickerPostProcess(int retCode);
	}

	public static DatePickerFragment showInstance(
			Context context, final FragmentActivity activity,
			Date date, IDPListener dpListener, int retCode) {

		final DatePickerFragment dialog = new DatePickerFragment();
		dialog.mContext = context;
		dialog.mDPListener = dpListener;
		dialog.mRetCode = retCode;
		dialog.setCompareToDate(
				(date == null) ? System.currentTimeMillis() : date.getTime());

		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				dialog.show(activity.getSupportFragmentManager(), "FreeFormPhraseDialog");
			}
		});

		return dialog;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		int year = mCompareToDate.get(Calendar.YEAR);
		int monthOfYear = mCompareToDate.get(Calendar.MONTH);
		int dayOfMonth = mCompareToDate.get(Calendar.DAY_OF_MONTH);

		mDatePickerDialog =
				new OurDatePickerDialog(
						getActivity(), mDateSetListener, year, monthOfYear, dayOfMonth);
		mDatePickerDialog.setCancelable(true);
		mDatePickerDialog.setOnCancelListener(new DismissListener());

		return mDatePickerDialog;
	}

	public DatePickerFragment() {
		super();
	}

	public void setCompareToDate(long date) {
		mCompareToDate = Calendar.getInstance();
		mCompareToDate.setTimeInMillis(date);
		mHoldHourOfDay = mCompareToDate.get(Calendar.HOUR_OF_DAY);
		mHoldMinute= mCompareToDate.get(Calendar.MONTH);
	}

	// the callback received when the user "sets" the time in the dialog
	DatePickerDialog.OnDateSetListener mDateSetListener =
			new DatePickerDialog.OnDateSetListener() {
		@Override
		public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
			if (mDialogCanceled) {
				mDialogCanceled = false;
				return;
			}

			// preset with what's already in there
			// either before or from onDateSet().
			// if not set, use now
			Calendar date = Calendar.getInstance();
			date.setTimeInMillis(mCompareToDate.getTimeInMillis());
			date.set(Calendar.SECOND, 0);
			date.set(Calendar.MILLISECOND, 0);

			if (date.get(Calendar.YEAR) != year
					|| date.get(Calendar.MONTH) != monthOfYear
					|| date.get(Calendar.DAY_OF_MONTH) != dayOfMonth) {
				date.set(Calendar.YEAR, year);
				date.set(Calendar.MONTH, monthOfYear);
				date.set(Calendar.DAY_OF_MONTH, dayOfMonth);
				mDPListener.datePickerDateChanged(date, mRetCode);
			}

			mDPListener.datePickerPostProcess(mRetCode);
		}
	};

	class DismissListener implements DialogInterface.OnCancelListener{
		@Override
		public void onCancel(DialogInterface dialog) {
			mDialogCanceled = true;
			dialog.dismiss();
		}
	}

	class OurDatePickerDialog extends DatePickerDialog {
		public OurDatePickerDialog(Context context, OnDateSetListener callBack,
				int year, int monthOfYear, int dayOfMonth) {

			super(context, callBack, year, monthOfYear, dayOfMonth);
		}

		@Override
		public void onBackPressed() {
			mDialogCanceled = true;
			super.onBackPressed();
		}

		@Override
		protected void onStart() {
			mDialogCanceled = false;
			super.onStart();
		}

		@Override
		protected void onStop() {
			mDialogCanceled = true;
			super.onStop();
		}
	}
}
