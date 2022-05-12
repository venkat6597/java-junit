package com.marksandspencer.foodshub.pal.stepDefinitions.stepDefs;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marksandspencer.foodshub.pal.context.ScenarioContext;
import com.marksandspencer.foodshub.pal.stepDefinitions.ITBaseStepDefinition;
import com.marksandspencer.foodshub.pal.transfer.PALProjectUpdateRequest;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.Assert;
import static com.marksandspencer.foodshub.pal.util.QueryBuilder.QUERY_BUILDER;

public class updatePALProjectStepDefinition extends ITBaseStepDefinition {

    @When("the service call with PUT request to update project service {string} with PageObject {string}, X-operation {string}, Origin {string} payload")
    public void the_service_call_with_put_request_to_update_project_service_with_page_object_x_operation_origin_payload(String uri, String PageObject, String operation, String origin, String payload) throws JsonProcessingException {
        String url =
                QUERY_BUILDER.newQuery().baseUrl(getResourceUrl(uri))
                        .build();
        ObjectMapper objectMapper = new ObjectMapper();
        PALProjectUpdateRequest palProjectUpdateRequest = objectMapper.readValue(payload, PALProjectUpdateRequest.class);
        scenarioContext().set(ScenarioContext.REQUEST, null);
        Response response = getRequestHeaders(PageObject,origin,operation).contentType("application/json").body(palProjectUpdateRequest).put(url);
        scenarioContext().setResponse(response);
        response.then().log().body();

    }
    @Then("cross verify with the response if the updated field is {string} {string}.")
    public void crossVerifyWithTheResponseIfTheUpdatedFieldIs(String fieldName, String updatedField) {
        JsonPath jsonPathEvaluator = scenarioContext().getResponse().jsonPath();
        Assert.assertEquals(updatedField, jsonPathEvaluator.get("data.information."+fieldName));

    }
}



