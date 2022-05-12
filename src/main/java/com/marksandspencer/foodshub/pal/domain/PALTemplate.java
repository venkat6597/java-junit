package com.marksandspencer.foodshub.pal.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@NoArgsConstructor
@Document(collection = "PALTemplate")
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PALTemplate {
    @Id
    private String id;
    private String templateName;
    private List<Section> sections; 
}
