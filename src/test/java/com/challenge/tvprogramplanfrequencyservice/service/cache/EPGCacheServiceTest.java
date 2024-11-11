package com.challenge.tvprogramplanfrequencyservice.service.cache;

import com.challenge.tvprogramplanfrequencyservice.service.cache.EPGCacheService;
import com.challenge.tvprogramplanfrequencyservice.service.cache.entities.TvShow;
import com.challenge.tvprogramplanfrequencyservice.service.cache.entities.TvShowAiring;
import com.challenge.tvprogramplanfrequencyservice.service.cache.exceptions.DataNotFoundException;
import com.challenge.tvprogramplanfrequencyservice.service.provider.EPGClient;
import com.challenge.tvprogramplanfrequencyservice.service.provider.dto.TvShowAiringEPGDto;
import com.challenge.tvprogramplanfrequencyservice.service.provider.dto.TvShowEPGDto;
import com.challenge.tvprogramplanfrequencyservice.util.DateConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EPGCacheServiceTest {

    @Mock
    private EPGClient epgClient;

    @InjectMocks
    private EPGCacheService epgCacheService;


    @Test
    void getRawData_DataInCache_ReturnsFromCache() {
        // Arrange
        String date = "2023-12-25";
        String formattedDate = DateConverter.formatDateKey(date);
        TvShow tvShow = new TvShow("1", "Title 1", "Description 1", Set.of());
        epgCacheService.getAiringMap().put(formattedDate, Map.of(tvShow.id(), tvShow));

        // Act & Assert
        StepVerifier.create(epgCacheService.getRawData(date))
                .expectNextMatches(tvShows -> tvShows.size() == 1 && tvShows.contains(tvShow))
                .verifyComplete();
    }

    @Test
    void getRawData_DataNotInCache_FetchesFromProvider() {
        // Arrange
        String date = "2023-12-25";
        String formattedDate = DateConverter.formatDateKey(date);
        TvShowEPGDto tvShowEPGDto = new TvShowEPGDto("1", "Title", "Description");
        TvShowAiringEPGDto tvShowAiringEPGDto = new TvShowAiringEPGDto("1", 1L, 1L, tvShowEPGDto, "2023-12-25T03:30:00+02:00", "2023-12-25T05:30:00+02:00");

        when(epgClient.fetchDataFromProvider(formattedDate))
                .thenReturn(Flux.just(tvShowAiringEPGDto));

        // Act & Assert
        StepVerifier.create(epgCacheService.getRawData(date))
                .expectNextMatches(tvShows -> tvShows.stream().anyMatch(tvShow -> tvShow.id().equals("1") && tvShow.title().equals("Title")))
                .verifyComplete();
    }

    @Test
    void getRawData_DataNotFoundInProvider_ThrowsDataNotFoundException() {
        // Arrange
        String date = "2023-12-25";
        String formattedDate = DateConverter.formatDateKey(date);

        when(epgClient.fetchDataFromProvider(formattedDate))
                .thenReturn(Flux.error(new DataNotFoundException("No data found for date: " + formattedDate)));

        // Act & Assert
        StepVerifier.create(epgCacheService.getRawData(date))
                .expectErrorMatches(throwable -> throwable instanceof DataNotFoundException &&
                        throwable.getMessage().equals("No data found for date: " + date))
                .verify();
    }

    @Test
    void getRawData_ErrorDuringFetch_RemovesPartialDataFromCache() {
        // Arrange
        String date = "2023-12-25";
        String formattedDate = DateConverter.formatDateKey(date);
        TvShowEPGDto tvShowEPGDto = new TvShowEPGDto("1", "Title", "Description");
        TvShowAiringEPGDto tvShowAiringEPGDto = new TvShowAiringEPGDto("1", 1L, 1L, tvShowEPGDto, "2023-12-25T03:30:00+02:00", "2023-12-25T05:30:00+02:00");

        // Simulate fetchDataFromProvider to add data to cache and then throw an error
        when(epgClient.fetchDataFromProvider(formattedDate))
                .thenAnswer(invocation -> {
                    // Simulate partial data being added to the cache by getRawData
                    epgCacheService.getAiringMap().put(formattedDate, new ConcurrentHashMap<>());
                    return Flux.just(tvShowAiringEPGDto).concatWith(Flux.error(new RuntimeException("Test error")));
                });

        // Act & Assert
        StepVerifier.create(epgCacheService.getRawData(date))
                .expectError(RuntimeException.class)  // Expect an error to be propagated
                .verify();

        // Verify that the partial data was removed from the cache after the error
        assert !epgCacheService.getAiringMap().containsKey(formattedDate) : "Cache should be empty after error";
    }

    @Test
    void getOrderedByOccurrences_ValidDateRange_ReturnsOrderedResults() {
        // Arrange
        String startDate = "2023-12-24";
        String endDate = "2023-12-25";
        String formattedStartDate = DateConverter.formatDateKey(startDate);
        String formattedEndDate = DateConverter.formatDateKey(endDate);

        // Populate cache with occurrences for start date
        Set<TvShowAiring> airings =  Set.of(new TvShowAiring("1", 1L, 1L, "2023-12-24T03:30:00+02:00", "2023-12-24T05:30:00+02:00"),
                new TvShowAiring("2", 2L, 2L, "2023-12-25T03:30:00+02:00", "2023-12-25T05:30:00+02:00"));

        TvShow tvShow2 = new TvShow("2", "Show 2", "Description 2",airings);
        TvShowEPGDto tvShowEPGDto = new TvShowEPGDto("1", "Show 1", "Description 1");
        TvShowAiringEPGDto tvShowAiringEPGDto = new TvShowAiringEPGDto("3", 1L, 1L, tvShowEPGDto, "2023-12-25T03:30:00+02:00", "2023-12-25T05:30:00+02:00");

        epgCacheService.getAiringMap().put(formattedStartDate, Map.of(tvShow2.id(), tvShow2));

        // Fetch data from provider for end date

        when(epgClient.fetchDataFromProvider(formattedEndDate)).thenReturn(Flux.just(tvShowAiringEPGDto));

        // Act & Assert
        StepVerifier.create(epgCacheService.getOrderedByOccurrences(startDate, endDate, "asc", 10))
                .expectNextMatches(occurrences -> occurrences.size() == 2 && occurrences.get(0).occurrences() == 1 && occurrences.get(1).occurrences() == 2)
                .verifyComplete();
    }

    @Test
    void getOrderedByOccurrences_StartDateAfterEndDate_ThrowsIllegalArgumentException() {
        // Arrange
        String startDate = "2023-12-26";
        String endDate = "2023-12-25";

        // Act & Assert
        StepVerifier.create(epgCacheService.getOrderedByOccurrences(startDate, endDate, "asc", 10))
                .expectErrorMatches(throwable -> throwable instanceof IllegalArgumentException &&
                        throwable.getMessage().equals("Start date cannot be after end date."))
                .verify();
    }

    @Test
    void getOrderedByOccurrences_DataNotFoundForDate_ThrowsDataNotFoundException() {
        // Arrange
        String startDate = "2023-12-24";
        String formattedDate = DateConverter.formatDateKey(startDate);

        when(epgClient.fetchDataFromProvider(formattedDate))
                .thenReturn(Flux.error(new DataNotFoundException("No data found for date: " + formattedDate)));

        // Act & Assert
        StepVerifier.create(epgCacheService.getOrderedByOccurrences(startDate, startDate, "asc", 10))
                .expectErrorMatches(throwable -> throwable instanceof DataNotFoundException &&
                        throwable.getMessage().equals("No data found for date: " + startDate))
                .verify();
    }

    @Test
    void getOrderedByOccurrences_ErrorDuringFetch_RemovesPartialData() {
        // Arrange
        String startDate = "2023-12-24";
        String formattedDate = DateConverter.formatDateKey(startDate);

        when(epgClient.fetchDataFromProvider(formattedDate))
                .thenAnswer(invocation -> {
                    // Simulate partial data being added to cache before error
                    epgCacheService.getAiringMap().put(formattedDate, new ConcurrentHashMap<>());
                    return Flux.error(new RuntimeException("Test error"));
                });

        // Act & Assert
        StepVerifier.create(epgCacheService.getOrderedByOccurrences(startDate, startDate, "asc", 10))
                .expectError(RuntimeException.class)
                .verify();

        // Ensure cache cleanup
        assert !epgCacheService.getAiringMap().containsKey(formattedDate) : "Cache should be empty after error";
    }

}