package com.aldersonet.automatonalert.Adapter;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract.Contacts;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;

import com.aldersonet.automatonalert.ActionMode.ContactListInfo;
import com.aldersonet.automatonalert.ActionMode.ContactListInfo.NotifyDataSet;
import com.aldersonet.automatonalert.BitmapLoader.BitmapLoader;

public class ContactCursorAdapter extends CursorAdapter
		implements ICLAdapterGetter {

	public static final String TAG = "ContactCursorAdapter";

	public ContactListAdapterHelper mHelper;
	Context mContext;
	ListFragment mFragment;
	boolean mForcedUpdateContactListInfo;
	BitmapLoader.SetListViewScrollBarListener mScrollBarListener;

	public ContactCursorAdapter(
			Context context, Cursor c, AppCompatActivity activity,
			ListFragment fragment, boolean forced) {

		super(context, c, 0);
		mContext = context;
		mFragment = fragment;
		mHelper = new ContactListAdapterHelper(context, this, activity, fragment);
		mForcedUpdateContactListInfo = forced;

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
			return mFragment.getListView();
		}
		return null;
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup parent) {
		return mHelper.newView(parent);
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		String id = cursor.getString(cursor.getColumnIndex(Contacts.LOOKUP_KEY));
		String name = cursor.getString(cursor.getColumnIndex(Contacts.DISPLAY_NAME));

		if (mForcedUpdateContactListInfo) {
			ContactListInfo.updateContactListInfo(id, name);
		}

		mHelper.bindView(context, view, id, name);
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
	}
}
