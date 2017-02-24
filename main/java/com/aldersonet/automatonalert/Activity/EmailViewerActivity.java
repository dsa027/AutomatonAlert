package com.aldersonet.automatonalert.Activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.SpannedString;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

import com.aldersonet.automatonalert.Account.AccountDO;
import com.aldersonet.automatonalert.Account.Accounts;
import com.aldersonet.automatonalert.AutomatonAlert;
import com.aldersonet.automatonalert.Email.AccountEmailDO;
import com.aldersonet.automatonalert.Email.EmailGet;
import com.aldersonet.automatonalert.Email.EmailGetException;
import com.aldersonet.automatonalert.Fragment.AlertListFragment;
import com.aldersonet.automatonalert.R;
import com.aldersonet.automatonalert.SMS.AccountSmsDO;
import com.aldersonet.automatonalert.Util.AAProgressDialog;
import com.aldersonet.automatonalert.ActionBar.ActionBarDrawer;
import com.aldersonet.automatonalert.Util.Utils;
import com.google.android.mms.ContentType;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Pattern;

public class EmailViewerActivity extends AppCompatActivity {
	public static String ACTON_BAR_TITLE = "Message Viewer";

	private AccountDO mAccount;
	private WebView mWebView;
	private String mBody = "";
	private HashMap<String, String> mMessage;

	private AlertListFragment.Mode mListMode;

	private BodyAsyncTask mBodyTask;
	ProgressDialog mProgressDialog;
	String mResult;
	boolean mBodyTaskFinished;

	ActionBarDrawer mActionBarDrawer;

    @Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (mActionBarDrawer != null) {
			mActionBarDrawer.getDrawerToggle().onConfigurationChanged(newConfig);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mActionBarDrawer != null) {
			if (mActionBarDrawer.getDrawerToggle().onOptionsItemSelected(item)) {
				return true;
			}
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		mActionBarDrawer.getDrawerToggle().syncState();
	}

	private void setDrawer() {
		mActionBarDrawer = new ActionBarDrawer(this);
	}

	private Bundle getAllFromBundle(final Bundle bundle) {
		return bundle;
	}

	@Override
	public void onActivityResult(final int requestCode, final int resultCode,
			final Intent data) {
		if (requestCode == 0) {
			if (resultCode == RESULT_OK) {
			}
		}
	}

	@Override
	public boolean onKeyUp(int keyCode, @NotNull KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			mActionBarDrawer.openDrawer();
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

    private ActionBar setActionBar() {
		ActionBar ab = getSupportActionBar();
		if (ab != null) {
			Utils.setActionBarCommon(getResources(), ab, ACTON_BAR_TITLE);
			ab.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		}

		return ab;
	}

	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//davedel -- disallow access
		finish();
		//davedel

		setContentView(R.layout.email_viewer);
		setActionBar();
		setDrawer();
		mBodyTask = (BodyAsyncTask)getLastCustomNonConfigurationInstance();

		if (savedInstanceState != null) {
			getAllFromBundle(savedInstanceState);
		}
		mBody = null;
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		// The activity is about to be destroyed.
		if (mAccount != null
				&& mAccount.mAccountType == AccountEmailDO.ACCOUNT_EMAIL) {
			// not updating, don't need to tryAcquire()
			AutomatonAlert.getMailGetSemaphores().get(
					mAccount.getKey()).mMailHandle.close();
		}
		destroyWebView();
	}

	private void destroyWebView() {

		if (mWebView == null) {
			return;
		}
		mWebView.setVisibility(WebView.GONE);
		mWebView.destroy();
		mWebView = null;
	}



	@SuppressWarnings("unchecked")
	@Override
	protected void onStart() {
		super.onStart();

//		AutomatonAlert.populateAppData();

		// get mMessage from server, process it, then show in WebView
		Intent thisActivityIntent = getIntent();

		mListMode = (AlertListFragment.Mode) thisActivityIntent
				.getSerializableExtra(AutomatonAlert.M_MODE);

		// get the mMessage from the intent
		final HashMap<String, String> intentMessage = (HashMap<String, String>) thisActivityIntent
				.getSerializableExtra(AutomatonAlert.MESSAGE_HASHMAP_TYPE);

		// don't get the mMessage if it's already in view
		if (Utils.equals(intentMessage, mMessage)) {
			return;
		}

		mMessage = intentMessage;

		// clean up our very own cache
		//clearAppCacheFolder(this.getApplicationContext().getCacheDir());

		final Button button = (Button) findViewById(R.id.message_view_show_pictures_button);
		button.setVisibility(View.GONE); // default

		// set header text
		final String sHeader = Utils.formatHeadersForView(
				Utils.markupHeaderFields(mMessage),
				false);/*no headers*/
		final TextView tvHeader = (TextView) findViewById(R.id.message_view_textview_header);
//		tvHeader.setTextColor(Color.parseColor(AutomatonAlert.THEME_HTML_PINK_HEX));
		tvHeader.setText(Html.fromHtml(sHeader));

		// get account
		final String accountKey = (mMessage.get(AutomatonAlert.ACCOUNT_KEY) == null ? ""
				: mMessage.get(AutomatonAlert.ACCOUNT_KEY));
		// will throw IOException on null
		mAccount = Accounts.get(accountKey);
		if (mAccount == null) {
			mBody = Html.toHtml(new SpannedString(
					"Unable to obtain account information"));
		}

		// nothing to do if it's an SMS
		if (mAccount != null
				&& mAccount.mAccountType == AccountSmsDO.ACCOUNT_SMS) {
			return;
		}


		// //////////////////////////
		// process mBody
		//
		// split this off onto its own thread to save UI
		// //////////////////////////

		if (mAccount != null) {
			if (mAccount.mAccountType == AccountEmailDO.ACCOUNT_EMAIL) {
				if (mBodyTask == null) {
					mBodyTask = new BodyAsyncTask(this, mAccount);
					mBodyTask.execute(mMessage);
				}
				else {
					mBodyTask.attach(this);
				}
			}
		}
	}

