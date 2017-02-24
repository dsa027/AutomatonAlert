package com.aldersonet.automatonalert.Provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

import com.aldersonet.automatonalert.Activity.RTUpdateActivity.FragmentTypeRT;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Preferences.NameValueDataDO;
import com.aldersonet.automatonalert.Util.Utils;

import java.util.HashMap;
import java.util.List;

public class AutomatonAlertProvider extends ContentProvider {

	public static final String TAG = "AutomatonAlertProvider";

	public static final String DATABASE_NAME = "automatonalert.db";
	public static final int DATABASE_VERSION = 4;

	public static final String SCHEME = AutomatonAlert.CONTENT_PREFIX;

	public static final String AUTHORITY =
			"com.aldersonet.provider.automatonalert";

	private static final String DEFAULT_SORT_ORDER = "timestamp DESC";

	private static final String CONTENT_DIR_TYPE =
			"vnd.android.cursor.dir/com.aldersonet.automatonalert";
	private static final String CONTENT_ITEM_TYPE =
			"vnd.android.cursor.item/com.aldersonet.automatonalert";

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ TABLES

	private static final String[] DATABASE_TABLE_NAMES = {
			"Account",
			"AlertItem",
			"NotificationItem",
			"FilterItem",
			"FilterItemAccount",
			"GeneralPrefs",
			"SourceType",
			"GroupNotification",
			"ContactInfo",
			"SourceAccount",
			"NameValueData",
			"ContactRTPrefs",
			"PostAlarm",
			"ContactListPrefs"
	};

	public static final String ACCOUNT_TABLE 			= DATABASE_TABLE_NAMES[0];
	public static final String ALERT_ITEM_TABLE 		= DATABASE_TABLE_NAMES[1];
	public static final String NOTIFICATION_ITEM_TABLE 	= DATABASE_TABLE_NAMES[2];
	public static final String FILTER_ITEM_TABLE 		= DATABASE_TABLE_NAMES[3];
	public static final String FILTER_ITEM_ACCOUNT_TABLE= DATABASE_TABLE_NAMES[4];
	public static final String GENERAL_PREFS_TABLE		= DATABASE_TABLE_NAMES[5];
	public static final String SOURCE_TYPE_TABLE		= DATABASE_TABLE_NAMES[6];
	public static final String GROUP_NOTIFICATION_TABLE	= DATABASE_TABLE_NAMES[7];
	public static final String CONTACT_INFO_TABLE		= DATABASE_TABLE_NAMES[8];
	public static final String SOURCE_ACCOUNT_TABLE		= DATABASE_TABLE_NAMES[9];
	public static final String NAME_VALUE_DATA_TABLE	= DATABASE_TABLE_NAMES[10];
	public static final String CONTACT_RT_PREFS_TABLE	= DATABASE_TABLE_NAMES[11];
	public static final String POST_ALARM_TABLE			= DATABASE_TABLE_NAMES[12];
	public static final String CONTACT_LIST_PREFS_TABLE	= DATABASE_TABLE_NAMES[13];

	public static final int ACCOUNT_TABLE_URI_ID			= 1;
	public static final int ACCOUNT_ID_URI_ID				= 2;

	public static final int ALERT_ITEM_TABLE_URI_ID			= 3;
	public static final int ALERT_ITEM_ID_URI_ID			= 4;

	public static final int NOTIFICATION_ITEM_TABLE_URI_ID	= 5;
	public static final int NOTIFICATION_ITEM_ID_URI_ID		= 6;

	public static final int FILTER_ITEM_TABLE_URI_ID		= 7;
	public static final int FILTER_ITEM_ID_URI_ID			= 8;

	public static final int FILTER_ITEM_ACCOUNT_TABLE_URI_ID= 9;
	public static final int FILTER_ITEM_ACCOUNT_ID_URI_ID	= 10;

	public static final int GENERAL_PREFS_TABLE_URI_ID		= 11;
	public static final int GENERAL_PREFS_ID_URI_ID			= 12;

	public static final int SOURCE_TYPE_TABLE_URI_ID		= 13;
	public static final int SOURCE_TYPE_ID_URI_ID			= 14;

	public static final int GROUP_NOTIFICATION_TABLE_URI_ID	= 15;
	public static final int GROUP_NOTIFICATION_ID_URI_ID	= 16;

	public static final int CONTACT_INFO_TABLE_URI_ID		= 17;
	public static final int CONTACT_INFO_ID_URI_ID			= 18;

	public static final int SOURCE_ACCOUNT_TABLE_URI_ID		= 19;
	public static final int SOURCE_ACCOUNT_ID_URI_ID		= 20;

	public static final int NAME_VALUE_DATA_TABLE_URI_ID	= 21;
	public static final int NAME_VALUE_DATA_ID_URI_ID		= 22;

	public static final int CONTACT_RT_PREFS_TABLE_URI_ID	= 23;
	public static final int CONTACT_RT_PREFS_ID_URI_ID		= 24;

	public static final int POST_ALARM_TABLE_URI_ID			= 25;
	public static final int POST_ALARM_ID_URI_ID			= 26;

	public static final int CONTACT_LIST_PREFS_TABLE_URI_ID	= 27;
	public static final int CONTACT_LIST_PREFS_ID_URI_ID	= 28;

	// account
	public static final Uri ACCOUNT_TABLE_URI  =
			Uri.parse(SCHEME + AUTHORITY + "/" + ACCOUNT_TABLE);
	public static final Uri ACCOUNT_ID_URI =
			Uri.parse(SCHEME + AUTHORITY + "/" + ACCOUNT_TABLE + "/#");

	// alert item
	public static final Uri ALERT_ITEM_TABLE_URI  =
			Uri.parse(SCHEME + AUTHORITY + "/" + ALERT_ITEM_TABLE);
	public static final Uri ALERT_ITEM_ID_URI =
			Uri.parse(SCHEME + AUTHORITY + "/" + ALERT_ITEM_TABLE + "/#");

	// notification item
	public static final Uri NOTIFICATION_ITEM_TABLE_URI  =
			Uri.parse(SCHEME + AUTHORITY + "/" + NOTIFICATION_ITEM_TABLE);
	public static final Uri NOTIFICATION_ITEM_ID_URI =
			Uri.parse(SCHEME + AUTHORITY + "/" + NOTIFICATION_ITEM_TABLE + "/#");

	// filter item
	public static final Uri FILTER_ITEM_TABLE_URI  =
			Uri.parse(SCHEME + AUTHORITY + "/" + FILTER_ITEM_TABLE);
	public static final Uri FILTER_ITEM_ID_URI =
			Uri.parse(SCHEME + AUTHORITY + "/" + FILTER_ITEM_TABLE + "/#");

	// filter item account
	public static final Uri FILTER_ITEM_ACCOUNT_TABLE_URI  =
			Uri.parse(SCHEME + AUTHORITY + "/" + FILTER_ITEM_ACCOUNT_TABLE);
	public static final Uri FILTER_ITEM_ACCOUNT_ID_URI =
			Uri.parse(SCHEME + AUTHORITY + "/" + FILTER_ITEM_ACCOUNT_TABLE + "/#");

	// general prefs
	public static final Uri GENERAL_PREFS_TABLE_URI  =
			Uri.parse(SCHEME + AUTHORITY + "/" + GENERAL_PREFS_TABLE);
	public static final Uri GENERAL_PREFS_ID_URI =
			Uri.parse(SCHEME + AUTHORITY + "/" + GENERAL_PREFS_TABLE + "/#");

	// source type
	public static final Uri SOURCE_TYPE_TABLE_URI  =
			Uri.parse(SCHEME + AUTHORITY + "/" + SOURCE_TYPE_TABLE);
	public static final Uri SOURCE_TYPE_ID_URI =
			Uri.parse(SCHEME + AUTHORITY + "/" + SOURCE_TYPE_TABLE + "/#");

	// group notification
	public static final Uri GROUP_NOTIFICATION_TABLE_URI  =
			Uri.parse(SCHEME + AUTHORITY + "/" + GROUP_NOTIFICATION_TABLE);
	public static final Uri GROUP_NOTIFICATION_ID_URI =
			Uri.parse(SCHEME + AUTHORITY + "/" + GROUP_NOTIFICATION_TABLE + "/#");

	// contact info
	public static final Uri CONTACT_INFO_TABLE_URI  =
			Uri.parse(SCHEME + AUTHORITY + "/" + CONTACT_INFO_TABLE);
	public static final Uri CONTACT_INFO_ID_URI =
			Uri.parse(SCHEME + AUTHORITY + "/" + CONTACT_INFO_TABLE + "/#");

	// source account
	public static final Uri SOURCE_ACCOUNT_TABLE_URI  =
			Uri.parse(SCHEME + AUTHORITY + "/" + SOURCE_ACCOUNT_TABLE);
	public static final Uri SOURCE_ACCOUNT_ID_URI =
			Uri.parse(SCHEME + AUTHORITY + "/" + SOURCE_ACCOUNT_TABLE + "/#");

	// name-value data
	public static final Uri NAME_VALUE_DATA_TABLE_URI  =
			Uri.parse(SCHEME + AUTHORITY + "/" + NAME_VALUE_DATA_TABLE);
	public static final Uri NAME_VALUE_DATA_ID_URI =
			Uri.parse(SCHEME + AUTHORITY + "/" + NAME_VALUE_DATA_TABLE + "/#");

	// contactRT prefs
	public static final Uri CONTACT_RT_PREFS_TABLE_URI  =
			Uri.parse(SCHEME + AUTHORITY + "/" + CONTACT_RT_PREFS_TABLE);
	public static final Uri CONTACT_RT_PREFS_ID_URI =
			Uri.parse(SCHEME + AUTHORITY + "/" + CONTACT_RT_PREFS_TABLE + "/#");

	// post alarm
	public static final Uri POST_ALARM_TABLE_URI  =
			Uri.parse(SCHEME + AUTHORITY + "/" + POST_ALARM_TABLE);
	public static final Uri POST_ALARM_ID_URI =
			Uri.parse(SCHEME + AUTHORITY + "/" + POST_ALARM_TABLE + "/#");

	// contact list prefs
	public static final Uri CONTACT_LIST_PREFS_TABLE_URI  =
			Uri.parse(SCHEME + AUTHORITY + "/" + CONTACT_LIST_PREFS_TABLE);
	public static final Uri CONTACT_LIST_PREFS_ID_URI =
			Uri.parse(SCHEME + AUTHORITY + "/" + CONTACT_LIST_PREFS_TABLE + "/#");


	private static final UriMatcher mUriMatcher;

