package com.marksandspencer.foodshub.pal.controller;

import com.marksandspencer.foodshub.pal.service.CachingService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cache.Cache;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CachingControllerTest {

    @InjectMocks
    CachingController cachingController = new CachingController();

    @Mock
    CachingService cachingService;

    @Captor
    ArgumentCaptor<String> cacheNameCaptor;

    @Captor
    ArgumentCaptor<String> cacheKeyCaptor;

    private String cacheName = "testCacheName";
    private String cacheKey = "testCacheKey";
    @Test
    public void getCacheTest() {
        List<Cache> cache = new ArrayList<>();
        when(cachingService.getCache(eq(cacheName))).thenReturn(cache);
        List<Cache> palFieldsCache = cachingController.getCache(cacheName);
        assertNotNull(palFieldsCache);
    }

    @Test
    public void evictCacheTest() {

        cachingController.evictCache(cacheName, cacheKey);
        verify(cachingService, times(1)).evictCache(cacheNameCaptor.capture(), cacheKeyCaptor.capture());
        assertEquals(cacheName, cacheNameCaptor.getValue());
        assertEquals(cacheKey, cacheKeyCaptor.getValue());
    }

}
