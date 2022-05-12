package com.marksandspencer.foodshub.pal.transfer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * the class ESProductPacksDataRequest
 *
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ESSupplierDataRequest {

    /**
     * the supplierIds
     */
    List<String> supplierIds;
}
