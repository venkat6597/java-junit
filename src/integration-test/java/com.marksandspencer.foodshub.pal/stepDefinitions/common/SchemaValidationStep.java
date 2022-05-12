package com.marksandspencer.foodshub.pal.stepDefinitions.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.marksandspencer.foodshub.pal.context.ScenarioContext;
import com.marksandspencer.foodshub.pal.exception.PALServiceException;
import com.marksandspencer.foodshub.pal.stepDefinitions.ITBaseStepDefinition;
import com.marksandspencer.foodshub.pal.transfer.*;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import java.util.Map;

import static com.marksandspencer.foodshub.pal.util.QueryBuilder.QUERY_BUILDER;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;

@Slf4j
public class SchemaValidationStep extends ITBaseStepDefinition {
    public static final String QUERY_SCHEMA = "QUERY_SCHEMA";
    public static final String QUERY_SCHEMA_PATH = "it.schema.query.path";

    @Autowired
    Environment environment;

    @Then("response must match with json schema {string}")
    public void response_must_match_with_json_schema(String schemaFor) {
        String path = getPathForSchema(schemaFor);
        log.info("Path for"+path);
        scenarioContext().getResponse().then().body(matchesJsonSchemaInClasspath(path));
    }

    @Then("cross verify with the response if the error message is {string}.")
    public void crossVerifyWithTheResponseIfTheErrorMessageIs(String errorMessage) {
        JsonPath jsonPathEvaluator = scenarioContext().getResponse().jsonPath();
        Assert.assertEquals(errorMessage, jsonPathEvaluator.get("exception.errorMessage"));
    }


     private String getPathForSchema(String schemaFor) {
        String path;
        switch (schemaFor) {
            case QUERY_SCHEMA:
                path = environment.getProperty(QUERY_SCHEMA_PATH );
                break;
            default:
                throw new PALServiceException("UNKNOWN Schema definition requested");
        }
        return path;
    }
    public static AppResponse<PALProjectResponse> palProjectResponseAppResponse;
    public static PALProjectRequest palProjectRequest;

    @When("the service call with POST request to fetch PAL project response with {string} with PageObject {string}, X-operation {string}, Origin {string} and payload")
    public void the_service_call_with_post_request_to_fetch_pal_project_response_with_with_page_object_x_operation_origin_and_payload(String uri, String PageObject, String operation, String origin, String payload) throws JsonProcessingException {
        String url =
                QUERY_BUILDER.newQuery().baseUrl(getResourceUrl(uri))
                        .build();
        ObjectMapper objectMapper = new ObjectMapper();
        palProjectRequest = objectMapper.readValue(payload, PALProjectRequest.class);
        scenarioContext().set(ScenarioContext.REQUEST, null);
        Response response = getRequestHeaders(PageObject,origin,operation).contentType("application/json").body(palProjectRequest).post(url);
        Map<String, Object> palProjectResponse = objectMapper.readValue(response.asString(), new TypeReference<>() {
        });
        objectMapper.registerModule(new JavaTimeModule());
        palProjectResponseAppResponse = objectMapper.convertValue(palProjectResponse, new TypeReference<>() {
        });
        scenarioContext().setResponse(response);
        response.then().log().body();
    }

    @When("the service call with POST request to fetch PAL project response with {string} for negative scenario with PageObject {string}, X-operation {string}, Origin {string} and payload")
    public void the_service_call_with_post_request_to_fetch_pal_project_response_with_for_negative_scenario_with_page_object_x_operation_origin_and_payload(String uri, String PageObject, String operation, String origin, String payload)throws JsonProcessingException {

        String url =
                QUERY_BUILDER.newQuery().baseUrl(getResourceUrl(uri))
                        .build();
        ObjectMapper objectMapper = new ObjectMapper();
        PALProjectRequest palProjectRequest = objectMapper.readValue(payload, PALProjectRequest.class);
        scenarioContext().set(ScenarioContext.REQUEST, null);
        Response response = getRequestHeaders(PageObject,origin,operation).contentType("application/json").body(palProjectRequest).post(url);
        scenarioContext().setResponse(response);
        response.then().log().body();
    }


    public static AppResponse<PALProductResponse> palProductResponseAppResponse;
    public static PALProductRequest palProductRequest;

    @When("the service call with POST request to fetch the product response {string} with PageObject {string}, X-operation {string}, Origin {string} and payload")
    public void the_service_call_with_post_request_to_fetch_the_product_response_with_page_object_x_operation_origin_and_payload(String uri, String PageObject, String operation, String origin, String payload) throws JsonProcessingException {

        String url =
                QUERY_BUILDER.newQuery().baseUrl(getResourceUrl(uri))
                        .build();
        ObjectMapper objectMapper = new ObjectMapper();
        palProductRequest = objectMapper.readValue(payload, PALProductRequest.class);
        scenarioContext().set(ScenarioContext.REQUEST, null);
        Response response = getRequestHeaders(PageObject, origin, operation).contentType("application/json").body(palProductRequest).post(url);
        Map<String, Object> palProductResponse = objectMapper.readValue(response.asString(), new TypeReference<>() {
        });
        objectMapper.registerModule(new JavaTimeModule());
        palProductResponseAppResponse = objectMapper.convertValue(palProductResponse, new TypeReference<>() {
        });
        scenarioContext().setResponse(response);
        response.then().log().body();
    }

    @When("the service call with POST request to fetch the product response {string} for negative scenario with PageObject {string}, X-operation {string}, Origin {string} and payload")
    public void the_service_call_with_post_request_to_fetch_the_product_response_for_negative_scenario_with_page_object_x_operation_origin_and_payload(String uri, String PageObject, String operation, String origin, String payload) throws JsonProcessingException {
        String url =
                QUERY_BUILDER.newQuery().baseUrl(getResourceUrl(uri))
                        .build();
        ObjectMapper objectMapper = new ObjectMapper();
        PALProductRequest palProductRequest = objectMapper.readValue(payload, PALProductRequest.class);
        scenarioContext().set(ScenarioContext.REQUEST, null);
        Response response = getRequestHeaders(PageObject, origin, operation).contentType("application/json").body(palProductRequest).post(url);
        scenarioContext().setResponse(response);
        response.then().log().body();
    }
}