package com.aldersonet.automatonalert.Filter;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;

import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Provider.AutomatonAlertProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;


public class FilterItems {

	public static final String TAG = "FilterItems";

	private FilterItems() {
		super();

	}

	public static boolean has(
			Collection<FilterItemDO> filterItems, FilterItemDO filterItem) {

		for (FilterItemDO fi : filterItems) {
			if (fi.equal(filterItem)) {
				return true;
			}
		}
		return false;
	}

	public static FilterItemDO get(int id) {

		Uri uri = ContentUris.withAppendedId(
				AutomatonAlertProvider.FILTER_ITEM_ID_URI, id);

		FilterItemDO filterItem = null;

		Cursor cursor = null;
		try {
			cursor = AutomatonAlert.getProvider().query(uri, null, null, null, null);
			if (cursor.moveToFirst()) {
				filterItem = new FilterItemDO().populate(cursor);
			}
		}
		catch (RemoteException ignored) {}
		catch (IllegalArgumentException ignored) {}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return filterItem;	// null if not found
	}

	public static ArrayList<FilterItemDO> get() {

		ArrayList<FilterItemDO> list = new ArrayList<FilterItemDO>();

		Cursor cursor = null;
		try {
			cursor = AutomatonAlert.getProvider().query(
					AutomatonAlertProvider.FILTER_ITEM_TABLE_URI,
					null,
					null,
					null,
					AutomatonAlertProvider.FILTER_ITEM_PHRASE + " ASC");

			if (cursor.moveToFirst()) {
				do {
					FilterItemDO item = new FilterItemDO().populate(cursor);
					list.add(item);

//					if (BuildConfig.DEBUG) {
////						Utils.dumpCursor(TAG + ".get()", cursor);
//						for (FilterItemAccountDO fiAccount : item.getAccounts()) {
//							if (fiAccount != null
//									&& fiAccount.getAccountId() != -1) {
//								AccountDO account = Accounts.get(fiAccount.getAccountId());
//								if (account != null) {
//									Log.d(
//											TAG + ".get()",
//											"Account[" + account.getName() + "]");
//								}
//							}
//						}
//					}

				} while (cursor.moveToNext());
			}
		}
		catch (RemoteException ignored) {}
		catch (IllegalArgumentException ignored) {}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return list;
	}

	public static ArrayList<FilterItemDO> getAccountId(int accountId) {

		ArrayList<FilterItemDO> list = new ArrayList<FilterItemDO>();

		String where =
				AutomatonAlertProvider.FILTER_ITEM_ACCOUNT_ACCOUNT_ID
				+ " = ?";
		String[] args = { accountId + "" };

		Cursor cursor = null;
		try {
			cursor = AutomatonAlert.getProvider().query(
					AutomatonAlertProvider.FILTER_ITEM_ACCOUNT_TABLE_URI,
					null,
					where,
					args,
					AutomatonAlertProvider.FILTER_ITEM_ACCOUNT_ID + " ASC");

			if (cursor.moveToFirst()) {
				do {
					// use FilterItemAccount to get the filter
					// item associated with accountId
					FilterItemAccountDO accountItem =
							new FilterItemAccountDO().populate(cursor);
					FilterItemDO item = get(accountItem.getFilterItemId());
					if (item != null) {
						list.add(item);
					}

				} while (cursor.moveToNext());
			}
		}
		catch (RemoteException ignored) {}
		catch (IllegalArgumentException ignored) {}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return list;
	}

	public static FilterItemDO getNotificationItemId(int id) {
		FilterItemDO filterItem = null;

		String where =
				AutomatonAlertProvider.FILTER_ITEM_NOTIFICATION_ITEM_ID
				+ " = ?";

		Cursor cursor = null;
		try {
			cursor = AutomatonAlert.getProvider().query(
					AutomatonAlertProvider.FILTER_ITEM_TABLE_URI,
					null,
					where,
					new String[] { id + "" },
					null);

			if (cursor.moveToFirst()) {
				do {
					filterItem = new FilterItemDO().populate(cursor);

				} while (cursor.moveToNext());
			}
		}
		catch (RemoteException ignored) {}
		catch (IllegalArgumentException ignored) {}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return filterItem;
	}

	public static FilterItemDO setPhrase(String phrase, int id) {

		FilterItemDO filterItem = get(id);

		if (filterItem != null) {
			filterItem.setPhrase(phrase);
			filterItem.save();
		}

		return filterItem;
	}

	public static ArrayList<FilterItemDO> getFiltersWithFieldName(
			String fieldName, int accountId) {
        Log.d(TAG, ".getFiltersWithFieldName(): fieldName: " + fieldName + ", acctId: " + accountId);

		ArrayList<FilterItemDO> filterItemList = new ArrayList<FilterItemDO>();

		// return empty set if fieldName is a field we don't search
		for (String noSearch : AutomatonAlert.HEADERS_DONT_SEARCH) {
			if (fieldName.equalsIgnoreCase(noSearch)) {
				return filterItemList;		// return empty filterItemList
			}
		}

		// look in each FilterItemDO for the header
		// if it's there, store the FilterItemId to be returned
		ArrayList<FilterItemDO> filterItems = getAccountId(accountId);
		for (FilterItemDO filterItem : filterItems) {
//			Log.d(TAG, ".getFiltersWithFieldName(): Looking at " + filterItem.getPhrase());
			if (filterItem.hasFieldName(fieldName)) {
				filterItemList.add(filterItem);
				Log.d(TAG, ".getFiltersWithFieldName(): " +
						"Found a filterItem, Phrase[" + filterItem.getPhrase() + "]");
			}
		}

		return filterItemList;
	}

	public static class DefaultComparator<T extends FilterItemDO> implements Comparator<T> {

		@Override
		public int compare(T i1, T i2) {
			return i1.getFilterItemId() - i2.getFilterItemId();
		}
	}

}
