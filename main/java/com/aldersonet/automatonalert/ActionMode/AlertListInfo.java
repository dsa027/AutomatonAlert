package com.aldersonet.automatonalert.ActionMode;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;

import com.aldersonet.automatonalert.Util.Utils;

public class AlertListInfo {
	public boolean mSelected;

	Context mContext;
	String mLookupKey;
	String mDisplayName;
	Bitmap mPhoto;

	public AlertListInfo(
			Context context,
			String lookupKey,
			String displayName,
			boolean selected) {
		mContext = context;
		mLookupKey = lookupKey;
		mDisplayName = displayName;
		mSelected = selected;
		getData();
	}

	private void getData() {
		if (!TextUtils.isEmpty(mLookupKey)) {
			getPhotoBitmap();
		}
	}

	private void getPhotoBitmap() {
		Utils.getPhotoBitmap(mContext, mLookupKey);
	}

	public Bitmap getPhoto() {
		return mPhoto;
	}
	public String getLookupKey() {
		return mLookupKey;
	}
	public String getDisplayName() {
		return mDisplayName;
	}
	public boolean isSelected() {
		return mSelected;
	}
	public void setLookupKey(String lookupKey) {
		mLookupKey = lookupKey;
	}
	public void setDisplayName(String displayName) {
		mDisplayName = displayName;
	}
	public void setSelected(Boolean selected) {
		mSelected = selected;
	}
}
