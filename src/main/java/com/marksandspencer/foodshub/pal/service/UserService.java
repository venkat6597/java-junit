package com.marksandspencer.foodshub.pal.service;

import com.marksandspencer.foodshub.pal.dto.PALUser;
import com.marksandspencer.foodshub.pal.transfer.AuthzSupplierResponse;

import java.util.List;
import java.util.Map;

public interface UserService {

    List<AuthzSupplierResponse> getSupplierIds(String jwt);

    Map<String, List<PALUser>> listUserByRoles(List<String> roles);
}
