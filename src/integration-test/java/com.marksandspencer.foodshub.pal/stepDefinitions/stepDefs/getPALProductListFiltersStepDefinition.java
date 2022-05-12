package com.marksandspencer.foodshub.pal.stepDefinitions.stepDefs;

import com.marksandspencer.foodshub.pal.stepDefinitions.ITBaseStepDefinition;
import com.marksandspencer.foodshub.pal.stepDefinitions.common.SchemaValidationStep;
import com.marksandspencer.foodshub.pal.transfer.*;
import io.cucumber.java.en.Then;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.junit.Assert;

@Slf4j
public class getPALProductListFiltersStepDefinition extends ITBaseStepDefinition {
    AppResponse<PALProjectResponse> palProjectResponseAppResponse = SchemaValidationStep.palProjectResponseAppResponse;
    PALProjectRequest palProjectRequest = SchemaValidationStep.palProjectRequest;

    @Then("assert the response")
    public void assert_the_response() {
        if (ObjectUtils.isNotEmpty(palProjectResponseAppResponse.getData().getProducts())) {
            palProjectResponseAppResponse.getData().getProducts().forEach(product -> {
                Header data = product.getData();
                if (palProjectRequest.getFilter().getSearchText() != null) {
                    if (palProjectRequest.getFilter().getSearchText().contains(data.getProductName())) {
                        log.debug("Expected: " + palProjectRequest.getFilter().getSearchText() + " Actual: " + data.getProductName());
                        Assert.assertEquals(palProjectRequest.getFilter().getSearchText(), data.getProductName());
                    } else if (palProjectRequest.getFilter().getSearchText().contains(data.getSupplierName())) {
                        Assert.assertEquals(palProjectRequest.getFilter().getSearchText(), data.getSupplierName());
                    }

                }
                if (palProjectRequest.getFilter().getStatus() != null) {
                    if (!palProjectRequest.getFilter().getStatus().contains(data.getStatus())) {
                        Assert.fail("Status did not match");
                    } else {
                        palProjectRequest.getFilter().getStatus().forEach(status -> {
                            if (status.equals(data.getStatus())) {
                                Assert.assertEquals(status, data.getStatus());
                                log.debug("Expected : " + status + " Actual: " + data.getStatus());
                            }
                        });
                    }
                }
                if (palProjectRequest.getFilter().getSuppliers() != null) {
                    if (!palProjectRequest.getFilter().getSuppliers().contains(data.getSupplierCode())) {
                        Assert.fail("Supplier did not match");
                    } else {
                        palProjectRequest.getFilter().getSuppliers().forEach(suppliers -> {
                            if (suppliers.equals(data.getSupplierCode())) {
                                Assert.assertEquals(suppliers, data.getSupplierCode());
                                log.debug("Expected : " + suppliers + " Actual: " + data.getSupplierCode());
                            }
                        });
                    }
                }
                if (palProjectRequest.getFilter().getProgressRange() != null) {
                    palProjectRequest.getFilter().getProgressRange().forEach(percentage -> {
                        if (!percentage.contains("-")) {
                            Assert.assertTrue(data.getPercentage() == Integer.valueOf(percentage));
                        } else if (percentage.contains("-")) {
                            String[] progress = percentage.split("-");
                            if (data.getPercentage() >= Integer.valueOf(progress[0]) && data.getPercentage() <= Integer.valueOf(progress[1])) {
                                Assert.assertTrue(data.getPercentage() >= Integer.valueOf(progress[0]) && data.getPercentage() <= Integer.valueOf(progress[1]));
                            }
                        }


                    });
                }
                if (palProjectRequest.getFilter().getType() != null) {
                    if (!palProjectRequest.getFilter().getType().contains(data.getProductType())) {
                        Assert.fail("Product Type did not match");
                    } else {
                        palProjectRequest.getFilter().getType().forEach(type -> {
                            if (type.equals(data.getProductType())) {
                                Assert.assertEquals(type, data.getProductType());
                                log.debug("Expected : " + type + " Actual: " + data.getProductType());
                            }
                        });
                    }
                }

            });
        }
    }
}




