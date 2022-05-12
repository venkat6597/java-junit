package com.marksandspencer.foodshub.pal.stepDefinitions.stepDefs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.marksandspencer.foodshub.pal.constant.ApplicationConstant;
import com.marksandspencer.foodshub.pal.context.ScenarioContext;
import com.marksandspencer.foodshub.pal.domain.PALFields;
import com.marksandspencer.foodshub.pal.domain.PALTemplate;
import com.marksandspencer.foodshub.pal.domain.Section;
import com.marksandspencer.foodshub.pal.stepDefinitions.ITBaseStepDefinition;
import com.marksandspencer.foodshub.pal.transfer.ProductField;
import com.marksandspencer.foodshub.pal.transfer.ProductSection;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.response.Response;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.math3.analysis.function.Exp;
import org.junit.Assert;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;


import static com.marksandspencer.foodshub.pal.stepDefinitions.common.SchemaValidationStep.palProductRequest;
import static com.marksandspencer.foodshub.pal.stepDefinitions.common.SchemaValidationStep.palProductResponseAppResponse;


public class getPALProductInformationlatestStepDefinition extends ITBaseStepDefinition {


    @Then("cross verify with the response for productId.")
    public void cross_verify_with_the_response_for_product_id() {
        Assert.assertEquals(palProductRequest.getProductId() ,palProductResponseAppResponse.getData().getId());
    }

    @Then("cross verify with the response for the role {string}")
    public void cross_verify_with_the_response_for_the_role(String role) throws IOException {
        PALTemplate palTemplate;
        ObjectMapper objectMapper = new ObjectMapper();
        palTemplate = objectMapper.readValue(new File("src/integration-test/resources/schema/db_PALTemplates.json"),
                new TypeReference<>() {
                });
        List<Section> sectionlist = palTemplate.getSections();
        Map<String, List<String>> Expectedmap = new HashMap<>();
        sectionlist.forEach(section -> {
            List<PALFields> palFields = section.getSectionFields().stream().filter(field -> field.getOwner().equalsIgnoreCase(role)).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(palFields)) {

                List<String> palfieldName = palFields.stream().map(PALFields::getId).collect(Collectors.toList());
                Collections.sort(palfieldName);
                Expectedmap.put(section.getSectionName(), palfieldName);

            }
        });

        List<ProductSection> ressectionlist = palProductResponseAppResponse.getData().getSections();
        Map<String, List<String>> Actualmap = new HashMap<>();
        ressectionlist.forEach(section -> {
            List<ProductField> productField = section.getFields().stream().filter(field -> field.getOwner().equalsIgnoreCase(role)).collect(Collectors.toList());
            ProductField smpfield = productField.stream().filter(field -> field.getName().equals(ApplicationConstant.PRINTER_TYPE)).findFirst().orElse(null);
            String smpvalue = ObjectUtils.isEmpty(smpfield)?null:smpfield.getValue();
            if(!ApplicationConstant.SMP.equalsIgnoreCase(smpvalue)){
                List<String> palfields = Expectedmap.get(section.getName());
                palfields.remove(ApplicationConstant.SMP_APPROVED);
                Expectedmap.put(section.getName(), palfields);
            }
            if (!CollectionUtils.isEmpty(productField)) {

                List<String> palfieldName = productField.stream().map(ProductField::getName).collect(Collectors.toList());
                Collections.sort(palfieldName);
                Actualmap.put(section.getName(), palfieldName);

            }
        });
        System.out.println("Expectedmap " + Expectedmap);
        System.out.println(" Actualmap " + Actualmap);
       Assert.assertTrue(Expectedmap.equals(Actualmap));
    }

}
