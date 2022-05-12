package com.marksandspencer.foodshub.pal.context;

import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import java.util.HashMap;
import java.util.Map;

import static com.marksandspencer.foodshub.pal.constants.ITConstants.*;
import static io.restassured.RestAssured.given;
import static java.lang.ThreadLocal.withInitial;

public enum ScenarioContext {
    CONTEXT;

    public static final String USER = "user";
    public static final String PASSWORD = "password";
    private static final String PAYLOAD = "PAYLOAD";
    public static final String REQUEST = "REQUEST";
    private static final String RESPONSE = "RESPONSE";
    private final ThreadLocal<Map<String, Object>> testContexts = withInitial(HashMap::new);
    private final RestAssuredConfig relaxedValidationConfig = RestAssured.config().sslConfig(new SSLConfig().relaxedHTTPSValidation());

    public <T> T get(String name) {
        return (T) testContexts.get()
                .get(name);
    }

    public <T> T set(String name, T object) {
        testContexts.get()
                .put(name, object);
        return object;
    }

    public RequestSpecification getRequest() {
        if (null == get(REQUEST)) {
            set(REQUEST, given().config(relaxedValidationConfig).auth().preemptive().basic(USER, PASSWORD).log()
                    .all());
        }

        return get(REQUEST);
    }

    public RequestSpecification getRequestHeaders(String PageObject, String Origin, String Operation) {
        if (null == get(REQUEST)) {
            set(REQUEST, given().config(relaxedValidationConfig).
                    headers(Headers(PageObject, Origin, Operation)).log().all());
        }
        return get(REQUEST);
    }

    public Map<String, String> Headers(String PageObject, String Origin, String Operation) {

        Map<String, String> headersKeyValue = new HashMap<>();
        headersKeyValue.put(XACLREQUIRED, "true");
        headersKeyValue.put(ORIGIN, Origin);
        headersKeyValue.put(XOBJECT, PageObject);
        headersKeyValue.put(XOPERATION, Operation);
        headersKeyValue.put(XMODULE, "onemnsfood");
        headersKeyValue.put(XAUTHENTICATION, "");
        return headersKeyValue;
    }

    public Response getResponse() {
        return get(RESPONSE);
    }

    public Response setResponse(Response response) {
        return set(RESPONSE, response);
    }

    public Object getPayload() {
        return get(PAYLOAD);
    }

    public <T> void setPayload(T object) {
        set(PAYLOAD, object);
    }

    public <T> T getPayload(Class<T> clazz) {
        return clazz.cast(get(PAYLOAD));
    }

    public void reset() {
        testContexts.get()
                .clear();
    }


}