package com.aldersonet.automatonalert.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Service.AutomatonAlertService;

public class BootReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = null;

        if (intent == null) {
            return;

        }
        action = intent.getAction();

		// start it all up...
		if (action.equalsIgnoreCase(
				AutomatonAlert.ANDROID_INTENT_ACTION_BOOT_COMPLETED)) {
			Intent serviceIntent = new Intent(
					context, AutomatonAlertService.class);
			context.startService(serviceIntent);
		}
	}
}
