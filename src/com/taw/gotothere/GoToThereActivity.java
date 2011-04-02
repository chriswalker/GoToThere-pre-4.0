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
import java.util.Locale;

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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapView;
import com.taw.gotothere.maps.NavigationOverlay;
import com.taw.gotothere.model.Leg;
import com.taw.gotothere.model.MapDirections;
import com.taw.gotothere.model.Step;
import com.taw.gotothere.provider.GoToThereSuggestionProvider;


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

	/** Directions button. */
	private ImageView directionsImageView;
	/** Bearing button. */
	private ImageView markerImageView;
	/** Location/search edit text. */
	private TextView searchTextView;
	
	// Keys for saving/restoring instance state
	
	/** End point Latitude  key. */
	private static final String END_LAT_KEY = "endLat";
	/** End point Longitude preference key. */
	private static final String END_LONG_KEY = "endLng";
	/** Start point Latitude  key. */
	private static final String START_LAT_KEY = "startLat";
	/** Start point Longitude preference key. */
	private static final String START_LONG_KEY = "startLng";
	/** Whether we're navigating. */
	private static final String NAVIGATING_KEY = "navigating";
	/** Whether we're placing a marker. */
	private static final String PLACING_MARKER_KEY = "placingMarker";
	/** Directions, retrieved from the API. */
	private static final String DIRECTIONS_KEY = "directions";
	/** Last query received via ACTION_SEARCH intent. */
	private static final String PREVIOUS_QUERY = "previousQuery";
	
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
	private static final String ACCEPTED_TOC = "ACCEPTED_TOC";
	
	/** Progress dialog, kicked off in the AsyncTask. */
	private ProgressDialog progress;
	
	/**
	 * Internal broadcast receiver for dealing with broadcasts from the
	 * navigation overlay - we get notified if the user has tapped on the map,
	 * in which case we clear out the auto complete text view.
	 */
	private class NavigationOverlayReceiver extends BroadcastReceiver {

		/* (non-Javadoc)
		 * @see android.content.BroadcastReceiver#onReceive(android.content.Context, android.content.Intent)
		 */
		@Override
		public void onReceive(Context context, Intent i) {
			directionsImageView.setEnabled(true);
			searchTextView.setText(null);
			
			// Remove any saved query, as it's now superceded by the marker
			// placed by tapping the map
			SharedPreferences.Editor edit = getPreferences(Activity.MODE_PRIVATE).edit();
			edit.remove(PREVIOUS_QUERY);
		}
		
	};
	
	/** Instance of the navigation overlay receiver. */
	private NavigationOverlayReceiver receiver = null;
	
