package com.marksandspencer.foodshub.pal.repository.custom.impl;

import com.marksandspencer.foodshub.pal.constant.ApplicationConstant;
import com.marksandspencer.foodshub.pal.constant.PALProjectConstants;
import com.marksandspencer.foodshub.pal.constant.Status;
import com.marksandspencer.foodshub.pal.dao.ProductAttributeListingDao;
import com.marksandspencer.foodshub.pal.domain.*;
import com.marksandspencer.foodshub.pal.repository.custom.PALCustomRepository;
import com.marksandspencer.foodshub.pal.transfer.Filter;
import com.marksandspencer.foodshub.pal.transfer.ProjectFilter;
import com.marksandspencer.foodshub.pal.util.CommonUtility;
import com.marksandspencer.foodshub.pal.util.Util;
import com.mongodb.bulk.BulkWriteResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Service
@Slf4j
public class PALCustomRepositoryImpl implements PALCustomRepository {

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    ProductAttributeListingDao palDao;

    /**
     * Sets the criteria regex pattern.
     * @param fieldId id of the field
     * @param fieldValues values of the field
     * @param criteriaList criteria list
     */
    private void setWhereInCriteria(List<Criteria> criteriaList, List<String> fieldValues, String fieldId) {
        if(!Objects.isNull(fieldValues)) {
            criteriaList.add(Criteria.where(fieldId).in(fieldValues));
        }
    }

    /**
     * Sets the criteria regex pattern.
     * @param fieldId field id
     * @param fromDate from date
     * @param toDate to date
     * @param criteriaList ctiretia list
     */
    private void setWhereDateRangeCriteria(List<Criteria> criteriaList, String fromDate, String toDate, String fieldId) {
        if (!StringUtils.isEmpty(fromDate) || !StringUtils.isEmpty(toDate)) {
            LocalDateTime localDateFrom = null;
            LocalDateTime localDateTo = null;
            if (!StringUtils.isEmpty(fromDate) && !StringUtils.isEmpty(toDate)) {
                localDateFrom = Util.dateConvertor(fromDate);
                localDateTo = Util.dateConvertor(toDate);
            } else if (!StringUtils.isEmpty(fromDate)) {
                localDateFrom = Util.dateConvertor(fromDate);
                localDateTo = localDateFrom;
            } else if (!StringUtils.isEmpty(toDate)) {
                localDateTo = Util.dateConvertor(toDate);
                localDateFrom = localDateTo;
            }
            if (!ObjectUtils.isEmpty(localDateFrom) && !ObjectUtils.isEmpty(localDateTo))
                criteriaList.add(Criteria.where(fieldId).gte(localDateFrom).lte(localDateTo));
        }
    }

    /**
     * Sets the criteria regex pattern.
     * @param fieldId field id
     * @param searchText search text
     * @param criteriaList criteria list
     */
    private void setWhereRegexCriteria(List<Criteria> criteriaList, String searchText, String fieldId) {
        if(!StringUtils.isEmpty(searchText)) {
            criteriaList.add(Criteria.where(fieldId).regex("(?i).*"+ Pattern.quote(searchText.toLowerCase()) + ".*"));
        }
    }

    /**
     * Sets the criteria is
     * @param fieldId field id
     * @param fieldValue field value
     * @param criteriaList criteria list
     */
    private void setWhereIsCriteria(List<Criteria> criteriaList, String fieldValue, String fieldId) {
        if(!StringUtils.isEmpty(fieldValue)) {
            criteriaList.add(Criteria.where(fieldId).is(fieldValue));
        }
    }

    /**
     * Sets the criteria is not
     * @param fieldId field id
     * @param fieldValue field value
     * @param criteriaList criteria list
     */
    private void setWhereIsNotCriteria(List<Criteria> criteriaList, String fieldValue, String fieldId) {
        if(!StringUtils.isEmpty(fieldValue)) {
            criteriaList.add(Criteria.where(fieldId).ne(fieldValue));
        }
    }

