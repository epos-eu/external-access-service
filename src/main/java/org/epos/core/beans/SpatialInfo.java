/*******************************************************************************
 * Copyright 2021 EPOS ERIC
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.epos.core.beans;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;

public class SpatialInfo {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(SpatialInfo.class);
	
	private Integer wkid;
	private ArrayList<Object> paths;
	private Object x;
	private Object y;

	public SpatialInfo() {
	}

	/**
	 * @return the paths
	 */
	public ArrayList<Object> getPaths() {
		if(paths==null) paths = new ArrayList<>();
		return paths;
	}

	/**
	 * @param paths the paths to set
	 */
	public void setPaths(ArrayList<Object> paths) {
		this.paths = paths;
	}

	public void addPaths(Object path, boolean isPoint) {
		wkid = 4326;
		if(isPoint) {
			JsonArray paths = (JsonArray) path;
			LOGGER.debug(paths.getAsString());
			x = paths.get(0);
			y = paths.get(1);
		}else {
			this.getPaths().add(path);
		}
	}
	/**
	 * @return the wkid
	 */
	public Integer getWkid() {
		return wkid;
	}

	/**
	 * @return the y
	 */
	public Object getY() {
		return y;
	}

	/**
	 * @param y the y to set
	 */
	public void setY(Object y) {
		this.y = y;
	}

	/**
	 * @return the x
	 */
	public Object getX() {
		return x;
	}

	/**
	 * @param x the x to set
	 */
	public void setX(Object x) {
		this.x = x;
	}

}
