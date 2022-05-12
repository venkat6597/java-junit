package com.marksandspencer.foodshub.pal.stepDefinitions.stepDefs;

import com.marksandspencer.foodshub.pal.context.ScenarioContext;
import com.marksandspencer.foodshub.pal.stepDefinitions.ITBaseStepDefinition;

import io.cucumber.java.en.When;
import io.restassured.response.Response;

import static com.marksandspencer.foodshub.pal.constants.ITConstants.LISTUSERBYROLE;
import static com.marksandspencer.foodshub.pal.util.QueryBuilder.QUERY_BUILDER;

public class getPALListUserByRoleStepDefinition extends ITBaseStepDefinition {
    @When("the service call with GET request is made to url {string} with {string} as queryParam")
    public void the_service_call_with_get_request_is_made_to_url_with_as_queryParam(String url, String role) {
        String requestUrl =
                QUERY_BUILDER.newQuery().baseUrl(getResourceUrl(url))
                        .queryParam("role", role)
                        .build();
        scenarioContext().set(ScenarioContext.REQUEST, null);
        scenarioContext().set(LISTUSERBYROLE,role);
        Response response = getRequest().get(requestUrl);
        scenarioContext().setResponse(response);
        response.then().log().body();
    }
}