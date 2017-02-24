package com.aldersonet.automatonalert.Receiver;

import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

import com.aldersonet.automatonalert.Account.AccountDO;
import com.aldersonet.automatonalert.Account.Accounts;
import com.aldersonet.automatonalert.Activity.AlarmVisualActivity;
import com.aldersonet.automatonalert.Activity.DummyForScreenWakeActivity;
import com.aldersonet.automatonalert.Activity.RTUpdateActivity;
import com.aldersonet.automatonalert.Activity.RTUpdateActivity.FragmentTypeRT;
import com.aldersonet.automatonalert.Alarm.AlarmRepeat;
import com.aldersonet.automatonalert.Alert.AlertIntentExtrasChecker;
import com.aldersonet.automatonalert.Alert.AlertItemDO;
import com.aldersonet.automatonalert.Alert.AlertItems;
import com.aldersonet.automatonalert.Alert.NotificationItemDO;
import com.aldersonet.automatonalert.Alert.NotificationItems;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Preferences.GeneralPrefsDO;
import com.aldersonet.automatonalert.Preferences.QuietTimePreference;
import com.aldersonet.automatonalert.Provider.AutomatonAlertProvider;
import com.aldersonet.automatonalert.Service.AutomatonAlertService;
import com.aldersonet.automatonalert.SoundBomb.RemindersQ;
import com.aldersonet.automatonalert.Util.Utils;

public class AlertReceiver extends WakefulBroadcastReceiver {

	public static final String TAG = "AlertReceiver";

	private Context mContext;
	AlertItemDO mAlertItem;
	int mAlertItemId;
	NotificationItemDO mNotificationItem;
	int mNotificationItemId;
	FragmentTypeRT mFragmentType;

	private boolean mThisIsATest = false;
	private long mWakeLockTime;
	private WakeLock mOnReceiveWakeLock;

	@Override
	public void onReceive(final Context context, final Intent intent) {
		doOnReceiveWakeLock(context);

		if (intent == null) {
			releaseOnReceiveWakeLock();
			return;
		}

		final String action = intent.getAction();

		if (action == null) {
			releaseOnReceiveWakeLock();
			return;
		}

		getIntentData(intent);

		// TRASH (with expire time at some point in the future) those
		// AlertItems that aren't save-to-list
		markAlertItemAsTrashIfNeeded();

		mContext = context;

		///////////////////////
		///////////////////////

		//
		// Do an Alarm/Alert (just sound or screen and sound)
		//
		if (action.equals(AutomatonAlert.ALARM_ALERT_EVENT)) {
			// don't alert or alarm if widget set to no alert/alarm
			// or if we're in Quiet Time and user asked us to pause.
			if (/*GeneralPrefsDO.isPauseAlertsAlarms()
					||*/ (GeneralPrefsDO.isQuietTimeDoNotVibrate())
					&& QuietTimePreference.inQuietTime(isThisATest(intent), mNotificationItem)) {
				releaseOnReceiveWakeLock();
				return;
			}

			AlertIntentExtrasChecker ok =
					new AlertIntentExtrasChecker(intent, action);
			if (ok.intentExtrasOk(true/*checkAction*/)) {
				doAlert(context, intent, action);
			}
		}

		else if (action.equals(AutomatonAlert.ALARM_ALERT_SNOOZE_ALARM)) {
			getAlertItem(intent);
			if (mAlertItem != null) {
				snoozeNow(mAlertItem);
				Utils.showSetAlarmToast(
						mContext,
						System.currentTimeMillis() + GeneralPrefsDO.getDefaultSnooze(),
						true/*snooze*/);
			}
			cancelNotification(context, intent);
		}

		else if (action.equals(AutomatonAlert.ALARM_ALERT_TURN_OFF_ALARM)) {
			getAlertItem(intent);
			if (mAlertItem != null) {
				turnOffNow(context, mAlertItem);
			}
			cancelNotification(context, intent);
		}

		else if (action.equals(AutomatonAlert.ALERT_TURN_OFF_ALL_NOTIFICATION_ITEM_SOUNDS)) {
			callServiceForAlert(context, action, intent);
		}

		else if (action.equals(AutomatonAlert.ALERT_TURN_OFF_NOTIFICATION_ITEM_SOUND)) {
			callServiceForAlert(context, action, intent);
		}

		else if (action.equals(AutomatonAlert.ACTION_EMAIL_CHECK_MAIL)) {
			callServiceGeneric(context, action, intent);
		}

		else if (action.equals(AutomatonAlert.ACTION_EMAIL_RESCHEDULE_POLL)) {
			callServiceGeneric(context, action, intent);
		}

		else if (action.equals(AutomatonAlert.ALERT_REMINDER_EVENT)) {
			callServiceGeneric(context, action, intent);
		}

		else if (action.equals(AutomatonAlert.ACTION_FOREGROUND_BACKGROUND)) {
			callServiceGeneric(context, action, intent);
		}

		else if (action.equals(AutomatonAlert.GC_POLL)) {
			callServiceGeneric(context, action, intent);
		}

		else if (action.equals(AutomatonAlert.WIDGET_UPDATE_2X1)) {
			callServiceGeneric(context, action, intent);
		}

		else if (action.equals(AutomatonAlert.WIDGET_UPDATE_3X1)) {
			callServiceGeneric(context, action, intent);
		}

		else if (action.equals(AutomatonAlert.WIDGET_ALL_SILENT_2X1)) {
			callServiceGeneric(context, action, intent);
		}

		else if (action.equals(AutomatonAlert.WIDGET_ALL_SILENT_3X1)) {
			callServiceGeneric(context, action, intent);
		}

		else if (action.equals(AutomatonAlert.WIDGET_PAUSE_ALERT_ALARM_2X1)) {
			callServiceGeneric(context, action, intent);
		}

		else if (action.equals(AutomatonAlert.WIDGET_PAUSE_ALERT_ALARM_3X1)) {
			callServiceGeneric(context, action, intent);
		}

		else if (action.equals(AutomatonAlert.WIDGET_OVERRIDE_VOL_TOGGLE_2X1)) {
			callServiceGeneric(context, action, intent);
		}

		else if (action.equals(AutomatonAlert.ACTION_GOTO_TEXT_PHONE_EMAIL_APP)) {
			gotoAppFromNotification(context, intent);
		}

		releaseOnReceiveWakeLock();
	}

