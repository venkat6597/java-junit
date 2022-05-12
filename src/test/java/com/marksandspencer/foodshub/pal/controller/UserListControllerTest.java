package com.marksandspencer.foodshub.pal.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marksandspencer.foodshub.pal.dto.PALUser;
import com.marksandspencer.foodshub.pal.service.UserService;
import com.marksandspencer.foodshub.pal.transfer.AppResponse;
import com.marksandspencer.foodshub.pal.transfer.AuthzSupplierResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)

public class UserListControllerTest {

	@InjectMocks
	UserListController userListController = new UserListController();

	@Mock
	UserService userService;

	@Mock
	List<AuthzSupplierResponse> supplierIdResponse;
	
	@Captor
	ArgumentCaptor<String> jwtArgumentCaptor;
	
	String token = "JWT_TOKEN";

	@Before
	public void beforeTest() {
	}

	@Test
	public void getSuppliersTest() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		
		supplierIdResponse = mapper.readValue(new File("src/test/resources/UserListResponse/SupplierIdResponse.json"),
				mapper.getTypeFactory().constructCollectionLikeType(List.class, AuthzSupplierResponse.class));

		when(userService.getSupplierIds(Mockito.anyString())).thenReturn(supplierIdResponse);
		AppResponse<List<AuthzSupplierResponse>> supplierIdActualResponse = userListController.getSupplierCodes(token).getBody();
		Mockito.verify(userService,Mockito.times(1)).getSupplierIds(jwtArgumentCaptor.capture());
		assertEquals(supplierIdResponse, supplierIdActualResponse.getData());
		assertEquals(token, jwtArgumentCaptor.getValue());
	}

	@Test
	public void listUserByRolesTest() throws  IOException {
		ObjectMapper mapper = new ObjectMapper();
		AppResponse<List<PALUser>> expectedResponse = mapper.readValue(new File("src/test/resources/ProductAttributeListingResponse/ListUsersForRolesResponse.json"),
				new TypeReference<>() {
				});
		List<PALUser> expectedUsers = expectedResponse.getData();
		Map<String, List<PALUser>> expectedUsersMap = new HashMap<>();
		expectedUsersMap.put("123", expectedUsers);
		List<String> roles = Arrays.asList("123");

		when(userService.listUserByRoles(eq(roles))).thenReturn(expectedUsersMap);
		ResponseEntity<AppResponse<Map<String, List<PALUser>>>> res = userListController.listUserByRoles(roles);
		assertEquals(HttpStatus.OK, res.getStatusCode());
		assertEquals(expectedUsersMap, res.getBody().getData());
	}

}
