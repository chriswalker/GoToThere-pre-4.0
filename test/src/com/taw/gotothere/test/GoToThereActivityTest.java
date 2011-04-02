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

import android.app.Instrumentation;
import android.test.ActivityInstrumentationTestCase2;
import android.test.UiThreadTest;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.taw.gotothere.GoToThereActivity;
import com.taw.gotothere.maps.NavigationOverlay;

/**
 * Tests the main GoToThereActivity. We're mainly interested in UI related behaviour
 * here, so tests related to UI and state are emphasised over app logic.
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
	/** Overlay used in map. */
	private NavigationOverlay overlay;
	
	// GeoPoints representing various locations
	
	/** User's location. */
	private GeoPoint myLocation;
	/** Where user taps. */
	private GeoPoint destination;
	
	/** Activity instrumentation. */
	private Instrumentation instr;
	
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
		
		// NavigationOverlay only one in overlays list
		overlay = (NavigationOverlay) map.getOverlays().get(0);
		
		myLocation = new GeoPoint((int) (-0.551796 * 1e6), (int) (51.306412 * 1e6));
		destination = new GeoPoint((int) (-0.556951 * 1e6), (int) (51.3185 * 1e6));
		
		instr = this.getInstrumentation();
	}
	
	/**
	 * Test initial activity state.
	 */
	public void testStartupConditions() {
		assertFalse(directionsImageView.isSelected());
		assertFalse(directionsImageView.isEnabled());							// Directions btn initially disabled
		assertFalse(markerImageView.isSelected());
		assert(searchTextView.getText().length() == 0);
		
	}
	
	// Marker functionality
	
	/**
	 * Test adding a marker.
	 */
	@UiThreadTest
	public void testAddAmarker() {
//		activity.runOnUiThread(new Runnable() {
//			public void run() {
//				markerImageView.requestFocus();
//				markerImageView.setSelected(true);
//				
//				map.requestFocus();
//				map.dispatchTouchEvent(new MotionEvent());
				
				// Click the marker actionbar button
				activity.onMarkerClick(markerImageView);
				assertTrue(markerImageView.isSelected());
				assertNull(overlay.getSelectedLocation());
				
				// Now tap on the map somewhere
				overlay.onTap(destination, map);
				assertNotNull(overlay.getSelectedLocation());
				assertTrue(directionsImageView.isEnabled());
//			}
//		});
	}
	
	/**
	 * Starting via ACTION_SEARCH intent from the quick search box.
	 */
	public void testStartBySearchIntent() {
		
	}
	
	/**
	 * Test UI state when switching between a marker set by a location query
	 * and one set by tapping on the map.
	 */

	
	
	// State saving on activity pause/finish
	
	/**
	 * Test MapView UI state maintained on activity restarts.
	 */
	@UiThreadTest
	public void testMapUIStateSaved() {
		int zoom = map.getZoomLevel();
		GeoPoint centre = map.getMapCenter();

		// Pause and resume the activity
		instr.callActivityOnPause(activity);
		instr.callActivityOnResume(activity);
		
		assertEquals(zoom, map.getZoomLevel());
		assertEquals(centre.getLatitudeE6(), map.getMapCenter().getLatitudeE6());
		assertEquals(centre.getLongitudeE6(), map.getMapCenter().getLongitudeE6());
		
		// Stop activity completely and start again
		activity.finish();
		activity = getActivity();
		
		assertEquals(zoom, map.getZoomLevel());
		assertEquals(centre.getLatitudeE6(), map.getMapCenter().getLatitudeE6());
		assertEquals(centre.getLongitudeE6(), map.getMapCenter().getLongitudeE6());
		
	}
	
	/**
	 * Test actionbar state maintained on activity restarts
	 */
	@UiThreadTest
	public void testActionbarUIStateSaved() {
		
	}
}
