package com.aldersonet.automatonalert.Util;

public class EmailAddress {
	String mUser,
	mLowLevelDomain,
	mHighLevelDomain,
	mEmailAddress;	// if this is null, then we have a valid email

	public EmailAddress() {
		mEmailAddress = "";	// not a valid email address
	}

	public EmailAddress(String emailAddress) {
		String[] parts = new String[3];

		mEmailAddress = emailAddress;
		if (mEmailAddress == null) {
			mEmailAddress = "";
			return;
		}

		int atSign = emailAddress.indexOf('@');
		int dot = emailAddress.lastIndexOf('.');

		if (atSign == -1
				|| dot == -1
				|| atSign > dot) {
			return;
		}

		try {
			parts[0] = emailAddress.substring(0, atSign);
			parts[1] = emailAddress.substring(atSign+1, dot);
			parts[2] = emailAddress.substring(dot+1);
		} catch (IndexOutOfBoundsException e) {
			return;
		}

		mUser = parts[0];
		mLowLevelDomain = parts[1];
		mHighLevelDomain = parts[2];

		mEmailAddress = null;
	}

	/* if null, the email address has been validated as accurate */
	public boolean isValid() {
		return mEmailAddress == null;
	}

	public String getEmailAddress() {
		if (mEmailAddress != null) {
			return mEmailAddress;
		}
		return mUser + "@" + mLowLevelDomain + "." + mHighLevelDomain;
	}

	public String getEmailUser() {
		if (mEmailAddress != null) {
			return null;
		}
		return mUser;
	}

	public String getDomain() {
		if (mEmailAddress != null) {
			return null;
		}
		return mLowLevelDomain + "." + mHighLevelDomain;
	}
}
