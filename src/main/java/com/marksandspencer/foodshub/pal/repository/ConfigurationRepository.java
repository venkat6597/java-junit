package com.marksandspencer.foodshub.pal.repository;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.marksandspencer.foodshub.pal.domain.Configuration;

/**
 * Interface of Configuration Repository
 */
@Repository
public interface ConfigurationRepository extends MongoRepository<Configuration, String>{

	/**
	 * get the supplier organizations list configured in the common configuration collection
	 * by type and pageObject as FSP PAL Projects will return the list of organizations that
	 * is allowed to access the PAL screen.
	 * 
	 * @param type
	 * @return Configuration
	 */
	@Cacheable("palSupplierConfigListEHCache")
	Configuration findAllByType(String type);
	
}
