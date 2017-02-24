package com.aldersonet.automatonalert.Alert;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.PhoneLookup;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.util.Log;

import com.aldersonet.automatonalert.Account.AccountDO;
import com.aldersonet.automatonalert.Activity.RTUpdateActivity.FragmentTypeRT;
import com.aldersonet.automatonalert.Alert.AlertItemDO.AlertItemType;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.BuildConfig;
import com.aldersonet.automatonalert.SourceAccount.SourceAccountDO;
import com.aldersonet.automatonalert.SourceType.SourceTypeDO;
import com.aldersonet.automatonalert.Util.EmailAddress;
import com.aldersonet.automatonalert.Util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class ContactAlert {

	public static final String TAG_MESSAGE_SOURCE_HEADER = "messageSourceHeader";
    public static final String TAG = "ContactAlert";

	private Context mContext;
	private FragmentTypeRT mType;
	private int mNumberOfAlerts;
	private int mAccountId;
	private AlertItemDO mAlertItem;

	private HashSet<ContactRec> mContactRecs;

	public static ContactAlert newInstance(
			Context context,
			FragmentTypeRT type,
			int accountId) {

		return new ContactAlert(context, type, accountId);
	}

	private ContactAlert(
			Context context,
			FragmentTypeRT type,
			int accountId) {
		super();
		mContext = context;
		mType = type;
		mContactRecs = new HashSet<>();
		mAccountId = accountId;
		mNumberOfAlerts = 0;
	}

	public int getNumberOfAlerts() {
		return mNumberOfAlerts;
	}

	public boolean isContactBlockedSmsMms(String lookupKey) {
		for (ContactRec rec : mContactRecs) {
			if (rec.mLookupKey.equals(lookupKey)) {
				return rec.mContactBlockedSmsMms;
			}
		}
		return false;
	}

	private void createAlertItem(HashMap<String, String> message, ContactRec rec) {
		// for joined contacts, need to get top-level lookupKey
//		Pair<String, String> keyName =
//				Utils.getEndPointLookupKeyDisplayName(mContext, rec.mLookupKey);
//		if (!(keyName.first.equals(rec.mLookupKey))) {
//			rec.mLookupKey = keyName.first;
//		}
		// add lookupKey to message
		message.put(Contacts.LOOKUP_KEY, rec.mLookupKey);
		mAlertItem = AlertItemDO.createSaveNewAlertItem(
				mAccountId, message, message, AlertItemType.CONTACT, true, false);
	}

	// returns true if user requested sms/mms block
	public void processRingtonesInMessages(
			HashSet<HashMap<String, String>> messages,
			boolean createAlertItem,
			boolean doSoundBomb) {
        Log.d(TAG, "processRingtonesInMessages(): messages: " + messages);

		NotificationItemDO notificationItem = null;
		mNumberOfAlerts = 0;

		// for each message
		for (HashMap<String, String> message : messages) {
			// use phone number
			String type = message.get(TAG_MESSAGE_SOURCE_HEADER);
			String searchString = message.get(AutomatonAlert.FROM);
            Log.d(TAG, "processRingtonesInMessages(): type: " + type + ", Search String: " + searchString);

			if (searchString == null) {
				continue;
			}
			if (type.equals(AccountDO.EMAIL) ||
					new EmailAddress(searchString).isValid()) {
                Log.d(TAG, "processRingtonesInMessages(): -->findContactByEmail()");
				findContactsByEmail(searchString);
			}
			else {
                Log.d(TAG, "processRingtonesInMessages(): -->findContactByNumber()");
				findContactsByNumber(searchString);
			}
			// for each contact with FROM: phone number or email
			for (ContactRec rec : mContactRecs) {
                Log.d(TAG, "processRingtonesInMessages(): for " + rec.mContactName);
				// see if user requested block on this contact
				notificationItem =
						NotificationItems.get(rec.mNotificationItemId);
				if (notificationItem != null) {
					if (notificationItem.getSoundPath()
							.equalsIgnoreCase(AutomatonAlert.BLOCK_SMS_MMS_LABEL)) {
						rec.mContactBlockedSmsMms = true;
						continue;
					}

					// if coming here from MMS observer, createAlertItem
					if (createAlertItem) {
						createAlertItem(message, rec);
					}

					// if coming here from SmsReceiver, do alert
					int alertItemId =
							mAlertItem == null ? -1 : mAlertItem.getAlertItemId();
					if (doSoundBomb) {

                        //davedel
                        if (BuildConfig.DEBUG) {
                            debugLog(rec);
                        }
                        //davedel

						Intent intent =
								AlertItemDO.setAlarmAlertIntent(
										alertItemId,
										rec.mNotificationItemId,
										mType,
										-1, -1, -1);
						mContext.sendBroadcast(intent);
					}
                    ++mNumberOfAlerts;
				}
			}
            if (mNumberOfAlerts <= 0 && BuildConfig.DEBUG) {
                if (message.get(TAG_MESSAGE_SOURCE_HEADER).equals("sms")) {
                    ContactRec rec = new ContactRec(
                            "LookupKey",
                            -98765,
                            message.get(Contacts.DISPLAY_NAME) + "/" +
                                    message.get(AutomatonAlert.FROM) + "/" +
                                    message.get(AutomatonAlert.SMS_BODY));
                    debugLog(rec);
                }
            }
        }
	}

    //davedel
    private static void debugLog(final ContactRec rec) {
        NotificationItemDO ni = NotificationItems.get(rec.mNotificationItemId);
        if (ni == null) {
            ni = new NotificationItemDO();
            ni.setNotificationItemId(-98765);
            ni.setSoundPath("no contact for sms");
        }
        String s = rec.mContactName +
                "|" + Utils.getSongName(Uri.parse(ni.getSoundPath())) +
                "|" + ni.getSoundPath();
        Utils.writeToDebugLog(s);
        Log.d(TAG, "processRingtonesInMessages(): " + s);
    }
    //davedel

    private boolean addContactRecForTEXT(String lookupKey, String displayName) {
        Log.d(TAG, "addContactRecForTEXT("+displayName+","+lookupKey+"): type: " + mType);
		SourceTypeDO sourceType = SourceTypeDO.get(lookupKey, mType.name());
		// make sure we have a sourceType and notificationItem
		if (sourceType != null) {
			int notificationItemId = sourceType.getNotificationItemId();
			if (notificationItemId >= 0) {
				ContactRec contactRec =
						new ContactRec(lookupKey, notificationItemId, displayName);
                Log.d(TAG, "addContactRecForTEXT(): added ContactRec. NId["+notificationItemId+"]");
				return contactRec.addContactRec();
			}
		}
        Log.d(TAG, "addContactRecForTEXT(): no SourceType or no notificationItem in SourceType");

		return false;
	}

	private boolean addContactRecForEMAIL(String lKey, String name, String mime) {
		if (mime.equalsIgnoreCase(Email.CONTENT_ITEM_TYPE)) {
			// get the EMAIL source type with this lookup_key
			SourceTypeDO sourceType =
					SourceTypeDO.get(lKey, FragmentTypeRT.EMAIL.name());
			if (sourceType == null) {
				return false;
			}
			if (sourceType.getNotificationItemId() < 0) {
				return false;
			}
			// get the accounts associated with this SourceTypeDO rec
			// and verify that the accountId is an account the user wants
			// email alerts on
			ArrayList<SourceAccountDO> sourceAccounts =
					SourceAccountDO.getSourceTypeId(sourceType.getSourceTypeId());
			for (SourceAccountDO sourceAccount : sourceAccounts) {
				// is it valid for this account?
				if (sourceAccount.getAccountId() != this.mAccountId) {
					continue;
				}
				// found it!
				ContactRec contactRec =
						new ContactRec(lKey, sourceType.getNotificationItemId(), name);
				return contactRec.addContactRec();
			}
		}

		return false;
	}

	private HashSet<ContactRec> findContactsByNumber(String number) {
		Uri phoneUri = Uri.withAppendedPath(
				PhoneLookup.CONTENT_FILTER_URI,
				Uri.encode(number));

		Cursor cursor = null;
        List<Pair<String, String>> keys = null;

		try {
            cursor = AutomatonAlert.THIS.getContentResolver().query(
                    phoneUri,
                    new String[] { Contacts.LOOKUP_KEY, Contacts.DISPLAY_NAME },
                    null,
                    null,
                    null);

            if (cursor != null && cursor.moveToFirst()) {
				do {
					String lookupKey = cursor.getString(
							cursor.getColumnIndex(Contacts.LOOKUP_KEY));
					String displayName = cursor.getString(
							cursor.getColumnIndex(Contacts.DISPLAY_NAME));

                    // add recs to map and when done fetching, to ContactRec
                    if (keys == null) keys = new ArrayList<>();
                    keys.add(new Pair<>(lookupKey, displayName));

				} while (cursor.moveToNext());
			}
		}
		catch (IllegalArgumentException ignored) {}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}

        // add db recs to ContactRec
        if (keys != null) {
            for(Pair<String, String> key : keys) {
                addContactRecForTEXT(key.first, key.second);
            }
        }

		return mContactRecs;
	}

	private HashSet<ContactRec> findContactsByEmail(String nameAndEmail) {

		// extract email address from contact string
		// and leave if contact is empty
		String contactEmail = Utils.extractEmailFromAddress(nameAndEmail);
		if (TextUtils.isEmpty(contactEmail)) {
			return null;
		}

		// lookup contact in contact database to get email address and lookup_key
		// then get SourceTypeDO with lookup_key and fragment type EMAIL.
		// then get SourceAccountDO to validate that this.mAccountId matches
		Cursor cursor = null;
        List<Pair<String, Pair<String, String>>> emailKeys = null;
        List<Pair<String, String>> textKeys = null;

		Uri uri = Uri.withAppendedPath(
				Email.CONTENT_LOOKUP_URI, Uri.encode(contactEmail));

		String[] projection = {
				Email.CONTACT_ID,
				Contacts.LOOKUP_KEY,
				Email.MIMETYPE,
				Contacts.DISPLAY_NAME
			};

		try {
            cursor = AutomatonAlert.THIS.getContentResolver().query(
                    uri, projection, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
				int lKeyIdx = cursor.getColumnIndex(Contacts.LOOKUP_KEY);
				int mimeIdx = cursor.getColumnIndex(Email.MIMETYPE);
				int nameIdx = cursor.getColumnIndex(Contacts.DISPLAY_NAME);
				do {
					String lKey = cursor.getString(lKeyIdx);
					String mime = cursor.getString(mimeIdx);
					String name = cursor.getString(nameIdx);

                    // save them to add to ContactRec later
					if (mType.equals(FragmentTypeRT.TEXT)) {
                        if (textKeys == null) textKeys = new ArrayList<>();
                        textKeys.add(new Pair<>(lKey, name));
					}
					else {
                        if (emailKeys == null) emailKeys = new ArrayList<>();
                        emailKeys.add(new Pair<>(lKey, new Pair<>(name, mime)));
					}

				} while (cursor.moveToNext());
			}
		}
		catch (IllegalArgumentException ignored) {}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}

        // add db recs to ContactRec
        if (textKeys != null) {
            for (Pair<String, String> key : textKeys) {
                addContactRecForTEXT(key.first, key.second);
            }
        }
        if (emailKeys != null) {
            for (Pair<String, Pair<String, String>> key : emailKeys) {
                addContactRecForEMAIL(key.first, key.second.first, key.second.second);
            }
        }

		return mContactRecs;
	}

	public class ContactRec {
		public String mLookupKey;
		public int mNotificationItemId;
		String mContactName;
		boolean mContactBlockedSmsMms;

		public ContactRec(
				String lookupKey, int notificationItemId, String contactName) {
			mLookupKey = lookupKey;
			mNotificationItemId = notificationItemId;
			mContactName = contactName;
			mContactBlockedSmsMms = false;
		}

		private boolean equals(ContactRec contactRec) {
			return mLookupKey.equals(contactRec.mLookupKey)
					&& mNotificationItemId == contactRec.mNotificationItemId;
		}

		private boolean isDuplicate() {
			for (ContactRec cr : mContactRecs) {
				if (equals(cr)) {
					return true;
				}
			}
			return false;
		}

		boolean addContactRec() {
			if (!isDuplicate()) {
				mContactRecs.add(this);
				return true;
			}
			return false;
		}
	}
}
