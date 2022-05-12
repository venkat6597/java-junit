package com.marksandspencer.foodshub.pal.repository;

import com.marksandspencer.foodshub.pal.domain.PALRole;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PALRoleRepository extends MongoRepository<PALRole, String> {
}
