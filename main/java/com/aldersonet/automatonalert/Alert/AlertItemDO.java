package com.aldersonet.automatonalert.Alert;

import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract.Contacts;
import android.util.Log;

import com.aldersonet.automatonalert.Account.AccountDO;
import com.aldersonet.automatonalert.Account.Accounts;
import com.aldersonet.automatonalert.Activity.AlertListActivity.FragmentTypeAL;
import com.aldersonet.automatonalert.Activity.RTUpdateActivity;
import com.aldersonet.automatonalert.Activity.RTUpdateActivity.FragmentTypeRT;
import com.aldersonet.automatonalert.Alarm.AlarmPendingIntent;
import com.aldersonet.automatonalert.Alarm.AlarmPendingIntent.ApiSubType;
import com.aldersonet.automatonalert.Alarm.AlarmPendingIntent.ApiType;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Filter.FilterItemDO;
import com.aldersonet.automatonalert.PostAlarm.PostAlarmDO;
import com.aldersonet.automatonalert.Preferences.GeneralPrefsDO;
import com.aldersonet.automatonalert.Preferences.RTPrefsDO;
import com.aldersonet.automatonalert.Provider.AutomatonAlertProvider;
import com.aldersonet.automatonalert.Receiver.AlertReceiver;
import com.aldersonet.automatonalert.SMS.AccountSmsDO;
import com.aldersonet.automatonalert.SoundBomb.SoundBomb;
import com.aldersonet.automatonalert.Util.Utils;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;

public class AlertItemDO {
	public static final String TAG = "AlertItemDO";
	public static final String TAG_ALERT_ITEM_ID = "alertItemId";

	private static final long MONTHLY_MILLIS = 28L * 24L * 60L * 60L * 1000L;
	private static final long YEARLY_MILLIS = 365L * 24L * 60L * 60L * 1000L;

	public static enum AlertItemType {
		SEARCH, CONTACT, UNKNOWN
	}

	// used to create default sound for SMS/MMS
	// when there's no match
	public static FilterItemDO mDefaultFilterItem;
	public static NotificationItemDO mDefaultNotificationItem;
	public static SoundBomb mSoundBomb;

	private int mAlertItemId;

	private HashMap<String, String> mKvDetails =
			new HashMap<String, String>();
	public HashMap<String, String> mKvRawDetails =
			new HashMap<String, String>();

	private AlertItemType mType;
	private String mUid;
	private Date mTimeStamp;
	private Status mStatus;
	private Date mDateExpires;
	private boolean mFavorite;
	private Date mDateRemind;
	private long mRepeatEvery;
	private long mStopAfter;
	private int mNotificationItemId;
	private int mAccountId;

	public static final String FAKE = "fake";
	public static final int FAKE_ALERT_ID = -99;

	public boolean isDirty = false;

	public enum Status {
		LIVE, NEW, SAVED, TRASH, DONT_SHOW
	}

	private void initAllFields() {
		mType = AlertItemType.UNKNOWN;
		mUid = "<>x<>";
		mNotificationItemId = -1;
		mAccountId = -1;
		mTimeStamp = new Date(System.currentTimeMillis());
		mStatus = Status.NEW;
		mDateExpires = null;
		mFavorite = false;
		mDateRemind = null;
		mRepeatEvery = -1;
		mStopAfter = 0;

		isDirty = true;
	}

	public AlertItemDO() {
		super();
		initAllFields();
	}

	public AlertItemDO(AlertItemDO alertItem) {
		this();

		mType = alertItem.mType;
		mUid = alertItem.mUid;
		mTimeStamp = alertItem.mTimeStamp;
		mStatus = alertItem.mStatus;
		mDateExpires = alertItem.mDateExpires;
		mFavorite = alertItem.mFavorite;
		mDateRemind = alertItem.mDateRemind;
		mRepeatEvery = alertItem.mRepeatEvery;
		mStopAfter = alertItem.mStopAfter;
		mNotificationItemId = alertItem.mNotificationItemId;
		mAccountId = alertItem.mAccountId;
		mKvDetails = new HashMap<String, String>(alertItem.mKvDetails);
		mKvRawDetails = new HashMap<String, String>(alertItem.mKvRawDetails);
	}

