package com.marksandspencer.foodshub.pal.domain;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@Document(collection = "PALFields")
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PALFields {
    @Id
    private String id;
    private String label;
    private String type;
    private String format;
    private boolean creativeGate;
    private boolean disabled;
    private String owner;
    private boolean mandatory;
    private String pattern;
    private String tooltip;
    private String placeholder;
    private Map<String,String> errorMessages;
    private Integer minDate;
    @DBRef
    private PALConfiguration optionsRef;
    private List<String> scripts;
    private List<String> readable;
    private List<String> editable;
    private List<String> description;
}
