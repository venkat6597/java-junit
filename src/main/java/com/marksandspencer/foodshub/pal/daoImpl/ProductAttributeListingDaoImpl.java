package com.marksandspencer.foodshub.pal.daoImpl;

import com.marksandspencer.foodshub.pal.constant.ApplicationConstant;
import com.marksandspencer.foodshub.pal.constant.Status;
import com.marksandspencer.foodshub.pal.dao.ProductAttributeListingDao;
import com.marksandspencer.foodshub.pal.domain.*;
import com.marksandspencer.foodshub.pal.repository.*;
import com.marksandspencer.foodshub.pal.repository.custom.PALCustomRepository;
import com.marksandspencer.foodshub.pal.transfer.Filter;
import com.marksandspencer.foodshub.pal.transfer.ProjectFilter;
import com.marksandspencer.foodshub.pal.util.CommonUtility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class ProductAttributeListingDaoImpl implements ProductAttributeListingDao {
	
	@Autowired
	PALTemplateRepository palTemplateRepository;
	
	@Autowired
	PALProductRepository palProductRepository;
	
	@Autowired
	PALProjectRepository palProjectRepository;
	
	@Autowired
	PALCustomRepository palCustomRepository;

	@Autowired
	PALFieldsRepository palFieldsRepository;
	
	@Autowired
	PALConfigurationRepository palConfigurationRepository;

	@Autowired
	PALAuditLogsRepository palAuditLogsRepository;
	
	@Autowired
	PALRoleRepository palRoleRepository;

	@Override
	@Cacheable("palFieldListEhCache")
	public List<PALFields> findAllPALFields() {
		log.info("ProductAttributeListingDaoImpl > findAllPALFields");
		return palFieldsRepository.findAll();
	}

	@Override
	public List<PALProject> findPALProjectList(Set<String> palProjectIds, ProjectFilter projectFilter) {
		log.info("ProductAttributeListingDaoImpl > findPALProjectList :: {}, {}", palProjectIds, projectFilter);
		return palCustomRepository.findPALProjectList(palProjectIds, projectFilter);
	}

	@Override
	@Cacheable("palConfigListEhCache")
	public List<PALConfiguration> findALLPALConfiguration() {
		log.info("ProductAttributeListingDaoImpl > findALLPALConfiguration");
		return palConfigurationRepository.findAll();
	}
	
	@Override
	public PALProject findPALProjectById(String projectId) {
		log.info("ProductAttributeListingDaoImpl > findPALProjectById :: {} ", projectId);
		return palProjectRepository.findById(projectId).stream()
				.filter(project -> !project.getStatus().equalsIgnoreCase(Status.DELETED.getStatus()))
				.findFirst().orElse(null);
	}

	@Override
	public PALProduct findPALProductById(String palProductId) {
		log.info("ProductAttributeListingDaoImpl > findPALProductById :: {} ", palProductId);
		return palProductRepository.findById(palProductId).stream()
				.filter(product -> !CommonUtility.getDataFieldValue(product.getDatafields(), ApplicationConstant.STATUS_FIELD).equalsIgnoreCase(Status.DELETED.getStatus()))
				.findFirst().orElse(null);
	}

	@Override
	@Cacheable("palTemplateListEhCache")
	public List<PALTemplate> findALLPALTemplate() {
		log.info("ProductAttributeListingDaoImpl > findALLPALTemplate");
		List<PALTemplate> dbPALTemplates = palTemplateRepository.findAll();
		if (!dbPALTemplates.isEmpty()) {
			return dbPALTemplates;			
		}
		return new ArrayList<>();
	}

	@Override
	public PALProduct savePALProduct(PALProduct palProduct) {
		log.info("ProductAttributeListingDaoImpl > savePALProduct :: {} ", palProduct);
		return palProductRepository.save(palProduct);
	}

	@Override
	public PALAuditLog findAuditLogs(String palProductId) {
		log.info("ProductAttributeListingDaoImpl > findAuditLogs :: {} ", palProductId);
		return palAuditLogsRepository.findByProductId(palProductId).stream().findFirst().orElse(null);
	}

	@Override
	public void updatePALAuditLog(String productId, List<Auditlog> auditlogs) {
		log.info("ProductAttributeListingDaoImpl > updatePALAuditLog for Product Id:: {} ", productId);
		PALAuditLog palAuditLog = findAuditLogs(productId);
		if (Objects.isNull(palAuditLog)) {
			palAuditLog = new PALAuditLog();
			palAuditLog.setProductId(productId);
			palAuditLog.setAuditLogs(auditlogs);
		} else {
			palAuditLog.getAuditLogs().addAll(auditlogs);
		}
		palAuditLogsRepository.save(palAuditLog);
		
	}

	@Override
	@Cacheable("palRoleListEhCache")
	public List<PALRole> findAllPALRoles() {
		log.info("ProductAttributeListingDaoImpl > findAllPALRoles");
		return palRoleRepository.findAll();
	}

	@Override
	public List<PALProduct> findPALProductsByProjectId(String projectId) {
		log.info("ProductAttributeListingDaoImpl > findPALProductsByProjectId for Project Id:: {} ", projectId);
		return palProductRepository.findProductsByProjectId(projectId).stream()
				.filter(product -> !CommonUtility.getDataFieldValue(product.getDatafields(), ApplicationConstant.STATUS_FIELD).equalsIgnoreCase(Status.DELETED.getStatus()))
				.collect(Collectors.toList());
	}
	
	@Override
	public PALProject savePALProject(PALProject palProject) {
		log.info("ProductAttributeListingDaoImpl > savePALProject :: {} ", palProject);
		return palProjectRepository.save(palProject);
	}
	
	@Override
	public PALProject updatePALProject(PALProject palProject) {
		log.info("ProductAttributeListingDaoImpl > updatePALProject :: {} ", palProject);
		return palCustomRepository.updatePALProject(palProject);
	}

	/**
	 * find Pal products by project Id and additional filters.
	 *
	 * @param projectId the project id
	 * @param filter    the filter
	 * @return the list
	 */
	@Override
	public List<PALProduct> findProductByFilterCondition(String projectId, Filter filter) {
		log.info("ProductAttributeListingDaoImpl > findProductByFilterCondition for Project Id:: {}, {} ", projectId, filter);
		return palCustomRepository.findProductByFilterCondition(projectId, filter);
	}

	/**
	 * find Pal products by project Id, product Id and suppliers
	 *
	 * @param projectId the project id
	 * @param productIds the list of products
	 * @param suppliers the suppliers list
	 * @return the list of palproducts
	 */
	@Override
	public List<PALProduct> findPALProducts(String projectId, List<String> productIds, List<String> suppliers) {
		log.info("ProductAttributeListingDaoImpl > findPALProducts for Project Id {} or product Id {} or suppliers {}", projectId, productIds, suppliers);
		return palCustomRepository.findProducts(projectId, productIds, suppliers).stream()
				.filter(product -> !CommonUtility.getDataFieldValue(product.getDatafields(), ApplicationConstant.STATUS_FIELD).equalsIgnoreCase(Status.DELETED.getStatus()))
				.collect(Collectors.toList());
	}

	@Override
	public PALConfiguration findPALConfigurationById(String configId) {
		return findALLPALConfiguration().stream().filter(config -> configId.equals(config.getId()))
				.findFirst().orElse(null);
	}

	@Override
	public List<PALProduct> saveAllProducts(List<PALProduct> palProductList) {
		return palProductRepository.saveAll(palProductList);
	}

	@Override
	public List<PALProduct> upsertProductFields(List<PALProduct> palProducts, Map<String, List<DataField>> upsertProductFields, String userRole, String userName) {
		return palCustomRepository.upsertProductFields(palProducts, upsertProductFields, userRole, userName);
	}

	@Override
	public PALConfiguration savePALConfiguration(PALConfiguration palConfigToUpdate) {
		return palConfigurationRepository.save(palConfigToUpdate);
	}
}
