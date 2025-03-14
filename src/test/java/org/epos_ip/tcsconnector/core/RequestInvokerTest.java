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
package org.epos_ip.tcsconnector.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.epos.core.ExternalServicesRequestOLD;
import org.json.JSONException;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.diff.Diff;

/**
 * Developer test for use with {@link RequestLegacyTest}: The RequestLegacyTest is used to generate files from the outputs
 * of the legacy <code>org.epos_ip.tcsconnector.core.Request</code> code.
 * 
 * As the content of the generated response files is dependent upon the external services this test should be used as a developer
 * aid only and not included as part of the build. 
 */
@Ignore
public class RequestInvokerTest {
	
	private static final Logger LOG = LoggerFactory.getLogger(RequestInvokerTest.class);
	
    @BeforeClass
    public static void setup() {
    	LOG.info("defaultCharacterEncoding by charSet:         " + Charset.defaultCharset());
    	LOG.info("defaultCharacterEncoding by System property: " + System.getProperty("file.encoding"));
    }
    
	@Test
	public void testRequestFileDownloadInfo_OK() {
		
		final String redirectUrl = "redirect-url";
		final String contentType = "content-type";
		final String httpStatusCode = "httpStatusCode";
		
		Map<String, Map<String, String>> testData = new HashMap<>();
		 
// No Redirects expected
	// Non-EPOS test sites
		 // Non-secure
		 testData.put("http://webcode.me", Map.of(
				 redirectUrl, "http://webcode.me",
				 contentType, "text/html",
				 httpStatusCode, "200"));

		 // Secure
		 testData.put("https://httpbin.org/get", Map.of(
				redirectUrl, "https://httpbin.org/get",
				contentType, "application/json",
				httpStatusCode, "200"));
		 
		// Secure self-signed cert - see https://badssl.com/
		 testData.put("https://superfish.badssl.com", Map.of(
				 redirectUrl, "https://superfish.badssl.com",
				 contentType, "text/html",
				 httpStatusCode, "200"));
		 
		 // Secure signed by invalid CA but root cert has same subject and issuer so still considered as self-signed - see https://badssl.com/
		 testData.put("https://untrusted-root.badssl.com", Map.of(
				 redirectUrl, "https://untrusted-root.badssl.com",
				 contentType, "text/html",
				 httpStatusCode, "200"));
		 
		 // Secure CA signed invalid (expired) cert - see https://badssl.com/
		 testData.put("https://expired.badssl.com", Map.of(
				 redirectUrl, "https://expired.badssl.com",
				 contentType, "",
				 httpStatusCode, ""));
 
		 // Secure: self-signed but will still fail SSL handshake due to no matching DNS names
		 testData.put("https://www.blanchardgenealogy.com/", Map.of(		// Due to SSL failure: "No subject alternative DNS name matching"
	 			 redirectUrl, "https://www.blanchardgenealogy.com/",
	 			 contentType, "",
	 			 httpStatusCode, ""));
			
	// EPOS test sites
		 // Non-secure
		 testData.put("http://geofon.gfz-potsdam.de/fdsnws/station/1/query?starttime=2010-01-01T00:00:00&endtime=2018-01-01T00:00:00&network=*&station=*&location=*&channel=*&minlatitude=-90&maxlatitude=90&minlongitude=-180&maxlongitude=180&level=station", Map.of(
				 redirectUrl, "http://geofon.gfz-potsdam.de/fdsnws/station/1/query?starttime=2010-01-01T00:00:00&endtime=2018-01-01T00:00:00&network=*&station=*&location=*&channel=*&minlatitude=-90&maxlatitude=90&minlongitude=-180&maxlongitude=180&level=station",
				 contentType, "application/xml",
				 httpStatusCode, "200"));
		 
		 testData.put("http://services.seismofaults.eu/geoserver/EDSF/ows?service=WFS&version=2.0.0&request=getFeature&typeNames=EDSF:crustal_fault_sources_top&outputFormat=json&srsName=EPSG:4326&bbox=30.303092956543%2C-12.3925075531006%2C52.0%2C45.2800407409668", Map.of(
				 redirectUrl, "http://services.seismofaults.eu/geoserver/EDSF/ows?service=WFS&version=2.0.0&request=getFeature&typeNames=EDSF:crustal_fault_sources_top&outputFormat=json&srsName=EPSG:4326&bbox=30.303092956543%2C-12.3925075531006%2C52.0%2C45.2800407409668",
				 contentType, "application/json;charset=UTF-8",
				 httpStatusCode, "200"));
		 
		 testData.put("http://eida.gein.noa.gr/fdsnws/station/1/query?starttime=2019-01-01T00:00:00&endtime=2019-01-01T00:00:00&network=*&station=*&location=*&channel=*&minlatitude=-90&maxlatitude=90&minlongitude=-180&maxlongitude=180&level=station", Map.of(
				 redirectUrl, "http://eida.gein.noa.gr/fdsnws/station/1/query?starttime=2019-01-01T00:00:00&endtime=2019-01-01T00:00:00&network=*&station=*&location=*&channel=*&minlatitude=-90&maxlatitude=90&minlongitude=-180&maxlongitude=180&level=station",
				 contentType, "application/xml",
				 httpStatusCode, "200"));

		 testData.put("http://hotvolc.opgc.fr/www/php/data_catalog.php?date_begin=2019-07-19T04:00:00Z&date_end=2019-07-19T08:00:00Z&volcano=Etna&ddss=WP11-DDSS-049&count=50", Map.of(
				 redirectUrl, "http://hotvolc.opgc.fr/www/php/data_catalog.php?date_begin=2019-07-19T04:00:00Z&date_end=2019-07-19T08:00:00Z&volcano=Etna&ddss=WP11-DDSS-049&count=50",
				 contentType, "application/json",
				 httpStatusCode, "200"));
		 
		 // Secure
		 testData.put("https://tcs.ah-epos.eu/api/epos/episodes", Map.of(
				 redirectUrl, "https://tcs.ah-epos.eu/api/epos/episodes",
				 contentType, "application/json;charset=UTF-8",
				 httpStatusCode, "200"));
		 
		 testData.put("https://tcs.ah-epos.eu/api/epos/apps", Map.of(
				 redirectUrl, "https://tcs.ah-epos.eu/api/epos/apps",
				 contentType, "application/json;charset=UTF-8",
				 httpStatusCode, "200"));
		 
		 testData.put("https://data.geoscience.earth/api/wfsBorehole", Map.of(
				 redirectUrl, "https://data.geoscience.earth/api/wfsBorehole",
				 contentType, "application/xml",
				 httpStatusCode, "200"));
		 
		 testData.put("https://data.geoscience.earth/api/wmsMine?service\\u003dWMS\\u0026version\\u003d1.3.0\\u0026request\\u003dGetMap\\u0026layers\\u003dMine\\u0026crs\\u003dEPSG:4326\\u0026format\\u003dimage/png\\u0026width\\u003d1536\\u0026height\\u003d660\\u0026bbox\\u003d-90,-180,90,180&SERVICE=WMS&VERSION=1.3.0&REQUEST=GetCapabilities", Map.of(
				 redirectUrl, "https://data.geoscience.earth/api/wmsMine?service\\u003dWMS\\u0026version\\u003d1.3.0\\u0026request\\u003dGetMap\\u0026layers\\u003dMine\\u0026crs\\u003dEPSG:4326\\u0026format\\u003dimage/png\\u0026width\\u003d1536\\u0026height\\u003d660\\u0026bbox\\u003d-90,-180,90,180&SERVICE=WMS&VERSION=1.3.0&REQUEST=GetCapabilities",
				 contentType, "text/xml; charset=UTF-8",
				 httpStatusCode, "200"));
		 
		 testData.put("https://www.emidius.eu/fdsnws/event/1/query?starttime=1742&endtime=1743-02-20", Map.of(
				 redirectUrl, "https://www.emidius.eu/fdsnws/event/1/query?starttime=1742&endtime=1743-02-20",
				 contentType, "text/xml; charset=utf-8",
				 httpStatusCode, "200"));
		
		 testData.put("https://www.orfeus-eu.org/fdsnws/station/1/query?starttime=2010-01-01T00:00:00&endtime=2019-03-01T00:00:00&network=NL&station=HGN", Map.of(
				 redirectUrl, "https://www.orfeus-eu.org/fdsnws/station/1/query?starttime=2010-01-01T00:00:00&endtime=2019-03-01T00:00:00&network=NL&station=HGN",
				 contentType, "application/xml",
				 httpStatusCode, "200"));
		 
		// Redirect expected (N.b. by design the redirect here is not catered for!)
			//Non-EPOS test sites
				 // Secure
		 testData.put("https://httpbin.org/redirect/3", Map.of(
				 redirectUrl, "https://httpbin.org/redirect/3",
				 contentType, "application/json",
				 httpStatusCode, "200"));
		
		 testData.keySet().forEach(k -> {			
			Map<String, String> expected = testData.get(k);
			Map<String, Object> actual = null;
			try {
				actual = ExternalServicesRequestOLD.getInstance().getRedirect(k);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		    String respMapAsString = getMapAsString(actual);
		    System.out.println(respMapAsString);
			
		    String actualContentType = (String) actual.get(contentType);
		    String actualHttpStatusCode = (String) actual.get(httpStatusCode);
		    
			assertEquals(expected.get(redirectUrl), actual.get(redirectUrl));
			assertEquals(expected.get(contentType), actualContentType == null ? "" : actualContentType);
			assertEquals(expected.get(httpStatusCode), actualHttpStatusCode == null ? "" : actualHttpStatusCode);
		});
		
	}
	
	
	@Test
	public void testRequestFileDownloadInfo_FAIL() {
		
		final String redirectUrl = "redirect-url";
		final String contentType = "content-type";
		final String httpStatusCode = "httpStatusCode";
		
		 Map<String, Map<String, String>> testData = new HashMap<>();
		 
		 // Timeout
//		 testData.put("https://data.geoscience.earth/api/wfsBorehole?service\\u003dWFS\\u0026request\\u003dGetFeature\\u0026typenames\\u003dgsmlp:BoreholeView\\u0026version\\u003d2.0.2\\u0026outputFormat\\u003djson\\u0026bbox\\u003d-90,-180,90,180", Map.of(
//				 redirectUrl, "https://data.geoscience.earth/api/wfsBorehole?service\\u003dWFS\\u0026request\\u003dGetFeature\\u0026typenames\\u003dgsmlp:BoreholeView\\u0026version\\u003d2.0.2\\u0026outputFormat\\u003djson\\u0026bbox\\u003d-90,-180,90,180",
//				 contentType, "",
//				 httpStatusCode, ""));

		 testData.keySet().forEach(k -> {			
			Map<String, String> expected = testData.get(k);
			Map<String, Object> actual = null;
			try {
				actual = ExternalServicesRequestOLD.getInstance().getRedirect(k);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		    String respMapAsString = getMapAsString(actual);
		    System.out.println(respMapAsString);
			
		    String actualContentType = (String) actual.get(contentType);
		    String actualHttpStatusCode = (String) actual.get(httpStatusCode);
		    
			assertEquals(expected.get(redirectUrl), actual.get(redirectUrl));
			assertEquals(expected.get(contentType), actualContentType == null ? "" : actualContentType);
			assertEquals(expected.get(httpStatusCode), actualHttpStatusCode == null ? "" : actualHttpStatusCode);
		});

	}

	@Test
	public void testRequestPayload_TEXT_HTML() {
		
		Map<String, String> testData = Map.of(
			"http://webcode.me", "webcode.me.dat"
//			"https://httpbin.org/get", "httpbin.org.get.dat"	// dynamic content, not easily predicted!
		);
		
		testData.forEach((uri, fileName) -> {			
			try {
				Path respPayloadFile = Paths.get("src", "test", "resources", "response-payloads", fileName);				
				String expected  = new String(Files.readAllBytes(respPayloadFile));				
				String actual = ExternalServicesRequestOLD.getInstance().requestPayload(uri);
				
				if (LOG.isDebugEnabled()) {
					LOG.debug(String.format("TEXT_HTML:%n%s", actual));
				}
				
				assertEquals(expected, actual);
				
				LOG.info("Tested TEXT response from... " + uri);
				
			} catch (IOException e) {
				LOG.error("Failed to read response payload file, " + fileName, e);
			}
			
		});
	}
	
	@Test
	public void testRequestPayload_APPLICATION_XML() {
		
		Map<String, String> testData = Map.of(
			"http://geofon.gfz-potsdam.de/fdsnws/station/1/query?starttime=2010-01-01T00:00:00&endtime=2018-01-01T00:00:00&network=*&station=*&location=*&channel=*&minlatitude=-90&maxlatitude=90&minlongitude=-180&maxlongitude=180&level=station", "geofon_gfz_potsdam_de_fdsnws_station.xml",
			"https://data.geoscience.earth/api/wfsBorehole", "data_geoscience_earth-api-wfsBorehole.xml",
			"https://data.geoscience.earth/api/wmsMine?service\u003dWMS\u0026version\u003d1.3.0\u0026request\u003dGetMap\u0026layers\u003dMine\u0026crs\u003dEPSG:4326\u0026format\u003dimage/png\u0026width\u003d1536\u0026height\u003d660\u0026bbox\u003d-90,-180,90,180&SERVICE=WMS&VERSION=1.3.0&REQUEST=GetCapabilities", "data_geoscience_earth-api-wsmine.xml",
			"https://www.emidius.eu/fdsnws/event/1/query?starttime=1742&endtime=1743-02-20", "emidius-eu_fdsnws_event.xml",
			"http://eida.gein.noa.gr/fdsnws/station/1/query?starttime=2019-01-01T00:00:00&endtime=2019-01-01T00:00:00&network=*&station=*&location=*&channel=*&minlatitude=-90&maxlatitude=90&minlongitude=-180&maxlongitude=180&level=station", "eida-gein-noa-gr_fdsnws_station.xml",
			"https://www.orfeus-eu.org/fdsnws/station/1/query?starttime=2010-01-01T00:00:00&endtime=2019-03-01T00:00:00&network=NL&station=HGN", "orfeus-eu-org_fdsnws_station.xml"
		);

		testData.forEach((uri, fileName) -> {			
			try {
				Path respPayloadFile = Paths.get("src", "test", "resources", "response-payloads", fileName);				
				String expected  = new String(Files.readString(respPayloadFile));				
				String actual = ExternalServicesRequestOLD.getInstance().requestPayload(uri);
				
				LOG.info("Testing XML response from... " + uri);
				
				if (LOG.isDebugEnabled()) {
					LOG.debug(String.format("APPLICATION_XML:%n%s", actual));
				}

			    Diff differ = DiffBuilder
			    	      .compare(expected)
			    	      .withTest(actual)
			    	      .withNodeFilter(node -> !node.getNodeName().equals("Created") && !node.getNodeName().equals("creationTime"))
			    	      .build();
			   assertFalse(differ.hasDifferences());
			    
			   LOG.info("Tested XML response from... " + uri);
			   
			} catch (IOException e) {
				LOG.error("Failed to read response payload file, " + fileName, e);
			}
			
		});
	}

	
	@Test
	public void testRequestPayload_APPLICATION_JSON() {
		
		Map<String, String> testData = Map.of(
			"http://services.seismofaults.eu/geoserver/EDSF/ows?service=WFS&version=2.0.0&request=getFeature&typeNames=EDSF:crustal_fault_sources_top&outputFormat=json&srsName=EPSG:4326&bbox=30.303092956543%2C-12.3925075531006%2C52.0%2C45.2800407409668", "seismofaults_geoserver_EDSF_ows.dat",
			"https://tcs.ah-epos.eu/api/epos/episodes", "tcs.ah-epos.eu_api_epos_episodes.json",
			"https://tcs.ah-epos.eu/api/epos/apps", "tcs.ah-epos.eu_api_epos_apps.json",
			"https://api.vedur.is/epos/v1/hazard_maps_meta?hazard_type=tephra fallout&bbox=63,-25,68.5,-10", "vedur-is_epos_v1_hazard_maps_meta.json",
			// previous implementation (org.epos_ip.tcsconnector.core.Request failed to deal with redirects involving switch of protocol i.e. http <-> https
			"http://hotvolc.opgc.fr/www/php/data_catalog.php?date_begin=2010-05-06T00:00:00Z&date_end=2010-06-06T00:00:00Z&ddss=WP11-DDSS-047&count=50&bbox=(-28,57,-1.5,68.5)", "hotvolc-opgc-fr_php_data_catalog.json"
		);

		testData.forEach((uri, fileName) -> {			
			try {
				Path respPayloadFile = Paths.get("src", "test", "resources", "response-payloads", fileName);				
				String expected  = new String(Files.readString(respPayloadFile));				
				String actual = ExternalServicesRequestOLD.getInstance().requestPayload(uri);
				
				if (LOG.isDebugEnabled()) {
					LOG.debug(String.format("APPLICATION_JSON:%n%s", actual));
				}

			   JSONAssert.assertEquals(expected, actual,
			            new CustomComparator(JSONCompareMode.LENIENT,
			                    new Customization("timeStamp", (o1, o2) -> true)
			            )); 
			   
			   LOG.info("Tested JSON response from... " + uri);
			   
			} catch (IOException e) {
				LOG.error("Failed to read response payload file, " + fileName, e);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		});
	}
	
	private String getMapAsString(Map<String, Object> actual) {
		String respMapAsString = actual.keySet().stream()
				.map(key -> key + "=" + actual.get(key))
				.collect(Collectors.joining(", ", "{", "}"));
		return respMapAsString;
	}
}
