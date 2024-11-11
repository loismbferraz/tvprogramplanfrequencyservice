package com.challenge.tvprogramplanfrequencyservice.api.dto;

import java.util.Set;

/**
 * Data Transfer Object (DTO) representing a TV show.
 *
 * @param id the unique identifier of the TV show
 * @param title the title of the TV show
 * @param description the description of the TV show
 * @param tvShowAirings the set of TV show airings associated with the TV show
 */
public record TvShowDto(String id, String title, String description, Set<TvShowAiringDto> tvShowAirings) {}
