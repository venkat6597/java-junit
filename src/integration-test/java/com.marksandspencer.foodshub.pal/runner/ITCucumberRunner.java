package com.marksandspencer.foodshub.pal.runner;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(features = "src/integration-test/resources/features", glue = "com/marksandspencer/foodshub/pal",
        plugin = {"pretty", "de.monochromata.cucumber.report.PrettyReports:target/cucumber", "json:target/cucumber" +
                "-report.json"},
        monochrome = true)

public class ITCucumberRunner {
}