    /**
     * Sets the criteria is.
     * @param fieldId field id
     * @param fieldValue field value
     * @param criteriaList criteria list
     */
    private void setDataFieldWhereIsNotCriteria(List<Criteria> criteriaList, String fieldId, String fieldValue) {
        if(!StringUtils.isEmpty(fieldValue)) {
            criteriaList.add(Criteria.where(ApplicationConstant.PALProductFilterFields.DATAFIELD)
                    .elemMatch(Criteria.where(ApplicationConstant.PALProductFilterFields.DATAFIELD_ID).is(fieldId)
                            .and(ApplicationConstant.PALProductFilterFields.DATAFIELD_VALUE).ne(fieldValue)));
        }
    }

    /**
     * Sets the criteria is.
     * @param fieldId field id
     * @param fieldValue field value
     * @param criteriaList criteria list
     */
    private void setDataFieldWhereIsCriteria(List<Criteria> criteriaList, String fieldId, String fieldValue) {
        if(!StringUtils.isEmpty(fieldValue)) {
            criteriaList.add(Criteria.where(ApplicationConstant.PALProductFilterFields.DATAFIELD)
                    .elemMatch(Criteria.where(ApplicationConstant.PALProductFilterFields.DATAFIELD_ID).is(fieldId)
                            .and(ApplicationConstant.PALProductFilterFields.DATAFIELD_VALUE).is(fieldValue)));
        }
    }

    /**
     * Sets the criteria in.
     * @param fieldId field id
     * @param fieldValues field values
     * @param criteriaList criteria list
     */
    private void setDataFieldWhereInCriteria(List<Criteria> criteriaList, String fieldId, List<String> fieldValues) {
        if(!Objects.isNull(fieldValues)) {
            criteriaList.add(Criteria.where(ApplicationConstant.PALProductFilterFields.DATAFIELD)
                    .elemMatch(Criteria.where(ApplicationConstant.PALProductFilterFields.DATAFIELD_ID).is(fieldId)
                            .and(ApplicationConstant.PALProductFilterFields.DATAFIELD_VALUE).in(fieldValues)));
        }
    }

    /**
     * Sets the criteria regex pattern.
     * @param fieldId field id
     * @param searchText search text
     * @param criteriaList criteria list
     */
    private void setDataFieldWhereRegexCriteria(List<Criteria> criteriaList, String fieldId, String searchText) {
        if(!StringUtils.isEmpty(searchText)) {
            criteriaList.add(Criteria.where(ApplicationConstant.PALProductFilterFields.DATAFIELD)
                    .elemMatch(Criteria.where(ApplicationConstant.PALProductFilterFields.DATAFIELD_ID).is(fieldId)
                            .and(ApplicationConstant.PALProductFilterFields.DATAFIELD_VALUE)
                            .regex("(?i).*"+Pattern.quote(searchText.toLowerCase()) + ".*")));
        }
    }

    /**
     * updates the palproject is
     * @param palProject project
     * @return palProject project
     */
    @Override
    public PALProject updatePALProject(PALProject palProject) {
        Query query = new Query();
        query.addCriteria(Criteria.where(PALProjectConstants.PALProjectFieldNames.ID).is(palProject.getId()));

        Update update = new Update();

        if (StringUtils.isNotEmpty(palProject.getStatus())) {
            update.set(PALProjectConstants.PALProjectFieldNames.STATUS, palProject.getStatus());
        }
        if (StringUtils.isNotEmpty(palProject.getProjectName())) {
            update.set(PALProjectConstants.PALProjectFieldNames.PROJECT_NAME,palProject.getProjectName());
        }
        if (StringUtils.isNotEmpty(palProject.getProjectType())) {
            update.set(PALProjectConstants.PALProjectFieldNames.PROJECT_TYPE,palProject.getProjectType());
        }
        if (StringUtils.isNotEmpty(palProject.getFinancialYear())) {
            update.set(PALProjectConstants.PALProjectFieldNames.FINANCIAL_YEAR,palProject.getFinancialYear());
        }
        if (palProject.getProjectCompletionDate() != null) {
            update.set(PALProjectConstants.PALProjectFieldNames.PROJECT_COMPLETION_DATE,palProject.getProjectCompletionDate());
        }

        if (palProject.getComments() != null) {
            update.set(PALProjectConstants.PALProjectFieldNames.COMMENTS,palProject.getComments());
        }

        if (palProject.getDeletedDate() != null) {
            update.set(ApplicationConstant.DELETED_DATE_FIELD_ID,palProject.getDeletedDate());
        }
        return mongoTemplate.findAndModify(
                query, update, new FindAndModifyOptions().returnNew(true),
                PALProject.class);
    }

