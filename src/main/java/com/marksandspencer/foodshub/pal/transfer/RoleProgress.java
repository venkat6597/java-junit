package com.marksandspencer.foodshub.pal.transfer;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data
@Builder
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleProgress {
    private String role;
    private String status;
    private Integer totalFields;
    private Integer completedFields;
    private Integer percentageCompletion;
    private String assignedUser;
}
