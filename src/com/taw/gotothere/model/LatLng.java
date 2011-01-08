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

/**
 * Represents a lat/long pair. We don't use GeoPoint locally as it's
 * not serializable.
 */
public class LatLng implements Serializable {
	/** Latitude, in micro degrees. */
	private int lat;
	/** Longitude in micro degrees. */
	private int lng;
			
	public LatLng(int lat, int lng) {
		this.lat = lat;
		this.lng = lng;
	}

	/**
	 * @return the lat
	 */
	public int getLat() {
		return lat;
	}

	/**
	 * @param lat the lat to set
	 */
	public void setLat(int lat) {
		this.lat = lat;
	}

	/**
	 * @return the lng
	 */
	public int getLng() {
		return lng;
	}

	/**
	 * @param lng the lng to set
	 */
	public void setLng(int lng) {
		this.lng = lng;
	}
	
}
