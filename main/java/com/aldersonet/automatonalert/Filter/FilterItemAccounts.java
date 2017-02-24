package com.aldersonet.automatonalert.Filter;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;

import com.aldersonet.automatonalert.Account.AccountDO;
import com.aldersonet.automatonalert.Account.Accounts;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Preferences.NameValueDataDO;
import com.aldersonet.automatonalert.Provider.AutomatonAlertProvider;
import com.aldersonet.automatonalert.SMS.AccountSmsDO;
import com.aldersonet.automatonalert.Util.Utils;

import java.util.ArrayList;
import java.util.List;


public class FilterItemAccounts {

	private FilterItemAccounts() {
		super();

	}

	public static boolean has(
			List<FilterItemAccountDO> list, FilterItemAccountDO source) {

		// look through items for item
		for (FilterItemAccountDO fia : list) {
			if (fia.getAccountId() == source.getAccountId()) {
				if (fia.getFilterItemId() == source.getFilterItemId()) {
					return true;
				}
			}
		}

		return false;
	}

	public static FilterItemAccountDO get(int id) {

		if (id == -1) {
			return null;
		}

		Uri uri = ContentUris.withAppendedId(
				AutomatonAlertProvider.FILTER_ITEM_ACCOUNT_ID_URI, id);

		FilterItemAccountDO filterItemAccount = null;

		Cursor cursor = null;
		try {
			cursor = AutomatonAlert.getProvider().query(
					uri, null, null, null, null);
			if (cursor.moveToFirst()) {
				filterItemAccount = new FilterItemAccountDO().populate(cursor);
			}
		}
		catch (RemoteException ignored) {}
		catch (IllegalArgumentException ignored) {}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return filterItemAccount;	// null if not found
	}

	public static List<FilterItemAccountDO> get() {

		ArrayList<FilterItemAccountDO> list = new ArrayList<FilterItemAccountDO>();

		Cursor cursor = null;
		try {
			cursor = AutomatonAlert.getProvider().query(
					AutomatonAlertProvider.FILTER_ITEM_ACCOUNT_TABLE_URI,
					null,
					null,
					null,
					null);

			if (cursor.moveToFirst()) {
				do {
					list.add((new FilterItemAccountDO()).populate(cursor));

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

	public static List<FilterItemAccountDO> getFilterItemId(int filterItemId) {
		ArrayList<FilterItemAccountDO> list = new ArrayList<FilterItemAccountDO>();

		if (filterItemId == -1) {
			return list;
		}

		String where =
				AutomatonAlertProvider.FILTER_ITEM_ACCOUNT_FILTER_ITEM_ID
				+ " = ?";
		String[] args = { filterItemId + "" };

		Cursor cursor = null;
		try {
			cursor = AutomatonAlert.getProvider().query(
					AutomatonAlertProvider.FILTER_ITEM_ACCOUNT_TABLE_URI,
					null,
					where,
					args,
					AutomatonAlertProvider.FILTER_ITEM_ACCOUNT_ACCOUNT_ID + " ASC");

			if (cursor.moveToFirst()) {
				do {
					list.add((new FilterItemAccountDO()).populate(cursor));

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

	public static List<FilterItemAccountDO> getAccountId(int accountId) {

		ArrayList<FilterItemAccountDO> list = new ArrayList<FilterItemAccountDO>();

		if (accountId == -1) {
			return list;
		}

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
					AutomatonAlertProvider.FILTER_ITEM_ACCOUNT_ACCOUNT_ID + " ASC");

			if (cursor.moveToFirst()) {
				do {
					list.add((new FilterItemAccountDO()).populate(cursor));

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

	public static void addAll(ArrayList<NameValueDataDO> specPrefs, int filterItemId) {
		if (filterItemId >= 0) {
			// always add SMS
			AccountDO accountSMS = Accounts.get(AccountSmsDO.SMS_KEY);
			if (accountSMS != null) {
				specPrefs.add(new NameValueDataDO("", accountSMS.getAccountId() + ""));
			}

			List<FilterItemAccountDO> filterItemAccounts =
					FilterItemAccounts.getFilterItemId(filterItemId);

			for (NameValueDataDO specPref : specPrefs) {
				int accountId = Utils.getInt(specPref.getValue(), -1);
				AccountDO account = (accountId == -1) ? null : Accounts.get(accountId);
				if (account != null) {
					accountId = account.getAccountId();
					if (accountId >= 0) {
						// add only if FilterItemAccountDO isn't already in db
						FilterItemAccountDO filterItemAccount =
								new FilterItemAccountDO(filterItemId, accountId);
						if (!has(filterItemAccounts, filterItemAccount)) {
							filterItemAccount.save();
						}
					}
				}
			}
		}
	}

	public static void delete(int filterItemId) {
		if (filterItemId == -1) {
			return;
		}
		// retrieve all then call delete on the list
		delete(FilterItemAccounts.getFilterItemId(filterItemId));
	}

	public static void delete(List<FilterItemAccountDO> list) {
		if (list == null) {
			return;
		}
		for (FilterItemAccountDO item : list) {
			item.delete();
		}
	}

	/* Doing this the lazy way...delete all then add all */
	/* Pass in filterItemId in param rather than search  */
	/* for it in the passed list                         */
	public static void save(int filterItemId, List<FilterItemAccountDO> list) {
		// delete all
		if (filterItemId != -1) {
			delete(filterItemId);
		}

		// save all
		for (FilterItemAccountDO item : list) {
			item.save();
		}
	}
}
