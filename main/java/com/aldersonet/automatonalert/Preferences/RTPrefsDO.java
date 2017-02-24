package com.aldersonet.automatonalert.Preferences;

import android.content.ContentValues;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.aldersonet.automatonalert.Alert.AlertItemDO;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Fragment.VolumeChooserFragment.VolumeTypes;
import com.aldersonet.automatonalert.Provider.AutomatonAlertProvider;

import java.util.Date;

public class RTPrefsDO {

	public static final String TAG = "RTPrefsDO";

	public static final int UI_VOLUME_MAX = 7;
	public static final int UI_VOLUME_DEFAULT_INDEX = 8;
	public static final int INTERNAL_VOLUME_MAX = 100;

	private static int mContactRTPrefsId = -1;
	// default Default
	private static String mDefaultRingtone;
	private static String mDefaultVolume;
	// new RT
	private static String mDefaultNewSilentMode;
	private static int    mDefaultNewVolume;
	private static long	  mDefaultNewPlayFor;
	private static String mDefaultNewVibrateMode;
	private static boolean mDefaultNewNotification;
	private static String mDefaultNewLight;
	// default text RT
	private static String mDefaultTextRingtone;
	private static String mDefaultTextSilentMode;
	private static int    mDefaultTextVolume;
	private static long	  mDefaultTextPlayFor;
	private static String mDefaultTextVibrateMode;
	private static boolean mDefaultTextNotification;
	private static String mDefaultTextLight;
	// errors
	private static String mDefaultLinkedAccountSpecificPrefix;
	private static String mAutoAddNewAccountsToDefault;
	private static String mAutoAddNewAccountsToActive;
//	private static String mShowToastOnSmsMmsBlock;  // TODO: taken over by mRemindersOn
	private static boolean mRemindersOn;
	// timestamp
	private static Date mTimeStamp;

	public static boolean mFlagThatSaysWeHavePopulatedFromDb;
	private static boolean isDirty = false;

	static {
		initValues();
	}

	private static void initValues() {
		mFlagThatSaysWeHavePopulatedFromDb = false;

		mContactRTPrefsId = -1;
		mDefaultRingtone = VolumeTypes.alarm.name();
		mDefaultVolume = VolumeTypes.alarm.name();
		mDefaultNewSilentMode = "0"; 		// silent in silent mode
		mDefaultNewPlayFor = -1;			// 1 loop
		mDefaultNewVibrateMode = AutomatonAlert.NEVER;	// never vibrate
		mDefaultNewNotification = false;	// no notification
		mDefaultNewLight = "0";			    // no led light
		mDefaultNewVolume = UI_VOLUME_DEFAULT_INDEX;
		mDefaultTextRingtone = VolumeTypes.alarm.name();
		mDefaultTextSilentMode = "0"; 		// silent in silent mode
		mDefaultTextPlayFor = 2000;			// 2 seconds
		mDefaultTextVibrateMode = AutomatonAlert.NEVER;	// never vibrate
		mDefaultTextNotification = false;	// no notification
		mDefaultTextLight = "0";	        // no led light
		mDefaultTextVolume = UI_VOLUME_DEFAULT_INDEX;
		mDefaultLinkedAccountSpecificPrefix = "ContactRTPrefix";
		mAutoAddNewAccountsToDefault = "1";	// add new accounts to list of default accounts
		mAutoAddNewAccountsToActive = "1";	// add new account to all active contact RTs
//		mShowToastOnSmsMmsBlock = "0";		// TODO: taken over by mRemindersOn
		mRemindersOn = false;		        // notificatoin reminders off

		isDirty = true;
	}

	public static String setDefaultVolume(String sVol) {
		if (!mDefaultVolume.equals(sVol)) {
			isDirty = true;
		}
		return mDefaultVolume = sVol;
	}

	public static String getDefaultVolume() {
		return mDefaultVolume;
	}

