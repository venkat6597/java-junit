package com.marksandspencer.foodshub.pal.rest.client;


import com.marksandspencer.foodshub.pal.dto.PALUser;
import com.microsoft.graph.requests.DirectoryObjectCollectionWithReferencesPage;
import com.microsoft.graph.requests.DirectoryObjectCollectionWithReferencesRequestBuilder;

import java.util.List;

public interface AzureRest {
	DirectoryObjectCollectionWithReferencesPage getUsersForRole(String role);
	DirectoryObjectCollectionWithReferencesPage getUsersForRole(DirectoryObjectCollectionWithReferencesRequestBuilder nextPage);
	List<PALUser> listUserByRole(String role);
}
