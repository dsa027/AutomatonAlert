package com.aldersonet.automatonalert.Fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Activity.RTUpdateActivity.FragmentTypeRT;
import com.aldersonet.automatonalert.Fragment.VolumeChooserFragment.VolumeTypes;
import com.aldersonet.automatonalert.Preferences.RTPrefsDO;
import com.aldersonet.automatonalert.Preferences.RTSettingsFragment;
import com.aldersonet.automatonalert.R;
import com.aldersonet.automatonalert.Util.Utils;
import com.google.android.mms.ContentType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RTChooserFragment extends DialogFragment {

	private static String ICON_ID = "iconId";
	private static String APP_LABEL = "appLabel";
	private static String PACKAGE_NAME = "packageName";
	private static String APP_NAME = "appName";
	private static String APP_TYPE = "appType";

	public static int APP_REQ_CODE = 0x8537;
	public static int SYS_REQ_CODE = 0x4324;
	public static int DEFAULT_REQ_CODE = 0x5843;

	Fragment mFragmentCaller;
	String mSongName;
	Uri mSongUri;
	Uri mCurrentRTUri;

	enum AppType {
		HEADER, SYSTEM, APP
	}

	private static int[] mSysRingtoneType = {
		RingtoneManager.TYPE_ALL,
		RingtoneManager.TYPE_RINGTONE,
		RingtoneManager.TYPE_ALARM,
		RingtoneManager.TYPE_NOTIFICATION
	};

	private static String[] mSysRingtoneTypeHeader = {
		"All System Sounds",
		"Ringtones",
		"Alarms",
		"Notifications"
	};

	private String mSourceType = FragmentTypeRT.SETTINGS.name();

	IRTChooserFragmentListener mListener;
    ArrayList<HashMap<String, Object>> mChoosers = new ArrayList<HashMap<String, Object>>();
    FragmentTypeRT mHostFragmentType = null;
	ListItemClickListener mListItemClick = new ListItemClickListener();

	public static RTChooserFragment showInstance(
			AppCompatActivity activity,
			FragmentTypeRT fragmentType, Uri currentRTUri) {

		final RTChooserFragment dialog = new RTChooserFragment();
		final FragmentManager fm = activity.getSupportFragmentManager();

		Bundle bundle = new Bundle();
		bundle.putString(AutomatonAlert.FRAGMENT_TYPE, fragmentType.name());
		bundle.putString("uri", (currentRTUri == null) ? "" : currentRTUri.toString());
		dialog.setArguments(bundle);

		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				dialog.show(fm, "RTChooserFragment" + "whatever");
			}
		});

		return dialog;
	}

	private Fragment findOurFragment() {
		if (mListener != null) {
			List<Fragment> list =
					((AppCompatActivity)mListener).getSupportFragmentManager().getFragments();
			for (Fragment fragment : list) {
				if (fragment instanceof RTUpdateFragment) {
					RTUpdateFragment updFragment = (RTUpdateFragment)fragment;
					if (updFragment.mRTChooserFragment != null
							&& updFragment.mRTChooserFragment == this) {
						return updFragment;
					}
				}
				if (fragment instanceof RTSettingsFragment) {
					RTSettingsFragment setFragment = (RTSettingsFragment)fragment;
					if (setFragment.mRTChooserFragment != null
							&& setFragment.mRTChooserFragment == this) {
						return setFragment;
					}
				}
			}
		}

		return null;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mListener = (IRTChooserFragmentListener) activity;
			mFragmentCaller = findOurFragment();
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement IRTChooserFragmentListener");
		}
	}

	@Override
	public void onDetach() {
		super.onDetach();
		if (mFragmentCaller instanceof RTUpdateFragment) {
			((RTUpdateFragment)mFragmentCaller).mRTChooserFragment = null;
		}
		else if (mFragmentCaller instanceof RTSettingsFragment) {
			((RTSettingsFragment)mFragmentCaller).mRTChooserFragment = null;
		}
		mListener = null;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog dialog = super.onCreateDialog(savedInstanceState);
		dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);

		Bundle bundle = getArguments();
		String fragmentType = bundle.getString((AutomatonAlert.FRAGMENT_TYPE));
		try { mHostFragmentType = FragmentTypeRT.valueOf(fragmentType); }
				catch (IllegalArgumentException ignored) {}
		mCurrentRTUri = Uri.parse(bundle.getString("uri"));

		return dialog;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
	                         Bundle savedInstanceState) {

		ListView returnView = (ListView)inflater.inflate(
				R.layout.list, container, false);

		addSystemRingtonesToChooser();
		addAppsToChooser();
		ChooserArrayAdapter arrayAdapter = new ChooserArrayAdapter(
				getActivity(),
				R.layout.rt_chooser_list_textview,
				mChoosers);
		returnView.setAdapter(arrayAdapter);

		return returnView;
	}

	private class ViewHolder {
		TextView mLabel;
		ImageView mImage;
		TextView mFull;
		TextView mType;
		TextView mPkg;
		TextView mApp;
	}

	private class ChooserArrayAdapter extends ArrayAdapter<HashMap<String, Object>> {
		int mLayout;

		public ChooserArrayAdapter(Context context, int textViewResourceId,
				List<HashMap<String, Object>> objects) {
			super(context, textViewResourceId, objects);
			mLayout = textViewResourceId;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder viewHolder;

			if (convertView == null) {
				convertView = ((LayoutInflater)getActivity()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
						.inflate(mLayout, null);

				viewHolder = new ViewHolder();

				viewHolder.mLabel = (TextView)convertView.findViewById(R.id.pcli_label);
				viewHolder.mImage = (ImageView)convertView.findViewById(R.id.pcli_icon);
				viewHolder.mFull = (TextView)convertView.findViewById(R.id.pcli_full_width);
				viewHolder.mType = (TextView)convertView.findViewById(R.id.pcli_type);
				viewHolder.mPkg = (TextView)convertView.findViewById(R.id.pcli_package);
				viewHolder.mApp = (TextView)convertView.findViewById(R.id.pcli_app);

				convertView.setTag(R.id.TAG_VIEWHOLDER, viewHolder);
			}
			else {
				viewHolder = (ViewHolder)convertView.getTag(R.id.TAG_VIEWHOLDER);
			}

			convertView.setTag(R.id.TAG_POSITION, position);

			HashMap<String, Object> map = mChoosers.get(position);

			if (map.get(APP_TYPE).equals(AppType.HEADER.name())) {
				viewHolder.mImage.setImageDrawable(null);
				viewHolder.mLabel.setText("");
				viewHolder.mFull.setTextColor(Color.WHITE);
				viewHolder.mFull.setText((String)mChoosers.get(position).get(APP_LABEL));
                int bg = getResources().getColor(android.R.color.holo_blue_light);
				convertView.setBackgroundColor(bg);
				convertView.setAlpha(.65f);
			}
			else {
				viewHolder.mImage.setImageDrawable((Drawable)mChoosers.get(position).get(ICON_ID));
				viewHolder.mLabel.setText((String)mChoosers.get(position).get(APP_LABEL));
				viewHolder.mFull.setTextColor(Color.BLACK);
				viewHolder.mFull.setText("");
                int bg = getResources().getColor(android.R.color.background_light);
				convertView.setBackgroundColor(bg);
				convertView.setAlpha(1f);
			}

			viewHolder.mType.setText((String)map.get(APP_TYPE));
			viewHolder.mPkg.setText((String)map.get(PACKAGE_NAME));
			viewHolder.mApp.setText((String)map.get(APP_NAME));

			convertView.setOnClickListener(mListItemClick);

			return convertView;
		}
	}

	class ListItemClickListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			int position = (Integer)v.getTag(R.id.TAG_POSITION);
			Map<String, Object> item = mChoosers.get(position);
			String type = (String) item.get(APP_TYPE);

			// external chooser requested
			if (type.equals(AppType.APP.name())) {
				String pkg = (String) item.get(PACKAGE_NAME);
				String app = (String) item.get(APP_NAME);
				if (pkg != null &&
						app != null) {
					Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
					intent.setType(ContentType.AUDIO_UNSPECIFIED);
					intent.setClassName(pkg, app);
					startActivityForResult(intent, APP_REQ_CODE);
				}
			}
			// default, silent, reset, or the system ringtone chooser
			else if (type.equals(AppType.SYSTEM.name())) {
				// default or silent
				String pName = (String) item.get(PACKAGE_NAME);
				String aName = (String) item.get(APP_NAME);
				if (        pName.equals(AutomatonAlert.DEFAULT)
						&&  aName.equals(AutomatonAlert.DEFAULT)
						||
							pName.equals(AutomatonAlert.SILENT)
						&&  aName.equals(AutomatonAlert.SILENT)
						||
							pName.equals(AutomatonAlert.BLOCK)
						&&  aName.equals(AutomatonAlert.BLOCK)) {
					// pass "default" through processActivityResult
					Intent intent = new Intent();
					intent.setData(Uri.parse(aName));
					processActivityResult(DEFAULT_REQ_CODE, intent);
				}
				// reset
				else if (item.get(PACKAGE_NAME).equals("") &&
						item.get(APP_NAME).equals("")) {
					Intent intent = new Intent();
					intent.setData(Uri.parse(""));
					processActivityResult(DEFAULT_REQ_CODE, intent);
				}
				// anything else
				else {
					showSystemRingtonePicker((String) item.get(APP_LABEL));
				}
			}
		}
	}

	public static Uri getCurrentRTUri(Uri currentRTUri) {
		Uri ret = Uri.parse("");
		String curr = currentRTUri == null ? "" : currentRTUri.toString();

		if (TextUtils.isEmpty(curr)) {
			ret = Uri.parse("");
		}
		else if (VolumeTypes.ringtone.name().equals(curr)) {
			ret = Settings.System.DEFAULT_RINGTONE_URI;
		}
		else if (VolumeTypes.notification.name().equals(curr)) {
			ret = Settings.System.DEFAULT_NOTIFICATION_URI;
		}
		else if (VolumeTypes.alarm.name().equals(curr)) {
			ret = Settings.System.DEFAULT_ALARM_ALERT_URI;
		}
		else {
			ret = currentRTUri;
		}

		return ret;
	}
	private void showSystemRingtonePicker(String type) {
		int iType = mSysRingtoneType[0];

		//match type (header string) with Ringtone.TYPE_xxxx
		int N=mSysRingtoneType.length;
		for (int i=0;i<N;i++) {
			if (type.equals(mSysRingtoneTypeHeader[i])) {
				iType = mSysRingtoneType[i];
			}
		}

		Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, iType);

		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, getCurrentRTUri(mCurrentRTUri));
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select from " + type);
		intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, Boolean.FALSE);
		startActivityForResult(intent, SYS_REQ_CODE);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode != DEFAULT_REQ_CODE) {
			super.onActivityResult(requestCode, resultCode, data);
		}

		if (data != null) {
			processActivityResult(requestCode, data);
		}

	}

	private void processActivityResult(int requestCode, Intent data) {
		String songName = null;
		Uri songUri = null;

		if (requestCode == APP_REQ_CODE) {
			songUri = Uri.parse(data.getDataString());
			songName = Utils.getSongName(songUri);
		}
		else if (requestCode == SYS_REQ_CODE) {
			if (data != null) {
				songUri = data.getParcelableExtra(
						RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
			}
			if (songUri != null) {
				songName = Utils.getSongName(songUri);
			}

			if (songUri == null
					|| TextUtils.isEmpty(songUri.toString())) {
				songUri = mCurrentRTUri;
				songName = Utils.getSongName(songUri);
			}
		}
		else if (requestCode == DEFAULT_REQ_CODE) {
			String s = data.getDataString();
			if (s.equals(AutomatonAlert.DEFAULT)) {
				songName = AutomatonAlert.DEFAULT_LABEL;
			}
			else if (s.equals(AutomatonAlert.SILENT)) {
				songName = AutomatonAlert.SILENT_LABEL;
			}
			else if (s.equals(AutomatonAlert.BLOCK)) {
				songName = AutomatonAlert.BLOCK_SMS_MMS_LABEL;
			}
			else {		// reset
				songName = "";
				songUri = Uri.parse("");
			}
		}

		if (songName !=null) {
			if (songName.equalsIgnoreCase(AutomatonAlert.DEFAULT_LABEL) ||
					songName.equalsIgnoreCase(AutomatonAlert.SILENT_LABEL) ||
					songName.equalsIgnoreCase(AutomatonAlert.BLOCK_SMS_MMS_LABEL)) {
				songUri = Uri.parse(songName);
			}
		}

		// if song is valid, make call to update song name on screen,
		// for both, then unattach us
		if (!(TextUtils.isEmpty(songName))) {
			mSongName = songName;
			mSongUri = songUri;
			mListener.updateRingtone(
					getDialog(), mFragmentCaller, mSourceType, mSongName, mSongUri);
		}
		else {
			mSongName = "";
			mSongUri = Uri.parse("");
			mListener.updateRingtone(
					getDialog(), mFragmentCaller, null, null, null);
		}
	}

	private void addAppsToChooser() {
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType(ContentType.AUDIO_UNSPECIFIED);

		PackageManager manager = getActivity().getPackageManager();
        List<ResolveInfo> info = manager.queryIntentActivities(intent, 0);

		// get list of apps that satisfy intent
    	boolean addedHeader = false;
        for (ResolveInfo ri : info) {
        	Drawable dIcon = null;
        	String appLabel = "";
        	try {
        		dIcon = manager.getApplicationIcon(ri.activityInfo.packageName);
        		appLabel = (String)manager.getApplicationLabel(
        				ri.activityInfo.applicationInfo);
        	} catch (NameNotFoundException ignored) {}
        	if (!addedHeader) {
                addToChooser(null, "Chooser Apps", "x", "x", AppType.HEADER);
                addedHeader = true;
        	}
        	addToChooser(
        			dIcon,
        			appLabel,
        			ri.activityInfo.packageName,
        			ri.activityInfo.name,
        			AppType.APP);
        }
	}

	private void addSystemRingtonesToChooser() {

		// header
        addToChooser(null, "System Sounds", "x", "x", AppType.HEADER);
		// Default
        if (!mHostFragmentType.equals(FragmentTypeRT.SETTINGS)) {
	        Pair<Uri, String> uriAndSongName =
			        RTUpdateFragment.getSpecificSongNameIfDefault(
			                getActivity(),
					        Uri.parse(RTPrefsDO.getDefaultRingtone()));
	        String appLabel = uriAndSongName.second;
        	addToChooser(
			        null, appLabel, AutomatonAlert.DEFAULT,
			        AutomatonAlert.DEFAULT, AppType.SYSTEM);
        }
		// Silent
    	addToChooser(
			    null, AutomatonAlert.SILENT_LABEL, AutomatonAlert.SILENT,
			    AutomatonAlert.SILENT, AppType.SYSTEM);
		//TODO: BLOCK SMS/MMS
		// Block
//    	if (mHostFragmentType.equals(FragmentTypeRT.TEXT)) {
//        	addToChooser(
//                  null, AutomatonAlert.BLOCK_SMS_MMS_LABEL, AutomatonAlert.BLOCK,
//                  AutomatonAlert.BLOCK, AppType.SYSTEM);
//    	}
		// All, Ringtones Alarms, Notifications
        for (String header : mSysRingtoneTypeHeader) {
        	addToChooser(null/*defaultDrawable*/, header, "x", "x", AppType.SYSTEM);
        }
	}

	private void addToChooser(
			Drawable iconId, String appLabel,
			String packageName, String appName, AppType appType) {

    	HashMap<String, Object> map = new HashMap<String, Object>(5);
        map.put(ICON_ID, iconId);
        map.put(APP_LABEL, appLabel);
        map.put(PACKAGE_NAME, packageName);
        map.put(APP_NAME, appName);
        map.put(APP_TYPE, appType.name());
        mChoosers.add(map);
	}

	public interface IRTChooserFragmentListener {
		public void updateRingtone(
				Dialog dialog, Fragment ruFragment, String sourceType, String song, Uri uri);
	}
}
