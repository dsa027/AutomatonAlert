package com.aldersonet.automatonalert.ContactInfo;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import android.provider.ContactsContract.Contacts;
import android.util.Log;
import android.util.Pair;

import com.aldersonet.automatonalert.ActionMode.ContactListInfo;
import com.aldersonet.automatonalert.Activity.ContactFreeFormListActivity.FragmentTypeCL;
import com.aldersonet.automatonalert.Activity.IActivityRefresh;
import com.aldersonet.automatonalert.Activity.RTUpdateActivity.FragmentTypeRT;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Fragment.ILookupKeyMerger;
import com.aldersonet.automatonalert.Provider.AutomatonAlertProvider;
import com.aldersonet.automatonalert.Util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

public class ContactInfoDO
		implements ILookupKeyMerger {

	public static final String TAG = "ContactInfoDO";

	private int mContactInfoId = -1;
	private String mLookupKey;
	private boolean mFavorite;
	private Date mTimeStamp;

	public boolean isDirty;

	public ContactInfoDO() {
			 super();

			 mContactInfoId = -1;
			 mLookupKey = "";
			 mFavorite = false;

			 isDirty = true;

		 }

	public ContactInfoDO(String lookupKey, boolean favorite) {
		super();
		mLookupKey = lookupKey;
		mFavorite = favorite;
	}

	public int getContactInfoId() {
		return mContactInfoId;
	}

	public int setContactInfoId(int id) {
		if (mContactInfoId != id) {
			isDirty = true;
		}
		return mContactInfoId = id;
	}

	public String getLookupKey() {
		return mLookupKey;
	}

	public String setLookupKey(String lookupKey) {
		if (!mLookupKey.equals(lookupKey)) {
			isDirty = true;
		}
		return mLookupKey = lookupKey;
	}

	public boolean isFavorite() {
		return mFavorite;
	}

	public boolean setFavorite(boolean favorite) {
		if (mFavorite != favorite) {
			isDirty = true;
		}
		return mFavorite = favorite;
	}

	public static ContactInfoDO get(int id) {

		Uri uri = ContentUris.withAppendedId(
				AutomatonAlertProvider.CONTACT_INFO_ID_URI, id);

		ContactInfoDO contactInfo = null;

		Cursor cursor = null;
        try {
            cursor = AutomatonAlert.getProvider().query(
                    uri, null, null, null, null);
        } catch (RemoteException e) {
            cursor = null;
            e.printStackTrace();
        }
        try {
			if (cursor != null && cursor.moveToFirst()) {
				contactInfo = new ContactInfoDO().populate(cursor);
			}
		}
		catch (IllegalArgumentException ignored) {}
        finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return contactInfo;	// null if not found
	}

	public static ContactInfoDO get(String lookupKey) {

		Uri uri = AutomatonAlertProvider.CONTACT_INFO_TABLE_URI;

		ContactInfoDO contactInfo = null;

		String selection =
				AutomatonAlertProvider.CONTACT_INFO_LOOKUP_KEY
						+ " = ?";

        Cursor cursor = null;
        try {
            cursor = AutomatonAlert.getProvider().query(
                    uri,
                    null,
                    selection,
                    new String[] { lookupKey },
                    null);
        } catch (RemoteException e) {
            cursor = null;
            e.printStackTrace();
        }

        try {
			if (cursor != null && cursor.moveToFirst()) {
				contactInfo = new ContactInfoDO().populate(cursor);
			}
		}
		catch (IllegalArgumentException ignored) {}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return contactInfo;	// null if not found
	}

	public static List<ContactInfoDO> get(boolean favorite) {
		ArrayList<ContactInfoDO> aList = new ArrayList<ContactInfoDO>();

		Uri uri = AutomatonAlertProvider.CONTACT_INFO_TABLE_URI;
		String selection =
				AutomatonAlertProvider.CONTACT_INFO_FAVORITE +
				" = ?";
		String[] selectionArgs = {
				(favorite) ? AutomatonAlert.TRUE : AutomatonAlert.FALSE
		};

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
					ContactInfoDO contactInfo = new ContactInfoDO().populate(cursor);
					aList.add(contactInfo);
				} while (cursor.moveToNext());
			}
		}
		catch (IllegalArgumentException ignored) {}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return aList;		// empty if not found
	}

	//////////////
	// MERGE
	//////////////
	@Override
	public boolean/*merged*/ mergeRecs(String oldKey, String topKey) {
		int numMerged = 0;
		Log.d(
				TAG, ".mergeRecs(): " +
				"need to merge:"
						+ " top["+topKey + "],"
						+ " old["+oldKey + "]");

		boolean merged = false;
		ContactInfoDO oldRec = ContactInfoDO.get(oldKey);
		if (oldRec != null) {
			merged = true;
			++numMerged;
			Log.d(TAG, ".getContactRecsForLookupKey(): deleting oldRec");
			oldRec.delete();    // get rid of old
			// see if there's already a new one
			ContactInfoDO newRec = ContactInfoDO.get(topKey);
			if (newRec == null) {
				// old to new
				Log.d(TAG, ".getContactRecsForLookupKey(): no newRec; saving with new key");
				oldRec.setLookupKey(topKey);
				oldRec.save(); // it's now a new rec
			}
			else {
				Log.d(TAG, ".getContactRecsForLookupKey():already have newRec, keeping");
			}
		}

		Log.d(TAG, ".getContactRecsForLookupKey(): number merged: " + numMerged);

		return merged;
	}

	public static class ContactMergeInfo extends Pair<Boolean, TreeSet<HashMap<String, String>>> {
		public ContactMergeInfo(Boolean merged, TreeSet<HashMap<String, String>> contacts) {
			super(merged, contacts);
		}

		public boolean getMerged() {
			return first;
		}

		public TreeSet<HashMap<String, String>> getContacts() {
			return second;
		}
	}

	///////////////////////////////////////
	// GET CONTACT RECS FOR LOOKUP KEY
	///////////////////////////////////////
	private static final String[] CONTACTS_PROJECTION = new String[] {
			Contacts._ID,
			Contacts.LOOKUP_KEY,
			Contacts.DISPLAY_NAME,
			Contacts.CUSTOM_RINGTONE,
			Contacts.SEND_TO_VOICEMAIL
	};

	public static ContactMergeInfo/*merged&contacts*/ getContactRecsForLookupKey(
			Context context,
			String dbLookupKey,
			String type,
			boolean justGetOne,
			ILookupKeyMerger merger) {

//		Log.d(TAG + ".getContactRecsForLookupKey()", "using[" + merger.getClass().getSimpleName() + "]");

		TreeSet<HashMap<String, String>> contacts = getEmptySortedTreeSet();
		boolean merged = false;
		Cursor cursor = null;
        cursor = AutomatonAlert.THIS.getContentResolver().query(
                Uri.withAppendedPath(Contacts.CONTENT_LOOKUP_URI, dbLookupKey),
                CONTACTS_PROJECTION,
                null,
                null,
                null);

        try {

			if (cursor != null && cursor.moveToFirst()) {
				int lookupKeyIdx = cursor.getColumnIndex(Contacts.LOOKUP_KEY);
				int displayNameIdx = cursor.getColumnIndex(Contacts.DISPLAY_NAME);
				int customRingtoneIdx = cursor.getColumnIndex(Contacts.CUSTOM_RINGTONE);
				int sendToVoiceMailIdx = cursor.getColumnIndex(Contacts.SEND_TO_VOICEMAIL);

				String topLookupKey;
				String displayName;

				do {
					HashMap<String, String> map = new HashMap<String, String>();

					map.put(Contacts.LOOKUP_KEY, (topLookupKey=cursor.getString(lookupKeyIdx)));
					map.put(Contacts.DISPLAY_NAME, (displayName=cursor.getString(displayNameIdx)));
					map.put(Contacts.CUSTOM_RINGTONE, cursor.getString(customRingtoneIdx));
					map.put(Contacts.SEND_TO_VOICEMAIL, cursor.getString(sendToVoiceMailIdx));
					map.put(AutomatonAlertProvider.SOURCE_TYPE_SOURCE_TYPE, type);

					// make sure we're up-to-date since we're here
					//
					ContactListInfo.updateContactListInfo(dbLookupKey, displayName);

                    // below not working 11/14/2016 dsa
                    //davedel - need to fix
					if (!topLookupKey.equals(dbLookupKey)) {
                        String s =
								".getContactRecsForLookupKey(): " +
								"need to merge:" +
										" \ntop[" + topLookupKey + "]," +
										" \n db[" + dbLookupKey + "]";
                        Log.d(TAG, s);
						// MERGE CONTACTS if needed (if contact is joined
						// externally, we need to merge our records here).
						if (merger.mergeRecs(dbLookupKey, topLookupKey)) {
							merged = true;
						}
                        //davedel
                        Utils.writeToDebugLog("CInfo: " + s);
                        //davedel
                    }
                    //davedel

					contacts.add(map);

					if (justGetOne) {
						break;
					}

				} while (cursor.moveToNext());
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return new ContactMergeInfo(merged, contacts);
	}

	/////////////////////////////////////
	// GET CONTACT INFO FOR CONTACTS
	/////////////////////////////////////
	private static final int MAX_MERGES = 5;
	/* Tries to get contactInfo for the contactRec's */
	/* If one or more contact recs were merged while */
	/* getting the info, we'll need to re-get it.    */
	/* Only re-get once.                             */
	/* contactRecs is written to. */
	//
	// Useful for any DO type object that uses contact lookup_key.
	//
	// Generically loops through a list to get ContactInfo
	// for the contacts. Process is: set();run();isMerged().
	// If merged, will re-get list once and go through again.
	///////
	// NOTE: we are NOT on the UI thread
	//////
	public static void getContactInfoForContacts(
			ContactInfoRunnable runnable,       // e.g., FavoriteRunnable below
			Runnable getList,
			IActivityRefresh activity) {

		boolean mergedRec = false;
		int counter = 0;

		do {
			runnable.mContactRecs.clear();    // new result set
			// get brand new list of data
			getList.run();
			List list = ((ICIListGetter) getList).getList();
			// for each SourceTypeDO/ContactInfoDO
			for (Object object : list) {
				runnable.setObject(object);
				runnable.run();
				if (runnable.getMerged()) {
					mergedRec = true;
					++counter;
				}
			}
		// allow up to 5 merges. we'll get any others next time in...
		} while (mergedRec && counter <= MAX_MERGES);

		if (mergedRec) {
			boolean[] doIts = new boolean[FragmentTypeCL.values().length];
			Arrays.fill(doIts, true);
			activity.refreshFragments(doIts, FragmentTypeCL.values());
		}
	}

	///
	// CONACT INFO RUNNABLE
	//
	// EXTEND this (e.g. FavoritesRunnable below, SourceTypeDO.ContactInfoRunnable)
	///
	public static class ContactInfoRunnable implements Runnable {
		boolean mMerged;
		protected TreeSet<HashMap<String, String>> mContactRecs;
		Object mObject; // SourceTypeDO/ContactInfoDO

		public ContactInfoRunnable() {
			super();
		}

		public void setContactRecs(TreeSet<HashMap<String, String>> contactRecs) {
			mContactRecs = contactRecs;
		}

		public void setObject(Object object) {
			mObject = object;
		}

		protected void setMerged(boolean merged) {
			mMerged = merged;
		}

		boolean getMerged() {
			return mMerged;
		}

		public boolean isInvalidObject() {
			return false;
		}

		public Object getObject() {
			return mObject;
		}

		@Override
		public void run() {
		}
	}

	/////////////////////////
	// FAVORITE RUNNABLE
	/////////////////////////
	public static class FavoriteRunnable extends ContactInfoRunnable {
		@Override
		public void run() {
			if (isInvalidObject()) {
				return;
			}
			// cast for easier access
			ContactInfoDO contactInfo = (ContactInfoDO) getObject();

			// MERGE CONTACTS if needed (if contact is joined externally,
			// we need to merge our records here). Need to notify caller
			// (setMerged()) so that it can re-get the contactRecs with the
			// newly-merged contacts
			ContactMergeInfo ci = getContactRecsForLookupKey(
					AutomatonAlert.THIS.getApplicationContext(),
					contactInfo.getLookupKey(),
					FragmentTypeRT.PHONE.name(),
					false/*justGetOne*/,
					new ContactInfoDO()/*to call mergeRecs()*/);
			if (ci.getMerged()) setMerged(true);
			mContactRecs.addAll(ci.getContacts());
		}

		@Override
		public boolean isInvalidObject() {
			return !(getObject() instanceof ContactInfoDO);
		}
	}

	/* contactRecs is written to */
	public static void getFavoriteContactRecs(
			TreeSet<HashMap<String, String>> contactRecs,
			IActivityRefresh activity) {

		// run() in ContactInfoDO.getContactInfo...()
		FavoriteRunnable favoriteRunnable = new FavoriteRunnable();
		// destination list of contacts
		favoriteRunnable.setContactRecs(contactRecs);
		// run() on list
		getContactInfoForContacts(favoriteRunnable, new GetListRunnable(), activity);
	}

	// this just gets the List for the ContactInfoDO.getContactInfoForContacts.
	// pass params for run() in constructor.
	public static class GetListRunnable implements Runnable, ICIListGetter<ContactInfoDO> {
		List<ContactInfoDO> mList;

		@Override public List<ContactInfoDO> getList() {
			return mList;
		}

		public void run() {
			mList = get(true/*favorite*/);
		}
	}


	/////////////////////////////////////
	// CRUD
	////////////////////////////////////
	public ContactInfoDO populate(Cursor cursor) {

		isDirty = false;

		mContactInfoId = cursor.getInt(
				cursor.getColumnIndex(AutomatonAlertProvider.CONTACT_INFO_ID));

		mLookupKey = cursor.getString(
				cursor.getColumnIndex(AutomatonAlertProvider.CONTACT_INFO_LOOKUP_KEY));

		mFavorite = (cursor.getString(
				cursor.getColumnIndex(AutomatonAlertProvider.CONTACT_INFO_FAVORITE)))
				.equals(AutomatonAlert.TRUE);

		long millis = cursor.getLong(
				cursor.getColumnIndex(AutomatonAlertProvider.CONTACT_INFO_TIMESTAMP));
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
					AutomatonAlertProvider.CONTACT_INFO_ID_URI, mContactInfoId);
			AutomatonAlert.getProvider().delete(uri, null, null);

		} catch (RemoteException e) {
			Log.e(TAG + ".delete()", "delete exception: " + e.toString());
		}
	}

	public synchronized void save() {
		ContentValues cv = AutomatonAlertProvider.getContactInfoContentValues(
				mLookupKey,
				(mFavorite) ? AutomatonAlert.TRUE : AutomatonAlert.FALSE
				);

		AutomatonAlertProvider aap =
				(AutomatonAlertProvider)AutomatonAlert.getProvider()
				.getLocalContentProvider();

		int id = aap == null ? -1 :
                aap.insertOrUpdate(
				cv,
				mContactInfoId,
				AutomatonAlertProvider.CONTACT_INFO_ID_URI,
				AutomatonAlertProvider.CONTACT_INFO_TABLE_URI);

		// if inserted, store new id
		if (id != -1
				&& id != mContactInfoId) {
			mContactInfoId = id;
		}

		isDirty = false;
	}

	public static TreeSet<HashMap<String, String>> getEmptySortedTreeSet() {
		return new TreeSet<HashMap<String, String>>(
				new DisplayNameSortOrderComparator());
	}

	public static class DisplayNameSortOrderComparator
			implements Comparator<HashMap<String, String>> {

		@Override
		public int compare(HashMap<String, String> lhs,
		                   HashMap<String, String> rhs) {
			String lName = lhs.get(Contacts.DISPLAY_NAME);
			String rName = rhs.get(Contacts.DISPLAY_NAME);
			if (lName == null) {
				lName = "";
			}
			if (rName == null) {
				rName = "";
			}
			int comp = lName.compareToIgnoreCase(rName);
			if (comp == 0) {
				String lId = lhs.get(Contacts.LOOKUP_KEY);
				String rId = rhs.get(Contacts.LOOKUP_KEY);
				comp = lId.compareToIgnoreCase(rId);
			}

			return comp;
		}
	}
}
