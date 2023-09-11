package org.epos_ip.tcsconnector.core;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.epos.api.utility.ContentType;
import org.epos.api.utility.Utils;
import org.epos.core.ExternalAccessHandler;
import org.epos.core.ExternalServicesRequest;
import org.epos.core.URLGeneration;
import org.epos.core.beans.Distribution;
import org.epos.core.beans.ErrorMessage;
import org.epos.core.beans.ServiceParameter;
import org.epos.router_framework.domain.Actor;
import org.epos.router_framework.domain.BuiltInActorType;
import org.epos.router_framework.types.ServiceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class AppTest {

	public static void main(String[] args) {

		String responseCode = "NO_CONTENT";
		System.out.println(HttpStatus.valueOf(responseCode));
		//String payload = "{\"operationid\":\"UGACNRSVELOCITIES/WS/operation\",\"href\":\"https://ics-c.epos-ip.org/development/k8s-epos-deploy/latest/api/v1/resources/details?id=6643533d-bcc9-4326-b811-032c25814a83\",\"id\":\"6643533d-bcc9-4326-b811-032c25814a83\",\"uid\":\"UGACNRSVELOCITIES/Distribution/001\",\"type\":\"WEB_SERVICE\",\"title\":\" EPOS GNSS Velocities from UGA-CNRS\",\"description\":\" Download the GNSS velocities produced by the UGA-CNRS processing center. The data can be filtered by a search parameter (e.g. station, etc).\",\"license\":\"http://creativecommons.org/licenses/by/4.0/\",\"downloadURL\":\"\",\"keywords\":[\"gnss station velocities\",\"gnss station\",\"velocities\",\"computed\",\"gnss\",\"estimated\",\"ground motion\",\"geodesy\",\"products\"],\"dataProvider\":[{\"dataProviderLegalName\":\"UGA - UniversitÃ© Grenoble Alpes\",\"dataProviderUrl\":\"https://www.univ-grenoble-alpes.fr/\",\"country\":\"France\"}],\"frequencyUpdate\":\"http://purl.org/cld/freq/continuous\",\"internalID\":[\"WP10-DDSS-012\"],\"spatial\":{\"wkid\":4326,\"paths\":[[[40.429,81.201],[-72.773,79.687],[-75.234,76.1],[-40.429,43.068],[-18.632,18.979],[-2.109,35.817],[22.016,35.496],[34.101,27.761],[52.822,38.616],[43.066,66.93],[40.429,81.201]]]},\"temporalCoverage\":{\"startDate\":\"1996-01-01T00:00:00Z\"},\"scienceDomain\":[\"GNSS Station Velocities\"],\"hasQualityAnnotation\":\"https://gnss-epos.eu/quality-assurance/\",\"availableFormats\":[{\"label\":\"JSON\",\"format\":\"json\",\"originalFormat\":\"json\",\"href\":\"https://ics-c.epos-ip.org/development/k8s-epos-deploy/latest/api/v1/execute?id=6643533d-bcc9-4326-b811-032c25814a83&format=json\",\"type\":\"ORIGINAL\"},{\"label\":\"XML\",\"format\":\"xml\",\"originalFormat\":\"xml\",\"href\":\"https://ics-c.epos-ip.org/development/k8s-epos-deploy/latest/api/v1/execute?id=6643533d-bcc9-4326-b811-032c25814a83&format=xml\",\"type\":\"ORIGINAL\"}],\"availableContactPoints\":[{\"href\":\"https://ics-c.epos-ip.org/development/k8s-epos-deploy/latest/api/v1/sender/send-email?id=e24b5d96-c5f1-4cc2-9155-477af7d9cccb&contactType=SERVICEPROVIDERS\",\"type\":\"SERVICEPROVIDERS\"},{\"href\":\"https://ics-c.epos-ip.org/development/k8s-epos-deploy/latest/api/v1/sender/send-email?id=f7d41958-4d3a-44c2-bdd7-3cd1a53a88f6&contactType=DATAPROVIDERS\",\"type\":\"DATAPROVIDERS\"},{\"href\":\"https://ics-c.epos-ip.org/development/k8s-epos-deploy/latest/api/v1/sender/send-email?id=6643533d-bcc9-4326-b811-032c25814a83&contactType=ALL\",\"type\":\"ALL\"}],\"serviceName\":\" EPOS GNSS Velocities from UGA-CNRS\",\"serviceDescription\":\"Download the GNSS velocities produced by the UGA-CNRS processing center. The data can be filtered by a search parameter (e.g. station, etc).\",\"serviceProvider\":{\"dataProviderLegalName\":\"UBI - University of Beira Interior\",\"dataProviderUrl\":\"http://www.ubi.pt\",\"country\":\"Portugal\"},\"serviceSpatial\":{},\"serviceTemporalCoverage\":{},\"serviceEndpoint\":\"https://gnssproducts.epos.ubi.pt/GlassFramework/webresources/products/velocities/\",\"serviceDocumentation\":\"https://gnss-epos.eu/ics-tcs/\",\"endpoint\":\"https://gnssproducts.epos.ubi.pt/GlassFramework/webresources/products/velocities/{station}/UGA-CNRS/{coordinates_system}/{format}\",\"serviceParameters\":[{\"name\":\"station\",\"label\":\"Four character station identification\",\"type\":\"string\",\"defaultValue\":\"CASC\",\"value\":\"CASC\",\"required\":true},{\"name\":\"coordinates_system\",\"label\":\"Coordinates System\",\"type\":\"string\",\"Enum\":[\"enu\",\"xyz\"],\"defaultValue\":\"enu\",\"value\":\"enu\",\"required\":true},{\"name\":\"format\",\"label\":\"format\",\"type\":\"string\",\"Enum\":[\"json\",\"xml\"],\"defaultValue\":\"json\",\"value\":\"json\",\"property\":\"schema:encodingFormat\",\"required\":true}],\"categories\":{\"children\":[{\"children\":[{\"children\":[{\"ddss\":\"category:gnssstativelocities\",\"name\":\"GNSS Station Velocities\"}],\"ddss\":\"category:products\",\"name\":\"Products\"}],\"name\":\"GNSS Data and Products\"}],\"name\":\"domains\"},\"params\":{\"format\":\"json\",\"id\":\"6643533d-bcc9-4326-b811-032c25814a83\",\"params\":\"{\\\"coordinates_system\\\":\\\"enu\\\",\\\"station\\\":\\\"AQUI\\\",\\\"format\\\":\\\"json\\\"}\",\"useDefaults\":\"false\"}}";
				
		//ystem.out.println(redirectRequestTest(ServiceType.EXTERNAL, new HashMap<String, Object>(), payload));
	}
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ExternalAccessHandler.class);

	private static final String PARAMS = "params";
	private static final String CONVERSION = "conversion";

	private static Gson gson = new Gson();


	private static Map<String, Object> redirectRequestTest(ServiceType service, Map<String, Object> requestParams, String payload) {

		LOGGER.debug(payload);
		String kind = "execute";

		JsonObject payObj = gson.fromJson(payload, JsonObject.class);
		JsonObject conversion = null;
		JsonObject usedByFile = payObj;	
		Map<String, Object> responseMap = new HashMap<>();

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
			responseMap.put("httpStatusCode", "503");
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

			LOGGER.debug("URL to be executed: "+compiledUrl);

			System.out.println("URL to be executed: "+compiledUrl);

			if (kind.contains("getoriginalurl")) {
				try {
					responseMap.put("url", compiledUrl);
				} catch (Exception ex) {
					LOGGER.error("Issue raised "+ex.getMessage()+" sending back a 503 message");
					responseMap.put("httpStatusCode", "503");
				}
			}


			if (payObj.getAsJsonObject().has("format") 
					&& checkFormat(payObj.get("format").getAsString(), compiledUrl.contains("WFS"))) {
				LOGGER.debug("Direct request");
				if(conversion==null) {
					LOGGER.debug("Is native GeoJSON or CovJSON");
					try {
						String responsePayload = ExternalServicesRequest.getInstance().requestPayload(compiledUrl);
						responseMap.remove("content");
						responseMap.put("content", responsePayload.length()==0? "{}" : responsePayload);
					} catch (IOException e) {
						LOGGER.error(e.getMessage());
						LOGGER.error("Impossible to get any response from "+compiledUrl);
						responseMap = new HashMap<>();
						responseMap.put("httpStatusCode", "503");
					}
				}
				else {
					LOGGER.debug("Is not native GeoJSON or CovJSON");
					try {
						Map<String, String> parametersMap = new HashMap<>();
						parametersMap.put("operation", conversion.get("operation").getAsString());
						parametersMap.put("requestContentType", conversion.get("requestContentType").getAsString());
						parametersMap.put("responseContentType", conversion.get("responseContentType").getAsString());
						responseMap.put("parameters", parametersMap);
						String responsePayload = ExternalServicesRequest.getInstance().requestPayload(compiledUrl);
						responseMap.put("content", responsePayload.length()==0? "{}" : responsePayload);
					} catch (Exception ex) {
						LOGGER.error(ex.getMessage());
						LOGGER.error("No Conversion parameter provided, sending back a 503 message");
						responseMap.put("httpStatusCode", "503");
					}
				}
			} else  {
				LOGGER.debug("Redirect");
				try {
					return ExternalServicesRequest.getInstance().getRedirect(compiledUrl);
				} catch (Exception ex) {
					LOGGER.error("Issue raised "+ex.getMessage()+" sending back a 503 message");
					responseMap.put("httpStatusCode", "503");
				}
			}
		}
		// ---------------------
		
		String responseCode = "OK";

		if (responseMap.containsKey("httpStatusCode")) {
			responseCode = HttpStatus.valueOf(Integer.parseInt((String) responseMap.get("httpStatusCode"))).name();
		} else {
			if(responseMap.get("content")==null) {
				ErrorMessage errorMessage = new ErrorMessage();
				errorMessage.setMessage("Error missing content from "+ responseMap);
				return (Map<String, Object>) ResponseEntity.status(HttpStatus.NO_CONTENT)
						.body(Utils.gson.toJsonTree(errorMessage).toString());
			}

			HttpHeaders httpHeaders = new HttpHeaders();
			if(conversion!=null)
				httpHeaders.add("content-type", conversion.get("responseContentType").getAsString().contains("/")? conversion.get("responseContentType").getAsString() : "application/"+conversion.get("responseContentType").getAsString());
			else
				httpHeaders.add("content-type", "application/geo+json");

			JsonObject outputResponse = null;
			
				try {
					outputResponse = Utils.gson.fromJson(responseMap.toString(), JsonElement.class).getAsJsonObject();

					return (Map<String, Object>) ResponseEntity.status(HttpStatus.OK)
							.headers(httpHeaders)
							.body(outputResponse.get("content").getAsJsonObject().toString());
				}catch(Exception e) {
					ErrorMessage errorMessage = new ErrorMessage();
					errorMessage.setMessage("Error missing content from "+ e.getLocalizedMessage());
					return (Map<String, Object>) ResponseEntity.status(HttpStatus.NO_CONTENT)
							.headers(httpHeaders)
							.body(Utils.gson.toJsonTree(errorMessage).toString());
				}
			
		}

		switch (responseCode) {

		default : {
			ErrorMessage errorMessage = new ErrorMessage();
			errorMessage.setMessage("Received response "+responseCode+" from external webservice");
			errorMessage.setHttpCode(responseCode);
			if(responseMap.containsKey("redirect-url")) errorMessage.setUrl(responseMap.get("redirect-url").toString());
			if(responseMap.containsKey("content-type")) errorMessage.setContentType(responseMap.get("content-type").toString());

			return (Map<String, Object>) ResponseEntity.status(HttpStatus.valueOf(responseCode))
					.body(Utils.gson.toJsonTree(errorMessage).toString());
		}
		case "OK" : {
			HttpHeaders httpHeaders = new HttpHeaders();

			if(responseMap.containsKey("redirect-url") 
					&& responseMap.containsKey("content-type")
					&& responseMap.containsKey("httpStatusCode")) {

				int _httpStatusCode = Integer.parseInt((String) responseMap.get("httpStatusCode"));
				HttpStatus httpStatusCode = HttpStatus.valueOf(_httpStatusCode);

				String redirectUrl = (String) responseMap.get("redirect-url");	
				String contentType = (String) responseMap.get("content-type");
				if (StringUtils.isBlank(redirectUrl) || StringUtils.isBlank(contentType)) {
					ErrorMessage errorMessage = new ErrorMessage();
					errorMessage.setMessage("Error on get redirect url of an external webservice:  causing "+httpStatusCode);
					errorMessage.setHttpCode(responseMap.get("httpStatusCode").toString());
					if(responseMap.containsKey("redirect-url")) errorMessage.setUrl(responseMap.get("redirect-url").toString());
					if(responseMap.containsKey("content-type")) errorMessage.setContentType(responseMap.get("content-type").toString());
					return (Map<String, Object>) ResponseEntity.status(HttpStatus.BAD_REQUEST)
							.headers(httpHeaders)
							.body(Utils.gson.toJsonTree(errorMessage).toString());
				}

				httpHeaders.add("Location", redirectUrl);
				httpHeaders.add("content-type", contentType);
				return (Map<String, Object>) ResponseEntity.status(HttpStatus.FOUND)
						.headers(httpHeaders)
						.body(new JsonObject().toString());
			}
			else {
				ErrorMessage errorMessage = new ErrorMessage();
				errorMessage.setMessage("Error on external webservice caused by missing information: "+Utils.gson.toJsonTree(responseMap));
				return (Map<String, Object>) ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Utils.gson.toJsonTree(errorMessage).toString());
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
