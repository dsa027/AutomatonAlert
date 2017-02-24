package com.aldersonet.automatonalert.Fragment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.Html;
import android.text.Spanned;
import android.view.View;
import android.widget.TextView;

import com.aldersonet.automatonalert.Activity.ContactFreeFormListActivity;
import com.aldersonet.automatonalert.Activity.ContactFreeFormListActivity.FragmentTypeCL;
import com.aldersonet.automatonalert.Activity.ICLActivity;
import com.aldersonet.automatonalert.AutomatonAlert;

public class ContactListGenericBase {

	public static final String TAG = "ContactListGenericBase";

	public static void showHideTextForEmptyList(
			ICLActivity activity,
			int listSize,
			FragmentTypeCL fragmentType,
			TextView textView,
			int res,
			Intent intent) {

		if (textView == null) {
			return;
		}

		if (activity.isThisFragmentShowingNow(fragmentType)) {
			if (listSize <= 0) {
				setEmptyListText(textView, res, intent);
			} else {
				textView.setVisibility(TextView.GONE);
			}
		}
	}

	private static void setEmptyListText(
			TextView textView, int res, final Intent intent) {

		final Context context = AutomatonAlert.THIS.getApplicationContext();

		textView.setVisibility(TextView.VISIBLE);
		textView.setText(getEmptyListText(res));
		if (intent != null) {
			textView.setOnClickListener(new TextView.OnClickListener() {
				@Override
				public void onClick(View v) {
					context.startActivity(intent);
				}
			});
		}
	}

	private static Spanned getEmptyListText(int res) {
		return Html.fromHtml(
				AutomatonAlert.THIS.getResources().getString(res));
	}

	public static Intent getIntentShowContacts() {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse("content://contacts/people"));
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		return intent;
	}

	public static Intent getIntentGoToSearch() {
		Intent intent = new Intent(
				AutomatonAlert.THIS,
				ContactFreeFormListActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra(
				AutomatonAlert.FRAGMENT_TYPE,
				FragmentTypeCL.SEARCH.name());

		return intent;
	}

}
