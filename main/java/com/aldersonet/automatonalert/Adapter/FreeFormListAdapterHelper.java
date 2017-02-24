package com.aldersonet.automatonalert.Adapter;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aldersonet.automatonalert.Account.AccountDO;
import com.aldersonet.automatonalert.Account.Accounts;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Filter.FilterItemAccountDO;
import com.aldersonet.automatonalert.Filter.FilterItemDO;
import com.aldersonet.automatonalert.Filter.FilterItems;
import com.aldersonet.automatonalert.Activity.ContactFreeFormListActivity;
import com.aldersonet.automatonalert.Activity.FragmentHostActivity;
import com.aldersonet.automatonalert.Activity.FragmentHostActivity.HostFragmentType;
import com.aldersonet.automatonalert.R;
import com.aldersonet.automatonalert.ActionMode.FreeFormListInfo;
import com.aldersonet.automatonalert.Util.ImageViewCheckBox;

import java.util.TreeMap;

public class FreeFormListAdapterHelper {

	public ActionMode mActionMode;
	private int mActionModeItems = 0;

	Context mContext;
	AppCompatActivity mActivity;
	ListFragment mFragment;
	TreeMap<Integer, FreeFormListInfo> mFilterItems;
	LayoutInflater mLayoutInflater;
	Adapter mAdapter;
	View mRootView;

	ActionModeListItemLongClickListener mActionModeLongClickListener;
	ListItemOnClickListener mListItemOnClickListener;
	CheckBoxOnClickListener mCheckBoxOnClickListener;

	public FreeFormListAdapterHelper(
			Context context,
			Adapter adapter,
			AppCompatActivity activity,
			Fragment fragment) {
		mContext = context;
		mAdapter = adapter;
		mActivity = activity;
		mFragment = (ListFragment)fragment;
		mFilterItems = new TreeMap<Integer, FreeFormListInfo>();
		mLayoutInflater = (LayoutInflater)context.getSystemService(
				Context.LAYOUT_INFLATER_SERVICE);
		createListeners();
	}

	private void createListeners() {
		mActionModeLongClickListener = new ActionModeListItemLongClickListener();
		mListItemOnClickListener = new ListItemOnClickListener();
		mCheckBoxOnClickListener = new CheckBoxOnClickListener();
	}

	public static class ViewHolder {
		RelativeLayout mLayout;
		TextView mPhrase;
		TextView mHeaders;
		TextView mAccounts;
		ImageViewCheckBox mCheckBox;
	}

	public View newView(ViewGroup parent) {
		View v = mLayoutInflater.inflate(R.layout.free_form_list_textview, parent, false);

		ViewHolder viewHolder = new ViewHolder();
		setViewHolder(v, viewHolder);

		return v;
	}

	public ViewHolder setViewHolder(View v, ViewHolder viewHolder) {
		viewHolder.mLayout = (RelativeLayout)v.findViewById(R.id.fflt_main_layout);
		viewHolder.mPhrase = (TextView)v.findViewById(R.id.fflt_phrase);
		viewHolder.mHeaders = (TextView)v.findViewById(R.id.fflt_headers);
		viewHolder.mAccounts = (TextView)v.findViewById(R.id.fflt_accounts);
		viewHolder.mCheckBox = (ImageViewCheckBox)v.findViewById(R.id.fflt_checkbox);

		setListeners(viewHolder);
		viewHolder.mLayout.setTag(viewHolder);

		return viewHolder;
	}

	private String getAccountsString(FilterItemDO filterItem) {
		String sAccounts = "";

		for (FilterItemAccountDO fia : filterItem.getAccounts()) {
			if (!TextUtils.isEmpty(sAccounts)) {
				sAccounts += ",\n ";
			}
			AccountDO account = Accounts.get(fia.getAccountId());
			if (account != null) {
				sAccounts += account.getName();
			}
		}
		if (TextUtils.isEmpty(sAccounts)) {
			sAccounts = "no accounts specified!";
		}

		return sAccounts;
	}

	private String getHeadersString(FilterItemDO filterItem) {
		String headers = filterItem.getSortedFieldNames(true/*isForDisplay*/);

		if (TextUtils.isEmpty(headers)) {
			headers = "no fields specified!";
		}

		return headers;
	}

