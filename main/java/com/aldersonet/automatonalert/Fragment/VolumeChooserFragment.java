package com.aldersonet.automatonalert.Fragment;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.SeekBar;

import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Preferences.RTPrefsDO;
import com.aldersonet.automatonalert.Preferences.RTSettingsFragment;
import com.aldersonet.automatonalert.R;
import com.aldersonet.automatonalert.Util.Utils;

import java.util.List;

public class VolumeChooserFragment extends DialogFragment {

	// these values are stored in db
	public enum VolumeTypes {
		ringtone,
		notification,
		alarm
	}
	CheckBox mRingtoneCheckBox;
	SeekBar mRingtoneSeeker;
	CheckBox mNotificationCheckBox;
	SeekBar mNotificationSeeker;
	CheckBox mAlarmsCheckBox;
	SeekBar mAlarmsSeeker;
	CheckBox mCustomCheckBox;
	SeekBar mCustomSeeker;
	int mRingtoneVolume;
	int mRingtoneMax;
	int mNotificationVolume;
	int mNotificationMax;
	int mAlarmVolume;
	int mAlarmMax;
	int mCustomVolume;
	int mCustomMax;
	CheckBoxOnClickListener mCheckBoxOnClickListener = new CheckBoxOnClickListener();
	SeekBarOnChangeListener mSeekBarOnChangeListener = new SeekBarOnChangeListener();

	Activity mListener;
	RTSettingsFragment mFragment;

	AudioManager mAm;

