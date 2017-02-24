package com.aldersonet.automatonalert.Preferences;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.os.RemoteException;
import android.util.Log;

import com.aldersonet.automatonalert.Alert.AlertItemDO;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Provider.AutomatonAlertProvider;

import java.util.Date;

public class GeneralPrefsDO {

	public static final String TAG = "GeneralPrefsDO";
	public static final String TAG_FOREGROUND = "mForeground";

	private static final long DEFAULT_EXPIRE_DELETED_AFTER   = 1 * 60 * 60 * 1000;   // 1 hour
	static final long DEFAULT_GC_POLL                        = 1 * 60 * 60 * 1000;   // 1 hour
	private static final long STOP_SOUND_AFTER               = 30 * 60 * 1000;       // 30 minutes

	private static int mGeneralPrefsId;
	private static boolean mKeepLists;
	private static boolean mKeepDeleted;
	private static long mExpireDeletedAfter;
	private static long mDefaultSnooze;
	private static boolean mAutoAck;
	private static long mAutoAckAfter;
	private static AckAs mAutoAckAs;
	private static long mRingtoneStopLoopAfter;
	private static long mSoundFileStopLoopAfter;
	private static long mQuietTimeStart;
	private static long mQuietTimeEnd;
	private static boolean mQuietTimeDoNotVibrate;
	private static boolean mGlobalPause;
	private static boolean mAlwaysShowNotification;
	private static NotificationAction mNotificationAction;
	private static int mPrefetchEmailsCount;
	private static String mMarkViewedAlertAs;
	private static boolean mShowPollInNotificationBar;
	private static boolean mShowPollInNotificationBarVibrate;
	private static boolean mStartAtBoot;
	private static boolean mSystemOn;
	private static boolean mPauseAlertsAlarms;
	private static int mMaxListSize;
	private static String mOverrideVol;
	private static int mImapMaxRetries;
	private static boolean mForeground;
	private static boolean mDebugMode;
	private static String mLastDbVersionChecked;
	private static long mGCPollInterval;
	private static long mLastPolled;
	private static Date mTimeStamp;

	public static boolean mFlagThatSaysWeHavePopulatedFromDb;
	private static boolean isDirty = false;

	static {
		initValues();
	}

	public enum OverrideVolLevel {
		DEFAULT,
		SILENT,
		LOW,
		MED,
		HI
	}
	public static OverrideVolLevel getOverrideVolLevel(String level, OverrideVolLevel def) {
		try {
			return OverrideVolLevel.valueOf(level);
		}
		catch (IllegalArgumentException e) {
			return def;
		}
	}

	public enum AckAs {
		Snoozed,
		Dismissed
	}

	public enum NotificationAction {
		MAIN,
		LIST,
		SETTINGS
	}

	/* USED TO MAINTAIN THESE FIELDS AT THESE VALUES */
	/* USED TO MAINTAIN THESE FIELDS AT THESE VALUES */
	/* USED TO MAINTAIN THESE FIELDS AT THESE VALUES */
	/* only DeveloperPrefrences will override at the */
	/* moment they're set and until another process  */
	/* (such as a boot or fc) changes their value.   */
	private static void setGCPollAndExpired() {
		mExpireDeletedAfter = DEFAULT_EXPIRE_DELETED_AFTER;
		mGCPollInterval = DEFAULT_GC_POLL;
		mKeepDeleted = true;
	}

	private static void initValues() {

		mFlagThatSaysWeHavePopulatedFromDb = false;

		setGCPollAndExpired();

		mGeneralPrefsId = 0;
		mKeepLists = false;
		mKeepDeleted = true;
		mDefaultSnooze = 10 * 60 * 1000;
//		mAutoAck = !(AutomatonAlert.mDevProdVersion.equals(DevProdVersion.PROD));
		mAutoAck = true;
		mAutoAckAfter = 5 * 60 * 1000;
		mAutoAckAs = AckAs.Snoozed;
		mRingtoneStopLoopAfter = 5 * 60 * 1000;
		mSoundFileStopLoopAfter = STOP_SOUND_AFTER;

		mQuietTimeStart = -1;
		mQuietTimeEnd = -1;
		mQuietTimeDoNotVibrate = false;
		mGlobalPause = false;								// NOT CURRENTLY USED
		mAlwaysShowNotification = false;					// NOT CURRENTLY USED
		mNotificationAction = NotificationAction.MAIN;		// NOT CURRENTLY USED
		mPrefetchEmailsCount = 5;
		mMarkViewedAlertAs = AlertItemDO.Status.SAVED.name();

		mShowPollInNotificationBar = false;
		mShowPollInNotificationBarVibrate = false;

		mStartAtBoot = true;
		mSystemOn = true;
		mPauseAlertsAlarms = false;
		mMaxListSize = 99999;
		mOverrideVol = OverrideVolLevel.DEFAULT.name();

		mImapMaxRetries = 1;
		mForeground = false;
		mDebugMode = false;

		mLastDbVersionChecked = "";

		mLastPolled = 0;

		mTimeStamp = new Date(System.currentTimeMillis());

		isDirty = true;
	}

