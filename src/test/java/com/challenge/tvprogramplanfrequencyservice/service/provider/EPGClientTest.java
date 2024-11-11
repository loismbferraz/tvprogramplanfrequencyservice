package com.challenge.tvprogramplanfrequencyservice.service.provider;

import com.challenge.tvprogramplanfrequencyservice.config.EPGConfig;
import com.challenge.tvprogramplanfrequencyservice.service.provider.EPGClient;
import com.challenge.tvprogramplanfrequencyservice.service.provider.dto.TvShowAiringEPGDto;
import com.challenge.tvprogramplanfrequencyservice.service.provider.dto.TvShowEPGDto;
import com.challenge.tvprogramplanfrequencyservice.service.provider.exceptions.EPGClientDataNotFoundException;
import com.challenge.tvprogramplanfrequencyservice.service.provider.exceptions.EPGClientServiceUnavailable;
import com.challenge.tvprogramplanfrequencyservice.service.provider.exceptions.EPGParseException;
import com.challenge.tvprogramplanfrequencyservice.service.provider.exceptions.EPGUriBuildException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import javax.naming.ServiceUnavailableException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;

class EPGClientTest {

    private MockWebServer mockWebServer;
    private EPGClient epgClient;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .build();

        EPGConfig epgConfig = new EPGConfig();
        epgConfig.setBaseUrl(mockWebServer.url("/").toString());
        epgConfig.setDomain("mock-domain");
        epgConfig.setType("mock-type");

        epgClient = new EPGClient(webClient, new ObjectMapper(), epgConfig);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void fetchDataFromProvider_SuccessfulFetch_ReturnsData() {
        // Arrange
        String date = "2023-12-25";
        String mockResponse = "{ \"data\": { \"items\": [ { \"id\": \"1\", \"season\": { \"number\": 1 }, \"episode\": { \"number\": 1 }, \"tvShow\": {\"id\": \"show1\", \"title\": \"Title\", \"description\": \"Description\"}, \"startTime\": \"2023-12-25T03:30:00+02:00\", \"endTime\": \"2023-12-25T05:30:00+02:00\" } ] } }";

        mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponse)
                .addHeader("Content-Type", "application/json"));

        TvShowAiringEPGDto expectedDto = new TvShowAiringEPGDto("1", 1L, 1L,
                new TvShowEPGDto("show1", "Title", "Description"), "2023-12-25T03:30:00+02:00", "2023-12-25T05:30:00+02:00");

        // Act & Assert
        StepVerifier.create(epgClient.fetchDataFromProvider(date))
                .expectNextMatches(tvShowAiringEPGDto ->
                        tvShowAiringEPGDto.id().equals(expectedDto.id()) &&
                                tvShowAiringEPGDto.season().equals(expectedDto.season()) &&
                                tvShowAiringEPGDto.episode().equals(expectedDto.episode()) &&
                                tvShowAiringEPGDto.tvShowEPGDto().id().equals(expectedDto.tvShowEPGDto().id()) &&
                                tvShowAiringEPGDto.startTime().equals(expectedDto.startTime()) &&
                                tvShowAiringEPGDto.endTime().equals(expectedDto.endTime()))
                .verifyComplete();
    }

    @Test
    void fetchDataFromProvider_404NotFound_ThrowsDataNotFoundException() {
        // Arrange
        String date = "2023-12-25";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(404)
                .setBody("Not Found"));

        // Act & Assert
        StepVerifier.create(epgClient.fetchDataFromProvider(date))
                .expectErrorMatches(throwable -> throwable instanceof EPGClientDataNotFoundException &&
                        throwable.getMessage().contains("No data found for the requested date: " + date))
                .verify();
    }

    @Test
    void fetchDataFromProvider_500ServerError_ThrowsServiceUnavailableException() {
        // Arrange
        String date = "2023-12-25";
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));

        // Act & Assert
        StepVerifier.create(epgClient.fetchDataFromProvider(date))
                .expectErrorMatches(throwable -> throwable instanceof EPGClientServiceUnavailable &&
                        throwable.getMessage().contains("Provider service unavailable."))
                .verify();
    }

    @Test
    void fetchDataFromProvider_InvalidUri_ThrowsUriBuildException() {
        // Arrange
        String date = "2023-12-25";
        epgClient = new EPGClient(WebClient.builder().build(), new ObjectMapper(), new EPGConfig() {{
            setBaseUrl("http://invalid-url");
            setDomain("domain");
            setType("type");
        }});

        // Act & Assert
        assertThrows(EPGUriBuildException.class, () -> epgClient.fetchDataFromProvider(date).blockLast());
    }

    @Test
    void parseResponse_InvalidJson_ThrowsParseException() {
        // Arrange
        String date = "2023-12-25";
        mockWebServer.enqueue(new MockResponse()
                .setBody("{ invalid json }")
                .addHeader("Content-Type", "application/json"));

        // Act & Assert
        StepVerifier.create(epgClient.fetchDataFromProvider(date))
                .expectError(EPGParseException.class)
                .verify();
    }
}