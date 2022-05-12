package com.marksandspencer.foodshub.pal.controller;

import com.marksandspencer.foodshub.pal.service.CachingService;
import com.marksandspencer.foodshub.pal.transfer.AppResponse;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CachingController {

    @Autowired
    CachingService cachingService;

    /**
     * list all caches from EhCache
     * @param cacheName Name of a cache
     * @return List<Cache> cache key and its values
     */
    @GetMapping(value = "/getCache")
    @ApiOperation(value = "getCache", response = AppResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 204, message = "No data"), @ApiResponse(code = 401, message = "Not authorized!"),
            @ApiResponse(code = 404, message = "Not found"), @ApiResponse(code = 400, message = "Bad request") })
    public List<Cache> getCache(@RequestParam(required = false) String cacheName) {
        return cachingService.getCache(cacheName);
    }

    /**
     * Evicts all caches from EhCache for a Cache Name
     * @param cacheName Name of a cache
     * @param cacheKey Key of a cache
     */
    @GetMapping(value = "/evictCache")
    @ApiOperation(value = "evictCache", response = AppResponse.class)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 204, message = "No data"), @ApiResponse(code = 401, message = "Not authorized!"),
            @ApiResponse(code = 404, message = "Not found"), @ApiResponse(code = 400, message = "Bad request") })
    public void evictCache(@RequestParam(required = false) String cacheName, @RequestParam(required = false) String cacheKey) {
        cachingService.evictCache(cacheName, cacheKey);
    }
}
