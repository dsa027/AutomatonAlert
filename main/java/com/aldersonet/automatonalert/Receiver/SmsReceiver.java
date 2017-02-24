package com.aldersonet.automatonalert.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.telephony.SmsMessage;
import android.util.Log;

import com.aldersonet.automatonalert.Account.Accounts;
import com.aldersonet.automatonalert.Activity.RTUpdateActivity.FragmentTypeRT;
import com.aldersonet.automatonalert.Alert.AlertItemDO;
import com.aldersonet.automatonalert.Alert.ContactAlert;
import com.aldersonet.automatonalert.Alert.NotificationItemDO;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Preferences.GeneralPrefsDO;
import com.aldersonet.automatonalert.SMS.AccountSmsDO;
import com.aldersonet.automatonalert.SoundBomb.SoundBomb;
import com.aldersonet.automatonalert.SoundBomb.SoundBombQ;
import com.aldersonet.automatonalert.Util.Utils;
import com.google.android.mms.pdu.GenericPdu;
import com.google.android.mms.pdu.NotificationInd;
import com.google.android.mms.pdu.PduHeaders;
import com.google.android.mms.pdu.PduParser;

import java.util.HashMap;
import java.util.HashSet;

public class SmsReceiver extends BroadcastReceiver {

	public static final String TAG = "SmsReceiver";

	static String mFrom = "";
	static String mSubject = "";
	static String mText = "";

	Context mContext;

	@Override
	public void onReceive(final Context context, final Intent intent) {
		if (!GeneralPrefsDO.isSystemOn()) {
			return;
		}

		mContext = context;

		String action = null;

		if (intent == null) {
			return;

		}
		action = intent.getAction();

		if (action == null) {
			return;
		}

		if (intent.getExtras() == null) {
			return;
		}

		if (action.equalsIgnoreCase(
				AutomatonAlert.ANDROID_PROVIDER_TELEPHONY_SMS_RECEIVED)) {
			processSms(intent);
		}

		else if (action.equalsIgnoreCase(
				AutomatonAlert.ANDROID_PROVIDER_TELEPHONY_WAP_PUSH_RECEIVED)) {
			processMms(intent);
		}
	}

	private void processMms(Intent intent) {
		String type = intent.getType();

		if (type != null) {
			if (type.equals(AutomatonAlert.MMS_MIME_TYPE)) {

				Bundle bundle = intent.getExtras();
				if (bundle != null) {

					byte[] pushData = bundle.getByteArray("data");
					mText = "";

					PduParser parser = new PduParser(pushData);
					GenericPdu pdu = null;
                    try {
                        pdu = parser.parse();
                    } catch (RuntimeException ignore) {}

					int iType = (pdu == null) ? -98478238 : pdu.getMessageType();

					if (pdu != null && iType == PduHeaders.MESSAGE_TYPE_NOTIFICATION_IND) {
						mFrom = "";
						mSubject = "";
						if (((NotificationInd)pdu).getFrom() != null) {
							mFrom = ((NotificationInd)pdu).getFrom().getString();
						}
						if (((NotificationInd)pdu).getSubject() != null) {
							mSubject = ((NotificationInd)pdu).getSubject().getString();
						}
						HashSet<HashMap<String, String>> messages =
								new HashSet<HashMap<String, String>>();
						HashMap<String, String> message = new HashMap<String, String>();

						message.put(ContactAlert.TAG_MESSAGE_SOURCE_HEADER, AccountSmsDO.MMS);
						message.put(AutomatonAlert.FROM, mFrom);
						message.put(AutomatonAlert.SUBJECT, mSubject);
						message.put(AutomatonAlert.DATE, "0");
						message.put(AutomatonAlert.SMS_BODY, mText);
						message.put(AutomatonAlert.UID, "" + 0);
						message.put(AutomatonAlert.ACCOUNT_KEY, AccountSmsDO.SMS_KEY);
						message.put(Contacts.DISPLAY_NAME, Utils.getContactNameFromNumber(mFrom));
						messages.add(message);

						// all this, up to here, is done to block sms/mms.
						// All other processing is done in MmsMonitor
						if (doAlerts(mContext, messages, AccountSmsDO.MMS)) {
							abortBroadcast();
						}
					}
				}
			}
		}
	}

