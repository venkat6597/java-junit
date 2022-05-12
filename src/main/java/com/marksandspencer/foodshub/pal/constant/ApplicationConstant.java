package com.marksandspencer.foodshub.pal.constant;

import com.google.common.collect.ImmutableMap;

/**
 * The type Application constant.
 */
public final class ApplicationConstant {

    private ApplicationConstant() {
	}

	public static final String ORGANIZATION = "Organization";

	/**
	 * The constant JWTHEADER.
	 */
	public static final String JWTHEADER = "x-authentication";

	public static final String BEARER = "Bearer";

	public static final String AUTHORIZATION = "Authorization";

	public static final String OFP = "ofp";

	public static final String TEMPLATE_CATEGORY = "CATEGORY";

	public static final String PRODUCT_FILE_TYPE_FIELD = "productFileType";

	public static final String PARENT = "PARENT";

	public static final String MULTIPLE_ARTWORKS_FIELD = "multipleArtworks";

	public static final String PRINTED_PACKAGING_TYPE = "printedPackagingType";

	public static final String MULTIPLE = "multiple";

	public static final String IN_DEPOT_DATE_FIELD = "inDepotDate";

	public static final String LAUNCH_PHASE_FIELD = "launchPhase";

	public static final String PRODUCT_TITLE_FIELD = "productTitle";

	public static final String CATEGORY_FIELD = "category";

	public static final String STATUS_FIELD = "status";

	public static final String SUPPLIER_NAME_FIELD = "supplierName";

	public static final String UPC_FIELD = "upc";

	public static final String WEIGHT_OR_VOLUME_FIELD = "weightOrVolume";

	public static final String UNIT_OF_MEASURE_FIELD = "unitOfMeasure";

	public static final String SUPPLIER_SITE_CODE_FIELD = "supplierSiteCode";

	public static final String STANDARD = "Standard";

	public static final String DESIGN = "design";

	public static final String MULTIPLE_ARTWORK_LABEL = "Multiple Artworks";

	public static final String MENU_RESTRICTED_ACCESS = "MenuRestrictedAccess";

	public static final String ORGANIZATIONS = "organizations";

	public static final String PAGE_OBJECT = "pageObject";

	public static final String FSP_PAL_PROJECT = "FSP PAL Projects";

	public static class PALProductFilterFields {

		public static final String PROJECT_ID = "projectId";
		public static final String STATUS = "status";
		public static final String SUPPLIERS = "supplierSiteCode";
		public static final String TYPE = "productType";
		public static final String PARENT = "productFileType";
		public static final String PRODUCT_IN_DEPO_DATE = "inDepotDate";
		public static final String PRODUCT_IN_STORE_DATE = "launchPhase";
		public static final String OCADO_PRODUCT = "soldInOcado";
		public static final String DATAFIELD_VALUE = "fieldValue";
		public static final String DATAFIELD_ID = "fieldId";
		public static final String DATAFIELD = "datafields";
		public static final String SUPPLIERNAME="supplierName";

	}

	public static final String PROJECT_UPDATE_ACCESS = "PAL PROJECT UPDATE ACCESS";

	public static final String PROJECT_STATUS = "PROJECT STATUS";

	public static final String PROJECT_MANAGER = "projectManager";

	public static final String ROLLING = "Rolling";

	public static final String PRODUCT_TYPE_FIELD = "productType";

	public static final String SUPPLIER = "supplier";

	public static final String TRAY_SELLING_VALUE_FIELD = "traySellingValue";

	public static final String SELLING_PRICE_FIELD = "sellingPrice";

	public static final String UPT_FIELD = "upt";

	public static final String DOCUMENT_CREATED_DATE = "createdDate";

	public static final String SUPPLIER_ROLE_NAME = "One MnS Foods supplier";

	public static final String TRAY_COST_VALUE_FIELD = "trayCostValue";

	public static final String MNS_GROSS_COST_PRICE_FIELD = "mnsGrossPrice";

	public static final String MARGIN_FIELD = "margin";

	public static final String AVERAGE_WEEKLY_SALES_MNS_FIELD = "averageWeeklySalesMandS";

	public static final String AVERAGE_WEEKLY_VOLUME_MNS_FIELD = "averageWeeklyVolumeMandS";

	public static final String BATON_HANDOVER_FIELD = "batonHandover";

	public static final String DESIGN_CONCEPT_HANDOVER_FIELD = "designConceptHandover";

	public static final String FINALISE_GATE_FIELD = "finaliseGate";

	public static final String AGREED_COST_PRICE_CURRENCY_FIELD = "agreedCostPriceCurrency";

	public static final String PROPOSED_NO_OF_STORES_FIELD = "proposedNoOfStores";

	public static final String SUCCESS_CRITERIA_FIELD = "successCriteria";

	public static final String FTP_DATE_FIELD = "ftpDate";

	public static final String EUR = "Euro";

	public static final String USD = "USD";

	public static final String CHILD_PRODUCT_TITLE_FIELD = "childProductTitle";