// Overrides
	
	// Activity overrides
	
	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        startupChecks();
        initMapView();

        markerImageView = (ImageView) findViewById(R.id.marker_button);
        directionsImageView = (ImageView) findViewById(R.id.directions_button);
        // Directions button initially disabled; if we're restoring state later
        // on, this may get overridden
		directionsImageView.setEnabled(false);
		
        searchTextView = (TextView) findViewById(R.id.location);
        
        progress = new ProgressDialog(this);
        progress.setMessage(getResources().getString(R.string.directions_text));
        
        registerReceiver();

        handleIntent(getIntent(), savedInstanceState);
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
		
		unregisterReceiver(receiver);
	}

	/* (non-Javadoc)
	 * @see com.google.android.maps.MapActivity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		Log.d(TAG, "onResume()");
		navigationOverlay.enableMyLocation();
		
		registerReceiver();
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onRestoreInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {		
		// Let superclass handle map type/centre point and zoom level
		super.onRestoreInstanceState(savedInstanceState);
		
		Log.d(TAG, "onRestoreInstanceState()");
		
		int latitude = savedInstanceState.getInt(END_LAT_KEY, -1);
		int longitude = savedInstanceState.getInt(END_LONG_KEY, -1);
		
		placingMarker = savedInstanceState.getBoolean(PLACING_MARKER_KEY);
		if (placingMarker) {
			startMarkerPlacement();
			
			// May have tapped an endpoint already
			if (latitude != -1 && longitude != -1) {
				navigationOverlay.setSelectedLocation(new GeoPoint(latitude, longitude));
				directionsImageView.setEnabled(true);
			} else {
				directionsImageView.setEnabled(false);
			}			
		}
		
		navigating = savedInstanceState.getBoolean(NAVIGATING_KEY);
		if (navigating) {

			navigationOverlay.startNavigating();
			
			if (latitude != -1 && longitude != -1) {
				navigationOverlay.setSelectedLocation(new GeoPoint(latitude, longitude));
			}
			
			latitude = savedInstanceState.getInt(START_LAT_KEY, -1);
			longitude = savedInstanceState.getInt(START_LONG_KEY, -1);

			if (latitude != -1 && longitude != -1) {
				navigationOverlay.setStartLocation(new GeoPoint(latitude, longitude));
			}
			
			navigationOverlay.setDirections((MapDirections) savedInstanceState.getSerializable(DIRECTIONS_KEY));
			
			directionsImageView.setSelected(true);
			directionsImageView.setEnabled(true);
			
			markerImageView.setEnabled(false);
		}
				
		// Save the previousQuery flag as a preference: if the screen is re-oriented
		// then the activity is killed + restarted, meaning we lose the 
		// savedInstanceState bundle. This is an easy way to keep track of this flag.
		SharedPreferences prefs = getPreferences(Activity.MODE_PRIVATE);
		searchTextView.setText(prefs.getString(PREVIOUS_QUERY, null));
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// Superclass handles map type, centre point of map and zoom level
		super.onSaveInstanceState(outState);
		
		Log.d(TAG, "onSaveInstanceState()");
		
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
		
		// Whether we're placing a marker to navigate to
		outState.putBoolean(PLACING_MARKER_KEY, placingMarker);
		
		// Whether we're navigating to a point
		outState.putBoolean(NAVIGATING_KEY, navigating);
		if (navigating) {
			outState.putSerializable(DIRECTIONS_KEY, navigationOverlay.getDirections());
		}
		
		SharedPreferences.Editor edit = getPreferences(Activity.MODE_PRIVATE).edit();
		edit.putString(PREVIOUS_QUERY, searchTextView.getText().toString()).commit();
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
	 * Handle clicking the directions actionbar button.
	 * 
	 * @param v View that received the click
	 */
	public void onDirectionsClick(View v) {
		if (!navigating) {
			if (placingMarker) {
				stopMarkerPlacement();
			} 

			if (isGPSOn()) {
				startNavigation();
			} else {
				displayLocationSettingsDialog();
			}
		} else {
			displayCancelNavigationDialog();
		}
	}
	
	/**
	 * Handle clicking the marker actionbar button.
	 * 
	 * @param v View that received the click
	 */
	public void onMarkerClick(View v) {
		if (!placingMarker) {
			startMarkerPlacement();
		} else {
			stopMarkerPlacement();
		}
	}
		
	/**
	 * Called when the user clicks in the edit text field; we pass through to
	 * the global search mechanism.
	 * 
	 * @param v View that received the click
	 */
	public void onSearchClicked(View v) {
		onSearchRequested();
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
		placingMarker = true;
		navigationOverlay.setPlacingMarker(placingMarker);
		
		markerImageView.setSelected(true);
	}
	
	/**
	 * Stop marker placement.
	 */
	private void stopMarkerPlacement() {
		placingMarker = false;
		navigationOverlay.setPlacingMarker(placingMarker);
		
		markerImageView.setSelected(false);
	}	
	
	/**
	 * Start the DirectionsTask to get directions. We check that there is a 
	 * starting point available (i.e. we have a GPS fix), otherwise we'll
	 * wait until we have one.
	 */
	private void startNavigation() {
		navigating = true;
		
		directionsImageView.setSelected(true);
		markerImageView.setEnabled(false);
		
		navigationOverlay.startNavigating();
		
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
		navigationOverlay.stopNavigating();
		map.invalidate();	
		
		if (searchTextView.getText().length() > 0) {
			searchTextView.setText(null);
			
		}
		
		directionsImageView.setSelected(false);
		directionsImageView.setEnabled(false);
		markerImageView.setEnabled(true);
		
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
	
	/**
	 * Register the broadcast receiver that listens for locations being set on the
	 * map.
	 */
	private void registerReceiver() {
		if (receiver == null) {
			receiver = new NavigationOverlayReceiver();
		}
		
		IntentFilter filter = new IntentFilter(NavigationOverlay.LOCATION_ON_MAP);
		registerReceiver(receiver, filter);
	}
	
	/**
	 * Geocode the location the user typed into the search box, and centre the map
	 * on it.
	 */
	private void geocodeResult(String address) {
		Log.d(TAG, "geocodeResult()");
		Geocoder geo = new Geocoder(this, Locale.getDefault());
		try {
			List<Address> addresses = geo.getFromLocationName(address, 10);			// Hmmmm, 1?
			if (addresses.size() > 0) {
				GeoPoint pt = new GeoPoint((int) (addresses.get(0).getLatitude() * 1e6),
						(int) (addresses.get(0).getLongitude() * 1e6));
				directionsImageView.setEnabled(true);
				navigationOverlay.setSelectedLocation(pt, this);
				map.getController().animateTo(pt);
				startMarkerPlacement();
				
	            searchTextView.setText(address);
			} else {
				Toast.makeText(this, R.string.error_not_found_text, Toast.LENGTH_SHORT).show();
				searchTextView.setText(null);
			}
		} catch (IOException ioe) {
			Log.e(TAG, "Could not geocode '" + address + "'", ioe);
			Toast.makeText(this, R.string.error_general_text, Toast.LENGTH_SHORT).show();
		}
	}
	
	/**
	 * Handle any intents passed into the activity. Currently we only deal with
	 * on, ACTION_SEARCH, which means we've been given a query string to search
	 * for via the quick search box. We'll also handle the case where the activity
	 * is restarted due to orientation changes; in this situation we still have the 
	 * intent with ACTION_SEARCH, so we check if we've already processed it; if so
	 * don't bother geocoding.
	 * 
	 * @param intent The intent to process
	 * @param savedInstanceState The bundle passed into the activity on (re)start
	 */
	private void handleIntent(Intent intent, Bundle savedInstanceState) {
		
        if (intent.getAction().equals(Intent.ACTION_SEARCH)) {
        	Log.d(TAG, "Started as a result of ACTION_SEARCH");
        	String query = intent.getStringExtra(SearchManager.QUERY);

        	SharedPreferences prefs = getPreferences(Activity.MODE_PRIVATE);
        	String previousQuery = prefs.getString(PREVIOUS_QUERY, null);
        	if (previousQuery == null || !previousQuery.equals(query)) {
        		Log.d(TAG, "    Haven't processed this query before");
        		SearchRecentSuggestions suggestions = 
        			new SearchRecentSuggestions(this, 
        					GoToThereSuggestionProvider.AUTHORITY, GoToThereSuggestionProvider.MODE);
	            suggestions.saveRecentQuery(query, null);
	            
	            geocodeResult(query);	            
        	} // Else UI stuff set up by onRestoreInstanceState() 
        }
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
		private GeoPoint destination = null;
		
		// Response status values from the Directions API
		
		/** OK. */
		private static final String RESP_OK = "OK";
		/** Address/location not found. */
		private static final String RESP_NOT_FOUND = "NOT_FOUND";
		/** No results found. */
		private static final String RESP_ZERO_RESULTS = "ZERO_RESULTS";
		/** Invalid request. */
		private static final String RESP_INVALID_REQUEST = "INVALID_REQUEST";
		
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
			
			String dest = null;
			if (destination != null) {
				buf = new StringBuffer();
				buf.append(destination.getLatitudeE6() / 1e6);
				buf.append(",");
				buf.append(destination.getLongitudeE6() / 1e6);
				dest = buf.toString();
			}
			httpParams.add(new BasicNameValuePair("destination", dest));
				
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
			progress.show();
			navigationOverlay.setStartLocation(origin);
		}

		/*
		 *  (non-Javadoc)
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(MapDirections directions) {
			if (directions.getError() < 0) {
				navigationOverlay.setDirections(directions);
				map.getController().animateTo(navigationOverlay.getMyLocation());
		        map.invalidate();
			} else {
				stopNavigation();
				Toast.makeText(getBaseContext(), directions.getError(), Toast.LENGTH_SHORT).show();
			}

			progress.cancel();
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
						Log.d(TAG, "Returned JSON =" + json);
						parse(json, directions);
					} 
					break;
				default:
					directions.setError(R.string.error_general_text);
				}
			} catch (JSONException jsone){
				Log.e(TAG, "Problem parsing directions!", jsone);
				directions.setError(R.string.error_json_text);
			} catch (Exception e) {
				get.abort();
				Log.e(TAG, "Problem getting directions!", e);
				directions.setError(R.string.error_general_text);
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
			
			String status = json.getString("status");
			if (status.equals(RESP_NOT_FOUND)) {
				directions.setError(R.string.error_not_found_text);
			} else if (status.equals(RESP_ZERO_RESULTS)) {
				directions.setError(R.string.error_zero_results_text);
			} else if (status.equals(RESP_INVALID_REQUEST)) {
				directions.setError(R.string.error_general_text);
			} else if (status.equals(RESP_OK)) {
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
				JSONObject location = null; 										// For retrieving locations
				for (int i = 0, j = jsonLegs.length(); i < j; i ++) {
					JSONObject jsonLeg = jsonLegs.getJSONObject(i);
					leg = new Leg();
					
					Step step = null;
					JSONArray jsonSteps = jsonLeg.getJSONArray("steps");
					for (int k = 0, l = jsonSteps.length(); k < l; k ++) {
						JSONObject jsonStep = jsonSteps.getJSONObject(k);
						step = new Step();
						
						location = jsonStep.getJSONObject("start_location");
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
					
					location = jsonLeg.getJSONObject("start_location");
					directions.setStartLocation((int) (location.getDouble("lat") * 1e6), 
								(int) (location.getDouble("lng") * 1e6));
					location = jsonLeg.getJSONObject("end_location");
					directions.setEndLocation((int) (location.getDouble("lat") * 1e6), 
								(int) (location.getDouble("lng") * 1e6));
					
					directions.addLeg(leg);
				}
			}
		}
	}

}