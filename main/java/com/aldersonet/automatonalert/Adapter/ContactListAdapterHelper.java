package com.aldersonet.automatonalert.Adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.QuickContactBadge;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aldersonet.automatonalert.ActionMode.ContactListInfo;
import com.aldersonet.automatonalert.Activity.ContactFreeFormListActivity.FragmentTypeCL;
import com.aldersonet.automatonalert.Activity.IActivityRefresh;
import com.aldersonet.automatonalert.Activity.ICLActivity;
import com.aldersonet.automatonalert.Activity.RTUpdateActivity;
import com.aldersonet.automatonalert.Activity.RTUpdateActivity.FragmentTypeRT;
import com.aldersonet.automatonalert.Alert.NotificationItemDO;
import com.aldersonet.automatonalert.Alert.NotificationItems;
import com.aldersonet.automatonalert.BitmapLoader.BitmapLoader;
import com.aldersonet.automatonalert.BitmapLoader.IBLGetter;
import com.aldersonet.automatonalert.Cache.Cache;
import com.aldersonet.automatonalert.ContactInfo.ContactInfoDO;
import com.aldersonet.automatonalert.Fragment.ContactActiveFragment;
import com.aldersonet.automatonalert.Fragment.ContactFavoriteFragment;
import com.aldersonet.automatonalert.Fragment.ICLFragmentController;
import com.aldersonet.automatonalert.R;
import com.aldersonet.automatonalert.SourceAccount.SourceAccountDO;
import com.aldersonet.automatonalert.SourceType.SourceTypeDO;
import com.aldersonet.automatonalert.Util.ImageViewCheckBox;
import com.aldersonet.automatonalert.Util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

