package com.aldersonet.automatonalert.Cache;

import android.support.v4.util.LruCache;

// Key = the key: currently String or Integer
// Rec = Object to store
// Listener = where to get the Rec if it's not in the Cache
public class Cache<Key, Rec> {
	public static final String TAG = "Cache";

	ICacheRecListener mListener;
	final LruCache<Key, Rec> mCache;
	int mSize;

	public static interface ICacheRecListener {
		<Rec> Rec getCacheRecFromSource(Object id);
		<Key> Key getId();
	}

	public Cache(int size, ICacheRecListener listener) {
		mListener = listener;
		mSize = size;
		mCache = new LruCache<Key, Rec>(mSize);
	}

	public static final int MIN_SIZE = 1;
	public boolean resize() {
		int holdSize = mSize;
		mSize = mCache.size() * 3/4;

		if (mSize < MIN_SIZE) mSize = MIN_SIZE;
		if (mSize == holdSize) return false;

		mCache.trimToSize(mSize);

		return true;
	}

	private boolean isRemove(Key id) {
		if (id == null) return true;

		if (id instanceof Number) {
			if ((Integer)id == -1) {
				return true;
			}
		}
		if (id instanceof String) {
			if (id.equals("")) {
				return true;
			}
		}

		return false;
	}

	public void remove(Key id) {
		if (id == null) return;

		if (isRemove(id)) {
			return;
		}
		mCache.remove(id);
	}

	public void add(Key id, Rec rec) {
		if (id == null) return;

		mCache.put(id, rec);
	}

	public Rec get(Key id) {
		if (id == null) return null;

		Rec rec = null;

		synchronized (mCache) {
			// GET from CACHE
			rec = mCache.get(id); // see if it's in cache
			// nothing from cache
			if (rec == null) {
				rec = mListener.getCacheRecFromSource(id);  // get it from source (db, file, ...)
				if (rec != null) {
					mCache.put(id, rec); // got it, add it to cache
				}
			}
			else {
				logIt(id);
			}
		}

		return rec;
	}

	public static String getClassName(Object obj) {
		if (obj == null) return "";

		String s = obj.toString();
		s = s.substring((s.lastIndexOf('.') + 1));
		int i = s.indexOf('@');
		if (i != -1) {
			s = s.substring(0, i);
		}

		return s;
	}

	public static String ellipseString(String str) {
		if (str == null) return "(null)";

		if (str.length() < 20) return str;

		//                                      last char = str.length()-1
		// 9 + 3 + 8 = 20;
		String s1 = str.substring(0, 9);
		String s2 = str.substring(str.length()-9);

		return (s1 + "..." + s2);
	}

	private String formatDecimal(float in) {
		return String.format("%.2f", in);
	}

	private void logIt(Key id) {
		if (id == null) return;

//		if (BuildConfig.DEBUG) {
//			int hit = mCache.hitCount();
//			int miss = mCache.missCount();
//
//			Log.d(TAG + ".get(" + ellipseString(id.toString()) + ")",
//					"Cache HIT(" + getClassName(mListener) + ")"
//							+ hit + "/" + (hit + miss)
//							+ " " + formatDecimal((float) hit / (hit + miss) * 100) + "%");
//		}
	}
}
