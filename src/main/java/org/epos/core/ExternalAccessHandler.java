package org.epos.core;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.EnumUtils;
import org.epos.api.utility.ContentType;
import org.epos.core.beans.Distribution;
import org.epos.core.beans.ServiceParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class ExternalAccessHandler {


	private static final Logger LOGGER = LoggerFactory.getLogger(ExternalAccessHandler.class);

	private static final String PARAMS = "params";
	private static final String CONVERSION = "conversion";

	private static Gson gson = new Gson();

	public static Map<String, Object> handle(String payload, String kind) {

		LOGGER.debug(payload);

		JsonObject payObj = gson.fromJson(payload, JsonObject.class);
		JsonObject conversion = null;
		JsonObject usedByFile = payObj;	

		/** PARAMETERS ANALYSIS AND SETUP **/
		if (payObj.has(CONVERSION)) {
			conversion = payObj.get(CONVERSION).getAsJsonObject();
		}

		if (payObj.has(PARAMS)) {
			payObj = payObj.getAsJsonObject(PARAMS);
		}

		Distribution distr = gson.fromJson(payload, Distribution.class);

		List<ServiceParameter> distParams = distr.getParameters();
		if (distParams == null) {
			LOGGER.error("No distribution parameters provided");
		}

		if (!usedByFile.getAsJsonObject().has("type")) {
			LOGGER.error("No Type parameter provided, sending back a 503 message");
			Map<String, Object> responseMap = new HashMap<>();
			responseMap.put("httpStatusCode", "503");
			return responseMap;
		}

		switch(usedByFile.getAsJsonObject().get("type").getAsString()) {
		case "DOWNLOADABLE_FILE" :
			try {
				String compiledUrl = usedByFile.getAsJsonObject().get("serviceEndpoint").getAsString();

				compiledUrl = URLGeneration.ogcWFSChecker(compiledUrl);

				return ExternalServicesRequest.getInstance().getRedirect(compiledUrl);
			} catch (Exception ex) {
				LOGGER.error(ex.getMessage());
				return null;
			}
		default:
			HashMap<String, Object> parameters = new HashMap<>();
			Map<String, Object> responseMap = new HashMap<>();

			if(distr.getParameters()!=null) {
				distr.getParameters().forEach(p -> {
					if (p.getValue() != null && !p.getValue().equals(""))
						parameters.put(p.getName(), p.getValue());
					if (p.getDefaultValue() != null && p.getValue() == null && p.getRequired())
						parameters.put(p.getName(), p.getDefaultValue());
				});
			}
			if(payObj.getAsJsonObject().has("format")) {
				if(ContentType.fromValue(payObj.get("format").getAsString()).isPresent() && conversion==null) {
					if(distr.getParameters()!=null) {
						for(ServiceParameter p : distr.getParameters()) {
							if(p.getProperty()!=null && p.getProperty().equals("schema:encodingFormat")) {
								parameters.remove(p.getName());
								parameters.put(p.getName(),payObj.get("format").getAsString());
							}
						}
					}
				}
				if(ContentType.fromValue(payObj.get("format").getAsString()).isEmpty()) {
					if(distr.getParameters()!=null) {
						for(ServiceParameter p : distr.getParameters()) {
							if(p.getProperty()!=null && p.getProperty().equals("schema:encodingFormat")) {
								parameters.remove(p.getName());
								parameters.put(p.getName(),payObj.get("format").getAsString());
							}
						}
					}
				}
			}

			String compiledUrl = null;
			compiledUrl = URLGeneration.generateURLFromTemplateAndMap(distr.getEndpoint(), parameters);
			try {
				compiledUrl = URLGeneration.ogcWFSChecker(compiledUrl);
			}catch(Exception e) {
				LOGGER.error("Found the following issue whilst executing the WFS Checker, issue raised "+ e.getMessage() + " - Continuing execution");
			}
			/*try {
				if(URLGeneration.removeQueryParameter(compiledUrl, null) != null)
					compiledUrl = URLGeneration.removeQueryParameter(compiledUrl, null);
			} catch (URISyntaxException e) {
				LOGGER.error(e.getMessage());
			}*/

			LOGGER.info("URL to be executed: "+compiledUrl);

			System.out.println("URL to be executed: "+compiledUrl);

			if (kind.contains("getoriginalurl")) {
				try {
					responseMap.put("url", compiledUrl);
					return responseMap;
				} catch (Exception ex) {
					LOGGER.error("Issue raised "+ex.getMessage()+" sending back a 503 message");
					responseMap.put("httpStatusCode", "503");
					return responseMap;
				}
			}


			if (payObj.getAsJsonObject().has("format") 
					&& checkFormat(payObj.get("format").getAsString(), compiledUrl.contains("WFS"))) {
				LOGGER.info("Direct request");
				if(conversion==null) {
					LOGGER.info("Is native GeoJSON or CovJSON");
					try {
						String responsePayload = ExternalServicesRequest.getInstance().requestPayload(compiledUrl);
						responseMap.remove("content");
						responseMap.put("content", responsePayload.length()==0? "{}" : responsePayload);
					} catch (IOException e) {
						LOGGER.error(e.getMessage());
						LOGGER.error("Impossible to get any response from "+compiledUrl);
						responseMap = new HashMap<>();
						responseMap.put("httpStatusCode", "503");
						return responseMap;
					}
					return responseMap;
				}
				else {
					LOGGER.info("Is not native GeoJSON or CovJSON");
					try {
						Map<String, String> parametersMap = new HashMap<>();
						parametersMap.put("operation", conversion.get("operation").getAsString());
						parametersMap.put("requestContentType", conversion.get("requestContentType").getAsString());
						parametersMap.put("responseContentType", conversion.get("responseContentType").getAsString());
						responseMap.put("parameters", parametersMap);
						String responsePayload = ExternalServicesRequest.getInstance().requestPayload(compiledUrl);
						responseMap.put("content", responsePayload.length()==0? "{}" : responsePayload);
						return responseMap;
					} catch (Exception ex) {
						LOGGER.error(ex.getMessage());
						LOGGER.error("No Conversion parameter provided, sending back a 503 message");
						responseMap.put("httpStatusCode", "503");
						return responseMap;
					}
				}
			} else  {
				LOGGER.info("Redirect");
				try {
					return ExternalServicesRequest.getInstance().getRedirect(compiledUrl);
				} catch (Exception ex) {
					LOGGER.error("Issue raised "+ex.getMessage()+" sending back a 503 message");
					responseMap.put("httpStatusCode", "503");
					return responseMap;
				}
			}
		}
	}

	private static boolean checkFormat(String format, boolean isWFS){
		format = format.toLowerCase();
		return format.equals("application/geo+json") 
				|| format.replaceAll("[^a-zA-Z0-9]", "").contains("geojson")
				|| format.replaceAll("[^a-zA-Z0-9]", "").contains("covjson")
				|| (format.contains("json") && isWFS)
				|| ContentType.fromValue(format).isPresent();
	}
}
