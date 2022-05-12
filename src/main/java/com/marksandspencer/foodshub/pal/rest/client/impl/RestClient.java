package com.marksandspencer.foodshub.pal.rest.client.impl;

import java.util.Base64;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class RestClient {

	public static final String X_API_KEY = "X-Api-Key";

	/**
	 * createAuthHeaders
	 * 
	 * @param authUsername
	 * @param authPassword
	 * @return
	 */
	protected HttpHeaders createAuthHeaders(final String authUsername, final String authPassword) {
		return new HttpHeaders() {
			private static final long serialVersionUID = 9044102848770318966L;
			{
				setContentType(MediaType.APPLICATION_JSON);
				if (X_API_KEY.equals(authUsername)) {
					set(X_API_KEY, authPassword);
				}
				else {
					String auth = authUsername + ":" + authPassword;
					byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
					String authHeader = "Basic " + new String(encodedAuth);
					set("Authorization", authHeader);
				}
			}
		};
	}
}
