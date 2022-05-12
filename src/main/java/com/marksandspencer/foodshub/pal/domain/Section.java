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
public class Section {
	private String sectionName;
	private String sectionLabel;
	private List<PALFields> sectionFields;
	private List<SubSection> subSections;
}
