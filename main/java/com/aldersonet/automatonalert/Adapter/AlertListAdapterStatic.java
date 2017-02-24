package com.aldersonet.automatonalert.Adapter;

import android.app.Activity;
import android.graphics.Bitmap;
import android.provider.ContactsContract.Contacts;
import android.support.v4.app.Fragment;
import android.text.Html;
import android.text.format.DateFormat;
import android.util.Pair;
import android.view.View;
import android.widget.CheckBox;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.QuickContactBadge;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aldersonet.automatonalert.Activity.AlarmVisualActivity;
import com.aldersonet.automatonalert.Activity.AlertListActivity.FragmentTypeAL;
import com.aldersonet.automatonalert.Adapter.AlertListArrayAdapter.ViewHolder;
import com.aldersonet.automatonalert.Alarm.AlarmPendingIntent;
import com.aldersonet.automatonalert.Alarm.AlarmPendingIntent.ApiType;
import com.aldersonet.automatonalert.Alarm.AlarmPendingIntent.ApiSubType;
import com.aldersonet.automatonalert.Alert.AlertItemDO;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Fragment.AlertListFragment.Mode;
import com.aldersonet.automatonalert.Fragment.IALFragmentController;
import com.aldersonet.automatonalert.PostAlarm.PostAlarmDO;
import com.aldersonet.automatonalert.R;
import com.aldersonet.automatonalert.ActionMode.AlertListInfo;
import com.aldersonet.automatonalert.Util.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class AlertListAdapterStatic {

	public static final int ALARM_FUTURE_COLOR = android.R.color.holo_blue_dark;
	public static final int ALARM_PAST_COLOR = android.R.color.holo_blue_dark;//red_dark;

	public static FragmentTypeAL getFragmentType(Fragment fragment, Activity activity) {
		if (fragment != null) {
			if (fragment instanceof IALFragmentController) {
				return ((IALFragmentController) fragment).getFragmentType();
			}
		}
		if (activity != null) {
			if (activity instanceof AlarmVisualActivity) {
				return ((AlarmVisualActivity)activity).mFragmentType;
			}
		}
		return FragmentTypeAL.SETTINGS;
	}

	static String getLookupKey(AlertItemDO alertItem) {
		if (alertItem == null) {
			return null;
		}
		return (alertItem.getKvRawDetails().get(Contacts.LOOKUP_KEY));
	}

	static String getDisplayName(AlertItemDO alertItem) {
		if (alertItem == null) {
			return null;
		}
		return (alertItem.getKvRawDetails().get(Contacts.DISPLAY_NAME));
	}

	static AlertItemDO setFakeAlertIfNeeded(AlertItemDO alertItem) {
		if (alertItem == null
				|| (alertItem.mKvRawDetails != null
				&& alertItem.mKvRawDetails.containsKey(AlertItemDO.FAKE))) {
			alertItem = new AlertItemDO();
			alertItem.setFakeAlertItem(0);
		}
		return alertItem;
	}

	static void setViewHolder(ViewHolder vh, View v) {
		vh.mRootLayoutWithVH = (RelativeLayout)v.findViewById(R.id.mltn_layout);
		vh.mDateReceived = (TextView)v.findViewById(R.id.mltn_date_received);
		vh.mTopRowIconAlarm = (ImageView)v.findViewById(R.id.mltn_top_row_icons_alarm);
		vh.mTopRowSourceWithAI = (ImageView)v.findViewById(R.id.mltn_top_row_icons_source);
		vh.mTopRowIconRepeat = (ImageView)v.findViewById(R.id.mltn_top_row_icons_repeat);
		vh.mTopRowIconSnooze = (ImageView)v.findViewById(R.id.mltn_top_row_icons_snooze);
		vh.mTextViewLine1 = (TextView)v.findViewById(R.id.mltn_textview_line_1);
		vh.mTextViewLine2 = (TextView)v.findViewById(R.id.mltn_textview_line_2);
		vh.mRepeatsLayout = (GridLayout)v.findViewById(R.id.mltn_repeats_layout);
		vh.mRepeatsEvery = (TextView)v.findViewById(R.id.mltn_repeats_every);
		vh.mRepeatsOrigHeader = (TextView)v.findViewById(R.id.mltn_repeats_orig_header);
		vh.mRepeatsNextHeader = (TextView)v.findViewById(R.id.mltn_repeats_next_header);
		vh.mRepeatsLastHeader = (TextView)v.findViewById(R.id.mltn_repeats_last_header);
		vh.mRepeatsNumberHeader = (TextView)v.findViewById(R.id.mltn_repeats_repeat_number_header);
		vh.mRepeatsOrigAlarm = (TextView)v.findViewById(R.id.mltn_repeats_orig);
		vh.mRepeatsNextAlarm = (TextView)v.findViewById(R.id.mltn_repeats_next);
		vh.mRepeatsLastAlarm = (TextView)v.findViewById(R.id.mltn_repeats_last);
		vh.mRepeatsNumber = (TextView)v.findViewById(R.id.mltn_repeats_repeat_number);
		vh.mTextViewRemainder = (TextView)v.findViewById(R.id.mltn_remainder);
		vh.mExpandCollapseWithALI = (ImageView)v.findViewById(R.id.mltn_expander);
		vh.mDelete = (ImageView)v.findViewById(R.id.mltn_delete);
		vh.mSave = (ImageView)v.findViewById(R.id.mltn_save);
		vh.mSeparator = (ImageView)v.findViewById(R.id.sawru_alarm_separator);
		vh.mAlarm = (ImageView)v.findViewById(R.id.mltn_alarm);
		vh.mAlarmDate = (TextView)v.findViewById(R.id.mltn_alarm_date);
		vh.mAlarmTime = (TextView)v.findViewById(R.id.mltn_alarm_time);
		vh.mAlarmFrame = v.findViewById(R.id.mltn_alarm_frame);
		vh.mGotoSourceWithSME = (ImageView)v.findViewById(R.id.mltn_gotoSource);
		vh.mContactWithLK = (QuickContactBadge)v.findViewById(R.id.mltn_contact);
		vh.mSpacer = v.findViewById(R.id.mltn_spacer);
		vh.mCheckBox = (CheckBox)v.findViewById(R.id.mltn_checkbox);
	}

	static void setGotoSourceView(ViewHolder vh, String source, boolean isSmsMms) {
		if (isSmsMms) {
			vh.mGotoSourceWithSME.setImageResource(
					R.drawable.android_messages_blue_64);
		}
		else {
			vh.mGotoSourceWithSME.setImageResource(
					R.drawable.android_email_blue_blue_64);
		}
		vh.mGotoSourceWithSME.setTag(source);
	}

	static void setContactBitmapView(
			ViewHolder vh, String lookupKey, Activity activity) {

		vh.mContactWithLK.setTag("");

		if (lookupKey != null) {
			Bitmap bitmap = Utils.getPhotoBitmap(activity, lookupKey);
			if (bitmap != null) {
				vh.mContactWithLK.setImageBitmap(bitmap);
				vh.mContactWithLK.setTag(lookupKey);
			}
			else {
				vh.mContactWithLK.setImageToDefault();
//						R.drawable.ic_launcher_automatonalert_40pct);
			}
		}
		else {
			vh.mContactWithLK.setImageToDefault();

		}
	}

	private static String formatSnooze(long snooze) {
		java.text.DateFormat df = null;
		String date = "";

		if (Utils.isToday(snooze)) {
			date = "Today @";
		}
		else if (Utils.isTomorrow(snooze)) {
			date = "Tomorrow @";
		}
		else if (Utils.isThisYear(snooze)) {
			df = Utils.getThisYearFormat(true/*showDayOfWeek*/);
			date = df.format(snooze) + " @";
		}
		else {
			df = Utils.getOutYearFormat(true/*showDayOfWeek*/);
			date = df.format(snooze) + " @";
		}
		return date;
	}

	private static String translateRepeatEvery(
			AlertItemDO alertItem, Activity activity) {

		// translate milliseconds to alarm_repeat_entries

		// first find the index into values
		String[] values =
				activity.getResources().getStringArray(R.array.alarm_repeat_values);
		String[] entries =
				activity.getResources().getStringArray(R.array.alarm_repeat_entries);
		long repeatEvery = alertItem.getRepeatEvery();
		String sRepeatEvery = null;
		String rep = repeatEvery + "";
		int N=values.length;
		int idx = 0;
		for (;idx<N;idx++) {
			if (values[idx].equals(rep)) {
				break;
			}
		}

		// shortcut
		// if found and "1 hour" or "1 minute" or "1 week"
		// just use that instead of translateMillis
		if (idx < N) {
			sRepeatEvery = entries[idx];
			String[] s = entries[idx].split(" ");
			if (s.length > 1) {
				if (s[0].equals("1")) {
					sRepeatEvery = s[1];
				}
			}

		}
		if (sRepeatEvery == null) {
			sRepeatEvery = Utils.translateMillis(repeatEvery, false/*shortTrans*/);
		}

		return sRepeatEvery;
	}

	private static String getIterationSlashStopAfter(
			AlertItemDO alertItem, long orig,
			long last, long now, boolean isForever) {

		long iteration = -1;

		// in between orig and next
		if (orig > now) {
			iteration = 0;
		}
		else if (last < now) {
			iteration = alertItem.getStopAfter();
		}
		else {
			iteration =
					AlertItemDO.getCurrentAlarmIteration(
							alertItem.getDateRemind(),
							alertItem.getRepeatEvery());
		}
		return
				"<font color='#0099cc'><b>"
				+ (iteration == -1 ? "-" : iteration)
				+ "</b></font>"
				+ " of "
				+ (isForever ? "forever" : alertItem.getStopAfter())
				;
	}

	static void setTextViewLine2(ViewHolder vh, int margin, int visibility) {
		vh.mTextViewLine2.setVisibility(visibility);
		vh.mTextViewLine2.setPadding(
				vh.mTextViewLine2.getPaddingLeft(),
				vh.mTextViewLine2.getPaddingTop(),
				vh.mTextViewLine2.getPaddingRight(),
				margin);
		vh.mTextViewLine2.requestLayout();
	}



	public static void setRepeatsTextViewLine2(
			AlertItemDO alertItem,
			ViewHolder vh,
			FragmentTypeAL type,
			Activity activity) {

		if (!(type.equals(FragmentTypeAL.REPEATS))) {
			return;
		}

		Date dOrig = alertItem.getDateRemind() == null ?
				new Date(0) : alertItem.getDateRemind();
		long orig = dOrig.getTime();
		long next = alertItem.getNextIteratedAlarm();
		long last = alertItem.getLastIteratedAlarm();
		long now = System.currentTimeMillis();
		long stopAfter = alertItem.getStopAfter();
		boolean isForever = stopAfter > 1000;

		String sIteration = getIterationSlashStopAfter(
				alertItem, orig, last, now, isForever);
		String sRepeatEvery = translateRepeatEvery(alertItem, activity);

		vh.mRepeatsEvery.setText(Html.fromHtml(sRepeatEvery));
		String sNext = null;
		if (next == -1) {
			sNext = "Completed";
		}
		else {
			sNext =
					Utils.getDateForButtonDisplay(next)
					+ " - "
					+ Utils.getTimeForButtonDisplay(next);
		}

		vh.mRepeatsNextAlarm.setText(Html.fromHtml(sNext));
		vh.mRepeatsOrigAlarm.setText(
				Utils.getDateForButtonDisplay(dOrig.getTime())
				+ " - "
				+ Utils.getTimeForButtonDisplay(dOrig.getTime()));
		vh.mRepeatsNumber.setText(Html.fromHtml(sIteration));
		if (isForever) {
			vh.mRepeatsLastHeader.setVisibility(TextView.GONE);
			vh.mRepeatsLastAlarm.setVisibility(TextView.GONE);
		}
		else {
			vh.mRepeatsLastHeader.setVisibility(TextView.VISIBLE);
			vh.mRepeatsLastAlarm.setVisibility(TextView.VISIBLE);
			vh.mRepeatsLastAlarm.setText(
				Utils.getDateForButtonDisplay(last)
				+ " - "
				+ Utils.getTimeForButtonDisplay(last));
		}
	}

	public static void setSnoozedTextViewLine2(
			AlertItemDO alertItem,
			ViewHolder vh,
			FragmentTypeAL type) {

		vh.mTextViewLine2.setLines(1);

		if (!(type.equals(FragmentTypeAL.SNOOZED))) {
			return;
		}

		// get snoozed api if there is one
		AlarmPendingIntent api = null;
		ArrayList<PostAlarmDO> postAlarms = null;

		if (alertItem != null) {
			ArrayList<AlarmPendingIntent> apis =
					AutomatonAlert.getAPIs().getAlarmPendingIntents(
							ApiType.ALERT,
							ApiSubType.SNOOZE,
							-1,
							alertItem.getAlertItemId(),
							alertItem.getNotificationItemId());
			if (!apis.isEmpty()) {
				api = apis.get(0);
			}
			// get from snooze db table
			postAlarms =
					PostAlarmDO.get(
							alertItem.getAlertItemId(), alertItem.getNotificationItemId());

		}

		long alarmTime = -1;
		String aiSummary = "";

		PostAlarmDO postAlarm = null;
		if (postAlarms != null
					&& postAlarms.size() > 0) {
			postAlarm = postAlarms.get(0);
			alarmTime = postAlarm.getNextAlarm();
		}
		else {
			if (api != null) {
				alarmTime = api.mAlarmTime;
			}
		}

		String snoozedDate = "";
		if (alarmTime > 0) {
			snoozedDate = formatSnooze(alarmTime);
			snoozedDate +=
					" "
					+ SimpleDateFormat.getTimeInstance(
							SimpleDateFormat.SHORT,
							Locale.getDefault())
					.format(alarmTime);
		}

		if (alertItem == null) {
			aiSummary =  "<b><u>Broken snooze. (AI null)";
		}
		else if (postAlarm == null) {
			aiSummary = "<b><u>Broken snooze. (PO null)";
		}
		else if (api == null) {
			aiSummary = "<b><u>Broken snooze. (API null)";
		}
		else /*if (api != null...found snoozed api above)*/ {
			aiSummary += "<b>" + snoozedDate + "</b>";
		}

		vh.mTextViewLine2.setText(Html.fromHtml(aiSummary));
	}

	static void setExpandCollapseImageView(ViewHolder vh, boolean isExpanded) {
//		if (isExpanded) {
//			vh.mExpandCollapseWithALI.setImageResource(
//					R.drawable.xxx_new_expander_close_holo_light);
//		}
//		else {
//			vh.mExpandCollapseWithALI.setImageResource(
//					R.drawable.xxx_new_expander_open_holo_light);
//		}
	}

	static void setRepeatsView(ViewHolder vh, int visibility, Activity activity) {
		vh.mRepeatsOrigHeader.setVisibility(visibility);
		vh.mRepeatsLastHeader.setVisibility(visibility);
		vh.mRepeatsNumberHeader.setVisibility(visibility);
		vh.mRepeatsOrigAlarm.setVisibility(visibility);
		vh.mRepeatsLastAlarm.setVisibility(visibility);
		vh.mRepeatsNumber.setVisibility(visibility);
		if (visibility == ImageView.GONE) {
            int bg = activity.getResources().getColor(android.R.color.transparent);
			vh.mRepeatsLayout.setBackgroundColor(bg);
		}
		else {
			vh.mRepeatsLayout.setBackgroundResource(R.drawable.list_border);
		}
	}

	static void setExpandedViews(
			ViewHolder vh, String lookupKey, String source,
			boolean isSmsMms, Activity activity, Mode mode) {

		setExpandCollapseImageView(vh, true);
		setTextViewLine2(vh, 0, View.GONE);
		setRepeatsView(vh, View.VISIBLE, activity);
		setContactBitmapView(vh, lookupKey, activity);
		setGotoSourceView(vh, source, isSmsMms);
		setAlarmView(vh, mode);
	}

	static void setCollapsedViews(
			ViewHolder vh, String lookupKey, Activity activity) {

		setExpandCollapseImageView(vh, false);
		setContactBitmapView(vh, lookupKey, activity);
		setTextViewLine2(vh, 8, View.VISIBLE);
		setRepeatsView(vh, View.GONE, activity);
	}

	static void setAlarmView(ViewHolder vh, Mode mode) {
		if (mode.equals(Mode.TRASH)) {
			vh.mAlarm.setVisibility(ImageView.GONE);
			vh.mAlarmDate.setVisibility(TextView.GONE);
			vh.mAlarmTime.setVisibility(TextView.GONE);
			vh.mAlarmFrame.setVisibility(View.GONE);
		}
	}

	public static String setSmsMmsSpecificFields(
			boolean[] isExpanded, int pos, AlertListArrayAdapter.ViewHolder vh,
			String displayName, String smsBody, String text) {

		vh.mTextViewLine1.setText(displayName);
		if (isExpanded[pos]) {
			vh.mTextViewLine2.setText(smsBody);
			CharSequence pattern = "<b>Subject: </b><br><br>";
			text = text.replace(pattern, "");
		}
		else {
			vh.mTextViewLine2.setText(smsBody);
		}
		return text;
	}

	static void setExpandedCollapsedViews(
			int pos, ViewHolder vh,	String lookupKey, String source,
			boolean isSmsMms, Activity activity, Mode mode,
			boolean[] isExpanded) {

		if (isExpanded[pos]) {
			setExpandedViews(vh, lookupKey, source, isSmsMms, activity, mode);
		}
		else {
			setCollapsedViews(vh, lookupKey, activity);
		}
	}

	static String setBody(String smsBody, AlertItemDO alertItem, boolean isSmsMms) {
		if (isSmsMms) {
			return(smsBody);
		}
		else {
			return(Utils.formatHeadersForView(alertItem.getKvRawDetails()));
		}
	}

	static void setNonSmsMmsSpecificFields(
			ViewHolder vh, String from, String subject) {

		String shortFrom = Utils.truncateEmailAddress(from);
		vh.mTextViewLine1.setText(shortFrom);
		vh.mTextViewLine2.setText(Html.fromHtml(subject==null?"":subject));
	}

	static void tagNewAlertListInfoIfNeeded(
			ViewHolder vh, String lookupKey, String displayName, Activity activity) {

		AlertListInfo ali = (AlertListInfo)vh.mExpandCollapseWithALI.getTag();
		if (ali == null) {
			ali = new AlertListInfo(
					activity, lookupKey, displayName, false/*not selected*/);
			vh.mExpandCollapseWithALI.setTag(ali);
		}
	}

	static void setImageViews(
			AlertItemDO alertItem, ViewHolder vh,
			Activity activity, Fragment fragment, Mode mode) {

		setTopRowIcons(alertItem, vh);
		setSavedImageView(alertItem, vh, mode);
		setSourceImageView(alertItem, vh);
		setAlarmImageView(alertItem, vh, activity, fragment);
	}

	static void setTopRowIcons(AlertItemDO alertItem, ViewHolder vh) {
		vh.mTopRowIconRepeat.setVisibility(ImageView.GONE);
		vh.mTopRowIconSnooze.setVisibility(ImageView.GONE);

		if (AlertItemDO.isSmsMms(alertItem.getKvRawDetails())) {
			vh.mTopRowSourceWithAI.setImageResource(R.drawable.android_messages_blue_48);
		}
		else {
			vh.mTopRowSourceWithAI.setImageResource(R.drawable.android_email_blue_blue_48);
		}
		if (alertItem.getDateRemind() != null) {
			vh.mTopRowIconAlarm.setVisibility(ImageView.VISIBLE);
			if (alertItem.getRepeatEvery() > 0
					&& alertItem.getStopAfter() > 0) {
				vh.mTopRowIconRepeat.setVisibility(ImageView.VISIBLE);
			}
			int aId = alertItem.getAlertItemId();
			int nId = alertItem.getNotificationItemId();
			if (aId > 0
					&& nId > 0) {
				if (PostAlarmDO.get(aId, nId).size() > 0) {
					vh.mTopRowIconSnooze.setVisibility(ImageView.VISIBLE);
				}
			}
		}
		else {
			vh.mTopRowIconAlarm.setVisibility(ImageView.GONE);
		}
	}

	static void setSavedImageView(
			final AlertItemDO alertItem, final ViewHolder vh, Mode mode) {

		if (mode == Mode.SAVED) {
			vh.mSave.setVisibility(ImageView.GONE);
		}
	}

	static void setSourceImageView(
			final AlertItemDO alertItem, final ViewHolder vh) {

		boolean isSmsMms = AlertItemDO.isSmsMms(alertItem.getKvRawDetails());
		if (isSmsMms) {
			vh.mTopRowSourceWithAI.setImageResource(R.drawable.android_messages_blue_48);
		}
		// email
		else {
			vh.mTopRowSourceWithAI.setImageResource(R.drawable.android_email_blue_blue_48);
		}

	}

	private static void setAlarmTextColorAndAlpha(
			ViewHolder vh, int color, boolean isFutureReminder) {

		vh.mAlarmDate.setTextColor(color);
		vh.mAlarmTime.setTextColor(color);
		if (!isFutureReminder) {
			vh.mAlarmDate.setAlpha(.35F);
			vh.mAlarmTime.setAlpha(.35F);
		}
		else {
			vh.mAlarmDate.setAlpha(1F);
			vh.mAlarmTime.setAlpha(1F);
		}
	}

	private static int getColorFromResource(Activity activity, int resource) {
		return activity.getResources().getColor(resource);
	}

	public static AlertItemDO stuffAlertItemForSnooze(AlertItemDO alertItem) {
		// stuff alertItem
		ArrayList<PostAlarmDO> postAlarms =
				PostAlarmDO.get(
						alertItem.getAlertItemId(),
						alertItem.getNotificationItemId());

		// try to get it from PostAlarmDO
		if (postAlarms.size() > 0) {
			alertItem.updateDateRemind(
					new Date(postAlarms.get(0).getNextAlarm()),
					false/*saveIt*/);
		}
		// if no po, try from AlarmPendingIntents
		else {
			ArrayList<AlarmPendingIntent> apis =
					AutomatonAlert.getAPIs().getAlarmPendingIntents(
							ApiType.ALERT,
							ApiSubType.SNOOZE,
							-1,
							alertItem.getAlertItemId(),
							alertItem.getNotificationItemId());
			if (!apis.isEmpty()) {
				AlarmPendingIntent api = apis.get(0);
				alertItem.updateDateRemind(new Date(api.mAlarmTime), false/*saveIt*/);
			}
		}
		return alertItem;
	}

	private static void enableDisableAlarmImageViews(
			ViewHolder vh, Fragment fragment) {

		if (fragment instanceof IALFragmentController
				&& ((IALFragmentController)fragment)
					.getFragmentType().equals(FragmentTypeAL.TRASH)) {
			if (vh.mAlarm.getVisibility() == ImageView.VISIBLE) {
				vh.mAlarm.setVisibility(ImageView.INVISIBLE);
				vh.mAlarmDate.setVisibility(TextView.INVISIBLE);
				vh.mAlarmTime.setVisibility(TextView.INVISIBLE);
				vh.mAlarm.setEnabled(false);
				vh.mAlarmDate.setEnabled(false);
				vh.mAlarmTime.setEnabled(false);
				vh.mAlarmFrame.setEnabled(false);
			}
		}
		else {
			if (vh.mAlarm.getVisibility() == ImageView.INVISIBLE) {
				vh.mAlarm.setVisibility(ImageView.VISIBLE);
				vh.mAlarmDate.setVisibility(TextView.VISIBLE);
				vh.mAlarmTime.setVisibility(TextView.VISIBLE);
				vh.mAlarm.setEnabled(true);
				vh.mAlarmDate.setEnabled(true);
				vh.mAlarmTime.setEnabled(true);
				vh.mAlarmFrame.setEnabled(true);
			}
		}
	}

	private static void setAlarmImageView(
			final AlertItemDO ai, final ViewHolder vh,
			Activity activity, Fragment fragment) {

		boolean isFutureReminder = true;

		enableDisableAlarmImageViews(vh, fragment);

		// use copy
		AlertItemDO alertItem = new AlertItemDO(ai);
		// we need to copy the id also
		alertItem.setAlertItemId(ai.getAlertItemId());

		// SNOOZE - stuff snooze into dateRemind
		if (getFragmentType(fragment, activity).equals(FragmentTypeAL.SNOOZED)) {
			alertItem = stuffAlertItemForSnooze(alertItem);
		}

		// get date and time strings
		String alarmDate = "";
		String alarmTime = "";
		if (alertItem.getDateRemind() != null) {
			alarmDate =
					DateFormat.getDateFormat(activity).format(
							alertItem.getDateRemind());
			alarmTime =
					DateFormat.getTimeFormat(activity).format(
							alertItem.getDateRemind());
		}

		// SNOOZE - format alarmDate and alarmTime
		if (getFragmentType(fragment, activity).equals(FragmentTypeAL.SNOOZED)) {
			alarmDate = "<u>" + alarmDate + "</u>";
			alarmTime = "<u>" + alarmTime + "</u>";
		}

		// default date/time color: blue
		int color = getColorFromResource(activity, ALARM_FUTURE_COLOR);

		if (alertItem.getDateRemind() != null) {
			// show alarm date and time
			vh.mAlarmDate.setText(Html.fromHtml(alarmDate));
			vh.mAlarmTime.setText(Html.fromHtml(alarmTime));

			// color date/time red
			if (alertItem.getDateRemind().before(new Date(System.currentTimeMillis()))) {
				isFutureReminder = false;
				color = getColorFromResource(activity, ALARM_PAST_COLOR);
			}
		}
		else {
			// set "Reminder"
			vh.mAlarmTime.setText(AlertListArrayAdapter.ALARM_REMINDER_LABEL);
			vh.mAlarmDate.setText("");
		}

		setAlarmTextColorAndAlpha(vh, color, isFutureReminder);
	}

	static boolean[] addSlotToBooleanArray(boolean[] array, int idx) {
		int newLength = array.length + 1;
		boolean[] newArray = new boolean[newLength];
		for (int i=0; i<newLength;i++) {
			if (i < idx) {
				newArray[i] = array[i];
			}
			else if (i == idx) {
				newArray[i] = false;
			}
			else if (i > idx) {
				newArray[i] = array[i-1];
			}
		}

		return newArray;
	}

	static boolean[] deleteSlotFromBooleanArray(boolean[] array, int idx) {
		int newLength = array.length - 1;
		boolean[] newArray = new boolean[newLength];
		for (int i=0; i<newLength;i++) {
			if (i<idx) {
				newArray[i] = array[i];
			}
			else if (i>=idx) {
				newArray[i] = array[i+1];
			}
		}

		return newArray;
	}

	static Pair<Object, View> getViewTag(int id, View startView) {
		// find the requested view by traversing up
		// the parent tree until it's found or
		// we've hit the root or gone up too much
		int idx = 0;
		View targetView = null;
		View rootView = startView.getRootView();
		View leafView = startView;
		boolean found = false;
		do {
			if (leafView == null) {
				break;
			}
			if (leafView.findViewById(id) != null) {
				targetView = leafView.findViewById(id);
				found = true;
				break;
			}
			leafView = (View)leafView.getParent();
		} while (leafView != rootView && ++idx <= 20);

		if (!found) {
			return Pair.create(null, startView);
		}
		return Pair.create(targetView.getTag(), leafView);
	}

	public static Pair<AlertItemDO, View> getAlertItem(View startView) {
		Pair<Object, View> pair = getViewTag(R.id.mltn_top_row_icons_source, startView);
		return new Pair<AlertItemDO, View>((AlertItemDO)pair.first, pair.second);
	}

	public static Pair<ViewHolder, View> getViewHolder(View startView) {
		Pair<Object, View> pair = getViewTag(R.id.mltn_layout, startView);
		return new Pair<ViewHolder, View>((ViewHolder)pair.first, pair.second);
	}

	public static Pair<AlertListInfo, View> getAlertListInfo(View startView) {
		Pair<Object, View> pair = getViewTag(R.id.mltn_expander, startView);
		return new Pair<AlertListInfo, View>((AlertListInfo)pair.first, pair.second);
	}
}
