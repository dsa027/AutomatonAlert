package com.aldersonet.automatonalert.BitmapLoader;

import android.graphics.Bitmap;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.aldersonet.automatonalert.Adapter.ICLAdapterGetter;
import com.aldersonet.automatonalert.Cache.Cache;
import com.aldersonet.automatonalert.Fragment.ICLFragmentController;
import com.aldersonet.automatonalert.Util.Utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

/////////////////////
// BITMAPLOADER
/////////////////////
public class BitmapLoader {
	public static final String TAG = "BitmapLoader";
	public String ADAPTER = "";
	public static final long SHOW_BITMAPS_EVERY = 1000;
	public static final int MAX_TO_START = 25;

	BaseAdapter mAdapter;
	LinkedBlockingDeque<GetBitmap> mQ = new LinkedBlockingDeque<GetBitmap>();
	Thread mShowBitmapsThread;
	boolean mStartBlocked;
	ExecutorService mExecutorService = null;

	public BitmapLoader(BaseAdapter adapter) {
		mAdapter = adapter;

		ADAPTER = "";
//		if (BuildConfig.DEBUG) {
//			ADAPTER = ((ICLFragmentController) ((ICLAdapterGetter) adapter).getFragment())
//					.getFragmentType().name();
//		}
	}

	public void getPhotoWithDelay(String lookupKey) {
//		Log.d(TAG + ".getPhotoWithDelay("+ADAPTER+")", ".....");

		// start one back up
		if (mExecutorService == null
				|| mExecutorService.isShutdown()
				|| mExecutorService.isTerminated()
				) {
			mExecutorService  = Executors.newFixedThreadPool(5);
		}

		// only start Q when needed. it'll kill itself
		// when it's empty
		if (mShowBitmapsThread == null
				|| !mShowBitmapsThread.isAlive()) {
			showBitmapsPeriodically();
		}

		if (!isDuplicate(lookupKey)) {
			addToQ(new GetBitmap(lookupKey));  // on top
		}
	}

	public void addToQ(GetBitmap getBitmap) {
			mQ.addFirst(getBitmap);
	}

