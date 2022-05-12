package com.marksandspencer.foodshub.pal.rest.client;

import java.util.List;

import com.marksandspencer.foodshub.pal.transfer.AuthAttribute;

public interface AuthzRestClient {

	List<AuthAttribute> getAuthAttribute(String origin,String jwt);
}
