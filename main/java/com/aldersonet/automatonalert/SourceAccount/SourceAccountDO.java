package com.aldersonet.automatonalert.SourceAccount;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

import com.aldersonet.automatonalert.Account.AccountDO;
import com.aldersonet.automatonalert.Account.Accounts;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Preferences.NameValueDataDO;
import com.aldersonet.automatonalert.Provider.AutomatonAlertProvider;
import com.aldersonet.automatonalert.Util.Utils;

import java.util.ArrayList;
import java.util.Date;

public class SourceAccountDO {

	public static final String TAG = "SourceAccountDO";

	private int mSourceAccountId = -1;
	private int mAccountId;
	private int mSourceTypeId;
	private Date mTimeStamp;

	public boolean isDirty;

	public SourceAccountDO() {
		super();

		mSourceAccountId = -1;
		mAccountId = -1;
		mSourceTypeId = -1;

		isDirty = true;

	}

	public SourceAccountDO(int accountId, int sourceTypeId) {
		this();
		mAccountId = accountId;
		mSourceTypeId = sourceTypeId;
	}

	public int getSourceAccountId() {
		return mSourceAccountId;
	}

	public int getAccountId() {
		return mAccountId;
	}

	public int setAccountId(int id) {
		if (mAccountId != id) {
			isDirty = true;
		}
		return mAccountId = id;
	}

	public int getSourceTypeId() {
		return mSourceTypeId;
	}

	public int setSourceTypeId(int id) {
		if (mSourceTypeId != id) {
			isDirty = true;
		}
		return mSourceTypeId = id;
	}

	public static SourceAccountDO get(int id) {

		Uri uri = ContentUris.withAppendedId(
				AutomatonAlertProvider.SOURCE_ACCOUNT_ID_URI, id);

		SourceAccountDO sourceAccount = null;

		Cursor cursor = null;
		try {
			cursor = AutomatonAlert.getProvider().query(
					uri, null, null, null, null);
			if (cursor != null && cursor.moveToFirst()) {
				sourceAccount = new SourceAccountDO().populate(cursor);
			}
		}
		catch (RemoteException | IllegalArgumentException ignored) {} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return sourceAccount;	// null if not found
	}

	// using both accountId and sourceTypeId will return 0 or 1 SourceAccountDO
	// using accountId and sourceTypeId(-1) will return 0..n
	public static ArrayList<SourceAccountDO> get(int accountId, int sourceTypeId) {
		ArrayList<SourceAccountDO> sourceAccounts = new ArrayList<>();

		// if sourceTypeId < 0, only get by accountId
		String selection = AutomatonAlertProvider.SOURCE_ACCOUNT_ACCOUNT_ID + " = ?";
		String[] args = null;

		if (sourceTypeId >= 0) {
			// by accountId and sourceTypeId
			selection +=
					" AND " + AutomatonAlertProvider.SOURCE_ACCOUNT_SOURCE_TYPE_ID + " = ?";
			args = new String[] { accountId + "" , sourceTypeId + "" };
		}
		else {
			// by accountId only
			args = new String[] { accountId + "" };
		}

        Cursor cursor = null;
        try {
            cursor = AutomatonAlert.getProvider().query(
                    AutomatonAlertProvider.SOURCE_ACCOUNT_TABLE_URI,
                    null,
                    selection,
                    args,
                    null);
        } catch (RemoteException e) {
            cursor = null;
            e.printStackTrace();
        }
        try {
			if (cursor != null && cursor.moveToFirst()) {
				do {
					sourceAccounts.add(new SourceAccountDO().populate(cursor));
				} while (cursor.moveToNext());
			}
		}
		catch (IllegalArgumentException ignored) {}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return sourceAccounts;
	}

