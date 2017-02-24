package com.aldersonet.automatonalert.Service;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Process;
import android.provider.ContactsContract.Contacts;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import com.aldersonet.automatonalert.Account.AccountDO;
import com.aldersonet.automatonalert.Account.Accounts;
import com.aldersonet.automatonalert.Activity.ContactFreeFormListActivity;
import com.aldersonet.automatonalert.Activity.RTUpdateActivity;
import com.aldersonet.automatonalert.Activity.RTUpdateActivity.FragmentTypeRT;
import com.aldersonet.automatonalert.Alarm.AlarmPendingIntent;
import com.aldersonet.automatonalert.Alarm.AlarmPendingIntent.ApiSubType;
import com.aldersonet.automatonalert.Alarm.AlarmPendingIntent.ApiType;
import com.aldersonet.automatonalert.Alert.AlertIntentExtrasChecker;
import com.aldersonet.automatonalert.Alert.AlertItemDO;
import com.aldersonet.automatonalert.Alert.AlertItems;
import com.aldersonet.automatonalert.Alert.NotificationItemDO;
import com.aldersonet.automatonalert.Alert.NotificationItems;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Email.AccountEmailDO;
import com.aldersonet.automatonalert.Email.EmailGetSemaphore;
import com.aldersonet.automatonalert.GC.GC;
import com.aldersonet.automatonalert.Preferences.GeneralPrefsDO;
import com.aldersonet.automatonalert.Preferences.GeneralPrefsDO.OverrideVolLevel;
import com.aldersonet.automatonalert.Provider.AutomatonAlertProvider;
import com.aldersonet.automatonalert.R;
import com.aldersonet.automatonalert.Receiver.AlertReceiver;
import com.aldersonet.automatonalert.SMS.MmsMonitor;
import com.aldersonet.automatonalert.SoundBomb.RemindersQ;
import com.aldersonet.automatonalert.SoundBomb.SoundBomb;
import com.aldersonet.automatonalert.SoundBomb.SoundBombQ;
import com.aldersonet.automatonalert.Util.Enums;
import com.aldersonet.automatonalert.Util.Utils;
import com.aldersonet.automatonalert.Widget.Widget2x1;
import com.aldersonet.automatonalert.Widget.Widget3x1;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.regex.PatternSyntaxException;

public class AutomatonAlertService extends Service {

	public static final String TAG = "AutomatonAlertService";

	public static String TAG_IS_BACKGROUND_DATA_OK = "mIsBackgroundDataOk";
	public static String NOTIFICATION_BRIEF_CONTENT_TITLE = "Scanning email";
	public static String NOTIFICATION_BRIEF_CONTENT_TEXT = "Tap here open app";

	public static long MIN_TIME_BETWEEN_ACCOUNT_CHECKS  = 29900;         // 29.9 seconds
	public static long MIN_TIME_BETWEEN_GC_POLL         = 14900;         // 14.9 seconds

	private int mStartId = 0;
	private ServiceHandler mServiceHandler;
	private boolean mStoppingFromUserAction;
	MmsMonitor mMmsMonitor;
	private long mLastGC;
	private GC mGC;



	public static boolean mIAmActive;

	public final class ServiceHandler extends Handler {

