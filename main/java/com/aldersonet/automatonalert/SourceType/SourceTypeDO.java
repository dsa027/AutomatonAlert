package com.aldersonet.automatonalert.SourceType;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import com.aldersonet.automatonalert.ActionMode.ContactListInfo;
import com.aldersonet.automatonalert.Activity.IActivityRefresh;
import com.aldersonet.automatonalert.Activity.RTUpdateActivity.FragmentTypeRT;
import com.aldersonet.automatonalert.Alert.NotificationItemDO;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.ContactInfo.ContactInfoDO;
import com.aldersonet.automatonalert.ContactInfo.ICIListGetter;
import com.aldersonet.automatonalert.Fragment.ContactActiveFragment;
import com.aldersonet.automatonalert.Fragment.ILookupKeyMerger;
import com.aldersonet.automatonalert.Provider.AutomatonAlertProvider;
import com.aldersonet.automatonalert.SourceAccount.SourceAccountDO;
import com.aldersonet.automatonalert.Util.Enums;
import com.aldersonet.automatonalert.Util.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

public class SourceTypeDO
		implements
		ILookupKeyMerger {

	public static final String TAG = "SourceTypeDO";

	private int mSourceTypeId = -1;
	private String mLookupKey;
	private int mNotificationItemId;
	private String mSourceType;
	private Date mTimeStamp;

	public boolean isDirty;

	public SourceTypeDO() {
		super();

		mSourceTypeId = -1;
		mLookupKey = "";
		mNotificationItemId = -1;
		mSourceType = FragmentTypeRT.PHONE.name();

		isDirty = true;

	}

	public SourceTypeDO(String lookupKey, String sourceType) {
		this();
		mLookupKey = lookupKey;
		mSourceType = sourceType;
	}

	public SourceTypeDO(SourceTypeDO copyThis) {
		mLookupKey = copyThis.getLookupKey();
		mNotificationItemId = copyThis.getNotificationItemId();
		mSourceType = copyThis.getSourceType();
	}

	public int getSourceTypeId() {
		return mSourceTypeId;
	}

	public String getLookupKey() {
		return mLookupKey;
	}

	private String setLookupKey(String lookupKey) {
		if (!mLookupKey.equals(lookupKey)) {
			isDirty = true;
		}
		return mLookupKey = lookupKey;
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

	public String getSourceType() {
		return mSourceType;
	}

	public String setSourceType(String sourceType) {
		if (!mSourceType.equals(sourceType)) {
			isDirty = true;
		}
		return mSourceType = sourceType;
	}

	public static SourceTypeDO get(int id) {

		Uri uri = ContentUris.withAppendedId(
				AutomatonAlertProvider.SOURCE_TYPE_ID_URI, id);

		SourceTypeDO sourceType = null;

		Cursor cursor = null;
		try {
			cursor = AutomatonAlert.getProvider().query(
					uri, null, null, null, null);
			if (cursor != null && cursor.moveToFirst()) {
				sourceType = new SourceTypeDO().populate(cursor);
			}
		}
		catch (RemoteException | IllegalArgumentException ignored) {}

        finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return sourceType;	// null if not found
	}

	public static ArrayList<SourceTypeDO> get(FragmentTypeRT type) {
		String sType = type.name();
		ArrayList<SourceTypeDO> list = new ArrayList<>();

        Cursor cursor = null;
        try {
            cursor = AutomatonAlert.getProvider().query(
                    AutomatonAlertProvider.SOURCE_TYPE_TABLE_URI,
                    null,
                    AutomatonAlertProvider.SOURCE_TYPE_SOURCE_TYPE + " = ?",
                    new String[] { sType },
                    null);
        } catch (RemoteException e) {
            cursor = null;
            e.printStackTrace();
        }
        try {
			if (cursor != null && cursor.moveToFirst()) {
				do {
					SourceTypeDO sourceType = new SourceTypeDO();
					sourceType.populate(cursor);
					if (Utils.isActiveSourceType(sourceType)) {
						list.add(sourceType);
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

		return list;
	}



	public static SourceTypeDO get(String lookupKey, String type) {
        Log.d(TAG, "get("+type+","+lookupKey+")");

        SourceTypeDO sourceType = null;
        Cursor cursor = null;

        try {
            cursor = AutomatonAlert.getProvider().query(
                    AutomatonAlertProvider.SOURCE_TYPE_TABLE_URI,
                    null,
                    AutomatonAlertProvider.SOURCE_TYPE_LOOKUP_KEY
                            + " = ? AND "
                            + AutomatonAlertProvider.SOURCE_TYPE_SOURCE_TYPE
                            + " = ?",
                    new String[] { lookupKey, type },
                    null);
        } catch (RemoteException e) {
            cursor = null;
            e.printStackTrace();
        }
        try {
            if (cursor != null && cursor.moveToFirst()) {
                Log.d(TAG, "get() found.");
                sourceType = new SourceTypeDO().populate(cursor);
            }
        }
        catch (IllegalArgumentException e) {
            Log.d(TAG, "get(): IllegalArgumentException");
            e.printStackTrace();
        }
        finally {
            if (cursor != null) cursor.close();
        }

        return sourceType;	// null if not found
    }

	public static SourceTypeDO getNotificationItemId(int id) {
		Uri uri = AutomatonAlertProvider.SOURCE_TYPE_TABLE_URI;

		SourceTypeDO sourceType = null;

		String selection =
				AutomatonAlertProvider.SOURCE_TYPE_NOTIFICATION_ITEM_ID
				+ " = ?";
		String[] selectionArgs = { id + "" };

        Cursor cursor = null;
        try {
            cursor = AutomatonAlert.getProvider().query(
                    uri,
                    null,
                    selection,
                    selectionArgs,
                    null);
        } catch (RemoteException e) {
            cursor = null;
            e.printStackTrace();
        }
        try {
			if (cursor != null && cursor.moveToFirst()) {
				sourceType = new SourceTypeDO().populate(cursor);
			}
		}
		catch (IllegalArgumentException ignored) {}

		finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return sourceType;	// null if not found
	}

	public static List<SourceTypeDO> get(String lookupKey) {
		ArrayList<SourceTypeDO> sourceTypes = new ArrayList<>();

		Uri uri = AutomatonAlertProvider.SOURCE_TYPE_TABLE_URI;
		String selection =
				AutomatonAlertProvider.SOURCE_TYPE_LOOKUP_KEY
						+ " = ?";
		String[] selectionArgs = { lookupKey };

        Cursor cursor = null;
        try {
            cursor = AutomatonAlert.getProvider().query(
                    uri,
                    null,
                    selection,
                    selectionArgs,
                    null);
        } catch (RemoteException e) {
            cursor = null;
            e.printStackTrace();
        }
        try {
			if (cursor != null && cursor.moveToFirst()) {
				do {
					SourceTypeDO sourceType = new SourceTypeDO().populate(cursor);
					sourceTypes.add(sourceType);
				} while (cursor.moveToNext());
			}
		}
		catch (IllegalArgumentException ignored) {}

		finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return sourceTypes;		// empty if not found
	}

	public static ArrayList<SourceTypeDO> get() {
		Uri uri = AutomatonAlertProvider.SOURCE_TYPE_TABLE_URI;

		ArrayList<SourceTypeDO> sourceTypes = new ArrayList<>();

		Cursor cursor = null;

		try {
            try {
                cursor = AutomatonAlert.getProvider().query(
                        uri,
                        null,
                        null,
                        null,
                        null);
            } catch (RemoteException e) {
                cursor = null;
                e.printStackTrace();
            }
            if (cursor != null && cursor.moveToFirst()) {
				do {
					SourceTypeDO sourceType = new SourceTypeDO().populate(cursor);
					sourceTypes.add(sourceType);
				} while (cursor.moveToNext());
			}
		}
		catch (IllegalArgumentException | SQLiteException ignored) {}

        finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return sourceTypes;
	}

	//////////////////////////
	// ILookupKeyBridge
	//////////////////////////
    //davedel
    // doesn't work
    //davedel
	@Override
	public boolean/*merged*/ mergeRecs(String oldKey, String topKey) {
		int numMerged = 0;
		boolean merged = false;

		// if there's been a split, we need to clean things up
		// since there may be hanging SourceTypes
		try {
			Utils.trimSourceType(AutomatonAlert.THIS.getApplicationContext());
		} catch (RemoteException ignored) {}

		// original lookupKey for SourceType's: TEXT, PHONE, EMAIL
		SourceTypeDO[] oldArray = {null, null, null};
		// new lookupKey for SourceType's: TEXT, PHONE, EMAIL
		SourceTypeDO[] topArray  = {null, null, null};

		// get set of SourceType's that have the original LookupKey
		List<SourceTypeDO> oldSet = get(oldKey);
		// get set of SourceType's that have the new LookupKey
		List<SourceTypeDO> topSet = get(topKey);

		// organize original SourceType's into the arrays
		for (SourceTypeDO oldSourceType : oldSet) {
			FragmentTypeRT type =
					Enums.getEnum(oldSourceType.getSourceType(), FragmentTypeRT.values(), null);
			if (type != null) {
				oldArray[type.ordinal()] = oldSourceType;
			}
		}
		// organize new id's SourceType's into the arrays
		for (SourceTypeDO topSourceType : topSet) {
			FragmentTypeRT type =
					Enums.getEnum(topSourceType.getSourceType(), FragmentTypeRT.values(), null);
			if (type != null) {
				topArray[type.ordinal()] = topSourceType;
			}
		}

		// if there is no new SourceType but there's an old one,
		// copy the old to the new. Discard all olds.
		int N=oldArray.length;
		for (int i=0;i<N;i++) {
			if (oldArray[i] != null) {
				if (topArray[i] == null) {
					// copy old to new and save with new lookupKey
					SourceTypeDO newSourceType = new SourceTypeDO(oldArray[i]); // old to new
					newSourceType.setLookupKey(topKey);
					newSourceType.save();
					// put it away
					topArray[i] = newSourceType;
					// get rid of old from db
					oldArray[i].delete();
					// EMAIL SourceAccount
					int oldId = oldArray[i].getSourceTypeId();
					int newId = newSourceType.getSourceTypeId();
					String type = newSourceType.getSourceType();
					moveSourceAccounts(type, oldId, newId);
					// PHONE data
					movePhoneData(type, oldKey, topKey);
					// done
					oldArray[i] = null;

					Log.d(ContactActiveFragment.TAG, ".getContactRecsForLookupKey(): replacing(db->top): " + newSourceType);
					merged = true;
					++numMerged;
				}
			}
		}
		// notify our friend in ContactInfo
		boolean contactInfoMerged = new ContactInfoDO().mergeRecs(oldKey, topKey);
		if (contactInfoMerged) {
			merged = true;
		}

		// get rid of orig that are doubled up (in both stOrig and stNew)
		for (SourceTypeDO sourceType : oldArray) {
			if (sourceType != null) {
				sourceType.delete();

				Log.d(ContactActiveFragment.TAG, ".getContactRecsForLookupKey(): " +
						"deleting db: " + sourceType.getSourceType());
                ++numMerged;
				merged = true;
			}
		}

		Log.d(TAG, ".getContactRecsForLookupKey(): number merged: " + numMerged);
        //davedel
        if (numMerged > 0) {
            Utils.writeToDebugLog("SourceType: number merged: " + numMerged);
        }
        //davedel


        return merged;
	}

	private void moveSourceAccounts(String type, int oldId, int newId) {
		if (!FragmentTypeRT.EMAIL.name().equals(type)) {
			return;
		}

		// get every rec with the old SourceTypeId and change it to the new one
		for (SourceAccountDO sourceAccount : SourceAccountDO.getSourceTypeId(oldId)) {
			sourceAccount.setSourceTypeId(newId);
			sourceAccount.save();
		}
	}

	private void movePhoneData(String type, String oldKey, String topKey) {
		if (!FragmentTypeRT.PHONE.name().equals(type)) {
			return;
		}

		Context context = AutomatonAlert.THIS.getApplicationContext();

		// get phone data from old
		Pair<String, String> info = Utils.getContactPhoneRTVM(context, oldKey);

		// we're going to keep the old one around
//		//	reset old
//		if (!info.first.equals("")
//				|| !info.second.equals("0")) {
//			Utils.updatePhoneRTVM(context, oldKey, "", "0");
//		}

		// udpate new phone data with old
		Utils.updatePhoneRTVM(context, topKey, info.first, info.second);

	}

	// CONTACT MERGE happens here: same contact under multiple lookup_key's
	// are merged into one record here with new taking precedence.
	// contactRecs must passed in with it's Comparator snugly in place
	public static void getSourceTypeContacts(
			TreeSet<HashMap<String, String>> contactRecs,
			String skipThisContact,
			boolean justGetOne,
			boolean ignorePhoneType,
			IActivityRefresh activity) {

		// TEXT,PHONE,EMAIL SourceTypeDO's
		// run() in ContactInfoDO.getContactInfo...()
		ContactInfoRunnable contactInfoRunnable = new ContactInfoRunnable();
		// destination list of contacts
		contactInfoRunnable.setContactRecs(contactRecs);
		// vars used in run()
		contactInfoRunnable.setRec(skipThisContact, justGetOne);
		// run() on list
		ContactInfoDO.getContactInfoForContacts(
				contactInfoRunnable,
				new GetListRunnable(ignorePhoneType),
				activity);
	}

	// this just gets the List for the ContactInfoDO.getContactInfoForContacts.
	// pass params for run() in constructor.
	public static class GetListRunnable implements Runnable, ICIListGetter<SourceTypeDO> {
		boolean mIgnorePhoneType;
		List<SourceTypeDO> mList;

		public GetListRunnable(boolean ignorePhoneType) {
			mIgnorePhoneType = ignorePhoneType;
		}

		@Override public List<SourceTypeDO> getList() {
			return mList;
		}
		public void run() {
			mList = getSourceTypeRecsByType(mIgnorePhoneType);
		}
	}

	private static List<SourceTypeDO> getSourceTypeRecsByType(boolean ignorePhoneType) {
		ArrayList<SourceTypeDO> list = new ArrayList<>();
		if (!ignorePhoneType) {
			list.addAll(get(FragmentTypeRT.PHONE));
		}
		list.addAll(get(FragmentTypeRT.TEXT));
		list.addAll(get(FragmentTypeRT.EMAIL));

		return list;
	}

	///////////////////////////////
	// CLASS cInfoRunnable
	///////////////////////////////
	public static class ContactInfoRunnable extends ContactInfoDO.ContactInfoRunnable {
		Rec mRec;

		@Override
		public void run() {
			if (isInvalidObject()
					|| mRec == null
					|| mContactRecs == null) {
				return;
			}

			SourceTypeDO sourceType = (SourceTypeDO)getObject();

			String key = sourceType.getLookupKey();
			String type = sourceType.getSourceType();
			if (key == null) {
				return;
			}
			if (mRec.mSkipThisContact != null) {
				if (key.equals(mRec.mSkipThisContact)) {
					return;
				}
			}
			// MERGE CONTACTS if needed (if contact is joined externally,
			// we need to merge our records here). Need to notify caller
			// (setMerged()) so that it can re-get the contactRecs with the
			// newly-merged contacts
			ContactInfoDO.ContactMergeInfo ci = ContactInfoDO.getContactRecsForLookupKey(
					AutomatonAlert.THIS.getApplicationContext(),
					key,
					type,
					mRec.mJustGetOne,
					new SourceTypeDO()/*to call mergeRecs()*/);
			if (ci.getMerged()) setMerged(true);
			mContactRecs.addAll(ci.getContacts());
		}

		public void setRec(Rec rec) {
			mRec = rec;
		}

		Rec setRec(String skipThisContact, boolean justGetOne) {
			return (mRec = new Rec(skipThisContact, justGetOne));
		}

		@Override
		public boolean isInvalidObject() {
			return !(getObject() instanceof SourceTypeDO);
		}

		public class Rec {
			String mSkipThisContact;
			boolean mJustGetOne;

			Rec(String skipThisContact, boolean justGetOne) {
				mSkipThisContact = skipThisContact;
				mJustGetOne = justGetOne;
			}
		}
	}

	private static Pair<String, String> getPhoneInfo(
			Context context, String lookupKey, ContactListInfo contactListInfo) {

		String customRingtone = null;
		String sendToVM = null;

		if (contactListInfo == null) {
			Pair<String, String> phoneStuff = Utils.getContactPhoneRTVM(context, lookupKey);
			customRingtone = phoneStuff.first;
			sendToVM = phoneStuff.second;
		}
		// get from contactListInfo
		else {
			customRingtone = contactListInfo.getCustomRingtone();
			sendToVM = contactListInfo.getSendToVM();
		}

		return new Pair<>(customRingtone, sendToVM);
	}

	public static SourceTypeDO createRecFromContactsDb(
		Context context, String lookupKey, String type, ContactListInfo contactListInfo) {

		if (TextUtils.isEmpty(lookupKey)) {
			return null;
		}

		// make sure we don't already have one
		SourceTypeDO sourceType = SourceTypeDO.get(lookupKey, type);

		// PHONE only
		// Create PHONE SourceType if needed
		if (sourceType == null
				&& type.equals(FragmentTypeRT.PHONE.name())) {

			// get CUSTOM_RINGTONE & SEND_TO_VM
			Pair<String, String> phoneInfo = getPhoneInfo(context, lookupKey, contactListInfo);
			String customRingtone = phoneInfo.first;
			String sendToVM = phoneInfo.second;

			// MISSING SOURCETYPE, create one
			if (!TextUtils.isEmpty(customRingtone)
					|| !sendToVM.equals("0")) {
				//////////////////////
				// Create SourceType and Notification Item
				//////////////////////
				sourceType = new SourceTypeDO(lookupKey, type);
				NotificationItemDO notificationItem = new NotificationItemDO();
				notificationItem.setSoundPath(customRingtone);
				notificationItem.setSilentMode(sendToVM);
				notificationItem.save();
				// save notificationItemId in SourceType
				sourceType.setNotificationItemId(notificationItem.getNotificationItemId());
				sourceType.save();
			}
		}

		// return old or new
		return sourceType;
	}

	public SourceTypeDO populate(Cursor cursor) {

		isDirty = false;

		mSourceTypeId = cursor.getInt(
				cursor.getColumnIndex(AutomatonAlertProvider.SOURCE_TYPE_ID));

		mLookupKey = cursor.getString(
				cursor.getColumnIndex(AutomatonAlertProvider.SOURCE_TYPE_LOOKUP_KEY));

		mNotificationItemId = cursor.getInt(
				cursor.getColumnIndex(AutomatonAlertProvider.SOURCE_TYPE_NOTIFICATION_ITEM_ID));

		mSourceType = cursor.getString(
				cursor.getColumnIndex(AutomatonAlertProvider.SOURCE_TYPE_SOURCE_TYPE));

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
					AutomatonAlertProvider.SOURCE_TYPE_ID_URI, mSourceTypeId);
			AutomatonAlert.getProvider().delete(uri, null, null);

		} catch (RemoteException e) {
			Log.e(TAG + ".delete()", "delete exception: " + e.toString());
		}

//		mCache.removeFromCache(getLookupKey() + "|" + getSourceType());
	}

	public synchronized void save() {
		// save
		ContentValues cv = AutomatonAlertProvider.getSourceTypeContentValues(
				mLookupKey,
				mNotificationItemId,
				mSourceType
				);

		AutomatonAlertProvider aap =
				(AutomatonAlertProvider)AutomatonAlert.getProvider()
				.getLocalContentProvider();

        int id = aap == null ? -1 :
                aap.insertOrUpdate(
				cv,
				mSourceTypeId,
				AutomatonAlertProvider.SOURCE_TYPE_ID_URI,
				AutomatonAlertProvider.SOURCE_TYPE_TABLE_URI);

		// if inserted, store new id
		if (id != -1
				&& id != mSourceTypeId) {
			mSourceTypeId = id;
		}

		isDirty = false;

//		mCache.replaceFromCache(this);
	}

	////////////////////////////
	// get(id) CACHE
	////////////////////////////

//	public static final Cache mCache =
//			new Cache<SourceTypeDO, SourceTypeDO, String>(50, new SourceTypeDO());
//
//	public static SourceTypeDO get(String lookupKey, String type) {
//		return (SourceTypeDO)mCache.get(getFromDb(lookupKey, type));
//	}
//
//	@Override
//	public SourceTypeDO getCacheRecFromSource(Object paramObject) {
//		if (paramObject == null) {
//			return null;
//		}
//
//		String[] params = ((String)paramObject).split("\\|");
//		if (params.length != 2) {
//			return null;
//		}
//						// lookupKey, type
//		return getNoCache(params[0], params[1]);
//	}
//
//	@Override
//	public Object getId() {
//		return getFromDb(getLookupKey(), getSourceType());
//	}
//
//	private static String getFromDb(String lookupKey, String type) {
//		return lookupKey + "|" + type;
//	}
}
