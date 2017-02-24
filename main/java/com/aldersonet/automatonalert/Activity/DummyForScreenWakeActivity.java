package com.aldersonet.automatonalert.Activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;

public class DummyForScreenWakeActivity extends Activity {


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(android.R.style.Theme_Holo_Light_NoActionBar);
		Window window = getWindow();
		window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
		window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		super.onCreate(savedInstanceState);

		final Intent intent = getIntent();
		startAfter(intent);
	}

	private void startAfter(final Intent intent) {
		new Handler().postDelayed(new Runnable() {
			public void run() {
				finish();
				overridePendingTransition(0, 0);
				startAlarmVisualActivity(intent);
			}
		}, 500);
	}

	private void startAlarmVisualActivity(Intent intent) {
		intent.setClass(getApplicationContext(), AlarmVisualActivity.class);
		intent.setFlags(intent.getFlags() ^ Intent.FLAG_ACTIVITY_NO_ANIMATION);
		startActivity(intent);
		overridePendingTransition(0, 0);
	}
}
