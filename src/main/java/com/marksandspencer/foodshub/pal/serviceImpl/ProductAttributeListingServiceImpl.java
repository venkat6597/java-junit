package com.marksandspencer.foodshub.pal.serviceImpl;

import com.marksandspencer.assemblyservice.config.transfer.AccessControlInfo;
import com.marksandspencer.foodshub.pal.constant.*;
import com.marksandspencer.foodshub.pal.dao.ProductAttributeListingDao;
import com.marksandspencer.foodshub.pal.domain.*;
import com.marksandspencer.foodshub.pal.dto.Auditlog;
import com.marksandspencer.foodshub.pal.dto.Category;
import com.marksandspencer.foodshub.pal.exception.PALServiceException;
import com.marksandspencer.foodshub.pal.rest.client.ESProductHierarchyServiceRestClient;
import com.marksandspencer.foodshub.pal.service.ExportHelper;
import com.marksandspencer.foodshub.pal.service.NotificationService;
import com.marksandspencer.foodshub.pal.service.ProductAttributeListingService;
import com.marksandspencer.foodshub.pal.service.UserDetailsService;
import com.marksandspencer.foodshub.pal.transfer.*;
import com.marksandspencer.foodshub.pal.util.AutoCalculateFunction;
import com.marksandspencer.foodshub.pal.util.CommonUtility;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProductAttributeListingServiceImpl implements ProductAttributeListingService {

	@Value("${confluent.kafka.enabled:false}")
	Boolean kafkaEnabled;

	@Value("${foods.hub.ui.product.url}")
	String productUrl;

	@Value("${pal.configuration.update.list:}")
	List<String> configIds;

	@Autowired
	ProductAttributeListingDao palDao;

	@Autowired
	ExportHelper exportHelper;

	@Autowired
	UserDetailsService userDetailsService;

	@Autowired
	NotificationService notificationService;

	@Autowired
	ESProductHierarchyServiceRestClient esProductHierarchyServiceRestClient;

	DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
	List<String> notificationProjectStatus = new ArrayList<>(Arrays.asList(Status.FINALISE_STAGE.getStatus(), Status.POST_FINALISE_GATE.getStatus()));

	@Override
	public List<PALTemplate> getPALTemplates(PALTemplateRequest templatesRequest) {

		long startTime = System.currentTimeMillis();
		List<PALTemplate> templates = new ArrayList<>();
		AtomicReference<List<String>> templateIds = new AtomicReference<>();
		if (!ObjectUtils.isEmpty(templatesRequest) && !CollectionUtils.isEmpty(templatesRequest.getPalTemplateIds()))
			templateIds.set(templatesRequest.getPalTemplateIds());

		log.info("ProductAttributeListingServiceImpl > Started getPALTemplates for {} at :: {}", templateIds.get(), startTime);
		List<PALTemplate> dbTemplates = palDao.findALLPALTemplate();

		if (!CollectionUtils.isEmpty(templateIds.get())) {
			dbTemplates = dbTemplates.stream().filter(template -> templateIds.get().contains(template.getId()))
					.collect(Collectors.toList());
		}

		List<String> dbTemplateIds = dbTemplates.stream().map(PALTemplate::getId).collect(Collectors.toList());

		if (CollectionUtils.isEmpty(dbTemplateIds)) {
			log.error("ProductAttributeListingServiceImpl > getPALTemplates > No Templates found of Ids:: {}", templateIds.get());
			throw new PALServiceException(ErrorCode.INVALID_TEMPLATE_ID);
		} else if (!CollectionUtils.isEmpty(templateIds.get()) && !dbTemplateIds.containsAll(templateIds.get())) {
			Set<String> missingIds = new HashSet<>(templateIds.get());
			missingIds.addAll(dbTemplateIds);
			missingIds.removeAll(dbTemplateIds);
			log.error("ProductAttributeListingServiceImpl > getPALTemplates > Invalid Template Ids found :: {}", missingIds);
			throw new PALServiceException(ErrorCode.INVALID_TEMPLATE_ID);
		}

		for (PALTemplate template : dbTemplates) {
			PALTemplate newtemplate = new PALTemplate();
			newtemplate.setId(template.getId());
			newtemplate.setTemplateName(template.getTemplateName());
			if (!ObjectUtils.isEmpty(templatesRequest) && !templatesRequest.isSectionsRequired()) {
				newtemplate.setSections(null);
			} else if (!ObjectUtils.isEmpty(templatesRequest) && !StringUtils.isEmpty(templatesRequest.getUserRole())) {
				// filter sections based on user role
				String userRole = templatesRequest.getUserRole();
				AccessControlInfo accessInfo = userDetailsService.getAccessControlDetails(userRole);
				template.getSections().stream().forEach(section -> {
					List<PALFields> fields = section.getSectionFields().stream().filter(field -> !CommonUtility.isNonEditableField(field, userRole, accessInfo))
							.collect(Collectors.toList());
					section.setSectionFields(fields);
				});
				newtemplate.setSections(template.getSections().stream().filter(section -> !CollectionUtils.isEmpty(section.getSectionFields()))
						.collect(Collectors.toList()));
			} else {
				newtemplate.setSections(template.getSections());
			}
			if (CollectionUtils.isEmpty(newtemplate.getSections()))
				newtemplate.setSections(null);
			templates.add(newtemplate);
		}

		long endTime = System.currentTimeMillis();
		log.info("ProductAttributeListingServiceImpl > Completed getPALTemplates at :: {}, Time Taken :: {}", endTime, endTime-startTime);
		return templates;
	}

	@Override
	public PALProductResponse getPALProductInformation(PALProductRequest palProductRequest) {

		long startTime = System.currentTimeMillis();
		log.info("ProductAttributeListingServiceImpl > Started getPALProductInformation for request :: {}", palProductRequest);
		if (StringUtils.isBlank(palProductRequest.getProductId()) || StringUtils.isBlank(palProductRequest.getUserRole())) {
			log.error("ProductAttributeListingServiceImpl > getPALProductInformation > Invalid Request :: {}", palProductRequest);
			throw new PALServiceException(ErrorCode.INVALID_REQUEST_DATA);
		}
		String userRole = palProductRequest.getUserRole();
		UserDetails userDetails = userDetailsService.validateUserDetails(userRole, null);

		PALProductResponse palProductResponse = getPALProductResponse(palProductRequest, ApplicationConstant.INFORMATION_TAB, userDetails.getOrganizations());
		
		if (StringUtils.isNotBlank(palProductRequest.getRole()) || StringUtils.isNotBlank(palProductRequest.getSectionName())) {
			filterSections(palProductRequest, palProductResponse);
			if (!Objects.isNull(palProductResponse)) {
				applySectionProgress(palProductResponse);
			}
		}

		long endTime = System.currentTimeMillis();
		log.debug("ProductAttributeListingServiceImpl > Completed getPALProductInformation for request :: {}, response :: {}", palProductRequest, palProductResponse);
		log.info("ProductAttributeListingServiceImpl > Completed getPALProductInformation for request :: {}, Time Taken :: {}", palProductRequest, endTime-startTime);

		return palProductResponse;
	}

	private void applySectionProgress(PALProductResponse palProductResponse) {
		log.debug("ProductAttributeListingServiceImpl > Started applySectionProgress for product Id :: {}",
				palProductResponse.getId());

		palProductResponse.getSections().forEach(section -> {
			List<ProductField> allFields = new ArrayList<>();
			List<ProductField> fields = section.getFields();
			List<ProductField> subFields = fields.stream()
					.filter(field -> !CollectionUtils.isEmpty(field.getSubSections()))
					.flatMap(subsection -> subsection.getSubSections().stream()
							.flatMap(field -> field.getSubfields().stream()))
					.filter(ProductField::isMandatory)
					.collect(Collectors.toList());
			allFields.addAll(fields);
			allFields.addAll(subFields);
			Integer totalFields = allFields.size();
			Integer completedFields = (int) allFields.stream()
					.filter(field -> !StringUtils.isBlank(field.getValue())).count();
			section.setCompletedFields(completedFields);
			section.setTotalFields(totalFields);
		});

		log.debug("ProductAttributeListingServiceImpl > Completed applySectionProgress for product Id :: {}, response :: {}",
				palProductResponse.getId(), palProductResponse);
	}

	private PALProductResponse getPALProductResponse(PALProductRequest palProductRequest, String productTab, List<String> organisations) {
		log.debug("ProductAttributeListingServiceImpl > Started getPALProductResponse for product Id :: {}",
				palProductRequest.getProductId());

		String palProductId = palProductRequest.getProductId();
		String userRole = palProductRequest.getUserRole();

		PALProduct palProduct = palDao.findPALProductById(palProductId);
		if (ApplicationConstant.SUPPLIER.equalsIgnoreCase(userRole))
			validateUserAccessToProduct(palProduct, organisations);

		PALProductResponse palProductResponse = null;
		if (!Objects.isNull(palProduct)) {
			String projectId = palProduct.getProjectId();
			PALProject palProject = palDao.findPALProjectById(projectId);

			String templateId = palProduct.getTemplateId();
			PALTemplate palTemplate = getPALTemplateById(templateId);
			
			List<PALProduct> palProducts = new ArrayList<>();
			palProducts.add(palProduct);
			
			List<PALProductResponse> palProductResponses = framePalProductResponses(userRole, palProducts, palProject,
					palTemplate, productTab);
			if (!CollectionUtils.isEmpty(palProductResponses)) 
				palProductResponse = palProductResponses.get(0); 
				
		} else {
			log.error("ProductAttributeListingServiceImpl > getPALProductResponse > No Data available for request :: {}", palProductRequest);
			throw new PALServiceException(ErrorCode.NO_DATA);
		}
		log.debug("ProductAttributeListingServiceImpl > Completed getPALProductResponse for product Id :: {}, response :: {}",
				palProductRequest.getProductId(), palProductResponse);
		return palProductResponse;
	}

	private PALTemplate getPALTemplateById(String templateId) {
		return palDao.findALLPALTemplate().stream()
				.filter(template -> template.getId().equalsIgnoreCase(templateId))
				.findFirst().orElse(null);
	}

	private PALTemplate getPALTemplateByName(String templateName) {
		return palDao.findALLPALTemplate().stream()
				.filter(template -> template.getTemplateName().equalsIgnoreCase(templateName))
				.findFirst().orElse(null);
	}

	private List<PALProductResponse> framePalProductResponses(String userRole, List<PALProduct> palProducts, PALProject palProject,
			PALTemplate palTemplate, String productTab) {
		List<String> palProductIds = palProducts.stream().map(PALProduct::getId).collect(Collectors.toList());
		log.debug("ProductAttributeListingServiceImpl > Started framePalProductResponses for product Ids :: {}", palProductIds);

		if (Objects.isNull(palProject) || Objects.isNull(palTemplate)) {
			log.error("ProductAttributeListingServiceImpl > framePalProductResponses > No PALTemplate or PALProject identified for products :: {}",
					palProductIds);
			throw new PALServiceException(ErrorCode.NO_DATA);
		}

		String projectName = palProject.getProjectName();
		String templateName = palTemplate.getTemplateName();
		String templateId = palTemplate.getId();
		List<Section> templateSections = palTemplate.getSections();
		AccessControlInfo accessInfo = userDetailsService.getAccessControlDetails(userRole);

		List<PALProductResponse> palProductResponses = new ArrayList<>();
		palProducts.forEach(palProduct -> {
			PALProductResponse palProductResponse;
			String palProductId = palProduct.getId();

			List<DataField> palProductFields = palProduct.getDatafields();
			Map<String, String> palProductFieldValuesMap = new ConcurrentHashMap<>();
			palProductFieldValuesMap.put(PALProjectConstants.PALProjectFieldNames.PROJECT_NAME, palProject.getProjectName());
			Map<String, List<MultiField>> palProductMultiFieldValuesMap = new ConcurrentHashMap<>();
			for (DataField dataField : palProductFields) {
				palProductFieldValuesMap.put(dataField.getFieldId(), dataField.getFieldValue());
				if (!CollectionUtils.isEmpty(dataField.getMultifields())) {
					if (dataField.getFieldId().equalsIgnoreCase(ApplicationConstant.PRODUCT_FILE_TYPE_FIELD) && dataField.getFieldValue().equalsIgnoreCase(ApplicationConstant.PARENT)) {
						palProductMultiFieldValuesMap.put(dataField.getFieldId(), dataField.getMultifields());
					}
					if (dataField.getFieldId().equalsIgnoreCase(ApplicationConstant.PRINTED_PACKAGING_TYPE) && dataField.getFieldValue().equalsIgnoreCase(ApplicationConstant.MULTIPLE)) {
						palProductMultiFieldValuesMap.put(dataField.getFieldId(), dataField.getMultifields());
					}
				}
			}

			List<ProductSection> sections = setProductSections(userRole, templateSections,
					palProductFieldValuesMap, palProductMultiFieldValuesMap, accessInfo);
			Personnel personnel = palProduct.getPersonnel();
			Progress progress = getProgressDetails(sections);
			Header header = setProductHeaderDetails(projectName, palProductFieldValuesMap, palProductMultiFieldValuesMap);
			header.setProductId(palProductId);
			if (!ObjectUtils.isEmpty(progress)) {
				header.setPercentage(percentageFunc.apply(progress.getCompletedFields(), progress.getTotalFields()));
			}

			palProductResponse = new PALProductResponse();
			palProductResponse.setId(palProductId);
			palProductResponse.setTemplateId(templateId);
			palProductResponse.setTemplateName(templateName);
			palProductResponse.setHeader(header);
			if (ApplicationConstant.PERSONNEL_TAB.equalsIgnoreCase(productTab) || ApplicationConstant.INFORMATION_TAB.equalsIgnoreCase(productTab)) {
				palProductResponse.setPersonnel(personnel);
				palProductResponse.setSections(sections);
				applySectionProgress(palProductResponse);
			} else if (ApplicationConstant.PROGRESS_TAB.equalsIgnoreCase(productTab)) {
				palProductResponse.setProgress(progress);
			}
			palProductResponses.add(palProductResponse);
		});
		Collections.sort(palProductResponses);
		log.debug("ProductAttributeListingServiceImpl > Completed framePalProductResponses for product Ids :: {}", palProductIds);
		return palProductResponses;
	}

	private Progress getProgressDetails(List<ProductSection> sections) {
		log.debug("ProductAttributeListingServiceImpl > Started getProgressDetails");

		if (!CollectionUtils.isEmpty(sections)) {
			AtomicInteger completedFields = new AtomicInteger();
			List<ProductField> productFieldList = sections.stream()
					.flatMap(productSection -> productSection.getFields().stream()).filter(ProductField::isMandatory)
					.collect(Collectors.toList());
			List<ProductField> productSubFieldList = productFieldList.stream()
					.filter(field -> !CollectionUtils.isEmpty(field.getSubSections()))
					.flatMap(subsection -> subsection.getSubSections().stream()
							.flatMap(field -> field.getSubfields().stream()))
					.filter(ProductField::isMandatory)
					.collect(Collectors.toList());
			productFieldList.addAll(productSubFieldList);
			Progress progress = Progress.builder()
					.intoDepotDate(getFieldValue(productFieldList, ApplicationConstant.IN_DEPOT_DATE_FIELD))
					.intoStoreDate(getFieldValue(productFieldList, ApplicationConstant.LAUNCH_PHASE_FIELD))
					.totalFields(productFieldList.size())
					.roles(new ArrayList<>()).build();

			for (Map.Entry<String, List<ProductField>> item : productFieldList.stream().collect(Collectors.groupingBy(ProductField::getOwner, Collectors.toList()))
					.entrySet()) {
				int total = item.getValue().size();
				int completed = Math.toIntExact(item.getValue().stream()
						.filter(productField -> productField.getValue() != null)
						.count());
				completedFields.set(completedFields.get() + completed);
				int percentage = percentageFunc.apply(completed, total);
				progress.getRoles().add(RoleProgress.builder().role(item.getKey())
						.completedFields(completed)
						.totalFields(total).percentageCompletion(percentage)
						.status(getStatus(percentage))
						.build());
			}
			progress.setCompletedFields(completedFields.get());
			log.debug("ProductAttributeListingServiceImpl > Completed getProgressDetails");
			return progress;
		}
		return null;
	}

	private String getStatus(int percentage){
		if(percentage == 0){
			return Status.YET_TO_START.getStatus();
		} else if(percentage == 100){
			return Status.COMPLETED.getStatus();
		} else {
			return Status.IN_PROGRESS.getStatus();
		}
	}

	private void filterSections(PALProductRequest request, PALProductResponse palProductResponse) {
		log.debug("ProductAttributeListingServiceImpl > Started filterSections for product Id :: {}", request.getProductId());

		if (!Objects.isNull(palProductResponse)) {

			String palSelectedRole = request.getRole();
			String palSelectedSection = request.getSectionName();
			List<ProductSection> filteredSections = new ArrayList<>();
			List<ProductSection> sections = palProductResponse.getSections();
			if (StringUtils.isNotBlank(palSelectedSection))
				sections = sections.stream().filter(s -> s.getName().equalsIgnoreCase(palSelectedSection)).collect(Collectors.toList());

			if (StringUtils.isNotBlank(palSelectedRole)) {

				for (ProductSection section: sections) {
					ProductSection productSection = new ProductSection();
					List<ProductField> fields = section.getFields().stream().filter(f -> f.getOwner().equalsIgnoreCase(palSelectedRole)).collect(Collectors.toList());
					if (!CollectionUtils.isEmpty(fields)) {
						productSection.setFields(fields);
						productSection.setLabel(section.getLabel());
						productSection.setName(section.getName());
						filteredSections.add(productSection);
					}
				}
			}
			palProductResponse.setSections(filteredSections);
		}
		log.debug("ProductAttributeListingServiceImpl > Completed filterSections for product Id :: {}", request.getProductId());
	}

	private List<ProductSection> setProductSections(String palUserRole, List<Section> sections, Map<String, String> palProductFieldValuesMap,
			Map<String, List<MultiField>> palProductMultiFieldValuesMap, AccessControlInfo accessInfo) {
		log.debug("ProductAttributeListingServiceImpl > Started setProductSections");
		List<ProductSection> productSections = new ArrayList<>();
		ProductSection multipleArtworkSection = new ProductSection();
		List<ProductField> multipleArtworkProductSectionFields = new ArrayList<>();
		boolean reorderedMultipleArtworkSection = false;
		boolean skipSMPApprovedField = true;

		for(Section section: sections) {
			ProductSection productSection = new ProductSection();
			productSection.setName(section.getSectionName());
			productSection.setLabel(section.getSectionLabel());
			List<ProductField> productSectionFields = new ArrayList<>();

			//filter only readable fields
			List<PALFields> readableFields = filterReadbleFields(palUserRole, section.getSectionFields(), accessInfo);

			for (PALFields field : readableFields) {
				ProductField productField = createProductFields(field, palProductFieldValuesMap, palUserRole, accessInfo);

				if (field.getId().equalsIgnoreCase(ApplicationConstant.PRODUCT_FILE_TYPE_FIELD) && ApplicationConstant.PARENT.equalsIgnoreCase(productField.getValue())) {
                    List<ProductSubSection> subsections = setProductSubSections(palUserRole, field.getId(), section.getSubSections(),
							palProductMultiFieldValuesMap, accessInfo);
                    productField.setSubSections(subsections);
                }

				if (ApplicationConstant.DESIGN.equalsIgnoreCase(section.getSectionName()) && ApplicationConstant.PRINTED_PACKAGING_TYPE.equalsIgnoreCase(field.getId()) && ApplicationConstant.MULTIPLE.equalsIgnoreCase(palProductFieldValuesMap.get(ApplicationConstant.PRINTED_PACKAGING_TYPE))) {
					reorderedMultipleArtworkSection = true;
					multipleArtworkSection.setName(ApplicationConstant.MULTIPLE_ARTWORKS_FIELD);
					multipleArtworkSection.setLabel(ApplicationConstant.MULTIPLE_ARTWORK_LABEL);
					List<ProductField> multipleArtworkSectionFields = new ArrayList<>();
					ProductField multipleArtworkField = createProductFields(field, palProductFieldValuesMap, palUserRole, accessInfo);

					List<ProductSubSection> subsections = setProductSubSections(palUserRole, field.getId(), section.getSubSections(),
							palProductMultiFieldValuesMap, accessInfo);
					multipleArtworkField.setSubSections(subsections);

					multipleArtworkSectionFields.add(multipleArtworkField);
					multipleArtworkSection.setFields(multipleArtworkSectionFields);
					multipleArtworkProductSectionFields.add(productField);
				}
				if(section.getSectionName().equals(ApplicationConstant.PRINT_AND_PACKAGING)
						&& field.getId().equals(ApplicationConstant.PRINTER_TYPE) &&
						ApplicationConstant.SMP.equalsIgnoreCase(palProductFieldValuesMap.getOrDefault(ApplicationConstant.PRINTER_TYPE,null))){
					skipSMPApprovedField = false;
				}
				//skip smpapproved if printer type != SMP
				if(productField.getName().equals(ApplicationConstant.SMP_APPROVED) && skipSMPApprovedField){
					skipSMPApprovedField = false;
					continue;
				}
				productSectionFields.add(productField);
			}

			if (!CollectionUtils.isEmpty(productSectionFields)) {
				updateFieldOnCondition(productSectionFields);
				productSection.setFields(productSectionFields);
				productSections.add(productSection);
			}

			//add multiple artwork after design
			if(reorderedMultipleArtworkSection && !CollectionUtils.isEmpty(multipleArtworkProductSectionFields) ){
					productSections.add(multipleArtworkSection);
					reorderedMultipleArtworkSection = false;
			}
		}
		log.debug("ProductAttributeListingServiceImpl > Completed setProductSections");
		return productSections;
	}

	private void updateFieldOnCondition(List<ProductField> productSectionFields) {
		Optional<ProductField> fixedBuyField = productSectionFields.stream().filter(field -> field.getName().equalsIgnoreCase(ApplicationConstant.FIXED_BUY))
				.findFirst();
		if (fixedBuyField.isPresent() && !StringUtils.isEmpty(fixedBuyField.get().getValue())
				&& fixedBuyField.get().getValue().contains(ApplicationConstant.VALUE_YES)) {
			productSectionFields.forEach(field -> {
				if (field.getName().equalsIgnoreCase(ApplicationConstant.FIXED_BUY_COMMITMENT_DEADLINE))
					field.setMandatory(true);
			});
		}
	}

	private List<ProductSubSection> setProductSubSections(String palUserRole, String fieldId, List<SubSection> sections,
		Map<String, List<MultiField>> palProductMultiFieldValuesMap, AccessControlInfo accessInfo) {
		 log.debug("ProductAttributeListingServiceImpl > Started setProductSubSections");
	        List<ProductSubSection> productSubSections = new ArrayList<>();
	        for (SubSection section : sections) {

	            List<MultiField> palProductMultiFieldValuesMapWithId = palProductMultiFieldValuesMap.get(fieldId);
	            if (CollectionUtils.isEmpty(palProductMultiFieldValuesMapWithId)) {
	                List<ProductField> productSubSectionFields = new ArrayList<>();
	                // Creataing sub section fields with null values
	                ProductSubSection productSubSection = new ProductSubSection();
	                productSubSection.setName(section.getSubSectionName() + 1);
	                productSubSection.setLabel(section.getSubSectionLabel() + " " + 1);

	                //filter only readable fields
					List<PALFields> readableFields = filterReadbleFields(palUserRole, section.getSubSectionFields(), accessInfo);

	                for (PALFields subfield : readableFields) {
	                    productSubSectionFields.add(createProductFields(subfield, null, palUserRole, accessInfo));
	                }
	                if (!CollectionUtils.isEmpty(productSubSectionFields)) {
	                    productSubSection.setSubfields(productSubSectionFields);
	                    productSubSections.add(productSubSection);
	                }
	            } else {
	                int j = 1;
	                // Creataing sub section fields with  values
	                for (MultiField innerEntry : palProductMultiFieldValuesMapWithId) {
	                    List<ProductField> productSubSectionFields = new ArrayList<>();
	                    ProductSubSection productSubSection = new ProductSubSection();
	                    productSubSection.setName(section.getSubSectionName() + j);
	                    productSubSection.setLabel(section.getSubSectionLabel() + " " + j);  
	                    productSubSection.setSubSectionId(innerEntry.getMultiFieldId());
	                    j++;
	                    Map<String, String> resultMap = new ConcurrentHashMap<>();
	                    
	                    for (DataField innerMost : innerEntry.getDatafields()) {
							if (!StringUtils.isEmpty(innerMost.getFieldValue()))
	                        	resultMap.put(innerMost.getFieldId(), innerMost.getFieldValue());
	                    }

						//filter only readable fields
						List<PALFields> readableFields = filterReadbleFields(palUserRole, section.getSubSectionFields(), accessInfo);

						for (PALFields subfield : readableFields) {
	                        productSubSectionFields.add(createProductFields(subfield, resultMap, palUserRole, accessInfo));
	                    }
	                    if (!CollectionUtils.isEmpty(productSubSectionFields)) {
	                        productSubSection.setSubfields(productSubSectionFields);
	                        productSubSections.add(productSubSection);
	                    }

	                }
	            }
	        }
		 	log.debug("ProductAttributeListingServiceImpl > Completed setProductSubSections");
	        return productSubSections;
	    }

	private List<PALFields> filterReadbleFields(String palUserRole, List<PALFields> palFields, AccessControlInfo accessInfo) {
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

	private ProductField createProductFields(PALFields field, Map<String, String> resultMap, String palUserRole, AccessControlInfo accessInfo) {
		ProductField productsFieldsValue = new ProductField();
		productsFieldsValue.setName(field.getId());
		productsFieldsValue.setCreativeGate(field.isCreativeGate());
		productsFieldsValue.setDisabled(true);
		productsFieldsValue.setLabel(field.getLabel());
		productsFieldsValue.setTooltip(field.getTooltip());
		productsFieldsValue.setPlaceholder(field.getPlaceholder());
		productsFieldsValue.setOwner(field.getOwner());
		productsFieldsValue.setType(field.getType());
		productsFieldsValue.setErrorMessages(field.getErrorMessages());
		if (ObjectUtils.isNotEmpty(resultMap)) {
			productsFieldsValue.setValue(resultMap.getOrDefault(field.getId(), null));
		}
		productsFieldsValue.setPattern(field.getPattern());
		productsFieldsValue.setMinDate(field.getMinDate());
		productsFieldsValue.setMandatory(field.isMandatory());
		if (field.getOptionsRef() != null) {
			List<String> options = new ArrayList<>(field.getOptionsRef().getValues());
			productsFieldsValue.setOptions(options);
		}

		if (!ObjectUtils.isEmpty(accessInfo) && accessInfo.isUpdateAccess() && !Objects.isNull(field.getEditable()) &&
				(field.getEditable().contains(palUserRole) ||
				!ApplicationConstant.SUPPLIER.equalsIgnoreCase(palUserRole) && field.getEditable().contains(ApplicationConstant.MNS))) {
			productsFieldsValue.setDisabled(false);
		}

		return productsFieldsValue;
	}

	private Header setProductHeaderDetails(String projectName, Map<String, String> palProductFieldValuesMap, 
			Map<String, List<MultiField>> palProductMultiFieldValuesMap) {
		log.debug("ProductAttributeListingServiceImpl > Started setProductHeaderDetails");
		Header header = new Header(); 
		header.setProjectName(projectName);
		header.setProductName(palProductFieldValuesMap.getOrDefault(ApplicationConstant.PRODUCT_TITLE_FIELD, null));
		header.setProductType(palProductFieldValuesMap.getOrDefault(ApplicationConstant.PRODUCT_TYPE_FIELD, null));
		header.setCategory(palProductFieldValuesMap.getOrDefault(ApplicationConstant.CATEGORY_FIELD, null));
		header.setProductFileType(palProductFieldValuesMap.getOrDefault(ApplicationConstant.PRODUCT_FILE_TYPE_FIELD, null));
		header.setStatus(palProductFieldValuesMap.getOrDefault(ApplicationConstant.STATUS_FIELD, null));
		header.setSupplierName(palProductFieldValuesMap.getOrDefault(ApplicationConstant.SUPPLIER_NAME_FIELD, null));
		header.setUpc(palProductFieldValuesMap.getOrDefault(ApplicationConstant.UPC_FIELD, null));
		header.setSubRange(palProductFieldValuesMap.getOrDefault(ApplicationConstant.SUBRANGE_FIELD_ID,null));
		String weight = palProductFieldValuesMap.getOrDefault(ApplicationConstant.WEIGHT_OR_VOLUME_FIELD, null);
		if (!StringUtils.isEmpty(weight)) {
			weight = weight + " " +	palProductFieldValuesMap.getOrDefault(ApplicationConstant.UNIT_OF_MEASURE_FIELD, null);
			header.setWeight(weight);
		}
		header.setSupplierCode(palProductFieldValuesMap.getOrDefault(ApplicationConstant.SUPPLIER_SITE_CODE_FIELD, null));
		
		List<MultiField> childProductList = palProductMultiFieldValuesMap.getOrDefault(ApplicationConstant.PRODUCT_FILE_TYPE_FIELD, null);
		if (!CollectionUtils.isEmpty(childProductList)) {
			List<ChildProduct> childProducts =new ArrayList<>();
			childProductList.forEach(childProductFields -> {
				ChildProduct childProduct = new ChildProduct();
				childProduct.setProductName(getDataFieldValue(childProductFields.getDatafields(), ApplicationConstant.CHILD_PRODUCT_TITLE_FIELD));
				childProduct.setSupplierName(getDataFieldValue(childProductFields.getDatafields(), ApplicationConstant.CHILD_SUPPLIER_NAME_FIELD));
				childProduct.setSupplierCode(getDataFieldValue(childProductFields.getDatafields(), ApplicationConstant.CHILD_SUPPLIER_SITE_CODE_FIELD));
				childProduct.setUpc(getDataFieldValue(childProductFields.getDatafields(), ApplicationConstant.CHILD_UPC_FIELD));
				childProducts.add(childProduct);
			});
			header.setChildProducts(childProducts);
		}
		log.debug("ProductAttributeListingServiceImpl > Completed setProductHeaderDetails");
		return header;
	}

	@Override
	public PALConfiguration getPALConfiguration(String configurationId) {
		List<PALConfiguration> palConfiguration = getPALConfigurations(Collections.singletonList(configurationId));
		return !CollectionUtils.isEmpty(palConfiguration)
				? palConfiguration.get(0)
				: null;
	}

	@Override
	public PALProductResponse createPALProduct(PALProductCreateRequest palProductCreateRequest) {
		long startTime = System.currentTimeMillis();
		log.info("ProductAttributeListingServiceImpl > Started createPALProduct for id :: {}", palProductCreateRequest);

		if (StringUtils.isBlank(palProductCreateRequest.getProjectId()) ||
				StringUtils.isBlank(palProductCreateRequest.getUserRole()) ||
				StringUtils.isBlank(palProductCreateRequest.getTemplateId()) ||
				CollectionUtils.isEmpty(palProductCreateRequest.getDataFields())) {
			throw new PALServiceException(ErrorCode.INVALID_REQUEST_DATA);
		}

		UserDetails userDetails = userDetailsService.validateUserDetails(palProductCreateRequest.getUserRole(), null);

		PALProject palProject = palDao.findPALProjectById(palProductCreateRequest.getProjectId());
		List<DataField> dataFields = palProductCreateRequest.getDataFields();

		dataFields.add(DataField.builder().fieldId(ApplicationConstant.STATUS_FIELD)
				.fieldValue(Status.IN_PROGRESS.getStatus())
				.build());
		//ignore null values
		dataFields = dataFields.stream().filter(field -> !StringUtils.isEmpty(field.getFieldValue())).collect(Collectors.toList());
		PALProduct palProduct = new PALProduct();
		palProduct.setProjectId(palProductCreateRequest.getProjectId());
		palProduct.setTemplateId(palProductCreateRequest.getTemplateId());
		palProduct.setDatafields(dataFields);
		palProduct.setPersonnel(palProductCreateRequest.getPersonnel());
		palProduct = palDao.savePALProduct(palProduct);

		PALProductRequest palProductRequest = new PALProductRequest();
		palProductRequest.setProductId(palProduct.getId());
		palProductRequest.setUserRole(palProductCreateRequest.getUserRole());
		PALProductResponse palProductResponse = getPALProductResponse(palProductRequest, ApplicationConstant.INFORMATION_TAB, userDetails.getOrganizations());

		if (kafkaEnabled && !palProject.getStatus().equalsIgnoreCase(Status.DRAFT.getStatus())) {
			notificationService.sendEmailMessage(palProject, palProduct, MessageTemplate.ADD_PRODUCT);
		}

		long endTime = System.currentTimeMillis();
		log.info("ProductAttributeListingServiceImpl > Completed createPALProduct for request :: {}, Time Taken :: {}", palProductCreateRequest, endTime-startTime);
		log.debug("ProductAttributeListingServiceImpl > Response for createPALProduct for request :: {} is :: {}", palProductCreateRequest, palProductResponse);

		return palProductResponse;
	}

	@Override
	public PALProductResponse getPALProductPersonnel(PALProductRequest palProductRequest) {

		long startTime = System.currentTimeMillis();
		log.info("ProductAttributeListingServiceImpl > Started getPALProductPersonnel for id :: {}", palProductRequest);
		
		if (StringUtils.isBlank(palProductRequest.getProductId()) || StringUtils.isBlank(palProductRequest.getUserRole())) {
			throw new PALServiceException(ErrorCode.INVALID_REQUEST_DATA);
		}
		UserDetails userDetails = userDetailsService.validateUserDetails(palProductRequest.getUserRole(), null);

		PALProductResponse palProductResponse = getPALProductResponse(palProductRequest, ApplicationConstant.PERSONNEL_TAB, userDetails.getOrganizations());
		long endTime = System.currentTimeMillis();
		log.info("ProductAttributeListingServiceImpl > Completed getPALProductPersonnel for request :: {}, Time Taken :: {}", palProductRequest, endTime-startTime);
		log.debug("ProductAttributeListingServiceImpl > Response for getPALProductPersonnel for request :: {} is :: {}", palProductRequest, palProductResponse);

		return palProductResponse;

	}

	@Override
	public PALProductResponse updatePALProductPersonnel(PALProductUpdateRequest palProductUpdateRequest) {
		long startTime = System.currentTimeMillis();
		log.info("ProductAttributeListingServiceImpl > Started updatePALProductPersonnel for id :: {}", palProductUpdateRequest);
		
		if (StringUtils.isBlank(palProductUpdateRequest.getProductId()) || 
				StringUtils.isBlank(palProductUpdateRequest.getUserRole()) ||
				StringUtils.isBlank(palProductUpdateRequest.getUser()) ||
				CollectionUtils.isEmpty(palProductUpdateRequest.getPersonnelUpdates())) {
			throw new PALServiceException(ErrorCode.INVALID_REQUEST_DATA);
		}
		UserDetails userDetails = userDetailsService.validateUserDetails(palProductUpdateRequest.getUserRole(), null);
		if (!isValidSupplierPersonnelUpdate(palProductUpdateRequest, userDetails.getUserRole().getRoleName())) {
			throw new PALServiceException(ErrorCode.UNAUTHORIIZED);
		}

		String productId = palProductUpdateRequest.getProductId();
		String userRole = palProductUpdateRequest.getUserRole();
		PALProduct palProduct = palDao.findPALProductById(productId);
		if (ApplicationConstant.SUPPLIER.equalsIgnoreCase(userRole))
			validateUserAccessToProduct(palProduct, userDetails.getOrganizations());
		if (!Objects.isNull(palProduct)) {
			Personnel personnel = palProduct.getPersonnel();
			List<PersonnelUpdate> personnelUpdates = palProductUpdateRequest.getPersonnelUpdates();
			personnelUpdates.forEach(update -> {
				List<String> newValues = update.getNewValue();
				String fieldkey = update.getField();
				personnel.getExternal().forEach(person -> {
					if (person.getRole().equalsIgnoreCase(fieldkey)) {
						person.setUsers(newValues);
					}
				});
				personnel.getInternal().forEach(person -> {
					if (person.getRole().equalsIgnoreCase(fieldkey)) {
						person.setUsers(newValues);
					}
				});
			});
			
			palProduct.setPersonnel(personnel);
			palDao.savePALProduct(palProduct);
		
		}

		PALProductResponse palProductResponse = getPALProductPersonnel(PALProductRequest.builder().productId(productId)
				.userRole(userRole).build());
		long endTime = System.currentTimeMillis();
		log.info("ProductAttributeListingServiceImpl > Completed updatePALProductPersonnel for request :: {}, Time Taken :: {}", palProductUpdateRequest,  endTime-startTime);
		log.debug("ProductAttributeListingServiceImpl > Response for updatePALProductPersonnel for request :: {} is :: {}", palProductUpdateRequest, palProductResponse);
		
		return palProductResponse;

	}

	private boolean isValidSupplierPersonnelUpdate(PALProductUpdateRequest palProductUpdateRequest, String roleName) {
		AtomicBoolean valid = new AtomicBoolean(true);
		palProductUpdateRequest.getPersonnelUpdates().forEach(update -> {
				if (ApplicationConstant.SUPPLIER.equalsIgnoreCase(roleName) && !roleName.equalsIgnoreCase(update.getField())) {
					valid.set(false);
				}
			});
		return valid.get();
	}

	BiFunction<Integer, Integer, Integer> percentageFunc = (x, y) -> {
		if(x == 0 && y == 0){
			return 100;
		}
		return Math.floorDiv(x * 100, y);
	};

	@Override
	public PALProductResponse getPALProductProgress(PALProductRequest palProductRequest) {
		long startTime = System.currentTimeMillis();
		log.info("ProductAttributeListingServiceImpl > Started getPALProductProgress for id :: {}", palProductRequest);

		if (StringUtils.isBlank(palProductRequest.getProductId()) || StringUtils.isBlank(palProductRequest.getUserRole())) {
			throw new PALServiceException(ErrorCode.INVALID_REQUEST_DATA);
		}
		UserDetails userDetails = userDetailsService.validateUserDetails(palProductRequest.getUserRole(), null);
		if (ApplicationConstant.SUPPLIER.equalsIgnoreCase(userDetails.getUserRole().getRoleName())) {
			throw new PALServiceException(ErrorCode.UNAUTHORIIZED);
		}
		PALProductResponse palProductResponse = getPALProductResponse(palProductRequest, ApplicationConstant.PROGRESS_TAB, userDetails.getOrganizations());

		long endTime = System.currentTimeMillis();
		log.info("ProductAttributeListingServiceImpl > Completed getPALProductProgress for request :: {}, Time Taken :: {}", palProductRequest, endTime - startTime);
		log.debug("ProductAttributeListingServiceImpl > Response for getPALProductProgress for request :: {} is :: {}", palProductRequest, palProductResponse);

		return palProductResponse;
	}

	@Override
	public PALProductResponse getPALProductAuditlogs(PALProductRequest palProductRequest) {
		long startTime = System.currentTimeMillis();
		log.info("ProductAttributeListingServiceImpl > Started getPALProductAuditlogs for id :: {}", palProductRequest);

		if (StringUtils.isBlank(palProductRequest.getProductId()) || StringUtils.isBlank(palProductRequest.getUserRole())) {
			throw new PALServiceException(ErrorCode.INVALID_REQUEST_DATA);
		}
		UserDetails userDetails = userDetailsService.validateUserDetails(palProductRequest.getUserRole(), null);
		PALProductResponse palProductResponse = getPALProductResponse(palProductRequest, ApplicationConstant.AUDITLOG_TAB, userDetails.getOrganizations());
		AccessControlInfo accessInfo = userDetails.getUserRole().getAccessControlInfoList().get(0);
		PALAuditLog palAuditLog = palDao.findAuditLogs(palProductRequest.getProductId());
		if (!Objects.isNull(palAuditLog)) {
			List<PALFields> palFieldsList = filterReadbleFields(palProductRequest.getUserRole(), palDao.findAllPALFields(), accessInfo);
			List<String> palFields = palFieldsList.stream()
					.map(PALFields::getId)
					.collect(Collectors.toList());
			List<Auditlog> auditlogs = palAuditLog.getAuditLogs().stream()
					.filter(auditlog -> palFields.contains(auditlog.getAuditField()))
					.map(auditlog -> Auditlog.builder().createdBy(auditlog.getUserName()).oldValue(auditlog.getOldValue())
							.newValue(auditlog.getNewValue()).field(auditlog.getAuditFieldLabel())
							.role(auditlog.getUserRoleLabel()).createdOn(auditlog.getAuditDateTimeStamp())
							.createdTime(auditlog.getAuditDateTimeStamp().format(timeFormatter)).build())
					.collect(Collectors.toList());
			if(!Objects.isNull(palProductRequest.getFilter())){
				if(palProductRequest.getFilter().getFromDate() != null
						&& palProductRequest.getFilter().getToDate() != null){
					LocalDateTime fromDate = LocalDate.parse(palProductRequest.getFilter().getFromDate(), dateFormatter).atStartOfDay();
					LocalDateTime toDate = LocalDate.parse(palProductRequest.getFilter().getToDate(), dateFormatter).plusDays(1).atStartOfDay();
					auditlogs = auditlogs.stream().filter(auditlog ->
						auditlog.getCreatedOn().isAfter(fromDate) && auditlog.getCreatedOn().isBefore(toDate)
					).collect(Collectors.toList());
				}
				if(palProductRequest.getFilter().getSearchText() != null){
					auditlogs = auditlogs.stream().filter(auditlog ->
						auditlog.getField().toLowerCase().contains(palProductRequest.getFilter().getSearchText().toLowerCase())
								|| auditlog.getRole().toLowerCase().contains(palProductRequest.getFilter().getSearchText().toLowerCase())
								|| auditlog.getCreatedBy().toLowerCase().contains(palProductRequest.getFilter().getSearchText().toLowerCase())
					).collect(Collectors.toList());
				}
			}
			auditlogs.sort(Comparator.comparing(Auditlog::getCreatedOn).reversed());
			if (!Objects.isNull(palProductResponse))
				palProductResponse.setAuditlogs(auditlogs);
		}

		long endTime = System.currentTimeMillis();
		log.info("ProductAttributeListingServiceImpl > Completed getPALProductAuditlogs for request :: {}, Time Taken :: {}", palProductRequest, endTime - startTime);
		log.debug("ProductAttributeListingServiceImpl > Response for getPALProductAuditlogs for request :: {} is :: {}", palProductRequest, palProductResponse);

		return palProductResponse;
	}

	private String getFieldValue(List<ProductField> productFields, String field) {
		Optional<ProductField> result = productFields.stream().filter(productField ->
				productField.getName().equals(field))
				.findFirst();
		return result.map(ProductField::getValue).orElse(null);
	}

	private String getDataFieldValue(List<DataField> dataFields, String field) {
		Optional<DataField> result = dataFields.stream().filter(dataField ->
				dataField.getFieldId().equals(field))
				.findFirst();
		return result.isPresent() ? result.get().getFieldValue() : null;
	}

	@Override
	public PALProductResponse updatePALProductInformation(PALProductUpdateRequest palProductUpdateRequest) {
		long startTime = System.currentTimeMillis();
		log.info("ProductAttributeListingServiceImpl > Started updatePALProductInformation for id :: {}", palProductUpdateRequest);
		
		if (StringUtils.isBlank(palProductUpdateRequest.getProductId()) || 
				StringUtils.isBlank(palProductUpdateRequest.getUserRole()) ||
				StringUtils.isBlank(palProductUpdateRequest.getUser()) ||
				CollectionUtils.isEmpty(palProductUpdateRequest.getFieldUpdates())) {
			throw new PALServiceException(ErrorCode.INVALID_REQUEST_DATA);
		}
		UserDetails userDetails = userDetailsService.validateUserDetails(palProductUpdateRequest.getUserRole(), null);
		PALProductResponse palProductResponse;
		// Ignore fields for which old and new values are same
		List<FieldUpdate> fieldUpdates = palProductUpdateRequest.getFieldUpdates().stream()
				.filter(field -> isValidFieldUpdate(field, userDetails))
				.collect(Collectors.toList());

		if (CollectionUtils.isEmpty(fieldUpdates)) {
			log.info("ProductAttributeListingServiceImpl > No Changes identified for update request :: {}", palProductUpdateRequest);
			throw new PALServiceException(ErrorCode.NO_CHANGES);
		}
		else {

			String productId = palProductUpdateRequest.getProductId();
			String userRole = palProductUpdateRequest.getUserRole();
			String userId = palProductUpdateRequest.getUser();

			PALProductRequest palProductRequest = new PALProductRequest();
			palProductRequest.setProductId(productId);
			palProductRequest.setUserRole(userRole);

			PALProduct palProduct = palDao.findPALProductById(productId);

			if (ApplicationConstant.SUPPLIER.equalsIgnoreCase(userRole))
				validateUserAccessToProduct(palProduct, userDetails.getOrganizations());

			String previousProductStatus;
			if (Objects.isNull(palProduct)) {
				log.error("ProductAttributeListingServiceImpl > PAL Product not identified for update request :: {}", palProductUpdateRequest);
				throw new PALServiceException(ErrorCode.NO_DATA);
			}
			else {
				List<DataField> datafields = palProduct.getDatafields();
				List<com.marksandspencer.foodshub.pal.domain.Auditlog> auditlogs = new ArrayList<>();
				previousProductStatus = getDataFieldValue(datafields, ApplicationConstant.STATUS_FIELD);
				fieldUpdates.forEach(update -> {

					String newfieldvalue = !StringUtils.isEmpty(update.getNewValue()) ? update.getNewValue().trim() : null;
					String fieldkey = update.getField();
					List<FieldUpdate> subFieldUpdates = update.getSubSectionFields();

					AtomicReference<String> subSectionId = new AtomicReference<>(update.getSubSectionId());
					if (StringUtils.isEmpty(subSectionId.get()) && !CollectionUtils.isEmpty(subFieldUpdates)) {
						subSectionId.set(UUID.randomUUID().toString());
					}
					AtomicReference<Boolean> firsttimeupdate = new AtomicReference<>(true);
					datafields.forEach(field -> {
						if (field.getFieldId().equalsIgnoreCase(fieldkey)) {
							firsttimeupdate.set(false);
							if (CollectionUtils.isEmpty(subFieldUpdates) && StringUtils.isEmpty(subSectionId.get())) {
								if (StringUtils.isEmpty(field.getFieldValue()) || !field.getFieldValue().equalsIgnoreCase(newfieldvalue)) {
									String prevFieldValue = field.getFieldValue();
									field.setFieldValue(newfieldvalue);
									addAuditlog(auditlogs, userId, userRole, fieldkey, prevFieldValue, newfieldvalue);
								}
							} else {
								AtomicReference<Boolean> multiFieldPresent = new AtomicReference<>(false);
								if (!CollectionUtils.isEmpty(field.getMultifields())) {
								
								if (CollectionUtils.isEmpty(subFieldUpdates)) {
									field.getMultifields().removeIf(item -> subSectionId.get().equalsIgnoreCase(item.getMultiFieldId()));
									multiFieldPresent.set(true);
								}
								field.getMultifields().forEach(multiField -> {
									if (multiField.getMultiFieldId().equalsIgnoreCase(subSectionId.get())) {
										multiFieldPresent.set(true);
										List<DataField> multiDataFields = multiField.getDatafields();
										
										subFieldUpdates.forEach(subUpdate -> {
											String newsubfieldvalue = !StringUtils.isEmpty(subUpdate.getNewValue()) ? subUpdate.getNewValue().trim() : null;
											String subfieldkey = subUpdate.getField();
											AtomicReference<Boolean> firsttimesubfieldupdate = new AtomicReference<>(true);
											multiDataFields.forEach(subField -> {
												if (subField.getFieldId().equalsIgnoreCase(subfieldkey)) {
													firsttimesubfieldupdate.set(false);
													firsttimeupdate.set(false);
													String prevValue = subField.getFieldValue();
													subField.setFieldValue(newsubfieldvalue);
													addAuditlog(auditlogs, userId, userRole, subfieldkey, prevValue, newsubfieldvalue);
												}
											});
											if (firsttimesubfieldupdate.get()) {
												DataField dataField = new DataField();
												dataField.setFieldId(subfieldkey);
												dataField.setFieldValue(newsubfieldvalue);
												multiDataFields.add(dataField);
												firsttimeupdate.set(false);
												addAuditlog(auditlogs, userId, userRole, subfieldkey, null, newsubfieldvalue);
											}
										});
									}
								});
								}
								if (!multiFieldPresent.get()) {
									firsttimeupdate.set(false);
									MultiField multiField = new MultiField();
									multiField.setMultiFieldId(subSectionId.get());
									List<DataField> multiDataFields = new ArrayList<>();
									subFieldUpdates.forEach(subUpdate -> {
										String newsubfieldvalue = !StringUtils.isEmpty(subUpdate.getNewValue()) ? subUpdate.getNewValue().trim() : null;
										String oldsubfieldvalue = !StringUtils.isEmpty(subUpdate.getOldValue()) ? subUpdate.getOldValue().trim() : null;
										String subfieldkey = subUpdate.getField();
										DataField dataField = new DataField();
										dataField.setFieldId(subfieldkey);
										dataField.setFieldValue(newsubfieldvalue);
										multiDataFields.add(dataField);
										if (StringUtils.compare(newsubfieldvalue, oldsubfieldvalue) != 0)
											addAuditlog(auditlogs, userId, userRole, subfieldkey, null, newsubfieldvalue);
									});
									multiField.setDatafields(multiDataFields.stream().distinct().collect(Collectors.toList()));
									List<MultiField> multiFields = new ArrayList<>();
									if (!CollectionUtils.isEmpty(field.getMultifields())) {					
										multiFields.addAll(field.getMultifields());
									}
									multiFields.add(multiField);
									field.setMultifields(multiFields);
								}
							}						
						}
					});
					if (firsttimeupdate.get()) {
						DataField dataField = new DataField();
						dataField.setFieldId(fieldkey);
						dataField.setFieldValue(newfieldvalue);
						datafields.add(dataField);
						addAuditlog(auditlogs, userId, userRole, fieldkey, null, newfieldvalue);
					}
				});
				List<DataField> updatedDataFields = AutoCalculateFunction
						.updateAutoCalculatedFields(fieldUpdates, datafields, palDao.findAllPALFields());
				palProduct.setDatafields(updatedDataFields.stream().filter(field ->
						!StringUtils.isEmpty(field.getFieldValue())).collect(Collectors.toList()));
				palDao.savePALProduct(palProduct);
				palDao.updatePALAuditLog(productId, auditlogs);
				PALProject palProject = palDao.findPALProjectById(palProduct.getProjectId());
				notificationProjectStatus = new ArrayList<>(Arrays.asList(Status.FINALISE_STAGE.getStatus(), Status.POST_FINALISE_GATE.getStatus()));
				FieldUpdate fieldUpdate = fieldUpdates.stream().filter(field -> field.getField().equalsIgnoreCase(ApplicationConstant.STATUS_FIELD))
						.findFirst().orElse(null);

				// Notify users for a product status change when project staus is in post-creative, finalise and post-finalise stage
				if (kafkaEnabled && !ObjectUtils.isEmpty(fieldUpdate) && notificationProjectStatus.contains(palProject.getStatus())) {
					List<DataField> palProductDataFields = palProduct.getDatafields();
					palProductDataFields.add(DataField.builder().fieldId(ApplicationConstant.PREVIOUS_STATUS)
							.fieldValue(previousProductStatus).build());
					palProduct.setDatafields(palProductDataFields);
					notificationService.sendEmailMessage(palProject, palProduct, MessageTemplate.UPDATE_PRODUCT_STATUS);
				}
				palProductResponse = getPALProductResponse(palProductRequest, ApplicationConstant.INFORMATION_TAB, userDetails.getOrganizations());
			}
		}
		long endTime = System.currentTimeMillis();
		log.info("ProductAttributeListingServiceImpl > Completed updatePALProductInformation for request :: {}, Time Taken :: {}", palProductUpdateRequest, endTime-startTime);
		log.debug("ProductAttributeListingServiceImpl > Response for updatePALProductInformation for request :: {} is :: {}", palProductUpdateRequest, palProductResponse);
				
		return palProductResponse;

	}

	private void validateUserAccessToProduct(PALProduct palProduct, List<String> organizations) {
		boolean valid = false;
		String productId = null;
		if (!ObjectUtils.isEmpty(palProduct)) {
			productId = palProduct.getId();
			List<DataField> dataFields = palProduct.getDatafields();
			String supplier = getDataFieldValue(dataFields, ApplicationConstant.SUPPLIER_SITE_CODE_FIELD);
			if (!StringUtils.isEmpty(supplier) && organizations.contains(supplier)) {
				valid = true;
			}
		}
		if (!valid) {
			log.error("ProductAttributeListingServiceImpl > Unauthorized to view PAL Product :: {}", productId);
			throw new PALServiceException(ErrorCode.UNAUTHORIIZED);
		}
	}

	private boolean isValidFieldUpdate(FieldUpdate field, UserDetails userDetails) {
		String userRole = userDetails.getUserRole().getRoleName();
		AccessControlInfo accessInfo = userDetails.getUserRole().getAccessControlInfoList().get(0);
		List<String> supplierCodes = userDetails.getOrganizations();
		// fail if supplier is deleting the subsection or updating the supplierCode which dosent belongs to him
		if (userRole.equalsIgnoreCase(ApplicationConstant.SUPPLIER) &&
				(StringUtils.isNotEmpty(field.getSubSectionId()) && CollectionUtils.isEmpty(field.getSubSectionFields())
				|| StringUtils.isNotEmpty(field.getNewValue()) && ApplicationConstant.SUPPLIER_SITE_CODE_FIELD.equalsIgnoreCase(field.getField()) &&
				!CollectionUtils.isEmpty(supplierCodes) && !supplierCodes.contains(field.getNewValue()))) {
			log.error("User {} does not have access to update/delete", userRole);
			throw new PALServiceException(ErrorCode.UNAUTHORIIZED);
		}

		// validate if the request fieldId is present the pal fields collection
		PALFields palField = palDao.findAllPALFields().stream()
				.filter(palFields -> palFields.getId().equals(field.getField()))
				.findFirst().orElse(null);
		if (ObjectUtils.isEmpty(palField)) {
			log.error("ProductAttributeListingServiceImpl > Invalid Field provided :: {}", field.getField());
			throw new PALServiceException(ErrorCode.INVALID_REQUEST_DATA);
		} else if (CommonUtility.isNonEditableField(palField, userRole, accessInfo)){
			log.error("ProductAttributeListingServiceImpl > User {} doesn't have access to update field {}", userRole, field.getField());
			throw new PALServiceException(ErrorCode.UNAUTHORIIZED);
		}

		String newValue = !StringUtils.isEmpty(field.getNewValue()) ? field.getNewValue().trim() : null;
		String oldValue = !StringUtils.isEmpty(field.getOldValue()) ? field.getOldValue().trim() : null;

		// validate if the non-null newValue is same as non-null oldvalue
		if (!StringUtils.isEmpty(newValue) && !StringUtils.isEmpty(oldValue) && newValue.equalsIgnoreCase(oldValue)) {
			log.error("ProductAttributeListingServiceImpl > No changes in the field :: {}", field.getField());
			throw new PALServiceException(ErrorCode.NO_CHANGES);
		}

		// validate if new value and old values are both null when subsections are not present
		if (CollectionUtils.isEmpty(field.getSubSectionFields()) && StringUtils.isEmpty(field.getSubSectionId()) &&
				StringUtils.isEmpty(newValue) && StringUtils.isEmpty(oldValue)) {
			log.error("ProductAttributeListingServiceImpl > Null values passed :: {}", field.getField());
			throw new PALServiceException(ErrorCode.INVALID_REQUEST_DATA);
		}

		// validate subsection fields
		if (!CollectionUtils.isEmpty(field.getSubSectionFields()) && !StringUtils.isEmpty(field.getSubSectionId())) {
			field.getSubSectionFields().forEach(subfield -> isValidFieldUpdate(subfield, userDetails));
		}

		return true;
	}

	private void addAuditlog(List<com.marksandspencer.foodshub.pal.domain.Auditlog> auditlogs, String userId, String userRole, String fieldkey,
			String oldfieldvalue, String newfieldvalue) {
		long startTime = System.currentTimeMillis();
		log.debug("ProductAttributeListingServiceImpl > Started addAuditlog for field :: {}", fieldkey);
		String userRoleLabel = userRole;
		String fieldLabel = fieldkey;

		PALRole palRole = listRoles().stream()
				.filter(role -> userRole.equalsIgnoreCase(role.getRole()))
				.findAny().orElse(null);
		if (!Objects.isNull(palRole)) {
			userRoleLabel = palRole.getName();
		}
		
		PALFields palField = palDao.findAllPALFields().stream()
				.filter(field -> fieldkey.equalsIgnoreCase(field.getId()))
				.findAny().orElse(null);
		if (!Objects.isNull(palField)) {
			fieldLabel = palField.getLabel();
		}

		auditlogs.add(new com.marksandspencer.foodshub.pal.domain.Auditlog(
				LocalDateTime.now(), userId, userId, fieldkey, fieldLabel, oldfieldvalue, newfieldvalue, userRole, userRoleLabel));
		long endTime = System.currentTimeMillis();
		log.debug("ProductAttributeListingServiceImpl > Completed addAuditlog for field :: {}, Time Taken :: {}", fieldkey,  endTime-startTime);
	}

	@Override
	public List<PALRole> listRoles() {
		return palDao.findAllPALRoles();
	}

	@Override
	public List<PALFields> getPalFields() {
		return palDao.findAllPALFields();
	}

	@Override
	public PALProjectResponse getPALProductList(PALProjectRequest palProjectRequest) {
		long startTime = System.currentTimeMillis();
		log.info("ProductAttributeListingServiceImpl > Started getPALProductList for request :: {}", palProjectRequest);
		
		if (StringUtils.isBlank(palProjectRequest.getProjectId()) || 
				StringUtils.isBlank(palProjectRequest.getUserRole())) {
			throw new PALServiceException(ErrorCode.INVALID_REQUEST_DATA);
		}

		UserDetails userDetails = userDetailsService.validateUserDetails(palProjectRequest.getUserRole(), null);
		if(userDetails.getUserRole().getRoleName().equals(ApplicationConstant.SUPPLIER)){
			supplierFilterProjectDetails(userDetails,palProjectRequest);
		}
		PALProjectResponse palProjectResponse = getPalProjectDetails(palProjectRequest);
		palProjectResponse.setPersonnel(null);
		
		long endTime = System.currentTimeMillis();
		log.info("ProductAttributeListingServiceImpl > Completed getPALProductList for request :: {}, Time Taken :: {}", palProjectRequest,  endTime-startTime);
		return palProjectResponse;
	}

	@Override
	public PALProjectResponse getPALProjectProgress(PALProjectRequest palProjectRequest) {
		long startTime = System.currentTimeMillis();
		log.info("ProductAttributeListingServiceImpl > Started getPALProjectProgress for request :: {}", palProjectRequest);

		if (StringUtils.isBlank(palProjectRequest.getProjectId()) ||
				StringUtils.isBlank(palProjectRequest.getUserRole())) {
			throw new PALServiceException(ErrorCode.INVALID_REQUEST_DATA);
		}
		userDetailsService.validateUserDetails(palProjectRequest.getUserRole(),null);
		PALProjectResponse palProjectResponse = getProjectProgress(palProjectRequest);
		palProjectResponse.setPersonnel(null);

		long endTime = System.currentTimeMillis();
		log.info("ProductAttributeListingServiceImpl > Completed getPALProductList for request :: {}, Time Taken :: {}", palProjectRequest,  endTime-startTime);
		return palProjectResponse;
	}

	@Override
	public List<PALProjectResponse> getPALProjectList(ProjectFilter palProjectRequest) {
		long startTime = System.currentTimeMillis();
		if (Objects.isNull(palProjectRequest.getUserRole())) {
			log.error("User Role is missing in the request :: {}", palProjectRequest);
			throw new PALServiceException(ErrorCode.MISSING_MANDATORY_FIELDS);
		}
		UserDetails userDetails = userDetailsService.validateUserDetails(palProjectRequest.getUserRole(),null);

		List<PALProjectResponse> palProjectResponseList = new ArrayList<>();
		// filter projects based on the organisation codes of a supplier
		Set<String> palProjectIds = new HashSet<>();
		if (!Objects.isNull(userDetails) && !CollectionUtils.isEmpty(userDetails.getOrganizations())
				&& ApplicationConstant.SUPPLIER.equalsIgnoreCase(palProjectRequest.getUserRole())) {
			Filter filter = new Filter();
			filter.setSuppliers(userDetails.getOrganizations());
			List<PALProduct> palProducts = palDao.findProductByFilterCondition(null,filter);
			palProjectIds = palProducts.stream().map(PALProduct::getProjectId).collect(Collectors.toSet());
			if (CollectionUtils.isEmpty(palProjectIds)) {
				throw new PALServiceException(ErrorCode.NO_DATA);
			}
		}
		List<PALProject> palProjects = palDao.findPALProjectList(palProjectIds, palProjectRequest);

		if (CollectionUtils.isEmpty(palProjects)) {
			log.error("No data available for request :: {}", palProjectRequest);
			throw new PALServiceException(ErrorCode.NO_DATA);
	    }

		List<PALProject> selectedProjects = palProjects.stream().filter(project -> isRequestedUserPresent(project.getPersonnel(), palProjectRequest.getProjectManager())).collect(Collectors.toList());

		selectedProjects.forEach(palProject -> {
			PALProjectResponse projectList = convertFromPALProject(palProject);
			projectList.setProducts(null);
			projectList.setProgress(null);
			projectList.setPersonnel(null);
			palProjectResponseList.add(projectList);
		});

		long endTime = System.currentTimeMillis();
		log.info("ProductAttributeListingServiceImpl > Completed getPALProductList for request :: {}, TimeTaken :: {}", palProjectResponseList, endTime-startTime);
		return palProjectResponseList;
	}

	private boolean isRequestedUserPresent(Personnel personnel, List<String> requestedUsers) {
		if (CollectionUtils.isEmpty(requestedUsers))
			return true;

		Set<String> projectUsers = new HashSet<>();
		if (!CollectionUtils.isEmpty(personnel.getInternal()))
			projectUsers.addAll(getPersonnelUsers(personnel.getInternal()));

		if (!CollectionUtils.isEmpty(personnel.getExternal()))
			projectUsers.addAll(getPersonnelUsers(personnel.getExternal()));

		List<String> userExists = requestedUsers.stream().filter(user -> projectUsers.contains(user)).collect(Collectors.toList());
		return !CollectionUtils.isEmpty(userExists);
	}

	private Set<String> getPersonnelUsers(List<User> usersList) {
		return usersList.stream().filter(user -> !CollectionUtils.isEmpty(user.getUsers()))
				.flatMap(user -> user.getUsers().stream()).collect(Collectors.toSet());
	}

	private PALProjectResponse getProjectProgress(PALProjectRequest palProjectRequest) {
		log.debug("ProductAttributeListingServiceImpl > Started getProjectProgress for project Id :: {}", palProjectRequest.getProjectId());

		PALProjectResponse palProjectResponse = new PALProjectResponse();

		String projectId =  palProjectRequest.getProjectId();
		String userRole = palProjectRequest.getUserRole();

		PALProject palProject = palDao.findPALProjectById(projectId);
		if (Objects.isNull(palProject)) {
			log.error("ProductAttributeListingServiceImpl > getProjectProgress > No Project available with id :: {}", projectId);
			throw new PALServiceException(ErrorCode.NO_DATA);
		}
		String templateId = palProject.getTemplateId();
		String templateName = palProject.getTemplateName();
		palProjectResponse.setId(projectId);
		palProjectResponse.setTemplateId(templateId);
		palProjectResponse.setTemplateName(templateName);
		palProjectResponse.setInformation(setProjectInformation(palProject));

		List<PALProduct> palProducts = palDao.findPALProductsByProjectId(projectId);

		if (!CollectionUtils.isEmpty(palProducts)) {
			PALTemplate palTemplate = getPALTemplateById(templateId);
			List<PALProductResponse> palProductResponses = framePalProductResponses(userRole, palProducts, palProject,
					palTemplate, ApplicationConstant.PROGRESS_TAB);
			if (!CollectionUtils.isEmpty(palProductResponses)) {
				AtomicInteger completedFields = new AtomicInteger();
				AtomicInteger totalFields = new AtomicInteger();

				List<RoleProgress> productProgress = palProductResponses.stream()
						.flatMap(progress -> progress.getProgress().getRoles().stream())
						.collect(Collectors.toList());
				Progress progress = Progress.builder()
						.roles(new ArrayList<>()).build();

				for (Map.Entry<String, List<RoleProgress>> item : productProgress.stream().collect(Collectors.groupingBy(RoleProgress::getRole, Collectors.toList()))
						.entrySet()) {
					int total = item.getValue().stream().mapToInt(RoleProgress::getTotalFields).sum();
					int completed = item.getValue().stream().mapToInt(RoleProgress::getCompletedFields).sum();
					totalFields.set(totalFields.get() + total);
					completedFields.set(completedFields.get() + completed);
					int percentage = percentageFunc.apply(completed, total);
					progress.getRoles().add(RoleProgress.builder().role(item.getKey())
							.completedFields(completed)
							.totalFields(total).percentageCompletion(percentage)
							.status(getStatus(percentage))
							.build());
				}
				progress.setTotalFields(totalFields.get());
				progress.setCompletedFields(completedFields.get());
				palProjectResponse.setProgress(progress);
			}
		}
		log.debug("ProductAttributeListingServiceImpl > Completed getProjectProgress for project Id :: {}", palProjectRequest.getProjectId());

		return palProjectResponse;
	}

	private PALProjectResponse getPalProjectDetails(PALProjectRequest palProjectRequest) {

		PALProjectResponse palProjectResponse = new PALProjectResponse();
		String projectId =  palProjectRequest.getProjectId();
		String userRole = palProjectRequest.getUserRole();
		log.debug("ProductAttributeListingServiceImpl > Started getPalProjectDetails for project Id :: {}", projectId);
		PALProject palProject = palDao.findPALProjectById(projectId);
		if (Objects.isNull(palProject)) {
			log.error("ProductAttributeListingServiceImpl > getPalProjectDetails > No Project available with id :: {}", projectId);
			throw new PALServiceException(ErrorCode.NO_DATA);
		}
		String templateId = palProject.getTemplateId();
		String templateName = palProject.getTemplateName();
		PALTemplate palTemplate = getPALTemplateById(templateId);
		if (Objects.isNull(palTemplate)) {
			log.error("ProductAttributeListingServiceImpl > getPalProjectDetails > No Template available with id :: {}", templateId);
			throw new PALServiceException(ErrorCode.INVALID_TEMPLATE_ID);
		}

		palProjectResponse.setId(projectId);
		palProjectResponse.setTemplateId(templateId);
		palProjectResponse.setTemplateName(templateName);

		palProjectResponse.setInformation(setProjectInformation(palProject));
		palProjectResponse.setPersonnel(palProject.getPersonnel());

		List<PALProduct> palProducts = palDao.findProductByFilterCondition(projectId, palProjectRequest.getFilter());
		if (!CollectionUtils.isEmpty(palProducts)) {

			List<PALProductResponse> palProductResponses = framePalProductResponses(userRole, palProducts, palProject, palTemplate, ApplicationConstant.INFORMATION_TAB);
			List<PALProductResponse> filteredPALProducts = filterPalProduct(palProjectRequest, palProductResponses);
			palProjectResponse.setProducts(setProductDetails(filteredPALProducts));
		} else if (palProjectRequest.getUserRole().equalsIgnoreCase(ApplicationConstant.SUPPLIER)){
			// For suppliers, throw unauthorised error when product list is empty.
			log.error("ProductAttributeListingServiceImpl > Unauthorized to view PAL Project :: {}", projectId);
			throw new PALServiceException(ErrorCode.NO_DATA);
		}
		log.debug("ProductAttributeListingServiceImpl > Completed getPalProjectDetails for project Id :: {}", projectId);

		return palProjectResponse;
	}

	private List<PALProductResponse> filterPalProduct(PALProjectRequest palProjectRequest,
													  List<PALProductResponse> palProductResponses) {
		if (null != palProjectRequest.getFilter()
				&& !CollectionUtils.isEmpty(palProjectRequest.getFilter().getProgressRange())) {
			return palProductResponses.stream()
					.filter(data -> isBetween(palProjectRequest.getFilter().getProgressRange(),
							data.getHeader().getPercentage()))
					.collect(Collectors.toList());
		}
		return palProductResponses;
	}

	//To filter the range based on Product completion percentage
	private static boolean isBetween(List<String> progressRange, Integer percentage) {
		for(String ranges: progressRange)
		{
			String[] range = ranges.split("-");
			int lower;
			int upper;
			if (range.length==1) {
				lower = Integer.parseInt(range[0]);
				upper = Integer.parseInt(range[0]);
			}
			else {
				lower = Integer.parseInt(range[0]);
				upper = Integer.parseInt(range[1]);
			}
			if (percentage >= lower && percentage <= upper)
				return true;
		}
		return false;
	}

	@Override
	public  PALProjectResponse getProjectDetails(PALProjectRequest palProjectRequest) {

		if (StringUtils.isBlank(palProjectRequest.getProjectId()) ||
				StringUtils.isBlank(palProjectRequest.getUserRole())) {
			throw new PALServiceException(ErrorCode.INVALID_REQUEST_DATA);
		}
		long startTime = System.currentTimeMillis();
		log.debug("ProductAttributeListingServiceImpl > Started getProjectDetails for request :: {}", palProjectRequest);

		UserDetails userDetails = userDetailsService.validateUserDetails(palProjectRequest.getUserRole(), null);
		if(userDetails.getUserRole().getRoleName().equals(ApplicationConstant.SUPPLIER)){
			supplierFilterProjectDetails(userDetails,palProjectRequest);
		}

		PALProjectResponse palProjectResponse = getPalProjectDetails(palProjectRequest);
		palProjectResponse.setProducts(null);
		long endTime = System.currentTimeMillis();

		log.debug("ProductAttributeListingServiceImpl > Completed getProjectDetails for request :: {}, TimeTaken :: {}", palProjectRequest, endTime-startTime);

		return palProjectResponse;
	}
	
	@Override
	public PALProjectResponse addPALProject(PALProjectUpdateRequest palProjectUpdateRequest) {
		long startTime = System.currentTimeMillis();
		log.info("ProductAttributeListingServiceImpl > Started addPALProject for request :: {}", palProjectUpdateRequest);

		//Check for the mandatory fields
		if (ObjectUtils.isEmpty(palProjectUpdateRequest) || ObjectUtils.isEmpty(palProjectUpdateRequest.getInformation())
				|| StringUtils.isEmpty(palProjectUpdateRequest.getInformation().getProjectName())
				|| StringUtils.isEmpty(palProjectUpdateRequest.getInformation().getProjectType())
				|| StringUtils.isEmpty(palProjectUpdateRequest.getInformation().getFinancialYear())
				|| ObjectUtils.isEmpty(palProjectUpdateRequest.getPersonnel())
				|| ObjectUtils.isEmpty(palProjectUpdateRequest.getPersonnel().getInternal())
				|| StringUtils.isEmpty(palProjectUpdateRequest.getUserRole())
				|| StringUtils.isEmpty(palProjectUpdateRequest.getUser())
				|| !palProjectUpdateRequest.getInformation().getProjectType()
						.equalsIgnoreCase(ApplicationConstant.ROLLING)
						&& palProjectUpdateRequest.getInformation().getProjectCompletionDate() == null) {
			throw new PALServiceException(ErrorCode.MISSING_MANDATORY_FIELDS);
		}

		// validate project personnel roles
		if (!ObjectUtils.isEmpty(palProjectUpdateRequest.getPersonnel().getInternal().stream()
				.filter(role -> StringUtils.isEmpty(role.getRole())).findFirst().orElse(null))) {
			throw new PALServiceException(ErrorCode.INVALID_REQUEST_DATA);
		}
		userDetailsService.validateUserDetails(palProjectUpdateRequest.getUserRole(),
				Collections.singletonList(ApplicationConstant.PROJECT_MANAGER));

		//setting default status to DRAFT
		palProjectUpdateRequest.getInformation().setStatus(Status.DRAFT.getStatus());
		//Convert request into domain object
		PALProject palProject = convertIntoPALProject(palProjectUpdateRequest);
		//Save the domain object in collection
		PALProject savedPalProject = palDao.savePALProject(palProject);
		if (kafkaEnabled)
			notificationService.sendEmailMessage(savedPalProject, null, MessageTemplate.ADD_PROJECT);
		PALProjectResponse palProjectResponse = convertFromPALProject(savedPalProject);
		long endTime = System.currentTimeMillis();
		log.info("ProductAttributeListingServiceImpl > Completed addPALProject , TimeTaken {}", endTime-startTime);

		//Return to UI with new id created
		return palProjectResponse;
	}

	@Override
	public PALExportResponse palExportData(PALExportRequest palExportRequest) {
		long startTime = System.currentTimeMillis();
		log.info("ProductAttributeListingServiceImpl > Started palExportData for request :: {}", palExportRequest);
		String projectId =  palExportRequest.getProjectId();
		String userRole = palExportRequest.getUserRole();
		List<String> productId =  palExportRequest.getProductId();

		if (StringUtils.isEmpty(projectId) && ObjectUtils.isEmpty(productId)|| StringUtils.isEmpty(userRole)) {
			throw new PALServiceException(ErrorCode.INVALID_REQUEST_DATA);
		}
		UserDetails userDetails = userDetailsService.validateUserDetails(palExportRequest.getUserRole(), null);
		List<String> suppliers = null;
		if (ApplicationConstant.SUPPLIER.equalsIgnoreCase(userDetails.getUserRole().getRoleName())) {
			suppliers = userDetails.getOrganizations();
			if (ObjectUtils.isEmpty(suppliers)) {
				log.error("ProductAttributeListingServiceImpl > palExportData > No Data available for the request {}", palExportRequest);
				throw new PALServiceException(ErrorCode.NO_DATA);
			}
		}

		try{

			PALProject palProject = palDao.findPALProjectById(projectId);
			if (Objects.isNull(palProject)) {
				log.error("ProductAttributeListingServiceImpl > palExportData > No Project available with id :: {}",
						projectId);
				throw new PALServiceException(ErrorCode.NO_DATA);
			}

			String templateId = palProject.getTemplateId();
			PALTemplate palTemplate = getPALTemplateById(templateId);
			if (Objects.isNull(palTemplate)) {
				log.error("ProductAttributeListingServiceImpl > palExportData > No Template available with id :: {}",
						templateId);
				throw new PALServiceException(ErrorCode.NO_DATA);
			}

			List<PALProduct> palProducts = palDao.findPALProducts(projectId, productId, suppliers);

			log.debug("ProductAttributeListingServiceImpl > palExportData > {} PAL Products identified for project :: {} & products {}",
					palProducts.size(), projectId, productId);

			List<PALProductResponse> palProductResponses = new ArrayList<>();
			if (!CollectionUtils.isEmpty(palProducts)) {
				palProductResponses = framePalProductResponses(userRole, palProducts, palProject, palTemplate, ApplicationConstant.INFORMATION_TAB);
			}
			List<PALRole> roles = palDao.findAllPALRoles();
			String fileName = palProject.getProjectName().trim();
			PALExportResponse response;
			if (ApplicationConstant.SUPPLIER.equalsIgnoreCase(userRole)) {
				response = new PALExportResponse(fileName+ApplicationConstant.DOWNLOAD_EXCEL_EXTENSION,
						exportHelper.createExcel(palProductResponses, roles, palProject));
			} else {
				response = new PALExportResponse(fileName+ApplicationConstant.DOWNLOAD_MACRO_EXCEL_EXTENSION,
						exportHelper.createExcelwithMacro(userRole, palProductResponses, roles, palProject, ApplicationConstant.STANDARD_DOWNLOAD_TEMPLATE));
			}
			long endTime = System.currentTimeMillis();
			log.info("ProductAttributeListingServiceImpl > Completed palExportData for request :: {}, TimeTaken :: {}",
					palExportRequest, endTime-startTime);
			return response;

		} catch (PALServiceException ex) {
			throw ex;
		} catch (Exception ex){
			log.error("ProductAttributeListingServiceImpl > palExportData > Export data as excel has been failed :: {}", ex.getMessage());
			throw new PALServiceException(ErrorCode.GENERAL_ERROR);
		}
	}

	@Override
	public PALProjectResponse updatePALProjectPersonnel(PALProjectPersonnelUpdateRequest palProjectPersonnelUpdateRequest) {
		long startTime = System.currentTimeMillis();
		log.info("ProductAttributeListingServiceImpl > Started updatePALProductPersonnel for id :: {}", palProjectPersonnelUpdateRequest);
		PALProjectResponse palProjectResponse;
		if (StringUtils.isBlank(palProjectPersonnelUpdateRequest.getProjectId()) ||
				StringUtils.isBlank(palProjectPersonnelUpdateRequest.getUserRole()) ||
				StringUtils.isBlank(palProjectPersonnelUpdateRequest.getUser()) ||
				CollectionUtils.isEmpty(palProjectPersonnelUpdateRequest.getPersonnelUpdates())) {
			log.error("ProductAttributeListingServiceImpl > updatePALProjectPersonnel > Invalid Request :: {}", palProjectPersonnelUpdateRequest);
			throw new PALServiceException(ErrorCode.INVALID_REQUEST_DATA);
		}
		List<String> accessibleUserRoleList = getRolesForUpdateAccess(ApplicationConstant.PROJECT_UPDATE_ACCESS);
		userDetailsService.validateUserDetails(palProjectPersonnelUpdateRequest.getUserRole(), accessibleUserRoleList);

		List<PersonnelUpdate> personnelUpdates = palProjectPersonnelUpdateRequest.getPersonnelUpdates();
		personnelUpdates.forEach(update -> {
			if(StringUtils.isEmpty(update.getField())){
				log.error("ProductAttributeListingServiceImpl > updatePALProjectPersonnel > Invalid Request :: {}", palProjectPersonnelUpdateRequest);
				throw new PALServiceException(ErrorCode.INVALID_REQUEST_DATA);
			} else if (ApplicationConstant.PROJECT_MANAGER.equalsIgnoreCase(update.getField()) && ObjectUtils.isEmpty(update.getNewValue())) {
				log.error("ProductAttributeListingServiceImpl > updatePALProjectPersonnel > Atleast One user should be selected :: {}", palProjectPersonnelUpdateRequest);
				throw new PALServiceException(String.format("Atleast one user should be selected for %s role", update.getField().toUpperCase()));
			}
		});
		try{
			String projectId = palProjectPersonnelUpdateRequest.getProjectId();
			String userRole = palProjectPersonnelUpdateRequest.getUserRole();
			PALProject palProject = palDao.findPALProjectById(projectId);

			if (!Objects.isNull(palProject)) {
				Personnel personnel = palProject.getPersonnel();
				AtomicBoolean roleExists = new AtomicBoolean(false);
				personnelUpdates.forEach(update -> {
					personnel.getInternal().forEach(person -> {
						if (person.getRole().equalsIgnoreCase(update.getField())) {
							person.setUsers(update.getNewValue());
							roleExists.set(true);
						}
					});
					if (!roleExists.get())
						personnel.getInternal().add(User.builder()
							.role(update.getField())
							.users(update.getNewValue()).build());
				});
				palProject.setPersonnel(personnel);
				palDao.savePALProject(palProject);
			} else {
				log.error("ProductAttributeListingServiceImpl > updatePALProjectPersonnel > Project Not Found :: {}", palProjectPersonnelUpdateRequest);
				throw new PALServiceException(ErrorCode.NO_DATA);
			}
			PALProjectRequest palProjectRequest = new PALProjectRequest();
			palProjectRequest.setProjectId(projectId);
			palProjectRequest.setUserRole(userRole);
			palProjectResponse = getPalProjectDetails(palProjectRequest);
			palProjectResponse.setProducts(null);
		} catch (PALServiceException ex) {
			throw ex;
		} catch (Exception ex){
			log.error("ProductAttributeListingServiceImpl > updatePALProjectPersonnel > Personnel Updates failed :: {}", ex.getMessage());
			throw new PALServiceException(ErrorCode.GENERAL_ERROR);
		}
		long endTime = System.currentTimeMillis();
		log.info("ProductAttributeListingServiceImpl > Completed updatePALProjectPersonnel for request :: {}, Time Taken :: {}", palProjectPersonnelUpdateRequest,  endTime-startTime);
		log.debug("ProductAttributeListingServiceImpl > Response for updatePALProjectPersonnel for request :: {} is :: {}", palProjectPersonnelUpdateRequest, palProjectResponse);

		return palProjectResponse;
	}

	private List<String> getRolesForUpdateAccess(String accessConfig) {
		PALConfiguration palConfiguration = getPALConfiguration(accessConfig);
		return !Objects.isNull(palConfiguration) ? palConfiguration.getValues() : new ArrayList<>();
	}

	private List<Product> setProductDetails(List<PALProductResponse> palProductResponses) {
		List<Product> products = new ArrayList<>();
		palProductResponses.forEach(productResponse -> {
			Header header = productResponse.getHeader();
			Product product = new Product();
			product.setData(setProductDetails(header));
			if (!CollectionUtils.isEmpty(header.getChildProducts())) 
					product.setChildren(setChildProductDetails(header.getChildProducts()));
			products.add(product);
		});
		
		return products;
	}

	private List<Product> setChildProductDetails(List<ChildProduct> childProducts) {
		List<Product> childProductsInfo = new ArrayList<>();
		
		childProducts.forEach(child -> {
			Header header = new Header();
			header.setProductName(child.getProductName());
			header.setSupplierName(child.getSupplierName());
			header.setUpc(child.getUpc());
			Product product = new Product();
			product.setData(header);
			childProductsInfo.add(product);
		});
		
		return childProductsInfo;
	}
	
	private Header setProductDetails(Header header) {
		return Header.builder()
				.productId(header.getProductId())
				.productName(header.getProductName())
				.status(header.getStatus())
				.supplierCode(header.getSupplierCode())
				.supplierName(header.getSupplierName())
				.weight(header.getWeight())
				.percentage(header.getPercentage())
				.productFileType(header.getProductFileType())
				.productType(header.getProductType())
				.category(header.getCategory())
				.upc(header.getUpc())
				.subRange(header.getSubRange())
				.build();
	}

	private Information setProjectInformation(PALProject palProject) {
		return Information.builder()
				.id(palProject.getId())
				.projectName(palProject.getProjectName())
				.projectCompletionDate(palProject.getProjectCompletionDate())
				.projectType(palProject.getProjectType())
				.financialYear(palProject.getFinancialYear())
				.comments(palProject.getComments())
				.status(palProject.getStatus())
				.templateId(palProject.getTemplateId())
				.templateName(palProject.getTemplateName())
				.build();
	}
	
	//Convert request into domain object
	private PALProject convertIntoPALProject(PALProjectUpdateRequest palProjectUpdateRequest) {
		
		PALProject palProject = new PALProject();
		Information information = palProjectUpdateRequest.getInformation();
		if(!isValidSelectionValue(ApplicationConstant.PROJECT_STATUS, information.getStatus()) ||
				!isValidSelectionValue(ApplicationConstant.PROJECT_TYPE, information.getProjectType())){
			log.error("Invalid dropdown valies passed in the request :  {}", palProjectUpdateRequest);
			throw new PALServiceException(ErrorCode.INVALID_REQUEST_DATA);
		}

		palProject.setId(palProjectUpdateRequest.getId());
		palProject.setProjectName(information.getProjectName());
		palProject.setProjectCompletionDate(information.getProjectCompletionDate());
		palProject.setProjectType(information.getProjectType());
		palProject.setFinancialYear(information.getFinancialYear());
		palProject.setStatus(information.getStatus());
		palProject.setComments(information.getComments());

		if(!StringUtils.isEmpty(information.getTemplateId())) {
			PALTemplate palTemplate = getPALTemplateById(information.getTemplateId());
			if(!Objects.isNull(palTemplate) && palTemplate.getTemplateName().equalsIgnoreCase(information.getTemplateName())) {
				palProject.setTemplateId(information.getTemplateId());
				palProject.setTemplateName(information.getTemplateName());
			}else {
				throw new PALServiceException(ErrorCode.INVALID_TEMPLATE_ID);
			}
		}else if(palProjectUpdateRequest.getId()== null) {
			PALTemplate palTemplate = getPALTemplateByName(ApplicationConstant.STANDARD);
			if (!Objects.isNull(palTemplate)) {
				palProject.setTemplateId(palTemplate.getId());
				palProject.setTemplateName(palTemplate.getTemplateName());
			}
		}
		palProject.setPersonnel(palProjectUpdateRequest.getPersonnel());
		return palProject;

	}

	private boolean isValidSelectionValue(String configId, String selectedValue) {
		if (!StringUtils.isEmpty(selectedValue) && !StringUtils.isEmpty(configId)) {
			PALConfiguration palConfiguration = getPALConfiguration(configId);
			return !Objects.isNull(palConfiguration) && palConfiguration.getValues().contains(selectedValue);
		}
		return true;
	}

	//Convert domain object into ui response
	private PALProjectResponse convertFromPALProject(PALProject palProject) {
		if(palProject != null) {
			return PALProjectResponse.builder()
				.id(palProject.getId())
				.information(setProjectInformation(palProject))
				.templateId(palProject.getTemplateId())
				.templateName(palProject.getTemplateName())
				.personnel(palProject.getPersonnel())
				.build();
		}else {
			return null;
		}
	}

	public PALProjectResponse updatePALProject(PALProjectUpdateRequest palProjectUpdateRequest) {

		//throw error for UI disabled fields
		if(ObjectUtils.isNotEmpty(palProjectUpdateRequest.getInformation().getFinancialYear())
		   || ObjectUtils.isNotEmpty(palProjectUpdateRequest.getInformation().getProjectType())){
			throw new PALServiceException(ErrorCode.INVALID_REQUEST_DATA);
		}

		// Check for the mandatory fields
		if (ObjectUtils.isEmpty(palProjectUpdateRequest)
			|| StringUtils.isEmpty(palProjectUpdateRequest.getId())
			|| ObjectUtils.isEmpty(palProjectUpdateRequest.getInformation())
			|| StringUtils.isEmpty(palProjectUpdateRequest.getUser())
			|| StringUtils.isEmpty(palProjectUpdateRequest.getUserRole())
			|| StringUtils.isEmpty(palProjectUpdateRequest.getInformation().getProjectName())
			&& !(StringUtils.isNotEmpty(palProjectUpdateRequest.getInformation().getProjectType())
				|| StringUtils.isNotEmpty(palProjectUpdateRequest.getInformation().getFinancialYear())
				|| StringUtils.isNotEmpty(palProjectUpdateRequest.getInformation().getStatus())
				|| StringUtils.isNotEmpty(palProjectUpdateRequest.getInformation().getTemplateId())
				|| StringUtils.isNotEmpty(palProjectUpdateRequest.getInformation().getTemplateName())
				|| palProjectUpdateRequest.getInformation().getComments() != null
				|| palProjectUpdateRequest.getInformation().getProjectCompletionDate() != null)) {
			throw new PALServiceException(ErrorCode.MISSING_MANDATORY_FIELDS);
		}

		PALConfiguration palConfiguration = getPALConfiguration(ApplicationConstant.PROJECT_UPDATE_ACCESS);
		List<String> accessibleUserRoleList = !Objects.isNull(palConfiguration) ? palConfiguration.getValues() : new ArrayList<>();
		userDetailsService.validateUserDetails(palProjectUpdateRequest.getUserRole(), accessibleUserRoleList);

		PALProject palProject = convertIntoPALProject(palProjectUpdateRequest);
		
		PALProject updatedPalProject = palDao.updatePALProject(palProject);
		
		if(updatedPalProject == null) {
			throw new PALServiceException(ErrorCode.INVALID_PROJECT_ID);
		}

		if (kafkaEnabled && StringUtils.isNotEmpty(palProjectUpdateRequest.getInformation().getStatus())
				&& !palProject.getStatus().equalsIgnoreCase(Status.DRAFT.getStatus()))

			notificationService.sendEmailMessage(updatedPalProject, null, MessageTemplate.UPDATE_PROJECT_STATUS);
		return convertFromPALProject(updatedPalProject);
	}

	@Override
	public List<PALConfiguration> getPALConfigurations(List<String> palConfigIds) {
		List<PALConfiguration> palConfigurations = palDao.findALLPALConfiguration();
		if (!CollectionUtils.isEmpty(palConfigIds))
			return palConfigurations.stream().filter(config -> palConfigIds.contains(config.getId()))
					.collect(Collectors.toList());
		else
			return palConfigurations;
	}

	@Override
	public List<PALRole> listProjectTemplateRoles(String projectId) {
		long startTime = System.currentTimeMillis();
		log.info("ProductAttributeListingServiceImpl > Started listProjectTemplateRoles for project id :: {}", projectId);
		List<PALRole> palRoles = null;
		PALProject palProject = palDao.findPALProjectById(projectId);

		//throw error if project is not available
		if(ObjectUtils.isEmpty(palProject)) {
			log.error("ProductAttributeListingServiceImpl > listProjectTemplateRoles > No Project available for ID :: {}", projectId);
			throw new PALServiceException(ErrorCode.INVALID_PROJECT_ID);
		}

		PALTemplate palTemplate = getPALTemplateById(palProject.getTemplateId());

		//throw error if project is not available
		if(ObjectUtils.isEmpty(palTemplate)) {
			log.error("ProductAttributeListingServiceImpl > listProjectTemplateRoles > No Template available for Project ID :: {}", projectId);
			throw new PALServiceException(ErrorCode.INVALID_TEMPLATE_ID);
		}

		if (palTemplate != null) {
			Set<String> fieldOwners = palTemplate.getSections().stream()
					.flatMap(section -> section.getSectionFields().stream())
					.map(PALFields::getOwner).collect(Collectors.toSet());
			Set<String> subFieldOwners = palTemplate.getSections().stream()
					.filter(section -> !CollectionUtils
							.isEmpty(section.getSubSections()))
					.flatMap(section -> section.getSubSections().stream()
							.flatMap(subSection -> subSection
									.getSubSectionFields().stream()))
					.map(PALFields::getOwner).collect(Collectors.toSet());
			fieldOwners.addAll(subFieldOwners);
			palRoles = palDao.findAllPALRoles().stream()
					.filter(role -> fieldOwners.contains(role.getRole()))
					.collect(Collectors.toList());
		}
		
		long endTime = System.currentTimeMillis();
		log.info("ProductAttributeListingServiceImpl > Completed listProjectTemplateRoles for project id :: {}, Time Taken :: {} ms", projectId, endTime-startTime);
		return palRoles;		
	}

	@Override
	public DuplicateProductResponse duplicateProducts(DuplicateProductRequest duplicateProductRequest) {
		long startTime = System.currentTimeMillis();
		log.info("ProductAttributeListingServiceImpl > Started duplicateProducts for request :: {}", duplicateProductRequest);

		if (ObjectUtils.isEmpty(duplicateProductRequest) ||
				StringUtils.isEmpty(duplicateProductRequest.getModelProductId()) ||
				StringUtils.isEmpty(duplicateProductRequest.getUserRole()) ||
				CollectionUtils.isEmpty(duplicateProductRequest.getProducts())) {
			log.warn("ProductAttributeListingServiceImpl > duplicateProducts > Missing mandatory fields in request :: {}",
					duplicateProductRequest);
			throw new PALServiceException(ErrorCode.MISSING_MANDATORY_FIELDS);
		}

		duplicateProductRequest.getProducts().forEach(request -> {
			if (ObjectUtils.isEmpty(request) ||
					StringUtils.isEmpty(request.getProjectId()) ||
					StringUtils.isEmpty(request.getTemplateId()) ||
					CollectionUtils.isEmpty(request.getDataFields()) ||
					ObjectUtils.isEmpty(request.getPersonnel())) {
				log.warn("ProductAttributeListingServiceImpl > duplicateProducts > Missing mandatory fields in request :: {}",
						duplicateProductRequest);
				throw new PALServiceException(ErrorCode.MISSING_MANDATORY_FIELDS);
			}
		});

		List<String> duplicateProductAccessList = getRolesForUpdateAccess(ApplicationConstant.PRODUCT_DUPLICATE_ACCESS);
		userDetailsService.validateUserDetails(duplicateProductRequest.getUserRole(), duplicateProductAccessList);

		PALProduct modelProduct = palDao.findPALProductById(duplicateProductRequest.getModelProductId());
		List<PALProject> palProjects = new ArrayList<>();
		validateDuplicateProductRequest(modelProduct, duplicateProductRequest, palProjects);

		List<PALProduct> palProductList = new ArrayList<>();
		duplicateProductRequest.getProducts().forEach(request -> {
			request.getDataFields().add(DataField.builder().fieldId(ApplicationConstant.STATUS_FIELD)
					.fieldValue(Status.IN_PROGRESS.getStatus())
					.build());
			List<DataField> dataFields = copyModelDataFields(request.getDataFields(), modelProduct.getDatafields());
			PALProduct palProduct = new PALProduct();
			palProduct.setProjectId(request.getProjectId());
			palProduct.setTemplateId(request.getTemplateId());
			palProduct.setDatafields(dataFields);
			palProduct.setPersonnel(request.getPersonnel());
			palProductList.add(palProduct);
		});

		DuplicateProductResponse duplicateProductResponse = new DuplicateProductResponse();
		List<ProductUpdateStatus> productUpdates = new ArrayList<>();
		if (!CollectionUtils.isEmpty(palProductList)) {
			List<PALProduct> updatedProducts = palDao.saveAllProducts(palProductList);
			updatedProducts.forEach(product -> {
				PALProject project = palProjects.stream().filter(proj -> proj.getId().equalsIgnoreCase(product.getProjectId()))
						.findFirst().orElse(null);
				if (kafkaEnabled && !project.getStatus().equalsIgnoreCase(Status.DRAFT.getStatus())) {
					notificationService.sendEmailMessage(project, product, MessageTemplate.ADD_PRODUCT);
				}
				String url = String.format(productUrl, product.getProjectId(), product.getId());				ProductUpdateStatus update = new ProductUpdateStatus();
				update.setProductId(product.getId());
				update.setProductName(getDataFieldValue(product.getDatafields(), ApplicationConstant.PRODUCT_TITLE_FIELD));
				update.setUpdateStatus(ApplicationConstant.SUCCESS);
				update.setProductUrl(url);
				productUpdates.add(update);
			});
			duplicateProductResponse.setProductUpdateStatus(productUpdates);
		}

		long endTime = System.currentTimeMillis();
		log.info("ProductAttributeListingServiceImpl > Completed duplicateProducts for request :: {}, Time Taken :: {} ms",
				duplicateProductRequest, endTime-startTime);
		return duplicateProductResponse;
	}

	private List<DataField> copyModelDataFields(List<DataField> requestDataFields, List<DataField> modelDataFields) {
		List<String> requestFieldIds = requestDataFields.stream().map(DataField::getFieldId).collect(Collectors.toList());
		List<DataField> dataFields = new ArrayList<>(modelDataFields);
		dataFields.removeIf(field -> requestFieldIds.contains(field.getFieldId()));
		for (DataField field : dataFields) {
			requestDataFields.forEach(reqField -> {
				if (reqField.getFieldId().equalsIgnoreCase(field.getFieldId())) {
					field.setFieldValue(reqField.getFieldValue());
					field.setMultifields(reqField.getMultifields());
				}
			});
		}
		dataFields.addAll(requestDataFields);
		return dataFields.stream().distinct().collect(Collectors.toList());
	}

	private void validateDuplicateProductRequest(PALProduct modelProduct, DuplicateProductRequest duplicateProductRequest,
												 List<PALProject> palProjects) {
		if (ObjectUtils.isEmpty(modelProduct)) {
			log.warn("ProductAttributeListingServiceImpl > duplicateProducts > Model Product not available :: {}",
					duplicateProductRequest.getModelProductId());
			throw new PALServiceException(ErrorCode.INVALID_PRODUCT_ID);
		}

		String templateId = modelProduct.getTemplateId();
		List<String> invalidTemplateIds = duplicateProductRequest.getProducts().stream()
				.filter(product -> !product.getTemplateId().equalsIgnoreCase(templateId))
				.map(PALProductCreateRequest::getTemplateId).distinct()
				.collect(Collectors.toList());

		if (ObjectUtils.isNotEmpty(invalidTemplateIds)) {
			log.warn("ProductAttributeListingServiceImpl > duplicateProducts > Template mismatch between model and duplicate products :: {}, " +
							"model product template Id :: {} , mismatch template Ids :: {}",
					duplicateProductRequest, templateId, invalidTemplateIds);
			throw new PALServiceException(ErrorCode.INVALID_TEMPLATE_ID);
		}

		PALTemplate palTemplate = getPALTemplateById(templateId);
		if (ObjectUtils.isEmpty(palTemplate)) {
			log.warn("ProductAttributeListingServiceImpl > duplicateProducts > Model Product Template not available :: {}",
					templateId);
			throw new PALServiceException(ErrorCode.INVALID_TEMPLATE_ID);
		}

		String projectId = modelProduct.getProjectId();
		List<String> projectIds = duplicateProductRequest.getProducts().stream()
				.map(PALProductCreateRequest::getProjectId).distinct()
				.collect(Collectors.toList());
		if (!projectIds.contains(projectId))
			projectIds.add(projectId);

		List<PALProject> dbProjects = palDao.findPALProjectList(new HashSet<>(projectIds), null);
		if (!CollectionUtils.isEmpty(dbProjects))
			palProjects.addAll(dbProjects);

		if (CollectionUtils.isEmpty(palProjects)) {
			log.warn("ProductAttributeListingServiceImpl > duplicateProducts > Projects not available for Ids :: {}",
					projectIds);
			throw new PALServiceException(ErrorCode.INVALID_PROJECT_ID);
		}

		List<String> dbProjectIds = palProjects.stream().map(PALProject::getId).distinct().collect(Collectors.toList());
		List<String> invalidProjects = projectIds.stream().filter(id -> !dbProjectIds.contains(id))
				.distinct().collect(Collectors.toList());

		if (!CollectionUtils.isEmpty(invalidProjects)) {
			log.warn("ProductAttributeListingServiceImpl > duplicateProducts > Projects not available for Ids :: {}",
					invalidProjects);
			throw new PALServiceException(ErrorCode.INVALID_PROJECT_ID);
		}
	}

	/**
	 *
	 * set the supplier code in filter object
	 *
	 * @param userDetails userDetails
	 * @param palProjectRequest projectRequest
	 */
	public void supplierFilterProjectDetails(UserDetails userDetails, PALProjectRequest palProjectRequest){
		if(ObjectUtils.isEmpty(palProjectRequest.getFilter())){
			  palProjectRequest.setFilter(Filter.builder().suppliers(userDetails.getOrganizations()).build());
		}
		else {
			palProjectRequest.getFilter().setSuppliers(userDetails.getOrganizations());
		}
		log.debug("PalProjectDetails list of product with supplier org: {}",userDetails.getOrganizations());
	}

	@Override
	public BulkProductUpdateResponse bulkUpdateInformation(BulkProductUpdateRequest bulkProductUpdateRequest) {
		long startTime = System.currentTimeMillis();
		log.info("ProductAttributeListingServiceImpl > Started bulkUpdateInformation for request :: {}", bulkProductUpdateRequest);
		UserDetails userDetails = validateBulkUpdateProductRequest(bulkProductUpdateRequest);
		String userRole = bulkProductUpdateRequest.getUserRole();
		String userName = bulkProductUpdateRequest.getUser();

		Map<String, List<DataField>> productFields = CommonUtility.convertBulkRequestToMapObject(bulkProductUpdateRequest);

		List<PALProduct> palProducts = palDao.findPALProducts(null, new ArrayList<>(productFields.keySet()), null);
		List<String> validProductList = palProducts.stream().map(PALProduct::getId).collect(Collectors.toList());
		List<String> invalidProducts = productFields.keySet().stream().filter(product -> !validProductList.contains(product))
				.collect(Collectors.toList());
		if (!CollectionUtils.isEmpty(invalidProducts)) {
			log.error("ProductAttributeListingServiceImpl > bulkUpdateInformation > Invalid Product Ids :: {}",
					invalidProducts);
			throw new PALServiceException(ErrorCode.INVALID_PRODUCT_ID);
		}


		if (ApplicationConstant.SUPPLIER.equalsIgnoreCase(userDetails.getUserRole().getRoleName()))
			palProducts.forEach(product -> validateUserAccessToProduct(product, userDetails.getOrganizations()));

		Map<String, List<DataField>> upsertProductFields = frameUpsertProductFields(productFields, palProducts);

		List<PALProduct> updatedProducts = palDao.upsertProductFields(palProducts, upsertProductFields, userRole, userName);
		List<ProductUpdateStatus> productUpdateStatuses = getProductUpdateStatus(productFields, updatedProducts, palProducts);
		long endTime = System.currentTimeMillis();
		log.info("ProductAttributeListingServiceImpl > Completed bulkUpdateInformation for request :: {}, TimeTaken :: {}",
				bulkProductUpdateRequest, endTime-startTime);
		return frameBulkUpdateResponse(productUpdateStatuses);
	}

	private Map<String, List<DataField>> frameUpsertProductFields(Map<String, List<DataField>> productFields, List<PALProduct> products) {
		Map<String, List<DataField>> upsertProductFields = new HashMap<>();
		for (Map.Entry<String, List<DataField>> upsertProduct : productFields.entrySet()) {
			String productId = upsertProduct.getKey();
			List<DataField> dataFields = upsertProduct.getValue();
			Optional<PALProduct> palProduct = products.stream().filter(product -> productId.equalsIgnoreCase(product.getId()))
					.findFirst();
			List<DataField> changedFields = new ArrayList<>();
			dataFields.forEach(dataField -> {
				String fieldId = dataField.getFieldId();
				String fieldValue = dataField.getFieldValue();
				String dbFieldValue = CommonUtility.getDataFieldValue(palProduct.get().getDatafields(), fieldId);
				if (!StringUtils.equalsIgnoreCase(dbFieldValue, fieldValue)) {
					changedFields.add(dataField);
				}
			});
			if (!CollectionUtils.isEmpty(changedFields)) {
				Set<String> updatableFields = new HashSet<>();
				if (palProduct.isPresent()) {
					PALProduct updatableProduct = new PALProduct();
					updatableProduct.setDatafields(AutoCalculateFunction.mergedDataFields(changedFields, palProduct.get().getDatafields()));
					dataFields.forEach(dataField -> {
						String fieldId = dataField.getFieldId();
						AutoCalculateFunction.findCalculatableFields(fieldId, getPalFields(), updatableFields);
						List<DataField> autoCalculatedDataFields = AutoCalculateFunction.getAutoCalculatedField(fieldId,
								updatableProduct.getDatafields(), getPalFields());
						updatableProduct.setDatafields(AutoCalculateFunction.mergedDataFields(autoCalculatedDataFields, updatableProduct.getDatafields()));
					});

					if (!CollectionUtils.isEmpty(updatableFields)) {
						changedFields.addAll(updatableProduct.getDatafields().stream()
								.filter(field -> updatableFields.contains(field.getFieldId()))
								.collect(Collectors.toList()));
					}
				}

				upsertProductFields.put(upsertProduct.getKey(), changedFields.stream().distinct().collect(Collectors.toList()));
			}
		}
		return upsertProductFields;
	}

	private BulkProductUpdateResponse frameBulkUpdateResponse(List<ProductUpdateStatus> productUpdateStatuses) {
		BulkProductUpdateResponse bulkUpdateResponse = new BulkProductUpdateResponse();
		List<ProductUpdateStatus> updateStatuses = productUpdateStatuses.stream()
				.filter(product -> !ApplicationConstant.FAILED.equalsIgnoreCase(product.getUpdateStatus()))
				.collect(Collectors.toList());
		if (!CollectionUtils.isEmpty(updateStatuses))
			bulkUpdateResponse.setSuccessProducts(updateStatuses);

		updateStatuses = productUpdateStatuses.stream()
				.filter(product -> ApplicationConstant.FAILED.equalsIgnoreCase(product.getUpdateStatus()))
				.collect(Collectors.toList());
		if (!CollectionUtils.isEmpty(updateStatuses))
			bulkUpdateResponse.setFailedProducts(updateStatuses);
		return bulkUpdateResponse;
	}

	private UserDetails validateBulkUpdateProductRequest(BulkProductUpdateRequest bulkProductUpdateRequest) {
		if (ObjectUtils.isEmpty(bulkProductUpdateRequest) ||
				StringUtils.isEmpty(bulkProductUpdateRequest.getUserRole()) ||
				StringUtils.isEmpty(bulkProductUpdateRequest.getUser()) ||
				CollectionUtils.isEmpty(bulkProductUpdateRequest.getProducts())) {
			log.warn("ProductAttributeListingServiceImpl > bulkUpdateInformation > Missing mandatory fields in request :: {}",
					bulkProductUpdateRequest);
			throw new PALServiceException(ErrorCode.MISSING_MANDATORY_FIELDS);
		}

		// validate if the request has the mandatory field update data
		List<ProductUpdate> products = bulkProductUpdateRequest.getProducts();
		products.forEach(product -> {
			if (StringUtils.isEmpty(product.getProductId()) ||
				CollectionUtils.isEmpty(product.getFieldUpdates()) ||
				!ObjectUtils.isEmpty(product.getFieldUpdates().stream().
					filter(field -> StringUtils.isEmpty(field.getField())).findFirst().orElse(null))) {
				log.warn("ProductAttributeListingServiceImpl > bulkUpdateInformation > Missing mandatory fields in request :: {}",
						bulkProductUpdateRequest);
				throw new PALServiceException(ErrorCode.MISSING_MANDATORY_FIELDS);
			}
		});

		// valid user access
		UserDetails userDetails = userDetailsService.validateUserDetails(bulkProductUpdateRequest.getUserRole(), null);
		AccessControlInfo accessInfo = userDetails.getUserRole().getAccessControlInfoList().get(0);

		Set<String> updatedFields = products.parallelStream()
				.flatMap(product -> product.getFieldUpdates().stream())
				.map(FieldUpdate::getField)
				.collect(Collectors.toSet());
		List<String> palFieldIds = getPalFields().stream().map(PALFields::getId)
				.collect(Collectors.toList());

		List<PALFields> validPalFields = new ArrayList<>();
		List<String> invalidPalFields = new ArrayList<>();

		// validate pal field
		updatedFields.forEach(field -> {
			if (palFieldIds.contains(field))
				validPalFields.add(CommonUtility.getPALFieldById(field, getPalFields()));
			else
				invalidPalFields.add(field);
		});

		if (!CollectionUtils.isEmpty(invalidPalFields)) {
			log.warn("ProductAttributeListingServiceImpl > bulkUpdateInformation > Invalid Fields provided :: {}",
					invalidPalFields);
			throw new PALServiceException(ErrorCode.INVALID_REQUEST_DATA);
		}

		// validate pal field update access for user
		validPalFields.forEach(field -> {
			if (CommonUtility.isNonEditableField(field, bulkProductUpdateRequest.getUserRole(), accessInfo))
				invalidPalFields.add(field.getId());
		});

		if (!CollectionUtils.isEmpty(invalidPalFields)) {
			log.warn("ProductAttributeListingServiceImpl > bulkUpdateInformation > Update Access denied for user {} on fields {}",
					bulkProductUpdateRequest.getUserRole(), invalidPalFields);
			throw new PALServiceException(ErrorCode.UNAUTHORIIZED);
		}

		return userDetails;
	}

	private void addProductUpdateStatuses(List<ProductUpdateStatus> productUpdateStatuses, String productId, PALProduct product, String updateStatus) {
		if (!productUpdateStatuses.stream().map(ProductUpdateStatus::getProductId).collect(Collectors.toList()).contains(productId)) {
			ProductUpdateStatus productUpdateStatus = new ProductUpdateStatus();
			productUpdateStatus.setProductId(productId);

			if (!org.springframework.util.ObjectUtils.isEmpty(product)) {
				productUpdateStatus.setProductName(CommonUtility.getDataFieldValue(product.getDatafields(), ApplicationConstant.PRODUCT_TITLE_FIELD));
				productUpdateStatus.setProductUrl(String.format(productUrl, product.getProjectId(), product.getId()));
				productUpdateStatus.setUpdateStatus(updateStatus);
			} else {
				productUpdateStatus.setUpdateStatus(updateStatus);
				productUpdateStatus.setErrorMessage(ErrorCode.INVALID_PRODUCT_ID.getErrorMessage());
			}
			productUpdateStatuses.add(productUpdateStatus);
		}
	}

	private List<ProductUpdateStatus> getProductUpdateStatus(Map<String, List<DataField>> upsertProductFields, List<PALProduct> productAfterUpdates, List<PALProduct> productBeforeUpdates) {
		List<ProductUpdateStatus> productUpdateStatuses = new ArrayList<>();
		Set<String> projectIds = productAfterUpdates.stream().map(PALProduct::getProjectId).collect(Collectors.toSet());
		AtomicReference<List<PALProject>> palProjects = new AtomicReference<>(new ArrayList<>());
		for (Map.Entry<String, List<DataField>> upsertProduct : upsertProductFields.entrySet()) {
			PALProduct updatedProduct = productAfterUpdates.stream().filter(product -> upsertProduct.getKey().equalsIgnoreCase(product.getId()))
					.findFirst().orElse(null);
			PALProduct productBeforeUpdate = productBeforeUpdates.stream().filter(product -> updatedProduct.getId().equalsIgnoreCase(product.getId()))
					.findFirst().orElse(null);

			if (!ObjectUtils.isEmpty(updatedProduct)) {
				AtomicBoolean isNotUpdated = new AtomicBoolean(false);
				AtomicBoolean isValueNotChanged = new AtomicBoolean(false);

				upsertProduct.getValue().forEach(updateField -> {
					DataField dataField = CommonUtility.getDataField(updatedProduct.getDatafields(), updateField.getFieldId());
					DataField oldDataField = CommonUtility.getDataField(productBeforeUpdate.getDatafields(), updateField.getFieldId());
					if(ObjectUtils.isEmpty(oldDataField) && ObjectUtils.isEmpty(dataField)) {
						isValueNotChanged.set(true);
					} else if(!ObjectUtils.isEmpty(oldDataField) && !ObjectUtils.isEmpty(dataField) &&
						oldDataField.getFieldValue().equalsIgnoreCase(dataField.getFieldValue())) {
						isValueNotChanged.set(true);
					}

					if (StringUtils.isEmpty(updateField.getFieldValue()) && !ObjectUtils.isEmpty(dataField)) {
						isNotUpdated.set(true);
					} else if (!StringUtils.isEmpty(updateField.getFieldValue()) && ObjectUtils.isEmpty(dataField)) {
						isNotUpdated.set(true);
					} else if (!StringUtils.isEmpty(updateField.getFieldValue()) && !dataField.getFieldValue().equals(updateField.getFieldValue())) {
						isNotUpdated.set(true);
					}

					if (kafkaEnabled && !isNotUpdated.get() && !isValueNotChanged.get()
						&& updateField.getFieldId().equalsIgnoreCase(ApplicationConstant.STATUS_FIELD)) {
						String projectId = updatedProduct.getProjectId();
						if (CollectionUtils.isEmpty(palProjects.get())) {
							palProjects.set(palDao.findPALProjectList(projectIds, null));
						}
						PALProject palProject = palProjects.get().stream().filter(project -> projectId.equalsIgnoreCase(project.getId()))
								.findFirst().orElse(null);
						if (!ObjectUtils.isEmpty(palProject) && notificationProjectStatus.contains(palProject.getStatus())) {
							List<DataField> palProductDataFields = updatedProduct.getDatafields();
							String previousStatus = ObjectUtils.isEmpty(oldDataField) ? null : oldDataField.getFieldValue();
							palProductDataFields.add(DataField.builder().fieldId(ApplicationConstant.PREVIOUS_STATUS)
									.fieldValue(previousStatus).build());
							updatedProduct.setDatafields(palProductDataFields);
							notificationService.sendEmailMessage(palProject, updatedProduct, MessageTemplate.UPDATE_PRODUCT_STATUS);
						}
					}
				});
				if (isNotUpdated.get()) {
					addProductUpdateStatuses(productUpdateStatuses, upsertProduct.getKey(), updatedProduct, ApplicationConstant.FAILED);
				} else {
					addProductUpdateStatuses(productUpdateStatuses, upsertProduct.getKey(), updatedProduct, ApplicationConstant.SUCCESS);
				}

			} else if (!productUpdateStatuses.stream().map(ProductUpdateStatus::getProductId).collect(Collectors.toList()).contains(upsertProduct.getKey())) {
				addProductUpdateStatuses(productUpdateStatuses, upsertProduct.getKey(), updatedProduct, ApplicationConstant.FAILED);
			} else {
				productUpdateStatuses.stream().filter(product -> upsertProduct.getKey().equalsIgnoreCase(product.getProductId()))
						.forEach(product -> {
							product.setUpdateStatus(ApplicationConstant.FAILED);
							product.setErrorMessage(ErrorCode.INVALID_PRODUCT_ID.getErrorMessage());
						});
			}

		}
		return productUpdateStatuses;
	}	

	/**
	 * get the bulk product informations list containing productId and oldValue
	 * 
	 * @param bulkProductRequest BulkProductRequest
	 * @return BulkProductResponse
	 */
	@Override
	public BulkProductResponse getBulkProductInformations(BulkProductRequest bulkProductRequest) {
		long startTime = System.currentTimeMillis();
		if (ObjectUtils.isEmpty(bulkProductRequest) || CollectionUtils.isEmpty(bulkProductRequest.getProducts())
				|| StringUtils.isBlank(bulkProductRequest.getFieldId())
				|| StringUtils.isBlank(bulkProductRequest.getUserRole())) {
			log.error("ProductAttributeListingServiceImpl > getBulkProductInformations > Invalid Request :: {}",
					bulkProductRequest);
			throw new PALServiceException(ErrorCode.INVALID_REQUEST_DATA);
		}
		// validate if the request fieldId is present in the pal fields collection
		PALFields palField = palDao.findAllPALFields().stream()
				.filter(palFields -> palFields.getId().equals(bulkProductRequest.getFieldId())).findFirst()
				.orElse(null);
		if (ObjectUtils.isEmpty(palField)) {
			log.error("ProductAttributeListingServiceImpl > Invalid Field provided :: {}",
					bulkProductRequest.getFieldId());
			throw new PALServiceException(ErrorCode.INVALID_REQUEST_DATA);
		} else if (CommonUtility.isNonEditableField(palField, bulkProductRequest.getUserRole(),
				userDetailsService.getAccessControlDetails(bulkProductRequest.getUserRole()))) {
			log.error("ProductAttributeListingServiceImpl > User {} doesn't have access to update field {}",
					bulkProductRequest.getUserRole(), bulkProductRequest.getFieldId());
			throw new PALServiceException(ErrorCode.UNAUTHORIIZED);
		}		
		List<PALProduct> palProducts = palDao.findPALProducts(null, bulkProductRequest.getProducts(), null);
		if (CollectionUtils.isEmpty(palProducts)) {
			throw new PALServiceException(ErrorCode.NO_DATA);
		}
		UserDetails userDetails = userDetailsService.validateUserDetails(bulkProductRequest.getUserRole(), null);
		List<BulkProduct> bulkProducts = palProducts.stream()
				.map(bulkProductResponseConverter(bulkProductRequest.getFieldId().trim(),bulkProductRequest.getUserRole(),userDetails.getOrganizations())).collect(Collectors.toList());
		long endTime = System.currentTimeMillis();
		log.info(
				"ProductAttributeListingServiceImpl > Completed getBulkProductInformations for request :: {}, Time Taken :: {} ms",
				bulkProductRequest, endTime - startTime);
		return BulkProductResponse.builder().productResponse(bulkProducts).build();
	}

	/**
	 * bulkProductResponseConverter
	 * 
	 * @param fieldId,userRole,organizations
	 * @return
	 */
	private Function<PALProduct, BulkProduct> bulkProductResponseConverter(
			String fieldId, String userRole, List<String> organizations) {
		return x -> {
			if (ApplicationConstant.SUPPLIER.equalsIgnoreCase(userRole))
				validateUserAccessToProduct(x, organizations);
			BulkProduct bulkProductResponse = new BulkProduct();
			bulkProductResponse.setProductId(x.getId());
			bulkProductResponse.setProductName(CommonUtility.getDataFieldValue(x.getDatafields(), ApplicationConstant.PRODUCT_TITLE_FIELD));
			bulkProductResponse.setOldValue(CommonUtility.getDataFieldValue(x.getDatafields(), fieldId));
			return bulkProductResponse;
		};
	}

	@Override
	public Map<String, String> updatePALConfigs() {
		Map<String, String> updateStatuses = new HashMap<>();
		for (String configId : configIds) {
			PALConfiguration palConfigToUpdate = getPALConfiguration(configId);
			String status = ApplicationConstant.FAILED;
			if (ObjectUtils.isEmpty(palConfigToUpdate)) {
				log.warn("ProductAttributeListingServiceImpl > updatePALConfigs > Invalid Config :: {}", configId);
			} else {
				switch (configId) {
					case ApplicationConstant.CATEGORY_CONFIG_ID:
						status = updateCategoryConfig(palConfigToUpdate);
						break;
					default:
						status = ApplicationConstant.FAILED;
						break;
				}
			}
			updateStatuses.put(configId, status);
		}
		return updateStatuses;
	}

	private String updateCategoryConfig(PALConfiguration palConfigToUpdate) {

		List<String> categoryList = new ArrayList<>();

		if (!ObjectUtils.isEmpty(palConfigToUpdate)) {
			Category category = esProductHierarchyServiceRestClient.getCategories();
			if (!ObjectUtils.isEmpty(category) && !ObjectUtils.isEmpty(category.getChildren()) &&
					!CollectionUtils.isEmpty(category.getChildren().getContent())) {
				category.getChildren().getContent().forEach(categoryItem -> {
					categoryList.add(categoryItem.getValue().substring(3, 7) + " " + categoryItem.getDescription());
				});
				if (!CollectionUtils.isEmpty(categoryList)) {
					palConfigToUpdate.setValues(categoryList);
					palDao.savePALConfiguration(palConfigToUpdate);
					return ApplicationConstant.SUCCESS;
				}
			}
		}
		if (!CollectionUtils.isEmpty(categoryList)) {
			log.warn("ProductAttributeListingServiceImpl > updatePALConfigs > No categories found from ES");
		}
		return ApplicationConstant.FAILED;
	}

	/**
	 * deleteProjectOrProducts - deletes project or products provided in the request
	 * sets TTL to configured 20 days and updates the status to Deleted.
	 * @param palDeleteRequest request
	 * @return string
	 */
	@Override
	public PALDeleteResponse deleteProjectOrProducts(PALDeleteRequest palDeleteRequest) {
		long startTime = System.currentTimeMillis();
		log.info("ProductAttributeListingServiceImpl > Started deleteProjectOrProducts for request :: {}", palDeleteRequest);
		PALDeleteResponse palDeleteResponse = new PALDeleteResponse();
		validateDeleteRequest(palDeleteRequest);
		List<String> palProductIds = palDeleteRequest.getProductIds();
		if (CollectionUtils.isEmpty(palDeleteRequest.getProductIds())) {
			List<PALProduct> palProducts = palDao.findPALProducts(palDeleteRequest.getProjectId(), null, null);
			palProductIds = palProducts.stream().map(PALProduct::getId).collect(Collectors.toList());
			PALProject palProject = new PALProject();
			palProject.setId(palDeleteRequest.getProjectId());
			palProject.setStatus(Status.DELETED.getStatus());
			palProject.setDeletedDate(LocalDateTime.now());
			PALProject palProjectUpdated = palDao.updatePALProject(palProject);
			if(ObjectUtils.isEmpty(palProjectUpdated)) {
				throw new PALServiceException(ErrorCode.INVALID_PROJECT_ID);
			}
			palDeleteResponse.setProjectId(palProjectUpdated.getId());
			palDeleteResponse.setProjectName(palProjectUpdated.getProjectName());
			palDeleteResponse.setProjectStatus(palProjectUpdated.getStatus());
			log.info("ProductAttributeListingServiceImpl > PALProject {} status updated as {}", palProjectUpdated.getId(), palProjectUpdated.getStatus());
		}
		BulkProductUpdateResponse response = new BulkProductUpdateResponse();
		if (!CollectionUtils.isEmpty(palProductIds)) {
			log.info("ProductAttributeListingServiceImpl > PALProducts to be deleted ::  {}", palProductIds);
			List<ProductUpdate> productUpdates = new ArrayList<>();
			palProductIds.forEach(productId ->
				productUpdates.add(ProductUpdate.builder().productId(productId)
						.fieldUpdates(new ArrayList<>(Arrays.asList(FieldUpdate.builder().field(ApplicationConstant.STATUS_FIELD).newValue(Status.DELETED.getStatus()).build()))).build())
			);

			BulkProductUpdateRequest updateRequest = new BulkProductUpdateRequest();
			updateRequest.setUser(palDeleteRequest.getUser());
			updateRequest.setUserRole(palDeleteRequest.getUserRole());
			updateRequest.setProducts(productUpdates);
			response = bulkUpdateInformation(updateRequest);

			palDeleteResponse.setFailedProducts(response.getFailedProducts());
			palDeleteResponse.setSuccessProducts(response.getSuccessProducts());
		}

		long endTime = System.currentTimeMillis();
		log.info(
				"ProductAttributeListingServiceImpl > Completed deleteProjectOrProducts for request :: {}, Time Taken :: {} ms",
				palDeleteRequest, endTime - startTime);
		if (ObjectUtils.isEmpty(palDeleteResponse) || !CollectionUtils.isEmpty(palDeleteResponse.getFailedProducts()) ||
			Status.DELETED.getStatus().equalsIgnoreCase(palDeleteResponse.getProjectStatus())) {
			log.warn("ProductAttributeListingServiceImpl > Deletion failed for request :: {}, response :: {}", palDeleteRequest, palDeleteResponse);
		}

		return palDeleteResponse;
	}

	private UserDetails validateDeleteRequest(PALDeleteRequest palDeleteRequest) {
		if (ObjectUtils.isEmpty(palDeleteRequest) ||
				StringUtils.isEmpty(palDeleteRequest.getUserRole()) ||
				StringUtils.isEmpty(palDeleteRequest.getUser()) ||
				StringUtils.isEmpty(palDeleteRequest.getProjectId())) {
			log.warn("ProductAttributeListingServiceImpl > bulkUpdateInformation > Missing mandatory fields in request :: {}",
					palDeleteRequest);
			throw new PALServiceException(ErrorCode.MISSING_MANDATORY_FIELDS);
		}

		List<String> deleteaccessiableusers;
		if (!CollectionUtils.isEmpty(palDeleteRequest.getProductIds())) {
			deleteaccessiableusers = new ArrayList<>(Arrays.asList(ApplicationConstant.PROJECT_MANAGER, ApplicationConstant.PRODUCT_DEVELOPER));
		} else {
			deleteaccessiableusers = new ArrayList<>(Arrays.asList(ApplicationConstant.PROJECT_MANAGER));
		}

		// valid user access
		return userDetailsService.validateUserDetails(palDeleteRequest.getUserRole(), deleteaccessiableusers);
	}

}
