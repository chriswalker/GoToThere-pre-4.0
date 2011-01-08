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

package com.taw.gotothere;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.taw.gotothere.maps.NavigationOverlay;
import com.taw.gotothere.model.Leg;
import com.taw.gotothere.model.MapDirections;
import com.taw.gotothere.model.Step;
import com.taw.gotothere.views.BearingButtonsLayout;


/**
 * Main activity for navigation actions over the map.
 * 
 * @author Chris
 */
public class GoToThereActivity extends MapActivity {

	/** Logging tag. */
	private static final String TAG = "GoToThereActivity";
		
	// Map stuff

	/** The map view. */
	private MapView map;
	/** Navigation overlay, used when we're actually navigating to a point. */
	private NavigationOverlay navigationOverlay = null;
	
	// Views
	
	/** Location button. */
	private ImageView locationImageView;
	/** Bearing button. */
	private ImageView markerImageView;
	/** Bearing button layout. */
	private BearingButtonsLayout bearingButtonLayout;
	
	// Keys for saving/restoring instance state
	
	/** End point Latitude  key. */
	public static final String END_LAT_KEY = "end_lat";
	/** End point Longitude preference key. */
	public static final String END_LONG_KEY = "end_lng";
	/** Start point Latitude  key. */
	public static final String START_LAT_KEY = "start_lat";
	/** Start point Longitude preference key. */
	public static final String START_LONG_KEY = "start_lng";
	/** Whether we're navigating. */
	public static final String NAVIGATING_KEY = "navigating";
	/** Whether we're placing a marker. */
	public static final String PLACING_MARKER_KEY = "placingMarker";
	/** Whether the compass inset is displayed. */
	public static final String DISPLAY_COMPASS_KEY = "displayCompass";
	/** Directions, retrieved from the API. */
	public static final String DIRECTIONS_KEY = "directions";
		
	// Map type array indices
	
	/** Map type: map. */
	public static final int MAP_TYPE_MAP = 0;
	/** Map type: satellite. */
	public static final int MAP_TYPE_SATELLITE = 1;
	
	// Misc
	
	/** Whether we're in the middle of placing a bearing marker. */
	private boolean placingMarker = false;
	/** Whether we're navigating to a point. */
	private boolean navigating = false;

	/** AsyncTask to retrieve directions. */
	private DirectionsTask directionsTask;
	
	/** Shared preference, indicating whether user has accepted the 'terms'. */
	private String ACCEPTED_TOC = "ACCEPTED_TOC";
	
// Overrides
	
	// Activity overrides
	
	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        startupChecks();
        
        initMapView();
        
