package com.aldersonet.automatonalert.SoundBomb;
/*
MediaPlayer finalized without being released
MediaPlayer finalized without being released
MediaPlayer finalized without being released
MediaPlayer finalized without being released
MediaPlayer finalized without being released
MediaPlayer finalized without being released
MediaPlayer finalized without being released
MediaPlayer finalized without being released

Hopefully this is the last unknown exit point.

 */
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Vibrator;
import android.provider.ContactsContract.Contacts;
import android.support.v4.app.Fragment;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.aldersonet.automatonalert.Activity.RTUpdateActivity;
import com.aldersonet.automatonalert.Activity.RTUpdateActivity.FragmentTypeRT;
import com.aldersonet.automatonalert.Alert.AlertItemDO;
import com.aldersonet.automatonalert.Alert.NotificationItemDO;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Fragment.RTUpdateFragment;
import com.aldersonet.automatonalert.Fragment.VolumeChooserFragment.VolumeTypes;
import com.aldersonet.automatonalert.Preferences.GeneralPrefsDO;
import com.aldersonet.automatonalert.Preferences.GeneralPrefsDO.OverrideVolLevel;
import com.aldersonet.automatonalert.Preferences.QuietTimePreference;
import com.aldersonet.automatonalert.Preferences.RTPrefsDO;
import com.aldersonet.automatonalert.R;
import com.aldersonet.automatonalert.Service.AutomatonAlertService;
import com.aldersonet.automatonalert.Util.Utils;

import java.io.IOException;
import java.util.HashMap;

public class SoundBomb {

	public static final String TAG = "SoundBomb";

	final static long[] mNormalPattern = { 0, 500, 500, 500, 2000 };
	final static long[] mSoftPattern = { 0, 50, 100, 50, 100, 50, 3000 };
	final static long[] mHardPattern = { 0, 2000, 500, 1500,  500, 1000, 2000 };

	final private static float VOLUME_LOW = 2/7F;
	final private static float VOLUME_MED = 4/7F;
	final private static float VOLUME_HI = 6/7F;
	final private static float VOLUME_IN_CALL = 0.25F;
	final private static float VOLUME_SPEAKERPHONE = VOLUME_MED;

	static long[] mPattern = mNormalPattern;

	protected boolean mIsThisATest;
	protected boolean mUsePlayFor; // for visual alert that doesn't use notificationItem.playFor
	protected boolean mTurnedOffVibrateAndSound;
	protected Context mContext;
	protected boolean mInSilentMode;
	protected boolean mInVibrateMode;
	protected boolean mPlayNothing;
	protected boolean mLoopingAndSilent;
	protected int mCurrentRingerMode;
	public NotificationItemDO mNotificationItem;
	public int mAlertItemId;
	public boolean mOk;

	AudioManager mAudioManager;
	Vibrator mVibrator;
	MediaPlayer mMediaPlayer;
	boolean mIsVibrating;
	boolean mIsSoundPlaying;
	boolean mShownInNotificationBar;

	int mCurrentStreamMaxVolume = 100;
	int mCurrentStreamVolume = 100;
	int mInitialAlarmStreamVolume = 100;
	double mVolumeCoefficient = VOLUME_LOW;

	public long mThreadStopAfter = 1000;

	Thread mDelayThread;

	String mContactName = "<unknown>";
	FragmentTypeRT mType;

	public SoundBomb(
			Context context,
			boolean thisIsATest,
			boolean usePlayFor,
			FragmentTypeRT fragmentType,
			int alertItemId,
			NotificationItemDO notificationItem,
			String contactName) {

		mVibrator = null;
		mMediaPlayer = null;
		mIsVibrating = false;
		mIsSoundPlaying = false;
		mShownInNotificationBar = false;

		mIsThisATest = thisIsATest;
		mUsePlayFor = usePlayFor;
		mContext = context;
		mNotificationItem = notificationItem;
		mAlertItemId = alertItemId;
		mOk = true;
		mType = (fragmentType == null) ?
				FragmentTypeRT.SETTINGS : fragmentType;

		if (mNotificationItem == null) {
			mNotificationItem = new NotificationItemDO();
		}

		getAudioManager();

		if (mContactName != null) {
			mContactName = contactName;
		}

		captureSoundStatus();

	}

