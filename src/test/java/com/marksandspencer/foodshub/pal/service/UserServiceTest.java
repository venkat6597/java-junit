package com.marksandspencer.foodshub.pal.service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marksandspencer.foodshub.pal.constant.ApplicationConstant;
import com.marksandspencer.foodshub.pal.constant.ErrorCode;
import com.marksandspencer.foodshub.pal.domain.Configuration;
import com.marksandspencer.foodshub.pal.dto.PALUser;
import com.marksandspencer.foodshub.pal.dto.Supplier;
import com.marksandspencer.foodshub.pal.dto.Suppliers;
import com.marksandspencer.foodshub.pal.exception.PALServiceException;
import com.marksandspencer.foodshub.pal.repository.ConfigurationRepository;
import com.marksandspencer.foodshub.pal.rest.client.AuthzRestClient;
import com.marksandspencer.foodshub.pal.rest.client.AzureRest;
import com.marksandspencer.foodshub.pal.rest.client.ESSupplierServiceRestClient;
import com.marksandspencer.foodshub.pal.serviceImpl.UserServiceImpl;
import com.marksandspencer.foodshub.pal.transfer.AuthAttribute;
import com.marksandspencer.foodshub.pal.transfer.AuthzSupplierResponse;
import com.marksandspencer.foodshub.pal.transfer.ESSupplierDataRequest;
import com.marksandspencer.foodshub.pal.transfer.UserDetails;
import com.marksandspencer.foodshub.pal.utility.TestUtility;
import com.microsoft.graph.requests.DirectoryObjectCollectionWithReferencesPage;
import org.apache.commons.collections4.map.HashedMap;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserServiceTest {

	@InjectMocks
	UserService userService = new UserServiceImpl();

	@Mock
	AuthzRestClient authzRestClient;

	@Mock
	private AzureRest azureRest;

	@Mock
	List<AuthAttribute> authAttribute;

	@Mock
	private DirectoryObjectCollectionWithReferencesPage directoryObjectCollectionWithReferencesPage;

	List<AuthzSupplierResponse> supplierIdResponse;
	@Mock
	UserDetailsService userDetailsService;

	@Captor
	ArgumentCaptor<String> jwtCaptor;

	@Captor
	ArgumentCaptor<String> originCaptor;

	String token = "JWT_TOKEN";

	Suppliers esSupplierResponse;

	@Mock
	private ESSupplierServiceRestClient esSupplierServiceRestClient;

	@Mock
	private ConfigurationRepository configurationRepository;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Before
	public void beforeTest() {
		ReflectionTestUtils.setField(userService, "suppliersPageSize", 50);
	}

	@Test
	public void getSupplierIdTest() throws IOException {
		Configuration configuration = null;
		ObjectMapper mapper = new ObjectMapper();
		authAttribute = mapper.readValue(new File("src/test/resources/UserListResponse/AuthzAttributeResponse.json"),
				mapper.getTypeFactory().constructCollectionLikeType(List.class, AuthAttribute.class));

		supplierIdResponse = mapper.readValue(new File("src/test/resources/UserListResponse/SupplierIdResponse.json"),
				mapper.getTypeFactory().constructCollectionLikeType(List.class, AuthzSupplierResponse.class));

		esSupplierResponse = mapper.readValue(new File("src/test/resources/UserListResponse/ESSupplierResponse.json"),
				mapper.getTypeFactory().constructType(Suppliers.class));

		when(authzRestClient.getAuthAttribute(Mockito.anyString(), Mockito.anyString())).thenReturn(authAttribute);

		when(configurationRepository.findAllByType(Mockito.anyString())).thenReturn(configuration);

		when(esSupplierServiceRestClient.getSuppliers(Mockito.any()))
				.thenReturn(CompletableFuture.completedFuture(esSupplierResponse));

		when(userDetailsService.getUserDetails())
				.thenReturn(TestUtility.getUserDetails(ApplicationConstant.PROJECT_MANAGER));

		List<AuthzSupplierResponse> supplierIdActualResponse = userService.getSupplierIds(token);
		Mockito.verify(authzRestClient, Mockito.times(1)).getAuthAttribute(originCaptor.capture(), jwtCaptor.capture());
		assertEquals(supplierIdResponse, supplierIdActualResponse);
		assertEquals(ApplicationConstant.OFP, originCaptor.getValue());
		assertEquals(token, jwtCaptor.getValue());
	}

	@Test
	public void getSupplierIdFilterTest() throws JsonParseException, JsonMappingException, IOException {
		Configuration configuration = new Configuration();
		List<Map<String, Object>> values = new ArrayList<>();
		Map<String, Object> valueMap = new HashedMap<>();
		valueMap.put(ApplicationConstant.PAGE_OBJECT, ApplicationConstant.FSP_PAL_PROJECT);
		List<String> organizations = new ArrayList<>();
		String supplierId = "F02489";
		organizations.add(supplierId);
		valueMap.put(ApplicationConstant.ORGANIZATIONS, organizations);
		values.add(valueMap);
		configuration.setValues(values);
		ObjectMapper mapper = new ObjectMapper();
		authAttribute = mapper.readValue(new File("src/test/resources/UserListResponse/AuthzAttributeResponse.json"),
				mapper.getTypeFactory().constructCollectionLikeType(List.class, AuthAttribute.class));

		supplierIdResponse = mapper.readValue(new File("src/test/resources/UserListResponse/SupplierIdResponse.json"),
				mapper.getTypeFactory().constructCollectionLikeType(List.class, AuthzSupplierResponse.class));

		esSupplierResponse = mapper.readValue(new File("src/test/resources/UserListResponse/ESSupplierResponse.json"),
				mapper.getTypeFactory().constructType(Suppliers.class));

		when(authzRestClient.getAuthAttribute(Mockito.anyString(), Mockito.anyString())).thenReturn(authAttribute);

		when(configurationRepository.findAllByType(Mockito.anyString())).thenReturn(configuration);

		when(esSupplierServiceRestClient.getSuppliers(Mockito.any()))
				.thenReturn(CompletableFuture.completedFuture(esSupplierResponse));

		when(userDetailsService.getUserDetails())
				.thenReturn(TestUtility.getUserDetails(ApplicationConstant.PROJECT_MANAGER));

		List<AuthzSupplierResponse> supplierIdActualResponse = userService.getSupplierIds(token);
		Mockito.verify(authzRestClient, Mockito.times(1)).getAuthAttribute(originCaptor.capture(), jwtCaptor.capture());
		assertEquals(1, supplierIdActualResponse.size());
		assertEquals(supplierId, supplierIdActualResponse.get(0).getSupplierId());
		assertEquals(ApplicationConstant.OFP, originCaptor.getValue());
		assertEquals(token, jwtCaptor.getValue());
	}

	@Test
	public void getSupplierOrganizationFilterTest() throws JsonParseException, JsonMappingException, IOException {
		Configuration configuration = new Configuration();
		List<Map<String, Object>> values = new ArrayList<>();
		Map<String, Object> valueMap = new HashedMap<>();
		valueMap.put(ApplicationConstant.PAGE_OBJECT, ApplicationConstant.FSP_PAL_PROJECT);
		List<String> organizations = Arrays.asList("F02489", "F01524", "F07410");
		valueMap.put(ApplicationConstant.ORGANIZATIONS, organizations);
		values.add(valueMap);
		configuration.setValues(values);
		ObjectMapper mapper = new ObjectMapper();
		authAttribute = mapper.readValue(new File("src/test/resources/UserListResponse/AuthzAttributeResponse.json"),
				mapper.getTypeFactory().constructCollectionLikeType(List.class, AuthAttribute.class));

		supplierIdResponse = mapper.readValue(new File("src/test/resources/UserListResponse/SupplierIdResponse.json"),
				mapper.getTypeFactory().constructCollectionLikeType(List.class, AuthzSupplierResponse.class));

		esSupplierResponse = mapper.readValue(new File("src/test/resources/UserListResponse/ESSupplierResponse.json"),
				mapper.getTypeFactory().constructType(Suppliers.class));

		when(authzRestClient.getAuthAttribute(Mockito.anyString(), Mockito.anyString())).thenReturn(authAttribute);

		when(configurationRepository.findAllByType(Mockito.anyString())).thenReturn(configuration);

		UserDetails userDetails = TestUtility.getUserDetails(ApplicationConstant.SUPPLIER);
		userDetails.getUserRole().setRoleName(ApplicationConstant.SUPPLIER_ROLE_NAME);

		List<String> userOrganization = userDetails.getOrganizations();
		List<Supplier> suppliers = esSupplierResponse.getSuppliers().stream()
				.filter(supplier -> userOrganization.contains(supplier.getSupplierId())).collect(Collectors.toList());
		Suppliers essupplier = new Suppliers();
		essupplier.setSuppliers(suppliers);
		ESSupplierDataRequest esSupplierDataRequest = new ESSupplierDataRequest();
		esSupplierDataRequest.setSupplierIds(userOrganization);
		when(esSupplierServiceRestClient.getSuppliers(eq(esSupplierDataRequest)))
				.thenReturn(CompletableFuture.completedFuture(essupplier));

		when(userDetailsService.getUserDetails()).thenReturn(userDetails);

		List<AuthzSupplierResponse> supplierIdActualResponse = userService.getSupplierIds(token);
		Mockito.verify(authzRestClient, Mockito.times(1)).getAuthAttribute(originCaptor.capture(), jwtCaptor.capture());
		assertEquals(1, supplierIdActualResponse.size());
		assertEquals(userDetails.getOrganizations().get(0), supplierIdActualResponse.get(0).getSupplierId());
		assertEquals(ApplicationConstant.OFP, originCaptor.getValue());
		assertEquals(token, jwtCaptor.getValue());
	}

	@Test
	public void getSupplierOrganizationUnathorizedTest() throws JsonParseException, JsonMappingException, IOException {
		Configuration configuration = new Configuration();
		List<Map<String, Object>> values = new ArrayList<>();
		Map<String, Object> valueMap = new HashedMap<>();
		valueMap.put(ApplicationConstant.PAGE_OBJECT, ApplicationConstant.FSP_PAL_PROJECT);
		List<String> organizations = Arrays.asList("F02489", "FO1524", "F07410");
		valueMap.put(ApplicationConstant.ORGANIZATIONS, organizations);
		values.add(valueMap);
		configuration.setValues(values);
		ObjectMapper mapper = new ObjectMapper();
		authAttribute = mapper.readValue(new File("src/test/resources/UserListResponse/AuthzAttributeResponse.json"),
				mapper.getTypeFactory().constructCollectionLikeType(List.class, AuthAttribute.class));

		when(authzRestClient.getAuthAttribute(Mockito.anyString(), Mockito.anyString())).thenReturn(authAttribute);

		when(configurationRepository.findAllByType(Mockito.anyString())).thenReturn(configuration);

		UserDetails userDetails = TestUtility.getUserDetails(ApplicationConstant.SUPPLIER);
		userDetails.getUserRole().setRoleName(ApplicationConstant.SUPPLIER_ROLE_NAME);
		userDetails.setOrganizations(null);
		when(userDetailsService.getUserDetails()).thenReturn(userDetails);
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.UNAUTHORIIZED.getErrorMessage());
		userService.getSupplierIds(token);

	}

	@Test(expected = PALServiceException.class)
	public void getSuppliersExceptionTest() throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		authAttribute = mapper.readValue(new File("src/test/resources/UserListResponse/AuthzAttributeResponse.json"),
				mapper.getTypeFactory().constructCollectionLikeType(List.class, AuthAttribute.class));

		when(authzRestClient.getAuthAttribute(Mockito.anyString(), Mockito.anyString())).thenReturn(authAttribute);

		userService.getSupplierIds(token);
	}

	@Test
	public void listUserByRolesTest() {
		List<PALUser> users = new ArrayList<>();
		users.add(PALUser.builder().name("xyz").id("123").email("xyz").build());
		when(azureRest.listUserByRole(any())).thenReturn(users);
		Map<String, List<PALUser>> palUsers = userService.listUserByRoles(Arrays.asList("123"));
		assertNotNull(palUsers);
		assertEquals(1, palUsers.size());
	}

}
