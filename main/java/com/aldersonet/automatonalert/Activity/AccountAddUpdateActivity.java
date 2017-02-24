package com.aldersonet.automatonalert.Activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.aldersonet.automatonalert.Account.AccountDO;
import com.aldersonet.automatonalert.Account.Accounts;
import com.aldersonet.automatonalert.ActionBar.ActionBarDrawer;
import com.aldersonet.automatonalert.Activity.RTUpdateActivity.FragmentTypeRT;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Email.AccountEmailDO;
import com.aldersonet.automatonalert.Email.EmailGet;
import com.aldersonet.automatonalert.Email.EmailGetException;
import com.aldersonet.automatonalert.Email.EmailGetSemaphore;
import com.aldersonet.automatonalert.Filter.FilterItemAccountDO;
import com.aldersonet.automatonalert.Filter.FilterItemAccounts;
import com.aldersonet.automatonalert.Filter.FilterItemDO;
import com.aldersonet.automatonalert.Filter.FilterItems;
import com.aldersonet.automatonalert.OkCancel.OkCancel;
import com.aldersonet.automatonalert.Preferences.NameValueDataDO;
import com.aldersonet.automatonalert.Preferences.RTPrefsDO;
import com.aldersonet.automatonalert.Preferences.SettingsFragment;
import com.aldersonet.automatonalert.Preferences.SettingsFragment.ShowMode;
import com.aldersonet.automatonalert.R;
import com.aldersonet.automatonalert.SourceAccount.SourceAccountDO;
import com.aldersonet.automatonalert.SourceType.SourceTypeDO;
import com.aldersonet.automatonalert.Util.EmailAddress;
import com.aldersonet.automatonalert.Util.Utils;

import org.apache.k9.AccountSettingsUtils;
import org.apache.k9.AccountSettingsUtils.Provider;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

public class AccountAddUpdateActivity extends AppCompatActivity {

	public static final String TAG = "AccountAddUpdateActivity";

	AlertDialog mAlertDialog;
	ProgressDialog mProgressDialog;
	boolean mValidInfo;
	String mProblem;
	AccountEmailDO mAccountEmail;
	Mode mMode;
	EmailGetSemaphore mEmailSemaphore;

	Intent mThisActivityIntent;

	enum Mode {
		ADD("Account Add"),
		UPDATE("Account Update");
        private String title;
        Mode(String title) {
            this.title = title;
        }
        public String getTitle() { return title; }
	}

	String mAccountKey;
	String mOldPassword;
	String mOldServer;

	EditText mEmail;
	EditText mServer;
	EditText mPassword;

	Button mFindServerButton;
	Button mNextButton;
	TextView mSearchForServer;

	ActionBarDrawer mActionBarDrawer;

    @Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (mActionBarDrawer != null) {
			mActionBarDrawer.getDrawerToggle().onConfigurationChanged(newConfig);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mActionBarDrawer != null) {
			if (mActionBarDrawer.getDrawerToggle().onOptionsItemSelected(item)) {
				return true;
			}
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mActionBarDrawer.getDrawerToggle().syncState();
	}

	private void setDrawer() {
		mActionBarDrawer = new ActionBarDrawer(this);
	}

