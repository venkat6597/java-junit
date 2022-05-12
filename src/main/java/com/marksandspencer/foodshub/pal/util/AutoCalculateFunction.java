package com.marksandspencer.foodshub.pal.util;

import com.marksandspencer.foodshub.pal.constant.ApplicationConstant;
import com.marksandspencer.foodshub.pal.constant.ErrorCode;
import com.marksandspencer.foodshub.pal.domain.DataField;
import com.marksandspencer.foodshub.pal.domain.PALFields;
import com.marksandspencer.foodshub.pal.exception.PALServiceException;
import com.marksandspencer.foodshub.pal.transfer.FieldUpdate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class AutoCalculateFunction {

    public static DecimalFormat df = new DecimalFormat("#.##");

    public static List<DataField> updateAutoCalculatedFields(List<FieldUpdate> fieldUpdates, List<DataField> datafields, List<PALFields> palFields) {
        List<DataField> autoCalculatedFields = new ArrayList<>();
        fieldUpdates.forEach(field -> {
            // Ignore subsection field updates
            if (StringUtils.isEmpty(field.getSubSectionId()) && ObjectUtils.isEmpty(field.getSubSectionFields())) {
                String fieldKey = field.getField();
                autoCalculatedFields.addAll(getAutoCalculatedField(fieldKey, datafields, palFields));
            }
        });

        return mergedDataFields(autoCalculatedFields, datafields);

    }

    public static List<DataField> mergedDataFields(List<DataField> replaceFields, List<DataField> datafields) {
        List<DataField> newDataFields = new ArrayList<>();
        datafields.forEach(field -> newDataFields.add(DataField.builder()
                .fieldId(field.getFieldId())
                .fieldValue(field.getFieldValue())
                .multifields(field.getMultifields())
                .build()));
        replaceFields.forEach(field -> {
            String fieldKey = field.getFieldId();
            String fieldValue = field.getFieldValue();
            DataField dataField = newDataFields.stream().filter(replaceField -> replaceField.getFieldId().equals(fieldKey))
                    .findFirst().orElse(null);
            if (!ObjectUtils.isEmpty(dataField)) {
                dataField.setFieldValue(fieldValue);
            }
            else {
                newDataFields.add(DataField.builder()
                        .fieldId(fieldKey)
                        .fieldValue(fieldValue)
                        .build());
            }
        });
        return newDataFields.stream().distinct().collect(Collectors.toList());
    }

    public static List<DataField> getAutoCalculatedField(String fieldId, List<DataField> datafields, List<PALFields> palFields) {
        List<DataField> autoCalculatedFields = new ArrayList<>();

        PALFields palField = CommonUtility.getPALFieldById(fieldId, palFields);

        if (!Objects.isNull(palField)) {
            List<String> palFunctions = palField.getScripts();
            if (!CollectionUtils.isEmpty(palFunctions)) {
                palFunctions.forEach(function -> autoCalculatedFields.addAll(callAutoCalculateFunction(function, datafields, palFields)));
            }
        }
        return autoCalculatedFields;
    }

    private static List<DataField> callAutoCalculateFunction(String functionName, List<DataField> datafields, List<PALFields> palFields) {
        List<DataField> autoCalculatedFields = new ArrayList<>();
        switch (functionName) {
            case ApplicationConstant.TRAY_SELLING_VALUE_FIELD:
                autoCalculatedFields.addAll(calculateTraySellingValue(datafields, palFields));
                break;
            case ApplicationConstant.TRAY_COST_VALUE_FIELD:
                autoCalculatedFields.addAll(calculateTrayCostValue(datafields, palFields));
                break;
            case ApplicationConstant.MARGIN_FIELD:
                autoCalculatedFields.addAll(calculateMargin(datafields, palFields));
                break;
            case ApplicationConstant.AVERAGE_WEEKLY_SALES_MNS_FIELD:
                autoCalculatedFields.addAll(calculateAverageWeeklySalesMnS(datafields, palFields));
                break;
            case ApplicationConstant.AVERAGE_WEEKLY_VOLUME_MNS_FIELD:
                autoCalculatedFields.addAll(calculateAverageWeeklyVolumeMns(datafields, palFields));
                break;
            case ApplicationConstant.BATON_HANDOVER_FIELD:
                autoCalculatedFields.addAll(calculateBatonHandover(datafields, palFields));
                break;
            case ApplicationConstant.DESIGN_CONCEPT_HANDOVER_FIELD:
                autoCalculatedFields.addAll(calculateDesignConceptHandOver(datafields, palFields));
                break;
            case ApplicationConstant.FINALISE_GATE_FIELD:
                autoCalculatedFields.addAll(calculateFinaliseGate(datafields, palFields));
                break;
            default:
                log.error("Autocalculation function not available for {}", functionName);
                throw new PALServiceException("Autocalculation function not available for " + functionName);
        }
        return autoCalculatedFields;
    }

    private static List<DataField> calculateFinaliseGate(List<DataField> datafields, List<PALFields> palFields) {
        String fieldKey = ApplicationConstant.FINALISE_GATE_FIELD;
        List<DataField> autoCalculatedFields = new ArrayList<>();
        String batonHandOverString = CommonUtility.getDataFieldValue(datafields, ApplicationConstant.BATON_HANDOVER_FIELD);
        String finaliseGate = null;
        if (!StringUtils.isEmpty(batonHandOverString)) {
            LocalDateTime batonHandover = Util.dateConvertor(batonHandOverString);
            LocalDateTime finaliseGateDate = Util.subtractDaysSkippingWeekends(batonHandover, ApplicationConstant.NO_OF_DAYS.get(fieldKey));
            finaliseGate = Util.convertLocalDateTimeToString(finaliseGateDate);
        }

        autoCalculatedFields.add(CommonUtility.createDataField(fieldKey, finaliseGate));

        List<DataField> mergedDataFields = mergedDataFields(autoCalculatedFields, datafields);
        autoCalculatedFields.addAll(getAutoCalculatedField(fieldKey, mergedDataFields, palFields));
        return autoCalculatedFields;
    }

    private static List<DataField> calculateDesignConceptHandOver(List<DataField> datafields, List<PALFields> palFields) {
        String fieldKey = ApplicationConstant.DESIGN_CONCEPT_HANDOVER_FIELD;
        List<DataField> autoCalculatedFields = new ArrayList<>();
        String batonHandOverString = CommonUtility.getDataFieldValue(datafields, ApplicationConstant.BATON_HANDOVER_FIELD);
        String designConceptHandover = null;
        if (!StringUtils.isEmpty(batonHandOverString)) {
            LocalDateTime batonHandover = Util.dateConvertor(batonHandOverString);
            LocalDateTime designConceptHandoverDate = Util.subtractDaysSkippingWeekends(batonHandover, ApplicationConstant.NO_OF_DAYS.get(fieldKey));
            designConceptHandover = Util.convertLocalDateTimeToString(designConceptHandoverDate);
        }

        autoCalculatedFields.add(CommonUtility.createDataField(fieldKey, designConceptHandover));

        List<DataField> mergedDataFields = mergedDataFields(autoCalculatedFields, datafields);
        autoCalculatedFields.addAll(getAutoCalculatedField(fieldKey, mergedDataFields, palFields));
        return autoCalculatedFields;
    }

    private static List<DataField> calculateBatonHandover(List<DataField> datafields, List<PALFields> palFields) {
        String fieldKey = ApplicationConstant.BATON_HANDOVER_FIELD;
        List<DataField> autoCalculatedFields = new ArrayList<>();
        String ftpDateString = CommonUtility.getDataFieldValue(datafields, ApplicationConstant.FTP_DATE_FIELD);
        String batonHandover = null;
        if (!StringUtils.isEmpty(ftpDateString)) {
            LocalDateTime ftpDate = Util.dateConvertor(ftpDateString);
            validateFTPDate(ftpDate);

            LocalDateTime batonHandOverDate = Util.subtractDaysSkippingWeekends(ftpDate, ApplicationConstant.NO_OF_DAYS.get(fieldKey));
            batonHandover = Util.convertLocalDateTimeToString(batonHandOverDate);
        }

        autoCalculatedFields.add(CommonUtility.createDataField(fieldKey, batonHandover));

        List<DataField> mergedDataFields = mergedDataFields(autoCalculatedFields, datafields);
        autoCalculatedFields.addAll(getAutoCalculatedField(fieldKey, mergedDataFields, palFields));
        return autoCalculatedFields;
    }

    private static void validateFTPDate(LocalDateTime ftpDate) {
        LocalDateTime currentDate = Util.currentLocalDateTime();
        Integer batonHandoverDays = ApplicationConstant.NO_OF_DAYS.get(ApplicationConstant.BATON_HANDOVER_FIELD);
        Integer finaliseGateDays = ApplicationConstant.NO_OF_DAYS.get(ApplicationConstant.FINALISE_GATE_FIELD);
        LocalDateTime validFTPDate = Util.addDaysSkippingWeekends(currentDate, batonHandoverDays+finaliseGateDays);
        if (ftpDate.isBefore(validFTPDate)) {
            String message = String.format("FTP Date should be after %s", Util.convertLocalDateTimeToString(validFTPDate));
            throw new PALServiceException(message);
        }
    }

    private static List<DataField> calculateAverageWeeklyVolumeMns(List<DataField> datafields, List<PALFields> palFields) {
        String fieldKey = ApplicationConstant.AVERAGE_WEEKLY_VOLUME_MNS_FIELD;
        List<DataField> autoCalculatedFields = new ArrayList<>();
        String averageWeeklySalesString = CommonUtility.getDataFieldValue(datafields, ApplicationConstant.AVERAGE_WEEKLY_SALES_MNS_FIELD);
        String sellingPriceString = CommonUtility.getDataFieldValue(datafields, ApplicationConstant.SELLING_PRICE_FIELD);
        String averageWeeklyVolume = null;
        if (!StringUtils.isEmpty(averageWeeklySalesString) && !StringUtils.isEmpty(sellingPriceString)) {
            double averageWeeklySales = Util.convertStringToDouble(averageWeeklySalesString);
            double sellingPrice = Util.convertStringToDouble(sellingPriceString);
            if (0 != (int) sellingPrice) {
                int calculatedAverageWeeklyVolume = (int) (averageWeeklySales / sellingPrice);
                averageWeeklyVolume = Integer.toString(calculatedAverageWeeklyVolume);
            } else {
                throw new PALServiceException(ErrorCode.INVALID_NUMBER);
            }
        }

        autoCalculatedFields.add(CommonUtility.createDataField(fieldKey, averageWeeklyVolume));

        List<DataField> mergedDataFields = mergedDataFields(autoCalculatedFields, datafields);
        autoCalculatedFields.addAll(getAutoCalculatedField(fieldKey, mergedDataFields, palFields));
        return autoCalculatedFields;
    }

    private static List<DataField> calculateAverageWeeklySalesMnS(List<DataField> datafields, List<PALFields> palFields) {
        String fieldKey = ApplicationConstant.AVERAGE_WEEKLY_SALES_MNS_FIELD;
        List<DataField> autoCalculatedFields = new ArrayList<>();
        String proposedNoOfStoresString = CommonUtility.getDataFieldValue(datafields, ApplicationConstant.PROPOSED_NO_OF_STORES_FIELD);
        String sellingPriceString = CommonUtility.getDataFieldValue(datafields, ApplicationConstant.SELLING_PRICE_FIELD);
        String successCriteriaString = CommonUtility.getDataFieldValue(datafields, ApplicationConstant.SUCCESS_CRITERIA_FIELD);
        String averageWeeklySales = null;
        if (!StringUtils.isEmpty(proposedNoOfStoresString) && !StringUtils.isEmpty(sellingPriceString) && !StringUtils.isEmpty(successCriteriaString)) {
            Integer proposedNoOfStores = Util.convertStringToInteger(proposedNoOfStoresString);
            double sellingPrice = Util.convertStringToDouble(sellingPriceString);
            Integer successCriteria = Util.convertStringToInteger(successCriteriaString);
            int calculatedAverageWeeklySales = (int) (proposedNoOfStores * sellingPrice * successCriteria);
            averageWeeklySales = Integer.toString(calculatedAverageWeeklySales);
        }

        autoCalculatedFields.add(CommonUtility.createDataField(fieldKey, averageWeeklySales));

        List<DataField> mergedDataFields = mergedDataFields(autoCalculatedFields, datafields);
        autoCalculatedFields.addAll(getAutoCalculatedField(fieldKey, mergedDataFields, palFields));
        return autoCalculatedFields;
    }

    private static List<DataField> calculateMargin(List<DataField> datafields, List<PALFields> palFields) {
        String fieldKey = ApplicationConstant.MARGIN_FIELD;
        List<DataField> autoCalculatedFields = new ArrayList<>();
        String costPriceString = CommonUtility.getDataFieldValue(datafields, ApplicationConstant.MNS_GROSS_COST_PRICE_FIELD);
        String sellingPriceString = CommonUtility.getDataFieldValue(datafields, ApplicationConstant.SELLING_PRICE_FIELD);
        String currencyString = CommonUtility.getDataFieldValue(datafields, ApplicationConstant.AGREED_COST_PRICE_CURRENCY_FIELD);
        String margin = null;
        if (!StringUtils.isEmpty(costPriceString) && !StringUtils.isEmpty(sellingPriceString) && !StringUtils.isEmpty(currencyString)) {
            double costPrice = Util.convertStringToDouble(costPriceString);
            double sellingPrice = Util.convertStringToDouble(sellingPriceString);
            double exchangeRate = Util.getExchangeRate(currencyString);
            if (0 != (int) sellingPrice) {
                double calculatedMargin = (sellingPrice - costPrice * exchangeRate) / sellingPrice;
                margin = df.format(calculatedMargin);
            } else {
                throw new PALServiceException(ErrorCode.INVALID_NUMBER);
            }
        }

        autoCalculatedFields.add(CommonUtility.createDataField(fieldKey, margin));

        List<DataField> mergedDataFields = mergedDataFields(autoCalculatedFields, datafields);
        autoCalculatedFields.addAll(getAutoCalculatedField(fieldKey, mergedDataFields, palFields));
        return autoCalculatedFields;
    }

    private static List<DataField> calculateTrayCostValue(List<DataField> datafields, List<PALFields> palFields) {
        String fieldKey = ApplicationConstant.TRAY_COST_VALUE_FIELD;
        List<DataField> autoCalculatedFields = new ArrayList<>();
        String costPriceString = CommonUtility.getDataFieldValue(datafields, ApplicationConstant.MNS_GROSS_COST_PRICE_FIELD);
        String uptString = CommonUtility.getDataFieldValue(datafields, ApplicationConstant.UPT_FIELD);
        String trayCostValue = null;
        if (!StringUtils.isEmpty(costPriceString) && !StringUtils.isEmpty(uptString)) {
            double costPrice = Util.convertStringToDouble(costPriceString);
            double upt = Util.convertStringToDouble(uptString);

            double calculatedTrayCostValue = costPrice * upt;
            trayCostValue = Double.toString(calculatedTrayCostValue);
        }

        autoCalculatedFields.add(CommonUtility.createDataField(fieldKey, trayCostValue));

        List<DataField> mergedDataFields = mergedDataFields(autoCalculatedFields, datafields);
        autoCalculatedFields.addAll(getAutoCalculatedField(fieldKey, mergedDataFields, palFields));
        return autoCalculatedFields;
    }

    private static List<DataField> calculateTraySellingValue(List<DataField> datafields, List<PALFields> palFields) {
        String fieldKey = ApplicationConstant.TRAY_SELLING_VALUE_FIELD;
        List<DataField> autoCalculatedFields = new ArrayList<>();
        String sellingPriceString = CommonUtility.getDataFieldValue(datafields, ApplicationConstant.SELLING_PRICE_FIELD);
        String uptString = CommonUtility.getDataFieldValue(datafields, ApplicationConstant.UPT_FIELD);
        String traySellingValue = null;
        if (!StringUtils.isEmpty(sellingPriceString) && !StringUtils.isEmpty(uptString)) {
            double sellingPrice = Util.convertStringToDouble(sellingPriceString);
            double upt = Util.convertStringToDouble(uptString);
            double calculatedTraySellingValue = sellingPrice * upt;
            traySellingValue = Double.toString(calculatedTraySellingValue);
        }

        autoCalculatedFields.add(CommonUtility.createDataField(fieldKey, traySellingValue));

        List<DataField> mergedDataFields = mergedDataFields(autoCalculatedFields, datafields);
        autoCalculatedFields.addAll(getAutoCalculatedField(fieldKey, mergedDataFields, palFields));
        return autoCalculatedFields;
    }

    public static void findCalculatableFields(String fieldId, List<PALFields> palFields, Set<String> calculatablefields) {
        PALFields palField = CommonUtility.getPALFieldById(fieldId, palFields);
        if (!Objects.isNull(palField)) {
            calculatablefields.add(fieldId);
            List<String> fields = palField.getScripts();
            if (!CollectionUtils.isEmpty(fields)) {
                fields.forEach(field -> findCalculatableFields(field, palFields, calculatablefields));
            }
        }
    }
}