	public AlertItemDO(
			AlertItemType type, String uid, HashMap<String, String> itemDetails,
			HashMap<String, String> itemRawDetails, int accountId) {
		this();

		if (type != null) {
			mType = type;
		}
		else {
			mType = AlertItemType.UNKNOWN;
		}

		if (uid != null) {
			mUid = uid;
		}
		else {
			mUid = "<>x<>";
		}

		if (itemDetails == null) {
			mKvDetails = new HashMap<String, String>();
		}
		else {
			mKvDetails = itemDetails;
		}

		// if null, strip itemDetails of html markup and add
		if (itemRawDetails == null) {
			mKvRawDetails = new HashMap<String, String>();
			for (Entry<String, String> header : mKvDetails.entrySet()) {
				mKvRawDetails.put(
						header.getKey(),
						Utils.stripMarkupHeaderField(header.getValue(), true));
			}
		}
		else {
			mKvRawDetails = itemRawDetails;
		}

		mAccountId = accountId;

		// if accountId == -1 or -99, ... attempt to find the
		// real accountId from what we have in mKvRawDetails
		if (accountId < 0) {
			if (mKvRawDetails != null) {
				String accountKey = mKvRawDetails.get(AutomatonAlert.ACCOUNT_KEY);
				if (accountKey != null) {
					AccountDO account =
							Accounts.get(accountKey);
					if (account != null) {
						mAccountId = account.getAccountId();
					}
				}
			}
		}

		isDirty = true;
	}

	public AlertItemDO(
			AlertItemType type, String uid, Status status,
			 HashMap<String, String> itemDetails,
			 HashMap<String, String> itemRawDetails, int accountId) {

		this(type, uid, itemDetails, itemRawDetails, accountId);
		mStatus = status;
	}

	public AlertItemDO setFakeAlertItem(long dateRemindMillis) {
		mAlertItemId = FAKE_ALERT_ID;
		if (dateRemindMillis > 0) {
			mDateRemind = new Date(dateRemindMillis);
		}
		mKvRawDetails.put(AutomatonAlert.TO, "your@email.com");
		mKvRawDetails.put(AutomatonAlert.FROM, AutomatonAlert.TECH_EMAIL_ADDR);
		mKvRawDetails.put(AutomatonAlert.DATE, "{email date}");
		mKvRawDetails.put(AutomatonAlert.SUBJECT, "This is a test alert");
		mKvRawDetails.put(AutomatonAlert.ACCOUNT_KEY, "account@name.com");
		mKvRawDetails.put(AutomatonAlert.UID, "12345");
		mKvRawDetails.put(ContactAlert.TAG_MESSAGE_SOURCE_HEADER, AccountDO.EMAIL);
		mKvRawDetails.put(AlertItemDO.FAKE, AlertItemDO.FAKE);
		mKvRawDetails.put(Contacts.DISPLAY_NAME, "Test Alert");

		return this;
	}

	private HashMap<String, String> detailsFromString(String sIn) {

		HashMap<String, String> details = new HashMap<String, String>();

		String[] sKvFields = sIn.split(Utils.FLDSEP);

		for (String sKvField : sKvFields) {
			String[] sKvF = sKvField.split(Utils.FLDEQ, 2);
			if (sKvF.length < 2) {
				details.put(sKvF[0], "");
			}
			else {
				details.put(sKvF[0], sKvF[1]);
			}
		}

		return details;
	}

	private String detailsToString(HashMap<String, String> details) {

		StringBuilder sB = new StringBuilder();

		// write the HashMap<String, String> the way we want rather than toString()
		boolean initial = true;
		for (Entry<String, String> field : details.entrySet()) {
			if (initial) {
				initial = false;
			}
			else {
				sB.append(Utils.FLDSEP);
			}
			sB.append(field.getKey()).append(Utils.FLDEQ).append(field.getValue());
		}

		return sB.toString();
	}

	public AlertItemType getAlertItemType(String inType) {
		try {
			return AlertItemType.valueOf(inType);

		} catch (NullPointerException e) {
			return AlertItemType.UNKNOWN;
		} catch (IllegalArgumentException e2) {
			return AlertItemType.UNKNOWN;
		}
	}

