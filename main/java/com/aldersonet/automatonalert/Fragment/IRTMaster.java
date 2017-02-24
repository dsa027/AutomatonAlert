package com.aldersonet.automatonalert.Fragment;

import android.text.Spanned;
import android.view.View;

interface IRTMaster {
	boolean isInitialErrorCheckDone();
	void setInitialErrorCheckDone();
	void setViewPointers(View v);
	void setInitialDbRecs();
	void setViewComponents(View v);
	void setMasterFieldHint();
	void setViewDefaults();
	void setMasterField(String text, Object object, boolean skipKeyFieldEqualCheck);
	boolean isMasterFieldChanged(String text, Object obj);
	void deleteMaster();
	void setMasterViewToDefaults();
	void updateMasterFieldAndViews(String phrase);
	void setMasterListeners();
	void enableDisableMasterFields();
	void enableDisableMasterFields(boolean enable);
	void setAlphaMasterFields(float f);
	void initializeNewMaster();
	void saveMasterNotificationItemId(int id);
	boolean clearMaster(Object ... obj);
	void addDefaultAccounts();
	Spanned getNoAccountsErrorText();

}
