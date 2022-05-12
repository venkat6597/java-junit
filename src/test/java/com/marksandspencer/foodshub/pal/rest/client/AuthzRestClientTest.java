package com.marksandspencer.foodshub.pal.rest.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marksandspencer.foodshub.pal.constant.ApplicationConstant;
import com.marksandspencer.foodshub.pal.exception.PALServiceException;
import com.marksandspencer.foodshub.pal.properties.AuthzPropertyConfig;
import com.marksandspencer.foodshub.pal.rest.client.impl.AuthzRestClientImpl;
import com.marksandspencer.foodshub.pal.transfer.AuthAttribute;

@RunWith(MockitoJUnitRunner.class)

public class AuthzRestClientTest {

	@InjectMocks
	AuthzRestClientImpl authzRestClient = new AuthzRestClientImpl();

	@Mock
	RestTemplate restTemplate;

	@Mock
	private AuthzPropertyConfig authzPropertyConfig;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	private String token = "JWT_TOKEN";

	private String endPoint = "http://localhost:8080/Authz/";

	private String attributes = "attributes";

	private ResponseEntity<List<AuthAttribute>> responseEntity;

	private List<AuthAttribute> authAttributeList;
	
	private List<AuthAttribute> authAttributeResponseList;

	@Before
	public void beforeTest() throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		authAttributeList = mapper.readValue(
				new File("src/test/resources/UserListResponse/AuthzAttributeResponse.json"),
				mapper.getTypeFactory().constructCollectionLikeType(List.class, AuthAttribute.class));
		responseEntity = new ResponseEntity<List<AuthAttribute>>(authAttributeList, HttpStatus.OK);
	}

	@Test
	public void authzRestClientPALServiceTest() {
		when(authzPropertyConfig.getEndPoint()).thenReturn(endPoint);
		when(authzPropertyConfig.getAuthAttributes()).thenReturn(attributes);
		when(restTemplate.exchange(Mockito.anyString(), Mockito.same(HttpMethod.GET), Mockito.any(),
				Mockito.eq(new ParameterizedTypeReference<List<AuthAttribute>>() {})))
		.thenReturn(responseEntity);
		authAttributeResponseList = authzRestClient.getAuthAttribute(ApplicationConstant.OFP,token);
		assertEquals(authAttributeList, authAttributeResponseList);
		Mockito.verify(restTemplate,Mockito.times(1)).exchange(Mockito.anyString(), Mockito.same(HttpMethod.GET), Mockito.any(),
				Mockito.eq(new ParameterizedTypeReference<List<AuthAttribute>>() {}));
	}

	@Test(expected = PALServiceException.class)
	public void authzRestClientPALServiceExceptionTest() {
		when(authzPropertyConfig.getEndPoint()).thenReturn(endPoint);
		when(authzPropertyConfig.getAuthAttributes()).thenReturn(attributes);
		when(restTemplate.exchange(Mockito.anyString(), Mockito.same(HttpMethod.GET), Mockito.any(),
				Mockito.eq(new ParameterizedTypeReference<List<AuthAttribute>>() {
				}))).thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Test exception"));
		authzRestClient.getAuthAttribute(ApplicationConstant.OFP, token);
	}
}
