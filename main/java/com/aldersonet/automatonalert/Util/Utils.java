package com.aldersonet.automatonalert.Util;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Environment;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.PhoneLookup;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.aldersonet.automatonalert.Account.AccountDO;
import com.aldersonet.automatonalert.Account.Accounts;
import com.aldersonet.automatonalert.ActionMode.ContactListInfo;
import com.aldersonet.automatonalert.Activity.AlertListActivity;
import com.aldersonet.automatonalert.Activity.IActivityRefresh;
import com.aldersonet.automatonalert.Activity.InAppPurchasesActivity;
import com.aldersonet.automatonalert.Activity.RTUpdateActivity.FragmentTypeRT;
import com.aldersonet.automatonalert.Alarm.AlarmPendingIntent;
import com.aldersonet.automatonalert.Alarm.AlarmPendingIntent.ApiSubType;
import com.aldersonet.automatonalert.Alarm.AlarmPendingIntent.ApiType;
import com.aldersonet.automatonalert.Alert.AlertItemDO;
import com.aldersonet.automatonalert.Alert.AlertItems;
import com.aldersonet.automatonalert.Alert.NotificationItemDO;
import com.aldersonet.automatonalert.Alert.NotificationItems;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.BuildConfig;
import com.aldersonet.automatonalert.ContactInfo.ContactInfoDO;
import com.aldersonet.automatonalert.Email.AccountEmailDO;
import com.aldersonet.automatonalert.Filter.FilterItemAccountDO;
import com.aldersonet.automatonalert.Filter.FilterItemAccounts;
import com.aldersonet.automatonalert.Filter.FilterItemDO;
import com.aldersonet.automatonalert.Filter.FilterItems;
import com.aldersonet.automatonalert.Fragment.VolumeChooserFragment.VolumeTypes;
import com.aldersonet.automatonalert.InAppPurchases.InAppPurchases;
import com.aldersonet.automatonalert.OkCancel.OkCancel;
import com.aldersonet.automatonalert.OkCancel.OkCancelDialog;
import com.aldersonet.automatonalert.OurDir.OurDir;
import com.aldersonet.automatonalert.PostAlarm.PostAlarmDO;
import com.aldersonet.automatonalert.Preferences.GeneralPrefsDO;
import com.aldersonet.automatonalert.Preferences.NameValueDataDO;
import com.aldersonet.automatonalert.Provider.AutomatonAlertProvider;
import com.aldersonet.automatonalert.R;
import com.aldersonet.automatonalert.Receiver.AlertReceiver;
import com.aldersonet.automatonalert.Receiver.BootReceiver;
import com.aldersonet.automatonalert.SMS.AccountSmsDO;
import com.aldersonet.automatonalert.SourceAccount.SourceAccountDO;
import com.aldersonet.automatonalert.SourceType.SourceTypeDO;

import org.apache.k9.Base64InputStream;
import org.apache.k9.Hex;
import org.apache.k9.IOUtils;
import org.apache.k9.QuotedPrintableInputStream;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringBufferInputStream;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.aldersonet.automatonalert.Fragment.VolumeChooserFragment.VolumeTypes.ringtone;

public class Utils {
	public static final String TAG = "Utils";

	public static final String FLDSEP = "#@%;%@#";
	public static final String FLDEQ = "%@#=#@%";

	public static final String NO_SUBJECT_HEADER = "(no subject)";
	public static final String HTML_SUBJECT_MARKUP_TAG = "<em>";
	public static final String HTML_SUBJECT_MARKUP_TAG_END = "</em>";
	public static final String HTML_DATE_MARKUP_TAG = "<small>";
	public static final String HTML_DATE_MARKUP_TAG_END = "</small>";
	public static final String HTML_FROM_MARKUP_TAG = "<pre>";
	public static final String HTML_FROM_MARKUP_TAG_END = "</pre>";

	public static final String SET_DATE_LABEL = "Set Date and Time";
	public static final String SET_TIME_LABEL = "Set Time";

	public static String decodeQuotedBase64(final String body,
			final String encoding) throws IOException {

		String[] charset = {""};
		String[] encodingArray = {encoding};
		String mBody = body;

		// if it starts with "=?<charset>?<encoding>?" then get encoding
		mBody = checkPrefixEncoded(body, charset, encodingArray);

		@SuppressWarnings("deprecation")
		final StringBufferInputStream bodyIn = new StringBufferInputStream(mBody);
		InputStream in = null;

		if (encodingArray[0] != null) {
			if (encodingArray[0].equalsIgnoreCase(AutomatonAlert.QUOTED_PRINTABLE)) {
				in = new QuotedPrintableInputStream(bodyIn);
			} else if (encodingArray[0].equalsIgnoreCase(AutomatonAlert.BASE64)) {
				in = new Base64InputStream(bodyIn);
			} else {
				return mBody;
			}
		} else {
			return mBody;
		}

		final OutputStream out = new ByteArrayOutputStream();
		IOUtils.copy(in, out);
		out.close();

		return out.toString();
	}

	private static Pattern mRegExCharSetEncoding =
			Pattern.compile("^=\\?\\S+\\?\\S\\?.*");

	public static String checkPrefixEncoded(String in, String[] charset, String[] encoding) {

		// starts with "=?<charset>?<encoding>?"
		if (mRegExCharSetEncoding.matcher(in).matches()) {
			int[] qm = new int[] {0, 0, 0};
			qm[0] = in.indexOf('?');
			qm[1] = in.indexOf('?', qm[0]+1);
			qm[2] = in.indexOf('?', qm[1]+1);

			if (qm[0] < qm[1] && qm[1] < qm[2]) {
				charset[0] = in.substring(qm[0]+1, qm[1]);
				encoding[0] = in.substring(qm[1]+1, qm[2]);
				if (encoding[0].equalsIgnoreCase("Q")) {
					encoding[0] = AutomatonAlert.QUOTED_PRINTABLE;
				}
				else if (encoding[0].equalsIgnoreCase("B")) {
					encoding[0] = AutomatonAlert.BASE64;
				}
				in = in.substring(qm[2]+1);
			}
		}
		return in;
	}

	private static Pattern mRegExHtmlEscapedLessThan =
			Pattern.compile(
					AutomatonAlert.HTML_ESCAPED_LESS_THAN,
					Pattern.CASE_INSENSITIVE);
	private static Pattern mRegExHtmlEscapedGreaterThan =
			Pattern.compile(
					AutomatonAlert.HTML_ESCAPED_GREATER_THAN,
					Pattern.CASE_INSENSITIVE);

	private static String escapedToLTGT(String s) {
		if (s == null) {
			return null;
		}
		s = mRegExHtmlEscapedLessThan.matcher(s).replaceAll("<");
		s = mRegExHtmlEscapedGreaterThan.matcher(s).replaceAll(">");
		return s;
	}

	private static Pattern mRegExLessThan =
			Pattern.compile("<", Pattern.CASE_INSENSITIVE);
	private static Pattern mRegExGreaterThan =
			Pattern.compile(">", Pattern.CASE_INSENSITIVE);

	private static String escapeLTGT(String s) {
		if (s == null) {
			return null;
		}
		s = mRegExLessThan.matcher(s).replaceAll(
				AutomatonAlert.HTML_ESCAPED_LESS_THAN);
		s = mRegExGreaterThan.matcher(s).replaceAll(
				AutomatonAlert.HTML_ESCAPED_GREATER_THAN);
		return s;
	}

	// for CONTACT headers
	public static String formatHeadersForView(
			final HashMap<String, String> message) {
		String sHeader = "";

		String[] inboxHeaders = AutomatonAlert.ALERT_INBOX_HEADERS_FOR_VIEW;

		for (final String fieldHeader : inboxHeaders) {
			if (fieldHeader == null) {
				continue;
			}
			String value = message.get(fieldHeader);

			// don't line break on last line (subject)
			if (value == null) {
				continue;
			}

			if (fieldHeader.equalsIgnoreCase(AutomatonAlert.ACCOUNT_KEY)) {
				if (AccountSmsDO.SMS_KEY.equals(value)) {
					continue;
				}
				else {
					value = value.split("\\|")[0];
				}
			}
			else if (fieldHeader.equalsIgnoreCase(AutomatonAlert.FROM)
					|| fieldHeader.equalsIgnoreCase(AutomatonAlert.TO)
					|| fieldHeader.equalsIgnoreCase(AutomatonAlert.CC)
					|| fieldHeader.equalsIgnoreCase(AutomatonAlert.BCC)) {
				value = stripAllEmailAddresses(value);
			}

			// don't add <br> to last header
			if (!fieldHeader.equalsIgnoreCase(inboxHeaders[inboxHeaders.length-1])) {
				value += "<br>";
			}
			if (value.startsWith(fieldHeader + ": ")) {
				value = value.substring(value.indexOf(":")+1);
			}
			sHeader += "<b>"//<font color=\'#0099CC\'>"
					+ fieldHeader
					+ ": </b>";//</font>";
			sHeader += value;
		}
		return sHeader;
	}

	// for SEARCH headers
	public static String formatHeadersForView(
			final HashMap<String, String> message,
			boolean showAllHeaderLabels) {
		String sHeader = "";

		for (final String fieldHeader : AutomatonAlert.HEADERS_FOR_VIEW) {
			String value = message.get(fieldHeader);

			// don't line break on last line (subject)
			if (value != null) {
				if (fieldHeader.equalsIgnoreCase(AutomatonAlert.ACCOUNT_KEY)) {
					if (AccountSmsDO.SMS_KEY.equals(value)) {
						value = AccountSmsDO.SMS_NAME;
					}
					else {
						value = value.split("\\|")[0];
					}
				}
				else if (fieldHeader.equalsIgnoreCase(AutomatonAlert.DATE)) {
					value = stripMarkupFromBeginningAndEnd(value);
					Long lDate = Utils.getLong(value, -1);
					value = (lDate != -1) ? smallDate(lDate) : "<unknown date>";
				}
				if (!fieldHeader.equalsIgnoreCase(AutomatonAlert.SUBJECT)) {
					value += "<br>";
				}
				if (showAllHeaderLabels) {
					value += "<br>";
					if (value.startsWith(fieldHeader + ": ")) {
						value = value.substring(value.indexOf(":")+1);
					}
					sHeader += "<b>" + fieldHeader + ": </b><br>";
				}
				sHeader += value;
			}
		}
		return sHeader;
	}

	public static List<String> intStringToList(final String string,
			final String delim, String tag) {

		final List<String> list = new ArrayList<String>();

		final String sParts[] = string.split(delim);
		for (final String sPart : sParts) {
			if (sPart.equals(tag)) {
				continue;
			}
			if (!TextUtils.isEmpty(sPart)
					&& TextUtils.isDigitsOnly(sPart)) {
				list.add(sPart);
			}
		}

		return list;
	}

	public static String markUpFoundString(final Matcher m) {
		try {
			return m.replaceAll(
					AutomatonAlert.HTML_FONT_COLOR_RED_START_TAG +
					m.group() +
					AutomatonAlert.HTML_FONT_END_TAG);

		} catch (IndexOutOfBoundsException ignored) {}

		return null;
	}

