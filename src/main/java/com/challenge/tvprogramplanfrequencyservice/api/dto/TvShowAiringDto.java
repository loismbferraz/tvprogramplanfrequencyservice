package com.challenge.tvprogramplanfrequencyservice.api.dto;

/**
 * Data Transfer Object (DTO) representing a TV show airing.
 *
 * @param id the unique identifier of the TV show airing
 * @param season the season number of the TV show
 * @param episode the episode number of the TV show
 * @param startTime the start time of the TV show airing
 * @param endTime the end time of the TV show airing
 */
public record TvShowAiringDto(String id, Long season, Long episode, String startTime, String endTime) {}
