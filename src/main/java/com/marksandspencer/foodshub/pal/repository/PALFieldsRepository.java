package com.marksandspencer.foodshub.pal.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.marksandspencer.foodshub.pal.domain.PALFields;

@Repository
public interface PALFieldsRepository extends MongoRepository<PALFields, String> {
}
