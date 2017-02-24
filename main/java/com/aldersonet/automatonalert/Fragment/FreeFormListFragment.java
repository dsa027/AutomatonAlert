package com.aldersonet.automatonalert.Fragment;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.aldersonet.automatonalert.ActionBar.ProgressBar;
import com.aldersonet.automatonalert.Activity.ContactFreeFormListActivity;
import com.aldersonet.automatonalert.Activity.ContactFreeFormListActivity.FragmentTypeCL;
import com.aldersonet.automatonalert.Activity.FragmentHostActivity;
import com.aldersonet.automatonalert.Activity.ICLActivity;
import com.aldersonet.automatonalert.Adapter.FreeFormListArrayAdapter;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Filter.FilterItemDO;
import com.aldersonet.automatonalert.Filter.FilterItems;
import com.aldersonet.automatonalert.R;
import com.aldersonet.automatonalert.Util.Utils;

import java.util.ArrayList;

public class FreeFormListFragment extends ListFragment
		implements
		ICLFragmentController,
		IProgressBarListener {

	public static final String TAG = "FreeFormListFragment";

	private IFreeFormListFragmentListener mActivityListener;

	public FragmentTypeCL mFragmentType = FragmentTypeCL.FREEFORM;
	public FreeFormListArrayAdapter mAdapter;
	ListView mListView;
	boolean mContentViewCreated = false;
	ProgressBar mProgressBar = ProgressBar.getInstance();
	ImageView mAddItem;
	ArrayList<FilterItemDO> mList;

	public static FreeFormListFragment newInstance() {
		return new FreeFormListFragment();
	}

	@Override
	public ListView getListView() {
		return mListView;
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
		mContentViewCreated = true;
		// Inflate the layout for this fragment
		View returnView = inflater.inflate(R.layout.free_form_list_fragment, container, false);
		setViewComponents(returnView);
		return returnView;
	}

	long mProgressBarStartKey = Integer.MAX_VALUE;

	// get data in the background
	class FreeFormAsyncTask extends AsyncTask<Object, Object, Object> {
		int mScrollPos = 0; int mScrollTop = 0;

		public FreeFormAsyncTask(ArrayList<FilterItemDO> list) {
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
			mList = getItems();
			return null;
		}

		@Override
		synchronized protected void onPostExecute(Object result) {
			super.onPostExecute(result);
			stopProgressBar();
			if (mList == null) {
				mList = new ArrayList<FilterItemDO>();
			}

			try {
				FreeFormListArrayAdapter adapter = new FreeFormListArrayAdapter(
						getActivity().getApplicationContext(),
						0,
						mList,
						(AppCompatActivity)getActivity(),
						FreeFormListFragment.this);

				setListAdapter(adapter);
				mAdapter = adapter;

			} catch (NullPointerException e) {
				return;
			}

			if (mListView != null) {
                mListView.setSelectionFromTop(mScrollPos, mScrollTop);
            }

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
		TextView textView = (TextView)getActivity().findViewById(R.id.fflf_empty_list_text);
		if (textView == null) {
			return;
		}
		ContactListGenericBase.showHideTextForEmptyList(
				(ICLActivity) getActivity(),
				(mList == null ? 0 : mList.size()),
				mFragmentType,
				textView,
				R.string.free_form_empty_list,
				null);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
	}

	@Override
	public void refreshData(boolean forcedNotUsed) {
		Log.d(TAG + ".refreshData()", "refreshing");
		new FreeFormAsyncTask(null).execute();
	}

	private ArrayList<FilterItemDO> getItems() {
		return FilterItems.get();
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		refreshData(false/*forced*/);
	}

	private void setViewComponents(View v) {
		mListView = (ListView)v.findViewById(android.R.id.list);
		mAddItem = (ImageView)v.findViewById(R.id.fflf_new);

		mAddItem.setOnClickListener(new ImageView.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (!Utils.inAppUpgradeIsFilterItemsAtLimit(getActivity())) {
					Intent intent =
							new Intent(
									getActivity().getApplicationContext(),
									FragmentHostActivity.class);
					intent.putExtra(
							AutomatonAlert.FRAGMENT_TYPE,
							FragmentHostActivity.HostFragmentType.FREEFORM.name());
					intent.putExtra(
							FilterItemDO.TAG_FILTER_ITEM_ID,
							-1);
					getActivity().startActivity(intent);
				}
			}
		});
	}

	@Override
	public String getInfo() {
		return null;
	}

	@Override
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
			mActivityListener = (IFreeFormListFragmentListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement IFreeFormListFragmentListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mActivityListener = null;
		mAdapter = null;
	}

	public interface IFreeFormListFragmentListener {
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
