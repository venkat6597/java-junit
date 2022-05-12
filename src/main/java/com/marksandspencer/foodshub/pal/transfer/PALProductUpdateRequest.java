package com.marksandspencer.foodshub.pal.transfer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.*;

import java.util.List;

@Data
@Builder
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonInclude(Include.NON_NULL)
public class PALProductUpdateRequest {
	private String productId;
	private String user;
	private String userRole;
	private List<PersonnelUpdate> personnelUpdates;
	private List<FieldUpdate> fieldUpdates;	
}
