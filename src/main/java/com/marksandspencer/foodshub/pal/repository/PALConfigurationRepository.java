package com.marksandspencer.foodshub.pal.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.marksandspencer.foodshub.pal.domain.PALConfiguration;

@Repository
public interface PALConfigurationRepository extends MongoRepository<PALConfiguration, String> {
}
