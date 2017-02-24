package com.aldersonet.automatonalert.Util;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Html;

import com.aldersonet.automatonalert.Activity.AccountAddUpdateActivity;
import com.aldersonet.automatonalert.Account.AccountDO;
import com.aldersonet.automatonalert.Account.Accounts;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Filter.FilterItemAccountDO;
import com.aldersonet.automatonalert.Filter.FilterItemDO;
import com.aldersonet.automatonalert.Filter.FilterItems;
import com.aldersonet.automatonalert.Fragment.RTUpdateFragment;
import com.aldersonet.automatonalert.Fragment.RTUpdateFragment.Mode;
import com.aldersonet.automatonalert.SourceAccount.SourceAccountDO;
import com.aldersonet.automatonalert.SourceType.SourceTypeDO;

import java.util.Arrays;

@SuppressLint("ValidFragment")
public class EmailAccountDialog extends DialogFragment {
	public String[] mAllAccountKeys;
	String[] mAllAccountNames;
	Object[] mAllStoredAccounts;
	boolean[] mSelectedAccountsBefore;
	boolean[] mSelectedAccountsAfter;

	RTUpdateFragment mFragment;
	Mode mMode;
	SourceTypeDO mSourceType;
	FilterItemDO mFilterItem;
	int mFilterItemId;
	String mDisplayName;

	public EmailAccountDialog(
			RTUpdateFragment fragment,
			String[] allAccountNames, String[] allAccountKeys,
			Mode mode, SourceTypeDO sourceType, FilterItemDO filterItem,
			String displayName) {
		super();

		mFragment = fragment;
		mMode = mode;
		mSourceType = sourceType;
		mFilterItem = filterItem;
		mFilterItemId = mFilterItem == null ? -1 : mFilterItem.getFilterItemId();
		mDisplayName = displayName;

		if (allAccountNames.length <= 0) {
			return;
		}

		// need to make sure the arrays are of the proper length
		int len = initializeNameAndKeyArrays(allAccountKeys, allAccountNames);

		// (takes into account there may be holes in the resulting array)
		if (mMode.equals(Mode.FREEFORM)) {
			mAllStoredAccounts =
					slotOrderedAccounts(
							mFilterItem.getAccounts().toArray(),
							mAllAccountKeys);
		}
		else {
			mAllStoredAccounts =
					slotOrderedAccounts(SourceAccountDO.getSourceTypeId(
									mSourceType.getSourceTypeId()).toArray(),
							mAllAccountKeys);
		}

		// create a list of currently active SourceAccounts
		mSelectedAccountsBefore = new boolean[len];

		// create boolean array depending on mAllStoredAccounts[i] being null
		int N= mAllStoredAccounts.length;
		for (int i=0;i<N;i++) {
			if (mAllStoredAccounts[i] != null) {
				mSelectedAccountsBefore[i] = true;
			}
		}
	}

	private int initializeNameAndKeyArrays(String[] origKeys, String[] origNames) {
		// need to make sure the arrays are of the proper length
		// find out where the non-null end is
		int len = origKeys.length;
		for ( ; len>0; len--) {
			if (origKeys[len-1] != null) {
				break;
			}
		}
		// right-sized arrays
		if (len != origKeys.length) {
			mAllAccountNames = Arrays.copyOf(origNames, len);
			mAllAccountKeys = Arrays.copyOf(origKeys, len);
		}
		else {
			mAllAccountNames = origNames;
			mAllAccountKeys = origKeys;
		}
		Arrays.sort(mAllAccountNames);
		Arrays.sort(mAllAccountKeys);

		return len;
	}

	private AccountDO getAccountId(Object[] array, int idx) {
		if (array[idx] instanceof SourceAccountDO) {
			return Accounts.get(((SourceAccountDO) array[idx]).getAccountId());
		}
		else if (array[idx] instanceof FilterItemAccountDO) {
			return Accounts.get(((FilterItemAccountDO)array[idx]).getAccountId());
		}
		return null;
	}


