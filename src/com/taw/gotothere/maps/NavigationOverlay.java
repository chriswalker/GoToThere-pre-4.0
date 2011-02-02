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

package com.taw.gotothere.maps;

import java.util.List;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Projection;
import com.taw.gotothere.R;
import com.taw.gotothere.model.MapDirections;
import com.taw.gotothere.model.Step;


/**
 * Provide own subclass of MyLocationOverlay, which also handles placing
 * destination markers on the map.
 * 
 * @author Chris
 */
public class NavigationOverlay extends MyLocationOverlay {

	/** Logging tag. */
	private static final String TAG = "NavigationOverlay";
	
	/** Whether the user is placing a marker. */
	private boolean placingMarker = false;
	/** Whether the user is navigating to a point. */
	private boolean navigating = false;
	
	/** Point user is navigating to. */
	private GeoPoint selectedLocation;
	/** Point user is starting from, updated when they decide to request directions. */
	private GeoPoint startLocation;
	
	/** Intent action string. Used when notifying activity we have a location on the map. */
	public static final String LOCATION_ON_MAP = 
		"com.taw.gotothere.intent.action.LOCATION_ON_MAP";
	
	/** Start point on the canvas. */
	private Point startPoint;
	/** End point on the canvas. */
	private Point endPoint;
	
	/** Directions from the start point to end point. */
	private MapDirections directions;
		
	/** Inner radius. */
	private int radius;

	/** High-density radius. */
	protected static final int HIGH_DENSITY_RADIUS = 15;
	/** Medium-density radius. */
	protected static final int MED_DENSITY_RADIUS = 10;
	/** Low-density radius. */
	protected static final int LOW_DENSITY_RADIUS = 5;

	/** Point offset when calculating user taps. */
	private static final int POINT_OFFSET = 10;
	
	// Paint objects
	
	/** Navigation line paint. */
	private Paint linePaint;
	/** Gradient line paint. */
	private Paint gradientLinePaint;
	
	/** Outer circle paint. */
	protected Paint outer = null;
	/** End point Inner circle fill paint. */
	protected Paint startInnerFill = null;
	/** End point Inner circle fill paint. */
	protected Paint endInnerFill = null;
	
	public NavigationOverlay(Context context, MapView mapView) {
		super(context, mapView);
		init(context);
	}
	
	public NavigationOverlay(Context context, MapView mapView, GeoPoint pt) {
		super(context, mapView);
		this.selectedLocation = pt;
		init(context);
	}

	/* (non-Javadoc)
	 * @see com.google.android.maps.MyLocationOverlay#draw(android.graphics.Canvas, com.google.android.maps.MapView, boolean, long)
	 */
	@Override
	public synchronized boolean draw(Canvas canvas, MapView mapView,
			boolean shadow, long when) {
		Projection proj = mapView.getProjection();

		if (isCompassEnabled()) {
			drawCompass(canvas, getOrientation());
		}
		
		if (directions != null) {
			List<Step> steps = directions.getLegs().get(0).getSteps();
			Point legStartPoint = null;
			Point legEndPoint = null;
			
			// Draw route first, then start point over it
			for (Step step : steps) {				
				List<GeoPoint> polyline = step.getDecodedPolyline();
				for (int i = 1, j = polyline.size(); i < j; i ++) {
					legStartPoint = new Point();
					proj.toPixels(polyline.get(i - 1), legStartPoint);
					legEndPoint = new Point();
					proj.toPixels(polyline.get(i), legEndPoint);
					
					// Connecting line start of step to end of step
					canvas.drawLine(legStartPoint.x, legStartPoint.y, legEndPoint.x, legEndPoint.y, gradientLinePaint);
					canvas.drawLine(legStartPoint.x, legStartPoint.y, legEndPoint.x, legEndPoint.y, linePaint);
				}
			}
			
			// Starting point
			startPoint = new Point();
			proj.toPixels(startLocation, startPoint);
			canvas.drawCircle(startPoint.x, startPoint.y, radius, startInnerFill);
			canvas.drawCircle(startPoint.x, startPoint.y, radius + 1, outer);
		}
		
		// Mark the end point
		if (selectedLocation != null) {
			endPoint = new Point();
			proj.toPixels(selectedLocation, endPoint);

			canvas.drawCircle(endPoint.x, endPoint.y, radius, endInnerFill);
			canvas.drawCircle(endPoint.x, endPoint.y, radius + 1, outer);
		}
		
		if (isMyLocationEnabled() && getMyLocation() != null) {			
			drawMyLocation(canvas, mapView, getLastFix(), getMyLocation(), when);
		}
		
		// As per default implementation
		return false;
	}
	
