package com.aldersonet.automatonalert;

import android.Manifest;
import android.app.Application;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.aldersonet.automatonalert.Account.AccountDO;
import com.aldersonet.automatonalert.Account.Accounts;
import com.aldersonet.automatonalert.Activity.ContactFreeFormListActivity;
import com.aldersonet.automatonalert.Alarm.AlarmPendingIntent;
import com.aldersonet.automatonalert.Alarm.AlarmPendingIntent.ApiSubType;
import com.aldersonet.automatonalert.Alarm.AlarmPendingIntents;
import com.aldersonet.automatonalert.Alert.AlertItemDO;
import com.aldersonet.automatonalert.Alert.AlertItems;
import com.aldersonet.automatonalert.Alert.ContactAlert;
import com.aldersonet.automatonalert.Email.EmailGetSemaphores;
import com.aldersonet.automatonalert.Fragment.RTUpdateFragment;
import com.aldersonet.automatonalert.PostAlarm.PostAlarmDO;
import com.aldersonet.automatonalert.Preferences.ContactListPrefsDO;
import com.aldersonet.automatonalert.Preferences.GeneralPrefsDO;
import com.aldersonet.automatonalert.Preferences.RTPrefsDO;
import com.aldersonet.automatonalert.Provider.AutomatonAlertProvider;
import com.aldersonet.automatonalert.SMS.AccountSmsDO;
import com.aldersonet.automatonalert.Service.AutomatonAlertService;
import com.aldersonet.automatonalert.SoundBomb.SoundBombs;
import com.aldersonet.automatonalert.Util.Utils;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.config.ACRAConfiguration;
import org.acra.config.ACRAConfigurationException;
import org.acra.config.ConfigurationBuilder;
import org.acra.sender.HttpSender;
import org.apache.k9.Header;

import java.util.ArrayList;
import java.util.List;

@ReportsCrashes(
        formUri =
                "https://aldersonet.cloudant.com/acra-contactringtones/_design/acra-storage/_update/report"
)

public class AutomatonAlert extends Application {

	public static final String TAG = "AutomatonAlert";

	// Indispensable variables also seen in:
	//      strings.xml
	//      AutomatonAlertProvider.java - database
	//      OurDir.java - filesystem directory for our files
	public static final boolean RTOnly = true;
	public static String VERSION = "";
	public static AutomatonAlert THIS = null;
	public static String mAppTitle;
	public static String mOurDirectoryName;
	public static String mOurOldDirectoryName;

	public static final String ANDROID_INTENT_ACTION_BOOT_COMPLETED 		= "android.intent.action.BOOT_COMPLETED";
	public static final String ANDROID_PROVIDER_TELEPHONY_SMS_RECEIVED 		= "android.provider.Telephony.SMS_RECEIVED";
	public static final String ANDROID_PROVIDER_TELEPHONY_WAP_PUSH_RECEIVED = "android.provider.Telephony.WAP_PUSH_RECEIVED";
	public static final String MMS_MIME_TYPE								= "application/vnd.wap.mms-message";

	public static final String ACTION_GOTO_TEXT_PHONE_EMAIL_APP = "com.aldersonet.automatonalert.Util.message.ACTION_GOTO_TEXT_PHONE_EMAIL_APP";
	public static final String ACTION_EMAIL_CHECK_MAIL 		= "com.aldersonet.automatonalert.Mail.message.ACTION_EMAIL_CHECK_MAIL";
	public static final String ACTION_EMAIL_RESCHEDULE_POLL = "com.aldersonet.automatonalert.Mail.message.ACTION_EMAIL_RESCHEDULE_POLL";
	public static final String ACTION_EMAIL_ACCOUNTS_CHANGE = "com.aldersonet.automatonalert.Mail.message.ACTION_EMAIL_ACCOUNTS_CHANGE";
	public static final String ACTION_CANCEL 				= "com.aldersonet.automatonalert.message.ACTION_CANCEL";
	public static final String ACTION_RESTART_SERVICE		= "com.aldersonet.automatonalert.Service.message.ACTION_RESTART_SERVICE";
	public static final String ACTION_STOP_SERVICE_USER		= "com.aldersonet.automatonalert.Service.message.ACTION_STOP_SERVICE_NOW";
	public static final String ACTION_NOTIFY				= "com.aldersonet.automatonalert.message.ACTION_NOTIFY";
	public static final String ACTION_FOREGROUND_BACKGROUND = "com.aldersonet.automatonalert.message.ACTION_FOREGROUND_BACKGROUND";