	static {
		mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

		mUriMatcher.addURI(AUTHORITY, ACCOUNT_TABLE, ACCOUNT_TABLE_URI_ID);
		mUriMatcher.addURI(AUTHORITY, ACCOUNT_TABLE + "/#", ACCOUNT_ID_URI_ID);

		mUriMatcher.addURI(AUTHORITY, ALERT_ITEM_TABLE, ALERT_ITEM_TABLE_URI_ID);
		mUriMatcher.addURI(AUTHORITY, ALERT_ITEM_TABLE + "/#", ALERT_ITEM_ID_URI_ID);

		mUriMatcher.addURI(AUTHORITY, NOTIFICATION_ITEM_TABLE, NOTIFICATION_ITEM_TABLE_URI_ID);
		mUriMatcher.addURI(AUTHORITY, NOTIFICATION_ITEM_TABLE + "/#", NOTIFICATION_ITEM_ID_URI_ID);

		mUriMatcher.addURI(AUTHORITY, FILTER_ITEM_TABLE, FILTER_ITEM_TABLE_URI_ID);
		mUriMatcher.addURI(AUTHORITY, FILTER_ITEM_TABLE + "/#", FILTER_ITEM_ID_URI_ID);

		mUriMatcher.addURI(AUTHORITY, FILTER_ITEM_ACCOUNT_TABLE, FILTER_ITEM_ACCOUNT_TABLE_URI_ID);
		mUriMatcher.addURI(AUTHORITY, FILTER_ITEM_ACCOUNT_TABLE + "/#", FILTER_ITEM_ACCOUNT_ID_URI_ID);

		mUriMatcher.addURI(AUTHORITY, GENERAL_PREFS_TABLE, GENERAL_PREFS_TABLE_URI_ID);
		mUriMatcher.addURI(AUTHORITY, GENERAL_PREFS_TABLE + "/#", GENERAL_PREFS_ID_URI_ID);

		mUriMatcher.addURI(AUTHORITY, SOURCE_TYPE_TABLE, SOURCE_TYPE_TABLE_URI_ID);
		mUriMatcher.addURI(AUTHORITY, SOURCE_TYPE_TABLE + "/#", SOURCE_TYPE_ID_URI_ID);

		mUriMatcher.addURI(AUTHORITY, GROUP_NOTIFICATION_TABLE, GROUP_NOTIFICATION_TABLE_URI_ID);
		mUriMatcher.addURI(AUTHORITY, GROUP_NOTIFICATION_TABLE + "/#", GROUP_NOTIFICATION_ID_URI_ID);

		mUriMatcher.addURI(AUTHORITY, CONTACT_INFO_TABLE, CONTACT_INFO_TABLE_URI_ID);
		mUriMatcher.addURI(AUTHORITY, CONTACT_INFO_TABLE + "/#", CONTACT_INFO_ID_URI_ID);

		mUriMatcher.addURI(AUTHORITY, SOURCE_ACCOUNT_TABLE, SOURCE_ACCOUNT_TABLE_URI_ID);
		mUriMatcher.addURI(AUTHORITY, SOURCE_ACCOUNT_TABLE + "/#", SOURCE_ACCOUNT_ID_URI_ID);

		mUriMatcher.addURI(AUTHORITY, NAME_VALUE_DATA_TABLE, NAME_VALUE_DATA_TABLE_URI_ID);
		mUriMatcher.addURI(AUTHORITY, NAME_VALUE_DATA_TABLE + "/#", NAME_VALUE_DATA_ID_URI_ID);

		mUriMatcher.addURI(AUTHORITY, CONTACT_RT_PREFS_TABLE, CONTACT_RT_PREFS_TABLE_URI_ID);
		mUriMatcher.addURI(AUTHORITY, CONTACT_RT_PREFS_TABLE + "/#", CONTACT_RT_PREFS_ID_URI_ID);

		mUriMatcher.addURI(AUTHORITY, POST_ALARM_TABLE, POST_ALARM_TABLE_URI_ID);
		mUriMatcher.addURI(AUTHORITY, POST_ALARM_TABLE + "/#", POST_ALARM_ID_URI_ID);

		mUriMatcher.addURI(AUTHORITY, CONTACT_LIST_PREFS_TABLE, CONTACT_LIST_PREFS_TABLE_URI_ID);
		mUriMatcher.addURI(AUTHORITY, CONTACT_LIST_PREFS_TABLE + "/#", CONTACT_LIST_PREFS_ID_URI_ID);

	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ SQLITE OPEN HELPER CLASS

	public AutomatonAlertDBOpenHelper mDBOpenHelper;

	public static class AutomatonAlertDBOpenHelper extends SQLiteOpenHelper {
		boolean mTestingValidity;

		public AutomatonAlertDBOpenHelper(
				Context context, String otherDb, boolean testingValidity) {

			super(
					context,
					otherDb == null ? DATABASE_NAME : otherDb,
					null,
					DATABASE_VERSION);

			mTestingValidity = testingValidity;
		}

		public int getDataBaseVersion() {
			NameValueDataDO nv = NameValueDataDO.get(NameValueDataDO.DATABASE_VERSION, null);
			if (nv == null) {
				return 2;
			}

			return Utils.getInt(nv.getValue(), 2);
		}

		@Override
		public void onCreate(SQLiteDatabase database) {
			if (mTestingValidity){
				return;
			}

			createAllTables(database);
			createAllIndexes(database);
		}

		@Override
		public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
			if (mTestingValidity) {
				return;
			}

            for (int i = oldVersion; i < newVersion; i++) {
                switch(i) {
                    case 1: upgrade1To2(database); break;
                    case 2: upgrade2To3(database); break;
                    case 3: upgrade3To4(database); break;
                }
            }

			// true = create NAME_VALUE_DATA_TABLE on error
			if (insertDatabaseVersion(database, true) == -1) {
				// TODO: research this, as in "why is it happening?"
				insertDatabaseVersion(database, false);
			}
		}

		private void upgrade1To2(SQLiteDatabase database) {
			try {
				database.execSQL("DROP TABLE IF EXISTS " + FILTER_ITEM_TABLE);
				database.execSQL(CREATE_FILTER_ITEM_TABLE);
				database.execSQL(CREATE_FILTERITEM_NOTIFICATIONITEMID_INDEX);

				database.execSQL(CREATE_FILTER_ITEM_ACCOUNT_TABLE);
				database.execSQL(CREATE_FILTERITEMACCOUNT_ACCOUNTID_INDEX);
				database.execSQL(CREATE_FILTERITEMACCOUNT_FILTERITEMID_INDEX);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		private void upgrade2To3(SQLiteDatabase database) {
			try {
				database.execSQL(CREATE_SOURCETYPE_SOURCETYPE_INDEX);
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

        private void upgrade3To4(SQLiteDatabase database) {
            /*//davedel
            try {
                database.execSQL(CREATE_SOURCETYPE_LOOKUPKEY_SOURCETYPE_INDEX);
                database.execSQL(CREATE_GMAIL_ACCOUNT_TABLE);
                database.execSQL(CREATE_INDICES...);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            //davedel*/
        }

		private void createAllTables(SQLiteDatabase database) {
			database.execSQL(CREATE_ACCOUNT_TABLE);
			database.execSQL(CREATE_ALERT_ITEM_TABLE);
			database.execSQL(CREATE_NOTIFICATION_ITEM_TABLE);
			database.execSQL(CREATE_FILTER_ITEM_TABLE);
			database.execSQL(CREATE_FILTER_ITEM_ACCOUNT_TABLE);
			database.execSQL(CREATE_GENERAL_PREFS_TABLE);
			database.execSQL(CREATE_SOURCE_TYPE_TABLE);
			database.execSQL(CREATE_GROUP_NOTIFICATION_TABLE);
			database.execSQL(CREATE_CONTACT_INFO_TABLE);
			database.execSQL(CREATE_SOURCE_ACCOUNT_TABLE);
			database.execSQL(CREATE_NAME_VALUE_DATA_TABLE);
			database.execSQL(CREATE_CONTACT_RT_PREFS_TABLE);
			database.execSQL(CREATE_POST_ALARM_TABLE);

			// false = don't create NAME_VALUE_DATA_TABLE on error
			insertDatabaseVersion(database, false);

		}

		private int insertDatabaseVersion(
				SQLiteDatabase database, boolean createNameValueDataTable) {

			// first delete old if this is from update()
			try {
				database.execSQL(
						"delete from "
								+ NAME_VALUE_DATA_TABLE
								+ " where "
								+ NAME_VALUE_DATA_NAME
								+ " = "
								+ "'" + NameValueDataDO.DATABASE_VERSION + "'"
				);
				// manually insert database version
				database.execSQL(
						"insert into "
								+ NAME_VALUE_DATA_TABLE
								+ " ("
								+ NAME_VALUE_DATA_NAME + ", "
								+ NAME_VALUE_DATA_VALUE + ", "
								+ NAME_VALUE_DATA_TIMESTAMP
								+ ")"
								+ " values "
								+ "("
								+ "'" + NameValueDataDO.DATABASE_VERSION + "', "
								+ "'" + DATABASE_VERSION + "', "
								+ System.currentTimeMillis()
								+ ")"
				);
			} catch (SQLiteException e) {
				if (createNameValueDataTable) {
					database.execSQL(CREATE_NAME_VALUE_DATA_TABLE);
				}
				return -1;
			}

			return 0;
		}

		private void createAllIndexes(SQLiteDatabase database) {
			database.execSQL(CREATE_ACCOUNT_NAMEEMAILADDRESS_INDEX);
			database.execSQL(CREATE_ALERTITEM_ACCOUNTID_INDEX);
			database.execSQL(CREATE_ALERTITEM_TYPE_INDEX);
			database.execSQL(CREATE_ALERTITEM_UID_INDEX);
			database.execSQL(CREATE_ALERTITEM_DATEREMIND_INDEX);
			database.execSQL(CREATE_ALERTITEM_DATEEXPIRES_INDEX);
			database.execSQL(CREATE_ALERTITEM_REPEATEVERY_INDEX);
			database.execSQL(CREATE_ALERTITEM_NOTIFICATIONITEMID_INDEX);
			database.execSQL(CREATE_FILTERITEM_NOTIFICATIONITEMID_INDEX);
			database.execSQL(CREATE_FILTERITEMACCOUNT_ACCOUNTID_INDEX);
			database.execSQL(CREATE_FILTERITEMACCOUNT_FILTERITEMID_INDEX);
			database.execSQL(CREATE_ALERTITEM_STATUS_INDEX);
			database.execSQL(CREATE_SOURCETYPE_LOOKUPKEY_INDEX);
			database.execSQL(CREATE_SOURCETYPE_NOTIFICATIONITEMID_INDEX);
			database.execSQL(CREATE_SOURCETYPE_SOURCETYPE_INDEX);
			database.execSQL(CREATE_GROUPNOTIFICATION_ANDROIDGROUPID_INDEX);
			database.execSQL(CREATE_GROUPNOTIFICATION_NOTIFICATIONITEMID_INDEX);
			database.execSQL(CREATE_CONTACTINFO_LOOKUPKEY_INDEX);
			database.execSQL(CREATE_CONTACTINFO_FAVORITE_INDEX);
			database.execSQL(CREATE_SOURCE_ACCOUNT_ACCOUNT_ID_INDEX);
			database.execSQL(CREATE_SOURCE_ACCOUNT_SOURCE_TYPE_ID_INDEX);
			database.execSQL(CREATE_NAME_VALUE_DATA_NAME_INDEX);
			database.execSQL(CREATE_POST_ALARM_ALERT_ITEM_ID_INDEX);
			database.execSQL(CREATE_POST_ALARM_NOTIFICATION_ITEM_ID_INDEX);
			database.execSQL(CREATE_POST_ALARM_NEXT_ALARM_INDEX);
		}

		private void dropAllTables(SQLiteDatabase database) {
			database.execSQL("DROP TABLE IF EXISTS " + ACCOUNT_TABLE);
			database.execSQL("DROP TABLE IF EXISTS " + ALERT_ITEM_TABLE);
			database.execSQL("DROP TABLE IF EXISTS " + NOTIFICATION_ITEM_TABLE);
			database.execSQL("DROP TABLE IF EXISTS " + FILTER_ITEM_TABLE);
			database.execSQL("DROP TABLE IF EXISTS " + FILTER_ITEM_ACCOUNT_TABLE);
			database.execSQL("DROP TABLE IF EXISTS " + GENERAL_PREFS_TABLE);
			database.execSQL("DROP TABLE IF EXISTS " + SOURCE_TYPE_TABLE);
			database.execSQL("DROP TABLE IF EXISTS " + GROUP_NOTIFICATION_TABLE);
			database.execSQL("DROP TABLE IF EXISTS " + CONTACT_INFO_TABLE);
			database.execSQL("DROP TABLE IF EXISTS " + SOURCE_ACCOUNT_TABLE);
			database.execSQL("DROP TABLE IF EXISTS " + NAME_VALUE_DATA_TABLE);
			database.execSQL("DROP TABLE IF EXISTS " + CONTACT_RT_PREFS_TABLE);
			database.execSQL("DROP TABLE IF EXISTS " + POST_ALARM_TABLE);
			database.execSQL("DROP TABLE IF EXISTS " + CONTACT_LIST_PREFS_TABLE);
		}
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ ACCOUNT

	private static final String[] ACCOUNT_COLUMNS = {
			"_id",
			"name",
			"email_address",
			"password",
			"server_name",
			"server_security",
			"server_port",
			"latest_uid",
			"show_images",
			"poll",
			"type",
			"save_to_list",
			"mark_as_seen",
			"last_checked",
			"timestamp"
	};
	public static final String ACCOUNT_ID 				= ACCOUNT_COLUMNS[0];
	public static final String ACCOUNT_NAME 			= ACCOUNT_COLUMNS[1];
	public static final String ACCOUNT_EMAIL_ADDRESS 	= ACCOUNT_COLUMNS[2];
	public static final String ACCOUNT_PASSWORD 		= ACCOUNT_COLUMNS[3];
	public static final String ACCOUNT_SERVER_NAME		= ACCOUNT_COLUMNS[4];
	public static final String ACCOUNT_SERVER_SECURITY 	= ACCOUNT_COLUMNS[5];
	public static final String ACCOUNT_SERVER_PORT 		= ACCOUNT_COLUMNS[6];
	public static final String ACCOUNT_LATEST_UID 		= ACCOUNT_COLUMNS[7];
	public static final String ACCOUNT_SHOW_IMAGES 		= ACCOUNT_COLUMNS[8];
	public static final String ACCOUNT_POLL 			= ACCOUNT_COLUMNS[9];
	public static final String ACCOUNT_TYPE 			= ACCOUNT_COLUMNS[10];
	public static final String ACCOUNT_SAVE_TO_LIST 	= ACCOUNT_COLUMNS[11];
	public static final String ACCOUNT_MARK_AS_SEEN 	= ACCOUNT_COLUMNS[12];
	public static final String ACCOUNT_LAST_CHECKED		= ACCOUNT_COLUMNS[13];
	public static final String ACCOUNT_TIMESTAMP 		= ACCOUNT_COLUMNS[14];

	private static final HashMap<String, String> mAccountProjectionMap;

	static {
		mAccountProjectionMap = new HashMap<String, String>(ACCOUNT_COLUMNS.length);
		for (String column : ACCOUNT_COLUMNS) {
			mAccountProjectionMap.put(column, column);
		}
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ ALERT_ITEM

	private static final String[] ALERT_ITEM_COLUMNS = {
			"_id",
			"type",
			"uid",
			"favorite",
			"date_remind",
			"repeat_every",
			"stop_after",
			"raw_fields",
			"markup_fields",
			"notification_item_id",
			"account_id",
			"status",
			"date_expires",
			"timestamp"
	};

	public static final String ALERT_ITEM_ID 					= ALERT_ITEM_COLUMNS[0];
	public static final String ALERT_ITEM_TYPE 					= ALERT_ITEM_COLUMNS[1];
	public static final String ALERT_ITEM_UID 					= ALERT_ITEM_COLUMNS[2];
	public static final String ALERT_ITEM_FAVORITE 				= ALERT_ITEM_COLUMNS[3];
	public static final String ALERT_ITEM_DATE_REMIND 			= ALERT_ITEM_COLUMNS[4];
	public static final String ALERT_ITEM_REPEAT_EVERY 			= ALERT_ITEM_COLUMNS[5];
	public static final String ALERT_ITEM_STOP_AFTER 			= ALERT_ITEM_COLUMNS[6];
	public static final String ALERT_ITEM_RAW_FIELDS			= ALERT_ITEM_COLUMNS[7];
	public static final String ALERT_ITEM_MARKUP_FIELDS			= ALERT_ITEM_COLUMNS[8];
	public static final String ALERT_ITEM_NOTIFICATION_ITEM_ID 	= ALERT_ITEM_COLUMNS[9];
	public static final String ALERT_ITEM_ACCOUNT_ID 			= ALERT_ITEM_COLUMNS[10];
	public static final String ALERT_ITEM_STATUS 				= ALERT_ITEM_COLUMNS[11];
	public static final String ALERT_ITEM_DATE_EXPIRES			= ALERT_ITEM_COLUMNS[12];
	public static final String ALERT_ITEM_TIMESTAMP 			= ALERT_ITEM_COLUMNS[13];

	private static final HashMap<String, String> mAlertItemProjectionMap;

	static {
		mAlertItemProjectionMap = new HashMap<String, String>(ALERT_ITEM_COLUMNS.length);
		for (String column : ALERT_ITEM_COLUMNS) {
			mAlertItemProjectionMap.put(column, column);
		}
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ NOTIFICATION_ITEM

	private static final String[] NOTIFICATION_ITEM_COLUMNS = {
			"_id",
			"active",
			"template_name",
			"sound_path",
			"sound_type",
			"volume",
			"even_when_silent",
			"vibrate",
			"no_alert_screen",
			"stop_sound_after",
			"show_in_notification_bar",
			"show_notification_led",
			"ignore_global_quiet_policy",
			"timestamp"
	};

	public static final String NOTIFICATION_ITEM_ID 						= NOTIFICATION_ITEM_COLUMNS[0];
	public static final String NOTIFICATION_ITEM_ACTIVE 					= NOTIFICATION_ITEM_COLUMNS[1];
	public static final String NOTIFICATION_ITEM_TEMPLATE_NAME 				= NOTIFICATION_ITEM_COLUMNS[2];
	public static final String NOTIFICATION_ITEM_SOUND_PATH 				= NOTIFICATION_ITEM_COLUMNS[3];
	public static final String NOTIFICATION_ITEM_SOUND_TYPE 				= NOTIFICATION_ITEM_COLUMNS[4];
	public static final String NOTIFICATION_ITEM_VOLUME 					= NOTIFICATION_ITEM_COLUMNS[5];
	public static final String NOTIFICATION_ITEM_SILENT_MODE 				= NOTIFICATION_ITEM_COLUMNS[6];//////
	public static final String NOTIFICATION_ITEM_VIBRATE_MODE 				= NOTIFICATION_ITEM_COLUMNS[7];//////
	public static final String NOTIFICATION_ITEM_NO_ALERT_SCREEN			= NOTIFICATION_ITEM_COLUMNS[8];
	public static final String NOTIFICATION_ITEM_PLAY_FOR					= NOTIFICATION_ITEM_COLUMNS[9];//////
	public static final String NOTIFICATION_ITEM_SHOW_NOTIFICATION 			= NOTIFICATION_ITEM_COLUMNS[10];
	public static final String NOTIFICATION_ITEM_LED_MODE					= NOTIFICATION_ITEM_COLUMNS[11];//////
	public static final String NOTIFICATION_ITEM_IGNORE_GLOBAL_QUIET_POLICY = NOTIFICATION_ITEM_COLUMNS[12];
	public static final String NOTIFICATION_ITEM_TIMESTAMP 					= NOTIFICATION_ITEM_COLUMNS[13];

	private static final HashMap<String, String> mNotificationItemProjectionMap;

	static {
		mNotificationItemProjectionMap = new HashMap<String, String>(NOTIFICATION_ITEM_COLUMNS.length);
		for (String column : NOTIFICATION_ITEM_COLUMNS) {
			mNotificationItemProjectionMap.put(column, column);
		}
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ FILTER_ITEM

	private static final String[] FILTER_ITEM_COLUMNS = {
			"_id",
			"field_names",
			"phrase",
			"notification_item_id",
			"timestamp"
	};

	public static final String FILTER_ITEM_ID 					= FILTER_ITEM_COLUMNS[0];
	public static final String FILTER_ITEM_FIELD_NAMES 			= FILTER_ITEM_COLUMNS[1];
	public static final String FILTER_ITEM_PHRASE 				= FILTER_ITEM_COLUMNS[2];
	public static final String FILTER_ITEM_NOTIFICATION_ITEM_ID = FILTER_ITEM_COLUMNS[3];
	public static final String FILTER_ITEM_TIMESTAMP 			= FILTER_ITEM_COLUMNS[4];

	private static final HashMap<String, String> mFilterItemProjectionMap;

	static {
		mFilterItemProjectionMap = new HashMap<String, String>(FILTER_ITEM_COLUMNS.length);
		for (String column : FILTER_ITEM_COLUMNS) {
			mFilterItemProjectionMap.put(column, column);
		}
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ FILTER_ACCOUNT

	private static final String[] FILTER_ITEM_ACCOUNT_COLUMNS = {
			"_id",
			"filter_item_id",
			"account_id",
			"timestamp"
	};

	public static final String FILTER_ITEM_ACCOUNT_ID 			 = FILTER_ITEM_ACCOUNT_COLUMNS[0];
	public static final String FILTER_ITEM_ACCOUNT_FILTER_ITEM_ID= FILTER_ITEM_ACCOUNT_COLUMNS[1];
	public static final String FILTER_ITEM_ACCOUNT_ACCOUNT_ID    = FILTER_ITEM_ACCOUNT_COLUMNS[2];
	public static final String FILTER_ITEM_ACCOUNT_TIMESTAMP 	 = FILTER_ITEM_ACCOUNT_COLUMNS[3];

	private static final HashMap<String, String> mFilterItemAccountProjectionMap;

	static {
		mFilterItemAccountProjectionMap = new HashMap<String, String>(FILTER_ITEM_ACCOUNT_COLUMNS.length);
		for (String column : FILTER_ITEM_ACCOUNT_COLUMNS) {
			mFilterItemAccountProjectionMap.put(column, column);
		}
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ GENERAL PREFS

	private static final String[] GENERAL_PREFS_COLUMNS = {
			"_id",
			"keep_lists",
			"keep_deleted",
			"expire_deleted_after",
			"default_snooze",
			"auto_ack",
			"auto_ack_after",
			"auto_ack_as",
			"ringtone_stop_loop_after",
			"soundfile_stop_loop_after",
			"quiet_time_start",
			"quiet_time_end",
			"quiet_time_pauses",
			"global_pause",
			"always_show_notification",
			"notification_action",
			"prefetch_emails_count",
			"mark_viewed_alert_as",
			"show_poll_in_notification_bar",
			"show_poll_in_notification_bar_vibrate",
			"start_at_boot",
			"system_on",
			"pause_alerts_alarms",
			"max_list_size",
			"override_vol",
			"imap_max_retries",
			"foreground",
			"debug_mode",
			"noop_interval",            // NOW IS gc_poll
			"last_db_version_checked",
			"last_polled",
			"timestamp"
	};

	public static final String GENERAL_PREFS_ID 						= GENERAL_PREFS_COLUMNS[0];
	public static final String GENERAL_PREFS_KEEP_LISTS 				= GENERAL_PREFS_COLUMNS[1];
	public static final String GENERAL_PREFS_KEEP_DELETED				= GENERAL_PREFS_COLUMNS[2];
	public static final String GENERAL_PREFS_EXPIRE_DELETED_AFTER		= GENERAL_PREFS_COLUMNS[3];
	public static final String GENERAL_PREFS_DEFAULT_SNOOZE				= GENERAL_PREFS_COLUMNS[4];
	public static final String GENERAL_PREFS_AUTO_ACK					= GENERAL_PREFS_COLUMNS[5];
	public static final String GENERAL_PREFS_AUTO_ACK_AFTER				= GENERAL_PREFS_COLUMNS[6];
	public static final String GENERAL_PREFS_AUTO_ACK_AS				= GENERAL_PREFS_COLUMNS[7];
	public static final String GENERAL_PREFS_RINGTONE_STOP_LOOP_AFTER	= GENERAL_PREFS_COLUMNS[8];
	public static final String GENERAL_PREFS_SOUNDFILE_STOP_LOOP_AFTER	= GENERAL_PREFS_COLUMNS[9];
	public static final String GENERAL_PREFS_QUIET_TIME_START			= GENERAL_PREFS_COLUMNS[10];
	public static final String GENERAL_PREFS_QUIET_TIME_END				= GENERAL_PREFS_COLUMNS[11];
	public static final String GENERAL_PREFS_QUIET_TIME_PAUSES			= GENERAL_PREFS_COLUMNS[12];
	public static final String GENERAL_PREFS_GLOBAL_PAUSE				= GENERAL_PREFS_COLUMNS[13];
	public static final String GENERAL_PREFS_ALWAYS_SHOW_NOTIFICATION	= GENERAL_PREFS_COLUMNS[14];
	public static final String GENERAL_PREFS_NOTIFICATION_ACTION		= GENERAL_PREFS_COLUMNS[15];
	public static final String GENERAL_PREFS_PREFETCH_EMAILS_COUNT		= GENERAL_PREFS_COLUMNS[16];
	public static final String GENERAL_PREFS_MARK_VIEWED_ALERT_AS		= GENERAL_PREFS_COLUMNS[17];
	public static final String GENERAL_PREFS_SHOW_POLL_IN_NOTIFICATION_BAR
			= GENERAL_PREFS_COLUMNS[18];
	public static final String GENERAL_PREFS_SHOW_POLL_IN_NOTIFICATION_BAR_VIBRATE
			= GENERAL_PREFS_COLUMNS[19];
	public static final String GENERAL_PREFS_START_AT_BOOT				= GENERAL_PREFS_COLUMNS[20];
	public static final String GENERAL_PREFS_SYSTEM_ON					= GENERAL_PREFS_COLUMNS[21];
	public static final String GENERAL_PREFS_PAUSE_ALERTS_ALARMS		= GENERAL_PREFS_COLUMNS[22];
	public static final String GENERAL_PREFS_MAX_LIST_SIZE				= GENERAL_PREFS_COLUMNS[23];
	public static final String GENERAL_PREFS_OVERRIDE_VOL				= GENERAL_PREFS_COLUMNS[24];
	public static final String GENERAL_PREFS_IMAP_MAX_RETRIES			= GENERAL_PREFS_COLUMNS[25];
	public static final String GENERAL_PREFS_FOREGROUND					= GENERAL_PREFS_COLUMNS[26];
	public static final String GENERAL_PREFS_DEBUG_MODE					= GENERAL_PREFS_COLUMNS[27];
	public static final String GENERAL_PREFS_GC_POLL_INTERVAL           = GENERAL_PREFS_COLUMNS[28];
	public static final String GENERAL_PREFS_LAST_DB_VERSION_CHECKED	= GENERAL_PREFS_COLUMNS[29];
	public static final String GENERAL_PREFS_LAST_POLLED				= GENERAL_PREFS_COLUMNS[30];
	public static final String GENERAL_PREFS_TIMESTAMP 					= GENERAL_PREFS_COLUMNS[31];

	private static final HashMap<String, String> mGeneralPrefsProjectionMap;

	static {
		mGeneralPrefsProjectionMap = new HashMap<String, String>(GENERAL_PREFS_COLUMNS.length);
		for (String column : GENERAL_PREFS_COLUMNS) {
			mGeneralPrefsProjectionMap.put(column, column);
		}
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ SORUCE_TYPE

	private static final String[] SOURCE_TYPE_COLUMNS = {
			"_id",
			"lookup_key",
			"notification_item_id",
			"source_type",
			"timestamp"
	};

	public static final String SOURCE_TYPE_ID 					= SOURCE_TYPE_COLUMNS[0];
	public static final String SOURCE_TYPE_LOOKUP_KEY 			= SOURCE_TYPE_COLUMNS[1];
	public static final String SOURCE_TYPE_NOTIFICATION_ITEM_ID = SOURCE_TYPE_COLUMNS[2];
	public static final String SOURCE_TYPE_SOURCE_TYPE 			= SOURCE_TYPE_COLUMNS[3];
	public static final String SOURCE_TYPE_TIMESTAMP 			= SOURCE_TYPE_COLUMNS[4];

	private static final HashMap<String, String> mSourceTypeProjectionMap;

	static {
		mSourceTypeProjectionMap = new HashMap<String, String>(SOURCE_TYPE_COLUMNS.length);
		for (String column : SOURCE_TYPE_COLUMNS) {
			mSourceTypeProjectionMap.put(column, column);
		}
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ GROUP_NOTIFICATION

	private static final String[] GROUP_NOTIFICATION_COLUMNS = {
			"_id",
			"android_group_id",
			"notification_item_id",
			"timestamp"
	};

	public static final String GROUP_NOTIFICATION_ID 					= GROUP_NOTIFICATION_COLUMNS[0];
	public static final String GROUP_NOTIFICATION_ANDROID_GROUP_ID 		= GROUP_NOTIFICATION_COLUMNS[1];
	public static final String GROUP_NOTIFICATION_NOTIFICATION_ITEM_ID 	= GROUP_NOTIFICATION_COLUMNS[2];
	public static final String GROUP_NOTIFICATION_TIMESTAMP 			= GROUP_NOTIFICATION_COLUMNS[3];

	private static final HashMap<String, String> mGroupNotificationProjectionMap;

	static {
		mGroupNotificationProjectionMap = new HashMap<String, String>(GROUP_NOTIFICATION_COLUMNS.length);
		for (String column : GROUP_NOTIFICATION_COLUMNS) {
			mGroupNotificationProjectionMap.put(column, column);
		}
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ CONTACT_INFO

	private static final String[] CONTACT_INFO_COLUMNS = {
			"_id",
			"lookup_key",
			"favorite",
			"timestamp"
	};

	public static final String CONTACT_INFO_ID 						= CONTACT_INFO_COLUMNS[0];
	public static final String CONTACT_INFO_LOOKUP_KEY 				= CONTACT_INFO_COLUMNS[1];
	public static final String CONTACT_INFO_FAVORITE 				= CONTACT_INFO_COLUMNS[2];
	public static final String CONTACT_INFO_TIMESTAMP 				= CONTACT_INFO_COLUMNS[3];

	private static final HashMap<String, String> mContactInfoProjectionMap;

	static {
		mContactInfoProjectionMap = new HashMap<String, String>(CONTACT_INFO_COLUMNS.length);
		for (String column : CONTACT_INFO_COLUMNS) {
			mContactInfoProjectionMap.put(column, column);
		}
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ SORUCE_ACCOUNT

	private static final String[] SOURCE_ACCOUNT_COLUMNS = {
			"_id",
			"account_id",
			"source_type_id",
			"timestamp"
	};

	public static final String SOURCE_ACCOUNT_ID 			= SOURCE_ACCOUNT_COLUMNS[0];
	public static final String SOURCE_ACCOUNT_ACCOUNT_ID 	= SOURCE_ACCOUNT_COLUMNS[1];
	public static final String SOURCE_ACCOUNT_SOURCE_TYPE_ID= SOURCE_ACCOUNT_COLUMNS[2];
	public static final String SOURCE_ACCOUNT_TIMESTAMP 	= SOURCE_ACCOUNT_COLUMNS[3];

	private static final HashMap<String, String> mSourceAccountProjectionMap;

	static {
		mSourceAccountProjectionMap = new HashMap<String, String>(SOURCE_ACCOUNT_COLUMNS.length);
		for (String column : SOURCE_ACCOUNT_COLUMNS) {
			mSourceAccountProjectionMap.put(column, column);
		}
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ NAME VALUE DATA

	private static final String[] NAME_VALUE_DATA_COLUMNS = {
			"_id",
			"name",
			"pref",
			"timestamp"
	};

	public static final String NAME_VALUE_DATA_ID 			= NAME_VALUE_DATA_COLUMNS[0];
	public static final String NAME_VALUE_DATA_NAME 		= NAME_VALUE_DATA_COLUMNS[1];
	public static final String NAME_VALUE_DATA_VALUE		= NAME_VALUE_DATA_COLUMNS[2];
	public static final String NAME_VALUE_DATA_TIMESTAMP 	= NAME_VALUE_DATA_COLUMNS[3];

	private static final HashMap<String, String> mNameValueDataProjectionMap;

	static {
		mNameValueDataProjectionMap = new HashMap<String, String>(NAME_VALUE_DATA_COLUMNS.length);
		for (String column : NAME_VALUE_DATA_COLUMNS) {
			mNameValueDataProjectionMap.put(column, column);
		}
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ CONTACT_RT PREFS

	private static final String[] CONTACT_RT_PREFS_COLUMNS = {
			"_id",
			"default_ringtone",
			"default_volume",
			"default_new_silent_mode",
			"default_new_play_for",
			"default_new_vibrate_mode",
			"default_new_notification",
			"default_new_light",
			"default_new_volume",
			"default_text_ringtone",
			"default_text_silent_mode",
			"default_text_play_for",
			"default_text_vibrate_mode",
			"default_text_notification",
			"default_text_light",
			"default_text_volume",
			"default_linked_account_specific_pref_prefix",
			"auto_add_new_accounts_to_default",
			"auto_add_new_accounts_to_active",
			"show_toast_on_sms_mms_block",//TODO: used for reminders_on
			"timestamp"
	};

	public static final String CONTACT_RT_PREFS_ID 						= CONTACT_RT_PREFS_COLUMNS[0];
	public static final String CONTACT_RT_PREFS_DEFAULT_RINGTONE	 	= CONTACT_RT_PREFS_COLUMNS[1];
	public static final String CONTACT_RT_PREFS_DEFAULT_VOLUME			= CONTACT_RT_PREFS_COLUMNS[2];
	public static final String CONTACT_RT_PREFS_DEFAULT_NEW_SILENT_MODE = CONTACT_RT_PREFS_COLUMNS[3];
	public static final String CONTACT_RT_PREFS_DEFAULT_NEW_PLAY_FOR	= CONTACT_RT_PREFS_COLUMNS[4];
	public static final String CONTACT_RT_PREFS_DEFAULT_NEW_VIBRATE_MODE= CONTACT_RT_PREFS_COLUMNS[5];
	public static final String CONTACT_RT_PREFS_DEFAULT_NEW_NOTIFICATION= CONTACT_RT_PREFS_COLUMNS[6];
	public static final String CONTACT_RT_PREFS_DEFAULT_NEW_LIGHT		= CONTACT_RT_PREFS_COLUMNS[7];
	public static final String CONTACT_RT_PREFS_DEFAULT_NEW_VOLUME		= CONTACT_RT_PREFS_COLUMNS[8];
	public static final String CONTACT_RT_PREFS_DEFAULT_TEXT_RINGTONE	= CONTACT_RT_PREFS_COLUMNS[9];
	public static final String CONTACT_RT_PREFS_DEFAULT_TEXT_SILENT_MODE= CONTACT_RT_PREFS_COLUMNS[10];
	public static final String CONTACT_RT_PREFS_DEFAULT_TEXT_PLAY_FOR	= CONTACT_RT_PREFS_COLUMNS[11];
	public static final String CONTACT_RT_PREFS_DEFAULT_TEXT_VIBRATE_MODE= CONTACT_RT_PREFS_COLUMNS[12];
	public static final String CONTACT_RT_PREFS_DEFAULT_TEXT_NOTIFICATION= CONTACT_RT_PREFS_COLUMNS[13];
	public static final String CONTACT_RT_PREFS_DEFAULT_TEXT_LIGHT		= CONTACT_RT_PREFS_COLUMNS[14];
	public static final String CONTACT_RT_PREFS_DEFAULT_TEXT_VOLUME		= CONTACT_RT_PREFS_COLUMNS[15];
	public static final String CONTACT_RT_PREFS_DEFAULT_LINKED_ACCOUNT_NAME_VALUE_DATA_PREFIX
			= CONTACT_RT_PREFS_COLUMNS[16];
	public static final String CONTACT_RT_PREFS_AUTO_ADD_NEW_ACCOUNTS_TO_DEFAULT
			= CONTACT_RT_PREFS_COLUMNS[17];
	public static final String CONTACT_RT_PREFS_AUTO_ADD_NEW_ACCOUNTS_TO_ACTIVE
			= CONTACT_RT_PREFS_COLUMNS[18];
	public static final String CONTACT_RT_PREFS_REMINDERS_ON            = CONTACT_RT_PREFS_COLUMNS[19];
	public static final String CONTACT_RT_PREFS_TIMESTAMP 				= CONTACT_RT_PREFS_COLUMNS[20];

	private static final HashMap<String, String> mContactRTPrefsProjectionMap;

	static {
		mContactRTPrefsProjectionMap = new HashMap<String, String>(CONTACT_RT_PREFS_COLUMNS.length);
		for (String column : CONTACT_RT_PREFS_COLUMNS) {
			mContactRTPrefsProjectionMap.put(column, column);
		}
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ POST ALARMS (Snoozes)

	private static final String[] POST_ALARM_COLUMNS = {
			"_id",
			"type",
			"alert_item_id",
			"notifiction_item_id",
			"next_alarm",
			"orig_alarm",
			"timestamp"
	};

	public static final String POST_ALARM_ID 					= POST_ALARM_COLUMNS[0];
	public static final String POST_ALARM_TYPE					= POST_ALARM_COLUMNS[1];
	public static final String POST_ALARM_ALERT_ITEM_ID 		= POST_ALARM_COLUMNS[2];
	public static final String POST_ALARM_NOTIFICATION_ITEM_ID	= POST_ALARM_COLUMNS[3];
	public static final String POST_ALARM_NEXT_ALARM 			= POST_ALARM_COLUMNS[4];
	public static final String POST_ALARM_ORIG_ALARM 			= POST_ALARM_COLUMNS[5];
	public static final String POST_ALARM_TIMESTAMP 			= POST_ALARM_COLUMNS[6];

	private static final HashMap<String, String> mPostAlarmProjectionMap;

	static {
		mPostAlarmProjectionMap = new HashMap<String, String>(POST_ALARM_COLUMNS.length);
		for (String column : POST_ALARM_COLUMNS) {
			mPostAlarmProjectionMap.put(column, column);
		}
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ CONTACT_LIST_PREFS_

	private static final String[] CONTACT_LIST_PREFS_COLUMNS = {
			"_id",
			"show_text",
			"show_phone",
			"show_email",
			"timestamp"
	};

	public static final String CONTACT_LIST_PREFS_ID 			= CONTACT_LIST_PREFS_COLUMNS[0];
	public static final String CONTACT_LIST_PREFS_SHOW_TEXT	    = CONTACT_LIST_PREFS_COLUMNS[1];
	public static final String CONTACT_LIST_PREFS_SHOW_PHONE 	= CONTACT_LIST_PREFS_COLUMNS[2];
	public static final String CONTACT_LIST_PREFS_SHOW_EMAIL 	= CONTACT_LIST_PREFS_COLUMNS[3];
	public static final String CONTACT_LIST_PREFS_TIMESTAMP 			= CONTACT_LIST_PREFS_COLUMNS[4];

	private static final HashMap<String, String> mContactListPrefsProjectionMap;

	static {
		mContactListPrefsProjectionMap = new HashMap<String, String>(CONTACT_LIST_PREFS_COLUMNS.length);
		for (String column : CONTACT_LIST_PREFS_COLUMNS) {
			mContactListPrefsProjectionMap.put(column, column);
		}
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ CREATE TABLE COMMANDS

	private static final String CREATE_ACCOUNT_TABLE =
			"CREATE TABLE IF NOT EXISTS " + ACCOUNT_TABLE + " ("
					+ ACCOUNT_ID + " INTEGER  NOT NULL PRIMARY KEY AUTOINCREMENT,"
					+ ACCOUNT_NAME + " Text NOT NULL,"
					+ ACCOUNT_EMAIL_ADDRESS + " Text NOT NULL,"
					+ ACCOUNT_PASSWORD + " Text  NOT NULL,"
					+ ACCOUNT_SERVER_NAME + " Text  NOT NULL,"
					+ ACCOUNT_SERVER_SECURITY + " Text  NOT NULL,"
					+ ACCOUNT_SERVER_PORT + " Integer  NOT NULL,"
					+ ACCOUNT_LATEST_UID + " Integer  NOT NULL,"
					+ ACCOUNT_SHOW_IMAGES + " Text  NOT NULL,"
					+ ACCOUNT_POLL + " Integer  NOT NULL,"
					+ ACCOUNT_TYPE + " Integer  NOT NULL,"
					+ ACCOUNT_SAVE_TO_LIST + " Text  NOT NULL,"
					+ ACCOUNT_MARK_AS_SEEN + " Text  NOT NULL,"
					+ ACCOUNT_LAST_CHECKED + " Integer  NOT NULL,"
					+ ACCOUNT_TIMESTAMP + " Integer DEFAULT 'CURRENT_TIMESTAMP' NOT NULL"
					+ " )";

	private static final String CREATE_ALERT_ITEM_TABLE =
			"CREATE TABLE IF NOT EXISTS " + ALERT_ITEM_TABLE + " ("
					+ ALERT_ITEM_ID + " Integer  NOT NULL PRIMARY KEY AUTOINCREMENT,"
					+ ALERT_ITEM_UID + " Text  NOT NULL,"
					+ ALERT_ITEM_TYPE + " Text  NOT NULL,"
					+ ALERT_ITEM_FAVORITE + " Text  NOT NULL,"
					+ ALERT_ITEM_DATE_REMIND + " Integer NOT NULL,"
					+ ALERT_ITEM_REPEAT_EVERY + " Integer  NOT NULL,"
					+ ALERT_ITEM_STOP_AFTER + " Integer  NOT NULL,"
					+ ALERT_ITEM_RAW_FIELDS	+ " BLOB NULL,"
					+ ALERT_ITEM_MARKUP_FIELDS	+ " BLOB NULL,"
					+ ALERT_ITEM_NOTIFICATION_ITEM_ID + " Integer  NOT NULL,"
					+ ALERT_ITEM_ACCOUNT_ID + " Integer  NOT NULL,"
					+ ALERT_ITEM_STATUS + " Text  NOT NULL,"
					+ ALERT_ITEM_DATE_EXPIRES + " Integer  NOT NULL,"
					+ ALERT_ITEM_TIMESTAMP + " Integer DEFAULT 'CURRENT_TIMESTAMP' NOT NULL"
					+ " )";

	private static final String CREATE_NOTIFICATION_ITEM_TABLE =
			"CREATE TABLE IF NOT EXISTS " + NOTIFICATION_ITEM_TABLE + " ("
					+ NOTIFICATION_ITEM_ID + " Integer  NOT NULL PRIMARY KEY AUTOINCREMENT,"
					+ NOTIFICATION_ITEM_ACTIVE + " Text  NOT NULL,"
					+ NOTIFICATION_ITEM_TEMPLATE_NAME + " Text  NOT NULL,"
					+ NOTIFICATION_ITEM_SOUND_PATH + " Text  NOT NULL,"
					+ NOTIFICATION_ITEM_SOUND_TYPE + " Text  NOT NULL,"
					+ NOTIFICATION_ITEM_VOLUME + " Integer  NOT NULL,"
					+ NOTIFICATION_ITEM_SILENT_MODE + " Text  NOT NULL,"
					+ NOTIFICATION_ITEM_VIBRATE_MODE + " Text  NOT NULL,"
					+ NOTIFICATION_ITEM_NO_ALERT_SCREEN + " Text  NOT NULL,"
					+ NOTIFICATION_ITEM_PLAY_FOR + " Integer  NOT NULL,"
					+ NOTIFICATION_ITEM_SHOW_NOTIFICATION + " Text  NOT NULL,"
					+ NOTIFICATION_ITEM_LED_MODE + " Text  NOT NULL,"
					+ NOTIFICATION_ITEM_IGNORE_GLOBAL_QUIET_POLICY + " Text  NOT NULL,"
					+ NOTIFICATION_ITEM_TIMESTAMP + " [timestamp] Integer DEFAULT 'CURRENT_TIMESTAMP' NOT NULL"
					+ " )";

	private static final String CREATE_FILTER_ITEM_TABLE =
			"CREATE TABLE IF NOT EXISTS " + FILTER_ITEM_TABLE + " ("
					+ FILTER_ITEM_ID + " Integer  PRIMARY KEY AUTOINCREMENT NOT NULL,"
					+ FILTER_ITEM_FIELD_NAMES + " Text NOT NULL,"
					+ FILTER_ITEM_PHRASE + " Text NOT NULL,"
					+ FILTER_ITEM_NOTIFICATION_ITEM_ID + " Integer  NOT NULL,"
					+ FILTER_ITEM_TIMESTAMP + " Integer DEFAULT 'CURRENT_TIMESTAMP' NOT NULL"
					+ " )";

	private static final String CREATE_FILTER_ITEM_ACCOUNT_TABLE =
			"CREATE TABLE IF NOT EXISTS " + FILTER_ITEM_ACCOUNT_TABLE + " ("
					+ FILTER_ITEM_ACCOUNT_ID + " Integer  PRIMARY KEY AUTOINCREMENT NOT NULL,"
					+ FILTER_ITEM_ACCOUNT_FILTER_ITEM_ID + " Integer NOT NULL,"
					+ FILTER_ITEM_ACCOUNT_ACCOUNT_ID + " Integer NOT NULL,"
					+ FILTER_ITEM_ACCOUNT_TIMESTAMP + " Integer DEFAULT 'CURRENT_TIMESTAMP' NOT NULL"
					+ " )";

	private static final String CREATE_GENERAL_PREFS_TABLE =
			"CREATE TABLE IF NOT EXISTS " + GENERAL_PREFS_TABLE + " ("
					+ GENERAL_PREFS_ID + " Integer  PRIMARY KEY AUTOINCREMENT NOT NULL,"
					+ GENERAL_PREFS_KEEP_LISTS  + " Text NOT NULL,"
					+ GENERAL_PREFS_KEEP_DELETED + " Text NOT NULL,"
					+ GENERAL_PREFS_EXPIRE_DELETED_AFTER + " Integer  NOT NULL,"
					+ GENERAL_PREFS_DEFAULT_SNOOZE + " Integer  NOT NULL,"
					+ GENERAL_PREFS_AUTO_ACK + " Text NOT NULL,"
					+ GENERAL_PREFS_AUTO_ACK_AFTER + " Integer  NOT NULL,"
					+ GENERAL_PREFS_AUTO_ACK_AS + " Text NOT NULL,"
					+ GENERAL_PREFS_RINGTONE_STOP_LOOP_AFTER + " Integer  NOT NULL,"
					+ GENERAL_PREFS_SOUNDFILE_STOP_LOOP_AFTER + " Integer  NOT NULL,"
					+ GENERAL_PREFS_QUIET_TIME_START + " Integer  NOT NULL,"
					+ GENERAL_PREFS_QUIET_TIME_END + " Integer  NOT NULL,"
					+ GENERAL_PREFS_QUIET_TIME_PAUSES + " Text  NOT NULL,"
					+ GENERAL_PREFS_GLOBAL_PAUSE + " Text  NOT NULL,"
					+ GENERAL_PREFS_ALWAYS_SHOW_NOTIFICATION + " Text  NOT NULL,"
					+ GENERAL_PREFS_NOTIFICATION_ACTION + " Text  NOT NULL,"
					+ GENERAL_PREFS_PREFETCH_EMAILS_COUNT + " Integer  NOT NULL,"
					+ GENERAL_PREFS_MARK_VIEWED_ALERT_AS + " Text NOT NULL,"
					+ GENERAL_PREFS_SHOW_POLL_IN_NOTIFICATION_BAR + " Text NOT NULL,"
					+ GENERAL_PREFS_SHOW_POLL_IN_NOTIFICATION_BAR_VIBRATE + " Text NOT NULL,"
					+ GENERAL_PREFS_START_AT_BOOT + " Text  NOT NULL,"
					+ GENERAL_PREFS_SYSTEM_ON + " Text  NOT NULL,"
					+ GENERAL_PREFS_PAUSE_ALERTS_ALARMS + " Text  NOT NULL,"
					+ GENERAL_PREFS_MAX_LIST_SIZE + " Integer  NOT NULL,"
					+ GENERAL_PREFS_OVERRIDE_VOL + " Text  NOT NULL,"
					+ GENERAL_PREFS_IMAP_MAX_RETRIES + " Integer  NOT NULL,"
					+ GENERAL_PREFS_FOREGROUND + " Text  NOT NULL,"
					+ GENERAL_PREFS_DEBUG_MODE + " Text  NOT NULL,"
					+ GENERAL_PREFS_GC_POLL_INTERVAL + " Integer NOT NULL,"
					+ GENERAL_PREFS_LAST_DB_VERSION_CHECKED + " Text  NOT NULL,"
					+ GENERAL_PREFS_LAST_POLLED + " Integer  NOT NULL,"
					+ GENERAL_PREFS_TIMESTAMP + " Integer DEFAULT 'CURRENT_TIMESTAMP' NOT NULL"
					+ " )";

	private static final String CREATE_SOURCE_TYPE_TABLE =
			"CREATE TABLE IF NOT EXISTS " + SOURCE_TYPE_TABLE + " ("
					+ SOURCE_TYPE_ID + " Integer  PRIMARY KEY AUTOINCREMENT NOT NULL,"
					+ SOURCE_TYPE_LOOKUP_KEY + " Text NOT NULL,"
					+ SOURCE_TYPE_NOTIFICATION_ITEM_ID + " Integer  NOT NULL,"
					+ SOURCE_TYPE_SOURCE_TYPE + " Text  NOT NULL,"
					+ SOURCE_TYPE_TIMESTAMP + " Integer DEFAULT 'CURRENT_TIMESTAMP' NOT NULL"
					+ " )";

	private static final String CREATE_GROUP_NOTIFICATION_TABLE =
			"CREATE TABLE IF NOT EXISTS " + GROUP_NOTIFICATION_TABLE + " ("
					+ GROUP_NOTIFICATION_ID + " Integer  PRIMARY KEY AUTOINCREMENT NOT NULL,"
					+ GROUP_NOTIFICATION_ANDROID_GROUP_ID + " Text NOT NULL,"
					+ GROUP_NOTIFICATION_NOTIFICATION_ITEM_ID + " Integer  NOT NULL,"
					+ GROUP_NOTIFICATION_TIMESTAMP + " Integer DEFAULT 'CURRENT_TIMESTAMP' NOT NULL"
					+ " )";

	private static final String CREATE_CONTACT_INFO_TABLE =
			"CREATE TABLE IF NOT EXISTS " + CONTACT_INFO_TABLE + " ("
					+ CONTACT_INFO_ID + " Integer  PRIMARY KEY AUTOINCREMENT NOT NULL,"
					+ CONTACT_INFO_LOOKUP_KEY + " Text UNIQUIE NOT NULL,"
					+ CONTACT_INFO_FAVORITE + " Text  NOT NULL,"
					+ CONTACT_INFO_TIMESTAMP + " Integer DEFAULT 'CURRENT_TIMESTAMP' NOT NULL"
					+ " )";

	private static final String CREATE_SOURCE_ACCOUNT_TABLE =
			"CREATE TABLE IF NOT EXISTS " + SOURCE_ACCOUNT_TABLE + " ("
					+ SOURCE_ACCOUNT_ID + " Integer  PRIMARY KEY AUTOINCREMENT NOT NULL,"
					+ SOURCE_ACCOUNT_ACCOUNT_ID + " Integer  NOT NULL,"
					+ SOURCE_ACCOUNT_SOURCE_TYPE_ID + " Integer  NOT NULL,"
					+ SOURCE_ACCOUNT_TIMESTAMP + " Integer DEFAULT 'CURRENT_TIMESTAMP' NOT NULL"
					+ " )";

	private static final String CREATE_NAME_VALUE_DATA_TABLE =
			"CREATE TABLE IF NOT EXISTS " + NAME_VALUE_DATA_TABLE + " ("
					+ NAME_VALUE_DATA_ID + " Integer  PRIMARY KEY AUTOINCREMENT NOT NULL,"
					+ NAME_VALUE_DATA_NAME + " Text  NOT NULL,"
					+ NAME_VALUE_DATA_VALUE + " Text  NOT NULL,"
					+ NAME_VALUE_DATA_TIMESTAMP + " Integer DEFAULT 'CURRENT_TIMESTAMP' NOT NULL"
					+ " )";

	private static final String CREATE_CONTACT_RT_PREFS_TABLE =
			"CREATE TABLE IF NOT EXISTS " + CONTACT_RT_PREFS_TABLE + " ("
					+ CONTACT_RT_PREFS_ID + " Integer  PRIMARY KEY AUTOINCREMENT NOT NULL,"
					+ CONTACT_RT_PREFS_DEFAULT_RINGTONE  + " Text NOT NULL,"
					+ CONTACT_RT_PREFS_DEFAULT_VOLUME + " Text  NOT NULL,"
					+ CONTACT_RT_PREFS_DEFAULT_NEW_SILENT_MODE + " Text NOT NULL,"
					+ CONTACT_RT_PREFS_DEFAULT_NEW_PLAY_FOR + " Integer NOT NULL,"
					+ CONTACT_RT_PREFS_DEFAULT_NEW_VIBRATE_MODE + " Text  NOT NULL,"
					+ CONTACT_RT_PREFS_DEFAULT_NEW_NOTIFICATION + " Text NOT NULL,"
					+ CONTACT_RT_PREFS_DEFAULT_NEW_LIGHT + " Text  NOT NULL,"
					+ CONTACT_RT_PREFS_DEFAULT_NEW_VOLUME + " Integer  NOT NULL,"
					+ CONTACT_RT_PREFS_DEFAULT_TEXT_RINGTONE  + " Text NOT NULL,"
					+ CONTACT_RT_PREFS_DEFAULT_TEXT_SILENT_MODE + " Text NOT NULL,"
					+ CONTACT_RT_PREFS_DEFAULT_TEXT_PLAY_FOR + " Integer NOT NULL,"
					+ CONTACT_RT_PREFS_DEFAULT_TEXT_VIBRATE_MODE + " Text  NOT NULL,"
					+ CONTACT_RT_PREFS_DEFAULT_TEXT_NOTIFICATION + " Text NOT NULL,"
					+ CONTACT_RT_PREFS_DEFAULT_TEXT_LIGHT + " Text  NOT NULL,"
					+ CONTACT_RT_PREFS_DEFAULT_TEXT_VOLUME + " Integer  NOT NULL,"
					+ CONTACT_RT_PREFS_DEFAULT_LINKED_ACCOUNT_NAME_VALUE_DATA_PREFIX + " Text NOT NULL,"
					+ CONTACT_RT_PREFS_AUTO_ADD_NEW_ACCOUNTS_TO_DEFAULT + " Text  NOT NULL,"
					+ CONTACT_RT_PREFS_AUTO_ADD_NEW_ACCOUNTS_TO_ACTIVE + " Text  NOT NULL,"
					+ CONTACT_RT_PREFS_REMINDERS_ON + " Text  NOT NULL,"
					+ CONTACT_RT_PREFS_TIMESTAMP + " Integer DEFAULT 'CURRENT_TIMESTAMP' NOT NULL"
					+ " )";

	private static final String CREATE_POST_ALARM_TABLE =
			"CREATE TABLE IF NOT EXISTS " + POST_ALARM_TABLE + " ("
					+ POST_ALARM_ID + " Integer  PRIMARY KEY AUTOINCREMENT NOT NULL,"
					+ POST_ALARM_TYPE + " Text NOT NULL,"
					+ POST_ALARM_ALERT_ITEM_ID + " Integer NOT NULL,"
					+ POST_ALARM_NOTIFICATION_ITEM_ID + " Integer NOT NULL,"
					+ POST_ALARM_NEXT_ALARM + " Integer NOT NULL,"
					+ POST_ALARM_ORIG_ALARM + " Integer NOT NULL,"
					+ POST_ALARM_TIMESTAMP + " Integer DEFAULT 'CURRENT_TIMESTAMP' NOT NULL"
					+ " )";

//	private static final String CREATE_CONTACT_LIST_PREFS_TABLE =
//			"CREATE TABLE IF NOT EXISTS " + CONTACT_LIST_PREFS_TABLE + " ("
//					+ CONTACT_LIST_PREFS_ID + " Integer  PRIMARY KEY AUTOINCREMENT NOT NULL,"
//					+ CONTACT_LIST_PREFS_SHOW_TEXT + " Text NOT NULL,"
//					+ CONTACT_LIST_PREFS_SHOW_PHONE + " Text NOT NULL,"
//					+ CONTACT_LIST_PREFS_SHOW_EMAIL + " Text NOT NULL,"
//					+ CONTACT_LIST_PREFS_TIMESTAMP + " Integer DEFAULT 'CURRENT_TIMESTAMP' NOT NULL"
//					+ " )";

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ CREATE INDEX COMMANDS

	private static final String CREATE_ACCOUNT_NAMEEMAILADDRESS_INDEX =
			"CREATE UNIQUE INDEX [account_nameemailaddress] ON [" + ACCOUNT_TABLE + "]("
					+ " [" + ACCOUNT_NAME + "]  ASC,"
					+ " [" + ACCOUNT_EMAIL_ADDRESS + "]  ASC"
					+ " )";

	private static final String CREATE_ALERTITEM_ACCOUNTID_INDEX =
			"CREATE INDEX [alertitem_accountid] ON [" + ALERT_ITEM_TABLE + "]("
					+ " [" + ALERT_ITEM_ACCOUNT_ID + "]  ASC"
					+ " )";

	private static final String CREATE_ALERTITEM_TYPE_INDEX =
			"CREATE INDEX [alertitem_type] ON [" + ALERT_ITEM_TABLE + "]("
					+ " [" + ALERT_ITEM_TYPE + "]  ASC"
					+ " )";

	private static final String CREATE_ALERTITEM_UID_INDEX =
			"CREATE INDEX [alertitem_uid] ON [" + ALERT_ITEM_TABLE + "]("
					+ " [" + ALERT_ITEM_UID + "]  ASC"
					+ " )";

	private static final String CREATE_ALERTITEM_DATEREMIND_INDEX =
			"CREATE INDEX [alertitem_dateremind] ON [" + ALERT_ITEM_TABLE + "]("
					+ " [" + ALERT_ITEM_DATE_REMIND + "]  DESC"
					+ " )";

	private static final String CREATE_ALERTITEM_DATEEXPIRES_INDEX =
			"CREATE INDEX [alertitem_dateexpires] ON [" + ALERT_ITEM_TABLE + "]("
					+ " [" + ALERT_ITEM_DATE_EXPIRES + "]  ASC"
					+ " )";

	private static final String CREATE_ALERTITEM_REPEATEVERY_INDEX =
			"CREATE INDEX [alertitem_repeatevery] ON [" + ALERT_ITEM_TABLE + "]("
					+ " [" + ALERT_ITEM_REPEAT_EVERY + "]  ASC"
					+ " )";

	private static final String CREATE_ALERTITEM_NOTIFICATIONITEMID_INDEX =
			"CREATE INDEX [alertitem_notificationitemid] ON [" + ALERT_ITEM_TABLE + "]("
					+ " [" + ALERT_ITEM_NOTIFICATION_ITEM_ID + "]  ASC"
					+ " )";

	private static final String CREATE_ALERTITEM_STATUS_INDEX =
			"CREATE INDEX [alertitem_status] ON [" + ALERT_ITEM_TABLE + "]("
					+ " [" + ALERT_ITEM_STATUS + "]  ASC"
					+ " )";

	private static final String CREATE_FILTERITEM_NOTIFICATIONITEMID_INDEX =
			"CREATE INDEX [filter_item_notificationitemid] ON [" + FILTER_ITEM_TABLE + "]("
					+ " [" + FILTER_ITEM_NOTIFICATION_ITEM_ID + "]  ASC"
					+ " )";

//	private static final String CREATE_FILTERITEM_ACCOUNTID_INDEX =
//			"CREATE INDEX [filteritem_accountid] ON [" + FILTER_ITEM_TABLE + "]("
//			+ " [" + FILTER_ITEM_ACCOUNT_ID + "]  ASC"
//			+ " )";

	private static final String CREATE_FILTERITEMACCOUNT_FILTERITEMID_INDEX =
			"CREATE INDEX [filteritemaccount_filteritemid] ON [" + FILTER_ITEM_ACCOUNT_TABLE + "]("
					+ " [" + FILTER_ITEM_ACCOUNT_FILTER_ITEM_ID + "]  ASC"
					+ " )";

	private static final String CREATE_FILTERITEMACCOUNT_ACCOUNTID_INDEX =
			"CREATE INDEX [filteritemaccount_accountid] ON [" + FILTER_ITEM_ACCOUNT_TABLE + "]("
					+ " [" + FILTER_ITEM_ACCOUNT_ACCOUNT_ID + "]  ASC"
					+ " )";

	private static final String CREATE_SOURCETYPE_LOOKUPKEY_INDEX =
			"CREATE INDEX [sourcetype_lookupkey] ON [" + SOURCE_TYPE_TABLE + "]("
					+ " [" + SOURCE_TYPE_LOOKUP_KEY + "]  ASC"
					+ " )";

	private static final String CREATE_SOURCETYPE_NOTIFICATIONITEMID_INDEX =
			"CREATE INDEX [sourcetype_notificationitemid] ON [" + SOURCE_TYPE_TABLE + "]("
					+ " [" + SOURCE_TYPE_NOTIFICATION_ITEM_ID + "]  ASC"
					+ " )";

	private static final String CREATE_SOURCETYPE_SOURCETYPE_INDEX =
			"CREATE INDEX [sourcetype_sourcetype] ON [" + SOURCE_TYPE_TABLE + "]("
					+ " [" + SOURCE_TYPE_SOURCE_TYPE + "]  ASC"
					+ " )";

	private static final String CREATE_GROUPNOTIFICATION_ANDROIDGROUPID_INDEX =
			"CREATE INDEX [groupnotification_androidgroupid] ON [" + GROUP_NOTIFICATION_TABLE+ "]("
					+ " [" + GROUP_NOTIFICATION_ANDROID_GROUP_ID + "]  ASC"
					+ " )";

	private static final String CREATE_GROUPNOTIFICATION_NOTIFICATIONITEMID_INDEX =
			"CREATE INDEX [groupnotification_notificationitemi] ON [" + GROUP_NOTIFICATION_TABLE+ "]("
					+ " [" + GROUP_NOTIFICATION_NOTIFICATION_ITEM_ID + "]  ASC"
					+ " )";

	private static final String CREATE_CONTACTINFO_LOOKUPKEY_INDEX =
			"CREATE UNIQUE INDEX [contactinfo_lookupkey] ON [" + CONTACT_INFO_TABLE + "]("
					+ " [" + CONTACT_INFO_LOOKUP_KEY + "]  ASC"
					+ " )";

	private static final String CREATE_CONTACTINFO_FAVORITE_INDEX =
			"CREATE INDEX [contactinfo_favorite] ON [" + CONTACT_INFO_TABLE + "]("
					+ " [" + CONTACT_INFO_FAVORITE + "]  ASC"
					+ " )";

	private static final String CREATE_SOURCE_ACCOUNT_ACCOUNT_ID_INDEX =
			"CREATE INDEX [sourceaccount_accountid] ON [" + SOURCE_ACCOUNT_TABLE + "]("
					+ " [" + SOURCE_ACCOUNT_ACCOUNT_ID + "]  ASC"
					+ " )";

	private static final String CREATE_SOURCE_ACCOUNT_SOURCE_TYPE_ID_INDEX =
			"CREATE INDEX [sourceaccount_sourcetypeid] ON [" + SOURCE_ACCOUNT_TABLE + "]("
					+ " [" + SOURCE_ACCOUNT_SOURCE_TYPE_ID + "]  ASC"
					+ " )";

	private static final String CREATE_NAME_VALUE_DATA_NAME_INDEX =
			"CREATE INDEX [namevaluedata_name] ON [" + NAME_VALUE_DATA_TABLE + "]("
					+ " [" + NAME_VALUE_DATA_NAME + "]  ASC"
					+ " )";

	private static final String CREATE_POST_ALARM_ALERT_ITEM_ID_INDEX =
			"CREATE INDEX [postalarm_alertitemid] ON [" + POST_ALARM_TABLE + "]("
					+ " [" + POST_ALARM_ALERT_ITEM_ID + "]  ASC"
					+ " )";

	private static final String CREATE_POST_ALARM_NOTIFICATION_ITEM_ID_INDEX =
			"CREATE INDEX [postalarm_notificationitemid] ON [" + POST_ALARM_TABLE + "]("
					+ " [" + POST_ALARM_NOTIFICATION_ITEM_ID + "]  ASC"
					+ " )";

	private static final String CREATE_POST_ALARM_NEXT_ALARM_INDEX =
			"CREATE INDEX [postalarm_next_alarm] ON [" + POST_ALARM_TABLE + "]("
					+ " [" + POST_ALARM_NEXT_ALARM + "]  ASC"
					+ " )";

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ CONTENT PROVIDER

	public void resetDbOpenHelper() {
		if (mDBOpenHelper != null) {
			mDBOpenHelper.close();
			mDBOpenHelper = null;
		}
		mDBOpenHelper =
				new AutomatonAlertDBOpenHelper(
						getContext(), null, false/*testingValidity*/);
	}
	@Override
	public boolean onCreate() {
		resetDbOpenHelper();

		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
	                    String[] selectionArgs, String sortOrder) {
		return query(
				mDBOpenHelper, uri, projection, selection, selectionArgs, sortOrder);
	}

	public Cursor query(AutomatonAlertDBOpenHelper helper,
	                    Uri uri, String[] projection, String selection,
	                    String[] selectionArgs, String sortOrder) {

		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

		// for uri's that are ID-specific
		List<String> segments = uri.getPathSegments();
		int last = segments.size() - 1;
		if (segments.get(last).equals("-1")) {
			throw new IllegalArgumentException("_id cannot equal '-1'");
		}

		switch (mUriMatcher.match(uri)) {

			case ACCOUNT_TABLE_URI_ID:
				queryBuilder.setTables(ACCOUNT_TABLE);
				queryBuilder.setProjectionMap(mAccountProjectionMap);
				break;

			case ACCOUNT_ID_URI_ID:
				queryBuilder.setTables(ACCOUNT_TABLE);
				queryBuilder.setProjectionMap(mAccountProjectionMap);
				if (last >= 0) {
					queryBuilder.appendWhere(
							ACCOUNT_ID
									+ " = ?");
					selectionArgs = new String[] { segments.get(last) };
				}
				break;

			case ALERT_ITEM_TABLE_URI_ID:
				queryBuilder.setTables(ALERT_ITEM_TABLE);
				queryBuilder.setProjectionMap(mAlertItemProjectionMap);
				break;

			case ALERT_ITEM_ID_URI_ID:
				queryBuilder.setTables(ALERT_ITEM_TABLE);
				queryBuilder.setProjectionMap(mAlertItemProjectionMap);
				if (last >= 0) {
					queryBuilder.appendWhere(
							ALERT_ITEM_ID
									+ " = ?");
					selectionArgs = new String[] { segments.get(last) };
				}
				break;

			case NOTIFICATION_ITEM_TABLE_URI_ID:
				queryBuilder.setTables(NOTIFICATION_ITEM_TABLE);
				queryBuilder.setProjectionMap(mNotificationItemProjectionMap);
				break;

			case NOTIFICATION_ITEM_ID_URI_ID:
				queryBuilder.setTables(NOTIFICATION_ITEM_TABLE);
				queryBuilder.setProjectionMap(mNotificationItemProjectionMap);
				if (last >= 0) {
					queryBuilder.appendWhere(
							NOTIFICATION_ITEM_ID
									+ " = ?");
					selectionArgs = new String[] { segments.get(last) };
				}
				break;

			case FILTER_ITEM_TABLE_URI_ID:
				queryBuilder.setTables(FILTER_ITEM_TABLE);
				queryBuilder.setProjectionMap(mFilterItemProjectionMap);
				break;

			case FILTER_ITEM_ID_URI_ID:
				queryBuilder.setTables(FILTER_ITEM_TABLE);
				queryBuilder.setProjectionMap(mFilterItemProjectionMap);
				if (last >= 0) {
					queryBuilder.appendWhere(
							FILTER_ITEM_ID
									+ " = ?");
					selectionArgs = new String[] { segments.get(last) };
				}
				break;

			case FILTER_ITEM_ACCOUNT_TABLE_URI_ID:
				queryBuilder.setTables(FILTER_ITEM_ACCOUNT_TABLE);
				queryBuilder.setProjectionMap(mFilterItemAccountProjectionMap);
				break;

			case FILTER_ITEM_ACCOUNT_ID_URI_ID:
				queryBuilder.setTables(FILTER_ITEM_ACCOUNT_TABLE);
				queryBuilder.setProjectionMap(mFilterItemAccountProjectionMap);
				if (last >= 0) {
					queryBuilder.appendWhere(
							FILTER_ITEM_ACCOUNT_ID
									+ " = ?");
					selectionArgs = new String[] { segments.get(last) };
				}
				break;

			case GENERAL_PREFS_TABLE_URI_ID:
				queryBuilder.setTables(GENERAL_PREFS_TABLE);
				queryBuilder.setProjectionMap(mGeneralPrefsProjectionMap);
				break;

			case GENERAL_PREFS_ID_URI_ID:
				queryBuilder.setTables(GENERAL_PREFS_TABLE);
				queryBuilder.setProjectionMap(mGeneralPrefsProjectionMap);
				if (last >= 0) {
					queryBuilder.appendWhere(
							GENERAL_PREFS_ID
									+ " = ?");
					selectionArgs = new String[] { segments.get(last) };
				}
				break;

			case SOURCE_TYPE_TABLE_URI_ID:
				queryBuilder.setTables(SOURCE_TYPE_TABLE);
				queryBuilder.setProjectionMap(mSourceTypeProjectionMap);
				break;

			case SOURCE_TYPE_ID_URI_ID:
				queryBuilder.setTables(SOURCE_TYPE_TABLE);
				queryBuilder.setProjectionMap(mSourceTypeProjectionMap);
				if (last >= 0) {
					queryBuilder.appendWhere(
							SOURCE_TYPE_ID
									+ " = ?");
					selectionArgs = new String[] { segments.get(last) };
				}
				break;

			case GROUP_NOTIFICATION_TABLE_URI_ID:
				queryBuilder.setTables(GROUP_NOTIFICATION_TABLE);
				queryBuilder.setProjectionMap(mGroupNotificationProjectionMap);
				break;

			case GROUP_NOTIFICATION_ID_URI_ID:
				queryBuilder.setTables(GROUP_NOTIFICATION_TABLE);
				queryBuilder.setProjectionMap(mGroupNotificationProjectionMap);
				if (last >= 0) {
					queryBuilder.appendWhere(
							GROUP_NOTIFICATION_ID
									+ " = ?");
					selectionArgs = new String[] { segments.get(last) };
				}
				break;

			case CONTACT_INFO_TABLE_URI_ID:
				queryBuilder.setTables(CONTACT_INFO_TABLE);
				queryBuilder.setProjectionMap(mContactInfoProjectionMap);
				break;

			case CONTACT_INFO_ID_URI_ID:
				queryBuilder.setTables(CONTACT_INFO_TABLE);
				queryBuilder.setProjectionMap(mContactInfoProjectionMap);
				if (last >= 0) {
					queryBuilder.appendWhere(
							CONTACT_INFO_ID
									+ " = ?");
					selectionArgs = new String[] { segments.get(last) };
				}
				break;

			case SOURCE_ACCOUNT_TABLE_URI_ID:
				queryBuilder.setTables(SOURCE_ACCOUNT_TABLE);
				queryBuilder.setProjectionMap(mSourceAccountProjectionMap);
				break;

			case SOURCE_ACCOUNT_ID_URI_ID:
				queryBuilder.setTables(SOURCE_ACCOUNT_TABLE);
				queryBuilder.setProjectionMap(mSourceAccountProjectionMap);
				if (last >= 0) {
					queryBuilder.appendWhere(
							SOURCE_ACCOUNT_ID
									+ " = ?");
					selectionArgs = new String[] { segments.get(last) };
				}
				break;

			case NAME_VALUE_DATA_TABLE_URI_ID:
				queryBuilder.setTables(NAME_VALUE_DATA_TABLE);
				queryBuilder.setProjectionMap(mNameValueDataProjectionMap);
				break;

			case NAME_VALUE_DATA_ID_URI_ID:
				queryBuilder.setTables(NAME_VALUE_DATA_TABLE);
				queryBuilder.setProjectionMap(mNameValueDataProjectionMap);
				if (last >= 0) {
					queryBuilder.appendWhere(
							NAME_VALUE_DATA_ID
									+ " = ?");
					selectionArgs = new String[] { segments.get(last) };
				}
				break;

			case CONTACT_RT_PREFS_TABLE_URI_ID:
				queryBuilder.setTables(CONTACT_RT_PREFS_TABLE);
				queryBuilder.setProjectionMap(mContactRTPrefsProjectionMap);
				break;

			case CONTACT_RT_PREFS_ID_URI_ID:
				queryBuilder.setTables(CONTACT_RT_PREFS_TABLE);
				queryBuilder.setProjectionMap(mContactRTPrefsProjectionMap);
				if (last >= 0) {
					queryBuilder.appendWhere(
							CONTACT_RT_PREFS_ID
									+ " = ?");
					selectionArgs = new String[] { segments.get(last) };
				}
				break;

			case POST_ALARM_TABLE_URI_ID:
				queryBuilder.setTables(POST_ALARM_TABLE);
				queryBuilder.setProjectionMap(mPostAlarmProjectionMap);
				break;

			case POST_ALARM_ID_URI_ID:
				queryBuilder.setTables(POST_ALARM_TABLE);
				queryBuilder.setProjectionMap(mPostAlarmProjectionMap);
				if (last >= 0) {
					queryBuilder.appendWhere(
							POST_ALARM_ID
									+ " = ?");
					selectionArgs = new String[] { segments.get(last) };
				}
				break;

			case CONTACT_LIST_PREFS_TABLE_URI_ID:
				queryBuilder.setTables(CONTACT_LIST_PREFS_TABLE);
				queryBuilder.setProjectionMap(mContactListPrefsProjectionMap);
				break;

			case CONTACT_LIST_PREFS_ID_URI_ID:
				queryBuilder.setTables(CONTACT_LIST_PREFS_TABLE);
				queryBuilder.setProjectionMap(mContactListPrefsProjectionMap);
				if (last >= 0) {
					queryBuilder.appendWhere(
							CONTACT_LIST_PREFS_ID
									+ " = ?");
					selectionArgs = new String[] { segments.get(last) };
				}
				break;

			default:
				throw new IllegalArgumentException ("Unknown URI: " + uri);
		}

		String orderBy;
		// If no sort order is specified, uses the default
		if (TextUtils.isEmpty(sortOrder)) {
			orderBy = DEFAULT_SORT_ORDER;
		}
		else {
			// otherwise, uses the incoming sort order
			orderBy = sortOrder;
		}

		SQLiteDatabase database = (helper != null) ?
				helper.getReadableDatabase()
				: mDBOpenHelper.getReadableDatabase();

		Cursor c = queryBuilder.query(
				database,            	// The database to query
				projection,    			// The columns to return from the query
				selection,     			// The columns for the where clause
				selectionArgs, 			// The values for the where clause
				null,          			// don't group the rows
				null,          			// don't filter by row groups
				orderBy	        		// The sort order
		);

		// Tells the Cursor what URI to watch, so it knows when its source data changes
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;

	}

	@Override
	public String getType(Uri uri) {

		switch (mUriMatcher.match(uri)) {

			case ACCOUNT_TABLE_URI_ID:
			case ALERT_ITEM_TABLE_URI_ID:
			case NOTIFICATION_ITEM_TABLE_URI_ID:
			case FILTER_ITEM_TABLE_URI_ID:
			case FILTER_ITEM_ACCOUNT_TABLE_URI_ID:
			case GENERAL_PREFS_TABLE_URI_ID:
			case SOURCE_TYPE_TABLE_URI_ID:
			case GROUP_NOTIFICATION_TABLE_URI_ID:
			case CONTACT_INFO_TABLE_URI_ID:
			case SOURCE_ACCOUNT_TABLE_URI_ID:
			case NAME_VALUE_DATA_TABLE_URI_ID:
			case CONTACT_RT_PREFS_TABLE_URI_ID:
			case POST_ALARM_TABLE_URI_ID:
			case CONTACT_LIST_PREFS_TABLE_URI_ID:
				return CONTENT_DIR_TYPE;

			case ACCOUNT_ID_URI_ID:
			case ALERT_ITEM_ID_URI_ID:
			case NOTIFICATION_ITEM_ID_URI_ID:
			case FILTER_ITEM_ID_URI_ID:
			case FILTER_ITEM_ACCOUNT_ID_URI_ID:
			case GENERAL_PREFS_ID_URI_ID:
			case SOURCE_TYPE_ID_URI_ID:
			case GROUP_NOTIFICATION_ID_URI_ID:
			case CONTACT_INFO_ID_URI_ID:
			case SOURCE_ACCOUNT_ID_URI_ID:
			case NAME_VALUE_DATA_ID_URI_ID:
			case CONTACT_RT_PREFS_ID_URI_ID:
			case POST_ALARM_ID_URI_ID:
			case CONTACT_LIST_PREFS_ID_URI_ID:
				return CONTENT_ITEM_TYPE;

			default:
				return null;
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		return insert(mDBOpenHelper, uri, values);
	}

	public Uri insert(AutomatonAlertDBOpenHelper helper, Uri uri, ContentValues values) {
		String tableName = "";
		String colHack = "";
		Uri insertUri = null;

		switch (mUriMatcher.match(uri)) {

			case ACCOUNT_TABLE_URI_ID:
				tableName = ACCOUNT_TABLE;
				colHack = ACCOUNT_ID;
				insertUri = ACCOUNT_TABLE_URI;
				break;
			case ALERT_ITEM_TABLE_URI_ID:
				tableName = ALERT_ITEM_TABLE;
				colHack = ALERT_ITEM_ID;
				insertUri = ALERT_ITEM_TABLE_URI;
				break;
			case NOTIFICATION_ITEM_TABLE_URI_ID:
				tableName = NOTIFICATION_ITEM_TABLE;
				colHack = NOTIFICATION_ITEM_ID;
				insertUri = NOTIFICATION_ITEM_TABLE_URI;
				break;
			case FILTER_ITEM_TABLE_URI_ID:
				tableName = FILTER_ITEM_TABLE;
				colHack = FILTER_ITEM_ID;
				insertUri = FILTER_ITEM_TABLE_URI;
				break;
			case FILTER_ITEM_ACCOUNT_TABLE_URI_ID:
				tableName = FILTER_ITEM_ACCOUNT_TABLE;
				colHack = FILTER_ITEM_ACCOUNT_ID;
				insertUri = FILTER_ITEM_ACCOUNT_TABLE_URI;
				break;
			case GENERAL_PREFS_TABLE_URI_ID:
				tableName = GENERAL_PREFS_TABLE;
				colHack = GENERAL_PREFS_ID;
				insertUri = GENERAL_PREFS_TABLE_URI;
				break;
			case SOURCE_TYPE_TABLE_URI_ID:
				tableName = SOURCE_TYPE_TABLE;
				colHack = SOURCE_TYPE_ID;
				insertUri = SOURCE_TYPE_TABLE_URI;
				break;
			case GROUP_NOTIFICATION_TABLE_URI_ID:
				tableName = GROUP_NOTIFICATION_TABLE;
				colHack = GROUP_NOTIFICATION_ID;
				insertUri = GROUP_NOTIFICATION_TABLE_URI;
				break;
			case CONTACT_INFO_TABLE_URI_ID:
				tableName = CONTACT_INFO_TABLE;
				colHack = CONTACT_INFO_ID;
				insertUri = CONTACT_INFO_TABLE_URI;
				break;
			case SOURCE_ACCOUNT_TABLE_URI_ID:
				tableName = SOURCE_ACCOUNT_TABLE;
				colHack = SOURCE_ACCOUNT_ID;
				insertUri = SOURCE_ACCOUNT_TABLE_URI;
				break;
			case NAME_VALUE_DATA_TABLE_URI_ID:
				tableName = NAME_VALUE_DATA_TABLE;
				colHack = NAME_VALUE_DATA_ID;
				insertUri = NAME_VALUE_DATA_TABLE_URI;
				break;
			case CONTACT_RT_PREFS_TABLE_URI_ID:
				tableName = CONTACT_RT_PREFS_TABLE;
				colHack = CONTACT_RT_PREFS_ID;
				insertUri = CONTACT_RT_PREFS_TABLE_URI;
				break;
			case POST_ALARM_TABLE_URI_ID:
				tableName = POST_ALARM_TABLE;
				colHack = POST_ALARM_ID;
				insertUri = POST_ALARM_TABLE_URI;
				break;
			case CONTACT_LIST_PREFS_TABLE_URI_ID:
				tableName = CONTACT_LIST_PREFS_TABLE;
				colHack = CONTACT_LIST_PREFS_ID;
				insertUri = CONTACT_LIST_PREFS_TABLE_URI;
				break;

			default:
				throw new IllegalArgumentException("Unknown URI for Insert: " + uri);

		}

		SQLiteDatabase database = (helper != null) ?
				helper.getWritableDatabase()
				: mDBOpenHelper.getWritableDatabase();

		long rowId = database.insert(
				tableName,
				colHack,
				values
		);

		if (rowId >= 0) {
			// uri + rowid for caller
			Uri resolverUri = ContentUris.withAppendedId(insertUri, rowId);

			// resolver's get notified
			getContext().getContentResolver().notifyChange(resolverUri, null);

			return resolverUri;
		}

		return null;

	}

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		return delete(mDBOpenHelper, uri, where, whereArgs);
	}

	public int delete(AutomatonAlertDBOpenHelper helper,
	                  Uri uri, String where, String[] whereArgs) {

		String tableName = "";
		String idWhere = null;

		// for uri's that are ID-specific
		List<String> segments = uri.getPathSegments();
		int last = segments.size() - 1;

		switch (mUriMatcher.match(uri)) {

			// account
			case ACCOUNT_TABLE_URI_ID:
				tableName = ACCOUNT_TABLE;
				break;
			case ACCOUNT_ID_URI_ID:
				tableName = ACCOUNT_TABLE;
				idWhere =
						ACCOUNT_ID
								+ " = ?";
				whereArgs = new String[] { segments.get(last) };
				break;

			// alert item
			case ALERT_ITEM_TABLE_URI_ID:
				tableName = ALERT_ITEM_TABLE;
				break;
			case ALERT_ITEM_ID_URI_ID:
				tableName = ALERT_ITEM_TABLE;
				idWhere =
						ALERT_ITEM_ID
								+ " = ?";
				whereArgs = new String[] { segments.get(last) };
				break;

			// notification item
			case NOTIFICATION_ITEM_TABLE_URI_ID:
				tableName = NOTIFICATION_ITEM_TABLE;
				break;
			case NOTIFICATION_ITEM_ID_URI_ID:
				tableName = NOTIFICATION_ITEM_TABLE;
				idWhere =
						NOTIFICATION_ITEM_ID
								+ " = ?";
				whereArgs = new String[] { segments.get(last) };
				break;

			// filter item
			case FILTER_ITEM_TABLE_URI_ID:
				tableName = FILTER_ITEM_TABLE;
				break;
			case FILTER_ITEM_ID_URI_ID:
				tableName = FILTER_ITEM_TABLE;
				idWhere =
						FILTER_ITEM_ID
								+ " = ?";
				whereArgs = new String[] { segments.get(last) };
				break;

			// filter item account
			case FILTER_ITEM_ACCOUNT_TABLE_URI_ID:
				tableName = FILTER_ITEM_ACCOUNT_TABLE;
				break;
			case FILTER_ITEM_ACCOUNT_ID_URI_ID:
				tableName = FILTER_ITEM_ACCOUNT_TABLE;
				idWhere =
						FILTER_ITEM_ACCOUNT_ID
								+ " = ?";
				whereArgs = new String[] { segments.get(last) };
				break;

			// general prefs
			case GENERAL_PREFS_TABLE_URI_ID:
				tableName = GENERAL_PREFS_TABLE;
				break;
			case GENERAL_PREFS_ID_URI_ID:
				tableName = GENERAL_PREFS_TABLE;
				idWhere =
						GENERAL_PREFS_ID
								+ " = ?";
				whereArgs = new String[] { segments.get(last) };
				break;

			// source type
			case SOURCE_TYPE_TABLE_URI_ID:
				tableName = SOURCE_TYPE_TABLE;
				break;
			case SOURCE_TYPE_ID_URI_ID:
				tableName = SOURCE_TYPE_TABLE;
				idWhere =
						SOURCE_TYPE_ID
								+ " = ?";
				whereArgs = new String[] { segments.get(last) };
				break;

			// group notification
			case GROUP_NOTIFICATION_TABLE_URI_ID:
				tableName = GROUP_NOTIFICATION_TABLE;
				break;
			case GROUP_NOTIFICATION_ID_URI_ID:
				tableName = GROUP_NOTIFICATION_TABLE;
				idWhere =
						GROUP_NOTIFICATION_ID
								+ " = ?";
				whereArgs = new String[] { segments.get(last) };
				break;

			// contact info
			case CONTACT_INFO_TABLE_URI_ID:
				tableName = CONTACT_INFO_TABLE;
				break;
			case CONTACT_INFO_ID_URI_ID:
				tableName = CONTACT_INFO_TABLE;
				idWhere =
						CONTACT_INFO_ID
								+ " = ?";
				whereArgs = new String[] { segments.get(last) };
				break;

			// source account
			case SOURCE_ACCOUNT_TABLE_URI_ID:
				tableName = SOURCE_ACCOUNT_TABLE;
				break;
			case SOURCE_ACCOUNT_ID_URI_ID:
				tableName = SOURCE_ACCOUNT_TABLE;
				idWhere =
						SOURCE_ACCOUNT_ID
								+ " = ?";
				whereArgs = new String[] { segments.get(last) };
				break;

			// name-value data
			case NAME_VALUE_DATA_TABLE_URI_ID:
				tableName = NAME_VALUE_DATA_TABLE;
				break;
			case NAME_VALUE_DATA_ID_URI_ID:
				tableName = NAME_VALUE_DATA_TABLE;
				idWhere =
						NAME_VALUE_DATA_ID
								+ " = ?";
				whereArgs = new String[] { segments.get(last) };
				break;

			// contactRT prefs
			case CONTACT_RT_PREFS_TABLE_URI_ID:
				tableName = CONTACT_RT_PREFS_TABLE;
				break;
			case CONTACT_RT_PREFS_ID_URI_ID:
				tableName = CONTACT_RT_PREFS_TABLE;
				idWhere =
						CONTACT_RT_PREFS_ID
								+ " = ?";
				whereArgs = new String[] { segments.get(last) };
				break;

			// post alarm
			case POST_ALARM_TABLE_URI_ID:
				tableName = POST_ALARM_TABLE;
				break;
			case POST_ALARM_ID_URI_ID:
				tableName = POST_ALARM_TABLE;
				idWhere =
						POST_ALARM_ID
								+ " = ?";
				whereArgs = new String[] { segments.get(last) };
				break;

			// contact list prefs
			case CONTACT_LIST_PREFS_TABLE_URI_ID:
				tableName = CONTACT_LIST_PREFS_TABLE;
				break;
			case CONTACT_LIST_PREFS_ID_URI_ID:
				tableName = CONTACT_LIST_PREFS_TABLE;
				idWhere =
						CONTACT_LIST_PREFS_ID
								+ " = ?";
				whereArgs = new String[] { segments.get(last) };
				break;

			default:
				throw new IllegalArgumentException("Unknown URI for Delete: " + uri);

		}

		SQLiteDatabase database = (helper != null) ?
				helper.getWritableDatabase()
				: mDBOpenHelper.getWritableDatabase();

		String finalWhere = "";

		if (where != null
				&& idWhere != null) {
			finalWhere = idWhere
					+ " AND "
					+ where;
		}
		else if (where != null) {
			finalWhere = where;
		}
		else {
			finalWhere = idWhere;
		}

		int count = database.delete(
				tableName,
				finalWhere,
				whereArgs
		);

		// notify resolvers
		getContext().getContentResolver().notifyChange(uri, null);

		// number of rows deleted
		return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String where,
	                  String[] whereArgs) {
		return update(mDBOpenHelper, uri, values, where, whereArgs);
	}

	public int update(AutomatonAlertDBOpenHelper helper,
	                  Uri uri, ContentValues values, String where,
	                  String[] whereArgs) {

		String tableName = "";
		String idWhere = null;

		// for uri's that are ID-specific
		List<String> segments = uri.getPathSegments();
		int last = segments.size() - 1;

		switch (mUriMatcher.match(uri)) {

			// account
			case ACCOUNT_TABLE_URI_ID:
				tableName = ACCOUNT_TABLE;
				break;
			case ACCOUNT_ID_URI_ID:
				tableName = ACCOUNT_TABLE;
				idWhere =
						ACCOUNT_ID
								+ " = ?";
				whereArgs = new String[] { segments.get(last) };
				break;

			// alert item
			case ALERT_ITEM_TABLE_URI_ID:
				tableName = ALERT_ITEM_TABLE;
				break;
			case ALERT_ITEM_ID_URI_ID:
				tableName = ALERT_ITEM_TABLE;
				idWhere =
						ALERT_ITEM_ID
								+ " = ?";
				whereArgs = new String[] { segments.get(last) };
				break;

			// notification item
			case NOTIFICATION_ITEM_TABLE_URI_ID:
				tableName = NOTIFICATION_ITEM_TABLE;
				break;
			case NOTIFICATION_ITEM_ID_URI_ID:
				tableName = NOTIFICATION_ITEM_TABLE;
				idWhere =
						NOTIFICATION_ITEM_ID
								+ " = ?";
				whereArgs = new String[] { segments.get(last) };
				break;

			// filter item
			case FILTER_ITEM_TABLE_URI_ID:
				tableName = FILTER_ITEM_TABLE;
				break;
			case FILTER_ITEM_ID_URI_ID:
				tableName = FILTER_ITEM_TABLE;
				idWhere =
						FILTER_ITEM_ID
								+ " = ?";
				whereArgs = new String[] { segments.get(last) };
				break;

			// filter item account
			case FILTER_ITEM_ACCOUNT_TABLE_URI_ID:
				tableName = FILTER_ITEM_ACCOUNT_TABLE;
				break;
			case FILTER_ITEM_ACCOUNT_ID_URI_ID:
				tableName = FILTER_ITEM_ACCOUNT_TABLE;
				idWhere =
						FILTER_ITEM_ACCOUNT_ID
								+ " = ?";
				whereArgs = new String[] { segments.get(last) };
				break;

			// general prefs
			case GENERAL_PREFS_TABLE_URI_ID:
				tableName = GENERAL_PREFS_TABLE;
				break;
			case GENERAL_PREFS_ID_URI_ID:
				tableName = GENERAL_PREFS_TABLE;
				idWhere =
						GENERAL_PREFS_ID
								+ " = ?";
				whereArgs = new String[] { segments.get(last) };
				break;

			// source type
			case SOURCE_TYPE_TABLE_URI_ID:
				tableName = SOURCE_TYPE_TABLE;
				break;
			case SOURCE_TYPE_ID_URI_ID:
				tableName = SOURCE_TYPE_TABLE;
				idWhere =
						SOURCE_TYPE_ID
								+ " = ?";
				whereArgs = new String[] { segments.get(last) };
				break;

			// group notification
			case GROUP_NOTIFICATION_TABLE_URI_ID:
				tableName = GROUP_NOTIFICATION_TABLE;
				break;
			case GROUP_NOTIFICATION_ID_URI_ID:
				tableName = GROUP_NOTIFICATION_TABLE;
				idWhere =
						GROUP_NOTIFICATION_ID
								+ " = ?";
				whereArgs = new String[] { segments.get(last) };
				break;

			// contact info
			case CONTACT_INFO_TABLE_URI_ID:
				tableName = CONTACT_INFO_TABLE;
				break;
			case CONTACT_INFO_ID_URI_ID:
				tableName = CONTACT_INFO_TABLE;
				idWhere =
						CONTACT_INFO_ID
								+ " = ?";
				whereArgs = new String[] { segments.get(last) };
				break;

			// source account
			case SOURCE_ACCOUNT_TABLE_URI_ID:
				tableName = SOURCE_ACCOUNT_TABLE;
				break;
			case SOURCE_ACCOUNT_ID_URI_ID:
				tableName = SOURCE_ACCOUNT_TABLE;
				idWhere =
						SOURCE_ACCOUNT_ID
								+ " = ?";
				whereArgs = new String[] { segments.get(last) };
				break;

			// name-value data
			case NAME_VALUE_DATA_TABLE_URI_ID:
				tableName = NAME_VALUE_DATA_TABLE;
				break;
			case NAME_VALUE_DATA_ID_URI_ID:
				tableName = NAME_VALUE_DATA_TABLE;
				idWhere =
						NAME_VALUE_DATA_ID
								+ " = ?";
				whereArgs = new String[] { segments.get(last) };
				break;

			// contactRT prefs
			case CONTACT_RT_PREFS_TABLE_URI_ID:
				tableName = CONTACT_RT_PREFS_TABLE;
				break;
			case CONTACT_RT_PREFS_ID_URI_ID:
				tableName = CONTACT_RT_PREFS_TABLE;
				idWhere =
						CONTACT_RT_PREFS_ID
								+ " = ?";
				whereArgs = new String[] { segments.get(last) };
				break;

			// post alarm
			case POST_ALARM_TABLE_URI_ID:
				tableName = POST_ALARM_TABLE;
				break;
			case POST_ALARM_ID_URI_ID:
				tableName = POST_ALARM_TABLE;
				idWhere =
						POST_ALARM_ID
								+ " = ?";
				whereArgs = new String[] { segments.get(last) };
				break;

			// contact list prefs
			case CONTACT_LIST_PREFS_TABLE_URI_ID:
				tableName = CONTACT_LIST_PREFS_TABLE;
				break;
			case CONTACT_LIST_PREFS_ID_URI_ID:
				tableName = CONTACT_LIST_PREFS_TABLE;
				idWhere =
						CONTACT_LIST_PREFS_ID
								+ " = ?";
				whereArgs = new String[] { segments.get(last) };
				break;

			default:
				throw new IllegalArgumentException("Unknown URI for Delete: " + uri);

		}

		SQLiteDatabase database = (helper != null) ?
				helper.getWritableDatabase()
				: mDBOpenHelper.getWritableDatabase();

		String finalWhere = idWhere;

		if (where != null
				&& idWhere != null) {
			finalWhere = idWhere
					+ " AND "
					+ where;
		}
		else if (where != null) {
			finalWhere = where;
		}
		else {
			finalWhere = idWhere;
		}

		int count = database.update(
				tableName,
				values,
				finalWhere,
				whereArgs
		);

		// notify resolvers
		getContext().getContentResolver().notifyChange(uri, null);

		// number of rows deleted
		return count;
	}

	public int insertOrUpdate(
			ContentValues cv,
			int id,
			Uri idUri,
			Uri tableUri) {

		// if record exists, update, otherwise insert & return new id
		if (id >= 0) {
			Uri uri = ContentUris.withAppendedId(idUri, id);
			boolean recFound = false;
			Cursor cursor = null;
			try {
				cursor = query(uri, null, null, null, null);
				if (cursor.moveToFirst()) {
					recFound = true;
				}
			}
			catch (IllegalArgumentException ignored) {}
			finally {
				if (cursor != null) {
					cursor.close();
				}
			}
			if (recFound) {
				update(uri, cv, null, null);
				return id;
			}
		}

		Uri retUri = insert(tableUri, cv);

		// save the new id
		if (retUri == null) {
			return -1;
		}
		List<String> segments = retUri.getPathSegments();

		return (Utils.getInt(segments.get(segments.size() - 1), -1));
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ FILL TABLE COLUMNS FOR INSERT, UPDATE

	public static String noNull(String in) {
		return in == null ? "" : in;
	}

	public static String noNull(String in, String defaultString) {
		return in == null ? (defaultString == null ? "" : defaultString) : in;
	}

	public static String booleanToString(Boolean in) {
		if (in.equals(true)) {
			return AutomatonAlert.TRUE;
		}
		return AutomatonAlert.FALSE;
	}

	public static Boolean stringToBoolean(String in) {
		return in.toLowerCase().equals(AutomatonAlert.TRUE);
	}

	// account
	public static ContentValues getAccountContentValues(
			String name,
			String email_address,
			String password,
			String serverName,
			String serverSecurity,
			int serverPort,
			int latestUid,
			String showImages,
			int poll,
			int type,
			String saveToList,
			String markAsSeen,
			long lastChecked) {


		ContentValues cv = new ContentValues();

		// generic
		cv.put(ACCOUNT_NAME, noNull(name));
		cv.put(ACCOUNT_EMAIL_ADDRESS, noNull(email_address));
		cv.put(ACCOUNT_PASSWORD, noNull(password));
		cv.put(ACCOUNT_SERVER_NAME, noNull(serverName));
		cv.put(ACCOUNT_SERVER_SECURITY, noNull(serverSecurity));
		cv.put(ACCOUNT_SERVER_PORT, serverPort);
		cv.put(ACCOUNT_LATEST_UID, latestUid);
		cv.put(ACCOUNT_SHOW_IMAGES, noNull(showImages));
		cv.put(ACCOUNT_POLL, poll);
		cv.put(ACCOUNT_TYPE, type);
		cv.put(ACCOUNT_SAVE_TO_LIST, noNull(saveToList));
		cv.put(ACCOUNT_MARK_AS_SEEN, noNull(markAsSeen));
		cv.put(ACCOUNT_LAST_CHECKED, lastChecked);
		cv.put(ACCOUNT_TIMESTAMP, System.currentTimeMillis());

		return cv;

	}

	// alert item
	public static ContentValues getAlertItemContentValues(
			String type,
			String uid,
			String favorite,
			long dateRemind,
			long repeatEvery,
			long stopAfter,
			String rawFields,
			String markupFields,
			int notificationItemId,
			int accountId,
			String status,
			long dateExpires) {

		ContentValues cv = new ContentValues();

		cv.put(ALERT_ITEM_TYPE, noNull(type));
		cv.put(ALERT_ITEM_UID, noNull(uid));
		cv.put(ALERT_ITEM_FAVORITE, noNull(favorite));
		cv.put(ALERT_ITEM_DATE_REMIND, dateRemind);
		cv.put(ALERT_ITEM_REPEAT_EVERY, repeatEvery);
		cv.put(ALERT_ITEM_STOP_AFTER, stopAfter);
		cv.put(ALERT_ITEM_RAW_FIELDS, rawFields);
		cv.put(ALERT_ITEM_MARKUP_FIELDS, markupFields);
		cv.put(ALERT_ITEM_NOTIFICATION_ITEM_ID, notificationItemId);
		cv.put(ALERT_ITEM_ACCOUNT_ID, accountId);
		cv.put(ALERT_ITEM_STATUS, noNull(status));
		cv.put(ALERT_ITEM_DATE_EXPIRES, dateExpires);
		cv.put(ALERT_ITEM_TIMESTAMP, System.currentTimeMillis());


		return cv;

	}

	// notification item
	public static ContentValues getNotificationItemContentValues(
			String active,
			String templateName,
			String soundPath,
			String soundType,
			int volume,
			String evenWhenSilent,
			String vibrate,
			String noAlertScreen,
			long stopSoundAfter,
			String showInNotificationBar,
			String showNotificationLed,
			String ignoreGlobalQuietPolicy) {

		ContentValues cv = new ContentValues();

		cv.put(NOTIFICATION_ITEM_ACTIVE, noNull(active));
		cv.put(NOTIFICATION_ITEM_TEMPLATE_NAME, noNull(templateName));
		cv.put(NOTIFICATION_ITEM_SOUND_PATH, noNull(soundPath));
		cv.put(NOTIFICATION_ITEM_SOUND_TYPE, noNull(soundType));
		cv.put(NOTIFICATION_ITEM_VOLUME, volume);
		cv.put(NOTIFICATION_ITEM_SILENT_MODE, noNull(evenWhenSilent));
		cv.put(NOTIFICATION_ITEM_VIBRATE_MODE, noNull(vibrate));
		cv.put(NOTIFICATION_ITEM_NO_ALERT_SCREEN, noNull(noAlertScreen));
		cv.put(NOTIFICATION_ITEM_PLAY_FOR, stopSoundAfter);
		cv.put(NOTIFICATION_ITEM_SHOW_NOTIFICATION, noNull(showInNotificationBar));
		cv.put(NOTIFICATION_ITEM_LED_MODE, noNull(showNotificationLed));
		cv.put(NOTIFICATION_ITEM_IGNORE_GLOBAL_QUIET_POLICY, noNull(ignoreGlobalQuietPolicy));
		cv.put(NOTIFICATION_ITEM_TIMESTAMP, System.currentTimeMillis());


		return cv;

	}

	// filter item
	public static ContentValues getFilterItemContentValues(
			String fieldNames,
			String phrase,
			int notificationItemId) {

		ContentValues cv = new ContentValues();

		cv.put(FILTER_ITEM_FIELD_NAMES, noNull(fieldNames));
		cv.put(FILTER_ITEM_PHRASE, noNull(phrase));
		cv.put(FILTER_ITEM_NOTIFICATION_ITEM_ID, notificationItemId);
		cv.put(FILTER_ITEM_TIMESTAMP, System.currentTimeMillis());

		return cv;

	}

	// filter item account
	public static ContentValues getFilterItemAccountContentValues(
			int filterItemId,
			int accountId) {

		ContentValues cv = new ContentValues();

		cv.put(FILTER_ITEM_ACCOUNT_FILTER_ITEM_ID, filterItemId);
		cv.put(FILTER_ITEM_ACCOUNT_ACCOUNT_ID, accountId);
		cv.put(FILTER_ITEM_TIMESTAMP, System.currentTimeMillis());

		return cv;

	}

	// general prefs
	public static ContentValues getGeneralPrefsContentValues(
			String keepLists,
			String keepDeleted,
			long expireDeletedAfter,
			long defaultSnooze,
			String autoAck,
			long autoAckAfter,
			String autoAckAs,
			long ringtoneStopLoopAfter,
			long soundfileStopLoopAfter,
			long quietTimeStart,
			long quietTimeEnd,
			String quietTimePauses,
			String globalPause,
			String alwaysShowNotification,
			String notificationAction,
			int prefetchEmailsCount,
			String markViewedAlertAs,
			String showPollInNotificationBar,
			String showPollInNotificationBarVibrate,
			String startAtBoot,
			String systemOn,
			String pauseAlertsAlarms,
			int maxListSize,
			String overrideVol,
			int imapMaxRetries,
			String foreground,
			String debugMode,
			String lastDbVersionChecked,
			long lastPolled,
			long gcPollInterval) {

		ContentValues cv = new ContentValues();

		cv.put(GENERAL_PREFS_KEEP_LISTS, noNull(keepLists));
		cv.put(GENERAL_PREFS_KEEP_DELETED, noNull(keepDeleted));
		cv.put(GENERAL_PREFS_EXPIRE_DELETED_AFTER, expireDeletedAfter);
		cv.put(GENERAL_PREFS_DEFAULT_SNOOZE, defaultSnooze);
		cv.put(GENERAL_PREFS_AUTO_ACK, noNull(autoAck));
		cv.put(GENERAL_PREFS_AUTO_ACK_AFTER, autoAckAfter);
		cv.put(GENERAL_PREFS_AUTO_ACK_AS, noNull(autoAckAs));
		cv.put(GENERAL_PREFS_RINGTONE_STOP_LOOP_AFTER, ringtoneStopLoopAfter);
		cv.put(GENERAL_PREFS_SOUNDFILE_STOP_LOOP_AFTER, soundfileStopLoopAfter);
		cv.put(GENERAL_PREFS_QUIET_TIME_START, quietTimeStart);
		cv.put(GENERAL_PREFS_QUIET_TIME_END, quietTimeEnd);
		cv.put(GENERAL_PREFS_QUIET_TIME_PAUSES, noNull(quietTimePauses));
		cv.put(GENERAL_PREFS_GLOBAL_PAUSE, noNull(globalPause));
		cv.put(GENERAL_PREFS_ALWAYS_SHOW_NOTIFICATION, noNull(alwaysShowNotification));
		cv.put(GENERAL_PREFS_NOTIFICATION_ACTION, noNull(notificationAction));
		cv.put(GENERAL_PREFS_PREFETCH_EMAILS_COUNT, prefetchEmailsCount);
		cv.put(GENERAL_PREFS_MARK_VIEWED_ALERT_AS, noNull(markViewedAlertAs));
		cv.put(GENERAL_PREFS_SHOW_POLL_IN_NOTIFICATION_BAR, noNull(showPollInNotificationBar));
		cv.put(GENERAL_PREFS_SHOW_POLL_IN_NOTIFICATION_BAR_VIBRATE, noNull(showPollInNotificationBarVibrate));
		cv.put(GENERAL_PREFS_START_AT_BOOT, noNull(startAtBoot));
		cv.put(GENERAL_PREFS_SYSTEM_ON, noNull(systemOn));
		cv.put(GENERAL_PREFS_PAUSE_ALERTS_ALARMS, noNull(pauseAlertsAlarms));
		cv.put(GENERAL_PREFS_MAX_LIST_SIZE, maxListSize);
		cv.put(GENERAL_PREFS_OVERRIDE_VOL, noNull(overrideVol));
		cv.put(GENERAL_PREFS_IMAP_MAX_RETRIES, imapMaxRetries);
		cv.put(GENERAL_PREFS_FOREGROUND, noNull(foreground));
		cv.put(GENERAL_PREFS_DEBUG_MODE, noNull(debugMode));
		cv.put(GENERAL_PREFS_LAST_DB_VERSION_CHECKED, noNull(lastDbVersionChecked));
		cv.put(GENERAL_PREFS_GC_POLL_INTERVAL, gcPollInterval);
		cv.put(GENERAL_PREFS_LAST_POLLED, lastPolled);
		cv.put(GENERAL_PREFS_TIMESTAMP, System.currentTimeMillis());

		return cv;

	}

	// source type
	public static ContentValues getSourceTypeContentValues(
			String lookupKey,
			int notificationItemId,
			String sourceType) {

		ContentValues cv = new ContentValues();

		cv.put(SOURCE_TYPE_LOOKUP_KEY, noNull(lookupKey));
		cv.put(SOURCE_TYPE_NOTIFICATION_ITEM_ID, notificationItemId);
		cv.put(SOURCE_TYPE_SOURCE_TYPE, noNull(sourceType));
		cv.put(SOURCE_TYPE_TIMESTAMP, System.currentTimeMillis());

		return cv;

	}

	// group notification
	public static ContentValues getGroupNotificationContentValues(
			String androidGroupId,
			int notificationItemId) {

		ContentValues cv = new ContentValues();

		cv.put(GROUP_NOTIFICATION_ANDROID_GROUP_ID, noNull(androidGroupId));
		cv.put(GROUP_NOTIFICATION_NOTIFICATION_ITEM_ID, notificationItemId);
		cv.put(GROUP_NOTIFICATION_TIMESTAMP, System.currentTimeMillis());

		return cv;

	}

	// contact info
	public static ContentValues getContactInfoContentValues(
			String lookupKey,
			String favorite) {

		ContentValues cv = new ContentValues();

		cv.put(CONTACT_INFO_LOOKUP_KEY, noNull(lookupKey));
		cv.put(CONTACT_INFO_FAVORITE, noNull(favorite));
		cv.put(CONTACT_INFO_TIMESTAMP, System.currentTimeMillis());

		return cv;

	}

	// source account
	public static ContentValues getSourceAccountContentValues(
			int accountId,
			int sourceTypeId) {

		ContentValues cv = new ContentValues();

		cv.put(SOURCE_ACCOUNT_ACCOUNT_ID, accountId);
		cv.put(SOURCE_ACCOUNT_SOURCE_TYPE_ID, sourceTypeId);
		cv.put(SOURCE_ACCOUNT_TIMESTAMP, System.currentTimeMillis());

		return cv;

	}

	// name-value data
	public static ContentValues getNameValueDataContentValues(
			String name,
			String pref) {

		ContentValues cv = new ContentValues();

		cv.put(NAME_VALUE_DATA_NAME, noNull(name));
		cv.put(NAME_VALUE_DATA_VALUE, noNull(pref));
		cv.put(NAME_VALUE_DATA_TIMESTAMP, System.currentTimeMillis());

		return cv;

	}

	// contactRT prefs
	public static ContentValues getContactRTPrefsContentValues(
			String defaultRingtone,
			String defaultVolume,
			String defaultNewSilentMode,
			Long defaultNewPlayFor,
			String defaultNewVibrateMode,
			Boolean defaultNewNotification,
			String defaultNewLight,
			Integer defaultNewVolume,
			String defaultTextRingtone,
			String defaultTextSilentMode,
			Long defaultTextPlayFor,
			String defaultTextVibrateMode,
			Boolean defaultTextNotification,
			String defaultTextLight,
			Integer defaultTextVolume,
			String defaultLinkedAccountSpecificPrefix,
			String autoAddNewAccountsToDefault,
			String autoAddNewAccountsToActive,
			Boolean mRemindersOn) {

		ContentValues cv = new ContentValues();

		cv.put(CONTACT_RT_PREFS_DEFAULT_RINGTONE, noNull(defaultRingtone));
		cv.put(CONTACT_RT_PREFS_DEFAULT_VOLUME, noNull(defaultVolume));
		cv.put(CONTACT_RT_PREFS_DEFAULT_NEW_SILENT_MODE, noNull(defaultNewSilentMode));
		cv.put(CONTACT_RT_PREFS_DEFAULT_NEW_PLAY_FOR, defaultNewPlayFor);
		cv.put(CONTACT_RT_PREFS_DEFAULT_NEW_VIBRATE_MODE, noNull(defaultNewVibrateMode));
		cv.put(CONTACT_RT_PREFS_DEFAULT_NEW_NOTIFICATION, booleanToString(defaultNewNotification));
		cv.put(CONTACT_RT_PREFS_DEFAULT_NEW_LIGHT, noNull(defaultNewLight));
		cv.put(CONTACT_RT_PREFS_DEFAULT_NEW_VOLUME, defaultNewVolume);
		cv.put(CONTACT_RT_PREFS_DEFAULT_TEXT_RINGTONE, noNull(defaultTextRingtone));
		cv.put(CONTACT_RT_PREFS_DEFAULT_TEXT_SILENT_MODE, noNull(defaultTextSilentMode));
		cv.put(CONTACT_RT_PREFS_DEFAULT_TEXT_PLAY_FOR, defaultTextPlayFor);
		cv.put(CONTACT_RT_PREFS_DEFAULT_TEXT_VIBRATE_MODE, noNull(defaultTextVibrateMode));
		cv.put(CONTACT_RT_PREFS_DEFAULT_TEXT_NOTIFICATION, booleanToString(defaultTextNotification));
		cv.put(CONTACT_RT_PREFS_DEFAULT_TEXT_LIGHT, noNull(defaultTextLight));
		cv.put(CONTACT_RT_PREFS_DEFAULT_TEXT_VOLUME, defaultTextVolume);
		cv.put(CONTACT_RT_PREFS_DEFAULT_LINKED_ACCOUNT_NAME_VALUE_DATA_PREFIX, noNull(defaultLinkedAccountSpecificPrefix));
		cv.put(CONTACT_RT_PREFS_AUTO_ADD_NEW_ACCOUNTS_TO_DEFAULT, noNull(autoAddNewAccountsToDefault));
		cv.put(CONTACT_RT_PREFS_AUTO_ADD_NEW_ACCOUNTS_TO_ACTIVE, noNull(autoAddNewAccountsToActive));
		cv.put(CONTACT_RT_PREFS_REMINDERS_ON, booleanToString(mRemindersOn));
		cv.put(CONTACT_RT_PREFS_TIMESTAMP, System.currentTimeMillis());

		return cv;

	}

	// post alarm
	public static ContentValues getPostAlarmContentValues(
			FragmentTypeRT type,
			int alertItemId,
			int notificationItemId,
			long next_alarm,
			long orig_alarm) {

		ContentValues cv = new ContentValues();

		cv.put(POST_ALARM_TYPE, noNull(type.name()));
		cv.put(POST_ALARM_ALERT_ITEM_ID, alertItemId);
		cv.put(POST_ALARM_NOTIFICATION_ITEM_ID, notificationItemId);
		cv.put(POST_ALARM_NEXT_ALARM, next_alarm);
		cv.put(POST_ALARM_ORIG_ALARM, orig_alarm);
		cv.put(POST_ALARM_TIMESTAMP, System.currentTimeMillis());

		return cv;

	}

	// contact list prefs
	public static ContentValues getContactListPrefsContentValues(
			boolean showText,
			boolean showPhone,
			boolean showEmail) {

		ContentValues cv = new ContentValues();

		String TRUE = AutomatonAlert.TRUE;
		String FALSE = AutomatonAlert.FALSE;

		cv.put(CONTACT_LIST_PREFS_SHOW_TEXT, noNull(showText ? TRUE : FALSE));
		cv.put(CONTACT_LIST_PREFS_SHOW_PHONE, noNull(showPhone ? TRUE : FALSE));
		cv.put(CONTACT_LIST_PREFS_SHOW_EMAIL, noNull(showEmail ? TRUE : FALSE));
		cv.put(POST_ALARM_TIMESTAMP, System.currentTimeMillis());

		return cv;

	}
}
