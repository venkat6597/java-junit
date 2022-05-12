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
public class Header {
	private String productId;
	private String projectName;
	private String productName;
	private String status;
	private String productType;
	private String upc;
	private String supplierName;
	private String supplierCode;
	private String weight;
	private String category;
	private String productFileType;
	private Integer percentage;
	private List<ChildProduct> childProducts;
	private String subRange;
}
