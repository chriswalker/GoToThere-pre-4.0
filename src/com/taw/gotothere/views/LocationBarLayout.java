/*
 * Copyright 2011 That Amazing Web Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.taw.gotothere.views;

import android.content.Context;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.RelativeLayout;

import com.taw.gotothere.R;

/**
 * 
 * @author Chris
 */
public class LocationBarLayout extends RelativeLayout {

	/** Logging tag. */
	private static final String TAG = "LocationBarLayout";
	
	/** Text input for address. */
	private AutoCompleteTextView addressTextView;
	
	/** Animation to apply to layout, when displaying. */
	private Animation anim;
	
	// TEMP
    private static final String[] LOCATIONS = new String[] {
        "Woking Station", "Spectrum", "Old Woking", "Old Gipper", "London"
    };
	
	public LocationBarLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public LocationBarLayout(Context context) {
		super(context);
		
		LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		li.inflate(R.layout.location_bar, this, true);
		
		addressTextView = (AutoCompleteTextView) findViewById(R.id.location);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
				R.layout.autocomplete_item, LOCATIONS);
		addressTextView.setAdapter(adapter);
		
		anim = AnimationUtils.loadAnimation(context, R.anim.location_bar_anim);
	}

	
	
	/* (non-Javadoc)
	 * @see android.widget.RelativeLayout#onLayout(boolean, int, int, int, int)
	 */
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		// TODO Auto-generated method stub
		super.onLayout(changed, l, t, r, b);
		
		startAnimation(anim);
	}

	/**
	 * Registers the supplied watcher with the AutoCompleteTextView.
	 * @param watcher The TextWatcher to register
	 */	
	public void registerTextWatcher(TextWatcher watcher) {
		addressTextView.addTextChangedListener(watcher);
	}
	
	/**
	 * Remove the registered text watcher from the autocompletetextview
	 */
	public void unregisterTextWatcher(TextWatcher watcher) {
		addressTextView.removeTextChangedListener(watcher);
	}
	
	/**
	 * Return the content of the AutoCompleteTextView to the calling method.
	 * @return String content of the view
	 */
	public String getAddress() {
		return (addressTextView.getText().length() > 0) ? addressTextView.getText().toString() : null;
	}
	
	/**
	 * Clear the address.
	 */
	 public void clearAddress() {
		 addressTextView.getText().clear();
	 }
}
