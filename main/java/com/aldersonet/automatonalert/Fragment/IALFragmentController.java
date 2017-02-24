package com.aldersonet.automatonalert.Fragment;

import com.aldersonet.automatonalert.Activity.AlertListActivity.FragmentTypeAL;

public interface IALFragmentController {
	FragmentTypeAL getFragmentType();
	public void refreshData();
}
