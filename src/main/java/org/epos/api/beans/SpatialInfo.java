package org.epos.api.beans;

import java.io.Serializable;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;

public class SpatialInfo implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final static Logger LOGGER = LoggerFactory.getLogger(SpatialInfo.class);
	
	private Integer wkid;
	private ArrayList<Object> paths;
	private Object x,y;

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
			LOGGER.debug("Point {}", paths);
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

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((paths == null) ? 0 : paths.hashCode());
		result = prime * result + ((wkid == null) ? 0 : wkid.hashCode());
		result = prime * result + ((x == null) ? 0 : x.hashCode());
		result = prime * result + ((y == null) ? 0 : y.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof SpatialInfo)) {
			return false;
		}
		SpatialInfo other = (SpatialInfo) obj;
		if (paths == null) {
			if (other.paths != null) {
				return false;
			}
		} else if (!paths.equals(other.paths)) {
			return false;
		}
		if (wkid == null) {
			if (other.wkid != null) {
				return false;
			}
		} else if (!wkid.equals(other.wkid)) {
			return false;
		}
		if (x == null) {
			if (other.x != null) {
				return false;
			}
		} else if (!x.equals(other.x)) {
			return false;
		}
		if (y == null) {
			if (other.y != null) {
				return false;
			}
		} else if (!y.equals(other.y)) {
			return false;
		}
		return true;
	}
	
	

}
