package org.epos_ip.tcsconnector.core;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.epos.api.utility.Utils;
import org.epos.core.ExternalAccessHandler;
import org.epos.core.beans.ErrorMessage;
import org.epos.router_framework.types.ServiceType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class AppTest {

	public static void main(String[] args) {

		//String payload = "{\"operationid\":\"https://www.epos-eu.org/epos-dcat-ap/Seismology/Dataset/001/EMSC/operation\",\"href\":\"https://ics-c.epos-ip.org/demo/k8s-epos-deploy/operational-testing/api/webapi/v1.3/resources/details?id=https://www.epos-eu.org/epos-dcat-ap/Seismology/Dataset/001/EMSC/Distribution\",\"id\":\"https://www.epos-eu.org/epos-dcat-ap/Seismology/Dataset/001/EMSC/Distribution\",\"type\":\"WEB_SERVICE\",\"title\":\"Parameters of modern earthquakes (1998-present) - FDSN event\",\"description\":\"The distribution provides access to worldwide seismic event data collected in real-time. Its encoded in QuakeML format v1.2 or in a column based csv format.\",\"license\":\"https://www.emsc-csem.org/policy.php\",\"downloadURL\":\"\",\"keywords\":[\"earthquake catalogue\",\"seismology\",\"earthquake\",\"magnitude\",\"event\",\"fdsnws-event\",\"seismicity\"],\"dataProvider\":[\"European-Mediterranean Seismological Centre\"],\"frequencyUpdate\":\"http://purl.org/cld/freq/continuous\",\"internalID\":[\"WP08-DDSS-017\"],\"spatial\":{\"wkid\":4326,\"paths\":[[[-180.0,90.0],[180.0,90.0],[180.0,-90.0],[-180.0,-90.0],[-180.0,90.0]]]},\"temporalCoverage\":{\"startDate\":\"1998-01-01T00:00\"},\"scienceDomain\":[\"epos:SeismicParametrics\"],\"hasQualityAnnotation\":\"https://www.emsc-csem.org/Files/epos/specifications/Specs_fdsnevent-WS.pdf\",\"availableFormats\":[{\"label\":\"TEXT\",\"format\":\"text\",\"href\":\"https://ics-c.epos-ip.org/demo/k8s-epos-deploy/operational-testing/api/webapi/v1.3/execute?id=https://www.epos-eu.org/epos-dcat-ap/Seismology/Dataset/001/EMSC/Distribution&format=text\",\"type\":\"ORIGINAL\"},{\"label\":\"JSON\",\"format\":\"json\",\"href\":\"https://ics-c.epos-ip.org/demo/k8s-epos-deploy/operational-testing/api/webapi/v1.3/execute?id=https://www.epos-eu.org/epos-dcat-ap/Seismology/Dataset/001/EMSC/Distribution&format=json\",\"type\":\"ORIGINAL\"},{\"label\":\"XML\",\"format\":\"xml\",\"href\":\"https://ics-c.epos-ip.org/demo/k8s-epos-deploy/operational-testing/api/webapi/v1.3/execute?id=https://www.epos-eu.org/epos-dcat-ap/Seismology/Dataset/001/EMSC/Distribution&format=xml\",\"type\":\"ORIGINAL\"},{\"label\":\"GEOJSON\",\"format\":\"application/epos.geo+json\",\"href\":\"https://ics-c.epos-ip.org/demo/k8s-epos-deploy/operational-testing/api/webapi/v1.3/execute?id=https://www.epos-eu.org/epos-dcat-ap/Seismology/Dataset/001/EMSC/Distribution&format=application/epos.geo+json\",\"type\":\"CONVERTED\"}],\"serviceName\":\"Seismic events collected by the European-Mediterranean Seismological Centre\",\"serviceDescription\":\"This web service provides acces to seismic event parameters. It is implemented according to the fdsnws-event standart defined by the Federation of Digital Seismograph Networks (FDSN). It returns no more than 1000 measurements per requests.\",\"serviceProvider\":\"European-Mediterranean Seismological Centre\",\"serviceSpatial\":{\"wkid\":4326,\"paths\":[[[-180.0,90.0],[180.0,90.0],[180.0,-90.0],[-180.0,-90.0],[-180.0,90.0]]]},\"serviceTemporalCoverage\":{},\"serviceEndpoint\":\"https://www.seismicportal.eu/fdsnws/event/1/query\",\"serviceDocumentation\":\"https://www.emsc-csem.org/Files/epos/specifications/Specs_fdsnevent-WS.pdf\",\"serviceType\":[\"epos:SeismicParametrics\"],\"endpoint\":\"https://www.seismicportal.eu/fdsnws/event/1/query{?starttime, endtime, minlatitude, maxlatitude, minlongitude, maxlongitude, mindepth, maxdepth, minmagnitude, maxmagnitude, includeallorigins, includearrivals, eventid, limit, offset, orderby, contributor, catalog, updatedafter, format, nodata, latitude, longitude, minradius, maxradius}\",\"serviceParameters\":[{\"name\":\"contributor\",\"label\":\"Contributor\",\"type\":\"string\",\"required\":false},{\"name\":\"minlatitude\",\"label\":\"Minimum latitude (deg)\",\"type\":\"float\",\"minValue\":\"-90\",\"maxValue\":\"90\",\"defaultValue\":\"30\",\"value\":\"30\",\"property\":\"epos:southernmostLatitude\",\"required\":false},{\"name\":\"maxlatitude\",\"label\":\"Maximum latitude (deg)\",\"type\":\"float\",\"minValue\":\"-90\",\"maxValue\":\"90\",\"defaultValue\":\"90\",\"value\":\"90\",\"property\":\"epos:northernmostLatitude\",\"required\":false},{\"name\":\"minmagnitude\",\"label\":\"Minimum magnitude\",\"type\":\"float\",\"defaultValue\":\"5\",\"value\":\"5\",\"required\":false},{\"name\":\"format\",\"label\":\"Output Format\",\"type\":\"string\",\"Enum\":[\"text\",\"json\",\"xml\"],\"defaultValue\":\"xml\",\"value\":\"xml\",\"property\":\"schema:encodingFormat\",\"required\":false},{\"name\":\"offset\",\"label\":\"Offset\",\"type\":\"integer\",\"minValue\":\"1\",\"required\":false},{\"name\":\"limit\",\"label\":\"Limit the no. of output entries\",\"type\":\"integer\",\"minValue\":\"1\",\"maxValue\":\"1000\",\"defaultValue\":\"250\",\"value\":\"250\",\"required\":true},{\"name\":\"maxradius\",\"label\":\"Maximum radius (deg)\",\"type\":\"float\",\"minValue\":\"0\",\"maxValue\":\"180\",\"required\":false},{\"name\":\"maxdepth\",\"label\":\"Maximum depth (km)\",\"type\":\"integer\",\"minValue\":\"0\",\"maxValue\":\"6371\",\"required\":false},{\"name\":\"latitude\",\"label\":\"Radius search latitude (deg)\",\"type\":\"float\",\"minValue\":\"-90\",\"maxValue\":\"90\",\"required\":false},{\"name\":\"mindepth\",\"label\":\"Minimum depth (km)\",\"type\":\"integer\",\"minValue\":\"0\",\"maxValue\":\"6371\",\"required\":false},{\"name\":\"includearrivals\",\"label\":\"Include All Arrivals\",\"type\":\"boolean\",\"Enum\":[\"false\",\"true\"],\"defaultValue\":\"false\",\"value\":\"false\",\"required\":false},{\"name\":\"magnitudetype\",\"label\":\"Type of Magnitude\",\"type\":\"string\",\"readOnlyValue\":\"true\",\"required\":false},{\"name\":\"maxlongitude\",\"label\":\"Maximum longitude (deg)\",\"type\":\"float\",\"minValue\":\"-180\",\"maxValue\":\"180\",\"defaultValue\":\"50\",\"value\":\"50\",\"property\":\"epos:easternmostLongitude\",\"required\":false},{\"name\":\"updatedafter\",\"label\":\"Is Updated After\",\"type\":\"dateTime\",\"readOnlyValue\":\"true\",\"required\":false},{\"name\":\"catalog\",\"label\":\"Catalogue\",\"type\":\"string\",\"required\":false},{\"name\":\"starttime\",\"label\":\"Start Time\",\"type\":\"dateTime\",\"minValue\":\"1998-01-01T00:00:00\",\"maxValue\":\"3000-12-31T00:00:00\",\"property\":\"schema:startDate\",\"valuePattern\":\"YYYY-MM-DDThh:mm:ss\",\"required\":false},{\"name\":\"endtime\",\"label\":\"End Time\",\"type\":\"dateTime\",\"minValue\":\"1998-01-01T00:00:00\",\"maxValue\":\"3000-12-31T00:00:00\",\"property\":\"schema:endDate\",\"valuePattern\":\"YYYY-MM-DDThh:mm:ss\",\"required\":false},{\"name\":\"maxmagnitude\",\"label\":\"Maximum magnitude\",\"type\":\"float\",\"required\":false},{\"name\":\"minradius\",\"label\":\"Minimum radius (deg)\",\"type\":\"float\",\"minValue\":\"0\",\"maxValue\":\"180\",\"required\":false},{\"name\":\"nodata\",\"label\":\"HTTP status code for 'no data', either '204' (default) or '404'\",\"type\":\"string\",\"Enum\":[\"404\",\"204\"],\"defaultValue\":\"204\",\"value\":\"204\",\"readOnlyValue\":\"true\",\"required\":false},{\"name\":\"longitude\",\"label\":\"Radius search longitude (deg)\",\"type\":\"float\",\"minValue\":\"-180\",\"maxValue\":\"180\",\"required\":false},{\"name\":\"minlongitude\",\"label\":\"Minimum longitude (deg)\",\"type\":\"float\",\"minValue\":\"-180\",\"maxValue\":\"180\",\"defaultValue\":\"-40\",\"value\":\"-40\",\"property\":\"epos:westernmostLongitude\",\"required\":false},{\"name\":\"eventid\",\"label\":\"Event Identifier\",\"type\":\"string\",\"required\":false},{\"name\":\"orderby\",\"label\":\"Order By\",\"type\":\"string\",\"Enum\":[\"magnitude-asc\",\"time-asc\",\"time\",\"magnitude\"],\"readOnlyValue\":\"true\",\"required\":false},{\"name\":\"includeallorigins\",\"label\":\"Include All Origins\",\"type\":\"boolean\",\"Enum\":[\"true\",\"false\"],\"defaultValue\":\"false\",\"value\":\"false\",\"required\":false}],\"params\":{\"format\":\"application/epos.geo+json\",\"id\":\"https://www.epos-eu.org/epos-dcat-ap/Seismology/Dataset/001/EMSC/Distribution\",\"params\":\"{\\\"contributor\\\":\\\"\\\",\\\"minlatitude\\\":\\\"30\\\",\\\"maxlatitude\\\":\\\"90\\\",\\\"minmagnitude\\\":\\\"5\\\",\\\"format\\\":\\\"xml\\\",\\\"offset\\\":\\\"\\\",\\\"limit\\\":\\\"250\\\",\\\"maxradius\\\":\\\"\\\",\\\"maxdepth\\\":\\\"\\\",\\\"latitude\\\":\\\"\\\",\\\"mindepth\\\":\\\"\\\",\\\"includearrivals\\\":\\\"false\\\",\\\"magnitudetype\\\":\\\"\\\",\\\"maxlongitude\\\":\\\"50\\\",\\\"updatedafter\\\":\\\"\\\",\\\"catalog\\\":\\\"\\\",\\\"starttime\\\":\\\"\\\",\\\"endtime\\\":\\\"\\\",\\\"maxmagnitude\\\":\\\"\\\",\\\"minradius\\\":\\\"\\\",\\\"nodata\\\":\\\"204\\\",\\\"longitude\\\":\\\"\\\",\\\"minlongitude\\\":\\\"-40\\\",\\\"eventid\\\":\\\"\\\",\\\"orderby\\\":\\\"\\\",\\\"includeallorigins\\\":\\\"false\\\"}\",\"useDefaults\":\"false\"},\"conversion\":{\"operation\":\"https://www.epos-eu.org/epos-dcat-ap/Seismology/Dataset/001/EMSC/operation\",\"requestContentType\":\"application/xml\",\"responseContentType\":\"application/epos.geo+json\"}}";
		
		String payload = "{\"operationid\":\"https://www.epos-eu.org/epos-dcat-ap/Seismology/WebService/AHEAD/restful/fdsnws-event/Operation\",\"href\":\"https://ics-c.epos-ip.org/demo/k8s-epos-deploy/operational-testing/api/webapi/v1.3/resources/details?id=https://www.epos-eu.org/epos-dcat-ap/Seismology/Dataset/AHEAD/events/distribution/restful\",\"id\":\"https://www.epos-eu.org/epos-dcat-ap/Seismology/Dataset/AHEAD/events/distribution/restful\",\"type\":\"WEB_SERVICE\",\"title\":\"Parameters of historical earthquakes (1000-1899) - FDSN event\",\"description\":\"The distribution of event parameters via the FDSN-event web service is the main and preferred way to access historical earthquake data archived in AHEAD.\",\"license\":\"https://www.emidius.eu/AHEAD/description.php#copyright\",\"downloadURL\":\"\",\"keywords\":[\"seismology\",\"magnitude\",\"earthquake\",\"event\",\"catalogue\",\"seismicity\"],\"dataProvider\":[\"National Institute of Geophysics and Volcanology\"],\"frequencyUpdate\":\"http://purl.org/cld/freq/irregular\",\"internalID\":[\"WP08-DDSS-024\"],\"DOI\":[\"10.6092/INGV.IT-AHEAD\"],\"spatial\":{\"wkid\":4326,\"paths\":[[[-34.0,74.0],[45.0,74.0],[45.0,33.0],[-34.0,33.0],[-34.0,74.0]]]},\"temporalCoverage\":{\"startDate\":\"1000-01-01T00:00\",\"endDate\":\"1899-12-31T23:59:59\"},\"scienceDomain\":[\"epos:Historicalearthquakes\"],\"hasQualityAnnotation\":\"https://www.emidius.eu/AHEAD/data_quality_assurance.php\",\"availableFormats\":[{\"label\":\"JSON\",\"format\":\"json\",\"href\":\"https://ics-c.epos-ip.org/demo/k8s-epos-deploy/operational-testing/api/webapi/v1.3/execute?id=https://www.epos-eu.org/epos-dcat-ap/Seismology/Dataset/AHEAD/events/distribution/restful&format=json\",\"type\":\"ORIGINAL\"},{\"label\":\"TEXT\",\"format\":\"text\",\"href\":\"https://ics-c.epos-ip.org/demo/k8s-epos-deploy/operational-testing/api/webapi/v1.3/execute?id=https://www.epos-eu.org/epos-dcat-ap/Seismology/Dataset/AHEAD/events/distribution/restful&format=text\",\"type\":\"ORIGINAL\"},{\"label\":\"XML\",\"format\":\"xml\",\"href\":\"https://ics-c.epos-ip.org/demo/k8s-epos-deploy/operational-testing/api/webapi/v1.3/execute?id=https://www.epos-eu.org/epos-dcat-ap/Seismology/Dataset/AHEAD/events/distribution/restful&format=xml\",\"type\":\"ORIGINAL\"},{\"label\":\"GEOJSON\",\"format\":\"application/epos.geo+json\",\"href\":\"https://ics-c.epos-ip.org/demo/k8s-epos-deploy/operational-testing/api/webapi/v1.3/execute?id=https://www.epos-eu.org/epos-dcat-ap/Seismology/Dataset/AHEAD/events/distribution/restful&format=application/epos.geo+json\",\"type\":\"CONVERTED\"}],\"serviceName\":\"Historical earthquakes 1000-1899 (FDSN-event)\",\"serviceDescription\":\"FDSN-event web service providing event parameters for historical earthquakes from AHEAD, the European Archive of Historical Earthquake Data\",\"serviceProvider\":\"National Institute of Geophysics and Volcanology\",\"serviceSpatial\":{\"wkid\":4326,\"paths\":[[[-33.5,73.5],[34.0,73.5],[34.0,33.5],[-33.5,33.5],[-33.5,73.5]]]},\"serviceTemporalCoverage\":{},\"serviceEndpoint\":\"https://www.emidius.eu/fdsnws/event/1/query\",\"serviceDocumentation\":\"https://www.emidius.eu/AHEAD/services/epos_events.php\",\"serviceType\":[\"epos:Historicalearthquakes\"],\"endpoint\":\"https://www.emidius.eu/fdsnws/event/1/query{?starttime, endtime, eventid, minlatitude, maxlatitude, minlongitude, maxlongitude, latitude, longitude, minradiuskm, maxradiuskm, minmagnitude, maxmagnitude, contributor, contributorid, format, orderby, includeallorigins, includeallmagnitudes, updatedafter, limit, nodata}\",\"serviceParameters\":[{\"name\":\"contributor\",\"label\":\"Data contributor code\",\"type\":\"string\",\"required\":false},{\"name\":\"minmagnitude\",\"label\":\"Minimum magnitude (Mw)\",\"type\":\"float\",\"minValue\":\"0.0\",\"maxValue\":\"10.0\",\"required\":false},{\"name\":\"starttime\",\"label\":\"Start time\",\"type\":\"dateTime\",\"minValue\":\"1000-01-01T00:00:00\",\"maxValue\":\"1899-12-31T23:59:59\",\"defaultValue\":\"1000-01-01T00:00:00\",\"value\":\"1000-01-01T00:00:00\",\"property\":\"schema:startDate\",\"valuePattern\":\"YYYY-MM-DDThh:mm:ss\",\"required\":true},{\"name\":\"includeallorigins\",\"label\":\"Include all origins\",\"type\":\"boolean\",\"Enum\":[\"true\",\"false\"],\"defaultValue\":\"false\",\"value\":\"false\",\"required\":false},{\"name\":\"longitude\",\"label\":\"Circular search center longitude\",\"type\":\"float\",\"minValue\":\"-34.0000\",\"maxValue\":\"34.0000\",\"required\":false},{\"name\":\"orderby\",\"label\":\"Output ordering\",\"type\":\"string\",\"Enum\":[\"magnitude-asc\",\"time\",\"magnitude\",\"time-asc\"],\"defaultValue\":\"time\",\"value\":\"time\",\"required\":false},{\"name\":\"nodata\",\"label\":\"Status code for \",\"type\":\"string\",\"Enum\":[\"404\",\"204\"],\"defaultValue\":\"204\",\"value\":\"204\",\"readOnlyValue\":\"true\",\"required\":false},{\"name\":\"format\",\"label\":\"Output format\",\"type\":\"string\",\"Enum\":[\"json\",\"text\",\"xml\"],\"defaultValue\":\"xml\",\"value\":\"xml\",\"property\":\"schema:encodingFormat\",\"required\":false},{\"name\":\"maxlatitude\",\"label\":\"Maximum latitude\",\"type\":\"float\",\"minValue\":\"33.0000\",\"maxValue\":\"74.0000\",\"defaultValue\":\"74.0000\",\"value\":\"74\",\"property\":\"epos:northernmostLatitude\",\"required\":false},{\"name\":\"updatedafter\",\"label\":\"Update after\",\"type\":\"dateTime\",\"minValue\":\"2010-01-01T00:00:00\",\"required\":false},{\"name\":\"maxradiuskm\",\"label\":\"Circular search max radius (km)\",\"type\":\"float\",\"minValue\":\"0.00\",\"maxValue\":\"500.00\",\"required\":false},{\"name\":\"minradiuskm\",\"label\":\"Circular search min radius (km)\",\"type\":\"float\",\"minValue\":\"0.00\",\"maxValue\":\"500.00\",\"required\":false},{\"name\":\"maxmagnitude\",\"label\":\"Maximum magnitude (Mw)\",\"type\":\"float\",\"minValue\":\"0.0\",\"maxValue\":\"10.0\",\"required\":false},{\"name\":\"minlongitude\",\"label\":\"Minimum longitude\",\"type\":\"float\",\"minValue\":\"-34.0000\",\"maxValue\":\"34.0000\",\"defaultValue\":\"-34.0000\",\"value\":\"-34\",\"property\":\"epos:westernmostLongitude\",\"required\":false},{\"name\":\"maxlongitude\",\"label\":\"Maximum longitude\",\"type\":\"float\",\"minValue\":\"-34.0000\",\"maxValue\":\"34.0000\",\"defaultValue\":\"34.0000\",\"value\":\"34\",\"property\":\"epos:easternmostLongitude\",\"required\":false},{\"name\":\"latitude\",\"label\":\"Circular search center latitude\",\"type\":\"float\",\"minValue\":\"33.0000\",\"maxValue\":\"74.0000\",\"required\":false},{\"name\":\"endtime\",\"label\":\"End time\",\"type\":\"dateTime\",\"minValue\":\"1000-01-01T00:00:00\",\"maxValue\":\"1899-12-31T23:59:59\",\"defaultValue\":\"1899-12-31T23:59:59\",\"value\":\"1899-12-31T23:59:59\",\"property\":\"schema:endDate\",\"valuePattern\":\"YYYY-MM-DDThh:mm:ss\",\"required\":true},{\"name\":\"minlatitude\",\"label\":\"Minimum latitude\",\"type\":\"float\",\"minValue\":\"33.0000\",\"maxValue\":\"74.0000\",\"defaultValue\":\"33.0000\",\"value\":\"33\",\"property\":\"epos:southernmostLatitude\",\"required\":false},{\"name\":\"limit\",\"label\":\"Limit the no. of output entries\",\"type\":\"integer\",\"minValue\":\"1\",\"maxValue\":\"5000\",\"defaultValue\":\"300\",\"value\":\"300\",\"required\":true},{\"name\":\"eventid\",\"label\":\"Event ID\",\"type\":\"string\",\"required\":false},{\"name\":\"includeallmagnitudes\",\"label\":\"Include all magnitudes\",\"type\":\"boolean\",\"Enum\":[\"false\",\"true\"],\"defaultValue\":\"false\",\"value\":\"false\",\"required\":false},{\"name\":\"contributorid\",\"label\":\"Contributor event ID\",\"type\":\"string\",\"required\":false}],\"params\":{\"format\":\"text\",\"id\":\"https://www.epos-eu.org/epos-dcat-ap/Seismology/Dataset/AHEAD/events/distribution/restful\",\"params\":\"{\\\"contributor\\\":\\\"\\\",\\\"minmagnitude\\\":\\\"\\\",\\\"starttime\\\":\\\"1000-01-01T00:00:00Z\\\",\\\"includeallorigins\\\":\\\"false\\\",\\\"longitude\\\":\\\"\\\",\\\"orderby\\\":\\\"time\\\",\\\"nodata\\\":\\\"204\\\",\\\"format\\\":\\\"xml\\\",\\\"maxlatitude\\\":\\\"74\\\",\\\"updatedafter\\\":\\\"\\\",\\\"maxradiuskm\\\":\\\"\\\",\\\"minradiuskm\\\":\\\"\\\",\\\"maxmagnitude\\\":\\\"\\\",\\\"minlongitude\\\":\\\"-34\\\",\\\"maxlongitude\\\":\\\"34\\\",\\\"latitude\\\":\\\"\\\",\\\"endtime\\\":\\\"1899-12-31T23:59:59Z\\\",\\\"minlatitude\\\":\\\"33\\\",\\\"limit\\\":\\\"300\\\",\\\"eventid\\\":\\\"\\\",\\\"includeallmagnitudes\\\":\\\"false\\\",\\\"contributorid\\\":\\\"\\\"}\",\"useDefaults\":\"false\"},\"conversion\":{\"operation\":\"https://www.epos-eu.org/epos-dcat-ap/Seismology/WebService/AHEAD/restful/fdsnws-event/Operation\",\"requestContentType\":\"application/xml\",\"responseContentType\":\"application/epos.geo+json\"}}";
		
		System.out.println(redirectRequestTest(ServiceType.EXTERNAL, new HashMap<String, Object>(), payload));
	}

	private static ResponseEntity<Object> redirectRequestTest(ServiceType service, Map<String, Object> requestParams, String payload) {

		JsonObject payObj = Utils.gson.fromJson(payload, JsonObject.class);
		String itemID = payObj.get("id").getAsString();
		JsonObject conversion = null;

		if (payObj.has("conversion")) {
			conversion = payObj.get("conversion").getAsJsonObject();
			System.out.println(conversion);
		}

		Map<String,Object> handlerResponse = ExternalAccessHandler.handle(payload, "execute");

		System.out.println("RESPONSE: "+handlerResponse);

		String responseCode = "OK";

		if (handlerResponse.containsKey("httpStatusCode")) {
			responseCode = HttpStatus.valueOf(Integer.parseInt((String) handlerResponse.get("httpStatusCode"))).name();
		} else {
			//TODO: converter request
			if(handlerResponse.get("content")==null) {
				ErrorMessage errorMessage = new ErrorMessage();
				errorMessage.setMessage("Error missing content from "+itemID+" "+ handlerResponse);
				return ResponseEntity.status(HttpStatus.NO_CONTENT)
						.body(Utils.gson.toJsonTree(errorMessage));
			}

			String response = "{}";

			if(conversion!=null)
				response = "{\"example\":\"test\"}";


			HttpHeaders httpHeaders = new HttpHeaders();
			if(conversion!=null)
				httpHeaders.add("content-type", conversion.get("responseContentType").getAsString());
			else
				httpHeaders.add("content-type", "application/geo+json");

			JsonObject outputResponse = null;

			System.out.println("Handler "+handlerResponse);


			if(conversion!=null) {
				outputResponse = Utils.gson.fromJson(response, JsonObject.class).getAsJsonObject();
				System.out.println(outputResponse);
				try {
					return ResponseEntity.status(HttpStatus.OK)
							.headers(httpHeaders)
							.body(outputResponse.toString());
				}catch(Exception e) {
					ErrorMessage errorMessage = new ErrorMessage();
					errorMessage.setMessage("Error missing content from "+itemID+" "+ e.getLocalizedMessage());
					return ResponseEntity.status(HttpStatus.NO_CONTENT)
							.headers(httpHeaders)
							.body(Utils.gson.toJsonTree(errorMessage));
				}
			}else {
				try {
					outputResponse = Utils.gson.fromJson(handlerResponse.toString(), JsonElement.class).getAsJsonObject();
					System.out.println(outputResponse);

					return ResponseEntity.status(HttpStatus.OK)
							.headers(httpHeaders)
							.body(outputResponse.get("content").getAsJsonObject().toString());
				}catch(Exception e) {
					ErrorMessage errorMessage = new ErrorMessage();
					errorMessage.setMessage("Error missing content from "+itemID+" "+ e.getLocalizedMessage());
					return ResponseEntity.status(HttpStatus.NO_CONTENT)
							.headers(httpHeaders)
							.body(Utils.gson.toJsonTree(errorMessage));
				}
			}

		}

		switch (responseCode) {

		default : {
			ErrorMessage errorMessage = new ErrorMessage();
			errorMessage.setMessage("Received response "+responseCode+" from external webservice");
			errorMessage.setHttpCode(responseCode);
			if(handlerResponse.containsKey("redirect-url")) errorMessage.setUrl(handlerResponse.get("redirect-url").toString());
			if(handlerResponse.containsKey("content-type")) errorMessage.setContentType(handlerResponse.get("content-type").toString());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Utils.gson.toJsonTree(errorMessage));
		}
		case "OK" : {
			HttpHeaders httpHeaders = new HttpHeaders();

			if(handlerResponse.containsKey("redirect-url") 
					&& handlerResponse.containsKey("content-type")
					&& handlerResponse.containsKey("httpStatusCode")) {

				int _httpStatusCode = Integer.parseInt((String) handlerResponse.get("httpStatusCode"));
				HttpStatus httpStatusCode = HttpStatus.valueOf(_httpStatusCode);

				String redirectUrl = (String) handlerResponse.get("redirect-url");	
				String contentType = (String) handlerResponse.get("content-type");
				if (StringUtils.isBlank(redirectUrl) || StringUtils.isBlank(contentType)) {
					ErrorMessage errorMessage = new ErrorMessage();
					errorMessage.setMessage("Error on get redirect url of an external webservice: "+itemID+ " causing "+httpStatusCode);
					errorMessage.setHttpCode(handlerResponse.get("httpStatusCode").toString());
					if(handlerResponse.containsKey("redirect-url")) errorMessage.setUrl(handlerResponse.get("redirect-url").toString());
					if(handlerResponse.containsKey("content-type")) errorMessage.setContentType(handlerResponse.get("content-type").toString());
					return ResponseEntity.status(HttpStatus.BAD_REQUEST)
							.headers(httpHeaders)
							.body(Utils.gson.toJsonTree(errorMessage));
				}

				httpHeaders.add("Location", redirectUrl);
				httpHeaders.add("content-type", contentType);
				return ResponseEntity.status(HttpStatus.FOUND)
						.headers(httpHeaders)
						.build();
			}
			else {
				ErrorMessage errorMessage = new ErrorMessage();
				errorMessage.setMessage("Error on external webservice caused by missing information: "+Utils.gson.toJsonTree(handlerResponse));
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Utils.gson.toJsonTree(errorMessage));
			}
		}
		}
	}

}