	public static void populate(Cursor cursor) {

		isDirty = false;

		mContactRTPrefsId = cursor.getInt(
				cursor.getColumnIndex(AutomatonAlertProvider.CONTACT_RT_PREFS_ID));

		// default Default
		mDefaultRingtone = cursor.getString(
				cursor.getColumnIndex(AutomatonAlertProvider.CONTACT_RT_PREFS_DEFAULT_RINGTONE));

		mDefaultVolume = cursor.getString(
				cursor.getColumnIndex(AutomatonAlertProvider.CONTACT_RT_PREFS_DEFAULT_VOLUME));

		// new RT
		mDefaultNewSilentMode = cursor.getString(
				cursor.getColumnIndex(AutomatonAlertProvider.CONTACT_RT_PREFS_DEFAULT_NEW_SILENT_MODE));

		mDefaultNewVolume = cursor.getInt(
				cursor.getColumnIndex(AutomatonAlertProvider.CONTACT_RT_PREFS_DEFAULT_NEW_VOLUME));

		mDefaultNewPlayFor = cursor.getLong(
				cursor.getColumnIndex(AutomatonAlertProvider.CONTACT_RT_PREFS_DEFAULT_NEW_PLAY_FOR));

		mDefaultNewVibrateMode = cursor.getString(
				cursor.getColumnIndex(AutomatonAlertProvider.CONTACT_RT_PREFS_DEFAULT_NEW_VIBRATE_MODE));

		mDefaultNewNotification = AutomatonAlertProvider.stringToBoolean(cursor.getString(
				cursor.getColumnIndex(
						AutomatonAlertProvider.CONTACT_RT_PREFS_DEFAULT_NEW_NOTIFICATION)));

		mDefaultNewLight = cursor.getString(
				cursor.getColumnIndex(AutomatonAlertProvider.CONTACT_RT_PREFS_DEFAULT_NEW_LIGHT));

		// default text RT
		mDefaultTextRingtone = cursor.getString(
				cursor.getColumnIndex(AutomatonAlertProvider.CONTACT_RT_PREFS_DEFAULT_TEXT_RINGTONE));

		mDefaultTextSilentMode = cursor.getString(
				cursor.getColumnIndex(AutomatonAlertProvider.CONTACT_RT_PREFS_DEFAULT_TEXT_SILENT_MODE));

		mDefaultTextVolume = cursor.getInt(
				cursor.getColumnIndex(AutomatonAlertProvider.CONTACT_RT_PREFS_DEFAULT_TEXT_VOLUME));

		mDefaultTextPlayFor = cursor.getLong(
				cursor.getColumnIndex(AutomatonAlertProvider.CONTACT_RT_PREFS_DEFAULT_TEXT_PLAY_FOR));

		mDefaultTextVibrateMode = cursor.getString(
				cursor.getColumnIndex(AutomatonAlertProvider.CONTACT_RT_PREFS_DEFAULT_TEXT_VIBRATE_MODE));

		mDefaultTextNotification = AutomatonAlertProvider.stringToBoolean(cursor.getString(
				cursor.getColumnIndex(
						AutomatonAlertProvider.CONTACT_RT_PREFS_DEFAULT_TEXT_NOTIFICATION)));

		mDefaultTextLight = cursor.getString(
				cursor.getColumnIndex(AutomatonAlertProvider.CONTACT_RT_PREFS_DEFAULT_TEXT_LIGHT));

		// errors
		mDefaultLinkedAccountSpecificPrefix = cursor.getString(
				cursor.getColumnIndex(AutomatonAlertProvider.CONTACT_RT_PREFS_DEFAULT_LINKED_ACCOUNT_NAME_VALUE_DATA_PREFIX));

		mAutoAddNewAccountsToDefault = cursor.getString(
				cursor.getColumnIndex(AutomatonAlertProvider.CONTACT_RT_PREFS_AUTO_ADD_NEW_ACCOUNTS_TO_DEFAULT));

		mAutoAddNewAccountsToActive = cursor.getString(
				cursor.getColumnIndex(AutomatonAlertProvider.CONTACT_RT_PREFS_AUTO_ADD_NEW_ACCOUNTS_TO_ACTIVE));

		// this is correct
		mRemindersOn = AutomatonAlertProvider.stringToBoolean(cursor.getString(
				cursor.getColumnIndex(
						AutomatonAlertProvider.CONTACT_RT_PREFS_REMINDERS_ON)));

//		mShowToastOnSmsMmsBlock = cursor.getString(
//				cursor.getColumnIndex(AutomatonAlertProvider.CONTACT_RT_PREFS_REMINDERS_ON));

		long millis = cursor.getLong(
				cursor.getColumnIndex(AutomatonAlertProvider.CONTACT_RT_PREFS_TIMESTAMP));
		if (millis != -1) {
			mTimeStamp = new Date(millis);
		}
		else {
			mTimeStamp = new Date(System.currentTimeMillis());
		}

		mFlagThatSaysWeHavePopulatedFromDb = true;
	}

	public static Uri getDefaultRingtone(String inPath) {
		String sIn = inPath.toLowerCase();
		int type = RingtoneManager.TYPE_ALARM;

		// if it's empty or has a valid uri, just pass inPath back
		if (sIn.startsWith(AutomatonAlert.CONTENT_PREFIX) ||
				TextUtils.isEmpty(sIn)) {
			return Uri.parse(inPath);
		}
		if (RTPrefsDO.mDefaultRingtone.equals(VolumeTypes.ringtone.name())) {
			type = RingtoneManager.TYPE_RINGTONE;
		}
		else if (RTPrefsDO.mDefaultRingtone.equals(VolumeTypes.notification.name())) {
			type = RingtoneManager.TYPE_NOTIFICATION;
		}
		else if (RTPrefsDO.mDefaultRingtone.equals(VolumeTypes.alarm.name())) {
			type = RingtoneManager.TYPE_ALARM;
		}

		return RingtoneManager.getDefaultUri(type);
	}

