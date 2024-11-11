package com.challenge.tvprogramplanfrequencyservice.api.util;

import com.challenge.tvprogramplanfrequencyservice.api.dto.TvShowAiringDto;
import com.challenge.tvprogramplanfrequencyservice.api.dto.TvShowDto;
import com.challenge.tvprogramplanfrequencyservice.api.dto.TvShowOccurrenceDto;
import com.challenge.tvprogramplanfrequencyservice.service.cache.entities.TvShow;
import com.challenge.tvprogramplanfrequencyservice.service.cache.entities.TvShowAiring;
import com.challenge.tvprogramplanfrequencyservice.service.cache.entities.TvShowOccurrence;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class for converting entities to DTOs for the API layer.
 */
public final class DtoConverter {

    /**
     * Converts a TvShowOccurrence entity to a TvShowOccurrenceDto.
     *
     * @param occurrence The TvShowOccurrence entity to convert.
     * @return A TvShowOccurrenceDto with data from the given entity.
     */
    public static TvShowOccurrenceDto convertTo(TvShowOccurrence occurrence) {
        return new TvShowOccurrenceDto(
                occurrence.id(),
                occurrence.title(),
                occurrence.description(),
                occurrence.occurrences()
        );
    }

    /**
     * Converts a Mono of List<TvShowOccurrence> to a Mono of List<TvShowOccurrenceDto>.
     *
     * @param occurrencesMono Mono containing a list of TvShowOccurrence entities.
     * @return Mono containing a list of TvShowOccurrenceDto after conversion.
     */
    public static Mono<List<TvShowOccurrenceDto>> convertListTo(Mono<List<TvShowOccurrence>> occurrencesMono) {
        return occurrencesMono
                .map(occurrences -> occurrences.stream()
                        .map(DtoConverter::convertTo) // Convert each TvShowOccurrence to its DTO
                        .collect(Collectors.toList())
                );
    }

    /**
     * Converts a TvShowAiring entity to a TvShowAiringDto.
     *
     * @param airing The TvShowAiring entity to convert.
     * @return A TvShowAiringDto with data from the given entity.
     */
    public static TvShowAiringDto convertAiringToDto(TvShowAiring airing) {
        return new TvShowAiringDto(
                airing.id(),
                airing.season(),
                airing.episode(),
                airing.startTime(),
                airing.endTime()
        );
    }

    /**
     * Converts a TvShow entity to a TvShowDto, including all its TvShowAiring entities.
     *
     * @param tvShow The TvShow entity to convert.
     * @return A TvShowDto with data from the given TvShow entity.
     */
    public static TvShowDto convertTo(TvShow tvShow) {
        // Convert each TvShowAiring to its DTO representation and collect into a Set
        Set<TvShowAiringDto> airingsDto = tvShow.airings().stream()
                .map(DtoConverter::convertAiringToDto)
                .collect(Collectors.toSet());

        return new TvShowDto(
                tvShow.id(),
                tvShow.title(),
                tvShow.description(),
                airingsDto
        );
    }

    /**
     * Converts a Mono of Set<TvShow> to a Mono of Set<TvShowDto>.
     *
     * @param tvShowSetMono Mono containing a set of TvShow entities.
     * @return Mono containing a set of TvShowDto after conversion.
     */
    public static Mono<Set<TvShowDto>> convertSetTo(Mono<Set<TvShow>> tvShowSetMono) {
        return tvShowSetMono.map(tvShowSet ->
                tvShowSet.stream()
                        .map(DtoConverter::convertTo) // Convert each TvShow to its DTO
                        .collect(Collectors.toSet())
        );
    }
}