	private static Pattern mRegExHeaderMarkups =
			Pattern.compile(
					"("
							+ HTML_SUBJECT_MARKUP_TAG + "|" + HTML_SUBJECT_MARKUP_TAG_END
							+ "|"
							+ HTML_DATE_MARKUP_TAG + "|" + HTML_DATE_MARKUP_TAG_END
							+ "|"
							+ HTML_FROM_MARKUP_TAG + "|" + HTML_FROM_MARKUP_TAG_END
							+ ")",
							Pattern.CASE_INSENSITIVE);
	private static Pattern mRegExFontEnclose =
			Pattern.compile(
					"(<font[^>]*>|</font>)",
					Pattern.CASE_INSENSITIVE);

	public static String stripMarkupHeaderField(
			final String inHeaders, final boolean stripHighlightedSearchPhrases) {

		String s = mRegExHeaderMarkups.matcher(inHeaders).replaceAll("");

		if (stripHighlightedSearchPhrases) {
			s = mRegExFontEnclose.matcher(s).replaceAll("");
		}

		s = escapedToLTGT(s);

		return s;

	}

	public static HashMap<String, String> markupHeaderFields(
			final HashMap<String, String> inFields) {

		HashMap<String, String> outFields = new HashMap<String, String>();

		if (inFields != null) {
			for (Entry<String, String> field : inFields.entrySet()) {
				String out = markupHeaderField(field.getKey(), field.getValue());
				if (!(TextUtils.isEmpty(out))) {
					outFields.put(field.getKey(), out);
				}
			}
		}

		return outFields;
	}

	public static String markupHeaderField(final String inKey,
			final String inValue) {

		String value = escapeLTGT(inValue);

		String sHeader = "";
		// CC & BCC get field header
		if (inKey.equalsIgnoreCase(AutomatonAlert.CC)
				|| inKey.equalsIgnoreCase(AutomatonAlert.BCC)
				|| inKey.equalsIgnoreCase(AutomatonAlert.FROM)
				|| inKey.equalsIgnoreCase(AutomatonAlert.TO)) {
			sHeader += inKey + ": ";
		}

		// do the markup
		if (inKey.equalsIgnoreCase(AutomatonAlert.SUBJECT)) {
			// if no subject, add default
			if (TextUtils.isEmpty(value)) {
				value = NO_SUBJECT_HEADER;
			}
			value = HTML_SUBJECT_MARKUP_TAG + value + HTML_SUBJECT_MARKUP_TAG_END;

		} else if (inKey.equalsIgnoreCase(AutomatonAlert.DATE)) {
			value = HTML_DATE_MARKUP_TAG + value + HTML_DATE_MARKUP_TAG_END;

		} else if (inKey.equalsIgnoreCase(AutomatonAlert.FROM)) {
			value = HTML_FROM_MARKUP_TAG + value + HTML_FROM_MARKUP_TAG_END;

		}
		sHeader += value;

		return sHeader;
	}

	public Utils() {
	}

	public static final int
			TYPE = 0,
			CHARSET = 1,
			ENCODING = 2,
			BODYPART = 3;

	public static String[] getBodyDescriptors(String string) {
		String fields[] = string.split("\\|", 5);

		if (fields.length != 5) {
			fields = new String[]
					{
					String.valueOf(AutomatonAlert.HTML),
					"ASCII",
					AutomatonAlert.QUOTED_PRINTABLE,
					"1",
					string
					};
		}
		return fields;
	}


	public static String[] getBodyDescriptors(String[][] fields) {
		String[] bodyDescriptors = new String[4];
		final int 	TYPE = 0,
				CHARSET = 1,
				ENCODING = 2,
				BODYPART = 3;

		int type = -1;
		String charset = "ASCII";
		String encoding = AutomatonAlert.QUOTED_PRINTABLE;
		String position = "";
		String bodyPart = "1"; // default to mBody[1]

		// preferred
		position = fields[AutomatonAlert.HTML][AutomatonAlert.POSITION];
		if (!(TextUtils.isEmpty(position))) {
			type = AutomatonAlert.HTML;
			encoding = fields[AutomatonAlert.HTML][AutomatonAlert.ENCODING];
			charset = fields[AutomatonAlert.HTML][AutomatonAlert.CHARSET];
			bodyPart = position;
		} else {
			// secondary preference
			// add html/mBody tags so that we'll always show html in WebView
			position = fields[AutomatonAlert.PLAIN][AutomatonAlert.POSITION];
			if (!(TextUtils.isEmpty(position))) {
				type = AutomatonAlert.PLAIN;
				encoding = fields[AutomatonAlert.PLAIN][AutomatonAlert.ENCODING];
				charset = fields[AutomatonAlert.HTML][AutomatonAlert.CHARSET];
				bodyPart = position;
			}
		}
		if (TextUtils.isEmpty(charset)) {
			charset = "ASCII";
		}

		bodyDescriptors[TYPE] = String.valueOf(type);
		bodyDescriptors[CHARSET] = charset;
		bodyDescriptors[ENCODING] = encoding;
		bodyDescriptors[BODYPART] = bodyPart;

		return bodyDescriptors;
	}

	public static boolean equals(HashMap<String, String> x,
			HashMap<String, String> y) {

		if (x == null || y == null) {
			return false;
		}

		// check for null
		// also, if one is null, return no match
		//       if both are null, return match
		String sX = x.get(AutomatonAlert.UID);
		String sY = y.get(AutomatonAlert.UID);
		if (sX == null || sY == null) {
			return sX == null && sY == null;
		}

		// look at key fields only
		if (!((x.get(AutomatonAlert.UID)).equals(y.get(AutomatonAlert.UID)))) {
			return false;
		}

		// check for null
		// also, if one is null, return no match
		//       if both are null, return match
		sX = x.get(AutomatonAlert.ACCOUNT_KEY);
		sY = y.get(AutomatonAlert.ACCOUNT_KEY);
		if (sX == null || sY == null) {
			return sX == null && sY == null;
		}

		if (!((x.get(AutomatonAlert.ACCOUNT_KEY)).equals(y.get(AutomatonAlert.ACCOUNT_KEY)))) {
			return false;
		}
		else {
			// if SMS, need to look at message body too
			if ((x.get(AutomatonAlert.ACCOUNT_KEY).equals(AccountSmsDO.SMS_KEY))) {
				sX = x.get(AutomatonAlert.SMS_BODY);
				sY = y.get(AutomatonAlert.SMS_BODY);
				if (sX == null || sY == null) {
					return sX == null && sY == null;
				}
				return x.get(AutomatonAlert.SMS_BODY).equals(y.get(AutomatonAlert.SMS_BODY));
			}
		}

		return true;
	}

	public static boolean equals(HashSet<HashMap<String, String>> x,
			HashSet<HashMap<String, String>> y) {

		if (x == null || y == null) {
			return false;
		}

		// have more/less messages?
		if (x.size() != y.size()) {
			return false;
		}

		for (HashMap<String, String> mX : x) {
			boolean found = false;
			for (HashMap<String, String> mY : y) {
				if (equals(mX, mY)) {
					found = true;
					break;
				}
			}
			if (!found) {
				return false;
			}
		}
		return true;

	}

	public static boolean equals(ArrayList<HashMap<String, String>> x,
			ArrayList<HashMap<String, String>> y) {

		HashSet<HashMap<String, String>> xHs = new HashSet<HashMap<String, String>>(x);
		HashSet<HashMap<String, String>> yHs = new HashSet<HashMap<String, String>>(y);
		return equals(xHs, yHs);
	}

	// strings don't have to be in order
	public static boolean equals(String[] sa1, String[] sa2) {
		boolean equal = false;

		if (sa1.length != sa2.length) {
			return false;
		}
		else {
			for (String s1 : sa1) {
				equal = false;
				for (String s2 : sa2) {
					if(s1.equals(s2)) {
						equal = true;
						break;
					}
				}
				if (!equal) {
					return false;
				}
			}
		}
		return true;
	}

	// strings don't have to be in order
	public static boolean equals(String[][] saa1, String[][] saa2) {
		boolean equal;

		if (saa1 == null || saa2 == null) {
			return false;
		}
		if (saa1.length != saa2.length) {
			return false;
		}

		for (String[] sa1 : saa1) {
			equal = false;
			for (String sa2[] : saa2) {
				if (equals(sa1, sa2)) {
					equal = true;
					break;
				}
			}
			if (!equal) {
				return false;
			}
		}

		return true;
	}

	public static boolean equals(AccountDO a1, AccountDO a2) {
		return a1.getKey().equalsIgnoreCase(a2.getKey());
	}

	public static String getContactNameFromNumber(String number) {
        Log.d(TAG, "getContactNameFromNumber(): number: " + number);
		if (TextUtils.isEmpty(number)) {
			return "";
		}

		String[] projection = new String[]{ContactsContract.Contacts.DISPLAY_NAME};

		Uri contactUri = Uri.withAppendedPath(
				PhoneLookup.CONTENT_FILTER_URI,
				Uri.encode(number));

        Cursor cursor = null;
        cursor = AutomatonAlert.THIS.getContentResolver().query(
                contactUri, projection, null, null, null);
        if (cursor == null) return number;

		try {
			if (cursor.moveToFirst()) {
				return cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
			}
			return number;

		} finally {
            cursor.close();
		}
	}

