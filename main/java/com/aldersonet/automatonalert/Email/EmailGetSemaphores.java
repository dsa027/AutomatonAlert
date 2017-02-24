package com.aldersonet.automatonalert.Email;

import com.aldersonet.automatonalert.Account.Accounts;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class EmailGetSemaphores extends ArrayList<EmailGetSemaphore> {

	private static final long serialVersionUID = 8866302831480463928L;

	@NotNull public EmailGetSemaphore get(String accountKey) {
		int N=size();
		for (int i=0;i<N;i++) {
			EmailGetSemaphore sem = get(i);
			if (sem.mAccountKey.equals(accountKey)) {
				// critial because the semaphore holds and AccountDO
				// object and the data changes in the db underneath.
				// NOTE: any non-immediate access to fields in
				// sem.mMailHandle.mAccount may be accessing stale data.
				refreshAccount(sem);
				return sem;
			}
		}
        return add(accountKey);
    }

	private void refreshAccount(EmailGetSemaphore sem) {
		// re-gets the account object since it may
		// have changed under us
		if (sem.mMailHandle != null
				&& sem.mMailHandle.mAccount != null
				&& sem.mMailHandle.mAccount.getAccountId() >= 0) {
			sem.mMailHandle.mAccount =
					Accounts.get(sem.mMailHandle.mAccount.getAccountId());
		}
	}

    public boolean tryAcquire(final EmailGetSemaphore sem) {
        boolean ret = sem.mSemaphore.tryAcquire();

        if (ret) {
            // jic...release after n seconds
            sem.mLifeCycleThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(59 * 1000);
                    }
                    catch (InterruptedException ignored) {
                    }
                    finally {
                        release(sem.mAccountKey);
                    }
                }
            });
            sem.mLifeCycleThread.start();
        }
        return ret;
    }

	public boolean tryAcquire(final String accountKey) {
        return tryAcquire(get(accountKey));
	}

	public void release(String accountKey) {
		release(get(accountKey));
	}

	public void release(EmailGetSemaphore sem) {
		if (sem == null) {
			return;
		}

		if (sem.mLifeCycleThread != null
				&& sem.mLifeCycleThread.isAlive()) {
			sem.mLifeCycleThread.interrupt();
		}

		sem.mSemaphore.release();
	}

	private EmailGetSemaphore add(String accountKey) {
		EmailGetSemaphore as = new EmailGetSemaphore(accountKey);
		add(as);
		return as;
	}

	protected void finalize() throws Throwable {
		for (EmailGetSemaphore sem : this) {
			if (sem != null) {
				sem.finalize();
			}
		}

        super.finalize();
    }
}
