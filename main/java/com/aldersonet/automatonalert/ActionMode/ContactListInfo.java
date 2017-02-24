package com.aldersonet.automatonalert.ActionMode;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.util.Pair;
import android.widget.BaseAdapter;

import com.aldersonet.automatonalert.Activity.RTUpdateActivity.FragmentTypeRT;
import com.aldersonet.automatonalert.Adapter.ICLAdapterGetter;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.BitmapLoader.BitmapLoader;
import com.aldersonet.automatonalert.BitmapLoader.IBLGetter;
import com.aldersonet.automatonalert.ContactInfo.ContactInfoDO;
import com.aldersonet.automatonalert.Fragment.ICLFragmentController;
import com.aldersonet.automatonalert.Util.Utils;

import java.util.TreeMap;

public class ContactListInfo {

	public static final String TAG = "ContactListInfo";

	public static String TAG_MARKED_FOR_DELETION = "selected";

	String mCustomRingtone;
	String mSendToVM;
	public boolean mSelected;
	public String mLookupKey;

	String mDisplayName;
	Boolean mHasText;
	Boolean mHasPhone;
	Boolean mHasEmail;
	Bitmap mPhoto;
	ContactInfoDO mContactInfo;

	boolean mLookedForPhoto;

	static TreeMap<String, ContactListInfo> mContacts =
			new TreeMap<String, ContactListInfo>();
	static long mKilledBitmapSize;

	public ContactListInfo(
			String lookupKey, String displayName,
			boolean selected, ContactInfoDO contactInfo) {

		init();

		mLookupKey = lookupKey;
		mDisplayName = displayName;
		mSelected = selected;
		mContactInfo = contactInfo;
		mLookedForPhoto = false;
		// setData(); lazy load via getters instead

		mContacts.put(mLookupKey, this);
	}

	public static void updateContactListInfo(String lookupKey, String displayName) {
		ContactListInfo contactListInfo = getContacts().get(lookupKey);

		if (contactListInfo != null) {
			contactListInfo.reset();    // init applicable vars to null to signal re-get
		}
		else {
			// "new" adds ContactListInfo into the static array mContacts
			new ContactListInfo(lookupKey, displayName, false, null);
//			getContacts().put(lookupKey, contactListInfo);
		}
	}

