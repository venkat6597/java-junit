package com.marksandspencer.foodshub.pal.transfer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.marksandspencer.foodshub.pal.dto.Auditlog;
import lombok.*;

import java.util.List;

@Data
@Builder
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonInclude(Include.NON_NULL)
public class PALProductResponse implements Comparable<PALProductResponse> {
	private String id;
	private String templateName;
	private String templateId;
    private Personnel personnel;
	private Header header;
	private Progress progress;
	private List<Auditlog> auditlogs;
	private List<ProductSection> sections;

	/**
	 * sort on subrange
	 * @param object pal product response
	 * @return boolean for the compared values
	 */
	@Override
	public int compareTo(PALProductResponse object) {

		if (object.getHeader().getSubRange() == null || getHeader().getSubRange() == null) {
			return -1;
		}
		return getHeader().getSubRange().compareTo(object.getHeader().getSubRange());
	}
}
