/*
http://anddev.org
Chitra
Master Developer

Posts: 213
Joined: Mon Mar 01, 2010 7:59 am
Location: Now in Chennai
*/

package com.aldersonet.automatonalert.SMS;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.provider.ContactsContract.Contacts;
import android.text.TextUtils;
import android.util.Log;

import com.aldersonet.automatonalert.Alert.ContactAlert;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Receiver.SmsReceiver;
import com.aldersonet.automatonalert.Util.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;

public class MmsMonitor {

	public static final String TAG = "MmsMonitor";

	private ContentResolver mContentResolver = null;
	private Handler mMmshandler = null;
	private ContentObserver mMmsObserver = null;
	private boolean mMonitorStatus = false;
	private int mMmsCount = 0;
	private Context mContext;

	public MmsMonitor(final Context context) {
		mContext = context;
		mContentResolver = AutomatonAlert.THIS.getContentResolver();
		mMmshandler = new MMSHandler();
		mMmsObserver = new MMSObserver(mMmshandler);
	}

	public void startMMSMonitoring() {
		mMonitorStatus = false;
		if (!mMonitorStatus) {
			mContentResolver.registerContentObserver(
					Uri.parse("content://mms-sms/"), false, mMmsObserver);

			getLatestCount();
		}
		Log.d(TAG, ".startMMSMonitoring(): mMmsCount[" + mMmsCount + "]");
	}

	private void getLatestCount() {
		Cursor mmsCur = null;
		try {
			Uri uriMMSURI = Uri.parse("content://mms");
			mmsCur = mContentResolver.query(
					uriMMSURI,
					new String[] {"_id"},
					"msg_box = ?",
					new String[] {"1"},
					"_id");
			if (mmsCur != null && mmsCur.getCount() > 0) {
				mMmsCount = mmsCur.getCount();
			}
		}
		catch (Exception ignored) {
		}
		finally {
			if (mmsCur != null) {
				mmsCur.close();
			}
		}
	}

	public void stopMMSMonitoring() {
		try {
			mMonitorStatus = false;
			if (!mMonitorStatus){
				mContentResolver.unregisterContentObserver(mMmsObserver);
			}
		} catch (Exception ignored) {
		}
	}

	static class MMSHandler extends Handler {
		public void handleMessage(final Message msg) {
			if (msg != null) {

			}
		}
	}

	class MMSObserver extends ContentObserver {
		private Handler mMmsHandle = null;

		public MMSObserver(final Handler mmshandle) {
			super(mmshandle);
			mMmsHandle = mmshandle;
		}

		public void onChange(final boolean bSelfChange) {
			super.onChange(bSelfChange);
			if (bSelfChange) {
				return;
			}

			Thread thread = new Thread() {
				public void run() {
					processMms();
				}
			};
			thread.start();
		}

		private void processMms() {
			Cursor mmsCur = null;
			try {
				mMonitorStatus = true;

				// Send message to Activity
				Message msg = new Message();
				mMmsHandle.sendMessage(msg);

				// Getting the mms count
				Uri uriMMSURI = Uri.parse("content://mms/");
				mmsCur = mContentResolver.query(
						uriMMSURI,
						new String[] {"date", "sub", "_id", "m_type", "seen"},
						"msg_box = ?",
						new String[]{"1"},
						"_id");

				int currMMSCount = 0;
				if (mmsCur == null) {
					return;
				}
				if (mmsCur.getCount() > 0) {
					currMMSCount = mmsCur.getCount();
				}
				if (mMmsCount > currMMSCount) {
					mMmsCount = currMMSCount; // jic
					getLatestCount();
				}

				if (currMMSCount > mMmsCount) {
					mMmsCount = currMMSCount;

					mmsCur.moveToLast();

					long date = mmsCur.getInt(mmsCur.getColumnIndex("date"));
					date *= 1000;
					String subject = mmsCur.getString(mmsCur.getColumnIndex("sub"));
					int id = Integer.parseInt(mmsCur.getString(mmsCur.getColumnIndex("_id")));

					/////////
					// get the message and the address, do alert
					////////
					String message = getMessage(id, mmsCur);
					if (message == null) {
						return;
					}
					String address = getAddress(id);
					doAlert(message, address, subject, date);
				}

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (mmsCur != null) {
					mmsCur.close();
				}
			}
		}

		private void doAlert(String message, String address, String subject, long date) {
			// message into map, map into set for search below
			if (TextUtils.isEmpty(address)) {
				Log.d(TAG + ".doAlert()", "(unknown sender): address is blank");
				address = "(unknown sender)";
			}
			HashSet<HashMap<String, String>> mmsMessages = new HashSet<>();
			HashMap<String, String> mmsMessage = new HashMap<>();
			mmsMessage.put(ContactAlert.TAG_MESSAGE_SOURCE_HEADER, AccountSmsDO.MMS);
			mmsMessage.put(AutomatonAlert.FROM, address);
			mmsMessage.put(AutomatonAlert.SUBJECT, subject);
			mmsMessage.put(AutomatonAlert.DATE, date + "");
			mmsMessage.put(AutomatonAlert.SMS_BODY, message);
			mmsMessage.put(AutomatonAlert.UID, "1");
			mmsMessage.put(AutomatonAlert.ACCOUNT_KEY, AccountSmsDO.SMS_KEY);
			mmsMessage.put(
					Contacts.DISPLAY_NAME,
					Utils.getContactNameFromNumber(address));
			mmsMessages.add(mmsMessage);
			Log.d(TAG + ".doAlert()", "calling SmsReceiver.doAlerts()");
			SmsReceiver.doAlerts(mContext, mmsMessages, MMSMONITOR);
		}

