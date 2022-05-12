package com.marksandspencer.foodshub.pal.repository.custom;

import com.marksandspencer.foodshub.pal.domain.DataField;
import com.marksandspencer.foodshub.pal.domain.PALProduct;
import com.marksandspencer.foodshub.pal.domain.PALProject;
import com.marksandspencer.foodshub.pal.transfer.Filter;
import com.marksandspencer.foodshub.pal.transfer.ProjectFilter;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface PALCustomRepository {
    PALProject updatePALProject(PALProject palProject);

    List<PALProject> findPALProjectList(Set<String> palProjectIds, ProjectFilter projectFilter);

    List<PALProduct> findProductByFilterCondition(String projectId, Filter filter);

    List<PALProduct> findProducts(String projectId, List<String> productIds, List<String> suppliers);

    List<PALProduct> upsertProductFields(List<PALProduct> productBeforeUpdates, Map<String, List<DataField>> upsertProductFields, String userRole, String userName);
}
