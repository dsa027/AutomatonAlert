// http://code.google.com/p/notime/
// unknown license

package com.aldersonet.automatonalert.Util;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ListView;

import com.aldersonet.automatonalert.R;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author declanshanaghy
 * http://blog.350nice.com/wp/archives/240
 * MultiChoice Preference Widget for Android
 *
 * @contributor matiboy
 * Added support for check all/none and custom separator defined in XML.
 * IMPORTANT: The following attributes MUST be defined (probably inside attr.xml) for the code to even compile
 * <declare-styleable name="ListPreferenceMultiSelect">
    	<attr format="string" name="checkAll" />
    	<attr format="string" name="separator" />
    </declare-styleable>
 *  Whether you decide to then use those attributes is up to you.
 *
 */
public class ListPreferenceMultiSelect extends ListPreference {
	public String separator;
	private static final String DEFAULT_SEPARATOR = "OV=I=XseparatorX=I=VO";
	//private static final String LOG_TAG = "ListPreferenceMultiSelect";
	private String checkAllKey = "*";
	private boolean[] mClickedDialogEntryIndices;

	// Constructor
	public ListPreferenceMultiSelect(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ListPreferenceMultiSelect);
        checkAllKey = a.getString( R.styleable.ListPreferenceMultiSelect_checkAll );
        String s = a.getString(R.styleable.ListPreferenceMultiSelect_separator );
        if (checkAllKey == null) {
        	checkAllKey = "*";
        }
        if( s != null ) {
        	separator = s;
        } else {
        	separator = DEFAULT_SEPARATOR;
        }
     // Initialize the array of boolean to the same size as number of entries
        mClickedDialogEntryIndices = new boolean[1];//getEntries().length];
        a.recycle();
    }

	@Override
    public void setEntries(CharSequence[] entries) {
    	super.setEntries(entries);
    	// Initialize the array of boolean to the same size as number of entries
        mClickedDialogEntryIndices = new boolean[entries.length];
    }

    public ListPreferenceMultiSelect(Context context) {
        this(context, null);
    }

    @Override
    protected void onPrepareDialogBuilder(@NotNull Builder builder) {
    	CharSequence[] entries = getEntries();
    	CharSequence[] entryValues = getEntryValues();
        if (entries == null || entryValues == null || entries.length != entryValues.length ) {
            throw new IllegalStateException(
                    "ListPreference requires an entries array and an entryValues array which are both the same length");
        }
        restoreCheckedEntries();
        builder.setMultiChoiceItems(entries, mClickedDialogEntryIndices,
                new DialogInterface.OnMultiChoiceClickListener() {
					public void onClick(DialogInterface dialog, int which, boolean val) {
						if(isCheckAllValue( which )) {
							checkAll( dialog, val );
						}
						else {	// not checkAllKey
							int checked = 0;
					    	int size = mClickedDialogEntryIndices.length;
							if (val) {	// if entry is checked
								// unclick "all" if something else is checked
								for(int i = 0; i < size; i++) {
									if (isCheckAllValue(i)) {
										mClickedDialogEntryIndices[i] = false;
								    	((AlertDialog) dialog)
								    			.getListView()
								    			.setItemChecked(i, false);
									}
									else {
										if (mClickedDialogEntryIndices[i]) {
											++checked;
										}
									}
								}
							}
							// if they're all checked, check "all"
							// and uncheck everything else
							if (checked == (size-1)) {
								val = false; // make sure this entry is unchecked
								for(int i = 0; i < size; i++) {
									if (isCheckAllValue(i)) {
										mClickedDialogEntryIndices[i] = true;
										((AlertDialog)dialog)
										.getListView()
										.setItemChecked(i, true);
									}
									else {
										mClickedDialogEntryIndices[i] = false;
										((AlertDialog)dialog)
										.getListView()
										.setItemChecked(i, false);
									}
								}
							}
						}
						mClickedDialogEntryIndices[which] = val;
					}
        });
    }

	private boolean isCheckAllValue( int which ){
    	final CharSequence[] entryValues = getEntryValues();
    	if(checkAllKey != null) {
			return entryValues[which].equals(checkAllKey);
		}
    	return false;
    }

    private void checkAll() {

    	int size = mClickedDialogEntryIndices.length;
		for(int i = 0; i < size; i++) {
			mClickedDialogEntryIndices[i] = isCheckAllValue(i);
		}
    }

    private void checkAll( DialogInterface dialog, boolean val ) {
    	ListView lv = ((AlertDialog) dialog).getListView();
		int size = lv.getCount();
		for(int i = 0; i < size; i++) {
			if (val) {
				if (isCheckAllValue(i)) {
					View v = lv.getChildAt(i);
					if (v != null) {
						//v.setEnabled(true);
						lv.setItemChecked(i, true);
					}
					mClickedDialogEntryIndices[i] = true;
				}
				else {
					View v = lv.getChildAt(i);
					if (v != null) {
						//v.setEnabled(false);
						lv.setItemChecked(i, false);
					}
					mClickedDialogEntryIndices[i] = false;
				}
			}
			else {
				View v = lv.getChildAt(i);
				if (v != null) {
					//v.setEnabled(true);
					lv.setItemChecked(i, false);
				}
				mClickedDialogEntryIndices[i] = false;
			}
//	        lv.setItemChecked(i, val);
//	        mClickedDialogEntryIndices[i] = val;
	    }
    }

    public String[] parseStoredValue(CharSequence val) {
		if (val == null || "".equals(val) ) {
			return null;
		}
		else {
			return ((String)val).split(separator);
		}
    }

    @Override
	public String getValue() {
	    return super.getValue();
    }

    @Override
	public void setValue(String value) {
		super.setValue(value);
	}

	public void restoreCheckedEntries() {
    	CharSequence[] entryValues = getEntryValues();

    	// Explode the string read in sharedpreferences
    	String[] vals = parseStoredValue(getValue());

    	if ( vals != null ) {
    		List<String> valuesList = Arrays.asList(vals);
//        	for ( int j=0; j<vals.length; j++ ) {
//    		TODO: Check why the trimming... Can there be some random spaces added somehow? What if we want a value with trailing spaces, is that an issue?
//        		String val = vals[j].trim();
        	for ( int i=0; i<entryValues.length; i++ ) {
        		CharSequence entry = entryValues[i];
            	if ( valuesList.contains(entry) ) {
            		if (checkAllKey.equals(entry)) {
            			checkAll();
            			break;
            		}
        			mClickedDialogEntryIndices[i] = true;
        		}
        	}
//        	}
    	}
    }

	@Override
    protected void onDialogClosed(boolean positiveResult) {
//        super.onDialogClosed(positiveResult);
		ArrayList<String> values = new ArrayList<String>();

    	CharSequence[] entryValues = getEntryValues();
        if (positiveResult && entryValues != null) {
        	for ( int i=0; i<entryValues.length; i++ ) {
        		if (mClickedDialogEntryIndices[i]) {
        			String val = (String) entryValues[i];
        			// Don't save the state of check all option - if any
        			//if( checkAllKey == null || (val.equals(checkAllKey) == false) ) {
        			if (checkAllKey != null
        					&& val.equals(checkAllKey)) {
        				values = new ArrayList<String>(1);
        				values.add(val);
        				break;
        			}
        			values.add(val);
        			//}
        		}
        	}

            if (callChangeListener(values)) {
        		setValue(join(values, separator));
            }
        }
    }

	// Credits to kurellajunior on this post http://snippets.dzone.com/posts/show/91
	public static String join( Iterable< ? extends Object > pColl, String separator )
    {
        Iterator< ? extends Object > oIter;
        if ( pColl == null || ( !( oIter = pColl.iterator() ).hasNext() ) )
            return "";
        StringBuilder oBuilder = new StringBuilder( String.valueOf( oIter.next() ) );
        while ( oIter.hasNext() )
            oBuilder.append( separator ).append( oIter.next() );
        return oBuilder.toString();
    }

	// TODO: Would like to keep this static but separator then needs to be put in by hand or use default separator "OV=I=XseparatorX=I=VO"...
	/**
	 *
	 * @param straw String to be found
	 * @param haystack Raw string that can be read direct from preferences
	 * @param separator Separator string. If null, static default separator will be used
	 * @return boolean True if the straw was found in the haystack
	 */
	public static boolean contains( String straw, String haystack, String separator ){
		if( separator == null ) {
			separator = DEFAULT_SEPARATOR;
		}
		String[] vals = haystack.split(separator);
		for( int i=0; i<vals.length; i++){
			if(vals[i].equals(straw)){
				return true;
			}
		}
		return false;
	}
}
