package com.marksandspencer.foodshub.pal.transfer;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonInclude(Include.NON_NULL)
public class FieldUpdate {
	private String section;
	private String role;
	private String field;
	private String oldValue;
	private String newValue;
	private String subSectionId;
	private List<FieldUpdate> subSectionFields;
}