        locationImageView = (ImageView) findViewById(R.id.location_button);
        markerImageView = (ImageView) findViewById(R.id.marker_button);
    }
    
	/* (non-Javadoc)
	 * @see com.google.android.maps.MapActivity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
//		Log.d(TAG, "onPause()");
		if (navigationOverlay.isMyLocationEnabled()) {
			navigationOverlay.disableMyLocation();
		}
		if (navigationOverlay.isCompassEnabled()) {
			navigationOverlay.disableCompass();
		}
	}

	/* (non-Javadoc)
	 * @see com.google.android.maps.MapActivity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
//		Log.d(TAG, "onResume()");
		navigationOverlay.enableMyLocation();
		
		// If coming back in from the home screen, onRestoreInstanceState() not
		// invoked, so compass won't be switched on if required
		if (locationImageView.isSelected() && !navigationOverlay.isCompassEnabled()) {
			navigationOverlay.enableCompass();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onRestoreInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {		
		// Let superclass handle map type/centre point and zoom level
		super.onRestoreInstanceState(savedInstanceState);
		
//		Log.d(TAG, "onRestoreInstanceState()");
		
		int latitude = savedInstanceState.getInt(END_LAT_KEY, -1);
		int longitude = savedInstanceState.getInt(END_LONG_KEY, -1);

		if (latitude != -1 && longitude != -1) {
			navigationOverlay.setSelectedLocation(new GeoPoint(latitude, longitude));
		}

		latitude = savedInstanceState.getInt(START_LAT_KEY, -1);
		longitude = savedInstanceState.getInt(START_LONG_KEY, -1);

		if (latitude != -1 && longitude != -1) {
			navigationOverlay.setStartLocation(new GeoPoint(latitude, longitude));
		}
		
		placingMarker = savedInstanceState.getBoolean(PLACING_MARKER_KEY);
		if (placingMarker) {
			startMarkerPlacement();
		}
		
		navigating = savedInstanceState.getBoolean(NAVIGATING_KEY);
		if (navigating) {
			navigationOverlay.setDirections((MapDirections) savedInstanceState.getSerializable(DIRECTIONS_KEY));
			markerImageView.setSelected(!markerImageView.isSelected());
		}
		
		if (savedInstanceState.getBoolean(DISPLAY_COMPASS_KEY)) {
			navigationOverlay.enableCompass();
			locationImageView.setSelected(!locationImageView.isSelected());
		}
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// Superclass handles map type, centre point of map and zoom level
		super.onSaveInstanceState(outState);
		
//		Log.d(TAG, "onSaveInstanceState()");
		
		GeoPoint pt = null;
		
		pt = navigationOverlay.getSelectedLocation();
		if (pt != null) {
			outState.putInt(END_LAT_KEY, pt.getLatitudeE6());
			outState.putInt(END_LONG_KEY, pt.getLongitudeE6());
		}

		pt = navigationOverlay.getStartLocation();
		if (pt != null) {
			outState.putInt(START_LAT_KEY, pt.getLatitudeE6());
			outState.putInt(START_LONG_KEY, pt.getLongitudeE6());
		}
		
		outState.putBoolean(DISPLAY_COMPASS_KEY, navigationOverlay.isCompassEnabled());
		
		// Whether we're placing a marker to navigate to
		outState.putBoolean(PLACING_MARKER_KEY, placingMarker);
		
		// Whether we're navigating to a point
		outState.putBoolean(NAVIGATING_KEY, navigating);
		if (navigating) {
			outState.putSerializable(DIRECTIONS_KEY, navigationOverlay.getDirections());
		}
	}
	
	/* 
	 * (non-Javadoc)
	 * @see com.google.android.maps.MapActivity#isRouteDisplayed()
	 */
	@Override
	protected boolean isRouteDisplayed() {
		return navigating;
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater mi = getMenuInflater();
		mi.inflate(R.menu.menu, menu);
		return true;
	}

	
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		
		MenuItem refresh = menu.findItem(R.id.refresh);
		if (!navigating) {
			// Disable the refresh menu item initially; it's re-enabled when the
			// user's navigating
			refresh.setEnabled(false);
		} else {
			refresh.setEnabled(true);
		}
		
		return true;
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.map_type:
			displayMapTypeOptions();
			return true;
		case R.id.refresh:
			if (navigating) {
				getDirections();
				return true;
			}
			break;
		}
		
		return false;
	}
	
	// onClick handlers
	
	/**
	 * Handle clicking the marker actionbar button.
	 * 
	 * @param v View that received the click
	 */
	public void onMarkerClick(View v) {
		if (placingMarker) {
			if (navigationOverlay.getSelectedLocation() == null) {
				displayCancelMarkerDialog();
			} else {
				stopMarkerPlacement();
				startNavigation();
				markerImageView.setSelected(!markerImageView.isSelected());
			}
		} else {
			if (!navigating) {
				// Not navigating, so need to place a marker
				startMarkerPlacement();
			} else {
				displayCancelNavigationDialog();
			}
		}
	}

	/**
	 * Handle clicking the location actionbar button.
	 * 
	 * @param v View that received the click
	 */
	public void onCompassClick(View v) {
		if (navigationOverlay.isCompassEnabled()) {
			navigationOverlay.disableCompass();
		} else {
			navigationOverlay.enableCompass();
		}
		
		locationImageView.setSelected(!locationImageView.isSelected());
	}
	
	/**
	 * Handle clicking the Done button.
	 * 
	 * @param v View that received the click
	 */
	public void onDoneButtonClick(View v) {
		if (navigationOverlay.getSelectedLocation() == null) {
			displayCancelMarkerDialog();
		} else {
			startNavigation();
			stopMarkerPlacement(); 
		}
	}
	
	/**
	 * Handle clicking the Cancel button.
	 * 
	 * @param v View that received the click
	 */
	public void onCancelButtonClick(View v) {
		displayCancelMarkerDialog();
	}
	
