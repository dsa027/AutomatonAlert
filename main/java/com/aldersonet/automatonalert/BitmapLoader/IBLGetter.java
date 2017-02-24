package com.aldersonet.automatonalert.BitmapLoader;

import android.graphics.Bitmap;

public interface IBLGetter {
	void setBitmap(String lookupKey, Bitmap bitmap);
	BitmapLoader getBitmapLoader();
}
