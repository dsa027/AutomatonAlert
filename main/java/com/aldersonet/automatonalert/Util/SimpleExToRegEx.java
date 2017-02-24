/**
 *
 */
package com.aldersonet.automatonalert.Util;

import java.util.regex.Pattern;

/**
 * @author David
 *
 */
public class SimpleExToRegEx {

	private static Pattern period			= Pattern.compile("\\.");
	private static Pattern backslash 		= Pattern.compile("\\\\");
	private static Pattern plus				= Pattern.compile("\\+");
	private static Pattern lParen	 		= Pattern.compile("\\(");
	private static Pattern rParen 			= Pattern.compile("\\)");
	private static Pattern lBracket 		= Pattern.compile("\\[");
	private static Pattern rBracket 		= Pattern.compile("\\]");
	private static Pattern lSquigglyBracket = Pattern.compile("\\{");
	private static Pattern rSquigglyBracket = Pattern.compile("\\}");
	private static Pattern caret 			= Pattern.compile("\\^");
	private static Pattern dollar 			= Pattern.compile("\\$");
	private static Pattern qMark            = Pattern.compile("\\?");
	private static Pattern asterisk 		= Pattern.compile("\\*");
	private static Pattern hash 			= Pattern.compile("#");

	public static String simpleExToRegEx(String sSimpleEx) {
		String mStr = sSimpleEx;

		// do period first so that qMark and asterisk to get overwritten
		mStr = period.matcher(mStr)			.replaceAll("\\\\.");
		mStr = plus.matcher(mStr)			.replaceAll("\\\\+");
		mStr = backslash.matcher(mStr)		.replaceAll("\\\\\\\\");
		mStr = lParen.matcher(mStr)			.replaceAll("\\\\(");
		mStr = rParen.matcher(mStr)			.replaceAll("\\\\)");
		mStr = lBracket.matcher(mStr)		.replaceAll("\\\\[");
		mStr = rBracket.matcher(mStr)		.replaceAll("\\\\]");
		mStr = lSquigglyBracket.matcher(mStr).replaceAll("\\\\{");
		mStr = rSquigglyBracket.matcher(mStr).replaceAll("\\\\}");
		mStr = caret.matcher(mStr)			.replaceAll("\\\\^");
		mStr = dollar.matcher(mStr)			.replaceAll("\\\\$");
		mStr = qMark.matcher(mStr)			.replaceAll(".");
		mStr = asterisk.matcher(mStr)		.replaceAll(".*");
		mStr = hash.matcher(mStr)			.replaceAll("[0-9]");

		return mStr;
	}
}
