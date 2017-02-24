package com.aldersonet.automatonalert.Fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.aldersonet.automatonalert.Activity.SetAlertActivity;
import com.aldersonet.automatonalert.Alarm.AlarmPendingIntent;
import com.aldersonet.automatonalert.Alert.AlertItemDO;
import com.aldersonet.automatonalert.Alert.AlertItems;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.PostAlarm.PostAlarmDO;
import com.aldersonet.automatonalert.R;
import com.aldersonet.automatonalert.Picker.DatePickerTimePicker;
import com.aldersonet.automatonalert.Util.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

@SuppressLint("ValidFragment")
public class SetAlarmMasterFragment extends Fragment
		implements
		IRTMaster,
		DatePickerTimePicker.IDPTPListener {

	public static final String TAG = "SetAlarmRTUpdateFragment";
	public static final String TAG_ALERT_ITEM_ID = "alertItemId";
	public static final String SET_DATE_LABEL = "Set Date and Time";

	private int mHour = -1;
	private int mMinute = -1;
	private int mYear = -1;
	private int mMonth = -1;
	private int mDay = -1;

	DatePickerTimePicker mPicker;

	// Set Alarm
	LinearLayout mSetAlarmMainLayout;
	Button mSetAlarmDateTime;
	TextView mSetAlarmRepeatEveryHeader;
	TextView mSetAlarmStopAfterHeader;
	TextView mSetAlarmShowAlarmScreenHeader;
	Spinner mSetAlarmRepeatEvery;
	Spinner mSetAlarmStopAfter;
	Switch mSetAlarmShowAlarmScreen;

	AlertItemDO mAlertItem;
	int mAlertItemId;

	boolean mSetAlarm;
	boolean mInitializingSetAlarmRepeatEverySpinner = false;
	boolean mInitializingSetAlarmStopAfterSpinner = false;
	boolean mSetAlarmDateTimeInitialized = false;

	View mTopView;
	RTUpdateFragment mRTListener;

	boolean mIsInitialErrorCheckDone;

	public SetAlarmMasterFragment() {
		mIsInitialErrorCheckDone = false;
	}

	public boolean isInitialErrorCheckDone() {
		return mIsInitialErrorCheckDone;
	}

	public void setInitialErrorCheckDone() {
		mIsInitialErrorCheckDone = true;
	}

    public static SetAlarmMasterFragment newInstance(int alertItemId) {
        SetAlarmMasterFragment fragment = new SetAlarmMasterFragment();

	    Bundle bundle = new Bundle();
	    bundle.putInt(TAG_ALERT_ITEM_ID, alertItemId);
	    fragment.setArguments(bundle);

        return fragment;
    }

	public void setRTListener(RTUpdateFragment listener) {
		mRTListener = listener;
	}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

	    Bundle bundle = getArguments();
	    mAlertItemId = bundle.getInt(TAG_ALERT_ITEM_ID);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View returnView =
		        inflater.inflate(R.layout.set_alert_master_fragment, container, false);

	    mTopView = returnView;
	    setViewPointers(returnView);
	    setInitialDbRecs();
	    setViewComponents(returnView);

	    return returnView;
    }

	@Override
	public Spanned getNoAccountsErrorText() {
		return Html.fromHtml("");
	}

	private void setYmdhm(int year, int month, int day, int hour, int minute) {
		mYear = year;
		mMonth = month;
		mDay = day;
		mHour = hour;
		mMinute = minute;
	}

	public void setYmdhm(Calendar alertDate) {
		setYmdhm(alertDate.get(Calendar.YEAR),
				alertDate.get(Calendar.MONTH),
				alertDate.get(Calendar.DATE),
				alertDate.get(Calendar.HOUR_OF_DAY),
				alertDate.get(Calendar.MINUTE));
	}

	@Override
	public void setViewPointers(View v) {
		mSetAlarmMainLayout = (LinearLayout)v.findViewById(R.id.sawru_alarm_layout);
		mSetAlarmDateTime = (Button)v.findViewById(R.id.sawru_datetime);
		mSetAlarmRepeatEveryHeader = (TextView)v.findViewById(R.id.sawru_repeat_every_header);
		mSetAlarmStopAfterHeader = (TextView)v.findViewById(R.id.sawru_stop_after_header);
		mSetAlarmShowAlarmScreenHeader = (TextView)v.findViewById(R.id.sawru_show_alarm_screen_header);
		mSetAlarmRepeatEvery = (Spinner)v.findViewById(R.id.sawru_repeat_every);
		mSetAlarmStopAfter = (Spinner)v.findViewById(R.id.sawru_stop_after);
		mSetAlarmShowAlarmScreen = (Switch)v.findViewById(R.id.sawru_show_alarm_screen);
	}

	@Override
	public void setInitialDbRecs() {
		mAlertItem = null;
		if (mAlertItemId != -1) {
			mAlertItem = AlertItems.get(mAlertItemId);
		}
		if (mAlertItem == null) {
			mAlertItem = new AlertItemDO();
		}
	}

	@Override
	public void setViewComponents(View v) {
		// order matters
		setMasterViewToDefaults();
		setViewDefaults();
		setMasterFieldHint();
		setMasterListeners();
	}

	@Override
	public void setMasterViewToDefaults() {
		if (mSetAlarmDateTime != null) {
			// only populate with defaults if there's no data in NotificationItemDO
			if (mRTListener.mNotificationItem.getNotificationItemId() == -1) {
				mSetAlarmDateTime.setText(null);
				initializeNewMaster();
			}
		}
		enableDisableMasterFields();
	}

	public void setViewDefaults() {
		// make sure there's an mAlertItem
		if (mAlertItem == null) {
			setInitialDbRecs();
		}
		// set Phrase
		setMasterField(
				getSetAlarmDateTimeText(), mAlertItem.getDateRemind(), false);

		setSpinnersAndButtons();
	}

	private void setSpinnersAndButtons() {
		setSetAlarmRepeatEverySpinner();
		setSetAlarmStopAfterSpinner();
	}

	@Override
	public void setMasterFieldHint() {
		mSetAlarmDateTime.setHint("Set Alarm Date and Time");
	}

	@Override
	public void setMasterField(
			String text, Object oDate, boolean skipKeyFieldEqualCheck) {

		Date date = (Date)oDate;

		// if this isn't the first time
		if (mSetAlarmDateTimeInitialized &&
				!skipKeyFieldEqualCheck) {
			// return immediately if nothing's changed
			if (date == null) {
				date = new Date(0);
			}
			if (!isMasterFieldChanged(text, date.getTime())) {
				return;
			}
		}

		if (mSetAlarmDateTime != null) {
			updateMasterFieldAndViews(text);
		}

		if (mSetAlarmDateTimeInitialized) {
			// delete RT
			if (TextUtils.isEmpty(text)) {
				deleteMaster();
				return;////////
			}

			mAlertItem.save();
//			mRTListener.mNotificationItem.save();
		}

		// if we're coming in with a notificationItem already set
		// in mAlertItem, then it needs to show, otherwise, keep
		// it empty
		if (mSetAlarmDateTimeInitialized
				|| !mSetAlarmDateTimeInitialized
						&& mAlertItem.getNotificationItemId() != -1) {
			mRTListener.setRingtoneFromNotificationItem(true);
		}

		if (!mSetAlarmDateTimeInitialized) {
			mSetAlarmDateTimeInitialized = true;
		}

		// alpha-dim ringtone-required fields if there's no ringtone.
		// only sets non-phone.
		enableDisableMasterFields();
	}

	@Override
	public boolean isMasterFieldChanged(String text, Object oDate) {
		long date = (Long)oDate;

		String textNow = (mSetAlarmDateTime == null) ?
				"SomeTextThatIsNotTheSame"
				: mSetAlarmDateTime.getText().toString();

		Date dateRemindNow = mAlertItem.getDateRemind();
		if (textNow.equals(text)) {
			if (dateRemindNow == null) {
				if (date <= 0) {
					return false;
				}
			}
			else {
				if (dateRemindNow.equals(new Date(date))) {
					return false;
				}
			}
		}
		return true;
	}

	private String getSetAlarmDateTimeText() {
		if (mAlertItem.getDateRemind() == null) {
			setYmdhm(-1, -1, -1, -1, -1);
		}
		else {
			Calendar cal = Calendar.getInstance(Locale.getDefault());
			cal.setTime(mAlertItem.getDateRemind());
			setYmdhm(cal);
		}
		String date = Utils.getDateForButtonDisplay(mYear, mMonth, mDay);
		String time = Utils.getTimeForButtonDisplay(mHour, mMinute);
		String dateTime = date + " - " + time;

		if (date.equals(SET_DATE_LABEL)) {
			dateTime = "";
		}
		return dateTime;
	}

	private void setSetAlarmRepeatEverySpinner() {
		mInitializingSetAlarmRepeatEverySpinner = true;

		mRTListener.setSpinnerSelection(
				mSetAlarmRepeatEvery,
				"" + mAlertItem.getRepeatEvery(),
				R.array.alarm_repeat_values);

		mSetAlarmRepeatEvery.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
			                           int position, long id) {

				String s = getResources().getStringArray(
						R.array.alarm_repeat_values)[position];
				long l = Utils.getLong(s, 24 * 60 * 60 * 1000); // day
				mAlertItem.setRepeatEvery(l);
				if (!mInitializingSetAlarmRepeatEverySpinner) {
					mAlertItem.save();
					setOrCancelRepeatIfNeeded();
				}
				mInitializingSetAlarmRepeatEverySpinner = false;
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
	}

	void setOrCancelRepeatIfNeeded() {
		// if already had the alarm, see if we need to set repeat alarm
		long now = System.currentTimeMillis();

		if (mAlertItem.getRepeatEvery() <= 0
				|| mAlertItem.getStopAfter() <= 0
				|| mAlertItem.getDateRemind() == null) {
			// cancel repeat alarm if there is one
			AutomatonAlert.getAPIs().findCancelRemovePendingIntentsPostAlarms(
					AlarmPendingIntent.ApiType.ALERT, AlarmPendingIntent.ApiSubType.REPEAT, -1,
					mAlertItem.getAlertItemId(), mAlertItem.getNotificationItemId());
		}

		// if dateRemind is in the past, check to see
		// if we need to set repeat alarm
		else if (mAlertItem.getDateRemind().getTime() < now) {
			// add repeat alarm
			long next = mAlertItem.getNextIteratedAlarm();
			if (next > now) {
				mAlertItem.setAlarm(AlarmPendingIntent.ApiSubType.REPEAT);
			}
		}
	}

	private void setSetAlarmStopAfterSpinner() {
		mInitializingSetAlarmStopAfterSpinner = true;
		mRTListener.setSpinnerSelection(
				mSetAlarmStopAfter,
				"" + mAlertItem.getStopAfter(),
				R.array.alarm_stop_after_values);

		mSetAlarmStopAfter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
			                           int position, long id) {

				String s = getResources().getStringArray(
						R.array.alarm_stop_after_values)[position];
				long l = Utils.getLong(s, 0); // don't repeat
				mAlertItem.setStopAfter(l);
				if (!mInitializingSetAlarmStopAfterSpinner) {
					mAlertItem.save();
					setOrCancelRepeatIfNeeded();
				}
				mInitializingSetAlarmStopAfterSpinner = false;
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
	}

	@Override
	public void deleteMaster() {
		// alarms - remove ALL (this is an alarm)
		mAlertItem.findCancelRemovePendingIntentsPostAlarms(AlarmPendingIntent.ApiSubType.ALARM);
		initializeNewMaster();
		setYmdhm(-1, -1, -1, -1, -1);
		mAlertItem.updateDateRemind(null, true/*save*/);
		updateMasterFieldAndViews(getSetAlarmDateTimeText());
		enableDisableMasterFields();

//		mSetAlarmClear.setVisibility(Button.GONE);
		setMasterViewToDefaults();
		setViewComponents(mTopView);
	}

	@Override
	public void updateMasterFieldAndViews(String dateTime) {
		mSetAlarmDateTime.setText(dateTime);
	}

	@Override
	public void setMasterListeners() {
		mSetAlarmDateTime.setOnClickListener(new SetControllerFieldListener());
		mSetAlarmShowAlarmScreen.setOnClickListener(new SetAlarmShowAlarmScreenListener());
		mSetAlarmShowAlarmScreen.setChecked(
				!mRTListener.mNotificationItem.isNoAlertScreen());
	}

	@Override
	public void enableDisableMasterFields() {
		if (TextUtils.isEmpty(mSetAlarmDateTime.getText().toString())) {
			setAlphaMasterFields(.3f);
			enableDisableMasterFields(false);
		}
		else {
			setAlphaMasterFields(1f);
			enableDisableMasterFields(true);
		}
		mRTListener.showHidePlayFor();
	}

	class SetAlarmShowAlarmScreenListener implements Switch.OnClickListener {
		@Override
		public void onClick(View v) {
			boolean checked = ((Switch)v).isChecked();
			mRTListener.mNotificationItem.setNoAlertScreen(!checked);
			mRTListener.mNotificationItem.save();
			mRTListener.showHidePlayFor();
		}
	}

	class SetControllerFieldListener implements Button.OnClickListener {
		@Override
		public void onClick(View v) {
			Calendar cal = Calendar.getInstance();
			if (mAlertItem.getDateRemind() != null) {
				cal.setTime(mAlertItem.getDateRemind());
			}
			setYmdhm(cal);

			clearMaster(
					DatePickerTimePicker.DATE_DIALOG_ID, false/*deleteRT*/);
		}
	}

	@Override
	public void enableDisableMasterFields(boolean enable) {
		mSetAlarmRepeatEveryHeader.setEnabled(enable);
		mSetAlarmStopAfterHeader.setEnabled(enable);
		mSetAlarmShowAlarmScreenHeader.setEnabled(enable);
		mSetAlarmRepeatEvery.setEnabled(enable);
		mSetAlarmStopAfter.setEnabled(enable);
		mSetAlarmShowAlarmScreen.setEnabled(enable);
	}

	@Override
	public void setAlphaMasterFields(float f) {
		mSetAlarmRepeatEveryHeader.setAlpha(f);
		mSetAlarmStopAfterHeader.setAlpha(f);
		mSetAlarmShowAlarmScreenHeader.setAlpha(f);
		mSetAlarmRepeatEvery.setAlpha(f);
		mSetAlarmStopAfter.setAlpha(f);
		mSetAlarmShowAlarmScreen.setAlpha(f);
	}

	@Override
	public void initializeNewMaster() {
		mAlertItem.setRepeatEvery(-1L);
		mAlertItem.setStopAfter(0L);
	}

	@Override
	public void addDefaultAccounts() {

	}

	private Spanned getNoEmailAccountErrorText() {
		return Html.fromHtml("");
	}

	@Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

	public void initMaster() {
//		clearSetAlarmDateTimeViewsAndDateRemind();
		initializeNewMaster();
		setMasterField("", null, true/*skipSetAlarmEqualCheck*/);
	}

	@Override
	public boolean/*areThereSnoozes*/ clearMaster(Object ... obj) {
			//final int dateTimeDialogId, boolean deleteRT) {

		final boolean isDeleteRT = (obj.length >= 2) ? (Boolean)obj[1] : false;
		int dateTimeDialogId = (obj.length >= 1) ? (Integer)obj[0] : -1;
		if (dateTimeDialogId == -1) {
			return false/*are there snoozes*/;
		}

		// get snooze - PASSTHRU if no snoozes
		final ArrayList<PostAlarmDO> postAlarms = PostAlarmDO.get(
				mAlertItem.getAlertItemId(),
				mAlertItem.getNotificationItemId());
		// No SNOOZES
		if (postAlarms.size() <= 0) {
			// CLEAR
			if (dateTimeDialogId == DatePickerTimePicker.CLEAR_DIALOG_ID) {
				if (isDeleteRT) {
					// pending delete via mClearButton
					mRTListener.clearRT();
				}
				initMaster();
			}
			// SET DATE/TIME
			else {
				mPicker = showDatePicker();
			}
			return false/*areThereSnoozes*/;
		}

		// Have SNOOZES
		showClearSnoozedAlarmDialog(postAlarms, dateTimeDialogId, isDeleteRT);

		return true/*areThereSnoozes*/;
	}

	private void showClearSnoozedAlarmDialog(
			final ArrayList<PostAlarmDO> postAlarms,
			final int dateTimeDialogId, final boolean isDeleteRT){

		// SNOOZES
		final long snooze = postAlarms.get(0).getNextAlarm();

		// ask to delete snoozes
		AlertDialog.Builder builder =
				new AlertDialog.Builder(getActivity())
						.setTitle("About to Clear a Snoozed Alarm")
						.setMessage("Modifying this alarm will clear a snoozed alarm set for "
								+ Utils.toLocaleString(snooze) + ".  Is that ok?")
						.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
								// delete first
								for (PostAlarmDO postAlarm : postAlarms) {
									postAlarm.cancelPostAlarm();
								}
								// CLEAR
								if (dateTimeDialogId == DatePickerTimePicker.CLEAR_DIALOG_ID) {
									if (isDeleteRT) {
										mRTListener.clearRT();
									}
									initMaster();
								}
								// SET DATE/TIME
								else {
									mPicker = showDatePicker();
								}
							}
						})
						.setNegativeButton(
								AutomatonAlert.CANCEL_LABEL, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
							}
						});
		// show it
		AlertDialog alertDialog = builder.create();
		alertDialog.show();
	}

	private DatePickerTimePicker showDatePicker() {
		return DatePickerTimePicker.showInstance(
				getActivity().getApplicationContext(),
				getActivity(),
				Utils.getDateRemindOrNow(mAlertItem).getTime(),
				this);
	}

	// called only for SetAlarm...
	private void saveAlertItem() {
		if (mAlertItem != null
				&& mAlertItem.isDirty) {
			mAlertItem.save();
		}
	}

	@Override
	public void saveMasterNotificationItemId(int id) {
		mAlertItem.setNotificationItemId(id);
		mAlertItem.save();

	}

	private void setAlarm() {
//		setStopAfter();
		saveAlertItem();
		mRTListener.saveNotificationItem();

		// set/reset the alarm if it's valid (AlertItemDO will check)
		if ((mSetAlarmDateTime != null
				&& mSetAlarmDateTime.getText().toString().equals(SET_DATE_LABEL))) {
			// date/time not complete, don't set alarm.
		}
		else {
			mAlertItem.setAlarm(AlarmPendingIntent.ApiSubType.ALARM);
		}
	}

	private void processDateTimeChanged(Calendar alertDate) {
		mSetAlarm = false;
		if (alertDate.before(Calendar.getInstance())) {
			SetAlertActivity.showRequireOnlyFutureAlarmsDialog(getActivity());
			// reset to show initial state
			if (mAlertItem.getDateRemind() == null) {
				setYmdhm(-1, -1, -1, -1, -1);
			}
			mSetAlarm = false;
		}
		else {
			setYmdhm(
					alertDate.get(Calendar.YEAR),
					alertDate.get(Calendar.MONTH),
					alertDate.get(Calendar.DATE),
					alertDate.get(Calendar.HOUR_OF_DAY),
					alertDate.get(Calendar.MINUTE));
			mAlertItem.updateDateRemind(alertDate.getTime(), true/*save*/);
			mSetAlarm = true;
		}
	}

	private void processDateTimeSet() {
		if (mSetAlarm) {
			// if no RT, get a new one (new NotificationItemDO).
			// Must be done before setAlarm otherwise there'll
			// be nothing to set (no NotificationItemDO)
			if (TextUtils.isEmpty(mRTListener.mRingtone.getText().toString())) {
				mRTListener.setRingtoneFromNotificationItem(true/*createDefaultOnEmpty*/);
			}
			setAlarm();
			updateMasterFieldAndViews(getSetAlarmDateTimeText());
			enableDisableMasterFields();
			Utils.showSetAlarmToast(
					getActivity(), mAlertItem.getDateRemind().getTime(), false/*snooze*/);
		}
		else {
			// make sure mYear, mMonth, mDay... are reset
			if (mAlertItem.getDateRemind() != null) {
				Calendar cal = Calendar.getInstance(Locale.getDefault());
				cal.setTime(mAlertItem.getDateRemind());
				setYmdhm(cal);
			}
			else {
				setYmdhm(-1, -1, -1, -1, -1);
			}
		}
		if (getActivity() instanceof SetAlertActivity) {
			((SetAlertActivity)getActivity()).setAlertItem(mAlertItem);
		}
	}

	// where we arrive at after SetAlarm
	@Override
	public boolean dateTimeChanged(Calendar alertDate) {
		setYmdhm(alertDate);
		processDateTimeChanged(alertDate);
		return mSetAlarm;
	}

	@Override
	public void postProcess(boolean dateChangeClientResult) {
		mSetAlarm = dateChangeClientResult;
		processDateTimeSet();
	}
}