	@Override
	public Object onRetainCustomNonConfigurationInstance() {
		mBodyTask.detach();
		if (mBodyTaskFinished) {
			return null;
		}
		return mBodyTask;
	}

	static class BodyAsyncTask extends
			AsyncTask<HashMap<String, String>, Void, String> {

		EmailViewerActivity mEmailViewActivity;
		AccountDO mAccount;

		public BodyAsyncTask(EmailViewerActivity messageViewActivity, AccountDO account) {
			attach(messageViewActivity);
			mAccount = account;
		}

		void attach(EmailViewerActivity activity) {
			mEmailViewActivity = activity;
		}

		void detach() {
			mEmailViewActivity = null;
		}

		@Override
		protected void onPreExecute() {
			mEmailViewActivity.mBodyTaskFinished = false;
			String message =
					"Please wait while the message is retrieved from the server...";
			mEmailViewActivity.mProgressDialog =
					new AAProgressDialog(
							mEmailViewActivity,
							message);
			mEmailViewActivity.mProgressDialog.setCancelable(false);
			mEmailViewActivity.mProgressDialog.setMessage(message);
			mEmailViewActivity.mProgressDialog.setIndeterminate(true);
			mEmailViewActivity.mProgressDialog.setButton(
					ProgressDialog.BUTTON_NEGATIVE,
					AutomatonAlert.CANCEL_LABEL,
					new ProgressDialog.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,
								int which) {
							cancel(false);
							mEmailViewActivity.finish();
						}
					});
			mEmailViewActivity.mProgressDialog.show();
		}

		@Override
		protected String doInBackground(
				final HashMap<String, String>... mMessage) {
			// not updating, don't need to tryAcquire()
			AutomatonAlert.getMailGetSemaphores().get(
					mAccount.getKey()).mMailHandle.setProgressCallback(
							mEmailViewActivity.mProgressDialog,
							Thread.currentThread());
			return mEmailViewActivity.processBody(mMessage[0]);
		}

		@Override
		protected void onPostExecute(final String result) {
			mEmailViewActivity.mResult = result;
			if (mEmailViewActivity.mProgressDialog != null) {
				mEmailViewActivity.mProgressDialog.dismiss();
				mEmailViewActivity.mProgressDialog = null;
			}
			mEmailViewActivity.loadBodyIntoWebView(result);
			mEmailViewActivity.mBodyTaskFinished = true;
		}

	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		boolean ret = super.onKeyDown(keyCode, event);
		if (keyCode == KeyEvent.KEYCODE_BACK) {
//			int alertItemId = mThisActivityIntent.getIntExtra(
//					AlertItems.ALERT_ITEMS_PREFIX, -1);

			// don't update non-ALARMS/ALERT mode
			if (mListMode != null
					&& !(mListMode.equals(AlertListFragment.Mode.ALARM))
					&& !(mListMode.equals(AlertListFragment.Mode.ALERT))) {
				return ret;
			}

			destroyWebView();
			finish();
		}
		return ret;
	}

	@Override
	public void onWindowFocusChanged(final boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
	}

	private static Pattern mRegExCR = Pattern.compile("\\r");
	private static Pattern mRegExLF = Pattern.compile("\\n");