	@Override
	public boolean onKeyUp(int keyCode, @NotNull KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			mActionBarDrawer.openDrawer();
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	private void setMode() {
		mMode = Mode.ADD;
		mAccountEmail = null;

		try {
			if (mThisActivityIntent != null) {
				if (mThisActivityIntent.getStringExtra(
						AutomatonAlert.M_MODE).equals(AutomatonAlert.UPDATE)) {
					mAccountKey = mThisActivityIntent.getStringExtra(
							AutomatonAlert.ACCOUNT_KEY);
					AccountDO account =
							Accounts.get(mAccountKey);
					if (account != null) {
						if (account.mAccountType == AccountEmailDO.ACCOUNT_EMAIL) {
							mAccountEmail = (AccountEmailDO) account;
							mMode = Mode.UPDATE;
						}
					}
				}
			}
		} catch (NullPointerException ignore) {}
	}

	private void setFields(boolean isAdd) {
		mPassword = (EditText)findViewById(R.id.asm_password_edittext);
		mServer = (EditText)findViewById(R.id.asm_imap_server_edittext);

		mFindServerButton = (Button)findViewById(R.id.asm_findserver_button);
		mFindServerButton.setOnClickListener(new FindServerOnClickListener());

		mSearchForServer = (TextView)findViewById(R.id.asm_find_server_http);
		mSearchForServer.setOnClickListener(new SearchForServerOnClickListener());

		mNextButton = (Button)findViewById(R.id.asm_next_button);
		mNextButton.setOnClickListener(new NextOnClickListener());

		mPassword.setOnFocusChangeListener(new EmailFocusChangeListener());

		if (isAdd) {
			/////////
			// ADD //
			/////////
			mOldPassword = "";
			mOldServer = "";

			mEmail = (EditText)findViewById(R.id.asm_email_edittext);
			mEmail.requestFocus();
			mEmail.setOnFocusChangeListener(new EmailFocusChangeListener());
		}
		else {
			////////////
			// UPDATE //
			////////////
			mOldPassword = mAccountEmail.getPassword();
			mOldServer = mAccountEmail.getServer();

			mEmail = (EditText)findViewById(R.id.asm_email_edittext);
			String[] split = mAccountKey.split("\\|");
			mEmail.setText(split[0]);
			mEmail.setFocusable(false);
			mEmail.setFocusableInTouchMode(false);
			mEmail.setClickable(false);

			mPassword.requestFocus();

			mServer.setText(mAccountEmail.getServer());
		}
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mThisActivityIntent = getIntent();
		setMode();

		if (mMode.equals(Mode.ADD)) {
			getWindow().setTitle(Mode.ADD.getTitle());
			setContentView(R.layout.account_add_update);
			setFields(true/*isAdd*/);
		}
		else {
			getWindow().setTitle(Mode.UPDATE.getTitle());
			setContentView(R.layout.account_add_update);
			setFields(false/*isAdd*/);
		}

		Utils.setActionBarCommon(
				getResources(), getSupportActionBar(), "Account Registration");
		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		setDrawer();
	}

	class EmailFocusChangeListener implements TextView.OnFocusChangeListener {
		@Override
		public void onFocusChange(View v, boolean hasFocus) {
			if (!hasFocus) {
				if (!TextUtils.isEmpty(((EditText)v).getText().toString())) {
					new FindServerOnClickListener().onClick(null);
				}
			}
		}
	}

	class FindServerOnClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			Provider provider = null;
			EmailAddress emailAddress =
					new EmailAddress(mEmail.getText().toString());

			Toast toast = Toast.makeText(
					AccountAddUpdateActivity.this, "", Toast.LENGTH_SHORT);

			if (emailAddress.isValid()) {
				provider = AccountSettingsUtils.findProviderForDomain(
						AccountAddUpdateActivity.this,
						emailAddress.getDomain(),
						R.xml.providers);
				if (provider != null) {
					mServer.setText(provider.incomingUriTemplate.getHost());
					toast.setText("Server name found");
				}
				else {
					toast.setText(
							"An IMAP server wasn't found. Try tapping the Web search link.");
				}
			}
			else {
				toast.setText("You need to enter a valid email address");
			}
			// if v == null then this onClick() was
			// called programmatically, so don't Toast
			if (v != null) {
				toast.show();
			}
		}
	}

	class SearchForServerOnClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			EmailAddress email = new EmailAddress(mEmail.getText().toString());

			if (!email.isValid()) {
				Toast.makeText(
						AccountAddUpdateActivity.this,
						"Please enter a valid email address before searching",
						Toast.LENGTH_LONG)
				.show();
				return;
			}

			Intent intent =
					new Intent(Intent.ACTION_WEB_SEARCH);
			String query = "IMAP server name " + email.getDomain();
			intent.putExtra(SearchManager.QUERY, query);
			startActivity(intent);
		}
	}

	class NextOnClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			EmailAddress emailAddress =
					new EmailAddress(mEmail.getText().toString());
			String password = mPassword.getText().toString();
			String server = mServer.getText().toString();

			// populate if it's not already
			if (TextUtils.isEmpty(server)) {
				new FindServerOnClickListener().onClick(null);
			}

			mValidInfo = false;
			mProblem = null;

			if (mMode.equals(Mode.UPDATE)) {
				if (!(password.equals(""))) {
					mAccountEmail.setPassword(password);
				}
				if (!(server.equals(""))) {
					mAccountEmail.setServer(server);
				}
				doProcessing();
			}
			else { // ADDING
				if (!emailAddress.isValid()) {
					mProblem = "The email address format is not correct";
					mValidInfo = false;
					asyncPostExecute();
				}
				else if (TextUtils.isEmpty(password)
						|| TextUtils.isEmpty(server)) {
					mProblem = "Please fill in both Password and Server";
					mValidInfo = false;
					asyncPostExecute();
				}
				else {
					mAccountEmail = new AccountEmailDO(
							emailAddress.getEmailAddress(),
							emailAddress.getEmailAddress(),
							mPassword.getText().toString(),
							mServer.getText().toString());
					if (AutomatonAlert.isDuplicateAccount(mAccountEmail)) {
						mAccountEmail = null;
						mProblem = "This account already exists";
						mValidInfo = false;
						asyncPostExecute();
					}
					else {
						// put it in db and process (deleted later if error)
						mAccountEmail.save();
						doProcessing();
					}
				}
			}
		}
	}

	private void doProcessing() {
		AsyncTask<String, String, String> at =
				new AsyncTask<String, String, String>() {

			@Override
			protected void onPreExecute() {
				mProgressDialog = ProgressDialog.show(
						AccountAddUpdateActivity.this,
						"",
						"Validating account information...",
						true);
			}
			@Override
			protected String doInBackground(String... str) {
				EmailGet mailHandle = null;

				if (mMode.equals(Mode.UPDATE)) {
					// don't test if nothing has changed
					if (mAccountEmail.getPassword().equals(mOldPassword)
							&& mAccountEmail.getServer().equalsIgnoreCase(mOldServer)) {
						mValidInfo = true;
						return "";
					}
				}

				// it's in the db even if it's new, so need to tryAcquire
				if (AutomatonAlert.getMailGetSemaphores().tryAcquire(mAccountEmail.getKey())) {
					mEmailSemaphore =
							AutomatonAlert.getMailGetSemaphores().get(mAccountEmail.getKey());
					mailHandle = mEmailSemaphore.mMailHandle;
					// mailHandle.mAccount will be null since it's
					// not yet in the database, hand over our
					// AccountDO object so that it can do its work
					mailHandle.mAccount = mAccountEmail;

					try {
						mailHandle.login();
						mValidInfo = true;
					} catch (IOException e) {
						mProblem =
								"Unable to connect to the email server. "
								+ "Please be sure it's an IMAP server.";
						mValidInfo = false;
					} catch (EmailGetException e) {
						mProblem = "Unable to login using the email and password provided";
						mValidInfo = false;
					} finally {
						mailHandle.close();
					}
				}
				return "";
			}

			@Override
			protected void onPostExecute(String str) {
				if (mProgressDialog != null) {
					mProgressDialog.dismiss();
				}
				asyncPostExecute();
			}

		};
		//.execute(null, null, null);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
		    at.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, "", "", "");
		}
		else {
			at.execute("", "", "");
		}
	}

	private void asyncPostExecute() {
		// no matter what, we need to release the semaphore
		if (mAccountEmail != null) {
			AutomatonAlert.getMailGetSemaphores().release(mAccountEmail.getKey());
		}

		if (!mValidInfo) {
			if (mMode.equals(Mode.UPDATE)) {
				mAccountEmail.setPassword(mOldPassword);
				mAccountEmail.setServer(mOldServer);
			}
			showProblemAlertDialog();
			if (mMode.equals(Mode.ADD)) {
				if (mAccountEmail != null) {
					mAccountEmail.delete();
				}
			}
		}
		else {
			// If requested, add this account to all active Contact Ringtones
			// (add SourceAccount with this Account if there's an EMAIL SourceType
			if (mMode.equals(Mode.ADD)) {
				addNewAccountToSourceAndFilterItemAccounts();
				addNewAccountToDefaultRegisteredAccounts();
			}
			showAccountAddedUpdatedDialog();
		}
	}

	private void addNewAccountToSourceAndFilterItemAccounts() {
		// add to SourceAccounts
		if (RTPrefsDO.getAutoAddNewAccountsToDefault().equals("1")) {
			// get all EMAIL SourceTypeDO. for each,
			// add a SourceAccountDO(AccountDO, SourceTypeDO)
			addNewAccountToSourceAccounts();
			addNewAccountToFilterItemAccounts();
		}
	}

	private void addNewAccountToSourceAccounts() {
		ArrayList<SourceTypeDO> sourceTypes =
				SourceTypeDO.get(FragmentTypeRT.EMAIL);
		for (SourceTypeDO sourceType : sourceTypes) {
			// see if it's already in the database
			ArrayList<SourceAccountDO> sourceAccount =
					SourceAccountDO.get(
							mAccountEmail.mAccountId,
							sourceType.getSourceTypeId());
			// not in db, create and save
			if (sourceAccount.size() <= 0) {
				new SourceAccountDO(
						mAccountEmail.mAccountId, sourceType.getSourceTypeId())
						.save();
			}
		}
	}

	private void addNewAccountToFilterItemAccounts() {
		ArrayList<FilterItemDO> items = FilterItems.get();
		for (FilterItemDO item : items) {
			// if account isn't part of FilterItem, add it
			FilterItemAccountDO account =
					new FilterItemAccountDO(item.getFilterItemId(), mAccountEmail.mAccountId);
			if (!FilterItemAccounts.has(item.getAccounts(), account)) {
				account.save();
			}
		}
	}

	private void addNewAccountToDefaultRegisteredAccounts() {
		// if requested, add this account to the list of default
		// registered accounts
		if (RTPrefsDO.getAutoAddNewAccountsToActive().equals("1")) {
			// add to default accounts in NameValueDataDO
			ArrayList<NameValueDataDO> specPrefs =
					NameValueDataDO.get(
							RTPrefsDO.getDefaultLinkedAccountSpecificPrefix(),
							true/*startsWith*/,
							null/*pref value*/);
			String names[] = Accounts.getKeyFromSpecPrefs(specPrefs);
			names = Arrays.copyOf(names, names.length+1);
			names[names.length-1] = mAccountEmail.getName();
			NameValueDataDO.replaceAllPrefix(
					RTPrefsDO.getDefaultLinkedAccountSpecificPrefix(),
					names);
		}
	}

	private void showAccountAddedUpdatedDialog() {
		String message = "";
		boolean problem = false;

		mAccountEmail.save();

		if (mMode.equals(Mode.UPDATE)) {
			String whatChanged = "";
			if (!(mAccountEmail.getServer().equalsIgnoreCase(mOldServer))) {
				whatChanged = "server";
			}
			if (!(mAccountEmail.getPassword().equals(mOldPassword))) {
				if (!(whatChanged.equals(""))) {
					whatChanged += ", ";
				}
				whatChanged += "password";
			}
			if (whatChanged.equals("")) {
				message = "Nothing changed.";
				problem = true;
			}
			else {
				message = "Success, "
						+ whatChanged
						+ " changed.";
			}
		}
		else { // ADDING
			mAccountKey = mAccountEmail.getKey();
			message = "Success! Account added.";
			// set first scan to only new emails
			OkCancel okCancel =
					SettingsFragment.getOkCancelResetAccountToTop(
							mAccountKey, false/*toastIt*/);
			okCancel.doRightButtonPressed();
//			if (BuildConfig.DEBUG) {
//				AccountEmailDO account = (AccountEmailDO)Accounts.get(mAccountKey);
//				if (account != null) {
//					Log.d(TAG + ".showAccountAdded...()",
//							"Account[" + account.getName() + "]"
//							+ "; UID[" + account.getLatestUidProcessed() + "]");
//				}
//			}
		}

		final boolean isProblem = problem;

		new AlertDialog.Builder(this)
		.setMessage(message)
		.setCancelable(false)
		.setPositiveButton(AutomatonAlert.OK_LABEL, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (!isProblem) {
					dialog.dismiss();
					finish();
					Pair<String, String> pair = new Pair<String, String>(
							AutomatonAlert.ACCOUNT_KEY,
							mAccountKey);
					SettingsFragment.callSelf(
							getApplicationContext(),
							ShowMode.ACCOUNT,
							pair);
				}
			}
		}).create().show();
	}

	private void showProblemAlertDialog() {
		if (mProblem == null) {
			mProblem = "The password or secure server provided isn't valid";
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(mProblem)
			.setCancelable(false)
			.setPositiveButton(AutomatonAlert.OK_LABEL, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					mAlertDialog.dismiss();
				}
			});
		mAlertDialog = builder.create();
		mAlertDialog.show();
	}

}