	public static void populate(Cursor cursor) {

		isDirty = false;

		mGeneralPrefsId = cursor.getInt(
				cursor.getColumnIndex(AutomatonAlertProvider.GENERAL_PREFS_ID));

		mKeepLists = cursor.getString(cursor.getColumnIndex(
				AutomatonAlertProvider.GENERAL_PREFS_KEEP_LISTS))
				.equalsIgnoreCase(AutomatonAlert.TRUE);

		mKeepDeleted = cursor.getString(cursor.getColumnIndex(
				AutomatonAlertProvider.GENERAL_PREFS_KEEP_DELETED))
				.equalsIgnoreCase(AutomatonAlert.TRUE);

		mExpireDeletedAfter = cursor.getLong(
				cursor.getColumnIndex(AutomatonAlertProvider.GENERAL_PREFS_EXPIRE_DELETED_AFTER));

		mDefaultSnooze = cursor.getLong(
				cursor.getColumnIndex(AutomatonAlertProvider.GENERAL_PREFS_DEFAULT_SNOOZE));

		mAutoAck = cursor.getString(cursor.getColumnIndex(
				AutomatonAlertProvider.GENERAL_PREFS_AUTO_ACK))
				.equalsIgnoreCase(AutomatonAlert.TRUE);

		mAutoAckAfter = cursor.getLong(
				cursor.getColumnIndex(AutomatonAlertProvider.GENERAL_PREFS_AUTO_ACK_AFTER));

		try {
			mAutoAckAs = AckAs.valueOf(cursor.getString(
					cursor.getColumnIndex(AutomatonAlertProvider.GENERAL_PREFS_AUTO_ACK_AS)));

		} catch (IllegalArgumentException e) {
			mAutoAckAs = AckAs.Snoozed;
		}

		mRingtoneStopLoopAfter = cursor.getLong(
				cursor.getColumnIndex(AutomatonAlertProvider.GENERAL_PREFS_RINGTONE_STOP_LOOP_AFTER));

		mSoundFileStopLoopAfter = cursor.getLong(
				cursor.getColumnIndex(AutomatonAlertProvider.GENERAL_PREFS_SOUNDFILE_STOP_LOOP_AFTER));

		mQuietTimeStart = cursor.getLong(
				cursor.getColumnIndex(AutomatonAlertProvider.GENERAL_PREFS_QUIET_TIME_START));

		mQuietTimeEnd = cursor.getLong(
				cursor.getColumnIndex(AutomatonAlertProvider.GENERAL_PREFS_QUIET_TIME_END));

		mQuietTimeDoNotVibrate = cursor.getString(cursor.getColumnIndex(
				AutomatonAlertProvider.GENERAL_PREFS_QUIET_TIME_PAUSES))
				.equalsIgnoreCase(AutomatonAlert.TRUE);

		mGlobalPause = cursor.getString(cursor.getColumnIndex(
				AutomatonAlertProvider.GENERAL_PREFS_GLOBAL_PAUSE))
				.equalsIgnoreCase(AutomatonAlert.TRUE);

		mAlwaysShowNotification = cursor.getString(cursor.getColumnIndex(
				AutomatonAlertProvider.GENERAL_PREFS_ALWAYS_SHOW_NOTIFICATION))
				.equalsIgnoreCase(AutomatonAlert.TRUE);

		try {
			mNotificationAction = NotificationAction.valueOf(cursor.getString(
					cursor.getColumnIndex(AutomatonAlertProvider.GENERAL_PREFS_NOTIFICATION_ACTION)));

		} catch (IllegalArgumentException e) {
			mNotificationAction = NotificationAction.MAIN;
		}

		mPrefetchEmailsCount = cursor.getInt(
				cursor.getColumnIndex(AutomatonAlertProvider.GENERAL_PREFS_PREFETCH_EMAILS_COUNT));

		mMarkViewedAlertAs = cursor.getString(
				cursor.getColumnIndex(AutomatonAlertProvider.GENERAL_PREFS_MARK_VIEWED_ALERT_AS));

		mShowPollInNotificationBar = cursor.getString(cursor.getColumnIndex(
				AutomatonAlertProvider.GENERAL_PREFS_SHOW_POLL_IN_NOTIFICATION_BAR))
				.equalsIgnoreCase(AutomatonAlert.TRUE);

		mShowPollInNotificationBarVibrate = cursor.getString(cursor.getColumnIndex(
				AutomatonAlertProvider.GENERAL_PREFS_SHOW_POLL_IN_NOTIFICATION_BAR_VIBRATE))
				.equalsIgnoreCase(AutomatonAlert.TRUE);

		mStartAtBoot = cursor.getString(cursor.getColumnIndex(
				AutomatonAlertProvider.GENERAL_PREFS_START_AT_BOOT))
				.equalsIgnoreCase(AutomatonAlert.TRUE);

		mSystemOn = cursor.getString(cursor.getColumnIndex(
				AutomatonAlertProvider.GENERAL_PREFS_SYSTEM_ON))
				.equalsIgnoreCase(AutomatonAlert.TRUE);

		mPauseAlertsAlarms = cursor.getString(cursor.getColumnIndex(
				AutomatonAlertProvider.GENERAL_PREFS_PAUSE_ALERTS_ALARMS))
				.equalsIgnoreCase(AutomatonAlert.TRUE);

		mMaxListSize = cursor.getInt(
				cursor.getColumnIndex(AutomatonAlertProvider.GENERAL_PREFS_MAX_LIST_SIZE));

		mOverrideVol = cursor.getString(
				cursor.getColumnIndex(AutomatonAlertProvider.GENERAL_PREFS_OVERRIDE_VOL));

		mImapMaxRetries = cursor.getInt(
				cursor.getColumnIndex(AutomatonAlertProvider.GENERAL_PREFS_IMAP_MAX_RETRIES));

		mForeground = cursor.getString(cursor.getColumnIndex(
				AutomatonAlertProvider.GENERAL_PREFS_FOREGROUND))
				.equalsIgnoreCase(AutomatonAlert.TRUE);

		mDebugMode = cursor.getString(cursor.getColumnIndex(
				AutomatonAlertProvider.GENERAL_PREFS_DEBUG_MODE))
				.equalsIgnoreCase(AutomatonAlert.TRUE);
		AutomatonAlert.DEBUG = mDebugMode;

		mGCPollInterval = cursor.getLong(
				cursor.getColumnIndex(AutomatonAlertProvider.GENERAL_PREFS_GC_POLL_INTERVAL));

		mLastDbVersionChecked = cursor.getString(
				cursor.getColumnIndex(AutomatonAlertProvider.GENERAL_PREFS_LAST_DB_VERSION_CHECKED));

		mLastPolled = cursor.getLong(
				cursor.getColumnIndex(AutomatonAlertProvider.GENERAL_PREFS_LAST_POLLED));

		long millis = cursor.getLong(
				cursor.getColumnIndex(AutomatonAlertProvider.GENERAL_PREFS_TIMESTAMP));
		if (millis != -1) {
			mTimeStamp = new Date(millis);
		}
		else {
			mTimeStamp = new Date(System.currentTimeMillis());
		}

		mFlagThatSaysWeHavePopulatedFromDb = true;

//		if (AutomatonAlert.mDevProdVersion.equals(DevProdVersion.PROD)) {
//			GeneralPrefsDO.mAutoAck = false;
//			GeneralPrefsDO.mQuietTimeStart = -1;
//			GeneralPrefsDO.mQuietTimeEnd = -1;
//		}

	}



