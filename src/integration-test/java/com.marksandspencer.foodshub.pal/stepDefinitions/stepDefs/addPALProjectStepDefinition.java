package com.marksandspencer.foodshub.pal.stepDefinitions.stepDefs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.marksandspencer.foodshub.pal.context.ScenarioContext;
import com.marksandspencer.foodshub.pal.stepDefinitions.ITBaseStepDefinition;
import com.marksandspencer.foodshub.pal.transfer.AppResponse;
import com.marksandspencer.foodshub.pal.transfer.PALProjectResponse;
import com.marksandspencer.foodshub.pal.transfer.PALProjectUpdateRequest;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.junit.Assert;
import com.fasterxml.jackson.databind.DeserializationFeature;
import java.util.Map;

import static com.marksandspencer.foodshub.pal.util.QueryBuilder.QUERY_BUILDER;

@Slf4j
public class addPALProjectStepDefinition extends ITBaseStepDefinition {
    AppResponse<PALProjectResponse> palprojectResponse;
    PALProjectUpdateRequest palProjectUpdateRequest;
    Map<String, Object> project;

    @When("the service call for PAL Project  with POST request with {string} with payload with PageObject {string}, X-operation {string}, Origin {string}")
    public void the_service_call_for_pal_project_with_post_request_with_with_payload(String uri,String PageObject,String operation, String origin,String payload) throws JsonProcessingException {
        String url =
                QUERY_BUILDER.newQuery().baseUrl(getResourceUrl(uri))
                        .build();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        palProjectUpdateRequest = objectMapper.readValue(payload, PALProjectUpdateRequest.class);
        ObjectMapper mapper = new ObjectMapper();
        System.out.println("palProjectUpdateRequestprinted" +mapper.writerWithDefaultPrettyPrinter().writeValueAsString(palProjectUpdateRequest));
        //System.out.println("palProjectUpdateRequestprinted" + palProjectUpdateRequest);
        scenarioContext().set(ScenarioContext.REQUEST, null);
        Response response = getRequestHeaders(PageObject,origin,operation).contentType("application/json").body(palProjectUpdateRequest).post(url);
        project = objectMapper.readValue(response.asString(), new TypeReference<Map<String, Object>>() {
        });
        palprojectResponse = objectMapper.convertValue(project, new TypeReference<AppResponse<PALProjectResponse>>() {
        });
        scenarioContext().setResponse(response);
        response.then().log().body();
    }

    @Then("assert the response for valid values")
    public void assert_the_response_for_valid_values() {
        if (ObjectUtils.isNotEmpty(palprojectResponse.getData())) {
            Assert.assertEquals(palProjectUpdateRequest.getInformation().getProjectName(), palprojectResponse.getData().getInformation().getProjectName());
            log.debug("Expected: "+palProjectUpdateRequest.getInformation().getProjectName()+"Actual: " +palprojectResponse.getData().getInformation().getProjectName());
            Assert.assertEquals(palProjectUpdateRequest.getInformation().getProjectType(), palprojectResponse.getData().getInformation().getProjectType());
            Assert.assertEquals("Draft", palprojectResponse.getData().getInformation().getStatus());
            log.debug("Expected: "+palProjectUpdateRequest.getInformation().getProjectType()+"Actual: " + palprojectResponse.getData().getInformation().getProjectType());
            Assert.assertEquals(palProjectUpdateRequest.getInformation().getFinancialYear(), palprojectResponse.getData().getInformation().getFinancialYear());
            log.debug("Expected: "+palProjectUpdateRequest.getInformation().getFinancialYear()+"Actual: " + palprojectResponse.getData().getInformation().getFinancialYear());
            if (palprojectResponse.getData().getInformation().getProjectType().equals("Seasonal"))
                Assert.assertEquals(palProjectUpdateRequest.getInformation().getProjectCompletionDate(), palprojectResponse.getData().getInformation().getProjectCompletionDate());
            if(palProjectUpdateRequest.getInformation().getComments()!=null)
                Assert.assertEquals(palProjectUpdateRequest.getInformation().getComments(), palprojectResponse.getData().getInformation().getComments());
            for(int i=0;i<palProjectUpdateRequest.getPersonnel().getInternal().size();i++)
                Assert.assertEquals(palProjectUpdateRequest.getPersonnel().getInternal().get(i).getUsers(), palprojectResponse.getData().getPersonnel().getInternal().get(i).getUsers());
        }
        else{
            Assert.fail("Response did not match");
        }
    }

    @When("the service call for PAL Project  with POST request with {string} for negative scenarios with payload with PageObject {string}, X-operation {string}, Origin {string}")
    public void the_service_call_for_pal_project_with_post_request_with_for_negative_scenarios_with_payload(String uri,String PageObject,String operation, String origin,String payload) throws JsonProcessingException {
        String url =
                QUERY_BUILDER.newQuery().baseUrl(getResourceUrl(uri))
                        .build();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        palProjectUpdateRequest = objectMapper.readValue(payload, PALProjectUpdateRequest.class);
        System.out.println("palProjectUpdateRequestprinted" + palProjectUpdateRequest.getInformation().getProjectCompletionDate());
        scenarioContext().set(ScenarioContext.REQUEST, null);
        Response response = getRequestHeaders(PageObject,origin,operation).contentType("application/json").body(palProjectUpdateRequest).post(url);
        scenarioContext().setResponse(response);
        response.then().log().body();
    }
}