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

/**
 * @author epos
 *
 */
public class ServiceParameter {

	private String name;
	private String label;
	private String type;
	private ArrayList<String> enumType;
	private String minValue;
	private String maxValue;
	private String version;
	private String defaultValue;
	private String value;
	private String property;
	private boolean required;
	private String valuePattern;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setEnumValue(String value) {
		if(this.enumType==null) this.enumType = new ArrayList<>();
		this.enumType.add(value);
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
	 * @return the enumType
	 */
	public ArrayList<String> getEnumType() {
		return enumType;
	}

	/**
	 * @param enumType the enumType to set
	 */
	public void setEnumType(ArrayList<String> enumType) {
		this.enumType = enumType;
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



	@Override
	public String toString() {
		return "ServiceParameter [name=" + name + ", label=" + label + ", type=" + type + ", enumType=" + enumType
				+ ", minValue=" + minValue + ", maxValue=" + maxValue + ", version=" + version + ", defaultValue="
				+ defaultValue + ", value=" + value + ", property=" + property + ", required=" + required
				+ ", valuePattern=" + valuePattern + "]";
	}

	public boolean isNull() {
		return name==null && label==null && type==null && enumType==null && minValue==null && maxValue==null && version==null;
	}

	public boolean getRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
}
