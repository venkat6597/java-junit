package com.marksandspencer.foodshub.pal.transfer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthzSupplierResponse {

	String supplierId;
	String attributeId;
	String supplierName;
	
}