	public static synchronized void save() {
		ContentValues cv = AutomatonAlertProvider.getContactRTPrefsContentValues(
				mDefaultRingtone,
				mDefaultVolume,
				mDefaultNewSilentMode,
				mDefaultNewPlayFor,
				mDefaultNewVibrateMode,
				mDefaultNewNotification,
				mDefaultNewLight,
				mDefaultNewVolume,
				mDefaultTextRingtone,
				mDefaultTextSilentMode,
				mDefaultTextPlayFor,
				mDefaultTextVibrateMode,
				mDefaultTextNotification,
				mDefaultTextLight,
				mDefaultTextVolume,
				mDefaultLinkedAccountSpecificPrefix,
				mAutoAddNewAccountsToDefault,
				mAutoAddNewAccountsToActive,
				mRemindersOn);

		AutomatonAlertProvider aap =
				(AutomatonAlertProvider)AutomatonAlert.getProvider()
				.getLocalContentProvider();

        int id = aap == null ? -1 :
                aap.insertOrUpdate(
				cv,
				mContactRTPrefsId,
				AutomatonAlertProvider.CONTACT_RT_PREFS_ID_URI,
				AutomatonAlertProvider.CONTACT_RT_PREFS_TABLE_URI);

		// if inserted, store new id
		if (id != -1
				&& id != mContactRTPrefsId) {
			mContactRTPrefsId = id;
		}

		// make sure the default text notification is up-to-date
		// with any changes made here
		AlertItemDO.createDefaultTextNotification();

		isDirty = false;
	}

	public static int getContactRTPrefsId() {
		refreshIfNeeded();
		return mContactRTPrefsId;
	}

	public static String getDefaultRingtone() {
		refreshIfNeeded();
		return mDefaultRingtone;
	}

	static void setDefaultRingtone(String mDefaultRingtone) {
		refreshIfNeeded();
		RTPrefsDO.mDefaultRingtone = mDefaultRingtone;
	}

	public static String getDefaultNewSilentMode() {
		refreshIfNeeded();
		return mDefaultNewSilentMode;
	}

	public static void setDefaultNewSilentMode(String mDefaultNewSilentMode) {
		refreshIfNeeded();
		RTPrefsDO.mDefaultNewSilentMode = mDefaultNewSilentMode;
	}

	public static long getDefaultNewPlayFor() {
		refreshIfNeeded();
		return mDefaultNewPlayFor;
	}

	public static void setDefaultNewPlayFor(long mDefaultNewPlayFor) {
		refreshIfNeeded();
		RTPrefsDO.mDefaultNewPlayFor = mDefaultNewPlayFor;
	}

	public static String getDefaultNewVibrateMode() {
		refreshIfNeeded();
		return mDefaultNewVibrateMode;
	}

	public static void setDefaultNewVibrateMode(String mDefaultNewVibrateMode) {
		refreshIfNeeded();
		RTPrefsDO.mDefaultNewVibrateMode = mDefaultNewVibrateMode;
	}

	public static boolean isDefaultNewNotification() {
		refreshIfNeeded();
		return mDefaultNewNotification;
	}

	public static void setDefaultNewNotification(boolean mDefaultNewNotification) {
		refreshIfNeeded();
		RTPrefsDO.mDefaultNewNotification = mDefaultNewNotification;
	}

	public static String getDefaultNewLight() {
		refreshIfNeeded();
		return mDefaultNewLight;
	}

	public static void setDefaultNewLight(String mDefaultNewLight) {
		refreshIfNeeded();
		RTPrefsDO.mDefaultNewLight = mDefaultNewLight;
	}

	public static int getDefaultNewVolume() {
		refreshIfNeeded();
		return mDefaultNewVolume;
	}

	public static void setDefaultNewVolume(int mDefaultNewVolume) {
		refreshIfNeeded();
		RTPrefsDO.mDefaultNewVolume = mDefaultNewVolume;
	}

	public static String getDefaultTextRingtone() {
		return mDefaultTextRingtone;
	}

	public static void setDefaultTextRingtone(String mDefaultTextRingtone) {
		RTPrefsDO.mDefaultTextRingtone = mDefaultTextRingtone;
	}

	public static int getDefaultTextVolume() {
		return mDefaultTextVolume;
	}

