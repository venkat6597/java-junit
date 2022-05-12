package com.marksandspencer.foodshub.pal.rest.client.impl;

import com.marksandspencer.foodshub.pal.dto.Suppliers;
import com.marksandspencer.foodshub.pal.exception.PALServiceException;
import com.marksandspencer.foodshub.pal.properties.ESSupplierServiceConfig;
import com.marksandspencer.foodshub.pal.rest.client.ESSupplierServiceRestClient;
import com.marksandspencer.foodshub.pal.transfer.ESSupplierDataRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/**
 * ESLocationServiceRestClient
 */
@Service
@Slf4j
public class ESSupplierServiceRestClientImpl extends RestClient implements ESSupplierServiceRestClient {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ESSupplierServiceConfig esSupplierServiceConfig;

    @Override
    @Async
    @Retryable(backoff = @Backoff(value = 3000), maxAttempts = 3, value = PALServiceException.class)
    public CompletableFuture<Suppliers> getSuppliers(ESSupplierDataRequest esSupplierDataRequest) {
        try {
            ResponseEntity<Suppliers> resp = restTemplate.exchange(
                    esSupplierServiceConfig.getEndpoint() + esSupplierServiceConfig.getSupplierIdUrl(),
                    HttpMethod.POST,
                    new HttpEntity<>(esSupplierDataRequest, createAuthHeaders(esSupplierServiceConfig.getUsername(), esSupplierServiceConfig.getPassword())), Suppliers.class);
            return CompletableFuture.completedFuture(resp.getBody());
        } catch (Exception exception) {
            log.warn("Failed getting supplier details for {} due to {}",esSupplierDataRequest, exception.getMessage());
            return CompletableFuture.completedFuture(Suppliers.builder().suppliers(Collections.emptyList()).build());
        }
    }


}
