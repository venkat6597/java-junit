package com.marksandspencer.foodshub.pal.service;

import org.springframework.cache.Cache;

import java.util.List;

public interface CachingService {

    void evictCache(String cacheName, String cacheKey);

    List<Cache> getCache(String cacheName);
}
