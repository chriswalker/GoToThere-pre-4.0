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
package com.taw.gotothere.test;

import android.test.ActivityInstrumentationTestCase2;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.taw.gotothere.GoToThereActivity;

/**
 * Tests the main GoToThereActivity.
 * 
 * @author Chris
 */
public class GoToThereActivityTest extends ActivityInstrumentationTestCase2<GoToThereActivity> {

	/** Activity under test. */
	private GoToThereActivity activity;
	
	// UI elements under test
	
	/** Directions image view. */
	private ImageView directionsImageView;
	/** Marker image view. */
	private ImageView markerImageView;
	/** Search TextView. */
	private TextView searchTextView;
	/** MapView within activity. */
	private MapView map;
	
	
	// GeoPoints representing various locations
	
	/** User's location. */
	private GeoPoint myLocation;
	/** Where user taps. */
	private GeoPoint destination;
	
	public GoToThereActivityTest() {
		super("com.taw.gotothere", GoToThereActivity.class);
	}
	
	protected void setUp() throws Exception {
		super.setUp();
		
		setActivityInitialTouchMode(false);
		
		activity = getActivity();
		
		directionsImageView = 
			(ImageView) activity.findViewById(com.taw.gotothere.R.id.directions_button);
		markerImageView = 
			(ImageView) activity.findViewById(com.taw.gotothere.R.id.marker_button);
		searchTextView = 
			(TextView) activity.findViewById(com.taw.gotothere.R.id.location);
		
		map = (MapView) activity.findViewById(com.taw.gotothere.R.id.map);
		
		myLocation = new GeoPoint((int) (-0.551796 * 1e6), (int) (51.306412 * 1e6));
		
	}
	
	/**
	 * Test initial activity state.
	 */
	public void testPreConditions() {
		assertFalse(directionsImageView.isSelected());
		assertFalse(directionsImageView.isEnabled());							// Directions btn initially disabled
		assertFalse(markerImageView.isSelected());
		assert(searchTextView.getText().length() == 0);
		
	}
	
	// Marker functionality
	
	/**
	 * Test adding a marker.
	 */
//	public void testAddAmarker() {
//		activity.runOnUiThread(new Runnable() {
//			public void run() {
//				markerImageView.requestFocus();
//				markerImageView.setSelected(true);
//				
//				map.requestFocus();
//				map.dispatchTouchEvent(new MotionEvent());
//			}
//		});
//	}
	
	/**
	 * Starting via ACTION_SEARCH intent from the quick search box.
	 */

	/**
	 * Test UI state when switching between a marker set by a location query
	 * and one set by tapping on the map.
	 */

	
	// Orientation change tests
	
	/**
	 * Test MapView UI state maintained on device reorientation.
	 */
	public void testMapUI() {
		int zoom = map.getZoomLevel();
		GeoPoint centre = map.getMapCenter();

		// Change orientation
		
		
		assertEquals(zoom, map.getZoomLevel());
		assertEquals(centre.getLatitudeE6(), map.getMapCenter().getLatitudeE6());
		assertEquals(centre.getLongitudeE6(), map.getMapCenter().getLongitudeE6());
	}
	
	
}
