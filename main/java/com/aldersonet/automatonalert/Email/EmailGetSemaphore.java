package com.aldersonet.automatonalert.Email;

import android.util.Log;

import com.aldersonet.automatonalert.Account.AccountDO;
import com.aldersonet.automatonalert.Account.Accounts;

import java.util.concurrent.Semaphore;

public class EmailGetSemaphore {

	String mAccountKey;
	Semaphore mSemaphore;
	public EmailGet mMailHandle;
	Thread mLifeCycleThread;

	public EmailGetSemaphore(String accountKey) {
		mAccountKey = accountKey;
		mSemaphore = new Semaphore(1, false);
		AccountEmailDO accountEmail = (AccountEmailDO) Accounts.get(accountKey);
        Log.e("EmailGetSemaphore", "Constructor(): accountEmail: " +
                accountEmail + ", accountKey: " + accountKey);

        int port = AccountDO.IMAP_SSL;
		if (accountEmail != null) {
            port = accountEmail.getPort();
        }

		if (port == AccountDO.IMAP_SSL) {
			mMailHandle = new ImapGet(accountEmail);
		}
		else {
			mMailHandle = null;
		}

	}

	@Override
	protected void finalize() {

		if (!(mSemaphore.tryAcquire())) {
			mSemaphore.release();
		}

		mMailHandle = null;

        try {
            super.finalize();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}
