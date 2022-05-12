package com.marksandspencer.foodshub.pal.transfer;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthAttribute {

	private int atributeId;

	private String attributeCode;

	private String attributeName;

	private String attributeType;

	private String description;
	
	private Timestamp createdOn;
	
	private Timestamp updatedOn;
}
