package com.aldersonet.automatonalert.Adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.text.Html;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.QuickContactBadge;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aldersonet.automatonalert.ActionBar.ProgressBar;
import com.aldersonet.automatonalert.ActionMode.AlertListInfo;
import com.aldersonet.automatonalert.Activity.AlarmVisualActivity;
import com.aldersonet.automatonalert.Activity.AlertListActivity;
import com.aldersonet.automatonalert.Activity.AlertListActivity.FragmentTypeAL;
import com.aldersonet.automatonalert.Activity.RTUpdateActivity.FragmentTypeRT;
import com.aldersonet.automatonalert.Activity.SetAlertActivity;
import com.aldersonet.automatonalert.Alarm.AlarmPendingIntent.ApiSubType;
import com.aldersonet.automatonalert.Alarm.AlarmPendingIntent.ApiType;
import com.aldersonet.automatonalert.Alert.AlertItemDO;
import com.aldersonet.automatonalert.Alert.AlertItemDO.Status;
import com.aldersonet.automatonalert.Alert.AlertItems;
import com.aldersonet.automatonalert.Alert.ContactAlert;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Fragment.AlertListFragment;
import com.aldersonet.automatonalert.Fragment.AlertListFragment.Mode;
import com.aldersonet.automatonalert.Fragment.IALActivityController;
import com.aldersonet.automatonalert.Fragment.IProgressBarListener;
import com.aldersonet.automatonalert.OkCancel.OkCancel;
import com.aldersonet.automatonalert.OkCancel.OkCancelDialog;
import com.aldersonet.automatonalert.OkCancel.OkCancelDialog.EWI;
import com.aldersonet.automatonalert.Picker.DatePickerTimePicker;
import com.aldersonet.automatonalert.Preferences.GeneralPrefsDO;
import com.aldersonet.automatonalert.Preferences.NameValueDataDO;
import com.aldersonet.automatonalert.R;
import com.aldersonet.automatonalert.SMS.AccountSmsDO;
import com.aldersonet.automatonalert.Util.OnItemRemovedListener;
import com.aldersonet.automatonalert.Util.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

