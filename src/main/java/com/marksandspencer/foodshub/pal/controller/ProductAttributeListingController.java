package com.marksandspencer.foodshub.pal.controller;

import com.marksandspencer.foodshub.pal.constant.ApplicationConstant;
import com.marksandspencer.foodshub.pal.domain.PALConfiguration;
import com.marksandspencer.foodshub.pal.domain.PALFields;
import com.marksandspencer.foodshub.pal.domain.PALRole;
import com.marksandspencer.foodshub.pal.domain.PALTemplate;
import com.marksandspencer.foodshub.pal.exception.PALServiceException;
import com.marksandspencer.foodshub.pal.service.ProductAttributeListingService;
import com.marksandspencer.foodshub.pal.transfer.*;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 
 * Product Attribute Listing Controller
 *
 */
@Slf4j
@RestController
@RequestMapping("/v1/pal")
public class ProductAttributeListingController {

	@Autowired
	ProductAttributeListingService palService;

	@GetMapping(value = "/")
	public String index() {
		log.info("Beginning of com.marksandspencer.foodshub.pal.controller.index()");
		return "index";
	}
	
	/**
	 * List down the PAL roles
	 * @return List<PALRole>
	 */
	@GetMapping(value = "/listRoles")
	@ApiOperation(value = "listRoles", response = AppResponse.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success"),
			@ApiResponse(code = 204, message = "No data"), @ApiResponse(code = 401, message = "Not authorized!"),
			@ApiResponse(code = 404, message = "Not found"), @ApiResponse(code = 400, message = "Bad request") })
	public ResponseEntity<AppResponse<List<PALRole>>> listRoles() {
		return new ResponseEntity<>(new AppResponse<>(palService.listRoles(), Boolean.TRUE),
				HttpStatus.OK);
	}

	/**
	 * List down the templates
	 * @param palTemplates list of template ids
	 * @return List<PALTemplate> list of pal templates
	 */
	@PostMapping(value = "/getPALTemplates")
	@ApiOperation(value = "getPALTemplates", response = AppResponse.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success"),
			@ApiResponse(code = 204, message = "No data"), @ApiResponse(code = 401, message = "Not authorized!"),
			@ApiResponse(code = 404, message = "Not found"), @ApiResponse(code = 400, message = "Bad request") })
	public ResponseEntity<AppResponse<List<PALTemplate>>> getPALTemplates(@RequestBody PALTemplateRequest palTemplates,
			@RequestHeader(value = ApplicationConstant.JWTHEADER) String jwtToken,
			@RequestHeader(value = ApplicationConstant.ACL_HEADER, defaultValue = "true") String isAcl,
			@RequestHeader(value = ApplicationConstant.MODULE_HEADER, defaultValue = ApplicationConstant.OFP_MODULE) String module ,
			@RequestHeader(value = ApplicationConstant.OBJECT_HEADER, defaultValue = ApplicationConstant.FSP_PAL_PRODUCT) String object,
			@RequestHeader(value = ApplicationConstant.OPERATION_HEADER, defaultValue=ApplicationConstant.READ) String operation)
			throws PALServiceException {
		return new ResponseEntity<>(new AppResponse<>(palService.getPALTemplates(palTemplates), Boolean.TRUE),
				HttpStatus.OK);
	}

	/**
	 * list the roles who are contributing to the project/product updates
	 * @param projectId id of the project
	 * @return List<PALRole>
	 */
	@GetMapping(value = "/listProjectTemplateRoles")
	@ApiOperation(value = "listProjectTemplateRoles", response = AppResponse.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success"),
			@ApiResponse(code = 204, message = "No data"), @ApiResponse(code = 401, message = "Not authorized!"),
			@ApiResponse(code = 404, message = "Not found"), @ApiResponse(code = 400, message = "Bad request") })
	public ResponseEntity<AppResponse<List<PALRole>>> listProjectTemplateRoles(@RequestParam String projectId) {
		return new ResponseEntity<>(new AppResponse<>(palService.listProjectTemplateRoles(projectId), Boolean.TRUE),
				HttpStatus.OK);
	}

	/**
	 * Add the product into the project
	 * @param palProductCreateRequest request for product creation
	 * @return Appresponse
	 */
	@PostMapping(value="/PALProduct/createProduct")
	@ApiOperation(value = "createPALProduct", response = AppResponse.class, tags = "PALProduct")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success"),
			@ApiResponse(code = 204, message = "No data"), @ApiResponse(code = 401, message = "Not authorized!"),
			@ApiResponse(code = 404, message = "Not found"), @ApiResponse(code = 400, message = "Bad request") })
	public ResponseEntity<AppResponse<PALProductResponse>> createPALProduct(
			@RequestBody PALProductCreateRequest palProductCreateRequest,
			@RequestHeader(value = ApplicationConstant.JWTHEADER) String jwtToken,
			@RequestHeader(value = ApplicationConstant.ACL_HEADER, defaultValue = "true") String isAcl,
			@RequestHeader(value = ApplicationConstant.MODULE_HEADER, defaultValue = ApplicationConstant.OFP_MODULE) String module ,
			@RequestHeader(value = ApplicationConstant.OBJECT_HEADER, defaultValue = ApplicationConstant.FSP_PAL_PROJECT_DETAILS) String object,
			@RequestHeader(value = ApplicationConstant.OPERATION_HEADER, defaultValue=ApplicationConstant.CREATE) String operation)  throws PALServiceException {

		log.info("Beginning of ProductAttributeListing: createPALProduct :: {}" , palProductCreateRequest);

		return new ResponseEntity<>(new AppResponse<>(palService.createPALProduct(palProductCreateRequest), Boolean.TRUE),
				HttpStatus.OK);
	}
	
	/**
	 * get PAL Product details
	 * @param palProductRequest request to fetch product information
	 * @return AppResponse
	 */
	@PostMapping(value="/PALProduct/Information")
	@ApiOperation(value = "getPALProductInformation", response = AppResponse.class, tags = "PALProduct")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success"),
			@ApiResponse(code = 204, message = "No data"), @ApiResponse(code = 401, message = "Not authorized!"),
			@ApiResponse(code = 404, message = "Not found"), @ApiResponse(code = 400, message = "Bad request") })
	public ResponseEntity<AppResponse<PALProductResponse>> getPALProductInformation(
			@RequestBody PALProductRequest palProductRequest,
			@RequestHeader(value = ApplicationConstant.JWTHEADER) String jwtToken,
			@RequestHeader(value = ApplicationConstant.ACL_HEADER, defaultValue = "true") String isAcl,
			@RequestHeader(value = ApplicationConstant.MODULE_HEADER, defaultValue = ApplicationConstant.OFP_MODULE) String module ,
			@RequestHeader(value = ApplicationConstant.OBJECT_HEADER, defaultValue = ApplicationConstant.FSP_PAL_PRODUCT) String object,
			@RequestHeader(value = ApplicationConstant.OPERATION_HEADER, defaultValue=ApplicationConstant.READ) String operation)  throws PALServiceException {
		
		log.info("Beginning of ProductAttributeListing: getPALProductInformation :: {}" , palProductRequest);
				
		return new ResponseEntity<>(new AppResponse<>(palService.getPALProductInformation(palProductRequest), Boolean.TRUE),
				HttpStatus.OK);
	}

	/**
	 * get PAL Product Personnel details
	 * @param palProductRequest request to fetch product personnel
	 * @return AppResponse
	 */
	@PostMapping(value="/PALProduct/Personnel")
	@ApiOperation(value = "getPALProductPersonnel", response = AppResponse.class, tags = "PALProduct")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success"),
			@ApiResponse(code = 204, message = "No data"), @ApiResponse(code = 401, message = "Not authorized!"),
			@ApiResponse(code = 404, message = "Not found"), @ApiResponse(code = 400, message = "Bad request") })
	public ResponseEntity<AppResponse<PALProductResponse>> getPALProductPersonnel(
			@RequestBody PALProductRequest palProductRequest,
			@RequestHeader(value = ApplicationConstant.JWTHEADER) String jwtToken,
			@RequestHeader(value = ApplicationConstant.ACL_HEADER, defaultValue = "true") String isAcl,
			@RequestHeader(value = ApplicationConstant.MODULE_HEADER, defaultValue = ApplicationConstant.OFP_MODULE) String module ,
			@RequestHeader(value = ApplicationConstant.OBJECT_HEADER, defaultValue = ApplicationConstant.FSP_PAL_PRODUCT) String object,
			@RequestHeader(value = ApplicationConstant.OPERATION_HEADER, defaultValue=ApplicationConstant.READ) String operation)  throws PALServiceException {
		
		log.info("Beginning of ProductAttributeListing: getPALProductPersonnel :: {}" , palProductRequest);
				
		return new ResponseEntity<>(new AppResponse<>(palService.getPALProductPersonnel(palProductRequest), Boolean.TRUE),
				HttpStatus.OK);
	}
	
	/**
	 * update PAL Product Personnel details
	 * @param palProductUpdateRequest request to update product personnel
	 * @return AppResponse
	 */
	@PutMapping(value="/PALProduct/Personnel")
	@ApiOperation(value = "updatePALProductPersonnel", response = AppResponse.class, tags = "PALProduct")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success"),
			@ApiResponse(code = 204, message = "No data"), @ApiResponse(code = 401, message = "Not authorized!"),
			@ApiResponse(code = 404, message = "Not found"), @ApiResponse(code = 400, message = "Bad request") })
	public ResponseEntity<AppResponse<PALProductResponse>> updatePALProductPersonnel(
			@RequestBody PALProductUpdateRequest palProductUpdateRequest,
			@RequestHeader(value = ApplicationConstant.JWTHEADER) String jwtToken,
			@RequestHeader(value = ApplicationConstant.ACL_HEADER, defaultValue = "true") String isAcl,
			@RequestHeader(value = ApplicationConstant.MODULE_HEADER, defaultValue = ApplicationConstant.OFP_MODULE) String module ,
			@RequestHeader(value = ApplicationConstant.OBJECT_HEADER, defaultValue = ApplicationConstant.FSP_PAL_PRODUCT) String object,
			@RequestHeader(value = ApplicationConstant.OPERATION_HEADER, defaultValue=ApplicationConstant.UPDATE) String operation)  throws PALServiceException {
		
		log.info("Beginning of ProductAttributeListing: updatePALProductPersonnel :: {}" , palProductUpdateRequest);
				
		return new ResponseEntity<>(new AppResponse<>(palService.updatePALProductPersonnel(palProductUpdateRequest), Boolean.TRUE),
				HttpStatus.OK);
	}

	/**
	 * Get the product progress
	 * @param palProductRequest request to fetch product progress
	 * @return AppResponse
	 * @throws PALServiceException
	 */
	@PostMapping(value="/PALProduct/Progress")
	@ApiOperation(value = "getPALProductProgress", response = AppResponse.class, tags = "PALProduct")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success"),
			@ApiResponse(code = 204, message = "No data"), @ApiResponse(code = 401, message = "Not authorized!"),
			@ApiResponse(code = 404, message = "Not found"), @ApiResponse(code = 400, message = "Bad request") })
	public ResponseEntity<AppResponse<PALProductResponse>> getPALProductProgress(
			@RequestBody PALProductRequest palProductRequest,
			@RequestHeader(value = ApplicationConstant.JWTHEADER) String jwtToken,
			@RequestHeader(value = ApplicationConstant.ACL_HEADER, defaultValue = "true") String isAcl,
			@RequestHeader(value = ApplicationConstant.MODULE_HEADER, defaultValue = ApplicationConstant.OFP_MODULE) String module ,
			@RequestHeader(value = ApplicationConstant.OBJECT_HEADER, defaultValue = ApplicationConstant.FSP_PAL_PRODUCT) String object,
			@RequestHeader(value = ApplicationConstant.OPERATION_HEADER, defaultValue=ApplicationConstant.READ) String operation)  throws PALServiceException {

		log.info("Beginning of ProductAttributeListing: getPALProductProgress :: {}" , palProductRequest);

		return new ResponseEntity<>(new AppResponse<>(palService.getPALProductProgress(palProductRequest), Boolean.TRUE),
				HttpStatus.OK);
	}
	
	/**
	 * update PAL Product Information details
	 * @param palProductUpdateRequest
	 * @return AppResponse
	 */
	@PutMapping(value="/PALProduct/Information")
	@ApiOperation(value = "updatePALProductInformation", response = AppResponse.class, tags = "PALProduct")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success"),
			@ApiResponse(code = 204, message = "No data"), @ApiResponse(code = 401, message = "Not authorized!"),
			@ApiResponse(code = 404, message = "Not found"), @ApiResponse(code = 400, message = "Bad request") })
	public ResponseEntity<AppResponse<PALProductResponse>> updatePALProductInformation(
			@RequestBody PALProductUpdateRequest palProductUpdateRequest,
			@RequestHeader(value = ApplicationConstant.JWTHEADER) String jwtToken,
			@RequestHeader(value = ApplicationConstant.ACL_HEADER, defaultValue = "true") String isAcl,
			@RequestHeader(value = ApplicationConstant.MODULE_HEADER, defaultValue = ApplicationConstant.OFP_MODULE) String module ,
			@RequestHeader(value = ApplicationConstant.OBJECT_HEADER, defaultValue = ApplicationConstant.FSP_PAL_PRODUCT) String object,
			@RequestHeader(value = ApplicationConstant.OPERATION_HEADER, defaultValue=ApplicationConstant.UPDATE) String operation)  throws PALServiceException {
		
		log.info("Beginning of ProductAttributeListing: updatePALProductPersonnel :: {}" , palProductUpdateRequest);
				
		return new ResponseEntity<>(new AppResponse<>(palService.updatePALProductInformation(palProductUpdateRequest), Boolean.TRUE),
				HttpStatus.OK);
	}

	/**
	 * Get the product progress
	 *
	 * @param palProductRequest
	 * @return AppResponse
	 * @throws PALServiceException
	 */
	@PostMapping(value="/PALProduct/Auditlogs")
	@ApiOperation(value = "getPALProductAuditlogs", response = AppResponse.class, tags = "PALProduct")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success"),
			@ApiResponse(code = 204, message = "No data"), @ApiResponse(code = 401, message = "Not authorized!"),
			@ApiResponse(code = 404, message = "Not found"), @ApiResponse(code = 400, message = "Bad request") })
	public ResponseEntity<AppResponse<PALProductResponse>> getPALProductAuditlogs(
			@RequestBody PALProductRequest palProductRequest,
			@RequestHeader(value = ApplicationConstant.JWTHEADER) String jwtToken,
			@RequestHeader(value = ApplicationConstant.ACL_HEADER, defaultValue = "true") String isAcl,
			@RequestHeader(value = ApplicationConstant.MODULE_HEADER, defaultValue = ApplicationConstant.OFP_MODULE) String module ,
			@RequestHeader(value = ApplicationConstant.OBJECT_HEADER, defaultValue = ApplicationConstant.FSP_PAL_PRODUCT) String object,
			@RequestHeader(value = ApplicationConstant.OPERATION_HEADER, defaultValue=ApplicationConstant.READ) String operation)  throws PALServiceException {

		log.info("Beginning of ProductAttributeListing: getPALProductAuditlogs :: {}" , palProductRequest);

		return new ResponseEntity<>(new AppResponse<>(palService.getPALProductAuditlogs(palProductRequest), Boolean.TRUE),
				HttpStatus.OK);
	}

	/**
	 * Get the all product fields
	 *
	 * @return AppResponse
	 * @throws PALServiceException
	 */
	@GetMapping(value = "/PALProduct/getPalFields")
	@ApiOperation(value = "getPalFields", response = AppResponse.class, tags = "PALProduct")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success"),
			@ApiResponse(code = 204, message = "No data"), @ApiResponse(code = 401, message = "Not authorized!"),
			@ApiResponse(code = 404, message = "Not found"), @ApiResponse(code = 400, message = "Bad request") })
	public ResponseEntity<AppResponse<List<PALFields>>> getPalFields() {
		return new ResponseEntity<>(new AppResponse<>(palService.getPalFields(), Boolean.TRUE),
				HttpStatus.OK);
	}
	
	/**
	 * Get the project product list details
	 *
	 * @param palProjectRequest
	 * @return AppResponse
	 * @throws PALServiceException
	 */
	@PostMapping(value="/PALProject/ProductList")
	@ApiOperation(value = "getPALProductList", response = AppResponse.class, tags = "PALProject")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success"),
			@ApiResponse(code = 204, message = "No data"), @ApiResponse(code = 401, message = "Not authorized!"),
			@ApiResponse(code = 404, message = "Not found"), @ApiResponse(code = 400, message = "Bad request") })
	public ResponseEntity<AppResponse<PALProjectResponse>> getPALProductList(
			@RequestBody PALProjectRequest palProjectRequest,
			@RequestHeader(value = ApplicationConstant.JWTHEADER) String jwtToken,
			@RequestHeader(value = ApplicationConstant.ACL_HEADER, defaultValue = "true") String isAcl,
			@RequestHeader(value = ApplicationConstant.MODULE_HEADER, defaultValue = ApplicationConstant.OFP_MODULE) String module ,
			@RequestHeader(value = ApplicationConstant.OBJECT_HEADER, defaultValue = ApplicationConstant.FSP_PAL_PROJECT_DETAILS) String object,
			@RequestHeader(value = ApplicationConstant.OPERATION_HEADER, defaultValue=ApplicationConstant.READ) String operation)  throws PALServiceException {

		log.info("Beginning of ProductAttributeListing: getPALProductList :: {}" , palProjectRequest);

		return new ResponseEntity<>(new AppResponse<>(palService.getPALProductList(palProjectRequest), Boolean.TRUE),
				HttpStatus.OK);
	}

