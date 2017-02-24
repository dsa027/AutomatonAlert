package com.aldersonet.automatonalert.Preferences;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.RemoteException;
import android.util.Log;

import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Provider.AutomatonAlertProvider;

import java.util.Date;

public class ContactListPrefsDO {

	public static final String TAG = "ContactListPrefsDO";

	private static int mContactListPrefsId = -1;
	private static boolean mShowText;
	private static boolean mShowPhone;
	private static boolean mShowEmail;
	private static Date mTimeStamp;

	public static boolean mFlagThatSaysWeHavePopulatedFromDb;
	private static boolean isDirty = false;

	static {
		initValues();
	}

	private static void initValues() {
		mFlagThatSaysWeHavePopulatedFromDb = false;

		mContactListPrefsId = -1;
		mShowText = true;
		mShowPhone = true;
		mShowEmail = true;

		isDirty = true;
	}

	public static void populate(Cursor cursor) {

		isDirty = false;

		mContactListPrefsId = cursor.getInt(
				cursor.getColumnIndex(AutomatonAlertProvider.CONTACT_LIST_PREFS_ID));

		mShowText = cursor.getString(
				cursor.getColumnIndex(
						AutomatonAlertProvider.CONTACT_LIST_PREFS_SHOW_TEXT))
				.equals(AutomatonAlert.TRUE);


		mShowPhone = cursor.getString(
				cursor.getColumnIndex(AutomatonAlertProvider.CONTACT_LIST_PREFS_SHOW_PHONE))
				.equals(AutomatonAlert.TRUE);

		mShowEmail = cursor.getString(
				cursor.getColumnIndex(AutomatonAlertProvider.CONTACT_LIST_PREFS_SHOW_EMAIL))
				.equals(AutomatonAlert.TRUE);

		long millis = cursor.getLong(
				cursor.getColumnIndex(AutomatonAlertProvider.CONTACT_LIST_PREFS_TIMESTAMP));
		if (millis != -1) {
			mTimeStamp = new Date(millis);
		}
		else {
			mTimeStamp = new Date(System.currentTimeMillis());
		}

		mFlagThatSaysWeHavePopulatedFromDb = true;
	}

	public static synchronized void save() {
		ContentValues cv = AutomatonAlertProvider.getContactListPrefsContentValues(
				mShowText,
				mShowPhone,
				mShowEmail

		);

		AutomatonAlertProvider aap =
				(AutomatonAlertProvider)AutomatonAlert.getProvider()
				.getLocalContentProvider();

        int id = aap == null ? -1 :
                aap.insertOrUpdate(
				cv,
				mContactListPrefsId,
				AutomatonAlertProvider.CONTACT_LIST_PREFS_ID_URI,
				AutomatonAlertProvider.CONTACT_LIST_PREFS_TABLE_URI);

		// if inserted, store new id
		if (id != -1
				&& id != mContactListPrefsId) {
			mContactListPrefsId = id;
		}

		isDirty = false;
	}

	public static boolean isDirty() {
		refreshIfNeeded();
		return isDirty;
	}

	public static void refreshIfNeeded() {
		if (!mFlagThatSaysWeHavePopulatedFromDb) {
			populateContactListPrefs();
		}
	}

	public static void populateContactListPrefs() {
		Cursor cursor = null;
		boolean isClean = false;

		try {
			cursor = AutomatonAlert.getProvider().query(
					AutomatonAlertProvider.CONTACT_LIST_PREFS_TABLE_URI,
					null, null, null, null);
			if (cursor.moveToFirst()) {
				ContactListPrefsDO.populate(cursor);
				isClean = true;
			}
		} catch (RemoteException e) {
			Log.e(TAG + ".populateContactListPrefs()",
					"query exception: " + e.toString());
		} catch (IllegalArgumentException e2) {
			Log.e(TAG + ".populateContactListPrefs()",
					"query exception: " + e2.toString());
		}
		finally {
			if (!isClean) {
				ContactListPrefsDO.save();
			}
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	public static int getContactListPrefsId() {
		refreshIfNeeded();
		return mContactListPrefsId;
	}

	public static void setContactListPrefsId(int mContactListPrefsId) {
		refreshIfNeeded();
		ContactListPrefsDO.mContactListPrefsId = mContactListPrefsId;
	}

	public static boolean isShowText() {
		refreshIfNeeded();
		return mShowText;
	}

	public static void setShowText(boolean mHideText) {
		refreshIfNeeded();
		ContactListPrefsDO.mShowText = mHideText;
	}

	public static boolean isShowPhone() {
		refreshIfNeeded();
		return mShowPhone;
	}

	public static void setShowPhone(boolean mHidePhone) {
		refreshIfNeeded();
		ContactListPrefsDO.mShowPhone = mHidePhone;
	}

	public static boolean isShowEmail() {
		refreshIfNeeded();
		return mShowEmail;
	}

	public static void setShowEmail(boolean mHideEmail) {
		refreshIfNeeded();
		ContactListPrefsDO.mShowEmail = mHideEmail;
	}
}
