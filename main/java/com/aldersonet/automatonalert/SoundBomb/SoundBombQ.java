package com.aldersonet.automatonalert.SoundBomb;

import android.util.Log;

import com.aldersonet.automatonalert.Alert.NotificationItemDO;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Preferences.RTPrefsDO;

import java.util.NoSuchElementException;
import java.util.concurrent.LinkedBlockingDeque;

public class SoundBombQ {

	public static final String TAG = "SoundBombQ";
	public static final int BREAK_RUN_LOOP = (Integer.MAX_VALUE - 8493) * -1;
	public static final long SLEEP_FOR = 100;
	public static final long MAX_WAIT_LOOPS = 60 * 1000 / SLEEP_FOR; // one minute's worth

	// THREAD
	static final QRunnable mQRunnable = new QRunnable();
	static Thread mQThread;

	// DEQUE - can add/get from front or back
	public static final LinkedBlockingDeque<SoundBombWrapper> mQ =
			new LinkedBlockingDeque<SoundBombWrapper>();

	// SOUND BOMB WRAPPER
	// keeps track of how many times a single instance
	// of SoundBomb has gone through the Q (done it's
	// doNotification())
	public static class SoundBombWrapper {
		public SoundBomb mSoundBomb;
		public int mTimesThroughQ;

		SoundBombWrapper(SoundBomb soundBomb) {
			mSoundBomb = soundBomb;
			mTimesThroughQ = 0;
		}
		SoundBombWrapper(SoundBomb soundBomb, int timesThroughQ) {
			mSoundBomb = soundBomb;
			mTimesThroughQ = timesThroughQ;
		}
	}

	public static synchronized void startQ() {
		if (mQThread == null
				|| !mQThread.isAlive()) {
			try {
				mQThread = new Thread(mQRunnable);
				mQThread.start();
			} catch (IllegalThreadStateException e) {
				e.printStackTrace();
			}

			Log.d(TAG + ".startQ()", "Starting Q");
		}
	}

	///////////////////////////////
	// DO NOTIFICATION
	//
	// Q ADD LAST
	//
	// marks the beginning of a multi-loop, the rest are
	// processed in mQRunnable, one's completion spawning the next.
	public static synchronized void doNotification(SoundBomb soundBomb, boolean isTest) {
		Log.d(TAG + ".doNotification()", "Adding: " + printObj(soundBomb));

		startQ();
		SoundBombWrapper wrapper = new SoundBombWrapper(soundBomb);
		try {
			mQ.addLast(wrapper);

			// REMINDERS on, add to RemindersQ
			if (!isTest
					&& RTPrefsDO.isRemindersOn()
					&& wrapper.mSoundBomb != null
					&& wrapper.mSoundBomb.mNotificationItem != null
					&& wrapper.mSoundBomb.mNotificationItem.isShowInNotificationBar()) {
				RemindersQ.add(wrapper.mSoundBomb);
			}

		} catch (IllegalStateException ignored) {}
	}

	// Q TAKE FIRST
	static SoundBombWrapper mLastSoundBombWrapper;
	static class QRunnable implements Runnable {
		int mEndlessLoopCounter = 0;
		@Override
		public void run() {
			Log.d(TAG + ".run()", "at top of run()");
			while (true) {
				try {
					// wait for the next soundBomb
					SoundBombWrapper soundBombWrapper = mQ.takeFirst();

					// poison pill found
					if (soundBombWrapper.mTimesThroughQ == BREAK_RUN_LOOP) {
						soundBombWrapper.mTimesThroughQ = -1;
						break;
					}

					// put it back...SoundBomb.stopAndRemove() will
					// take it off permanently. This statement needs to
					// complete before stopAndRemove()...removeFirst()
					// is called.
					mQ.addFirst(soundBombWrapper);

					// we put the SoundBomb back on the Q so that
					// SoundBomb.stopAndRemove() can take it off
					// permanently. If it's the same SoundBomb, just
					// wait a second and look again to see if stopAndRemove()
					// finally took it off and we can look at the next one
					// in the Q
					if (soundBombWrapper == mLastSoundBombWrapper) {
						// make sure we aren't in an endless loop waiting
						// for SoundBomb.stopAndRemove() to get rid of
						// this SoundBomb.
						if (mEndlessLoopCounter++ > MAX_WAIT_LOOPS) {
							// hard stop. stopAndRemove() will take
							// soundBomb off the Q
							Log.d(TAG + ".run(EndlessLoopException)", "calling stopAndRemove()");
							soundBombWrapper.mSoundBomb.stopAndRemove();

							// if stopAndRemove() (somehow) doesn't
							// remove soundBomb, we have to do it here.
							boolean atTheFront = false;
							if (mQ.peekFirst() == soundBombWrapper) {
								try {
									mQ.takeFirst();
									atTheFront = true;
									Log.d(TAG + ".run(EndlessLoopException)", "at_the_front=true");

								} catch (InterruptedException ignored) {
									// until something works, we'll be back...
								}
							}
							// and if still yet, let's search deeper
							if (!atTheFront) {
								Log.d(TAG + ".run(EndlessLoopException)", "at_the_front=false");
								mQ.remove(soundBombWrapper);
							}
							mLastSoundBombWrapper = null;
							mEndlessLoopCounter = 0;
							continue;
						}

						// wait...don't want to hog resources
						Thread.sleep(SLEEP_FOR);
						continue;
					}
					// save it off for comparison. we don't
					// want to do anything until the this
					// SoundBomb is different from the last
					// (i.e. stopAndRemove() has taken it
					// // from under us)
					mLastSoundBombWrapper = soundBombWrapper;
					mEndlessLoopCounter = 0;

					// do the notification
					soundBombWrapper.mSoundBomb.doNotificationFromQueue(
							soundBombWrapper.mTimesThroughQ);
					if (isPutItBackIntoQ(soundBombWrapper)) {
						// need a new wrapper so that this loop doesn't
						// think it's looking at the same one and discard it
						freshenWrapperAndAddToFrontOfQ(soundBombWrapper);
					}
				}
				catch (InterruptedException e) {
					e.printStackTrace();
					Log.d(TAG + ".run(InterruptedException)", "error");
				}
				catch (IllegalStateException e) {
					e.printStackTrace();
					Log.d(TAG + ".run(IllegalStateException)", "error");
				}
				catch (NullPointerException e) {
					e.printStackTrace();
					Log.d(TAG + ".run(NullPointerException)", "error");
				}
			}
			//noinspection InfiniteLoopStatement
		}
	}

