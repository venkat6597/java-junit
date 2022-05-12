package com.marksandspencer.foodshub.pal.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marksandspencer.foodshub.pal.constant.ErrorCode;
import com.marksandspencer.foodshub.pal.dao.ProductAttributeListingDao;
import com.marksandspencer.foodshub.pal.domain.PALFields;
import com.marksandspencer.foodshub.pal.domain.PALProduct;
import com.marksandspencer.foodshub.pal.domain.PALProject;
import com.marksandspencer.foodshub.pal.domain.PALTemplate;
import com.marksandspencer.foodshub.pal.exception.PALServiceException;
import com.marksandspencer.foodshub.pal.serviceImpl.ProductAttributeListingServiceImpl;
import com.marksandspencer.foodshub.pal.transfer.*;
import com.marksandspencer.foodshub.pal.utility.TestUtility;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ProductAttributeListingServiceCurdOperationTest {

	@InjectMocks
	ProductAttributeListingService  palService = new ProductAttributeListingServiceImpl();

	@Mock
	ProductAttributeListingDao palDao;

	@Mock
	UserDetailsService userDetailsService;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	Map<String, PALTemplate> palTemplateMap;
	List<PALFields> palFields;
	
	@Before
	public void beforeTest() throws IOException {
		ObjectMapper mapper = new ObjectMapper();

		palFields = mapper.readValue(new File("src/test/resources/ProductAttributeListingResponse/db_PALFields.json"),
				new TypeReference<>() {});

		List<PALTemplate> palTemplates = mapper.readValue(new File("src/test/resources/ProductAttributeListingResponse/db_PALTemplates.json"),
				new TypeReference<>() {});
		palTemplateMap = palTemplates.stream()
				.collect(Collectors.toMap(PALTemplate::getTemplateName, template -> template));
		when(palDao.findALLPALTemplate()).thenReturn(palTemplates);

		ReflectionTestUtils.setField(palService, "kafkaEnabled", false);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void createPALProductTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductCreateTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProductCreateRequest request = (PALProductCreateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProductCreateRequest>() {});

		PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct", null),
				new TypeReference<PALProduct>() {});

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		when(palDao.findPALProductById(Mockito.anyString())).thenReturn(palProduct);
		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(palDao.savePALProduct(Mockito.any())).thenReturn(palProduct);
		when(userDetailsService.validateUserDetails(Mockito.any(),Mockito.any()))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));

		PALProductResponse actualResponse = palService.createPALProduct(request);
		assertNotNull(actualResponse);
		assertEquals(request.getPersonnel(), actualResponse.getPersonnel());
		assertNotNull(actualResponse.getHeader());
		assertNotNull(actualResponse.getSections());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void createPALProductInvalidTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductCreateTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);


		PALProductCreateRequest request = (PALProductCreateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("requestInvalid", null),
				new TypeReference<PALProductCreateRequest>() {});
		PALServiceException exception = assertThrows(PALServiceException.class,
				() -> palService.createPALProduct(request));
		Assert.assertEquals(ErrorCode.INVALID_REQUEST_DATA.getErrorMessage(),exception.getErrorMessage());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void updatePALProjectPersonnelTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProjectUpdateTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProjectPersonnelUpdateRequest request = (PALProjectPersonnelUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProjectPersonnelUpdateRequest>() {});

		PALProject palProject =  (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palProject", null),
				new TypeReference<PALProject>() {});

		PALProduct palProduct1 = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct1", null),
				new TypeReference<PALProduct>() {});
		PALProduct palProduct2 = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct2", null),
				new TypeReference<PALProduct>() {});
		PALProduct palProduct3 = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct3", null),
				new TypeReference<PALProduct>() {});
		List<PALProduct> palProducts = new ArrayList<>();
		palProducts.add(palProduct1);
		palProducts.add(palProduct2);
		palProducts.add(palProduct3);

		AppResponse<PALProjectResponse> appresponse =  (AppResponse<PALProjectResponse>) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("response", null),
				new TypeReference<AppResponse<PALProjectResponse>>() {});
		PALProjectResponse expectedResponse = appresponse.getData();


		when(palDao.savePALProject(Mockito.any())).thenReturn(palProject);
		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(palDao.findProductByFilterCondition(Mockito.any(),Mockito.any())).thenReturn(palProducts);
		when(userDetailsService.validateUserDetails(Mockito.any(),Mockito.any()))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		PALProjectResponse actualResponse = palService.updatePALProjectPersonnel(request);
		assertEquals(expectedResponse, actualResponse);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void updatePALProjectPersonnelInvalidTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProjectUpdateTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProjectPersonnelUpdateRequest request = (PALProjectPersonnelUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("requestInvalid", null),
				new TypeReference<PALProjectPersonnelUpdateRequest>() {});

		PALServiceException exception = assertThrows(PALServiceException.class,
				() -> palService.updatePALProjectPersonnel(request));
		Assert.assertEquals(ErrorCode.INVALID_REQUEST_DATA.getErrorMessage(),exception.getErrorMessage());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void updatePALProjectPersonnelNoDataTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProjectUpdateTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProjectPersonnelUpdateRequest request = (PALProjectPersonnelUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("requestNoData", null),
				new TypeReference<PALProjectPersonnelUpdateRequest>() {});

		PALServiceException exception = assertThrows(PALServiceException.class,
				() -> palService.updatePALProjectPersonnel(request));
		Assert.assertEquals(ErrorCode.NO_DATA.getErrorMessage(),exception.getErrorMessage());
	}
}