		private String getMessage(int id, Cursor mmsCur) throws RemoteException {
			String message = "";
			Cursor curPart = null;

			//	m_type=128   = MESSAGE_TYPE_SEND_REQ
			//	m_type=130   = MESSAGE_TYPE_NOTIFICATION_IND
			//	m_type=132   = MESSAGE_TYPE_RETRIEVE_CONF
			//int type = Integer.parseInt(mmsCur.getString(12));
			int type = Integer.parseInt(mmsCur.getString(mmsCur.getColumnIndex("m_type")));
			int seen = Integer.parseInt(mmsCur.getString(mmsCur.getColumnIndex("seen")));
			// old one...
			if (seen != 0) {
				return null;
			}
			// anything but incoming
			if (type != 132) {
				return null;
			}

			Log.d(TAG + ".getMessage()", "getting message");

			// Get Parts
			Uri uriMMSPart = Uri.parse("content://mms/part");
			curPart = mContentResolver.query(
					uriMMSPart,
					new String[] {"ct", "_id", "text"},
					"mid = ?",
					new String[]{id + ""},
					"_id");
			if (curPart != null && curPart.moveToLast()) {
				do {
					String contentType = curPart.getString(curPart.getColumnIndex("ct"));
					String partId = curPart.getString(curPart.getColumnIndex("_id"));

					// Get the message
					if (contentType.toLowerCase().startsWith("text/")) {
						byte[] messageData = readMMSPart(partId);
						if (messageData != null && messageData.length > 0) {
							message += new String(messageData);
						}
						if (message.equals("")) {
							Cursor curPart1 = mContentResolver.query(
									uriMMSPart,
									null,
									"mid = ? AND _id = ?",
									new String[]{id + "", partId},
									"_id");
							if (curPart1 != null && curPart1.moveToLast()) {
								message += curPart1.getString(curPart1.getColumnIndex("text"));
								curPart1.close();
							}
						}
					}
				} while (curPart.moveToPrevious());
			}

			if (curPart != null) {
				curPart.close();
			}

			return message;
		}

		private String getAddress(int id) throws RemoteException {
			Log.d(TAG + ".getAddress()", "getting address");
			Cursor addrCur = null;
			String address = "";
			int addressCounter = 0;

			// sometimes we need to wait for the address
			while (address.isEmpty()
					&& ++addressCounter < 50) {
				Uri uriAddrPart = Uri.parse("content://mms/" + id + "/addr");
				// type=137="To:", type=151="From:", type=130=group from?
				addrCur = mContentResolver.query(
						uriAddrPart,
						new String[] {"address"},
						"type = 137",
						null,
						"_id");
				if (addrCur != null) {
					if (addrCur.moveToLast()) {
						do {
							int addColIndx = addrCur.getColumnIndex("address");
							if (addColIndx != -1) {
								address = addrCur.getString(addColIndx);
								Log.d(TAG + ".getAddress()", "got address[" + address + "]");
								break;
							}
						} while (addrCur.moveToPrevious());
					}
				}
				if (address.isEmpty()) {
					try {
						Thread.sleep(2); // don't thrash
					} catch (InterruptedException ignored) {
						//
					}
				}
			}

			if (addrCur != null) {
				addrCur.close();
			}

			return address;
		}
	}

	private byte[] readMMSPart(String partId) {
		byte[] partData = null;
		Uri partURI = Uri.parse("content://mms/part/" + partId);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		InputStream is = null;

		try {
			is = mContentResolver.openInputStream(partURI);
			if (is == null) {
				return null;
			}

			byte[] buffer = new byte[256];
			int len = is.read(buffer);
			while (len >= 0) {
				baos.write(buffer, 0, len);
				len = is.read(buffer);
			}
			partData = baos.toByteArray();

		} catch (Exception ignored) {
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (IOException ignored) {
				}
			}
		}
		return partData;
	}


	private boolean isImageType(String mime) {
		boolean result = false;
		if (mime.equalsIgnoreCase("image/jpg")
				|| mime.equalsIgnoreCase("image/jpeg")
				|| mime.equalsIgnoreCase("image/png")
				|| mime.equalsIgnoreCase("image/gif")
				|| mime.equalsIgnoreCase("image/bmp")) {
			result = true;
		}
		return result;
	}

	private static String MMSMONITOR = "MmsMonitor";

	public static boolean isMmsMonitor(String in) {
		return (in.equals(MMSMONITOR));
	}
}

