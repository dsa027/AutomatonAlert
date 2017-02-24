package com.aldersonet.automatonalert.ActionBar;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.aldersonet.automatonalert.Activity.ContactFreeFormListActivity;
import com.aldersonet.automatonalert.Activity.ContactFreeFormListActivity.FragmentTypeCL;
import com.aldersonet.automatonalert.Activity.FragmentHostActivity;
import com.aldersonet.automatonalert.Activity.FragmentHostActivity.HostFragmentType;
import com.aldersonet.automatonalert.Activity.HelpActivity;
import com.aldersonet.automatonalert.Activity.InAppPurchasesActivity;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.BackupRestore.BackupRestore;
import com.aldersonet.automatonalert.OkCancel.OkCancel;
import com.aldersonet.automatonalert.OkCancel.OkCancelDialog;
import com.aldersonet.automatonalert.OurDir.OurDir;
import com.aldersonet.automatonalert.R;
import com.aldersonet.automatonalert.Util.Utils;
import com.google.android.mms.ContentType;

import java.util.ArrayList;
import java.util.List;

import static com.aldersonet.automatonalert.Activity.GetPermissionsActivity.PERMISSIONS_REQ;

public class ActionBarDrawer {
	private Activity mActivity;
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;
	private OurArrayAdapter mDrawerAdapter;
    private ActionBarDrawerToggle mDrawerToggle;

	private float mHeaderRowHeight;
    private float mNormalTextSize;
    private float mHeaderTextSize;

	private static ArrayList<Rec> mItems = new ArrayList<>();

	static {
		// create TextViews for ContactList
		Rec rec = null;

		// spacer
		rec = new Rec("Contacts", null, null, -1);
		mItems.add(rec);

		rec = new Rec(
				"New Ringtone", ContactFreeFormListActivity.class,
				"SEARCH", R.drawable.ic_action_new_holo_blue);
		mItems.add(rec);

		rec = new Rec(
				"Edit Ringtones", ContactFreeFormListActivity.class,
				"ACTIVE", R.drawable.ic_action_edit_holo_blue_24);
		mItems.add(rec);

		rec = new Rec(
				"Favorites", ContactFreeFormListActivity.class,
				"FAVORITES", R.drawable.favorite_on_holo_light);
		mItems.add(rec);

		// spacer
		rec = new Rec("Text/Email", null, null, -1);
		mItems.add(rec);

		rec = new Rec(
				"Free-Form Alerts", ContactFreeFormListActivity.class,
				"FREEFORM", R.drawable.free_form_icon_holo_blue_24);
		mItems.add(rec);

		//davedel -- force show AlertItem's in dev
//		if (BuildConfig.DEBUG) {
//			rec = new Rec(
//					"Set Alarm", AlertListActivity.class,
//					"NEW", R.drawable.free_form_icon_holo_blue_24);
//			mItems.add(rec);
//
//			rec = new Rec(
//					"Snoozes/Repeats", AlertListActivity.class,
//					"ALARMS", R.drawable.free_form_icon_holo_blue_24);
//			mItems.add(rec);
//		}
		//davedel

		// spacer
		rec = new Rec("Quick Settings", null, null, -1);
		mItems.add(rec);

		rec = new Rec(
				"Global Ringtone", FragmentHostActivity.class,
				HostFragmentType.GLOBAL_RINGTONE.name(),
				R.drawable.ic_action_settings_24);
		mItems.add(rec);
		rec = new Rec(
				"Default Ringtone", FragmentHostActivity.class,
				HostFragmentType.DEFAULT_RINGTONE.name(),
				R.drawable.ic_action_settings_24);
		mItems.add(rec);

		rec = new Rec(
				"Default Volume", FragmentHostActivity.class,
				HostFragmentType.DEFAULT_VOLUME.name(),
				R.drawable.ic_action_settings_24);
		mItems.add(rec);

		// spacer
		rec = new Rec(null, null, null, -1);
		mItems.add(rec);

		rec = new Rec(
				"SETTINGS", FragmentHostActivity.class,
				HostFragmentType.GENERAL_SETTINGS.name(),
				R.drawable.ic_action_settings_24);
		mItems.add(rec);

		rec = new Rec(
				"BACKUP/RESTORE", FragmentHostActivity.class,
				HostFragmentType.BACKUP_RESTORE.name(),
				R.drawable.ic_settings_backup_restore_grey_24);
		mItems.add(rec);

		rec = new Rec(
				"HELP", HelpActivity.class,
				null, R.drawable.ic_action_help);
		mItems.add(rec);

		rec = new Rec(
				"FEEDBACK", HelpActivity.class,
				null, R.drawable.ic_action_email);
		mItems.add(rec);

		rec = new Rec(
				"PURCHASES", InAppPurchasesActivity.class,
				null, R.drawable.ic_action_send_now);
		mItems.add(rec);
	}

