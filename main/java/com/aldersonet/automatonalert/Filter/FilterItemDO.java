package com.aldersonet.automatonalert.Filter;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.text.TextUtils;

import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Provider.AutomatonAlertProvider;
import com.aldersonet.automatonalert.Util.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class FilterItemDO {

	private int mFilterItemId;
	private String[] mFieldNames;
	private String mPhrase;
	private int mNotificationItemId;
	private List<FilterItemAccountDO> mAccounts;
	private Date mTimeStamp;

	public static final String TAG_FILTER_ITEM_ID = "mFilterItemId";

	public boolean isDirty;

	public FilterItemDO() {
		super();

		mFilterItemId = -1;
		mFieldNames = new String[] { "Subject", "Message" };
		mPhrase = "";
		mNotificationItemId = -1;
		mAccounts = new ArrayList<FilterItemAccountDO>();
		mTimeStamp = new Date(System.currentTimeMillis());

		isDirty = true;
	}

	public FilterItemDO(int filterItemId, String fieldNames, String phrase,
			int notificationItemId, List<FilterItemAccountDO> accounts) {
		this();

		mFilterItemId = filterItemId;
		setFieldNames(fieldNames);
		mPhrase = phrase;
		mNotificationItemId = notificationItemId;
		if (accounts != null) {
			mAccounts = accounts;
		}
		else {
			mAccounts = FilterItemAccounts.getFilterItemId(mFilterItemId);
		}

		isDirty = true;
	}

	public void delete() {
		Uri uri = ContentUris.withAppendedId(
				AutomatonAlertProvider.FILTER_ITEM_ID_URI, mFilterItemId);

		try {
			// delete FilterItemAccount first, then FilterItem
			FilterItemAccounts.delete(mAccounts);
			AutomatonAlert.getProvider().delete(uri, null, null);
		} catch (RemoteException ignored) {}
	}

	public boolean hasFieldName(String fieldName) {
		if (mFieldNames == null) {
			return false;
		}

		for (String field : mFieldNames) {
			// "*" matches any
			if (field.equals("*")) {
				return true;
			}
			if (field.equalsIgnoreCase(fieldName)) {
				return true;
			}
		}
		return false;
	}

	public boolean equal(FilterItemDO filterItem) {
		// order doesn't matter
		return Utils.equals(getFieldNamesArray(), filterItem.getFieldNamesArray())
				&& mPhrase.equals(filterItem.mPhrase)
				&& mNotificationItemId == filterItem.mNotificationItemId;
	}

	public int getFilterItemId() {
		return mFilterItemId;
	}

	public String getPhrase() {
		return mPhrase;
	}

	public String setPhrase(String phrase) {
		if (mPhrase == null) {
			isDirty = true;
		}
		else if (!(mPhrase.equals(phrase))) {
			isDirty = true;
		}
		mPhrase = phrase.trim();

		return phrase;
	}

	public String getFieldNames() {
		return mergeEachField();
	}

	private String mergeEachField() {
		boolean first = true;
		String s = "";

		if (mFieldNames == null) {
			return "";
		}

		for (String field : mFieldNames) {
			if (first) {
				first = false;
			}
			else {
				s += ",";
			}
			s += field;
		}

		return s;
	}

	public String getSortedFieldNames(boolean isForDisplay) {
		String out = "";

		if (hasFieldName("*")) {
			return "All Fields";
		}

		for (String header : AutomatonAlert.HEADERS_FOR_VIEW) {
			for (String name : mFieldNames) {
				if (name.equalsIgnoreCase(header)) {
					if (!TextUtils.isEmpty(out)) {
						out += ", ";
					}
					if (isForDisplay
							&& name.equals(AutomatonAlert.SMS_BODY)) {
						out += "Text Msg";       // match what's in the layout
					}
					else {
						out += name;
					}
					break;
				}
			}
		}

		return out;
	}

	public String[] getFieldNamesArray() {
		return mFieldNames;
	}

	public String setFieldNames(String fieldNames) {
		if (mFieldNames == null
				|| (!(mergeEachField().equals(fieldNames)))) {
			isDirty = true;
		}
		mFieldNames = fieldNames.split(",");

		return fieldNames;
	}

	public int getNotificationItemId() {
		return mNotificationItemId;
	}

	public int setNotificationItemId(int notificationItemId) {
		if (mNotificationItemId != notificationItemId) {
			isDirty = true;
		}
		mNotificationItemId = notificationItemId;

		return notificationItemId;
	}

	public List<FilterItemAccountDO> getAccounts() {
		if (mAccounts == null) {
			mAccounts = new ArrayList<FilterItemAccountDO>();
		}
		return mAccounts;
	}

	public void addToAccounts(int accountId) {
		// see if it's in the array first
		FilterItemAccountDO item = new FilterItemAccountDO(mFilterItemId, accountId);
		if (!FilterItemAccounts.has(mAccounts, item)) {
			mAccounts.add(item);
			item.save();
		}
	}

	public Date getTimeStamp() {
		return mTimeStamp;
	}

	public FilterItemDO populate(Cursor cursor) {
		isDirty = false;

		mFilterItemId = cursor.getInt(cursor.getColumnIndex(
				AutomatonAlertProvider.FILTER_ITEM_ID));

		setFieldNames(cursor.getString(cursor.getColumnIndex(
				AutomatonAlertProvider.FILTER_ITEM_FIELD_NAMES)));

		mPhrase = cursor.getString(cursor.getColumnIndex(
				AutomatonAlertProvider.FILTER_ITEM_PHRASE));

		mNotificationItemId = cursor.getInt(cursor.getColumnIndex(
				AutomatonAlertProvider.FILTER_ITEM_NOTIFICATION_ITEM_ID));

		long millis = cursor.getLong(
				cursor.getColumnIndex(AutomatonAlertProvider.FILTER_ITEM_TIMESTAMP));
		if (millis != -1) {
			mTimeStamp = new Date(millis);
		}
		else {
			mTimeStamp = new Date(System.currentTimeMillis());
		}

		mAccounts = FilterItemAccounts.getFilterItemId(mFilterItemId);

		return this;
	}

	public void save() {
		ContentValues cv = AutomatonAlertProvider.getFilterItemContentValues(
				getFieldNames(),
				mPhrase,
				mNotificationItemId
				);

		AutomatonAlertProvider aap =
				(AutomatonAlertProvider)AutomatonAlert.getProvider()
						.getLocalContentProvider();

		// filter item
        int id = aap == null ? -1 :
                aap.insertOrUpdate(
				cv,
				mFilterItemId,
				AutomatonAlertProvider.FILTER_ITEM_ID_URI,
				AutomatonAlertProvider.FILTER_ITEM_TABLE_URI);

		// if inserted, store new id
		if (id != -1
				&& id != mFilterItemId) {
			mFilterItemId = id;
		}

		// filter item account
		FilterItemAccounts.save(mFilterItemId, mAccounts);

		isDirty = false;
	}
}
