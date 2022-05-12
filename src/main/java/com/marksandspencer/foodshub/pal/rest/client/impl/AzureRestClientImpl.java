package com.marksandspencer.foodshub.pal.rest.client.impl;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.marksandspencer.foodshub.pal.constant.ErrorCode;
import com.marksandspencer.foodshub.pal.dto.PALUser;
import com.marksandspencer.foodshub.pal.exception.PALServiceException;
import com.marksandspencer.foodshub.pal.rest.client.AzureRest;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.DirectoryObjectCollectionWithReferencesPage;
import com.microsoft.graph.requests.DirectoryObjectCollectionWithReferencesRequestBuilder;
import com.microsoft.graph.requests.GraphServiceClient;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AzureRestClientImpl implements AzureRest {

	@Value("${azure.ad.clientId}")
	private String clientId;

	@Value("${azure.ad.tenantId}")
	private String tenantId;

	@Value("${azure.ad.appSecret}")
	private String appSecret;

	@Value("${azure.ad.scopeUrl}")
	private String scopeUrl;

	@Override
	public DirectoryObjectCollectionWithReferencesPage getUsersForRole(String role) {
		try {
			if (!role.trim().isEmpty() && !role.equals("null")) {
				final TokenCredentialAuthProvider tokenCredentialAuthProvider = new TokenCredentialAuthProvider(
						Arrays.asList(scopeUrl), getClientSecretCredential());
				final GraphServiceClient<Request> graphClient = GraphServiceClient.builder()
						.authenticationProvider(tokenCredentialAuthProvider).buildClient();
				return graphClient.groups(role).members().buildRequest().get();
			} else {
				throw new PALServiceException(ErrorCode.INVALID_REQUEST_DATA);
			}

		} catch (GraphServiceException e) {
			log.error("Exception occured on getUsersForRole [{}] ", e.getMessage());
			if(e.getResponseCode() == HttpStatus.SC_BAD_REQUEST){
				throw new PALServiceException(ErrorCode.INVALID_ROLE);
			}
			else{
				throw new PALServiceException(ErrorCode.GENERAL_ERROR);
			}
		}
		catch(Exception e){
			log.error("Exception occured on getUsersForRole [{}] ", e.getMessage());
			throw new PALServiceException(ErrorCode.GENERAL_ERROR);
		}
	}

	@Override
	public DirectoryObjectCollectionWithReferencesPage getUsersForRole(
			DirectoryObjectCollectionWithReferencesRequestBuilder nextPage) {
		try {
			return nextPage.buildRequest().get();
		} catch (Exception e) {
			log.error("Exception occured on getUsersForRole [{}] ", e.getMessage());
			throw new PALServiceException(e.getMessage());
		}
	}

	@Cacheable("palUserListEhCache")
	@Override
	public List<PALUser> listUserByRole(String role) {
		DirectoryObjectCollectionWithReferencesPage userList = getUsersForRole(role);
		List<PALUser> users = userMapping(userList);
		DirectoryObjectCollectionWithReferencesRequestBuilder nextPage = userList.getNextPage();
		while (nextPage != null) {
			userList = getUsersForRole(nextPage);
			users.addAll(userMapping(userList));
			nextPage = userList.getNextPage();
		}
		return users;
	}

	private List<PALUser> userMapping(DirectoryObjectCollectionWithReferencesPage userList) {
		return userList.getCurrentPage().stream().filter(u -> u instanceof User).map(u -> (User) u)
				.map(u -> PALUser.builder().id(u.id).name(u.displayName).email(u.mail).build())
				.collect(Collectors.toList());
	}

	private ClientSecretCredential getClientSecretCredential() {
		return new ClientSecretCredentialBuilder().clientId(clientId).clientSecret(appSecret).tenantId(tenantId)
				.build();
	}
}
