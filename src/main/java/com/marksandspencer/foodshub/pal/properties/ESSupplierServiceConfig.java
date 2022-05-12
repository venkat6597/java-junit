package com.marksandspencer.foodshub.pal.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Configuration
@ConfigurationProperties(prefix = "es.supplier")
public class ESSupplierServiceConfig {

    private String endpoint;

    private String supplierIdUrl;

    private String username;

    private String password;

}
