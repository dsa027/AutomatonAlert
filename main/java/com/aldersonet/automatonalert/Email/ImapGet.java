package com.aldersonet.automatonalert.Email;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.ContactsContract.Contacts;
import android.text.TextUtils;
import android.util.Log;

import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Preferences.GeneralPrefsDO;
import com.aldersonet.automatonalert.Service.AutomatonAlertService.ServiceHandler;
import com.aldersonet.automatonalert.Util.Encryption;
import com.aldersonet.automatonalert.Util.Utils;

import org.apache.k9.TrustManagerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

public class ImapGet extends EmailGet {

	public static final String TAG = "ImapGet";
	private static final String TAG_RETURN_OK = " OK";
	private static final String TAG_RETURN_BAD = " BAD";
	//
	// terminology
	//
	// messages = a set of messages
	// message = a set of headers
	// headers = a set of key=value pairs
	// key = "To", "From", "Date", etc.
	//
	//

	private volatile Socket mSocket;
	private volatile ImapStreamIO mImapStreamIO;
	private volatile boolean mNeedLogin = true;

	private String sTag = "0";

	private static final String TLS_MODE = "TLS";
	private static final int READ_BUFFER_LENGTH = 4096;
	private static final int WRITE_BUFFER_LENGTH = 1024;
	private static final int CONNECT_TIMEOUT = 60 * 1000;
	private static final int READ_TIMEOUT = 30 * 1000;

	private ProgressDialog mProgressCallback;
	private Thread mUiThread;
	private boolean mProgressCallbackReportRead = false;

	private static Pattern mRegExCROrLF = Pattern.compile("\\r|\\n");
	private List<String> mLatestUids = new ArrayList<String>();
	private int mLatestUidProcessed = 1;
	private HashSet<HashMap<String, String>> mLatestMessagesSet;

	@Override
	protected void finalize() throws Throwable {

		close();

		super.finalize();
	}

	private class ImapStreamIO {

		BufferedInputStream bIn;
		BufferedOutputStream bOut;

		private ImapStreamIO(final BufferedInputStream inStream,
				final BufferedOutputStream outStream) {

			bIn = inStream;
			bOut = outStream;

		}

		private void close() {

			if (bIn != null) {
				try {
					bIn.close();
				} catch (final IOException ignored) {
				}
				bIn = null;
			}
			if (bOut != null) {
				try {
					bOut.close();
				} catch (final IOException ignored) {
				}
				bOut = null;
			}
		}

		private String read(String lookFor) {
			ReadAsyncTask rat = new ReadAsyncTask(this);
			String ret = "";
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			    rat.executeOnExecutor(
			    		AsyncTask.THREAD_POOL_EXECUTOR, (lookFor));
			}
			else {
				rat.execute(lookFor);
			}

			try {
				ret = rat.get();

			} catch (InterruptedException | ExecutionException ignored) {}

            return ret;
		}

		class ReadAsyncTask extends AsyncTask<String, Void, String> {
			ImapStreamIO mStream;
			ReadAsyncTask(ImapStreamIO stream) {
				mStream = stream;
			}
			@Override
			protected String doInBackground(String... params) {
				String ret = "";
				try {
					ret = mStream.doRead(params[0]);

				} catch (IOException e) {
					try {
						throw new IOException(e.toString());
					} catch (IOException ignored) {}
				}

				return ret;
			}
		}

		private String doRead(final String lookFor) throws IOException {
			if (bIn == null) {
				return null;
			}

			String sBuffer = "";
			int numRead;
			int total = 0;

			try {
				final byte[] tempBuffer = new byte[READ_BUFFER_LENGTH];

				do {
					if ((numRead = bIn.read(tempBuffer, 0, READ_BUFFER_LENGTH)) == -1) {
						break;
					}

					if (mProgressCallback != null
							&& mProgressCallbackReportRead) {
						total += numRead;
						mProgressCallback.setMessage("Read " + total + " bytes");
					}

					sBuffer = sBuffer.concat(
							new String(tempBuffer, 0, numRead));
				}
				// if buffer was full, go back for more
				while (numRead == READ_BUFFER_LENGTH
						|| !sBuffer.contains(lookFor));

			} catch (final IOException e) {
				// we're done
			}

			Log.d(TAG + ".doRead()", "<<"+sBuffer);

			return sBuffer;
		}

