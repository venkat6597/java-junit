package com.marksandspencer.foodshub.pal.serviceImpl;

import com.marksandspencer.foodshub.pal.constant.ApplicationConstant;
import com.marksandspencer.foodshub.pal.constant.ErrorCode;
import com.marksandspencer.foodshub.pal.constant.ExcelConstant;
import com.marksandspencer.foodshub.pal.dao.AzureStorageDao;
import com.marksandspencer.foodshub.pal.domain.PALProject;
import com.marksandspencer.foodshub.pal.domain.PALRole;
import com.marksandspencer.foodshub.pal.exception.PALServiceException;
import com.marksandspencer.foodshub.pal.service.ExportHelper;
import com.marksandspencer.foodshub.pal.transfer.*;
import com.marksandspencer.foodshub.pal.util.Util;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Service
@Slf4j
public class ExportHelperImpl implements ExportHelper {

    @Autowired
    AzureStorageDao azureStorageDao;

    private XSSFCellStyle headerCellStyle;
    private XSSFCellStyle dataCellStyle;

    @Override
    public ByteArrayResource createExcel(List<PALProductResponse> palProductResponses, List<PALRole> roles
            , PALProject palProject) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            dataCellStyle = getDataStyle(workbook);
            headerCellStyle = getHeaderStyle(workbook, 10);
            AtomicInteger rowCount = new AtomicInteger(0);

            // Project sheet
            XSSFSheet projectSheet = workbook.createSheet(ExcelConstant.PROJECT);
            List<String> projectHeaders = new ArrayList<>();
            Collections.addAll(projectHeaders, ExcelConstant.PROJECT_HEADER);
            List<String> projectData = new ArrayList<>();
            Collections.addAll(projectData,palProject.getProjectName(), palProject.getTemplateName()
                    , palProject.getStatus(), palProject.getProjectType(), palProject.getFinancialYear(),
                    palProject.getProjectCompletionDate().toString(), palProject.getComments());
            Stream.concat(!CollectionUtils.isEmpty(palProject.getPersonnel().getInternal()) ?
                            palProject.getPersonnel().getInternal().stream() : Stream.empty(),
                    !CollectionUtils.isEmpty(palProject.getPersonnel().getExternal()) ?
                            palProject.getPersonnel().getExternal().stream() : Stream.empty()).forEach(user -> {
                projectHeaders.add(getRoleName(roles, user.getRole()));
                if (!ObjectUtils.isEmpty(user.getUsers()))
                    projectData.add(StringUtils.join(user.getUsers(), ", \n"));
            });
            createRow(rowCount, projectSheet, projectHeaders, headerCellStyle);
            createRow(rowCount, projectSheet, projectData, dataCellStyle);
            adjustColumn(projectSheet, projectHeaders.size());

