package com.marksandspencer.foodshub.pal.stepDefinitions.stepDefs;


import com.marksandspencer.foodshub.pal.stepDefinitions.ITBaseStepDefinition;
import com.marksandspencer.foodshub.pal.transfer.AppResponse;
import com.marksandspencer.foodshub.pal.transfer.PALProductResponse;
import io.cucumber.java.en.Then;
import org.junit.Assert;



import static com.marksandspencer.foodshub.pal.stepDefinitions.common.SchemaValidationStep.palProductResponseAppResponse;



public class GetProductPersonnelStepDefinition extends ITBaseStepDefinition {
    AppResponse<PALProductResponse> GetProductPersonnelResponse=palProductResponseAppResponse;

    @Then("cross verify if the response contains {string},{string}")
    public void cross_verify_if_the_response_contains(String productId,String role1)  {
        Assert.assertEquals(productId, GetProductPersonnelResponse.getData().getId());
        Assert.assertEquals(role1, GetProductPersonnelResponse.getData().getPersonnel().getInternal().get(0).getRole());
    }



}