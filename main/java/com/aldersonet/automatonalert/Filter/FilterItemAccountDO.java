package com.aldersonet.automatonalert.Filter;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;

import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Provider.AutomatonAlertProvider;

import java.util.Date;

public class FilterItemAccountDO {

	private int mSeq;
	private int mFilterItemId;
	private int mAccountId;
	private Date mTimeStamp;

	public boolean isDirty;

	public FilterItemAccountDO() {
		super();

		mSeq = -1;
		mFilterItemId = -1;
		mAccountId = -1;
		mTimeStamp = new Date(System.currentTimeMillis());

		isDirty = true;
	}

	public FilterItemAccountDO(int filterItemId, int accountId) {
		this();

		mSeq = -1;
		mFilterItemId = filterItemId;
		mAccountId = accountId;

		isDirty = true;
	}

	public void delete() {

		Uri uri = ContentUris.withAppendedId(
				AutomatonAlertProvider.FILTER_ITEM_ACCOUNT_ID_URI, mSeq);

		try {
			AutomatonAlert.getProvider().delete(uri, null, null);
		} catch (RemoteException ignored) {}
	}

	public int getFilterItemAccountId() {
		return mSeq;
	}

	public int getFilterItemId() {
		return mFilterItemId;
	}

	public int setFilterItemId(int filterItemId) {
		if (mFilterItemId != filterItemId) {
			isDirty = true;
		}
		mFilterItemId = filterItemId;

		return filterItemId;
	}

	public int getAccountId() {
		return mAccountId;
	}

	public int setAccountId(int accountId) {
		if (mAccountId != accountId) {
			isDirty = true;
		}
		mAccountId = accountId;

		return accountId;
	}

	public Date getTimeStamp() {
		return mTimeStamp;
	}

	public FilterItemAccountDO populate(Cursor cursor) {
		isDirty = false;

		mSeq = cursor.getInt(cursor.getColumnIndex(
				AutomatonAlertProvider.FILTER_ITEM_ACCOUNT_ID));

		mFilterItemId = cursor.getInt(cursor.getColumnIndex(
				AutomatonAlertProvider.FILTER_ITEM_ACCOUNT_FILTER_ITEM_ID));

		mAccountId = cursor.getInt(cursor.getColumnIndex(
				AutomatonAlertProvider.FILTER_ITEM_ACCOUNT_ACCOUNT_ID));

		long millis = cursor.getLong(
				cursor.getColumnIndex(AutomatonAlertProvider.FILTER_ITEM_TIMESTAMP));
		if (millis != -1) {
			mTimeStamp = new Date(millis);
		}
		else {
			mTimeStamp = new Date(System.currentTimeMillis());
		}

		return this;
	}

	public void save() {
		ContentValues cv = AutomatonAlertProvider.getFilterItemAccountContentValues(
				mFilterItemId,
				mAccountId
				);

		AutomatonAlertProvider aap =
				(AutomatonAlertProvider)AutomatonAlert.getProvider()
						.getLocalContentProvider();

		// filter item account
        int id = aap == null ? -1 :
                aap.insertOrUpdate(
				cv,
				mSeq,
				AutomatonAlertProvider.FILTER_ITEM_ACCOUNT_ID_URI,
				AutomatonAlertProvider.FILTER_ITEM_ACCOUNT_TABLE_URI);

		// if inserted, store new id
		if (id != -1
				&& id != mSeq) {
			mSeq = id;
		}

		isDirty = false;
	}
}
