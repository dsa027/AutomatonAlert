package com.aldersonet.automatonalert.Fragment;

import android.support.v7.app.ActionBar;
import android.util.Pair;
import android.widget.BaseAdapter;

import com.aldersonet.automatonalert.Activity.AlertListActivity.FragmentTypeAL;

import java.util.List;

public interface IALActivityController {
	List<BaseAdapter> getAdapterList();
	ActionBar getTheActionBar();
	void refreshFragments(boolean[] doIts, FragmentTypeAL[] types);
	void stopActionModeAllFragments();
	Pair<Integer, Integer> decodeRequestCode(int reqCode);
	int encodeRequestCode(int originalNumber, FragmentTypeAL type);
	void setActionBarTitle(BaseAdapter adapter);
}
