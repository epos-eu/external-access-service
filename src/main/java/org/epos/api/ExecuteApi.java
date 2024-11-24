/**
 * NOTE: This class is auto generated by the swagger code generator program (3.0.29).
 * https://github.com/swagger-api/swagger-codegen
 * Do not edit the class manually.
 */
package org.epos.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import javax.validation.Valid;
import javax.validation.constraints.*;

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-10-11T14:51:06.469Z[GMT]")
@Validated
public interface ExecuteApi {

	@Operation(summary = "queries on external services endpoint", description = "this endpoint enable queries on external services from ics-c to tcs to get data to be visualized or downloaded", tags = {
			"External Access Service" })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "ok.", content = @Content(mediaType = "*/*", schema = @Schema(implementation = String.class))),

			@ApiResponse(responseCode = "201", description = "Created.", content = @Content(mediaType = "*/*", schema = @Schema(implementation = String.class))),

			@ApiResponse(responseCode = "204", description = "No content.", content = @Content(mediaType = "*/*", schema = @Schema(implementation = String.class))),

			@ApiResponse(responseCode = "301", description = "Moved Permanently.", content = @Content(mediaType = "*/*", schema = @Schema(implementation = String.class))),

			@ApiResponse(responseCode = "400", description = "Bad request."),

			@ApiResponse(responseCode = "401", description = "Access token is missing or invalid"),

			@ApiResponse(responseCode = "403", description = "Forbidden"),

			@ApiResponse(responseCode = "404", description = "Not Found") })
	@RequestMapping(value = "/execute/{instance_id}", produces = { "*/*" }, method = RequestMethod.GET)
	ResponseEntity<String> tcsconnectionsExecuteGet(
			@NotNull @Parameter(in = ParameterIn.PATH, description = "the id of item to be executed", required = true, schema = @Schema()) @PathVariable("instance_id") String id,
			@Parameter(in = ParameterIn.QUERY, description = "useDefaults", schema = @Schema()) @Valid @RequestParam(value = "useDefaults", required = true, defaultValue = "false") Boolean useDefaults,
			@Parameter(in = ParameterIn.QUERY, description = "output format requested", schema = @Schema()) @Valid @RequestParam(value = "format", required = false) String format,
			@Parameter(in = ParameterIn.QUERY, description = "startDate", schema = @Schema()) @Valid @RequestParam(value = "startDate", required = false) String startDate,
			@Parameter(in = ParameterIn.QUERY, description = "endDate", schema = @Schema()) @Valid @RequestParam(value = "endDate", required = false) String endDate,
			@Parameter(in = ParameterIn.QUERY, description = "bbox", schema = @Schema()) @Valid @RequestParam(value = "bbox", required = false) String bbox,
			@Parameter(in = ParameterIn.QUERY, description = "pluginId", schema = @Schema()) @Valid @RequestParam(value = "pluginId", required = false) String pluginId,
			@Parameter(in = ParameterIn.QUERY, description = "input format for the plugin execution", schema = @Schema()) @Valid @RequestParam(value = "inputFormat", required = false) String inputFormat,
			@Parameter(in = ParameterIn.QUERY, description = "params", schema = @Schema()) @Valid @RequestParam(value = "params", required = false) String params);

}
