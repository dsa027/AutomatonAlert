package com.aldersonet.automatonalert.SMS;

import android.database.Cursor;

import com.aldersonet.automatonalert.Account.AccountDO;

public class AccountSmsDO extends AccountDO {

	public static final String SMS = "sms";
	public static final String MMS = "mms";

	public static final String SMS_NAME = "SMS";
	public static final String SMS_KEY = "00SMS|" + SMS_NAME;
	public static final int ACCOUNT_SMS = 20;

	public AccountSmsDO() {
		super();
		mName = SMS_NAME;
		mAccountType = ACCOUNT_SMS;
	}

	public AccountSmsDO populate(Cursor cursor) {

		super.populate(cursor);
		mAccountType = ACCOUNT_SMS;

		return this;
	}

	@Override
	public String getKey() {
		return SMS_KEY;
	}

}
