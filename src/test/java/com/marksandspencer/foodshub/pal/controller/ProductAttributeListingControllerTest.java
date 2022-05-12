package com.marksandspencer.foodshub.pal.controller;


import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marksandspencer.foodshub.pal.constant.ApplicationConstant;
import com.marksandspencer.foodshub.pal.constant.ErrorCode;
import com.marksandspencer.foodshub.pal.constant.Status;
import com.marksandspencer.foodshub.pal.domain.PALConfiguration;
import com.marksandspencer.foodshub.pal.domain.PALFields;
import com.marksandspencer.foodshub.pal.domain.PALRole;
import com.marksandspencer.foodshub.pal.domain.PALTemplate;
import com.marksandspencer.foodshub.pal.exception.PALServiceException;
import com.marksandspencer.foodshub.pal.service.ProductAttributeListingService;
import com.marksandspencer.foodshub.pal.transfer.AppResponse;
import com.marksandspencer.foodshub.pal.transfer.BulkProductRequest;
import com.marksandspencer.foodshub.pal.transfer.BulkProductResponse;
import com.marksandspencer.foodshub.pal.transfer.BulkProductUpdateRequest;
import com.marksandspencer.foodshub.pal.transfer.BulkProductUpdateResponse;
import com.marksandspencer.foodshub.pal.transfer.DuplicateProductRequest;
import com.marksandspencer.foodshub.pal.transfer.DuplicateProductResponse;
import com.marksandspencer.foodshub.pal.transfer.PALDeleteRequest;
import com.marksandspencer.foodshub.pal.transfer.PALDeleteResponse;
import com.marksandspencer.foodshub.pal.transfer.PALExportRequest;
import com.marksandspencer.foodshub.pal.transfer.PALExportResponse;
import com.marksandspencer.foodshub.pal.transfer.PALProductCreateRequest;
import com.marksandspencer.foodshub.pal.transfer.PALProductRequest;
import com.marksandspencer.foodshub.pal.transfer.PALProductResponse;
import com.marksandspencer.foodshub.pal.transfer.PALProductUpdateRequest;
import com.marksandspencer.foodshub.pal.transfer.PALProjectPersonnelUpdateRequest;
import com.marksandspencer.foodshub.pal.transfer.PALProjectRequest;
import com.marksandspencer.foodshub.pal.transfer.PALProjectResponse;
import com.marksandspencer.foodshub.pal.transfer.PALProjectUpdateRequest;
import com.marksandspencer.foodshub.pal.transfer.PALTemplateRequest;
import com.marksandspencer.foodshub.pal.transfer.ProjectFilter;
import com.marksandspencer.foodshub.pal.utility.TestUtility;

@RunWith(MockitoJUnitRunner.class)
public class ProductAttributeListingControllerTest {

	@InjectMocks
	private ProductAttributeListingController productAttributeListingController;

	@Mock
	private ProductAttributeListingService palService;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	private List<PALTemplate> palTemplates;
	
	private List<String> palTemplateNames;

	@Before
	public void beforeTest() throws IOException {
		ObjectMapper mapper = new ObjectMapper();

		palTemplates = mapper.readValue(new File("src/test/resources/ProductAttributeListingResponse/db_PALTemplates.json"),
				new TypeReference<>() {});
		palTemplateNames = palTemplates.stream().map(PALTemplate::getTemplateName).collect(Collectors.toList());

	}

	@SuppressWarnings("unchecked")
	@Test
	public void getPALProductInformationTest() {
		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductInformationTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);
				
		PALProductRequest request = (PALProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProductRequest>() {});

		PALProductResponse palProductResponse = new PALProductResponse();
		palProductResponse.setId("123");

		when(palService.getPALProductInformation(request)).thenReturn(palProductResponse);
		ResponseEntity<AppResponse<PALProductResponse>> actualResponse =
				productAttributeListingController.getPALProductInformation(request, null, null, null, null, null);
		assertEquals(palProductResponse, actualResponse.getBody().getData());
		assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void getPALProductPersonnelTest() {
		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductPersonnelTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);
				
		PALProductRequest request = (PALProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProductRequest>() {});

		PALProductResponse palProductResponse = new PALProductResponse();
		palProductResponse.setId("123");

