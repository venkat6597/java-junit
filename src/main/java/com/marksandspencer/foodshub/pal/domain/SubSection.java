package com.marksandspencer.foodshub.pal.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Data
@Builder
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubSection {
	private String subSectionName;
	private String subSectionLabel;
	private List<PALFields> subSectionFields;
}