	public static synchronized void save() {

//		if (AutomatonAlert.mDevProdVersion.equals(DevProdVersion.PROD)) {
//			mAutoAck = false;
//			mQuietTimeStart = -1;
//			mQuietTimeEnd = -1;
//		}

		ContentValues cv = AutomatonAlertProvider.getGeneralPrefsContentValues(
				Boolean.toString(mKeepLists),
				Boolean.toString(mKeepDeleted),
				mExpireDeletedAfter,
				mDefaultSnooze,
				Boolean.toString(mAutoAck),
				mAutoAckAfter,
				mAutoAckAs.toString(),
				mRingtoneStopLoopAfter,
				mSoundFileStopLoopAfter,
				mQuietTimeStart,
				mQuietTimeEnd,
				Boolean.toString(mQuietTimeDoNotVibrate),
				Boolean.toString(mGlobalPause),
				Boolean.toString(mAlwaysShowNotification),
				mNotificationAction.toString(),
				mPrefetchEmailsCount,
				mMarkViewedAlertAs,
				Boolean.toString(mShowPollInNotificationBar),
				Boolean.toString(mShowPollInNotificationBarVibrate),
				Boolean.toString(mStartAtBoot),
				Boolean.toString(mSystemOn),
				Boolean.toString(mPauseAlertsAlarms),
				mMaxListSize,
				mOverrideVol,
				mImapMaxRetries,
				Boolean.toString(mForeground),
				Boolean.toString(mDebugMode),
				mLastDbVersionChecked,
				mLastPolled,
				mGCPollInterval
				);

		AutomatonAlertProvider aap =
				(AutomatonAlertProvider)AutomatonAlert.getProvider()
				.getLocalContentProvider();

		int id = -1;

		try {
            id = aap == null ? -1 :
                    aap.insertOrUpdate(
					cv,
					mGeneralPrefsId,
					AutomatonAlertProvider.GENERAL_PREFS_ID_URI,
					AutomatonAlertProvider.GENERAL_PREFS_TABLE_URI);
		} catch (SQLiteException ignore) {}

		// if inserted, store new id
		if (id != -1
				&& id != mGeneralPrefsId) {
			mGeneralPrefsId = id;
		}

		isDirty = false;
	}