//	private static Pattern mRegExEqualLF = Pattern.compile("=\\n");
    private static Object syncObject = new Object();

	private String processBody(final HashMap<String, String> mMessage) {
		boolean criticalError = false;
        String criticalErrorMsg = null;
		String errorMessage = "";
		int type = -1;
		// not updating, don't need to tryAcquire()
		EmailGet mailHandle = AutomatonAlert.getMailGetSemaphores().get(
				mAccount.getKey()).mMailHandle;

		/////////////////////
		// get body from cache or from server and process
		/////////////////////
		try {

			String encoding = AutomatonAlert.QUOTED_PRINTABLE;
			String charset = "ASCII";
			synchronized (syncObject) {

				////////////////////
				// not in cache, get it from server
				////////////////////
				final String[][] fields = mailHandle
						.fetchMessageBodyStructure(mMessage.get(AutomatonAlert.UID));

				// try to get text/html, if not found, get text/plain
				mBody = "---unable to read mMessage mBody---";

				String bodyDescriptors[] = Utils.getBodyDescriptors(fields);
				String sType = bodyDescriptors[Utils.TYPE];
				type = Utils.getInt(sType, AutomatonAlert.HTML);
				charset = bodyDescriptors[Utils.CHARSET];
				encoding = bodyDescriptors[Utils.ENCODING];
				String bodyPart = bodyDescriptors[Utils.BODYPART];

				// //////////////////////////
				// get and process mBody
				// //////////////////////////
				// get the mBody part from the server
				mBody = mailHandle.fetchMessageBody(
						mMessage.get(AutomatonAlert.UID), bodyPart);

			}
			// charset translation
			ByteArrayOutputStream bOut = new ByteArrayOutputStream();
			bOut.write(mBody.getBytes());
			mBody = bOut.toString(charset);

			// if text/plain, make html
			if (type == AutomatonAlert.PLAIN) {
				mBody = mRegExCR.matcher(mBody).replaceAll("");
				mBody = mRegExLF.matcher(mBody).replaceAll("<br>");
				mBody = "<html><body>" + mBody + "</body></html>";
			}

			// if there are 2 "=3D" strings, then it's likely that there are
			// invalid line continuations
			// get rid of EOLs like "=\r\n"...
			if (type == AutomatonAlert.HTML
					&& mBody.matches("(?ims:.*=[0-9A-F]{2}.*)")) {
				mBody = Utils.tripletOfEqualSignPlusHexByteToDecString(mBody);
				mBody = mRegExCR.matcher(mBody).replaceAll("");
				mBody = mRegExLF.matcher(mBody).replaceAll("");
			}

			// base64 and quoted-printable decoding
			mBody = Utils.decodeQuotedBase64(mBody, encoding);
			// uri decoding
			mBody = Uri.decode(mBody); // get rid of %hex's
			// encoding = "UTF-8"; // now it's all UTF-8 because of Uri.decode()

			// //////////////////////////
			// process WebView.loadData... errors
			// //////////////////////////
		} catch (final IOException e) {
			criticalError = true;
			errorMessage = "Unable to connect to mail server (try again later)";
		} catch (final EmailGetException e) {
			criticalError = true;
			if (e.getMessage().contains("Message no longer in INBOX")) {
				errorMessage = "This message is no longer in the INBOX and cannot be displayed";
			}
			else {
				errorMessage = "Unable to read mMessage from server (try again later)";
			}
		} catch (final Exception e) {
			criticalError = true;
			errorMessage = "Unable to view mMessage";
		} finally {
			mailHandle.close();
			if (criticalError) {
				criticalErrorMsg =
                        Html.toHtml(new SpannedString(Uri.decode(errorMessage)));
			}
		}

        if (criticalErrorMsg != null) {
            return criticalErrorMsg;
        }

		return mBody;
	}

	private void setImageButton(final String mBody) {

		if (mAccount.mAccountType != AccountEmailDO.ACCOUNT_EMAIL) {
			return;
		}

		// defaults
		final Button button = (Button) findViewById(R.id.message_view_show_pictures_button);
		button.setVisibility(View.GONE);
		mWebView.getSettings().setLoadsImagesAutomatically(false);

		// if we aren't automatically showing images...
		if (!((AccountEmailDO)mAccount).getShowImagesInEmail()) {
			// and there's an image...
			if (mBody.contains("<img ") || mBody.contains("<IMG ")) {
				// then show the button and wait for a click to show images
				button.setVisibility(Button.VISIBLE);
				button.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(final View v) {
						button.setVisibility(View.GONE);
						mWebView.getSettings().setLoadsImagesAutomatically(true);
						mWebView.refreshDrawableState();
					}
				});
			}
		}
	}

	private void setWebViewSettings() {
		mWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.getSettings().setLoadWithOverviewMode(true);
		mWebView.getSettings().setBuiltInZoomControls(true);
		mWebView.getSettings().setSupportZoom(true);
		mWebView.getSettings().setDefaultZoom(WebSettings.ZoomDensity.FAR);
		if (mAccount == null) {
			mWebView.getSettings().setLoadsImagesAutomatically(false);
		} else {
			if (mAccount.mAccountType == AccountEmailDO.ACCOUNT_EMAIL) {
				mWebView.getSettings().setLoadsImagesAutomatically(
						((AccountEmailDO)mAccount).getShowImagesInEmail());
			}
		}
		if (mWebView.getSettings().getBlockNetworkLoads()) {
			mWebView.getSettings().setBlockNetworkLoads(false);
		}
		if (mWebView.getSettings().getBlockNetworkImage()) {
			mWebView.getSettings().setBlockNetworkImage(false);
		}
	}

	private void loadBodyIntoWebView(final String body) {
		// set up the browser to show the email
		mWebView = (WebView) findViewById(R.id.webview);
		setWebViewSettings();
		// show the email in the web browser (always will end up processed into
		// html)
		mWebView.loadDataWithBaseURL("email://", body, ContentType.TEXT_HTML, "UTF-8",
				null);
		// if there's an image in the mBody
		setImageButton(body);
		// attempt to merge header and WebView into one scrolling entity
	}
}

