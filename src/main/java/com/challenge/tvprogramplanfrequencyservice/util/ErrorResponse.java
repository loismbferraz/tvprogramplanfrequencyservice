package com.challenge.tvprogramplanfrequencyservice.util;

/**
 * Represents an error response.
 *
 * @param errorId the unique identifier for the error
 * @param technicalMessage the technical message describing the error
 * @param userMessage the user-friendly message describing the error
 */
public record ErrorResponse(String errorId, String technicalMessage, String userMessage) {}
