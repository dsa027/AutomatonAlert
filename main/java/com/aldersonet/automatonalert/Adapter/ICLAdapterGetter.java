package com.aldersonet.automatonalert.Adapter;

import android.support.v4.app.Fragment;

public interface ICLAdapterGetter {
	Object getHelper();
	Fragment getFragment();
	void notifyDataSet();
}
