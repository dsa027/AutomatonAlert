package com.aldersonet.automatonalert.Widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.aldersonet.automatonalert.Activity.ContactFreeFormListActivity;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Preferences.GeneralPrefsDO;
import com.aldersonet.automatonalert.R;
import com.aldersonet.automatonalert.Receiver.AlertReceiver;
import com.aldersonet.automatonalert.Service.AutomatonAlertService;

import org.jetbrains.annotations.NotNull;


public class Widget2x1 extends AppWidgetProvider {

	@Override
	public void onUpdate(
			Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
		if (AutomatonAlertService.mIAmActive) {
			super.onUpdate(context, appWidgetManager, appWidgetIds);
			Intent intent = new Intent(
					context,
					AlertReceiver.class);
			intent.setAction(AutomatonAlert.WIDGET_UPDATE_2X1);
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
			intent.setAction(AutomatonAlert.WIDGET_UPDATE_2X1);
			intent.setFlags(0);
			context.sendBroadcast(intent);
		}
	}

	@Override
	public void onReceive(@NotNull Context context, @NotNull Intent intent) {
		super.onReceive(context, intent);
		if (AutomatonAlertService.mIAmActive) {
			//standard widget calls
			RemoteViews updateViews = buildWidgetUpdate2x1(context);
			// Push update for this widget to the home screen
			ComponentName thisWidget = new ComponentName(context, Widget2x1.class);
			AppWidgetManager manager = AppWidgetManager.getInstance(context);
			manager.updateAppWidget(thisWidget, updateViews);
		}
	}

	private RemoteViews buildWidgetUpdate2x1(Context context) {
		RemoteViews updateViews =
				new RemoteViews(context.getPackageName(), R.layout.widget_2x1);

		return setRemoteViewsPendingIntents2x1(context, updateViews);
	}

	public static RemoteViews setRemoteViewsPendingIntents2x1(
			Context context, RemoteViews updateViews) {

		Intent intent = null;
		PendingIntent pendingIntent = null;

		intent = new Intent(context, ContactFreeFormListActivity.class);
		intent.setFlags(0);
		pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
		updateViews.setOnClickPendingIntent(R.id.w2x1_iv1, pendingIntent);

		intent = new Intent(context, AlertReceiver.class);
		intent.setAction(AutomatonAlert.WIDGET_OVERRIDE_VOL_TOGGLE_2X1);
		intent.setFlags(0);
		pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
		updateViews.setOnClickPendingIntent(R.id.w2x1_iv3, pendingIntent);

		return updateViews;
	}

	public static RemoteViews updateViews2x1(Context context) {
		RemoteViews updateViews =
				new RemoteViews(
						context.getPackageName(), R.layout.widget_2x1);

		updateViews.setImageViewResource(
				R.id.w2x1_iv1, R.drawable.app_icon_blue_no_border_64);

		if (GeneralPrefsDO.getOverrideVol().equals(GeneralPrefsDO.OverrideVolLevel.DEFAULT.name())) {
			updateViews.setImageViewResource(
					R.id.w2x1_iv3, R.drawable.widget_volume_default_outline_64);
		}
		else if (GeneralPrefsDO.getOverrideVol().equals(GeneralPrefsDO.OverrideVolLevel.SILENT.name())) {
			updateViews.setImageViewResource(
					R.id.w2x1_iv3, R.drawable.widget_volume_silent_outline_64);
		}
		else if (GeneralPrefsDO.getOverrideVol().equals(GeneralPrefsDO.OverrideVolLevel.HI.name())) {
			updateViews.setImageViewResource(
					R.id.w2x1_iv3, R.drawable.widget_volume_hi_outline_64);
		}
		else if (GeneralPrefsDO.getOverrideVol().equals(GeneralPrefsDO.OverrideVolLevel.LOW.name())) {
			updateViews.setImageViewResource(
					R.id.w2x1_iv3, R.drawable.widget_volume_low_outline_64);
		}
		else if (GeneralPrefsDO.getOverrideVol().equals(GeneralPrefsDO.OverrideVolLevel.MED.name())) {
			updateViews.setImageViewResource(
					R.id.w2x1_iv3, R.drawable.widget_volume_med_outline_64);
		}

		return setRemoteViewsPendingIntents2x1(context, updateViews);
	}

}
