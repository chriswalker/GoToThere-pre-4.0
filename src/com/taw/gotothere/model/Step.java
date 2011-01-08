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

package com.taw.gotothere.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.google.android.maps.GeoPoint;

/**
 * Represents a single step within the larger leg.
 * 
 * @author Chris
 *
 */
public class Step implements Serializable {
	
	private String travelMode;
	
	private LatLng startLocation;
	
	private LatLng endLocation;
	
	private String htmlInstructions;
	
	/** Encoded polyline. */
	private String polyLine; 
	
	/** Duration of the route, in seconds. */
	private int duration;
	/** Duration of the route, displayable text. */
	private String durationText;
	/** Distance of the route, in meters. */
	private int distance;
	/** Disance of the route, displayable text. */
	private String distanceText;
	
	/**
	 * The Directions API returns a start & end point for the leg, but
	 * this would just yield a straight line on the map, which only approximately
	 * follows roads, so we use the polyline associated with each step instead. 
	 * This is encoded as per Google's Polyline ecoding algorithm. The decoder 
	 * below is taken (with much relief) from:
	 * 
	 * http://jeffreysambells.com/posts/2010/05/27/decoding-polylines-from-google-maps-direction-api-with-java/
	 * 
	 * @return points a List of GeoPoints for the polyline of this step
	 */
	public List<GeoPoint> getDecodedPolyline() {

	    List<GeoPoint> points = new ArrayList<GeoPoint>();
	    int index = 0, len = polyLine.length();
	    int lat = 0, lng = 0;

	    while (index < len) {
	        int b, shift = 0, result = 0;
	        do {
	            b = polyLine.charAt(index++) - 63;
	            result |= (b & 0x1f) << shift;
	            shift += 5;
	        } while (b >= 0x20);
	        int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
	        lat += dlat;

	        shift = 0;
	        result = 0;
	        do {
	            b = polyLine.charAt(index++) - 63;
	            result |= (b & 0x1f) << shift;
	            shift += 5;
	        } while (b >= 0x20);
	        int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
	        lng += dlng;

	        GeoPoint p = new GeoPoint((int) (((double) lat / 1E5) * 1E6),
	             (int) (((double) lng / 1E5) * 1E6));
	        points.add(p);
	    }

	    return points;
	}
	
// Accessors
	
	/**
	 * @return the travelMode
	 */
	public String getTravelMode() {
		return travelMode;
	}
	
	/**
	 * @param travelMode the travelMode to set
	 */
	public void setTravelMode(String travelMode) {
		this.travelMode = travelMode;
	}
	
	/**
	 * @return the startLocation
	 */
	public LatLng getStartLocation() {
		return startLocation;
	}
	
	/**
	 * @param startLocation the startLocation to set
	 */
	public void setStartLocation(int lat, int lng) {		
		this.startLocation = new LatLng(lat, lng);
	}
	
	/**
	 * @return the endLocation
	 */
	public LatLng getEndLocation() {
		return endLocation;
	}
	
	/**
	 * @param endLocation the endLocation to set
	 */
	public void setEndLocation(int lat, int lng) {
		this.endLocation = new LatLng(lat, lng);
	}
	
	/**
	 * @return the htmlInstructions
	 */
	public String getHtmlInstructions() {
		return htmlInstructions;
	}
	
	/**
	 * @param htmlInstructions the htmlInstructions to set
	 */
	public void setHtmlInstructions(String htmlInstructions) {
		this.htmlInstructions = htmlInstructions;
	}
	
	/**
	 * @return the duration
	 */
	public int getDuration() {
		return duration;
	}
	
	/**
	 * @param duration the duration to set
	 */
	public void setDuration(int duration) {
		this.duration = duration;
	}
	
	/**
	 * @return the durationText
	 */
	public String getDurationText() {
		return durationText;
	}
	
	/**
	 * @param durationText the durationText to set
	 */
	public void setDurationText(String durationText) {
		this.durationText = durationText;
	}
	
	/**
	 * @return the distance
	 */
	public int getDistance() {
		return distance;
	}
	
	/**
	 * @param distance the distance to set
	 */
	public void setDistance(int distance) {
		this.distance = distance;
	}
	
	/**
	 * @return the distanceText
	 */
	public String getDistanceText() {
		return distanceText;
	}
	
	/**
	 * @param distanceText the distanceText to set
	 */
	public void setDistanceText(String distanceText) {
		this.distanceText = distanceText;
	}

	/**
	 * @return the polyLine
	 */
	public String getPolyLine() {
		return polyLine;
	}

	/**
	 * @param polyLine the polyLine to set
	 */
	public void setPolyLine(String polyLine) {
		this.polyLine = polyLine;
	}
	
	
}