	private static void addBitmapSize(Bitmap bitmap) {
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			mKilledBitmapSize += bitmap.getAllocationByteCount();
		}
		else {
			mKilledBitmapSize += bitmap.getByteCount();
		}
	}

	public static void killAllBitmaps() {
		mKilledBitmapSize = 0;
		for(ContactListInfo cli : mContacts.values()) {
			cli.mLookedForPhoto = true;

			if (cli.mPhoto != null) {
				addBitmapSize(cli.mPhoto);
				cli.mPhoto.recycle();
				cli.mPhoto = null;
			}
		}
	}

	public void setData() {
		// get from database
		setPhoneData();
		setHasPhone();
		setHasText();
		setHasEmail();
		getContactInfo();
//		getPhotoBitmap();
	}

	// will cause getters to re-get this data
	public void reset() {
		mCustomRingtone = null;
		mSendToVM = null;
		mSelected = false;
		mHasText = null;
		mHasPhone = null;
		mHasEmail = null;
		// mPhoto = null;   // don't re-get this, too expensive
		mContactInfo = null;
	}

	public void init() {
		reset();
		mPhoto = null;
	}

	private void setHasPhone() {
		mHasPhone = Utils.verifySourceTypeData(
				AutomatonAlert.THIS.getApplicationContext(),
				mLookupKey, FragmentTypeRT.PHONE.name(), this);
	}

	private void setHasText() {
		mHasText = Utils.verifySourceTypeData(
				AutomatonAlert.THIS.getApplicationContext(),
				mLookupKey, FragmentTypeRT.TEXT.name(), this);
	}

	private void setHasEmail() {
		mHasEmail = Utils.verifySourceTypeData(
				AutomatonAlert.THIS.getApplicationContext(),
				mLookupKey, FragmentTypeRT.EMAIL.name(), this);
	}

	private void setPhoneData() {
		Pair<String, String> phoneInfo = Utils.getContactPhoneRTVM(
				AutomatonAlert.THIS.getApplicationContext(),
				mLookupKey);
		mCustomRingtone = phoneInfo.first;
		mSendToVM = phoneInfo.second;
	}

	private Bitmap getPhotoBitmap(BaseAdapter adapter) {
		if (adapter == null) {
			return null;
		}
		BitmapLoader bitmapLoader =
				((IBLGetter)((ICLAdapterGetter)adapter).getHelper()).getBitmapLoader();
		if (bitmapLoader != null) {
			// the meat
			bitmapLoader.getPhotoWithDelay(mLookupKey);
			// ^^^
		}

		return null;
	}

	public void setPhoto(Bitmap bitmap) {
		if (bitmap == null) {
			mLookedForPhoto = true;
		}
		mPhoto = bitmap;
	}

	public static TreeMap<String, ContactListInfo> getContacts() {
		return mContacts;
	}

	public boolean hasPhone() {
		if (mHasPhone == null) {
			setHasPhone();
		}
		return mHasPhone;
	}
	public boolean hasText() {
		if (mHasText == null) {
			setHasText();
		}
		return mHasText;
	}
	public boolean hasEmail() {
		if (mHasEmail == null) {
			setHasEmail();
		}
		return mHasEmail;
	}
	public Bitmap getPhoto(BaseAdapter adapter) {
		if (mPhoto != null
				|| mLookedForPhoto) {
//			Log.d(TAG + ".getPhoto()", "have photo already");
			return mPhoto;
		}
		getPhotoBitmap(adapter);

		return null;
	}

	public String getCustomRingtone() {
		if (mCustomRingtone == null) {
			setPhoneData();
		}
		return mCustomRingtone;
	}

	public String getSendToVM() {
		if (mSendToVM == null) {
			setPhoneData();
		}
		return mSendToVM;
	}

	public String getLookupKey() {
		return mLookupKey;
	}

	public String getDisplayName() {
		return mDisplayName;
	}

	public boolean getMarkedForDeletion() {
		return mSelected;
	}

	public ContactInfoDO getContactInfo() {
		if (mContactInfo == null) {
			// DB
			mContactInfo = ContactInfoDO.get(mLookupKey);
			if (mContactInfo == null) {
				// NEW
				mContactInfo = new ContactInfoDO(mLookupKey, false);
			}
		}
		return mContactInfo;
	}

	public void setLookupKey(String lookupKey) {
		mLookupKey = lookupKey;
	}

	public void setDisplayName(String displayName) {
		mDisplayName = displayName;
	}

	public void setMarkedForDeletion(Boolean selected) {
		mSelected = selected;
	}

	public void setContactInfo(ContactInfoDO contactInfo) {
		mContactInfo = contactInfo;
	}

	public static void refreshRow(Intent intent, BaseAdapter adapter) {
		// get the lookupKey
		if (intent != null) {
			Bundle bundle = intent.getExtras();
			String lookupKey = bundle.getString(ContactsContract.Contacts.LOOKUP_KEY);
			// get the rec with the key
			ContactListInfo contactListInfo = getContacts().get(lookupKey);

			// change the data and notify
			if (contactListInfo != null) {
				contactListInfo.setData();      // refresh the row
				adapter.notifyDataSetChanged();
			}
		}
	}

	public static class NotifyDataSet {
		Rec mRec;

		public NotifyDataSet() {

		}

		public void setRec(Rec rec) {
			mRec = rec;
		}

		public void notifyDataSet() {
			if (mRec.mFragment == null
					|| mRec.mFragment.getActivity() == null) {
				return;
			}

			mRec.mFragment.getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Log.d(TAG + ".notifyDataSet()",
							((ICLFragmentController) mRec.mFragment).getFragmentType().name());
					mRec.mAdapter.notifyDataSetChanged();
				}
			});
		}

		public static class Rec {
			public Fragment mFragment;
			public BaseAdapter mAdapter;

			public Rec(Fragment fragment, BaseAdapter adapter) {
				mFragment = fragment;
				mAdapter = adapter;
			}
		}
	}
}
