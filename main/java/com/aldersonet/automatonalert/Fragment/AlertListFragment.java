package com.aldersonet.automatonalert.Fragment;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.Spanned;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.aldersonet.automatonalert.ActionBar.ProgressBar;
import com.aldersonet.automatonalert.Activity.AlertListActivity;
import com.aldersonet.automatonalert.Activity.AlertListActivity.FragmentTypeAL;
import com.aldersonet.automatonalert.Adapter.AlertListArrayAdapter;
import com.aldersonet.automatonalert.Alert.AlertItemDO;
import com.aldersonet.automatonalert.Alert.AlertItemDO.Status;
import com.aldersonet.automatonalert.Alert.AlertItems;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.PostAlarm.PostAlarmDO;
import com.aldersonet.automatonalert.Preferences.GeneralPrefsDO;
import com.aldersonet.automatonalert.R;
import com.aldersonet.automatonalert.Util.Enums;
import com.aldersonet.automatonalert.Util.OnItemRemovedListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AlertListFragment extends ListFragment
		implements
		IALFragmentController,
		IProgressBarListener {

	public static enum Mode {
		ALERT,		// (new) default mode (if no intent passed), show alert items
		ACCOUNT, 	// DEFUNCT: all inbox messages for an account, choose to make alert/saved
		TRASH,		// all trashed messages, choose to recover into saved
		SAVED,		// all saved, choose to trash
		ALARM		// alarms, snoozes
	}

	private Mode mMode = Mode.ALERT;

	private FragmentTypeAL mFragmentType;
	public AlertListArrayAdapter mAdapter;

	private IAlertListFragmentListener mActivityListener;
	boolean mContentViewCreated = false;
	private ArrayList<AlertItemDO> mShowList;
	private ListView mListView;
	long mProgressBarStartKey = Integer.MAX_VALUE;

//	RadioGroup mExpandCollapseCustom;
	Button mExpandAll;
	Button mCollapseAll;
	Button mUnselectAll;
	Button mSelectAll;
	TextView mEmptyListText;

	ProgressBar mProgressBar =
			ProgressBar.getInstance();

	public static AlertListFragment newInstance(String fragmentType) {
		AlertListFragment fragment = new AlertListFragment();
		Bundle args = new Bundle();
		args.putString(AutomatonAlert.FRAGMENT_TYPE, fragmentType);
		fragment.setArguments(args);

		return fragment;
	}

	@Override
	public void startProgressBar() {
		mProgressBarStartKey = ProgressBar.startProgressBar(
				mProgressBar,
				mProgressBarStartKey,
				new ProgressBar.StartObject(
						(AppCompatActivity) getActivity(),
						null,//(BaseAdapter)mAdapter,
						((IALActivityController) getActivity()).getAdapterList()
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
		new GetDataAsyncTask().execute();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	private void cancelNotification() {
		if (mFragmentType == null) {
			return;
		}
		final NotificationManager notificationManager =
				(NotificationManager)getActivity()
					.getSystemService(Context.NOTIFICATION_SERVICE);

		if (mFragmentType.equals(FragmentTypeAL.SAVED)) {
			notificationManager.cancel(AutomatonAlert.ALERT_LIST_FAVED);
		}
		else {
			notificationManager.cancel(AutomatonAlert.ALERT_LIST_NEW);
		}
	}

	private void populateShowList() {
		// default sort comparator
		Comparator<AlertItemDO> comparator =
				new AlertItems.DefaultComparator<AlertItemDO>();

		if (mFragmentType.equals(FragmentTypeAL.NEW)) {
			mMode = Mode.ALERT;
			mShowList = AlertItems.get(Status.NEW, false);
			cancelNotification();
		}
		else if (mFragmentType.equals(FragmentTypeAL.SAVED)) {
			mMode = Mode.SAVED;
			mShowList = AlertItems.get(Status.SAVED, false);
		}
		else if (mFragmentType.equals(FragmentTypeAL.TRASH)) {
			mMode = Mode.TRASH;
			mShowList = AlertItems.get(Status.TRASH, false);
		}
		else if (mFragmentType.equals(FragmentTypeAL.ALARMS)) {
			mMode = Mode.ALARM;
			mShowList = AlertItems.getAlarms(false/*not currentOnly*/);
			comparator = new AlertItems.DateRemindComparator<AlertItemDO>();
		}
		else if (mFragmentType.equals(FragmentTypeAL.SNOOZED)) {
			mMode = Mode.ALARM;
			ArrayList<Pair<AlertItemDO, PostAlarmDO>> snoozes = null;
			snoozes = AutomatonAlert.getAPIs().getSnoozes();
			mShowList = sortSnoozes(snoozes);
		}
		else if (mFragmentType.equals(FragmentTypeAL.REPEATS)) {
			mMode = Mode.ALARM;
			mShowList = getRepeats();
			comparator = new AlertItems.RepeatComparator<AlertItemDO>();
		}
		else {
			mShowList = new ArrayList<AlertItemDO>();
		}

		// cut-off list at requested size
		if (mShowList.size() > GeneralPrefsDO.getMaxListSize()) {
			List<AlertItemDO> subList =
					mShowList.subList(
							0, GeneralPrefsDO.getMaxListSize());
			mShowList = new ArrayList<AlertItemDO>(subList);
		}

		// if NOT snoozed, sort it (snooze already sorted)
		if (!mFragmentType.equals(FragmentTypeAL.SNOOZED)) {
			Collections.sort(mShowList, comparator);
		}

		// if there are a substantial number of items
		// show the fast scroller
		final int SUBSTANTIAL_NUMBER = 50;
		if (getActivity() != null) {
			getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (mShowList.size() > SUBSTANTIAL_NUMBER) {
						// appears to be innocuous, but fastScroll apparently (via StackExchange) causes:
						//requestLayout() improperly called by android.widget.TextView{...app:id/mltn_date_received} during layout: running second layout pass
						mListView.setFastScrollEnabled(true);
						mListView.setFastScrollAlwaysVisible(true);
					}
					else {
						mListView.setFastScrollEnabled(false);
						mListView.setFastScrollAlwaysVisible(false);
					}
				}
			});
		}
	}

	private ArrayList<AlertItemDO> getRepeats() {
		return AlertItems.getRepeatEvery();

	}

	private ArrayList<AlertItemDO> sortSnoozes(
			ArrayList<Pair<AlertItemDO, PostAlarmDO>> snoozes) {

		// descending snooze time
		Collections.sort(
				snoozes,
				new AlertItems.SnoozeComparator<Pair<AlertItemDO, PostAlarmDO>>());

		// create array compatible to mShowList
		ArrayList<AlertItemDO> alertItems =
				new ArrayList<AlertItemDO>(snoozes.size());
		for (Pair<AlertItemDO, PostAlarmDO> pair : snoozes) {
			alertItems.add(pair.first);
		}

		return alertItems;
	}


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View returnView = inflater.inflate(R.layout.alert_list_fragment, container, false);
		mContentViewCreated = true;
		return returnView;
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		setViewComponents(view);					// views populated
	}

	private Spanned getEmptyListText() {
		return Html.fromHtml(
				getResources().getString(R.string.alert_list_empty_list)
		);
	}

	// get data in the background
	class GetDataAsyncTask extends AsyncTask<Object, Object, Object> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			if (getActivity() instanceof AppCompatActivity
					&& getActivity() instanceof AlertListActivity) {
				startProgressBar();
			}
		}

		@Override
		protected Object doInBackground(Object... params) {
			populateShowList();

			if (mAdapter == null) {
				setListAdapter();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Object result) {
			super.onPostExecute(result);

			stopProgressBar();

			if (mAdapter != null) {
				mAdapter.refreshShowList(
						new ArrayList<AlertItemDO>(mShowList));
			}
			if (mListView.getAdapter() == null) {
				mListView.setAdapter(mAdapter);
			}
			if (mActivityListener != null) {
				mActivityListener.setActionBarTitle(null);
			}

			if (mShowList.size() > 0) {
				mEmptyListText.setVisibility(TextView.GONE);
			}
			else {
				mEmptyListText.setVisibility(TextView.VISIBLE);
				mEmptyListText.setText(getEmptyListText());
			}

			if (mAdapter != null) {
				mAdapter.notifyDataSetChanged();
			}

		}
	}

	private void setListAdapter() {
		if (mShowList == null) {
			// fake it
			mShowList = new ArrayList<AlertItemDO>();
		}

		mAdapter = new AlertListArrayAdapter(
				getActivity(),
				this,
				R.layout.alert_list_textview,
				mShowList,
				mMode/*,
				null*/);

		mAdapter.setOnItemRemoved(new MessageListOnItemRemoved());
	}

	@Override
	public void refreshData() {
		new GetDataAsyncTask().execute();
	}

	private void setViewComponents(View v) {
		mListView = getListView();
		mMode = Mode.ALERT;
		setViewControls(v);
	}

    private void setViewControls(View v) {
//    	mExpandCollapseCustom = (RadioGroup)v.findViewById(R.id.ail_radio_group);
    	mExpandAll = (Button)v.findViewById(R.id.ail_expand_all);
    	mCollapseAll = (Button)v.findViewById(R.id.ail_collapse_all);
    	mUnselectAll = (Button)v.findViewById(R.id.ail_unselect_all);
    	mSelectAll = (Button)v.findViewById(R.id.ail_select_all);
    	mEmptyListText = (TextView)v.findViewById(R.id.ail_empty_list_text);

    	mExpandAll.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				mAdapter.expandAll();
				mAdapter.notifyDataSetChanged();
			}
    	});
    	mCollapseAll.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				mAdapter.collapseAll();
				mAdapter.notifyDataSetChanged();
			}
    	});
    	mUnselectAll.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				mAdapter.selectUnselectAllItems(false/*un-select all items*/);
			}
    	});
    	mSelectAll.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(View v) {
				mAdapter.selectUnselectAllItems(true/*select all items*/);
			}
    	});
    }

	public class MessageListOnItemRemoved extends OnItemRemovedListener {
		@Override
		public void setTitle(int num) {
			super.setTitle(num);
			if (mActivityListener != null) {
				mActivityListener.setActionBarTitle(null);
			}
		}
	}

	public BaseAdapter sendAdapterToActivity() {
		if (mActivityListener != null) {
			return mActivityListener.setFragmentAdapterList(mAdapter);
		}
		return null;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// set mFragmentType from ags
		mFragmentType = null;
		if (getArguments() != null) {
			mFragmentType =
					Enums.getEnum(
							getArguments().getString(AutomatonAlert.FRAGMENT_TYPE),
							FragmentTypeAL.values(),
							FragmentTypeAL.SETTINGS);
//					getFragmentType(getArguments().getString(AutomatonAlert.FRAGMENT_TYPE));
		}

		// save calling Activity and give Activity our Adapter
		try {
			mActivityListener = (IAlertListFragmentListener) activity;
			if (mAdapter == null) {
				setListAdapter();
			}
			sendAdapterToActivity();
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement IAlertListFragmentListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		mActivityListener = null;
	}

	public interface IAlertListFragmentListener {
		public BaseAdapter setFragmentAdapterList(BaseAdapter listAdapter);
		public void setActionBarTitle(BaseAdapter adapter);
//		public void refreshFragment(FragmentTypeAL type);
	}

	@Override
	public FragmentTypeAL getFragmentType() {
		return mFragmentType;
	}
}