	public static int getGeneralPrefsId() {
		refreshIfNeeded();
		return mGeneralPrefsId;
	}

	public static boolean isKeepLists() {
		refreshIfNeeded();
		return mKeepLists;
	}

	public static void setKeepLists(boolean keepLists) {
		refreshIfNeeded();
		GeneralPrefsDO.mKeepLists = keepLists;
	}

	public static boolean isKeepDeleted() {
		refreshIfNeeded();
		return mKeepDeleted;
	}

	public static void setKeepDeleted(boolean keepDeleted) {
		refreshIfNeeded();
		GeneralPrefsDO.mKeepDeleted = keepDeleted;
	}

	public static long getDefaultSnooze() {
		refreshIfNeeded();
		return mDefaultSnooze;
	}

	public static void setDefaultSnooze(long defaultSnooze) {
		refreshIfNeeded();
		GeneralPrefsDO.mDefaultSnooze = defaultSnooze;
	}

	public static boolean isAutoAck() {
		refreshIfNeeded();
		return mAutoAck;
	}

	public static void setAutoAck(boolean autoAck) {
		refreshIfNeeded();
		GeneralPrefsDO.mAutoAck = autoAck;
	}

	public static long getAutoAckAfter() {
		refreshIfNeeded();
		return mAutoAckAfter;
	}

	public static void setAutoAckAfter(long autoAckAfter) {
		refreshIfNeeded();
		GeneralPrefsDO.mAutoAckAfter = autoAckAfter;
	}

	public static AckAs getAutoAckAs() {
		refreshIfNeeded();
		return mAutoAckAs;
	}

	public static void setAutoAckAs(AckAs autoAckAs) {
		refreshIfNeeded();
		GeneralPrefsDO.mAutoAckAs = autoAckAs;
	}

	public static long getRingtoneStopLoopAfter() {
		refreshIfNeeded();
		return mRingtoneStopLoopAfter;
	}

	public static void setRingtoneStopLoopAfter(long ringtoneStopLoopAfter) {
		refreshIfNeeded();
		GeneralPrefsDO.mRingtoneStopLoopAfter = ringtoneStopLoopAfter;
	}

	public static long getSoundfileStopLoopAfter() {
		refreshIfNeeded();
		return mSoundFileStopLoopAfter;
	}

	public static void setSoundfileStopLoopAfter(long soundfileStopLoopAfter) {
		refreshIfNeeded();
		GeneralPrefsDO.mSoundFileStopLoopAfter = soundfileStopLoopAfter;
	}

	static long getQuietTimeStart() {
		refreshIfNeeded();
		return mQuietTimeStart;
	}