		when(palService.getPALProductPersonnel(request)).thenReturn(palProductResponse);
		ResponseEntity<AppResponse<PALProductResponse>> actualResponse =
				productAttributeListingController.getPALProductPersonnel(request, null, null, null, null, null);
		assertEquals(palProductResponse, actualResponse.getBody().getData());
		assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
	}
	
	@Test
	public void getPALProductInformationInvalidRequestTest() {		
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_REQUEST_DATA.getErrorMessage());

		PALProductRequest request = new PALProductRequest();
		request.setProductId("");
		when(palService.getPALProductInformation(request)).thenThrow(new PALServiceException(ErrorCode.INVALID_REQUEST_DATA));
		productAttributeListingController.getPALProductInformation(request, null, null, null, null, null);
	}
	
	@Test
	public void getPALProductPersonnelInvalidRequestTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_REQUEST_DATA.getErrorMessage());
		
		PALProductRequest request = new PALProductRequest();
		request.setProductId("");
		when(palService.getPALProductPersonnel(request)).thenThrow(new PALServiceException(ErrorCode.INVALID_REQUEST_DATA));
		productAttributeListingController.getPALProductPersonnel(request, null, null, null, null, null);
	}
	
	@Test
	public void getPALProductInformationNullResponseTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.NO_DATA.getErrorMessage());
		
		PALProductRequest request = new PALProductRequest();
		request.setProductId("abcd");
		request.setProductId("buyer");
		when(palService.getPALProductInformation(request)).thenThrow(new PALServiceException(ErrorCode.NO_DATA));
		productAttributeListingController.getPALProductInformation(request, null, null, null, null, null);
	}
	
	@Test
	public void getPALProductPersonnelNullResponseTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.NO_DATA.getErrorMessage());
		
		PALProductRequest request = new PALProductRequest();
		request.setProductId("abcd");
		request.setProductId("buyer");
		when(palService.getPALProductPersonnel(request)).thenThrow(new PALServiceException(ErrorCode.NO_DATA));
		productAttributeListingController.getPALProductPersonnel(request, null, null, null, null, null);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void updatePALProductPersonnelTest() {
		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductPersonnelUpdateTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);
				
		PALProductUpdateRequest request = (PALProductUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProductUpdateRequest>() {});
		
		AppResponse<PALProductResponse> appresponse =  (AppResponse<PALProductResponse>) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("response", null),
				new TypeReference<AppResponse<PALProductResponse>>() {});
		PALProductResponse expectedResponse = appresponse.getData();
		
		when(palService.updatePALProductPersonnel(request)).thenReturn(expectedResponse);
		ResponseEntity<AppResponse<PALProductResponse>> actualResponse =
				productAttributeListingController.updatePALProductPersonnel(request, null, null, null, null, null);
		assertEquals(expectedResponse.getPersonnel(), actualResponse.getBody().getData().getPersonnel());
		assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
	}
	
	@Test
	public void updatePALProductPersonnelInvalidRequestTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_REQUEST_DATA.getErrorMessage());
		
		PALProductUpdateRequest request = new PALProductUpdateRequest();
		request.setProductId("");
		when(palService.updatePALProductPersonnel(request)).thenThrow(new PALServiceException(ErrorCode.INVALID_REQUEST_DATA));
		productAttributeListingController.updatePALProductPersonnel(request, null, null, null, null, null);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void updatePALProductPersonnelNoDataTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.NO_DATA.getErrorMessage());
		
		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductPersonnelUpdateTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);
				
		PALProductUpdateRequest request = (PALProductUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProductUpdateRequest>() {});
		request.setProductId("aaaa");
		when(palService.updatePALProductPersonnel(request)).thenThrow(new PALServiceException(ErrorCode.NO_DATA));
		productAttributeListingController.updatePALProductPersonnel(request, null, null, null, null, null);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void updatePALProductInformationTest() {
		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductInformationUpdateTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);
				
		PALProductUpdateRequest request = (PALProductUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProductUpdateRequest>() {});

		PALProductResponse palProductResponse = new PALProductResponse();
		palProductResponse.setId("123");

		when(palService.updatePALProductInformation(request)).thenReturn(palProductResponse);
		ResponseEntity<AppResponse<PALProductResponse>> actualResponse =
				productAttributeListingController.updatePALProductInformation(request, null, null, null, null, null);
		assertEquals(palProductResponse, actualResponse.getBody().getData());
		assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
	}

	@Test
	public void updatePALProductInformationInvalidRequestTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_REQUEST_DATA.getErrorMessage());
		
		PALProductUpdateRequest request = new PALProductUpdateRequest();
		request.setProductId("");
		when(palService.updatePALProductInformation(request)).thenThrow(new PALServiceException(ErrorCode.INVALID_REQUEST_DATA));
		productAttributeListingController.updatePALProductInformation(request, null, null, null, null, null);
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void updatePALProductInformationNoDataTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.NO_DATA.getErrorMessage());
		
		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductInformationUpdateTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);
				
		PALProductUpdateRequest request = (PALProductUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProductUpdateRequest>() {});
		request.setProductId("aaaa");
		when(palService.updatePALProductInformation(request)).thenThrow(new PALServiceException(ErrorCode.NO_DATA));
		productAttributeListingController.updatePALProductInformation(request, null, null, null, null, null);
	}

	@Test
	public void listRolesTest() throws  IOException {
		ObjectMapper mapper = new ObjectMapper();
		AppResponse<List<PALRole>> response = mapper.readValue(new File("src/test/resources/ProductAttributeListingResponse/ListRolesResponse.json"),
				new TypeReference<AppResponse<List<PALRole>>>(){});
		List<PALFields> palFields = mapper.readValue(new File("src/test/resources/ProductAttributeListingResponse/db_PALFields.json"),
				new TypeReference<>() {});
		List<PALRole> PALRoles = response.getData();
		when(palService.listRoles()).thenReturn(PALRoles);
		ResponseEntity<AppResponse<List<PALRole>>> res = productAttributeListingController.listRoles();
		assertEquals(new ResponseEntity<>(response, HttpStatus.OK), res);
	}

	@Test
	public void getPalFieldsTest() throws  IOException {
		ObjectMapper mapper = new ObjectMapper();
		AppResponse<List<PALFields>> response = mapper.readValue(new File("src/test/resources/ProductAttributeListingResponse/PALFields.json"),
				new TypeReference<AppResponse<List<PALFields>>>(){});
		List<PALFields> PALFields = response.getData();
		when(palService.getPalFields()).thenReturn(PALFields);
		ResponseEntity<AppResponse<List<PALFields>>> res = productAttributeListingController.getPalFields();
		assertEquals(new ResponseEntity<>(response, HttpStatus.OK), res);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void getPALProductListTest() {
		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductListTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);
				
		PALProjectRequest request = (PALProjectRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProjectRequest>() {});
		
		AppResponse<PALProjectResponse> appresponse =  (AppResponse<PALProjectResponse>) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("response", null),
				new TypeReference<AppResponse<PALProjectResponse>>() {});
		PALProjectResponse expectedResponse = appresponse.getData();
		
		when(palService.getPALProductList(request)).thenReturn(expectedResponse);
		ResponseEntity<AppResponse<PALProjectResponse>> actualResponse =
				productAttributeListingController.getPALProductList(request, null, null, null, null, null);
		assertEquals(expectedResponse.getProducts(), actualResponse.getBody().getData().getProducts());
		assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
	}

	
	@SuppressWarnings("unchecked")
	@Test
	public void addPALProjectTest() {
		
		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProjectTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);
				
		PALProjectUpdateRequest request = (PALProjectUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProjectUpdateRequest>() {});
		
		AppResponse<PALProjectResponse> appresponse =  (AppResponse<PALProjectResponse>) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("response", null),
				new TypeReference<AppResponse<PALProjectResponse>>() {});
		PALProjectResponse expectedResponse = appresponse.getData();
		
		
		when(palService.addPALProject(request)).thenReturn(expectedResponse);
		ResponseEntity<AppResponse<PALProjectResponse>> actualResponse =
				productAttributeListingController.addPALProject(request, null, null, null, null, null);
		assertEquals(HttpStatus.CREATED, actualResponse.getStatusCode());
		assertNotNull(actualResponse.getBody().getData().getId());
	}
	


	@SuppressWarnings("unchecked")
	@Test
	public void createPALProductTest() {
		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductCreateTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProductCreateRequest request = (PALProductCreateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProductCreateRequest>() {});

		AppResponse<PALProductResponse> appresponse =  (AppResponse<PALProductResponse>) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("response", null),
				new TypeReference<AppResponse<PALProductResponse>>() {});
		PALProductResponse expectedResponse = appresponse.getData();

		when(palService.createPALProduct(request)).thenReturn(expectedResponse);
		when(palService.createPALProduct(request)).thenReturn(expectedResponse);
		ResponseEntity<AppResponse<PALProductResponse>> actualResponse =
				productAttributeListingController.createPALProduct(request, null, null, null, null, null);
		assertEquals(expectedResponse.getSections(), actualResponse.getBody().getData().getSections());
		assertNotNull(actualResponse.getBody().getData().getHeader());
	}


	@SuppressWarnings("unchecked")
	@Test
	public void getPALProductProgressTest() {
		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductProgressTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProductRequest request = (PALProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProductRequest>() {});

		AppResponse<PALProductResponse> appresponse =  (AppResponse<PALProductResponse>) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("response", null),
				new TypeReference<AppResponse<PALProductResponse>>() {});
		PALProductResponse expectedResponse = appresponse.getData();

		when(palService.getPALProductProgress(request)).thenReturn(expectedResponse);
		ResponseEntity<AppResponse<PALProductResponse>> actualResponse =
				productAttributeListingController.getPALProductProgress(request, null, null, null, null, null);
		assertEquals(expectedResponse.getProgress(), actualResponse.getBody().getData().getProgress());
		assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void getPALProductAuditlogsTest() {
		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductAuditTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProductRequest request = (PALProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProductRequest>() {});

		AppResponse<PALProductResponse> appresponse =  (AppResponse<PALProductResponse>) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("response", null),
				new TypeReference<AppResponse<PALProductResponse>>() {});
		PALProductResponse expectedResponse = appresponse.getData();

		when(palService.getPALProductAuditlogs(request)).thenReturn(expectedResponse);
		ResponseEntity<AppResponse<PALProductResponse>> actualResponse =
				productAttributeListingController.getPALProductAuditlogs(request, null, null, null, null, null);
		assertEquals(expectedResponse.getAuditlogs(), actualResponse.getBody().getData().getAuditlogs());
		assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void getPALProjectProgressTest() {
		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProjectProgressTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProjectRequest request = (PALProjectRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProjectRequest>() {});

		AppResponse<PALProjectResponse> appresponse =  (AppResponse<PALProjectResponse>) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("response", null),
				new TypeReference<AppResponse<PALProjectResponse>>() {});
		PALProjectResponse expectedResponse = appresponse.getData();

		when(palService.getPALProjectProgress(request)).thenReturn(expectedResponse);
		ResponseEntity<AppResponse<PALProjectResponse>> actualResponse =
				productAttributeListingController.getPALProjectProgress(request, null, null, null, null, null);
		assertEquals(expectedResponse.getProgress(), actualResponse.getBody().getData().getProgress());
		assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void updatePALProjectTest() throws JsonParseException, JsonMappingException, IOException {
		
		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProjectTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);
				
		PALProjectUpdateRequest request = (PALProjectUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("updateRequest", null),
				new TypeReference<PALProjectUpdateRequest>() {});
		
		AppResponse<PALProjectResponse> appresponse =  (AppResponse<PALProjectResponse>) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("updateResponse", null),
				new TypeReference<AppResponse<PALProjectResponse>>() {});
		PALProjectResponse expectedResponse = appresponse.getData();
		
		
		when(palService.updatePALProject(request)).thenReturn(expectedResponse);
		ResponseEntity<AppResponse<PALProjectResponse>> actualResponse =
				productAttributeListingController.updatePALProject(request, null, null, null, null, null);
		assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
		assertEquals(actualResponse.getBody().getData().getInformation().getStatus(), Status.ARCHIVED.getStatus());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void palExportDataTest() {
		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductExport.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALExportRequest request = (PALExportRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALExportRequest>() {});

		PALExportResponse response = new PALExportResponse("Test", new ByteArrayResource("Test".getBytes()));
		when(palService.palExportData(request)).thenReturn(response);
		ResponseEntity<ByteArrayResource> actualResponse =
				productAttributeListingController.download(request, null, null, null, null, null);
		assertEquals(response.getOut().getByteArray(), actualResponse.getBody().getByteArray());
		assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
	}


	@SuppressWarnings("unchecked")
	@Test
	public void updatePALProjectPersonnelTest() {
		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProjectUpdateTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProjectPersonnelUpdateRequest request = (PALProjectPersonnelUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProjectPersonnelUpdateRequest>() {});

		AppResponse<PALProjectResponse> appresponse =  (AppResponse<PALProjectResponse>) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("response", null),
				new TypeReference<AppResponse<PALProjectResponse>>() {});
		PALProjectResponse expectedResponse = appresponse.getData();

		when(palService.updatePALProjectPersonnel(request)).thenReturn(expectedResponse);
		ResponseEntity<AppResponse<PALProjectResponse>> actualResponse =
				productAttributeListingController.updatePALProjectPersonnel(request, null, null, null, null, null);
		assertEquals(expectedResponse, actualResponse.getBody().getData());
		assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void getPALProjectListTest() {
		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProjectFilter.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);
	
		ProjectFilter request = (ProjectFilter) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<ProjectFilter>() {});
		AppResponse<List<PALProjectResponse>> appresponse = (AppResponse<List<PALProjectResponse>>) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("response", null),
				new TypeReference<AppResponse<List<PALProjectResponse>>>() {});
		List<PALProjectResponse> expectedResponse = appresponse.getData();
	
		when(palService.getPALProjectList(request)).thenReturn((List<PALProjectResponse>) expectedResponse);
		ResponseEntity<AppResponse<List<PALProjectResponse>>> actualResponse =
				productAttributeListingController.getPALProjectList(request, null, null, null, null, null);
		assertEquals(expectedResponse,actualResponse.getBody().getData());
		assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void getPALProjectListNegativeTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.NO_DATA.getErrorMessage());
		
		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProjectFilter.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);
				
		ProjectFilter request = (ProjectFilter) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("requestForNegative", null),
				new TypeReference<ProjectFilter>() {});
		when(palService.getPALProjectList(request)).thenThrow(new PALServiceException(ErrorCode.NO_DATA));
		productAttributeListingController.getPALProjectList(request, null, null, null, null, null);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void getPALTemplatesInvalidTemplateTest() {

		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_TEMPLATE_ID.getErrorMessage());
		PALTemplateRequest request = new PALTemplateRequest();
		when(palService.getPALTemplates(any())).thenThrow(new PALServiceException(ErrorCode.INVALID_TEMPLATE_ID));
		productAttributeListingController.getPALTemplates(request,  null, null, null, null, null);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void getPALTemplatesWithoutSectionTest() {
		PALTemplateRequest request = new PALTemplateRequest();
		request.setSectionsRequired(false);
		palTemplates.forEach(template -> template.setSections(null));
		when(palService.getPALTemplates(any())).thenReturn(palTemplates);
		ResponseEntity<AppResponse<List<PALTemplate>>> responseEntity = productAttributeListingController.getPALTemplates(request,  null, null, null, null, null);
		List<PALTemplate> response = responseEntity.getBody().getData();
		assertEquals(palTemplates, response);
		assertEquals(palTemplateNames,response.stream().map(PALTemplate::getTemplateName).collect(Collectors.toList()));
		assertEquals(5, response.size());
		assertNull(response.get(0).getSections());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void getPALTemplatesWithSectionTest() {
		PALTemplateRequest request = new PALTemplateRequest();
		request.setSectionsRequired(true);
		when(palService.getPALTemplates(any())).thenReturn(palTemplates);
		ResponseEntity<AppResponse<List<PALTemplate>>> responseEntity = productAttributeListingController.getPALTemplates(request,  null, null, null, null, null);
		List<PALTemplate> response = responseEntity.getBody().getData();
		assertEquals(palTemplates, response);
		assertEquals(palTemplateNames,response.stream().map(PALTemplate::getTemplateName).collect(Collectors.toList()));
		assertEquals(5, response.size());
		assertNotNull(response.get(0).getSections());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void getPALTemplatesNullRequestTest() {
		PALTemplateRequest request = null;
		when(palService.getPALTemplates(any())).thenReturn(palTemplates);
		ResponseEntity<AppResponse<List<PALTemplate>>> responseEntity = productAttributeListingController.getPALTemplates(request, null, null, null, null, null);
		List<PALTemplate> response = responseEntity.getBody().getData();
		assertEquals(palTemplates, response);
		assertEquals(palTemplateNames,response.stream().map(PALTemplate::getTemplateName).collect(Collectors.toList()));
		assertEquals(5, response.size());
		assertNotNull(response.get(0).getSections());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void duplicateProductsValidTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/DuplicateProduct.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		DuplicateProductRequest request = (DuplicateProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("requestForSameProject", null),
				new TypeReference<DuplicateProductRequest>() {});

		DuplicateProductResponse expectedResponse = (DuplicateProductResponse) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("responseForSameProject", null),
				new TypeReference<DuplicateProductResponse>() {});

		when (palService.duplicateProducts(eq(request))).thenReturn(expectedResponse);
		ResponseEntity<AppResponse<DuplicateProductResponse>> responseEntity =
				productAttributeListingController.duplicateProducts(request, null, null, null, null, null);
		DuplicateProductResponse actualResponse = responseEntity.getBody().getData();
		assertNotNull(actualResponse);
		assertEquals(expectedResponse, actualResponse);
	}

	@Test
	public void duplicateProductsUnauthorizedTest() {
		DuplicateProductRequest request = new DuplicateProductRequest();
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.UNAUTHORIIZED.getErrorMessage());
		when(palService.duplicateProducts(eq(request))).thenThrow(new PALServiceException(ErrorCode.UNAUTHORIIZED));
		productAttributeListingController.duplicateProducts(request, null, null, null, null, null);
	}

	@Test
	public void duplicateProductsMissingMandatoryFieldsTest() {
		DuplicateProductRequest request = new DuplicateProductRequest();
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.MISSING_MANDATORY_FIELDS.getErrorMessage());
		when(palService.duplicateProducts(eq(request))).thenThrow(new PALServiceException(ErrorCode.MISSING_MANDATORY_FIELDS));
		productAttributeListingController.duplicateProducts(request, null, null, null, null, null);
	}

	@Test
	public void duplicateProductsInvalidTemplateTest() {
		DuplicateProductRequest request = new DuplicateProductRequest();
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_TEMPLATE_ID.getErrorMessage());
		when(palService.duplicateProducts(eq(request))).thenThrow(new PALServiceException(ErrorCode.INVALID_TEMPLATE_ID));
		productAttributeListingController.duplicateProducts(request, null, null, null, null, null);
	}

	@Test
	public void duplicateProductsInvalidProjectTest() {
		DuplicateProductRequest request = new DuplicateProductRequest();
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_PROJECT_ID.getErrorMessage());
		when(palService.duplicateProducts(eq(request))).thenThrow(new PALServiceException(ErrorCode.INVALID_PROJECT_ID));
		productAttributeListingController.duplicateProducts(request, null, null, null, null, null);
	}

	@Test
	public void duplicateProductsInvalidProductTest() {
		DuplicateProductRequest request = new DuplicateProductRequest();
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_PRODUCT_ID.getErrorMessage());
		when (palService.duplicateProducts(eq(request))).thenThrow(new PALServiceException(ErrorCode.INVALID_PRODUCT_ID));
		productAttributeListingController.duplicateProducts(request, null, null, null, null, null);
	}

	@Test
	public void bulkProductUpdateMissingMandatoryFieldsTest() {
		BulkProductUpdateRequest request = new BulkProductUpdateRequest();
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.MISSING_MANDATORY_FIELDS.getErrorMessage());
		when(palService.bulkUpdateInformation(eq(request))).thenThrow(new PALServiceException(ErrorCode.MISSING_MANDATORY_FIELDS));
		productAttributeListingController.bulkUpdateInformation(request, null, null, null, null, null);
	}

	@Test
	public void bulkProductUpdateUnauthorisedTest() {
		BulkProductUpdateRequest request = new BulkProductUpdateRequest();
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.UNAUTHORIIZED.getErrorMessage());
		when(palService.bulkUpdateInformation(eq(request))).thenThrow(new PALServiceException(ErrorCode.UNAUTHORIIZED));
		productAttributeListingController.bulkUpdateInformation(request, null, null, null, null, null);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void bulkProductUpdateValidTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/BulkProductUpdate.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		BulkProductUpdateRequest request = (BulkProductUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<BulkProductUpdateRequest>() {});

		BulkProductUpdateResponse expectedResponse = (BulkProductUpdateResponse) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("response", null),
				new TypeReference<BulkProductUpdateResponse>() {});
		when (palService.bulkUpdateInformation(eq(request))).thenReturn(expectedResponse);
		ResponseEntity<AppResponse<BulkProductUpdateResponse>> responseEntity =
				productAttributeListingController.bulkUpdateInformation(request, null, null, null, null, null);
		assertNotNull(responseEntity.getBody());
		BulkProductUpdateResponse  actualResponse = responseEntity.getBody().getData();
		assertNotNull(actualResponse);
		assertEquals(expectedResponse, actualResponse);
  }
  
	@Test
	public void getBulkProductInformationsTest() {
		String fileName = "src/test/resources/ProductAttributeListingResponse/BulkProductResponse.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		BulkProductRequest request = (BulkProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<BulkProductRequest>() {});

		AppResponse<BulkProductResponse> expectedResponse = (AppResponse<BulkProductResponse>) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("response", null),
				new TypeReference<AppResponse<BulkProductResponse>>() {});

		when (palService.getBulkProductInformations(eq(request))).thenReturn(expectedResponse.getData());
		ResponseEntity<AppResponse<BulkProductResponse>> responseEntity =
				productAttributeListingController.getBulkProductInformations(request, null, null, null, null, null);
		AppResponse<BulkProductResponse> actualResponse = responseEntity.getBody();
		assertNotNull(actualResponse);
		assertEquals(expectedResponse, actualResponse);
		verify(palService,Mockito.times(1)).getBulkProductInformations(Mockito.any());

	}

	@Test
	public void updatePalConfigControllerInvalidTest() {
		Map<String, String> statuses = new HashMap<>();
		statuses.put(ApplicationConstant.CATEGORY_CONFIG_ID, ApplicationConstant.FAILED);
		when(palService.updatePALConfigs()).thenReturn(statuses);
		ResponseEntity<AppResponse<Map<String,String>>> actualResponse = productAttributeListingController.updatePalConfiguration();
		assertNotNull(actualResponse.getBody().getData());
		assertEquals(statuses, actualResponse.getBody().getData());
	}

	@Test
	public void updatePalConfigControllerValidTest() {
		Map<String, String> statuses = new HashMap<>();
		statuses.put(ApplicationConstant.CATEGORY_CONFIG_ID, ApplicationConstant.SUCCESS);
		when(palService.updatePALConfigs()).thenReturn(statuses);
		ResponseEntity<AppResponse<Map<String,String>>> actualResponse = productAttributeListingController.updatePalConfiguration();
		assertNotNull(actualResponse.getBody().getData());
		assertEquals(statuses, actualResponse.getBody().getData());
	}

	@Test
	public void getPalProductForInvalidRequestTest(){
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_REQUEST_DATA.getErrorMessage());
		PALProjectRequest request = new PALProjectRequest();
		request.setProjectId("");
		when(palService.getProjectDetails(request)).thenThrow(new PALServiceException(ErrorCode.INVALID_REQUEST_DATA));
		productAttributeListingController.getProjectDetails(request,null,null,null,null,null);
	}
	
	@Test
	public void getPALConfigurationsTest() {
		String fileName = "src/test/resources/ProductAttributeListingResponse/PALConfigurations.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> configuratons = TestUtility.readFile(fileName, typeReference);
		List<String> request = (List<String>) TestUtility.convertMapToListObject((List<Object>)configuratons.getOrDefault("request", null),
				new TypeReference<List<String>>() {});

		AppResponse<List<PALConfiguration>> expectedResponse = (AppResponse<List<PALConfiguration>>) TestUtility.convertMapToObject((Map<String,Object>) configuratons.getOrDefault("response", null),
				new TypeReference<AppResponse<List<PALConfiguration>>>() {});
		when(palService.getPALConfigurations(Mockito.anyList())).thenReturn(expectedResponse.getData());
		AppResponse<List<PALConfiguration>> actualResponse = productAttributeListingController.getPALConfigurations(request).getBody();
		assertNotNull(actualResponse.getData());
		assertEquals(expectedResponse, actualResponse);
	}
	
	@Test
	public void getPALProjectResponseTest() {
		PALProjectRequest palProjectRequest = new PALProjectRequest();
		AppResponse<PALProjectResponse> expectedResponse = new AppResponse<>();
		PALProjectResponse palProjectResponse=new PALProjectResponse();
		palProjectResponse.setId("60ff64a1476c45bf24a684d4");
		expectedResponse.setData(palProjectResponse);
		when(palService.getProjectDetails(any())).thenReturn(palProjectResponse);
		AppResponse<PALProjectResponse> actualResponse = productAttributeListingController.getProjectDetails(palProjectRequest, null, null, null, null, null).getBody();
		assertNotNull(actualResponse.getData());
		assertEquals(expectedResponse.getData(), actualResponse.getData());
	}
	

	@Test
	public void listProjectTemplateRolesTest() {
		AppResponse<List<PALRole>> expectedResponse = new AppResponse<List<PALRole>>();
		PALRole palRole =new PALRole();
		palRole.setId("1");
		palRole.setName("projectManager");
		List<PALRole> palRolesList = new ArrayList<PALRole>();
		palRolesList.add(palRole);
		expectedResponse.setData(palRolesList);
		when(palService.listProjectTemplateRoles(Mockito.anyString())).thenReturn(expectedResponse.getData());
		ResponseEntity<AppResponse<List<PALRole>>> responseEntity =
				productAttributeListingController.listProjectTemplateRoles("projectId");
		AppResponse<List<PALRole>> actualResponse = responseEntity.getBody();
		assertNotNull(actualResponse);
		assertEquals(expectedResponse.getData(), actualResponse.getData());
	}

	@Test
	public void deleteProjectOrProduct_InvalidRequestTest(){
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_REQUEST_DATA.getErrorMessage());
		PALDeleteRequest request = new PALDeleteRequest();
		when(palService.deleteProjectOrProducts(request)).thenThrow(new PALServiceException(ErrorCode.INVALID_REQUEST_DATA));
		productAttributeListingController.deleteProjectOrProducts(request,null,null,null,null,null);
	}

	@Test
	public void deleteProjectOrProduct_InvalidProjectRequestTest(){
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_PROJECT_ID.getErrorMessage());
		PALDeleteRequest request = new PALDeleteRequest();
		when(palService.deleteProjectOrProducts(request)).thenThrow(new PALServiceException(ErrorCode.INVALID_PROJECT_ID));
		productAttributeListingController.deleteProjectOrProducts(request,null,null,null,null,null);
	}

	@Test
	public void deleteProjectOrProduct_InvalidProductRequestTest(){
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_PRODUCT_ID.getErrorMessage());
		PALDeleteRequest request = new PALDeleteRequest();
		when(palService.deleteProjectOrProducts(request)).thenThrow(new PALServiceException(ErrorCode.INVALID_PRODUCT_ID));
		productAttributeListingController.deleteProjectOrProducts(request,null,null,null,null,null);
	}

	@Test
	public void deleteProjectOrProduct_validRequestTest(){
		PALDeleteRequest request = new PALDeleteRequest();
		request.setUserRole(ApplicationConstant.PROJECT_MANAGER);
		request.setUser(ApplicationConstant.PROJECT_MANAGER);
		request.setProjectId("123");
		PALDeleteResponse response = new PALDeleteResponse();
		response.setProjectName("dummy");
		response.setProjectStatus("Deleted");
		response.setProjectId("123");
		when(palService.deleteProjectOrProducts(request)).thenReturn(response);
		ResponseEntity<AppResponse<PALDeleteResponse>> responseEntity =
				productAttributeListingController.deleteProjectOrProducts(request,null,null,null,null,null);
		assertNotNull(responseEntity.getBody().getData());
		assertEquals(response.getProjectId(), responseEntity.getBody().getData().getProjectId());
		assertEquals(response.getProjectName(), responseEntity.getBody().getData().getProjectName());
		assertEquals(response.getProjectStatus(), responseEntity.getBody().getData().getProjectStatus());
		assertNull(responseEntity.getBody().getData().getSuccessProducts());
		assertNull(responseEntity.getBody().getData().getFailedProducts());
	}
}
