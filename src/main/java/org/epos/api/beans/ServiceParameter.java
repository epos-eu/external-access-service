package org.epos.api.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class ServiceParameter implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String name;
	private String label;
	private String type;
	@SerializedName(value = "Enum")
	private List<String> enumValue;
	private String minValue;
	private String maxValue;
	private String version;
	private String defaultValue;
	private String value;
	private String property;
	private String valuePattern;
	private String readOnlyValue;
	private String multipleValue;
	private boolean required;


	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setEnumValue(String value) {
		if(this.enumValue==null) this.enumValue = new ArrayList<>();
		this.enumValue.add(value);
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getLabel() {
	    return label;
	}

	public void setLabel(String label) {
	    this.label = label;
	}

	public String getMinValue() {
	    return minValue;
	}

	public void setMinValue(String minValue) {
	    this.minValue = minValue;
	}

	public String getMaxValue() {
	    return maxValue;
	}

	public void setMaxValue(String maxValue) {
	    this.maxValue = maxValue;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	
	/**
	 * @return the enum
	 */
	public List<String> getEnumValue() {
		return enumValue;
	}

	/**
	 * @param enum1 the enum to set
	 */
	public void setEnumValue(List<String> enum1) {
		enumValue = enum1;
	}

	/**
	 * @return the property
	 */
	public String getProperty() {
		return property;
	}

	/**
	 * @param property the property to set
	 */
	public void setProperty(String property) {
		this.property = property;
	}

	/**
	 * @return the valuePattern
	 */
	public String getValuePattern() {
		return valuePattern;
	}

	/**
	 * @param valuePattern the valuePattern to set
	 */
	public void setValuePattern(String valuePattern) {
		this.valuePattern = valuePattern;
	}


	public boolean isNull() {
		return name==null && label==null && type==null && enumValue==null && minValue==null && maxValue==null && version==null;
	}

	/**
	 * @return the required
	 */
	public boolean isRequired() {
		return required;
	}

	/**
	 * @param required the required to set
	 */
	public void setRequired(boolean required) {
		this.required = required;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getReadOnlyValue() {
		return readOnlyValue;
	}

	public void setReadOnlyValue(String readOnlyValue) {
		this.readOnlyValue = readOnlyValue;
	}

	public String getMultipleValue() {
		return multipleValue;
	}

	public void setMultipleValue(String multipleValue) {
		this.multipleValue = multipleValue;
	}

	@Override
	public String toString() {
		return "ServiceParameter [name=" + name + ", label=" + label + ", type=" + type + ", enumValue=" + enumValue
				+ ", minValue=" + minValue + ", maxValue=" + maxValue + ", version=" + version + ", defaultValue="
				+ defaultValue + ", value=" + value + ", property=" + property + ", valuePattern=" + valuePattern
				+ ", readOnlyValue=" + readOnlyValue + ", multipleValue=" + multipleValue + ", required=" + required
				+ "]";
	}
	
}
