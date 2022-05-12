package com.marksandspencer.foodshub.pal.rest.client;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.marksandspencer.foodshub.pal.dto.Supplier;
import com.marksandspencer.foodshub.pal.dto.Suppliers;
import com.marksandspencer.foodshub.pal.properties.ESSupplierServiceConfig;
import com.marksandspencer.foodshub.pal.rest.client.impl.ESSupplierServiceRestClientImpl;
import com.marksandspencer.foodshub.pal.transfer.ESSupplierDataRequest;
import com.marksandspencer.foodshub.pal.util.Util;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ESSupplierServiceRestClientTest {

	@InjectMocks
	private ESSupplierServiceRestClientImpl esSupplierServiceRestClient = new ESSupplierServiceRestClientImpl();

	@Mock
	private RestTemplate restTemplate;

	@Mock
	private ESSupplierServiceConfig esSupplierServiceConfig;

	Suppliers suppliers;

	private String endpoint = "https://supplier-service-stage.dev.platform.mnscorp.net";

	private String supplierIdUrl = "/api/suppliers/v1/ids";

	private String username = "testUser";

	private String password = "testpass";

	private ResponseEntity<Suppliers> responseEntity;

	private ESSupplierDataRequest esSupplierDataRequest = new ESSupplierDataRequest();

	private List<String> suppliersList = new ArrayList<>();

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	List<CompletableFuture<Suppliers>> futureResult;

	@Before
	public void beforeTest() throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		futureResult = Lists.newArrayList();
		suppliers = mapper.readValue(new File("src/test/resources/UserListResponse/ESSupplierResponse.json"),
				mapper.getTypeFactory().constructType(Suppliers.class));
		responseEntity = new ResponseEntity<Suppliers>(suppliers, HttpStatus.OK);
		suppliersList.add("F01504");
		suppliersList.add("F02489");
		suppliersList.add("F07410");
		esSupplierDataRequest.setSupplierIds(suppliersList);
	}

	@Test
	public void testGetSuppliers() {
		when(esSupplierServiceConfig.getEndpoint()).thenReturn(endpoint);
		when(esSupplierServiceConfig.getSupplierIdUrl()).thenReturn(supplierIdUrl);
		when(esSupplierServiceConfig.getUsername()).thenReturn(username);
		when(esSupplierServiceConfig.getPassword()).thenReturn(password);
		when(restTemplate.exchange(Mockito.anyString(), Mockito.same(HttpMethod.POST), Mockito.any(),
				Mockito.eq(Suppliers.class))).thenReturn(responseEntity);
		CompletableFuture<Suppliers> response = esSupplierServiceRestClient.getSuppliers(esSupplierDataRequest);
		futureResult.add(response);
		CompletableFuture.allOf(futureResult.toArray(new CompletableFuture[futureResult.size()]));
		List<Supplier> suppliersResponse = futureResult.stream().map(Util.<Suppliers>getFutureObject())
				.filter(o -> o != null).flatMap(o -> o.getSuppliers().stream()).collect(Collectors.toList());
		assertEquals(suppliersResponse, suppliers.getSuppliers());
	}

	@Test
	public void testGetSuppliersException() {
		when(esSupplierServiceConfig.getEndpoint()).thenReturn(endpoint);
		when(esSupplierServiceConfig.getSupplierIdUrl()).thenReturn(supplierIdUrl);
		when(esSupplierServiceConfig.getUsername()).thenReturn(username);
		when(esSupplierServiceConfig.getPassword()).thenReturn(password);
		when(restTemplate.exchange(Mockito.anyString(), Mockito.same(HttpMethod.POST), Mockito.any(),
				Mockito.eq(Suppliers.class)))
						.thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "Test exception"));
		CompletableFuture<Suppliers> response =  esSupplierServiceRestClient.getSuppliers(esSupplierDataRequest);
		futureResult.add(response);
		List<Supplier> suppliers = futureResult.stream().map(Util.<Suppliers>getFutureObject())
				.filter(o -> o != null).flatMap(o -> o.getSuppliers().stream()).collect(Collectors.toList());
		assertTrue(suppliers.isEmpty());
	}

}
