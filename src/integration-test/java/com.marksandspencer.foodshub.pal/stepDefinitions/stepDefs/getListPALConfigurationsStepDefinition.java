package com.marksandspencer.foodshub.pal.stepDefinitions.stepDefs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.marksandspencer.foodshub.pal.context.ScenarioContext;
import com.marksandspencer.foodshub.pal.domain.PALConfiguration;
import com.marksandspencer.foodshub.pal.stepDefinitions.ITBaseStepDefinition;
import com.marksandspencer.foodshub.pal.transfer.AppResponse;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.marksandspencer.foodshub.pal.util.QueryBuilder.QUERY_BUILDER;

public class getListPALConfigurationsStepDefinition extends ITBaseStepDefinition {
    AppResponse<List<PALConfiguration>> palConfiguration;
    List<String> configurationIds;
    Map<String, Object> listpalConfigurations;
    @When("service call with POST Request for PAL Configurations endpoint with {string} for following payload")
    public void service_call_with_post_request_for_pal_configurations_endpoint_with_for_following_payload(String uri, String payload) throws JsonProcessingException {
        String url =
                QUERY_BUILDER.newQuery().baseUrl(getResourceUrl(uri))
                        .build();
        ObjectMapper objectMapper = new ObjectMapper();
        configurationIds = objectMapper.readValue(payload,ArrayList.class);
        scenarioContext().set(ScenarioContext.REQUEST, null);
        Response response= getRequest().contentType("application/json").body(configurationIds).post(url);
       objectMapper.registerModule(new JavaTimeModule());
      listpalConfigurations = objectMapper.readValue(response.asString(), new TypeReference<Map<String, Object>>() {
        });
      palConfiguration =  objectMapper.convertValue(listpalConfigurations, new TypeReference<AppResponse<List<PALConfiguration>>>() {
        });
        scenarioContext().setResponse(response);
        response.then().log().body();
    }

    @When("service call with POST Request for PAL Configurations endpoint with {string} for following payload for invalid configurations")
    public void service_call_with_post_request_for_pal_configurations_endpoint_with_for_following_payload_for_invalid_configurations(String uri, String payload) throws JsonProcessingException {
        String url =
                QUERY_BUILDER.newQuery().baseUrl(getResourceUrl(uri))
                        .build();
        ObjectMapper objectMapper = new ObjectMapper();
        configurationIds = objectMapper.readValue(payload,ArrayList.class);
        scenarioContext().set(ScenarioContext.REQUEST, null);
        Response response = getRequest().contentType("application/json").body(configurationIds).post(url);
        scenarioContext().setResponse(response);
        response.then().log().body();
    }

    @Then("Assert the response for post configuration")
    public void response_must_contain_for_post_configuration() {
        List<String> response = new ArrayList<>();
        System.out.println("responseids "+response);
        palConfiguration.getData().forEach(configurationId -> response.add(configurationId.getId()));
        Collections.sort(configurationIds);
        Collections.sort(response);
        Assert.assertEquals(configurationIds,response);
        System.out.println("(configurationIds "+configurationIds+" "+response);
    }
    @Then("assert the response for invalid configurationid")
    public void assert_the_response_for_invalid_configurationid() {
        JsonPath jsonPathEvaluator = scenarioContext().getResponse().jsonPath();
        List<String> response =jsonPathEvaluator.get("data");
        Assert.assertEquals(0, response.size());
    }
}
