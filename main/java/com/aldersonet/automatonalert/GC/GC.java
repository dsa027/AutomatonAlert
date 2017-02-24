package com.aldersonet.automatonalert.GC;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

import com.aldersonet.automatonalert.Alert.AlertItemDO;
import com.aldersonet.automatonalert.Alert.AlertItems;
import com.aldersonet.automatonalert.Alert.NotificationItemDO;
import com.aldersonet.automatonalert.Alert.NotificationItems;
import com.aldersonet.automatonalert.Util.Utils;

import java.util.ArrayList;

public class GC {

	private static final String TAG = "GC";
	private Context mContext;
	private static GC mInstance = null;
	private static int mCounter = 0;

	private GC(Context context) {
		super();

		mContext = context;
	}

	public static GC getGC(Context context) {
		if (mInstance == null) {
			mInstance = new GC(context);
		}

		return mInstance;
	}

	synchronized public void doGC() {
		new Thread(new Runnable() {
			@Override
			public void run() {

				deleteExpiredTrash();

				// every 4th time (every 4 hours is GC is run every hour)
				if (++mCounter % 4 == 0) {
					try {
						Utils.trimSourceType(mContext);
					} catch (RemoteException ignored) {}

					Utils.makeSureAllPhoneRTVMHaveSourceType(mContext);
				}
			}

			private void deleteExpiredTrash() {
				int alertItemsDeleted = 0;
				int notificationItemsDeleted = 0;

				ArrayList<AlertItemDO> alertItems = AlertItems.getExpiredTrash();
				NotificationItemDO notificationItem = null;

				for (AlertItemDO alertItem : alertItems) {
					if (alertItem.getNotificationItemId() > 0) {
						notificationItem =
								NotificationItems.get(alertItem.getNotificationItemId());
						if (notificationItem != null) {
							++notificationItemsDeleted;
							notificationItem.delete();
						}
					}
					++alertItemsDeleted;
					alertItem.delete();
				}
				Log.d(TAG + ".doGC()", "!!!!!!!!!!!! doGC  !!!!!!!!!!!!!!!!!!");
				Log.d(TAG + ".doGC()", "AlertItems deleted[" + alertItemsDeleted + "]");
				Log.d(TAG + ".doGC()", "NotificationItems deleted[" + notificationItemsDeleted + "]");
			}
		}).start();
	}
}
