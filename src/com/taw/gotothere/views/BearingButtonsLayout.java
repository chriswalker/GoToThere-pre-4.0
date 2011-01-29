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
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.Button;
import android.widget.LinearLayout;

import com.taw.gotothere.R;

/**
 * 
 * @author Chris
 */
public class BearingButtonsLayout extends LinearLayout {

	private Button cancel;
	private Button done;
	
	public BearingButtonsLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public BearingButtonsLayout(Context context) {
		super(context);
		
		LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		li.inflate(R.layout.bearing_buttons, this, true);
		
		cancel = (Button) findViewById(R.id.cancel);
		//done = (Button) findViewById(R.id.done);
	}

	/**
	 * Registers the supplied listener as an OnClick listener with the buttons in
	 * this layout. It is assumed that a single listener will handle clicks for
	 * all buttons in the toolbar.
	 * @param listener The listener to register against each of the toolbar buttons
	 */
	public void registerOnClickListener(OnClickListener listener) {
		cancel.setOnClickListener(listener);
		done.setOnClickListener(listener);
	}
}
