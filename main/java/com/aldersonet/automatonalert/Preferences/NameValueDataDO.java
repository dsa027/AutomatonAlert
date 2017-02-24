package com.aldersonet.automatonalert.Preferences;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

import com.aldersonet.automatonalert.Account.AccountDO;
import com.aldersonet.automatonalert.Account.Accounts;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Provider.AutomatonAlertProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class NameValueDataDO {

	public static final String TAG = "NameValueDataDO";

	// Intstalled first time?
	// NEVER CHANGE THIS VALUE
	public static final String JUST_INSTALLED_FIRST_TIME                = "JustInstalled";

	// Tour/Version Features?
	public static final String USER_WAS_ASKED_TO_VIEW_TOUR              = "UserWasAskedToViewTour";
	public static final String USER_WAS_SHOWN_NEW_VERSION_FEATURES_VC15 = "UserWasShownNewVersionFeaturesVC15";

	// AlertList Errors/Warnings/Info
	public static final String ALERT_LIST_DELETE_DONT_SHOW 			    = "AlertListDeleteDontShow";
	public static final String ALERT_LIST_SAVE_DONT_SHOW 	            = "AlertListSaveDontShow";
	public static final String ALERT_LIST_CLEAR_REMINDER_DONT_SHOW 	    = "AlertListClearReminderDontShow";
	public static final String ALERT_LIST_CLEAR_SNOOZE_DONT_SHOW 		= "AlertListClearSnoozeDontShow";
	public static final String ALERT_LIST_CLEAR_REPEAT_DONT_SHOW 		= "AlertListClearRepeatDontShow";

	// Ringtone Errors/Warnings/Info
	public static final String RT_ERROR_NO_CONTACT_EMAIL_DONT_SHOW      = "RTErrorNoContactEmailDontShow";
	public static final String RT_WARNING_BLOCKED_MESSAGE_DONT_SHOW     = "RTWarningBlockedMessageDontShow";
	public static final String RT_WARNING_LONG_SONG_DURATION_DONT_SHOW  = "RTWarningLongSongDurationDontShow";
	public static final String RT_WARNING_ON_DELETE_DONT_SHOW           = "RTWarningOnDeleteDontShow";
	public static final String RT_WARNING_ON_FREEFORM_DELETE_DONT_SHOW  = "RTWarningOnFreeFormDeleteDontShow";
	public static final String RT_WARNING_ON_ALARM_DELETE_DONT_SHOW     = "RTWarningOnAlertDeleteDontShow";
	public static final String RT_ERROR_NO_ACCOUNTS_SET_UP_DONT_SHOW    = "RTErrorNoAccountsSetUpDontShow";

	public static final String DATABASE_VERSION                         = "DatabaseVersion";
	public static final String LAST_ASKED_TO_RATE_APP                   = "LastAskedToRateApp";
	public static final String SOUND_BOMB_DEQUE_DEAD                    = "SoundBombDequeDead";

	public static final long ASK_AGAIN_TO_RATE_APP_INTERVAL             = 14 * 24 * 60 * 60 * 1000;

	private int mNameValueDataId = -1;
	private String mName;
	private String mValue;
	private Date mTimeStamp;

	public boolean isDirty;

	public NameValueDataDO() {
		super();

		mNameValueDataId = -1;
		mName = "";
		mValue = "";

		isDirty = true;

	}

	public NameValueDataDO(String name, String value) {
		this();
		mName = name;
		mValue = value;
	}

	public int getNameValueDataId() {
		return mNameValueDataId;
	}

	public String getName() {
		return mName;
	}

	public String setName(String name) {
		if (!mName.equals(name)) {
			isDirty = true;
		}
		return mName = name;
	}

	public String getValue() {
		return mValue;
	}

	public String setValue(String value) {
		if (!mValue.equals(value)) {
			isDirty = true;
		}
		return mValue = value;
	}

	public static void set(String name, String value) {
		NameValueDataDO nv = get(name, null);

		if (nv != null) {
			nv.setValue(value);
		}
		else {
			nv = new NameValueDataDO(name, value);
		}
		nv.save();
	}

	public static NameValueDataDO get(String name, String value) {
		ArrayList<NameValueDataDO> list = get(name, false, value);
		if (list.size() > 0) {
			return list.get(0);
		}
		return null;
	}

	public static ArrayList<NameValueDataDO> get(
			String prefix, boolean startsWith, String value/*send null if not used!*/) {

		Uri uri = AutomatonAlertProvider.NAME_VALUE_DATA_TABLE_URI;
		String[] projection = {
				AutomatonAlertProvider.NAME_VALUE_DATA_ID,
				AutomatonAlertProvider.NAME_VALUE_DATA_NAME,
				AutomatonAlertProvider.NAME_VALUE_DATA_VALUE,
				AutomatonAlertProvider.NAME_VALUE_DATA_TIMESTAMP,
				};
		String selection = null;
		String[] args = null;
		// prefix search
		if (startsWith) {
			selection = AutomatonAlertProvider.NAME_VALUE_DATA_NAME +
					" LIKE '" + prefix + "%'";
		}
		// prefix equal
		else {
			selection = AutomatonAlertProvider.NAME_VALUE_DATA_NAME + " = ?";
			args = new String[] { prefix };
		}
		// get using pref too
		if (value != null) {
			selection +=
					" AND " + AutomatonAlertProvider.NAME_VALUE_DATA_VALUE + " = ?";
			if (args == null) {
				args = new String[] { value };
			}
			else {
				Arrays.copyOf(args, args.length+1);
				args[args.length-1] = value;
			}
		}

		ArrayList<NameValueDataDO> specPrefs = new ArrayList<NameValueDataDO>();

		Cursor cursor = null;
		try {
			cursor = AutomatonAlert.getProvider().query(
					uri, projection, selection, args, null);
			if (cursor.moveToFirst()) {
				do {
//					if (AutomatonAlert.DEBUG) {
//						Utils.dumpCursor("SpecPrefs", cursor);
//					}
					specPrefs.add(new NameValueDataDO().populate(cursor));
				}
				while (cursor.moveToNext());
			}
		}
		catch (RemoteException ignored) {}
		catch (IllegalArgumentException ignored) {}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return specPrefs;	// null if not found
	}

	public static void replaceAllPrefix(String prefix, String[] names) {
		// delete all prefix entries
		ArrayList<NameValueDataDO> specPrefs = delete(prefix);
		// save names of deleted
		String[] deletedNames = new String[specPrefs.size()];
		deletedNames = names;
		// if not all names were added...
		int shouldBe = names.length;
		if (addAll(names, prefix) < shouldBe) {
			// delete what did get added
			delete(prefix);
			// add initial deleted that we saved (revert)
			addAll(deletedNames, prefix);
		}
	}

	private static ArrayList<NameValueDataDO> delete(String prefix) {
		ArrayList<NameValueDataDO> specPrefs = get(
				prefix, true /*startsWith*/, null/*pref value*/);
		for (NameValueDataDO specPref : specPrefs) {
			specPref.delete();
		}
		return specPrefs;
	}

	private static int addAll(String[] names, String prefix) {
		int count = 0;
		int suffix = 0;
		for (String name : names) {
			AccountDO account = Accounts.get(name);
			count++;
			if (account != null) {
				String sSuffix = String.format(Locale.getDefault(), "%012d", suffix);
				NameValueDataDO specPref = new NameValueDataDO(
						prefix + sSuffix,
						"" + account.getAccountId());
				specPref.save();
				suffix++;
			}
		}
		return count;
	}

	public static ArrayList<NameValueDataDO> get() {
		ArrayList<NameValueDataDO> list = new ArrayList<NameValueDataDO>();

		NameValueDataDO specificPrefs = null;

        Cursor cursor = null;
        try {
            cursor = AutomatonAlert.getProvider().query(
                    AutomatonAlertProvider.NAME_VALUE_DATA_TABLE_URI,
                    null,
                    null,
                    null,
                    null);
        } catch (RemoteException e) {
            cursor = null;
            e.printStackTrace();
        }
        try {
			if (cursor != null && cursor.moveToFirst()) {
				do {
					specificPrefs = new NameValueDataDO().populate(cursor);
					list.add(specificPrefs);
				} while (cursor.moveToNext());
			}
		}
		catch (IllegalArgumentException ignored) {}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return list;
	}

	public synchronized void delete() {
		try {

			Uri uri = ContentUris.withAppendedId(
					AutomatonAlertProvider.NAME_VALUE_DATA_ID_URI, mNameValueDataId);
			AutomatonAlert.getProvider().delete(uri, null, null);

		} catch (RemoteException e) {
			Log.e(TAG + ".delete()", "delete exception: " + e.toString());
		}
	}

	public NameValueDataDO populate(Cursor cursor) {

		isDirty = false;

		mNameValueDataId = cursor.getInt(
				cursor.getColumnIndex(AutomatonAlertProvider.NAME_VALUE_DATA_ID));

		mName = cursor.getString(
				cursor.getColumnIndex(AutomatonAlertProvider.NAME_VALUE_DATA_NAME));

		mValue = cursor.getString(
				cursor.getColumnIndex(AutomatonAlertProvider.NAME_VALUE_DATA_VALUE));

		long millis = cursor.getLong(
				cursor.getColumnIndex(AutomatonAlertProvider.NAME_VALUE_DATA_TIMESTAMP));
		if (millis != -1) {
			mTimeStamp = new Date(millis);
		}
		else {
			mTimeStamp = new Date(System.currentTimeMillis());
		}

		return this;
	}

	public synchronized void save() {
		ContentValues cv = AutomatonAlertProvider.getNameValueDataContentValues(
				mName,
				mValue
				);

		AutomatonAlertProvider aap =
				(AutomatonAlertProvider)AutomatonAlert.getProvider()
				.getLocalContentProvider();

        int id = aap == null ? -1 :
                aap.insertOrUpdate(
				cv,
				mNameValueDataId,
				AutomatonAlertProvider.NAME_VALUE_DATA_ID_URI,
				AutomatonAlertProvider.NAME_VALUE_DATA_TABLE_URI);

		// if inserted, store new id
		if (id != -1
				&& id != mNameValueDataId) {
			mNameValueDataId = id;
		}

		isDirty = false;
	}

	public static boolean getJustInstalledFirstTime(boolean markInstalled) {
		// there'll be no db rec if jut installed
		boolean justInstalled =
				NameValueDataDO.get(JUST_INSTALLED_FIRST_TIME, null) == null;

		// make sure there's a db rec
		if (justInstalled
				&& markInstalled) {
			NameValueDataDO.set(JUST_INSTALLED_FIRST_TIME, AutomatonAlert.TRUE);
		}

		return justInstalled;
	}

	public static boolean hasBeenAskedToViewTour() {
		// asked = true if there's already a rec in the db
		boolean asked =
				NameValueDataDO.get(USER_WAS_ASKED_TO_VIEW_TOUR, null) != null;

		// add the rec
		if (!asked) {
			NameValueDataDO.set(USER_WAS_ASKED_TO_VIEW_TOUR, AutomatonAlert.TRUE);
		}

		return asked;
	}

	public static boolean hasBeenShownNewVersionFeatures() {
		// asked = true if there's already a rec in the db
		boolean asked =
				NameValueDataDO.get(USER_WAS_SHOWN_NEW_VERSION_FEATURES_VC15, null) != null
				;

		// add the rec
		if (!asked) {
			NameValueDataDO.set(USER_WAS_SHOWN_NEW_VERSION_FEATURES_VC15, AutomatonAlert.TRUE);
		}

		return asked;
	}

	public static void dontShowUserToRateAppEverAgain() {
		// the following is why ASK_AGAIN_TO_RATE_APP_INTERNAL is used here
		// Utils.isItTimeToAskUserToRateApp()
		// 		long nextAsk = lastAsked + NameValueDataDO.ASK_AGAIN_TO_RATE_APP_INTERVAL;
		//      " - 1000" is strictly due to the author's cynical view on life
		String value = (Long.MAX_VALUE - ASK_AGAIN_TO_RATE_APP_INTERVAL - 1000) + "";

		NameValueDataDO nv = NameValueDataDO.get(LAST_ASKED_TO_RATE_APP, null);
		if (nv == null) {
			nv = new NameValueDataDO(LAST_ASKED_TO_RATE_APP, value);
		}
		else {
			nv.setValue(value);
		}
		nv.save();
	}

}
