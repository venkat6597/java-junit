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
public class ProductUpdate {
    private String productId;
    private List<FieldUpdate> fieldUpdates;
}
