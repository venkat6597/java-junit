package com.marksandspencer.foodshub.pal.utility;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marksandspencer.foodshub.pal.constant.ApplicationConstant;
import com.marksandspencer.foodshub.pal.constant.ErrorCode;
import com.marksandspencer.foodshub.pal.domain.DataField;
import com.marksandspencer.foodshub.pal.domain.PALFields;
import com.marksandspencer.foodshub.pal.exception.PALServiceException;
import com.marksandspencer.foodshub.pal.transfer.FieldUpdate;
import com.marksandspencer.foodshub.pal.util.AutoCalculateFunction;
import com.marksandspencer.foodshub.pal.util.Util;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class AutoCalculateFunctionTest {

    List<PALFields> palFields;
    Map<String, Integer> noOfDays;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void beforeTest() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        palFields = mapper.readValue(new File("src/test/resources/ProductAttributeListingResponse/db_PALFields.json"),
                new TypeReference<>() {
                });
        noOfDays = ApplicationConstant.NO_OF_DAYS;
    }

    @Test
    public void autoCompleteSellingPriceFieldUpdateTest() {
        List<FieldUpdate> fieldUpdates = new ArrayList<>();
        fieldUpdates.add(createFieldUpdate(ApplicationConstant.SELLING_PRICE_FIELD, "4.0"));

        List<DataField> dataFields = new ArrayList<>();
        dataFields.add(createDataField(ApplicationConstant.SELLING_PRICE_FIELD, "4.0"));
        dataFields.add(createDataField(ApplicationConstant.UPT_FIELD, "6"));
        dataFields.add(createDataField(ApplicationConstant.MNS_GROSS_COST_PRICE_FIELD, "2.0"));
        dataFields.add(createDataField(ApplicationConstant.AGREED_COST_PRICE_CURRENCY_FIELD, ApplicationConstant.USD));
        dataFields.add(createDataField(ApplicationConstant.PROPOSED_NO_OF_STORES_FIELD, "25"));
        dataFields.add(createDataField(ApplicationConstant.SUCCESS_CRITERIA_FIELD, "10"));

        List<DataField> autoCalculatedDataFields = AutoCalculateFunction.updateAutoCalculatedFields(fieldUpdates, dataFields, palFields);

        DataField traySellingValueField = getDataField(autoCalculatedDataFields, ApplicationConstant.TRAY_SELLING_VALUE_FIELD);
        assertNotNull(traySellingValueField);
        assertEquals(ApplicationConstant.TRAY_SELLING_VALUE_FIELD, traySellingValueField.getFieldId());
        // traySellingValue = sellingprice * upt = 4.0 * 6
        assertEquals("24.0", traySellingValueField.getFieldValue());

        DataField marginField = getDataField(autoCalculatedDataFields, ApplicationConstant.MARGIN_FIELD);
        assertNotNull(marginField);
        assertEquals(ApplicationConstant.MARGIN_FIELD, marginField.getFieldId());
        // margin = (sellingprice - costprice*exchangerate)/sellingprice = (4.0 - 2.0*0.7)/4.0
        assertEquals("0.65", marginField.getFieldValue());

        DataField averageWeeklySalesField = getDataField(autoCalculatedDataFields, ApplicationConstant.AVERAGE_WEEKLY_SALES_MNS_FIELD);
        assertNotNull(averageWeeklySalesField);
        assertEquals(ApplicationConstant.AVERAGE_WEEKLY_SALES_MNS_FIELD, averageWeeklySalesField.getFieldId());
        // averageWeeklySales = sellingprice * noofstores * successcritria = 4.0 * 25 * 10
        assertEquals("1000", averageWeeklySalesField.getFieldValue());

        DataField averageWeeklyVolumeField = getDataField(autoCalculatedDataFields, ApplicationConstant.AVERAGE_WEEKLY_VOLUME_MNS_FIELD);
        assertNotNull(averageWeeklyVolumeField);
        assertEquals(ApplicationConstant.AVERAGE_WEEKLY_VOLUME_MNS_FIELD, averageWeeklyVolumeField.getFieldId());
        // averageWeeklyVolume = averageWeeklySales / sellingprice = (4.0 * 25 * 10)/4.0 = 250
        assertEquals("250", averageWeeklyVolumeField.getFieldValue());

    }

    private DataField getDataField(List<DataField> dataFields, String fieldId) {
        return dataFields.stream()
                .filter(field -> field.getFieldId().equals(fieldId))
                .findFirst().orElse(null);
    }

    private DataField createDataField(String fieldId, String fieldValue) {
        DataField dataField = new DataField();
        dataField.setFieldId(fieldId);
        dataField.setFieldValue(fieldValue);
        return dataField;
    }

    private FieldUpdate createFieldUpdate(String fieldId, String fieldValue) {
        FieldUpdate update = new FieldUpdate();
        update.setField(fieldId);
        update.setNewValue(fieldValue);
        return update;
    }

    @Test
    public void autoCompleteUPTFieldUpdateTest() {
        List<FieldUpdate> fieldUpdates = new ArrayList<>();
        fieldUpdates.add(createFieldUpdate(ApplicationConstant.UPT_FIELD, "6"));

        List<DataField> dataFields = new ArrayList<>();
        dataFields.add(createDataField(ApplicationConstant.SELLING_PRICE_FIELD, "4.0"));
        dataFields.add(createDataField(ApplicationConstant.MNS_GROSS_COST_PRICE_FIELD, "2.0"));
        dataFields.add(createDataField(ApplicationConstant.UPT_FIELD, "6"));

        List<DataField> autoCalculatedDataFields = AutoCalculateFunction.updateAutoCalculatedFields(fieldUpdates, dataFields, palFields);

        DataField traySellingValueField = getDataField(autoCalculatedDataFields, ApplicationConstant.TRAY_SELLING_VALUE_FIELD);
        assertNotNull(traySellingValueField);
        assertEquals(ApplicationConstant.TRAY_SELLING_VALUE_FIELD, traySellingValueField.getFieldId());
        // traySellingValue = sellingprice * upt = 4.0 * 6
        assertEquals("24.0", traySellingValueField.getFieldValue());

        DataField trayCostValueField = getDataField(autoCalculatedDataFields, ApplicationConstant.TRAY_COST_VALUE_FIELD);
        assertNotNull(trayCostValueField);
        assertEquals(ApplicationConstant.TRAY_COST_VALUE_FIELD, trayCostValueField.getFieldId());
        // trayCostPrice = costprice * upt = 2.0 * 6
        assertEquals("12.0", trayCostValueField.getFieldValue());
    }

    @Test
    public void autoCompleteNonFieldTest() {
        List<FieldUpdate> fieldUpdates = new ArrayList<>();
        fieldUpdates.add(createFieldUpdate(ApplicationConstant.PRODUCT_TITLE_FIELD, "TEST"));

        List<DataField> dataFields = new ArrayList<>();
        dataFields.add(createDataField(ApplicationConstant.PRODUCT_TITLE_FIELD, "TEST"));
        List<DataField> dataFieldsActual = AutoCalculateFunction.updateAutoCalculatedFields(fieldUpdates, dataFields, palFields);
        assertNotNull(dataFieldsActual);
        assertEquals(ApplicationConstant.PRODUCT_TITLE_FIELD, dataFieldsActual.get(0).getFieldId());
        assertEquals("TEST", dataFieldsActual.get(0).getFieldValue());
    }

    @Test
    public void autoCompleteMnSCostPriceFieldUpdateTest() {
        List<FieldUpdate> fieldUpdates = new ArrayList<>();
        fieldUpdates.add(createFieldUpdate(ApplicationConstant.MNS_GROSS_COST_PRICE_FIELD, "2.0"));

        List<DataField> dataFields = new ArrayList<>();
        dataFields.add(createDataField(ApplicationConstant.SELLING_PRICE_FIELD, "4.0"));
        dataFields.add(createDataField(ApplicationConstant.MNS_GROSS_COST_PRICE_FIELD, "2.0"));
        dataFields.add(createDataField(ApplicationConstant.UPT_FIELD, "6"));
        dataFields.add(createDataField(ApplicationConstant.AGREED_COST_PRICE_CURRENCY_FIELD, ApplicationConstant.EUR));

        List<DataField> autoCalculatedDataFields = AutoCalculateFunction.updateAutoCalculatedFields(fieldUpdates, dataFields, palFields);

        DataField trayCostValueField = getDataField(autoCalculatedDataFields, ApplicationConstant.TRAY_COST_VALUE_FIELD);
        assertNotNull(trayCostValueField);
        assertEquals(ApplicationConstant.TRAY_COST_VALUE_FIELD, trayCostValueField.getFieldId());
        // trayCostPrice = costprice * upt = 2.0 * 6
        assertEquals("12.0", trayCostValueField.getFieldValue());

        DataField marginField = getDataField(autoCalculatedDataFields, ApplicationConstant.MARGIN_FIELD);
        assertNotNull(marginField);
        assertEquals(ApplicationConstant.MARGIN_FIELD, marginField.getFieldId());
        // margin = (sellingprice - costprice*exchangerate)/sellingprice = (4.0 - 2.0*0.9)/4.0
        assertEquals("0.55", marginField.getFieldValue());
    }

    @Test
    public void autoCompleteCurrencyFieldUpdateTest() {
        List<FieldUpdate> fieldUpdates = new ArrayList<>();
        fieldUpdates.add(createFieldUpdate(ApplicationConstant.AGREED_COST_PRICE_CURRENCY_FIELD, "USD"));

        List<DataField> dataFields = new ArrayList<>();
        dataFields.add(createDataField(ApplicationConstant.SELLING_PRICE_FIELD, "4.0"));
        dataFields.add(createDataField(ApplicationConstant.AGREED_COST_PRICE_CURRENCY_FIELD, "GBP"));
        dataFields.add(createDataField(ApplicationConstant.MNS_GROSS_COST_PRICE_FIELD, "2.0"));

        List<DataField> autoCalculatedDataFields = AutoCalculateFunction.updateAutoCalculatedFields(fieldUpdates, dataFields, palFields);

        DataField marginField = getDataField(autoCalculatedDataFields, ApplicationConstant.MARGIN_FIELD);
        assertNotNull(marginField);
        assertEquals(ApplicationConstant.MARGIN_FIELD, marginField.getFieldId());
        // margin = (sellingprice - costprice*exchangerate)/sellingprice = (4.0 - 2.0*1)/4.0
        assertEquals("0.5", marginField.getFieldValue());
    }

    @Test
    public void autoCompleteProposedNoOfStoresFieldUpdateTest() {
        List<FieldUpdate> fieldUpdates = new ArrayList<>();
        fieldUpdates.add(createFieldUpdate(ApplicationConstant.PROPOSED_NO_OF_STORES_FIELD, "10"));

        List<DataField> dataFields = new ArrayList<>();
        dataFields.add(createDataField(ApplicationConstant.SELLING_PRICE_FIELD, "4.0"));
        dataFields.add(createDataField(ApplicationConstant.PROPOSED_NO_OF_STORES_FIELD, "10"));
        dataFields.add(createDataField(ApplicationConstant.SUCCESS_CRITERIA_FIELD, "25"));

        List<DataField> autoCalculatedDataFields = AutoCalculateFunction.updateAutoCalculatedFields(fieldUpdates, dataFields, palFields);

        DataField averageWeeklySalesField = getDataField(autoCalculatedDataFields, ApplicationConstant.AVERAGE_WEEKLY_SALES_MNS_FIELD);
        assertNotNull(averageWeeklySalesField);
        assertEquals(ApplicationConstant.AVERAGE_WEEKLY_SALES_MNS_FIELD, averageWeeklySalesField.getFieldId());
        // averageWeeklySales = sellingprice * noofstores * successcritria = 4.0 * 25 * 10
        assertEquals("1000", averageWeeklySalesField.getFieldValue());

        DataField averageWeeklyVolumeField = getDataField(autoCalculatedDataFields, ApplicationConstant.AVERAGE_WEEKLY_VOLUME_MNS_FIELD);
        assertNotNull(averageWeeklyVolumeField);
        assertEquals(ApplicationConstant.AVERAGE_WEEKLY_VOLUME_MNS_FIELD, averageWeeklyVolumeField.getFieldId());
        // averageWeeklyVolume = averageWeeklySales / sellingprice = (4.0 * 25 * 10)/4.0 = 250
        assertEquals("250", averageWeeklyVolumeField.getFieldValue());
    }

    @Test
    public void autoCompleteSuccessCriteriaFieldUpdateTest() {
        List<FieldUpdate> fieldUpdates = new ArrayList<>();
        fieldUpdates.add(createFieldUpdate(ApplicationConstant.SUCCESS_CRITERIA_FIELD, "25"));

        List<DataField> dataFields = new ArrayList<>();
        dataFields.add(createDataField(ApplicationConstant.SELLING_PRICE_FIELD, "4.0"));
        dataFields.add(createDataField(ApplicationConstant.PROPOSED_NO_OF_STORES_FIELD, "10"));
        dataFields.add(createDataField(ApplicationConstant.SUCCESS_CRITERIA_FIELD, "25"));

        List<DataField> autoCalculatedDataFields = AutoCalculateFunction.updateAutoCalculatedFields(fieldUpdates, dataFields, palFields);

        DataField averageWeeklySalesField = getDataField(autoCalculatedDataFields, ApplicationConstant.AVERAGE_WEEKLY_SALES_MNS_FIELD);
        assertNotNull(averageWeeklySalesField);
        assertEquals(ApplicationConstant.AVERAGE_WEEKLY_SALES_MNS_FIELD, averageWeeklySalesField.getFieldId());
        // averageWeeklySales = sellingprice * noofstores * successcritria = 4.0 * 25 * 10
        assertEquals("1000", averageWeeklySalesField.getFieldValue());

        DataField averageWeeklyVolumeField = getDataField(autoCalculatedDataFields, ApplicationConstant.AVERAGE_WEEKLY_VOLUME_MNS_FIELD);
        assertNotNull(averageWeeklyVolumeField);
        assertEquals(ApplicationConstant.AVERAGE_WEEKLY_VOLUME_MNS_FIELD, averageWeeklyVolumeField.getFieldId());
        // averageWeeklyVolume = averageWeeklySales / sellingprice = (4.0 * 25 * 10)/4.0 = 250
        assertEquals("250", averageWeeklyVolumeField.getFieldValue());
    }

    @Test
    public void autoCompleteFtpDateFieldUpdateTest() {
        List<FieldUpdate> fieldUpdates = new ArrayList<>();

        fieldUpdates.add(createFieldUpdate(ApplicationConstant.FTP_DATE_FIELD, "30/12/2500"));

        List<DataField> dataFields = new ArrayList<>();
        dataFields.add(createDataField(ApplicationConstant.FTP_DATE_FIELD, "30/12/2500"));

        List<DataField> autoCalculatedDataFields = AutoCalculateFunction.updateAutoCalculatedFields(fieldUpdates, dataFields, palFields);

        DataField batonHandOverField = getDataField(autoCalculatedDataFields, ApplicationConstant.BATON_HANDOVER_FIELD);
        assertNotNull(batonHandOverField);
        assertEquals(ApplicationConstant.BATON_HANDOVER_FIELD, batonHandOverField.getFieldId());
        // batonHandover = ftpDate - 19 workingdays = 30/12/2500 - (19 working days + 8 weekend days)
        assertEquals("03/12/2500", batonHandOverField.getFieldValue());

        DataField designConceptHandOverField = getDataField(autoCalculatedDataFields, ApplicationConstant.DESIGN_CONCEPT_HANDOVER_FIELD);
        assertNotNull(designConceptHandOverField);
        assertEquals(ApplicationConstant.DESIGN_CONCEPT_HANDOVER_FIELD, designConceptHandOverField.getFieldId());
        // designConceptHandOverField = batonHandover - 12 workingdays = 03/12/2500 - (12 working days + 4 weekend days)
        assertEquals("17/11/2500", designConceptHandOverField.getFieldValue());

        DataField finaliseGateField = getDataField(autoCalculatedDataFields, ApplicationConstant.FINALISE_GATE_FIELD);
        assertNotNull(finaliseGateField);
        assertEquals(ApplicationConstant.FINALISE_GATE_FIELD, finaliseGateField.getFieldId());
        // finaliseGate = batonHandOver - 14 workingdays = 15/11/2500 - (14 working days + 4 weekend days)
        assertEquals("15/11/2500", finaliseGateField.getFieldValue());
    }

    @Test
    public void autoCompleteBatonHandOverFieldUpdateTest() {
        List<FieldUpdate> fieldUpdates = new ArrayList<>();

        fieldUpdates.add(createFieldUpdate(ApplicationConstant.BATON_HANDOVER_FIELD, "03/12/2500"));

        List<DataField> dataFields = new ArrayList<>();
        dataFields.add(createDataField(ApplicationConstant.BATON_HANDOVER_FIELD, "03/12/2500"));

        List<DataField> autoCalculatedDataFields = AutoCalculateFunction.updateAutoCalculatedFields(fieldUpdates, dataFields, palFields);

        DataField designConceptHandOverField = getDataField(autoCalculatedDataFields, ApplicationConstant.DESIGN_CONCEPT_HANDOVER_FIELD);
        assertNotNull(designConceptHandOverField);
        assertEquals(ApplicationConstant.DESIGN_CONCEPT_HANDOVER_FIELD, designConceptHandOverField.getFieldId());
        // designConceptHandOverField = batonHandover - 12 workingdays = 03/12/2500 - (12 working days + 4 weekend days)
        assertEquals("17/11/2500", designConceptHandOverField.getFieldValue());

        DataField finaliseGateField = getDataField(autoCalculatedDataFields, ApplicationConstant.FINALISE_GATE_FIELD);
        assertNotNull(finaliseGateField);
        assertEquals(ApplicationConstant.FINALISE_GATE_FIELD, finaliseGateField.getFieldId());
        // batonHandover = ftpDate - 14 workingdays = 15/11/2500 - (14 working days + 4 weekend days)
        assertEquals("15/11/2500", finaliseGateField.getFieldValue());
    }

    @Test
    public void autoCompleteFtpDateFieldInvalidDateFormatUpdateTest() {
        expectedException.expect(PALServiceException.class);
        expectedException.expectMessage(ErrorCode.INVALID_DATE.getErrorMessage());

        List<FieldUpdate> fieldUpdates = new ArrayList<>();
        fieldUpdates.add(createFieldUpdate(ApplicationConstant.FTP_DATE_FIELD, "invalid"));

        List<DataField> dataFields = new ArrayList<>();
        dataFields.add(createDataField(ApplicationConstant.FTP_DATE_FIELD, "invalid"));

        AutoCalculateFunction.updateAutoCalculatedFields(fieldUpdates, dataFields, palFields);

    }

    @Test
    public void autoCompleteFtpDateFieldUpdatePastBatonHandOverDateTest() {
        Integer days = noOfDays.get(ApplicationConstant.BATON_HANDOVER_FIELD) +
                noOfDays.get(ApplicationConstant.FINALISE_GATE_FIELD);
        String msg = "FTP Date should be after ".concat(Util.convertLocalDateTimeToString(Util.addDaysSkippingWeekends(Util.currentLocalDateTime(), days)));
        expectedException.expect(PALServiceException.class);
        expectedException.expectMessage(msg);

        List<FieldUpdate> fieldUpdates = new ArrayList<>();
        fieldUpdates.add(createFieldUpdate(ApplicationConstant.FTP_DATE_FIELD, "28/12/2021"));

        List<DataField> dataFields = new ArrayList<>();
        dataFields.add(createDataField(ApplicationConstant.FTP_DATE_FIELD, "28/12/2021"));

        AutoCalculateFunction.updateAutoCalculatedFields(fieldUpdates, dataFields, palFields);

    }

    @Test
    public void autoCompleteSellingPriceFieldUpdateExistingTest() {
        List<FieldUpdate> fieldUpdates = new ArrayList<>();
        fieldUpdates.add(createFieldUpdate(ApplicationConstant.SELLING_PRICE_FIELD, "4.0"));

        List<DataField> dataFields = new ArrayList<>();
        dataFields.add(createDataField(ApplicationConstant.SELLING_PRICE_FIELD, "4.0"));
        dataFields.add(createDataField(ApplicationConstant.UPT_FIELD, "6"));
        dataFields.add(createDataField(ApplicationConstant.MNS_GROSS_COST_PRICE_FIELD, "2.0"));
        dataFields.add(createDataField(ApplicationConstant.AGREED_COST_PRICE_CURRENCY_FIELD, "USD"));
        dataFields.add(createDataField(ApplicationConstant.PROPOSED_NO_OF_STORES_FIELD, "25"));
        dataFields.add(createDataField(ApplicationConstant.SUCCESS_CRITERIA_FIELD, "10"));
        dataFields.add(createDataField(ApplicationConstant.TRAY_SELLING_VALUE_FIELD, "10"));
        dataFields.add(createDataField(ApplicationConstant.MARGIN_FIELD, "10"));
        dataFields.add(createDataField(ApplicationConstant.AVERAGE_WEEKLY_SALES_MNS_FIELD, "10"));
        dataFields.add(createDataField(ApplicationConstant.AVERAGE_WEEKLY_VOLUME_MNS_FIELD, "10"));


        List<DataField> autoCalculatedDataFields = AutoCalculateFunction.updateAutoCalculatedFields(fieldUpdates, dataFields, palFields);

        DataField traySellingValueField = getDataField(autoCalculatedDataFields, ApplicationConstant.TRAY_SELLING_VALUE_FIELD);
        assertNotNull(traySellingValueField);
        assertEquals(ApplicationConstant.TRAY_SELLING_VALUE_FIELD, traySellingValueField.getFieldId());
        // traySellingValue = sellingprice * upt = 4.0 * 6
        assertEquals("24.0", traySellingValueField.getFieldValue());

        DataField marginField = getDataField(autoCalculatedDataFields, ApplicationConstant.MARGIN_FIELD);
        assertNotNull(marginField);
        assertEquals(ApplicationConstant.MARGIN_FIELD, marginField.getFieldId());
        // margin = (sellingprice - costprice*exchangerate)/sellingprice = (4.0 - 2.0*0.7)/4.0
        assertEquals("0.65", marginField.getFieldValue());

        DataField averageWeeklySalesField = getDataField(autoCalculatedDataFields, ApplicationConstant.AVERAGE_WEEKLY_SALES_MNS_FIELD);
        assertNotNull(averageWeeklySalesField);
        assertEquals(ApplicationConstant.AVERAGE_WEEKLY_SALES_MNS_FIELD, averageWeeklySalesField.getFieldId());
        // averageWeeklySales = sellingprice * noofstores * successcritria = 4.0 * 25 * 10
        assertEquals("1000", averageWeeklySalesField.getFieldValue());

        DataField averageWeeklyVolumeField = getDataField(autoCalculatedDataFields, ApplicationConstant.AVERAGE_WEEKLY_VOLUME_MNS_FIELD);
        assertNotNull(averageWeeklyVolumeField);
        assertEquals(ApplicationConstant.AVERAGE_WEEKLY_VOLUME_MNS_FIELD, averageWeeklyVolumeField.getFieldId());
        // averageWeeklyVolume = averageWeeklySales / sellingprice = (4.0 * 25 * 10)/4.0 = 250
        assertEquals("250", averageWeeklyVolumeField.getFieldValue());

    }

    @Test
    public void autoCompleteSellingPriceFieldUpdateNullFieldTest() {
        List<FieldUpdate> fieldUpdates = new ArrayList<>();
        fieldUpdates.add(createFieldUpdate(ApplicationConstant.SELLING_PRICE_FIELD, "4.0"));

        List<DataField> dataFields = new ArrayList<>();
        dataFields.add(createDataField(ApplicationConstant.SELLING_PRICE_FIELD, "4.0"));
        dataFields.add(createDataField(ApplicationConstant.UPT_FIELD, "6"));
        dataFields.add(createDataField(ApplicationConstant.MNS_GROSS_COST_PRICE_FIELD, "2.0"));
        dataFields.add(createDataField(ApplicationConstant.AGREED_COST_PRICE_CURRENCY_FIELD, "USD"));
        dataFields.add(createDataField(ApplicationConstant.SUCCESS_CRITERIA_FIELD, "10"));
        dataFields.add(createDataField(ApplicationConstant.TRAY_SELLING_VALUE_FIELD, "10"));
        dataFields.add(createDataField(ApplicationConstant.MARGIN_FIELD, null));

        List<DataField> autoCalculatedDataFields = AutoCalculateFunction.updateAutoCalculatedFields(fieldUpdates, dataFields, palFields);

        DataField traySellingValueField = getDataField(autoCalculatedDataFields, ApplicationConstant.TRAY_SELLING_VALUE_FIELD);
        assertNotNull(traySellingValueField);
        assertEquals(ApplicationConstant.TRAY_SELLING_VALUE_FIELD, traySellingValueField.getFieldId());
        // traySellingValue = sellingprice * upt = 4.0 * 6
        assertEquals("24.0", traySellingValueField.getFieldValue());

        DataField marginField = getDataField(autoCalculatedDataFields, ApplicationConstant.MARGIN_FIELD);
        assertNotNull(marginField);
        assertEquals(ApplicationConstant.MARGIN_FIELD, marginField.getFieldId());
        // margin = (sellingprice - costprice*exchangerate)/sellingprice = (4.0 - 2.0*0.7)/4.0
        assertEquals("0.65", marginField.getFieldValue());

        DataField averageWeeklySalesField = getDataField(autoCalculatedDataFields, ApplicationConstant.AVERAGE_WEEKLY_SALES_MNS_FIELD);
        assertEquals(null, averageWeeklySalesField.getFieldValue());

        DataField averageWeeklyVolumeField = getDataField(autoCalculatedDataFields, ApplicationConstant.AVERAGE_WEEKLY_VOLUME_MNS_FIELD);
        assertEquals(null, averageWeeklyVolumeField.getFieldValue());
    }

    @Test
    public void autoCompleteZeroSellingPriceFieldUpdateTest() {

        expectedException.expect(PALServiceException.class);
        expectedException.expectMessage(ErrorCode.INVALID_NUMBER.getErrorMessage());

        List<FieldUpdate> fieldUpdates = new ArrayList<>();
        fieldUpdates.add(createFieldUpdate(ApplicationConstant.SELLING_PRICE_FIELD, "0.0"));

        List<DataField> dataFields = new ArrayList<>();
        dataFields.add(createDataField(ApplicationConstant.SELLING_PRICE_FIELD, "0.0"));
        dataFields.add(createDataField(ApplicationConstant.UPT_FIELD, "6"));
        dataFields.add(createDataField(ApplicationConstant.MNS_GROSS_COST_PRICE_FIELD, "2.0"));
        dataFields.add(createDataField(ApplicationConstant.AGREED_COST_PRICE_CURRENCY_FIELD, "USD"));
        dataFields.add(createDataField(ApplicationConstant.SUCCESS_CRITERIA_FIELD, "10"));

        AutoCalculateFunction.updateAutoCalculatedFields(fieldUpdates, dataFields, palFields);
    }

    @Test
    public void autoCompleteFieldUpdateInvalidAutoFieldTest() {

        expectedException.expect(PALServiceException.class);
        expectedException.expectMessage(ErrorCode.INVALID_NUMBER.getErrorMessage());

        List<FieldUpdate> fieldUpdates = new ArrayList<>();
        fieldUpdates.add(createFieldUpdate(ApplicationConstant.SELLING_PRICE_FIELD, "0.0"));

        List<DataField> dataFields = new ArrayList<>();
        dataFields.add(createDataField(ApplicationConstant.SELLING_PRICE_FIELD, "0.0"));
        dataFields.add(createDataField(ApplicationConstant.UPT_FIELD, "6"));
        dataFields.add(createDataField(ApplicationConstant.MNS_GROSS_COST_PRICE_FIELD, "2.0"));
        dataFields.add(createDataField(ApplicationConstant.AGREED_COST_PRICE_CURRENCY_FIELD, "USD"));
        dataFields.add(createDataField(ApplicationConstant.SUCCESS_CRITERIA_FIELD, "10"));

        AutoCalculateFunction.updateAutoCalculatedFields(fieldUpdates, dataFields, palFields);
    }
}