    /**
     * gets list of palprojects based on productIds and filter conditions
     * @param palProjectIds project ids
     * @param projectFilter project filter
     * @return palProjects pal projects
     */
    @Override
    public List<PALProject> findPALProjectList(Set<String> palProjectIds, ProjectFilter projectFilter) {
        String collectionName = mongoTemplate.getCollectionName(PALProject.class);
        Query projectFilterQuery = new Query();
        projectFilterQuery.with(Sort.by(Sort.Direction.DESC, ApplicationConstant.DOCUMENT_CREATED_DATE));
        List<Criteria> criteriaList = new ArrayList<>();
        if (!ObjectUtils.isEmpty(palProjectIds)) {
            setWhereInCriteria(criteriaList, List.copyOf(palProjectIds), PALProjectConstants.PALProjectFieldNames.ID);
        }
        if (!Objects.isNull(projectFilter)) {
            setWhereRegexCriteria(criteriaList, projectFilter.getSearchText(), PALProjectConstants.PALProjectFieldNames.PROJECT_NAME);
            setWhereDateRangeCriteria(criteriaList, projectFilter.getFromDate(), projectFilter.getToDate(), PALProjectConstants.PALProjectFieldNames.PROJECT_COMPLETION_DATE);
            setWhereInCriteria(criteriaList, projectFilter.getProjectType(), PALProjectConstants.PALProjectFieldNames.PROJECT_TYPE);
            setWhereInCriteria(criteriaList, projectFilter.getFinancialYear(), PALProjectConstants.PALProjectFieldNames.FINANCIAL_YEAR);
            setWhereInCriteria(criteriaList, projectFilter.getProjectStatuses(), PALProjectConstants.PALProjectFieldNames.STATUS);
            setWhereIsCriteria(criteriaList, projectFilter.getTemplateId(), PALProjectConstants.PALProjectFieldNames.TEMPLATE_ID);
        }
        setWhereIsNotCriteria(criteriaList, Status.DELETED.getStatus(), PALProjectConstants.PALProjectFieldNames.STATUS);
        if(criteriaList.size()!=0) {
            projectFilterQuery.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }
        return mongoTemplate.find(projectFilterQuery, PALProject.class, collectionName);
    }

    /**
     * gets list of palproducts based on projectId and filter conditions
     * @param projectId project id
     * @param filter project filter
     * @return palproducts products
     */
    @Override
    public List<PALProduct> findProductByFilterCondition(String projectId, Filter filter) {
        String collectionName = mongoTemplate.getCollectionName(PALProduct.class);

        Query productsWithFilterQuery = new Query();
        productsWithFilterQuery.with(Sort.by(Sort.Direction.DESC, ApplicationConstant.DOCUMENT_CREATED_DATE));
        List<Criteria> criteriaList = new ArrayList<>();

        setWhereIsCriteria(criteriaList, projectId, ApplicationConstant.PALProductFilterFields.PROJECT_ID);
        setDataFieldWhereIsNotCriteria(criteriaList,
                ApplicationConstant.PALProductFilterFields.STATUS, Status.DELETED.getStatus());

        if (!Objects.isNull(filter)) {
            setDataFieldWhereInCriteria(criteriaList, ApplicationConstant.PALProductFilterFields.STATUS, filter.getStatus());
            setDataFieldWhereInCriteria(criteriaList, ApplicationConstant.PALProductFilterFields.TYPE, filter.getType());
            setDataFieldWhereIsCriteria(criteriaList, ApplicationConstant.PALProductFilterFields.PARENT, filter.getParent());
            setDataFieldWhereIsCriteria(criteriaList,
                    ApplicationConstant.PALProductFilterFields.PRODUCT_IN_DEPO_DATE, filter.getIntoDepotDate());
            setDataFieldWhereIsCriteria(criteriaList,
                    ApplicationConstant.PALProductFilterFields.PRODUCT_IN_STORE_DATE, filter.getLaunchByDate());
            setDataFieldWhereIsCriteria(criteriaList,
                    ApplicationConstant.PALProductFilterFields.OCADO_PRODUCT,
                    !Objects.isNull(filter.getOcadoProduct())?"Yes":null);
            setDataFieldWhereInCriteria(criteriaList,
                    ApplicationConstant.PALProductFilterFields.SUPPLIERS, filter.getSuppliers());

            if(!Objects.isNull(filter.getSearchText())){
                List<Criteria> searchCriteria = new ArrayList<>();
                setDataFieldWhereRegexCriteria(searchCriteria,
                        ApplicationConstant.PALProductFilterFields.SUPPLIERNAME, filter.getSearchText());
                setDataFieldWhereRegexCriteria(searchCriteria,
                        ApplicationConstant.UPC_FIELD, filter.getSearchText());
                setDataFieldWhereRegexCriteria(searchCriteria,
                        ApplicationConstant.PRODUCT_TITLE_FIELD, filter.getSearchText());
                criteriaList.add(new Criteria().orOperator(searchCriteria.toArray(new Criteria[0])));
            }
        }

        productsWithFilterQuery
                .addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        return mongoTemplate.find(productsWithFilterQuery, PALProduct.class, collectionName);
    }

