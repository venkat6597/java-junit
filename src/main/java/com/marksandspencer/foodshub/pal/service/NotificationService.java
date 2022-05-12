package com.marksandspencer.foodshub.pal.service;

import com.marksandspencer.foodshub.pal.constant.MessageTemplate;
import com.marksandspencer.foodshub.pal.domain.PALProduct;
import com.marksandspencer.foodshub.pal.domain.PALProject;

public interface NotificationService {
    void sendEmailMessage(PALProject savedPalProject, PALProduct palProduct, MessageTemplate messageTemplate);
}
