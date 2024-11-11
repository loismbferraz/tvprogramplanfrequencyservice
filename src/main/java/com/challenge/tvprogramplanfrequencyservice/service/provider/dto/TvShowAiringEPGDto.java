package com.challenge.tvprogramplanfrequencyservice.service.provider.dto;

public record TvShowAiringEPGDto(String id, Long season, Long episode, TvShowEPGDto tvShowEPGDto, String startTime, String endTime) {
}
