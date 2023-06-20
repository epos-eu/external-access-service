package org.epos.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.lang3.StringUtils;
import org.epos.api.utility.Utils;
import org.epos.core.ExternalAccessHandler;
import org.epos.core.beans.CacheData;
import org.epos.core.beans.ErrorMessage;
import org.epos.core.beans.repositories.CacheDataRepository;
import org.epos.router_framework.domain.Response;
import org.epos.router_framework.types.ServiceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestParam;
import javax.validation.Valid;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@CrossOrigin(origins = "*", allowedHeaders = "*")
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-10-11T14:51:06.469Z[GMT]")
@RestController
public class GetoriginalurlApiController extends ApiController implements GetoriginalurlApi {

	private static final Logger LOGGER = LoggerFactory.getLogger(GetoriginalurlApiController.class);
	private static final String A_PROBLEM_WAS_ENCOUNTERED_DECODING = "A problem was encountered decoding: ";

	private final ObjectMapper objectMapper;

	private final HttpServletRequest request;

    private final CacheDataRepository cacheDataRepository;

	@org.springframework.beans.factory.annotation.Autowired
	public GetoriginalurlApiController(ObjectMapper objectMapper, HttpServletRequest request, CacheDataRepository cacheDataRepository) {
		super(request);
		this.objectMapper = objectMapper;
		this.request = request;
		this.cacheDataRepository = cacheDataRepository;
	}

	public ResponseEntity<String> tcsconnectionGetOriginalUrlGet(@Parameter(in = ParameterIn.QUERY, description = "id" ,schema=@Schema()) @Valid @RequestParam(value = "id", required = true) String id, @Parameter(in = ParameterIn.QUERY, description = "useDefaults" ,schema=@Schema()) @Valid @RequestParam(value = "useDefaults", required = true, defaultValue = "false") Boolean useDefaults, @Parameter(in = ParameterIn.QUERY, description = "params" ,schema=@Schema()) @Valid @RequestParam(value = "params", required = false) String params) {

		if(id==null) {
			ErrorMessage errorMessage = new ErrorMessage();
			errorMessage.setMessage("No id parameter provided");
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Utils.gson.toJsonTree(errorMessage).toString());
		}

		HashMap<String,Object> requestParameters = new HashMap<>();

		if(!StringUtils.isBlank(id)) {
			try {
				id=java.net.URLDecoder.decode(id, StandardCharsets.UTF_8.name());
			} catch (UnsupportedEncodingException e) {
				LOGGER.warn(A_PROBLEM_WAS_ENCOUNTERED_DECODING + "id: "+ id, e);
				ErrorMessage errorMessage = new ErrorMessage();
				errorMessage.setMessage(A_PROBLEM_WAS_ENCOUNTERED_DECODING + "id: "+ id);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(Utils.gson.toJsonTree(errorMessage).toString());
			}
			requestParameters.put("id", id);
		}

		requestParameters.put("useDefaults", Boolean.toString(useDefaults));

		if(!StringUtils.isBlank(params)) {
			try {
				params=java.net.URLDecoder.decode(params, StandardCharsets.UTF_8.name());
			} catch (UnsupportedEncodingException e) {
				LOGGER.warn(A_PROBLEM_WAS_ENCOUNTERED_DECODING + "params: "+ params, e);
				ErrorMessage errorMessage = new ErrorMessage();
				errorMessage.setMessage(A_PROBLEM_WAS_ENCOUNTERED_DECODING + "id: "+ id);
				return ResponseEntity.status(HttpStatus.BAD_REQUEST)
						.body(Utils.gson.toJsonTree(errorMessage).toString());
			}
			requestParameters.put("params", params);
		}

		return redirectRequest(ServiceType.EXTERNAL, requestParameters, cacheDataRepository);
	}

	private ResponseEntity<String> redirectRequest(ServiceType service, Map<String, Object> requestParams, CacheDataRepository cacheDataRepository) {
		
		String cacheId = StringUtils.join(requestParams);
		String cachedResponse = null;

		try {
			Optional<CacheData> optionalCacheData = cacheDataRepository.findById(cacheId);
			if (optionalCacheData.isPresent()) cachedResponse = optionalCacheData.get().getValue();
		}catch(Exception e) {
			LOGGER.error("Unable to connect to Redis server, skipping caching mechanism, Cause: "+e.getMessage());
		}

		Response response = null;

		if(cachedResponse==null) {
			response = doRequest(service, requestParams);
			try {
				CacheData cacheData = new CacheData(cacheId, response.getPayloadAsPlainText().get());
				cacheDataRepository.save(cacheData);
			}catch(Exception e) {
				LOGGER.error("Unable to connect to Redis server, skipping caching mechanism, Cause: "+e.getMessage());

			}
		}

		Map<String,Object> handlerResponse = ExternalAccessHandler.handle((cachedResponse==null)? response.getPayloadAsPlainText().get() : cachedResponse, "getoriginalurl");

		String responseCode = "OK";

		if (handlerResponse.containsKey("httpStatusCode")) {
			responseCode = HttpStatus.valueOf(Integer.parseInt((String) handlerResponse.get("httpStatusCode"))).name();
		}

		switch (responseCode) {

		default : {
			ErrorMessage errorMessage = new ErrorMessage();
			errorMessage.setMessage("Received response "+responseCode+" on copying the URL");
			errorMessage.setHttpCode(responseCode);
			if(handlerResponse.containsKey("redirect-url")) errorMessage.setUrl(handlerResponse.get("redirect-url").toString());
			if(handlerResponse.containsKey("content-type")) errorMessage.setContentType(handlerResponse.get("content-type").toString());

			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Utils.gson.toJsonTree(errorMessage).toString());
		}
		case "OK" : {
			HttpHeaders httpHeaders = new HttpHeaders();
			httpHeaders.setContentType(new MediaType("application", "json"));
			return ResponseEntity.ok()
					.headers(httpHeaders)
					.body(Utils.gson.toJsonTree(handlerResponse).toString());	

		}
		}

	}

}