	public static ArrayList<SourceAccountDO> getSourceTypeId(int sourceTypeId) {
		ArrayList<SourceAccountDO> list = new ArrayList<>();

        Cursor cursor = null;
        try {
            cursor = AutomatonAlert.getProvider().query(
                    AutomatonAlertProvider.SOURCE_ACCOUNT_TABLE_URI,
                    null,
                    AutomatonAlertProvider.SOURCE_ACCOUNT_SOURCE_TYPE_ID + " = ?",
                    new String[] { sourceTypeId + "" },
                    null);
        } catch (RemoteException e) {
            cursor = null;
            e.printStackTrace();
        }
        try {
			if (cursor != null && cursor.moveToFirst()) {
				do {
					SourceAccountDO sourceAccount = new SourceAccountDO();
					sourceAccount.populate(cursor);
					list.add(sourceAccount);
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

	public static ArrayList<SourceAccountDO> getViaAccountId(int sourceTypeId) {
		ArrayList<SourceAccountDO> list = new ArrayList<>();

        Cursor cursor = null;
        try {
            cursor = AutomatonAlert.getProvider().query(
                    AutomatonAlertProvider.SOURCE_ACCOUNT_TABLE_URI,
                    null,
                    AutomatonAlertProvider.SOURCE_ACCOUNT_SOURCE_TYPE_ID + " = ?",
                    new String[] { sourceTypeId + "" },
                    null);
        } catch (RemoteException e) {
            cursor = null;
            e.printStackTrace();
        }
        try {
			if (cursor != null && cursor.moveToFirst()) {
				do {
					SourceAccountDO sourceAccount = new SourceAccountDO();
					sourceAccount.populate(cursor);
					list.add(sourceAccount);
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

	public static ArrayList<SourceAccountDO> get() {
		ArrayList<SourceAccountDO> list = new ArrayList<>();

        Cursor cursor = null;
        try {
            cursor = AutomatonAlert.getProvider().query(
                    AutomatonAlertProvider.SOURCE_ACCOUNT_TABLE_URI,
                    null, null, null, null);
        } catch (RemoteException e) {
            cursor = null;
            e.printStackTrace();
        }
        try {
			if (cursor != null && cursor.moveToFirst()) {
				do {
					SourceAccountDO sourceAccount = new SourceAccountDO();
					sourceAccount.populate(cursor);
					list.add(sourceAccount);
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

	public static ArrayList<SourceAccountDO> delete(int accountId) {
		ArrayList<SourceAccountDO> sourceAccounts = null;
		sourceAccounts = get(accountId, -1);
		for (SourceAccountDO sourceAccount : sourceAccounts) {
			sourceAccount.delete();
		}
		return sourceAccounts;
	}

	public synchronized int delete() {
		int count = 0;
		try {

			Uri uri = ContentUris.withAppendedId(
					AutomatonAlertProvider.SOURCE_ACCOUNT_ID_URI, mSourceAccountId);
			count = AutomatonAlert.getProvider().delete(uri, null, null);

		} catch (RemoteException e) {
			Log.e(TAG, ".delete(): delete exception: " + e.toString());
		}
		return count;
	}

	public static void addAll(ArrayList<NameValueDataDO> specPrefs, int sourceTypeId) {
		ArrayList<SourceAccountDO> sourceAccounts = null;
		if (sourceTypeId >= 0) {
			for (NameValueDataDO specPref : specPrefs) {
				int accountId = Utils.getInt(specPref.getValue(), -1);
				AccountDO account = (accountId == -1) ? null : Accounts.get(accountId);
				if (account != null) {
					accountId = account.getAccountId();
					if (accountId >= 0) {
						sourceAccounts =
								SourceAccountDO.get(accountId, sourceTypeId);
						// add only if SourceAccountDO isn't already in db
						if (sourceAccounts.size() <= 0) {
							SourceAccountDO sourceAccount =
									new SourceAccountDO(accountId, sourceTypeId);
							sourceAccount.save();
						}
					}
				}
			}
		}
	}

	public SourceAccountDO populate(Cursor cursor) {

		isDirty = false;

		mSourceAccountId = cursor.getInt(
				cursor.getColumnIndex(AutomatonAlertProvider.SOURCE_ACCOUNT_ID));

		mAccountId = cursor.getInt(
				cursor.getColumnIndex(AutomatonAlertProvider.SOURCE_ACCOUNT_ACCOUNT_ID));

		mSourceTypeId = cursor.getInt(
				cursor.getColumnIndex(AutomatonAlertProvider.SOURCE_ACCOUNT_SOURCE_TYPE_ID));

		long millis = cursor.getLong(
				cursor.getColumnIndex(AutomatonAlertProvider.SOURCE_ACCOUNT_TIMESTAMP));
		if (millis != -1) {
			mTimeStamp = new Date(millis);
		}
		else {
			mTimeStamp = new Date(System.currentTimeMillis());
		}

		return this;
	}

	public synchronized void save() {
		ContentValues cv = AutomatonAlertProvider.getSourceAccountContentValues(
				mAccountId,
				mSourceTypeId
				);

		AutomatonAlertProvider aap =
				(AutomatonAlertProvider)AutomatonAlert.getProvider()
				.getLocalContentProvider();

        int id = aap == null ? -1 :
                aap.insertOrUpdate(
				cv,
				mSourceAccountId,
				AutomatonAlertProvider.SOURCE_ACCOUNT_ID_URI,
				AutomatonAlertProvider.SOURCE_ACCOUNT_TABLE_URI);

		// if inserted, store new id
		if (id != -1
				&& id != mSourceAccountId) {
			mSourceAccountId = id;
		}

		isDirty = false;
	}

}
