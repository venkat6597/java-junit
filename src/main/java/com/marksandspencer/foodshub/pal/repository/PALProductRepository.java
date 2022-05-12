package com.marksandspencer.foodshub.pal.repository;

import com.marksandspencer.foodshub.pal.domain.PALProduct;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PALProductRepository extends MongoRepository<PALProduct, String> {

	@Query("{'projectId': ?0}")
	List<PALProduct> findProductsByProjectId(String projectId);

}