		private String write(String sOut) throws IOException {
			WriteAsyncTask wat = new WriteAsyncTask(this);
			String ret = "";
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			    wat.executeOnExecutor(
			    		AsyncTask.THREAD_POOL_EXECUTOR, (sOut));
			}
			else {
				wat.execute(sOut);
			}

			try {
				ret = wat.get();

			} catch (InterruptedException | ExecutionException ignored) {}

            return ret;
		}

		class WriteAsyncTask extends AsyncTask<String, Void, String> {
			ImapStreamIO mStream;
			WriteAsyncTask(ImapStreamIO stream) {
				mStream = stream;
			}
			@Override
			protected String doInBackground(String... params) {
				String ret = "";
				try {
					ret = mStream.doWrite(params[0]);

				} catch (IOException e) {
					try {
						throw new IOException(e.toString());
					} catch (IOException ignored) {}
				}
				return ret;
			}
		}

		private String doWrite(final String sOut) throws IOException {
//			if (BuildConfig.DEBUG) {
//				if (sOut.startsWith("login ")) {
//					String sArray[] = sOut.split(" +");
//					int N = sArray.length;
//					sArray[N - 1] = "************";
//					// jic
//					sArray[2] = "************";
//					String out = "";
//					for (String value : sArray) {
//						out += value + " ";
//					}
//					Log.d(TAG + ".doWrite()", ">>" + out);
//				} else {
//					Log.d(TAG + ".doWrite()", ">>" + sOut);
//				}
//			}
			///////////////////

			final String s = sTag + " " + sOut + "\r\n";
			final byte[] outBuffer = s.getBytes();

			bOut.write(outBuffer, 0, outBuffer.length);
			bOut.flush();

			return sOut;
		}
	}

	//
	// constructor
	//
	public ImapGet(final AccountEmailDO acct) {
		super();
		mAccount = acct;
		if (acct != null) {
			sTag = acct.hashCode() + "1";
		}
		else {
			sTag = Math.round(Math.random()*10000000d) + "";
		}
	}

	public void setProgressCallback(ProgressDialog progressDialog, Thread thread) {
		mUiThread = thread;
		mProgressCallback = progressDialog;

	}

	public void checkMail(Context context, final ServiceHandler serviceHandler, int startId) {
		mLatestMessagesSet = new HashSet<>();

		try {
			// get headers from email account
			// populates mLatestMessagesSet
			searchInboxForLatest();

		// need to return on exceptions so we don't do any processing
		} catch (IOException e) {
			close();
			Log.e(TAG + ".checkMail()",
					"IO Exception for IMAP socket: " + e.toString());
			return;

		} catch (EmailGetException e) {
			close();
			Log.e(TAG + ".checkMail()",
					"IMAP Processing Exception: " + e.toString());
			return;
		}

		// for each message in set of latest messages
		// see if the user-specified search string(s) match
		// the user-specified header field
		// messages with successful searches will end up here
		int latestUidInFindSearchString = -1;

		if (mLatestMessagesSet.size() > 0) {
			latestUidInFindSearchString =
					mAccount.findSearchStringAndAlert(context, mLatestMessagesSet);
		}

		if (latestUidInFindSearchString != -1) {   // -1 if nothing found
			// save latest uid processed
			((AccountEmailDO) mAccount).setLatestUidProcessed(
					Integer.toString(latestUidInFindSearchString));

			if (mAccount.isDirty()) {
				mAccount.save();
			}
		}

		/////////
		close();
		/////////
	}

	public void close() {
		try {
			CloseAsyncTask cat = new CloseAsyncTask();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			    cat.executeOnExecutor(
			    		AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
			}
			else {
				cat.execute();
			}
			cat.get();

		} catch (InterruptedException | ExecutionException ignored) {}
    }

	class CloseAsyncTask extends AsyncTask<Void, Void, String> {
		@Override
		protected String doInBackground(Void...voids) {
			if (mImapStreamIO != null) {
				mImapStreamIO.close();
				if (mSocket != null) {
					try {
						mSocket.close();
					} catch (final IOException e) {
						//
					}
				}
			}
			mSocket = null;
			mNeedLogin = true;
			return "closed";
		}
	}

	//
	// login using mAccount, read the response
	//
	public void login() throws IOException, EmailGetException {
		if (mSocket == null || !(mSocket.isConnected())) {
			open();
		}

		if (!mNeedLogin) {
			return;
		}

		if (mProgressCallback != null) {
			mProgressCallback.setMessage("Logging in to IMAP server");
		}

		// try and re-throw so we can tell if we had a successful login (mNeedLogin = false)
		try {
			String ePassword = ((AccountEmailDO)mAccount).getPassword();
			String password = Encryption.decrypt(ePassword);
			enterCommandReceiveResponse("login " + ((AccountEmailDO) mAccount).getEmail() + " "
					+ password, sTag, sTag + TAG_RETURN_OK);

		} catch (IOException e) {
			throw new IOException(e.toString());

		} catch (GeneralSecurityException e) {
			throw new EmailGetException(e.toString());

		} catch (EmailGetException e) {
			// if it comes back with "BAD", then we're logged in already
			if (e.toString().indexOf(sTag + TAG_RETURN_BAD) != 0) {
				throw new EmailGetException(e.toString());
			}
		}

		mNeedLogin = false;

	}

	//
	// create the encryption socket, open it, read what the server says...
	//
	public void open() throws IOException, EmailGetException {
		if (mProgressCallback != null) {
			mProgressCallback.setMessage("Opening SSL connection to IMAP server");
		}

		try {
			OpenAsyncTask oat = new OpenAsyncTask();
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			    oat.executeOnExecutor(
			    		AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
			}
			else {
				oat.execute();
			}
			oat.get();
			enterCommandReceiveResponse(null, "", "* OK");

		} catch (InterruptedException | ExecutionException | IOException e) {
			throw new IOException(e.toString());
		} catch (EmailGetException e) {
			throw new EmailGetException(e.toString());
		}

	}

	class OpenAsyncTask extends AsyncTask<Void, Void, String> {

		SocketAddress mSocketAddress;
		SSLContext mSslContext;

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
		}

		@Override
		protected String doInBackground(Void...voids) {

			if (mSocket != null && mSocket.isConnected()) {
				mNeedLogin = false;
				return "already connected";
			}
			//close();	// just in case and to re-set
			mNeedLogin = true;

			try {
				mSocketAddress = new InetSocketAddress(((AccountEmailDO) mAccount).getHost(),
						((AccountEmailDO) mAccount).getPort());
				mSslContext = SSLContext.getInstance(TLS_MODE);
				mSslContext.init(
						null,
						new TrustManager[] { TrustManagerFactory.get(
								((AccountEmailDO) mAccount).getHost(), true) },
								new SecureRandom());

				mSocket = mSslContext.getSocketFactory().createSocket();
				mSocket.connect(mSocketAddress, CONNECT_TIMEOUT);
				mSocket.setSoTimeout(READ_TIMEOUT);

				mImapStreamIO = new ImapStreamIO(new BufferedInputStream(
						mSocket.getInputStream(), READ_BUFFER_LENGTH),
						new BufferedOutputStream(mSocket.getOutputStream(),
								WRITE_BUFFER_LENGTH));

			} catch (final NullPointerException | IOException | NoSuchAlgorithmException e) {
				try { throw new IOException(e.toString()); }
                catch (IOException ignored) {}

			} catch (final KeyManagementException | ArrayIndexOutOfBoundsException e) {
				try { throw new EmailGetException(e.toString()); }
                catch (EmailGetException ignored) {}
			}

            return "success";
		}
	}

	public String enterCommandReceiveResponse(
			final String command, final String tag, final String responseOkDelim)
			throws IOException, EmailGetException {

		String sResponse = "";
		String sError = "";
		int tries;
		int maxRetries = GeneralPrefsDO.getImapMaxRetries();

		for (tries = 1; tries <= maxRetries; tries++) {
			String localCommand = command == null ? "" : command;
			try {
				// see if our socket's still there
				if (mImapStreamIO == null
						|| mImapStreamIO.bIn == null
						|| mImapStreamIO.bOut == null) {
					throw new IOException("IMAP IOException: Socket dropped.");
				}
				// don't write if there's no command, just read response
				///////////////////////
				if (!localCommand.equals("")) {
					mImapStreamIO.write(localCommand);
				}
				sResponse = mImapStreamIO.read(tag);
				///////////////////////
				if (sResponse != null) {
					if (sResponse.contains(responseOkDelim)) {
						break;		// successful
					}
					else {
						sError = "IMAP command unsuccessful: "
								+ localCommand.split(" ")[0] + "; ";
						if (sResponse.indexOf(tag + TAG_RETURN_BAD) == 0) {
							// not a communications error, but a bad command, don't retry
							sError += "BAD command";
							break;
						}
					}
				} else {
					sError = "IMAP error: unable to communicate with server: "
							+ localCommand.split(" ")[0] + "; ";
				}
			} catch (IOException e) {
				sError += "IMAP error: unable to communicate with server: "
						+ localCommand.split(" ")[0] + "; ";
				// if we're attempting to read response from opening connection
				// don't close then open again
				if (localCommand.equals("")) {
					continue;
				}
				close();
				if (localCommand.contains("login")) {
					open();
				}
				else {
					login();
				}
			}
		}

		// error'd out
		if (tries > GeneralPrefsDO.getImapMaxRetries()
				|| !(sError.equals(""))) {
			if (sError.contains("IMAP error")) {
				throw new IOException(sError);
			}
			else {
				throw new EmailGetException(sError);
			}
		}

		return sResponse;
	}

	private void examineInbox() throws IOException, EmailGetException {

		login();

		if (mProgressCallback != null) {
			mProgressCallback.setMessage("Opening inbox Read-Only");
		}

		enterCommandReceiveResponse("examine INBOX", sTag, sTag + TAG_RETURN_OK);

	}

	private static Pattern mRegExCR = Pattern.compile("\\r");
	private static Pattern mRegExFirstLine = Pattern.compile("^\\*.*\\n");
	private static Pattern mRegExLeftover = Pattern.compile("\\)\\n$");

	public String fetchMessageBody(final String uid, final String bodyPart)
			throws IOException, EmailGetException {


		examineInbox();

		if (mProgressCallback != null) {
			mProgressCallbackReportRead = true;
			mProgressCallback.setMessage("Getting message body");
		}

		// get the message body
		String sResponse = enterCommandReceiveResponse("uid fetch " + uid
				+ " body[" + bodyPart + "]", sTag, sTag + TAG_RETURN_OK);

		if (sResponse.indexOf(sTag + TAG_RETURN_OK) == 0) {
			throw new EmailGetException("EmailGetException: Message no longer in INBOX");
		}

		sResponse = mRegExCR.matcher(sResponse).replaceAll("");
		sResponse = mRegExFirstLine.matcher(sResponse).replaceFirst("");
		sResponse = Pattern.compile(			// response line
				sTag + " OK .*$").matcher(sResponse).replaceFirst("");
		sResponse = mRegExLeftover.matcher(sResponse).replaceFirst("");

		if (mProgressCallback != null) {
			mProgressCallbackReportRead = false;
		}

		return sResponse;
	}

	private static Pattern mRegExBodyStructurePreamble =
			Pattern.compile("\\*.*bodystructure[ ]*",
					Pattern.CASE_INSENSITIVE);
	private static Pattern mRegExDelims = Pattern.compile("\"|\\(|\\)| ");
	private static Pattern mRegExQuoteOrSpace = Pattern.compile("[\" ]");

	// String[2][] holds text/plain and text/html bodystructure fields (charset, encoding,...)
	private String[][] mFields = new String[2][AutomatonAlert.FIELDLENGTH];

	public synchronized String[][] fetchMessageBodyStructure(final String uid)
			throws IOException, EmailGetException {

		examineInbox();

		if (mProgressCallback != null) {
			mProgressCallback.setMessage("Getting message display information");
		}

		// get the message bodystructure
		String sResponse = enterCommandReceiveResponse("uid fetch " + uid
				+ " bodystructure", sTag, sTag + TAG_RETURN_OK);

		if (sResponse.indexOf(sTag + TAG_RETURN_OK) == 0) {
			throw new EmailGetException("EmailGetException: Message no longer in INBOX");
		}

		if (sResponse.toLowerCase().contains("\"text\" \"plain\"")
				|| sResponse.toLowerCase().contains("\"text\" \"html\"")) {

			parseBodyStructure(sResponse);

		}

		// make position into IMAP body[pos]. e.g. 1 or 2 or 1.1 or 1.2 or
		// 1.1.1...
		for (int type = AutomatonAlert.PLAIN; type <= AutomatonAlert.HTML; type++) {
			if (!(TextUtils.isEmpty(mFields[type][AutomatonAlert.LEVEL]))) {
				int level = Utils.getInt(mFields[type][AutomatonAlert.LEVEL], -1);
				if (level == -1) {
					continue;
				}
				String position = "";
				for (int idx = 2; idx <= level; idx++) {
					position += "1.";
				}
				mFields[type][AutomatonAlert.POSITION] = position
						+ mFields[type][AutomatonAlert.POSITION];
			}
		}

		return mFields;
	}

	private int mMessageType = 99;
	private int mFieldIdx = 99;
	private StringTokenizer mSt = null;
	private boolean mEatCharsetToCloseParen = false;
	private int mMessagePosition = 0;

	private void parseBodyStructure(String sResponse)
			throws IOException {
		sResponse = mRegExBodyStructurePreamble.matcher(sResponse).replaceFirst("");
		sResponse = Pattern.compile(
				"\\)[\\r\\n]*" + sTag + " OK .*$", Pattern.CASE_INSENSITIVE)
						.matcher(sResponse).replaceFirst("");

		// delims quote, parens, space; return delims
		final String delims = "\"\\(\\) ";
		//final String delimRegEx = "\"|\\(|\\)| "; // or'd set
		String thisToken = "";
		int level = -1;
		boolean doneCountingLevel = false;

		mSt = new StringTokenizer(sResponse, delims, true);

		// get text/plain and text/html fields and put them in fields[][]
		while (mSt.hasMoreTokens()) {
			thisToken = mSt.nextToken();

			// if this is a delim
			if (mRegExDelims.matcher(thisToken).matches()) {
				level = handleDelims(thisToken, level, doneCountingLevel);
				continue;
			}

			// not a delim
			++mFieldIdx;
			doneCountingLevel = true;
			if (thisToken.equalsIgnoreCase("TEXT")) {
				handleText();
				continue;
			}
			switch (mFieldIdx) {
			case AutomatonAlert.SUBTYPE:
				handleSubType(thisToken, level);
				break;

			case AutomatonAlert.CHARSET:
				handleCharset(thisToken);
				break;

			case AutomatonAlert.ENCODING:
				handleEncoding(thisToken);
				break;

			default:
				break;
			}
		}
	}

	private int handleDelims(String thisToken, int level, boolean doneCountingLevel) {
		// level is counted only for the first set of parens
		if (thisToken.equals("(")) {
			// if charset is enclosed in parens, deal with separately
			if (AutomatonAlert.CHARSET == (mFieldIdx + 1)) {
				mEatCharsetToCloseParen = true;
			} else {
				// still at the beginning looking for level
				if (!doneCountingLevel) {
					++level;
				}
			}
		}
		return level;
	}

	private void handleText() {
		mFieldIdx = 0;
		++mMessagePosition;
	}

	private void handleSubType(String thisToken, int level) {
		mMessageType = 99;
		if (thisToken.equalsIgnoreCase("PLAIN")) {
			mMessageType = AutomatonAlert.PLAIN;
		}
		else if (thisToken.equalsIgnoreCase("HTML")) {
			mMessageType = AutomatonAlert.HTML;
		}
		if (mMessageType != 99) {
			// if we already have this type, move on
			if (mFields[mMessageType][AutomatonAlert.SUBTYPE] != null) {
				mFieldIdx = 99;	// will make it go to the next "TEXT"
			}
			else {
				mFields[mMessageType][AutomatonAlert.POSITION] =
						Integer.toString(mMessagePosition);
				mFields[mMessageType][AutomatonAlert.LEVEL] =
						Integer.toString(level);
				mFields[mMessageType][AutomatonAlert.SUBTYPE] = thisToken;
			}
		}
	}

	private void handleCharset(String thisToken)
			throws IOException {
		if (mMessageType != AutomatonAlert.HTML
				&& mMessageType != AutomatonAlert.PLAIN) {
			return;
		}
		String type = "";
		if (thisToken.equalsIgnoreCase("CHARSET")) {
			// parenthesized
			// get to next non-delim token
			while (mSt.hasMoreTokens()) {
				type = mSt.nextToken();
				if (!(mRegExDelims.matcher(type).matches())) {
					return;
				}
			}
		}
		++mFieldIdx;
		// if charset looks like "=?<charset>?<encoding>?blahblahblah", just
		type = Utils.decodeQuotedBase64(type, "");
		type = mRegExQuoteOrSpace.matcher(type).replaceAll("");
		mFields[mMessageType][AutomatonAlert.CHARSET] = type;
		if (mEatCharsetToCloseParen) {
			mEatCharsetToCloseParen = false;
			while (mSt.hasMoreTokens()) {
				type = mSt.nextToken();
				if (type.equals(")")) {
					return;
				}
			}
		}
	}

	private void handleEncoding(String thisToken) {
		if (mMessageType != AutomatonAlert.HTML
				&& mMessageType != AutomatonAlert.PLAIN) {
			return;
		}
		mFields[mMessageType][AutomatonAlert.ENCODING] = thisToken;
	}

	//
	// search the inbox for messages we haven't looked at yet
	//

	private synchronized void searchInboxForLatest()
			throws IOException, EmailGetException {

		mLatestUidProcessed =
				((AccountEmailDO) mAccount).getLatestUidProcessedInt();
		examineInbox();

		if (mProgressCallback != null) {
			mProgressCallback.setMessage("Getting list of emails to process");
		}

		// ////////////////////////////////////////
		// command: uid search uid n:*
		// response: * SEARCH 17421 17425 17426
		// ///////////////////////////////////////
		String sResponse = enterCommandReceiveResponse("uid search uid "
				+ (mLatestUidProcessed+1) + ":* unseen undeleted", sTag, sTag + TAG_RETURN_OK);

		mLatestUids = createListOfUnprocessedUids(sResponse);
		int num = mLatestUids.size();
		// if there's nothing to do, leave
		if (num > 0) {
			if (mProgressCallback != null) {
				mProgressCallback.setMessage("Getting " + num + " message headers");
			}

			// ///////////////////////////////////////////
			// get subject of each unseen message and add to HashMap
			// response: * 4 FETCH (UID 17248 BODY[HEADER.FIELDS (to from
			// subject...)] {128}
			// //////////////////////////////////////////
			fetchHeadersAndAddToLatestMessages(false/*limitToMaxSize*/, num);
		}
	}

	private List<String> createListOfUnprocessedUids(String sResponse) {
		// clean response string
		List<String> latestUids;

		sResponse = mRegExCROrLF.matcher(sResponse).replaceAll(" ");
		final int iTagPos = sResponse.indexOf(sTag + TAG_RETURN_OK);
		if (iTagPos >= 0) {
			sResponse = sResponse.substring(0, iTagPos);
		}
		// convert response to list of uids
		latestUids = Utils.intStringToList(sResponse, " ", sTag);
		// get rid of any we've already processed
		ArrayList<String> uidsToRemove = new ArrayList<>();
		for (final String uid : latestUids) {
			if (Utils.getInt(uid, Integer.MAX_VALUE) <= mLatestUidProcessed) {
				uidsToRemove.add(uid);
			}
		}
		for (String uid : uidsToRemove) {
			latestUids.remove(uid);
		}

		// highest UID first in case we're not getting them all
		// because user throttled size of list
		Collections.reverse(latestUids);

		return latestUids;
	}

	private void fetchHeadersAndAddToLatestMessages(
			boolean limitToMaxListSize, int num) throws IOException {

		String sResponse = "";
		int at = 0;
		for (final String sUid : mLatestUids) {
			try {
				sResponse = enterCommandReceiveResponse(
						"uid fetch "
								+ sUid
								+ " body[header.fields "
								+ "(date subject from sender reply-to to cc bcc message-id)]",
						sTag, sTag + TAG_RETURN_OK);
			} catch (final EmailGetException e) {
				break; // we'll try to process this message+ later
			}

			++at;

			if (mProgressCallback != null) {
				mProgressCallback.setMessage(
						"Getting " + at + " of " + num + " headers");
			}
			// each header line gets put into keyValue HashMap
			// each set of header lines gets put into mLatestMessagesSet set
			sResponse = mRegExCR.matcher(sResponse).replaceAll("");
			final String[] sResponses = sResponse.split("\\n"); // split on linefeed

			// split each line on ':'
			final HashMap<String, String> keyValue = new HashMap<>(
					sResponses.length);
			for (final String sLine : sResponses) {
				final int idx = sLine.indexOf(':');
				if (idx >= 0) {
					// first put in UID=uid pair and account key pair
					if (keyValue.isEmpty()) {
						keyValue.put(AutomatonAlert.UID, sUid);
						keyValue.put(AutomatonAlert.ACCOUNT_KEY, mAccount.getKey());
					}
					// FROM, TO, SUBJECT...
					final String key = sLine.substring(0, idx).trim();
					String value = sLine.substring(idx + 1).trim();
					// decode all fields
					value = Utils.decodeQuotedBase64(
							value,
							AutomatonAlert.QUOTED_PRINTABLE);
					if (key.equals(AutomatonAlert.FROM)) {
						keyValue.put(
								Contacts.DISPLAY_NAME,
								Utils.stripAllEmailAddresses(value));
					}
					keyValue.put(key, value);
				}
			}
			if (!keyValue.isEmpty()) {
				mLatestMessagesSet.add(keyValue);
			}

			if (limitToMaxListSize) {
				if (at >= GeneralPrefsDO.getMaxListSize()) {
					break;
				}
			}
		}
	}

	public String getTopUid() {
		String ret = null;
		String simpleTag = TAG_RETURN_OK;
		int latestUid = 1;

		try {
			examineInbox();

			if (mAccount != null) {
				latestUid = ((AccountEmailDO) mAccount).getLatestUidProcessedInt();
			}

			String sResponse =
					enterCommandReceiveResponse(
							"uid search uid "
									+ latestUid
									+ ":* not deleted",
							simpleTag,
							simpleTag);

			// clean response string
			sResponse = mRegExCROrLF.matcher(sResponse).replaceAll(" ");
			final int iTagPos = sResponse.indexOf(simpleTag);
			if (iTagPos >= 0) {
				sResponse = sResponse.substring(0, iTagPos);
			}
			// convert response to list of uids
			List<String> latestUids =
					Utils.intStringToList(sResponse, " ", sTag);
			if (latestUids.size() > 0) {
				ret = latestUids.get(latestUids.size() - 1);
			}
		}
		catch (IOException | EmailGetException ignored) {}

        return ret;
	}
}