	private void processSms(Intent intent) {

		HashSet<HashMap<String, String>> messages =
				new HashSet<HashMap<String, String>>();

		Object[] pdus = (Object[])(intent.getExtras()).get("pdus");

		if (pdus != null &&
				pdus.length > 0) {
			SmsMessage msg = null;
			HashMap<String, String> message = null;
//			String date = null;
			String subject = null;
			String body = null;
			String fromNumber = null;
			int msgNo = 0;
			// each SMS message
			for (Object pdu : pdus) {

				// package this SMS into a map and put into set to pass to search
				message = new HashMap<String, String>();
				msg = SmsMessage.createFromPdu((byte[])pdu);
				fromNumber = msg.getOriginatingAddress();
				subject = msg.getPseudoSubject();
				body = msg.getDisplayMessageBody();
				if (subject == null) {
					subject = "";
				}
				if (body == null) {
					body = "";
				}
                if (msg.getEmailFrom() != null) {
                    Log.d(TAG, "processSms(): fromEmail: " + msg.getEmailFrom());
                    fromNumber = msg.getEmailFrom();
                }
				// message into map, map into set for search below.
				// for DATE, use now instead of getTimestampMillis
				// because the latter gives incorrect datetime (2 hours off)
				// and because we know best when it came in.
				message.put(ContactAlert.TAG_MESSAGE_SOURCE_HEADER, AccountSmsDO.SMS);
				message.put(AutomatonAlert.FROM, fromNumber);
				message.put(AutomatonAlert.SUBJECT, subject);
				message.put(AutomatonAlert.DATE, System.currentTimeMillis()/*msg.getTimestampMillis()*/ + "");
				message.put(AutomatonAlert.SMS_BODY, body);
				message.put(AutomatonAlert.UID, "" + ++msgNo);
				message.put(AutomatonAlert.ACCOUNT_KEY, AccountSmsDO.SMS_KEY);
				message.put(Contacts.DISPLAY_NAME, Utils.getContactNameFromNumber(fromNumber));
				messages.add(message);
			}
		}

		// do alert
		if (messages.size() > 0) {
			if (doAlerts(mContext, messages, AccountSmsDO.SMS)) {
				abortBroadcast();
			}
		}

	}

	public synchronized static /*abort*/boolean doAlerts(
			Context context,
			HashSet<HashMap<String, String>> messages,
			String smsMms) {

		AccountSmsDO accountSms = (AccountSmsDO)Accounts.get(AccountSmsDO.SMS_KEY);

		// TODO: BLOCK SMS/MMS
//		if (blockContact(context, smsMms, accountSms, messages)) {
//			return true;
//		}

		if (smsMms.equals(AccountSmsDO.MMS)) {
			// only needed to check for a blocked contact and there isn't one.
            // mms messages are processed elsewhere
			return false/*abort*/;
		}

		// check for both contact and free-form matches
		int numAccountAlerts = 0;
		if (accountSms != null) {
			accountSms.findSearchStringAndAlert(context, messages);
			numAccountAlerts = accountSms.getNumberOfAlerts();
		}
		// if none, sound the default/global ringtone
		if (numAccountAlerts <= 0) {
			NotificationItemDO notificationItem = setDefaultNotificationItem();
			SoundBomb soundBomb = new SoundBomb(
					context,
					false,/*thisIsATest*/
					true,/*usePlayFor*/
					FragmentTypeRT.TEXT,
					-1, // alertItemId
					notificationItem,
					"");
			AutomatonAlert.getSoundBombs().add(soundBomb);
			SoundBombQ.doNotification(soundBomb, false/*isTest*/);
		}

		return false/*requested abort broadcast*/;
	}

	private boolean blockContact(
			Context context, String smsMms, AccountSmsDO accountSms,
			HashSet<HashMap<String, String>> messages) {

		String fromName = "";
		boolean createAlertItem = true;
		boolean doSoundBomb = true;
		/////////////
		//  mms only needs to look for an abortBroadcast.
		if (smsMms.equals(AccountSmsDO.MMS)) {
			createAlertItem = false;
			doSoundBomb = false;
		}
		// reads messages, creates contact recs, creates AlertItem's and does SoundBomb.
		// for mms, reads messages and creates contact recs
		ContactAlert contactAlert =
				ContactAlert.newInstance(
						context,
						FragmentTypeRT.TEXT,
						accountSms.getAccountId());
		contactAlert.processRingtonesInMessages(messages, createAlertItem, doSoundBomb);

		// check if blocked sms/mms
		// TODO: BLOCK SMS/MMS
//		for (ContactAlert.ContactRec rec : contactAlert.mContactRecs) {
//			fromName = rec.mContactName;
//			if (contactAlert.isContactBlockedSmsMms(rec.mLookupKey)) {
//				//////////////////////////////////////////
//				//////////////////////////////////////////
//				// user requested to block this contact //
//				//////////////////////////////////////////
//				//////////////////////////////////////////
//				abortBroadcast();
//				if (RTPrefsDO.getShowToastOnSmsMmsBlock().equals("1")) {
//					Toast.makeText(
//							AutomatonAlert.THIS,
//							"Message blocked from " + rec.mContactName,
//							Toast.LENGTH_SHORT)
//							.show();
//				}
//				return true/*abort*/;
//			}
//		}

		int numContactBombs = contactAlert.getNumberOfAlerts();

		return false;

	}

	private static NotificationItemDO setDefaultNotificationItem() {
		if (AlertItemDO.mDefaultNotificationItem == null) {
			AlertItemDO.createDefaultTextNotification();
		}
		return AlertItemDO.mDefaultNotificationItem;
	}
}
