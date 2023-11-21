package org.epos.api.beans;

import java.util.List;
import java.util.Map;

import com.google.gson.annotations.SerializedName;

public class Plugin {
	
	private String identifier;
	private String name;
	private String description;
	private String downloadURL;
	private String license;
	private String softwareVersion;
	private String documentationURL;
    @SerializedName(value = "proxy-type")
	private String proxyType;
	private String requirements;
	private Map<String, Map<String,String>> action;
	private List<String> operations;
	private String location;
	
	public Plugin(String identifier) {
		this.identifier = identifier;
	}
	
	public Plugin(String identifier, String name, String description, String downloadURL, String license,
			String softwareVersion, String documentationURL, String proxyType, String requirements,
			Map<String, Map<String, String>> action, List<String> operations) {
		super();
		this.identifier = identifier;
		this.name = name;
		this.description = description;
		this.downloadURL = downloadURL;
		this.license = license;
		this.softwareVersion = softwareVersion;
		this.documentationURL = documentationURL;
		this.proxyType = proxyType;
		this.requirements = requirements;
		this.action = action;
		this.operations = operations;
	}

	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDownloadURL() {
		return downloadURL;
	}

	public void setDownloadURL(String downloadURL) {
		this.downloadURL = downloadURL;
	}

	public String getLicense() {
		return license;
	}

	public void setLicense(String license) {
		this.license = license;
	}

	public String getSoftwareVersion() {
		return softwareVersion;
	}

	public void setSoftwareVersion(String softwareVersion) {
		this.softwareVersion = softwareVersion;
	}

	public String getDocumentationURL() {
		return documentationURL;
	}

	public void setDocumentationURL(String documentationURL) {
		this.documentationURL = documentationURL;
	}

	public String getProxyType() {
		return proxyType;
	}

	public void setProxyType(String proxyType) {
		this.proxyType = proxyType;
	}

	public String getRequirements() {
		return requirements;
	}

	public void setRequirements(String requirements) {
		this.requirements = requirements;
	}

	public Map<String, Map<String, String>> getAction() {
		return action;
	}

	public void setAction(Map<String, Map<String, String>> action) {
		this.action = action;
	}

	public List<String> getOperations() {
		return operations;
	}

	public void setOperations(List<String> operations) {
		this.operations = operations;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	@Override
	public String toString() {
		return "Plugin [identifier=" + identifier + ", name=" + name + ", description=" + description + ", downloadURL="
				+ downloadURL + ", license=" + license + ", softwareVersion=" + softwareVersion + ", documentationURL="
				+ documentationURL + ", proxyType=" + proxyType + ", requirements=" + requirements + ", action="
				+ action + ", operations=" + operations + "]";
	}
	
	

}