            if (!CollectionUtils.isEmpty(palProductResponses)) {
                // Product sheet
                XSSFSheet productSheet = workbook.createSheet(ExcelConstant.PRODUCT);
                rowCount.set(0);
                List<List<ProductSection>> products = getProductData(palProductResponses);
                createProductSheetHeaderRow(productSheet, products.get(0), roles, rowCount);
                products.forEach(product -> createProductSheetDataRow(productSheet, product, rowCount));
                adjustPalSheetColumn(productSheet, products.get(0));

                List<ProductSubSection> multiChilds = getProductSubSections(palProductResponses
                        , ApplicationConstant.PRODUCT_FILE_TYPE_FIELD);
                if (!multiChilds.isEmpty()) {
                    XSSFSheet multipackChildren = workbook.createSheet(ExcelConstant.MULTIPACK_CHILDREN_SHEET);
                    rowCount.set(0);
                    AtomicInteger cellCount = new AtomicInteger(0);
                    XSSFRow row = multipackChildren.createRow(rowCount.intValue());
                    List<ProductField> subfields = multiChilds.get(0).getSubfields();
                    subfields.forEach(productField -> {
                        createCell(row, cellCount, productField.getLabel(), headerCellStyle);
                        cellCount.incrementAndGet();
                    });
                    rowCount.incrementAndGet();
                    createChildDataRow(rowCount, multipackChildren, multiChilds);
                    adjustColumn(multipackChildren, subfields.size());
                }

                // Create Multiple Artwork for Product
                List<ProductSubSection> multipleArtworks = getProductSubSections(palProductResponses
                        , ApplicationConstant.MULTIPLE_ARTWORKS_FIELD);
                if (!multipleArtworks.isEmpty()) {
                    XSSFSheet multipackChildren = workbook.createSheet(ExcelConstant.MULTI_ARTWORK_SHEET);
                    rowCount.set(0);
                    AtomicInteger cellCount = new AtomicInteger(0);
                    XSSFRow row = multipackChildren.createRow(rowCount.intValue());
                    List<ProductField> subfields = multipleArtworks.get(0).getSubfields();
                    subfields.forEach(productField -> {
                        createCell(row, cellCount, productField.getLabel(), headerCellStyle);
                        cellCount.incrementAndGet();
                    });
                    rowCount.incrementAndGet();
                    createChildDataRow(rowCount, multipackChildren, multipleArtworks);
                    adjustColumn(multipackChildren, subfields.size());
                }
            }
            workbook.write(out);
            return new ByteArrayResource(out.toByteArray());
        } catch (Exception ex) {
            log.error("ExportHelperImpl > createExcel() :: {}", ex.getMessage());
            throw new PALServiceException(ErrorCode.DOWNLOAD_FAILED);
        }
    }

    private List<ProductSubSection> getProductSubSections(List<PALProductResponse> palProductResponses, String sectionName) {
        List<ProductSubSection> subSections = new ArrayList<>();
        palProductResponses.forEach(palProductResponse ->
            subSections.addAll(palProductResponse.getSections().stream()
                    .filter(productSection -> productSection.getName().equalsIgnoreCase(sectionName))
                    .flatMap(productSection -> productSection.getFields().stream())
                    .filter(productField -> productField.getSubSections() != null)
                    .flatMap(productField -> productField.getSubSections().stream())
                    .map(productSubSection -> {
                        List<ProductField> subfields = new ArrayList<>();
                        if(sectionName.equals(ApplicationConstant.PRODUCT_FILE_TYPE_FIELD)){
                            subfields = productSubSection.getSubfields().stream()
                                    .sorted(Comparator.comparing(ProductField::getName)).collect(Collectors.toList());
                            subfields.add(ProductField.builder().name(ExcelConstant.PARENT_UPC).label(ExcelConstant.PARENT_UPC_LABEL)
                                    .value(palProductResponse.getHeader().getUpc()).build());
                            subfields.add(ProductField.builder().name(ExcelConstant.PARENT_TITLE).label(ExcelConstant.PARENT_TITLE_LABEL)
                                    .value(palProductResponse.getHeader().getProductName()).build());
                        } else if(sectionName.equals(ApplicationConstant.MULTIPLE_ARTWORKS_FIELD)){
                            subfields.add(ProductField.builder().name(ExcelConstant.PARENT_UPC).label(ExcelConstant.PARENT_UPC_LABEL)
                                    .value(palProductResponse.getHeader().getUpc()).build());
                            subfields.add(ProductField.builder().name(ExcelConstant.PARENT_TITLE).label(ExcelConstant.PARENT_TITLE_LABEL)
                                    .value(palProductResponse.getHeader().getProductName()).build());
                            subfields.add(ProductField.builder().name(ExcelConstant.SUPPLIER_NAME).label(ExcelConstant.SUPPLIER_NAME_LABEL)
                                    .value(palProductResponse.getHeader().getSupplierName()).build());
                            subfields.add(ProductField.builder().name(ExcelConstant.SUPPLIER_SITE_CODE).label(ExcelConstant.SUPPLIER_SITE_CODE_LABEL)
                                    .value(palProductResponse.getHeader().getSupplierCode()).build());
                            subfields.addAll(productSubSection.getSubfields().stream()
                                    .sorted(Comparator.comparing(ProductField::getName)).collect(Collectors.toList()));
                        }
                        if (validateSubFields(subfields))
                            productSubSection.setSubfields(subfields);
                        return productSubSection;
                    }).collect(Collectors.toList())));
        return subSections;
    }

    private boolean validateSubFields(List<ProductField> subfields) {
        List<ProductField> fields = subfields.stream()
                .filter(field -> !StringUtils.isEmpty(field.getValue()) &&
                        !field.getName().contains("parent")).collect(Collectors.toList());
        return !CollectionUtils.isEmpty(fields);
    }

    private void createRow(AtomicInteger rowCount, XSSFSheet projectSheet, List<String> data, XSSFCellStyle cellStyle) {
        AtomicInteger cellCount = new AtomicInteger(0);
        XSSFRow row = projectSheet.createRow(rowCount.intValue());
        data.forEach(value -> {
            createCell(row, cellCount,value, cellStyle);
            cellCount.incrementAndGet();
        });
        rowCount.incrementAndGet();
    }

    private List<List<ProductSection>> getProductData(List<PALProductResponse> productResponses) {
        return productResponses.stream().map(PALProductResponse::getSections)
                .collect(Collectors.toList());
    }

    private void createProductSheetHeaderRow(XSSFSheet palSheet, List<ProductSection> product, List<PALRole> roles
            , AtomicInteger rowCount) {
        XSSFRow firstRow = palSheet.createRow(rowCount.intValue());
        rowCount.incrementAndGet();
        XSSFRow secondRow = palSheet.createRow(rowCount.intValue());
        rowCount.incrementAndGet();
        AtomicInteger cellCount = new AtomicInteger(0);
        product.forEach(productSection ->
            productSection.getFields().forEach(productField -> {
                XSSFCellStyle cellStyle = getHeaderStyle(palSheet.getWorkbook()
                        , getRoleColor(roles, productField.getOwner()));
                createCell(firstRow, cellCount, getRoleName(roles, productField.getOwner()), cellStyle);
                createCell(secondRow, cellCount, productField.getLabel(), cellStyle);
                cellCount.incrementAndGet();
            }));
    }

    private String getRoleName(List<PALRole> roles, String role) {
        Optional<PALRole> palRol = roles.stream().filter(palRole -> palRole.getRole().equals(role)).findFirst();
        if (palRol.isPresent()) {
            return palRol.get().getName();
        }
        return "";
    }

    private int getRoleColor(List<PALRole> roles, String role) {
        for(int index = 0; index < roles.size(); index++){
            if (roles.get(index).getRole().equals(role)) {
                return index;
            }
        }
        return 10;
    }

    private void createProductSheetDataRow(XSSFSheet palSheet, List<ProductSection> product, AtomicInteger rowCount) {
        XSSFRow dataRow = palSheet.createRow(rowCount.intValue());
        rowCount.incrementAndGet();
        AtomicInteger cellCount = new AtomicInteger(0);
        product.forEach(productSection ->
            productSection.getFields().forEach(productField -> {
                createCell(dataRow, cellCount, productField.getValue(), dataCellStyle);
                cellCount.incrementAndGet();
            })
        );
    }

    private void createChildDataRow(AtomicInteger rowCount, XSSFSheet multipackChildren, List<ProductSubSection> subSections) {
        subSections.forEach(productSubSection -> {
            XSSFRow row = multipackChildren.createRow(rowCount.intValue());
            AtomicInteger cellCount = new AtomicInteger(0);
            productSubSection.getSubfields().forEach(productField -> {
                createCell(row, cellCount, productField.getValue(), dataCellStyle);
                cellCount.incrementAndGet();
            });
            rowCount.incrementAndGet();
        });
    }

    private void createCell(XSSFRow row, AtomicInteger cellCount, String value, XSSFCellStyle cellStyle) {
        XSSFCell firstRowCell = row.createCell(cellCount.intValue());
        firstRowCell.setCellValue(value);
        firstRowCell.setCellStyle(cellStyle);
    }

    private void adjustPalSheetColumn(XSSFSheet palSheet, List<ProductSection> product) {
        AtomicInteger columnCount = new AtomicInteger(0);
        product.forEach(productSection -> columnCount.addAndGet(productSection.getFields().size()));
        adjustColumn(palSheet, columnCount.get());
    }

    private void adjustColumn(XSSFSheet palSheet, int columnCount) {
        IntStream.range(0, columnCount).forEach(column -> palSheet.autoSizeColumn(column));
    }

    private XSSFCellStyle getHeaderStyle(XSSFWorkbook workbook, int colorIndex) {
        XSSFCellStyle cellStyle = getHeaderStyle(workbook);
        cellStyle.setFillForegroundColor(getColor(workbook, colorIndex));
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return cellStyle;
    }

    private XSSFColor getColor(XSSFWorkbook workbook, int value){
        IndexedColorMap colorMap = workbook.getStylesSource().getIndexedColors();
        switch (value){
            case 0:
                return  new XSSFColor(new byte[]{(byte)248, (byte)203, (byte)173}, colorMap);
            case 1:
                return  new XSSFColor(new byte[]{(byte)255, (byte)230, (byte)153}, colorMap);
            case 2:
                return  new XSSFColor(new byte[]{(byte)155, (byte)193, (byte)230}, colorMap);
            case 3:
                return  new XSSFColor(new byte[]{(byte)198, (byte)224, (byte)180}, colorMap);
            case 4:
                return  new XSSFColor(new byte[]{(byte)255, (byte)139, (byte)139}, colorMap);
            case 5:
                return  new XSSFColor(new byte[]{(byte)255, (byte)197, (byte)255}, colorMap);
            case 6:
                return  new XSSFColor(new byte[]{(byte)201, (byte)166, (byte)228}, colorMap);
            case 7:
                return  new XSSFColor(new byte[]{(byte)150, (byte)188, (byte)156}, colorMap);
            case 8:
                return  new XSSFColor(new byte[]{(byte)122, (byte)205, (byte)216}, colorMap);
            case 9:
                return  new XSSFColor(new byte[]{(byte)217, (byte)211, (byte)175}, colorMap);
            default:
                return  new XSSFColor(new byte[]{(byte)192, (byte)192, (byte)192}, colorMap);
        }
    }

    private XSSFCellStyle getHeaderStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setFontHeightInPoints((short) 10);
        font.setFontName(ExcelConstant.FONT);
        font.setColor(IndexedColors.BLACK.getIndex());
        font.setBold(true);
        font.setItalic(false);
        setTextAlign(style);
        setBorder(style);
        style.setFont(font);
        return style;
    }

    private XSSFCellStyle getDataStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        XSSFFont font = workbook.createFont();
        font.setFontHeightInPoints((short) 12);
        font.setFontName(ExcelConstant.FONT);
        font.setColor(IndexedColors.BLACK.getIndex());
        font.setBold(false);
        font.setItalic(false);
        setTextAlign(style);
        setBorder(style);
        style.setFont(font);
        return style;
    }

    private void setTextAlign(XSSFCellStyle style) {
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
    }

    private void setBorder(XSSFCellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderTop(BorderStyle.THIN);
        style.setTopBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderLeft(BorderStyle.THIN);
        style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
        style.setBorderRight(BorderStyle.THIN);
        style.setRightBorderColor(IndexedColors.BLACK.getIndex());
    }

    @Override
    public ByteArrayResource createExcelwithMacro(String userRole, List<PALProductResponse> palProductResponses, List<PALRole> roles
            , PALProject palProject, String fileName) {
        File downloadFile;
        try {
            String blobFileName = fileName + ApplicationConstant.DOWNLOAD_MACRO_EXCEL_EXTENSION;
            downloadFile = azureStorageDao.downloadBlobToFile(blobFileName, File.createTempFile(fileName, ApplicationConstant.DOWNLOAD_MACRO_EXCEL_EXTENSION));

            log.info("DownloadFile name :: {}" , downloadFile.getAbsolutePath());

            XSSFWorkbook workbook = new XSSFWorkbook(OPCPackage.open(downloadFile.getAbsolutePath()));

            updateHomePage(workbook, palProject);
            if (!CollectionUtils.isEmpty(palProductResponses)) {
                updatePALPage(workbook, palProject, palProductResponses);
                updateSubSectionPage(workbook, palProductResponses, ExcelConstant.MULTIPACK_CHILDREN_SHEET, ApplicationConstant.PRODUCT_FILE_TYPE_FIELD, ApplicationConstant.PRODUCT_FILE_TYPE_FIELD,
                        ApplicationConstant.PARENT, ExcelConstant.MULTIPACK_FIELD_SETTING_ROWNO);
                updateSubSectionPage(workbook, palProductResponses, ExcelConstant.MULTI_ARTWORK_SHEET, ApplicationConstant.MULTIPLE_ARTWORKS_FIELD, ApplicationConstant.PRINTED_PACKAGING_TYPE,
                        ApplicationConstant.MULTIPLE, ExcelConstant.MULTIARTWORK_FIELD_SETTING_ROWNO);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            workbook.close();
            return new ByteArrayResource(out.toByteArray());
        } catch (IOException | InvalidFormatException e) {
            log.error("ExportHelperImpl > createExcel() :: {}", e);
            throw new PALServiceException(ErrorCode.DOWNLOAD_FAILED);
        }
    }

    private void updateSubSectionPage(XSSFWorkbook workbook, List<PALProductResponse> palProductResponses, String sheetName,
                                      String sectionId, String subSectionFieldId, String subSectionFieldValue, Integer settingsRowNo) {
        XSSFSheet page = workbook.getSheet(sheetName);
        XSSFCellStyle cellStyle = workbook.createCellStyle();
        AtomicReference<Integer> rowNo = new AtomicReference<>(2);
        Map<Integer, String> dbFields = getDBFieldSettingMapping(workbook, settingsRowNo);
        Integer productFieldsColumnStart = ApplicationConstant.PRODUCT_FILE_TYPE_FIELD.equalsIgnoreCase(subSectionFieldId) ? 2 : 4;
        for (PALProductResponse palProductResponse : palProductResponses) {
            List<ProductField> productFields = getProductFields(palProductResponse, null).stream().distinct().collect(Collectors.toList());
            Map<String,String> productFieldValues = new HashMap<>();
            for (ProductField field : productFields) {
                productFieldValues.put(field.getName(), field.getValue());
            }
            List<ProductField> productSectionFields = getProductFields(palProductResponse, sectionId).stream().distinct().collect(Collectors.toList());
            ProductField productField = productSectionFields.stream()
                    .filter(field -> field.getName().equalsIgnoreCase(subSectionFieldId))
                    .findFirst().orElse(null);

            if (!ObjectUtils.isEmpty(productField) && subSectionFieldValue.equalsIgnoreCase(productField.getValue())) {
                List<ProductSubSection> productSubSections = productField.getSubSections();
                if (!CollectionUtils.isEmpty(productSubSections)) {
                    productSubSections.forEach(productSubSection -> {
                        List<ProductField> productSubFields = productSubSection.getSubfields().stream()
                                .filter(field -> !StringUtils.isEmpty(field.getValue())).collect(Collectors.toList());
                        if (!CollectionUtils.isEmpty(productSubFields)) {
                            Map<String, String> productSubFieldValues = productSubFields.stream().
                                    collect(Collectors.toMap(ProductField::getName, ProductField::getValue));

                            XSSFRow row = page.getRow(rowNo.get());
                            if (ObjectUtils.isEmpty(row)) {
                                row = page.createRow(rowNo.get());
                            }
                            for (Map.Entry<Integer, String> entry : dbFields.entrySet()) {
                                Integer cellNo = entry.getKey();
                                String fieldId = entry.getValue();
                                String cellValue;
                                if (cellNo <=productFieldsColumnStart) {
                                    // get details from product main section
                                    cellValue = productFieldValues.getOrDefault(fieldId, null);
                                } else {
                                    // get details from product sub sections
                                    cellValue = productSubFieldValues.getOrDefault(fieldId, null);
                                }
                                createCell(row, cellNo, cellValue, cellStyle);
                            }
                            rowNo.set(rowNo.get()+1);
                        }
                    });
                }
            }
        }
    }

    private List<ProductField> getProductFields(PALProductResponse palProductResponse, String sectionId) {
        List<ProductField> productFields = new ArrayList<>();
        if (!StringUtils.isEmpty(sectionId)) {
            ProductSection productSection = palProductResponse.getSections().stream().filter(section -> section.getName().equalsIgnoreCase(sectionId))
                    .findFirst().orElse(null);
            if (!ObjectUtils.isEmpty(productSection) && !CollectionUtils.isEmpty(productSection.getFields())) {
                productFields = productSection.getFields();
            }
        } else {
            productFields = palProductResponse.getSections().stream()
                    .flatMap(section -> section.getFields().stream())
                    .filter(field -> !StringUtils.isEmpty(field.getValue()))
                    .collect(Collectors.toList());
        }
        return productFields;
    }

    private void updatePALPage(XSSFWorkbook workbook, PALProject palProject, List<PALProductResponse> palProductResponses) {
        Map<Integer, String> dbFields = getDBFieldSettingMapping(workbook, ExcelConstant.PAL_FIELD_SETTING_ROWNO);
        XSSFSheet palPage = workbook.getSheet(ExcelConstant.PAL_SHEET);
        XSSFCellStyle cellStyle = workbook.createCellStyle();
        XSSFCell projectNameCell = palPage.getRow(1).getCell(6);
        projectNameCell.setCellValue(palProject.getProjectName());
        Integer rowNo = 5;
        for (PALProductResponse palProductResponse : palProductResponses) {
            Map<Integer, String> palFielddbValues = getPALProductDBValues(palProductResponse, dbFields);
            XSSFRow row = palPage.getRow(rowNo);
            if (ObjectUtils.isEmpty(row)) {
                row = palPage.createRow(rowNo);
            }
            for (Map.Entry<Integer, String> entry : palFielddbValues.entrySet()) {
                Integer cellNo = entry.getKey();
                String cellValue = entry.getValue();
                createCell(row, cellNo, cellValue, cellStyle);
            }
            rowNo = rowNo + 1;
        }
    }

    private void createCell(XSSFRow row, Integer cellNo, String cellValue, XSSFCellStyle cellStyle) {
        setBorder(cellStyle);
        XSSFCell cell = row.createCell(cellNo);
        cell.setCellValue(cellValue);
        cell.setCellStyle(cellStyle);
    }

    private Map<Integer, String> getPALProductDBValues(PALProductResponse palProductResponse, Map<Integer, String> dbFields) {
        Map<Integer, String> palFielddbValues = new HashMap<>();
        Map<String, List<String>> personnelDetails = getPersonnelDetails(palProductResponse.getPersonnel());
        List<ProductField> productFields = getProductFields(palProductResponse, null);
        Map<String, String> productFieldValues = new HashMap<>();
        for (ProductField field : productFields) {
            productFieldValues.put(field.getName(), field.getValue());
        }

        for (Map.Entry<Integer, String> entry : dbFields.entrySet()) {
            Integer columnNo = entry.getKey();
            String dbField = entry.getValue();
            String dbFieldValue = productFieldValues.getOrDefault(dbField, null);
            if (StringUtils.isEmpty(dbFieldValue)) {
                List<String> users = personnelDetails.getOrDefault(dbField, null);
                if (!CollectionUtils.isEmpty(users))
                    dbFieldValue = StringUtils.join(users, ", \n");
            }
            palFielddbValues.put(columnNo, dbFieldValue);
        }
        return palFielddbValues;
    }

    private Map<String, List<String>> getPersonnelDetails(Personnel personnel) {
        Map<String, List<String>> personnelDetails = new HashMap<>();
        List<User> internalUsers = personnel.getInternal();
        List<User> externalUsers = personnel.getExternal();
        if (!CollectionUtils.isEmpty(internalUsers))
            internalUsers.forEach(user -> personnelDetails.put(user.getRole(), user.getUsers()));
        if (!CollectionUtils.isEmpty(externalUsers))
            externalUsers.forEach(user -> personnelDetails.put(user.getRole(), user.getUsers()));

        return personnelDetails;
    }

    private Map<Integer, String> getDBFieldSettingMapping(XSSFWorkbook workbook, Integer rowNo) {
        Map<Integer, String> dbFields = new HashMap<>();
        XSSFSheet settings = workbook.getSheet(ExcelConstant.SETTING_SHEET);
        XSSFRow dbFieldRow = settings.getRow(rowNo);
        Integer cellCount = 1;
        boolean endOfCell = false;
        while (!endOfCell) {
            XSSFCell cell = dbFieldRow.getCell(cellCount);
            String cellValue = null;
            if (!ObjectUtils.isEmpty(cell)) {
                cellValue = cell.getStringCellValue();
            }
            
            if (!StringUtils.isEmpty(cellValue)) {
                dbFields.put(cellCount, cellValue);
                cellCount = cellCount + 1;
            } else {
                endOfCell = true;
            }
        }
        return dbFields;
    }

    private void updateHomePage(XSSFWorkbook workbook, PALProject palProject) {
        XSSFSheet homePage = workbook.getSheet(ExcelConstant.HOMEPAGE_SHEET);
        if (!StringUtils.isEmpty(palProject.getProjectName()))
            homePage.getRow(5).getCell(6).setCellValue(palProject.getProjectName());
        if (!StringUtils.isEmpty(palProject.getStatus()))
            homePage.getRow(6).getCell(6).setCellValue(palProject.getStatus());
        if (!StringUtils.isEmpty(palProject.getFinancialYear()))
            homePage.getRow(7).getCell(6).setCellValue(palProject.getFinancialYear());
        if (!ObjectUtils.isEmpty(palProject.getProjectCompletionDate()))
            homePage.getRow(8).getCell(6).setCellValue(Util.convertLocalDateTimeToString(palProject.getProjectCompletionDate()));
        if (!StringUtils.isEmpty(palProject.getProjectType()))
            homePage.getRow(9).getCell(6).setCellValue(palProject.getProjectType());
        if (!StringUtils.isEmpty(palProject.getComments()))
            homePage.getRow(10).getCell(6).setCellValue(palProject.getComments());
    }
}
