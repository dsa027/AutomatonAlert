package com.aldersonet.automatonalert.Fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.support.v4.app.ListFragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.aldersonet.automatonalert.ActionBar.ProgressBar;
import com.aldersonet.automatonalert.ActionMode.ContactListInfo;
import com.aldersonet.automatonalert.Activity.ContactFreeFormListActivity;
import com.aldersonet.automatonalert.Activity.ContactFreeFormListActivity.FragmentTypeCL;
import com.aldersonet.automatonalert.Activity.IActivityRefresh;
import com.aldersonet.automatonalert.Activity.ICLActivity;
import com.aldersonet.automatonalert.Adapter.ContactListArrayAdapter;
import com.aldersonet.automatonalert.ContactInfo.ContactInfoDO;
import com.aldersonet.automatonalert.R;
import com.aldersonet.automatonalert.Util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;

public class ContactFavoriteFragment extends ListFragment
		implements
		ICLFragmentController,
		IProgressBarListener {

	public static final String TAG = "ContactFavoriteFragment";

	private IContactFavoriteFragmentListener mActivityListener;

	public FragmentTypeCL mFragmentType = FragmentTypeCL.FAVORITES;
	public ContactListArrayAdapter mAdapter;
	ListView mListView;
	boolean mContentViewCreated = false;
	ProgressBar mProgressBar =
			ProgressBar.getInstance();
	private long mProgressBarStartKey = Integer.MAX_VALUE;
	TextView mEmptyListText;
	ArrayList<HashMap<String, String>> mList;

	public static ContactFavoriteFragment newInstance() {
		return new ContactFavoriteFragment();
	}

	@Override
	public ListView getListView() {
		return mListView;
	}

	public ArrayList<HashMap<String, String>> getList() {
		return mList;
	}

	@Override
	public void stopActionModeAllFragments() {
		if (getActivity() instanceof ContactFreeFormListActivity) {
			((ICLActivity)getActivity()).stopActionModeAllFragments();
		}
	}

	@Override
	public void startProgressBar() {
		mProgressBarStartKey = ProgressBar.startProgressBar(
				mProgressBar,
				mProgressBarStartKey,
				new ProgressBar.StartObject(
						(AppCompatActivity) getActivity(),
						null,
						((ICLActivity) getActivity()).getAdapterList()
				));
	}

	public void stopProgressBar() {
		mProgressBarStartKey =
				ProgressBar.stopProgressBar(mProgressBar, mProgressBarStartKey);
	}

	@Override
	public void onPause() {
		super.onPause();
		mContentViewCreated = false;
	}

	@Override
	public void onStart() {
		super.onStart();
		refreshData(false/*forced*/);
	}

	@Override
	public void onResume() {
		super.onResume();
		stopActionModeAllFragments();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View returnView = inflater.inflate(R.layout.contact_list_fragment, container, false);
		setViewComponents(returnView);
		return returnView;
	}

	// get data in the background
	class ContactAsyncTask extends AsyncTask<Object, Object, Object> {
		int mScrollPos = 0; int mScrollTop = 0;

		public ContactAsyncTask(ArrayList<HashMap<String, String>> list) {
			super();
			mList = list;
		}
		@Override
		synchronized protected void onPreExecute() {
			super.onPreExecute();

			Pair<Integer, Integer> pos = Utils.getScrollPosition(mListView);
			mScrollPos = pos.first;
			mScrollTop = pos.second;

			if (getActivity() instanceof AppCompatActivity
					&& getActivity() instanceof ContactFreeFormListActivity) {
				startProgressBar();
			}
		}

		@Override
		synchronized protected Object doInBackground(Object... params) {
			TreeSet<HashMap<String, String>> mSet;
			mSet = getItems();
			mList = new ArrayList<HashMap<String, String>>(mSet);

			return null;
		}

		@Override
		synchronized protected void onPostExecute(Object result) {
			super.onPostExecute(result);
			stopProgressBar();
			if (mList == null) {
				mList = new ArrayList<HashMap<String, String>>();
			}

			try {
				ContactListArrayAdapter adapter = new ContactListArrayAdapter(
						getActivity().getApplicationContext(),
						0,
						mList,
						(AppCompatActivity)getActivity(),
						ContactFavoriteFragment.this);

				setListAdapter(adapter);
				mAdapter = adapter;

			} catch (NullPointerException e) {
				return;
			}

			mListView.setSelectionFromTop(mScrollPos, mScrollTop);

			if (mContentViewCreated) {
				ListView lv = getListView();
				lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
			}
			sendAdapterToActivity();
			showTextForEmptyList();
			mAdapter.notifyDataSetChanged();
		}
	}

	///////////////////////////
	// IContactListActivity
	///////////////////////////
	@Override
	public void showTextForEmptyList() {
		if (mEmptyListText == null) {
			return;
		}
		ContactListGenericBase.showHideTextForEmptyList(
				(ICLActivity) getActivity(),
				(mList == null ? 0 : mList.size()),
				mFragmentType,
				mEmptyListText,
				R.string.contact_favorite_empty_list,
				ContactListGenericBase.getIntentGoToSearch());
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mContentViewCreated = true;
	}

	@Override
	public void refreshData(boolean forcedNotUsed) {
		Log.d(TAG + ".refreshData()", "refreshing");
		new ContactAsyncTask(null).execute();
	}

	// These are the Contacts rows that we will retrieve.
	static final String[] CONTACTS_PROJECTION = new String[] {
		Contacts._ID,
		Contacts.LOOKUP_KEY,
		Contacts.DISPLAY_NAME,
		Contacts.CUSTOM_RINGTONE,
		Contacts.SEND_TO_VOICEMAIL
	};

	private TreeSet<HashMap<String, String>> getItems() {
		// sorted
		TreeSet<HashMap<String, String>> set = ContactInfoDO.getEmptySortedTreeSet();
		ContactInfoDO.getFavoriteContactRecs(set, (IActivityRefresh)getActivity());

		return set;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (mAdapter == null) {
			refreshData(false/*forceNotUsed*/);
		}
		else {
			ContactListInfo.refreshRow(data, mAdapter);
		}
	}

	private void setViewComponents(View v) {
		mListView = (ListView)v.findViewById(android.R.id.list);
		mEmptyListText =
				(TextView)v.findViewById(R.id.cl_empty_list_text);
	}

	public String getInfo() {
		return null;
	}

	public BaseAdapter sendAdapterToActivity() {
		if (mActivityListener != null) {
			return mActivityListener.setFragmentAdapterHandle(mAdapter);
		}
		return null;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mActivityListener = (IContactFavoriteFragmentListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement IContactFavoriteFragmentListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mActivityListener = null;
		mAdapter = null;
	}

	public interface IContactFavoriteFragmentListener {
		public BaseAdapter setFragmentAdapterHandle(BaseAdapter listAdapter);
	}

	@Override
	public FragmentTypeCL getFragmentType() {
		return mFragmentType;
	}

	@Override
	public BaseAdapter getAdapter() {
		return mAdapter;
	}
}
