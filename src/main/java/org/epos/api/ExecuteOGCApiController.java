package org.epos.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;

import org.apache.commons.lang3.StringUtils;
import org.epos.api.beans.Distribution;
import org.epos.api.beans.ErrorMessage;
import org.epos.api.utility.Utils;
import org.epos.core.ExecuteItemGenerationJPA;
import org.epos.core.ExternalServicesRequest;
import org.epos.core.PluginGeneration;
import org.epos.router_framework.domain.Actor;
import org.epos.router_framework.domain.BuiltInActorType;
import org.epos.router_framework.domain.Response;
import org.epos.router_framework.types.ServiceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import javax.validation.constraints.*;
import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-10-11T14:51:06.469Z[GMT]")
@RestController
public class ExecuteOGCApiController extends ApiController implements ExecuteOGCApi {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteOGCApiController.class);

	private final ObjectMapper objectMapper;

	private final HttpServletRequest request;

	@org.springframework.beans.factory.annotation.Autowired
	public ExecuteOGCApiController(ObjectMapper objectMapper, HttpServletRequest request) {
		super(request);
		this.objectMapper = objectMapper;
		this.request = request;
	}

	@Override
	public ResponseEntity<Object> tcsconnectionsOGCExecuteGet(@NotNull @Parameter(in = ParameterIn.PATH, description = "the id of item to be executed" ,required=true,schema=@Schema()) @PathVariable("instance_id") String id) {

		Map<String, Object> idMap = new HashMap<>();
		idMap.put("id", id);

		Map<String, Object> parametersMap = new HashMap<>();
		parametersMap.put("query", request.getQueryString());

		return redirectRequest(ServiceType.EXTERNAL, idMap, parametersMap);
	}

	private ResponseEntity<Object> redirectRequest(ServiceType service, Map<String, Object> requestParams, Map<String, Object> otherParams) {

		Distribution response = ExecuteItemGenerationJPA.generate(requestParams);
		Response conversionResponse = null;
		JsonObject conversion = null;

		if(response.getOperationid()!=null) {
			JsonObject conversionParameters = new JsonObject();
			conversionParameters.addProperty("type", "plugins");
			conversionParameters.addProperty("operation", response.getOperationid());

			JsonArray softwareConversionList = PluginGeneration.generate(new JsonObject(), conversionParameters, "plugin");
			if(!softwareConversionList.isJsonNull() && !softwareConversionList.get(0).isJsonNull()) {
				JsonObject conversionInner = softwareConversionList.get(0).getAsJsonObject();
				JsonObject singleConversion = new JsonObject();
				singleConversion.addProperty("operation", response.getOperationid());
				singleConversion.addProperty("requestContentType", conversionInner
						.get("action").getAsJsonObject()
						.get("object").getAsJsonObject()
						.get("encodingFormat").getAsString());
				singleConversion.addProperty("responseContentType", conversionInner
						.get("action").getAsJsonObject()
						.get("result").getAsJsonObject()
						.get("encodingFormat").getAsString());
				conversion = singleConversion;
			}
		}

		String compiledUrl = response.getServiceEndpoint().split("\\?")[0].replaceAll("\\?", "")+"?"+otherParams.get("query");

		HttpHeaders httpHeaders = new HttpHeaders();

		Map<String, List<String>> headers = new HashMap<String, List<String>>();
		try {
			headers = ExternalServicesRequest.getInstance().requestHeaders(compiledUrl);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		for(String key : headers.keySet()) {
			httpHeaders.put(key,headers.get(key));
		}

		try {

			if(compiledUrl.contains("GetFeatureInfo") && conversion!=null) {
				Map<String, Object> responseMap = new HashMap<>();
				Map<String, String> parametersMap = new HashMap<>();
				parametersMap.put("operation", conversion.get("operation").getAsString());
				parametersMap.put("requestContentType", conversion.get("requestContentType").getAsString());
				parametersMap.put("responseContentType", conversion.get("responseContentType").getAsString());
				responseMap.put("parameters", parametersMap);
				String responsePayload = ExternalServicesRequest.getInstance().requestPayload(compiledUrl);
				responseMap.put("content", responsePayload.length()==0? "{}" : responsePayload);
				conversionResponse = doRequest(ServiceType.EXTERNAL, Actor.getInstance(BuiltInActorType.CONVERTER), responseMap);
				if(conversionResponse!=null) {
					JsonObject outputResponse = Utils.gson.fromJson(conversionResponse.getPayloadAsPlainText().get(), JsonObject.class).getAsJsonObject();
	
					return ResponseEntity.status(HttpStatus.OK)
							.headers(httpHeaders)
							.body(outputResponse.get("content").getAsJsonObject().toString());
				}else {
					ErrorMessage errorMessage = new ErrorMessage();
					errorMessage.setMessage("Error missing conversion from "+response.getDistributionid());
					return ResponseEntity.status(HttpStatus.NO_CONTENT)
							.body(Utils.gson.toJsonTree(errorMessage).toString());
				}
			}


			if(compiledUrl.contains("GetMap")) {
				//System.out.println(ExternalServicesRequest.getInstance().requestPayloadImage(compiledUrl));
				/*String base64image = ExternalServicesRequest.getInstance().requestPayloadImage(compiledUrl);
				httpHeaders.setContentLength(base64image.length());
				httpHeaders.remove("Transfer-Encoding");

				return ResponseEntity.status(HttpStatus.OK)
						.headers(httpHeaders)
						.body(base64image);*/
				
				Map<String, Object> handlerResponse = ExternalServicesRequest.getInstance().getRedirect(compiledUrl);

				if(handlerResponse.containsKey("redirect-url") 
						&& handlerResponse.containsKey("content-type")
						&& handlerResponse.containsKey("httpStatusCode")) {

					int _httpStatusCode = Integer.parseInt((String) handlerResponse.get("httpStatusCode"));
					HttpStatus httpStatusCode = HttpStatus.valueOf(_httpStatusCode);

					String redirectUrl = (String) handlerResponse.get("redirect-url");	
					String contentType = (String) handlerResponse.get("content-type");
					if (StringUtils.isBlank(redirectUrl) || StringUtils.isBlank(contentType)) {
						ErrorMessage errorMessage = new ErrorMessage();
						errorMessage.setMessage("Error on get redirect url of an external webservice: "+response.getDistributionid()+ " causing "+httpStatusCode);
						errorMessage.setHttpCode(handlerResponse.get("httpStatusCode").toString());
						if(handlerResponse.containsKey("redirect-url")) errorMessage.setUrl(handlerResponse.get("redirect-url").toString());
						if(handlerResponse.containsKey("content-type")) errorMessage.setContentType(handlerResponse.get("content-type").toString());
						return ResponseEntity.status(HttpStatus.BAD_REQUEST)
								.headers(httpHeaders)
								.body(Utils.gson.toJsonTree(errorMessage).toString());
					}

					httpHeaders.add("Location", redirectUrl);
					httpHeaders.add("content-type", contentType);
					return ResponseEntity.status(HttpStatus.FOUND)
							.headers(httpHeaders)
							.body(new JsonObject().toString());
				}
				else {
					ErrorMessage errorMessage = new ErrorMessage();
					errorMessage.setMessage("Error on external webservice caused by missing information: "+Utils.gson.toJsonTree(handlerResponse));
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Utils.gson.toJsonTree(errorMessage).toString());
				}
			}

			if(compiledUrl.contains("GetTile")) {
				//System.out.println(ExternalServicesRequest.getInstance().requestPayloadImage(compiledUrl));
				/*String base64image = ExternalServicesRequest.getInstance().requestPayloadImage(compiledUrl);
				httpHeaders.setContentLength(base64image.length());
				httpHeaders.remove("Transfer-Encoding");

				return ResponseEntity.status(HttpStatus.OK)
						.headers(httpHeaders)
						.body(base64image);*/
				Map<String, Object> handlerResponse = ExternalServicesRequest.getInstance().getRedirect(compiledUrl);

				if(handlerResponse.containsKey("redirect-url") 
						&& handlerResponse.containsKey("content-type")
						&& handlerResponse.containsKey("httpStatusCode")) {

					int _httpStatusCode = Integer.parseInt((String) handlerResponse.get("httpStatusCode"));
					HttpStatus httpStatusCode = HttpStatus.valueOf(_httpStatusCode);

					String redirectUrl = (String) handlerResponse.get("redirect-url");	
					String contentType = (String) handlerResponse.get("content-type");
					if (StringUtils.isBlank(redirectUrl) || StringUtils.isBlank(contentType)) {
						ErrorMessage errorMessage = new ErrorMessage();
						errorMessage.setMessage("Error on get redirect url of an external webservice: "+response.getDistributionid()+ " causing "+httpStatusCode);
						errorMessage.setHttpCode(handlerResponse.get("httpStatusCode").toString());
						if(handlerResponse.containsKey("redirect-url")) errorMessage.setUrl(handlerResponse.get("redirect-url").toString());
						if(handlerResponse.containsKey("content-type")) errorMessage.setContentType(handlerResponse.get("content-type").toString());
						return ResponseEntity.status(HttpStatus.BAD_REQUEST)
								.headers(httpHeaders)
								.body(Utils.gson.toJsonTree(errorMessage).toString());
					}

					httpHeaders.add("Location", redirectUrl);
					httpHeaders.add("content-type", contentType);
					return ResponseEntity.status(HttpStatus.FOUND)
							.headers(httpHeaders)
							.body(new JsonObject().toString());
				}
				else {
					ErrorMessage errorMessage = new ErrorMessage();
					errorMessage.setMessage("Error on external webservice caused by missing information: "+Utils.gson.toJsonTree(handlerResponse));
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Utils.gson.toJsonTree(errorMessage).toString());
				}
			}

			return ResponseEntity.status(HttpStatus.OK)
					.headers(httpHeaders)
					.body(ExternalServicesRequest.getInstance().requestPayload(compiledUrl));
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.headers(httpHeaders).build();
		}
	}

}
