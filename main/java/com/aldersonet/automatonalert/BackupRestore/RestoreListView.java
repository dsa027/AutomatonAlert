package com.aldersonet.automatonalert.BackupRestore;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.ListView;

import org.jetbrains.annotations.NotNull;

public class RestoreListView extends ListView {
	Runnable mRunnable;
	public RestoreListView(Context context) {
		super(context);
	}

	public RestoreListView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public RestoreListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void setRunnable(Runnable runnable) {
		mRunnable = runnable;
	}

	@Override
	public void draw(@NotNull Canvas canvas) {
		super.draw(canvas);
		if (isInEditMode()) {
			return;
		}
		if (mRunnable != null) {
			mRunnable.run();
		}
	}

}