		ServiceHandler(final Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(final Message msg) {

			if (msg == null) {
				return;
			}

			mStartId = msg.arg1;
			Intent intent = (Intent)msg.obj;
			Bundle bundle = intent.getExtras();
			if (bundle == null) {
				return;
			}

			final String action = bundle.getString(AutomatonAlert.ACTION);
			if (action == null) {
				return;
			}

			AccountDO account = null;
			final String accountKey = bundle.getString(AutomatonAlert.ACCOUNT_KEY);
			if (accountKey != null) {
				account = Accounts.get(accountKey);
			}

			if (action.startsWith(AutomatonAlert.ACTION_EMAIL_CHECK_MAIL)) {
				doCheckEmail(account);
			}

			else if (action.equals(AutomatonAlert.ACTION_EMAIL_RESCHEDULE_POLL)) {
				doRescheduleEmail(account);
			}

			else if (action.equals(AutomatonAlert.ACTION_EMAIL_ACCOUNTS_CHANGE)) {
				doAccountsChange(bundle);
			}

			else if (action.equals(AutomatonAlert.ACTION_CANCEL)) {
				doCancel(bundle);
			}

			else if (action.equals(AutomatonAlert.ALERT_REMINDER_EVENT)) {
				doProcessRepeatQ();
			}

			else if (action.equals(AutomatonAlert.ACTION_RESTART_SERVICE)) {
				doRestartService(bundle);
			}

			else if (action.equals(AutomatonAlert.ACTION_STOP_SERVICE_USER)) {
				doStopService();
			}

			else if (action.equals(AutomatonAlert.ACTION_NOTIFY)) {
				doNotify(bundle);
			}

			else if (action.equals(AutomatonAlert.ACTION_DO_NOTIFICATION_ITEM_ALERT)) {
				doNotificationItemAlert(bundle);
			}

			else if (action.equals(AutomatonAlert.ALERT_TURN_OFF_ALL_NOTIFICATION_ITEM_SOUNDS)) {
				doTurnOffNotificationItemSounds();
			}

			else if (action.equals(AutomatonAlert.ALERT_TURN_OFF_NOTIFICATION_ITEM_SOUND)) {
				doTurnOffNotificationItemSound(bundle);
			}

			else if (action.equals(AutomatonAlert.ACTION_FOREGROUND_BACKGROUND)) {
				doForegroundBackground(bundle);
			}

			else if (action.equals(AutomatonAlert.GC_POLL)) {
				if (!isGCPollTooSoon()) {
					// resets for now+poll
					Utils.setGCPollAlarm(AutomatonAlertService.this);
					long now = System.currentTimeMillis();
					GeneralPrefsDO.setLastPolled(now);
					GeneralPrefsDO.save();
					long nextGC = mLastGC + GeneralPrefsDO.getGCPollInterval();
					if (nextGC < now) {
						mLastGC = now;
						mGC.doGC();
					}
				}
			}

			else if (action.equals(AutomatonAlert.WIDGET_UPDATE_2X1)) {
				doWidgetUpdate2x1();
			}

			else if (action.equals(AutomatonAlert.WIDGET_UPDATE_3X1)) {
				doWidgetUpdate3x1();
			}

			else if (action.equals(AutomatonAlert.WIDGET_ALL_SILENT_2X1)) {
				doWidgetToggleAllSilent2x1();
			}

			else if (action.equals(AutomatonAlert.WIDGET_ALL_SILENT_3X1)) {
				doWidgetToggleAllSilent3x1();
			}

			else if (action.equals(AutomatonAlert.WIDGET_PAUSE_ALERT_ALARM_2X1)) {
				doWidgetTogglePauseAlertAlarm2x1();
			}
			else if (action.equals(AutomatonAlert.WIDGET_PAUSE_ALERT_ALARM_3X1)) {
				doWidgetTogglePauseAlertAlarm3x1();
			}
			else if (action.equals(AutomatonAlert.WIDGET_OVERRIDE_VOL_TOGGLE_2X1)) {
				doWidgetToggleOverrideVol2x1();
			}

			else {
				Log.e(TAG, ".handleMessage(): message contained invalid"
						+ "action command: [" + action + "]");
			}

			try {
				AlertReceiver.completeWakefulIntent(intent);
			} catch (NullPointerException ignore) {}
		}
	}

	public static WakeLock getThreadWakeLock(Context context, String name) {
		PowerManager pm =
				(PowerManager)context.getSystemService(Context.POWER_SERVICE);
		final WakeLock wl =
				pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, name);

		releaseWaitLock(wl);

		if (wl != null) {
			wl.acquire();
		}

		return wl;
	}

	private static void releaseWaitLock(WakeLock wl) {
		if (wl != null
				&& wl.isHeld()) {
			wl.release();
		}
	}

	private void doCheckEmail(AccountDO account) {
        Log.d(TAG, "doCheckMail(): emailAcct: " + (account == null ? "null" : account.getKey()));
		if (!GeneralPrefsDO.isSystemOn()) {
			return;
		}

		final WakeLock wl = getThreadWakeLock(
				getApplicationContext(),
				AutomatonAlertService.class.getName() + ":doCheckEmail");

		// EMAIL only
		if (account != null
				&& account.mAccountType == AccountEmailDO.ACCOUNT_EMAIL) {

			final AccountEmailDO acct = (AccountEmailDO)account;

			// check on a new thread
			new Thread(new Runnable() {
                @Override
                public void run() {
                    checkMail(acct);
                    releaseWaitLock(wl);
                }
            }).start();
		}
		else {
			releaseWaitLock(wl);
		}
	}

	private void checkMail(AccountEmailDO acct) {
        Log.d(TAG, "checkMail(): emailAcct: " + acct.getKey());
		boolean updateAccountLastChecked = false;
        // email account semaphore.
		EmailGetSemaphore semaphore = AutomatonAlert.getMailGetSemaphores().get(acct.getKey());
		// Keep checkMail from being executed
		// concurrently because of either a slow
		// queue or messages being added too quickly.
		// We'll throw away the message if duplicate
		// message is being processed.


		// drops duplicate threads here while holding semaphore.
		if (AutomatonAlert.getMailGetSemaphores().tryAcquire(semaphore)) {
			// mMailHandle does not observe data changes. It needs to refreshAccount().
			//noinspection SynchronizeOnNonFinalField
			synchronized (semaphore.mMailHandle) {
				// make sure we're connected
				ConnectivityManager cm =
						(ConnectivityManager) getApplicationContext()
								.getSystemService(Context.CONNECTIVITY_SERVICE);
				// crash reported 11/1/14, this was a fix and a HIT on the bug
				NetworkInfo ni = cm.getActiveNetworkInfo();
				if (ni != null
						&& ni.isConnected()) {

					// forced time-wait for jic
					if (!isEmailCheckTooSoon(acct.getAccountId())) {

						// show in notification bar
						if (GeneralPrefsDO.isShowPollInNotificationBar()) {
							briefNotificationBarIcon(true);
						}
						// CHECK MAIL
						semaphore.mMailHandle.checkMail(
								AutomatonAlertService.this, mServiceHandler, mStartId);
						updateAccountLastChecked = true;

						// back to normal in notification bar
						if (GeneralPrefsDO.isShowPollInNotificationBar()) {
							briefNotificationBarIcon(false);
						}

						// make sure this happens again
						doRescheduleEmail(acct);
					}
				}
			}
			// ok if null
			// open the gate
			AutomatonAlert.getMailGetSemaphores().release(semaphore);
		}
		if (updateAccountLastChecked) {
			updateLastChecked(acct.getAccountId());
		}
	}