	public ActionBarDrawer(Activity activity) {
		mActivity = activity;

		// set views
		mDrawerLayout = (DrawerLayout)activity.findViewById(R.id.drawer_layout);
		mDrawerList = (ListView)activity.findViewById(R.id.left_drawer);
		mDrawerAdapter = new OurArrayAdapter(activity, 0, mItems);
		mDrawerList.setAdapter(mDrawerAdapter);
		// child click listener
		mDrawerList.setOnItemClickListener(new DrawerOnItemClickListener());
		// actionBar drawer toggle
		mDrawerToggle = new ActionBarDrawerToggle(
				mActivity,
				mDrawerLayout,
				R.drawable.ic_drawer_am_dark,
				R.string.empty_string,
				R.string.empty_string
				);
		mDrawerLayout.setDrawerListener(mDrawerToggle);
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

		// bg color
        int bg = mActivity.getResources().getColor(android.R.color.background_light);
		mDrawerList.setBackgroundColor(bg);
	}

	public ActionBarDrawerToggle getDrawerToggle() {
		return mDrawerToggle;
	}

	private class ViewHolder {
		RelativeLayout mRootView;
		ImageView mImage;
		TextView mLabel;

		View mSpacerTop;
		View mSpacerBottom;
		View mSeparator;
	}

	private class OurArrayAdapter extends ArrayAdapter<Rec> {

		OurArrayAdapter(Context context, int resource, ArrayList<Rec> items) {
			super(context, resource, items);
		}

		@NonNull
        @Override
		public View getView(int position, View convertView, @NonNull ViewGroup parent) {
			ViewHolder viewHolder;

			if (convertView == null) {
				convertView = ((LayoutInflater)mActivity
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
						.inflate(R.layout.imageview_textview_list_item, parent, false);

				viewHolder = new ViewHolder();

				viewHolder.mRootView = (RelativeLayout)convertView.findViewById(R.id.itli_root_layout);
				viewHolder.mLabel = (TextView)convertView.findViewById(R.id.itli_textview);
				viewHolder.mImage = (ImageView)convertView.findViewById(R.id.itli_imageView);

				viewHolder.mSpacerTop = convertView.findViewById(R.id.itli_spacer_top);
				viewHolder.mSpacerBottom = convertView.findViewById(R.id.itli_spacer_bottom);
				viewHolder.mSeparator = convertView.findViewById(R.id.itli_separator);

				convertView.setTag(viewHolder);

				setInitialTextViewDimensions();

			}
			else {
				viewHolder = (ViewHolder)convertView.getTag();
			}

			Rec rec = mItems.get(position);
			ViewGroup.LayoutParams lp = setViewHolderViewsToDefault(viewHolder);

			// spacer
			if (rec.mClz == null) {
				setVisibility(viewHolder, View.GONE);
				viewHolder.mRootView.setBackgroundColor(Color.LTGRAY);
				if (rec.mTitle != null) {
					viewHolder.mLabel.setText(rec.mTitle);
					viewHolder.mLabel.setTextSize(mHeaderTextSize);
					viewHolder.mLabel.setTextColor(Color.WHITE);
				}
				lp.height = Math.round(mHeaderRowHeight);
				viewHolder.mLabel.setLayoutParams(lp);
			}
			else {
				setVisibility(viewHolder, View.VISIBLE);
			}
			// no icon
			if (rec.mIconResource == -1) {
				viewHolder.mImage.setVisibility(ImageView.GONE);
				viewHolder.mLabel.setText(rec.mTitle);
			}
			// icon
			else {
				viewHolder.mImage.setImageResource(rec.mIconResource);
				viewHolder.mLabel.setText(rec.mTitle);
			}
			// settings/help/...
			if (rec.mTitle != null
					&& rec.mTitle.equals(rec.mTitle.toUpperCase())) {
//				viewHolder.mLabel.setTextColor(Color.LTGRAY);
				viewHolder.mLabel.setTextAppearance(
						mActivity,
						android.R.style.TextAppearance_Holo_Small);
			}

			return convertView;
		}
	}

