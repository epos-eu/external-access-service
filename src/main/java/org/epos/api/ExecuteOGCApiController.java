package org.epos.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import kotlin.text.Charsets;

import org.apache.commons.lang3.StringUtils;
import org.epos.api.utility.Utils;
import org.epos.core.ExternalAccessHandler;
import org.epos.core.ExternalServicesRequest;
import org.epos.core.URLGeneration;
import org.epos.core.beans.ErrorMessage;
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
import org.springframework.web.bind.annotation.RequestParam;
import javax.validation.constraints.*;
import javax.validation.Valid;
import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiFunction;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-10-11T14:51:06.469Z[GMT]")
@RestController
public class ExecuteOGCApiController extends ApiController implements ExecuteOGCApi {

	private static final String A_PROBLEM_WAS_ENCOUNTERED_DECODING = "A problem was encountered decoding: ";

	private static final Logger LOGGER = LoggerFactory.getLogger(ExecuteOGCApiController.class);

	private final ObjectMapper objectMapper;

	private final HttpServletRequest request;

	private static Gson gson = new Gson();

	@org.springframework.beans.factory.annotation.Autowired
	public ExecuteOGCApiController(ObjectMapper objectMapper, HttpServletRequest request) {
		super(request);
		this.objectMapper = objectMapper;
		this.request = request;
	}

	@Override
	public ResponseEntity<Object> tcsconnectionsOGCExecuteGet(@NotNull @Parameter(in = ParameterIn.PATH, description = "the id of item to be executed" ,required=true,schema=@Schema()) @PathVariable("id") String id) {

		Map<String, Object> idMap = new HashMap<>();
		idMap.put("id", id);
		
		Map<String, Object> parametersMap = new HashMap<>();
		parametersMap.put("query", request.getQueryString());
		
		return redirectRequest(ServiceType.EXTERNAL, idMap, parametersMap);
	}
	
	private ResponseEntity<Object> redirectRequest(ServiceType service, Map<String, Object> requestParams, Map<String, Object> otherParams) {

		Response response;

		response = doRequest(service, requestParams);
		
		System.out.println(response);

		JsonObject payObj = gson.fromJson(response.getPayloadAsPlainText().get(), JsonObject.class);
		String itemID = payObj.get("id").getAsString();
		JsonObject conversion = null;

		if (payObj.has("conversion")) {
			conversion = payObj.get("conversion").getAsJsonObject();
		}
		
		System.out.println(payObj.getAsJsonObject().get("serviceEndpoint").getAsString());
		
		String compiledUrl = payObj.getAsJsonObject().get("serviceEndpoint").getAsString().split("\\?")[0].replaceAll("\\?", "")+"?"+otherParams.get("query");
		
		System.out.println(compiledUrl);
		
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
			if(compiledUrl.contains("GetMap")) {
				//System.out.println(ExternalServicesRequest.getInstance().requestPayloadImage(compiledUrl));
				String base64image = ExternalServicesRequest.getInstance().requestPayloadImage(compiledUrl);
				httpHeaders.setContentLength(base64image.length());
				httpHeaders.remove("Transfer-Encoding");
				
				return ResponseEntity.status(HttpStatus.OK)
						.headers(httpHeaders)
						.body(base64image);
			}
			
			if(compiledUrl.contains("GetTile")) {
				//System.out.println(ExternalServicesRequest.getInstance().requestPayloadImage(compiledUrl));
				String base64image = ExternalServicesRequest.getInstance().requestPayloadImage(compiledUrl);
				httpHeaders.setContentLength(base64image.length());
				httpHeaders.remove("Transfer-Encoding");
				
				return ResponseEntity.status(HttpStatus.OK)
						.headers(httpHeaders)
						.body(base64image);
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
