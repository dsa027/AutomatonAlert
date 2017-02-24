package com.aldersonet.automatonalert.Alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.aldersonet.automatonalert.Alert.AlertItemDO;
import com.aldersonet.automatonalert.Alert.AlertItems;
import com.aldersonet.automatonalert.AutomatonAlert;

import java.util.Comparator;

public class AlarmPendingIntent {

	// fields that make this unique
	public ApiType mApiType;
	public ApiSubType mApiSubType;
	public int mAccountId;
	public int mAlertItemId;
	public int mNotificationItemId;
	public long mAlarmTime;

	public int mRequestCode;
	public Intent mIntent;
	public int mFlags;

	public PendingIntent mPendingIntent;

	public AlarmManager mAlarmManager;

	public enum ApiType {
		ALERT,
		EMAIL_POLL,
		GC_POLL,
	}

	public enum ApiSubType {
		ALARM,
		SNOOZE,
		REPEAT,
		REMINDER,
		NONE
	}

	public AlarmPendingIntent(
			ApiType apiT, ApiSubType subType, int accountId, int alertItemId,
			int notificationItemId,	int requestCode, Intent intent, int flags) {

		mApiType = apiT;
		mApiSubType = subType;
		mAccountId = accountId;
		mAlertItemId = alertItemId;
		mNotificationItemId = notificationItemId;
		mAlarmTime = -1;

		mRequestCode = requestCode;
		mIntent = intent;
		mFlags = flags;

		Context context = AutomatonAlert.THIS.getApplicationContext();
		mAlarmManager = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

		mPendingIntent = PendingIntent.getBroadcast(context, mRequestCode, mIntent, flags);
	}

	private static void setSystemAlarm(
			AlarmManager am, int type, long millisTime,
			PendingIntent pendingIntent) {

        if (am == null) {
            return;
        }

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			am.setExact(type, millisTime, pendingIntent);
		}
		else {
			am.set(type, millisTime, pendingIntent);
		}
	}

	private static synchronized PendingIntent setTheAlarm(
			long millisTime, AlarmPendingIntent api) {

		PendingIntent pendingIntent = api.mPendingIntent;

		// what's being sent in, api, is what needs
		// canceling, so that will determine cancel
		// ALARM, SNOOZE, REPEAT
		AutomatonAlert.getAPIs().findCancelRemovePendingIntentsPostAlarms(api);

		if (millisTime > System.currentTimeMillis()) {
			api.mAlarmTime = millisTime;
			setSystemAlarm(
					api.mAlarmManager, AlarmManager.RTC_WAKEUP,
					millisTime, pendingIntent);
			AutomatonAlert.getAPIs().add(api);
		}

		return pendingIntent;
	}

	// used only for EMAIL_POLL and GC_POLL
	private static synchronized PendingIntent setTheRepeatingAlarm(
			int mode, long repeat, AlarmPendingIntent api) {

		PendingIntent pendingIntent = api.mPendingIntent;

		AutomatonAlert.getAPIs().findCancelRemovePendingIntentsPostAlarms(api);

		long millisTime = System.currentTimeMillis() + repeat;

		api.mAlarmTime = millisTime;
		api.mAlarmManager.setRepeating(mode, millisTime, repeat, pendingIntent);
		AutomatonAlert.getAPIs().add(api);

		return pendingIntent;
	}

	public PendingIntent setAlarm(long millisTime) {
		return setTheAlarm(millisTime, this);
	}

	public PendingIntent setRepeatingAlarm(int mode, long repeat) {
		return setTheRepeatingAlarm(mode, repeat, this);
	}

	public PendingIntent setRepeatingAlarm(long repeat) {
		return setRepeatingAlarm(AlarmManager.RTC_WAKEUP, repeat);
	}

	public void cancelAlarm() {
		mAlarmManager.cancel(mPendingIntent);
		AutomatonAlert.getAPIs().remove(this);
	}

	@Override
	public String toString() {
		return "AlarmPendingIntent {"
				+ "Type: " + mApiType.toString()
				+ ", AccountId: " + mAccountId
				+ ", AlertItemId: " + mAlertItemId
				+ ", NotificationItemId: " + mNotificationItemId
				+ ", RequestCode: " + mRequestCode
				+ ", Flags: " + Integer.toHexString(mFlags)
				+ ", Intent: " + mIntent.toString()
				+ "}";

	}

	private int compareTo(AlarmPendingIntent apiTo) {
		AlertItemDO alertItem =	AlertItems.get(mAlertItemId);
		AlertItemDO alertItemTo = AlertItems.get(apiTo.mAlertItemId);
		int ret = 0;

		ret = mApiType.compareTo(apiTo.mApiType);

		if (ret != 0) {
			return ret;
		}

		// if alertItem or alertItemTo are null...
		if (alertItem == null) {
			if (alertItemTo == null) {
				return 0;
			}
			else {
				return -1;
			}
		}
		else {
			if (alertItemTo == null) {
				return 1;
			}
		}

		// if dateRemind or dateRemindTo are null...
		if (alertItem.getDateRemind() == null) {
			if (alertItemTo.getDateRemind() == null) {
				return 0;
			}
			else {
				return -1;
			}
		}
		else {
			if (alertItemTo.getDateRemind() == null) {
				return 1;
			}
			else {
				return alertItem.getDateRemind().compareTo(
						alertItemTo.getDateRemind());
			}
		}

	}

	public static class DescendingComparator implements Comparator<AlarmPendingIntent> {

		@Override
		public int compare(
				final AlarmPendingIntent api1, final AlarmPendingIntent api2) {

			return api2.compareTo(api1);

		}
	}

	public static class AscendingComparator implements Comparator<AlarmPendingIntent> {

		@Override
		public int compare(
				final AlarmPendingIntent api1, final AlarmPendingIntent api2) {

			return api1.compareTo(api2);

		}
	}
}