	private void setInitialTextViewDimensions() {
		// GET INITIAL MEASUREMENTS
		if (mHeaderRowHeight == 0) {
			mNormalTextSize =  18;
			mHeaderTextSize = mNormalTextSize * 0.75F;

			DisplayMetrics metrics = new DisplayMetrics();
			mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
			Rect bounds = getTextStringBounds(null, null, Math.round(mNormalTextSize));
			mHeaderRowHeight = bounds.height() * metrics.density;
		}
	}

	private ViewGroup.LayoutParams setViewHolderViewsToDefault(ViewHolder viewHolder) {
		// anything that's specifically set in getView() is first
		// set to default here
		ViewGroup.LayoutParams lp =
				viewHolder.mLabel.getLayoutParams();
		lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
		viewHolder.mLabel.setLayoutParams(lp);

		viewHolder.mRootView.setBackgroundColor(Color.WHITE);
		viewHolder.mImage.setVisibility(ImageView.VISIBLE);
		viewHolder.mImage.setImageResource(0);
		viewHolder.mLabel.setText("");
		viewHolder.mLabel.setTextAppearance(mActivity, android.R.style.TextAppearance_Medium);
//		viewHolder.mLabel.setTextSize(mNormalTextSize);
        //noinspection ResourceType
        viewHolder.mLabel.setTextColor(
				Color.parseColor(
						mActivity.getResources().getString(android.R.color.holo_blue_dark)));

		return lp;
	}

	private static Rect getTextStringBounds(
			String inStr, Typeface typeface, int textSize) {

		Paint paint = new Paint();
		Rect bounds = new Rect();

		// input
		String text = "Py";
		if (!TextUtils.isEmpty(inStr)) {
			text = inStr;
		}

		// TYPEFACE
		paint.setTypeface(Typeface.DEFAULT);
		if (typeface != null) {
			paint.setTypeface(typeface);
		}

		// TEXTSIZE
		paint.setTextSize(25);
		if (textSize != -1) {
			paint.setTextSize(textSize);
		}

		paint.getTextBounds(text, 0, text.length(), bounds);

		return bounds;
	}

	private void setVisibility(ViewHolder vh, int visibility) {
		vh.mSpacerTop.setVisibility(visibility);
		vh.mSpacerBottom.setVisibility(visibility);
		vh.mSeparator.setVisibility(visibility);
	}

	private class DrawerOnItemClickListener implements AdapterView.OnItemClickListener {
		@Override
		public void onItemClick(
				AdapterView<?> parent, View view, int position, long id) {

			selectDrawerItem(mItems.get(position));
		}