	private boolean isEmailCheckTooSoon(int accountId) {

		AccountEmailDO account = (AccountEmailDO)Accounts.get(accountId);

		long lastChecked = account.getLastChecked();

		if (lastChecked > 0) {
			long checkAfter = lastChecked + MIN_TIME_BETWEEN_ACCOUNT_CHECKS;
			// it's been long enough, check is ok
			if (checkAfter < System.currentTimeMillis()) {
				return false;
			}
			else {
				// too soon, so reschedule for later.
				doRescheduleEmail(account);
				Log.e(
						TAG, ".isEmailCheckTooSoon(): " +
						account.getKey() + ": checkAfter(last + "
								+ Utils.translateMillis(MIN_TIME_BETWEEN_ACCOUNT_CHECKS, true)
								+ ") = " + Utils.toLocaleString(checkAfter));

				return true;
			}
		}

		// on bad data, go ahead and check
		return false;
	}

	private boolean isGCPollTooSoon() {
		if (GeneralPrefsDO.getLastPolled() > 0) {
			long checkAfter =
					GeneralPrefsDO.getLastPolled() + MIN_TIME_BETWEEN_GC_POLL;
			// it's been long enough, check is ok
			if (checkAfter < System.currentTimeMillis()) {
				return false;
			}
			else {
				// too soon, so reschedule for later
				Utils.cancelGCPollAlarm();
				Utils.setGCPollAlarm(this);
				Log.d(
						TAG, ".isGCPollTooSoon(): " +
						"checkAfter(last + "
								+ Utils.translateMillis(MIN_TIME_BETWEEN_GC_POLL, true)
								+ ") = " + Utils.toLocaleString(checkAfter));

				return true;
			}
		}

		// on bad data, go ahead and check
		return false;
	}

	private void updateLastChecked(int accountId) {
		AccountEmailDO account = (AccountEmailDO)Accounts.get(accountId);
		account.setLastChecked();
		account.save();
	}

	private void putInForeground() {
		Intent intent = new Intent(this, ContactFreeFormListActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK  | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 1, intent, 0);

		NotificationCompat.Builder builder =
				new NotificationCompat.Builder(this);
		builder	.setSmallIcon(R.drawable.app_icon_blue_24)
				.setLargeIcon(getBitmap(R.drawable.app_icon_blue_24))
				.setTicker(AutomatonAlert.mAppTitle + " is running")
				.setContentTitle(AutomatonAlert.mAppTitle)
				.setContentText("Tap here to open " + AutomatonAlert.mAppTitle)
				.setWhen(System.currentTimeMillis())
				.setContentIntent(pendingIntent)
		;
		startForeground(10024, builder.build());
	}

