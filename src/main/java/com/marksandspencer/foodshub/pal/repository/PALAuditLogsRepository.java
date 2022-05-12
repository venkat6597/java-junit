package com.marksandspencer.foodshub.pal.repository;

import com.marksandspencer.foodshub.pal.domain.PALAuditLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PALAuditLogsRepository extends MongoRepository<PALAuditLog, String> {
    Optional<PALAuditLog> findByProductId(String palProductId);
}
