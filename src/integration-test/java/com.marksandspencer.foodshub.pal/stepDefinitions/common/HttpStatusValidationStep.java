package com.marksandspencer.foodshub.pal.stepDefinitions.common;

import com.marksandspencer.foodshub.pal.stepDefinitions.ITBaseStepDefinition;
import io.cucumber.java.en.Then;

import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

public class HttpStatusValidationStep extends ITBaseStepDefinition {

    @Then("service should return {int} status code")
    public void service_should_return_status_code(Integer statusCode) {
        scenarioContext().getResponse().then().statusCode(statusCode.intValue());
    }

    @Then("response must match with json schema {string}.")
    public void responseMustMatchWithJsonSchema(String schemaFor) {
        scenarioContext().getResponse().then().body(matchesJsonSchemaInClasspath(schemaFor));
    }
}