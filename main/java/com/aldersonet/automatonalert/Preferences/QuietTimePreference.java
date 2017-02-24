package com.aldersonet.automatonalert.Preferences;

import android.app.TimePickerDialog;
import android.content.Context;
import android.preference.Preference;
import android.support.v4.preference.PreferenceFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TimePicker;

import com.aldersonet.automatonalert.Alert.NotificationItemDO;
import com.aldersonet.automatonalert.SoundBomb.SoundBomb;
import com.aldersonet.automatonalert.R;
import com.aldersonet.automatonalert.Picker.TimePickerFragment;
import com.aldersonet.automatonalert.Util.Utils;

import java.util.Calendar;
import java.util.Locale;

public class QuietTimePreference extends Preference
			implements TimePickerFragment.ITPListener {

	public static final int START_DIALOG = 1;
	public static final int END_DIALOG = 2;

	Context mContext;
	PreferenceFragment mParentFragment;

	Button mQuietTimeStartButton;
	Button mQuietTimeEndButton;
	Button mQuietTimeClearButton;
	CheckBox mQuietTimeDoNotVibrate;

	int mHour;
	int mMinute;

	TimePickerDialog.OnTimeSetListener mQuietTimeStartSetListener =
			new QuietTimeStartSetListener();
	TimePickerDialog.OnTimeSetListener mQuietTimeEndSetListener =
			new QuietTimeEndSetListener();
	DoNotVibrateOnClickListener mDoNotVibrateOnClickListener =
			new DoNotVibrateOnClickListener();

	public QuietTimePreference(Context context, PreferenceFragment parentFragment) {
		super(context);

		mContext = context;
		mParentFragment = parentFragment;
	}

	@Override
	protected View onCreateView(ViewGroup parent) {
		View view = ((LayoutInflater) mContext
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
				.inflate(R.layout.quiet_time_preference, parent, false);

		setViewPointers(view);
		setListeners();
		setQuietTimeButtonText(false/*invalidateViews*/);

		mQuietTimeDoNotVibrate.setChecked(GeneralPrefsDO.isQuietTimeDoNotVibrate());

		return view;
	}

	private void setViewPointers(View view) {
		mQuietTimeStartButton =
				(Button)view.findViewById(R.id.sqt_start_time);
		mQuietTimeEndButton =
				(Button)view.findViewById(R.id.sqt_end_time);
		mQuietTimeClearButton =
				(Button)view.findViewById(R.id.sqt_clear_button);
		mQuietTimeDoNotVibrate =
				(CheckBox) view.findViewById(R.id.sqt_do_not_vibrate);
	}

	private void setListeners() {
		mQuietTimeStartButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onQuietTimeStartClicked();
			}
		});

		mQuietTimeEndButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onQuietTimeEndClicked();
			}
		});

		mQuietTimeClearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				onQuietTimeClearClicked();
			}
		});

		mQuietTimeDoNotVibrate.setOnClickListener(mDoNotVibrateOnClickListener);
	}

	public void onQuietTimeStartClicked() {
		Calendar cal = Calendar.getInstance(Locale.getDefault());

		if (GeneralPrefsDO.getQuietTimeStart() < 0) {
			cal.setTimeInMillis(System.currentTimeMillis());
			cal.set(Calendar.MINUTE, 0);
		}
		else {
			cal.setTimeInMillis(GeneralPrefsDO.getQuietTimeStart());
		}

		mHour = cal.get(Calendar.HOUR_OF_DAY);
		mMinute = cal.get(Calendar.MINUTE);

		TimePickerFragment.showInstance(
				mContext, mParentFragment.getActivity(), cal.getTime(), this, START_DIALOG);

	}

	public void onQuietTimeEndClicked() {
		Calendar cal = Calendar.getInstance(Locale.getDefault());

		if (GeneralPrefsDO.getQuietTimeEnd() < 0) {
			cal.setTimeInMillis(System.currentTimeMillis());
			cal.set(Calendar.MINUTE, 0);
		}
		else {
			cal.setTimeInMillis(GeneralPrefsDO.getQuietTimeEnd());
		}

		mHour = cal.get(Calendar.HOUR_OF_DAY);
		mMinute = cal.get(Calendar.MINUTE);

		TimePickerFragment.showInstance(
				mContext, mParentFragment.getActivity(), cal.getTime(), this, END_DIALOG);
	}

	public void onQuietTimeClearClicked() {
		GeneralPrefsDO.setQuietTimeStart(-1);
		GeneralPrefsDO.setQuietTimeEnd(-1);
		GeneralPrefsDO.save();

		setQuietTimeButtonText(true/*invalidateViews*/);
	}

	class QuietTimeStartSetListener implements TimePickerDialog.OnTimeSetListener {
		@Override
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			Calendar cal = Calendar.getInstance(Locale.getDefault());
			cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
			cal.set(Calendar.MINUTE, minute);

			GeneralPrefsDO.setQuietTimeStart(cal.getTimeInMillis());
			GeneralPrefsDO.save();

			setQuietTimeButtonText(true/*invalidateViews*/);
		}
	}

	class QuietTimeEndSetListener implements TimePickerDialog.OnTimeSetListener {
		@Override
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			Calendar cal = Calendar.getInstance(Locale.getDefault());
			cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
			cal.set(Calendar.MINUTE, minute);

			GeneralPrefsDO.setQuietTimeEnd(cal.getTimeInMillis());
			GeneralPrefsDO.save();

			setQuietTimeButtonText(true/*invalidateViews*/);
		}
	}

	private void setQuietTimeButtonText(boolean invalidateViews) {
		int hours;
		int minutes;

		if (GeneralPrefsDO.getQuietTimeStart() != -1) {
			Calendar cal = Calendar.getInstance(Locale.getDefault());
			cal.setTimeInMillis(GeneralPrefsDO.getQuietTimeStart());
			hours = cal.get(Calendar.HOUR_OF_DAY);
			minutes = cal.get(Calendar.MINUTE);
		}
		else {
			hours = -1;
			minutes = -1;
		}
		mQuietTimeStartButton.setText(
				Utils.getTimeForButtonDisplay(hours, minutes));

		if (GeneralPrefsDO.getQuietTimeEnd() != -1) {
			Calendar cal = Calendar.getInstance(Locale.getDefault());
			cal.setTimeInMillis(GeneralPrefsDO.getQuietTimeEnd());
			hours = cal.get(Calendar.HOUR_OF_DAY);
			minutes = cal.get(Calendar.MINUTE);
		}
		else {
			hours = -1;
			minutes = -1;
		}
		mQuietTimeEndButton.setText(
				Utils.getTimeForButtonDisplay(hours, minutes));

		// basically only invalidateViews if we aren't
		// initializing (which would cause and endless loop...)
		if (invalidateViews) {
			mParentFragment.getListView().invalidateViews();
		}
	}

	class DoNotVibrateOnClickListener implements CheckBox.OnClickListener {
		@Override
		public void onClick(View v) {
			GeneralPrefsDO.setQuietTimeDoNotVibrate(((CheckBox) v).isChecked());
			GeneralPrefsDO.save();
		}
	}

	@Override
	public boolean timePickerTimeChanged(Calendar date, int retCode) {
		switch (retCode) {
			case START_DIALOG:
				mQuietTimeStartSetListener.onTimeSet(
						null, date.get(Calendar.HOUR_OF_DAY), date.get(Calendar.MINUTE));
				return true;
			case END_DIALOG:
				mQuietTimeEndSetListener.onTimeSet(
						null, date.get(Calendar.HOUR_OF_DAY), date.get(Calendar.MINUTE));
				return true;
		}

		return false;

	}

	@Override
	public void timePickerPostProcess(int retCode) {

	}

	public static boolean inQuietTime(boolean isThisATest, NotificationItemDO notificationItem) {
		if (isThisATest) {
			return false;
		}
		// if All Silent, then no matter what, be silent
		if (GeneralPrefsDO.getOverrideVol().equals(GeneralPrefsDO.OverrideVolLevel.SILENT.name())) {
			return true;
		}

		if (notificationItem != null) {
			// if NotificationItemDO ignores quiet time...
			return !notificationItem.isIgnoreGlobalQuietPolicy() && nowIsInQuietTime();
		}
		else {
			return nowIsInQuietTime();
		}

	}

	// also called from outside this class
	public static boolean nowIsInQuietTime() {

		// if quiet time is not defined...
		if (GeneralPrefsDO.getQuietTimeStart() == -1
				|| GeneralPrefsDO.getQuietTimeEnd() == -1) {
			return false;
		}

		// START DATE
		Calendar qtStart = Calendar.getInstance(Locale.getDefault());
		qtStart.setTimeInMillis(GeneralPrefsDO.getQuietTimeStart());

		// END DATE
		Calendar qtEnd = Calendar.getInstance(Locale.getDefault());
		qtEnd.setTimeInMillis(GeneralPrefsDO.getQuietTimeEnd());

		// NOW
		Calendar now = Calendar.getInstance(Locale.getDefault());
		int cNowYear  = now.get(Calendar.YEAR);
		int cNowMonth = now.get(Calendar.MONTH);
		int cNowDate = now.get(Calendar.DATE);

		// START DATE = now & quiet time start HOUR & MINUTE
		qtStart.set(cNowYear, cNowMonth, cNowDate,
				qtStart.get(Calendar.HOUR_OF_DAY), qtStart.get(Calendar.MINUTE), 0);
		qtStart.set(Calendar.MILLISECOND, 0);

		// END DATE = now & quiet time end HOUR & MINUTE
		qtEnd.set(cNowYear, cNowMonth, cNowDate,
				qtEnd.get(Calendar.HOUR_OF_DAY), qtEnd.get(Calendar.MINUTE), 0);
		qtEnd.set(Calendar.MILLISECOND, 0);

		Log.d(SoundBomb.TAG + ".nowIsInQuietTime()",
				"cStart[" + qtStart.getTime().toString()
						+ "], cEnd[" + qtEnd.getTime().toString() + "]");

		if (qtEnd.before(qtStart)) {
			// spans two days...just need to switch start and end
			// and if we end up outside then we're in quiet time
			Calendar qtTemp = qtStart;
			qtStart = qtEnd;
			qtEnd = qtTemp;

			Log.d(SoundBomb.TAG + ".nowIsInQuietTime()",
					"end<start: returning: [" + (now.before(qtStart) || now.after(qtEnd)) + "]");

			return (now.before(qtStart) || now.after(qtEnd));
		}
		else {
			Log.d(SoundBomb.TAG + ".nowIsInQuietTime()",
					"end>=start: returning: [" + (!(now.before(qtStart) || now.after(qtEnd))) + "]");

			// start < end: outside means we're not in quiet time.
			// note: this code is just copied from above and !not'd
			return !(now.before(qtStart) || now.after(qtEnd));
		}
	}

}
