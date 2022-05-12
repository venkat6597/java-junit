package com.marksandspencer.foodshub.pal.transfer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@Builder
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonInclude(Include.NON_NULL)
public class ProductField {
	private String label;
	private String name;
	private String tooltip;
	private String placeholder;
	private String type;
	private List<String> options;
	private String value;
	private String owner;
	private boolean disabled;
	private boolean creativeGate;
	private boolean mandatory;
	private String pattern;
	private Map<String,String> errorMessages;
	private Integer minDate;
	private List<ProductSubSection> subSections;
}
