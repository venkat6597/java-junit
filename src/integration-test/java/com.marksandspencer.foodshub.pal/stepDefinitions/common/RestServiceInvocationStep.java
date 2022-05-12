package com.marksandspencer.foodshub.pal.stepDefinitions.common;

import com.marksandspencer.foodshub.pal.stepDefinitions.ITBaseStepDefinition;
import io.cucumber.java.en.Given;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

public class RestServiceInvocationStep extends ITBaseStepDefinition {

    @Given("GET service call for {string} endpoint")
    public void get_service_call_for_endpoint(String url) {
        Response response = getRequest().get(getResourceUrl(url));
        scenarioContext().setResponse(response);
    }

}