package com.aldersonet.automatonalert.Alert;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.util.Pair;

import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Alert.AlertItemDO.Status;
import com.aldersonet.automatonalert.PostAlarm.PostAlarmDO;
import com.aldersonet.automatonalert.Provider.AutomatonAlertProvider;
import com.aldersonet.automatonalert.Util.Utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;

public class AlertItems {

	private AlertItems() {
		super();
	}

	public static AlertItemDO get(int id) {

		if (id < 0) {
			return null;
		}

		Uri uri = ContentUris.withAppendedId(
				AutomatonAlertProvider.ALERT_ITEM_ID_URI, id);

		AlertItemDO alertItem = null;
		Cursor cursor = null;

		try {
			cursor = AutomatonAlert.getProvider().query(
					uri, null, null, null, null);
			if (cursor.moveToFirst()) {
				alertItem = new AlertItemDO().populate(cursor);
			}
		}
		catch (RemoteException ignored) {}
		catch (IllegalArgumentException ignored) {}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}


		return alertItem;	// null if not found
	}

	public static ArrayList<AlertItemDO> get() {
		ArrayList<AlertItemDO> list = new ArrayList<AlertItemDO>();

		Cursor cursor = null;
		try {
			cursor = AutomatonAlert.getProvider().query(
					AutomatonAlertProvider.ALERT_ITEM_TABLE_URI,
					null,
					null,
					null,
					null);

			if (cursor.moveToFirst()) {
				do {
					list.add((new AlertItemDO()).populate(cursor));

				} while (cursor.moveToNext());
			}
		}
		catch (RemoteException ignored) {}
		catch (IllegalArgumentException ignored) {}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return list;
	}

	public static ArrayList<AlertItemDO> getRepeatEvery() {

		Cursor cursor = null;
		ArrayList<AlertItemDO> alertItems = new ArrayList<AlertItemDO>();

		try {
			cursor = AutomatonAlert.getProvider().query(
					AutomatonAlertProvider.ALERT_ITEM_TABLE_URI,
					null,
						AutomatonAlertProvider.ALERT_ITEM_STATUS
						+ " <> ? AND "
						+ AutomatonAlertProvider.ALERT_ITEM_STATUS
						+ " <> ? AND "
						+ AutomatonAlertProvider.ALERT_ITEM_REPEAT_EVERY
						+ " <> ? AND "
						+ AutomatonAlertProvider.ALERT_ITEM_STOP_AFTER
						+ " <> ? AND "
						+ AutomatonAlertProvider.ALERT_ITEM_DATE_REMIND
						+ " > ?",
					new String[] {
							Status.TRASH.name(),
							Status.DONT_SHOW.name(),
							"0",
							"0",
							"0" },
					null);

			if (cursor.moveToFirst()) {
				do {
					alertItems.add(new AlertItemDO().populate(cursor));
				} while (cursor.moveToNext());
			}
		}
		catch (RemoteException ignored) {}
		catch (IllegalArgumentException ignored) {}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return alertItems;
	}

	public static ArrayList<AlertItemDO> getExpiredTrash() {
		Cursor cursor = null;
		ArrayList<AlertItemDO> alertItems = new ArrayList<AlertItemDO>();
		long now = System.currentTimeMillis();

		try {
			cursor = AutomatonAlert.getProvider().query(
					AutomatonAlertProvider.ALERT_ITEM_TABLE_URI,
					null,
						AutomatonAlertProvider.ALERT_ITEM_STATUS
						+ " = ? AND "
						+ AutomatonAlertProvider.ALERT_ITEM_DATE_EXPIRES
						+ " > ?",
					new String[] {
						Status.TRASH.name(),
						"0"},
					null);

			if (cursor.moveToFirst()) {
				int expireIdx = cursor.getColumnIndex(
						AutomatonAlertProvider.ALERT_ITEM_DATE_EXPIRES);
				do {
					long expire = Utils.getLong(cursor, expireIdx);
					if (expire != -1
							&& expire < now) {
						alertItems.add(new AlertItemDO().populate(cursor));
					}
				} while (cursor.moveToNext());
			}
		}
		catch (RemoteException ignored) {}
		catch (IllegalArgumentException ignored) {}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return alertItems;
	}

	public static AlertItemDO getNotificationItemId(int id) {

		if (id < 0) {
			return null;
		}

		Cursor cursor = null;
		AlertItemDO alertItem = null;

		try {
			cursor = AutomatonAlert.getProvider().query(
					AutomatonAlertProvider.ALERT_ITEM_TABLE_URI,
					null,
					AutomatonAlertProvider.ALERT_ITEM_NOTIFICATION_ITEM_ID
						+ " = ?",
					new String[] { id + "" },
					null);

			if (cursor.moveToFirst()) {
				alertItem = new AlertItemDO().populate(cursor);
			}
		}
		catch (RemoteException ignored) {}
		catch (IllegalArgumentException ignored) {}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return alertItem;	// null if not found
	}

	public static AlertItemDO get(String sUid, int accountId) {

		if (sUid == null) {
			return null;
		}

		String where = "";
		String[] args = null;

		if (accountId != -1) {
			where += AutomatonAlertProvider.ALERT_ITEM_ACCOUNT_ID
					+ " = ? AND "
					+ AutomatonAlertProvider.ALERT_ITEM_UID
					+ " = ? AND ";
			args = new String[] { accountId+"", sUid, Status.DONT_SHOW.name() };
		}
		else {
			args = new String[] { Status.DONT_SHOW.name() };
		}

		where += AutomatonAlertProvider.ALERT_ITEM_STATUS
					+ " <> ?";

		Cursor cursor = null;

		try {
			cursor = AutomatonAlert.getProvider().query(
					AutomatonAlertProvider.ALERT_ITEM_TABLE_URI,
					null,
					where,
					args,
					AutomatonAlertProvider.ALERT_ITEM_ACCOUNT_ID + " ASC");

			if (cursor.moveToFirst()) {
				return (new AlertItemDO().populate(cursor));
			}
		}
		catch (RemoteException ignored) {}
		catch (IllegalArgumentException ignored) {}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return null;
	}

	public static ArrayList<AlertItemDO> get(Status status, boolean not) {

		ArrayList<AlertItemDO> list = new ArrayList<AlertItemDO>();

		String[] args = null;

		String where =
				AutomatonAlertProvider.ALERT_ITEM_STATUS
						+ (not ?
								" <> ?"
								:
								" = ?");

		if (not) {
			where += " AND "
					+ AutomatonAlertProvider.ALERT_ITEM_STATUS
					+ " <> ?";
			args = new String[] { status.name(), Status.DONT_SHOW.name() };
		}
		else {
			args = new String[] { status.name() };
		}

		Cursor cursor = null;
		try {
			cursor = AutomatonAlert.getProvider().query(
					AutomatonAlertProvider.ALERT_ITEM_TABLE_URI,
					null,
					where,
					args,
					AutomatonAlertProvider.ALERT_ITEM_STATUS + " ASC");

			if (cursor.moveToFirst()) {
				do {
					list.add((new AlertItemDO()).populate(cursor));

				} while (cursor.moveToNext());
			}
		}
		catch (RemoteException ignored) {}
		catch (IllegalArgumentException ignored) {}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}


		return list;
	}

	public static ArrayList<AlertItemDO> getAlarms(boolean currentOnly) {

		ArrayList<AlertItemDO> list = new ArrayList<AlertItemDO>();

		String where =
				AutomatonAlertProvider.ALERT_ITEM_DATE_REMIND
						+ " > 0 "
						+ " AND "
						+ AutomatonAlertProvider.ALERT_ITEM_STATUS
						+ " <> ?"
						+ " AND "
						+ AutomatonAlertProvider.ALERT_ITEM_STATUS
						+ " <> ?";

		String[] args = new String[] { Status.TRASH.name(),	Status.DONT_SHOW.name() };


		Cursor cursor = null;
		try {
			cursor = AutomatonAlert.getProvider().query(
					AutomatonAlertProvider.ALERT_ITEM_TABLE_URI,
					null,
					where,
					args,
					null);

			if (cursor.moveToFirst()) {
				long now = System.currentTimeMillis();
				do {
					AlertItemDO alertItem = new AlertItemDO().populate(cursor);
					if (currentOnly) {
						long nextAlarm = alertItem.getNextIteratedAlarm();
						if (nextAlarm > now) {
							list.add(alertItem);
						}
					}
					else {
						list.add(alertItem);
					}

				} while (cursor.moveToNext());
			}
		}
		catch (RemoteException ignored) {}
		catch (IllegalArgumentException ignored) {}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}


		return list;
	}

	public static class DefaultComparator<T extends AlertItemDO> implements Comparator<T> {
		@Override
		public int compare(T itT, T i2T) {
			Long i1L = -1L;
			Long i2L = -1L;

			if (itT != null) {
				// DATE is a long stored as a string
				String i1s = itT.getKvRawDetails().get(AutomatonAlert.DATE);
				if (i1s != null) {
					i1L = Utils.getLong(i1s, i1L);
				}
			}

			if (i2T != null) {
				String i2s = i2T.getKvRawDetails().get(AutomatonAlert.DATE);
				if (i2s != null) {
					i2L = Utils.getLong(i2s, i2L);
				}
			}

			return i2L.compareTo(i1L);
		}
	}

	private static int listCompare(Long now, Long i1, Long i2) {
		//////////////////////////////////////
		// Future dates on top, ascending 	//
		// Past date on bottom, descending	//
		//////////////////////////////////////
		// == future/future or future/past
		if (i1 > now) {
			if (i2 > now) {
				// both are in the future, use ascending
				return i1.compareTo(i2);
			}
			else {
				// 1=future, 2=past
				// future needs to be on top
				return -1;
			}
		}
		// == past/past or past/future
		else {
			if (i2 < now) {
				// both are in the past, use descending
				return i2.compareTo(i1);
			}
			else {
				// 1=past, 2=future
				// future needs to be on top
				return 1;
			}
		}
	}

	public static class DateRemindComparator<T extends AlertItemDO> implements Comparator<T> {
		@Override
		public int compare(T i1, T i2) {
			// first: future dates, ascending
			// next/last: past dates, descending
			Date i1l = new Date(0);
			Date i2l = new Date(0);

			if (i1 != null) {
				if (i1.getDateRemind() != null) {
					i1l = i1.getDateRemind();
				}
			}
			if (i2 != null) {
				if (i2.getDateRemind() != null) {
					i2l = i2.getDateRemind();
				}
			}

			return listCompare(
					System.currentTimeMillis(), i1l.getTime(), i2l.getTime());
		}
	}

	public static class RepeatComparator<T extends AlertItemDO> implements Comparator<T> {
		@Override
		public int compare(T i1, T i2) {
			// first: future dates, ascending
			// next/last: past dates, descending
			Long i1l = -1L;
			Long i2l = -1L;

			if (i1 != null) {
				i1l = i1.getNextIteratedAlarm();
			}
			if (i2 != null) {
				i2l = i2.getNextIteratedAlarm();
			}

			return listCompare(System.currentTimeMillis(), i1l, i2l);
		}
	}

	public static class SnoozeComparator<T extends Pair<AlertItemDO, PostAlarmDO>>
			implements Comparator<T> {

		@Override
		public int compare(T i1, T i2) {
			// first: future dates, ascending
			// next/last: past dates, descending
			Long i1l = -1L;
			Long i2l = -1L;

			if (i1 != null) {
				if (i1.second != null) {
					i1l = i1.second.getNextAlarm();
				}
			}
			if (i2 != null) {
				if (i2.second != null) {
					i2l = i2.second.getNextAlarm();
				}
			}

			return listCompare(System.currentTimeMillis(), i1l, i2l);
		}
	}
}

