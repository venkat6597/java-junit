package com.marksandspencer.foodshub.pal.repository.custom;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marksandspencer.foodshub.pal.constant.ApplicationConstant;
import com.marksandspencer.foodshub.pal.dao.ProductAttributeListingDao;
import com.marksandspencer.foodshub.pal.domain.*;
import com.marksandspencer.foodshub.pal.repository.custom.impl.PALCustomRepositoryImpl;
import com.marksandspencer.foodshub.pal.transfer.BulkProductUpdateRequest;
import com.marksandspencer.foodshub.pal.transfer.Filter;
import com.marksandspencer.foodshub.pal.transfer.ProjectFilter;
import com.marksandspencer.foodshub.pal.util.CommonUtility;
import com.marksandspencer.foodshub.pal.utility.TestUtility;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.query.Query;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PALCustomRepositoryTest {

	@InjectMocks
	PALCustomRepository palCustomRepository = new PALCustomRepositoryImpl();

	@Mock
	MongoTemplate mongoTemplate;

	@Captor
	ArgumentCaptor<Query> queryCaptor;

	@Mock
	ProductAttributeListingDao palDao;

	private String palProductCollection = "PALProduct";
	private String palAuditLogCollection = "PALAuditLogs";
	private String palProjectCollection = "PALProject";

	@Before
	public void setup() throws IOException {
		when(mongoTemplate.getCollectionName(eq(PALProduct.class))).thenReturn(palProductCollection);
		when(mongoTemplate.getCollectionName(eq(PALAuditLog.class))).thenReturn(palAuditLogCollection);
		when(mongoTemplate.getCollectionName(eq(PALProject.class))).thenReturn(palProjectCollection);
		when(mongoTemplate.getConverter()).thenReturn(Mockito.mock(MappingMongoConverter.class));
		when(mongoTemplate.bulkOps(any(BulkOperations.BulkMode.class),anyString())).thenReturn(Mockito.mock(BulkOperations.class));

		ObjectMapper mapper = new ObjectMapper();

		List<PALFields> palFields = mapper.readValue(new File("src/test/resources/ProductAttributeListingResponse/db_PALFields.json"),
				new TypeReference<>() {});
		when(palDao.findAllPALFields()).thenReturn(palFields);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void findByProductIdAndDataFieldFiltersTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductFilterListTest.json";

		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		Filter request = (Filter) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<Filter>() {});
		List<String> status =new ArrayList<>();
		status.add("Inprogress");
		request.setStatus(status);
		List<String> type =new ArrayList<>();
		type.add("Parent");
		request.setType(type);
		request.setParent("Parent");
		request.setOcadoProduct(true);
		request.setInternationalProduct(true);
		request.setIntoDepotDate("25/09/2021");
		request.setLaunchByDate("P06 SEP 2020");
		request.setSearchText("Sum");
		List<String> suppliers = new ArrayList<String>();
		suppliers.add("Supplier 1234");
		request.setSuppliers(suppliers);

		String projectId = "60ff64a1476c45bf24a684d4";

		PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct1", null),
				new TypeReference<PALProduct>() {});
		List<Object> palProducts = new ArrayList<>();
		palProducts.add(palProduct);


		PALProduct response =  (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("response", null),
				new TypeReference<PALProduct>() {});
		List<PALProduct> expectedResponse = new ArrayList<>();
		expectedResponse.add(response);
		when(mongoTemplate.find(Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(palProducts);

		List<PALProduct> actualResponse = palCustomRepository.findProductByFilterCondition(projectId, request);
		assertEquals(1, actualResponse.size());
		assertEquals("Inprogress",actualResponse.get(0).getDatafields().get(4).getFieldValue());
		assertEquals("Supplier 1234",actualResponse.get(0).getDatafields().get(17).getFieldValue());
		assertEquals(expectedResponse, actualResponse);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void findByProductIdAndDataFieldNullFiltersTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductFilterListTest.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		Filter request= (Filter) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("requestNull", null),
				new TypeReference<Filter>() {});
		String projectId = "60ff64a1476c45bf24a684d4";

		PALProduct palProduct1 = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct1", null),
				new TypeReference<PALProduct>() {});
		PALProduct palProduct2 = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct2", null),
				new TypeReference<PALProduct>() {});
		List<Object> palProducts = new ArrayList<>();
		palProducts.add(palProduct1);
		palProducts.add(palProduct2);

		when(mongoTemplate.find(Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(palProducts);

		List<PALProduct> actualResponse = palCustomRepository.findProductByFilterCondition(projectId, request);
		assertEquals(2, actualResponse.size());

	}

	@SuppressWarnings("unchecked")
	@Test
	public void findByProductIdAndDataFieldFiltersCombinationTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductFilterListTest.json";

		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		Filter request = (Filter) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<Filter>() {});
		List<String> status = new ArrayList<>();
		status.add("No Go");
		request.setStatus(status);
		List<String> type = new ArrayList<>();
		type.add("Existing");
		request.setType(type);
		List<String> suppliers = new ArrayList<String>();
		suppliers.add("Supplier 5678");
		request.setSuppliers(suppliers);

		String projectId = "60ff64a1476c45bf24a684d4";

		PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct2", null),
				new TypeReference<PALProduct>() {});
		List<Object> palProducts = new ArrayList<>();
		palProducts.add(palProduct);


		PALProduct response =  (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("response2", null),
				new TypeReference<PALProduct>() {});
		List<PALProduct> expectedResponse = new ArrayList<>();
		expectedResponse.add(response);

		when(mongoTemplate.find(Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(palProducts);

		List<PALProduct> actualResponse = palCustomRepository.findProductByFilterCondition(projectId, request);
		assertEquals(1, actualResponse.size());
		assertEquals("No Go",actualResponse.get(0).getDatafields().get(4).getFieldValue());
		assertEquals("Supplier 5678",actualResponse.get(0).getDatafields().get(17).getFieldValue());
		assertEquals("Existing",actualResponse.get(0).getDatafields().get(22).getFieldValue());
		assertEquals(expectedResponse, actualResponse);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void findByProductIdAndDataFieldFiltersSecondCombinationTest() {

		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductFilterListTest.json";

		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		Filter request = (Filter) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<Filter>() {});
		request.setIntoDepotDate("01/02/2022");
		request.setLaunchByDate("P10 JAN 2021");


		String projectId = "60ff64a1476c45bf24a684d4";

		PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproduct2", null),
				new TypeReference<PALProduct>() {});
		List<Object> palProducts = new ArrayList<>();
		palProducts.add(palProduct);


		PALProduct response =  (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("response2", null),
				new TypeReference<PALProduct>() {});
		List<PALProduct> expectedResponse = new ArrayList<>();
		expectedResponse.add(response);

		when(mongoTemplate.find(Mockito.any(),Mockito.any(),Mockito.any())).thenReturn(palProducts);

		List<PALProduct> actualResponse = palCustomRepository.findProductByFilterCondition(projectId, request);
		assertEquals(1, actualResponse.size());
		assertEquals("P10 JAN 2021",actualResponse.get(0).getDatafields().get(1).getFieldValue());
		assertEquals("01/02/2022",actualResponse.get(0).getDatafields().get(2).getFieldValue());
		assertEquals(actualResponse,expectedResponse);
	}

	@Test
	public void updatePALProjectTest() {
		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProjectTest.json";

		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palProject", null),
				new TypeReference<PALProject>() {});

		when(mongoTemplate.findAndModify(Mockito.any(),Mockito.any(),Mockito.any(FindAndModifyOptions.class),Mockito.any()))
				.thenReturn(palProject);
		PALProject response = palCustomRepository.updatePALProject(palProject);
		assertEquals(palProject, response);
	}

	@Test
	public void updatePALProjectInvalidTest() {
		String fileName = "src/test/resources/ProductAttributeListingResponse/PALProjectTest.json";

		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palProject", null),
				new TypeReference<PALProject>() {});

		when(mongoTemplate.findAndModify(Mockito.any(),Mockito.any(),Mockito.any(FindAndModifyOptions.class),Mockito.any()))
				.thenReturn(null);
		PALProject response = palCustomRepository.updatePALProject(palProject);
		assertNull(response);
	}

	@Test
	public void findPALProjectListNoFilterTest() {
		when(mongoTemplate.find(Mockito.any(),Mockito.any(),Mockito.any()))
				.thenReturn(new ArrayList<>());
		List<PALProject> response = palCustomRepository.findPALProjectList(null, null);
		assertEquals(new ArrayList<>(), response);
	}

	@Test
	public void findPALProjectListByProjectIdsTest() {
		String fileName = "src/test/resources/ProductAttributeListingResponse/projectFilterDetails.json";

		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});
		List<PALProject> palProjects = new ArrayList<>();
		palProjects.add(palProject);

		Set<String> projectIds = new HashSet<>();
		projectIds.add("60ff64a1476c45bf24a684d4");

		when(mongoTemplate.find(Mockito.any(),eq(PALProject.class),Mockito.any()))
				.thenReturn(palProjects);
		List<PALProject> response = palCustomRepository
				.findPALProjectList(eq(projectIds), null);
		assertEquals(palProjects, response);
	}

	@Test
	public void findPALProjectListByProjectIdsAndFilterTest() {
		String fileName = "src/test/resources/ProductAttributeListingResponse/projectFilterDetails.json";

		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
				new TypeReference<PALProject>() {});
		List<PALProject> palProjects = new ArrayList<>();
		palProjects.add(palProject);

		Set<String> projectIds = new HashSet<>();
		projectIds.add("60ff64a1476c45bf24a684d4");

		ProjectFilter filter = (ProjectFilter) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("request", null),
				new TypeReference<ProjectFilter>() {});


		when(mongoTemplate.find(Mockito.any(),eq(PALProject.class),Mockito.any()))
				.thenReturn(palProjects);
		List<PALProject> response = palCustomRepository
				.findPALProjectList(eq(projectIds), filter);
		assertEquals(palProjects, response);
	}

	@Test
	public void findProductsForProjectIdOnlyTest() {
		String projectId = "123";
		palCustomRepository.findProducts(projectId, null, null);
		verify(mongoTemplate, times(1)).find(queryCaptor.capture(), eq(PALProduct.class),eq(palProductCollection));
		Query query = queryCaptor.getValue();
		assertNotNull(query);
		assertEquals("Query: { \"$and\" : [{ \"projectId\" : \"123\"}]}, Fields: {}, Sort: { \"createdDate\" : -1}", query.toString());
	}

	@Test
	public void findProductsForProductIdsOnlyTest() {
		List<String> products = new ArrayList<>(Arrays.asList("123","456"));
		palCustomRepository.findProducts(null, products, null);
		verify(mongoTemplate, times(1)).find(queryCaptor.capture(), eq(PALProduct.class),eq(palProductCollection));
		Query query = queryCaptor.getValue();
		assertNotNull(query);
		assertEquals("Query: { \"$and\" : [{ \"_id\" : { \"$in\" : [\"123\", \"456\"]}}]}, Fields: {}, Sort: { \"createdDate\" : -1}",
				query.toString());
	}

	@Test
	public void findProductsForProjectProductIdsSuppliersTest() {
		String projectId = "10";
		List<String> products = new ArrayList<>(Arrays.asList("123","456"));
		List<String> suppliers = new ArrayList<>(Collections.singletonList("789"));
		palCustomRepository.findProducts(projectId, products, suppliers);
		verify(mongoTemplate, times(1)).find(queryCaptor.capture(), eq(PALProduct.class),eq(palProductCollection));
		Query query = queryCaptor.getValue();
		assertNotNull(query);
		assertEquals("Query: { \"$and\" : [{ \"projectId\" : \"10\"}, { \"_id\" : { \"$in\" : [\"123\", \"456\"]}}, { \"datafields\" : { \"$elemMatch\" : { \"fieldId\" : \"supplierSiteCode\", \"fieldValue\" : { \"$in\" : [\"789\"]}}}}]}, Fields: {}, Sort: { \"createdDate\" : -1}",
				query.toString());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void upsertProductFieldsValidTest() {
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

		when(mongoTemplate.find(any(),eq(PALProduct.class), any())).thenReturn(productsAfterUpdate);

		List<PALProduct> actualResponse = palCustomRepository.upsertProductFields(productsBeforeUpdate, upsertProductFields,
				request.getUserRole(),request.getUser());

		assertNotNull(actualResponse);
		assertEquals(1, actualResponse.size());
		assertEquals(productsAfterUpdate, actualResponse);


	}

	@SuppressWarnings("unchecked")
	@Test
	public void upsertProductFieldsInvalidProductTest() {
		String fileName = "src/test/resources/ProductAttributeListingResponse/BulkProductUpdate.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		BulkProductUpdateRequest request = (BulkProductUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("invalidProductUpdateRequest", null),
				new TypeReference<BulkProductUpdateRequest>() {});
		Map<String, List<DataField>> upsertProductFields = CommonUtility.convertBulkRequestToMapObject(request);

		PALProduct productbeforeupdate = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("product1", null),
				new TypeReference<PALProduct>() {});
		List<PALProduct> productsBeforeUpdate = new ArrayList<>(Collections.singletonList(productbeforeupdate));

		List<DataField> productFieldUpdates = upsertProductFields.getOrDefault(productbeforeupdate.getId(), new ArrayList<>());
		PALProduct productafterupdate = TestUtility.getPALProductafterFieldUpdate(productbeforeupdate, productFieldUpdates);
		List<PALProduct> productsAfterUpdate = new ArrayList<>(Collections.singletonList(productafterupdate));

		List<PALProduct> actualResponse = palCustomRepository.upsertProductFields(productsBeforeUpdate, upsertProductFields,
				request.getUserRole(),request.getUser());
		assertNotNull(actualResponse);
		assertEquals(productsAfterUpdate, actualResponse);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void upsertProductFieldsNoChangeProductTest() {
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

		List<PALProduct> actualResponse = palCustomRepository.upsertProductFields(productsBeforeUpdate, upsertProductFields,
				request.getUserRole(),request.getUser());
		assertNotNull(actualResponse);
		assertEquals(CommonUtility.getDataField(productsAfterUpdate.get(0).getDatafields(), ApplicationConstant.UPC_FIELD),
				CommonUtility.getDataField(actualResponse.get(0).getDatafields(), ApplicationConstant.UPC_FIELD));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void upsertProductFieldsMultipleProductsTest() {
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

		List<PALProduct> productsBeforeUpdate = new ArrayList<>(Arrays.asList(product1beforeupdate, product2beforeupdate));

		List<DataField> productFieldUpdates = upsertProductFields.getOrDefault(product2beforeupdate.getId(), new ArrayList<>());
		PALProduct productafterupdate = TestUtility.getPALProductafterFieldUpdate(product2beforeupdate, productFieldUpdates);
		List<PALProduct> productsAfterUpdate = new ArrayList<>(Collections.singletonList(productafterupdate));

		when(mongoTemplate.find(any(),eq(PALProduct.class), any())).thenReturn(productsAfterUpdate);

		List<PALProduct> actualResponse = palCustomRepository.upsertProductFields(productsBeforeUpdate, upsertProductFields,
				request.getUserRole(),request.getUser());
		assertNotNull(actualResponse);
		assertEquals(productsAfterUpdate, actualResponse);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void upsertProductFieldsDeleteFieldTest() {
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

		when(mongoTemplate.find(any(),eq(PALProduct.class), any())).thenReturn(productsAfterUpdate);

		List<PALProduct> actualResponse = palCustomRepository.upsertProductFields(productsBeforeUpdate, upsertProductFields,
				request.getUserRole(),request.getUser());
		assertNotNull(actualResponse);
		assertEquals(productsAfterUpdate, actualResponse);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void upsertProductFieldsInsertFieldTest() {
		String fileName = "src/test/resources/ProductAttributeListingResponse/BulkProductUpdate.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		BulkProductUpdateRequest request = (BulkProductUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("insertFieldUpdateRequest", null),
				new TypeReference<BulkProductUpdateRequest>() {});
		Map<String, List<DataField>> upsertProductFields = CommonUtility.convertBulkRequestToMapObject(request);

		PALProduct productbeforeupdate = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("product1", null),
				new TypeReference<PALProduct>() {});
		List<PALProduct> productsBeforeUpdate = new ArrayList<>(Collections.singletonList(productbeforeupdate));

		List<DataField> productFieldUpdates = upsertProductFields.getOrDefault(productbeforeupdate.getId(), new ArrayList<>());
		PALProduct productafterupdate = TestUtility.getPALProductafterFieldUpdate(productbeforeupdate, productFieldUpdates);
		List<PALProduct> productsAfterUpdate = new ArrayList<>(Collections.singletonList(productafterupdate));

		List<PALProduct> actualResponse = palCustomRepository.upsertProductFields(productsBeforeUpdate, upsertProductFields,
				request.getUserRole(),request.getUser());
		assertNotNull(actualResponse);
		assertEquals(CommonUtility.getDataField(productsAfterUpdate.get(0).getDatafields(), ApplicationConstant.UPT_FIELD),
				CommonUtility.getDataField(actualResponse.get(0).getDatafields(), ApplicationConstant.UPT_FIELD));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void upsertProductFieldsMultipleFieldTest() {
		String fileName = "src/test/resources/ProductAttributeListingResponse/BulkProductUpdate.json";
		TypeReference<Map<String, Object>> typeReference = new TypeReference<>(){};
		Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);

		BulkProductUpdateRequest request = (BulkProductUpdateRequest) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("multipleFieldUpdateRequest", null),
				new TypeReference<BulkProductUpdateRequest>() {});
		Map<String, List<DataField>> upsertProductFields = CommonUtility.convertBulkRequestToMapObject(request);

		PALProduct productbeforeupdate = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("product1", null),
				new TypeReference<PALProduct>() {});
		List<PALProduct> productsBeforeUpdate = new ArrayList<>(Collections.singletonList(productbeforeupdate));

		List<DataField> productFieldUpdates = upsertProductFields.getOrDefault(productbeforeupdate.getId(), new ArrayList<>());
		PALProduct productafterupdate = TestUtility.getPALProductafterFieldUpdate(productbeforeupdate, productFieldUpdates);
		List<PALProduct> productsAfterUpdate = new ArrayList<>(Collections.singletonList(productafterupdate));

		when(mongoTemplate.find(any(),eq(PALProduct.class), any())).thenReturn(productsAfterUpdate);

		List<PALProduct> actualResponse = palCustomRepository.upsertProductFields(productsBeforeUpdate, upsertProductFields,
				request.getUserRole(),request.getUser());
		assertNotNull(actualResponse);
		assertEquals(productsAfterUpdate, actualResponse);
	}

	@Test
	public void findPALProjectListByProjectStatusFilterTest() {
		ProjectFilter projectFilter = new ProjectFilter();
		projectFilter.setProjectStatuses(new ArrayList<>(Arrays.asList("Draft","Creative Stage")));

		palCustomRepository.findPALProjectList(null, projectFilter);
		verify(mongoTemplate, times(1)).find(queryCaptor.capture(), eq(PALProject.class),eq(palProjectCollection));
		Query query = queryCaptor.getValue();
		assertNotNull(query);
		assertTrue(query.toString().contains("status"));
		assertTrue(query.toString().contains("Draft"));
		assertTrue(query.toString().contains("Creative Stage"));
	}

	@Test
	public void findPALProjectListByTemplateIdFilterTest() {
		ProjectFilter projectFilter = new ProjectFilter();
		projectFilter.setTemplateId("1234567890");

		palCustomRepository.findPALProjectList(null, projectFilter);
		verify(mongoTemplate, times(1)).find(queryCaptor.capture(), eq(PALProject.class),eq(palProjectCollection));
		Query query = queryCaptor.getValue();
		assertNotNull(query);
		assertTrue(query.toString().contains("templateId"));
		assertTrue(query.toString().contains("1234567890"));

	}
}