	private void doTimedWakeLock(Context context, boolean isScreenWl) {
		doTimedWakeLock(context, 0L, isScreenWl);
	}

	private void doTimedWakeLock(Context context, long wakeLockTime, boolean isScreenWl) {
		PowerManager pm =
				(PowerManager)context.getSystemService(Context.POWER_SERVICE);
		WakeLock wl = null;
		int flags = 0;
		String tag = this.getClass().getName();

		setWakeLockTime(wakeLockTime, isScreenWl);
		flags = getFlags(isScreenWl);

		// get the wakeLock
		wl = pm.newWakeLock(flags, tag);

		if (wl != null) {
			// make sure we don't crash
			if (wl.isHeld()) {
				wl.release();
			}
			// wake up
			wl.acquire(mWakeLockTime);
		}
	}

	private void doOnReceiveWakeLock(Context context) {
		releaseOnReceiveWakeLock();
		PowerManager pm =
				(PowerManager)context.getSystemService(Context.POWER_SERVICE);
		pm.newWakeLock(
				PowerManager.PARTIAL_WAKE_LOCK,
				this.getClass().getName()+ ":doOnReceiveWakeLock");
	}

	private void releaseOnReceiveWakeLock() {
		if (mOnReceiveWakeLock != null
				&& mOnReceiveWakeLock.isHeld()) {
			mOnReceiveWakeLock.release();
		}
	}

	private void setWakeLockTime(long wakeLockTime, boolean isScreenWl) {
		// determine length of wakeLock
		if (wakeLockTime > 0) {
			mWakeLockTime = wakeLockTime;
		}
		else {
			if (isScreenWl) {
				mWakeLockTime = 5000;
			}
			else {
				mWakeLockTime = 50;
			}
		}
	}

	private int getFlags(boolean isScreenWl) {
		// determine type of wakelock
		if (isScreenWl) {
			return PowerManager.SCREEN_BRIGHT_WAKE_LOCK
					| PowerManager.ACQUIRE_CAUSES_WAKEUP;
		}
		else {
			return PowerManager.PARTIAL_WAKE_LOCK;
		}
	}

	private void getIntentData(Intent intent) {
		if (intent != null) {
			getThisIsATest(intent);
			getFragmentType(intent);
		}

		getAlertItem(intent);
		getNotificationItem(intent);
	}

