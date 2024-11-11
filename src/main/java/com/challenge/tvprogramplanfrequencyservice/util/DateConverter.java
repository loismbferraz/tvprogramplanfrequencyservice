package com.challenge.tvprogramplanfrequencyservice.util;

import com.challenge.tvprogramplanfrequencyservice.service.cache.exceptions.DateParsingException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for date parsing, formatting, and normalization.
 * <p>
 * This class provides methods to parse date strings into {@link LocalDate} or {@link OffsetDateTime}
 * objects, format dates to a standardized key format, and normalize dates to UTC midnight. It supports
 * both ISO offset (e.g., "yyyy-MM-dd'T'HH:mm:ss.SSSX") and local date (e.g., "yyyy-MM-dd") formats.
 * </p>
 * <p>
 * Each method handles exceptions gracefully by throwing custom exceptions with informative error messages.
 * </p>
 */
public final class DateConverter {


    /**
     * Parses a date string to LocalDate, supporting both ISO offset and local date formats.
     *
     * @param date the date string to parse
     * @return the parsed LocalDate
     * @throws DateParsingException if parsing fails
     */
    public static LocalDate parseToLocalDate(String date) {
        try {
            return parseDate(date).toLocalDate();
        } catch (Exception e) {
            throw new DateParsingException("Error parsing date: " + date, e);
        }
    }

    /**
     * Formats a date string to 'yyyy-MM-dd', supporting both ISO offset and local date formats.
     *
     * @param date the date string to format
     * @return the formatted date string
     * @throws DateParsingException if formatting fails
     */
    public static String formatDateKey(String date) {
        try {
            return parseDate(date).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            throw new DateParsingException("Error formatting date key: " + date, e);
        }
    }

    /**
     * Normalizes a date string to UTC midnight in the format 'yyyy-MM-dd'T'HH:mm:ss.SSSX'.
     *
     * @param date the date string to normalize
     * @return the normalized UTC midnight date string
     * @throws IllegalArgumentException if normalization fails
     */
    public static String normalizeDateToUtcMidnight(String date) {
        try {
            OffsetDateTime dateTime = parseDate(date).withOffsetSameInstant(ZoneOffset.UTC)
                    .withHour(0)
                    .withMinute(0)
                    .withSecond(0)
                    .withNano(0);
            return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX"));
        } catch (Exception e) {
            throw new IllegalArgumentException("Error normalizing date to UTC midnight: " + e.getMessage(), e);
        }
    }

    /**
     * Parses a date string to OffsetDateTime, supporting both ISO offset and local date formats.
     *
     * @param date the date string to parse
     * @return the parsed OffsetDateTime
     * @throws DateParsingException if parsing fails
     */
    private static OffsetDateTime parseDate(String date) {
        return date.contains("T") ? OffsetDateTime.parse(date, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                : LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay(ZoneOffset.UTC).toOffsetDateTime();
    }

}
