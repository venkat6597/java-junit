package com.marksandspencer.foodshub.pal.exception;

import com.marksandspencer.foodshub.pal.constant.ErrorCode;

/*** Generic Exception class to throw any customized exception */
public class PALServiceException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private String errorCode;
	private String errorMessage;
	private Integer statusCode;

	/**
	 * Instantiates a new Document service exception.
	 *
	 * @param errorMessage the error message
	 */
	public PALServiceException(String errorMessage) {
		super(errorMessage);
		this.errorMessage = errorMessage;
	}

  /**
	 * Instantiates a new Document service exception.
	 *
	 * @param errorCodeEnum the error code enum
	 */
	public PALServiceException(ErrorCode errorCodeEnum) {
		super(errorCodeEnum.getErrorMessage());
		this.errorCode = errorCodeEnum.getErrorCode();
		this.errorMessage = errorCodeEnum.getErrorMessage();
		this.statusCode = errorCodeEnum.getStatusCode();
	}

  /**
	 * Instantiates a new Document service exception.
	 *
	 * @param errorCode    the error code
	 * @param errorMessage the error message
	 * @param statusCode   the status code
	 */
	public PALServiceException(String errorCode, String errorMessage, Integer statusCode) {
		super(errorMessage);
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
		this.statusCode = statusCode;
	}

  /**
	 * Creates the custom exception object with error stack trace.
	 *
	 * @param message
	 * @param cause
	 */
	public PALServiceException(String message, Throwable cause) {
		super(message, cause);
		this.errorMessage = message;
	}

  /**
	 * Instantiates a new Document service exception.
	 * 
	 * @param errorCode
	 * @param errorMessage
	 */
	public PALServiceException(String errorCode, String errorMessage) {
		super(errorMessage);
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
	}

	/**
	 * Gets error code.
	 *
	 * @return the error code
	 */
	public String getErrorCode() {
		return errorCode;
	}

	/**
	 * Sets error code.
	 *
	 * @param errorCode the error code
	 */
	public void setErrorCode(String errorCode) {
		this.errorCode = errorCode;
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
	 * Sets error message.
	 *
	 * @param errorMessage the error message
	 */
	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	/**
	 * Gets status code.
	 *
	 * @return the status code
	 */
	public Integer getStatusCode() {
		return statusCode;
	}

	/**
	 * Sets status code.
	 *
	 * @param statusCode the status code
	 */
	public void setStatusCode(Integer statusCode) {
		this.statusCode = statusCode;
	}

}
