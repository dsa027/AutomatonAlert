package com.aldersonet.automatonalert.Account;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Pair;

import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Email.AccountEmailDO;
import com.aldersonet.automatonalert.Preferences.NameValueDataDO;
import com.aldersonet.automatonalert.Provider.AutomatonAlertProvider;
import com.aldersonet.automatonalert.SMS.AccountSmsDO;
import com.aldersonet.automatonalert.Util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class Accounts {

	private Accounts() {
		super();
	}

	public static AccountDO get(final int id) {

		Uri uri = ContentUris.withAppendedId(
				AutomatonAlertProvider.ACCOUNT_ID_URI, id);

		AccountDO account = null;

		Cursor cursor = null;
		try {
			cursor = AutomatonAlert.getProvider().query(
					uri, null, null, null, null);
			if (cursor != null && cursor.moveToFirst()) {
				account = populateAccount(cursor);
			}
		}
		catch (RemoteException | IllegalArgumentException ignored) {
        } finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return account;	// null if not found
	}

	public static AccountDO get(final String key) {

		if (key == null) {
			return null;
		}

		String sKey = key;

		// if it's not a key yet, make it one
		if (!(sKey.contains("|"))) {
			sKey += "|" + key;
		}

		String[] sParts = sKey.split("\\|");

		String accountName =
				(sParts[1].equals(AccountSmsDO.SMS_NAME) ?
						sParts[1]
						:
						sParts[0]);

		String where =
				AutomatonAlertProvider.ACCOUNT_NAME
				+ " = ?";
		String[] args = { accountName };

		if (!(sParts[1].equals(AccountSmsDO.SMS_NAME))
				&& sParts.length >= 2) {
			where +=
					" AND "
					+ AutomatonAlertProvider.ACCOUNT_EMAIL_ADDRESS
					+ " = ?";
			args = new String[] { accountName, sParts[1] };
		}

		AccountDO account = null;
		Cursor cursor = null;

		try {
			cursor = AutomatonAlert.getProvider().query(
					AutomatonAlertProvider.ACCOUNT_TABLE_URI,
					null,
					where,
					args,
					AutomatonAlertProvider.ACCOUNT_NAME
						+ " ASC, "
						+ AutomatonAlertProvider.ACCOUNT_EMAIL_ADDRESS
						+ " ASC");

			if (cursor != null && cursor.moveToFirst()) {
				account = populateAccount(cursor);
			}
		}
		catch (RemoteException | IllegalArgumentException ignored) {}
        finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return account;
	}

	public static ArrayList<AccountDO> get() {

		ArrayList<AccountDO> list = new ArrayList<>();

		Cursor cursor = null;
		try {
			cursor = AutomatonAlert.getProvider().query(
					AutomatonAlertProvider.ACCOUNT_TABLE_URI,
					null,
					null,
					null,
					null);

			if (cursor != null && cursor.moveToFirst()) {
				do {
					list.add(populateAccount(cursor));

				} while (cursor.moveToNext());
			}
		}
		catch (RemoteException | IllegalArgumentException ignored) {}
        finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return list;
	}

	public static ArrayList<AccountDO> getByAccountType(int accountType) {

		ArrayList<AccountDO> list = new ArrayList<>();

		Cursor cursor = null;
		try {
			cursor = AutomatonAlert.getProvider().query(
					AutomatonAlertProvider.ACCOUNT_TABLE_URI,
					null,
					null,
					null,
					null);

			if (cursor != null && cursor.moveToFirst()) {
				do {
					AccountDO account = populateAccount(cursor);
					if (account.mAccountType == accountType) {
						list.add(account);
					}

				} while (cursor.moveToNext());
			}
		}
		catch (RemoteException | IllegalArgumentException ignored) {} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return list;
	}

	public static Pair<String[], String[]> getAllAccountsNameKeyArrays(int accountType) {
		ArrayList<AccountDO> acctList = null;

		if (accountType == AccountDO.ACCOUNT_GENERIC) {
			// get all
			acctList = Accounts.get();
		}
		else {
			// get only the type specified
			acctList = Accounts.getByAccountType(accountType);
		}

		if (acctList == null) {
			acctList = new ArrayList<>(0);
		}
		ArrayList<String> nameList = new ArrayList<>(0);
		ArrayList<String> keyList = new ArrayList<>(0);
		for (AccountDO acct : acctList) {
			if (acct != null) {
				nameList.add(acct.getName());
				keyList.add(acct.getKey());
			}
		}
		// from ArrayList to String[]
		String[] sNameList =
				nameList.subList(0, nameList.size()).toArray(new String[(nameList.size())]);
		String[] sKeyList =
				keyList.subList(0, keyList.size()).toArray(new String[(keyList.size())]);
		if (sNameList.length > 0) {
			Arrays.sort(sNameList);
			Arrays.sort(sKeyList);
		}
		return new Pair<>(sNameList, sKeyList);
	}

	private static AccountDO populateAccount(Cursor cursor) {
		String name =
				cursor.getString(cursor.getColumnIndex(
						AutomatonAlertProvider.ACCOUNT_NAME));

		if (name.equals(AccountSmsDO.SMS_NAME)) {
			return (new AccountSmsDO()).populate(cursor);
		}

		String email =
				cursor.getString(cursor.getColumnIndex(
						AutomatonAlertProvider.ACCOUNT_NAME));

		if (!(TextUtils.isEmpty(email))) {
			return (new AccountEmailDO()).populate(cursor);
		}

		return (new AccountDO()).populate(cursor);
	}

	public static class DefaultComparator<T extends AccountDO> implements Comparator<T> {
		@Override
		public int compare(T i1, T i2) {
			return i1.getKey().compareToIgnoreCase(i2.getKey());
		}
	}

	public static String[] getKeyFromSpecPrefs(ArrayList<NameValueDataDO> specPrefs) {
		String[] keys = new String[specPrefs.size()];
		AccountDO account = null;
		int i=0;
		for (NameValueDataDO specPref : specPrefs) {
			int accountNum = Utils.getInt(specPref.getValue(), -1);
			if (accountNum == -1) continue;

			account = get(accountNum);
			if (account != null) {
				keys[i++] = account.getName();
			}
		}
		return keys;
	}

//	////////////////////////////
//	// get(id) CACHE
//	////////////////////////////
//
//	public static final Cache mCache =
//			new Cache<AccountDO, Accounts, Integer>(5, new Accounts());
//
//	public static AccountDO get(int accountId) {
//		return (AccountDO)mCache.get(accountId);
//	}
//
//	@Override
//	public AccountDO getCacheRecFromSource(Object id) {
//		return getFromDb((Integer) id);
//	}

}