	private void getThisIsATest(Intent intent) {
		String test =
				AlarmVisualActivity.TEST_ALARM
				+ "|"
				+ AlarmVisualActivity.TEST_ALARM;

		String data = intent.getDataString();

		if (data != null
				&& data.equals(test)) {
			mThisIsATest = true;
		}
	}

	private void getFragmentType(Intent intent) {
		String type = intent.getStringExtra(RTUpdateActivity.TAG_FRAGMENT_TYPE);
		if (type == null) {
			type = "";
		}
		try {
			mFragmentType = FragmentTypeRT.valueOf(type);
		}
		catch (IllegalArgumentException e) {
			mFragmentType = FragmentTypeRT.TEXT;
		}
	}

	private void markAlertItemAsTrashIfNeeded() {
		// TRASH AlertItem's that aren't save-to-list
		if (mAlertItem != null) {
			final AlertItemDO alertItem = mAlertItem;
			new Thread(new Runnable() {
				@Override
				public void run() {
					int accountId = alertItem.getAccountId();
					if (accountId != -1) {
						AccountDO account = Accounts.get(accountId);
						if (account != null) {
							if (!account.isSaveToList()) {
								if (!alertItem.getStatus().equals(AlertItemDO.Status.TRASH)) {
									alertItem.updateStatus(AlertItemDO.Status.TRASH);
									Log.d(
											TAG + ".noSaveAlertItemToTrash()",
											"Trashing AlertItem");
								}
							}
						}
					}
				}
			}).start();
		}
	}

	private void cancelNotification(Context context, Intent intent) {
		int reqCode = intent.getIntExtra(AutomatonAlert.REQUEST_CODE, -1);
		if (reqCode != -1) {
			NotificationManager notificationManager =
					(NotificationManager)context.getSystemService(
							Context.NOTIFICATION_SERVICE);
			notificationManager.cancel(reqCode);
		}
	}

	private void callServiceGeneric(Context context, String action, Intent intent) {
		Intent doIntent = new Intent(intent);

		doIntent.setClass(
				context,
				AutomatonAlertService.class);

		doIntent.putExtra(
				AutomatonAlert.ACTION,
				action);

		try {
			startWakefulService(context, doIntent);
		} catch (NullPointerException ignore) {}
	}

	private Intent setAlarmVisualActivityIntentFlags(Intent intent) {
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
		intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
		intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
		if (!isThisATest(intent)) {
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		}

		return intent;
	}

	private void startDummyOrAlertVisual(Context context, final Intent doIntent) {
		Class cls = DummyForScreenWakeActivity.class;
		boolean shortCircuit =
				isScreenOn(context) && !isKeyguardShowing(context);
		if (shortCircuit) {
			cls = AlarmVisualActivity.class;
		}

		Intent intent = new Intent(doIntent);
		intent.setClass(mContext, cls);
		intent = setAlarmVisualActivityIntentFlags(intent);

		doTimedWakeLock(context, true/*isScreenWl*/);
		mContext.startActivity(intent);
	}

	private boolean isScreenOn(Context context) {
		return ((PowerManager)context.getSystemService(
				Context.POWER_SERVICE)).isScreenOn();
	}

	private boolean isKeyguardShowing(Context context) {
		return ((KeyguardManager)context.getSystemService(
				Context.KEYGUARD_SERVICE)).inKeyguardRestrictedInputMode();
	}

	private void doAlert(Context context, final Intent intent, String action) {
		Intent doIntent = null;

		// no NotificationItemDO, leave
		if (mNotificationItem == null) {
			return;
		}

		if (mNotificationItem.isNoAlertScreen()) {
			// check for repeat (this is called in
			// AlarmVisualActivity also)
			AlarmRepeat alarmRepeat =
					new AlarmRepeat(intent, action);
			if (alarmRepeat.isDoItAgain()) {
				alarmRepeat.reSendAlarm();
			}

			// sound only (no screen), hand it off
			callServiceForAlert(
					context,
					AutomatonAlert.ACTION_DO_NOTIFICATION_ITEM_ALERT,
					intent);
		}
		else {
			doIntent = new Intent(intent);
			doIntent.setClass(
					context,
					AlarmVisualActivity.class);
			doIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startDummyOrAlertVisual(context, doIntent);
		}
	}


