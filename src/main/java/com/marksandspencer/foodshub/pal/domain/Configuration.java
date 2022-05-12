package com.marksandspencer.foodshub.pal.domain;

import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@Data
@Builder
@RequiredArgsConstructor
@EqualsAndHashCode
@AllArgsConstructor
@Document(collection = "configuration")
public class Configuration {

	@Id
	private String id;
	private String type;
	private String description;
	private List<Map<String,Object>> values;
}
