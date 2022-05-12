package com.marksandspencer.foodshub.pal.serviceImpl;

import com.google.common.collect.Lists;
import com.marksandspencer.foodshub.pal.constant.ApplicationConstant;
import com.marksandspencer.foodshub.pal.constant.ErrorCode;
import com.marksandspencer.foodshub.pal.domain.Configuration;
import com.marksandspencer.foodshub.pal.dto.PALUser;
import com.marksandspencer.foodshub.pal.dto.Supplier;
import com.marksandspencer.foodshub.pal.dto.Suppliers;
import com.marksandspencer.foodshub.pal.exception.PALServiceException;
import com.marksandspencer.foodshub.pal.repository.ConfigurationRepository;
import com.marksandspencer.foodshub.pal.rest.client.AuthzRestClient;
import com.marksandspencer.foodshub.pal.rest.client.AzureRest;
import com.marksandspencer.foodshub.pal.rest.client.ESSupplierServiceRestClient;
import com.marksandspencer.foodshub.pal.service.UserDetailsService;
import com.marksandspencer.foodshub.pal.service.UserService;
import com.marksandspencer.foodshub.pal.transfer.AuthAttribute;
import com.marksandspencer.foodshub.pal.transfer.AuthzSupplierResponse;
import com.marksandspencer.foodshub.pal.transfer.ESSupplierDataRequest;
import com.marksandspencer.foodshub.pal.transfer.UserDetails;
import com.marksandspencer.foodshub.pal.util.Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

	@Autowired
	AuthzRestClient authzRestClient;

	@Autowired
	AzureRest azureRest;

	@Autowired
	ESSupplierServiceRestClient esSupplierServiceRestClient;

	@Autowired
	ConfigurationRepository configurationRepository;

	@Autowired
	UserDetailsService userDetailsService;

	@Value("${es.supplier.pageSize}")
	Integer suppliersPageSize;

	/**
	 * get supplier organization id from authz
	 */
	@Override
	public List<AuthzSupplierResponse> getSupplierIds(String jwt) {
		List<AuthzSupplierResponse> supplierIdResponse = new ArrayList<>();
		try {

			List<AuthAttribute> authAttribute = authzRestClient.getAuthAttribute(ApplicationConstant.OFP, jwt);
			List<AuthAttribute> authAttributeFiltered = authAttribute.stream().filter(
					attribute -> attribute.getAttributeType().equalsIgnoreCase(ApplicationConstant.ORGANIZATION))
					.collect(Collectors.toList());
			authAttributeFiltered.forEach(filteredList -> {
				AuthzSupplierResponse supplierIdResponseObj = new AuthzSupplierResponse();
				supplierIdResponseObj.setAttributeId(filteredList.getAttributeCode());
				supplierIdResponseObj.setSupplierId(filteredList.getAttributeName());
				supplierIdResponse.add(supplierIdResponseObj);
			});

			List<AuthzSupplierResponse> filteredSuppliers = filterSupplierCodes(supplierIdResponse);

			List<List<String>> partitionedList = Lists.partition(Lists.newArrayList(
					filteredSuppliers.stream().map(AuthzSupplierResponse::getSupplierId).collect(Collectors.toSet())),
					suppliersPageSize);
			List<CompletableFuture<Suppliers>> futureResult = Lists.newArrayList();
			partitionedList.forEach(x -> {
				CompletableFuture<Suppliers> suppliers = esSupplierServiceRestClient
						.getSuppliers(ESSupplierDataRequest.builder().supplierIds(x).build());
				futureResult.add(suppliers);
			});
			// wait until all the asynchronous calls are completed
			CompletableFuture.allOf(futureResult.toArray(new CompletableFuture[futureResult.size()]));
			// stream the future objects combine,applyRule and get all the locations
			List<Supplier> suppliers = futureResult.stream().map(Util.<Suppliers>getFutureObject())
					.filter(o -> o != null).flatMap(o -> o.getSuppliers().stream()).collect(Collectors.toList());
			if (CollectionUtils.isEmpty(suppliers)) {
				log.error("failed to get the supplier details from ES service");
				// TODO shall we not throw exception if we are not able to enrich the PO for UI
			} else {
				filteredSuppliers.forEach(suppResp -> {
					suppliers.forEach(o -> {
						if (o.getSupplierId().equals(suppResp.getSupplierId())) {
							suppResp.setSupplierName(o.getName());
						}
					});
				});
			}

			return filteredSuppliers;

		} catch (PALServiceException exception) {
			log.error("PAL - Exception occured in getSupplierIds {}", exception.getMessage());
			throw exception;
		} catch	(Exception exception) {
			log.error("PAL - Exception occured in getSupplierIds {}", exception.getMessage());
			throw new PALServiceException(exception.getMessage());
		}
	}

	/**
	 * based on the configuration the supplierCodes to be filter out.
	 * 
	 * configuration collection has page object(FSP PAL Projects) based supplier
	 * organizations list configured the filtered suppliers should be return by this
	 * method.
	 * 
	 * @param supplierIdResponse
	 * @return List<AuthzSupplierResponse>
	 */
	@SuppressWarnings("unchecked")
	private List<AuthzSupplierResponse> filterSupplierCodes(List<AuthzSupplierResponse> supplierIdResponse) {
		
		if (!CollectionUtils.isEmpty(supplierIdResponse)) {
			Configuration configuration = configurationRepository
					.findAllByType(ApplicationConstant.MENU_RESTRICTED_ACCESS);
			if (!ObjectUtils.isEmpty(configuration)) {
				Optional<Map<String, Object>> valueMap = configuration.getValues().stream()
						.filter(x -> ApplicationConstant.FSP_PAL_PROJECT.equals(x.get(ApplicationConstant.PAGE_OBJECT)))
						.findFirst();
				if (valueMap.isPresent()) {
					
					List<String> organizations = (List<String>) valueMap.get().get(ApplicationConstant.ORGANIZATIONS);
					 supplierIdResponse =supplierIdResponse.parallelStream().filter(x -> organizations.contains(x.getSupplierId()))
							.collect(Collectors.toList());
				}
			}
		}
		UserDetails userDetails = userDetailsService.getUserDetails();
		if (ApplicationConstant.SUPPLIER_ROLE_NAME.equals(userDetails.getUserRole().getRoleName())) {
			if (!CollectionUtils.isEmpty(userDetails.getOrganizations())) {
				List<String> organizations = userDetails.getOrganizations();
				return supplierIdResponse.parallelStream().filter(x -> organizations.contains(x.getSupplierId()))
						.collect(Collectors.toList());

			} else {
				throw new PALServiceException(ErrorCode.UNAUTHORIIZED);
			}

		}
		return supplierIdResponse;
	}

	@Override
	public Map<String, List<PALUser>> listUserByRoles(List<String> roles) {
		Map<String, List<PALUser>> users = new ConcurrentHashMap<>();
		roles.parallelStream().forEach(role -> users.put(role, azureRest.listUserByRole(role)));
		return users;
	}

}
