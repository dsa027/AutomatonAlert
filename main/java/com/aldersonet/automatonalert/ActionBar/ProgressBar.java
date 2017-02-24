package com.aldersonet.automatonalert.ActionBar;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.widget.BaseAdapter;

import com.aldersonet.automatonalert.Fragment.IActivityMenuGetter;

import java.util.HashMap;
import java.util.List;

public class ProgressBar {
	// keep it from flickering and being otherwise annoying
	public static final long PROGRESSBAR_START_PAUSE = 50L;
	public static final long PROGRESSBAR_ALWAYS_STOP_AFTER = 30L * 1000L;

	private static long LONG_DIGITS = Math.round(Math.log10(Long.MAX_VALUE));

	public static ProgressBar mBar;
	final public HashMap<Long, Rec> mRecs = new HashMap<Long, Rec>();
	final public HashMap<Long, Rec> mOns = new HashMap<Long, Rec>(); // in the ON state

	public static ProgressBar getInstance() {
		// make sure we have one
		if (mBar == null) {
			mBar = new ProgressBar();
		}
		return mBar;
	}

	private ProgressBar() {
		super();
	}

	public long getHandle(
			AppCompatActivity activity, BaseAdapter baseAdapter,
			List<BaseAdapter> adapterList, ActionBar actionBar) {

		// add as a Rec
		Rec rec = new Rec(activity, baseAdapter, adapterList, actionBar);
		mRecs.put(rec.mKey, rec);
		return rec.mKey;
	}

	public long start(long key) {
		synchronized (mRecs) {
			Rec rec = mRecs.get(key);
			if (rec == null) {
				return -1;
			}
			startProgressBar(rec);
			mOns.put(rec.mKey, rec);
			return rec.mKey;
		}
	}

	public void stop(long key) {
		synchronized (mRecs) {
			Rec rec = mRecs.get(key);
			if (rec == null) {
				rec = mOns.get(key);
			}
			if (rec == null) {
				return;
			}
			// interrupt thread since we don't need it now that we have a stop
			if (rec.mSelfDestruct != null
					&& rec.mSelfDestruct.isAlive()) {
				rec.mSelfDestruct.interrupt();
			}
			rec = mOns.remove(key);
			// interrupt
			if (rec.mWaitThread != null
					&& rec.mWaitThread.isAlive()) {
				rec.mWaitThread.interrupt();
			}
			if (!isOneOn()) {
				// stop progressBar
				stopProgressBar(rec);
			}
			mRecs.remove(key);
		}
	}

	private boolean isOneOn() {
		return mOns.size() > 0;
	}

	private int getOnsSize() {
		return mOns.size();
	}

	private void visibleInvisibleMenuItems(Menu menu, boolean visible) {
		if (menu != null) {
			for (int i=0;i<menu.size();i++) {
				menu.getItem(i).setVisible(visible);
			}
		}
	}

	class WaitASecond {
		public boolean mInterrupted;
		public Rec mRec;
		long mSecond;
		Runnable mRunnable;
		Thread mThread;

		public WaitASecond(Rec rec, final long second, final Runnable runnable) {
			mInterrupted = false;
			mRec = rec;
			mSecond = second;
			mRunnable = runnable;
			mThread = null;
		}

		public Thread doWait() {
			// run right away, as requested
			if (mSecond == 0) {
				mRunnable.run();
				return null;
			}

			Runnable ourRunnable = new Runnable() {
				@Override
				public void run() {
					mInterrupted = false;
					try {
						Thread.sleep(mSecond);
						// if already stop()ed, don't run
						// (check to see if we're still in mRecs
						// cuz if not, our stop has run)
						if (mRecs.get(mRec.mKey) == null) {
							return;
						}
						mRunnable.run();
					}
					catch (InterruptedException e) {
						mInterrupted = true;
					}
				}
			};

			mThread = new Thread(ourRunnable);
			mThread.start();

			return mThread;
		}
	}

	class StartOrStopRunnable implements Runnable {
		public final Rec mRec;
		final long mKey;
		final boolean mStart;

		public StartOrStopRunnable(Rec rec, boolean start) {
			mRec = rec;
			mKey = rec.mKey;
			mStart = start;
		}