	private void briefNotificationBarIcon(boolean on) {
		int reqCode = 44;
		final NotificationManager notificationManager =
				(NotificationManager)getSystemService(NOTIFICATION_SERVICE);

		if (!on) {
			notificationManager.cancel(reqCode);
			return;
		}

		long[] pattern = { 0, 50 };

		final int icon = R.drawable.app_icon_white_24;

		final Intent intent = new Intent(this, ContactFreeFormListActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

		final PendingIntent pendingIntent = PendingIntent.getActivity(
				this,
				79392,
				intent,
				PendingIntent.FLAG_UPDATE_CURRENT);

		final NotificationCompat.Builder builder =
				new NotificationCompat.Builder(this);
		builder .setSmallIcon(icon)
				.setLargeIcon(getBitmap(icon))
//				.setTicker("")
				.setWhen(System.currentTimeMillis())
				.setContentTitle(NOTIFICATION_BRIEF_CONTENT_TITLE)
				.setContentText(NOTIFICATION_BRIEF_CONTENT_TEXT)
				.setContentIntent(pendingIntent);

		if (GeneralPrefsDO.isShowPollInNotificationBarVibrate()) {
			builder.setVibrate(pattern);
		}

		notificationManager.notify(reqCode, builder.build());
	}

	private void doRescheduleEmail(AccountDO account) {
		if (account != null
				&& account.mAccountType == AccountEmailDO.ACCOUNT_EMAIL) {
			cancelAndMaybeReschedAlarm(account);
		}
	}

	private void cancelAndMaybeReschedAlarm(AccountDO account) {
		final long milliseconds = ((AccountEmailDO)account).getPoll();

		if (milliseconds <= 0) {
			// just cancel
			AutomatonAlert.getAPIs().findCancelRemovePendingIntentsPostAlarms(
					ApiType.EMAIL_POLL,
					ApiSubType.NONE,
					account.getAccountId(),
					-1,
					-1);
		}
		else {
			AlarmPendingIntent api = new AlarmPendingIntent(
					AlarmPendingIntent.ApiType.EMAIL_POLL,
					AlarmPendingIntent.ApiSubType.NONE,
					account.getAccountId(),
					-1,
					-1,
					account.getAccountId(),
					setEmailCheckIntent(account),
					PendingIntent.FLAG_CANCEL_CURRENT);

			// api.setRepeatingAlarm() does findCancelRemovePending...
			api.setRepeatingAlarm(AlarmManager.RTC_WAKEUP, milliseconds);
		}
	}

	public Intent setEmailCheckIntent(AccountDO account) {
		Intent intent = new Intent();

		intent.setAction(AutomatonAlert.ACTION_EMAIL_CHECK_MAIL);

		intent.putExtra(
				AutomatonAlert.ACTION,
				AutomatonAlert.ACTION_EMAIL_CHECK_MAIL);

		intent.setClass(
				getApplicationContext(),
				AlertReceiver.class);

		// make intent different for each account
		intent.setData(Uri.parse("" + account.getAccountId()));

		intent.putExtra(AutomatonAlert.ACCOUNT_KEY, account.getKey());
		intent.putExtra(AutomatonAlert.ACCOUNT_ID, account.getAccountId());

		return intent;
	}

	private void doAccountsChange(Bundle bundle) {
		// accounts information has changed, need to restart
		mServiceHandler.removeMessages(AutomatonAlert.ACTION_EMAIL_ACCOUNTS_CHANGE_WHAT);
		final Message newMsg = mServiceHandler.obtainMessage(
				AutomatonAlert.ACTION_EMAIL_ACCOUNTS_CHANGE_WHAT,
				++mStartId, 0, bundle);
		mServiceHandler.sendMessage(newMsg);
	}

	private void doCancel(Bundle bundle) {
		// cancel current operation
		mServiceHandler.removeMessages(AutomatonAlert.ACTION_CANCEL_WHAT);
		final Message newMsg =
				mServiceHandler.obtainMessage(
						AutomatonAlert.ACTION_CANCEL_WHAT, ++mStartId, 0, bundle);
		mServiceHandler.sendMessage(newMsg);
	}

	private void doProcessRepeatQ() {
		RemindersQ.processQ();
	}

	private void doRestartService(Bundle bundle) {
		// restart this service
		mServiceHandler
				.removeMessages(AutomatonAlert.ACTION_RESTART_SERVICE_WHAT);
		final Intent intent = new Intent(
				getApplicationContext(),
				AutomatonAlertService.class);
		intent.putExtras(bundle);
		startService(intent);
	}

	// this doesn't work, btw. All Activites have to be finish()'d
	// first for the System.exit(0) to have the desired effect.
	private void doStopService() {
		// shut ourselves down
		mServiceHandler
				.removeMessages(AutomatonAlert.ACTION_STOP_SERVICE_WHAT);
		final Intent intent = new Intent(
				getApplicationContext(),
				AutomatonAlertService.class);
		mStoppingFromUserAction = true;
		stopForeground(true);
		stopService(intent);
		System.exit(0);
	}

	private void doNotify(Bundle bundle) {
		int alertItemId = -1;
		String contactName = "<unknown>";

		if (bundle.containsKey(Contacts.DISPLAY_NAME)) {
			contactName = bundle.getString(Contacts.DISPLAY_NAME);
		}
		// NEW
		alertItemId = bundle.getInt(AlertItemDO.TAG_ALERT_ITEM_ID, -1);
		String ledMode = bundle.getString(NotificationItemDO.TAG_SHOW_NOTIFICATION_LED);
		String type = bundle.getString(RTUpdateActivity.TAG_FRAGMENT_TYPE, "");
		showInNotificationBar(alertItemId, type, contactName, ledMode);
	}

	private void doTurnOffNotificationItemSounds() {
		final WakeLock wl = getThreadWakeLock(
				getApplicationContext(),
				AutomatonAlertService.class.getName()
						+ ":doTurnOffNotificationItemSounds");

        new Thread(new Runnable() {
            @Override
            public void run() {
                RemindersQ.killAll();
                SoundBombQ.mQ.clear();
                // don't want concurrent mod error, so copy for remove()s
                HashSet<SoundBomb> soundBombs =
                        new HashSet<>(AutomatonAlert.getSoundBombs());
                for (SoundBomb soundBomb : soundBombs) {
                    Log.d(TAG, ".doTurnOffNotificationItemSounds(): calling stopAndRemove()");
                    soundBomb.stopAndRemove();
                }
                releaseWaitLock(wl);
            }
        }).start();
	}

	private void doTurnOffNotificationItemSound(final Bundle bundle) {
		final WakeLock wl = getThreadWakeLock(
				getApplicationContext(),
				AutomatonAlertService.class.getName()
						+ ":doTurnOffNotificationItemSound");

		new Thread(new Runnable() {
            @Override
            public void run() {
                int notificationItemId = bundle.getInt(
                        NotificationItemDO.TAG_NOTIFICATION_ITEM_ID);
                if (notificationItemId >= 0) {
                    NotificationItemDO notificationItem =
                            NotificationItems.get(notificationItemId);
                    if (notificationItem != null) {
                        // don't want concurrent mod error, so copy for remove()s
                        HashSet<SoundBomb> soundBombs =
                                new HashSet<>(AutomatonAlert.getSoundBombs());
                        for (SoundBomb soundBomb : soundBombs) {
                            if (soundBomb.mNotificationItem.getNotificationItemId()
                                    == notificationItem.getNotificationItemId()) {
                                Log.d(TAG, ".doTurnOffNotificationItemSound(): calling stopAndRemove()");
                                soundBomb.stopAndRemove();
                                RemindersQ.removeSoundBombFromQ(soundBomb);
                                SoundBombQ.removeSoundBombFromQ(soundBomb);
                            }
                        }
                    }
                }

                releaseWaitLock(wl);
            }
        }).start();
	}

	private void doNotificationItemAlert(final Bundle bundle) {
		final int alertItemId = bundle.getInt(AlertItemDO.TAG_ALERT_ITEM_ID, -1);
		int notificationItemId = bundle.getInt(
				NotificationItemDO.TAG_NOTIFICATION_ITEM_ID, -1);
		if (notificationItemId >= 0) {
			final NotificationItemDO notificationItem =
					NotificationItems.get(notificationItemId);
			if (notificationItem != null) {
				doNotificationItemNotification
						(bundle, alertItemId, notificationItem);
			}
		}
	}

	private void doNotificationItemNotification(
			final Bundle bundle, final int alertItemId,
			final NotificationItemDO notificationItem) {

		final WakeLock wl = getThreadWakeLock(
				getApplicationContext(),
				AutomatonAlertService.class.getName()
						+ ".doNotificationItemNotification()"
		);

		new Thread(new Runnable() {
            @Override
            public void run() {
                boolean thisIsATest = false;
                String contactName = "<unknown>";
                String type = null;
                if (bundle != null) {
                    if (bundle.containsKey(AutomatonAlert.TEST_LABEL)) {
                        thisIsATest = true;
                    }
                    if (bundle.containsKey(Contacts.DISPLAY_NAME)) {
                        contactName = bundle.getString(
                                Contacts.DISPLAY_NAME);
                    }
                    if (bundle.containsKey(AutomatonAlert.ACTION)) {
                        String action =
                                bundle.getString(AlertIntentExtrasChecker.TAG_ORIGINAL_ACTION);
                        if (action != null
                                && action.equals(AutomatonAlert.ALARM_ALERT_EVENT)) {
                            type = bundle.getString(
                                    RTUpdateActivity.TAG_FRAGMENT_TYPE,
                                    FragmentTypeRT.SETTINGS.name());
                        }
                    }
                }
                notificationItem.doNotification(
                        getApplicationContext(),
                        thisIsATest,
                        true,/*usePlayFor*/
                        Enums.getEnum(type, FragmentTypeRT.values(), null),
//						RTUpdateFragment.getFragmentType(type),
                        alertItemId,
                        contactName);

                releaseWaitLock(wl);

            }
        }).start();
	}

	private void doForegroundBackground(Bundle bundle) {
		if (bundle.getBoolean(GeneralPrefsDO.TAG_FOREGROUND)) {
			putInForeground();
		}
		else {
			putInBackground();
		}
	}

	private void doWidgetUpdate2x1() {
		//standard widget calls
		RemoteViews updateViews = Widget2x1.updateViews2x1(this);
		// Push update for this widget to the home screen
		ComponentName thisWidget = new ComponentName(this, Widget2x1.class);
		AppWidgetManager manager = AppWidgetManager.getInstance(this);
		manager.updateAppWidget(thisWidget, updateViews);

		// if changed to Silent, immediately silence any soundBomb's
		if (GeneralPrefsDO.getOverrideVol().equals(OverrideVolLevel.SILENT.name())) {
			final Intent intent = new Intent(
					getApplicationContext(),
					AutomatonAlertService.class);
			intent.putExtra(AutomatonAlert.ACTION,
					AutomatonAlert.ALERT_TURN_OFF_ALL_NOTIFICATION_ITEM_SOUNDS);
			startService(intent);
		}
	}

	private void doWidgetToggleAllSilent2x1() {
		if (GeneralPrefsDO.getOverrideVol().equals(OverrideVolLevel.SILENT.name())) {
			GeneralPrefsDO.setOverrideVol(OverrideVolLevel.DEFAULT.name());
		}
		else {
			GeneralPrefsDO.setOverrideVol(OverrideVolLevel.SILENT.name());
		}
		GeneralPrefsDO.save();
		doWidgetUpdate2x1();
	}

	private void doWidgetTogglePauseAlertAlarm2x1() {
		GeneralPrefsDO.setPauseAlertsAlarms(!GeneralPrefsDO.isPauseAlertsAlarms());
		GeneralPrefsDO.save();
		doWidgetUpdate2x1();
	}

	private void doWidgetUpdate3x1() {
		//standard widget calls
		RemoteViews updateViews = Widget3x1.updateViews3x1(this);
		// Push update for this widget to the home screen
		ComponentName thisWidget = new ComponentName(this, Widget3x1.class);
		AppWidgetManager manager = AppWidgetManager.getInstance(this);
		manager.updateAppWidget(thisWidget, updateViews);
	}

	private void doWidgetToggleAllSilent3x1() {
		if (GeneralPrefsDO.getOverrideVol().equals(OverrideVolLevel.SILENT.name())) {
			GeneralPrefsDO.setOverrideVol(OverrideVolLevel.DEFAULT.name());
		}
		else {
			GeneralPrefsDO.setOverrideVol(OverrideVolLevel.SILENT.name());
		}
		GeneralPrefsDO.save();
		doWidgetUpdate3x1();
	}

	private void doWidgetTogglePauseAlertAlarm3x1() {
		GeneralPrefsDO.setPauseAlertsAlarms(!GeneralPrefsDO.isPauseAlertsAlarms());
		GeneralPrefsDO.save();
		doWidgetUpdate3x1();
	}

	String mLastOverrideVol2x1;
	private void doWidgetToggleOverrideVol2x1() {
		boolean keepLooking = true;
		// if, coming back from silent, the thread's alive (<3 seconds since last press),
		// move along with the next logical OverrideVolLevel. If it's been >3 seconds,
		// go back to the OverrideVolLevel we were before we hit silent.
		boolean isThreadAlive =
				mThreadShowSilentNext2x1 != null
				&& mThreadShowSilentNext2x1.isAlive();

		// Silent is always the next image shown after the timer runs out
		if (isShowSilentNextWidget2x1()) {
			mLastOverrideVol2x1 = GeneralPrefsDO.getOverrideVol();
			GeneralPrefsDO.setOverrideVol(OverrideVolLevel.SILENT.name());
			mSkipNextSilent = true;
			keepLooking = false;
		}
		else {
			// make it toggle-like after silent
			if (mLastOverrideVol2x1 != null) {
				// >3 seconds, go back to OverrideVolLevel before silent
				GeneralPrefsDO.setOverrideVol(mLastOverrideVol2x1);
				if (isThreadAlive) {
					setNextOverrideVol(true/*skipSilent*/);
				}
				mLastOverrideVol2x1 = null;
				keepLooking = false;
			}
		}

		if (keepLooking) {
			setNextOverrideVol(false/*skipSilent*/);
		}

		GeneralPrefsDO.save();
		doWidgetUpdate2x1();
	}

	private void setNextOverrideVol(boolean skipSilent) {
		OverrideVolLevel dbOverrideVol =
				GeneralPrefsDO.getOverrideVolLevel(
						GeneralPrefsDO.getOverrideVol(),
						OverrideVolLevel.DEFAULT);

		OverrideVolLevel level = OverrideVolLevel.DEFAULT;

		if (dbOverrideVol.equals(OverrideVolLevel.DEFAULT)) {
			level = OverrideVolLevel.HI;
		}
		else if (dbOverrideVol.equals(OverrideVolLevel.HI)) {
			level = OverrideVolLevel.MED;
		}
		else if (dbOverrideVol.equals(OverrideVolLevel.MED)) {
			level = OverrideVolLevel.LOW;
		}
		else if (dbOverrideVol.equals(OverrideVolLevel.LOW)) {
			if (skipSilent) {
				level = OverrideVolLevel.DEFAULT;
			}
			else {
				level = OverrideVolLevel.SILENT;
			}
		}
		else if (dbOverrideVol.equals(OverrideVolLevel.SILENT)) {
			level = OverrideVolLevel.DEFAULT;
		}

		GeneralPrefsDO.setOverrideVol(level.name());
	}

	boolean mSkipNextSilent;
	Thread mThreadShowSilentNext2x1;
	private boolean isShowSilentNextWidget2x1() {
		boolean showSilentNext = false;
		boolean threadAlive =
				mThreadShowSilentNext2x1 != null
						&& mThreadShowSilentNext2x1.isAlive();

		if (!GeneralPrefsDO.getOverrideVol().equals(OverrideVolLevel.SILENT.name())
			&& !threadAlive) {
			// !SILENT AND DEAD
			showSilentNext = true;
			runShowSilentNextThread();
		}
		else {
			// SILENT OR ALIVE
			// thread is still running (touch: interrupt and rerun)
			if (threadAlive) {
				mThreadShowSilentNext2x1.interrupt();
			}
			runShowSilentNextThread();
		}

		return showSilentNext;
	}

	private void runShowSilentNextThread() {
		mThreadShowSilentNext2x1 =
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(3000);  // 3 seconds
                        Log.d(TAG, ".isShowSilentNextWidget2x1(): Done with SLEEP");
                    } catch (InterruptedException ignored) {}
                }
            });
        mThreadShowSilentNext2x1.start();
	}

	@Override
	public void onCreate() {
		mIAmActive = true;

		doInitializationStuff();
		startLooper();
		startMmsMonitor();
		updateWidget();
	}

	private void doInitializationStuff() {

		// get rid of extraneous records in the DB.
		// build ContactListInfo
		new Thread(new Runnable() {
			@Override
			public void run() {
				Utils.trimDb();
			}
		}).start();

		// if user asked for a foreground service
		if (GeneralPrefsDO.isForeground()) {
			putInForeground();
		}

		// send message to check email on all accounts
		initialEmailCheck();

		// in order to stay alive, we need to
		// poll for email or set up a gc poll
		makeSureWeArePolling();
	}

	private void startLooper() {
		final HandlerThread serviceThread = new HandlerThread(
				TAG, Process.THREAD_PRIORITY_BACKGROUND);
		serviceThread.start();

		// Make the thread's looper calls our Handler
		Looper mServiceLooper = serviceThread.getLooper();
		mServiceHandler = new ServiceHandler(mServiceLooper);
	}

	private void startMmsMonitor() {
		mMmsMonitor = new MmsMonitor(AutomatonAlert.THIS);
		mMmsMonitor.startMMSMonitoring();
	}

	private void updateWidget() {
		doWidgetUpdate2x1();
		doWidgetUpdate3x1();
	}

	public static void cancelAllAlarms() {
		AutomatonAlert.getAPIs().cancelAllAlarms();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		// we asked to stop via message ACTION_STOP_SERVICE (User Exit)
		// cancel alarms and unregister receivers
		if (mStoppingFromUserAction) {
			cancelAllAlarms();

			if (mMmsMonitor != null) {
				mMmsMonitor.stopMMSMonitoring();
			}
		}
	}

	@Override
	public int onStartCommand(
			final Intent intent, final int flags, final int sId) {

		super.onStartCommand(intent, flags, sId);

		mStoppingFromUserAction = false;
		int what = 0;

		// process the request
		if (intent != null) {
			mServiceHandler.sendMessage(
					mServiceHandler.obtainMessage(
							what, ++mStartId, flags, intent/*.getExtras()*/));
		}

		return START_STICKY;
	}

	private void makeSureWeArePolling() {

		// make sure the widget is alive
		updateWidget();

		// email polls
		ArrayList<AccountDO> accounts = Accounts.get();
		for (final AccountDO account : accounts) {
			// only check if poll is not Never/Manual
			if (account.mAccountType == AccountEmailDO.ACCOUNT_EMAIL
					&& ((AccountEmailDO)account).getPoll() > 0) {
				if (AutomatonAlert.getAPIs().getAlarmPendingIntents(
								ApiType.EMAIL_POLL, account.getAccountId(), -1, -1) == null) {
					doRescheduleEmail(account);
				}
			}
		}

		Utils.setGCPollAlarm(this);

		mGC = GC.getGC(this);
	}

	/* do email checks as soon as service starts */
	private void initialEmailCheck() {
		// only check email accounts
		ArrayList<AccountDO> accounts =
				Accounts.getByAccountType(AccountEmailDO.ACCOUNT_EMAIL);

		// only check accounts that are polled
		for (final AccountDO account : accounts) {
			if (((AccountEmailDO)account).getPoll() > 0) {
				doCheckEmail(account);
			}
		}
	}

	private CharSequence getNotificationBarTickerText(
			FragmentTypeRT type, String contactName, String msg) {

		String sName = TextUtils.isEmpty(contactName) ? ": " : "from " + contactName + ": ";

		return Utils.initCap(
				type.name())
				+ " "
				+ sName
				+ msg;
	}

	private CharSequence getNotificationBarContentText(String msgType) {
		return "Tap to open " + msgType + " app";

	}

	private void addToTextOrEmailList(FragmentTypeRT type, String name, String msg) {
		String cMsg = msg;
		if (TextUtils.isEmpty(msg)) {
			cMsg = "";
		}
		else {
			cMsg = ": " + msg;
		}
		if (type.equals(FragmentTypeRT.TEXT)) {
			AutomatonAlert.mTexts.add(name + cMsg);
		}
		else if (type.equals(FragmentTypeRT.EMAIL)) {
			AutomatonAlert.mEmails.add(name + cMsg);
		}
	}

	private int setInboxStyleLines(
			NotificationCompat.InboxStyle inbox, FragmentTypeRT type) {

		ArrayList<String> list = null;

		if (type.equals(FragmentTypeRT.TEXT)) {
			list = AutomatonAlert.mTexts;
		}
		else if (type.equals(FragmentTypeRT.EMAIL)) {
			list = AutomatonAlert.mEmails;
		}
		else {
			return 0;
		}
		for (String name : list) {
			inbox.addLine(name);
		}

		inbox.setSummaryText(
				list.size()
						+ (list.size() == 1 ? " Message" : " Messages"));

		return list.size();
	}

	private Intent getNotificationBarIntent(
			int alertItemId, FragmentTypeRT type, String contactName) {

		// calls receiver
		Intent intent = new Intent(this, AlertReceiver.class);
		intent.setAction(AutomatonAlert.ACTION_GOTO_TEXT_PHONE_EMAIL_APP);
		intent.putExtra(Contacts.DISPLAY_NAME, contactName);
		intent.putExtra(AlertItemDO.TAG_ALERT_ITEM_ID, alertItemId);
		intent.putExtra(AutomatonAlertProvider.SOURCE_TYPE_SOURCE_TYPE, type.name());

		return intent;
	}

	private Intent getDeleteNotificationBarIntent(
			int alertItemId, FragmentTypeRT type, String contactName) {

		Intent intent = getNotificationBarIntent(alertItemId, type, contactName);
		intent.putExtra("justClear", true);

		return intent;
	}

	private String getAlertItemMsg(int alertItemId, FragmentTypeRT type) {
		String msg = "";
		if (alertItemId != -1) {
			AlertItemDO alertItem = AlertItems.get(alertItemId);
			if (alertItem != null) {
				if (type.equals(FragmentTypeRT.TEXT)) {
					msg = alertItem.getKvRawDetails().get(AutomatonAlert.SMS_BODY);
				}
				else if (type.equals(FragmentTypeRT.EMAIL)) {
					msg = alertItem.getKvRawDetails().get(AutomatonAlert.SUBJECT);
				}
				if (msg == null) {
					msg = "";
				}
			}
		}

		return msg;
	}

	private void showInNotificationBar(
			int alertItemId, String type, String contactName, String ledMode) {

		// get contactName from AlertItem if needed
		if (contactName == null
				|| contactName.toLowerCase().equals("<unknown>")) {
			contactName =
					AlertItemDO.getDisplayNameFromAlertItem(alertItemId);
		}

		// translate FragmentTypeRT
		FragmentTypeRT fragType =
				Enums.getEnum(type, FragmentTypeRT.values(), null);
        if (fragType == null) { // should never happen, but it, in fact, does
            return;
        }

		// get <name><sms_body|subject>, then add to mEmails or mTexts
		String msg = getAlertItemMsg(alertItemId, fragType);
		addToTextOrEmailList(fragType, contactName, msg);

		int reqCode =
				fragType.equals(FragmentTypeRT.TEXT) ?
						  AutomatonAlert.NOTIFICATION_BAR_TEXT_ALERT
						: AutomatonAlert.NOTIFICATION_BAR_EMAIL_ALERT;

		// get intent and create PendingIntent
		Intent intent = getNotificationBarIntent(alertItemId, fragType, contactName);
		final PendingIntent contentIntent = PendingIntent.getBroadcast(this, reqCode, intent, 0);

		intent = getDeleteNotificationBarIntent(alertItemId, fragType, contactName);
		final PendingIntent deleteIntent = PendingIntent.getBroadcast(this, reqCode+10, intent, 0);

		// for .builder
		final CharSequence tickerText =
				getNotificationBarTickerText(fragType, contactName, msg);
		int smallIcon = fragType.equals(FragmentTypeRT.TEXT) ?
				  R.drawable.ic_automaton_alert_with_text
				: R.drawable.ic_automaton_alert_with_email;

		// setup Inbox Style
		NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
		inboxStyle.setBigContentTitle(tickerText);
		int size = setInboxStyleLines(inboxStyle, fragType);

		// build and notify
		NotificationCompat.Builder builder =
				new NotificationCompat.Builder(this)
				.setStyle(inboxStyle)
				.setContentIntent(contentIntent)
				.setDeleteIntent(deleteIntent)
				.setContentInfo(size + "")
				.setSmallIcon(smallIcon)
				.setLargeIcon(getBitmap(smallIcon))
				.setTicker(tickerText)
				.setWhen(System.currentTimeMillis())
				.setAutoCancel(true)
				.setContentTitle(tickerText)
				.setContentText(getNotificationBarContentText(Utils.initCap(fragType.name())))
				.setLights(
						getLightsColor(ledMode),
						getLightsOn(),
						getLightsOff(ledMode));

		final NotificationManager notificationManager =
				(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		notificationManager.notify(reqCode, builder.build());
	}

	private int getLightsColor(String ledMode) {
		int color = Color.BLUE;

		if (ledMode != null &&
				!ledMode.equals("0")) {
			String[] modes = { "", "" };
			try {
				modes = ledMode.split("_");
			}
			catch (PatternSyntaxException ignored) {}

			if (modes.length >= 2) {
				String colorPart = modes[1];
				if (colorPart.equalsIgnoreCase("red")) {
					color = Color.RED;
				}
				else if (colorPart.equalsIgnoreCase("green")) {
					color = Color.GREEN;
				}
				else if (colorPart.equalsIgnoreCase("yellow")) {
					color = Color.YELLOW;
				}
				else if (colorPart.equalsIgnoreCase("blue")) {
					color = Color.BLUE;
				}
				else if (colorPart.equalsIgnoreCase("cyan")) {
					color = Color.CYAN;
				}
				else if (colorPart.equalsIgnoreCase("magenta")) {
					color = Color.MAGENTA;
				}
				else if (colorPart.equalsIgnoreCase("white")) {
					color = Color.WHITE;
				}
			}
			int red = Color.red(color);
			int green = Color.green(color);
			int blue = Color.blue(color);
			color = Color.argb(0, red, green, blue);
		}
		return color;
	}

	private int getLightsOff(String ledMode) {
		int length = 3000;
		String[] modes = { "", "" };
		try {
			modes = ledMode.split("_");
		}
		catch (PatternSyntaxException ignored) {}

		if (modes.length >= 2) {
			if (modes[0].equalsIgnoreCase("fast")) {
				length = 500;
			}
			else if (modes[0].equalsIgnoreCase("slow")) {
				length = 3000;
			}
		}
		return length;
	}

	private int getLightsOn() {
		return 500;
	}

	private Bitmap getBitmap(int resource) {
		return BitmapFactory.decodeResource(getResources(), resource);
	}

	private void putInBackground() {
		stopForeground(true);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
