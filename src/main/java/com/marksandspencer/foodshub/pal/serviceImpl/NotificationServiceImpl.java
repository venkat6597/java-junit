package com.marksandspencer.foodshub.pal.serviceImpl;

import com.marksandspencer.foodshub.pal.constant.ApplicationConstant;
import com.marksandspencer.foodshub.pal.constant.MessageTemplate;
import com.marksandspencer.foodshub.pal.constant.PALProjectConstants;
import com.marksandspencer.foodshub.pal.dao.ProductAttributeListingDao;
import com.marksandspencer.foodshub.pal.domain.*;
import com.marksandspencer.foodshub.pal.dto.PALUser;
import com.marksandspencer.foodshub.pal.event.KafkaMessageProducer;
import com.marksandspencer.foodshub.pal.service.NotificationService;
import com.marksandspencer.foodshub.pal.service.UserService;
import com.marksandspencer.foodshub.pal.transfer.Personnel;
import com.marksandspencer.foodshub.pal.util.Util;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    @Value("${confluent.kafka.message.topic}")
    String messageTopic;

    @Value("${foods.hub.ui.project.url}")
    String projectUrl;

    @Value("${foods.hub.ui.product.url}")
    String productUrl;

    @Value("${pal.notification.from.emailid}")
    String fromId;

    @Autowired
    KafkaMessageProducer kafkaMessageProducer;

    @Autowired
    ProductAttributeListingDao palDao;

    @Autowired
    UserService userService;

    /**
     * sends email message to kafka topic
     * @param palProject project details
     * @param palProduct product details
     * @param messageTemplate message Template Id
     */
    @Override
    public void sendEmailMessage(PALProject palProject, PALProduct palProduct, MessageTemplate messageTemplate) {
        long startTime = System.currentTimeMillis();
        log.info("PAL NotificationServiceImpl > sendEmailMessage started at {} ", new Date(startTime));
        String messageTemplateId = messageTemplate.getTemplateId();

        Map<String, Object> notificationData = new HashMap<>();
        Set<Map<String, String>> notificationUsers = new HashSet<>();

        if (messageTemplateId.equalsIgnoreCase(MessageTemplate.ADD_PROJECT.getTemplateId())) {
            // send notifications when a project is created
            buildMessageForAddNewProject(palProject, notificationData, notificationUsers, messageTemplate);
        } else if (messageTemplateId.equalsIgnoreCase(MessageTemplate.UPDATE_PROJECT_STATUS.getTemplateId())) {
            // send notifications when project status is updated
            buildMessageForUpdateProjectStatus(palProject, notificationData, notificationUsers, messageTemplate);
        } else if (messageTemplateId.equalsIgnoreCase(MessageTemplate.ADD_PRODUCT.getTemplateId())) {
            // send notifications when product is added to an existing project
            buildMessageForAddNewProduct(palProject, palProduct, notificationData, notificationUsers, messageTemplate);
        } else if (messageTemplateId.equalsIgnoreCase(MessageTemplate.UPDATE_PRODUCT_STATUS.getTemplateId())) {
            // send notification when product status is updated
            buildMessageForUpdateProductStatus(palProject, palProduct, notificationData, notificationUsers, messageTemplate);
        }

        Map<String, Object> palNotification = new HashMap<>();
        palNotification.put(ApplicationConstant.Notification.PUBLISHER,ApplicationConstant.OFP.toUpperCase());
        palNotification.put(ApplicationConstant.Notification.EVENT_MODULE,ApplicationConstant.PAL);
        palNotification.put(ApplicationConstant.Notification.EVENT_NAME,messageTemplate.getEventName());
        palNotification.put(ApplicationConstant.Notification.EVENT_ID,UUID.randomUUID().toString());
        palNotification.put(ApplicationConstant.Notification.DATA,notificationData);
        palNotification.put(ApplicationConstant.Notification.USERS, notificationUsers);
        kafkaMessageProducer.sendMessage(palNotification, messageTopic);
        long endTime = System.currentTimeMillis();
        log.info("PAL NotificationServiceImpl > sendEmailMessage completed at {}, TimeTaken :: {} ms ", new Date(endTime), endTime-startTime);
    }

    private void buildMessageForUpdateProductStatus(PALProject palProject, PALProduct palProduct, Map<String, Object> notificationData, Set<Map<String, String>> notificationUsers, MessageTemplate messageTemplate) {
        log.debug("PAL NotificationServiceImpl > buildMessageForUpdateProductStatus started for {}", palProduct.getId());
        List<Personnel> personnels = new ArrayList<>();
        personnels.add(palProject.getPersonnel());
        personnels.add(palProduct.getPersonnel());

        //fetching users for product status change from notification configuration
        String configId = messageTemplate.getTemplateId() + ApplicationConstant.Notification.USERROLES;
        Map<String, List<String>> notificationUserDetails = getNotificationConfig(configId);
        notificationUsers.addAll(getUsersForNotification(notificationUserDetails, personnels));

        buildEmailMessageForProduct(palProject, palProduct, notificationData, messageTemplate);
        log.debug("PAL NotificationServiceImpl > buildMessageForUpdateProductStatus completed for {}", palProduct.getId());
    }

    private void buildMessageForAddNewProduct(PALProject palProject, PALProduct palProduct, Map<String, Object> notificationData, Set<Map<String, String>> notificationUsers, MessageTemplate messageTemplate) {
        log.debug("PAL NotificationServiceImpl > buildMessageForAddNewProduct started for {}", palProduct.getId());
        List<Personnel> personnels = new ArrayList<>();
        personnels.add(palProject.getPersonnel());
        personnels.add(palProduct.getPersonnel());
        //fetching users when a new product is added to project from notification configuration
        String configId = messageTemplate.getTemplateId() + ApplicationConstant.Notification.USERROLES;
        Map<String, List<String>> notificationUserDetails = getNotificationConfig(configId);
        notificationUsers.addAll(getUsersForNotification(notificationUserDetails, personnels));

        buildEmailMessageForProduct(palProject, palProduct, notificationData, messageTemplate);
        log.debug("PAL NotificationServiceImpl > buildMessageForAddNewProduct completed for {}", palProduct.getId());
    }

    // builds email message content for the new product notification template
    private void buildEmailMessageForProduct(PALProject palProject, PALProduct palProduct, Map<String, Object> notificationData, MessageTemplate messageTemplate) {
        log.debug("PAL NotificationServiceImpl > buildEmailMessageForProduct started for {}", palProduct.getId());
        String url = String.format(productUrl, palProduct.getProjectId(), palProduct.getId());
        List<DataField> dataFields = palProduct.getDatafields();
        String productName = getDataFieldValue(dataFields, ApplicationConstant.PRODUCT_TITLE_FIELD);
        String projectName = palProject.getProjectName();
        String productStatus = getDataFieldValue(dataFields, ApplicationConstant.STATUS_FIELD);
        Map<String, Object> emailMessageData = new HashMap<>();
        emailMessageData.put(PALProjectConstants.PALProjectFieldNames.PROJECT_NAME, projectName);
        emailMessageData.put(ApplicationConstant.PRODUCT_TITLE_FIELD, productName);
        emailMessageData.put(ApplicationConstant.PRODUCT_TYPE_FIELD, getDataFieldValue(dataFields, ApplicationConstant.PRODUCT_TYPE_FIELD));
        emailMessageData.put(ApplicationConstant.PRODUCT_STATUS_FIELD, productStatus);
        emailMessageData.put(ApplicationConstant.PROJECT_STATUS_FIELD, palProject.getStatus());
        if (!ObjectUtils.isEmpty(palProject.getProjectCompletionDate()))
            emailMessageData.put(PALProjectConstants.PALProjectFieldNames.PROJECT_COMPLETION_DATE, Util.convertLocalDateTimeToString(palProject.getProjectCompletionDate()));
        emailMessageData.put(ApplicationConstant.SUBRANGE_FIELD_ID, getDataFieldValue(dataFields, ApplicationConstant.SUBRANGE_FIELD_ID));
        emailMessageData.put(ApplicationConstant.URL, url);
        String previousProductStatus = getDataFieldValue(dataFields,ApplicationConstant.PREVIOUS_STATUS);
        if (!StringUtils.isEmpty(previousProductStatus))
            emailMessageData.put(ApplicationConstant.PREVIOUS_STATUS, previousProductStatus);

        Map<String, Object> emailMessage = new HashMap<>();
        emailMessage.put(ApplicationConstant.Notification.TEMPLATE_ID,messageTemplate.getTemplateId());

        if (messageTemplate.getTemplateId().equalsIgnoreCase(MessageTemplate.UPDATE_PRODUCT_STATUS.getTemplateId()))
            emailMessage.put(ApplicationConstant.Notification.SUBJECT,String.format(messageTemplate.getMessageSubject(), productName, productStatus));
        else
            emailMessage.put(ApplicationConstant.Notification.SUBJECT,String.format(messageTemplate.getMessageSubject(), productName, projectName));
        emailMessage.put(ApplicationConstant.Notification.FROM,fromId);
        emailMessage.put(ApplicationConstant.Notification.DATA,emailMessageData);

        notificationData.put(ApplicationConstant.Notification.EMAIL_MESSAGE, emailMessage);
        log.debug("PAL NotificationServiceImpl > buildEmailMessageForProduct completed for {}", palProduct.getId());
    }

    // builds users list and email message content for the notification when project status is changed
    private void buildMessageForUpdateProjectStatus(PALProject palProject, Map<String, Object> notificationData, Set<Map<String, String>> notificationUsers, MessageTemplate messageTemplate) {
        log.debug("PAL NotificationServiceImpl > buildMessageForUpdateProjectStatus started for {}", palProject.getId());
        List<Personnel> personnels = new ArrayList<>();
        personnels.add(palProject.getPersonnel());
        List<PALProduct> palProducts = palDao.findPALProducts(palProject.getId(), null, null);
        if (!CollectionUtils.isEmpty(palProducts))
            palProducts.forEach(palProduct -> personnels.add(palProduct.getPersonnel()));

        //fetching users for update project status from notification configuration
        String configId = messageTemplate.getTemplateId() + palProject.getStatus().replaceAll("[- ]", "") + ApplicationConstant.Notification.USERROLES;
        Map<String, List<String>> notificationUserDetails = getNotificationConfig(configId);
        notificationUsers.addAll(getUsersForNotification(notificationUserDetails, personnels));

        buildEmailMessageForProject(palProject, notificationData, messageTemplate);
        log.debug("PAL NotificationServiceImpl > buildMessageForUpdateProjectStatus completed for {}", palProject.getId());
    }

    // builds users list and email message content for the notification when new project is created
    private void buildMessageForAddNewProject(PALProject palProject, Map<String, Object> notificationData, Set<Map<String, String>> notificationUsers, MessageTemplate messageTemplate) {
        log.debug("PAL NotificationServiceImpl > buildMessageForAddNewProject started for {}", palProject.getId());
        List<Personnel> personnels = new ArrayList<>();
        personnels.add(palProject.getPersonnel());
        //fetching users for add project from notification configuration
        String configId = messageTemplate.getTemplateId() + ApplicationConstant.Notification.USERROLES;
        Map<String, List<String>> notificationUserDetails = getNotificationConfig(configId);
        notificationUsers.addAll(getUsersForNotification(notificationUserDetails, personnels));

        buildEmailMessageForProject(palProject, notificationData, messageTemplate);
        log.debug("PAL NotificationServiceImpl > buildMessageForAddNewProject completed for {}", palProject.getId());
    }

    private Set<Map<String, String>> getUsersForNotification(Map<String, List<String>> notificationUserDetails, List<Personnel> personnels) {
        Set<Map<String, String>> notificationUsers = new HashSet<>();
        Set<String> userList = new HashSet<>();
        for (Map.Entry<String, List<String>> notificationMap : notificationUserDetails.entrySet()) {
            String key = notificationMap.getKey();
            List<String> userRoles = notificationMap.getValue();
            if (ApplicationConstant.Notification.PROJECT_USERS.equals(key) && !CollectionUtils.isEmpty(userRoles)) {
                userList.addAll(getPersonnelUserList(userRoles, personnels));
            } else if (ApplicationConstant.Notification.PRODUCT_USERS.equals(key) && !CollectionUtils.isEmpty(userRoles)) {
                userList.addAll(getPersonnelUserList(userRoles, personnels));
            } else if (ApplicationConstant.Notification.GROUP_USERS.equals(key) && !CollectionUtils.isEmpty(userRoles)) {
                userList.addAll(getGroupUserList(userRoles));
            }
        }

        if (!CollectionUtils.isEmpty(userList)) {
            userList.stream().filter(user-> !StringUtils.isEmpty(user)).distinct().forEach(user -> {
                Map<String, String> userMap = new HashMap<>();
                userMap.put(ApplicationConstant.Notification.USERID, user);
                userMap.put(ApplicationConstant.Notification.STATUS, ApplicationConstant.READ.toUpperCase());
                userMap.put(ApplicationConstant.Notification.TYPE, ApplicationConstant.Notification.TYPE_EMAIL);
                userMap.put(ApplicationConstant.Notification.MAILTYPE, ApplicationConstant.Notification.MAILTYPE_TO);
                notificationUsers.add(userMap);
            });
        }
        return notificationUsers;
    }

    private Set<String> getGroupUserList(List<String> userRoles) {
        Set<String> users = new HashSet<>();
        List<String> selectedRoles = palDao.findAllPALRoles().stream()
                .filter(role -> userRoles.contains(role.getRole()))
                .map(PALRole::getObjectId)
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(selectedRoles)) {
            Map<String, List<PALUser>> palUsers = userService.listUserByRoles(selectedRoles);
            users.addAll(palUsers.values().stream()
                    .filter(palUser -> !CollectionUtils.isEmpty(palUser))
                    .flatMap(palUser -> palUser.stream().map(PALUser::getEmail))
                    .collect(Collectors.toSet()));
        }
        return users;
    }

    private Set<String> getPersonnelUserList(List<String> userRoles, List<Personnel> personnels) {
        Set<String> personnelUsers = new HashSet<>();
        if (!CollectionUtils.isEmpty(personnels)) {
            personnels.forEach(personnel -> {
                if (!ObjectUtils.isEmpty(personnel.getInternal())) {
                    personnelUsers.addAll(personnel.getInternal().stream()
                            .filter(user -> userRoles.contains(user.getRole()) && !CollectionUtils.isEmpty(user.getUsers()))
                            .flatMap(user -> user.getUsers().stream())
                            .collect(Collectors.toSet()));
                }
                if (!ObjectUtils.isEmpty(personnel.getExternal())) {
                    personnelUsers.addAll(personnel.getExternal().stream()
                            .filter(user -> userRoles.contains(user.getRole()) && !CollectionUtils.isEmpty(user.getUsers()))
                            .flatMap(user -> user.getUsers().stream())
                            .collect(Collectors.toSet()));
                }
            });
        }
        return personnelUsers;
    }

    private Map<String, List<String>> getNotificationConfig(String configId) {
        PALConfiguration notificationConfiguration = palDao.findPALConfigurationById(ApplicationConstant.Notification.NOTIFICATION_CONFIG);

        Map<String, List<String>> notificationUserRoles = new HashMap<>();
        if (!ObjectUtils.isEmpty(notificationConfiguration) && !ObjectUtils.isEmpty(notificationConfiguration.getMapValues())) {
            for (Map<String, Object> notificationMap : notificationConfiguration.getMapValues()) {
                if (configId.equalsIgnoreCase((String) notificationMap.getOrDefault(ApplicationConstant.Notification.TRIGGER, null))) {
                    notificationMap.entrySet().stream()
                            .filter(map -> !ApplicationConstant.Notification.TRIGGER.equals(map.getKey()))
                            .forEach(map -> notificationUserRoles.put(map.getKey(), (List<String>) map.getValue()));
                }
            }
        }
        return notificationUserRoles;
    }

    // builds email message content for the new project and status update notification template
    private void buildEmailMessageForProject(PALProject palProject, Map<String, Object> notificationData, MessageTemplate messageTemplate) {
        log.debug("PAL NotificationServiceImpl > buildEmailMessageForProject started for {}", palProject.getId());
        String url = String.format(projectUrl, palProject.getId());
        Map<String, Object> emailMessageData = new HashMap<>();
        emailMessageData.put(PALProjectConstants.PALProjectFieldNames.PROJECT_NAME, palProject.getProjectName());
        emailMessageData.put(PALProjectConstants.PALProjectFieldNames.TEMPLATE_NAME, palProject.getTemplateName());
        emailMessageData.put(PALProjectConstants.PALProjectFieldNames.PROJECT_TYPE, palProject.getProjectType());
        emailMessageData.put(PALProjectConstants.PALProjectFieldNames.FINANCIAL_YEAR, palProject.getFinancialYear());
        emailMessageData.put(ApplicationConstant.PROJECT_STATUS_FIELD, palProject.getStatus());
        emailMessageData.put(ApplicationConstant.URL, url);
        if (!ObjectUtils.isEmpty(palProject.getProjectCompletionDate())) {
            emailMessageData.put(PALProjectConstants.PALProjectFieldNames.PROJECT_COMPLETION_DATE, Util.convertLocalDateTimeToString(palProject.getProjectCompletionDate()));
        }

        Map<String, Object> emailMessage = new HashMap<>();
        emailMessage.put(ApplicationConstant.Notification.TEMPLATE_ID,messageTemplate.getTemplateId());
        emailMessage.put(ApplicationConstant.Notification.FROM,fromId);
        if (messageTemplate.getTemplateId().equalsIgnoreCase(MessageTemplate.UPDATE_PROJECT_STATUS.getTemplateId()))
            emailMessage.put(ApplicationConstant.Notification.SUBJECT,String.format(messageTemplate.getMessageSubject(), palProject.getProjectName(), palProject.getStatus()));
        else
            emailMessage.put(ApplicationConstant.Notification.SUBJECT,String.format(messageTemplate.getMessageSubject(), palProject.getProjectName()));
        emailMessage.put(ApplicationConstant.Notification.DATA,emailMessageData);

        notificationData.put(ApplicationConstant.Notification.EMAIL_MESSAGE, emailMessage);
        log.debug("PAL NotificationServiceImpl > buildEmailMessageForProject completed for {}", palProject.getId());
    }

    private String getDataFieldValue(List<DataField> dataFields, String field) {
        DataField result = dataFields.stream().filter(dataField ->
                dataField.getFieldId().equals(field))
                .findFirst().orElse(null);
        return !ObjectUtils.isEmpty(result) ? result.getFieldValue().trim() : null;
    }
}
