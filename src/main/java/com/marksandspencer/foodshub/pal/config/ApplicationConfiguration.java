package com.marksandspencer.foodshub.pal.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScans;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.marksandspencer.assemblyservice.config.security.utils.UiAuthFilter;
import com.marksandspencer.foodshub.pal.interceptor.AuthInterceptor;

@Configuration
@PropertySource(value = "classpath:application-common.properties")
@ComponentScans({ @ComponentScan(basePackages = { "com.marksandspencer.assemblyservice.config" }),
		@ComponentScan(basePackages = "com.marksandspencer.foodshub.pal") })
@EnableRetry
@EnableCaching
public class ApplicationConfiguration implements WebMvcConfigurer {

	@Value("${supplier.forbbiden.urls}")
	private String[] supplierForbiddenUrls;
	
	@Autowired
	private AuthInterceptor authInterceptor;
	
	@Bean
	UiAuthFilter getUiAuthFilter() {
		return new UiAuthFilter();
	}
	
	@Override
	public void addInterceptors(InterceptorRegistry registry) {
	    registry.addInterceptor(authInterceptor).addPathPatterns(Arrays.asList(supplierForbiddenUrls));
	}
}