// Private methods
    
	/**
	 * Check if this is the first time the app has run; if so, display the first-run
	 * dialog, otherwise check if the GPS is on and offer to display the location
	 * settings activity if not.
	 */
	private void startupChecks() {
		if (!getPreferences(MODE_PRIVATE).getBoolean(ACCEPTED_TOC, false)) {
			displayFirstRunDialog();
		} 
//		else {
//	        if (!isGPSOn()) {
//	        	displayLocationSettingsDialog();
//	    	}
//		}
	}
	
	/** 
	 * Initialize the map view.
	 */
	private void initMapView() { 
		map = (MapView) findViewById(R.id.map);

		map.setBuiltInZoomControls(true);
		
		navigationOverlay = new NavigationOverlay(this, map);
		map.getOverlays().add(navigationOverlay);
		navigationOverlay.enableMyLocation();
		navigationOverlay.runOnFirstFix(new Runnable() {
			public void run() {
				map.getController().setZoom(17); 
				map.getController().animateTo(navigationOverlay.getMyLocation());
			}
		});
		
		// Set the map centre. Use the last known GPS location if there is one,
		// otherwise centre on UK
		LocationManager locationManager = 
    		(LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
		Location last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		
		GeoPoint pt = null;
		if (last != null) {
			pt = new GeoPoint((int) (last.getLatitude() * 1E6), (int) (last.getLongitude() * 1E6));
			map.getController().setZoom(17);
		} else {
			// Centre on UK for moment
			pt = new GeoPoint((int) (53.354203 * 1E6), (int) (-2.117468 * 1E6));
			map.getController().setZoom(7);
		}
		
		map.getController().setCenter(pt);	
	}
	
	/**
	 * Displays the first-run dialog.
	 */
	private void displayFirstRunDialog() {
		
		final SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		
		new AlertDialog.Builder(this)
		.setTitle(getResources().getString(R.string.first_run_dialog_title))
		.setMessage(getResources().getString(R.string.first_run_text))
		.setPositiveButton(getResources().getString(R.string.accept_button_label), 
			new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	        	   prefs.edit().putBoolean(ACCEPTED_TOC, true).commit();
	           }
	       })
	    .setNegativeButton(getResources().getString(R.string.dont_accept_button_label), 
	    	new DialogInterface.OnClickListener() {
	    		public void onClick(DialogInterface dialog, int id) {
	    			dialog.cancel();
	    			finish();
	    		}
	       })
		.show();
	}
	
	/**
	 * Display a warning dialog asking the user if they are sure they want to
	 * quit, since configuration is incomplete.
	 */
	private void displayCancelMarkerDialog() {
		new AlertDialog.Builder(this)
		.setTitle(getResources().getString(R.string.marker_dialog_title))
		.setMessage(getResources().getString(R.string.marker_dialog_text))
		.setPositiveButton(getResources().getString(R.string.yes_button_label), 
			new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	        	   stopMarkerPlacement();
	        	   markerImageView.setSelected(!markerImageView.isSelected());
	           }
	       })
	    .setNegativeButton(getResources().getString(R.string.no_button_label), 
	    	new DialogInterface.OnClickListener() {
	    		public void onClick(DialogInterface dialog, int id) {
	    			dialog.cancel();
	    		}
	       })
		.show();
	}

	/**
	 * Display a warning dialog asking the user if they are sure they want to
	 * cancel navigation.
	 */
	private void displayCancelNavigationDialog() {
		new AlertDialog.Builder(this)
		.setTitle(getResources().getString(R.string.navigation_dialog_title))
		.setMessage(getResources().getString(R.string.navigation_dialog_text))
		.setPositiveButton(getResources().getString(R.string.yes_button_label), 
			new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	        	   stopNavigation();
	           }
	       })
	    .setNegativeButton(getResources().getString(R.string.no_button_label), 
	    	new DialogInterface.OnClickListener() {
	    		public void onClick(DialogInterface dialog, int id) {
	    			dialog.cancel();
	    		}
	       })
		.show();
	}
	
	/**
	 * Display a dialog asking the user if they want to access the location
	 * settings
	 */
	private void displayLocationSettingsDialog() {
		new AlertDialog.Builder(this)
		.setTitle(getResources().getString(R.string.gps_dialog_title))
		.setMessage(getResources().getString(R.string.gps_dialog_text))
		.setPositiveButton(getResources().getString(R.string.yes_button_label), 
			new DialogInterface.OnClickListener() {
	           public void onClick(DialogInterface dialog, int id) {
	        	   startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
	           }
	       })
	    .setNegativeButton(getResources().getString(R.string.no_button_label), 
	    	new DialogInterface.OnClickListener() {
	    		public void onClick(DialogInterface dialog, int id) {
	    			dialog.cancel();
	    		}
	       })
		.show();
	}
	
	/**
	 * Start marker placement.
	 */
	private void startMarkerPlacement() {
        if (!isGPSOn()) {
        	displayLocationSettingsDialog();
    	} else {
			placingMarker = true;
			navigationOverlay.setPlacingMarker(placingMarker);
			
			Toast.makeText(this, R.string.place_marker_text, Toast.LENGTH_LONG).show();
			
			// Add buttonbar to layout.
			addBearingButtonsView();
			
			markerImageView.setSelected(!markerImageView.isSelected());
    	}
	}
	
	/**
	 * Stop marker placement.
	 */
	private void stopMarkerPlacement() {
		placingMarker = false;
		navigationOverlay.setPlacingMarker(placingMarker);
		
 	   	removeAddBearingButtonView();
	}	
	
	/**
	 * Start the DirectionsTask to get directions. We check that there is a 
	 * starting point available (i.e. we have a GPS fix), otherwise we'll
	 * wait until we have one.
	 */
	private void startNavigation() {
		navigating = true;
		navigationOverlay.setNavigating(navigating);
		
		if (navigationOverlay.getMyLocation() != null) {
			getDirections();
		} else {
			Toast.makeText(this, R.string.waiting_text, Toast.LENGTH_LONG).show();

			navigationOverlay.runOnFirstFix(new Runnable() {
				public void run() {
					// getDirections() has to be called from the UI thread
					runOnUiThread(new Runnable() {
						public void run() {
							getDirections();
						}
					});
				}
			});
		}
	}
	
	/**
	 * Start a thread to retrieve the directions from the user's current location
	 * to their selected point. The thread will update the navigationOverlay
	 * once it has directions.
	 */
	private void getDirections() {		
		if (directionsTask == null || directionsTask.getStatus().equals(AsyncTask.Status.FINISHED)) {
			directionsTask = new DirectionsTask(getBaseContext(), 
					navigationOverlay.getMyLocation(), 
					navigationOverlay.getSelectedLocation());
			directionsTask.execute();
		}		
	}
	
	/**
	 * Stop the navigation process. Remove the overlays from the map, remove
	 * any stored locations, and reset flags.
	 */
	private void stopNavigation() {
		navigating = false;
		navigationOverlay.reset();
				
		map.invalidate();
		
		markerImageView.setSelected(!markerImageView.isSelected());
	}
		
	/**
	 * Add the bearingbuttons to the map view.
	 */
	private void addBearingButtonsView() {
		if (bearingButtonLayout == null) {
			bearingButtonLayout = new BearingButtonsLayout(this);
		}
				
		MapView.LayoutParams lp = new MapView.LayoutParams(MapView.LayoutParams.FILL_PARENT, 
				MapView.LayoutParams.WRAP_CONTENT, 
				0, 0, 
				MapView.LayoutParams.TOP);
		map.addView(bearingButtonLayout, lp);
	}
	
	/**
	 * Remove the bearing buttons from the map view.
	 */
	private void removeAddBearingButtonView() {
		map.removeView(bearingButtonLayout);
	}
	
	/** 
	 * Displays the Map Types dialog box - we do it this way so we can
	 * set checkmarks against selected options (we can't do this when
	 * specifying the menu via XML)
	 */
	private void displayMapTypeOptions() {
		
		int checked = -1;
		if (!map.isSatellite()) {
			checked = MAP_TYPE_MAP;
		} else if (map.isSatellite()) {
			checked = MAP_TYPE_SATELLITE;
		}
		
		new AlertDialog.Builder(this)
			.setTitle(R.string.map_type_title)
			.setSingleChoiceItems(R.array.map_type, checked,
					new DialogInterface.OnClickListener() {						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							toggleMapType(which);
							dialog.dismiss();
						}
					}).show();
	}
	
	/** 
	 * Change the map type depending on selected map type option.
	 * 
	 * @param type index into map type array
	 */
	private void toggleMapType(int type) {
		switch (type) {
		case MAP_TYPE_MAP:
			map.setSatellite(false);
			break;
		case MAP_TYPE_SATELLITE:
			map.setSatellite(true);
			break;
		}
	}
	
	/**
	 *  Check if the GPS sensor is switched on.
	 *  
	 *  @return true if on, false otherwise
	 */
	private boolean isGPSOn() {
		LocationManager locationManager = (LocationManager) getSystemService(Service.LOCATION_SERVICE);
		return (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) ? true : false;
    }
	
