package com.marksandspencer.foodshub.pal.constant;

/**
 * The enum Error code.
 */
public enum ErrorCode {

	/**
	 * The General error.
	 */
	GENERAL_ERROR("PAL-01", "Something went wrong, please try again"),
	/**
	 * The No data.
	 */
	NO_DATA("PAL-02", "No Data Available",204),
	/**
	 * The Invalid jwt token.
	 */
	INVALID_JWT_TOKEN("PAL-03", "Invalid JWT Token"),
	/**
	 * Request part is missing
	 */
	INVALID_REQUEST_DATA("PAL-04", "Bad Request"),
	/**
	 * Not authorized to access
	 */
	UNAUTHORIIZED("PAL-05", "Not authorized to access", 403),
	/**
	 * Invalid date format
	 */
	INVALID_DATE("PAL-06", "Invalid date format"),
	/**
	 * No Changes Identified to Update
	 */
	NO_CHANGES("PAL-07", "No Changes Identified to Update"),
	/**
	 * Missing Mandatory fields 
	 */
	MISSING_MANDATORY_FIELDS("PAL-08", "Missing mandatory fields"),
	/**
	 * Incorrect template
	 */
	INVALID_TEMPLATE_ID("PAL-09", "Invalid template(s)"),
	/**
	 * Invalid Id
	 */
	INVALID_PROJECT_ID("PAL-10", "Invalid Project(s)"),
	/**
	 * Invalid Id
	 */
	INVALID_PRODUCT_ID("PAL-10", "Invalid Product(s)"),
	/*
	 * Download PAL data failed
	 */
	DOWNLOAD_FAILED("PAL-11", "Download PAL data failed"),
    /**
     * Invalid role
     */
    INVALID_ROLE("PAL-12","Invalid role"),
	
    /**
     * FORBIDEEN
     */
    FORBIDEEN("PAL-13","Access denied",403),

	/**
	 * Invalid Number
	 */
	INVALID_NUMBER("PAL-14", "Invalid Number"),

	/**
	 * Kafka error
	 */
	KAFKA_ERROR("PAL-15", "Unable to send message"),

	/**
	 * Update Failed
	 */
	UPDATE_ERROR("PAL-16", "Update failed"),
	/**
	 * Unknown category id
	 */
	ES_ERROR("PAL-17", "Exception in Enterprise Service call"),
	/**
	 * Deletion Failed
	 */
	DELETE_ERROR("PAL-18", "Deletion Failed");

	private String errorMessage;
	private Integer statusCode;
	private String code;

	ErrorCode(final String code, final String errorMessage, final Integer statusCode) {
		this.code = code;
		this.errorMessage = errorMessage;
		this.statusCode = statusCode;
	}

	ErrorCode(final String code, final String errorMessage) {
		this.code = code;
		this.errorMessage = errorMessage;
	}

	/**
	 * Gets error code.
	 *
	 * @return the error code
	 */
	public String getErrorCode() {
		return code;
	}

	/**
	 * Gets error message.
	 *
	 * @return the error message
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * Gets status code.
	 *
	 * @return the status code
	 */
	public Integer getStatusCode() {
		return statusCode;
	}
}
