package com.aldersonet.automatonalert.Adapter;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.aldersonet.automatonalert.ActionMode.ContactListInfo;
import com.aldersonet.automatonalert.ActionMode.ContactListInfo.NotifyDataSet;
import com.aldersonet.automatonalert.Filter.FilterItemDO;

import java.util.List;

public class FreeFormListArrayAdapter extends ArrayAdapter<FilterItemDO>
		implements ICLAdapterGetter {

	public static final String TAG = "FreeFormListArrayAdapter";

	public FreeFormListAdapterHelper mHelper;
	Context mContext;
	Fragment mFragment;
	List<FilterItemDO> mList;

	public FreeFormListArrayAdapter(
			Context context,
			int textViewResourceId,
			List<FilterItemDO> objects,
			AppCompatActivity activity,
			Fragment fragment) {

		super(context, textViewResourceId, objects);

		mContext = context;
		mFragment = fragment;
		mList = objects;
		mHelper = new FreeFormListAdapterHelper(context, this, activity, fragment);

		setNotifyOnChange(true);
	}

	@Override
	public final View getView(
			final int position, View convertView, final ViewGroup parent) {

		if (convertView == null) {
			convertView = mHelper.newView(parent);
		}
		mHelper.bindView(convertView, mContext, mList.get(position));

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
	}
}
