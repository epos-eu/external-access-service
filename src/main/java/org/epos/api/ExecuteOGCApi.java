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

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.*;

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-10-11T14:51:06.469Z[GMT]")
@Validated
public interface ExecuteOGCApi {

    @Operation(summary = "queries on external services endpoint", description = "this endpoint enable queries on external services from ics-c to tcs to get data to be visualized or downloaded", tags={ "External Access Service" })
    @ApiResponses(value = { 
        @ApiResponse(responseCode = "200", description = "ok.", content = @Content(mediaType = "*/*", schema = @Schema(implementation = String.class))),
        
        @ApiResponse(responseCode = "201", description = "Created.", content = @Content(mediaType = "*/*", schema = @Schema(implementation = String.class))),
        
        @ApiResponse(responseCode = "204", description = "No content.", content = @Content(mediaType = "*/*", schema = @Schema(implementation = String.class))),
        
        @ApiResponse(responseCode = "301", description = "Moved Permanently.", content = @Content(mediaType = "*/*", schema = @Schema(implementation = String.class))),
        
        @ApiResponse(responseCode = "400", description = "Bad request."),
        
        @ApiResponse(responseCode = "401", description = "Access token is missing or invalid"),
        
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        
        @ApiResponse(responseCode = "404", description = "Not Found") })
    @RequestMapping(value = "/ogcexecute/{id}",
        produces = { "*/*" }, 
        method = RequestMethod.GET)
    ResponseEntity<Object> tcsconnectionsOGCExecuteGet(@NotNull @Parameter(in = ParameterIn.PATH, description = "the id of item to be executed" ,required=true,schema=@Schema()) @PathVariable("id") String id);
   /* ResponseEntity<String> tcsconnectionsExecuteGet(
    		@NotNull @Parameter(in = ParameterIn.QUERY, description = "the id of item to be executed" ,required=true,schema=@Schema()) @Valid @RequestParam(value = "id", required = true) String id,
    		@Parameter(in = ParameterIn.QUERY, description = "useDefaults" ,schema=@Schema()) @Valid @RequestParam(value = "useDefaults", required = true, defaultValue = "false") Boolean useDefaults, 
    		@Parameter(in = ParameterIn.QUERY, description = "output version requested" ,schema=@Schema()) @Valid @RequestParam(value = "version", required = false) String version, 
    		@Parameter(in = ParameterIn.QUERY, description = "output service requested" ,schema=@Schema()) @Valid @RequestParam(value = "service", required = false) String service, 
    		@Parameter(in = ParameterIn.QUERY, description = "output request requested" ,schema=@Schema()) @Valid @RequestParam(value = "request", required = false) String request, 
    		@Parameter(in = ParameterIn.QUERY, description = "output updatesequence requested" ,schema=@Schema()) @Valid @RequestParam(value = "updatesequence", required = false) String updatesequence,
    		@Parameter(in = ParameterIn.QUERY, description = "output layers requested" ,schema=@Schema()) @Valid @RequestParam(value = "layers", required = false) String layers,
    		@Parameter(in = ParameterIn.QUERY, description = "output srs requested" ,schema=@Schema()) @Valid @RequestParam(value = "srs", required = false) String srs,
    		@Parameter(in = ParameterIn.QUERY, description = "output bbox requested" ,schema=@Schema()) @Valid @RequestParam(value = "bbox", required = false) String bbox,
    		@Parameter(in = ParameterIn.QUERY, description = "output width requested" ,schema=@Schema()) @Valid @RequestParam(value = "width", required = false) String width,
    		@Parameter(in = ParameterIn.QUERY, description = "output height requested" ,schema=@Schema()) @Valid @RequestParam(value = "height", required = false) String height,
    		@Parameter(in = ParameterIn.QUERY, description = "output format requested" ,schema=@Schema()) @Valid @RequestParam(value = "format", required = false) String format,
    		@Parameter(in = ParameterIn.QUERY, description = "output transparent requested" ,schema=@Schema()) @Valid @RequestParam(value = "transparent", required = false) String transparent,
    		@Parameter(in = ParameterIn.QUERY, description = "output bgcolor requested" ,schema=@Schema()) @Valid @RequestParam(value = "bgcolor", required = false) String bgcolor,
    		@Parameter(in = ParameterIn.QUERY, description = "output exceptions requested" ,schema=@Schema()) @Valid @RequestParam(value = "exceptions", required = false) String exceptions,
    		@Parameter(in = ParameterIn.QUERY, description = "output time requested" ,schema=@Schema()) @Valid @RequestParam(value = "time", required = false) String time,
    		@Parameter(in = ParameterIn.QUERY, description = "output elevation requested" ,schema=@Schema()) @Valid @RequestParam(value = "elevation", required = false) String elevation,
    		@Parameter(in = ParameterIn.QUERY, description = "output sld requested" ,schema=@Schema()) @Valid @RequestParam(value = "sld", required = false) String sld,
    		@Parameter(in = ParameterIn.QUERY, description = "output wfs requested" ,schema=@Schema()) @Valid @RequestParam(value = "wfs", required = false) String wfs,
    		@Parameter(in = ParameterIn.QUERY, description = "output query_layers requested" ,schema=@Schema()) @Valid @RequestParam(value = "query_layers", required = false) String query_layers,
    		@Parameter(in = ParameterIn.QUERY, description = "output info_format requested" ,schema=@Schema()) @Valid @RequestParam(value = "info_format", required = false) String info_format,
    		@Parameter(in = ParameterIn.QUERY, description = "output feature_count requested" ,schema=@Schema()) @Valid @RequestParam(value = "feature_count", required = false) String feature_count,
    		@Parameter(in = ParameterIn.QUERY, description = "output x requested" ,schema=@Schema()) @Valid @RequestParam(value = "x", required = false) String x,
    		@Parameter(in = ParameterIn.QUERY, description = "output y requested" ,schema=@Schema()) @Valid @RequestParam(value = "y", required = false) String y
    		);*/

}

