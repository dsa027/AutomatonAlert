package com.aldersonet.automatonalert.Alarm;


import android.util.Pair;

import com.aldersonet.automatonalert.Activity.RTUpdateActivity;
import com.aldersonet.automatonalert.Alarm.AlarmPendingIntent.ApiSubType;
import com.aldersonet.automatonalert.Alarm.AlarmPendingIntent.ApiType;
import com.aldersonet.automatonalert.Alert.AlertItemDO;
import com.aldersonet.automatonalert.Alert.AlertItems;
import com.aldersonet.automatonalert.PostAlarm.PostAlarmDO;

import java.util.ArrayList;
import java.util.HashSet;

public class AlarmPendingIntents extends HashSet<AlarmPendingIntent> {

	@Override
	public boolean add(AlarmPendingIntent object) {
		synchronized (this) {
			return super.add(object);
		}
	}

	@Override
	public void clear() {
		synchronized (this) {
			super.clear();
		}
	}

	@Override
	public boolean remove(Object object) {
		synchronized (this) {
			return super.remove(object);
		}
	}

	public void cancelAllAlarms() {
		synchronized (this) {
			for (AlarmPendingIntent api : this) {
				api.mAlarmManager.cancel(api.mPendingIntent);
			}
		}
		clear();
	}

	public ArrayList<Pair<AlertItemDO, PostAlarmDO>> getSnoozes() {
		ArrayList<Pair<AlertItemDO, PostAlarmDO>> recs =
				new ArrayList<Pair<AlertItemDO, PostAlarmDO>>();

		synchronized (this) {
			 for (AlarmPendingIntent api : this) {
				 if (api.mApiType.equals(ApiType.ALERT)
						 && api.mApiSubType.equals(ApiSubType.SNOOZE)) {
					 AlertItemDO alertItem = AlertItems.get(api.mAlertItemId);
					 if (alertItem != null) {
						 PostAlarmDO postAlarm = getPostAlarm(alertItem);
						 if (postAlarm != null) {
							 recs.add(new Pair<AlertItemDO, PostAlarmDO>(
									 alertItem, postAlarm));
						 }
					 }
				 }
			 }
		 }

		return recs;
	}

	// getPostAlarm() ensures there's a PostAlarmDO returned
	// even if it's a temp that has api.mAlarmTime as snooze
	private PostAlarmDO getPostAlarm(AlertItemDO alertItem) {
		if (alertItem == null) {
			return null;
		}

		// get the PostAlarmDO
		ArrayList<PostAlarmDO> pas =
				PostAlarmDO.get(alertItem.getAlertItemId(), alertItem.getNotificationItemId());
		if (pas.size() > 0) {
			return pas.get(0);
		}

		// PostAlarmDO not found, see if it's in AlarmPendingIntents
		// TODO: need to see if this is necessary (is there ever a temp?)
		AlarmPendingIntent api = null;
		// can't synchronize this, it's already synchronized in calling method
		for (AlarmPendingIntent ap : this) {
			if (ap.mApiType.equals(ApiType.ALERT)
					&& ap.mApiSubType.equals(ApiSubType.SNOOZE)) {
				if (alertItem.getAlertItemId() == ap.mAlertItemId
						&& alertItem.getNotificationItemId() == ap.mNotificationItemId) {
					api = ap;
				}
			}
		}

		if (api != null) {
			return new PostAlarmDO(
					RTUpdateActivity.FragmentTypeRT.TEXT,
					alertItem.getAlertItemId(),
					alertItem.getNotificationItemId(),
					api.mAlarmTime,
					alertItem.getDateRemind().getTime());
		}

		return null;
	}

	public boolean findCancelRemovePendingIntentsPostAlarms(
			AlarmPendingIntent api) {

		return findCancelRemovePendingIntentsPostAlarms(
				api.mApiType,
				api.mApiSubType,
				api.mAccountId,
				api.mAlertItemId,
				api.mNotificationItemId);
	}

