package com.challenge.tvprogramplanfrequencyservice.service.cache.exceptions;

public class DataNotFoundException extends EPGCacheException {
    public DataNotFoundException(final String message) {
        super(message, null);
    }
}