	public static VolumeChooserFragment showInstance(AppCompatActivity activity) {
		final FragmentManager fm = activity.getSupportFragmentManager();
		final VolumeChooserFragment dialog = new VolumeChooserFragment();

		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				dialog.show(fm, "VolumeChooserFragment" + "whatever");
			}
		});

		return dialog;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		mListener = activity;
		mFragment = findOurFragment();
	}

	@Override
	public void onDetach() {
		super.onDetach();
		if (mFragment != null) {
			mFragment.mVolumeChooserFragment = null;
		}
		mListener = null;
	}

	private RTSettingsFragment findOurFragment() {
		if (mListener != null) {
			List<Fragment> list =
					((AppCompatActivity)mListener).getSupportFragmentManager().getFragments();
			for (Fragment fragment : list) {
				if (fragment instanceof RTSettingsFragment) {
					RTSettingsFragment sFragment = (RTSettingsFragment) fragment;
					if (sFragment.mVolumeChooserFragment != null
							&& sFragment.mVolumeChooserFragment == this) {
						return sFragment;
					}
				}
			}
		}

		return null;
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Dialog dialog = super.onCreateDialog(savedInstanceState);
		dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);

		return dialog;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		mAm = (AudioManager)getActivity().getSystemService(Context.AUDIO_SERVICE);
		// Inflate the layout for this fragment
		View returnView = inflater.inflate(R.layout.volume_chooser, container, false);
		setViewPointersAndTags(returnView);
		setViewComponents();
		return returnView;
	}

	private void setViewPointersAndTags(View v) {
		mRingtoneCheckBox = (CheckBox)v.findViewById(R.id.vc_ringtone_checkbox);
		mRingtoneSeeker = (SeekBar)v.findViewById(R.id.vc_ringtone_seeker);
		mNotificationCheckBox  = (CheckBox)v.findViewById(R.id.vc_notification_checkbox);
		mNotificationSeeker = (SeekBar)v.findViewById(R.id.vc_notification_seeker);
		mAlarmsCheckBox  = (CheckBox)v.findViewById(R.id.vc_alarms_checkbox);
		mAlarmsSeeker = (SeekBar)v.findViewById(R.id.vc_alarms_seeker);
		mCustomCheckBox  = (CheckBox)v.findViewById(R.id.vc_custom_checkbox);
		mCustomSeeker = (SeekBar)v.findViewById(R.id.vc_custom_seeker);

		mRingtoneSeeker.setTag(AudioManager.STREAM_RING);
		mNotificationSeeker.setTag(AudioManager.STREAM_NOTIFICATION);
		mAlarmsSeeker.setTag(AudioManager.STREAM_ALARM);
		// -1 identifies the seekBar as Custom
		mCustomSeeker.setTag(-1);
	}

	private static int BLUE =
			AutomatonAlert.THIS.getResources().getColor(android.R.color.holo_blue_dark);
	private static int BLACK =
			AutomatonAlert.THIS.getResources().getColor(android.R.color.black);

	private void setCheckedUnchecked(
			boolean ringtone,
			boolean notification,
			boolean alarms,
			boolean custom) {

		mRingtoneCheckBox.setChecked(ringtone);
		mNotificationCheckBox.setChecked(notification);
		mAlarmsCheckBox.setChecked(alarms);
		mCustomCheckBox.setChecked(custom);
		setBoldUnBold(ringtone, notification, alarms, custom);
	}
	private void setBoldUnBold(
			boolean ringtone,
			boolean notification,
			boolean alarms,
			boolean custom) {

		mRingtoneCheckBox.setTextColor(ringtone ? BLUE:BLACK);
		mNotificationCheckBox.setTextColor(notification ? BLUE:BLACK);
		mAlarmsCheckBox.setTextColor(alarms ? BLUE:BLACK);
		mCustomCheckBox.setTextColor(custom ? BLUE:BLACK);
	}

	private void setCheckBoxesAndSave(View v) {
		setCheckBoxesAndSave(((CheckBox) v).getText().toString(), true/*saveIt*/);

	}

	private void setCheckBoxesAndSave(String whichOne, boolean saveIt) {
		boolean isCustom = false;
		whichOne = whichOne.toLowerCase();

		// set checkboxes
		if (whichOne.equals(VolumeTypes.ringtone.name())) {
			setCheckedUnchecked(true, false, false, false);
		}
		else if (whichOne.equals(VolumeTypes.notification.name())) {
			setCheckedUnchecked(false, true, false, false);
		}
		else if (whichOne.equals(VolumeTypes.alarm.name())) {
			setCheckedUnchecked(false, false, true, false);
		}
		// custom
		else {
			setCheckedUnchecked(false, false, false, true);
			isCustom = true;
		}

		// save defaultVolume if requested
		if (saveIt) {
			if (isCustom) {
				saveCustomVolumeIfChecked(mCustomSeeker.getProgress());
			}
			else {
				saveNonCustomVolume(whichOne);
			}
		}
	}

	private void saveNonCustomVolume(String whichOne) {
		RTPrefsDO.setDefaultVolume(whichOne);
		RTPrefsDO.save();
		setCallerSummary(whichOne);
	}

	private void saveCustomVolumeIfChecked(int progress) {
		if (mCustomCheckBox.isChecked()) {
			String sProgress = progress + "";
			RTPrefsDO.setDefaultVolume(sProgress);
			RTPrefsDO.save();
			setCallerSummary(sProgress);
		}
	}

	private void setCallerSummary(String value) {
		if (mFragment != null) {
			mFragment.setDefaultVolumeSummary(value);
		}
	}

	class CheckBoxOnClickListener implements CheckBox.OnClickListener {
		@Override
		public void onClick(View v) {
			setCheckBoxesAndSave(v);
		}
	}

	class SeekBarOnChangeListener implements SeekBar.OnSeekBarChangeListener {
		boolean mQuiet;

		@Override
		public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			Integer stream = (Integer) seekBar.getTag();
			if (stream != null) {
				// CUSTOM
				if (stream == -1) {
					if (fromUser
						/*	&& !mQuiet*/) {
						mAm.playSoundEffect(
								AudioManager.FX_KEYPRESS_STANDARD,
								(float) progress / RTPrefsDO.INTERNAL_VOLUME_MAX);
						saveCustomVolumeIfChecked(progress);
					}

				// RINGTONE/NOTIFICATION/ALARM
				} else {
					mAm.setStreamVolume(
							stream,
							progress,
							AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_VIBRATE);
				}
			}
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			mQuiet = true;
		}

		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			mQuiet = false;
		}
	}

	private void setProgress(SeekBar seekBar, int inVolume, int inMax) {
		int max = seekBar.getMax();
		if (max != inMax) {
			seekBar.setMax(inMax);
		}
		seekBar.setProgress(inVolume);
	}

	private void setViewComponents() {
		mRingtoneCheckBox.setChecked(false);

		mRingtoneCheckBox.setChecked(false);
		mRingtoneVolume = mAm.getStreamVolume(AudioManager.STREAM_RING);
		mRingtoneMax = mAm.getStreamMaxVolume(AudioManager.STREAM_RING);
		setProgress(mRingtoneSeeker, mRingtoneVolume, mRingtoneMax);
		mRingtoneCheckBox.setOnClickListener(mCheckBoxOnClickListener);
		mRingtoneSeeker.setOnSeekBarChangeListener(mSeekBarOnChangeListener);

		mNotificationCheckBox.setChecked(false);
		mNotificationVolume = mAm.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
		mNotificationMax = mAm.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
		setProgress(mNotificationSeeker, mNotificationVolume, mNotificationMax);
		mNotificationCheckBox.setOnClickListener(mCheckBoxOnClickListener);
		mNotificationSeeker.setOnSeekBarChangeListener(mSeekBarOnChangeListener);

		mAlarmsCheckBox.setChecked(false);
		mAlarmVolume = mAm.getStreamVolume(AudioManager.STREAM_ALARM);
		mAlarmMax = mAm.getStreamMaxVolume(AudioManager.STREAM_ALARM);
		setProgress(mAlarmsSeeker, mAlarmVolume, mAlarmMax);
		mAlarmsCheckBox.setOnClickListener(mCheckBoxOnClickListener);
		mAlarmsSeeker.setOnSeekBarChangeListener(mSeekBarOnChangeListener);

		mCustomCheckBox.setChecked(false);
		mCustomCheckBox.setOnClickListener(mCheckBoxOnClickListener);
		mCustomSeeker.setOnSeekBarChangeListener(mSeekBarOnChangeListener);
		mCustomMax = RTPrefsDO.INTERNAL_VOLUME_MAX;

		// NAN? not custom, set to max/3
		mCustomVolume = Utils.getInt(RTPrefsDO.getDefaultVolume(), mCustomMax/3);
		setProgress(mCustomSeeker, mCustomVolume, mCustomMax);

		// set checkBoxes
		setCheckBoxesAndSave(RTPrefsDO.getDefaultVolume(), false/*saveIt*/);
	}

//	private Pair<Integer, Integer> getVolPair() {
//		// for string-based volume (alarm, notification, ringtone), the
//		// pair's first value is negative and +1 indexed
//		// otherwise the pair are real vol/maxVol
//		if (mRingtoneCheckBox.isChecked()) {
//			return new Pair<Integer, Integer>(-1 * (mTypes.RINGTONE.ordinal()+1), 0);
//		}
//		else if (mNotificationCheckBox.isChecked()) {
//			return new Pair<Integer, Integer>(-1 * (mTypes.NOTIFICATION.ordinal()+1), 0);
//		}
//		else if (mAlarmsCheckBox.isChecked()) {
//			return new Pair<Integer, Integer>(-1 * (mTypes.ALARMS.ordinal()+1), 0);
//		}
//		else {
//			return new Pair<Integer, Integer>(mCustomSeeker.getProgress(), mCustomSeeker.getMax());
//		}
//	}

}
