package com.aldersonet.automatonalert.Util;

import java.util.ArrayList;

public class Lists<T> {
	public ArrayList<T> fill(Object fillWith, int size) {
		ArrayList<T> list = new ArrayList<T>(size);
		for(int i=0;i<size;i++) list.add(i, null);

		return list;
	}
}
