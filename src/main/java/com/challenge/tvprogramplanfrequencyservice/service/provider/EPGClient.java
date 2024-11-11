package com.challenge.tvprogramplanfrequencyservice.service.provider;

import com.challenge.tvprogramplanfrequencyservice.config.EPGConfig;
import com.challenge.tvprogramplanfrequencyservice.service.cache.EPGCacheService;
import com.challenge.tvprogramplanfrequencyservice.service.provider.dto.TvShowAiringEPGDto;
import com.challenge.tvprogramplanfrequencyservice.service.provider.dto.TvShowEPGDto;
import com.challenge.tvprogramplanfrequencyservice.service.provider.exceptions.EPGClientDataNotFoundException;
import com.challenge.tvprogramplanfrequencyservice.service.provider.exceptions.EPGClientException;
import com.challenge.tvprogramplanfrequencyservice.service.provider.exceptions.EPGClientServiceUnavailable;
import com.challenge.tvprogramplanfrequencyservice.service.provider.exceptions.EPGParseException;
import com.challenge.tvprogramplanfrequencyservice.service.provider.exceptions.EPGUriBuildException;
import com.challenge.tvprogramplanfrequencyservice.util.DateConverter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class EPGClient {

  private final WebClient webClient;
  private final ObjectMapper objectMapper;
  private final EPGConfig epgConfig;
  private static final Logger logger = LoggerFactory.getLogger(EPGCacheService.class);

  public EPGClient(WebClient webClient, ObjectMapper objectMapper, EPGConfig epgConfig) {
    this.webClient = webClient;
    this.objectMapper = objectMapper;
    this.epgConfig = epgConfig;
  }

  /**
   * Fetches TV show airing data from the external provider based on the specified date.
   *
   * @param date the date for which data is to be fetched, formatted as 'yyyy-MM-dd'
   * @return a Flux containing TvShowAiringEPGDto instances for each airing
   */
  public Flux<TvShowAiringEPGDto> fetchDataFromProvider(final String date) {
    String formattedDateString = DateConverter.normalizeDateToUtcMidnight(date);

    URI uri;
    try {
      uri = buildUri(formattedDateString);
    } catch (EPGUriBuildException e) {
      // Return the EPGUriBuildException if URI construction fails
      return Flux.error(e);
    }

    logger.info("Retrieving data from provider for day: {}", date);
    return webClient
        .get()
        .uri(uri)
        .retrieve()
        .onStatus(
            HttpStatusCode::is4xxClientError,
            clientResponse -> handle4xxError(clientResponse, date))
        .onStatus(HttpStatusCode::is5xxServerError, this::handle5xxError)
        .bodyToMono(String.class)
        .flatMapMany(this::parseResponse)
        .onErrorMap(this::mapConnectionErrors);
  }

  /**
   * Builds a URI with query parameters for the request to the external provider.
   *
   * @param formattedDate the date in UTC midnight format
   * @return the URI with necessary parameters
   * @throws EPGUriBuildException if URI building fails
   */
  private URI buildUri(final String formattedDate) {
    try {
      String variablesJson =
          objectMapper.writeValueAsString(
              Map.of(
                  "date",
                  formattedDate,
                  "domain",
                  epgConfig.getDomain(),
                  "type",
                  epgConfig.getType()));
      String encodedVariables = UriUtils.encode(variablesJson, StandardCharsets.UTF_8);
      return new URI(epgConfig.getBaseUrl() + "?variables=" + encodedVariables);
    } catch (Exception e) {
      throw new EPGUriBuildException("Error building URI: " + e.getMessage(), e);
    }
  }

  /**
   * Parses the provider's JSON response to create a Flux of TvShowAiringEPGDto objects.
   *
   * @param response the JSON response from the provider
   * @return a Flux of TvShowAiringEPGDto parsed from the JSON response
   */
  private Flux<TvShowAiringEPGDto> parseResponse(final String response) {
    try {
      JsonNode itemsNode = objectMapper.readTree(response).path("data").path("items");
      return Flux.fromIterable(itemsNode).map(this::convertJsonNodeToDto);
    } catch (Exception e) {
      throw new EPGParseException("Error processing response: " + e.getMessage(), e);
    }
  }

  /**
   * Converts a JSON node representing a single airing to a TvShowAiringEPGDto object.
   *
   * @param itemNode the JSON node containing airing information
   * @return a TvShowAiringEPGDto object
   */
  private TvShowAiringEPGDto convertJsonNodeToDto(final JsonNode itemNode) {
    try {

      String id = itemNode.path("id").asText();
      Long season = itemNode.path("season").path("number").asLong();
      Long episode =
          itemNode.path("episode").path("number").isNull()
              ? null
              : itemNode.path("episode").path("number").asLong();
      String tvShowId = itemNode.path("tvShow").path("id").asText();
      String title = itemNode.path("tvShow").path("title").asText();
      String description = itemNode.path("tvShow").path("description").asText();
      String startTime = itemNode.path("startTime").asText();
      String endTime = itemNode.path("endTime").asText();

      return new TvShowAiringEPGDto(
          id, season, episode, new TvShowEPGDto(tvShowId, title, description), startTime, endTime);
    } catch (Exception e) {
      throw new EPGParseException("Error converting JSON node to DTO: " + e.getMessage(), e);
    }
  }

  /**
   * Handles 4xx HTTP errors by throwing specific exceptions based on the status code.
   *
   * @param clientResponse the response containing the status code and error details
   * @param date the date for which data was requested
   * @return a Mono of Throwable representing the error to be thrown
   */
  private Mono<Throwable> handle4xxError(final ClientResponse clientResponse, final String date) {
    return clientResponse.statusCode().equals(HttpStatus.NOT_FOUND)
        ? clientResponse
            .bodyToMono(String.class)
            .defaultIfEmpty("No additional error details")
            .flatMap(
                body ->
                    Mono.error(
                        new EPGClientDataNotFoundException(
                            "No data found for the requested date: " + date, new Throwable(body))))
        : clientResponse
            .bodyToMono(String.class)
            .flatMap(
                body ->
                    Mono.error(
                        new EPGClientException(
                            "Client error from provider: " + clientResponse.statusCode(),
                            new Throwable(body))));
  }

  /**
   * Handles 5xx HTTP errors by throwing a service unavailable exception.
   *
   * @param clientResponse the response containing the error details
   * @return a Mono of Throwable indicating the service is unavailable
   */
  private Mono<Throwable> handle5xxError(final ClientResponse clientResponse) {
    return clientResponse
        .bodyToMono(String.class)
        .defaultIfEmpty("No additional error details")
        .flatMap(
            body ->
                Mono.error(
                    new EPGClientServiceUnavailable(
                        "Provider service unavailable.", new Throwable(body))));
  }

  /**
   * Maps connection-related exceptions (e.g., network issues) to a custom exception.
   *
   * @param throwable the original connection error
   * @return the mapped exception to be propagated
   */
  private Throwable mapConnectionErrors(Throwable throwable) {
    if (throwable instanceof WebClientRequestException) {
      return new EPGUriBuildException(
          "Failed to connect to provider: " + throwable.getMessage(), throwable);
    }
    return throwable;
  }
}
