package com.aldersonet.automatonalert.Widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.R;
import com.aldersonet.automatonalert.Receiver.AlertReceiver;
import com.aldersonet.automatonalert.Service.AutomatonAlertService;

import org.jetbrains.annotations.NotNull;


public class Widget3x1 extends AppWidgetProvider {

	public static RemoteViews updateViews3x1(Context context) {
		RemoteViews updateViews =
				new RemoteViews(
						context.getPackageName(), R.layout.widget_3x1);

		return setRemoteViewsPendingIntents3x1(context, updateViews);

	}

	@Override
	public void onUpdate(
			Context context, AppWidgetManager appWidgetManager,	int[] appWidgetIds) {
		if (AutomatonAlertService.mIAmActive) {
			super.onUpdate(context, appWidgetManager, appWidgetIds);
			Intent intent = new Intent(
					context,
					AlertReceiver.class);
			intent.setAction(AutomatonAlert.WIDGET_UPDATE_3X1);
			intent.setFlags(0);
			context.sendBroadcast(intent);
		}
	}

	@Override
	public void onEnabled(Context context) {
		if (AutomatonAlertService.mIAmActive) {
			super.onEnabled(context);
			Intent intent = new Intent(
					context,
					AlertReceiver.class);
			intent.setAction(AutomatonAlert.WIDGET_UPDATE_3X1);
			intent.setFlags(0);
			context.sendBroadcast(intent);
		}
	}

	@Override
	public void onReceive(@NotNull Context context, @NotNull Intent intent) {
		if (AutomatonAlertService.mIAmActive) {
			super.onReceive(context, intent);
		}
		else {
			//standard widget calls
			RemoteViews updateViews = buildWidgetUpdate3x1(context);
			// Push update for this widget to the home screen
			ComponentName thisWidget = new ComponentName(context, Widget3x1.class);
			AppWidgetManager manager = AppWidgetManager.getInstance(context);
			manager.updateAppWidget(thisWidget, updateViews);
		}
	}

	public static RemoteViews setRemoteViewsPendingIntents3x1(
			Context context, RemoteViews updateViews) {

		Intent intent = null;
		PendingIntent pendingIntent = null;

		intent = new Intent(context, AlertReceiver.class);
		intent.setAction(AutomatonAlert.WIDGET_ALL_SILENT_3X1);
		intent.setFlags(0);
		pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
		updateViews.setOnClickPendingIntent(R.id.w3x1_iv1, pendingIntent);

		intent = new Intent(context, AlertReceiver.class);
		intent.setAction(AutomatonAlert.WIDGET_PAUSE_ALERT_ALARM_3X1);
		intent.setFlags(0);
		pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
		updateViews.setOnClickPendingIntent(R.id.w3x1_iv2, pendingIntent);

		intent = new Intent(context, AlertReceiver.class);
		intent.setAction(AutomatonAlert.WIDGET_OVERRIDE_VOL_TOGGLE_2X1);
		intent.setFlags(0);
		pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
		updateViews.setOnClickPendingIntent(R.id.w3x1_iv3, pendingIntent);

//		intent = new Intent(context, AutomatonAlertActivity.class);
//		pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
//		updateViews.setOnClickPendingIntent(R.id.w3x1_ll_frame, pendingIntent);

		return updateViews;
	}

	private RemoteViews buildWidgetUpdate3x1(Context context) {

		RemoteViews updateViews =
				new RemoteViews(
						context.getPackageName(), R.layout.widget_3x1);

		return setRemoteViewsPendingIntents3x1(context, updateViews);

	}
}