	public void bindView(View view, Context context, FilterItemDO filterItem) {
		if (mRootView == null) {
			mRootView = view.getRootView();
		}
		ViewHolder viewHolder = (ViewHolder)view.getTag();
		FreeFormListInfo ffListInfo = null;
		int filterItemId = filterItem.getFilterItemId();

		if (!mFilterItems.containsKey(filterItemId)) {
			ffListInfo = new FreeFormListInfo(context, filterItemId, false);
			mFilterItems.put(filterItemId, ffListInfo);
		}

		viewHolder.mPhrase.setTag(filterItem);

		viewHolder.mPhrase.setText(filterItem.getPhrase());
		viewHolder.mHeaders.setText(getHeadersString(filterItem));
		viewHolder.mAccounts.setText(getAccountsString(filterItem));
		setListItemViewsActionMode(view);
	}

	private void setListeners(ViewHolder viewHolder) {
		// long clicks
		viewHolder.mLayout.setOnLongClickListener(mActionModeLongClickListener);
		viewHolder.mPhrase.setOnLongClickListener(mActionModeLongClickListener);
		viewHolder.mHeaders.setOnLongClickListener(mActionModeLongClickListener);
		viewHolder.mAccounts.setOnLongClickListener(mActionModeLongClickListener);
		// clicks
		viewHolder.mLayout.setOnClickListener(mListItemOnClickListener);
		viewHolder.mPhrase.setOnClickListener(mListItemOnClickListener);
		viewHolder.mHeaders.setOnClickListener(mListItemOnClickListener);
		viewHolder.mAccounts.setOnClickListener(mListItemOnClickListener);

		viewHolder.mCheckBox.setOnClickListener(mCheckBoxOnClickListener);

	}

	class ListItemOnClickListener implements RelativeLayout.OnClickListener {
		@Override
		public void onClick(View v) {
			if (mActionMode != null) {
				return;
			}

			// get ViewHolder
			View vhView = null;
			if (v instanceof RelativeLayout) {
				vhView = v;
			}
			else {
				vhView = (View) v.getParent();
			}
			ViewHolder vh = (ViewHolder)vhView.getTag();

			// get filterItemId
			FilterItemDO filterItem = null;
			int filterItemId = -1;
			if (vh != null) {
				filterItem = (FilterItemDO)vh.mPhrase.getTag();
			}
			if (filterItem != null) {
				filterItemId = filterItem.getFilterItemId();
			}


			// start activity
			Intent intent =
					new Intent(mActivity.getApplicationContext(), FragmentHostActivity.class);
			intent.putExtra(
					AutomatonAlert.FRAGMENT_TYPE,
					HostFragmentType.FREEFORM.name());
			intent.putExtra(
					FilterItemDO.TAG_FILTER_ITEM_ID,
					filterItemId);
			mActivity.startActivity(intent);
		}
	}

	private ViewHolder getViewHolder(View v) {
		View vp = v;

		while (vp.getId() != R.id.fflt_main_layout) {
			vp = (View)vp.getParent();
		}
		return (ViewHolder)vp.getTag();
	}

	private FreeFormListInfo getFreeFormListInfo(View v) {
		ViewHolder vh = getViewHolder(v);
		if (vh != null) {
			FilterItemDO filterItem = (FilterItemDO)vh.mPhrase.getTag();
			if (filterItem != null) {
				return mFilterItems.get(filterItem.getFilterItemId());
			}
		}

		return null;
	}

	private boolean itemSelectedUnselectedActionMode(View v) {
		FreeFormListInfo ffListInfo = getFreeFormListInfo(v);
		if (ffListInfo == null) {
			return false;
		}
		ViewHolder vh = getViewHolder(v);

		if (mActionMode == null) {
			mActionMode = mActivity.startSupportActionMode(mActionModeCallback);
			clearAllActionModeViewsAndLists();
			showAllCheckBoxes();
		}
		// from marked to not-marked
		if (ffListInfo.mSelected) {
			--mActionModeItems;
			ffListInfo.mSelected = false;
			vh.mCheckBox.setChecked(false);
		}
		// from not-marked to marked
		else {
			++mActionModeItems;
			ffListInfo.mSelected = true;
			vh.mCheckBox.setChecked(true);
		}
		setListItemViewsActionMode(v);
		actionModeSubTitleUpdate();

		return true;
	}

