package com.marksandspencer.foodshub.pal.transfer;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Data
@Builder
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Progress {
    private String intoDepotDate;
    private String intoStoreDate;
    private Integer totalFields;
    private Integer completedFields;
    private List<RoleProgress> roles;
}
