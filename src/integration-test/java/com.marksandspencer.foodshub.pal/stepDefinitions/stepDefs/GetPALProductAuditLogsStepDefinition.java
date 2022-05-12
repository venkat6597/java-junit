package com.marksandspencer.foodshub.pal.stepDefinitions.stepDefs;


import com.marksandspencer.foodshub.pal.stepDefinitions.ITBaseStepDefinition;
import com.marksandspencer.foodshub.pal.transfer.AppResponse;
import com.marksandspencer.foodshub.pal.transfer.PALProductResponse;
import io.cucumber.java.en.Then;
import org.junit.Assert;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


import static com.marksandspencer.foodshub.pal.constants.ITConstants.DATEMONTHFORMAT;
import static com.marksandspencer.foodshub.pal.stepDefinitions.common.SchemaValidationStep.palProductResponseAppResponse;


public class GetPALProductAuditLogsStepDefinition extends ITBaseStepDefinition {
    AppResponse<PALProductResponse> AuditLogsResponse = palProductResponseAppResponse;

    @Then("cross verify if the response contains productId {string}")
    public void cross_verify_if_the_response_contains_productId(String productId) {
        Assert.assertEquals(productId,AuditLogsResponse.getData().getId());
    }

    @Then("auditlogs in response must be between {string} and {string}")
    public void auditlogs_in_response_must_be_between_and(String fromDate, String toDate) {
        if (!AuditLogsResponse.getData().getAuditlogs().isEmpty()) {
            for (int i = 0; i <AuditLogsResponse.getData().getAuditlogs().size(); i++) {
                DateTimeFormatter pattern = DateTimeFormatter.ofPattern(DATEMONTHFORMAT);
                LocalDateTime date =AuditLogsResponse.getData().getAuditlogs().get(i).getCreatedOn();
                LocalDate datenew = date.toLocalDate();
                String string = DateTimeFormatter.ofPattern(DATEMONTHFORMAT).format(datenew);
                LocalDate createdOn = LocalDate.parse(string,pattern);
                LocalDate fromdate = LocalDate.parse(fromDate,pattern);
                LocalDate todate = LocalDate.parse(toDate,pattern);
                if ((fromdate.compareTo(createdOn) > 0) && (todate.compareTo(createdOn) < 0)||(fromdate.compareTo(createdOn) == 0) ) {
                    for (LocalDate localdate = fromdate;!localdate.isAfter(todate); localdate = localdate.plusDays(1)) {
                        if ((localdate.compareTo(createdOn) == 0)) {
                            Assert.assertEquals(localdate,createdOn);
                            break;
                        }
                    }
                }
            }
        }
    }
}

