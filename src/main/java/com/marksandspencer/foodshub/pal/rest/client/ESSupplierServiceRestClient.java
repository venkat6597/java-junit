package com.marksandspencer.foodshub.pal.rest.client;

import com.marksandspencer.foodshub.pal.dto.Suppliers;
import com.marksandspencer.foodshub.pal.transfer.ESSupplierDataRequest;

import java.util.concurrent.CompletableFuture;

public interface ESSupplierServiceRestClient {

    CompletableFuture<Suppliers> getSuppliers(ESSupplierDataRequest esSupplierDataRequest) ;

}