public class ContactListAdapterHelper
		implements IBLGetter {

	public static final String TAG = "ContactListAdapterHelper";

	public ActionMode mActionMode;
	private int mActionModeItems = 0;

	public BitmapLoader mBitmapLoader;

	Context mContext;
	AppCompatActivity mActivity;
	ListFragment mFragment;
//	public TreeMap<String, ContactListInfo> mContacts;
	LayoutInflater mLayoutInflater;
	BaseAdapter mAdapter;
	View mRootView;

	ActionModeListItemLongClickListener mActionModeLongClickListener;
	BadgeOnClickListener mBadgeOnClickListener;
	CheckBoxOnClickListener mCheckBoxOnClickListener;
	ListItemOnClickListener mListItemOnClickListener;

	public ContactListAdapterHelper(
			Context context,
			BaseAdapter adapter,
			AppCompatActivity activity,
			Fragment fragment) {
		mContext = context;
		mAdapter = adapter;
		mActivity = activity;
		mFragment = (ListFragment)fragment;
//		mContacts = new TreeMap<String, ContactListInfo>();
		mLayoutInflater = (LayoutInflater)context.getSystemService(
				Context.LAYOUT_INFLATER_SERVICE);
		createListeners();

		mBitmapLoader = new BitmapLoader(mAdapter);
	}

	private void createListeners() {
		mActionModeLongClickListener = new ActionModeListItemLongClickListener();
		mBadgeOnClickListener = new BadgeOnClickListener();
		mCheckBoxOnClickListener = new CheckBoxOnClickListener();
		mListItemOnClickListener = new ListItemOnClickListener();
	}

	public static class ViewHolder {
		QuickContactBadge mQuickContactBadge;
		TextView mName;
		RelativeLayout mLayout;
		ImageViewCheckBox mCheckBox;
		ImageView mPhone;
		ImageView mText;
		ImageView mEmail;
		ImageView mFavorite;
	}

	public View newView(ViewGroup parent) {
		View v = mLayoutInflater.inflate(R.layout.contact_list_textview, parent, false);

		ViewHolder viewHolder = new ViewHolder();
		setViewHolder(v, viewHolder);

		return v;
	}

	public ViewHolder setViewHolder(View v, ViewHolder viewHolder) {

		viewHolder.mQuickContactBadge =
				(QuickContactBadge)v.findViewById(R.id.clt_badge);
		viewHolder.mName = (TextView)v.findViewById(R.id.clt_name);
		viewHolder.mLayout = (RelativeLayout)v.findViewById(R.id.clt_main_layout);
		viewHolder.mCheckBox = (ImageViewCheckBox)v.findViewById(R.id.clt_checkbox);
		viewHolder.mPhone = (ImageView)v.findViewById(R.id.clt_phone);
		viewHolder.mText = (ImageView)v.findViewById(R.id.clt_text);
		viewHolder.mEmail = (ImageView)v.findViewById(R.id.clt_email);
		viewHolder.mFavorite = (ImageView)v.findViewById(R.id.clt_favorite);

		// attempted system-level problem app-level fix.
		// Is holo_blue in some circumstances on Samsung Exhilerate
//		viewHolder.mCheckBox.setBackgroundResource(
//				android.R.drawable.screen_background_light);

		setListeners(viewHolder);
		v.setTag(viewHolder);

		return viewHolder;
	}

	public TreeMap<String, ContactListInfo> getContacts() {
		return ContactListInfo.getContacts();
	}

	public void bindView(Context context, View view, String lookupKey, String name) {

		if (mRootView == null) {
			mRootView = view.getRootView();
		}

		// GET TAG - ViewHolder
		ViewHolder viewHolder = (ViewHolder)view.getTag();
		ContactListInfo contactListInfo = null;

		// create or re-use ContactListInfo and put in mContacts
		contactListInfo = getContacts().get(lookupKey);
		if (contactListInfo == null) {
			// "new" adds ContactListInfo into the static array mContacts
			contactListInfo = new ContactListInfo(lookupKey, name, false, null);
//			getContacts().put(lookupKey, contactListInfo);
		}

		// SET TAG - lookupKey
		viewHolder.mName.setText(contactListInfo.getDisplayName());
		viewHolder.mName.setTag(lookupKey);

		Bitmap bitmap = contactListInfo.getPhoto(mAdapter);
		if (bitmap == null) {
			viewHolder.mQuickContactBadge.setImageToDefault();
		}
		else {
			viewHolder.mQuickContactBadge.setImageBitmap(bitmap);
		}

		setFavoriteDrawable(viewHolder.mFavorite, contactListInfo);
		setRingtoneIcons(contactListInfo, viewHolder);
		setListItemViewsActionMode(viewHolder);
	}

	private void setRingtoneIcons(
			ContactListInfo contactListInfo, ViewHolder viewHolder) {

		if (contactListInfo.hasPhone()) {
			viewHolder.mPhone.setImageResource(R.drawable.android_phone_blue_64);
		}
		else {
			viewHolder.mPhone.setImageResource(R.drawable.android_phone_grey_64);
		}

		if (contactListInfo.hasText()) {
			viewHolder.mText.setImageResource(R.drawable.android_messages_blue_64);
		}
		else {
			viewHolder.mText.setImageResource(R.drawable.android_messages_grey_64);
		}

		if (contactListInfo.hasEmail()) {
			viewHolder.mEmail.setImageResource(R.drawable.android_email_blue_blue_64);
		}
		else {
			viewHolder.mEmail.setImageResource(R.drawable.android_email_blue_grey_64);
		}
	}

	TextOnClickListener mTextOnClickListener = new TextOnClickListener();
	class TextOnClickListener implements ImageView.OnClickListener {
		@Override
		public void onClick(View v) {
			if (mActionMode != null) {
				return;
			}
			mFragment.startActivityForResult(
					setRingtoneUpdateIntentAndInfo(v, FragmentTypeRT.TEXT),
					ContactActiveFragment.RT_REQUEST_CODE + FragmentTypeRT.TEXT.ordinal());
		}
	}
	PhoneOnClickListener mPhoneOnClickListener = new PhoneOnClickListener();
	class PhoneOnClickListener implements ImageView.OnClickListener {
		@Override
		public void onClick(View v) {
			if (mActionMode != null) {
				return;
			}
			mFragment.startActivityForResult(
					setRingtoneUpdateIntentAndInfo(v, FragmentTypeRT.PHONE),
					ContactActiveFragment.RT_REQUEST_CODE + FragmentTypeRT.PHONE.ordinal());
		}
	}
	EmailOnClickListener mEmailOnClickListener = new EmailOnClickListener();
	class EmailOnClickListener implements ImageView.OnClickListener {
		@Override
		public void onClick(View v) {
			if (mActionMode != null) {
				return;
			}
			mFragment.startActivityForResult(
					setRingtoneUpdateIntentAndInfo(v, FragmentTypeRT.EMAIL),
					ContactActiveFragment.RT_REQUEST_CODE + FragmentTypeRT.TEXT.ordinal());
		}
	}

	FavoriteOnClickListener mFavoriteOnClickListener = new FavoriteOnClickListener();
	class FavoriteOnClickListener implements ImageView.OnClickListener {
		@Override
		public void onClick(View v) {
			if (mActionMode != null) {
				return;
			}
			boolean isFavorited = false;
			ContactListInfo contactListInfo = getContactListInfo(v);
			if (contactListInfo != null) {
				ContactInfoDO cInfo = contactListInfo.getContactInfo();
				if (cInfo.isFavorite()) {
					cInfo.setFavorite(false);
					cInfo.delete();
				}
				else {
					isFavorited = true;
					cInfo.setFavorite(true);
					cInfo.save();
				}
			}
			setFavoriteDrawable(v, contactListInfo);

			// On ContactFavoriteFragment and need to delete list item
			boolean needRefresh = false;
			if (!isFavorited) {
				if (mFragment instanceof ContactFavoriteFragment) {
					ArrayList<HashMap<String, String>> list =
							((ContactFavoriteFragment) mFragment).getList();
					removeFavorite(list, contactListInfo);
					if (list.size() == 0) {
						needRefresh = true;
					}
				}
			}
			// need to refresh Favorites
			if (needRefresh) {
				refreshAllTheseFragments(
						new FragmentTypeCL[]{
								FragmentTypeCL.FAVORITES,
						});
			}

			// no records added/deleted
			notifyAllTheseAdapters(
					new FragmentTypeCL[] {
							FragmentTypeCL.SEARCH,
							FragmentTypeCL.ACTIVE,
							FragmentTypeCL.FAVORITES
					});
		}
	}

	private boolean removeFavorite(
			ArrayList<HashMap<String, String>> list, ContactListInfo contactListInfo) {

		if (list == null
				|| contactListInfo == null
				|| list.size() == 0) {
			return false;
		}


		String contactInfoLookupKey = contactListInfo.getLookupKey();
		String listLookupKey = "";
		// start from the screen's top list item to save some cycles
		Pair<Integer, Integer> pair = Utils.getScrollPosition(mFragment.getListView());
		int i = (pair.first >= 0) ? pair.first : 0;

		int N=list.size();
		for (;i<N;i++) {
			HashMap<String, String> map = list.get(i);
			if (map != null) {
				listLookupKey = map.get(Contacts.LOOKUP_KEY);
				if (listLookupKey.equals(contactInfoLookupKey)) {
					list.remove(i);
					return true;
				}
			}
		}

		// this should never happen, but if we didn't find
		// the item above, let's go backwards from pair.first-1
		// (since we just went forward from pair.first to last
		if (pair.first != 0) {
			i = pair.first - 1;
			for (;i>=0;i--) {
				HashMap<String, String> map = list.get(i);
				if (map != null) {
					listLookupKey = map.get(Contacts.LOOKUP_KEY);
					if (listLookupKey.equals(contactInfoLookupKey)) {
						list.remove(i);
						return true;
					}
				}
			}
		}

		return false;
	}

	private void setListeners(ViewHolder viewHolder) {
		// long clicks
		viewHolder.mLayout.setOnLongClickListener(mActionModeLongClickListener);
		viewHolder.mPhone.setOnLongClickListener(mActionModeLongClickListener);
		viewHolder.mText.setOnLongClickListener(mActionModeLongClickListener);
		viewHolder.mEmail.setOnLongClickListener(mActionModeLongClickListener);
		viewHolder.mQuickContactBadge.setOnLongClickListener(mActionModeLongClickListener);
		viewHolder.mFavorite.setOnLongClickListener(mActionModeLongClickListener);

		// clicks
		viewHolder.mLayout.setOnClickListener(mListItemOnClickListener);
		viewHolder.mCheckBox.setOnClickListener(mCheckBoxOnClickListener);
		viewHolder.mPhone.setOnClickListener(mPhoneOnClickListener);
		viewHolder.mText.setOnClickListener(mTextOnClickListener);
		viewHolder.mEmail.setOnClickListener(mEmailOnClickListener);
		viewHolder.mQuickContactBadge.setOnClickListener(mBadgeOnClickListener);
		viewHolder.mFavorite.setOnClickListener(mFavoriteOnClickListener);
	}

	private void refreshAllTheseFragments(FragmentTypeCL[] types) {
		boolean[] doIts = new boolean[types.length];
		Arrays.fill(doIts, true);
		((IActivityRefresh)mActivity).refreshFragments(doIts, types);
	}

	private void notifyAllTheseAdapters(FragmentTypeCL[] types) {
		boolean[] doIts = new boolean[types.length];
		Arrays.fill(doIts, true);
		((ICLActivity)mActivity).notifyAdapters(doIts, types);
	}

	private void setFavoriteDrawable(View v, ContactListInfo contactListInfo) {
		if (contactListInfo == null) {
			contactListInfo = getContactListInfo(v);
		}

		if (contactListInfo.getContactInfo().isFavorite()) {
			((ImageView)v).setImageResource(
					R.drawable.favorite_on_app_blue);
		}
		else {
			((ImageView)v).setImageResource(
					R.drawable.favorite_off_holo_light);
		}
	}

	class BadgeOnClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			if (mActionMode != null) {
				return;
			}
//			stopActionModeAllFragments();
			showContactLookup(v);
		}
	}

	private void showContactLookup(View v) {
		String id = getLookupKeyFromView(v);
		Intent intent = new Intent();
		intent.setAction(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(Contacts.CONTENT_LOOKUP_URI + "/" + id));
		mActivity.startActivityForResult(intent, 1);
	}

	private Intent setRingtoneUpdateIntentAndInfo(
			View v, FragmentTypeRT sourceType) {

		String id = getLookupKeyFromView(v);
		ContactListInfo contactListInfo = getContacts().get(id);

		Intent intent = new Intent();

		intent.setClass(mContext, RTUpdateActivity.class);
		intent.putExtra(Contacts.LOOKUP_KEY, contactListInfo.getLookupKey());
		intent.putExtra(Contacts.DISPLAY_NAME, contactListInfo.getDisplayName());
		intent.putExtra(ContactListInfo.TAG_MARKED_FOR_DELETION,
				contactListInfo.getMarkedForDeletion());
		intent.putExtra(RTUpdateActivity.TAG_FRAGMENT_TYPE, sourceType.name());

		return intent;
	}

	class ListItemOnClickListener implements RelativeLayout.OnClickListener {
		@Override
		public void onClick(View v) {
			if (mActionMode != null) {
				return;
			}
			mFragment.startActivityForResult(
					setRingtoneUpdateIntentAndInfo(v, FragmentTypeRT.TEXT),
					ContactActiveFragment.RT_REQUEST_CODE + FragmentTypeRT.TEXT.ordinal());
		}
	}

	private String getLookupKeyFromView(View v) {
		//main layout contains ViewHolder
		//name field contains lookupKey
		return (String)(getViewHolder(v).mName.getTag());
	}

	private ViewHolder getViewHolder(View v) {
		View vp = v;

		while (vp.getId() != R.id.clt_main_layout) {
			vp = (View)vp.getParent();
		}
		return (ViewHolder)vp.getTag();
	}

	private ContactListInfo getContactListInfo(View v) {
		String id = getLookupKeyFromView(v);
		return (getContacts().get(id));
	}

	private boolean itemSelectedUnselectedActionMode(View v) {
		ContactListInfo contactListInfo = getContactListInfo(v);
		if (contactListInfo == null) {
			return false;
		}
		ViewHolder vh = getViewHolder(v);

		if (mActionMode == null) {
			mActionMode = mActivity.startSupportActionMode(mActionModeCallback);
			clearAllActionModeViewsAndLists();
			showAllCheckBoxes();
		}
		// from marked to not-marked
		if (contactListInfo.mSelected) {
			--mActionModeItems;
			contactListInfo.mSelected = false;
			vh.mCheckBox.setChecked(false);
		}
		// from not-marked to marked
		else {
			++mActionModeItems;
			contactListInfo.mSelected = true;
			vh.mCheckBox.setChecked(true);
		}
		setListItemViewsActionMode(vh);
		actionModeSubTitleUpdate();

		return true;
	}

	class ActionModeListItemLongClickListener implements View.OnLongClickListener {
		// Called when the user long-clicks on someView
		public boolean onLongClick(View v) {
			return itemSelectedUnselectedActionMode(v);
		}
	}

	private void clearPhoneRT(ContactListInfo cli) {
		Utils.updatePhoneRTVM(mActivity, cli.mLookupKey, "", "0");
	}

	private void deleteAllRT(ContactListInfo cli) {
		// All we have to do is get rid of all
		// SourceType recs with lookupKey as the id
		// and the NotificationItem in the SourceType.
		// Then reset the Contacts table lookupKey
		// entry to get rid of contact RT and Send To VM
		List<SourceTypeDO> list = SourceTypeDO.get(cli.mLookupKey);
		NotificationItemDO notificationItem = null;
		for (SourceTypeDO sourceType : list) {
			int id = sourceType.getNotificationItemId();
			if (id >= 0) {
				notificationItem = NotificationItems.get(id);
				if (notificationItem != null) {
					notificationItem.delete();
				}
				// SourceAccount holds the email Account's
				// that are used in periodic search
				ArrayList<SourceAccountDO> sourceAccounts =
						SourceAccountDO.getSourceTypeId(id);
				for (SourceAccountDO sourceAccount : sourceAccounts) {
					sourceAccount.delete();
				}
			}
			sourceType.delete();
		}
		clearPhoneRT(cli);
	}

	private void clearRingtoneSelectedItems() {
		new Thread(new Runnable() {
			int cleared = 0;
			@Override
			public void run() {
				for (ContactListInfo cli : getContacts().values()) {
					if (cli.mSelected) {
						deleteAllRT(cli);
						cleared++;
						cli.setMarkedForDeletion(false);
						cli.setData();  // re-get for re-fresh
					}
				}
				mActivity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (cleared > 0) {
							// still in these
							notifyAllTheseAdapters(
									new FragmentTypeCL[] {
											FragmentTypeCL.FAVORITES,
											FragmentTypeCL.SEARCH
									});
							// will have been cleared from here
							refreshAllTheseFragments(
									new FragmentTypeCL[] {
											FragmentTypeCL.ACTIVE,
									});
						}
						AlertListArrayAdapter.toastIt(mActivity, cleared, "item", "cleared");
						clearAllActionMode();
					}
				});
			}
		}).start();
	}

	private void showProgressBar(MenuItem item) {
		View actionView = null;
		LayoutInflater inflater =
				(LayoutInflater)mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		actionView = inflater.inflate(R.layout.actionbar_refresh_progressbar, null);
		item.setActionView(actionView);
	}

	private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			showProgressBar(item);
			// Respond to clicks on the actions in the CAB
			switch (item.getItemId()) {
			case R.id.ringtones_clear:
				clearRingtoneSelectedItems();
				return true;
			default:
				return false;
			}
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();
			inflater.inflate(R.menu.contact_list_menu, menu);
	        return true;
        }

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			mActionMode = null;
			clearAllActionMode();
	    }

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
	        return false;
		}

	};

	class CheckBoxOnClickListener implements CheckBox.OnClickListener {
		@Override
		public void onClick(View v) {
			// toggle isChecked
			ImageViewCheckBox cb = (ImageViewCheckBox)v;
			cb.setChecked(!cb.isChecked());

			ContactListInfo contactListInfo = getContactListInfo(v);
			if (contactListInfo == null) {
				return;
			}

			// selectItemActionMode is a from-to
			// so we need to do the opposite of
			// what is checked/not checked here
			if (cb.isChecked()) {
				// will change in selectItemActionMode
				// to markedForDeletion = true;
				contactListInfo.setMarkedForDeletion(false);
			}
			else {
				contactListInfo.setMarkedForDeletion(true);
			}
			itemSelectedUnselectedActionMode(v);
		}
	}

	private void setListItemViewsActionMode(View v) {
		setListItemViewsActionMode(getViewHolder(v));

	}
	private void setListItemViewsActionMode(ViewHolder vh) {
		if (vh == null) {
			return;
		}
//		Resources res = mActivity.getResources();
//		int colorHoloBlueDark =	res.getColor(android.R.color.holo_blue_dark);
//		int colorBackgroundLight = res.getColor(android.R.color.background_light);
		int colorHoloBlueDark =	0xff0099cc;
		int colorBackgroundLight = 0xffffffff;

		if (mActionMode == null) {
			if (vh.mCheckBox.isChecked()) {
				vh.mCheckBox.setChecked(false);
			}
//			if (vh.mCheckBox.getVisibility() == CheckBox.VISIBLE) {
//				vh.mCheckBox.setVisibility(CheckBox.GONE);
//			}
			vh.mLayout.setBackgroundColor(colorBackgroundLight);
			vh.mName.setTextColor(colorHoloBlueDark);
			return;
		}

		// in ACTION MODE
		vh.mCheckBox.setVisibility(CheckBox.VISIBLE);

		String key = (String)vh.mName.getTag();
		ContactListInfo contactListInfo = getContacts().get(key);

		if (contactListInfo == null) {
			return;
		}

		if (contactListInfo.getMarkedForDeletion()) {
			vh.mLayout.setBackgroundColor(colorHoloBlueDark);
			vh.mName.setTextColor(colorBackgroundLight);
			vh.mCheckBox.setChecked(true);
		}
		else {
			vh.mLayout.setBackgroundColor(colorBackgroundLight);
			vh.mName.setTextColor(colorHoloBlueDark);
			vh.mCheckBox.setChecked(false);
		}
	}

	private void clearAllActionModeViewsAndLists() {
		for (ContactListInfo cli : getContacts().values()) {
			cli.setMarkedForDeletion(false);
		}
		mActionModeItems = 0;
		ListView lv = mFragment.getListView();
		if (lv != null) {
			int count = lv.getChildCount();
			for (int i=0;i<count;i++) {
				View child = lv.getChildAt(i);
				setListItemViewsActionMode(child);
			}
		}
	}

	private void showAllCheckBoxes() {
		ListView lv = mFragment.getListView();
		if (lv != null) {
			int count = lv.getChildCount();
			for (int i=0;i<count;i++) {
				View child = lv.getChildAt(i);
				ViewHolder vh = getViewHolder(child);
				if (vh != null) {
					vh.mCheckBox.setVisibility(CheckBox.VISIBLE);
					vh.mCheckBox.setChecked(false);
				}
			}
		}
	}

	private void clearAllActionMode() {
		if (mActionMode != null) {
			mActionMode.finish();
			mActionMode = null;
		}
		mActionModeItems = 0;
		for (ContactListInfo contactListInfo : getContacts().values()) {
			contactListInfo.setMarkedForDeletion(false);
		}

		clearAllActionModeViewsAndLists();

		if (mAdapter instanceof ContactCursorAdapter) {
			mAdapter.notifyDataSetChanged();
		}
		else if (mAdapter instanceof ContactListArrayAdapter) {
			mAdapter.notifyDataSetChanged();
		}
	}

	private void actionModeSubTitleUpdate() {
		if (mActionModeItems == 0) {
			mActionMode.setSubtitle(null);
			clearAllActionMode();
		}
		else {
			mActionMode.setSubtitle("" + mActionModeItems + " Selected");
		}
	}

	@Override
	public void setBitmap(String lookupKey, Bitmap bitmap) {
		String fn = ((ICLFragmentController)mFragment).getFragmentType().name();
		Log.d(TAG + ".setBitmap("+fn+")", "lookupKey[" + Cache.ellipseString(lookupKey) + "]");
		if (getContacts() != null) {
			ContactListInfo contactListInfo = getContacts().get(lookupKey);
			if (contactListInfo == null) {
				Log.d(TAG + ".setBitmap("+fn+")", "CREATING ContactListInfo");
				return;
			}
//			Log.d(TAG + ".setBitmap("+fn+")", "SETTING PHOTO in contactListInfo");
			contactListInfo.setPhoto(bitmap);
		}
	}

	@Override
	public BitmapLoader getBitmapLoader() {
		return mBitmapLoader;
	}

}
