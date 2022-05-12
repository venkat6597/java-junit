package com.marksandspencer.foodshub.pal.stepDefinitions;

import com.marksandspencer.foodshub.pal.context.ScenarioContext;
import io.restassured.specification.RequestSpecification;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.LocalServerPort;

@Getter
public class ITBaseStepDefinition {
    @Value("${server.servlet.context-path}")
    protected String rootContext;
    @Value("${it.server.http.host}")
    private String httpHostColon;
    @LocalServerPort
    private int port;

    public ScenarioContext scenarioContext() {
        return ScenarioContext.CONTEXT;
    }

    public String getResourceUrl(String url) {
        return getBaseUri() + url;
    }

    private String getBaseUri() {
        return httpHostColon + port + rootContext;
    }

    public RequestSpecification getRequest() {
        return scenarioContext().getRequest();
    }

    public RequestSpecification getRequestHeaders(String PageObject, String Origin, String Operation) {
        return scenarioContext().getRequestHeaders(PageObject,Origin,Operation);
    }
}