	public static void setDefaultTextVolume(int mDefaultTextVolume) {
		RTPrefsDO.mDefaultTextVolume = mDefaultTextVolume;
	}

	public static String getDefaultTextSilentMode() {
		return mDefaultTextSilentMode;
	}

	public static void setDefaultTextSilentMode(String mDefaultTextSilentMode) {
		RTPrefsDO.mDefaultTextSilentMode = mDefaultTextSilentMode;
	}

	public static long getDefaultTextPlayFor() {
		return mDefaultTextPlayFor;
	}

	public static void setDefaultTextPlayFor(long mDefaultTextPlayFor) {
		RTPrefsDO.mDefaultTextPlayFor = mDefaultTextPlayFor;
	}

	public static String getDefaultTextVibrateMode() {
		return mDefaultTextVibrateMode;
	}

	public static void setDefaultTextVibrateMode(String mDefaultTextVibrateMode) {
		RTPrefsDO.mDefaultTextVibrateMode = mDefaultTextVibrateMode;
	}

	public static boolean isDefaultTextNotification() {
		return mDefaultTextNotification;
	}

	public static void setDefaultTextNotification(boolean mDefaultTextNotification) {
		RTPrefsDO.mDefaultTextNotification = mDefaultTextNotification;
	}

	public static String getDefaultTextLight() {
		return mDefaultTextLight;
	}

	public static void setDefaultTextLight(String mDefaultTextLight) {
		RTPrefsDO.mDefaultTextLight = mDefaultTextLight;
	}

	public static String getDefaultLinkedAccountSpecificPrefix() {
		refreshIfNeeded();
		return mDefaultLinkedAccountSpecificPrefix;
	}

	public static void setDefaultLinkedAccountSpecificPrefix(
			String mDefaultLinkedAccountSpecificPrefix) {
		refreshIfNeeded();
		RTPrefsDO.mDefaultLinkedAccountSpecificPrefix = mDefaultLinkedAccountSpecificPrefix;
	}

	public static String getAutoAddNewAccountsToDefault() {
		refreshIfNeeded();
		return "1";
//		return mAutoAddNewAccountsToDefault;
	}

	public static void setAutoAddNewAccountsToDefault(
			String mAutoAddNewAccountsToDefault) {
		refreshIfNeeded();
		RTPrefsDO.mAutoAddNewAccountsToDefault = "1";//mAutoAddNewAccountsToDefault;
	}

	public static String getAutoAddNewAccountsToActive() {
		refreshIfNeeded();
		return "1";
//		return mAutoAddNewAccountsToActive;
	}

	public static void setAutoAddNewAccountsToActive(
			String mAutoAddNewAccountsToActive) {
		refreshIfNeeded();
		RTPrefsDO.mAutoAddNewAccountsToActive = "1";//mAutoAddNewAccountsToActive;
	}

//	public static String getDontShowErrorNoContactEmail() {
//		refreshIfNeeded();
//		return mAvailable1;
//	}

//	public static void setDontShowErrorNoContactEmail(
//			String mDontShowErrorNoContactEmail) {
//		refreshIfNeeded();
//		RTPrefsDO.mAvailable1 = mDontShowErrorNoContactEmail;
//	}

//	public static String getDontShowWarningBlockedSmsMms() {
//		refreshIfNeeded();
//		return mDontShowWarningBlockedSmsMms;
//	}

//	public static void setDontShowWarningBlockedSmsMms(
//			String mDontShowWarningBlockedSmsMms) {
//		refreshIfNeeded();
//		RTPrefsDO.mDontShowWarningBlockedSmsMms = mDontShowWarningBlockedSmsMms;
//	}

	public static boolean isRemindersOn() {
		refreshIfNeeded();
		return mRemindersOn;
	}

	static void setRemindersOn(boolean remindersOn) {
		refreshIfNeeded();
		RTPrefsDO.mRemindersOn = remindersOn;
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

	private static void refreshIfNeeded() {
		if (!mFlagThatSaysWeHavePopulatedFromDb) {
			populateContactRTPrefs();
		}
	}

	private static void populateContactRTPrefs() {
		Cursor cursor = null;
		boolean isClean = false;

		try {
			cursor = AutomatonAlert.getProvider().query(
					AutomatonAlertProvider.CONTACT_RT_PREFS_TABLE_URI,
					null, null, null, null);
			if (cursor != null && cursor.moveToFirst()) {
				RTPrefsDO.populate(cursor);
				isClean = true;
			}
		} catch (RemoteException | IllegalArgumentException e) {
			Log.e(TAG, ".populateContactRTPrefs(): " +
					"query exception: " + e.toString());
		} finally {
			if (!isClean) {
				RTPrefsDO.save();
			}
			if (cursor != null) {
				cursor.close();
			}
		}
	}

}
