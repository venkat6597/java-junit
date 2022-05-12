package com.marksandspencer.foodshub.pal.stepDefinitions.stepDefs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.marksandspencer.foodshub.pal.context.ScenarioContext;
import com.marksandspencer.foodshub.pal.stepDefinitions.ITBaseStepDefinition;
import com.marksandspencer.foodshub.pal.transfer.AppResponse;
import com.marksandspencer.foodshub.pal.transfer.PALProjectResponse;
import com.marksandspencer.foodshub.pal.transfer.ProjectFilter;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static com.marksandspencer.foodshub.pal.constants.ITConstants.DATEMONTHFORMAT;
import static com.marksandspencer.foodshub.pal.util.QueryBuilder.QUERY_BUILDER;

public class GetPALProjectListStepDefinition extends ITBaseStepDefinition {
    AppResponse<List<PALProjectResponse>> projectListResponse;
    @When("the service call for PAL Project List with POST request with {string} with payload  with PageObject {string}, X-operation {string}, Origin {string}")
    public void the_service_call_for_pal_project_list_with_post_request_with_with_payload(String uri,String PageObject,String operation, String origin,String payload) throws JsonProcessingException {
        String url =
                QUERY_BUILDER.newQuery().baseUrl(getResourceUrl(uri))
                        .build();
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);
        ProjectFilter projectFilter = objectMapper.readValue(payload, ProjectFilter.class);
        scenarioContext().set(ScenarioContext.REQUEST, null);
        Response response = getRequestHeaders(PageObject,origin,operation).contentType("application/json").body(projectFilter).post(url);
        System.out.println(response.asString());
        Map<String, Object> projectList = objectMapper.readValue(response.asString(), new TypeReference<Map<String, Object>>() {
        });
        objectMapper.registerModule(new JavaTimeModule());
        projectListResponse = objectMapper.convertValue(projectList, new  TypeReference<AppResponse<List<PALProjectResponse>>>() {
        });
        scenarioContext().setResponse(response);
        response.then().log().body();
    }
    @When("the service call for PAL Project List with POST request with {string} with payload with filter for negative scenarios  with PageObject {string}, X-operation {string}, Origin {string}")
    public void the_service_call_for_pal_project_list_with_post_request_with_with_payload_with_filter_for_negative_scenarios(String uri,String PageObject,String operation, String origin,String payload) throws JsonProcessingException {
        String url =
                QUERY_BUILDER.newQuery().baseUrl(getResourceUrl(uri))
                        .build();
        ObjectMapper objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,false);;
        ProjectFilter projectFilter = objectMapper.readValue(payload, ProjectFilter.class);
        scenarioContext().set(ScenarioContext.REQUEST, null);
        Response response = getRequestHeaders(PageObject,origin,operation).contentType("application/json").body(projectFilter).post(url);
        scenarioContext().setResponse(response);
        response.then().log().body();
    }
    @Then("cross verify if the response contains projectType {string}")
    public void cross_verify_if_the_response_contains_project_type(String projectType){
        for (int i = 0; i < projectListResponse.getData().size(); i++){
            Assert.assertEquals(projectType,projectListResponse.getData().get(i).getInformation().getProjectType());
            System.out.println(projectListResponse.getData().get(i).getInformation().getProjectType());
        }

    }
    @Then("cross verify if the response contains projectTypes {string},{string}")
    public void cross_verify_if_the_response_contains_project_types(String projectType1, String projectType2) {
        for (int i = 0; i < projectListResponse.getData().size(); i++) {
            if(projectType1.equalsIgnoreCase(projectListResponse.getData().get(i).getInformation().getProjectType())) {
                Assert.assertEquals(projectType1, projectListResponse.getData().get(i).getInformation().getProjectType());
            }
            else
                Assert.assertEquals(projectType2, projectListResponse.getData().get(i).getInformation().getProjectType());
        }
    }

    @Then("cross verify if the response contains searchText {string}")
    public void cross_verify_if_the_response_contains_search_text(String searchText) {
        for (int i = 0; i < projectListResponse.getData().size(); i++){
            Assert.assertEquals(StringUtils.containsIgnoreCase(projectListResponse.getData().get(i).getInformation().getProjectName(),searchText),true);
        }

    }
    @Then("cross verify if the response contains financialYear {string}")
    public void cross_verify_if_the_response_contains_financial_year(String financialYear) {
        for (int i = 0; i < projectListResponse.getData().size(); i++){
            Assert.assertEquals(financialYear, projectListResponse.getData().get(i).getInformation().getFinancialYear());
        }

    }

    @Then("cross verify if the response contains financialYears {string},{string}")
    public void cross_verify_if_the_response_contains_financial_years(String financialYear1, String financialYear2) {
        for (int i = 0; i < projectListResponse.getData().size(); i++){
            if(financialYear1.equals(projectListResponse.getData().get(i).getInformation().getFinancialYear())) {
                Assert.assertEquals(financialYear1, projectListResponse.getData().get(i).getInformation().getFinancialYear());
            }
            else
                Assert.assertEquals(financialYear2, projectListResponse.getData().get(i).getInformation().getFinancialYear());
        }

    }


    @Then("cross verify if the response contains projects between {string} and {string}")
    public void cross_verify_if_the_response_contains_projects_between_and(String fromDate, String toDate) {
        for (int i = 0; i < projectListResponse.getData().size(); i++) {
            DateTimeFormatter pattern = DateTimeFormatter.ofPattern(DATEMONTHFORMAT);
            LocalDateTime date = projectListResponse.getData().get(i).getInformation().getProjectCompletionDate();
            LocalDate datenew = date.toLocalDate();
            String string = DateTimeFormatter.ofPattern(DATEMONTHFORMAT).format(datenew);
            LocalDate createdOn = LocalDate.parse(string, pattern);
            LocalDate fromdate = LocalDate.parse(fromDate, pattern);
            LocalDate todate = LocalDate.parse(toDate, pattern);
            if ((fromdate.compareTo(createdOn) > 0) && (todate.compareTo(createdOn) < 0) || (fromdate.compareTo(createdOn) == 0)) {
                for (LocalDate localdate = fromdate; !localdate.isAfter(todate); localdate = localdate.plusDays(1)) {
                    if ((localdate.compareTo(createdOn) == 0)) {
                        Assert.assertEquals(localdate, createdOn);
                        break;
                    }
                }
            }
        }
    }
}