	/////////////////////////////////////////////
	// KEEP THESE AT ALL COSTS AND DO NOT CHANGE
	public static final String ACTION_DO_NOTIFICATION_ITEM_ALERT 			= "com.aldersonet.automatonalert.Alert.ACTION_DO_NOTIFICATION_ITEM_ALERT";
	public static final String ALERT_TURN_OFF_ALL_NOTIFICATION_ITEM_SOUNDS	= "com.aldersonet.automatonalert.Alert.ACTION_TURN_OFF_ALL_NOTIFICATION_ITEM_SOUNDS";
	public static final String ALERT_TURN_OFF_NOTIFICATION_ITEM_SOUND		= "com.aldersonet.automatonalert.Alert.ACTION_TURN_OFF_NOTIFICATION_ITEM_SOUND";
	/////////////////////////////////////////////

	public static final String ALARM_ALERT_EVENT			= "com.aldersonet.automatonalert.Alert.ALARM_ALERT_EVENT_RECEIVER_NEW";
	public static final String ALARM_ALERT_SNOOZE_ALARM		= "com.aldersonet.automatonalert.Alert.ALARM_ALERT_SNOOZE_ALARM";
	public static final String ALARM_ALERT_TURN_OFF_ALARM	= "com.aldersonet.automatonalert.Alert.ALARM_ALERT_TURN_OFF_ALARM";
	public static final String GC_POLL = "com.aldersonet.automatonalert.Alert.GC_POLL";
	public static final String WIDGET_UPDATE_2X1			= "com.aldersonet.automatonalert.Utils.WIDGET_UPDATE_2X1";
	public static final String WIDGET_UPDATE_3X1			= "com.aldersonet.automatonalert.Utils.WIDGET_UPDATE_3X1";
	public static final String WIDGET_ALL_SILENT_2X1		= "com.aldersonet.automatonalert.Utils.WIDGET_ALL_SILENT_2X1";
	public static final String WIDGET_ALL_SILENT_3X1		= "com.aldersonet.automatonalert.Utils.WIDGET_ALL_SILENT_3X1";
	public static final String WIDGET_PAUSE_ALERT_ALARM_2X1	= "com.aldersonet.automatonalert.Utils.WIDGET_PAUSE_ALERT_ALARM_2X1";
	public static final String WIDGET_PAUSE_ALERT_ALARM_3X1	= "com.aldersonet.automatonalert.Utils.WIDGET_PAUSE_ALERT_ALARM_3X1";
	public static final String WIDGET_OVERRIDE_VOL_TOGGLE_2X1 = "com.aldersonet.automatonalert.Utils.WIDGET_OVERRIDE_VOL_TOGGLE_2X1";
	public static final String ALERT_REMINDER_EVENT          = "com.aldersonet.automatonalert.Utils.ALERT_REMINDER_EVENT";

	public static final int ACTION_EMAIL_ACCOUNTS_CHANGE_WHAT 	= 3;
	public static final int ACTION_CANCEL_WHAT 					= 4;
	public static final int ACTION_RESTART_SERVICE_WHAT			= 5;
	public static final int ACTION_STOP_SERVICE_WHAT			= 6;

	public static final int ALERT_LIST_NEW					= 1;
	public static final int ALERT_LIST_FAVED				= 2;
	public static final int NOTIFICATION_BAR_PHONE_ALERT	= 3;
	public static final int NOTIFICATION_BAR_TEXT_ALERT 	= 4;
	public static final int NOTIFICATION_BAR_EMAIL_ALERT	= 5;

	public static final long MAX_TIME_TO_BE_CONSIDERED_POLLED_ACCOUNT = 15 * 60 * 1000;

	public static final long MAX_TIME_TO_KEEP_DONT_SHOW_IN_LIST_ITEMS = 24 * 60 * 60 * 1000;

	public static final String MESSAGE_HASHMAP_TYPE			= "message.HashMap<String,String>";

	public static String FRAGMENT_TYPE = "fragmentType";

	public static boolean DEBUG = false;

    // these point to fragments that are part of Settings
    public RTUpdateFragment mRTDefaultTextFragment;
    public RTUpdateFragment mRTDefaultNewFragment;
    public RTUpdateFragment mRTFreeFormFragment;

    // for licensed code
	public Header header = new Header(DEBUG, TAG, this);

	public final static String ACCOUNT_NAME = "Account Name";
	public final static String ACCOUNT_KEY = "Account";
	public final static String ACCOUNT_ID = "Account Id";
	public final static String UID = "Uid";
	public final static String DATE = "Date";
	public final static String FROM = "From";
	public final static String TO = "To";
	public final static String CC = "Cc";
	public final static String BCC = "Bcc";
	public final static String SUBJECT = "Subject";
	public final static String SMS_BODY = "Message";
	public final static String QUOTED_PRINTABLE = "quoted-printable";
	public final static String BASE64 = "base64";
	public final static String ACTION = "action";

