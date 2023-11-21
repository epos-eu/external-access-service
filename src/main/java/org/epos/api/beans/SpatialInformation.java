package org.epos.api.beans;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class SpatialInformation {

	private final static Logger LOGGER = LoggerFactory.getLogger(SpatialInformation.class);
	
	public static JsonArray doSpatial(String spatial)
	{
		JsonObject spatialReturn = new JsonObject();
		JsonObject wkid = new JsonObject();
		wkid.addProperty("wkid", 4326);
		spatialReturn.add("spatialReference", wkid);
		boolean isPoint = (spatial.contains("POINT")) ? true : false;

		LOGGER.debug("Is point: {}", isPoint);
		spatial = spatial.replaceAll("POLYGON", "").replaceAll("POINT", "").replaceAll("\\)", "").replaceAll("\\(", "");

		LOGGER.debug("Spatial: {}", spatial);

		if(isPoint) spatial = spatial.replaceAll("\\,", "");
		String[] points = spatial.split(",");


		JsonArray path = new JsonArray();
		
		if(isPoint) {
			String[] latlon = points[0].trim().split(" ");
			//spatialReturn.addProperty("x", Double.parseDouble(latlon[0]));
			//spatialReturn.addProperty("y", Double.parseDouble(latlon[1]));
			path.add(Double.parseDouble(latlon[0]));
			path.add(Double.parseDouble(latlon[1]));
		} else {
			try {
				for(String point : points) {
					String[] latlon  = point.trim().split(" ");
					JsonArray points1 = new JsonArray();
					points1.add(Double.parseDouble(latlon[0]));
					points1.add(Double.parseDouble(latlon[1]));
					path.add(points1);
				}
			}catch(Exception e) {}
		}
		return path;

	}

	public static boolean checkPoint(String spatial) {
		return (spatial.contains("POINT"))? true : false;
	}
}
