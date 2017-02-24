package com.aldersonet.automatonalert.Alarm;

import android.content.Intent;
import android.net.Uri;

import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Alarm.AlarmPendingIntent.ApiSubType;
import com.aldersonet.automatonalert.Alert.AlertIntentExtrasChecker;
import com.aldersonet.automatonalert.Alert.AlertItemDO;
import com.aldersonet.automatonalert.Alert.AlertItems;
import com.aldersonet.automatonalert.Activity.AlertListActivity.FragmentTypeAL;

import java.util.Date;

public class AlarmRepeat {

	AlertItemDO mAlertItem;
	int mAlertItemId;
	int mNotificationItemId;
	long mRepeatEvery;
	long mStopAfter;
	long mCurrentIteration;
	Date mDateRemind;
	long mDateRemindMillis;

	Intent mIntent;
	String mAction;

	public AlarmRepeat(Intent intent, String action) {
		init();
		mIntent = intent;
		mAction = action;

		if (mIntent == null) {
			mAlertItemId = -1;
		}
		else {
			mAlertItemId = mIntent.getIntExtra(
					AlertItemDO.TAG_ALERT_ITEM_ID, -1);
		}

		if (mAlertItemId != -1) {
			if ((mAlertItem = AlertItems.get(mAlertItemId)) != null) {
				mNotificationItemId = mAlertItem.getNotificationItemId();
				mRepeatEvery = mAlertItem.getRepeatEvery();
				mStopAfter = mAlertItem.getStopAfter();
				mDateRemind = mAlertItem.getDateRemind();
				mDateRemindMillis = mDateRemind == null ? 0 : mDateRemind.getTime();
			}
		}
	}

	private void init() {
		mAlertItem = null;
		mAlertItemId = -1;
		mNotificationItemId = -1;
		mRepeatEvery = 0;
		mStopAfter = 0;
		mCurrentIteration = -1;
		mDateRemind = null;
		mDateRemindMillis = -1;
	}

	public boolean isDoItAgain() {
		if (mIntent == null) {
			return false;
		}

		AlertIntentExtrasChecker ok =
				new AlertIntentExtrasChecker(mIntent, mAction);

		if (!ok.intentExtrasOk(false/*checkAction*/)) return false;

		return !AlertIntentExtrasChecker.isThisASnooze(mIntent.getDataString());

	}

	public boolean reSendAlarm() {
		if (mAlertItem == null
				|| mDateRemind == null) {
			return false;
		}
		// there should never be one, but just in case, get rid of
		// competing repeat
		mAlertItem.findCancelRemovePendingIntentsPostAlarms(ApiSubType.REPEAT);

		Intent intent = AlertItemDO.setAlarmAlertIntent(
				mAlertItemId,
				mNotificationItemId,
				AlertItemDO.getFragmentTypeFromAlertItem(mAlertItem),
				mDateRemind.getTime(),
				mRepeatEvery,
				mStopAfter);

		intent.setData(
				Uri.parse(
						intent.getDataString() + "|" + AutomatonAlert.REPEAT));

		// alarms - cancel REPEAT only
		AlarmPendingIntent api = AlertItemDO.setApi(
				intent, AlertItemDO.getRequestCode(mAlertItem, FragmentTypeAL.REPEATS),
				mAlertItem, ApiSubType.REPEAT);

		return AlertItemDO.setTheAlarm(mAlertItem, api);
	}


}
