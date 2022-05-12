package com.marksandspencer.foodshub.pal.runner;

import com.marksandspencer.foodshub.pal.context.ScenarioContext;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.spring.CucumberContextConfiguration;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.PropertySource;


@Slf4j
@CucumberContextConfiguration
@PropertySource("application.properties")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CucumberSpringConfiguration {


    @Before
    public void setUp() {
        log.info("------------- TEST CONTEXT SETUP -------------");
    }

    @After
    public void tearDown() {
        log.info("------------- TEST CONTEXT TEAR DOWN -------------");
        ScenarioContext.CONTEXT.reset();
    }

}