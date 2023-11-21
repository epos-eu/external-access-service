package org.epos.api.beans;

import java.io.Serializable;

public class TemporalCoverage  implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String startDate;
	
	private String endDate;

	public String getStartDate() {
		return startDate;
	}

	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	@Override
	public String toString() {
		return "TemporalCoverage [startDate=" + startDate + ", endDate=" + endDate + "]";
	}


}
