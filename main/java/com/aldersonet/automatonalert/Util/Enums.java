package com.aldersonet.automatonalert.Util;

public class Enums<T extends Enum> {
	public static <T> T getEnum(int ordinal, T[] array, T def) {
		T t = getEnum(array, ordinal);

		return (t == null) ? def : t;
	}

	public static <T> T getEnum(String string, T[] array, T def) {
		T t = getEnum(array, string);

		return (t == null) ? def : t;
	}
	private static <T> T getEnum(T[] array, int ordinal) {
		if (array == null
				|| ordinal < 0
				|| ordinal >= array.length) {
			return null;
		}

		return array[ordinal];
	}

	private static <T> T getEnum(T[] array, String string) {
		for(T t : array) {
			if (((Enum)t).name().equals(string)) {
				return t;
			}
		}

		return null;
	}
}
