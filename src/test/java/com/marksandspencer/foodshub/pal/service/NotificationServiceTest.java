package com.marksandspencer.foodshub.pal.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marksandspencer.foodshub.pal.constant.ApplicationConstant;
import com.marksandspencer.foodshub.pal.constant.MessageTemplate;
import com.marksandspencer.foodshub.pal.constant.PALProjectConstants;
import com.marksandspencer.foodshub.pal.constant.Status;
import com.marksandspencer.foodshub.pal.dao.ProductAttributeListingDao;
import com.marksandspencer.foodshub.pal.domain.PALConfiguration;
import com.marksandspencer.foodshub.pal.domain.PALProduct;
import com.marksandspencer.foodshub.pal.domain.PALProject;
import com.marksandspencer.foodshub.pal.domain.PALRole;
import com.marksandspencer.foodshub.pal.dto.PALUser;
import com.marksandspencer.foodshub.pal.event.KafkaMessageProducer;
import com.marksandspencer.foodshub.pal.serviceImpl.NotificationServiceImpl;
import com.marksandspencer.foodshub.pal.transfer.AppResponse;
import com.marksandspencer.foodshub.pal.util.Util;
import com.marksandspencer.foodshub.pal.utility.TestUtility;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NotificationServiceTest {

    @InjectMocks
    NotificationService  notificationService = new NotificationServiceImpl();

    @Mock
    ProductAttributeListingDao palDao;

    @Mock
    UserService userService;

    @Mock
    KafkaMessageProducer kafkaMessageProducer = new KafkaMessageProducer();

    @Captor
    ArgumentCaptor<Map<String, Object>> messageCaptor;

    Map<String, List<PALUser>> palUsersForRoles;
    List<PALConfiguration> palConfigurations;
    PALConfiguration palConfiguration;
    List<PALRole> palRoles;

    @Before
    public void beforeTest() throws IOException {
        ReflectionTestUtils.setField(notificationService, "messageTopic", "testTopic");
        ReflectionTestUtils.setField(notificationService, "projectUrl", "https://localhost/home/products/new-product-development/%s");
        ReflectionTestUtils.setField(notificationService, "productUrl", "https://localhost/home/products/new-product-development/%s/product/%s");
        ReflectionTestUtils.setField(notificationService, "fromId", "fromId@xyz.com");

        ObjectMapper mapper = new ObjectMapper();
        AppResponse<List<PALRole>> response = mapper.readValue(new File("src/test/resources/ProductAttributeListingResponse/ListRolesResponse.json"),
                new TypeReference<>() {});
        palRoles = response.getData();
        when(palDao.findAllPALRoles()).thenReturn(palRoles);

        AppResponse<Map<String, List<PALUser>>> userlistresponse = mapper.readValue(new File("src/test/resources/Notification/ListUsersForRolesResponse.json"),
                new TypeReference<>() {});
        palUsersForRoles = userlistresponse.getData();

        palConfigurations = mapper.readValue(new File("src/test/resources/ProductAttributeListingResponse/db_PALConfiguration.json"),
                new TypeReference<>() {});
        palConfiguration = palConfigurations.stream().filter(config -> config.getId().equalsIgnoreCase(ApplicationConstant.Notification.NOTIFICATION_CONFIG))
                .findFirst().orElse(null);
        when(palDao.findPALConfigurationById(eq(ApplicationConstant.Notification.NOTIFICATION_CONFIG))).thenReturn(palConfiguration);
    }

    @Test
    public void sendEmailMessageAddProjectTest() {

        MessageTemplate messageTemplate = MessageTemplate.ADD_PROJECT;
        String triggerConfigId = messageTemplate.getTemplateId() + ApplicationConstant.Notification.USERROLES;

        String fileName = "src/test/resources/Notification/PALNotification.json";
        TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
        };
        Map<String, Object> notificationDetails = TestUtility.readFile(fileName, typeReference);

        PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) notificationDetails.getOrDefault("palProject", null),
                new TypeReference<PALProject>() {});

        mockUserServiceListUserByRoles(triggerConfigId);

        notificationService.sendEmailMessage(palProject, null, messageTemplate);

        Mockito.verify(kafkaMessageProducer,Mockito.times(1)).sendMessage(messageCaptor.capture(), Mockito.anyString());
        Map<String, Object> message = messageCaptor.getValue();
        assertNotNull(message);
        assertEquals(ApplicationConstant.OFP.toUpperCase(), message.get(ApplicationConstant.Notification.PUBLISHER));
        assertEquals(ApplicationConstant.PAL, message.get(ApplicationConstant.Notification.EVENT_MODULE));
        assertEquals(messageTemplate.getEventName(), message.get(ApplicationConstant.Notification.EVENT_NAME));

        Set<Map<String, String>> userList = (Set<Map<String, String>>) message.getOrDefault(ApplicationConstant.Notification.USERS, null);
        assertNotNull(userList);
        assertEquals(1, userList.size());
        //project level user
        assertTrue(userList.contains(Map.of("userId", "productDeveloper1@xyz.com", "status", "READ", "type", "EMAIL", "mailType", "TO")));

        Map<String, Object> data = (Map<String, Object>) message.getOrDefault(ApplicationConstant.Notification.DATA, null);
        assertNotNull(data);

        Map<String, Object> emailMessage = (Map<String, Object>) data.getOrDefault(ApplicationConstant.Notification.EMAIL_MESSAGE, null);
        assertNotNull(emailMessage);
        assertEquals(String.format(messageTemplate.getMessageSubject(), palProject.getProjectName()), emailMessage.get(ApplicationConstant.Notification.SUBJECT));
        assertEquals(messageTemplate.getTemplateId(), emailMessage.get(ApplicationConstant.Notification.TEMPLATE_ID));
        assertEquals("fromId@xyz.com",emailMessage.get(ApplicationConstant.Notification.FROM));

        Map<String, String> emailData = (Map<String, String>) emailMessage.getOrDefault(ApplicationConstant.Notification.DATA, null);
        assertNotNull(emailData);
        assertEquals(palProject.getProjectName(), emailData.get(PALProjectConstants.PALProjectFieldNames.PROJECT_NAME));
        assertEquals(palProject.getStatus(), emailData.get(ApplicationConstant.PROJECT_STATUS_FIELD));
        assertEquals(palProject.getFinancialYear(), emailData.get(PALProjectConstants.PALProjectFieldNames.FINANCIAL_YEAR));
        assertEquals(palProject.getProjectType(), emailData.get(PALProjectConstants.PALProjectFieldNames.PROJECT_TYPE));
        assertEquals(palProject.getTemplateName(), emailData.get(PALProjectConstants.PALProjectFieldNames.TEMPLATE_NAME));
        assertEquals(Util.convertLocalDateTimeToString(palProject.getProjectCompletionDate()), emailData.getOrDefault(PALProjectConstants.PALProjectFieldNames.PROJECT_COMPLETION_DATE, null));
        assertTrue(emailData.get(ApplicationConstant.URL).contains(palProject.getId()));
    }

    private void mockUserServiceListUserByRoles(String triggerConfigId) {
        Map<String, Object> notificationConfig= palConfiguration.getMapValues().stream()
                .filter(config -> triggerConfigId.equalsIgnoreCase((String) config.get(ApplicationConstant.Notification.TRIGGER)))
                .findFirst().orElse(null);
        List<String> groupUsers = (List<String>) notificationConfig.get(ApplicationConstant.Notification.GROUP_USERS);
        if (!CollectionUtils.isEmpty(groupUsers)) {
            List<String> palRoleIds = palRoles.stream().filter(role -> groupUsers.contains(role.getRole()))
                    .map(PALRole::getObjectId).collect(Collectors.toList());
            Map<String, List<PALUser>> palGroupUsers = palUsersForRoles.entrySet().stream()
                    .filter(role -> palRoleIds.contains(role.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            when(userService.listUserByRoles(palRoleIds)).thenReturn(palGroupUsers);
        }
    }

    @Test
    public void sendEmailMessageCreativeStageStatusTest() {

        String fileName = "src/test/resources/Notification/PALNotification.json";
        TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
        };
        Map<String, Object> notificationDetails = TestUtility.readFile(fileName, typeReference);

        PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) notificationDetails.getOrDefault("palProject", null),
                new TypeReference<PALProject>() {});
        palProject.setStatus(Status.CREATIVE_STAGE.getStatus());

        PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) notificationDetails.getOrDefault("palProduct", null),
                new TypeReference<PALProduct>() {});
        List<PALProduct> palProducts = new ArrayList<>();
        palProducts.add(palProduct);

        when(palDao.findPALProducts(eq(palProject.getId()), eq(null),eq(null))).thenReturn(palProducts);

        MessageTemplate messageTemplate = MessageTemplate.UPDATE_PROJECT_STATUS;
        String triggerConfigId = messageTemplate.getTemplateId() + palProject.getStatus().replaceAll("[- ]", "") + ApplicationConstant.Notification.USERROLES;
        mockUserServiceListUserByRoles(triggerConfigId);

        notificationService.sendEmailMessage(palProject, null, messageTemplate);

        Mockito.verify(kafkaMessageProducer,Mockito.times(1)).sendMessage(messageCaptor.capture(), Mockito.anyString());
        Map<String, Object> message = messageCaptor.getValue();
        assertNotNull(message);
        assertEquals(ApplicationConstant.OFP.toUpperCase(), message.get(ApplicationConstant.Notification.PUBLISHER));
        assertEquals(ApplicationConstant.PAL, message.get(ApplicationConstant.Notification.EVENT_MODULE));
        assertEquals(messageTemplate.getEventName(), message.get(ApplicationConstant.Notification.EVENT_NAME));

        Set<Map<String, String>> userList = (Set<Map<String, String>>) message.getOrDefault(ApplicationConstant.Notification.USERS, null);
        assertNotNull(userList);

        List<String> userEmails = getUserEmails(userList);

        assertEquals(6, userEmails.size());
        assertTrue(userEmails.contains("projectManager1@xyz.com")); //project user
        assertTrue(userEmails.contains("projectManager2@xyz.com")); //project user
        assertTrue(userEmails.contains("productDeveloper1@xyz.com")); //project user
        assertTrue(userEmails.contains("buyer1@xyz.com")); //project user
        assertTrue(userEmails.contains("foodTechnologist2@xyz.com")); //project user
        assertTrue(userEmails.contains("supplier1@xyz.com")); //product user

        Map<String, Object> data = (Map<String, Object>) message.getOrDefault(ApplicationConstant.Notification.DATA, null);
        assertNotNull(data);

        Map<String, Object> emailMessage = (Map<String, Object>) data.getOrDefault(ApplicationConstant.Notification.EMAIL_MESSAGE, null);
        assertNotNull(emailMessage);
        assertEquals(String.format(messageTemplate.getMessageSubject(), palProject.getProjectName(), palProject.getStatus()), emailMessage.get(ApplicationConstant.Notification.SUBJECT));
        assertEquals(messageTemplate.getTemplateId(), emailMessage.get(ApplicationConstant.Notification.TEMPLATE_ID));
        assertEquals("fromId@xyz.com",emailMessage.get(ApplicationConstant.Notification.FROM));

        Map<String, String> emailData = (Map<String, String>) emailMessage.getOrDefault(ApplicationConstant.Notification.DATA, null);
        assertNotNull(emailData);
        assertEquals(palProject.getProjectName(), emailData.get(PALProjectConstants.PALProjectFieldNames.PROJECT_NAME));
        assertEquals(palProject.getStatus(), emailData.get(ApplicationConstant.PROJECT_STATUS_FIELD));
        assertEquals(palProject.getFinancialYear(), emailData.get(PALProjectConstants.PALProjectFieldNames.FINANCIAL_YEAR));
        assertEquals(palProject.getProjectType(), emailData.get(PALProjectConstants.PALProjectFieldNames.PROJECT_TYPE));
        assertEquals(palProject.getTemplateName(), emailData.get(PALProjectConstants.PALProjectFieldNames.TEMPLATE_NAME));
        assertEquals(Util.convertLocalDateTimeToString(palProject.getProjectCompletionDate()), emailData.getOrDefault(PALProjectConstants.PALProjectFieldNames.PROJECT_COMPLETION_DATE, null));
        assertTrue(emailData.get(ApplicationConstant.URL).contains(palProject.getId()));
    }

    @Test
    public void sendEmailMessagePostCreativeGateStatusTest() {

        String fileName = "src/test/resources/Notification/PALNotification.json";
        TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
        };
        Map<String, Object> notificationDetails = TestUtility.readFile(fileName, typeReference);

        PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) notificationDetails.getOrDefault("palProject", null),
                new TypeReference<PALProject>() {});
        palProject.setStatus(Status.POST_CREATIVE_GATE.getStatus());

        PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) notificationDetails.getOrDefault("palProduct", null),
                new TypeReference<PALProduct>() {});
        List<PALProduct> palProducts = new ArrayList<>();
        palProducts.add(palProduct);

        when(palDao.findPALProducts(eq(palProject.getId()), eq(null),eq(null))).thenReturn(palProducts);

        MessageTemplate messageTemplate = MessageTemplate.UPDATE_PROJECT_STATUS;
        String triggerConfigId = messageTemplate.getTemplateId() + palProject.getStatus().replaceAll("[- ]", "") + ApplicationConstant.Notification.USERROLES;
        mockUserServiceListUserByRoles(triggerConfigId);

        notificationService.sendEmailMessage(palProject, null, messageTemplate);

        Mockito.verify(kafkaMessageProducer,Mockito.times(1)).sendMessage(messageCaptor.capture(), Mockito.anyString());
        Map<String, Object> message = messageCaptor.getValue();
        assertNotNull(message);
        assertEquals(ApplicationConstant.OFP.toUpperCase(), message.get(ApplicationConstant.Notification.PUBLISHER));
        assertEquals(ApplicationConstant.PAL, message.get(ApplicationConstant.Notification.EVENT_MODULE));
        assertEquals(messageTemplate.getEventName(), message.get(ApplicationConstant.Notification.EVENT_NAME));

        Set<Map<String, String>> userList = (Set<Map<String, String>>) message.getOrDefault(ApplicationConstant.Notification.USERS, null);
        assertNotNull(userList);

        List<String> userEmails = getUserEmails(userList);

        assertEquals(5, userEmails.size());
        assertTrue(userEmails.contains("international2@xyz.com")); //project user
        assertTrue(userEmails.contains("vat1@xyz.com")); //AD Group user
        assertTrue(userEmails.contains("vat2@xyz.com")); //AD Group user
        assertTrue(userEmails.contains("ocado1@xyz.com")); //AD Group user
        assertTrue(userEmails.contains("ocado2@xyz.com")); //AD Group user

        Map<String, Object> data = (Map<String, Object>) message.getOrDefault(ApplicationConstant.Notification.DATA, null);
        assertNotNull(data);

        Map<String, Object> emailMessage = (Map<String, Object>) data.getOrDefault(ApplicationConstant.Notification.EMAIL_MESSAGE, null);
        assertNotNull(emailMessage);
        assertEquals(String.format(messageTemplate.getMessageSubject(), palProject.getProjectName(), palProject.getStatus()), emailMessage.get(ApplicationConstant.Notification.SUBJECT));
        assertEquals(messageTemplate.getTemplateId(), emailMessage.get(ApplicationConstant.Notification.TEMPLATE_ID));
        assertEquals("fromId@xyz.com",emailMessage.get(ApplicationConstant.Notification.FROM));

        Map<String, String> emailData = (Map<String, String>) emailMessage.getOrDefault(ApplicationConstant.Notification.DATA, null);
        assertNotNull(emailData);
        assertEquals(palProject.getProjectName(), emailData.get(PALProjectConstants.PALProjectFieldNames.PROJECT_NAME));
        assertEquals(palProject.getStatus(), emailData.get(ApplicationConstant.PROJECT_STATUS_FIELD));
        assertEquals(palProject.getFinancialYear(), emailData.get(PALProjectConstants.PALProjectFieldNames.FINANCIAL_YEAR));
        assertEquals(palProject.getProjectType(), emailData.get(PALProjectConstants.PALProjectFieldNames.PROJECT_TYPE));
        assertEquals(palProject.getTemplateName(), emailData.get(PALProjectConstants.PALProjectFieldNames.TEMPLATE_NAME));
        assertEquals(Util.convertLocalDateTimeToString(palProject.getProjectCompletionDate()), emailData.getOrDefault(PALProjectConstants.PALProjectFieldNames.PROJECT_COMPLETION_DATE, null));
        assertTrue(emailData.get(ApplicationConstant.URL).contains(palProject.getId()));
    }

    @Test
    public void sendEmailMessageFinaliseStageStatusTest() {

        String fileName = "src/test/resources/Notification/PALNotification.json";
        TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
        };
        Map<String, Object> notificationDetails = TestUtility.readFile(fileName, typeReference);

        PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) notificationDetails.getOrDefault("palProject", null),
                new TypeReference<PALProject>() {});
        palProject.setStatus(Status.FINALISE_STAGE.getStatus());

        PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) notificationDetails.getOrDefault("palProduct", null),
                new TypeReference<PALProduct>() {});
        List<PALProduct> palProducts = new ArrayList<>();
        palProducts.add(palProduct);

        when(palDao.findPALProducts(eq(palProject.getId()), eq(null),eq(null))).thenReturn(palProducts);

        MessageTemplate messageTemplate = MessageTemplate.UPDATE_PROJECT_STATUS;
        String triggerConfigId = messageTemplate.getTemplateId() + palProject.getStatus().replaceAll("[- ]", "") + ApplicationConstant.Notification.USERROLES;
        mockUserServiceListUserByRoles(triggerConfigId);

        notificationService.sendEmailMessage(palProject, null, messageTemplate);

        Mockito.verify(kafkaMessageProducer,Mockito.times(1)).sendMessage(messageCaptor.capture(), Mockito.anyString());
        Map<String, Object> message = messageCaptor.getValue();
        assertNotNull(message);
        assertEquals(ApplicationConstant.OFP.toUpperCase(), message.get(ApplicationConstant.Notification.PUBLISHER));
        assertEquals(ApplicationConstant.PAL, message.get(ApplicationConstant.Notification.EVENT_MODULE));
        assertEquals(messageTemplate.getEventName(), message.get(ApplicationConstant.Notification.EVENT_NAME));

        Set<Map<String, String>> userList = (Set<Map<String, String>>) message.getOrDefault(ApplicationConstant.Notification.USERS, null);
        assertNotNull(userList);

        List<String> userEmails = getUserEmails(userList);

        assertEquals(5, userEmails.size());
        assertTrue(userEmails.contains("commercialPlanner1@xyz.com")); //project user
        assertTrue(userEmails.contains("gsp1@xyz.com")); //AD Group user
        assertTrue(userEmails.contains("gsp2@xyz.com")); //AD Group user
        assertTrue(userEmails.contains("ashbury1@xyz.com")); //AD Group user
        assertTrue(userEmails.contains("ashbury2@xyz.com")); //AD Group user

        Map<String, Object> data = (Map<String, Object>) message.getOrDefault(ApplicationConstant.Notification.DATA, null);
        assertNotNull(data);

        Map<String, Object> emailMessage = (Map<String, Object>) data.getOrDefault(ApplicationConstant.Notification.EMAIL_MESSAGE, null);
        assertNotNull(emailMessage);
        assertEquals(String.format(messageTemplate.getMessageSubject(), palProject.getProjectName(), palProject.getStatus()), emailMessage.get(ApplicationConstant.Notification.SUBJECT));
        assertEquals(messageTemplate.getTemplateId(), emailMessage.get(ApplicationConstant.Notification.TEMPLATE_ID));
        assertEquals("fromId@xyz.com",emailMessage.get(ApplicationConstant.Notification.FROM));

        Map<String, String> emailData = (Map<String, String>) emailMessage.getOrDefault(ApplicationConstant.Notification.DATA, null);
        assertNotNull(emailData);
        assertEquals(palProject.getProjectName(), emailData.get(PALProjectConstants.PALProjectFieldNames.PROJECT_NAME));
        assertEquals(palProject.getStatus(), emailData.get(ApplicationConstant.PROJECT_STATUS_FIELD));
        assertEquals(palProject.getFinancialYear(), emailData.get(PALProjectConstants.PALProjectFieldNames.FINANCIAL_YEAR));
        assertEquals(palProject.getProjectType(), emailData.get(PALProjectConstants.PALProjectFieldNames.PROJECT_TYPE));
        assertEquals(palProject.getTemplateName(), emailData.get(PALProjectConstants.PALProjectFieldNames.TEMPLATE_NAME));
        assertEquals(Util.convertLocalDateTimeToString(palProject.getProjectCompletionDate()), emailData.getOrDefault(PALProjectConstants.PALProjectFieldNames.PROJECT_COMPLETION_DATE, null));
        assertTrue(emailData.get(ApplicationConstant.URL).contains(palProject.getId()));
    }

    @Test
    public void sendEmailMessagePostFinaliseGateStatusTest() {

        String fileName = "src/test/resources/Notification/PALNotification.json";
        TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
        };
        Map<String, Object> notificationDetails = TestUtility.readFile(fileName, typeReference);

        PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) notificationDetails.getOrDefault("palProject", null),
                new TypeReference<PALProject>() {});
        palProject.setStatus(Status.POST_FINALISE_GATE.getStatus());

        PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) notificationDetails.getOrDefault("palProduct", null),
                new TypeReference<PALProduct>() {});
        List<PALProduct> palProducts = new ArrayList<>();
        palProducts.add(palProduct);

        when(palDao.findPALProducts(eq(palProject.getId()), eq(null),eq(null))).thenReturn(palProducts);

        MessageTemplate messageTemplate = MessageTemplate.UPDATE_PROJECT_STATUS;
        String triggerConfigId = messageTemplate.getTemplateId() + palProject.getStatus().replaceAll("[- ]", "") + ApplicationConstant.Notification.USERROLES;
        mockUserServiceListUserByRoles(triggerConfigId);

        notificationService.sendEmailMessage(palProject, null, messageTemplate);

        Mockito.verify(kafkaMessageProducer,Mockito.times(1)).sendMessage(messageCaptor.capture(), Mockito.anyString());
        Map<String, Object> message = messageCaptor.getValue();
        assertNotNull(message);
        assertEquals(ApplicationConstant.OFP.toUpperCase(), message.get(ApplicationConstant.Notification.PUBLISHER));
        assertEquals(ApplicationConstant.PAL, message.get(ApplicationConstant.Notification.EVENT_MODULE));
        assertEquals(messageTemplate.getEventName(), message.get(ApplicationConstant.Notification.EVENT_NAME));

        Set<Map<String, String>> userList = (Set<Map<String, String>>) message.getOrDefault(ApplicationConstant.Notification.USERS, null);
        assertNotNull(userList);

        List<String> userEmails = getUserEmails(userList);

        assertEquals(8, userEmails.size());
        assertTrue(userEmails.contains("productDeveloper1@xyz.com")); //project user
        assertTrue(userEmails.contains("buyer1@xyz.com")); //project user
        assertTrue(userEmails.contains("foodTechnologist2@xyz.com")); //project user
        assertTrue(userEmails.contains("international2@xyz.com")); //project user
        assertTrue(userEmails.contains("gsp1@xyz.com")); //AD Group user
        assertTrue(userEmails.contains("gsp2@xyz.com")); //AD Group user
        assertTrue(userEmails.contains("ocado1@xyz.com")); //AD Group user
        assertTrue(userEmails.contains("ocado2@xyz.com")); //AD Group user

        Map<String, Object> data = (Map<String, Object>) message.getOrDefault(ApplicationConstant.Notification.DATA, null);
        assertNotNull(data);

        Map<String, Object> emailMessage = (Map<String, Object>) data.getOrDefault(ApplicationConstant.Notification.EMAIL_MESSAGE, null);
        assertNotNull(emailMessage);
        assertEquals(String.format(messageTemplate.getMessageSubject(), palProject.getProjectName(), palProject.getStatus()), emailMessage.get(ApplicationConstant.Notification.SUBJECT));
        assertEquals(messageTemplate.getTemplateId(), emailMessage.get(ApplicationConstant.Notification.TEMPLATE_ID));
        assertEquals("fromId@xyz.com",emailMessage.get(ApplicationConstant.Notification.FROM));

        Map<String, String> emailData = (Map<String, String>) emailMessage.getOrDefault(ApplicationConstant.Notification.DATA, null);
        assertNotNull(emailData);
        assertEquals(palProject.getProjectName(), emailData.get(PALProjectConstants.PALProjectFieldNames.PROJECT_NAME));
        assertEquals(palProject.getStatus(), emailData.get(ApplicationConstant.PROJECT_STATUS_FIELD));
        assertEquals(palProject.getFinancialYear(), emailData.get(PALProjectConstants.PALProjectFieldNames.FINANCIAL_YEAR));
        assertEquals(palProject.getProjectType(), emailData.get(PALProjectConstants.PALProjectFieldNames.PROJECT_TYPE));
        assertEquals(palProject.getTemplateName(), emailData.get(PALProjectConstants.PALProjectFieldNames.TEMPLATE_NAME));
        assertEquals(Util.convertLocalDateTimeToString(palProject.getProjectCompletionDate()), emailData.getOrDefault(PALProjectConstants.PALProjectFieldNames.PROJECT_COMPLETION_DATE, null));
        assertTrue(emailData.get(ApplicationConstant.URL).contains(palProject.getId()));
    }

    private List<String> getUserEmails(Set<Map<String, String>> userList) {
        List<String> userEmails = new ArrayList<>();
        userList.forEach(user ->
                user.entrySet().stream().forEach(map -> {
                    if (ApplicationConstant.Notification.USERID.equalsIgnoreCase(map.getKey()))
                        userEmails.add(map.getValue());
                })
        );
        return userEmails;
    }

    @Test
    public void sendEmailMessageAddProductTest() {

        String fileName = "src/test/resources/Notification/PALNotification.json";
        TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
        };
        Map<String, Object> notificationDetails = TestUtility.readFile(fileName, typeReference);

        PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) notificationDetails.getOrDefault("palProduct", null),
                new TypeReference<PALProduct>() {});

        PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) notificationDetails.getOrDefault("palProject", null),
                new TypeReference<PALProject>() {});
        palProject.setStatus(Status.CREATIVE_STAGE.getStatus());

        MessageTemplate messageTemplate = MessageTemplate.ADD_PRODUCT;
        String triggerConfigId = messageTemplate.getTemplateId() + ApplicationConstant.Notification.USERROLES;
        mockUserServiceListUserByRoles(triggerConfigId);

        notificationService.sendEmailMessage(palProject, palProduct, messageTemplate);

        Mockito.verify(kafkaMessageProducer,Mockito.times(1)).sendMessage(messageCaptor.capture(), Mockito.anyString());
        Map<String, Object> message = messageCaptor.getValue();
        assertNotNull(message);
        assertEquals(ApplicationConstant.OFP.toUpperCase(), message.get(ApplicationConstant.Notification.PUBLISHER));
        assertEquals(ApplicationConstant.PAL, message.get(ApplicationConstant.Notification.EVENT_MODULE));
        assertEquals(messageTemplate.getEventName(), message.get(ApplicationConstant.Notification.EVENT_NAME));

        Set<Map<String, String>> userList = (Set<Map<String, String>>) message.getOrDefault(ApplicationConstant.Notification.USERS, null);
        assertNotNull(userList);
        List<String> userEmails = getUserEmails(userList);

        assertEquals(9, userEmails.size());
        assertTrue(userEmails.contains("ocado1@xyz.com")); //AD Group user
        assertTrue(userEmails.contains("ocado2@xyz.com")); //AD Group user
        assertTrue(userEmails.contains("projectManager1@xyz.com")); //Project user
        assertTrue(userEmails.contains("projectManager2@xyz.com")); //Project user
        assertTrue(userEmails.contains("international2@xyz.com")); //Project user
        assertTrue(userEmails.contains("productDeveloper1@xyz.com")); //Product user
        assertTrue(userEmails.contains("buyer1@xyz.com")); //Product user
        assertTrue(userEmails.contains("foodTechnologist2@xyz.com")); //Product user
        assertTrue(userEmails.contains("supplier1@xyz.com")); //Product user

        Map<String, Object> data = (Map<String, Object>) message.getOrDefault(ApplicationConstant.Notification.DATA, null);
        assertNotNull(data);

        Map<String, Object> emailMessage = (Map<String, Object>) data.getOrDefault(ApplicationConstant.Notification.EMAIL_MESSAGE, null);
        assertNotNull(emailMessage);
        assertEquals(messageTemplate.getTemplateId(), emailMessage.get(ApplicationConstant.Notification.TEMPLATE_ID));
        assertEquals("fromId@xyz.com", emailMessage.get(ApplicationConstant.Notification.FROM));

        Map<String, String> emailData = (Map<String, String>) emailMessage.getOrDefault(ApplicationConstant.Notification.DATA, null);
        assertEquals("2021-2022 Christmas Delicatessen", emailData.get(PALProjectConstants.PALProjectFieldNames.PROJECT_NAME));
        assertEquals("In Progress", emailData.get(ApplicationConstant.PRODUCT_STATUS_FIELD));
        assertEquals("Creative Stage", emailData.get(ApplicationConstant.PROJECT_STATUS_FIELD));
        assertEquals("Sausage Roll", emailData.get(ApplicationConstant.PRODUCT_TITLE_FIELD));
        assertEquals("New Product", emailData.get(ApplicationConstant.PRODUCT_TYPE_FIELD));
        assertTrue(emailData.get(ApplicationConstant.URL).contains(palProduct.getId()));
        assertTrue(emailData.get(ApplicationConstant.URL).contains(palProduct.getProjectId()));
    }

    @Test
    public void sendEmailMessageUpdateProductStatusTest() {

        String fileName = "src/test/resources/Notification/PALNotification.json";
        TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {
        };
        Map<String, Object> notificationDetails = TestUtility.readFile(fileName, typeReference);

        PALProduct palProduct = (PALProduct) TestUtility.convertMapToObject((Map<String,Object>) notificationDetails.getOrDefault("palProduct", null),
                new TypeReference<PALProduct>() {});

        PALProject palProject = (PALProject) TestUtility.convertMapToObject((Map<String,Object>) notificationDetails.getOrDefault("palProject", null),
                new TypeReference<PALProject>() {});
        palProject.setStatus(Status.FINALISE_STAGE.getStatus());

        MessageTemplate messageTemplate = MessageTemplate.UPDATE_PRODUCT_STATUS;
        String triggerConfigId = messageTemplate.getTemplateId() + ApplicationConstant.Notification.USERROLES;
        mockUserServiceListUserByRoles(triggerConfigId);

        notificationService.sendEmailMessage(palProject, palProduct, messageTemplate);

        Mockito.verify(kafkaMessageProducer,Mockito.times(1)).sendMessage(messageCaptor.capture(), Mockito.anyString());
        Map<String, Object> message = messageCaptor.getValue();
        assertNotNull(message);
        assertEquals(ApplicationConstant.OFP.toUpperCase(), message.get(ApplicationConstant.Notification.PUBLISHER));
        assertEquals(ApplicationConstant.PAL, message.get(ApplicationConstant.Notification.EVENT_MODULE));
        assertEquals(messageTemplate.getEventName(), message.get(ApplicationConstant.Notification.EVENT_NAME));

        Set<Map<String, String>> userList = (Set<Map<String, String>>) message.getOrDefault(ApplicationConstant.Notification.USERS, null);
        assertNotNull(userList);
        List<String> userEmails = getUserEmails(userList);
        assertEquals(8, userEmails.size());
        assertTrue(userEmails.contains("ocado1@xyz.com")); //AD Group user
        assertTrue(userEmails.contains("ocado2@xyz.com")); //AD Group user
        assertTrue(userEmails.contains("projectManager1@xyz.com")); //Project user
        assertTrue(userEmails.contains("projectManager2@xyz.com")); //Project user
        assertTrue(userEmails.contains("buyer1@xyz.com")); //Project user
        assertTrue(userEmails.contains("international2@xyz.com")); //Project user
        assertTrue(userEmails.contains("foodTechnologist2@xyz.com")); //Project user
        assertTrue(userEmails.contains("productDeveloper1@xyz.com")); //Project user

        Map<String, Object> data = (Map<String, Object>) message.getOrDefault(ApplicationConstant.Notification.DATA, null);
        assertNotNull(data);

        Map<String, Object> emailMessage = (Map<String, Object>) data.getOrDefault(ApplicationConstant.Notification.EMAIL_MESSAGE, null);
        assertNotNull(emailMessage);
        assertEquals(messageTemplate.getTemplateId(), emailMessage.get(ApplicationConstant.Notification.TEMPLATE_ID));
        assertEquals("fromId@xyz.com", emailMessage.get(ApplicationConstant.Notification.FROM));

        Map<String, String> emailData = (Map<String, String>) emailMessage.getOrDefault(ApplicationConstant.Notification.DATA, null);
        assertEquals("2021-2022 Christmas Delicatessen", emailData.get(PALProjectConstants.PALProjectFieldNames.PROJECT_NAME));
        assertEquals("In Progress", emailData.get(ApplicationConstant.PRODUCT_STATUS_FIELD));
        assertEquals("GO", emailData.get(ApplicationConstant.PREVIOUS_STATUS));
        assertEquals("Finalise Stage", emailData.get(ApplicationConstant.PROJECT_STATUS_FIELD));
        assertEquals("29/09/2021", emailData.get(PALProjectConstants.PALProjectFieldNames.PROJECT_COMPLETION_DATE));
        assertEquals("Sausage Roll", emailData.get(ApplicationConstant.PRODUCT_TITLE_FIELD));
        assertEquals("New Product", emailData.get(ApplicationConstant.PRODUCT_TYPE_FIELD));
        assertTrue(emailData.get(ApplicationConstant.URL).contains(palProduct.getId()));
        assertTrue(emailData.get(ApplicationConstant.URL).contains(palProduct.getProjectId()));
    }
}