	/* (non-Javadoc)
	 * @see com.google.android.maps.MyLocationOverlay#onTap(com.google.android.maps.GeoPoint, com.google.android.maps.MapView)
	 */
	@Override
	public boolean onTap(GeoPoint p, MapView map) {

		if (placingMarker) {
			setSelectedLocation(p, map.getContext());
			map.invalidate();
			return true;
		} else if (navigating && directions != null) {
			Projection proj = map.getProjection();

			// Check if start/end points clicked in, and display addresses if so
			if (isTapWithin(proj, p, selectedLocation)) {
				Toast.makeText(map.getContext(), directions.getEndAddress(), Toast.LENGTH_SHORT).show();
				return true;
			}
			
			if (isTapWithin(proj, p, startLocation)) {
				Toast.makeText(map.getContext(), directions.getStartAddress(), Toast.LENGTH_SHORT).show();
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Resets elements of the overlay after we've finished navigating.
	 */
	public void reset() {
		setSelectedLocation(null);
		setDirections(null);
		
		navigating = false;
	}
	
// Private methods

	/**
	 * Sets up the various canvas objects prior to the drawing.
	 */
	private void init(Context context) {		
    	DashPathEffect dash = new DashPathEffect(new float[] { 10, 10, }, 1);
    	
		linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		linePaint.setStrokeWidth(5);
		linePaint.setColor(context.getResources().getColor(R.color.navigation_line));
		linePaint.setPathEffect(dash);
		linePaint.setAlpha(159); // temp - do this in proper color def
		
		gradientLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		gradientLinePaint.setStrokeWidth(12);
		gradientLinePaint.setColor(context.getResources().getColor(R.color.navigation_line));
		gradientLinePaint.setAlpha(25);
		
		startInnerFill = new Paint(Paint.ANTI_ALIAS_FLAG);
		startInnerFill.setColor(context.getResources().getColor(R.color.start_fill));
		startInnerFill.setStyle(Style.FILL);
		startInnerFill.setStrokeWidth(1);

		endInnerFill = new Paint(Paint.ANTI_ALIAS_FLAG);
		endInnerFill.setColor(context.getResources().getColor(R.color.end_fill));
		endInnerFill.setStyle(Style.FILL);
		endInnerFill.setStrokeWidth(1);
		
		outer = new Paint(Paint.ANTI_ALIAS_FLAG);
		outer.setColor(context.getResources().getColor(R.color.location_outline));
		outer.setStyle(Style.STROKE);
		outer.setStrokeWidth(3);
		
		// Set up marker radius size based on screen density
		WindowManager manager = (WindowManager) context.getSystemService(Service.WINDOW_SERVICE);
		
		DisplayMetrics metrics = new DisplayMetrics();
		manager.getDefaultDisplay().getMetrics(metrics);
		
		switch (metrics.densityDpi) {
		case DisplayMetrics.DENSITY_LOW:
			radius = LOW_DENSITY_RADIUS;
			break;
		case DisplayMetrics.DENSITY_MEDIUM:
			radius = MED_DENSITY_RADIUS;
			break;
		case DisplayMetrics.DENSITY_HIGH:
			radius = HIGH_DENSITY_RADIUS;
			break;		
		}
	}

// Private methods
	
	private boolean isTapWithin(Projection proj, GeoPoint tap, GeoPoint location) {
		int offset = radius + POINT_OFFSET;

		Point pt = new Point();
		Point tapPt = new Point();

		// Check if start/end points clicked in, and display addresses if so
		proj.toPixels(tap, tapPt);
		
		proj.toPixels(location, pt);
		Rect rect = new Rect(pt.x - offset, pt.y - offset, pt.x + offset, pt.y + offset);
		
		return (rect.contains(tapPt.x, tapPt.y)) ? true : false;
	}
	
// Accessors
	
	/**
	 * @return the directions
	 */
	public MapDirections getDirections() {
		return directions;
	}

	/**
	 * @param directions the directions to set
	 */
	public void setDirections(MapDirections directions) {
		this.directions = directions;
	}
	
	/**
	 * @return the selectedLocation
	 */
	public GeoPoint getSelectedLocation() {
		return selectedLocation;
	}

	/**
	 * @param selectedLocation the selectedLocation to set
	 */
	public void setSelectedLocation(GeoPoint selectedLocation) {
		this.selectedLocation = selectedLocation;
	}

	/**
	 * Overloaded version that also sends a broadcast, indicating it has been set.
	 * @param selectedLocation the selectedLocation to set
	 */
	public void setSelectedLocation(GeoPoint selectedLocation, Context context) {
		this.selectedLocation = selectedLocation;
		
		// Fire event to notify activity we have a location on the map
		Intent i = new Intent(LOCATION_ON_MAP);
		context.sendBroadcast(i);
	}
	
	/**
	 * @return the startLocation
	 */
	public GeoPoint getStartLocation() {
		return startLocation;
	}

	/**
	 * @param startLocation the startLocation to set
	 */
	public void setStartLocation(GeoPoint point) {
		this.startLocation = point;
	}

	/**
	 * @return the placingMarker
	 */
	public boolean isPlacingMarker() {
		return placingMarker;
	}

	/**
	 * @param placingMarker the placingMarker to set
	 */
	public void setPlacingMarker(boolean placingMarker) {
		this.placingMarker = placingMarker;
	}

	/**
	 * @return the navigating
	 */
	public boolean isNavigating() {
		return navigating;
	}

	/**
	 * @param navigating the navigating to set
	 */
	public void setNavigating(boolean navigating) {
		this.navigating = navigating;
	}	
}
