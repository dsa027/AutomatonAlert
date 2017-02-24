package com.aldersonet.automatonalert.Alert;

import android.content.Intent;

import com.aldersonet.automatonalert.Activity.AlarmVisualActivity;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Provider.AutomatonAlertProvider;
import com.aldersonet.automatonalert.Util.Utils;

import java.util.Date;

public class AlertIntentExtrasChecker {

	public static final String TAG_ORIGINAL_ACTION = "originalAction";

	private AlertItemDO mAlertItem;
	private int mNotificationItemId;
	private long mDateRemindMillis;
	private long mRepeatEvery;
	private long mStopAfter;

	private Date mDateRemind;
	private String mData;

	private Intent mIntent;
	private String mAction;

	private int mCurrentIteration;

	public AlertIntentExtrasChecker(Intent intent, String action) {
		super();
		init(intent, action);
	}

	private void init(Intent intent, String action) {
		mIntent = intent;
		mAction = action;

		if (mIntent == null) {
			return;
		}
		if (mAction == null) {
			mAction = "";
		}

		mData = mIntent.getDataString();
		getAlertItem(mIntent);
		getNotificationItem(mIntent);

		if (mAlertItem != null) {
			if (mAlertItem.getDateRemind() != null) {
				mDateRemindMillis = Utils.getDateRemindLong(mAlertItem.getDateRemind());
			}
			else {
				mDateRemindMillis = 0;
			}
			mDateRemind = mAlertItem.getDateRemind();
			mRepeatEvery = mAlertItem.getRepeatEvery();
			mStopAfter = mAlertItem.getStopAfter();
			// get AlertItem fields
			mDateRemindMillis = mIntent.getLongExtra(
					AutomatonAlertProvider.ALERT_ITEM_DATE_REMIND, -1);
			if (mDateRemindMillis > 0) {
				mDateRemind = new Date(mDateRemindMillis);
			}
			else {
				mDateRemind = null;
			}
			mRepeatEvery = mIntent.getLongExtra(AutomatonAlertProvider.ALERT_ITEM_REPEAT_EVERY, -1);
			mStopAfter = mIntent.getLongExtra(AutomatonAlertProvider.ALERT_ITEM_STOP_AFTER, -1);
		}

		mCurrentIteration =
				AlertItemDO.getCurrentAlarmIteration(
						mDateRemind, mRepeatEvery);
	}

	public static boolean isThisASnooze(String data) {
		return data != null && (data.contains("|" + AutomatonAlert.SNOOZE));
	}

	public static boolean isThisATest(String data) {
		return data != null && data.contains(
				AlarmVisualActivity.TEST_ALARM + "|" + AlarmVisualActivity.TEST_ALARM);
	}

	public static boolean isThisARepeat(AlertItemDO alertItem) {
		if (alertItem == null
				|| alertItem.getDateRemind() == null) {
			return false;
		}
		// dateRemind must be in the past and there must
		// be a future repeat alarm
		long curr =
				AlertItemDO.getCurrentAlarmIteration(
						alertItem .getDateRemind(),
						alertItem.getRepeatEvery());
		// reminder hasn't gone off yet
		return curr > 0
				&& alertItem.getLastIteratedAlarm()
						>= AlertItemDO.cutOffSeconds(System.currentTimeMillis());

	}

	private boolean isRepeatOk() {
		if (mDateRemind != null
				&& mDateRemindMillis > 0
				&& mRepeatEvery > 0
				&& mStopAfter > 0) {

			// if we're done repeating and only know it now, drop the alarm
			if (mCurrentIteration > mStopAfter) {
				return false;
			}

			// if we're done based on 1st alarm + (repeatEvery * stopAfter)
			// and only know it now, drop the alarm.  Not using
			// (stopAfter - 1) because of possible latency

			long last = mDateRemindMillis + (mRepeatEvery * mStopAfter);
			long now = AlertItemDO.cutOffSeconds(System.currentTimeMillis());
			if (last < now) {
				return false;
			}
		}
		return true;
	}

	public boolean intentExtrasOk(boolean checkAction) {
		if (checkAction) {
			if (!mAction.equals(AutomatonAlert.ALARM_ALERT_EVENT)) {
				return false;
			}
		}

		// if key items were missing, drop the alarm
		if (mNotificationItemId == -1) {
			return false;
		}

		if (!isThisASnooze(mData)) {
			if (!isRepeatOk()) {
				return false;
			}
		}

		if (mAction.equals(AutomatonAlert.ALARM_ALERT_EVENT)) {
			mIntent.putExtra(TAG_ORIGINAL_ACTION, mAction);
		}

		return true;
	}

	private void getAlertItem(Intent intent) {
		if (intent == null) {
			return;
		}
		int alertItemId = intent.getIntExtra(AlertItemDO.TAG_ALERT_ITEM_ID, -1);

		mAlertItem = null;
		if (alertItemId != -1) {
			mAlertItem = AlertItems.get(alertItemId);
		}
	}

	private void getNotificationItem(Intent intent) {
		if (intent == null) {
			return;
		}
		mNotificationItemId =
				intent.getIntExtra(NotificationItemDO.TAG_NOTIFICATION_ITEM_ID, -1);
	}

}
