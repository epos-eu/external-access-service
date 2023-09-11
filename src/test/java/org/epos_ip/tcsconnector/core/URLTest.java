package org.epos_ip.tcsconnector.core;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.epos.api.utility.Utils;
import org.epos.core.ExternalServicesRequest;
import org.epos.core.URLGeneration;
import org.epos.core.beans.ErrorMessage;
import org.epos.router_framework.domain.Actor;
import org.epos.router_framework.domain.BuiltInActorType;
import org.epos.router_framework.types.ServiceType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class URLTest {

	public static void main(String[] args) throws IOException {

		String template1 = "https://gnssproducts.epos.ubi.pt/GlassFramework/webresources/products/velocities/{station}/UGA-CNRS/{coordinates_system}/{format}";

		HashMap<String, Object> parameters = new HashMap<>();
		parameters.put("coordinates_system", "enu");
		parameters.put("station", "AQUI");
		parameters.put("format", "json");

		String compiledUrl = null;
		compiledUrl = URLGeneration.generateURLFromTemplateAndMap(template1, parameters);

		System.out.println("URL Prerefined:" + compiledUrl);

		try {
			if (URLGeneration.removeQueryParameter(compiledUrl, null) != null)
				compiledUrl = URLGeneration.removeQueryParameter(compiledUrl, null);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		System.out.println("Final URL \n"+compiledUrl);

		System.out.println("Redirect");
		Map<String, Object> handlerResponse = ExternalServicesRequest.getInstance().getRedirect(compiledUrl);



		String responseCode = "OK";
		JsonObject outputResponse = null;

		if (handlerResponse.containsKey("httpStatusCode")) {
			responseCode = HttpStatus.valueOf(Integer.parseInt((String) handlerResponse.get("httpStatusCode"))).name();
		} else {
			if(handlerResponse.get("content")==null) {
				ErrorMessage errorMessage = new ErrorMessage();
				errorMessage.setMessage("Error missing content from "+ handlerResponse);
				System.out.println(ResponseEntity.status(HttpStatus.NO_CONTENT)
						.body(Utils.gson.toJsonTree(errorMessage).toString()));
			}

			try {
				outputResponse = Utils.gson.fromJson(handlerResponse.toString(), JsonElement.class).getAsJsonObject();

				System.out.println(ResponseEntity.status(HttpStatus.OK)
						.body(outputResponse.get("content").getAsJsonObject().toString()));
			}catch(Exception e) {
				ErrorMessage errorMessage = new ErrorMessage();
				errorMessage.setMessage("Error missing content from "+ e.getLocalizedMessage());
				System.out.println(ResponseEntity.status(HttpStatus.NO_CONTENT)
						.body(Utils.gson.toJsonTree(errorMessage).toString()));
			}
		}
		System.out.println(responseCode);

		switch (responseCode) {
		case "OK" : {
			System.out.println("----- OK -----");
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
					errorMessage.setMessage("Error on get redirect url of an external webservice: causing "+httpStatusCode);
					errorMessage.setHttpCode(handlerResponse.get("httpStatusCode").toString());
					if(handlerResponse.containsKey("redirect-url")) errorMessage.setUrl(handlerResponse.get("redirect-url").toString());
					if(handlerResponse.containsKey("content-type")) errorMessage.setContentType(handlerResponse.get("content-type").toString());
					System.out.println(ResponseEntity.status(HttpStatus.BAD_REQUEST)
							.headers(httpHeaders)
							.body(Utils.gson.toJsonTree(errorMessage).toString()));
				}

				httpHeaders.add("Location", redirectUrl);
				httpHeaders.add("content-type", contentType);
				System.out.println(ResponseEntity.status(HttpStatus.FOUND)
						.headers(httpHeaders)
						.body(new JsonObject().toString()));
			}
			else {
				ErrorMessage errorMessage = new ErrorMessage();
				errorMessage.setMessage("Error on external webservice caused by missing information: "+Utils.gson.toJsonTree(handlerResponse));
				System.out.println(ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Utils.gson.toJsonTree(errorMessage).toString()));
			}
			break;
		}
		default : {
			ErrorMessage errorMessage = new ErrorMessage();
			errorMessage.setMessage("Received response "+responseCode+" from external webservice");
			errorMessage.setHttpCode(responseCode);
			if(handlerResponse.containsKey("redirect-url")) errorMessage.setUrl(handlerResponse.get("redirect-url").toString());
			if(handlerResponse.containsKey("content-type")) errorMessage.setContentType(handlerResponse.get("content-type").toString());
			System.out.println("----- default -----");
			System.out.println(ResponseEntity.status(HttpStatus.valueOf(responseCode))
					.body(Utils.gson.toJsonTree(errorMessage).toString()));
			break;
		}
		}
		
	}

}
