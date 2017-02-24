package com.aldersonet.automatonalert.Email;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import com.aldersonet.automatonalert.Account.AccountDO;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Provider.AutomatonAlertProvider;
import com.aldersonet.automatonalert.Util.Encryption;
import com.aldersonet.automatonalert.Util.Utils;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

public class AccountEmailDO extends AccountDO {

	public static final int ACCOUNT_EMAIL = 10;
	{
		mAccountType = ACCOUNT_EMAIL;
	}

	private String mEmail;
	private String mPassword;
	private String mServer;
	private String mLatestUidProcessed;
	private boolean mShowImagesInEmail;
	private int mPort;
	private int mPoll;
	private long mLastChecked;

	private static final int MIN_POLL = 30 * 1000;
	private static final int DEFAULT_POLL = 15 * 60 * 1000;

	public AccountEmailDO() {
		super();

		mEmail = "";
		mPassword = "";
		mServer = "";
		mLatestUidProcessed = "1";
		mShowImagesInEmail = false;
		mPort = 993;
		mPoll = 60 * 60 * 1000; // an hour
		mLastChecked = -1;
	}

	public AccountEmailDO(final String name, final String user, final String password,
			final String mailServer) {
		this();
		mName = name;
		mEmail = user;
		mPassword = setPassword(password);
		mServer = mailServer;
	}

	@Override
	public boolean isComplete() {
		return !(TextUtils.isEmpty(mName))
				&& !(TextUtils.isEmpty(mEmail))
				&& !(TextUtils.isEmpty(mPassword))
				&& !(TextUtils.isEmpty(mServer));
	}

	public String getHost() {
		return getServer();
	}

	public String getServer() {
		if (mServer == null) {
			return "";
		}
		return mServer;
	}

	public String getLatestUidProcessed() {
		if (mLatestUidProcessed == null) {
			return "1";
		}
		return mLatestUidProcessed;
	}

	public int getLatestUidProcessedInt() {
		return Utils.getInt(mLatestUidProcessed, 1);
	}

	public String getPassword() {
		if (mPassword == null) {
			return "";
		}
		return mPassword;
	}

	public int getPort() {
		return mPort;
	}

	public boolean getShowImagesInEmail() {
		return mShowImagesInEmail;
	}

	public String getEmail() {
		if (mEmail == null) {
			return "";
		}
		return mEmail;
	}

	public int getPoll() {
		return mPoll;
	}

	public long getLastChecked() {
		return mLastChecked;
	}

	public long setLastChecked() {
		return mLastChecked = System.currentTimeMillis();
	}

	public String getKey() {
		return mName + "|" + mEmail;
	}

	public String setServer(final String server) {
		if (!(mServer.equalsIgnoreCase(server))) {
			mIsDirty = true;
		}
		mServer = server;
		return server;
	}

	public String setLatestUidProcessed(final String latestUidProcessed) {
		if (!(mLatestUidProcessed.equals(latestUidProcessed))) {
			mIsDirty = true;
		}
		mLatestUidProcessed = latestUidProcessed;
		return latestUidProcessed;
	}

	public String setPassword(final String password) {
		if (!(mPassword.equals(password))) {
			mIsDirty = true;
		}
		mPassword = null;
		try {
			mPassword = Encryption.encrypt(password);
		} catch (GeneralSecurityException ignored) {
		} catch (UnsupportedEncodingException ignored) {
		}
		return mPassword;
	}

	public boolean setShowImagesInEmail(final boolean showImagesInEmail) {
		if (mShowImagesInEmail != showImagesInEmail) {
			mIsDirty = true;
		}
		mShowImagesInEmail = showImagesInEmail;
		return showImagesInEmail;
	}

	public String setEmail(final String email) {
		if (!(mEmail.equalsIgnoreCase(email))) {
			mIsDirty = true;
		}
		mEmail = email;
		return email;
	}

	public int setPoll (final int poll) {

		if ((poll < MIN_POLL && poll != -1)
				|| mPoll != poll) {
			mIsDirty = true;
		}

		if (poll < MIN_POLL
				&& poll != -1) {	// min = 2 minute
			mPoll = DEFAULT_POLL;	// default is 15 minutes
		}
		else {
			mPoll = poll;
		}
		return mPoll;
	}

	public AccountEmailDO populate(Cursor cursor) {

		super.populate(cursor);

		mIsDirty = false;

		mAccountType = ACCOUNT_EMAIL;

			mEmail = cursor.getString(cursor.getColumnIndex(
					AutomatonAlertProvider.ACCOUNT_EMAIL_ADDRESS));

			mPassword = cursor.getString(cursor.getColumnIndex(
					AutomatonAlertProvider.ACCOUNT_PASSWORD));

			mServer = cursor.getString(cursor.getColumnIndex(
					AutomatonAlertProvider.ACCOUNT_SERVER_NAME));

			mLatestUidProcessed = cursor.getString(cursor.getColumnIndex(
					AutomatonAlertProvider.ACCOUNT_LATEST_UID));

			mShowImagesInEmail = cursor.getString(cursor.getColumnIndex(
					AutomatonAlertProvider.ACCOUNT_SHOW_IMAGES))
					.equalsIgnoreCase(AutomatonAlert.TRUE);

			mPoll = cursor.getInt(cursor.getColumnIndex(
					AutomatonAlertProvider.ACCOUNT_POLL));

			mLastChecked = cursor.getLong(cursor.getColumnIndex(
					AutomatonAlertProvider.ACCOUNT_LAST_CHECKED));

			return this;

	}

	@Override
	public synchronized void save() {

		// populate the values for the insert/update
		ContentValues cv = AutomatonAlertProvider.getAccountContentValues(
				mName,
				mEmail,
				mPassword,
				mServer,
				"NONE",
				mPort,
				getLatestUidProcessedInt(),
				Boolean.toString(mShowImagesInEmail),
				mPoll,
				mAccountType,
				Boolean.toString(mSaveToList),
				Boolean.toString(mMarkAsSeen),
				mLastChecked);

		saveToDB(cv);

		mIsDirty = false;
	}

}
