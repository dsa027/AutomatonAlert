package com.aldersonet.automatonalert.Activity;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.text.Html;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.aldersonet.automatonalert.Activity.AlertListActivity.FragmentTypeAL;
import com.aldersonet.automatonalert.Adapter.AlertListArrayAdapter;
import com.aldersonet.automatonalert.Alarm.AlarmRepeat;
import com.aldersonet.automatonalert.Alert.AlertIntentExtrasChecker;
import com.aldersonet.automatonalert.Alert.AlertItemDO;
import com.aldersonet.automatonalert.Alert.AlertItemDO.Status;
import com.aldersonet.automatonalert.Alert.AlertItems;
import com.aldersonet.automatonalert.Alert.ContactAlert;
import com.aldersonet.automatonalert.Alert.NotificationItemDO;
import com.aldersonet.automatonalert.Alert.NotificationItems;
import com.aldersonet.automatonalert.SoundBomb.SoundBomb;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Fragment.AlertListFragment.Mode;
import com.aldersonet.automatonalert.PostAlarm.PostAlarmDO;
import com.aldersonet.automatonalert.Preferences.GeneralPrefsDO;
import com.aldersonet.automatonalert.Provider.AutomatonAlertProvider;
import com.aldersonet.automatonalert.R;
import com.aldersonet.automatonalert.Receiver.AlertReceiver;
import com.aldersonet.automatonalert.SMS.AccountSmsDO;
import com.aldersonet.automatonalert.SoundBomb.SoundBombQ;
import com.aldersonet.automatonalert.Util.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class AlarmVisualActivity extends FragmentActivity {
	public static final int TEST_ALARM = -99999;
	public static final int SNOOZE_OFFSET = 1;
	public static final int DISMISS_OFFSET = 2;
	public static final String TAG_FROM_NOTIFICATION = "fromNotification";

	public Thread mTimedModeThread;

	public FragmentTypeAL mFragmentType = FragmentTypeAL.ALARMS;

	public int mAlertItemId;
	public AlertItemDO mAlertItem;
	public int mNotificationItemId;
	public NotificationItemDO mNotificationItem;
	public String mAlertName;

	public long mDateRemindMillis;
	public Date mDateRemind;
	public long mRepeatEvery;
	public long mStopAfter;
	public int mCurrentIteration;
	public String mData;

	public View mDismissTextView;
	public View mSnoozeTextView;
	public Spinner mSnoozeDurationSpinner;

	String[] mSnoozeEntries;
	String[] mSnoozeValues;
	long mSnoozeMillis;

	Intent mThisActivityIntent;

	private boolean mThisIsATest;
	private boolean mThisIsASnooze;
	private boolean mThisIsARepeat;
	private boolean mUserRequestedDismiss;
	private boolean mUserRequestedSnooze;
	private boolean mAutoAcked;

	private ListView mListView;
	AlertListArrayAdapter mAlertListAdapter;
	boolean mShowNotification;
	long mNotificationTimer = 0;

	SoundBomb mSoundBomb;

	public ListView getListView() {
		return mListView;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//davedel -- disallow access
		finish();
		//davedel

		mThisActivityIntent = getIntent();
		if (mThisActivityIntent != null) {
			getIntentInfo();
			if (!intentOk()) {
				return;
			}
		}

		setTheme(android.R.style.Theme_Holo_Light_NoActionBar);

		///////////////////
		// the only entrance to this class should be through
		// AlertReceiver.  It filters based on sound only
		// (NotificationItemDO.isNoAlertScreen
		// or showing this screen
		///////////////////

		Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		//
		// set up screen
		setContentView(R.layout.alarm_visual_layout);


		// screen stuff
		setFieldsAndListeners();

		// set up adapter
		// drop sound bomb
		boolean problem = true;
        if (mThisActivityIntent != null) {
            // title
            setAlarmText();

            // get a quick AlertListAdapter, then use it
            // to get a single View back of the AlertItem.
            // use global so that it doesn't go away
	        ArrayList<AlertItemDO> showList = new ArrayList<AlertItemDO>();
            showList.add(mAlertItem);
            mAlertListAdapter = new AlertListArrayAdapter(
                    this,
                    null,
                    R.layout.alert_list_textview,
		            showList,
                    Mode.ALARM/*,
                    "0"*/);
            mListView.setAdapter(mAlertListAdapter);

            // if we at least get here, we're ok even
            // if sound doesn't work
            problem = false;
	        int alertItemId = (mAlertItem == null) ? -1 : mAlertItem.getAlertItemId();

            // sound bomb
            if (mNotificationItem != null) {
                // we want to be the only ones
                turnOffAllNotificationItemSoundsExceptUs();
                mNotificationTimer = System.currentTimeMillis();
                mSoundBomb = mNotificationItem.doNotification(
                        getApplicationContext(),
                        mThisIsATest,
                        false,/*usePlayFor*/
		                null,
                        alertItemId,
		                mAlertItem == null ?
		                          ""
		                        : mAlertItem.getKvRawDetails().get(Contacts.DISPLAY_NAME));
            }
		}

		// problem somewhere along the line, just exit
		if (problem) {
			finish();
		}

		// set again if REPEAT
		AlarmRepeat alarmRepeat =
				new AlarmRepeat(mThisActivityIntent, null/*action*/);
		if (alarmRepeat.isDoItAgain()) {
			alarmRepeat.reSendAlarm();
		}

		// if user asked for auto-acknowledge, start the timeout thread
		startScreenTimeoutThread();
	}

	private boolean isTimedModeThreadNeeded() {
		return mThisIsATest || GeneralPrefsDO.isAutoAck();
	}

	private void startScreenTimeoutThread() {
		if (isTimedModeThreadAlive()) {
			return;
		}

		mUserRequestedDismiss = false;
		mUserRequestedSnooze = false;
		mAutoAcked = false;

		// if user asked us to only have alert go off for a period of time, or
		// this is a test alert, snooze for a while and then either dismiss
		// or snooze
		if (isTimedModeThreadNeeded()) {
			(mTimedModeThread = new Thread (new Runnable() {
				@Override
				public void run() {
					// default
					long sleepyTime = NotificationItemDO.MAX_TIME_MAKING_SOUND_TEST_MODE;

					// user told us how long to snooze.
					if (!mThisIsATest) {
						sleepyTime = GeneralPrefsDO.getAutoAckAfter();
					}

					try {
						Thread.sleep(sleepyTime);
						mAutoAcked = true;
						finish();

					} catch (InterruptedException e) {
						// nothing
					}
				}
			})).start();
		}

	}

	private PostAlarmDO cancelPostAlarm() {
		if (mAlertItem != null) {
			return AlertItemDO.cancelPostAlarm(mAlertItem);
		}
		return null;
	}

	private void setFieldsAndListeners() {
		mListView = (ListView)findViewById(R.id.avm_listview);

		///////// handles
		mDismissTextView = findViewById(R.id.avm_dismiss_textview);
		mSnoozeTextView = findViewById(R.id.avm_snooze_textview);
		mSnoozeDurationSpinner =
				(Spinner)findViewById(R.id.avm_snooze_spinner);
		mSnoozeEntries = getResources().getStringArray(R.array.alarm_snooze_entries);
		mSnoozeValues = getResources().getStringArray(R.array.alarm_snooze_values);

		///////// default snooze
		int snoozeIdx = getSnoozeDurationArrayIdx(
				"" + GeneralPrefsDO.getDefaultSnooze(), mSnoozeValues);

		if (snoozeIdx >= 0) {
			mSnoozeDurationSpinner.setSelection(snoozeIdx);
		}
		else {
			mSnoozeDurationSpinner.setSelection(0);
		}

		///////////////////////
		// listeners
		///////////////////////
		///////// turn off alarm
		mDismissTextView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mShowNotification = false;
				mUserRequestedDismiss = true;
				finish();
			}
		});

		///////// snooze alarm
		mSnoozeTextView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mShowNotification = false;
				mUserRequestedSnooze = true;
				finish();
			}
		});

		///////// choose a snooze duration
		mSnoozeDurationSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View v,
					int position, long id) {

				mSnoozeMillis = translateSnoozeDuration(
						mSnoozeEntries[position], mSnoozeEntries, mSnoozeValues);
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
	}

	public int getSnoozeDurationArrayIdx(String snooze, String[] lookIn) {

		if (lookIn == null) {
			return -1;
		}

		for (int i = 0; i < lookIn.length; i++) {
			if (snooze.equals(lookIn[i])) {
				return i;
			}
		}

		return -1;
	}

	public long translateSnoozeDuration(String snooze, String[] in, String[] out) {

		if (mSnoozeEntries == null
				|| mSnoozeValues == null) {
			return -1;
		}

		for (int i = 0; i < in.length; i++) {
			if (snooze.equals(in[i])) {
				return Utils.getLong(out[i], -1);
			}
		}

		return -1;
	}

	private void setAlarmText() {

		TextView tvInfo = (TextView)findViewById(R.id.avm_info_textview);
		String tvText = "";

		if (mThisIsATest) {
			tvText =
					"<b>Test Alert.</b>"
					+ " <i>Functionality is disabled.</i>";
		}
		else if (mThisIsASnooze) {
			tvText =
					"<b>Snoozed Alarm</b>";
		}
		else if (mThisIsARepeat) {
			tvText =
					"<b>Repeat Alarm</b> ("
					+ AlertItemDO.getCurrentAlarmIteration(
							mAlertItem.getDateRemind(),
							mAlertItem.getRepeatEvery())
					+ "/"
					+ mAlertItem.getStopAfter()
					+ ")";
		}
		tvText += "<br><br><b><i>" + AutomatonAlert.mAppTitle + "!</b></i><br>";

		tvInfo.setText(Html.fromHtml(tvText));

	}

	private void getIntentInfo() {

		mAlertName = mThisActivityIntent.getStringExtra(
				AlertListArrayAdapter.TAG_ALERT_NAME);
		mAlertItemId = mThisActivityIntent.getIntExtra(
				AlertItemDO.TAG_ALERT_ITEM_ID,
				-1);
		mAlertItem = AlertItems.get(mAlertItemId);
		mNotificationItemId = mThisActivityIntent.getIntExtra(
				NotificationItemDO.TAG_NOTIFICATION_ITEM_ID, -1);
		mNotificationItem = NotificationItems.get(mNotificationItemId);

		// user left AlarmVisualActivity without either turning off or
		// snoozing.  Cancel the alarm that was set to bring this back
		// up in one minute since the user came back on their own.
		if (mThisActivityIntent.getBooleanExtra(TAG_FROM_NOTIFICATION, false)) {
			cancelPostAlarm();
		}

		/////
		// use current AlertItem if it exists
		/////
		if (mAlertItem != null) {
			mDateRemind = mAlertItem.getDateRemind();
			if (mDateRemind != null) {
				mDateRemindMillis = mDateRemind.getTime();
			}
			else {
				mDateRemindMillis = 0;
			}
			mRepeatEvery = mAlertItem.getRepeatEvery();
			mStopAfter = mAlertItem.getStopAfter();
		}
		else {
			/////
			// use what was in AlertItem when alarm was set
			/////
			mDateRemindMillis = mThisActivityIntent.getLongExtra(
					AutomatonAlertProvider.ALERT_ITEM_DATE_REMIND, -1);
			if (mDateRemindMillis > 0) {
				mDateRemind = new Date(mDateRemindMillis);
			}
			else {
				mDateRemind = null;
			}
			mRepeatEvery = mThisActivityIntent.getLongExtra(
					AutomatonAlertProvider.ALERT_ITEM_REPEAT_EVERY, -1);
			mStopAfter = mThisActivityIntent.getLongExtra(
					AutomatonAlertProvider.ALERT_ITEM_STOP_AFTER, -1);
		}

		// current iteration tells how many times
		// we've repeated the alarm
		mCurrentIteration =
				AlertItemDO.getCurrentAlarmIteration(mDateRemind, mRepeatEvery);

		mData = mThisActivityIntent.getDataString();
		mThisIsATest = AlertIntentExtrasChecker.isThisATest(mData);
		mThisIsASnooze = AlertIntentExtrasChecker.isThisASnooze(mData);
		mThisIsARepeat = AlertIntentExtrasChecker.isThisARepeat(mAlertItem);

		if (mThisIsATest) {
			if (mAlertItem == null) {
				mAlertItem = new AlertItemDO();
				mAlertItem.setFakeAlertItem(0);
			}
		}
	}

	private boolean intentOk() {
		if (mThisIsATest) {
			return true;
		}

		// if key items were missing, it's a bad alert/alarm
		if (/*mAlertItemId == -1
				||*/ mNotificationItemId == -1
				|| mAlertItem == null
				|| mNotificationItem == null) {
			return false;
		}

		AlertIntentExtrasChecker ok =
				new AlertIntentExtrasChecker(mThisActivityIntent, "");

		return ok.intentExtrasOk(false/*checkAction*/);
	}

	private Intent setNotificationIntent() {
		int alertItemId = -1;
		if (mAlertItem != null) {
			AlertItemDO.getFragmentTypeFromAlertItem(mAlertItem);
			alertItemId = mAlertItem.getAlertItemId();
		}
		Intent intent =
				AlertItemDO.setAlarmAlertIntent(
						alertItemId,
						mNotificationItemId,
						null, /* FragmentTypeRT */
						-1L, -1L, -1L);
		// need our unique stuff
		if (mAlertItem != null) {
			intent.putExtra(
					AutomatonAlert.REQUEST_CODE,
					AlertItemDO.getRequestCode(
							mAlertItem,
							getFragmentTypeForCancelNotification()));
		}
		intent.putExtra(TAG_FROM_NOTIFICATION, true);
		return intent;
	}

	private String getNotificationTitle() {
		String name = "";
		String title = "New alert";
		if (mAlertItem != null) {
			name = mAlertItem.getKvRawDetails().get(Contacts.DISPLAY_NAME);
		}
		if (!TextUtils.isEmpty(name)) {
			title = name;
		}
		return title;
	}

	private String getNotificationMessage() {
		String msg = "";
		String source = "";
		if (mAlertItem != null) {
			source = mAlertItem.getKvRawDetails().get(
					ContactAlert.TAG_MESSAGE_SOURCE_HEADER);
			msg =
					"<b>"
					+ source.toUpperCase(Locale.getDefault())
					+ (mThisIsASnooze?"(S)":mThisIsARepeat?"(R)":"")
					+ ":</b> "
					;
			if (source.equals(AccountSmsDO.SMS)
					|| source.equals(AccountSmsDO.MMS)) {
				String body = mAlertItem.getKvRawDetails().get(
						AutomatonAlert.SMS_BODY);
				msg += body;
			}
			else {
				msg +=
						mAlertItem.getKvRawDetails().get(AutomatonAlert.FROM)
						+ "<b>/ </b>"
						+ mAlertItem.getKvRawDetails().get(AutomatonAlert.SUBJECT)
						;

						//Utils.formatHeadersForView(mAlertItem.getKvRawDetails());
			}
		}
		return msg;
	}

	private PendingIntent getNotificationPendingIntent(
			Intent intent, int offset, int reqCode) {

		// headed to AlertReceiver
		intent.setClass(getApplicationContext(), AlertReceiver.class);
		// SNOOZE
		if (offset == SNOOZE_OFFSET) {
			intent.setAction(AutomatonAlert.ALARM_ALERT_SNOOZE_ALARM);
			intent.putExtra(FragmentTypeAL.SNOOZED.name(), true);
		}
		// TURN OFF
		else {
			intent.setAction(AutomatonAlert.ALARM_ALERT_TURN_OFF_ALARM);
			intent.putExtra(FragmentTypeAL.TRASH.name(), true);
		}
		// save so that it can be cancelled
		intent.putExtra(AutomatonAlert.REQUEST_CODE, reqCode);
		// save unique for turn off/snooze
		reqCode += offset;
		// make the pendingIntent

		return PendingIntent.getBroadcast(
				getApplicationContext(),
				reqCode,
				intent,
				PendingIntent.FLAG_UPDATE_CURRENT);
	}

	private void showNotification() {
		NotificationCompat.Builder builder =
				new NotificationCompat.Builder(this)
						.setContentTitle(getNotificationTitle())
						.setContentText(Html.fromHtml(getNotificationMessage()))
						.setSmallIcon(R.drawable.app_icon_white_24);

		int reqCode =
				AlertItemDO.getRequestCode(
						mAlertItem,
						getFragmentTypeForCancelNotification());

		// SHOW alarm
		Intent resultIntent = setNotificationIntent();
		PendingIntent resultPendingIntent =
				PendingIntent.getBroadcast(
						this, reqCode, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(resultPendingIntent);

		// TURN OFF ignored alarm
		builder.addAction(
						R.drawable.app_icon_white_24,
						"Dismiss",
						getNotificationPendingIntent(
								resultIntent, DISMISS_OFFSET, reqCode));
		// SNOOZE ignored alarm
		builder.addAction(
						R.drawable.app_icon_white_24,
						"Snooze " + Utils.translateMillis(
								GeneralPrefsDO.getDefaultSnooze(), true),
						getNotificationPendingIntent(
								resultIntent, SNOOZE_OFFSET, reqCode));
		// TURN OFF on notification dismiss
		builder.setDeleteIntent(
						getNotificationPendingIntent(
								resultIntent, DISMISS_OFFSET, reqCode));

		builder.setAutoCancel(true);

		// show notification
		Notification notification = builder.build();
		NotificationManager notificationManager =
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(reqCode, notification);
	}

	private FragmentTypeAL getFragmentTypeForCancelNotification() {
		FragmentTypeAL type = FragmentTypeAL.ALARMS;
		if (mThisIsASnooze) {
			type = FragmentTypeAL.SNOOZED;
		}
		else if (mThisIsARepeat) {
//			type = FragmentTypeAL.REPEATS;
			// Case: 2 Notifications from one Alarm: 1 ALARM, 1 REPEAT
			// REPEAT needs to cancel an ALARM so that the
			// ALARM doesn't cancel the REPEAT (for IGNORED
			// notifications).
			type = FragmentTypeAL.ALARMS;
		}
		return type;
	}

	private void cancelNotification() {
		// get rid of notification
		NotificationManager notificationManager =
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(
				AlertItemDO.getRequestCode(
						mAlertItem,
						getFragmentTypeForCancelNotification()));
	}

	@Override
	public void onResume() {
		super.onResume();
		// since we're asking for snooze/turnoff, get rid of PostAlarmDO snoozes, if any
		// including an ignore (user presses home or back-arrow or leaves somehow
		// without turn off or snooze)
		cancelNotification();
		cancelPostAlarm();
		mShowNotification = true;
		resumeAlarm();
	}

	@Override
	public void onStart() {
		super.onStart();
//		AutomatonAlert.populateAppData();
	}

	// this is an almost-duplicate of the eponymous function
	// in AutomatonAlertService except we don't remove
	// them from AutomatonAlert.getSoundBombs() list since
	// we're about to play it
	private void turnOffAllNotificationItemSoundsExceptUs() {
		for (SoundBomb soundBomb : AutomatonAlert.getSoundBombs()) {
			// don't turnOff this.mSoundBomb
			if (soundBomb != mSoundBomb) {
				soundBomb.stopAndRemove();
			}
		}
	}

	private void resumeAlarm() {
		if (mSoundBomb != null) {
			mNotificationTimer = System.currentTimeMillis();
			SoundBombQ.doNotification(mSoundBomb, mThisIsATest);
		}
		if (mTimedModeThread == null
				|| !mTimedModeThread.isAlive()) {
			startScreenTimeoutThread();
		}
	}

	@Override
	public void onRestart() {
		super.onRestart();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 2041) {
			if (mAlertItem != null) {
				// re-get from database since it's probably changed
				// while in SetAlertActivity
				mAlertItem = AlertItems.get(mAlertItem.getAlertItemId());
				if (mAlertItem.getStatus().equals(Status.DONT_SHOW)) {
					if (mAlertItem.getDateRemind() != null) {
						mAlertItem.updateStatus(Status.SAVED);
					}
				}
			}
			mAlertListAdapter.notifyDataSetChanged();
		}
	}

	private boolean isTimedModeThreadAlive() {
		return mTimedModeThread != null && mTimedModeThread.isAlive();
	}

	private boolean interruptTimedModeThread() {
		if (isTimedModeThreadAlive()) {
			mTimedModeThread.interrupt();
			return true;
		}
		return false;
	}

	public static final int FORCED_SOUND_TIME = 10000; // 10 seconds

	private void turnOffVibrateAndSoundDelayed() {
		if (mSoundBomb == null) {
			return;
		}
		// just cut it off for test
		if (mThisIsATest) {
			mSoundBomb.turnOffVibrateAndSound();
			return;
		}
		// doesn't need delay
		if (!mSoundBomb.isMakingNoise()) {
			mSoundBomb.turnOffVibrateAndSound();
			// no need to stay
			return;
		}
		// make sure sound runs at most n seconds
		// even though this activity is going away
		long now = System.currentTimeMillis();
		long sinceNotification = now - mNotificationTimer;
		long t = FORCED_SOUND_TIME - sinceNotification;
		if (t > 0) {
			mSoundBomb.delayThenStopAndRemove(t);
		}
		else {
			mSoundBomb.turnOffVibrateAndSound();
		}
	}

	private void interruptThreadsAndSilence() {
		interruptTimedModeThread();
		if (mSoundBomb != null) {
			mSoundBomb.interruptSoundThread();
			mSoundBomb.turnOffVibrateAndSound();
		}
	}

	private void autoAck() {
		// auto-ack SAVE
		if (GeneralPrefsDO.getAutoAckAs().equals(
				GeneralPrefsDO.AckAs.Dismissed)) {
			mAlertItem.updateStatus(Status.SAVED);
			cancelPostAlarm();
		}
		// auto-ack SNOOZE
		else if (GeneralPrefsDO.getAutoAckAs().equals(
				GeneralPrefsDO.AckAs.Snoozed)) {
			// this is a 'virtual' press via the 'auto ack'
			// setting
			AlertItemDO.setSnooze(
					mAlertItem, mSoundBomb, mSnoozeMillis);
		}
	}

	private void processOnStop() {
		// TEST_LABEL
		if (mThisIsATest) {
			interruptThreadsAndSilence();
		}
		// AUTO-ACK
		else if (mAutoAcked) {
			interruptThreadsAndSilence();
			autoAck();
		}
		// SNOOZE
		else if (mUserRequestedSnooze) {
			interruptThreadsAndSilence();
			AlertItemDO.setSnooze(
					mAlertItem, mSoundBomb, mSnoozeMillis);
			Utils.showSetAlarmToast(
					this,
					System.currentTimeMillis() + mSnoozeMillis,
					true/*snooze*/);
		}
		//DISMISS
		else if (mUserRequestedDismiss) {
			interruptThreadsAndSilence();
			cancelPostAlarm();
		}
		// IGNORED (system, home button, back arrow, recents list...)
		else {
			showNotification();
			cancelPostAlarm();
			interruptTimedModeThread();
			turnOffVibrateAndSoundDelayed();
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		processOnStop();
	}
}