	static void setQuietTimeStart(long quietTimeStart) {
		refreshIfNeeded();
		GeneralPrefsDO.mQuietTimeStart = quietTimeStart;
	}

	static long getQuietTimeEnd() {
		refreshIfNeeded();
		return mQuietTimeEnd;
	}

	static void setQuietTimeEnd(long quietTimeEnd) {

		refreshIfNeeded();
		GeneralPrefsDO.mQuietTimeEnd = quietTimeEnd;
	}

	public static boolean isQuietTimeDoNotVibrate() {
		refreshIfNeeded();
		return mQuietTimeDoNotVibrate;
	}

	static void setQuietTimeDoNotVibrate(boolean quietTimeDoNotVibrate) {
		refreshIfNeeded();
		GeneralPrefsDO.mQuietTimeDoNotVibrate = quietTimeDoNotVibrate;
	}

	public static boolean isGlobalPause() {
		refreshIfNeeded();
		return mGlobalPause;
	}

	public static void setGlobalPause(boolean globalPause) {
		refreshIfNeeded();
		GeneralPrefsDO.mGlobalPause = globalPause;
	}

	public static boolean isAlwaysShowNotification() {
		refreshIfNeeded();
		return mAlwaysShowNotification;
	}

	public static void setAlwaysShowNotification(boolean alwaysShowNotification) {
		refreshIfNeeded();
		GeneralPrefsDO.mAlwaysShowNotification = alwaysShowNotification;
	}

	public static NotificationAction getNotificationAction() {
		refreshIfNeeded();
		return mNotificationAction;
	}

	public static void setNotificationAction(NotificationAction notificationAction) {
		refreshIfNeeded();
		GeneralPrefsDO.mNotificationAction = notificationAction;
	}

	public static int getPrefetchEmailsCount() {
		refreshIfNeeded();
		return mPrefetchEmailsCount;
	}

	public static void setPrefetchEmailsCount(int prefetchEmailsCount) {
		refreshIfNeeded();
		GeneralPrefsDO.mPrefetchEmailsCount = prefetchEmailsCount;
	}

	public static String getMarkViewedAlertAs() {
		refreshIfNeeded();
		return mMarkViewedAlertAs;
	}

	public static void setMarkViewedAlertAs(String markViewedAlertAs) {
		refreshIfNeeded();
		GeneralPrefsDO.mMarkViewedAlertAs = markViewedAlertAs;
	}

	public static boolean isShowPollInNotificationBar() {
		refreshIfNeeded();
		return mShowPollInNotificationBar;
	}

	public static void setShowPollInNotificationBar(
			boolean showPollInNotificationBar) {
		refreshIfNeeded();
		GeneralPrefsDO.mShowPollInNotificationBar = showPollInNotificationBar;
	}

	public static boolean isShowPollInNotificationBarVibrate() {
		refreshIfNeeded();
		return mShowPollInNotificationBarVibrate;
	}

	public static void setShowPollInNotificationBarVibrate(
			boolean showPollInNotificationBarVibrate) {
		refreshIfNeeded();
		GeneralPrefsDO.mShowPollInNotificationBarVibrate = showPollInNotificationBarVibrate;
	}

	public static boolean isStartAtBoot() {
		refreshIfNeeded();
		return mStartAtBoot;
	}

	public static void setStartAtBoot(boolean startAtBoot) {
		refreshIfNeeded();
		GeneralPrefsDO.mStartAtBoot = startAtBoot;
	}

	public static boolean isSystemOn() {
		refreshIfNeeded();
		return mSystemOn;
	}

	static void setSystemOn(boolean systemOn) {
		refreshIfNeeded();
		GeneralPrefsDO.mSystemOn = systemOn;
	}

	public static boolean isPauseAlertsAlarms() {
		refreshIfNeeded();
		return mPauseAlertsAlarms;
	}

	public static void setPauseAlertsAlarms(boolean pauseAlertsAlarms) {
		refreshIfNeeded();
		GeneralPrefsDO.mPauseAlertsAlarms = pauseAlertsAlarms;
	}

	public static int getMaxListSize() {
		refreshIfNeeded();
		return mMaxListSize;
	}

	public static void setMaxListSize(int maxListSize) {
		refreshIfNeeded();
		GeneralPrefsDO.mMaxListSize = maxListSize;
	}

