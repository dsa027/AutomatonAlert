package com.aldersonet.automatonalert.Email;

import android.app.ProgressDialog;
import android.content.Context;

import com.aldersonet.automatonalert.Account.AccountDO;
import com.aldersonet.automatonalert.Service.AutomatonAlertService.ServiceHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

public class EmailGet {

	public volatile AccountDO mAccount;

	public void open() throws IOException, EmailGetException {}
	public void close() {}
	public void login() throws IOException, EmailGetException {}
	public void checkMail(Context context, ServiceHandler serviceHandler, int startId) {}

	public String enterCommandReceiveResponse(
			String command,
			String tag,
			String responseOkDelim)
		throws IOException, EmailGetException { return null; }

	public void setProgressCallback(ProgressDialog progressDialog, Thread thread) {}

	protected HashSet<HashMap<String, String>> searchInboxForLatest(
			boolean getLatestUidFromPreferences,
			boolean limitToMaxListSize)
		throws IOException, EmailGetException { return null; }

	public String fetchMessageBody(
			String uid,
			String bodyPart)
		throws IOException, EmailGetException { return null; }

	public String[][] fetchMessageBodyStructure(String uid)
			throws IOException, EmailGetException { return null; }

	public String getTopUid() {return null; }
}