	// BodyStructure
	public final static int PLAIN = 0, HTML = 1;
	public final static int SUBTYPE = 1, CHARSET = 2, ENCODING = 6,
			LEVEL = 7, POSITION = 8, FIELDLENGTH = POSITION + 1;

	public final static String[] HEADERS_FOR_VIEW =
			{SUBJECT, SMS_BODY, TO, CC, BCC, FROM, DATE, ACCOUNT_KEY };
	public final static String[] ALERT_INBOX_HEADERS_FOR_VIEW =
			{ACCOUNT_KEY, TO, CC, BCC, SUBJECT, SMS_BODY };
	public final static String[] HEADERS_DONT_SEARCH = {
            ACCOUNT_KEY,
            UID,
            ContactAlert.TAG_MESSAGE_SOURCE_HEADER,
            ContactsContract.Contacts.LOOKUP_KEY,
            DATE
    };

	public static final String M_MODE   = "mMode";
	public static float ALPHA_DISABLED  = 0.65f;

	public static final String TRUE 		= "true";
	public static final String FALSE 		= "false";
	public static final String UPDATE 		= "update";
	public static final String ADD 			= "add";
	public static final String POS 			= "pos";
	public static final String TOP 			= "top";
	public static final String SNOOZE 		= "snooze";
	public static final String REPEAT		= "repeat";
	public static final String SILENT       = "silent";
	public static final String DEFAULT      = "default";
	public static final String ALWAYS       = "always";
	public static final String NEVER        = "never";
	public static final String ONLY         = "only";
	public static final String SOFT         = "soft";
	public static final String HARD         = "hard";
	public static final String NORMAL       = "normal";
	public static final String MODE         = "mode";
	public static final String REQUEST_CODE = "requestCode";
	public static final String BLOCK        = "block";

	public static final String OK_LABEL             = "Ok";
	public static final String CANCEL_LABEL         = "Cancel";
	public static final String TEST_LABEL           = "Test";
	public static final String SILENT_LABEL         = "Silent";
	public static final String DEFAULT_LABEL        = "Default";
	public static final String BLOCK_SMS_MMS_LABEL  = "Block SMS/MMS";

	public static final String CONTENT_PREFIX   = "content://";
	public static final String TECH_EMAIL_ADDR  = "manager@aldersonet.com";

	public static final String HTML_FONT_COLOR_RED_START_TAG  	= "<font color=\"red\">";
	public static final String HTML_FONT_END_TAG 				= "</font>";
	public static final String HTML_ESCAPED_LESS_THAN 			= "\\&lt;";
	public static final String HTML_ESCAPED_GREATER_THAN 		= "\\&gt;";
	public static final String HTML_ABOUT						= "<u>About</u>";
	public static final String HTML_LICENSES					= "<u>Licenses</u>";
	public static final String HTML_HELP						= "<u>Help</u>";

    //////////////////
	private static AlarmPendingIntents mAlarmPendingIntents;
	private static SoundBombs mSoundBombs;
	private static EmailGetSemaphores mMailGetSemaphores;
	private static ContentProviderClient mProvider;
	public ContactFreeFormListActivity mContactFreeFormListActivity;

	// for text and email notifications
	public static ArrayList<String> mEmails = new ArrayList<>();
	public static ArrayList<String> mTexts = new ArrayList<>();

	// In-App Purchases
	public static boolean mDefunctUpgrade;
	public static boolean mIntroductoryUnlimitedUpgrade;
	private static boolean mHasDevelopersCode;

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    static String[] DEVS =
            {"24044348574481366549",
                    "0102648994"};

	public static boolean hasDevelopersCode() {
        if (mHasDevelopersCode) return true;

        for (String dev : DEVS) {
            getDeveloperCodeContact(dev);
            if (mHasDevelopersCode) return true;
        }

        return false;
	}