public class AlertListArrayAdapter extends ArrayAdapter<AlertItemDO>
		implements
		DatePickerTimePicker.IDPTPListener,
		IProgressBarListener {

	public static final String TAG = "AlertListArrayAdapter";

	static FragmentTypeAL[] mAlarmTypes = {
		FragmentTypeAL.ALARMS,
		FragmentTypeAL.SNOOZED,
		FragmentTypeAL.REPEATS
	};

	public Mode mMode;

	public ActionMode mActionMode;
	private int mActionModeItems = 0;
	ProgressBar mProgressBar =
			ProgressBar.getInstance();
	long mProgressBarStartKey = Integer.MAX_VALUE;

//	private boolean mIsThisATest;
	private int mTextViewId;
	private Activity mActivity;
	public AlertListFragment mFragment;
	private OnItemRemovedListener mOnItemRemovedListener;

	volatile private List<AlertItemDO> mShowList;
	volatile private boolean[] mIsExpanded;
	volatile private HashMap<AlertItemDO, AlertListInfo> mAlertListInfos;
	DatePickerTimePicker mPicker;
	AlertItemDO mAlertItem;
	OkCancelDialog mOkCancelDialog;

	public static final String ALARM_REMINDER_LABEL = "Set Reminder";
	public static final String TAG_ALERT_NAME = "mAlertName";

	private static int mHoloBlueDarkColor;

	ExpandCollapseOnClickListener mExpandCollapseOnClickListener;
	DeleteOnClickListener mDeleteOnClickListener;
	SaveOnClickListener mSaveOnClickListener;
	AlarmOnClickListener mAlarmOnClickListener;
	ContactOnClickListener mContactOnClickListener;
	GotoSourceOnClickListener mGotoSourceOnClickListener;
	ActionModeListItemLongClickListener mActionModeListItemLongClickListener;
	CheckBoxOnClickListener mCheckBoxOnClickListener;

	public AlertListArrayAdapter(
			Activity activity,
			AlertListFragment fragment,
			int textViewId,
			List<AlertItemDO> showList,
			Mode status/*,
			String sIsThisATest*/) {

		super(activity, textViewId, showList);

		mActivity = activity;

		if (mActivity instanceof AlertListActivity) {
			mProgressBar =
					ProgressBar.getInstance();
		} else {
			mProgressBar = null;
		}
		mTextViewId = textViewId;
		mShowList = showList;
		if (mShowList == null) {
			mShowList = new ArrayList<AlertItemDO>();
		}
		initIsExpanded();
		mAlertListInfos = new HashMap<AlertItemDO, AlertListInfo>();
		mMode = status;
		mFragment = fragment;
		addListeners();
		setNotifyOnChange(true);

		mHoloBlueDarkColor =
				mActivity.getResources().getColor(android.R.color.holo_blue_dark);
	}

	@Override
	public void startProgressBar() {
		mProgressBarStartKey = ProgressBar.startProgressBar(
				mProgressBar,
				mProgressBarStartKey,
				new ProgressBar.StartObject(
						(AppCompatActivity) mActivity,
						null,//(BaseAdapter)this,
						((IALActivityController) mActivity).getAdapterList()
				));
	}

	public void stopProgressBar() {
		mProgressBarStartKey =
				ProgressBar.stopProgressBar(mProgressBar, mProgressBarStartKey);
	}

	public void refreshShowList(List<AlertItemDO> list) {
		// only reset mIsExpanded if size has changed
		boolean[] holdIsExpanded = null;
		if (list.size() == mShowList.size()) {
			holdIsExpanded = Arrays.copyOf(mIsExpanded, mIsExpanded.length);
		}

		// in case list is mShowList
		List<AlertItemDO> theList = list;

		if (theList == mShowList) {
			theList = new ArrayList<AlertItemDO>(list);
		}

		this.clear();
		this.addAll(theList);

		// reinstate if held
		if (holdIsExpanded != null) {
			mIsExpanded = Arrays.copyOf(holdIsExpanded, holdIsExpanded.length);
		}
		else {
			initIsExpanded();
		}
	}

	private void initIsExpanded() {
		mIsExpanded = new boolean[mShowList.size()];
		Arrays.fill(mIsExpanded, true);
	}

	private FragmentTypeAL getFragmentType() {
		return AlertListAdapterStatic.getFragmentType(mFragment, mActivity);
	}

	private ListView getListView() {
		if (mFragment != null) {
			return mFragment.getListView();
		}
		if (mActivity != null) {
			if (mActivity instanceof AlarmVisualActivity) {
				return ((AlarmVisualActivity)mActivity).getListView();
			}
		}
		return null;
	}

	private void addListeners() {
		mExpandCollapseOnClickListener = new ExpandCollapseOnClickListener();
		mDeleteOnClickListener = new DeleteOnClickListener();
		mSaveOnClickListener = new SaveOnClickListener();
		mAlarmOnClickListener = new AlarmOnClickListener();
		mContactOnClickListener = new ContactOnClickListener();
		mGotoSourceOnClickListener = new GotoSourceOnClickListener();
		mActionModeListItemLongClickListener = new ActionModeListItemLongClickListener();
		mCheckBoxOnClickListener = new CheckBoxOnClickListener();
	}

	public static class ViewHolder {
		RelativeLayout mRootLayoutWithVH;
		TextView mDateReceived;
		ImageView mTopRowIconAlarm;
		ImageView mTopRowSourceWithAI;
		ImageView mTopRowIconRepeat;
		ImageView mTopRowIconSnooze;
		TextView mTextViewLine1;
		TextView mTextViewLine2;
		GridLayout mRepeatsLayout;
		TextView mRepeatsEvery;
		TextView mRepeatsOrigHeader;
		TextView mRepeatsNextHeader;
		TextView mRepeatsLastHeader;
		TextView mRepeatsNumberHeader;
		TextView mRepeatsOrigAlarm;
		TextView mRepeatsNextAlarm;
		TextView mRepeatsLastAlarm;
		TextView mRepeatsNumber;
		TextView mTextViewRemainder;
		ImageView mExpandCollapseWithALI;
		ImageView mDelete;
		ImageView mSave;
		ImageView mSeparator;
		ImageView mAlarm;
		TextView mAlarmDate;
		TextView mAlarmTime;
		View mAlarmFrame;
		ImageView mGotoSourceWithSME;
		QuickContactBadge mContactWithLK;
		View mSpacer;
		CheckBox mCheckBox;
	}

	@Override
	public final View getView(
			final int position, View convertView, final ViewGroup parent) {

		///////////////////////////////////////
		// tags:
		// 		convertView 			ViewHolder
		//		mTopRowSourceWithAI 			AlertItem
		//		mExpandCollapseWithALI	AlertListInfo
		//		mGotoSourceWithSME		"SMS", "MMS", "EMAIL"
		//		mContactWithLK			lookupKey
		///////////////////////////////////////

		ViewHolder viewHolder;

		if (convertView == null) {
			convertView = ((LayoutInflater) mActivity
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
					.inflate(mTextViewId, null);

			viewHolder = new ViewHolder();
			AlertListAdapterStatic.setViewHolder(viewHolder, convertView);
			setListeners(viewHolder);
			convertView.setTag(viewHolder);
		}
		else {
			viewHolder = (ViewHolder)convertView.getTag();
		}

		expandCollapseItem(position, viewHolder);
		AlertItemDO alertItem = getItem(position);
		setTextViews(alertItem, viewHolder, position, mActivity);
		alertItem = AlertListAdapterStatic.setFakeAlertIfNeeded(alertItem);
		AlertListAdapterStatic.setImageViews(
				alertItem, viewHolder, mActivity, mFragment, mMode);
		viewHolder.mTopRowSourceWithAI.setTag(alertItem);

		AlertListInfo alertListInfo = mAlertListInfos.get(alertItem);
		if (alertListInfo == null) {
			alertListInfo = createAlertListInfo(alertItem, false/*selected*/);
			mAlertListInfos.put(alertItem, alertListInfo);
		}
		viewHolder.mExpandCollapseWithALI.setTag(alertListInfo);

		setListItemViewsActionMode(convertView);

		setTextViewLine2View(viewHolder);

		return convertView;
	}

	private void setLongClickListeners(ViewHolder viewHolder) {
		viewHolder.mRootLayoutWithVH.setOnLongClickListener(mActionModeListItemLongClickListener);
		viewHolder.mDelete.setOnLongClickListener(mActionModeListItemLongClickListener);
		viewHolder.mSave.setOnLongClickListener(mActionModeListItemLongClickListener);
		viewHolder.mAlarm.setOnLongClickListener(mActionModeListItemLongClickListener);
		viewHolder.mAlarmDate.setOnLongClickListener(mActionModeListItemLongClickListener);
		viewHolder.mAlarmTime.setOnLongClickListener(mActionModeListItemLongClickListener);
		viewHolder.mAlarmFrame.setOnLongClickListener(mActionModeListItemLongClickListener);
		viewHolder.mContactWithLK.setOnLongClickListener(mActionModeListItemLongClickListener);
		viewHolder.mGotoSourceWithSME.setOnLongClickListener(mActionModeListItemLongClickListener);
		viewHolder.mTextViewLine1.setOnLongClickListener(mActionModeListItemLongClickListener);
		viewHolder.mTextViewLine2.setOnLongClickListener(mActionModeListItemLongClickListener);
		viewHolder.mRepeatsLayout.setOnLongClickListener(mActionModeListItemLongClickListener);
		viewHolder.mTextViewRemainder.setOnLongClickListener(mActionModeListItemLongClickListener);
	}

	private void setListeners(ViewHolder viewHolder) {
		// showing the single list item in AlarmVisualActivity...
		// no clicking!
		if (mActivity != null
				&& mActivity instanceof AlarmVisualActivity) {
			return;
		}
		setLongClickListeners(viewHolder);

		viewHolder.mRootLayoutWithVH.setOnClickListener(mExpandCollapseOnClickListener);
		viewHolder.mTextViewLine1.setOnClickListener(mExpandCollapseOnClickListener);
		viewHolder.mTextViewLine2.setOnClickListener(mExpandCollapseOnClickListener);
		viewHolder.mRepeatsLayout.setOnClickListener(mExpandCollapseOnClickListener);
		viewHolder.mTextViewRemainder.setOnClickListener(mExpandCollapseOnClickListener);
		viewHolder.mSeparator.setOnClickListener(mExpandCollapseOnClickListener);
		viewHolder.mTopRowIconAlarm.setOnClickListener(mExpandCollapseOnClickListener);
		viewHolder.mTopRowSourceWithAI.setOnClickListener(mExpandCollapseOnClickListener);
		viewHolder.mSpacer.setOnClickListener(mExpandCollapseOnClickListener);
		viewHolder.mDateReceived.setOnClickListener(mExpandCollapseOnClickListener);

		viewHolder.mDelete.setOnClickListener(mDeleteOnClickListener);
		viewHolder.mSave.setOnClickListener(mSaveOnClickListener);
		viewHolder.mAlarm.setOnClickListener(mAlarmOnClickListener);
		viewHolder.mAlarmDate.setOnClickListener(mAlarmOnClickListener);
		viewHolder.mAlarmTime.setOnClickListener(mAlarmOnClickListener);
		viewHolder.mAlarmFrame.setOnClickListener(mAlarmOnClickListener);
		viewHolder.mContactWithLK.setOnClickListener(mContactOnClickListener);
		viewHolder.mGotoSourceWithSME.setOnClickListener(mGotoSourceOnClickListener);
		viewHolder.mCheckBox.setOnClickListener(mCheckBoxOnClickListener);
	}

	// SNOOZED
	private void setTextViewLine2View(ViewHolder vh) {
		FragmentTypeAL type = getFragmentType();
		vh.mRepeatsLayout.setVisibility(GridLayout.GONE);
		RelativeLayout.LayoutParams rl =
				(RelativeLayout.LayoutParams)vh.mTextViewRemainder.getLayoutParams();

		if (type.equals(FragmentTypeAL.SNOOZED)) {
			vh.mTextViewLine2.setVisibility(TextView.VISIBLE);
			vh.mTextViewLine2.setTextColor(mHoloBlueDarkColor);
			rl.addRule(RelativeLayout.BELOW, R.id.mltn_textview_line_2);
		}
		else if (type.equals(FragmentTypeAL.REPEATS)) {
			vh.mTextViewLine2.setVisibility(TextView.GONE);
			vh.mRepeatsLayout.setVisibility(TextView.VISIBLE);
			rl.addRule(RelativeLayout.BELOW, R.id.mltn_repeats_layout);
		}
	}

	private void setTextViews(
			AlertItemDO alertItem, ViewHolder vh, int pos, Activity activity) {

		String date = alertItem.getKvRawDetails().get(AutomatonAlert.DATE);
		String from = alertItem.getKvRawDetails().get(AutomatonAlert.FROM);
		String subject = alertItem.getKvRawDetails().get(AutomatonAlert.SUBJECT);
		String source = alertItem.getKvRawDetails().get(ContactAlert.TAG_MESSAGE_SOURCE_HEADER);
		String smsBody = alertItem.getKvRawDetails().get(AutomatonAlert.SMS_BODY);
		String displayName = alertItem.getKvRawDetails().get(Contacts.DISPLAY_NAME);
		String lookupKey = alertItem.getKvRawDetails().get(Contacts.LOOKUP_KEY);
		displayName = (displayName == null) ? "" : displayName;
		AlertListAdapterStatic.tagNewAlertListInfoIfNeeded(
				vh, lookupKey, displayName, activity);
		String text = "";

		//
		boolean isSmsMms = AlertItemDO.isSmsMms(alertItem.getKvRawDetails());
		AlertListAdapterStatic.setExpandedCollapsedViews(
				pos, vh, lookupKey, source, isSmsMms, activity, mMode, mIsExpanded);
		text = AlertListAdapterStatic.setBody(smsBody, alertItem, isSmsMms);

		// get rid of markup
		while (text.endsWith("<br>")) {
			text = text.substring(0, (text.length() - "<br>".length()));
		}

		// sms/mms
		if (isSmsMms) {
			text = AlertListAdapterStatic.setSmsMmsSpecificFields(
					mIsExpanded, pos, vh, displayName, smsBody, text);
		}
		// other
		else {
			AlertListAdapterStatic.setNonSmsMmsSpecificFields(vh, from, subject);
		}

		if (GeneralPrefsDO.isDebugMode()) {
			text += "<br><i>a[" + alertItem.getAlertItemId()
					+ "] n[" + alertItem.getNotificationItemId()
					+ "] u[" + (alertItem.getUid().contains("Contact") ? "c" : alertItem.getUid())
					+ "] [" + Utils.smallDate(alertItem.getTimeStamp().getTime())
					+ "]</i>"
			;
		}

		// text
		vh.mTextViewRemainder.setText(Html.fromHtml(text));
		AlertListAdapterStatic.setSnoozedTextViewLine2(alertItem, vh, getFragmentType());
		AlertListAdapterStatic.setRepeatsTextViewLine2(alertItem, vh, getFragmentType(), activity);

		// if 'date' NAN: print it; else smallDate() it
		long lDate = Utils.getLong(date, -1);
		vh.mDateReceived.setText((lDate == -1) ? date : Utils.smallDate(lDate));
	}

	public void setOnItemRemoved(OnItemRemovedListener onItemRemoved) {
		mOnItemRemovedListener = onItemRemoved;
	}

	public void expandAll() {
		Arrays.fill(mIsExpanded, true);
	}

	public void collapseAll() {
		Arrays.fill(mIsExpanded, false);
	}

	private void expandCollapseItem(int position, ViewHolder vh) {
		if (mIsExpanded[position]) {
			expandItem(vh);
		}
		else {
			collapseItem(vh);
		}
	}

	private void setVisibilityExpandedCollapsedView(int visibility, ViewHolder vh) {
		// General
		vh.mTextViewRemainder.setVisibility(visibility);
		vh.mAlarm.setVisibility(visibility);
		vh.mAlarmDate.setVisibility(visibility);
		vh.mAlarmTime.setVisibility(visibility);
		vh.mAlarmFrame.setVisibility(visibility);
		vh.mGotoSourceWithSME.setVisibility(visibility);
		vh.mSeparator.setVisibility(visibility);
		vh.mSave.setVisibility(visibility);
		vh.mSpacer.setVisibility(visibility);

		// Specific

		// No SAVE button
		FragmentTypeAL type = getFragmentType();
		if (type.equals(FragmentTypeAL.SAVED)) {
			vh.mSave.setVisibility(ImageView.GONE);
		}
		// CLEAR (eraser) instead of TRASH and
		// TRASH instead of SAVE
		if (type.equals(FragmentTypeAL.ALARMS)
				||type.equals(FragmentTypeAL.SNOOZED)
				|| type.equals(FragmentTypeAL.REPEATS)) {
			vh.mDelete.setImageResource(R.drawable.delete_bin_app_blue);
			vh.mSave.setImageResource(R.drawable.www_iconbeast_com_eraser_blue);
		}

	}
	private void collapseItem(ViewHolder vh) {
		setVisibilityExpandedCollapsedView(View.GONE, vh);
		if (vh.mCheckBox.getVisibility() == CheckBox.VISIBLE) {
			vh.mCheckBox.setVisibility(CheckBox.GONE);
		}
	}

	private void expandItem(ViewHolder vh) {
		setVisibilityExpandedCollapsedView(View.VISIBLE, vh);
		if (vh.mCheckBox.getVisibility() == CheckBox.GONE) {
			vh.mCheckBox.setVisibility(CheckBox.VISIBLE);
		}
	}

	private void doExpandCollapse(View v) {
		Pair<AlertItemDO, View> pair = AlertListAdapterStatic.getAlertItem(v);
		int pos = -1;
		if (pair.first != null) {
			AlertItemDO alertItem = pair.first;
			pos = getPosition(alertItem);
			if (pos != -1) {
				if (mIsExpanded[pos]) {
					mIsExpanded[pos] = false;
					collapseItem(AlertListAdapterStatic.getViewHolder(v).first);
				}
				else {
					mIsExpanded[pos] = true;
					expandItem(AlertListAdapterStatic.getViewHolder(v).first);
				}
				notifyDataSetChanged();
			}
		}
	}

	public class ExpandCollapseOnClickListener implements ImageView.OnClickListener {
		@Override
		public void onClick(View v) {
			doExpandCollapse(v);
		}
	}

	@Override
	public void insert(AlertItemDO alertItem, int index) {
		super.insert(alertItem, index);
		// expand array by one at index and init to false
		mIsExpanded = AlertListAdapterStatic.addSlotToBooleanArray(mIsExpanded, index);
		mAlertListInfos.put(
				alertItem,
				new AlertListInfo(mActivity, "", "", false));
	}

	private void removeFromView(int pos, AlertItemDO alertItem) {
		if (pos < 0
				|| pos >= getCount()) {
			pos = getPosition(alertItem);
			if (pos == -1) {
				return;
			}
		}
		this.remove(alertItem);
		mIsExpanded = AlertListAdapterStatic.deleteSlotFromBooleanArray(mIsExpanded, pos);
	}

	private void removeListItem(AlertItemDO alertItem) {
		int idx = getPosition(alertItem);
		if (idx != -1) {
			removeFromView(idx, alertItem);
			notifyDataSetChanged();
		}
	}

	private void moveToTrash(AlertItemDO alertItem) {
		alertItem.updateFavorite(false);
		alertItem.updateStatus(Status.TRASH);
		removeListItem(alertItem);
		mAlertListInfos.remove(alertItem);
	}

	private void deleteFromDb(AlertItemDO alertItem) {
		alertItem.delete();
		// alarms - remove ALL (this is an ALARM)
		alertItem.findCancelRemovePendingIntentsPostAlarms(ApiSubType.ALARM);
		removeListItem(alertItem);
		// let object be used to re-insert (e.g. undo)
		alertItem.setAccountId(-1);
		mAlertListInfos.remove(alertItem);
	}

	private FragmentTypeAL getFragmentTypeAL(AlertItemDO alertItem) {
		if (alertItem == null) {
			return null;
		}
		switch(alertItem.getStatus()) {
		case NEW:
			return FragmentTypeAL.NEW;
		case SAVED:
			return FragmentTypeAL.SAVED;
		case TRASH:
			return FragmentTypeAL.TRASH;
		default:
			return null;
		}
	}

	private FragmentTypeAL[] getRefreshTypes(AlertItemDO alertItem) {
		// figure out which lists to refresh
		FragmentTypeAL[] types = {FragmentTypeAL.TRASH};
		FragmentTypeAL type = getFragmentTypeAL(alertItem);

		if (type != null
				&& !type.equals(FragmentTypeAL.TRASH)) {
			types =
					new FragmentTypeAL[] {
							FragmentTypeAL.TRASH,
							type
			};
		}

		return types;
	}

	private OkCancel getDeleteOkCancel(final AlertItemDO alertItem) {
		return new OkCancel() {
			// DELETE //
			@Override
			protected  void ok(DialogInterface dialog) {
				// get types before deleting/trashing
				FragmentTypeAL[] types = getRefreshTypes(alertItem);
				boolean isToTrash = true;

				// toggle
				if (alertItem != null) {
					if (alertItem.getStatus().equals(Status.TRASH)) {
						// already in .TRASH, delete from db
						deleteFromDb(alertItem);
						isToTrash = false;
					}
					else {
						// change its status
						moveToTrash(alertItem);
						isToTrash = true;
					}
					Utils.toastIt(mActivity, isToTrash ? "Trashed." : "Deleted.");

					refreshAllTheseFragments(types);

					if (mOnItemRemovedListener != null) {
						mOnItemRemovedListener.setTitle(getCount());
					}
				}
				OkCancelDialog.saveOkCancelCheckBox(
						mOkCancelDialog,
						NameValueDataDO.ALERT_LIST_DELETE_DONT_SHOW);
			}

			// CANCEL DELETE //
			@Override
			protected  void cancel(DialogInterface dialog) {
				//
			}
		};
	}

	private void showDeleteOrTrashOkCancel(final AlertItemDO alertItem) {
		boolean isToTrash = true;
		String str =
				"<b>Move this item to the Trash?</b>"
				+ "<br><br>Item can be restored in the Trash tab."
				;
		if (alertItem != null) {
			if (alertItem.getStatus().equals(Status.TRASH)) {
				str = "Permanently delete this item?";
				isToTrash = false;
			}
		}

		OkCancel okCancel = getDeleteOkCancel(alertItem);

		OkCancelDialog.UserActionOnChecked skipDialog =
				OkCancelDialog.isSkipDialog(
						NameValueDataDO.ALERT_LIST_DELETE_DONT_SHOW);

		if (skipDialog.equals(OkCancelDialog.UserActionOnChecked.DONT_SKIP)) {
			mOkCancelDialog = OkCancelDialog.showInstance(
					(AppCompatActivity) mActivity,
					str,
					"Don't show this message again",
					AutomatonAlert.CANCEL_LABEL,
					(isToTrash ?
							AutomatonAlert.OK_LABEL + ", Trash"
							: AutomatonAlert.OK_LABEL + ", Delete"),
					OkCancelDialog.CancelButton.LEFT,
					EWI.WARNING
			);
			mOkCancelDialog.setOkCancel(okCancel);
		}
		else {
			okCancel.doUserActionOkOrCancel(skipDialog);
		}
	}

	private OkCancel getRemoveReminderOkCancel(final AlertItemDO alertItem) {
		return new OkCancel() {
			// clear REMINDER/ALARM
			@Override
			protected  void ok(DialogInterface dialog) {
				// cancel alarms, delete postAlarms, remove api
				clearReminder(alertItem);
				// get rid of it from the list
				remove(alertItem);
				deleteOrTrashListItem(alertItem);
				setActionBarTitle();
				removeFromView(-1, alertItem);
				Utils.toastIt(mActivity, "Cleared.");
				refreshAllTheseFragments(mAlarmTypes);
				OkCancelDialog.saveOkCancelCheckBox(
						mOkCancelDialog,
						NameValueDataDO.ALERT_LIST_CLEAR_REMINDER_DONT_SHOW);
			}

			// CANCEL clear //
			@Override
			protected  void cancel(DialogInterface dialog) {
				//
			}
		};
	}

	private void showRemoveReminderOkCancel(final AlertItemDO alertItem) {
		OkCancel okCancel = getRemoveReminderOkCancel(alertItem);

		OkCancelDialog.UserActionOnChecked skipDialog =
				OkCancelDialog.isSkipDialog(
						NameValueDataDO.ALERT_LIST_CLEAR_REMINDER_DONT_SHOW);

		if (skipDialog.equals(OkCancelDialog.UserActionOnChecked.DONT_SKIP)) {
			mOkCancelDialog = OkCancelDialog.showInstance(
					(AppCompatActivity) mActivity,
					"<b>Clear this reminder?</b>"
							+ "<br>(this won't Trash the item)",
					"Don't show this message again",
					AutomatonAlert.CANCEL_LABEL,
					("Clear Reminder"),
					OkCancelDialog.CancelButton.LEFT,
					EWI.WARNING
			);
			mOkCancelDialog.setOkCancel(okCancel);
		}
		else {
			okCancel.doUserActionOkOrCancel(skipDialog);
		}

	}

	private void removeReminder(AlertItemDO alertItem) {
		showRemoveReminderOkCancel(alertItem);
	}

	private OkCancel getRemoveSnoozeOkCancel(final AlertItemDO alertItem) {
		return new OkCancel() {
			// clear REMINDER/ALARM
			@Override
			public void ok(DialogInterface dialog) {
				// cancel alarms, delete postAlarms, remove api
				AlertItemDO.cancelPostAlarm(alertItem);
				// get rid of it from the list
				remove(alertItem);
				deleteOrTrashListItem(alertItem);
				setActionBarTitle();
				removeFromView(-1, alertItem);
				Utils.toastIt(mActivity, "Cleared.");
				refreshAllTheseFragments(mAlarmTypes);
				OkCancelDialog.saveOkCancelCheckBox(
						mOkCancelDialog,
						NameValueDataDO.ALERT_LIST_CLEAR_SNOOZE_DONT_SHOW);
			}

			// CANCEL clear //
			@Override
			public void cancel(DialogInterface dialog) {
				//
			}
		};
	}

	private void showRemoveSnoozeOkCancel(final AlertItemDO alertItem) {
		OkCancel okCancel = getRemoveSnoozeOkCancel(alertItem);

		OkCancelDialog.UserActionOnChecked skipDialog =
				OkCancelDialog.isSkipDialog(
						NameValueDataDO.ALERT_LIST_CLEAR_SNOOZE_DONT_SHOW);

		if (skipDialog.equals(OkCancelDialog.UserActionOnChecked.DONT_SKIP)) {
			mOkCancelDialog = OkCancelDialog.showInstance(
					(AppCompatActivity) mActivity,
					"<b>Clear this snooze?</b>"
							+ "<br>(this won't Trash the item)",
					"Don't show this message again",
					AutomatonAlert.CANCEL_LABEL,
					("Clear Snooze"),
					OkCancelDialog.CancelButton.LEFT,
					EWI.WARNING
			);
			mOkCancelDialog.setOkCancel(okCancel);
		}
		else {
			okCancel.doUserActionOkOrCancel(skipDialog);
		}
	}

	private void removeSnooze(AlertItemDO alertItem) {
		showRemoveSnoozeOkCancel(alertItem);
	}

	private void clearRepeatAndSave(AlertItemDO alertItem) {
		AutomatonAlert.getAPIs().findCancelRemovePendingIntentsPostAlarms(
				ApiType.ALERT,
				ApiSubType.REPEAT,
				-1,
				alertItem.getAlertItemId(),
				alertItem.getNotificationItemId());

		// clear out repeats fields
		alertItem.setRepeatEvery(0);
		alertItem.setStopAfter(0);
		alertItem.save();
	}

	private OkCancel getRemoveRepeatOkCancel(final AlertItemDO alertItem) {
		return new OkCancel() {
			// clear REMINDER/ALARM
			@Override
			protected  void ok(DialogInterface dialog) {
				clearRepeatAndSave(alertItem);
				// get rid of it from the list
				remove(alertItem);
				deleteOrTrashListItem(alertItem);
				setActionBarTitle();
				removeFromView(-1, alertItem);
				Utils.toastIt(mActivity, "Cleared.");
				refreshAllTheseFragments(mAlarmTypes);
				OkCancelDialog.saveOkCancelCheckBox(
						mOkCancelDialog,
						NameValueDataDO.ALERT_LIST_CLEAR_REPEAT_DONT_SHOW);
			}

			// CANCEL clear //
			@Override
			protected  void cancel(DialogInterface dialog) {
				//
			}
		};
	}

	private void showRemoveRepeatOkCancel(final AlertItemDO alertItem) {
		OkCancel okCancel = getRemoveRepeatOkCancel(alertItem);

		OkCancelDialog.UserActionOnChecked skipDialog =
				OkCancelDialog.isSkipDialog(
						NameValueDataDO.ALERT_LIST_CLEAR_REPEAT_DONT_SHOW);

		if (skipDialog.equals(OkCancelDialog.UserActionOnChecked.DONT_SKIP)) {
			mOkCancelDialog = OkCancelDialog.showInstance(
					(AppCompatActivity) mActivity,
					"<b>Clear this repeating alarm?</b>"
							+ "<br>(this won't Trash the item)",
					"Don't show this message again",
					AutomatonAlert.CANCEL_LABEL,
					"Clear Repeat",
					OkCancelDialog.CancelButton.LEFT,
					EWI.WARNING
			);
			mOkCancelDialog.setOkCancel(okCancel);
		}
		else {
			okCancel.doUserActionOkOrCancel(skipDialog);
		}
	}

	private void removeRepeat(AlertItemDO alertItem) {
		showRemoveRepeatOkCancel(alertItem);
	}

	// listener
	private class DeleteOnClickListener implements ImageView.OnClickListener {
		@Override
		public void onClick(View v) {
			if (mActionMode != null) {
				return;
			}
			AlertItemDO alertItem = AlertListAdapterStatic.getAlertItem(v).first;
			if (alertItem != null) {
				int pos = getPosition(alertItem);
				if (pos >= 0) {
					showDeleteOrTrashOkCancel(alertItem);
				}
			}
		}
	}

	private OkCancel getSaveOkCancel(final AlertItemDO alertItem) {
		return new OkCancel() {
			// clear REMINDER/ALARM
			@Override
			protected  void ok(DialogInterface dialog) {
				// change its status (.update() saves db rec)
				alertItem.updateStatus(Status.SAVED);
				// change types
				removeFromView(-1, alertItem);
				Utils.toastIt(mActivity, "Saved.");
				refreshAllTheseFragments(
						new FragmentTypeAL[] {FragmentTypeAL.SAVED});
				OkCancelDialog.saveOkCancelCheckBox(
						mOkCancelDialog,
						NameValueDataDO.ALERT_LIST_SAVE_DONT_SHOW);
			}

			// CANCEL clear //
			@Override
			protected  void cancel(DialogInterface dialog) {
				//
			}
		};
	}

	private void showSaveOkCancel(final AlertItemDO alertItem) {
		OkCancel okCancel = getSaveOkCancel(alertItem);

		OkCancelDialog.UserActionOnChecked skipDialog =
				OkCancelDialog.isSkipDialog(
						NameValueDataDO.ALERT_LIST_SAVE_DONT_SHOW);

		if (skipDialog.equals(OkCancelDialog.UserActionOnChecked.DONT_SKIP)) {
			mOkCancelDialog = OkCancelDialog.showInstance(
					(AppCompatActivity) mActivity,
					"<b>Save this item?</b>"
							+ "<br><br>This item will be moved to the Saved tab",
					"Don't show this message again",
					AutomatonAlert.CANCEL_LABEL,
					(AutomatonAlert.OK_LABEL + ", " +
							"Save"),
					OkCancelDialog.CancelButton.LEFT,
					EWI.WARNING
			);
			mOkCancelDialog.setOkCancel(okCancel);
		}
		else {
			okCancel.doUserActionOkOrCancel(skipDialog);
		}
	}

	private class SaveOnClickListener implements ImageView.OnClickListener {
		@Override
		public void onClick(View v) {
			if (mActionMode != null) {
				return;
			}
//			stopActionModeAllFragments();
			AlertItemDO alertItem = AlertListAdapterStatic.getAlertItem(v).first;
			if (alertItem != null) {

				/////////////////////////////
				// Alarm types are CLEAR'd instead of SAVE'd
				/////////////////////////////
				// ALARMS
				if (getFragmentType().equals(FragmentTypeAL.ALARMS)) {
					removeReminder(alertItem);
					// keep types
				}
				//SNOOZED
				else if (getFragmentType().equals(FragmentTypeAL.SNOOZED)) {
					removeSnooze(alertItem);
					// keep types
				}
				//REPEATS
				else if (getFragmentType().equals(FragmentTypeAL.REPEATS)) {
					removeRepeat(alertItem);
					// keep types
				}
				//////////////////////
				// SAVE it
				//////////////////////
				//ALL ELSE
				else {
					showSaveOkCancel(alertItem);
				}
			}
		}
	}

	/////////////
	// alarm
	/////////////

	// listener
	private class AlarmOnClickListener implements ImageView.OnClickListener {
		@Override
		public void onClick(View v) {
			if (mActionMode != null) {
				return;
			}
			Pair<AlertItemDO, View> pair = AlertListAdapterStatic.getAlertItem(v);
			// toggle
			if (pair.first != null) {
				if (getFragmentType().equals(FragmentTypeAL.SNOOZED)) {
					showDatePickerTimePicker(pair.first);
				}
				else {
//					if (getFragmentType().equals(FragmentTypeAL.ALARMS)) {
						startSetAlertActivity(pair.first);
//					}
//					else {
//						startSetAlertActivity(pair.first);
//					}
				}
			}
		}
	}

	private void showDatePickerTimePicker(AlertItemDO alertItem) {
		mAlertItem = alertItem;
		AlertListAdapterStatic.stuffAlertItemForSnooze(mAlertItem);

		mPicker =
				DatePickerTimePicker.showInstance(
						mActivity.getApplicationContext(),
						(FragmentActivity)mActivity,
						Utils.getDateRemindOrNow(alertItem).getTime(),
						this);
	}

	private void startSetAlertActivity(AlertItemDO alertItem) {
		Intent intent = new Intent(
				mActivity,
				SetAlertActivity.class);
		intent.putExtra(
				AlertItemDO.TAG_ALERT_ITEM_ID,
				alertItem.getAlertItemId());
		intent.putExtra(AutomatonAlert.M_MODE, SetAlertActivity.Mode.ALARM);
		intent.putExtra(
				TAG_ALERT_NAME,
				Utils.formatHeadersForView(
						Utils.markupHeaderFields(
								alertItem.getKvRawDetails())));
		//intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		int idx = getPosition(alertItem);
		int reqCode =
				((IALActivityController)mActivity).encodeRequestCode(idx, getFragmentType());
//		idx += AlertListActivity.ALARM_SET_REQCODE_BASE;
		mActivity.startActivityForResult(intent, reqCode);
	}

	public void replaceShowListAlertItem(int requestCode) {
		int idx = ((IALActivityController)mActivity).decodeRequestCode(requestCode).second;
		// no data came back
		if (idx <= -1) {
			return;
		}
		// bad data
		int count = getCount();
		if (idx >= count) {
			return;
		}
		AlertItemDO holdAlertItem = getItem(idx);
		AlertItemDO dbAlertItem = AlertItems.get(holdAlertItem.getAlertItemId());
		// problem, keep the old one
		if (dbAlertItem == null) {
			return;
		}
		// there's nothing equivalent in
		// ArrayAdapter, so replace item in mShowList directly
		mShowList.set(idx, dbAlertItem);
		notifyDataSetChanged();
	}

	class GotoSourceOnClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			if (mActionMode != null) {
				return;
			}
			String source = (String)v.getTag();
			if (source != null) {
				FragmentTypeRT fragType = null;
				if (source.equals(AccountSmsDO.SMS)
						|| source.equals(AccountSmsDO.MMS)) {
					fragType = FragmentTypeRT.TEXT;
				}
				else {
					fragType = FragmentTypeRT.EMAIL;
				}
				Utils.IntentReqTypeRec rec = null;
				Pair<AlertItemDO, View> pair = AlertListAdapterStatic.getAlertItem(v);
				if (pair.first != null) {
//					String from =
//							pair.first.getKvRawDetails().get(AutomatonAlert.FROM);
					rec = Utils.setSourceIntents(fragType);
				}
				if (rec == null) {
					return;
				}

				try {
					mActivity.startActivity(rec.mIntent);
				}
				catch (ActivityNotFoundException e) {
					Utils.toastIt(
							mActivity.getApplicationContext(),
							"Unable to open messaging app!");
				}
			}
		}
	}

	class ContactOnClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			if (mActionMode != null) {
				return;
			}
