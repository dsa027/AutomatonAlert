package com.aldersonet.automatonalert.SoundBomb;

import android.content.Intent;

import com.aldersonet.automatonalert.Activity.RTUpdateActivity;
import com.aldersonet.automatonalert.Activity.RTUpdateActivity.FragmentTypeRT;
import com.aldersonet.automatonalert.Alarm.AlarmPendingIntent;
import com.aldersonet.automatonalert.Alarm.AlarmPendingIntent.ApiSubType;
import com.aldersonet.automatonalert.Alarm.AlarmPendingIntent.ApiType;
import com.aldersonet.automatonalert.Alert.NotificationItems;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Receiver.AlertReceiver;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingDeque;

public class RemindersQ {
	public static final String TAG = "RemindersQ";

	public static final long REMIND_AFTER  = /*30*1000;*/5 * 60 * 1000;  // sound every 5 minutes
	public static final long CHECK_Q_AFTER = /*10*1000;*/30 * 1000;      // check every 30 seconds

	// DEQUE - can add/get from front or back
	public static final LinkedBlockingDeque<SoundBombWrapper> mQ =
			new LinkedBlockingDeque<SoundBombWrapper>();

	public static void processQ() {
		long now = System.currentTimeMillis();
		for (SoundBombWrapper wrapper : mQ) {
			if (now > wrapper.mWhen) {
				mQ.remove(wrapper);
				// make sure it'll sound
				wrapper.mSoundBomb.reInit();
				// notificationItem may have changed since last notification
				// e.g., volume, or even whether to notify
				reGetNotificationItem(wrapper.mSoundBomb);
				// do notification, which might do an add(),
				// which will setAlarm below
				SoundBombQ.doNotification(wrapper.mSoundBomb, false/*isTest*/);
			}
		}

		// if we're not empty, keep the alarm loop going
		if (!processEmptyQ()) {
			setAlarm();
		}
	}

	private static void reGetNotificationItem(SoundBomb soundBomb) {
		if (soundBomb.mNotificationItem != null) {
			int nid = soundBomb.mNotificationItem.getNotificationItemId();
			if (nid != -1) {
				soundBomb.mNotificationItem = NotificationItems.get(nid);
			}
		}
	}

	private static boolean processEmptyQ() {
		if (mQ.isEmpty()) {
			cancelCurrentAlarm();
			return true;
		}

		return false;
	}

	public synchronized static boolean add(SoundBomb soundBomb) {
		if (soundBomb == null) return false;

		SoundBombWrapper wrapper = new SoundBombWrapper(soundBomb);
		mQ.add(wrapper);
		setAlarmIfNone();

		return true;
	}

	public static synchronized void killAll() {
		mQ.clear();
		processEmptyQ();
	}

	public static synchronized void killType(FragmentTypeRT type) {
		for (SoundBombWrapper wrapper : mQ) {
			if (wrapper.mType.equals(type)) {
				mQ.remove(wrapper);
			}
		}
		processEmptyQ();
	}

	public static boolean removeSoundBombFromQ(SoundBomb removeMe) {
		for (SoundBombWrapper wrapper : mQ) {
			if (wrapper.mSoundBomb.equals(removeMe)) {
				mQ.remove(wrapper);
				return true;
			}
		}
		processEmptyQ();

		return false;
	}

	/////////////////////////////////////////////
	// SOUND BOMB WRAPPER
	// tells us when to send it to the SoundBombQ
	private static class SoundBombWrapper {
		public SoundBomb mSoundBomb;
		public long mWhen;
		public RTUpdateActivity.FragmentTypeRT mType;

		SoundBombWrapper(SoundBomb soundBomb) {
			mSoundBomb = soundBomb;
			mWhen = System.currentTimeMillis() + REMIND_AFTER;
			mType = soundBomb.mType;
		}
	}
	/////////////////////////////////////////////

	private static synchronized void setAlarmIfNone() {
		ArrayList<AlarmPendingIntent> list =
				AutomatonAlert.getAPIs().getAlarmPendingIntents(
						ApiType.ALERT, ApiSubType.REMINDER);

		if (list.isEmpty()) {
			setAlarm();
		}
	}

	static void setAlarm() {
//		cancelCurrentAlarm();   // this is done in setRepeatingAlarm()
		AlarmPendingIntent api = getAlarmPendingIntent();
		api.setRepeatingAlarm(CHECK_Q_AFTER);
	}

	static void cancelCurrentAlarm() {
		AutomatonAlert.getAPIs().findCancelRemovePendingIntentsPostAlarms(
				ApiType.ALERT,
				ApiSubType.REMINDER,
				-1,
				-1,
				-1);

	}

	static AlarmPendingIntent getAlarmPendingIntent() {
		Intent intent = getReminderIntent();

		return new AlarmPendingIntent(
				ApiType.ALERT,
				ApiSubType.REMINDER,
				-1,
				-1,
				-1,
				17761876,
				intent,
				android.app.PendingIntent.FLAG_CANCEL_CURRENT);
	}

	static Intent getReminderIntent() {
		// create the intent to fire when alarm goes off
		Intent intent = new Intent(
				AutomatonAlert.THIS.getApplicationContext(),
				AlertReceiver.class);
		intent.setAction(AutomatonAlert.ALERT_REMINDER_EVENT);
		intent.setFlags(0);

		return intent;
	}
}