	/* reinitialize for going through the SoundBomb Q again */
	public void reInit() {
		mVibrator = null;
		mMediaPlayer = null;
		mIsVibrating = false;
		mIsSoundPlaying = false;
		mTurnedOffVibrateAndSound = false;
		mOk = true;

		captureSoundStatus();
	}

	private AudioManager getAudioManager() {
		if (mAudioManager == null) {
			mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
		}

		return mAudioManager;
	}

	private void captureSoundStatus() {
		mInitialAlarmStreamVolume = // used to reset volume upon leaving
				getAudioManager().getStreamVolume(AudioManager.STREAM_ALARM);

		mCurrentRingerMode = getAudioManager().getRingerMode();

		mInVibrateMode = (mCurrentRingerMode == AudioManager.RINGER_MODE_VIBRATE);
		mInSilentMode = (mCurrentRingerMode == AudioManager.RINGER_MODE_SILENT);

		String staySilent = mNotificationItem.getSilentMode();
		String doVibrate = mNotificationItem.getVibrateMode();

		mPlayNothing =
				mInSilentMode   // Play except in Silent Mode || Always Silent
						&& (staySilent.equals("0") || staySilent.equals(AutomatonAlert.ALWAYS))
				||
				mInVibrateMode
						&& doVibrate.equals(AutomatonAlert.NEVER)
						&& (staySilent.equals("0") || staySilent.equals(AutomatonAlert.ALWAYS))
				||
				!mInSilentMode && !mInVibrateMode
						&& staySilent.equals(AutomatonAlert.ALWAYS)
						&& doVibrate.equals(AutomatonAlert.NEVER);
	}

	public boolean isDelayThreadAlive() {
		return mDelayThread != null && mDelayThread.isAlive();
	}

	public void interruptSoundThread() {
		if (isDelayThreadAlive()) {
			mDelayThread.interrupt();
		}
	}

	public void delayThenStopAndRemove(final long delay) {
		if (isMakingNoise()) {
			mDelayThread = new Thread(new Runnable() {
				boolean interrupted = false;
				@Override
				public void run() {
					try {
						Thread.sleep(delay);
					}
					catch (InterruptedException e) {
						interrupted = true;
					}
					finally {
						if (!interrupted) {
							Log.d(TAG + ".delayThenStopAndRemove()", "calling stopAndRemove()");
							stopAndRemove();
						}
					}
				}
			});
			mDelayThread.start();
		}
	}


	protected void doVibrate(final Context context) {
		String mode = mNotificationItem.getVibrateMode();
		mPattern = mNormalPattern;

		if (mode.contains("soft")) {
			mPattern = mSoftPattern;
		}
		else if (mode.contains("hard")) {
			mPattern = mHardPattern;
		}

		mVibrator =
				(Vibrator)context.getSystemService(Context.VIBRATOR_SERVICE);

		// if looping the sound, stop after 1
		if (mThreadStopAfter < 0) {
			mVibrator.vibrate(mPattern, -1);
		}
		else {
			mVibrator.vibrate(mPattern, 0);
		}
	}

	protected boolean okToPlaySound() {
		// nothing to play/vibrate
		if (mPlayNothing) {
			return false;
		}
		if (!mType.equals(FragmentTypeRT.PHONE)) {
			// if volume is down all the way
			if (mNotificationItem.getVolumeLevel() == 0) {
				return false;
			}
			// in a silent mode and not "never silent"
			if ((mInVibrateMode
					|| mInSilentMode)
					&& !mNotificationItem.getSilentMode().equalsIgnoreCase(AutomatonAlert.NEVER)) {
				return false;
			}
			// requested no ringtone
			if (mNotificationItem.getSoundPath().equalsIgnoreCase(AutomatonAlert.SILENT)) {
				return false;
			}
			// always silent
			if (mNotificationItem.getSilentMode().equals(AutomatonAlert.ALWAYS)) {
				return false;
			}
		}
		// now is in quiet time
		if (inQuietTime()) {
			return false;
		}
		if (mType.equals(FragmentTypeRT.PHONE)) {
			return true;
		}
		// block (really should never get here...
		return !mNotificationItem.getSoundPath().equalsIgnoreCase(AutomatonAlert.BLOCK_SMS_MMS_LABEL);

	}

