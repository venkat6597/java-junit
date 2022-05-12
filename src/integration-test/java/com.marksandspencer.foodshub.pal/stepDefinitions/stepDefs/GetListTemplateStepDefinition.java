package com.marksandspencer.foodshub.pal.stepDefinitions.stepDefs;
import com.marksandspencer.foodshub.pal.context.ScenarioContext;
import com.marksandspencer.foodshub.pal.stepDefinitions.ITBaseStepDefinition;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.junit.Assert;
import java.util.List;
import java.util.Map;
import static com.marksandspencer.foodshub.pal.util.QueryBuilder.QUERY_BUILDER;

public class GetListTemplateStepDefinition extends ITBaseStepDefinition {
    @Given("Get service call for users list templates endpoint {string}")
    public void get_service_call_for_users_list_templates_endpoint(String uri) {
        String url =
                QUERY_BUILDER.newQuery().baseUrl(getResourceUrl(uri))
                        .build();
        scenarioContext().set(ScenarioContext.REQUEST, null);
        Response response = getRequest().get(url);
        scenarioContext().setResponse(response);
        response.then().log().body();
    }
    @Then("cross verify with the response if the id is {string} and name is {string}")
    public void cross_verify_with_the_response_if_the_id_is_and_name_is(String id, String name) {
        JsonPath jsonPathEvaluator = scenarioContext().getResponse().jsonPath();
        List<Map<String,String>> listTemplates=jsonPathEvaluator.get("data.templates");
        for(int index =0;index< listTemplates.size();index++)
        {
            Assert.assertEquals(id,listTemplates.get(index).get("id"));
            Assert.assertEquals(name,listTemplates.get(index).get("name"));
        }
    }
}
