package com.aldersonet.automatonalert.Account;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;

import com.aldersonet.automatonalert.Activity.RTUpdateActivity.FragmentTypeRT;
import com.aldersonet.automatonalert.Alert.AlertItemDO;
import com.aldersonet.automatonalert.Alert.AlertItemDO.AlertItemType;
import com.aldersonet.automatonalert.Alert.ContactAlert;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Email.AccountEmailDO;
import com.aldersonet.automatonalert.Filter.FilterItemDO;
import com.aldersonet.automatonalert.Filter.FilterItems;
import com.aldersonet.automatonalert.Provider.AutomatonAlertProvider;
import com.aldersonet.automatonalert.SMS.AccountSmsDO;
import com.aldersonet.automatonalert.Util.EmailAddress;
import com.aldersonet.automatonalert.Util.SimpleExToRegEx;
import com.aldersonet.automatonalert.Util.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class AccountDO {

	public static final String TAG = "AccountDO";

	public static String EMAIL = "email";

	public static final int IMAP_SSL = 993;
//	public static final int POP_SSL = 995;

	public static final int ACCOUNT_GENERIC = 0;
	public int mAccountType = ACCOUNT_GENERIC;

	public static final String ACCOUNT_CHECK_NEVER = "Never/Manual Check";

	public int mAccountId;
	protected String mName = "";
	protected boolean mSaveToList;
	protected boolean mMarkAsSeen;
	protected Date mTimeStamp;
	protected int mNumberOfAlerts;

	public boolean mIsDirty = false;

	private volatile boolean mSearchMatchFoundOverall = false;
	private volatile boolean mSearchMatchFoundForThisMessage = false;
	private volatile boolean mHaveSearchDefaultThisMessage = false;
	private volatile boolean mContactMatchFoundForThisMessage = false;
	private volatile FilterItemDO mDefaultFilterItem = null;

	public AccountDO() {
		mAccountId = -1;
		mIsDirty = true;
		//davedel -- force keep AlertItem's in dev
//		if (BuildConfig.DEBUG) {
//			mSaveToList = true;
//		}
		//davedel
//		mSaveToList = false;
		mMarkAsSeen = false;
		mTimeStamp = new Date(System.currentTimeMillis());
		mNumberOfAlerts = 0;
	}

	public AccountDO(final String name) {
		this();
		mName = name;
	}

	public int getAccountId() {
		return mAccountId;
	}

	public boolean isDirty() {

		return mIsDirty;
	}

	public int getNumberOfAlerts() {
		return mNumberOfAlerts;
	}

	public static int getType(String key) {
		try {
			String sType = key.substring(key.indexOf("|") + 1);
			if (sType.equals(AccountSmsDO.SMS_NAME)) {
				return AccountSmsDO.ACCOUNT_SMS;
			}

			EmailAddress emailAddress = new EmailAddress(sType);
			if (emailAddress.isValid()) {
				return AccountEmailDO.ACCOUNT_EMAIL;
			}

		} catch (IndexOutOfBoundsException ignored) {}

		return AccountDO.ACCOUNT_GENERIC;

	}

	public enum AccountSecurity {
		SSL, SSL_TLS, NONE
	}

	public enum AccountType {
		IMAP, POP3, EXCHANGE, SMS
	}

	public boolean isComplete() {
		return !(TextUtils.isEmpty(mName));
	}

	public String getName() {
		if (mName == null) {
			return "";
		}
		return mName;
	}

	public String getKey() {
		return mName;
	}

	public String setName(final String name) {
		if (!(mName.equals(name))) {
			mIsDirty = true;
		}
		mName = name;
		return name;
	}

	public boolean isSaveToList() {
		return mSaveToList;
	}

	public void setSaveToList(boolean saveToList) {
		if (mSaveToList != saveToList) {
			mIsDirty = true;
		}
		mSaveToList = saveToList;
	}

	public boolean isMarkAsSeen() {
		return mMarkAsSeen;
	}

	public void setMarkAsSeen(boolean markAsSeen) {
		if (mMarkAsSeen != markAsSeen) {
			mIsDirty = true;
		}
		mMarkAsSeen = markAsSeen;
	}

	public Date getTimeStamp() {
		return mTimeStamp;
	}

	public synchronized void delete() {
		try {

			Uri uri = ContentUris.withAppendedId(
					AutomatonAlertProvider.ACCOUNT_ID_URI, mAccountId);
			AutomatonAlert.getProvider().delete(uri, null, null);

		} catch (RemoteException e) {
			Log.e(TAG + ".delete()", "Account.delete() delete exception: "
					+ e.toString());
		}

//		Accounts.mCache.removeFromCache(getAccountId());
	}

	public void saveToDB(ContentValues cv) {

		AutomatonAlertProvider aap =
				(AutomatonAlertProvider)AutomatonAlert.getProvider()
				.getLocalContentProvider();

        int id = aap == null ? -1 :
                aap.insertOrUpdate(
				cv,
				mAccountId,
				AutomatonAlertProvider.ACCOUNT_ID_URI,
				AutomatonAlertProvider.ACCOUNT_TABLE_URI);

		// if inserted, store new id and spread out to FilterItemDO.AccountId's
		if (id != -1
				&& id != mAccountId) {
			mAccountId = id;
			ArrayList<FilterItemDO> filterItems = FilterItems.getAccountId(mAccountId);
			for (FilterItemDO filterItem : filterItems) {
				filterItem.addToAccounts(mAccountId);
			}
		}
	}

	public  AccountDO populate(Cursor cursor) {

		mIsDirty = false;

		// populate fields
		mAccountId = cursor.getInt(
				cursor.getColumnIndex(AutomatonAlertProvider.ACCOUNT_ID));
		mAccountType = ACCOUNT_GENERIC;

		setName(cursor.getString(cursor.getColumnIndex(
				AutomatonAlertProvider.ACCOUNT_NAME)));

		setSaveToList(cursor.getString(cursor.getColumnIndex(
				AutomatonAlertProvider.ACCOUNT_SAVE_TO_LIST))
				.equalsIgnoreCase(AutomatonAlert.TRUE));

		setMarkAsSeen(cursor.getString(cursor.getColumnIndex(
				AutomatonAlertProvider.ACCOUNT_MARK_AS_SEEN))
				.equalsIgnoreCase(AutomatonAlert.TRUE));

		long millis = cursor.getLong(
				cursor.getColumnIndex(AutomatonAlertProvider.ACCOUNT_TIMESTAMP));
		if (millis != -1) {
			mTimeStamp = new Date(millis);
		}
		else {
			mTimeStamp = new Date(System.currentTimeMillis());
		}

		//davedel -- force keep AlertItem's in dev
		mSaveToList = true;
		//davedel

		return this;
	}

	public synchronized void save() {

		// save account data
		ContentValues cv = AutomatonAlertProvider.getAccountContentValues(
				mName,
				null,
				null,
				null,
				null,
				993,
				-1,
				null,
				-1,
				mAccountType,
				Boolean.toString(mSaveToList),
				Boolean.toString(mMarkAsSeen),
				-1
				);

		saveToDB(cv);

		mIsDirty = false;

//		Accounts.mCache.replaceFromCache(this);
	}

	private void addToMessage(
			String fieldName, String fieldValue, HashMap<String, String> message) {
		message.put(fieldName, fieldValue);
	}

	private int getLatestUid(String fieldValue, int latestUid) {
		return Math.max(Utils.getInt(fieldValue, latestUid), latestUid);
	}

	private void setAndSoundAlert(
			HashMap<String, String> rawMessage,
			HashMap<String, String> message,
			HashSet<FilterItemDO> filterMatches) {

		AlertItemDO alertItem = AlertItemDO.createSaveNewAlertItem(
				mAccountId,
				message,
				rawMessage,
				AlertItemType.SEARCH,
				mSaveToList,
				mMarkAsSeen);

		soundTheAlert(alertItem, filterMatches);
		// don't keep <<default>> or created default
		// (this only deletes from db, the object
		// hangs around for soundTheAlert() to work
//		if (mUsingSearchDefaultThisMessage) {
//			alertItem.delete();
//		}
		mNumberOfAlerts++;
	}

	private void doPatternMatching(
			ArrayList<FilterItemDO> filterItemList,
			HashSet<FilterItemDO> filterMatches,
			String fieldValue) {

		if (filterItemList == null
				|| filterItemList.size() <= 0) {
			return;
		}
		if (TextUtils.isEmpty(fieldValue)) {
			return;
		}

		Pattern p;
		Matcher m;
		////////////////////////////////
		// EACH FILTER ITEM IN ACCOUNT
		////////////////////////////////
		for (FilterItemDO filterItem : filterItemList) {
			Log.d(TAG, ".doPatternMatching(): " +
					"Looking for " + fieldValue + " in [" + filterItem.getPhrase() + "]");

			// if there's no phrase in the filterItem, move on
			if (TextUtils.isEmpty(filterItem.getPhrase())) {
				continue;
			}
			// change regex to our own simple regex
			String sTranslated =
					SimpleExToRegEx.simpleExToRegEx(filterItem.getPhrase());
			try {
				p = Pattern.compile(sTranslated, Pattern.CASE_INSENSITIVE);
			} catch (PatternSyntaxException e) {
				continue;
			}
			// see if there are matches
			m = p.matcher(fieldValue);

			// highlight found field, then replace field in message map
			if (m.find()) {
				Log.d(TAG, ".doPatternMatching(): " +
						"found " + fieldValue + " in [" + sTranslated + "]");

				String sFound = "";
				if ((sFound = Utils.markUpFoundString(m)) != null) {
					fieldValue = sFound;
				}
				mSearchMatchFoundForThisMessage = true;
				// see if it's already in the list
				if (!(FilterItems.has(filterMatches, filterItem))) {
					filterMatches.add(filterItem);
				}
			}
		}
	}

	public synchronized int findSearchStringAndAlert(
			Context context, HashSet<HashMap<String, String>> latestMessagesSet) {

		int latestUid = -1;
		mNumberOfAlerts = 0;
		mSearchMatchFoundOverall = false;

		// for each message in set of latest messages
		// see if the user-specified search string(s) match
		// the user-specified header field
		/////////////////
		// EACH MESSAGE
		/////////////////
		for (final HashMap<String, String> message : latestMessagesSet) {
            Log.d(TAG, "findSearchStringAndAlert(): @message: " + message);

			HashSet<FilterItemDO> filterItemMatches =	new HashSet<>();

			mContactMatchFoundForThisMessage = false;
			mSearchMatchFoundForThisMessage = false;
			mHaveSearchDefaultThisMessage = false;
			mDefaultFilterItem = null;

			//////////////////////////
			// EACH HEADER IN MESSAGE
			//////////////////////////
			for (final HashMap.Entry<String, String> field : message.entrySet()) {
				String fieldName = field.getKey();
				String fieldValue = field.getValue();

				Log.d(TAG + ".findSearchStringAndAlert(" + mName + ")",
						"examining header: " + fieldName + "[" + fieldValue + "]");

				// capture the latest email message UID to be processed
				if (fieldName.equalsIgnoreCase(AutomatonAlert.UID)) {
					latestUid = getLatestUid(fieldValue, latestUid);
				}

				// CONTACT MATCH ON FROM: //
				else if (fieldName.equalsIgnoreCase(AutomatonAlert.FROM)) {
                    Log.d(TAG, "findSearchStringAndAlert(): checking contact");
					checkContactAndAlert(message);
				}

				// convert to long //
				else if (fieldName.equalsIgnoreCase(AutomatonAlert.DATE)) {
					long date = Utils.stringDateToLong(fieldValue);
					if (date > 0) {
						fieldValue = Utils.stringDateToLong(fieldValue) + "";
					}
				}

				// CHECK FILTER ITEMS NEXT //
				ArrayList<FilterItemDO> filterItemList =
						FilterItems.getFiltersWithFieldName(fieldName, mAccountId);

				// if there are no filter item matches,
				// just pass field along and continue
				if (filterItemList.size() <= 0) {
					addToMessage(fieldName, fieldValue, message);
					continue;
				}

				// PATTERN MATCH //
				doPatternMatching(filterItemList, filterItemMatches, fieldValue);

				// now we can mark up the search field and pass it along
				addToMessage(fieldName, fieldValue, message);
			}

			doAlertForMatchingMessages(message, filterItemMatches);

		} // each message

		if (mSearchMatchFoundOverall) {
			if (!AutomatonAlert.RTOnly) {
				Utils.reShowListActivityIfOnTop(context);
			}
		}

		return latestUid;
	}

	/* check for contact match */
	private void checkContactAndAlert(HashMap<String, String> message) {
        Log.d(TAG, "checkContactAndAlert(): @message: " + message);

		// default to EMAIL
		FragmentTypeRT type = FragmentTypeRT.EMAIL;
		String sType = EMAIL;
		// is SMS
		if (this.getKey().equals(AccountSmsDO.SMS_KEY)) {
			type = FragmentTypeRT.TEXT;
			sType = AccountSmsDO.SMS;
		}
		// this class looks for alerts in message
		ContactAlert contactAlert = ContactAlert.newInstance(
				AutomatonAlert.THIS, type, mAccountId);

		// copy of message so that we don't change message.
		// HashSet for processRingtonesInMessages()
		HashMap<String, String> holdMessage = new HashMap<>(message);
		holdMessage.put(ContactAlert.TAG_MESSAGE_SOURCE_HEADER, sType);
		HashSet<HashMap<String, String>> messagesSet = new HashSet<>();
		messagesSet.add(holdMessage);

		// see if there's a match and sound alert if there is
		contactAlert.processRingtonesInMessages(
				messagesSet, true/*createAlertItem*/, true/*doSoundBomb*/);

		// keep track of number of alerts
		int numberOfAlerts = contactAlert.getNumberOfAlerts();
		if (numberOfAlerts > 0) {
			mContactMatchFoundForThisMessage = true;
			mNumberOfAlerts += numberOfAlerts;
		}
	}

	private void doAlertForMatchingMessages(
			HashMap<String, String> message, HashSet<FilterItemDO> filterItemMatches) {

		HashMap<String, String> rawMessage = new HashMap<String, String>(message);

		// add as new alert item or get it if it's an AlertItemDO
		if (mSearchMatchFoundForThisMessage) {
			mSearchMatchFoundOverall = true;
			String type = EMAIL;
			if (mAccountType == AccountSmsDO.ACCOUNT_SMS) {
				type = AccountSmsDO.SMS;
			}
			rawMessage.put(ContactAlert.TAG_MESSAGE_SOURCE_HEADER, type);
			setAndSoundAlert(rawMessage, message, filterItemMatches);
		}
	}

	private FragmentTypeRT getFragmentType() {
		int type = getType(getKey());
		switch(type) {
			case AccountEmailDO.ACCOUNT_EMAIL:
				return FragmentTypeRT.EMAIL;
			case AccountSmsDO.ACCOUNT_SMS:
				return FragmentTypeRT.TEXT;
			default:
				return null;
		}
	}

	private void soundTheAlert(AlertItemDO alertItem, HashSet<FilterItemDO> filterMatches) {
		for (FilterItemDO filterItem : filterMatches) {
			if (filterItem.getNotificationItemId() >= 0) {
				Intent intent =
						AlertItemDO.setAlarmAlertIntent(
								alertItem.getAlertItemId(),
								filterItem.getNotificationItemId(),
								getFragmentType(),
								-1L, -1L, -1L);
				AutomatonAlert.THIS.sendBroadcast(intent);
			}
		}
	}

//	@Override
//	public Object getId() {
//		return getAccountId();
//	}

}
