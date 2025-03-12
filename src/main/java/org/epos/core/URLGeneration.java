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
package org.epos.core;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class URLGeneration {

	private static final Logger LOGGER = LoggerFactory.getLogger(URLGeneration.class);

	private static final ArrayList<String> blackList = new ArrayList<>();
	static {
		blackList.add("()");
		blackList.add("(,,,)");
		blackList.add("[]");
		blackList.add("[,,,]");
	}

	private URLGeneration() {}

	/**
	 * Generate URL from a template "http://url.org/{id}{?param}" and a parametermap
	 * 
	 * 
	 * @param template
	 * @param map
	 * @return
	 */
	public static String generateURLFromTemplateAndMap(String template, Map<String, Object> map)
	{

		boolean isSegment = false;
		boolean isThereQuestionMark = false;
		StringBuilder sb = new StringBuilder();
		sb.ensureCapacity(template.length());

		for(int i = 0; i < template.length(); i++) {
			char c = template.charAt(i);
			if(c == '?') isThereQuestionMark = true;
			switch(c) {
			case '{': {
				isSegment = true;
				sb.append('{'); break;
			}
			case '}': {
				sb.append('}'); 
				isSegment = false;
				String calcTemplate = calculateTemplate(sb.toString(), map);
				LOGGER.debug("infos: "+sb.toString()+" "+map.toString());
				LOGGER.debug("calcTemplate: "+calcTemplate);
                assert calcTemplate != null;
                if(calcTemplate.isEmpty() && isThereQuestionMark) template = template.replace(sb.toString(), "?");
                template = template.replace(sb.toString(), calcTemplate);
                LOGGER.debug("template updated: "+template);
                if(template.contains("{")) {
                    template = generateURLFromTemplateAndMap(template, map);
                }
                if(calcTemplate.isEmpty()){
					template = template.replace(sb.toString(), "");
				}
				break;
			}
			default: {
				if(isSegment) sb.append(c);
				break;
			}
			}
		}

		LOGGER.info("TEMPLATE: "+template);
		if(template.endsWith("?")) template = template.substring(0, template.length() - 1);

		return template;
	}


	private static String calculateTemplate(String segment, Map<String, Object> map) {

		//boolean iHaveQuestionMark = false;
		segment = segment.replaceAll("\\{", "").replaceAll("}", "");
		LOGGER.info("segmento: "+segment);
		StringBuilder segmentOutput = new StringBuilder();

		String switchValue = segment.startsWith("%20AND%20")? "%20AND%20" : segment.substring(0, 1);

		switch(switchValue) {
		case "?":
			//iHaveQuestionMark = true;
			String[] parameters = segment.replace("?","").split(",");
            LOGGER.info("Pre-output: "+segmentOutput.toString());
            for (String parameter : parameters) {
                LOGGER.info("Loop-Start: " + segmentOutput.toString());
                if (map.containsKey(parameter.trim())) {
                    if (segmentOutput.length() <= 1) {
                        segmentOutput.append(parameter.trim()).append("=").append(map.get(parameter.trim()));
                    } else {
                        segmentOutput.append("&").append(parameter.trim()).append("=").append(map.get(parameter.trim()));
                    }
                }
                LOGGER.info("Loop-End: " + segmentOutput.toString());
            }
			//if(iHaveQuestionMark) segmentOutput.insert(0, "?");
			if(!segmentOutput.toString().isEmpty()) segmentOutput.insert(0, "?");
			break;
		case "&":
			String[] parameters1 = segment.replace("&","").split(",");
            LOGGER.info("Pre-output: "+segmentOutput.toString());
            for (String s : parameters1) {
                LOGGER.info("Loop-Start: " + segmentOutput.toString());
                if (map.containsKey(s.trim())) {
                    if (segmentOutput.length() <= 1) {
                        segmentOutput.append(s.trim()).append("=").append(map.get(s.trim()));
                    } else {
                        segmentOutput.append("&").append(s.trim()).append("=").append(map.get(s.trim()));
                    }
                }
                LOGGER.info("Loop-End: " + segmentOutput.toString());
            }
			if(!segmentOutput.toString().isEmpty()) segmentOutput.insert(0, "&");
			break;
		case "/":
			String[] parameters2 = segment.replace("/","").split(",");
            LOGGER.info("Pre-output: "+segmentOutput.toString());
            for (String string : parameters2) {
                LOGGER.info("Loop-Start: " + segmentOutput.toString());
                if (map.containsKey(string.trim())) {
                    if (segmentOutput.length() <= 1) {
                        segmentOutput.append(string.trim()).append("=").append(map.get(string.trim()));
                    } else {
                        segmentOutput.append("&").append(string.trim()).append("=").append(map.get(string.trim()));
                    }
                }
                LOGGER.info("Loop-End: " + segmentOutput.toString());
            }
			if(!segmentOutput.toString().isEmpty()) segmentOutput.insert(0, "/");;
			break;
		case "%20AND%20":
			String[] parameters3 = segment.replace("%20AND%20","").split(",");
            LOGGER.info("Pre-output: "+segmentOutput.toString());
            for (String s : parameters3) {
                LOGGER.info("Loop-Start: " + segmentOutput.toString());
                if (map.containsKey(s.trim())) {
                    if (segmentOutput.length() <= 1) {
                        segmentOutput.append(s.trim()).append("=").append(map.get(s.trim()));
                    } else {
                        segmentOutput.append("%20AND%20").append(s.trim()).append("=").append(map.get(s.trim()));
                    }
                }
                LOGGER.info("Loop-End: " + segmentOutput.toString());
            }
			if(!segmentOutput.toString().isEmpty()) segmentOutput.insert(0, "%20AND%20");
			break;
		default:
			String[] parameters11 = segment.split(",");
            for (String s : parameters11) {
                if (map.containsKey(s.trim())) {
                    segmentOutput.append(",").append(map.get(s.trim()));
                }
            }
            try {
				if(segmentOutput.length()>0) {
					String aux =  segmentOutput.substring(1, segmentOutput.length());
					segmentOutput = new StringBuilder();
					segmentOutput.append(aux);
				}
			}catch(Exception e) {
				LOGGER.error(e.toString());
				return null;
			}
			break;
		}

		LOGGER.info("Segment-output: "+segmentOutput.toString());

		return segmentOutput.toString();
	}


	public static String removeQueryParameter(String url, String parameterName) throws URISyntaxException {

		url = url.replaceAll(";", "-");
		LOGGER.info("Url: "+url);
		URIBuilder uriBuilder = new URIBuilder(url);
		List<NameValuePair> queryParameters = uriBuilder.getQueryParams();
		for (Iterator<NameValuePair> queryParameterItr = queryParameters.iterator(); queryParameterItr.hasNext();) {
			NameValuePair queryParameter = queryParameterItr.next();
			LOGGER.info("Query parameters: "+queryParameter.toString());
			if(queryParameter.getValue()!=null) {
				if (queryParameter.getValue().isBlank() || queryParameter.getValue().isEmpty() || blackList.contains(queryParameter.getValue())) {
					queryParameterItr.remove();
				}
			}
		}
		uriBuilder.setParameters(queryParameters);
		LOGGER.debug("Url built: "+uriBuilder.build().toString());

		return uriBuilder.build().toString();

	}

	public static String ogcWFSChecker(String url) {
		String[] questionMarkSplit = url.split("\\?", 2);
		String[] parametersSplit = questionMarkSplit[1].split("&");

		ArrayList<String> parametersAdjusted = new ArrayList<String>();
        for (String s : parametersSplit) {
            if (s.contains("CQL_FILTER")) {
                String CQLContext = s.replaceAll("CQL_FILTER=", "");
                if (!CQLContext.isBlank()) {
                    String[] splits = CQLContext.split("%20AND%20");
                    ArrayList<String> updatedSplit = new ArrayList<String>();
                    for (String split : splits) {
                        if (!split.isBlank()) {
                            if (split.contains("bbox")) {
                                int splitSize = split.replaceAll("bbox", "").replaceAll("\\(", "").replaceAll("\\)", "").split(",").length;
                                if (splitSize == 5) updatedSplit.add(split);
                            } else updatedSplit.add(split);
                        }
                    }
                    if (!updatedSplit.isEmpty()) {
                        parametersAdjusted.add("CQL_FILTER=" + String.join("%20AND%20", updatedSplit));
                    }
                }
            } else parametersAdjusted.add(s);
        }

		return questionMarkSplit[0]+"?"+String.join("&", parametersAdjusted);
	}
}
