package com.aldersonet.automatonalert.Alert;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;

import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Provider.AutomatonAlertProvider;

import java.util.ArrayList;


public class NotificationItems {

	public static final String TAG = "NotificationItems";

	private NotificationItems() {
		super();
	}

	public static NotificationItemDO get(int notificationItemId) {
		Uri uri = ContentUris.withAppendedId(
				AutomatonAlertProvider.NOTIFICATION_ITEM_ID_URI, notificationItemId);

		NotificationItemDO notificationItem = null;

		Cursor cursor = null;
		try {
			cursor = AutomatonAlert.getProvider().query(
					uri, null, null, null, null);
			if (cursor.moveToFirst()) {
				notificationItem = new NotificationItemDO().populate(cursor);
			}
		} catch (RemoteException ignored) {
		} catch (IllegalArgumentException ignored) {
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return notificationItem;    // null if not found
	}

	public static ArrayList<NotificationItemDO> get() {
		ArrayList<NotificationItemDO> list = new ArrayList<NotificationItemDO>();

		Cursor cursor = null;
		try {
			cursor = AutomatonAlert.getProvider().query(
					AutomatonAlertProvider.NOTIFICATION_ITEM_TABLE_URI,
					null,
					null,
					null,
					null);

			if (cursor.moveToFirst()) {
				do {
					list.add((new NotificationItemDO()).populate(cursor));

				} while (cursor.moveToNext());
			}
		} catch (RemoteException ignored) {
		} catch (IllegalArgumentException ignored) {
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return list;
	}

	////////////////////////////
	// get(id) CACHE
	////////////////////////////

//	public static final Cache mCache =
//			new Cache<NotificationItemDO, NotificationItems, Integer>(20, new NotificationItems());
//
//	public static NotificationItemDO get(int notificationItemId) {
//		return (NotificationItemDO)mCache.get(notificationItemId);
//	}
//
//	@Override
//	public NotificationItemDO getCacheRecFromSource(Object id) {
//		return getFromDb((Integer) id);
//	}

}
