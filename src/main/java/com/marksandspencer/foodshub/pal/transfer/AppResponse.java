package com.marksandspencer.foodshub.pal.transfer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * The type App response.
 *
 * @param <T> the type parameter
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class AppResponse<T> {
	private T data;
	private ErrorInfo exception;
	private Boolean status;

	/**
	 * Instantiates a new App response.
	 *
	 * @param data   the data
	 * @param status the status
	 */
	public AppResponse(T data,Boolean status) {
		this.data = data;
		this.status=status;
	}
}
