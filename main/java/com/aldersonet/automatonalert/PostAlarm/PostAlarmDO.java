package com.aldersonet.automatonalert.PostAlarm;

import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Log;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Alarm.AlarmPendingIntent;
import com.aldersonet.automatonalert.Alarm.AlarmPendingIntent.ApiSubType;
import com.aldersonet.automatonalert.Activity.RTUpdateActivity.FragmentTypeRT;
import com.aldersonet.automatonalert.Provider.AutomatonAlertProvider;
import java.util.ArrayList;
import java.util.Date;

public class PostAlarmDO {

	public static final String TAG = "PostAlarmDO";

	private int mPostAlarmId = -1;
	private FragmentTypeRT mType;
	private int mAlertItemId;
	private int mNotificationItemId;
	private long mNextAlarm;
	private long mOrigAlarm;
	private Date mTimeStamp;

	public boolean isDirty;

	private PostAlarmDO() {
		super();

		mType = FragmentTypeRT.TEXT;
		mPostAlarmId = -1;
		mAlertItemId = -1;
		mNotificationItemId = -1;
		mNextAlarm = 0;
		mOrigAlarm = 0;

		isDirty = true;
	}

	public PostAlarmDO(
			FragmentTypeRT type,
			int alertItemId,
			int notificationItemId,
			long nextAlarm,
			long origAlarm) {

		this();
		mType = type;
		mAlertItemId = alertItemId;
		mNotificationItemId = notificationItemId;
		mNextAlarm = nextAlarm;
		mOrigAlarm = origAlarm;
	}

	public int getPostAlarmId() {
		return mPostAlarmId;
	}

	public FragmentTypeRT getType() {
		return mType;
	}

	public FragmentTypeRT setType(FragmentTypeRT type) {
		if (!mType.equals(type)) {
			isDirty = true;
		}
		return mType = type;
	}

	public int getAlertItemId() {
		return mAlertItemId;
	}

	public int setAlertItemId(int id) {
		if (mAlertItemId != id) {
			isDirty = true;
		}
		return mAlertItemId = id;
	}

	public int getNotificationItemId() {
		return mNotificationItemId;
	}

	public int setNotificationItemId(int id) {
		if (mNotificationItemId != id) {
			isDirty = true;
		}
		return mNotificationItemId = id;
	}

	public long getNextAlarm() {
		return mNextAlarm;
	}

	public long setNextAlarm(long nextAlarm) {
		if (mNextAlarm != nextAlarm) {
			isDirty = true;
		}
		return mNextAlarm = nextAlarm;
	}

	public long getOrigAlarm() {
		return mOrigAlarm;
	}

	public long setOrigAlarm(long OrigAlarm) {
		if (mOrigAlarm != OrigAlarm) {
			isDirty = true;
		}
		return mOrigAlarm = OrigAlarm;
	}

	public static PostAlarmDO get(int id) {

		Uri uri = ContentUris.withAppendedId(
				AutomatonAlertProvider.POST_ALARM_ID_URI, id);

		PostAlarmDO postAlarm = null;

		Cursor cursor = null;
		try {
			cursor = AutomatonAlert.getProvider().query(
					uri, null, null, null, null);
			if (cursor.moveToFirst()) {
				postAlarm = new PostAlarmDO().populate(cursor);
			}
		}
		catch (RemoteException ignored) {}
		catch (IllegalArgumentException ignored) {}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return postAlarm;	// null if not found
	}

	public static ArrayList<PostAlarmDO> get(long after) {
		ArrayList<PostAlarmDO> list = new ArrayList<PostAlarmDO>();

        Cursor cursor = null;
        try {
            cursor = AutomatonAlert.getProvider().query(
                    AutomatonAlertProvider.POST_ALARM_TABLE_URI,
                    null,
                    AutomatonAlertProvider.POST_ALARM_NEXT_ALARM + " > ?",
                    new String[] { after+"" },
                    null);
        } catch (RemoteException e) {
            cursor = null;
            e.printStackTrace();
        }
        try {
			if (cursor != null && cursor.moveToFirst()) {
				do {
					list.add(new PostAlarmDO().populate(cursor));
				} while (cursor.moveToNext());
			}
		}
		catch (IllegalArgumentException ignored) {}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return list;
	}

	public static ArrayList<PostAlarmDO> get(int alertItemId, int notificationItemId) {
		ArrayList<PostAlarmDO> list = new ArrayList<PostAlarmDO>();

        Cursor cursor = null;
        try {
            cursor = AutomatonAlert.getProvider().query(
                    AutomatonAlertProvider.POST_ALARM_TABLE_URI,
                    null,
                    AutomatonAlertProvider.POST_ALARM_ALERT_ITEM_ID
                            + " = ? AND "
                            + AutomatonAlertProvider.POST_ALARM_NOTIFICATION_ITEM_ID
                            + " = ?",
                            new String[] { alertItemId+"", notificationItemId+"" },
                    null);
        } catch (RemoteException e) {
            cursor = null;
            e.printStackTrace();
        }
        try {
			if (cursor != null && cursor.moveToFirst()) {
				do {
					list.add(new PostAlarmDO().populate(cursor));
				} while (cursor.moveToNext());
			}
		}
		catch (IllegalArgumentException ignored) {}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return list;
	}

