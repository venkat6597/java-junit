package com.marksandspencer.foodshub.pal.service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marksandspencer.assemblyservice.config.security.utils.UiAuthenticationToken;
import com.marksandspencer.assemblyservice.config.transfer.AccessControlInfo;
import com.marksandspencer.foodshub.pal.constant.ErrorCode;
import com.marksandspencer.foodshub.pal.dao.ProductAttributeListingDao;
import com.marksandspencer.foodshub.pal.domain.PALRole;
import com.marksandspencer.foodshub.pal.exception.PALServiceException;
import com.marksandspencer.foodshub.pal.serviceImpl.UserDetailsServiceImpl;
import com.marksandspencer.foodshub.pal.transfer.AppResponse;
import com.marksandspencer.foodshub.pal.transfer.UserDetails;
import com.marksandspencer.foodshub.pal.utility.TestUtility;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserDetailsServiceTest {

    @InjectMocks
    UserDetailsServiceImpl  userDetailsService = new UserDetailsServiceImpl();

    @Mock
    ProductAttributeListingDao palDao;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private ProductAttributeListingService palService;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private void testSecurityContextHolder(String role, boolean organization) {
        when(securityContext.getAuthentication()).thenReturn(TestUtility.getUserAuthentication(role));
        SecurityContextHolder.setContext(securityContext);
        UiAuthenticationToken auth = userDetailsService.getUIAuthentication();

        assertNotNull(auth);
        assertEquals(role,auth.getPrincipal().toString());
        assertEquals(organization,!CollectionUtils.isEmpty(auth.getUserAccessInfo().getOrganizations()));
        assertEquals(role,auth.getUserAccessInfo().getUserRoleList().get(0).getRoleName());
        assertEquals(organization, auth.getUserAccessInfo().getUserRoleList().get(0).isOrganizationEnabled());
    }

    @Test
    public void userAuthenticationTest() {
        testSecurityContextHolder("projectManager", false);
        testSecurityContextHolder("productDeveloper", false);
        testSecurityContextHolder("buyer", false);
        testSecurityContextHolder("supplier", true);
    }

    @Test
    //user not in AD
    public void uiNoUserAuthenticationTest() {
        String role = "nouser";
        boolean organization = false;

        expectedException.expect(PALServiceException.class);
        expectedException.expectMessage(ErrorCode.GENERAL_ERROR.getErrorMessage());

        testSecurityContextHolder(role, organization);
    }

    @Test
    public void userDetailsTest() {
        String role = "projectManager";
        when(securityContext.getAuthentication()).thenReturn(TestUtility.getUserAuthentication(role));
        SecurityContextHolder.setContext(securityContext);
        UserDetails userDetails = userDetailsService.validateUserDetails(role, null);
        assertNotNull(userDetails);
        assertEquals(role, userDetails.getUserRole().getRoleName());

        role = "productDeveloper";
        when(securityContext.getAuthentication()).thenReturn(TestUtility.getUserAuthentication(role));
        SecurityContextHolder.setContext(securityContext);
        userDetails = userDetailsService.validateUserDetails(role, null);
        assertNotNull(userDetails);
        assertEquals(role, userDetails.getUserRole().getRoleName());

        role = "buyer";
        when(securityContext.getAuthentication()).thenReturn(TestUtility.getUserAuthentication(role));
        SecurityContextHolder.setContext(securityContext);
        userDetails = userDetailsService.validateUserDetails(role, null);
        assertNotNull(userDetails);
        assertEquals(role, userDetails.getUserRole().getRoleName());

        role = "supplier";
        when(securityContext.getAuthentication()).thenReturn(TestUtility.getUserAuthentication(role));
        SecurityContextHolder.setContext(securityContext);
        userDetails = userDetailsService.validateUserDetails(role, null);
        assertNotNull(userDetails);
        assertEquals(role, userDetails.getUserRole().getRoleName());
    }

    @Test
    public void userValidAccessDetailsTest() {
        String role = "projectManager";
        when(securityContext.getAuthentication()).thenReturn(TestUtility.getUserAuthentication(role));
        SecurityContextHolder.setContext(securityContext);
        UserDetails userDetails = userDetailsService.validateUserDetails(role, null);
        assertNotNull(userDetails);
        assertEquals(role, userDetails.getUserRole().getRoleName());

        role = "productDeveloper";
        when(securityContext.getAuthentication()).thenReturn(TestUtility.getUserAuthentication(role));
        SecurityContextHolder.setContext(securityContext);
        userDetails = userDetailsService.validateUserDetails(role, null);
        assertNotNull(userDetails);
        assertEquals(role, userDetails.getUserRole().getRoleName());

        role = "buyer";
        when(securityContext.getAuthentication()).thenReturn(TestUtility.getUserAuthentication(role));
        SecurityContextHolder.setContext(securityContext);
        userDetails = userDetailsService.validateUserDetails(role, null);
        assertNotNull(userDetails);
        assertEquals(role, userDetails.getUserRole().getRoleName());

        role = "supplier";
        when(securityContext.getAuthentication()).thenReturn(TestUtility.getUserAuthentication(role));
        SecurityContextHolder.setContext(securityContext);
        userDetails = userDetailsService.validateUserDetails(role, null);
        assertNotNull(userDetails);
        assertEquals(role, userDetails.getUserRole().getRoleName());
    }
    
    @Test
    //User in AD but not in PAL
    public void buyerUserInvalidAccessTest() {
        String role = "buyer";
        expectedException.expect(PALServiceException.class);
        expectedException.expectMessage(ErrorCode.UNAUTHORIIZED.getErrorMessage());
        when(securityContext.getAuthentication()).thenReturn(TestUtility.getUserAuthentication(role));
        SecurityContextHolder.setContext(securityContext);
        userDetailsService.validateUserDetails(role, Arrays.asList("projectManager"));
    }
    
    @Test
    public void getUserDetailsTest() {
    	UserDetails userDetails = userDetailsService.getUserDetails();
    	assertNotNull(userDetails);
        assertEquals("supplier", userDetails.getUserRole().getRoleName());
        assertEquals(true, userDetails.getUserRole().isOrganizationEnabled());
        assertEquals("F01524", userDetails.getOrganizations().get(0));
    }
    
    @Test
    public void getAccessControlDetailsTest() {
    	String role="supplier";
    	AccessControlInfo accessControlInfo = userDetailsService.getAccessControlDetails(role);
    	assertNotNull(accessControlInfo);
        assertEquals(true, accessControlInfo.isReadAccess());
        assertEquals(false, accessControlInfo.isCreateAccess());
    }
}
