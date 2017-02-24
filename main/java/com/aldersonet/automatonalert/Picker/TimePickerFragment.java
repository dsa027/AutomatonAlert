package com.aldersonet.automatonalert.Picker;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;

import java.util.Calendar;
import java.util.Date;

public class TimePickerFragment extends DialogFragment {

	Calendar mCompareToDate;
	Context mContext;
	ITPListener mTPListener;
	int mRetCode;

	TimePickerDialog mTimePickerDialog;
	boolean mDialogCanceled = false;

	int mHoldMonthOfYear;
	int mHoldDayOfMonth;
	int mHoldYear;

	public interface ITPListener {
		public boolean timePickerTimeChanged(Calendar date, int retCode);
		public void timePickerPostProcess(int retCode);
	}

	public static TimePickerFragment showInstance(
			Context context, final FragmentActivity activity,
			Date date, ITPListener tpListener, int retCode) {

		final TimePickerFragment dialog = new TimePickerFragment();
		dialog.mContext = context;
		dialog.mTPListener = tpListener;
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
		int hour = mCompareToDate.get(Calendar.HOUR_OF_DAY);
		int min = mCompareToDate.get(Calendar.MINUTE);

		mTimePickerDialog =
				new OurTimePickerDialog(
						getActivity(), mTimeSetListener, hour, min, false);
		mTimePickerDialog.setCancelable(true);
		mTimePickerDialog.setOnCancelListener(new DismissListener());

		return mTimePickerDialog;
	}

	public TimePickerFragment() {
		super();
	}

	public void setCompareToDate(long date) {
		mCompareToDate = Calendar.getInstance();
		mCompareToDate.setTimeInMillis(date);
		mHoldYear = mCompareToDate.get(Calendar.YEAR);
		mHoldMonthOfYear = mCompareToDate.get(Calendar.MONTH);
		mHoldDayOfMonth = mCompareToDate.get(Calendar.DAY_OF_MONTH);
	}

	// the callback received when the user "sets" the time in the dialog
	TimePickerDialog.OnTimeSetListener mTimeSetListener =
			new TimePickerDialog.OnTimeSetListener() {
		@Override
		public void onTimeSet(android.widget.TimePicker view, int hourOfDay, int minute) {
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

			if (date.get(Calendar.HOUR_OF_DAY) != hourOfDay
					|| date.get(Calendar.MINUTE) != minute) {
				date.set(Calendar.HOUR_OF_DAY, hourOfDay);
				date.set(Calendar.MINUTE, minute);
				mTPListener.timePickerTimeChanged(date, mRetCode);
			}

			mTPListener.timePickerPostProcess(mRetCode);
		}
	};

	class DismissListener implements DialogInterface.OnCancelListener{
		@Override
		public void onCancel(DialogInterface dialog) {
			mDialogCanceled = true;
			dialog.dismiss();
		}
	}

	class OurTimePickerDialog extends TimePickerDialog {
		public OurTimePickerDialog(Context context, OnTimeSetListener callBack,
				int hourOfDay, int minute, boolean is24HourView) {

			super(context, callBack, hourOfDay, minute, is24HourView);
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