	private static void getDeveloperCodeContact(String code) {
		Uri phoneUri = Uri.withAppendedPath(
				ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
				Uri.encode(code));

        Cursor cursor = null;
        cursor = AutomatonAlert.THIS.getContentResolver().query(
                phoneUri,
                new String[] {
                        ContactsContract.Contacts.LOOKUP_KEY,
                        ContactsContract.Contacts.DISPLAY_NAME
                },
                null,
                null,
                null);
        try {
			if (cursor != null && cursor.moveToFirst()) {
				mHasDevelopersCode = true;
			}
		}
		catch (Exception ignored) {}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        // The following line triggers the initialization of ACRA
        final ACRAConfiguration config;
        try {
            config = new ConfigurationBuilder(this)
                    .setReportType(HttpSender.Type.JSON)
                    .setHttpMethod(HttpSender.Method.POST)
                    .setFormUriBasicAuthLogin("berstereethentlentseetri")
                    .setFormUriBasicAuthPassword("ivWsdI7CT0bsYJ8e5N6yLoUu")
                    .setCustomReportContent(
                            ReportField.APP_VERSION_CODE,
                            ReportField.APP_VERSION_NAME,
                            ReportField.ANDROID_VERSION,
                            ReportField.PACKAGE_NAME,
                            ReportField.REPORT_ID,
                            ReportField.BUILD,
                            ReportField.STACK_TRACE)
                    .setReportingInteractionMode(ReportingInteractionMode.TOAST)
                    .setResToastText(R.string.acra_toast)
                    .build();
            ACRA.init(this, config);
        } catch (ACRAConfigurationException e) {
            e.printStackTrace();
            Log.e(TAG, "ACRA error reporting is not initialized and is not functional");
        }
    }

    @Override
	public void onCreate() {
		super.onCreate();
		THIS = this;
		mAppTitle = getResources().getString(R.string.app_title);
		mOurDirectoryName = getResources().getString(R.string.app_directory);
		mOurOldDirectoryName = getResources().getString(R.string.app_old_directory);
	}

    public static boolean mIsInitialized = false;
    public void initializeApp() {
        if (mIsInitialized) return;
        mIsInitialized = true;

        // load preferences
        populateAppData();

        // make sure we have one: create and save to DB
        AccountSmsDO accountSms = (AccountSmsDO)Accounts.get(AccountSmsDO.SMS_KEY);
        if (accountSms == null) {
            accountSms = new AccountSmsDO();
            accountSms.setSaveToList(false);
            accountSms.setMarkAsSeen(false);
            accountSms.save();
        }

        // start
        // start our service
        final Intent intent = new Intent(this, AutomatonAlertService.class);
        startService(intent);

        // for the "About" box
        try {
            VERSION =
                    getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            VERSION = "<error getting version>";
        }
    }

    private boolean hasPermission(String perm) {
        return ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED;
    }

    public static final int CRITICAL_PERMISSIONS       = 0x0001;
    public static final int NON_CRITICAL_PERMISSIONS   = 0x0010;
    public static final int BACKUP_RESTORE_PERMISSIONS = 0x0100;

