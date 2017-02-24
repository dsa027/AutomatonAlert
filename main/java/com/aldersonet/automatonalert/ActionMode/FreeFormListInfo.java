package com.aldersonet.automatonalert.ActionMode;

import android.content.Context;

public class FreeFormListInfo {

	public static String TAG_MARKED_FOR_DELETION = "selected";

	private int mFilterItemId;
	public boolean mSelected;

	Context mContext;

	public FreeFormListInfo(Context context, int filterItemId, boolean selected) {
		mContext = context;
		mFilterItemId = filterItemId;
		mSelected = selected;
	}

	public boolean getMarkedForDeletion() {
		return mSelected;
	}

	public int getFilterItemId() {
		return mFilterItemId;
	}

	public int setFilterItemId(int filterItemId) {
		mFilterItemId = filterItemId;
		return mFilterItemId;
	}

	public void setMarkedForDeletion(Boolean selected) {
		mSelected = selected;
	}
}
