package com.marksandspencer.foodshub.pal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * The type Supplier.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class Supplier {

    private String supplierId;
    private String name;
    private String legacyCode;
    private String supplierType;
    private String supplierSubType;
    private Boolean live;

}
