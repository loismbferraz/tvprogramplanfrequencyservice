package com.challenge.tvprogramplanfrequencyservice.api.controller;

import com.challenge.tvprogramplanfrequencyservice.api.dto.TvShowDto;
import com.challenge.tvprogramplanfrequencyservice.api.dto.TvShowOccurrenceDto;
import com.challenge.tvprogramplanfrequencyservice.api.openapi.OpenApi;
import com.challenge.tvprogramplanfrequencyservice.api.openapi.OpenApi.StatusCode;
import com.challenge.tvprogramplanfrequencyservice.api.util.DtoConverter;
import com.challenge.tvprogramplanfrequencyservice.util.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

import com.challenge.tvprogramplanfrequencyservice.service.cache.EPGCacheService;

@RestController
@RequestMapping("/api/shows")
@Tag(name = "ShowsApi")
@Validated
public class ShowsController {

  private final EPGCacheService epgCacheService;

  public ShowsController(EPGCacheService epgCacheService) {
    this.epgCacheService = epgCacheService;
  }

  @GetMapping("/aggregatedbytvshow")
  @Operation(operationId = "GetDataAggregatedByTvShow", summary = "Get Data Aggregated by TvShow.")
  @ApiResponse(
      responseCode = OpenApi.StatusCode.OK,
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TvShowDto.class),
              examples = @ExampleObject(value = OpenApi.ResponseEntity.AGGREGATED_BY_TVSHOW)))
  @ApiResponse(
      responseCode = OpenApi.StatusCode.INTERNAL_SERVER_ERROR,
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = OpenApi.StatusCode.BAD_REQUEST,
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = StatusCode.NOT_FOUND,
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = StatusCode.SERVICE_UNAVAILABLE,
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class)))
  public Mono<ResponseEntity<Set<TvShowDto>>> getRawData(
      @Parameter(
              description =
                  "Date in yyyy-MM-dd format to retrieve data for a specific day. To test use 2024-10-15")
          @RequestParam
          final String date) {
    return DtoConverter.convertSetTo(epgCacheService.getRawData(date)).map(ResponseEntity::ok);
  }

  @GetMapping("/orderedbyoccurrences")
  @Operation(
      operationId = "GetDataOrderedByOccurrences",
      summary =
          "Get Data Ordered By Occurrences that occurred between the given dates. If no end date is provided, it will return data for the given start date only.")
  @ApiResponse(
      responseCode = OpenApi.StatusCode.OK,
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = TvShowDto.class),
              examples = @ExampleObject(value = OpenApi.ResponseEntity.ORDERED_BY_OCCURRECNCES)))
  @ApiResponse(
      responseCode = OpenApi.StatusCode.INTERNAL_SERVER_ERROR,
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = OpenApi.StatusCode.BAD_REQUEST,
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = StatusCode.NOT_FOUND,
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(
      responseCode = StatusCode.SERVICE_UNAVAILABLE,
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ErrorResponse.class)))
  public Mono<ResponseEntity<List<TvShowOccurrenceDto>>> getOrderedByOccurrences(
      @Parameter(description = "Date in yyyy-MM-dd format to retrieve data for a specific day. To test use 2024-10-01")
          @RequestParam
          final String startDate,
      @Parameter(description = "Date in yyyy-MM-dd format to retrieve data for a specific day.To test use 2024-10-05")
          @RequestParam(required = false)
          final String endDate,
      @Parameter(description = "Order in which to retrieve data. Can be 'asc' or 'desc'.")
          @RequestParam(defaultValue = "asc")
          final String order,
      @Parameter(description = "Limit of data to retrieve.") @RequestParam(defaultValue = "10")
          final int limit) {

    return DtoConverter.convertListTo(
            epgCacheService.getOrderedByOccurrences(startDate, endDate, order, limit))
        .map(ResponseEntity::ok);
  }
}
