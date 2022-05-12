package com.marksandspencer.foodshub.pal.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marksandspencer.assemblyservice.config.transfer.AccessControlInfo;
import com.marksandspencer.foodshub.pal.constant.ApplicationConstant;
import com.marksandspencer.foodshub.pal.constant.ErrorCode;
import com.marksandspencer.foodshub.pal.constant.MessageTemplate;
import com.marksandspencer.foodshub.pal.constant.Status;
import com.marksandspencer.foodshub.pal.dao.ProductAttributeListingDao;
import com.marksandspencer.foodshub.pal.domain.*;
import com.marksandspencer.foodshub.pal.dto.Category;
import com.marksandspencer.foodshub.pal.exception.PALServiceException;
import com.marksandspencer.foodshub.pal.rest.client.impl.ESProductHierarchyServiceRestClientImpl;
import com.marksandspencer.foodshub.pal.serviceImpl.ProductAttributeListingServiceImpl;
import com.marksandspencer.foodshub.pal.transfer.*;
import com.marksandspencer.foodshub.pal.util.CommonUtility;
import com.marksandspencer.foodshub.pal.util.Util;
import com.marksandspencer.foodshub.pal.utility.TestUtility;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings({ "unchecked", "unused" })
public class ProductAttributeListingServiceTest {

	@InjectMocks
	ProductAttributeListingService  palService = new ProductAttributeListingServiceImpl();

	@Mock
	ProductAttributeListingDao palDao;

	@Mock
	UserDetailsService userDetailsService;

	@Mock
	NotificationService notificationService;

	@Mock
	ESProductHierarchyServiceRestClientImpl esProductHierarchyServiceRestClient;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Captor
	ArgumentCaptor<List<Auditlog>> auditLogsCaptor;
	
	@Captor
	ArgumentCaptor<ProjectFilter> projectFilterCaptor;

	@Captor
	ArgumentCaptor<List<PALProduct>> productsCaptor;

	@Captor
	ArgumentCaptor<Map<String, List<DataField>>> upsertProductCaptor;

	@Captor
	ArgumentCaptor<PALConfiguration> palConfigurationCaptor;

	@Captor
	ArgumentCaptor<List<String>> palaccessibleRolesCaptor;

	@Captor
	ArgumentCaptor<PALProject> palProjectCaptor;

	Map<String, PALTemplate> palTemplateMap;
	List<PALTemplate> palTemplates;
	List<PALConfiguration> palConfigs;

	@Before
	public void beforeTest() throws IOException {
		ReflectionTestUtils.setField(palService, "productUrl", "https://localhost/home/products/new-product-development/%s/product/%s");
		ReflectionTestUtils.setField(palService, "configIds", Arrays.asList(ApplicationConstant.CATEGORY_CONFIG_ID, "INVALID"));

		ObjectMapper mapper = new ObjectMapper();
		AppResponse<List<PALRole>> response = mapper.readValue(new File("src/test/resources/ProductAttributeListingResponse/ListRolesResponse.json"),
				new TypeReference<>() {});
		when(palDao.findAllPALRoles()).thenReturn(response.getData());

		List<PALFields> palFields = mapper.readValue(new File("src/test/resources/ProductAttributeListingResponse/db_PALFields.json"),
				new TypeReference<>() {});
		when(palDao.findAllPALFields()).thenReturn(palFields);

		palConfigs = mapper.readValue(new File("src/test/resources/ProductAttributeListingResponse/db_PALConfiguration.json"),
				new TypeReference<>() {});
		when(palDao.findALLPALConfiguration()).thenReturn(palConfigs);

		palTemplates = mapper.readValue(new File("src/test/resources/ProductAttributeListingResponse/db_PALTemplates.json"),
				new TypeReference<>() {});
		palTemplateMap = palTemplates.stream()
				.collect(Collectors.toMap(PALTemplate::getTemplateName, template -> template));
		when(palDao.findALLPALTemplate()).thenReturn(palTemplates);

		ReflectionTestUtils.setField(palService, "kafkaEnabled", true);
	}

