package com.marksandspencer.foodshub.pal.rest.client.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.marksandspencer.foodshub.pal.constant.ApplicationConstant;
import com.marksandspencer.foodshub.pal.exception.PALServiceException;
import com.marksandspencer.foodshub.pal.properties.AuthzPropertyConfig;
import com.marksandspencer.foodshub.pal.rest.client.AuthzRestClient;
import com.marksandspencer.foodshub.pal.transfer.AuthAttribute;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AuthzRestClientImpl implements AuthzRestClient {

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private AuthzPropertyConfig authzPropertyConfig;

	@Override
	@Retryable(value = { HttpServerErrorException.class, ResourceAccessException.class,
			RestClientException.class }, maxAttempts = 2, backoff = @Backoff(delay = 1000))
	public List<AuthAttribute> getAuthAttribute(String origin, String jwt) {
		List<AuthAttribute> authAttributeList = new ArrayList<>();
		try {
			String uri = authzPropertyConfig.getEndPoint() + authzPropertyConfig.getAuthAttributes();
			UriComponents builder = UriComponentsBuilder.fromHttpUrl(uri).queryParam("origin", origin).build();
			ResponseEntity<List<AuthAttribute>> responseEntity = restTemplate.exchange(builder.toUriString(),
					HttpMethod.GET, new HttpEntity<>(origin, createAuthHeaders(jwt)),
					new ParameterizedTypeReference<List<AuthAttribute>>() {
					});
			authAttributeList = responseEntity.getBody();
		} catch (Exception exception) {
			log.error("PAL - Exception occured in getAuthAttribute {}", exception.getMessage());
			throw new PALServiceException(exception.getMessage());
		}
		return authAttributeList;
	}

	/**
	 * Creates the auth headers.
	 *
	 * @param jwt    the jwt
	 * @param module the module
	 * @return the http headers
	 */
	protected HttpHeaders createAuthHeaders(String jwt) {
		HttpHeaders headers = new HttpHeaders();
		String auth = ApplicationConstant.BEARER + jwt;
		headers.set(ApplicationConstant.AUTHORIZATION, auth);
		return headers;
	}

}
