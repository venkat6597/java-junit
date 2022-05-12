package com.marksandspencer.foodshub.pal.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Auditlog {
    private LocalDateTime createdOn;
    private String createdTime;
    private String createdBy;
    private String field;
    private String oldValue;
    private String newValue;
    private String role;
}
