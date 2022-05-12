package com.marksandspencer.foodshub.pal.rest.client;

import com.marksandspencer.foodshub.pal.dto.Category;

public interface ESProductHierarchyServiceRestClient {
    /**
     * Gets categories.
     *
     * @return the categories
     */
    Category getCategories();
}
