package com.marksandspencer.foodshub.pal.service;

import com.marksandspencer.assemblyservice.config.transfer.AccessControlInfo;
import com.marksandspencer.foodshub.pal.transfer.UserDetails;

import java.util.List;

public interface UserDetailsService {

    UserDetails validateUserDetails(String palUserRole, List<String> accessibleUserRoleList);

    UserDetails getUserDetails();

    AccessControlInfo getAccessControlDetails(String userRole);
}
