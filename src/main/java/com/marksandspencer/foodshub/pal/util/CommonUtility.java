package com.marksandspencer.foodshub.pal.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.util.ObjectUtils;

import com.marksandspencer.assemblyservice.config.transfer.AccessControlInfo;
import com.marksandspencer.foodshub.pal.constant.ApplicationConstant;
import com.marksandspencer.foodshub.pal.domain.DataField;
import com.marksandspencer.foodshub.pal.domain.PALFields;
import com.marksandspencer.foodshub.pal.domain.PALRole;
import com.marksandspencer.foodshub.pal.transfer.BulkProductUpdateRequest;
import com.marksandspencer.foodshub.pal.transfer.FieldUpdate;
import com.marksandspencer.foodshub.pal.transfer.ProductUpdate;

public class CommonUtility {
    public static Map<String, List<DataField>> convertBulkRequestToMapObject(BulkProductUpdateRequest bulkProductUpdateRequest) {
        return bulkProductUpdateRequest.getProducts().stream()
                .collect(Collectors.toMap(ProductUpdate::getProductId, product->convertFieldUpdateToDataField(product.getFieldUpdates())));
    }

    public static List<DataField> convertFieldUpdateToDataField(List<FieldUpdate> fieldUpdates) {
        List<DataField> dataFields = new ArrayList<>();
        fieldUpdates.forEach(field ->
                dataFields.add(DataField.builder().fieldId(field.getField()).fieldValue(field.getNewValue()).build()));
        return dataFields;
    }

    public static String getDataFieldValue(List<DataField> dataFields, String field) {
        DataField result = getDataField(dataFields, field);
        return !ObjectUtils.isEmpty(result) ? result.getFieldValue() : null;
    }

    public static DataField getDataField(List<DataField> dataFields, String field) {
        return dataFields.stream().filter(dataField ->
                dataField.getFieldId().equals(field))
                .findFirst().orElse(null);
    }

    public static PALFields getPALFieldById(String field, List<PALFields> palFields) {
        return palFields.stream().filter(palField -> palField.getId().equals(field))
                .findFirst().orElse(null);
    }

    public static String getFieldDetails(PALFields field, String fieldLabel) {
        String fieldValue = null;
        switch (fieldLabel) {
            case ApplicationConstant.FIELD_LABEL:
                fieldValue = field.getLabel();
                break;
            default :
                break;
        }
        return fieldValue;
    }

    public static String getUserRolelabel(String userRole, List<PALRole> roles) {
        String roleLabel = userRole;
        PALRole palRole = roles.stream().filter(role -> userRole.equals(role.getRole())).findFirst().orElse(null);
        if (!ObjectUtils.isEmpty(palRole)) {
            roleLabel = palRole.getName();
        }
        return roleLabel;
    }

    public static DataField createDataField(String fieldKey, String fieldValue) {
        DataField dataField = new DataField();
        dataField.setFieldId(fieldKey);
        dataField.setFieldValue(fieldValue);
        return dataField;
    }

    public static boolean isNonEditableField(PALFields field, String userRole, AccessControlInfo accessInfo) {
        return ObjectUtils.isEmpty(accessInfo) || !accessInfo.isUpdateAccess() || Objects.isNull(field.getEditable()) ||
                !field.getEditable().contains(userRole) &&
                        (ApplicationConstant.SUPPLIER.equalsIgnoreCase(userRole) || !field.getEditable().contains(ApplicationConstant.MNS));
    }

}
