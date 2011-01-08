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
 * Represents a single leg of a route. Normally there will be a single leg,
 * as the user cannot specify waypoints (and so we shouldn't get more than one
 * returned)
 * @author Chris
 *
 */
public class Leg implements Serializable {

	/** All the steps in this leg. */
	private List<Step> steps = new ArrayList<Step>();
	
	/**
	 * Add a single step to the steps array.
	 * 
	 * @param step
	 */
	public void addStep(Step step) {
		steps.add(step);
	}


	
// Accessors
	
	public Step getStep(int idx) {
		return steps.get(idx);
	}
	
	/**
	 * @return the steps
	 */
	public List<Step> getSteps() {
		return steps;
	}

	/**
	 * @param steps the steps to set
	 */
	public void setSteps(List<Step> steps) {
		this.steps = steps;
	}
}