	public static final String CHILD_SUPPLIER_NAME_FIELD = "childSupplierName";

	public static final String CHILD_SUPPLIER_SITE_CODE_FIELD = "childSupplierSiteCode";

	public static final String CHILD_UPC_FIELD = "childUpc";

	public static final String ACL_HEADER = "x-acl-required";

	public static final String MODULE_HEADER = "x-module";

	public static final String OBJECT_HEADER = "x-object";

	public static final String OPERATION_HEADER = "x-operation";

	public static final String CREATE = "Create";

	public static final String READ = "Read";

	public static final String UPDATE = "Update";

	public static final String OFP_MODULE = "onemnsfood";

	public static final String FSP_PAL_PROJECT_DETAILS = "FSP PAL Project Details";

	public static final String FSP_PAL_PRODUCT = "FSP PAL Product";

	public static final String INFORMATION_TAB = "Information";

	public static final String PERSONNEL_TAB = "Personnel";

	public static final String PROGRESS_TAB = "Progress";

	public static final String AUDITLOG_TAB = "Auditlog";

	public static final String ID = "_id";

	public static final String PRINTER_TYPE= "printerType";

	public static final String SMP = "SMP";

	public static final String SMP_APPROVED = "smpApproved";

	public static final String PRINT_AND_PACKAGING = "printAndPackaging";

	public static final String SUBRANGE_FIELD_ID = "subRange";

	public static final String PROJECT_TYPE = "PROJECT TYPE";

	public static final String PAL = "PAL";

	public static final String PRODUCT_DEVELOPER = "productDeveloper";

	public static final String VAT = "vat";

	public static final String OCADO = "ocado";

	public static final String INTERNATIONAL = "international";

	public static final String VIEWER = "viewer";

	public static final String URL = "url";

	public static final String PRODUCT_STATUS_FIELD = "productStatus";

	public static final String PROJECT_STATUS_FIELD = "projectStatus";

	public static final String PREVIOUS_STATUS = "productPreviousStatus";

	public static class Notification {
		public static final String PUBLISHER = "publisher";
		public static final String EVENT_MODULE = "eventModule";
		public static final String EVENT_NAME = "eventName";
		public static final String EVENT_ID = "eventId";
		public static final String DATA = "data";
		public static final String USERS = "users";
		public static final String TEMPLATE_ID = "templateId";
		public static final String SUBJECT = "subject";
		public static final String FROM = "from";
		public static final String EMAIL_MESSAGE = "emailMessage";
		public static final String USERID = "userId";
		public static final String STATUS = "status";
		public static final String TYPE = "type";
		public static final String MAILTYPE = "mailType";
		public static final String TYPE_EMAIL = "EMAIL";
		public static final String MAILTYPE_TO = "TO";
		public static final String USERROLES = "UserRoles";
		public static final String NOTIFICATION_CONFIG = "PAL NOTIFICATION CONFIGURATION";
		public static final String TRIGGER = "trigger";
		public static final String PROJECT_USERS = "projectUsers";
		public static final String PRODUCT_USERS = "productUsers";
		public static final String GROUP_USERS = "groupUsers";
	}

	public static final ImmutableMap<String, Integer> NO_OF_DAYS = new ImmutableMap.Builder<String, Integer>()
			.put(FINALISE_GATE_FIELD, 14)
			.put(DESIGN_CONCEPT_HANDOVER_FIELD, 12)
			.put(BATON_HANDOVER_FIELD, 19).build();

	public static final String DEFAULT_END_POINT_URL = "DefaultEndpointsProtocol=https";

	public static final String SEPERATOR = ";";

	public static final String ACCOUNT_NAME = "AccountName=";

	public static final String ACCOUNT_KEY = "AccountKey=";

	public static final String STANDARD_DOWNLOAD_TEMPLATE = "Standard_Download_Template";

	public static final String DOWNLOAD_EXCEL_EXTENSION = ".xlsx";

	public static final String DOWNLOAD_MACRO_EXCEL_EXTENSION = ".xlsm";

	public static final String MNS = "MnS";

	public static final String FIXED_BUY = "fixedBuy";

	public static final String FIXED_BUY_COMMITMENT_DEADLINE = "fixedBuyCommitmentDeadline";

	public static final String VALUE_YES = "Yes";

	public static final String TEXT_FILE_EXTENSION = ".txt";

	public static final String PRODUCT_DUPLICATE_ACCESS = "PAL PRODUCT DUPLICATE ACCESS";

	public static final String SUCCESS = "Success";

	public static final String FAILED = "Failed";

	public static final String NO_CHANGE = "No Change";

	public static final String FIELD_LABEL = "label";

	public static final String PRODUCT_ID = "productId";

	public static final String CATEGORY_CONFIG_ID = "CATEGORY";

	public static final String NODEVALUE = "F2-FD";

	public static final String NODELEVEL = "02";

	public static final String CHILDNODELEVEL = "06";

	public static final String DELETE = "Delete";

	public static final String DELETED_DATE_FIELD_ID = "deletedDate";
}