package com.challenge.tvprogramplanfrequencyservice.api.dto;

/**
 * Data Transfer Object (DTO) representing a TV show occurrence.
 *
 * @param id the unique identifier of the TV show
 * @param title the title of the TV show
 * @param description the description of the TV show
 * @param occurrences the number of occurrences of the TV show
 */
public record TvShowOccurrenceDto(String id, String title, String description, long occurrences) {}
