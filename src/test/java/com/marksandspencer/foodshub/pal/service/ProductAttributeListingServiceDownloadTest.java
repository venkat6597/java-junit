package com.marksandspencer.foodshub.pal.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marksandspencer.foodshub.pal.constant.ApplicationConstant;
import com.marksandspencer.foodshub.pal.constant.ErrorCode;
import com.marksandspencer.foodshub.pal.dao.ProductAttributeListingDao;
import com.marksandspencer.foodshub.pal.domain.PALProduct;
import com.marksandspencer.foodshub.pal.domain.PALProject;
import com.marksandspencer.foodshub.pal.domain.PALRole;
import com.marksandspencer.foodshub.pal.domain.PALTemplate;
import com.marksandspencer.foodshub.pal.exception.PALServiceException;
import com.marksandspencer.foodshub.pal.serviceImpl.ProductAttributeListingServiceImpl;
import com.marksandspencer.foodshub.pal.transfer.AppResponse;
import com.marksandspencer.foodshub.pal.transfer.PALExportRequest;
import com.marksandspencer.foodshub.pal.transfer.PALExportResponse;
import com.marksandspencer.foodshub.pal.transfer.UserDetails;
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
import org.springframework.core.io.ByteArrayResource;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ProductAttributeListingServiceDownloadTest {

	@InjectMocks
	ProductAttributeListingService  palService = new ProductAttributeListingServiceImpl();

	@Mock
	ProductAttributeListingDao palDao;

	@Mock
	UserDetailsService userDetailsService;

	@Mock
	ExportHelper exportHelper;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	Map<String, PALTemplate> palTemplateMap;

	@Before
	public void beforeTest() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        AppResponse<List<PALRole>> response = mapper.readValue(new File("src/test/resources/ProductAttributeListingResponse/ListRolesResponse.json"),
                new TypeReference<>() {});
        when(palDao.findAllPALRoles()).thenReturn(response.getData());

        List<PALTemplate> palTemplates = mapper.readValue(new File("src/test/resources/ProductAttributeListingResponse/db_PALTemplates.json"),
                new TypeReference<>() {});
        palTemplateMap = palTemplates.stream()
                .collect(Collectors.toMap(PALTemplate::getTemplateName, template -> template));
        when(palDao.findALLPALTemplate()).thenReturn(palTemplates);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void palExportDataTest() throws IOException {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductExport.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALExportRequest palExportRequest = (PALExportRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALExportRequest>() {});

		PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct", null),
				new TypeReference<PALProduct>() {});
		PALProduct supplierProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("supplierproduct", null),
				new TypeReference<PALProduct>() {});

		List<PALProduct> palProductList = new ArrayList<>();
		palProductList.add(palProduct);
		palProductList.add(supplierProduct);
		List<String> productIds = palProductList.stream().map(PALProduct::getId).collect(Collectors.toList());
		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		when(palDao.findPALProducts(eq(palProject.getId()), eq(productIds), eq(null))).thenReturn(palProductList);
		when(palDao.findPALProjectById(eq(palProject.getId()))).thenReturn(palProject);
		when(userDetailsService.validateUserDetails(eq(palExportRequest.getUserRole()),eq(null)))
				.thenReturn(TestUtility.getUserDetails(palExportRequest.getUserRole()));
		when(exportHelper.createExcelwithMacro(any(),any(),any(),any(),any())).thenReturn(new ByteArrayResource("test".getBytes()));
		PALExportResponse actualResponse = palService.palExportData(palExportRequest);
		assertEquals(palProject.getProjectName()+".xlsm", actualResponse.getFileName());
		assertNotNull(actualResponse.getOut());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void palExportDataInvalidTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductExport.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALExportRequest palExportRequest = (PALExportRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALExportRequest>() {});
		palExportRequest.setUserRole(null);
		PALServiceException exception = assertThrows(PALServiceException.class,
				() -> palService.palExportData(palExportRequest));
		Assert.assertEquals(ErrorCode.INVALID_REQUEST_DATA.getErrorMessage(),exception.getErrorMessage());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void palExportDataNoDataTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductExport.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALExportRequest palExportRequest = (PALExportRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALExportRequest>() {});
		when(userDetailsService.validateUserDetails(Mockito.any(),Mockito.any()))
				.thenReturn(TestUtility.getUserDetails(palExportRequest.getUserRole()));
		PALServiceException exception = assertThrows(PALServiceException.class,
				() -> palService.palExportData(palExportRequest));
		Assert.assertEquals(ErrorCode.NO_DATA.getErrorMessage(),exception.getErrorMessage());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void palExportDataSupplierTest() throws IOException {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductExport.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALExportRequest palExportRequest = (PALExportRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALExportRequest>() {});
		palExportRequest.setUserRole(ApplicationConstant.SUPPLIER);
		PALProduct supplierProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("supplierproduct", null),
				new TypeReference<PALProduct>() {});
		PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct", null),
				new TypeReference<PALProduct>() {});

		List<PALProduct> palProductList = new ArrayList<>();
		palProductList.add(palProduct);
		palProductList.add(supplierProduct);

		List<String> productIds = palProductList.stream().map(PALProduct::getId).collect(Collectors.toList());
		UserDetails userDetails = TestUtility.getUserDetails(palExportRequest.getUserRole());
		List<String> suppliers = userDetails.getOrganizations();

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		when(palDao.findPALProducts(eq(palProject.getId()), eq(productIds), eq(suppliers))).thenReturn(palProductList);
		when(palDao.findPALProjectById(eq(palProject.getId()))).thenReturn(palProject);
		when(userDetailsService.validateUserDetails(eq(palExportRequest.getUserRole()),eq(null)))
				.thenReturn(userDetails);
		when(exportHelper.createExcel(any(),any(), any())).thenReturn(new ByteArrayResource("test".getBytes()));
		PALExportResponse actualResponse = palService.palExportData(palExportRequest);
		assertEquals(palProject.getProjectName()+".xlsx", actualResponse.getFileName());
		assertNotNull(actualResponse.getOut());
	}
}