	/* updates soundBombWrapper.mTimesThroughQ */
	private static boolean isPutItBackIntoQ(SoundBombWrapper soundBombWrapper) {
		NotificationItemDO notificationItem = soundBombWrapper.mSoundBomb.mNotificationItem;
		if (notificationItem != null) {
			int loop = (int)notificationItem.getPlayFor();
			if (loop < 0) {
				loop *= -1;
				if (++soundBombWrapper.mTimesThroughQ < loop) {
					return true;
				}
			}
		}

		return false;
	}

	private static void freshenWrapperAndAddToFrontOfQ(SoundBombWrapper staleWrapper) {
		// only try if it's still in SoundBombs
		if (!AutomatonAlert.getSoundBombs().contains(staleWrapper.mSoundBomb)) {
			// At this point, SoundBomb has been taken out of SoundBombs in stopAndRemove().
			// Add it back here so it can be dereferenced and silenced if need be.
			AutomatonAlert.getSoundBombs().add(staleWrapper.mSoundBomb);
		}
		// make sure the SoundBomb can go off again
		staleWrapper.mSoundBomb.reInit();
		// the wrapper won't be recognized by the Q and will again call doNotification
		addSelfToFrontOfQ(
				new SoundBombWrapper(
						staleWrapper.mSoundBomb,
						staleWrapper.mTimesThroughQ));
	}

	public static SoundBombWrapper removeSoundBombFromQ(SoundBomb removeMe) {
		dumpQ("TOP:removeSoundBombFromQ");
		// Pop off SoundBomb Q so it can proceed with next SoundBomb
		try {
			// make sure there's something in the Q
			SoundBombWrapper topOfQ = SoundBombQ.mQ.peekFirst();
			if (topOfQ != null) {
				if (topOfQ.mSoundBomb == removeMe) {
					SoundBombWrapper wrapper = mQ.removeFirst();
					dumpQ("BOTTOM: removeSoundBombFromQ");
					return wrapper;
				}
			}
		} catch (NoSuchElementException e) {
			e.printStackTrace();
		}

		Log.d(TAG + ".removeSoundBombFromQ()", "NOT THERE: " + printObj(removeMe));
		return null;
	}

	/* add back to front: 1) behind self if already there or 2) first */
	private static void addSelfToFrontOfQ(SoundBombWrapper self) {
		dumpQ("TOP: addSelfToFrontOfQ");
		synchronized (mQ) {
			try {
				boolean added = false;
				// see if there's something on the Q
				SoundBombWrapper fromTheQ = SoundBombQ.mQ.peekFirst();
				if (fromTheQ != null) {
					// this would only be false if
					// fromTheQ.mSoundBomb.stopAndRemove()'s Q.takeFirst()
					// happens between the time of assigning mLastSoundBombWrapper
					// and the top of the run() loop's Q.takeFirst
					if (fromTheQ == mLastSoundBombWrapper) {
						// LOOPING
						if (self.mSoundBomb == fromTheQ.mSoundBomb) {
							Log.d(TAG + ".addSelfToFrontOfQ(dual)", ".......");
							SoundBombWrapper keepOnTop = mQ.removeFirst();
							mQ.addFirst(self);
							mQ.addFirst(keepOnTop);
							added = true;
						}
					}
				}
				if (!added) {
					Log.d(TAG + ".addSelfToFrontOfQ(single)", ".......");
					mQ.addFirst(self);
				}
			}
			catch (NoSuchElementException ignored) {}
			catch (IllegalStateException ignored) {}
			catch (NullPointerException ignored) {}
			dumpQ("BOTTOM: addSelfToFrontOfQ");
		}
	}

	private static String printObj(Object obj) {
		if (obj == null) {
			return "null";
		}
		String sObj = obj.toString();
		return (sObj.substring(sObj.indexOf('@')));
	}

	private static void dumpQ(String from) {
//		if (BuildConfig.DEBUG) {
//			Log.d(TAG + "."+from+"()", "vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv");
//			SoundBombWrapper[] wrappers = mQ.toArray(new SoundBombWrapper[1]);
//			for (SoundBombWrapper wrapper : wrappers) {
//				if (wrapper == null) continue;//<<<<<<<<<<<<<
//				Log.d(
//						TAG + "."+from+"()",
//						"wrap[" + printObj(wrapper) + "], sb["
//								+ printObj(wrapper.mSoundBomb) + "], #["
//								+ wrapper.mTimesThroughQ + "]");
//			}
//			Log.d(TAG + "."+from+"()", "^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^");
//		}
	}
}
