package com.marksandspencer.foodshub.pal.rest.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marksandspencer.foodshub.pal.constant.ErrorCode;
import com.marksandspencer.foodshub.pal.dto.Category;
import com.marksandspencer.foodshub.pal.exception.PALServiceException;
import com.marksandspencer.foodshub.pal.properties.EnterpriseServiceProductHierarchyConfig;
import com.marksandspencer.foodshub.pal.rest.client.impl.ESProductHierarchyServiceRestClientImpl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@RestClientTest(ESProductHierarchyServiceRestClientImpl.class)
public class ESProductHierarchyServiceRestClientTest {
	@InjectMocks
	ESProductHierarchyServiceRestClientImpl esProductHierarchyServiceRestClient;

	@Mock
	private RestTemplate restTemplate;

	@Mock
	private EnterpriseServiceProductHierarchyConfig enterpriseServiceProductHierarchyConfig;

	private String endPoint = "http://producthierarchy-service-stage.dev.platform.mnscorp.net:80";
	private String categoriesUri = "/api/producthierarchy/v1/nodes/levels/{nodeLevel}/{nodeValue}/children/levels/{childNodeLevel}?size={pageSize}";
	private String authUsername = "testuser";
	private String authPassword = "testpassword";
	private String pageSize = "200";

	private Category categoryResponse = new Category();

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Before
	public void beforeTest() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		categoryResponse = mapper.readValue(new File(
				"src/test/resources/UserListResponse/ESProductHeirarchyResponse.json"),
				mapper.getTypeFactory().constructType(Category.class));

		when(enterpriseServiceProductHierarchyConfig.getEndPoint())
				.thenReturn(endPoint);
		when(enterpriseServiceProductHierarchyConfig.getCategoriesUrl())
				.thenReturn(categoriesUri);
		when(enterpriseServiceProductHierarchyConfig.getAuthUsername())
				.thenReturn(authUsername);
		when(enterpriseServiceProductHierarchyConfig.getAuthPassword())
				.thenReturn(authPassword);
		when(enterpriseServiceProductHierarchyConfig.getCategoriesPageSize())
				.thenReturn(pageSize);
	}

	@Test
	public void getESProductHierarchyServiceTest() {
		ResponseEntity responseEntity = new ResponseEntity(categoryResponse,
				HttpStatus.OK);
		when(restTemplate.exchange(Mockito.anyString(),
				Mockito.same(HttpMethod.GET), Mockito.any(),
				Mockito.eq(Category.class), Mockito.anyMap()))
						.thenReturn(responseEntity);
		Category category = esProductHierarchyServiceRestClient.getCategories();
		assertEquals(responseEntity.getBody(), category);
	}

	@Test
	public void getESProductHierarchyServiceExceptionTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.ES_ERROR.getErrorMessage());
		when(restTemplate.exchange(Mockito.anyString(),
				Mockito.same(HttpMethod.GET), Mockito.any(),
				Mockito.eq(Category.class), Mockito.anyMap()))
						.thenThrow(new RestClientException("exception"));
		esProductHierarchyServiceRestClient.getCategories();
	}

	@Test
	public void getCategoriesHttpServerErrorExceptionScenario() {
		expectedException.expectMessage(ErrorCode.ES_ERROR.getErrorMessage());
		when(restTemplate.exchange(Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.same(Category.class), Mockito.anyMap())).thenThrow(
						new HttpServerErrorException(HttpStatus.BAD_GATEWAY));
		esProductHierarchyServiceRestClient.getCategories();
	}

	@Test
	public void getCategoriesResourceAccessExceptionScenario() {
		expectedException.expectMessage(ErrorCode.ES_ERROR.getErrorMessage());
		when(restTemplate.exchange(Mockito.any(), Mockito.any(), Mockito.any(),
				Mockito.same(Category.class), Mockito.anyMap())).thenThrow(
						new ResourceAccessException("resourceAccessException"));
		esProductHierarchyServiceRestClient.getCategories();
	}

}