	@Test
	public void getPalProductInformationTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductInformationTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);


		PALProductRequest request = (PALProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProductRequest>() {});

		PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct", null),
				new TypeReference<PALProduct>() {});

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		when(palDao.findPALProductById(Mockito.anyString())).thenReturn(palProduct);
		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(userDetailsService.getAccessControlDetails(eq(request.getUserRole())))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()).getUserRole().getAccessControlInfoList().get(0));
		PALProductResponse actualResponse = palService.getPALProductInformation(request);
		assertNotNull(actualResponse);
		validatePALProductResponse(request, "Standard", palProduct, actualResponse);
	}
	
	@Test
	public void getPalProductInformation_WithoutMultiFields_Test() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductInformationTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);


		PALProductRequest request = (PALProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProductRequest>() {});

		PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct1", null),
				new TypeReference<PALProduct>() {});

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		when(palDao.findPALProductById(Mockito.anyString())).thenReturn(palProduct);
		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(userDetailsService.getAccessControlDetails(eq(request.getUserRole())))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()).getUserRole().getAccessControlInfoList().get(0));
		PALProductResponse actualResponse = palService.getPALProductInformation(request);
		assertNotNull(actualResponse);
		validatePALProductResponse(request, "Standard", palProduct, actualResponse);
	}
	
	@Test
	public void getPalProductInformationTest_NoData_PAlProjectEmpty() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.NO_DATA.getErrorMessage());
		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductInformationTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);


		PALProductRequest request = (PALProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProductRequest>() {});

		PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct", null),
				new TypeReference<PALProduct>() {});

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		when(palDao.findPALProductById(Mockito.anyString())).thenReturn(palProduct);
		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(null);
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		palService.getPALProductInformation(request);
	}

	private void validatePALProductResponse(PALProductRequest request, String palTemplateId, PALProduct palProduct, PALProductResponse actualResponse) {
		PALTemplate palTemplate = palTemplateMap.getOrDefault(palTemplateId, null);
		String requestedSection = request.getSectionName();
		String requestedOnwerField = request.getRole();
		String userRole = request.getUserRole();
		assertNotNull(palTemplate);
		assertNotNull(actualResponse);

		assertEquals(request.getProductId(), actualResponse.getId());
		assertEquals(palProduct.getPersonnel(), actualResponse.getPersonnel());
		assertEquals(palProduct.getTemplateId(), actualResponse.getTemplateId());
		assertEquals(palProduct.getId(), actualResponse.getHeader().getProductId());
		assertEquals(palTemplate.getId(), actualResponse.getTemplateId());

		List<ProductSection> responseSections = actualResponse.getSections();
		assertNotEquals(0, responseSections.size());

		List<ProductField> responseFields = responseSections.stream().flatMap(section -> section.getFields().stream()).collect(Collectors.toList());
		List<String> responseFieldIds = responseFields.stream().map(ProductField::getName).collect(Collectors.toList());
		List<ProductSubSection> responseChildSubSections = responseFields.stream()
				.filter(field -> field.getName().equals(ApplicationConstant.PRODUCT_FILE_TYPE_FIELD)
						&& ApplicationConstant.PARENT.equalsIgnoreCase(field.getValue())
						&& !Objects.isNull(field.getSubSections()))
				.flatMap(field -> field.getSubSections().stream())
				.collect(Collectors.toList());

		List<ProductSubSection> responseMultipleArtworkSubSections = responseFields.stream()
				.filter(field -> ApplicationConstant.PRINTED_PACKAGING_TYPE.equalsIgnoreCase(field.getName())
						&& ApplicationConstant.MULTIPLE.equalsIgnoreCase(field.getValue())
						&& !Objects.isNull(field.getSubSections()))
				.flatMap(field -> field.getSubSections().stream())
				.collect(Collectors.toList());

		List<Section> templateSectionsAll = palTemplate.getSections();
		List<Section> templateSections = filterFieldsBySection(requestedSection, templateSectionsAll);
		List<PALFields> templateFieldsAll = templateSections.stream().flatMap(section -> section.getSectionFields().stream()).collect(Collectors.toList());
		List<PALFields> templateFields =filterReadbleFields(userRole, templateFieldsAll);
		templateFields = filterFieldsByOwner(requestedOnwerField, templateFields);

		List<String> templateFieldIds = templateFields.stream().map(PALFields::getId).collect(Collectors.toList());
		List<PALFields> templateChildFieldsAll = templateSections.stream()
				.filter(section -> section.getSectionName().equalsIgnoreCase(ApplicationConstant.PRODUCT_FILE_TYPE_FIELD))
				.flatMap(section -> section.getSubSections().stream()
						.flatMap(subSection -> subSection.getSubSectionFields().stream()))
				.collect(Collectors.toList());
		List<PALFields> templateChildFields = filterFieldsByOwner(requestedOnwerField, templateChildFieldsAll);

		List<PALFields> templateMultipleArtWorkFieldsAll = templateSections.stream()
				.filter(section -> section.getSectionName().equalsIgnoreCase(ApplicationConstant.DESIGN)).filter(section->!ObjectUtils.isEmpty(section.getSubSections()))
				.flatMap(section -> section.getSubSections().stream()
						.flatMap(subSection -> subSection.getSubSectionFields().stream()))
				.collect(Collectors.toList());
		List<PALFields> templateMultipleArtWorkFields = filterFieldsByOwner(requestedOnwerField, templateMultipleArtWorkFieldsAll);

		List<DataField> productFields = palProduct.getDatafields();
		List<MultiField> productChildSubSections = productFields.stream()
				.filter(field -> ApplicationConstant.PRODUCT_FILE_TYPE_FIELD.equalsIgnoreCase(field.getFieldId())
						&& ApplicationConstant.PARENT.equalsIgnoreCase(field.getFieldValue()))
				.filter(field -> !ObjectUtils.isEmpty(field.getMultifields())).flatMap(field -> field.getMultifields().stream())
				.collect(Collectors.toList());
		List<MultiField> productMultipleArtworkSubSections = productFields.stream()
				.filter(field -> ApplicationConstant.PRINTED_PACKAGING_TYPE.equalsIgnoreCase(field.getFieldId())
						&& ApplicationConstant.MULTIPLE.equalsIgnoreCase(field.getFieldValue()))
				.filter(field -> !ObjectUtils.isEmpty(field.getMultifields()))
				.flatMap(field -> field.getMultifields().stream())
				.collect(Collectors.toList());

		assertTrue(templateFieldIds.containsAll(responseFieldIds));

		templateFields.forEach(templateField -> {
			validateField(templateField, productFields, responseFields, userRole);

			if (!CollectionUtils.isEmpty(productChildSubSections)) {
				validateSubsectionFields(templateChildFields, productChildSubSections, responseChildSubSections, userRole);
			}

			if (!CollectionUtils.isEmpty(productMultipleArtworkSubSections)) {
				validateSubsectionFields(templateMultipleArtWorkFields, productMultipleArtworkSubSections, responseMultipleArtworkSubSections, userRole);
			}
		});
	}

	private List<PALFields> filterReadbleFields(String palUserRole, List<PALFields> palFields) {
		AccessControlInfo accessInfo = TestUtility.getUserDetails(palUserRole).getUserRole().getAccessControlInfoList().get(0);
		if (!ObjectUtils.isEmpty(accessInfo) && accessInfo.isReadAccess()) {
			if (ApplicationConstant.SUPPLIER.equalsIgnoreCase(palUserRole)) {
				return palFields.stream()
						.filter(field -> !CollectionUtils.isEmpty(field.getReadable()) && field.getReadable().contains(palUserRole))
						.collect(Collectors.toList());
			}
			return palFields;
		}
		return new ArrayList<>();
	}

	private List<PALFields> filterFieldsByOwner(String requestedUserRole, List<PALFields> templateFields) {

		if (requestedUserRole == null) {
			return templateFields;
		} else {
			List<PALFields> filteredFields = new ArrayList<>();
			templateFields.forEach(field -> {
				if (StringUtils.equalsIgnoreCase(field.getOwner(), requestedUserRole)) {
					filteredFields.add(field);
				}
			});

			return filteredFields;
		}
	}

	private List<Section> filterFieldsBySection(String requestedSection, List<Section> sections) {
		if (requestedSection == null) {
			return sections;
		} else {
			List<Section> filteredSections = new ArrayList<>();
			sections.forEach(section -> {
				if (StringUtils.equalsIgnoreCase(section.getSectionName(), requestedSection)) {
					filteredSections.add(section);
				}
			});
			return filteredSections;
		}
	}

	private void validateField(PALFields templateField, List<DataField> productFields,
							   List<ProductField> responseFields, String userRole) {
		AtomicBoolean skipSMPApprovedField = new AtomicBoolean(false);
		responseFields.forEach(fields->{
			if(fields.getName().equals(ApplicationConstant.PRINTER_TYPE)
					&& !fields.getValue().equals(ApplicationConstant.SMP)){
				skipSMPApprovedField.set(true);
			}
		});
		if(!(skipSMPApprovedField.get() && templateField.getId().equals(ApplicationConstant.SMP_APPROVED))){

			DataField dataField = productFields.stream()
					.filter(field -> templateField.getId().equals(field.getFieldId()))
					.findFirst().orElse(new DataField());

			ProductField productField = responseFields.stream()
					.filter(field -> templateField.getId().equals(field.getName()))
					.findFirst().orElse(new ProductField());
			boolean isDisabled = isEditableField(templateField, userRole);

			assertEquals(dataField.getFieldValue(), productField.getValue());
			assertEquals(templateField.getOwner(), productField.getOwner());
			assertEquals(templateField.getMinDate(), productField.getMinDate());
			assertEquals(templateField.getLabel(), productField.getLabel());
			assertEquals(templateField.getType(), productField.getType());
			assertEquals(templateField.isMandatory(), productField.isMandatory());
			assertEquals(isDisabled, productField.isDisabled());
			assertEquals(templateField.getErrorMessages(), productField.getErrorMessages());
			assertEquals(!Objects.isNull(templateField.getOptionsRef())?templateField.getOptionsRef().getValues():null, productField.getOptions());
			assertEquals(templateField.getPattern(), productField.getPattern());
		}
	}

	private boolean isEditableField(PALFields field, String userRole) {
		AccessControlInfo accessInfo = TestUtility.getUserDetails(userRole).getUserRole().getAccessControlInfoList().get(0);
		return ObjectUtils.isEmpty(accessInfo) || !accessInfo.isUpdateAccess() || Objects.isNull(field.getEditable()) ||
				(!field.getEditable().contains(userRole) &&
						(ApplicationConstant.SUPPLIER.equalsIgnoreCase(userRole) || !field.getEditable().contains(ApplicationConstant.MNS)));
	}

	private void validateSubsectionFields(List<PALFields> templateSubSectionFields, List<MultiField> productSubSections,
										  List<ProductSubSection> responseSubSections, String userRole) {
		productSubSections.forEach(productSubSection -> {
			List<DataField> productFields = productSubSection.getDatafields();
			List<ProductField> responseFields = responseSubSections.stream()
					.filter(subSection -> subSection.getSubSectionId().equalsIgnoreCase(productSubSection.getMultiFieldId()))
					.flatMap(subSection -> subSection.getSubfields().stream())
					.collect(Collectors.toList());
			templateSubSectionFields.forEach(templateSubField -> validateField(templateSubField, productFields, responseFields, userRole));
		});


	}

	@Test
	public void getPalProductInformationNoDataTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.NO_DATA.getErrorMessage());

		PALProductRequest request = new PALProductRequest();
		request.setProductId("00001111");
		request.setUserRole("buyer");
		when(palDao.findPALProductById(Mockito.anyString())).thenReturn(null);
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		palService.getPALProductInformation(request);
	}

	@Test
	public void getPalProductInformationInvalidRequestTest() {

		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_REQUEST_DATA.getErrorMessage());

		PALProductRequest request = new PALProductRequest();
		request.setProductId("");
		palService.getPALProductInformation(request);
	}

	@Test
	public void getPalProductBuyerRoleTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductInformationTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProductRequest request = (PALProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("buyerRequest", null),
				new TypeReference<PALProductRequest>() {});

		PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct", null),
				new TypeReference<PALProduct>() {});

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		when(palDao.findPALProductById(Mockito.anyString())).thenReturn(palProduct);
		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(userDetailsService.getAccessControlDetails(eq(request.getUserRole())))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()).getUserRole().getAccessControlInfoList().get(0));
		PALProductResponse actualResponse = palService.getPALProductInformation(request);
		assertNotNull(actualResponse);
		validatePALProductResponse(request, "Standard", palProduct, actualResponse);
	}
	
	@Test
	public void getPalProductSectionTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductInformationTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);


		PALProductRequest request = (PALProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("sectionRequest", null),
				new TypeReference<PALProductRequest>() {});

		PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct", null),
				new TypeReference<PALProduct>() {});

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		when(palDao.findPALProductById(Mockito.anyString())).thenReturn(palProduct);
		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(userDetailsService.validateUserDetails(request.getUserRole(), null))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(userDetailsService.getAccessControlDetails(any()))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()).getUserRole().getAccessControlInfoList().get(0));
		PALProductResponse actualResponse = palService.getPALProductInformation(request);
		assertNotNull(actualResponse);
		validatePALProductResponse(request, "Standard", palProduct, actualResponse);
	}

	@Test
	public void getPalProductPersonnelTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductPersonnelTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProductRequest request = (PALProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProductRequest>() {});

		PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct", null),
				new TypeReference<PALProduct>() {});

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		when(palDao.findPALProductById(Mockito.anyString())).thenReturn(palProduct);
		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));

		PALProductResponse actualResponse = palService.getPALProductPersonnel(request);
		assertNotNull(actualResponse);
		assertEquals(request.getProductId(), actualResponse.getId());
		assertEquals(palProduct.getPersonnel(), actualResponse.getPersonnel());
	}

	@Test
	public void getPalProductPersonnelNullTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductPersonnelTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProductRequest request = (PALProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("nullPersonnelRequest", null),
				new TypeReference<PALProductRequest>() {});

		PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("nullPersonnelProduct", null),
				new TypeReference<PALProduct>() {});

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		when(palDao.findPALProductById(Mockito.anyString())).thenReturn(palProduct);
		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));

		PALProductResponse actualResponse = palService.getPALProductPersonnel(request);
		assertNotNull(actualResponse);
		assertEquals(request.getProductId(), actualResponse.getId());
		assertNull(actualResponse.getPersonnel());
	}

	@Test
	public void getPalProductPersonnelNoDataTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.NO_DATA.getErrorMessage());

		PALProductRequest request = new PALProductRequest();
		request.setProductId("00001111");
		request.setUserRole("buyer");
		when(palDao.findPALProductById(Mockito.anyString())).thenReturn(null);
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		palService.getPALProductPersonnel(request);
	}

	@Test
	public void getPalProductPersonnelInvalidRequestTest() {

		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_REQUEST_DATA.getErrorMessage());

		PALProductRequest request = new PALProductRequest();
		request.setProductId("");

		palService.getPALProductPersonnel(request);
	}

	@Test
	public void updatePalProductPersonnelInvalidRequestTest() {

		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_REQUEST_DATA.getErrorMessage());

		PALProductUpdateRequest request = new PALProductUpdateRequest();
		request.setProductId("");

		palService.updatePALProductPersonnel(request);
	}

	@Test
	public void updatePalProductPersonnelNoDataTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.NO_DATA.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductPersonnelUpdateTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProductUpdateRequest request = (PALProductUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProductUpdateRequest>() {});

		when(palDao.findPALProductById(Mockito.anyString())).thenReturn(null);
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));

		palService.updatePALProductPersonnel(request);
	}

	@Test
	public void updatePalProductPersonnelTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductPersonnelUpdateTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProductUpdateRequest request = (PALProductUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProductUpdateRequest>() {});

		PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct", null),
				new TypeReference<PALProduct>() {});

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		when(palDao.findPALProductById(Mockito.anyString())).thenReturn(palProduct);
		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));

		PALProductResponse actualResponse = palService.updatePALProductPersonnel(request);
		assertNotNull(actualResponse);
		assertEquals(palProduct.getPersonnel(), actualResponse.getPersonnel());
		assertEquals(request.getProductId(), actualResponse.getId());
	}

	private void validatePALProductUpdateResponse(PALProductUpdateRequest request, String palTemplateId, PALProduct palProduct, PALProductResponse actualResponse) {
		PALTemplate palTemplate = palTemplateMap.getOrDefault(palTemplateId, null);
		if (!Objects.isNull(actualResponse) && !Objects.isNull(palTemplate)) {
			assertEquals(request.getProductId(), actualResponse.getId());
			assertEquals(palProduct.getPersonnel(), actualResponse.getPersonnel());
			assertEquals(palProduct.getTemplateId(), actualResponse.getTemplateId());
			assertEquals(palProduct.getId(), actualResponse.getHeader().getProductId());
			assertEquals(palTemplate.getId(),actualResponse.getTemplateId());

			List<ProductSection> responseSections = actualResponse.getSections();

			if (!CollectionUtils.isEmpty(responseSections)) {

				List<FieldUpdate> requestDataFields = request.getFieldUpdates();
				List<String> requestDataUpdateFieldIds = requestDataFields.stream().map(FieldUpdate::getField).collect(Collectors.toList());
				List<FieldUpdate> requestChildSubSections = requestDataFields.stream()
						.filter(field -> ApplicationConstant.PRODUCT_FILE_TYPE_FIELD.equalsIgnoreCase(field.getField())
								&& (!StringUtils.isEmpty(field.getSubSectionId()) || !CollectionUtils.isEmpty(field.getSubSectionFields())))
						.collect(Collectors.toList());
				List<FieldUpdate> requestMultipleArtworkSubSections = requestDataFields.stream()
						.filter(field -> ApplicationConstant.PRINTED_PACKAGING_TYPE.equalsIgnoreCase(field.getField())
								&& (!StringUtils.isEmpty(field.getSubSectionId()) || !CollectionUtils.isEmpty(field.getSubSectionFields())))
						.collect(Collectors.toList());

				List<ProductField> responseFields = responseSections.stream().flatMap(section -> section.getFields().stream()
						.filter(field -> requestDataUpdateFieldIds.contains(field.getName()))).collect(Collectors.toList());
				List<String> responseFieldIds = responseFields.stream().map(ProductField::getName).collect(Collectors.toList());
				List<ProductSubSection> responseChildSubSections = responseFields.stream()
						.filter(field -> field.getName().equals(ApplicationConstant.PRODUCT_FILE_TYPE_FIELD)
								&& ApplicationConstant.PARENT.equalsIgnoreCase(field.getValue())
								&& !Objects.isNull(field.getSubSections()))
						.flatMap(field -> field.getSubSections().stream())
						.collect(Collectors.toList());
				List<ProductSubSection> responseMultipleArtworkSubSections = responseFields.stream()
						.filter(field -> ApplicationConstant.PRINTED_PACKAGING_TYPE.equalsIgnoreCase(field.getName())
								&& ApplicationConstant.MULTIPLE.equalsIgnoreCase(field.getValue())
								&& !Objects.isNull(field.getSubSections()))
						.flatMap(field -> field.getSubSections().stream())
						.collect(Collectors.toList());

				List<Section> templateSections = palTemplate.getSections();
				List<PALFields> templateFields = templateSections.stream().flatMap(section -> section.getSectionFields().stream()
						.filter(field -> requestDataUpdateFieldIds.contains(field.getId()))).collect(Collectors.toList());

				List<String> templateFieldIds = templateFields.stream().map(PALFields::getId).collect(Collectors.toList());
				List<PALFields> templateChildFields = templateSections.stream()
						.filter(section -> section.getSectionName().equalsIgnoreCase(ApplicationConstant.PRODUCT_FILE_TYPE_FIELD))
						.flatMap(section -> section.getSubSections().stream()
								.flatMap(subSection -> subSection.getSubSectionFields().stream()))
						.collect(Collectors.toList());

				List<PALFields> templateMultipleArtWorkFields = templateSections.stream()
						.filter(section -> section.getSectionName().equalsIgnoreCase(ApplicationConstant.DESIGN))
						.flatMap(section -> section.getSubSections().stream()
								.flatMap(subSection -> subSection.getSubSectionFields().stream()))
						.collect(Collectors.toList());

				assertTrue(templateFieldIds.containsAll(responseFieldIds));

				List<DataField> productFields = palProduct.getDatafields();
				List<MultiField> productChildSubSections = productFields.stream()
						.filter(field -> !CollectionUtils.isEmpty(field.getMultifields())
								&& ApplicationConstant.PRODUCT_FILE_TYPE_FIELD.equalsIgnoreCase(field.getFieldId())
								&& ApplicationConstant.PARENT.equalsIgnoreCase(field.getFieldValue()))
						.flatMap(field -> field.getMultifields().stream())
						.collect(Collectors.toList());
				List<MultiField> productMultipleArtworkSubSections = productFields.stream()
						.filter(field -> !CollectionUtils.isEmpty(field.getMultifields())
								&& ApplicationConstant.PRINTED_PACKAGING_TYPE.equalsIgnoreCase(field.getFieldId())
								&& ApplicationConstant.MULTIPLE.equalsIgnoreCase(field.getFieldValue()))
						.flatMap(field -> field.getMultifields().stream())
						.collect(Collectors.toList());

				templateFields.forEach(templateField -> {
					validateFieldUpdate(templateField, requestDataFields, responseFields);

					if (!CollectionUtils.isEmpty(productChildSubSections)) {
						validateFieldSubUpdate(templateChildFields, productChildSubSections, requestChildSubSections, responseChildSubSections);
					}

					if (!CollectionUtils.isEmpty(productMultipleArtworkSubSections)) {
						validateFieldSubUpdate(templateMultipleArtWorkFields, productMultipleArtworkSubSections, requestMultipleArtworkSubSections, responseMultipleArtworkSubSections);
					}
				});
			}
		}
	}

	private void validateFieldSubUpdate(List<PALFields> templateSubFields, List<MultiField> productSubSections, List<FieldUpdate> requestSubSections, List<ProductSubSection> responseSubSections) {
		for (FieldUpdate requestUpdate : requestSubSections) {
			List<DataField> dataSubFields = new ArrayList<>();
			List<ProductField> productSubFields;
			List<FieldUpdate> requestSubFields = requestUpdate.getSubSectionFields();
			String subSectionId = requestUpdate.getSubSectionId();
			if (!StringUtils.isEmpty(requestUpdate.getSubSectionId()) && !CollectionUtils.isEmpty(requestSubFields)) {
				//update subfields
				MultiField multiField = productSubSections.stream().filter(subSection -> subSection.getMultiFieldId().equalsIgnoreCase(subSectionId)).findFirst().orElse(null);
				ProductSubSection productSubSection = responseSubSections.stream().filter(subSection -> subSection.getSubSectionId().equalsIgnoreCase(subSectionId)).findFirst().orElse(null);
				assertFalse(ObjectUtils.isEmpty(productSubSection));
				assertFalse(ObjectUtils.isEmpty(productSubSection.getSubfields()));

				assert multiField != null;
				productSubFields = productSubSection.getSubfields();

				// validate fields
				for (PALFields palField : templateSubFields){
					validateFieldUpdate(palField, requestSubFields, productSubFields);
				}
			} else if (!StringUtils.isEmpty(requestUpdate.getSubSectionId()) && CollectionUtils.isEmpty(requestSubFields)) {
				//delete all subfields
				ProductSubSection productSubSection = responseSubSections.stream().filter(subSection -> subSection.getSubSectionId().equalsIgnoreCase(subSectionId)).findFirst().orElse(null);
				assertTrue(ObjectUtils.isEmpty(productSubSection));
			} else if (!CollectionUtils.isEmpty(requestSubFields) && StringUtils.isEmpty(subSectionId)) {
				// create subsection fields
				MultiField multiField = productSubSections.stream().filter(subSection -> subSection.getMultiFieldId().equalsIgnoreCase(subSectionId)).findFirst().orElse(null);
				List<String> dbSubSectionIds = productSubSections.stream().map(MultiField::getMultiFieldId).collect(Collectors.toList());
				List<String> responseSubSectionIds = responseSubSections.stream().map(ProductSubSection::getSubSectionId).collect(Collectors.toList());
				assertEquals(dbSubSectionIds.size(), responseSubSectionIds.size());
				String newSubsectionId = responseSubSectionIds.get(responseSubSectionIds.size()-1);
				ProductSubSection productSubSection = responseSubSections.stream().filter(subSection -> subSection.getSubSectionId().equalsIgnoreCase(newSubsectionId)).findFirst().orElse(null);
				assertTrue(ObjectUtils.isEmpty(multiField));
				assertFalse(ObjectUtils.isEmpty(productSubSection));
				assertFalse(ObjectUtils.isEmpty(productSubSection.getSubfields()));

				for (PALFields palField : templateSubFields){
					validateFieldUpdate(palField, requestSubFields, productSubSection.getSubfields());
				}
			}
		}
	}

	private void validateFieldUpdate(PALFields templateField, List<FieldUpdate> requestDataFields, List<ProductField> responseFields) {
		requestDataFields.forEach(requestUpdate -> {
			if (!StringUtils.equalsIgnoreCase(requestUpdate.getOldValue(), requestUpdate.getNewValue()) &&
			    templateField.getId().equalsIgnoreCase(requestUpdate.getField())) {
				String fieldId = requestUpdate.getField();

				ProductField productField = responseFields.stream().filter(field -> field.getName().equalsIgnoreCase(fieldId)).findFirst().orElse(null);
				assertNotNull(productField);

				assertTrue(StringUtils.equalsIgnoreCase(requestUpdate.getNewValue(), productField.getValue()));
			}
		} );
	}

	@Test
	public void getPalProductProgressTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductProgressTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);


		PALProductRequest request = (PALProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProductRequest>() {});

		PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct", null),
				new TypeReference<PALProduct>() {});

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		AppResponse<PALProductResponse> appresponse =  (AppResponse<PALProductResponse>) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("response", null),
				new TypeReference<AppResponse<PALProductResponse>>() {});
		PALProductResponse expectedResponse = appresponse.getData();

		when(palDao.findPALProductById(Mockito.anyString())).thenReturn(palProduct);
		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(userDetailsService.getAccessControlDetails(eq(request.getUserRole())))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()).getUserRole().getAccessControlInfoList().get(0));
		PALProductResponse actualResponse = palService.getPALProductProgress(request);
		assertNotNull(actualResponse);
		assertEquals(request.getProductId(), actualResponse.getId());
		assertEquals(expectedResponse.getHeader(), actualResponse.getHeader());
		assertEquals(expectedResponse.getProgress(), actualResponse.getProgress());
	}

	@Test
	public void updatePalProductInformationInvalidRequestTest() {

		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_REQUEST_DATA.getErrorMessage());

		PALProductUpdateRequest request = new PALProductUpdateRequest();
		request.setProductId("");

		palService.updatePALProductInformation(request);
	}

	@Test
	public void updatePalProductInformationNoDataTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.NO_DATA.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductInformationUpdateTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProductUpdateRequest request = (PALProductUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProductUpdateRequest>() {});

		when(palDao.findPALProductById(Mockito.anyString())).thenReturn(null);
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));

		palService.updatePALProductInformation(request);
	}

	@Test
	public void getPalProductAuditTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductAuditTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProductRequest request = (PALProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProductRequest>() {});

		PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct", null),
				new TypeReference<PALProduct>() {});

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		PALAuditLog palAuditLog = (PALAuditLog) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palAudit", null),
				new TypeReference<PALAuditLog>() {});
		AppResponse<PALProductResponse> appresponse =  (AppResponse<PALProductResponse>) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("response", null),
				new TypeReference<AppResponse<PALProductResponse>>() {});
		PALProductResponse expectedResponse = appresponse.getData();

		when(palDao.findPALProductById(Mockito.anyString())).thenReturn(palProduct);
		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(palDao.findAuditLogs(Mockito.anyString())).thenReturn(palAuditLog);
		when(userDetailsService.validateUserDetails(any(), any()))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(userDetailsService.getAccessControlDetails(any()))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()).getUserRole().getAccessControlInfoList().get(0));
		PALProductResponse actualResponse = palService.getPALProductAuditlogs(request);
		assertFalse(ObjectUtils.isEmpty(actualResponse.getAuditlogs()));
		assertEquals(expectedResponse.getHeader(), actualResponse.getHeader());
		assertEquals(expectedResponse.getAuditlogs(), actualResponse.getAuditlogs());
	}

	@Test
	public void getPalProductAuditWithDateFilterTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductAuditTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);


		PALProductRequest request = (PALProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("dateFilterRequest", null),
				new TypeReference<PALProductRequest>() {});

		PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct", null),
				new TypeReference<PALProduct>() {});

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		PALAuditLog palAuditLog = (PALAuditLog) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palAudit", null),
				new TypeReference<PALAuditLog>() {});

		AppResponse<PALProductResponse> appresponse =  (AppResponse<PALProductResponse>) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("dateFilterResponse", null),
				new TypeReference<AppResponse<PALProductResponse>>() {});
		PALProductResponse expectedResponse = appresponse.getData();

		when(palDao.findPALProductById(Mockito.anyString())).thenReturn(palProduct);
		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(palDao.findAuditLogs(Mockito.anyString())).thenReturn(palAuditLog);
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(userDetailsService.getAccessControlDetails(eq(request.getUserRole())))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()).getUserRole().getAccessControlInfoList().get(0));
		PALProductResponse actualResponse = palService.getPALProductAuditlogs(request);
		assertFalse(ObjectUtils.isEmpty(actualResponse.getAuditlogs()));
		assertEquals(expectedResponse.getHeader(), actualResponse.getHeader());
		assertEquals(expectedResponse.getAuditlogs(), actualResponse.getAuditlogs());
	}

	@Test
	public void getPalProductAuditWithTextFilterTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductAuditTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);


		PALProductRequest request = (PALProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("textFilterRequest", null),
				new TypeReference<PALProductRequest>() {});

		PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct", null),
				new TypeReference<PALProduct>() {});

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		PALAuditLog palAuditLog = (PALAuditLog) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palAudit", null),
				new TypeReference<PALAuditLog>() {});

		AppResponse<PALProductResponse> appresponse =  (AppResponse<PALProductResponse>) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("textFilterResponse", null),
				new TypeReference<AppResponse<PALProductResponse>>() {});
		PALProductResponse expectedResponse = appresponse.getData();

		when(palDao.findPALProductById(Mockito.anyString())).thenReturn(palProduct);
		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(palDao.findAuditLogs(Mockito.anyString())).thenReturn(palAuditLog);
		when(userDetailsService.validateUserDetails(any(), any()))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(userDetailsService.getAccessControlDetails(any()))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()).getUserRole().getAccessControlInfoList().get(0));
		PALProductResponse actualResponse = palService.getPALProductAuditlogs(request);
		assertEquals(expectedResponse, actualResponse);
	}

	@Test
	public void getPalProductAuditWithDateAndTextFilterTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductAuditTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);


		PALProductRequest request = (PALProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("dateAndTextFilterRequest", null),
				new TypeReference<PALProductRequest>() {});

		PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct", null),
				new TypeReference<PALProduct>() {});

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		PALAuditLog palAuditLog = (PALAuditLog) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palAudit", null),
				new TypeReference<PALAuditLog>() {});

		AppResponse<PALProductResponse> appresponse =  (AppResponse<PALProductResponse>) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("dateAndTextFilterResponse", null),
				new TypeReference<AppResponse<PALProductResponse>>() {});
		PALProductResponse expectedResponse = appresponse.getData();

		when(palDao.findPALProductById(Mockito.anyString())).thenReturn(palProduct);
		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(palDao.findAuditLogs(Mockito.anyString())).thenReturn(palAuditLog);
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(userDetailsService.getAccessControlDetails(eq(request.getUserRole())))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()).getUserRole().getAccessControlInfoList().get(0));
		PALProductResponse actualResponse = palService.getPALProductAuditlogs(request);
		assertEquals(expectedResponse, actualResponse);
	}

	@Test
	public void updatePalProductInformationTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductInformationUpdateTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProductUpdateRequest request = (PALProductUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProductUpdateRequest>() {});

		PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct", null),
				new TypeReference<PALProduct>() {});
		for (DataField datafield : palProduct.getDatafields()) {
			if (datafield.getFieldId().equalsIgnoreCase(ApplicationConstant.PRODUCT_TITLE_FIELD))
				datafield.setFieldValue("Sausage");
		}
		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		List<Map<String, Object>> auditLogs = (List<Map<String, Object>>) paldetails.getOrDefault("auditLogList", null);

		when(palDao.findPALProductById(Mockito.anyString())).thenReturn(palProduct);
		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));

		PALProductResponse actualResponse = palService.updatePALProductInformation(request);
		verify(palDao,Mockito.times(1)).updatePALAuditLog(Mockito.anyString(), auditLogsCaptor.capture());
		List<Auditlog> actualAuditLogs = auditLogsCaptor.getValue();
		assertEquals(auditLogs.size(), actualAuditLogs.size());
		assertEquals(auditLogs.get(0).getOrDefault("auditField", null), actualAuditLogs.get(0).getAuditField());
		assertEquals(auditLogs.get(0).getOrDefault("auditFieldLabel", null), actualAuditLogs.get(0).getAuditFieldLabel());
		assertEquals(auditLogs.get(0).getOrDefault("oldValue", null), actualAuditLogs.get(0).getOldValue());
		assertEquals(auditLogs.get(0).getOrDefault("newValue", null), actualAuditLogs.get(0).getNewValue());
		assertEquals(auditLogs.get(0).getOrDefault("user", null), actualAuditLogs.get(0).getUser());
		assertEquals(auditLogs.get(0).getOrDefault("userName", null), actualAuditLogs.get(0).getUserName());
		assertEquals(auditLogs.get(0).getOrDefault("userRole", null), actualAuditLogs.get(0).getUserRole());
		assertEquals(auditLogs.get(0).getOrDefault("userRoleLabel", null), actualAuditLogs.get(0).getUserRoleLabel());
		assertEquals(auditLogs.get(1).getOrDefault("auditField", null), actualAuditLogs.get(1).getAuditField());
		assertEquals(auditLogs.get(1).getOrDefault("auditFieldLabel", null), actualAuditLogs.get(1).getAuditFieldLabel());
		assertEquals(auditLogs.get(1).getOrDefault("oldValue", null), actualAuditLogs.get(1).getOldValue());
		assertEquals(auditLogs.get(1).getOrDefault("newValue", null), actualAuditLogs.get(1).getNewValue());
		assertEquals(auditLogs.get(1).getOrDefault("user", null), actualAuditLogs.get(1).getUser());
		assertEquals(auditLogs.get(1).getOrDefault("userName", null), actualAuditLogs.get(1).getUserName());
		assertEquals(auditLogs.get(1).getOrDefault("userRole", null), actualAuditLogs.get(1).getUserRole());
		assertEquals(auditLogs.get(1).getOrDefault("userRoleLabel", null), actualAuditLogs.get(1).getUserRoleLabel());
		assertNotNull(actualResponse);
		validatePALProductUpdateResponse(request, "Standard", palProduct, actualResponse);
	}

	@Test
	public void updatePalProductInformationWithNoChangesTest() {

		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.NO_CHANGES.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductInformationUpdateTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProductUpdateRequest request = (PALProductUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("requestWithNoChange", null),
				new TypeReference<PALProductUpdateRequest>() {});
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		palService.updatePALProductInformation(request);
	}

	@Test
	public void getPalProductAuditWithNoChangesTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductAuditTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProductRequest request = (PALProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProductRequest>() {});

		PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct", null),
				new TypeReference<PALProduct>() {});

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		AppResponse<PALProductResponse> appresponse =  (AppResponse<PALProductResponse>) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("nullAuditResponse", null),
				new TypeReference<AppResponse<PALProductResponse>>() {});
		PALProductResponse expectedResponse = appresponse.getData();

		when(palDao.findPALProductById(Mockito.anyString())).thenReturn(palProduct);
		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(palDao.findAuditLogs(Mockito.anyString())).thenReturn(null);
		when(userDetailsService.validateUserDetails(any(), any()))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(userDetailsService.getAccessControlDetails(any()))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()).getUserRole().getAccessControlInfoList().get(0));
		PALProductResponse actualResponse = palService.getPALProductAuditlogs(request);
		assertEquals(expectedResponse, actualResponse);
	}

	@Test
	public void listRoleTest() {
		List<PALRole> palRole = palService.listRoles();
		assertEquals(15, palRole.size());
		assertEquals("Project Manager", palRole.get(0).getName());
		assertEquals("1", palRole.get(0).getId());
	}

	@Test
	public void getPalFieldsTest() {
		List<PALFields> palFields = palService.getPalFields();
		assertNotEquals(0,palFields.size());
	}

	@Test
	public void getPalProjectProductListTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductListTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);


		PALProjectRequest request = (PALProjectRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProjectRequest>() {});

		PALProduct palProduct1 = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct1", null),
				new TypeReference<PALProduct>() {});
		PALProduct palProduct2 = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct2", null),
				new TypeReference<PALProduct>() {});
		List<PALProduct> palProducts = new ArrayList<>();
		palProducts.add(palProduct1);
		palProducts.add(palProduct2);

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		AppResponse<PALProjectResponse> appresponse =  (AppResponse<PALProjectResponse>) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("response", null),
				new TypeReference<AppResponse<PALProjectResponse>>() {});
		PALProjectResponse expectedResponse = appresponse.getData();
		
		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(palDao.findProductByFilterCondition(Mockito.any(),Mockito.any())).thenReturn(palProducts);
		when(userDetailsService.validateUserDetails(Mockito.any(),Mockito.any()))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(userDetailsService.getAccessControlDetails(any()))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()).getUserRole().getAccessControlInfoList().get(0));
		PALProjectResponse actualResponse = palService.getPALProductList(request);
		assertEquals(2, actualResponse.getProducts().size());
		assertEquals(2, actualResponse.getProducts().get(1).getChildren().size());
		assertNull(actualResponse.getProducts().get(0).getChildren());
		assertEquals(expectedResponse, actualResponse);
	}
	
	@Test
	public void getPalProjectProductFilterListTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductListTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);
		
				
		PALProjectRequest request = (PALProjectRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProjectRequest>() {});
		List<String> status=new ArrayList<>();
		status.add("Inprogress");
		Filter filter = Filter.builder().status(status).build();
		request.setFilter(filter);
		PALProduct palProduct1 = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct1", null),
				new TypeReference<PALProduct>() {});
		PALProduct palProduct2 = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct2", null),
				new TypeReference<PALProduct>() {});
		List<PALProduct> palProducts = new ArrayList<>();
		palProducts.add(palProduct1);
		palProducts.add(palProduct2);
		
		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});
		
		AppResponse<PALProjectResponse> appresponse =  (AppResponse<PALProjectResponse>) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("response", null),
				new TypeReference<AppResponse<PALProjectResponse>>() {});
		PALProjectResponse expectedResponse = appresponse.getData();
		
		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(palDao.findProductByFilterCondition(Mockito.any(),Mockito.any())).thenReturn(palProducts);
		when(userDetailsService.validateUserDetails(Mockito.any(),Mockito.any()))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(userDetailsService.getAccessControlDetails(any()))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()).getUserRole().getAccessControlInfoList().get(0));
		PALProjectResponse actualResponse = palService.getPALProductList(request);
		assertEquals(2, actualResponse.getProducts().size());
		assertEquals(2, actualResponse.getProducts().get(1).getChildren().size());
		assertNull(actualResponse.getProducts().get(0).getChildren());
		assertEquals(expectedResponse, actualResponse);
	}

	@Test
	public void getPalProjectNullProductListTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductListTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProjectRequest request = (PALProjectRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProjectRequest>() {});

		List<PALProduct> palProducts = new ArrayList<>();
	
		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		AppResponse<PALProjectResponse> appresponse =  (AppResponse<PALProjectResponse>) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("response2", null),
				new TypeReference<AppResponse<PALProjectResponse>>() {});

		PALProjectResponse expectedResponse = appresponse.getData();
		
		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(palDao.findProductByFilterCondition(Mockito.any(),Mockito.any())).thenReturn(palProducts);
		when(userDetailsService.validateUserDetails(Mockito.any(),Mockito.any()))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		PALProjectResponse actualResponse = palService.getPALProductList(request);
		assertNull(actualResponse.getProducts());
		assertEquals(expectedResponse.getInformation(), actualResponse.getInformation());
	}

	@Test
	public void getPalProjectProductListNoTemplateTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductListTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProjectRequest request = (PALProjectRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProjectRequest>() {});

		PALProduct palProduct1 = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct1", null),
				new TypeReference<PALProduct>() {});
		PALProduct palProduct2 = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct2", null),
				new TypeReference<PALProduct>() {});

		List<PALProduct> palProducts = new ArrayList<>();
		palProducts.add(palProduct1);
		palProducts.add(palProduct2);

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(palDao.findALLPALTemplate()).thenReturn(new ArrayList<>());
		when(userDetailsService.validateUserDetails(Mockito.any(),Mockito.any()))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_TEMPLATE_ID.getErrorMessage());

		palService.getPALProductList(request);
	}

	@Test
	public void getPalProjectProductListNoProjectTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductListTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);


		PALProjectRequest request = (PALProjectRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProjectRequest>() {});

		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.NO_DATA.getErrorMessage());
		when(userDetailsService.validateUserDetails(Mockito.any(),Mockito.any()))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
        palService.getPALProductList(request);
     }

	@Test
	public void getPalProjectProgressTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProjectProgressTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProjectRequest request = (PALProjectRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProjectRequest>() {});

		PALProduct palProduct1 = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct1", null),
				new TypeReference<PALProduct>() {});
		PALProduct palProduct2 = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct2", null),
				new TypeReference<PALProduct>() {});
		List<PALProduct> palProducts = new ArrayList<>();
		palProducts.add(palProduct1);
		palProducts.add(palProduct2);

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		AppResponse<PALProjectResponse> appresponse =  (AppResponse<PALProjectResponse>) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("response", null),
				new TypeReference<AppResponse<PALProjectResponse>>() {});
		PALProjectResponse expectedResponse = appresponse.getData();

		when(palDao.findPALProductsByProjectId(Mockito.anyString())).thenReturn(palProducts);
		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(userDetailsService.getAccessControlDetails(eq(request.getUserRole())))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()).getUserRole().getAccessControlInfoList().get(0));
		PALProjectResponse actualResponse = palService.getPALProjectProgress(request);
		assertEquals(expectedResponse, actualResponse);
	}

	@Test
	public void getPalProjectProgressWithProductTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProjectProgressTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProjectRequest request = (PALProjectRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProjectRequest>() {});

		PALProduct palProduct1 = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct1", null),
				new TypeReference<PALProduct>() {});
		PALProduct palProduct2 = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct2", null),
				new TypeReference<PALProduct>() {});
		List<PALProduct> palProducts = new ArrayList<>();
		palProducts.add(palProduct1);
		palProducts.add(palProduct2);

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		AppResponse<PALProjectResponse> appresponse =  (AppResponse<PALProjectResponse>) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("responseWithNoProduct", null),
				new TypeReference<AppResponse<PALProjectResponse>>() {});
		PALProjectResponse expectedResponse = appresponse.getData();

		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(palDao.findPALProductsByProjectId(Mockito.anyString())).thenReturn(new ArrayList<>());
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));

		PALProjectResponse actualResponse = palService.getPALProjectProgress(request);
		assertEquals(expectedResponse, actualResponse);
	}

	@Test
	public void updatePalProductSubFieldInformationTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductInformationSubFieldUpdateTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProductUpdateRequest request = (PALProductUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request_updateExisting", null),
				new TypeReference<PALProductUpdateRequest>() {});

		PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct", null),
				new TypeReference<PALProduct>() {});

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		List<Map<String, Object>> auditLogs = (List<Map<String, Object>>) paldetails.getOrDefault("auditLogList", null);

		when(palDao.findPALProductById(Mockito.anyString())).thenReturn(palProduct);
		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));

		PALProductResponse actualResponse = palService.updatePALProductInformation(request);
		verify(palDao,Mockito.times(1)).updatePALAuditLog(Mockito.anyString(), auditLogsCaptor.capture());
		List<Auditlog> actualAuditLogs = auditLogsCaptor.getValue();
		assertEquals(auditLogs.size(), actualAuditLogs.size());
		assertEquals(auditLogs.get(0).getOrDefault("auditField", null), actualAuditLogs.get(0).getAuditField());
		assertEquals(auditLogs.get(0).getOrDefault("auditFieldLabel", null), actualAuditLogs.get(0).getAuditFieldLabel());
		assertEquals(auditLogs.get(0).getOrDefault("oldValue", null), actualAuditLogs.get(0).getOldValue());
		assertEquals(auditLogs.get(0).getOrDefault("newValue", null), actualAuditLogs.get(0).getNewValue());
		assertEquals(auditLogs.get(0).getOrDefault("user", null), actualAuditLogs.get(0).getUser());
		assertEquals(auditLogs.get(0).getOrDefault("userName", null), actualAuditLogs.get(0).getUserName());
		assertEquals(auditLogs.get(0).getOrDefault("userRole", null), actualAuditLogs.get(0).getUserRole());
		assertEquals(auditLogs.get(0).getOrDefault("userRoleLabel", null), actualAuditLogs.get(0).getUserRoleLabel());
		assertNotNull(actualResponse);
		validatePALProductUpdateResponse(request, "Standard", palProduct, actualResponse);
	}

	@Test
	public void updatePalProductAddNewChildInformationTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductInformationSubFieldUpdateTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProductUpdateRequest request = (PALProductUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request_addNewChild", null),
				new TypeReference<PALProductUpdateRequest>() {});

		PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct", null),
				new TypeReference<PALProduct>() {});

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		List<Map<String, Object>> auditLogs = (List<Map<String, Object>>) paldetails.getOrDefault("auditLogAddList", null);

		when(palDao.findPALProductById(Mockito.anyString())).thenReturn(palProduct);
		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));

		PALProductResponse actualResponse = palService.updatePALProductInformation(request);
		verify(palDao,Mockito.times(1)).updatePALAuditLog(Mockito.anyString(), auditLogsCaptor.capture());
		List<Auditlog> actualAuditLogs = auditLogsCaptor.getValue();
		assertEquals(auditLogs.size(), actualAuditLogs.size());
		assertEquals(auditLogs.get(0).getOrDefault("auditField", null), actualAuditLogs.get(0).getAuditField());
		assertEquals(auditLogs.get(0).getOrDefault("auditFieldLabel", null), actualAuditLogs.get(0).getAuditFieldLabel());
		assertEquals(auditLogs.get(0).getOrDefault("oldValue", null), actualAuditLogs.get(0).getOldValue());
		assertEquals(auditLogs.get(0).getOrDefault("newValue", null), actualAuditLogs.get(0).getNewValue());
		assertEquals(auditLogs.get(0).getOrDefault("user", null), actualAuditLogs.get(0).getUser());
		assertEquals(auditLogs.get(0).getOrDefault("userName", null), actualAuditLogs.get(0).getUserName());
		assertEquals(auditLogs.get(0).getOrDefault("userRole", null), actualAuditLogs.get(0).getUserRole());
		assertEquals(auditLogs.get(0).getOrDefault("userRoleLabel", null), actualAuditLogs.get(0).getUserRoleLabel());
		assertNotNull(actualResponse);
		validatePALProductUpdateResponse(request, "Standard", palProduct, actualResponse);
	}

	@Test
	public void deletePalProductChildInformationTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductInformationSubFieldUpdateTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProductUpdateRequest request = (PALProductUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request_deleteExisting", null),
				new TypeReference<PALProductUpdateRequest>() {});

		PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct", null),
				new TypeReference<PALProduct>() {});

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		when(palDao.findPALProductById(Mockito.anyString())).thenReturn(palProduct);
		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(userDetailsService.getAccessControlDetails(eq(request.getUserRole())))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()).getUserRole().getAccessControlInfoList().get(0));
		PALProductResponse actualResponse = palService.updatePALProductInformation(request);
		assertNotNull(actualResponse);
		validatePALProductUpdateResponse(request, "Standard", palProduct, actualResponse);
	}

	@Test
	public void addPALProjectTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProjectTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProjectUpdateRequest request = (PALProjectUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProjectUpdateRequest>() {});

		PALProject palProject =  (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palProject", null),
				new TypeReference<PALProject>() {});
		when(palDao.savePALProject(Mockito.any())).thenReturn(palProject);
		PALProjectResponse actualResponse = palService.addPALProject(request);
		assertNotNull(actualResponse.getId());
		verify(notificationService, times(1)).sendEmailMessage(any(),any(),eq(MessageTemplate.ADD_PROJECT));
	}

	@Test
	public void addPALProjectTestInvalidTemplate() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_TEMPLATE_ID.getErrorMessage());
		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProjectTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProjectUpdateRequest request = (PALProjectUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProjectUpdateRequest>() {});
		PALTemplate palTemplate = new PALTemplate();
		palTemplate.setId("611d11f7cde71e991b217579");
		palTemplate.setTemplateName("Invalid");
		when(palDao.findALLPALTemplate()).thenReturn(Collections.singletonList(palTemplate));
		palService.addPALProject(request);
	}
	
	@Test
	public void addPALProjectTestWithoutTemplate() {
		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProjectTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProjectUpdateRequest request = (PALProjectUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request2", null),
				new TypeReference<PALProjectUpdateRequest>() {});

		PALProject palProject =  (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palProject", null),
				new TypeReference<PALProject>() {});

		when(palDao.savePALProject(Mockito.any())).thenReturn(palProject);
		PALProjectResponse actualResponse = palService.addPALProject(request);
		assertNotNull(actualResponse.getId());
		assertNotNull(actualResponse.getInformation().getTemplateId());
	}

	@Test
	public void updatePalProductChildInformationTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductInformationSubFieldUpdateTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProductUpdateRequest request = (PALProductUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request_addFirstChild", null),
				new TypeReference<PALProductUpdateRequest>() {});

		PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproductAddFirstChild", null),
				new TypeReference<PALProduct>() {});

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		List<Map<String, Object>> auditLogs = (List<Map<String, Object>>) paldetails.getOrDefault("auditLogFirstList", null);

		when(palDao.findPALProductById(Mockito.anyString())).thenReturn(palProduct);
		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));

		PALProductResponse actualResponse = palService.updatePALProductInformation(request);
		verify(palDao,Mockito.times(1)).updatePALAuditLog(Mockito.anyString(), auditLogsCaptor.capture());
		List<Auditlog> actualAuditLogs = auditLogsCaptor.getValue();
		assertEquals(auditLogs.size(), actualAuditLogs.size());
		assertEquals(auditLogs.get(0).getOrDefault("auditField", null), actualAuditLogs.get(0).getAuditField());
		assertEquals(auditLogs.get(0).getOrDefault("auditFieldLabel", null), actualAuditLogs.get(0).getAuditFieldLabel());
		assertEquals(auditLogs.get(0).getOrDefault("oldValue", null), actualAuditLogs.get(0).getOldValue());
		assertEquals(auditLogs.get(0).getOrDefault("newValue", null), actualAuditLogs.get(0).getNewValue());
		assertEquals(auditLogs.get(0).getOrDefault("user", null), actualAuditLogs.get(0).getUser());
		assertEquals(auditLogs.get(0).getOrDefault("userName", null), actualAuditLogs.get(0).getUserName());
		assertEquals(auditLogs.get(0).getOrDefault("userRole", null), actualAuditLogs.get(0).getUserRole());
		assertEquals(auditLogs.get(0).getOrDefault("userRoleLabel", null), actualAuditLogs.get(0).getUserRoleLabel());
		assertNotNull(actualResponse);
		validatePALProductUpdateResponse(request, "Standard", palProduct, actualResponse);
	}

	@Test
	public void addPALProjectTestWithoutMandatory() {

		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.MISSING_MANDATORY_FIELDS.getErrorMessage());
		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProjectTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProjectUpdateRequest request = (PALProjectUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request3", null),
				new TypeReference<PALProjectUpdateRequest>() {});

		palService.addPALProject(request);
	}

	@Test
	public void addPALProjectTestWithoutStatus() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProjectTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProjectUpdateRequest request = (PALProjectUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request4", null),
				new TypeReference<PALProjectUpdateRequest>() {});

		PALProject palProject =  (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palProject", null),
				new TypeReference<PALProject>() {});
		palProject.setStatus(Status.DRAFT.getStatus());
		when(palDao.savePALProject(Mockito.any())).thenReturn(palProject);

		PALProjectResponse actualResponse = palService.addPALProject(request);

		assertEquals(request.getInformation().getProjectName(), actualResponse.getInformation().getProjectName());
		assertEquals(request.getPersonnel(), actualResponse.getPersonnel());
		assertEquals(request.getInformation().getProjectType(), actualResponse.getInformation().getProjectType());
		assertEquals(request.getInformation().getTemplateId(), actualResponse.getInformation().getTemplateId());
		assertEquals(request.getInformation().getTemplateName(), actualResponse.getInformation().getTemplateName());
		assertEquals(Status.DRAFT.getStatus(), actualResponse.getInformation().getStatus());
		assertEquals(request.getInformation().getComments(), actualResponse.getInformation().getComments());
		assertEquals(request.getInformation().getFinancialYear(), actualResponse.getInformation().getFinancialYear());
		assertEquals(request.getInformation().getProjectCompletionDate(), actualResponse.getInformation().getProjectCompletionDate());
	}

	@Test
	public void getPalProductProgressExceptionTestProductIdEmpty() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_REQUEST_DATA.getErrorMessage());
		PALProductRequest request = new PALProductRequest();
		request.setUserRole("projectManager");
		request.setProductId("");
		palService.getPALProductProgress(request);
	}

	@Test
	public void getPalProductProgressExceptionTestUserRoleEmpty() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_REQUEST_DATA.getErrorMessage());
		PALProductRequest request = new PALProductRequest();
		request.setUserRole("");
		request.setProductId("34");
		palService.getPALProductProgress(request);
	}
	
	@Test
	public void updatePALProjectTest() {
		
		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProjectTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);
				
		PALProjectUpdateRequest request = (PALProjectUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("updateRequest", null),
				new TypeReference<PALProjectUpdateRequest>() {});
		
		PALProject palProject =  (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palProjectArchived", null),
				new TypeReference<PALProject>() {});

		when(palDao.updatePALProject(Mockito.any())).thenReturn(palProject);
		PALProjectResponse actualResponse = palService.updatePALProject(request);
		assertEquals(Status.ARCHIVED.getStatus(), actualResponse.getInformation().getStatus());
	}
	
	@Test
	public void updatePALProjectTestWithNoAccess() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.UNAUTHORIIZED.getErrorMessage());
		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProjectTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);
				
		PALProjectUpdateRequest request = (PALProjectUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("updateRequestNoAccess", null),
				new TypeReference<PALProjectUpdateRequest>() {});
		when(userDetailsService.validateUserDetails(Mockito.any(),Mockito.any()))
				.thenThrow(new PALServiceException(ErrorCode.UNAUTHORIIZED));

		PALProjectResponse actualResponse = palService.updatePALProject(request);
	}
	
	@Test
	public void updatePALProjectTestWithNoId() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.MISSING_MANDATORY_FIELDS.getErrorMessage());
		
		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProjectTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);
				
		PALProjectUpdateRequest request = (PALProjectUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("updateRequestNoId", null),
				new TypeReference<PALProjectUpdateRequest>() {});

		PALProjectResponse actualResponse = palService.updatePALProject(request);
	}
	
	@Test
	public void updatePALProjectTestForInvalidStatus()  {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_REQUEST_DATA.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProjectTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);
				
		PALProjectUpdateRequest request = (PALProjectUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("updateRequestInvalidStatus", null),
				new TypeReference<PALProjectUpdateRequest>() {});
		
		palService.updatePALProject(request);

	}

	@Test
	public void updatePALProjectTestMissingRole() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.MISSING_MANDATORY_FIELDS.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProjectTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProjectUpdateRequest request = (PALProjectUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("updateRequestRoleMissing", null),
				new TypeReference<PALProjectUpdateRequest>() {});

		palService.updatePALProject(request);
	}

	@Test
	public void updatePALProjectTestWithAllFields() {
		
		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProjectTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);
				
		PALProjectUpdateRequest request = (PALProjectUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("updateRequest1", null),
				new TypeReference<PALProjectUpdateRequest>() {});

		PALProject palProject =  (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palProjectFrmDb", null),
				new TypeReference<PALProject>() {});
		
		when(palDao.updatePALProject(Mockito.any())).thenReturn(palProject);
		PALProjectResponse actualResponse = palService.updatePALProject(request);
		assertEquals("Post-Creative Gate", actualResponse.getInformation().getStatus() );
		assertEquals(palProject.getProjectName(), actualResponse.getInformation().getProjectName());
	}
	
	@Test
	public void updatePALProjectTestInvalidId() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_PROJECT_ID.getErrorMessage());
		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProjectTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);
				
		PALProjectUpdateRequest request = (PALProjectUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("updateRequest1", null),
				new TypeReference<PALProjectUpdateRequest>() {});
		
		when(palDao.updatePALProject(Mockito.any())).thenReturn(null);
		palService.updatePALProject(request);
	}

	@Test
	public void getPalProductInformationWithValidation() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PalProductInformationTestWithValidation.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProductRequest request = (PALProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProductRequest>() {});

		PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct", null),
				new TypeReference<PALProduct>() {});

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		when(palDao.findPALProductById(Mockito.anyString())).thenReturn(palProduct);
		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(userDetailsService.getAccessControlDetails(eq(request.getUserRole())))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()).getUserRole().getAccessControlInfoList().get(0));
		PALProductResponse actualResponse = palService.getPALProductInformation(request);
		validatePALProductResponse(request, "Standard", palProduct, actualResponse);
	}

	@Test
	public void getPALProductInformationInvalidUserTest() {

		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.UNAUTHORIIZED.getErrorMessage());

		PALProductRequest request = new PALProductRequest();
		request.setProductId("00001");
		request.setUserRole("invaliduser");
		when(userDetailsService.validateUserDetails(Mockito.any(),Mockito.any()))
				.thenThrow(new PALServiceException(ErrorCode.UNAUTHORIIZED));

		palService.getPALProductInformation(request);
	}
	
	@Test
	public void getPALProductAuditlogsInvalidUserTest() {

		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.UNAUTHORIIZED.getErrorMessage());

		PALProductRequest request = new PALProductRequest();
		request.setProductId("00001");
		request.setUserRole("invaliduser");
		when(userDetailsService.validateUserDetails(Mockito.any(),Mockito.any()))
				.thenThrow(new PALServiceException(ErrorCode.UNAUTHORIIZED));

		palService.getPALProductAuditlogs(request);
	}
	
	@Test
	public void getPALProductListInvalidUserTest() {

		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.NO_DATA.getErrorMessage());
		PALProjectRequest request = new PALProjectRequest();
		request.setProjectId("00001");
		request.setUserRole("invaliduser");
		when(userDetailsService.validateUserDetails(Mockito.any(),Mockito.any()))
				.thenThrow(new PALServiceException(ErrorCode.NO_DATA));

		palService.getPALProductList(request);
	}
	
	@Test
	public void getPALProductPersonnelInvalidUserTest() {

		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.UNAUTHORIIZED.getErrorMessage());

		PALProductRequest request = new PALProductRequest();
		request.setProductId("00001");
		request.setUserRole("invaliduser");
		when(userDetailsService.validateUserDetails(Mockito.any(),Mockito.any()))
				.thenThrow(new PALServiceException(ErrorCode.UNAUTHORIIZED));

		palService.getPALProductPersonnel(request);
	}
	
	@Test
	public void getPALProductProgressInvalidUserTest() {

		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.UNAUTHORIIZED.getErrorMessage());

		PALProductRequest request = new PALProductRequest();
		request.setProductId("00001");
		request.setUserRole("invaliduser");
		when(userDetailsService.validateUserDetails(request.getUserRole(), null))
				.thenThrow(new PALServiceException(ErrorCode.UNAUTHORIIZED));
		palService.getPALProductProgress(request);
	}
	
	@Test
	public void getPALProjectProgressInvalidUserTest() {

		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.UNAUTHORIIZED.getErrorMessage());

		PALProjectRequest request = new PALProjectRequest();
		request.setProjectId("00001");
		request.setUserRole("invaliduser");
		when(userDetailsService.validateUserDetails(request.getUserRole(), null))
				.thenThrow(new PALServiceException(ErrorCode.UNAUTHORIIZED));
		palService.getPALProjectProgress(request);
	}
	
	@Test
	public void getPalProductInformation_Multiple_Artwork_Subsections() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductInformationMultipleArtworkTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProductRequest request = (PALProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProductRequest>() {});

		PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct", null),
				new TypeReference<PALProduct>() {});

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		when(palDao.findPALProductById(Mockito.anyString())).thenReturn(palProduct);
		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(userDetailsService.validateUserDetails(any(), any()))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(userDetailsService.getAccessControlDetails(any()))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()).getUserRole().getAccessControlInfoList().get(0));
		PALProductResponse actualResponse = palService.getPALProductInformation(request);
		assertNotNull(actualResponse);

		ProductSection productSection = actualResponse.getSections().stream().filter(x -> "multipleArtworks".equals(x.getName())).findFirst().get();
		assertEquals("multipleArtworks",productSection.getName());
		assertNotNull(productSection.getFields().get(0).getSubSections());
		assertNotNull(productSection.getFields().get(0).getSubSections().get(0).getSubfields());

		validatePALProductResponse(request, "Standard", palProduct, actualResponse);
	}
	
	@Test
	public void getPalProjectListTest() {
		String fileName = "src/test/resources/ProductAttributeListingResponse/projectFilterDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		ProjectFilter request = (ProjectFilter) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("requestFromDate", null),
				new TypeReference<ProjectFilter>() {});
		AppResponse<List<PALProjectResponse>> appresponse = (AppResponse<List<PALProjectResponse>>) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("responseFromDate", null),
				new TypeReference<AppResponse<List<PALProjectResponse>>>() {});
		List<PALProjectResponse> expectedResponse = appresponse.getData();
		List<PALProject> palProjects = new ArrayList<>();
		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});
		palProjects.add(palProject);
		when(palDao.findPALProjectList(Mockito.any(),Mockito.any())).thenReturn((palProjects));
		when(userDetailsService.validateUserDetails(request.getUserRole(), null))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		List<PALProjectResponse> actualResponse = palService.getPALProjectList(request);
		assertEquals(expectedResponse.get(0).getInformation().getProjectName(), actualResponse.get(0).getInformation().getProjectName());
	}
	
	@Test
	public void getPalProjectListSearchNameTest() {
		String fileName = "src/test/resources/ProductAttributeListingResponse/projectFilterDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		ProjectFilter request = (ProjectFilter) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("searchNameRequest", null),
				new TypeReference<ProjectFilter>() {});
		AppResponse<List<PALProjectResponse>> appresponse = (AppResponse<List<PALProjectResponse>>) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("responseFromDate", null),
				new TypeReference<AppResponse<List<PALProjectResponse>>>() {});
		List<PALProjectResponse> expectedResponse = appresponse.getData();
		List<PALProject> palProjects = new ArrayList<>();
		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});
		palProjects.add(palProject);
		when(palDao.findPALProjectList(Mockito.any(), Mockito.any())).thenReturn((palProjects));
		when(userDetailsService.validateUserDetails(request.getUserRole(), null))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		List<PALProjectResponse> actualResponse = palService.getPALProjectList(request);
		assertEquals(expectedResponse.get(0).getInformation().getProjectName(), actualResponse.get(0).getInformation().getProjectName());
	}

	@Test
	public void getPALProjectsearchNoContent() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.NO_DATA.getErrorMessage());
		String fileName = "src/test/resources/ProductAttributeListingResponse/projectFilterDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		ProjectFilter request = (ProjectFilter) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("requestForNegative", null),
				new TypeReference<ProjectFilter>() {});
		when(userDetailsService.validateUserDetails(request.getUserRole(), null))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		palService.getPALProjectList(request);
	}
	
	@Test
	public void palProjectFilterfromDateTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/projectFilterDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		ProjectFilter request = (ProjectFilter) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("requestDateProjectType", null),
				new TypeReference<ProjectFilter>() {});

		List<PALProject> palProjects = new ArrayList<>();
		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});
		palProjects.add(palProject);
		when(palDao.findPALProjectList(Mockito.any(), Mockito.any())).thenReturn((palProjects));
		when(userDetailsService.validateUserDetails(request.getUserRole(), null))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		List<PALProjectResponse> actualResponse = palService.getPALProjectList(request);
		verify(palDao,Mockito.times(1)).findPALProjectList(Mockito.any(), projectFilterCaptor.capture());
		ProjectFilter actualProjectFilter = projectFilterCaptor.getValue();
		List<String> projectType = new ArrayList<>();
		projectType.add(palProject.getProjectType());
		assertEquals(projectType,actualProjectFilter.getProjectType());
		assertEquals(1, actualResponse.size());
	}

	@Test
	public void palProjectFilterProjectTypeTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/projectFilterDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		ProjectFilter request = (ProjectFilter) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("requestDateProjectType", null),
				new TypeReference<ProjectFilter>() {});

		List<PALProject> palProjects = new ArrayList<>();
		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});
		palProjects.add(palProject);
		when(palDao.findPALProjectList(Mockito.any(), Mockito.any())).thenReturn((palProjects));
		when(userDetailsService.validateUserDetails(request.getUserRole(), null))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		List<PALProjectResponse> actualResponse = palService.getPALProjectList(request);
		verify(palDao,Mockito.times(1)).findPALProjectList(Mockito.any(), projectFilterCaptor.capture());
		ProjectFilter actualProjectFilter = projectFilterCaptor.getValue();
		List<String> projectType = new ArrayList<>();
		projectType.add(palProject.getProjectType());
		assertEquals(projectType,actualProjectFilter.getProjectType());
		assertEquals(1, actualResponse.size());
	}
	
	@Test
	public void getPalProjectListMultipleFilterTest() {
		String fileName = "src/test/resources/ProductAttributeListingResponse/projectFilterDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		ProjectFilter request = (ProjectFilter) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("requestCombinationFilter", null),
				new TypeReference<ProjectFilter>() {});
		List<PALProject> palProjects = new ArrayList<>();
		PALProject palProjectFilter1 = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("responseFilter1", null),
				new TypeReference<PALProject>() {});
		PALProject palProjectFilter2 = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("responseFilter2", null),
				new TypeReference<PALProject>() {});
		PALProject palProjectFilter3 = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("responseFilter3", null),
				new TypeReference<PALProject>() {});
		palProjects.add(palProjectFilter1);palProjects.add(palProjectFilter2);palProjects.add(palProjectFilter3);
		when(palDao.findPALProjectList(Mockito.any(), Mockito.any())).thenReturn((palProjects));
		when(userDetailsService.validateUserDetails(request.getUserRole(), null))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		List<PALProjectResponse> actualResponse = palService.getPALProjectList(request);
		assertEquals(palProjects.size(), actualResponse.size());
		assertEquals(palProjects.get(0).getProjectName(), actualResponse.get(0).getInformation().getProjectName());
	}
	
	@Test
	public void getPalProjectListMultipleFilterCombinationTest() {
		String fileName = "src/test/resources/ProductAttributeListingResponse/projectFilterDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		ProjectFilter request = (ProjectFilter) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("requestMultipleFilter", null),
				new TypeReference<ProjectFilter>() {});
		List<PALProject> palProjects = new ArrayList<>();
		PALProject palProjectFilter1 = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("responseFilter1", null),
				new TypeReference<PALProject>() {});
		PALProject palProjectFilter2 = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("responseFilter2", null),
				new TypeReference<PALProject>() {});
		PALProject palProjectFilter3 = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("responseFilter3", null),
				new TypeReference<PALProject>() {});
		palProjects.add(palProjectFilter1);palProjects.add(palProjectFilter2);palProjects.add(palProjectFilter3);
		when(palDao.findPALProjectList(Mockito.any(), Mockito.any())).thenReturn((palProjects));
		when(userDetailsService.validateUserDetails(request.getUserRole(), null))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		List<PALProjectResponse> actualResponse = palService.getPALProjectList(request);
		assertEquals(palProjects.size(), actualResponse.size());
		assertEquals(palProjects.get(0).getProjectName(), actualResponse.get(0).getInformation().getProjectName());
	}
	
	@Test
	public void getPalProjectListNegativeTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.NO_DATA.getErrorMessage());
		String fileName = "src/test/resources/ProductAttributeListingResponse/projectFilterDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		ProjectFilter request = (ProjectFilter) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("requestMultipleFilter", null),
				new TypeReference<ProjectFilter>() {});
		when(userDetailsService.validateUserDetails(request.getUserRole(), null))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		palService.getPALProjectList(request);
	}

	@Test
	public void addPALProjectTestRollingStatusWoCompletionDate() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProjectTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProjectUpdateRequest request = (PALProjectUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProjectUpdateRequest>() {});
		
		request.getInformation().setProjectType("Rolling");
		request.getInformation().setProjectCompletionDate(null);
		PALProject palProject =  (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palProject", null),
				new TypeReference<PALProject>() {});
		when(palDao.savePALProject(Mockito.any())).thenReturn(palProject);
		PALProjectResponse actualResponse = palService.addPALProject(request);
		assertNotNull(actualResponse.getId());
	}
	
	@Test(expected = PALServiceException.class)
	public void addPALProjectTestStatusWoCompletionDate() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProjectTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProjectUpdateRequest request = (PALProjectUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProjectUpdateRequest>() {});
		
		request.getInformation().setProjectType("Standard NDP");
		request.getInformation().setProjectCompletionDate(null);

		palService.addPALProject(request);
	}

	@Test
	public void getProductFilterListTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductFilter.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProjectRequest request = (PALProjectRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request1", null),
				new TypeReference<PALProjectRequest>() {});

		PALProduct palProduct1 = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct1", null),
				new TypeReference<PALProduct>() {});
		PALProduct palProduct2 = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct2", null),
				new TypeReference<PALProduct>() {});
		List<PALProduct> palProducts = new ArrayList<>();
		palProducts.add(palProduct1);
		palProducts.add(palProduct2);

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		AppResponse<PALProjectResponse> appresponse =  (AppResponse<PALProjectResponse>) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("response1", null),
				new TypeReference<AppResponse<PALProjectResponse>>() {});
		PALProjectResponse expectedResponse = appresponse.getData();

		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(palDao.findProductByFilterCondition(Mockito.any(),Mockito.any())).thenReturn(palProducts);
		when(userDetailsService.validateUserDetails(Mockito.any(),Mockito.any()))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(userDetailsService.getAccessControlDetails(any()))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()).getUserRole().getAccessControlInfoList().get(0));
		PALProjectResponse actualResponse = palService.getPALProductList(request);
		assertEquals(2, actualResponse.getProducts().size());
		assertEquals(2, actualResponse.getProducts().get(1).getChildren().size());
		assertNull(actualResponse.getProducts().get(0).getChildren());
		assertEquals(expectedResponse, actualResponse);
	}
	@Test
	public void getProductFilterListWithoutProductTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductFilter.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProjectRequest request = (PALProjectRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request2", null),
				new TypeReference<PALProjectRequest>() {});

		PALProduct palProduct1 = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct1", null),
				new TypeReference<PALProduct>() {});
		PALProduct palProduct2 = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct2", null),
				new TypeReference<PALProduct>() {});
		List<PALProduct> palProducts = new ArrayList<>();
		palProducts.add(palProduct1);
		palProducts.add(palProduct2);

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		AppResponse<PALProjectResponse> appresponse =  (AppResponse<PALProjectResponse>) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("response2", null),
				new TypeReference<AppResponse<PALProjectResponse>>() {});
		PALProjectResponse expectedResponse = appresponse.getData();

		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(palDao.findProductByFilterCondition(Mockito.any(),Mockito.any())).thenReturn(palProducts);
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(userDetailsService.getAccessControlDetails(eq(request.getUserRole())))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()).getUserRole().getAccessControlInfoList().get(0));
		PALProjectResponse actualResponse = palService.getPALProductList(request);
		assertEquals(0, actualResponse.getProducts().size());
		assertEquals(expectedResponse, actualResponse);
	}

	@Test
	public void getProductFilterProgressRange() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductFilter.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProjectRequest request = (PALProjectRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request3", null),
				new TypeReference<PALProjectRequest>() {});

		PALProduct palProduct1 = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct1", null),
				new TypeReference<PALProduct>() {});
		PALProduct palProduct2 = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct2", null),
				new TypeReference<PALProduct>() {});
		List<PALProduct> palProducts = new ArrayList<>();
		palProducts.add(palProduct1);
		palProducts.add(palProduct2);

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		AppResponse<PALProjectResponse> appresponse =  (AppResponse<PALProjectResponse>) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("response3", null),
				new TypeReference<AppResponse<PALProjectResponse>>() {});
		PALProjectResponse expectedResponse = appresponse.getData();

		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(palDao.findProductByFilterCondition(Mockito.any(),Mockito.any())).thenReturn(palProducts);
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(userDetailsService.getAccessControlDetails(eq(request.getUserRole())))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()).getUserRole().getAccessControlInfoList().get(0));
		PALProjectResponse actualResponse = palService.getPALProductList(request);
		assertEquals(1, actualResponse.getProducts().size());
		assertNull(actualResponse.getProducts().get(0).getChildren());
		assertEquals(expectedResponse, actualResponse);
	}

	@Test
	public void getPALProjectPersonnelInvalidRequestTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage("Atleast one user should be selected for PROJECTMANAGER role");

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProjectUpdateTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProjectPersonnelUpdateRequest request = (PALProjectPersonnelUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("requestInValidData", null),
				new TypeReference<PALProjectPersonnelUpdateRequest>() {});
		when(palService.updatePALProjectPersonnel(request)).thenThrow(new PALServiceException(ErrorCode.INVALID_REQUEST_DATA));
		palService.updatePALProjectPersonnel(request);
	}


	@Test
	public void updatePalProductInformationEmptyFieldIdTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_REQUEST_DATA.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductInformationUpdateTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProductUpdateRequest request = (PALProductUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("requestWithEmptyFieldId", null),
				new TypeReference<PALProductUpdateRequest>() {});
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		palService.updatePALProductInformation(request);
	}

	@Test
	public void updatePalProductInformationNullFieldIdTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_REQUEST_DATA.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductInformationUpdateTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProductUpdateRequest request = (PALProductUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("requestWithNullFieldId", null),
				new TypeReference<PALProductUpdateRequest>() {});
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));

		palService.updatePALProductInformation(request);
	}

	@Test
	public void updatePalProductInformationNullFieldValueTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_REQUEST_DATA.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductInformationUpdateTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProductUpdateRequest request = (PALProductUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("requestWithNullFieldValue", null),
				new TypeReference<PALProductUpdateRequest>() {});
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		palService.updatePALProductInformation(request);
	}

	@Test
	public void updatePalProductInformationAutoFieldTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductInformationUpdateTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProductUpdateRequest request = (PALProductUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("requestWithAutoFieldUpdate", null),
				new TypeReference<PALProductUpdateRequest>() {});

		PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct", null),
				new TypeReference<PALProduct>() {});

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		when(palDao.findPALProductById(Mockito.anyString())).thenReturn(palProduct);
		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(userDetailsService.getAccessControlDetails(any()))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()).getUserRole().getAccessControlInfoList().get(0));
		PALProductResponse actualResponse = palService.updatePALProductInformation(request);
		assertNotNull(actualResponse);
		List<ProductField> responseFields = actualResponse.getSections().stream().flatMap(section -> section.getFields().stream())
				.collect(Collectors.toList());
		Double sellingPrice = Util.convertStringToDouble(getProductFieldValue(ApplicationConstant.SELLING_PRICE_FIELD, responseFields));
		Double upt = Util.convertStringToDouble(getProductFieldValue(ApplicationConstant.UPT_FIELD, responseFields));
		Double traySellingValue = Util.convertStringToDouble(getProductFieldValue(ApplicationConstant.TRAY_SELLING_VALUE_FIELD, responseFields));
		assertEquals(traySellingValue, sellingPrice*upt);
		validatePALProductUpdateResponse(request, "Standard", palProduct, actualResponse);

	}

	private String getProductFieldValue(String fieldId, List<ProductField> responseFields) {
		ProductField productField = responseFields.stream()
				.filter(field -> field.getName().equals(fieldId))
				.findFirst().orElse(null);
		return Objects.isNull(productField) ? null : productField.getValue();

	}

	@Test
	public void updatePALProjectTestNullProjectName() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.MISSING_MANDATORY_FIELDS.getErrorMessage());
		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProjectTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProjectUpdateRequest request = (PALProjectUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("updateRequestWithoutProjectName", null),
				new TypeReference<PALProjectUpdateRequest>() {});
		palService.updatePALProject(request);
	}

	@Test
	public void updatePALProjectTestNullStatus() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.MISSING_MANDATORY_FIELDS.getErrorMessage());
		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProjectTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProjectUpdateRequest request = (PALProjectUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("updateRequestWithoutStatus", null),
				new TypeReference<PALProjectUpdateRequest>() {});
		palService.updatePALProject(request);
	}

	@Test
	public void addPALProjectTestWithoutProjectName() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.MISSING_MANDATORY_FIELDS.getErrorMessage());
		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProjectTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProjectUpdateRequest request = (PALProjectUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("addPALProjectTestWithoutProjectName", null),
				new TypeReference<PALProjectUpdateRequest>() {});

		palService.addPALProject(request);
	}

	@Test
	public void getProductFilterProgressRangeWithSingleValue() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductFilter.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProjectRequest request = (PALProjectRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request4", null),
				new TypeReference<PALProjectRequest>() {});
		PALProduct palProduct1 = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct1", null),
				new TypeReference<PALProduct>() {});
		PALProduct palProduct2 = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct2", null),
				new TypeReference<PALProduct>() {});
		List<PALProduct> palProducts = new ArrayList<>();
		palProducts.add(palProduct1);
		palProducts.add(palProduct2);

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		AppResponse<PALProjectResponse> appresponse =  (AppResponse<PALProjectResponse>) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("response4", null),
				new TypeReference<AppResponse<PALProjectResponse>>() {});
		PALProjectResponse expectedResponse = appresponse.getData();

		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(palDao.findProductByFilterCondition(Mockito.any(),Mockito.any())).thenReturn(palProducts);
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(userDetailsService.getAccessControlDetails(eq(request.getUserRole())))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()).getUserRole().getAccessControlInfoList().get(0));
		PALProjectResponse actualResponse = palService.getPALProductList(request);
		assertEquals(1, actualResponse.getProducts().size());
		assertEquals(expectedResponse.getProducts().get(0).getChildren(), actualResponse.getProducts().get(0).getChildren());
		assertEquals(expectedResponse, actualResponse);
	}

	@Test
	public void getPalProjectListSupplierTest() {
		String fileName = "src/test/resources/ProductAttributeListingResponse/projectFilterDetails.json";

		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		ProjectFilter request = (ProjectFilter) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("supplierRequest1", null),
				new TypeReference<ProjectFilter>() {});
		AppResponse<List<PALProjectResponse>> appresponse = (AppResponse<List<PALProjectResponse>>) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("supplierResponseFilter1", null),
				new TypeReference<AppResponse<List<PALProjectResponse>>>() {});
		List<PALProjectResponse> expectedResponse = appresponse.getData();
		List<PALProject> palProjects = new ArrayList<>();
		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});
		PALProduct palProducts = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("supplierPalProduct1", null),
				new TypeReference<PALProduct>() {});
		Set<String> productIds = new HashSet<>();
		productIds.add(palProducts.getProjectId());

		palProjects.add(palProject);
		when(userDetailsService.validateUserDetails(request.getUserRole(), null))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(palDao.findProductByFilterCondition(Mockito.any(), Mockito.any())).thenReturn(Collections.singletonList(palProducts));
		when(palDao.findPALProjectList(ArgumentMatchers.eq(productIds) ,Mockito.any())).thenReturn((palProjects));
		List<PALProjectResponse> actualResponse = palService.getPALProjectList(request);
		assertEquals(expectedResponse, actualResponse);
		assertEquals(expectedResponse.get(0).getInformation().getProjectName(), actualResponse.get(0).getInformation().getProjectName());
	}

  	@Test
	public void getProductFilterListSupplierViewTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductFilter.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);
		PALProjectRequest request = (PALProjectRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request1", null),
				new TypeReference<PALProjectRequest>() {});
		request.setUserRole(ApplicationConstant.SUPPLIER);

		PALProduct palProduct1 = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct1", null),
				new TypeReference<PALProduct>() {});
		PALProduct palProduct2 = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct2", null),
				new TypeReference<PALProduct>() {});
		List<PALProduct> palProducts = new ArrayList<>();
		palProducts.add(palProduct1);
		palProducts.add(palProduct2);

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(palDao.findProductByFilterCondition(Mockito.any(),Mockito.any())).thenReturn(palProducts);
		when(userDetailsService.validateUserDetails(Mockito.any(),Mockito.any()))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(userDetailsService.getAccessControlDetails(eq(request.getUserRole())))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()).getUserRole().getAccessControlInfoList().get(0));
		PALProjectResponse actualResponse = palService.getPALProductList(request);
		assertEquals(2, actualResponse.getProducts().size());
		assertEquals(2, actualResponse.getProducts().get(1).getChildren().size());
		assertNull(actualResponse.getProducts().get(0).getChildren());

	}

	@Test
	public void updatePalProductInformationSupplierValidFieldTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductInformationUpdateTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProductUpdateRequest request = (PALProductUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("supplierFieldValidUpdatedRequest", null),
				new TypeReference<PALProductUpdateRequest>() {});

		PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct", null),
				new TypeReference<PALProduct>() {});

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		when(palDao.findPALProductById(any())).thenReturn(palProduct);
		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(userDetailsService.getAccessControlDetails(any()))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()).getUserRole().getAccessControlInfoList().get(0));
		PALProductResponse actualResponse = palService.updatePALProductInformation(request);
		assertNotNull(actualResponse);
		FieldUpdate updateField = request.getFieldUpdates().get(0);
		List<ProductField> productFields = actualResponse.getSections().stream()
				.flatMap(section -> section.getFields().stream()).collect(Collectors.toList());
		ProductField productField = productFields.stream()
				.filter(field -> updateField.getField().equalsIgnoreCase(field.getName()))
				.findFirst().orElse(new ProductField());
		assertEquals(updateField.getField(), productField.getName());
		assertEquals(updateField.getNewValue(), productField.getValue());

	}

	@Test
	public void updatePalProductInformationSupplierInValidFieldTest() {

		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.UNAUTHORIIZED.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductInformationUpdateTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProductUpdateRequest request = (PALProductUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("supplierFieldInValidUpdatedRequest", null),
				new TypeReference<PALProductUpdateRequest>() {});

		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));

		palService.updatePALProductInformation(request);
	}

	@Test
	public void updatePalProductSubFieldInformationSupplierValidTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductInformationSubFieldUpdateTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProductUpdateRequest request = (PALProductUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("supplierValidFieldUpdateRequest", null),
				new TypeReference<PALProductUpdateRequest>() {});

		PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct", null),
				new TypeReference<PALProduct>() {});

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		when(palDao.findPALProductById(any())).thenReturn(palProduct);
		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(userDetailsService.getAccessControlDetails(eq(request.getUserRole())))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()).getUserRole().getAccessControlInfoList().get(0));
		PALProductResponse actualResponse = palService.updatePALProductInformation(request);
		assertNotNull(actualResponse);
		FieldUpdate updateField = request.getFieldUpdates().get(0);
		List<ProductField> productFields = actualResponse.getSections().stream()
				.flatMap(section -> section.getFields().stream()).collect(Collectors.toList());
		ProductField productField = productFields.stream()
				.filter(field -> updateField.getField().equalsIgnoreCase(field.getName()))
				.findFirst().orElse(new ProductField());
		List<ProductField> productSubFields = productField.getSubSections().stream()
				.flatMap(section -> section.getSubfields().stream()).collect(Collectors.toList());
		ProductField productSubField = productSubFields.stream()
				.filter(field -> ApplicationConstant.CHILD_UPC_FIELD.equalsIgnoreCase(field.getName()))
				.findFirst().orElse(new ProductField());

		assertEquals(ApplicationConstant.CHILD_UPC_FIELD, productSubField.getName());
		assertEquals("12345678", productSubField.getValue());

	}

	@Test
	public void updatePalProductSubFieldInformationSupplierDeleteTest() {

		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.UNAUTHORIIZED.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductInformationSubFieldUpdateTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProductUpdateRequest request = (PALProductUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request_deleteExisting", null),
				new TypeReference<PALProductUpdateRequest>() {});
		request.setUserRole(ApplicationConstant.SUPPLIER);

		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));

		palService.updatePALProductInformation(request);
	}

	@Test
	public void getPalProductAuditSupplierValidTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductAuditTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProductRequest request = (PALProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("supplierAuditLogValidRequest", null),
				new TypeReference<PALProductRequest>() {});

		PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct", null),
				new TypeReference<PALProduct>() {});

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		PALAuditLog palAuditLog = (PALAuditLog) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("supplierValidAuditLog", null),
				new TypeReference<PALAuditLog>() {});
		List<Auditlog> auditlogsExpected = palAuditLog.getAuditLogs();
		when(palDao.findPALProductById(Mockito.anyString())).thenReturn(palProduct);
		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(palDao.findAuditLogs(Mockito.anyString())).thenReturn(palAuditLog);
		when(userDetailsService.validateUserDetails(any(), any()))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		PALProductResponse actualResponse = palService.getPALProductAuditlogs(request);
		assertFalse(ObjectUtils.isEmpty(actualResponse.getAuditlogs()));
		List<com.marksandspencer.foodshub.pal.dto.Auditlog> auditlogsActual = actualResponse.getAuditlogs();
		assertEquals(palAuditLog.getProductId(), actualResponse.getId());
		assertEquals(auditlogsExpected.size(),auditlogsActual.size());
		assertEquals(auditlogsExpected.get(0).getAuditFieldLabel(), auditlogsActual.get(0).getField());
	}

	@Test
	public void getPalProductAuditSupplierInValidTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductAuditTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProductRequest request = (PALProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("supplierAuditLogInValidRequest", null),
				new TypeReference<PALProductRequest>() {});

		PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct", null),
				new TypeReference<PALProduct>() {});

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		PALAuditLog palAuditLog = (PALAuditLog) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("supplierInValidAuditLog", null),
				new TypeReference<PALAuditLog>() {});
		when(palDao.findPALProductById(Mockito.anyString())).thenReturn(palProduct);
		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(palDao.findAuditLogs(Mockito.anyString())).thenReturn(palAuditLog);
		when(userDetailsService.validateUserDetails(any(), any()))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		PALProductResponse actualResponse = palService.getPALProductAuditlogs(request);
		assertEquals(palAuditLog.getProductId(), actualResponse.getId());
		assertTrue(ObjectUtils.isEmpty(actualResponse.getAuditlogs()));
	}

	@Test
	public void updatePalProductInformationSupplierDeleteSubSectionTest() {

		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.UNAUTHORIIZED.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductInformationSubFieldUpdateTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProductUpdateRequest request = (PALProductUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("supplierDeleteSubSectionRequest", null),
				new TypeReference<PALProductUpdateRequest>() {});

		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));

		palService.updatePALProductInformation(request);
	}

	@Test
	public void updatePalProductPersonnelSupplierInvalidTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.UNAUTHORIIZED.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductPersonnelUpdateTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProductUpdateRequest request = (PALProductUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProductUpdateRequest>() {});
		request.setUserRole(ApplicationConstant.SUPPLIER);
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));

		palService.updatePALProductPersonnel(request);
	}

	@Test
	public void getPalProductInformationSMPRequiredTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductInformationTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);


		PALProductRequest request = (PALProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProductRequest>() {});

		PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct", null),
				new TypeReference<PALProduct>() {});
		DataField field = new DataField();
		field.setFieldId("printerType");
		field.setFieldValue("SMP");
		palProduct.getDatafields().add(field);
		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		when(palDao.findPALProductById(Mockito.anyString())).thenReturn(palProduct);
		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), any()))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(userDetailsService.getAccessControlDetails(eq(request.getUserRole())))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()).getUserRole().getAccessControlInfoList().get(0));

		PALProductResponse actualResponse = palService.getPALProductInformation(request);
		assertNotNull(actualResponse);
		validatePALProductResponse(request, "Standard", palProduct, actualResponse);
		actualResponse.getSections().forEach(sections ->{
			if(sections.getName().equals(ApplicationConstant.PRINT_AND_PACKAGING)){
				Optional<ProductField> isSMPApprovedPresent = sections.getFields().stream().filter(fields -> fields.getName().equals(ApplicationConstant.SMP_APPROVED)).findAny();
				assertTrue(isSMPApprovedPresent.isPresent());
				assertEquals(isSMPApprovedPresent.get().getName(),ApplicationConstant.SMP_APPROVED);
			}
		});
	}

	@Test
	public void getPalProductInformationSMPNotRequiredTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductInformationTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);


		PALProductRequest request = (PALProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProductRequest>() {});

		PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct", null),
				new TypeReference<PALProduct>() {});
		palProduct.getDatafields().forEach(fields->{
			if(fields.getFieldId().equals(ApplicationConstant.PRINTER_TYPE)){
				fields.setFieldValue("A List");
			}
		});
		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		when(palDao.findPALProductById(Mockito.anyString())).thenReturn(palProduct);
		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(userDetailsService.getAccessControlDetails(eq(request.getUserRole())))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()).getUserRole().getAccessControlInfoList().get(0));

		PALProductResponse actualResponse = palService.getPALProductInformation(request);
		assertNotNull(actualResponse);
		actualResponse.getSections().forEach(sections ->{
			if(sections.getName().equals(ApplicationConstant.PRINT_AND_PACKAGING)){
				Optional<ProductField> isSMPApprovedPresent = sections.getFields().stream().filter(fields -> fields.getName().equals(ApplicationConstant.SMP_APPROVED)).findAny();
				assertFalse(isSMPApprovedPresent.isPresent(),"SMP Required field should not be present");
			}
		});
	}

	@Test
	public void listTemplateRolesTest() {
		String projectId = "123";
		PALProject palProject = new PALProject();
		palProject.setId(projectId);
		palProject.setTemplateId("611d11f7cde71e991b217579");
		when(palDao.findPALProjectById(eq(projectId))).thenReturn(palProject);

		List<PALRole> palRoles = palService.listProjectTemplateRoles(projectId);
		assertNotNull(palRoles);
		assertEquals(9, palRoles.size());
	}

	@Test
	public void listTemplateRolesInvalidTemplateTest() {
		String projectId = "123";
		PALProject palProject = new PALProject();
		palProject.setId(projectId);
		palProject.setTemplateId("611d11f7cde71e991b217580");
		when(palDao.findPALProjectById(eq(projectId))).thenReturn(palProject);

		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_TEMPLATE_ID.getErrorMessage());

		palService.listProjectTemplateRoles(projectId);
		
	}

	@Test
	public void listTemplateRolesInvalidProjectTest() {
		when(palDao.findPALProjectById(any())).thenReturn(null);

		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_PROJECT_ID.getErrorMessage());

		palService.listProjectTemplateRoles("123");

	}

	@Test
	public void getPalProjectDetailsForOtherSupplierTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.NO_DATA.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductListTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProjectRequest request = (PALProjectRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProjectRequest>() {});
		request.setUserRole(ApplicationConstant.SUPPLIER);

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(palDao.findProductByFilterCondition(Mockito.any(),Mockito.any())).thenReturn(null);
		when(userDetailsService.validateUserDetails(Mockito.any(),Mockito.any()))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		palService.getProjectDetails(request);
	}

	@Test
	public void getPalProductListForOtherSupplierTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.NO_DATA.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductListTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProjectRequest request = (PALProjectRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProjectRequest>() {});
		request.setUserRole(ApplicationConstant.SUPPLIER);

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(palDao.findProductByFilterCondition(Mockito.any(),Mockito.any())).thenReturn(null);
		when(userDetailsService.validateUserDetails(Mockito.any(),Mockito.any()))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		palService.getPALProductList(request);
	}

	@Test
	public void getPALTemplatesWithSectionTest() {
		PALTemplateRequest request = new PALTemplateRequest();
		request.setSectionsRequired(true);
		when(palDao.findALLPALTemplate()).thenReturn(palTemplates);
		List<PALTemplate> response = palService.getPALTemplates(request);
		assertEquals(palTemplates, response);
		assertEquals(palTemplates.stream().map(PALTemplate::getId).collect(Collectors.toList()), response.stream().map(PALTemplate::getId).collect(Collectors.toList()));
		assertEquals(5, response.size());
		assertNotNull(response.get(0).getSections());
	}

	@Test
	public void getPALTemplatesWithoutSectionTest() {
		PALTemplateRequest request = new PALTemplateRequest();
		request.setSectionsRequired(false);
		List<PALTemplate> expectedResult = new ArrayList<>(palTemplates);
		expectedResult.forEach(template -> template.setSections(null));
		when(palDao.findALLPALTemplate()).thenReturn(expectedResult);
		List<PALTemplate> response = palService.getPALTemplates(request);
		assertEquals(expectedResult, response);
		assertEquals(expectedResult.stream().map(PALTemplate::getId).collect(Collectors.toList()), response.stream().map(PALTemplate::getId).collect(Collectors.toList()));
		assertEquals(5, response.size());
		assertNull(response.get(0).getSections());
	}

	@Test
	public void getPALTemplatesIdsWithoutSectionTest() {
		String templateId = "611d11f7cde71e991b217579";
		PALTemplateRequest request = new PALTemplateRequest();
		request.setPalTemplateIds(new ArrayList<>(Collections.singletonList(templateId)));
		request.setSectionsRequired(false);
		List<PALTemplate> expectedResult = palTemplates.stream().filter(template -> template.getId().equalsIgnoreCase(templateId)).collect(Collectors.toList());
		expectedResult.forEach(template -> template.setSections(null));
		when(palDao.findALLPALTemplate()).thenReturn(expectedResult);
		List<PALTemplate> response = palService.getPALTemplates(request);
		assertEquals(expectedResult, response);
		assertEquals(1, response.size());
		assertNull(response.get(0).getSections());
	}

	@Test
	public void getPALTemplatesIdsWithSectionTest() {
		String templateId = "611d11f7cde71e991b217579";
		PALTemplateRequest request = new PALTemplateRequest();
		request.setPalTemplateIds(new ArrayList<>(Collections.singletonList(templateId)));
		request.setSectionsRequired(true);
		when(palDao.findALLPALTemplate()).thenReturn(palTemplates.stream().filter(template -> template.getId().equalsIgnoreCase(templateId)).collect(Collectors.toList()));
		List<PALTemplate> response = palService.getPALTemplates(request);
		assertEquals(palTemplates.stream().filter(x->templateId.equals(x.getId())).collect(Collectors.toList()), response);
		assertEquals(1, response.size());
		assertNotNull(response.get(0).getSections());
	}

	@Test
	public void getPALTemplatesIdsWithSectionUserRoleTest() {
		String templateId = "611d11f7cde71e991b217579";
		PALTemplateRequest request = new PALTemplateRequest();
		request.setPalTemplateIds(new ArrayList<>(Collections.singletonList(templateId)));
		request.setSectionsRequired(true);
		request.setUserRole(ApplicationConstant.PROJECT_MANAGER);
		when(palDao.findALLPALTemplate()).thenReturn(palTemplates.stream().filter(template -> template.getId().equalsIgnoreCase(templateId)).collect(Collectors.toList()));
		when(userDetailsService.getAccessControlDetails(any())).thenReturn(TestUtility.getUserDetails(request.getUserRole()).getUserRole().getAccessControlInfoList().get(0));
		List<PALTemplate> response = palService.getPALTemplates(request);
		assertEquals(1, response.size());
		assertNotNull(response.get(0).getSections());
	}

	@Test
	public void getPALTemplatesIdsWithSectionReadOnlyUserTest() {
		String templateId = "611d11f7cde71e991b217579";
		PALTemplateRequest request = new PALTemplateRequest();
		request.setPalTemplateIds(new ArrayList<>(Collections.singletonList(templateId)));
		request.setSectionsRequired(true);
		request.setUserRole("readOnlyUser");
		when(palDao.findALLPALTemplate()).thenReturn(palTemplates.stream().filter(template -> template.getId().equalsIgnoreCase(templateId)).collect(Collectors.toList()));
		AccessControlInfo accessInfo = TestUtility.getUserDetails(request.getUserRole()).getUserRole().getAccessControlInfoList().get(0);
		when(userDetailsService.getAccessControlDetails(any())).thenReturn(accessInfo);
		List<PALTemplate> response = palService.getPALTemplates(request);
		assertEquals(1, response.size());
		assertNull(response.get(0).getSections());
	}

	@Test
	public void getPALTemplatesInvalidTemplateIdTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_TEMPLATE_ID.getErrorMessage());

		PALTemplateRequest request = new PALTemplateRequest();
		request.setPalTemplateIds(new ArrayList<>(Collections.singletonList("invalidId")));
		request.setSectionsRequired(true);
		when(palDao.findALLPALTemplate()).thenReturn(palTemplates);
		palService.getPALTemplates(request);
	}

	@Test
	public void getPALTemplatesMissingTemplateIdsTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_TEMPLATE_ID.getErrorMessage());

		PALTemplateRequest request = new PALTemplateRequest();
		request.setPalTemplateIds(new ArrayList<>(Arrays.asList("611d11f7cde71e991b217579","missingId")));
		request.setSectionsRequired(true);
		when(palDao.findALLPALTemplate()).thenReturn(palTemplates);
		palService.getPALTemplates(request);
	}

	@Test
	public void getPALTemplatesIdsNullRequestTest() {
		when(palDao.findALLPALTemplate()).thenReturn(palTemplates);
		List<PALTemplate> response = palService.getPALTemplates(null);
		assertEquals(palTemplates, response);
		assertEquals(palTemplates.stream().map(PALTemplate::getId).collect(Collectors.toList()), response.stream().map(PALTemplate::getId).collect(Collectors.toList()));
		assertEquals(5, response.size());
		assertNotNull(response.get(0).getSections());
	}

	@Test
	public void duplicateProductsNullUserRoleTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.MISSING_MANDATORY_FIELDS.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/DuplicateProduct.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		DuplicateProductRequest request = (DuplicateProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("validationRequest", null),
				new TypeReference<DuplicateProductRequest>() {});
		request.setUserRole(null);
		palService.duplicateProducts(request);
	}

	@Test
	public void duplicateProductsNullModelProductTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.MISSING_MANDATORY_FIELDS.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/DuplicateProduct.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		DuplicateProductRequest request = (DuplicateProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("validationRequest", null),
				new TypeReference<DuplicateProductRequest>() {});
		request.setModelProductId(null);
		palService.duplicateProducts(request);
	}

	@Test
	public void duplicateProductsNullRequestTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.MISSING_MANDATORY_FIELDS.getErrorMessage());

		DuplicateProductRequest request = new DuplicateProductRequest();
		palService.duplicateProducts(request);
	}

	@Test
	public void duplicateProductsNullProductListRequestTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.MISSING_MANDATORY_FIELDS.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/DuplicateProduct.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		DuplicateProductRequest request = (DuplicateProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("validationRequest", null),
				new TypeReference<DuplicateProductRequest>() {});
		request.setProducts(new ArrayList<>());
		palService.duplicateProducts(request);
	}

	@Test
	public void duplicateProductsNullPersonnelRequestTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.MISSING_MANDATORY_FIELDS.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/DuplicateProduct.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		DuplicateProductRequest request = (DuplicateProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("validationRequest", null),
				new TypeReference<DuplicateProductRequest>() {});
		request.getProducts().forEach(product -> product.setPersonnel(null));
		palService.duplicateProducts(request);
	}

	@Test
	public void duplicateProductsNullTemplateIdRequestTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.MISSING_MANDATORY_FIELDS.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/DuplicateProduct.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		DuplicateProductRequest request = (DuplicateProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("validationRequest", null),
				new TypeReference<DuplicateProductRequest>() {});
		request.getProducts().forEach(product -> product.setTemplateId(null));
		palService.duplicateProducts(request);
	}

	@Test
	public void duplicateProductsNullProjectIdRequestTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.MISSING_MANDATORY_FIELDS.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/DuplicateProduct.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		DuplicateProductRequest request = (DuplicateProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("validationRequest", null),
				new TypeReference<DuplicateProductRequest>() {});
		request.getProducts().forEach(product -> product.setProjectId(null));
		palService.duplicateProducts(request);
	}

	@Test
	public void duplicateProductsNullDataFieldsRequestTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.MISSING_MANDATORY_FIELDS.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/DuplicateProduct.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		DuplicateProductRequest request = (DuplicateProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("validationRequest", null),
				new TypeReference<DuplicateProductRequest>() {});
		request.getProducts().forEach(product -> product.setDataFields(null));
		palService.duplicateProducts(request);
	}

	@Test
	public void duplicateProductsUnauthorizedUserTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.UNAUTHORIIZED.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/DuplicateProduct.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		DuplicateProductRequest request = (DuplicateProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("validationRequest", null),
				new TypeReference<DuplicateProductRequest>() {});
		request.setUserRole(ApplicationConstant.SUPPLIER);
		PALConfiguration accessConfig = palConfigs.stream().filter(config -> ApplicationConstant.PRODUCT_DUPLICATE_ACCESS.equalsIgnoreCase(config.getId()))
				.findFirst().orElse(null);
		assertNotNull(accessConfig);
		List<String> accessibleRoles = accessConfig.getValues();
		assertNotNull(accessibleRoles);

		when(userDetailsService.validateUserDetails(eq(request.getUserRole()),eq(accessibleRoles)))
				.thenThrow(new PALServiceException(ErrorCode.UNAUTHORIIZED));

		palService.duplicateProducts(request);
	}

	@Test
	public void duplicateProductsModelProductNotPresentTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_PRODUCT_ID.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/DuplicateProduct.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		DuplicateProductRequest request = (DuplicateProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("validationRequest", null),
				new TypeReference<DuplicateProductRequest>() {});
		PALConfiguration accessConfig = palConfigs.stream().filter(config -> ApplicationConstant.PRODUCT_DUPLICATE_ACCESS.equalsIgnoreCase(config.getId()))
				.findFirst().orElse(null);
		assertNotNull(accessConfig);
		List<String> accessibleRoles = accessConfig.getValues();
		assertNotNull(accessibleRoles);

		when(userDetailsService.validateUserDetails(eq(request.getUserRole()),eq(accessibleRoles)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(palDao.findPALProductById(eq(request.getModelProductId())))
				.thenReturn(null);

		palService.duplicateProducts(request);
	}

	@Test
	public void duplicateProductsTemplateMismatchTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_TEMPLATE_ID.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/DuplicateProduct.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		DuplicateProductRequest request = (DuplicateProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("validationRequest", null),
				new TypeReference<DuplicateProductRequest>() {});

		PALProduct modelProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("modelProduct", null),
				new TypeReference<PALProduct>() {});
		modelProduct.setTemplateId("mismatch");

		PALConfiguration accessConfig = palConfigs.stream().filter(config -> ApplicationConstant.PRODUCT_DUPLICATE_ACCESS.equalsIgnoreCase(config.getId()))
				.findFirst().orElse(null);
		assertNotNull(accessConfig);
		List<String> accessibleRoles = accessConfig.getValues();
		assertNotNull(accessibleRoles);

		when(userDetailsService.validateUserDetails(eq(request.getUserRole()),eq(accessibleRoles)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(palDao.findPALProductById(eq(request.getModelProductId())))
				.thenReturn(modelProduct);

		palService.duplicateProducts(request);
	}

	@Test
	public void duplicateProductsInvalidTemplateTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_TEMPLATE_ID.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/DuplicateProduct.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		DuplicateProductRequest request = (DuplicateProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("validationRequest", null),
				new TypeReference<DuplicateProductRequest>() {});
		request.getProducts().forEach(product -> product.setTemplateId("invalid"));

		PALProduct modelProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("modelProduct", null),
				new TypeReference<PALProduct>() {});
		modelProduct.setTemplateId("invalid");

		PALConfiguration accessConfig = palConfigs.stream().filter(config -> ApplicationConstant.PRODUCT_DUPLICATE_ACCESS.equalsIgnoreCase(config.getId()))
				.findFirst().orElse(null);
		assertNotNull(accessConfig);
		List<String> accessibleRoles = accessConfig.getValues();
		assertNotNull(accessibleRoles);

		when(userDetailsService.validateUserDetails(eq(request.getUserRole()),eq(accessibleRoles)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(palDao.findPALProductById(eq(request.getModelProductId())))
				.thenReturn(modelProduct);

		palService.duplicateProducts(request);
	}

	@Test
	public void duplicateProductsNullProjectsPresentTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_PROJECT_ID.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/DuplicateProduct.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		DuplicateProductRequest request = (DuplicateProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("validationRequest", null),
				new TypeReference<DuplicateProductRequest>() {});

		PALProduct modelProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("modelProduct", null),
				new TypeReference<PALProduct>() {});

		Set<String> projectIds = request.getProducts().stream().map(PALProductCreateRequest::getProjectId)
				.collect(Collectors.toSet());
		projectIds.add(modelProduct.getProjectId());

		PALConfiguration accessConfig = palConfigs.stream().filter(config -> ApplicationConstant.PRODUCT_DUPLICATE_ACCESS.equalsIgnoreCase(config.getId()))
				.findFirst().orElse(null);
		assertNotNull(accessConfig);
		List<String> accessibleRoles = accessConfig.getValues();
		assertNotNull(accessibleRoles);

		when(userDetailsService.validateUserDetails(eq(request.getUserRole()),eq(accessibleRoles)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(palDao.findPALProductById(eq(request.getModelProductId())))
				.thenReturn(modelProduct);
		when(palDao.findPALProjectList(eq(new HashSet<>(projectIds)), eq(null)))
				.thenReturn(null);

		palService.duplicateProducts(request);
	}

	@Test
	public void duplicateProductsMissingProjectsTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_PROJECT_ID.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/DuplicateProduct.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		DuplicateProductRequest request = (DuplicateProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("validationRequest", null),
				new TypeReference<DuplicateProductRequest>() {});

		PALProduct modelProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("modelProduct", null),
				new TypeReference<PALProduct>() {});

		PALProject differentProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("differentProject", null),
				new TypeReference<PALProject>() {});

		List<PALProject> projects = new ArrayList<>();
		projects.add(differentProject);

		Set<String> projectIds = request.getProducts().stream().map(PALProductCreateRequest::getProjectId)
				.collect(Collectors.toSet());
		projectIds.add(modelProduct.getProjectId());

		PALConfiguration accessConfig = palConfigs.stream().filter(config -> ApplicationConstant.PRODUCT_DUPLICATE_ACCESS.equalsIgnoreCase(config.getId()))
				.findFirst().orElse(null);
		assertNotNull(accessConfig);
		List<String> accessibleRoles = accessConfig.getValues();
		assertNotNull(accessibleRoles);

		when(userDetailsService.validateUserDetails(eq(request.getUserRole()),eq(accessibleRoles)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(palDao.findPALProductById(eq(request.getModelProductId())))
				.thenReturn(modelProduct);
		when(palDao.findPALProjectList(eq(new HashSet<>(projectIds)), eq(null)))
				.thenReturn(projects);

		palService.duplicateProducts(request);
	}

	@Test
	public void duplicateProductsInSameProjectTest() {
		String fileName = "src/test/resources/ProductAttributeListingResponse/DuplicateProduct.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		DuplicateProductRequest request = (DuplicateProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("requestForSameProject", null),
				new TypeReference<DuplicateProductRequest>() {});

		PALProduct modelProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("modelProduct", null),
				new TypeReference<PALProduct>() {});

		PALProject sameProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("modelProject", null),
				new TypeReference<PALProject>() {});

		DuplicateProductResponse expectedResponse = (DuplicateProductResponse) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("responseForSameProject", null),
				new TypeReference<DuplicateProductResponse>() {});

		List<PALProject> projects = new ArrayList<>();
		projects.add(sameProject);

		Set<String> projectIds = request.getProducts().stream().map(PALProductCreateRequest::getProjectId)
				.collect(Collectors.toSet());
		projectIds.add(modelProduct.getProjectId());

		PALConfiguration accessConfig = palConfigs.stream().filter(config -> ApplicationConstant.PRODUCT_DUPLICATE_ACCESS.equalsIgnoreCase(config.getId()))
				.findFirst().orElse(null);
		assertNotNull(accessConfig);
		List<String> accessibleRoles = accessConfig.getValues();
		assertNotNull(accessibleRoles);

		when(userDetailsService.validateUserDetails(eq(request.getUserRole()),eq(accessibleRoles))).thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(palDao.findPALProductById(eq(request.getModelProductId()))).thenReturn(modelProduct);
		when(palDao.findPALProjectList(eq(new HashSet<>(projectIds)), eq(null))).thenReturn(projects);

		palService.duplicateProducts(request);
		verify(palDao,Mockito.times(1)).saveAllProducts(productsCaptor.capture());
		List<PALProduct> dbproducts = productsCaptor.getValue();
		when(palDao.saveAllProducts(eq(dbproducts))).thenReturn(getProductSaveResponse(dbproducts));

		DuplicateProductResponse actualResponse = palService.duplicateProducts(request);

		assertNotNull(actualResponse);
		assertEquals(expectedResponse, actualResponse);
		// no notification should be triggered since project status is in Draft
		verify(notificationService, times(0)).sendEmailMessage(any(),any(),eq(MessageTemplate.ADD_PRODUCT));
	}

	@Test
	public void duplicateProductsInDifferentProjectTest() {
		String fileName = "src/test/resources/ProductAttributeListingResponse/DuplicateProduct.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		DuplicateProductRequest request = (DuplicateProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("requestForDifferentProject", null),
				new TypeReference<DuplicateProductRequest>() {});

		PALProduct modelProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("modelProduct", null),
				new TypeReference<PALProduct>() {});

		DuplicateProductResponse expectedResponse = (DuplicateProductResponse) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("responseForDifferentProject", null),
				new TypeReference<DuplicateProductResponse>() {});

		PALProject sameProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("modelProject", null),
				new TypeReference<PALProject>() {});
		PALProject differentProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("differentProject", null),
				new TypeReference<PALProject>() {});

		List<PALProject> projects = new ArrayList<>();
		projects.add(sameProject);
		projects.add(differentProject);

		Set<String> projectIds = request.getProducts().stream().map(PALProductCreateRequest::getProjectId)
				.collect(Collectors.toSet());
		projectIds.add(modelProduct.getProjectId());

		PALConfiguration accessConfig = palConfigs.stream().filter(config -> ApplicationConstant.PRODUCT_DUPLICATE_ACCESS.equalsIgnoreCase(config.getId()))
				.findFirst().orElse(null);
		assertNotNull(accessConfig);
		List<String> accessibleRoles = accessConfig.getValues();
		assertNotNull(accessibleRoles);

		when(userDetailsService.validateUserDetails(eq(request.getUserRole()),eq(accessibleRoles))).thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(palDao.findPALProductById(eq(request.getModelProductId()))).thenReturn(modelProduct);
		when(palDao.findPALProjectList(eq(new HashSet<>(projectIds)), eq(null))).thenReturn(projects);

		palService.duplicateProducts(request);
		verify(palDao,Mockito.times(1)).saveAllProducts(productsCaptor.capture());
		List<PALProduct> dbproducts = productsCaptor.getValue();
		when(palDao.saveAllProducts(eq(dbproducts))).thenReturn(getProductSaveResponse(dbproducts));

		DuplicateProductResponse actualResponse = palService.duplicateProducts(request);

		assertNotNull(actualResponse);
		assertEquals(expectedResponse, actualResponse);
		// no notification should be triggered since project status is in Draft
		verify(notificationService, times(0)).sendEmailMessage(any(),any(),eq(MessageTemplate.ADD_PRODUCT));
	}

	@Test
	public void duplicateProductsMultipleProductsTest() {
		String fileName = "src/test/resources/ProductAttributeListingResponse/DuplicateProduct.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		DuplicateProductRequest request = (DuplicateProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("requestForMultipleProducts", null),
				new TypeReference<DuplicateProductRequest>() {});

		PALProduct modelProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("modelProduct", null),
				new TypeReference<PALProduct>() {});

		DuplicateProductResponse expectedResponse = (DuplicateProductResponse) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("responseForMultipleProducts", null),
				new TypeReference<DuplicateProductResponse>() {});

		PALProject sameProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("modelProject", null),
				new TypeReference<PALProject>() {});
		PALProject differentProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("differentProject", null),
				new TypeReference<PALProject>() {});

		List<PALProject> projects = new ArrayList<>();
		projects.add(sameProject);
		projects.add(differentProject);

		Set<String> projectIds = request.getProducts().stream().map(PALProductCreateRequest::getProjectId)
				.collect(Collectors.toSet());
		projectIds.add(modelProduct.getProjectId());

		PALConfiguration accessConfig = palConfigs.stream().filter(config -> ApplicationConstant.PRODUCT_DUPLICATE_ACCESS.equalsIgnoreCase(config.getId()))
				.findFirst().orElse(null);
		assertNotNull(accessConfig);
		List<String> accessibleRoles = accessConfig.getValues();
		assertNotNull(accessibleRoles);

		when(userDetailsService.validateUserDetails(eq(request.getUserRole()),eq(accessibleRoles))).thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(palDao.findPALProductById(eq(request.getModelProductId()))).thenReturn(modelProduct);
		when(palDao.findPALProjectList(eq(new HashSet<>(projectIds)), eq(null))).thenReturn(projects);

		palService.duplicateProducts(request);
		verify(palDao,Mockito.times(1)).saveAllProducts(productsCaptor.capture());
		List<PALProduct> dbproducts = productsCaptor.getValue();
		when(palDao.saveAllProducts(eq(dbproducts))).thenReturn(getProductSaveResponse(dbproducts));

		DuplicateProductResponse actualResponse = palService.duplicateProducts(request);

		assertNotNull(actualResponse);
		assertEquals(expectedResponse, actualResponse);
		// no notification should be triggered since project status is in Draft
		verify(notificationService, times(0)).sendEmailMessage(any(),any(),eq(MessageTemplate.ADD_PRODUCT));
	}

	private List<PALProduct> getProductSaveResponse(List<PALProduct> products) {
		List<PALProduct> palProducts = new ArrayList<>();
		AtomicInteger count = new AtomicInteger(0);
		products.forEach(product -> {
			PALProduct palProduct = new PALProduct();
			palProduct.setId(String.format("product%s", count.incrementAndGet()));
			palProduct.setTemplateId(product.getTemplateId());
			palProduct.setProjectId(product.getProjectId());
			palProduct.setPersonnel(product.getPersonnel());
			palProduct.setDatafields(product.getDatafields());
			palProducts.add(palProduct);
		});

		return palProducts;
	}

	@Test
	public void duplicateProductsMultipleProductsNotification1Test() {
		String fileName = "src/test/resources/ProductAttributeListingResponse/DuplicateProduct.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		DuplicateProductRequest request = (DuplicateProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("requestForMultipleProducts", null),
				new TypeReference<DuplicateProductRequest>() {});

		PALProduct modelProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("modelProduct", null),
				new TypeReference<PALProduct>() {});

		DuplicateProductResponse expectedResponse = (DuplicateProductResponse) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("responseForMultipleProducts", null),
				new TypeReference<DuplicateProductResponse>() {});

		PALProject sameProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("modelProject", null),
				new TypeReference<PALProject>() {});
		sameProject.setStatus(Status.CREATIVE_STAGE.getStatus());

		PALProject differentProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("differentProject", null),
				new TypeReference<PALProject>() {});

		List<PALProject> projects = new ArrayList<>();
		projects.add(sameProject);
		projects.add(differentProject);

		Set<String> projectIds = request.getProducts().stream().map(PALProductCreateRequest::getProjectId)
				.collect(Collectors.toSet());
		projectIds.add(modelProduct.getProjectId());

		PALConfiguration accessConfig = palConfigs.stream().filter(config -> ApplicationConstant.PRODUCT_DUPLICATE_ACCESS.equalsIgnoreCase(config.getId()))
				.findFirst().orElse(null);
		assertNotNull(accessConfig);
		List<String> accessibleRoles = accessConfig.getValues();
		assertNotNull(accessibleRoles);

		when(userDetailsService.validateUserDetails(eq(request.getUserRole()),eq(accessibleRoles))).thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(palDao.findPALProductById(eq(request.getModelProductId()))).thenReturn(modelProduct);
		when(palDao.findPALProjectList(eq(new HashSet<>(projectIds)), eq(null))).thenReturn(projects);

		palService.duplicateProducts(request);
		verify(palDao,Mockito.times(1)).saveAllProducts(productsCaptor.capture());
		List<PALProduct> dbproducts = productsCaptor.getValue();
		when(palDao.saveAllProducts(eq(dbproducts))).thenReturn(getProductSaveResponse(dbproducts));

		DuplicateProductResponse actualResponse = palService.duplicateProducts(request);

		assertNotNull(actualResponse);
		assertEquals(expectedResponse, actualResponse);
		// 1 notification should be triggered for duplicating product in project with CreativeStage status
		verify(notificationService, times(1)).sendEmailMessage(any(),any(),eq(MessageTemplate.ADD_PRODUCT));
	}

	@Test
	public void duplicateProductsMultipleProductsNotification2Test() {
		String fileName = "src/test/resources/ProductAttributeListingResponse/DuplicateProduct.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		DuplicateProductRequest request = (DuplicateProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("requestForMultipleProducts", null),
				new TypeReference<DuplicateProductRequest>() {});

		PALProduct modelProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("modelProduct", null),
				new TypeReference<PALProduct>() {});

		DuplicateProductResponse expectedResponse = (DuplicateProductResponse) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("responseForMultipleProducts", null),
				new TypeReference<DuplicateProductResponse>() {});

		PALProject sameProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("modelProject", null),
				new TypeReference<PALProject>() {});
		sameProject.setStatus(Status.CREATIVE_STAGE.getStatus());

		PALProject differentProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("differentProject", null),
				new TypeReference<PALProject>() {});
		differentProject.setStatus(Status.CREATIVE_STAGE.getStatus());

		List<PALProject> projects = new ArrayList<>();
		projects.add(sameProject);
		projects.add(differentProject);

		Set<String> projectIds = request.getProducts().stream().map(PALProductCreateRequest::getProjectId)
				.collect(Collectors.toSet());
		projectIds.add(modelProduct.getProjectId());

		PALConfiguration accessConfig = palConfigs.stream().filter(config -> ApplicationConstant.PRODUCT_DUPLICATE_ACCESS.equalsIgnoreCase(config.getId()))
				.findFirst().orElse(null);
		assertNotNull(accessConfig);
		List<String> accessibleRoles = accessConfig.getValues();
		assertNotNull(accessibleRoles);

		when(userDetailsService.validateUserDetails(eq(request.getUserRole()),eq(accessibleRoles))).thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(palDao.findPALProductById(eq(request.getModelProductId()))).thenReturn(modelProduct);
		when(palDao.findPALProjectList(eq(new HashSet<>(projectIds)), eq(null))).thenReturn(projects);

		palService.duplicateProducts(request);
		verify(palDao,Mockito.times(1)).saveAllProducts(productsCaptor.capture());
		List<PALProduct> dbproducts = productsCaptor.getValue();
		when(palDao.saveAllProducts(eq(dbproducts))).thenReturn(getProductSaveResponse(dbproducts));

		DuplicateProductResponse actualResponse = palService.duplicateProducts(request);

		assertNotNull(actualResponse);
		assertEquals(expectedResponse, actualResponse);
		// 2 notifications should be triggered for duplicating products in projects with CreativeStage status
		verify(notificationService, times(2)).sendEmailMessage(any(),any(),eq(MessageTemplate.ADD_PRODUCT));
	}	
	
	@Test
	public void getBulkProductInformationsSuccess_Scenario() {
		List<PALProduct> productResponse = new ArrayList<>();
		PALProduct product = new PALProduct();
		product.setId("62311c0e36889a40d1a8a1cd");
		List<DataField> datafields =new ArrayList<>();
		datafields.add(DataField.builder().fieldId(ApplicationConstant.STATUS_FIELD).fieldValue(Status.IN_PROGRESS.getStatus()).build());
		datafields.add(DataField.builder().fieldId(ApplicationConstant.PRODUCT_TITLE_FIELD).fieldValue("Test").build());
		product.setDatafields(datafields);
		productResponse.add(product);		
		BulkProductRequest bulkProductRequest = new BulkProductRequest();
		List<String> products = new ArrayList<>();
		products.add("62311c0e36889a40d1a8a1cd");
		bulkProductRequest.setProducts(products);
		bulkProductRequest.setFieldId("status");
		bulkProductRequest.setUserRole("projectManager");
		AccessControlInfo accessInfo = TestUtility.getUserDetails(bulkProductRequest.getUserRole()).getUserRole().getAccessControlInfoList().get(0);
		when(userDetailsService.getAccessControlDetails(Mockito.anyString())).thenReturn(accessInfo);
		when(userDetailsService.validateUserDetails(eq(bulkProductRequest.getUserRole()), eq(null)))
		.thenReturn(TestUtility.getUserDetails(bulkProductRequest.getUserRole()));
		when(palDao.findPALProducts(Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(productResponse);
		BulkProductResponse actualResponse = palService.getBulkProductInformations(bulkProductRequest);
		assertNotNull(actualResponse);
		assertEquals(1,actualResponse.getProductResponse().size());
		assertEquals("62311c0e36889a40d1a8a1cd",actualResponse.getProductResponse().get(0).getProductId());
		assertEquals("In Progress",actualResponse.getProductResponse().get(0).getOldValue());
		assertEquals("Test",actualResponse.getProductResponse().get(0).getProductName());
		verify(palDao, times(1)).findAllPALFields();
		verify(palDao, times(1)).findPALProducts(Mockito.any(),Mockito.any(),Mockito.any());
		verify(userDetailsService, times(1)).getAccessControlDetails(Mockito.anyString());
	}
	
	@Test
	public void getBulkProductInformation_No_Data() {	
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.NO_DATA.getErrorMessage());		
		BulkProductRequest bulkProductRequest = new BulkProductRequest();
		List<String> products = new ArrayList<>();
		products.add("62311c0e36889a40d1a8a1cd");
		bulkProductRequest.setProducts(products);
		bulkProductRequest.setFieldId("status");
		bulkProductRequest.setUserRole("projectManager");
		AccessControlInfo accessInfo = TestUtility.getUserDetails(bulkProductRequest.getUserRole()).getUserRole().getAccessControlInfoList().get(0);
		when(userDetailsService.getAccessControlDetails(Mockito.anyString())).thenReturn(accessInfo);
		when(palDao.findPALProducts(Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(new ArrayList<>());
		palService.getBulkProductInformations(bulkProductRequest);
	}
	
	@Test
	public void getBulkProductInformations_Not_Authorized() {	
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.UNAUTHORIIZED.getErrorMessage());		
		BulkProductRequest bulkProductRequest = new BulkProductRequest();
		List<String> products = new ArrayList<>();
		products.add("62311c0e36889a40d1a8a1cd");
		bulkProductRequest.setProducts(products);
		bulkProductRequest.setFieldId("productTitle");
		bulkProductRequest.setUserRole("projectManager");
		AccessControlInfo accessInfo = TestUtility.getUserDetails(bulkProductRequest.getUserRole()).getUserRole().getAccessControlInfoList().get(0);
		when(userDetailsService.getAccessControlDetails(Mockito.anyString())).thenReturn(accessInfo);
		palService.getBulkProductInformations(bulkProductRequest);
	}
	
	@Test
	public void getBulkProductInformations_Invalid_Request() {	
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_REQUEST_DATA.getErrorMessage());
		BulkProductRequest bulkProductRequest = new BulkProductRequest();
		List<String> products = new ArrayList<>();
		products.add("62311c0e36889a40d1a8a1cd");
		bulkProductRequest.setProducts(products);
		bulkProductRequest.setFieldId("status");
		palService.getBulkProductInformations(bulkProductRequest);
	}
	
	@Test
	public void getBulkProductInformationsSuccess_Invalid_FieldId() {	
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_REQUEST_DATA.getErrorMessage());		
		BulkProductRequest bulkProductRequest = new BulkProductRequest();
		List<String> products = new ArrayList<>();
		products.add("62311c0e36889a40d1a8a1cd");
		bulkProductRequest.setProducts(products);
		bulkProductRequest.setFieldId("intoDepoDate");
		bulkProductRequest.setUserRole("projectManager");
		palService.getBulkProductInformations(bulkProductRequest);
	}
	
	@Test
	public void getBulkProductInformationsSuccess_Scenario_With_SupplierRole() {
		List<PALProduct> productResponse = new ArrayList<>();
		PALProduct product = new PALProduct();
		product.setId("62311c0e36889a40d1a8a1cd");
		List<DataField> datafields = new ArrayList<>();
		datafields.add(DataField.builder().fieldId(ApplicationConstant.UPC_FIELD).fieldValue("1234567").build());
		datafields.add(DataField.builder().fieldId(ApplicationConstant.SUPPLIER_SITE_CODE_FIELD).fieldValue("F01524").build());
		datafields.add(DataField.builder().fieldId(ApplicationConstant.PRODUCT_TITLE_FIELD).fieldValue("Test").build());
		product.setDatafields(datafields);
		productResponse.add(product);		
		BulkProductRequest bulkProductRequest = new BulkProductRequest();
		List<String> products = new ArrayList<>();
		products.add("62311c0e36889a40d1a8a1cd");
		bulkProductRequest.setProducts(products);
		bulkProductRequest.setFieldId("upc");
		bulkProductRequest.setUserRole("supplier");
		AccessControlInfo accessInfo = TestUtility.getUserDetails(bulkProductRequest.getUserRole()).getUserRole().getAccessControlInfoList().get(0);
		when(userDetailsService.getAccessControlDetails(Mockito.anyString())).thenReturn(accessInfo);
		when(userDetailsService.validateUserDetails(eq(bulkProductRequest.getUserRole()), eq(null)))
		.thenReturn(TestUtility.getUserDetails(bulkProductRequest.getUserRole()));
		when(palDao.findPALProducts(Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(productResponse);
		BulkProductResponse actualResponse = palService.getBulkProductInformations(bulkProductRequest);
		assertNotNull(actualResponse);
		assertEquals(1,actualResponse.getProductResponse().size());
		assertEquals("62311c0e36889a40d1a8a1cd",actualResponse.getProductResponse().get(0).getProductId());
		assertEquals("1234567",actualResponse.getProductResponse().get(0).getOldValue());
		assertEquals("Test",actualResponse.getProductResponse().get(0).getProductName());
		verify(palDao, times(1)).findAllPALFields();
		verify(palDao, times(1)).findPALProducts(Mockito.any(),Mockito.any(),Mockito.any());
		verify(userDetailsService, times(1)).getAccessControlDetails(Mockito.anyString());
	}
	
	@Test
	public void getBulkProductInformation_UnauthorizedAccess_With_Different_SupplierCode() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.UNAUTHORIIZED.getErrorMessage());	
		List<PALProduct> productResponse = new ArrayList<>();
		PALProduct product = new PALProduct();
		product.setId("62311c0e36889a40d1a8a1cd");
		List<DataField> datafields = new ArrayList<>();
		datafields.add(DataField.builder().fieldId(ApplicationConstant.UPC_FIELD).fieldValue("1234567").build());
		datafields.add(DataField.builder().fieldId(ApplicationConstant.SUPPLIER_SITE_CODE_FIELD).fieldValue("FO6529").build());
		datafields.add(DataField.builder().fieldId(ApplicationConstant.PRODUCT_TITLE_FIELD).fieldValue("Test").build());
		product.setDatafields(datafields);
		productResponse.add(product);		
		BulkProductRequest bulkProductRequest = new BulkProductRequest();
		List<String> products = new ArrayList<>();
		products.add("62311c0e36889a40d1a8a1cd");
		bulkProductRequest.setProducts(products);
		bulkProductRequest.setFieldId("upc");
		bulkProductRequest.setUserRole("supplier");
		AccessControlInfo accessInfo = TestUtility.getUserDetails(bulkProductRequest.getUserRole()).getUserRole().getAccessControlInfoList().get(0);
		when(userDetailsService.getAccessControlDetails(Mockito.anyString())).thenReturn(accessInfo);
		when(userDetailsService.validateUserDetails(eq(bulkProductRequest.getUserRole()), eq(null)))
		.thenReturn(TestUtility.getUserDetails(bulkProductRequest.getUserRole()));
		when(palDao.findPALProducts(Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(productResponse);
		palService.getBulkProductInformations(bulkProductRequest);
	}

	@Test
	public void bulkUpdateInformation_MissingUserInRequest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.MISSING_MANDATORY_FIELDS.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/BulkProductUpdate.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		BulkProductUpdateRequest request = (BulkProductUpdateRequest) TestUtility.convertMapToObject((Map<String, Object>) paldetails.getOrDefault("validUpdateRequest", null),
				new TypeReference<BulkProductUpdateRequest>() {
				});
		request.setUser(null);

		palService.bulkUpdateInformation(request);
	}

	@Test
	public void bulkUpdateInformation_MissingProductsInRequest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.MISSING_MANDATORY_FIELDS.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/BulkProductUpdate.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		BulkProductUpdateRequest request = (BulkProductUpdateRequest) TestUtility.convertMapToObject((Map<String, Object>) paldetails.getOrDefault("validUpdateRequest", null),
				new TypeReference<BulkProductUpdateRequest>() {
				});
		request.setProducts(null);

		palService.bulkUpdateInformation(request);
	}

	@Test
	public void bulkUpdateInformation_MissingProductIdInRequest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.MISSING_MANDATORY_FIELDS.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/BulkProductUpdate.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		BulkProductUpdateRequest request = (BulkProductUpdateRequest) TestUtility.convertMapToObject((Map<String, Object>) paldetails.getOrDefault("validUpdateRequest", null),
				new TypeReference<BulkProductUpdateRequest>() {
				});
		request.getProducts().get(0).setProductId(null);

		palService.bulkUpdateInformation(request);
	}

	@Test
	public void bulkUpdateInformation_MissingProductFieldUpdatesInRequest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.MISSING_MANDATORY_FIELDS.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/BulkProductUpdate.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		BulkProductUpdateRequest request = (BulkProductUpdateRequest) TestUtility.convertMapToObject((Map<String, Object>) paldetails.getOrDefault("validUpdateRequest", null),
				new TypeReference<BulkProductUpdateRequest>() {
				});
		request.getProducts().get(0).setFieldUpdates(null);

		palService.bulkUpdateInformation(request);
	}

	@Test
	public void bulkUpdateInformation_MissingProductFieldIdInRequest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.MISSING_MANDATORY_FIELDS.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/BulkProductUpdate.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		BulkProductUpdateRequest request = (BulkProductUpdateRequest) TestUtility.convertMapToObject((Map<String, Object>) paldetails.getOrDefault("validUpdateRequest", null),
				new TypeReference<BulkProductUpdateRequest>() {
				});
		request.getProducts().get(0).getFieldUpdates().get(0).setField(null);

		palService.bulkUpdateInformation(request);
	}

	@Test
	public void bulkUpdateInformation_UnauthorisedUser() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.UNAUTHORIIZED.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/BulkProductUpdate.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		BulkProductUpdateRequest request = (BulkProductUpdateRequest) TestUtility.convertMapToObject((Map<String, Object>) paldetails.getOrDefault("validUpdateRequest", null),
				new TypeReference<BulkProductUpdateRequest>() {
				});
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenThrow(new PALServiceException(ErrorCode.UNAUTHORIIZED));
		palService.bulkUpdateInformation(request);
	}

	@Test
	public void bulkUpdateInformation_InvalidPALFieldUpdate() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_REQUEST_DATA.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/BulkProductUpdate.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		BulkProductUpdateRequest request = (BulkProductUpdateRequest) TestUtility.convertMapToObject((Map<String, Object>) paldetails.getOrDefault("validUpdateRequest", null),
				new TypeReference<BulkProductUpdateRequest>() {
				});
		request.getProducts().get(0).getFieldUpdates().get(0).setField("InvalidField");
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		palService.bulkUpdateInformation(request);
	}

	@Test
	public void bulkUpdateInformation_UnauthorisedUserAccess() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.UNAUTHORIIZED.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/BulkProductUpdate.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		BulkProductUpdateRequest request = (BulkProductUpdateRequest) TestUtility.convertMapToObject((Map<String, Object>) paldetails.getOrDefault("validUpdateRequest", null),
				new TypeReference<BulkProductUpdateRequest>() {
				});
		request.getProducts().get(0).getFieldUpdates().get(0).setField(ApplicationConstant.PRODUCT_TITLE_FIELD);
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		palService.bulkUpdateInformation(request);
	}

	@Test
	public void bulkUpdateInformation_MissingUserRoleInRequest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.MISSING_MANDATORY_FIELDS.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/BulkProductUpdate.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		BulkProductUpdateRequest request = (BulkProductUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("validUpdateRequest", null),
				new TypeReference<BulkProductUpdateRequest>() {});
		request.setUserRole(null);

		palService.bulkUpdateInformation(request);
	}

	@Test
	public void bulkUpdateInformation_ValidProductUpdateRequest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/BulkProductUpdate.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		BulkProductUpdateRequest request = (BulkProductUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("validUpdateRequest", null),
				new TypeReference<BulkProductUpdateRequest>() {});

		Map<String, List<DataField>> upsertProductFields = CommonUtility.convertBulkRequestToMapObject(request);

		PALProduct productbeforeupdate = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("product1", null),
				new TypeReference<PALProduct>() {});
		List<PALProduct> productsBeforeUpdate = new ArrayList<>(Collections.singletonList(productbeforeupdate));

		List<DataField> productFieldUpdates = upsertProductFields.getOrDefault(productbeforeupdate.getId(), new ArrayList<>());
		PALProduct productafterupdate = TestUtility.getPALProductafterFieldUpdate(productbeforeupdate, productFieldUpdates);
		List<PALProduct> productsAfterUpdate = new ArrayList<>(Collections.singletonList(productafterupdate));

		BulkProductUpdateResponse expectedResponse = (BulkProductUpdateResponse) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("validUpdateResponse", null),
				new TypeReference<BulkProductUpdateResponse>() {});

		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(palDao.findPALProducts(any(), any(), any())).thenReturn(productsBeforeUpdate);
		when(palDao.upsertProductFields(any(), any(), any(), any())).thenReturn(productsAfterUpdate);

		BulkProductUpdateResponse actualResponse = palService.bulkUpdateInformation(request);

		assertNotNull(actualResponse);
		assertEquals(expectedResponse, actualResponse);
	}

	@Test
	public void bulkUpdateInformation_InvalidProductUpdateRequest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_PRODUCT_ID.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/BulkProductUpdate.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		BulkProductUpdateRequest request = (BulkProductUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("invalidProductUpdateRequest", null),
				new TypeReference<BulkProductUpdateRequest>() {});

		List<PALProduct> productsBeforeUpdate = new ArrayList<>();

		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(palDao.findPALProducts(any(), any(), any())).thenReturn(productsBeforeUpdate);

		palService.bulkUpdateInformation(request);
	}

	@Test
	public void bulkUpdateInformation_ValidProductNoFieldChangeRequest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/BulkProductUpdate.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		BulkProductUpdateRequest request = (BulkProductUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("noChangeProductUpdateRequest", null),
				new TypeReference<BulkProductUpdateRequest>() {});

		Map<String, List<DataField>> upsertProductFields = CommonUtility.convertBulkRequestToMapObject(request);

		PALProduct productbeforeupdate = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("product1", null),
				new TypeReference<PALProduct>() {});
		List<PALProduct> productsBeforeUpdate = new ArrayList<>(Collections.singletonList(productbeforeupdate));

		List<DataField> productFieldUpdates = upsertProductFields.getOrDefault(productbeforeupdate.getId(), new ArrayList<>());
		PALProduct productafterupdate = TestUtility.getPALProductafterFieldUpdate(productbeforeupdate, productFieldUpdates);
		List<PALProduct> productsAfterUpdate = new ArrayList<>(Collections.singletonList(productafterupdate));

		BulkProductUpdateResponse expectedResponse = (BulkProductUpdateResponse) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("validUpdateResponse", null),
				new TypeReference<BulkProductUpdateResponse>() {});

		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(palDao.findPALProducts(any(), any(), any())).thenReturn(productsBeforeUpdate);
		when(palDao.upsertProductFields(any(), any(), any(), any())).thenReturn(productsAfterUpdate);

		BulkProductUpdateResponse actualResponse = palService.bulkUpdateInformation(request);

		assertNotNull(actualResponse);
		assertEquals(expectedResponse, actualResponse);
	}

	@Test
	public void bulkUpdateInformation_multipleProductUpdateRequest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/BulkProductUpdate.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		BulkProductUpdateRequest request = (BulkProductUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("multipleProductUpdateRequest", null),
				new TypeReference<BulkProductUpdateRequest>() {});

		Map<String, List<DataField>> upsertProductFields = CommonUtility.convertBulkRequestToMapObject(request);

		PALProduct product1beforeupdate = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("product1", null),
				new TypeReference<PALProduct>() {});
		PALProduct product2beforeupdate = TestUtility.getPALProductafterFieldUpdate(product1beforeupdate, new ArrayList<>());
		product2beforeupdate.setId("7238a5ca33b8296bae3a3f38");
		PALProduct product3beforeupdate = TestUtility.getPALProductafterFieldUpdate(product1beforeupdate, new ArrayList<>());
		product3beforeupdate.setId("8238a5ca33b8296bae3a3f38");

		List<PALProduct> productsBeforeUpdate = new ArrayList<>(Arrays.asList(product1beforeupdate, product2beforeupdate, product3beforeupdate));

		List<DataField> productFieldUpdates = upsertProductFields.getOrDefault(product2beforeupdate.getId(), new ArrayList<>());
		PALProduct productafterupdate = TestUtility.getPALProductafterFieldUpdate(product2beforeupdate, productFieldUpdates);
		List<PALProduct> productsAfterUpdate = new ArrayList<>(Arrays.asList(product1beforeupdate, productafterupdate, product3beforeupdate));

		BulkProductUpdateResponse expectedResponse = (BulkProductUpdateResponse) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("multipleProductUpdateResponse", null),
				new TypeReference<BulkProductUpdateResponse>() {});

		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(palDao.findPALProducts(any(), any(), any())).thenReturn(productsBeforeUpdate);
		when(palDao.upsertProductFields(any(), any(), any(), any())).thenReturn(productsAfterUpdate);

		BulkProductUpdateResponse actualResponse = palService.bulkUpdateInformation(request);

		assertNotNull(actualResponse);
		assertEquals(expectedResponse, actualResponse);
	}

	@Test
	public void bulkUpdateInformation_deleteFieldUpdateRequest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/BulkProductUpdate.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		BulkProductUpdateRequest request = (BulkProductUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("deleteFieldUpdateRequest", null),
				new TypeReference<BulkProductUpdateRequest>() {});

		Map<String, List<DataField>> upsertProductFields = CommonUtility.convertBulkRequestToMapObject(request);

		PALProduct productbeforeupdate = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("product1", null),
				new TypeReference<PALProduct>() {});
		List<PALProduct> productsBeforeUpdate = new ArrayList<>(Collections.singletonList(productbeforeupdate));

		List<DataField> productFieldUpdates = upsertProductFields.getOrDefault(productbeforeupdate.getId(), new ArrayList<>());
		PALProduct productafterupdate = TestUtility.getPALProductafterFieldUpdate(productbeforeupdate, productFieldUpdates);
		List<PALProduct> productsAfterUpdate = new ArrayList<>(Collections.singletonList(productafterupdate));

		BulkProductUpdateResponse expectedResponse = (BulkProductUpdateResponse) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("validUpdateResponse", null),
				new TypeReference<BulkProductUpdateResponse>() {});

		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(palDao.findPALProducts(any(), any(), any())).thenReturn(productsBeforeUpdate);
		when(palDao.upsertProductFields(any(), any(), any(), any())).thenReturn(productsAfterUpdate);

		BulkProductUpdateResponse actualResponse = palService.bulkUpdateInformation(request);

		assertNotNull(actualResponse);
		assertEquals(expectedResponse, actualResponse);
	}

	@Test
	public void bulkUpdateInformation_autocalculateFieldUpdateRequest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/BulkProductUpdate.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		BulkProductUpdateRequest request = (BulkProductUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("autocalculateFieldUpdateRequest", null),
				new TypeReference<BulkProductUpdateRequest>() {});

		Map<String, List<DataField>> upsertProductFields = CommonUtility.convertBulkRequestToMapObject(request);

		PALProduct productbeforeupdate = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("product1", null),
				new TypeReference<PALProduct>() {});
		List<PALProduct> productsBeforeUpdate = new ArrayList<>(Collections.singletonList(productbeforeupdate));

		List<DataField> productFieldUpdates = upsertProductFields.getOrDefault(productbeforeupdate.getId(), new ArrayList<>());
		productFieldUpdates.add(DataField.builder().fieldId(ApplicationConstant.SELLING_PRICE_FIELD).fieldValue("5").build());
		productFieldUpdates.add(DataField.builder().fieldId(ApplicationConstant.TRAY_SELLING_VALUE_FIELD).fieldValue("25").build());
		PALProduct productafterupdate = TestUtility.getPALProductafterFieldUpdate(productbeforeupdate, productFieldUpdates);
		List<PALProduct> productsAfterUpdate = new ArrayList<>(Collections.singletonList(productafterupdate));

		BulkProductUpdateResponse expectedResponse = (BulkProductUpdateResponse) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("validUpdateResponse", null),
				new TypeReference<BulkProductUpdateResponse>() {});

		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(palDao.findPALProducts(any(), any(), any())).thenReturn(productsBeforeUpdate);
		when(palDao.upsertProductFields(any(), any(), any(), any())).thenReturn(productsAfterUpdate);

		BulkProductUpdateResponse actualResponse = palService.bulkUpdateInformation(request);
		verify(palDao, Mockito.times(1)).upsertProductFields(Mockito.any(),
				upsertProductCaptor.capture(),Mockito.any(),Mockito.any());
		Map<String, List<DataField>> productDataFieldUpdates = upsertProductCaptor.getValue();
		assertNotNull(productDataFieldUpdates);
		assertEquals(ApplicationConstant.SELLING_PRICE_FIELD, productDataFieldUpdates.get("6238a5ca33b8296bae3a3f38").get(0).getFieldId());
		assertEquals("5", productDataFieldUpdates.get("6238a5ca33b8296bae3a3f38").get(0).getFieldValue());
		assertEquals(ApplicationConstant.TRAY_SELLING_VALUE_FIELD, productDataFieldUpdates.get("6238a5ca33b8296bae3a3f38").get(1).getFieldId());
		assertEquals("25.0", productDataFieldUpdates.get("6238a5ca33b8296bae3a3f38").get(1).getFieldValue());
		assertNotNull(actualResponse);
		assertEquals(expectedResponse, actualResponse);
	}

	@Test
	public void bulkUpdateInformation_statusUpdateNotificationRequest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/BulkProductUpdate.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		BulkProductUpdateRequest request = (BulkProductUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("statusUpdateNotificationRequest", null),
				new TypeReference<BulkProductUpdateRequest>() {});

		Map<String, List<DataField>> upsertProductFields = CommonUtility.convertBulkRequestToMapObject(request);

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("project", null),
				new TypeReference<PALProject>() {});
		List<PALProject> palProjects = new ArrayList<>(Collections.singletonList(palProject));

		PALProduct productbeforeupdate = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("product1", null),
				new TypeReference<PALProduct>() {});
		List<PALProduct> productsBeforeUpdate = new ArrayList<>(Collections.singletonList(productbeforeupdate));

		List<DataField> productFieldUpdates = upsertProductFields.getOrDefault(productbeforeupdate.getId(), new ArrayList<>());
		PALProduct productafterupdate = TestUtility.getPALProductafterFieldUpdate(productbeforeupdate, productFieldUpdates);
		List<PALProduct> productsAfterUpdate = new ArrayList<>(Collections.singletonList(productafterupdate));

		BulkProductUpdateResponse expectedResponse = (BulkProductUpdateResponse) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("validUpdateResponse", null),
				new TypeReference<BulkProductUpdateResponse>() {});

		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(palDao.findPALProducts(any(), any(), any())).thenReturn(productsBeforeUpdate);
		when(palDao.upsertProductFields(any(), any(), any(), any())).thenReturn(productsAfterUpdate);
		when(palDao.findPALProjectList(any(),any())).thenReturn(palProjects);

		BulkProductUpdateResponse actualResponse = palService.bulkUpdateInformation(request);
		verify(notificationService, times(1)).sendEmailMessage(any(),any(),eq(MessageTemplate.UPDATE_PRODUCT_STATUS));
		assertNotNull(actualResponse);
		assertEquals(expectedResponse, actualResponse);
	}

	@Test
	public void bulkUpdateInformation_statusUpdateNoNotificationRequest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/BulkProductUpdate.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		BulkProductUpdateRequest request = (BulkProductUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("statusUpdateNoNotificationRequest", null),
				new TypeReference<BulkProductUpdateRequest>() {});

		Map<String, List<DataField>> upsertProductFields = CommonUtility.convertBulkRequestToMapObject(request);

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("project", null),
				new TypeReference<PALProject>() {});

		PALProduct productbeforeupdate = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("product1", null),
				new TypeReference<PALProduct>() {});
		List<PALProduct> productsBeforeUpdate = new ArrayList<>(Collections.singletonList(productbeforeupdate));

		List<DataField> productFieldUpdates = upsertProductFields.getOrDefault(productbeforeupdate.getId(), new ArrayList<>());
		PALProduct productafterupdate = TestUtility.getPALProductafterFieldUpdate(productbeforeupdate, productFieldUpdates);
		List<PALProduct> productsAfterUpdate = new ArrayList<>(Collections.singletonList(productafterupdate));

		BulkProductUpdateResponse expectedResponse = (BulkProductUpdateResponse) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("validUpdateResponse", null),
				new TypeReference<BulkProductUpdateResponse>() {});

		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(palDao.findPALProducts(any(), any(), any())).thenReturn(productsBeforeUpdate);
		when(palDao.upsertProductFields(any(), any(), any(), any())).thenReturn(productsAfterUpdate);

		BulkProductUpdateResponse actualResponse = palService.bulkUpdateInformation(request);
		verify(notificationService, never()).sendEmailMessage(any(),any(),eq(MessageTemplate.UPDATE_PRODUCT_STATUS));
		assertNotNull(actualResponse);
		assertEquals(expectedResponse, actualResponse);
	}

	@Test
	public void bulkUpdateInformation_statusUpdateMultipleNotificationRequest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/BulkProductUpdate.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		BulkProductUpdateRequest request = (BulkProductUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("statusUpdateMultipleNotificationRequest", null),
				new TypeReference<BulkProductUpdateRequest>() {});

		Map<String, List<DataField>> upsertProductFields = CommonUtility.convertBulkRequestToMapObject(request);

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("project", null),
				new TypeReference<PALProject>() {});
		List<PALProject> palProjects = new ArrayList<>(Collections.singletonList(palProject));

		PALProduct product1beforeupdate = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("product1", null),
				new TypeReference<PALProduct>() {});
		PALProduct product2beforeupdate = TestUtility.getPALProductafterFieldUpdate(product1beforeupdate, new ArrayList<>());
		product2beforeupdate.setId("7238a5ca33b8296bae3a3f38");
		List<PALProduct> productsBeforeUpdate = new ArrayList<>(Arrays.asList(product1beforeupdate, product2beforeupdate));

		List<DataField> productFieldUpdates = upsertProductFields.getOrDefault(product2beforeupdate.getId(), new ArrayList<>());
		PALProduct product1afterupdate = TestUtility.getPALProductafterFieldUpdate(product1beforeupdate, productFieldUpdates);
		PALProduct product2afterupdate = TestUtility.getPALProductafterFieldUpdate(product2beforeupdate, productFieldUpdates);
		List<PALProduct> productsAfterUpdate = new ArrayList<>(Arrays.asList(product1afterupdate, product2afterupdate));

		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(palDao.findPALProducts(any(), any(), any())).thenReturn(productsBeforeUpdate);
		when(palDao.upsertProductFields(any(), any(), any(), any())).thenReturn(productsAfterUpdate);
		when(palDao.findPALProjectList(any(), any())).thenReturn(palProjects);

		BulkProductUpdateResponse actualResponse = palService.bulkUpdateInformation(request);
		verify(notificationService, times(2)).sendEmailMessage(any(),any(),eq(MessageTemplate.UPDATE_PRODUCT_STATUS));
		assertNotNull(actualResponse);
	}

	@Test
	public void updatePalConfigTest() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		Category esCategoryResponse = mapper.readValue(new File("src/test/resources/UserListResponse/ESProductHeirarchyResponse.json"),
				mapper.getTypeFactory().constructType(Category.class));
		when(esProductHierarchyServiceRestClient.getCategories())
				.thenReturn(esCategoryResponse);
		Map<String, String> response = palService.updatePALConfigs();
		verify(palDao, times(1)).savePALConfiguration(palConfigurationCaptor.capture());
		assertEquals(ApplicationConstant.SUCCESS, response.get(ApplicationConstant.CATEGORY_CONFIG_ID));
		assertEquals(ApplicationConstant.FAILED, response.get("INVALID"));
		assertNotNull(palConfigurationCaptor.getValue());
		assertEquals(73, palConfigurationCaptor.getValue().getValues().size());
	}

	@Test
	public void getPALConfigurationsEmptyRequestTest(){
		List<String> palConfigIds = new ArrayList<>();
		List<PALConfiguration> actualResponse = palService.getPALConfigurations(palConfigIds);
		List<PALConfiguration> expectedResponse = palDao.findALLPALConfiguration();
		assertEquals(expectedResponse,actualResponse);
	}

	@Test
	public void updatePalConfigNullESResponseTest() {
		when(esProductHierarchyServiceRestClient.getCategories())
				.thenReturn(null);
		Map<String, String> response = palService.updatePALConfigs();
		assertEquals(ApplicationConstant.FAILED, response.get(ApplicationConstant.CATEGORY_CONFIG_ID));
	}

	@Test
	public void updatePalConfigESExceptionTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.ES_ERROR.getErrorMessage());

		when(esProductHierarchyServiceRestClient.getCategories())
				.thenThrow(new PALServiceException(ErrorCode.ES_ERROR));
		palService.updatePALConfigs();
	}

	@Test
	public void deleteProjectOrProducts_missingUserRoleTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.MISSING_MANDATORY_FIELDS.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/DeleteProjectProduct.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALDeleteRequest request = (PALDeleteRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("deleteProductRequest", null),
				new TypeReference<PALDeleteRequest>() {});
		request.setUserRole(null);
		palService.deleteProjectOrProducts(request);
	}

	@Test
	public void deleteProjectOrProducts_missingUserTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.MISSING_MANDATORY_FIELDS.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/DeleteProjectProduct.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALDeleteRequest request = (PALDeleteRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("deleteProductRequest", null),
				new TypeReference<PALDeleteRequest>() {});
		request.setUser(null);
		palService.deleteProjectOrProducts(request);
	}

	@Test
	public void deleteProjectOrProducts_missingProjectIdTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.MISSING_MANDATORY_FIELDS.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/DeleteProjectProduct.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALDeleteRequest request = (PALDeleteRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("deleteProductRequest", null),
				new TypeReference<PALDeleteRequest>() {});
		request.setProjectId(null);
		palService.deleteProjectOrProducts(request);
	}

	@Test
	public void deleteProjectOrProducts_unauthorisedRoleTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.UNAUTHORIIZED.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/DeleteProjectProduct.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALDeleteRequest request = (PALDeleteRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("deleteProductRequest", null),
				new TypeReference<PALDeleteRequest>() {});
		request.setUserRole("buyer");

		when(userDetailsService.validateUserDetails(eq(request.getUserRole()),any()))
				.thenThrow(new PALServiceException(ErrorCode.UNAUTHORIIZED));
		palService.deleteProjectOrProducts(request);
	}

	@Test
	public void deleteProjectOrProducts_deleteProductRequestTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/DeleteProjectProduct.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALDeleteRequest request = (PALDeleteRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("deleteProductRequest", null),
				new TypeReference<PALDeleteRequest>() {});
		List<DataField> dataFields = new ArrayList<>(Collections.singletonList(DataField.builder().fieldId(ApplicationConstant.STATUS_FIELD).fieldValue(Status.DELETED.getStatus()).build()));
		PALProduct product = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palProduct1", null),
				new TypeReference<PALProduct>() {});
		PALProduct productafterupdate = TestUtility.getPALProductafterFieldUpdate(product,dataFields);

		when(userDetailsService.validateUserDetails(eq(request.getUserRole()),any()))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(palDao.findPALProducts(any(), any(), any())).thenReturn(new ArrayList<>(Collections.singletonList(product)));
		when(palDao.upsertProductFields(any(), any(), any(), any())).thenReturn(new ArrayList<>(Collections.singletonList(productafterupdate)));

		PALDeleteResponse response = palService.deleteProjectOrProducts(request);
		verify(userDetailsService, atLeastOnce()).validateUserDetails(any(), palaccessibleRolesCaptor.capture());
		assertNotNull(palaccessibleRolesCaptor.getAllValues());
		List<String> accessibleUsers = palaccessibleRolesCaptor.getAllValues().get(0);
		assertEquals(2, accessibleUsers.size());
		assertTrue(accessibleUsers.contains(ApplicationConstant.PROJECT_MANAGER));
		assertTrue(accessibleUsers.contains(ApplicationConstant.PRODUCT_DEVELOPER));

		verify(palDao, times(1)).upsertProductFields(any(), upsertProductCaptor.capture(), any(), any());
		assertEquals(1, upsertProductCaptor.getValue().keySet().size());
		assertNotNull(upsertProductCaptor.getValue());
		assertTrue(upsertProductCaptor.getValue().keySet().contains(product.getId()));

		assertNotNull(response);
		assertNull(response.getProjectId());
		assertNull(response.getFailedProducts());
		assertNotNull(response.getSuccessProducts());
		assertEquals(product.getId(), response.getSuccessProducts().get(0).getProductId());
	}

	@Test
	public void deleteProjectOrProducts_deleteProductsRequestTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/DeleteProjectProduct.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALDeleteRequest request = (PALDeleteRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("deleteProductsRequest", null),
				new TypeReference<PALDeleteRequest>() {});
		List<DataField> dataFields = new ArrayList<>(Collections.singletonList(DataField.builder().fieldId(ApplicationConstant.STATUS_FIELD).fieldValue(Status.DELETED.getStatus()).build()));
		PALProduct product1 = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palProduct1", null),
				new TypeReference<PALProduct>() {});
		PALProduct product1afterupdate = TestUtility.getPALProductafterFieldUpdate(product1,dataFields);

		PALProduct product2 = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palProduct2", null),
				new TypeReference<PALProduct>() {});
		PALProduct product2afterupdate = TestUtility.getPALProductafterFieldUpdate(product2,dataFields);

		when(userDetailsService.validateUserDetails(eq(request.getUserRole()),any()))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(palDao.findPALProducts(any(), any(), any())).thenReturn(new ArrayList<>(Arrays.asList(product1, product2)));
		when(palDao.upsertProductFields(any(), any(), any(), any())).thenReturn(new ArrayList<>(Arrays.asList(product1afterupdate, product2afterupdate)));

		PALDeleteResponse response = palService.deleteProjectOrProducts(request);
		verify(userDetailsService, atLeastOnce()).validateUserDetails(any(), palaccessibleRolesCaptor.capture());
		assertNotNull(palaccessibleRolesCaptor.getAllValues());
		List<String> accessibleUsers = palaccessibleRolesCaptor.getAllValues().get(0);
		assertEquals(2, accessibleUsers.size());
		assertTrue(accessibleUsers.contains(ApplicationConstant.PROJECT_MANAGER));
		assertTrue(accessibleUsers.contains(ApplicationConstant.PRODUCT_DEVELOPER));

		verify(palDao, times(1)).upsertProductFields(any(), upsertProductCaptor.capture(), any(), any());
		assertNotNull(upsertProductCaptor.getValue());
		assertEquals(2, upsertProductCaptor.getValue().keySet().size());
		assertTrue(upsertProductCaptor.getValue().keySet().contains(product1.getId()));
		assertTrue(upsertProductCaptor.getValue().keySet().contains(product2.getId()));

		assertNotNull(response);
		assertNull(response.getProjectId());
		assertNull(response.getFailedProducts());
		assertNotNull(response.getSuccessProducts());
		assertTrue(response.getSuccessProducts().stream().map(ProductUpdateStatus::getProductId).collect(Collectors.toList()).contains(product1.getId()));
		assertTrue(response.getSuccessProducts().stream().map(ProductUpdateStatus::getProductId).collect(Collectors.toList()).contains(product2.getId()));
	}

	@Test
	public void deleteProjectOrProducts_deleteProjectRequestTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/DeleteProjectProduct.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALDeleteRequest request = (PALDeleteRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("deleteProjectRequest", null),
				new TypeReference<PALDeleteRequest>() {});
		PALProject projectafterupdate = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palProject", null),
				new TypeReference<PALProject>() {});

		List<DataField> dataFields = new ArrayList<>(Collections.singletonList(DataField.builder().fieldId(ApplicationConstant.STATUS_FIELD).fieldValue(Status.DELETED.getStatus()).build()));
		PALProduct product1 = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palProduct1", null),
				new TypeReference<PALProduct>() {});
		PALProduct product1afterupdate = TestUtility.getPALProductafterFieldUpdate(product1,dataFields);

		PALProduct product2 = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palProduct2", null),
				new TypeReference<PALProduct>() {});
		PALProduct product2afterupdate = TestUtility.getPALProductafterFieldUpdate(product2,dataFields);

		when(userDetailsService.validateUserDetails(eq(request.getUserRole()),any()))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(palDao.findPALProducts(any(), any(), any())).thenReturn(new ArrayList<>(Arrays.asList(product1, product2)));
		when(palDao.upsertProductFields(any(), any(), any(), any())).thenReturn(new ArrayList<>(Arrays.asList(product1afterupdate, product2afterupdate)));
		when(palDao.updatePALProject(any())).thenReturn(projectafterupdate);

		PALDeleteResponse response = palService.deleteProjectOrProducts(request);
		verify(userDetailsService, atLeastOnce()).validateUserDetails(any(), palaccessibleRolesCaptor.capture());
		assertNotNull(palaccessibleRolesCaptor.getAllValues());
		List<String> accessibleUsers = palaccessibleRolesCaptor.getAllValues().get(0);
		assertEquals(1, accessibleUsers.size());
		assertTrue(accessibleUsers.contains(ApplicationConstant.PROJECT_MANAGER));

		verify(palDao, times(1)).updatePALProject(palProjectCaptor.capture());
		assertNotNull(palProjectCaptor.getValue());
		assertEquals(projectafterupdate.getStatus(), palProjectCaptor.getValue().getStatus());
		assertEquals(projectafterupdate.getId(), palProjectCaptor.getValue().getId());
		assertNotNull(palProjectCaptor.getValue().getDeletedDate());

		verify(palDao, times(1)).upsertProductFields(any(), upsertProductCaptor.capture(), any(), any());
		assertNotNull(upsertProductCaptor.getValue());
		assertEquals(2, upsertProductCaptor.getValue().keySet().size());
		assertTrue(upsertProductCaptor.getValue().keySet().contains(product1.getId()));
		assertTrue(upsertProductCaptor.getValue().keySet().contains(product2.getId()));

		assertNotNull(response);
		assertEquals(projectafterupdate.getId(), response.getProjectId());
		assertEquals(projectafterupdate.getStatus(), Status.DELETED.getStatus());
		assertNull(response.getFailedProducts());
		assertNotNull(response.getSuccessProducts());
		assertTrue(response.getSuccessProducts().stream().map(ProductUpdateStatus::getProductId).collect(Collectors.toList()).contains(product1.getId()));
		assertTrue(response.getSuccessProducts().stream().map(ProductUpdateStatus::getProductId).collect(Collectors.toList()).contains(product2.getId()));
	}

	@Test
	public void deleteProjectOrProducts_InvalidProductRequestTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_PRODUCT_ID.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/DeleteProjectProduct.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALDeleteRequest request = (PALDeleteRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("deleteProductRequest", null),
				new TypeReference<PALDeleteRequest>() {});
		List<DataField> dataFields = new ArrayList<>(Collections.singletonList(DataField.builder().fieldId(ApplicationConstant.STATUS_FIELD).fieldValue(Status.DELETED.getStatus()).build()));
		PALProduct product = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palProduct1", null),
				new TypeReference<PALProduct>() {});
		product.setId("1234");

		when(userDetailsService.validateUserDetails(eq(request.getUserRole()),any()))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(palDao.findPALProducts(any(), any(), any())).thenReturn(new ArrayList<>(Collections.singletonList(product)));

		palService.deleteProjectOrProducts(request);
	}

	@Test
	public void deleteProjectOrProducts_invalidProjectRequestTest() {
		expectedException.expect(PALServiceException.class);
		expectedException.expectMessage(ErrorCode.INVALID_PROJECT_ID.getErrorMessage());

		String fileName = "src/test/resources/ProductAttributeListingResponse/DeleteProjectProduct.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALDeleteRequest request = (PALDeleteRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("deleteProjectRequest", null),
				new TypeReference<PALDeleteRequest>() {});

		when(userDetailsService.validateUserDetails(eq(request.getUserRole()),any()))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(palDao.updatePALProject(any())).thenReturn(null);

		palService.deleteProjectOrProducts(request);
	}
	
	@Test
	public void getPalProductInformationTestFor_WinesTemplate() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductInformationWinesTemplateTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProductRequest request = (PALProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProductRequest>() {});

		PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct", null),
				new TypeReference<PALProduct>() {});

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		when(palDao.findPALProductById(Mockito.anyString())).thenReturn(palProduct);
		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(userDetailsService.getAccessControlDetails(eq(request.getUserRole())))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()).getUserRole().getAccessControlInfoList().get(0));
		PALProductResponse actualResponse = palService.getPALProductInformation(request);
		assertNotNull(actualResponse);
		validatePALProductResponse(request, "Wines", palProduct, actualResponse);
	}
	
	@Test
	public void getPalProductInformationTestFor_ISBTemplate() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductInformationISBTemplateTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProductRequest request = (PALProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProductRequest>() {});

		PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct", null),
				new TypeReference<PALProduct>() {});

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		when(palDao.findPALProductById(Mockito.anyString())).thenReturn(palProduct);
		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(userDetailsService.getAccessControlDetails(eq(request.getUserRole())))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()).getUserRole().getAccessControlInfoList().get(0));
		PALProductResponse actualResponse = palService.getPALProductInformation(request);
		assertNotNull(actualResponse);
		validatePALProductResponse(request, "ISB", palProduct, actualResponse);
	}
	
	@Test
	public void getPalProductInformationTestFor_HospitalityTemplate() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductInformationHospitalityTemplateTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);


		PALProductRequest request = (PALProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProductRequest>() {});

		PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct", null),
				new TypeReference<PALProduct>() {});

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		when(palDao.findPALProductById(Mockito.anyString())).thenReturn(palProduct);
		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(userDetailsService.getAccessControlDetails(eq(request.getUserRole())))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()).getUserRole().getAccessControlInfoList().get(0));
		PALProductResponse actualResponse = palService.getPALProductInformation(request);
		assertNotNull(actualResponse);
		validatePALProductResponse(request, "Hospitality", palProduct, actualResponse);
	}
	
	@Test
	public void getPalProductInformationTestFor_HortiComTemplate() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductInformationHorticomTemplateTestDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProductRequest request = (PALProductRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<PALProductRequest>() {});

		PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct", null),
				new TypeReference<PALProduct>() {});

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});

		when(palDao.findPALProductById(Mockito.anyString())).thenReturn(palProduct);
		when(palDao.findPALProjectById(Mockito.anyString())).thenReturn(palProject);
		when(userDetailsService.validateUserDetails(eq(request.getUserRole()), eq(null)))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		when(userDetailsService.getAccessControlDetails(eq(request.getUserRole())))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()).getUserRole().getAccessControlInfoList().get(0));
		PALProductResponse actualResponse = palService.getPALProductInformation(request);
		assertNotNull(actualResponse);
		validatePALProductResponse(request, "HORTI.COM", palProduct, actualResponse);
	}
  
	@Test
	public void getPalProjectList_requestValidUserFilterTest() {
		String fileName = "src/test/resources/ProductAttributeListingResponse/projectFilterDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		ProjectFilter request = (ProjectFilter) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("requestValidUserFilter", null),
				new TypeReference<ProjectFilter>() {});
		AppResponse<List<PALProjectResponse>> appresponse = (AppResponse<List<PALProjectResponse>>) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("responseFromDate", null),
				new TypeReference<AppResponse<List<PALProjectResponse>>>() {});
		List<PALProjectResponse> expectedResponse = appresponse.getData();
		List<PALProject> palProjects = new ArrayList<>();
		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});
		palProjects.add(palProject);
		when(palDao.findPALProjectList(Mockito.any(),Mockito.any())).thenReturn((palProjects));
		when(userDetailsService.validateUserDetails(request.getUserRole(), null))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		List<PALProjectResponse> actualResponse = palService.getPALProjectList(request);
		assertEquals(expectedResponse.get(0).getInformation().getProjectName(), actualResponse.get(0).getInformation().getProjectName());
	}

	@Test
	public void getPalProjectList_requestInValidUserFilterTest() {
		String fileName = "src/test/resources/ProductAttributeListingResponse/projectFilterDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		ProjectFilter request = (ProjectFilter) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("requestInValidUserFilter", null),
				new TypeReference<ProjectFilter>() {});
		List<PALProject> palProjects = new ArrayList<>();
		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});
		palProjects.add(palProject);
		when(palDao.findPALProjectList(Mockito.any(),Mockito.any())).thenReturn((palProjects));
		when(userDetailsService.validateUserDetails(request.getUserRole(), null))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		List<PALProjectResponse> actualResponse = palService.getPALProjectList(request);
		assertTrue(CollectionUtils.isEmpty(actualResponse));
	}

	@Test
	public void getPalProjectList_requestMultipleUserFilterTest() {
		String fileName = "src/test/resources/ProductAttributeListingResponse/projectFilterDetails.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
		};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		ProjectFilter request = (ProjectFilter) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("requestMultipleUserFilter", null),
				new TypeReference<ProjectFilter>() {});
		AppResponse<List<PALProjectResponse>> appresponse = (AppResponse<List<PALProjectResponse>>) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("responseFromDate", null),
				new TypeReference<AppResponse<List<PALProjectResponse>>>() {});
		List<PALProjectResponse> expectedResponse = appresponse.getData();

		List<PALProject> palProjects = new ArrayList<>();
		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});
		palProjects.add(palProject);
		when(palDao.findPALProjectList(Mockito.any(),Mockito.any())).thenReturn((palProjects));
		when(userDetailsService.validateUserDetails(request.getUserRole(), null))
				.thenReturn(TestUtility.getUserDetails(request.getUserRole()));
		List<PALProjectResponse> actualResponse = palService.getPALProjectList(request);
		assertEquals(1, actualResponse.size());
		assertEquals(expectedResponse.get(0).getInformation().getProjectName(), actualResponse.get(0).getInformation().getProjectName());
	}
}