	class ActionModeListItemLongClickListener implements View.OnLongClickListener {
		// Called when the user long-clicks on someView
		public boolean onLongClick(View v) {
			return itemSelectedUnselectedActionMode(v);
		}
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
				clearFreeFormSelectedItems();
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

	private void clearFreeFormSelectedItems() {
		new Thread(new Runnable() {
			int cleared = 0;
			@Override
			public void run() {
				for (FreeFormListInfo ffli : mFilterItems.values()) {
					if (ffli.mSelected) {
						FilterItemDO filterItem = FilterItems.get(ffli.getFilterItemId());
						if (filterItem != null) {
							filterItem.delete();
						}
						cleared++;
						ffli.setMarkedForDeletion(false);
					}
				}
				mActivity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (cleared > 0) {
							ContactFreeFormListActivity.refreshFragment(mFragment);
						}
						AlertListArrayAdapter.toastIt(mActivity, cleared, "item", "cleared");
						clearAllActionMode();
					}
				});
			}
		}).start();
	}

	class CheckBoxOnClickListener implements CheckBox.OnClickListener {
		@Override
		public void onClick(View v) {
			// toggle isChecked
			ImageViewCheckBox cb = (ImageViewCheckBox)v;
			cb.setChecked(!cb.isChecked());

			FreeFormListInfo ffListInfo = getFreeFormListInfo(v);
			if (ffListInfo == null) {
			return;
		}

		// selectItemActionMode is a from-to
		// so we need to do the opposite of
		// what is checked/not checked here
		if (cb.isChecked()) {
			// will change in selectItemActionMode
			// to markedForDeletion = true;
			ffListInfo.setMarkedForDeletion(false);
		}
		else {
			ffListInfo.setMarkedForDeletion(true);
		}
		itemSelectedUnselectedActionMode(v);
		}
	}

		private void setListItemViewsActionMode(View v) {
			ViewHolder vh = getViewHolder(v);
			if (vh == null) {
				return;
			}
			Resources res = mActivity.getResources();
			int colorHoloBlueDark =	res.getColor(android.R.color.holo_blue_dark);
			int colorBackgroundLight = res.getColor(android.R.color.background_light);

			if (mActionMode == null) {
				if (vh.mCheckBox.isChecked()) {
					vh.mCheckBox.setChecked(false);
				}
//				if (vh.mCheckBox.getVisibility() == CheckBox.VISIBLE) {
//					vh.mCheckBox.setVisibility(CheckBox.GONE);
//				}
				vh.mLayout.setBackgroundColor(colorBackgroundLight);
				return;
			}

			// in ACTION MODE
			vh.mCheckBox.setVisibility(CheckBox.VISIBLE);

			FreeFormListInfo ffListInfo = null;
			FilterItemDO filterItem = (FilterItemDO) vh.mPhrase.getTag();
			if (filterItem != null) {
				ffListInfo = mFilterItems.get(filterItem.getFilterItemId());
			}

			if (ffListInfo == null) {
				return;
			}

			if (ffListInfo.getMarkedForDeletion()) {
				vh.mLayout.setBackgroundColor(colorHoloBlueDark);
				vh.mPhrase.setTextColor(colorBackgroundLight);
				vh.mCheckBox.setChecked(true);
			}
			else {
				vh.mLayout.setBackgroundColor(colorBackgroundLight);
				vh.mPhrase.setTextColor(colorHoloBlueDark);
				vh.mCheckBox.setChecked(false);
			}
		}

		private void clearAllActionModeViewsAndLists() {
			for (FreeFormListInfo cli : mFilterItems.values()) {
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
			for (FreeFormListInfo ffListInfo : mFilterItems.values()) {
				ffListInfo.setMarkedForDeletion(false);
			}

			clearAllActionModeViewsAndLists();

			if (mAdapter instanceof FreeFormListArrayAdapter) {
				((FreeFormListArrayAdapter)mAdapter).notifyDataSetChanged();
			}
		}

		private void actionModeSubTitleUpdate() {
			if (mActionModeItems == 0) {
				mActionMode.setSubtitle(null);
				clearAllActionMode();
			} else {
				mActionMode.setSubtitle("" + mActionModeItems + " Selected");
			}
		}
}
