package com.marksandspencer.foodshub.pal.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marksandspencer.foodshub.pal.dao.AzureStorageDao;
import com.marksandspencer.foodshub.pal.domain.PALProject;
import com.marksandspencer.foodshub.pal.domain.PALRole;
import com.marksandspencer.foodshub.pal.serviceImpl.ExportHelperImpl;
import com.marksandspencer.foodshub.pal.transfer.AppResponse;
import com.marksandspencer.foodshub.pal.transfer.PALProductResponse;
import com.marksandspencer.foodshub.pal.utility.TestUtility;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.openxml4j.opc.ZipPackage;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockedStatic.Verification;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.io.ByteArrayResource;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ExportHelperTest {

    @InjectMocks
    ExportHelper exportHelper = new ExportHelperImpl();

    List<PALRole> palRoles = new ArrayList<>();
    
    @Mock
    private AzureStorageDao azureStorageDao;

    @Before
    public void beforeTest() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        AppResponse<List<PALRole>> response = mapper.readValue(new File("src/test/resources/ProductAttributeListingResponse/ListRolesResponse.json"),
                new TypeReference<>() {});
        palRoles = response.getData();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void createExcelTest() {
        String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductCreateTest.json";
        TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
        };
        Map<String, Object> responseDetails = TestUtility.readFile(fileName, typeReference);
        AppResponse<PALProductResponse> appresponse =  (AppResponse<PALProductResponse>) TestUtility.convertMapToObject((Map<String,Object>) responseDetails.getOrDefault("response", null),
                new TypeReference<AppResponse<PALProductResponse>>() {});
        List<PALProductResponse> responses = new ArrayList<>(Collections.singletonList(appresponse.getData()));

        fileName = "src/test/resources/ProductAttributeListingResponse/PALProductExport.json";
        Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);
        PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
                new TypeReference<PALProject>() {});

        ByteArrayResource btyeResponse = exportHelper.createExcel(responses, palRoles, palProject);
        assertNotNull(btyeResponse);

    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void createExcelWithMultipleArtworkTest() {
        String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductCreateTest.json";
        TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
        };
        Map<String, Object> responseDetails = TestUtility.readFile(fileName, typeReference);
        AppResponse<PALProductResponse> appresponse =  (AppResponse<PALProductResponse>) TestUtility.convertMapToObject((Map<String,Object>) responseDetails.getOrDefault("response1", null),
                new TypeReference<AppResponse<PALProductResponse>>() {});
        List<PALProductResponse> responses = new ArrayList<>(Collections.singletonList(appresponse.getData()));

        fileName = "src/test/resources/ProductAttributeListingResponse/PALProductExport.json";
        Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);
        PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
                new TypeReference<PALProject>() {});

        ByteArrayResource btyeResponse = exportHelper.createExcel(responses, palRoles, palProject);
        assertNotNull(btyeResponse);

    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void createExcelwithMacroTest() throws InvalidFormatException {
    	File downloadFile =new File("src/test/resources/ExportHelper/exportHelper.xlsm");
        String fileName = "src/test/resources/ProductAttributeListingResponse/PALProductCreateTest.json";
        TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
        };
        Map<String, Object> responseDetails = TestUtility.readFile(fileName, typeReference);
        AppResponse<PALProductResponse> appresponse =  (AppResponse<PALProductResponse>) TestUtility.convertMapToObject((Map<String,Object>) responseDetails.getOrDefault("response", null),
                new TypeReference<AppResponse<PALProductResponse>>() {});
        List<PALProductResponse> responses = new ArrayList<>(Collections.singletonList(appresponse.getData()));

        fileName = "src/test/resources/ProductAttributeListingResponse/PALProductExport.json";
        Map<String, Object> paldetails = TestUtility.readFile(fileName, typeReference);
        PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) paldetails.getOrDefault("palproject", null),
                new TypeReference<PALProject>() {});
        when(azureStorageDao.downloadBlobToFile(Mockito.anyString(),Mockito.any())).thenReturn(downloadFile);
        ByteArrayResource btyeResponse = exportHelper.createExcelwithMacro("supplier",responses, palRoles, palProject,"filename");
        assertNotNull(btyeResponse);
    }
}