	public AlertItemDO populate(Cursor cursor) {

		isDirty = false;

		mAlertItemId = cursor.getInt(
				cursor.getColumnIndex(AutomatonAlertProvider.ALERT_ITEM_ID));

		mType = getAlertItemType(
						cursor.getString(cursor.getColumnIndex(
								AutomatonAlertProvider.ALERT_ITEM_TYPE)));

		mUid = cursor.getString(
				cursor.getColumnIndex(AutomatonAlertProvider.ALERT_ITEM_UID));

		mFavorite = cursor.getString(cursor.getColumnIndex(
				AutomatonAlertProvider.ALERT_ITEM_FAVORITE))
				.equalsIgnoreCase(AutomatonAlert.TRUE);

		long millis = cursor.getLong(
				cursor.getColumnIndex(AutomatonAlertProvider.ALERT_ITEM_DATE_REMIND));
		if (millis != -1) {
			mDateRemind = new Date(millis);
		}
		else {
			mDateRemind = null;
		}

		mRepeatEvery = cursor.getLong(
				cursor.getColumnIndex(AutomatonAlertProvider.ALERT_ITEM_REPEAT_EVERY));

		mStopAfter = cursor.getLong(
				cursor.getColumnIndex(AutomatonAlertProvider.ALERT_ITEM_STOP_AFTER));

		String fields = "";

		fields = cursor.getString(
				cursor.getColumnIndex(AutomatonAlertProvider.ALERT_ITEM_RAW_FIELDS));

		mKvRawDetails = detailsFromString(fields);

		fields = cursor.getString(
				cursor.getColumnIndex(AutomatonAlertProvider.ALERT_ITEM_MARKUP_FIELDS));

		mKvDetails = detailsFromString(fields);

		mNotificationItemId = cursor.getInt(
				cursor.getColumnIndex(AutomatonAlertProvider.ALERT_ITEM_NOTIFICATION_ITEM_ID));

		mAccountId = cursor.getInt(
				cursor.getColumnIndex(AutomatonAlertProvider.ALERT_ITEM_ACCOUNT_ID));

		try {
			mStatus = Status.valueOf(cursor.getString(
					cursor.getColumnIndex(AutomatonAlertProvider.ALERT_ITEM_STATUS)));

		} catch (IllegalArgumentException e) {
			mStatus = Status.NEW;
		}

		millis = cursor.getLong(
				cursor.getColumnIndex(AutomatonAlertProvider.ALERT_ITEM_DATE_EXPIRES));
		if (millis != -1) {
			mDateExpires = new Date(millis);
		}
		else {
			mDateExpires = null;
		}

		millis = cursor.getLong(
				cursor.getColumnIndex(AutomatonAlertProvider.ALERT_ITEM_TIMESTAMP));
		if (millis != -1) {
			mTimeStamp = new Date(millis);
		}
		else {
			mTimeStamp = new Date(System.currentTimeMillis());
		}

		return this;
	}

	public HashMap<String, String> getKvDetails() {
		return mKvDetails;
	}

	public HashMap<String, String> getKvRawDetails() {
		return mKvRawDetails;
	}

	public Date getTimeStamp() {
		return mTimeStamp;
	}

	public Date getDateRemind() {
		return mDateRemind;
	}

	public long getStopAfter() {
		return mStopAfter;
	}

	public long setStopAfter(long stopAfter) {
		if (mStopAfter != stopAfter) {
			isDirty = true;
		}
		mStopAfter = stopAfter;

		return stopAfter;
	}

	public long getRepeatEvery() {
		return mRepeatEvery;
	}

	public long setRepeatEvery(long repeatEvery) {
		if (mRepeatEvery != repeatEvery) {
			isDirty = true;
		}
		mRepeatEvery = repeatEvery;

		return repeatEvery;
	}

	public int getAlertItemId() {
		return mAlertItemId;
	}

	public String getUid() {
		return mUid;
	}

	public int getAccountId() {
		return mAccountId;
	}

	public int setAccountId(int id) {
		if (mAccountId != id) {
			isDirty = true;
		}
		return mAccountId = id;
	}

	public int getNotificationItemId() {
		return mNotificationItemId;
	}

	public int setAlertItemId(int id) {
		if (mAlertItemId != id) {
			isDirty = true;
		}
		// don't do a save() here...it needs to be taken care of
		// by the caller since this method should only be called
		// when constructing a new this
		return mAlertItemId = id;
	}

	public int setNotificationItemId(int id) {
		if (mNotificationItemId != id) {
			isDirty = true;
		}
		return mNotificationItemId = id;
	}

	public Status getStatus() {
		return mStatus;
	}

	// here in case developer wants to create a
	// setStatus that only sets the status instead
	// of the processing done in updateStatus
	public Status setStatus(Status status) {
		updateStatus(status);
		return status;
	}

	public synchronized AlertItemDO updateStatus(Status toStatus) {
		// don't delete a favorite
		if (toStatus.equals(Status.TRASH)
				&& mFavorite) {
			return this;
		}

		if (mStatus != toStatus) {
			isDirty = true;
		}

		// if we're hiding (deleting), put an expiration date on
		// it so it doesn't stick around endlessly.
		// also cancel alarm if one is pending
		if (toStatus.equals(Status.TRASH)) {

			// cancel alarm on deleted AlertItemDO
			if (mDateRemind != null) {
				// alarms - remove ALL (this is an alarm)
				findCancelRemovePendingIntentsPostAlarms(ApiSubType.ALARM);
				mDateRemind = null;
				isDirty = true;
			}

			// status == mStatus == TRASH: delete AlertItemDO
			// 		or
			// mExpireDeleted = false: delete AlertItemDO
			//
			// else
			//
			// mKeepDeleted == true:
			//		 			mExpireDeletedAfter == -1: never delete
			//									   not -1: delete later
			//
			if (mStatus != Status.TRASH
					&& GeneralPrefsDO.isKeepDeleted()) {
				if (GeneralPrefsDO.getExpireDeletedAfter() != -1) {
					mDateExpires =
							new Date(System.currentTimeMillis()
									+ GeneralPrefsDO.getExpireDeletedAfter());
					isDirty = true;
				}
				// asked to never expire
				else {
					mDateExpires = null;
				}
			}
			else { /* need to delete */
				delete();
				isDirty = false;	// don't go and save it after deleting...
			}
		}

		// if changing to new or seen, clear the expired field
		else if ((toStatus.equals(Status.NEW)
				|| toStatus.equals(Status.SAVED))
				&& mStatus.equals(Status.TRASH)
				&& mDateExpires != null) {
			mDateExpires = null;
			isDirty = true;
		}

		mStatus = toStatus;

		if (isDirty) {
			save();
		}

		return this;
	}

