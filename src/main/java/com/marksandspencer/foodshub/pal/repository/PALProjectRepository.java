package com.marksandspencer.foodshub.pal.repository;


import com.marksandspencer.foodshub.pal.domain.PALProject;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PALProjectRepository extends MongoRepository<PALProject, String> {
	
}
