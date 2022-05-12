package com.marksandspencer.foodshub.pal.service;

import com.marksandspencer.foodshub.pal.serviceImpl.CachingServiceImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CachingServiceTest {

    @InjectMocks
    CachingService cachingService = new CachingServiceImpl();

    @Mock
    CacheManager cacheManagerMock;

    @Captor
    ArgumentCaptor<String> cacheNameCaptor;

    public String cacheName = "TestCacheName";
    public String cacheKey = "TestCacheKey";
    Cache cache = new ConcurrentMapCache("test");

    @Before
    public void before() {
        when(cacheManagerMock.getCache(eq(cacheName))).thenReturn(cache);
        when(cacheManagerMock.getCacheNames()).thenReturn(Collections.singletonList(cacheName));
    }

    @Test
    public void evictCacheByNameAndKeyTest() {
        cachingService.evictCache(cacheName, cacheKey);
        verify(cacheManagerMock, times(1)).getCache(cacheNameCaptor.capture());
        assertEquals(cacheName, cacheNameCaptor.getValue());
    }

    @Test
    public void evictCacheByNameTest() {
        cachingService.evictCache(cacheName, null);
        verify(cacheManagerMock, times(1)).getCache(cacheNameCaptor.capture());
        assertEquals(cacheName, cacheNameCaptor.getValue());
    }

    @Test
    public void evictCacheAllTest() {
        cachingService.evictCache(null, null);
        verify(cacheManagerMock, times(1)).getCacheNames();
    }

    @Test
    public void getCacheAllTest() {
        List<Cache>  caches = cachingService.getCache(cacheName);
        assertNotNull(caches);
        assertEquals(cache, caches.get(0));
    }

    @Test
    public void getCacheByNameTest() {
        List<Cache>  caches = cachingService.getCache(null);
        assertNotNull(caches);
        assertEquals(cache, caches.get(0));
    }
}