// Inner AsyncTask
	
	/**
	 * Retrieves directions from the supplied origin and destination points via
	 * Google's Directions API.
	 * 
	 * @author Chris
	 */
	public class DirectionsTask extends AsyncTask<Void, Integer, MapDirections> {

		/** Logging tag. */
		private static final String TAG = "DirectionsTask";
		
		/** Origin geo point. */
		private GeoPoint origin;
		/** Destination geo point. */
		private GeoPoint destination;
		
		/** HttpClient. */
		private HttpClient httpClient;
		
		public DirectionsTask(Context ctx, GeoPoint origin, GeoPoint destination) {
			super();

			this.origin = origin;
			this.destination = destination;
			
			httpClient = new DefaultHttpClient();
			
			HttpParams params = new BasicHttpParams();
	        HttpConnectionParams.setConnectionTimeout(params, 20 * 1000);
	        HttpConnectionParams.setSoTimeout(params, 20 * 1000);

	        HttpConnectionParams.setSocketBufferSize(params, 8192);
		}

	// Overrides
		
		@Override
		protected MapDirections doInBackground(Void... params) {
			MapDirections directions = null;
		
			List<NameValuePair> httpParams = new ArrayList<NameValuePair>();
			httpParams.add(new BasicNameValuePair("sensor", "true"));
			httpParams.add(new BasicNameValuePair("mode", "walking"));
			
			StringBuffer buf = new StringBuffer();
			buf.append(origin.getLatitudeE6() / 1e6);
			buf.append(",");
			buf.append(origin.getLongitudeE6() / 1e6);
			httpParams.add(new BasicNameValuePair("origin", buf.toString()));
			
			buf = new StringBuffer();
			buf.append(destination.getLatitudeE6() / 1e6);
			buf.append(",");
			buf.append(destination.getLongitudeE6() / 1e6);
			httpParams.add(new BasicNameValuePair("destination", buf.toString()));
			
			try {
				directions = execute(httpParams);
			} catch (IOException ioe) {
				Log.e(TAG, "Could not retrieve directions!");
				// Null directions handled in onPostExecute(), so just carry on
				// here
			}
			
			return directions;
		}

		/* (non-Javadoc)
		 * @see android.os.AsyncTask#onPreExecute()
		 */
		@Override
		protected void onPreExecute() {
			findViewById(R.id.progress).setVisibility(View.VISIBLE);
			navigationOverlay.setStartLocation(origin);
		}

		/*
		 *  (non-Javadoc)
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(MapDirections directions) {
			if (directions != null && directions.getError() < 0) {
				navigationOverlay.setDirections(directions);	        	
		        map.invalidate();
			} else {
				Toast.makeText(getBaseContext(), directions.getError(), Toast.LENGTH_SHORT).show();
			}

	        findViewById(R.id.progress).setVisibility(View.GONE);
		}
		
	// Private methods
		
		/**
		 * Sends the Http request to the designated server.
		 */
		private MapDirections execute(List<NameValuePair> params) throws IOException {
			MapDirections directions = new MapDirections();
			
			HttpGet get = null;
			HttpEntity entity = null;
			try {
				String encodedParams = URLEncodedUtils.format(params, "UTF-8");
				
				URI uri = URIUtils.createURI("http", "maps.googleapis.com", -1, 
						"maps/api/directions/json", encodedParams, null);			
				get = new HttpGet(uri);				
				HttpResponse rsp = httpClient.execute(get);
				
				switch (rsp.getStatusLine().getStatusCode()) {
				case HttpStatus.SC_OK:
					entity = rsp.getEntity();
					if (entity != null) {
						String json = EntityUtils.toString(entity);
						parse(json, directions);
					} 
					break;
				default:
					directions.setError(R.string.general_error_text);
				}
			} catch (JSONException jsone){
				Log.e(TAG, "Problem parsing directions!", jsone);
				directions.setError(R.string.json_error_text);
			} catch (Exception e) {
				get.abort();
				Log.e(TAG, "Problem getting directions!", e);
				directions.setError(R.string.general_error_text);
			} finally {
	            if (entity != null) {
	                entity.consumeContent();
	            }
			}	
			
			return directions;
		}
		
		/**
		 * Parse the response and return a complete MapDirections object.
		 * 
		 * @param jsonStr String containing response JSON sent by the Directions API
		 * @params directions a MapDirections object to populate
		 */
		private void parse(String jsonStr, MapDirections directions) throws JSONException {
			
			JSONObject json = new JSONObject(jsonStr);

			// Only get one route back
			JSONObject jsonRoute = json.getJSONArray("routes").getJSONObject(0);
			
			JSONArray jsonWarnings = jsonRoute.getJSONArray("warnings");
			List<String> warnings = new ArrayList<String>();
			for (int i = 0, j = jsonWarnings.length(); i < j; i ++) {
				warnings.add(jsonWarnings.getString(i));
			}
			directions.setWarning(warnings);
			
			directions.setSummary(jsonRoute.getString("summary"));
			
			// Loop through Legs (should only be one)
			JSONArray jsonLegs = jsonRoute.getJSONArray("legs");
			Leg leg = null;
			for (int i = 0, j = jsonLegs.length(); i < j; i ++) {
				JSONObject jsonLeg = jsonLegs.getJSONObject(i);
				leg = new Leg();
				
				Step step = null;
				JSONArray jsonSteps = jsonLeg.getJSONArray("steps");
				for (int k = 0, l = jsonSteps.length(); k < l; k ++) {
					JSONObject jsonStep = jsonSteps.getJSONObject(k);
					step = new Step();
					
					JSONObject location = jsonStep.getJSONObject("start_location");
					step.setStartLocation((int) (location.getDouble("lat") * 1e6), 
							(int) (location.getDouble("lng") * 1e6));
					
					location = jsonStep.getJSONObject("end_location");
					step.setEndLocation((int) (location.getDouble("lat") * 1e6), 
							(int) (location.getDouble("lng") * 1e6));
					
					step.setTravelMode(jsonStep.getString("travel_mode"));
					
					step.setDistance(jsonStep.getJSONObject("distance").getInt("value"));
					step.setDistanceText(jsonStep.getJSONObject("distance").getString("text"));
					
					step.setDuration(jsonStep.getJSONObject("duration").getInt("value"));
					step.setDurationText(jsonStep.getJSONObject("duration").getString("text"));

					step.setHtmlInstructions(jsonStep.getString("html_instructions"));
					
					step.setPolyLine(jsonStep.getJSONObject("polyline").getString("points"));
					
					leg.addStep(step);
				}
				
				directions.setStartAddress(jsonLeg.getString("start_address"));
				directions.setEndAddress(jsonLeg.getString("end_address"));
				
				directions.addLeg(leg);
			}
		}
	}

}