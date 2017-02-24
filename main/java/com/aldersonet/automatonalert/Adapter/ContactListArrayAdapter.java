package com.aldersonet.automatonalert.Adapter;

import android.content.Context;
import android.provider.ContactsContract.Contacts;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.aldersonet.automatonalert.ActionMode.ContactListInfo;
import com.aldersonet.automatonalert.ActionMode.ContactListInfo.NotifyDataSet;
import com.aldersonet.automatonalert.BitmapLoader.BitmapLoader;
import com.aldersonet.automatonalert.Fragment.ICLFragmentController;

import java.util.HashMap;
import java.util.List;

public class ContactListArrayAdapter extends ArrayAdapter<HashMap<String, String>>
		implements ICLAdapterGetter {

	public static final String TAG = "ContactListArrayAdapter";

	public ContactListAdapterHelper mHelper;
	Context mContext;
	Fragment mFragment;
	AppCompatActivity mActivity;
	List<HashMap<String, String>> mList;
	BitmapLoader.SetListViewScrollBarListener mScrollBarListener;
//	WeakReference mHelperWR;

	public ContactListArrayAdapter(
			Context context,
			int textViewResourceId,
			List<HashMap<String, String>> objects,
			AppCompatActivity activity,
			Fragment fragment) {
		super(context, textViewResourceId, objects);

		mContext = context;
		mActivity = activity;
		mFragment = fragment;
		mList = objects;
		mHelper = new ContactListAdapterHelper(context, this, activity, fragment);
//		mHelperWR = new WeakReference(mHelper);

		setNotifyOnChange(true);

		// for BitmapLoader
		mScrollBarListener =
				new BitmapLoader.SetListViewScrollBarListener(
						mHelper.getBitmapLoader(),
						getListView(),
						mHelper);
		mScrollBarListener.setListViewScrollBarListener();
	}

	private ListView getListView() {
		if (mFragment != null) {
			return ((ICLFragmentController)mFragment).getListView();
		}
		return null;
	}

	@Override
	public final View getView(
			final int position, View convertView, final ViewGroup parent) {

		if (convertView == null) {
			convertView = mHelper.newView(parent);
		}
		String id = mList.get(position).get(Contacts.LOOKUP_KEY);
		String name = mList.get(position).get(Contacts.DISPLAY_NAME);

//		Log.d(TAG + ".bindView()", "calling helper.bindView()");
		mHelper.bindView(mContext, convertView, id, name);

		return convertView;
	}

	ContactListInfo.NotifyDataSet mNotifyDataSet;
	@Override
	public void notifyDataSet() {
		if (mNotifyDataSet == null) {
			mNotifyDataSet = new ContactListInfo.NotifyDataSet();
		}
		NotifyDataSet.Rec rec = new NotifyDataSet.Rec(mFragment, this);
		mNotifyDataSet.setRec(rec);

		mNotifyDataSet.notifyDataSet();
	}

	@Override
	public Object getHelper() {
		return mHelper;
	}

	@Override
	public Fragment getFragment() {
		return mFragment;
	}}