		@Override
		public void run() {
			mRec.mActivity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					// gone from under us, let's leave
					if (/*!mStart
							&&*/ mRecs.get(mKey) == null) {
						return;
					}
					if (mRec.mActivity instanceof IActivityMenuGetter) {
						Menu menu = (((IActivityMenuGetter)mRec.mActivity).getMenu());
						visibleInvisibleMenuItems(menu, !mStart/*visible*/);
					}
					mRec.mActivity.setSupportProgressBarIndeterminateVisibility(mStart);
					mRec.mActivity.setSupportProgressBarIndeterminate(mStart);
				}
			});
		}

	}

	private void createStartOrStopRunnable(final Rec rec, final boolean start) {
		if (rec == null) {
			return;
		}

		Runnable runnable = new StartOrStopRunnable(rec, start);

		if (start) {
			rec.mRunnableStart = runnable;
		}
		else {
			rec.mRunnableStop = runnable;
		}
	}

	private Thread selfDestructRecWithDelay(final Rec rec) {
		Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						Thread.sleep(PROGRESSBAR_ALWAYS_STOP_AFTER);

						// Rec from mRecs or mOns, either one
						Rec gotItRec = null;
						if ((gotItRec = mRecs.get(rec.mKey)) == null) {
							gotItRec = mOns.get(rec.mKey);
						}
						if (gotItRec != null) {
							// if we're still in mOns, run stop()
							// (which also pops us of mRecs and mOns)
							if (mOns.get(rec.mKey) != null) {
								// add it back in so that it can be stopped
								// if it isn't already there (somehow)
								if (mRecs.get(rec.mKey) == null) {
									mRecs.put(rec.mKey, rec);
								}
								stop(rec.mKey);
							}
							// 1) we're in either mRecs or mOns. 2) We're not in
							// mOns. 3) So, we're in mRecs and shouldn't be
							// (because we're not active).
							else {
								mRecs.remove(rec.mKey);
							}
						}
					}
					catch (InterruptedException e) {
						// all will be done for us, elsewhere
					}
				}
			});

		thread.start();

		return thread;
	}

	private void startProgressBar(Rec rec) {
		if (rec != null) {
			createStartOrStopRunnable(rec, true/*start*/);
			// runnaway self destruct
			rec.mSelfDestruct = selfDestructRecWithDelay(rec);
			// don't just flash the view quickly on and off,
			// make sure it's spinning for a bit even if it's done
			rec.mSpinProgressForABit = new WaitASecond(
					rec, PROGRESSBAR_START_PAUSE, rec.mRunnableStart);
			rec.mWaitThread = rec.mSpinProgressForABit.doWait();
		}
	}

	private void stopProgressBar(Rec rec) {
		if (rec != null) {
			createStartOrStopRunnable(rec, false/*start*/);
			rec.mRunnableStop.run();
		}
	}

	public class Rec {
		long mKey;
		AppCompatActivity mActivity;
		BaseAdapter mAdapter;
		List<BaseAdapter> mAdapterList;
		ActionBar mActionBar;
		Thread mSelfDestruct;
		Thread mWaitThread;
		WaitASecond mSpinProgressForABit;
		Runnable mRunnableStart;
		Runnable mRunnableStop;

		public Rec(AppCompatActivity activity, BaseAdapter baseAdapter,
				List<BaseAdapter> adapterList, ActionBar actionBar) {

			super();
			mActivity = activity;
			mAdapter = baseAdapter;
			mAdapterList = adapterList;
			mActionBar = actionBar;
			mSelfDestruct = null;
			mWaitThread = null;
			mRunnableStart = null;
			mRunnableStop = null;
			mKey = Math.abs(Math.round(Math.random() * Math.pow(10, (LONG_DIGITS - 2))));
		}
	}

	public static class StartObject {
		AppCompatActivity mActivity;
		BaseAdapter mAdapter;
		List mList;

		public StartObject(
				AppCompatActivity activity,
				BaseAdapter adapter,
				List list) {
			mActivity = activity;
			mAdapter = adapter;
			mList = list;
		}
	}

	public static long/*startKey*/ startProgressBar(
			ProgressBar progressBar,
			long startKey,
			StartObject startObject) {

		if (progressBar == null
				|| startObject == null) {
			return -1;
		}
		// with a valid key, make sure we're not running
		if (startKey != -1) {
			stopProgressBar(progressBar, startKey);
		}
		// our key
		startKey = progressBar.getHandle(
				startObject.mActivity,
				startObject.mAdapter,
				startObject.mList,
				startObject.mActivity.getSupportActionBar());

		// start it up
		progressBar.start(startKey);

		return startKey;
	}

	public static long stopProgressBar(ProgressBar progressBar, long startKey) {
		if (progressBar != null) {
			progressBar.stop(startKey);
		}

		return -1;
	}


}

