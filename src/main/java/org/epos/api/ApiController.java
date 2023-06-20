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
package org.epos.api;

import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.epos.api.utility.Utils;
import org.epos.api.utility.WebUtils;
import org.epos.router_framework.RpcRouter;
import org.epos.router_framework.domain.Actor;
import org.epos.router_framework.domain.Request;
import org.epos.router_framework.domain.RequestBuilder;
import org.epos.router_framework.domain.Response;
import org.epos.router_framework.types.ServiceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;

abstract class ApiController {

	private static final Logger LOGGER = LoggerFactory.getLogger(ApiController.class);
	protected final HttpServletRequest request;

	//protected static Gson gson = new Gson();

	@Autowired
	private RpcRouter router;

	protected ApiController(HttpServletRequest request) {
		this.request = request;
	}

	protected Response doRequest(ServiceType service, Map<String, Object> requestParams) {
		return this.doRequest(service, null, requestParams);
	}

	protected Response doRequest(ServiceType service, Actor nextComponentOverride, Map<String, Object> requestParams) 
	{
		String op = request.getMethod().toLowerCase();		
		String requestType = request.getRequestURI()
				.replaceAll("^\\/+", "")		// strip out leading slashes
				.replaceAll("\\/+$", "")		// strip out trailing slashes
				.replaceAll("\\/", "\\.");		// remaining slashes make dots

		Map<String, Object> headers = WebUtils.getHeadersInfo(request);
		headers.put("kind", op+"."+requestType);

		LOGGER.debug("Sending " + service.name() + " request with header, " + headers.toString() + ", and parameters, "
				+ requestParams.toString());

		Request localRequest = RequestBuilder.instance(service, op, requestType)//
				.addPayloadPlainText(Utils.gson.toJson(requestParams))//
				.addHeaders(headers)//
				.build();

		Response response;
		if (nextComponentOverride != null) {
			response = router.makeRequest(localRequest, nextComponentOverride);
		} else {
			response = router.makeRequest(localRequest);
		}
		LOGGER.debug(response.toString());

		return response;
	}

	protected ResponseEntity<String> standardRequest(ServiceType service, Map<String, Object> requestParams) {

		Response response = doRequest(service, requestParams);

		String responseCode = "OK";

		if (response.getErrorCode().isPresent()) {
			LOGGER.debug(response.getComponentAudit());
			String errStr = String.format("[ERROR: %s] %s",
					response.getErrorCode().get().name(),
					response.getErrorMessage().isPresent() ? response.getErrorMessage().get() : "NONE");
			LOGGER.debug(errStr);
			responseCode = response.getErrorCode().get().name();
		}

		switch (responseCode) {

		case "USAGE_ERROR" : {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		case "INTERNAL_ERROR" : {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
		case "TIMED_OUT" : {
			return new ResponseEntity<>(HttpStatus.REQUEST_TIMEOUT);
		}
		case "INTERRUPTED" : {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}
		case "LOGIC_ERROR" : {
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		default : {
			if (response.getPayloadAsPlainText().isPresent()) {

				LOGGER.debug("Payload text received => "+response.getPayloadAsPlainText().orElse("<no payload>"));

				if(Utils.gson.fromJson(response.getPayloadAsPlainText().get(), JsonElement.class).isJsonObject()) {
					if(!Utils.gson.fromJson(response.getPayloadAsPlainText().get(), JsonElement.class).getAsJsonObject().entrySet().isEmpty()) {
						LOGGER.debug("Payload Array received => "+response.getPayloadAsPlainText());
						Optional<String> payload = response.getPayloadAsPlainText();


						LOGGER.debug("Payload Array GET  => "+payload.get());

						return hasContent(payload)? ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(payload.get()) : ResponseEntity.status(HttpStatus.NO_CONTENT).build();
					}
				}
				else if(Utils.gson.fromJson(response.getPayloadAsPlainText().get(), JsonElement.class).isJsonArray()) {
					if(Utils.gson.fromJson(response.getPayloadAsPlainText().get(), JsonElement.class).getAsJsonArray().size()>0) {
						LOGGER.debug("Payload Array received => "+response.getPayloadAsPlainText());
						Optional<String> payload = response.getPayloadAsPlainText();


						LOGGER.debug("Payload Array GET  => "+payload.get());

						return hasContent(payload)? ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(payload.get()) : ResponseEntity.status(HttpStatus.NO_CONTENT).build();
					}
				}
				else {
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
				}				
			} 
			else {
				return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
			}
		}
		}
		return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
	}


	private static boolean hasContent(Optional<String> payload) throws JsonSyntaxException {
		LOGGER.debug("Has Content Payload => "+payload);
		if (!payload.isPresent()) return false;
		JsonElement jsonElement = Utils.gson.fromJson(payload.get(), JsonElement.class);
		LOGGER.debug("Has Content Json Element => "+jsonElement.toString());
		return ((jsonElement.isJsonArray() && jsonElement.getAsJsonArray().size()<1) 
				|| (jsonElement.isJsonObject() && jsonElement.getAsJsonObject().entrySet().isEmpty()))? 
						false : true;
	}
}
