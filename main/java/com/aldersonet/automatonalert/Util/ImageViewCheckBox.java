package com.aldersonet.automatonalert.Util;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.aldersonet.automatonalert.R;

public class ImageViewCheckBox extends ImageView {
	public ImageViewCheckBox(Context context) {
		super(context);
		init();
	}

	public ImageViewCheckBox(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public ImageViewCheckBox(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		setTag(0);
		setImageResource(R.drawable.ic_check_box_outline_blank_grey);
	}

	public boolean isChecked() {
		return (Integer) getTag() == 1;
	}

	public void setChecked(boolean checked) {
		setTag(checked ? 1 : 0);
		if (checked) {
			setImageResource(R.drawable.ic_check_box_grey);
		}
		else {
			setImageResource(R.drawable.ic_check_box_outline_blank_grey);
		}
	}
}

