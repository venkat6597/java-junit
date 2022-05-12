package com.marksandspencer.foodshub.pal.transfer;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * The type Error info.
 */
@Data
@Builder
@EqualsAndHashCode
@AllArgsConstructor
public class ErrorInfo {
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy HH:mm:ss")
	private LocalDateTime timestamp;
	private String errorCode;
	private String errorMessage;
}