	public static ArrayList<PostAlarmDO> get() {
		ArrayList<PostAlarmDO> list = new ArrayList<PostAlarmDO>();

        Cursor cursor = null;
        try {
            cursor = AutomatonAlert.getProvider().query(
                    AutomatonAlertProvider.POST_ALARM_TABLE_URI, null, null, null, null);
        } catch (RemoteException e) {
            cursor = null;
            e.printStackTrace();
        }
        try {
			if (cursor != null && cursor.moveToFirst()) {
				do {
					list.add(new PostAlarmDO().populate(cursor));
				} while (cursor.moveToNext());
			}
		}
		catch (IllegalArgumentException ignored) {}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return list;
	}

	public PostAlarmDO cancelPostAlarm() {
		return cancelPostAlarm(mAlertItemId, mNotificationItemId);
	}

	public static PostAlarmDO cancelPostAlarm(int alertItemId, int notificationItemId) {
		// get what's being deleted before it's gone
		ArrayList<PostAlarmDO> returnPas =
				PostAlarmDO.get(alertItemId, notificationItemId);

		// cancel alarms, delete PostAlarm's
		// alarms - SNOOZE only
		AutomatonAlert.getAPIs().findCancelRemovePendingIntentsPostAlarms(
				AlarmPendingIntent.ApiType.ALERT,
				ApiSubType.SNOOZE,
				-1,
				alertItemId,
				notificationItemId);

		if (returnPas.size() > 0) {
			return returnPas.get(0);
		}
		return null;
	}

	public PostAlarmDO populate(Cursor cursor) {

		isDirty = false;

		mPostAlarmId = cursor.getInt(
				cursor.getColumnIndex(AutomatonAlertProvider.POST_ALARM_ID));

		String sType = cursor.getString(
				cursor.getColumnIndex(AutomatonAlertProvider.POST_ALARM_TYPE));
		try {
			mType = FragmentTypeRT.valueOf(sType);
		}
		catch (IllegalArgumentException e) {
			mType = FragmentTypeRT.TEXT;
		}
		catch (NullPointerException e2) {
			mType = FragmentTypeRT.TEXT;
		}

		mAlertItemId = cursor.getInt(
				cursor.getColumnIndex(AutomatonAlertProvider.POST_ALARM_ALERT_ITEM_ID));

		mNotificationItemId = cursor.getInt(
				cursor.getColumnIndex(AutomatonAlertProvider.POST_ALARM_NOTIFICATION_ITEM_ID));

		mNextAlarm = cursor.getLong(
				cursor.getColumnIndex(AutomatonAlertProvider.POST_ALARM_NEXT_ALARM));

		mOrigAlarm = cursor.getLong(
				cursor.getColumnIndex(AutomatonAlertProvider.POST_ALARM_ORIG_ALARM));

		long millis = cursor.getLong(
				cursor.getColumnIndex(AutomatonAlertProvider.SOURCE_TYPE_TIMESTAMP));
		if (millis != -1) {
			mTimeStamp = new Date(millis);
		}
		else {
			mTimeStamp = new Date(System.currentTimeMillis());
		}

		return this;
	}

	public synchronized void delete() {
		try {
			Uri uri = ContentUris.withAppendedId(
					AutomatonAlertProvider.POST_ALARM_ID_URI, mPostAlarmId);
			AutomatonAlert.getProvider().delete(uri, null, null);

		} catch (RemoteException e) {
			Log.e(TAG + ".delete()", "delete exception:  " + e.toString());
		}
	}

	public synchronized void save() {
		// save
		ContentValues cv = AutomatonAlertProvider.getPostAlarmContentValues(
				mType,
				mAlertItemId,
				mNotificationItemId,
				mNextAlarm,
				mOrigAlarm
			);

		AutomatonAlertProvider aap =
				(AutomatonAlertProvider)AutomatonAlert.getProvider()
				.getLocalContentProvider();

        int id = aap == null ? -1 :
                aap.insertOrUpdate(
				cv,
				mPostAlarmId,
				AutomatonAlertProvider.POST_ALARM_ID_URI,
				AutomatonAlertProvider.POST_ALARM_TABLE_URI);

		// if inserted, store new id
		if (id != -1
				&& id != mPostAlarmId) {
			mPostAlarmId = id;
		}

		isDirty = false;
	}

}
