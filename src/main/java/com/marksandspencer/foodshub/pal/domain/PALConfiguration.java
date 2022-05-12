package com.marksandspencer.foodshub.pal.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@Document(collection = "PALConfiguration")
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PALConfiguration {
    @Id
    private String id;
    
    private List<String> values;

    private List<Map<String, Object>> mapValues;

}
