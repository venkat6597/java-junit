package com.marksandspencer.foodshub.pal.transfer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonInclude(Include.NON_NULL)
public class Information {
    private String id;
    private String projectName;
    private String templateId;
    private String templateName;
    private String status;
    private String projectType;
    private String financialYear;
    private LocalDateTime projectCompletionDate;
    private String comments;
}