	public boolean findCancelRemovePendingIntentsPostAlarms(
			ApiType type,
			ApiSubType inSubType,
			int accountId,
			int alertItemId,
			int notificationItemId) {

		boolean found = false;

		ArrayList<AlarmPendingIntent> apis = getAlarmPendingIntents(
				type, accountId, alertItemId, notificationItemId);

		synchronized (this) {
			for (AlarmPendingIntent api : apis) {
				// for ALERT's, need to figure out what
				// exactly to cancel, repeat? snooze? all?
				if (type.equals(ApiType.ALERT)) {
					switch (inSubType) {
						case ALARM:
							// cancel ALL
							api.cancelAlarm();
							deletePostAlarms(api.mAlertItemId, api.mNotificationItemId);
							found = true;
							break;
						case SNOOZE:
							// cancel SNOOZE only
							if (api.mApiSubType.equals(ApiSubType.SNOOZE)) {
								deletePostAlarms(api.mAlertItemId, api.mNotificationItemId);
								api.cancelAlarm();
								found = true;
							}
							break;
						case REPEAT:
							// cancel REPEAT only
							if (api.mApiSubType.equals(ApiSubType.REPEAT)) {
								api.cancelAlarm();
								found = true;
							}
							break;
						case REMINDER:
							// cancel REMINDER only
							if (api.mApiSubType.equals(ApiSubType.REMINDER)) {
								api.cancelAlarm();
								found = true;
							}
							break;
						case NONE:
							// do nada
							break;
					}
				} else {
					// GC_POLL, EMAIL_POLL
					api.cancelAlarm();
					found = true;
				}
			}
		}

		return found;
	}

	private void deletePostAlarms(int alertItemId, int notificationItemId) {
		ArrayList<PostAlarmDO> postAlarms =
				PostAlarmDO.get(alertItemId, notificationItemId);
		for (PostAlarmDO postAlarm : postAlarms) {
			postAlarm.delete();
		}
	}

	public ArrayList<AlarmPendingIntent> getAlarmPendingIntents(
			ApiType type, ApiSubType subType) {

		ArrayList<AlarmPendingIntent> apis = new ArrayList<AlarmPendingIntent>();
		synchronized (this) {
			for (AlarmPendingIntent ap : this) {
				if (ap.mApiType.equals(type)) {
					if (ap.mApiSubType.equals(subType)) {
						apis.add(ap);
					}
				}
			}
		}

		return apis;
	}



	public ArrayList<AlarmPendingIntent> getAlarmPendingIntents(
			ApiType type, ApiSubType subType,
			int accountId, int alertItemId, int notificationItemId) {

		ArrayList<AlarmPendingIntent> apis = new ArrayList<AlarmPendingIntent>();

		synchronized (this) {
			for (AlarmPendingIntent ap : this) {
				if (ap.mApiType.equals(type)
						&& ap.mApiSubType.equals(subType)) {
					if (accountId != -1) {
						if (accountId == ap.mAccountId) {
							apis.add(ap);
							continue;
						}
					}
					if (alertItemId != -1
							&& notificationItemId != -1) {
						if (alertItemId == ap.mAlertItemId
								&& notificationItemId == ap.mNotificationItemId) {
							apis.add(ap);
						}
					}
				}
			}
		}

		return apis;
	}

	public ArrayList<AlarmPendingIntent> getAlarmPendingIntents(
			ApiType type, int accountId, int alertItemId, int notificationItemId) {

		ArrayList<AlarmPendingIntent> apis = new ArrayList<AlarmPendingIntent>();

		synchronized (this) {
			for (AlarmPendingIntent ap : this) {
				if (ap.mApiType.equals(type)) {
					switch (type) {
						case GC_POLL:
							apis.add(ap);

						case EMAIL_POLL:
							if (accountId == ap.mAccountId) {
								apis.add(ap);
							}
							break;

						case ALERT:
							if (alertItemId == ap.mAlertItemId
									&& notificationItemId == ap.mNotificationItemId) {
								apis.add(ap);
							}
							break;
					}
				}
			}
		}

		return apis;
	}
}

