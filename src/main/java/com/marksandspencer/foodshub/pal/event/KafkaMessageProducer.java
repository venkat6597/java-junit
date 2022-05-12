package com.marksandspencer.foodshub.pal.event;

import com.marksandspencer.foodshub.pal.constant.ErrorCode;
import com.marksandspencer.foodshub.pal.dao.AzureStorageDao;
import com.marksandspencer.foodshub.pal.exception.PALServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.Map;

@Component
@Slf4j
public class KafkaMessageProducer {

    @Autowired(required = false)
    KafkaTemplate<String, Object> palMessageProducer;

    @Autowired
    AzureStorageDao storageDao;

    public void sendMessage(final Object message, final String topic) {
        if (!ObjectUtils.isEmpty(palMessageProducer)) {
            ListenableFuture<SendResult<String, Object>> future = palMessageProducer.send(topic, message);
            future.addCallback(new ListenableFutureCallback<>() {
                @Override
                public void onSuccess(SendResult<String, Object> result) {
                    log.debug("Sent message=[{}] with topic=[{}], offset=[{}], partition=[{}]", message, topic,
                            result.getRecordMetadata().offset(), result.getRecordMetadata().partition());
                    log.info("Message Sent topic=[{}], offset =[{}], partition=[{}]", topic,
                            result.getRecordMetadata().offset(), result.getRecordMetadata().partition());
                }

                @Override
                public void onFailure(Throwable ex) {
                    storageDao.writeNotificationsBlob((Map<String, Object>) message);
                    log.error("Unable to send message=[ {} ] due to : {}", message, ex.getMessage());
                    throw new PALServiceException(ErrorCode.KAFKA_ERROR);
                }
            });
        }
    }
}