	private Object[] slotOrderedAccounts(
			Object[] accountsIn, String[] orderedKeys) {

		Object[] ret = new Object[orderedKeys.length];
		Arrays.fill(ret, null);

		int N=accountsIn.length;
		// place each original Source AccountDO into ret according to
		// the order of the entries in orderedKeys
		for (int i=0;i<N;i++) {
			if (accountsIn[i] == null) {
				continue;
			}
			AccountDO account = getAccountId(accountsIn, i);
			if (account != null) {
				String originalKey = account.getKey();
				int N2=orderedKeys.length;
				for (int j=0;j<N2;j++) {
					// see if they both have the same name
					if (originalKey.equals(orderedKeys[j])) {
						ret[j] = accountsIn[i];
						break;
					}
				}
			}
		}
		return ret;
	}

	private Object addNewAccount(int accountId) {
		Object acct = null;

		if (mMode.equals(Mode.FREEFORM)) {
			acct = new FilterItemAccountDO(mFilterItemId, accountId);
			((FilterItemAccountDO)acct).save();
		}
		else {
			acct = new SourceAccountDO(accountId, mSourceType.getSourceTypeId());
			((SourceAccountDO) acct).save();
		}

		return acct;
	}

	private void deleteOldAccount(Object acct) {
		if (acct != null) {
			if (acct instanceof SourceAccountDO) {
				((SourceAccountDO) acct).delete();
			}
			else if (acct instanceof FilterItemAccountDO) {
				((FilterItemAccountDO) acct).delete();
			}
		}
	}

	private void deleteOrSaveAccounts() {
		if (mAllAccountKeys == null) {
			return;
		}

		// delete newly unchecked, add newly checked
		AccountDO account = null;
		int N = mAllAccountKeys.length;
		for (int i=0;i<N;i++) {
			String key = mAllAccountKeys[i];
			account = Accounts.get(key);
			// selected, need a db item
			if (mSelectedAccountsAfter[i]) {
				if (!mSelectedAccountsBefore[i]) {
					// add newly-checked to db
					addNewAccount(account.getAccountId());
				}
			}
			else {
				// unselected, remove from db
				if (mSelectedAccountsBefore[i]) {
					// delete newly unchecked from db
					// (make sure it's there first)
					deleteOldAccount(mAllStoredAccounts[i]);
				}
			}
		}
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		// Where we track the selected items
		AlertDialog.Builder builder =
				new AlertDialog.Builder(getActivity());

		// Set the dialog title
		String msgPart = Utils.returnPluralOrOriginal(
				mAllAccountNames, "account", "s");
		builder.setTitle(
				"Alert if " + mDisplayName + " emails the selected " + msgPart + ".");
		if (mAllAccountNames == null) {
			builder.setMessage(Html.fromHtml(
					"<font color='#0099cc'>There are no email accounts"
							+ " registered. Please add an account.</font>"
			));
		}
		// Specify the list array, the items to be selected by default (null for none),
		// and the listener through which to receive callbacks when items are selected
		if (mSelectedAccountsBefore != null) {
			mSelectedAccountsAfter =
					Arrays.copyOf(
							mSelectedAccountsBefore,
							mSelectedAccountsBefore.length);
		}
		//
		builder.setMultiChoiceItems(
				// list of all accounts
				mAllAccountNames,
				// ordered list of checked accounts
				mSelectedAccountsAfter,
				new DialogInterface.OnMultiChoiceClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which,
					                    boolean isChecked) {
					}
				});
		// Go to settings to add an account
		builder.setPositiveButton(
				"Add Account", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (Utils.inAppUpgradeCheckAccountsAtLimit(getActivity())) {
							return;
						}
						Intent intent = new Intent(
								getActivity(), AccountAddUpdateActivity.class);
						intent.putExtra(AutomatonAlert.M_MODE, AutomatonAlert.ADD);
						startActivity(intent);
					}
				});

		// Process...
		builder.setNegativeButton("Done", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int id) {
				// add or delete sourceAccounts from/to db
				// depending on the results of the Dialog
				deleteOrSaveAccounts();
				mFragment.showEmailAccounts();
			}
		});

		return builder.create();
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		// mFilterItem's mAccounts has possibly changed, we
		// need to re-get so that our mFilterItem has the changes
		if (mMode.equals(Mode.FREEFORM)) {
			mFragment.reSetFilterItem(FilterItems.get(mFilterItemId));
		}

		super.onDismiss(dialog);
	}
}
