package com.marksandspencer.foodshub.pal.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Configuration
@ConfigurationProperties(prefix = "authz.service")
public class AuthzPropertyConfig {

	private String endPoint;
	
	private String authAttributes;
}
