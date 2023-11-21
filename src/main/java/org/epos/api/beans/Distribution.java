package org.epos.api.beans;

import java.io.Serializable;
import java.util.*;

public class Distribution implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private transient String productid;
	private transient String distributionid;
	private String operationid;
	
	private String id;
	private String type;
	private String license;
	private String downloadURL;
	
	// WEBSERVICE
	private String serviceEndpoint;
	private String endpoint;

	private List<ServiceParameter> serviceParameters;
	
	public Distribution() {}

	public Distribution(String productid, String distributionid, String operationid, String href, String id, String uid,
						String type, String title, String description, String license, String downloadURL, List<String> keywords,
						String frequencyUpdate, List<String> internalID, List<String> DOI, SpatialInfo spatial,
						TemporalCoverage temporalCoverage, String serviceName, String serviceDescription,SpatialInfo serviceSpatial,
						TemporalCoverage serviceTemporalCoverage, String serviceEndpoint, String serviceDocumentation,
						String endpoint, List<ServiceParameter> serviceParameters) {
		super();
		this.productid = productid;
		this.distributionid = distributionid;
		this.operationid = operationid;
		this.id = id;
		this.type = type;
		this.license = license;
		this.downloadURL = downloadURL;
		this.serviceEndpoint = serviceEndpoint;
		this.endpoint = endpoint;
		this.serviceParameters = serviceParameters;
	}



	public String getProductid() {
		return productid;
	}

	public void setProductid(String productid) {
		this.productid = productid;
	}

	public String getDistributionid() {
		return distributionid;
	}

	public void setDistributionid(String distributionid) {
		this.distributionid = distributionid;
	}

	public String getOperationid() {
		return operationid;
	}

	public void setOperationid(String operationid) {
		this.operationid = operationid;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getLicense() {
		return license;
	}

	public void setLicense(String license) {
		this.license = license;
	}

	public String getDownloadURL() {
		return downloadURL;
	}

	public void setDownloadURL(String downloadURL) {
		this.downloadURL = downloadURL;
	}


	public String getServiceEndpoint() {
		return serviceEndpoint;
	}

	public void setServiceEndpoint(String serviceEndpoint) {
		this.serviceEndpoint = serviceEndpoint;
	}

	public List<ServiceParameter> getParameters() {
		if(serviceParameters==null) serviceParameters = new ArrayList<ServiceParameter>();
		return serviceParameters;
	}

	public void setParameters(List<ServiceParameter> parameters) {
		this.serviceParameters = parameters;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	@Override
	public int hashCode() {
		return Objects.hash(downloadURL, endpoint, id, license, operationid, serviceEndpoint, serviceParameters, type);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Distribution other = (Distribution) obj;
		return Objects.equals(downloadURL, other.downloadURL) && Objects.equals(endpoint, other.endpoint)
				&& Objects.equals(id, other.id) && Objects.equals(license, other.license)
				&& Objects.equals(operationid, other.operationid)
				&& Objects.equals(serviceEndpoint, other.serviceEndpoint)
				&& Objects.equals(serviceParameters, other.serviceParameters) && Objects.equals(type, other.type);
	}

	@Override
	public String toString() {
		return "Distribution [operationid=" + operationid + ", id=" + id + ", type=" + type + ", license=" + license
				+ ", downloadURL=" + downloadURL + ", serviceEndpoint=" + serviceEndpoint + ", endpoint=" + endpoint
				+ ", serviceParameters=" + serviceParameters + "]";
	}

}
