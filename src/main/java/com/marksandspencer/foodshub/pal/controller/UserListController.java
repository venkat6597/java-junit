package com.marksandspencer.foodshub.pal.controller;

import com.marksandspencer.foodshub.pal.constant.ApplicationConstant;
import com.marksandspencer.foodshub.pal.dto.PALUser;
import com.marksandspencer.foodshub.pal.service.UserService;
import com.marksandspencer.foodshub.pal.transfer.AppResponse;
import com.marksandspencer.foodshub.pal.transfer.AuthzSupplierResponse;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/users")
public class UserListController {

	@Autowired
	UserService userService;

	@GetMapping(value = "/getSupplierCodes")
	@ApiOperation(value = "getSupplierCodes", response = AppResponse.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success"),
			@ApiResponse(code = 204, message = "No data"), @ApiResponse(code = 401, message = "Not authorized!"),
			@ApiResponse(code = 404, message = "Not found"), @ApiResponse(code = 400, message = "Bad request") })
	public ResponseEntity<AppResponse<List<AuthzSupplierResponse>>> getSupplierCodes(
			@RequestHeader(value = ApplicationConstant.JWTHEADER) String jwtToken) {
		return new ResponseEntity<>(new AppResponse<>(userService.getSupplierIds(jwtToken), Boolean.TRUE),
				HttpStatus.OK);
	}

	/**
	 * List down the PAL users
	 * @return List<PALUser>
	 */
	@PostMapping(value = "/listUserByRoles")
	@ApiOperation(value = "listUserByRoles", response = AppResponse.class)
	@ApiResponses(value = { @ApiResponse(code = 200, message = "Success"),
			@ApiResponse(code = 204, message = "No data"), @ApiResponse(code = 401, message = "Not authorized!"),
			@ApiResponse(code = 404, message = "Not found"), @ApiResponse(code = 400, message = "Bad request") })
	public ResponseEntity<AppResponse<Map<String, List<PALUser>>>> listUserByRoles(@RequestBody List<String> roles) {
		return new ResponseEntity<>(new AppResponse<>(userService.listUserByRoles(roles), Boolean.TRUE),
				HttpStatus.OK);
	}
}
