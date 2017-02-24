package com.aldersonet.automatonalert.Picker;

import android.content.Context;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DatePickerTimePicker extends DialogFragment
		implements
		DatePickerFragment.IDPListener,
		TimePickerFragment.ITPListener {

	public static final int DATE_DIALOG_ID = 1;
	public static final int TIME_DIALOG_ID = 2;
	public static final int CLEAR_DIALOG_ID = 3;

//	OurDatePickerDialog mDatePicker;
//	OurTimePickerDialog mTimePicker;

	Calendar mCompareToDate;
	Context mContext;
	FragmentActivity mActivity;
	IDPTPListener mDPTPListener;
	boolean mDialogCanceled = false;

//	private int mHoldYear;
//	private int mHoldMonth;
//	private int mHoldDay;

	Calendar mDatePickerDate;
	boolean mDateChangedClientResult;

	static DatePickerTimePicker mDatePickerTimePicker;

	public interface IDPTPListener {
		public boolean dateTimeChanged(Calendar alertDate);
		public void postProcess(boolean dateChangeClientResult);
	}

	public static DatePickerTimePicker showInstance(
			Context context, final FragmentActivity activity,
			long dateMillis, IDPTPListener dptpListener) {

		mDatePickerTimePicker = new DatePickerTimePicker();

		mDatePickerTimePicker.mContext = context;
		mDatePickerTimePicker.mActivity = activity;
		mDatePickerTimePicker.mDPTPListener = dptpListener;
		mDatePickerTimePicker.setCompareToDate(dateMillis);

		DatePickerFragment.showInstance(
				context, activity, new Date(dateMillis), mDatePickerTimePicker, DATE_DIALOG_ID);

		return mDatePickerTimePicker;
	}

	public DatePickerTimePicker() {
		super();
	}

	@Override
	public void onStop() {
		super.onStop();
		if (mDatePickerTimePicker != null) {
			mDatePickerTimePicker = null;
		}
	}

	public void setCompareToDate(long date) {
		mCompareToDate = (Calendar.getInstance());
		mCompareToDate.setTimeInMillis(date);
	}

	@Override
	public boolean datePickerDateChanged(Calendar date, int retCode) {
		mDatePickerDate = date;
		return false;
	}

	@Override
	public void datePickerPostProcess(int retCode) {
		if (mDialogCanceled) {
			return;
		}
		Calendar dateTime = Calendar.getInstance(Locale.getDefault());
		// if user didn't change the date, use now
		if (mDatePickerDate == null) mDatePickerDate = mCompareToDate;

		dateTime.set(
				// new ymd
				mDatePickerDate.get(Calendar.YEAR),
				mDatePickerDate.get(Calendar.MONTH),
				mDatePickerDate.get(Calendar.DAY_OF_MONTH),
				// old hm
				mCompareToDate.get(Calendar.HOUR_OF_DAY),
				mCompareToDate.get(Calendar.MINUTE),
				0);
		dateTime.set(Calendar.MILLISECOND, 0);

		TimePickerFragment.showInstance(
				mContext, mActivity, dateTime.getTime(),
				mDatePickerTimePicker, TIME_DIALOG_ID);
	}

	@Override
	public boolean timePickerTimeChanged(Calendar date, int retCode) {
		mDatePickerDate = date;
		return mDateChangedClientResult = mDPTPListener.dateTimeChanged(date);
	}

	@Override
	public void timePickerPostProcess(int retCode) {
		mDPTPListener.postProcess(mDateChangedClientResult);
	}

}
