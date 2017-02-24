package com.aldersonet.automatonalert.Fragment;

import android.widget.BaseAdapter;
import android.widget.ListView;

import com.aldersonet.automatonalert.Activity.ContactFreeFormListActivity.FragmentTypeCL;

public interface ICLFragmentController {
	FragmentTypeCL getFragmentType();
	void refreshData(boolean forced);
	void showTextForEmptyList();
	void stopActionModeAllFragments();
	String getInfo();
	BaseAdapter sendAdapterToActivity();
	BaseAdapter getAdapter();
	ListView getListView();
}