    @Override
    public List<PALProduct> findProducts(String projectId, List<String> productIds, List<String> suppliers) {
        String collectionName = mongoTemplate.getCollectionName(PALProduct.class);

        Query productsWithFilterQuery = new Query();
        productsWithFilterQuery.with(Sort.by(Sort.Direction.DESC, ApplicationConstant.DOCUMENT_CREATED_DATE));
        List<Criteria> criteriaList = new ArrayList<>();
        setWhereIsCriteria(criteriaList, projectId, ApplicationConstant.PALProductFilterFields.PROJECT_ID);
        setWhereInCriteria(criteriaList, productIds, ApplicationConstant.ID);
        setDataFieldWhereInCriteria(criteriaList, ApplicationConstant.PALProductFilterFields.SUPPLIERS, suppliers);
        productsWithFilterQuery
                .addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        return mongoTemplate.find(productsWithFilterQuery, PALProduct.class, collectionName);
    }

    /**
     * upserts the datafields in a product
     * @param palProducts products to be updated
     * @param upsertProductFields products with datafields to be updated
     * @param userRole user role
     * @param userName user name
     * @return palProducts updated products
     */
    @Override
    public List<PALProduct> upsertProductFields(List<PALProduct> palProducts, Map<String, List<DataField>> upsertProductFields, String userRole, String userName) {
        log.info("ProductAttributeListingServiceImpl > bulkUpdateInformation > upsertProductFields > Update started for :: {}",
                upsertProductFields);

        String collectionName = mongoTemplate.getCollectionName(PALProduct.class);
        ((MappingMongoConverter) mongoTemplate.getConverter()).setTypeMapper(new DefaultMongoTypeMapper(null));
        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, collectionName);

        Set<String> validProducts = new HashSet<>();
        for (Map.Entry<String, List<DataField>> upsertProduct : upsertProductFields.entrySet()) {

            Query query = new Query().addCriteria(new Criteria(ApplicationConstant.ID).is(upsertProduct.getKey()));
            Optional<PALProduct> dbProduct = palProducts.stream().filter(product -> upsertProduct.getKey().equalsIgnoreCase(product.getId()))
                    .findFirst();
            Update update = new Update();

            if (dbProduct.isPresent()) {
                String status = CommonUtility.getDataFieldValue(upsertProduct.getValue(), ApplicationConstant.STATUS_FIELD);
                if (!StringUtils.isEmpty(status) && status.equalsIgnoreCase(Status.DELETED.getStatus())) {
                    update.set(ApplicationConstant.DELETED_DATE_FIELD_ID, LocalDateTime.now());
                }
                for (DataField dataField : upsertProduct.getValue()) {
                    DataField dbDataField = CommonUtility.getDataField(dbProduct.get().getDatafields(), dataField.getFieldId());
                    setDataFieldUpdates(update, dataField, dbDataField);

                }
            }
            if (!ObjectUtils.isEmpty(update.getUpdateObject())) {
                bulkOps.upsert(query, update);
                validProducts.add(dbProduct.isPresent()? dbProduct.get().getId():null);
            }
        }