	protected boolean okToVibrate() {
		if (mPlayNothing) {
			return false;
		}
		if (mType.equals(FragmentTypeRT.PHONE)) {
			return false;
		}
		// 'never vibrate'
		if (mNotificationItem.getVibrateMode().equals(AutomatonAlert.NEVER)) {
			return false;
		}
		// in silent mode
		if (mInSilentMode) {
			return false;
		}
		// 'in vibrate mode only' but not in vibrate mode
		if (mNotificationItem.getVibrateMode().contains("only")) {
			// and not in vibrate mode
			if (!mInVibrateMode) {
				return false;
			}
		}
		// quiet time + don't vibrate in quiet time
		return !(GeneralPrefsDO.isQuietTimeDoNotVibrate() && inQuietTime());
	}

	private boolean setRTUpdateFragmentTestButton(final boolean isSet) {
		HashMap<Integer, Fragment> fragments =
				RTUpdateActivity.getFragmentList();

		// go through each fragment that the Activity is managing
		for (Fragment fragment : fragments.values()) {
			if (fragment == null ||
					fragment.getActivity() == null) {
				continue;
			}
			// run a UI thread to reset the Test! button
			final Fragment runFragment = fragment;
			fragment.getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (runFragment instanceof RTUpdateFragment) {
						int res = isSet ?
								R.string.test_exclamation_point_label : R.string.stop_label;
						String sTest = mContext.getResources().getString(res);
						((RTUpdateFragment)runFragment).setTestButtonText(sTest);
					}
				}
			});
		}
		return fragments.size() != 0;
	}

	synchronized protected void doPlaySound() {
		String soundPath = mNotificationItem.getSoundPath();

		if (soundWillBeSilent(soundPath)) {
			return;
		}

		if (soundIsForBlockedSmsMms(soundPath)) {
			return;
		}

		soundPath = checkForDefaultSound(soundPath);

		duckAudioManager();

		// PLAY
		try {
			resetReleaseAndNullMediaPlayer();

            mMediaPlayer = new MediaPlayer();
			mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
			mMediaPlayer.setDataSource(mContext, Uri.parse(soundPath));
			setMediaPlayerListeners();

			// feels like backwards logic, but...
			// if getPlayFor() < 0, it's looping. For looping, we
			// need to not loop MediaPlayer since we'll control the
			// number of loops. For non-looping, we need to have the
			// sound loop forever and we'll cut it off at getPlayFor()
			mMediaPlayer.setLooping(mNotificationItem.getPlayFor() > 0);

			mMediaPlayer.setWakeMode(mContext, PowerManager.PARTIAL_WAKE_LOCK);

			// sets MediaPlayer volume
			setVolumeToPlay();
            // plays track
			mMediaPlayer.prepareAsync();

		}
		catch (IOException e) {
			Log.d(TAG + ".doPlaySound(IOException)", "calling stopAndRemove()");
			stopAndRemove();
		}
		catch (IllegalStateException e) {
			Log.d(TAG + ".doPlaySound(IllegalStateException)", "calling stopAndRemove()");
			stopAndRemove();
		}
		catch (NullPointerException e) {
			Log.d(TAG + ".doPlaySound(NullPointerException)", "calling stopAndRemove()");
			stopAndRemove();
		}
	}

	private boolean soundWillBeSilent(final String soundPath) {
		return TextUtils.isEmpty(soundPath) ||
				soundPath.equalsIgnoreCase(AutomatonAlert.SILENT);
	}

	private boolean soundIsForBlockedSmsMms(final String soundPath) {
		return soundPath.equalsIgnoreCase(AutomatonAlert.BLOCK_SMS_MMS_LABEL);

	}

	private String checkForDefaultSound(final String soundPath) {
		// make sure we have a valid ringtone
		String sPath = soundPath.toLowerCase();
		if (!(sPath.startsWith(AutomatonAlert.CONTENT_PREFIX))) {
			// "alarm", "notification", "ringtone"
			return RTPrefsDO.getDefaultRingtone(soundPath).toString();
		}

		return soundPath;
	}

	private void duckAudioManager() {
		getAudioManager().requestAudioFocus(
				audioFocusChangeListener,
				AudioManager.STREAM_ALARM,
				AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
	}

	private void resetReleaseAndNullMediaPlayer() {
		if (mMediaPlayer != null) {
			try {

				if (mMediaPlayer.isPlaying()) {
					mMediaPlayer.stop();
				}

				if (mMediaPlayer != null) {
					mMediaPlayer.reset(); // really, it crashed here with NullPointer
					if (mMediaPlayer != null) {
						mMediaPlayer.release();  // here too. putting exception in catch
					}
					mMediaPlayer = null;
				}

			} catch (IllegalStateException ignore) {}
			  catch (NullPointerException ignore) {}
		}
	}

	private void setMediaPlayerListeners() {
		mMediaPlayer.setOnErrorListener(new MPOnErrorListener());
		mMediaPlayer.setOnPreparedListener(new MPOnPreparedListener());
		mMediaPlayer.setOnCompletionListener(new MPOnCompletionListener());
		// https://code.google.com/p/android/issues/detail?id=1314
		// "ANDROID_LOOP=true" metadata tag that causes ogg files
		// to loop on their own. this will catch them.
		mMediaPlayer.setOnSeekCompleteListener(new MPOnSeekCompleteListener());
	}

	class MPOnSeekCompleteListener implements MediaPlayer.OnSeekCompleteListener {
		@Override
		public void onSeekComplete(MediaPlayer mp) {
			// if if loops, stop it now (ogg files can loop
			// even with loop=false)
			if (mp != null) {
				if (mNotificationItem != null &&
						mNotificationItem.getPlayFor() < 0) {
					mp.stop();
					Log.d(TAG + ".onSeekComplete()", "calling stopAndRemove()");
					stopAndRemove();
				}
			}
		}
	}

	class MPOnPreparedListener implements MediaPlayer.OnPreparedListener {
		@Override
		public void onPrepared(MediaPlayer mp) {
			mp.setVolume(1f, 1f);
			try {
				mp.start();
				if (mUsePlayFor
						&& mNotificationItem.getPlayFor() > 0) {
					stopBombAfterTime();
				}
			} catch (IllegalStateException e) {
				e.printStackTrace();
			}
		}
	}

	class MPOnErrorListener implements MediaPlayer.OnErrorListener {
		@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {
			Log.d(TAG + ".onError()", "calling stopAndRemove()");
			stopAndRemove();
			return true;
		}
	}

	class MPOnCompletionListener implements MediaPlayer.OnCompletionListener {
		@Override
		public void onCompletion(MediaPlayer mp) {
			Log.d(TAG + ".onCompletion()", "calling stopAndRemove()");
			stopAndRemove();
		}
	}

	private long getTotalVibrateTime() {
		String vibrateMode = mNotificationItem.getVibrateMode();

		if (mInSilentMode
				|| vibrateMode.equals(AutomatonAlert.NEVER)
				|| (!mInVibrateMode
					&& vibrateMode.startsWith(AutomatonAlert.ONLY))) {
			return 0;
		}

		if (vibrateMode.contains(AutomatonAlert.SOFT)) {
			return addLongArray(mSoftPattern);
		}
		if (vibrateMode.contains(AutomatonAlert.NORMAL)) {
			return addLongArray(mNormalPattern);
		}
		if (vibrateMode.contains(AutomatonAlert.HARD)) {
			return addLongArray(mHardPattern);
		}

		return 0;
	}

	private long addLongArray(long[] array) {
		int total = 0;
		for (long i : array) {
			total += i;
		}

		return total;
	}

	protected void stopBombAfterTime() {
		// if no sound and no vibrate
		if (mPlayNothing) {
			Log.d(TAG + ".stopBombAfterTime(mPlayNothing)", "calling stopAndRemove()");
			stopAndRemove();
			return;
		}

		// don't go on forever if looping and silent.
		// if not vibrating, stop after 2 seconds.
		// if vibrating, stop after vibrating stops
		if (mLoopingAndSilent) {
			mThreadStopAfter = Math.max(2000, getTotalVibrateTime() + 200);
		}

		if (mThreadStopAfter > 0) {
			// stop after a certain amount of time no matter what
			new Thread (new Runnable() {
				@Override
				public void run() {
					try {
						Thread.sleep(mThreadStopAfter);
					} catch (InterruptedException ignored) {
					} finally {
						Log.d(TAG + ".stopBombAfterTime(finally)", "calling stopAndRemove()");
						stopAndRemove();
					}
				}
			}).start();
		}
	}

	public static Pair<Integer, Integer> translateVolumeField(String volumeField) {
		AudioManager am =
				(AudioManager)AutomatonAlert.THIS.getSystemService(Context.AUDIO_SERVICE);
		int vol = 0;
		int maxVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM);

		// CUSTOM default volume
		if (TextUtils.isDigitsOnly(volumeField)) {
			int defVolume = Utils.getInt(volumeField, -1);
			vol = (defVolume == -1) ? 2 : Math.round(
					((float) defVolume / RTPrefsDO.INTERNAL_VOLUME_MAX) * maxVol);
		}
		// default ringtone, alarm, notification
		else {
			// legacy can be value "alarms" with an "s", so
			// we need to default to alarm
			int type = AudioManager.STREAM_ALARM;
			if (volumeField.equalsIgnoreCase(VolumeTypes.notification.name())) {
				type = AudioManager.STREAM_NOTIFICATION;
			}
			else if (volumeField.equalsIgnoreCase(VolumeTypes.ringtone.name())) {
				type = AudioManager.STREAM_RING;
			}

			vol = am.getStreamVolume(type);
			maxVol = am.getStreamMaxVolume(type);
		}

		return new Pair<Integer, Integer>(vol, maxVol);
	}

	private boolean isNotificationSoundSilent() {
		String sound = mNotificationItem.getSoundPath();

		boolean soundIsSilent = sound.equalsIgnoreCase(AutomatonAlert.SILENT);
		boolean silentMp3 = sound.equals(Utils.SILENT_MP3);
		boolean volZero = mNotificationItem.getVolumeLevel() == 0;
		boolean alwaysSilent = mNotificationItem.getSilentMode().equals(AutomatonAlert.ALWAYS);

		return 	soundIsSilent
				|| silentMp3
				|| volZero
				|| alwaysSilent;
	}

	private void setVolumeToPlay() {
		setVolumeParams();
		moderateVolumeParams();
		setVolumeUsingVolumeParams();
	}

	private void moderateVolumeParams() {
//		if (mAudioManager.isWiredHeadsetOn()) {
//			mVolumeCoefficient = VOLUME_IN_CALL;
//		}
		if (getAudioManager().isSpeakerphoneOn()) {
			if (mVolumeCoefficient > VOLUME_SPEAKERPHONE) {
				mVolumeCoefficient = VOLUME_SPEAKERPHONE;
			}
		}
	}

	private void setVolumeParamsForStreamAlarm() {
		// default to STREAM_ALARM
		mCurrentStreamMaxVolume = getAudioManager().getStreamMaxVolume(
				AudioManager.STREAM_ALARM);
		mCurrentStreamVolume = getAudioManager().getStreamVolume(
				AudioManager.STREAM_ALARM);
	}

	private void setVolumeParamsDbDefaultVolume() {
		Pair<Integer, Integer> pair =
				translateVolumeField(RTPrefsDO.getDefaultVolume());

		mCurrentStreamVolume = pair.first;
		mCurrentStreamMaxVolume = pair.second;
		mVolumeCoefficient =
				(double)mCurrentStreamVolume / (double)mCurrentStreamMaxVolume;
	}

	private boolean isUserInACall() {
		return ((TelephonyManager)AutomatonAlert.THIS.getSystemService(
				Context.TELEPHONY_SERVICE)).getCallState() != TelephonyManager.CALL_STATE_IDLE;
	}

	private void setVolumeParams() {
		// jic
		if (mMediaPlayer == null) {
			return;
		}

		setVolumeParamsForStreamAlarm();

		// IN A CALL
		if (isUserInACall()) {
			mVolumeCoefficient = VOLUME_IN_CALL;
		}
		// HEADSET
		else if (getAudioManager().isBluetoothA2dpOn()) {
			mVolumeCoefficient = VOLUME_IN_CALL;
		}
		// PHONE RINGTONE (Text mode, since for PHONE,
		// that's the only way to get here
		else if (mType.equals(FragmentTypeRT.PHONE)) {
			setVolumeParamsDbDefaultVolume();
		}
		else {
			setVolumeParamsDefaultOverrideVol();
		}
	}

	private void setVolumeParamsDefaultOverrideVol() {
		double coefficientWas = -1;

		// Override.DEFAULT means play at volume
		// specified in NotificationItem.
		// NotificationItem volume can be:
		// Silent=0, a level=1-7, or Default=8.
		// Default means get alarm/notification/ringtone default volume
		// and play at that volume
		//
		// Override is set at DEFAULT, so get volume from NotificationItem
		if (GeneralPrefsDO.getOverrideVol().equals(OverrideVolLevel.DEFAULT.name())) {
			int nVol = mNotificationItem.getVolumeLevel();

			// DEFAULT VOLUME,
			// (need to get current/max volume for alarm/notification/ringtone)
			if (nVol == RTPrefsDO.UI_VOLUME_DEFAULT_INDEX) {
				setVolumeParamsDbDefaultVolume();
			}
			// VOLUME 0
			else if (nVol == 0) {
				setVolumeParamsVolume0();
			}
			// VOLUME n
			else {
				mVolumeCoefficient = (double)nVol / (double)RTPrefsDO.UI_VOLUME_MAX;
			}
		}
		else {
			coefficientWas = setVolumeParamsOverrideVol();
		}

		setVolumeParamsNeverSilent(coefficientWas);
	}

	private void setVolumeParamsVolume0() {
		mCurrentStreamMaxVolume = 0;
		mCurrentStreamVolume = 0;
		mVolumeCoefficient = 0;
	}

	private void setVolumeParamsNeverSilent(double coefficientWas) {
		// NEVER SILENT with SILENT OVERRIDE: never silent wins
		String staySilent = mNotificationItem.getSilentMode();
		String overrideVol = GeneralPrefsDO.getOverrideVol();
		String silent = OverrideVolLevel.SILENT.name();

		if (mVolumeCoefficient == 0                             // Silent
				&& staySilent.equals(AutomatonAlert.NEVER)      // Never Silent
				&& (mInSilentMode
						|| mInVibrateMode
						|| isNotificationSoundSilent()
						|| overrideVol.equals(silent))) {
			// if Widget is SILENT, set coefficient back to what it was
			// (play at volume requested w/o being in Widget SILENT mode
			if (coefficientWas != -1/*never assigned*/) {
				mVolumeCoefficient = coefficientWas;
			}
			else {
				mVolumeCoefficient = VOLUME_MED;
			}
		}
	}

	private double setVolumeParamsOverrideVol() {
		double coefficientWas = -1;

		// Override is set at HI
		if (OverrideVolLevel.HI.name().equals(GeneralPrefsDO.getOverrideVol())) {
			mVolumeCoefficient = VOLUME_HI;
		}
		// Override is set at MED
		else if (OverrideVolLevel.MED.name().equals(GeneralPrefsDO.getOverrideVol())) {
			mVolumeCoefficient = VOLUME_MED;
		}
		// Override is set at LOW
		else if (OverrideVolLevel.LOW.name().equals(GeneralPrefsDO.getOverrideVol())) {
			mVolumeCoefficient = VOLUME_LOW;
		}
		// Override is set at SILENT
		else if (OverrideVolLevel.SILENT.name().equals(GeneralPrefsDO.getOverrideVol())) {
			coefficientWas = mVolumeCoefficient;
			mVolumeCoefficient = 0;
		}

		return coefficientWas;
	}

	private void setVolumeUsingVolumeParams() {
		getAudioManager().setStreamVolume(
				AudioManager.STREAM_ALARM,
				(int) Math.round(mVolumeCoefficient * mCurrentStreamMaxVolume),
				0);
	}

	protected void doNotifyInStatusBar(Context context) {
		final Intent notifyIntent = new Intent(context, AutomatonAlertService.class);
		notifyIntent.putExtra(AutomatonAlert.ACTION, AutomatonAlert.ACTION_NOTIFY);
		notifyIntent.putExtra(RTUpdateActivity.TAG_FRAGMENT_TYPE, mType.name());
		notifyIntent.putExtra(Contacts.DISPLAY_NAME, mContactName);
		notifyIntent.putExtra(
				NotificationItemDO.TAG_NOTIFICATION_ITEM_ID,
				mNotificationItem.getNotificationItemId());
		notifyIntent.putExtra(
				AlertItemDO.TAG_ALERT_ITEM_ID,
				mAlertItemId);
		notifyIntent.putExtra(
				NotificationItemDO.TAG_SHOW_NOTIFICATION_LED,
				mNotificationItem.getLedMode());
		context.startService(notifyIntent);
	}

	OnAudioFocusChangeListener audioFocusChangeListener = new OnAudioFocusChangeListener() {
		public void onAudioFocusChange(int focusChange) {
			// do nothing...
		}
	};

	public void doNotificationFromQueue(int loop) {
		final WakeLock wl = AutomatonAlertService.getThreadWakeLock(
				mContext,
				SoundBomb.class.getName()
						+ ".doNotificationFromQueue()");

		// show "Stop" on test button
		setRTUpdateFragmentTestButton(false/*isSet*/);

		// PREP
		if (doNotificationCheckForBlock(wl)) return;
		mTurnedOffVibrateAndSound = false;
		mLoopingAndSilent = false;
		captureSoundStatus();
		mThreadStopAfter = getStopAfter();
		doNotificationSetLoopingAndSilent();
		doNotificationPhoneCheck();

		// WORK
		doNotificationVibrate();
		doNotificationSound();
		if (loop == 0) {
			doNotificationBar();
		}

		// if there is no sound, we need to kill it manually
		if (!mIsSoundPlaying) {
			stopBombAfterTime();
			setRTUpdateFragmentTestButton(true/*isSet*/);
		}

		mOk = true;

		if (wl != null
				&& wl.isHeld()) {
			wl.release();
		}
	}

	private boolean doNotificationCheckForBlock(final WakeLock wl) {
		// block (really should never get here...
		if (mNotificationItem.getSoundPath().equalsIgnoreCase(AutomatonAlert.BLOCK_SMS_MMS_LABEL)) {
			mThreadStopAfter = 0;
			stopBombAfterTime();
			setRTUpdateFragmentTestButton(true/*isSet*/);
			if (wl != null
					&& wl.isHeld()) {
				wl.release();
			}
			return true;
		}

		return false;
	}

	private void doNotificationSetLoopingAndSilent() {
		// LOOPING & NO SOUND //
		// STOP MANUALLY      //
		if (mThreadStopAfter < 0                        // looping
				&& (isNotificationSoundSilent()         // vol=0|sound=silent|always silent
				|| mInSilentMode                    // don't play sounds or vibrate
				|| mInVibrateMode)) {               // only vibrate
			mLoopingAndSilent = true;                   // stop thread after 2 seconds
		}
	}

	private void doNotificationVibrate() {
		if (!mIsVibrating &&
				okToVibrate()) {
			mIsVibrating = true;
			new Thread(new Runnable() {
				@Override
				public void run() {
					Log.d(TAG + ".doNotificationVibrate().run()", "about to vibrate");
					doVibrate(mContext);
				}
			}).start();
		}
	}

	private void doNotificationPhoneCheck() {
		// if this is a Test! of PHONE, only make the sound
		if (mType.equals(FragmentTypeRT.PHONE)) {
			// skip these
			mIsVibrating = true;
			mShownInNotificationBar = true;
		}
	}

	private void doNotificationSound() {
		if (!mIsSoundPlaying &&
				okToPlaySound()) {
			mIsSoundPlaying = true;
			new Thread(new Runnable() {
				@Override
				public void run() {
					Log.d(TAG + ".doNotificationSound().run()", "about to play sound");
					doPlaySound();
				}
			}).start();
		}
	}

	private void doNotificationBar() {
		if (!mShownInNotificationBar
				&& mNotificationItem.isShowInNotificationBar()) {
			mShownInNotificationBar = true;
			doNotifyInStatusBar(mContext);
		}
	}

	private long getStopAfter() {
		long stopAfter =
				Math.min(
						mNotificationItem.getPlayFor(),
						NotificationItemDO.NO_SCREEN_STOP_SOUND_AFTER_DEFAULT);

		//////////
		// TEST //
		//////////
		if (mIsThisATest) {
			if (mType.equals(FragmentTypeRT.PHONE)) {
				// there is no 'Play For' field for PHONE, only 'Silent Mode'
				stopAfter = NotificationItemDO.MAX_TIME_MAKING_SOUND_TEST_MODE;
			}
			else {
				stopAfter = Math.min(
						stopAfter,
						NotificationItemDO.MAX_TIME_MAKING_SOUND_TEST_MODE);
			}
		}

		return stopAfter;
	}

	private boolean inQuietTime() {
		return QuietTimePreference.inQuietTime(mIsThisATest, mNotificationItem);
	}

	private void resetStreamVolume() {
		synchronized(getAudioManager()) {
			int is = getAudioManager().getStreamVolume(AudioManager.STREAM_ALARM);
			int shouldBe = mInitialAlarmStreamVolume;
			if (is == shouldBe) return;

			int direction = (is > shouldBe) ?
					AudioManager.ADJUST_LOWER: AudioManager.ADJUST_RAISE;
			int diff = Math.abs(is - shouldBe);

			Log.d(TAG + ".resetStreamVolume()", "is[" + is + "], shouldBe[" + shouldBe + "]");

			// incrementally adust volume
			for (int i=0; i < diff; i++) {
				getAudioManager().adjustStreamVolume(
						AudioManager.STREAM_ALARM, direction, 0);
			}
		}
	}

	public boolean isMakingNoise() {
		return mIsVibrating || mIsSoundPlaying;
	}

	private void cancelVibrator() {
		if (mVibrator != null) {
			mVibrator.cancel();
		}
		mVibrator = null;
		mIsVibrating = false;
	}

	private void stopMediaPlayer() {
		if (mMediaPlayer != null) {
			try {
				if (mMediaPlayer.isPlaying()) {
					mMediaPlayer.stop();
				}
				resetReleaseAndNullMediaPlayer();

			} catch (IllegalStateException ignored) {}
			finally {
				mMediaPlayer = null;
			}
		}
		mIsSoundPlaying = false;
	}

	public void turnOffVibrateAndSound() {
		// did this already since doNotificationFromQueue()? If so, don't
		// do it again since something else may be playing
		if (mTurnedOffVibrateAndSound) {
			resetReleaseAndNullMediaPlayer();
			return;
		}
		mTurnedOffVibrateAndSound = true;

		// RESET VOLUME
		resetStreamVolume();

		// UNDUCK
		getAudioManager().abandonAudioFocus(audioFocusChangeListener);

		// !VIBRATOR
		cancelVibrator();

		// !MEDIA PLAYER
		stopMediaPlayer();
	}


	public void stopAndRemove() {
		Log.d(TAG + ".stopAndRemove()", "starting");

		// STOP
		setRTUpdateFragmentTestButton(true/*isSet*/);
		turnOffVibrateAndSound();
		resetReleaseAndNullMediaPlayer();

		// REMOVE
		if (AutomatonAlert.getSoundBombs() != null) {
			AutomatonAlert.getSoundBombs().remove(this);
		}
		SoundBombQ.removeSoundBombFromQ(this);

		Log.d(TAG + ".stopAndRemove()", "leaving");
	}

	/* pretty sure this is NEVER called */
	@Override
	protected void finalize() throws Throwable {
		Log.d(TAG + ".finalize()", "calling stopAndRemove()");
		stopAndRemove();
		super.finalize();
	}

}
