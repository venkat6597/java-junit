package com.marksandspencer.foodshub.pal.service;

import com.marksandspencer.foodshub.pal.domain.PALConfiguration;
import com.marksandspencer.foodshub.pal.domain.PALFields;
import com.marksandspencer.foodshub.pal.domain.PALRole;
import com.marksandspencer.foodshub.pal.domain.PALTemplate;
import com.marksandspencer.foodshub.pal.transfer.*;

import java.util.List;
import java.util.Map;

public interface ProductAttributeListingService {

    List<PALTemplate> getPALTemplates(PALTemplateRequest palTemplates);

    PALConfiguration getPALConfiguration(String configurationId);

    PALProductResponse createPALProduct(PALProductCreateRequest palProductCreateRequest);

    PALProductResponse getPALProductInformation(PALProductRequest palProductRequest);

    PALProductResponse getPALProductPersonnel(PALProductRequest palProductRequest);

    PALProductResponse updatePALProductPersonnel(PALProductUpdateRequest palProductUpdateRequest);

    PALProductResponse getPALProductProgress(PALProductRequest palProductRequest);

    PALProductResponse updatePALProductInformation(PALProductUpdateRequest palProductUpdateRequest);

    PALProductResponse getPALProductAuditlogs(PALProductRequest palProductRequest);

    List<PALRole> listRoles();

    List<PALFields> getPalFields();

    PALProjectResponse getPALProductList(PALProjectRequest palProjectRequest);

    PALProjectResponse getProjectDetails(PALProjectRequest palProjectRequest);

    PALProjectResponse getPALProjectProgress(PALProjectRequest palProjectRequest);

    List<PALProjectResponse> getPALProjectList(ProjectFilter projectFilter);

    PALProjectResponse addPALProject(PALProjectUpdateRequest palProjectUpdateRequest);

    PALProjectResponse updatePALProject(PALProjectUpdateRequest palProjectUpdateRequest);

    PALExportResponse palExportData(PALExportRequest palExportRequest);

    PALProjectResponse updatePALProjectPersonnel(PALProjectPersonnelUpdateRequest palProjectPersonnelUpdateRequest);

    List<PALConfiguration> getPALConfigurations(List<String> configurationIds);

    List<PALRole> listProjectTemplateRoles(String projectId);

    DuplicateProductResponse duplicateProducts(DuplicateProductRequest duplicateProductRequest);

    BulkProductUpdateResponse bulkUpdateInformation(BulkProductUpdateRequest bulkProductUpdateRequest);

    BulkProductResponse getBulkProductInformations(BulkProductRequest bulkProductRequest);

    Map<String, String> updatePALConfigs();

    PALDeleteResponse deleteProjectOrProducts(PALDeleteRequest palDeleteRequest);
}