/**
	 * Get the project details
	 *
	 * @param palProjectRequest
	 * @return AppResponse
	 * @throws PALServiceException
	 */

	@PostMapping(value="/PALProject/ProjectDetails")
	@ApiOperation(value = "getProjectDetails", response = AppResponse.class, tags = "PALProject")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success"),
			@ApiResponse(code = 204, message = "No data"), @ApiResponse(code = 401, message = "Not authorized!"),
			@ApiResponse(code = 404, message = "Not found"), @ApiResponse(code = 400, message = "Bad request") })
	public ResponseEntity<AppResponse<PALProjectResponse>> getProjectDetails(
			@RequestBody PALProjectRequest palProjectRequest,
			@RequestHeader(value = ApplicationConstant.JWTHEADER) String jwtToken,
			@RequestHeader(value = ApplicationConstant.ACL_HEADER, defaultValue = "true") String isAcl,
			@RequestHeader(value = ApplicationConstant.MODULE_HEADER, defaultValue = ApplicationConstant.OFP_MODULE) String module ,
			@RequestHeader(value = ApplicationConstant.OBJECT_HEADER, defaultValue = ApplicationConstant.FSP_PAL_PROJECT_DETAILS) String object,
			@RequestHeader(value = ApplicationConstant.OPERATION_HEADER, defaultValue=ApplicationConstant.READ) String operation)  throws PALServiceException {


		return new ResponseEntity<>(new AppResponse<>(palService.getProjectDetails(palProjectRequest), Boolean.TRUE),
				HttpStatus.OK);
	}

	/**
	 * Get the project progress details
	 *
	 * @param palProjectRequest
	 * @return AppResponse
	 * @throws PALServiceException
	 */
	@PostMapping(value="/PALProject/Progress")
	@ApiOperation(value = "getPALProjectProgress", response = AppResponse.class, tags = "PALProject")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success"),
			@ApiResponse(code = 204, message = "No data"), @ApiResponse(code = 401, message = "Not authorized!"),
			@ApiResponse(code = 404, message = "Not found"), @ApiResponse(code = 400, message = "Bad request") })
	public ResponseEntity<AppResponse<PALProjectResponse>> getPALProjectProgress(
			@RequestBody PALProjectRequest palProjectRequest,
			@RequestHeader(value = ApplicationConstant.JWTHEADER) String jwtToken,
			@RequestHeader(value = ApplicationConstant.ACL_HEADER, defaultValue = "true") String isAcl,
			@RequestHeader(value = ApplicationConstant.MODULE_HEADER, defaultValue = ApplicationConstant.OFP_MODULE) String module ,
			@RequestHeader(value = ApplicationConstant.OBJECT_HEADER, defaultValue = ApplicationConstant.FSP_PAL_PROJECT_DETAILS) String object,
			@RequestHeader(value = ApplicationConstant.OPERATION_HEADER, defaultValue=ApplicationConstant.READ) String operation)  throws PALServiceException {

		log.info("Beginning of ProductAttributeListing: getPALProjectProgress :: {}" , palProjectRequest);

		return new ResponseEntity<>(new AppResponse<>(palService.getPALProjectProgress(palProjectRequest), Boolean.TRUE),
				HttpStatus.OK);
	}
	
	/**
	 * Add New PAL Project
	 * @param palProjectUpdateRequest
	 * @return AppResponse
	 * @throws PALServiceException
	 */
	@PostMapping(value="/PALProject/addProject")
	@ApiOperation(value = "addPALProject", response = AppResponse.class, tags = "PALProject")
	@ApiResponses(value = { @ApiResponse(code = 201, message = "Created"),
			@ApiResponse(code = 204, message = "No data"), @ApiResponse(code = 401, message = "Not authorized!"),
			@ApiResponse(code = 404, message = "Not found"), @ApiResponse(code = 400, message = "Bad request") })
	public ResponseEntity<AppResponse<PALProjectResponse>> addPALProject(
			@RequestBody PALProjectUpdateRequest palProjectUpdateRequest,
			@RequestHeader(value = ApplicationConstant.JWTHEADER) String jwtToken,
			@RequestHeader(value = ApplicationConstant.ACL_HEADER, defaultValue = "true") String isAcl,
			@RequestHeader(value = ApplicationConstant.MODULE_HEADER, defaultValue = ApplicationConstant.OFP_MODULE) String module ,
			@RequestHeader(value = ApplicationConstant.OBJECT_HEADER, defaultValue = ApplicationConstant.FSP_PAL_PROJECT) String object,
			@RequestHeader(value = ApplicationConstant.OPERATION_HEADER, defaultValue=ApplicationConstant.CREATE) String operation)  throws PALServiceException {

		log.info("Beginning of ProductAttributeListing: addPALProject :: {}" , palProjectUpdateRequest);
		return new ResponseEntity<>(new AppResponse<>(palService.addPALProject(palProjectUpdateRequest), Boolean.TRUE),
				HttpStatus.CREATED);
		
	}

	
	/**
	 * Update PAL Project
	 * @param palProjectUpdateRequest
	 * @return AppResponse
	 * @throws PALServiceException
	 */
	@PutMapping(value="/PALProject/updateProject")
	@ApiOperation(value = "updatePALProject", response = AppResponse.class, tags = "PALProject")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success"),
			@ApiResponse(code = 204, message = "No data"), @ApiResponse(code = 401, message = "Not authorized!"),
			@ApiResponse(code = 404, message = "Not found"), @ApiResponse(code = 400, message = "Bad request") })
	public ResponseEntity<AppResponse<PALProjectResponse>> updatePALProject(
			@RequestBody PALProjectUpdateRequest palProjectUpdateRequest,
			@RequestHeader(value = ApplicationConstant.JWTHEADER) String jwtToken,
			@RequestHeader(value = ApplicationConstant.ACL_HEADER, defaultValue = "true") String isAcl,
			@RequestHeader(value = ApplicationConstant.MODULE_HEADER, defaultValue = ApplicationConstant.OFP_MODULE) String module ,
			@RequestHeader(value = ApplicationConstant.OBJECT_HEADER, defaultValue = ApplicationConstant.FSP_PAL_PROJECT_DETAILS) String object,
			@RequestHeader(value = ApplicationConstant.OPERATION_HEADER, defaultValue=ApplicationConstant.UPDATE) String operation)  throws PALServiceException {

		log.info("Beginning of ProductAttributeListing: updatePALProject :: {}" , palProjectUpdateRequest);
		return new ResponseEntity<>(new AppResponse<>(palService.updatePALProject(palProjectUpdateRequest), Boolean.TRUE),
				HttpStatus.OK);
		
	}

	/**
	 * Get the project list details
	 * @param projectFilter
	 * @return AppResponse
	 * @throws PALServiceException
	 */
	@PostMapping(value = "/PALProject/ProjectList")
	@ApiOperation(value = "getPALProjectList", response = AppResponse.class, tags = "PALProject")
	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success"),
			@ApiResponse(code = 204, message = "No data"), @ApiResponse(code = 401, message = "Not authorized!"),
			@ApiResponse(code = 404, message = "Not found"), @ApiResponse(code = 400, message = "Bad request")})
	public ResponseEntity<AppResponse<List<PALProjectResponse>>> getPALProjectList(
			@RequestBody ProjectFilter projectFilter,
			@RequestHeader(value = ApplicationConstant.JWTHEADER) String jwtToken,
			@RequestHeader(value = ApplicationConstant.ACL_HEADER, defaultValue = "true") String isAcl,
			@RequestHeader(value = ApplicationConstant.MODULE_HEADER, defaultValue = ApplicationConstant.OFP_MODULE) String module ,
			@RequestHeader(value = ApplicationConstant.OBJECT_HEADER, defaultValue = ApplicationConstant.FSP_PAL_PROJECT) String object,
			@RequestHeader(value = ApplicationConstant.OPERATION_HEADER, defaultValue=ApplicationConstant.READ) String operation) throws PALServiceException {

		return new ResponseEntity<>(new AppResponse<>(palService.getPALProjectList(projectFilter), Boolean.TRUE),
				HttpStatus.OK);
	}

	/**
	 * Export data
	 *@param palExportRequest
	 * @return
	 */
	@PostMapping(value = "/download")
	@ApiOperation(value = "download", response = ByteArrayResource.class, tags = "Export")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success"),
			@ApiResponse(code = 204, message = "No data"), @ApiResponse(code = 401, message = "Not authorized!"),
			@ApiResponse(code = 404, message = "Not found"), @ApiResponse(code = 400, message = "Bad request") })
	public ResponseEntity<ByteArrayResource> download(
			@RequestBody PALExportRequest palExportRequest,
			@RequestHeader(value = ApplicationConstant.JWTHEADER) String jwtToken,
			@RequestHeader(value = ApplicationConstant.ACL_HEADER, defaultValue = "true") String isAcl,
			@RequestHeader(value = ApplicationConstant.MODULE_HEADER, defaultValue = ApplicationConstant.OFP_MODULE) String module ,
			@RequestHeader(value = ApplicationConstant.OBJECT_HEADER, defaultValue = ApplicationConstant.FSP_PAL_PROJECT_DETAILS) String object,
			@RequestHeader(value = ApplicationConstant.OPERATION_HEADER, defaultValue=ApplicationConstant.READ) String operation) {
		PALExportResponse palExportResponse = palService.palExportData(palExportRequest);
		HttpHeaders header = new HttpHeaders();
		header.setContentType(new MediaType("application", "force-download"));
		header.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename="+palExportResponse.getFileName());
		return new ResponseEntity<>(palExportResponse.getOut(),
				header, HttpStatus.OK);
	}


	/**
	 * update PAL Project Personnel details
	 * @param palProjectPersonnelUpdateRequest
	 * @return AppResponse
	 */
	@PutMapping(value="/PALProject/Personnel")
	@ApiOperation(value = "updatePALProjectPersonnel", response = AppResponse.class, tags = "PALProject")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success"),
			@ApiResponse(code = 204, message = "No data"), @ApiResponse(code = 401, message = "Not authorized!"),
			@ApiResponse(code = 404, message = "Not found"), @ApiResponse(code = 400, message = "Bad request") })
	public ResponseEntity<AppResponse<PALProjectResponse>> updatePALProjectPersonnel(
			@RequestBody PALProjectPersonnelUpdateRequest palProjectPersonnelUpdateRequest,
			@RequestHeader(value = ApplicationConstant.JWTHEADER) String jwtToken,
			@RequestHeader(value = ApplicationConstant.ACL_HEADER, defaultValue = "true") String isAcl,
			@RequestHeader(value = ApplicationConstant.MODULE_HEADER, defaultValue = ApplicationConstant.OFP_MODULE) String module ,
			@RequestHeader(value = ApplicationConstant.OBJECT_HEADER, defaultValue = ApplicationConstant.FSP_PAL_PROJECT_DETAILS) String object,
			@RequestHeader(value = ApplicationConstant.OPERATION_HEADER, defaultValue=ApplicationConstant.UPDATE) String operation)  throws PALServiceException {

		log.info("Beginning of ProductAttributeListing: updatePALProjectPersonnel :: {}" , palProjectPersonnelUpdateRequest);

		return new ResponseEntity<>(new AppResponse<>(palService.updatePALProjectPersonnel(palProjectPersonnelUpdateRequest), Boolean.TRUE),
				HttpStatus.OK);
	}

	/**
	 * get all PAL Configuration details
	 * @param configurationIds PAL Configuration Ids
	 * @return List<PALConfiguration></PALConfiguration>
	 */
	@PostMapping(value = "/PALConfigurations")
	@ApiOperation(value = "PALConfigurations", response = AppResponse.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success"),
			@ApiResponse(code = 204, message = "No data"), @ApiResponse(code = 401, message = "Not authorized!"),
			@ApiResponse(code = 404, message = "Not found"), @ApiResponse(code = 400, message = "Bad request") })
	public ResponseEntity<AppResponse<List<PALConfiguration>>> getPALConfigurations(@RequestBody List<String> configurationIds) {
		return new ResponseEntity<>(new AppResponse<>(palService.getPALConfigurations(configurationIds), Boolean.TRUE),
				HttpStatus.OK);
	}

	/**
	 * Add the product into the project
	 * @param duplicateProductRequest
	 * @return Appresponse
	 * @throws PALServiceException
	 */
	@PostMapping(value="/PALProduct/duplicateProducts")
	@ApiOperation(value = "duplicateProducts", response = AppResponse.class, tags = "PALProduct")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success"),
			@ApiResponse(code = 204, message = "No data"), @ApiResponse(code = 401, message = "Not authorized!"),
			@ApiResponse(code = 404, message = "Not found"), @ApiResponse(code = 400, message = "Bad request") })
	public ResponseEntity<AppResponse<DuplicateProductResponse>> duplicateProducts(
			@RequestBody DuplicateProductRequest duplicateProductRequest,
			@RequestHeader(value = ApplicationConstant.JWTHEADER) String jwtToken,
			@RequestHeader(value = ApplicationConstant.ACL_HEADER, defaultValue = "true") String isAcl,
			@RequestHeader(value = ApplicationConstant.MODULE_HEADER, defaultValue = ApplicationConstant.OFP_MODULE) String module ,
			@RequestHeader(value = ApplicationConstant.OBJECT_HEADER, defaultValue = ApplicationConstant.FSP_PAL_PROJECT_DETAILS) String object,
			@RequestHeader(value = ApplicationConstant.OPERATION_HEADER, defaultValue=ApplicationConstant.CREATE) String operation)  throws PALServiceException {

		log.info("Beginning of ProductAttributeListing: duplicateProducts :: {}" , duplicateProductRequest);

		return new ResponseEntity<>(new AppResponse<>(palService.duplicateProducts(duplicateProductRequest), Boolean.TRUE),
				HttpStatus.OK);
	}

	/**
	 * Bulk update a single field in multiple products
	 * @param bulkProductUpdateRequest Bulk Update product request
	 * @return Appresponse
	 * @throws PALServiceException
	 */
	@PostMapping(value="/PALProduct/bulkUpdateInformation")
	@ApiOperation(value = "bulkUpdateInformation", response = AppResponse.class, tags = "PALProduct")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success"),
			@ApiResponse(code = 204, message = "No data"), @ApiResponse(code = 401, message = "Not authorized!"),
			@ApiResponse(code = 404, message = "Not found"), @ApiResponse(code = 400, message = "Bad request") })
	public ResponseEntity<AppResponse<BulkProductUpdateResponse>> bulkUpdateInformation(
			@RequestBody BulkProductUpdateRequest bulkProductUpdateRequest,
			@RequestHeader(value = ApplicationConstant.JWTHEADER) String jwtToken,
			@RequestHeader(value = ApplicationConstant.ACL_HEADER, defaultValue = "true") String isAcl,
			@RequestHeader(value = ApplicationConstant.MODULE_HEADER, defaultValue = ApplicationConstant.OFP_MODULE) String module ,
			@RequestHeader(value = ApplicationConstant.OBJECT_HEADER, defaultValue = ApplicationConstant.FSP_PAL_PROJECT_DETAILS) String object,
			@RequestHeader(value = ApplicationConstant.OPERATION_HEADER, defaultValue=ApplicationConstant.UPDATE) String operation)  throws PALServiceException {

		log.info("Beginning of ProductAttributeListing: bulkUpdateInformation :: {}" , bulkProductUpdateRequest);

		return new ResponseEntity<>(new AppResponse<>(palService.bulkUpdateInformation(bulkProductUpdateRequest), Boolean.TRUE),
				HttpStatus.OK);
	}	

	@PostMapping(value = "/PALProduct/bulkInformation")
	@ApiOperation(value = "getBulkProductInformations", response = AppResponse.class, tags = "PALProduct")
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success"),
			@ApiResponse(code = 204, message = "No data"), @ApiResponse(code = 401, message = "Not authorized!"),
			@ApiResponse(code = 404, message = "Not found"), @ApiResponse(code = 400, message = "Bad request") })
	public ResponseEntity<AppResponse<BulkProductResponse>> getBulkProductInformations(@RequestBody BulkProductRequest bulkProductRequest,
			@RequestHeader(value = ApplicationConstant.JWTHEADER) String jwtToken,
			@RequestHeader(value = ApplicationConstant.ACL_HEADER, defaultValue = "true") String isAcl,
			@RequestHeader(value = ApplicationConstant.MODULE_HEADER, defaultValue = ApplicationConstant.OFP_MODULE) String module,
			@RequestHeader(value = ApplicationConstant.OBJECT_HEADER, defaultValue = ApplicationConstant.FSP_PAL_PRODUCT) String object,
			@RequestHeader(value = ApplicationConstant.OPERATION_HEADER, defaultValue = ApplicationConstant.READ) String operation) {
		return new ResponseEntity<>(new AppResponse<>(palService.getBulkProductInformations(bulkProductRequest), Boolean.TRUE),
				HttpStatus.OK);
	}

	/**
	 * Update Configurations from ES into PAL DB
	 * @return Response
	 * @throws PALServiceException
	 */
	@PostMapping("/updatePalConfigurations")
	@ApiOperation(value = "updatePalConfigurations", response = AppResponse.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success"),
			@ApiResponse(code = 204, message = "No data"), @ApiResponse(code = 401, message = "Not authorized!"),
			@ApiResponse(code = 404, message = "Not found"), @ApiResponse(code = 400, message = "Bad request") })
	@Scheduled(cron = "${pal.configuration.update.cron.expression}")
	public ResponseEntity<AppResponse<Map<String,String>>> updatePalConfiguration(){
		return new ResponseEntity<>(new AppResponse<>(palService.updatePALConfigs(), Boolean.TRUE),
				HttpStatus.OK);
	}

	/**
	 * Deletes project or products from db
	 * @param palDeleteRequest request
	 * @return AppResponse
	 * @throws PALServiceException
	 */
	@PostMapping(value = "/PALProduct/deleteProjectOrProducts")
	@ApiOperation(value = "deleteProjectOrProducts", response = AppResponse.class, tags = "PALProduct")
	@ApiResponses(value = {@ApiResponse(code = 200, message = "Success"),
			@ApiResponse(code = 204, message = "No data"), @ApiResponse(code = 401, message = "Not authorized!"),
			@ApiResponse(code = 404, message = "Not found"), @ApiResponse(code = 400, message = "Bad request")})
	public ResponseEntity<AppResponse<PALDeleteResponse>> deleteProjectOrProducts(
			@RequestBody PALDeleteRequest palDeleteRequest,
			@RequestHeader(value = ApplicationConstant.JWTHEADER) String jwtToken,
			@RequestHeader(value = ApplicationConstant.ACL_HEADER, defaultValue = "true") String isAcl,
			@RequestHeader(value = ApplicationConstant.MODULE_HEADER, defaultValue = ApplicationConstant.OFP_MODULE) String module ,
			@RequestHeader(value = ApplicationConstant.OBJECT_HEADER, defaultValue = ApplicationConstant.FSP_PAL_PROJECT) String object,
			@RequestHeader(value = ApplicationConstant.OPERATION_HEADER, defaultValue=ApplicationConstant.DELETE) String operation) throws PALServiceException {

		return new ResponseEntity<>(new AppResponse<>(palService.deleteProjectOrProducts(palDeleteRequest), Boolean.TRUE),
				HttpStatus.OK);
	}
}
