package org.epos_ip.tcsconnector.core;

import java.util.HashMap;

import org.epos.core.URLGeneration;

public class CQLFilterTest {

	
	public static void main(String[] args) {
		
		String template = "http://domain{?service}&CQL_FILTER=bbox({geometry},{minlatitude,minlongitude,maxlatitude,maxlongitude}){%20AND%20parameter}";
		
		HashMap<String,Object> parametersMap = new HashMap<String, Object>();
		parametersMap.put("service", "WFS");
		parametersMap.put("geometry", "geom");
		parametersMap.put("minlatitude", "123");
		parametersMap.put("minlongitude", "123");
		parametersMap.put("maxlatitude", "123");
		parametersMap.put("maxlongitude", "123");
		parametersMap.put("parameter", "prova");
		
		String url = URLGeneration.generateURLFromTemplateAndMap(template, parametersMap);
		System.out.println(url);
		
	}

	
}