	private void callServiceForAlert(Context context, String action, Intent intent) {
		Intent doIntent = new Intent(intent);

		doIntent.setClass(
				context,
				AutomatonAlertService.class);

		doIntent.putExtra(AutomatonAlert.ACTION, action);

		doIntent.putExtra(
				NotificationItemDO.TAG_NOTIFICATION_ITEM_ID,
				mNotificationItemId);

		if (mThisIsATest) {
			doIntent.putExtra(AutomatonAlert.TEST_LABEL, AutomatonAlert.TEST_LABEL);
		}

		startWakefulService(context, doIntent);
	}

	private void gotoAppFromNotification(Context context, Intent intent) {
		if (intent == null) {
			RemindersQ.killAll();
			return;
		}

		String sType = intent.getStringExtra(
				AutomatonAlertProvider.SOURCE_TYPE_SOURCE_TYPE);
		if (sType == null) {
			return;
		}

		NotificationManager nm =
				(NotificationManager)context.getSystemService(
						Context.NOTIFICATION_SERVICE);

		FragmentTypeRT type = null;
		if (sType.equals(FragmentTypeRT.TEXT.name())) {
			AutomatonAlert.mTexts.clear();
			type = FragmentTypeRT.TEXT;
			RemindersQ.killType(type);
			nm.cancel(AutomatonAlert.NOTIFICATION_BAR_TEXT_ALERT);
		}
		else if (sType.equals(FragmentTypeRT.EMAIL.name())) {
			AutomatonAlert.mEmails.clear();
			type = FragmentTypeRT.EMAIL;
			RemindersQ.killType(type);
			nm.cancel(AutomatonAlert.NOTIFICATION_BAR_EMAIL_ALERT);
		}
		else {
			return;
		}

		// user swiped or did a clear all for the notification.
		// just leave.
		if (intent.getBooleanExtra("justClear", false)) {
			return;
		}

		// go to Text Messages app or Email app
		Utils.IntentReqTypeRec rec = Utils.setSourceIntents(type);

		// start Activity
		if (rec != null) {
			try {
				context.startActivity(rec.mIntent);
			}
			catch (ActivityNotFoundException e) {
				Utils.toastIt(context, "Unable to open messaging app!");
			}
		}
	}

	private void getAlertItem(Intent intent) {
		if (intent == null) {
			return;
		}
		mAlertItemId = intent.getIntExtra(AlertItemDO.TAG_ALERT_ITEM_ID, -1);

		mAlertItem = null;
		if (mAlertItemId != -1) {
			mAlertItem = AlertItems.get(mAlertItemId);
		}
	}

	private void getNotificationItem(Intent intent) {
		if (intent == null) {
			mNotificationItemId = -1;
			return;
		}
		mNotificationItemId =
				intent.getIntExtra(NotificationItemDO.TAG_NOTIFICATION_ITEM_ID, -1);

		mNotificationItem = null;
		if (mNotificationItemId >= 0) {
			mNotificationItem =
					NotificationItems.get(mNotificationItemId);
			// purely defensive
			if (AutomatonAlert.RTOnly
					|| mFragmentType == FragmentTypeRT.PHONE) {
				//davedel -- force show AlertItem's in dev
//				if (!BuildConfig.DEBUG) {
					if (mNotificationItem != null) {
						mNotificationItem.setNoAlertScreen(true);
					}
//				}
				//davedel
			}
		}
	}

	private boolean isThisATest(Intent intent) {
		return intent != null && AlertIntentExtrasChecker.isThisATest(intent.getDataString());
	}

	private static void turnOffNow(Context context, AlertItemDO alertItem) {
		if (alertItem != null) {
			AlertItemDO.cancelPostAlarm(alertItem);
			final Intent intent = new Intent(
					context,
					AutomatonAlertService.class);
			intent.putExtra(
					AutomatonAlert.ACTION,
					AutomatonAlert.ALERT_TURN_OFF_NOTIFICATION_ITEM_SOUND);
			intent.putExtra(
					NotificationItemDO.TAG_NOTIFICATION_ITEM_ID,
					alertItem.getNotificationItemId());
			AutomatonAlert.THIS.startService(intent);

		}
		Utils.toastIt(context, "Alert dismissed");
	}

	private static void snoozeNow(AlertItemDO alertItem) {
		if (alertItem != null) {
			AlertItemDO.setSnooze(alertItem, null, GeneralPrefsDO.getDefaultSnooze());
		}
	}

}
