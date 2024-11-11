package com.challenge.tvprogramplanfrequencyservice.util;

import com.challenge.tvprogramplanfrequencyservice.service.cache.exceptions.DataNotFoundException;
import com.challenge.tvprogramplanfrequencyservice.service.cache.exceptions.DataStoreException;
import com.challenge.tvprogramplanfrequencyservice.service.cache.exceptions.DateParsingException;
import com.challenge.tvprogramplanfrequencyservice.service.provider.exceptions.EPGClientDataNotFoundException;
import com.challenge.tvprogramplanfrequencyservice.service.provider.exceptions.EPGClientException;
import com.challenge.tvprogramplanfrequencyservice.service.provider.exceptions.EPGClientServiceUnavailable;
import com.challenge.tvprogramplanfrequencyservice.service.provider.exceptions.EPGParseException;
import com.challenge.tvprogramplanfrequencyservice.service.provider.exceptions.EPGUriBuildException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebExchangeBindException;

import java.util.UUID;

/**
 * Global exception handler to manage and handle exceptions thrown across the application.
 * Provides centralized exception handling by returning meaningful error messages and HTTP statuses.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles validation errors, particularly those resulting from invalid date formats.
     * Responds with a 400 BAD_REQUEST status and a message indicating the correct date format.
     *
     * @param ex the exception thrown for validation issues.
     * @return ResponseEntity containing a custom message and HTTP BAD_REQUEST status.
     */
    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<String> handleValidationException(WebExchangeBindException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid date format. Expected format is yyyy-MM-dd.");
    }

    /**
     * Handles errors specific to URI construction issues for EPG requests.
     * Responds with a 400 BAD_REQUEST status.
     *
     * @param ex the EPGUriBuildException thrown.
     * @return ResponseEntity containing an error response and HTTP BAD_REQUEST status.
     */
    @ExceptionHandler(EPGUriBuildException.class)
    public ResponseEntity<ErrorResponse> handleEPGUriBuildException(EPGUriBuildException ex) {
        return createErrorResponse(ex, HttpStatus.BAD_REQUEST, "Invalid request to the server.");
    }

    /**
     * Handles exceptions thrown during data parsing in EPGClient.
     * Responds with a 500 INTERNAL_SERVER_ERROR status.
     *
     * @param ex the EPGParseException thrown during data parsing.
     * @return ResponseEntity containing an error response and HTTP INTERNAL_SERVER_ERROR status.
     */
    @ExceptionHandler(EPGParseException.class)
    public ResponseEntity<ErrorResponse> handleEPGParseException(EPGParseException ex) {
        return createErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred while processing data.");
    }

    /**
     * Handles generic EPG client errors, responding with a 500 INTERNAL_SERVER_ERROR.
     *
     * @param ex the EPGClientException thrown during data retrieval.
     * @return ResponseEntity with an error response and HTTP INTERNAL_SERVER_ERROR status.
     */
    @ExceptionHandler(EPGClientException.class)
    public ResponseEntity<ErrorResponse> handleEPGClientException(EPGClientException ex) {
        return createErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error during data retrieval.");
    }

    /**
     * Handles specific data not found errors in the EPG client, responding with a 404 NOT_FOUND.
     *
     * @param ex the EPGClientDataNotFoundException thrown when data is unavailable for a given date.
     * @return ResponseEntity with an error response and HTTP NOT_FOUND status.
     */
    @ExceptionHandler(EPGClientDataNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEPGClientDataNotFoundException(EPGClientDataNotFoundException ex) {
        return createErrorResponse(ex, HttpStatus.NOT_FOUND, "No data found for the requested date.");
    }

    /**
     * Handles service unavailability errors from the EPG client, responding with a 503 SERVICE_UNAVAILABLE.
     *
     * @param ex the EPGClientServiceUnavailable thrown when the provider service is down.
     * @return ResponseEntity with an error response and HTTP SERVICE_UNAVAILABLE status.
     */
    @ExceptionHandler(EPGClientServiceUnavailable.class)
    public ResponseEntity<ErrorResponse> handleEPGClientServiceUnavailable(EPGClientServiceUnavailable ex) {
        return createErrorResponse(ex, HttpStatus.SERVICE_UNAVAILABLE, "Provider Service is not available.");
    }

    /**
     * Handles data not found errors specific to cache data, responding with a 404 NOT_FOUND.
     *
     * @param ex the DataNotFoundException thrown when data is unavailable in cache.
     * @return ResponseEntity with an error response and HTTP NOT_FOUND status.
     */
    @ExceptionHandler(DataNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDataNotFoundException(DataNotFoundException ex) {
        return createErrorResponse(ex, HttpStatus.NOT_FOUND, "No data found for the requested date.");
    }

    /**
     * Handles errors related to date parsing, such as invalid date formats, responding with a 400 BAD_REQUEST.
     *
     * @param ex the DateParsingException thrown for invalid date formats.
     * @return ResponseEntity with an error response and HTTP BAD_REQUEST status.
     */
    @ExceptionHandler(DateParsingException.class)
    public ResponseEntity<ErrorResponse> handleDateParsingException(DateParsingException ex) {
        return createErrorResponse(ex, HttpStatus.BAD_REQUEST, "Invalid request to the server.");
    }

    /**
     * Handles data storage exceptions, responding with a 500 INTERNAL_SERVER_ERROR.
     *
     * @param ex the DataStoreException thrown during data storage errors.
     * @return ResponseEntity with an error response and HTTP INTERNAL_SERVER_ERROR status.
     */
    @ExceptionHandler(DataStoreException.class)
    public ResponseEntity<ErrorResponse> handleDataStoreException(DataStoreException ex) {
        return createErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
    }

    /**
     * Handles generic illegal argument exceptions, responding with a 400 BAD_REQUEST.
     *
     * @param ex the IllegalArgumentException thrown due to invalid input arguments.
     * @return ResponseEntity with an error response and HTTP BAD_REQUEST status.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(Exception ex) {
        return createErrorResponse(ex, HttpStatus.BAD_REQUEST, "Invalid request to the server.");
    }

    /**
     * Handles any uncaught exceptions, returning a generic error message with a 500 INTERNAL_SERVER_ERROR status.
     *
     * @param ex the Exception thrown for any unspecified errors.
     * @return ResponseEntity with an error response and HTTP INTERNAL_SERVER_ERROR status.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        return createErrorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
    }

    /**
     * Utility method to create an error response entity.
     * Generates a unique error ID and logs the details for troubleshooting.
     *
     * @param ex            the exception thrown.
     * @param status        the HTTP status to return.
     * @param userMessage   the user-friendly message to include in the response.
     * @return ResponseEntity containing the ErrorResponse object and HTTP status.
     */
    private ResponseEntity<ErrorResponse> createErrorResponse(Exception ex, HttpStatus status, String userMessage) {
        String errorId = UUID.randomUUID().toString();
        String technicalMessage = ex.getMessage();

        logger.error("Error ID: {} | Status: {} | Technical Message: {}", errorId, status, technicalMessage, ex);

        ErrorResponse errorResponse = new ErrorResponse(errorId, technicalMessage, userMessage);
        return new ResponseEntity<>(errorResponse, status);
    }
}