    public List<String> checkPermissions(int permissions) {
        List<String> list = new ArrayList<>();

        if ((permissions & CRITICAL_PERMISSIONS) != 0) {
            if (!hasPermission(Manifest.permission.READ_CONTACTS)) {
                list.add(Manifest.permission.READ_CONTACTS);
            }
            if (!hasPermission(Manifest.permission.READ_SMS)) {
                list.add(Manifest.permission.READ_SMS);
            }
        }
        if ((permissions & NON_CRITICAL_PERMISSIONS) != 0 ||
                (permissions & BACKUP_RESTORE_PERMISSIONS) != 0) {
            if (!hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                list.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }

        return list;
    }

    public static AlarmPendingIntents getAPIs() {
        if (mAlarmPendingIntents == null) {
            populateAlarmPendingIntents();
        }
		return mAlarmPendingIntents;
	}

	public static SoundBombs getSoundBombs() {
		if (mSoundBombs == null) {
			mSoundBombs = new SoundBombs();
		}
		return mSoundBombs;
	}

	public static EmailGetSemaphores getMailGetSemaphores() {
		if (mMailGetSemaphores == null) {
			mMailGetSemaphores = new EmailGetSemaphores();
		}
		return mMailGetSemaphores;
	}

	public static boolean isDuplicateAccount(AccountDO aNew) {
		ArrayList<AccountDO> accounts = Accounts.get();

		for (AccountDO aHave : accounts) {
			if (Utils.equals(aHave, aNew)) {
				return true;
			}
		}
		return false;
	}

	private static void setAlarms() {
		// set current alarms from existing AlertItem's
		ArrayList<AlertItemDO> list = AlertItems.getAlarms(true/*currentOnly*/);
		for (AlertItemDO alertItem : list) {
			alertItem.setAlarm(ApiSubType.ALARM);
		}

		/////
		// set snoozed alarms that are stored in the db
		/////
		ArrayList<PostAlarmDO> postAlarms = PostAlarmDO.get();
		AlertItemDO alertItem = null;
		long now = System.currentTimeMillis();
		for (PostAlarmDO postAlarm : postAlarms) {
			alertItem = AlertItems.get(postAlarm.getAlertItemId());
			if (alertItem == null) {
				// don't schedule
				postAlarm.delete();
				continue;
			}
			long next = postAlarm.getNextAlarm();
			if (next < now) {
				postAlarm.delete();
			}
			else {
				AlertItemDO.setSnooze(alertItem, postAlarm);
			}
		}
	}

	public static ContentProviderClient getProvider() {
		if (mProvider == null) {
			ContentResolver resolver = AutomatonAlert.THIS.getContentResolver();
			if (resolver != null) {
				mProvider = resolver.acquireContentProviderClient(
						AutomatonAlertProvider.AUTHORITY);
			}
		}

		return mProvider;

	}

	private static void populateAlarmPendingIntents() {
		if (mAlarmPendingIntents == null) {
			mAlarmPendingIntents =	new AlarmPendingIntents();
		}
		else {
			// cancel any leftovers
			for (AlarmPendingIntent api : mAlarmPendingIntents) {
				// not api.findCancelRemovePendingIntentsPostAlarms
				// because we want to keep PostAlarmDO data
				api.cancelAlarm();
			}
			mAlarmPendingIntents.clear();
		}

		setAlarms();
	}

	public static void populateAppData() {
		doLegacySettingsCleanup();

		// we may be coming in after a database restore.
		// reset single record data objects
		GeneralPrefsDO.mFlagThatSaysWeHavePopulatedFromDb = false;
		RTPrefsDO.mFlagThatSaysWeHavePopulatedFromDb = false;
		ContactListPrefsDO.mFlagThatSaysWeHavePopulatedFromDb = false;

		// this may crash on install because of
		// timing issues. Don't crash
		try {
			populateAlarmPendingIntents();
		} catch (Exception ignored) {}

		getSoundBombs();
		getMailGetSemaphores();

		// don't bomb if there's a timing issue
		// on install with creating the db
		try {
			// first call will init in each
			GeneralPrefsDO.isFlagThatSaysWeHavePopulatedFromDb();
			RTPrefsDO.isFlagThatSaysWeHavePopulatedFromDb();
		} catch (Exception ignored) {}
	}

	@Override
	protected void finalize() throws Throwable {
		AutomatonAlertService.cancelAllAlarms();
		super.finalize();
	}

	private static void doLegacySettingsCleanup() {
		//davedel -- comment this out for prod
//		AutomatonAlertProvider.AutomatonAlertDBOpenHelper db =
//				new AutomatonAlertProvider.AutomatonAlertDBOpenHelper(
//						AutomatonAlert.THIS);
//		SQLiteDatabase database = db.getWritableDatabase();
//		db.onUpgrade(database, 1, 1);
//		db.close();

//		nvGetDelete(NameValueDataDO.ALERT_LIST_DELETE_DONT_SHOW);
//		nvGetDelete(NameValueDataDO.ALERT_LIST_SAVE_DONT_SHOW);
//		nvGetDelete(NameValueDataDO.ALERT_LIST_CLEAR_REMINDER_DONT_SHOW);
//		nvGetDelete(NameValueDataDO.ALERT_LIST_CLEAR_SNOOZE_DONT_SHOW);
//		nvGetDelete(NameValueDataDO.ALERT_LIST_CLEAR_REPEAT_DONT_SHOW);
//		nvGetDelete(NameValueDataDO.RT_ERROR_NO_CONTACT_EMAIL_DONT_SHOW);
//		nvGetDelete(NameValueDataDO.RT_WARNING_BLOCKED_MESSAGE_DONT_SHOW);
//		nvGetDelete(NameValueDataDO.RT_WARNING_LONG_SONG_DURATION_DONT_SHOW);
//		nvGetDelete(NameValueDataDO.RT_WARNING_ON_DELETE_DONT_SHOW);
//		nvGetDelete(NameValueDataDO.RT_WARNING_ON_FREEFORM_DELETE_DONT_SHOW);
//		nvGetDelete(NameValueDataDO.RT_WARNING_ON_ALARM_DELETE_DONT_SHOW);
//		nvGetDelete(NameValueDataDO.RT_ERROR_NO_ACCOUNTS_SET_UP_DONT_SHOW);
	}

//	private static void nvGetDelete(String key) {
//		NameValueDataDO nv = NameValueDataDO.get(key, null);
//		if (nv != null) nv.delete();
//	}
	//davedel <<
}

