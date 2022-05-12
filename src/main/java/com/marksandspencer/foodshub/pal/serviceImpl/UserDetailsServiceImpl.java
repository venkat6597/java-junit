package com.marksandspencer.foodshub.pal.serviceImpl;

import com.marksandspencer.assemblyservice.config.security.utils.UiAuthenticationToken;
import com.marksandspencer.assemblyservice.config.transfer.AccessControlInfo;
import com.marksandspencer.assemblyservice.config.transfer.UserAccessInfo;
import com.marksandspencer.assemblyservice.config.transfer.UserRole;
import com.marksandspencer.foodshub.pal.constant.ApplicationConstant;
import com.marksandspencer.foodshub.pal.constant.ErrorCode;
import com.marksandspencer.foodshub.pal.exception.PALServiceException;
import com.marksandspencer.foodshub.pal.service.UserDetailsService;
import com.marksandspencer.foodshub.pal.transfer.UserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private UserDetails getUserDetails(String palUserRole) {
        List<String> organisations = null;
        UserRole userRole = null;

        UiAuthenticationToken uiAuthentication = getUIAuthentication();
        UserAccessInfo userAccessInfo = uiAuthentication.getUserAccessInfo();
        if (!ObjectUtils.isEmpty(userAccessInfo)) {
            userRole = !CollectionUtils.isEmpty(userAccessInfo.getUserRoleList())
                    ? getUserRole(userAccessInfo.getUserRoleList(), palUserRole)
                    : null;
            if (!Objects.isNull(userRole) && userRole.isOrganizationEnabled())
                organisations = userAccessInfo.getOrganizations();
        }
        return UserDetails.builder().userRole(userRole).organizations(organisations)
                .email((String) uiAuthentication.getPrincipal()).build();
    }

    private UserRole getUserRole(List<UserRole> userRoles, String palUserRole) {
        for (UserRole userRole : userRoles) {
            if (userRole.getRoleName().equalsIgnoreCase(palUserRole)) {
                return userRole;
            } else if (userRole.isOrganizationEnabled() && ApplicationConstant.SUPPLIER.equalsIgnoreCase(palUserRole)) {
                userRole.setRoleName(ApplicationConstant.SUPPLIER);
                return userRole;
            }
        }
        log.error("UserDetailsServiceImpl -> getUserRole -> User not authorized to perform the action");
        throw new PALServiceException(ErrorCode.UNAUTHORIIZED);
    }

    /**
     * @return UiAuthenticationToken
     */
    public UiAuthenticationToken getUIAuthentication() {
        UiAuthenticationToken uiAuthenticationToken = (UiAuthenticationToken) SecurityContextHolder.getContext()
                .getAuthentication();
        if (ObjectUtils.isEmpty(uiAuthenticationToken)) {
            log.error("Failed to retrieve the user authentication object");
            throw new PALServiceException(ErrorCode.GENERAL_ERROR);
        }
        return uiAuthenticationToken;
    }

    /**
     * Validates the user details
     */
    @Override
    public UserDetails validateUserDetails(String palUserRole, List<String> accessibleUserRoleList) {

        UserDetails userDetails = getUserDetails(palUserRole);
        if (ApplicationConstant.SUPPLIER.equalsIgnoreCase(userDetails.getUserRole().getRoleName())
                && CollectionUtils.isEmpty(userDetails.getOrganizations())) {
            log.error("UserDetailsServiceImpl -> validateUserDetails -> User not authorized to perform the action");
            throw new PALServiceException(ErrorCode.UNAUTHORIIZED);
        }

        if (!CollectionUtils.isEmpty(accessibleUserRoleList) && !accessibleUserRoleList.contains(palUserRole)) {
            log.error("UserDetailsServiceImpl -> validateUserDetails -> User not authorized to perform the action");
            throw new PALServiceException(ErrorCode.UNAUTHORIIZED);
        }

        return userDetails;
    }

    @Override
    public UserDetails getUserDetails() {
        List<String> organisations = null;
        UserRole userRole = null;
        UiAuthenticationToken uiAuthentication = getUIAuthentication();
        UserAccessInfo userAccessInfo = uiAuthentication.getUserAccessInfo();
        if (!ObjectUtils.isEmpty(userAccessInfo)) {
            userRole = !CollectionUtils.isEmpty(userAccessInfo.getUserRoleList())
                    ? userAccessInfo.getUserRoleList().get(0)
                    : null;
            organisations = userAccessInfo.getOrganizations();
        }
        return UserDetails.builder().userRole(userRole).organizations(organisations)
                .email((String) uiAuthentication.getPrincipal()).build();
    }

    @Override
    public AccessControlInfo getAccessControlDetails(String userRole) {
        UserDetails userDetails = getUserDetails(userRole);
        if (!ObjectUtils.isEmpty(userDetails)) {
            return userDetails.getUserRole().getAccessControlInfoList().get(0);
        }
        return null;
    }
}
