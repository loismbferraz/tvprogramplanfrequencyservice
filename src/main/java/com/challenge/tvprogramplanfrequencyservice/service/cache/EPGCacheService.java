package com.challenge.tvprogramplanfrequencyservice.service.cache;

import com.challenge.tvprogramplanfrequencyservice.service.cache.entities.TvShow;
import com.challenge.tvprogramplanfrequencyservice.service.cache.entities.TvShowAiring;
import com.challenge.tvprogramplanfrequencyservice.service.cache.entities.TvShowOccurrence;
import com.challenge.tvprogramplanfrequencyservice.service.cache.exceptions.DataNotFoundException;
import com.challenge.tvprogramplanfrequencyservice.service.cache.exceptions.DataStoreException;
import com.challenge.tvprogramplanfrequencyservice.service.provider.EPGClient;
import com.challenge.tvprogramplanfrequencyservice.service.provider.dto.TvShowAiringEPGDto;
import com.challenge.tvprogramplanfrequencyservice.util.DateConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class EPGCacheService {

  private final EPGClient EPGClient;
  private final Map<String, Map<String, TvShow>> airingMap = new ConcurrentHashMap<>();
  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  private static final Logger logger = LoggerFactory.getLogger(EPGCacheService.class);

  public EPGCacheService(EPGClient EPGClient) {
    this.EPGClient = EPGClient;
  }

    /**
     * Retrieves the raw data for a given date. Checks the cache first;
     * if not found, fetches data from the provider and caches it.
     *
     * @param date the date to retrieve data for in 'yyyy-MM-dd' format
     * @return a Mono containing the set of TvShow objects for the given date
     */
  public Mono<Set<TvShow>> getRawData(final String date) {
    String formattedDate = DateConverter.formatDateKey(date);
    Map<String, TvShow> existingData = airingMap.get(formattedDate);

    if (existingData != null) {
      logger.info("Retrieving from cache for day: {}", date);
      return Mono.just(Set.copyOf(existingData.values()));
    }

    return EPGClient.fetchDataFromProvider(formattedDate)
        .switchIfEmpty(
            Mono.error(new DataNotFoundException("No data found for the requested date: " + date)))
        .flatMap(this::storeInMap)
        .then(
            Mono.fromSupplier(
                () -> Set.copyOf(airingMap.getOrDefault(formattedDate, Map.of()).values())))
        .doOnError(
            e -> {
              airingMap.remove(formattedDate);
            });
  }

    /**
     * Retrieves occurrences of TV shows ordered by frequency within a date range.
     * Checks cache first and fetches missing dates if necessary.
     *
     * @param startDate the start date in 'yyyy-MM-dd' format
     * @param endDate the end date in 'yyyy-MM-dd' format (nullable, defaults to start date)
     * @param order the sort order, either 'asc' or 'desc'
     * @param limit the maximum number of results to return
     * @return a Mono containing a list of TvShowOccurrence objects
     */
  public Mono<List<TvShowOccurrence>> getOrderedByOccurrences(
      final String startDate, final String endDate, final String order, final int limit) {
    if (!"asc".equalsIgnoreCase(order) && !"desc".equalsIgnoreCase(order)) {
      return Mono.error(new IllegalArgumentException("Order must be 'asc' or 'desc'."));
    }
    LocalDate start = DateConverter.parseToLocalDate(startDate);
    LocalDate end = (endDate != null) ? DateConverter.parseToLocalDate(endDate) : start;

    if (start.isAfter(end)) {
      return Mono.error(new IllegalArgumentException("Start date cannot be after end date."));
    }

    List<LocalDate> datesInRange = start.datesUntil(end.plusDays(1)).collect(Collectors.toList());

    return Flux.fromIterable(datesInRange)
        .flatMap(
            date -> {
              String formattedDate = date.format(DATE_FORMATTER);
              Map<String, TvShow> tvShowsForDate = airingMap.get(formattedDate);
              if (tvShowsForDate != null) {
                logger.info("Retrieving from cache for day: {}", date);
                return Mono.just(tvShowsForDate);
              } else {
                return EPGClient.fetchDataFromProvider(formattedDate)
                    .switchIfEmpty(
                        Mono.error(
                            new DataNotFoundException("No data found for date: " + formattedDate)))
                    .flatMap(this::storeInMap)
                    .then(Mono.fromCallable(() -> airingMap.getOrDefault(formattedDate, Map.of())))
                    .doOnError(
                        e -> {
                          airingMap.remove(formattedDate);
                        });
              }
            })
        .collectList()
        .map(tvShowMaps -> aggregateAndSortOccurrences(tvShowMaps, order, limit));
  }
    /**
     * Stores the TV show airing data in the cache (airingMap) by date.
     * Updates an existing TV show if already present; otherwise, creates a new entry.
     *
     * @param tvShowAiringEPGDto the DTO containing airing data for a TV show
     * @return an empty Mono<Void>
     */
  private Mono<Void> storeInMap(final TvShowAiringEPGDto tvShowAiringEPGDto) {
    try {
      String dateKey = DateConverter.formatDateKey(tvShowAiringEPGDto.startTime());
      airingMap
          .computeIfAbsent(dateKey, k -> new ConcurrentHashMap<>())
          .compute(
              tvShowAiringEPGDto.tvShowEPGDto().id(),
              (id, existingTvShow) -> {
                if (existingTvShow == null) {
                  existingTvShow =
                      new TvShow(
                          tvShowAiringEPGDto.tvShowEPGDto().id(),
                          tvShowAiringEPGDto.tvShowEPGDto().title(),
                          tvShowAiringEPGDto.tvShowEPGDto().description(),
                          new HashSet<>());
                }
                // Add airing details in a thread-safe manner
                synchronized (existingTvShow.airings()) {
                  existingTvShow
                      .airings()
                      .add(
                          new TvShowAiring(
                              tvShowAiringEPGDto.id(),
                              tvShowAiringEPGDto.season(),
                              tvShowAiringEPGDto.episode(),
                              tvShowAiringEPGDto.startTime(),
                              tvShowAiringEPGDto.endTime()));
                }
                return existingTvShow;
              });
      return Mono.empty();
    } catch (Exception e) {
      throw new DataStoreException(
          "Error storing data in airingMap for TvShow ID: "
              + tvShowAiringEPGDto.tvShowEPGDto().id(),
          e);
    }
  }

    /**
     * Aggregates TV shows occurrences over a list of maps by ID and sorts them.
     *
     * @param tvShowMaps a list of maps of TV shows grouped by date
     * @param order the sorting order, either 'asc' or 'desc'
     * @param limit the maximum number of occurrences to include in the result
     * @return a sorted list of TvShowOccurrence objects
     */
  private List<TvShowOccurrence> aggregateAndSortOccurrences(
      final List<Map<String, TvShow>> tvShowMaps, final String order, final int limit) {
    Map<String, TvShowOccurrence> occurrences =
        tvShowMaps.stream()
            .flatMap(map -> map.values().stream())
            .collect(
                Collectors.toMap(
                    TvShow::id,
                    tvShow ->
                        new TvShowOccurrence(
                            tvShow.id(),
                            tvShow.title(),
                            tvShow.description(),
                            tvShow.airings().size()),
                    (dto1, dto2) ->
                        new TvShowOccurrence(
                            dto1.id(),
                            dto1.title(),
                            dto1.description(),
                            dto1.occurrences() + dto2.occurrences())));
    return occurrences.values().stream()
        .sorted(
            (a, b) ->
                "asc".equalsIgnoreCase(order)
                    ? Long.compare(a.occurrences(), b.occurrences())
                    : Long.compare(b.occurrences(), a.occurrences()))
        .limit(limit)
        .collect(Collectors.toList());
  }

    /**
     * Provides access to the internal airing map for caching purposes.
     *
     * @return the current state of the airingMap
     */
  public Map<String, Map<String, TvShow>> getAiringMap() {
    return airingMap;
  }
}