	public Date getDateExpires() {
		return mDateExpires;
	}

	public synchronized AlertItemDO updateFavorite(boolean favorite) {

		if (mFavorite != favorite) {
			isDirty = true;
		}
		if (/* now a */favorite) {
			if (mDateExpires != null) {
				mDateExpires = null;
				isDirty = true;
			}
		}

		mFavorite = favorite;

		if (isDirty) {
			save();
		}

		return this;
	}

	public boolean isFavorite() {
		return mFavorite;
	}

	public Date updateDateRemind(Date dateRemind, boolean saveIt) {
		if ((mDateRemind == null
				&& dateRemind != null)
				|| mDateRemind != null
				&& dateRemind == null) {
			isDirty = true;
		}
		else if (mDateRemind != null
				&& dateRemind != null) {
			if (!(mDateRemind.equals(dateRemind))) {
				isDirty = true;
			}
		}

		// need to delete notificationItem when mDateRemind is set to null
		if (saveIt) {
			if (mDateRemind != null
					&& dateRemind == null
					&& mNotificationItemId != -1) {
				NotificationItemDO notificationItem =
						NotificationItems.get(mNotificationItemId);
				if (notificationItem != null) {
					notificationItem.delete();
				}
				mNotificationItemId = -1;
				isDirty = true;
			}
		}

		mDateRemind = dateRemind;

		if (isDirty
				&& saveIt) {
			save();
		}

		return dateRemind;
	}

