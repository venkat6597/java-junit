package com.marksandspencer.foodshub.pal.util;

import com.marksandspencer.foodshub.pal.constants.UrlConstants;
import com.marksandspencer.foodshub.pal.exception.PALServiceException;

public enum QueryBuilder {
    QUERY_BUILDER;

    public static final String QUERY_STRING = "QUERY_STRING";
    private StringBuilder queryBuilder;

    public QueryBuilder queryParam(String param, String value) {

        if (value == null || param == null) {
            throw new PALServiceException("Query Param cannot take null param or null value");
        }
        if (!queryBuilder.toString().contains("?")) {
            queryBuilder.append("?");
        }
        queryBuilder.append(param).append("=").append(value).append("&");

        return QUERY_BUILDER;
    }

    public QueryBuilder newQuery() {
        queryBuilder = new StringBuilder();
        return QUERY_BUILDER;
    }


    public QueryBuilder baseUrl(String baseUrl) {
        queryBuilder.append(baseUrl);
        return QUERY_BUILDER;
    }

    public QueryBuilder pathParam(String poNumber) {
        queryBuilder.append(UrlConstants.SLASH).append(poNumber);
        return QUERY_BUILDER;
    }

    public String build() {
        return queryBuilder.toString();
    }
}