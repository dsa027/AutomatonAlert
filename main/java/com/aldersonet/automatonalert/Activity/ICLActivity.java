package com.aldersonet.automatonalert.Activity;

import android.support.v7.app.ActionBar;
import android.widget.BaseAdapter;

import com.aldersonet.automatonalert.Activity.ContactFreeFormListActivity.FragmentTypeCL;

import java.util.List;

public interface ICLActivity {
	List<BaseAdapter> getAdapterList();
	ActionBar getTheActionBar();
	void notifyAdapters(boolean[] doIts, FragmentTypeCL[] types);
	boolean isThisFragmentShowingNow(FragmentTypeCL inFragmentType);
	void stopActionModeAllFragments();
}