//			stopActionModeAllFragments();
			showContactLookup(v);
		}
	}

	private void showContactLookup(View v) {
		Intent intent = new Intent();
		String id = (String)v.getTag();

		if (!TextUtils.isEmpty(id)) {
			intent.setAction(Intent.ACTION_VIEW);
			intent.setData(Uri.parse(Contacts.CONTENT_LOOKUP_URI + "/" + id));
			mActivity.startActivity(intent);
		}
		else {
			AlertItemDO alertItem = AlertListAdapterStatic.getAlertItem(v).first;
			if (alertItem != null) {
				intent.setAction(Intent.ACTION_INSERT);
				String from = alertItem.getKvRawDetails().get(AutomatonAlert.FROM);
				String displayName = alertItem.getKvRawDetails().get(Contacts.DISPLAY_NAME);
				if (AlertItemDO.isSmsMms(alertItem.getKvRawDetails())) {
					intent.putExtra(ContactsContract.Intents.Insert.PHONE, from);
				}
				else {
					intent.putExtra(ContactsContract.Intents.Insert.EMAIL, from);
				}
				intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
				intent.putExtra(ContactsContract.Intents.Insert.NAME, displayName);
				mActivity.startActivity(intent);
			}
		}
	}

	private boolean deleteOrTrashListItem(AlertItemDO alertItem) {
		if (getFragmentType().equals(FragmentTypeAL.NEW)
				|| getFragmentType().equals(FragmentTypeAL.SAVED)) {
			alertItem.updateStatus(Status.TRASH);
			return true; /* refresh Trash */
		}
		else if (getFragmentType().equals(FragmentTypeAL.TRASH)) {
			alertItem.delete();
			return true; /* refresh Trash */
		}

		return false;
	}

	private AlertListInfo createAlertListInfo(
			AlertItemDO alertItem, boolean selected) {

		return new AlertListInfo(
				mActivity,
				AlertListAdapterStatic.getLookupKey(alertItem),
				AlertListAdapterStatic.getDisplayName(alertItem),
				selected);
	}

	private void postCreateFullAlertListInfoSet(final boolean isSelected) {
		mFragment.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				selectUnselectVisibleViews(isSelected);
				stopProgressBar();

				if (isSelected) {
					mActionModeItems = mShowList.size();
				}
				else {
					mActionModeItems = 0;
					mActionMode.finish();
					mActionMode = null;
				}
				subTitleUpdateActionMode();
			}
		});
	}

	private void createFullAlertListInfoSet(final boolean isSelected) {
		// create an complete list of new AlertListInfo's
		// with AlertItem as the key.  Get AlertItem from
		// mShowList.  Don't get a new AlertListInfo if
		// there's already one in the list
		startProgressBar();

		new Thread(new Runnable() {
			@Override
			public void run() {
				AlertListInfo ali = null;
				for (AlertItemDO alertItem : mShowList) {
					// new AlertListInfo, selected
					if ((ali = mAlertListInfos.get(alertItem)) == null) {
						ali = createAlertListInfo(alertItem, isSelected);
						mAlertListInfos.put(alertItem, ali);
					}
					else {
						ali.setSelected(isSelected);
					}
				}
				postCreateFullAlertListInfoSet(isSelected);
			}
		})
		.start();

	}

	private void selectUnselectVisibleViews(boolean isSelected) {
		ListView listView = getListView();
		// start ActionMode if we're selecting all
		if (isSelected) {
			if (mActivity instanceof AppCompatActivity) {
				startActionMode(false/*do not clear all ActionMode data*/);
			}
		}
		// make sure visible list items reflect being selected/unselected
		int N=listView.getChildCount();
		AlertListInfo ali = null;
		for (int i=0;i<N;i++) {
			View child = listView.getChildAt(i);
			// It seems setTag() copies the object so it can't be
			// directly modified via AlertListInfos (set above)
			AlertItemDO alertItem = AlertListAdapterStatic.getAlertItem(child).first;
			ViewHolder vh = AlertListAdapterStatic.getViewHolder(child).first;
			// get the one that's already selected and tag it
			ali = mAlertListInfos.get(alertItem);
			vh.mExpandCollapseWithALI.setTag(ali);
			// hilight the list item
			setListItemViewsActionMode(child);
		}
	}

	public void selectUnselectAllItems(boolean isSelected) {
		// if none are selected and asking to
		// unselect all, just return
		if (!isSelected
				&& mActionModeItems <= 0) {
			return;
		}
		createFullAlertListInfoSet(isSelected);
	}

	private void setActionBarTitle() {
		((IALActivityController)mActivity).setActionBarTitle(AlertListArrayAdapter.this);
	}

	private void clearReminder(AlertItemDO alertItem) {
		// alarms - clear ALL (this is an ALARM)
		alertItem.findCancelRemovePendingIntentsPostAlarms(ApiSubType.ALARM);
		alertItem.updateDateRemind(null, true/*save*/);
	}


	private void clearReminderSelectedItems() {
		new Thread(new Runnable() {
			int mNewCount = 0;
			int mSavedCount = 0;

			ArrayList<AlertItemDO> mAlertItems = new ArrayList<AlertItemDO>();
			@Override
			public void run() {
				for (Entry<AlertItemDO, AlertListInfo> entry : mAlertListInfos.entrySet()) {
					if (entry.getValue().isSelected()) {
						AlertItemDO alertItem = entry.getKey();
						if (alertItem.getDateRemind() != null) {
							clearReminder(alertItem);
							mAlertItems.add(alertItem);

							if (alertItem.getStatus().equals(AlertItemDO.Status.NEW)) {
								++mNewCount;
							}
							else if (alertItem.getStatus().equals(AlertItemDO.Status.SAVED)) {
								++mSavedCount;
							}
						}
					}
				}
				uiRemoveRefreshClearAndToastActionMode(
						mAlertItems,
						new boolean[] {
								(mNewCount>0||mSavedCount>0)
						},
						new FragmentTypeAL[] {
								FragmentTypeAL.ALARMS
						},
						"reminder",
						"cleared");
			}
		}).start();
	}


	private void clearSnoozedSelectedItems() {
		new Thread(new Runnable() {
			ArrayList<AlertItemDO> mAlertItems = new ArrayList<AlertItemDO>();
			@Override
			public void run() {
				for (Entry<AlertItemDO, AlertListInfo> entry : mAlertListInfos.entrySet()) {
					if (entry.getValue().isSelected()) {
						AlertItemDO alertItem = entry.getKey();
						if (alertItem != null) {
							AlertItemDO.cancelPostAlarm(alertItem);
							mAlertItems.add(alertItem);
						}
					}
				}
				uiRemoveRefreshClearAndToastActionMode(
						mAlertItems,
						new boolean[] {
								mAlertItems.size()>0,
								mAlertItems.size()>0,
								},
						new FragmentTypeAL[] {
								FragmentTypeAL.ALARMS,
								FragmentTypeAL.SNOOZED
						},
						"snooze",
						"cleared");
			}
		}).start();
	}


	private void clearRepeatsSelectedItems() {
		new Thread(new Runnable() {
			ArrayList<AlertItemDO> mAlertItems = new ArrayList<AlertItemDO>();
			@Override
			public void run() {
				for (Entry<AlertItemDO, AlertListInfo> entry : mAlertListInfos.entrySet()) {
					if (entry.getValue().isSelected()) {
						AlertItemDO alertItem = entry.getKey();
						if (alertItem != null) {
							clearRepeatAndSave(alertItem);
							mAlertItems.add(alertItem);
						}
					}
				}
				uiRemoveRefreshClearAndToastActionMode(
						mAlertItems,
						new boolean[] {
								mAlertItems.size()>0,
								mAlertItems.size()>0,
								mAlertItems.size()>0,
								mAlertItems.size()>0
								},
						new FragmentTypeAL[] {
								FragmentTypeAL.NEW,
								FragmentTypeAL.SAVED,
								FragmentTypeAL.REPEATS,
								FragmentTypeAL.TRASH
						},
						"repeat",
						"cleared");
			}
		}).start();
	}

	private void saveSelectedItems() {
		new Thread(new Runnable() {
			ArrayList<AlertItemDO> mAlertItems = new ArrayList<AlertItemDO>();
			@Override
			public void run() {
				for (Entry<AlertItemDO, AlertListInfo> entry : mAlertListInfos.entrySet()) {
					if (entry.getValue().isSelected()) {
						AlertItemDO alertItem = entry.getKey();
						alertItem.updateStatus(Status.SAVED);
						mAlertItems.add(alertItem);
					}
				}

				uiRemoveRefreshClearAndToastActionMode(
						mAlertItems,
						new boolean[] {
								mAlertItems.size()>0,
								mAlertItems.size()>0,
								mAlertItems.size()>0
								},
						new FragmentTypeAL[] {
								FragmentTypeAL.NEW,
								FragmentTypeAL.TRASH,
								FragmentTypeAL.SAVED
						},
						"item",
						"saved");
			}
		}).start();
	}

	private void refreshAllTheseFragments(FragmentTypeAL[] types) {
		boolean[] doIts = new boolean[types.length];
		Arrays.fill(doIts, true);
		refreshAllTheseFragments(doIts, types);
	}

	private void refreshAllTheseFragments(boolean[] doIts, FragmentTypeAL[] types) {
		if (mActivity == null) {
			return;
		}
		((IALActivityController)mActivity).refreshFragments(doIts, types);
	}

	private void uiRemoveRefreshClearAndToastActionMode(
			final ArrayList<AlertItemDO> alertItems, final boolean[] refreshes,
			final FragmentTypeAL[] types,	final String item,
			final String action) {

		mActivity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				for (AlertItemDO alertItem : alertItems) {
					remove(alertItem);
				}
				setActionBarTitle();
				refreshAllTheseFragments(refreshes, types);
				clearAllActionMode();
				toastIt(mActivity, alertItems.size(), item, action);
			}
		});
	}

	private void deleteSelectedItems() {
		new Thread(new Runnable() {
			boolean mRefreshTrash = false;
			ArrayList<AlertItemDO> mAlertItems = new ArrayList<AlertItemDO>();
			@Override
			public void run() {
				for (Entry<AlertItemDO, AlertListInfo> entry : mAlertListInfos.entrySet()) {
					if (entry.getValue().isSelected()) {
						AlertItemDO alertItem = entry.getKey();
						clearReminder(alertItem);
						mAlertItems.add(alertItem);
						deleteOrTrashListItem(alertItem);
					}
				}
				uiRemoveRefreshClearAndToastActionMode(
						mAlertItems,
						new boolean[] {
								mRefreshTrash
								},
						new FragmentTypeAL[] {
								FragmentTypeAL.TRASH
						},
						"item",
						"trashed/deleted");
			}
		}).start();
	}

	private void startActionMode(boolean clearAll) {
		if (mActionMode == null) {
			if (mActivity instanceof AppCompatActivity) {
				mActionMode =
						((AppCompatActivity)mActivity)
								.startSupportActionMode(mActionModeCallback);
				showAllCheckBoxesViewsActionMode();
			}
			if (clearAll) {
				clearAllViewsAndListsActionMode();
			}
		}
	}

	private boolean itemSelectedUnselectedActionMode(
			View v, ViewHolder vh, AlertListInfo alertListInfo) {

		if (alertListInfo == null) {
			return false;
		}
		startActionMode(true/*clear all ActionMode data*/);

		// TOGGLE TOGGLE TOGGLE TOGGLE TOGGLE
		// from marked to not-marked
		if (alertListInfo.mSelected) {
			--mActionModeItems;
			alertListInfo.mSelected = false;
			vh.mCheckBox.setChecked(false);
		}
		// from not-marked to marked
		else {
			++mActionModeItems;
			alertListInfo.mSelected = true;
			vh.mCheckBox.setChecked(true);
		}
		setListItemViewsActionMode(v);
		subTitleUpdateActionMode();

		return true;
	}

	private boolean itemSelectedUnselectedActionMode(View v) {
		ViewHolder vh =
				AlertListAdapterStatic.getViewHolder(v).first;
		if (vh == null) {
			return false;
		}
		AlertListInfo alertListInfo =
				(AlertListInfo)vh.mExpandCollapseWithALI.getTag();

		return alertListInfo != null && itemSelectedUnselectedActionMode(v, vh, alertListInfo);

	}

	class ActionModeListItemLongClickListener implements View.OnLongClickListener {
		public boolean onLongClick(View v) {
			return itemSelectedUnselectedActionMode(v);
		}
	}

	public static void toastIt(Context context, int count, String cat, String action) {
		Utils.toastIt(
				context,
				count
				+ " "
				+ Utils.returnPluralOrOriginal(count, cat, "s")
				+ " "
				+ action);
	}

	@SuppressLint("InflateParams")
	private void showProgressBar(MenuItem item) {
		View actionView = null;
		LayoutInflater inflater =
				(LayoutInflater)mActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		actionView = inflater.inflate(R.layout.actionbar_refresh_progressbar, null);
		item.setActionView(actionView);
	}

	private ActionMode.Callback mActionModeCallback = new ActionMode.Callback() {
		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
			showProgressBar(item);

			switch (item.getItemId()) {
			case R.id.alm_clear_reminders:
				clearReminderSelectedItems();
				return true;
			case R.id.alm_clear_snoozed:
				clearSnoozedSelectedItems();
				return true;
			case R.id.alm_clear_repeats:
				clearRepeatsSelectedItems();
				return true;
			case R.id.alm_trash:
				deleteSelectedItems();
				return true;
			case R.id.alm_save:
				saveSelectedItems();
				return true;
			default:
				return false;
			}
		}

		private int getActionModeContextMenu() {
			int rMenu = R.menu.alert_list_menu_new;

			if (getFragmentType().equals(FragmentTypeAL.SAVED)) {
				rMenu = R.menu.alert_list_menu_saved;
			}
			else if (getFragmentType().equals(FragmentTypeAL.TRASH) ){
				rMenu = R.menu.alert_list_menu_trash;
			}
			else if (getFragmentType().equals(FragmentTypeAL.ALARMS) ){
				rMenu = R.menu.alert_list_menu_alarms;
			}
			else if (getFragmentType().equals(FragmentTypeAL.SNOOZED) ){
				rMenu = R.menu.alert_list_menu_snoozed;
			}
			else if (getFragmentType().equals(FragmentTypeAL.REPEATS) ){
				rMenu = R.menu.alert_list_menu_repeats;
			}
			return rMenu;
		}

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			MenuInflater inflater = mode.getMenuInflater();
			int rMenu = getActionModeContextMenu();
			inflater.inflate(rMenu, menu);
	        return true;
        }

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			mActionMode = null;
			clearAllActionMode();
	    }

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
	        return false;
		}

	};

	public class CheckBoxOnClickListener implements CheckBox.OnClickListener {
		@Override
		public void onClick(View v) {
			AlertListInfo alertListInfo =
					AlertListAdapterStatic.getAlertListInfo(v).first;
			if (alertListInfo == null) {
				return;
			}
			CheckBox cb = (CheckBox)v;

			// selectItemActionMode is a from-to
			// so we need to do the opposite of
			// what is checked/not checked here
			if (cb.isChecked()) {
				// will change in selectItemActionMode
				// to markedForDeletion = true;
				alertListInfo.setSelected(false);
			}
			else {
				alertListInfo.setSelected(true);
			}
			itemSelectedUnselectedActionMode(v);
		}
	}

	private void setListItemViewsActionMode(
			ViewHolder vh, AlertListInfo alertListInfo) {

		Resources res = mActivity.getResources();
		int colorHoloBlueDark =	res.getColor(android.R.color.holo_blue_dark);
		int colorBackgroundLight = res.getColor(android.R.color.background_light);

		// make sure things are clear and GONE
		// if not in action mode and then leave
		if (mActionMode == null) {
			if (vh.mCheckBox.isChecked()) {
				vh.mCheckBox.setChecked(false);
			}
			vh.mRootLayoutWithVH.setBackgroundColor(colorBackgroundLight);
			vh.mTextViewLine1.setTextColor(colorHoloBlueDark);
			return;
		}

		// in ACTION MODE
		vh.mCheckBox.setVisibility(CheckBox.VISIBLE);

		if (alertListInfo == null) {
			return;
		}
		if (alertListInfo.isSelected()) {
			vh.mRootLayoutWithVH.setBackgroundColor(colorHoloBlueDark);
			vh.mTextViewLine1.setTextColor(colorBackgroundLight);
			vh.mCheckBox.setChecked(true);
		}
		else {
			vh.mRootLayoutWithVH.setBackgroundColor(colorBackgroundLight);
			vh.mTextViewLine1.setTextColor(colorHoloBlueDark);
			vh.mCheckBox.setChecked(false);
		}
	}


	private void setListItemViewsActionMode(View v) {
		ViewHolder vh = AlertListAdapterStatic.getViewHolder(v).first;
		if (vh == null) {
			return;
		}

//		AlertListInfo alertListInfo =
//		AlertListAdapterHelper.getAlertListInfo(v).first;
		AlertListInfo alertListInfo =
				(AlertListInfo)vh.mExpandCollapseWithALI.getTag();
		if (alertListInfo == null) {
			return;
		}

		setListItemViewsActionMode(vh, alertListInfo);
	}

	private void clearAllViewsAndListsActionMode() {
		for (AlertListInfo ali : mAlertListInfos.values()) {
			ali.setSelected(false);
		}
		mActionModeItems = 0;
		ListView lv = getListView();
		int count = lv.getChildCount();
		for (int i=0;i<count;i++) {
			View child = lv.getChildAt(i);
			setListItemViewsActionMode(child);
		}
	}

	private void showAllCheckBoxesViewsActionMode() {
		ListView lv = getListView();
		int count = lv.getChildCount();
		for (int i=0;i<count;i++) {
			View child = lv.getChildAt(i);
			ViewHolder vh = AlertListAdapterStatic.getViewHolder(child).first;
			if (vh != null) {
				vh.mCheckBox.setVisibility(CheckBox.VISIBLE);
				vh.mCheckBox.setChecked(false);
			}
		}
	}

	private void clearAllActionMode() {
		if (mActionMode != null) {
			mActionMode.finish();
			mActionMode = null;
		}
		mActionModeItems = 0;
		for (AlertListInfo info : mAlertListInfos.values()) {
			info.setSelected(false);
		}
		clearAllViewsAndListsActionMode();
		notifyDataSetChanged();
	}

	private void subTitleUpdateActionMode() {
		if (mActionModeItems == 0) {
			if (mActionMode != null) {
				mActionMode.setSubtitle(null);
			}
			clearAllActionMode();
		}
		else {
			mActionMode.setSubtitle("" + mActionModeItems + " Selected");
		}
	}

	private boolean isDateInTheFuture(Calendar alertDate) {
		return !(alertDate.before(Calendar.getInstance()));
	}

	@Override
	public boolean dateTimeChanged(Calendar alertDate) {
		// make sure it's not in the past
		if (isDateInTheFuture(alertDate)) {
			AlertItemDO.setSnooze(
					mAlertItem,
					null,
					alertDate.getTimeInMillis() - System.currentTimeMillis());
			refreshShowList(mShowList);
			Utils.showSetAlarmToast(
					mActivity, alertDate.getTimeInMillis(), true/*snooze*/);
		}
		else {
			SetAlertActivity.showRequireOnlyFutureAlarmsDialog(mActivity);
		}

		return true;
	}

	@Override
	public void postProcess(boolean dateChangeClientResult) {
	}
}

