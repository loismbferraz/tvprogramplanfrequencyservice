package com.challenge.tvprogramplanfrequencyservice.service.cache.entities;

import java.util.Set;

public record TvShow (String id, String title, String description, Set<TvShowAiring> airings) {}
