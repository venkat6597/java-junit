package com.marksandspencer.foodshub.pal.serviceImpl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.thymeleaf.util.StringUtils;

import com.marksandspencer.foodshub.pal.service.CachingService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CachingServiceImpl implements CachingService {

    @Autowired
    CacheManager cacheManager;

    @Override
    public void evictCache(String cacheName, String cacheKey) {
        long start = System.currentTimeMillis();
        log.info("ProductAttributeListingApplication > CachingService > Cache Eviction Started at {}", start);
        if (!StringUtils.isEmpty(cacheName) && !StringUtils.isEmpty(cacheKey)) {
        	Cache cache = cacheManager.getCache(cacheName);
        	if(!(ObjectUtils.isEmpty(cache)))
        		cache.evict(cacheKey);
            log.info("ProductAttributeListingApplication > CachingService > Evicting cache for Name : {} and Key : {} ", cacheName, cacheKey);
        } else if (!StringUtils.isEmpty(cacheName)) {
        	Cache cache = cacheManager.getCache(cacheName);
        	if(!(ObjectUtils.isEmpty(cache)))
        		cache.clear();
            log.info("ProductAttributeListingApplication > CachingService > Evicting cache for Name : {}", cacheName);
        } else {
            cacheManager.getCacheNames()
                    .parallelStream()
                    .forEach(cache -> cacheManager.getCache(cache).clear());
            log.info("ProductAttributeListingApplication > CachingService > Evicting All cache");
        }
        long end = System.currentTimeMillis();
        log.info("ProductAttributeListingApplication > CachingService > Cache Eviction Completed at {}", end-start);
    }

    @Override
    public List<Cache> getCache(String cacheName) {
        List<Cache> caches = new ArrayList<>();

        long start = System.currentTimeMillis();
        log.info("ProductAttributeListingApplication > CachingService > Get Cache Started at {}", start);
        if (!StringUtils.isEmpty(cacheName)) {
            caches.add(cacheManager.getCache(cacheName));
            log.info("ProductAttributeListingApplication > CachingService > get cache for Name : {}", cacheName);
        } else {
            cacheManager.getCacheNames()
                    .parallelStream()
                    .forEach(cache -> caches.add(cacheManager.getCache(cache)));
            log.info("ProductAttributeListingApplication > CachingService > get All cache");
        }
        long end = System.currentTimeMillis();
        log.info("ProductAttributeListingApplication > CachingService > Get Cache Completed at {}", end-start);
        return caches;
    }
}
