package com.marksandspencer.foodshub.pal.rest.client.impl;

import com.marksandspencer.foodshub.pal.constant.ApplicationConstant;
import com.marksandspencer.foodshub.pal.constant.ErrorCode;
import com.marksandspencer.foodshub.pal.dto.Category;
import com.marksandspencer.foodshub.pal.exception.PALServiceException;
import com.marksandspencer.foodshub.pal.properties.EnterpriseServiceProductHierarchyConfig;
import com.marksandspencer.foodshub.pal.rest.client.ESProductHierarchyServiceRestClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class ESProductHierarchyServiceRestClientImpl extends RestClient implements ESProductHierarchyServiceRestClient {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private EnterpriseServiceProductHierarchyConfig enterpriseServiceProductHierarchyConfig;

    @Override
    @Async
    @Retryable(value = PALServiceException.class , maxAttempts = 2, backoff = @Backoff(delay = 1000))
    public Category getCategories() {
        Map<String, String> params = new HashMap<>();
        params.put("nodeLevel", ApplicationConstant.NODELEVEL);
        params.put("nodeValue", ApplicationConstant.NODEVALUE);
        params.put("childNodeLevel", ApplicationConstant.CHILDNODELEVEL);
        params.put("pageSize", enterpriseServiceProductHierarchyConfig.getCategoriesPageSize());
        try {
            ResponseEntity<Category> resp = restTemplate.exchange(
                    enterpriseServiceProductHierarchyConfig.getEndPoint() + enterpriseServiceProductHierarchyConfig.getCategoriesUrl(), HttpMethod.GET,
                    new HttpEntity<>(createAuthHeaders(enterpriseServiceProductHierarchyConfig.getAuthUsername(), enterpriseServiceProductHierarchyConfig.getAuthPassword())), Category.class, params);
            return resp.getBody();
        } catch (HttpServerErrorException httpSerErrEx) {
            log.error("Error occurred in ES Call Category : httpSerErrEx - RestClient :- " + httpSerErrEx.getMessage());
            throw new PALServiceException(ErrorCode.ES_ERROR);
        } catch (ResourceAccessException resourceAccEx) {
            log.error("Error occurred in ES Call Category : resourceAccEx - RestClient :- " + resourceAccEx.getMessage());
            throw new PALServiceException(ErrorCode.ES_ERROR);
        } catch (RestClientException restClientEx) {
            log.error("Error occurred in getRequest : restClientEx - RestClient :- " + restClientEx.getMessage());
            throw new PALServiceException(ErrorCode.ES_ERROR);
        }
    }

}