	public static void reShowListActivityIfOnTop(Context context) {
		ActivityManager am =
				(ActivityManager)AutomatonAlert.THIS.getSystemService(
						Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningTaskInfo> rtl = am.getRunningTasks(1);
		for (ActivityManager.RunningTaskInfo rt : rtl) {
			if (rt.topActivity.getClassName().equals(
					AlertListActivity.class.getName())) {
				final Intent intent = new Intent(context, AlertListActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
				AutomatonAlert.THIS.startActivity(intent);
			}
		}
	}

	public static String translateMillis(long millis, boolean shortTrans) {

		String millisecond = " millisecond";
		String second = " second";
		String minute = " minute";
		String hour = " hour";
		String day = " day";
		String week = " week";
		String loop = " loop";

		if (shortTrans) {
			millisecond = "ms";
			second = "s";
			minute = "m";
			hour = "h";
			day = "d";
			week = "w";
		}

		String suffix = "";

		if (millis < 100) {
			if (millis > 1
					&& !shortTrans) {
				suffix = "s";
			}
			return "" + millis + loop + suffix;
		}

		if (millis < 1000) {
			if (millis > 1
					&& !shortTrans) {
				suffix = "s";
			}
			return "" + millis + millisecond + suffix;
		}

		millis /= 1000;
		if (millis < 60) {
			if (millis > 1
					&& !shortTrans) {
				suffix = "s";
			}
			return "" + millis + second + suffix;
		}
		millis /= 60;
		if (millis < 60) {
			if (millis > 1
					&& !shortTrans) {
				suffix = "s";
			}
			return "" + millis + minute + suffix;
		}
		millis /= 60;
		if (millis < 24) {
			if (millis > 1
					&& !shortTrans) {
				suffix = "s";
			}
			return "" + millis + hour + suffix;
		}
		millis /= 24;
		if (millis < 7) {
			if (millis > 1
					&& !shortTrans) {
				suffix = "s";
			}
			return "" + millis + day + suffix;
		}
		millis /= 7;
		if (millis > 1
				&& !shortTrans) {
			suffix = "s";
		}
		return "" + millis + week + suffix;



	}

	public static String getDateForButtonDisplay(long in) {
		Calendar cal = Calendar.getInstance(Locale.getDefault());
		cal.setTimeInMillis(in);
		int y = cal.get(Calendar.YEAR);
		int m = cal.get(Calendar.MONTH);
		int d = cal.get(Calendar.DATE);

		return getDateForButtonDisplay(y, m, d);
	}

	public static String getDateForButtonDisplay(int inYear, int inMonth, int inDay) {
		Calendar cal = Calendar.getInstance(Locale.getDefault());
		cal.set(Calendar.MONTH, inMonth);
		CharSequence month = android.text.format.DateFormat.format("MMM", cal);

		return inMonth == -1 ?
				SET_DATE_LABEL
				:
				Locale.getDefault().equals(Locale.US) ?
						month
								+ " "
								+ inDay
								+ ", "
								+ inYear
						:
						inDay
								+ " "
								+ month
								+ ", "
								+ inYear;
	}

	public static String getTimeForButtonDisplay(long in) {
		Calendar cal = Calendar.getInstance(Locale.getDefault());
		cal.setTimeInMillis(in);
		return getTimeForButtonDisplay(
				cal.get(Calendar.HOUR_OF_DAY),
				cal.get(Calendar.MINUTE));
	}

	public static String getTimeForButtonDisplay(int inHour, int inMinute) {
		String s = "";

		int hour = ((inHour-1)%12)+1;
		if (hour == 0) {
			hour = 12;
		}
		s = ((inMinute == -1) ? SET_TIME_LABEL : (hour
				+ ":"
				+ ((inMinute < 10) ? "0" : "")
				+ inMinute
				+ ((((inHour * 100) + inMinute) <= 1159) ? "am" : "pm")));

		return s;
	}

	public static int numberOfPolledAccounts(boolean setCeiling) {

		int numberOfPolledAccounts = 0;

		ArrayList<AccountDO> accounts = Accounts.get();

		for (final AccountDO account : accounts) {
			// only check if poll is not Never/Manual
			if (account.mAccountType == AccountEmailDO.ACCOUNT_EMAIL
					&& ((AccountEmailDO)account).getPoll() > 0) {

				// ceiling for counting an account as polled
				// e.g., if celing is 15 minutes, only accounts
				// that poll <= 15 minutes are counted
				if (setCeiling) {
					if (((AccountEmailDO)account).getPoll() <=
							AutomatonAlert.MAX_TIME_TO_BE_CONSIDERED_POLLED_ACCOUNT) {
						++numberOfPolledAccounts;
					}
				}
				else {
					++numberOfPolledAccounts;
				}
			}
		}
		return numberOfPolledAccounts;
	}

	public static void showSetAlarmToast(Context context, long millisTime, boolean snooze) {
		long now = 0;
		if (snooze) {
			now = System.currentTimeMillis();
		}
		else {
			now = AlertItemDO.cutOffSeconds(System.currentTimeMillis());
		}
		String msg = Utils.getTimeRemainingMillis(millisTime - now, snooze);

		Utils.toastIt(context,
				(snooze ? "Snooze" : "Reminder")
				+ (TextUtils.isEmpty(msg) ?
						" Set"
						:
						" set for " + msg + " from now"));
	}

	public static String getTimeRemainingMillis(long millis, boolean showSeconds) {
		long years = 0;
		long weeks = 0;
		long days = TimeUnit.DAYS.convert(millis, TimeUnit.MILLISECONDS);
		long rem = millis / (24 * 60 * 60 * 1000);
		if (days <= 0 && rem <= 0) {
			rem = millis;
		}
		else {
			weeks = days / 7;
			if (weeks > 0) {
				days %= 7;
				years = weeks / 52;
				if (years > 0) {
					weeks %= 52;
				}
			}
		}
		long hold = rem;
		long hours = TimeUnit.HOURS.convert(rem, TimeUnit.MILLISECONDS);
		rem %= (60 * 60 * 1000);
		if (hours <= 0 && rem <= 0) {
			rem = hold;
		}
		hold = rem;
		long minutes = TimeUnit.MINUTES.convert(rem, TimeUnit.MILLISECONDS);
		rem %= (60 * 1000);
		if (minutes <= 0 && rem <= 0) {
			rem = hold;
		}

		long seconds = TimeUnit.SECONDS.convert(rem, TimeUnit.MILLISECONDS);

		String result = "";
		if (years > 0) {
			if (!TextUtils.isEmpty(result)) {
				result += ", ";
			}
			result += years + " " + returnPluralOrOriginal((int)years, "year", "s");
		}
		if (weeks > 0) {
			if (!TextUtils.isEmpty(result)) {
				result += ", ";
			}
			result += weeks + " " + returnPluralOrOriginal((int)weeks, "week", "s");
		}
		if (days > 0) {
			if (!TextUtils.isEmpty(result)) {
				result += ", ";
			}
			result += days + " " + returnPluralOrOriginal((int)days, "day", "s");
		}
		if (hours > 0) {
			if (!TextUtils.isEmpty(result)) {
				result += ", ";
			}
			result += hours + " " + returnPluralOrOriginal((int)hours, "hour", "s");
		}
		if (minutes > 0) {
			if (!TextUtils.isEmpty(result)) {
				result += ", ";
			}
			result += minutes + " " + returnPluralOrOriginal((int)minutes, "minute", "s");
		}
		if (seconds > 0
				&& showSeconds) {
			if (!TextUtils.isEmpty(result)) {
				result += ", ";
			}
			result += seconds + " " + returnPluralOrOriginal((int)seconds, "second", "s");
		}

		return result;
	}

	public static void toastIt(Context context, String toast) {
		Toast.makeText(context, toast, Toast.LENGTH_SHORT)
				.show();
	}

	private static AlarmPendingIntent getGCPollAlarmPendingIntent(Context context) {
		// gc poll alarms (in this case, the GC_POLL don't kill alarm
		Intent intent = new Intent(context, AlertReceiver.class);
		intent.setAction(AutomatonAlert.GC_POLL);
		intent.setFlags(0);

		return new AlarmPendingIntent(
				ApiType.GC_POLL,
				ApiSubType.NONE,
				-1,
				-1,
				-1,
				123456,
				intent,
				PendingIntent.FLAG_CANCEL_CURRENT);
	}

	/* set poll for now+millis and repeats every millis */
	public static void setGCPollAlarm(Context context) {
		long milliseconds = GeneralPrefsDO.getGCPollInterval();
		AlarmPendingIntent api = getGCPollAlarmPendingIntent(context);
		api.setRepeatingAlarm(milliseconds);

		Log.d(
				TAG + ".setGCPollAlarm()",
				"GC_POLL set for " + Utils.translateMillis(milliseconds, false));
	}

	public static void cancelGCPollAlarm() {
		AutomatonAlert.getAPIs().findCancelRemovePendingIntentsPostAlarms(
				ApiType.GC_POLL,
				ApiSubType.NONE,
				-1,
				-1,
				-1);
	}

	public static String translatePhoneType(int type, String label) {
		switch (type) {
		case ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM:
			if (!TextUtils.isEmpty(label)) {
				return label;
			}
			else {
				return "Other";
			}
		case ContactsContract.CommonDataKinds.Phone.TYPE_HOME:
			return "Home";
		case ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE:
			return "Mobile";
		case ContactsContract.CommonDataKinds.Phone.TYPE_WORK:
			return "Work";
		case ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK:
			return "Fax Work";
		case ContactsContract.CommonDataKinds.Phone.TYPE_FAX_HOME:
			return "Fax Home";
		case ContactsContract.CommonDataKinds.Phone.TYPE_PAGER:
			return "Pager";
		case ContactsContract.CommonDataKinds.Phone.TYPE_OTHER:
			return "Other";
		case ContactsContract.CommonDataKinds.Phone.TYPE_CALLBACK:
			return "Callback";
		case ContactsContract.CommonDataKinds.Phone.TYPE_CAR:
			return "Car";
		case ContactsContract.CommonDataKinds.Phone.TYPE_COMPANY_MAIN:
			return "Company Main";
		case ContactsContract.CommonDataKinds.Phone.TYPE_ISDN:
			return "ISDN";
		case ContactsContract.CommonDataKinds.Phone.TYPE_MAIN:
			return "Main";
		case ContactsContract.CommonDataKinds.Phone.TYPE_OTHER_FAX:
			return "Other Fax";
		case ContactsContract.CommonDataKinds.Phone.TYPE_RADIO:
			return "Radio";
		case ContactsContract.CommonDataKinds.Phone.TYPE_TELEX:
			return "Telex";
		case ContactsContract.CommonDataKinds.Phone.TYPE_TTY_TDD:
			return "TTY TDD";
		case ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE:
			return "Work Mobile";
		case ContactsContract.CommonDataKinds.Phone.TYPE_WORK_PAGER:
			return "Work Pager";
		case ContactsContract.CommonDataKinds.Phone.TYPE_ASSISTANT:
			return "Assistant";
		case ContactsContract.CommonDataKinds.Phone.TYPE_MMS:
			return "MMS";
		default:
			return "Other";
		}
	}

	public static String translateEmailType(int type, String label) {

		switch (type) {
		case ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM:
			if (!TextUtils.isEmpty(label)) {
				return label;
			}
			else {
				return "Other";
			}
		case ContactsContract.CommonDataKinds.Email.TYPE_HOME:
			return "Home";
		case ContactsContract.CommonDataKinds.Email.TYPE_MOBILE:
			return "Mobile";
		case ContactsContract.CommonDataKinds.Email.TYPE_WORK:
			return "Work";
		case ContactsContract.CommonDataKinds.Email.TYPE_OTHER:
			return "Other";
		default:
			return "Other";
		}
	}

	public static int getIndexOfEntry(String from, String[] in) {
		for (int i = 0; i < in.length; i++) {
			if (from.equalsIgnoreCase(in[i])) {
				return i;
			}
		}
		return -1;
	}

	public static int getIndexOfEntry(String key, int stringArrayRId) {
		Resources resources = AutomatonAlert.THIS.getResources();

		return getIndexOfEntry(
				key,
				resources.getStringArray(stringArrayRId));
	}

	public static String translateEntriesValues(String from, String[] in, String[] out) {

		for (int i = 0; i < in.length; i++) {
			if (from.equalsIgnoreCase(in[i])) {
				return out[i];
			}
		}
		return "";
	}

	public static String translateEntriesValues(String from, int in, int out) {

		Resources resources = AutomatonAlert.THIS.getResources();

		return translateEntriesValues(
				from,
				resources.getStringArray(in),
				resources.getStringArray(out));
	}

	public static int trimSourceType(Context context) throws RemoteException, SQLiteException {
		int numFloatingSourceTypes = 0;

		// SourceTypeDO (Contact-based RT
		ArrayList<SourceTypeDO> sourceTypes = SourceTypeDO.get();

		for (SourceTypeDO sourceType : sourceTypes) {
			// if: SourceType's NotificationItemId == -1
			//     || NotificationItem == null
			//     || non-PHONE and NotificationItem has no ringtone
			//     || PHONE and NotificationItem has no send to vm
			if (!Utils.isActiveSourceType(sourceType)) {
				sourceType.delete();
				++numFloatingSourceTypes;
				continue;
			}

			// user deleted phone data (rt||vm) from under us and
			// now have a hanging SourceType
			if (sourceType.getSourceType().equals(FragmentTypeRT.PHONE.name())) {
				Pair<String, String> phoneData =
						getContactPhoneRTVM(context, sourceType.getLookupKey());
				// delete SourceType if db if not custom RT and no sendToVM
				if (phoneData.first.equals("")
						&& (phoneData.second.equals("0")
							|| phoneData.second.equals(""))) {
					sourceType.delete();
					++numFloatingSourceTypes;
				}
			}
		}
		Log.d(TAG + ".trimSourceType()", "!!!!!!!!!!!!!! doTrim  !!!!!!!!!!!!!!!!!!");
		Log.d(TAG + ".trimSourceType()", "SourceTypes deleted[" + numFloatingSourceTypes + "]");

		return numFloatingSourceTypes;
	}

	private static ArrayList<String> getAllAccountIds() throws RemoteException, SQLiteException {
		ArrayList<String> accountIds = new ArrayList<String>();

		ArrayList<AccountDO> accounts = Accounts.get();
		for (AccountDO account : accounts) {
			accountIds.add(account.getAccountId() + "");
		}

		return accountIds;
	}

	private static Pair<String, String[]> buildAccountWhereClause(
			ArrayList<String>accountIds) {

		String accountWhere = "";
		String[] args = new String[accountIds.size()];
		int idx = 0;

		// where clause: account_id <> 1 AND account_id <> 2...
		for (String id : accountIds) {
			if (!(accountWhere.equals(""))) {
				accountWhere += " AND ";
			}

			// used for the where clause for both AlertItemDO and FilterItemDO
			// the two have the same column name for account id
			accountWhere +=
					AutomatonAlertProvider.ALERT_ITEM_ACCOUNT_ID
					+ " <> ?";
			args[idx++] = id+"";
		}
		return new Pair<String, String[]>(accountWhere, args);
	}

	private static Pair<Integer, Integer> trimAlertItem(
			String accountWhere, String[] args) throws RemoteException, SQLiteException {

		int numFloatingAlertItems = 0;
		int numDeletedAlertItems = 0;

		// these are all the AlertItems that have an
		// account_id that isn't one of the active account _id's
		Cursor alertItemCursor =
				AutomatonAlert.getProvider().query(
						AutomatonAlertProvider.ALERT_ITEM_TABLE_URI,
						null,
						accountWhere,
						args,
						null);

		int iAlertItemIdCol = alertItemCursor.getColumnIndex(
				AutomatonAlertProvider.ALERT_ITEM_ID);
		if (iAlertItemIdCol == -1) {
			throw new RemoteException();
		}
		// delete each of them
		if (alertItemCursor.moveToFirst()) {
			do {
				++numFloatingAlertItems;
//				dumpCursor(
//						AutomatonAlertProvider.ALERT_ITEM_TABLE,
//						alertItemCursor);
				int id = alertItemCursor.getInt(iAlertItemIdCol);
				Uri uri = ContentUris.withAppendedId(
						AutomatonAlertProvider.ALERT_ITEM_ID_URI, id);
				AutomatonAlert.getProvider().delete(uri, null, null);

			} while (alertItemCursor.moveToNext());
		}
		alertItemCursor.close();

		//
		// -- expired
		//
		// these are all the AlertItems that have an
		// account_id that isn't one of the active account _id's
		ArrayList<AlertItemDO> alertItems =
				AlertItems.get(AlertItemDO.Status.TRASH, false/*not*/);
		for (AlertItemDO alertItem : alertItems) {
			if (alertItem.isExpired()) {
				alertItem.delete();
				++numDeletedAlertItems;
			}
		}

		return new Pair<Integer, Integer>(numFloatingAlertItems, numDeletedAlertItems);

	}

	private static int trimFilterItem(String accountWhere, String[] args)
			throws RemoteException, SQLiteException {

		int numFloatingFilterItems = 0;
		int numFloatingFilterItemAccounts = 0;
		HashSet<Integer> filterItems = new HashSet<Integer>();

		// get FilterItemAccount recs that have stale account ids
		Cursor filterItemAccountCursor =
				AutomatonAlert.getProvider().query(
						AutomatonAlertProvider.FILTER_ITEM_ACCOUNT_TABLE_URI,
						null,
						accountWhere,
						args,
						null);

		// get rid of stale accounts from FilterItemAccount
		if (filterItemAccountCursor.moveToFirst()) {
			int iFilterItemIdCol = filterItemAccountCursor.getColumnIndex(
					AutomatonAlertProvider.FILTER_ITEM_ID);
			do {
				// first, delete the stale account recs
				// and save the filterItemIds for later
				int filterItemId = filterItemAccountCursor.getInt(iFilterItemIdCol);
				if (!filterItems.contains(filterItemId)) {
					filterItems.add(filterItemId);
				}
				Uri uri = ContentUris.withAppendedId(
						AutomatonAlertProvider.FILTER_ITEM_ACCOUNT_ID_URI, filterItemId);
				AutomatonAlert.getProvider().delete(uri, null, null);
				++numFloatingFilterItemAccounts;
			} while (filterItemAccountCursor.moveToNext());
		}
		filterItemAccountCursor.close();

		// now get rid of hanging FilterItems (orphaned via
		// above FilterItemAccount deletes)
		for (int filterItemId : filterItems) {
			FilterItemDO filterItem = FilterItems.get(filterItemId);
			if (filterItem != null) {
				if (filterItem.getAccounts().size() == 0) {
					filterItem.delete();
				}
			}
		}

		// now get rid of FilterItemAccounts that have no FilterItem
		List<FilterItemAccountDO> list = FilterItemAccounts.get();
		for (FilterItemAccountDO fia : list) {
			FilterItemDO item = FilterItems.get(fia.getFilterItemId());
			if (item == null) {
				fia.delete();
				++numFloatingFilterItemAccounts;
			}
		}

		return numFloatingFilterItems;
	}

	private static int trimNotificationItem() throws RemoteException, SQLiteException {
		int numFloatingNotificationItems = 0;

		// different strategy here since there may be hundreds of
		// AlertItems and dozens of FilterItems (as opposed to above
		// where there are a limited number of accounts).  Get each
		// NotificationItemDO and then try to find an AlertItemDO, a
		// SourceItemDO or a FilterItemDO for it.  If none, delete it.
		ArrayList<NotificationItemDO> notificationItems = NotificationItems.get();
		for (NotificationItemDO notificationItem : notificationItems) {
			int notificationItemId = notificationItem.getNotificationItemId();

			AlertItemDO alertItem =
					AlertItems.getNotificationItemId(notificationItemId);
			boolean haveAlertItem = alertItem != null;

			FilterItemDO filterItem =
					FilterItems.getNotificationItemId(notificationItemId);
			boolean haveFilterItem = filterItem != null;

			// It's on the Contact RT side of the house
			// see if either 1) an RT has been set
			// 					there's a linked SourceTypeDO
			SourceTypeDO sourceType = null;
			boolean haveSourceType = false;
			if (!haveFilterItem) {
				if (!TextUtils.isEmpty(notificationItem.getSoundPath())) {
					sourceType = SourceTypeDO.getNotificationItemId(notificationItemId);
				}
				haveSourceType = sourceType != null;
			}
				// if there're no connected AlertItemDO or FilterItemDO
				// or SourceTypeDO
				// delete this NotificationItemDO
			if (!haveAlertItem
					&& !haveFilterItem
					&& !haveSourceType) {
				notificationItem.delete();
				++numFloatingNotificationItems;
			}

		}
		return numFloatingNotificationItems;
	}

	private static int trimSourceAccount() throws RemoteException, SQLiteException {
		int numFloatingSourceAccounts = 0;
		// SourceAccountDO
		//
		// delete each that don't have
		// an associated account or a source type
		ArrayList<SourceAccountDO> sourceAccounts = SourceAccountDO.get();
		for (SourceAccountDO sourceAccount : sourceAccounts) {
			int sourceTypeId = sourceAccount.getSourceTypeId();
			int accountId = sourceAccount.getAccountId();
			SourceTypeDO sourceType = SourceTypeDO.get(sourceTypeId);
			boolean haveSourceType = sourceType != null;
			AccountDO account = Accounts.get(accountId);
			boolean haveAccount = account != null;
			if (!haveSourceType
					|| !haveAccount) {
				sourceAccount.delete();
				++numFloatingSourceAccounts;
			}
		}

		return numFloatingSourceAccounts;
	}

	private static int trimPostAlarm() throws RemoteException, SQLiteException {
		int numFloatingPostAlarms = 0;
		long now = System.currentTimeMillis();

		// get rid of alarms that are in the past
		ArrayList<PostAlarmDO> postAlarms = PostAlarmDO.get();
		ArrayList<PostAlarmDO> deletedAlarms =
				new ArrayList<PostAlarmDO>();
		for (PostAlarmDO postAlarm : postAlarms) {
			if (postAlarm.getNextAlarm() < now) {
				deletedAlarms.add(postAlarm);
				postAlarm.delete();
				++numFloatingPostAlarms;
			}
		}
		postAlarms.removeAll(deletedAlarms);

		// no duplicates
		//
		// go through all PostAlarm's.
		// for each, get on alertItemId, notificationItemId
		// to see if there's more than one.  If so, save
		// the latest duplicate and delete all others
		for (PostAlarmDO postAlarm : postAlarms) {
			ArrayList<PostAlarmDO> pas
					= PostAlarmDO.get(
							postAlarm.getAlertItemId(),
							postAlarm.getNotificationItemId());
			if (pas.size() > 1) {
				long latest = -1;
				int latestId = -1;
				// get the latest .mNextAlarm
				for (PostAlarmDO pa : pas) {
					if (pa.getNextAlarm() > latest) {
						latest = pa.getNextAlarm();
						latestId = postAlarm.getPostAlarmId();
					}
				}
				// delete all but latest
				for (PostAlarmDO pa : pas) {
					if (pa.getPostAlarmId() != latestId) {
						pa.delete();
					}
				}
			}
		}
		return numFloatingPostAlarms;
	}

	private static int trimContactInfo() throws RemoteException, SQLiteException {
		return AutomatonAlert.getProvider().delete(
				AutomatonAlertProvider.CONTACT_INFO_TABLE_URI,
				AutomatonAlertProvider.CONTACT_INFO_FAVORITE +
				" = 'false'",
				null);
	}

	private static ArrayList<Integer> mNoSaveAccounts = new ArrayList<Integer>();

	private static void getNoSaveAccounts() {
		// RTOnly: all accounts are no-save-to-list
		if (AutomatonAlert.RTOnly) {
			return;
		}
		// get all accounts
		List<AccountDO> accounts = Accounts.get();

		// save accounts that specify not to save AlertItems
		for (AccountDO account: accounts) {
			if (!account.isSaveToList()) {
				mNoSaveAccounts.add(account.getAccountId());
			}
		}
		// sort for quicker "contains" access
		if (accounts.size() > 0) {
			Collections.sort(mNoSaveAccounts);
		}
	}

	private static boolean isNoSaveAccount(int account) {
		return (mNoSaveAccounts.contains(account));
	}

	/* instead of just deleting expired TRASH AlertItem's, */
	/* delete them all. we only keep no-save-to-list TRASH */
	/* so that the app can do things like notifications    */
	/* on AlertItem's that have just alerted the user.     */
	private static void deleteNoSaveAlertItems() throws RemoteException, SQLiteException {
		getNoSaveAccounts();
		// if we're not saving any (RTOnly) or there
		// are accounts that specify not to save...
		if (AutomatonAlert.RTOnly
				|| mNoSaveAccounts.size() > 0) {
			// get all alertItems
			List<AlertItemDO> alerts = AlertItems.get();
			for (AlertItemDO alert : alerts) {
				// if we're not saving any or the AlertItem
				// account is not saving AlertItems...
				if (AutomatonAlert.RTOnly
						|| isNoSaveAccount(alert.getAccountId())) {
					alert.delete();
				}
			}
		}
	}

	public static void trimDb() {
		Context context = AutomatonAlert.THIS.getApplicationContext();

		try {
			Pair<String, String[]> whereAndArgs;

			trimSourceType(context);
			makeSureAllPhoneRTVMHaveSourceType(context);
			ArrayList<String> accountIds = getAllAccountIds();
			int numAccounts = accountIds.size();
			if (numAccounts > 0) {
				whereAndArgs = buildAccountWhereClause(accountIds);
				String where = whereAndArgs.first;
				String[] args = whereAndArgs.second;
				trimAlertItem(where, args);
				trimFilterItem(where, args);
			}
			//davedel -- force show AlertItem's in dev
//			if (!BuildConfig.DEBUG) {
				// get rid of alertItem's in don't-save-list accounts
			    deleteNoSaveAlertItems();
//			}
			//davedel
			trimNotificationItem();
			trimContactInfo();
			trimSourceAccount();
			trimPostAlarm();

		} catch (RemoteException | SQLiteException ignored) {}

    }

	public static long getLong(Cursor c, int idx) {
		if (c == null) {
			return -1;
		}
		try {
			return c.getLong(idx);
		}
		catch (Exception e) {
			return -1;
		}
	}

	public static int getInt(String num, int def) {
		if (num == null) {
			return def;
		}
		try {
			return Integer.parseInt(num);
		}
		catch (NumberFormatException e) {
			return def;
		}
	}

	public static long getLong(String num, long def) {
		if (num == null) {
			return def;
		}
		try {
			return Long.parseLong(num);
		}
		catch (NumberFormatException e) {
			return def;
		}
	}

    public static BigInteger getBigInteger(String num, BigInteger def) {
        if (num == null) {
            return def;
        }
        try {
            return new BigInteger(num);
        }
        catch (NumberFormatException e) {
            return def;
        }
    }

	private static Pattern mRegExPasswordField =
			Pattern.compile(".*password.*", Pattern.CASE_INSENSITIVE);

	public static void dumpCursor(String table, Cursor cursor) {
//		if (BuildConfig.DEBUG) {
//			Log.d(TAG + ".dumpCursor()", table + ": dumpCursor>>>>>>dumpCursor>>>>>>");
//			String out = "";
//			String[] names = cursor.getColumnNames();
//			for (String name : names) {
//				int idx = cursor.getColumnIndex(name);
//				out += idx+")"+name + "; ";
//			}
//			Log.d(TAG + ".dumpCursor()", "fieldNames: " + out);
//			out = "";
//			for (String name : names) {
//				if (mRegExPasswordField.matcher(name).matches()) {
//					continue;
//				}
//				int idx = cursor.getColumnIndex(name);
//				String value = "";
//				int type = cursor.getType(idx);
//				switch (type) {
//					case Cursor.FIELD_TYPE_NULL:
//						value = "<null>";
//						break;
//					case Cursor.FIELD_TYPE_INTEGER:
//						value = cursor.getInt(idx) + "";
//						break;
//					case Cursor.FIELD_TYPE_FLOAT:
//						value = cursor.getFloat(idx) + "";
//						break;
//					case Cursor.FIELD_TYPE_STRING:
//						value = cursor.getString(idx);
//						break;
//					case Cursor.FIELD_TYPE_BLOB:
//						//noinspection ImplicitArrayToString
//						value = cursor.getBlob(idx).toString();
//						break;
//				}
//				if (value.length() > 25) {
//					value = value.substring(0, 25);
//				}
//				out += name + "=" + value + "; ";
//			}
//			Log.d(TAG + ".dumpCursor()", out);
//		}
	}

	private static PackageManager mPackageManager;

	public static void enableDisableBootReceiver(Context context) {

		setPackageManager(context);
		mPackageManager.setComponentEnabledSetting(
				getBootReceiverComponentName(context),
				getEnabledDisabled(GeneralPrefsDO.isStartAtBoot()),
				PackageManager.DONT_KILL_APP);
	}

	public static void enableDisableProvider(Context context, boolean enable) {
		setPackageManager(context);
		mPackageManager.setComponentEnabledSetting(
				getProviderComponentName(context),
				getEnabledDisabled(enable),
				PackageManager.DONT_KILL_APP);
	}

	public static boolean isProviderEnabled(Context context) {
		setPackageManager(context);
		return (mPackageManager.getComponentEnabledSetting(
				getProviderComponentName(context))
						== PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
	}

	private static void setPackageManager(Context context) {
		if (mPackageManager == null) {
			mPackageManager = context.getPackageManager();
		}
	}

	public static ComponentName getBootReceiverComponentName(Context context) {
		return new ComponentName(context, BootReceiver.class);
	}

	public static ComponentName getProviderComponentName(Context context) {
		return new ComponentName(context, AutomatonAlertProvider.class);
	}

	public static int getEnabledDisabled(boolean enabled) {
		if (enabled) {
			return PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
		}
		else {
			return PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
		}

	}

//	public static void loadAdView(AdView adView) {
//
//		int a = 1;
//		if (a == 1) {
//			return;
//		}
//		if (AutomatonAlert.mDevProdVersion.equals(AdProVersion.PROD)) {
//			AdRequest adRequest = new AdRequest();
//			adRequest.addTestDevice("FCBABDC7072F17B7FA499597474E16F0");
//			adRequest.addTestDevice("FDB0AB6765E55848E44EF57AF1896007");
//			adRequest.addTestDevice("C3E9929DFB14071CBD178B185E813F44");
//			adRequest.addTestDevice("EC8622FA7D3E72EA1F9DAA7093AB523B");
//			adRequest.addTestDevice("2D9B25AB9CCD6CE31E2E3C51DCF618A2");
//			adView.loadAd(adRequest);
//		}
//		else {
//			adView.setVisibility(AdView.GONE);
//		}
//	}

	private static int getNumberOfActiveContacts(
			Activity activity, String currentContact) {
		// the non-duplicate TreeSet is key here to getting unique contacts.
		// the comparator first checks on lowercase name, if a match, then
		// compares on lookupKey. if match on both, then it doesn't add the contact.
		TreeSet<HashMap<String, String>> set = ContactInfoDO.getEmptySortedTreeSet();
		SourceTypeDO.getSourceTypeContacts(
				set, currentContact, false/*justGetOne*/, true/*ignorePhoneType*/,
				(IActivityRefresh)activity);
		return set.size();
	}

	public static OkCancel getOkCancelAppVersionRestrictions(final Activity context) {
		return new OkCancel() {
			@Override
			protected void ok(DialogInterface dialog) {
				Intent intent = new Intent(
						context.getApplicationContext(), InAppPurchasesActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
				context.getApplicationContext().startActivity(intent);
			}
			@Override
			protected void cancel(DialogInterface dialog) {
				//
			}
		};
	}

	public static boolean inAppUpgradeIsRTContactsAtLimit(
			Activity activity, String currentContact) {

		// mIntroductoryUnlimitedUpgrade = unlimited
		if (AutomatonAlert.mIntroductoryUnlimitedUpgrade
				|| AutomatonAlert.hasDevelopersCode()) {
			// don't need to check
			return false;
		}

		int max = AutomatonAlert.mDefunctUpgrade ?
				  InAppPurchases.DEFUNCT_UPGRADE_VERSION_NUM_ACTIVE_CONTACTS_ALLOWED
				: InAppPurchases.FREE_VERSION_NUM_ACTIVE_CONTACTS_ALLOWED;

		int num = getNumberOfActiveContacts(activity, currentContact);
		if (num >= max) {
			OkCancel okCancel = getOkCancelAppVersionRestrictions(activity);
			OkCancelDialog okCancelDialog = OkCancelDialog.showInstance(
					(AppCompatActivity) activity,
					"This free version gives you up to <b>"
							+ max
							+ " contacts</b>. "
							+ getAppVersionFeatureText(),
					null,
					AutomatonAlert.OK_LABEL,
					"Purchase",
					OkCancelDialog.CancelButton.LEFT,
					OkCancelDialog.EWI.INFO
			);
			okCancelDialog.setOkCancel(okCancel);
			return true;
		}

		return false;
	}

	public static boolean inAppUpgradeIsFilterItemsAtLimit(Activity context) {
		// mIntroductoryUnlimitedUpgrade = unlimited
		if (AutomatonAlert.mIntroductoryUnlimitedUpgrade
				|| AutomatonAlert.hasDevelopersCode()) {
			// don't need to check
			return false;
		}

		int max = AutomatonAlert.mDefunctUpgrade ?
					  InAppPurchases.DEFUNCT_UPGRADE_VERSION_NUM_FILTER_ITEMS_ALLOWED
					: InAppPurchases.FREE_VERSION_NUM_FILTER_ITEMS_ALLOWED;

		int num = FilterItems.get().size();
		if (num >= max) {
			OkCancel okCancel = getOkCancelAppVersionRestrictions(context);
			OkCancelDialog okCancelDialog = OkCancelDialog.showInstance(
					(AppCompatActivity) context,
					"This free version gives you up to <b>"
							+ max
							+ " free-form alerts</b>. "
							+ getAppVersionFeatureText(),
					null,
					AutomatonAlert.OK_LABEL,
					"Purchase",
					OkCancelDialog.CancelButton.LEFT,
					OkCancelDialog.EWI.INFO
			);
			okCancelDialog.setOkCancel(okCancel);

			return true;
		}

		return false;
	}

	public static boolean inAppUpgradeCheckAccountsAtLimit(Activity context) {
		// mIntroductoryUnlimitedUpgrade = unlimited
		if (AutomatonAlert.mIntroductoryUnlimitedUpgrade
				|| AutomatonAlert.hasDevelopersCode()) {
			// don't need to check
			return false;
		}

		int max = AutomatonAlert.mDefunctUpgrade ?
					  InAppPurchases.DEFUNCT_UPGRADE_VERSION_NUM_ACCOUNTS_ALLOWED
					: InAppPurchases.FREE_VERSION_NUM_ACCOUNTS_ALLOWED;

		if (Accounts.getByAccountType(AccountEmailDO.ACCOUNT_EMAIL).size() >= max) {
			OkCancel okCancel = getOkCancelAppVersionRestrictions(context);
			OkCancelDialog okCancelDialog = OkCancelDialog.showInstance(
					(AppCompatActivity) context,
					"This free version gives you "
							+ (max == 1 ? "" : "up to ") + "<b>"
							+ max
							+ (max == 1 ? " account" : " accounts") + "</b>. "
							+ getAppVersionFeatureText(),
					null,
					AutomatonAlert.OK_LABEL,
					"Purchase",
					OkCancelDialog.CancelButton.LEFT,
					OkCancelDialog.EWI.INFO
			);
			okCancelDialog.setOkCancel(okCancel);

			return true;
		}

		return false;
	}

	public static boolean inAppUpgradeNoBackupRestore(Activity context) {
		// mIntroductoryUnlimitedUpgrade = unlimited
		if (AutomatonAlert.mIntroductoryUnlimitedUpgrade
				|| AutomatonAlert.hasDevelopersCode()) {
			// don't need to check
			return false;
		}

		OkCancel okCancel = getOkCancelAppVersionRestrictions(context);
		OkCancelDialog okCancelDialog = OkCancelDialog.showInstance(
				(AppCompatActivity) context,
				"This free version doesn't come with Backup/Restore. "
						+ getAppVersionFeatureText(),
				null,
				AutomatonAlert.OK_LABEL,
				"Purchase",
				OkCancelDialog.CancelButton.LEFT,
				OkCancelDialog.EWI.INFO
		);
		okCancelDialog.setOkCancel(okCancel);

		return true;
	}

	public static boolean isItTimeToAskUserToRateApp(boolean resetClock) {
		boolean needToAsk = false;
		long lastAsked = 0;  // essentially defaults to ask=true
		long now = System.currentTimeMillis();

		NameValueDataDO nv = NameValueDataDO.get(NameValueDataDO.LAST_ASKED_TO_RATE_APP, null);
		if (nv != null) {
			lastAsked = Utils.getLong(nv.getValue(), 0);
		}
		else {
			// give new users an interval to evaluate
			lastAsked = now;
			nv = new NameValueDataDO(NameValueDataDO.LAST_ASKED_TO_RATE_APP, now + "");
		}

		// NameValueDataDO.dontShowUserToRateAppEverAgain() uses:
		// 		String value = (Long.MAX_VALUE - ASK_AGAIN_TO_RATE_APP_INTERVAL - 1000) + "";
		long nextAsk = lastAsked + NameValueDataDO.ASK_AGAIN_TO_RATE_APP_INTERVAL;
		if (now >= nextAsk) {
			needToAsk = true;
		}

		// reset lastAsked to now
		if (needToAsk) {
			if (resetClock) {
				nv.setValue(now + "");
				nv.save();
			}
		}

		return needToAsk;
	}

	private static String getAppVersionFeatureText() {
		return "Purchase these extended features:<br><br>"
				+ "<b>Unlimited Everything + Backup/Restore</b>."
				;
	}

	private static OkCancel getOkCancelOkOrPlayStore(final Activity context) {
		return new OkCancel() {
			@Override
			protected  void ok(DialogInterface dialog) {
				Intent intent = new Intent(
						Intent.ACTION_VIEW,
						Uri.parse("https://play.google.com/store/search?q=aldersonet"));
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.getApplicationContext().startActivity(intent);
			}
			@Override
			protected  void cancel(DialogInterface dialog) {
				//
			}
		};
	}

	public static boolean showOkCancelAbout(Activity context) {
		OkCancel okCancel = getOkCancelOkOrPlayStore(context);
		OkCancelDialog okCancelDialog = OkCancelDialog.showInstance(
				(AppCompatActivity) context,
				"Version "
						+ AutomatonAlert.VERSION
						+ "<br>©Copyright AldersoNet, LLC "
						+ "2012, 2014, 2015, 2016. All rights reserved.",
				null,
				AutomatonAlert.OK_LABEL,
				"Play Store",
				OkCancelDialog.CancelButton.LEFT,
				OkCancelDialog.EWI.INFO
		);
		okCancelDialog.setOkCancel(okCancel);
		return true;
	}

	public static boolean showOkCancelLicenses(Activity context) {
		Resources res = context.getResources();

		OkCancel okCancel = getOkCancelOkOrPlayStore(context);
		OkCancelDialog okCancelDialog = OkCancelDialog.showInstance(
				(AppCompatActivity) context,
				"<b>Licenses</b><br><b><i>"
						+ res.getString(R.string.licenses_apache_2_0_files)
						+ "</i></b><br><br>\""
						+ res.getString(R.string.licenses_apache_2_0)
						+ "\"<br><br>"
						+ "<b>Copyrights</b><br><b><i>"
						+ res.getString(R.string.licenses_android_open_source_files)
						+ "</i></b><br><br>\""
						+ res.getString(R.string.licenses_android_open_source)
						+ "\"<br><br>"
						+ "<b><i>Email icon (changes made):</b></i><br>"
						+ "<b>Creative Commons 4.0:</b><br>"
						+ "http://creativecommons.org/licenses/by-sa/4.0/<br>"
						+ "<b>Artist Address</b><br>"
						+ "https://github.com/alecive<br><br>"
						+ "Open Source code may be found at "
						+ "http://www.aldersonet.com/",
				null,
				AutomatonAlert.OK_LABEL,
				"Play Store",
				OkCancelDialog.CancelButton.LEFT,
				OkCancelDialog.EWI.INFO
		);
		okCancelDialog.setOkCancel(okCancel);

		return true;
	}

	public static void deleteRegularRT(String lookupKey, FragmentTypeRT type) {

		SourceTypeDO sourceType = SourceTypeDO.get(lookupKey, type.name());
		if (sourceType != null) {
			if ((sourceType.getNotificationItemId()) != -1) {
				NotificationItemDO notificationItem =
						NotificationItems.get(sourceType.getNotificationItemId());
				notificationItem.delete();
			}
			// delete any related SourceAccounts
			ArrayList<SourceAccountDO> sourceAccounts =
					SourceAccountDO.getSourceTypeId(sourceType.getSourceTypeId());
			for (SourceAccountDO sourceAccount : sourceAccounts) {
				sourceAccount.delete();
			}
			sourceType.delete();
		}
	}

	public static void deleteFilterItemRT(
			NotificationItemDO notificationItem, FilterItemDO filterItem) {

		if (notificationItem != null
				&& notificationItem.getNotificationItemId() != -1) {
			notificationItem.delete();
		}

		if (filterItem != null
				&& filterItem.getFilterItemId() != -1) {
			filterItem.delete();
		}
	}

	public static final String SILENT_MP3 = "silent.mp3";
	private static Pair<String, String> getSilentMp3Path() {
		String path = OurDir.getOurSdPath();
		String file = SILENT_MP3;

		return new Pair<>(path, file);
	}

	private static String checkForSilentMp3() {
		Pair<String, String> pair = getSilentMp3Path();
		String path = pair.first;
		String file = pair.second;

	    File tempdir = new File(path + file);
	    if (!tempdir.exists()) {
			copyFileFromRaw(AutomatonAlert.THIS, file, path, R.raw.silent);
	    }

	    return path + file;
	}

	private static final String CONTACTS_SELECTION =
			Contacts.CUSTOM_RINGTONE + " IS NOT NULL OR " +
			Contacts.SEND_TO_VOICEMAIL + " <> '0'";

	private static final String[] CONTACTS_PROJECTION = new String[] {
			Contacts.LOOKUP_KEY,
			Contacts.DISPLAY_NAME,
			Contacts.CUSTOM_RINGTONE,
			Contacts.SEND_TO_VOICEMAIL
	};

	public static void makeSureAllPhoneRTVMHaveSourceType(Context context) {
		int numSourceTypesCreated = 0;
        Cursor contactCursor = null;
        contactCursor = AutomatonAlert.THIS.getContentResolver().query(
                Contacts.CONTENT_URI,
                CONTACTS_PROJECTION,
                CONTACTS_SELECTION,
                null,
                null);

        try {

			// delete each of them
			if (contactCursor != null && contactCursor.moveToFirst()) {
				int keyCol = contactCursor.getColumnIndex(Contacts.LOOKUP_KEY);
				int nameCol = contactCursor.getColumnIndex(Contacts.DISPLAY_NAME);
				int rtCol = contactCursor.getColumnIndex(Contacts.CUSTOM_RINGTONE);
				int vmCol = contactCursor.getColumnIndex(Contacts.SEND_TO_VOICEMAIL);
				do {
					String key = contactCursor.getString(keyCol);
					String rt = contactCursor.getString(rtCol);
					String vm = contactCursor.getString(vmCol);
//					if (BuildConfig.DEBUG) {
//						String name = contactCursor.getString(nameCol);
//					}
					// rt == "": need to clear out rt/vm and leave
					if (TextUtils.isEmpty(rt)
							&& vm.equals("0")) {
						updatePhoneRTVM(context, key, "", "0");
						continue;
					}
					SourceTypeDO source = SourceTypeDO.get(key, FragmentTypeRT.PHONE.name());
					// if we don't have a SourceType, we need to make one
					if (source == null) {
						// create SourceType with LookupKey and PHONE
						source = new SourceTypeDO(key, FragmentTypeRT.PHONE.name());
						// create NotificationItem and populate ringtone path then save
						NotificationItemDO notificationItem = new NotificationItemDO();
						if (rt != null) {
							notificationItem.setSoundPath(rt);
						}
						notificationItem.save();
						// link NotificationItem with SourceType and save
						source.setNotificationItemId(notificationItem.getNotificationItemId());
						source.save();
						++numSourceTypesCreated;
					}

				} while (contactCursor.moveToNext());
			}
		} catch (Exception ignored) {}
		finally {
			if (contactCursor != null) {
				contactCursor.close();
			}
		}
		Log.d(TAG + ".makeSure()", "!!!!!!!!!!!!!! RTVMHaveSourceType  !!!!!!!!!!!!!!!!!!");
		Log.d(TAG + ".makeSure()", "SourceTypes created[" + numSourceTypesCreated + "]");
	}

	public static boolean updatePhoneRTVM(
			Context context, String lookupId, String ringtone, String sendToVM) {

		boolean updated = false;

		if (ringtone != null) {
			if (ringtone.equalsIgnoreCase(AutomatonAlert.SILENT)) {
				ringtone = checkForSilentMp3();
			}
		}

        Cursor cursor = null;
        cursor = AutomatonAlert.THIS.getContentResolver().query(
                Uri.withAppendedPath(Contacts.CONTENT_LOOKUP_URI, lookupId),
               new String[] { 	Contacts._ID },
               null,
               null,
               null);

        try {
			if (cursor != null && cursor.moveToFirst()) {
				int iId = cursor.getColumnIndex(Contacts._ID);
				int _id = cursor.getInt(iId);
				Uri updateUri = Uri.withAppendedPath(Contacts.CONTENT_URI, ""+_id);
				ContentValues cv = new ContentValues();
				// null ? don't set
				// "" ? unset (null)
				if (ringtone != null) {
					String newRT = ringtone.equals("") ? null : ringtone;
					cv.put(Contacts.CUSTOM_RINGTONE, newRT);
				}
				// null ? don't set
				if (sendToVM != null) {
					cv.put(Contacts.SEND_TO_VOICEMAIL, sendToVM);
				}
				int count = AutomatonAlert.THIS.getContentResolver().update(
                        updateUri, cv, null, null);
				if (count > 0) {
					updated = true;
				}
			}
		} catch (Exception ignored) {
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return updated;
	}

	private static void copyFileFromRaw(Context context, String file, String dest, int res) {
	    InputStream in = null;
	    FileOutputStream fout = null;

	    // create directory if it doesn't exist
	    File tempdir = new File(dest);
	    if (!tempdir.exists()) {
	        tempdir.mkdirs();
	    }

	    // write the file
	    in = null;
	    fout = null;
	    try {
		    in = context.getResources().openRawResource(res);
			byte[] buffer = new byte[in.available()];
			in.read(buffer);
			in.close();
			String filename = dest + file;
			fout = new FileOutputStream(filename);
			fout.write(buffer);
			fout.close();
	    }
	    catch (Exception ignored) {}
	    finally {
	    	if (in != null) {
	    		try {
	    			in.close();
	    		} catch (IOException ignored) {}
	    	}
	    	if (fout != null) {
	    		try {
	    			fout.close();
	    		} catch (IOException ignored) {}
	    	}
	    }
	}

	public static boolean verifySourceTypeData(
			Context context, String lookupKey, String type, ContactListInfo contactListInfo) {

		SourceTypeDO sourceType =
				SourceTypeDO.createRecFromContactsDb(
						context, lookupKey, type, contactListInfo);

		return sourceType != null && isActiveSourceType(sourceType);
	}

	public static boolean isActiveSourceType(SourceTypeDO sourceType) {
		if (sourceType != null) {
			int nid = sourceType.getNotificationItemId();
			if (nid >= 0) {
//				NotificationItemDO notificationItem = NotificationItems.get(nid);
//				if (notificationItem != null) {
//					boolean isPhone =
//							sourceType.getSourceType().equals(FragmentTypeRT.PHONE.name());
//					String sound = notificationItem.getSoundPath();
//					String silent = notificationItem.getSilentMode();
//					// has ringtone or PHONE & send-to-vm active
//					if (!TextUtils.isEmpty(sound)
//							|| isPhone && !silent.equals("0")) {
						return true;
//					}
//				}
			}
		}
		return false;
	}

	public static Pair<String, String> getContactPhoneRTVM(
			Context context, String lookupKey) {

        Cursor cursor = null;
        cursor = AutomatonAlert.THIS.getContentResolver().query(
                Uri.withAppendedPath(Contacts.CONTENT_LOOKUP_URI, lookupKey),
               new String[] {
                        Contacts.CUSTOM_RINGTONE,
                        Contacts.SEND_TO_VOICEMAIL
               },
               null,
               null,
               null);

        try {
			if (cursor != null && cursor.moveToFirst()) {
				int iCustom = cursor.getColumnIndex(Contacts.CUSTOM_RINGTONE);
				int iToVM = cursor.getColumnIndex(Contacts.SEND_TO_VOICEMAIL);
				String customRingtone = cursor.getString(iCustom);
				String sendToVM = cursor.getString(iToVM);
				if (customRingtone == null) {
					customRingtone = "";
				}
				if (sendToVM == null) {
					sendToVM = "0";
				}
				return new Pair<>(customRingtone, sendToVM);
			}
		}
		catch (Exception ignored) {}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}

		return new Pair<>("", "0");
	}

	public static ArrayList<String> getPhoneNumbers(Context context, String id) {

        Cursor cursor = null;
        cursor = AutomatonAlert.THIS.getContentResolver().query(
               Data.CONTENT_URI,
               new String[] {
                       Phone.NUMBER,
                       Phone.TYPE,
                       Phone.LABEL,
                       Phone.CUSTOM_RINGTONE,
                       Phone.SEND_TO_VOICEMAIL
               },
               Phone.LOOKUP_KEY + " = ? AND " + Phone.MIMETYPE + " = ?",
               new String[] {
                       id,
                       Phone.CONTENT_ITEM_TYPE
               },
               null);

        ArrayList<String> phones = new ArrayList<>();
		try {
			if (cursor != null && cursor.moveToFirst()) {
				int i = 0;
				int iNumberCol = cursor.getColumnIndex(Phone.NUMBER);
				int iTypeCol = cursor.getColumnIndex(Phone.TYPE);
				int iLabelCol = cursor.getColumnIndex(Phone.LABEL);
				do {
					if (++i > 20) {
						break;
					}
					String number = cursor.getString(iNumberCol);
					int type = cursor.getInt(iTypeCol);
					String label = cursor.getString(iLabelCol);

					if (number != null &&
							!number.equals("")) {
						String sType = " (Other)";
						sType = translatePhoneType(type, label);
						sType = " (" + sType + ")";
						phones.add(number + sType);
					}
				} while (cursor.moveToNext());
			}
		}
		catch (Exception ignored) {}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}


		return phones;
	}

	public static ArrayList<String> getEmailAddresses(Context context, String id, int howMany) {

        Cursor cursor = null;
        cursor = AutomatonAlert.THIS.getContentResolver().query(
               Data.CONTENT_URI,
               new String[] { Email.ADDRESS, Email.TYPE, Email.LABEL },
               Email.LOOKUP_KEY + " = ? AND " + Email.MIMETYPE + " = ?",
               new String[] { id, Email.CONTENT_ITEM_TYPE },
               null);

        ArrayList<String> emails = new ArrayList<String>();
		try {
			if (cursor != null && cursor.moveToFirst()) {
				int i = 0;
				int iNumberCol = cursor.getColumnIndex(Email.ADDRESS);
				int iTypeCol = cursor.getColumnIndex(Email.TYPE);
				int iLabelCol = cursor.getColumnIndex(Email.LABEL);
				do {
					if (++i > howMany) {
						break;
					}
					String address = cursor.getString(iNumberCol);
					int type = cursor.getInt(iTypeCol);
					String label = cursor.getString(iLabelCol);

					if (address != null &&
							!address.equals("")) {
						String sType = " (Other)";
						sType = translateEmailType(type, label);
						sType = " (" + sType + ")";
						emails.add(address + sType);
					}
				} while (cursor.moveToNext());
			}
		}
		catch (Exception ignored) {}
		finally {
			if (cursor != null) {
				cursor.close();
			}
		}


		return emails;
	}

	private static boolean setDataSource(
			Context context, String uri, MediaMetadataRetriever retriever) {

        if (retriever == null) {
            return false;
        }

		try {
			retriever.setDataSource(context, Uri.parse(uri));
			return true;
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	private static long getSongDurationAlternate(String song) {
		long duration = 0;
		MediaPlayer mp = new MediaPlayer();

		try {
			mp.setDataSource(song);
			mp.prepare();
			duration = mp.getDuration();
		} catch (IllegalArgumentException e) {
			//
		} catch (SecurityException e) {
			//
		} catch (IllegalStateException e) {
			//
		} catch (IOException e) {
			//
		} finally {
			if (mp != null) {
				mp.reset();
				if (mp != null) {
					mp.release();
					mp = null;
				}
			}
		}

		return duration;
	}

	public static long getSongDuration(Context context, String name, Uri uri) {
		if (uri == null) {
			return 0;
		}

		String song = "";

		if (!uri.toString().toLowerCase().startsWith(AutomatonAlert.CONTENT_PREFIX)) {
			String[] uriAndTitle = getDefaultRingtoneUriAndTitle(context, name, false, false);
			song = uriAndTitle[0];
		}
		else {
			song = uri.toString();
		}

		long duration = 0;

		if (!TextUtils.isEmpty(song)) {
			MediaMetadataRetriever retriever = new MediaMetadataRetriever();
			if (!setDataSource(context, song, retriever)) {
				// problem with uri, do alternate method with MediaPlayer
				duration = getSongDurationAlternate(song);
			}
			else {
				// use metadata extraction to get duration
				String sDuration =
						retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
				duration = getLong(sDuration, 0);
			}
		}

		return duration;
	}

	public static String getSongName(Uri path) {
		if (path == null
				|| TextUtils.isEmpty(path.toString())) {
			return "";
		}

		// translate our own silent.mp3 to the songname "Silent"
		Pair<String, String> pair = getSilentMp3Path();
		if (path.toString().equals(pair.first + pair.second)) {
			return AutomatonAlert.SILENT_LABEL;
		}

		// return path if it doesn't start with "content://"
		String sPath = path.toString().toLowerCase();
		if (!(sPath.startsWith(AutomatonAlert.CONTENT_PREFIX))) {
			return path.toString();
		}

		String songName = "";
		Ringtone ringtone =
				RingtoneManager.getRingtone(AutomatonAlert.THIS, path);
		if (ringtone != null) {
			songName = ringtone.getTitle(AutomatonAlert.THIS);
		}
		return songName;
	}

	public static String[] getDefaultRingtoneUriAndTitle(
			Context context, String name,
			boolean trimTitleOfDefaultPhrase, boolean justTheSongName) {

		// figure out which type: ALARM/NOTIFICATION/RINGTONE
		int type = RingtoneManager.TYPE_ALARM;
		if (name.equalsIgnoreCase(ringtone.name())) {
			type = RingtoneManager.TYPE_RINGTONE;
		}
		else if (name.equalsIgnoreCase(VolumeTypes.notification.name())) {
			type = RingtoneManager.TYPE_NOTIFICATION;
		}

		// get uri and title
		Uri uri = RingtoneManager.getDefaultUri(type);
		Ringtone ringtone =
				RingtoneManager.getRingtone(context, uri);
		String title = "";
		if (ringtone != null) {
			title = ringtone.getTitle(context);
		}

		// get rid of extra "Default ringtone" in title if it's there
		if (trimTitleOfDefaultPhrase) {
			String str = "Default ringtone";
			int i = title.indexOf(str);
			if (i == 0) {
				title = title.substring(str.length());
			}
		}

		String[] ret = null;

		if (justTheSongName) {
			ret = new String[]
					{ uri.toString(), title.trim() };
		}
		else {
			// capitalize
			if (!TextUtils.isEmpty(name)) {
				name = initCap(name);
			}
			// return uri, title
			ret = new String[]
					{ uri.toString(), name.concat(" ").concat(title.trim()) };
		}

		return ret;
	}

	public static String extractEmailFromAddress(String in) {
		if (TextUtils.isEmpty(in)) {
			return in;
		}
		int i1 = in.indexOf('<');
		int i2 = in.lastIndexOf('>');
		if (i1 >= 0 &&
				i2 > 0 &&
				i1 < i2) {
			return (in.substring(i1+1, (i2)));
		}
		return in;
	}

	public static String truncateEmailAddress(String from) {
		if (from == null) {
			return "";
		}
		int idx = from.indexOf('<');
		if (idx >= 0) {
			return from.substring(0, idx).trim();
		}
		return from;
	}


	public static final String emailRegEx = "(?ims:<[^ ]+@[^ ]+\\.[^ ]+>)";
	public static final Pattern emailPattern = Pattern.compile(emailRegEx);

	public static String stripAllEmailAddresses(String from) {
		String out = emailPattern.matcher(from).replaceAll("");
		return out.trim();
	}

	public static void setActionBarTitle(ActionBar ab, String title) {
		if (ab == null) return;

		// fer-shure way to set title color
		if (title == null) title = "<unknown>";
		Spannable newTitle = new SpannableString(title);
		newTitle.setSpan(
				new ForegroundColorSpan(Color.WHITE),
				0,
				newTitle.length(),
				Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
		ab.setTitle(newTitle);
	}

	public static void setActionBarCommon(Resources res, ActionBar ab, String title) {
		if (ab == null) return;

		ab.setHomeAsUpIndicator(R.drawable.ic_drawer_am_dark);
		ab.setDisplayHomeAsUpEnabled(true);
		ab.setHomeButtonEnabled(true);
		ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		ab.setDisplayShowTitleEnabled(true);
		ab.setIcon(R.drawable.app_icon_blue_no_border_64);
		ab.setBackgroundDrawable(
				res.getDrawable(R.drawable.swatch_app_blue));
		setActionBarTitle(ab, title);
	}

	public static String returnPluralOrOriginal(
			Object[] array, String inString, String pluralString) {

		if (array == null) {
			return inString;
		}
		if (array.length == 0 || array.length > 1) {
			if (pluralString.equals("ies")) {
				// ends in y
				return (inString.substring(0, inString.length()-1) + pluralString);
			}
			else {
				return (inString + pluralString);
			}
		}
		return inString;
	}

	public static String returnPluralOrOriginal(
			int count, String inString, String pluralString) {
		if (count == 0 || count > 1) {
			if (pluralString.equals("ies")) {
				// ends in y
				return (inString.substring(0, inString.length()-1) + pluralString);
			}
			else {
				return (inString + pluralString);
			}
		}
		return inString;
	}

	private static boolean isDay(long inDate, long from) {
		Calendar calFrom = Calendar.getInstance();
		calFrom.setTimeInMillis(from);
        int year = calFrom.get(Calendar.YEAR);
        int month = calFrom.get(Calendar.MONTH);
        int day = calFrom.get(Calendar.DAY_OF_MONTH);

		Calendar dStart = Calendar.getInstance();
		dStart.set(year, month, day, 0, 0);

		Calendar dEnd = Calendar.getInstance();
		dEnd.set(year, month, day, 23, 59);

		long lStart = dStart.getTimeInMillis();
		long lEnd = dEnd.getTimeInMillis();

		return inDate >= lStart && inDate <= lEnd;
	}

	public static boolean isToday(long inDate) {
		return isDay(inDate, System.currentTimeMillis());
	}

	public static boolean isTomorrow(long inDate) {
		return isDay(inDate, System.currentTimeMillis() + 24 * 60 * 60 * 1000);
	}

	public static boolean isYesterday(long inDate) {
		return isDay(inDate, System.currentTimeMillis() - 24 * 60 * 60 * 1000);
	}

	public static boolean isThisYear(long inDate) {
		Calendar calNow = Calendar.getInstance();

		Calendar dStart = Calendar.getInstance();
		dStart.set(calNow.get(Calendar.YEAR), Calendar.JANUARY, 0, 0, 0);

		Calendar dEnd = Calendar.getInstance();
		dEnd.set(calNow.get(Calendar.YEAR), Calendar.DECEMBER, 31, 23, 59);

		long lStart = dStart.getTimeInMillis();
		long lEnd = dEnd.getTimeInMillis();

		return inDate >= lStart && inDate <= lEnd;
	}

	public static DateFormat getTodayFormat() {
		return SimpleDateFormat.getTimeInstance(
				SimpleDateFormat.SHORT, Locale.getDefault());
	}

	public static DateFormat getThisYearFormat(boolean showDayOfWeek) {
		String format = "";
		if (Locale.getDefault().equals(Locale.US)) {
			format = "MMM d";
		}
		else {
			format = "d MMM";
		}
		if (showDayOfWeek) {
			format = "EEE " + format;
		}
		return new SimpleDateFormat(format, Locale.getDefault());
	}

	public static DateFormat getOutYearFormat(boolean showDayOfWeek) {
		String format = "";
		if (Locale.getDefault().equals(Locale.US)) {
			format = "MMM d yyyy";
		}
		else {
			format = "d MMM yyyy";
		}
		if (showDayOfWeek) {
			format = "EEE " + format;
		}
		return new SimpleDateFormat(format, Locale.getDefault());
	}

	public static String smallDate(long inDate) {
		DateFormat sdf = null;

		if (inDate == -1) {
			return "";
		}

		// TODAY
		if (isToday(inDate)) {
			sdf = getTodayFormat();
		}

		// THIS YEAR
		else if (isThisYear(inDate)) {
			sdf = getThisYearFormat(false/*showDayOfWeek*/);
		}

		// LAST YEAR OR BEFORE
		else {
			sdf = getOutYearFormat(false/*showDayOfWeek*/);
		}

		return sdf.format(inDate);
	}

	private static long parseDate(String inDate, String format) {
		long oDate = -1;
		SimpleDateFormat sdf = null;
		Date dDate = null;

		try {
			sdf = new SimpleDateFormat(format, Locale.getDefault());
			dDate = sdf.parse(inDate);
			return dDate.getTime();

		} catch (ParseException ignored) {}

		return oDate;
	}

	private static Pattern mRegExMarkupBeginningEnd =
			Pattern.compile("^(<[^>]+>)(.*)(<[^>]+>)$");


	public static String stripMarkupFromBeginningAndEnd(String in) {
		return mRegExMarkupBeginningEnd.matcher(in).replaceAll("$2");
	}

	public static long stringDateToLong(String inDate) {
		long date = Utils.getLong(inDate, -1);
		// if it's not a string, return it
		if (date != -1) return date;

		String sIn = stripMarkupFromBeginningAndEnd(inDate);
		long oDate = parseDate(sIn, "E MMM dd kk:mm:ss zzz yyyy");
		if (oDate == -1) {
			oDate = parseDate(sIn, "E, d MMM yyyy kk:mm:ss ZZZ");
		}
		return oDate;
	}

	public static Bitmap getPhotoBitmap(Context context, String lookupKey) {
		Bitmap photo = null;
		if (context == null) {
			context = AutomatonAlert.THIS.getApplicationContext();
		}
		Uri uri = Uri.parse(Contacts.CONTENT_LOOKUP_URI + "/" + lookupKey);

		InputStream input =
				Contacts.openContactPhotoInputStream(
						AutomatonAlert.THIS.getContentResolver(), uri, false);
		if (input == null) {
			photo = null;
		}
		else {
			photo = BitmapFactory.decodeStream(input);
			try {
				input.close();
			} catch (Exception ignored) {}
		}
		return photo;
	}

	public static Date getDateRemindOrNow(AlertItemDO alertItem) {
		if (alertItem == null
				|| alertItem.getDateRemind() == null) {
			return new Date(System.currentTimeMillis());
		}
		return alertItem.getDateRemind();
	}

	public static class IntentReqTypeRec {
		public Intent mIntent;
		int mReqCode;
		String mMsgType;
		public IntentReqTypeRec() {
			mIntent = new Intent();
		}
	}

	public static IntentReqTypeRec setSourceIntents(FragmentTypeRT type) {
		IntentReqTypeRec rec = new IntentReqTypeRec();

		//TEXT
		if (type.equals(FragmentTypeRT.TEXT)) {
			rec.mMsgType = "SMS/MMS";
			rec.mReqCode = AutomatonAlert.NOTIFICATION_BAR_TEXT_ALERT;
			rec.mIntent = new Intent(Intent.ACTION_MAIN);
			rec.mIntent.setType("vnd.android-dir/mms-sms");
			rec.mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		}
		// PHONE
		else if (type.equals(FragmentTypeRT.PHONE)) {
			rec.mMsgType = "phone";
			rec.mReqCode = AutomatonAlert.NOTIFICATION_BAR_PHONE_ALERT;
			rec.mIntent.setAction(Intent.ACTION_VIEW);
			rec.mIntent.setData(Uri.parse("tel:"));
			rec.mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		}
		// EMAIL
		else if (type.equals(FragmentTypeRT.EMAIL)) {
			rec.mMsgType = "email";
			rec.mReqCode = AutomatonAlert.NOTIFICATION_BAR_EMAIL_ALERT;
			rec.mIntent = new Intent(Intent.ACTION_MAIN);
			rec.mIntent.addCategory(Intent.CATEGORY_APP_EMAIL);
			rec.mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		}
		else {
			return null;
		}

		return rec;
	}

	public static String tripletOfEqualSignPlusHexByteToDecString(String inString) {
		String in = inString;
		String out = "";


		int start = 0;
		while((start=in.indexOf('=')) >= 0) {
			if (Hex.isHexByte(in.substring(start+1, start+3))) {
				char c = (char)Hex.hexToDec(in.substring(start+1, start+3));
				out += in.substring(0, start) +  c;
				in = in.substring(start+3);
			}
			else {
				out += in.substring(0, start);
				in = in.substring(++start);
			}
		}
		if (in.length() > 0) {
			out += in;
		}

		return out;
	}

	public static long getDateRemindLong(Date dateRemind) {
		if (dateRemind == null) {
			return 0;
		}
		return dateRemind.getTime();
	}

	public static String initCap(String in) {
		if (TextUtils.isEmpty(in)) {
			return "";
		}
		if (in.length() == 1) {
			return in.toUpperCase(Locale.getDefault());
		}
		return in.substring(0,1).toUpperCase(Locale.getDefault())
				+ in.substring(1).toLowerCase(Locale.getDefault());

	}

	public static void okCancelDialogNative(
			final Activity activity, final String title,
			final String message, final OkCancel action) {

		okCancelDialogNative(activity, title, message, action, null);
	}

	public static void okCancelDialogNative(
			final Activity activity, final String message, final OkCancel action) {

		okCancelDialogNative(activity, null, message, action, null);
	}

	public static void okCancelDialogNative(
			final Activity activity, final String title,
			final String message, final OkCancel action, View customView) {

		AlertDialog.Builder builder =
				new AlertDialog.Builder(activity);
		builder.setMessage(Html.fromHtml(message));
		builder.setPositiveButton(
				AutomatonAlert.OK_LABEL, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				action.doRightButtonPressed(dialog);
				dialog.dismiss();
			}
		});
		builder.setNegativeButton(
				AutomatonAlert.CANCEL_LABEL, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				action.doLeftButtonPressed(dialog);
				dialog.dismiss();
			}
		});

		if (customView != null) {
			builder.setView(customView);
		}

		if (!TextUtils.isEmpty(title)) {
			builder.setTitle(title);
		}

		AlertDialog dialog = builder.create();
		dialog.show();
	}

	public static String toLocaleString(long inDate) {
		Calendar cal = Calendar.getInstance(Locale.getDefault());
		cal.setTimeInMillis(inDate);
		String format = "";

		format = (Locale.getDefault().equals(Locale.US)) ?
				"MMM d, yyyy h:mm:ss a" : "d MMM yyyy k:mm:ss";

		return (String)android.text.format.DateFormat.format(format, cal);
	}

	public static Pair<Integer, Integer> getScrollPosition(ListView listView) {
		if (listView == null) return new Pair<Integer, Integer>(0, 0);

		int pos = listView.getFirstVisiblePosition();

		View v = listView.getChildAt(0);
		int top = (v != null) ? v.getTop() : 0;

		return new Pair<Integer, Integer>(pos, top);
	}

    public static void writeToDebugLog(final String out) {
        if (BuildConfig.DEBUG) {
            class Runner implements Runnable {
                @Override
                public void run() {
                    Calendar cal = Calendar.getInstance(Locale.getDefault());
                    String loggerPath =
                            OurDir.getOurSdPath()
                                    + "debuglog"
                                    + android.text.format.DateFormat.format("yyMMdd.txt", cal);

                    try {
                        File sd = Environment.getExternalStorageDirectory();
                        if (sd.canWrite()) {
                            File logger = new File(loggerPath);
                            BufferedWriter bw = new BufferedWriter(new FileWriter(logger, true));
                            bw.write(
                                    android.text.format.DateFormat.format("yyMMdd:kkmmss  ", cal) +
                                            out);
                            bw.newLine();
                            bw.close();
                        }
                    } catch (Exception ignore) {
                    }
                }
            }

            new Thread(new Runner()).start();
        }
    }
}

