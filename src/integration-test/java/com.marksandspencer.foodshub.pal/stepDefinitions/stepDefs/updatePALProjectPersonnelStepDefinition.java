package com.marksandspencer.foodshub.pal.stepDefinitions.stepDefs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.marksandspencer.foodshub.pal.context.ScenarioContext;
import com.marksandspencer.foodshub.pal.stepDefinitions.ITBaseStepDefinition;
import com.marksandspencer.foodshub.pal.transfer.AppResponse;
import com.marksandspencer.foodshub.pal.transfer.PALProjectPersonnelUpdateRequest;
import com.marksandspencer.foodshub.pal.transfer.PALProjectResponse;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.junit.Assert;

import java.util.Map;

import static com.marksandspencer.foodshub.pal.util.QueryBuilder.QUERY_BUILDER;


public class updatePALProjectPersonnelStepDefinition extends ITBaseStepDefinition {
    AppResponse<PALProjectResponse> updateProjectPersonnelResponse;
    @When("the service call with PUT request with {string} with payload with PageObject {string}, X-operation {string}, Origin {string}")
    public void the_service_call_with_put_request_with_with_payload(String uri,String PageObject,String operation, String origin,String payload) throws JsonProcessingException {
        String url =
                QUERY_BUILDER.newQuery().baseUrl(getResourceUrl(uri))
                        .build();
        ObjectMapper objectMapper = new ObjectMapper();
        PALProjectPersonnelUpdateRequest palProjectPersonnelUpdateRequest = objectMapper.readValue(payload, PALProjectPersonnelUpdateRequest.class);
        scenarioContext().set(ScenarioContext.REQUEST, null);
        //Response response = getRequest().contentType("application/json").body(palProjectPersonnelUpdateRequest).put(url);
        Response response = getRequestHeaders(PageObject,origin,operation).contentType("application/json").body(palProjectPersonnelUpdateRequest).put(url);
        Map<String, Object> updateprojectpersonnel = objectMapper.readValue(response.asString(), new TypeReference<Map<String, Object>>() {
        });
        objectMapper.registerModule(new JavaTimeModule());
        updateProjectPersonnelResponse = objectMapper.convertValue(updateprojectpersonnel, new TypeReference<AppResponse<PALProjectResponse>>() {
        });
        scenarioContext().setResponse(response);
        response.then().log().body();
    }
    @When("the service call with PUT request with {string} for invalid cases with payload with PageObject {string}, X-operation {string}, Origin {string}")
    public void the_service_call_with_put_request_with_for_invalid_cases_with_payload(String uri,String PageObject,String operation, String origin,String payload) throws JsonProcessingException{
        String url =
                QUERY_BUILDER.newQuery().baseUrl(getResourceUrl(uri))
                        .build();
        ObjectMapper objectMapper = new ObjectMapper();
        PALProjectPersonnelUpdateRequest palProjectPersonnelUpdateRequest = objectMapper.readValue(payload, PALProjectPersonnelUpdateRequest.class);
        scenarioContext().set(ScenarioContext.REQUEST, null);
        Response response = getRequestHeaders(PageObject,origin,operation).contentType("application/json").body(palProjectPersonnelUpdateRequest).put(url);
        scenarioContext().setResponse(response);
        response.then().log().body();
    }
    @Then("cross verify if the response contains {string}")
    public void cross_verify_if_the_response_contains(String projectId){
        Assert.assertEquals(projectId,updateProjectPersonnelResponse.getData().getId());

    }
    @Then("cross verify if the personnel is updated to {string}")
    public void cross_verify_if_the_personnel_is_updated_to(String newValue){

        Assert.assertEquals(newValue,updateProjectPersonnelResponse.getData().getPersonnel().getInternal().get(0).getUsers().get(0));

    }

}