	public boolean isExpired(){
		if (!mFavorite) {
			if (mStatus.equals(Status.TRASH)) {
				if (!GeneralPrefsDO.isKeepDeleted()	// == !mKeepDeleted
						|| (mDateExpires != null	// mKeepDeleted but expired
						&& new Date(System.currentTimeMillis()).after(getDateExpires()))) {
					return true;
				}
			}
			if (mStatus.equals(Status.DONT_SHOW)) {
				// get rid of these after a day
				long millis = mTimeStamp.getTime() + (
						AutomatonAlert.MAX_TIME_TO_KEEP_DONT_SHOW_IN_LIST_ITEMS);
				if (new Date(System.currentTimeMillis()).after(new Date(millis))) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isPastDateRemindAndRepeat() {
		long last = getLastIteratedAlarm();

		return !(last > System.currentTimeMillis());
	}

	public long getLastIteratedAlarm() {
		if (mDateRemind == null) {
			return 0;
		}
		return getLastIteratedAlarm(
				mDateRemind.getTime(), mRepeatEvery, mStopAfter);
	}

	public static long getLastIteratedAlarm(AlertItemDO alertItem) {
		return getLastIteratedAlarm(
				Utils.getDateRemindLong(alertItem.getDateRemind()),
				alertItem.getRepeatEvery(),
				alertItem.getStopAfter());
	}

	private static long getLastIteratedAlarm(long start, long every, long stopAfter) {
		if (start <= 0
				|| every <= 0
				|| stopAfter <= 0)  {
			return start;
		}
		return start + (every * stopAfter);
	}


	private static int getCurrentAlarmIterationMillis(
			Date dateRemind, long repeatEvery) {

		if (repeatEvery <= 0) {
			return Integer.MAX_VALUE;
		}

		if (dateRemind == null) {
			return Integer.MAX_VALUE;
		}

		long dateRemindMillis = dateRemind.getTime();
		long nowMillis = System.currentTimeMillis();
		int iteration = (int)((nowMillis - dateRemindMillis) / repeatEvery);

		if (iteration < 0) {
			return Integer.MAX_VALUE;
		}

		return iteration;
	}

	public static int getCurrentAlarmIteration(Date dateRemind, long repeatEvery) {
		if (dateRemind == null) {
			return Integer.MAX_VALUE;
		}
		// no repeat, return 0
		if (repeatEvery <= 0) {
			return 0;
		}

		int iteration = 0;
		long next = -1;

		if (repeatEvery == MONTHLY_MILLIS) {
			next = getNextOrCurrentIteratedAlarmNonMillis(
					Calendar.MONTH, dateRemind.getTime(), repeatEvery, false/*getNext*/);
			iteration = (int)next;
		}
		else if (repeatEvery == YEARLY_MILLIS) {
			next = getNextOrCurrentIteratedAlarmNonMillis(
					Calendar.YEAR, dateRemind.getTime(), repeatEvery, false/*getNext*/);
			iteration = (int)next;
		}
		else {
			iteration = getCurrentAlarmIterationMillis(dateRemind, repeatEvery);
		}

		return iteration;
	}

	private static long getNextOrCurrentIteratedAlarmNonMillis(
			int period, long dateRemind, long stopAfter, boolean getNext) {

		long next = -1;

		Calendar now = Calendar.getInstance();

		if (new Date(dateRemind).after(now.getTime())) {
			return dateRemind;
		}

		Calendar res = Calendar.getInstance();
		res.setTimeInMillis(dateRemind);

		for (int i=1; i<=stopAfter; i++) {
			res.add(period, 1);
			if (res.after(now)) {
				if (getNext) {
					return res.getTimeInMillis();
				}
				else {
					return (i - 1);
				}
			}
		}

		return next;
	}

	private static long getNextIteratedAlarmMonthly(long dateRemind, long stopAfter) {
		return getNextOrCurrentIteratedAlarmNonMillis(
				Calendar.MONTH, dateRemind, stopAfter, true/*getNext*/);
	}

	private static long getNextIteratedAlarmYearly(long dateRemind, long stopAfter) {
		return getNextOrCurrentIteratedAlarmNonMillis(
				Calendar.YEAR, dateRemind, stopAfter, true/*getNext*/);
	}

	public static long getNextIteratedAlarmMillis(
			long dateRemind, long repeatEvery, long stopAfter) {

		if (repeatEvery <= 0) {
			return -1;
		}

		long now = System.currentTimeMillis();

		if (dateRemind > now) {
			return dateRemind;
		}

		long next = -1;
		long diff = now - dateRemind;

		if (diff > 0) {
			long times = diff / repeatEvery;
			times++; // next
			if (times <= stopAfter) {
				next = dateRemind + (repeatEvery * times);
			}
		}

		return next;
	}

	public long getNextIteratedAlarm() {
		if (mDateRemind == null) {
			return -1;
		}
		long next = -1;

		// if dateRemind is after now, just return dateRemind
		Date rightNow = new Date(System.currentTimeMillis());
		long lDateRemind = cutOffSeconds(mDateRemind.getTime());

		if (new Date(lDateRemind).after(rightNow)) {
			return lDateRemind;
		}

		if (mRepeatEvery == MONTHLY_MILLIS) {
			next = getNextIteratedAlarmMonthly(lDateRemind, mStopAfter);
		}
		else if (mRepeatEvery == YEARLY_MILLIS) {
			next = getNextIteratedAlarmYearly(lDateRemind, mStopAfter);
		}
		else {
			next = getNextIteratedAlarmMillis(
					lDateRemind, mRepeatEvery, mStopAfter);
		}

		return next;
	}

	public void findCancelRemovePendingIntentsPostAlarms(ApiSubType subType) {
		AutomatonAlert.getAPIs().findCancelRemovePendingIntentsPostAlarms(
				ApiType.ALERT,
				subType,
				-1,
				mAlertItemId,
				mNotificationItemId);
	}

	public static FragmentTypeRT getFragmentTypeFromAlertItem(AlertItemDO alertItem) {
		if (alertItem != null) {
			String aType = alertItem.getKvRawDetails().get(
					ContactAlert.TAG_MESSAGE_SOURCE_HEADER);
			if (aType != null) {
				if (aType.equals(AccountSmsDO.SMS)
						|| aType.equals(AccountSmsDO.MMS)) {
					return FragmentTypeRT.TEXT;
				}
				else if (aType.equals(AccountDO.EMAIL)) {
					return FragmentTypeRT.EMAIL;
				}
			}
		}

		return null;
	}

	public static String getDisplayNameFromAlertItem(int alertItemId) {
		if (alertItemId != -1) {
			AlertItemDO alertItem = AlertItems.get(alertItemId);
			if (alertItem != null) {
				return alertItem.getKvRawDetails().get(Contacts.DISPLAY_NAME);
			}
		}

		return "<unknown>";
	}

	public static Intent setAlarmAlertIntent(
			int alertItemId,
			int notificationItemId,
			FragmentTypeRT type,
			long dateRemind,
			long repeatEvery,
			long stopAfter) {

		String displayName =
				AlertItemDO.getDisplayNameFromAlertItem(alertItemId);

		// create the intent to fire when alarm goes off
		Intent intent = new Intent(
				AutomatonAlert.THIS.getApplicationContext(),
				AlertReceiver.class);
		intent.setAction(AutomatonAlert.ALARM_ALERT_EVENT);
		intent.putExtra(AlertItemDO.TAG_ALERT_ITEM_ID, alertItemId);
		intent.putExtra(
				NotificationItemDO.TAG_NOTIFICATION_ITEM_ID,
				notificationItemId);
		intent.putExtra(RTUpdateActivity.TAG_FRAGMENT_TYPE, type.name());
		intent.putExtra(Contacts.DISPLAY_NAME, displayName);
		intent.putExtra(AutomatonAlertProvider.ALERT_ITEM_DATE_REMIND, dateRemind);
		intent.putExtra(AutomatonAlertProvider.ALERT_ITEM_REPEAT_EVERY, repeatEvery);
		intent.putExtra(AutomatonAlertProvider.ALERT_ITEM_STOP_AFTER, stopAfter);
		String ids = alertItemId + "|" + notificationItemId;
		intent.setData(Uri.parse(ids));
		intent.setFlags(0);

		return intent;
	}

	public static Intent setAlarmIntent(AlertItemDO alertItem) {
		return setAlarmAlertIntent(
				alertItem.getAlertItemId(),
				alertItem.getNotificationItemId(),
				AlertItemDO.getFragmentTypeFromAlertItem(alertItem),
				Utils.getDateRemindLong(alertItem.getDateRemind()),
				alertItem.getRepeatEvery(),
				alertItem.getStopAfter());
	}

	public Intent setAlarmIntent() {
		return setAlarmAlertIntent(
				mAlertItemId,
				mNotificationItemId,
				getFragmentTypeFromAlertItem(this),
				mDateRemind.getTime(),
				mRepeatEvery,
				mStopAfter);
	}

	public static AlarmPendingIntent setApi(
			Intent intent, int requestCode,
			AlertItemDO alertItem, ApiSubType subType) {

		// cancel before add
		if (alertItem != null) {
			// alarms - send along subType
			alertItem.findCancelRemovePendingIntentsPostAlarms(subType);
		}

		// create our new one

		return new AlarmPendingIntent(
				ApiType.ALERT,
				subType,
				-1,
				(alertItem == null ? -1 : alertItem.getAlertItemId()),
				(alertItem == null ? -1 : alertItem.getNotificationItemId()),
				requestCode,
				intent,
				PendingIntent.FLAG_CANCEL_CURRENT);
	}

	public AlarmPendingIntent setApi(
			Intent intent, int requestCode, ApiSubType subType) {

		return setApi(intent, requestCode, this, subType);
	}

	public static boolean setTheAlarm(AlertItemDO alertItem, AlarmPendingIntent api) {
		// this may be a repeat
		if (alertItem != null) {
			long time = alertItem.getNextIteratedAlarm();
//			time = cutOffSeconds(time);
			if (time > System.currentTimeMillis()) {
				api.setAlarm(time);
				return true;
			}
		}
		return false;
	}

	private void setTheAlarm(AlarmPendingIntent api) {
		setTheAlarm(this, api);
	}

	public void setAlarm(ApiSubType subType) {
		// make sure we should be here
		if (mStatus.equals(Status.TRASH)
				|| mStatus.equals(Status.DONT_SHOW)) {
			return;
		}
		if (isPastDateRemindAndRepeat()) {
			// get rid of past alarm
//			if (mDateRemind != null) {
//				mDateRemind = null;
//			}
			return;
		}

		// make sure we can get the notificationItem rec
		NotificationItemDO notificationItem =
				NotificationItems.get(mNotificationItemId);
		if (notificationItem == null) {
			return;
		}

		Intent intent = setAlarmIntent();
		int reqCode = getRequestCode(this, FragmentTypeAL.ALARMS);
		// alarms - ALARM or REPEAT
		AlarmPendingIntent api = setApi(intent, reqCode, subType);
		setTheAlarm(api);
	}

	public static int getRequestCode(
			AlertItemDO alertItem, FragmentTypeAL type) {

		// divide Integer.MAX_VALUE / 3 and get about 700million.
		// each category (alarm, snooze, repeat) will be in
		// one of these divisions:
		// ALARM 1 - 700,000,000
		// SNOOZE 700,000,001 - 1,400,000,000
		// REPEAT 1,400,000,001 - MAX
		if (alertItem != null) {
			if (alertItem.getAlertItemId() != -1) {
				int id = alertItem.getAlertItemId();
				switch (type) {
				case SNOOZED:
					id += 700000000;
					break;
				case REPEATS:
					id += 1400000000;
					break;
				default:
					break;
				}
				return Math.min(Integer.MAX_VALUE, id);
			}
		}

		return 9876543;	// alarm
	}

	private static FragmentTypeRT getType(AlertItemDO alertItem) {
		if (alertItem == null) {
			return FragmentTypeRT.EMAIL;
		}
		String sType =
				alertItem.getKvRawDetails().get(
						ContactAlert.TAG_MESSAGE_SOURCE_HEADER);
		if (sType.equals(AccountSmsDO.MMS)
				|| sType.equals(AccountSmsDO.SMS)) {
			return FragmentTypeRT.TEXT;
		}
		else if (sType.equals(AccountDO.EMAIL)) {
			return FragmentTypeRT.EMAIL;
		}
		return FragmentTypeRT.EMAIL;
	}

	private static long addUpdatePostAlarm(
			PostAlarmDO postAlarm, AlertItemDO alertItem, long snoozeMillis) {

		FragmentTypeRT type = getType(alertItem);

		// default to AutoAckAfter.  if alarmTime is
		// in the past, the alert will go off right away and
		// create a loop that can only be broken by a fc
		long alarmTime = System.currentTimeMillis() + GeneralPrefsDO.getAutoAckAfter();

		if (snoozeMillis != -1) {
			// computed time. don't cutOffSeconds since this may
			// be a fine-grained alarm (less than a minute)
			alarmTime = System.currentTimeMillis() + snoozeMillis;
		}
		else {
			// PostAlarm.mNextAlarm
			if (postAlarm != null) {
				alarmTime = postAlarm.getNextAlarm();
			}
			alarmTime = cutOffSeconds(alarmTime);
		}

		if (postAlarm == null) {
			postAlarm = new PostAlarmDO(
					type,
					alertItem.getAlertItemId(),
					alertItem.getNotificationItemId(),
					alarmTime,
					Utils.getDateRemindLong(alertItem.getDateRemind()));
		}
		else {
			postAlarm.setNextAlarm(alarmTime);
		}
		postAlarm.save();

		return alarmTime;
	}

	public static PostAlarmDO cancelPostAlarm(AlertItemDO alertItem) {
		if (alertItem == null) {
			return null;
		}
		return PostAlarmDO.cancelPostAlarm(
				alertItem.getAlertItemId(),
				alertItem.getNotificationItemId());
	}

	private static void doTheSnooze(AlertItemDO alertItem, long alarmTime) {
		if (alertItem == null) {
			return;
		}
		Intent intent = setAlarmIntent(alertItem);

		//make the intent a snooze intent
		String data = intent.getDataString();
		data += "|" + AutomatonAlert.SNOOZE;
		intent.setData(Uri.parse(data));

		// setup our tracking object
		// and add our snooze stuff
		// alarms - cancel SNOOZE only
		AlarmPendingIntent api = AlertItemDO.setApi(
				intent,
				getRequestCode(alertItem, FragmentTypeAL.SNOOZED),
				alertItem,
				ApiSubType.SNOOZE);
		api.setAlarm(alarmTime);
	}

	public static void setSnooze(AlertItemDO alertItem, PostAlarmDO postAlarm) {
		long alarmTime = postAlarm.getNextAlarm();

		alertItem.updateDateRemind(
				new Date(alarmTime), false/*don't save*/);
		// make sure it's exact, sending millisTime=-1
		// truncates the seconds
		setSnooze(alertItem, null, alarmTime - System.currentTimeMillis());
	}

	// compute PostAlarm.mNextAlarm from now+snooze.
	// comes from anywhere that only the snooze duration
	// is known.
	public synchronized static void setSnooze(
			AlertItemDO alertItem, SoundBomb soundBomb, long snooze) {

		if (soundBomb != null) {
			soundBomb.turnOffVibrateAndSound();
		}
		// don't delete the PostAlarmDO because it's either not there (doing a new)
		// or it's there and we'll update it.
		PostAlarmDO postAlarm = cancelPostAlarm(alertItem);
		long alarmTime = addUpdatePostAlarm(postAlarm, alertItem, snooze);
		doTheSnooze(alertItem, alarmTime);
	}

	public static long cutOffSeconds(long in) {
		long out = in/1000;   	// from milliseconds to seconds
		long rem = out % 60;  	// get extra seconds
		out -= rem;			  	// subtract them
		return out*1000;		// back to milliseconds
	}

	public static AlertItemDO createSaveNewAlertItem(
			int accountId,
			HashMap<String, String> message,
			HashMap<String, String> rawMessage,
			AlertItemType alertItemType,
			boolean saveToList,
			boolean markAsSeen) {

		AlertItemDO alertItem = null;
		String sUid = rawMessage.get(AutomatonAlert.UID);
		String source = rawMessage.get(ContactAlert.TAG_MESSAGE_SOURCE_HEADER);
		// if FREEFORM & not SMS/MMS & have UID, try to get the AlertItem
		if (alertItemType.equals(AlertItemType.SEARCH)) {
			if (!source.equals(AccountSmsDO.SMS)
					&& !source.equals(AccountSmsDO.MMS)) {
				if (sUid != null) {
					alertItem = AlertItems.get(sUid, accountId);
				}
			}
		}
		if (alertItem == null) {
			alertItem =	new AlertItemDO(
					alertItemType, sUid, message, rawMessage, accountId);
			alertItem.save();
		}
		// depending on mSaveToList and mMarkAsSeen
		Status status = getNewAlertItemStatus(saveToList, markAsSeen);
		if (!(status.equals(alertItem.getStatus()))) {
			alertItem.updateStatus(status);
		}
		return alertItem;
	}

	private static Status getNewAlertItemStatus(
            boolean saveToList, boolean markAsSeen) {

		if (saveToList) {
			if (markAsSeen) {
				return Status.SAVED;
			}
			return Status.NEW;
		}
		else {
			return Status.DONT_SHOW;
		}
	}

	public static SoundBomb createDefaultTextNotification() {
		// get rid of existing
		if (mDefaultNotificationItem != null
				&& mDefaultNotificationItem.getNotificationItemId() != -1) {
			mDefaultNotificationItem.delete();
		}
		if (mDefaultFilterItem != null
				&& mDefaultFilterItem.getFilterItemId() != -1) {
			mDefaultFilterItem.delete();
		}
		if (mSoundBomb != null) {
			mSoundBomb = null;
		}
		// build
		mDefaultNotificationItem = new NotificationItemDO();
		// save for FilterItem
		mDefaultNotificationItem.save();

		mDefaultFilterItem = new FilterItemDO();
		mDefaultFilterItem.setNotificationItemId(
				mDefaultNotificationItem.getNotificationItemId());

		// complete NotificationItem
		mDefaultNotificationItem.setSoundPath(RTPrefsDO.getDefaultTextRingtone());
		mDefaultNotificationItem.setVolumeLevel(RTPrefsDO.getDefaultTextVolume());
		mDefaultNotificationItem.setPlayFor(RTPrefsDO.getDefaultTextPlayFor());
		mDefaultNotificationItem.setNoAlertScreen(true);
		mDefaultNotificationItem.setVibrateMode(RTPrefsDO.getDefaultTextVibrateMode());
		mDefaultNotificationItem.setSilentMode(RTPrefsDO.getDefaultTextSilentMode());
		mDefaultNotificationItem.setShowInNotificationBar(RTPrefsDO.isDefaultTextNotification());
		mDefaultNotificationItem.setLedMode(RTPrefsDO.getDefaultTextLight());
		// 2 of 2 saves, this one to save above updates
		mDefaultNotificationItem.save();

		return new SoundBomb(
				AutomatonAlert.THIS.getApplicationContext(),
				false,/*thisIsATest*/
				true,/*usePlayFor*/
				FragmentTypeRT.TEXT,
				-1, //alertItemId
				mDefaultNotificationItem,
				"Global Text Ringtone");
	}

	public synchronized void delete() {
		try {

			Uri uri = ContentUris.withAppendedId(
					AutomatonAlertProvider.ALERT_ITEM_ID_URI, mAlertItemId);
			AutomatonAlert.getProvider().delete(uri, null, null);

		} catch (RemoteException e) {
			Log.e(TAG + ".delete()", "delete exception: " + e.toString());
		}
		// clean up
		// alarms - this cancel ALL
		findCancelRemovePendingIntentsPostAlarms(ApiSubType.ALARM);

		// delete dependent notification item
		if (mNotificationItemId >= 0) {
			NotificationItemDO notificationItem =
					NotificationItems.get(mNotificationItemId);
			if (notificationItem != null) {
				notificationItem.delete();
			}
		}
		// leave notificationItem hanging, will clean up on re-entry to app
	}

	public synchronized void save() {
		// don't save fake or invalid AlertItemDO
		if (mAlertItemId == FAKE_ALERT_ID
				|| mAccountId == -1) {
			return;
		}

		ContentValues cv = AutomatonAlertProvider.getAlertItemContentValues(
				mType.name(),
				mUid,
				Boolean.toString(mFavorite),
				(mDateRemind == null) ? -1 : mDateRemind.getTime(),
				mRepeatEvery,
				mStopAfter,
				detailsToString(mKvRawDetails),
				detailsToString(mKvDetails),
				mNotificationItemId,
				mAccountId,
				mStatus.toString(),
				(mDateExpires == null) ? -1 : mDateExpires.getTime()
				);

		AutomatonAlertProvider aap =
				(AutomatonAlertProvider)AutomatonAlert.getProvider()
				.getLocalContentProvider();

        int id = aap == null ? -1 :
                aap.insertOrUpdate(
				cv,
				mAlertItemId,
				AutomatonAlertProvider.ALERT_ITEM_ID_URI,
				AutomatonAlertProvider.ALERT_ITEM_TABLE_URI);

		// if inserted, store new id
		if (id != -1
				&& id != mAlertItemId) {
			mAlertItemId = id;
		}

		isDirty = false;

	}

	public static boolean isSmsMms(HashMap<String, String> message) {
		String source = message.get(ContactAlert.TAG_MESSAGE_SOURCE_HEADER);
		return source != null
				&& (source.equals(AccountSmsDO.SMS) || source.equals(AccountSmsDO.MMS));
	}

}

