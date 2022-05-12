package com.marksandspencer.foodshub.pal.stepDefinitions.common;

import com.marksandspencer.foodshub.pal.stepDefinitions.ITBaseStepDefinition;
import io.cucumber.java.en.Given;
import org.junit.jupiter.api.Assertions;

public class ScenarioContextValidationStep extends ITBaseStepDefinition {

    @Given("Scenario context has empty response")
    public void test_context_has_empty_response() {
        Assertions.assertNull(scenarioContext().getResponse());
    }
}