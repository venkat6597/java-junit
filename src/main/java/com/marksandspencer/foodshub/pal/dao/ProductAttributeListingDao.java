package com.marksandspencer.foodshub.pal.dao;

import com.marksandspencer.foodshub.pal.domain.*;
import com.marksandspencer.foodshub.pal.transfer.Filter;
import com.marksandspencer.foodshub.pal.transfer.ProjectFilter;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ProductAttributeListingDao {
    List<PALFields> findAllPALFields();

    List<PALConfiguration> findALLPALConfiguration();

    PALProject findPALProjectById(String projectId);

    PALProduct findPALProductById(String palProductId);

    List<PALProject> findPALProjectList(Set<String> palProjectIds, ProjectFilter projectFilter);

    List<PALTemplate> findALLPALTemplate();

    PALProduct savePALProduct(PALProduct palProduct);

    PALAuditLog findAuditLogs(String palProductId);

    void updatePALAuditLog(String productId, List<Auditlog> auditlogs);

    List<PALRole> findAllPALRoles();

    List<PALProduct> findPALProductsByProjectId(String projectId);

    PALProject savePALProject(PALProject palProject);

    PALProject updatePALProject(PALProject palProject);

    List<PALProduct> findProductByFilterCondition(String projectId, Filter filter);

    List<PALProduct> findPALProducts(String projectId, List<String> productId, List<String> suppliers);

    PALConfiguration findPALConfigurationById(String configId);

    List<PALProduct> saveAllProducts(List<PALProduct> palProductList);

    List<PALProduct> upsertProductFields(List<PALProduct> palProducts, Map<String, List<DataField>> upsertProductFields,
                                         String userRole, String userName);

    PALConfiguration savePALConfiguration(PALConfiguration palConfigToUpdate);

}
