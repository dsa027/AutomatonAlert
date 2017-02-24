package com.aldersonet.automatonalert.Fragment;

public interface ILookupKeyMerger {
	boolean/*merged*/ mergeRecs(String dbKey, String topKey);
}
