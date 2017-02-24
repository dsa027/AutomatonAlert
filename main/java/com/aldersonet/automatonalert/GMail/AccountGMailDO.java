package com.aldersonet.automatonalert.GMail;

import android.database.Cursor;
import android.text.TextUtils;

import com.aldersonet.automatonalert.Account.AccountDO;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Provider.AutomatonAlertProvider;
import com.aldersonet.automatonalert.Util.Utils;

import java.math.BigInteger;

import static com.aldersonet.automatonalert.Email.AccountEmailDO.ACCOUNT_EMAIL;

public class AccountGMailDO extends AccountDO {

	public static final int ACCOUNT_GMAIL = 30;
	{
		mAccountType = ACCOUNT_GMAIL;
	}

	private String mEmail;
	private String mLatestUidProcessed;
	private boolean mShowImagesInEmail;
	private int mPoll;
	private long mLastChecked;

	private static final int MIN_POLL = 30 * 1000;
	private static final int DEFAULT_POLL = 15 * 60 * 1000;

	public AccountGMailDO() {
		super();

		mEmail = "";
		mLatestUidProcessed = "1";
		mShowImagesInEmail = false;
		mPoll = 60 * 60 * 1000; // an hour
		mLastChecked = -1;
	}

	public AccountGMailDO(final String name, final String user, final String password,
                          final String mailServer) {
		this();
		mName = name;
		mEmail = user;
	}

	@Override
	public boolean isComplete() {
		return !(TextUtils.isEmpty(mName))
				&& !(TextUtils.isEmpty(mEmail));
	}

	public String getLatestUidProcessed() {
		if (mLatestUidProcessed == null) {
			return "1";
		}
		return mLatestUidProcessed;
	}

	public BigInteger getLatestUidProcessedBigInteger() {

        return Utils.getBigInteger(mLatestUidProcessed, new BigInteger("1"));
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

	public String setLatestUidProcessed(final String latestUidProcessed) {
		if (!(mLatestUidProcessed.equals(latestUidProcessed))) {
			mIsDirty = true;
		}
		mLatestUidProcessed = latestUidProcessed;
		return latestUidProcessed;
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

	public AccountGMailDO populate(Cursor cursor) {

		super.populate(cursor);

		mIsDirty = false;

		mAccountType = ACCOUNT_EMAIL;

			mEmail = cursor.getString(cursor.getColumnIndex(
					AutomatonAlertProvider.ACCOUNT_EMAIL_ADDRESS));

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
        /*//davedel
		ContentValues cv = AutomatonAlertProvider.getGMailAccountContentValues(
				mName,
				mEmail,
				"NONE",
				getLatestUidProcessedBigInteger(),
				Boolean.toString(mShowImagesInEmail),
				mPoll,
				mAccountType,
				Boolean.toString(mSaveToList),
				Boolean.toString(mMarkAsSeen),
				mLastChecked);

		saveToDB(cv);
        //davedel*/

		mIsDirty = false;
	}

}
