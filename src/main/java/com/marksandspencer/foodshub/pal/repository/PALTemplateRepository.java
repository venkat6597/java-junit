package com.marksandspencer.foodshub.pal.repository;

import com.marksandspencer.foodshub.pal.domain.PALTemplate;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PALTemplateRepository extends MongoRepository<PALTemplate, String> {

	Optional<PALTemplate> findByTemplateName(String templateName);
}