	public static String getOverrideVol() {
		refreshIfNeeded();
		return mOverrideVol;
	}

	public static void setOverrideVol(String overrideVol) {
		refreshIfNeeded();
		GeneralPrefsDO.mOverrideVol = overrideVol;
	}

	public static int getImapMaxRetries() {
		refreshIfNeeded();
		return mImapMaxRetries;
	}

	static void setImapMaxRetries(int imapMaxRetries) {
		refreshIfNeeded();
		GeneralPrefsDO.mImapMaxRetries = imapMaxRetries;
	}

	public static boolean isForeground() {
		refreshIfNeeded();
		return mForeground;
	}

	static void setForeground(boolean foregroundMode) {
		refreshIfNeeded();
		GeneralPrefsDO.mForeground = foregroundMode;
	}

	public static boolean isDebugMode() {
		refreshIfNeeded();
		return mDebugMode;
	}

	static void setDebugMode(boolean debugMode) {
		refreshIfNeeded();
		GeneralPrefsDO.mDebugMode = debugMode;
	}

	public static String getLastDbVersionChecked() {
		refreshIfNeeded();
		return mLastDbVersionChecked;
	}

	public static void setLastDbVersionChecked(String lastDbVersionChecked) {
		refreshIfNeeded();
		GeneralPrefsDO.mLastDbVersionChecked = lastDbVersionChecked;
	}

	public static long getGCPollInterval() {
		refreshIfNeeded();
		return mGCPollInterval;
	}

	static void setGCPollInterval(long gcPollInterval) {
		refreshIfNeeded();
		GeneralPrefsDO.mGCPollInterval = gcPollInterval;
//		resetGCPollInterval();
	}

//	public static boolean resetGCPollInterval() {
//		// make sure we GC_POLL poll at least the mExpireDeletedAfter time
//		long gcPoll =	Math.min(mGCPollInterval, mExpireDeletedAfter);
//		if (gcPoll != mGCPollInterval) {
//			if (mDebugMode) {
//				Utils.toastIt(
//						"Had to reset GC Poll to "
//								+ Utils.getTimeRemainingMillis(gcPoll, true/*showSeconds*/));
//			}
//			GeneralPrefsDO.mGCPollInterval = gcPoll;
//			return true;
//		}
//		return false;
//	}

	public static long getExpireDeletedAfter() {
		refreshIfNeeded();
		return mExpireDeletedAfter;
	}

//	public static void setExpireDeletedAfter(long expireDeletedAfter) {
//		refreshIfNeeded();
//		GeneralPrefsDO.mExpireDeletedAfter = expireDeletedAfter;
//		resetGCPollInterval();
//	}

	public static long getLastPolled() {
		refreshIfNeeded();
		return mLastPolled;
	}

	public static void setLastPolled(long lastPolled) {
		refreshIfNeeded();
		GeneralPrefsDO.mLastPolled = lastPolled;
	}

	public static Date getTimeStamp() {
		refreshIfNeeded();
		return mTimeStamp;
	}

	public static boolean isFlagThatSaysWeHavePopulatedFromDb() {
		refreshIfNeeded();
		return mFlagThatSaysWeHavePopulatedFromDb;
	}

	public static boolean isDirty() {
		refreshIfNeeded();
		return isDirty;
	}

	public static void setDirty(boolean isDirty) {
		refreshIfNeeded();
		GeneralPrefsDO.isDirty = isDirty;
	}

	private static void refreshIfNeeded() {
		if (!mFlagThatSaysWeHavePopulatedFromDb) {
			populateGeneralPrefs();
		}
	}

	private static void populateGeneralPrefs() {
		Cursor cursor = null;
		boolean isClean = false;

		try {
			cursor =
					AutomatonAlert.getProvider().query(
							AutomatonAlertProvider.GENERAL_PREFS_TABLE_URI,
							null, null, null, null);
			if (cursor != null && cursor.moveToFirst()) {
				GeneralPrefsDO.populate(cursor);
				isClean = true;
			}
		}
		catch (RemoteException | IllegalArgumentException e) {
			Log.e(TAG, ".populateGeneralPrefs(): " +
					"query exception: " + e.toString());
		} finally {
			if (!isClean) {
				GeneralPrefsDO.save();
			}
			if (cursor != null) {
				cursor.close();
			}
		}

		//davedel - force hardcoded values
		setGCPollAndExpired();
		//davedel
	}
}