	private void showBitmapsPeriodically() {
		mShowBitmapsThread = new Thread(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(100);

						processQ();

						Thread.sleep(SHOW_BITMAPS_EVERY - 100);

					} catch (InterruptedException e) {
						Log.d(TAG + ".showBitmapsPeriodically(" + ADAPTER + ")", "STOPPING loop");
						break;
					}
				}
			}
		});
		mShowBitmapsThread.start();
	}

	synchronized private void processQ() {
		boolean foundOne = false;
		int count = 0;

		for (GetBitmap getBitmap : mQ) {
			// START it
			if (!getBitmap.isStartedLoading()) {
				if (mStartBlocked) {    // scrolling
					Log.d(TAG + ".startThread(" + ADAPTER + ")", "scrolling, start BLOCKED");
					continue;
				}
				if (++count <= MAX_TO_START) {
					getBitmap.getBitmap();
				}
			}
			// DONE - get rid of it
			else if (getBitmap.isDoneLoading()) {
				mQ.remove(getBitmap);
				foundOne = true;
			}
		}

		processEmptyQ();
		if (foundOne) {
			processChangedQ();
		}
	}

	void blockStart(boolean block) {
		mStartBlocked = block;
		if (!mStartBlocked) {
			processQ();
		}
	}

	private boolean isDuplicate(String lookupKey) {
		for (GetBitmap getBitmap : mQ) {
			if (getBitmap.mLookupKey.equals(lookupKey)) {
				return true;
			}
		}

		return false;
	}

	private void stopAll() {
		mQ.clear();
		mExecutorService.shutdownNow();
	}

	synchronized private void processEmptyQ() {
		if (mQ.isEmpty()) {
			// done loading all bitmaps, finish thread.
			// thread will start back up on next request
			// to load a bitmap
			Log.d(TAG + ".processEmptyQ(" + ADAPTER + ")", "done, Q empty");
			if (mShowBitmapsThread != null) {
				mShowBitmapsThread.interrupt();
			}
		}
	}

	private void processChangedQ() {
		Log.d(TAG + ".processChangedQ("+ADAPTER+")", "notifyDataSetChanged()");
		((ICLAdapterGetter)mAdapter).notifyDataSet();
	}

	///////////////////
	// GETBITMAP
	///////////////////
	class GetBitmap {
		String mLookupKey;
		Bitmap mBitmap;
		boolean mStartedLoading;
		boolean mDoneLoading;

		GetBitmap(String lookupKey) {
			super();
			mLookupKey = lookupKey;
			mBitmap = null;
			mStartedLoading = false;
			mDoneLoading = false;
		}

		public void getBitmap() {
			mExecutorService.submit(new Runnable() {
				public void run() {
					mStartedLoading = true;
					mBitmap = Utils.getPhotoBitmap(null, mLookupKey);
					saveBitmapInAdapter();
					mDoneLoading = true;
					Log.d(TAG + ".startThread(" + ADAPTER + ")", "thread COMPLETE");
				}
			});
		}

		private boolean isAdapterFragmentAdapterNull() {
			Fragment fragment = ((ICLAdapterGetter) mAdapter).getFragment();
			if (fragment != null) {
				BaseAdapter adapter = ((ICLFragmentController)fragment).getAdapter();
				if (adapter != null) {
					return false;
				}
			}

			return true;
		}

		private boolean isAdapterHelperNull() {
			return getHelper() == null;

		}

		private IBLGetter getHelper() {
			return (IBLGetter)((ICLAdapterGetter) mAdapter).getHelper();
		}

		private void saveBitmapInAdapter() {
			boolean adapterWentAway = true;

			if (mAdapter != null) {
				if (!isAdapterFragmentAdapterNull()) {
					if (!isAdapterHelperNull()) {
						adapterWentAway = false;
						getHelper().setBitmap(mLookupKey, mBitmap);
					}
				}

				// the adapter goes away
				if (adapterWentAway) {
					Log.d(TAG + ".saveBitmapInAdapter(" + ADAPTER + ")", "Adapter GONE, stopAll()!");
					stopAll();
				}
			}
		}

		private boolean isDoneLoading() {
			return mDoneLoading;
		}

		private boolean isStartedLoading() {
			return mStartedLoading;
		}
	}

	public static class SetListViewScrollBarListener {
		BitmapLoader mLoader;
		ListView mListView;
		IBLGetter mHelper;

		public SetListViewScrollBarListener(
				final BitmapLoader loader, ListView lv, IBLGetter helper) {
			mLoader = loader;
			mListView = lv;
			mHelper = helper;
		}
		public void setListViewScrollBarListener() {
			if (mListView == null
					|| mLoader == null) {
				return;
			}

			mListView.setOnScrollListener(new ListView.OnScrollListener() {
				@Override
				public void onScrollStateChanged(AbsListView view, int scrollState) {
					switch (scrollState) {
						// IDLE
						case AbsListView.OnScrollListener.SCROLL_STATE_IDLE:
							Log.d(TAG + ".onScrollStateChanged()", "unblocking");
							mLoader.blockStart(false);
							break;
						// FLINGING
						case AbsListView.OnScrollListener.SCROLL_STATE_FLING:
							Log.d(TAG + ".onScrollStateChanged()", "Fling BLOCKING");
							mLoader.blockStart(true);
							break;
						// FLINGING
						case AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
							Log.d(TAG + ".onScrollStateChanged()", "Scroll BLOCKING");
							mLoader.blockStart(true);
							break;
					}
				}

				@Override
				public void onScroll(
						AbsListView view, int firstVisibleItem,
						int visibleItemCount, int totalItemCount) {

//					Log.d(TAG + ".onScrollStateChanged()",
//							firstVisibleItem + "/" + visibleItemCount + "/" + totalItemCount
//									+ " - 1st/Count/Total");
				}
			});
		}
	}

	synchronized void dumpQ() {
		Log.d(TAG + ".dumpQ()", "VVVVVVVVVVVVVVVV"+ADAPTER+"VVVVVVVVVVVVVVVVVVVV");

		for (GetBitmap getBitmap : mQ) {
			Log.d(TAG + ".dumpQ()",
					Cache.ellipseString(getBitmap.mLookupKey)
							+ ", start: " + getBitmap.mStartedLoading
							+ ", done: " + getBitmap.mDoneLoading);
		}

		Log.d(TAG + ".dumpQ()", "^^^^^^^^^^^^^^^^"+ADAPTER+"^^^^^^^^^^^^^^^^^^^^");
	}

}