		private void selectDrawerItem(Rec rec) {
			boolean isSpecialCase = false;

			if (rec.mClz == null) {
				return;
			}
			mDrawerLayout.closeDrawer(mDrawerList);

            switch (rec.mTitle) {
                case "FEEDBACK":
                    isSpecialCase = true;
                    sendEmailToDeveloper();
                    break;
                case "BACKUP/RESTORE":
                    if (Utils.inAppUpgradeNoBackupRestore(mActivity)) {
                        return;
                    }
                    // check for permission problem
                    List<String> permList =
                            AutomatonAlert.THIS.checkPermissions(
                                    AutomatonAlert.BACKUP_RESTORE_PERMISSIONS);
                    if (permList.size() > 0) {
                        showGetBackupRestoreDiskPermisions(permList);
                        return;
                    }
                    if (!OurDir.haveBackupRestoreCapability(mActivity)) {
                        BackupRestore.showNoBackupRestoreCapabilityWarning(mActivity, rec);
                        return;
                    }
                    break;
                default:
                    isSpecialCase =
                            selectPageForContactFreeFormListActivity(rec.mClz, rec.mArg);
                    break;
            }

			if (!isSpecialCase) {
				Intent intent = getDrawerIntent(mActivity, rec.mClz, rec.mArg);
				if (intent != null) {
					mActivity.startActivity(intent);
				}
			}
		}
	}

    private void showGetBackupRestoreDiskPermisions(final List<String> permList) {
        OkCancelDialog okCancelDialog = OkCancelDialog.showInstance(
                (AppCompatActivity) mActivity,
                "You need to give this app permission to read and write" +
                        " to your external drive.",
                "",
                "Cancel",
                "Give<br>Permission",
                OkCancelDialog.CancelButton.LEFT,
                OkCancelDialog.EWI.INFO
        );
        okCancelDialog.setOkCancel(
                new OkCancel() {
                    @Override
                    protected  void ok(DialogInterface dialog) {
                        ActivityCompat.requestPermissions(
                                mActivity,
                                permList.toArray(new String[permList.size()]),
                                PERMISSIONS_REQ);
                    }
                    @Override
                    protected  void cancel(DialogInterface dialog) {
                    }
                }
        );
    }

	private void sendEmailToDeveloper() {
		Intent intent = new Intent(Intent.ACTION_SENDTO);
		intent.setType(ContentType.TEXT_PLAIN);
		intent.putExtra(Intent.EXTRA_SUBJECT, "Reporting an Issue with Contact Ringtones");
		intent.setData(Uri.parse("mailto:" + AutomatonAlert.TECH_EMAIL_ADDR));
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		mActivity.startActivity(intent);
	}

	private boolean selectPageForContactFreeFormListActivity(Class clz, String param) {
		if (clz != ContactFreeFormListActivity.class) {
			return false;
		}
		FragmentTypeCL type;

		if (AutomatonAlert.THIS.mContactFreeFormListActivity != null) {
			try {
				type = FragmentTypeCL.valueOf(param);
			}
			catch (IllegalArgumentException e) {
				type = FragmentTypeCL.SEARCH;
			}
			AutomatonAlert.THIS.mContactFreeFormListActivity.
					getSupportActionBar().setSelectedNavigationItem(type.ordinal());

			return true;
		}

		return false;

	}

	public static Intent getDrawerIntent(Activity activity, Class clz, String param) {
		Intent intent = new Intent(activity.getApplicationContext(), clz);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		if (param != null) {
			intent.putExtra(AutomatonAlert.FRAGMENT_TYPE, param);
		}

		return intent;
	}

	public void openDrawer() {
		mDrawerLayout.openDrawer(GravityCompat.START);
	}

	public static class Rec {
		String mTitle;
		public Class mClz;
		public String mArg;
		int mIconResource;

		Rec(String title, Class clz, String arg, int iconResource) {
			mTitle = title;
			mClz = clz;
			mArg = arg;
			mIconResource = iconResource;
		}
	}
}