        if (!CollectionUtils.isEmpty(validProducts)) {
            BulkWriteResult results = bulkOps.execute();
            log.info("ProductAttributeListingServiceImpl > bulkUpdateInformation > upsertProductFields :: {} , Bulk Write Result :: {}",
                    validProducts, results);
            List<PALProduct> updatedProducts = findProducts(null, new ArrayList<>(upsertProductFields.keySet()),null);
            updateProductAuditLogs(validProducts, palProducts, updatedProducts, upsertProductFields, userRole, userName);
            return updatedProducts;
        }
        return palProducts;
    }

    private void updateProductAuditLogs(Set<String> validProducts, List<PALProduct> beforeUpdatedProducts, List<PALProduct> afterUpdatedProducts,
                                        Map<String, List<DataField>> upsertProductFields, String userRole, String userName) {
        String collectionName = mongoTemplate.getCollectionName(PALAuditLog.class);
        ((MappingMongoConverter) mongoTemplate.getConverter()).setTypeMapper(new DefaultMongoTypeMapper(null));
        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, collectionName);
        validProducts.forEach(productId -> {
            PALProduct beforeUpdateProduct = beforeUpdatedProducts.stream().filter(product -> productId.equalsIgnoreCase(product.getId()))
                    .findFirst().orElse(null);
            PALProduct afterUpdateProduct = afterUpdatedProducts.stream().filter(product -> productId.equalsIgnoreCase(product.getId()))
                    .findFirst().orElse(null);

            List<DataField> dataFields = upsertProductFields.getOrDefault(productId, null);
            List<Auditlog> auditLogs = new ArrayList<>();
            if (!CollectionUtils.isEmpty(dataFields) && !ObjectUtils.isEmpty(beforeUpdateProduct) && !ObjectUtils.isEmpty(afterUpdateProduct)) {
                dataFields.forEach(dataField -> {
                    String fieldId = dataField.getFieldId();
                    PALFields palField = CommonUtility.getPALFieldById(fieldId, palDao.findAllPALFields());
                    String upsertValue = StringUtils.isEmpty(dataField.getFieldValue())?null:dataField.getFieldValue();
                    String oldValue = CommonUtility.getDataFieldValue(beforeUpdateProduct.getDatafields(), fieldId);
                    String newValue = CommonUtility.getDataFieldValue(afterUpdateProduct.getDatafields(), fieldId);

                    if (StringUtils.equals(upsertValue,newValue) && !StringUtils.equals(oldValue, newValue)) {
                        auditLogs.add(Auditlog.builder().auditField(fieldId)
                                .auditFieldLabel(CommonUtility.getFieldDetails(palField, ApplicationConstant.FIELD_LABEL))
                                .auditDateTimeStamp(LocalDateTime.now())
                                .user(userName).userName(userName)
                                .userRole(userRole).userRoleLabel(CommonUtility.getUserRolelabel(userRole, palDao.findAllPALRoles()))
                                .oldValue(oldValue).newValue(newValue).build());
                    }

                });

                if (!CollectionUtils.isEmpty(auditLogs)) {
                    Query query = new Query().addCriteria(new Criteria(ApplicationConstant.PRODUCT_ID).is(productId));
                    auditLogs.forEach(auditLog -> {
                        Update update = new Update();
                        update.push("auditLogs", auditLog);
                        bulkOps.upsert(query, update);
                    });
                }
            }
        });
        BulkWriteResult results = bulkOps.execute();
        log.info("ProductAttributeListingServiceImpl > bulkUpdateInformation > updateProductAuditLogs :: {} , Bulk Write Result :: {}",
                validProducts, results);
    }

    private void setDataFieldUpdates(Update update, DataField updateDataField, DataField dbDataField) {
        if (StringUtils.isEmpty(updateDataField.getFieldValue()) && !ObjectUtils.isEmpty(dbDataField)) {
            // delete existing field if new field value is null
            update.pull("datafields", dbDataField);
        } else if (ObjectUtils.isEmpty(dbDataField) && !StringUtils.isEmpty(updateDataField.getFieldValue())) {
            // if field not exists, then add to the datafields
            update.push("datafields", updateDataField);
        } else if (!StringUtils.isEmpty(updateDataField.getFieldValue())
                && !updateDataField.getFieldValue().equals(dbDataField.getFieldValue())) {
            // if field exists, then update
            update.set("datafields.$[elem]", updateDataField);
            update.filterArray("elem.fieldId", updateDataField.getFieldId());
        }
    }
}
