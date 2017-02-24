package com.aldersonet.automatonalert.Alert;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.util.Log;

import com.aldersonet.automatonalert.Activity.RTUpdateActivity.FragmentTypeRT;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Provider.AutomatonAlertProvider;
import com.aldersonet.automatonalert.Service.AutomatonAlertService;
import com.aldersonet.automatonalert.SoundBomb.SoundBomb;
import com.aldersonet.automatonalert.SoundBomb.SoundBombQ;
import com.aldersonet.automatonalert.Util.Utils;

import java.util.Date;

public class NotificationItemDO {

	public static final String TAG = "NotificationItemDO";

	public static final String TAG_NOTIFICATION_ITEM_ID = "notificationItemId";
	public static final String TAG_SHOW_NOTIFICATION_LED = "mShowNotificationLed";
	public static final long NO_SCREEN_STOP_SOUND_AFTER_DEFAULT = 10 * 1000;
	public static final long MAX_TIME_MAKING_SOUND_TEST_MODE = 5 * 1000;

	private int mNotificationItemId;
	private boolean mActive;
	private String mTemplateName;
	private String mSoundPath;
	private String mSoundType;
	private int mVolumeLevel;
	private String mSilentMode;			// Phone:0/1; Other:0/always/never
	private String mVibrateMode;
	private boolean mNoAlertScreen;
	private long mPlayFor;
	private boolean mShowInNotificationBar;
	private String mLedMode;
	private boolean mIgnoreGlobalQuietPolicy;
	private Date mTimeStamp;

	public boolean isDirty = false;

	public NotificationItemDO() {
		super();

		mNotificationItemId = -1;
		mActive = true;
		mTemplateName = "";
		mSoundPath = "";
		mSoundType = "NONE";
		mVolumeLevel = 50;
		mSilentMode = "0";
		mVibrateMode = AutomatonAlert.NEVER;
		mIgnoreGlobalQuietPolicy = false;
		mNoAlertScreen = true;
		mPlayFor 		= -1;       // 1 loop
		mShowInNotificationBar = false;
		mLedMode = "0";
		mTimeStamp = new Date(System.currentTimeMillis());

		isDirty = true;

	}

	public NotificationItemDO populate(Cursor cursor) {

		isDirty = false;

		mNotificationItemId = cursor.getInt(
				cursor.getColumnIndex(AutomatonAlertProvider.NOTIFICATION_ITEM_ID));

		mTemplateName = cursor.getString(
				cursor.getColumnIndex(AutomatonAlertProvider.NOTIFICATION_ITEM_TEMPLATE_NAME));

		mSoundPath = cursor.getString(
				cursor.getColumnIndex(AutomatonAlertProvider.NOTIFICATION_ITEM_SOUND_PATH));

		mSoundType = cursor.getString(
				cursor.getColumnIndex(AutomatonAlertProvider.NOTIFICATION_ITEM_SOUND_TYPE));

		mVolumeLevel = cursor.getInt(
				cursor.getColumnIndex(AutomatonAlertProvider.NOTIFICATION_ITEM_VOLUME));

		mSilentMode = cursor.getString(
				cursor.getColumnIndex(AutomatonAlertProvider.NOTIFICATION_ITEM_SILENT_MODE));

		mVibrateMode = cursor.getString(cursor.getColumnIndex(
				AutomatonAlertProvider.NOTIFICATION_ITEM_VIBRATE_MODE));

		mNoAlertScreen = cursor.getString(cursor.getColumnIndex(
				AutomatonAlertProvider.NOTIFICATION_ITEM_NO_ALERT_SCREEN))
				.equalsIgnoreCase(AutomatonAlert.TRUE);

		mPlayFor = cursor.getLong(
				cursor.getColumnIndex(AutomatonAlertProvider.NOTIFICATION_ITEM_PLAY_FOR));

		String showInNotificationBar = cursor.getString(cursor.getColumnIndex(
				AutomatonAlertProvider.NOTIFICATION_ITEM_SHOW_NOTIFICATION));
		mShowInNotificationBar =
				showInNotificationBar.equalsIgnoreCase(AutomatonAlert.TRUE)
				|| showInNotificationBar.equals("1");

		mLedMode = cursor.getString(cursor.getColumnIndex(
				AutomatonAlertProvider.NOTIFICATION_ITEM_LED_MODE));

		mIgnoreGlobalQuietPolicy = cursor.getString(cursor.getColumnIndex(
				AutomatonAlertProvider.NOTIFICATION_ITEM_IGNORE_GLOBAL_QUIET_POLICY))
				.equalsIgnoreCase(AutomatonAlert.TRUE);

		long millis = cursor.getLong(
				cursor.getColumnIndex(AutomatonAlertProvider.NOTIFICATION_ITEM_TIMESTAMP));
		if (millis != -1) {
			mTimeStamp = new Date(millis);
		}
		else {
			mTimeStamp = new Date(System.currentTimeMillis());
		}

		return this;
	}

	public int getNotificationItemId() {
		return mNotificationItemId;
	}

	public int setNotificationItemId(int id) {
		if (mNotificationItemId != id) {
			isDirty = true;
		}
		// don't do a save() here...it needs to be taken care of
		// by the caller since this method should only be called
		// when constructing a new this
		return mNotificationItemId = id;
	}

	public boolean isActive() {
		return mActive;
	}

