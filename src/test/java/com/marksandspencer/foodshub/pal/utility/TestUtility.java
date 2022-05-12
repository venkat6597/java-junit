package com.marksandspencer.foodshub.pal.utility;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.marksandspencer.assemblyservice.config.security.utils.UiAuthenticationToken;
import com.marksandspencer.assemblyservice.config.transfer.AccessControlInfo;
import com.marksandspencer.assemblyservice.config.transfer.UserAccessInfo;
import com.marksandspencer.assemblyservice.config.transfer.UserRole;
import com.marksandspencer.foodshub.pal.constant.ApplicationConstant;
import com.marksandspencer.foodshub.pal.domain.*;
import com.marksandspencer.foodshub.pal.exception.PALServiceException;
import com.marksandspencer.foodshub.pal.transfer.UserDetails;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class TestUtility {

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Object convertMapToObject(Map<String,Object> mapObject, TypeReference typeReference) {
		Object object = new Object();
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.registerModule(new JavaTimeModule());
			object = mapper.convertValue(mapObject, typeReference);

		} catch (Exception e) {
			throw new PALServiceException("Json Parsing Error ", e.getMessage());
		}
		return object;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Object convertMapToListObject(List<Object> obj, TypeReference typeReference) {
		Object object = new Object();
		try {
			ObjectMapper mapper = new ObjectMapper();
			mapper.registerModule(new JavaTimeModule());
			object = mapper.convertValue(obj, typeReference);

		} catch (Exception e) {
			throw new PALServiceException("Json Parsing Error ", e.getMessage());
		}
		return object;
	}

	@SuppressWarnings("rawtypes")
	public static Map<String, Object> readFile(String fileName, TypeReference typeReference) {
		ObjectMapper mapper = new ObjectMapper();
		Map<String,Object> mapValues = new HashMap<>();
		try {
			mapValues = mapper.readValue(new File(fileName),
					new TypeReference<>(){});
		} catch (Exception e) {
			throw new PALServiceException("Json Parsing Error ", e.getMessage());
		}
		return mapValues;
	}

	public static List<PALFields> getPALFields() {
		List<PALFields> palFields = new ArrayList<>();
		PALFields field = new PALFields();
		field.setId("productTitle");
		field.setLabel("PRODUCT TITLE");
		palFields.add(field);
		
		field = new PALFields();
		field.setId("productSubTitle");
		field.setLabel("PRODUCT SUBTITLE");
		palFields.add(field);

        field = new PALFields();
        field.setId("productFileType");
        field.setLabel("FIND PRODUCT FILETYPE");
        palFields.add(field);

        return palFields;
	}

	public static List<PALConfiguration> getPALConfigs() {
		List<PALConfiguration> palConfigs = new ArrayList<>();
		PALConfiguration config = new PALConfiguration();
		config.setId(ApplicationConstant.PROJECT_STATUS);
		config.setValues(Arrays.asList(
				"Active - Pre-Creative Seal",
				"Active - Post-Creative Seal",
				"Archived"));
		palConfigs.add(config);

		config = new PALConfiguration();
		config.setId(ApplicationConstant.PROJECT_UPDATE_ACCESS);
		config.setValues(Arrays.asList(
				"projectManager",
				"productDeveloper"));
		palConfigs.add(config);

		return palConfigs;
	}

	public static PALTemplate getPALTemplate(String templateId) {
		String fileName = "src/test/resources/ProductAttributeListingResponse/PALTemplateRequest.json";
		Map<String, Object> palTemplatedetails = TestUtility.readFile(fileName, new TypeReference<>(){});
		return (PALTemplate) TestUtility.convertMapToObject((Map<String,Object>) palTemplatedetails.getOrDefault(templateId, null),
				new TypeReference<PALTemplate>() {});
	}

	public static UiAuthenticationToken getUserAuthentication(String user) {
		UserRole userRole=null;
		UserAccessInfo userAccessInfo = null;
		AccessControlInfo accessInfo = null;
		if (user.equalsIgnoreCase("projectManager")) {
			userRole = createUserRole("projectManager", false);
			accessInfo = createAccessInfo("projectManager", true, true, true, true);
			userRole.setAccessControlInfoList(Arrays.asList(accessInfo));
			userAccessInfo = createUserAccessInfo(Arrays.asList(userRole), null);
			return new UiAuthenticationToken("projectManager", userAccessInfo, "projectManager");
		} else if (user.equalsIgnoreCase("productDeveloper")) {
			userRole = createUserRole("productDeveloper",  false);
			accessInfo = createAccessInfo("productDeveloper", true, true, false, true);
			userRole.setAccessControlInfoList(Arrays.asList(accessInfo));
			userAccessInfo = createUserAccessInfo(Arrays.asList(userRole), null);
			return new UiAuthenticationToken("productDeveloper", userAccessInfo, "productDeveloper");
		} else if (user.equalsIgnoreCase("buyer")) {
			userRole = createUserRole("buyer",false);
			accessInfo = createAccessInfo("buyer", false, true, false, true);
			userRole.setAccessControlInfoList(Arrays.asList(accessInfo));
			userAccessInfo = createUserAccessInfo(Arrays.asList(userRole),null);
			return new UiAuthenticationToken("buyer", userAccessInfo, "buyer");
		} else if (user.equalsIgnoreCase("supplier")) {
			userRole = createUserRole("supplier", true);
			accessInfo = createAccessInfo("supplier", false, true, false, true);
			userRole.setAccessControlInfoList(Arrays.asList(accessInfo));
			userAccessInfo = createUserAccessInfo(Arrays.asList(userRole), Arrays.asList("F01524"));
			return new UiAuthenticationToken("supplier", userAccessInfo, "supplier");
		} else if (user.equalsIgnoreCase("readOnlyUser")) {
			userRole = createUserRole("readOnlyUser", false);
			accessInfo = createAccessInfo("readOnlyUser", false, false, false, true);
			userRole.setAccessControlInfoList(Arrays.asList(accessInfo));
			userAccessInfo = createUserAccessInfo(Arrays.asList(userRole), null);
			return new UiAuthenticationToken("supplier", userAccessInfo, "readOnlyUser");
		}
		return null;
	}

	private static AccessControlInfo createAccessInfo(String projectManager, boolean create, boolean update, boolean delete, boolean read) {
		AccessControlInfo accessInfo = new AccessControlInfo();
		accessInfo.setCreateAccess(create);
		accessInfo.setReadAccess(read);
		accessInfo.setDeleteAccess(delete);
		accessInfo.setUpdateAccess(update);
		return accessInfo;
	}

	public static UserAccessInfo createUserAccessInfo(List<UserRole> userRoles, List<String> organizations) {
		UserAccessInfo userAccessInfo = new UserAccessInfo();
		userAccessInfo.setUserRoleList(userRoles);
		userAccessInfo.setOrganizations(organizations);
		return userAccessInfo;
	}

	public static UserRole createUserRole(String role, boolean organization) {
		UserRole userRole = new UserRole();
		userRole.setRoleName(role);
		userRole.setOrganizationEnabled(organization);
		return userRole;
	}

	public static UserDetails getUserDetails(String role) {
		UserAccessInfo userAccessInfo = getUserAuthentication(role).getUserAccessInfo();
		UserRole userRole = userAccessInfo.getUserRoleList().get(0);
		List<String> organizations = userAccessInfo.getOrganizations();

		return UserDetails.builder().userRole(userRole).organizations(organizations)
				.email("pauserRole").build();
	}

	public static PALProduct getPALProductafterFieldUpdate(PALProduct productbeforeupdate, List<DataField> fieldUpdates) {
		PALProduct productafterupdate = new PALProduct();
		productafterupdate.setId(productbeforeupdate.getId());
		productafterupdate.setTemplateId(productbeforeupdate.getTemplateId());
		productafterupdate.setProjectId(productbeforeupdate.getProjectId());
		productafterupdate.setPersonnel(productbeforeupdate.getPersonnel());
		productafterupdate.setCreatedDate(productbeforeupdate.getCreatedDate());

		List<DataField> dataFields = fieldUpdates.stream().filter(field -> !StringUtils.isEmpty(field.getFieldValue()))
				.collect(Collectors.toList());
		productbeforeupdate.getDatafields().forEach(dataField -> {
			DataField fieldUpdate = fieldUpdates.stream().filter(field -> field.getFieldId().equalsIgnoreCase(dataField.getFieldId()))
					.findFirst().orElse(null);
			if (ObjectUtils.isEmpty(fieldUpdate)) {
				dataFields.add(dataField);
			} else if (!StringUtils.isEmpty(fieldUpdate.getFieldValue())) {
				dataFields.add(fieldUpdate);
			}
		});
		productafterupdate.setDatafields(dataFields.stream().distinct().collect(Collectors.toList()));
		return productafterupdate;
	}
}
