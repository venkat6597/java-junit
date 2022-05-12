package com.marksandspencer.foodshub.pal.properties;


import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * The type Enterprise service product hierarchy category config.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Configuration
@ConfigurationProperties(prefix = "es.producthierarchy")
public class EnterpriseServiceProductHierarchyConfig {

    private String endPoint;
    private String categoriesUrl;
    private String authUsername;
    private String authPassword;
    private String categoriesPageSize;
}