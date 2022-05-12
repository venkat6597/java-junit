package com.marksandspencer.foodshub.pal.stepDefinitions.stepDefs;

import com.marksandspencer.foodshub.pal.stepDefinitions.ITBaseStepDefinition;

import com.marksandspencer.foodshub.pal.stepDefinitions.common.SchemaValidationStep;
import io.cucumber.java.en.Then;

import org.junit.Assert;

public class getPALProjectProgressStepDefinition extends ITBaseStepDefinition {

    @Then("assert the PALProjectProgress response")
    public void assert_the_pal_project_progress_response() {
        Assert.assertEquals(SchemaValidationStep.palProjectRequest.getProjectId(), SchemaValidationStep.palProjectResponseAppResponse.getData().getId());
        SchemaValidationStep.palProjectResponseAppResponse.getData().getProgress().getRoles().forEach(Role -> {
            Assert.assertNotNull(Role.getRole());
            Assert.assertNotNull(Role.getStatus());
            Assert.assertNotNull(Role.getTotalFields());
            Assert.assertNotNull(Role.getCompletedFields());
        });
    }
}
