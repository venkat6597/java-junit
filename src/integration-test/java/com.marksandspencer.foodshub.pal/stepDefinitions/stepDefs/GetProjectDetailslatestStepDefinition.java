package com.marksandspencer.foodshub.pal.stepDefinitions.stepDefs;

import com.marksandspencer.foodshub.pal.stepDefinitions.ITBaseStepDefinition;
import com.marksandspencer.foodshub.pal.stepDefinitions.common.SchemaValidationStep;
import io.cucumber.java.en.Then;

import org.junit.Assert;

public class GetProjectDetailslatestStepDefinition extends ITBaseStepDefinition {

    @Then("assert the response for getProjectDetails")
    public void assert_the_response_for_get_project_details() {
        Assert.assertEquals(SchemaValidationStep.palProjectRequest.getProjectId(), SchemaValidationStep.palProjectResponseAppResponse.getData().getId());
    }
}
