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
public class PALProjectResponse {
	private String id;
	private String templateName;
	private String templateId;
    private Personnel personnel;
	private Progress progress;
	private List<Product> products;
	private Information information;
}
