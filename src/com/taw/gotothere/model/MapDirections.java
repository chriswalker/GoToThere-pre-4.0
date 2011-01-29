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

/**
 * Represents a Directions API response. Contains legs, warnings etc. The 
 * NavigationOverlay will use this to plot the directions for the user.
 * This clas is analagous to the top-level "routes" array returned in the
 * Directions API JSON. We should only get back a single route entry.
 * 
 * @author Chris
 *
 */
public class MapDirections implements Serializable {

	/** Any warning message to display to the user, as per TOCs. */
	private List<String> warning = new ArrayList<String>();
	
	/** Route summary. */
	private String summary;
	
	/** Start address. */
	private String startAddress;
	/** End address. */
	private String endAddress;
	/** Start location. */
	private LatLng startLocation;
	/** End location. */
	private LatLng endLocation;
	
	/** Any copyright. */
	private String copyright;

	/** Duration of the route, in seconds. */
	private int duration;
	/** Duration of the route, displayable text. */
	private String durationText;
	/** Distance of the route, in meters. */
	private int distance;
	/** Disance of the route, displayable text. */
	private String distanceText;

	/** ID of a string that may be used to display to the user if there was a problem. */
	private int error = -1;
	
	/** All the legs in this route (usually one, as no waypoints specified by user.) */
	private List<Leg> legs = new ArrayList<Leg>();
	
// Public methods
	
	public void addLeg(Leg leg) {
		legs.add(leg);
	}
	
// Accessors
	
	/**
	 * @return the warning
	 */
	public List<String> getWarning() {
		return warning;
	}
	
	/**
	 * @param warning the warning to set
	 */
	public void setWarning(List<String> warning) {
		this.warning = warning;
	}
	
	/**
	 * @return the copyright
	 */
	public String getCopyright() {
		return copyright;
	}
	
	/**
	 * @param copyright the copyright to set
	 */
	public void setCopyright(String copyright) {
		this.copyright = copyright;
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
	 * @return the summary
	 */
	public String getSummary() {
		return summary;
	}

	/**
	 * @param summary the summary to set
	 */
	public void setSummary(String summary) {
		this.summary = summary;
	}

	/**
	 * @return the legs
	 */
	public List<Leg> getLegs() {
		return legs;
	}

	/**
	 * @param legs the legs to set
	 */
	public void setLegs(List<Leg> legs) {
		this.legs = legs;
	}

	/**
	 * @return the startAddress
	 */
	public String getStartAddress() {
		return startAddress;
	}

	/**
	 * @param startAddress the startAddress to set
	 */
	public void setStartAddress(String startAddress) {
		this.startAddress = startAddress;
	}

	/**
	 * @return the endAddress
	 */
	public String getEndAddress() {
		return endAddress;
	}

	/**
	 * @param endAddress the endAddress to set
	 */
	public void setEndAddress(String endAddress) {
		this.endAddress = endAddress;
	}

	/**
	 * @return the error
	 */
	public int getError() {
		return error;
	}

	/**
	 * @param error the error to set
	 */
	public void setError(int error) {
		this.error = error;
	}

	/**
	 * @return the startLocation
	 */
	public LatLng getStartLocation() {
		return startLocation;
	}

	/**
	 * @param lat integer Latitiude (in microdegrees)
	 * @param lat integer Longitude (in microdegrees)
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
	 * @param lat integer Latitiude (in microdegrees)
	 * @param lat integer Longitude (in microdegrees)
	 */

	public void setEndLocation(int lat, int lng) {
		this.endLocation = new LatLng(lat, lng);
	}
}