	public boolean setActive(boolean active) {
		if (mActive != active) {
			isDirty = true;
		}
		return mActive = active;
	}

	public String getSoundPath() {
		return mSoundPath;
	}

	public void setSoundPath(String soundPath) {
		if (!mSoundPath.equals(soundPath)) {
			isDirty = true;
		}

		this.mSoundPath = soundPath;
	}

	public int getVolumeLevel() {
		return mVolumeLevel;
	}

	public void setVolumeLevel(int volumeLevel) {
		if (mVolumeLevel != volumeLevel) {
			isDirty = true;
		}

		this.mVolumeLevel = volumeLevel;
	}

	public String getSilentMode() {
		return mSilentMode;
	}

	public void setSilentMode(String silentMode) {
		if (!mSilentMode.equals(silentMode)) {
			isDirty = true;
		}

		this.mSilentMode = silentMode;
	}

	public String getVibrateMode() {
		return mVibrateMode;
	}

	public void setVibrateMode(String vibrateMode) {
		if (!mVibrateMode.equals(vibrateMode)) {
			isDirty = true;
		}
		mVibrateMode = vibrateMode;
	}

	public boolean isNoAlertScreen() {
		return mNoAlertScreen;
	}

	public boolean setNoAlertScreen(boolean noAlertScreen) {
		if (mNoAlertScreen != noAlertScreen) {
			isDirty = true;
		}
		mNoAlertScreen = noAlertScreen;

		return noAlertScreen;
	}

	public long getPlayFor() {
		return mPlayFor;
	}

	public long setPlayFor(long playFor) {
		if (mPlayFor != playFor) {
			isDirty = true;
		}
		mPlayFor = playFor;

		return playFor;
	}

	public long setPlayFor(String playFor) {
		long lPlayFor = Utils.getLong(playFor, 2000);

		return setPlayFor(lPlayFor);
	}

	public boolean isShowInNotificationBar() {
		return mShowInNotificationBar;
	}

	public void setShowInNotificationBar(boolean showInNotificationBar) {
		if (mShowInNotificationBar != showInNotificationBar) {
			isDirty = true;
		}
		mShowInNotificationBar = showInNotificationBar;
	}

	public String getLedMode() {
		return mLedMode;
	}

	public void setLedMode(String ledMode) {
		if (!mLedMode.equals(ledMode)) {
			isDirty = true;
		}
		mLedMode = ledMode;
	}

	public boolean isIgnoreGlobalQuietPolicy() {
		return mIgnoreGlobalQuietPolicy;
	}

	public void setIgnoreGlobalQuietPolicy(boolean ignoreGlobalQuietPolicy) {
		if (mIgnoreGlobalQuietPolicy != ignoreGlobalQuietPolicy) {
			isDirty = true;
		}
		this.mIgnoreGlobalQuietPolicy = ignoreGlobalQuietPolicy;
	}

	public Date getTimeStamp() {
		return mTimeStamp;
	}

	public synchronized void delete() {
		try {

			Uri uri = ContentUris.withAppendedId(
					AutomatonAlertProvider.NOTIFICATION_ITEM_ID_URI, mNotificationItemId);
			AutomatonAlert.getProvider().delete(uri, null, null);

		} catch (RemoteException e) {
			Log.e(TAG + ".delete()", "delete exception: " + e.toString());
		}

//		NotificationItems.mCache.removeFromCache((Integer)getId());
	}

	public synchronized void save() {
		ContentValues cv = AutomatonAlertProvider.getNotificationItemContentValues(
				Boolean.toString(mActive),
				mTemplateName,
				mSoundPath,
				mSoundType,
				mVolumeLevel,
				mSilentMode,
				mVibrateMode,
				Boolean.toString(mNoAlertScreen),
				mPlayFor,
				Boolean.toString(mShowInNotificationBar),
				mLedMode,
				Boolean.toString(mIgnoreGlobalQuietPolicy)
		);

		AutomatonAlertProvider aap =
				(AutomatonAlertProvider)AutomatonAlert.getProvider()
				.getLocalContentProvider();

        int id = aap == null ? -1 :
                aap.insertOrUpdate(
				cv,
				mNotificationItemId,
				AutomatonAlertProvider.NOTIFICATION_ITEM_ID_URI,
				AutomatonAlertProvider.NOTIFICATION_ITEM_TABLE_URI);

		// if inserted, store new id
		if (id != -1
				&& id != mNotificationItemId) {
			mNotificationItemId = id;
		}

		isDirty = false;

//		NotificationItems.mCache.replaceFromCache(this);
	}

	public SoundBomb doNotification(
			Context context,
			boolean thisIsATest,
			boolean usePlayFor,
			FragmentTypeRT type,
			int alertItemId,
			String contactName) {

		final WakeLock wl = AutomatonAlertService.getThreadWakeLock(
				context,
				NotificationItemDO.class.getName()
						+ ".doNotification()");

		SoundBomb soundBomb =
				new SoundBomb(
						context,
						thisIsATest,
						usePlayFor,
						type,
						alertItemId,
						this,
						contactName);
		AutomatonAlert.getSoundBombs().add(soundBomb);

		SoundBombQ.doNotification(soundBomb, thisIsATest);

		if (wl != null
				&& wl.isHeld()) {
			wl.release();
		}

		if (soundBomb.mOk) {
			return soundBomb;
		}

		return null;

	}

//	@Override
//	public Object getId() {
//		return getNotificationItemId();
//	}
}
