package com.marksandspencer.foodshub.pal.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class Category {

    private String value;
    private String level;
    private String description;
    private CategoryChildren children;

}
